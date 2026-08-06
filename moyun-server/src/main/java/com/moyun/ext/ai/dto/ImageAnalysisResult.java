package com.moyun.ext.ai.dto;

import lombok.Data;

/**
 * 图片分析结果DTO
 * 
 * <p>封装多模态AI对图片的分析结果，包括价值判断和详细内容提取</p>
 * 
 * @author laomao
 */
@Data
public class ImageAnalysisResult {
    
    /**
     * 图片类型
     * 如：架构图、流程图、界面截图、数据图表、Logo、装饰图等
     */
    private String imageType;
    
    /**
     * 信息价值评估
     * 取值：高/低
     */
    private String informationValue;
    
    /**
     * 存储建议
     * 取值：存储/丢弃
     */
    private String storageAdvice;
    
    /**
     * 判断理由
     * AI给出的判断依据说明
     */
    private String reason;
    
    /**
     * 图片主题
     * 一句话概括图片核心内容（15字内）
     */
    private String subject;
    
    /**
     * 关键词
     * 从图片中提取的所有可见文字，逗号分隔
     */
    private String keywords;
    
    /**
     * 关系结构
     * 描述图片中的核心流程或结构关系
     */
    private String relationship;
    
    /**
     * 补充说明
     * 其他重要特征的补充描述
     */
    private String supplement;
    
    /**
     * 组件列表
     * 图片中的主要组件或模块名称
     */
    private String components;
    
    /**
     * 完整描述
     * AI返回的原始完整响应文本
     */
    private String fullDescription;
    
    /**
     * 判断是否应该存储该图片
     * 
     * @return true-应该存储，false-应该丢弃
     */
    public boolean shouldStore() {
        return "存储".equals(storageAdvice);
    }
    
    /**
     * 判断是否为高价值图片
     * 
     * @return true-高价值，false-低价值
     */
    public boolean isHighValue() {
        return "高".equals(informationValue);
    }
    
    /**
     * 获取关键词数量
     * 
     * @return 关键词个数
     */
    public int getKeywordCount() {
        if (keywords == null || keywords.isEmpty()) {
            return 0;
        }
        return keywords.split("[,，]").length;
    }
    
    /**
     * 构建详细描述文本
     * 
     * <p>将各个字段组合成结构化的描述文本，用于向量化存储</p>
     * 
     * @return 格式化的详细描述，如果必要字段缺失则返回null
     */
    public String getDetailedDescription() {
        // 如果主题和关键词都为空，使用完整描述
        if ((subject == null || subject.isEmpty()) && (keywords == null || keywords.isEmpty())) {
            return fullDescription;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("【类型】").append(imageType != null ? imageType : "未知").append("\n");
        
        if (subject != null && !subject.isEmpty()) {
            sb.append("【主题】").append(subject).append("\n");
        }
        
        if (keywords != null && !keywords.isEmpty()) {
            sb.append("【关键词】").append(keywords).append("\n");
        }
        
        if (relationship != null && !relationship.isEmpty()) {
            sb.append("【关系】").append(relationship).append("\n");
        }
        
        if (components != null && !components.isEmpty()) {
            sb.append("【组件】").append(components).append("\n");
        }
        
        if (supplement != null && !supplement.isEmpty()) {
            sb.append("【补充】").append(supplement);
        }
        
        String result = sb.toString();
        // 如果构建的描述太短，返回完整描述
        return result.length() > 20 ? result : fullDescription;
    }
}
