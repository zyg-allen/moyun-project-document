# AI能力统一接入层 — 完整方案文档

**文档版本**：V2.0
**编制日期**：2026-09-08
**状态**：待评审
**关联模块**：AI底座、面试模块、简历模块、出题模块、记账模块、敏感词模块、定时任务模块


## 一、问题背景与目标

### 1.1 当前现状

项目已建成AI底座（知识库、工具、工作流、模型配置、Agent），但各业务模块直接调用AI的方式存在严重问题：

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              当前状态                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  面试交互 ──→ LlmClient.chat(prompt)                                           │
│  简历分析 ──→ LlmClient.chat(prompt + resumeData)                              │
│  自动出题 ──→ LlmClient.chat(prompt + questionData)                            │
│  财务分析 ──→ LlmClient.chat(prompt + ledgerData)                              │
│  敏感词生成 ──→ LlmClient.chat(prompt)                                         │
│  今日主题 ──→ LlmClient.chat(prompt + date)                                   │
│  ... (20+ 个散落的AI调用点)                                                   │
│                                                                                 │
│  问题：                                                                        │
│  ❌ 每个模块各自拼接Prompt、各自调用、各自解析返回结果                         │
│  ❌ 代码重复、没有标准、难以维护                                               │
│  ❌ 新增场景没有指引，代码风格不一致                                           │
│  ❌ 接口爆炸（20+ 个AI接口，每个都不一样）                                    │
│  ❌ 无法统一监控、限流、成本控制                                               │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心目标

| 目标         | 说明                                       |
| ------------ | ------------------------------------------ |
| **统一入口** | 所有AI调用走一个接口                       |
| **场景隔离** | 每个场景独立Handler，互不干扰              |
| **配置驱动** | 新增场景只需配置+实现Handler，不改核心代码 |
| **输出规范** | 统一响应格式，调用方无需关心解析细节       |
| **可观测**   | 全链路追踪、监控、日志                     |
| **成本可控** | 限流、降级、缓存、模型路由                 |


## 二、整体架构

### 2.1 架构分层

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         AI 能力统一接入层（完整架构）                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  业务层（20+ 个调用点）                                                         │
│  ├── 面试交互 ──────┐                                                         │
│  ├── 简历分析 ──────┼───→ 统一入口 ( /api/ai/execute )                       │
│  ├── 自动出题 ──────┤         │                                               │
│  ├── 财务分析 ──────┤         ▼                                               │
│  ├── 敏感词管理 ────┤    AiSceneRegistry（场景注册中心）                      │
│  ├── 今日主题 ──────┤         │                                               │
│  └── ... ───────────┘         ▼                                               │
│                          Handler 路由                                         │
│                    ┌─────────┼─────────┐                                     │
│                    ▼         ▼         ▼                                     │
│              Interview   Resume    Question                                   │
│              Handler     Handler   Handler                                    │
│                    │         │         │                                     │
│                    └─────────┼─────────┘                                     │
│                              ▼                                               │
│                    ┌─────────────────────────────────────────────────────────┐│
│                    │            AI 底座                                     ││
│                    │  Agent / 模型 / 工具 / 知识库 / 工作流                  ││
│                    └─────────────────────────────────────────────────────────┘│
│                                                                                 │
│  收益：                                                                        │
│  ✅ 20+ 接口 → 1 个接口                                                        │
│  ✅ 每个场景独立 Handler，职责单一                                              │
│  ✅ 配置驱动，新增场景只需注册配置 + 实现 Handler                               │
│  ✅ 公共逻辑统一处理（限流/日志/监控/异常）                                     │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 数据流

```
用户请求
    │
    ▼
【第1层】意图判断（前置）
    ├── 规则匹配（快速，零成本）
    ├── 轻量模型分类（准确，低成本）
    ├── 上下文推断（多轮对话）
    └── 兜底追问（不清楚就问）
    │
    ▼
【第2层】缓存检查
    ├── 计算输入文本的Embedding
    ├── 在缓存中检索相似请求（相似度 > 0.95）
    ├── 命中 → 直接返回缓存结果
    └── 未命中 → 继续
    │
    ▼
【第3层】场景路由
    ├── 根据 scene_code 获取配置
    ├── 选择对应的 Handler
    └── 确定输出模式（sync/stream）
    │
    ▼
【第4层】Handler 执行
    ├── 1. 参数提取
    ├── 2. 查询补充数据（数据库/Redis）
    ├── 3. 构建上下文（Context）
    ├── 4. 构建系统提示词（动态生成）
    ├── 5. 调用 AI 底座（Agent/模型/工作流）
    └── 6. 解析返回结果
    │
    ▼
【第5层】响应输出
    ├── 同步模式 → 返回完整 JSON
    └── 流式模式 → SSE 流式返回
```


