package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

@Data
@TableName("portal_comment")
public class PortalComment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务主键（前缀com_，用于跨表关联，避免自增id在TRUNCATE后错乱） */
    private String businessId;

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /** 文章业务主键（关联 portal_article.business_id，双轨过渡） */
    private String articleBusinessId;

    private Long authorId;

    /** 作者业务主键（关联 portal_user.business_id，双轨过渡） */
    private String authorBusinessId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Long parentId;

    /** 父评论业务主键（关联 portal_comment.business_id，双轨过渡） */
    private String parentBusinessId;

    private Long rootId;

    /** 根评论业务主键（关联 portal_comment.business_id，双轨过渡） */
    private String rootBusinessId;

    private Long replyTo;

    private String replyToContent;

    private Long likeCount;

    private String status;

    public PortalComment()
    {
    }

    public PortalComment(Long id)
    {
        this.id = id;
    }
}
