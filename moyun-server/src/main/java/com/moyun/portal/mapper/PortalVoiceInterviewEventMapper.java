package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.moyun.portal.domain.entity.PortalVoiceInterviewEvent;

/**
 * 语音面试会话事件日志 数据层（V2重构）
 *
 * @author moyun
 */
@Mapper
public interface PortalVoiceInterviewEventMapper extends BaseMapper<PortalVoiceInterviewEvent> {
}
