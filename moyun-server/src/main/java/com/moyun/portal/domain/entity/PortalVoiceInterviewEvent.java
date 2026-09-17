package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 语音面试会话事件日志 portal_voice_interview_event（v11.88 V2重构）
 *
 * <p>记录面试全链路关键节点（start/answer/next/finish/close/error），
 * 支撑断点恢复与全链路追溯。事件只增不改。</p>
 *
 * @author moyun
 */
@Data
@TableName("portal_voice_interview_event")
public class PortalVoiceInterviewEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 面试会话ID（portal_voice_interview.id） */
    private Long interviewId;

    /** 事件类型：start/answer/next/finish/close/error */
    private String eventType;

    /** 事件数据（题目索引/动作/原因等） */
    private String eventData;

    /** 事件时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
