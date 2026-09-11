package com.moyun.ext.ai2.model.data;

import lombok.Data;

/**
 * 场景6：今日主题数据（scene = daily_topic）
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
public class TopicSceneData {

    /** 主题标题 */
    private String title;

    /** 主题描述 */
    private String description;

    /** 分类 */
    private String category;

    /** 来源：ai_generated/manual */
    private String source;
}
