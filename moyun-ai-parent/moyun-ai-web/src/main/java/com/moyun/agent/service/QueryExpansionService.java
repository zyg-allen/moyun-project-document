package com.moyun.agent.service;

import java.util.List;
import java.util.Map;

/**
 * 查询扩展服务接口
 *
 * <p>通过领域知识词典扩展用户查询，提升RAG召回率</p>
 *
 * @author laomao
 */
public interface QueryExpansionService {

    /**
     * 从数据库加载词典到内存
     */
    void loadFromDatabase();

    /**
     * 加载智能体的词典
     *
     * @param agentId 智能体ID
     * @return 词典Map
     */
    Map<String, List<String>> loadDictionaryForAgent(Long agentId);

    /**
     * 扩展查询（使用全局词典）
     *
     * @param originalQuery 原始查询
     * @return 扩展后的查询词列表
     */
    List<String> expandQuery(String originalQuery);

    /**
     * 扩展查询（支持智能体）
     *
     * @param originalQuery 原始查询
     * @param scope 作用域
     * @param agentId 智能体ID
     * @return 扩展后的查询词列表
     */
    List<String> expandQuery(String originalQuery, String scope, Long agentId);

    /**
     * 生成扩展查询字符串
     *
     * @param originalQuery 原始查询
     * @return 扩展后的查询字符串
     */
    String getExpandedQueryString(String originalQuery);

    /**
     * 生成扩展查询字符串（支持指定作用域）
     *
     * @param originalQuery 原始查询
     * @param scope 作用域
     * @param scopeId 作用域ID
     * @return 扩展后的查询字符串
     */
    String getExpandedQueryString(String originalQuery, String scope, Long scopeId);

    /**
     * 添加自定义领域词典
     *
     * @param key 核心词
     * @param relatedTerms 相关词列表
     */
    void addDomainTerms(String key, List<String> relatedTerms);

    /**
     * 添加自定义领域词典（完整版）
     *
     * @param key 核心词
     * @param relatedTerms 相关词列表
     * @param category 分类
     * @param description 说明
     */
    void addDomainTerms(String key, List<String> relatedTerms, String category, String description);

    /**
     * 批量添加领域词典
     *
     * @param dictionary 词典Map
     */
    void addDomainTermsBatch(Map<String, List<String>> dictionary);

    /**
     * 删除领域词典
     *
     * @param key 核心词
     */
    void removeDomainTerms(String key);

    /**
     * 获取领域词典（只读副本）
     *
     * @return 领域词典
     */
    Map<String, List<String>> getDomainDictionary();

    /**
     * 获取词典大小
     *
     * @return 核心词数量
     */
    int getDictionarySize();
}
