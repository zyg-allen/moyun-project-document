package com.moyun.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moyun.agent.entity.AgentDictionaryRelation;
import com.moyun.agent.entity.DomainDictionary;
import com.moyun.agent.mapper.AgentDictionaryRelationMapper;
import com.moyun.agent.mapper.DomainDictionaryMapper;
import com.moyun.agent.service.QueryExpansionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 查询扩展服务实现类
 * 
 * <p>核心功能：</p>
 * <ul>
 *     <li>通过领域知识词典扩展用户查询，提升RAG召回率</li>
 *     <li>支持全局词典和智能体特定词典</li>
 *     <li>使用ConcurrentHashMap实现线程安全的内存缓存</li>
 * </ul>
 * 
 * <p>线程安全：ConcurrentHashMap保证并发读写安全，无需synchronized</p>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExpansionServiceImpl implements QueryExpansionService {

    private final DomainDictionaryMapper dictionaryMapper;
    private final AgentDictionaryRelationMapper relationMapper;

    /** 
     * 领域知识词典（内存缓存）
     * 使用ConcurrentHashMap保证线程安全，支持高并发读写
     */
    private final Map<String, List<String>> domainDictionary = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadFromDatabase();
    }

    @Override
    public void loadFromDatabase() {
        try {
            QueryWrapper<DomainDictionary> wrapper = new QueryWrapper<>();
            wrapper.eq("enabled", true);
            wrapper.eq("is_global", true);
            wrapper.orderByDesc("priority");

            List<DomainDictionary> dictionaries = dictionaryMapper.selectList(wrapper);
            domainDictionary.clear();

            for (DomainDictionary dict : dictionaries) {
                String keyword = dict.getKeyword();
                String relatedTermsStr = dict.getRelatedTerms();
                if (relatedTermsStr != null && !relatedTermsStr.isEmpty()) {
                    domainDictionary.put(keyword, Arrays.asList(relatedTermsStr.split(",")));
                }
            }

            log.info("从数据库加载全局领域词典完成，共 {} 个核心词", domainDictionary.size());
        } catch (Exception e) {
            log.error("从数据库加载领域词典失败，使用默认词典", e);
            initDefaultDictionary();
        }
    }

    @Override
    public Map<String, List<String>> loadDictionaryForAgent(Long agentId) {
        Map<String, List<String>> result = new HashMap<>(domainDictionary);

        try {
            QueryWrapper<AgentDictionaryRelation> relationWrapper = new QueryWrapper<>();
            relationWrapper.eq("agent_id", agentId);
            relationWrapper.eq("enabled", true);
            List<AgentDictionaryRelation> relations = relationMapper.selectList(relationWrapper);

            if (relations.isEmpty()) {
                return result;
            }

            List<Long> dictionaryIds = relations.stream()
                .map(AgentDictionaryRelation::getDictionaryId)
                .collect(Collectors.toList());

            QueryWrapper<DomainDictionary> dictWrapper = new QueryWrapper<>();
            dictWrapper.in("id", dictionaryIds);
            dictWrapper.eq("enabled", true);
            dictWrapper.orderByDesc("priority");

            List<DomainDictionary> dictionaries = dictionaryMapper.selectList(dictWrapper);

            for (DomainDictionary dict : dictionaries) {
                String keyword = dict.getKeyword();
                String relatedTermsStr = dict.getRelatedTerms();
                if (relatedTermsStr != null && !relatedTermsStr.isEmpty()) {
                    result.put(keyword, Arrays.asList(relatedTermsStr.split(",")));
                }
            }
        } catch (Exception e) {
            log.error("加载智能体词典失败", e);
        }

        return result;
    }

    private void initDefaultDictionary() {
        domainDictionary.put("服务器", Arrays.asList("cpu", "gpu", "npu", "内存", "存储", "硬盘", "系统盘", "数据盘", "鲲鹏", "昇腾", "算力", "主机", "机器", "配置", "规格"));
        domainDictionary.put("架构", Arrays.asList("系统架构", "技术架构", "平台架构", "设计", "模块", "组件", "层次", "结构", "框架"));
        domainDictionary.put("模型", Arrays.asList("大模型", "embedding", "向量", "llm", "ai模型", "算法", "训练", "推理"));
        domainDictionary.put("知识库", Arrays.asList("文档", "向量库", "rag", "检索", "知识管理", "知识图谱"));
        domainDictionary.put("部署", Arrays.asList("安装", "配置", "环境", "运维", "上线", "发布"));
        domainDictionary.put("性能", Arrays.asList("速度", "效率", "吞吐量", "延迟", "响应时间", "优化"));
        domainDictionary.put("安全", Arrays.asList("权限", "认证", "授权", "加密", "防护", "隔离"));
        log.warn("使用默认领域词典，共 {} 个核心词", domainDictionary.size());
    }

    @Override
    public List<String> expandQuery(String originalQuery) {
        return expandQuery(originalQuery, null, null);
    }

    @Override
    public List<String> expandQuery(String originalQuery, String scope, Long agentId) {
        Map<String, List<String>> dictionary = (agentId != null)
            ? loadDictionaryForAgent(agentId)
            : domainDictionary;

        Set<String> expandedTerms = new LinkedHashSet<>();
        String[] originalTerms = originalQuery.toLowerCase().split("\\s+");
        expandedTerms.addAll(Arrays.asList(originalTerms));

        // 🚀 优化：限制扩展词数量，避免引入过多噪音
        int maxExpansionTerms = 5; // 最多扩展5个词
        int addedCount = 0;

        for (String term : originalTerms) {
            if (dictionary.containsKey(term)) {
                List<String> relatedTerms = dictionary.get(term);
                
                // 优先添加高优先级的扩展词
                for (String relatedTerm : relatedTerms) {
                    if (addedCount >= maxExpansionTerms) {
                        log.debug("⚠️ 已达到扩展词数量上限 ({}), 停止扩展", maxExpansionTerms);
                        break;
                    }
                    
                    // 避免添加已存在的词
                    if (!expandedTerms.contains(relatedTerm.toLowerCase())) {
                        expandedTerms.add(relatedTerm.toLowerCase());
                        addedCount++;
                        log.debug("✅ 扩展词: {} -> {}", term, relatedTerm);
                    }
                }
                
                if (addedCount >= maxExpansionTerms) {
                    break;
                }
            }
        }

        log.info("📊 查询扩展完成: 原始词数={}, 扩展词数={}, 总词数={}", 
                 originalTerms.length, addedCount, expandedTerms.size());

        return new ArrayList<>(expandedTerms);
    }

    @Override
    public String getExpandedQueryString(String originalQuery) {
        return getExpandedQueryString(originalQuery, null, null);
    }

    @Override
    public String getExpandedQueryString(String originalQuery, String scope, Long scopeId) {
        List<String> expandedTerms = expandQuery(originalQuery, scope, scopeId);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(originalQuery).append(" ");

        String expandedPart = expandedTerms.stream()
                .filter(term -> !originalQuery.contains(term))
                .collect(Collectors.joining(" "));

        queryBuilder.append(expandedPart);
        return queryBuilder.toString().trim();
    }

    @Override
    public void addDomainTerms(String key, List<String> relatedTerms) {
        addDomainTerms(key, relatedTerms, "general", null);
    }

    /**
     * 添加领域词汇
     * 
     * <p>同时更新数据库和内存缓存</p>
     * <p>线程安全：ConcurrentHashMap.put是线程安全的</p>
     * 
     * @param key 关键词
     * @param relatedTerms 相关词汇列表
     * @param category 分类
     * @param description 描述
     */
    @Override
    public void addDomainTerms(String key, List<String> relatedTerms, String category, String description) {
        try {
            // 1. 更新数据库（异步操作，不阻塞内存缓存更新）
            DomainDictionary dict = new DomainDictionary();
            dict.setKeyword(key);
            dict.setRelatedTerms(String.join(",", relatedTerms));
            dict.setCategory(category);
            dict.setDescription(description);
            dict.setEnabled(true);
            dict.setPriority(0);

            QueryWrapper<DomainDictionary> wrapper = new QueryWrapper<>();
            wrapper.eq("keyword", key);
            DomainDictionary existing = dictionaryMapper.selectOne(wrapper);

            if (existing != null) {
                dict.setId(existing.getId());
                dictionaryMapper.updateById(dict);
            } else {
                dictionaryMapper.insert(dict);
            }
            
            log.debug("💾 领域词典已保存到数据库: key={}", key);
        } catch (Exception e) {
            log.error("❌ 添加领域词典失败 - {}", key, e);
        } finally {
            // 2. 更新内存缓存（总是执行，即使数据库失败）
            // ConcurrentHashMap.put是原子操作，线程安全
            domainDictionary.put(key, new ArrayList<>(relatedTerms));
            log.debug("✅ 领域词典已更新到缓存: key={}", key);
        }
    }

    /**
     * 批量添加领域词汇
     * 
     * <p>ConcurrentHashMap的forEach和put都是线程安全的，无需synchronized</p>
     * 
     * @param dictionary 词典Map
     */
    @Override
    public void addDomainTermsBatch(Map<String, List<String>> dictionary) {
        if (dictionary == null || dictionary.isEmpty()) {
            return;
        }
        
        // ConcurrentHashMap.putAll比forEach+put更高效
        Map<String, List<String>> copyMap = new HashMap<>();
        dictionary.forEach((key, terms) -> copyMap.put(key, new ArrayList<>(terms)));
        domainDictionary.putAll(copyMap);
        
        log.info("📊 批量添加领域词典: count={}", dictionary.size());
    }

    /**
     * 删除领域词汇
     * 
     * <p>ConcurrentHashMap.remove是线程安全的，无需synchronized</p>
     * 
     * @param key 关键词
     */
    @Override
    public void removeDomainTerms(String key) {
        List<String> removed = domainDictionary.remove(key);
        if (removed != null) {
            log.debug("🗑️  删除领域词典: key={}", key);
        }
    }

    @Override
    public Map<String, List<String>> getDomainDictionary() {
        return new HashMap<>(domainDictionary);
    }

    @Override
    public int getDictionarySize() {
        return domainDictionary.size();
    }
}
