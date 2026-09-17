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

    /** v11.91 V3：计划大问题数量（默认走服务端 QUESTION_COUNT） */
    private Integer questionCount;

    /** 简历ID（可选，有简历时面试官上下文携带简历摘要） */
    private Long resumeId;

    /** 难度 easy/medium/hard */
    private String difficulty;

    /** v11.90 V2：岗位要求 JD（可选，面试官提问方向与深度贴合岗位要求） */
    @Size(max = 2000, message = "岗位要求不能超过2000个字符")
    private String jobRequirements;
}
