package cn.pumluda.domain.document.adapter.repository;

import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档分块仓储接口（domain 层定义契约）
 */
public interface IDocumentChunkRepository {

    /**
     * 批量保存分块
     */
    List<DocumentChunkEntity> saveAll(List<DocumentChunkEntity> chunks);

    /**
     * 根据文档 ID 查询所有分块，按 chunk_index 升序
     */
    List<DocumentChunkEntity> findByDocumentId(String documentId);

}
