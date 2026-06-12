package cn.pumluda.infrastructure.adapter.prompt;

import cn.pumluda.domain.agent.adapter.prompt.IPromptLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Prompt 加载实现 —— 从 classpath 下的 prompts 目录读取 .md 文件
 * <p>
 * 每次调用都重新读取文件，改完 docs/prompts/*.md 后重新编译/热部署即刻生效。
 */
@Slf4j
@Component
public class PromptLoaderImpl implements IPromptLoader {

    private static final String PROMPT_PATH = "prompts/agent/system-prompt.md";

    @Override
    public String loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(PROMPT_PATH);
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            log.debug("[Prompt] 加载完成: {} chars", content.length());
            return content;
        } catch (IOException e) {
            log.warn("[Prompt] 加载失败，使用默认 Prompt: {}", e.getMessage());
            return getDefaultPrompt();
        }
    }

    /**
     * 文件不可用时的兜底 Prompt
     */
    private String getDefaultPrompt() {
        return """
               你是一个技术知识库助手。
               核心规则：
               1. 必须基于提供的检索证据回答，不能编造
               2. 证据不足时明确告知用户
               3. 回答末尾标注来源
               
               检索证据：
               {evidence}
               """;
    }

}
