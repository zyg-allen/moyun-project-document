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
 * 每日写作 prompt
 *
 * @author moyun
 */
@Data
@TableName("portal_writing_prompt")
public class PortalWritingPrompt extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** prompt 日期（唯一） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate promptDate;

    /** prompt 标题 */
    private String title;

    /** prompt 描述 */
    private String description;

    /** 分类（如：生活/职场/情感/虚构/哲思） */
    private String category;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

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
