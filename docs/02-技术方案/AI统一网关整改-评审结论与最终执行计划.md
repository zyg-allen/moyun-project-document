# AI 统一网关整改：评审结论与最终执行计划

> 版本：v3.1（在 v3.0 基础上吸收二轮评审 5 处细化 + 2 处顺序调整 + 3 处遗漏补齐）
> 日期：2026-09-24
> 前置文档：`docs/02-技术方案/AI 统一网关：现状、缺陷、整改方案（完整版）-v2.md`（下称"v2 方案"）
> 参考事实：`docs/01-架构设计/AI底座与调用场景链路导读.md`、`docs/09-临时报告/AI统一网关架构复杂度评估.md`
> 性质：评审结论 + 修正后的执行计划。**执行以本文档为准。**
> v3.1 变更：① 阶段一补 per-agent 流式落地设计（已随实现定稿）；② 阶段二拆分 2A（基础设施）/2B（业务迁移）两个子阶段；③ task 拆行 scene_code 五处定义裁定；④ 配置回滚与会话一致性规则；⑤ prompt 迁移验证样本量分级；⑥ 包重命名前置为 2A 独立提交；⑦ 影响面裁定（AiSceneJsonClient / AiSceneEnum / 多模态现状）。

---

## 第一部分：对 v2 方案的评审结论

### 1.1 总体判定

| 项 | 判定 |
|---|---|
| 方向（收口、简化、配置驱动） | ✅ 正确，维持 |
| 三层参数模型（Envelope/Payload/Metadata） | ✅ 正确，维持（类名需调整，见 1.3-⑥） |
| 阶段划分与"每阶段独立验证" | ✅ 正确，维持框架 |
| "11 Handler → 0 Handler" | ✅ **目标维持 0**（2026-09-24 用户裁决：业务封装下沉各自 Service，Service 与 Handler 功能重合，做正确的事值得）；执行约束见 1.2-① |
| 阶段三 Adapter | ❌ **砍掉**，见 1.2-③ |
| 阶段四性能（P1） | ⚠️ **降级为 P2 且后移**，见 1.2-④ |
| 阶段五成本指标 | ⚠️ **先基线后目标**，见 1.2-⑤ |
| 四同步（SQL/菜单/文档） | ❌ **v2 完全缺失**，本文档补齐 |

### 1.2 不足清单与修正决策

**① "删 Handler → 0"：目标成立（用户裁决），v2 缺的是落地设计，本文档补齐**

**裁决依据（2026-09-24 用户确认）**：Service 与 Handler 功能重合——业务 Service 已在组装数据（评分明细、JD+简历全文），Handler 又做一遍模板组装，一个场景的完整逻辑被切在两个包里。正确分层：**公共逻辑放网关、场景差异放配置、业务上下文放各自的 Service**。任务虽大，但做正确的事值得。

**代码证据（比裁决更有力的支撑）**：`FinanceAnalysisHandler` 位于网关包 `ext.aiapp`，却 import 了 `com.moyun.ledger.mapper.*` 5 个业务 Mapper 和 `com.moyun.portal.service.IPortalUserService`——**网关层反向依赖业务层，依赖方向倒置**。这不是"保留 Handler 的理由"，恰恰是"删 Handler 的理由"：业务逻辑长进了网关层才是病，切回 Service 是治。

**v2 的真实缺口（本文档补齐的落地设计）**：
- task 子任务表达：v2 全文未回答"一场景多 task"。补齐方案：ai_scene_config 按 task 拆配置行，场景码约定 `resume_optimize:advice` / `voice_interview:warmup` 格式，AiSceneRegistry 路由时支持冒号拆分（先按全码查，未命中回退主场景码）；
- FinanceAnalysis 迁移路径：查数 + 指标计算逻辑**下沉回 `LedgerAiAnalysisServiceImpl`**，Service 组装完整数据 Payload → 网关只做模板渲染 + LLM + 解析。顺带修正 aiapp → ledger/portal 反向依赖（依赖恢复为 业务 → 网关 单向）；
- VoiceInterviewHandler 迁移路径：阶段一会话流式落地后，主干走网关，3 个 task 拆行走 DefaultSceneExecutor；
- `AbstractAiSceneHandler` 已沉淀的骨架（chatJson 解析失败重试、mergePersona、wrapData、parseOutput）**迁入 DefaultSceneExecutor**（迁移而非重写）。

