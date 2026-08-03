package com.moyun.ext.cms.domain.query;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 文章查询对象
 *
 * @author moyun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CmsArticleQuery extends PageDomain implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 根分类ID
     */
    private Long rootCategoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 状态
     */
    private String status;

    /**
     * 是否置顶
     */
    private Boolean isTop;

    /**
     * 是否推荐
     */
    private Boolean isFeatured;

    /**
     * 是否轮播
     */
    private Boolean isCarousel;

    /**
     * 是否分类推荐
     */
    private Boolean isCategoryRecommended;

    // ==================== 审核台过滤字段 ====================

    /**
     * 审核人ID（sys_user.user_id）
     * 用于审核台筛选"我审核过的文章"
     */
    private Long auditorId;

    /**
     * 审核时间范围-开始（含），用于审核台按审核时间段筛选
     */
    private LocalDateTime auditTimeStart;

    /**
     * 审核时间范围-结束（含），用于审核台按审核时间段筛选
     */
    private LocalDateTime auditTimeEnd;
}
