# AI 语音面试统一 AI 入口重构（V3：滑窗记忆 + 流式话术 + 配置收口）

## Context

用户痛点：面试模块没有真正复用统一 AI 会话入口（/ai/chat 那套），自己每轮重拼上下文，导致：
1. **回答后等好久**：Agent 模式面试官输出"围栏 JSON"（分析+决策+话术混在一起），必须等完整生成才能继续下一轮
2. **面试官重复问题**：`buildAgentTurnMessages` 每轮从 DB 重拼历史（伪多轮），LLM 视角每轮都是新对话
3. **提示词重复拼接**：agent 系统提示词、场景表系统提示词、简历/岗位/题库任务指令多处拼接，二次会话无法沉淀
4. **前端一堆下拉列表**（面试官智能体/出题方式/岗位模板/场景/风格），应收口为后台配置

**目标架构**：规则管决策（评分/推进/追问，毫秒级）+ LLM 只管说话（流式话术）+ Redis 滑窗管记忆（复用 /ai/chat 基础设施）+ 后台管配置。

用户已确认三个决策：A. Redis 滑窗记忆；B. 话术流式+分析纯异步；C. 前端仅留岗位/JD/简历/难度。

## 批次 1：滑窗记忆层（新增 `InterviewChatMemoryService`）

新文件：`moyun-server/src/main/java/com/moyun/ext/cms/service/interview/InterviewChatMemoryService.java`

- `memoryId(interviewId)` → `"voice-interview:" + interviewId`（复用 RedisChatMemoryStore，30 天过期）
- `getMemory(interview, agent)`：`MessageWindowChatMemory.builder().id(memoryId).maxMessages(agent.maxHistoryTurns*2 ?: 40).chatMemoryStore(redisChatMemoryStore).build()`
- `initFirstTurn(interview, agent)`：滑窗为空时注入**一次**（幂等）：
  - SystemMessage = `buildAgentSystemMessage`（agent 系统提示词 + 岗位/难度/JD/风格占位符渲染）
  - UserMessage = `PromptInjectionGuard.wrapData` 包裹的【简历摘要 + 岗位JD + 预生成题单题面列表】
  - AssistantMessage = 开场白 + 首题
- `rebuildFromDb(interview, agent)`：断点续接/旧会话时滑窗为空（Redis 过期）→ 首轮注入 + 按 DB QA 逐对追加（User=转写, Assistant=speakText）重建
- 面试 finished 时清理滑窗

复用：[RedisChatMemoryStore.java](d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/ai/store/RedisChatMemoryStore.java)、[DynamicChatServiceImpl.java](d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/ai/service/impl/DynamicChatServiceImpl.java) L251-287 的构建模式。

## 批次 2：新流式轮次 `runHybridTurn`（替代围栏模式）

位置：[VoiceInterviewServiceImpl.java](d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/impl/VoiceInterviewServiceImpl.java)

```
runHybridTurn(emitter, interview, agent, qa, question, transcript, sr, latencyMs):
① 同步段（复用 runLegacyTurn ①②）：转写落库 + score 事件 + decideNextAction 规则决策
   + advanceToNextQuestion/createFollowupQa 预创建下一题（决策不等 LLM）
② 话术流式：messages = 滑窗.messages()
   + UserMessage(wrapData("候选人回答", transcript) + "请对上一回答给一句简短反馈，自然引出下一题：" + nextQuestion)
   agentClient.chatStream(agent, messages,
     onToken     → delta 事件（前端已有打字机 appendDelta），
     onComplete  → 滑窗 append(User=转写 / Assistant=话术)、qa.speakText 落库、
                   speak 事件（兜底）、data 事件(nextAction/nextQaId/nextQuestion)、end、
                   asyncAnalyzeAnswer(...) 异步深度分析，
     onError     → 降级 runLegacyTurn(scoreSent=true))
```

- `submitAnswer` 路由：agent 可用 → runHybridTurn；否则 runLegacyTurn（保留为 AI 不可用降级兜底）
- `answerCandidateQuestion`（候选人反问）同步改走滑窗 + chatStream
- `resumeInterview`（断点续接）调用 `rebuildFromDb`
- SSE 协议零新增：score → delta* → speak → data → end，前端 parseSseBlock 已全兼容
- **下一题题面由后端结构化控制**（题单预生成 questionPaper 保留）→ LLM 不自由出题 → 彻底消除重复问题

## 批次 3：删除清单（化繁为简）

废弃：`buildAgentTurnMessages`（每轮重拼）、`runAgentTurnStream`（围栏模式）、`finishAgentTurn`、`InterviewTurnResult` + 围栏解析、`generateNextQuestion` 的 LLM 出题分支、`speak_text` 场景子任务（话术改流式直出）。
保留：`runLegacyTurn`（降级）、`asyncAnalyzeAnswer` + `answer_analysis` 场景子任务（异步分析）、`buildContextualSystemPrompt` 收敛并入 `buildAgentSystemMessage`。

## 批次 4：前端收口 + 分句 TTS

- [voiceInterview.ts](d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/voiceInterview.ts) `VoiceStartConfig`：删 agentId/dynamicMode/jobTemplateId/scene/style，保留 position/jobRequirements/resumeId/difficulty/questionCount
- [VoiceInterviewPage.vue](d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/VoiceInterviewPage.vue) L2068-2136 设置面板：删面试官智能体/出题方式/岗位模板/场景/风格五个下拉及 agentList/jobTemplateList 拉取逻辑
- **delta 分句 TTS**：appendDelta 累积文本遇 。！？；切句立即入 TTS 队列；speak 事件仅在整轮无 delta（降级路径）时整段播报（hasDelta flag 控制，防重复播报）；说完最后一句才重新开麦（TTS 队列空判断）
- data 事件到达即渲染下一题（不等 end）

## 批次 5：配置与四同步

- sys_config 新增：`voice.interview.defaultScene`、`voice.interview.defaultQuestionCount`、`voice.interview.memoryWindowTurns`（默认 20）；`voice.interview.defaultAgentId` 已有
- SQL 增量脚本（sys_config INSERT，不动原语句）+ devlog v11.92 + V2 实施文档补充 V3 章节；菜单无变更

## 验证

1. 批次 1/2 各自 `mvn compile`（全路径 D:\dev_en\apache-maven-3.9.14\bin\mvn.cmd）+ 后端启动
2. 实测面试流程：回答提交 → delta 首字到达时延明显（不等分析）；面试官不重复问题；话术自然非模板
3. 断点续接：面试中刷新 → 继续面试 → 上下文连贯（rebuildFromDb）
4. 降级链路：AI 关闭 → runLegacyTurn 完整可用（speak 整段播报）
5. 前端 `vue-tsc + vite build`；准备页仅剩岗位/JD/简历/难度

## 风险与对策

① 旧进行中会话无滑窗 → rebuildFromDb 兜底；② LLM 话术夹带题面与预创建不一致 → data 事件 nextQuestion 为准覆盖；③ 分句 TTS 与自动聆听衔接 → TTS 队列空才开麦；④ 降级路径无 delta → speak 事件整段播报保留