## 三、核心数据模型

### 3.1 场景注册表（核心配置）

```sql
-- ============================================================
-- AI 场景注册表
-- ============================================================
CREATE TABLE ai_scene_registry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_code VARCHAR(50) NOT NULL UNIQUE COMMENT '场景代码: interview/resume/question_generate/ledger/sensitive/daily_topic',
    scene_name VARCHAR(100) NOT NULL COMMENT '场景名称',
    scene_category VARCHAR(30) NOT NULL COMMENT '分类: chat/analysis/generation/classification',
    description VARCHAR(500) COMMENT '场景描述',
    
    -- ===== 绑定配置（三选一，灵活绑定） =====
    bind_type VARCHAR(20) NOT NULL DEFAULT 'agent' COMMENT '绑定类型: agent/workflow/model/knowledge_only',
    agent_id BIGINT COMMENT 'bind_type=agent 时使用',
    workflow_id BIGINT COMMENT 'bind_type=workflow 时使用',
    model_id BIGINT COMMENT 'bind_type=model 时使用',
    knowledge_base_ids JSON COMMENT '知识库ID列表（任何类型都可关联）',
    tool_ids JSON COMMENT '工具ID列表（任何类型都可关联）',
    
    -- ===== Handler 配置 =====
    handler_bean_name VARCHAR(100) NOT NULL COMMENT '对应的Spring Bean名称',
    handler_method VARCHAR(50) DEFAULT 'execute' COMMENT '执行方法名',
    
    -- ===== Prompt 配置 =====
    system_prompt_template TEXT COMMENT '系统提示词模板（支持占位符 {{variable}}）',
    user_prompt_template TEXT COMMENT '用户提示词模板',
    prompt_placeholders JSON COMMENT '占位符说明 {key: description}',
    
    -- ===== 输出配置 =====
    output_mode VARCHAR(20) DEFAULT 'sync' COMMENT '输出模式: sync/stream/both',
    output_schema JSON COMMENT '输出结构定义',
    output_parser VARCHAR(50) COMMENT '解析器: json/markdown/custom',
    
    -- ===== 策略配置 =====
    max_tokens INT DEFAULT 2048,
    temperature DECIMAL(2,1) DEFAULT 0.7,
    timeout_seconds INT DEFAULT 30,
    retry_count INT DEFAULT 3,
    rate_limit_key VARCHAR(50) COMMENT '限流Key',
    rate_limit_count INT DEFAULT 100 COMMENT '限流次数',
    rate_limit_time INT DEFAULT 60 COMMENT '限流时间窗口(秒)',
    
    -- ===== 降级配置 =====
    fallback_model_id BIGINT COMMENT '备用模型ID',
    fallback_response TEXT COMMENT '兜底回复（AI不可用时返回）',
    enable_cache TINYINT DEFAULT 0 COMMENT '是否启用缓存',
    cache_ttl INT DEFAULT 3600 COMMENT '缓存时间(秒)',
    
    -- ===== 状态 =====
    is_active TINYINT DEFAULT 1,
    is_default TINYINT DEFAULT 0,
    priority INT DEFAULT 0 COMMENT '优先级（数字越大越优先）',
    version VARCHAR(20) DEFAULT 'v1' COMMENT '版本号',
    weight INT DEFAULT 100 COMMENT '灰度权重',
    parent_id BIGINT COMMENT '父配置ID（用于继承）',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_scene_code (scene_code),
    INDEX idx_is_active (is_active)
) COMMENT 'AI场景注册表';
```

### 3.2 调用日志表

