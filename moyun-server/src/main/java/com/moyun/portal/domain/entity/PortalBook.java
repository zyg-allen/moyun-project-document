package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 书籍对象 portal_book
 *
 * @author moyun
 */
@Data
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

    /** 书籍类型：published=出版物，novel=网络小说，longform=长文 */
    @Size(min = 0, max = 20, message = "书籍类型长度不能超过20个字符")
    private String type;

    /** 连载状态：ongoing=连载中，completed=已完结，hiatus=暂停更新 */
    @Size(min = 0, max = 20, message = "连载状态长度不能超过20个字符")
    private String serialStatus;

    /** 总字数（章节字数之和） */
    private Long wordCount;

    /** 总章节数 */
    private Integer chapterCount;

    /** 最新章节ID（用于追更展示） */
    private Long latestChapterId;

    /** 最新章节标题 */
    @Size(min = 0, max = 500, message = "最新章节标题长度不能超过500个字符")
    private String latestChapterTitle;

    /** 最后更新时间（章节发布时同步） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdateTime;

    /** 是否完结：1=完结，0=连载中（冗余字段，便于查询） */
    private Boolean isFinished;

    /** 访问级别: free=免费公开, vip=会员专享, preview=试读（前30%免费） */
    @Size(min = 0, max = 20, message = "访问级别长度不能超过20个字符")
    private String accessLevel;

    /** 免费试读比例（0-100，当 access_level=preview 时生效） */
    private Integer previewRatio;

    /** 书籍单价（元，预留：未来付费单本购买） */
    private BigDecimal price;

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
}
