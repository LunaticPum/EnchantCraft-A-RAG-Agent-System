package cn.pumluda.trigger.http;

import cn.pumluda.api.dto.AgentChatRequest;
import cn.pumluda.domain.agent.model.valobj.Citation;
import cn.pumluda.domain.agent.model.valobj.RetrievalMode;
import cn.pumluda.domain.agent.service.chat.IAgentChat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Agent 对话接口 —— SSE 流式推送
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@CrossOrigin("*")
public class AgentController {

    private final IAgentChat agentChat;

    public AgentController(IAgentChat agentChat) {
        this.agentChat = agentChat;
    }

    /**
     * Agent RAG 对话（SSE 流式）
     * <p>
     * 事件类型：
     * <ul>
     *   <li>{@code citation} — 检索完成后推送的引用列表（JSON）</li>
     *   <li>{@code token} — 逐字推送的回答文本</li>
     *   <li>{@code done} — 对话结束</li>
     * </ul>
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody AgentChatRequest request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        RetrievalMode mode = "TOOL".equalsIgnoreCase(
                request.getRetrievalMode()) ? RetrievalMode.TOOL : RetrievalMode.FORCE;

        log.info("[Agent接口] 对话请求: sessionId={}, mode={}, message={}", sessionId, mode, request.getMessage());

        SseEmitter emitter = new SseEmitter(5 * 60_000L); // 5 分钟超时

        CompletableFuture.runAsync(() -> {
            try {
                List<Citation>[] citationsHolder = new List[1];

                String answer = agentChat.chat(
                        sessionId, request.getMessage(), mode, token -> {
                            try {
                                emitter.send(SseEmitter.event().name("token").data(token));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }, citations -> {
                            citationsHolder[0] = citations;
                            try {
                                emitter.send(SseEmitter.event().name("citation").data(citations));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );

                emitter.send(SseEmitter.event().name("done").data("{\"sessionId\":\"" + sessionId + "\"}"));
                emitter.complete();

            } catch (Exception e) {
                log.error("[Agent接口] 对话失败: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

}
