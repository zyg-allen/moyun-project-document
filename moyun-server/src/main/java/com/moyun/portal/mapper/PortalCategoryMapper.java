package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.domain.query.CategoryQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 门户分类表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalCategoryMapper extends BaseMapper<PortalCategory> {

    /**
     * 根据条件分页查询分类列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分类信息集合信息
     */
    Page<PortalCategory> selectPortalCategoryPage(Page<PortalCategory> page, @Param("params") CategoryQuery query);

    /**
     * 根据条件查询分类列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 分类信息集合信息
     */
    List<PortalCategory> selectPortalCategoryList(@Param("params") CategoryQuery query);

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
