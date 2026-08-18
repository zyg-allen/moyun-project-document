package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 语音面试会话主表 portal_voice_interview（V10.1）
 *
 * <p>基础字段（id/create_time/update_time/del_flag）独立声明，不再继承 BaseEntity。
 * 原因：前台用户生成的会话不需要 create_by / update_by / remark 审计字段，
 * 避免 MP 自动查询时带不存在的列引发 Unknown column 异常。
 *
 * @author moyun
 */
@Data
@TableName("portal_voice_interview")
public class PortalVoiceInterview implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 面试用户ID */
    private Long userId;

    /** 面试岗位 */
    private String position;

    /** 面试场景 */
    private String scene;

    /** 简历ID（有简历时启用项目深挖题源） */
    private Long resumeId;

    /** 状态 in_progress/finished */
    private String status;

    /** 面试官风格 professional/friendly/strict */
    private String style;

    /** 难度 easy/medium/hard */
    private String difficulty;

    /** 主问题目总数（不含追问） */
    private Integer totalQa;

    /** 当前主问题目序号（从0开始） */
    private Integer currentIdx;

    /** 面试总分（0-100，结束时计算） */
    private Integer score;

    /** AI 生成的面试总结 */
    private String summary;

    /** 报告 JSON（含维度分/亮点/薄弱点/逐题点评） */
    private String report;

    /** 配置 JSON（hintsEnabled/stuckThreshold/style/difficulty） */
    private String configJson;

    /** 是否基于画像抽题（0随机 1画像驱动） */
    private Integer isPersonalized;

    /** 抽题时的画像快照 JSON */
    private String profileSnapshot;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 删除标记（0=存在 2=删除） */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;
}
