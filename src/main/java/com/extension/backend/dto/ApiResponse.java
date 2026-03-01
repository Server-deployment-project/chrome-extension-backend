package com.extension.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private String message;
    
    private T data;
    
    private String error;
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, null);
    }
    
    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(null, null, error);
    }
}
