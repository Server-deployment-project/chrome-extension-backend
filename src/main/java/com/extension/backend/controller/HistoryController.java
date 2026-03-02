package com.extension.backend.controller;

import com.extension.backend.dto.ApiResponse;
import com.extension.backend.entity.Conversation;
import com.extension.backend.entity.Message;
import com.extension.backend.entity.User;
import com.extension.backend.exception.BusinessException;
import com.extension.backend.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
     * 获取用户的所有会话列表 - 返回 200
     */
    @GetMapping("/history")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getHistory(ServerWebExchange exchange) {
        User user = exchange.getAttribute("USER");
        if (user == null) {
            throw BusinessException.unauthorized("Invalid or inactive extension token");
        }

        return Mono.fromCallable(() -> {
            List<Conversation> conversations = conversationService.getUserConversations(user.getId());

            List<Map<String, Object>> result = conversations.stream().map(conv -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", conv.getId());
                map.put("title", conv.getTitle());
                map.put("created_at", conv.getCreatedAt().toString());
                map.put("updated_at", conv.getUpdatedAt().toString());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取指定会话的详细信息（包含消息） - 返回 200 或 404
     */
    @GetMapping("/history/{conversationId}")
    public Mono<ResponseEntity<Map<String, Object>>> getHistoryDetail(
            @PathVariable String conversationId,
            ServerWebExchange exchange) {

        User user = exchange.getAttribute("USER");
        if (user == null) {
            throw BusinessException.unauthorized("Invalid or inactive extension token");
        }

        return Mono.fromCallable(() -> {
            Conversation conversation = conversationService.getConversation(conversationId, user.getId());
            if (conversation == null) {
                throw BusinessException.notFound("Conversation not found");
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

            return ResponseEntity.ok(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除会话 - 返回 204
     */
    @DeleteMapping("/history/{conversationId}")
    public Mono<ResponseEntity<Void>> deleteHistory(
            @PathVariable String conversationId,
            ServerWebExchange exchange) {

        User user = exchange.getAttribute("USER");
        if (user == null) {
            throw BusinessException.unauthorized("Invalid or inactive extension token");
        }

        return Mono.fromCallable(() -> {
            conversationService.deleteConversation(conversationId, user.getId());
            return ResponseEntity.noContent().<Void>build();
        })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
