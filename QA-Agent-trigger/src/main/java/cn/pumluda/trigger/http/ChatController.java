package cn.pumluda.trigger.http;

import cn.pumluda.api.dto.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Project: QA-Agent-Pumluda <p>
 * File: ChatController <p>
 * Created by: 16374 <p>
 * Date: 2026/6/10 <p>
 * Time: 11:09 <p>
 * Description: 对话接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@CrossOrigin("*")
public class ChatController {

    private final OpenAiChatModel chatModel;

    public ChatController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(2 * 60_000L);  // SSE 连接的最大响应时长设置为 2 分钟

        new Thread(() -> {
            try {
                String fullResponse = chatModel.chat(request.getMessage());
                for (char c : fullResponse.toCharArray()) {
                    emitter.send(SseEmitter.event()
                                           .name("token")
                                           .data(String.valueOf(c)));
                    Thread.sleep(20);
                }
                emitter.send(SseEmitter.event().name("done").data("{}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("chat error", e);
                throw new RuntimeException(e);
            }
        }).start();

        return emitter;
    }

}
