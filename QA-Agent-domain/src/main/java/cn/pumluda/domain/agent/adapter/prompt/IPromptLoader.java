package cn.pumluda.domain.agent.adapter.prompt;

import cn.pumluda.domain.agent.model.valobj.RetrievalMode;

/**
 * Prompt 模板加载器 —— 每次调用实时读取文件，支持热更新
 */
public interface IPromptLoader {

    /**
     * 根据检索模式加载对应的 System Prompt
     */
    String loadPrompt(RetrievalMode mode);

}
