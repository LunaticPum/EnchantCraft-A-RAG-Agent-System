-- V5.2: source_document 新增 directory_path 字段，支持目录扫描上传时记录文件层级

ALTER TABLE source_document
    ADD COLUMN directory_path VARCHAR(500) NOT NULL DEFAULT '' COMMENT '文件在原始目录中的相对路径，如 MySQL/存储引擎';
