package cn.pumluda.domain.bagu.adapter.repository;

import cn.pumluda.domain.bagu.model.entity.QaSetEntity;
import cn.pumluda.domain.bagu.model.entity.QaItemEntity;

import java.util.List;

public interface IBaguSetRepository {
    QaSetEntity saveSet(QaSetEntity set);
    QaItemEntity saveItem(QaItemEntity item);
    void updateItemCount(String setId, int count);
    void saveDocumentRef(String setId, String documentId);
    List<QaSetEntity> findAllSets();
    QaSetEntity findSetById(String id);
    List<QaItemEntity> findItemsBySetId(String setId);
    void deleteSet(String id);
}
