package cn.pumluda.domain.agent.model.valobj;

/**
 * 检索模式枚举
 */
public enum RetrievalMode {

    /** 强制检索 —— 每次对话先 RAG 再生成 */
    FORCE,

    /** Tool Calling —— LLM 自主决定是否检索（V4.2 实现） */
    TOOL

}