```sql
-- ============================================================
-- AI 调用日志表
-- ============================================================
CREATE TABLE ai_execute_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL COMMENT '请求ID',
    scene_code VARCHAR(50) NOT NULL COMMENT '场景代码',
    handler_name VARCHAR(100) COMMENT 'Handler名称',
    bind_type VARCHAR(20) COMMENT '绑定类型',
    model_used VARCHAR(100) COMMENT '使用的模型',
    agent_used VARCHAR(100) COMMENT '使用的Agent',
    token_used INT DEFAULT 0 COMMENT 'Token消耗',
    tool_calls JSON COMMENT '工具调用记录',
    input_summary VARCHAR(500) COMMENT '输入摘要',
    output_summary VARCHAR(500) COMMENT '输出摘要',
    status VARCHAR(20) DEFAULT 'success' COMMENT 'success/fail/timeout',
    error_msg TEXT COMMENT '错误信息',
    elapsed_ms BIGINT COMMENT '耗时(毫秒)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_request_id (request_id),
    INDEX idx_scene_code (scene_code),
    INDEX idx_created_at (created_at)
) COMMENT 'AI执行日志表';
```


## 四、统一数据模型

### 4.1 统一请求

```java
@Data
public class AiExecuteRequest {
    
    // ===== 必填 =====
    @NotBlank
    private String scene;              // 场景代码: interview/resume/question_generate
    
    // ===== 业务输入 =====
    private Map<String, Object> input; // 业务参数
    
    // ===== 可选覆盖 =====
    private String outputMode;         // sync/stream（覆盖场景默认）
    private Map<String, Object> config;// 运行时配置覆盖
    private Boolean async;             // 是否异步执行
    
    // ===== 上下文（自动注入） =====
    private String requestId;          // 自动生成
    private Long userId;               // 当前用户
    private String sessionId;          // 会话ID
}
```

### 4.2 统一响应

```java
@Data
public class AiExecuteResponse<T> {
    
    // ===== 通用字段 =====
    private String requestId;
    private String scene;
    private Integer code;              // 0=成功，非0=失败
    private String msg;
    private Long elapsedMs;
    
    // ===== 业务数据 =====
    private T data;
    
    // ===== 元数据（可选） =====
    private AiMetadata metadata;
}

@Data
public class AiMetadata {
    private String modelUsed;
    private String agentUsed;
    private Integer tokenUsed;
    private String modelProvider;
    private List<ToolCallInfo> toolCalls;
    private Boolean fromCache;         // 是否来自缓存
    private Integer retryCount;        // 重试次数
}
```

### 4.3 各场景 Data 结构

```java
// ============================================================
// 场景1: 面试交互 (scene = "interview")
// ============================================================
@Data
public class InterviewSceneData {
    private String question;
    private String questionId;
    private String questionType;       // 八股/算法/项目
    private Integer round;
    private Integer totalRounds;
    private String nextAction;         // ask/followup/next/end
    private String hint;
    private Long thinkTime;            // 建议思考时间(秒)
}

// ============================================================
// 场景2: 简历分析 (scene = "resume")
// ============================================================
@Data
public class ResumeSceneData {
    private String optimizedText;
    private String originalText;
    private List<String> suggestions;
    private Integer score;
    private List<String> keywords;
}

// ============================================================
// 场景3: 自动出题 (scene = "question_generate")
// ============================================================
@Data
public class QuestionSceneData {
    private List<GeneratedQuestion> questions;
    private Integer totalCount;
    private String difficulty;
}

// ============================================================
// 场景4: 财务分析 (scene = "ledger")
// ============================================================
@Data
public class LedgerSceneData {
    private String summary;
    private List<CategoryExpense> categoryExpenses;
    private List<TrendPoint> trendData;
    private String suggestion;
}

// ============================================================
// 场景5: 敏感词检测 (scene = "sensitive_word")
// ============================================================
@Data
public class SensitiveWordSceneData {
    private Boolean hasSensitive;
    private List<String> words;
    private String riskLevel;          // high/medium/low
    private String suggestion;
}

// ============================================================
// 场景6: 今日主题 (scene = "daily_topic")
// ============================================================
@Data
public class TopicSceneData {
    private String title;
    private String description;
    private String category;
    private String source;             // ai_generated/manual
}
```


