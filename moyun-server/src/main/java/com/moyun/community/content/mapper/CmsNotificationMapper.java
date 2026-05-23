package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CmsNotificationMapper extends BaseMapper<CmsNotification> {

    List<CmsNotification> selectNotificationList(@Param("userId") Long userId);

    List<CmsNotification> selectUnreadNotificationList(@Param("userId") Long userId);

    int markAsRead(@Param("notificationId") Long notificationId);

    int markAllAsRead(@Param("userId") Long userId);

    int getUnreadCount(@Param("userId") Long userId);
}