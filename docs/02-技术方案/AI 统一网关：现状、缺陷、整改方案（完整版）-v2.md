#  AI 统一网关：现状、缺陷、整改方案（完整版）

> 版本：v2.0
> 日期：2026-09-24
> 作者：钟永国
> 用途：Trae 可直接读取执行
> 原则：简化、统一、性能、可维护

---

## 第一部分：现状分析

### 1.1 当前架构

```
业务层（ext.cms / portal / ledger）
  ↓ AiSceneJsonClient / AiGatewayService
网关层（aiapp）
  Controller → AiGatewayService 五层编排 → 11 个 Handler
  横切：注入防护 / 语义缓存 / 限流 / 熔断 / 输出过滤 / 降级
底座层（ext.ai）
  模型工厂 / 场景解析 / LLM服务 / Agent / RAG / 工作流
  底层：LangChain4j
```

### 1.2 当前入口

| 入口      | 路径                            | 用途   | 治理    |
| ------- | ----------------------------- | ---- | ----- |
| HTTP 同步 | `POST /api/ai/execute`        | 外部调用 | ✅     |
| HTTP 流式 | `POST /api/ai/execute/stream` | SSE  | ⚠️ 部分 |
| Service | `AiGatewayService.execute()`  | 业务内部 | ✅     |
| 直连（例外）  | `InterviewAgentClientImpl`    | 面试主干 | ❌     |
| 管理端直连   | `/cms/ai/chat/stream`         | 测试功能 | ❌     |

### 1.3 已实现能力

| 能力                      | 状态                   |
| ----------------------- | -------------------- |
| 场景化配置（ai_scene_config）  | ✅ 直查库，即时生效           |
| 多模型/多提供商（ai_provider）   | ✅ 配置驱动               |
| Agent/Workflow/Model 绑定 | ✅                    |
| 安全三件套                   | ✅ 注入防护 + 成本熔断 + 输出脱敏 |
| 可观测性（ai_execute_log）    | ✅                    |
| 降级兜底                    | ✅ 三层                 |
| 限流                      | ✅ Redis 滑动窗口         |
| 语义缓存                    | ✅ MD5 + Embedding    |
| 异步任务                    | ✅ 表驱动 + Redis        |

---

## 第二部分：缺陷清单

### 2.1 架构缺陷

| #   | 缺陷                  | 根因                                                    | 影响                 |
| --- | ------------------- | ----------------------------------------------------- | ------------------ |
| D1  | 半收口                 | 网关不支持会话型流式                                            | 面试主干、管理端 chat 在网关外 |
| D2  | 11 个 Handler 重复     | 按业务场景分                                                | 维护成本线性增长           |
| D3  | Envelope/Payload 混层 | AiExecuteRequest 只有一个 input Map                       | 治理组件无法可靠提取         |
| D4  | 配置四源                | ai_scene_config / ai_model_config / sys_config / yaml | 排障查四处              |
| D5  | 双命名                 | ai2 / aiapp                                           | 认知成本               |
| D6  | SSE 治理未覆盖           | 治理为同步设计                                               | Token/脱敏缺口         |
| D7  | 多模态未抽象              | Payload 是 Map                                         | 声音/文件处理散落          |
| D8  | 管理端测试边界模糊           | 未明确两套入口                                               | 收口可能破坏测试           |
| D9  | Token 成本无系统控制       | 各场景自己缩减                                               | 多轮对话成本指数增长         |
| D10 | 特例粘合成本高             | 面试主干直连背 5 概念                                          | 灰度+前置+共享参数         |
| D11 | 配置直查库               | 无本地缓存                                                 | 每次调用 1 次 DB        |
| D12 | 会话记忆同步读             | ContextManager                                        | 每轮 1 次 Redis       |
| D13 | 摘要同步生成              | ContextManager                                        | 阻塞当前轮              |
| D14 | 多模态串行               | ModalityPreprocessor                                  | 首字节延迟              |
| D15 | 治理链多次 Redis         | 限流/成本/缓存各一次                                           | 3-4 次往返            |
| D16 | 无压测基线               | —                                                     | 不知道瓶颈              |
| D17 | 无配置版本管理             | —                                                     | 改错无法回滚             |
| D18 | 无场景调试工具             | —                                                     | 问题定位链路长            |

### 2.2 设计歧义

| #   | 歧义                   | 结论                             |
| --- | -------------------- | ------------------------------ |
| A1  | Handler 和 Service 边界 | Handler 职责通用，可删；业务上下文归 Service |
| A2  | 场景定义边界               | 场景按业务语义，技术步骤（向量化/召回）不做场景       |
| A3  | 会话是否独立成场景            | 不独立，做成场景的配置维度                  |
| A4  | 公共能力边界               | 治理上浮网关，业务上下文下沉 Service         |

