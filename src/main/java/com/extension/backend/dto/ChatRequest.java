package com.extension.backend.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 聊天请求 DTO
 */
@Data
public class ChatRequest {
    
    private String action; // "analyze" or "chat"
    
    private String content; // 用户输入的内容
    
    private List<Map<String, Object>> messages; // 完整的消息列表
    
    private List<Map<String, String>> history; // 历史对话记录
    
    private String model; // 可选：指定模型
    
    private Double temperature = 0.7; // 温度参数
    
    private Integer maxTokens = 4096; // 最大 token 数
    
    private String conversationId; // 会话 ID
}
