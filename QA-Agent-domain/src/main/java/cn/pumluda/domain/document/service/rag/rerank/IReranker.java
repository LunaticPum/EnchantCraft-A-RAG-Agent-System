package cn.pumluda.domain.document.service.rag.rerank;

import cn.pumluda.domain.document.model.valobj.SearchResult;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 重排序接口——对候选检索结果进行精细语义重排
 */
public interface IReranker {

    /**
     * 对候选结果重排序
     *
     * @param query      原始查询文本
     * @param candidates 候选检索结果列表（最多 20 条）
     * @return 按相关性降序重排后的结果，数量与输入相同
     */
    List<SearchResult> rerank(String query, List<SearchResult> candidates);

}
