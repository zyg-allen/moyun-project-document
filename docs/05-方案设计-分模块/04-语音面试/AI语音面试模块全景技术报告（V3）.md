# AI 语音面试模块全景技术报告（V3 · 统一 AI 入口纯 Agent 自由面试）

> **版本基线**：v11.93（2026-09-16）
> **报告范围**：预热阶段设计方案（含代码结构/Prompt 模板/数据流转）+ 全模块表结构、业务流、数据流、Java 类、节点核心逻辑、统一 AI 入口调用矩阵、场景化、前端核心代码与参数、优缺点、存在问题、拓展方向、性能/安全/延迟、遗漏点
> **代码基线**：moyun-server（Spring Boot 3 + MyBatis-Plus）、moyun-portal（Vue3 + TS）

---

# 第一部分：预热阶段设计方案（待实施）

> 定位：把"理解成本"前置——会话开始时一次性完成 **简历 + JD + 题目知识库（RAG）** 的理解，产出显式"面试理解"（候选人画像 + 考察方向），常驻滑窗上下文；后续问答零重拼、零检索。

## 1.1 设计原则

| 原则 | 落地方式 |
|---|---|
| 理解成本前置 | start() 同步段一次 LLM 调用生成"面试理解"（前端已有"准备中"进度条掩盖等待） |
| 知识库只在前置介入 | 预热时 RAG 检索 top-K 题目文档片段 → 拼进 warmup 输入；面试中不检索（保持流式零延迟） |
| 不回到规则决策 | 产出的"考察方向"是**提示上下文**而非题单，agent 仍自由追问；不落"必须问第 N 题"约束 |
| 失败可降级 | warmup LLM/检索失败 → 无 plan 直接走现有链路，不阻塞开面 |

## 1.2 Java 类设计

```
com.moyun.ext.cms.service.interview
├── InterviewWarmupService          【新增】预热编排：检索+理解生成+降级
├── InterviewChatMemoryService      【现有】滑窗（不变，首轮注入内容多一段）
├── InterviewAgentClient            【现有】chat/chatStream（warmup 用同步 chat）
└── impl/InterviewWarmupServiceImpl

com.moyun.ext.cms.domain.vo
└── InterviewWarmupPlan              【新增】面试理解产物（内存对象 + 序列化进 configJson）
```

### InterviewWarmupService 接口

```java
public interface InterviewWarmupService {

    /**
     * 预热：检索题目知识库 + 生成面试理解（候选人画像 + 考察方向）
     * 任何内部失败均降级返回 null（不阻塞开面）
     */
    InterviewWarmupPlan buildPlan(PortalVoiceInterview interview, Agent agent,
                                  String resumeDigest, String jobRequirements);

    /** plan → system 提示词追加段（常驻滑窗） */
    String renderSystemSection(InterviewWarmupPlan plan);
}
```

### InterviewWarmupPlan 结构

```java
@Data
public class InterviewWarmupPlan {
    /** 候选人一句话画像（LLM 从简历摘要提炼） */
    private String candidateDigest;
    /** 考察方向（3-5 个，来自 JD + 简历 + 题库检索） */
    private List<FocusArea> focusAreas;
    /** 题库检索命中片段（预热注入，供面试官取材） */
    private List<String> questionSnippets;

    @Data
    public static class FocusArea {
        private String area;    // 方向名，如"Redis 缓存一致性"
        private String reason;   // 为什么考察（JD要求/简历项目涉及/题库高频）
        private String depth;    // 预期深度 basic/intermediate/deep
    }
}
```

## 1.3 Prompt 模板（两段式，一次调用）

### 输入拼装（InterviewWarmupServiceImpl 内部）

```
[System] = agent.systemPrompt（复用 buildInterviewerSystemPrompt 的人设部分）
[User]   = wrapData("候选人资料", 简历摘要 + JD)
           + wrapData("题库参考片段", RAG top-K 检索结果，每段截断 200 字，总量 ≤ 2000 字)
           + 任务指令（见下）
```

