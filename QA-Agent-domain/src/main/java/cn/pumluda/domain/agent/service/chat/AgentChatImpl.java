package cn.pumluda.domain.agent.service.chat;

import cn.pumluda.domain.agent.adapter.prompt.IPromptLoader;
import cn.pumluda.domain.agent.model.valobj.Citation;
import cn.pumluda.domain.agent.model.valobj.RetrievalMode;
import cn.pumluda.domain.agent.service.agents.IRagToolCallingAgent;
import cn.pumluda.domain.agent.service.mcp.tool.KnowledgeSearchTool;
import cn.pumluda.domain.agent.service.memory.IAgentMemory;
import cn.pumluda.domain.agent.service.rewrite.IQueryRewriter;
import cn.pumluda.domain.document.model.valobj.SearchResult;
import cn.pumluda.domain.document.service.rag.retriever.impl.HybridRetrieverImpl;
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

/**
 * Agent 对话实现 —— RAG 检索 + LLM 流式生成编排
 * <p>
 * 两种模式：
 * <ul>
 *   <li>FORCE：强制检索 → StreamingChatModel 真流式（LLM 生成一个字推一个字）</li>
 *   <li>TOOL：AiServices 代理 → LLM 自主调用 @Tool 检索 → ChatModel 同步 + 伪流式</li>
 * </ul>
 * 两者共用改写和混检链路，区别在于底层模型和 System Prompt。
 */
@Slf4j
@Service
public class AgentChatImpl implements IAgentChat {

    private final StreamingChatModel streamingChatModel;    // SSE 流式输出对话模型
    private final ChatModel chatModel;                      // 普通对话对话模型（适用 AiServices 动态代理）
    private final HybridRetrieverImpl hybridRetriever;      // 混合检索（语义+关键词+RRF+Rerank）
    private final IPromptLoader promptLoader;               // System Prompt 外置热加载
    private final IAgentMemory agentMemory;                 // 短期记忆（滑动窗口 20 条）
    private final IQueryRewriter queryRewriter;             // 查询改写（口语→检索关键词）
    private final IRagToolCallingAgent ragToolCallingAgent; // AiServices 动态代理 Agent（TOOL Calling 模式）

    public AgentChatImpl(StreamingChatModel streamingChatModel,
                         ChatModel chatModel,
                         HybridRetrieverImpl hybridRetriever,
                         IPromptLoader promptLoader,
                         IAgentMemory agentMemory,
                         IQueryRewriter queryRewriter,
                         KnowledgeSearchTool knowledgeSearchTool) {

        this.streamingChatModel = streamingChatModel;
        this.chatModel = chatModel;
        this.hybridRetriever = hybridRetriever;
        this.queryRewriter = queryRewriter;
        this.promptLoader = promptLoader;
        this.agentMemory = agentMemory;

        // 构建 AiServices 代理：System Prompt 由 provider 动态加载（支持外部热更新）
        this.ragToolCallingAgent = AiServices.builder(IRagToolCallingAgent.class)
                                             .chatModel(chatModel)
                                             .chatMemoryProvider(agentMemory.getProvider())
                                             .systemMessageProvider(memoryId -> promptLoader.loadPrompt(RetrievalMode.TOOL))
                                             .tools(knowledgeSearchTool)
                                             .build();
    }

    @Override
    public String chat(String sessionId,
                       String userMessage,
                       RetrievalMode mode,
                       Consumer<String> onToken,
                       Consumer<List<Citation>> onCitation) {
        log.info("[Agent对话] sessionId={}, mode={}, message={}", sessionId, mode, userMessage);

        if (mode == RetrievalMode.TOOL) {
            return chatWithToolCalling(sessionId, userMessage, onToken);
        }
        return chatWithForceRetrieval(sessionId, userMessage, onToken, onCitation);
    }

    // ==================== FORCE: 强制检索 + 真流式 ====================

