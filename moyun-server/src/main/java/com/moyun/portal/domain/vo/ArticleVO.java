package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章视图对象
 *
 * @author moyun
 */
@Data
@Schema(description = "文章VO")
public class ArticleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    @Schema(description = "文章ID", example = "1")
    private Long id;

    /**
     * 文章标题
     */
    @Schema(description = "文章标题", example = "Spring Boot 最佳实践")
    private String title;

    /**
     * 文章URL别名，用于SEO语义化路径
     */
    @Schema(description = "文章URL别名", example = "spring-boot-best-practice")
    private String slug;

    /**
     * 文章内容
     */
    @Schema(description = "文章内容", example = "文章详细内容...")
    private String content;

    /**
     * 文章摘要
     */
    @Schema(description = "文章摘要", example = "本文介绍了Spring Boot的核心特性...")
    private String excerpt;

    /**
     * 封面图片URL
     */
    @Schema(description = "封面图片URL", example = "https://example.com/cover.jpg")
    private String cover;

    /**
     * 作者ID
     */
    @Schema(description = "作者ID", example = "1")
    private Long authorId;

    /**
     * 作者用户名
     */
    @Schema(description = "作者用户名", example = "john_doe")
    private String authorUsername;

    /**
     * 作者昵称
     */
    @Schema(description = "作者昵称", example = "约翰")
    private String authorNickname;

    /**
     * 作者头像
     */
    @Schema(description = "作者头像", example = "https://example.com/avatar.jpg")
    private String authorAvatar;

    /**
     * 作者简介
     */
    @Schema(description = "作者简介", example = "热爱写作，分享生活")
    private String authorBio;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    /**
     * 顶级分类ID
     */
    @Schema(description = "顶级分类ID", example = "1")
    private Long rootCategoryId;

    /**
     * 分类路径
     */
    @Schema(description = "分类路径", example = "1,3,5")
    private String categoryPath;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称", example = "技术")
    private String categoryName;

    /**
     * 标签列表
     */
    @Schema(description = "标签列表")
    private List<TagVO> tags;

    /**
     * 标签名称列表（简化版，用于前台显示）
     */
    @Schema(description = "标签名称列表", example = "[\"Java\", \"Spring\"]")
    private List<String> tagNames;

    /**
     * 状态（draft/pending/published/rejected/archived）
     */
    @Schema(description = "状态", example = "published")
    private String status;

    /**
     * 审核意见/驳回原因（审核驳回时填写，用于详情页展示给作者）
     */
    @Schema(description = "审核意见/驳回原因")
    private String remark;

    /**
     * 是否精选
     */
    @Schema(description = "是否精选", example = "true")
    private Boolean isFeatured;

    /**
     * 是否置顶
     */
    @Schema(description = "是否置顶", example = "false")
    private Boolean isTop;

    /**
     * 是否轮播
     */
    @Schema(description = "是否轮播", example = "false")
    private Boolean isCarousel;

    /**
     * 浏览量
     */
    @Schema(description = "浏览量", example = "1000")
    private Long views;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数", example = "100")
    private Long likes;

    /**
     * 评论数
     */
    @Schema(description = "评论数", example = "50")
    private Long comments;

    /**
     * 分享数
     */
    @Schema(description = "分享数", example = "20")
    private Long shareCount;

    /**
     * 收藏数
     */
    @Schema(description = "收藏数", example = "30")
    private Long bookmarkCount;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间", example = "2024-01-01 12:00:00")
    private LocalDateTime publishedAt;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    private LocalDateTime createTime;

    /**
     * 时间字段别名，兼容前台（createdAt 字段）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间（前台用）", example = "2024-01-01 12:00:00")
    private LocalDateTime createdAt;

    /**
     * 外部链接（用于广告或跳转链接）
     */
    @Schema(description = "外部链接", example = "https://example.com")
    private String link;

    /**
     * 编辑器模式（richtext/markdown）
     */
    @Schema(description = "编辑器模式", example = "richtext")
    private String editorMode;

    /**
     * 编辑会话标识（一次编辑会话唯一，用于草稿/发布幂等去重）
     */
    @Schema(description = "编辑会话标识")
    private String sessionToken;

    /**
     * Markdown 原始内容
     */
    @Schema(description = "Markdown 原始内容", example = "# 标题\n内容...")
    private String contentMarkdown;

    /**
     * 是否付费阅读 0=免费 1=付费
     */
    @Schema(description = "是否付费阅读", example = "0")
    private Integer isPaid;

    /**
     * 付费内容（购买后可见；未购买时后端会清空该字段）
     */
    @Schema(description = "付费内容（购买后可见）")
    private String paidContent;

    /**
     * 试读字数（未购买可预览的字数）
     */
    @Schema(description = "试读字数", example = "200")
    private Integer previewLength;

    /**
     * 付费价格，0=免费
     */
    @Schema(description = "付费价格", example = "9.90")
    private BigDecimal price;

    /**
     * 当前用户是否已购买该付费文章（非持久化，详情接口动态填充）
     */
    @Schema(description = "当前用户是否已购买", example = "false")
    private Boolean isPurchased;

    /**
     * 当前用户是否已点赞（非持久化，详情/列表接口动态填充；未登录为 false）
     */
    @Schema(description = "当前用户是否已点赞", example = "false")
    private Boolean isLiked;

    /**
     * 当前用户是否已收藏（非持久化，详情/列表接口动态填充；未登录为 false）
     */
    @Schema(description = "当前用户是否已收藏", example = "false")
    private Boolean isBookmarked;
}
