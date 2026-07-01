package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalBookRecommend;
import com.moyun.portal.domain.query.BookRecommendQuery;

/**
 * 书籍推荐 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookRecommendMapper extends BaseMapper<PortalBookRecommend> {

    /**
     * 分页查询推荐（含书名，关联 portal_book）
     */
    Page<PortalBookRecommend> selectBookRecommendPage(Page<PortalBookRecommend> page, @Param("query") BookRecommendQuery query);

    /**
     * 列表查询推荐（含书名）
     */
    List<PortalBookRecommend> selectBookRecommendList(@Param("query") BookRecommendQuery query);

    /**
     * 按主键查询（含书名）
     */
    PortalBookRecommend selectBookRecommendById(@Param("id") Long id);

    int insertBookRecommend(PortalBookRecommend bookRecommend);

    int updateBookRecommend(PortalBookRecommend bookRecommend);

    int deleteBookRecommendById(@Param("id") Long id);

    int deleteBookRecommendByIds(@Param("ids") Long[] ids);

    /**
     * 前台用：查指定位置生效中的推荐（含时间窗过滤）
     */
    List<PortalBookRecommend> selectActiveByPosition(@Param("position") String position);

    /**
     * 批量更新排序
     */
    int batchUpdateSort(@Param("list") List<PortalBookRecommend> list);

    /**
     * 上下架切换
     */
    int toggleActive(@Param("id") Long id, @Param("isActive") Boolean isActive);
}
