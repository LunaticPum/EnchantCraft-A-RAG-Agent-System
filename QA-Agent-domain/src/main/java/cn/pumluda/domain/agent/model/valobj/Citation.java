package cn.pumluda.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 来源引用 —— 检索命中的文档分块标识
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Citation {

    /** 分块 ID */
    private String chunkId;

    /** 所属文档 ID */
    private String documentId;

    /** 标题路径，如 "MySQL 存储引擎 > InnoDB > B+Tree" */
    private String titlePath;

    /** 命中片段的前 200 字摘要 */
    private String snippet;

}
