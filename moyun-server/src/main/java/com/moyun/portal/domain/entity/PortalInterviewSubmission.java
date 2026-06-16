package com.moyun.portal.domain.entity;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 题目提交记录
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_submission")
public class PortalInterviewSubmission extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目ID */
    private Long questionId;

    /** 用户ID */
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

    /** 备注 */
    private String note;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // === BaseEntity 中不需要持久化的字段 ===
    @TableField(exist = false)
    private String createBy;

    @TableField(exist = false)
    private String updateBy;

    @TableField(exist = false)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String remark;
}
