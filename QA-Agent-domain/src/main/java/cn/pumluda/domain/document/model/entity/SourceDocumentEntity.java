package cn.pumluda.domain.document.model.entity;

import cn.pumluda.domain.document.model.valobj.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档领域实体——表示一份已上传的 Markdown 文档
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDocumentEntity {

    /** 文档唯一标识（UUID） */
    private String id;

    /** 上传时的原始文件名，如 "Java面试笔记.md" */
    private String fileName;

    /** 文档类型，当前仅支持 MARKDOWN */
    private DocumentType fileType;

    /** 文档完整原始文本内容 */
    private String rawContent;

    /** 文件在原始目录中的相对路径，如 MySQL/存储引擎 */
    private String directoryPath;

    /** 文档内容的 MD5 摘要（32位十六进制），用于上传去重 */
    private String contentMd5;

    /** 被 QA 集引用的次数 */
    private Integer refCount;

    /** 软删除标记：false=正常，true=已删除 */
    private Boolean isDeleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最近更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 创建新文档实体（工厂方法）
     *
     * @param fileName      原始文件名
     * @param rawContent    文档文本内容
     * @param directoryPath 文件相对路径，如 MySQL/存储引擎
     * @return 初始化后的文档实体，id 为 null（由持久层自动生成），contentMd5 已计算
     */
    public static SourceDocumentEntity create(String fileName, String rawContent, String directoryPath) {
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(rawContent);
        return SourceDocumentEntity.builder()
                .fileName(fileName)
                .fileType(DocumentType.MARKDOWN)
                .rawContent(rawContent)
                .directoryPath(directoryPath)
                .contentMd5(md5)
                .refCount(0)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

}
