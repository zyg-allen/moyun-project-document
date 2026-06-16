package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalInterviewQuestionLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 题目点赞 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewQuestionLikeMapper extends BaseMapper<PortalInterviewQuestionLike>
{
    /**
     * 查询用户是否已点赞某题目
     */
    public PortalInterviewQuestionLike selectLike(@Param("questionId") Long questionId, @Param("userId") Long userId);
}