### 任务指令模板

```
请基于以上候选人资料与题库参考，为本场「{position}」面试制定理解摘要，只输出 JSON：
{
  "candidateDigest": "候选人一句话画像（技术栈+经验层级+亮点，40字内）",
  "focusAreas": [
    {"area": "考察方向（来自JD或简历项目）", "reason": "考察理由", "depth": "basic|intermediate|deep"}
  ]   // 3-5 个，覆盖岗位核心要求；有简历时至少 1 个方向锚定其项目经历
}
不要输出 JSON 以外的任何内容。
```

### system 追加段（renderSystemSection，首轮注入滑窗后常驻）

```
【本场面试理解】
候选人画像：{candidateDigest}
考察方向（围绕这些方向提问，可按回答情况自由追问，不必全部覆盖）：
1. {area}（{reason}，预期深度：{depth}）
...
题库参考（可从中取材组织问题，禁止逐字照搬）：
- {snippet 摘要}
```

## 1.4 数据流转

```
VoiceStartConfig(5字段)
   │
   ▼
start() ──► agent 解析(sys_config:48)
   │
   ├─► buildResumeDigest(resumeId)          简历摘要（现有）
   ├─► RagRetrievalService.retrieve(        【新增】按 agent 绑定知识库
   │        query = position + 简历项目关键词,   检索 top-K=5 题目片段
   │        agent.knowledgeBaseIds)
   ▼
InterviewWarmupService.buildPlan(...)
   │  LLM 同步调用（agentClient.chat，任务指令如上）
   │  失败/超时(10s) → 返回 null → 降级走现有链路
   ▼
InterviewWarmupPlan
   ├─► 存 interview.configJson.warmupPlan（断点续接时无需重算，重建滑窗可直接复用）
   ├─► systemPrompt += renderSystemSection(plan)   常驻滑窗
   ▼
generateOpening()（现有，一次 LLM）→ initFirstTurn()（滑窗首轮注入）→ 首题 QA 落库
```

## 1.5 改造点清单（预估 ~150 行）

| 文件 | 改动 |
|---|---|
| [VoiceInterviewServiceImpl.java](../../../../../moyun-server/src/main/java/com/moyun/ext/cms/service/impl/VoiceInterviewServiceImpl.java) `start()` | buildResumeDigest 后插入 `warmupService.buildPlan(...)`；systemPrompt 追加段；configJson 增 warmupPlan 键 |
| 同上 `rebuildMemoryIfNeeded()` | 优先从 configJson 读 warmupPlan 恢复 system 追加段（避免二次 LLM） |
| 新增 `InterviewWarmupService(+Impl)`、`InterviewWarmupPlan` | 如 1.2/1.3 |
| SQL | **无 DDL**（warmupPlan 存 config_json text 字段内） |
| 报告增强（可选二期） | `buildKnowledgePoints` 改用 RAG 检索替换题库 tags 聚合（当前 V3 恒空，见 8.3-P3） |

---

# 第二部分：当前版本全景

## 2.1 表结构（全部相关）

### 2.1.1 核心业务表

**portal_voice_interview（会话主表）** — 建表 v10.1 + V2 增量（20260915-05）

