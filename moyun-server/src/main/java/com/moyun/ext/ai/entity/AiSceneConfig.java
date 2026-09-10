package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AI 场景配置实体类
 *
 * <p>对应数据库表 ai_scene_config，业务场景与 Agent/模型/知识库/工具/工作流的动态绑定配置。</p>
 *
 * <p>继承 {@link AiBaseEntity}，复用 createTime / updateTime / deleted 字段。</p>
 *
 * <p>解析顺序（AiSceneResolver）：绑定 Agent（agent_id 非空且启用）> 直接绑定模型
 * （model_config_id 兜底，合成伪 Agent）> 返回 empty() 走业务原有默认逻辑。</p>
 *
 * @author moyun
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_scene_config")
public class AiSceneConfig extends AiBaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 场景代码（须为 AiSceneEnum 注册值：voice_interview/resume_optimize/question_generate/finance_analysis） */
    private String sceneCode;

    /** 场景名称 */
    private String sceneName;

    /** 场景描述 */
    private String description;

    /** 绑定的智能体（ai_agent.id，启用状态才生效） */
    private Long agentId;

    /** 直接绑定模型（ai_model_config.id，agent_id 为空时生效） */
    private Long modelConfigId;

    /** 知识库ID列表 JSON数组，如 [1,2,3] */
    private String knowledgeLibraryIds;

    /** 工具ID列表 JSON数组 */
    private String toolIds;

    /** 绑定工作流（ai_workflow.id） */
    private Long workflowId;

    /** 场景策略配置 JSON（如 {"dynamicMode":true}） */
    private String configJson;

    // ==================== v11.41 执行层配置（统一接入层） ====================

    /** 场景分类: chat/analysis/generation/classification */
    private String sceneCategory;

    /** 对应 Spring Bean 名称（统一网关路由） */
    private String handlerBeanName;

    /** 执行方法名（默认 execute） */
    private String handlerMethod;

    /** 系统提示词模板（支持占位符 {{variable}}） */
    private String systemPromptTemplate;

    /** 用户提示词模板 */
    private String userPromptTemplate;

    /** 占位符说明 JSON */
    private String promptPlaceholders;

    /** 输出模式: sync/stream/both */
    private String outputMode;

    /** 输出结构定义 JSON */
    private String outputSchema;

    /** 解析器: json/markdown/custom */
    private String outputParser;

    /** 最大 Token 数 */
    private Integer maxTokens;

    /** 温度参数 */
    private BigDecimal temperature;

    /** 超时秒数 */
    private Integer timeoutSeconds;

    /** 重试次数 */
    private Integer retryCount;

    /** 限流 Key */
    private String rateLimitKey;

    /** 限流次数 */
    private Integer rateLimitCount;

    /** 限流时间窗口（秒） */
    private Integer rateLimitTime;

    /** 备用模型 ID */
    private Long fallbackModelId;

    /** 兜底回复（AI 不可用时返回） */
    private String fallbackResponse;

    /** 是否启用缓存 */
    private Boolean enableCache;

    /** 缓存时间（秒） */
    private Integer cacheTtl;

    /** 版本号（同场景多版本灰度） */
    private String version;

    /** 灰度权重（同场景多版本按权重轮盘赌） */
    private Integer weight;

    /** 优先级（全 0 权重时取 is_default，再按 priority DESC） */
    private Integer priority;

    /** 是否默认版本 */
    private Boolean isDefault;

    /** 是否启用 */
    private Boolean enabled;

    /** 是否开放通用入口调用（v11.51：open_api=1 才能经 /api/ai/execute 外部调用；业务内部网关调用不受限） */
    private Boolean openApi;
}