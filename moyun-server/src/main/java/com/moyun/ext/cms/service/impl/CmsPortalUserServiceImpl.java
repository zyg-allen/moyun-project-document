package com.moyun.ext.cms.service.impl;

import java.util.Arrays;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.ext.cms.domain.query.CmsPortalUserQuery;
import com.moyun.ext.cms.domain.vo.CmsPortalUserVO;
import com.moyun.ext.cms.service.ICmsPortalUserService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;

/**
 * CMS门户用户服务实现类
 *
 * @author moyun
 */
@Service
public class CmsPortalUserServiceImpl implements ICmsPortalUserService
{
    @Autowired
    private PortalUserMapper portalUserMapper;

    @Override
    public Page<CmsPortalUserVO> selectUserPage(Page<CmsPortalUserVO> page, CmsPortalUserQuery query)
    {
        Page<PortalUser> entityPage = new Page<>(page.getCurrent(), page.getSize());
        entityPage = portalUserMapper.selectPage(entityPage, buildQueryWrapper(query));
        
        Page<CmsPortalUserVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<CmsPortalUserVO> voList = BeanUtil.copyToList(entityPage.getRecords(), CmsPortalUserVO.class);
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<PortalUser> selectUserList(CmsPortalUserQuery query)
    {
        return portalUserMapper.selectList(buildQueryWrapper(query));
    }

    @Override
    public PortalUser selectUserById(Long id)
    {
        return portalUserMapper.selectById(id);
    }

    @Override
    public int insertUser(PortalUser user)
    {
        return portalUserMapper.insert(user);
    }

    @Override
    public int updateUser(PortalUser user)
    {
        return portalUserMapper.updateById(user);
    }

    @Override
    public int changeStatus(PortalUser user)
    {
        PortalUser updateUser = new PortalUser();
        updateUser.setId(user.getId());
        updateUser.setStatus(user.getStatus());
        return portalUserMapper.updateById(updateUser);
    }

    @Override
    public int deleteUserByIds(Long[] ids)
    {
        return portalUserMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public int resetUserPwd(PortalUser user)
    {
        PortalUser updateUser = new PortalUser();
        updateUser.setId(user.getId());
        updateUser.setPassword(user.getPassword());
        return portalUserMapper.updateById(updateUser);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<PortalUser> buildQueryWrapper(CmsPortalUserQuery query)
    {
        LambdaQueryWrapper<PortalUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ObjectUtil.isNotEmpty(query.getUsername()), PortalUser::getUsername, query.getUsername());
        wrapper.like(ObjectUtil.isNotEmpty(query.getNickname()), PortalUser::getNickname, query.getNickname());
        wrapper.like(ObjectUtil.isNotEmpty(query.getEmail()), PortalUser::getEmail, query.getEmail());
        wrapper.like(ObjectUtil.isNotEmpty(query.getPhone()), PortalUser::getPhone, query.getPhone());
        wrapper.eq(ObjectUtil.isNotEmpty(query.getStatus()), PortalUser::getStatus, query.getStatus());
        wrapper.orderByDesc(PortalUser::getCreateTime);
        return wrapper;
    }
}