---

## 第三部分：核心设计原则

### 3.1 Agent vs Workflow

| 维度  | Agent       | Workflow    |
| --- | ----------- | ----------- |
| 主动权 | LLM 自主      | 人工编排        |
| 特点  | 多轮调用，成本不可预测 | 固定 I/O，成本可控 |
| 适用  | 开放对话        | 固定格式        |

**场景表绑定 agent/workflow/model/rag = 让主动权成为配置项。**

### 3.2 三层参数

| 层        | 名称  | 谁封装           | 进 Prompt | 作用               |
| -------- | --- | ------------- | -------- | ---------------- |
| Envelope | 信封  | 网关入口          | ❌        | 治理、路由、追踪         |
| Payload  | 载荷  | 业务 Service    | ✅        | LLM 输入数据         |
| Metadata | 元数据 | SceneExecutor | ⚠️ 部分    | Prompt 模板 + 控制参数 |

### 3.3 两套入口，一个资源层

```
业务层 → 网关（场景化调用，有治理）
管理端层 → 资源（资源级调用，无治理）
资源层 → Agent/Workflow/Model（原生接口，两套都调）
Adapter 只在网关侧，不侵入资源
```

### 3.4 场景 vs 能力

| 概念  | 定义        | 配置化 | 举例                           |
| --- | --------- | --- | ---------------------------- |
| 场景  | 业务语义，对外暴露 | ✅   | resume_parse、voice_interview |
| 能力  | 技术步骤，内部调用 | ❌   | 向量化、召回、rerank                |
| 会话  | 交互模式      | ✅   | 单次/多轮/长对话                    |

### 3.5 目标架构

```
┌─────────────────────────────────────────┐
│ 业务层                                   │
│ Service → 网关（组装 Payload）           │
├─────────────────────────────────────────┤
│ 管理端层                                 │
│ Controller → Agent/Workflow/ModelService │
├─────────────────────────────────────────┤
│ 网关层（aiapp）                          │
│ SceneExecutor（1 个，通用）              │
│ 通用组件：ContextManager / RagRetriever  │
│          ModalityPreprocessor / Adapter  │
├─────────────────────────────────────────┤
│ 资源层（ext.ai）                         │
│ Agent / Workflow / Model / RAG           │
├─────────────────────────────────────────┤
│ 配置层 ai_scene_config                   │
│ 声明场景一切差异                          │
└─────────────────────────────────────────┘
```

---

## 第四部分：整改方案

### 阶段一：补会话型流式（P0，2-3 周）

**目标**：网关支持多轮 + 记忆 + per-agent 流式。

**改动**：

| #   | 动作                                       | 文件            |
| --- | ---------------------------------------- | ------------- |
| 1.1 | AiExecuteRequest 加 sessionId/sessionMode | aiapp/model   |
| 1.2 | 新增 ChatMemoryProvider 接口                 | aiapp/support |
| 1.3 | 实现 RedisChatMemoryProvider               | aiapp/support |
| 1.4 | 新增 ContextManager                        | aiapp/support |
| 1.5 | SceneExecutor 支持会话流式                     | aiapp/service |
| 1.6 | 面试主干迁移到网关                                | ext/cms       |
| 1.7 | 删灰度开关、治理前置                               | aiapp         |

**验收**：面试主干走网关，延迟不增加；多轮记忆正常；流式输出正常。

---

### 阶段二：删 Handler，抽 SceneExecutor（P0，2-3 周）

**目标**：11 个 Handler → 0 个，通用执行器覆盖所有场景。

**改动**：

| #   | 动作                      |
| --- | ----------------------- |
| 2.1 | 定义 SceneExecutor 接口     |
| 2.2 | 实现 DefaultSceneExecutor |
| 2.3 | Handler 通用逻辑上移          |
| 2.4 | 业务上下文下沉 Service         |
| 2.5 | 场景差异进 ai_scene_config   |
| 2.6 | 迁移 3 个简单场景验证            |
| 2.7 | 迁移剩余场景                  |
| 2.8 | 删除 11 个 Handler         |

**验收**：所有场景功能正常；新增场景只加配置 + Service 方法；代码净减少。

---

### 阶段三：加 Adapter，保留独立调用（P1，1-2 周）

**目标**：三层模型翻译成资源原生入参，管理端测试零改动。

**改动**：

