-- Chrome Extension Backend Database Schema
-- MySQL 5.7+

CREATE DATABASE IF NOT EXISTS chrome_extension_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chrome_extension_db;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE COMMENT '电子邮箱',
    password VARCHAR(128) COMMENT '加密密码',
    token VARCHAR(255) UNIQUE NOT NULL COMMENT '用户Token',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    daily_quota INT DEFAULT 30 COMMENT '每日配额',
    requests_today INT DEFAULT 0 COMMENT '今日请求次数',
    last_request_at DATETIME COMMENT '最后请求时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 自定义对话模型配置
    custom_text_api_key VARCHAR(512) COMMENT '对话API Key',
    custom_text_api_base VARCHAR(255) COMMENT '对话API Base',
    custom_text_model VARCHAR(100) COMMENT '对话模型ID',
    
    -- 自定义视觉模型配置
    custom_vision_api_key VARCHAR(512) COMMENT '视觉API Key',
    custom_vision_api_base VARCHAR(255) COMMENT '视觉API Base',
    custom_vision_model VARCHAR(100) COMMENT '视觉模型ID',
    
    INDEX idx_token (token),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 会话表
CREATE TABLE IF NOT EXISTS conversations (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) DEFAULT '新会话' COMMENT '会话标题',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id CHAR(36) NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant',
    content TEXT NOT NULL COMMENT '消息内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';
