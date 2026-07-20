package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 话题点赞
 *
 * @author moyun
 */
@Data
@TableName("portal_topic_like")
public class PortalTopicLike extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 话题 ID */
    private Long topicId;

    /** 点赞用户 ID */
    private Long userId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // BaseEntity 公共字段对应列在 portal_topic_like 表中不存在，排除 MyBatis-Plus 映射
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private LocalDateTime createTime;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String remark;
}
