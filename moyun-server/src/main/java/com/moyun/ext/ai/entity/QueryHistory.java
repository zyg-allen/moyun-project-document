package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 查询历史实体
 *
 * @author laomao
 */
@Data
@TableName("query_history")
public class QueryHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据源ID
     */
    private Long datasourceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 自然语言查询
     */
    private String naturalQuery;

    /**
     * 生成的SQL语句
     */
    private String generatedSql;

    /**
     * 查询类型: select, aggregate, join, analysis
     */
    private String queryType;

    /**
     * 涉及的表(逗号分隔)
     */
    private String tablesInvolved;

    /**
     * 结果行数
     */
    private Integer resultCount;

    /**
     * 执行时间(毫秒)
     */
    private Integer executionTime;

    /**
     * 执行状态: success, failed, timeout
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 分析类型: basic, trend, correlation, ranking
     */
    private String analysisType;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 是否生成洞察
     */
    private Boolean hasInsight;

    /**
     * LLM消耗的Token数
     */
    private Integer tokenUsed;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
