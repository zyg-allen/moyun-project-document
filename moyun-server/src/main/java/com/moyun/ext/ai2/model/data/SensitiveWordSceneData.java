package com.moyun.ext.ai2.model.data;

import lombok.Data;

import java.util.List;

/**
 * 场景5：敏感词检测数据（scene = sensitive_word）
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
public class SensitiveWordSceneData {

    /** 是否包含敏感词 */
    private Boolean hasSensitive;

    /** 命中的敏感词 */
    private List<String> words;

    /** 风险等级：high/medium/low */
    private String riskLevel;

    /** 处理建议 */
    private String suggestion;
}
