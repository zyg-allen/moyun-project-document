package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 书籍对象 portal_book
 *
 * @author moyun
 */
@TableName("portal_book")
public class PortalBook extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 书名
     */
    @NotBlank(message = "书名不能为空")
    @Size(min = 0, max = 500, message = "书名长度不能超过500个字符")
    private String title;

    /**
     * 作者
     */
    @NotBlank(message = "作者不能为空")
    @Size(min = 0, max = 200, message = "作者长度不能超过200个字符")
    private String author;

    /**
     * 封面URL
     */
    @Size(min = 0, max = 500, message = "封面URL长度不能超过500个字符")
    private String cover;

    /**
     * 简介
     */
    private String description;

    /**
     * ISBN
     */
    @Size(min = 0, max = 50, message = "ISBN长度不能超过50个字符")
    private String isbn;

    /**
     * 出版社
     */
    @Size(min = 0, max = 200, message = "出版社长度不能超过200个字符")
    private String publisher;

    /**
     * 出版日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate publishDate;

    /**
     * 页数
     */
    private Integer pageCount;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 标签，逗号分隔
     */
    @Size(min = 0, max = 500, message = "标签长度不能超过500个字符")
    private String tags;

    /**
     * 评分
     */
    private BigDecimal rating;

    /**
     * 阅读人数
     */
    private Long readingCount;

    /**
     * 状态:active,inactive
     */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    public PortalBook() {
    }

    public PortalBook(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public LocalDate getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(LocalDate publishDate) {
        this.publishDate = publishDate;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Long getReadingCount() {
        return readingCount;
    }

    public void setReadingCount(Long readingCount) {
        this.readingCount = readingCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("title", getTitle())
                .append("author", getAuthor())
                .append("cover", getCover())
                .append("description", getDescription())
                .append("isbn", getIsbn())
                .append("publisher", getPublisher())
                .append("publishDate", getPublishDate())
                .append("pageCount", getPageCount())
                .append("categoryId", getCategoryId())
                .append("tags", getTags())
                .append("rating", getRating())
                .append("readingCount", getReadingCount())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