**执行约束（不改变 0 目标，只保障落地不翻车）**：
- `AiSceneHandler` **SPI 接口保留**（删 11 个实现，留接口）——万一某场景 output_schema 表达力不足，还有零成本扩展点；
- 逐场景迁移验证（先 3 个简单场景试点），不做一次性大爆炸；
- 配置版本化必须前置（见 2.1）——prompt 全量进配置前先有回滚安全网。

**② 违反四同步铁律（代码/文档/SQL/菜单）**

v2 改动清单只有 Java 文件。实际还必须同步：
- SQL：ai_scene_config 加列（ALTER TABLE 追加在 DDL 文件对应模块末尾，不改动原 CREATE TABLE）；
- DML：9 个待迁移场景的 user_prompt_template 逐字收编 UPDATE、task 拆行的 INSERT、init-sql 同步；
- 菜单：场景配置管理页若增加新字段展示，sys_menu 无需动（复用现有页面），但管理端 Vue 页面需同步加列编辑；
- 文档：导读文档 §7 的调用链同步更新。

**③ 阶段三 Adapter 砍掉**

理由：管理端测试调的是资源层原生接口（`DynamicChatService` / `DiagramChatService`），**根本不经过网关**——"管理端零改动"由"两套入口、一个资源层"的架构本身保证，不需要 Adapter。为不存在的问题引入 AgentAdapter/WorkflowAdapter/ModelAdapter 三层新抽象，与"简化"原则矛盾。
执行器到资源层的适配继续走现有 `AiSceneResolver` → `ModelConfigService` / `AgentService` 链路（已被验证）。
v2 的 6.3 理由（"Adapter 只在网关侧，不侵入"）描述的是资源层不被侵入的现状，而非需要新建的东西。

**④ 阶段四性能优化降级后移**

- 项目当前处于**开发设计阶段，无生产流量**——"QPS≥50、P99≤3s"无验收意义，且 D16 自己承认"无压测基线"，先优化后压测是倒置；
- **"同场景 100 次调用 DB≤1 次"直接推翻现有"直查库即时生效"设计决策**：Caffeine 本地缓存 + 失效通知在多实例下存在一致性窗口，管理端改配置可能出现"部分实例旧配置"的排障噩梦。相对秒级 LLM 调用，一次索引查询（<5ms）的开销可忽略——**维持现状不缓存是合理的性能取舍**，v2 未明示这是拿即时生效换性能；
- 保留项：治理链 Redis 合并（Lua/批量）与摘要异步化在阶段一顺手做（会话链路本身要动）；配置缓存、多模态并行化移入"生产前置阶段"，待有真实流量与压测基线后再做。

**⑤ 阶段五成本控制改为"先基线、后目标"**

- 先在 ai_execute_log 上跑统计（tokenUsed 已落库），得各场景真实基线，再定缩减目标；
- **面试场景红线**：滑窗/摘要可能丢失追问线索，影响面试体验（追问质量依赖完整上下文）。面试场景默认 context_strategy=full，只做输出侧 max_output_tokens 限制；缩减目标只对财务分析/简历解析等任务型场景设定。

**⑥ 其他技术修正**

