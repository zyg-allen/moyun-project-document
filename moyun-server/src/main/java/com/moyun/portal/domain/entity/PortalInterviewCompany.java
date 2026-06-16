package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 面试公司标签
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_company")
public class PortalInterviewCompany extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司名称 */
    @NotBlank(message = "公司名称不能为空")
    @Size(min = 0, max = 200, message = "公司名称长度不能超过200个字符")
    private String name;

    /** URL标识 */
    @Size(min = 0, max = 200, message = "URL标识长度不能超过200个字符")
    private String slug;

    /** 公司Logo */
    @Size(min = 0, max = 500, message = "Logo长度不能超过500个字符")
    private String logo;

    /** 公司简介 */
    private String description;

    /** 行业 */
    @Size(min = 0, max = 200, message = "行业长度不能超过200个字符")
    private String industry;

    /** 关联题目数量 */
    private Integer questionCount;

    /** 排序 */
    private Integer sort;

    /** 状态：active/inactive */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
