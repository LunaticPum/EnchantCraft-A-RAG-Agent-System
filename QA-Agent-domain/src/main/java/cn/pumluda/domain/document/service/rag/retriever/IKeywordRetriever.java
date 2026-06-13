package cn.pumluda.domain.document.service.rag.retriever;

import cn.pumluda.domain.document.model.valobj.SearchResult;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 关键词检索接口——通过 MySQL LIKE 在分块文本中精确匹配关键词
 */
public interface IKeywordRetriever {

    /**
     * 关键词检索
     *
     * @param keyword 搜索关键词
     * @param topK    返回条数上限
     * @return 按命中位置排序的检索结果，score 统一为 1.0（关键词匹配无相似度）
     */
    List<SearchResult> search(String keyword, int topK);

}
