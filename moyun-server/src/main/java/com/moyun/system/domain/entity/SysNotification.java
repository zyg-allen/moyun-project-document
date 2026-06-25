package com.moyun.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统通知主体表 sys_notification
 * 合并原 portal_notification + sys_notice，采用两表结构：
 * - sys_notification（主体表）：存内容，scope=user 个人 / scope=all 广播
 * - sys_notification_read（已读关系表）：存已读记录
 *
 * 注意：createTime/updateTime/createBy/updateBy/remark 继承自 BaseEntity，不要在子类重复声明
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notification")
public class SysNotification extends BaseEntity {

    /** 通知ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类型：system/comment/like/follow/order/notice/announcement */
    @Size(max = 50, message = "类型长度不能超过50个字符")
    private String type;

    /** 通知标题 */
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知数据（JSON格式） */
    private String data;

    /** 范围：user=个人通知 / all=全局广播 */
    private String scope;

    /** 接收用户ID（scope=user 时必填，scope=all 时为 NULL） */
    private Long userId;

    /** 接收用户类型：portal=门户用户 / sys=系统用户（scope=user 时标识 user_id 属于哪套用户体系） */
    private String userType;

    /** 通知/公告分类：1=通知 / 2=公告（兼容 sys_notice 字典 sys_notice_type） */
    private String noticeType;

    /** 状态：0=正常 / 1=关闭（兼容 sys_notice 字典 sys_notice_status） */
    private String status;

    /** 用户昵称（非持久字段，查询时关联填充） */
    @TableField(exist = false)
    private String userNickname;

    /** 是否已读（非持久字段，查询时通过 NOT EXISTS 计算） */
    @TableField(exist = false)
    @JsonProperty("isRead")
    private Boolean isRead;
}

