package com.moyun.agent.service;

import com.moyun.agent.entity.KnowledgeConfig;

/**
 * 自适应分片服务接口
 *
 * <p>根据文档类型和配置，动态选择最佳的分片策略</p>
 *
 * @author laomao
 * @since 2025-01-22
 */
public interface AdaptiveChunkingService {

    /**
     * 获取自适应分片大小
     *
     * @param config       知识库配置
     * @param documentType 文档类型
     * @param content      文档内容
     * @return 分片大小（字符数）
     */
    int getAdaptiveChunkSize(KnowledgeConfig config, String documentType, String content);

    /**
     * 获取智能边界位置
     * <p>
     * 在指定位置附近查找最近的句子边界（句号、问号、感叹号、换行符）
     * </p>
     *
     * @param content      文档内容
     * @param targetPos    目标位置
     * @param searchRadius 搜索半径（向前向后各搜索多少字符）
     * @return 智能边界位置
     */
    int findSmartBoundary(String content, int targetPos, int searchRadius);

    /**
     * 是否应该使用智能边界
     *
     * @param config       知识库配置
     * @param documentType 文档类型
     * @return true if should use smart boundary
     */
    boolean shouldUseSmartBoundary(KnowledgeConfig config, String documentType);
}
