package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试题目详情 VO
 *
 * @author moyun
 */
@Data
public class InterviewQuestionDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String description;

    private String difficulty;

    private Long categoryId;

    private String categoryName;

    private List<String> tags;

    private List<com.moyun.portal.domain.vo.TagVO> tagList;

    private List<InterviewCompanyVO> companies;

    private BigDecimal acceptanceRate;

    private Long submissionCount;

    private Long likeCount;

    /** 提示 */
    private String hint;

    /** 参考答案 */
    private String solution;

    private Integer sort;

    private String status;

    /** 我是否已点赞 */
    private Boolean liked;

    /** 我是否已收藏 */
    private Boolean bookmarked;

    /** 我的做题状态 */
    private String attemptStatus;

    /** 我的最近提交（历史） */
    private List<InterviewSubmissionVO> mySubmissions;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
