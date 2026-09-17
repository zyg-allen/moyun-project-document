# AI 能力架构 v4 整合 · 全局现状对比与优化实施计划

> **基线**：v11.93 现状 × 《AI 能力架构完整设计文档_v4》目标架构
> **输入**：v4 设计文档 + V3 全景技术报告 + 代码核实（2026-09-16）
> **用途**：全局视角的差距评估与分阶段实施计划（决策完整，可直接执行）

---

# 一、全局现状 vs v4 目标对比矩阵

## 1.1 三层架构职责

| 层 | v4 目标 | 当前现状 | 达成度 |
|---|---|---|---|
| **场景层** | 只配 agent_id + 治理（限流/降级/缓存/output_schema/灰度） | ai_scene_config 存在完整治理字段 ✅；但同时存在**直绑模型路径**（modelConfigId→伪Agent）、knowledgeLibraryIds/toolIds、systemPromptTemplate/userPromptTemplate 字段（KnowledgeQaHandler 在用场景模板） | ⚠️ 60% |
| **Agent 层** | system_prompt 唯一来源 + 模型/工具/知识库/工作流 | 面试主干 ✅（agent48 人设+模型路由+maxHistoryTurns）；分析链路 handler 硬编码任务指令（v4 允许代码层任务指令，不违反）；KnowledgeQaHandler 用场景模板 ❌ 唯一来源被破坏 | ⚠️ 75% |
| **底座层** | 模型/知识库/工具独立 | ai_model_config/知识库 ✅；**ai_tool 不存在** ❌ | ⚠️ 65% |

## 1.2 消息模型与上下文

| v4 要求 | 当前现状 | 达成度 |
|---|---|---|
| 三层组装：System（agent人设）→ User（业务上下文）→ 任务指令 | V3 面试完全吻合：system=agent人设+本场约束；context user=简历+JD（wrapData）；每轮指令（进度 x/N） | ✅ 100% |
| 历史对话保留最近 N 轮 | **V3 滑窗（Redis MessageWindowChatMemory，N=maxHistoryTurns×2）优于 v4 7.3 的"每轮 recentQAs 重拼"描述**——滑窗零重拼，v4 文档需按 V3 实现修正 | ✅（超出） |
| 上下文长度控制（单轮 ≤3000 tokens） | 滑窗 40 条上限 ✅；简历摘要≤3 项目 ✅；JD 无截断 ⚠️；RAG 未接入（无 Top3 控制）❌ | ⚠️ 70% |
| SystemMessage 唯一且在最前 | initFirstTurn 幂等保证 ✅ | ✅ |

## 1.3 治理与调用链

| v4 要求 | 当前现状 | 达成度 |
|---|---|---|
| 所有业务调用走 aiGatewayService.execute | 分析链路 ✅（6 场景收口：voice_interview/resume_parse/resume_optimize/resume_deep/finance_analysis/question_generate）；**面试主干直连 agent 模型** ❌（无限流/熔断/日志，仅业务 RateLimiter 60/h 兜底） | ⚠️ 70% |
| Chat 接口走 default_chat 场景 | /ai/chat 走 DynamicChatService 管道，**无场景、无 default_chat** ❌ | ❌ 0% |
| 执行日志全链路 | ai_execute_log 分析链路 ✅；主干直连无日志 ❌ | ⚠️ 60% |
| 结构化输出 JSON Schema（★★★★★） | 全部 Prompt 约束（★★）+ parseJsonMap 容错 | ⚠️ 40% |

## 1.4 扩展能力

| 能力 | v4 目标 | 现状 |
|---|---|---|
| 工具调用（Function Calling） | ai_tool 表 + 执行器（记账查询/题库检索） | ❌ 未实现 |
| 多模态 | 模型表 supports_vision/audio 字段 + 账单识别场景 | ❌ 未实现（记账账单截图需求待依赖） |
| 灰度 version/weight | 场景表字段已有 | ⚠️ 字段在、逻辑弱 |

## 1.5 面试应用（v4 第七部分 vs V3）

| v4 要求 | V3 现状 | 结论 |
|---|---|---|
| 一个场景 + 一个 Agent + 多 task 共用 | voice_interview 场景 + agent48，task 实际活跃仅 answer_analysis/knowledge_desc | ✅ 结构吻合 |
| **task=warmup**：一次调用输出 understanding + interviewPlan + opening + firstQuestion | 无 warmup；start 一次调用仅产 opening+首题；无理解/计划产物 | ❌ 待实施（v4 方案优于 V3 报告的两次调用设计——**合并为一次**） |
| task=candidate_ask（候选人反问） | handler 子任务在，**调用方已随 V3 删除** | ❌ 待恢复（四段式第 4 段） |
| task=speak_text / self_intro | handler 子任务在，调用方已删（死链路） | 建议废弃（违背死代码清理原则，v4 文档同步修正） |
| task=report（报告综合评审） | 业务侧 runBatchAnalysis + 规则聚合 | ⚠️ 可留业务侧（规则融合含权重表逻辑），暂不迁场景 |
| RAG 参考知识（参考知识 Top3 入上下文） | **未接入**（InterviewAgentClient 直连裸模型） | ❌ 待实施（预热检索） |

