package cn.pumluda.domain.document.adapter.repository;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 消息任务仓储接口——追踪 Kafka 异步索引消息状态
 */
public interface IMessageJobRepository {

    /**
     * 插入一条 PENDING 状态的消息追踪记录
     *
     * @param documentId 文档 ID
     * @param topic      Kafka 主题
     */
    void savePending(String documentId, String topic);

    /**
     * 更新消息状态
     *
     * @param documentId 文档 ID
     * @param status     新状态：COMPLETED / FAILED
     * @param errorMsg   失败时的错误信息
     */
    void updateStatus(String documentId, String status, String errorMsg);

    /**
     * 查询文档的 Embedding 状态
     *
     * @param documentId 文档 ID
     * @return 状态：PENDING / COMPLETED / FAILED，未找到返回 "NOT_FOUND"
     */
    String getStatus(String documentId);

    /**
     * 查询所有 PENDING 或 FAILED（且重试次数 < maxRetries）的消息
     *
     * @param maxRetries 最大重试次数
     * @return 待处理的文档 ID 列表
     */
    List<String> findPendingOrFailed(int maxRetries);

}
