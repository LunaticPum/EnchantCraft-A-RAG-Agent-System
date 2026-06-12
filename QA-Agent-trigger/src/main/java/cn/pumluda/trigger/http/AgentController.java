package cn.pumluda.trigger.http;

import cn.pumluda.api.dto.AgentChatRequest;
import cn.pumluda.domain.agent.model.valobj.RetrievalMode;
import cn.pumluda.domain.agent.service.chat.IAgentChat;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@CrossOrigin("*")
public class AgentController {

    private final IAgentChat agentChat;

    public AgentController(IAgentChat agentChat) {
        this.agentChat = agentChat;
    }

    @PostMapping(value = "/chat", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chat(@RequestBody AgentChatRequest request) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId() : UUID.randomUUID().toString();
        RetrievalMode mode = "TOOL".equalsIgnoreCase(request.getRetrievalMode())
                ? RetrievalMode.TOOL : RetrievalMode.FORCE;

        log.info("[Agent接口] 对话请求: sessionId={}, mode={}, message={}", sessionId, mode, request.getMessage());

        SseEmitter emitter = new SseEmitter(5 * 60_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(emitter::completeWithError);

        // 单线程执行，保序
        new Thread(() -> {
            try {
                // 先发引用
                agentChat.chat(
                        sessionId, request.getMessage(), mode,
                        token -> {
                            try {
                                sse(emitter, "token", token);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        citations -> {
                            try {
                                sse(emitter, "citation", JSON.toJSONString(citations));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                sse(emitter, "done", sessionId);
                emitter.complete();
            } catch (Exception e) {
                log.error("[Agent接口] 失败: {}", e.getMessage(), e);
                try {
                    sse(emitter, "error", e.getMessage());
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();

        return emitter;
    }

    private void sse(SseEmitter emitter, String event, String data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data));
    }

}