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
 * 题目点赞记录
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_question_like")
public class PortalInterviewQuestionLike extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目ID */
    private Long questionId;

    /** 用户ID */
    private Long userId;

    /** 点赞时间 */
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

    // 覆盖 BaseEntity 的 delFlag：本表无 del_flag 列（迁移脚本排除），保持物理删除（toggle/流水语义）
    @TableField(exist = false)
    private String delFlag;
}
