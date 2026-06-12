package cn.pumluda.domain.agent.service.memory;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

/**
 * Agent 记忆实现 —— 每会话保留最近 20 条消息
 * <p>
 * {@code MessageWindowChatMemory} 是滑动窗口——超出窗口的消息自动丢弃，
 * 保证每次 LLM 调用的上下文不超过限制。
 */
@Component
public class AgentMemoryImpl implements IAgentMemory {

    /**
     * 每个会话最多保留的历史消息对数（用户+助手 = 1 对）
     */
    private static final int MAX_MESSAGES = 20;

    @Override
    public ChatMemoryProvider getProvider() {
        return memoryId -> MessageWindowChatMemory.withMaxMessages(MAX_MESSAGES);
    }

}
