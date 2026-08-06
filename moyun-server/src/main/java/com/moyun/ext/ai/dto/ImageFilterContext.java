package com.moyun.ext.ai.dto;

import lombok.Data;
import java.awt.image.BufferedImage;

/**
 * 图片过滤上下文DTO
 * 
 * <p>封装图片过滤所需的所有信息，包括图片本身、位置、特征等</p>
 * 
 * @author laomao
 */
@Data
public class ImageFilterContext {
    
    /**
     * 图片对象
     */
    private BufferedImage image;
    
    /**
     * 图片位置信息
     */
    private ImagePosition position;
    
    /**
     * 图片hash值（用于重复检测）
     */
    private String imageHash;
    
    /**
     * 重复出现次数
     */
    private int repeatCount;
    
    /**
     * 所在页码
     */
    private int pageNumber;
    
    /**
     * 页面内索引
     */
    private int imageIndex;
    
    /**
     * 图片宽度（像素）
     */
    private int width;
    
    /**
     * 图片高度（像素）
     */
    private int height;
    
    /**
     * 图片面积（像素）
     */
    private int area;
    
    /**
     * 长宽比
     */
    private double aspectRatio;
    
    /**
     * 唯一颜色数量
     */
    private int uniqueColors;
    
    /**
     * 边缘密度（0.0-1.0）
     */
    private double edgeDensity;
    
    /**
     * 构造函数
     * 
     * @param image 图片对象
     * @param position 位置信息
     */
    public ImageFilterContext(BufferedImage image, ImagePosition position) {
        this.image = image;
        this.position = position;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.area = width * height;
        this.aspectRatio = Math.max(width, height) / (double) Math.min(width, height);
    }
}
