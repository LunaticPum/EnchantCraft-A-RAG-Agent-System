package cn.pumluda.infrastructure.adapter.producer;

import cn.pumluda.domain.document.adapter.producer.IIndexingMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Project: QA-Agent-Pumluda
 * Description: Kafka 异步索引消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingProducer implements IIndexingMessageProducer {

    private static final String TOPIC = "document.indexing";

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Override
    public void sendChunkEmbedMessage(String documentId, int chunkCount) {
        Map<String, Object> message = Map.of(
                "type", "CHUNK_EMBED",
                "documentId", documentId,
                "chunkCount", chunkCount,
                "timestamp", LocalDateTime.now().toString()
        );
        kafkaTemplate.send(TOPIC, documentId, message);
        log.info("[Kafka生产者] 发送 Embedding 消息: documentId={}, chunks={}", documentId, chunkCount);
    }

}
