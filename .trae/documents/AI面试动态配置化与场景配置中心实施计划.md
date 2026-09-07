# AI 面试全链路动态配置化 实施计划（完整版·合并）

## Context（背景与目标）

依据 `docs/AI面试流程与相关模块拓展优化方案-20260907.md`（V2.2）核查现状，目标是**所有优化全部执行、整个链路完整**：场景配置中心 → 智能出题 → 6 阶段面试流程 → 结构化评分 → 报告增强 → 错题闭环，全部动态配置化。

### 已完成（无需重做）

| 能力        | 现状                                                                                                                                                     |
| --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Agent 绑定链 | ai\_agent + portal\_voice\_interview\.agent\_id + sys\_config 默认值 + 占位符渲染（{{position}}/{{resumeDigest}} 等）+ 模型路由（InterviewAgentClientImpl.createModel） |
| 简历来源字段    | portal\_user\_resume 已有 source\_type/source\_file\_url/source\_file\_name/full\_text                                                                   |
| 附件解析      | ResumeParseService：PDF/Word 抽取 + LLM 结构化 + 规则兜底                                                                                                        |
| 规则评分      | scoreAnswer() 6 维度（relevance/professionalism/fluency/interactivity/confidence/logic）+ rule\_dimensions\_json                                           |
| LLM 深度分析  | llm\_analysis\_json（sentiment/redFlags/fluency）+ InterviewDecisionPolicy 决策                                                                            |
| 报告聚合      | finish()：totalScore/dimensions/highlights/weakPoints/questionReviews/summary/suggestion                                                                |
| 错题本完整体系   | portal\_wrong\_question + recordWrongQuestion（答题失败自动入库已有先例，可直接复用）                                                                                      |
| 工作流引擎     | WorkflowEngine + WorkflowServiceImpl.execute 已存在                                                                                                       |
| 后台已有页面    | ai/agent、ai/model-config、ai/provider、cms/interview/\*（question等）、cms/voiceInterview                                                                    |

### 缺失需补齐（本计划全部实施）

场景配置中心、岗位模板、面试配置、智能出题器（权重化）、ResumeContext、6 阶段流程状态机（自我介绍/反问）、评分权重化重构（LLM70%+规则30%）、报告增强（自我介绍评分/强弱项/改进建议/错题入库）、JD 关键词 LLM 提取、3 个后台新页面、工作流异步接线、portal 前端配套展示。

### 关键约束（设计依据）

- `PortalInterviewQuestion.difficulty` 是 easy/medium/hard 字符串 → 新表难度对齐该枚举

- Agent 解析链（前端 agentId → sys\_config → null）只增不改，场景解析插链头

- 菜单：面试管理=5192、AI基础配置=5238、当前最大 menu\_id=5440 → 新菜单 5450 起

- AI 表规范 deleted tinyint（AiBaseEntity）；portal 表规范 BaseEntity 五件套 + del\_flag

- 后台路由由 sys\_menu.component 驱动，新页面只需菜单 INSERT + views 目录

## 有所取舍（2 项 + 理由）

| 项                                    | 决策        | 理由                                                         |
| ------------------------------------ | --------- | ---------------------------------------------------------- |
| 简历缓存表 portal\_resume\_context\_cache | **不做**    | digest 已随会话存 configJson（天然快照）；面试低频调用，内存构建 ResumeContext 足够 |
| 评分双模型交叉验证                            | **不做**    | 延迟翻倍、Token 成本翻倍；用四重规则校验替代（范围/一致性/字数/关键词）                   |
| 工作流在对话轮次中执行                          | **不接线轮次** | 轮次要求低延迟；仅在 finish() 报告生成后异步触发                              |

## 四大阶段总览（进度坐标）

```
阶段A 架构底座     → 阶段B 智能面试流程    → 阶段C 评分与报告      → 阶段D 管理页面与收尾
(SQL/场景/简历/模板)   (出题器/6阶段状态机)     (权重评分/报告/错题)     (3页面/工作流/前端/文档)
```

***

## 阶段 A：架构底座

### A1. SQL 增量脚本

新建 `moyun-server/src/main/resources/sql/20260907-moyun-ai-interview-full.sql`：

**新表 ×3**：

- `ai_scene_config`（AI 规范/AiBaseEntity）：scene\_code/scene\_name/description/agent\_id/model\_config\_id/knowledge\_library\_ids/tool\_ids/workflow\_id/config\_json/version/weight/priority/is\_default/enabled/deleted，UNIQUE(scene\_code,version)

- `portal_job_template`（BaseEntity 规范）：name/category/position\_code/description/jd\_text/keywords/difficulty(easy|medium|hard)/question\_count/weights/status(active)