| # | v2 问题 | 修正 |
|---|---|---|
| a | `AiMetadata` 类名与现有 `aiapp/model/AiMetadata`（响应元数据：tokenUsed/modelUsed）冲突 | 配置侧改名 `AiSceneMetadata` |
| b | `AiSceneMetadata` 缺 output_mode、open_api、fallback_model_id 字段 | 补齐（网关入口校验依赖前两者） |
| c | SceneExecutor 伪代码无 IntentClassifier 步骤，未交代多轮会话下的意图分类 | 明确：sessionId 非空（会话模式）时**跳过意图分类**（会话已绑定场景，分类是多余且有误打断风险）；单次调用保留 |
| d | 改动清单缺 AiExecuteRequest/AiSceneEnum/测试迁移/AiSceneJsonClient 影响面 | 本文第三部分补齐 |
| e | 回滚方案缺失；配置版本管理（7.4）排在 prompt 大规模进配置之后，顺序倒置 | 版本管理前置到阶段二第一步（见执行计划） |
| f | 面试迁移"延迟不增加"无测量定义 | 定义为：SSE 首字节 P50 不劣化超过 10%，P99 不劣化超过 15%，以 ai_execute_log.elapsedMs + 前端埋点双口径测量 |

---

## 第二部分：修正后的分阶段计划

> 原则：P0 只有两件事——收口会话流式（净删代码）、简单场景泛化（减少 Handler）。其余后移或砍掉。
> 每阶段结束必须满足四同步 + 回滚预案 + 独立验证三条件才进入下一阶段。

### 阶段一：会话型流式 + 面试收口（P0，约 2-3 周）

**目标**：网关支持多轮+记忆+per-agent 流式；面试主干迁移进网关；删除灰度开关与治理前置。

| # | 动作 | 文件 | 类型 |
|---|---|---|---|
| 1.1 | AiExecuteRequest 加 sessionId/sessionMode 字段 | aiapp/model/AiExecuteRequest.java | 修改 |
| 1.2 | 新增 ChatMemoryProvider 接口 + RedisChatMemoryProvider 实现 | aiapp/support/（新增 2 文件） | 新增 |
| 1.3 | 新增 ContextManager（滑窗读取 + 记忆拼接；**摘要异步预生成**，不阻塞当前轮） | aiapp/support/ContextManager.java | 新增 |
| 1.4 | AiGatewayService 新增会话流式通道 executeConversationStream：sessionId 非空跳过意图分类，ContextManager 注入历史消息，模型走 agent 绑定链 | aiapp/service/AiGatewayService.java | 修改 |
| 1.5 | 消息列表入参落地（`List<ChatMessage>` 直传流式模型；实现于网关会话通道，Handler 基类不加无消费者的重载） | aiapp/service/AiGatewayService.java | 修改 |
| 1.6 | InterviewChatMemoryService 适配 ChatMemoryProvider（滑窗口径不变） | ext/cms/service/interview/（适配层） | 修改 |
| 1.7 | 面试主干 LLM 调用收口网关 executeConversationStream（**SSE 事件协议 delta/end/error 由业务侧组装，前端零改动**；Controller→Service→网关 分层，业务编排留在 Service） | ext/cms VoiceInterviewServiceImpl | 修改 |
| 1.8 | 删除灰度开关 ai.gateway.interview.enabled、治理前置、直连分支、同步模拟流式兜底 | InterviewAgentClientImpl 等 | 删除 |
| 1.9 | SSE 完成回调补 TokenCostGuard.consume（onCompleteResponse 提取 tokenUsage 汇总） | AiGatewayService.executeConversationStream | 修改 |

**per-agent 流式落地设计（2026-09-24 已实现，落定四个问题）**：

| 问题 | 落地结论 |
|---|---|
| Agent 绑定模型怎么解析 | **不走 AiSceneResolver**（那是"场景绑定"链）。新增 `aiapp/support/AgentModelRouter`（模型路由公共能力，从 InterviewAgentClientImpl 迁入）：`agent.modelConfigId` → 默认 chat 配置，两级回退；同步/流式统一收口 |
| StreamingChatLanguageModel 怎么创建和缓存 | `ModelConfigService.createStreamingChatModel(configId, temperature, maxTokens)`——底座已有 `streamingModelClientCache`（ConcurrentHashMap，key 含 temperature/maxTokens 覆盖参数），配置变更主动失效，网关不另建缓存。绑定模型不支持流式时自动挑选「启用+chat+支持流式」配置（默认优先，其余按 id 升序） |
| SSE 事件协议怎么保持与面试主干一致 | 面试协议实为 `delta`/`end`/`error`（end 携带 nextQaId/roundDone 等业务载荷）。网关采用**回调式通道**（onToken/onComplete/onError 三个 Consumer），SSE 事件名与载荷由业务 Service 组装——公共治理收口网关、业务编排留在 Service（符合"业务上下文放各自 Service"裁决），前端零改动 |
| 会话记忆和历史消息怎么注入 | `ContextManager.buildTurnMessages(sessionId, maxMessages, userInput, directives)`：当前输入先入滑窗（LLM 失败不丢用户消息，与既有行为一致）→ 读滑窗（MessageWindowChatMemory 口径：SystemMessage 永驻、淘汰最旧非系统消息）→ 早期摘要注入（首条 SystemMessage 之后）→ 瞬态指令尾插（如每轮话术风格约束，不入滑窗）。成功后 `recordAiReply` 追加 AI 回复，超窗时异步预生成摘要 |