| 字段 | 类型 | 说明 / V3 现状 |
|---|---|---|
| id | bigint PK | |
| user_id | bigint | 面试用户 |
| position | varchar(64) | 岗位（V3 输入） |
| scene / style | varchar(64/20) | **V3 已废弃**（历史数据兼容，不再写入） |
| resume_id | bigint | 简历 ID（项目深挖题源 → V3 转为上下文注入） |
| agent_id | bigint | 面试官 ai_agent.id（V3 由 sys_config 统一解析写入） |
| phase | varchar(30) | 阶段机残留，V3 仅写 FINISHED（finish 时） |
| intro_score_json | text | **V3 无来源**（submitSelfIntro 已删，恒 NULL） |
| status | varchar(16) | in_progress / finished |
| difficulty | varchar(20) | easy/medium/hard（V3 输入） |
| total_qa | int | 计划大问题数（V3 输入 questionCount，默认 5） |
| current_idx | int | 当前题序（V3 由 QA 行为事实推进，此字段近似冗余） |
| score | int | 面试总分（报告聚合写入） |
| summary / report | text | 报告摘要 / 报告 JSON |
| share_token / share_expire_time / share_count | | 分享令牌（uk 唯一）/ 过期 / 计数 |
| config_json | text | V3 实际键：difficulty、jobRequirements、resumeDigest（+设计的 warmupPlan） |
| is_personalized / profile_snapshot | | **V3 已废弃**（题库抽题时代产物） |
| context_snapshot | JSON | **V2 增量**：岗位/难度/JD/简历摘要快照（报告第二栏数据源） |
| question_paper | JSON | **V2 增量，V3 恒空**（题单模式产物） |
| analysis_status / analysis_progress | tinyint/int | 报告分析状态 0/1/2 + 进度 0-100（前端轮询） |
| closed_reason | varchar(50) | user / abandon（开始新面试自动收口遗留会话） |
| create_time / update_time / del_flag | | 索引：agent_id、del_flag、status、(user_id,create_time) |

**portal_voice_interview_qa（问答明细表）** — 建表 v10.1 + V2 增量

| 字段 | 类型 | 说明 / V3 现状 |
|---|---|---|
| id | bigint PK | |
| interview_id | bigint | 索引 |
| question_id | bigint | **V3 恒 NULL**（agent 自由出题，不再关联题库） |
| question_source | varchar(20) | V3 恒 `agent` |
| question_idx | int | 主问题序号（每轮 +1，跳过也占一题行） |
| parent_qa_id | bigint | 追问链，V3 未使用（追问内化在话术里） |
| question | varchar(1000) | V3 = 面试官话术全文（含反馈+下一问，报告回放用） |
| user_answer / answer_raw | text | V3 两者同值；**跳过题均 NULL** |
| answer_time | datetime | V2 增量：提交时刻 |
| transcription_edited | tinyint | 转写编辑标记（前端可编辑答案） |
| ai_feedback / speak_text | text | ai_feedback=V2 逐题分析评语；speak_text=面试官话术（V3 与 question 同源） |
| score / score_draft | int | score=融合后分；score_draft=V2 增量草稿分（批量分析 LLM 写回） |
| rule_dimensions_json | text | **V3 恒空**（实时规则评分已删） |
| llm_score_json / llm_analysis_json | text | 批量分析写回：scores/total/comment；flaws/level/guidance/redFlags |
| analysis_status | tinyint | V2 增量：单题分析状态 |
| hint_used | int | 提示次数 0~3 |
| latency_ms | int | 答题耗时 |
| next_action | varchar(20) | **V3 已废弃**（决策动作残留） |
| 索引 | | del_flag、interview_id、parent_qa_id、(interview_id,question_idx) |

**portal_voice_interview_event（会话事件日志表）** — v11.88 新建（"系统日志"）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| interview_id | bigint | 索引 |
| event_type | varchar(50) | V3 实际写入：`start` / `answer` / `skip` / `next` / `wrap_up` / `finish` / `close` / `resume` |
| event_data | JSON | 事件数据（agentId/qaId/latencyMs/roundDone/closedReason 等） |
| create_time | datetime | |

### 2.1.2 关联表

