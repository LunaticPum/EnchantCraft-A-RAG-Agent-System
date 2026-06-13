package cn.pumluda.domain.document.service.rag.retriever;

import cn.pumluda.domain.document.model.valobj.SearchResult;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: RAG 检索服务接口：三种实现。。。
 */
public interface IHybridRetriever {

    /**
     * 检索
     *
     * @param query 用户查询文本
     * @param topK  返回最相似的分块数量
     * @return 按相似度降序排列的检索结果
     */
    List<SearchResult> search(String query, int topK, boolean rerank);

}