    /**
     * 链路：改写 → RAG检索 → Prompt拼装 → StreamingChatModel 真流式输出 → 记忆追加
     */
    private String chatWithForceRetrieval(String sessionId,
                                          String userMessage,
                                          Consumer<String> onToken,
                                          Consumer<List<Citation>> onCitation) {
        // ① 查询改写：口语化问题 → 检索关键词
        String searchQuery = queryRewriter.rewrite(userMessage);

        // ② RAG 混合检索（语义+关键词+RRF+Rerank）→ 找到 Top-5 证据
        List<SearchResult> results = hybridRetriever.search(searchQuery, 5, true);
        onCitation.accept(buildCitations(results));
        log.info("[Agent-FORCE] 检索完成: query={}, 命中={}", searchQuery, results.size());

        // ③ 加载 System Prompt + 注入检索证据 + 拼装消息
        ChatMemory memory = agentMemory.getProvider().get(sessionId);
        String systemPrompt = promptLoader.loadPrompt(RetrievalMode.FORCE)
                                          .replace("{evidence}", buildEvidenceText(results));
        UserMessage userMsg = UserMessage.from(userMessage);

        List<ChatMessage> allMessages = new ArrayList<>();
        allMessages.add(SystemMessage.from(systemPrompt));
        allMessages.addAll(memory.messages());   // 历史对话（最近 20 条）
        allMessages.add(userMsg);

        // ④ 真流式 LLM：底层调 DeepSeek stream API，onPartialResponse 逐 token 推送
        log.info("[Agent-FORCE] 开始流式生成...");
        StringBuilder fullAnswer = new StringBuilder();
        // streamingChatModel.chat() 内部异步执行流式请求，立即返回 void
        // 用 CompletableFuture 做同步栅栏——done.join() 阻塞当前线程，等待流式完成后在 onCompleteResponse 中调用 done.complete(null) 打开栅栏，确保 onToken 全部回调完毕再返回
        CompletableFuture<Void> done = new CompletableFuture<>();
        streamingChatModel.chat(
                allMessages,
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        // \n 在 SSE data: 行内会被当帧分隔符吃掉 → 替换为 [BR] 标记，前端解码
                        onToken.accept(token.replace("\n", "[BR]"));
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        // 流式结束：保存完整回答 + 追加到短期记忆
                        String text = response.aiMessage().text();
                        fullAnswer.append(text);
                        memory.add(userMsg);
                        memory.add(AiMessage.from(text));
                        done.complete(null);
                        log.info("[Agent-FORCE] 流式完成: answerLength={}", fullAnswer.length());
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("[Agent-FORCE] 流式错误: {}", error.getMessage());
                        done.completeExceptionally(error);
                    }
                }
        );
        done.join();
        return fullAnswer.toString();
    }

    // ==================== TOOL: AiServices @Tool + 伪流式 ====================

    /**
     * 链路：AiServices 代理 → LLM 自主判断是否调 @Tool 检索 → 同步返回 → 手动拆字
     * <p>
     * 为什么 TOOL 是伪流式：LangChain4j 1.14.0 的 AiServices 只接受 ChatModel（同步对话），
     * 不支持 StreamingChatModel。等未来版本升级后可改为真流式。
     */
    private String chatWithToolCalling(String sessionId, String userMessage, Consumer<String> onToken) {
        log.info("[Agent-TOOL] AiServices 调用中...");

        // ① AiServices 代理：LLM 自主决定是否调用 KnowledgeSearchTool.searchKnowledgeBase()
        String fullAnswer = ragToolCallingAgent.chat(sessionId, userMessage);

        // ② 伪流式：完整回答到手后逐字拆分 + 30ms 间隔模拟打字机
        log.info("[Agent-TOOL] 返回完成: answerLength={}", fullAnswer.length());
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

    /**
     * 检索结果 → Citation 列表（前端引用卡片用）
     */
    private List<Citation> buildCitations(List<SearchResult> results) {
        return results.stream().map(r -> Citation.builder()
                                                 .chunkId(r.getChunkId())
                                                 .documentId(r.getDocumentId())
                                                 .titlePath(r.getTitlePath())
                                                 .snippet(truncate(r.getContent(), 200))
                                                 .build()).collect(Collectors.toList());
    }

    /**
     * 检索结果 → Prompt 内嵌的证据文本块
     */
    private String buildEvidenceText(List<SearchResult> results) {
        if (results.isEmpty()) return "（未检索到相关证据）";
        StringBuilder sb = new StringBuilder();
        for (SearchResult r : results)
            sb.append("---\n来源: ").append(r.getTitlePath())
              .append("\n内容: ").append(r.getContent()).append("\n\n");
        return sb.toString();
    }

    /**
     * 文本截断（Citation snippet 显示用）
     */
    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}