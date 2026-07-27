package com.moyun.agent.service;

/**
 * 查询类型分类器接口
 *
 * <p>识别用户查询的类型，用于动态调整混合检索权重</p>
 *
 * @author laomao
 * @since 2025-01-22
 */
public interface QueryTypeClassifier {

    /**
     * 分类查询类型
     *
     * @param query 用户查询
     * @return 查询类型：semantic(语义查询), exact(精确查询), hybrid(混合查询)
     */
    String classifyQueryType(String query);

    /**
     * 是否为精确查询
     * <p>
     * 精确查询特征：
     * - 包含错误代码（如 E1001、ERR-500）
     * - 包含专有名词（如 Qwen3-Rerank、Redis）
     * - 包含版本号（如 v1.0.0、Java 17）
     * - 包含特定标识符（如 ID、编号）
     * </p>
     *
     * @param query 用户查询
     * @return true if exact query
     */
    boolean isExactQuery(String query);

    /**
     * 是否为语义查询
     * <p>
     * 语义查询特征：
     * - 开放性问题（如何、为什么、什么）
     * - 概念性问题（原理、机制、架构）
     * - 比较性问题（区别、对比、优劣）
     * </p>
     *
     * @param query 用户查询
     * @return true if semantic query
     */
    boolean isSemanticQuery(String query);

    /**
     * 获取推荐的检索权重
     *
     * @param queryType 查询类型
     * @return 权重配置 {vectorWeight, bm25Weight}
     */
    double[] getRecommendedWeights(String queryType);
}