---

# 二、冲突裁决（V3 实现 × v4 文档的 6 个矛盾点）

| # | 矛盾 | 裁决 | 理由 |
|---|---|---|---|
| 1 | v4 §7.3 每轮 recentQAs 重拼上下文 vs V3 Redis 滑窗 | **保留滑窗**，v4 文档修正为"历史对话层=滑窗实现" | 滑窗零重拼/防重复提问是 V3 最大收益；v4 该段是写文档时的旧理解 |
| 2 | v4 §12 一切调用走网关 vs V3 面试主干直连 | **中期收口**：网关新增"流式直通"通道（executeStream 透传 SSE），主干改走网关获得限流/熔断/日志，配置查询走缓存增量 <5ms | 直连的性能顾虑只在网关做重活时成立；治理收益（Token 熔断/日志）值得收口。短期维持直连不阻塞 |
| 3 | v4 §10.1 场景表不配提示词 vs 我上轮建议"任务提示词迁场景表模板" | **以 v4 为准，撤回上轮建议**：人设唯一在 agent.systemPrompt；任务指令留在 handler 代码（v4 §3.4 第 3 层"任务指令←代码"） | 避免两处提示词冲突是 v4 核心决策；运营调人设走 agent 管理页即可满足 |
| 4 | v4 warmup 输出全量（理解+计划+开场+首题）vs V3 报告设计的"warmup+generateOpening 两次调用" | **按 v4 合并为一次调用** | start 同步段保持 1 次 LLM 延迟不变，多产出理解+计划；失败降级走现有无计划链路 |
| 5 | v4 §7.1 保留 speak_text/candidate_ask vs V3 删除反问环节、speak_text 同值冗余 | **存量保留，重新定义角色**：speak_text 从"与 question 同值"改为**独立话术**（TTS 优先播、空则 fallback question）；candidate_ask 作为**可选 task**（sys_config 开关控制，四段式第 4 段）；self_intro 保持待激活 | 不是死链，而是待激活的能力；增量可控（开关），语义清晰（question=结构化文本 / speakText=口语话术） |
| 6 | v4 场景表废弃直绑模型路径 vs 现有 AiSceneBinding.modelConfig 兜底路径 | **中期收口**：6 个活跃场景全部迁 agent 绑定后，直绑路径标记 @Deprecated 保留一个版本再删 | 避免一次性大迁移风险；先补账单解析等新 Agent，再废路径 |

---

# 三、优化实施计划

## 3.1 P0 · v11.94 立即修（功能缺陷，半天）

| # | 任务 | 改动 | 验证 |
|---|---|---|---|
| 1 | **报告分数断链**：runBatchAnalysis 单题分析成功后 `qa.setScore(analysis.score)` 写回（V3 无实时分，score 恒 null 导致聚合全跳过） | [VoiceInterviewServiceImpl.java](../../../moyun-server/src/main/java/com/moyun/ext/cms/service/impl/VoiceInterviewServiceImpl.java) ~3 行 | 走完一场面试，报告有总分/维度/逐题点评 |
| 2 | **interviewSelfIntro 错位**：system 增段序约束"第 1 问固定请候选人自我介绍"（顺带 P1-4） | buildInterviewerSystemPrompt +1 行 | 报告第一栏有自我介绍摘要 |
| 3 | **知识点/错题本恒空**：与 3.2 预热合并处理（题库 RAG 化），本版本先隐藏报告空 Tab | 前端 v-if | 无空 Tab |

## 3.2 短期 · v11.94 面试 v4 落地（预热 + 四段式，1 个版本）

### 3.2.1 warmup 一次调用（v4 §7.2 协议）

**场景配置**（ai_scene_config.voice_interview 的 config_json，无 DDL）：

```json
"tasks": {
  "warmup": {
    "output_schema": {
      "type": "object",
      "properties": {
        "understanding": {"candidateProfile": "...", "strengths": [], "concerns": []},
        "interviewPlan": {"focusAreas": [{"area": "...", "reason": "...", "depth": "basic|intermediate|deep"}]},
        "opening": "...", "firstQuestion": "..."
      },
      "required": ["understanding", "interviewPlan", "opening", "firstQuestion"]
    }
  }
}
```

