package cn.pumluda.domain.bagu;

import cn.pumluda.api.dto.BaguItemResponse;
import cn.pumluda.api.dto.BaguSetResponse;
import cn.pumluda.domain.bagu.adapter.repository.IBaguSetRepository;
import cn.pumluda.domain.bagu.model.entity.QaItemEntity;
import cn.pumluda.domain.bagu.model.entity.QaSetEntity;
import cn.pumluda.domain.document.adapter.repository.IDocumentRepository;
import cn.pumluda.domain.document.model.entity.SourceDocumentEntity;
import cn.pumluda.types.enums.ResponseCode;
import cn.pumluda.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
    private final IBaguSetRepository baguSetRepository;
    private final OpenAiChatModel chatModel;

    @Override
    public BaguSetResponse generate(String shelfName, List<String> documentIds) {
        log.info("[BaguSkill] 开始生成问答集, shelf={}, docs={}", shelfName, documentIds.size());

        // 1. 加载文档
        List<SourceDocumentEntity> docs = documentIds.stream()
                .map(id -> documentRepository.findById(id)
                        .orElseThrow(() -> new AppException(ResponseCode.DOCUMENT_NOT_FOUND.getCode(), "文档不存在")))
                .collect(Collectors.toList());

        // 2. 构建 Prompt
        String prompt = buildPrompt(shelfName, docs);

        // 3. 调用 LLM 生成
        log.info("[BaguSkill] 调用LLM生成...");
        String response = chatModel.chat(prompt);
        log.info("[BaguSkill] LLM响应长度: {}", response.length());

        // 4. 解析 JSON
        JSONObject result = parseResponse(response);

        // 5. 落库（通过 domain repository 接口）
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
        for (SourceDocumentEntity doc : docs) {
            String content = doc.getRawContent();
            if (content == null) continue;
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
        sb.append("  \"description\": \"问答集简介\",\n");
        sb.append("  \"items\": [\n");
        sb.append("    {\"question\": \"...\", \"answer\": \"...\", \"difficulty\": \"EASY|MEDIUM|HARD\"}\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("要求：生成5-8题，难度分布1-2EASY/3-4MEDIUM/1-2HARD，答案详细准确\n");

        return sb.toString();
    }

    private JSONObject parseResponse(String response) {
        String json = response.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf("\n");
            int end = json.lastIndexOf("```");
            if (start > 0 && end > start) json = json.substring(start, end).trim();
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            log.error("[BaguSkill] JSON解析失败: {}", json.substring(0, Math.min(200, json.length())));
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "LLM返回格式异常，请重试");
        }
    }

    private BaguSetResponse saveToDb(String shelfName, List<String> documentIds, JSONObject result) {
        // 保存 qa_set
        QaSetEntity setEntity = new QaSetEntity();
        setEntity.setTitle(result.getString("title"));
        setEntity.setDescription(result.getString("description"));
        QaSetEntity saved = baguSetRepository.saveSet(setEntity);

        // 保存 qa_items
        JSONArray items = result.getJSONArray("items");
        if (items == null || items.isEmpty()) {
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "LLM未生成任何题目");
        }

        List<BaguItemResponse> itemResponses = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            QaItemEntity itemEntity = new QaItemEntity();
            itemEntity.setSetId(saved.getId());
            itemEntity.setQuestion(item.getString("question"));
            itemEntity.setAnswer(item.getString("answer"));
            itemEntity.setDifficulty(item.getString("difficulty") != null ? item.getString("difficulty") : "MEDIUM");
            itemEntity.setSortOrder(i + 1);
            QaItemEntity savedItem = baguSetRepository.saveItem(itemEntity);

            BaguItemResponse ir = new BaguItemResponse();
            ir.setId(savedItem.getId());
            ir.setQuestion(savedItem.getQuestion());
            ir.setAnswer(savedItem.getAnswer());
            ir.setDifficulty(savedItem.getDifficulty());
            ir.setSortOrder(savedItem.getSortOrder());
            itemResponses.add(ir);
        }

        // 更新 item_count
        baguSetRepository.updateItemCount(saved.getId(), items.size());

        // 保存文档关联
        for (String docId : documentIds) {
            baguSetRepository.saveDocumentRef(saved.getId(), docId);
        }

        // 构建响应
        BaguSetResponse resp = new BaguSetResponse();
        resp.setId(saved.getId());
        resp.setTitle(saved.getTitle());
        resp.setDescription(saved.getDescription());
        resp.setItemCount(items.size());
        resp.setItems(itemResponses);
        resp.setCreatedAt(saved.getCreatedAt());
        return resp;
    }

    @Override
    public List<BaguSetResponse> listSets() {
        return baguSetRepository.findAllSets().stream().map(set -> {
            BaguSetResponse r = new BaguSetResponse();
            r.setId(set.getId());
            r.setTitle(set.getTitle());
            r.setDescription(set.getDescription());
            r.setItemCount(set.getItemCount());
            r.setCreatedAt(set.getCreatedAt());
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public BaguSetResponse getSet(String id) {
        QaSetEntity set = baguSetRepository.findSetById(id);
        if (set == null) throw new AppException(ResponseCode.DOCUMENT_NOT_FOUND.getCode(), "题目集不存在");
        List<QaItemEntity> items = baguSetRepository.findItemsBySetId(id);
        List<BaguItemResponse> itemResponses = items.stream().map(item -> {
            BaguItemResponse ir = new BaguItemResponse();
            ir.setId(item.getId());
            ir.setQuestion(item.getQuestion());
            ir.setAnswer(item.getAnswer());
            ir.setDifficulty(item.getDifficulty());
            ir.setSortOrder(item.getSortOrder());
            return ir;
        }).collect(Collectors.toList());

        BaguSetResponse r = new BaguSetResponse();
        r.setId(set.getId());
        r.setTitle(set.getTitle());
        r.setDescription(set.getDescription());
        r.setItemCount(itemResponses.size());
        r.setItems(itemResponses);
        r.setCreatedAt(set.getCreatedAt());
        return r;
    }
}
