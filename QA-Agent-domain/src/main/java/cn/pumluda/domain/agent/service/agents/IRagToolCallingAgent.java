package cn.pumluda.domain.agent.service.agents;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/**
 * AiServices Agent 接口 —— 带 @Tool 检索能力
 * System Prompt 由 systemMessageProvider 动态提供（支持外部热加载）
 */
public interface IRagToolCallingAgent {

    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

}
