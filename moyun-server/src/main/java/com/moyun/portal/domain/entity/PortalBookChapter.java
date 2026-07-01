package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 书籍章节对象 portal_book_chapter
 *
 * @author moyun
 */
@Data
@TableName("portal_book_chapter")
public class PortalBookChapter extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属书籍ID */
    @NotNull(message = "书籍ID不能为空")
    private Long bookId;

    /** 章节标题 */
    @NotBlank(message = "章节标题不能为空")
    @Size(min = 0, max = 500, message = "章节标题长度不能超过500个字符")
    private String title;

    /** 章节正文（HTML格式，上限4GB） */
    private String content;

    /** Markdown原始内容（上限64KB，单章足够） */
    private String contentMarkdown;

    /** 编辑器模式：richtext/markdown */
    @Size(min = 0, max = 20, message = "编辑器模式长度不能超过20个字符")
    private String editorMode;

    /** 字数统计 */
    private Integer wordCount;

    /** 章节序号（用于排序，从1开始） */
    private Integer chapterNo;

    /** 所属分卷ID（可选，支持分卷管理） */
    private Long volumeId;

    /** 是否免费：1=免费，0=VIP章节 */
    private Boolean isFree;

    /** 章节单价（元，VIP章节购买） */
    private BigDecimal price;

    /** 是否已发布：0=草稿，1=已发布 */
    private Boolean isPublished;

    /** 发布时间（支持定时发布） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    /** 章节浏览量 */
    private Long viewCount;

    public PortalBookChapter() {
    }

    public PortalBookChapter(Long id) {
        this.id = id;
    }
}
