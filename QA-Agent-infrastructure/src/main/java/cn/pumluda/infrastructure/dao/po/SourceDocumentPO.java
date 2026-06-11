package cn.pumluda.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Project: QA-Agent-Pumluda
 * Description: source_document 表持久化对象，通过 MyBatis-Plus 映射数据库记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("source_document")
public class SourceDocumentPO {

    /** 文档唯一标识（UUID），插入时自动生成 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 上传时的原始文件名 */
    private String fileName;

    /** 文件类型，默认 MARKDOWN */
    private String fileType;

    /** 文档完整原始文本内容 */
    private String rawContent;

    /** 文档内容的 MD5 摘要（32位十六进制），用于上传去重校验 */
    private String contentMd5;

    /** 被 QA 集引用次数 */
    private Integer refCount;

    /** 软删除标记：0=正常，1=已删除（MyBatis-Plus @TableLogic 自动过滤） */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最近更新时间 */
    private LocalDateTime updatedAt;

}
