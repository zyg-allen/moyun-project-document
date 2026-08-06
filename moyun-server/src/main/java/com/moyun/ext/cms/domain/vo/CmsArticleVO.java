package com.moyun.ext.cms.domain.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 文章视图对象
 *
 * @author moyun
 */
@Data
public class CmsArticleVO extends BaseEntity {
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

    /** 作者用户名 */
    private String authorUsername;

    /** 作者头像 */
    private String authorAvatar;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 分类别名 */
    private String categorySlug;

    /** 顶级分类ID */
    private Long rootCategoryId;

    /** 分类路径，用逗号分隔，包含所有祖先分类ID */
    private String categoryPath;

    /** 是否分类推荐 */
    private Boolean isCategoryRecommended;

    /** 文章别名（SEO URL） */
    private String slug;

    /** 状态 */
    private String status;

    /** 审核人ID（sys_user.user_id） */
    private Long auditorId;

    /** 审核人昵称（sys_user.nick_name，CMS审核台展示用） */
    private String auditorNickname;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 审核意见/驳回原因（独立字段，审核台展示用） */
    private String auditRemark;

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

    /** 编辑会话标识（用于协作编辑/自动保存） */
    private String sessionToken;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 创建人 */
    private String createBy;

    /** 更新人 */
    private String updateBy;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
