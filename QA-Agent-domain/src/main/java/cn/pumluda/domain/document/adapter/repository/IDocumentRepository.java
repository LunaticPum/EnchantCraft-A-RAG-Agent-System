package cn.pumluda.domain.document.adapter.repository;

import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;

import java.util.List;
import java.util.Optional;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档仓储接口（domain 层定义契约，infrastructure 层实现）
 */
public interface IDocumentRepository {

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

    /**
     * 根据内容 MD5 查重——用于上传幂等校验
     *
     * @param contentMd5 文件内容的 MD5 摘要
     * @return 已存在的文档（如有）
     */
    Optional<SourceDocumentEntity> findByContentMd5(String contentMd5);

    /** 物理删除文档（设置 is_deleted=1） */
    void deleteById(String id);

}
