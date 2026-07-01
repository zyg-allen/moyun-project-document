package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalBookRecommend;
import com.moyun.portal.domain.query.BookRecommendQuery;

/**
 * 书籍推荐 业务层接口
 *
 * @author moyun
 */
public interface IPortalBookRecommendService extends IService<PortalBookRecommend> {

    Page<PortalBookRecommend> selectBookRecommendPage(Page<PortalBookRecommend> page, BookRecommendQuery query);

    List<PortalBookRecommend> selectBookRecommendList(BookRecommendQuery query);

    PortalBookRecommend selectBookRecommendById(Long id);

    int insertBookRecommend(PortalBookRecommend bookRecommend);

    int updateBookRecommend(PortalBookRecommend bookRecommend);

    int deleteBookRecommendById(Long id);

    int deleteBookRecommendByIds(Long[] ids);

    /**
     * 前台用：查指定位置生效中的推荐（含时间窗过滤）
     */
    List<PortalBookRecommend> selectActiveByPosition(String position);

    /**
     * 批量更新排序
     */
    int batchUpdateSort(List<PortalBookRecommend> list);

    /**
     * 上下架切换
     */
    int toggleActive(Long id, Boolean isActive);
}
