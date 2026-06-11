package cn.pumluda.domain.document.service.chunk;

import cn.pumluda.domain.document.model.entity.DocumentChunkEntity;
import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Project: QA-Agent-Pumluda
 * Description: flexmark AST 分块实现——按标题层级（H1-H6）将 Markdown 文档切分为语义块
 */
@Slf4j
@Service
public class MarkdownChunkerImpl implements IMarkdownChunker {

    // 引入 flexmark 解析器
    private static final Parser PARSER = Parser.builder().build();

    @Override
    public List<DocumentChunkEntity> chunk(SourceDocumentEntity document) {
        log.info(
                "[分块] 开始处理: documentId={}, contentLength={}",
                document.getId(),
                document.getRawContent().length()
        );

        String rawContent = document.getRawContent();
        if (rawContent == null || rawContent.isBlank()) {
            log.warn("[分块] 文档内容为空，返回空列表");
            return Collections.emptyList();
        }

        // 先解析原始md文本，获得AST抽象语法树
        Document ast = PARSER.parse(rawContent);
        List<DocumentChunkEntity> chunks = extractChunks(ast, document.getId());
        log.info("[分块] 处理完成: chunks={}", chunks.size());
        return chunks;
    }

    /**
     * 遍历 AST，按标题节点H1-H6切分内容
     */
    private List<DocumentChunkEntity> extractChunks(Document ast, String documentId) {
        List<DocumentChunkEntity> chunks = new ArrayList<>();  // 保存文本最终分块的结果容器

        // 标题路径栈（FILO）：栈顶为当前标题层级，用于构建 title_path
        LinkedList<String> headingStack = new LinkedList<>();
        StringBuilder currentContent = new StringBuilder();
        int chunkIndex = 0;

        /*
        * 遍历所有 AST 子节点，包括：
        * - Heading 标题
        * - Paragraph 段落
        * - CodeBlock 代码块
        * - ThematicBreak 分割线 等等
        * */
        for (Node node : ast.getChildren()) {
            if (node instanceof Heading heading) {
                // 遇到新标题 → 结束当前 chunk，开始新的
                int level = heading.getLevel();

                // 保存上一个 chunk（如果有内容）
                if (!currentContent.isEmpty()) {
                    chunkIndex++;
                    chunks.add(buildChunk(documentId, chunkIndex, headingStack, currentContent.toString()));
                    currentContent.setLength(0);  // 清空 StringBuilder，准备接受下一个Chunk的String文本
                }

                /*
                * 维护标题路径栈：只要当前遍历的标题节点已经不再此前的递归层级中，就连续弹出 >= 当前层级的所有标题，例如：
                * # A (H1)
                * ## B (H2)
                * ### C (H3)
                * ## D (H2)
                * 遍历到 D，将标题路径栈中的 B 和 C 弹出，然后压入当前标题D；不管什么情况都会压入当前标题，只要当前标题不再是嵌套的层级，就要弹出上一个节点
                * */
                while (headingStack.size() >= level) {
                    headingStack.pollLast();
                }
                headingStack.addLast(heading.getText().toString());
            } else {
                // 非标题节点 → 追加到当前 chunk 的内容
                String text = node.getChars().toString().trim();
                if (!text.isEmpty()) {
                    if (!currentContent.isEmpty()) {
                        currentContent.append("\n\n");
                    }
                    currentContent.append(text);
                }
            }
        }

        // 最后一个 chunk
        if (!currentContent.isEmpty()) {
            chunkIndex++;
            chunks.add(buildChunk(documentId, chunkIndex, headingStack, currentContent.toString()));
        }

        return chunks;
    }

    /**
     * 从标题路径栈构造一个 DocumentChunkEntity
     */
    private DocumentChunkEntity buildChunk(String documentId, int chunkIndex,
                                           LinkedList<String> headingStack, String content) {
        String titlePath = String.join(" > ", headingStack);
        List<String> moduleTags = extractModuleTags(headingStack);
        LocalDateTime now = LocalDateTime.now();

        return DocumentChunkEntity.builder()
                                  .documentId(documentId)
                                  .chunkIndex(chunkIndex)
                                  .titlePath(titlePath.isEmpty() ? null : titlePath)
                                  .content(content)
                                  .moduleTags(moduleTags)
                                  .createdAt(now)
                                  .updatedAt(now)
                                  .build();
    }

    /**
     * 从标题路径提取模块标签（取每层标题作为标签关键词）
     */
    private List<String> extractModuleTags(LinkedList<String> headingStack) {
        return headingStack.stream()
                           .map(String::trim)
                           .filter(s -> !s.isEmpty())
                           .collect(Collectors.toList());
    }

}
