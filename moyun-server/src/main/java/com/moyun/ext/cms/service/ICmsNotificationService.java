package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.CmsNotificationQuery;
import com.moyun.ext.cms.domain.vo.CmsNotificationVO;
import com.moyun.portal.domain.entity.PortalNotification;

import java.util.List;

/**
 * CMS通知服务接口
 *
 * @author moyun
 */
public interface ICmsNotificationService
{
    /**
     * 查询通知分页列表
     *
     * @param query 查询条件
     * @return 通知分页列表
     */
    Page<CmsNotificationVO> selectNotificationPage(CmsNotificationQuery query);

    /**
     * 查询通知列表
     *
     * @param query 查询条件
     * @return 通知列表
     */
    List<PortalNotification> selectNotificationList(CmsNotificationQuery query);

    /**
     * 查询通知详情
     *
     * @param id 通知ID
     * @return 通知信息
     */
    PortalNotification selectNotificationById(Long id);

    /**
     * 新增通知
     *
     * @param notification 通知信息
     * @return 结果
     */
    int insertNotification(PortalNotification notification);

    /**
     * 修改通知
     *
     * @param notification 通知信息
     * @return 结果
     */
    int updateNotification(PortalNotification notification);

    /**
     * 批量删除通知
     *
     * @param ids 需要删除的通知ID
     * @return 结果
     */
    int deleteNotificationByIds(Long[] ids);

    /**
     * 发送系统通知给所有用户
     *
     * @param notification 通知信息
     * @return 结果
     */
    int sendSystemNotification(PortalNotification notification);
}