## 五、核心接口与实现

### 5.1 Handler 接口

```java
public interface AiSceneHandler {
    
    /**
     * 场景代码（用于注册）
     */
    String getSceneCode();
    
    /**
     * 返回类型（用于泛型解析）
     */
    default Class<?> getResponseType() {
        return Object.class;
    }
    
    /**
     * 支持的输出模式
     */
    default String getSupportedOutputMode() {
        return "sync";  // sync / stream / both
    }
    
    /**
     * 同步执行
     */
    default AiExecuteResponse<?> execute(AiExecuteRequest request) {
        throw new UnsupportedOperationException("同步执行未实现");
    }
    
    /**
     * 流式执行（SSE）
     */
    default void executeStream(AiExecuteRequest request, SseEmitter emitter) {
        throw new UnsupportedOperationException("流式执行未实现");
    }
    
    /**
     * 验证请求
     */
    default void validate(AiExecuteRequest request) {
        if (request.getInput() == null || request.getInput().isEmpty()) {
            throw new ServiceException("输入参数不能为空");
        }
    }
    
    /**
     * 构建系统提示词
     */
    default String buildSystemPrompt(AiExecuteRequest request, AiSceneRegistryConfig config) {
        return null;
    }
    
    /**
     * 构建用户提示词
     */
    default String buildUserPrompt(AiExecuteRequest request, AiSceneRegistryConfig config) {
        return null;
    }
}
```

### 5.2 场景注册中心

```java
@Component
@Slf4j
public class AiSceneRegistry {
    
    private final Map<String, AiSceneHandler> handlerMap = new ConcurrentHashMap<>();
    private final Map<String, AiSceneRegistryConfig> configMap = new ConcurrentHashMap<>();
    
    @Autowired
    private List<AiSceneHandler> handlers;
    
    @Autowired
    private AiSceneRegistryMapper configMapper;
    
    @PostConstruct
    public void init() {
        // 1. 注册所有 Handler
        for (AiSceneHandler handler : handlers) {
            String sceneCode = handler.getSceneCode();
            handlerMap.put(sceneCode, handler);
            log.info("注册AI场景处理器: {} -> {}", sceneCode, handler.getClass().getSimpleName());
        }
        
        // 2. 加载配置
        loadConfigs();
    }
    
    public AiSceneHandler getHandler(String sceneCode) {
        AiSceneHandler handler = handlerMap.get(sceneCode);
        if (handler == null) {
            throw new ServiceException("未知场景: " + sceneCode);
        }
        return handler;
    }
    
    public AiSceneRegistryConfig getConfig(String sceneCode) {
        return configMap.get(sceneCode);
    }
    
    private void loadConfigs() {
        List<AiSceneRegistryConfig> configs = configMapper.selectAll();
        configMap.clear();
        for (AiSceneRegistryConfig config : configs) {
            configMap.put(config.getSceneCode(), config);
            log.info("加载场景配置: {} (bindType={}, outputMode={})", 
                config.getSceneCode(), config.getBindType(), config.getOutputMode());
        }
    }
    
    public void refresh() {
        handlerMap.clear();
        configMap.clear();
        init();
        log.info("场景注册中心刷新完成");
    }
}
```

### 5.3 统一入口 Controller

