package com.moyun.ext.cms.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.CmsNotificationQuery;
import com.moyun.ext.cms.domain.vo.CmsNotificationVO;
import com.moyun.ext.cms.service.ICmsNotificationService;
import com.moyun.portal.domain.entity.PortalNotification;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalNotificationMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CMS通知服务实现类
 *
 * @author moyun
 */
@Service
public class CmsNotificationServiceImpl implements ICmsNotificationService
{
    @Autowired
    private PortalNotificationMapper portalNotificationMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Override
    public Page<CmsNotificationVO> selectNotificationPage(CmsNotificationQuery query)
    {
        Page<PortalNotification> page = new Page<>(query.getPageNum(), query.getPageSize());
        page = portalNotificationMapper.selectPage(page, buildQueryWrapper(query));
        
        Page<CmsNotificationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<CmsNotificationVO> voList = BeanUtil.copyToList(page.getRecords(), CmsNotificationVO.class);
        
        // 批量查询用户信息，避免N+1查询问题
        if (CollUtil.isNotEmpty(voList))
        {
            Set<Long> userIds = voList.stream()
                    .map(CmsNotificationVO::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            if (CollUtil.isNotEmpty(userIds))
            {
                List<PortalUser> users = portalUserMapper.selectBatchIds(userIds);
                Map<Long, String> userNicknameMap = users.stream()
                        .collect(Collectors.toMap(PortalUser::getId, PortalUser::getNickname));
                
                for (CmsNotificationVO vo : voList)
                {
                    if (vo.getUserId() != null)
                    {
                        vo.setUserNickname(userNicknameMap.get(vo.getUserId()));
                    }
                }
            }
        }
        
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<PortalNotification> selectNotificationList(CmsNotificationQuery query)
    {
        return portalNotificationMapper.selectList(buildQueryWrapper(query));
    }

    @Override
    public PortalNotification selectNotificationById(Long id)
    {
        return portalNotificationMapper.selectById(id);
    }

    @Override
    public int insertNotification(PortalNotification notification)
    {
        return portalNotificationMapper.insert(notification);
    }

    @Override
    public int updateNotification(PortalNotification notification)
    {
        return portalNotificationMapper.updateById(notification);
    }

    @Override
    public int deleteNotificationByIds(Long[] ids)
    {
        return portalNotificationMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int sendSystemNotification(PortalNotification notification)
    {
        // 查询所有启用的用户
        LambdaQueryWrapper<PortalUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(PortalUser::getStatus, "0");
        List<PortalUser> users = portalUserMapper.selectList(userWrapper);
        
        if (CollUtil.isEmpty(users))
        {
            return 0;
        }
        
        // 批量构建通知对象
        List<PortalNotification> notifications = new ArrayList<>();
        for (PortalUser user : users)
        {
            PortalNotification n = new PortalNotification();
            n.setUserId(user.getId());
            n.setType(notification.getType());
            n.setTitle(notification.getTitle());
            n.setContent(notification.getContent());
            n.setData(notification.getData());
            n.setIsRead(false);
            notifications.add(n);
        }
        
        // 批量插入
        int count = 0;
        for (PortalNotification n : notifications)
        {
            count += portalNotificationMapper.insert(n);
        }
        
        return count;
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<PortalNotification> buildQueryWrapper(CmsNotificationQuery query)
    {
        LambdaQueryWrapper<PortalNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtil.isNotNull(query.getUserId()), PortalNotification::getUserId, query.getUserId());
        wrapper.eq(ObjectUtil.isNotEmpty(query.getType()), PortalNotification::getType, query.getType());
        wrapper.eq(ObjectUtil.isNotNull(query.getIsRead()), PortalNotification::getIsRead, query.getIsRead());
        wrapper.orderByDesc(PortalNotification::getCreateTime);
        return wrapper;
    }
}
