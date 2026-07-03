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
 * 职位
 *
 * @author moyun
 */
@Data
@TableName("portal_job")
public class PortalJob extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司ID（关联 portal_interview_company） */
    private Long companyId;

    /** 职位名称 */
    private String title;

    /** 工作城市 */
    private String city;

    /** 薪资下限 */
    private BigDecimal salaryMin;

    /** 薪资上限 */
    private BigDecimal salaryMax;

    /** 经验要求 */
    private String experience;

    /** 学历要求 */
    private String education;

    /** 职位描述 */
    private String description;

    /** 任职要求 */
    private String requirement;

    /** 状态：open/closed */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    // BaseEntity 公共字段对应列在表中不存在，排除 MyBatis-Plus 映射，避免 SELECT/INSERT 报未知列
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
