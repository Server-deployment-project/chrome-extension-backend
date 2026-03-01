package com.extension.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.llm")
public class LlmConfig {
    
    private String apiKey;
    
    private String apiBase;
    
    private String defaultModel;
    
    private String visionModel;
    
    private Long timeout = 60000L;
}
