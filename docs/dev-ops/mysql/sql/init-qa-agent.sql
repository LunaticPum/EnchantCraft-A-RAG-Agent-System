-- QA Agent 数据库初始化（Docker MySQL 首次启动时自动执行）
-- 仅当 /var/lib/mysql 为空（首次启动）时运行
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `qa_agent` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `qa_agent`;

-- =====================
-- V2.1: source_document 文档仓库表
-- =====================
CREATE TABLE IF NOT EXISTS source_document (
    id              VARCHAR(36)  PRIMARY KEY COMMENT 'UUID',
    file_name       VARCHAR(255) NOT NULL COMMENT '上传时的原始文件名',
    file_type       VARCHAR(20)  NOT NULL DEFAULT 'MARKDOWN' COMMENT '文件类型',
    directory_path  VARCHAR(500) NOT NULL DEFAULT '' COMMENT '文件在原始目录中的相对路径',
    raw_content     MEDIUMTEXT   NOT NULL COMMENT '文档完整原始文本内容',
    content_md5     VARCHAR(32)  NOT NULL DEFAULT '' COMMENT 'MD5 摘要，用于上传去重校验',
    ref_count       INT          NOT NULL DEFAULT 0 COMMENT '被 QA 集引用次数',
    is_deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0=正常，1=已删除',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_content_md5 (content_md5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档仓库表';

-- =====================
-- V2.2: document_chunk 文档分块表
-- =====================
CREATE TABLE IF NOT EXISTS document_chunk (
    id           VARCHAR(36)  PRIMARY KEY COMMENT 'UUID',
    document_id  VARCHAR(36)  NOT NULL COMMENT '所属文档ID',
    chunk_index  INT          NOT NULL COMMENT '块在文档中的序号（从1开始）',
    title_path   VARCHAR(500) COMMENT '标题路径',
    content      MEDIUMTEXT   NOT NULL COMMENT '分块文本内容',
    module_tags  VARCHAR(500) COMMENT '模块标签JSON数组',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分块表';

-- =====================
-- V3.4: message_job Kafka消息追踪表
-- =====================
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

-- =====================
-- V6.1: user_account 用户账户表
-- =====================
CREATE TABLE IF NOT EXISTS user_account (
    id              VARCHAR(36)  PRIMARY KEY COMMENT 'UUID',
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password        VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
    email           VARCHAR(100) COMMENT '邮箱',
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN / USER',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=正常，0=禁用',
    search_count    INT          NOT NULL DEFAULT 0 COMMENT '今日检索次数',
    chat_count      INT          NOT NULL DEFAULT 0 COMMENT '今日对话次数',
    last_reset      DATE         COMMENT '配额重置日期',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户表';

SET FOREIGN_KEY_CHECKS = 1;
