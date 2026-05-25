package com.moyun.portal.service;

import com.moyun.portal.domain.PortalCategory;

import java.util.List;

/**
 * 门户分类 业务层
 *
 * @author moyun
 */
public interface IPortalCategoryService {

    /**
     * 根据条件分页查询分类列表
     *
     * @param portalCategory 分类信息
     * @return 分类信息集合信息
     */
    public List<PortalCategory> selectPortalCategoryList(PortalCategory portalCategory);

    /**
     * 通过分类ID查询分类
     *
     * @param id 分类ID
     * @return 分类对象信息
     */
    public PortalCategory selectPortalCategoryById(Long id);

    /**
     * 新增分类信息
     *
     * @param portalCategory 分类信息
     * @return 结果
     */
    public int insertPortalCategory(PortalCategory portalCategory);

    /**
     * 修改分类信息
     *
     * @param portalCategory 分类信息
     * @return 结果
     */
    public int updatePortalCategory(PortalCategory portalCategory);

    /**
     * 通过分类ID删除分类
     *
     * @param id 分类ID
     * @return 结果
     */
    public int deletePortalCategoryById(Long id);

    /**
     * 批量删除分类信息
     *
     * @param ids 需要删除的分类ID
     * @return 结果
     */
    public int deletePortalCategoryByIds(Long[] ids);
}
