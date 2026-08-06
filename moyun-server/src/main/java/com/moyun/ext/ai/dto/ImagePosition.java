package com.moyun.ext.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片位置信息DTO
 * 
 * <p>记录图片在PDF页面中的位置坐标和相对位置信息</p>
 * 
 * @author laomao
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagePosition {
    
    /**
     * 图片左上角X坐标（绝对坐标）
     */
    private float x;
    
    /**
     * 图片左上角Y坐标（绝对坐标）
     */
    private float y;
    
    /**
     * 图片宽度
     */
    private float width;
    
    /**
     * 图片高度
     */
    private float height;
    
    /**
     * 页面总宽度
     */
    private float pageWidth;
    
    /**
     * 页面总高度
     */
    private float pageHeight;
    
    /**
     * 获取相对X坐标（0.0-1.0）
     * 
     * @return X坐标占页面宽度的比例
     */
    public float getRelativeX() {
        return pageWidth > 0 ? x / pageWidth : 0;
    }
    
    /**
     * 获取相对Y坐标（0.0-1.0）
     * 
     * @return Y坐标占页面高度的比例
     */
    public float getRelativeY() {
        return pageHeight > 0 ? y / pageHeight : 0;
    }
    
    /**
     * 判断是否在页眉或页脚区域
     * 
     * <p>页眉：顶部10%区域；页脚：底部15%区域</p>
     * 
     * @return true-在页眉或页脚，false-不在
     */
    public boolean isInHeaderOrFooter() {
        float relY = getRelativeY();
        return relY < 0.1f || relY > 0.85f;
    }
    
    /**
     * 判断是否在页面边距区域
     * 
     * <p>左右边距各占5%</p>
     * 
     * @return true-在边距，false-不在
     */
    public boolean isInMargin() {
        float relX = getRelativeX();
        return relX < 0.05f || relX > 0.95f;
    }
    
    /**
     * 判断是否在页面中心内容区
     * 
     * <p>垂直方向30%-70%区域</p>
     * 
     * @return true-在中心区，false-不在
     */
    public boolean isInContentCenter() {
        float relY = getRelativeY();
        return relY >= 0.3f && relY <= 0.7f;
    }
    
    /**
     * 获取图片所在的页面区域标识
     * 
     * @return 区域名称：HEADER_TOP/HEADER/CONTENT_CENTER/CONTENT_TOP/CONTENT_BOTTOM/FOOTER/FOOTER_BOTTOM/MARGIN_LEFT/MARGIN_RIGHT
     */
    public String getRegion() {
        float relY = getRelativeY();
        float relX = getRelativeX();
        
        // 垂直位置优先判断
        if (relY < 0.05f) return "HEADER_TOP";
        if (relY < 0.1f) return "HEADER";
        if (relY > 0.95f) return "FOOTER_BOTTOM";
        if (relY > 0.85f) return "FOOTER";
        
        // 水平边距判断
        if (relX < 0.05f) return "MARGIN_LEFT";
        if (relX > 0.95f) return "MARGIN_RIGHT";
        
        // 内容区域判断
        if (relY >= 0.3f && relY <= 0.7f) return "CONTENT_CENTER";
        if (relY < 0.3f) return "CONTENT_TOP";
        return "CONTENT_BOTTOM";
    }
}
