-- V3.2: chunk_search 表补充检索需要的业务字段
-- 原表只有 chunk_id + embedding + content_tsv + title_path + module_tags
-- 补充 document_id（关联文档）和 content（原始文本，用于检索返回）

ALTER TABLE chunk_search
    ADD COLUMN IF NOT EXISTS document_id VARCHAR(36),    -- 关联 source_document.id
    ADD COLUMN IF NOT EXISTS content      TEXT;           -- 分块原始文本（检索时返回）
