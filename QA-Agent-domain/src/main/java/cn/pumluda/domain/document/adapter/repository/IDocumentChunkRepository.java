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

    /**
     * 关键词检索——在分块内容中 LIKE 匹配关键词，按 chunk_index 排序
     *
     * @param keyword 搜索关键词
     * @param limit   返回条数上限
     * @return 匹配的分块列表
     */
    List<DocumentChunkEntity> findByKeyword(String keyword, int limit);

}
