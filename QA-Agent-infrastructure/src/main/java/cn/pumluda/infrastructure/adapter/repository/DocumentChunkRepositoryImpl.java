package cn.pumluda.infrastructure.adapter.repository;

import cn.pumluda.domain.document.adapter.repository.IDocumentChunkRepository;
import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;
import cn.pumluda.infrastructure.dao.DocumentChunkDao;
import cn.pumluda.infrastructure.dao.po.DocumentChunkPO;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 文档分块仓储实现——适配 MyBatis-Plus DAO
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DocumentChunkRepositoryImpl implements IDocumentChunkRepository {

    private final DocumentChunkDao documentChunkDao;

    @Override
    public List<DocumentChunkEntity> saveAll(List<DocumentChunkEntity> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            log.debug("[分块仓储] 保存: 空列表，跳过");
            return chunks;
        }

        List<DocumentChunkPO> poList = chunks.stream()
                                             .map(this::toChunkPO)
                                             .toList();

        for (DocumentChunkPO po : poList) {
            documentChunkDao.insert(po);
        }

        log.info("[分块仓储] 批量保存完成: count={}", poList.size());
        // 回写 PO 的 id 到 Entity
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setId(poList.get(i).getId());
        }
        return chunks;
    }

    @Override
    public List<DocumentChunkEntity> findByDocumentId(String documentId) {
        List<DocumentChunkPO> poList = documentChunkDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocumentChunkPO>()
                        .eq(DocumentChunkPO::getDocumentId, documentId)
                        .orderByAsc(DocumentChunkPO::getChunkIndex)
        );
        return poList.stream()
                     .map(this::toChunkEntity)
                     .toList();
    }

    // ==================== PO ↔ Entity 转换 ====================

    /**
     * Entity → PO
     */
    private DocumentChunkPO toChunkPO(DocumentChunkEntity entity) {
        return DocumentChunkPO.builder()
                              .id(entity.getId())
                              .documentId(entity.getDocumentId())
                              .chunkIndex(entity.getChunkIndex())
                              .titlePath(entity.getTitlePath())
                              .content(entity.getContent())
                              .moduleTags(
                                      entity.getModuleTags() != null ? JSON.toJSONString(entity.getModuleTags()) : null)
                              .createdAt(entity.getCreatedAt())
                              .updatedAt(entity.getUpdatedAt())
                              .build();
    }

    /**
     * PO → Entity
     */
    private DocumentChunkEntity toChunkEntity(DocumentChunkPO po) {
        List<String> tags = null;
        if (po.getModuleTags() != null && !po.getModuleTags().isBlank()) {
            tags = JSON.parseArray(po.getModuleTags(), String.class);
        }
        return DocumentChunkEntity.builder()
                                  .id(po.getId())
                                  .documentId(po.getDocumentId())
                                  .chunkIndex(po.getChunkIndex())
                                  .titlePath(po.getTitlePath())
                                  .content(po.getContent())
                                  .moduleTags(tags)
                                  .createdAt(po.getCreatedAt())
                                  .updatedAt(po.getUpdatedAt())
                                  .build();
    }

}
