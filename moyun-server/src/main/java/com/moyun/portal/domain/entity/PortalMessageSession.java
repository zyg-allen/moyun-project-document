package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

    /** 用户B（较大ID） */
    private Long userB;

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
}
