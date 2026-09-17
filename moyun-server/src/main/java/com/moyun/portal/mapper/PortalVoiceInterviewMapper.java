package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.moyun.portal.domain.entity.PortalVoiceInterview;

/**
 * 语音面试会话主表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalVoiceInterviewMapper extends BaseMapper<PortalVoiceInterview> {
}
