package com.extension.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.extension.backend.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
