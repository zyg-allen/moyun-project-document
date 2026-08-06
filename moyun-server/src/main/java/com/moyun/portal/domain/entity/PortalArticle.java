package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

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

    /**
     * 文章URL别名，用于SEO语义化路径
     * 例如：spring-boot-best-practice
     */
    @Size(min = 0, max = 500, message = "文章别名长度不能超过500个字符")
    private String slug;

    private String content;

    @Size(min = 0, max = 1000, message = "文章摘要长度不能超过1000个字符")
    private String excerpt;

    @Size(min = 0, max = 10485760, message = "封面长度不能超过10MB")
    private String cover;

    private Long authorId;

    private Long categoryId;

    /**
     * 顶级分类ID
     */
    private Long rootCategoryId;

    /**
     * 分类路径，用逗号分隔，包含所有祖先分类ID，例如：1,3,5
     */
    @Size(min = 0, max = 500, message = "分类路径长度不能超过500个字符")
    private String categoryPath;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String categorySlug;

    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    /** 审核人ID（系统用户ID，审核时写入） */
    private Long auditorId;

    /** 审核意见/驳回原因（独立于通用 remark，专用于审核记录） */
    private String auditRemark;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    private Boolean isFeatured;

    private Boolean isTop;

    private Boolean isCarousel;

    private Boolean isCategoryRecommended;

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

    /**
     * 编辑会话标识（一次编辑会话唯一）。
     * 前端进入发布页生成，保存草稿/发布都带上同一 token；
     * 后端用 token 做幂等：同 token 已存在记录则更新，否则新建，
     * 保证一次编辑会话只产生一条文章记录。
     */
    @Size(min = 0, max = 64, message = "会话标识长度不能超过64个字符")
    private String sessionToken;

    private String contentMarkdown;

    /** 是否付费阅读 0=免费 1=付费 */
    private Integer isPaid = 0;

    /** 付费内容（购买后可见） */
    private String paidContent;

    /** 试读字数（未购买可预览的字数） */
    private Integer previewLength;

    /** 付费价格，0=免费 */
    private BigDecimal price;

    /** 作者昵称 */
    @TableField(exist = false)
    private String authorNickname;

    /** 作者用户名 */
    @TableField(exist = false)
    private String authorUsername;

    /** 作者头像 */
    @TableField(exist = false)
    private String authorAvatar;

    /** 标签ID列表（非持久化，用于接收前端传参，绑定/解绑时同步维护 reference_count） */
    @TableField(exist = false)
    private java.util.List<Long> tagIds;

    /** 标签名称列表（非持久化，用于自动创建新标签） */
    @TableField(exist = false)
    private java.util.List<String> tagNames;

}
