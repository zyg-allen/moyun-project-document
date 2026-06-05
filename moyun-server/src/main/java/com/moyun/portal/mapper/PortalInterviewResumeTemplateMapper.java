package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalInterviewResumeTemplate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户简历模板表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewResumeTemplateMapper extends BaseMapper<PortalInterviewResumeTemplate>
{
    /**
     * 根据条件分页查询简历模板列表
     *
     * @param portalInterviewResumeTemplate 简历模板信息
     * @return 简历模板信息集合信息
     */
    public List<PortalInterviewResumeTemplate> selectPortalInterviewResumeTemplateList(PortalInterviewResumeTemplate portalInterviewResumeTemplate);

    /**
     * 通过简历模板ID查询简历模板
     *
     * @param id 简历模板ID
     * @return 简历模板对象信息
     */
    public PortalInterviewResumeTemplate selectPortalInterviewResumeTemplateById(Long id);

    /**
     * 新增简历模板信息
     *
     * @param portalInterviewResumeTemplate 简历模板信息
     * @return 结果
     */
    public int insertPortalInterviewResumeTemplate(PortalInterviewResumeTemplate portalInterviewResumeTemplate);

    /**
     * 修改简历模板信息
     *
     * @param portalInterviewResumeTemplate 简历模板信息
     * @return 结果
     */
    public int updatePortalInterviewResumeTemplate(PortalInterviewResumeTemplate portalInterviewResumeTemplate);

    /**
     * 通过简历模板ID删除简历模板
     *
     * @param id 简历模板ID
     * @return 结果
     */
    public int deletePortalInterviewResumeTemplateById(Long id);

    /**
     * 批量删除简历模板信息
     *
     * @param ids 需要删除的简历模板ID
     * @return 结果
     */
    public int deletePortalInterviewResumeTemplateByIds(Long[] ids);
}