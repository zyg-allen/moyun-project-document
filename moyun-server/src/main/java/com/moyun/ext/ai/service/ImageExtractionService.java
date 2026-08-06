package com.moyun.ext.ai.service;

import com.moyun.ext.ai.entity.DocumentImage;
import com.moyun.ext.ai.entity.KnowledgeBase;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

/**
 * 图片提取服务接口
 *
 * <p>从PDF文档中提取图片，使用多模态模型理解图片内容，并将描述向量化存储</p>
 *
 * @author laomao
 */
public interface ImageExtractionService {

    /**
     * 从PDF中提取所有图片并向量化
     *
     * @param knowledge 知识库实体
     * @param embeddingModel 向量模型
     * @return 提取的图片列表
     */
    List<DocumentImage> extractAndVectorizeImages(KnowledgeBase knowledge, EmbeddingModel embeddingModel);
}
