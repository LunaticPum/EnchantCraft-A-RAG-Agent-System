package cn.pumluda.domain.document.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Project: QA-Agent-Pumluda
 * Description: 检索结果值对象——表示一次语义检索命中的分块及其相关性
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    /** 分块 ID */
    private String chunkId;

    /** 所属文档 ID */
    private String documentId;

    /** 标题路径，如 "Java基础 > 集合 > HashMap"，用于告知用户答案来自文档的哪个章节 */
    private String titlePath;

    /** 分块文本内容 */
    private String content;

    /** 相似度得分（0~1），分数越高越相关 */
    private Double score;

}
