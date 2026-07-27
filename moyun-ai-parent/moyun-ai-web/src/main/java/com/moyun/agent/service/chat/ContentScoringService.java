package com.moyun.agent.service.chat;

import dev.langchain4j.rag.content.Content;

/**
 * 内容评分服务接口
 *
 * <p>负责计算检索内容与用户查询的相关性分数，包括：
 * <ul>
 *   <li>关键词匹配评分</li>
 *   <li>语义相关性评分</li>
 *   <li>主题一致性检查</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
public interface ContentScoringService {

    /**
     * 计算内容与查询的相关性分数
     *
     * @param text      待评分的文本内容
     * @param keywords  提取的关键词数组
     * @param fullQuery 完整的用户查询
     * @return 相关性分数
     */
    double calculateRelevanceScore(String text, String[] keywords, String fullQuery);

    /**
     * 检查主题一致性
     *
     * @param text      文本内容
     * @param fullQuery 完整查询
     * @param keywords  关键词数组
     * @return 惩罚系数（0.0-1.0）
     */
    double checkTopicConsistency(String text, String fullQuery, String[] keywords);

    /**
     * 计算语义相关分数
     *
     * @param text      待评分的文本内容
     * @param keywords  提取的关键词数组
     * @param fullQuery 完整的用户查询
     * @return 语义相关分数
     */
    double calculateSemanticScore(String text, String[] keywords, String fullQuery);

    /**
     * 计算子串在文本中出现的次数
     *
     * @param text      文本内容
     * @param substring 要查找的子串
     * @return 出现次数
     */
    int countOccurrences(String text, String substring);

    /**
     * 提取并过滤关键词
     *
     * @param query 用户查询
     * @return 关键词数组
     */
    String[] extractKeywords(String query);

    /**
     * 带分数的内容包装类
     */
    class ScoredContent {
        public Content content;
        public double score;

        public ScoredContent(Content content, double score) {
            this.content = content;
            this.score = score;
        }
    }
}
