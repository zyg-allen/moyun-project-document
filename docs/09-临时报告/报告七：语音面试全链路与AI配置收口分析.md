# 报告七：语音面试全链路 · 配置体系 · 提示词体系综合评审（重构版 v3 · 开发指导稿）

> **状态**：分析稿 + **开发指导稿**，**代码未改动**。
> **方法**：源码逐行取证（`文件:行号`）+ dev 库实测（`moyun-db`，`COUNT(*)` 实测值）+ 管理端页面/表单核对 + **场景码全量扫描（代码 + SQL）**。
>
> ---
> ### ⚠️ v13.39 勘误与现状更新（2026-09-30 复核，**改前必读**）
>
> **① 数据库口径已变更**：本报告成稿时实测的是 **`moyun-db`**（旧库）；应用现连 **`moyun-db2`**（新库，`application-dev.yaml` 默认值）。
> 两库 AI 配置**不是同一套数据**（`ai_agent` 旧库 7 行 / 新库 2 行；agent id 分叉）。
> **结论 2 的"dev 库三条链引用全悬空 / 从未成功开面"仅对 `moyun-db2` 成立** ——
> `moyun-db` 实测有 **18 场**面试记录、绑定链自洽，故"从未成功开面"的表述**不准确**。
>
> **② 悬空引用根因已定位并修复（v13.38 / v13.39）**：
> 根因是 **`init-sql` 种子里硬编码了来源库的 agent id**（`ai_scene_config.agent_id=48/47`、`sys_config.defaultAgentId='48'`），
> 而 `ai_agent` 的 INSERT **不含 `id` 列**（自增分配，全新库恒为 1/2）→ **任何全新库初始化即带悬空**。
> 已改为**按 `name` 子查询取 id**（`moyun-db-dml-init.sql`）+ 增量脚本 `20260929-04` 修现网（幂等 + `_bak_` 备份）。
> **现状：`moyun-db2` 悬空引用 = 0，`defaultAgentId=2`（AI面试官·默认），面试可正常开启。**
> ⇒ **§0.1 结论 2、§3.5 悬空表、§8 阶段 0 的 T0-1/T0-2 均已过时**（数据已修复，无需再改数据）。
>
> **③ 两处判定更正**：见 §0.1 结论 5（P1-2）与 §4 清单 P2-4 的内联勘误。
>
> **④ 报告定位澄清**：本报告是**「配置与提示词专项审计」**，**不是**四视角（功能完整性/扩展性/性能/架构）全链路评审。
> 报告未覆盖的 6 项运行期隐患（并发保护/事务/超时/断连/成本熔断/多实例）见
> 《全端-评审-报告七再评审-20260930》。
> ---
> **用途**：**明天据此指导开发修改**（§8 是可直接照做的实施清单，含验收标准）。
> **读者**：决策人（§0、§4、§7）；实施人（§2、§3、§5、§6、**§8**、§9）。

---

## 0. 一页速览

### 0.1 七条核心结论

| # | 结论 | 级别 |
|---|---|---|
| 1 | **面试官/模型解析没统一到场景**：对话链读 `sys_config.voice.interview.defaultAgentId`，预热/分析/自介读 `ai_scene_config`，报告链又用会话 `agentId` 另起一次 → **一场面试三条互不校验的 AI 绑定链** | P0 |
| 2 | **dev 库三条链引用全悬空**（场景 `agent_id=48`、sys_config `=4`、`ai_agent.model_config_id=17` 均不存在）→ `start()` 必抛"AI 面试官未配置或不可用"；实测 **`portal_voice_interview` 0 行**（从未成功开面） | P0 |
| 3 | **场景码登记不全 + 校验粒度只认整串**：真实**主场景 11 个、枚举仅 10 个**，缺的正是 **`default_chat`**（AI 对话主链，`ChatController:157` 运行时必需，库里 id 5 有配置行）；`AiSceneEnum.of()` 是**精确匹配** → 合法的 `main:task` 子场景反而**存不了**，`resume_optimize:*`／`voice_interview:*`／`default_chat` **共 12 行只能靠 SQL 维护**；子任务侧还缺常量（`jd_keywords` 是裸字面量） | P0 |
| 4 | **提示词"两个都用"但三条链三种拼法**：场景 JSON 链 = Agent 人设(系统) + 场景 user 模板(用户)；**面试正式对话链完全不用场景模板**（系统提示词是业务代码拼的，首轮写进记忆）；报告链提示词**硬编码在 Java 字符串**。场景 `system_prompt_template` 与 `portal_interview_config.prompt_template` 是**死字段但 UI 可编辑** | P1 |
| 5 | **上下文与评分三处割裂**：逐题分析**无对话历史**；一题**最多评三次**（逐题→报告骨架→报告 LLM），代码里已留"分数二次融合失真"防错。<br>⚠️ **v13.39 改判**：原写的"自我介绍**被评两次**"**不成立** —— `ScoringEngine.evaluateSelfIntro` **全仓零调用**、`setIntroScoreJson(` **0 处调用** ⇒ `intro_score_json` **恒为 NULL**，**自介评分从未接线**，总分也从不含自介分。这是"**功能缺失**"而非"重复评分"，修法完全不同（应接线或删死代码，不是去重） | P1 |
| 6 | **面试规则 10 条只有 1 条有配置入口**：`interviewConfig` 仅提供 `scoringWeights`；其余在 `sys_config`／Agent 表／场景表／**硬编码**；该表另有 7 字段 UI 可编辑但后端零消费 | P1 |
| 7 | **版本锁只覆盖会话链**：`resolveSessionConfig` 仅对带 `sessionId` 的调用锁版本，而预热/分析/自介**不传 sessionId**；且 `ai_scene_config_history` **0 行**（从未写入快照）→ 会话中途改配置会回落当前配置 | P1 |

### 0.2 关系总图（现状）

```
                         ┌──────────────── 管理端（运维） ────────────────┐
                         │ 场景配置页 /ai/ai-config/scene ← 注册表(枚举,只读)│
                         │   └─ list: DB 20 行（含 scene:task 与 default_chat）│
                         │ 智能体管理(ai_agent)  面试配置(portal_interview_config)│
                         └───────┬───────────────────────┬────────────────┘
                                 │写                      │写（仅 scoringWeights 活）
                     ┌───────────▼──────────┐   ┌────────▼─────────┐
                     │ ai_scene_config(20)  │   │sys_config(2 键)  │
                     │ agent/model/tpl/治理 │   │ durationMinutes  │
                     └───────────┬──────────┘   │ defaultAgentId   │
                                 │              └────────┬─────────┘
  场景唯一入口（本应）            │                       │（对话链实际入口）
                     ┌───────────▼───────────────────────▼─────────┐
                     │            AI 统一网关 AiGatewayService       │
                     │  同步 execute（场景JSON链）│会话流式 executeConversationStream│
                     └───┬───────────────────────┬──────────────────┘
        ┌────────────────▼───────┐      ┌────────▼─────────────────┐
        │ DefaultSceneExecutor    │      │ ContextManager 滑窗记忆  │
        │ 系统=Agent人设+输出约束  │      │ 系统消息=业务代码拼的     │
        │ 用户=场景 user 模板      │      │ （buildInterviewerSystemPrompt）│
        └─────────────────────────┘      └──────────────────────────┘
                         ▲                       ▲
        ┌────────────────┴───────────────────────┴──────────────────┐
        │ VoiceInterviewServiceImpl（预热/对话/逐题分析/自介/报告）    │
        └──────────────────────────┬───────────────────────────────┘
        ┌──────────────────────────▼───────────────────────────────┐
        │ portal_voice_interview（会话·0 行）＋ _qa（25 列）＋ _event │
        │ ＋ report(JSON) ＋ introScoreJson ＋ configJson/contextSnapshot│
        └──────────────────────────────────────────────────────────┘
```

