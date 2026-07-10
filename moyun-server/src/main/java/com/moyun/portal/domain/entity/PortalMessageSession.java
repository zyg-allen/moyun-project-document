package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

import java.time.LocalDateTime;

/**
 * 私信会话对象 portal_message_session
 *
 * <p>约定：user_a 永远是较小 ID，user_b 是较大 ID，保证唯一索引 uk_users 不重复。</p>
 *
 * <p>说明：本表仅有 create_time / update_time 列，无 create_by / update_by / remark 列，
 * 因此将继承自 BaseEntity 的这些字段标记为非持久，
 * 避免 MyBatis-Plus 自动生成不存在的列引用导致 SQL 异常。</p>
 *
 * @author moyun
 */
@TableName("portal_message_session")
@Data
public class PortalMessageSession extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户A（较小ID） */
    private Long userA;

    /** A方用户类型 portal/sys */
    private String userAType;

    /** 用户B（较大ID） */
    private Long userB;

    /** B方用户类型 portal/sys */
    private String userBType;

    /** 最后一条消息ID */
    private Long lastMessageId;

    /** 最后消息内容预览 */
    private String lastMessageContent;

    /** 最后消息时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageTime;

    /** A未读数 */
    private Integer unreadA;

    /** B未读数 */
    private Integer unreadB;

    // ===== 继承自 BaseEntity 的字段在 portal_message_session 表中不存在，标记为非持久 =====
    // 表中仅有 create_time / update_time，无 create_by / update_by / remark
    @TableField(exist = false)
    private String createBy;

    @TableField(exist = false)
    private String updateBy;

    @TableField(exist = false)
    private String remark;
}
