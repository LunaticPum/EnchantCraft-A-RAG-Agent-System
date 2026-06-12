package cn.pumluda.trigger.job;

import cn.pumluda.domain.document.adapter.repository.IMessageJobRepository;
import cn.pumluda.domain.document.service.IDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 消息兜底轮询——定时扫描 message_job 表中 PENDING/FAILED 消息，重新触发处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRetryJob {

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRIES = 3;
    /**
     * 扫描间隔：30 秒
     */
    private static final long POLL_INTERVAL_MS = 30_000L;

    private final IMessageJobRepository messageJobRepository;
    private final IDocumentService documentService;

    /**
     * 每 30 秒扫描一次 message_job 表，
     * 对 PENDING 或 FAILED（未达最大重试）的消息重新触发 Embedding
     */
    @Scheduled(fixedRate = POLL_INTERVAL_MS)
    public void retryPendingMessages() {
        List<String> pendingIds = messageJobRepository.findPendingOrFailed(MAX_RETRIES);
        if (pendingIds.isEmpty()) return;

        log.info("[消息兜底] 发现 {} 条待处理消息，开始重试", pendingIds.size());
        for (String documentId : pendingIds) {
            try {
                documentService.embedDocumentChunks(documentId);
                messageJobRepository.updateStatus(documentId, "COMPLETED", null);
                log.info("[消息兜底] 重试成功: documentId={}", documentId);
            } catch (Exception e) {
                log.warn("[消息兜底] 重试失败: documentId={}, error={}", documentId, e.getMessage());
                messageJobRepository.updateStatus(documentId, "FAILED", e.getMessage());
            }
        }
    }

}
