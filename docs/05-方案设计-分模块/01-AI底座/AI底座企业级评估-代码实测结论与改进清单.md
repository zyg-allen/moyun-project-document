# AI 底座企业级评估 · 代码实测结论与改进清单

> **文档定位**：《AI 底座企业级评估标准与项目现状分析.md》的配套实测结论。原文档自述"基于文档推断，非代码实测"（基线 v11.48），本文用代码扫描逐项核实，修正误判、确认缺口，作为后续改造的执行依据。
> **实测基线**：v11.55（2026-09-11，含统一网关落地、finance_analysis 全链路、异步任务化等最新改动）
> **核实方法**：4 路并行代码扫描（模型/网关横切、Agent/工具、RAG/工作流、场景/安全/测试）+ 关键点人工复核，全部结论有文件级证据。

---

## 第一部分 · 总体结论

### 1.1 一句话结论（对照原文档 A.3）

原文档说你是"设计精良但未完工的大厦——Function Calling、语义缓存、意图识别、安全防护停留在图纸阶段"。

**实测修正：大厦比你自评的完工度高——Function Calling 链路、语义缓存、意图识别接线、密钥加密、JVector 向量库、查询改写这些都已真实存在，原文档低估了落地度；但三类硬缺口被确认且比文档描述的更聚焦：安全防护（注入/输出过滤/成本熔断）为零、测试与新链路严重脱节（12 个测试全在老包，ai2 网关/财务/评分引擎全裸奔）、"场景收口"是 1/7 而非文档暗示的设计问题。**

### 1.2 评分修正总表

| 维度 | 原评分 | 实测评分 | 主要修正依据 |
|---|---|---|---|
| 1. 模型配置 | 4.0 | **4.3** | 密钥 AES-GCM 加密已实现（原文档"待核实"项落实为已有） |
| 2. Agent 体系 | 2.6 | **3.1** | Redis 窗口记忆 + Agent 工具绑定链路实有（原文档判"无记忆组件/无工具链路"） |
| 3. 工具调用 | 1.5 | **3.2**（v11.63↑） | 最大误判：ToolRegistry/[TOOL_CALL] 执行链路完整存在，且参数 JSON Schema 执行前校验已补（P1-2）；仍非原生 FC |
| 4. 知识库/RAG | 3.8 | **4.6**（v11.65↑） | 查询改写+扩展已实现、JVector 已落地（原文档判"缺失/未执行"）；引用溯源实测修正：完整链路已存在（原文档判"仍缺"系误判，v11.64）；v11.65 knowledge_qa 场景接入统一网关（Agent 驱动多路召回+引用透出，C 端可消费） |
| 5. 工作流 | 2.8 | **3.2** | ai_workflow_execution 状态持久化实有（原文档判"缺失"） |
| 6. 场景网关 | 3.4 | **4.5**（v11.66↑） | 语义缓存/意图识别/限流/降级全部实测可用；v11.48~55 落地大幅推进；业务收口 **7/7**（v11.59 全部完成）；v11.65 新增 knowledge_qa 场景（知识问答网关化，场景数 8）；v11.66 output_parser/output_mode 配置消费闭环（此前可编辑零消费） |
| 7. 可观测性 | 2.9 | **3.7**（v11.60↑） | metadata 三列落库（v11.51）、Token 统计管理页已有；成本金额回填（v11.57 cost_yuan）；执行日志管理页（v11.60 P1-1：筛选/汇总/详情/清理） |
| 8. 安全治理 | 2.5 | **4.0**（v11.62↑） | Prompt 注入防护 + 成本熔断（日 Token 配额）+ 输出内容过滤（DFA 词树脱敏，按场景开关）全部完成；剩余短板收敛为流式路径未覆盖过滤 |
| 9. 工程化 | 4.2 | **4.4**（v11.70↑） | 测试缺口大幅收敛（ai2 80 用例：注入/成本/收口契约/评分引擎/财务护栏/基础设施全绿）；异步任务收敛落地（v11.67：双轨制选型规则 + 孤儿任务恢复 + 老包存量测试修复）；资金链路测试补齐（v11.70：打赏双链路+回调闭环 22 用例，充值/提现业务未实现无从测试，全库 308 用例全绿） |
| 10. 架构扩展 | 4.0 | **4.0** | 维持 |
| **加权综合** | **3.10** | **3.84**（v11.70↑） | |

### 1.3 修正后的定位判断

> **设计能力 ★★★★★，落地能力 ★★★★☆（原判 ★★★☆☆），安全能力 ★★★★☆（原判 ★★★☆☆，v11.57~63 注入防护+成本熔断+场景收口+测试护栏+日志管理页+输出过滤+工具 Schema 校验后回升）。**
> 项目已跨过"骨架阶段"——网关横切能力（配置/人设/缓存/限流/降级/日志/白名单）真实可跑且有生产流量（finance_analysis 全链路）。v11.59 后业务收口 7/7 全部完成（全部 LLM 直调清零，统一过网关的日志/成本/限流/注入防护体系）；v11.60 后 P0 全清（注入防护/成本熔断/场景收口/新链路测试四项全部完成，ai2 测试 80 用例全绿，且单测揪出评分引擎 2 个真 bug）；v11.61 后执行日志管理页落地（收口产生的数据有人看，可观测性闭环）；v11.62 后输出内容过滤落地（安全三件套注入/熔断/输出过滤齐备）；v11.63 后工具参数 Schema 校验闭环（chat/工作流/管理页三路径执行前收口，ai2+工具测试 107 用例全绿）；v11.64 实测修正引用溯源误判（RAG 完整引用链路自 2025-12-11 已实装，维度 4 → 4.5，综合 → 3.80）；v11.65 知识问答接入统一网关（knowledge_qa 场景：Agent 驱动多路召回+引用溯源透出，网关场景数 8，维度 4 → 4.6/维度 6 → 4.4，综合 → 3.81，ai2 测试 119 用例全绿）；v11.66 output_parser/output_mode 配置消费闭环（此前可编辑零消费，维度 6 → 4.5，综合 → 3.82，回归 128 用例全绿）；v11.67 异步任务收敛落地（实测修正："三套模式"中 JudgeAsyncWorker 系 OJ 判题专属误计入；双轨制选型规则落三处类注释 + 表驱动任务孤儿恢复 + 老包 3 个存量测试失败修复，维度 9 → 4.3，综合 → 3.83，全库 286 用例全绿）；v11.70 资金链路测试补齐（打赏双链路 22 用例：积分打赏全分支 + 微信支付下单 + 回调闭环幂等/并发竞争/复式分账金额守恒；实测澄清——充值/提现业务逻辑尚未实现，钱包 Service 仅 CRUD 无从测试；维度 9 → 4.4，综合 → 3.84，全库 308 用例全绿）。**P1 级全部清零**（知识库数据回填转运营执行，指引已交付）；当前的真实短板收敛为：**耗时分位数与错误率告警（P2）、流式路径输出过滤未覆盖（随网关多轮消息契约 P2 扩展一并处理）**。

