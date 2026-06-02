package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 面试题目对象 portal_interview_question
 *
 * @author moyun
 */
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
     * 状态:active,inactive
     */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    public PortalInterviewQuestion() {
    }

    public PortalInterviewQuestion(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getCompanies() {
        return companies;
    }

    public void setCompanies(String companies) {
        this.companies = companies;
    }

    public BigDecimal getAcceptanceRate() {
        return acceptanceRate;
    }

    public void setAcceptanceRate(BigDecimal acceptanceRate) {
        this.acceptanceRate = acceptanceRate;
    }

    public Long getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(Long submissionCount) {
        this.submissionCount = submissionCount;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("title", getTitle())
                .append("description", getDescription())
                .append("difficulty", getDifficulty())
                .append("categoryId", getCategoryId())
                .append("tags", getTags())
                .append("companies", getCompanies())
                .append("acceptanceRate", getAcceptanceRate())
                .append("submissionCount", getSubmissionCount())
                .append("likeCount", getLikeCount())
                .append("hint", getHint())
                .append("solution", getSolution())
                .append("sort", getSort())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
