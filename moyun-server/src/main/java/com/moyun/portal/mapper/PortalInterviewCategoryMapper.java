package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.moyun.portal.domain.entity.PortalInterviewCategory;

/**
 * 门户面试分类表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewCategoryMapper extends BaseMapper<PortalInterviewCategory>
{
    /**
     * 根据条件分页查询面试分类列表
     *
     * @param portalInterviewCategory 面试分类信息
     * @return 面试分类信息集合信息
     */
    public List<PortalInterviewCategory> selectPortalInterviewCategoryList(PortalInterviewCategory portalInterviewCategory);

    /**
     * 通过面试分类ID查询面试分类
     *
     * @param id 面试分类ID
     * @return 面试分类对象信息
     */
    public PortalInterviewCategory selectPortalInterviewCategoryById(Long id);

    /**
     * 新增面试分类信息
     *
     * @param portalInterviewCategory 面试分类信息
     * @return 结果
     */
    public int insertPortalInterviewCategory(PortalInterviewCategory portalInterviewCategory);

    /**
     * 修改面试分类信息
     *
     * @param portalInterviewCategory 面试分类信息
     * @return 结果
     */
    public int updatePortalInterviewCategory(PortalInterviewCategory portalInterviewCategory);

    /**
     * 通过面试分类ID删除面试分类
     *
     * @param id 面试分类ID
     * @return 结果
     */
    public int deletePortalInterviewCategoryById(Long id);

    /**
     * 批量删除面试分类信息
     *
     * @param ids 需要删除的面试分类ID
     * @return 结果
     */
    public int deletePortalInterviewCategoryByIds(Long[] ids);
}