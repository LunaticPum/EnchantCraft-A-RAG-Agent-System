package cn.pumluda.infrastructure.adapter.repository;

import cn.pumluda.domain.document.adapter.repository.IDocumentRepository;
import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;
import cn.pumluda.domain.document.model.valobj.DocumentType;
import cn.pumluda.infrastructure.dao.SourceDocumentDao;
import cn.pumluda.infrastructure.dao.po.SourceDocumentPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档仓储实现——适配 MyBatis-Plus DAO，负责 PO ↔ Entity 转换
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DocumentRepositoryImpl implements IDocumentRepository {

    private final SourceDocumentDao sourceDocumentDao;

    @Override
    public SourceDocumentEntity save(SourceDocumentEntity entity) {
        SourceDocumentPO po = toDocumentPO(entity);
        if (entity.getId() == null || entity.getId().isBlank()) {
            sourceDocumentDao.insert(po);
            log.debug("[文档仓储] 插入新记录: id={}", po.getId());
        } else {
            sourceDocumentDao.updateById(po);
            log.debug("[文档仓储] 更新记录: id={}", po.getId());
        }
        return toDocumentEntity(po);
    }

    @Override
    public Optional<SourceDocumentEntity> findById(String id) {
        SourceDocumentPO po = sourceDocumentDao.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDocumentEntity(po));
    }

    @Override
    public Optional<SourceDocumentEntity> findByContentMd5(String contentMd5) {
        SourceDocumentPO po = sourceDocumentDao.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SourceDocumentPO>().eq(
                SourceDocumentPO::getContentMd5,
                contentMd5
        ));
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDocumentEntity(po));
    }

    @Override
    public List<SourceDocumentEntity> findAll() {
        List<SourceDocumentPO> poList = sourceDocumentDao.selectList(null);
        return poList.stream().map(this::toDocumentEntity).collect(Collectors.toList());
    }

    // ==================== PO ↔ Entity 转换 ====================

    /**
     * Entity → PO：将领域实体转换为数据库持久化对象
     */
    private SourceDocumentPO toDocumentPO(SourceDocumentEntity entity) {
        return SourceDocumentPO.builder()
                               .id(entity.getId())
                               .fileName(entity.getFileName())
                               .fileType(entity.getFileType() != null ? entity.getFileType()
                                                                              .getCode() : DocumentType.MARKDOWN.getCode())
                               .rawContent(entity.getRawContent())
                               .contentMd5(entity.getContentMd5())
                               .refCount(entity.getRefCount())
                               .isDeleted(entity.getIsDeleted() != null && entity.getIsDeleted() ? 1 : 0)
                               .createdAt(entity.getCreatedAt())
                               .updatedAt(entity.getUpdatedAt())
                               .build();
    }

    /**
     * PO → Entity：将数据库记录转换为领域实体
     */
    private SourceDocumentEntity toDocumentEntity(SourceDocumentPO po) {
        DocumentType docType = DocumentType.MARKDOWN;
        if (po.getFileType() != null) {
            for (DocumentType type : DocumentType.values()) {
                if (type.getCode().equals(po.getFileType())) {
                    docType = type;
                    break;
                }
            }
        }
        return SourceDocumentEntity.builder()
                                   .id(po.getId())
                                   .fileName(po.getFileName())
                                   .fileType(docType)
                                   .rawContent(po.getRawContent())
                                   .contentMd5(po.getContentMd5())
                                   .refCount(po.getRefCount())
                                   .isDeleted(po.getIsDeleted() != null && po.getIsDeleted() == 1)
                                   .createdAt(po.getCreatedAt())
                                   .updatedAt(po.getUpdatedAt())
                                   .build();
    }

}
