package cn.pumluda.domain.agent.service.rewrite;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 查询改写实现 —— 用轻量 LLM 调用将口语化问题转为检索关键词
 * <p>
 * 失败时直接返回原始输入，不阻塞主流程。
 */
@Slf4j
@Service
public class QueryRewriterImpl implements IQueryRewriter {

    private static final String PROMPT_PATH = "prompts/query-rewriter-prompt.md";

    private final ChatModel chatModel;

    public QueryRewriterImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String rewrite(String rawQuery) {
        try {
            String systemPrompt = loadPrompt();
            ChatResponse response = chatModel.chat(SystemMessage.from(systemPrompt), UserMessage.from(rawQuery));
            String rewritten = response.aiMessage().text().trim();
            if (rewritten.isBlank()) return rawQuery;

            log.info("[查询改写] 原始={} → 改写={}", rawQuery, rewritten);
            return rewritten;
        } catch (Exception e) {
            log.warn("[查询改写] 失败，回退原始查询: {}", e.getMessage());
            return rawQuery;
        }
    }

    private String loadPrompt() throws IOException {
        ClassPathResource resource = new ClassPathResource(PROMPT_PATH);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

}
