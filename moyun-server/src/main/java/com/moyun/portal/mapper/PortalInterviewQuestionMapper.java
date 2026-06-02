package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PortalInterviewQuestionMapper extends BaseMapper<PortalInterviewQuestion>
{
    public List<PortalInterviewQuestion> selectPortalInterviewQuestionList(PortalInterviewQuestion portalInterviewQuestion);

    public PortalInterviewQuestion selectPortalInterviewQuestionById(Long id);

    public int insertPortalInterviewQuestion(PortalInterviewQuestion portalInterviewQuestion);

    public int updatePortalInterviewQuestion(PortalInterviewQuestion portalInterviewQuestion);

    public int deletePortalInterviewQuestionById(Long id);

    public int deletePortalInterviewQuestionByIds(Long[] ids);
}
