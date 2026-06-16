package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 做题记录 VO
 *
 * @author moyun
 */
@Data
public class InterviewAttemptVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long questionId;

    private Long userId;

    private Integer attemptCount;

    /** 状态：not_attempted/attempted/solved */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAttemptAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstSolvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSolvedAt;

    /** 关联题目信息 */
    private InterviewQuestionVO question;
}
