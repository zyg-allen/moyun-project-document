package com.moyun.agent.stats;

import lombok.Data;

/**
 * 图片过滤统计信息
 * 
 * <p>记录三层过滤的详细统计数据，用于分析过滤效果和成本节省</p>
 * 
 * @author laomao
 */
@Data
public class ImageFilterStats {
    
    /**
     * 总提取图片数
     */
    private int totalExtracted = 0;
    
    // ========== 第一层：规则过滤统计 ==========
    
    /**
     * 极小尺寸过滤数
     */
    private int filteredByExtremeSize = 0;
    
    /**
     * 极端长宽比过滤数
     */
    private int filteredByAspectRatio = 0;
    
    /**
     * 纯色图片过滤数
     */
    private int filteredByPureColor = 0;
    
    /**
     * 页眉页脚Logo过滤数
     */
    private int filteredByHeaderFooter = 0;
    
    /**
     * 边距装饰过滤数
     */
    private int filteredByMargin = 0;
    
    /**
     * 超高频重复过滤数
     */
    private int filteredByHighRepeat = 0;
    
    /**
     * 简单内容过滤数
     */
    private int filteredBySimpleContent = 0;
    
    /**
     * 大面积单色过滤数
     */
    private int filteredByLargeMonochrome = 0;
    
    /**
     * 综合评分低过滤数
     */
    private int filteredByLowScore = 0;
    
    /**
     * 通过规则过滤的图片数
     */
    private int passedRuleFilter = 0;
    
    // ========== 第二层：AI分析统计 ==========
    
    /**
     * AI分析的图片数
     */
    private int aiAnalyzed = 0;
    
    /**
     * AI判断为低价值的图片数
     */
    private int aiRejectedLowValue = 0;
    
    /**
     * AI分析质量不达标的图片数
     */
    private int aiRejectedInvalidAnalysis = 0;
    
    /**
     * 描述过短的图片数
     */
    private int aiRejectedShortDescription = 0;
    
    // ========== 第三层：最终存储统计 ==========
    
    /**
     * 最终存储的图片数
     */
    private int finalStored = 0;
    
    /**
     * 获取规则过滤总数
     * 
     * @return 规则过滤的图片总数
     */
    public int getTotalFilteredByRules() {
        return filteredByExtremeSize + filteredByAspectRatio + filteredByPureColor +
               filteredByHeaderFooter + filteredByMargin + filteredByHighRepeat +
               filteredBySimpleContent + filteredByLargeMonochrome + filteredByLowScore;
    }
    
    /**
     * 获取AI过滤总数
     * 
     * @return AI过滤的图片总数
     */
    public int getTotalFilteredByAI() {
        return aiRejectedLowValue + aiRejectedInvalidAnalysis + aiRejectedShortDescription;
    }
    
    /**
     * 获取规则过滤率
     * 
     * @return 规则过滤率（百分比）
     */
    public double getRuleFilterRate() {
        return totalExtracted > 0 ? (getTotalFilteredByRules() * 100.0 / totalExtracted) : 0;
    }
    
    /**
     * 获取AI过滤率
     * 
     * @return AI过滤率（百分比）
     */
    public double getAIFilterRate() {
        return aiAnalyzed > 0 ? (getTotalFilteredByAI() * 100.0 / aiAnalyzed) : 0;
    }
    
    /**
     * 获取最终存储率
     * 
     * @return 最终存储率（百分比）
     */
    public double getFinalStoreRate() {
        return totalExtracted > 0 ? (finalStored * 100.0 / totalExtracted) : 0;
    }
    
    /**
     * 重置所有统计数据
     */
    public void reset() {
        totalExtracted = 0;
        filteredByExtremeSize = 0;
        filteredByAspectRatio = 0;
        filteredByPureColor = 0;
        filteredByHeaderFooter = 0;
        filteredByMargin = 0;
        filteredByHighRepeat = 0;
        filteredBySimpleContent = 0;
        filteredByLargeMonochrome = 0;
        filteredByLowScore = 0;
        passedRuleFilter = 0;
        aiAnalyzed = 0;
        aiRejectedLowValue = 0;
        aiRejectedInvalidAnalysis = 0;
        aiRejectedShortDescription = 0;
        finalStored = 0;
    }
    
    /**
     * 生成统计摘要报告
     * 
     * @return 格式化的统计报告文本
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== 图片过滤统计 ==========\n");
        sb.append(String.format("总提取: %d 张\n", totalExtracted));
        
        // 第一层统计
        sb.append("\n【第一层：规则过滤】\n");
        sb.append(String.format("  - 极小尺寸: %d\n", filteredByExtremeSize));
        sb.append(String.format("  - 极端长宽比: %d\n", filteredByAspectRatio));
        sb.append(String.format("  - 纯色图片: %d\n", filteredByPureColor));
        sb.append(String.format("  - 页眉页脚Logo: %d\n", filteredByHeaderFooter));
        sb.append(String.format("  - 边距装饰: %d\n", filteredByMargin));
        sb.append(String.format("  - 超高频重复: %d\n", filteredByHighRepeat));
        sb.append(String.format("  - 简单内容: %d\n", filteredBySimpleContent));
        sb.append(String.format("  - 大面积单色: %d\n", filteredByLargeMonochrome));
        sb.append(String.format("  - 综合评分低: %d\n", filteredByLowScore));
        sb.append(String.format("  小计过滤: %d (%.1f%%)\n", getTotalFilteredByRules(), getRuleFilterRate()));
        sb.append(String.format("  通过规则: %d\n", passedRuleFilter));
        
        // 第二层统计
        sb.append("\n【第二层：AI分析】\n");
        sb.append(String.format("  - AI分析数: %d\n", aiAnalyzed));
        sb.append(String.format("  - 判断低价值: %d\n", aiRejectedLowValue));
        sb.append(String.format("  - 分析质量差: %d\n", aiRejectedInvalidAnalysis));
        sb.append(String.format("  - 描述过短: %d\n", aiRejectedShortDescription));
        sb.append(String.format("  小计过滤: %d (%.1f%%)\n", getTotalFilteredByAI(), getAIFilterRate()));
        
        // 第三层统计
        sb.append("\n【第三层：最终存储】\n");
        sb.append(String.format("  - 存储数量: %d (%.1f%%)\n", finalStored, getFinalStoreRate()));
        
        // 成本节省统计
        sb.append("\n【成本节省】\n");
        sb.append(String.format("  - 多模态调用: %d 次 (节省 %d 次)\n", 
            aiAnalyzed, totalExtracted - aiAnalyzed));
        sb.append(String.format("  - 向量化调用: %d 次 (节省 %d 次)\n", 
            finalStored, totalExtracted - finalStored));
        sb.append("==================================\n");
        
        return sb.toString();
    }
}
