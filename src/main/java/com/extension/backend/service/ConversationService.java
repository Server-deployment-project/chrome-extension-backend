package com.extension.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.extension.backend.entity.Conversation;
import com.extension.backend.entity.Message;
import com.extension.backend.mapper.ConversationMapper;
import com.extension.backend.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 会话服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {
    
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    
    /**
     * 创建新会话
     */
    public Conversation createConversation(Long userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setTitle(title != null ? title : "新会话");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        
        conversationMapper.insert(conversation);
        return conversation;
    }
    
    /**
     * 获取用户的会话
     */
    public Conversation getConversation(String conversationId, Long userId) {
        QueryWrapper<Conversation> wrapper = new QueryWrapper<>();
        wrapper.eq("id", conversationId).eq("user_id", userId);
        return conversationMapper.selectOne(wrapper);
    }
    
    /**
     * 获取用户的所有会话
     */
    public List<Conversation> getUserConversations(Long userId) {
        QueryWrapper<Conversation> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("updated_at");
        return conversationMapper.selectList(wrapper);
    }
    
    /**
     * 添加消息
     */
    public Message addMessage(String conversationId, String role, String content) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        
        messageMapper.insert(message);
        
        // 更新会话的 updated_at
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
        
        return message;
    }
    
    /**
     * 获取会话的消息历史（最近 N 条）
     */
    public List<Message> getMessages(String conversationId, int limit) {
        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("conversation_id", conversationId)
                .orderByDesc("created_at")
                .last("LIMIT " + limit);
        
        List<Message> messages = messageMapper.selectList(wrapper);
        // 反转顺序，使其按时间正序排列
        java.util.Collections.reverse(messages);
        return messages;
    }
    
    /**
     * 删除会话
     */
    public void deleteConversation(String conversationId, Long userId) {
        QueryWrapper<Conversation> wrapper = new QueryWrapper<>();
        wrapper.eq("id", conversationId).eq("user_id", userId);
        conversationMapper.delete(wrapper);
    }
}
