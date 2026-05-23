package com.moyun.community.content.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ArticlePublishDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long articleId;

    @NotBlank(message = "文章标题不能为空")
    private String title;

    private String summary;

    private String coverUrl;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    private String contentType;

    @NotBlank(message = "文章类型不能为空")
    private String articleType;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private List<Long> tagIds;

    private String sourceType;

    private String originalUrl;

    private Integer wordCount;

    private Integer readingTime;

    private Integer allowComment;

    private Boolean isDraft;

    private Boolean isSubmit;
}
