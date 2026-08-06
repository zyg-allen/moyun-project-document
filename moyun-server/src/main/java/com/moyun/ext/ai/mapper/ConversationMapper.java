package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话会话Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
