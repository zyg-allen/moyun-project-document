package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalLike;
import com.moyun.portal.domain.query.LikeQuery;

import java.util.List;

/**
 * 门户点赞 业务层
 *
 * @author moyun
 */
public interface IPortalLikeService {

    /**
     * 根据条件分页查询点赞列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalLike> selectPortalLikePage(Page<PortalLike> page, LikeQuery query);

    /**
     * 根据条件查询点赞列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 点赞信息集合
     */
    List<PortalLike> selectPortalLikeList(LikeQuery query);

    /**
     * 通过点赞ID查询点赞
     *
     * @param id 点赞ID
     * @return 点赞对象信息
     */
    public PortalLike selectPortalLikeById(Long id);

    /**
     * 新增点赞信息
     *
     * @param portalLike 点赞信息
     * @return 结果
     */
    public int insertPortalLike(PortalLike portalLike);

    /**
     * 修改点赞信息
     *
     * @param portalLike 点赞信息
     * @return 结果
     */
    public int updatePortalLike(PortalLike portalLike);

    /**
     * 通过点赞ID删除点赞
     *
     * @param id 点赞ID
     * @return 结果
     */
    public int deletePortalLikeById(Long id);

    /**
     * 批量删除点赞信息
     *
     * @param ids 需要删除的点赞ID
     * @return 结果
     */
    public int deletePortalLikeByIds(Long[] ids);
}
