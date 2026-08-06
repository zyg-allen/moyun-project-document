package com.moyun.ext.ai.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 数据源连接池状态
 */
@Data
@Builder
public class DataSourcePoolStatus {
    
    /**
     * 数据源ID
     */
    private Long datasourceId;
    
    /**
     * 数据源名称
     */
    private String datasourceName;
    
    /**
     * 活跃连接数
     */
    private Integer activeConnections;
    
    /**
     * 空闲连接数
     */
    private Integer idleConnections;
    
    /**
     * 总连接数
     */
    private Integer totalConnections;
    
    /**
     * 最大连接数
     */
    private Integer maxConnections;
    
    /**
     * 等待连接数
     */
    private Integer waitingThreads;
    
    /**
     * 连接池使用率
     */
    private Double usageRate;
    
    /**
     * 连接池状态: healthy, warning, critical
     */
    private String status;
    
    /**
     * 最近查询数（最近1分钟）
     */
    private Integer recentQueryCount;
    
    /**
     * 平均查询时间（毫秒）
     */
    private Long avgQueryTime;
}