### 0.3 关键数字（dev 库实测）

| 项 | 值 | 含义 |
|---|---|---|
| `ai_scene_config` | **20 行 / 45 列** | 主场景 **11**（枚举 10 + `default_chat`）＋ 子任务 **9**（`main:task`） |
| `ai_scene_config_history` | **0 行** | 版本锁已实现但**从未写入快照** → 改配置回落当前值（warn 日志） |
| `ai_agent` / `ai_model_config` | 2 / 4 | 两个 agent 的 `model_config_id=17` **不存在** → v13.39 说明：**不影响运行**（`AgentModelRouter` 自动挑选默认 chat 模型）；`is_default=1` 两行分属 embedding/chat，**非二义** |
| `portal_voice_interview` / `_qa` / `_event` | **0 / 0 / 0** | 新库 `moyun-db2` 尚未开面（**旧库 `moyun-db` 实测 18 场**）；v13.38 已修复绑定链，现可正常开面 |
| `portal_interview_config` | 1 行 / 18 列 | 仅 `scoringWeights` 活，其余 7 字段死 |
| `portal_user_resume` | 4 行 | 报告链简历摘要可用 |
| 场景码全量扫描 | 主场景 11 / 子任务 9 / **枚举缺 1** / **常量缺 1** | 见 §3.3 |

---

## 1. 角色（Actors）与元素（Elements）

### 1.1 角色表

| # | 角色 | 是什么 | 代码中是谁 | 拥有（可写） | 只读/消费 |
|---|---|---|---|---|---|
| A1 | **候选人（用户）** | 门户登录用户 | `PortalSecurityUtils.getUserId()` | 回答、简历 | 报告、分享页 |
| A2 | **面试会话** | 一场面试的权威容器 | `PortalVoiceInterview` | 状态/分数/快照/报告 | 全部链路的上下文来源 |
| A3 | **面试官 Agent** | 人设与模型载体 | `Agent` / `ai_agent`（34 列） | `system_prompt`、模型、温度、`maxHistoryTurns`、知识库 | 各链路读取 |
| A4 | **场景（任务）** | AI 调用的治理与任务单位 | `AiSceneConfig` / `ai_scene_config`（45 列） | 绑定（agent/model/知识库/工作流）、提示词模板、输出结构、限流/配额、版本 | 网关路由、提示词 |
| A5 | **模型** | 推理服务 | `ModelConfig` / `ai_model_config`（20 列） | provider/model/流式能力/默认标记 | `AgentModelRouter` |
| A6 | **AI 统一网关** | 治理+路由+记忆 | `AiGatewayService` | 执行日志、Token 计量、会话版本锁 | 场景配置、Agent |
| A7 | **面试业务服务** | 编排全流程（**目前也拼提示词**） | `VoiceInterviewServiceImpl`（2303 行） | 会话/QA/报告/`configJson`/`contextSnapshot` | Agent、场景、面试配置 |
| A8 | **知识库（RAG）** | 考察方向检索源 | `ragRetrievalService` + `Agent.knowledgeLibraryIds` | 检索片段 | 预热注入 |
| A9 | **工作流** | 报告后置动作 | `ai_workflow` + `SceneBinding.workflowId` | 报告后的自动化 | 会话/报告 |
| A10 | **会话记忆** | 对话滑窗+摘要+版本锁 | `ContextManager` + Redis | 滑窗消息、摘要、锁定版本 | 对话链 |
| A11 | **管理端运维** | 配置维护者 | 场景配置/智能体/面试配置页 | 场景/Agent/面试配置 | — |
| A12 | **报告** | 最终交付物 | `VoiceInterviewReportVO` + 会话 `report` 列 | 分数/维度/点评/知识点 | 前端展示、分享 |

### 1.2 元素清单（分层）

| 层 | 元素 | 位置 | 现状备注 |
|---|---|---|---|
| 页面 | 准备/面试页 | `moyun-portal/src/pages/interview/VoiceInterviewPage.vue`（3550 行） | 5 步准备条 → `start` |
| 页面 | 场景配置页 | `moyun-admin-vue/src/views/ai/scene/index.vue` | 注册表**只读**；场景码是 **el-select(仅枚举)** |
| 页面 | 面试配置页 | `moyun-admin-vue/src/views/cms/interview/interviewConfig/index.vue` | 9 字段，仅 1 有效 |
| 页面 | 智能体管理 | `moyun-admin-vue/src/views/ai/agent/index.vue` | 人设/模型/知识库 |
| 接口 | 面试主接口 | `PortalVoiceInterviewController`（`/portal/interview/voice`） | `start`(@VipOnly)/`{id}/answer`(SSE)/`{id}/hint`/`{id}/finish`/`{id}/analysis`/`{id}/regenerate-report`/`{id}/resume`/`{id}/share`/`share/{token}`/`my/list`/`active`/`qa/{id}/toWrongBook`/`asr`/`hint`/`keywords` |
| 接口 | 场景配置接口 | `AiSceneConfigController`（`/cms/ai/scene`） | `registry` 供注册表；`validate` 硬校验 |
| 接口 | 面试配置接口 | `CmsInterviewConfigController`（`/cms/interview/config`） | CRUD，权限 `cms:interview:config:*` |
| 服务 | 面试编排 | `VoiceInterviewServiceImpl` | 预热/对话/分析/自介/报告全在内部 |
| 服务 | 评分引擎 | `ScoringEngine` | 自介独立 LLM 评分（4 维） |
| 服务 | Agent 客户端 | `InterviewAgentClientImpl` | `resolveAgent` / `resolveAgentForScene`（4 级链，**start 未用**）/ `chat` |
| 服务 | 场景注册表 | `AiSceneRegistry` | handlerMap → 主场景 → `DefaultSceneExecutor`；`getConfig` 请求级记忆化 |
| 服务 | 统一执行器 | `DefaultSceneExecutor` | 系统=人设+输出约束；用户=场景模板 |
| 表 | 场景配置 | `ai_scene_config`（45 列） | 绑定/提示词/输出/模型参数/治理/安全/缓存/版本 |
| 表 | 场景版本快照 | `ai_scene_config_history` | **0 行** |
| 表 | Agent / 模型 | `ai_agent`(34 列/2 行) / `ai_model_config`(20 列/4 行) | 悬空引用 + 双默认 |
| 表 | 会话/问答/事件 | `portal_voice_interview`(30 列) / `_qa`(25 列) / `_event`(5 列) | 均 0 行；含 `configJson`/`contextSnapshot` |
| 表 | 面试配置 | `portal_interview_config`(18 列/1 行) | 7 死字段 |
| 配置源 | `sys_config` 面试键 | `durationMinutes=20` / `defaultAgentId=4` / `reportLlm.enabled` | 对话链 agent 唯一来源，**悬空** |
| 提示词 | Agent 人设 | `ai_agent.system_prompt`（448~578 字） | 活（系统消息主体） |
| 提示词 | 场景用户模板 | `ai_scene_config.user_prompt_template`（子场景 586/588/771 字；父场景 NULL） | 活（仅旁路链） |
| 提示词 | 场景系统模板 | `ai_scene_config.system_prompt_template` | **废弃不读**，**UI 可编辑** |
| 提示词 | 面试配置模板 | `portal_interview_config.prompt_template` | **死字段**，UI 可编辑 |
| 评分维度 | 逐题 6 维 | `relevance/professionalism/fluency/interactivity/confidence/logic` | 活 |
| 评分维度 | 自介 4 维 | `structure/awareness/matching/fluency` | 活（独立场景） |
| 评分维度 | 报告 6 维 + perQuestion | `enhanceReportByAgent` | 活（**硬编码提示词**） |
| 报告字段 | 20+ | `VoiceInterviewReportVO` | 见 §2.7 |

