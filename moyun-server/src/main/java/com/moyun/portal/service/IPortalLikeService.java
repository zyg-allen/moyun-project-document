package com.moyun.portal.service;

import com.moyun.portal.domain.PortalLike;

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
     * @param portalLike 点赞信息
     * @return 点赞信息集合信息
     */
    public List<PortalLike> selectPortalLikeList(PortalLike portalLike);

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