```java
@RestController
@RequestMapping("/api/ai")
@Slf4j
public class AiGatewayController {
    
    @Autowired
    private AiSceneRegistry registry;
    
    @Autowired
    private IntentClassifier intentClassifier;
    
    @Autowired
    private SemanticCache semanticCache;
    
    @Autowired
    private RateLimiter rateLimiter;
    
    @Autowired
    private AiMetricsCollector metricsCollector;
    
    /**
     * 统一AI执行入口
     */
    @PostMapping("/execute")
    public AiExecuteResponse<?> execute(@RequestBody @Valid AiExecuteRequest request) {
        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);
        
        long startTime = System.currentTimeMillis();
        log.info("[AI网关] 请求: scene={}, requestId={}", request.getScene(), requestId);
        
        try {
            // 1. 意图判断
            IntentResult intent = intentClassifier.classify(
                request.getInput().get("userInput"),
                request.getScene()
            );
            if (intent.getConfidence() < 0.6) {
                return AiExecuteResponse.clarification(intent.getClarificationQuestion())
                    .setRequestId(requestId)
                    .setScene(request.getScene());
            }
            if (intent.getSuggestedScene() != null) {
                request.setScene(intent.getSuggestedScene());
            }
            
            // 2. 缓存检查
            String cacheKey = buildCacheKey(request);
            if (semanticCache.isEnabled(request.getScene())) {
                AiExecuteResponse<?> cached = semanticCache.get(cacheKey);
                if (cached != null) {
                    log.info("[AI网关] 缓存命中: scene={}, requestId={}", request.getScene(), requestId);
                    return cached.setRequestId(requestId).setMetadata(cached.getMetadata());
                }
            }
            
            // 3. 获取配置
            AiSceneRegistryConfig config = registry.getConfig(request.getScene());
            if (config == null) {
                throw new ServiceException("场景配置不存在: " + request.getScene());
            }
            
            // 4. 限流
            rateLimiter.check(config.getRateLimitKey(), config.getRateLimitCount(), config.getRateLimitTime());
            
            // 5. 合并运行时配置
            if (request.getConfig() == null) {
                request.setConfig(new HashMap<>());
            }
            
            // 6. 获取 Handler
            AiSceneHandler handler = registry.getHandler(request.getScene());
            
            // 7. 确定输出模式
            String mode = request.getOutputMode();
            if (mode == null) {
                mode = config.getOutputMode();
            }
            
            // 8. 执行
            AiExecuteResponse<?> response;
            if ("stream".equals(mode)) {
                // 流式执行走另一个接口，这里抛异常引导
                throw new ServiceException("流式请求请使用 /execute/stream 接口");
            } else {
                response = handler.execute(request);
            }
            
            // 9. 填充通用字段
            long elapsed = System.currentTimeMillis() - startTime;
            response.setRequestId(requestId)
                    .setScene(request.getScene())
                    .setElapsedMs(elapsed);
            
            // 10. 写入缓存
            if (semanticCache.isEnabled(request.getScene()) && response.getCode() == 0) {
                semanticCache.put(cacheKey, response);
            }
            
            // 11. 记录指标
            metricsCollector.record(request.getScene(), "success", elapsed);
            
            log.info("[AI网关] 成功: scene={}, requestId={}, elapsed={}ms", 
                request.getScene(), requestId, elapsed);
            
            return response;
            
        } catch (RateLimitException e) {
            log.warn("[AI网关] 限流: scene={}, requestId={}", request.getScene(), requestId);
            return AiExecuteResponse.failure(1002, "请求过于频繁，请稍后再试")
                .setRequestId(requestId)
                .setScene(request.getScene());
                
        } catch (Exception e) {
            log.error("[AI网关] 失败: scene={}, requestId={}", request.getScene(), requestId, e);
            metricsCollector.record(request.getScene(), "fail", System.currentTimeMillis() - startTime);
            return AiExecuteResponse.failure(1000, e.getMessage())
                .setRequestId(requestId)
                .setScene(request.getScene());
        }
    }
    
    /**
     * 流式执行（SSE）
     */
    @PostMapping("/execute/stream")
    public SseEmitter executeStream(@RequestBody @Valid AiExecuteRequest request) {
        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);
        
        SseEmitter emitter = new SseEmitter(60000L);
        
        try {
            // 1. 获取 Handler
            AiSceneHandler handler = registry.getHandler(request.getScene());
            
            // 2. 验证是否支持流式
            if (!"stream".equals(handler.getSupportedOutputMode()) && !"both".equals(handler.getSupportedOutputMode())) {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("error", "该场景不支持流式输出"))
                );
                emitter.complete();
                return emitter;
            }
            
            // 3. 执行流式
            handler.executeStream(request, emitter);
            
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("error", e.getMessage()))
                );
                emitter.completeWithError(e);
            } catch (IOException ex) {
                // ignore
            }
        }
        
        return emitter;
    }
}
```


## 六、Handler 实现示例

### 6.1 面试场景 Handler（流式）

