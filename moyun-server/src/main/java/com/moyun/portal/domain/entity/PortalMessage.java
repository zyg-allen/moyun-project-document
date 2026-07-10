package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

import com.moyun.core.base.BaseEntity;

/**
 * 私信消息对象 portal_message
 *
 * <p>说明：本表仅有 create_time 列，无 update_time / create_by / update_by / remark 列，
 * 因此将继承自 BaseEntity 的这些字段标记为非持久，
 * 避免 MyBatis-Plus 自动生成不存在的列引用，
 * 同时避免 MyMetaObjectHandler 自动填充不存在的列导致 INSERT 失败。</p>
 *
 * @author moyun
 */
@TableName("portal_message")
@Data
public class PortalMessage extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private Long sessionId;

    /** 发送者 */
    private Long senderId;

    /** 发送者类型 portal/sys */
    private String senderType;

    /** 接收者 */
    private Long receiverId;

    /** 接收者类型 portal/sys */
    private String receiverType;

    /** 消息内容 */
    private String content;

    /** 消息类型 text/image/file */
    private String msgType;

    /** 是否已读（0未读 1已读） */
    private Integer isRead;

    // ===== 继承自 BaseEntity 的字段在 portal_message 表中不存在，标记为非持久 =====
    // 表中仅有 create_time，无 update_time / create_by / update_by / remark
    @TableField(exist = false)
    private String createBy;

    @TableField(exist = false)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String updateBy;

    @TableField(exist = false)
    private String remark;

    public PortalMessage() {
    }

    public PortalMessage(Long id) {
        this.id = id;
    }
}
