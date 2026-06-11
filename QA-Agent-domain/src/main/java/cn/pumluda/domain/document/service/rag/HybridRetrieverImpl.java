package cn.pumluda.domain.document.service.rag;

import cn.pumluda.domain.document.model.valobj.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Project: QA-Agent-Pumluda
 * Description: 混合检索实现——语义检索 + 关键词检索 → RRF（倒数排名融合）合并排序
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrieverImpl {

    /**
     * RRF 常数 k，用于平滑排名差异
     */
    private static final double RRF_K = 60.0;

    private final ISemanticRetriever semanticRetriever;
    private final IKeywordRetriever keywordRetriever;

    /**
     * 混合检索
     *
     * @param query 用户查询文本
     * @param topK  最终返回条数
     * @return RRF 融合排序后的检索结果
     */
    public List<SearchResult> search(String query, int topK) {
        log.info("[混合检索] 开始: query={}, topK={}", query, topK);

        // 1. 两路并行召回，各取 topK 条
        List<SearchResult> semanticResults = semanticRetriever.search(query, topK);
        List<SearchResult> keywordResults = keywordRetriever.search(query, topK);

        // 2. RRF 融合：以 chunkId 为 key，综合两路排名重新打分
        Map<String, SearchResult> chunkMap = new LinkedHashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        // 语义路：排名越靠前 RRF 分数越高
        accumulateRrf(semanticResults, chunkMap, rrfScores, "语义");
        // 关键词路：同样贡献 RRF 分数
        accumulateRrf(keywordResults, chunkMap, rrfScores, "关键词");

        // 3. 按 RRF 分数降序排列，截取 topK
        List<SearchResult> merged = chunkMap.values().stream()
                                            .peek(r -> r.setScore(rrfScores.getOrDefault(r.getChunkId(), 0.0)))
                                            .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                                            .limit(topK)
                                            .toList();

        log.info(
                "[混合检索] 语义检索命中 {} 条 + 关键词检索命中 {} 条 → 融合后保留 {} 条，最终返回 {} 条",
                semanticResults.size(), keywordResults.size(), chunkMap.size(), merged.size()
        );
        return merged;
    }

    /**
     * 将一路检索结果累加到 RRF 分数表
     * RRF 公式：score += 1 / (k + rank)，rank 从 1 开始
     */
    private void accumulateRrf(List<SearchResult> results,
                               Map<String, SearchResult> chunkMap,
                               Map<String, Double> rrfScores,
                               String source) {
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            String chunkId = r.getChunkId();
            double rrf = 1.0 / (RRF_K + i + 1);  // rank = i + 1
            rrfScores.merge(chunkId, rrf, Double::sum);
            // 保留第一次出现的 SearchResult（语义路的结果优先）
            chunkMap.putIfAbsent(chunkId, r);
            log.debug(
                    "[RRF] 来源={}, rank={}, chunkId={}, titlePath={}, rrf={}",
                    source, i + 1, chunkId, r.getTitlePath(), rrf
            );
        }
    }

}
