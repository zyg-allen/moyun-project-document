package com.moyun.ext.ai.service.chat;

import com.moyun.ext.ai.entity.Agent;

import java.util.List;

/**
 * 查询改写服务接口
 *
 * <p>使用LLM对用户查询进行改写优化，提升检索效果：
 * <ul>
 *   <li>查询扩展 - 添加同义词、相关概念</li>
 *   <li>查询分解 - 将复杂问题分解为多个子查询</li>
 *   <li>查询规范化 - 修正口语化表述</li>
 *   <li>HyDE - 生成假设答案用于检索</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-12
 */
public interface QueryRewritingService {

    /**
     * 改写用户查询
     *
     * <p>使用LLM将用户的原始查询改写为更适合向量检索的形式</p>
     *
     * @param originalQuery 原始用户查询
     * @param agent         智能体配置（包含领域信息）
     * @return 改写后的查询
     */
    String rewriteQuery(String originalQuery, Agent agent);

    /**
     * 将查询分解为多个子查询
     *
     * <p>适用于复杂问题，分解后可进行多路检索再合并</p>
     *
     * @param originalQuery 原始用户查询
     * @param agent         智能体配置
     * @return 子查询列表
     */
    List<String> decomposeQuery(String originalQuery, Agent agent);

    /**
     * 生成假设文档（HyDE）
     *
     * <p>让LLM生成一个假设的答案，用这个答案去检索相关文档，
     * 可以更好地捕捉语义</p>
     *
     * @param originalQuery 原始用户查询
     * @param agent         智能体配置
     * @return 假设文档内容
     */
    String generateHypotheticalDocument(String originalQuery, Agent agent);

    /**
     * 判断是否需要查询改写
     *
     * <p>短查询、专业术语查询可能不需要改写</p>
     *
     * @param query 用户查询
     * @return 是否需要改写
     */
    boolean shouldRewrite(String query);
}
