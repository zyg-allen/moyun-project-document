package com.moyun.portal.domain.entity;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 动态收件箱 portal_feed_inbox（推模式：关注者收件箱）
 *
 * <p>说明：本表使用 created_time 作为时间字段（区别于 BaseEntity 的 create_time），
 * 因此将继承自 BaseEntity 的 create_time/update_time/create_by/update_by/remark 标记为非持久字段，
 * 避免 MyBatis-Plus 自动生成不存在的列引用。</p>
 *
 * @author moyun
 */
@Data
@TableName("portal_feed_inbox")
public class PortalFeedInbox extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者 */
    private Long userId;

    /** 动态事件ID */
    private Long eventId;

    /** 入箱时间（对应 created_time 列） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // ===== 继承自 BaseEntity 的字段在 portal_feed_inbox 表中不存在，标记为非持久 =====
    @TableField(exist = false)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String createBy;

    @TableField(exist = false)
    private String updateBy;

    @TableField(exist = false)
    private String remark;

    // 覆盖 BaseEntity 的 delFlag：本表无 del_flag 列（迁移脚本排除），保持物理删除（toggle/流水语义）
    @TableField(exist = false)
    private String delFlag;

    public PortalFeedInbox() {
    }

    public PortalFeedInbox(Long id) {
        this.id = id;
    }
}
