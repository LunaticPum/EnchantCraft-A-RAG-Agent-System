-- V7.1: Bagu Skill 问答集表
-- qa_set: 生成的问答集
-- qa_item: 单个问答题目
-- qa_set_document_ref: 问答集与源文档的多对多关联

CREATE TABLE IF NOT EXISTS qa_set (
    id          VARCHAR(36)  PRIMARY KEY COMMENT 'UUID',
    title       VARCHAR(255) NOT NULL COMMENT '问答集标题',
    description TEXT         COMMENT '问答集描述',
    item_count  INT          NOT NULL DEFAULT 0 COMMENT '题目数量',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bagu Skill 问答集';

CREATE TABLE IF NOT EXISTS qa_item (
    id          VARCHAR(36)  PRIMARY KEY COMMENT 'UUID',
    set_id      VARCHAR(36)  NOT NULL COMMENT '关联 qa_set.id',
    question    TEXT         NOT NULL COMMENT '题目',
    answer      MEDIUMTEXT   NOT NULL COMMENT '参考答案',
    difficulty  VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM' COMMENT '难度: EASY/MEDIUM/HARD',
    tags        VARCHAR(500) COMMENT '标签JSON数组',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_set_id (set_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bagu Skill 问答题目';

CREATE TABLE IF NOT EXISTS qa_set_document_ref (
    id          VARCHAR(36)  PRIMARY KEY COMMENT 'UUID',
    set_id      VARCHAR(36)  NOT NULL COMMENT '关联 qa_set.id',
    document_id VARCHAR(36)  NOT NULL COMMENT '关联 source_document.id',
    INDEX idx_set_id (set_id),
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答集-文档关联表';
