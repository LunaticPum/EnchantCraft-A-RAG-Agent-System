package cn.pumluda.trigger.consumer;

import cn.pumluda.domain.document.adapter.repository.IMessageJobRepository;
import cn.pumluda.domain.document.service.IDocumentService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Project: QA-Agent-Pumluda
 * Description: Kafka 异步索引消费者——消费 document.indexing 消息，执行 Embedding
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingConsumer {

    private final IDocumentService documentService;
    private final IMessageJobRepository messageJobRepository;

    @KafkaListener(topics = "document.indexing", groupId = "qa-agent-indexing")
    public void onMessage(String message) {
        log.info("[Kafka消费者] 收到消息: {}", message);
        JSONObject msg = JSON.parseObject(message);
        String type = msg.getString("type");
        String documentId = msg.getString("documentId");

        if (!"CHUNK_EMBED".equals(type)) {
            log.warn("[Kafka消费者] 未知消息类型: {}", type);
            return;
        }

        try {
            documentService.embedDocumentChunks(documentId);
            messageJobRepository.updateStatus(documentId, "COMPLETED", null);
            log.info("[Kafka消费者] Embedding 完成: documentId={}", documentId);
        } catch (Exception e) {
            log.error("[Kafka消费者] Embedding 失败: documentId={}, error={}", documentId, e.getMessage());
            messageJobRepository.updateStatus(documentId, "FAILED", e.getMessage());
        }
    }

}
