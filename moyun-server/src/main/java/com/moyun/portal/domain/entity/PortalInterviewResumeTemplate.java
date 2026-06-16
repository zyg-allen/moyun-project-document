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

    /** 是否付费 */
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

    /** 状态：active/inactive */
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
