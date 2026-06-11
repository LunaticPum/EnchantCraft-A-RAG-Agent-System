-- V2.2: 创建 document_chunk 表（文档分块）
-- 将 Markdown 文档按标题层级切分为语义块，每条记录为一个独立的检索单元

CREATE TABLE IF NOT EXISTS document_chunk (
    id           VARCHAR(36)  PRIMARY KEY COMMENT 'UUID',
    document_id  VARCHAR(36)  NOT NULL COMMENT '所属文档ID，关联 source_document.id',
    chunk_index  INT          NOT NULL COMMENT '块在文档中的序号（从1开始）',
    title_path   VARCHAR(500) COMMENT '标题路径，如 "Java基础 > 集合 > HashMap"',
    content      MEDIUMTEXT   NOT NULL COMMENT '分块文本内容',
    module_tags  VARCHAR(500) COMMENT '模块标签JSON数组，如 ["java","集合"]',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分块表';