**公共组件抽象原则（2026-09-24 用户确认：简化、统一、职责分明）**：
- **有第二实现需求或依赖隔离需求才抽接口，否则具体类**——过早接口化是负资产；
- 仅 `ChatMemoryProvider` 抽接口（网关不直接依赖 LangChain4j ChatMemoryStore 类型 + 统一面试滑窗与通用会话两种读取口径）；
- 降级 / 成本控制 / 限流 / 语义缓存**均不做适配器**：单一实现策略直接 @Component；降级兜底文案数据化（迁入配置行 fallback_response）而非代码化；限流/熔断/缓存将在阶段四合并为 Lua Pipeline，现在抽接口等于改两次；
- 记忆端口复用约定：`ChatMemoryProvider` 是网关侧窄端口，`RedisChatMemoryProvider` **复用底座 `RedisChatMemoryStore` 的存储结构 / 共享同一 Redis key 规范**（该 Store 服务管理端 DynamicChatService），不另起炉灶、不出现两套记忆抽象。

**基线采集（v3.1 补，验收前置步骤）**：
- SSE 首字节基线在**回滚分支**（保留直连实现的 commit）上跑 20 轮采集，记录 P50/P99 存 `docs/perf/baseline.md`，再切回主干跑网关通道 20 轮对比；
- 双口径：`ai_execute_log.elapsedMs`（网关侧）+ 前端埋点首字节时间戳（直连无网关日志，以前端口径为准）。

**验收**：
- 面试全流程（预热→问答→报告）功能回归通过；
- SSE 首字节 P50 劣化 ≤10%、P99 ≤15%（对照迁移前后各 20 轮，基线采集见上）；
- 删除的灰度/治理前置/直连/模拟流式代码（InterviewAgentClientImpl 456→约 170 行 + SQL）与新增公共组件（Provider/ContextManager/AgentModelRouter/会话通道）折算后职责收敛：三套重复治理逻辑 → 网关一套；
- 灰度开关从 sys_config 移除（DML 种子行删除 + 已部署库 DELETE）。

**回滚**：InterviewAgentClientImpl 直连分支在单独分支保留一个版本周期，网关会话流式异常时按 commit 回退（实现回滚=进程整体回退，进行中会话随进程结束自然终止，无一致性问题）。

**回滚后会话一致性（v3.1 补）**——分两类，规则不同：
- **实现回滚（阶段一）**：commit 回退 + 重启进程，进行中面试会话按旧逻辑继续到结束，无跨版本混跑；
- **配置回滚（阶段二版本化后）**：**回滚只影响新会话，进行中会话锁定起始版本**。实现：会话首轮将 `config_version` 写入会话元数据（Redis `chat:memory:session:{sessionId}`），网关会话通道每轮按锁定版本从 `ai_scene_config_history` 快照读配置；会话结束/过期后元数据清除，新会话读当前版本。单次调用（无 sessionId）不受影响，始终读当前版本。**不采用**"每轮直查当前配置"——那会导致第 N 轮 prompt 与前 N-1 轮对话上下文风格断裂（如面试进行到第 10 轮回滚 prompt，第 11 轮新 prompt 与前 10 轮旧 prompt 生成的历史混跑）。

