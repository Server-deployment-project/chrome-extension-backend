package com.extension.backend.controller;

import com.extension.backend.constant.PromptConstants;
import com.extension.backend.dto.ChatRequest;
import com.extension.backend.entity.Conversation;
import com.extension.backend.entity.Message;
import com.extension.backend.entity.User;
import com.extension.backend.service.ConversationService;
import com.extension.backend.service.SiliconFlowService;
import com.extension.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天控制器 - 处理文本对话
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {
    
    private final SiliconFlowService siliconFlowService;
    private final UserService userService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 文本对话流式接口
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request, ServerWebExchange exchange) {
        User user = exchange.getAttribute("USER");
        if (user == null) {
            return Flux.just("data: {\"error\":\"用户未认证\"}\n\n");
        }
        
        try {
            log.info("Chat request from user: {}, action: {}, conversationId: {}", 
                    user.getEmail(), request.getAction(), request.getConversationId());
            
            // 判断是否使用自定义配置
            boolean isCustom = user.hasCustomTextConfig();
            
            // 检查配额（非自定义配置才检查）
            if (!isCustom) {
                user.resetQuotaIfNeeded();
                if (user.getRequestsToday() >= user.getDailyQuota()) {
                    return Flux.just("data: {\"error\":\"您的每日 " + user.getDailyQuota() + 
                            " 次请求额度已用完，请明天再试。\",\"code\":\"rate_limit_exceeded\"}\n\n");
                }
            }
            
            // 获取或创建会话
            Conversation conversation = getOrCreateConversation(user, request);
            
            // 保存用户消息到数据库
            String userContent = extractUserContent(request);
            if (userContent != null && !userContent.isEmpty()) {
                saveMessageAsync(conversation.getId(), "user", userContent);
            }
            
            // 更新请求计数
            if (!isCustom) {
                updateUserRequestCount(user);
            }
            
            // 构建消息列表
            List<Map<String, Object>> messages = buildMessages(request, conversation);
            
            // 收集完整响应（用于存储到数据库）
            StringBuilder fullResponse = new StringBuilder();
            
            // 调用 LLM 服务
            return siliconFlowService.chatStream(
                    messages,
                    user.getCustomTextApiKey(),
                    user.getCustomTextApiBase(),
                    request.getModel() != null ? request.getModel() : user.getCustomTextModel(),
                    request.getTemperature(),
                    request.getMaxTokens()
            )
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
                log.error("Chat stream error: ", error);
                return Flux.just("data: {\"error\":\"" + error.getMessage() + "\"}\n\n");
            });
            
        } catch (Exception e) {
            log.error("Chat error: ", e);
            return Flux.just("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
        }
    }
    
    /**
     * 获取或创建会话
     */
    private Conversation getOrCreateConversation(User user, ChatRequest request) {
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
    private String extractTitle(ChatRequest request) {
        String content = extractUserContent(request);
        if (content != null && !content.isEmpty()) {
            return content.length() > 20 ? content.substring(0, 20) : content;
        }
        return "新会话";
    }
    
    /**
     * 提取用户内容
     */
    private String extractUserContent(ChatRequest request) {
        if (request.getContent() != null && !request.getContent().isEmpty()) {
            return request.getContent();
        }
        
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            Map<String, Object> lastMessage = request.getMessages().get(request.getMessages().size() - 1);
            if ("user".equals(lastMessage.get("role"))) {
                return String.valueOf(lastMessage.get("content"));
            }
        }
        
        return null;
    }
    
    /**
     * 构建消息列表
     */
    private List<Map<String, Object>> buildMessages(ChatRequest request, Conversation conversation) {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // 如果有完整的 messages，直接使用
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            return request.getMessages();
        }
        
        // 否则构建消息列表
        String action = request.getAction() != null ? request.getAction() : "chat";
        String systemPrompt = "analyze".equals(action) ? PromptConstants.SYSTEM_PROMPT : PromptConstants.CHAT_SYSTEM_PROMPT;
        
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        
        // 加载历史记录
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            messages.addAll(request.getHistory().stream()
                    .map(h -> (Map<String, Object>) new HashMap<>(h))
                    .collect(Collectors.toList()));
        } else {
            // 从数据库加载历史
            List<Message> historyMessages = conversationService.getMessages(conversation.getId(), 20);
            for (Message msg : historyMessages) {
                Map<String, Object> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                messages.add(m);
            }
        }
        
        // 添加当前用户消息（如果还没有的话）
        if (request.getContent() != null && !request.getContent().isEmpty()) {
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getContent());
            messages.add(userMessage);
        }
        
        return messages;
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