---

## 第二部分 · 原文档误判修正（13 项实测证据）

这是本次实测最有价值的部分——原文档基于文档推断的结论，有 9 项低估、2 项高估、2 项基本准确：

### 2.1 低估项（实际比文档说的好）

| # | 原文档判断 | 实测结论 | 代码证据 |
|---|---|---|---|
| 1 | 密钥加密"待核实"★★★☆☆ | **已实现 AES-GCM**：入库前加密（`ENC:` 前缀密文），管理端接口返回前解密+脱敏，兼容历史明文 | [ApiKeyCryptoUtils](../../../moyun-server/src/main/java/com/moyun/ext/ai/util/ApiKeyCryptoUtils.java#L9) + [ModelConfigServiceImpl](../../../moyun-server/src/main/java/com/moyun/ext/ai/service/impl/ModelConfigServiceImpl.java#L441) save() 加密 + [ModelConfigController](../../../moyun-server/src/main/java/com/moyun/ext/ai/controller/ModelConfigController.java#L31) maskApiKey() |
| 2 | Function Calling"完全缺失，无注册/Schema/引擎/审计"★★☆☆☆ | **自定义协议链路完整**：`[TOOL_CALL]` 文本协议解析 → ToolRegistry 自动发现 ToolExecutor → 执行 + 异步审计日志；Agent 经 agent_tool 关联表绑定工具并注入提示词；工作流有 ToolNodeExecutor；管理端有工具管理页 | [ToolCallingService](../../../moyun-server/src/main/java/com/moyun/ext/ai/service/ToolCallingService.java#L158) 执行链 + [ToolRegistry](../../../moyun-server/src/main/java/com/moyun/ext/ai/engine/tool/ToolRegistry.java#L82) 自动注册 + views/ai/tool 页面 |
| 3 | 语义缓存"未实现" | **已实现**：精确匹配（md5）+ Embedding 余弦相似度 0.95 阈值，Embedding 模型懒加载，不可用降级为仅精确匹配 | [SemanticCache](../../../moyun-server/src/main/java/com/moyun/ext/ai2/support/SemanticCache.java#L71) |
| 4 | 意图识别"未接入网关" | **已接线**（v11.52）：顶层 userInput 字段触发 IntentClassifier，置信度<0.6 返回 clarification，高置信度动态改路由场景 | [AiGatewayService](../../../moyun-server/src/main/java/com/moyun/ext/ai2/service/AiGatewayService.java#L75) |
| 5 | 限流"是否生效待验证" | **已验证生效**：Redis INCR+EXPIRE，键 `ai2:rate:{scene}:{identity}`，同步与流式双路径都过 | [SceneRateLimiter](../../../moyun-server/src/main/java/com/moyun/ext/ai2/support/SceneRateLimiter.java#L41) |
| 6 | 降级策略"字段有，逻辑未见" | **已实现**：场景配置 fallback_response 优先，否则按场景内置兜底 JSON/文本 | [FallbackStrategy](../../../moyun-server/src/main/java/com/moyun/ext/ai2/support/FallbackStrategy.java#L29) |
| 7 | RAG 查询优化"缺失（无改写/HyDE）" | **查询改写+查询扩展已实现**（按配置启用），另有 ragMinScore/ragMaxResults/ragRecallMultiplier 三参可调 | [RagRetrievalServiceImpl](../../moyun-server/src/main/java/com/moyun/ext/ai/service/chat/impl/RagRetrievalServiceImpl.java#L418) |
| 8 | 向量存储"ES（规划迁 JVector）方案已评审未执行" | **JVector 已落地**：HNSW 余弦检索 + BM25 双路，minScore 过滤在检索器内生效 | [JVectorEmbeddingStore](../../../moyun-server/src/main/java/com/moyun/ext/ai/store/JVectorEmbeddingStore.java#L200) |
| 9 | 记忆"无独立记忆组件"★★☆☆☆ | **Redis 窗口记忆实有**：RedisChatMemoryStore（30 天 TTL）+ MessageWindowChatMemory（agent.maxHistoryTurns 窗口） | [RedisChatMemoryStore](../../../moyun-server/src/main/java/com/moyun/ext/ai/store/RedisChatMemoryStore.java#L34) + [DynamicChatServiceImpl](../../../moyun-server/src/main/java/com/moyun/ext/ai/service/impl/DynamicChatServiceImpl.java#L251) |

### 2.2 高估/需下调项

| # | 原文档判断 | 实测结论 | 代码证据 |
|---|---|---|---|
| 10 | 场景迁移"仅 finance_analysis 接入，剩余待迁移" | **Handler 层 7/7 全注册，但业务真实收口仅 1/7（v11.55 基线实况）**：全库唯一 `aiGatewayService.execute(` 业务调用点是 LedgerAiAnalysisServiceImpl:273（财务分析）。语音面试主流程仍直调 VoiceInterviewServiceImpl、敏感词业务走本地 DFA 词树、简历/出题链路未过网关。**→ v11.57~58 已全部收口（7/7）：sensitive_word/daily_topic（v11.57）→ resume_parse/resume_optimize/question_generate（v11.58 P0-3b，7 个业务调用点）→ voice_interview（v11.58 P0-3c，5 处 llmClient 直调清零；Agent 多轮流式链路保留底座路由）** | grep 全库 `llmClient.chat(` 已清零；网关调用点 14 处（aiGatewayService.execute 5 + aiSceneJsonClient.executeForJson 9） |
| 11 | 工程化 ★★★★☆（测试"缺单测"） | **测试倾斜比认知严重**：12 个测试类全部在老 chat/RAG 包；AnswerScoringEngine（v11.47 专为可测性拆出）**至今零测试**；ai2 网关、FinanceAnalysisHandler、资金链路（pay/ledger）零测试 | src/test 目录清单实测 |

### 2.3 确认准确项

| # | 原文档判断 | 实测确认 |
|---|---|---|
| 12 | Prompt 注入防护"缺失"★☆☆☆☆ | **确认零防护**：网关/基类/7 个 Handler 均无 sanitize/jailbreak/指令隔离；用户文本直接拼接（ResumeParseHandler "简历原文：\n"+text 等） |
| 13 | 多 Agent 协作/规划能力（ReAct/Plan-Execute）/AiServices/Agent 版本管理"缺失" | **全部确认不存在**：无 Supervisor/MultiAgent/Planner/AiServices.builder；ai_agent 表无 version 字段 |

---

## 第三部分 · 十维度实测详情与剩余缺口

### 维度 1：模型配置管理 —— 4.0 → **4.3**

| 子项 | 实测 | 证据/缺口 |
|---|---|---|
| 密钥管理 | ★★★★★（上调） | AES-GCM 加密存储 + 接口脱敏，全链路核实 |
| 多供应商 | ★★★★☆ | provider 配置驱动（OpenAI 兼容协议 + baseUrl，通义/DeepSeek 等皆可接），无供应商硬编码分支 |
| 参数配置 | ★★★☆☆ | temperature/max_tokens/timeout 可配且被消费；**top_p/retry 不可配**（表无列、createChatModel 不消费） |
| 降级链 | ★★★★☆ | 绑定模型失败→回落默认模型（v11.54 含空内容回落+WARN 留痕）；**fallback_model_id 零消费**（仅实体字段，无任何调用） |
| 用量/配额 | ★★★☆☆ | token_used 落库（v11.51）+ Token 统计管理页已有；**无成本金额换算、无日/月 Token 配额** |

**剩余缺口**：top_p/retry 参数化；fallback_model_id 消费或删除（死代码纪律）；成本换算（模型单价表 × token 用量）。

### 维度 2：Agent 体系 —— 2.6 → **3.1**

| 子项 | 实测 | 证据/缺口 |
|---|---|---|
| Agent 定义 | ★★★★☆ | system_prompt/model_config_id/knowledge_library_ids/workflow_id 齐备；**tools 经关联表绑定且真消费**（注入提示词工具列表） |
| 记忆管理 | ★★★☆☆（上调） | 短期窗口记忆实有（Redis+窗口）；**无长期记忆/向量记忆** |
| 工具调用 | ★★★☆☆ | 见维度 3 |
| 人设注入 | ★★★★☆ | 网关 injectAgentPersona + 任务边界防呆（v11.49/53），改人设即生效 |
| 规划/多Agent/版本 | ★☆☆☆☆ | 全部确认缺失 |

**剩余缺口**：长期记忆；Agent 版本化（变更留痕/回滚）。

### 维度 3：工具（Function Calling）—— 1.5 → **3.2**（v11.63 Schema 校验落地，最大修正）

**实测现状**：自定义 `[TOOL_CALL]` 文本协议完整链路——

```
ai_tool 表 + agent_tool 关联表（管理页可维护）
  → ToolCallingService: 提示词注入可用工具列表+调用格式约定
  → LLM 返回 [TOOL_CALL]{...}[/TOOL_CALL]
  → 解析 JSON → ToolRegistry.hasTool 校验 → executeTool 执行
  → 异步审计日志
  →（工作流侧）ToolNodeExecutor 节点执行
```

| 企业级标准 | 实测 | 差距 |
|---|---|---|
| 声明式注册+自动发现 | ★★★★☆ | ToolExecutor Bean 自动发现注册 |
| Schema 定义 | ★★★★☆（v11.63↑） | **实测修正：ai_agent_tool.parameters 种子数据本就是 JSON Schema**（type/required/properties，WeatherTool 等内置工具 getParametersSchema() 同构），原判"文本描述"系误判；v11.63 补齐执行前校验闭环 |
| 执行引擎 | ★★★★☆（v11.63↑） | 自定义协议解析可用 + ToolParamValidator 执行前 Schema 校验（required/type/enum/min-max/items 子集，integer 宽容整值浮点，fail-open 兼容存量脏 schema），ToolRegistry.executeTool 单点收口覆盖 chat/工作流/管理页测试三路径；非原生 FC 依赖提示词约定不变 |
| 错误处理 | ★★★☆☆（v11.63↑） | 校验失败文案为 LLM 可读自纠指令（期望类型 vs 实际值），经工具失败通道回传供下轮修正；仍无超时/重试 |
| 权限控制 | ★☆☆☆☆ | 无按用户/角色的工具权限 |
| 审计 | ★★★★☆ | 调用日志异步落库（v11.63 起校验失败也留痕） |

**剩余缺口**：超时/重试；工具权限。**不建议**推倒重来换 langchain4j 原生 AiServices——现有链路已被 chat 和工作流消费，Schema 校验增强（v11.63 已落）已闭环参数安全。

### 维度 4：知识库 / RAG —— 3.8 → **4.6**（v11.65 knowledge_qa 网关场景接入）

| 子项 | 实测 | 证据/缺口 |
|---|---|---|
| 检索策略 | ★★★★★ | 向量+BM25+RRF 融合+知识库权重+Rerank，国内主流最佳实践 |
| 查询优化 | ★★★★☆（上调） | 查询改写+查询扩展已实现（按配置启用） |
| 向量存储 | ★★★★☆（上调） | JVector 已落地（HNSW+BM25），非"规划中" |
| 引用溯源 | ★★★★☆（v11.64 大幅上调，原文判误；v11.65 网关场景补齐 C 端能力） | **实测修正：完整链路已存在**（2025-12-11 起实装，原判"仍缺"系未查到 chat 消费链）。检索片段 metadata 携带 fileName/fileType/segmentIndex/knowledgeBaseId（即文档记录 ID）/pageNumber/totalSegments/imagePath → Rerank 分数 Map → [ReferenceSourceFilterImpl](../../../moyun-server/src/main/java/com/moyun/ext/ai/service/impl/chat/ReferenceSourceFilterImpl.java#L53) 质量过滤（动态阈值 max(最高分75%, 平均分85%, minScore)、页码去重、目录/短文/图片描述过滤、被引用图片必保留）→ SSE 流式 [buildReferencesButtons](../../../moyun-server/src/main/java/com/moyun/ext/ai/service/impl/chat/ChatResponseBuilderImpl.java#L253) 渲染"来源N"按钮组（data-rerank-score/data-vector-score 相似度透出 + 片段全文 data-text 弹窗 + 不准确反馈按钮）→ [ChatMessagePersistenceImpl](../../../moyun-server/src/main/java/com/moyun/ext/ai/service/impl/chat/ChatMessagePersistenceImpl.java#L100-L108) 将 referenceSources JSON 持久化 → GET /conversation/{id}/messages 历史回传 → admin chat/index.vue 历史消息解析重建按钮。**v11.65 补齐 C 端消费方**：knowledge_qa 场景接入统一网关（[KnowledgeQaHandler](../../../moyun-server/src/main/java/com/moyun/ext/ai2/handler/impl/KnowledgeQaHandler.java)），Agent 驱动多路召回（查询改写+向量/BM25+RRF+Rerank+分片合并，RAG 参数读 Agent 配置与 chat 链一致），references（fileName/页码/分片/文档ID/相似度/摘录）随统一响应透出，消费端可渲染来源。**已知小缺口**：历史 JSON 未存相似度分数（实时流有），前端历史渲染不消费分数故不补（避免过度设计） |
| 数据填充 | ⚠️ | 知识库数据仍空（原文档 P1-5 依旧成立，这是运营问题非技术问题） |

### 维度 5：工作流编排 —— 2.8 → **3.2**

| 子项 | 实测 | 证据/缺口 |
|---|---|---|
| 状态管理 | ★★★☆☆（上调） | ai_workflow_execution 表持久化 status/outputData/executionLog/durationMs |
| 工具节点 | ★★★★☆ | ToolNodeExecutor 接 ToolRegistry |
| 错误处理 | ★★☆☆☆ | 节点失败即 BusinessException 终止；**无节点级重试/异常分支/断点续跑** |
| 触发器/版本 | ★★☆☆☆ | 手动触发；无版本管理 |

### 维度 6：场景配置与网关 —— 3.4 → **4.5**（v11.66 output_parser/output_mode 消费闭环）

| 子项 | 实测 | 证据 |
|---|---|---|
| 场景注册 | ★★★★★ | 7 场景 7 Handler 全注册 + knowledge_qa（v11.65，知识问答网关化：Agent 驱动多路召回 + 引用溯源透出，open_api=1） |
| 配置驱动 | ★★★★★ | 直查库即时生效（v11.48）；模板/人设/限流/缓存/降级全配置化 |
| 语义缓存 | ★★★★☆（激活） | 实现已验证；finance_analysis enable_cache=0（v11.50 因指标过期主动关闭——**注：语义缓存只适合纯文本变换场景，带规则指标的场景禁用是正确决策**） |
| 意图识别 | ★★★★☆（接线） | v11.52 顶层 userInput 契约 |
| 限流 | ★★★★☆ | Redis 实现，双路径生效 |
| 降级 | ★★★★☆ | FallbackStrategy + Handler 内部降级双层 |
| 开放入口管控 | ★★★★☆ | open_api 白名单（v11.51），存量场景默认关闭 |
| 执行日志 | ★★★★☆ | ai_execute_log 含 model_used/agent_used/token_used/elapsed_ms（v11.51 三列有值） |
| 灰度/AB | ★★★☆☆ | version/weight/parent_id 字段在，轮盘赌未实现 |

**核心缺口（改写原 P0-3）已闭环：业务收口 7/7（v11.58）**——
- finance_analysis（v11.55）：财务分析全链路
- sensitive_word（v11.57）：admin 内容安全检测工具 `/cms/ai/safety/detect`
- daily_topic（v11.57）：CMS 话题管理"AI 生成话题"→官方账号发布
- resume_parse / resume_optimize / question_generate（v11.58 P0-3b）：7 个业务调用点（简历解析/建议/岗位匹配/深优×2/深优生成器/JD 关键词）经 task+context 契约切网关（AiSceneJsonClient 统一通道）
- voice_interview（v11.58 P0-3c）：5 处 llmClient 直调全部收口（answer_analysis 回答深析 / candidate_ask 反问应答 / knowledge_desc 知识点简介 / speak_text 轮次话术 / self_intro 自我介绍评分，提示词逐字收编至 VoiceInterviewHandler，外部转写数据全走 wrapData 数据通道隔离）；**Agent 多轮流式链路（InterviewAgentClient，6 处 chat/chatStream）保留底座 Agent 框架路由**（AiSceneBinding 灰度控制）——网关多轮消息契约为后续扩展方向（P2）
- 收口验证：全库 grep `llmClient.chat(` 清零；业务保留 LlmClient 仅剩 `isEnabled()` 前置短路判断

### 维度 7：可观测性 —— 2.9 → **3.7**（v11.60↑，执行日志管理页落地）

已有：requestId 贯穿、ai_execute_log 全字段落库、Token 统计管理页（token-usage/index.vue）、elapsed_ms、**成本金额**（v11.57 P0-2：ai_execute_log.cost_yuan 按模型单价×实际 token 回填，复用 TokenUsageService.calculateCost 与统计页口径一致）、**执行日志管理页**（v11.60 P1-1：`/cms/ai/execute-log` + admin ai/execute-log/index.vue——列表筛选 requestId/场景/模型/状态/日期，汇总卡片调用量/成功率/Token/成本/平均耗时随筛选联动，详情抽屉含输入输出摘要/工具调用/错误信息，过期数据清理入口；SQL 20260911-05 菜单 5472-5474）。
**缺口**：无 P50/P90/P99 耗时分位聚合（列表已有耗时列与均值，分位数待做）；无错误率告警。

### 维度 8：安全与治理 —— 2.5 → **4.0**（v11.62 注入防护 + 成本熔断 + 输出过滤齐备）

| 子项 | 实测 |
|---|---|
| Prompt 注入防护 | **已完成（v11.57 P0-1）**：网关双通道防护——指令通道（顶层 userInput）sanitizeAndCap 清洗 + 危险模式扫描（指令覆盖/提示词探取 → INPUT_REJECTED 1008 拒绝）；数据通道（简历/文档类 input 值）字符级清洗 + Handler wrapData 分隔符隔离（防误杀合法数据）。同步与流式双路径接线，16 用例单测覆盖 |
| 输出内容过滤 | **已完成（v11.62 P1-3）**：[AiOutputFilter](../../../moyun-server/src/main/java/com/moyun/ext/ai2/support/AiOutputFilter.java#L58) 复用 SensitiveWordFilter 的 DFA 词树，对网关同步响应 data 的全部文本节点脱敏（命中词替换 *，保留原文格式；数值/布尔零影响）。**类型保持**（Jackson 往返重建原类型——业务侧 instanceof 强转不受影响）；**零侵入无命中**（未命中原引用返回不重建）；**失败放行**（Jackson 异常放行原文 + ERROR 留痕）；**位置约束**（缓存回写/执行日志之前——缓存与日志留痕均脱敏后内容，缓存命中路径同样过滤兜底存量旧缓存）；场景开关 ai_scene_config.enable_output_filter（默认 0 关闭，按需开启，SQL 20260911-06），11 用例单测覆盖（开关/跳过分支/未命中零侵入/Map 与类型化对象与嵌套结构脱敏/空白绕过仍命中/DFA 异常放行/不可序列化放行）。**已知局限**：流式（SSE）由 Handler 直发不经此过滤（跨片词无法有效匹配），随网关多轮消息契约（P2）一并扩展 |
| 成本熔断 | **已完成（v11.57 P0-2）**：①ai_execute_log.cost_yuan 成本回填（模型单价×input/output token，部分供应商仅回合计时按输出价保守估上界）②ai_scene_config.daily_token_limit 场景日 Token 配额（全体用户共享），TokenCostGuard Redis INCR 日累计 + 跨日自然切换 + TTL 2 天，超限拒绝（AI_TOKEN_LIMIT_EXCEEDED），null/0=不限；Redis 异常放行（fail-open，基础设施抖动不阻断业务）。admin 场景表单可配，13 用例单测覆盖 |
| 密钥/合规 | ★★★★☆（密钥加密实测达标，证件号 AES 沿用） |

### 维度 9：工程化 —— 4.2 → **4.4**（v11.70↑，测试护栏+异步任务收敛+资金链路）

| 子项 | 实测 |
|---|---|
| 异步任务 | **已收敛（v11.67 P1-6）**：双轨制——表驱动 portal_ai_task（AiTaskService，长任务/需审计，孤儿任务启动自动置败恢复）+ Redis+线程池（财务分析 v11.55，短任务轻量态）；JudgeAsyncWorker 系 OJ 判题领域专属（非 LLM），类注释已声明边界；选型规则落三处类注释（要留痕走表，要轻快走 Redis） |
| 流式输出 | 网关 executeStream + chat SSE 可用；前端 chat/diagram 页在用 |
| 提示词管理 | ★★★★★ 配置模板+占位符+版本（version 字段） |
| 测试 | ★★★☆☆：老 chat/RAG 包 12 个 + ai2 新增 112 个 + 工具新增 16 个（v11.66，合计 140）（PromptInjectionGuard 16 / TokenCostGuard 13，v11.57；SceneConvergenceContractTest 14 用例收口契约，v11.58~59；**P0-4 新增 37 用例（v11.60）：AnswerScoringEngineTest 12——评分公式/6维连续性/关键词提取护栏（发现并修复 answer=null NPE 与 tags 分支无上限两个真 bug）/LLM动态题题干回退；FinanceAnalysisHandlerTest 9——指标护栏（应急基金月数/健康分四段公式/资产负债率/还款压力/储蓄率/赤字月数）、debtFact 清偿测算 ceiling、LLM 失败降级契约（aiEnabled=false+模板综述+指标照常）、input 契约；Ai2InfraSupportTest 16——FallbackStrategy 分场景内置兜底/配置兜底优先/非法JSON原样、SceneRateLimiter 固定窗口/首次setExpire/超限拒绝/Redis异常放行、SemanticCache 精确命中往返/fromCache标记/ttl<=0不写/场景隔离/Redis异常降级**；**AiOutputFilterTest 11（v11.62）：开关判定/失败码与空数据跳过/未命中原引用零侵入/Map+类型化对象+嵌套结构脱敏与类型保持/空白绕过仍命中/DFA 异常与不可序列化放行**；**ToolParamValidatorTest 16（v11.63）：fail-open 三跳过分支（空/非法 JSON/非 object 根）/必填缺失/类型不匹配含自纠文案断言/integer 宽容（整值 Double/Long 过、3.5 与字符串数字拒）/enum/minimum-maximum/数组元素类型/未知类型跳过/多余参数宽容/多错误并报**；**KnowledgeQaHandlerTest 12（v11.65）：参数校验/知识库三级优先级与三种传参形态/非法 ID 忽略/未绑定快速失败/检索为空零 token 响应/LLM 成功引用溯源（metadata+rerankScore+摘录截断）/场景模板覆盖/LLM 空回答失败/Agent 加载失败空壳默认/ThreadLocal 必清理**；**OutputParserWiringTest 9（v11.66）：parseOutput 分派分支（null/blank 默认 json、markdown·text 原文包装、未知容错）+ DailyTopic 双参配置驱动回归（默认 json 行为不变、markdown 配置真实生效）**；**AiTaskServiceTest 9（v11.67）：表驱动任务提交校验/入库触发/触发失败回写/越权拒绝/result 解析容错/孤儿恢复（含 MP UpdateWrapper 惰性求值断言技巧——IN 表达式需先触发 getSqlSegment 才注册参数）**；**老包存量修复 3+新增 2（v11.67）：ContentScoring 停用词（无分词器现状 documenting）/开头加分（文本加长 ≥80 字符避开长度惩罚 ×0.7）、RagRetrieval 质量过滤（阈值 max(2.0, top×0.3) 补过滤用例）——三处均系实现合理演进后测试未跟进，修测试不修实现**；全库 286 用例全绿（v11.67）；**资金链路 22 用例（v11.70）——PortalTipServiceTest 16（积分打赏闭环：原子扣分 WHERE 防护/对方加分/PAID 订单/双方成长事件；专栏打赏解析专栏创建者；对象不存在/金额非法（null·0·负·0.5 截断）/自赏/积分不足不加对方分不落单/article_paid 占位拦截/未实名回滚；微信下单 pending+网关统一下单+收银台参数/超 10000 元拒/未登录拒）+ TipPayCallbackHandlerTest 6（回调闭环：pending→paid 条件更新/复式分账平台抽成+作者所得金额守恒/双方通知含到账明细/已支付幂等返回不重复分账/状态异常拒/并发竞争 rows=0 整体回滚）**；全库 308 用例全绿（v11.70）；充值/提现业务逻辑未实现（钱包 Service 仅 CRUD），无从测试 |

### 维度 10：架构与扩展 —— 4.0（维持）

分层清晰（业务→ai2 依赖方向恒定、Handler 差异化编码区范式已经 finance_analysis 验证）；模型/向量库可替换；服务无状态。

---

## 第四部分 · 修正后的缺口清单（按优先级）

> 原文档 P0-1（FC 完全缺失）经实测降级改写；P0-2/P0-4 维持；P0-3 改写为"业务收口"。

### P0 · 阻断级

| # | 缺口 | 实测现状 | 改进方向（结合文档标准） | 工作量预估 |
|---|---|---|---|---|
| P0-1 | **Prompt 注入防护** | ✅ **已完成（v11.57）**：PromptInjectionGuard 双通道防护（指令通道清洗+拦截 / 数据通道 wrapData 隔离），网关同步+流式接线，INPUT_REJECTED=1008，16 用例单测 | — | — |
| P0-2 | **成本监控与熔断** | ✅ **已完成（v11.57）**：①成本回填复用 ai_model_config 既有单价列（input_price/output_price，与 Token 统计页同源 TokenUsageService.calculateCost）②ai_execute_log.cost_yuan 按实际 token 回填③ai_scene_config.daily_token_limit + TokenCostGuard（Redis 日累计，超限 AI_TOKEN_LIMIT_EXCEEDED 拒绝，fail-open），admin 表单可配，13 用例单测 | — | — |
| P0-3 | **场景业务收口 7/7** | ✅ **全部完成（v11.57~59）**：①v11.57 sensitive_word/daily_topic——SensitiveWordHandler 输入契约修正（userInput→input.text 数据通道，修复意图分类器误打断）+ admin 内容安全检测工具页（AiSafetyController `/cms/ai/safety/detect` + ai/safety/index.vue）+ CMS 话题管理"AI 生成话题"（30 条标题去重上下文 → 网关草稿 → 管理员确认 → moyun_official 官方账号发布）；SQL 20260911-04（官方账号+菜单 5470/5471）②v11.59 P0-3b resume_parse/resume_optimize/question_generate——7 个业务调用点切 AiSceneJsonClient（task+context 契约，子任务提示词逐字收编进 Handler）③v11.59 P0-3c voice_interview——5 处 llmClient 直调收口（answer_analysis/candidate_ask/knowledge_desc/speak_text/self_intro 五子任务），ScoringEngine.evaluateSelfIntro 增加 userId 归属参数；InterviewSceneData 新增 structured 透传字段，AiSceneJsonClient 解包泛化（Resume+Interview 双 Data 类型）；Agent 多轮流式链路（InterviewAgentClient 6 处）保留底座路由，网关多轮消息契约列为 P2 扩展；SceneConvergenceContractTest 14 用例全绿 | — | — |
| P0-4 | **新链路测试覆盖** | ✅ **已完成（v11.60）**：①AnswerScoringEngineTest 12 用例——评分公式（覆盖率×80+长度加成）、6 维连续性（雷达图键对齐/单调性）、关键词提取护栏（停用词/纯数字/长度/去重/12 上限）、LLM 动态题题干回退；**单测发现并修复 2 个真 bug：answer=null NPE（answer.length() 未判空）、tags 分支无 MAX_KEYWORDS 封顶（20 词全量入库）**②FinanceAnalysisHandlerTest 9 用例——指标护栏（应急基金月数=流动资产/月均支出 8.3、健康分四段公式 72、资产负债率/还款压力/储蓄率、赤字月数/采样月数）、debtFact 清偿测算（ceiling 取整/无月供不测算/期数进度透传）、LLM 失败降级契约（aiEnabled=false+模板综述+指标照常返回）、input 契约（缺 userId 拒绝/非法 range 回落 month/3m 窗口起点）③Ai2InfraSupportTest 16 用例——FallbackStrategy（voice_interview end 兜底/sensitive_word 安全默认/通用 failure/配置 JSON 优先/非法 JSON 原样/空白走内置）、SceneRateLimiter（固定窗口计数/首次 setExpire/超限拒绝/limit≤0 放行/Redis 异常放行）、SemanticCache（精确命中往返/fromCache 标记/ttl≤0 不写/场景隔离/查询异常降级）。Mock 全 Mapper 离线可测，基类 LLM 依赖置 null 自然走降级路径 | — | — |

### P1 · 重要级

| # | 缺口 | 实测现状 | 改进方向 |
|---|---|---|---|
| P1-1 | ai_execute_log 管理页 | ✅ **已完成（v11.60）**：后端 AiExecuteLogController（`/cms/ai/execute-log`——list 分页筛选 requestId/场景/模型/状态/日期，summary 汇总卡片调用量/成功率/Token/成本/平均耗时随筛选联动，scene-options 场景下拉，{id} 详情，{ids} 批量清理）+ admin 前端 ai/execute-log/index.vue（筛选表单/汇总卡片/列表/详情抽屉含输入输出摘要与工具调用 JSON/删除确认）+ api/ai/execute-log.js；SQL 20260911-05（菜单 5472-5474 挂 AI基础配置 5238，权限 cms:ai:execute-log:list/query/remove） | — | — |
| P1-2 | 工具 Schema 校验 | ✅ **已完成（v11.63）**：实测修正——ai_agent_tool.parameters 种子数据本就是 JSON Schema，缺的是执行链校验。ToolParamValidator（required/type/enum/min-max/items 子集，integer 宽容整值浮点，fail-open）+ ToolRegistry.executeTool 执行前收口（chat/工作流/管理页测试三路径全覆盖）+ 校验失败文案为 LLM 可读自纠指令经既有失败通道回传 + buildToolPrompt 前置合规指令；16 用例单测。剩余超时/重试与工具权限为 P2+ 事项 | — |
| P1-3 | 输出内容过滤 | ✅ **已完成（v11.62）**：AiOutputFilter 复用 SensitiveWordFilter 的 DFA 词树，网关同步响应 data 全部文本节点脱敏（类型保持/零侵入无命中/失败放行/缓存与日志留痕均脱敏后内容/命中路径兜底旧缓存）；场景开关 enable_output_filter（默认关，SQL 20260911-06）；AiOutputFilterTest 11 用例。流式路径未覆盖（随 P2 网关多轮消息契约扩展） | — |
| P1-4 | 引用溯源 | ✅ **实测修正（v11.64）：已实现，原判"无来源透出"系误判**。完整链路 2025-12-11 起实装：metadata 携带文档 ID/片段定位（fileName/segmentIndex/pageNumber/lineStart-lineEnd/imagePath）→ Rerank 分数过滤（动态阈值+质量过滤）→ SSE 流式"来源N"按钮（相似度 data-rerank-score/vector-score + 片段全文弹窗 + 不准确反馈）→ referenceSources JSON 持久化 → 历史消息 API 回传 → admin chat 前端渲染（实时+历史重建）。历史 JSON 未存分数为已知小缺口（前端不消费，不补）。**v11.65 增强：knowledge_qa 场景接入统一网关**（KnowledgeQaHandler：Agent 驱动多路召回，references 含 fileName/页码/分片/文档ID/相似度/摘录随统一响应透出，C 端可消费） | — |
| P1-5 | output_parser/output_mode 消费 | ✅ **已完成（v11.66）**：①基类 parseOutput(raw, config) 按 output_parser 分派（json 默认行为零变化/markdown·text 原文包装 content/未知值 WARN 容错），6 个存量 Handler 升级双参 execute 使 config 直达主流程（子任务契约保留固定 JSON 不受配置影响）；②网关路由校验——output_mode='stream' 拒同步入口、'sync' 拒流式端点（Handler 能力+配置双保险）；③SQL 20260911-08 对齐 voice_interview 配置 sync→both（Handler 已实现流式，此前拍脑袋填 sync 因零消费无人发现）；④listScenes 透出 outputParser。OutputParserWiringTest 9 用例（分派分支+DailyTopic 配置驱动回归） | — |
| P1-6 | 异步任务收敛 | ✅ **已完成（v11.67）**：实测修正——"三套模式"中 JudgeAsyncWorker 系 OJ 代码判题专属基础设施（非 LLM 任务），误计入 AI 债，类注释已声明领域边界；其余两套为**合理双轨制**，收敛策略为选型规则文档化而非强行统一抽象（避免过度设计）：①选型规则落 [AiTaskService](../../../moyun-server/src/main/java/com/moyun/ext/cms/service/AiTaskService.java)（表驱动：长任务/需审计/结果持久化）与 LedgerAiAnalysisServiceImpl（Redis：短任务/时效性强）类注释，口诀"要留痕走表，要轻快走 Redis"②真代码缺口修复——表驱动任务孤儿恢复（@Async 存活于 JVM 内存，应用重启后 pending/running 永久卡死前端轮询挂死；ApplicationReadyEvent 启动时统一置 failed"服务重启，任务中断"，单实例语义正确）③AiTaskServiceTest 9 用例；顺带修复老包 3 个存量测试失败（实现演进后测试未跟进），全库 286 用例全绿 | — | — |
| P1-7 | 知识库数据回填 | ✅ **运营指引已出（v11.67）**：[知识库数据回填运营方案](./知识库数据回填运营方案-Java前端面试题库.md)——建库规划/内容模板（RAG 分片友好）/上传向量化流程/场景绑定 SQL/验收标准；内容数据由运营自主生产（Java 后端+前端面试题库），零代码改动 |
| P1-8 | 资金链路测试 | ✅ **已完成（v11.70，纯测试增量零生产代码改动）**：PortalTipServiceTest 16——积分打赏 [toggleTipOrList](../../../moyun-server/src/main/java/com/moyun/portal/service/impl/PortalTipServiceImpl.java) 全分支（对象解析 article/column/不存在/未知类型、实名校验回滚、原子扣分 WHERE 防护（余额不足**不加对方分不落订单**）、自赏拦截、金额边界（null·0·负·0.5 截断·积分通道无 10000 上限）、article_paid 占位拦截（未扣费不发放付费阅读权限）、微信下单（pending 落库+网关统一下单+收银台参数）/超 10000 元拒/未登录拒；TipPayCallbackHandlerTest 6——[TipPayCallbackHandler](../../../moyun-server/src/main/java/com/moyun/portal/service/impl/TipPayCallbackHandler.java) 回调闭环（pending→paid 条件更新、复式分账平台抽成+作者所得**金额守恒断言**、双方站内通知含到账明细、已支付幂等返回不重复分账、状态异常拒、并发竞争 rows=0 整体回滚不通知）。**实测澄清**：充值/提现业务逻辑尚未实现（PortalWalletServiceImpl 仅 CRUD，全库无 recharge/withdraw 方法，PayCallbackHandler 仅 tip 一种），无从测试——属功能范围澄清而非测试缺口 | — | — |

### P2 · 增强级

| # | 缺口 | 改进方向 |
|---|---|---|
| P2-1 | top_p/retry 参数化 | 表加列 + createChatModel 消费 |
| P2-2 | fallback_model_id 零消费 | 二级降级链（绑定模型→fallback_model→默认模型）；不做则删字段 |
| P2-3 | 灰度轮盘赌 | version/weight 已有字段，网关按权重分流 |
| P2-4 | 长期记忆 | 向量记忆（用户画像/历史摘要入库，会话隔离） |
| P2-5 | Agent 版本管理 | ai_agent 加 version + 变更历史表 |
| P2-6 | 工作流断点续跑/触发器 | 节点状态表 + 定时/Webhook 触发 |
| P2-7 | 多 Agent 协作/ReAct | 等原生 FC 增强（P1-2）后再评估，避免过度建设 |
| P2-8 | P50/P90/P99 聚合 + 告警 | 基于 ai_execute_log 的统计任务 + 阈值告警 |

---

## 第五部分 · 修正后的行动路线（供排期参考）

### 第一批 · 安全止血（对应原文档"阶段1"，内容修正）

1. ~~Prompt 注入防护钩子（P0-1）~~ ✅ **已完成（v11.57）**
2. ~~成本三件套：单价列 + cost 回填 + daily_token_limit 熔断（P0-2）~~ ✅ **已完成（v11.57）**
3. ~~AnswerScoringEngine + FinanceAnalysisHandler 指标护栏单测 + 基础设施单测（P0-4）~~ ✅ **已完成（v11.60，37 用例全绿，揪出 2 个真 bug）**

### 第二批 · 收口与可观测

4. ~~ai_execute_log 管理页（P1-1，快赢）~~ ✅ **已完成（v11.60：后端 5 接口 + admin 页面 + 菜单 SQL）**
5. ~~场景业务收口分批切换（P0-3）~~ ✅ **已完成（v11.57~59，7/7 全量收口，llmClient.chat 全库清零）**
6. ~~输出过滤钩子复用 DFA 词树（P1-3）~~ ✅ **已完成（v11.62：AiOutputFilter + 场景开关 + 11 用例，安全三件套齐备）**

### 第三批 · 能力增强

7. ~~工具 JSON Schema 校验（P1-2）~~ ✅ **已完成（v11.63：ToolParamValidator + executeTool 收口 + 16 用例）**
8. ~~引用溯源（P1-4）~~ ✅ **实测修正（v11.64）：完整链路已实装，评估原判误判，无代码改动**
9. ~~output_parser 接线（P1-5）~~ ✅ **已完成（v11.66：parseOutput 分派 + 网关 output_mode 路由校验 + voice_interview 配置对齐 both）**
10. ~~异步任务模式收敛文档化（P1-6）~~ ✅ **已完成（v11.67：双轨制选型规则 + 孤儿任务恢复 + 老包存量测试修复，全库 286 全绿）**
11. ~~资金链路测试~~ ✅ **已完成（v11.70：打赏双链路 22 用例——积分打赏全分支 + 微信支付下单 + 回调闭环幂等/并发竞争/复式分账；充值/提现业务未实现无从测试，全库 308 全绿）**

> **P1 级全部清零（v11.70）**——知识库数据回填转运营执行（指引已交付）。后续迭代进入 P2：top_p/retry 参数化、耗时分位数与错误率告警、流式路径输出过滤（随网关多轮消息契约扩展）。

### 原文档路线图调整说明

- 原阶段1的"Function Calling 体系从零建设"取消——链路已在，改为 Schema 增强（降为 P1-2）
- 原阶段2的"语义缓存实现"取消——已实现；改为语义缓存适用边界文档化（纯文本变换场景开、带规则指标场景关）
- 原阶段2的"意图识别接入"取消——v11.52 已接线
- 安全类（注入/输出过滤/熔断）整体前移到第一批——实测确认这是当前最大的生产风险面

---

## 第六部分 · 实测 Checklist（勾选版，对照原文档第八部分）

### 模型层
- [x] 密钥 AES-GCM 加密 + 接口脱敏（**实测达标，原文档待核实项通过**）
- [x] 模型路由（Agent→直绑→默认 责任链）
- [x] 绑定失败回落默认模型（v11.54 含空内容回落）
- [x] Token 统计落库 + 管理页
- [ ] top_p/retry 参数化
- [ ] 多级降级链（fallback_model_id 未消费）
- [ ] 成本换算 + 日/月配额

### Agent 层
- [x] Agent 定义（prompt+模型+知识库+工作流+工具关联）
- [x] 短期记忆（Redis 窗口）
- [x] 人设注入 + 任务边界防呆
- [x] Function Calling Schema 校验（v11.63 ✅；原生化维持自定义 [TOOL_CALL] 协议不迁移）
- [ ] 长期记忆
- [ ] 规划/多 Agent/版本管理

### 工具层
- [x] 注册 + 自动发现（ToolExecutor Bean）
- [x] 执行引擎（[TOOL_CALL] 协议）
- [x] 调用审计（异步日志）
- [x] JSON Schema 定义 + 参数校验（v11.63 P1-2 ✅：ToolParamValidator 执行前校验，ToolRegistry 收口三路径）
- [ ] 超时/重试/权限控制

### RAG 层
- [x] 混合检索（向量+BM25+RRF）+ 重排
- [x] 查询改写 + 扩展
- [x] JVector 向量存储
- [x] minScore 阈值过滤
- [x] 引用溯源（v11.64 实测修正：ReferenceSourceFilter 质量过滤 + 流式来源按钮含相似度/片段弹窗/反馈 + 持久化与历史回传，2025-12-11 已实装；v11.65 knowledge_qa 网关场景 references 透出，C 端可消费）
- [ ] 数据回填（运营执行中——v11.67 指引已交付：[知识库数据回填运营方案](./知识库数据回填运营方案-Java前端面试题库.md)，数据由运营自主生产）

### 工作流层
- [x] 执行状态持久化（ai_workflow_execution）
- [x] 工具节点
- [ ] 断点续跑/节点重试/版本/触发器

### 网关层
- [x] 场景注册表（AiSceneEnum 单一权威源）
- [x] 配置驱动免发版（直查库）
- [x] 统一入口 + open_api 白名单
- [x] 多维限流（Redis，场景×用户）
- [x] 语义缓存（精确+0.95 余弦）
- [x] 降级策略（双层）
- [x] 执行日志（含模型/Agent/token/cost_yuan）
- [x] 执行日志管理页（v11.60 P1-1 ✅：/cms/ai/execute-log + admin ai/execute-log，筛选/汇总/详情/清理）
- [x] 意图识别（v11.52 接线）
- [x] **业务收口 7/7（P0-3 ✅ v11.57~58：全部场景 LLM 直调清零，统一经 AiSceneJsonClient/aiGatewayService，日志/成本/限流/注入防护全链路生效）**
- [ ] 灰度轮盘赌（字段有逻辑无）
- [x] output_parser/output_mode 消费（v11.66 P1-5 ✅：parseOutput 分派 + 网关 output_mode 路由校验）

### 可观测性
- [x] requestId 贯穿
- [x] Token 多维统计页
- [x] ai_execute_log 查询页（v11.60 P1-1 ✅ 筛选/汇总/详情/清理）
- [x] 成本监控（v11.57 P0-2：cost_yuan 回填 + daily_token_limit 熔断 ✅）
- [ ] P50/P90/P99 + 告警

### 安全治理
- [x] 密钥加密
- [x] 审计日志
- [x] **Prompt 注入防护（P0-1 ✅ v11.57）**
- [x] **输出内容过滤（P1-3 ✅ v11.62：DFA 词树脱敏，按场景开关，流式待 P2）**
- [x] **成本熔断（P0-2 ✅ v11.57）**

### 工程化
- [x] 异步任务（财务 Redis+线程池 / 简历表驱动）
- [x] SSE 流式（网关+chat）
- [x] 提示词模板+占位符+版本字段
- [x] 测试覆盖（v11.60 ✅ P0-4 完成：ai2 80 用例——注入 16/成本 13/收口契约 14/评分引擎 12/财务护栏 9/基础设施 16，揪出并修复 2 个真 bug；v11.67 ✅ AiTaskServiceTest 9 + 老包存量 3 修复 + 2 新增；v11.70 ✅ 资金链路 22 用例——打赏积分链路全分支+微信下单+回调闭环幂等/并发/分账守恒，全库 308 全绿）
- [x] 异步任务模式收敛（✅ v11.67 P1-6：双轨制选型规则 + 孤儿任务恢复；JudgeAsyncWorker 系 OJ 专属非 AI）

---

## 附录 · 本实测的验证方法与局限

- **方法**：4 路并行代码扫描覆盖 com.moyun.ext.ai / ext.ai2 / ledger / admin-vue，关键结论（业务收口 1/7、fallback_model_id 零消费、注入防护为零）经全库 grep 复核
- **局限**：①未实测运行时行为（如语义缓存命中率、限流实际阈值表现），仅静态核实代码存在与接线；②工作流节点类型枚举未逐项展开；③管理页仅核实存在性未逐一走查交互
- **建议**：每完成一个 P0/P1 项，同步更新本文档对应勾选项与维度评分（与原文档的维护约定一致）

---

> **配套文档**：《AI 底座企业级评估标准与项目现状分析.md》（评估标准与框架）/ `devlog.md`（变更记录）/ `docs/06-规划路线/开发进度与规划.md`（Phase 排期）
