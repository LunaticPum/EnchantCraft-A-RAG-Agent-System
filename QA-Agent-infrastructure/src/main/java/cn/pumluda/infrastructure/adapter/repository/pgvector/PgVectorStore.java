package cn.pumluda.infrastructure.adapter.repository.pgvector;

import cn.pumluda.domain.document.adapter.repository.IFullTextSearchRepository;
import cn.pumluda.domain.document.model.valobj.SearchResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * PostgreSQL pgvector 向量存储 —— 基于 JdbcTemplate 直接操作 chunk_search 表
 * <p>
 * 一表双引擎：
 * <ul>
 *   <li>语义检索：pgvector 余弦距离 {@code embedding <=> ?::vector}</li>
 *   <li>关键词检索：zhparser 中文分词 + {@code ts_rank + tsquery + @@}</li>
 * </ul>
 * 数据从 MySQL document_chunk 全量重建，不构成独立数据源
 */
@Slf4j
public class PgVectorStore implements EmbeddingStore<TextSegment>, IFullTextSearchRepository {

    private final JdbcTemplate jdbc;

    public PgVectorStore(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    // ════════════════════════════════════════════════════════════════
    // EmbeddingStore 接口 —— 存储
    // ════════════════════════════════════════════════════════════════

    /**
     * 单条写入（生成随机 ID）
     */
    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        add(id, embedding);
        return id;
    }

    /**
     * 单条写入（指定 ID）—— 仅基础字段
     */
    @Override
    public void add(String id, Embedding embedding) {
        jdbc.update(
                "INSERT INTO chunk_search (chunk_id, embedding, created_at, updated_at) " +
                "VALUES (?, ?::vector, NOW(), NOW())",
                id, vectorToString(embedding.vector())
        );
    }

    /**
     * 单条写入（带 TextSegment）
     */
    @Override
    public String add(Embedding embedding, TextSegment segment) {
        addAll(List.of(embedding), List.of(segment));
        return UUID.randomUUID().toString();
    }

