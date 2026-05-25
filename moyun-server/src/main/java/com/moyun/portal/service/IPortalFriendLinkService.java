package com.moyun.portal.service;

import com.moyun.portal.domain.entity.PortalFriendLink;

import java.util.List;

/**
 * 门户友情链接 业务层
 *
 * @author moyun
 */
public interface IPortalFriendLinkService {

    /**
     * 根据条件分页查询友情链接列表
     *
     * @param portalFriendLink 友情链接信息
     * @return 友情链接信息集合信息
     */
    public List<PortalFriendLink> selectPortalFriendLinkList(PortalFriendLink portalFriendLink);

    /**
     * 通过友情链接ID查询友情链接
     *
     * @param id 友情链接ID
     * @return 友情链接对象信息
     */
    public PortalFriendLink selectPortalFriendLinkById(Long id);

    /**
     * 新增友情链接信息
     *
     * @param portalFriendLink 友情链接信息
     * @return 结果
     */
    public int insertPortalFriendLink(PortalFriendLink portalFriendLink);

    /**
     * 修改友情链接信息
     *
     * @param portalFriendLink 友情链接信息
     * @return 结果
     */
    public int updatePortalFriendLink(PortalFriendLink portalFriendLink);

    /**
     * 通过友情链接ID删除友情链接
     *
     * @param id 友情链接ID
     * @return 结果
     */
    public int deletePortalFriendLinkById(Long id);

    /**
     * 批量删除友情链接信息
     *
     * @param ids 需要删除的友情链接ID
     * @return 结果
     */
    public int deletePortalFriendLinkByIds(Long[] ids);
}
