package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习计划
 *
 * @author moyun
 */
@Data
@TableName("portal_study_plan")
public class PortalStudyPlan extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 计划标题 */
    private String title;

    /** 计划类型 daily_question/weekly_reading/custom */
    private String planType;

    /** 目标数量 */
    /**
     * 目标数量。
     *
     * <p><b>为什么指定 updateStrategy=ALWAYS</b>：MyBatis-Plus 默认 {@code NOT_NULL} ——
     * 实体字段为 null 时**不会**出现在 UPDATE 语句里。而门户编辑计划允许"清空"这些可选字段，
     * 清空后前端发的就是 null/缺省 ⇒ 默认策略下**永远清不掉**（清单 P2 的"清空不生效"）。
     * 本表这 4 个字段在 DDL 中均可空，故显式改为 ALWAYS：null 也写入（即置 NULL）。</p>
     */
    @com.baomidou.mybatisplus.annotation.TableField(
            updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private Integer targetCount;

    /** 目标分类 */
    /** 目标分类（可空；清空语义同 targetCount：需 ALWAYS 才能写 NULL） */
    @com.baomidou.mybatisplus.annotation.TableField(
            updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private String targetCategory;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 开始日期（可空；清空语义同 targetCount：需 ALWAYS 才能写 NULL） */
    @com.baomidou.mybatisplus.annotation.TableField(
            updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private LocalDate startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    /** 结束日期（可空；清空语义同 targetCount：需 ALWAYS 才能写 NULL） */
    @com.baomidou.mybatisplus.annotation.TableField(
            updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private LocalDate endDate;

    /** 状态 active/completed/abandoned */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // BaseEntity 公共字段对应列在 portal_study_plan 表中不存在，排除 MyBatis-Plus 映射，避免 SELECT/INSERT 报未知列
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
