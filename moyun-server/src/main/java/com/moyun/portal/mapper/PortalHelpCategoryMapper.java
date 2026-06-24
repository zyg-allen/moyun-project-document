package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalHelpCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 帮助中心分类 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalHelpCategoryMapper extends BaseMapper<PortalHelpCategory> {

    /**
     * 分页查询帮助分类
     */
    Page<PortalHelpCategory> selectHelpCategoryPage(Page<PortalHelpCategory> page, @Param("category") PortalHelpCategory category);

    /**
     * 查询帮助分类列表（不分页）
     */
    List<PortalHelpCategory> selectHelpCategoryList(@Param("category") PortalHelpCategory category);

    /**
     * 查询所有启用的分类（前台用）
     */
    List<PortalHelpCategory> selectActiveCategoryList();
}
