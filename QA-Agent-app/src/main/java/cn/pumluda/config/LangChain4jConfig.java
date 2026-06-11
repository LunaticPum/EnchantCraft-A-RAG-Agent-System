package cn.pumluda.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Project: QA-Agent-Pumluda <p>
 * File: LangChain4jConfig <p>
 * Created by: 16374 <p>
 * Date: 2026/6/10 <p>
 * Time: 10:49 <p>
 * Description: langChain4j框架配置
 */

@Configuration
public class LangChain4jConfig {

    @Value("${deepSeek.model}")
    private String chatModel;
    @Value("${deepSeek.api-key}")
    private String chatModelApiKey;
    @Value("${deepSeek.base-url}")
    private String chatModelBaseUrl;

    @Value("${dashScope.embedding-model}")
    private String embeddingModel;
    @Value("${dashScope.rerank-model}")
    private String rerankModel;          // 预留：V2.4 混合检索时用，需自定义适配 DashScope gte-rerank
    @Value("${dashScope.api-key}")
    private String dashScopeApiKey;
    @Value("${dashScope.base-url}")
    private String dashScopeBaseUrl;

    /**
     * 依据 OpenAI 标准定义的对话模型
     *
     * @return DeepSeek 对话模型
     */
    @Bean
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder().apiKey(chatModelApiKey).baseUrl(chatModelBaseUrl).modelName(chatModel).timeout(
                                      Duration.ofSeconds(60)).logRequests(true)  // 打印模型输入和输出的返回内容
                              .logResponses(true).build();
    }

    /**
     * 依据 OpenAI 标准定义的 Embedding 模型
     *
     * @return DeepSeek Embedding 模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {

        return OpenAiEmbeddingModel.builder()
                                   .apiKey(dashScopeApiKey)
                                   .baseUrl(dashScopeBaseUrl)
                                   .modelName(embeddingModel)
                                   .timeout(Duration.ofSeconds(60))
                                   .logRequests(false)  // 打印模型输入和输出的返回内容
                                   .logResponses(false)
                                   .build();
    }

    /**
     * 在内存中存储 Embedded 向量,后续再考虑更换别的存储方式
     *
     * @return 向量存储
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

}
