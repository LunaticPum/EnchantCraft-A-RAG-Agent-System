package cn.pumluda.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Project: QA-Agent-Pumluda <p>
 * File: VaultProperties <p>
 * Created by: 16374 <p>
 * Date: 2026/6/10 <p>
 * Time: 09:42 <p>
 * Description: 配置要加载本地md笔记目录以及笔记切片方式
 */
@Data
@Component
@ConfigurationProperties(prefix = "vault")
public class VaultProperties {

    /**
     * Obsidian vault 根目录路径
     */
    private String path = "D:/Obsidian/ObsProject/日常笔记";

    /**
     * 向量检索返回的最大切片数
     */
    private int retrievalTopK = 5;

    /**
     * 每个切片的最大字符数
     */
    private int chunkMaxSize = 2000;
}