### 1.3 元素关系矩阵（**当前态**：消费者 → 依赖源）

| 消费方 ↓ / 依赖 → | `ai_scene_config` | `sys_config` | `ai_agent` | `portal_interview_config` | 会话快照 | 记忆 | 枚举 |
|---|---|---|---|---|---|---|---|
| `start()` 解析面试官 | — | ✅**defaultAgentId** | ✅（经上值） | — | 写 `agentId` | 写首轮 | — |
| 会话时长 | — | ✅**durationMinutes** | — | — | — | — | — |
| 预热 warmup | ✅子场景行 | — | ✅（子行 agent 2） | — | 读简历/JD | — | ✅常量 |
| 对话链（每轮） | ⚠️**仅治理/版本/限流** | — | ✅（会话 `agentId`） | — | 读 `configJson` | ✅滑窗 | ✅常量 |
| 逐题分析 | ✅子场景行 | — | ✅（同上） | — | 读简历 | ❌**不含** | ✅常量 |
| 自介评分 | ✅子场景行 | — | ✅（同上） | — | — | ❌ | ✅常量 |
| 报告骨架 | — | — | — | ✅**scoringWeights** | 读 QA 全量 | ❌ | — |
| 报告 LLM 复盘 | — | ✅开关 | ✅（会话 `agentId`） | — | 读 QA 全量 | ❌ | — |
| 版本锁 | ✅`config_version` | — | — | — | 锁在 Redis | — | — |

---

## 2. 逐环节链路（现状 + 证据）

> 每节格式：**触发 → 入参 → 服务 → LLM 调用 → 配置来源 → 上下文 → 落库 → 一致性判定**。

### 2.1 准备：`/interview/voice` 页面

- 前端 5 步准备条（`VoiceInterviewPage.vue:1478-1486`），请求返回即 100%。
- 入参（`:1488-1495`）：`{position, jobRequirements?, resumeId?, difficulty, questionCount}` → **无 `sceneCode`、无 `agentId`**。
- 会员：前端仅提示，后端 `@VipOnly`（`PortalVoiceInterviewController:70`）兜底。
- **一致性判定**：场景由后端常量决定（`AiSceneEnum.VOICE_INTERVIEW`），前端无法选择。

### 2.2 `start()`：解析 → 预热 → 建会话

| 步骤 | 代码 | 配置来源 | 说明 |
|---|---|---|---|
| 时长 | `:374` | `sys_config…durationMinutes=20` | 硬边界 |
| **面试官** | `:378 resolveAgent(null)` | **`sys_config…defaultAgentId=4`（悬空）** | 失败即 `throw`（`:379-381`）；`:376-377` 注释留着断句"// 面试官应该是" |
| 知识库预热 | `:387` | Agent 绑定知识库 | 失败不阻塞 |
| 预热 LLM | `:393 → :632-661` task=warmup | **`ai_scene_config` 子场景行（agent 2）** | 产出 understanding/interviewPlan/opening/firstQuestion |
| 建会话 | `:410 setAgentId`、`:428 setConfigJson`、`:435 setContextSnapshot` | — | 只落 agentId，**不落模型/场景版本** |
| 记忆播种 | `:473-474 initFirstTurn(id, maxTurns, systemPrompt, ctxUserMsg, opening)` | systemPrompt 来自 `buildInterviewerSystemPrompt:490+`（**代码拼**） | 对话链人设在此固化 |
| 事件 | `:477-482 recordEvent("start", …)` | — | 审计 |

- **一致性判定**：面试官（sys_config）与预热（场景）**从第一秒就可能不同**；当前都悬空 → 开不了面。

### 2.3 对话循环（每轮，SSE）

- 端点 `POST /{id}/answer`（`PortalVoiceInterviewController:86`，TEXT_EVENT_STREAM）。
- 服务 `runAgentTurn:762-784`：`resolveAgent(interview.getAgentId())` → `ConversationStreamCommand{sceneCode=voice_interview, sessionId=memoryId(id), userId, userInput, agentId, maxMessages, directives}`
- 网关 `AiGatewayService:362+`：①`resolveSessionConfig` **锁版本**（`:524-556`）②注入防护/限流/Token 熔断 ③`createStreamingModel`（`:421-426`）④`ContextManager.buildTurnMessages`（`:433`）⑤流式→入滑窗+计量（`:459-486`）
- 消息顺序（`ContextManager:64-87`）：**[SystemMessage(人设)] → [早期摘要] → 滑窗历史 → 本轮瞬态指令**
- **一致性判定**：✅ 轮次间人设/模型恒定；❌ **场景 user 模板完全不生效**（父场景模板本就 NULL，代码也不读）；⚠️ 口吻取决于代码拼装，**运维改不到**。

### 2.4 逐题分析（旁路）

- `analyzeAnswerByLlm:178-211`（task=`answer_analysis`）。
- 输入：`岗位 + 简历摘要 + 题目(≤400) + 考察要点` + `transcript`；**无 `sessionId`、无对话历史**。
- 输出：`score/feedback/dimensions(6)/sentiment/fluency/redFlags/completeness` → `QA.llmScoreJson`/`llmAnalysisJson`。
- **一致性判定**：❌ 割裂点 1；❌ 不参与版本锁（`resolveSessionConfig:528` 注释"非会话模式始终读当前配置"）。

### 2.5 自我介绍（**⚠️ v13.39 改判：不是"双评"，是"从未接线"**）

- 对话层：✅ V4 段序约束把第 1 问固定为自我介绍；`buildCandidateProfile:1427-1435` 取第 1 题作答。
- 评分层：❌ **死代码**。`ScoringEngine.evaluateSelfIntro:61` **全仓零调用点**（含 test/vue/sql）；
  `PortalVoiceInterview.introScoreJson` 只被**读**（`:1294`、`:1535-1540`），**全仓无 `setIntroScoreJson(` 调用**
  ⇒ `intro_score_json` **恒为 NULL**。
- 融合：❌ 因上游恒 NULL，`:1296-1299` 的 `introScoreView == null` 分支**恒真** ⇒ `fuseTotalScore` 实际只用 LLM 均分，
  **自介分从未参与总分**；`:1366` `report.setIntroScore(null)` ⇒ **报告里自介分始终为空**。
- 同向线索：`:1178` 注释「V3 规则引擎已删，ScoreResult 为默认值」；`ScoringEngine.java:29`「随 C1 接入 submitAnswer 链路时启用」。
- **一致性判定**：❌ 割裂点 2 —— 但性质是**功能缺失（未接线）**，不是"同段话两套标准"。
  ⚠️ **修法差异**：应做「接线启用」或「删死代码」的**二选一裁决**，而非"消除重复评分"。

