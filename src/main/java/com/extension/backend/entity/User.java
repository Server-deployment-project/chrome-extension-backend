package com.extension.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("users")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String email;
    
    private String password;
    
    private String token;
    
    private Boolean isActive;
    
    private Integer dailyQuota;
    
    private Integer requestsToday;
    
    private LocalDateTime lastRequestAt;
    
    private LocalDateTime createdAt;
    
    // 自定义对话模型配置
    private String customTextApiKey;
    
    private String customTextApiBase;
    
    private String customTextModel;
    
    // 自定义视觉模型配置
    private String customVisionApiKey;
    
    private String customVisionApiBase;
    
    private String customVisionModel;
    
    /**
     * 重置配额（如果需要）
     */
    public void resetQuotaIfNeeded() {
        LocalDateTime now = LocalDateTime.now();
        if (lastRequestAt != null && lastRequestAt.toLocalDate().isBefore(now.toLocalDate())) {
            this.requestsToday = 0;
        }
    }
    
    /**
     * 是否使用自定义文本配置
     * 必须同时配置apiKey、apiBase和model才算完整的自定义配置
     */
    public boolean hasCustomTextConfig() {
        return customTextApiKey != null && !customTextApiKey.isEmpty() 
                && customTextApiBase != null && !customTextApiBase.isEmpty()
                && customTextModel != null && !customTextModel.isEmpty();
    }
    
    /**
     * 是否使用自定义视觉配置
     * 必须同时配置apiKey、apiBase和model才算完整的自定义配置
     */
    public boolean hasCustomVisionConfig() {
        return customVisionApiKey != null && !customVisionApiKey.isEmpty() 
                && customVisionApiBase != null && !customVisionApiBase.isEmpty()
                && customVisionModel != null && !customVisionModel.isEmpty();
    }
}
