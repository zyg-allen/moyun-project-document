package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalNotification;
import com.moyun.portal.domain.query.NotificationQuery;
import com.moyun.portal.mapper.PortalNotificationMapper;
import com.moyun.portal.service.IPortalNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户通知 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalNotificationServiceImpl extends ServiceImpl<PortalNotificationMapper, PortalNotification> implements IPortalNotificationService {

    @Autowired
    private PortalNotificationMapper portalNotificationMapper;

    /**
     * 根据条件分页查询通知列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalNotification> selectPortalNotificationPage(Page<PortalNotification> page, NotificationQuery query) {
        return baseMapper.selectPortalNotificationPage(page, query);
    }

    /**
     * 根据条件查询通知列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 通知信息集合
     */
    @Override
    public List<PortalNotification> selectPortalNotificationList(NotificationQuery query) {
        return baseMapper.selectPortalNotificationList(query);
    }

    /**
     * 通过通知ID查询通知
     *
     * @param id 通知ID
     * @return 通知对象信息
     */
    @Override
    public PortalNotification selectPortalNotificationById(Long id) {
        return portalNotificationMapper.selectPortalNotificationById(id);
    }

    /**
     * 新增通知信息
     *
     * @param portalNotification 通知信息
     * @return 结果
     */
    @Override
    public int insertPortalNotification(PortalNotification portalNotification) {
        return portalNotificationMapper.insertPortalNotification(portalNotification);
    }

    /**
     * 修改通知信息
     *
     * @param portalNotification 通知信息
     * @return 结果
     */
    @Override
    public int updatePortalNotification(PortalNotification portalNotification) {
        return portalNotificationMapper.updatePortalNotification(portalNotification);
    }

    /**
     * 通过通知ID删除通知
     *
     * @param id 通知ID
     * @return 结果
     */
    @Override
    public int deletePortalNotificationById(Long id) {
        return portalNotificationMapper.deletePortalNotificationById(id);
    }

    /**
     * 批量删除通知信息
     *
     * @param ids 需要删除的通知ID
     * @return 结果
     */
    @Override
    public int deletePortalNotificationByIds(Long[] ids) {
        return portalNotificationMapper.deletePortalNotificationByIds(ids);
    }
}