### 阶段二：场景配置版本化 + 简单场景泛化（P0，约 2-3 周，v3.1 拆分为 2A/2B 两个子阶段）

**目标**：先立配置安全网（2A 基础设施），再做业务迁移（2B）；**11 个 Handler 全部删除（目标 0 实现，SPI 接口保留）**，业务上下文下沉各自 Service。

**拆分理由（v3.1 裁定）**：版本化/包重命名/测试改造是基础设施（无功能影响），task 拆行/场景迁移/查数下沉是业务迁移（有回归风险）——混在一起出问题时难定位是重命名导致还是迁移导致。2A 验收通过后才进 2B。

#### 子阶段 2A：基础设施（版本化 + 包重命名 + 执行器，无功能影响）

| # | 动作 | 说明 |
|---|---|---|
| 2A.1 | **ai_scene_config 配置版本化（前置安全网）** | 新增 config_version 字段 + ai_scene_config_history 快照表（DDL 追加 ALTER）；管理端保存时自动快照，支持一键回滚上一版本。**此步必须先于任何 prompt 迁移**。回滚与会话一致性规则见阶段一"回滚后会话一致性"（会话锁定起始版本） |
| 2A.2 | **包重命名（独立提交，先于场景迁移）** | `com.moyun.ext.aiapp` → `com.moyun.ext.aigateway`（2026-09-24 用户定名）；测试包 `ext/ai2` → `ext/aigateway`；日志前缀 `[ai2:]` → `[aigateway:]`。**单独一个 commit，重命名后先跑一轮全量回归（编译+测试+面试主干冒烟），确认无功能影响，再做场景迁移** |
| 2A.3 | 定义 AiSceneMetadata（改名修正 ⑥-a）+ DefaultSceneExecutor | 配置驱动执行：模板渲染 → LLM → output_schema 解析 → 兜底，全链路复用 AbstractAiSceneHandler 已验证的骨架方法（迁入执行器，非重写） |
| 2A.4 | task 拆行基础设施 | 场景码 `scene:task` 路由支持（定义见下表）；AiSceneRegistry 冒号拆分回退 |

**task 拆行 scene_code 定义（v3.1 裁定，五处口径）**：

| 位置 | 值 | 说明 |
|---|---|---|
| ai_scene_config.scene_code | `resume_optimize:advice`（**全码**） | task 拆行，一行一个 task |
| AiSceneEnum | `RESUME_OPTIMIZE`（**主场景**） | 枚举只保留主场景值；task 用字符串常量（如 `AiSceneTasks.RESUME_OPTIMIZE_ADVICE = "advice"`），不膨胀枚举 |
| 业务调用 | `resume_optimize` + `input.task=advice` | **兼容现有调用方式**，AiSceneJsonClient 签名不变（见"影响面裁定"） |
| AiSceneRegistry 路由 | 先按全码查，未命中回退主码 | 冒号拆分：`scene:task` 全码 → 主码 |
| ai_execute_log.scene_code | **全码** | 便于按 task 维度统计成本（阶段三基线依赖） |

**2A 验收**：全量编译+测试通过；面试主干冒烟 + 3 个简单场景冒烟无功能影响；管理端配置页版本查看/回滚操作可用。

#### 子阶段 2B：业务迁移（task 拆行 + 场景泛化 + 查数下沉）

| # | 动作 | 说明 |
|---|---|---|
| 2B.1 | task 拆行 DML | 8 个 task 拆行 INSERT（prompt 从 Handler 逐字迁移）；init-sql 同步 |
| 2B.2 | 迁移 3 个简单场景验证 | daily_topic / writing_prompt / content_tags（输出结构简单、无业务查数）→ 删除对应 3 个 Handler |
| 2B.3 | 迁移剩余 6 个简单场景 | resume_parse / resume_optimize(4 task 已拆行) / question_generate / article_meta / sensitive_word / knowledge_qa → 删 6 个 Handler |
| 2B.4 | FinanceAnalysis 查数下沉 | 查数 + 指标计算从 Handler 下沉回 `LedgerAiAnalysisServiceImpl`（Service 组装完整数据 Payload），删除 FinanceAnalysisHandler；**顺带修正 aigateway → ledger/portal 反向依赖** |
| 2B.5 | VoiceInterview 拆行迁移 | 3 个 task（warmup/answer_analysis/self_intro）拆行走 DefaultSceneExecutor（主干已在阶段一走网关会话流式），删除 VoiceInterviewHandler |
| 2B.6 | AiSceneEnum 收敛 | 枚举保留（主场景编码唯一权威来源不变）；新增场景一律 = 枚举 + 配置行 + Service 组装，不再写 Handler；`AiSceneHandler` SPI 接口保留作为逃生舱扩展点 |

