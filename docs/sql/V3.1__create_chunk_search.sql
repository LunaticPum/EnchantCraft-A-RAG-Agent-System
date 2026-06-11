-- V3.1: PostgreSQL chunk_search 表（RAG 检索引擎核心表）
-- 用途：存储分块向量 + 中文全文索引，承载所有语义/关键词/混合检索
-- 该表从 MySQL document_chunk 重建，不构成独立数据源

-- 启用 pgvector 向量扩展
CREATE EXTENSION IF NOT EXISTS vector;
-- 启用中文分词扩展
CREATE EXTENSION IF NOT EXISTS zhparser;
-- 将 zhparser 设为默认中文分词配置
CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l,j WITH simple;

-- RAG 检索引擎核心表
CREATE TABLE IF NOT EXISTS chunk_search (
    chunk_id     VARCHAR(36) PRIMARY KEY,          -- 关联 MySQL document_chunk.id
    embedding    vector(1024),                     -- 文本向量（1024维），DashScope text-embedding-v4
    title_path   TEXT,                             -- 标题路径，检索命中后返回标识
    module_tags  JSONB,                            -- 模块标签，用于按标签过滤检索范围
    content_tsv  tsvector,                         -- 中文分词全文索引
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- HNSW 向量索引：加速语义相似度搜索（余弦距离）
CREATE INDEX IF NOT EXISTS idx_chunk_search_embedding
    ON chunk_search USING hnsw (embedding vector_cosine_ops);

-- GIN 全文索引：加速中文关键词检索
CREATE INDEX IF NOT EXISTS idx_chunk_search_content_tsv
    ON chunk_search USING GIN (content_tsv);

-- 模块标签索引：加速按标签过滤的查询
CREATE INDEX IF NOT EXISTS idx_chunk_search_module_tags
    ON chunk_search USING GIN (module_tags);