```java
@Component
@Slf4j
public class InterviewHandler implements AiSceneHandler {
    
    @Autowired
    private ResumeContextService resumeService;
    
    @Autowired
    private PositionService positionService;
    
    @Autowired
    private StreamingChatLanguageModel streamingChatModel;
    
    @Override
    public String getSceneCode() {
        return "interview";
    }
    
    @Override
    public String getSupportedOutputMode() {
        return "stream";
    }
    
    @Override
    public void executeStream(AiExecuteRequest request, SseEmitter emitter) {
        try {
            // 1. 参数提取
            Long resumeId = (Long) request.getInput().get("resumeId");
            Long positionId = (Long) request.getInput().get("positionId");
            String question = (String) request.getInput().get("question");
            String answer = (String) request.getInput().get("answer");
            Integer round = (Integer) request.getInput().getOrDefault("round", 1);
            
            // 2. 构建上下文
            ResumeContext resume = resumeService.getContext(resumeId);
            PositionJob position = positionService.getById(positionId);
            
            // 3. 构建系统提示词
            String systemPrompt = buildInterviewSystemPrompt(position, resume);
            
            // 4. 发送开始事件
            emitter.send(SseEmitter.event()
                .name("start")
                .data(Map.of("round", round, "totalRounds", 5))
            );
            
            // 5. 调用流式 AI
            streamingChatModel.generate(
                systemPrompt + "\n用户回答：" + answer,
                new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(Map.of("content", token))
                            );
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    
                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("done")
                                .data(Map.of("status", "completed"))
                            );
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data(Map.of("error", error.getMessage()))
                            );
                            emitter.completeWithError(error);
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                }
            );
            
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("error", e.getMessage()))
                );
                emitter.completeWithError(e);
            } catch (IOException ex) {
                // ignore
            }
        }
    }
    
    private String buildInterviewSystemPrompt(PositionJob position, ResumeContext resume) {
        return """
            你是资深技术面试官。
            
            【岗位信息】
            岗位：%s
            技能要求：%s
            
            【候选人信息】
            姓名：%s
            技能：%s
            项目：%s
            """.formatted(
                position.getName(),
                position.getSkills(),
                resume.getName(),
                resume.getSkills(),
                resume.getProjectSummary()
            );
    }
}
```

### 6.2 同步场景 Handler（以敏感词检测为例）

```java
@Component
@Slf4j
public class SensitiveWordHandler implements AiSceneHandler {
    
    @Override
    public String getSceneCode() {
        return "sensitive_word";
    }
    
    @Override
    public String getSupportedOutputMode() {
        return "sync";
    }
    
    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request) {
        String text = (String) request.getInput().get("text");
        
        // 调用同步 AI
        ChatLanguageModel model = getSyncModel();
        ChatResponse response = model.generate(
            ChatMessage.userMessage("检测以下文本是否包含敏感词：" + text)
        );
        
        SensitiveWordSceneData data = parseResult(response.content().text());
        
        return AiExecuteResponse.success(data)
            .setScene("sensitive_word");
    }
}
```


## 七、增强能力模块

### 7.1 意图判断

```java
@Component
public class IntentClassifier {
    
    private static final Map<String, String> RULES = new LinkedHashMap<>();
    
    static {
        RULES.put(".*(退款|退货|取消).*", "REFUND");
        RULES.put(".*(怎么|如何|步骤|教程).*", "HOW_TO");
        RULES.put(".*(你好|早上好|下午好|晚上好).*", "GREETING");
        RULES.put(".*(继续|下一题|下一个).*", "NEXT");
        RULES.put(".*(结束|完成|好了).*", "FINISH");
    }
    
    public IntentResult classify(String userInput, String currentScene) {
        // 1. 规则匹配
        for (Map.Entry<String, String> entry : RULES.entrySet()) {
            if (userInput.matches(entry.getKey())) {
                return new IntentResult(entry.getValue(), 1.0);
            }
        }
        
        // 2. 场景特定意图
        if ("interview".equals(currentScene)) {
            if (userInput.contains("不知道") || userInput.contains("不清楚") || userInput.length() < 10) {
                return new IntentResult("CONFUSED", 0.85);
            }
            if (userInput.length() > 100) {
                return new IntentResult("COMPLETE_ANSWER", 0.80);
            }
        }
        
        // 3. 默认
        return new IntentResult("UNKNOWN", 0.3);
    }
}
```

