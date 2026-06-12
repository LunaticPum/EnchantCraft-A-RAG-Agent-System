package cn.pumluda.domain.agent.service.chat;

import cn.pumluda.domain.agent.adapter.prompt.IPromptLoader;
import cn.pumluda.domain.agent.model.valobj.Citation;
import cn.pumluda.domain.agent.model.valobj.RetrievalMode;
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

    public AgentChatImpl(ChatModel chatModel, HybridRetrieverImpl hybridRetriever,
                         IPromptLoader promptLoader, IAgentMemory agentMemory,
                         IQueryRewriter queryRewriter) {
        this.chatModel = chatModel;
        this.hybridRetriever = hybridRetriever;
        this.promptLoader = promptLoader;
        this.agentMemory = agentMemory;
        this.queryRewriter = queryRewriter;
    }

    @Override
    public String chat(String sessionId, String userMessage, RetrievalMode retrievalMode, Consumer<String> onToken, Consumer<List<Citation>> onCitation) {
        log.info("[Agent对话] sessionId={}, mode={}, message={}", sessionId, retrievalMode, userMessage);

        // 0. 查询改写（提升检索召回率）
        String searchQuery = queryRewriter.rewrite(userMessage);

        // 1. RAG 检索证据（以改写后的 query 检索）
        List<SearchResult> searchResults = hybridRetriever.search(searchQuery, 5, true);
        List<Citation> citations = buildCitations(searchResults);
        onCitation.accept(citations);

        // 2. 组装上下文
        ChatMemory memory = agentMemory.getProvider().get(sessionId);
        String evidence = buildEvidenceText(searchResults);
        String systemPrompt = promptLoader.loadSystemPrompt().replace("{evidence}", evidence);
        UserMessage userMsg = UserMessage.from(userMessage);

        // 3. 组装消息列表：SystemPrompt + 历史 + 当前用户问题
        List<ChatMessage> allMessages = new ArrayList<>();
        allMessages.add(SystemMessage.from(systemPrompt));
        allMessages.addAll(memory.messages());      // ChatMemory 已有的历史消息
        allMessages.add(userMsg);

        // 4. 调 LLM → Response<AiMessage> → .aiMessage().text()
        ChatResponse response = chatModel.chat(allMessages);
        String fullAnswer = response.aiMessage().text();

        // 5. 追加到记忆
        memory.add(userMsg);
        memory.add(response.aiMessage());

        // 6. 逐字符回调给上层（Controller 用 SSE 推送）
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
