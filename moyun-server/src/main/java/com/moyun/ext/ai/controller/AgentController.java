package com.moyun.ext.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.common.ListResponse;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.AgentDictionaryRelation;
import com.moyun.ext.ai.entity.DomainDictionary;
import com.moyun.ext.ai.mapper.AgentDictionaryRelationMapper;
import com.moyun.ext.ai.mapper.AgentToolRelationMapper;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.ChatHistoryService;
import com.moyun.ext.ai.service.DomainDictionaryService;
import com.moyun.ext.ai.vo.AgentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "智能体管理")
@RestController
@RequestMapping("/cms/ai/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private DomainDictionaryService dictionaryService;

    @Autowired
    private AgentDictionaryRelationMapper relationMapper;

    @Autowired
    private AgentToolRelationMapper toolRelationMapper;
    
    @Autowired
    private ChatHistoryService chatHistoryService;

    @Operation(summary = "获取所有智能体")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:list')")
    public AjaxResult getList() {
        try {
            List<Agent> list = agentService.listOrderByCreateTimeDesc();
            List<AgentVO> voList = list.stream().map(agent -> {
                AgentVO vo = AgentVO.from(agent);
                Long toolCount = toolRelationMapper.selectCount(
                    new QueryWrapper<com.moyun.ext.ai.entity.AgentToolRelation>()
                        .eq("agent_id", agent.getId())
                        .eq("enabled", true)
                );
                vo.setToolCount(toolCount.intValue());
                
                try {
                    java.util.Map<String, Object> stats = chatHistoryService.getAgentStats(agent.getId());
                    if (stats != null) {
                        Object sessionCount = stats.get("session_count");
                        Object messageCount = stats.get("message_count");
                        Object totalTokens = stats.get("total_tokens");
                        vo.setSessionCount(sessionCount != null ? ((Number) sessionCount).intValue() : 0);
                        vo.setMessageCount(messageCount != null ? ((Number) messageCount).intValue() : 0);
                        vo.setTotalTokens(totalTokens != null ? ((Number) totalTokens).longValue() : 0L);
                    }
                } catch (Exception e) {
                    log.debug("获取智能体统计失败: {}", e.getMessage());
                }
                return vo;
            }).collect(Collectors.toList());
            return AjaxResult.success(new ListResponse<>(voList));
        } catch (Exception e) {
            log.error("获取智能体列表失败", e);
            return AjaxResult.error("获取列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取启用的智能体")
    @GetMapping("/enabled")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:list')")
    public AjaxResult getEnabled() {
        try {
            List<Agent> list = agentService.listEnabled();
            return AjaxResult.success(new ListResponse<>(list));
        } catch (Exception e) {
            log.error("获取启用智能体列表失败", e);
            return AjaxResult.error("获取列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建智能体")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:add')")
    public AjaxResult create(@RequestBody Agent agent) {
        try {
            log.info("========== 创建智能体 ==========");
            log.info("名称: {}", agent.getName());
            log.info("知识库IDs(新): {}", agent.getKnowledgeLibraryIds());
            log.info("知识库IDs(旧): {}", agent.getKnowledgeBaseIds());
            log.info("知识库权重: {}", agent.getKnowledgeBaseWeights());
            
            agent.setCreateTime(LocalDateTime.now());
            agent.setUpdateTime(LocalDateTime.now());
            if (agent.getEnabled() == null) {
                agent.setEnabled(true);
            }
            agentService.save(agent);
            
            log.info("✅ 创建智能体成功 - ID: {}, 名称: {}", agent.getId(), agent.getName());
            log.info("✅ 保存后的知识库IDs: {}", agent.getKnowledgeLibraryIds());
            log.info("=====================================");
            
            return AjaxResult.success("创建成功", agent);
        } catch (Exception e) {
            log.error("❌ 创建智能体失败", e);
            return AjaxResult.error("创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新智能体")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:edit')")
    public AjaxResult update(@RequestBody Agent agent) {
        try {
            log.info("========== 更新智能体 ==========");
            log.info("ID: {}", agent.getId());
            log.info("名称: {}", agent.getName());
            log.info("知识库IDs(新): {}", agent.getKnowledgeLibraryIds());
            log.info("知识库IDs(旧): {}", agent.getKnowledgeBaseIds());
            log.info("知识库权重: {}", agent.getKnowledgeBaseWeights());
            
            agent.setUpdateTime(LocalDateTime.now());
            agentService.updateById(agent);
            
            log.info("✅ 更新智能体成功 - ID: {}", agent.getId());
            log.info("=====================================");
            
            return AjaxResult.success("更新成功");
        } catch (Exception e) {
            log.error("❌ 更新智能体失败 - ID: {}", agent.getId(), e);
            return AjaxResult.error("更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除智能体")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:remove')")
    public AjaxResult delete(@PathVariable("id") Long id) {
        try {
            agentService.removeById(id);
            log.info("删除智能体成功 - ID: {}", id);
            return AjaxResult.success("删除成功");
        } catch (Exception e) {
            log.error("删除智能体失败 - ID: {}", id, e);
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取智能体详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:query')")
    public AjaxResult getDetail(@PathVariable("id") Long id) {
        try {
            Agent agent = agentService.getById(id);
            if (agent != null) {
                return AjaxResult.success(agent);
            } else {
                return AjaxResult.error("记录不存在");
            }
        } catch (Exception e) {
            log.error("获取智能体详情失败 - ID: {}", id, e);
            return AjaxResult.error("获取详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取智能体关联的词典")
    @GetMapping("/{id}/dictionaries")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:query')")
    public AjaxResult getAgentDictionaries(@PathVariable("id") Long id) {
        try {
            QueryWrapper<AgentDictionaryRelation> wrapper = new QueryWrapper<>();
            wrapper.eq("agent_id", id);
            wrapper.eq("enabled", true);
            List<AgentDictionaryRelation> relations = relationMapper.selectList(wrapper);

            List<Long> dictionaryIds = relations.stream()
                .map(AgentDictionaryRelation::getDictionaryId)
                .collect(Collectors.toList());

            return AjaxResult.success(dictionaryIds);
        } catch (Exception e) {
            log.error("获取智能体词典失败 - ID: {}", id, e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新智能体关联的词典")
    @PostMapping("/{id}/dictionaries")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:edit')")
    public AjaxResult updateAgentDictionaries(
            @PathVariable("id") Long id,
            @RequestBody List<Long> dictionaryIds) {
        try {
            QueryWrapper<AgentDictionaryRelation> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("agent_id", id);
            relationMapper.delete(deleteWrapper);

            if (dictionaryIds != null && !dictionaryIds.isEmpty()) {
                for (Long dictionaryId : dictionaryIds) {
                    AgentDictionaryRelation relation = new AgentDictionaryRelation();
                    relation.setAgentId(id);
                    relation.setDictionaryId(dictionaryId);
                    relation.setEnabled(true);
                    relation.setCreateTime(LocalDateTime.now());
                    relationMapper.insert(relation);
                }
            }

            log.info("更新智能体词典关联成功 - AgentID: {}, 词典数: {}", id,
                dictionaryIds != null ? dictionaryIds.size() : 0);
            return AjaxResult.success("更新成功");
        } catch (Exception e) {
            log.error("更新智能体词典失败 - ID: {}", id, e);
            return AjaxResult.error("更新失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取所有专业词典（供智能体选择）")
    @GetMapping("/available-dictionaries")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:list')")
    public AjaxResult getAvailableDictionaries() {
        try {
            List<DomainDictionary> dictionaries = dictionaryService.listProfessional();
            return AjaxResult.success(dictionaries);
        } catch (Exception e) {
            log.error("获取可用词典失败", e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "获取智能体对话统计")
    @GetMapping("/{id}/stats")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:query')")
    public AjaxResult getAgentStats(@PathVariable Long id) {
        try {
            Map<String, Object> stats = chatHistoryService.getAgentStats(id);
            return AjaxResult.success(stats);
        } catch (Exception e) {
            log.error("获取智能体统计失败 - ID: {}", id, e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "获取智能体会话列表")
    @GetMapping("/{id}/sessions")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:query')")
    public AjaxResult getAgentSessions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<Map<String, Object>> sessions = chatHistoryService.getAgentSessions(id, limit);
            return AjaxResult.success(sessions);
        } catch (Exception e) {
            log.error("获取智能体会话列表失败 - ID: {}", id, e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "清空智能体对话历史")
    @DeleteMapping("/{id}/history")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:remove')")
    public AjaxResult clearAgentHistory(@PathVariable Long id) {
        try {
            chatHistoryService.clearAgentHistory(id);
            return AjaxResult.success("清空成功", null);
        } catch (Exception e) {
            log.error("清空智能体对话历史失败 - ID: {}", id, e);
            return AjaxResult.error("清空失败: " + e.getMessage());
        }
    }
}
