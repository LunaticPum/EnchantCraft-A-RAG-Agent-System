package cn.pumluda.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Project: QA-Agent-Pumluda
 * Description: document_chunk 表持久化对象——每个 chunk 是文档按标题切分后的一个语义块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("document_chunk")
public class DocumentChunkPO {

    /** 分块唯一标识（UUID），插入时自动生成 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属文档ID，关联 source_document.id */
    private String documentId;

    /** 块在文档中的序号，从 1 开始递增 */
    private Integer chunkIndex;

    /** 标题路径，如 "Java基础 > 集合框架 > HashMap"，表示该块在文档层级中的位置 */
    private String titlePath;

    /** 分块文本内容——该标题节点下的全部段落/代码/列表等文本 */
    private String content;

    /** 模块标签 JSON 数组，如 ["java","集合"]——从标题路径提取的关键词 */
    private String moduleTags;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最近更新时间 */
    private LocalDateTime updatedAt;

}
