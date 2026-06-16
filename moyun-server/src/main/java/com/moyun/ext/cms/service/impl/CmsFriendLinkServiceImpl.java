package com.moyun.ext.cms.service.impl;

import java.util.Arrays;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.ext.cms.domain.query.CmsFriendLinkQuery;
import com.moyun.ext.cms.domain.vo.CmsFriendLinkVO;
import com.moyun.ext.cms.service.ICmsFriendLinkService;
import com.moyun.portal.domain.entity.PortalFriendLink;
import com.moyun.portal.mapper.PortalFriendLinkMapper;

@Service
public class CmsFriendLinkServiceImpl implements ICmsFriendLinkService
{
    @Autowired
    private PortalFriendLinkMapper portalFriendLinkMapper;

    @Override
    public Page<CmsFriendLinkVO> selectFriendLinkPage(Page<CmsFriendLinkVO> page, CmsFriendLinkQuery query)
    {
        Page<PortalFriendLink> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<PortalFriendLink> result = portalFriendLinkMapper.selectPage(entityPage, buildQueryWrapper(query));
        Page<CmsFriendLinkVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(BeanUtil.copyToList(result.getRecords(), CmsFriendLinkVO.class));
        return voPage;
    }

    @Override
    public List<CmsFriendLinkVO> selectFriendLinkList(CmsFriendLinkQuery query)
    {
        List<PortalFriendLink> list = portalFriendLinkMapper.selectList(buildQueryWrapper(query));
        return BeanUtil.copyToList(list, CmsFriendLinkVO.class);
    }

    @Override
    public CmsFriendLinkVO selectFriendLinkById(Long id)
    {
        PortalFriendLink friendLink = portalFriendLinkMapper.selectById(id);
        return BeanUtil.copyProperties(friendLink, CmsFriendLinkVO.class);
    }

    @Override
    public int insertFriendLink(PortalFriendLink friendLink)
    {
        return portalFriendLinkMapper.insert(friendLink);
    }

    @Override
    public int updateFriendLink(PortalFriendLink friendLink)
    {
        return portalFriendLinkMapper.updateById(friendLink);
    }

    @Override
    public int deleteFriendLinkByIds(Long[] ids)
    {
        return portalFriendLinkMapper.deleteBatchIds(Arrays.asList(ids));
    }

    private LambdaQueryWrapper<PortalFriendLink> buildQueryWrapper(CmsFriendLinkQuery query)
    {
        LambdaQueryWrapper<PortalFriendLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ObjectUtil.isNotEmpty(query.getName()), PortalFriendLink::getName, query.getName());
        wrapper.eq(ObjectUtil.isNotEmpty(query.getStatus()), PortalFriendLink::getStatus, query.getStatus());
        wrapper.orderByAsc(PortalFriendLink::getSort);
        wrapper.orderByDesc(PortalFriendLink::getCreateTime);
        return wrapper;
    }
}
