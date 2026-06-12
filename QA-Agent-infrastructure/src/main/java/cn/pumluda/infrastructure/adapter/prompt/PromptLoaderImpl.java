package cn.pumluda.infrastructure.adapter.prompt;

import cn.pumluda.domain.agent.adapter.prompt.IPromptLoader;
import cn.pumluda.domain.agent.model.valobj.RetrievalMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class PromptLoaderImpl implements IPromptLoader {

    @Override
    public String loadPrompt(RetrievalMode mode) {
        String path = mode == RetrievalMode.TOOL ? "prompts/tool/system-prompt.md" : "prompts/agent/system-prompt.md";
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("[Prompt] 加载失败: {}, 用默认", path);
            return "你是技术知识库助手。基于检索证据回答。\n\n检索证据：\n{evidence}";
        }
    }

}
