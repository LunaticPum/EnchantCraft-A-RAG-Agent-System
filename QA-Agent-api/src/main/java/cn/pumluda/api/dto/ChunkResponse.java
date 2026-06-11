package cn.pumluda.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 分块查询响应 DTO——对外暴露的分块数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResponse {

    /** 分块唯一标识（UUID） */
    private String id;

    /** 所属文档ID */
    private String documentId;

    /** 块序号（从 1 开始） */
    private Integer chunkIndex;

    /** 标题路径，如 "Java基础 > 集合 > HashMap" */
    private String titlePath;

    /** 分块文本内容 */
    private String content;

    /** 模块标签列表，如 ["java", "集合"] */
    private List<String> moduleTags;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
