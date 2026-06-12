package cn.pumluda.domain.agent.service.chat;

import cn.pumluda.domain.agent.adapter.prompt.IPromptLoader;
import cn.pumluda.domain.agent.model.valobj.Citation;
import cn.pumluda.domain.agent.model.valobj.RetrievalMode;
import cn.pumluda.domain.agent.service.mcp.agent.IToolCallingAgent;
import cn.pumluda.domain.agent.service.mcp.tool.KnowledgeBaseSearchTool;
import cn.pumluda.domain.agent.service.memory.IAgentMemory;
import cn.pumluda.domain.agent.service.rewrite.IQueryRewriter;
import cn.pumluda.domain.document.model.valobj.SearchResult;
import cn.pumluda.domain.document.service.rag.recall.HybridRetrieverImpl;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Agent 对话实现 —— 编排：检索 → Prompt → 生成
 * <p>
 * 使用同步 ChatModel.chat() 获取完整回答后由 Controller 层做 SSE 逐字推送。
 * 参考 LangChain4j 1.14.0: ChatMemory.messages() / ChatMemory.add(ChatMessage).
 */
@Slf4j
@Service
public class AgentChatImpl implements IAgentChat {

    private final ChatModel chatModel;
    private final HybridRetrieverImpl hybridRetriever;
    private final IPromptLoader promptLoader;
    private final IAgentMemory agentMemory;
    private final IQueryRewriter queryRewriter;
    private final IToolCallingAgent toolCallingAgent;

    public AgentChatImpl(ChatModel chatModel, HybridRetrieverImpl hybridRetriever, IPromptLoader promptLoader, IAgentMemory agentMemory, IQueryRewriter queryRewriter, KnowledgeBaseSearchTool knowledgeBaseTool) {
        this.chatModel = chatModel;
        this.hybridRetriever = hybridRetriever;
        this.promptLoader = promptLoader;
        this.agentMemory = agentMemory;
        this.queryRewriter = queryRewriter;
        this.toolCallingAgent = AiServices.builder(IToolCallingAgent.class).chatModel(chatModel).chatMemoryProvider(
                agentMemory.getProvider()).tools(knowledgeBaseTool).build();
    }

    @Override
    public String chat(String sessionId, String userMessage, RetrievalMode retrievalMode, Consumer<String> onToken, Consumer<List<Citation>> onCitation) {
        log.info("[Agent对话] sessionId={}, mode={}, message={}", sessionId, retrievalMode, userMessage);

        if (retrievalMode == RetrievalMode.TOOL) {
            return chatWithToolCalling(sessionId, userMessage, onToken);
        }
        return chatWithForceRetrieval(sessionId, userMessage, onToken, onCitation);
    }

    // ==================== FORCE 模式（强制检索） ====================

    private String chatWithForceRetrieval(String sessionId, String userMessage, Consumer<String> onToken, Consumer<List<Citation>> onCitation) {
        // ① 查询改写 → ② RAG 检索 → ③ Prompt 组装 → ④ LLM 生成 → ⑤ 记忆追加
        String searchQuery = queryRewriter.rewrite(userMessage);
        List<SearchResult> searchResults = hybridRetriever.search(searchQuery, 5, true);
        onCitation.accept(buildCitations(searchResults));

        ChatMemory memory = agentMemory.getProvider().get(sessionId);
        String evidence = buildEvidenceText(searchResults);
        String systemPrompt = promptLoader.loadSystemPrompt().replace("{evidence}", evidence);
        UserMessage userMsg = UserMessage.from(userMessage);

        List<ChatMessage> allMessages = new ArrayList<>();
        allMessages.add(SystemMessage.from(systemPrompt));
        allMessages.addAll(memory.messages());
        allMessages.add(userMsg);

        ChatResponse response = chatModel.chat(allMessages);
        String fullAnswer = response.aiMessage().text();

        memory.add(userMsg);
        memory.add(response.aiMessage());

        for (char c : fullAnswer.toCharArray()) {
            onToken.accept(String.valueOf(c));
        }
        return fullAnswer;
    }

    // ==================== TOOL 模式（AiServices Tool Calling） ====================

    private String chatWithToolCalling(String sessionId, String userMessage, Consumer<String> onToken) {
        String fullAnswer = toolCallingAgent.chat(sessionId, userMessage);

        for (char c : fullAnswer.toCharArray()) {
            onToken.accept(String.valueOf(c));
        }
        return fullAnswer;
    }

    // ==================== 辅助方法 ====================

    private List<Citation> buildCitations(List<SearchResult> results) {
        return results.stream().map(r -> Citation.builder().chunkId(r.getChunkId()).documentId(r.getDocumentId())
                                                 .titlePath(r.getTitlePath()).snippet(truncate(r.getContent(), 200))
                                                 .build()).collect(Collectors.toList());
    }

    private String buildEvidenceText(List<SearchResult> results) {
        if (results.isEmpty()) return "（未检索到相关证据）";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append("---\n").append("来源: ").append(r.getTitlePath()).append("\n").append("内容: ").append(
                    r.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

}
