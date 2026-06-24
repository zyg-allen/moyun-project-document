package com.moyun.portal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 文章发布数据传输对象
 *
 * @author moyun
 */
@Data
@Schema(description = "文章发布DTO")
public class ArticlePublishDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章标题
     */
    @NotBlank(message = "文章标题不能为空")
    @Size(min = 1, max = 500, message = "文章标题长度必须在1-500个字符之间")
    @Schema(description = "文章标题", example = "Spring Boot 最佳实践")
    private String title;

    /**
     * 文章内容
     */
    @NotBlank(message = "文章内容不能为空")
    @Schema(description = "文章内容", example = "这是一篇关于Spring Boot的文章...")
    private String content;

    /**
     * 文章摘要
     */
    @Size(max = 1000, message = "文章摘要长度不能超过1000个字符")
    @Schema(description = "文章摘要", example = "本文介绍了Spring Boot的核心特性...")
    private String excerpt;

    /**
     * 封面图片URL或Base64
     */
    @Size(max = 10485760, message = "封面长度不能超过10MB")
    @Schema(description = "封面图片URL或Base64", example = "https://example.com/cover.jpg")
    private String cover;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    /**
     * 标签ID列表
     */
    @Schema(description = "标签ID列表", example = "[1, 2, 3]")
    private List<Long> tagIds;

    /**
     * 标签名称列表（用于自动创建新标签，与 tagIds 合并去重）
     */
    @Schema(description = "标签名称列表", example = "[\"Spring Boot\", \"Java\"]")
    private List<String> tagNames;

    /**
     * 是否设为精选
     */
    @Schema(description = "是否设为精选", example = "false")
    private Boolean isFeatured = false;

    /**
     * 是否置顶
     */
    @Schema(description = "是否置顶", example = "false")
    private Boolean isTop = false;

    /**
     * 是否加入轮播
     */
    @Schema(description = "是否加入轮播", example = "false")
    private Boolean isCarousel = false;
}
