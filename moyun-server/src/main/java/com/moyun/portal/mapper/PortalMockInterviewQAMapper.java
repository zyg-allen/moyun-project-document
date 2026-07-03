package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalMockInterviewQA;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模拟面试问答 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalMockInterviewQAMapper extends BaseMapper<PortalMockInterviewQA> {

    /**
     * 按面试会话ID查询全部问答（按题目序号升序）
     */
    @Select("SELECT * FROM portal_mock_interview_qa WHERE interview_id = #{interviewId} ORDER BY question_idx ASC")
    List<PortalMockInterviewQA> selectByInterviewId(@Param("interviewId") Long interviewId);
}
