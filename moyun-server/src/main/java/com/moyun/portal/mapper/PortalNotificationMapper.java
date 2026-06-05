package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalNotification;
import com.moyun.portal.domain.query.NotificationQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 门户通知表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalNotificationMapper extends BaseMapper<PortalNotification> {

    /**
     * 根据条件分页查询通知列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 通知信息集合信息
     */
    Page<PortalNotification> selectPortalNotificationPage(Page<PortalNotification> page, @Param("params") NotificationQuery query);

    /**
     * 根据条件查询通知列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 通知信息集合信息
     */
    List<PortalNotification> selectPortalNotificationList(@Param("params") NotificationQuery query);

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
