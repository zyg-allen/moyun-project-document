package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.domain.query.CategoryQuery;

/**
 * 门户分类 业务层
 *
 * @author moyun
 */
public interface IPortalCategoryService {

    /**
     * 根据条件分页查询分类列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalCategory> selectPortalCategoryPage(Page<PortalCategory> page, CategoryQuery query);

    /**
     * 根据条件查询分类列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 分类信息集合
     */
    List<PortalCategory> selectPortalCategoryList(CategoryQuery query);

    /**
     * 获取树形分类列表
     *
     * @param query 查询条件
     * @return 树形分类列表
     */
    List<PortalCategory> selectPortalCategoryTree(CategoryQuery query);

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

    /**
     * 获取分类的完整路径ID列表（从根到当前）
     *
     * @param categoryId 分类ID
     * @return 分类ID列表
     */
    List<Long> getCategoryPathIds(Long categoryId);

    /**
     * 获取分类的路径字符串（逗号分隔）
     *
     * @param categoryId 分类ID
     * @return 路径字符串，例如：1,3,5
     */
    String getCategoryPath(Long categoryId);

    /**
     * 获取顶级分类ID
     *
     * @param categoryId 分类ID
     * @return 顶级分类ID
     */
    Long getRootCategoryId(Long categoryId);
}
