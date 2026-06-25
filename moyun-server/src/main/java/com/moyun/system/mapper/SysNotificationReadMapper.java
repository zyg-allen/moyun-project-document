package com.moyun.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.system.domain.entity.SysNotificationRead;

/**
 * 系统通知用户已读关系表 数据层
 *
 * @author moyun
 */
@Mapper
public interface SysNotificationReadMapper extends BaseMapper<SysNotificationRead> {

    /**
     * 标记单条通知已读（INSERT IGNORE 防重复）
     *
     * @param notificationId 通知ID
     * @param userId         用户ID
     * @param userType       用户类型：portal=门户用户 / sys=系统用户
     */
    int markAsRead(@Param("notificationId") Long notificationId,
                   @Param("userId") Long userId,
                   @Param("userType") String userType);

    /**
     * 标记用户所有未读通知为已读（批量 INSERT IGNORE）
     * 将用户未读的个人通知 + 广播通知一次性写入已读关系表
     *
     * @param userId   用户ID
     * @param userType 用户类型：portal=门户用户 / sys=系统用户
     */
    int markAllAsRead(@Param("userId") Long userId,
                      @Param("userType") String userType);

    /**
     * 检查用户是否已读某条通知
     *
     * @param notificationId 通知ID
     * @param userId         用户ID
     * @param userType       用户类型：portal=门户用户 / sys=系统用户
     */
    int countByNotificationAndUser(@Param("notificationId") Long notificationId,
                                   @Param("userId") Long userId,
                                   @Param("userType") String userType);
}
