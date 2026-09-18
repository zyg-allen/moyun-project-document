package com.moyun.ext.aiapp.model.data;

import lombok.Data;
import java.util.Map;

/**
 * 通用场景数据载体
 *
 * <p>非 Resume/Interview 族场景的统一数据载体：
 * {@code content} 承载纯文本结果（prompt_gen/data_analysis/sql_gen），
 * {@code structured} 承载 JSON 解析后的 Map（article_meta/content_tags/writing_prompt/workflow_gen）。
 * AiSceneJsonClient.unwrapStructured 已支持本类型。</p>
 *
 * @author laomao
 * @since 2026-09-18
 */
@Data
public class GenericSceneData {
    /** 通用文本结果（非结构化场景） */
    private String content;
    /** 结构化结果（JSON 解析后的 Map） */
    private Map<String, Object> structured;
    /** 来源标记 */
    private String source = "ai";
}
