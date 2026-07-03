package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.ColumnQuery;
import com.moyun.ext.cms.domain.vo.ColumnArticleSortItem;
import com.moyun.ext.cms.domain.vo.ColumnListItemVO;
import com.moyun.ext.cms.domain.vo.ColumnVO;
import com.moyun.portal.domain.entity.PortalColumn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 专栏 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalColumnMapper extends BaseMapper<PortalColumn> {

    /**
     * 专栏公开列表分页（仅 published，含作者信息）
     */
    Page<ColumnListItemVO> selectListPage(Page<ColumnListItemVO> page, @Param("query") ColumnQuery query);

    /**
     * 后台专栏列表分页（含所有状态，含作者信息）
     * 支持 keyword（标题/副标题）/status/categoryId 筛选
     */
    Page<ColumnListItemVO> selectAdminListPage(Page<ColumnListItemVO> page, @Param("query") ColumnQuery query);

    /**
     * 专栏详情（含作者信息，不含文章目录与订阅状态）
     */
    ColumnVO selectDetailById(@Param("id") Long id);

    /**
     * 我创建的专栏分页（含所有状态）
     */
    Page<ColumnListItemVO> selectMyColumnsPage(Page<ColumnListItemVO> page, @Param("userId") Long userId);

    /**
     * 我订阅的专栏分页
     */
    Page<ColumnListItemVO> selectSubscribedColumnsPage(Page<ColumnListItemVO> page, @Param("userId") Long userId);

    /**
     * 原子更新文章数（递减时使用 GREATEST 防止出现负数）
     */
    @Update("UPDATE portal_column SET article_count = GREATEST(article_count + #{delta}, 0) WHERE id = #{id}")
    int updateArticleCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子更新订阅数（递减时使用 GREATEST 防止出现负数）
     */
    @Update("UPDATE portal_column SET subscribe_count = GREATEST(subscribe_count + #{delta}, 0) WHERE id = #{id}")
    int updateSubscribeCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子更新浏览数
     */
    @Update("UPDATE portal_column SET view_count = view_count + #{delta} WHERE id = #{id}")
    int updateViewCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 统计用户名下专栏数量
     */
    @Select("SELECT COUNT(*) FROM portal_column WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 批量更新专栏内文章排序（按 article_id 匹配，单条 CASE WHEN 语句）
     */
    int updateArticleSort(@Param("columnId") Long columnId, @Param("items") List<ColumnArticleSortItem> items);
}
