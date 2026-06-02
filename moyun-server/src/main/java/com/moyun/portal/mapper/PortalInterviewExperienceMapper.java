package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PortalInterviewExperienceMapper extends BaseMapper<PortalInterviewExperience>
{
    public List<PortalInterviewExperience> selectPortalInterviewExperienceList(PortalInterviewExperience portalInterviewExperience);

    public PortalInterviewExperience selectPortalInterviewExperienceById(Long id);

    public int insertPortalInterviewExperience(PortalInterviewExperience portalInterviewExperience);

    public int updatePortalInterviewExperience(PortalInterviewExperience portalInterviewExperience);

    public int deletePortalInterviewExperienceById(Long id);

    public int deletePortalInterviewExperienceByIds(Long[] ids);
}
