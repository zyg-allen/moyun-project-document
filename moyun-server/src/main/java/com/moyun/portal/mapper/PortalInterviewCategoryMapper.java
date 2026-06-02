package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalInterviewCategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PortalInterviewCategoryMapper extends BaseMapper<PortalInterviewCategory>
{
    public List<PortalInterviewCategory> selectPortalInterviewCategoryList(PortalInterviewCategory portalInterviewCategory);

    public PortalInterviewCategory selectPortalInterviewCategoryById(Long id);

    public int insertPortalInterviewCategory(PortalInterviewCategory portalInterviewCategory);

    public int updatePortalInterviewCategory(PortalInterviewCategory portalInterviewCategory);

    public int deletePortalInterviewCategoryById(Long id);

    public int deletePortalInterviewCategoryByIds(Long[] ids);
}
