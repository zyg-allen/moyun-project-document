package com.moyun.ext.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多模态输入
 * 
 * <p>封装文本、图片、视频等多种模态的输入数据</p>
 * 
 * @author laomao
 * @since 2025-01-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultimodalInput {
    
    /**
     * 输入类型
     */
    public enum Type {
        /** 纯文本 */
        TEXT,
        /** 纯图片 */
        IMAGE,
        /** 纯视频 */
        VIDEO,
        /** 文本 + 图片 */
        TEXT_IMAGE,
        /** 文本 + 视频 */
        TEXT_VIDEO
    }
    
    /**
     * 输入类型
     */
    private Type type;
    
    /**
     * 文本内容（可选）
     */
    private String text;
    
    /**
     * 图片字节数组（可选）
     */
    private byte[] imageBytes;
    
    /**
     * 图片 URL（可选，与 imageBytes 二选一）
     */
    private String imageUrl;
    
    /**
     * 视频字节数组（可选）
     */
    private byte[] videoBytes;
    
    /**
     * 视频 URL（可选，与 videoBytes 二选一）
     */
    private String videoUrl;
    
    /**
     * 视频采样帧率（默认 1.0）
     */
    private Double fps;
    
    /**
     * 视频最大帧数（默认 64）
     */
    private Integer maxFrames;
    
    /**
     * 任务指令（可选，用于指导模型理解任务）
     * 
     * <p>例如："Retrieve images or text relevant to the user's query."</p>
     */
    private String instruction;
    
    // ========== 便捷构造方法 ==========
    
    /**
     * 创建纯文本输入
     */
    public static MultimodalInput text(String text) {
        return MultimodalInput.builder()
            .type(Type.TEXT)
            .text(text)
            .build();
    }
    
    /**
     * 创建纯图片输入（字节数组）
     */
    public static MultimodalInput image(byte[] imageBytes) {
        return MultimodalInput.builder()
            .type(Type.IMAGE)
            .imageBytes(imageBytes)
            .build();
    }
    
    /**
     * 创建纯图片输入（URL）
     */
    public static MultimodalInput image(String imageUrl) {
        return MultimodalInput.builder()
            .type(Type.IMAGE)
            .imageUrl(imageUrl)
            .build();
    }
    
    /**
     * 创建文本 + 图片输入
     */
    public static MultimodalInput textImage(String text, byte[] imageBytes) {
        return MultimodalInput.builder()
            .type(Type.TEXT_IMAGE)
            .text(text)
            .imageBytes(imageBytes)
            .build();
    }
    
    /**
     * 创建文本 + 图片输入（URL）
     */
    public static MultimodalInput textImage(String text, String imageUrl) {
        return MultimodalInput.builder()
            .type(Type.TEXT_IMAGE)
            .text(text)
            .imageUrl(imageUrl)
            .build();
    }
    
    /**
     * 创建视频输入
     */
    public static MultimodalInput video(byte[] videoBytes, double fps, int maxFrames) {
        return MultimodalInput.builder()
            .type(Type.VIDEO)
            .videoBytes(videoBytes)
            .fps(fps)
            .maxFrames(maxFrames)
            .build();
    }
}