### 2.6 结束与批量分析

- `finish:1040+` → `analysisStatus=1` → `submitBatchAnalysis:1094-1120` → `runBatchAnalysis` → `aggregateAndStoreReport`（规则聚合）→ `enhanceReportByAgent`。
- 防错：`regenerate-report` 注释（`:1494-1495`）要求清 `report/summary`、`scoreDraft`、`analysisStatus`，否则"分数二次融合失真"。
- **一致性判定**：❌ 割裂点 3。

### 2.7 报告与分享

- `enhanceReportByAgent:1591-1660`：输入 = 岗位 + JD(≤600) + 简历(≤800) + **全部问答**（题≤150/答≤400/初评分）；**系统提示词硬编码**（`:1631-1642`）；输出 `overallComment/jobMatch/highlights/weakPoints/suggestions/perQuestion/dimensions(6)`。
- 报告字段：`totalScore/dimensions/highlights/weakPoints/questionReviews/summary/suggestion/sentimentTrend/redFlags/fluencyAvg/introScore/improvementSuggestions/knowledgePoints/candidate/jobInfo/overallComment/jobMatch/highlightViews/weakPointViews`。
- **一致性判定**：✅ 用户/简历/岗位/情感/流畅/知识点都有；❌ 报告无面试官/模型/场景版本；❌ 知识点与题目/得分无稳定关联；❌ 缺"语气/措辞"维度。

---

## 3. 配置体系（五源四层）

### 3.1 五个配置源

| # | 来源 | 位置 | 关键字段 | 消费方 | 状态 |
|---|---|---|---|---|---|
| C1 | **场景配置** | `ai_scene_config`（45 列） | `agent_id`/`model_config_id`/`user_prompt_template`/`output_schema`/`config_json`/`workflow_id`/限流/`config_version` | 网关（旁路链+治理） | ✅ 活（编辑受枚举限制） |
| C2 | **sys_config** | `sys_config` | `voice.interview.defaultAgentId`、`durationMinutes`、`reportLlm.enabled` | `start()`、`resolveDurationMinutes`、报告开关 | ⚠️ agent 悬空 |
| C3 | **Agent 表** | `ai_agent` | `system_prompt`、`model_config_id`、`temperature`、`maxHistoryTurns`、知识库 | 对话链（人设/模型/记忆）、旁路链（人设） | ⚠️ 模型悬空 |
| C4 | **面试配置** | `portal_interview_config` | `scoringWeights`（活）；7 个死字段 | 评分融合 4 处 | ⚠️ 7 字段死 |
| C5 | **硬编码** | Java 源码 | 报告系统提示词、段序约束、首问=自我介绍、6 维/4 维维度表 | 报告链、对话链 | ❌ 运维不可改 |

### 3.2 提示词体系（**"两个都用"但三条链三种拼法**）

**角色划分（设计本意）**

| 层 | 字段 | 角色 | 落点 |
|---|---|---|---|
| Agent | `ai_agent.system_prompt` | **谁在说**（人设/语气/边界） | **SystemMessage**（`injectAgentPersona:588-611` 渲染 `{{}}` → `mergePersona:338-339` **前置**） |
| 场景 | `ai_scene_config.user_prompt_template` | **这轮产出什么**（任务+输出结构） | **UserMessage**（`AbstractAiSceneHandler:321-323`） |
| 场景 | `ai_scene_config.system_prompt_template` | — | **废弃不读**（`:310`；DB 全 NULL），**UI 可编辑** |
| 面试配置 | `portal_interview_config.prompt_template` | — | **死字段**，UI 可编辑 |

**三条链对照**

| 链路 | 系统消息 | 用户消息 | 场景 user 模板 |
|---|---|---|---|
| 场景 JSON（预热/分析/自介） | Agent 人设 + 输出结构约束（`DefaultSceneExecutor:77/122-125`） | **场景模板渲染** | ✅ 生效 |
| **面试正式对话**（流式） | **业务代码** `buildInterviewerSystemPrompt:490+`（人设+岗位/难度/题数+段序+预热计划），首轮写记忆 | 回答 + `directives` | ❌ 不生效 |
| 报告整场复盘 | **硬编码字符串**（`:1631-1642`） | 代码拼（岗位/JD/简历/QA 全量） | ❌ 不生效 |

### 3.3 场景码登记制（**结论：枚举缺 1 个主场景；校验粒度只认整串**）

> 口径澄清（重要）：**"登记制"本身是对的**——主场景必须登记（挡住乱加），子任务不膨胀枚举但要进白名单。问题不在规则，在**登记表不全 + 校验粒度**。

**场景码全量扫描结果（代码 + SQL，非抽样）**

| 来源 | 扫描结果 | 判定 |
|---|---|---|
| 业务服务常量（11 处 `SCENE_*`） | 全部 `AiSceneEnum.X.getCode()` | ✅ 已登记 |
| `ChatController:33` | **`"default_chat"` 裸字面量**（`:157 getConfig("default_chat")` 运行时必需） | ❌ **枚举唯一缺项** |
| SQL seed（`moyun-db-dml-init.sql:77+`） | 20 行场景码，与 DB 完全一致 | ✅ 无孤儿行 |
| `DefaultSceneExecutor.SCENE_CODE = "default"` | 执行器内部键，`AiSceneRegistry:74-76` 明确**不进路由表** | ✅ 不是场景 |
| `PortalAiController.mapSceneCode:128-132` | **白名单**（未知返回 null）且返回枚举码 | ✅ 无缺口 |
| `IntentClassifier.suggestedScene` → 网关 `:152-155` | `IntentResult` 构造仅 2 参，**从未赋值** → 该分支为死代码 | ✅ 不是来源 |
| 子任务常量（`AiSceneTasks`，8 个） | 与库中 8 个子任务行一一对应 | ✅ 一致 |
| `question_generate:jd_keywords` | `PortalJobTemplateServiceImpl:185` **裸字面量**，`AiSceneTasks` **无该常量** | ❌ **常量缺项** |

**真实主场景 11 个 vs 枚举 10 个 → 缺 `default_chat`**

| 主场景 | 枚举 | 库行 | 代码使用点 |
|---|---|---|---|
| `voice_interview`/`resume_parse`/`resume_optimize`/`question_generate`/`finance_analysis`/`sensitive_word`/`daily_topic`/`article_meta`/`content_tags`/`writing_prompt` | ✅ | ✅ | 各业务服务 |
| **`default_chat`** | ❌ **缺** | ✅ id 5（stream） | `ChatController:33,157`（AI 对话主链） |

**校验现状与被误拒的合法配置**

| 事实 | 证据 |
|---|---|
| 枚举注册表在管理页**只读总览** | `AiSceneConfigController:63-72`；页面文案"场景代码由代码注册（AiSceneEnum）" |
| 表单场景码是 **el-select（仅枚举 10 个）** | `views/ai/scene/index.vue:111-125` |
| 保存/更新都过 `validate()`，**`of()` 精确匹配** → 非枚举字符串直接拒绝 | `:95`(create)/`:132`(update) → `:233-257`；`AiSceneEnum:105-107` |
| ⇒ **合法子任务被误拒**：9 行 `main:task` **无法在管理端保存**（只能 SQL/seed 维护） | 上述两条叠加 |
| ⇒ **`default_chat` 也被误拒**（未登记） | 同上 |
| 运行时**不再需要 Handler**：SPI → 主场景 → `DefaultSceneExecutor` | `AiSceneRegistry.getHandler:94-103`；注释"不再要求每场景一个 Handler" |
| **真门禁是"必须有配置行"**（否则 `SCENE_NOT_FOUND`） | `AiSceneRegistry:92` + `@PostConstruct:186-200` |

