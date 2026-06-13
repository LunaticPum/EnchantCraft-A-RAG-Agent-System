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
        try {
            return Files.readString(external);
        } catch (IOException e) {
            throw new RuntimeException("无法加载外部Prompt: " + external, e);
        }
    }

}