    /**
     * 批量写入（无 TextSegment）
     */
    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return embeddings.stream().map(this::add).toList();
    }

    /**
     * 批量写入（带 TextSegment）—— 核心方法
     * <p>
     * 从 metadata 提取 chunkId / documentId / titlePath / moduleTags，
     * 写入 chunk_search 表，同时生成 zhparser 分词向量（content_tsv）。
     * <p>
     * {@code ON CONFLICT (chunk_id) DO UPDATE} 保证 Re-embed 时自动覆写旧数据。
     */
    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        if (embeddings.isEmpty()) return List.of();
        log.info("[PgVector存储] 批量写入: {} 条", embeddings.size());

        String sql = """
                     INSERT INTO chunk_search (chunk_id, document_id, embedding, title_path,
                                               content, module_tags, content_tsv, created_at, updated_at)
                     VALUES (?, ?, ?::vector, ?, ?, ?::jsonb, to_tsvector('chinese', ?), NOW(), NOW())
                     ON CONFLICT (chunk_id) DO UPDATE SET
                         embedding   = EXCLUDED.embedding,
                         content_tsv = EXCLUDED.content_tsv,
                         content     = EXCLUDED.content,
                         module_tags = EXCLUDED.module_tags,
                         updated_at  = NOW()
                     """;

        List<Object[]> batchArgs = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            TextSegment seg = segments.get(i);
            String chunkId = seg.metadata().getString("chunkId");
            if (chunkId == null) chunkId = UUID.randomUUID().toString();

            batchArgs.add(new Object[]{
                    chunkId,
                    seg.metadata().getString("documentId"),
                    vectorToString(embeddings.get(i).vector()),
                    seg.metadata().getString("titlePath"),
                    seg.text(),
                    toJsonbArray(seg.metadata().getString("moduleTags")),
                    seg.text()   // content_tsv 的原始文本
            });
            ids.add(chunkId);
        }
        jdbc.batchUpdate(sql, batchArgs);
        return ids;
    }

    // ════════════════════════════════════════════════════════════════
    // EmbeddingStore 接口 —— 语义检索
    // ════════════════════════════════════════════════════════════════

    /**
     * 语义检索 —— pgvector 余弦相似度
     * <p>
     * {@code 1 - (embedding <=> queryVec)} 将余弦距离（0~2）转为相似度（-1~1，越大越相似）。
     * HNSW 索引自动加速，无需全表遍历。
     */
    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        double minScore = request.minScore();
        int maxResults = request.maxResults();
        float[] queryVector = request.queryEmbedding().vector();

        String sql = """
                     SELECT chunk_id, document_id, title_path, content, module_tags,
                            1 - (embedding <=> ?::vector) AS score
                     FROM chunk_search
                     WHERE 1 - (embedding <=> ?::vector) >= ?
                     ORDER BY embedding <=> ?::vector
                     LIMIT ?
                     """;

        List<EmbeddingMatch<TextSegment>> matches = jdbc.query(
                sql,
                (rs, rowNum) -> {
                    TextSegment segment = TextSegment.from(
                            rs.getString("content"),
                            new dev.langchain4j.data.document.Metadata()
                                    .put("chunkId", rs.getString("chunk_id"))
                                    .put("documentId", rs.getString("document_id"))
                                    .put("titlePath", rs.getString("title_path"))
                                    .put("moduleTags", rs.getString("module_tags"))
                    );
                    return new EmbeddingMatch<>(
                            rs.getDouble("score"),
                            rs.getString("chunk_id"), null, segment
                    );
                },
                // SQL 中 5 个占位符：SELECT向量, WHERE向量, minScore, ORDER BY向量, LIMIT
                vectorToString(queryVector), vectorToString(queryVector), minScore,
                vectorToString(queryVector), maxResults
        );

        return new EmbeddingSearchResult<>(matches);
    }

    // ════════════════════════════════════════════════════════════════
    // EmbeddingStore 接口 —— 删除
    // ════════════════════════════════════════════════════════════════

    @Override
    public void remove(String id) {
        jdbc.update("DELETE FROM chunk_search WHERE chunk_id = ?", id);
    }

    @Override
    public void removeAll() {
        jdbc.update("DELETE FROM chunk_search");
    }

    @Override
    public void removeAll(Collection<String> ids) {
        if (ids.isEmpty()) return;
        ids.forEach(this::remove);
    }

    @Override
    public void deleteByDocumentId(String documentId) {
        int deleted = jdbc.update("DELETE FROM chunk_search WHERE document_id = ?", documentId);
        log.info("[PgVector存储] 删除向量: documentId={}, deleted={}", documentId, deleted);
    }

    @Override
    public long count() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM chunk_search", Long.class);
        return c != null ? c : 0;
    }

    /**
     * 按条件过滤删除 —— 暂未实现
     */
    @Override
    public void removeAll(Filter filter) {
        throw new UnsupportedOperationException("Filter-based removal not implemented");
    }

    // ════════════════════════════════════════════════════════════════
    // IFullTextSearchRepository 接口 —— 中文分词全文检索（V3.3）
    // ════════════════════════════════════════════════════════════════

    /**
     * 中文分词全文检索 —— zhparser + ts_rank + GIN 索引
     * <p>
     * 将用户查询文本按空格拆词后用 {@code &}（AND）连接，
     * 与 content_tsv 做全文匹配并按相关性排序。
     */
    @Override
    public List<SearchResult> searchByFullText(String queryText, int limit) {
        // 空格分词 → AND 连接：InnoDB 存储引擎 → InnoDB & 存储 & 引擎
        String tsquery = toTsquery(queryText);
        log.info("[PgVector全文检索] queryText={}, tsquery={}, limit={}", queryText, tsquery, limit);

        String sql = """
                     SELECT chunk_id, document_id, title_path, content, module_tags,
                            ts_rank(content_tsv, to_tsquery('chinese', ?)) AS score
                     FROM chunk_search
                     WHERE content_tsv @@ to_tsquery('chinese', ?)
                     ORDER BY score DESC
                     LIMIT ?
                     """;

        List<SearchResult> result = jdbc.query(
                sql,
                (rs, rowNum) -> SearchResult.builder()
                                            .chunkId(rs.getString("chunk_id"))
                                            .documentId(rs.getString("document_id"))
                                            .titlePath(rs.getString("title_path"))
                                            .content(rs.getString("content"))
                                            .score(rs.getDouble("score"))
                                            .build(),
                tsquery, tsquery, limit
        );
        log.info("[PgVector全文检索] 命中 {} 条", result.size());
        return result;
    }

    // ════════════════════════════════════════════════════════════════
    // 内部工具方法
    // ════════════════════════════════════════════════════════════════

    /**
     * float[] → pgvector 字符串格式
     * <p>
     * JDBC 不识别 pgvector 类型，通过 {@code ?::vector} 强制转换。
     * 需预先将向量转为 {@code [0.1, 0.2, 0.3, ...]} 格式的字符串传入。
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 逗号分隔字符串 → PostgreSQL jsonb 数组
     * <p>
     * 例：{@code "java,集合" → '["java","集合"]'}
     */
    private String toJsonbArray(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) return "[]";
        String[] parts = commaSeparated.split(",");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(parts[i].trim()).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 用户查询文本 → PostgreSQL tsquery AND 格式
     * <p>
     * 例：{@code "InnoDB 存储引擎" → "InnoDB & 存储 & 引擎"}
     */
    private String toTsquery(String queryText) {
        if (queryText == null || queryText.isBlank()) return "";
        String[] words = queryText.trim().split("\\s+");
        return String.join(" & ", words);
    }

}
