package cn.pumluda.infrastructure.adapter.prompt;

import cn.pumluda.domain.agent.adapter.prompt.IPromptLoader;
import cn.pumluda.domain.agent.model.valobj.RetrievalMode;
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
        this.basePath = Path.of(basePath);
    }

    @Override
    public String loadPrompt(RetrievalMode mode) {
        String fileName = mode == RetrievalMode.TOOL
                ? "tool/system-prompt.md" : "agent/system-prompt.md";

        // 1. 优先读外部文件系统（生产环境挂载的宿主目录，支持热更新）
        Path external = basePath.resolve(fileName);
        if (Files.exists(external)) {
            try {
                return Files.readString(external);
            } catch (IOException e) {
                log.warn("[Prompt] 外部文件读取失败: {}, 回退classpath", external);
            }
        }

        // 2. 回退 classpath（开发环境 / JAR 内置默认）
        try {
            return new ClassPathResource("prompts/" + fileName)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("[Prompt] classpath读取失败，用兜底");
            return "你是技术知识库助手。基于检索证据回答。\n\n检索证据：\n{evidence}";
        }
    }

}
