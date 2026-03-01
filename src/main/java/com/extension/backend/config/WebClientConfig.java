package com.extension.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient 配置
 */
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient(LlmConfig llmConfig) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(llmConfig.getTimeout()));
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
