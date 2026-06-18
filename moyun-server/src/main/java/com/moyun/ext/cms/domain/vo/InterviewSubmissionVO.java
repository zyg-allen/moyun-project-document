package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目提交记录 VO
 *
 * @author moyun
 */
@Data
public class InterviewSubmissionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long questionId;

    private Long userId;

    /** 提交的代码 */
    private String code;

    /** 提交的文字答案 */
    private String content;

    /** 编程语言 */
    private String language;

    /** 答案类型：code/text/design */
    private String answerType;

    /** 状态：accepted/wrong_answer/time_limit/compile_error/pending */
    private String status;

    /** 是否通过 */
    private Boolean isSuccess;

    /** 运行时间（毫秒） */
    private Integer runtime;

    /** 内存使用（KB） */
    private Integer memoryUsage;

    /** 用户备注 */
    private String note;

    /** 是否被精选 */
    private Boolean isFeatured;

    /** 精选时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime featuredTime;

    /** 提交者昵称（精选笔记展示用） */
    private String userNickname;

    /** 提交者头像（精选笔记展示用） */
    private String userAvatar;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
