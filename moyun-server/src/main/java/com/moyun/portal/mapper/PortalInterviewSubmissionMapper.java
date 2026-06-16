package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalInterviewSubmission;

/**
 * 题目提交记录 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewSubmissionMapper extends BaseMapper<PortalInterviewSubmission>
{
    /**
     * 查询用户对某题目的最近提交
     */
    public List<PortalInterviewSubmission> selectSubmissionsByQuestionAndUser(@Param("questionId") Long questionId, @Param("userId") Long userId);

    /**
     * 查询某题目的提交数量（用于计算通过率）
     */
    public long countSubmissionsByQuestion(@Param("questionId") Long questionId);

    /**
     * 查询某题目的通过数量（用于计算通过率）
     */
    public long countSuccessByQuestion(@Param("questionId") Long questionId);
}
