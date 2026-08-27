package com.moyun.ext.cms.domain.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 语音面试开始请求配置（V10.1）
 *
 * @author moyun
 */
@Data
public class VoiceStartConfig {

    /** 面试岗位（如 后端开发） */
    @Size(max = 64, message = "面试岗位名称不能超过64个字符")
    private String position;

    /** 面试场景（如 算法/系统设计） */
    @Size(max = 64, message = "面试场景名称不能超过64个字符")
    private String scene;

    /** 简历ID（可选，有简历时启用项目深挖题源） */
    private Long resumeId;

    /** 面试官风格 professional/friendly/strict */
    private String style;

    /** 难度 easy/medium/hard */
    private String difficulty;

    /** 是否基于画像抽题 */
    private Boolean personalized;

    /** 是否启用提示功能 */
    private Boolean hintsEnabled;

    /** 卡壳阈值（秒，超过则自动提示） */
    private Integer stuckThreshold;
}
