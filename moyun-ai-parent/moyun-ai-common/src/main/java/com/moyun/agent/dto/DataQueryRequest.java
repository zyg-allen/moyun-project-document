package com.moyun.agent.dto;

import lombok.Data;

/**
 * 数据查询请求DTO
 *
 * @author laomao
 */
@Data
public class DataQueryRequest {

    /**
     * 数据源ID
     */
    private Long datasourceId;

    /**
     * 自然语言查询
     */
    private String query;

    /**
     * 会话ID(用于上下文记忆)
     */
    private String sessionId;

    /**
     * 是否需要分析
     */
    private Boolean needAnalysis = true;

    /**
     * 是否需要图表推荐
     */
    private Boolean needChart = true;

    /**
     * 最大返回行数
     */
    private Integer maxRows = 100;
}
