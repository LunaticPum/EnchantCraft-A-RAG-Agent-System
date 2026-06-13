package cn.pumluda.domain.document.service.embedding;

import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: Embedding 服务接口——将分块文本向量化并存入向量存储
 */
public interface IEmbeddingService {

    /**
     * 对分块列表进行 Embedding 向量化并存储
     *
     * @param chunks 待向量化的分块列表
     */
    void embedChunks(List<DocumentChunkEntity> chunks);

    void deleteByDocumentId(String documentId);

}
