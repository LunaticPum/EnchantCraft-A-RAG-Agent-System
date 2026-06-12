package cn.pumluda.domain.document.service.rag.recall;

import cn.pumluda.domain.document.adapter.repository.IFullTextSearchRepository;
import cn.pumluda.domain.document.model.valobj.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 关键词检索实现——PostgreSQL zhparser 中文分词 + ts_rank 全文检索（V3.3 替换 MySQL LIKE）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordRetrieverImpl implements IKeywordRetriever {

    private final IFullTextSearchRepository fullTextSearchRepository;

    @Override
    public List<SearchResult> search(String keyword, int topK) {
        log.info("[关键词检索] PostgreSQL 全文检索: keyword={}, topK={}", keyword, topK);

        List<SearchResult> results = fullTextSearchRepository.searchByFullText(keyword, topK);
        log.info("[关键词检索] 命中 {} 条", results.size());
        return results;
    }

}
