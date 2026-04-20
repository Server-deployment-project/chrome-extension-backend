package com.extension.backend.controller;

import com.extension.backend.constant.PromptConstants;
import com.extension.backend.dto.VisionRequest;
import com.extension.backend.entity.Conversation;
import com.extension.backend.entity.Message;
import com.extension.backend.entity.User;
import com.extension.backend.exception.BusinessException;
import com.extension.backend.service.ConversationService;
import com.extension.backend.service.SiliconFlowService;
import com.extension.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 视觉控制器 - 处理图片理解
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VisionController {
    
    private final SiliconFlowService siliconFlowService;
    private final UserService userService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 视觉流式接口 - 返回 200 或报错
     */
    @PostMapping(value = "/vision", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> vision(@RequestBody VisionRequest request, ServerWebExchange exchange) {
        User user = exchange.getAttribute("USER");
        
        // 验证认证
        if (user == null) {
            throw BusinessException.unauthorized("Invalid or inactive extension token");
        }
        
        // 验证参数：至少需要一张图片
        List<String> images = normalizeImages(request);
        if (images.isEmpty()) {
            throw BusinessException.badRequest("image or images is required");
        }
        
        try {
            log.info("Vision request from user: {}, action: {}, conversationId: {}, imageCount: {}",
                    user.getEmail(), request.getAction(), request.getConversationId(), images.size());
            
            // 判断是否使用自定义配置
            boolean isCustom = user.hasCustomVisionConfig();
            
            // 检查配额（非自定义配置才检查）
            if (!isCustom) {
                user.resetQuotaIfNeeded();
                if (user.getRequestsToday() >= user.getDailyQuota()) {
                    throw BusinessException.rateLimitExceeded(
                            "您的每日 " + user.getDailyQuota() + " 次请求额度已用完，请明天再试。");
                }
            }
            
            // 获取或创建会话
            Conversation conversation = getOrCreateConversation(user, request);
            
            // 保存用户消息到数据库
            String userContent = request.getContent() != null ? request.getContent() : 
                    PromptConstants.VISION_DEFAULT_USER_MESSAGE;
            saveMessageAsync(conversation.getId(), "user", "[图片x" + images.size() + "] " + userContent);
            
            // 更新请求计数
            if (!isCustom) {
                updateUserRequestCount(user);
            }
            
            // 构建历史记录
            List<Map<String, Object>> history = buildHistory(request, conversation);
            
            // 确定系统提示词
            String action = request.getAction() != null ? request.getAction() : "analyze";
            String systemPrompt = "analyze".equals(action) ? PromptConstants.SYSTEM_PROMPT : 
                    PromptConstants.CHAT_SYSTEM_PROMPT;
            
            // 收集完整响应
            StringBuilder fullResponse = new StringBuilder();
            
            // 调用 LLM 服务
            Flux<String> stream = siliconFlowService.visionStream(
                    images,
                    systemPrompt,
                    userContent,
                    history,
                    user.getCustomVisionApiKey(),
                    user.getCustomVisionApiBase(),
                    request.getModel() != null ? request.getModel() : user.getCustomVisionModel(),
                    request.getTemperature(),
                    request.getMaxTokens()
            );
            
            // 在流最前面注入包含 conversation_id 的前置数据包
            String initEvent = "data: {\"conversation_id\":\"" + conversation.getId() + "\"}\n\n";
            stream = stream.startWith(initEvent)
            .doOnNext(chunk -> {
                // 解析并提取 content
                try {
                    if (chunk.startsWith("data: ") && !chunk.contains("[DONE]")) {
                        String json = chunk.substring(6).trim();
                        if (!json.isEmpty()) {
                            var node = objectMapper.readTree(json);
                            if (node.has("choices")) {
                                var delta = node.get("choices").get(0).get("delta");
                                if (delta.has("content")) {
                                    fullResponse.append(delta.get("content").asText());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse chunk for storage", e);
                }
            })
            .doOnComplete(() -> {
                // 流结束后保存 assistant 消息
                String assistantContent = fullResponse.toString();
                if (!assistantContent.isEmpty()) {
                    saveMessageAsync(conversation.getId(), "assistant", assistantContent);
                }
            })
            .onErrorResume(error -> {
                log.error("Vision stream error: ", error);
                // 返回错误信息
                String errorMsg = error.getMessage() != null ? error.getMessage() : "Unknown error";
                return Flux.just("data: {\"error\":{\"message\":\"" + errorMsg + "\",\"code\":\"upstream_error\"}}\n\n");
            });
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(stream);
            
        } catch (Exception e) {
            log.error("Vision error: ", e);
            // 异常由 GlobalExceptionHandler 处理
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
        }
    }
    
    /**
     * 获取或创建会话
     */
    private Conversation getOrCreateConversation(User user, VisionRequest request) {
        Conversation conversation = null;
        
        if (request.getConversationId() != null) {
            conversation = conversationService.getConversation(request.getConversationId(), user.getId());
        }
        
        if (conversation == null) {
            String title = extractTitle(request);
            conversation = conversationService.createConversation(user.getId(), title);
        }
        
        return conversation;
    }
    
    /**
     * 提取标题
     */
    private String extractTitle(VisionRequest request) {
        String content = request.getContent();
        if (content != null && !content.isEmpty()) {
            return content.length() > 20 ? content.substring(0, 20) : content;
        }
        return "图片分析";
    }

    /**
     * 兼容 image(单图) 和 images(多图) 字段，统一归一化为图片列表
     */
    private List<String> normalizeImages(VisionRequest request) {
        List<String> normalized = new ArrayList<>();

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (String image : request.getImages()) {
                if (image != null && !image.isBlank()) {
                    normalized.add(image.trim());
                }
            }
        }

        if (normalized.isEmpty() && request.getImage() != null && !request.getImage().isBlank()) {
            normalized.add(request.getImage().trim());
        }

        return normalized;
    }
    
    /**
     * 构建历史记录
     */
    private List<Map<String, Object>> buildHistory(VisionRequest request, Conversation conversation) {
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            return request.getHistory().stream()
                    .map(h -> {
                        Map<String, Object> map = new HashMap<>();
                        h.forEach(map::put);
                        return map;
                    })
                    .collect(Collectors.toList());
        }
        
        // 从数据库加载历史
        List<Message> historyMessages = conversationService.getMessages(conversation.getId(), 20);
        List<Map<String, Object>> history = new ArrayList<>();
        for (Message msg : historyMessages) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            history.add(m);
        }
        return history;
    }
    
    /**
     * 异步保存消息
     */
    private void saveMessageAsync(String conversationId, String role, String content) {
        Schedulers.boundedElastic().schedule(() -> {
            try {
                conversationService.addMessage(conversationId, role, content);
            } catch (Exception e) {
                log.error("Failed to save message", e);
            }
        });
    }
    
    /**
     * 更新用户请求计数
     */
    private void updateUserRequestCount(User user) {
        Schedulers.boundedElastic().schedule(() -> {
            try {
                user.setRequestsToday(user.getRequestsToday() + 1);
                user.setLastRequestAt(LocalDateTime.now());
                userService.updateUser(user);
            } catch (Exception e) {
                log.error("Failed to update user request count", e);
            }
        });
    }
}
