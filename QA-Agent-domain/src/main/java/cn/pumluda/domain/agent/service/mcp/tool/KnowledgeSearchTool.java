package cn.pumluda.domain.agent.service.mcp.tool;

import cn.pumluda.domain.document.service.rag.recall.HybridRetrieverImpl;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MCP 工具：RAG 知识库检索 —— 供 AiServices Agent 调用
 */
@Slf4j
@Component
public class KnowledgeSearchTool {

    private final HybridRetrieverImpl hybridRetriever;

    public KnowledgeSearchTool(HybridRetrieverImpl hybridRetriever) {
        this.hybridRetriever = hybridRetriever;
    }

    @Tool("在知识库中搜索与查询关键词相关的文档内容。" +
          "返回标题路径和正文片段，LLM 应基于返回的片段回答用户问题。")
    public String searchKnowledgeBase(
            @ToolMemoryId String memoryId,
            String query) {
        log.info("[MCP工具] searchKnowledgeBase: query={}", query);
        var results = hybridRetriever.search(query, 5, true);
        if (results.isEmpty()) return "未找到相关内容。";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            var r = results.get(i);
            sb.append("---\n来源").append(i + 1).append(": ").append(r.getTitlePath())
              .append("\n内容: ").append(r.getContent()).append("\n\n");
        }
        return sb.toString();
    }

}