### 3.4 版本 / 灰度 / 快照机制（已实现，但快照表为空）

- `ai_scene_config` 自带 `version`（灰度）/`config_version`（自增）/`weight`/`priority`/`is_default`/`enabled`/`open_api`。
- 会话版本锁：`resolveSessionConfig:524-556` 首轮写 Redis（30 天），版本变化时读 `ai_scene_config_history`。
- **实测 history = 0 行** → 真发生"会话中改配置"时 `loadSnapshot=null` → **回落当前配置**（`:555-557` warn）。
- 且**只对带 `sessionId` 的调用生效**（对话链有；预热/分析/自介没有）。

### 3.5 数据体检（实测）

```sql
-- 三处悬空引用（全部 matched = NULL）
SELECT s.scene_code, s.agent_id, a.id matched FROM ai_scene_config s LEFT JOIN ai_agent a ON a.id=s.agent_id WHERE s.scene_code LIKE 'voice_interview%';
SELECT c.config_key, c.config_value, a.id matched FROM sys_config c LEFT JOIN ai_agent a ON a.id=CAST(c.config_value AS UNSIGNED) WHERE c.config_key='voice.interview.defaultAgentId';
SELECT a.id, a.model_config_id, m.id matched FROM ai_agent a LEFT JOIN ai_model_config m ON m.id=a.model_config_id;
-- 默认模型「按 model_type 各一个」核查（v13.39：原注释"二义"有误，AgentModelRouter 只查 CHAT）
SELECT id,name,model_type,is_default,enabled FROM ai_model_config WHERE is_default=1 ORDER BY model_type;
```

| 项 | 实测 | 风险 |
|---|---|---|
| 父场景 `voice_interview.agent_id` | **48（不存在）** | 对话链若改走场景即失败 |
| 三个子场景 `agent_id` | **2（存在）** | 旁路链实际用 agent 2 |
| `sys_config.defaultAgentId` | **4（不存在）** | **`start()` 直接抛错** |
| `ai_agent.model_config_id` | **17（不存在）** | 走 `AgentModelRouter` 自动挑选 |
| `ai_model_config` 默认标记 | **两行 `is_default=1`** | 默认模型不确定 |
| `portal_voice_interview` | **0 行** | 从未开面成功 |

---

## 4. 问题清单（分级 · 证据 · 影响 · 归属）

| # | 级别 | 问题 | 证据 | 影响 | 归属层 |
|---|---|---|---|---|---|
| P0-1 | P0 | **三处 AI 绑定悬空** | §3.5 | 开面失败；修好 agent 后模型仍悬空 | 数据 + 配置校验 |
| P0-2 | P0 | **"面试官"由三个来源决定** | `:378`(sys_config) / 子场景行 / 会话 `agentId` | 三个"面试官"；无法审计 | 解析契约 |
| P0-3 | P0 | **枚举缺 1 个主场景 `default_chat`** | `ChatController:33,157`；库 id 5；枚举 10 个 | 运行时必需的场景**未登记**，管理端不可见/不可改 | 场景登记 |
| P0-4 | P0 | **子任务缺白名单 + 裸字面量** | `jd_keywords`（`PortalJobTemplateServiceImpl:185`）不在 `AiSceneTasks`；`default_chat` 亦为裸字面量 | 无法防"乱加子任务"；改名/重构易漏 | 场景登记 |
| P0-5 | P0 | **校验粒度只认整串 → 12 行只能 SQL 维护** | `AiSceneEnum.of()` 精确匹配 + `validate:233-257` | 面试 3 子场景、`resume_optimize:*`、`default_chat` **管理端无法合法维护** | 场景登记 + UI |
| P1-1 | P1 | **逐题分析无对话上下文** | `:189-201`（无 sessionId、无历史） | 追问链/一致性无法评估 | 上下文契约 |
| P1-2 | P1 | **自我介绍评分从未接线**（v13.39 改判，原判"双评"不成立） | `ScoringEngine.java:61` 零调用；全仓无 `setIntroScoreJson(`；`VoiceInterviewServiceImpl.java:1294/1366` | 自介分恒为空、从不参与总分 → **功能缺失**；且后续若在该字段上加消费方必踩空 | 评分契约 |
| P1-3 | P1 | **一题最多评三次** | 逐题→报告骨架→报告 `perQuestion`；`regenerate-report:1494` 防错注释 | 分数可复现性差 | 评分契约 |
| P1-4 | P1 | **版本锁不覆盖分析链** | `executeForJson` 无 sessionId；`:528` 注释 | 对话 v1、分析 v2 | 上下文契约 |
| P1-5 | P1 | **人设漂移** | 对话人设写记忆 vs 分析链现取 `agentPersona` | 提问旧人设、评分新人设 | 提示词契约 |
| P1-6 | P1 | **对话链不读场景模板** | `buildInterviewerSystemPrompt:490+` | 场景配置改不到正式问答 | 提示词契约 |
| P1-7 | P1 | **7 个死字段 UI 可编辑** | §3.1 C4 | 运维误以为生效 | 规则归属 |
| P1-8 | P1 | **面试规则 10 条仅 1 条有配置入口** | §2 各环节 | 题数/追问/自介开关/报告提示词均硬编码 | 规则归属 |
| P1-9 | P1 | **快照表 0 行** | §3.4 | 版本锁形同虚设 | 运维 |
| P2-1 | P2 | **报告无面试官/模型/场景版本** | `VoiceInterviewReportVO` | 无法回答"谁/哪个模型产出" | 可观测 |
| P2-2 | P2 | **知识点未与题目/得分关联** | warmup 计划只进提示词 | 无法做题目↔知识点↔得分分析 | 数据模型 |
| P2-3 | P2 | **会话死列** `style`/`profileSnapshot`/`questionPaper` | 全后端无写读 | 表结构误导 | 数据模型 |
| P2-4 | ~~P2~~ → **已撤销** | ~~默认模型二义~~（v13.39 校正：**不是问题，无需修复**） | `AgentModelRouter.java:72` 只按 `ModelType.CHAT` 查默认；`ai_model_config` 两行 `is_default=1` 分属 `embedding` 与 `chat` | **无影响**：按模型类型各一个默认，符合设计 | — |

---

## 5. 目标架构（To-Be）

### 5.1 角色职责矩阵（目标）

| 角色 | 拥有 | 明确不拥有 |
|---|---|---|
| Agent | 人设/语气/边界（跨任务复用）、模型绑定、记忆窗口 | 任务指令、业务变量、评分维度 |
| 场景 | 任务指令模板、输出结构、占位符契约、绑定校验、治理参数 | 人设正文、业务规则（评分权重/开关） |
| 业务服务 | 变量装配、会话编排、落库 | **提示词正文**、AI 绑定决定 |
| 面试规则配置（L2） | 评分维度与权重、流程开关、追问上限、上下文装配清单 | AI 路由 |
| 会话快照（L3） | 本场生效 agent/model/sceneVersion + contextSnapshot | 长期配置 |
| 网关 | 治理、记忆、版本锁、执行日志 | 业务语义 |
| 管理端 | 配置维护 + **引用有效性校验** + 生效预览 | — |

