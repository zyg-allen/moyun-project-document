package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试题目 VO（列表页显示用）——字段与前端 TS 类型对齐
 *
 * @author moyun
 */
@Data
public class InterviewQuestionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String description;

    private String difficulty; // easy/medium/hard

    private Long categoryId;

    private String categoryName;

    /** 标签列表（由 tags 字符串切分） */
    private List<String> tags;

    /** 通用标签（来自 portal_tag 关联） */
    private List<com.moyun.portal.domain.vo.TagVO> tagList;

    /** 关联公司 */
    private List<InterviewCompanyVO> companies;

    /** 通过率 */
    private BigDecimal acceptanceRate;

    /** 提交次数 */
    private Long submissionCount;

    /** 点赞数 */
    private Long likeCount;

    private Integer sort;

    private String status;

    /** 当前用户是否已点赞（登录态返回） */
    private Boolean liked;

    /** 当前用户是否已收藏（登录态返回） */
    private Boolean bookmarked;

    /** 当前用户做题状态（登录态返回） */
    private String attemptStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
