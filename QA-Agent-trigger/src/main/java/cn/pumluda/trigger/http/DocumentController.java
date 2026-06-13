package cn.pumluda.trigger.http;

import cn.pumluda.api.dto.ChunkResponse;
import cn.pumluda.api.dto.DocumentResponse;
import cn.pumluda.api.dto.SearchResultResponse;
import cn.pumluda.api.response.Response;
import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;
import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;
import cn.pumluda.domain.document.model.valobj.SearchResult;
import cn.pumluda.domain.document.service.IDocumentService;
import cn.pumluda.domain.document.service.rag.retriever.IHybridRetriever;
import cn.pumluda.domain.document.service.rag.retriever.IKeywordRetriever;
import cn.pumluda.domain.document.service.rag.retriever.ISemanticRetriever;
import cn.pumluda.domain.identity.adapter.repository.IUserRepository;
import cn.pumluda.types.enums.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档管理 REST 接口——提供文档上传、详情查询、列表查询、分块查询、语义检索
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/document")
@CrossOrigin("*")
@RequiredArgsConstructor
public class DocumentController {

    private final IDocumentService documentService;
    private final ISemanticRetriever semanticRetriever; // 按语义检索（直接用 LangChain4j 现成的 Embedding 相关度算法）
    private final IKeywordRetriever keywordRetriever;   // 按关键字检索（MySQL 模糊匹配）
    private final IHybridRetriever hybridRetriever;  // 上述两者结果 + RRF 评分重计算 + topK
    private final IUserRepository userRepository;