| 表 | 用途 | V3 关系 |
|---|---|---|
| portal_user_resume | 简历（projects JSON → 摘要、name/skills/selfIntro/score → 报告第一栏） | start 读取 + 报告读取 |
| ai_agent（Agent） | 面试官人设/模型路由/temperature/maxTokens/maxHistoryTurns/knowledgeBaseWeights | sys_config `voice.interview.defaultAgentId=48` 绑定 |
| ai_model_config | 模型配置（agent.modelConfigId → ModelConfigService 创建裸模型） | 每次调用动态创建 |
| ai_scene_config | 场景配置中心：scene_code=voice_interview，可绑 agent/模型/知识库/工作流，灰度权重 | 分析链路经此路由 |
| ai 相关 Redis | chatMemory 键（`voice-interview:{id}`，30 天过期） | 滑窗存储 |
| portal_interview_question | 题库（V3 主链路已不消费；/hint?questionId=、/keywords 演示页仍在用） | 边缘保留 |
| portal_interview_config | 面试评分权重配置（scoringWeights：llmRatio 70%、intro/tech 融合权重） | 报告聚合读取 |
| portal_job_template | 岗位模板 | **V3 已不消费**（端点已删） |
| sys_config | `voice.interview.defaultAgentId=48`；`voice.interview.dynamicMode`（残留键，V3 不读） | 面试官收口配置 |

## 2.2 业务流（完整步骤）

```
① 准备页（/interview/voice）
   ├─ 噪声检测（环境检查）+ 设备自检
   ├─ 5 项配置：岗位 / JD / 简历 / 难度 / 题数
   └─ GET /active 探测未完成会话 → 有则显示续接横幅（继续 / 放弃并生成报告）
        │
② POST /start（同步，~一次 LLM 调用）
   校验 → agent 解析(sys_config:48) → 简历摘要 → closeStaleInterviews(遗留会话自动结束+异步报告)
   → 会话落库(configJson+contextSnapshot) → system 提示词 → 同步生成开场白+首题
   → 滑窗首轮注入(system+context+opening) → 首题 QA 落库 → recordEvent("start")
        │
③ 面试循环（每轮）
   ├─ ASR 录音转写（useInterviewRecorder，可编辑） / 打字输入
   ├─ POST /{id}/answer（SSE，skip 可选）
   │    回答落库 → 滑窗追加 UserMessage → 每轮指令(进度 x/N)
   │    → chatStream 流式输出：delta 增量（打字机+分句TTS）
   │    → onComplete：话术入滑窗+落库 → 未满：预创建下一题 QA → end{nextQaId,nextQuestion}
   │                    已满：end{finished:true}
   ├─ POST /{id}/hint：agent 滑窗一句思考引导（0~3 次）
   ├─ 连续跳过 3 次 → 前端自动触发结束
   └─ 意外关闭：beforeunload 确认 → 会话保持 in_progress（刷新后回①探测）
        │
④ POST /{id}/finish（同步 <200ms）
   状态收口 finished + closed_reason=user → 清滑窗 → 触发异步批量分析 → 返回报告骨架
        │
⑤ 异步批量分析（aiTaskExecutor，前端 5s 轮询 /{id}/analysis 进度条）
   逐题 analyzeAnswerByLlm（场景网关）写 scoreDraft/llmScoreJson/llmAnalysisJson（进度至 80%）
   → aggregateAndStoreReport：分数融合 → 维度聚合 → 报告三段式 → 错题本 → 场景工作流
   → analysisStatus=2 / progress=100
        │
⑥ 报告页：总分/维度/亮点薄弱/逐题点评/相关知识点/心态趋势/红疑信号/对话回放
   ├─ POST /{id}/share 生成令牌 → GET /shared/{token} 匿名查看（过期/计数）
   └─ 我的面试记录（/interview/voice/history）
```

## 2.3 数据流（三条通道）

```
通道1 对话主干（agent 直连流式）
  用户回答 ──► portal_voice_interview_qa（先存原始）
          ──► Redis 滑窗 voice-interview:{id}（UserMessage）
          ──► InterviewAgentClient.chatStream ──► Agent(ai_agent) → ModelConfigService → LLM
          ◄── delta 流式话术（SSE）──► 前端打字机 + 分句 TTS
          ──► 话术 AiMessage 入滑窗 + qa.speakText 落库 + 下一题 QA 预创建

通道2 报告分析（ai2 场景网关，异步）
  DB qaList ──► AiSceneJsonClient.executeForJson(scene=voice_interview,
                  task=answer_analysis, {context, transcript}) ──► ai_scene_config 路由
              ──► LLM JSON ──► scoreDraft/llmScoreJson/llmAnalysisJson 写回 ──► 聚合 report

通道3 记忆（Redis 滑窗，统一 AI 会话机制复用）
  首轮: system + context user + opening assistant（一次性）
  每轮: + user 回答 + assistant 话术
  断点续接: 滑窗在→直接复用；过期→DB QA 逐对重建（system+context 按 start 同源重注入）
  结束: clear
```

