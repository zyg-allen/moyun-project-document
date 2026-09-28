# AI 统一网关 · 演进史与现行实现

> **文档性质**：**演进史**（不是规范、不是待执行方案）。记录 AI 网关从"各业务直连 LLM"到
> "`ai_scene_config` 配置驱动"的全过程、期间被**推翻的设计**，以及当前真实实现。
>
> **本文由三份阶段方案归档收敛而成**（`AI统一入口整改方案`、`AI 统一网关：现状、缺陷、整改方案-v2`、
> `AI统一网关整改-评审结论与最终执行计划-v3.1`），三者原件已移入 `.archive/docs-obsolete-20260928/`。
>
> **效力**：本文只描述**可验证的实现与已登记的决策**；与代码冲突时**以代码为准**。
> 现行规范见 [项目开发规范](../../02-开发指南/全端-规范-项目开发规范-V13.30.md)，现状全景见 [全端-规划-项目现状总结](../../06-规划路线/全端-规划-项目现状总结-V13.29.md) 第 1 节。

---

## 一、时间线与版本

| 版本 | 日期 | 事件 |
|---|---|---|
| v11.x | ≤2026-09-16 | **问题期**：业务方各自直连 LLM（`llmService.generate`），治理（限流/熔断/注入防护/日志）无处统一施加；场景码硬编码散落 |
| v12.1 | 2026-09-18 | Mapper 内联 SQL 全量外置 XML（消除手写注入面，TD-04）——同期进行 |
| **v12.2** | 2026-09-18 | **统一入口原则确立**：调度层语义化；全平台 AI 调用收口网关，业务层只包装上下文 |
| v12.2.1 | 2026-09-18 | AI 场景码**枚举化**收口（`AiSceneEnum`） |
| v12.2.2 | 2026-09-18 | **删除早期自造封装** `LlmClient` / `NoopLlmClient` / `AiModuleLlmClient`（与网关重叠） |
| v12.2.3 | 2026-09-18 | **业务端直连 LLM 收编**：`PortalAiController` 自造场景分发、`CmsWritingPromptServiceImpl` 直调、`KnowledgeQaHandler` 场景码入枚举 |
| v12.3 | 2026-09-27 | 全项目架构评审整改（三轮验证 · 附录 A~I） |
| **v13.0** | 2026-09-27 | **文档基线校正**：明确"7/7 场景逐一手写 Handler"作废 → 实际为**配置驱动**；包名 `ai2 → aigateway` |
| v13.19 | 2026-09-27 | 场景配置改**请求级记忆化**（同请求不重复查 `ai_scene_config`），且**不用 TTL 缓存**以保留"改配置下次调用即生效" |
| v13.20 | 2026-09-27 | LLM 输出解析收敛为唯一实现 `util.json.LlmJsonExtractor` |

## 二、四个阶段的演进

### 阶段 1：问题暴露（v12.0 前）

- 业务层直接 `llmService.generate(...)`：限流、成本熔断、注入防护、执行日志**无法统一施加**；
- 场景标识硬编码字符串（如 `"knowledge_qa"`），**不在枚举内** → 管理端场景总览存在盲区；
- 早期自造封装 `LlmClient` 与后来的网关**职责重叠**（仅被当作冗余开关 `isEnabled()` 使用）。

### 阶段 2：统一入口原则（v12.2）

**四条原则**（当时确立，至今有效）：

1. **业务层只包装上下文**：构造 input Map（`{userId, range, title, content...}`），不直接调 LLM；
2. **调用方绑定 `sceneCode`（+ 可选 `agentId`）**：走 `AiGatewayService.execute(AiExecuteRequest)`
   或便捷入口 `AiSceneJsonClient.executeForJson(sceneCode, input, userId)`；
3. **治理由网关统一施加**：限流 / Token 熔断 / 语义缓存 / 意图分类 / 注入防护 / 输出过滤 / 执行日志 / 降级，业务无感；
4. **admin 端例外**（用户 v12.2.3 裁决）：admin 侧系统管理（agent / 工作流 / 大模型配置）的 AI 调用
   属合理直连，**不在收编范围**——`PromptGeneratorServiceImpl`、`WorkflowGeneratorServiceImpl`、
   `IntelligentAnalysisServiceImpl`、`SQLGeneratorServiceImpl`、`DiagramChatServiceImpl` 5 个 Service 保持直调。

