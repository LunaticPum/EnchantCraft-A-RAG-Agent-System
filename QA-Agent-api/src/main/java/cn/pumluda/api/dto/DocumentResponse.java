package cn.pumluda.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档相关的 API 响应 DTO——对外暴露的文档数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    /** 文档唯一标识（UUID） */
    private String id;

    /** 文件名 */
    private String fileName;

    /** 文件类型（MARKDOWN） */
    private String fileType;

    /** 文档完整原始文本内容 */
    private String rawContent;

    /** 被 QA 集引用次数 */
    private Integer refCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最近更新时间 */
    private LocalDateTime updatedAt;

}
