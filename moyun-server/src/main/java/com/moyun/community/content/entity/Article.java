package com.moyun.community.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("cms_article")
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "article_id", type = IdType.AUTO)
    private Long articleId;

    private Long authorId;

    private Long categoryId;

    private String title;

    private String summary;

    private String coverUrl;

    private String content;

    private String contentType;

    private String articleType;

    private String sourceType;

    private String originalUrl;

    private Integer wordCount;

    private Integer readingTime;

    private String status;

    private Integer isTop;

    private Integer isRecommend;

    private Integer isDeleted;

    private Long viewCount;

    private Long likeCount;

    private Long collectCount;

    private Integer commentCount;

    private Integer shareCount;

    private Integer rewardCount;

    private Integer allowComment;

    private LocalDateTime publishTime;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime deleteTime;

    private LocalDateTime auditTime;

    private String auditReason;
}