- `portal_interview_config`（BaseEntity 规范）：config\_name/persona\_type/prompt\_template/scoring\_weights/question\_weights/max\_followups/followup\_triggers/enable\_self\_intro/self\_intro\_duration/is\_default/status

**ALTER ×4**：

- `portal_user_resume` ADD `parse_confidence tinyint`（LLM=85/规则=60）

- `portal_interview_question` ADD `job_template_id bigint` + 索引

- `portal_voice_interview` ADD `phase varchar(30)`（6阶段状态机，NULL=旧流程）+ ADD `intro_score_json text`（自我介绍评分）

- `portal_voice_interview_qa` ADD `llm_score_json text`（LLM 结构化评分）

**菜单**（幂等 DELETE+INSERT，5450-5464）：

- 5450 场景配置（parent=5238，component=ai/scene/index，perms=cms:ai:scene:\*）+ 4 按钮

- 5455 岗位模板（parent=5192，component=cms/interview/jobTemplate/index，perms=cms:interview:jobTemplate:\*）+ 4 按钮

- 5460 面试配置（parent=5192，component=cms/interview/interviewConfig/index，perms=cms:interview:config:\*）+ 4 按钮

**种子数据**：voice\_interview 场景（agent\_id=子查询取 sys\_config 现有默认 agent，上线行为零变化）+ 默认面试配置（出题权重 40/30/20/10 + 评分权重默认）+ Java/前端岗位模板示例 2 行。

### A2. AI 场景配置中心后端（com.moyun.ext.ai）

- `entity/AiSceneConfig` + `mapper/AiSceneConfigMapper` + `service/AiSceneConfigService(+Impl)`（CRUD + listEnabledBySceneCode）

- `dto/AiSceneBinding`：sceneConfig/agent/modelConfig/knowledgeLibraryIds/toolIds/workflowId/configJson + hasAgent()/hasModelOnly()/empty()

- `service/AiSceneResolver(+Impl)`：`resolve(sceneCode)` — 单行直接命中；多行按 weight 轮盘赌灰度（全 0 时 is\_default → priority DESC）；任何异常返回 empty 不阻断业务

- `vo/AiSceneConfigVO` + `controller/AiSceneConfigController`（/cms/ai/scene：list/get/create/update/delete/{id}/test，@PreAuthorize @ss.hasPermi）

### A3. ResumeContext（ext/cms/service/interview/ResumeContext.java）

- `from(PortalUserResume)` 一次解析 JSON 字段；digest（迁移 buildResumeDigest：projects 前3 name/stack/highlight）+ keywords（stack+skills，上限12）懒加载

- `PortalUserResume` +parseConfidence；`ResumeParseService` 回填（LLM 85/规则 60）

- `VoiceInterviewServiceImpl.buildResumeDigest` 改为委托，外部行为不变

### A4. 岗位模板 + 面试配置后端

- `portal/domain/entity/PortalJobTemplate`、`PortalInterviewConfig`（extends BaseEntity）+ Mapper ×2

- `IPortalJobTemplateService(+Impl)`：CRUD + `extractKeywords(jdText)`（LLM 提取 + 规则分词兜底，见 C4）+ `bindQuestions(id, questionIds)`（全量覆盖 question.job\_template\_id）

- `IPortalInterviewConfigService(+Impl)`：CRUD + getDefaultConfig() + is\_default 互斥

- `CmsJobTemplateController`（/cms/interview/jobTemplate）、`CmsInterviewConfigController`（/cms/interview/config）

- `PortalInterviewQuestion` +jobTemplateId；InterviewQuestionQuery 加筛选条件

***

## 阶段 B：智能面试流程

### B1. QuestionPicker 智能出题器（ext/cms/service/interview/）

- `QuestionPicker` 接口：`pick(QuestionPickCommand)`

- `QuestionWeights`：默认 40/30/20/10，`quota(count)` 最大余数法分配题额

- `QuestionPickResult`：questions + snapshots（沿用锚定题协议）+ sources

- `impl/QuestionPickerImpl`：吸收 VoiceInterviewServiceImpl 现有出题私有方法（pickQuestionsByProfile/pickQuestionsWithResume/queryByTag/pickQuestions），四题源 + 自动流转降级：

  - **job**：job\_template\_id 匹配按难度递进 → 不足用 keywords 匹配 tags

  - **resume**：ResumeContext.keywords 匹配 tags → 不足生成项目锚定题（上限2）

  - **weak**：snapshot.weakTags 按 failRate DESC 匹配

  - **random**：随机兜底，最终不足继续补满

  - 题源不足自动向后续题源流转；题库为空仍抛原 ServiceException

