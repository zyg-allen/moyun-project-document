package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_comment")
public class CmsComment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long commentId;

    private Long articleId;

    private Long userId;

    private Long parentId;

    private Long replyId;

    private Long replyUserId;

    private String content;

    private Integer likeCount;

    private String status;

    private String ipAddress;

    private String deviceType;
}