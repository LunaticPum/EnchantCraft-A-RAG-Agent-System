package cn.pumluda.domain.agent.service.chat;

import cn.pumluda.domain.agent.model.valobj.Citation;
import cn.pumluda.domain.agent.model.valobj.RetrievalMode;

import java.util.List;
import java.util.function.Consumer;

/**
 * Agent 核心对话接口 —— RAG 检索 + LLM 生成编排
 */
public interface IAgentChat {

    /**
     * 执行一次 Agent 对话
     *
     * @param sessionId     会话 ID（多轮记忆的标识）
     * @param userMessage   用户输入
     * @param retrievalMode 检索模式
     * @param onToken       流式回调：每收到一个 token 时触发
     * @param onCitation    引用回调：检索完成后触发，传回命中的引用列表
     * @return 完整回答文本
     */
    String chat(String sessionId,
                String userMessage,
                RetrievalMode retrievalMode,
                Consumer<String> onToken,
                Consumer<List<Citation>> onCitation);

}