- 权重解析链：前端传参 > 面试配置 > 岗位模板 > 默认 40/30/20/10

### B2. 6 阶段流程状态机

新建 `ext/cms/service/interview/InterviewPhase.java` 枚举：

```
INTRO_WAITING → INTRO_RECEIVED → INTRO_FOLLOWUP → TECH_QUESTION →
PROJECT_DEEP → SYSTEM_DESIGN → CANDIDATE_ASK → FINISHED
```

- `portal_voice_interview.phase` 字段驱动；**enable\_self\_intro=0 时跳过 INTRO 系列**（直接 TECH\_QUESTION）

- 旧会话无 phase 视为旧流程，完全兼容

- 流转规则：TECH→PROJECT→SYSTEM 按题单 question\_type/难度段分配；PROJECT\_DEEP 优先用简历锚定题；CANDIDATE\_ASK 在题单耗尽后进入（agent 邀请反问，最多 3 问，用户说"没有了"或 finish 触发结束）

- 新增 portal 接口：`POST /portal/interview/voice/self-intro`（提交自我介绍 → LLM 评分存 intro\_score\_json → 生成追问或进入 TECH）

- 自我介绍评分 4 维度：逻辑结构30/自我认知25/岗位匹配25/表达流畅20（权重来自面试配置 scoring\_weights.selfIntro）

### B3. 面试链路接线（VoiceInterviewServiceImpl.start 改造）

- `InterviewAgentClient` +`resolveScene(sceneCode)` +`resolveAgentForScene(sceneCode, agentId)`；实现：场景 Agent > 场景直绑模型（合成伪 Agent，空 systemPrompt 自动回退旧人设）> 原 resolveAgent 链

- start() 改造：

  - agent 解析改用 resolveAgentForScene（常量 SCENE\_VOICE\_INTERVIEW="voice\_interview"）

  - 动态模式判定插场景 configJson.dynamicMode（优先于 sys\_config）

  - 权重解析链：前端 > 面试配置 > 岗位模板 > 默认

  - preset 三路径合并为 questionPicker.pick(command)

  - phase 初始化（enable\_self\_intro ? INTRO\_WAITING : TECH\_QUESTION）

- configJson 新增 key（只加不改）：sceneConfigId/interviewConfigId/jobTemplateId/questionWeights/enableSelfIntro/maxFollowups

- `VoiceStartConfig` +jobTemplateId、+questionWeights

- 轮次链路（submitAnswer/finish）按 phase 分发；旧会话（无 phase）走原逻辑

***

## 阶段 C：评分与报告

### C1. 评分引擎权重化重构

新建 `ext/cms/service/interview/ScoringEngine.java`：

- **每题得分 = LLM 分 × 70% + 规则分 × 30%**（比例可配，来自面试配置 scoring\_weights.fusion）

- LLM 评分：结构化 prompt（对齐设计文档 3.4.6 模板），输出 JSON：scores{relevance,depth,fluency,logic,confidence}/total/strengths/weaknesses/comment；解析失败回退规则分（=旧行为）

- 规则分：现有 `scoreAnswer()` 6 维度保留复用

- **防幻觉四重校验**：分数范围截断 0-100；总分与维度加权和一致性校验（偏差>10 按维度占比重算总分）；字数<30 扣分；关键词零命中降级

- LLM 评分结果存 `portal_voice_interview_qa.llm_score_json`；融合分写 score

- 评分维度权重（relevance20/depth25/fluency20/logic20/confidence15）从面试配置读取，prompt 动态注入

- 自我介绍评分走同一引擎（4 维度独立 prompt + 规则字数/结构词校验）

### C2. 报告增强（finish 改造）

- **自我介绍独立评分模块**：报告新增 `introScore`（4 维度 + 总分 + 评语，来自 intro\_score\_json）

- **最终分 = 自我介绍分 × 20% + 技术面平均 × 80%**（无自我介绍时 100% 技术面，兼容旧数据）

- **强项/弱项自动分析**：维度聚合 + 各题 LLM strengths/weaknesses 聚合去重（top3 强项 + top3 待提升）

- **改进建议**：LLM 生成 improvementPlan（复用 llmClient，失败回退规则模板）

- `VoiceInterviewReportVO` +introScore/strengths/weaknesses/improvementPlan（只加字段，前端渐进展示）

### C3. 错题自动入库

- C1 评分后：**得分 < 60 的题目**调用现有 `wrongQuestionService.recordWrongQuestion`（attemptId 传 interviewId 标记来源为面试）

- finish 时返回 `wrongQuestionCount`，提示"已将 N 道错题同步到错题本"

### C4. JD 关键词 LLM 提取

