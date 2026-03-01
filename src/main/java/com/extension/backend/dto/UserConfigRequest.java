package com.extension.backend.dto;

import lombok.Data;

/**
 * 用户配置请求 DTO
 */
@Data
public class UserConfigRequest {
    
    private TextConfig text;
    
    private VisionConfig vision;
    
    @Data
    public static class TextConfig {
        private String apiKey;
        private String apiBase;
        private String model;
    }
    
    @Data
    public static class VisionConfig {
        private String apiKey;
        private String apiBase;
        private String model;
    }
}
