package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;

import java.time.LocalDateTime;

/**
 * 文章视图对象
 *
 * @author moyun
 */
public class CmsArticleVO extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文章ID */
    private Long id;

    /** 文章标题 */
    private String title;

    /** 文章内容 */
    private String content;

    /** 文章摘要 */
    private String excerpt;

    /** 封面URL */
    private String cover;

    /** 作者ID */
    private Long authorId;

    /** 作者昵称 */
    private String authorNickname;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 状态 */
    private String status;

    /** 是否推荐 */
    private Boolean isFeatured;

    /** 是否置顶 */
    private Boolean isTop;

    /** 是否轮播 */
    private Boolean isCarousel;

    /** 阅读数 */
    private Long views;

    /** 点赞数 */
    private Long likes;

    /** 评论数 */
    private Long comments;

    /** 分享数 */
    private Long shareCount;

    /** 收藏数 */
    private Long bookmarkCount;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    /** 外部链接 */
    private String link;

    /** 编辑器模式 */
    private String editorMode;

    /** Markdown 内容 */
    private String contentMarkdown;

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

    public String getAuthorNickname()
    {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname)
    {
        this.authorNickname = authorNickname;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
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
}