- `extractKeywords(jdText)`：优先 `llmClient.chat`（结构化 prompt 返回 JSON 数组，限 15 词），失败回退规则分词（停用词过滤 + 技术词表匹配）

***

## 阶段 D：管理页面与收尾

### D1. 场景配置中心前端

- `src/api/ai/scene.js` + `views/ai/scene/index.vue`

- 列表：场景代码/版本/绑定方式/权重/优先级/默认/状态

- 编辑对话框：Agent 下拉（api/ai/agent.js）+ 直接绑定区（模型/知识库/工作流/工具下拉，Agent 未选时展示）+ 版本/权重/优先级/默认/启用 + configJson textarea（保存前 JSON.parse 校验）

- 测试按钮：POST /{id}/test 展示解析后 binding 摘要（agent 名/模型名/知识库数）

### D2. 岗位模板 + 面试配置前端

- `api/cms/interview.js` 追加两组 API

- `views/cms/interview/jobTemplate/index.vue`：基础信息 + JD textarea + 「LLM 提取关键词」按钮 + el-tag 关键词增删 + 四路权重（合计=100 校验）+ 关联题目弹窗（复用 listInterviewQuestion + bindQuestions）

- `views/cms/interview/interviewConfig/index.vue`：人设 radio（对齐现有 style 文案：professional/friendly/strict）/ 提示词模板 textarea + 占位符说明 / 出题权重 4 项 / 评分权重（5 维度 + 融合比例 + 自我介绍 4 维度）/ 追问策略（max\_followups + 触发条件）/ 自我介绍开关 + 时长

### D3. 题库页面增强

- `views/cms/interview/question/index.vue`：表单加「所属岗位模板」下拉（数据源 jobTemplate list）+ 列表筛选

### D4. 工作流接线（有限度，见取舍）

- 场景绑定 workflowId 时：finish() 报告生成后**异步**触发 `WorkflowService.execute`（传入面试上下文变量：候选人/岗位/得分/强弱项），失败仅日志不影响报告

- 对话轮次不执行工作流

### D5. portal 前端 + 四同步收尾

- portal 轻量接口：`GET /portal/interview/voice/job-templates`（active 岗位模板下拉，前台选择）

- portal 面试页前端：自我介绍环节交互（提交/追问提示）、候选人反问环节交互（"没有了"结束按钮）

- portal 报告页前端：展示 introScore/strengths/weaknesses/improvementPlan/wrongQuestionCount（渐进增强，旧报告无新字段不显示）

- devlog v11.x 完整记录（分阶段）；设计文档状态从"待评审"更新为"已实施"并标注实施映射

***

## 向后兼容保障

1. 种子场景绑定现有默认 agent → 上线当天行为零变化；停用场景行即回退
2. enable\_self\_intro 默认 0 → 6 阶段状态机默认不激活，旧流程完全保留；旧会话无 phase 走原逻辑
3. 出题降级完备：无模板/简历/画像 → 额度流转 → 纯随机（等价原行为）
4. configJson/报告 VO 只加不改，旧数据宽容解析
5. 评分：LLM 失败回退规则分（纯规则=旧行为）
6. 进行中会话不受配置变更影响（start 时刻快照语义）
7. 工作流异步触发失败不影响主流程；JD 提取 LLM 失败回退规则分词

## 实施顺序（依赖链）

```
A1 → A2/A3/A4（可并行）→ B1 → B3 → B2 → C1 → C2/C3（并行）→ C4（独立）
D1/D2/D3 依赖 A2/A4 完成即可开始（可与 B/C 并行）→ D4/D5 收尾
```

## 验证方式

1. 执行 SQL 脚本（DataGrip），校验 SELECT（表/菜单/种子）
2. 后台三个新页面：CRUD 正常 + 场景测试按钮 + JD LLM 提取关键词 + 关联题目 + 题库选题模板
3. 门户语音面试全链路：

   - enable\_self\_intro=1：start 后 phase=INTRO\_WAITING → 提交自我介绍 → intro\_score\_json 评分 → 追问 → TECH/PROJECT/SYSTEM → CANDIDATE\_ASK 反问 → finish

   - enable\_self\_intro=0：回归原流程（无自我介绍，行为与现状一致）

   - 带 jobTemplateId：后端日志确认出题来源分布符合权重

   - 得分<60 的题自动入错题本（查 portal\_wrong\_question）；报告含强弱项与改进建议
4. 评分引擎：构造超短回答（<30字）确认扣分；LLM 返回异常分数确认截断/重算
5. 附件简历上传 → parse\_confidence 回填；旧会话/旧报告数据不受影响
6. 场景绑定工作流后 finish → 异步触发日志可见，失败不影响报告

