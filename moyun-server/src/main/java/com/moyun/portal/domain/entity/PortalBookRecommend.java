package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

import java.time.LocalDateTime;

/**
 * 书籍推荐（运营位）实体 portal_book_recommend
 *
 * @author moyun
 */
@Data
@TableName("portal_book_recommend")
public class PortalBookRecommend extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 书籍ID */
    private Long bookId;

    /** 推荐位置：home_banner/home_hot/category_top/limit_free/discover_banner */
    private String position;

    /** 排序（越小越靠前，默认 0） */
    private Integer sort;

    /** 生效开始时间（为空表示立即生效） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 生效结束时间（为空表示长期有效） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 是否启用（默认 true） */
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 书名（关联 portal_book 查询，非表字段） */
    @TableField(exist = false)
    private String bookTitle;

    public PortalBookRecommend() {
    }

    public PortalBookRecommend(Long id) {
        this.id = id;
    }
}
