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
     * 题库类型：interview=面试 / certification=职业资格 / civil-service=公务员 /
     * postgraduate=考研 / other=其他
     * <p>用于把题库按「职业 / 行业考试类型」归类，前台按此筛选。
     */
    @Size(min = 0, max = 32, message = "题库类型长度不能超过32个字符")
    private String bankType;

    /**
     * 上级分类ID（0=顶级），支持两级分类
     */
    private Long parentId;

    /**
     * 职业族群（后端/前端/测试/产品/运维…），便于跨考试类型聚合
     */
    @Size(min = 0, max = 64, message = "职业族群长度不能超过64个字符")
    private String jobFamily;

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

    /**
     * 前台展示：0=展示 1=隐藏
     */
    @Size(min = 0, max = 1, message = "前台展示标记长度不能超过1个字符")
    private String visible;

    public PortalInterviewCategory() {
    }

    public PortalInterviewCategory(Long id) {
        this.id = id;
    }
}
