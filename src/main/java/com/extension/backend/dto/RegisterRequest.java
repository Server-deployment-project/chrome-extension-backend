package com.extension.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 用户注册请求 DTO
 */
@Data
public class RegisterRequest {

    private String email;

    private String password;

    @JsonProperty("confirm_password")
    private String confirmPassword;
}