### 5.2 目标关系图

```
运维 ──写──> 场景(任务/模板/绑定) ──┐
      ──写──> Agent(人设/模型) ─────┤
      ──写──> 面试规则(评分/开关) ───┤
                                     ▼
业务服务(只装配变量) ──> 统一解析器(场景→Agent→模型) ──> 会话快照(固化)
                                     │
                     ┌───────────────┴───────────────┐
                     ▼                               ▼
              对话链(记忆+同一模板)          旁路链(预热/分析/自介/报告)
                     └───────────┬───────────────────┘
                                 ▼
                     单次评分聚合 ──> 报告(含面试官/模型/版本 + 知识点关联)
```

### 5.3 五个契约（落地的关键）

**① 解析契约**：所有 LLM 调用统一走 `resolveAgentForScene(sceneBinding, explicitAgentId=null)`；`sys_config.defaultAgentId` 降级为兜底或删除；**子场景 agent/model 留空即继承父场景**。

**② 提示词契约**
- 系统消息 = **Agent 人设 + 场景任务边界/输出约束**（业务代码不再拼提示词正文）；
- 用户消息 = **场景 `user_prompt_template`**（渲染 `{{变量}}`）；
- **对话链也纳入场景模板**（补 `voice_interview` 对话模板或新增 `:dialog` 子场景），`buildInterviewerSystemPrompt` 退化为"提供变量"；
- 占位符**强校验**（保存时拒绝未知占位符）；业务变量**只出现在场景层**；
- 死字段（场景 `system_prompt_template`、面试配置 `promptTemplate`）**UI 下线或标注废弃**。

**③ 上下文契约**：新增**统一装配器**（`InterviewContextAssembler`），四处调用共享同一份"上下文规格"；**所有链路携带 `sessionId`**（与对话链同锁版本）；生效绑定与上下文规格落**会话快照**。

**④ 评分契约**：统一 **8 维**（`relevance/professionalism/fluency/tone(新)/interactivity/confidence/logic/sentimentStability(新)`）；**自介不再独立评审**（作为第 1 题的维度子集映射）；**总分只算一次**（结束时单次聚合），逐题分仅作明细。

**⑤ 场景登记契约（对应 §3.3）**
- **主场景 `scene_code`**：**必须进 `AiSceneEnum`**（枚举 = 主场景目录 + 元数据 + 业务常量 + 注册表只读总览）→ 新增场景必须"加枚举项 + 加配置行"；
- **子任务 `main:task`**：**不进枚举**（枚举自身注释即为该口径），但 **task 段必须来自 `AiSceneTasks` 白名单**，主段必须是已登记主场景；
- **校验两段式**：`validate()` 判定 = `main ∈ 枚举` 且（无 task 或 `task ∈ AiSceneTasks`）；
- **代码零裸字面量**：`executeForJson(..)`/`setSceneCode(..)`/`put("task", ..)` 必须引用 `AiSceneEnum`/`AiSceneTasks` 常量，由结构守卫强制（§8 T1-5）；
- **UI 不可能加错**：场景码控件 = 「主场景下拉 + 子任务下拉（可空）」。

### 5.4 会话快照字段设计（建议）

| 列 | 类型 | 内容 | 作用 |
|---|---|---|---|
| `scene_code` | varchar(50) | `voice_interview` | 本场场景 |
| `scene_version` | int | 首轮锁定的 `config_version` | 复现 |
| `model_config_id` | bigint | 实际生效模型 | 审计/报告展示 |
| `agent_snapshot` | json | agentId/name/personaHash | 人设漂移检测 |
| `rule_snapshot` | json | 评分权重/流程开关/追问上限 | 规则复现 |
| `context_snapshot` | json（**已存在**） | 简历/JD/知识库片段/人设版本 | 上下文复现 |
| `config_json` | text（**已存在**） | 业务运行态（warmupPlan 等） | 运行态 |
| ~~`style`/`profileSnapshot`/`questionPaper`~~ | — | 死列 → 删除或明确用途 | 清理 |

### 5.5 配置归属终态

| 配置类别 | 归宿 | 谁可改 | 是否随会话锁定 |
|---|---|---|---|
| AI 绑定（agent/model/知识库/工作流） | `ai_scene_config` | 运维（带引用校验） | ✅ 快照 |
| 人设/语气 | `ai_agent.system_prompt` | 运维 | ✅ 首轮写记忆 |
| 任务指令/输出结构 | `ai_scene_config.user_prompt_template` + `output_schema` | 运维 | ✅ 快照 |
| 评分维度/权重、流程开关、追问上限 | **面试规则配置（L2）** | 运维 | ✅ 快照 |
| 时长/题数 | 业务参数（L2 或 `sys_config`） | 运维 | ✅ 快照 |
| 上下文装配规格 | L2 或装配器默认值 | 研发/运维 | ✅ 快照 |

---

## 6. 迁移路线（4 阶段 · 可独立发布/回滚）

| 阶段 | 内容 | 影响面 | 回滚 | 验收 |
|---|---|---|---|---|
| **① 数据修复** | 对齐 agent/model（父场景 agent、sys_config agent、`agent.model_config_id`）；去重复 `is_default`；子场景行清空 agent 改继承 | 仅数据 | 备份表还原 | 三条悬空 SQL 全 matched 非空；能成功开面 1 场 |
| **② 场景登记补全** | 枚举补 `default_chat`；`AiSceneTasks` 补 `jd_keywords`；去掉裸字面量；`validate` 改两段式；UI 改「主+子」双下拉；**新增裸字面量守卫**；死模板字段 UI 标注废弃 | 管理端场景页、枚举、`AiSceneConfigController`、`PortalJobTemplateServiceImpl` | 配置回退（无 DDL）；守卫可临时白名单兜底 | 管理端能合法新增/编辑 `voice_interview:warmup`；`default_chat` 可编辑；守卫红→绿 |
| **③ 会话快照 + 上下文/评分收口** | 解析统一（场景优先 + 兜底开关）；所有链路带 `sessionId`；统一装配器；自介并入统一维度；总分单点计算；报告补 interviewer 信息 | `VoiceInterviewServiceImpl`/`InterviewAgentClientImpl`/报告 VO/DDL | 按快照有无 `scene_version` **分流新旧链** + 灰度开关 | 一场面试全程 `agentId/model` 一致；报告显示场景/Agent/模型；重算同分 |
| **④ 规则归位** | `portal_interview_config` 收窄为"面试规则"（或新建 L2 表）；死字段下线；报告提示词进场景 | 管理端面试配置页/`ScoringEngine` | 保留表与历史数据 | 面试规则页字段**均有消费证据** |

**不受影响项**：`@VipOnly` 与 `TokenCostGuard` 按**场景**计费，改绑定不影响计费维度；`AiSceneRegistry` 已请求级记忆化 → 改配置下次调用生效。

---

## 7. 决策点（按主题分组 · 标注建议默认值）

**A. 配置归属（规则放哪里）**

| # | 决策 | 建议默认 |
|---|---|---|
| A1 | `interviewConfig` 归属：收窄为"面试评分与流程规则"／新建 `portal_interview_rule`／并入场景 `config_json` | **收窄**（改动小、保留历史），字段只留"有消费证据"的 |
| A2 | 业务参数（时长/题数）落点：`sys_config`／独立业务配置表／场景 `config_json` | **`sys_config`**（最小改动） |
| A3 | 是否重新启用"出题权重/追问上限/自介开关" | **暂不启用**（现无消费点，属功能增强，另立批次） |

