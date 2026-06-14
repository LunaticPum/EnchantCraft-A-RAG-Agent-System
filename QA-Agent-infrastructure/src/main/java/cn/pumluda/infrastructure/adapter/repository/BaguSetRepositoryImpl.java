package cn.pumluda.infrastructure.adapter.repository;

import cn.pumluda.domain.bagu.adapter.repository.IBaguSetRepository;
import cn.pumluda.domain.bagu.model.entity.QaItemEntity;
import cn.pumluda.domain.bagu.model.entity.QaSetEntity;
import cn.pumluda.infrastructure.dao.QaItemDao;
import cn.pumluda.infrastructure.dao.QaSetDao;
import cn.pumluda.infrastructure.dao.QaSetDocumentRefDao;
import cn.pumluda.infrastructure.dao.po.QaItemPO;
import cn.pumluda.infrastructure.dao.po.QaSetDocumentRefPO;
import cn.pumluda.infrastructure.dao.po.QaSetPO;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BaguSetRepositoryImpl implements IBaguSetRepository {

    private final QaSetDao qaSetDao;
    private final QaItemDao qaItemDao;
    private final QaSetDocumentRefDao qaSetDocumentRefDao;

    @Override
    public QaSetEntity saveSet(QaSetEntity entity) {
        QaSetPO po = toPO(entity);
        qaSetDao.insert(po);
        return fromPO(po);
    }

    @Override
    public QaItemEntity saveItem(QaItemEntity entity) {
        QaItemPO po = toPO(entity);
        qaItemDao.insert(po);
        return fromPO(po);
    }

    @Override
    public void updateItemCount(String setId, int count) {
        QaSetPO po = qaSetDao.selectById(setId);
        if (po != null) {
            po.setItemCount(count);
            qaSetDao.updateById(po);
        }
    }

    @Override
    public void saveDocumentRef(String setId, String documentId) {
        QaSetDocumentRefPO ref = QaSetDocumentRefPO.builder()
                .setId(setId)
                .documentId(documentId)
                .build();
        qaSetDocumentRefDao.insert(ref);
    }

    private QaSetPO toPO(QaSetEntity e) {
        return QaSetPO.builder()
                .title(e.getTitle())
                .description(e.getDescription())
                .itemCount(e.getItemCount())
                .build();
    }

    private QaSetEntity fromPO(QaSetPO po) {
        QaSetEntity e = new QaSetEntity();
        e.setId(po.getId());
        e.setTitle(po.getTitle());
        e.setDescription(po.getDescription());
        e.setItemCount(po.getItemCount() != null ? po.getItemCount() : 0);
        e.setCreatedAt(po.getCreatedAt());
        return e;
    }

    private QaItemPO toPO(QaItemEntity e) {
        return QaItemPO.builder()
                .setId(e.getSetId())
                .question(e.getQuestion())
                .answer(e.getAnswer())
                .difficulty(e.getDifficulty())
                .sortOrder(e.getSortOrder())
                .build();
    }

    private QaItemEntity fromPO(QaItemPO po) {
        QaItemEntity e = new QaItemEntity();
        e.setId(po.getId());
        e.setSetId(po.getSetId());
        e.setQuestion(po.getQuestion());
        e.setAnswer(po.getAnswer());
        e.setDifficulty(po.getDifficulty());
        e.setSortOrder(po.getSortOrder() != null ? po.getSortOrder() : 0);
        return e;
    }

    @Override
    public List<QaSetEntity> findAllSets() {
        return qaSetDao.selectList(null).stream().map(this::fromPO).collect(Collectors.toList());
    }

    @Override
    public QaSetEntity findSetById(String id) {
        QaSetPO po = qaSetDao.selectById(id);
        return po != null ? fromPO(po) : null;
    }

    @Override
    public List<QaItemEntity> findItemsBySetId(String setId) {
        return qaItemDao.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QaItemPO>()
                .eq(QaItemPO::getSetId, setId)
                .orderByAsc(QaItemPO::getSortOrder))
                .stream().map(this::fromPO).collect(Collectors.toList());
    }
}