## 2.4 Java 类清单

### 后端（moyun-server）

| 类 | 职责 |
|---|---|
| [PortalVoiceInterviewController](../../../moyun-server/src/main/java/com/moyun/portal/controller/PortalVoiceInterviewController.java) | 14 个端点：start/answer(SSE)/hint/finish/analysis/share/shared/list/active/resume/{id}/by-qa/{qaId}/asr/hint?questionId=/keywords + admin 三端点；RateLimiter（answer 60/h、hint 30/h） |
| [IVoiceInterviewService](../../../moyun-server/src/main/java/com/moyun/ext/cms/service/IVoiceInterviewService.java) | V3 接口：start/submitAnswer(SSE,skip)/requestHint/finish/getAnalysisStatus/createShareToken/getSharedReport/listMy/getActiveInterview/resumeInterview/getDetail/getDetailByQaId/adminList/adminGetDetail/adminDelete |
| [VoiceInterviewServiceImpl](../../../moyun-server/src/main/java/com/moyun/ext/cms/service/impl/VoiceInterviewServiceImpl.java) | 核心实现（v11.93 后约 1640 行）：start/buildInterviewerSystemPrompt/generateOpening/submitAnswer/runAgentTurn/buildTurnDirective/countAnsweredRounds/requestHint/finish/triggerBatchAnalysis/runBatchAnalysis/updateAnalysisProgress/aggregateAndStoreReport/analyzeAnswerByLlm/buildCandidateProfile/buildJobInfo/buildSummary/buildSuggestion/buildImprovementSuggestions/buildKnowledgePoints/tryLlmKnowledgeDesc/recordWrongQuestionsQuietly/triggerSceneWorkflowAsync/getActiveInterview/resumeInterview/rebuildMemoryIfNeeded/readConfigKey/closeStaleInterviews/getDetail*/share*/admin*/toVO/toQaVO/assembleVO/recordEvent/sendEvent/toJson/mustOwnInterview/buildResumeDigest |
| [InterviewChatMemoryService](../../../moyun-server/src/main/java/com/moyun/ext/cms/service/interview/InterviewChatMemoryService.java) | 滑窗：memoryId 规则、getMemory（maxHistoryTurns×2，默认 40 条）、initFirstTurn（幂等）、rebuildFromDb、clear |
| [InterviewAgentClient(Impl)](../../../moyun-server/src/main/java/com/moyun/ext/cms/service/interview/impl/InterviewAgentClientImpl.java) | agent 解析（sys_config 优先）、chat 同步、chatStream 流式（流式模型自动路由 + 同步模拟流式兜底）、模型路由 agent.modelConfigId→ModelConfigService |
| [RedisChatMemoryStore](../../../moyun-server/src/main/java/com/moyun/ext/ai/store/RedisChatMemoryStore.java) | LangChain4j ChatMemoryStore 的 Redis 实现（30 天过期，统一 AI 会话共用） |
| HintEngine(Impl) | 题库分级提示（仅 /hint?questionId=、/keywords 演示页用） |
| ScoringEngine | 规则评分/分数融合（fuseAnswerScore/fuseTotalScore，报告聚合用） |
| AnswerScoringEngine | ScoreResult 结构（批量分析载体；6 维关键词对齐） |
| InterviewTurnResult | LLM 深度分析 JSON 反序列化（sentiment/redFlags/fluency 聚合用） |
| InterviewPhase | 阶段枚举（仅 finish 写 FINISHED） |
| VO：VoiceStartConfig(5字段)/VoiceInterviewVO/VoiceInterviewQaVO/VoiceInterviewReportVO/HintVO | 传输对象 |
| Entity：PortalVoiceInterview/PortalVoiceInterviewQA/PortalVoiceInterviewEvent/PortalUserResume | 表实体 |

