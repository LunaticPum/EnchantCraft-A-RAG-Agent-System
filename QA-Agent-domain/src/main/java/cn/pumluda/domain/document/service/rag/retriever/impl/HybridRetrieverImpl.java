package cn.pumluda.domain.document.service.rag.retriever.impl;

import cn.pumluda.domain.document.model.valobj.SearchResult;
import cn.pumluda.domain.document.service.rag.rerank.IReranker;
import cn.pumluda.domain.document.service.rag.retriever.IHybridRetriever;
import cn.pumluda.domain.document.service.rag.retriever.IKeywordRetriever;
import cn.pumluda.domain.document.service.rag.retriever.ISemanticRetriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Project: QA-Agent-Pumluda
 * Description: 混合检索实现——语义 + 关键词双路召回 → RRF 融合 → Rerank 精排
 */
@Slf4j
@Service
public class HybridRetrieverImpl implements IHybridRetriever {

    /**
     * RRF 常数 k，用于平滑排名差异
     */
    private static final double RRF_K = 60.0;

    private final ISemanticRetriever semanticRetriever;
    private final IKeywordRetriever keywordRetriever;
    private final IReranker reranker;

    public HybridRetrieverImpl(ISemanticRetriever semanticRetriever, IKeywordRetriever keywordRetriever, IReranker reranker) {
        this.semanticRetriever = semanticRetriever;
        this.keywordRetriever = keywordRetriever;
        this.reranker = reranker;
    }

    /**
     * 混合检索 + Rerank 精排
     *
     * @param query  用户查询文本
     * @param topK   最终返回条数
     * @param rerank 是否启用 Rerank 精排
     * @return 排序后的检索结果
     */
    public List<SearchResult> search(String query, int topK, boolean rerank) {
        log.info("[混合检索] 开始: query={}, topK={}, rerank={}", query, topK, rerank);

        int recallSize = rerank ? topK * 2 : topK;  // 先初排 topK * 2条结果作为候选集，然后再 rerank 精排

        // 1. 两路并行召回，各取 recallSize 条
        List<SearchResult> semanticResults = semanticRetriever.search(query, recallSize);
        List<SearchResult> keywordResults = keywordRetriever.search(query, recallSize);

        // 2. RRF 融合：以 chunkId 为 key，综合两路排名重新打分
        Map<String, SearchResult> chunkMap = new LinkedHashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        accumulateRrf(semanticResults, chunkMap, rrfScores, "语义");
        accumulateRrf(keywordResults, chunkMap, rrfScores, "关键词");

        // 3. 按 RRF 分数降序排列
        List<SearchResult> merged = chunkMap.values().stream().peek(
                r -> r.setScore(rrfScores.getOrDefault(r.getChunkId(), 0.0))).sorted(
                Comparator.comparingDouble(SearchResult::getScore).reversed()).toList();

        log.info(
                "[混合检索] 语义{}条 + 关键词{}条 → RRF融合{}条", semanticResults.size(), keywordResults.size(),
                merged.size()
        );

        // 4. Rerank 精排（可选）
        if (rerank && !merged.isEmpty()) {
            int rerankInputSize = Math.min(merged.size(), recallSize);
            List<SearchResult> toRerank = merged.subList(0, rerankInputSize);
            List<SearchResult> reranked = reranker.rerank(query, toRerank);
            // Rerank 结果截断 topK
            List<SearchResult> result = reranked.size() > topK ? reranked.subList(0, topK) : reranked;
            log.info("[混合检索] Rerank 精排完成: 最终返回 {} 条", result.size());
            return result;
        }

        // 不启用 Rerank：直接截断 topK
        List<SearchResult> result = merged.size() > topK ? merged.subList(0, topK) : merged;
        log.info("[混合检索] 跳过 Rerank: 最终返回 {} 条", result.size());
        return result;
    }

    /**
     * 将一路检索结果累加到 RRF 分数表
     * RRF 公式：score += 1 / (k + rank)，rank 从 1 开始
     */
    private void accumulateRrf(List<SearchResult> results, Map<String, SearchResult> chunkMap, Map<String, Double> rrfScores, String source) {
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            String chunkId = r.getChunkId();
            double rrf = 1.0 / (RRF_K + i + 1);  // rank = i + 1
            rrfScores.merge(chunkId, rrf, Double::sum);
            // 保留第一次出现的 SearchResult（语义路的结果优先）
            chunkMap.putIfAbsent(chunkId, r);
            log.debug(
                    "[RRF] 来源={}, rank={}, chunkId={}, titlePath={}, rrf={}", source, i + 1, chunkId,
                    r.getTitlePath(), rrf
            );
        }
    }

}
