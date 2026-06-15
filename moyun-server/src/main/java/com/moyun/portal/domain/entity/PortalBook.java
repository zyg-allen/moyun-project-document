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

    /** 阅读人数 */
    private Long readingCount;

    /** 状态:active,inactive */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    /** 访问级别: free=免费公开, vip=会员专享, preview=试读（前30%免费） */
    @Size(min = 0, max = 20, message = "访问级别长度不能超过20个字符")
    private String accessLevel;

    /** 免费试读比例（0-100，当 access_level=preview 时生效） */
    private Integer previewRatio;

    /** 书籍单价（元，预留：未来付费单本购买） */
    private java.math.BigDecimal price;

    /** 是否精选 */
    private Boolean isFeatured;

    /** 是否推荐（首页展示用） */
    private Boolean isRecommended;

    /** 简介（纯文本） */
    private String summary;

    /** 作者简介 */
    private String authorBio;

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

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public Integer getPreviewRatio() {
        return previewRatio;
    }

    public void setPreviewRatio(Integer previewRatio) {
        this.previewRatio = previewRatio;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Boolean getIsRecommended() {
        return isRecommended;
    }

    public void setIsRecommended(Boolean isRecommended) {
        this.isRecommended = isRecommended;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getAuthorBio() {
        return authorBio;
    }

    public void setAuthorBio(String authorBio) {
        this.authorBio = authorBio;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("title", getTitle())
                .append("author", getAuthor())
                .append("cover", getCover())
                .append("description", getDescription())
                .append("summary", getSummary())
                .append("isbn", getIsbn())
                .append("publisher", getPublisher())
                .append("publishDate", getPublishDate())
                .append("pageCount", getPageCount())
                .append("categoryId", getCategoryId())
                .append("tags", getTags())
                .append("rating", getRating())
                .append("readingCount", getReadingCount())
                .append("status", getStatus())
                .append("accessLevel", getAccessLevel())
                .append("previewRatio", getPreviewRatio())
                .append("price", getPrice())
                .append("isFeatured", getIsFeatured())
                .append("isRecommended", getIsRecommended())
                .append("authorBio", getAuthorBio())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
