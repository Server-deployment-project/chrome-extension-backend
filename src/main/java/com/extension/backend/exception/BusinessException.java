package com.extension.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常工厂类 - 用于创建各种业务异常
 */
public class BusinessException {

    /**
     * 400 - 客户端错误（参数验证失败等）
     */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "CLIENT_ERROR", message);
    }

    /**
     * 401 - Token 无效或未提供（认证错误）
     */
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_ERROR", message);
    }

    /**
     * 403 - 权限错误（账号被禁用等）
     */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "PERMISSION_ERROR", message);
    }

    /**
     * 404 - 资源不存在（用户、会话等）
     */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND_ERROR", message);
    }

    /**
     * 429 - 速率限制（配额已用完）
     */
    public static ApiException rateLimitExceeded(String message) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded", message);
    }

    /**
     * 500 - 服务器内部错误
     */
    public static ApiException internalServerError(String message) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", message);
    }

    /**
     * 502 - 上游服务错误
     */
    public static ApiException badGateway(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "upstream_error", message);
    }
}
