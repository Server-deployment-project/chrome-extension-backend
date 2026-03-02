package com.extension.backend.dto;

import org.springframework.http.HttpStatus;

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

    private Integer statusCode;

    private String code;

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setMessage(message);
        response.setData(data);
        response.setStatusCode(HttpStatus.OK.value());
        return response;
    }

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setMessage(message);
        response.setData(data);
        response.setStatusCode(status.value());
        return response;
    }

    public static <T> ApiResponse<T> error(String error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setError(error);
        response.setStatusCode(HttpStatus.OK.value());
        return response;
    }
}
