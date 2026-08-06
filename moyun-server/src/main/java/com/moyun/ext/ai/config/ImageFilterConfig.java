package com.moyun.ext.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片过滤配置类
 * 
 * <p>支持通过配置文件调整过滤规则的各项阈值</p>
 * <p>配置前缀：image.filter</p>
 * 
 * @author laomao
 */
@Data
@Component
@ConfigurationProperties(prefix = "image.filter")
public class ImageFilterConfig {
    
    /**
     * 最小尺寸阈值（像素）
     * 宽高都小于此值的图片将被过滤
     */
    private int minSize = 50;
    
    /**
     * 最大长宽比阈值
     * 长宽比超过此值的图片将被过滤（通常是分隔线）
     */
    private double maxAspectRatio = 15.0;
    
    /**
     * 纯色图片最大颜色数
     * 颜色种类小于等于此值的图片将被过滤
     */
    private int maxPureColors = 3;
    
    /**
     * 页眉页脚重复次数阈值
     * 重复次数达到此值的小图片将被判定为Logo
     */
    private int headerFooterRepeatThreshold = 5;
    
    /**
     * 页眉页脚Logo最大宽度（像素）
     */
    private int headerFooterMaxWidth = 250;
    
    /**
     * 页眉页脚Logo最大高度（像素）
     */
    private int headerFooterMaxHeight = 150;
    
    /**
     * 边距装饰最大尺寸（像素）
     */
    private int marginMaxSize = 100;
    
    /**
     * 超高频重复阈值
     * 重复次数达到此值将被判定为Logo/水印
     */
    private int highRepeatThreshold = 10;
    
    /**
     * 简单内容最大颜色数
     */
    private int simpleContentMaxColors = 10;
    
    /**
     * 简单内容最大边缘密度
     */
    private double simpleContentMaxEdgeDensity = 0.05;
    
    /**
     * 大面积单色最小面积（像素）
     */
    private int largeMonochromeMinArea = 50000;
    
    /**
     * 大面积单色最大颜色数
     */
    private int largeMonochromeMaxColors = 5;
    
    /**
     * 综合评分阈值
     * 评分低于此值的图片将被过滤
     */
    private double comprehensiveScoreThreshold = 0.25;
    
    /**
     * 最小关键词数量
     * AI分析结果的关键词数量必须达到此值
     * 降低到2个，因为有些架构图文字较少
     */
    private int minKeywordCount = 2;
    
    /**
     * 最小描述长度（字符）
     * AI生成的描述长度必须达到此值
     */
    private int minDescriptionLength = 30;
    
    /**
     * 位置权重系数
     * 用于综合评分计算
     */
    private double positionWeight = 0.4;
    
    /**
     * 尺寸权重系数
     * 用于综合评分计算
     */
    private double sizeWeight = 0.3;
    
    /**
     * 复杂度权重系数
     * 用于综合评分计算
     */
    private double complexityWeight = 0.3;
}
