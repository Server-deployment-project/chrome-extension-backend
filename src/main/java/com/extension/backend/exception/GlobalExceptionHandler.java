package com.extension.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.extension.backend.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 API 业务异常
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        log.error("API Exception: code={}, message={}", ex.getCode(), ex.getMessage());

        ApiResponse<Void> response = new ApiResponse<>();
        response.setError(ex.getMessage());
        response.setStatusCode(ex.getStatus().value());
        response.setCode(ex.getCode());

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    /**
     * 处理其他运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime Exception: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = new ApiResponse<>();
        response.setError(ex.getMessage());
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setCode("SERVER_ERROR");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Exception: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = new ApiResponse<>();
        response.setError("服务器内部错误");
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setCode("SERVER_ERROR");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
