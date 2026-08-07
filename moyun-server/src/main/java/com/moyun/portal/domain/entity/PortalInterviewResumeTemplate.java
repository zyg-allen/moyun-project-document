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
 * 简历模板
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_resume_template")
public class PortalInterviewResumeTemplate extends BaseEntity
{
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板标题 */
    @NotBlank(message = "模板标题不能为空")
    @Size(min = 0, max = 500, message = "模板标题长度不能超过500个字符")
    private String title;

    /** 模板描述 */
    private String description;

    /** 封面URL */
    private String cover;

    /** 下载地址 */
    private String downloadUrl;

    /** 分类 */
    private String category;

    /** 文件类型：docx/pdf/psd */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /**
     * 是否付费
     * 注意：历史字段名为 isPremium（VIP语义），前端使用 isPaid（通用付费语义），
     *       实体保留 isPremium 列名，MyBatis-Plus 自动映射，前端传参 isPaid 已由 Service 层适配。
     */
    private Boolean isPremium;

    /** 使用指南 */
    private String usageGuide;

    /** 点赞数 */
    private Long likeCount;

    /** 下载次数 */
    private Long downloadCount;

    /** 排序 */
    private Integer sort;

    /** 标签（逗号分隔，可选） */
    private String tags;

    /**
     * 状态:draft 草稿/published 已发布
     * 注意：历史注释为 active/inactive（启停语义），实际前端使用 draft/published（内容生命周期语义），
     *       已修正注释与前端保持一致；存量数据兼容。
     */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public PortalInterviewResumeTemplate()
    {
    }

    public PortalInterviewResumeTemplate(Long id)
    {
        this.id = id;
    }
}
