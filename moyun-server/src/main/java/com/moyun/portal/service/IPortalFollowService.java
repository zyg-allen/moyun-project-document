package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.portal.domain.entity.PortalFollow;
import com.moyun.portal.domain.query.FollowQuery;

/**
 * 门户关注 业务层
 *
 * @author moyun
 */
public interface IPortalFollowService {

    /**
     * 根据条件分页查询关注列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalFollow> selectPortalFollowPage(Page<PortalFollow> page, FollowQuery query);

    /**
     * 根据条件查询关注列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 关注信息集合
     */
    List<PortalFollow> selectPortalFollowList(FollowQuery query);

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
