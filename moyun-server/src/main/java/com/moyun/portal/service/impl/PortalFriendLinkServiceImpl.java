package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.PortalFriendLink;
import com.moyun.portal.mapper.PortalFriendLinkMapper;
import com.moyun.portal.service.IPortalFriendLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户友情链接 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalFriendLinkServiceImpl extends ServiceImpl<PortalFriendLinkMapper, PortalFriendLink> implements IPortalFriendLinkService {

    @Autowired
    private PortalFriendLinkMapper portalFriendLinkMapper;

    /**
     * 根据条件分页查询友情链接列表
     *
     * @param portalFriendLink 友情链接信息
     * @return 友情链接信息集合信息
     */
    @Override
    public List<PortalFriendLink> selectPortalFriendLinkList(PortalFriendLink portalFriendLink) {
        return portalFriendLinkMapper.selectPortalFriendLinkList(portalFriendLink);
    }

    /**
     * 通过友情链接ID查询友情链接
     *
     * @param id 友情链接ID
     * @return 友情链接对象信息
     */
    @Override
    public PortalFriendLink selectPortalFriendLinkById(Long id) {
        return portalFriendLinkMapper.selectPortalFriendLinkById(id);
    }

    /**
     * 新增友情链接信息
     *
     * @param portalFriendLink 友情链接信息
     * @return 结果
     */
    @Override
    public int insertPortalFriendLink(PortalFriendLink portalFriendLink) {
        return portalFriendLinkMapper.insertPortalFriendLink(portalFriendLink);
    }

    /**
     * 修改友情链接信息
     *
     * @param portalFriendLink 友情链接信息
     * @return 结果
     */
    @Override
    public int updatePortalFriendLink(PortalFriendLink portalFriendLink) {
        return portalFriendLinkMapper.updatePortalFriendLink(portalFriendLink);
    }

    /**
     * 通过友情链接ID删除友情链接
     *
     * @param id 友情链接ID
     * @return 结果
     */
    @Override
    public int deletePortalFriendLinkById(Long id) {
        return portalFriendLinkMapper.deletePortalFriendLinkById(id);
    }

    /**
     * 批量删除友情链接信息
     *
     * @param ids 需要删除的友情链接ID
     * @return 结果
     */
    @Override
    public int deletePortalFriendLinkByIds(Long[] ids) {
        return portalFriendLinkMapper.deletePortalFriendLinkByIds(ids);
    }
}
