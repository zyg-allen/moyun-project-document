# AI 语音面试 V3 收尾清理实施计划（统一 AI 入口 · 纯 agent 自由面试）

## 摘要

V3 主链路（`start` 滑窗初始化 + `submitAnswer → runAgentTurn` 流式话术 + 断点续接 + 结束批量分析）已落地，但存在两类残留：

1. **协议缺口（功能 bug）**：`runAgentTurn` 只发 `delta`/`end`，不创建下一题 QA、不下发 `nextQaId`；前端仍按旧 `onData(nextQaId)` 契约等一个永远不会来的事件 → 第二问提交会覆盖同一 QA 行。
2. **死代码堆积**：围栏模式（`runAgentTurnStream`/`finishAgentTurn`/`buildAgentTurnMessages`）、规则链路（`runLegacyTurn`/题单推进/评分决策）、自我介绍阶段机（`submitSelfIntro`/`routePhaseTurn`）、候选人反问、题库出题（`pickQuestions*`）、hintEngine 分级提示等约 2000+ 行死代码及其专属类。

本计划：补全 SSE 轮次协议 → 跳过/提示改走 agent 统一链路 → 彻底删除死方法与死类 → 前端配置收口 → 四同步。

## 现状关键事实（已核查）

- `submitAnswer`(L917) 已直连 `runAgentTurn`(L956)，`routePhaseTurn`/`runAgentTurnStream` **无任何调用方**（纯死代码）。
- 控制器（`com.moyun.portal.controller.PortalVoiceInterviewController`）仍暴露旧端点：`POST /{id}/self-intro`、`POST /{id}/next`、`GET /agents`、`GET /job-templates`；前端 **无页面调用** self-intro；`/agents`、`/job-templates` 仅被 VoiceInterviewPage 的多余下拉使用。
- `GET /hint?questionId=`、`GET /keywords` 被 `VoiceEngineDemoPage`（经 `useInterviewHint.ts`）使用 → **保留**，HintEngine 类保留。
- 报告链路（`analyzeAnswerByLlm`/`runBatchAnalysis`/`aggregateAndStoreReport`）依赖 `ScoringEngine`、`AnswerScoringEngine.ScoreResult`、`parseAnalysis` → **保留**。
- 前端 `VoiceStartConfig`（voiceInterview.ts L41-60）残留 9 个废弃字段；页面 L576-680 拉取 agent/岗位模板列表，L2072-2130 设置面板含场景/风格/智能体/动态出题/岗位模板五个多余下拉。

## 变更清单

### A. 后端：补全 SSE 轮次协议（修复断链，最优先）

文件：`moyun-server/src/main/java/com/moyun/ext/cms/service/impl/VoiceInterviewServiceImpl.java`

`runAgentTurn` 的 `onComplete`（当前 L984-1004）改为：

1. 话术入滑窗 + `qa.speakText` 落库（现状保留）。
2. 计算 `done = countAnsweredRounds(...)`、`total = interview.getTotalQa() ?: QUESTION_COUNT`：
   - `done < total`：创建下一题 QA（`questionSource="agent"`、`questionIdx = qa.getQuestionIdx()+1`、`question = speak` 全文，报告回放用），end 载荷追加 `nextQaId`、`nextQuestion=speak`。
   - `done >= total`（与 `buildTurnDirective` 收尾分支一致）：不再建 QA，end 载荷追加 `finished=true`。
3. end 事件统一载荷：`{roundDone, nextQaId?, nextQuestion?, finished?}`，之后 `emitter.complete()`。

### B. 后端：跳过（forceNext）收口进 answer 链路

- `PortalVoiceInterviewController` 的 `AnswerRequest` 增加 `Boolean skip`；删除 `POST /{id}/next` 端点与 `IVoiceInterviewService.forceNext`。
- `submitAnswer`：`skip=true` 时跳过空 transcript 校验；`userAnswer/answerRaw` 保持 null；`recordEvent(interviewId, "skip", ...)`；滑窗注入 `UserMessage("（候选人表示跳过本题）")`；`buildTurnDirective` 感知 skip（指令说明"候选人跳过了上一题，请简短带过并自然进入下一问"）。其余复用 `runAgentTurn` 全流程（同一 SSE 协议）。
- 删除死方法群：`forceNext`、`advanceDynamicByForce`、`advanceDynamicNextQuestion`、`advanceToNextQuestion`、`generateNextQuestion`。

### C. 后端：requestHint 重写为 LLM 提示（走滑窗，不再用 HintEngine 分级）

`requestHint(interviewId, userId, qaId)` 重写（保留接口与端点 `POST /{id}/hint`）：

