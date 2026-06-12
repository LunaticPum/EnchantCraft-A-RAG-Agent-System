package cn.pumluda.domain.agent.service.mcp.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AiServices Agent 接口 —— Tool Calling 模式的对话 Agent
 * <p>
 * LangChain4j 通过 AOP 自动注入 ChatMemory、调用 @Tool 方法，
 * LLM 自主判断是否需要调用工具检索知识库。
 */
public interface IToolCallingAgent {

    /**
     * Tool Calling 对话
     *
     * @param memoryId    会话 ID（自动关联 ChatMemoryProvider）
     * @param userMessage 用户输入
     * @return LLM 回答
     */
    @SystemMessage(fromResource = "prompts/tool/system-prompt.md")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

}
