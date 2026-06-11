package cn.pumluda.domain.document.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档分块领域实体——表示文档按标题层级切分后的一个语义块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunkEntity {

    /** 分块唯一标识（UUID） */
    private String id;

    /** 所属文档ID */
    private String documentId;

    /** 块在文档中的序号（从 1 开始） */
    private Integer chunkIndex;

    /** 标题路径，如 "Java基础 > 集合框架 > HashMap" */
    private String titlePath;

    /** 分块文本内容 */
    private String content;

    /** 模块标签列表，如 ["java", "集合"] */
    private List<String> moduleTags;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最近更新时间 */
    private LocalDateTime updatedAt;

}
