# 语音面试 Agent 化深度改造实施计划

## Context

用户反馈语音面试体验"死板、不连贯、AI 使用不完善"。代码证据：`start()` 一次性预生成固定题单存 configJson（题库轮询而非会话）；`submitAnswer()` 每题独立调用 LLM（无对话历史）；提示词硬编码在 `VoiceInterviewServiceImpl`；无心态感知/流畅度/漏洞分析；无联网能力。

目标：参考后台已实现的智能体会话（`DynamicChatServiceImpl` + `ai_agent` 表），把语音面试改造成"一个面试一个会话"的流式智能体——面试官人设/模型/提示词后台可配动态生效，多轮上下文记忆，动态出题随机应变（深挖/换题/收尾），扩展心态感知与漏洞分析，前端流式打字体验。

**用户已确认：Phase 0-4 全部一次做完。**

## 关键架构决策

| 决策点 | 结论 |
|---|---|
| 模型路由 | **不走 `LlmClient`/`LLMService`**（只支持默认模型单串 prompt，已验证 `LLMServiceImpl.java:57-67`）。新建 `InterviewAgentClient` 直连 `AgentService` + `ModelConfigService.createChatModel(configId, temperature, maxTokens)`，落实 agent.modelConfigId/temperature/maxTokens |
| Agent 发现 | `sys_config` 键 `voice.interview.defaultAgentId`（admin 参数设置页可直接改）；agent 编辑后 `AgentServiceImpl` 自动清 Redis 缓存 → 动态发布免费获得 |
| 会话记忆 | 每次调用从 qa 链现建 `List<ChatMessage>`（assistant=问题话术，user=回答，每条截断 600 字，最近 N 轮），不引入 ai_conversation 表 |
| LLM 输出协议 | **单次调用**：前段自然语言（可流式播报）+ 后段 ```json 围栏块（结构化分析+出题决策）。解析三级兜底：围栏 → 整段 readTree → null 降级规则 |
| nextAction 语义 | `deepen/change_topic/wrap_up`，后端 `InterviewDecisionPolicy` 硬约束裁决（预算/链深/轮数），LLM 只有建议权；SSE 保留旧值映射（deepen→followup 等）兼容旧前端 |
| 题库联动 | 动态出题注入候选题（含 candidateId），LLM 采用则回写 `qa.questionId` → 保住错题本联动 |
| 联网搜索 | 只建 `WebSearchService` 接口 + Noop 实现 + 开关；DashScope OpenAI 兼容模式无法传 `enable_search`（已验证），真实实现需 dashscope-sdk-java 显式依赖或三方搜索 API，记录在接口 Javadoc |
| 降级矩阵 | LLM 挂/agent 禁用/解析失败 → preset 模式 = 现状行为；dynamic 模式 = 兜底题队列 + 规则 decideNextAction。每层只 warn 不抛错 |

## Phase 0：基建（纯增量，无行为变化）

**DDL** 新文件 `moyun-server/src/main/resources/sql/20260906-moyun-voice-interview-agent.sql`：
- `portal_voice_interview` + `agent_id bigint NULL`（idx_agent）
- `portal_voice_interview_qa` + `question_source varchar(20)`（bank/resume_project/llm）、`llm_analysis_json text`
- INSERT 预置面试官 agent 到 `ai_agent`（systemPrompt 骨架见下文，temperature 0.7，maxTokens 2048，maxHistoryTurns 20，enabled=1）
- INSERT `sys_config`：`voice.interview.defaultAgentId`、`voice.interview.dynamicMode`（true）

**新建类**（包 `com.moyun.ext.cms.service.interview`）：
- `InterviewAgentClient`（接口）+ `impl/InterviewAgentClientImpl`：`resolveAgent(agentId)`（入参→sys_config 默认→null）；`chat(Agent, List<ChatMessage>)` 同步（`modelConfigService.createChatModel(agent.modelConfigId, agent.temperature, agent.maxTokens)`，未配模型回退默认）；`chatStream(...)` 流式回调（仿 `LLMServiceImpl.generateStream`）；异常全吞返回 null 供降级
- `InterviewPromptAssembler`：`renderSystemPrompt(Agent, interview, ctx)` 占位符 `{{key}}` 正则替换（缺失渲染"无"）；`buildHistory(interview, maxTurns)` qa 链→消息列表；`buildTaskDirective(...)` 任务协议+转写+题库候选+进度注入
- `InterviewAnalysisParser`：围栏→裸 JSON→null 三级解析，字段容错默认（score 缺省回规则分）
- `InterviewTurnResult`（DTO）：reply/score/dimensions/feedback/flaws/redFlags/sentiment/fluencyAssessment/completeness/level/followupWorth/nextAction/nextQuestion/candidateId/transition/guidance
- `InterviewDecisionPolicy`：纯函数裁决（规则见 Phase 2）

**实体**：`PortalVoiceInterview` + agentId；`PortalVoiceInterviewQA` + questionSource/llmAnalysisJson（驼峰自动映射，无 XML 改动；实施前确认 mapper 无手写 resultMap）

**单测**：`InterviewPromptAssemblerTest`、`InterviewAnalysisParserTest`、`InterviewDecisionPolicyTest`（≥12 用例矩阵）

## Phase 1：Agent 绑定 + 多轮上下文 + 扩展分析

改 `VoiceInterviewServiceImpl`：
1. `start()`：`VoiceStartConfig.agentId` → resolveAgent → `interview.setAgentId()`；configJson 写 `questionMode: "preset"`（本阶段仍预生成题单）
2. `analyzeAnswerByLlm()` 重写为委托 `InterviewAgentClient`：消息 = `[SystemMessage(渲染 agent 提示词+任务协议)] + buildHistory()`——**多轮记忆生效点**；失败回退旧 `buildContextualSystemPrompt()` 单轮调用（旧代码保留为降级路径）
3. `submitAnswer()`：analysis 换 `InterviewTurnResult`；`qa.setLlmAnalysisJson()` 持久化；SSE `data` 事件新增 `analysis`/`agentAction`/`transition` 字段，保留旧 `nextAction` 映射
4. `generateSpeakText()`：agent 模式直接用 `InterviewTurnResult.reply`（省一次调用），失败回退规则话术

VO 增量：`VoiceStartConfig`+agentId/dynamicMode；`VoiceInterviewVO`+agentId/agentName/questionMode；`VoiceInterviewQaVO`+questionSource

## Phase 2：动态出题（状态机上线）

1. `start()` 动态分支（`questionMode="dynamic"`，sys_config 或 start 参数控制）：
   - 不预生成题单；一次 LLM 调用产出开场白（agent.welcomeMessage 渲染，否则规则 buildGreetText）+ 首问（题库候选 topN=8 注入，已问排除）
   - `qa.questionId` = 采用候选的真实 id 或 null；`questionSource` = bank|llm；totalQa 仍 5（主问预算）
   - LLM 失败降级：现场 `pickQuestions()` 取首题 + 惰性回填 configJson.questionIds 兜底队列
2. `submitAnswer()` 决策接管（仅 dynamic）：`InterviewDecisionPolicy.resolve(llmAction, followupDepth, followupUsed, roundsDone, totalPlanned)`：
   ```
   wrap_up 合法   ⇔ roundsDone >= totalPlanned（LLM 提前收尾仅当 >= ceil(0.8*totalPlanned)）
   deepen 合法    ⇔ followupDepth < 2 && followupUsed < 4 && roundsDone < totalPlanned
   change_topic 合法 ⇔ roundsDone < totalPlanned
   拒绝降级链：deepen→change_topic→wrap_up（记录 override 原因）
   ```
   currentIdx 仅 change_topic 时 +1；candidateId 校验在本轮注入清单内才回链
3. `advanceToNextQuestion()`/`forceNext()`：dynamic 模式改为"生成下一题"（LLM 短调用或兜底队列）
4. `requestHint()`：questionId=null 时新增 LLM 提示路径（产出 HintVO 同构），失败回退复述+关键词模板；题库题继续 HintEngine

## Phase 3：流式体验（SSE delta 事件）

`submitAnswer()` 升级 `InterviewAgentClient.chatStream`：
- 回调 onPartialResponse：增量文本在 ``` 围栏起点**之前**的部分 → `emitter.send(event("delta").data(token))`；围栏后只入缓冲
- onComplete：合成全文 → 解析 → score（规则分最先）→ speak（全文）→ data → end
- SSE 发送收敛到回调线程链（天然串行）；executor 只编排启动

