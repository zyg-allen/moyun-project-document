package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专栏
 *
 * @author moyun
 */
@Data
@TableName("portal_column")
public class PortalColumn extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创作者 */
    private Long userId;

    /** 专栏名 */
    private String title;

    /** 副标题 */
    private String subtitle;

    /** 专栏简介 */
    private String description;

    /** 封面 */
    private String cover;

    /** 分类 */
    private Long categoryId;

    /** 状态 draft/published/archived */
    private String status;

    /** 文章数 */
    private Integer articleCount;

    /** 订阅数 */
    private Integer subscribeCount;

    /** 浏览数 */
    private Integer viewCount;

    /** 是否完结 0/1 */
    private Integer isFinished;

    /** 专栏会员价，0=免费 */
    private BigDecimal price;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    // BaseEntity 公共字段对应列在 portal_column 表中不存在，排除 MyBatis-Plus 映射，避免 SELECT/INSERT 报未知列
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private LocalDateTime createTime;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String remark;
}
