package cn.pumluda.domain.agent.service.memory;

import dev.langchain4j.memory.chat.ChatMemoryProvider;

/**
 * Agent 记忆管理 —— 包装 LangChain4j ChatMemoryProvider
 * <p>
 * 通过 memoryId（sessionId）区分不同会话，每个会话自动维护独立的历史上下文。
 */
public interface IAgentMemory {

    /**
     * 获取 ChatMemoryProvider 实例
     * <p>
     * LangChain4j 的 AI Service 会通过 AOP 自动注入历史消息，
     * ChatMemoryProvider 在每次 LLM 调用时根据 @MemoryId 取值获取对应的 ChatMemory。
     */
    ChatMemoryProvider getProvider();

}
