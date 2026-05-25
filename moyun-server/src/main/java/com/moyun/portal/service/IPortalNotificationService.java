package com.moyun.portal.service;

import com.moyun.portal.domain.PortalNotification;

import java.util.List;

/**
 * 门户通知 业务层
 *
 * @author moyun
 */
public interface IPortalNotificationService {

    /**
     * 根据条件分页查询通知列表
     *
     * @param portalNotification 通知信息
     * @return 通知信息集合信息
     */
    public List<PortalNotification> selectPortalNotificationList(PortalNotification portalNotification);

    /**
     * 通过通知ID查询通知
     *
     * @param id 通知ID
     * @return 通知对象信息
     */
    public PortalNotification selectPortalNotificationById(Long id);

    /**
     * 新增通知信息
     *
     * @param portalNotification 通知信息
     * @return 结果
     */
    public int insertPortalNotification(PortalNotification portalNotification);

    /**
     * 修改通知信息
     *
     * @param portalNotification 通知信息
     * @return 结果
     */
    public int updatePortalNotification(PortalNotification portalNotification);

    /**
     * 通过通知ID删除通知
     *
     * @param id 通知ID
     * @return 结果
     */
    public int deletePortalNotificationById(Long id);

    /**
     * 批量删除通知信息
     *
     * @param ids 需要删除的通知ID
     * @return 结果
     */
    public int deletePortalNotificationByIds(Long[] ids);
}
