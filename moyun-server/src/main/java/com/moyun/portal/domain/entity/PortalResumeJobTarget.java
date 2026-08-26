package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历优化-岗位目标（v10.13 简历优化重构）
 * <p>用户管理的目标岗位与 JD，岗位匹配评分的核心输入。</p>
 *
 * @author moyun
 */
@Data
@TableName("portal_resume_job_target")
public class PortalResumeJobTarget implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 目标岗位名称（如 Java开发工程师） */
    private String position;

    /** 目标公司（选填） */
    private String company;

    /** 期望城市（选填） */
    private String city;

    /** 岗位类型（全职/兼职/实习） */
    private String jobType;

    /** 岗位描述/JD 原文（匹配分析核心输入） */
    private String jdText;

    /** AI 提取的 JD 核心关键词（逗号分隔） */
    private String jdKeywords;

    /** 是否默认岗位：0=否 1=是 */
    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
