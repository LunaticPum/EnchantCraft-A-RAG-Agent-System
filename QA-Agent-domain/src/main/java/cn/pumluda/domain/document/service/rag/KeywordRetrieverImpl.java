package cn.pumluda.domain.document.service.rag;

import cn.pumluda.domain.document.adapter.repository.IDocumentChunkRepository;
import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;
import cn.pumluda.domain.document.model.valobj.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: 关键词检索实现——MySQL LIKE 匹配分块内容
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordRetrieverImpl implements IKeywordRetriever {

    private final IDocumentChunkRepository chunkRepository;

    @Override
    public List<SearchResult> search(String keyword, int topK) {
        log.info("[关键词检索] keyword={}, topK={}", keyword, topK);

        List<DocumentChunkEntity> chunks = chunkRepository.findByKeyword(keyword, topK);
        List<SearchResult> results = chunks.stream()
                .map(chunk -> SearchResult.builder()
                        .chunkId(chunk.getId())
                        .documentId(chunk.getDocumentId())
                        .titlePath(chunk.getTitlePath())
                        .content(chunk.getContent())
                        .score(1.0) // 关键词匹配无相似度分数，统一给 1.0
                        .build())
                .toList();

        log.info("[关键词检索] 命中 {} 条", results.size());
        return results;
    }

}
