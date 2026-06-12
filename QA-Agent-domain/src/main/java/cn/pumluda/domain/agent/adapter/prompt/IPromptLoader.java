package cn.pumluda.domain.agent.adapter.prompt;

/**
 * Prompt 模板加载器 —— 从外部文件读取 System Prompt
 * <p>
 * 每次调用实时读取文件，天然支持热更新——改完 docs/prompts/*.md 无需重启。
 */
public interface IPromptLoader {

    /**
     * 加载 Agent System Prompt 模板
     *
     * @return 原始模板文本（包含 {evidence} 等占位符）
     */
    String loadSystemPrompt();

}
