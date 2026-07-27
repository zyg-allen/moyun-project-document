package com.moyun.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智能体实体类
 *
 * <p>对应数据库表 agent，存储智能体配置信息</p>
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("agent")
public class Agent {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 智能体名称
    private String name;

    // 智能体描述
    private String description;

    // 系统提示词
    private String systemPrompt;

    // 关联的知识库ID（多个用逗号分隔）- 旧字段，兼容保留
    private String knowledgeBaseIds;
    
    // 关联的知识库ID列表（JSON数组格式）- 新字段
    private String knowledgeLibraryIds;
    
    // 知识库权重配置（JSON格式：{"1": 1.0, "2": 0.8, "3": 0.5}）
    // 权重范围 0.1-1.0，默认1.0
    // 用于多知识库检索时的结果排序和过滤
    private String knowledgeBaseWeights;

    // 使用的模型配置ID（对应数据库的model_config_id字段）
    private Long modelConfigId;

    // 温度参数
    private Double temperature;

    // 最大token数
    private Integer maxTokens;

    // RAG 检索配置（必填参数，有默认值）
    // 相似度阈值（0.3-1.0，默认0.5，推荐0.5-0.6）
    private Double ragMinScore;

    // 最大检索结果数量（1-20，默认10，推荐8-10）
    private Integer ragMaxResults;

    // 第一阶段召回倍数（1.0-5.0，默认2.0，推荐2.0）
    private Double ragRecallMultiplier;

    // 是否启用混合检索（向量+BM25，默认true）
    private Boolean ragEnableHybridSearch;

    // 是否启用查询扩展（默认true）
    private Boolean ragEnableQueryExpansion;

    // BM25检索权重（0-1，默认0.3）
    private Double ragBm25Weight;

    // 向量检索权重（0-1，默认0.7）
    private Double ragVectorWeight;

    // 是否启用
    private Boolean enabled;
    
    // ========== 扩展配置 ==========
    
    // 开场白
    private String welcomeMessage;
    
    // 预设问题（JSON数组）
    private String suggestedQuestions;
    
    // 是否显示引用来源
    private Boolean showCitations;
    
    // 最大历史轮数
    private Integer maxHistoryTurns;
    
    // 是否启用API
    private Boolean apiEnabled;
    
    // API Key
    private String apiKey;
    
    // ========== 工作流关联 ==========
    
    // 关联工作流ID
    private Long workflowId;
    
    // 工作流触发模式: manual/auto/keyword
    private String workflowTriggerMode;
    
    // 触发关键词(JSON数组)
    private String workflowTriggerKeywords;
    
    // ========== 应用发布 ==========
    
    // 是否发布为应用
    private Boolean publishEnabled;
    
    // 发布访问Token
    private String publishToken;
    
    // 发布设置(JSON)
    private String publishSettings;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
