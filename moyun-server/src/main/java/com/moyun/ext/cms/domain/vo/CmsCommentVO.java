package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;

import java.time.LocalDateTime;

/**
 * 评论视图对象
 *
 * @author moyun
 */
public class CmsCommentVO extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 评论ID */
    private Long id;

    /** 文章ID */
    private Long articleId;

    /** 文章标题 */
    private String articleTitle;

    /** 评论者ID */
    private Long authorId;

    /** 评论者昵称 */
    private String authorNickname;

    /** 评论者头像 */
    private String authorAvatar;

    /** 评论内容 */
    private String content;

    /** 父评论ID */
    private Long parentId;

    /** 回复目标ID */
    private Long replyTo;

    /** 点赞数 */
    private Long likeCount;

    /** 状态 */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

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

    public String getArticleTitle()
    {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle)
    {
        this.articleTitle = articleTitle;
    }

    public Long getAuthorId()
    {
        return authorId;
    }

    public void setAuthorId(Long authorId)
    {
        this.authorId = authorId;
    }

    public String getAuthorNickname()
    {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname)
    {
        this.authorNickname = authorNickname;
    }

    public String getAuthorAvatar()
    {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar)
    {
        this.authorAvatar = authorAvatar;
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

    public Long getReplyTo()
    {
        return replyTo;
    }

    public void setReplyTo(Long replyTo)
    {
        this.replyTo = replyTo;
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
    public LocalDateTime getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(LocalDateTime createTime)
    {
        this.createTime = createTime;
    }
}
