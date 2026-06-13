package cn.pumluda.domain.document.service;

import cn.pumluda.domain.document.adapter.producer.IIndexingMessageProducer;
import cn.pumluda.domain.document.adapter.repository.IDocumentChunkRepository;
import cn.pumluda.domain.document.adapter.repository.IDocumentRepository;
import cn.pumluda.domain.document.adapter.repository.IMessageJobRepository;
import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;
import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;
import cn.pumluda.domain.document.service.chunk.IMarkdownChunker;
import cn.pumluda.domain.document.service.embedding.IEmbeddingService;
import cn.pumluda.types.enums.ResponseCode;
import cn.pumluda.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档领域服务实现——上传落库（事务） + 异步 Embedding（Kafka 驱动）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements IDocumentService {

    private final IDocumentRepository documentRepository;
    private final IDocumentChunkRepository chunkRepository;

    private final IMarkdownChunker markdownChunker;
    private final IEmbeddingService embeddingService;

    private final IIndexingMessageProducer indexingProducer;
    private final IMessageJobRepository messageJobRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SourceDocumentEntity uploadDocument(String fileName, String content, String directoryPath) {
        log.info("[文档上传] 开始: fileName={}, dir={}, contentLength={}", fileName, directoryPath, content.length());

        if (fileName == null || fileName.isBlank()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "文件名不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new AppException(ResponseCode.DOCUMENT_CONTENT_EMPTY.getCode(), "文件内容不能为空");
        }
        if (!fileName.endsWith(".md") && !fileName.endsWith(".markdown")) {
            throw new AppException(
                    ResponseCode.DOCUMENT_TYPE_UNSUPPORTED.getCode(),
                    "仅支持 Markdown 文件（.md / .markdown）"
            );
        }

        // ① MD5 查重
        String contentMd5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(content);
        documentRepository.findByContentMd5(contentMd5).ifPresent(existing -> {
            throw new AppException(
                    ResponseCode.DOCUMENT_DUPLICATE.getCode(),
                    "文档内容重复，已存在: " + existing.getFileName()
            );
        });

        // ② 保存文档原文 + 分块（纯 MySQL，事务内）
        SourceDocumentEntity entity = SourceDocumentEntity.create(fileName, content, directoryPath);
        SourceDocumentEntity saved = documentRepository.save(entity);
        List<DocumentChunkEntity> chunks = markdownChunker.chunk(saved);
        chunkRepository.saveAll(chunks);
        log.info("[文档上传] 落库完成: id={}, chunks={}", saved.getId(), chunks.size());

        // ③ 发送 Kafka 消息 + 记录 message_job（触发异步 Embedding）
        indexingProducer.sendChunkEmbedMessage(saved.getId(), chunks.size());
        messageJobRepository.savePending(saved.getId(), "document.indexing");

        return saved;
    }

    /**
     * 异步执行 Embedding —— Kafka Consumer 调用
     */
    @Override
    public void embedDocumentChunks(String documentId) {
        log.info("[异步Embedding] 开始: documentId={}", documentId);
        List<DocumentChunkEntity> chunks = chunkRepository.findByDocumentId(documentId);
        if (chunks.isEmpty()) {
            log.warn("[异步Embedding] 文档无分块: documentId={}", documentId);
            return;
        }
        embeddingService.embedChunks(chunks);
        log.info("[异步Embedding] 完成: documentId={}, chunks={}", documentId, chunks.size());
    }

    @Override
    public String getEmbeddingStatus(String documentId) {
        return messageJobRepository.getStatus(documentId);
    }

    @Override
    public SourceDocumentEntity getDocument(String id) {
        log.info("[文档查询] id={}", id);
        return documentRepository.findById(id).orElseThrow(
                () -> new AppException(ResponseCode.DOCUMENT_NOT_FOUND.getCode(), "文档不存在: " + id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String documentId) {
        log.info("[文档删除] 开始: documentId={}", documentId);
        documentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ResponseCode.DOCUMENT_NOT_FOUND.getCode(), "文档不存在"));
        // ① 软删除 source_document
        documentRepository.deleteById(documentId);
        // ② 删除 MySQL document_chunk
        chunkRepository.deleteByDocumentId(documentId);
        // ③ 删除 PG chunk_search 向量
        embeddingService.deleteByDocumentId(documentId);
        log.info("[文档删除] 完成: documentId={}", documentId);
    }

    @Override
    public List<SourceDocumentEntity> listDocuments() {
        log.info("[文档列表] 查询所有");
        return documentRepository.findAll();
    }

    @Override
    public List<DocumentChunkEntity> getDocumentChunks(String documentId) {
        log.info("[文档分块] documentId={}", documentId);
        documentRepository.findById(documentId).orElseThrow(
                () -> new AppException(ResponseCode.DOCUMENT_NOT_FOUND.getCode(), "文档不存在: " + documentId));
        return chunkRepository.findByDocumentId(documentId);
    }

}