| #   | 动作                       |
| --- | ------------------------ |
| 3.1 | 定义 AgentAdapter          |
| 3.2 | 定义 WorkflowAdapter       |
| 3.3 | 定义 ModelAdapter          |
| 3.4 | SceneExecutor 集成 Adapter |
| 3.5 | 管理端测试回归                  |

**验收**：管理端测试零改动；资源层零改动；网关调用正常。

---

### 阶段四：性能优化（P1，2-3 周）

**目标**：解决 D11-D16。

**改动**：

| #   | 动作                       | 解决  |
| --- | ------------------------ | --- |
| 4.1 | Caffeine 配置（CacheConfig） | D11 |
| 4.2 | 配置本地缓存 + 失效通知            | D11 |
| 4.3 | 会话记忆二级缓存 + 批量读           | D12 |
| 4.4 | 摘要异步预生成                  | D13 |
| 4.5 | 多模态并行化                   | D14 |
| 4.6 | 治理链 Pipeline 合并          | D15 |
| 4.7 | JMeter 压测                | D16 |

**验收**：

- 同场景 100 次调用，DB ≤ 1 次
- 同会话 10 轮，Redis ≤ 1 次
- Redis 往返 ≤ 2 次/请求
- QPS ≥ 50，P99 ≤ 3s，错误率 ≤ 0.1%

---

### 阶段五：Token 成本控制（P1，2-3 周）

**目标**：按场景特点系统化缩减上下文。

**改动**：

| #   | 动作                              |
| --- | ------------------------------- |
| 5.1 | ai_scene_config 加成本控制字段         |
| 5.2 | ContextManager 实现滑窗 + 摘要 + 实体记忆 |
| 5.3 | 各场景实现 preprocessPayload         |
| 5.4 | 输出侧加 max_tokens                 |
| 5.5 | 语义缓存增强                          |
| 5.6 | 成本监控看板                          |

**成本控制字段**：

| 字段                | 说明                         | 默认   |
| ----------------- | -------------------------- | ---- |
| context_strategy  | full/window/summary/hybrid | full |
| context_window    | 滑窗轮数                       | 10   |
| summary_threshold | 摘要触发阈值                     | 5    |
| max_input_tokens  | 输入上限                       | 8000 |
| max_output_tokens | 输出上限                       | 2000 |
| truncate_strategy | head/tail/middle           | tail |

**验收**：语音面试 token 减 75%；财务分析减 80%；简历解析减 70%。

---

### 阶段六：多模态抽象（P2，2-3 周）

**目标**：ContentPart 统一，网关预处理。

**改动**：

| #   | 动作                                     |
| --- | -------------------------------------- |
| 6.1 | 定义 ContentPart 接口及子类                   |
| 6.2 | Payload 值类型升级                          |
| 6.3 | 实现 ModalityPreprocessor                |
| 6.4 | ai_model_config 加 input_modalities     |
| 6.5 | ai_scene_config 加 supported_modalities |
| 6.6 | 语音面试迁移 AudioPart                       |
| 6.7 | 简历解析迁移 FilePart                        |
| 6.8 | 证件识别迁移 ImagePart                       |

**ContentPart 定义**：

```java
interface ContentPart {}
class TextPart implements ContentPart { String text; }
class ImagePart implements ContentPart { String url; String mimeType; String detail; }
class AudioPart implements ContentPart { String url; String mimeType; String transcript; }
class FilePart implements ContentPart { String url; String mimeType; String extractedText; }
class VideoPart implements ContentPart { String url; List<ImagePart> frames; String transcript; }
```

**验收**：业务只传原始 Part；转换逻辑在网关；新增模态只加 Preprocessor。

---

### 阶段七：清理与优化（P2，1 周）

**改动**：

| #   | 动作                | 解决  |
| --- | ----------------- | --- |
| 7.1 | ai2 → aiapp 重命名   | D5  |
| 7.2 | 删 AiProperties 残留 | D4  |
| 7.3 | 配置四源收敛            | D4  |
| 7.4 | 配置版本管理 + 回滚       | D17 |
| 7.5 | 场景调试工具            | D18 |
| 7.6 | 错误码统一             | —   |
| 7.7 | Prompt 变量校验       | —   |

---

## 第五部分：核心代码定义

### 5.1 三层参数

