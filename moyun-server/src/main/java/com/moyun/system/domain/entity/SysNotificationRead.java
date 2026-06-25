package com.moyun.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知用户已读关系表 sys_notification_read
 * 用于记录用户已读通知（广播通知的 per-user 已读状态）
 *
 * @author moyun
 */
@Data
@TableName("sys_notification_read")
public class SysNotificationRead {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 通知ID（关联 sys_notification.id） */
    private Long notificationId;

    /** 用户ID */
    private Long userId;

    /** 用户类型：portal=门户用户 / sys=系统用户 */
    private String userType;

    /** 阅读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
