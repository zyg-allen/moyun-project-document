package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalInterviewResumeTemplate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PortalInterviewResumeTemplateMapper extends BaseMapper<PortalInterviewResumeTemplate>
{
    public List<PortalInterviewResumeTemplate> selectPortalInterviewResumeTemplateList(PortalInterviewResumeTemplate portalInterviewResumeTemplate);

    public PortalInterviewResumeTemplate selectPortalInterviewResumeTemplateById(Long id);

    public int insertPortalInterviewResumeTemplate(PortalInterviewResumeTemplate portalInterviewResumeTemplate);

    public int updatePortalInterviewResumeTemplate(PortalInterviewResumeTemplate portalInterviewResumeTemplate);

    public int deletePortalInterviewResumeTemplateById(Long id);

    public int deletePortalInterviewResumeTemplateByIds(Long[] ids);
}
