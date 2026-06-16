package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalInterviewAttempt;

/**
 * 做题记录 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewAttemptMapper extends BaseMapper<PortalInterviewAttempt>
{
    /**
     * 查询用户对某题目的做题记录
     */
    public PortalInterviewAttempt selectAttempt(@Param("questionId") Long questionId, @Param("userId") Long userId);

    /**
     * 查询用户所有做题记录
     */
    public List<PortalInterviewAttempt> selectAttemptListByUserId(@Param("userId") Long userId);
}
