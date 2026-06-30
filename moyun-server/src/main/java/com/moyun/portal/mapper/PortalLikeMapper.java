package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.moyun.portal.domain.entity.PortalLike;

/**
 * 门户点赞表 数据层
 *
 * <p>说明：原 PortalLikeController / IPortalLikeService / PortalLikeServiceImpl 已删除
 * （通用 CRUD 接口前端从未调用，文章点赞由 PortalArticleController.toggleLikeArticle 承担）。
 * 本 Mapper 保留，因 PortalArticleController 直接注入并使用 BaseMapper 通用方法
 * （selectOne / insert / deleteById / selectCount）操作 portal_like 表。</p>
 *
 * @author moyun
 */
@Mapper
public interface PortalLikeMapper extends BaseMapper<PortalLike> {
}
