package com.extension.backend.service;

import com.extension.backend.config.LlmConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SiliconFlow API 服务 - 处理与 LLM 提供商的交互
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiliconFlowService {
    
    private final WebClient webClient;
    private final LlmConfig llmConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 流式聊天接口
     */
    public Flux<String> chatStream(
            List<Map<String, Object>> messages,
            String apiKey,
            String apiBase,
            String model,
            Double temperature,
            Integer maxTokens) {
        
        String effectiveApiKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : llmConfig.getApiKey();
        String effectiveApiBase = (apiBase != null && !apiBase.isEmpty()) ? apiBase : llmConfig.getApiBase();
        String effectiveModel = (model != null && !model.isEmpty()) ? model : llmConfig.getDefaultModel();
        
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            return Flux.just("data: " + createErrorJson("API_KEY not configured") + "\n\n");
        }
        
        String url = effectiveApiBase + "/chat/completions";
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", effectiveModel);
        payload.put("messages", messages);
        payload.put("stream", true);
        payload.put("temperature", temperature != null ? temperature : 0.7);
        payload.put("max_tokens", maxTokens != null ? maxTokens : 4096);
        
        // 打印请求日志
        try {
            log.info("[Outgoing LLM Request] URL: {}", url);
            log.debug("[Outgoing LLM Request] Payload: {}", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to log request payload", e);
        }
        
        return webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + effectiveApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(error -> log.error("Stream error: ", error))
                .onErrorResume(error -> {
                    String errorJson = createErrorJson("Stream error: " + error.getMessage());
                    return Flux.just("data: " + errorJson + "\n\n");
                });
    }
    
    /**
     * 流式视觉接口
     */
    public Flux<String> visionStream(
            String image,
            String prompt,
            String content,
            List<Map<String, Object>> history,
            String apiKey,
            String apiBase,
            String model,
            Double temperature,
            Integer maxTokens) {
        
        String effectiveApiKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : llmConfig.getApiKey();
        String effectiveApiBase = (apiBase != null && !apiBase.isEmpty()) ? apiBase : llmConfig.getApiBase();
        String effectiveModel = (model != null && !model.isEmpty()) ? model : llmConfig.getVisionModel();
        
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            return Flux.just("data: " + createErrorJson("API_KEY not configured") + "\n\n");
        }
        
        String url = effectiveApiBase + "/chat/completions";
        
        // 构建消息列表
        List<Map<String, Object>> messages = new java.util.ArrayList<>();
        
        // 添加系统提示
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", prompt);
        messages.add(systemMessage);
        
        // 添加历史记录
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        
        // 添加当前视觉请求
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        
        List<Map<String, Object>> contentList = new java.util.ArrayList<>();
        
        // 添加图片
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");
        Map<String, String> imageUrl = new HashMap<>();
        imageUrl.put("url", image);
        imageContent.put("image_url", imageUrl);
        contentList.add(imageContent);
        
        // 添加文本
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", content);
        contentList.add(textContent);
        
        userMessage.put("content", contentList);
        messages.add(userMessage);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", effectiveModel);
        payload.put("messages", messages);
        payload.put("stream", true);
        payload.put("temperature", temperature != null ? temperature : 0.7);
        payload.put("max_tokens", maxTokens != null ? maxTokens : 4096);
        
        // 打印请求日志
        try {
            log.info("[Outgoing Vision Request] URL: {}", url);
            log.debug("[Outgoing Vision Request] Payload: {}", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to log vision request payload", e);
        }
        
        return webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + effectiveApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(error -> log.error("Vision stream error: ", error))
                .onErrorResume(error -> {
                    String errorJson = createErrorJson("Vision stream error: " + error.getMessage());
                    return Flux.just("data: " + errorJson + "\n\n");
                });
    }
    
    /**
     * 创建错误 JSON
     */
    private String createErrorJson(String message) {
        try {
            Map<String, String> error = new HashMap<>();
            error.put("error", message);
            return objectMapper.writeValueAsString(error);
        } catch (Exception e) {
            return "{\"error\":\"" + message + "\"}";
        }
    }
}