**B. 场景登记（对应 §3.3）**

| # | 决策 | 建议默认 |
|---|---|---|
| B1 | 主场景登记：**补 `default_chat` 进枚举** | **补**（唯一缺项，1 行 + 元数据） |
| B2 | 子任务白名单：**补 `jd_keywords` 进 `AiSceneTasks`**、清裸字面量 | **补 + 清** |
| B3 | `validate` 改两段式（主段 ∈ 枚举 且 task ∈ `AiSceneTasks`） | **改** |
| B4 | UI：场景码改「主场景下拉 + 子任务下拉（可空）」 | **改**（从交互上杜绝乱加） |
| B5 | 是否再上 `ai_scene_registry` 表（后台自助新增主场景） | **暂不上**（登记制已够；待明确"业务自助新增"需求再评估） |
| B6 | 新增**裸字面量守卫**（`SceneCodeLiteralGuardTest`） | **加**（红→绿可验证） |

**C. 提示词**

| # | 决策 | 建议默认 |
|---|---|---|
| C1 | 对话链是否纳入场景模板 | **纳入**（补对话模板/`:dialog` 子场景） |
| C2 | 占位符是否强校验（拒绝未知占位符） | **校验** |
| C3 | 人设层是否禁止写业务变量 | **禁止**（业务变量只在场景层） |

**D. 评分与上下文**

| # | 决策 | 建议默认 |
|---|---|---|
| D1 | 维度是否统一为 8 维（含语气/情感稳定性） | **统一 8 维** |
| D2 | 自我介绍是否取消独立评审 | **取消**（作为第 1 题维度子集） |

---

## 8. 开发实施清单（**明天照着做**）

> 约定：每项含 **目标 / 位置 / 改动要点 / 验收 / 依赖**。每阶段完成后按项目惯例**四同步**（devlog + 本报告附录 + 开发规范 + 现状总结）并跑 `mvn -o -B test` 全量。

### 阶段 0 · 数据修复（无代码 · ~30 分钟）

| 项 | 目标 | 具体动作 | 验收 |
|---|---|---|---|
| T0-1 | 让面试能开起来 | `sys_config.voice.interview.defaultAgentId`：`4 → 2`；父场景 `voice_interview.agent_id`：`48 → 2`；`ai_agent.model_config_id`：`17 → 4`；`ai_model_config` 去掉多余 `is_default=1`（保留 deepseek 那行） | §3.5 三条悬空 SQL 全部 matched 非空；门户能成功开 1 场面试（`portal_voice_interview` ≥ 1 行） |
| T0-2 | 修复可回滚 | 修复前对 `ai_scene_config`/`sys_config`/`ai_agent`/`ai_model_config` 相关行做备份（`_bak_` 表） | 备份表存在且有数据 |

### 阶段 1 · 场景登记补全（~0.5 天 · **可独立提交**）

| 项 | 目标 | 位置 | 改动要点 | 验收 |
|---|---|---|---|---|
| T1-1 | 枚举补 `default_chat` | `AiSceneEnum.java` | 增 `DEFAULT_CHAT("default_chat","智能体对话", <能力>, <输入>, <输出>)`（元数据与 `ChatController` 语义一致） | `/cms/ai/scene/registry` 返回 11 项；`default_chat` 行在管理端可编辑保存 |
| T1-2 | 子任务常量补全 + 清裸字面量 | `AiSceneTasks.java`、`PortalJobTemplateServiceImpl.java:185` | 增 `QUESTION_JD_KEYWORDS = "jd_keywords"`；`:185` 的 `"jd_keywords"` 改引用常量 | `grep '"jd_keywords"'` 仅剩常量定义处 |
| T1-3 | 校验两段式 | `AiSceneConfigController.validate:233-257` | 先按 `:` 切分主段/子段；`main ∈ AiSceneEnum`；有子段时校验 `task ∈ AiSceneTasks` 常量集合；错误信息列出合法主场景 + 合法子任务 | 新增单测 `AiSceneConfigValidateTest`：`voice_interview:warmup` ✅、`voice_interview:whatever` ❌、`unknown_scene` ❌、`default_chat` ✅ |
| T1-4 | UI 双下拉 | `moyun-admin-vue/src/views/ai/scene/index.vue:111-125` | 场景码 = 主场景 `el-select`（来自 `/registry`）+ 子任务 `el-select`（可空，来自新接口或前端常量表）；回显时拆 `main:task` | 手工回归：能新建/编辑 `voice_interview:warmup` 并保存成功；不能输入任意串 |
| T1-5 | **裸字面量守卫** | 新增 `moyun-server/src/test/java/com/moyun/SceneCodeLiteralGuardTest.java` | 扫描 `src/main/java` 中 `executeForJson("…")`/`setSceneCode("…")`/`put("task", "…")` 的**字符串字面量**调用；注释先剥离；白名单 + 理由 + 失效自检 + **正控断言**（扫描量 > 0） | **先红**（应命中 `default_chat`、`jd_keywords` 两处）→ T1-1/T1-2 后**转绿**；`mvn -o test -Dtest=SceneCodeLiteralGuardTest` |
| T1-6 | 死模板字段标注 | 场景页表单（`system_prompt_template`）、面试配置页（`promptTemplate`） | 标注"已废弃/不生效"并置灰或隐藏（后端字段保留） | 页面不再可编辑死字段 |

### 阶段 2 · 解析与提示词收口（~1.5 天）

| 项 | 目标 | 位置 | 改动要点 | 验收 |
|---|---|---|---|---|
| T2-1 | 解析统一走场景 | `VoiceInterviewServiceImpl:378` | 改为 `resolveAgentForScene(resolveScene(SCENE_VOICE_INTERVIEW), null)`；`sys_config` 仅作兜底（保留键，逻辑降级）；异常文案指向"场景配置" | 手测：清空 `sys_config` 键后仍能从场景解析出面试官；场景未绑定时按约定报错或兜底 |
| T2-2 | 子场景继承 | `ai_scene_config`（4 行） | 三个子行 `agent_id` 置空（继承父行）；父行保留 agent | 预热/分析/自介人设与对话链**同一个 agent** |
| T2-3 | 对话链纳入场景模板 | `VoiceInterviewServiceImpl.buildInterviewerSystemPrompt:490+`；`ai_scene_config` | 新增/启用对话模板（`voice_interview` 或 `:dialog`），段序约束/计划渲染改占位符变量；业务只传变量 | 改场景模板后**正式问答口吻随之变化** |
| T2-4 | 占位符校验 | `AiSceneConfigController`（JSON 归一化处） | 校验模板 `{{}}` 与 `prompt_placeholders` 一致；未知占位符拒绝 | 单测：未知占位符保存失败、已知通过 |

### 阶段 3 · 会话快照 + 上下文/评分收口（~2.5 天）

