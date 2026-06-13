package cn.pumluda.domain.document.adapter.repository;

import cn.pumluda.domain.document.model.valobj.SearchResult;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: PostgreSQL 全文检索仓储接口——zhparser 中文分词 + ts_rank 相关性排序
 */
public interface IFullTextSearchRepository {

    /**
     * 中文分词全文检索
     *
     * @param queryText 用户查询文本
     * @param limit     返回条数上限
     * @return 按 ts_rank 相关性降序的结果
     */
    List<SearchResult> searchByFullText(String queryText, int limit);

    void deleteByDocumentId(String documentId);

    long count();

}
