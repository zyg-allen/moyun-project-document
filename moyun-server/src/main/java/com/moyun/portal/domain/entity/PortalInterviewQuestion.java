package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 面试题目对象 portal_interview_question
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_question")
public class PortalInterviewQuestion extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题目标题
     */
    @NotBlank(message = "题目标题不能为空")
    @Size(min = 0, max = 500, message = "题目标题长度不能超过500个字符")
    private String title;

    /**
     * 题目描述
     */
    private String description;

    /**
     * 难度:easy,medium,hard
     */
    @Size(min = 0, max = 20, message = "难度长度不能超过20个字符")
    private String difficulty;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 标签，逗号分隔
     */
    @Size(min = 0, max = 500, message = "标签长度不能超过500个字符")
    private String tags;

    /**
     * 公司，逗号分隔
     */
    @Size(min = 0, max = 500, message = "公司长度不能超过500个字符")
    private String companies;

    /**
     * 通过率
     */
    private BigDecimal acceptanceRate;

    /**
     * 提交次数
     */
    private Long submissionCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 提示
     */
    private String hint;

    /**
     * 参考答案
     */
    private String solution;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态:draft 草稿/published 已发布/archived 已归档
     * 注意：历史注释为 active/inactive（启停语义），实际前端使用 draft/published/archived（内容生命周期语义），
     *       已修正注释与前端保持一致；存量数据兼容。
     */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    public PortalInterviewQuestion() {
    }

    public PortalInterviewQuestion(Long id) {
        this.id = id;
    }
}
