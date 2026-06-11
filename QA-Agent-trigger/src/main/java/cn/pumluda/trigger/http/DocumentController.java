package cn.pumluda.trigger.http;

import cn.pumluda.api.dto.DocumentResponse;
import cn.pumluda.api.response.Response;
import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;
import cn.pumluda.domain.document.service.IDocumentService;
import cn.pumluda.types.enums.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档管理 REST 接口——提供文档上传、详情查询、列表查询
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/document")
@CrossOrigin("*")
@RequiredArgsConstructor
public class DocumentController {

    private final IDocumentService documentService;

    /**
     * 上传 Markdown 文档
     * <p>
     * 接收 HTTP multipart/form-data 中的文件，读取 UTF-8 编码的文本内容，
     * 委托领域服务完成校验和持久化，返回文档元数据。
     */
    @PostMapping("/upload")
    public Response<DocumentResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("[文档接口] 收到上传请求: fileName={}, size={} bytes", file.getOriginalFilename(), file.getSize());

        String rawFileName = file.getOriginalFilename();  // 浏览器会预先对请求头中文字符串做URL编码，因此我们要先做URL解码
        String fileName;
        if (rawFileName != null) {
            fileName = java.net.URLDecoder.decode(rawFileName, StandardCharsets.UTF_8);
        } else {
            throw new RuntimeException("文件上传有误，文件名为空");
        }
        // MultipartFile.getBytes() 获取上传文件的原始字节流，按 UTF-8 解码为文本
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        SourceDocumentEntity entity = documentService.uploadDocument(fileName, content);
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

    // ==================== DTO 转换 ====================

    /**
     * SourceDocumentEntity → DocumentResponse：将领域实体转换为 API 响应 DTO
     */
    private DocumentResponse toDocumentResponse(SourceDocumentEntity entity) {
        return DocumentResponse.builder()
                               .id(entity.getId())
                               .fileName(entity.getFileName())
                               .fileType(entity.getFileType() != null ? entity.getFileType().getCode() : null)
                               .rawContent(entity.getRawContent())
                               .refCount(entity.getRefCount())
                               .createdAt(entity.getCreatedAt())
                               .updatedAt(entity.getUpdatedAt())
                               .build();
    }

}
