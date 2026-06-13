-- V6.1: user_account 表 —— 用户认证与权限管理

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
