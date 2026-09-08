package com.moyun.ext.ai2.model.data;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 场景2：简历场景数据（scene = resume_parse / resume_optimize）
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
public class ResumeSceneData {

    /** 优化后的简历文本（resume_optimize） */
    private String optimizedText;

    /** 原始简历文本 */
    private String originalText;

    /** 优化建议列表 */
    private List<String> suggestions;

    /** 匹配/质量评分 */
    private Integer score;

    /** 关键词 */
    private List<String> keywords;

    /** 结构化简历（resume_parse）：基本信息/教育/工作/项目/技能 */
    private Map<String, Object> structured;
}
