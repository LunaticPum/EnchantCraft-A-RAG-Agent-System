package cn.pumluda.infrastructure.adapter.pgvector;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Project: QA-Agent-Pumluda
 * Description: 自定义 pgvector EmbeddingStore —— 基于 JdbcTemplate 直接操作 PostgreSQL chunk_search 表
 * <p>
 * 手写 SQL + pgvector 运算符（<=> 余弦距离），不引入额外第三方 pgvector 包
 */
public class PgVectorStore implements EmbeddingStore<TextSegment> {

    private final JdbcTemplate jdbc;

    public PgVectorStore(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    // ==================== 存储 ====================

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        jdbc.update(
                "INSERT INTO chunk_search (chunk_id, embedding, created_at, updated_at) VALUES (?, ?::vector, NOW(), NOW())",
                id, vectorToString(embedding.vector())
        );
    }

    @Override
    public String add(Embedding embedding, TextSegment segment) {
        String id = UUID.randomUUID().toString();
        addAll(List.of(embedding), List.of(segment));
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return embeddings.stream().map(this::add).toList();
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        if (embeddings.isEmpty()) return List.of();

        String sql = """
                     INSERT INTO chunk_search (chunk_id, document_id, embedding, title_path, content, module_tags, created_at, updated_at)
                     VALUES (?, ?, ?::vector, ?, ?, ?::jsonb, NOW(), NOW())
                     """;

        List<Object[]> batchArgs = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String chunkId = segments.get(i).metadata().getString("chunkId");
            if (chunkId == null) chunkId = UUID.randomUUID().toString();
            String documentId = segments.get(i).metadata().getString("documentId");
            String titlePath = segments.get(i).metadata().getString("titlePath");
            String content = segments.get(i).text();
            String moduleTagsStr = segments.get(i).metadata().getString("moduleTags");
            String moduleTagsJson = toJsonbArray(moduleTagsStr);

            batchArgs.add(new Object[]{chunkId,
                    documentId != null ? documentId : "", vectorToString(embeddings.get(i).vector()),
                    titlePath != null ? titlePath : "", content != null ? content : "", moduleTagsJson});
            ids.add(chunkId);
        }
        jdbc.batchUpdate(sql, batchArgs);
        return ids;
    }

    // ==================== 检索 ====================

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
                sql, (rs, rowNum) -> {
                    String chunkId = rs.getString("chunk_id");
                    String documentId = rs.getString("document_id");
                    String titlePath = rs.getString("title_path");
                    String content = rs.getString("content");
                    String moduleTags = rs.getString("module_tags");
                    double score = rs.getDouble("score");

                    TextSegment segment = TextSegment.from(
                            content != null ? content : "", new dev.langchain4j.data.document.Metadata()
                                    .put("chunkId", chunkId)
                                    .put("documentId", documentId != null ? documentId : "")
                                    .put("titlePath", titlePath != null ? titlePath : "")
                                    .put("moduleTags", moduleTags != null ? moduleTags : "")
                    );

                    return new EmbeddingMatch<>(score, chunkId, null, segment);
                }, vectorToString(queryVector), minScore, vectorToString(queryVector), maxResults
        );

        return new EmbeddingSearchResult<>(matches);
    }

    // ==================== 删除 ====================

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
    public void removeAll(Filter filter) {
        throw new UnsupportedOperationException("Filter-based removal not implemented yet");
    }

    // ==================== 工具方法 ====================

    /**
     * float[] → pgvector 兼容的字符串格式：[0.1,0.2,0.3,...]
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
     * 逗号分隔字符串 → PostgreSQL jsonb 数组格式：["a","b","c"]
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

}