- `hintUsed + 1` 落库（现状保留）。
- messages = 滑窗 messages + UserMessage："请以面试官身份给候选人一句简短的思考提示（引导方向，不泄露答案），只输出这句话。" → `agentClient.chat(agent, messages)` 同步调用。
- 返回 `HintVO{ title="思考提示", speakText=文本 }`；失败降级固定话术"可以从你熟悉的相关项目经历入手想想"。
- 删除：`generateHintByAgent`、`hintLevelTitle`、类内 `hintEngine` 注入（HintEngine 类本身保留给 demo 页端点）。

### D. 后端：删除死端点 + 死方法群（彻底删减，不保留注释尸体）

控制器删除：`POST /{id}/self-intro`、`POST /{id}/next`、`GET /agents`、`GET /job-templates`（对应 service 方法 `submitSelfIntro`、`listUsableAgents`、`listActiveJobTemplates` 一并删；`InterviewAgentClient.listUsableAgents` 若无他处引用则同删）。

`VoiceInterviewServiceImpl` 删除方法（按功能组分批删，每批后编译验证；行号为当前快照参考）：

| 组 | 方法 |
|---|---|
| 上下文重拼 | `buildContextualSystemPrompt`、`difficultyText`、`difficultyRequirement`、`sceneText`、`sceneRequirement`、`styleText`、`resolveDynamicMode`、`isDynamicInterview`、`buildPlaceholders`、`joinList`、`buildAgentSystemMessage`、`readInterviewConfig` |
| 围栏模式 | `buildAgentTurnMessages`、内部类 `AgentTurnMessages`、`runAgentTurnStream`、`finishAgentTurn`、`mapAgentAction`、`createAgentFollowupQa`、`accumulateProfileFromTurn` |
| 规则链路 | `runLegacyTurn`、`scoreAnswer`、`decideNextAction`、`createFollowupQa`、`generateFollowupQuestion`、`extractKeyword`、`countFollowupUsed`、`followupDepthOf`、`generateSpeakText`、`buildInterviewerPrompt`、`buildRuleSpeakText`、`buildGreetText`、`buildQuestionIntro`、`buildSummary`、`buildSuggestion`、`accumulateProfile`、`asyncAnalyzeAnswer`、`loadInterviewConfigQuietly` |
| 阶段机/自我介绍 | `submitSelfIntro`、`handleIntroSubmission`、内部类 `IntroTurnOutcome`、`insertSelfIntroQa`、`findSelfIntroQa`、`advanceToFirstTechQuestion`、`phaseForQuestion`、`routePhaseTurn`、`runSelfIntroTurn`、`runIntroFollowupTurn` |
| 候选人反问 | `enterCandidateAsk`、`tryEnterCandidateAsk`、`countCandidateAsks`、`runCandidateAskTurn`、`isNoMoreQuestions`、`answerCandidateQuestion` |
| 题库出题 | `usedQuestionIds`、`pickAgentCandidates`、`countRoundsDone`、`generateFirstQuestionByAgent`、`normalizeAgentQuestion`、`matchCandidate`、`resolveCandidateId`、`insertFirstQa`、`pickQuestionsByProfile`、`pickQuestionsWithResume`、`loadResumeProjects`、`extractQuestionSnapshotTitle`、`pickQuestions`、`queryQuestions`、`questionWrapper`、`queryByTag`、`queryWithSolvedExclusion`、`solvedQuestionIds`、`mergeUnique` |

保留：`analyzeAnswerByLlm`、`parseAnalysis`、`clamp`、`runBatchAnalysis`、`aggregateAndStoreReport`、`buildCandidateProfile`、`buildJobInfo`、`parseIntroScoreView`、`triggerSceneWorkflowAsync`、断点续接组（`getActiveInterview`/`resumeInterview`/`rebuildMemoryIfNeeded`/`closeStaleInterviews`）、报告/分享/管理端组。删除后清理孤儿 `@Autowired`（questionPicker、hintEngine 等）与孤儿 import。

**注意**：`interview.phase` 字段仅剩 `finish` 中 `InterviewPhase.isLegacy/FINISHED` 一处写入 → 保留不动（避免 DB 兼容问题）。

### E. 后端：删除死类及其测试

`moyun-server/src/main/java/com/moyun/ext/cms/service/interview/` 下删除（已核实仅被上述死方法/彼此引用）：

- `QuestionPicker.java` + `impl/QuestionPickerImpl.java`
- `QuestionPickCommand.java`、`QuestionPickResult.java`、`QuestionWeights.java`
- `ResumeContext.java`（面试模块的；注意勿动 `ResumeDeepOptimizeGenerator.buildResumeContext`，仅同名的私有方法）
- `WebSearchService.java` + `impl/NoopWebSearchServiceImpl.java`（删除前 grep 确认无他处引用）
- `InterviewDecisionPolicy.java`、`InterviewTurnResult.java`、`InterviewAnalysisParser.java`、`InterviewPromptAssembler.java`

