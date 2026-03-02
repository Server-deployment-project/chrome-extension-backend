package com.extension.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.extension.backend.dto.ApiResponse;
import com.extension.backend.dto.LoginRequest;
import com.extension.backend.dto.RegisterRequest;
import com.extension.backend.entity.User;
import com.extension.backend.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.java.com.extension.backend.exception.BusinessException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
     * 用户注册 - 返回 201
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> register(@RequestBody RegisterRequest request) {
        return Mono.fromCallable(() -> {
            // 参数校验
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                throw BusinessException.badRequest("邮箱不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw BusinessException.badRequest("密码不能为空");
            }
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw BusinessException.badRequest("两次输入的密码不一致");
            }

            // 注册用户
            User user = userService.register(request.getEmail(), request.getPassword());

            Map<String, String> data = new HashMap<>();
            data.put("token", user.getToken());

            ApiResponse<Map<String, String>> response = ApiResponse.success(HttpStatus.CREATED, "注册成功", data);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 用户登录 - 返回 200
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> login(@RequestBody LoginRequest request) {
        return Mono.fromCallable(() -> {
            // 参数校验
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                throw BusinessException.badRequest("邮箱不能为空");
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw BusinessException.badRequest("密码不能为空");
            }

            // 登录验证
            User user = userService.login(request.getEmail(), request.getPassword());

            Map<String, String> data = new HashMap<>();
            data.put("token", user.getToken());

            ApiResponse<Map<String, String>> response = ApiResponse.success("登录成功", data);
            return ResponseEntity.ok(response);
        })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 重置密码 - 返回 200
     */
    @PostMapping("/reset-password")
    public Mono<ResponseEntity<ApiResponse<Void>>> resetPassword(@RequestBody Map<String, String> request) {
        return Mono.fromCallable(() -> {
            String email = request.get("email");
            String newPassword = request.get("new_password");

            if (email == null || email.isEmpty()) {
                throw BusinessException.badRequest("邮箱不能为空");
            }
            if (newPassword == null || newPassword.isEmpty()) {
                throw BusinessException.badRequest("新密码不能为空");
            }

            userService.resetPassword(email, newPassword);

            ApiResponse<Void> response = ApiResponse.success("密码重置成功", null);
            return ResponseEntity.ok(response);
        })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