| 项 | 目标 | 位置 | 改动要点 | 验收 |
|---|---|---|---|---|
| T3-1 | 快照列 | DDL + `increment-sql/`（幂等脚本） | `portal_voice_interview` 增 `scene_code`/`scene_version`/`model_config_id`/`agent_snapshot`/`rule_snapshot`；死列清理另评 | 脚本连跑两次 exit 0；`information_schema` 复核 |
| T3-2 | 写快照 + 全链读快照 | `VoiceInterviewServiceImpl`（start/分析/自介/报告） | start 落快照；旁路链改读快照（`AiSceneJsonClient` 增带 sessionId 的重载以纳入版本锁） | 一场面试 `agentId/model/sceneVersion` 全程一致；执行日志可查 |
| T3-3 | 统一上下文装配器 | 新增 `InterviewContextAssembler` | 四处调用共用一份上下文规格；**分析链加入最近 N 轮** | 单测：四处上下文片段一致；分析链含历史 |
| T3-4 | 评分收口 | `ScoringEngine`/`parseAnalysis`/`enhanceReportByAgent`/`VoiceInterviewReportVO` | 8 维统一（含 tone、sentimentStability）；自介并入维度子集；**总分单点计算** | 单测：总分只由聚合产生；`regenerate-report` 重算同分 |
| T3-5 | 报告补信息 | `VoiceInterviewReportVO` + 报告页 | 增 interviewer（agent 名/模型/场景版本）；知识点↔题目关联 | 报告页可见；接口字段齐全 |

### 阶段 4 · 规则归位（~1 天）

| 项 | 目标 | 位置 | 改动要点 | 验收 |
|---|---|---|---|---|
| T4-1 | 规则表收窄 | `portal_interview_config`（或新建 `portal_interview_rule`） | 只保留有消费证据的字段（`scoringWeights` + 流程开关）；其余 `@Deprecated` | 表单字段与后端消费点一一对应 |
| T4-2 | 死字段下线 | 管理端表单 + 实体注释 | 7 个死字段从表单移除（列保留） | 页面无"假开关" |
| T4-3 | （可选）启用规则 | 对话链消费点 | 若启用题数/追问上限/自介开关，需补消费代码 | 另立批次，单独验收 |

### 8.1 明天开工顺序（建议）

```
T0-1/T0-2（数据，先让面试能开）
   └─> 阶段 1（T1-1 → T1-2 → T1-5 红 → T1-3/T1-4/T1-6 → T1-5 绿）  ← 可独立提交
          └─> 阶段 2（T2-1/T2-2 可并行 T2-3/T2-4）
                 └─> 阶段 3（T3-1 → T3-2 → T3-3 → T3-4 → T3-5）
                        └─> 阶段 4
```

### 8.2 提交切分与四同步

| 提交 | 内容 |
|---|---|
| commit-1 | 阶段 0 数据修复说明（SQL 记录）+ 阶段 1（枚举/常量/校验/UI/守卫） |
| commit-2 | 阶段 2（解析统一 + 对话模板纳入 + 占位符校验） |
| commit-3 | 阶段 3（快照 + 装配器 + 评分收口 + 报告字段） |
| commit-4 | 阶段 4（规则归位 + 死字段下线） |

**每批必做四同步**：`docs/07-变更日志/devlog.md`（版本条目）、本报告（附录：改动/证据/红绿）、`docs/02-开发指南/项目开发规范.md`（新契约入规范：场景登记契约、提示词契约、上下文/评分契约）、`docs/06-规划路线/00-项目现状总结.md`（开发铁律新增条目）。

---

## 9. 附录

### 9.1 证据索引（关键 `file:line`）

| 主题 | 位置 |
|---|---|
| 前端入参（无 scene/agent） | `moyun-portal/src/pages/interview/VoiceInterviewPage.vue:1488-1495` |
| start 解析 agent（sys_config） | `VoiceInterviewServiceImpl.java:374,378-381` |
| 预热 input / 调用 | 同上 `:632-661`、`:393`、`:201` |
| 建会话+快照+记忆播种 | 同上 `:410,428,435,473-482` |
| 对话链命令与流式 | 同上 `:762-784`；`AiGatewayService.java:362,375,421-433,524-556` |
| 逐题分析上下文 | 同上 `:178-211` |
| 自介评分 | `ScoringEngine.java:61-100` |
| 报告 LLM（硬编码提示词） | `VoiceInterviewServiceImpl.java:1591-1660`（`:1631-1642`） |
| 报告信息装配 | 同上 `:1406-1471` |
| 记忆装配顺序 | `ContextManager.java:27,64-87` |
| 场景解析/Handler 回落 | `AiSceneRegistry.java:48,69-103,158-160,186-226` |
| **场景码登记与校验** | `AiSceneEnum.java:30-75,105-107`；`AiSceneTasks.java:29-46`；`AiSceneConfigController.java:95,132,233-257`；`ChatController.java:33,157`；`PortalJobTemplateServiceImpl.java:44,185`；`DefaultSceneExecutor.java:44`；`PortalAiController.java:128-132`；`IntentClassifier.java:37-82` |
| 提示词装配 | `AbstractAiSceneHandler.java:310-323,338-339`；`DefaultSceneExecutor.java:77,122-125,133-134`；`AiGatewayService.java:588-611` |
| Agent 客户端解析链 | `InterviewAgentClientImpl.java:35,60-120` |
| 计费/会员 | `PortalVoiceInterviewController.java:70` |

### 9.2 复现命令

```powershell
# 一行体检：三条链的悬空引用 + 默认模型二义 + 关键行数
mysql -uroot -D moyun-db -e "
SELECT s.scene_code, s.agent_id, a.id matched FROM ai_scene_config s LEFT JOIN ai_agent a ON a.id=s.agent_id WHERE s.scene_code LIKE 'voice_interview%';
SELECT c.config_key, c.config_value, a.id matched FROM sys_config c LEFT JOIN ai_agent a ON a.id=CAST(c.config_value AS UNSIGNED) WHERE c.config_key LIKE 'voice.interview%';
SELECT a.id, a.model_config_id, m.id matched FROM ai_agent a LEFT JOIN ai_model_config m ON m.id=a.model_config_id;
SELECT id,name,is_default FROM ai_model_config WHERE is_default=1;
SELECT (SELECT COUNT(*) FROM ai_scene_config) scene_rows, (SELECT COUNT(*) FROM ai_scene_config_history) history_rows,
       (SELECT COUNT(*) FROM portal_voice_interview) interviews;"
```

### 9.3 管理端入口与现状对照

| 入口 | 路径 | 现状 |
|---|---|---|
| 场景配置 | 菜单 `/ai/ai-config/scene` | 注册表**只读**（枚举驱动）；DB 20 行；子场景/`default_chat` **编辑必失败** |
| 智能体管理 | 菜单 `/ai/agent` | 页面提示"启用状态的智能体可被语音面试官绑定为人设" |
| **面试配置** | 菜单链：门户管理(`/portal`) → 面试管理(`/portal/interview`) → 面试配置（menu_id 32，`path=interviewConfig`，`component=cms/interview/interviewConfig/index`，权限 `cms:interview:config:list`）→ **`/portal/interview/interviewConfig`** | 18 列 / 9 表单字段，**7 个死** |
| 参考：简历模板 | 门户管理 → 简历管理(`/portal/resume`) → 简历模板（menu_id 25）→ `/portal/resume/template` | 与本报告无关，仅说明菜单 URL 拼装规则 |
| 参数设置 | `/system/config` | `voice.interview.*`（agent 悬空、时长 20 分钟） |
| 门户面试页 | `/interview/voice` | 5 步准备 → `start`（当前必失败） |

---

**（本报告为分析 + 开发指导稿，代码未改。按 §8 清单开工，每阶段完成后四同步；若 §7 决策点有异议，先定 B1~B6（场景登记）——它是阶段 1 的全部内容。）**