### 前端（moyun-portal）

| 文件 | 核心内容 |
|---|---|
| [voiceInterview.ts](../../../moyun-portal/src/api/voiceInterview.ts) | VoiceStartConfig{position,jobRequirements,resumeId,difficulty,questionCount}；SseCallbacks{onDelta,onEnd(payload),onError,onAborted}；parseSseBlock（仅 delta/end/error）；submitVoiceAnswer(.., skip) |
| [VoiceInterviewPage.vue](../../../moyun-portal/src/pages/interview/VoiceInterviewPage.vue) | 三阶段 phase：setup/interview/report。核心：噪声检测、checkActiveInterview/handleResume 续接横幅、startVoiceInterview、buildAnswerCallbacks（delta 打字机 + ttsSentenceBuffer 分句 TTS）、handleSkip（skip:true 同 SSE 路径 + skipStreak 3 次）、handleHint（hint.speakText 单句）、handleFinish→pollAnalysisOnce（5s 轮询）、报告三段式渲染、beforeunload 防误关 |
| MyVoiceInterviewsPage.vue | 历史列表（报告查看入口） |
| VoiceEngineDemoPage.vue + useInterviewHint.ts | 题库演示页（/hint?questionId=、/keywords，与主链路解耦） |

## 2.5 节点核心逻辑 × 统一 AI 入口调用矩阵

| 节点 | 核心逻辑 | 统一 AI 入口？ | 说明 |
|---|---|---|---|
| start 开场 | 一次同步 chat：system+context+开场指令 → 开场白+首题 | ✅ agent 通道 | agent 模型路由直连（非 /ai/chat 网关），复用统一 agent 配置与模型管理 |
| runAgentTurn 每轮 | 滑窗全量 + 每轮指令 → chatStream 流式 | ✅ agent 通道 | 同上；上下文机制复用统一会话 Redis 滑窗 |
| requestHint | 滑窗 + 提示指令 → 同步 chat | ✅ agent 通道 | |
| analyzeAnswerByLlm | aiSceneJsonClient(scene=voice_interview, task=answer_analysis) | ✅ 场景化网关（ai2） | 走 ai_scene_config 路由（可绑 agent/模型/工作流/灰度），执行日志落 ai 侧 |
| tryLlmKnowledgeDesc | task=knowledge_desc | ✅ 场景化网关 | |
| triggerSceneWorkflowAsync | 场景绑定工作流异步触发 | ✅ 场景化 | 报告归档/学习计划扩展点 |
| **RAG 检索** | — | ❌ **未接入** | agent 绑定知识库在面试链路不生效（InterviewAgentClient 直连裸模型）；预热方案补齐（见第一部分） |
| 评分 | ScoringEngine 规则 + LLM 融合（llmRatio 70%） | 部分 | 报告阶段规则融合 |

**结论**：V3"统一 AI 入口"= ①agent 配置收口（人设/模型/记忆窗口统一在 ai_agent）+ ②统一会话上下文机制（Redis 滑窗）+ ③分析链路场景化网关（ai_scene_config）。主干对话走 agent 直连流式通道以保证延迟，**未走** /ai/chat 的 DynamicChatService 管道（该管道含意图识别/RAG/引用追踪，与面试流式协议耦合成本高——设计取舍，非遗漏）。

## 2.6 前端核心代码与参数

