package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 错题本 VO（含题目简要信息）
 *
 * @author moyun
 */
@Data
public class WrongQuestionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private Long questionId;

    /** 最近一次答题ID */
    private Long attemptId;

    /** 状态 wrong/reviewing/mastered */
    private String status;

    /** 答错次数 */
    private Integer wrongCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastWrongTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextReviewTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // ==================== 题目信息（JOIN 查询） ====================

    /** 题目标题 */
    private String questionTitle;

    /** 题目难度 easy/medium/hard */
    private String questionDifficulty;

    /** 题目标签（逗号分隔） */
    private String questionTags;

    /** 题目分类ID */
    private Long questionCategoryId;
}
