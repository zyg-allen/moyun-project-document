package com.moyun.ext.ai2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI统一接入层-场景注册配置实体
 *
 * <p>对应表 ai2_scene_registry。依据《AI能力统一接入层 — 完整方案文档》V2.0 §3.1。</p>
 *
 * <p>与 AI 底座的 ai_scene_config 职责区分：本表管理统一网关的场景执行配置
 * （Handler 路由 / Prompt 模板 / 限流 / 降级 / 缓存），底座表管理场景与模型的绑定关系。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
@TableName("ai2_scene_registry")
public class AiSceneRegistryConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 场景代码（唯一） */
    @TableField("scene_code")
    private String sceneCode;

    /** 场景名称 */
    @TableField("scene_name")
    private String sceneName;

    /** 分类: chat/analysis/generation/classification */
    @TableField("scene_category")
    private String sceneCategory;

    /** 场景描述 */
    @TableField("description")
    private String description;

    /** 绑定类型: agent/workflow/model/knowledge_only */
    @TableField("bind_type")
    private String bindType;

    /** Agent ID */
    @TableField("agent_id")
    private Long agentId;

    /** 工作流 ID */
    @TableField("workflow_id")
    private Long workflowId;

    /** 直绑模型 ID */
    @TableField("model_id")
    private Long modelId;

    /** 知识库ID列表（JSON数组字符串） */
    @TableField("knowledge_base_ids")
    private String knowledgeBaseIds;

    /** 工具ID列表（JSON数组字符串） */
    @TableField("tool_ids")
    private String toolIds;

    /** Handler Spring Bean 名称 */
    @TableField("handler_bean_name")
    private String handlerBeanName;

    /** 执行方法名 */
    @TableField("handler_method")
    private String handlerMethod;

    /** 系统提示词模板（支持 {{variable}} 占位符） */
    @TableField("system_prompt_template")
    private String systemPromptTemplate;

    /** 用户提示词模板 */
    @TableField("user_prompt_template")
    private String userPromptTemplate;

    /** 占位符说明（JSON对象字符串） */
    @TableField("prompt_placeholders")
    private String promptPlaceholders;

    /** 输出模式: sync/stream/both */
    @TableField("output_mode")
    private String outputMode;

    /** 输出结构定义（JSON） */
    @TableField("output_schema")
    private String outputSchema;

    /** 解析器: json/markdown/custom */
    @TableField("output_parser")
    private String outputParser;

    /** 最大Token数 */
    @TableField("max_tokens")
    private Integer maxTokens;

    /** 温度 */
    @TableField("temperature")
    private Double temperature;

    /** 超时（秒） */
    @TableField("timeout_seconds")
    private Integer timeoutSeconds;

    /** 重试次数 */
    @TableField("retry_count")
    private Integer retryCount;

    /** 限流Key */
    @TableField("rate_limit_key")
    private String rateLimitKey;

    /** 限流次数 */
    @TableField("rate_limit_count")
    private Integer rateLimitCount;

    /** 限流时间窗口（秒） */
    @TableField("rate_limit_time")
    private Integer rateLimitTime;

    /** 备用模型ID */
    @TableField("fallback_model_id")
    private Long fallbackModelId;

    /** 兜底回复（AI不可用时返回，JSON字符串） */
    @TableField("fallback_response")
    private String fallbackResponse;

    /** 是否启用缓存: 0/1 */
    @TableField("enable_cache")
    private Integer enableCache;

    /** 缓存时间（秒） */
    @TableField("cache_ttl")
    private Integer cacheTtl;

    /** 是否启用: 0/1 */
    @TableField("is_active")
    private Integer isActive;

    /** 是否默认: 0/1 */
    @TableField("is_default")
    private Integer isDefault;

    /** 优先级（越大越优先） */
    @TableField("priority")
    private Integer priority;

    /** 版本号 */
    @TableField("version")
    private String version;

    /** 灰度权重 */
    @TableField("weight")
    private Integer weight;

    /** 父配置ID（用于继承） */
    @TableField("parent_id")
    private Long parentId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