```java
// Envelope（网关封装）
class AiEnvelope {
    String sceneCode;
    Long userId;
    String platformCode;
    String sessionId;
    SessionMode sessionMode;
    String traceId;
    String requestId;
    boolean stream;
}

// Payload（业务封装）
class AiPayload {
    Map<String, Object> data;  // 值可为 String / ContentPart / List<ContentPart>
}

// Metadata（配置读取）
class AiMetadata {
    // Prompt
    String systemPromptTemplate;
    String userPromptTemplate;
    // 输出
    String outputSchema;
    String outputParser;
    // 模型
    Long modelConfigId;
    Long agentId;
    Long workflowId;
    Long knowledgeBaseId;
    // 会话
    SessionMode sessionMode;
    Integer memoryWindow;
    // 上下文
    String contextStrategy;
    Integer contextWindow;
    Integer summaryThreshold;
    Integer maxInputTokens;
    Integer maxOutputTokens;
    // 治理
    Integer rateLimitCount;
    Integer dailyTokenLimit;
    Boolean enableCache;
    Boolean enableOutputFilter;
    String fallbackResponse;
    // 多模态
    List<String> supportedModalities;
    String modalityPreprocess;
}
```

### 5.2 SceneExecutor

```java
class SceneExecutor {
    AiExecuteResponse<?> execute(AiExecuteRequest req) {
        // 1. 封装 Envelope
        AiEnvelope env = buildEnvelope(req);
        // 2. 读 Metadata（缓存）
        AiMetadata meta = sceneConfigCache.get(req.sceneCode);
        // 3. 取 Payload
        AiPayload payload = req.getPayload();
        // 4. 治理（Pipeline 合并）
        governancePipeline.check(env, meta);
        // 5. 构建上下文
        List<Message> messages = contextManager.build(env, payload, meta);
        // 6. 预处理多模态
        List<ContentPart> parts = modalityPreprocessor.preprocess(payload, meta);
        // 7. 渲染 Prompt
        String prompt = renderTemplate(meta, payload, messages, parts);
        // 8. Adapter 适配
        Object input = adapter.adapt(env, payload, meta);
        // 9. 执行
        Object result = executeByType(input, meta);
        // 10. 解析输出
        Object parsed = parseOutput(result, meta);
        // 11. 输出过滤
        Object filtered = outputFilter.apply(parsed, meta);
        // 12. 日志（异步）
        logService.record(env, meta, result);
        return response(filtered);
    }
}
```

### 5.3 通用组件清单

| 组件                   | 职责                 | 阶段  |
| -------------------- | ------------------ | --- |
| SceneExecutor        | 配置驱动执行             | 二   |
| ContextManager       | 多轮上下文管理            | 一   |
| ModalityPreprocessor | 多模态预处理             | 六   |
| RagRetriever         | RAG 召回 + 重排        | —   |
| AgentAdapter         | 三层 → AgentInput    | 三   |
| WorkflowAdapter      | 三层 → WorkflowInput | 三   |
| ModelAdapter         | 三层 → ModelInput    | 三   |
| SceneConfigCache     | 配置缓存               | 四   |
| SessionMemoryCache   | 会话缓存               | 四   |
| SummaryPreGenerator  | 摘要预生成              | 四   |
| GovernancePipeline   | 治理 Pipeline        | 四   |

---

## 第六部分：理由说明

### 6.1 为什么参数分三层

- Envelope 由网关统一构造，业务不传 → 治理组件可靠
- Payload 由业务传，网关不解析 → 职责清晰
- Metadata 从配置读，业务不关心 → 配置驱动

### 6.2 为什么删 Handler

- Handler 职责全是通用逻辑 → 网关一份够
- 业务上下文归 Service → 边界清晰
- 场景差异进配置 → 新增场景零代码

### 6.3 为什么保留独立调用

- 管理端测试是已完备资产 → 不应破坏
- 资源层保持原生接口 → 两套入口都调
- Adapter 只在网关侧 → 不侵入

### 6.4 为什么补会话型流式

- 半收口比全收口/全不收口更乱
- 补上后净删代码
- 是 D1/D2/D10 的共同解锁钥匙

### 6.5 为什么做多模态抽象

- 声音/文件已在用，未抽象
- 转换逻辑散落各处
- 新增模态改所有场景

### 6.6 为什么系统化 Token 控制

- 多轮对话成本指数增长
- 各场景自己缩减，无统一策略
- 成本失控风险

### 6.7 为什么做性能优化

- 配置直查库、会话同步读、摘要阻塞、多模态串行
- 无压测基线，不知道瓶颈
- 上生产必须解决

---

## 第七部分：三维度评估

| 维度  | 当前   | 整改后  | 说明                 |
| --- | ---- | ---- | ------------------ |
| 简化  | 8/10 | 9/10 | Handler 删掉，配置驱动    |
| 性能  | 5/10 | 8/10 | 缓存 + 异步 + Pipeline |
| 维护  | 7/10 | 9/10 | 版本管理 + 调试工具        |
| 统一  | 6/10 | 9/10 | 全收口，两套入口清晰         |

