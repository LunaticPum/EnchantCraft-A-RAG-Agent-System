package cn.pumluda.domain.document.service.chunk;

import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;
import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;

import java.util.List;

/**
 * Project: QA-Agent-Pumluda
 * Description: Markdown 分块服务接口——将 Markdown 文档按标题层级拆分为语义块
 */
public interface IMarkdownChunker {

    /**
     * 对文档进行分块
     *
     * @param document 源文档（包含 rawContent）
     * @return 按标题层级切分后的语义块列表，按 chunk_index 升序
     */
    List<DocumentChunkEntity> chunk(SourceDocumentEntity document);

}
