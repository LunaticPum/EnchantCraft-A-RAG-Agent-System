-- V2.3.1: source_document 表新增 content_md5 字段，用于文档上传幂等校验（MD5查重）

ALTER TABLE source_document
    ADD COLUMN content_md5 VARCHAR(32) NOT NULL DEFAULT '' COMMENT '文档原始内容的 MD5 摘要，用于上传去重校验',
    ADD INDEX idx_content_md5 (content_md5);
