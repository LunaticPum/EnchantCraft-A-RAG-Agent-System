package cn.pumluda.domain.document.service;

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

}
