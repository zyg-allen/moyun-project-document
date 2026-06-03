package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户面试经验表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewExperienceMapper extends BaseMapper<PortalInterviewExperience>
{
    /**
     * 根据条件分页查询面试经验列表
     *
     * @param portalInterviewExperience 面试经验信息
     * @return 面试经验信息集合信息
     */
    public List<PortalInterviewExperience> selectPortalInterviewExperienceList(PortalInterviewExperience portalInterviewExperience);

    /**
     * 通过面试经验ID查询面试经验
     *
     * @param id 面试经验ID
     * @return 面试经验对象信息
     */
    public PortalInterviewExperience selectPortalInterviewExperienceById(Long id);

    /**
     * 新增面试经验信息
     *
     * @param portalInterviewExperience 面试经验信息
     * @return 结果
     */
    public int insertPortalInterviewExperience(PortalInterviewExperience portalInterviewExperience);

    /**
     * 修改面试经验信息
     *
     * @param portalInterviewExperience 面试经验信息
     * @return 结果
     */
    public int updatePortalInterviewExperience(PortalInterviewExperience portalInterviewExperience);

    /**
     * 通过面试经验ID删除面试经验
     *
     * @param id 面试经验ID
     * @return 结果
     */
    public int deletePortalInterviewExperienceById(Long id);

    /**
     * 批量删除面试经验信息
     *
     * @param ids 需要删除的面试经验ID
     * @return 结果
     */
    public int deletePortalInterviewExperienceByIds(Long[] ids);
}