### 阶段 3：清理与收编（v12.2.x）

| 项 | 处置 |
|---|---|
| `LlmClient` / `NoopLlmClient` / `AiModuleLlmClient` | **删除**（6 个 Service 去掉冗余开关注入，改用 `aiGlobalSwitch`） |
| `PortalAiController` 自维护 SCENES Map + 直调 | **薄化**，改调网关 |
| `CmsWritingPromptServiceImpl` 直调 | 改调网关 |
| `KnowledgeQaHandler` 硬编码场景码 | 收进 `AiSceneEnum` |
| `FinanceAnalysisHandler` 散落位置 | 归口到网关 handler 目录 |

### 阶段 4：架构评审与定案（v2 → v3.1 → v13.0）

两份方案（v2 问题分析、v3.1 评审结论+执行计划，均 2026-09-24）提出七阶段整改。
**v3.1 相对 v2 的关键修正**（保留存档价值）：

| v2 原案 | v3.1 修正 | 理由 |
|---|---|---|
| 11 Handler → 0 | **维持 0**（用户裁决），SPI 接口保留作"逃生舱" | 业务封装下沉各自 Service，Service 与 Handler 功能重合；且 `FinanceAnalysisHandler` 反向依赖 `ledger`/`portal`（网关层渗入业务层） |
| 阶段三 Adapter（3 个） | **砍掉** | 解决不存在的问题；现有 `AiSceneResolver` 链路已验证 |
| 阶段四性能 P1（缓存/压测/QPS50） | **降为 P2 生产前置** | 开发设计阶段无流量；配置缓存牺牲即时生效需显式决策 |
| 阶段五 token 减 75%/80%/70% | **先基线后定目标**；面试禁滑窗 | 无基线的指标是拍脑袋；面试上下文完整性是体验红线 |
| 七阶段顺序（版本管理最后） | **版本管理前置为阶段二第一步** | prompt 大规模进配置前必须有回滚安全网 |
| 仅 Java 改动清单 | 补齐 SQL / DML / 菜单 / Vue / 测试 | 四同步铁律 |
| `IntentClassifier` 未交代 | 会话模式**跳过**意图分类 | 会话已绑定场景；分类误打断风险 |

> ⚠️ **v3.1 的部分表述现已过期**：其"补落地设计：task 拆行 + 查数下沉 Service +
> 逐场景封装"等描述，已由 v13.0 的**配置驱动**实现统一取代（见下节）。
> 该文档当时也沿用了 `aiapp` 包名——包名沿革为 **`ai2` → `aiapp` → `aigateway`**，最终定为后者。

## 三、现行实现（以代码为准，v13.x）

**包**：`com.moyun.ext.aigateway`（`ext/ai2`、`ext/aiapp` **均已不存在**）

```
业务层（ext.cms / portal / ledger / ext.ai）
  │ 构造 input Map（LinkedHashMap/HashMap，禁 Map.of）
  ▼
AiSceneJsonClient.executeForJson(sceneCode, input, userId)      ← 业务方便捷入口
  ▼
AiGatewayService（治理链：限流 → 注入清洗 → 成本熔断 → 场景配置校验 → Agent/模型路由）
  │ 场景配置读自 ai_scene_config（**请求级记忆化**，非 TTL 缓存 → 改配置下次调用即生效）
  ▼
AiSceneRegistry → DefaultSceneExecutor → AbstractAiSceneHandler   ← 统一执行器，非逐场景类
  ▼
ai_execute_log 落库（success/fail/error_msg/elapsed/model_used）
  ▼
FallbackStrategy 降级 → 业务规则兜底
```

**关键事实（可逐条对照代码验证）**：

