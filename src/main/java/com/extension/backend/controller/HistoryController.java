package com.extension.backend.controller;

import com.extension.backend.dto.ApiResponse;
import com.extension.backend.entity.Conversation;
import com.extension.backend.entity.Message;
import com.extension.backend.entity.User;
import com.extension.backend.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 历史记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HistoryController {
    
    private final ConversationService conversationService;
    
    /**
     * 获取用户的所有会话列表
     */
    @GetMapping("/history")
    public Mono<List<Map<String, Object>>> getHistory(ServerWebExchange exchange) {
        User user = exchange.getAttribute("USER");
        if (user == null) {
            return Mono.error(new RuntimeException("用户未认证"));
        }
        
        return Mono.fromCallable(() -> {
            List<Conversation> conversations = conversationService.getUserConversations(user.getId());
            
            return conversations.stream().map(conv -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", conv.getId());
                map.put("title", conv.getTitle());
                map.put("created_at", conv.getCreatedAt().toString());
                map.put("updated_at", conv.getUpdatedAt().toString());
                return map;
            }).collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 获取指定会话的详细信息（包含消息）
     */
    @GetMapping("/history/{conversationId}")
    public Mono<Map<String, Object>> getHistoryDetail(
            @PathVariable String conversationId,
            ServerWebExchange exchange) {
        
        User user = exchange.getAttribute("USER");
        if (user == null) {
            return Mono.error(new RuntimeException("用户未认证"));
        }
        
        return Mono.fromCallable(() -> {
            Conversation conversation = conversationService.getConversation(conversationId, user.getId());
            if (conversation == null) {
                throw new RuntimeException("会话不存在");
            }
            
            List<Message> messages = conversationService.getMessages(conversationId, 100);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", conversation.getId());
            response.put("title", conversation.getTitle());
            response.put("created_at", conversation.getCreatedAt().toString());
            response.put("updated_at", conversation.getUpdatedAt().toString());
            
            List<Map<String, Object>> messageList = messages.stream().map(msg -> {
                Map<String, Object> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                m.put("created_at", msg.getCreatedAt().toString());
                return m;
            }).collect(Collectors.toList());
            
            response.put("messages", messageList);
            
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/history/{conversationId}")
    public Mono<ApiResponse<Void>> deleteHistory(
            @PathVariable String conversationId,
            ServerWebExchange exchange) {
        
        User user = exchange.getAttribute("USER");
        if (user == null) {
            return Mono.just(ApiResponse.error("用户未认证"));
        }
        
        return Mono.fromCallable(() -> {
            conversationService.deleteConversation(conversationId, user.getId());
            return ApiResponse.<Void>success("会话已删除", null);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(error -> {
            log.error("Delete history error: ", error);
            return Mono.just(ApiResponse.error(error.getMessage()));
        });
    }
}
