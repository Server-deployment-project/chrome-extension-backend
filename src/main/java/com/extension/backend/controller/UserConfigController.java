package com.extension.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import com.extension.backend.dto.ApiResponse;
import com.extension.backend.dto.UserConfigRequest;
import com.extension.backend.entity.User;
import com.extension.backend.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.java.com.extension.backend.exception.BusinessException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 用户配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserConfigController {

    private final UserService userService;

    /**
     * 获取用户配置 - 返回 200
     */
    @GetMapping("/config")
    public Mono<Map<String, Object>> getConfig(ServerWebExchange exchange) {
        User user = exchange.getAttribute("USER");
        if (user == null) {
            throw BusinessException.unauthorized("Invalid or inactive extension token");
        }

        return Mono.fromCallable(() -> {
            // 重置配额
            user.resetQuotaIfNeeded();
            userService.updateUser(user);

            Map<String, Object> response = new HashMap<>();

            // 文本配置
            Map<String, String> textConfig = new HashMap<>();
            if (user.getCustomTextApiKey() != null && user.getCustomTextApiKey().length() > 8) {
                textConfig.put("api_key", user.getCustomTextApiKey().substring(0, 4) + "..." +
                        user.getCustomTextApiKey().substring(user.getCustomTextApiKey().length() - 4));
            } else {
                textConfig.put("api_key", "");
            }
            textConfig.put("api_base", user.getCustomTextApiBase() != null ? user.getCustomTextApiBase() : "");
            textConfig.put("model", user.getCustomTextModel() != null ? user.getCustomTextModel() : "");
            response.put("text", textConfig);

            // 视觉配置
            Map<String, String> visionConfig = new HashMap<>();
            if (user.getCustomVisionApiKey() != null && user.getCustomVisionApiKey().length() > 8) {
                visionConfig.put("api_key", user.getCustomVisionApiKey().substring(0, 4) + "..." +
                        user.getCustomVisionApiKey().substring(user.getCustomVisionApiKey().length() - 4));
            } else {
                visionConfig.put("api_key", "");
            }
            visionConfig.put("api_base", user.getCustomVisionApiBase() != null ? user.getCustomVisionApiBase() : "");
            visionConfig.put("model", user.getCustomVisionModel() != null ? user.getCustomVisionModel() : "");
            response.put("vision", visionConfig);

            // 配额信息
            Map<String, Integer> quota = new HashMap<>();
            quota.put("used", user.getRequestsToday());
            quota.put("total", user.getDailyQuota());
            response.put("quota", quota);

            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新用户配置 - 返回 200
     */
    @PostMapping("/config")
    public Mono<ResponseEntity<ApiResponse<Void>>> updateConfig(@RequestBody UserConfigRequest request,
            ServerWebExchange exchange) {
        User user = exchange.getAttribute("USER");
        if (user == null) {
            throw BusinessException.unauthorized("Invalid or inactive extension token");
        }

        return Mono.fromCallable(() -> {
            // 更新文本配置
            if (request.getText() != null) {
                UserConfigRequest.TextConfig textConfig = request.getText();
                if (textConfig.getApiKey() != null && !textConfig.getApiKey().contains("...")) {
                    user.setCustomTextApiKey(textConfig.getApiKey().isEmpty() ? null : textConfig.getApiKey());
                }
                if (textConfig.getApiBase() != null) {
                    user.setCustomTextApiBase(textConfig.getApiBase());
                }
                if (textConfig.getModel() != null) {
                    user.setCustomTextModel(textConfig.getModel());
                }
            }

            // 更新视觉配置
            if (request.getVision() != null) {
                UserConfigRequest.VisionConfig visionConfig = request.getVision();
                if (visionConfig.getApiKey() != null && !visionConfig.getApiKey().contains("...")) {
                    user.setCustomVisionApiKey(visionConfig.getApiKey().isEmpty() ? null : visionConfig.getApiKey());
                }
                if (visionConfig.getApiBase() != null) {
                    user.setCustomVisionApiBase(visionConfig.getApiBase());
                }
                if (visionConfig.getModel() != null) {
                    user.setCustomVisionModel(visionConfig.getModel());
                }
            }

            userService.updateUser(user);

            ApiResponse<Void> response = ApiResponse.success("配置已同步", null);
            return ResponseEntity.ok(response);
        })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
