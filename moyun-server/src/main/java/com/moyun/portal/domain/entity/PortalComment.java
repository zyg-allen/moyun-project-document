package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getArticleId()
    {
        return articleId;
    }

    public void setArticleId(Long articleId)
    {
        this.articleId = articleId;
    }

    public Long getAuthorId()
    {
        return authorId;
    }

    public void setAuthorId(Long authorId)
    {
        this.authorId = authorId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Long getRootId()
    {
        return rootId;
    }

    public void setRootId(Long rootId)
    {
        this.rootId = rootId;
    }

    public Long getReplyTo()
    {
        return replyTo;
    }

    public void setReplyTo(Long replyTo)
    {
        this.replyTo = replyTo;
    }

    public String getReplyToContent()
    {
        return replyToContent;
    }

    public void setReplyToContent(String replyToContent)
    {
        this.replyToContent = replyToContent;
    }

    public Long getLikeCount()
    {
        return likeCount;
    }

    public void setLikeCount(Long likeCount)
    {
        this.likeCount = likeCount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("articleId", getArticleId())
            .append("authorId", getAuthorId())
            .append("content", getContent())
            .append("parentId", getParentId())
            .append("rootId", getRootId())
            .append("replyTo", getReplyTo())
            .append("replyToContent", getReplyToContent())
            .append("likeCount", getLikeCount())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
