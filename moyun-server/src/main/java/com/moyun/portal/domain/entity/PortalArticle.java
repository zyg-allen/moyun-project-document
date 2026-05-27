package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

@TableName("portal_article")
public class PortalArticle extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "文章标题不能为空")
    @Size(min = 0, max = 500, message = "文章标题长度不能超过500个字符")
    private String title;

    private String content;

    @Size(min = 0, max = 1000, message = "文章摘要长度不能超过1000个字符")
    private String excerpt;

    @Size(min = 0, max = 500, message = "封面URL长度不能超过500个字符")
    private String cover;

    @NotNull(message = "作者ID不能为空")
    private Long authorId;

    private Long categoryId;

    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    private Boolean isFeatured;

    private Boolean isTop;

    private Boolean isCarousel;

    private Long views;

    private Long likes;

    private Long comments;

    private Long shareCount;

    private Long bookmarkCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    @Size(min = 0, max = 500, message = "外部链接长度不能超过500个字符")
    private String link;

    @Size(min = 0, max = 20, message = "编辑器模式长度不能超过20个字符")
    private String editorMode;

    private String contentMarkdown;

    public PortalArticle()
    {
    }

    public PortalArticle(Long id)
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

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getExcerpt()
    {
        return excerpt;
    }

    public void setExcerpt(String excerpt)
    {
        this.excerpt = excerpt;
    }

    public String getCover()
    {
        return cover;
    }

    public void setCover(String cover)
    {
        this.cover = cover;
    }

    public Long getAuthorId()
    {
        return authorId;
    }

    public void setAuthorId(Long authorId)
    {
        this.authorId = authorId;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Boolean getIsFeatured()
    {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured)
    {
        this.isFeatured = isFeatured;
    }

    public Boolean getIsTop()
    {
        return isTop;
    }

    public void setIsTop(Boolean isTop)
    {
        this.isTop = isTop;
    }

    public Boolean getIsCarousel()
    {
        return isCarousel;
    }

    public void setIsCarousel(Boolean isCarousel)
    {
        this.isCarousel = isCarousel;
    }

    public Long getViews()
    {
        return views;
    }

    public void setViews(Long views)
    {
        this.views = views;
    }

    public Long getLikes()
    {
        return likes;
    }

    public void setLikes(Long likes)
    {
        this.likes = likes;
    }

    public Long getComments()
    {
        return comments;
    }

    public void setComments(Long comments)
    {
        this.comments = comments;
    }

    public Long getShareCount()
    {
        return shareCount;
    }

    public void setShareCount(Long shareCount)
    {
        this.shareCount = shareCount;
    }

    public Long getBookmarkCount()
    {
        return bookmarkCount;
    }

    public void setBookmarkCount(Long bookmarkCount)
    {
        this.bookmarkCount = bookmarkCount;
    }

    public LocalDateTime getPublishedAt()
    {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt)
    {
        this.publishedAt = publishedAt;
    }

    public String getLink()
    {
        return link;
    }

    public void setLink(String link)
    {
        this.link = link;
    }

    public String getEditorMode()
    {
        return editorMode;
    }

    public void setEditorMode(String editorMode)
    {
        this.editorMode = editorMode;
    }

    public String getContentMarkdown()
    {
        return contentMarkdown;
    }

    public void setContentMarkdown(String contentMarkdown)
    {
        this.contentMarkdown = contentMarkdown;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("title", getTitle())
            .append("content", getContent())
            .append("excerpt", getExcerpt())
            .append("cover", getCover())
            .append("authorId", getAuthorId())
            .append("categoryId", getCategoryId())
            .append("status", getStatus())
            .append("isFeatured", getIsFeatured())
            .append("isTop", getIsTop())
            .append("isCarousel", getIsCarousel())
            .append("views", getViews())
            .append("likes", getLikes())
            .append("comments", getComments())
            .append("shareCount", getShareCount())
            .append("bookmarkCount", getBookmarkCount())
            .append("publishedAt", getPublishedAt())
            .append("link", getLink())
            .append("editorMode", getEditorMode())
            .append("contentMarkdown", getContentMarkdown())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