```typescript
// 开始（仅 5 参数）
startVoiceInterview({ position, jobRequirements, resumeId, difficulty, questionCount })

// 作答（SSE）
submitVoiceAnswer(interviewId, qaId, transcript, latencyMs, {
  onDelta: (t) => { appendDelta(t); feedDeltaToTts(t); },   // 打字机 + 句末标点切句入 TTS 队列
  onEnd: (p) => {                                          // {roundDone, nextQaId?, nextQuestion?, finished?}
    if (p.finished) return handleFinish();
    currentQaId.value = p.nextQaId!;                       // 轮次推进（不重复推气泡）
    resetForNewQuestion(p.nextQuestion);
  },
  onError, onAborted
});
// 跳过：submitVoiceAnswer(..., '', 0, callbacks, true)；skipStreak≥3 → 结束确认
// 提示：POST hint → {hint:{speakText}} 单句展示 + TTS
// 报告：POST finish → 轮询 GET analysis（5s）→ analysisStatus=2 拉完整报告
```

TTS：useSpeechSynthesis 自带顺序播报队列（逐句 onend 串联），说完才置 speaking=false → watch 开麦，天然保证"全部说完才重新开麦"。

## 2.7 优点

1. **体验质变**：delta 流式首字即显 + 分句 TTS 边生成边播报，彻底消除"回答后干等分析"；分析全部后置异步。
2. **不重复提问**：滑窗完整上下文 + 首轮一次性注入，二次会话零重拼——直击旧版最大痛点。
3. **理解成本前置**：system+context 常驻滑窗（等价于"每轮注入计划"的效果，成本 O(1)）。
4. **代码瘦身 55%**：impl 3584→约 1640 行，11 个专属类删除，5 个配置下拉收口为 5 个输入项——统一 AI 入口理念真正落地（agent/模型/记忆配置全在后台）。
5. **数据铁律保持**：回答提交即落库（answer_raw）、事件日志全链路、断点三防线（beforeunload→横幅续接→自动收口）完整保留。
6. **健壮性兜底链**：流式模型自动路由→同步模拟流式；批量分析失败→规则聚合兜底；线程池饱和→同步降级。

## 2.8 存在的问题（按优先级）

### P0（功能缺陷，建议尽快修）

1. **报告分数聚合断链**：`aggregateAndStoreReport` L827 `if (qa.getScore() == null) continue;` —— V3 无实时规则评分，qa.score 恒 NULL，批量分析只写 scoreDraft 不回写 score → **逐题点评/总分/维度聚合全空**。修法：runBatchAnalysis 单题分析成功后 `qa.setScore(analysis.score)` 一并写回（或聚合时以 scoreDraft 为准）。
2. **相关知识点恒空**：buildKnowledgePoints 依赖 questionId（题库题），V3 agent 出题恒 NULL → 报告 Tab 恒空。修法：改用 RAG 检索（见拓展）或 tags 来自 agent 话术。
3. **错题本恒空**：recordWrongQuestionsQuietly 同样依赖 questionId。

### P1（体验/一致性）

4. **interviewSelfIntro 取值错位**：报告第一栏取 `questionIdx==1` 的回答（V2"第 1 问=自我介绍"假设），V3 agent 自由发问不保证 → 时常缺失。可改为让 agent 开场固定先请自我介绍（buildInterviewerSystemPrompt 加一句约束，成本最低）。
5. **纯跳过永不问满**：done 只计有 userAnswer 的 QA，连续跳过时后端 done<total 恒成立（靠前端 skipStreak 3 次兜底结束）；后端独立视角缺保护。
6. **countAnsweredRounds 每轮 2 次 DB count**（指令+收尾），量大时可改 Redis 计数或内存缓存。

### P2（工程健壮性）

7. sseExecutor 固定 2 线程无监控/无优雅关闭，并发面试排队时首 token 延迟增大。
8. QA 表/主表残留 8 个 V3 恒空字段（scene/style/question_id/rule_dimensions_json/next_action/parent_qa_id/is_personalized/question_paper），读写无害但语义噪声。
9. `voice.interview.dynamicMode` sys_config 残留键已无消费方。
10. current_idx 字段与 QA 事实序号存在双源，弱一致。

## 2.9 性能 / 安全 / 响应延迟处理现状

