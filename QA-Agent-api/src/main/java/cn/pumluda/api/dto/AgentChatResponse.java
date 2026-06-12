package cn.pumluda.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent 对话响应 DTO（非流式的完整响应）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponse {

    /** 会话 ID */
    private String sessionId;

    /** 完整回答 */
    private String answer;

    /** 来源引用 */
    private List<CitationDTO> citations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitationDTO {
        private String chunkId;
        private String documentId;
        private String titlePath;
        private String snippet;
    }

}
