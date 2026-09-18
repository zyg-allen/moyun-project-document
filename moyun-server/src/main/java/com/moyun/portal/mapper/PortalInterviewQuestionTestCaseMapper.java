package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalInterviewQuestionTestCase;

/**
 * 面试题目测试用例 Mapper（OJ 判题系统）
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewQuestionTestCaseMapper extends BaseMapper<PortalInterviewQuestionTestCase> {

    /**
     * 查询某题目全部用例（按 order_num 升序，用于判题）
     */

    List<PortalInterviewQuestionTestCase> selectByQuestionId(@Param("questionId") Long questionId);

    /**
     * 查询某题目的样例用例（is_sample=1，前端展示用，不泄露隐藏用例）
     */

    List<PortalInterviewQuestionTestCase> selectSamplesByQuestionId(@Param("questionId") Long questionId);

    /**
     * 统计某题目的用例总数
     */

    long countByQuestionId(@Param("questionId") Long questionId);

    /**
     * 统计某题目的样例用例数
     */

    long countSamplesByQuestionId(@Param("questionId") Long questionId);
}
