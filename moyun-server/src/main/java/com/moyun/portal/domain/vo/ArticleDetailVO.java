package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情视图对象
 *
 * @author moyun
 */
@Data
@Schema(description = "文章详情VO")
public class ArticleDetailVO implements Serializable {

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
     * 文章内容
     */
    @Schema(description = "文章内容", example = "这是一篇关于Spring Boot的文章...")
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
    @Schema(description = "作者昵称", example = "John Doe")
    private String authorNickname;

    /**
     * 作者头像
     */
    @Schema(description = "作者头像", example = "https://example.com/avatar.jpg")
    private String authorAvatar;

    /**
     * 作者简介
     */
    @Schema(description = "作者简介", example = "资深Java开发者")
    private String authorBio;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

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
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "2024-01-01 12:00:00")
    private LocalDateTime updateTime;
}
