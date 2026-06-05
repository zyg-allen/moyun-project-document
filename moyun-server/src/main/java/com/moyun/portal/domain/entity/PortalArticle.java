package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import com.moyun.core.base.page.PageDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

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

}