**三维度张力**：简化 ↔ 性能 ↔ 维护，需显式取舍。

**当前取舍**：功能优先，性能次之。**整改后**：三者平衡。

---

## 第八部分：改动清单（Trae 执行）

| #   | 文件                                                  | 类型  | 阶段  |
| --- | --------------------------------------------------- | --- | --- |
| 1   | aiapp/model/AiEnvelope.java                         | 新增  | 一   |
| 2   | aiapp/model/AiPayload.java                          | 新增  | 一   |
| 3   | aiapp/model/AiMetadata.java                         | 新增  | 一   |
| 4   | aiapp/support/ChatMemoryProvider.java               | 新增  | 一   |
| 5   | aiapp/support/RedisChatMemoryProvider.java          | 新增  | 一   |
| 6   | aiapp/support/ContextManager.java                   | 新增  | 一   |
| 7   | aiapp/service/SceneExecutor.java                    | 新增  | 二   |
| 8   | aiapp/service/impl/DefaultSceneExecutor.java        | 新增  | 二   |
| 9   | aiapp/adapter/AgentAdapter.java                     | 新增  | 三   |
| 10  | aiapp/adapter/WorkflowAdapter.java                  | 新增  | 三   |
| 11  | aiapp/adapter/ModelAdapter.java                     | 新增  | 三   |
| 12  | aiapp/config/CacheConfig.java                       | 新增  | 四   |
| 13  | aiapp/cache/SceneConfigCache.java                   | 新增  | 四   |
| 14  | aiapp/support/SessionMemoryCache.java               | 新增  | 四   |
| 15  | aiapp/support/SummaryPreGenerator.java              | 新增  | 四   |
| 16  | aiapp/support/GovernancePipeline.java               | 新增  | 四   |
| 17  | aiapp/support/ModalityPreprocessor.java             | 新增  | 六   |
| 18  | aiapp/model/ContentPart.java                        | 新增  | 六   |
| 19  | aiapp/registry/AiSceneRegistry.java                 | 修改  | 四   |
| 20  | aiapp/support/ModalityPreprocessor.java             | 修改  | 四   |
| 21  | ext/ai/service/impl/AiSceneConfigServiceImpl.java   | 修改  | 四   |
| 22  | ext/cms/service/impl/VoiceInterviewServiceImpl.java | 修改  | 一   |
| 23  | 11 个 Handler 类                                      | 删除  | 二   |
| 24  | InterviewAgentClientImpl 直连分支                       | 删除  | 一   |
| 25  | ai2 → aiapp                                         | 重命名 | 七   |

---

## 第九部分：验收标准

| 项          | 目标                        |
| ---------- | ------------------------- |
| Handler 数量 | 0                         |
| 新增场景       | 3 步（枚举 + 配置 + Service 方法） |
| 配置查询       | 同场景 100 次，DB ≤ 1 次        |
| 会话读取       | 同会话 10 轮，Redis ≤ 1 次      |
| Redis 往返   | ≤ 2 次/请求                  |
| QPS        | ≥ 50                      |
| P99        | ≤ 3s                      |
| 错误率        | ≤ 0.1%                    |
| 语音面试 token | 减 75%                     |
| 财务分析 token | 减 80%                     |
| 简历解析 token | 减 70%                     |
| 管理端测试      | 零改动                       |
| 资源层        | 零改动                       |

---

## 第十部分：执行顺序

```
阶段一（会话流式）→ 阶段二（删 Handler）→ 阶段三（Adapter）
  → 阶段四（性能）→ 阶段五（成本）→ 阶段六（多模态）→ 阶段七（清理）

每阶段独立验证，不做大爆炸。
阶段一 + 二 是核心，做完停下来评估。
```

---

## 第十一部分：一页总结

```
现状：11 Handler + 半收口 + 参数混层 + 多模态未抽象 + 无性能优化
目标：0 Handler + 全收口 + 三层参数 + ContentPart + 缓存/异步

核心决策：
1. 参数分三层（Envelope/Payload/Metadata）
2. 删 Handler，SceneExecutor + 配置驱动
3. 保留管理端独立调用（两套入口，一个资源层）
4. Adapter 只在网关侧
5. 补会话型流式
6. 多模态抽象 ContentPart
7. Token 成本系统控制
8. 性能：缓存 + 异步 + Pipeline

落地：7 阶段，阶段一+二是核心
原则：简化、统一、兼容、性能、配置驱动
```

---

**本文档为 Trae 执行基线，按第八节改动清单、第十节执行顺序执行，按第九节验收。**