**四同步清单（本阶段强制）**：
- SQL：DDL 追加（config_version 列 + history 表）；DML：task 拆行 INSERT、11 个场景 prompt UPDATE、init-sql 同步；
- 菜单：管理端场景配置页（/cms/ai/scene）增加版本查看/回滚操作，sys_menu 增量 UPDATE；
- 文档：导读文档 §3/§7 更新（Handler 清单、task 拆行后的调用方式）；
- 测试：`ext/ai2` 测试包随 2A.2 重命名同步改造。

**验收**：
- 12 个场景（11 + interview 主干）全部功能回归；
- Handler 数量 11 → **0**（SPI 接口保留）；新增场景 = 枚举 + 配置行 + Service 组装（不再写 Handler）；
- **prompt 迁移验证样本量分级（v3.1 裁定）**：
  - 简单场景（daily_topic / writing_prompt / content_tags / sensitive_word）：各抽 5 条对比；
  - 复杂场景（resume_parse / resume_optimize / finance_analysis / question_generate / article_meta / knowledge_qa 及面试 3 task）：各抽 **20 条**，需覆盖典型输入 + 边界输入（空值/超长/特殊字符）；
  - 关键场景（面试主干）：阶段一已走网关，不在本阶段；其 3 个 task 迁移期间**保留旧 Handler 输出对比脚本**，新旧并行跑比对一轮；
- 配置回滚演练：改错一个 prompt → 一键恢复 → 新会话生效验证（进行中会话按锁定版本继续）。

**回滚**：2B.2/2B.3 逐场景提交，单场景异常按 commit 回退 + 配置版本回滚双保险。

### 阶段三：Token 成本基线与任务型场景缩减（P1，约 1-2 周）

| # | 动作 |
|---|---|
| 3.1 | 从 ai_execute_log 统计各场景 token 基线（近 30 天，区分 input/output） |
| 3.2 | ai_scene_config 加成本控制字段：max_input_tokens / max_output_tokens / truncate_strategy（**暂不加 context_strategy 滑窗/摘要**——会话滑窗已在阶段一 ContextManager 内置） |
| 3.3 | 任务型场景（resume_parse / finance_analysis / resume_optimize）实施输入截断 + 输出上限 |
| 3.4 | **面试场景只做 max_output_tokens**，context_strategy 固定 full（追问质量红线） |
| 3.5 | 成本看板：ai_execute_log 按 场景×日 聚合（管理端复用执行日志页加聚合视图） |

**验收**：目标在 3.1 基线出来后设定（示例：任务型场景 input token 减 40-60%），不拍脑袋先定数。

### 阶段四：治理链合并 + 卫生清理（P1，约 1 周）

| # | 动作 | 说明 |
|---|---|---|
| 4.1 | 治理链 Redis 合并 | 限流 INCR + 熔断读 + 缓存 get 合并为 Lua 脚本单次往返（3-4 次 → 1 次）；SSE 路径输出脱敏以"完整句子缓冲"方式补齐（D6 另一半） |
| 4.2 | 删 AiProperties 残留 | D4 双轨收敛 |
| 4.3 | 错误码统一复核 | AiErrorCodes 与全局 Result 错误码对齐 |
| 4.4 | PortalAiController TODO 收尾 | 薄化为仅调 AiSceneJsonClient |
| 4.5 | Prompt 变量校验 | 保存配置时校验模板占位符与场景入参匹配 |