测试删除：`src/test/java/com/moyun/ext/cms/service/interview/` 下 `InterviewPromptAssemblerTest.java`、`InterviewDecisionPolicyTest.java`、`InterviewAnalysisParserTest.java`。

保留：`HintEngine`、`InterviewPhase`、`ScoringEngine`、`AnswerScoringEngine`、`InterviewAgentClient`、`InterviewChatMemoryService`。

`VoiceStartConfig.java` 收口为 5 字段：`position`、`questionCount`、`resumeId`、`difficulty`、`jobRequirements`（删 scene/style/personalized/hintsEnabled/stuckThreshold/agentId/dynamicMode/jobTemplateId/questionWeights）。

### F. 前端：API 收口（voiceInterview.ts）

- `VoiceStartConfig` 同步收口为 5 字段（position/jobRequirements/resumeId/difficulty/questionCount）。
- 删除 `getVoiceAgents`、`getVoiceJobTemplates`、`forceVoiceNext`、`VoiceAgentItem`、`VoiceJobTemplateItem`、`InterviewAnalysis` 接口。
- `SseCallbacks` 收口为：`onDelta(text)`、`onEnd(payload: {roundDone:number; nextQaId?:number; nextQuestion?:string; finished?:boolean})`、`onError(msg)`、`onAborted()`；`parseSseBlock` 只处理 `delta/end/error` 三类事件（speak/score/data 分支删除）。
- `submitVoiceAnswer` 增加 `skip?: boolean` 参数（body 追加 `skip`）。
- `VoiceInterviewVO` 删除 scene/style/questionMode/isPersonalized 等废弃字段（后端 `toVO` 对应字段同步清理）。

### G. 前端：VoiceInterviewPage.vue 收口

- **设置面板**（模板 ~L2072-2130）：删 面试场景/面试官风格/面试官智能体/动态出题/岗位模板 五个下拉及 `hintsEnabled` 卡壳开关；保留 岗位、难度、题数、JD、简历。
- **script**：删 `agentList/selectedAgentId/dynamicMode/jobTemplateList/selectedJobTemplateId` 状态与拉取（~L27-28、L576-680）、`SCENE_LABEL/STYLE_LABEL` 及顶栏对应展示（~L1001-1003、L2211-2212）、卡壳自动提示逻辑（~L940-950）；`startVoiceInterview` 调用只传新 5 字段（~L1339-1343）。
- **handleSubmitAnswer 重写回调**：`onDelta` → `appendDelta` + **分句 TTS**（累积文本遇 `。！？；\n` 切句立即 `ttsSpeak`，说完最后一句才重新开麦）；`onEnd(payload)` → 有 `nextQaId` 则 `currentQaId = nextQaId` 并 `presentQuestion(nextQuestion)`，`finished` 则 `handleFinish()`；删 `onData/onScore/onSpeak` 全部分支（guidance/agentAction/nextAction 逻辑随之消失）。
- **handleSkip**：改调 `submitVoiceAnswer(..., {skip:true})`，复用 SSE 渲染；`skipStreak` 连续 3 次跳过自动结束逻辑保留。
- **handleHint**：简化为展示后端返回的单条提示文本（tag "提示"）。
- 删 `interviewer-style`"动态出题"徽标等旧展示。

### H. 四同步

- devlog 追加 v11.92：V3 统一 AI 入口收尾（协议补全 + 死代码删减清单 + 配置收口）。
- 《AI 面试全链路重构 · 完整实施文档（V2 · 强化版）.md》追加 V3 章节（纯 agent 自由面试模型：滑窗记忆/流式话术/skip/hint/end 协议）。
- SQL：无表结构、菜单变更，**无增量脚本**（`voice.interview.defaultAgentId` 已存在）。
- 菜单：无变更。

## 验证

1. 后端：`D:\dev_en\apache-maven-3.9.14\bin\mvn.cmd compile`（全路径，防 PATH 劫持）→ 删类后 `mvn.cmd test-compile` 确认测试无残留引用。
2. 前端：portal `vue-tsc + vite build`（build:prod）。
3. 实测流程：开始 → 回答（delta 打字机 + 分句 TTS，不等分析）→ 面试官不重复提问 → 第二问 `currentQaId` 正确推进（新 QA 行）→ 跳过 → 提示 → 问满收尾 → 报告批量分析正常。
4. 断点续接回归：进行中刷新 → 继续 → 滑窗/重建上下文连贯。

## 执行顺序

1. A（协议补全）→ 2. B（skip）→ 3. C（hint 重写）→ 4. D（删方法，分组分批 + 每批编译）→ 5. E（删类+测试+VO 收口）→ 6. F（api）→ 7. G（页面）→ 8. H（四同步）→ 验证。
