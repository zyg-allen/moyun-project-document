package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ext.ai.constant.RedisKeys;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.mapper.AgentMapper;
import com.moyun.ext.ai.mapper.KnowledgeBaseMapper;
import com.moyun.ext.ai.entity.KnowledgeBase;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 智能体服务实现类
 * 
 * <p>核心功能：</p>
 * <ul>
 *     <li>智能体的CRUD操作</li>
 *     <li>基于Redis的缓存机制（读写穿透）</li>
 *     <li>按发布Token查询智能体</li>
 *     <li>知识库ID解析</li>
 *     <li>启用状态过滤</li>
 * </ul>
 * 
 * <p>缓存策略：</p>
 * <ul>
 *     <li>查询时：先查Redis，未命中则查数据库并缓存</li>
 *     <li>更新时：同步删除Redis缓存</li>
 *     <li>删除时：同步删除Redis缓存</li>
 *     <li>缓存过期时间：由RedisKeys.AGENT_EXPIRE_MINUTES配置</li>
 * </ul>
 * 
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 获取所有启用的智能体列表
     * 
     * <p>过滤enabled=true的智能体，按创建时间降序排列</p>
     * 
     * @return 启用的智能体列表
     */
    @Override
    public List<Agent> listEnabled() {
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Agent::getEnabled, true)
               .orderByDesc(Agent::getCreateTime);
        List<Agent> agents = this.list(wrapper);
        log.debug("📊 获取启用智能体列表，数量: {}", agents.size());
        return agents;
    }

    /**
     * 获取所有智能体列表
     * 
     * <p>按创建时间降序排列，不过滤启用状态</p>
     * <p>性能优化：添加合理的数量限制</p>
     * 
     * @return 所有智能体列表（最多1000条）
     */
    @Override
    public List<Agent> listOrderByCreateTimeDesc() {
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Agent::getCreateTime)
               .last("LIMIT 1000"); // 限制最多返回1000条
        return this.list(wrapper);
    }

    /**
     * 获取智能体关联的文档ID列表（用于RAG检索）
     * 
     * <p>支持两种格式：</p>
     * <ul>
     *   <li>新版：knowledgeLibraryIds (JSON数组) - 先查询知识库下的所有文档ID</li>
     *   <li>旧版：knowledgeBaseIds (逗号分隔) - 直接返回文档ID</li>
     * </ul>
     * 
     * @param agentId 智能体ID
     * @return 文档ID列表（knowledge_base表的id），用于检索过滤
     */
    @Override
    public List<Long> getKnowledgeBaseIds(Long agentId) {
        Agent agent = this.getById(agentId);
        if (agent == null) {
            log.warn("⚠️  智能体不存在: agentId={}", agentId);
            return Collections.emptyList();
        }

        List<Long> documentIds = new ArrayList<>();
        
        // 1. 优先使用新版格式：knowledgeLibraryIds (JSON数组)
        String libraryIds = agent.getKnowledgeLibraryIds();
        if (StringUtils.hasText(libraryIds)) {
            try {
                List<Long> libIds = objectMapper.readValue(libraryIds, new TypeReference<List<Long>>() {});
                if (libIds != null && !libIds.isEmpty()) {
                    log.info("📚 使用新版知识库格式，知识库IDs: {}", libIds);
                    // 查询这些知识库下的所有文档ID
                    for (Long libId : libIds) {
                        List<KnowledgeBase> docs = knowledgeBaseMapper.selectList(
                            new LambdaQueryWrapper<KnowledgeBase>()
                                .eq(KnowledgeBase::getLibraryId, libId)
                                .eq(KnowledgeBase::getStatus, 2)  // 只选择处理完成的
                        );
                        for (KnowledgeBase doc : docs) {
                            documentIds.add(doc.getId());
                        }
                    }
                    log.info("📄 从 {} 个知识库中获取到 {} 个文档", libIds.size(), documentIds.size());
                    // 新版格式配置了知识库，直接返回（即使为空也不回退到旧版）
                    return documentIds;
                }
            } catch (Exception e) {
                log.error("❌ 解析knowledgeLibraryIds失败: {}", libraryIds, e);
                // 解析失败才尝试旧版格式
            }
        }
        
        // 2. 兼容旧版格式：knowledgeBaseIds (逗号分隔的文档ID)
        // 只有当新版格式未配置或解析失败时才使用
        String knowledgeBaseIds = agent.getKnowledgeBaseIds();
        if (StringUtils.hasText(knowledgeBaseIds)) {
            try {
                documentIds = Arrays.stream(knowledgeBaseIds.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                log.info("📄 使用旧版格式，文档IDs: {}", documentIds);
                return documentIds;
            } catch (NumberFormatException e) {
                log.error("❌ 解析knowledgeBaseIds失败: {}", knowledgeBaseIds, e);
            }
        }
        
        return Collections.emptyList();
    }

    /**
     * 根据发布Token获取智能体
     * 
     * <p>用于公开API访问，通过发布Token认证智能体</p>
     * 
     * @param publishToken 发布Token
     * @return 智能体，如果不存在则返回null
     */
    @Override
    public Agent getByPublishToken(String publishToken) {
        if (!StringUtils.hasText(publishToken)) {
            log.warn("⚠️  publishToken为空");
            return null;
        }
        
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Agent::getPublishToken, publishToken);
        Agent agent = this.getOne(wrapper);
        
        if (agent == null) {
            log.debug("🔍 未找到对应的智能体: publishToken={}", publishToken);
        }
        
        return agent;
    }

    /**
     * 重写getById方法，添加Redis缓存支持
     * 
     * <p>缓存策略：Cache-Aside（旁路缓存）</p>
     * <ol>
     *     <li>先查询Redis缓存</li>
     *     <li>缓存命中则直接返回</li>
     *     <li>缓存未命中则查询数据库</li>
     *     <li>将查询结果写入缓存并返回</li>
     * </ol>
     * 
     * @param id 智能体ID
     * @return 智能体对象，不存在则返回null
     */
    @Override
    public Agent getById(java.io.Serializable id) {
        String cacheKey = RedisKeys.agent((Long) id);

        // 1. 尝试从Redis缓存获取
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            Agent agent = JsonUtils.fromJson(cached, Agent.class);
            if (agent != null) {
                log.debug("✅ 缓存命中: agentId={}", id);
                return agent;
            }
        }

        // 2. 缓存未命中，从数据库查询
        log.debug("🔍 缓存未命中，查询数据库: agentId={}", id);
        Agent agent = super.getById(id);

        // 3. 查询成功则写入缓存
        if (agent != null) {
            cacheAgent(cacheKey, agent);
        }

        return agent;
    }

    /**
     * 重写updateById方法，同步删除缓存
     * 
     * <p>更新成功后立即删除缓存，下次查询时重新加载最新数据</p>
     * 
     * @param entity 智能体实体
     * @return 是否更新成功
     */
    @Override
    public boolean updateById(Agent entity) {
        boolean result = super.updateById(entity);
        if (result) {
            clearAgentCache(entity.getId());
            log.debug("🔄 智能体已更新，清除缓存: agentId={}", entity.getId());
        }
        return result;
    }

    /**
     * 重写removeById方法，同步删除缓存
     * 
     * <p>删除成功后立即清除对应的缓存</p>
     * 
     * @param id 智能体ID
     * @return 是否删除成功
     */
    @Override
    public boolean removeById(java.io.Serializable id) {
        boolean result = super.removeById(id);
        if (result) {
            clearAgentCache((Long) id);
            log.info("🗑️  智能体已删除，清除缓存: agentId={}", id);
        }
        return result;
    }

    /**
     * 将智能体缓存到Redis
     * 
     * <p>序列化为JSON并设置过期时间</p>
     * 
     * @param key Redis键
     * @param agent 智能体对象
     */
    private void cacheAgent(String key, Agent agent) {
        String json = JsonUtils.toJson(agent);
        if (json != null) {
            try {
                redisTemplate.opsForValue().set(key, json, RedisKeys.AGENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
                log.debug("💾 智能体已缓存: agentId={}, ttl={}分钟", agent.getId(), RedisKeys.AGENT_EXPIRE_MINUTES);
            } catch (Exception e) {
                log.warn("⚠️  缓存智能体失败: agentId={}, error={}", agent.getId(), e.getMessage());
            }
        }
    }

    /**
     * 清除智能体的Redis缓存
     * 
     * @param agentId 智能体ID
     */
    private void clearAgentCache(Long agentId) {
        try {
            String cacheKey = RedisKeys.agent(agentId);
            Boolean deleted = redisTemplate.delete(cacheKey);
            log.debug("🧹 清除智能体缓存: agentId={}, deleted={}", agentId, deleted);
        } catch (Exception e) {
            log.warn("⚠️  清除智能体缓存失败: agentId={}, error={}", agentId, e.getMessage());
        }
    }
}
