package com.moyun.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.portal.domain.entity.PortalNotification;
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
     * @param portalNotification 通知信息
     * @return 通知信息集合信息
     */
    @Override
    public List<PortalNotification> selectPortalNotificationList(PortalNotification portalNotification) {
        return portalNotificationMapper.selectPortalNotificationList(portalNotification);
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