### 7.2 语义缓存

```java
@Component
public class SemanticCache {
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final double SIMILARITY_THRESHOLD = 0.95;
    
    public boolean isEnabled(String scene) {
        // 从配置读取
        return true;
    }
    
    public AiExecuteResponse<?> get(String key) {
        // 从缓存检索
        return null;
    }
    
    public void put(String key, AiExecuteResponse<?> response) {
        // 写入缓存
    }
}
```

### 7.3 降级策略

```java
@Component
public class FallbackStrategy {
    
    public AiExecuteResponse<?> executeFallback(String scene, Exception e) {
        // 根据不同场景返回不同兜底
        switch (scene) {
            case "interview":
                return AiExecuteResponse.success(
                    new InterviewSceneData()
                        .setQuestion("AI服务暂时不可用，请稍后重试")
                        .setNextAction("end")
                );
            case "sensitive_word":
                return AiExecuteResponse.success(
                    new SensitiveWordSceneData()
                        .setHasSensitive(false)
                        .setSuggestion("AI服务暂时不可用，已跳过检测")
                );
            default:
                return AiExecuteResponse.failure(2000, "AI服务暂时不可用");
        }
    }
}
```


## 八、错误码定义

```java
public interface AiErrorCodes {
    int SUCCESS = 0;
    
    // 通用错误 (1000-1099)
    int UNKNOWN_ERROR = 1000;
    int TIMEOUT = 1001;
    int RATE_LIMITED = 1002;
    int INVALID_REQUEST = 1003;
    int SCENE_NOT_FOUND = 1004;
    int HANDLER_ERROR = 1005;
    int OUTPUT_MODE_NOT_SUPPORTED = 1006;
    
    // AI相关错误 (2000-2099)
    int AI_CALL_FAILED = 2000;
    int AI_PARSE_ERROR = 2001;
    int AI_EMPTY_RESULT = 2002;
    int AI_SAFETY_BLOCKED = 2003;
    int AI_TOKEN_LIMIT_EXCEEDED = 2004;
    int AI_MODEL_UNAVAILABLE = 2005;
    
    // 业务相关错误 (3000-3099)
    int RESUME_NOT_FOUND = 3000;
    int POSITION_NOT_FOUND = 3001;
    int QUESTION_NOT_FOUND = 3002;
}
```


## 九、实施路线图

| 阶段        | 任务                                    | 工时 | 产出              |
| ----------- | --------------------------------------- | ---- | ----------------- |
| **Phase 1** | 数据表 + 核心类（注册中心/Handler接口） | 2天  | DDL + 核心代码    |
| **Phase 2** | 统一入口 Controller + 基础Handler       | 2天  | GatewayController |
| **Phase 3** | 迁移3个核心场景（面试/简历/出题）       | 3天  | 3个Handler        |
| **Phase 4** | 意图判断 + 语义缓存                     | 2天  | 增强能力          |
| **Phase 5** | 可观测性（日志/监控/链路追踪）          | 2天  | 指标采集          |
| **Phase 6** | 灰度发布 + 降级策略                     | 2天  | 高可用能力        |
| **Phase 7** | 迁移剩余场景 + 关闭旧接口               | 2天  | 全量切换          |

**总工期：约 15 天（3 周）**


## 十、收益评估

| 维度             | 改造前   | 改造后      | 提升       |
| ---------------- | -------- | ----------- | ---------- |
| AI调用接口数     | 20+ 个   | 1 个        | ✅ 95% 减少 |
| 新增场景开发成本 | 2-3天    | 0.5-1天     | ✅ 50% 减少 |
| 代码重复率       | 高       | 低          | ✅ 显著降低 |
| 问题排查时间     | 分散查找 | 统一追踪    | ✅ 70% 减少 |
| Token成本控制    | 不可控   | 可监控+限流 | ✅ 30% 节省 |
| 可观测性         | 无       | 全链路      | ✅ 从0到1   |


## 十一、总结

> **用 1 个统一入口替代 20+ 个散落的AI调用点，每个场景独立Handler，配置驱动、职责单一、易于扩展。配合意图判断、语义缓存、模型路由、可观测性、灰度降级等增强能力，形成完整的AI能力网关。**