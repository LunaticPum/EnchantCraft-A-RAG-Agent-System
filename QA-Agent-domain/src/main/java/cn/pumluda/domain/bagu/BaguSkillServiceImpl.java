package cn.pumluda.domain.bagu;

import cn.pumluda.api.dto.BaguItemResponse;
import cn.pumluda.api.dto.BaguSetResponse;
import cn.pumluda.domain.document.adapter.repository.IDocumentRepository;
import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;
import cn.pumluda.infrastructure.dao.QaItemDao;
import cn.pumluda.infrastructure.dao.QaSetDao;
import cn.pumluda.infrastructure.dao.QaSetDocumentRefDao;
import cn.pumluda.infrastructure.dao.po.QaItemPO;
import cn.pumluda.infrastructure.dao.po.QaSetDocumentRefPO;
import cn.pumluda.infrastructure.dao.po.QaSetPO;
import cn.pumluda.types.enums.ResponseCode;
import cn.pumluda.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaguSkillServiceImpl implements IBaguSkillService {

    private final IDocumentRepository documentRepository;
    private final QaSetDao qaSetDao;
    private final QaItemDao qaItemDao;
    private final QaSetDocumentRefDao qaSetDocumentRefDao;
    private final OpenAiChatModel chatModel;

    @Override
    public BaguSetResponse generate(String shelfName, List<String> documentIds) {
        log.info("[BaguSkill] 开始生成问答集, shelf={}, docs={}", shelfName, documentIds.size());

        // 1. 加载文档
        List<SourceDocumentEntity> docs = documentIds.stream()
                .map(id -> documentRepository.findById(id)
                        .orElseThrow(() -> new AppException(ResponseCode.DOCUMENT_NOT_FOUND)))
                .collect(Collectors.toList());

        // 2. 构建 Prompt
        String prompt = buildPrompt(shelfName, docs);

        // 3. 调用 LLM 生成
        log.info("[BaguSkill] 调用LLM生成...");
        String response = chatModel.chat(prompt);
        log.info("[BaguSkill] LLM响应长度: {}", response.length());

        // 4. 解析 JSON
        JSONObject result = parseResponse(response);

        // 5. 落库
        BaguSetResponse setResponse = saveToDb(shelfName, documentIds, result);

        log.info("[BaguSkill] 生成完成, setId={}, items={}", setResponse.getId(), setResponse.getItemCount());
        return setResponse;
    }

    private String buildPrompt(String shelfName, List<SourceDocumentEntity> docs) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深技术面试官。请基于以下学习笔记，生成一套结构化面试题。\n\n");
        sb.append("## 资料库主题：").append(shelfName).append("\n\n");
        sb.append("## 学习笔记内容：\n\n");

        int totalLen = 0;
        for (int i = 0; i < docs.size(); i++) {
            SourceDocumentEntity doc = docs.get(i);
            String content = doc.getRawContent();
            if (content == null) continue;
            // 限制总输入长度
            int remaining = 12000 - totalLen;
            if (remaining <= 0) break;
            String snippet = content.length() > remaining ? content.substring(0, remaining) + "..." : content;
            sb.append("### ").append(doc.getFileName()).append("\n");
            sb.append(snippet).append("\n\n");
            totalLen += snippet.length();
        }

        sb.append("## 输出要求\n");
        sb.append("请以JSON格式输出，不要包含markdown代码块标记。格式如下：\n");
        sb.append("{\n");
        sb.append("  \"title\": \"问答集标题\",\n");
        sb.append("  \"description\": \"问答集简介，一句话描述覆盖的知识点\",\n");
        sb.append("  \"items\": [\n");
        sb.append("    {\n");
        sb.append("      \"question\": \"面试题目\",\n");
        sb.append("      \"answer\": \"参考答案（尽可能详细，引用笔记中的关键概念）\",\n");
        sb.append("      \"difficulty\": \"EASY|MEDIUM|HARD\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("要求：\n");
        sb.append("- 生成 5-8 道题目\n");
        sb.append("- 难度分布：1-2道EASY，3-4道MEDIUM，1-2道HARD\n");
        sb.append("- 答案要详细、准确，基于笔记内容\n");
        sb.append("- 题目覆盖不同知识点，避免重复\n");

        return sb.toString();
    }

    private JSONObject parseResponse(String response) {
        // 尝试提取JSON（去除可能的markdown代码块）
        String json = response.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf("\n");
            int end = json.lastIndexOf("```");
            if (start > 0 && end > start) {
                json = json.substring(start, end).trim();
            }
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            log.error("[BaguSkill] JSON解析失败: {}", json.substring(0, Math.min(200, json.length())));
            throw new AppException(ResponseCode.UN_ERROR, "LLM返回格式异常，请重试");
        }
    }

    private BaguSetResponse saveToDb(String shelfName, List<String> documentIds, JSONObject result) {
        // 保存 qa_set
        QaSetPO setPO = QaSetPO.builder()
                .title(result.getString("title"))
                .description(result.getString("description"))
                .itemCount(0)
                .build();
        qaSetDao.insert(setPO);

        // 保存 qa_items
        JSONArray items = result.getJSONArray("items");
        if (items == null || items.isEmpty()) {
            throw new AppException(ResponseCode.UN_ERROR, "LLM未生成任何题目");
        }

        List<BaguItemResponse> itemResponses = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            QaItemPO itemPO = QaItemPO.builder()
                    .setId(setPO.getId())
                    .question(item.getString("question"))
                    .answer(item.getString("answer"))
                    .difficulty(item.getString("difficulty") != null ? item.getString("difficulty") : "MEDIUM")
                    .sortOrder(i + 1)
                    .build();
            qaItemDao.insert(itemPO);

            BaguItemResponse ir = new BaguItemResponse();
            ir.setId(itemPO.getId());
            ir.setQuestion(itemPO.getQuestion());
            ir.setAnswer(itemPO.getAnswer());
            ir.setDifficulty(itemPO.getDifficulty());
            ir.setSortOrder(itemPO.getSortOrder());
            itemResponses.add(ir);
        }

        // 更新 item_count
        setPO.setItemCount(items.size());
        qaSetDao.updateById(setPO);

        // 保存文档关联
        for (String docId : documentIds) {
            QaSetDocumentRefPO ref = QaSetDocumentRefPO.builder()
                    .setId(setPO.getId())
                    .documentId(docId)
                    .build();
            qaSetDocumentRefDao.insert(ref);
        }

        // 构建响应
        BaguSetResponse resp = new BaguSetResponse();
        resp.setId(setPO.getId());
        resp.setTitle(setPO.getTitle());
        resp.setDescription(setPO.getDescription());
        resp.setItemCount(items.size());
        resp.setItems(itemResponses);
        resp.setCreatedAt(setPO.getCreatedAt());
        return resp;
    }
}
