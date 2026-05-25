package com.moyun.portal.service;

import com.moyun.portal.domain.entity.PortalFollow;

import java.util.List;

/**
 * 门户关注 业务层
 *
 * @author moyun
 */
public interface IPortalFollowService {

    /**
     * 根据条件分页查询关注列表
     *
     * @param portalFollow 关注信息
     * @return 关注信息集合信息
     */
    public List<PortalFollow> selectPortalFollowList(PortalFollow portalFollow);

    /**
     * 通过关注ID查询关注
     *
     * @param id 关注ID
     * @return 关注对象信息
     */
    public PortalFollow selectPortalFollowById(Long id);

    /**
     * 新增关注信息
     *
     * @param portalFollow 关注信息
     * @return 结果
     */
    public int insertPortalFollow(PortalFollow portalFollow);

    /**
     * 通过关注ID删除关注
     *
     * @param id 关注ID
     * @return 结果
     */
    public int deletePortalFollowById(Long id);

    /**
     * 修改关注信息
     *
     * @param portalFollow 关注信息
     * @return 结果
     */
    public int updatePortalFollow(PortalFollow portalFollow);

    /**
     * 批量删除关注信息
     *
     * @param ids 需要删除的关注ID
     * @return 结果
     */
    public int deletePortalFollowByIds(Long[] ids);
}
