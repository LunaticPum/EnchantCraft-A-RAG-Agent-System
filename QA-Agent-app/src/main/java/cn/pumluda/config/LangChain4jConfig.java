package cn.pumluda.config;

import cn.pumluda.infrastructure.adapter.repository.pgvector.PgVectorStore;
import com.zaxxer.hikari.HikariDataSource;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 框架配置 —— Chat / Embedding / VectorStore
 */
@Configuration
public class LangChain4jConfig {

    @Value("${deepSeek.api-key}")
    private String chatModelApiKey;
    @Value("${deepSeek.base-url}")
    private String chatModelBaseUrl;
    @Value("${deepSeek.model}")
    private String chatModel;

    @Value("${dashScope.api-key}")
    private String dashScopeApiKey;
    @Value("${dashScope.base-url}")
    private String dashScopeBaseUrl;
    @Value("${dashScope.embedding-model}")
    private String embeddingModel;

    @Value("${pg.datasource.url}")
    private String pgUrl;
    @Value("${pg.datasource.username}")
    private String pgUsername;
    @Value("${pg.datasource.password}")
    private String pgPassword;
    @Value("${pg.datasource.driver-class-name}")
    private String pgDriver;

    @Bean
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
                              .apiKey(chatModelApiKey)
                              .baseUrl(chatModelBaseUrl)
                              .modelName(chatModel)
                              .timeout(Duration.ofSeconds(60))
                              .logRequests(true)
                              .logResponses(true)
                              .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                                   .apiKey(dashScopeApiKey)
                                   .baseUrl(dashScopeBaseUrl)
                                   .modelName(embeddingModel)
                                   .timeout(Duration.ofSeconds(60))
                                   .logRequests(false)
                                   .logResponses(false)
                                   .build();
    }

    @Bean
    public PgVectorStore embeddingStore() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(pgUrl);
        dataSource.setUsername(pgUsername);
        dataSource.setPassword(pgPassword);
        dataSource.setDriverClassName(pgDriver);
        dataSource.setMinimumIdle(2);
        dataSource.setMaximumPoolSize(10);
        return new PgVectorStore(dataSource);
    }

}
