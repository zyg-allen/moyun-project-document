package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目收藏 VO（个人中心显示）
 *
 * @author moyun
 */
@Data
public class InterviewBookmarkVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long questionId;

    private Long userId;

    private String note;

    /** 关联题目信息 */
    private InterviewQuestionVO question;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
