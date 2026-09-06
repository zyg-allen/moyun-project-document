package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 语音面试会话详情 VO（V10.1）
 *
 * @author moyun
 */
@Data
public class VoiceInterviewVO {

    private Long id;
    private Long userId;
    private String position;
    private String scene;
    private Long resumeId;
    /** 面试官智能体ID（NULL=未绑定） */
    private Long agentId;
    /** 面试官智能体名称（前端展示） */
    private String agentName;
    /** 出题模式 preset=预生成题单 / dynamic=智能体动态出题 */
    private String questionMode;
    private String status;
    private String style;
    private String difficulty;
    private Integer totalQa;
    private Integer currentIdx;
    private Integer score;
    private String summary;
    private String configJson;
    private Integer isPersonalized;
    private LocalDateTime createTime;

    /** 问答列表（含追问） */
    private List<VoiceInterviewQaVO> qaList;

    /** 当前题目（首问或当前进行中的题目） */
    private VoiceInterviewQaVO currentQa;

    /** 开场话术（TTS 播报） */
    private String greetText;

    /** 分级提示（requestHint 接口返回，含 title/keywords/structureHint/examinePoints/speakText） */
    private HintVO hint;
}
