package cn.pumluda.domain.agent.service.mcp.tool;

import cn.pumluda.domain.agent.model.valobj.Citation;
import cn.pumluda.domain.document.model.valobj.SearchResult;
import cn.pumluda.domain.document.service.rag.recall.HybridRetrieverImpl;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP 工具：知识库检索
 * <p>
 * 被 LangChain4j AiServices Agent 调用，LLM 自主判断是否需要查资料。
 */
@Slf4j
@Component
public class KnowledgeBaseSearchTool {

    private final HybridRetrieverImpl hybridRetriever;

    public KnowledgeBaseSearchTool(HybridRetrieverImpl hybridRetriever) {
        this.hybridRetriever = hybridRetriever;
    }

    /**
     * 在知识库中搜索相关内容
     *
     * @param query 检索关键词，应为经过提炼的技术术语
     * @return 检索到的证据文本及来源，未找到时返回"未找到相关内容"
     */
    @Tool("在用户的知识库（Markdown 学习笔记）中搜索相关内容。" +
          "当用户询问技术问题、概念解释、代码用法等可能需要查资料的问题时调用。" +
          "query 参数使用关键词形式，如 'InnoDB 索引原理'。")
    public String searchKnowledgeBase(String query) {
        log.info("[MCP工具] 知识库检索: query={}", query);
        List<SearchResult> results = hybridRetriever.search(query, 5, true);

        if (results.isEmpty()) {
            return "未找到相关内容";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append("--- 来源 ").append(i + 1).append(": ")
              .append(r.getTitlePath()).append(" ---\n")
              .append(r.getContent()).append("\n\n");
        }
        return sb.toString();
    }

}
