package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

@Data
@TableName("portal_article")
public class PortalArticle extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "文章标题不能为空")
    @Size(min = 0, max = 500, message = "文章标题长度不能超过500个字符")
    private String title;

    /**
     * 文章URL别名，用于SEO语义化路径
     * 例如：spring-boot-best-practice
     */
    @Size(min = 0, max = 500, message = "文章别名长度不能超过500个字符")
    private String slug;

    private String content;

    @Size(min = 0, max = 1000, message = "文章摘要长度不能超过1000个字符")
    private String excerpt;

    @Size(min = 0, max = 10485760, message = "封面长度不能超过10MB")
    private String cover;

    private Long authorId;

    private Long categoryId;

    /**
     * 顶级分类ID
     */
    private Long rootCategoryId;

    /**
     * 分类路径，用逗号分隔，包含所有祖先分类ID，例如：1,3,5
     */
    @Size(min = 0, max = 500, message = "分类路径长度不能超过500个字符")
    private String categoryPath;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String categorySlug;

    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    private Boolean isFeatured;

    private Boolean isTop;

    private Boolean isCarousel;

    private Boolean isCategoryRecommended;

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

    /** 作者昵称 */
    @TableField(exist = false)
    private String authorNickname;

    /** 作者用户名 */
    @TableField(exist = false)
    private String authorUsername;

    /** 作者头像 */
    @TableField(exist = false)
    private String authorAvatar;

}