### 阶段五：生产前置（P2，有真实流量/上线前触发，当前不排期）

触发条件后执行：压测基线（JMeter）→ 视数据决定是否做 SceneConfig 本地缓存（明示牺牲即时生效 + 失效通知方案）→ 多模态 ContentPart 抽象（AudioPart/FilePart/ImagePart，语音面试/简历/证件三场景迁移）→ QPS/P99 指标设定。

**明确不做**（v2 阶段三）：AgentAdapter / WorkflowAdapter / ModelAdapter——资源层适配已由 AiSceneResolver 承担，管理端独立调用由架构分层天然保证。

---

## 补充：影响面裁定（v3.1 补，二轮评审遗漏项）

### ① AiSceneJsonClient 保持不变

**业务标准入口地位不变**：签名与行为零改动，继续作为业务侧调用网关的首选门面（`executeForJson` / `executeForText`）。task 拆行后业务仍传主场景码 + `input.task`，由 AiSceneRegistry 冒号路由消化——对业务调用方完全透明。2B 删除 11 个 Handler 后，AiSceneJsonClient 内部路由自动落到 DefaultSceneExecutor，调用方无感。

### ② AiSceneEnum 扩展规则

**主场景一个枚举值，task 用字符串常量**（定义见阶段二 task 拆行表）：
- 新增**场景** = AiSceneEnum 加一个主场景枚举值 + ai_scene_config 插配置行 + 业务 Service 组装上下文；
- 新增**task** = 现有主场景枚举值不变 + ai_scene_config 插 `scene:task` 拆行 + `AiSceneTasks` 常量类加常量；
- 枚举不随 task 膨胀（避免 11 场景 × 多 task 的枚举爆炸）。

### ③ 多模态 ContentPart：维持现状声明（避免"半抽象"状态）

项目当前已在用声音（面试 ASR）和文件（简历 PDF 解析），但均为**业务侧自理**：业务 Service 调 ASR/解析服务拿到文本，走 userInput/input 通道进网关——网关只处理文本，现状能工作。
**裁定**：P2 触发前**不做任何改动、不做部分抽象**（半抽象比不抽象更糟：两套调用方式并存）。P2 触发时一次性迁移到 ContentPart（AudioPart/FilePart/ImagePart），三场景（语音面试/简历/证件）统一收口。阶段一~四不为此预留任何代码。

---

## 第三部分：修正后改动清单

| # | 文件/对象 | 类型 | 阶段 |
|---|---|---|---|
| 1 | aiapp/model/AiExecuteRequest.java | 修改（+sessionId/sessionMode） | 一 |
| 2 | aiapp/support/ChatMemoryProvider.java + RedisChatMemoryProvider.java | 新增 | 一 |
| 3 | aiapp/support/ContextManager.java | 新增 | 一 |
| 4 | aiapp/support/AgentModelRouter.java + model/ConversationStreamCommand.java | 新增 | 一 |
| 5 | aiapp/service/AiGatewayService.java 会话流式通道（消息列表直传+Token 汇总） | 修改+新增 | 一 |
| 6 | ext/cms InterviewAgentClientImpl 直连分支/灰度开关/治理前置 | 删除 | 一 |
| 7 | DDL：ai_scene_config 加 config_version + history 表（追加 ALTER） | SQL | 二（2A） |
| 8 | DML：8 个 task 拆行 + 11 场景 prompt 迁移 + init-sql 同步 | SQL | 二（2B） |
| 9 | sys_menu：场景配置页加版本操作（增量 UPDATE） | SQL | 二（2A） |
| 10 | aiapp/model/AiSceneMetadata.java + service/DefaultSceneExecutor.java | 新增 | 二（2A） |
| 11 | aiapp/registry/AiSceneRegistry.java | 修改（冒号路由） | 二（2A） |
| 12 | 11 个场景 Handler 实现 | 删除（SPI 接口保留） | 二（2B） |
| 13 | FinanceAnalysis 查数逻辑 → LedgerAiAnalysisServiceImpl（修正反向依赖） | 迁移 | 二（2B） |
| 14 | 管理端 Vue 场景配置页（版本/回滚/task 拆行编辑） | 修改 | 二 |
| 15 | **主代码包 com.moyun.ext.aiapp → com.moyun.ext.aigateway；测试包 ext/ai2 → ext/aigateway（独立提交）** | 重命名 | 二（2A.2） |
| 16 | DDL：ai_scene_config 加 max_input_tokens/max_output_tokens/truncate_strategy | SQL | 三 |
| 17 | ai_execute_log 成本聚合视图 + 管理端看板 | 修改 | 三 |
| 18 | 治理链 Lua 合并脚本 + SSE 脱敏缓冲 | 修改 | 四 |
| 19 | ext/cms/config/AiProperties.java | 删除 | 四 |