**代码结构**：
```
VoiceInterviewHandler 新增 task=warmup：
  system = agent.systemPrompt（人设）
  user   = wrapData(简历摘要) + wrapData(JD) + wrapData(RAG top-K 题库片段) + 任务指令（输出 JSON）
  chat → parseJsonMap → InterviewSceneData.structured

VoiceInterviewServiceImpl.start() 重构：
  ① buildResumeDigest（现有）
  ② RagRetrievalService 检索 top-5（query=岗位+简历项目关键词，按 agent48 绑定知识库）
  ③ agentClient/场景网关调 warmup → understanding+plan+opening+firstQuestion
  ④ 失败降级：无 plan，走现有 generateOpening 链路（不阻塞开面）
  ⑤ plan 存 configJson.warmupPlan（断点续接 rebuild 免重算）
  ⑥ systemPrompt += renderSystemSection(plan)（面试理解+考察方向，常驻滑窗）
  ⑦ opening+firstQuestion 替代 generateOpening 产物（start 仍 1 次 LLM）
```

### 3.2.2 四段式段序约束（纯提示词，滑窗保证执行）

system 增两段：
- **第 1 段**：首问固定请自我介绍；随后 2~3 问必须从自我介绍提取深挖点逐一追问
- **第 4 段（可选，`sys_config: voice.interview.candidateAsk.enabled` 开关控制，默认开）**：问满 N 题后，先邀请候选人提问并解答（复用 handler `candidate_ask` 子任务），完成后再结束

前端配合：开关开启时 finished 前 catch 一次"反问输入框"模式（inputMode='ask'，直调 candidate_ask 子任务，语义清晰）；开关关闭时保持现状直收尾。

### 3.2.3 知识点/错题本 RAG 化

- `buildKnowledgePoints`：改 RagRetrieval（position+低分题话术关键词）top-5 片段 → title/desc
- 错题本：低分题 + 检索关联知识点入库（questionId 可空，title 存话术节选）

### 3.2.4 存量能力语义重定义（存量保留，增量可控，语义清晰）

**原则：不是死链，而是待激活的能力。**

| 项 | 旧语义 | 新语义（v11.94 重定义） |
|---|---|---|
| `speak_text` task + qa.speakText 字段 | 与 question 同值（冗余） | **独立话术**：question=结构化问题文本（报告回放/分析用），speakText=口语化话术（TTS 播报用）。落库时同值兼容现状；前端 TTS **优先播 speakText、为空 fallback question**；后续可让 warmup/话术生成分离两者（如 speakText 带语气词、question 去语气词） |
| `candidate_ask` task | 反问环节（V3 调用方已删） | **可选 task**：由 `sys_config: voice.interview.candidateAsk.enabled` 开关控制激活（四段式第 4 段），关闭时零开销 |
| `self_intro` task | 自我介绍 4 维评分 | 保持现状待激活（报告第一栏 selfIntro 摘要已有来源；若后续需要 4 维评分再启用） |
| sys_config `voice.interview.dynamicMode` | 动态出题开关（已无消费方） | 删除（纯残留键，无语义可保留） |

**四同步**：devlog v11.94、V2 文档 V4 章节、SQL（config_json 更新 UPDATE 脚本 + sys_config 新增 candidateAsk 开关，无 DDL）、菜单无变更。
**验证**：mvn compile（全路径防劫持）+ vue-tsc + build:h5；实测一场含反问段的完整面试。

## 3.3 中期 · v11.95 架构收口（v4 短期任务 1-4 + 中期 5/7/8）——✅ 已实施（2026-09-16，详见 devlog v11.95）

