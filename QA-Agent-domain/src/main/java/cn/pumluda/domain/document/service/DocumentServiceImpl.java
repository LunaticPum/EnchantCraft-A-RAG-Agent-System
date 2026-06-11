package cn.pumluda.domain.document.service;

import cn.pumluda.domain.document.adapter.repository.IDocumentRepository;
import cn.pumluda.domain.document.adapter.repository.IDocumentChunkRepository;
import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;
import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;
import cn.pumluda.domain.document.service.chunk.IMarkdownChunker;
import cn.pumluda.types.enums.ResponseCode;
import cn.pumluda.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档领域服务实现——处理文档上传、查询、分块等核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements IDocumentService {

    private final IDocumentRepository documentRepository;
    private final IMarkdownChunker markdownChunker;
    private final IDocumentChunkRepository chunkRepository;

    @Override
    public SourceDocumentEntity uploadDocument(String fileName, String content) {
        log.info("[文档上传] 开始处理: fileName={}, contentLength={}", fileName, content.length());

        if (fileName == null || fileName.isBlank()) {
            log.warn("[文档上传] 文件名为空");
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "文件名不能为空");
        }
        if (content == null || content.isBlank()) {
            log.warn("[文档上传] 文件内容为空: fileName={}", fileName);
            throw new AppException(ResponseCode.DOCUMENT_CONTENT_EMPTY.getCode(), "文件内容不能为空");
        }
        if (!fileName.endsWith(".md") && !fileName.endsWith(".markdown")) {
            log.warn("[文档上传] 不支持的文件类型: fileName={}", fileName);
            throw new AppException(ResponseCode.DOCUMENT_TYPE_UNSUPPORTED.getCode(), "仅支持 Markdown 文件（.md / .markdown）");
        }

        // 1. 保存文档原文
        SourceDocumentEntity entity = SourceDocumentEntity.create(fileName, content);
        SourceDocumentEntity saved = documentRepository.save(entity);
        log.info("[文档上传] 文档保存成功: id={}", saved.getId());

        // 2. 分块 + 批量存储
        List<DocumentChunkEntity> chunks = markdownChunker.chunk(saved);
        chunkRepository.saveAll(chunks);
        log.info("[文档上传] 分块完成: chunks={}", chunks.size());

        return saved;
    }

    @Override
    public SourceDocumentEntity getDocument(String id) {
        log.info("[文档查询] 查询文档: id={}", id);
        return documentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[文档查询] 文档不存在: id={}", id);
                    return new AppException(ResponseCode.DOCUMENT_NOT_FOUND.getCode(), "文档不存在: " + id);
                });
    }

    @Override
    public List<SourceDocumentEntity> listDocuments() {
        log.info("[文档列表] 查询所有文档");
        List<SourceDocumentEntity> documents = documentRepository.findAll();
        log.info("[文档列表] 查询完成: count={}", documents.size());
        return documents;
    }

    @Override
    public List<DocumentChunkEntity> getDocumentChunks(String documentId) {
        log.info("[文档分块] 查询分块: documentId={}", documentId);
        // 先校验文档存在
        documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.warn("[文档分块] 文档不存在: id={}", documentId);
                    return new AppException(ResponseCode.DOCUMENT_NOT_FOUND.getCode(), "文档不存在: " + documentId);
                });
        List<DocumentChunkEntity> chunks = chunkRepository.findByDocumentId(documentId);
        log.info("[文档分块] 查询完成: count={}", chunks.size());
        return chunks;
    }

}
