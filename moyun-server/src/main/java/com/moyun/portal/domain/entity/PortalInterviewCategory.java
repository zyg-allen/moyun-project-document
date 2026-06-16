package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 面试题目分类对象 portal_interview_category
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_category")
public class PortalInterviewCategory extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 0, max = 200, message = "分类名称长度不能超过200个字符")
    private String name;

    /**
     * 分类标识
     */
    @Size(min = 0, max = 200, message = "分类标识长度不能超过200个字符")
    private String slug;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 图标URL
     */
    @Size(min = 0, max = 500, message = "图标URL长度不能超过500个字符")
    private String icon;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 状态:active,inactive
     */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    public PortalInterviewCategory() {
    }

    public PortalInterviewCategory(Long id) {
        this.id = id;
    }
}