| # | 任务 | 要点 | 风险控制 | 实施状态 |
|---|---|---|---|---|
| 1 | **场景表提示词废弃**（v4 §11.1-2/3） | 直绑模型/知识库/工具/提示词模板字段标废弃（DDL 注释 + 代码 @Deprecated 一个版本）；6 场景全量核对 agent 绑定；KnowledgeQaHandler 提示词迁 agent | 增量 ALTER 注释、不删列；逐场景迁移回归 | ✅ **用户裁决加严：场景表系统提示词彻底废弃（非兜底）**——buildSystemPrompt 恒返 null，人设统一走 ai_agent.system_prompt；场景行 agent_id 空时 loadAgent 空壳兜底 |
| 2 | **Chat 走 default_chat 场景**（v4 §9.2） | 新增 default_chat 场景（dynamicAgent:true）；DynamicChatService 前置网关限流/日志，agent 动态指定逻辑不变 | /ai/chat 行为不变，仅加治理 | ✅ ChatController /stream+/regenerate 前置限流（60次/3600s，场景行未部署不限流）+ ai_execute_log；问候语不占额度；Agent 动态指定/RAG/工作流不动 |
| 3 | **面试主干网关化**（裁决#2） | 网关新增流式直通：executeStream 透传 SSE（限流+Token熔断+日志，不碰消息体）；InterviewAgentClient.chatStream 改走网关 | 分流灰度：sys_config 开关控制直连/网关双跑一周 | ✅ **落地形态调整**：灰度键 `ai.gateway.interview.enabled`（缺省 false=直连）开启后 chatStream 前置网关治理（voice_interview 场景限流+Token熔断+ai_execute_log，handler=interviewMainTrunk），滑窗消息体与模型调用链路不变（T2 生产方案：不引 SSE 回调桥接，直连保性能）；治理异常降级放行；顺带落地 T2 承诺的模型客户端实例缓存（configId:temp:maxTokens:jsonMode，updateById 清除） |
| 4 | **结构化输出升级**（v4 §6） | 模型表加 supports_json_mode 字段；网关 responseFormat 按场景 output_schema 下发；parse 失败自动降级 Prompt 约束 | 模型不支持时回退 ★★ 路径（v4 §6.5 兜底表） | ✅ DDL+实体+管理端开关；AiSceneResolver 按场景 output_schema 请求 jsonMode（OpenAI 兼容 response_format=json_object / Ollama format=json）；AbstractAiSceneHandler.chatJson 解析失败自动重试一次（12 处结构化子任务切换，纯文本子任务不切） |
| 5 | 面试多 task 统一确认 | warmup/answer_analysis/candidate_ask/knowledge_desc 四 task 共用 agent48（3.2 后即达成） | — | ✅ 现状已达成，零改动 |

**部署清单**：SQL `20260916-03/04/05/06`（-03 字段废弃标注 / -04 default_chat 场景行 / -05 灰度键缺省 false / -06 supports_json_mode DDL）；管理端模型页按模型能力开启 JSON Mode；灰度键保持 false 即历史行为。

## 3.4 长期 · v11.96+（v4 中期 6/长期 9-12 + 记账扩展）

| # | 任务 | 说明 |
|---|---|---|
| 1 | **ai_tool 工具表 + 执行器** | LangChain4j @Tool 注解式；首批工具：记账查询（ledger_query）、题库检索（question_search）——题库检索工具落地后面试官可面试中自主查题库（替代/增强预热 RAG） |
| 2 | **模型表多模态字段** | supports_vision/audio；账单识别场景 ledger_bill_parse（多模态+JSON Schema，v4 §8 完整流程） |
| 3 | Agent 版本管理 + 场景灰度 | 变更留痕、version/weight 生效 |
| 4 | ai_execute_log 管理页 | 网关化完成后日志即全链路 |
| 5 | report task | 报告综合评审迁场景（当前规则融合留业务侧，暂缓） |

---

# 四、版本路线图（依赖关系）

```
v11.94（本周）  P0 三修 + warmup 一次调用 + RAG 预热 + 四段式段序 + 反问段 + 知识点RAG化
                 └─ 无 DDL（config_json UPDATE 即可）
v11.95（次周）  场景表字段废弃标注 + default_chat 治理 + 主干网关化灰度 + JSON Schema
                 └─ 依赖 v11.94 warmup 稳定（网关流式通道以 warmup/主干为验收载体）
v11.96+         ai_tool 工具 → 题库检索工具 → 多模态 → 账单识别场景
                 └─ 依赖 v11.95 网关化（工具调用/多模态统一挂网关）
```

## 4.1 关键指标验收

| 指标 | 目标 |
|---|---|
| start 同步段 LLM 调用数 | 保持 1 次（warmup 合并 opening） |
| 每轮首 token 延迟 | ≤ 现状 +5ms（网关化后配置走缓存） |
| 面试官重复提问率 | 0（滑窗保证，实测抽样） |
| 报告完整率 | 分数/维度/逐题/知识点/错题本 100% 非空 |
| JSON 解析失败率 | warmup/analysis <2%（重试 1 次后） |
| 死代码 | speak_text/candidate_ask/self_intro 零删除；仅 dynamicMode 残留键归零 |

## 4.2 v4 文档需回写的修正（四同步之文档）

1. §7.3 历史组装改为"滑窗（Redis MessageWindowChatMemory）承载历史对话层"
2. §7.1 task 表保留 speak_text（标注新语义：独立话术，TTS 用，question 为结构化文本）、candidate_ask（标注：sys_config 开关控制的可选 task）、self_intro（标注：待激活）；补 dynamicMode 删除注记
3. §10.1 补迁移计划注记（直绑模型路径 @Deprecated 时间表）
