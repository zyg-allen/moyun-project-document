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
 * 动态事件流 portal_feed_event
 *
 * <p>说明：本表使用 created_time 作为时间字段（区别于 BaseEntity 的 create_time），
 * 因此将继承自 BaseEntity 的 create_time/update_time/create_by/update_by/remark 标记为非持久字段，
 * 避免 MyBatis-Plus 自动生成不存在的列引用。</p>
 *
 * @author moyun
 */
@Data
@TableName("portal_feed_event")
public class PortalFeedEvent extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件发布者 */
    private Long userId;

    /** 事件类型：publish_article/publish_experience/new_column/checkin 等 */
    private String eventType;

    /** 目标类型：article/experience/column/book 等 */
    private String targetType;

    /** 目标对象ID */
    private Long targetId;

    /** 目标标题 */
    private String title;

    /** 动态摘要 */
    private String summary;

    /** 封面图 */
    private String cover;

    /** 事件创建时间（对应 created_time 列） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // ===== 继承自 BaseEntity 的字段在 portal_feed_event 表中不存在，标记为非持久 =====
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

    public PortalFeedEvent() {
    }

    public PortalFeedEvent(Long id) {
        this.id = id;
    }
}
