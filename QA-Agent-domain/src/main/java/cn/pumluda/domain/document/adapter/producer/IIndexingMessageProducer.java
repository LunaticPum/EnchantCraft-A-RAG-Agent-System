package cn.pumluda.domain.document.adapter.producer;

/**
 * Project: QA-Agent-Pumluda
 * Description: 异步索引消息生产者——发送 Kafka 消息，通知 Consumer 执行 Embedding
 */
public interface IIndexingMessageProducer {

    /**
     * 发送文档分块 Embedding 消息
     *
     * @param documentId 文档 ID
     * @param chunkCount 分块总数
     */
    void sendChunkEmbedMessage(String documentId, int chunkCount);

}
