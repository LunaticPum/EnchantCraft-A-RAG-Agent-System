package cn.pumluda.domain.document.service.rag.recall;

import cn.pumluda.domain.document.model.valobj.SearchResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 语义检索实现——将查询文本向量化后在 EmbeddingStore 中搜索最相似的分块
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticRetrieverImpl implements ISemanticRetriever {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public List<SearchResult> search(String query, int topK) {
        log.info("[语义检索] 开始检索: query={}, topK={}", query, topK);

        // 1. 将用户的查询文本（提问）向量化
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // 2. 在向量存储中查找最相似的 topK 个分块（topK 召回检索）
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                                                                     .queryEmbedding(queryEmbedding)
                                                                     .maxResults(topK)
                                                                     .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();

        log.info("[语义检索] 命中 {} 条结果", matches.size());

        // 3. 将 EmbeddingMatch 转为业务层 SearchResult
        return matches.stream()
                      .map(match -> {
                          TextSegment segment = match.embedded();
                          return SearchResult.builder()
                                             .chunkId(segment.metadata().getString("chunkId"))
                                             .documentId(segment.metadata().getString("documentId"))
                                             .titlePath(segment.metadata().getString("titlePath"))
                                             .content(segment.text())
                                             .score(match.score())
                                             .build();
                      })
                      .toList();
    }

}
