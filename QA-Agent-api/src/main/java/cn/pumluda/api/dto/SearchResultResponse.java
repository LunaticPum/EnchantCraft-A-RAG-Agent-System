package cn.pumluda.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Project: QA-Agent-Pumluda
 * Description: 检索结果响应 DTO——对外暴露的搜索结果数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {

    /** 命中分块 ID */
    private String chunkId;

    /** 所属文档 ID */
    private String documentId;

    /** 标题路径，如 "Java基础 > 集合 > HashMap" */
    private String titlePath;

    /** 分块文本内容 */
    private String content;

    /** 相似度得分（0~1），降序排列 */
    private Double score;

}