| 项 | 现实 |
|---|---|
| Handler 类数量 | `handler/` 下**仅 2 个文件**：`AiSceneHandler`（SPI 接口）+ `AbstractAiSceneHandler`（基类）。**无逐场景 Handler 类** |
| 场景差异在哪 | 全部由 **`ai_scene_config` 表**声明（绑定 Agent/模型/限流/输出模式）；后台可改，业务零感知 |
| 新增场景成本 | **通常零代码**：后台建 `ai_scene_config` + 业务拼 input |
| 执行器 | `DefaultSceneExecutor`（统一执行路径） |
| **流式** | ✅ 已落地：`AiGatewayController.executeStream`（SSE）→ `AiGatewayService.executeStream`；`AiSceneHandler` 提供 `executeStream` 默认实现 → **面试主干已可走网关**，无需 langchain4j 直连旁路 |
| **配置版本化** | ✅ 已落地：`ai_scene_config.version` + `ai_scene_config_history` 快照 + `AiSceneConfigVersionService`（更新=快照旧版 → 版本+1 → 更新，同事务） |
| **会话版本锁** | ✅ 已落地：会话首轮把 `config_version` 锁进 Redis（`RedisKeys` 会话配置版本前缀）→ **配置回滚只影响新会话，进行中会话按锁定版本继续** |
| 模型缓存键 | `模型ID:temperature:maxTokens:jsonMode`（同配置共享 `ChatLanguageModel` 实例） |
| 安全三件套 | `PromptInjectionGuard`（输入清洗，需**可变 Map**）、Token 成本熔断、输出过滤 |
| 语义缓存隔离 | 键带 `userId`（`ai2:cache:u:{userId}:{scene}:...`），**空 userId 不查不写**（v13.2 修复跨用户泄漏） |
| Token 计量 | 非流式由服务端回传 usage；**流式拿不到 usage** → `TokenMeter` 本地估算并置 `token_estimated=1`，**真实优先、估算可区分**（v13.3） |
| 运行时开关 | `AiGlobalSwitch` → `sys_config`（热生效）；yaml `moyun.ai.enabled` 仅承担 bean 装配 |
| LLM 输出解析 | 唯一入口 `com.moyun.util.json.LlmJsonExtractor`（v13.20 收敛，含括号配平扫描） |

## 四、未被采纳 / 已否决的设计（避免后人重走）

| 设计 | 结论 |
|---|---|
| 为每个业务写一个场景 Handler 类 | **否决**（用户 2026-09-24 裁决"维持 0"）；业务封装下沉各自 Service，场景差异走配置表 |
| 阶段三"加 Adapter 保留独立调用" | **砍掉**——解决不存在的问题 |
| 用 TTL 缓存缓存场景配置 | **否决**（v13.19）：会把"管理端改配置下次调用立即生效"降级为"最多 TTL 后生效"；改用请求级记忆化 |
| 业务端直连 `LlmClient` 自造封装 | **删除**（v12.2.2），与网关职责重叠 |
| admin 端 AI 调用一并收编 | **不整改**（用户裁决）：admin 系统管理的 AI 调用属合理直连 |
| 会话模式跑意图分类 | **跳过**：会话已绑定场景，分类误打断 |

## 五、开发规约（现行，摘自 全端-规划-项目现状总结）

1. 所有 LLM 调用**一律经网关**收口，业务方用 `AiSceneJsonClient.executeForJson`；**禁止直连 LLM 客户端**
   （admin 端 5 个 Service 除外，见 §2 原则 4）；
2. 新场景 = 后台建 `ai_scene_config` + 业务拼 input，**通常零代码**；
3. 网关输入用**可变 Map**（`LinkedHashMap`/`HashMap`），**禁 `Map.of`**（不可变集合会被输入清洗路径击穿）；
4. 外部不可信数据（转写/提问/简历）走 `input.context` / `input.transcript` + `PromptInjectionGuard.wrapData`，
   **禁走顶层 `userInput`**；
5. 语义缓存**必须按用户隔离**，确需跨用户共享的公共场景应显式声明作用域；
6. 同请求内**不重复查同一配置行**（请求级记忆化，禁 TTL 缓存替代）；
7. 异步重任务走通用 AI 异步任务表（轮询进度），同步链路不做重活。

---

## 附：原文归档位置

三份阶段方案原件（含完整论证、代码草案、验收标准）已移入
`.archive/docs-obsolete-20260928/docs/02-技术方案/`：

- `AI统一入口整改方案.md`（v12.2 原则与 v12.2.x 收编清单）
- `AI 统一网关：现状、缺陷、整改方案（完整版）-v2.md`（问题分析 + 七阶段方案 + 三维度评估）
- `AI统一网关整改-评审结论与最终执行计划.md`（v3.1 评审修正 + 验收标准 + 停止点）

> 归档原因：作为"待执行方案"已失效（其描述与现行配置驱动实现不一致），
> 但改名沿革与整改论证过程有参考价值，故收敛为本文并保留原件。
