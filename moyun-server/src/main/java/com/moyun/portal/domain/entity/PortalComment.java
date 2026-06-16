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

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    private Long authorId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Long parentId;

    private Long rootId;

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
