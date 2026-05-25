package com.moyun.portal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文章查询数据传输对象
 *
 * @author moyun
 */
@Data
@Schema(description = "文章查询DTO")
public class ArticleQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章标题（模糊查询）
     */
    @Size(max = 500, message = "文章标题长度不能超过500个字符")
    @Schema(description = "文章标题", example = "Spring")
    private String title;

    /**
     * 作者ID
     */
    @Schema(description = "作者ID", example = "1")
    private Long authorId;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    /**
     * 文章状态
     */
    @Schema(description = "文章状态", example = "published")
    private String status;

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
}
