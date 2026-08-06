package com.moyun.ext.ai.service;

import java.awt.image.BufferedImage;

/**
 * 多模态服务接口
 *
 * <p>提供图片理解功能，支持使用多模态大模型（如GPT-4V、通义千问VL）分析图片内容</p>
 *
 * @author laomao
 */
public interface MultimodalService {

    /**
     * 使用多模态模型理解图片内容（本地文件路径）
     *
     * @param imagePath 图片文件路径
     * @param prompt 提示词
     * @return 图片内容描述
     */
    String understandImage(String imagePath, String prompt);

    /**
     * 使用多模态模型理解图片内容（远程URL）
     *
     * @param imageUrl 图片远程URL
     * @param prompt 提示词
     * @return 图片内容描述
     */
    String understandImageFromUrl(String imageUrl, String prompt);

    /**
     * 使用多模态模型理解图片（BufferedImage）
     *
     * @param image 图片对象
     * @param prompt 提示词
     * @return 图片内容描述
     */
    String understandImage(BufferedImage image, String prompt);

    /**
     * 批量理解图片
     *
     * @param images 图片数组
     * @param prompt 提示词
     * @return 所有图片的描述
     */
    String batchUnderstandImages(BufferedImage[] images, String prompt);
}
