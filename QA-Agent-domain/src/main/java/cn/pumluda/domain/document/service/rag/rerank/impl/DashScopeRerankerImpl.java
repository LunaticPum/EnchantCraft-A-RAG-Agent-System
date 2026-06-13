package cn.pumluda.domain.document.service.rag.rerank.impl;

import cn.pumluda.domain.document.model.valobj.SearchResult;
import cn.pumluda.domain.document.service.rag.rerank.IReranker;
import com.alibaba.dashscope.rerank.TextReRank;
import com.alibaba.dashscope.rerank.TextReRankOutput;
import com.alibaba.dashscope.rerank.TextReRankParam;
import com.alibaba.dashscope.rerank.TextReRankResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: DashScope gte-rerank 重排序实现——将查询词与每条候选文本成对送入模型精细打分
 */
@Slf4j
@Service
public class DashScopeRerankerImpl implements IReranker {

    /**
     * DashScope Rerank API 单次请求最多支持 20 条文档
     */
    private static final int RERANK_LIMIT = 20;

    private final String apiKey;
    private final String rerankModel;

    public DashScopeRerankerImpl(
            @Value("${dashScope.api-key}") String apiKey,
            @Value("${dashScope.rerank-model}") String rerankModel) {
        this.apiKey = apiKey;
        this.rerankModel = rerankModel;
    }

    @Override
    public List<SearchResult> rerank(String query, List<SearchResult> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        log.info("[Rerank] 开始: query={}, candidates={}", query, candidates.size());

        // 超过 RERANK_LIMIT 时截断（保留靠前的候选）
        List<SearchResult> topCandidates = candidates.size() > RERANK_LIMIT
                ? new ArrayList<>(candidates.subList(0, RERANK_LIMIT))
                : new ArrayList<>(candidates);

        try {
            // 1. 提取候选文本列表
            List<String> documents = topCandidates.stream()
                                                  .map(SearchResult::getContent)
                                                  .toList();

            // 2. 构造 Rerank 请求：query + documents
            TextReRankParam param = TextReRankParam.builder()
                                                   .apiKey(apiKey)
                                                   .model(rerankModel)
                                                   .query(query)
                                                   .documents(documents)
                                                   .build();

            // 3. 调用 DashScope Rerank API
            TextReRankResult result = new TextReRank().call(param);
            List<TextReRankOutput.Result> rerankResults = result.getOutput().getResults();

            // 4. 用 Rerank 返回的 relevanceScore 替换原有 score，并重新排序
            if (rerankResults != null) {
                for (TextReRankOutput.Result r : rerankResults) {
                    if (r.getIndex() < topCandidates.size()) {
                        // relevanceScore 是 Double，SearchResult.score 也是 Double
                        topCandidates.get(r.getIndex()).setScore(r.getRelevanceScore());
                    }
                }
                topCandidates.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());
            }

            log.info("[Rerank] 完成: {} 条候选 → {} 条重排", candidates.size(), topCandidates.size());
            return topCandidates;

        } catch (Exception e) {
            // 容错降级
            log.warn("[Rerank] 调用失败，回退原始排序: {}", e.getMessage());
            return candidates;
        }
    }

}