## Phase 4：联网接口 + 报告增强 + admin 完善

1. `WebSearchService` 接口 + `NoopWebSearchServiceImpl` + 开关 `voice.interview.webSearchEnabled`；`InterviewPromptAssembler` 预留注入点（wrap_up 前/报告阶段核查可疑事实）；Javadoc 记录实现选项（dashscope-sdk-java enableSearch / Bocha / Tavily）
2. `finish()` 增强：聚合 llm_analysis_json → 报告新增 `sentimentTrend`/`redFlags` 合并去重/`fluencyAvg`；可选 LLM 总结调用（失败回退现有 buildSummary）；`VoiceInterviewReportVO` 加可选字段（旧报告 parseReport 已容错）
3. 新增 `GET /portal/interview/voice/agents`：列出启用面试官 agent 供 portal 选择
4. admin：`views/ai/agent/index.vue` 加占位符说明 el-alert（纯文案）；`views/cms/voiceInterview/index.vue` 复盘详情展示 questionSource 徽标 + 红旗/心态摘要

## 提示词设计

**Agent systemPrompt 骨架**（预置 ai_agent，后台可改；只管"你是谁"，协议全部代码侧拼装）：
```
你是一位经验丰富的面试官，正在进行一场真实的模拟面试。
【面试设定】岗位：{{position}} 场景：{{scene}} 难度：{{difficulty}} 风格：{{style}}
【候选人背景】简历项目摘要：{{resumeDigest}} 已识别薄弱点：{{profileGaps}} 当前水平：{{levelEstimate}}
【面试守则】1.一次只问一个问题，紧密结合候选人此前的回答随机应变；2.有明显漏洞优先追问，话题充分后换新话题，主问完毕自然收尾；3.不泄露评分维度与标准答案；4.口语化自然，单次发言不超过80字。
```
占位符数据源：interview 主表（scene/difficulty 经 sceneText/difficultyText 转换）+ configJson（resumeDigest/profileGaps/levelEstimate）

