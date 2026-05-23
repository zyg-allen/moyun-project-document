package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_notification")
public class CmsNotification extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long notificationId;

    private Long userId;

    private String type;

    private String title;

    private String content;

    private String sourceType;

    private Long sourceId;

    private Long fromUserId;

    private Integer isRead;

    private String readTime;
}