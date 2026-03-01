package com.extension.backend.dto;

import lombok.Data;

/**
 * 用户登录请求 DTO
 */
@Data
public class LoginRequest {
    
    private String email;
    
    private String password;
}
