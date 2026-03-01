package com.extension.backend.dto;

import lombok.Data;

/**
 * 用户注册请求 DTO
 */
@Data
public class RegisterRequest {
    
    private String email;
    
    private String password;
    
    private String confirmPassword;
}
