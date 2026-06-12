package cn.pumluda.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 对话请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatRequest {

    /** 会话 ID（多轮对话标识，首次传 null 由后端生成） */
    private String sessionId;

    /** 用户输入 */
    private String message;

    /** 检索模式：FORCE（强制检索）/ TOOL（LLM自主，V4.2） */
    private String retrievalMode;

}
