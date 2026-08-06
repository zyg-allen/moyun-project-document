package com.moyun.ext.ai.model;

import java.util.List;

/**
 * 多模态 Embedding 模型接口
 * 
 * <p>支持文本、图片、视频等多种模态的向量化，生成统一语义空间的向量表示。
 * 适用于跨模态检索场景（如文本查图片、图片查文本）。</p>
 * 
 * <p><b>注意：</b>此接口为预留接口，等待 DashScope API 支持 Qwen3-VL-Embedding 后实现。
 * 当前可以通过本地部署 vLLM 服务来使用。</p>
 * 
 * @author laomao
 * @since 2025-01-22
 * @see <a href="https://github.com/QwenLM/Qwen3-VL-Embedding">Qwen3-VL-Embedding</a>
 */
public interface MultimodalEmbeddingModel {
    
    /**
     * 对文本进行向量化
     * 
     * @param text 文本内容
     * @return 向量表示（维度取决于模型，如 2048 或 4096）
     */
    float[] embedText(String text);
    
    /**
     * 对图片进行向量化
     * 
     * @param imageBytes 图片字节数组（支持 JPEG、PNG 等格式）
     * @return 向量表示
     */
    float[] embedImage(byte[] imageBytes);
    
    /**
     * 对图片进行向量化（支持 URL）
     * 
     * @param imageUrl 图片 URL
     * @return 向量表示
     */
    float[] embedImage(String imageUrl);
    
    /**
     * 对视频进行向量化
     * 
     * @param videoBytes 视频字节数组
     * @param fps 采样帧率（默认 1.0）
     * @param maxFrames 最大帧数（默认 64）
     * @return 向量表示
     */
    float[] embedVideo(byte[] videoBytes, double fps, int maxFrames);
    
    /**
     * 对多模态内容进行向量化（文本 + 图片）
     * 
     * @param text 文本内容
     * @param imageBytes 图片字节数组
     * @return 向量表示（融合文本和图片的语义）
     */
    float[] embedMultimodal(String text, byte[] imageBytes);
    
    /**
     * 批量向量化（支持混合模态）
     * 
     * @param inputs 输入列表，每个元素可以是文本、图片或混合
     * @return 向量列表
     */
    List<float[]> embedBatch(List<MultimodalInput> inputs);
    
    /**
     * 获取向量维度
     * 
     * @return 向量维度（如 2048、4096）
     */
    int getDimension();
    
    /**
     * 是否支持 Matryoshka 表示学习（MRL）
     * 
     * <p>如果支持，可以通过 {@link #setDimension(int)} 动态调整向量维度</p>
     * 
     * @return true 表示支持
     */
    boolean supportsMRL();
    
    /**
     * 设置向量维度（仅在支持 MRL 时有效）
     * 
     * @param dimension 目标维度（如 512、768、1024、1536、2048、3072、4096）
     */
    void setDimension(int dimension);
}