---

## 第四部分：验收标准（修正版）

| 项 | 目标 | 备注 |
|---|---|---|
| Handler 数量 | 11 → **0**（SPI 接口保留） | 业务上下文下沉各自 Service |
| 新增场景 | 枚举 + 配置行 + Service 组装 | 不再写 Handler；output_schema 表达力不足时才用 SPI |
| 包命名 | ext.aiapp / ext.ai2 → **ext.aigateway** | 主代码/测试/日志前缀一次统一 |
| 面试 SSE 首字节 | P50 劣化 ≤10%，P99 ≤15% | 迁移前后各 20 轮实测 |
| 代码量 | 阶段一、二合计**净减少** | 含删除的直连/灰度/11 个 Handler |
| 配置回滚 | 一键恢复上一版本并生效 | 阶段二 2.1 交付 |
| Redis 往返 | 治理链 ≤1 次（Lua 合并） | 阶段四交付；**不做配置本地缓存**（维持即时生效） |
| Token 缩减 | 基线统计后按场景设定 | 面试仅限输出侧 |
| 管理端独立调用 | 零改动 | 架构分层保证，无需 Adapter |
| 四同步 | 每阶段 SQL/文档/菜单与代码同 commit | 项目铁律 |

---

## 第五部分：与 v2 方案的差异对照

| v2 原案 | 本文档修正 | 理由 |
|---|---|---|
| 11 Handler → 0 | **维持 0**（用户裁决），补齐落地设计：task 拆行 + 查数下沉 Service + SPI 接口保留作逃生舱 | Service 与 Handler 功能重合；且 FinanceAnalysisHandler 反向依赖 ledger/portal（网关层渗入业务层），删 Handler 同时修正依赖方向 |
| 阶段三 Adapter（3 个） | 砍掉 | 解决不存在的问题；现有 AiSceneResolver 链路已验证 |
| 阶段四性能 P1（缓存/压测/QPS50） | 降为 P2 生产前置 | 开发设计阶段无流量；配置缓存牺牲即时生效需显式决策 |
| 阶段五 token 减 75%/80%/70% | 先基线后定目标；面试禁滑窗 | 无基线的指标是拍脑袋；面试上下文完整性是体验红线 |
| 七阶段顺序（版本管理最后） | 版本管理前置为阶段二第一步 | prompt 大规模进配置前必须有回滚安全网 |
| 仅 Java 改动清单 | 补齐 SQL/DML/菜单/Vue/测试 | 四同步铁律 |
| IntentClassifier 未交代 | 会话模式跳过意图分类 | 会话已绑定场景；分类误打断风险 |

---

## 第六部分：执行顺序与停止点

```
阶段一（会话流式+面试收口）──停止点A：净删代码验证、面试回归通过
    ↓
阶段二（版本化→task拆行→场景泛化）──停止点B：12场景回归、回滚演练通过
    ↓
阶段三（成本基线与缩减）
    ↓
阶段四（治理合并+卫生清理）
    ↓
阶段五（生产前置）——上线前触发，当前不排期
```

**停止点 A、B 必须停下来整体评估后再继续**（沿用 v2 正确原则）。执行中任一验收不达标：单场景按 commit 回退 + 配置版本回滚，禁止带病进入下一阶段。
