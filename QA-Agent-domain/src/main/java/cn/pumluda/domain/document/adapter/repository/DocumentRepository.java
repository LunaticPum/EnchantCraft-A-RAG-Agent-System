package cn.pumluda.domain.document.adapter.repository;

import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;

import java.util.List;
import java.util.Optional;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档仓储接口（domain 层定义契约，infrastructure 层实现）
 */
public interface DocumentRepository {

    /**
     * 保存文档
     */
    SourceDocumentEntity save(SourceDocumentEntity entity);

    /**
     * 根据 ID 查询文档（仅返回未删除的）
     */
    Optional<SourceDocumentEntity> findById(String id);

    /**
     * 查询所有未删除的文档列表
     */
    List<SourceDocumentEntity> findAll();

}
