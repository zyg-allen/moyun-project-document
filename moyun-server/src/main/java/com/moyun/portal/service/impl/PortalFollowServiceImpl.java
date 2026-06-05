package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalFollow;
import com.moyun.portal.domain.query.FollowQuery;
import com.moyun.portal.mapper.PortalFollowMapper;
import com.moyun.portal.service.IPortalFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户关注 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalFollowServiceImpl extends ServiceImpl<PortalFollowMapper, PortalFollow> implements IPortalFollowService {

    @Autowired
    private PortalFollowMapper portalFollowMapper;

    /**
     * 根据条件分页查询关注列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalFollow> selectPortalFollowPage(Page<PortalFollow> page, FollowQuery query) {
        return baseMapper.selectPortalFollowPage(page, query);
    }

    /**
     * 根据条件查询关注列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 关注信息集合
     */
    @Override
    public List<PortalFollow> selectPortalFollowList(FollowQuery query) {
        return baseMapper.selectPortalFollowList(query);
    }

    /**
     * 通过关注ID查询关注
     *
     * @param id 关注ID
     * @return 关注对象信息
     */
    @Override
    public PortalFollow selectPortalFollowById(Long id) {
        return portalFollowMapper.selectPortalFollowById(id);
    }

    /**
     * 新增关注信息
     *
     * @param portalFollow 关注信息
     * @return 结果
     */
    @Override
    public int insertPortalFollow(PortalFollow portalFollow) {
        return portalFollowMapper.insertPortalFollow(portalFollow);
    }

    /**
     * 修改关注信息
     *
     * @param portalFollow 关注信息
     * @return 结果
     */
    @Override
    public int updatePortalFollow(PortalFollow portalFollow) {
        return portalFollowMapper.updatePortalFollow(portalFollow);
    }

    /**
     * 通过关注ID删除关注
     *
     * @param id 关注ID
     * @return 结果
     */
    @Override
    public int deletePortalFollowById(Long id) {
        return portalFollowMapper.deletePortalFollowById(id);
    }

    /**
     * 批量删除关注信息
     *
     * @param ids 需要删除的关注ID
     * @return 结果
     */
    @Override
    public int deletePortalFollowByIds(Long[] ids) {
        return portalFollowMapper.deletePortalFollowByIds(ids);
    }
}
