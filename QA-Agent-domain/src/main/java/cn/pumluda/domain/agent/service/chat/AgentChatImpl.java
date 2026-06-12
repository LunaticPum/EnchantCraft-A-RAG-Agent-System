package cn.pumluda.domain.agent.service.chat;

import cn.pumluda.domain.agent.adapter.prompt.IPromptLoader;
import cn.pumluda.domain.agent.model.valobj.Citation;
import cn.pumluda.domain.agent.model.valobj.RetrievalMode;
import cn.pumluda.domain.agent.service.mcp.agent.IAgentAssistant;
import cn.pumluda.domain.agent.service.mcp.tool.KnowledgeSearchTool;
import cn.pumluda.domain.agent.service.memory.IAgentMemory;
import cn.pumluda.domain.agent.service.rewrite.IQueryRewriter;
import cn.pumluda.domain.document.model.valobj.SearchResult;
import cn.pumluda.domain.document.service.rag.recall.HybridRetrieverImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentChatImpl implements IAgentChat {

    private final StreamingChatModel streamingChatModel;
    private final ChatModel chatModel;
    private final HybridRetrieverImpl hybridRetriever;
    private final IPromptLoader promptLoader;
    private final IAgentMemory agentMemory;
    private final IQueryRewriter queryRewriter;
    private final IAgentAssistant agentAssistant;

    public AgentChatImpl(StreamingChatModel streamingChatModel, ChatModel chatModel,
                         HybridRetrieverImpl hybridRetriever, IPromptLoader promptLoader,
                         IAgentMemory agentMemory, IQueryRewriter queryRewriter,
                         KnowledgeSearchTool knowledgeSearchTool) {
        this.streamingChatModel = streamingChatModel;
        this.chatModel = chatModel;
        this.hybridRetriever = hybridRetriever;
        this.promptLoader = promptLoader;
        this.agentMemory = agentMemory;
        this.queryRewriter = queryRewriter;
        this.agentAssistant = AiServices.builder(IAgentAssistant.class)
                                        .chatModel(chatModel)
                                        .chatMemoryProvider(agentMemory.getProvider())
                                        .tools(knowledgeSearchTool)
                                        .build();
    }

    @Override
    public String chat(String sessionId, String userMessage, RetrievalMode mode,
                       Consumer<String> onToken, Consumer<List<Citation>> onCitation) {
        log.info("[Agent对话] sessionId={}, mode={}", sessionId, mode);

        if (mode == RetrievalMode.TOOL) {
            return chatWithToolCalling(sessionId, userMessage, onToken);
        }
        return chatWithForceRetrieval(sessionId, userMessage, onToken, onCitation);
    }

    // ==================== FORCE: 真流式 ====================

    private String chatWithForceRetrieval(String sessionId, String userMessage,
                                          Consumer<String> onToken, Consumer<List<Citation>> onCitation) {
        String searchQuery = queryRewriter.rewrite(userMessage);
        List<SearchResult> results = hybridRetriever.search(searchQuery, 5, true);
        onCitation.accept(buildCitations(results));

        ChatMemory memory = agentMemory.getProvider().get(sessionId);
        String systemPrompt = promptLoader.loadPrompt(RetrievalMode.FORCE)
                                          .replace("{evidence}", buildEvidenceText(results));
        UserMessage userMsg = UserMessage.from(userMessage);

        List<ChatMessage> allMessages = new ArrayList<>();
        allMessages.add(SystemMessage.from(systemPrompt));
        allMessages.addAll(memory.messages());
        allMessages.add(userMsg);

        StringBuilder fullAnswer = new StringBuilder();
        CompletableFuture<Void> done = new CompletableFuture<>();
        streamingChatModel.chat(
                allMessages, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        onToken.accept(token.replace("\n", "[BR]"));
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        String text = response.aiMessage().text();
                        fullAnswer.append(text);
                        memory.add(userMsg);
                        memory.add(AiMessage.from(text));
                        done.complete(null);
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("[Agent流式] 错误: {}", error.getMessage());
                        done.completeExceptionally(error);
                    }
                }
        );
        done.join();
        return fullAnswer.toString();
    }

    // ==================== TOOL: AiServices @Tool 调用 + 伪流式 ====================

    private String chatWithToolCalling(String sessionId, String userMessage,
                                       Consumer<String> onToken) {
        // AiServices 代理：LLM 自主决定是否调用 KnowledgeSearchTool.searchKnowledgeBase()
        String fullAnswer = agentAssistant.chat(sessionId, userMessage);

        // 手动拆字模拟打字机
        for (char c : fullAnswer.toCharArray()) {
            onToken.accept(c == '\n' ? "[BR]" : String.valueOf(c));
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                break;
            }
        }
        return fullAnswer;
    }

    // ==================== 辅助 ====================

    private List<Citation> buildCitations(List<SearchResult> results) {
        return results.stream().map(r -> Citation.builder().chunkId(r.getChunkId())
                                                 .documentId(r.getDocumentId()).titlePath(r.getTitlePath())
                                                 .snippet(truncate(r.getContent(), 200)).build()).collect(
                Collectors.toList());
    }

    private String buildEvidenceText(List<SearchResult> results) {
        if (results.isEmpty()) return "（未检索到相关证据）";
        StringBuilder sb = new StringBuilder();
        for (SearchResult r : results)
            sb.append("---\n来源: ").append(r.getTitlePath()).append("\n内容: ").append(r.getContent()).append("\n\n");
        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}