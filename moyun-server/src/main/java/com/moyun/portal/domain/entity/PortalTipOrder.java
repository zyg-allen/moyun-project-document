package com.moyun.portal.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 打赏订单（复用为付费阅读购买记录，target_type=article_paid）
 *
 * @author moyun
 */
@Data
@TableName("portal_tip_order")
public class PortalTipOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 打赏者用户ID */
    private Long userId;

    /** 被打赏者用户ID */
    private Long authorId;

    /** 打赏对象类型 article/column/article_paid */
    private String targetType;

    /** 打赏对象ID */
    private Long targetId;

    /** 打赏金额 */
    private BigDecimal amount;

    /** 打赏留言 */
    private String message;

    /** 状态 pending/paid/refunded */
    private String status;

    /** 支付方式 */
    private String payMethod;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // BaseEntity 公共字段对应列在 portal_tip_order 表中不存在，排除 MyBatis-Plus 映射，避免 SELECT/INSERT 报未知列
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

    /** 打赏者昵称（JOIN 查询时填充） */
    @TableField(exist = false)
    private String userNickname;

    /** 打赏者头像（JOIN 查询时填充） */
    @TableField(exist = false)
    private String userAvatar;

    /** 被打赏者昵称（JOIN 查询时填充） */
    @TableField(exist = false)
    private String authorNickname;

    /** 被打赏者头像（JOIN 查询时填充） */
    @TableField(exist = false)
    private String authorAvatar;

    /** 目标标题（JOIN 查询时填充，付费阅读订单为文章标题） */
    @TableField(exist = false)
    private String targetTitle;
}
