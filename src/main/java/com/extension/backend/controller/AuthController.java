package com.extension.backend.controller;

import com.extension.backend.dto.ApiResponse;
import com.extension.backend.dto.LoginRequest;
import com.extension.backend.dto.RegisterRequest;
import com.extension.backend.entity.User;
import com.extension.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 - 注册、登录、重置密码
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Chrome Extension Backend is running");
        return Mono.just(response);
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Mono<ApiResponse<Map<String, String>>> register(@RequestBody RegisterRequest request) {
        return Mono.fromCallable(() -> {
            // 参数校验
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                throw new RuntimeException("邮箱不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new RuntimeException("密码不能为空");
            }
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new RuntimeException("两次输入的密码不一致");
            }
            
            // 注册用户
            User user = userService.register(request.getEmail(), request.getPassword());
            
            Map<String, String> data = new HashMap<>();
            data.put("token", user.getToken());
            
            return ApiResponse.success("注册成功", data);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(error -> {
            log.error("Register error: ", error);
            return Mono.just(ApiResponse.error(error.getMessage()));
        });
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Mono<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        return Mono.fromCallable(() -> {
            // 参数校验
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                throw new RuntimeException("邮箱不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new RuntimeException("密码不能为空");
            }
            
            // 登录验证
            User user = userService.login(request.getEmail(), request.getPassword());
            
            Map<String, String> data = new HashMap<>();
            data.put("token", user.getToken());
            
            return ApiResponse.success("登录成功", data);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(error -> {
            log.error("Login error: ", error);
            return Mono.just(ApiResponse.error(error.getMessage()));
        });
    }
    
    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Mono<ApiResponse<Void>> resetPassword(@RequestBody Map<String, String> request) {
        return Mono.fromCallable(() -> {
            String email = request.get("email");
            String newPassword = request.get("new_password");
            
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("邮箱不能为空");
            }
            if (newPassword == null || newPassword.isEmpty()) {
                throw new RuntimeException("新密码不能为空");
            }
            
            userService.resetPassword(email, newPassword);
            
            return ApiResponse.<Void>success("密码重置成功", null);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(error -> {
            log.error("Reset password error: ", error);
            return Mono.just(ApiResponse.error(error.getMessage()));
        });
    }
}
