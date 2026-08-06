package com.moyun.ext.ai.dto;

import com.moyun.ext.ai.vo.KnowledgeBaseVO;
import lombok.Data;
import java.util.List;

/**
 * 知识库统计响应
 *
 * @author laomao
 */
@Data
public class KnowledgeStatsResponse {
    
    /**
     * 知识库总数
     */
    private Integer total;
    
    /**
     * 已完成数量
     */
    private Integer completed;
    
    /**
     * 总使用次数
     */
    private Integer totalUsage;
    
    /**
     * 平均命中率（百分比）
     */
    private Double avgHitRate;
    
    /**
     * 热门知识库 Top 10
     */
    private List<KnowledgeBaseVO> topKnowledge;
}
