package com.moyun.ext.ai.service.chat;

import com.moyun.ext.ai.entity.Agent;
import dev.langchain4j.rag.content.Content;

import java.util.List;
import java.util.Map;

/**
 * RAG检索服务接口
 *
 * <p>负责知识库检索相关功能，包括：
 * <ul>
 *   <li>混合检索（向量 + BM25）</li>
 *   <li>重排（Rerank）</li>
 *   <li>相邻分片合并</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
public interface RagRetrievalService {

    /**
     * 获取检索内容列表
     *
     * @param query            用户查询
     * @param knowledgeBaseIds 知识库ID列表
     * @param agent            智能体配置
     * @return 检索到的内容列表
     */
    List<Content> retrieveContents(String query, List<Long> knowledgeBaseIds, Agent agent);

    /**
     * 重排检索结果
     *
     * @param contents 原始检索结果
     * @param query    用户查询
     * @param topK     返回的最大结果数
     * @return 重排后的结果列表
     */
    List<Content> rerankContents(List<Content> contents, String query, int topK);

    /**
     * 合并相邻分片
     *
     * @param contents 原始内容列表
     * @return 合并后的内容列表
     */
    List<Content> mergeAdjacentSegments(List<Content> contents);

    /**
     * 获取当前线程的重排分数映射
     *
     * @return 重排分数映射
     */
    Map<Content, Double> getContentRerankScores();

    /**
     * 清空当前线程的重排分数映射
     */
    void clearContentRerankScores();
}