**性能与延迟**
- start：一次同步 LLM（开场白+首题），前端进度条掩盖；预热方案会再加一次（可合并为一次调用输出 JSON+开场，或接受 +1 RTT）。
- 每轮：SSE 异步线程池（不占请求线程）→ 滑窗读（Redis 1 次 get）→ 流式首 token 即推 delta；**全程无逐轮 DB 分析、无逐轮重拼提示词**。
- 报告：全异步 + 进度轮询（5s）+ 三层降级（LLM 失败→规则聚合；线程池饱和→同步降级）。
- SSE_TIMEOUT 120s；流式模型不可用自动路由 + 同步模拟流式。

**安全**
- PromptInjectionGuard.wrapData 包裹简历/JD（数据不可信边界）；⚠️ 候选人 transcript 以原文入滑窗（理论上可注入面试官指令，靠 system 常驻 + 每轮指令韧性兜底，**建议也 wrapData 包裹**，改动一行）。
- mustOwnInterview 归属校验全端点；分享令牌 16-64 位白名单 + 过期 + 计数；RateLimiter 关键端点限流。
- 报告含 PII（姓名/简历内容），分享链路无脱敏——若对外分享需评估。

**遗留风险**
- Redis 滑窗 30 天过期 vs 断点续接：已有 DB 重建兜底，闭环。
- agent 未配置时 start 直接报错（提示管理员配置 sys_config）——fail-fast 正确。

## 2.10 拓展方向

1. **预热注入（本期设计）**：第一部分方案——RAG 题库 + 面试理解（画像+考察方向）前置，agent 提问从"发散"变"有锚"。
2. **报告知识库增强**：buildKnowledgePoints/错题本改走 RAG（题目文档 tags 检索），一并解决 P0-2/P0-3。
3. **语音体验**：TTS 打断（barge-in，用户开口即停播）、实时字幕流式修正、情绪/语速分析入报告。
4. **岗位能力模型**：JD → 能力项映射，报告给"能力雷达 + 达标建议"，与考察方向闭环。
5. **多面试官**：按岗位路由不同 agent（sys_config 按岗位字典扩展），人设差异化（压力面/HR 面/技术面）。
6. **学习闭环**：低分方向 → 场景工作流生成学习计划 → 错题本 → 复盘面试（triggerSceneWorkflowAsync 已预留钩子）。
7. **数据运营**：事件表已含全链路数据，可出"平均轮次延迟/跳过率/重复提问率"看板（配合 Redis 计数埋点）。

## 2.11 遗漏点核查清单（本次评审发现）

| # | 遗漏点 | 影响 | 建议 |
|---|---|---|---|
| 1 | 报告分数断链（score 恒 null 跳过） | P0 报告无分 | runBatchAnalysis 写回 qa.score |
| 2 | 知识点/错题本依赖题库 ID 失效 | P0 报告 Tab 空 | RAG 化或移除入口 |
| 3 | RAG 未接面试链路 | 提问发散 | 预热方案（第一部分） |
| 4 | transcript 未 wrapData | 提示词注入面 | 包裹（一行） |
| 5 | interviewSelfIntro 假设失效 | 报告第一栏缺口头介绍 | system 加"先请自我介绍"约束 |
| 6 | 纯跳过后端无终止保护 | 极端场景无限循环 | done 计数改为"已消耗 QA 行数" |
| 7 | 动态出题 sys_config 残留 | 配置噪声 | 清理键 + 菜单文案 |
| 8 | intro_score_json/phase 等历史字段 | 语义噪声 | 下个大版本清理（含 DDL） |

---

**结论**：V3 主链路（滑窗+流式+收口+断点续接）已达"统一 AI 入口纯 agent 自由面试"的设计目标，体验痛点（重复提问/等待分析/配置冗余）已消除；剩余问题集中在**报告侧 V2 遗产与新链路的数据断点**（P0 三项）与 **RAG 未接入**，其中报告分数断链建议立即修复，RAG 走预热方案落地。
