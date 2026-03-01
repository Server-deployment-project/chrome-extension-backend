package com.extension.backend.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 视觉请求 DTO
 */
@Data
public class VisionRequest {
    
    private String action; // "analyze" or "chat"
    
    private String image; // 图片 URL 或 Base64
    
    private String content; // 用户的问题
    
    private List<Map<String, Object>> messages; // 完整的消息列表
    
    private List<Map<String, String>> history; // 历史对话记录
    
    private String model; // 可选：指定模型
    
    private Double temperature = 0.7;
    
    private Integer maxTokens = 4096;
    
    private String conversationId; // 会话 ID
}