**任务协议**（user 消息尾段，代码拼装）：对话历史 + 当前问题 + 转写 + 本轮任务（先口头回应≤60字，再 ```json 输出）+ nextAction 判定参考 + 题库候选（change_topic 场景）+ 进度（X/5 轮、链深、追问预算）+ 打分参考（跑题<30 / 浅层 50-65 / 框架+细节 65-80 / 深入权衡 80+）

**分析 JSON Schema 核心字段**：score、dimensions{relevance/professionalism/fluency/logic/confidence}、feedback、flaws[]、redFlags[]（答非所问/背诵痕迹/前后矛盾/夸大数据）、sentiment{state: nervous|confident|hesitant|calm, note}、fluencyAssessment{score,comment}、completeness{covered[],missing[]}、level、followupWorth、nextAction、nextQuestion、candidateId、transition、guidance

## 接口变更（全部渐进兼容）

| 接口 | 变更 |
|---|---|
| POST /start | 请求 +agentId/dynamicMode 可选；响应 +agentId/agentName/questionMode |
| POST /{id}/answer (SSE) | +delta 事件（Phase 3）；data +analysis/agentAction/transition；nextAction 保留映射 |
| 其他全部接口 | 路径签名零变化 |
| GET /agents | 新增（Phase 4） |

## 前端改造（moyun-portal）

**`src/api/voiceInterview.ts`**：SseCallbacks+onDelta；parseSseBlock+case 'delta'；data 类型 +analysis/agentAction/transition；StartConfig/VO 类型增量

**`VoiceInterviewPage.vue`**（ASR/TTS/计时器不动）：
1. 流式打字气泡：ChatMessage+streaming；onDelta 创建/追加 ai 气泡（30ms/字打字机）；onSpeak/onData 落定全文；TTS 播报延后到落定
2. agentAction 映射：deepen→追问分支；change_topic→next 分支（transition 先 push 过渡气泡）；wrap_up→report 分支
3. 分析卡片增强：心态（state 中文映射+note）、流畅度、redFlags 红标签、完整性 covered/missing
4. 顶栏显示 interview.agentName
5. 报告页：面试官剖析 tab 加 redFlags/心态趋势（字段缺失时隐藏）

## 兼容与回滚

- DDL 全可空列无删改；旧 in_progress 面试 questionMode 按 preset 处理；旧报告容错
- 回滚：sys_config `dynamicMode=false` + `defaultAgentId` 清空 → 回到改造前行为；代码回滚不影响数据
- 降级矩阵：每层 LLM 失败只 warn，行为回退规则链路

## 验证方案

1. 编译：`mvn compile -DskipTests`；前端 `vue-tsc --noEmit`
2. 单测：新增 3 个测试类全绿
3. 手动路径：
   - Phase 1：答 2 轮→后台改 agent 提示词加口癖→第 3 轮生效（动态发布）；`moyun.ai.enabled=false`→规则链路全通；刷新页面 detail 恢复
   - Phase 2：答模糊触发 deepen 引用原话；追问预算耗尽自动 change_topic；第 5 主问 wrap_up→报告；候选题回链错题本可用；模型停用→兜底队列不中断
   - Phase 3：EventStream 顺序 score→delta\*N→speak→data→end；打字机效果
   - Phase 4：报告含 sentimentTrend/redFlags；agents 接口返回列表
   - 回归：ASR 流式、TTS、90s 倒计时、卡壳提示、移动端 HTTPS 麦克风、admin 复盘、/my/list

## 关键文件

- 后端核心：`moyun-server/src/main/java/com/moyun/ext/cms/service/impl/VoiceInterviewServiceImpl.java`
- 模型路由：`moyun-server/src/main/java/com/moyun/ext/ai/service/impl/ModelConfigServiceImpl.java`（createChatModel/createStreamingChatModel）
- 参照实现：`moyun-server/src/main/java/com/moyun/ext/ai/service/impl/DynamicChatServiceImpl.java`（消息组装/流式回调）
- 前端：`moyun-portal/src/api/voiceInterview.ts`、`moyun-portal/src/pages/interview/VoiceInterviewPage.vue`
- admin：`moyun-admin-vue/src/views/ai/agent/index.vue`、`moyun-admin-vue/src/views/cms/voiceInterview/index.vue`
