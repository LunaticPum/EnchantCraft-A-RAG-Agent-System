package cn.pumluda.infrastructure.adapter.repository;

import cn.pumluda.domain.document.adapter.repository.IMessageJobRepository;
import cn.pumluda.infrastructure.dao.MessageJobDao;
import cn.pumluda.infrastructure.dao.po.MessageJobPO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 消息任务仓储实现——适配 MyBatis-Plus DAO
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageJobRepositoryImpl implements IMessageJobRepository {

    private final MessageJobDao messageJobDao;

    @Override
    public void savePending(String documentId, String topic) {
        MessageJobPO po = MessageJobPO.builder().documentId(documentId).topic(topic).status("PENDING").retryCount(0)
                                      .build();
        messageJobDao.insert(po);
        log.debug("[消息仓储] 记录 PENDING: documentId={}", documentId);
    }

    @Override
    public void updateStatus(String documentId, String status, String errorMsg) {
        LambdaUpdateWrapper<MessageJobPO> wrapper = new LambdaUpdateWrapper<MessageJobPO>().eq(
                MessageJobPO::getDocumentId,
                documentId
        ).set(MessageJobPO::getStatus, status);
        if (errorMsg != null) {
            wrapper.set(MessageJobPO::getErrorMsg, errorMsg);
        }
        if ("FAILED".equals(status)) {
            wrapper.setSql("retry_count = retry_count + 1");
        }
        messageJobDao.update(null, wrapper);
        log.debug("[消息仓储] 更新状态: documentId={}, status={}", documentId, status);
    }

    @Override
    public String getStatus(String documentId) {
        MessageJobPO po = messageJobDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MessageJobPO>().eq(
                        MessageJobPO::getDocumentId,
                        documentId
                ).orderByDesc(MessageJobPO::getCreatedAt).last("LIMIT 1"));
        return po != null ? po.getStatus() : "NOT_FOUND";
    }

    @Override
    public List<String> findPendingOrFailed(int maxRetries) {
        List<MessageJobPO> poList = messageJobDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MessageJobPO>().and(
                        w -> w.eq(MessageJobPO::getStatus, "PENDING")
                              .or(w2 -> w2.eq(MessageJobPO::getStatus, "FAILED")
                                          .lt(MessageJobPO::getRetryCount, maxRetries))));
        return poList.stream().map(MessageJobPO::getDocumentId).toList();
    }

}
