-- V3.4: message_job 表（Kafka 异步索引消息追踪）
-- 用途：追踪文档 Embedding 消息的发送和消费状态，支持失败重试

CREATE TABLE IF NOT EXISTS message_job (
    job_id      VARCHAR(36)  PRIMARY KEY COMMENT '消息ID（UUID）',
    document_id VARCHAR(36)  NOT NULL COMMENT '关联 source_document.id',
    topic       VARCHAR(100) NOT NULL COMMENT 'Kafka 主题名',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/COMPLETED/FAILED',
    retry_count INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_msg   TEXT         COMMENT '最后一次失败的错误信息',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Kafka 消息任务追踪表';
