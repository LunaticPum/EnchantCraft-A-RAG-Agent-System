-- V2.1: 创建 source_document 表（文档仓库）
-- 用于存储用户上传的 Markdown 文档原文

CREATE TABLE IF NOT EXISTS source_document (
    id          VARCHAR(36)  PRIMARY KEY COMMENT 'UUID',
    file_name   VARCHAR(255) NOT NULL COMMENT '上传时的原始文件名',
    file_type   VARCHAR(20)  NOT NULL DEFAULT 'MARKDOWN' COMMENT '文件类型，当前仅支持 MARKDOWN',
    raw_content MEDIUMTEXT   NOT NULL COMMENT '文档完整原始文本内容',
    ref_count   INT          NOT NULL DEFAULT 0 COMMENT '被 QA 集引用次数',
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常，1=已删除',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档仓库表';
