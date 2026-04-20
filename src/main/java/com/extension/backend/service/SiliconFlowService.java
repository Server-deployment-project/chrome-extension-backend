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

    private static final String LOCAL_PRIVATE_CONFIG_PATH = "config/application-private.yml";
    private static final String PLACEHOLDER_API_KEY = "sk-your-key";
    
    private final WebClient webClient;
    private final LlmConfig llmConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 流式聊天接口
     * 
     * 参数优先级：
     * 1. 如果传入了完整的自定义配置（apiKey、apiBase、model都不为空），则使用自定义配置
     * 2. 否则使用系统默认配置
     * 
     * 注意：必须同时提供apiKey、apiBase和model三个参数才能使用自定义配置
     */
    public Flux<String> chatStream(
            List<Map<String, Object>> messages,
            String apiKey,
            String apiBase,
            String model,
            Double temperature,
            Integer maxTokens) {
        
        // 检查是否提供了完整的自定义配置（trim后不为空）
        boolean hasFullCustomConfig = apiKey != null && !apiKey.trim().isEmpty() 
                && apiBase != null && !apiBase.trim().isEmpty() 
                && model != null && !model.trim().isEmpty();
        
        // 根据配置完整性决定使用哪个配置
        String effectiveApiKey;
        String effectiveApiBase;
        String effectiveModel;
        
        if (hasFullCustomConfig) {
            // 使用完整的自定义配置
            effectiveApiKey = apiKey.trim();
            effectiveApiBase = apiBase.trim();
            effectiveModel = model.trim();
            log.info("使用自定义文本配置 - Model: {}, API Base: {}", effectiveModel, effectiveApiBase);
        } else {
            // 使用系统默认配置
            effectiveApiKey = llmConfig.getApiKey();
            effectiveApiBase = llmConfig.getApiBase();
            effectiveModel = llmConfig.getDefaultModel();
            log.info("使用系统默认配置 - Model: {}, API Base: {}", effectiveModel, effectiveApiBase);

            String configError = validateSystemConfig(effectiveApiKey, effectiveApiBase, effectiveModel, "文本");
            if (configError != null) {
                return Flux.just(configError);
            }
        }
        
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            return Flux.just(createErrorJson("API_KEY not configured"));
        }
        
        // 确保 URL 正确拼接，移除尾部斜杠
        String baseUrl = effectiveApiBase.endsWith("/") ? 
                effectiveApiBase.substring(0, effectiveApiBase.length() - 1) : effectiveApiBase;
        String url = baseUrl + "/chat/completions";
        
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
                    return Flux.just(errorJson);
                });
    }
    
    /**
     * 流式视觉接口
     * 
     * 参数优先级：
     * 1. 如果传入了完整的自定义配置（apiKey、apiBase、model都不为空），则使用自定义配置
     * 2. 否则使用系统默认配置
     * 
     * 注意：必须同时提供apiKey、apiBase和model三个参数才能使用自定义配置
     */
    public Flux<String> visionStream(
            List<String> images,
            String prompt,
            String content,
            List<Map<String, Object>> history,
            String apiKey,
            String apiBase,
            String model,
            Double temperature,
            Integer maxTokens) {
        
        // 检查是否提供了完整的自定义配置（trim后不为空）
        boolean hasFullCustomConfig = apiKey != null && !apiKey.trim().isEmpty() 
                && apiBase != null && !apiBase.trim().isEmpty() 
                && model != null && !model.trim().isEmpty();
        
        // 根据配置完整性决定使用哪个配置
        String effectiveApiKey;
        String effectiveApiBase;
        String effectiveModel;
        
        if (hasFullCustomConfig) {
            // 使用完整的自定义配置
            effectiveApiKey = apiKey.trim();
            effectiveApiBase = apiBase.trim();
            effectiveModel = model.trim();
            log.info("使用自定义视觉配置 - Model: {}, API Base: {}", effectiveModel, effectiveApiBase);
        } else {
            // 使用系统默认配置
            effectiveApiKey = llmConfig.getApiKey();
            effectiveApiBase = llmConfig.getApiBase();
            effectiveModel = llmConfig.getVisionModel();
            log.info("使用系统默认配置 - Model: {}, API Base: {}", effectiveModel, effectiveApiBase);

            String configError = validateSystemConfig(effectiveApiKey, effectiveApiBase, effectiveModel, "视觉");
            if (configError != null) {
                return Flux.just(configError);
            }
        }
        
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            return Flux.just(createErrorJson("API_KEY not configured"));
        }
        
        // 确保 URL 正确拼接，移除尾部斜杠
        String baseUrl = effectiveApiBase.endsWith("/") ? 
                effectiveApiBase.substring(0, effectiveApiBase.length() - 1) : effectiveApiBase;
        String url = baseUrl + "/chat/completions";
        
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
        
        // 添加图片（支持多图）
        if (images != null) {
            for (String image : images) {
                if (image == null || image.isBlank()) {
                    continue;
                }
                Map<String, Object> imageContent = new HashMap<>();
                imageContent.put("type", "image_url");
                Map<String, String> imageUrl = new HashMap<>();
                imageUrl.put("url", image);
                imageContent.put("image_url", imageUrl);
                contentList.add(imageContent);
            }
        }
        
        // 添加文本
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", content != null ? content : "");
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
                    return Flux.just(errorJson);
                });
    }

    private String validateSystemConfig(String apiKey, String apiBase, String model, String scene) {
        if (apiKey == null || apiKey.isBlank() || PLACEHOLDER_API_KEY.equals(apiKey.trim())) {
            return createErrorJson("LLM API Key 未配置，请检查 " + LOCAL_PRIVATE_CONFIG_PATH);
        }
        if (apiBase == null || apiBase.isBlank()) {
            return createErrorJson(scene + " API Base 未配置，请检查 " + LOCAL_PRIVATE_CONFIG_PATH);
        }
        if (model == null || model.isBlank()) {
            return createErrorJson(scene + " 模型未配置，请检查 " + LOCAL_PRIVATE_CONFIG_PATH);
        }
        return null;
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