    /**
     * 上传 Markdown 文档
     * <p>
     * 接收 HTTP multipart/form-data 中的文件，读取 UTF-8 编码的文本内容，
     * 委托领域服务完成校验和持久化，返回文档元数据。
     */
    @PostMapping("/upload")
    public Response<DocumentResponse> upload(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "directoryPath", defaultValue = "") String directoryPath) throws IOException {
        log.info(
                "[文档接口] 收到上传请求: fileName={}, dir={}, size={} bytes",
                file.getOriginalFilename(),
                directoryPath,
                file.getSize()
        );

        String rawFileName = file.getOriginalFilename();
        String fileName;
        if (rawFileName != null) {
            fileName = java.net.URLDecoder.decode(rawFileName, StandardCharsets.UTF_8);
        } else {
            throw new RuntimeException("文件上传有误，文件名为空");
        }
        String directoryPathDecoded = java.net.URLDecoder.decode(directoryPath, StandardCharsets.UTF_8);
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        SourceDocumentEntity entity = documentService.uploadDocument(fileName, content, directoryPathDecoded);
        log.info("[文档接口] 上传完成: id={}", entity.getId());

        return Response.<DocumentResponse>builder().code(ResponseCode.SUCCESS.getCode()).info("上传成功").data(
                toDocumentResponse(entity)).build();
    }

    /**
     * 根据 ID 查询文档详情
     */
    @GetMapping("/{id}")
    public Response<DocumentResponse> detail(@PathVariable("id") String id) {
        log.info("[文档接口] 查询详情: id={}", id);
        SourceDocumentEntity entity = documentService.getDocument(id);
        return Response.<DocumentResponse>builder()
                       .code(ResponseCode.SUCCESS.getCode())
                       .info(ResponseCode.SUCCESS.getInfo())
                       .data(toDocumentResponse(entity))
                       .build();
    }

    /**
     * 查询所有未删除的文档列表
     */
    @GetMapping("/list")
    public Response<List<DocumentResponse>> list() {
        log.info("[文档接口] 查询文档列表");
        List<SourceDocumentEntity> entities = documentService.listDocuments();
        List<DocumentResponse> data = entities.stream().map(this::toDocumentResponse).collect(Collectors.toList());
        log.info("[文档接口] 返回 {} 条记录", data.size());
        return Response.<List<DocumentResponse>>builder()
                       .code(ResponseCode.SUCCESS.getCode())
                       .info(ResponseCode.SUCCESS.getInfo())
                       .data(data)
                       .build();
    }

    /**
     * 查询某文档的所有分块
     */
    @GetMapping("/{id}/chunks")
    public Response<List<ChunkResponse>> chunks(@PathVariable("id") String id) {
        log.info("[文档接口] 查询分块: documentId={}", id);
        List<DocumentChunkEntity> chunks = documentService.getDocumentChunks(id);
        List<ChunkResponse> data = chunks.stream().map(this::toChunkResponse).collect(Collectors.toList());
        log.info("[文档接口] 返回 {} 个分块", data.size());
        return Response.<List<ChunkResponse>>builder()
                       .code(ResponseCode.SUCCESS.getCode())
                       .info(ResponseCode.SUCCESS.getInfo())
                       .data(data)
                       .build();
    }

    /**
     * RAG检索——输入查询关键词，返回最相似的文档分块
     */
    @GetMapping("/search")
    public Response<List<SearchResultResponse>> search(@RequestParam("keyword") String keyword,
                                                       @RequestParam(value = "topK", defaultValue = "5") int topK,
                                                       @RequestParam(value = "strategy", defaultValue = "HYBRID") String strategy,
                                                       @RequestParam(value = "rerank", defaultValue = "true") boolean rerank,
                                                       HttpServletRequest request) {
        log.info("[文档接口] 检索请求: keyword={}, topK={}, strategy={}, rerank={}", keyword, topK, strategy, rerank);
        String userId = (String) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        userRepository.checkAndIncrementSearch(userId, role);

        List<SearchResult> results = switch (strategy.toUpperCase()) {
            case "SEMANTIC" -> semanticRetriever.search(keyword, topK);
            case "KEYWORD" -> keywordRetriever.search(keyword, topK);
            case "HYBRID" -> hybridRetriever.search(keyword, topK, rerank);
            default -> {
                log.warn("[文档接口] 未知检索策略: {}，回退为 HYBRID", strategy);
                yield hybridRetriever.search(keyword, topK, rerank);
            }
        };

        List<SearchResultResponse> data = results.stream().map(this::toSearchResultResponse).toList();
        log.info("[文档接口] 返回 {} 条结果", data.size());
        return Response.<List<SearchResultResponse>>builder()
                       .code(ResponseCode.SUCCESS.getCode())
                       .info(ResponseCode.SUCCESS.getInfo())
                       .data(data)
                       .build();
    }

    /**
     * 查询文档 Embedding 状态
     */
    @GetMapping("/{id}/embedding-status")
    public Response<String> embeddingStatus(@PathVariable("id") String id) {
        String status = documentService.getEmbeddingStatus(id);
        return Response.<String>builder().code(ResponseCode.SUCCESS.getCode()).info(status).data(status).build();
    }

    /**
     * 手动触发 Embedding（用于重试或编辑后重建）
     */
    @PostMapping("/{id}/re-embed")
    public Response<Void> reEmbed(@PathVariable("id") String id) {
        log.info("[文档接口] 手动触发 Embedding: documentId={}", id);
        documentService.embedDocumentChunks(id);
        return Response.<Void>builder().code(ResponseCode.SUCCESS.getCode()).info("Embedding 完成").build();
    }

    /**
     * 删除文档：软删除 source_document + 清理 chunks + 清理 PG 向量
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable("id") String id) {
        documentService.deleteDocument(id);
        return Response.<Void>builder().code(ResponseCode.SUCCESS.getCode()).info("删除成功").build();
    }

    /**
     * 查询当前用户剩余配额
     */
    @GetMapping("/quota")
    public Response<Map<String, Integer>> quota(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return Response.<Map<String, Integer>>builder()
                       .code(ResponseCode.SUCCESS.getCode())
                       .data(Map.of(
                               "search",
                               userRepository.getSearchRemaining(userId),
                               "chat",
                               userRepository.getChatRemaining(userId)
                       ))
                       .build();
    }

    /**
     * 检查 PG 向量数据健康状态
     */
    @GetMapping("/vector-health")
    public Response<Long> vectorHealth() {
        long count = documentService.checkVectorHealth();
        return Response.<Long>builder()
                       .code(ResponseCode.SUCCESS.getCode())
                       .info(count > 0 ? "向量数据正常" : "向量数据异常")
                       .data(count)
                       .build();
    }

    /**
     * 全量重建 Embedding
     */
    @PostMapping("/re-embed-all")
    public Response<Void> reEmbedAll() {
        documentService.embedAllDocuments();
        return Response.<Void>builder().code(ResponseCode.SUCCESS.getCode()).info("全量 Embedding 已触发").build();
    }

    // ==================== DTO 转换 ====================

    /**
     * SourceDocumentEntity → DocumentResponse：将领域实体转换为 API 响应 DTO
     */
    private DocumentResponse toDocumentResponse(SourceDocumentEntity entity) {
        return DocumentResponse.builder()
                               .id(entity.getId())
                               .fileName(entity.getFileName())
                               .fileType(entity.getFileType() != null ? entity.getFileType().getCode() : null)
                               .directoryPath(entity.getDirectoryPath())
                               .rawContent(entity.getRawContent())
                               .refCount(entity.getRefCount())
                               .createdAt(entity.getCreatedAt())
                               .updatedAt(entity.getUpdatedAt())
                               .build();
    }

    /**
     * DocumentChunkEntity → ChunkResponse：将分块领域实体转换为 API 响应 DTO
     */
    private ChunkResponse toChunkResponse(DocumentChunkEntity entity) {
        return ChunkResponse.builder()
                            .id(entity.getId())
                            .documentId(entity.getDocumentId())
                            .chunkIndex(entity.getChunkIndex())
                            .titlePath(entity.getTitlePath())
                            .content(entity.getContent())
                            .moduleTags(entity.getModuleTags())
                            .createdAt(entity.getCreatedAt())
                            .build();
    }

    /**
     * SearchResult → SearchResultResponse：将检索结果值对象转换为 API 响应 DTO
     */
    private SearchResultResponse toSearchResultResponse(SearchResult result) {
        return SearchResultResponse.builder().chunkId(result.getChunkId()).documentId(result.getDocumentId()).titlePath(
                result.getTitlePath()).content(result.getContent()).score(result.getScore()).build();
    }

}
