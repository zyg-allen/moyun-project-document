package com.moyun.ext.cms.service;

import com.moyun.ext.cms.domain.query.CmsCategoryQuery;
import com.moyun.ext.cms.domain.vo.CmsCategoryVO;
import com.moyun.portal.domain.entity.PortalCategory;

import java.util.List;

/**
 * CMS分类服务接口
 *
 * @author moyun
 */
public interface ICmsCategoryService
{
    /**
     * 查询分类列表
     *
     * @param query 查询条件
     * @return 分类列表
     */
    List<CmsCategoryVO> selectCategoryList(CmsCategoryQuery query);

    /**
     * 查询分类树
     *
     * @param query 查询条件
     * @return 分类树
     */
    List<CmsCategoryVO> selectCategoryTree(CmsCategoryQuery query);

    /**
     * 查询分类详情
     *
     * @param id 分类ID
     * @return 分类信息
     */
    PortalCategory selectCategoryById(Long id);

    /**
     * 新增分类
     *
     * @param category 分类信息
     * @return 结果
     */
    int insertCategory(PortalCategory category);

    /**
     * 修改分类
     *
     * @param category 分类信息
     * @return 结果
     */
    int updateCategory(PortalCategory category);

    /**
     * 批量删除分类
     *
     * @param ids 需要删除的分类ID
     * @return 结果
     */
    int deleteCategoryByIds(Long[] ids);

    /**
     * 检查是否有子分类
     *
     * @param id 分类ID
     * @return 结果
     */
    boolean hasChildCategory(Long id);
}
