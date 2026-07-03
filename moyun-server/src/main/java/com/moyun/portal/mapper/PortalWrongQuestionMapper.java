package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.WrongQuestionQuery;
import com.moyun.ext.cms.domain.vo.WrongQuestionVO;
import com.moyun.portal.domain.entity.PortalWrongQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 错题本 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalWrongQuestionMapper extends BaseMapper<PortalWrongQuestion> {

    /**
     * 错题本分页列表（含题目简要信息，按状态/标签/关键词筛选）
     */
    Page<WrongQuestionVO> selectWrongQuestionPage(Page<WrongQuestionVO> page, @Param("userId") Long userId, @Param("query") WrongQuestionQuery query);

    /**
     * 查询某用户的最近错题（不含已掌握，倒序，最多 N 条）
     */
    @Select("SELECT wq.id, wq.user_id, wq.question_id, wq.attempt_id, wq.status, wq.wrong_count, " +
            "wq.last_wrong_time, wq.next_review_time, wq.created_time " +
            "FROM portal_wrong_question wq " +
            "WHERE wq.user_id = #{userId} AND wq.status != 'mastered' " +
            "ORDER BY wq.last_wrong_time DESC LIMIT #{limit}")
    List<PortalWrongQuestion> selectRecentWrong(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 统计某用户在指定状态下的错题数（status 为 null 时统计全部）
     */
    @Select("<script>SELECT COUNT(*) FROM portal_wrong_question WHERE user_id = #{userId} " +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if></script>")
    Long countByUserAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 统计某用户今日待复习错题数（next_review_time <= NOW() 且未掌握）
     */
    @Select("SELECT COUNT(*) FROM portal_wrong_question " +
            "WHERE user_id = #{userId} AND status != 'mastered' " +
            "AND next_review_time IS NOT NULL AND next_review_time <= NOW()")
    Long countTodayReview(@Param("userId") Long userId);

    /**
     * 标记题目已掌握（更新 status 为 mastered）
     */
    @Update("UPDATE portal_wrong_question SET status = 'mastered' WHERE user_id = #{userId} AND question_id = #{questionId}")
    int markMastered(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 查询某用户对某题的错题记录
     */
    @Select("SELECT * FROM portal_wrong_question WHERE user_id = #{userId} AND question_id = #{questionId}")
    PortalWrongQuestion selectByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);
}
