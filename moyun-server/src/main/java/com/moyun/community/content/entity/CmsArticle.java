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
@TableName("cms_article")
public class CmsArticle extends BaseEntity {

    @TableId(type = IdType.AUTO)
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

    private Date publishTime;

    private Date auditTime;

    private String auditReason;
}
