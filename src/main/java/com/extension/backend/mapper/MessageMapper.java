package com.extension.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.extension.backend.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息 Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
