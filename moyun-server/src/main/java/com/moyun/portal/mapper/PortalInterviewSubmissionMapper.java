package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 原子更新精选状态（后台采纳/取消采纳笔记）
     *
     * @param id           提交记录ID
     * @param isFeatured   是否精选
     * @return 受影响行数
     */
    @Update("UPDATE portal_interview_submission SET is_featured = #{isFeatured}, " +
            "featured_time = CASE WHEN #{isFeatured} = 1 THEN NOW() ELSE NULL END WHERE id = #{id}")
    int updateFeatured(@Param("id") Long id, @Param("isFeatured") boolean isFeatured);

    /**
     * 查询某题目的精选笔记列表
     *
     * @param questionId 题目ID
     * @return 精选提交记录列表
     */
    @Select("SELECT * FROM portal_interview_submission WHERE question_id = #{questionId} " +
            "AND is_featured = 1 AND note IS NOT NULL AND note != '' ORDER BY featured_time DESC")
    List<PortalInterviewSubmission> selectFeaturedByQuestion(@Param("questionId") Long questionId);
}
