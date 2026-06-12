package cn.pumluda.domain.agent.service.mcp.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AiServices Agent 接口 —— 带 @Tool 检索能力
 * <p>
 * LangChain4j 生成代理实现，LLM 可自主调用 KnowledgeSearchTool 检索知识库。
 */
public interface IAgentAssistant {

    @SystemMessage(fromResource = "prompts/tool/system-prompt.md")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

}
