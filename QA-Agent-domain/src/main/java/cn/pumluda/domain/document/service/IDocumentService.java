package cn.pumluda.domain.document.service;

import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;
import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档领域服务接口
 */
public interface IDocumentService {

    /**
     * 上传并保存 Markdown 文档
     *
     * @param fileName 原始文件名
     * @param content  文件内容
     * @return 保存后的文档实体
     */
    SourceDocumentEntity uploadDocument(String fileName, String content);

    /**
     * 根据 ID 查询文档
     */
    SourceDocumentEntity getDocument(String id);

    /**
     * 查询所有文档列表
     */
    List<SourceDocumentEntity> listDocuments();

    /**
     * 根据文档 ID 查询其所有分块
     *
     * @param documentId 文档 ID
     * @return 分块实体列表，按 chunk_index 升序
     */
    List<DocumentChunkEntity> getDocumentChunks(String documentId);

    /**
     * 异步执行 Embedding —— 从 MySQL 加载分块后向量化存入 PostgreSQL
     *
     * @param documentId 文档 ID
     */
    void embedDocumentChunks(String documentId);

    /**
     * 查询文档 Embedding 状态
     *
     * @param documentId 文档 ID
     * @return 状态：PENDING / COMPLETED / FAILED / NOT_FOUND
     */
    String getEmbeddingStatus(String documentId);

}
