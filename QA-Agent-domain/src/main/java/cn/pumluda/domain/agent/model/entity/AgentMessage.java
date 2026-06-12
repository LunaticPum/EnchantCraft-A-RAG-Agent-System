package cn.pumluda.domain.agent.model.entity;

import cn.pumluda.domain.agent.model.valobj.Citation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 对话消息 —— 一次问答的记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 角色：USER / ASSISTANT
     */
    private String role;

    /**
     * 消息正文
     */
    private String content;

    /**
     * 来源引用（仅 ASSISTANT 消息填充）
     */
    private List<Citation> citations;

    /**
     * 消息时间
     */
    private LocalDateTime timestamp;

    public static AgentMessage user(String sessionId, String content) {
        return AgentMessage.builder()
                           .sessionId(sessionId)
                           .role("USER")
                           .content(content)
                           .timestamp(LocalDateTime.now())
                           .build();
    }

    public static AgentMessage assistant(String sessionId, String content, List<Citation> citations) {
        return AgentMessage.builder()
                           .sessionId(sessionId)
                           .role("ASSISTANT")
                           .content(content)
                           .citations(citations)
                           .timestamp(LocalDateTime.now())
                           .build();
    }

}
