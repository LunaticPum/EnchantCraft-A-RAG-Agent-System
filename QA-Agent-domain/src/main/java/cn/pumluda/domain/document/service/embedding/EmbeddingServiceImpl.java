package cn.pumluda.domain.document.service.embedding;

import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: DeepSeek Embedding 实现——将分块文本批量向量化并存入 InMemoryEmbeddingStore
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements IEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public void embedChunks(List<DocumentChunkEntity> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            log.debug("[Embedding] 空分块列表，跳过");
            return;
        }
        log.info("[Embedding] 开始向量化: chunks={}", chunks.size());

        // 1. 为每个 chunk 构造 TextSegment，携带 metadata 用于检索时还原标识信息
        List<TextSegment> segments = chunks.stream().map(chunk -> {
            Metadata metadata = new Metadata();
            metadata.put("chunkId", chunk.getId());
            metadata.put("documentId", chunk.getDocumentId());
            metadata.put("titlePath", chunk.getTitlePath() != null ? chunk.getTitlePath() : "");
            metadata.put("moduleTags", chunk.getModuleTags() != null ? String.join(",", chunk.getModuleTags()) : "");
            return TextSegment.from(chunk.getContent(), metadata);
        }).toList();

        // 2. 分批 Embedding：DashScope 限制单次最多 10 条，超过需分批发送
        int batchSize = 10;
        int totalEmbedded = 0;
        for (int i = 0; i < segments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, segments.size());
            List<TextSegment> batch = segments.subList(i, end);

            Response<List<Embedding>> response = embeddingModel.embedAll(batch);
            List<Embedding> embeddings = response.content();

            // 3. 将本批向量配对存入向量存储
            embeddingStore.addAll(embeddings, batch);
            totalEmbedded += embeddings.size();

            log.debug(
                    "[Embedding] 批次 {}/{}: {} 条",
                    (i / batchSize) + 1,
                    (segments.size() + batchSize - 1) / batchSize,
                    embeddings.size()
            );
        }
        log.info("[Embedding] 向量化全部完成: {} 条", totalEmbedded);
    }

}
