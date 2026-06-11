package cn.pumluda.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Project: QA-Agent-Pumluda
 * Description: MyBatis-Plus 配置
 */
@Configuration
@MapperScan("cn.pumluda.infrastructure.dao")
public class MyBatisPlusConfig {

}
