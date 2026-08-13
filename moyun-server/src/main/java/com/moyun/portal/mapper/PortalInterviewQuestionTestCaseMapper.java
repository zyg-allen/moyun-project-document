package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.moyun.portal.domain.entity.PortalInterviewQuestionTestCase;

/**
 * 面试题目测试用例 Mapper（v6.3 OJ 判题系统）
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewQuestionTestCaseMapper extends BaseMapper<PortalInterviewQuestionTestCase> {

    /**
     * 查询某题目全部用例（按 order_num 升序，用于判题）
     */
    @Select("SELECT * FROM portal_interview_question_test_case " +
            "WHERE question_id = #{questionId} ORDER BY order_num ASC, id ASC")
    List<PortalInterviewQuestionTestCase> selectByQuestionId(@Param("questionId") Long questionId);

    /**
     * 查询某题目的样例用例（is_sample=1，前端展示用，不泄露隐藏用例）
     */
    @Select("SELECT * FROM portal_interview_question_test_case " +
            "WHERE question_id = #{questionId} AND is_sample = 1 " +
            "ORDER BY order_num ASC, id ASC")
    List<PortalInterviewQuestionTestCase> selectSamplesByQuestionId(@Param("questionId") Long questionId);

    /**
     * 统计某题目的用例总数
     */
    @Select("SELECT COUNT(*) FROM portal_interview_question_test_case WHERE question_id = #{questionId}")
    long countByQuestionId(@Param("questionId") Long questionId);

    /**
     * 统计某题目的样例用例数
     */
    @Select("SELECT COUNT(*) FROM portal_interview_question_test_case " +
            "WHERE question_id = #{questionId} AND is_sample = 1")
    long countSamplesByQuestionId(@Param("questionId") Long questionId);
}
