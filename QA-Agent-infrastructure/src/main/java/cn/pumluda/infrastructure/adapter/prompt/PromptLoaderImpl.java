package cn.pumluda.infrastructure.adapter.prompt;

import cn.pumluda.domain.agent.adapter.prompt.IPromptLoader;
import cn.pumluda.domain.agent.model.valobj.RetrievalMode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class PromptLoaderImpl implements IPromptLoader {

    private final Path basePath;

    public PromptLoaderImpl(@Value("${prompt.base-path:./docs/prompts}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        log.info("[Prompt] 外部路径: {} (exists={})", basePath, Files.exists(basePath));
    }

    @Override
    public String loadPrompt(RetrievalMode mode) {
        String fileName = mode == RetrievalMode.TOOL
                ? "tool/system-prompt.md" : "agent/system-prompt.md";

        Path external = basePath.resolve(fileName);
        if (Files.exists(external)) {
            try {
                String content = Files.readString(external);
                log.info("[Prompt] 外部加载成功: {}", external);
                return content;
            } catch (IOException e) {
                log.warn("[Prompt] 外部读取失败: {}", e.getMessage());
            }
        } else {
            log.info("[Prompt] 外部不存在: {}，回退classpath", external);
        }

        try {
            String content = new ClassPathResource("prompts/" + fileName)
                    .getContentAsString(StandardCharsets.UTF_8);
            log.info("[Prompt] classpath加载: {}", fileName);
            return content;
        } catch (IOException e) {
            log.warn("[Prompt] 全部失败，用兜底");
            return "你是技术知识库助手。基于检索证据回答。\n\n检索证据：\n{evidence}";
        }
    }

}
