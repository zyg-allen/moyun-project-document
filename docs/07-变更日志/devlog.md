# 旭林知行 · 开发日志

本文件记录旭林知行项目核心版本变更，作为代码提交与发布的参考记录。
日期采用 `YYYY-MM-DD` 格式。

***

## v11.70 (2026-09-11) 资金链路测试补齐：打赏双链路 22 用例（P1-8，附充值/提现范围澄清）

> 依据《AI底座企业级评估-代码实测结论与改进清单》最后一项 P1：资金链路零测试。**实测澄清**：充值/提现业务逻辑尚未实现（PortalWalletServiceImpl 仅 CRUD，全库无 recharge/withdraw 业务方法，PayCallbackHandler 实现仅 tip 一种）——资金链路现状 = 打赏（积分打赏 + 微信支付 + 回调复式分账），无从测试的部分属功能范围澄清而非测试缺口。纯测试增量，零生产代码改动。维度 9 → 4.4，综合 → 3.84。

### PortalTipServiceTest 16 用例（积分打赏 toggleTipOrList + 微信下单 createWechatTipOrder）

- **积分打赏闭环**：article/column 对象解析 → 实名校验 → 原子扣分（deductPoints 带 `WHERE points >= delta` 防护）→ 对方加分 → PAID+points 订单落库 → 双方成长事件（receive_tip / tip_others）全链路断言
- **资金安全分支**：积分不足（deductPoints 返回 0）→ POINTS_INSUFFICIENT 且**不加对方分、不落订单**；自赏拦截；未实名回滚不扣分；article_paid 占位拦截（未扣费不发放付费阅读权限）
- **金额边界**：null / 0 / 负数拒绝；0.5 元截断为 0 拒绝（积分须正整数）；积分通道无 10000 上限（与微信通道的元上限语义区分）
- **微信支付下单**：pending+wechat 订单落库 + 网关统一下单 + 收银台参数（payNo/codeUrl/amount/expireTime/mockEnabled）+ 不扣积分（走支付回调）；超 10000 元拒、未登录拒

### TipPayCallbackHandlerTest 6 用例（回调闭环）

- **成功闭环**：pending→paid 条件更新 → 复式分账（平台抽成 + 作者所得，**金额守恒断言** 10=0.5+9.5）→ 双方站内通知含到账明细
- **幂等与并发**：已支付幂等返回（渠道重试不重复分账不重复通知）；条件更新 rows=0（并发竞争）抛 IllegalStateException 整体回滚，不分账不通知；状态异常（closed）拒绝推进；打赏单不存在抛异常

### 测试技巧沉淀

- **MP LambdaUpdateWrapper 纯单测**：`can not find lambda cache for this entity` —— MyBatis-Plus 的 lambda 列解析依赖实体 TableInfo 缓存，纯 Mockito 环境无启动流程，需 `TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Entity.class)` 手动初始化（@BeforeAll 一次）
- **PayProperties.mockEnabled 默认 true**（开发演示模式），断言生产语义需显式 setMockEnabled(false)

### 验证

- `mvn.cmd test -Dtest=PortalTipServiceTest,TipPayCallbackHandlerTest`：22/22 全绿
- 全库回归：**308/308 全绿**（v11.67 基线 286 + 资金链路 22），surefire 报告时间戳核对通过

### 四同步

- 代码：仅新增两个测试类（PortalTipServiceTest / TipPayCallbackHandlerTest），生产代码零改动；SQL：无；文档：评估文档（P1-8 完成行、维度 9 → 4.4、综合 → 3.84、行动路线第 11 项 + P1 清零说明、checklist 测试覆盖行与漏勾的 output_parser 项补勾）+ 本记录
- 部署：无需重启（纯测试增量）

---

## v11.69 (2026-09-11) sys_file 系统文件管理菜单补齐：后台管理入口上线（附图片预览 bug 修复）

> 现状实测：sys_file 表、后端 SysFileController（/system/file，list/{id}/upload/byUrl/{ids}/storage/*，@PreAuthorize system:file:*）、前端页面（views/system/file/index.vue：搜索/上传/批量删除/预览/下载/分页）与 API（api/system/file.js）均已存在——**唯一缺口是 sys_menu 无菜单记录，管理端无入口可见**。

### 变更内容

**菜单 SQL（20260911-09-sys-file-admin-menu.sql，幂等 DELETE+INSERT）**：
- 文件管理 5475（C，parent=5243 基础管理，order 6 排岗位管理之后，component='system/file/index'，perms='system:file:list'，icon='upload'）
- 按钮权限位：文件查询 5476（system:file:query）/ 文件上传 5477（system:file:add，含存储模式切换）/ 文件删除 5478（system:file:remove）
- 管理员角色授权（role_id=1 × 5475-5478）
- 权限标识与后端 @PreAuthorize 完全对齐，接口级权限不变

**前端 bug 修复（views/system/file/index.vue）**：
- `handlePreview` 原调用 `this.$imagePreview()`——该原型方法**未在任何位置注册**（main.js 仅注册 ImagePreview 标签组件），点击预览必抛 TypeError
- 修复：内嵌 `<el-image-viewer>`（Element Plus 全量引入可直接使用），v-if 控制显隐，@close 关闭

### 验证

- 前端：`npm run build:prod` 构建通过
- 菜单：执行 SQL 后 `SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id BETWEEN 5475 AND 5478;` 确认 4 条
- 页面：管理端重新登录 → 系统设置 → 基础管理 → 文件管理，验证列表加载/上传/图片预览/删除（删除走 byUrl 统一清理路径：存储+记录）

### 追加修复（同日）：列表数据不显示

- **现象一**：页面打开后列表始终为空（分页 total 正常）
- **根因一**：`getList` 取值字段名错误——`response.data?.list`，而 MyBatis-Plus Page 序列化字段为 `records`（request.js 拦截器还会把 `data.records` 复制为 `data.rows`），`list` 恒为 undefined
- **现象二**：字段修复后仍不显示，Console 报 `TypeError: Cannot read properties of undefined (reading 'row')`，表格表头渲染但 tbody 空
- **根因二（真根因）**：**该页面整体为 Vue 2 语法跑在 Vue 3（3.4.0）项目里**——`<template slot-scope="scope">` 在 Vue 3 编译后 scope 为 undefined，`scope.row.fileType` 抛 TypeError 中断表格行渲染；同页还有 `@keyup.enter.native`、`el-icon-*` 旧类名、`this.$modal` 等不兼容写法
- **修复**：按项目标准（对齐 sensitiveWord 页面）整体重写为 Vue 3 `<script setup lang="ts">` 写法——`#default="scope"` 插槽、v-model:page/limit 分页、ElMessage/ElMessageBox、字符串 icon、v-hasPermi 权限指令、right-toolbar 工具栏；保留全部原有功能（搜索/上传/批量删除/单删走 byUrl/下载/图片预览 el-image-viewer/大小格式化）
- **教训**：admin-vue 是 Vue 3 + Element Plus 项目，存量 Vue 2 风格页面（slot-scope/.native/$modal）在该项目内属隐藏地雷——新写页面一律以 sensitiveWord 等页面为模板

### 四同步

- 代码：moyun-admin-vue/views/system/file/index.vue（预览修复，仅 bug 修复无功能变更）
- SQL：20260911-09-sys-file-admin-menu.sql（菜单+授权，零表结构变更）
- 菜单：见上（5475-5478）
- 文档：本条 devlog；后端零改动无需同步
- 部署：执行 SQL → 管理端重新登录（菜单为登录时拉取）→ 无需重启服务

---

## v11.68 (2026-09-11) 功能闭环自测整改：三大模块五环补全 + 跨模块四数据流全通（P0/P1/P2 闭环清单清零）

> 依据《20260911-功能闭环自测文档》：三大模块（面试/练习/简历优化）"五环"各断一环（再入口），跨模块四个数据流断三个，硬断链 5 项 + 软断链 3 项。本版实测修正 2 项误判、真修复 5 项，四数据流全通。

### 实测误判（2 项，零代码改动）

延续"评估文档系统性误判模式"（先实测再动手）：

- **P0-1 编程题 AC 成长事件**：同步路径（PortalJudgeServiceImpl.finalizeCallback）与异步路径（JudgeAsyncWorker.applyResult）均已回调 `finalizeJudgeResult → recordAttemptAndGrowth`——首次通过发 solve_question 成长事件 + Feed 答题动态，闭环完整
- **P1-1 阅读行为记录（半误判）**：后端 `recordQuestionRead` 已实装（每日幂等 read_question + write_note 事件），但前端为"加载即上报"——按《Portal与Admin自测指导文档》"停留 <60s 不记录"要求，本版补齐 ≥60s 停留计时上报（切题/离开清除计时）

### 真修复 6 项

**P0-2 面试→练习（跨模块数据流 2）**：报告页"缺点"卡片每条弱项新增"🎯 去练习"按钮；`practiceKeyword()` 从弱项文本提取搜索关键词（去疑问修饰、截取核心 12 字），跳转 `/learn/practice/choice?keyword=` 选择题列表同源筛选。

**P0-3 练习→面试（跨模块数据流 3）**：出题链路全量接入已掌握避让——`pickQuestions/queryQuestions` 签名增加 userId，经 `queryWithSolvedExclusion`（attempt.status=solved 的题 notIn 排除）出题；5 个调用点（agent 候选/首问兜底×2/画像补齐/首问动态）全部传入；排除后无题自动回退不排除（出题数量优先于避让）。上次会话遗留的签名不一致编译错误一并修复。

**P2-1 简历→面试（跨模块数据流 1）**：优化完成页新增"🚀 去面试"按钮（savedResumeId 存在时显示）；语音面试页 `loadResumeList` 支持 `?resumeId=` URL 预选简历（优先于默认第一份，并带入求职意向岗位）。

**面试→简历（跨模块数据流 4）**：报告页"面试官剖析"Tab 改进建议区新增"📝 按建议优化简历"入口 → `/interview/resume/optimize`。

**P1-2 双维度路由塌缩**：题库（QuestionListPage：分类+难度+题型+关键词）/ 选择题列表 / 编程题列表三个页面，筛选与分页状态变化时 `router.replace` 回写 URL query——刷新/浏览器返回不再塌缩为"全部"，URL 可分享复现。

**P1-1 补全 阅读停留上报**：QuestionDetailPage 阅读埋点由"加载即上报"改为 60s 停留计时上报（READ_REPORT_DELAY_MS=60_000，切题重置、离开清除，满足"停留 <60s 不记录"防刷要求）。

### 验证

- 前端：`npm run check`（vue-tsc）两轮通过（含全部新增改动）
- 后端：`mvn.cmd compile` 通过（class 产物时间戳核对 17:19）；面试包单测 InterviewPromptAssemblerTest(8) + InterviewDecisionPolicyTest(13) + InterviewAnalysisParserTest(9) = 30/30 全绿（surefire 时间戳 17:21 核对）
- 出题避让回归路径：画像出题（queryByTag×已升级）/ 随机兜底 / 简历画像补齐 / agent 候选 / 首问兜底全部传 userId

### 四同步

- 代码：后端 VoiceInterviewServiceImpl（pickQuestions 全链路 userId + 避让回退）；前端 VoiceInterviewPage（去练习/去优化简历/resumeId 预选）+ ResumeOptimizePage（去面试）+ QuestionDetailPage（60s 停留上报）+ QuestionListPage / PracticeChoiceListPage / PracticeCodingListPage（URL 回写）
- 文档：20260911-功能闭环自测文档（断链清单状态全量更新、四数据流标记全通、自测清单勾选、优先级表加状态列）+ 20260911-Portal与Admin自测指导文档（闭环验收表、简历/阅读/时间线自测项状态同步）
- SQL：无（零表结构变更，重启即生效）
- 菜单：无涉及
- 部署：重启 moyun-server + 重新构建 moyun-portal 即生效；建议按自测文档第四部分走一遍完整用户旅程（7 步闭环验证）

---

## v11.67 (2026-09-11) P1-6 异步任务收敛：双轨制选型规则 + 孤儿任务恢复（附老包存量测试修复）

> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-6："三套异步任务模式并存"实测修正——JudgeAsyncWorker 系 OJ 代码判题专属基础设施（非 LLM 任务），误计入 AI 债；其余两套为合理双轨制，收敛策略为选型规则文档化而非强行统一抽象。维度 9 → 4.3，综合 → 3.83。

### 实测修正：三套并存 → 双轨制 + 一处误分类

- **表驱动（portal_ai_task，AiTaskService v10.23）**：简历解析/岗位匹配/空字段草稿/深度优化——长任务（分钟级）/需审计追溯/结果持久化供多次查看
- **Redis + 线程池（LedgerAiAnalysisServiceImpl v11.55）**：财务分析——短任务（秒级）/结果时效性强（30 分钟 TTL，报告快照另有落表）
- **JudgeAsyncWorker（OJ 判题）**：进程内判题引擎的队列消费 Worker（BLPOP + 固定线程池 + 重试），领域专属基础设施，**不属于 AI 异步任务体系**，类注释已声明边界

选型口诀：**要留痕走表，要轻快走 Redis**（勿再引入第三套）。选型规则落三处类注释（AiTaskService / LedgerAiAnalysisServiceImpl / JudgeAsyncWorker），开发时就近可见。

### 真代码缺口修复：表驱动任务孤儿恢复

@Async 任务存活于 JVM 内存，应用重启后执行线程丢失，卡在 pending/running 的记录永远等不到终态（前端轮询挂死）。`AiTaskService.recoverOrphanTasks()` 监听 ApplicationReadyEvent，启动时将 pending/running 统一置 failed（error="服务重启，任务中断，请重新提交"）。单实例部署语义正确（多实例需实例标识区分，当前单体架构无此需求）。

### 顺带修复：老包 3 个存量测试失败（实现演进后测试未跟进，修测试不修实现）

全量回归（286 用例）时发现老 chat 包 3 个稳定失败，均为实现合理演进后测试断言过时：

1. **ContentScoringServiceImplTest.shouldFilterStopWords**：测试假设中文分词能力（"什么是的怎么"拆词过滤），实现从未支持（关键词按空格/标点切分）→ 改为空格分隔停用词验证真实能力 + 新增现状 documenting 用例（连续中文整体保留）
2. **ContentScoringServiceImplTest.shouldAddBonusWhenFullMatchInBeginning**：期望 ≥35（30 完整匹配 + 5 开头加分），实际 33.67 = 48.1×0.7——文本 56 字符落在 30-80 长度惩罚区间 → 文本加长至 ≥80 字符避开惩罚，保持原断言语义
3. **RagRetrievalServiceImplTest.shouldSortByScoreDescending**：低分 1.0 被 rerankByRules 质量过滤剔除（阈值 max(2.0, topScore×0.3)）→ 低分改为 2.5（高于阈值）保持排序验证意图 + 新增质量过滤专项用例

### 单测：AiTaskServiceTest 9 用例

提交校验（未知类型拒绝）/入库触发（pending 落库 + ID 回填 + 异步触发参数）/触发失败回写（线程池拒绝 → failed + error）/轮询查询（不存在拒绝/越权拒绝/result JSON 解析/非法 JSON 容错）/孤儿恢复（pending/running 置 failed 参数断言 + 无孤儿 noop）。

**MyBatis-Plus 测试技巧**：UpdateWrapper 的 IN 表达式惰性求值——`getParamNameValuePairs()` 断言前须先调用 `getSqlSegment()` 触发 lambda 执行注册参数，否则 IN 值不在参数表中。

### 验证

- `mvn.cmd test`：AiTaskServiceTest 9/9；全库回归 **286/286 全绿**（v11.66 基线 128 + 老包存量修复 + 新增用例）
- 时间戳核对通过（surefire 报告 16:28 与最新运行同步）

### 四同步

- 代码：AiTaskService（选型规则注释 + recoverOrphanTasks）+ LedgerAiAnalysisServiceImpl/JudgeAsyncWorker（选型定位注释）；测试：AiTaskServiceTest 新增 + ContentScoring/RagRetrieval 修复；SQL：无（零表结构变更，重启即生效）；文档：评估文档（P1-6 完成、维度 9 → 4.3、综合 → 3.83、全库 286 用例、第三批行动路线第 10 项完成、checklist 勾选）+ 本记录
- 部署：重启 moyun-server（启动时自动执行孤儿恢复；存量卡死任务将被置败，用户可重新提交）

---

## v11.66 (2026-09-11) P1-5 output_parser/output_mode 配置接线：从"可编辑零消费"到消费闭环

> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-5：ai_scene_config.output_parser/output_mode 两字段此前配置可编辑零消费（管理页改了没效果，与实际脱节）。本版接线后配置即刻生效。维度 6 → 4.5，综合 → 3.82。

### output_parser 消费：基类 parseOutput(raw, config) 分派

- **json（含 null/空，默认）**：容错提取 JSON 主体为 Map——既有行为，存量场景（全部 'json' 配置）零变化
- **markdown / text**：原文清洗后包装 `{"content": 原文}`——与 system_prompt_template 组合使用（管理员改模板为自由文本输出 + parser 同步改；类型化字段映射场景此时取不到字段，消费方读 content）
- **未知值**：WARN 留痕 + 按 json 容错（配置错误可发现不阻断）

### Handler 双参升级（6 个）：config 直达主流程

DailyTopic/SensitiveWord/ResumeParse/QuestionGenerate/ResumeOptimize/VoiceInterview 的 `execute(request)` 升级为 `execute(request, config)`（基类 v11.48 双参 default 此前被单参覆写"吞掉" config），主流程 `parseJsonMap(raw)` 调用点切换为 `parseOutput(raw, config)`。**子任务方法（VoiceInterview 4 处 + ResumeOptimize executeSubTask + QuestionGenerate jd_keywords）保留 parseJsonMap**——task 契约提示词逐字收编固定 JSON，不受配置影响（契约稳定性设计）。

### output_mode 消费：网关路由校验

- `execute()`（同步入口）：`output_mode='stream'` → 拒绝（failCode=stream_only_scene，提示走流式端点）；both/null 放行
- `executeStream()`（流式端点）：Handler 能力校验（getSupportedOutputMode）之外，`output_mode='sync'` 显式拒绝——**配置与 Handler 能力双保险**

### 配置对齐：voice_interview sync → both（SQL 20260911-08）

调研发现 voice_interview 的 DB 配置 output_mode='sync' 但 Handler 已实现流式（getSupportedOutputMode=both + executeStream）——此前字段零消费，拍脑袋填的值无人发现。新校验上线会误拒现有流式请求，SQL 一次性对齐（其余场景 sync 与 Handler 一致；finance_analysis 的 parser='text' 与综述原文透传行为天然一致）。

### 其他

- AiSceneRegistry.listScenes 透出 outputParser（管理/调试可见性）

### 单测：OutputParserWiringTest 9 用例

- parseOutput 分派分支：null config 默认 json / blank parser 默认 json / 显式 json / markdown 原文包装 / text 别名 / 未知值容错
- DailyTopic 双参配置驱动回归：默认 json 行为不变（title 映射成功）、markdown 配置真实生效（title 为 null——此前改了没效果）、config=null 内部默认 json
- Handler 单参调用点全库核查：仅网关一处双参调用（handler.execute(request, config)），主代码/测试无单参直接调用——升级无回归面

### 验证

- `mvn.cmd test`：OutputParserWiringTest 9/9 全绿；全量回归 128/128 全绿（新增 9 用例并入）
- 时间戳核对通过（15:57 surefire / class 产物同步）

### 四同步

- 代码：AbstractAiSceneHandler（parseOutput 新增）+ 6 Handler 双参升级 + AiGatewayService（output_mode 路由校验两处）+ AiSceneRegistry（outputParser 透出）；SQL：20260911-08（voice_interview 配置对齐）；文档：评估文档（P1-5 完成、维度 6 → 4.5、综合 → 3.82、测试 140 用例、第三批行动路线第 9 项完成、定位判断补 v11.66）+ 本记录
- 部署：①执行 SQL 20260911-08 ②重启 moyun-server

---

## v11.65 (2026-09-11) 知识问答接入统一网关：knowledge_qa 场景（Agent 驱动多路召回 + 引用溯源透出）

> 承接 v11.64 P1-4 实测修正的后续动作：知识问答从 admin chat 专属收编进统一网关（ai2），通过 Agent 实现检索与多路召回，C 端/开放入口可消费。网关场景数 7 → 8，维度 4 → 4.6、维度 6 → 4.4，综合 3.80 → 3.81。

### 核心设计：零新建检索逻辑，全量复用 chat 链底座

- **多路召回**：`RagRetrievalService.retrieveContents(query, kbIds, agent)` 原样复用——查询改写 + 混合检索（向量 + BM25 + RRF 融合 + 知识库权重）+ Rerank 重排 + 相邻分片合并；RAG 参数（ragMinScore/ragMaxResults/召回倍数/混合开关/双路权重）读 Agent 配置，与 chat 链完全一致
- **提示词构建**：`ChatContextBuilderService` 原样复用——buildSystemPrompt（Agent 人设 + 知识库使用规则）+ buildRagContext（参考资料上下文）+ buildProcessedUserMessage。人设经 Agent 实体统一构建，不走网关注入的 input.agentPersona（避免重复注入）
- **引用溯源**：检索片段 metadata（fileName/fileType/pageNumber/segmentIndex/knowledgeBaseId/imagePath）+ Rerank 分数 → references 数组随统一响应透出（消费端可渲染来源），摘录截断 200 字符

### Handler 契约（KnowledgeQaHandler，scene = knowledge_qa）

- **输入**：userInput=问题（必填）；input.knowledgeLibraryIds=知识库 ID（可选，支持 List/JSON数组串/逗号串）
- **知识库三级优先级**：input.knowledgeLibraryIds > ai_scene_config.knowledge_library_ids > agent.knowledgeLibraryIds
- **Agent 绑定**：ai_scene_config.agent_id → ai_agent（RAG 参数 + 人设 + 模型一体的推荐绑定方式）；未绑定/加载失败用空壳 Agent 走全局默认 RAG 配置（不阻断）
- **检索为空**：零 token 返回标准范围外话术（"这个问题不在我的知识库范围内"），不调 LLM
- **LLM 失败**：failure(AI_CALL_FAILED) 附检索命中数提示；模型链走基类 chatDetailed（Agent 绑定 → 场景直绑 → 默认回落）
- **ThreadLocal 卫生**：Rerank 分数为 ThreadLocal 存储，finally 必清理（网关线程池复用会串台——单测覆盖两分支）
- **场景模板覆盖**：ai_scene_config.system_prompt_template 有值时优先生效（v11.48 配置驱动契约）

### SQL：20260911-07-ai2-knowledge-qa-v11-65.sql

- INSERT knowledge_qa 场景（幂等）：scene_category=chat、handler_bean_name=knowledgeQaHandler、open_api=1（统一入口直接可用）、enable_cache=0（KB 内容变更缓存语义复杂）、**enable_output_filter=1**（C 端直出文本场景启用 v11.62 输出过滤）、daily_token_limit=500000（开放入口保护性成本上限）；agent_id/knowledge_library_ids 留空按需绑定（脚本附三种绑定示例）

### 单测：KnowledgeQaHandlerTest 12 用例（子类覆写 chatDetailed 打桩 LLM，无需 mock 模型链）

- 参数校验（userInput 必填）；知识库未绑定三级皆空快速失败（不进检索）
- 三级优先级（input 覆盖场景配置与 Agent；场景配置覆盖 Agent）+ 三种传参形态（List/JSON 数组串/逗号串）+ 非法 ID 忽略保留合法项
- 检索为空零 token 标准响应（验证未消费提示词构建）
- LLM 成功：回答 + 引用溯源字段（fileName/fileType/pageNumber/segmentIndex/knowledgeBaseId/rerankScore/摘录 200 截断）+ metadata（modelUsed/tokenUsed）
- 场景模板覆盖系统提示词（验证未走 chat 链 buildSystemPrompt）
- LLM 空回答失败（附检索命中数）；Agent 加载失败空壳默认不阻断
- ThreadLocal 必清理（空检索与正常链路两分支）

### 验证

- `mvn.cmd test`：KnowledgeQaHandlerTest 12/12 全绿；全量回归（注入 16/成本 13/收口契约 14/基础设施 16/输出过滤 11/校验器 16/评分引擎 12/财务护栏 9/知识问答 12）**119/119 全绿**
- surefire 报告（15:43:07）与 class 产物（15:41:33）时间戳核对通过

### 调用示例（统一入口）

```json
POST /api/ai/execute
{"sceneCode": "knowledge_qa", "userInput": "什么是火箭发动机？", "input": {"knowledgeLibraryIds": [1]}}
```

响应 data：`{answer, references: [{fileName, fileType, pageNumber, segmentIndex, knowledgeBaseId, imagePath, rerankScore, excerpt}], retrievalCount, imageCount}`

### 已知边界（记入评估文档）

- 单轮问答（同步）；多轮对话历史与流式输出随网关多轮消息契约（P2）扩展
- 前端消费页（portal 问答入口）待业务排期——场景与 API 已就绪

### 四同步

- 代码：KnowledgeQaSceneData（新增）+ KnowledgeQaHandler（新增）；SQL：20260911-07（场景注册）；文档：评估文档（维度 4 引用溯源补 v11.65、维度 4 → 4.6/维度 6 → 4.4/综合 → 3.81、P1-4 行增强、测试 119 用例、RAG 层与场景注册 checklist 更新、定位判断补 v11.65）+ 本记录
- 部署：①执行 SQL 20260911-07 ②重启 moyun-server ③绑定知识库（推荐先在管理页建 Agent 配好 RAG 参数与知识库，再 UPDATE ai_scene_config SET agent_id=<id> WHERE scene_code='knowledge_qa'）④统一入口或 AiSceneJsonClient 即可调用

---

## v11.64 (2026-09-11) P1-4 引用溯源：实测修正——完整链路已实装，评估原判"仍缺"系误判，零代码改动

> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-4（原判：检索结果无来源文档/片段/相似度透出到消费端）。逐层调研后确认与 P1-2 同类的**评估误判**：引用溯源完整链路自 2025-12-11 已实装（早于评估文档 2026-09-09 成文），评估时未追到 chat 消费链。本版仅修正评估文档（维度 4 → 4.5，综合 → 3.80），**无代码/SQL/前端改动**。

### 调研确认的完整链路（全部已存在）

1. **检索 metadata 携带溯源字段**：KnowledgeBaseServiceImpl 入库时 TextSegment metadata 写入 fileName/fileType/segmentIndex/knowledgeBaseId（即文档记录 ID，knowledge 表主键）/embeddingId/pageNumber/totalSegments；图片片段另有 imagePath/lineStart/lineEnd
2. **Rerank 分数**：检索链路产出 Content→Double 重排分数 Map（避免 ThreadLocal 跨线程丢失，异步线程传参）
3. **质量过滤**：ReferenceSourceFilterImpl（2025-12-11）——动态阈值 max(最高分×75%, 平均分×85%, ragMinScore)、知识库+页码去重、目录内容/过短(<80字符)/纯图片描述过滤、AI 回复中引用的图片必保留、按智能体 ragMaxResults 限量
4. **SSE 流式渲染**：ChatResponseBuilderImpl.buildReferencesButtons——"来源N"按钮组，data-rerank-score/data-vector-score 相似度透出、data-text 片段全文（弹窗展示）、data-page/lineStart/lineEnd 定位、附"不准确"反馈按钮（含分数用于反馈上下文）
5. **持久化**：ChatMessagePersistenceImpl.saveAssistantMessage → buildReferenceSourcesJson → ConversationMessage.referenceSources JSON 落库
6. **历史回传**：GET /conversation/{conversationId}/messages 返回完整实体（含 referenceSources）
7. **前端渲染**：admin chat/index.vue——实时流式渲染 HTML 按钮 + 历史消息 JSON 解析重建按钮（processHistoricalMessage）+ showReferenceSource 弹窗

### 判定不补的已知小缺口（避免过度设计）

- **历史 JSON 未存相似度分数**：buildReferenceSourcesJson 仅存 fileName/pageNumber/type/imagePath/knowledgeBaseId/segmentIndex/fileType，无 rerank/vector 分数（实时流有）。核实前端历史渲染（index.vue 146-181 行）不消费任何分数字段——后端存了也没人用，不补
- **C 端无 RAG 问答消费方**：知识问答仅在 admin chat（ai2 网关无 knowledge_qa 场景，chat 走老 Agent 框架属既定架构决策）——"透出到消费端"的消费端即 admin chat，已覆盖

### 四同步

- 代码：无改动；SQL：无变更；文档：评估文档（维度 4 引用溯源 ★★→★★★★并附完整证据链、维度 4 4.3→4.5、综合 3.78→3.80、P1-4 标记实测修正、第三批行动路线第 8 项完成、RAG 层 checklist 勾选、定位判断补 v11.64）+ 本记录
- 部署：无需部署

---

## v11.63 (2026-09-11) P1-2 工具参数 JSON Schema 校验：执行链从"裸奔"到执行前收口

> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-2。**实测修正**：调研发现 ai_agent_tool.parameters 种子数据本就是合法 JSON Schema（`{"type":"object","required":[...],"properties":{...}}`，与内置工具 getParametersSchema() 同构），原评估判"文本描述"系误判——真正缺口是**执行链零校验**：LLM 生成的参数类型错误（如 days 传字符串 "3"）直接打入执行器，轻则执行失败重则 ClassCastException。本版补齐校验闭环，维度 3 工具调用 3.0 → 3.2，综合 3.77 → 3.78。

### 校验器：ToolParamValidator（engine/tool，纯静态工具类，同 PromptInjectionGuard 风格）

- **支持子集**（覆盖本项目全部工具 schema 实际用到的关键字）：type（string/integer/number/boolean/array/object）、required、properties、enum、minimum/maximum、items.type（数组元素）
- **fail-open**：schema 为空/不可解析/type≠object 时跳过校验——兼容管理端手填的文本描述型脏数据，校验器不可用不阻断工具执行（执行器自有 asInt/asString 兜底）
- **integer 宽容**：Integer/Long/整值 Double/BigDecimal 均视为 integer（LLM JSON 序列化常产出 3.0 形态）；字符串数字"3"不宽容（应让 LLM 修正类型而非静默转换）
- **不改写参数**：纯校验无副作用；未知 type/Schema 未声明的多余参数跳过（additionalProperties 默认宽容）
- **错误可自纠**：错误文案为 LLM 可读的修正指令（"参数 days 应为 integer 类型，实际收到 string：\"3\"，请修正类型后重新调用"）——经既有工具失败通道回传（chat HTML 展示/工作流 NodeResult.fail/管理页测试响应），下一轮对话 LLM 可据此重新生成合规 [TOOL_CALL]。不新建 LLM 自动重试循环（流式场景重建对话成本高，避免过度设计）

### 接线：ToolRegistry.executeTool 单点收口

- 执行前取 `executor.getParametersSchema()` 校验 params，不合规直接 `ToolResult.fail("参数校验失败：<明细>。请对照工具参数格式修正后重新生成 [TOOL_CALL] 调用")` 不执行工具，且**异步落调用日志**（status=failed，与执行失败同等待遇可审计）
- 三个调用方同时受益：chat LLM 路径（ToolCallingService.detectAndExecute）/ 工作流工具节点（ToolNodeExecutor）/ 管理页工具测试（ToolController /test/{toolName}）
- 工作流路径参数来自节点配置（非 LLM 生成），校验同样捕获配置错误并给出明确提示

### 提示词强化：ToolCallingService.buildToolPrompt

- 工具使用规则新增第 3 条："params 必须严格符合各工具的参数 JSON Schema：必填项齐全、类型正确（string/integer/boolean 等，数字不要加引号）——类型错误会被校验拒绝"（前置降低 LLM 犯错率，校验兜底）

### 单测：ToolParamValidatorTest 16 用例（纯静态类无需 mock）

- fail-open 三跳过分支（空/blank schema、非法 JSON、非 object 根）
- 必填缺失（含 null params 场景）、类型不匹配（**含自纠文案断言**——期望类型与实际类型都出现在错误信息中）
- integer 宽容矩阵（3.0/3L 过，3.5/"3" 拒）
- enum 不在范围、minimum/maximum 边界、数组元素类型错误
- 未知 type 跳过、多余参数宽容、boolean 类型、多错误并报

### 验证

- `mvn.cmd test`：ToolParamValidatorTest 16/16 全绿；回归（注入 16/成本 13/收口契约 14/基础设施 16/输出过滤 11/校验器 16）86/86 全绿
- surefire 报告（15:19）与 class 产物（15:17）时间戳核对通过

### 无 SQL 变更

- ai_agent_tool.parameters 列已存在且种子数据即 JSON Schema，无需 DDL/DML

### 四同步

- 代码：ToolParamValidator（新增）+ ToolRegistry（executeTool 收口）+ ToolCallingService（提示词规则 3）；SQL：无变更；文档：评估文档（P1-2 勾选、维度 3 详情表 Schema 定义 ★★→★★★★/执行引擎/错误处理上调、"剩余缺口"收敛为超时重试+权限、Agent 层与工具层 checklist 勾选、综合 3.77→3.78、测试 91→107 用例、第三批行动路线第 7 项完成、定位判断更新）+ 本记录
- 部署：重启 moyun-server 即生效（校验内置于执行链，无配置项；存量脏 schema 自动 fail-open 跳过）

---

## v11.62 (2026-09-11) P1-3 输出内容过滤：网关复用 DFA 词树脱敏，安全三件套（注入/熔断/输出过滤）齐备

> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-3：LLM 返回直出客户端无敏感词过滤。本版复用 `SensitiveWordFilter`（sys_sensitive_word 词库，DFA O(n)）为网关输出过滤钩子，不另建检测体系。至此维度 8 安全治理的三个 P1 前缺口全部落地，评分 2.5 → 4.0。

### 后端：AiOutputFilter（ai2/support，新增组件）

- **能力**：对网关同步响应 data 的**全部文本节点**（任意深度对象值/数组元素）做敏感词脱敏——命中词替换 `*`，保留原文空白与格式；数值/布尔等非文本节点零影响（金额、评分不动）
- **类型保持**：Jackson 往返（valueToTree → mask 文本节点 → treeToValue 回原类型）。业务消费方存在 `resp.getData() instanceof TopicSceneData/QuestionSceneData` 强转，data 不能替换为 JsonNode
- **零侵入无命中**：未命中时原对象原引用返回，不做往返重建（零开销零风险）
- **失败放行**：DFA 内存操作稳定，异常仅可能来自 Jackson 序列化（极端 data 结构）——放行原文 + ERROR 留痕（过滤不可用不阻断业务，与 Agent 人设注入容错策略一致）
- **位置约束**：钩子位于缓存回写/执行日志之前——缓存与日志留痕的均为脱敏后内容；**缓存命中路径同样过滤**（兜底过滤功能上线前的存量旧缓存）

### 网关接线：AiGatewayService 两处

- 步骤 6.5：`handler.execute` 返回后、`fillCommon`/缓存/日志之前（同步主路径）
- 步骤 3：缓存命中分支（旧缓存兜底，幂等——`*` 不会再命中任何词）
- 开关：`ai_scene_config.enable_output_filter`（TINYINT(1) 默认 0 关闭，存量场景行为不变，按场景按需开启；网关直查库，改列即时生效）

### SQL：20260911-06-ai2-output-filter-v11-62.sql

- `ALTER TABLE ai_scene_config ADD COLUMN enable_output_filter TINYINT(1) DEFAULT 0 AFTER daily_token_limit` + 验证查询；附按需开启示例 UPDATE（注释状态，确认后执行）

### 单测：AiOutputFilterTest 11 用例（真实 DFA 集成，mock mapper 加载词库非 mock 行为）

- 开关判定（null/未设置/显式关闭/开启）
- 跳过分支（null response / null data / 失败 code 原样保留）
- 未命中零侵入（assertSame 原引用）
- Map 脱敏（文本 mask + score/amount 数值断言不变；缓存命中 data 即此型）
- 类型化对象脱敏（assertInstanceOf 类型保持 + 字段 mask/不变）
- 嵌套结构递归（List<Map<List<String>>> + 未命中文本原样）
- 空白绕过仍命中（"炸 弹" 插空白，DFA 跳空白特性）
- DFA 异常放行原文 / 不可序列化对象放行原文（OpaqueData → InvalidDefinitionException 分支）

### 验证

- `mvn.cmd test`：AiOutputFilterTest 11/11 全绿；全量 ai2 回归 70/70 全绿（注入 16/成本 13/收口契约 14/基础设施 16/输出过滤 11——AnswerScoringEngine 12 与 FinanceAnalysisHandler 9 未在本轮清单，合计库内 ai2 用例 91）
- surefire 报告与 class 产物时间戳核对通过（15:03/15:04）

### 已知局限（记入评估文档）

- 流式（SSE）输出由 Handler 直发 emitter，逐 token 分片无法有效匹配跨片敏感词，本过滤仅覆盖同步路径——随网关多轮消息契约（P2）一并扩展

### 四同步

- 代码：AiOutputFilter（新增）+ AiGatewayService（两处接线）+ AiSceneConfig（enableOutputFilter 字段）；SQL：20260911-06；文档：评估文档（P1-3 勾选、维度 8 2.5→3.8→4.0、综合 3.75→3.77、测试 80→91 用例、行动路线第二批全清、安全治理 checklist 补勾，顺手补 P1-1 漏勾的"ai_execute_log 查询页"）+ 本记录
- 部署：①执行 SQL 20260911-06 ②重启 moyun-server ③按需为 C 端直出文本场景开启开关（如 `UPDATE ai_scene_config SET enable_output_filter=1 WHERE scene_code='finance_analysis' AND enabled=1`）④词库由 sys_sensitive_word 管理页维护，reload 后即时生效

---

## v11.61 (2026-09-11) P1-1 AI 执行日志管理页：统一网关日志从"数据在采"到"有人看"

> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-1：ai_execute_log 后端全字段落库（v11.57 起 cost_yuan 成本回填），但无管理端查询页——数据在采没人看。本版补齐查询/汇总/详情/清理闭环，是收口 7/7（v11.59）后网关可观测性的最后一块拼图。

### 后端：AiExecuteLogController（/cms/ai/execute-log，ai2/controller）

| 接口 | 权限 | 说明 |
|---|---|---|
| GET /list | cms:ai:execute-log:list | 分页列表，筛选 requestId（精确）/场景/模型（like）/状态（success/fail/timeout）/日期范围，create_time+id 双列倒序 |
| GET /summary | cms:ai:execute-log:list | 汇总卡片：COUNT/SUM(CASE success)/SUM(token)/SUM(cost)/AVG(elapsed)，**与列表同筛选联动**；failCount=total-success 全口径，successRate 保留 1 位小数 |
| GET /scene-options | cms:ai:execute-log:list | DISTINCT scene_code 场景下拉（来自日志本身，不依赖场景配置） |
| GET /{id} | cms:ai:execute-log:query | 详情（输入/输出摘要、工具调用 JSON、错误信息） |
| DELETE /{ids} | cms:ai:execute-log:remove | 批量清理（物理删除，日志只增不改；@Log 操作审计） |

- 汇总用 QueryWrapper.select 聚合下推 DB（COALESCE 兜底空表）；日期参数 "yyyy-MM-dd" → 当日起止，非法输入忽略不过滤
- 查询模式与既有索引对齐：idx_request_id/idx_scene_code/idx_create_time

### 前端：admin ai/execute-log（api/ai/execute-log.js + views/ai/execute-log/index.vue）

- 筛选区：场景下拉（scene-options）/requestId/模型/状态/日期范围，回车与变更即时查询
- 汇总卡片行：调用量/成功率（≥99 绿 ≥90 黄 其余红）/Token/成本(¥)/平均耗时（<1s 显示 ms）——随筛选联动刷新
- 列表：requestId 一键复制、模型+Agent 标签、Token/成本/耗时右对齐、状态三色、时间列
- 详情抽屉：全字段 + 输入/输出摘要 pre 展示 + toolCalls JSON pretty + 错误信息红色高亮
- 删除：确认弹窗 + v-hasPermi 权限指令；删除后列表与汇总双刷新

### SQL：20260911-05-ai2-execute-log-admin-v11-60.sql

- 菜单 5472（C，AI执行日志，挂 AI基础配置 5238，order 12）+ 5473（F 查询）+ 5474（F 删除），幂等 DELETE+INSERT，管理员角色授权
- 顺手修正 AiExecuteLogService javadoc 表名笔误（ai2_execute_log → ai_execute_log，实体 @TableName 与 DDL 实际为后者）

### 验证

- 后端：`mvn.cmd compile` BUILD SUCCESS（AiExecuteLogController.class 产物确认）
- 前端：`npm run build:prod` 构建通过
- 无单测新增（Controller 为薄查询层，聚合 SQL 与分页为 MyBatis-Plus 标准用法；护栏逻辑已在 v11.60 Ai2InfraSupportTest 覆盖基础设施层）

### 四同步

- 代码：后端 Controller + 前端页面/API；SQL：20260911-05（菜单，无表结构变更）；文档：评估文档（P1-1 勾选、维度 7 可观测性 3.5→3.7、综合 3.74→3.75、checklist 网关层补执行日志管理页条目）+ 本记录
- 部署：①执行 SQL 20260911-05 ②重启 moyun-server ③admin 前端重新构建发布；菜单需重新登录或刷新路由缓存后可见（AI基础配置 → AI执行日志）

---

## v11.60 (2026-09-11) P0-4 新链路测试覆盖：评分引擎/财务指标护栏/网关基础设施 37 用例，揪出 2 个真 bug + 修复 mvn PATH 劫持导致的假执行

> 依据《AI底座企业级评估-代码实测结论与改进清单》P0-4：AnswerScoringEngine（v11.47 拆出）零测试、FinanceAnalysisHandler 数值护栏零测试、FallbackStrategy/SceneRateLimiter/SemanticCache 零测试。本版补齐 37 用例并全部转绿，**P0 级四项（注入防护/成本熔断/场景收口/测试覆盖）全部完成**。

### 环境事故：mvn PATH 劫持导致此前的"假执行"

- 现象：`mvn` 命令静默"成功"（no output / exit 0），但 class 产物与 surefire 报告时间戳不更新
- 根因：`C:\Windows\System32\mvn`（无扩展名文档，疑似误放）在 PATH 中排在 `D:\dev_en\apache-maven-3.9.14\bin\mvn.cmd` 之前，PowerShell 将其当"文档"打开——**Maven 从未执行**
- 影响：v11.58~59 会话内的 `mvn compile/test` 全部为假阳性（v11.59 的 devlog"15 用例/44 个"数字虚报，已修正）
- 处置：改用全路径 `D:\dev_en\apache-maven-3.9.14\bin\mvn.cmd` 执行；本版全量重编译（1422 源文件）+ 全测试（80 用例）真实通过，**v11.58~59 的代码改动经受住了真实编译与测试验证**
- 待办：删除 `C:\Windows\System32\mvn`（需管理员权限，人工处理）

### AnswerScoringEngineTest（12 用例）——纯规则离线测试

- 评分公式：覆盖率×80 + 长度加成≤20（全覆盖长答案≥80 / 零覆盖短答案<40 / 半覆盖中间分）
- 6 维连续性：雷达图维度键对齐（relevance/professionalism/fluency/interactivity/confidence/logic）、[0,100] 越界钳制、fluency 随长度单调不降
- 关键词提取护栏：停用词/纯数字/单字符/超长词过滤、中英文逗号分隔、去重、12 上限
- LLM 动态题：无 tags 时从题干提取关键词（v11.30.2 修复项回归）；question=null 时长度推断覆盖率

### 单测揪出的 2 个真 bug（已修复）

1. **answer=null NPE**：`scoreAnswer` 中 `answer.length()` 未判空（转写失败/未作答场景直接 500）——统一为方法头 `int len = answer == null ? 0 : answer.length()`
2. **tags 分支无 MAX_KEYWORDS 封顶**：`extractKeywords` 的 tags 分支（与 solution 分支不同）无 12 词上限，20 个 tags 全量参与评分——补 `kw.size() < MAX_KEYWORDS` 检查

### FinanceAnalysisHandlerTest（9 用例）——Mockito 全 Mapper 离线测试

- **指标护栏**（LLM 只解读不计算的数值防线）：应急基金月数=流动资产/月均支出（8.3，HALF_UP 1位）、总资产（includeInTotal 过滤语义）、资产负债率/还款压力/储蓄率精确值、采样月数/赤字月数
- **健康分四段公式**：储蓄率×25 + 赤字≤25 + 负债率三段 + 压力三段；分段边界（0.5 严格小于、0.5<0.5 为 false 落 0 分段）
- **debtFact 清偿测算**：payoffMonths=ceiling(balance/月供)（50000/2000=25、50001/2000=26 向上取整）、无月供/余额清零不测算、期数进度透传
- **LLM 降级契约**：基类 sceneResolver/llmService 置 null → chatDetailed 失败 → aiEnabled=false + 模板综述（含精确指标）+ risks/suggestions 空 + **指标照常返回**（前端 KPI 不受影响）
- **input 契约**：缺 userId 抛 IllegalArgumentException、非法 range 回落 month、3m 窗口起点=当月起共3自然月

### Ai2InfraSupportTest（16 用例）

- **FallbackStrategy**：voice_interview 内置兜底（nextAction=end 防卡死）、sensitive_word 安全默认（放行不误伤）、其他场景通用 failure、配置 JSON 兜底优先、非法 JSON 原样文本、空白配置走内置
- **SceneRateLimiter**（mock RedisTemplate）：固定窗口计数（第1次 setExpire）、超限拒绝（6/5）、limit/window≤0 视为未配置放行、Redis 异常放行（限流不阻断业务）
- **SemanticCache**（内存 Map 模拟 Redis 往返）：精确命中（fromCache=true 标记）、未命中 null、ttl≤0 不写、场景隔离、查询异常降级直连

### 验证（全路径 mvn.cmd 真实执行）

- `mvn.cmd compile`：BUILD SUCCESS（1422 源文件，含 v11.58~59 全部改动的首次真实编译验证）
- `mvn.cmd test -Dtest=AnswerScoringEngineTest,FinanceAnalysisHandlerTest,Ai2InfraSupportTest,SceneConvergenceContractTest,TokenCostGuardTest,PromptInjectionGuardTest`：**Tests run: 80, Failures: 0, Errors: 0**（首跑暴露 5 处失败：2 个产品 bug + 3 个测试自身错误——mock 绕过 SQL 过滤的 includeInTotal=0 数据设计、健康分两处手算期望值错误，修正后全绿）

### 四同步

- 代码：AnswerScoringEngine（2 bug 修复）+ 3 个新测试类；SQL/菜单：无变更；文档：评估文档（维度9 3.9→4.1、综合 3.72→3.74、P0-4 勾选完成、测试子项 ★★→★★★、定位判断改写）+ v11.59 devlog 数字勘误 + 本记录
- 部署：重启 moyun-server（评分引擎 bug 修复影响线上转写空值场景）

---

## v11.59 (2026-09-11) P0-3 场景业务收口全部完成：resume/question + voice_interview 切统一网关（7/7）

> 依据《AI底座企业级评估-代码实测结论与改进清单》P0-3：v11.55 实测业务收口仅 1/7（全库唯一网关调用点=财务分析），其余 6 场景业务绕行网关直调 LLM，无执行日志/成本核算/限流/注入防护。v11.57 先行收口 sensitive_word/daily_topic；本版完成剩余 4 场景，**收口 7/7，全库 `llmClient.chat(` 清零**。

### P0-3b：resume_parse / resume_optimize / question_generate（7 个业务调用点）

- **Handler 子任务契约**（task+context 模式，提示词逐字收编进 Handler，业务侧只传数据）：
  - ResumeOptimizeHandler：advice / job_match / field_assist / draft_empty / deep_optimize 五子任务
  - QuestionGenerateHandler：jd_keywords 子任务（原 PortalJobTemplateServiceImpl.extractByLlm）
  - ResumeParseHandler：text 必填（收编原解析提示词）
- **业务切换**：ResumeParseService（解析）、ResumeAiAdviceService（建议）、ResumeJobMatchService（岗位匹配）、ResumeDeepOptimizeService×2（深优）、ResumeDeepOptimizeGenerator（深优生成）、PortalJobTemplateServiceImpl（JD 关键词）→ 统一经 AiSceneJsonClient
- **AiSceneJsonClient**（新组件 ai2/support）：构造请求→网关执行→解包 structured JsonNode；任何失败返回 null，业务保留原规则兜底（与收口前异常语义一致）

### P0-3c：voice_interview（5 处 llmClient 直调收口）

- **VoiceInterviewHandler 五子任务**（提示词逐字收编）：

  | task | 原调用点 | 输出 |
  |---|---|---|
  | answer_analysis | analyzeAnswerByLlm（回答深析：评分校正/6维/漏洞/水平/追问建议） | structured JSON |
  | candidate_ask | answerCandidateQuestion LLM 分支（反问应答） | {"text": ...} |
  | knowledge_desc | tryLlmKnowledgeDesc（知识点批量简介） | {"points": [...]} |
  | speak_text | generateSpeakText（轮次话术） | {"text": ...} |
  | self_intro | ScoringEngine.tryLlmSelfIntro（自我介绍 4 维评分） | structured JSON |
- **数据通道设计**：候选人转写/提问等外部不可信数据全走 input.transcript + PromptInjectionGuard.wrapData 隔离，**不走顶层 userInput**（避免意图分类器置信度<0.6 误打断——v11.57 sensitive_word 收口时已踩过的坑）；未知 task 在 validate 阶段快速失败
- **ScoringEngine.evaluateSelfIntro** 增加 userId 参数（网关限流身份/日志归属），调用方 handleIntroSubmission 传入 interview.getUserId()；parseIntroScore 改收 JsonNode（Handler 已容错解析，业务侧只做字段映射+权重融合）
- **InterviewSceneData** 新增 structured 透传字段；**AiSceneJsonClient 解包泛化**（ResumeSceneData/InterviewSceneData 双类型分派）
- **保留项**：Agent 多轮流式链路（InterviewAgentClient 6 处 chat/chatStream）走底座 Agent 框架路由（AiSceneBinding 灰度控制），网关多轮消息契约列为 P2 扩展方向；业务侧 LlmClient 依赖仅剩 isEnabled() 前置短路判断（避免 AI 关闭时空走网关链路）
- **默认评估路径加固**：VoiceInterviewHandler 默认路径（管理台调试）的候选人回答拼接改 wrapData 数据通道隔离

### 验证
- `mvn compile -DskipTests`：BUILD SUCCESS
- `mvn test -Dtest=SceneConvergenceContractTest,TokenCostGuardTest,PromptInjectionGuardTest`：全绿（SceneConvergenceContractTest 扩至 14 用例：resume_optimize 5 + question_generate 2 + resume_parse 1 + voice_interview 6，覆盖五子任务参数校验/未知 task 拒绝/默认路径必填）。注：v11.59 当时的 mvn 命令因 PATH 劫持未真实执行（详见 v11.60），本条数字按 v11.60 真实运行结果修正
- 全库 grep `llmClient.chat(`：0 命中（收口完成标志）；网关业务调用点 14 处（aiGatewayService.execute 5 + aiSceneJsonClient.executeForJson 9）

### 四同步
- 代码：上述 8 个业务类 + 3 个 Handler + AiSceneJsonClient/InterviewSceneData；SQL/菜单：**无变更**（voice_interview 等 7 场景在 ai_scene_config 已注册且 enabled=1，20260909-ai2-unified-gateway.sql）；文档：评估文档（维度6 4.1→4.3、综合 3.70→3.72、P0-3 勾选完成、网关层 checklist+收口条目）+ 本记录
- 部署：重启 moyun-server；行为变化——面试/简历链路 LLM 调用开始产生 ai_execute_log 记录与 cost_yuan 成本核算，受场景限流与日 Token 配额约束（默认值不变）

---

## v11.58 (2026-09-11) CMS 栏目管理：修复存量三级栏目无法编辑的问题

> 用户反馈：编辑栏目只想修改路由路径（`/reading/space/quotes` → `/reading/quotes`），却被"最多只支持两级栏目，不能设置为三级"拦截。

### 根因
- 存量数据 `id=88 金句摘录(reading-space-quotes)` 挂在 `id=71 读书空间` 下，而 71 本身是 `id=54` 的二级栏目——88 实为三级（历史 SQL 直插绕过校验）
- 原 `updateCategory` 对**每次保存**都校验父级层级，即使 parentId 未变化，导致存量超两级数据任何字段都无法编辑

### 修复（仅父级变化时才校验层级）
- **后端** CmsCategoryServiceImpl.updateCategory：先查旧记录对比 oldParentId，父级未变则跳过层级校验
- **前端** category/index.vue：新增 `originalParentId` 记录编辑前父级，`submitForm` 校验条件由 `isLevel2 && parentId!==0` 改为"父级发生变化且非 0"；`reset`/`handleUpdate` 同步维护

### 校验行为（修复后）
| 场景 | 结果 |
|---|---|
| 编辑存量三级栏目，只改路由/名称等 | 允许保存 |
| 新增时选择二级栏目作父级 | 拦截（不能设置为三级） |
| 给二级栏目"新增子栏目" | 拦截（前后端双重） |
| 一级栏目改挂到二级栏目下 | 拦截 |

### 验证
- `mvn compile -DskipTests -pl moyun-server -am`：编译通过

### 部署
重启 moyun-server；管理端为 vite dev 热更新直接生效（生产需重新构建）。

---

## v11.57 (2026-09-11) P0-1 注入防护 + P0-2 成本熔断落地（评估整改）

### P0-2 成本监控与熔断（依据《AI底座企业级评估-代码实测结论与改进清单》P0-2）
- **成本回填**：ai_execute_log 新增 cost_yuan DECIMAL(12,6)（SQL 20260911-03）；AiExecuteLogService 调用成功后按模型单价×实际 token 核算——复用 TokenUsageService.calculateCost（与 Token 统计页同源同口径，ai_model_config 既有 input_price/output_price 单价列，5 分钟缓存）；部分供应商仅回传合计 token 时按输出单价保守估上界
- **Token 精细化**：ChatOutcome/AiMetadata 新增 inputTokens/outputTokens（AbstractAiSceneHandler 从 ChatResponse.usage 回填），成本核算从"仅合计"升级为"输入/输出分开计价"
- **场景日配额熔断**：ai_scene_config 新增 daily_token_limit（null/0=不限）；新增 TokenCostGuard（ai2/support）——Redis INCR 日累计（键含 yyyyMMdd 跨日自然切换，TTL 2 天），网关 execute 前置配额检查，超限拒绝（AI_TOKEN_LIMIT_EXCEEDED）+ 日志留痕（status=fail, error=token_limit_exceeded），成功后按实际 tokenUsed 累加；Redis 异常放行（fail-open，基础设施抖动不阻断业务）
- **admin 前端**：场景配置"限流与降级"页签新增"日Token上限"字段（步骤 10000，空/0=不限，含场景级共享语义提示）
- **注入防护补强**：SUSPECT_HIJACK 补充中文"请你(来)扮演/充当"模式（原测试用例"请你扮演一位资深前端专家"漏匹配 → 修复对齐，SUSPECT 仅标记不拦截）
- **单测**：TokenCostGuardTest 13 用例（纯判定/配额检查含脏数据与 Redis 异常放行/消费累计含 TTL 设置时机）+ PromptInjectionGuardTest 回归，29 用例全绿；后端编译 BUILD SUCCESS
- **文档同步**：评估文档维度7 2.9→3.5（成本金额）、维度8 3.2→3.8（熔断落地）、维度9 测试行更新（ai2 新增 29 用例）、P0-2 勾选完成

### P0-1 变更（依据《AI底座企业级评估-代码实测结论与改进清单》P0-1）
- **新增 PromptInjectionGuard**（ai2/support）：双通道防护——
  - 指令通道（顶层 userInput）：sanitizeAndCap 清洗（控制字符/零宽字符/8000 截断）+ scan 扫描（指令覆盖/提示词探取中英模式 → DANGEROUS 拒绝；角色扮演 → SUSPECT 放行，业务存在合法角色扮演）
  - 数据通道（input 字符串值）：字符级清洗；Handler 嵌入不可信数据用 wrapData 分隔符隔离（防误杀简历/文档类数据，符合 OWASP LLM 指引）
- **网关接线**：execute 与 executeStream 双路径；DANGEROUS → INPUT_REJECTED(1008) + ai_execute_log 留痕（status=fail, error=prompt_injection_blocked）
- **Handler 改造**：ResumeParseHandler/SensitiveWordHandler 用户文本拼接改 wrapData 隔离
- **单测**：PromptInjectionGuardTest 16 用例（清洗/截断/中英危险模式/角色扮演放行/良性零误杀/包裹格式），全绿
- **文档同步**：评估文档维度8 评分 2.5→3.2、综合 3.55→3.61、P0-1 勾选完成

### 四同步
- 代码：编译+单测通过；SQL/菜单：无关（纯网关横切）；文档：本记录+评估文档更新

## v11.56 (2026-09-11) AI 底座企业级评估·代码实测结论文档

### 内容
- 对照《AI 底座企业级评估标准与项目现状分析.md》（v11.48 基线、基于文档推断）做全量代码实测，产出配套结论文档：`docs/11-记账模块需求分析/AI底座企业级评估-代码实测结论与改进清单.md`
- 实测方法：4 路并行扫描（模型/网关横切、Agent/工具、RAG/工作流、场景/安全/测试）+ 关键点全库 grep 复核

### 核心结论（13 项证据修正）
- **9 项原文档低估**：密钥 AES-GCM 加密已有、[TOOL_CALL]+ToolRegistry 工具链路已有、语义缓存已实现（0.95 余弦）、意图识别已接线（v11.52）、限流已生效、降级策略已实现、RAG 查询改写+扩展已有、JVector 已落地、Redis 窗口记忆已有
- **2 项高估**：场景业务收口实为 1/7（全库唯一 aiGatewayService.execute 业务调用点=财务分析）；测试 12 个全在老 chat/RAG 包，AnswerScoringEngine/ai2 网关/财务/资金链路零测试
- **2 项确认**：Prompt 注入防护为零（userInput 顶层化后风险面更大）、输出过滤/成本熔断为零
- 综合评分 3.10 → 3.55；原文档 P0-1（FC 从零建设）取消，改为 Schema 校验增强（P1）；安全类（注入/输出过滤/熔断）前移第一批

### 四同步
- 纯文档变更（代码/SQL/菜单无关）；后续改造按结论文档第四部分 P0/P1/P2 清单执行

## v11.55 (2026-09-11) 财务分析异步任务化 + 报告多版本

### 背景
- 换绑快速模型后 LLM 生成成功，但 30-60s+ 的同步等待体验差（前端转圈/超时风险）
- 旧报告表 UNIQUE(user_id,period) 覆盖式存储：重新分析丢上一版，无法对比回看

### 变更
**后端（异步任务 + 多版本）**
- 任务三件套（LedgerAiAnalysisServiceImpl + PortalLedgerAiController）：
  - `POST /portal/ledger/ai/analysis/task`：提交任务返回 taskId（UUID 16 位）；**同用户进行中任务复用**（Redis running 标记，防重复烧 token）
  - `GET /portal/ledger/ai/analysis/task/{taskId}`：轮询状态 pending/running/success(带完整report)/failed(带error)/not_found；归属校验（非本人任务视为不存在）
  - 执行走 applicationTaskExecutor（复用 @EnableAsync 线程池），完成/失败回写 Redis 任务态（TTL 30 分钟），running 标记释放（TTL 15 分钟兜底）
- 多版本（SQL 20260911-02 + 代码）：
  - 表：DROP UNIQUE uk_user_period → 普通索引 idx_user_period；每次生成 INSERT 新记录
  - 快照命中：同 period 取最新一条比对指纹（旧版本保留）
  - `GET /reports/{id}` 详情（完整回看）/ `DELETE /reports/{id}` 删除（归属校验）
  - 历史列表按 id 倒序（新版本在前），"共 N 份"
- 旧同步接口 `/analysis` 保留语义：页面进入走它（快照命中毫秒级），主动重算改走任务接口

**前端（moyun-ledger-app）**
- api/ledger.js：submitAiAnalysisTask/getAiAnalysisTask/getAiReportDetail/deleteAiReport
- analysis/index.vue：
  - "重新分析" → 异步任务 + 2s 轮询（最长 10 分钟）；页面内"生成中"提示条（转圈动画，替代 uni.showLoading 白屏），期间可自由浏览
  - 历史列表"查看"升级：详情接口完整回看（指标/风险/建议还原到页面，替代旧 showModal 只显综述）
  - 历史列表每行加"删除"（确认弹窗）
  - onHide/onUnload 清理轮询定时器

### 四同步
- 代码：后端编译通过；SQL：20260911-02-ledger-ai-report-multi-version.sql（含索引存在性预查）；菜单：无关；文档：本记录
- 验证：① 执行 SQL（DROP UNIQUE 是唯一人工确认项）→ 重启后端；② 分析页点"重新分析"→ 立即出现"生成中"提示条，期间可切页；③ 完成后 toast"分析完成"自动渲染新报告；④ 连点两次"重新分析"→ 第二次复用进行中任务（resubmitted=true，不重复烧 token）；⑤ 历史列表同月多条版本可回看/删除；⑥ 进页面（快照命中）依旧毫秒级返回

## v11.54 (2026-09-11) 财务分析"LLM 无返回"根因修复：空响应静默分支 + 模型超时

### 根因（日志实证，两类失败叠加）
- 用户反馈：分析页一直走 `if (!outcome.isSuccess()) → LLM 无返回，降级模板综述`
- **失败A（超时重试）**：`RestClientException → SocketTimeoutException`（okhttp Http2 StreamTimeout）+ langchain4j RetryUtils 重试 3 次（attempt 1/2/3 of 3）——模型17（deepseek-v4-pro，https://api.deepseek.com）对财务分析的长上下文+大 JSON 生成耗时超过模型客户端读超时；单次调用实测 279~298 秒
- **失败B（空响应静默）**：另一并发请求 HTTP 200 但 content 为空（疑似推理模型只出 reasoning_content 或触发内容审查），旧 chatDetailed 代码 `resp.aiMessage() != null` 即 return——text=null 直接静默失败，无任何日志（本次排障最大盲区）
- 反证：管理页"测试连接"（简单文本）3 秒 HTTP 200 —— API 连通与密钥正常，纯属长生成超时

### 修复
1. **代码（chatDetailed）**：空内容分支不再静默 return——记录 WARN（含模型名）并回落默认模型再试一次；成功判定收紧为 `text 非null非blank`（AbstractAiSceneHandler）
2. **SQL（20260911-01）**：模型17 timeout 条件提升至 180 秒（仅当 <180 时）；注释说明 langchain4j 硬编码重试 3 次的最坏等待，及换绑非推理快速模型的建议

### 待观察
- 若 deepseek-v4-pro 为推理型模型，长 JSON 生成 5 分钟+ 属正常行为——建议 finance_analysis 场景绑定非推理快速模型（deepseek-chat 类），吞吐优先；或等 chat 收口后改流式调用
- ai_scene_config.timeout_seconds 字段仍未被消费（网关建模型走 Agent→模型配置的 timeout），列入断点清单待接线

### 四同步
- 代码：编译通过；SQL：20260911-01-model17-timeout-v11-54.sql（条件 UPDATE+前后验证查询）；菜单：无关；文档：本记录
- 验证：执行 SQL → 重启 → 分析页"重新分析"；若仍降级，日志现必有 WARN（"绑定模型返回空内容"或"场景绑定模型调用失败"）可定位真实原因

## v11.53.1 (2026-09-10) 死代码清理：基类孤儿方法 parseJson(String, Class)

### 审计范围（v11.48~v11.53 触达文件全量扫描）
- LedgerAiAnalysisServiceImpl：~440 行全量复核，所有私有方法均有调用（v11.50 薄化重构无残留）
- FinanceAnalysisHandler：27 个私有方法逐一引用计数，全部存活
- AbstractAiSceneHandler：14 个 protected 方法全库引用扫描，**唯一孤儿 = parseJson(String, Class)**（7 个 Handler 全部使用 parseJsonMap；LedgerAiAnalysisServiceImpl 的 parseJson 是其自有私有方法，同名不同物）
- AiGatewayService：8 个私有方法全部存活（v11.52 删 getStringInput 后无新孤儿）
- SensitiveWordHandler：v11.52 迁移（input.text→userInput）无残留；管理前端无旧 key 引用

### 删除
- AbstractAiSceneHandler.parseJson(String, Class<T>)：零调用者，与 parseJsonMap 能力重复（均基于 extractJson 容错提取）。编译通过

### 四同步
- 代码：编译通过；SQL/菜单：无关；文档：本记录

## v11.48 (2026-09-10) finance_analysis 场景标准化：风险/建议完全交 LLM + 配置即时生效

### 背景
- 用户评审 `/portal/ledger/ai/analysis`：旧实现规则引擎把结论都算完，LLM 只做皮毛综述；且 ai_scene_config 表填了配置却不生效，与建表初衷（配置驱动、免发版调提示词）相悖
- 核查发现 v11.48 前次重构烂尾：`callAiReport`/`castList` 被调用但未定义，编译不过；网关用 `FinanceAnalysisHandler.setCurrentConfig` 静态 ThreadLocal hack 给所有场景注入配置；`AiSceneRegistry.configMap` 仅启动加载一次，管理页改配置永不生效

### 变更（对齐《AI能力统一接入层 — 完整方案文档》V2.0 §4.2/§5.1/§5.2/§5.3）
- **业务侧标准化流程落地**（LedgerAiAnalysisServiceImpl 重写）：
  查数据（流水/资产/负债/画像 + 新增支出结构 Top5）→ 组装 ledgerContext（结构化数据 + 规则算好的指标护栏 + 债务到期等时点事实）→ 网关 execute(finance_analysis, {ledgerContext, window}) → LLM 生成标准 JSON {summary, healthScore, risks[], suggestions[]} → 解析合并（健康分以规则值为基准，LLM 值不采纳）→ 落表 → 返回
- **风险/建议完全交 LLM**（用户决策：先看效果不做规则兜底，LLM 挂了仅综述降级模板、风险/建议列表为空）；删除本地硬编码 risk/suggest 生成死代码与 v11.39 直调链路（generateSummary/buildPrompt/callLlm/callLlmDirect 等 6 方法）
- **网关去 hack**（AiGatewayService）：改用 AiSceneHandler 接口 v11.48 预留的 `execute(request, config)` 双参方法下发配置；FinanceAnalysisHandler 删除静态 ThreadLocal（该 hack 会给所有场景错误注入 finance 配置）
- **配置即时生效**（AiSceneRegistry）：getConfig 直查 ai_scene_config（priority DESC LIMIT 1，LLM 调用秒级，索引查询开销可忽略），管理页改提示词模板/输出结构/绑定关系**下次调用立即生效**，无需重启或手动 refresh
- **快照与配置变更联动**：数据指纹新增 `cfg:` 段（finance_analysis 配置行 update_time），改模板当月快照自动失效重算，"改配置立即生效"端到端闭环
- **死代码清理**：LedgerSceneData.java（零引用，文档 §4.3 的草图结构已被 v11.48 Map 结构取代，经全库引用核查后删除）
- **SQL**：新增 [20260910-ai2-finance-analysis-templates.sql]——finance_analysis 种子模板（system/user 模板 + output_schema + prompt_placeholders），仅空值时填充不覆盖管理员已填值，幂等可重放

### 四同步
- 代码：编译通过（mvn compile）；SQL：见上；菜单：无新接口、复用 `cms:ai:scene:*` 权限，无需变更；文档：本记录 + 开发进度与规划.md 场景迁移条目更新

### 遗留与后续
- 语义缓存提示：enable_cache 开启时 ledgerContext 中金额变动会导致缓存键变化，命中率取决于数据变更频率（规划 P2 灰度验证项）
- 后续场景迁移（voice_interview 等）复用本次确立的双参 execute 模式，不再出现 ThreadLocal 类 hack

## v11.53 (2026-09-10) 财务分析 LLM 链路修复：Agent 人设带偏防呆 + 文本兜底 + Agent 参数调优

### 问题定位（日志实证，非猜测）
- 现象：分析页综述仅一句话"（模板分析）根据你的记账数据……整体收支可控"，标签"基础分析"
- 日志铁证：LLM 实际返回 `"您好"`、`"我需要先获取你的基础财务数据"`、`"我需要基于你的真实收支……"` ——**对话文本而非 JSON** → parseJsonMap 失败 → 降级模板
- 部分调用 JSON 被截断（`Unexpected end-of-input`）——max_tokens 偏小
- 根因：Agent 47「财务分析师」（管理页创建）人设为对话式；mergePersona 将人设前置且无边界约束，LLM 把后台批处理任务当聊天，无视 output_schema 的 JSON 指令

### 修复（三层）
1. **基类防呆（根治所有场景）**：mergePersona 注入人设后追加【任务边界】声明——"系统自动执行的后台分析任务，数据已完整提供：不要问候、不要反问、不要索要信息，严格遵守输出格式"。任何风格人设都不会再把任务带偏成聊天（AbstractAiSceneHandler）
2. **文本兜底（Handler）**：LLM 返回非 JSON 时，cleanLlmText 清洗（剥围栏；{ 或 [ 开头的 JSON 残骸视为截断不入综述）后作为 summary 展示，risks/suggestions 留空——符合既定契约"LLM 挂了就只有综述没有风险列表"，LLM 生成内容不再整体丢弃（FinanceAnalysisHandler + 基类 cleanLlmText）
3. **Agent 参数调优（SQL）**：Agent 47 max_tokens <4096 提升（JSON 报告实测 2000+ tokens，截断主因）、temperature >0.3 收敛（结构化输出要稳定）。resolveChatModel 确认消费 agent.temperature/maxTokens。不覆盖用户编辑的人设内容

### 提示词合并契约（本次审计明确）
最终 system = Agent人设（含任务边界声明）+ ai_scene_config.system_prompt_template + output_schema 约束——Agent 定义"是谁"（口吻/视角），场景配置定义"做什么/怎么输出"，两者都生效、互不覆盖

### 四同步
- 代码：编译通过；SQL：20260910-05-agent47-params-v11-53.sql（条件修复+验证查询）；菜单：无关；文档：本记录
- 验证：① 执行 SQL → 重启 moyun-server → 分析页"重新分析"；② 预期：LLM 按 JSON 输出完整报告（summary 讲数据故事 + risks 带"依据" + suggestions 带"预期"），标签变"AI 分析"；③ 若模型仍偶发截断，ai_execute_log.elapsed_ms 可观测，max_tokens 可继续调大


## v11.52 (2026-09-10) 入参契约语义分层：userInput 提升为顶层显式字段

### 背景
- 审计发现网关意图判断分支读 `input.userInput`，但全库无任何调用方传入（死分支）；各 Handler 文本参数 key 各自为政（text/question/resumeText）。
- 用户决策：不删（chat 收口/Agent-First 预留），但要在合理位置显式定义——对话类场景天然需要"用户自由文本"。

### 契约改动
- **AiExecuteRequest 新增顶层字段 `userInput`**（@Size(max=8000)），语义分层确立：
  - `userInput` = 用户自由文本（人打的原始输入）——网关横切（IntentClassifier 意图路由、SemanticCache 键）统一消费
  - `input` = 结构化业务参数（userId/range/position 等强类型键值）——Handler 自定义取值，网关不解释
- **网关两处读取点迁移**：意图判断 `getStringInput(request,"userInput")` → `request.getUserInput()`；primaryInputText 优先顶层字段（原 input.userInput/text 特判移除）。
- **基类新增 `requireUserInput(request)`**：对话/检测/生成类 Handler 取用户文本的统一入口。
- **SensitiveWordHandler 迁移**：input.text → 顶层 userInput（validate 快速失败 + execute 取参）。零影响：业务侧敏感词走 DFA 词树 ISensitiveWordService 不过网关，该场景无内部调用方。
- **死代码清理**：getStringInput 无调用方，删除。
- 意图分支现状说明写入代码注释：chat 收口后对话消息即 userInput，该分支成为场景路由器。

### 四同步
- 代码：编译通过；SQL：无关（纯契约层）；菜单：无关；文档：本记录 + 方案文档 §4.1 入参定义与语义分层说明同步更新
- 验证：① 重启后 POST /api/ai/execute {"sceneCode":"sensitive_word","userInput":"测试文本"}（需先开 open_api）→ 正常返回检测结果；② 不传 userInput → 网关 validate 快速失败返回参数错误；③ 财务分析等结构化场景行为不变（不传 userInput 意图分支自然跳过）


## v11.51 (2026-09-10) AI 底座闭环补强：可观测性（metadata）+ 通用入口白名单（open_api）

### 背景
- 契约全景审计发现两大断点：① AiMetadata 七字段仅 fromCache 被填充（模型/Agent/token 不可观测）；② /api/ai/execute 可直调任何 enabled 场景，可越权绕过业务 Controller 编排（如 finance_analysis 的鉴权/快照/落表）。

### 断点① 可观测性闭环
- 新增 ChatOutcome 模型：LLM 调用结构化结果（text + modelUsed + modelProvider + tokenUsed，取自 langchain4j ChatResponse 的 tokenUsage/metadata.modelName）。
- AbstractAiSceneHandler：chat() 委托新 chatDetailed()（向后兼容）；新增 buildMetadata(outcome) 辅助。
- AiGatewayService：injectAgentPersona 返回 Agent 名，fillAgentMetadata 统一补 agentUsed（Handler 填模型/token，网关补 Agent——职责清晰不重复）。
- FinanceAnalysisHandler 接入 chatDetailed，成功响应携带 metadata。
- AiExecuteLogService.record 签名升级：第5参数 String modelUsed → AiMetadata，model_used/agent_used/token_used 三列自此有数据（表列 v11.39 就预留，一直空置）。
- 顺序保证：fillAgentMetadata 先于 record 调用，日志三元组完整。

### 断点⑤ 通用入口白名单
- ai_scene_config 新增 open_api 列（TINYINT(1) DEFAULT 0）：存量场景默认关闭。
- AiGatewayController：/execute 与 /execute/stream 前置校验 open_api，未开放返回 SCENE_NOT_OPEN(1007) / SSE error 事件。业务 Service 直调 AiGatewayService 不经 Controller 层，不受限。
- 管理页场景配置"基础配置"tab 新增"开放API"开关（表单/默认值/编辑回填三处同步），listScenes 总览输出 openApi。

### 四同步
- 代码：编译通过；SQL：20260910-04-ai2-scene-open-api-v11-51.sql（增量）+ 20260909-ai2-unified-gateway.sql 末尾追加同款 ALTER（新环境全量初始化）；菜单：无关；文档：本记录
- 验证：① 执行增量 SQL → 重启 → 调财务分析 → ai_execute_log 新记录的 model_used/agent_used/token_used 有值；② curl POST /api/ai/execute {"sceneCode":"finance_analysis"} → 返回 code=1007 SCENE_NOT_OPEN；③ 管理页编辑场景可见"开放API"开关，开启后 /api/ai/execute 可调用


## v11.50 (2026-09-10) finance_analysis 架构重构：数据组装下沉 Handler，Service 薄化

### 背景
- 用户明确架构方向："坚持配置，差异只在查库和拼上下文，独立编码交给各自 Handler，最后调 Agent，不要太多逻辑放 Service"。
- 审计确认前后台配置标准一致（管理页字段完整、Controller 全量保存、网关消费链路通）；遗留不一致：output_parser/output_mode 可编辑但零消费（已列 Phase 1 UniversalSceneHandler 接线）。

### 重构内容
- **FinanceAnalysisHandler 迁至 com.moyun.ledger.handler**（原 ai2/handler/impl 删除）：数据组装（查库/指标/趋势上下文/画像文本）+ 模板渲染（人设→system→output_schema→user）+ LLM 调用 + LLM 失败内部降级（模板综述+空列表，指标照常返回）。放业务包的原因：Handler 需注入 ledger Mapper，避免 ai2 基础设施反向依赖业务模块（依赖方向恒为 业务→ai2）。
- **LedgerAiAnalysisServiceImpl 薄化**（856→约400行）：只承担 快照缓存（指纹）/ 网关调用 / 报告落表 / 画像管理。Handler 输入简化为 {userId, range}，返回完整报告 data。
- **双重缓存消歧**：网关 TTL 语义缓存关闭（20260910-03 SQL，enable_cache=0）——Handler 现返回实时指标，TTL 缓存会返回过期数据；缓存职责统一归 Service 数据指纹快照（数据/预算/画像/配置变化自动失效）。
- **指纹 pf 段**：快照存档 profileStamp 保留标签翻译（存档可读性），指纹判断粒度不变。

### 四同步
- 代码：编译通过；SQL：20260910-03-ai2-finance-analysis-v11-50.sql（幂等）；菜单：无关；文档：本记录
- 验证：重启后调 /portal/ledger/ai/analysis?range=month，行为与 v11.49 一致（综述/风险依据/建议预期/应急基金指标）；日志无 ai2 错误；ai_execute_log 有新记录

## v11.49.1 (2026-09-10) 修复 ai_execute_log 落库失败：实体列名与 DDL 不符

### 现象
- v11.49 验证时发现每次网关调用后 warn：`Unknown column 'created_at' in 'field list'`——执行日志全部落库失败（不影响业务主流程，但可观测性数据缺失）

### 根因与修复
- AiExecuteLog 实体映射 `@TableField("created_at")`，而 DDL（20260909-ai2-unified-gateway.sql）定义的列是 `create_time`（表已按 DDL 建好，DDL 为权威）。实体/Service 改为 `create_time`，顺带修正实体注释表名笔误（ai2_execute_log → ai_execute_log）
- 全库 grep 确认无其他 createdAt 引用（残留命中均为 Knowledge/Article 等无关实体）

### 四同步
- 代码：编译通过；SQL：无变更（表结构本来就正确）；菜单：无关；文档：本记录

## v11.49 (2026-09-10) finance_analysis 深度增强：趋势/预算/应急基金上下文 + evidence/expectedImpact 契约 + Agent 人设打通

### 背景
- v11.48 落地后用户反馈报告"太简单、没参考意义"。诊断：三层根因——上下文薄（LLM 只拿到 10 个汇总数字，看不到趋势/预算/流动性）、输出契约扁平（风险无依据、建议无量化预期）、Agent 编辑器未接入（agent_id 字段闲置，人设硬编码在 Handler）

### 变更
- **A 深度上下文**（LedgerAiAnalysisServiceImpl）：
  - `buildTrendContext`：近6个月流水 → 逐月收支序列（income/expense/surplus）+ 分类环比 Top5（本月 vs 上月含 changePct）+ 预算执行（总/分类预算 usedPct、剩余、月末剩余天数，复用既有 ledger_budget 链路）
  - 应急基金月数 = 流动资产（cash/savings/ewallet/stored_value）÷ 月均支出，入 indicators 护栏
  - debtFact 增清偿测算（payoffMonths = 余额 ÷ 月供，不含息近似）
  - 数据指纹：tx 窗口扩至近6个月（与趋势口径一致，上月流水变化同样失效快照）+ 新增预算签名（bd）
- **B 输出契约富化**：risks[].evidence（数据依据）、suggestions[].expectedImpact（量化预期）贯穿 Handler 解析/默认模板/前端展示（风险"依据"标签、建议"预期"标签）
- **C Agent 打通**：网关 `injectAgentPersona`（ai_scene_config.agent_id → ai_agent.system_prompt 渲染 {{占位符}} 后注入 input.agentPersona，位于缓存键计算前——人设变更自动失效缓存）；`AbstractAiSceneHandler#mergePersona` 统一前置人设。分工：Agent 定义"是谁"，场景配置定义"做什么/怎么输出"。同步/流式入口均注入；Agent 禁用/无提示词/失败静默跳过
- **默认系统模板重写**（反空话硬约束）：禁"建议合理规划"类空话、综述讲"发现的故事"、风险必须给 evidence、建议必须可执行且量化 expectedImpact
- **前端**（analysis/index.vue）：流动资产/应急基金月数（含分级标注）行、风险"依据"、建议"预期"展示

### SQL
- 新增 [20260910-02-ai2-finance-analysis-templates-v11-49.sql]：模板覆盖升级（仅旧种子前缀匹配才更新，管理员自定义不覆盖；幂等可重放）

### 四同步
- 代码：编译通过；SQL：见上；菜单：无新接口；文档：方案文档 §5.1 增"实现演进注记"（双参 execute/Agent 人设合并/配置即时生效）、本记录

### 效果预期与验证
- 验证：执行 SQL → 重启 → `GET /portal/ledger/ai/analysis?range=month`（refresh=true）→ 风险应带数据依据（如"餐饮环比+65%"）、建议应带量化预期（如"每月约节省¥800"）；管理页给 finance_analysis 绑定 agent_id → 人设生效且不脏读缓存
- 后续场景迁移范式：业务侧只组装结构化 context → 网关（人设注入/缓存/限流）→ Handler 读配置模板渲染 → 富化 JSON 契约

## v11.47 (2026-09-10) 上帝类拆分第一步：AnswerScoringEngine 抽取

### 背景
- Phase 1 P0「拆上帝类」启动：VoiceInterviewServiceImpl 3169 行按"评分 / 题目选择 / 文案构建"三块小步剥离，每步编译验证（用户确认小规模持续处理模式）
- 本轮第一块：**规则评分引擎**——无 LLM 依赖、方法边界完整（scoreAnswer + 6 维计算 + 关键词提取 + 反馈构建），是最安全的切分点

### 变更
- 新增 [AnswerScoringEngine.java]（185 行，@Component）：`scoreAnswer`/`extractKeywords`/`clamp`/`countStructureWords`/`countInteractiveSignals` + 停用词表整体迁入；`ScoreResult` 从主服务 private 内部类升为公共静态类（字段/构造器签名不变，主服务 19 处引用零改动兼容）
- 主服务 `scoreAnswer` 改为单行委托注入的引擎；删除已迁出的 5 个方法与 2 个孤儿常量（MAX_KEYWORDS/STOPWORDS）；`clamp` 保留（LLM 解析 parseAnalysis 仍用）
- LLM 融合评分（scoreSent/fused）留在主服务——依赖场景模型调用，属流程编排层，后续随网关 voice_interview 场景迁移处理
- 主服务 3169 → **3031 行**；评分逻辑现可脱离 Spring/LLM 独立单测（Phase 1 单测任务的第一个可测单元）

### 四同步
- 代码：编译通过；SQL/菜单：纯内部重构无表与接口变更，无需同步；文档：本记录

### 后续拆分计划（小步进行）
1. 题目选择块（buildQuestionPaper 召回链）→ QuestionSelectEngine
2. 文案构建块（提示词拼接）→ PromptBuilder
3. LLM 评分迁移至 AI 网关 voice_interview 场景

## v11.46 (2026-09-10) 死代码清理第一轮 + 评审结论入规划

### 规划路线更新
- Phase 1 重排为「AI 统一网关收口 + 质量攻坚」：拆上帝类（VoiceInterviewServiceImpl 3169 行）、资金链路单测、死代码清理、VIP 付费墙四项 P0 入列

### 死代码清理（本轮 2 组，均经"前端三端无引用 + 后端无内部调用"双重验证）
1. **架构图同步生成旧链路**（已被流式版替代）：删除 `DiagramController`（POST /cms/ai/diagram/generate）、`DiagramService` 接口、`DiagramServiceImpl`、`DiagramGenerateDTO` 4 文件
   - 前端 chat.vue 已改用 `/cms/ai/diagram/chat/stream`（DiagramStreamController，`cms:ai:diagram:list` 权限），同步接口零引用
   - 菜单核查：sys_menu 仅 5057（diagram/chat 页面，list 权限）在用，无 `diagram:query` 按钮权限 → **SQL/菜单无需变更**
2. **AI 仪表盘实时接口**：删除 `DashboardController.getRealtime`（GET /cms/ai/dashboard/realtime）——dashboard 页仅调 overview，realtime 前端零引用；复用权限 `dashboard:list` 仍被 overview 使用 → 菜单无需变更

### 保留判定（避免误删）
- AiGatewayController 的 /api/ai/execute 等网关接口：当前前端无调用，但属 Phase 1 规划内统一入口，保留
- 后续每轮清理遵循：删除前交叉验证前端 api/页面直连 fetch、后端内部转发、菜单权限按钮三处引用

## v11.45 (2026-09-10) 规划路线文档更新至 v11.44 现状

### 06-规划路线/开发进度与规划.md 全面重写
- 原文档停留在 v9.5（2026-08-16），落后实际进度约 70 个版本，已按 devlog 记录同步至 v11.44
- **新增「重大项目转折分析」章节**：回溯 6 次方向性调整并分析动因与影响——
  v9.0 战略收缩（砍 PK/圈子聚焦内容）、v9.5 分支合并基线、v10.x AI 面试核心差异化（旧 MockInterview 下线）、
  v11.0 商业化落地、v11.14 记账 App 第四端新产品线、v11.38 AI 统一网关 Agent-First 方向
- **新增「版本里程碑对照」**：v9.5 时点原规划 vs 实际演进——AI 面试/记账产品线/AI 网关均为计划外主动投资，
  数据运营与 VIP 商业化持续后移，结论"基建强、营收弱"，下一阶段需并行补齐
- **下一阶段路线图重排**：Phase 1 AI 网关收口（剩余场景迁移 P0）→ Phase 2 Agent-First（工具注册/Workflow Agent 节点/行为预测）→ Phase 3 商业化与运营并行 → Phase 4 质量生态
- 技术债务清单更新：新增 TD-09（12 处 LLM 直调待收口）、TD-10（devlog 含二进制字节）

## v11.44 (2026-09-10) docs 目录阶段性规整

### 删除（18 文件）
- `08-原型设计/` 整目录（12 文件）：原型已落地且多轮优化，HTML 原型稿不再保留
- `09-归档/` 整目录（5 文件）：v4.0/v5.2/v7.8 历史评审与检讨文档
- 根目录 `Relevant Code Snippets.md`（AI 会话草稿）、`栏目重构设计-20260819.md`（与 05-方案设计重复副本）
- `05-方案设计/题库模块重构方案-20260820.md`（已被综合评审 20260830 替代）
- `11-记账模块需求分析/新增记账应用的需求说明书.md`（已被 V1.1 需求 + V1.3 设计替代）
- `10-大模型配置相关/new 5.txt`（临时文件）

### 归位（根目录 6 个散落文档 → 大类）
- AI 类 5 份 → `05-方案设计/`（统一接入层/面试配置化/面试拓展优化/简历重构/ES→JVector）
- 岗位画像自检清单 → `04-测试验收/`

### README.md 重写
- 目录结构表与文档清单反映规整后状态（9 个大类目录、29 个文档），标注本次规整说明
- 文档结构：根目录仅保留 README.md，其余全部归入编号目录

## v11.43 (2026-09-09) 财务分析接入 AI 统一网关 + 场景管理页下拉/注册表修复

### 财务分析走统一网关（finance_analysis 示范接入）
- **FinanceAnalysisHandler**：新增「业务透传模式」——input.prompt 携带业务侧已构建的完整提示词时直接对话返回综述文本（data.summary）；原 ledgerData 结构化模式保留。网关的限流/缓存/执行日志/降级策略对业务内部调用同样生效
- **LedgerAiAnalysisServiceImpl.callLlm(userId, prompt)**：优先经 `AiGatewayService.execute`（scene=finance_analysis，userId 用于用户级限流）；网关不可用/场景未启用配置/调用失败时回落 v11.39 直调链路（`callLlmDirect`），业务降级模板行为不变
- **generateSummary 增加 userId 参数**透传至网关

### 场景管理页两处修复（ai/scene/index.vue）
- **场景代码改下拉**：原自由输入框 → el-select，选项来自 `/cms/ai/scene/registry`（AiSceneEnum 注册表，与 Handler Bean 注册一致，新增 Handler 需同步加枚举）；filterable + allow-create 兜底，编辑态锁定不可改
- **注册表总览修复 0 个场景**：根因是 onMounted 从未调用 `sceneRegistry()`，registry 恒为空数组。新增 `loadRegistry()` 并在 onMounted 调用（复用已有接口与 `cms:ai:scene:list` 权限）

### SQL（20260909-ai2-unified-gateway.sql 追加）
- 为 7 个枚举场景补幂等 INSERT 默认启用配置行（finance_analysis / voice_interview / resume_parse / resume_optimize / question_generate + 已有 sensitive_word / daily_topic）——原 UPDATE 仅命中已有行，无行场景网关 `getConfig()` 返回 null 导致 SCENE_NOT_FOUND

### 验证
- 重启 moyun-server → 进 `/ai/ai-config/scene`：注册表总览应显示 7 个场景；新增配置时场景代码为下拉
- 记账分析页（`/pages/analysis/index`）触发分析 → 服务端日志出现 `[ai2:网关] 请求: scene=finance_analysis` 与 `ai_execute_log` 落记录

### 命名统一（同日追加）
- `AiExecuteRequest` / `AiExecuteResponse` 字段 `scene` → `sceneCode`（对外 JSON 字段同步变化：POST /api/ai/execute 的 body 与响应体）
- 同步修改：LedgerAiAnalysisServiceImpl（setSceneCode）、AiGatewayController 注释、AiSceneRegistry.listScenes 返回 key（scene→sceneCode）
- 前端三工程尚无网关调用方，无需改动；后续前端接入统一用 `sceneCode`

## v11.41 (2026-09-09) AI 统一接入层：ai_scene_config 增量加列 + ai_execute_log 日志表

### 背景
原方案新建 `ai2_scene_registry` 独立表，但绑定字段（agent_id/model_id/workflow_id/knowledge_base_ids/tool_ids）与已有 `ai_scene_config` 完全重复，且底座 `AiSceneResolver.resolveChatModel()` 已按 Agent→直绑模型→默认 责任链解析了绑定关系（v11.39 已完成）。改为对 `ai_scene_config` 增量加列，避免数据割裂和重复 CRUD。

### SQL 变更（20260909-ai2-unified-gateway.sql）
- **ai_scene_config 增量加列 20 个**（ALTER TABLE）：scene_category / handler_bean_name / handler_method / system_prompt_template / user_prompt_template / prompt_placeholders / output_mode / output_schema / output_parser / max_tokens / temperature / timeout_seconds / retry_count / rate_limit_key / rate_limit_count / rate_limit_time / fallback_model_id / fallback_response / enable_cache / cache_ttl
- **ai_execute_log 独立新建**（纯日志表，无 update_time/deleted，不继承 AiBaseEntity）
- 前缀统一 ai_，废弃 ai2_ 前缀
- UPDATE 已有 5 个场景补充 handler_bean_name / scene_category
- INSERT 新增 sensitive_word / daily_topic 两个场景（幂等）

### 后端
- **AiSceneConfig 实体**：补 20 个执行层字段（BigDecimal temperature / Integer maxTokens 等）
- **AiSceneEnum**：补 SENSITIVE_WORD / DAILY_TOPIC 两个场景枚举
- **AiExecuteLog 实体 + Mapper**：新建（纯日志，不继承 AiBaseEntity）
- Mapper 无 XML（纯 BaseMapper），Service/Controller 标准 CRUD 无需改动

### 前端（admin-vue）
- **ai/scene/index.vue**：表单弹窗从 720px 扩为 860px，改为 3 个 Tab 分组
  - 基础配置：原字段 + scene_category + 行内排列权重/优先级/默认/启用
  - 执行配置：Handler / Prompt 模板 / 输出模式 / 温度 / Token / 超时 / 重试
  - 限流与降级：限流三件套 / 备用模型 / 兜底回复 / 缓存
- 表格列表补「分类」「Handler」两列
- `makeDefaultForm` / `handleEdit` 同步 20 个新字段的默认值与回填

## v11.40 (2026-09-08) AI 分析快照数据指纹自动失效 + 借款自动建户欠款双倍修复

### AI 分析快照：数据指纹（免手动 refresh）
- 问题：分析页进页面命中当月快照直接返回旧报告，记账后不点「重新分析」永远看不到最新数据（refresh 参数前端只在手动重分析时传）
- 方案：`ledger_ai_analysis_report` 新增 `data_fingerprint`（SQL 增量 ALTER，见 20260908-05 末尾）；`analyze()` 命中快照前比对指纹——本月流水（条数+最后变更时间）/启用资产/负债账户（数量+最后变更时间）/画像文本，任一变化自动落穿重算并覆盖；无变化仍零 token 命中
- `buildFingerprint`/`aggSignature` 私有方法；指纹构建失败返回 null（当次不命中缓存，直接重算，安全侧）；存量快照无指纹 → 视为不一致自动重算一次并回填
- 行为语义：进页面有新账 → 自动出新报告；无新账 → 零 token；手动「重新分析」（refresh=true）仍强制重算
- `/portal/ledger/ai/profile` 本身实时查库无缓存，未改动

### bug：借款未选负债账户时欠款双倍计入
- 场景：记借款 ¥800 不选「借款自」→ 自动新建负债，流水 800、principal 800 正确，但 balance=1600
- 根因：`LedgerTransactionServiceImpl.applyBalanceEffect` BORROW 分支自动建户时 `balance=amount`，随后 `applyLiabilityDelta` 再 `+amount`，双倍
- 修复：自动建户 `balance=0`、`principal=amount`（初始本金），欠款完全由 `applyLiabilityDelta` 累加——向新对象借 800：初始欠款=当前欠款=800，后续借款正常累加；冲正（删除/改流水）回退路径对称无需改
- 存量脏数据：按惯例彻底删除测试数据（该负债及其流水）即可，无需修补脚本

## v11.39 (2026-09-08) LLM 统一入口：scene_code 全链路接入（设计模式落地）

### 设计模式
- Factory：AiSceneResolver.resolveChatModel(sceneCode)——scene_code → ChatLanguageModel 的唯一工厂
- Chain of Responsibility：模型解析责任链 Agent 绑定（带温度/maxTokens）→ 直绑模型 → null（回落默认）
- Strategy：LlmClient.chat(sceneCode, system, user)——业务只声明"用哪个场景"，模型策略由工厂决定
- Registry：AiSceneEnum 场景注册表（v11.38），新增 resume_parse 场景

### 基础设施
- AiSceneResolver/Impl：新增 resolveChatModel 工厂方法（责任链封装在解析器，LlmClient 不参与解析）
- LlmClient 接口：新增 chat(sceneCode, ...) 与 isSceneBound(sceneCode)；AiModuleLlmClient 委托工厂解析，绑定模型失败回落默认模型（行为平滑）；NoopLlmClient 同语义兜底
- 模型调用适配 langchain4j 1.0.0-beta3 API（chat(messages) + ChatResponse）

### 业务接入（12 处直调全部消除）
- voice_interview：VoiceInterviewServiceImpl ×4（追问分析/候选人提问/知识点归纳/报告增强）、ScoringEngine ×1（自我介绍评分）
- resume_parse：ResumeParseService ×1（简历文本解析）
- resume_optimize：ResumeAiAdviceService ×1、ResumeDeepOptimizeService ×2、ResumeDeepOptimizeGenerator ×1、ResumeJobMatchService ×1
- question_generate：PortalJobTemplateServiceImpl ×1（岗位模板智能出题）
- finance_analysis：LedgerAiAnalysisServiceImpl.callLlm 场景优先、默认模型回落

### 行为语义
- 未绑定场景 → 与改造前完全一致（默认模型）
- 绑定模型/Agent → 场景独立模型（Agent 带温度/maxTokens），后台配置即时生效
- 绑定模型调用失败 → 自动回落默认模型，业务不中断

### 后续方向（Agent-First，已确认）
- 架构定位：Agent 嵌入 Workflow（Agent 为壳，流程为工具），非 Workflow 里嵌 Agent（BPMN 式节点自治）；统一入口（scene_code，本版完成）→ Agent 决策层（规划/路由/工具调用）→ 执行层（确定性 Workflow + 单轮 LLM 场景 + 预测服务）
- 落地三步：① 业务能力/workflow 注册为 Agent Tool（LangChain4j @Tool/ToolSpecification，AiSceneEnum 的输入/输出五元组即 Tool 描述雏形）；② 高风险流程（支付/提现/审核链）Agent 只决定"是否触发"，执行仍走确定性 workflow；③ 护栏（步数/token 预算、超时、tool 调用链审计、失败降级到 scene_code 直绑链路）
- 用户行为预测不受影响：数据层服务包成 Tool 暴露给 Agent，预测结果作为路由决策输入

## v11.38 (2026-09-08) AI 场景注册表（AiSceneEnum）

### 背景
- ai_scene_config 是绑定配置表（场景→Agent/模型/工作流，多版本灰度），但缺"场景是什么"的注册元数据；场景代码散落在实体注释、VoiceInterviewServiceImpl 硬编码、SQL 种子三处
- 实际注册 4 个场景：voice_interview（唯一完整接线 resolveScene）、resume_optimize（业务走 llmClient 直调，未接场景解析）、question_generate（嵌在面试流程/工作流节点）、finance_analysis（记账 AI 分析，走 ModelConfigService）

### 后端
- 新增 `AiSceneEnum`：code/name/capability/input/output 五元组，场景代码唯一权威来源；registry() 供管理页总览；isRegistered/of 供校验
- `AiSceneConfigController`：新增 `GET /cms/ai/scene/registry`；create/update 校验 scene_code 必须已注册，scene_name 以注册表为准自动覆盖（避免多版本名称不一致）
- `VoiceInterviewServiceImpl` 的硬编码场景串改为引用枚举

### 前端（admin-vue 场景配置页）
- 顶部新增「场景注册表」总览卡：场景代码/名称/核心能力/输入/输出/已建绑定数（只读，来自枚举）
- 表单：场景代码改为注册表下拉（编辑时锁定），选中后展示该场景能力/输入/输出提示；场景名称自动填充只读

### 新增场景三步（写入枚举 javadoc）
1）AiSceneEnum 加一项；2）业务代码 agentClient.resolveScene(枚举)；3）管理页为该场景创建绑定（未绑定走业务默认逻辑）

## v11.37 (2026-09-08) AI 分析多维度切换 + 收入占比修复

### bug：收入来源占比缩小 10 倍（100% 显示成 10%）
- 根因：incomeSources 的 ratio 计算 `multiply(BigDecimal.TEN)`，乘 10 而非 100；与建议文案（正确公式）自相矛盾
- 修复：`multiply(BigDecimal.valueOf(100))`，各分类占比 = 该维度内分类收入 ÷ 总收入 × 100

### 多维度分析（month / 3m / 6m / year）
- 后端 `analyze(userId, refresh, range)`：自然月对齐窗口——本月=本月1号起；3m/6m/year=含本月往前 N 个自然月
- 快照策略：仅"本月"维度读写月度快照（period 对齐自然月）；3m/6m/year 实时计算不落库
- indicators 新增 range/rangeLabel/rangeStart；LLM 提示词明确"统计范围：近N个月（含数据 M 个月）"，避免"近1个月"歧义
- 前端分析页：指标卡顶部维度切换 Tab（本月/近3月/近6月/近12月），标题与收入结构标注当前维度；「本月报告」标记仅本月维度显示
- 月均收支仍按"含数据的月份数"平均，避免无数据月份稀释

## v11.36 (2026-09-08) AI 财务分析月度快照 + 收入分类修复

### 新表 ledger_ai_analysis_report（sql/20260908-05）
- 每月一条快照（UNIQUE user_id+period）：进入分析页命中当月报告直接返回（零 LLM token）；「重新分析」（refresh=true）强制重新生成并覆盖
- 字段：health_score（财务健康分 0-100，储蓄率/收支平衡/负债率/还款压力四维各 25 分）、metrics/income/risk/advice 四段 JSON、ai_summary、profile_snapshot（当次画像）

### 后端
- `analyze()` 缓存逻辑 + 落库（saveReport 失败仅告警不影响返回）；返回结构去掉 profile（前端独立请求）
- 新增 `GET /portal/ledger/ai/reports` 历史报告分页（period 倒序）
- `listReports`/`reportToResult`/`computeHealthScore`/`parseJson` 私有方法

### bug 修复：收入来源全部显示「其他收入」
- 根因：`loadCategoryNames` 查询条件 `user_id=? OR user_id IS NULL`，但系统预设分类 user_id=0（非 NULL），工资等系统分类名永远查不到
- 修复：对齐 `LedgerCategoryServiceImpl` 口径 `eq(0L).or().eq(userId)`，收入来源正确显示「工资」等分类名

### 前端（ledger-app analysis 页）
- 画像独立请求先返回先渲染（不再 Promise.all 等分析）
- AI 综述卡新增：报告月份、财务健康分（颜色分级）、fromCache「本月报告」标记
- 新增「历史报告」列表（分页 5 条/页，点开弹层看该期综述）

## v11.35.2 (2026-09-08) 定时记账「立即执行」日期语义修正 + 审核中心路由双轨

### 定时记账（bug 修复）
- 问题：「立即执行」沿用 nextExecDate 作为流水交易日期。任务预定 10-01 执行时，手动立即执行生成的流水落在未来（10-01），本月收支统计（[月初,今天]）不包含，但账户余额当天已扣减，出现"列表可见、总支出不统计、月末净资产对不上"
- 修复：`LedgerScheduleServiceImpl.executeOnce` 增加 forcedExecDate 参数——`runNow` 立即执行传今天（钱今天动，账记今天）；`runDueTasks` 定时到期传 null（沿用预定日期，费用归属其所属周期）；`retryLog` 失败重试沿用日志原定日期
- 语义：立即执行 = 提前消费下一期（nextExecDate 正常推进，不会双记账）

### 审核中心（上轮收尾）
- `AuditTaskType` 新增 `bizRoutePath`（原业务管理页）：文章/专栏/话题/面经/面经评论/认证/反馈/举报 8 类各自映射真实管理菜单页
- `AuditTaskVO` 透传 bizRoutePath；详情弹层「查看原业务」优先使用，避免统一 routePath（审核中心）导致跳回自己
- 存量脚本 `20260908-04-audit-task-route-fix.sql`：修正 sys_audit_task 中 /cms/feedback、/cms/report 旧路由

## v11.35.0 (2026-09-08) ledger-app 注册功能 + 意见反馈对接 + 备忘录布局优化

### 后端（moyun-server）
- `PortalLoginController.register` 注册方式双轨：手机号+短信验证码 或 邮箱+邮箱验证码（二选一）；手机模式含手机号占用校验；短信验证码一次性消费（verifyCode 通过即失效）
- `PortalSmsController` 场景白名单新增 `register`，该场景匿名可发码（注册时未登录；服务层 60s 间隔+日限额+防枚举兜底）
- `PortalUser` 新增瞬态字段 `smsCode`（仅注册接口入参，不落库）
- 意见反馈复用门户既有链路（`/portal/feedback/submit` + `/portal/feedback/my-list`，提交自动进统一审核），后端零改动

### 前端（moyun-ledger-app）
- 新增 `pages/mine/register/index.vue`：手机/邮箱双 Tab 注册，验证码 60s 倒计时，图形验证码按全局开关显隐，密码强度校验（大小写+数字），注册成功即自动登录返回「我的」
- 新增 `pages/mine/feedback/index.vue`：提交反馈（四类型）+ 我的反馈历史（处理状态/后台回复展开查看）
- `pages/mine/index.vue`：登录表单下新增「立即注册」入口；反馈宫格接线；备忘录列表改单行布局（标签+标题一行、完成/删除按钮一行）

### 验证
- 后端 `mvn compile` 通过；ledger-app `npm run build:h5` 通过
- mock 短信验证码写入 dev 日志（`[sms] 验证码已发送`），联调时从服务端日志取码

## v11.34.0 (2026-09-08) 备忘录增强：事项/内容/时间/提醒方式/重要程度

### 数据库（20260908-02 末尾增量 ALTER，不改动原 CREATE）

ledger_memo 新增 6 列：title（事项标题，NOT NULL，存量数据按 content 前50字回填）、event_time（事项时间=提醒基准）、remind_enabled、remind_rule（on_time/advance_30m/advance_1h/advance_2h/advance_1d/advance_1d_9am）、importance（low/normal/high/urgent）、reminded（防重标记）+ idx_user_remind 索引

### 后端

- LedgerMemo 实体补 6 字段与规则常量
- LedgerMemoServiceImpl：create/update 接收新字段（开启提醒强制要求 event_time，remindRule 缺省 on_time）；listMemos 排序改为 未完成 → 重要度 urgent>high>normal>low → 事项时间升序；新增 sendDueReminders()——按规则计算提醒时刻（提前一天上午9点=前一天09:00，其余按分钟数），到期经 pay 模块 INotificationService 发站内通知，过期超1天直接标记不再打扰；提醒设置变更时重置 reminded 允许重新提醒
- 新增 LedgerMemoRemindTask：每 5 分钟扫描发送
- PortalLedgerMemoController：create/update 透传新字段（eventTime 字符串转 LocalDateTime）

### 前端（memo/index.vue 重构）

- 列表项：标题 + 重要程度彩色徽标（紧急红/重要橙/一般灰/不重要灰）+ 内容 + 事项时间 + 🔔提醒方式 + 已提醒标记
- 添加/编辑弹层：事项、内容、日期+时间选择、是否提醒（不提醒/提醒）、提醒方式六选一 picker、重要程度四选一；开启提醒未选时间时前端拦截提示
- 悬浮＋按钮替代原底部输入栏；点待办项进入编辑
- dashboard 首页待办展示 title（fallback content）

### 验证

- 后端 Maven compile / H5 build 通过
- **需执行 20260908-02 末尾新增的 ALTER 段**（已建表用户），重启 moyun-server
- 测试：添加"紧急+提前1小时提醒"待办 → 到点后收到站内通知（消息中心可见），列表显示已提醒

***
## v11.33.0 (2026-09-08) 记账 App「我的」四大功能全链路落地

### 功能（全部对接后端，替换原本地 storage 实现）

1. **打赏**：/portal/ledger/tips（POST 模拟支付成功落库 + GET 累计金额），金额单位元
2. **存钱计划**：/portal/ledger/savings——列表汇总（剩余需存/累计存入/目标金额）+ 详情期次流水（待存/成功/失败三态、失败原因、存入可输入实际金额）+ 四种存钱法（52周 10n 元累计 13780 / fixed 固定金额 / monthly 每月固定 / custom 首期+递增+期数，末期自动凑整）；计划状态自动流转（全部期次完成或达标→成功）
3. **备忘录**：/portal/ledger/memos CRUD + 完成态切换；**首页（总览）新增待办事项卡片**（前 3 条未完成，点击进入备忘录）
4. **定时记账**：/portal/ledger/schedules——列表（启用开关/立即执行/执行日志/失败重试/删除）+ 添加页（循环周期每天/每周/每月/每N天、执行时间、起止日期、类型、分类、金额、账户、备注，前端实时预览下次记账时间）；后台 LedgerScheduleExecuteTask 每 10 分钟扫描到期任务，复用 ILedgerTransactionService 生成流水（余额联动+净资产快照），写 ledger_schedule_log，推进 next_exec_date，end_date 到期自动停用；失败留痕不推进，支持手动重试

### 修复

- **pages.json 未注册子页面**（报错 navigateTo:fail page /pages/mine/tip/index is not found 根因）：补注册 savings×3 / schedule×2 / tip / memo 共 7 页
- dashboard 待办卡片原先无数据源（todos 永远为空数组），对接备忘录 API

### 技术要点

- 新增 2 个缺失 Mapper（LedgerScheduleTaskMapper/LedgerScheduleLogMapper）
- 后端 4 组 Service/Impl + 4 个 Controller，全部走 PortalSecurityUtils.getUserId() 数据隔离
- 金额 BigDecimal 元口径（DECIMAL(18,2)），与 20260908-02 DDL 一致
- 期次/日志 list 均带 userId 冗余校验，防越权

### 验证与执行

- 后端 Maven compile / ledger-app H5 build 通过
- **需在 DataGrip 执行 sql/20260908-02-moyun-ledger-personal-tools.sql**（6 张表 IF NOT EXISTS，可重复执行），重启 moyun-server
- 注：浏览器控制台 reportAllChanges startTime 报错非本项目代码（源码与依赖均无该符号），疑似浏览器插件（如 Vue DevTools），无痕窗口验证即可

***
## v11.32.0 (2026-09-08) 后台登录验证码风控 + 参数配置缓存 TTL

### 决策

用户反馈：参数配置页 sys.account.captchaEnabled=true，但登录始终不要验证码。排查两个根因：
1. **配置缓存无 TTL**：SysConfigServiceImpl 四处 setCacheObject 均不带过期时间，Redis 残留旧值 "false" 永不失效，DB 改 true 读不到，两边长期脱节
2. **pwd_err_cnt 是死代码**：SysPasswordService.validate（密码错误计数/锁定，user.password.maxRetryCount=5/lockTime=10）从未接入登录链路——UserDetailsServiceImpl 未调用它，密码错 5 次也不锁定，更谈不上触发验证码

### 实现（验证码 = 全局开关 || 风险触发）

1. **SysConfigServiceImpl**：4 处缓存写入统一补 30 分钟 TTL（CONFIG_CACHE_TTL_MINUTES），直接改库最多 30 分钟内自动生效
2. **SysLoginService**：
   - 接入 SysPasswordService.validate（补接死代码）：密码错误计数 ≥5 锁 10 分钟
   - 新增 isRiskCaptchaRequired：密码错误 ≥2 次（captcha.riskFailThreshold，复用 pwd_err_cnt 滑动窗口）或距上次成功登录 ≥30 天（captcha.riskInactiveDays，含从未登录）→ 即使全局开关关闭也强制验证码；服务端强制，绕过前端无法跳过
3. **CaptchaController**：/captchaImage 支持可选 username 参数，返回 captchaEnabled = 全局开关 || 风险判定，风险账号同样下发图片
4. **application.yaml**：captcha.riskFailThreshold=2 / riskInactiveDays=30
5. **admin-vue**：login.js getCodeImg(username)；login.vue 用户名变化 500ms 防抖重判风险、登录失败后必刷验证码（原代码只在开关开时刷新，风险态漏刷）

### 验证

- 后端 Maven compile / admin-vue build:prod 通过
- 场景回归：①全局开关开→正常验证码；②开关关+正常账号→无验证码；③开关关+连续错密码 2 次→第 3 次登录必须验证码；④错 5 次→锁定 10 分钟；⑤30 天未登录账号→必须验证码
- Redis 清理验证：DEL sys:config:sys.account.captchaEnabled 后 30 分钟 TTL 生效，改库自动回源

***
## v11.31.1 (2026-09-08) 金额口径收尾：迁移脚本双方案 + ledger-app 去 cent 命名

### 实现

1. **SQL**：`20260908-03-moyun-pay-amount-yuan.sql` 重构为方案A（数据为空/已是元口径：纯改类型+注释，不动数据，默认启用）/方案B（存量分口径：÷100 三步定型，注释保留）二选一结构，附口径核对方法
2. **ledger-app 工具收口**：utils/money.js 删除全部 cent 误导命名与恒等转换层——yuanToCent→toNum、centToYuan→toFixedYuan、centToAmount→formatAmount、centToAbsAmount→formatAbsAmount、centToSigned→formatSigned、safeSumCents→safeSum；8 个引用页面（analysis/dashboard/mine-budget/portfolio/record-edit/record-index/record-list/report）与局部变量 cent→amountNum 同步重命名；H5 构建通过，全链路已无 ×100/÷100
3. **portal 修复**：voiceInterview.ts 中 createReportShareToken/getSharedReport 误用未导入的 request 对象，改用 httpPost/httpGet（expireDays 改 query 拼接），修复 Vite 构建 "request is not defined" 错误
4. **文档**：设计方案 V1.3 附录 E.1/E.3 更新为重命名后口径（原"签名兼容零改动"表述废止）

***
## v11.31.0 (2026-09-08) pay 模块金额单位统一为元（全项目金额口径收口）

### 决策

PayChannelRequest/PayOrder/钱包/分账/提现原以 BIGINT「分」存储（V11.0 设计，对齐微信 API + 整数守恒）。评估结论：微信的分只是 API 边界契约（适配层转换即可），支付宝本就是元字符串，「分」并非通用渠道货币；而 portal_tip_order（元）与 pay_order（分）两套单位并存正是 v11.23 100x bug 的根因。mock 模式零真实资金数据，是统一改造的零成本窗口。与记账模块 v11.26（20260904-03）同口径收口，全项目金额自此唯一：人民币元 DECIMAL(18,2)/BigDecimal。

### 实现

1. **SQL**：新增 `20260908-03-moyun-pay-amount-yuan.sql`（pay_order/pay_user_account/pay_ledger_entry/pay_withdraw_order 四表，÷100 三步定型，含守恒验证查询）；20260902 建表脚本头注释同步废止声明
2. **实体**（Long 分 → BigDecimal 元）：PayOrder、LedgerEntry、UserAccount、WithdrawOrder、PayChannelRequest；删除 amountYuan/balanceAfterYuan 等 transient 换算字段
3. **网关/服务**：IPayGateway.createOrder、PayGatewayImpl、ILedgerService.settle（守恒校验改为 BigDecimal `platform.add(user)==amount`）、LedgerServiceImpl（抽成 `amount×feeRate.setScale(2,HALF_UP)`）、IUserAccountService.credit/debit、UserAccountMapper 原子 SQL 参数 BigDecimal 化
4. **边界收口**：WechatPayChannel 新增 `yuanToFen()`（元×100 HALF_UP，微信 v3 唯一换算点，真实 API TODO 注释同步）；PortalTipServiceImpl 删除 `movePointRight(2)` 透传 BigDecimal
5. **控制器**：PortalPayController / CmsPayOrderController / CmsPayLedgerController 删除 fillYuan 与 *Yuan 响应字段，接口所见即所得；分账汇总 sumAmount 改 BigDecimal
6. **前端**：admin-vue 支付订单/分账流水页（amountYuan→amount 等 4 字段）；portal WalletPage（balanceYuan→balance 等 5 字段）、types/api.ts 三个接口类型同步；PayCashierPage 本就收元无需改
7. **规范**：项目开发规范 §2.3.1 补充「边界换算（唯一允许的转换点）」条款与 pay 迁移脚本引用

### 验证

- 执行 `20260908-03-moyun-pay-amount-yuan.sql` 后重启 moyun-server
- 打赏流程回归：下单（pay_order.amount 元）→ mock 支付 → 分账（守恒 SQL 验证）→ 钱包余额/流水（元直读）→ 后台支付订单/分账流水页金额显示
- 管理端分账汇总卡片：平台抽成 + 用户所得 = 总额

***
## v11.30.5 (2026-09-07) 面试报告分享（token 免登录公开访问）

### 决策

报告数据本已完整持久化于 `portal_voice_interview.report` JSON 列（finish 时序列化整个 VO），独立报告表属于重复设计，已否决并清理（中途产出的实体/Mapper/双写代码全部删除）。仅补分享三字段。

### 实现

1. **SQL**：`portal_voice_interview` ALTER 增加 `share_token`（唯一索引）/`share_expire_time`/`share_count` 三列
2. **实体**：PortalVoiceInterview 同步三字段
3. **后端**（IVoiceInterviewService + Impl + Controller）：
   - `POST /portal/interview/voice/{id}/share`：本人已结束面试生成 token（UUID，1-30 天有效期默认 7 天，限流 20/h）
   - `GET /portal/interview/voice/share/{token}`：免登录公开（`/portal/interview/**` 已 permitAll），校验过期，share_count 计数，复用 parseReport 解析 JSON 列返回 VO（不含用户信息，脱敏）
4. **前端**：
   - voiceInterview.ts 新增 `createReportShareToken`/`getSharedReport` API
   - 新建 SharedReportPage.vue（`/interview/share/:token`，免登录路由）：总分+等级、6 维度条形图、自我介绍评分、AI 总结、亮点/薄弱点/改进建议、逐题点评、知识点卡片
   - VoiceInterviewPage 分享按钮从假分享（需登录的 ?id= 链接）改为 token 公开链接，复制到剪贴板

***
## v11.30.4 (2026-09-07) 报告「相关知识点」Tab 从未生成 → 实现题库 tags 聚合 + LLM 简介

### 问题

报告页「📚 相关知识点」Tab 恒显示占位文案「暂无相关知识点（V10.2 LLM 版本启用后自动生成）」。根因：该功能为 V10.2 规划项，前端渲染与类型（KnowledgePointItem）早已就绪，但后端报告 VO 无 knowledgePoints 字段、finish 也无生成逻辑——从未实现，纯前端占位。

### 实现（VoiceInterviewReportVO + VoiceInterviewServiceImpl）

1. **VO**：新增 `List<KnowledgePointView> knowledgePoints`（title/desc，@Data 内部类）
2. **finish 生成**：`buildKnowledgePoints(position, qaList)` —— 本场题库题（有 questionId 且已作答）回查题库聚合 tags，低分题（<60）权重 ×2（薄弱知识点优先），按权重取 top 8
3. **LLM 简介**：`tryLlmKnowledgeDesc` 批量生成一句话简介（40 字内，含岗位上下文），失败/AI 关闭回退规则描述（出现次数 + 复盘提示）
4. **前端空态文案更新**：过时的「V10.2 LLM 版本启用后自动生成」改为准确提示（知识点来自题库标签聚合，纯 AI 动态出题场次不生成）

***
## v11.30.3 (2026-09-07) 面试报告页「对话回放」无数据修复

### 问题

面试结束后报告页切换到「💬 对话回放」Tab 显示空（"暂无对话记录"）。根因：回放数据源是 `interview.value.qaList`，而 `handleFinish` 成功后只更新了 `report.value`（后端 finish 只返回报告），`interview.value` 仍是 start 接口返回的 VO（不含完整问答列表）；只有从历史记录进入（loadHistoryReport → 详情接口）才会填充 qaList。QA 数据实际已正常落表（portal_voice_interview_qa），纯前端展示链路问题。

### 修复（VoiceInterviewPage.vue）

finish 成功展示报告后，静默重拉 `getVoiceInterviewDetail(id)` 刷新 `interview.value`（含完整 qaList），对话回放 Tab 立即有数据；详情刷新失败不影响报告展示。

***
## v11.30.2 (2026-09-07) 实时分析维度分固定值修复（连续化重构）

### 问题

实时分析气泡的维度分固定不变（流畅度恒 100、专业度恒 40、互动性恒 40、逻辑恒 35）。根因：`scoreAnswer` 规则评分用二值阈值公式（professionalism = matched≥2?70:40 等），且 v11.x LLM 动态题（追问/系统设计/自我介绍）无 tags/solution → 关键词为空 → matched 恒 0 → 输出恒定组合；LLM 分析仅输出 3 维，其余 3 维回退规则固定值。

### 修复（VoiceInterviewServiceImpl + 前端）

1. **规则 6 维连续化**：relevance=覆盖率；professionalism=30+覆盖率×50+长度稳健；fluency=长度分段线性（<40 偏短 / 40-300 递增 / >300 饱和微降）；interactivity=30+覆盖率×35+互动信号×10+长度参与度；confidence=35+长度饱满+命中+覆盖率；logic=30+结构词计数×12+覆盖率×22。新增 `countStructureWords`（16 个逻辑连接词）与 `countInteractiveSignals`（举例/对比/坦诚等信号）
2. **动态题关键词兜底**：keywords 为空时从题干 `title` 提取，LLM 生成题的 matched 不再恒 0
3. **LLM 分析升级 6 维**：prompt 输出全 6 维（含维度定义），`parseAnalysis` 逐维解析并对 LLM/规则分按 llmRatio（默认 70/30）融合，未输出维度回退规则分
4. **前端气泡刷新**：LLM 融合维度到达后重算亮点/缺口覆盖规则版初值

***
## v11.30.1 (2026-09-07) 管理端语音面试 Controller 补建 + 全链路接口核对

### 问题与修复

1. **管理端语音面试 404**：admin 页面（views/cms/voiceInterview）调用 `/cms/voice-interview/list` 报 NoResourceFoundException——前端与菜单 SQL（20260901，5253-5256 cms:voiceInterview:*）早已就绪，后端 Controller 一直缺失。补建 `CmsVoiceInterviewController`（/cms/voice-interview）：list（username/position/status 筛选 + 批量填充 username）、/{id} 详情（不校验归属）、DELETE 删除；服务层新增 adminList/adminGetDetail/adminDelete 三方法；VoiceInterviewVO 补 username 字段
2. **种子 JSON 键名不一致**：portal_interview_config 种子 scoring_weights 写的是 selfAwareness/jobMatch/fusion，而 ScoringEngine 解析 awareness/matching/llmRatio——后台按错误键名调权重会静默失效回退默认值。已修正为 `{"selfIntro":{"structure":30,"awareness":25,"matching":25,"fluency":20},"llmRatio":70,"total":{"intro":20,"tech":80}}`
3. **语音演示页接口缺失**：VoiceEngineDemoPage（/interview/voice-demo）经 useInterviewHint 调用 GET /hint、GET /keywords，后端无此端点。补建两个轻量 GET 端点（按 questionId 直调 HintEngine，限流 60/h）

### 全链路核对结论（admin-vue / portal 前端 ↔ 后端）

- 场景配置 /cms/ai/scene/*、岗位模板 /cms/interview/jobTemplate/*、面试配置 /cms/interview/config/*：前端调用与后端路由**全部一一匹配**
- 权限标识核对：hasPermi 与菜单 perms 全覆盖（scene 5450-5454、jobTemplate 5455-5459、config 5460-5464、voiceInterview 5253-5256），admin 角色已授权（role_menu）
- 题库 jobTemplateId 筛选：InterviewQuestionQuery.jobTemplateId 字段与 selectQuestionPage 过滤条件均已就绪
- portal 端 job-templates / self-intro / agents / start / answer / hint(POST) / next / finish / my/list / detail：全部匹配

***
## v11.30 (2026-09-07) AI 面试全链路动态配置化 + AI 场景配置中心

### 一、需求背景

依据《AI面试流程与相关模块拓展优化方案-20260907.md》与《AI 面试全链路动态配置化 实施计划（完整版·合并）》，将语音面试从"硬编码流程"升级为"配置驱动"：面试 Agent/模型/知识库/工作流动态绑定、岗位模板智能出题、6 阶段流程状态机、评分权重化、报告增强、错题自动入库、JD 关键词 LLM 提取。

### 二、数据库（20260907-moyun-ai-interview-full.sql）

- 新表：`ai_scene_config`（场景配置中心：Agent/模型/知识库/工具/工作流绑定 + 版本灰度权重）、`portal_job_template`（岗位模板：JD/关键词/出题权重）、`portal_interview_config`（面试配置：人设/提示词/评分权重/追问策略/自我介绍）
- ALTER：`portal_voice_interview` 加 `agent_id/phase/intro_score_json`；`portal_voice_interview_qa` 加 `question_source/parent_qa_id`；`portal_interview_question` 加 `job_template_id`；`portal_user_resume` 加 `parse_confidence`
- 菜单：5450-5454 场景配置（AI基础配置下）、5455-5459 岗位模板、5460-5464 面试配置（面试管理下），admin 角色已授权
- 种子：voice_interview 场景绑定现有默认 agent（上线零变化）

### 三、后端（moyun-server）

**A. 场景配置中心（com.moyun.ext.ai）**
- `AiSceneConfig` 实体/Mapper/Service/Resolver：`AiSceneResolver.resolve(sceneCode)` → `AiSceneBinding`（Agent > 直绑模型伪 Agent > empty 降级）
- `AiSceneConfigController`（/cms/ai/scene）：CRUD + `/{id}/test` 场景解析测试

**B. 面试链路接线**
- `InterviewAgentClientImpl.resolveAgentForScene(binding, agentId)`：显式入参 > 场景 Agent > 场景模型伪 Agent > sys_config 默认
- `VoiceInterviewServiceImpl.start()`：场景解析 → 面试配置（enableSelfIntro 决定初始 phase）→ QuestionPicker 四路题源出题（job/resume/weak/random 权重配额 + 最大余数法 + 额度流转）
- `InterviewPhase` 6 阶段状态机：INTRO_WAITING → INTRO_RECEIVED → INTRO_FOLLOWUP → TECH_QUESTION → PROJECT_DEEP → SYSTEM_DESIGN → CANDIDATE_ASK → FINISHED；旧会话 phase=NULL 走原逻辑
- `POST /portal/interview/voice/{id}/self-intro`：提交自我介绍（ScoringEngine 4 维度评分 → 追问或进入首题）
- `GET /portal/interview/voice/job-templates`：启用中岗位模板（portal 开始面试选择）

**C. 评分引擎与报告**
- `ScoringEngine`：自我介绍 LLM 结构化评分（structure/awareness/matching/fluency 加权，LLM 失败回退规则）；`fuseAnswerScore`（每题 LLM+规则融合，llmRatio 默认 70%）；`fuseTotalScore`（总分 intro 20% + tech 80%）；权重全部来自 portal_interview_config.scoring_weights JSON
- 报告增强：`VoiceInterviewReportVO` 加 `introScore`（自我介绍独立评分视图）+ `improvementSuggestions`（薄弱点/自我介绍不足针对性建议，最多 5 条）
- 错题自动入库：finish 时 <60 分题库主问题调 `IWrongQuestionService.recordWrongQuestion`（幂等累加 wrong_count）

**D. 工作流异步接线**
- finish() 后 `triggerSceneWorkflowAsync`：场景绑定 workflowId 时经 aiTaskExecutor 异步执行 `WorkflowService.execute`（输入含 interviewId/得分/强弱项/建议），失败仅日志

**E. JD 关键词 LLM 提取**
- `CmsJobTemplateController` `/extract-keywords`：LLM 提取 JD 关键词，失败回退规则分词

### 四、前端

**moyun-admin-vue（后台）**
- `views/ai/scene/index.vue` + `api/ai/scene.js`：场景配置中心页面（绑定 Agent/模型/工作流、版本灰度权重、测试按钮）
- `views/cms/interview/jobTemplate/index.vue` + `api/cms/interviewJobTemplate.js`：岗位模板管理（JD 编辑 + LLM 提取关键词 + 关联题目 + 出题权重）
- `views/cms/interview/interviewConfig/index.vue` + `api/cms/interviewConfig.js`：面试配置管理（人设/提示词模板/评分权重/追问策略/自我介绍环节/默认配置）
- `views/cms/interview/question/index.vue`：题库增强（岗位模板筛选列 + 表单归属选择）

**moyun-portal（前台）**
- `api/voiceInterview.ts`：VoiceStartConfig 加 jobTemplateId/questionWeights；报告 VO 加 introScore/improvementSuggestions；新增 getVoiceJobTemplates
- `pages/interview/VoiceInterviewPage.vue`：开始面试岗位模板选择（job 题源提示）；报告页自我介绍独立评分卡（4 维度条形图 + 总评）；改进建议优先展示 improvementSuggestions

### 五、兼容性

1. enable_self_intro 默认 0：6 阶段状态机默认不激活，旧流程完全保留
2. 场景未配置/异常 → AiSceneBinding.empty() → 走原有 sys_config 默认链
3. LLM 全链路失败回退：评分→规则、JD 提取→分词、出题→随机兜底
4. 报告/VO 字段只加不改，旧数据宽容解析（@JsonIgnoreProperties）

***
## v11.23 (2026-09-04) 记账模块：资产负债列表一键查关联流水 + 金额口径再确认

### 一、需求背景

用户两个优化请求：
1. 资产负债页（/pages/portfolio/index）账户列表 / 负债列表「点击查流水」不方便；旧版点击 = 直接打开编辑弹层，想追溯某笔余额变动必须先退到 TabBar 流水页再手动按日期/类型翻，入口过深。
2. 接口返回 `"balance":"22200","principal":"22200"` 但页面显示 222.00，用户怀疑 100 倍计算错误。

### 二、功能 1：资产/负债行 → 一键跳「关联流水」

#### 交互变化（pages/portfolio/index）

| 入口 | 旧行为 | 新行为 |
| --- | --- | --- |
| 行空白区点击 | 打开「编辑账户」弹层 | **跳 /pages/record/list 自动带 accountId / liabilityId 过滤** |
| ✎ 铅笔图标（行尾，新增） | 无 | **打开编辑弹层**（原编辑入口保留，@tap.stop 阻止冒泡） |

路由传参约定：`/pages/record/list?accountId=X&name=Y` 或 `/pages/record/list?liabilityId=X&name=Y`

#### 流水列表页联动（pages/record/list）

- **NavBar 动态标题**：带过滤时显示 `「招商银行储蓄卡 · 流水」`；无过滤时回到 `流水明细`
- **顶部筛选横幅**（蓝色渐变）：
  - 资产时提示：`仅显示「xxx」的关联流水 · 资产账户 · 作为转出/转入方都会匹配`
  - 负债时提示：`仅显示「xxx」的关联流水 · 负债账户 · 借款/还款/校准联动记录`
  - 右侧「×」一键清筛选，回到全量流水
- **onLoad 解析参数**：accountId / liabilityId 二选一（互斥，后者覆盖前者）；name 经 decodeURIComponent 解码写入 filterName
- **load() API**：query 传入 accountId / liabilityId → 后端 TransactionQuery 已有原生筛选支持，不用新增字段

#### 后端复用说明（无后端代码新增）

- `LedgerTransactionServiceImpl.pageTransactions` L216-220 早已有：accountId → `WHERE account_id = ? OR target_account_id = ?`，转账类双边流水都会出现在双方账户明细中 ✓
- L221-222：liabilityId → `WHERE liability_id = ?`，借款/还款/校准涉及负债的流水 100% 命中 ✓
- 合计 total 随过滤条件同步，前端显示"共 N 笔 · 收/支汇总"与横幅匹配

#### 变更文件

- `moyun-ledger-app/src/pages/portfolio/index.vue`：行 @tap→viewTxn；加 ✎ pencil-btn / row-actions 样式；新增 viewTxn 方法（encodeURIComponent 传 name 防中文/特殊字符）
- `moyun-ledger-app/src/pages/record/list.vue`：NavBar 动态 title；新增 filter-banner（样式+关闭）；onLoad 参数解析；API 带过滤字段；clearAccountFilter 方法

### 三、问题 2：balance=22200 显示 222.00 是**正确**的

全链路复算确认：

| 环节 | 值 | 说明 |
| --- | --- | --- |
| DB ledger_liability_account.balance | 22200 | BIGINT 分单位 |
| Jackson 序列化 + 防精度 Long→String | "22200" | JSON 字符串 |
| 前端 toNum("22200") | 22200 | 安全整数 |
| centToAmount(22200) = 22200/100 → 千分位 | **"222.00"** | ✅ 与页面显示完全一致 |

**结论**：22200 分 = 222.00 元，显示链路**无错**。

#### 如果你的业务含义其实是「22200 元」（2.22 万元）

那 DB 中应存 `22200 × 100 = 2,220,000 分`，当前值 22200 少了 100 倍，说明录入时**未走 yuanToCent（前端漏转）/或直接 SQL 以"元"值插入**。处理办法：

1. **App 端推荐**：删除该负债（归档）→ 重新在「资产负债 → 负债 → +」录入 22200 元，前端自动 `yuanToCent(22200) = 2220000` 入库
2. **SQL 批量修正（仅限确认是"旧数据漏转"的情形）**：
   ```sql
   -- 先 SELECT 核对（预计 < 1000000 分 且实际金额明显 > 1万 的负债，手动核对）
   SELECT id, name, balance, principal, balance/100 AS yuan_now
   FROM ledger_liability_account
   WHERE user_id = <当前用户ID> AND balance < 1000000 AND balance > 0;
   -- 确认后 UPDATE（×100）
   UPDATE ledger_liability_account
   SET balance = balance * 100, principal = principal * 100
   WHERE id IN (<需要修正的ID列表>);
   ```

3. **纵深防御**：v11.22 已加后端 Controller 入库上限校验（0 ≤ cent ≤ 100 亿元 = 1e12 分）+ 空/负值拦截，未来漏传 100 倍的错误数据能被及早拦截。

---
## v11.24 (2026-09-04) 记账模块：分类体系全类型覆盖 + 语义分组

### 一、需求背景

分类管理页（/pages/mine/categories/index）仅支出/收入两个 Tab，但记账有 6 种交易类型（支出/收入/转账/还款/借款/校准）。记一笔时选转账/还款/借款/校准后，分类区展示全部收支分类（无针对性），用户请求覆盖全 6 类并按语义分组。

### 二、SQL 变更（20260904-02-moyun-ledger-category-expand.sql）

1. `ALTER TABLE ledger_category ADD COLUMN group_name VARCHAR(50)` — 语义分组列
2. `UPDATE` 现有 26 条系统预设分类补 group_name（如 餐饮→生活刚需、房贷/房租→负债还款、工资→劳动收入）
3. `INSERT` 20 条新系统预设分类：

| 类型 | 分组 | 分类项 |
| --- | --- | --- |
| transfer（转账） | 账户间 / 亲友间 / 其他 | 账户间互转、转给亲友、代付代收、退款退回、其他转账 |
| repayment（还款） | 信用卡 / 贷款 / 私人 / 利息 / 其他 | 信用卡还款、贷款还款、私人借款还、利息支出、其他还款 |
| borrow（借款） | 信用卡 / 网贷 / 贷款 / 分期 / 私人 / 其他 | 信用卡消费、网贷借款、银行贷款、消费分期、私人借款、其他借款 |
| adjust（校准） | 余额修正 / 其他调整 | 余额修正、手续费调整、汇率差异、其他调整 |

### 三、后端变更

- `LedgerCategory.java` 新增 `groupName` 字段（String，映射 group_name 列）
- `LedgerCategoryServiceImpl.listAvailable()` 无需改动（已支持任意 type 过滤，type=null 时返回全部）

### 四、前端变更

1. **pages/mine/categories/index.vue**（重写）
   - Tab 从 2 个 → **6 个**（支出/收入/转账/还款/借款/校准），横滑支持
   - 列表按 `groupName` **语义分组**展示：每组一个标题 + 卡片，组内按 sortOrder 排序
   - 添加自定义分类时自动带上当前 Tab 的 type
2. **pages/record/index.vue**
   - `filteredCategories`：旧逻辑仅 expense/income 按 type 过滤、其余返回全部 → **改为所有 6 类都按 `c.type === t` 精确过滤**
   - `ICON_MAP` 补全 20 个新 icon 的 emoji 映射（转账🔄/还款💳/借款🌐/校准⚖️ 等）

### 五、变更文件列表

- SQL：`moyun-server/src/main/resources/sql/20260904-02-moyun-ledger-category-expand.sql`
- 后端：`LedgerCategory.java`
- 前端：`pages/mine/categories/index.vue`、`pages/record/index.vue`
- 文档：本 devlog + 设计方案附录 D

---
## v11.25 (2026-09-04) 记账模块：修复凭证上传（api/ledger.js 缺 import 致 ReferenceError）

### 问题

记一笔页点击「📷 截图」上传凭证图片，无任何反应 / 报 `ReferenceError: BASE_URL is not defined`。

### 根因

`api/ledger.js` 的 `uploadVoucher()` 和 `exportTransactionsCsv()` 函数体中使用了 `BASE_URL` 和 `useUserStore()`，但文件顶部**从未 import**这两个符号，调用时直接 ReferenceError 崩溃。

### 修复

1. `utils/request.js`：`const BASE_URL` → `export const BASE_URL`（已定义只需加 export）
2. `api/ledger.js` 顶部补 `import { ..., BASE_URL } from '@/utils/request'` + `import { useUserStore } from '@/stores/user'`

### 链路验证（修复后完整闭环）

| 环节 | 代码 | 状态 |
| --- | --- | --- |
| 选图 | `uni.chooseImage({ count:1, sizeType:['compressed'] })` | ✓ |
| 上传 | `uni.uploadFile({ url: BASE_URL+'/portal/file/upload', name:'file', header:{Authorization} })` | ✓ |
| 后端 | `PortalFileController.upload()` → `SysFileServiceImpl.uploadFileForPortal()` → 返回 `SysFile.fileUrl` | ✓ 已存在 |
| URL 格式 | `http://localhost:8080/profile/2026/09/04/xxx.jpg` | ✓ |
| 静态访问 | `ResourcesConfig` 映射 `/profile/**` → 本地目录；`SecurityConfig` L163 `permitAll()` GET `/profile/**` | ✓ |
| 前端回显 | `this.form.voucherUrl = url` → `<image :src="form.voucherUrl" mode="aspectFill">` | ✓ |
| 编辑页回显 | `record/edit.vue` 同链路，`this.txn.voucherUrl = url` → `<image :src="txn.voucherUrl">` | ✓ |

### 变更文件

- `moyun-ledger-app/src/utils/request.js`（1 行：加 export）
- `moyun-ledger-app/src/api/ledger.js`（2 行：补 import）

---
## v11.26 (2026-09-04) 记账模块：金额单位统一为元 + 借款自动建负债

### 一、需求

1. 统一金额单位为「元/人民币」——废除 BIGINT 存分的设计，全链路改用 DECIMAL(18,2) 存元 + Java BigDecimal 运算
2. 借款 type=borrow 时不强制选负债账户，不选则自动生成新负债记录

### 二、SQL 变更（20260904-03-moyun-ledger-amount-yuan.sql）

5 张表、11 个金额列 BIGINT → DECIMAL(18,2)，带数据迁移（÷100）：

| 表 | 列 | 旧（分） | 新（元） |
| --- | --- | --- | --- |
| ledger_asset_account | balance, valuation | BIGINT | DECIMAL(18,2) |
| ledger_liability_account | balance, principal, monthly_payment | BIGINT | DECIMAL(18,2) |
| ledger_transaction | amount, balance_after, target_balance_after, liability_balance_after | BIGINT | DECIMAL(18,2) |
| ledger_budget | amount | BIGINT | DECIMAL(18,2) |
| ledger_net_worth_snapshot | total_asset, total_liability, net_worth | BIGINT | DECIMAL(18,2) |

### 三、后端变更

#### 实体层（5 个实体 + 1 个 DTO）
- `LedgerTransaction`: amount/balanceAfter/targetBalanceAfter/liabilityBalanceAfter: Long → BigDecimal
- `LedgerAssetAccount`: balance/valuation: Long → BigDecimal
- `LedgerLiabilityAccount`: balance/principal/monthlyPayment: Long → BigDecimal
- `LedgerBudget`: amount: Long → BigDecimal
- `LedgerNetWorthSnapshot`: totalAsset/totalLiability/netWorth: Long → BigDecimal
- `TransactionCreateDTO`: amount: Long → BigDecimal

#### Service 层
- `LedgerTransactionServiceImpl`: 全部 long 算术改 BigDecimal（add/subtract/compareTo/negate）
  - `validate()`: `amount <= 0` → `compareTo(BigDecimal.ZERO) <= 0`
  - `applyBalanceEffect()`: TYPE_BORROW 时若 liabilityId=null → 自动创建 LedgerLiabilityAccount（type=OTHER, balance=principal=amount, settleFlag=0）并 insert，再 setLiabilityId
  - `applyAssetDelta/applyLiabilityDelta`: long delta → BigDecimal delta, 乐观锁 setSql 不变
  - `checkBudgetAlert()`: mapToLong → map+reduce(BigDecimal::add), floorDiv → multiply+divide
  - `refreshNetWorthSnapshot()`: long 累加 → BigDecimal.add
  - 删除 centToYuanText() 和 toYuan()（金额已是元，直接 toPlainString）
  - `reverseBalanceEffect()`: TYPE_BORROW 加 null 检查（防御历史数据无 liabilityId）
- `LedgerAssetAccountServiceImpl`: createAccount 签名 Long→BigDecimal, refreshSnapshot BigDecimal 算术
- `LedgerLiabilityAccountServiceImpl`: createAccount 签名 Long→BigDecimal
- 接口 `ILedgerAssetAccountService` / `ILedgerLiabilityAccountService`: 签名同步

#### Controller 层
- `PortalLedgerAssetController`: initialBalance 解析改为 new BigDecimal(str), 边界 100亿元
- `PortalLedgerLiabilityController`: initialBalance + monthlyPayment 解析改 BigDecimal, 边界同步

### 四、前端变更

- `utils/money.js` 重写：
  - `yuanToCent()` → 恒等映射（不再 ×100）
  - `centToAmount()` → 直接千分位格式化（不再 ÷100）
  - `centToYuan()` → 直接 toFixed(2)（不再 ÷100）
  - `centToAbsAmount()` → Math.abs 后千分位
  - `safeSumCents()` → 元直接求和
  - **函数签名全部不变**，10+ 个引用文件零改动

### 五、借款自动建负债（需求 2）

- `validate()` 中移除 TYPE_BORROW 的 liabilityId 必填校验
- `applyBalanceEffect()` TYPE_BORROW 分支：liabilityId=null 时自动创建负债账户
- 前端 record/index.vue 的 borrow 类型不再强制弹出负债选择器

### 六、变更文件列表

- SQL: `20260904-03-moyun-ledger-amount-yuan.sql`
- 后端: 6 实体/DTO + 3 Service + 2 接口 + 2 Controller = 13 个 Java 文件
- 前端: `utils/money.js`（1 文件，签名兼容，其余文件零改动）
- 文档: 本 devlog + 设计方案附录 E

---
## v11.22 (2026-09-04) 记账模块：金额安全计算全链路修复 + 负号/浮点/单位边界三重修复

> 触发：用户反馈「总资产出现负数仍显示正数 + ledger_liability_account 存 2200 前端显示 22 元」两个金额 BUG，排查后暴露 centToAmount(Math.abs) 丢符号、yuanToCent(浮点乘 100) 精度、表单回显千分位回写 Number 变 NaN 三处隐患，统一全链路修复。

### 一、前端金额工具链修复（money.js）

| 函数 | 修复前 | 修复后 |
| --- | --- | --- |
| 	oNum | isNaN 判定，空串未显式处理 | 先判 null/'' + Number.isFinite，严格收敛 |
| yuanToCent | Math.round(Number(yuan) * 100) — 1.005 元 = 100 分 ✗；含千分位 Number('1,234.56')=NaN ✗ | **字符串安全解析**：剥 ,/符号/按 . 拆整数与小数段；小数 ≥3 位时第三位四舍五入；9e14 上限保护。1.005→101 ✓、'1,234.56'→123456 ✓ |
| centToAmount | (Math.abs(c)/100).toLocaleString(...) — **永远去负号**，净资产/负债合计负数一律显示正数 | **保留负号**：(c / 100).toLocaleString(...)，负数自动本地化输出 -1,234.56 |
| centToAbsAmount | 无 | **新增**：旧 centToAmount 行为（绝对值 + 无符号千分位），专供 +收入/-支出、欠 ¥xxx 调用方自行拼接语义前缀的场景 |
| centToSigned | 内部手写 Math.abs + prefix | 复用 centToAbsAmount，行为不变 |
| safeSumCents(items, picker) | 无 | **新增**：分单位整数安全求和；picker 支持函数或属性名；每项经 	oNum 防止后端 Long→字符串序列化导致字符串拼接 |

### 二、前端页面同步修正

1. **pages/portfolio/index.vue**
   - ssetTotalText / liabTotalText 合计改用 safeSumCents(list, 'balance')（原 reduce + toNum 语义一致，但工具化避免后续手写拼接）
   - editLiability 月供回显：centToAmount(l.monthlyPayment) → **centToYuan(l.monthlyPayment)**
     - 原 BUG：centToAmount 输出 1,234.56（含千分位）写入表单 <input>；再次保存走 yuanToCent(Number('1,234.56')) → NaN → 0，**月供被清零**
2. **pages/report/index.vue**
   - 年收入 '+' + centToAmount(yearIncome) → '+' + centToAbsAmount(yearIncome)
   - 年支出 '-' + centToAmount(yearExpense) → '-' + centToAbsAmount(yearExpense)
   - （防收入/支出异常为负时出现 +-xxx / --xxx）
3. **pages/record/index.vue**
   - 负债名提示 欠  → 欠 
   - 账户选择器子标题 欠款 ¥ + centToAmount(l.balance) → 同上
   - （「欠」本身已表方向，不必重复显示负号；溢缴负余额显示仍一致可读）

### 三、后端入库边界校验（Controller 防线）

- PortalLedgerAssetController.create：initialBalance 非空时校验   ≤ cent ≤ 100 亿元（1e12 分），超范围返回"请确认金额单位"
- PortalLedgerLiabilityController.create：同上校验 initialBalance + monthlyPayment（0 ≤ 月供 ≤ 1 亿元）
- 作用：**防止前端漏调 yuanToCent（误传"元"值入库，放大 100 倍）** 及负值入库

### 四、影响范围说明

- 所有使用 centToAmount 的 9 个页面（dashboard、analysis、report、record/list、record/edit、record/index、mine/budget、portfolio）的金额显示统一升级：
  - 资产 / 负债 / 净资产 为负数时，**首次正确输出 -1,234.56**
  - 收入、支出、分类排名、月供 等天然正数的展示，前后结果完全一致（无符号差异）
- 后端校验仅对「新增资产 / 新增负债」接口生效，不影响历史数据与流水联动写余额路径（流水联动走 Service，余额可正可负由业务场景决定）

### 五、变更文件列表

- 前端：moyun-ledger-app/src/utils/money.js、pages/portfolio/index.vue、pages/report/index.vue、pages/record/index.vue
- 后端：moyun-server/.../PortalLedgerAssetController.java、.../PortalLedgerLiabilityController.java
- 文档：本 devlog + 记账模块设计方案 V1.3「附录 B：金额安全规范」增补

---
## v11.21 (2026-09-04) 记账模块：AI 财务分析 + 资产负债合并 Tab 页 + 交互细节优化

> 用户需求 3 项：① 报表/总览加 AI 分析入口（分析资产结构、收入来源、债务风险、给出综述与建议）；② 资产/负债两页合并为「资产负债」Tab 切换页，腾出 Tab 位给「分析」页；③ 交互细节（tips 可关闭、弹层可取消、分类宫格可收起）。涉及前后端 + SQL + 后台字典。

### 改动内容

**后端（com.moyun.ledger）**
- 新增 [ILedgerAiAnalysisService](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ledger/service/ILedgerAiAnalysisService.java) / [LedgerAiAnalysisServiceImpl](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ledger/service/impl/LedgerAiAnalysisServiceImpl.java)：规则引擎计算财务指标（资产负债率/月均收支/还款压力/储蓄率/连续入不敷出月数）+ 收入来源结构（按分类占比）+ 债务风险评估（车贷/信用卡还款提示、还款日临近、还款能力）+ 分级建议（补记收入/兼职拓展/控制负债/理财建议）；LLM 生成个性化综述（未启用 AI 或调用失败降级模板文案）
- 新增 [PortalLedgerAiController](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ledger/controller/PortalLedgerAiController.java)：`GET/POST /portal/ledger/ai/profile`（画像读取/维护，与门户共用 portal_user 表）、`GET /portal/ledger/ai/analysis`（分析报告）
- 用户画像维度：portal_user 增量字段 `identity_tag`（身份标签），字典 `ledger_identity_tag`（学生/上班族/自由职业/个体经营者/退休/其他）
- **Bug 修复**：[PortalUserMapper.xml](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/mapper/portal/PortalUserMapper.xml) resultMap 与两处查询列清单缺 `identity_tag`，导致画像保存后读取恒为 null——已补齐 resultMap/查询/insert/update 四处

**前端（moyun-ledger-app）**
- 新增分析页 [pages/analysis/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/pages/analysis/index.vue)：用户画像卡（身份标签/职位/公司，可编辑）+ 财务健康指标 + AI 财务综述 + 收入来源结构 + 债务风险提示 + 分级建议；未登录显示登录引导卡
- 新增资产负债合并页 [pages/portfolio/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/pages/portfolio/index.vue)：顶部「资产/负债」分段 Tab 切换，汇总卡 + 账户列表 + 新增/编辑弹层（含取消/删除归档），替代原 asset/liability 两个 Tab 页
- [pages.json](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/pages.json)：TabBar 由 总览/资产/记一笔/负债/我的 → 总览/资产负债/记一笔/分析/我的
- 记一笔页：分类宫格右上角「收起/展开」折叠（uni-view 可收起）；选择账户/负债弹层底部补「取消」按钮
- [api/ledger.js](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/api/ledger.js) 新增 getAiAnalysis / getAiProfile / updateAiProfile

**SQL**（[20260904-01-moyun-ledger-ai-analysis.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/20260904-01-moyun-ledger-ai-analysis.sql)）
- `ALTER TABLE portal_user ADD identity_tag`（增量追加，不动原建表）
- 字典类型 `ledger_identity_tag` + 6 项字典数据（后台「系统管理→字典管理」可直接维护，无需新增页面）

### 验证（后端 8080 重启后 API 直连 + 浏览器 5174）
- 画像更新：identityTag=office_worker → 返回「上班族/测试员/墨韵科技」✅（mapper 修复前恒 null，修复后正常）
- 记收入 ¥100 后重新分析：avgMonthlyIncome 0→¥100.00、收入来源 0→1 项（工资）✅
- 未登录态分析页：显示登录引导卡、刷新后 console 无新增报错 ✅
- 债务风险：资产为负时正确输出「资产负债率过高」高风险提示 + 「补记收入来源」建议 ✅

### 部署
- 执行 SQL：20260904-01-moyun-ledger-ai-analysis.sql；重启 moyun-server；前端热更新
- 测试账号 zhangsan 密码已重置为 `Test@12345`（原密码遗失，DB 直改 bcrypt）

***

## v11.20 (2026-09-04) 记账模块：资产页UI升级 + 移除原生导航栏改自定义标题

> 用户反馈 2 项：资产页同参考图风格升级；移动端去掉顶部原生"墨韵记账"导航栏（或跟随主题）。纯前端，无 SQL、无后端改动。

### 改动内容
- **全局移除原生导航栏**：[pages.json](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/pages.json) globalStyle 增加 `navigationStyle: custom`——所有页面不再显示"墨韵记账"原生标题条（彻底避免静态导航栏背景色与主题不同步问题，页面标题改由页面内自绘并天然跟随主题）
- **自定义 NavBar 组件**：[NavBar.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/components/NavBar/NavBar.vue)（easycom 自动注册）——白底吸顶、主题色圆形返回键 + 居中标题 + 右侧 slot；无上级页面时兜底跳总览。已插入 6 个非 Tab 页（分类管理/预算设置/设置/流水明细/编辑流水/报表中心）
- **Tab 页顶部安全区**：资产/负债汇总卡新增页内大标题（"我的资产"/"我的负债"）+ `env(safe-area-inset-top)` 顶部间距；总览 hero/brand-hero、我的页 user-card 同步补 safe-area，替代被移除的原生导航占位
- **资产页 UI 升级**：汇总卡绿色渐变→主题色背景；账户图标由绿色文字圆改为**彩色圆底 emoji**（现金💰黄/储蓄卡🏦蓝/电子钱包📱绿/储值卡🎫橙/投资📈绿/固定资产🏠/债权🤝粉/其他🔖灰，88rpx 圆形）；FAB 按钮统一 var(--primary)

### 验证
- `npm run build:h5` 编译通过；浏览器（5174，zhangsan 登录态）逐项验证：
  - 4 个 Tab 页均无原生标题条，内容从状态区开始，TabBar 正常
  - 资产页：标题+薄荷绿汇总卡（rgb(127,191,148)）、彩色圆底 emoji 图标（🏦rgb(74,158,255)/💰rgb(245,197,24)）、总资产 ¥32,320.71 数学求和正确
  - 负债页：标题+主题色卡，总负债 ¥4,322.00 = 222+4100 正确
  - NavBar：设置/报表/流水明细 3 页白底吸顶返回键正常，返回导航成功
  - 主题切换：海洋蓝下资产汇总卡 rgb(91,155,213)，切回恢复 rgb(127,191,148)
  - 记一笔页回归：布局正常无遮挡，console 无业务报错

### 部署
前端热更新自动生效。无 SQL、无后端变更。

**v11.20 补充修复（同日）**：`setTabBarStyle:fail not TabBar page` 报错——App onLaunch 时若当前为非 TabBar 页（如 URL 直达子页）调用 uni.setTabBarStyle 会失败。修复：[theme.js](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/stores/theme.js) setTabBarStyle 增加 fail 静默；5 个 Tab 页 onShow 调用 restore()，保证切回 Tab 页时 TabBar 选中色同步（浏览器验证：报错消除、海洋蓝切换后 TabBar 文字 rgb(63,127,191) 正常联动）。

***

## v11.19 (2026-09-04) 记账模块：全局主题系统 + 记一笔页重设计 + Long序列化拼接Bug修复

> 用户反馈 5 项：总负债拼接 Bug、UI 风格按参考图全局优化、记一笔页按类型展示分类并突出核心字段、主题可切换、上传与预览。无表结构变更、无 SQL、无菜单变更。

### 改动内容
- **Long 拼接 Bug（关键修复）**：后端全局配置 Long→String 序列化（防 JS 精度丢失），前端对 balance/amount 做 reduce 累加时字符串拼接（222+4100 显示"02224100"）。[money.js](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/utils/money.js) 新增 toNum() 强转数字，修复负债页/资产页总计数、流水页收支汇总、报表页分类合计、记一笔页余额校验共 8 处累加点
- **主题系统**：[theme store](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/stores/theme.js) — 5 套主题（薄荷绿默认/紫罗兰/海洋蓝/蜜桃橙/樱花粉），CSS 变量注入页面根元素（--primary/--primary-strong/--primary-soft/--primary-shadow），App.vue 定义变量默认值；全部 11 个页面根元素 :style="themeVars" 注入并替换硬编码紫色系；设置页新增主题切换卡（彩色圆点选择，TabBar 选中色联动，本地存储记忆）；pages.json TabBar/导航栏默认色改薄荷绿
- **记一笔页重设计**（按参考图）：类型 Tab 改白色胶囊（主色底白字激活）→ 分类宫格（4 列彩色圆底 emoji 图标，选中主色放大）→ 备注卡片（突出）→ 主色金额条（左金额标签+日期、右大数字）→ 账户/负债选择卡片 → 数字键盘（可收起）→ 保存按钮；分类按当前类型过滤（支出/收入），**常用优先排序**（usedCount 降序+sortOrder）；二级分类横滑标签条（表结构已有 parent_id，前端预留展示）
- **后端分类接口增强**：[LedgerCategoryServiceImpl](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ledger/service/impl/LedgerCategoryServiceImpl.java) listAvailable 回填 usedCount（@TableField(exist=false) 展示字段，按用户流水分组统计），支撑前端常用排序
- **负债页样式**：总负债卡红色渐变→主题色渐变；列表图标红底→主题色
- **上传接口确认**：/portal/file/upload 存在且正常（未登录返回 HTTP 200+code:401 属预期，v11.15 已落地凭证上传），前端 uni.uploadFile 携带 Bearer token；预览走 uni.previewImage

### 验证
- `npm run build:h5` + `mvn compile`：均通过
- 浏览器端到端（zhangsan 登录态）：总负债 ¥4,322.00 = 222.00+4,100.00 数学求和正确；总资产/净资产求和均正确；记一笔新布局（类型Tab/分类宫格按类型过滤/金额条突出）正常；主题切换海洋蓝→记一笔页 Tab 与金额条实时变蓝（#5B9BD5），切回薄荷绿恢复；console 无业务报错
- 上传的 H5 文件选择器在自动化浏览器中受限，需人工验证；接口 curl 确认可达

### 部署
**重启 moyun-server** 生效（分类接口新增 usedCount 字段）。前端 Vite 热更新自动生效。无 SQL。

***

## v11.18 (2026-09-03) 记账模块体验迭代：分类标签 + 语义提示 + 未登录引导

> 用户试用反馈 8 项的落地。无表结构变更、无 SQL、无菜单变更，纯前端交互与引导优化（复式记账流水在 Phase 1 已实现，本次补语义说明）。

### 改动内容
- **备注+分类标签**：[记一笔页](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/pages/record/index.vue) — 分类改为不区分类型拉取全部可用分类，6 种行为（支出/收入/转账/还款/借款/校准）均可选分类打标签（提示"选填，打标签"）；备注行对所有类型可用
- **数字键盘可收起**：键盘区增加"收起键盘 ∨"把手（kbVisible 状态），收起后金额区显示"⌨ 输入"按钮点击再展开，不再常驻占屏
- **类型语义提示**：类型栏下方 hint 条逐类型说明 — 转账=资产账户间互转（A 减、B 加，总资产不变）、还款=资产出钱+欠款减少（余额不足提示补录）、借款=欠款增加+钱入账户（可不选账户）、校准=余额直接修正为目标值
- **负债快速补录**：还款/借款的借款项目选择弹层内置"快速补录"表单（名称+当前欠款），调 createLiability 创建后自动选用；空列表点击也引导跳负债页
- **还款余额不足校验**：保存前校验扣款账户余额，不足时弹窗说明"该账户余额不能兑现此笔还款"，提供「去补录收入」（切换为收入类型、预选同一账户、保留金额，补记资金来源如刚到账的奖金）与「仍要保存」两个选择
- **我的页未登录收敛**：[我的页](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/pages/mine/index.vue) — 未登录时功能菜单改为置灰预览（"登录后可用"），不可点击误导；登录表单常驻；go() 兜底校验登录
- **首页价值前置**：[总览页](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/src/pages/dashboard/index.vue) — 未登录：品牌区（墨韵记账·一个数字，帮你随时随地了解你的身家）+「你是否也有这些烦恼」3 条（钱花哪了说不清/净资产糊涂账/借出去的钱没着落）+ 三步引导（录资产负债→随手记一笔→净资产自动更新）+ 登录入口（注明门户账号可直接登录）；已登录：可关闭的操作提示条（提示备注/分类/凭证/余额补录能力）+ 原有数据总览
- **复式流水说明**（既有实现确认）：转账在 ledger_transaction 双写 A 出账/B 入账流水（balance_after 各自快照），借/还分别记 borrow/repayment 流水并联动负债余额，总资产口径下转账不改变净资产

### 验证
- `npm run build:h5`：编译通过
- 浏览器验证（已登录态）：记一笔 6 类型切换、转账语义提示+转入账户行、还款行、备注/分类行、键盘收起展开均正常
- 浏览器验证（清除 token 后未登录态）：首页显示品牌区/痛点/三步/登录按钮（无净资产数据）；我的页显示登录表单+置灰菜单预览（无退出登录）；记一笔弹出登录引导弹窗

### 部署
前端 Vite 热更新自动生效，无需重启后端、无 SQL。

***

## v11.17 (2026-09-03) 记账模块 Phase 3 收尾：站内预算提醒 + 新手引导

> 设计方案 V1.3 §13 预算提醒的站内通道（订阅消息模板待申请，先落地事务内实时判断）+ Phase 2 遗留的新手引导。无表结构变更。

### 改动内容
- **站内预算提醒**：[LedgerTransactionServiceImpl](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ledger/service/impl/LedgerTransactionServiceImpl.java) 新增 checkBudgetAlert(userId) — 记账事务提交后判断当月总预算使用率：≥100% 提示「预算已超支」、≥80% 提示「已使用 N%」；每阈值每自然月仅推一次（Redis key `ledger:budget:alert:{userId}:{yyyyMM}:{阈值}`，TTL 至月底）；异常兜底不影响记账主流程
- **接口返回**：POST /portal/ledger/transactions 响应新增 budgetAlert 字段（null=无提醒）；前端记一笔页保存后以弹窗展示提醒（替代普通 toast）
- **新手引导**：dashboard 接口新增 assetAccountCount/liabilityAccountCount 字段；总览页无任何账户且未关闭过时显示「3 步开始」引导卡（添加账户 → 记第一笔 → 设预算），可点击直达对应页、可"不再显示"（ledger_guide_dismissed 存储）
- **容错**：旧接口结构（无新字段）下前端不崩溃，budgetAlert 为空走普通 toast

### 验证
- `mvn compile`：通过（0 错误）
- 浏览器验证：总览/记一笔页正常渲染、console 无报错、引导卡显示逻辑正确
- 端到端验证发现并修复 Bug：`Duration.between(LocalDate, LocalDate)` 抛 UnsupportedTemporalTypeException（LocalDate 无时间单位）被 catch 吞掉 → 去重 key 写入但 expire 失败（TTL=-1 永不过期）且提醒文案返回 null。已改用 `ChronoUnit.SECONDS.between(now.atStartOfDay(), nextMonthStart.atStartOfDay())` 计算 TTL；catch 补充 log.warn 便于排查；本地 Redis 脏 key 已清理

### 部署
**重启 moyun-server** 生效（dashboard 新字段 + transactions 新响应字段）。无 SQL。

***

## v11.16 (2026-09-03) 记账模块 Phase 3：报表中心 + CSV 导出

> 设计方案 V1.3 §12 Phase 3 核心项：报表全量 + 数据导出。无表结构变更（复用既有 6 表数据），纯增量接口与页面。

### 改动内容
- **新增报表服务**：[LedgerReportServiceImpl](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ledger/service/impl/LedgerReportServiceImpl.java) + ILedgerReportService — overview(userId, year) 返回：月度收支趋势（12 月，is_budget=1 口径）/ 年收支结余 / 分类收支排名（系统预设+自定义合并名称）/ 净资产趋势（近 30 天快照）/ 账户余额分布 / 在还负债一览 / 当月总预算执行；buildCsv 按日期区间导出流水（UTF-8 BOM + 逗号引号转义，Excel 兼容）
- **新增接口**：[PortalLedgerReportController](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ledger/controller/PortalLedgerReportController.java) — GET /portal/ledger/reports/overview?year=、GET /portal/ledger/reports/export?startDate=&endDate=（CSV attachment，URLEncoder 文件名）；无表变更、无菜单变更，落入门户安全链 authenticated 兜底
- **前端报表中心**：pages/report/index.vue — 年份切换（‹ 2026 ›）、年度收支概览（收/支/结余）、月度收支柱状图（纯 CSS 双色柱）、支出/收入分类 TOP 横条、净资产趋势条、账户余额分布、在还负债、CSV 导出（H5 fetch blob 下载 / 小程序 downloadFile+openDocument 条件编译）；响应隐私模式（金额脱敏 ****）
- **入口**：pages.json 注册 + 「我的」菜单"报表中心" + 总览"本月收支"卡右上角"报表"链接

### 验证
- `mvn compile`：通过（0 错误）
- 浏览器验证：报表页 6 类卡片全部渲染、无 JS 错误（后端重启前接口数据为空属预期）

### 部署
**重启 moyun-server** 生效（新接口 /portal/ledger/reports/**）。与 v11.15 的重启可合并为一次。

***

## v11.15 (2026-09-03) 记账模块 Phase 2：uni-app 前端落地 + 凭证截图 + UI 紧凑化

> 前置修复：门户登录态以 HTTP 200 + code:401 返回时前端拦截器未识别（记一笔页账户/负债列表静默为空）。request.js 增加 body.code===401 分支（清 token + 引导登录），记一笔页 onShow 登录校验 + 空列表引导。

### 改动内容
- **增量 SQL**：[20260903-02-moyun-ledger-voucher.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/20260903-02-moyun-ledger-voucher.sql) — ledger_transaction 新增 voucher_url VARCHAR(500)（凭证截图URL，复用门户文件服务）
- **凭证截图（前后端）**：记一笔/编辑页支持选择截图（相册/相机）→ 上传 /portal/file/upload（businessType=ledger_voucher）→ voucher_url 入库；流水明细页"凭证"角标点击 uni.previewImage 预览；编辑页支持更换/移除（传空串清除，null 保持）
- **流水分页增强**：[LedgerTransactionServiceImpl](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ledger/service/impl/LedgerTransactionServiceImpl.java) pageTransactions 批量回填 accountName/targetAccountName/liabilityName/categoryName（@TableField(exist=false) 展示字段），前端零二次查询
- **流水明细页紧凑化**：按日分组（今天/日期 + 日收支小计）+ 页首汇总条（笔数/收/支）+ 行内分类/商户/账户标签（图标 72→56rpx，行距 20→14rpx）
- **记一笔页 UI**：选择弹层加高（sheet 70→82vh、列表 56→70vh）、键盘键 100→88rpx、保存按钮 88→76rpx；新增凭证截图行
- **首页隐私开关**：总览 hero 卡右上角"隐私 开/关"胶囊，与设置页共用 ledger_privacy 存储
- **空列表引导**：记一笔选择弹层空数据时按类型跳转资产/负债/分类管理页

### 验证
- `mvn compile`：通过（0 错误）
- 20260903-02 SQL 已在本地库执行（voucher_url 字段已确认）

### 部署
执行 20260903-02-moyun-ledger-voucher.sql 后**重启 moyun-server**（流水分页返回结构新增名称字段与 voucherUrl）；前端 Vite 热更新自动生效。

***

## v11.14 (2026-09-03) 记账模块（个人资产管理）Phase 1：后端核心落地

> 新需求立项：记账 App/小程序，核心价值「一个数字看清全部身家」。设计方案经两轮评审定稿为 V1.3（单账本模型、仅人民币，App 端走 /portal/ledger/** 复用门户安全链）。本阶段交付后端全量：6 张表、资产/负债账户、6 种记账类型的联动事务核心、流水冲正重放、dashboard 总览、净资产每日快照任务。微信登录绑定（用户中心改造）与 uni-app 前端工程属 Phase 1 后续项。

### 改动内容
- **DDL**：[20260903-01-moyun-ledger.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/20260903-01-moyun-ledger.sql) — 6 表（ledger_asset_account / ledger_liability_account / ledger_transaction / ledger_category / ledger_budget / ledger_net_worth_snapshot，金额统一 BIGINT 分）+ 26 条系统预设分类（user_id=0+is_system=1）+ 管理端菜单 5400-5413（记账管理/预设分类/运营统计，超管已授权），幂等可重复执行
- **新增模块** com.moyun.ledger（33 个类）：6 实体（不继承 BaseEntity，流水自管 status 逻辑删除）+ 6 Mapper（MapperScan 通配自动覆盖）+ 6 Service + 6 Controller + 1 定时任务
- **记账事务核心**：[LedgerTransactionServiceImpl](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ledger/service/impl/LedgerTransactionServiceImpl.java) — 单 @Transactional 内完成余额双边更新（乐观锁 version）+ balance_after 系列快照写入 + 净资产快照 upsert；修改=旧记录冲正→新记录重放；删除=冲正→status=0 归档；还款超额拒绝；还款至 0 自动置 settle_flag=1（冲正回到 >0 时重置）
- **账户服务**：资产初始余额自动生成 adjust 流水、负债初始欠款自动生成 borrow 流水（全明细追溯）；update 强制保持原 balance（校准必须走记账）；删除即 status=0 归档，流水永久保留
- **API（/portal/ledger/**）**：dashboard（净资产/总资产/总负债/涨跌对比昨日快照/本月收支/预算进度/最近 20 条流水）、assets / liabilities CRUD、transactions POST/GET/PUT/DELETE（路径参数均带 {id:[0-9]+} 正则约束）、categories（系统预设+自定义合并）、budgets（存在即更新的 upsert 语义）
- **定时任务**：[LedgerNetWorthSnapshotTask](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ledger/job/LedgerNetWorthSnapshotTask.java) — 每日 00:10 全量补漏快照（当日有记账的用户由事务内实时 upsert 维护）
- **安全**：/portal/ledger/** 落入门户链 securityMatcher("/portal/**") 的 anyRequest().authenticated() 兜底，零安全链改动；所有操作强制 user_id 归属校验（防水平越权）

### 设计要点（与方案的对应）
- 联动规则矩阵 V1.3 §5.1：income/expense/transfer/repayment/borrow/adjust 六类，转账/还款/借款净资产守恒
- 统计口径 V1.3 §4.3.6：净资产=账户现值聚合，涨跌=对比昨日快照，收支=流水表 is_budget=1 实时聚合
- user_id 口径 = portal_user.id（V1.3 §3.1 双 ID 陷阱警示）

### 验证
- `mvn compile`：通过（0 错误）

### 部署
执行 20260903-01-moyun-ledger.sql 后重启 moyun-server。管理端页面（views/cms/ledger/ 预设分类 + 运营统计）与 uni-app 前端工程为后续交付项；微信登录端点依赖用户中心改造（appid 申请为前置 TODO）。

---

## v11.15 (2026-09-03) 记账模块 Phase 1 收尾：uni-app 工程 + 管理端页面

> 前后端闭环完成：新建 uni-app 工程 moyun-ledger-app（一套代码编译 H5/小程序），管理端补齐预设分类维护与脱敏运营统计，与后端 v11.14 交付的 API 全面对接。

### 改动内容
- **新工程** [moyun-ledger-app](file:///d:/zyg_new_work/moyun-project-document/moyun-ledger-app/package.json)（uni-app 4.29 + Vue3 + Pinia + Vite5，自定义组件替代 uView Plus 降低依赖面）：
  - 5 Tab：总览（净资产卡+涨跌标识+预算进度+最近20条流水）/ 资产 / 记一笔 / 负债 / 我的
  - 记一笔页：6 类型切换（支出/收入/转账/还款/借款/校准）+ 自研数字键盘（两位小数限制）+ 账户/负债/分类底部选择器；保存后保留账户便于连续记账
  - 流水明细页（类型/日期区间筛选+分页加载）、流水编辑页（冲正重放提示）、分类管理（系统+自定义）、预算设置（总预算+分类预算 upsert）、隐私模式设置（金额打星）
  - 请求层与门户约定一致：token key `moyun_token`、`Authorization: Bearer`、AjaxResult code=200 判定、401 自动登出
- **管理端后端**（com.moyun.ledger.controller）：
  - CmsLedgerCategoryController（/cms/ledger/category，仅维护 user_id=0 系统预设，用户自定义分类无后台入口——隐私红线）
  - CmsLedgerStatsController（/cms/ledger/stats/overview，仅聚合指标：用户数/30日活跃/流水量/类型分布，无个体数据）
- **管理端前端**（moyun-admin-vue）：[预设分类](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/ledger/category/index.vue)（CRUD+颜色选择器+停用）+ [运营统计](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/ledger/stats/index.vue)（四指标卡+类型分布表+脱敏声明），API 模块 [api/cms/ledger](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/api/cms/ledger/index.js)
- **金额规范**：前端元/后端分转换统一走 utils/money.js（yuanToCent/centToYuan），页面内禁止手写 *100

### 验证
- moyun-ledger-app：`npm run build:h5` 与 `npm run build:mp-weixin` 双端构建通过
- moyun-server：`mvn compile` 通过（含新增 CMS Controller）

### 部署
- App 端开发调试：`npm run dev:h5`（H5 直接访问）或导入 `dist/dev/mp-weixin` 到微信开发者工具（需勾选"不校验合法域名"）
- 小程序上线前需在 manifest.json 填入 appid 并配置合法域名；管理端重新构建发布后，菜单"记账管理"（5400）出现
- 登录复用门户账号（/portal/login），微信授权登录仍为 TODO（依赖用户中心改造）

---

***

## v11.13 (2026-09-02) 面试题库归属学习中心：路由反转 + 面包屑修正

> 用户反馈：题库页 URL 为 `/interview/questions`，面包屑显示"首页/面试指南/面试题库"，但题库实际归属学习中心（与 portal_category.nav_route_path 及学习中心栏目对齐）。修正为 URL `/learn/questions`、面包屑"首页/学习中心/面试题库"。

### 改动内容
- **路由反转**：[router/index.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/router/index.ts#L234-L244) — `/learn/questions` 升为主路由（name `interview-questions` 保留，无站内 name 导航引用）；`/interview/questions` 改为 redirect 兼容旧收藏/外链
- **面包屑**：[QuestionListPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/learn/QuestionListPage.vue#L166-L170) 改"首页/学习中心/面试题库"；[QuestionDetailPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/learn/QuestionDetailPage.vue#L265-L270) 同步为"首页/学习中心/面试题库/题目详情"
- **SEO canonicalPath**：QuestionListPage 规范化 URL 改 `/learn/questions`
- **站内 11 处跳转引用同步**：HomePage/InterviewPage（3 处含 categoryId 透传）/MyBookmarksPage/MyAttemptsPage/KnowledgeGraphPage（keyword 透传）/LeaderboardPage/PracticeCenterPage/PracticeChoiceListPage/PracticeCodingListPage/QuestionDetailPage 返回按钮/StudyCalendarPage，全部改指 `/learn/questions`；query 透传不受影响，相邻题导航逻辑同源正常

### 验证
- `vue-tsc -b`：类型检查通过（0 错误）

### 部署
仅前端改动，无 SQL/后端变更，`npm run build:prod` 重新构建部署。旧链接 `/interview/questions*`（含 query）自动 302 到 `/learn/questions*`。

---

## v11.12 (2026-09-02) 通用 AI 异步任务基础设施 + 全局慢请求保护 + URL 状态参数化（v10.23）

> 用户反馈三个问题：① 门户 LLM 接口多为同步调用，大输入场景（简历解析/岗位匹配等）直接超时且前台无感；② 慢接口等待中用户切页/刷新无提示，结果静默丢失；③ 有状态页面（如优化工作台 step/选中岗位）刷新后回到初始步，要求状态参数放 URL 支持多次刷新。评估结论：三个问题全部成立；流式输出（SSE）不适合本项目（LLM 输出为结构化 JSON，半截 JSON 无法渲染），统一采用"异步任务 + 前端轮询"，已在深度优化场景验证过。

### 问题1：LLM 同步接口超时 → 通用 AI 异步任务基础设施

全项目 8 处 llmClient.chat 调用点风险分级后，4 个高风险场景统一接入新建的通用任务表：

- **DDL**：[20260902-01-moyun-ai-task-unify.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/20260902-01-moyun-ai-task-unify.sql) — 新建 portal_ai_task（task_type/biz_ref/status/progress_msg/result/error）；旧表 portal_resume_optimize_task 保留不写入
- **基础设施**（新建 7 类）：PortalAiTask/PortalAiTaskMapper/AiTaskVO/AiTaskHandler 接口/AiTaskService（提交+查询，handlerMap 校验 taskType）/AiTaskAsyncExecutor（@Async("aiTaskExecutor")，pending→running→success/failed，error 截断 900 字符）/PortalAiTaskController（`POST /portal/ai/task/submit`、`GET /portal/ai/task/{id}`）
- **四个 Handler**：ResumeParseTaskHandler(resume_parse)/JobMatchTaskHandler(job_match)/AiDraftTaskHandler(ai_draft)/DeepOptimizeTaskHandler(deep_optimize)，bizRef 分别为 {resumeId,fileUrl,fileName}/{resumeId,jobTargetId}/{resumeId,jobTargetId?}/{resumeId,jobTargetId}
- **简历解析快路径**：[PortalUserResumeController](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalUserResumeController.java) parse 接口改为秒回（保存文件+建附件简历记录+提交任务，返回 {resumeId, taskId, fileName}），LLM 解析挪入 executeParse 异步执行
- **深度优化迁移**：/deep/.../async 与 /deep/task/{taskId} 内部切换到 portal_ai_task，返回结构映射为旧 ResumeOptimizeTaskVO（前端零改动）；删除旧 ResumeOptimizeAsyncExecutor/PortalResumeOptimizeTask/PortalResumeOptimizeTaskMapper；线程池 resumeOptimizeExecutor 改名 aiTaskExecutor
- **保持同步**：fieldAssist（单字段输出小）、语音面试（自有 SSE 逻辑）

### 问题2：慢接口切页/关页无提示 → 全局追踪 + 双守卫

- **请求追踪**：[client.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/client.ts) isAiSlowUrl 判定 POST 慢接口（parse/ai-assist/ai-draft/match/deep 同步版/语音面试 start/finish/answer），request() 内自动登记/finally 清理；语音面试 SSE 用 trackAiSlowRequest 手动登记
- **路由守卫**：[router/index.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/router/index.ts) beforeEach——AI 慢请求进行中跳转页面弹确认框（离开将中断生成）
- **关页提醒**：[main.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/main.ts) beforeunload——进行中刷新/关闭触发浏览器原生确认
- **异步任务豁免**：异步任务轮询期间离开是安全的（任务后端继续跑，回来可恢复），不进拦截名单，符合"能恢复的不拦、会丢的拦"原则

### 问题3：刷新后回不到原位 → URL 参数化（URL 优先于 localStorage）

- **前端 API**：[aiTask.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/aiTask.ts) — submitAiTask/getAiTask/pollAiTask（onTick 进度回调，resolve 任务结果内容）
- **优化工作台**：[ResumeOptimizePage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeOptimizePage.vue) — step/resumeId/targetId/taskId/matchTaskId 五参数 router.replace 同步到 URL；刷新时 URL 优先、localStorage 兜底；匹配任务改 runMatchAsync（提交 job_match 任务+轮询+进度动画），refresh 可恢复轮询（resumeMatchPolling）
- **编辑页**：[ResumeEditPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue) — 上传解析 URL 带 parseTaskId，刷新后继续轮询，完成后跳附件简历编辑页；AI 填充空字段改异步任务（提交 ai_draft+轮询）

### 验证
- moyun-server `mvn compile`：编译成功（exit 0）
- moyun-portal `vue-tsc -b`：类型检查通过（0 错误）

### 部署
1. 执行 SQL 补丁 `20260902-01-moyun-ai-task-unify.sql`
2. 重启 moyun-server
3. 前端 `npm run build:prod` 重新构建
4. 验证：上传简历秒回并显示解析进度→切页弹确认→刷新后 URL 恢复 step/岗位→匹配/深度优化全程可刷新续轮询

---

## v11.11 (2026-09-01) 简历模块重构：上传闭环+解析反显+附件版本+AI填空+全文分析+diff回放（v10.22）

> 用户反馈：简历模块不合理不完善——上传简历只是临时解析不保存源文件、解析结果没反显表单、空字段无 AI 填充、AI 分析用结构化 JSON 而非全文、优化结果无 diff 回放、编辑页多个 AI 优化入口重复。本次重构按"上传→解析→维护→AI分析→反显修改"完整闭环重新设计。

### 四阶段改造总览

#### 阶段1：上传闭环（源文件保存 + 解析反显 + 附件简历版本）
- **DDL 补丁**：[20260901-moyun-resume-upload-parse-fulltext.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/20260901-moyun-resume-upload-parse-fulltext.sql) — portal_user_resume 加 source_type/source_file_url/source_file_name/full_text 字段
- **Entity**：[PortalUserResume.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/domain/entity/PortalUserResume.java) — 加 4 个字段
- **VO**：UserResumeVO/ResumeParseVO — 加 sourceType/sourceFileUrl/sourceFileName/attachmentResumeId/fullText
- **后端核心**：[ResumeParseService.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeParseService.java) — 新增 parse(MultipartFile, Long) 重载：保存源文件→创建 attachment 类型简历记录→解析结果填充到附件简历→返回 attachmentResumeId
- **后端接口**：[PortalUserResumeController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalUserResumeController.java) — 新增 /attachments（附件列表）、/{id}/download-attachment（下载源文件）、/{id}/convert-to-online（转在线简历）
- **Service**：UserResumeServiceImpl — 新增 selectAttachmentList/convertAttachmentToOnline，saveResume 自动拼接 full_text，entityToVO 映射新字段
- **前端上传**：[ResumeEditPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue) — 上传成功后跳转 `/interview/resume/edit?resumeId={attachmentResumeId}`，编辑页 loadResume 自动反显解析数据
- **前端列表**：MyResumesPage.vue — 附件简历显示"附件"标签+文件名+"下载源文件"+"转为在线"按钮
- **统一 AI 入口**：编辑页移除右侧 aiAdvice 面板（与优化工作台重复），底部"AI优化"按钮统一跳转优化工作台，保留字段级 fieldAssist 实时辅助

#### 阶段2：AI 填充空字段
- **后端**：[ResumeDeepOptimizeService.aiDraftEmptyFields](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeDeepOptimizeService.java) — 为空的工作经历/项目经历/自我介绍生成初始草稿（区别于 fieldAssist 优化已有内容），基于已有信息+可选目标岗位 JD
- **接口**：`POST /portal/resume/optimize/ai-draft/{resumeId}?jobTargetId=`
- **前端**：[ResumeEditPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue) — 横幅区域新增"AI 填充空字段"按钮（works/projects/selfIntro 有空字段时显示），生成后自动填充+静默保存

#### 阶段3：全文存储 + AI 分析改造
- **全文拼接**：UserResumeServiceImpl.saveResume 自动拼接结构化字段为纯文本存入 full_text
- **AI 分析改造**：[ResumeJobMatchService](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeJobMatchService.java) + [ResumeDeepOptimizeGenerator](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeDeepOptimizeGenerator.java) — 优先用 full_text（上下文更完整），为空时降级为结构化 JSON

#### 阶段4：diff 回放
- **新组件**：[DiffView.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/DiffView.vue) — 自实现 LCS 字符级 diff，added 绿色背景/removed 红色删除线，不引入第三方库
- **接入**：[OptimizeCompare.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/OptimizeCompare.vue) — 优化后文本区改用 DiffView 渲染，直观看到增删改

### 关键设计决策

1. **附件简历独立类型而非版本**：source_type=attachment 作为独立简历出现在列表，可下载、可转在线简历。避免与版本历史（versionNo）耦合
2. **full_text 用纯文本而非 HTML**：纯文本对 LLM 更友好，HTML 需维护模板且不如纯文本上下文完整
3. **字段映射用 LLM prompt 固定标准**：暂不引入独立配置模块（过度设计），LLM prompt 已定义标准字段
4. **aiDraftEmptyFields vs fieldAssist**：前者为空字段生成初始内容，后者为已有内容生成优化版本，职责分离
5. **AI 分析降级**：full_text 为空时降级为结构化 JSON（兼容旧简历）

### 验证
- moyun-server `mvn compile`：编译成功（exit 0）
- moyun-portal `vue-tsc -b`：类型检查通过（0 错误）

### 部署
1. 执行 SQL 补丁 `20260901-moyun-resume-upload-parse-fulltext.sql`
2. 重启 moyun-server 后端
3. 前端 `npm run build:prod` 重新构建
4. 验证：上传 PDF 简历→自动跳转编辑页反显→列表可见附件标签→可下载源文件→可转在线简历→AI 填充空字段→优化工作台 diff 回放

---

## v11.10 (2026-09-01) 简历优化页刷新状态恢复（v10.21）

> 用户反馈：简历优化页 `/interview/resume/optimize?resumeId=1` 在 AI 分析或深度优化进度条等待期间，不小心刷新页面会回到 step1 重新开始，之前的分析结果、采纳状态、异步任务全部丢失。

### 修复方案：localStorage 持久化 + 按 resumeId 分 key

#### 1. 持久化关键状态
[ResumeOptimizePage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeOptimizePage.vue#L96-L172)

按 `resumeId` 分 storage key（`moyun:resume-optimize:state:{resumeId}`），避免多简历串扰。TTL 2 小时过期防止陈旧状态。

持久化字段：step、selectedTargetId、matchReport、optimizeResult、adoptedIndexes、savedResumeId、asyncTaskId

#### 2. 异步任务轮询恢复
新增 `currentAsyncTaskId` ref 跟踪当前异步任务，`generateOptimize` 提交后记录 taskId，`pollOptimizeTaskStatus` 成功/失败时清除。新增 `resumeAsyncPolling(taskId)`：刷新后若检测到持久化的 asyncTaskId 且 step=4，自动重建进度动画 + 4 秒轮询。

#### 3. onMounted 恢复流程
loadJobTargets → loadResumes → 设置 selectedResumeId → loadStateFromStorage → applyRestoredState → 若 asyncTaskId 存在且 step=4 调 resumeAsyncPolling

#### 4. loadJobTargets 不覆盖已恢复选择
原代码总是设置默认岗位覆盖 selectedTargetId，改为「仅在未选中时设置默认」。

#### 5. 切换简历恢复对应快照
watch(selectedResumeId) 切换时：有快照则恢复（含异步轮询），无快照重置到 step1。

#### 6. 自动持久化 watch
watch([step, selectedTargetId, matchReport, optimizeResult, adoptedSet, savedResumeId, currentAsyncTaskId], { deep: true }) 任一变化时自动保存。

### 恢复场景

| 场景 | 刷新后恢复 |
|---|---|
| 深度优化轮询中（step4） | 进度条+轮询自动恢复，任务完成显示结果 |
| 优化结果已生成（step4） | 结果+采纳状态完整恢复 |
| 已保存（step5） | step5，预览已保存简历 |
| AI 分析中（step3） | 需重新点分析（同步接口无 taskId） |

### 验证
- moyun-portal `vue-tsc -b`：类型检查通过（0 错误）

### 部署
仅前端改动，`npm run build:prod` 重新构建部署，无 SQL/后端变更。

---

## v11.9 (2026-09-01) 简历深度优化采纳失效修复：section 归一化 + 越界报错（v10.20）

> 用户反馈：工作经历、项目经历采纳后没有保存。根因：LLM 返回的 `section` 值不一致（可能返回 `works`/`projects`/`experience` 等复数或同义词），前后端 `switch case "work"`/`case "project"` 精确匹配走不到，静默走 default 忽略，用户以为采纳了实际没生效。

### 根因分析

LLM prompt 指示 section 取值为 `objective/education/work/project/skills/selfIntro`，但实际返回可能存在：
- 复数形式：`works`/`projects`/`educations`
- 同义词：`experience`/`introduction`/`summary`
- 大小写/空格：`Work`/` work `

原代码前后端 `switch(section)` 精确匹配上述变体均走 default 静默忽略，**采纳了但没应用**，保存的 resume 仍是原内容，用户感知为"没有保存"。

### 修复方案：三层防御

#### 1. Prompt 约束加强（Generator）
[ResumeDeepOptimizeGenerator.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeDeepOptimizeGenerator.java#L70-L80)
- section 枚举说明改为「必为以下枚举之一：...；严禁使用复数如 works/projects，严禁使用 experience/introduction 等同义词，必须完全匹配枚举值」

#### 2. Section 归一化容错（前后端一致）
新增 `normalizeSection()` 方法（前后端逻辑完全一致），对 section 做 trim+lowercase 并映射变体：
- `works`/`experience`/`experiences`/`working` → `work`
- `projects`/`project_experience` → `project`
- `educations`/`education_experience` → `education`
- `self_intro`/`selfintro`/`selfintroduction`/`introduction`/`intro`/`summary` → `selfIntro`
- `job_intention`/`jobintention`/`intention` → `objective`
- `skill`/`skill_list`/`skilllist` → `skills`

应用点（三处保持一致）：
- 后端 [ResumeDeepOptimizeService.applyItem](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeDeepOptimizeService.java#L323-L377)（保存时应用到 resume）
- 后端 [ResumeDeepOptimizeGenerator.fillOriginal](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeDeepOptimizeGenerator.java#L213-L265)（生成时回填原文）
- 前端 [ResumeOptimizePage.applyOptimizes](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeOptimizePage.vue#L599-L627)（预览应用）
- 前端 [OptimizeCompare.sectionLabel](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/OptimizeCompare.vue#L172-L221)（标题显示）

#### 3. 越界/未知 section 改为明确报错（不再静默忽略）
- **applyItem 越界**：原 `idx < size()` 失败静默跳过，改为抛 `ServiceException("工作经历索引越界：2（简历只有 1 条）")`，用户看到明确错误可重试
- **未知 section**：原 default 静默忽略，改为抛 `ServiceException("无法识别的优化字段类型：xxx（建议重新生成）")`
- **fillOriginal 越界**：保持静默（original 留空不影响生成结果展示，容错优先）

#### 4. 关键日志便于排查
- Generator.generate 返回后输出 items 摘要：`[DeepOptimize] generate 返回 5 项：[0]section=work,index=0,field=description, [1]section=project,index=1,field=description, ...`
- applyAndSave 开始时输出简历实际数据：`[DeepOptimize] applyAndSave 开始：resumeId=1 简历含 works=2/projects=1/educations=1，采纳索引=[0, 1]`
- 每条应用前输出：`[DeepOptimize] 应用建议[0]：section=work, index=0, field=description`

### 验证
- moyun-server `mvn compile`：编译成功（exit 0）
- moyun-portal `vue-tsc -b`：类型检查通过（0 错误）
- 线上排查：重启后端后，在 moyun-server 日志中搜索 `[DeepOptimize]` 可看到 generate 返回的 section 值与 applyAndSave 的应用过程

### 部署
- 后端：重启 moyun-server
- 前端：`npm run build:prod` 重新构建
- 无 SQL 变更

---

## v11.8 (2026-09-01) 简历深度优化循环依赖根治：抽离 Generator（v10.19 结构调整）

> v11.6 用 `@Lazy` 绕过 ResumeDeepOptimizeService ↔ ResumeOptimizeAsyncExecutor 循环依赖，但 `@Lazy` 只是推迟注入时机，结构问题未解决。本次从代码结构层面抽离 Generator Bean，彻底消除循环。

### 依赖图对比

**改造前（有环 A→B→A）**：
```
ResumeDeepOptimizeService ──→ ResumeOptimizeAsyncExecutor
         ↑                           │
         └───────────────────────────┘  executeTask 调 generate
```

**改造后（单向无环）**：
```
ResumeDeepOptimizeService ──→ ResumeOptimizeAsyncExecutor ──→ ResumeDeepOptimizeGenerator
         └────────────────────────→ ResumeDeepOptimizeGenerator ─┘
```

### 1. 新建 Generator
- [ResumeDeepOptimizeGenerator.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeDeepOptimizeGenerator.java)
  - 持有 `generate()` 及其私有方法 `buildResumeContext/fillOriginal/safe` 和 LLM 相关依赖（`aiProperties/llmClient/objectMapper/jobTargetMapper`）
  - 不反向依赖任何 Service/Executor，职责单一
  - 对外暴露辅助方法 `isAiAvailable()` 和 `getJobTarget(id)`，供 Service 前置校验复用

### 2. 改造 AsyncExecutor
- [ResumeOptimizeAsyncExecutor.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeOptimizeAsyncExecutor.java)
  - 移除 `@Lazy` 注解和 `deepOptimizeService` 依赖
  - 改注入 `ResumeDeepOptimizeGenerator`，`executeTask` 调 `generator.generate()`（不再调 Service）
  - 类注释明确标注依赖方向「Service → Executor → Generator（单向无环）」

### 3. 改造 Service
- [ResumeDeepOptimizeService.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeDeepOptimizeService.java)
  - 移除 `generate()` 实现（迁移到 Generator）、`buildResumeContext/fillOriginal/safe` 私有方法
  - `generate()` 改为门面委托 `generator.generate()`，保留公共 API 不变（Controller 调用方无感）
  - `submitTask()` 前置校验改用 `generator.getJobTarget()` / `generator.isAiAvailable()`，避免重复实现
  - 新增 `@Autowired ResumeDeepOptimizeGenerator generator` 依赖

### 设计取舍
- **抽 Generator vs 其他方案**：
  - `@Lazy`（原方案）：推迟注入时机，结构问题未解决，Bean 间语义混乱
  - `ObjectFactory<>` / `Provider<>`：每次调用解析，开销略大，且本质同 `@Lazy`
  - 事件驱动：异步任务用 Spring Event 解耦，但引入 EventBus 监听器，过度设计
  - 抽 Generator：最直接，把「生成能力」作为独立维度，符合单一职责
- **保留 Service.generate() 门面**：Controller 等调用方无需改动，迁移对调用方透明
- **isAiAvailable/getJobTarget 公开**：供 Service 前置校验复用，避免 Service 重复注入 aiProperties/llmClient/jobTargetMapper（这些已迁到 Generator）

### 验证
- moyun-server `mvn compile -DskipTests -pl moyun-server -am -q`：编译成功（exit 0）
- 启动验证（部署后执行）：重启 moyun-server，不再报 `BeanCurrentlyInCreationException` / `BeanCreationNotAllowedException`

### 部署
仅后端 Java 代码改动，无 SQL/前端变更。重启 moyun-server 后端即可。

---

## v11.7 (2026-09-01) 简历深度优化采纳交互优化（v10.20）

> 用户反馈：采纳按钮点击后体验割裂——没有"写到对应字段"的视觉反馈，缺单字段重新生成能力，预览必须跳转 step5。本次重构 step4 交互：采纳后卡片折叠为「已应用到[字段]」状态，新增单字段重新生成（3 候选版本弹窗），step4 内增加就地预览面板（不必跳转）。

### 1. 组件改造
- **OptimizeCompare 增强**：[OptimizeCompare.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/OptimizeCompare.vue)
  - 采纳后卡片折叠为「已应用到 {{字段名}}」状态，显示优化后内容作为当前内容（替代原双栏对比，避免视觉割裂）
  - 每条建议新增「重新生成」按钮（emit `regenerate` 事件，调用方拉取 3 候选版本）
  - 已采纳态新增「撤销」按钮（取消采纳恢复对比视图）
  - 顶部新增「预览采纳结果」按钮（emit `preview` 事件，step4 内就地展开预览，不跳转 step5）
  - 头部增加字段定位标签（section + field，如「工作经历 #1 · 描述」）
- **新建弹窗组件**：[FieldRegenerateDialog.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/FieldRegenerateDialog.vue)
  - 展示 3 个候选版本卡片，单选高亮，底部「应用此版本」按钮
  - 顶部展示原文参照（用户重新生成时提醒「我要替换什么」）
  - 加载中 / 错误 / 空状态完整处理

### 2. 页面交互重构
- [ResumeOptimizePage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeOptimizePage.vue)
  - 新增 `quickPreviewVisible` 状态 + `toggleQuickPreview()`：step4 内就地预览面板，首次展开加载完整简历详情，复用 `applyOptimizes` 计算采纳后简历（采纳状态变化自动重算）
  - 新增 `regenerateItem(idx)`：调用 `aiFieldAssist` 拉取 3 候选版本（映射 section 到 work_description/project_description/self_intro/skills），弹窗展示供用户选择
  - 新增 `applyRegenCandidate(text)`：不可变替换 items[idx].optimized，触发 Vue 响应式更新，同步刷新就地预览
  - 底部按钮调整：原「预览最终结果」改为「完整预览」（次要按钮，跳 step5）+「保存优化结果」（主按钮，直接保存不强制跳转）

### 设计取舍
- **就地预览 vs 强制跳转 step5**：用户反馈预览跳转割裂，改为 step4 内折叠面板（采纳后实时反映），step5 完整预览作为可选次要入口
- **单字段重新生成 vs 整体重生成**：单字段成本更低（一次 aiFieldAssist 调用 ~3-5s），不影响其他已采纳项；整体重新生成会丢失所有采纳状态，不适合微调
- **3 候选版本弹窗 vs 直接替换**：3 版本给用户选择权，避免单次 AI 输出不稳定导致用户反复点重新生成
- **不可变替换 items**：Vue 3 响应式要求，直接修改 item.optimized 不会触发 computed 重算，必须重建数组
- **采纳后卡片折叠 vs 保留对比**：采纳后用户关注的是"当前内容是什么"而非"前后差异"，折叠态更聚焦；撤销采纳恢复对比视图

### 验证
- moyun-portal `npx vue-tsc -b`：类型检查通过（exit 0，0 个 TS 错误）
- 功能验证（部署后执行）：访问 `/interview/resume/optimize?resumeId=1` → 深度优化 → ① 采纳某项后卡片折叠为「已应用到[字段]」态 ② 点「重新生成」弹窗展示 3 候选版本 ③ 点「预览采纳结果」step4 内就地展开简历预览

---

## v11.6 (2026-09-01) 简历深度优化异步任务化：解决大模型调用超时（v10.19）

> 用户反馈 `/interview/resume/optimize?resumeId=1` 简历深度优化调用大模型频繁超时（dashscope compatible-mode timeout 60s），langchain4j 默认重试 3 次累计 180s 仍失败。根因：①默认 `ai_model_config.timeout=60s` 偏紧；②Prompt 直接序列化整个 `UserResumeVO`（含 id/version/status/时间戳等无关字段）输入 token 过大；③同步阻塞 HTTP 线程，关闭页面即失败。采用「异步任务 + 状态记录 + 调大超时」组合方案，支持关闭页面后回来查看、失败可重试。

### 1. 后端：异步任务化（独立执行器避开 Spring 自调用陷阱）

- **新建 Entity**：[PortalResumeOptimizeTask](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/domain/entity/PortalResumeOptimizeTask.java) + [Mapper](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/mapper/PortalResumeOptimizeTaskMapper.java)（字段：status/result\_json/error\_msg/start\_time/finish\_time）

- **新建 VO**：[ResumeOptimizeTaskVO](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/ResumeOptimizeTaskVO.java)（含 status/progress 0-100/result/errorMsg，前端轮询返回结构）

- **线程池**：[AsyncConfig](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/ai/config/AsyncConfig.java) 新增 `resumeOptimizeExecutor` Bean（core=2/max=4/queue=20，独立于知识库/工作流线程池）

- **异步执行器**：[ResumeOptimizeAsyncExecutor](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeOptimizeAsyncExecutor.java) 独立 Bean 承载 `@Async("resumeOptimizeExecutor") executeTask()`，避开 Spring AOP 自调用陷阱（同类内 this.executeTask() 不会触发代理，会导致异步失效退化为同步）

- **Service**：[ResumeDeepOptimizeService](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeDeepOptimizeService.java)

  - 新增 `submitTask()`：同步入库返回 taskId，立即触发异步执行

  - 新增 `getTaskStatus()`：前端轮询查询任务状态，含权限校验防越权

  - 新增 `buildResumeContext()`：精简 prompt 输入，仅传简历核心内容（姓名/求职意向/教育/工作/项目/技能/自我评价），去掉 id/version/status/时间戳等无关字段，降低输入 token 加快响应

- **Controller**：[PortalResumeOptimizeController](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalResumeOptimizeController.java) 新增 2 端点

  - `POST /portal/resume/optimize/deep/{resumeId}/{jobTargetId}/async`：提交异步任务，立即返回 taskId

  - `GET /portal/resume/optimize/deep/task/{taskId}`：查询任务状态（前端轮询）

  - 原 `POST /deep/{resumeId}/{jobTargetId}` 同步接口保留兼容（Swagger 标注「长耗时场景建议改用 async」）

### 2. 前端：提交任务 + 轮询 + 进度动画

- **API**：[resumeOptimize.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/resumeOptimize.ts) 新增 `submitDeepOptimizeTask` / `getDeepOptimizeTaskStatus`，导出 `ResumeOptimizeTaskVO` 类型

- **页面**：[ResumeOptimizePage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeOptimizePage.vue)

  - `generateOptimize` 改造为「提交任务 → 启动 4s 轮询 + 进度动画 → success 显示结果 / failed 显示错误」

  - 新增 `pollOptimizeTaskStatus`：4 秒轮询任务状态，取后端 progress 与前端动画较大值避免倒退

  - `onUnmounted` 钩子清理轮询定时器（防止组件卸载后内存泄漏）

  - 复用现有 `OptimizeProgress` 组件与 `progressPercent/progressStep` 状态

### 3. SQL 补丁（建表 + 调大模型超时）

- [20260901-moyun-resume-optimize-async-task.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/20260901-moyun-resume-optimize-async-task.sql)：

  - `CREATE TABLE IF NOT EXISTS portal_resume_optimize_task`（幂等）

  - `UPDATE ai_model_config SET timeout=180, max_tokens=4000 WHERE model_type='chat' AND enabled=1`（兜底，避免任务内部 60s 仍超时；条件过滤已调过的不重复改）

### 设计取舍

- **异步任务 vs 流式响应**：深度优化输出是结构化 JSON（3-6 项建议），流式拼接 JSON 增量解析复杂度高；异步任务天然支持关闭页面后回来查看、失败可重试、状态持久化，更适合长耗时结构化生成场景

- **独立 Bean 承载 @Async**：Spring AOP 通过代理生效，同类内 `this.executeTask()` 自调用不会触发代理导致退化为同步。抽到独立 Bean 是最干净的解法（替代方案 AopContext.currentProxy() 需开启 exposeProxy，self-injection 有循环依赖警告）

- **保留同步接口**：`POST /deep/{resumeId}/{jobTargetId}` 不删除只标注「兼容旧版」，避免影响外部调用方；新接口路径 `/async` 后缀明确区分

- **精简 prompt 而非调大 maxTokens 兜底**：精简输入是根治（降低 LLM 处理时间），调大 timeout 是兜底（防网络波动），双保险

- **进度粗粒度估算**：pending=10/running=50/success=100/failed=0，前端动画 + 后端返回取较大值，避免倒退视觉抖动

- **任务表不继承 BaseEntity**：表无 create\_by/update\_by 字段（参考 PortalResumeJobMatch 风格），按项目硬约束不继承

### 部署

1. 执行 SQL 补丁 `20260901-moyun-resume-optimize-async-task.sql`（幂等可重复执行）
2. 重启 moyun-server 后端
3. 前端 `npm run build:prod` 重新构建部署
4. （可选）后台「AI 模块 → 模型配置」可进一步调整具体模型的 timeout/max\_tokens（脚本已统一调整 chat 模型）

### 验证

- moyun-server `mvn compile -DskipTests -pl moyun-server -am -q`：编译成功（exit 0）

- moyun-portal `npx vue-tsc -b`：类型检查通过（exit 0，0 个 TS 错误）

- 功能验证（部署后执行）：访问 `http://localhost:3000/interview/resume/optimize?resumeId=1` 提交深度优化，观察网络面板：① 立即返回 taskId ② 每 4 秒轮询 /deep/task/{taskId} ③ success 时收到完整优化结果

***

## v11.5 (2026-08-31) CMS 文章+专栏管理合并：Tab 入口 + 批量加入专栏 + 维护文章弹窗 + 标签展示

> 用户反馈：①文章管理列表操作栏过宽、缺用户名搜索；②列表/详情未关联 portal\_tag 标签；③缺「批量加入专栏」能力；④文章管理与专栏管理分菜单割裂。统一收口为「文章管理 Tab 入口 + 专栏管理 Tab 复用 + 维护文章弹窗」的合并形态。

### 1. 文章列表优化（操作栏收窄 + 用户名搜索 + 标签展示）

- **后端**：

  - [CmsArticleQuery](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/query/CmsArticleQuery.java)：新增 `authorName` 查询字段（昵称/用户名 OR 模糊）

  - [CmsArticleVO](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/CmsArticleVO.java)：新增 `tagNames` 字段（逗号分隔字符串，由 Mapper 子查询 group\_concat 聚合）

  - [PortalArticleMapper.xml](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/mapper/portal/PortalArticleMapper.xml)：列表/详情 SQL 关联 `portal_entity_tag` + `portal_tag` 取标签名；新增 `authorName` 筛选条件（昵称 OR 用户名）

- **前端**（[ArticleListTab.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/article/components/ArticleListTab.vue)）：操作栏由 360px 收窄至 180px（去掉多余空白）；新增「用户名称」搜索项；新增「标签」列（最多展示 3 个 tag，超出折叠为 +N tooltip）；作者列展示「昵称 + 用户名」双行

### 2. 批量加入专栏

- **后端**：[CmsColumnController](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsColumnController.java) 新增 3 端点

  - `POST /cms/column/{id}/articles`：批量绑定文章（自动跳过已绑定，事务内同步 column.article\_count）

  - `GET /cms/column/{id}/articles`：分页查询专栏已绑定文章（含作者昵称/用户名）

  - `DELETE /cms/column/{id}/articles/{articleId}`：移出文章（同步 article\_count）

- **Service**：[ICmsColumnService](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ICmsColumnService.java) + Impl 新增 `batchBindArticles` / `selectColumnArticlesPage` / `removeColumnArticle`；批量插入时计算 max(sort\_order)+1 延续顺序

- **Mapper**：[PortalColumnArticleMapper](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/mapper/PortalColumnArticleMapper.java) + XML 新增 `selectColumnArticlesPage`，关联 `portal_article` + `portal_user` 取作者信息

- **VO**：[ArticleSimpleVO](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/ArticleSimpleVO.java) 新增 `userId` / `authorName` / `authorUsername` 三个非持久字段

- **前端 API**：[api/cms/column.js](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/api/cms/column.js) 新增 `listColumnArticles` / `bindColumnArticles` / `removeColumnArticle`

- **弹窗**（ArticleListTab.vue）：左侧展示已选文章（标题+作者昵称），右侧专栏列表支持「按作者昵称 + 专栏名」搜索 + 分页 + radio 单选；保存调 `bindColumnArticles` 批量绑定

### 3. 菜单合并（文章管理入口 + 专栏管理 Tab）

- **路由复用**（以文章为主）：[article/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/article/index.vue) 改造为 `TabContainer` 容器（borderless 变体），文章 Tab 为默认（ArticleListTab），专栏 Tab 复用 [cms/column/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/column/index.vue)

- **菜单隐藏**（[补丁 SQL](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/20260831-moyun-cms-article-column-merge.sql)）：`UPDATE sys_menu SET visible='1' WHERE menu_id=5122`（侧边栏隐藏但路由保留供 Tab 组件复用），`INSERT IGNORE sys_role_menu (1, 5122)` 兜底 admin 权限

### 4. 专栏管理 Tab 优化（用户名搜索 + 审核按钮条件显示 + 隐藏分类ID + 维护文章弹窗）

- **ColumnQuery + PortalColumnMapper.xml**：新增 `authorName` 字段 + SQL 关联 portal\_user.username 映射 authorUsername

- **审核按钮条件显示**：列表操作栏 `v-if="scope.row.status === 'pending'"` 才显示「审核」按钮（与文章一致；审核通过后自动隐藏）；CmsColumnServiceImpl.insertColumn/updateColumn 在 pending 态自动调用 `auditTaskService.submit` 写入 sys\_audit\_task，使首页/审核中心待办可见

- **修改弹窗**：隐藏「分类ID」表单项（注释保留）；「作者ID」旁新增「创建者」只读项（展示 authorName/authorUsername，详情未返回时回退到列表 row 数据）

- **维护文章弹窗**（column/index.vue）：操作栏新增「维护文章」按钮

  - 上半部分：el-descriptions 展示专栏基本信息（ID/名/作者/状态/文章数/订阅数）

  - 下半部分：已绑定文章列表（标题/作者/浏览/点赞/顺序/加入时间），支持标题搜索 + 分页 + 「移出」操作

  - 嵌套弹窗：「新增文章绑定」复用 listArticle 接口（可按标题+作者搜索+分页+多选），保存调 `bindColumnArticles`

### 设计取舍

- 复用文章菜单路径而非新增父菜单：用户明确选择「以文章为主」，避免菜单层级加深；专栏菜单隐藏但路由保留以最小代价支持 Tab 复用

- 维护文章弹窗的「新增文章绑定」复用 listArticle 接口而非新建端点：管理员可绑定任意已发布文章，无需新建查询；列表已自动过滤重复绑定（前端勾选 + 后端 batchBindArticles 二次校验）

- 批量加入专栏与维护文章弹窗都走相同的 `bindColumnArticles` 后端逻辑，保证幂等（已绑定的自动跳过）

- 标签展示取逗号分隔字符串而非 List：避免引入 CommaStringToListTypeHandler，前端 `split(',').filter(Boolean)` 简单处理即可

### 验证

- moyun-admin-vue `npm run build:prod`：构建成功，0 报错

- moyun-server `mvn compile -q -DskipTests`：编译成功，0 报错

- 需执行菜单补丁 [20260831-moyun-cms-article-column-merge.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/20260831-moyun-cms-article-column-merge.sql)（幂等：UPDATE visible + INSERT IGNORE role\_menu）+ 重启后端

***

## v11.4 (2026-08-31) 简历模块重构补丁：模板套用打通 + 子组件抽离 + 评分报告归档（设计文档 4 处偏差收口）

> 对照《简历编辑和优化模块重构设计-20260826.md》走查，收口 v10.13 落地时遗留的 4 处偏差：①模板套用仅预填标题（无结构化内容）②ResumeOptimizePage 内联 UI 未抽组件 ③缺跨页数据传递 ④评分无归档可追溯。

### 1. 模板套用打通（sampleData 结构化示例数据）

- **DDL 增量**（`202608201435-moyun-db-ddl-moyun-db.sql` 末尾）：`portal_interview_resume_template` 新增 `sample_data` 字段（TEXT，存 JSON 字符串，字段语义对齐 UserResumeVO：name/phone/email/avatar/jobIntention/educations/works/projects/skills/selfIntro）

- **后端**：`PortalInterviewResumeTemplate` 实体 + Mapper 增加 sampleData 字段，`GET /portal/interview/resume/{id}` 详情接口直传

- **前端 store**：[stores/resume.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/stores/resume.ts) Pinia store 承担跨页传递——`fillFromTemplate` 解析 sampleData 暂存，编辑页 `onMounted` 消费后 `clearTemplateSource`（避免刷新残留）

- **ResumeTemplatePage**：[useTemplate](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeTemplatePage.vue#L163-L190) 改为拉详情 → 填 store → 跳转带 `source=template` 标识；sampleData 为空时回退 query 预填标题/岗位

- **ResumeEditPage**：[applyTemplateQuery](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue#L1146-L1192) 优先消费 store 结构化字段（标量填空、列表覆盖），回退 query；顶栏新增「基于模板：xxx」徽章

### 2. 子组件抽离（阶段二，ResumeOptimizePage + ResumeEditPage 内联 UI 抽 6 个独立组件）

- [AIHelperDialog.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/AIHelperDialog.vue)：字段级 3 版本 AI 优化弹窗（纯展示+事件回传，调用方负责 openFieldAssist/adoptAssist/autoSave）

- [JobTargetForm.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/JobTargetForm.vue)：新建岗位目标弹窗

- [JobMatchPanel.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/JobMatchPanel.vue)：JD 匹配结果摘要（匹配度+四维+关键词）

- [OptimizeProgress.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/OptimizeProgress.vue)：5 阶段分析进度

- [OptimizeCompare.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/OptimizeCompare.vue)：深度优化前后对比+逐项采纳

- [ScoreReportDialog.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/ScoreReportDialog.vue)：评分报告+优化历史双列表（见下节）

- **接入**：ResumeOptimizePage 替换 5 处内联 UI 为组件标签；ResumeEditPage 内联 AI 实时辅助弹窗替换为 `<AIHelperDialog>`，保留原 adoptAssist/autoSaveAfterAdopt 逻辑

### 3. 评分报告归档（阶段五，独立可追溯）

- **DDL 增量**：`portal_resume_score_report` 新表（resume\_id/user\_id/job\_target\_id/position\_snapshot/score/score\_detail/source\[manual/optimize/template]/create\_time）

- **后端**：`PortalResumeScoreReport` 实体 + Mapper + Controller 端点

  - `POST /portal/resume/optimize/score-report`：`saveScoreReport`（source=manual 触发归档；source 非 manual 或带 jobTargetId 时后端先 scoreResume 再补全报告记录）

  - `GET /portal/resume/optimize/score-report/{resumeId}`：`getScoreReports` 按时间倒序

- **前端**：

  - [ResumeOptimizePage.rescore](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeOptimizePage.vue)：重新评分调 saveScoreReport(source=optimize) 并 refreshScoreReports

  - [ResumeEditPage.handleScore](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue#L389-L428)：评分成功后归档 saveScoreReport(source=manual)，失败不阻断主流程

  - **ScoreReportDialog**：评分报告列表（点击回显快照分数）+ 优化历史列表（点击跳转优化工作台）；ResumeActionBar 新增「评分报告」入口按钮

### 设计取舍

- store 仅承担"跨页面"数据传递，编辑页内表单状态仍由页面 ref 管理（避免过度中心化）；消费即清空，杜绝刷新残留

- 标量字段（姓名/电话/邮箱）仅填空避免覆盖个人中心真实信息；结构化列表（教育/工作/项目/技能）模板示例直接覆盖（模板套用即采用模板结构与示例表述）

- 评分报告归档失败静默（console.warn），不影响评分主流程可用性

### 验证

- moyun-portal `npx vue-tsc -b`：全量 0 编译错误

  - 附带修复 [utils/date.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/utils/date.ts) `formatDate`：补可选第 3 参 `dateOnlyFormat`（时间部分为 00:00:00 时改用日期型模板），根治 MyReportsPage/MyFeedbackPage/MyResumesPage 7 处 3 参调用历史遗留错误

- 需执行 DDL 增量补丁 [20260831-moyun-db-patch-resume-template-and-score-report.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/20260831-moyun-db-patch-resume-template-and-score-report.sql)（幂等：sample\_data 字段 + portal\_resume\_score\_report 表；主库 DDL 末尾亦已同步）+ 重启后端

***

## v11.3 (2026-09-02) 支付表统一 pay\_ 前缀 + 钱包页重构 + 支付通知并入消息中心

> 补丁：`moyun-pay-v11.3-refactor.patch`

### 1. 支付模块表统一 `pay_` 前缀（两处线上报错根治）

**报错根因**：v11.0 建表与 v11.3 迁移 SQL 已将表改为 `pay_` 前缀新结构，但实体类未同步，导致 `Unknown column 'id' in 'user_account'`（钱包总览）与 `Unknown column 'settle_no' in 'ledger_entry'`（后台流水汇总）。

- **表重命名**（存量库迁移脚本 `sql/20260902-moyun-pay-v11.3-rename.sql`）：

  - `user_account` → `pay_user_account`（主键改为 `user_id`，删除 `id`，无 `version`）

  - `ledger_entry` → `pay_ledger_entry`（删除 `settle_no` 列——单笔记账以 `pay_no`/`biz_no` 关联即可定位，结算批次概念收敛到订单维度）

  - `user_bank_card` → `pay_user_bank_card`（删除 `phone_masked`/`card_type`/`update_time` 冗余列）

  - `withdraw_order` → `pay_withdraw_order`（状态机简化为 `AUDITING`/`PAID`/`REJECTED`，新增 `fee` 手续费列）

  - 主建表脚本 `sql/20260902-moyun-pay-gateway.sql` 同步为最终结构

- **实体对齐**（4 个实体 + 3 个服务 + 2 个 Controller + Mapper）：

  - `UserAccount`：删 `id`，`userId` 改 `@TableId(type = INPUT)`（开户幂等天然依赖主键冲突回读）

  - `LedgerEntry`：删 `settleNo` 字段；`LedgerServiceImpl` 删 `generateSettleNo()` 全链及无用 import

  - `UserBankCard`：删 `phoneMasked`/`cardType`/`updateTime`；`BankCardServiceImpl` 与两个 Controller 的 safe map 同步清理（后台/门户均不下发已删字段）

  - `WithdrawOrder`：按新表结构整文件重写

  - `UserAccountMapper`：原子更新 SQL 表名同步 `pay_user_account`

- **前端对齐**：Admin 流水页删「结算单号」列；Admin 银行卡页删「手机号/卡类型」列；Portal `types/api.ts` 同步删除对应字段

### 2. 钱包页重构（宽度对齐 + UI 升级）

- **布局修复**：移除挤压宽度的多余嵌套 div，采用与首页/列表页一致的标准骨架（`Breadcrumb` 吸顶面包屑栏 + `max-w-7xl` 主容器）

- **UI 重设计**：

  - 余额总览卡：金额大字突出 + 累计收入/累计提现/待结算三指标卡

  - 流水列表：时间线式渲染，收入/支出方向色区分（credit 绿 / debit 灰），含摘要与余额快照

  - 银行卡：卡片式布局 + 默认卡标识 + 解绑确认，绑定表单收进 Drawer

  - 加载骨架屏 + 空状态 + 分页加载更多

### 3. 支付通知并入消息中心（独立页删除）

- **Portal** **`/messages`**：新增「支付通知」Tab（与互动/系统消息并列），复用 `payNotification` API 分页加载、单条已读、全部已读

- **头部统一提醒**：`messageStore` 新增 `payUnreadCount`（`loadAllUnread` 并行拉取，登出 `reset` 清零），Navbar 铃铛角标 = 互动 + 系统 + 支付通知合计（单一消息中心入口）

- **删除独立页**：`PayNotificationsPage.vue`、`/pay/notifications` 路由、Navbar 旧入口全部移除

### 验证

- Java 静态审查（引号/括号配平/重复方法/旧表名与已删字段零残留）通过

- Portal `vue-tsc` 类型检查通过；Portal/Admin `vite build` 双端构建通过

> 补丁：`moyun-pay-v11.2-fix.patch`

### 修复：打赏按钮不可见

- **文章详情页**：打赏按钮 `v-if="currentUser && !isArticleOwner"` 改为 `v-if="!isArticleOwner"`——未登录也显示入口，点击走 `requireAuth` 登录引导（登录后回跳原页面继续打赏），登录转化链路完整

- 专栏详情页核查：同条件已正确（未登录可见）

### 后台契约对齐（4 处 404/403 修复）

- **权限标识对齐**：4 个 CMS 支付 Controller 的 `@PreAuthorize` 由 `pay:order:*` 等补全 `cms:` 前缀，与 sys\_menu 按钮权限（`cms:payOrder:*`/`cms:payLedger:*`/`cms:payBankCard:*`/`cms:payConfig:*`）及前端 `v-hasPermi` 三方一致——修复非超管角色全部 403

- **银行卡路径**：前端 `/cms/pay/bankcard/list` → `/cms/pay/bank-card/list`（修复 404）

- **费率更新方法**：前端 PUT → POST（与后端 `@PostMapping` 对齐，修复 405）

- **SQL 按钮权限补齐**：新增 `cms:payLedger:query`（5315 分账明细）/ `cms:payLedger:summary`（5316 分账汇总）按钮及角色关联（修复流水明细/汇总 403）

### 后台数据展示修复（3 处空值）

- **订单详情抽屉**：详情接口返回 `{order, ledgerEntries}` 包装结构，页面直接读 `response.data` 导致全字段 undefined → 改为 `response.data.order`

- **分账明细元转换**：`CmsPayLedgerController.detail` 补 `fillYuan`（修复详情抽屉分账金额显示空）

- **流水汇总卡片**：`summary.totalCount` → `summary.totalEntries`（与后端返回字段一致）

### 支付配置页重写（对齐后端真实字段）

- 旧页面读取不存在的 `effectiveFeeRate`/`wechatMockEnabled` 等字段（会误显示"生产通道"且保存必失败）→ 按后端 `/cms/pay/config/view` 真实结构重写：`feeRateConfigValue`（sys\_config 运行时值）/`feeRateYamlFallback`（yaml 兜底值）/`wechatMockEnabled`（mock 开关，从通道配置读取）/`merchantConfigured`（商户参数是否已配置，布尔不下发密钥）

- 费率调整入参对齐：`{feeRate}`（0\~1 小数，后端校验）

### 安全加固

- `/portal/pay/status/{payNo}` 与 `/portal/pay/mock/{payNo}` 补登录校验（资金相关接口全部要求登录态，mock 仅 dev 环境可用的双保险不变）

### 验证

- Java 47 文件静态审查通过（pay + sms + 4 CMS Controller + tip 回调）

- Portal：vue-tsc 零错误 + vite build 通过

- Admin：vite build 通过

***

## v11.1 (2026-09-02) 支付闭环走查修复 + 短信验证码 + 旧订单模块清理

> 补丁：`moyun-pay-v11.1-cleanup.patch`
> SQL：`moyun-server/src/main/resources/sql/20260902-moyun-pay-v11.1-cleanup.sql`

### 修复：误删页面恢复

- 恢复 `moyun-admin-vue/src/views/cms/growth/log/index.vue`（上游提交误删该文件导致 growth-config 五 Tab 容器构建失败），成长日志查询功能回归

### 新增：短信验证码模块（真实 API 配置位 + 模拟实现）

- **模块** `com.moyun.core.sms`：`SmsSender` SPI（mock/阿里云双实现条件装配）

  - `MockSmsSender`：验证码写日志（联调可见），频控/存储/校验/防枚举全流程与真实通道一致

  - `AliyunSmsSender`：阿里云配置位（AccessKey/签名/模板）+ SDK 接入点 TODO 标注；配置不完整时明确拒绝发送，绝不伪装成功

- **服务** `SmsCodeServiceImpl`：企业级安全要点全量落地

  - 频控：60s 发送间隔 + 每日上限（Redis 计数，按自然日过期）

  - 一次性：验证通过立即删除，防重放

  - 防枚举：连续错误 5 次作废当前验证码

  - 手机号格式校验 + 日志脱敏（138\*\*\*\*5678）

- **接口** `POST /portal/sms/code/send|verify`：登录态 + scene 白名单（bankcard/member）

- **配置** `moyun.sms.*`（application-dev.yaml）：mock 开关/有效期/频控参数/阿里云配置位

- **接入银行卡绑定闭环**：`moyun.pay.security.bank-card-sms-verify=true`（默认开启）时绑定强制校验短信验证码；Portal 钱包页绑定表单新增验证码输入 + 60s 倒计时发送按钮（`api/sms.ts`）

### 会员支付架构预留（本期只留位，不实现）

- 支付通道天然支持：新业务（模拟面试会员/简历优化额度）实现 `PayCallbackHandler`（bizType=member）即可接入，网关/分账/通知零改动

- 短信 scene 白名单已含 `member`（会员开通短信核验预留）

- `WithdrawOrder` 状态机与提现链路完整保留，额度类商品（简历优化次数）可直接复用 pay\_order + ledger\_entry 复式记账

### 清理：删除旧通用订单模块（未被使用，与支付中心功能重复）

- **后端删 9 文件**：`PortalOrderController` / `IPortalOrderService` / `PortalOrderServiceImpl` / `PortalOrderMapper`(+XML) / `PortalOrder` 实体 / `OrderQuery` / `CmsOrderController` / `ICmsOrderService` / `CmsOrderServiceImpl`

- **Admin 删 3 处**：`views/cms/order/` / `views/cms/transaction/`（v9.5 隐藏菜单页）/ `api/cms/order.js`

- **SQL 清理**：删除 sys\_menu 旧交易管理菜单（5148）及按钮权限（5132/5133）+ 角色关联

- **误提交产物清理**：删除误入仓库的 `moyun-pay-gateway-v11.0.patch`

- 保留：PortalTipOrder 打赏全链、CreatorSettlement 结算链、PaymentStatus/PaymentChannel 枚举（支付中心在用）

- Portal 前端本就零调用旧接口，无涉及

### 验证

- Java 42 文件静态审查（pay + sms + tip 回调）全部通过

- Portal：vue-tsc 零错误 + vite build 通过

- Admin：删除页面后 vite build 通过（growth/log 恢复生效）

***

## v11.0 (2026-09-02) 微信支付公共通道 + 打赏分账体系

> 补丁：`moyun-pay-gateway-v11.0.patch`
> SQL：`moyun-server/src/main/resources/sql/20260902-moyun-pay-gateway.sql`

### 架构：公共支付通道（SPI 扩展）

- **通道层** `PayChannel` SPI：`prepay / query / verifyNotify / parseNotify / close` 五方法，新增渠道（支付宝/云闪付）实现接口即可，业务代码零改动

- **网关层** `PayGatewayImpl`：统一下单/查单/关单/回调分发，状态机 `CREATED → PAID → SETTLED / CLOSED`，幂等（同 bizType+bizNo 未支付单复用），`TransactionTemplate` 事务包裹「验签→落流水→标记PAID→业务分发→SETTLED推进」

- **业务层** `PayCallbackHandler` SPI：按 `bizType` 分发（本期实现 `tip` 打赏），业务方只关心支付成功事件

- **微信通道** `WechatPayChannel`：mock 模拟模式与真实模式同构（验签/回调/分账/通知全流程一致），`wechatpay-java` SDK 接入点 TODO 标注，商户参数（appId/mchId/merchantSerial/privateKeyPath/apiV3Key/notifyUrl）配置位完整保留

### 分账（业内成熟做法：复式记账）

- 每笔支付成功拆两条流水：**平台抽成**（PLATFORM/credit）+ **用户所得**（USER/credit），金额守恒校验（platform + user == total，偏差即抛异常回滚）

- 费率双轨：`sys_config("pay.platform.fee-rate")` 运行时可调 > `yaml(moyun.pay.platform-fee-rate)` 兜底（默认 10%）

- 金额全链路**分**（long 整型），展示层才转元（`amountYuan` transient 字段）

- 账户余额变动全部走原子 SQL（`balance >=` 条件更新 + 乐观锁），杜绝读改写竞态

### 安全（企业级红线）

- 银行卡号/手机号 AES-GCM 加密落库，任何接口只下发脱敏字段（Controller 层强制剥离密文）

- 回调 `@Anonymous` + 渠道验签，验签失败直接拒绝；原始报文落 `pay_notify_log` 审计留痕

- 商户密钥仅返回"已配置"布尔状态，值永不下发

- 金额区间校验（0.01\~10000 元）、防自我打赏、实名校验（`RealNameChecker`）、银行卡绑定数上限

### 后端文件（com.moyun.pay 模块 36 文件 + 打赏扩展）

- `channel/`：PayChannel + Request/Response/NotifyMessage + WechatPayChannel（mock + 真实 TODO）

- `gateway/`：IPayGateway / PayGatewayImpl / PayCallbackHandler

- `domain/entity/`：PayOrder（STATUS\_ 常量）、UserAccount、LedgerEntry、UserBankCard、WithdrawOrder、PayNotification、PayNotifyLog

- `mapper/`：7 Mapper（UserAccountMapper 含原子余额 SQL）

- `service/`：UserAccount / Ledger / BankCard / Notification 四服务

- `controller/`：PortalPayController（下单/状态轮询/账户总览/流水）、PayCallbackController（@Anonymous 回调）、PortalBankCardController（绑定/列表/删除/默认）、PortalPayNotificationController（通知列表/未读数/已读）

- 打赏接入：`PortalTipServiceImpl.createWechatTipOrder` + `TipPayCallbackHandler`（bizType=tip：打赏单 PAID → 分账 → 双方站内通知）+ `PortalTipController` 新端点 `POST /portal/tip/{targetType}/{targetId}/wechat`

- CMS 后台：`CmsPayOrderController`（分页/详情/手动关单）、`CmsPayLedgerController`（双视角流水/单笔明细/汇总）、`CmsPayBankCardController`（脱敏列表）、`CmsPayConfigController`（配置总览/费率调整写 sys\_config）

- `application-dev.yaml`：`moyun.pay` 配置段（enabled/orderExpireMinutes/platformFeeRate/security/wechat）

### 前端

**Portal（Vue3 + TS + Pinia）**：

- `api/pay.ts`：下单/状态/账户/流水/银行卡/通知全套接口

- `pages/pay/PayCashierPage.vue`：收银台（qrcode 本地渲染二维码 + 3s 轮询 + mock 模拟支付按钮 + 支付成功动画）

- `pages/pay/WalletPage.vue`：我的钱包（余额总览/流水分页/银行卡管理）

- `pages/pay/PayNotificationsPage.vue`：支付通知中心（未读数/已读）

- `TipModal.vue`：积分/微信双模式打赏（快捷金额元、0.01\~10000 校验、跳转收银台）

- Navbar 注入「我的钱包 / 支付通知」登录态入口；路由 `/pay/cashier` `/pay/wallet` `/pay/notifications`（requiresAuth）

- 依赖：+ `qrcode` `@types/qrcode`

**Admin（Element Plus）**：

- `api/cms/pay.js` + 四页面：`pay/order`（状态筛选/详情抽屉含分账明细/手动关单）、`pay/ledger`（平台/用户双视角 + 汇总卡片）、`pay/bankcard`（脱敏列表）、`pay/config`（配置总览/费率调整）

- 权限标识：`cms:payOrder:query|close`、`cms:payConfig:edit`（菜单 SQL 已含 sys\_menu）

### 验证

- Java 36 文件静态审查（引号奇偶/括号配平/重复方法/类名一致）全部通过

- Portal `vue-tsc` 零错误；`vite build` 通过（NODE\_OPTIONS 堆内存调优）

- Admin `vite build` 通过

- 数据库 7 张新表 + sys\_config 费率 + sys\_menu 后台菜单 SQL 交付于 `sql/20260902-moyun-pay-gateway.sql`

***

## v10.18 (2026-09-01) AI 面试官 LLM 驱动动态追问体系

> 补丁：`moyun-llm-followup-v10.4.patch`（交付轮次编号 V10.4，下同）

**后端** **`VoiceInterviewServiceImpl.java`**：

- 新增 LLM 分析协议 `analyzeAnswerByLlm`：一次调用同时产出评分校正、漏洞识别（≤3条）、水平评估（junior/mid/senior）、针对性追问建议（必须引用候选人原话）、引导提示；LLM 不可用/失败时全链路回退规则评分，可用性永不中断

- LLM 分与规则分按 0.7/0.3 加权融合，避免单边极端

- 上下文系统提示词 `buildContextualSystemPrompt`：岗位+难度+简历项目摘要+累积薄弱点+风格，替代原单句提示词

- start 时 `buildResumeDigest` 提取简历项目摘要（≤3个：项目名/技术栈/亮点）写入 configJson，作为追问上下文底座

- 动态追问决策：LLM followupWorth 优先 + 追问总预算 `FOLLOWUP_BUDGET`（4次）+ 链深限制 `FOLLOWUP_MAX_DEPTH`（2层防死循环），预算耗尽自动降级推进

- 追问问题 `generateFollowupQuestion`：优先 LLM 针对漏洞生成（引用候选人原话），播报话术先点明漏洞再追问；规则模板保留兜底

- 水平画像累积 `accumulateProfile`：每轮漏洞写回 configJson.profileGaps（≤8条），驱动后续追问覆盖验证

- finish 报告增强：薄弱点并入画像真实漏洞；总评写入水平画像（junior→初级/mid→中级/senior→高级）

- SSE data 事件新增 `guidance` 字段：回答跑偏时下发引导提示

**前端**：

- `VoiceInterviewPage.vue`：guidance 以「引导」标签气泡展示并语音播报，引导用户回答而非直接判死

- `api/voiceInterview.ts`：onData 类型扩展 guidance

## v10.17 (2026-09-01) 简历→语音面试全链路打通 + 历史面试留存

> 补丁：`moyun-voice-link-v10.3.patch`

**修复 4 个断链**：

- 假简历上传（仅改 UI 状态+硬编码假数据）→ 真实简历库选择器：进页拉取简历列表（名称/评分/更新时间/状态徽章）单选，空库引导维护，登录门禁，未选禁开

- startSession payload 缺 resumeId 致后端简历深挖出题路死代码 → payload 接通，出题配比「简历项目 2 题+画像 2 题+兜底 1 题」生效

- 目标岗位硬编码 3 项 → 从所选简历求职意向自动带出+快捷选项+自定义任意岗位；报告头部回显岗位/风格/难度/时间元信息

- 历史面试零入口（后端接口存在但无页面消费）→ 新建 `MyVoiceInterviewsPage.vue`（评分环形/岗位/题数/时长/删除/分页），路由 `/interview/voice/history`，「我的答题记录」页导流卡，报告页顶部返回链，报告新增第 4 Tab「对话回放」完整回看 QA 问答流

**部署归档**：`20260901-moyun-voice-interview-menu.sql`（sys\_menu 语音面试管理菜单+4 按钮权限，修复新环境 Admin 空白页）

## v10.16 (2026-08-31) 编程题接入真实 OJ 判题（题库练习链路收口）

> 补丁：`moyun-oj-v10.2.patch`

**后端** **`ProcessJudgeEngine.java`（防作弊加固）**：首败即停逻辑中隐藏用例的 input/expected/actual 明文下发 → 仅样例用例回填，隐藏用例只下发状态码（与选择题答案剥离同标准）

**前端** **`CodingPracticePage.vue`** **完全重写（655 行）**：

- 关键修复：判题引擎为 stdin/stdout ACM 模式，旧页面 LeetCode 函数式模板必然 WA → 新模板 7 语言全覆盖（JS/TS/Python/Java/Go/C++/Rust）均为完整可跑的 ACM 解法

- 真实判题接入：运行/提交均走服务端权威评测（POST /portal/judge/submit，dev 同步模式），提交入历史并跳转记录 Tab

- 用例明细：样例失败展示输入/期望/实际（后端回填+前端 orderNum 匹配），隐藏用例仅状态+耗时

- 判题状态元数据 AC/WA/TLE/MLE/RE/CE/SE/PENDING 中文标签配色；多语言代码槽；Ctrl+Enter 提交

**至此题库练习四大断链全部修复**（判分形同虚设/答案明文下发/选择题纯本地/编程题纯前端模拟）。

## v10.15 (2026-08-31) AI 语音面试对话可视化增强（对标 HireVue/面试鸭AI）

> 补丁：`moyun-voice-ui-v10.1.patch`

- 新增 `useAudioLevel.ts`：getUserMedia+AudioContext+AnalyserNode 频域分析，10 柱人声主频段分桶（85Hz\~3.8kHz），attack/release 平滑，全量资源清理（stop tracks/关闭 ctx/取消 RAF）

- VoiceInterviewPage 六项增强：聆听实时音浪条（watch(listening) 统一驱动覆盖全部路径）、四态状态环（聆听红/分析黄/播报蓝/空闲绿）、AI 头像播报声波动画、顶栏静音切换、停止播报按钮、播报结束自动开麦（对话节奏闭环，setup 可关）

- 前置轮次（V10.0 语音 MVP）已含：ASR 实时转写边说边打印、SSE 流式打字机渲染、TTS 队列播报、用户说话自动打断 TTS

## v10.14 (2026-08-26) 简历编辑页 AI 实时辅助编辑（设计文档 P0 需求#2）+ 编译错误修复

### 需求

按《简历编辑和优化模块重构设计-20260826.md》P0 需求#2：编辑简历时字段级 AI 优化建议。工作/项目经历描述、自我评价旁新增「✨AI优化」按钮 → 弹窗展示原文 + 3 个差异化版本（成果量化/技术深度/业务价值，STAR 法则+量化占位符）→ 一键采纳替换并联动自动保存。

### 后端

- [ResumeDeepOptimizeService.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeDeepOptimizeService.java)：新增 `fieldAssist(field, originalText, position, skillNames)`，按字段类型差异化提示词，返回 3 版建议（text+reason）；AI 未启用/原文为空抛明确提示

- [PortalResumeOptimizeController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalResumeOptimizeController.java)：新增 `POST /portal/resume/optimize/ai-assist`

- 深度优化输出截断修复（JsonEOFException）：提示词不再要求 LLM 回显 original（原文由 `fillOriginal` 服务端按 section/index/field 回填），items 限 3-6 项、optimized 限 50-200 字、reason 限 30 字；JSON 解析失败时给出可操作提示（引导调大模型 maxTokens）

- 编译修复：`objectMapper.createArray()` → `createArrayNode()`（ResumeDeepOptimizeService/ResumeJobMatchService 共 6 处）；`Arrays.stream(boolean[])` 不存在 → 改为循环计数（ResumeJobMatchService 结构完整度）

### 前端（moyun-portal）

- [resumeOptimize.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/resumeOptimize.ts)：新增 `aiFieldAssist` + `FieldAssistSuggestion` 类型

- [ResumeEditPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue)：

  - 工作经历/项目经历描述、自我评价区块旁 ✨AI优化 按钮（紫色胶囊样式，对比度达标）

  - 辅助弹窗：原文对比基准（限高滚动）+ 3 版本卡片（版本标签+优化理由+采纳按钮）+ 空态/加载态 + 占位符提示（\[X%]/\[X万] 需替换真实数值）

  - `adoptAssist` 采纳后 `autoSaveAfterAdopt` 联动静默保存（有 id 时）；评分进度条修复为 `:style` 动态绑定

- [ResumeActionBar.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/ResumeActionBar.vue)：saveStatus 扩展 `dirty` 状态（"有未保存修改"提示）

### 验证

- moyun-server mvn compile ✓ / moyun-portal vite build ✓

- 无新增 SQL；需重启后端生效

***

## v10.13 (2026-08-26) 简历优化重构：岗位匹配评分 + 深度优化前后对比闭环（5步工作台）

### 需求

按《简历编辑和优化模块重构设计-20260826.md》重构简历优化链路：岗位目标管理 → JD 匹配评分 → AI 逐项优化（前后对比采纳）→ 预览（完整度）→ 保存 → 重新评分/匹配。参考熊猫简历预览页的完整度百分比 + 待核对清单设计。无版本概念：优化直接更新原简历（幂等），优化历史快照可追溯。

### 数据库（DDL 增量，202608201435-moyun-db-ddl-moyun-db.sql 末尾）

- `portal_resume_job_target`：岗位目标（position/jdText 核心，jd\_keywords 冗余，is\_default）

- `portal_resume_job_match`：匹配报告存档（match\_score/grade/matched+missing keywords/dimensions JSON 四维/ai\_powered）

- `portal_resume_optimize_history`：优化历史（score/match 前后对比 + optimize\_data 全量建议快照含采纳状态）

### 后端（新增）

- 实体+Mapper：PortalResumeJobTarget / PortalResumeJobMatch / PortalResumeOptimizeHistory（dimensions·optimize\_data 走 JacksonTypeHandler，autoResultMap）

- [LlmJsonExtractor.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/LlmJsonExtractor.java)：LLM 返回 JSON 提取统一工具（剥围栏+截取{}本体）

- [ResumeJobMatchService.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeJobMatchService.java)：LLM 四维匹配分析（关键词/经验/技能/结构）+ 规则兜底（JD 关键词表+结构完整度 6:4 加权）；报告入库可追溯

- [ResumeDeepOptimizeService.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeDeepOptimizeService.java)：LLM 逐项优化建议（section+index+field 定位，original/optimized/reason）；采纳→应用到简历→saveResume 幂等更新（无版本概念）；skills 分级文本解析合并去重；LLM 幻觉 section 防御；优化历史快照记录

- [PortalResumeOptimizeController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalResumeOptimizeController.java)：岗位 CRUD（/portal/resume/optimize/job-target）+ 匹配（match/{resumeId}/{jobTargetId}）+ 深度优化（deep 生成/apply 采纳）+ 历史查询；全部校验归属

### 前端（moyun-portal）

- [resumeOptimize.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/resumeOptimize.ts) + types/api.ts：岗位目标/匹配报告/深度优化 VO 类型与 API

- [ResumeOptimizePage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeOptimizePage.vue)（路由 /interview/resume/optimize）：5 步向导

  - STEP1 岗位：卡片选择+新建弹窗（JD 必填）+删除+默认标识

  - STEP2 简历：网格选择（带版本/评分/更新时间），支持 ?resumeId= 直达

  - STEP3 分析：进度弹窗（5 阶段动画+真实接口）

  - STEP4 对比：匹配度大数字+四维进度条+已匹配/缺失关键词标签+summary；深度优化建议卡片（优化前/后双栏+理由+逐项采纳/全部采纳）

  - STEP5 预览：简历完整度%（9 项核对清单，参考熊猫简历）+ 最终简历预览（前端预演应用采纳项）+ 保存优化结果（幂等更新原简历）+ 重新匹配（前后匹配度对比展示）+ 重新评分 + 去微调

- 入口：MyResumesPage 卡片"岗位优化"按钮；ResumeEditPage 顶部紫色引导条（有 id 时）；ResumeTemplatePage"基于此模板创建简历"（fromTemplate query 预填标题/期望岗位，跳过最新简历反显）

### 设计取舍

- 无版本概念（用户决策）：优化直接更新原简历，幂等；优化历史表保留前后快照满足可追溯

- 深度优化无规则兜底：文本改写必须 LLM，未配置 AI 时明确提示；匹配分析保留规则兜底（关键词命中可无 LLM 计算）

- 模板无结构化内容字段：模板套用降级为预填标题+岗位，模板文件可下载参考（结构化模板内容列为后续扩展）

### 验证

- moyun-server mvn compile ✓ / moyun-portal vite build ✓

- 需执行 DDL 三张新表 + 重启后端

***

## v10.12 (2026-08-25) 简历附件上传实装：单文件上传 + 解析覆盖填充到在线简历

### 需求

`/interview/resume/edit` 上传区"点击或拖拽上传"无反应（原为占位符）。需要：单独文件上传作为附件简历；可解析附件并按字段语义映射覆盖填充到在线文档。

### 后端（新增）

- [ResumeParseVO.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/ResumeParseVO.java)：解析结果 VO，字段语义对齐 UserResumeVO（birthDate 用 String 承载避免不完整日期反序列化失败）

- [ResumeParseService.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeParseService.java)：

  - 文本抽取：PDF（PDFBox 3.x Loader）/ DOCX（POI XWPF）/ DOC（POI HWPF）/ TXT·MD（UTF-8→GBK 探测）；≤10MB；超长截断 12000 字符

  - LLM 结构化解析（复用简历建议的 AI 开关）：prompt 限定字段结构 + "只抽取原文明确存在信息禁止编造"；markdown 围栏剥离（同 v10.11 extractJson 策略）

  - 规则粗解析兜底（AI 未启用/失败）：正则抽取邮箱/电话/出生日期/姓名（2-4 汉字行）/常见技术栈关键词

  - 归一化 normalize()：日期→yyyy-MM-dd（缺日补 01）、性别"男性"→"男"、薪资非正值/空串→null（前端 null 保留原表单值）

- [PortalUserResumeController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalUserResumeController.java)：新增 `POST /portal/interview/resume/user/parse`（multipart，需登录）

### 前端（ResumeEditPage.vue + api/interview\.ts + types/api.ts）

- **上传区实装**：隐藏 input（点击）+ dragover/drop（拖拽，高亮反馈）；类型限制 pdf/doc/docx/txt/md + 10MB 校验；上传走统一 `/portal/file/upload`，fileUrl 持久化到简历（保存时提交）

- **附件卡片**：文件名 + 大小 + 「解析并填充」+「预览」（新窗口）+「移除」；编辑已有简历时历史附件回显（本地 File 不可恢复，标注"重新上传后可解析"）

- **解析确认弹窗**：识别摘要表（姓名/性别/生日/电话/邮箱/求职意向/教育/工作/项目/技能条数）+ AI/规则解析标识 + 覆盖规则说明；确认后填充，不自动保存（dirty 状态提醒手动保存）

- **字段语义映射覆盖**：标量字段非空覆盖；jobIntention 子字段逐个覆盖；educations/works/projects/skills 非空数组整体替换；解析为空的字段保留原值

### 验证

- moyun-server mvn compile ✓ / moyun-portal vite build ✓

### 需要操作

1. 重启后端（新增 parse 端点与服务）
2. 验证：上传 PDF/Word → 附件卡片出现 → 解析并填充 → 确认弹窗摘要 → 确认后表单覆盖 → 检查保存
3. AI 未启用时走规则粗解析（仅基础字段）；启用 moyun.ai 后为完整结构化解析

***

## v10.11 (2026-08-25) 简历 AI 建议重构：对齐原型（模块 Tab + 采纳自动填充字段）；LLM 返回 markdown 包裹修复

### 需求

1. 简历 AI 改进建议页面对照原型 `docs/08-原型设计/ai_interview_system/resume_optimizer_page.html` 重构
2. 建议按模块 Tab 分组展示，采纳按钮自动填充到对应表单字段（注意字段映射）

### LLM JSON 解析修复（前置问题）

- 报错 `Unexpected character ('`' (code 96))\`：LLM 返回被 markdown 代码块包裹

- [ResumeAiAdviceService.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/ResumeAiAdviceService.java) 新增 `extractJson()`：剥离 \`\`\` 围栏 + 截取首尾大括号（兼容前后说明文字）；system prompt 追加"禁止 markdown 包裹"双保险

### 后端变更

- [ResumeAiAdviceVO.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/ResumeAiAdviceVO.java)：AdviceItem 新增 `optimized` 字段（AI 优化结果，可直接采纳的文本；与 content"为什么改"区分）

- LLM prompt：要求每条建议返回 optimized（基于现有简历改写，无依据量化用 \[X%] 占位符），dimension 取值限定 8 个维度

- 规则化兜底：`buildDimensionOptimized()` 按维度生成结构模板（教育/工作/项目/技能/自评）；基本信息/求职意向为结构化字段返回 null（前端引导手动完善）；岗位匹配 optimized="了解：缺失技能列表"

### 前端变更（ResumeEditPage.vue + types/api.ts）

- **评分总览**（对齐原型 analysis-overview）：大号评分数字 + 等级描述 + summary + AI 生成标识 + 各维度评分进度条（scoreDetail，颜色按得分率 4 级分色）

- **模块 Tab 栏**：全部 / 基本信息 / 求职意向 / 教育背景 / 工作经历 / 项目经历 / 专业技能 / 自我评价 / 岗位匹配（仅显示有建议的维度，固定顺序）

- **建议卡片**（对齐原型 a-card）：卡头（模块名+维度得分徽章+优先级+类型）→"优化建议"黄色反馈块（content）→"AI 优化结果"绿色 diff 块（optimized，含占位符提示）→ 采纳按钮

- **采纳字段映射**（核心）：

  | dimension    | 填充目标                                 |
  | ------------ | ------------------------------------ |
  | 自我介绍         | form.selfIntro（整体替换）                 |
  | 教育经历         | educations\[0].description（无记录引导先添加） |
  | 工作经历         | works\[0].description（同上）            |
  | 项目经历         | projects\[0].description（同上）         |
  | 技能列表 / 岗位匹配度 | 解析"精通：A、B"分级文本追加 skills 条目（去重）       |
  | 基本信息 / 求职意向  | 结构化字段，关闭弹窗滚动到对应锚点手动完善                |

- **缺失技能**：新增"一键加入技能清单"按钮（level=了解 避免虚标）

- 采纳状态 key 由列表索引改为 dimension+内容（Tab 过滤后索引不稳定）

### 验证

- moyun-server mvn compile ✓ / moyun-portal vite build ✓

### 需要操作

1. 重启后端（LLM prompt 与 VO 变更）
2. 简历编辑页 → 评分 → AI 建议：确认评分总览/Tab/卡片/优化结果块展示；采纳各模块建议验证字段填充；无记录时引导提示

***

## v10.10 (2026-08-25) 实名策略分层重构：发布开放+敏感场景强制实名；面经草稿/发布链路修复；简历编辑页增强

### 需求

1. `/interview/my/experiences` 保存草稿后列表消失，链路断裂
2. 面经/文章发布不应强制实名（"这样谁还敢发"），未实名可发布仅提示可跳过；打赏、积分消费等敏感场景才强制实名
3. 各模块草稿列表 + 重新编辑 + 审核状态查看需连贯（参考文章模块）
4. 简历编辑页：到岗时间改字典下拉（可存文本）；已有简历反显最新版本续编

### 根因与修复

**面经草稿消失（核心 BUG）**

- 根因：`selectMyExperienceList` 复用 `selectExperiencePage`，后者对 null status 默认只查 `published`，把草稿/待审核全部过滤

- 修复：改为独立查询（默认全状态 + 支持状态筛选）

- 连带修复①：公开列表关键词搜索 `like(title).or().like(content)` 打断 status 过滤，草稿可泄露到公开搜索 → `and()` 包裹

- 连带修复②：草稿/被拒面经通过编辑"提交发布"时不创建审核任务（永远停在 pending 且审核中心不可见）→ 补齐 submitAuditTask + 成长事件 + Feed 动态（已 pending 的编辑不重复提交）

**实名策略分层（前后端对齐）**

- 发布文章/面经/创建专栏：后端移除 `CreatorPermissionChecker.checkCreator` 拦截（类已删除）；前端 `promptRealNameOptional()` 弹窗提示可跳过

- 打赏/积分兑换：新建 [RealNameChecker.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/util/RealNameChecker.java) 强制校验（certType=identity 且 approved）；`PortalTipServiceImpl`、`ShopServiceImpl.exchange` 接入；前端 TipModal 调 `requireRealName()` 预检

- 实名判定：认证表 identity+approved；前端接口失败降级 isCertifiedCreator 兜底

### 变更清单

**后端**

- [PortalInterviewServiceImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/impl/PortalInterviewServiceImpl.java)：selectMyExperienceList 独立实现；selectExperiencePage 关键词 and() 包裹；insert/updateExperience 移除认证拦截；updateExperience 补齐发布链路

- [PortalArticleServiceImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/service/impl/PortalArticleServiceImpl.java)：publishArticle / updatePortalArticle 移除认证拦截

- [ColumnServiceImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/impl/ColumnServiceImpl.java)：saveColumn 新建分支移除认证拦截

- [InterviewExperienceVO.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/InterviewExperienceVO.java)：新增 auditRemark（驳回原因回显）

- 删除 CreatorPermissionChecker.java（无调用方）

**前端（moyun-portal）**

- [creatorPermission.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/utils/creatorPermission.ts)：重写为 `promptRealNameOptional()`（提示可跳过）+ `requireRealName()`（强制）

- PublishPage / ExperiencePublishPage / ColumnEditPage：requireCreator → promptRealNameOptional

- [TipModal.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/TipModal.vue)：打赏前 requireRealName 预检

- [MyExperiencesPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/MyExperiencesPage.vue)：状态筛选 Tab（全部/草稿/待审核/已发布/已驳回，支持 ?status= 直达）+ 驳回原因展示

- ExperiencePublishPage：保存/发布后跳转带 `?status=` 定位对应 Tab

- [ResumeEditPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue)：

  - 到岗时间：文本输入 → `portal_available_time` 字典下拉（字典未配置时本地默认兜底；历史存量值动态补入选项；值直接存文本）

  - 反显：无 :id 进入时若已有简历，取列表第一份（updateTime 倒序=最新）拉详情续编（form.id 带上，保存即更新不重复建）；无简历才走个人中心预填

- 认证相关文案更新（certification.ts CERT\_TYPE\_OPTIONS、UserPage 认证入口描述）

**SQL（202608201435-moyun-db-dml.sql）**

- 新增字典：sys\_dict\_type(130, portal\_available\_time) + sys\_dict\_data(128-133，6 项中文文本值)

### 验证

- moyun-server mvn compile ✓ / moyun-portal vite build ✓

### 需要操作

1. 执行 DML：portal\_available\_time 字典 7 条 INSERT
2. 重启后端
3. 验证链路：发布面经存草稿 → 列表"草稿"Tab 可见 → 编辑 → 提交发布 → "待审核"Tab + 审核中心可见 → 审核通过/驳回 → 状态流转 + 驳回原因展示
4. 未实名发布文章/面经：弹窗提示可跳过；未实名打赏：拦截并引导认证
5. /interview/resume/edit：到岗时间下拉可选；已有简历进入自动反显最新一版

***

## v10.9 (2026-08-25) 创作者认证实名合规改造：证件号加密存储 + 脱敏展示 + 数据联动

### 需求

1. 简历编辑页生日反显出现非法格式（如 `1995-0705`）导致 `<input type="date">` 报错、保存接口 500（用户确认脏数据自行处理，系统不做归一化）
2. 创作者认证（实名制）改造：实名后保留姓名/性别/身份证号等数据，按业内合规做法（个保法最小必要 + 敏感信息加密）设计存储与展示，并使简历、个人中心数据联动连贯；预留后期真实实名核验接口接入

### 方案设计（业内合规基线）

- **加密存储**：证件号只存 AES-GCM 密文（`cert_no_enc`，格式 `enc:v1:iv:cipher`），明文不落库；原 `cert_no` 字段仅兼容存量数据，新写入置 NULL

- **脱敏展示**：所有查询/详情/审核接口统一返回脱敏值（`110***********1234`），存量明文运行时脱敏兼容

- **信息推导**：由身份证号推导性别（`derived_gender`）与出生日期（`derived_birth`），存认证表供审核/风控使用，不回填 portal\_user 公开资料（隐私边界）

- **核验渠道抽象**：`RealNameVerifier` 接口 + manual（人工审核）默认实现，后期接入阿里云/腾讯云实名 API 只需新增实现类

### 变更清单

**数据库（202608201435-moyun-db-ddl-moyun-db.sql）**

- `portal_creator_certification` 增量 ALTER：`cert_no_enc` / `cert_no_mask` / `derived_gender` / `derived_birth` / `verify_channel`（默认 manual）/ `verify_serial`（预留）

**后端（moyun-server）**

- 新建 [AesGcmUtils.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/util/crypto/AesGcmUtils.java)：AES-GCM 加解密（随机 IV + 128 位 Tag，口令 SHA-256 派生密钥），支持密文格式识别与存量明文兼容

- 新建 [CertSecurityProperties.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/config/CertSecurityProperties.java)：`moyun.security.cert-no-encrypt-key` 加密口令配置

- 新建 realname 包：[RealNameVerifier](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/service/realname/RealNameVerifier.java) 接口 / [RealNameVerifyResult](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/service/realname/RealNameVerifyResult.java) / [ManualRealNameVerifier](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/service/realname/ManualRealNameVerifier.java)

- [IdCardUtil.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/util/string/IdCardUtil.java)：新增 `mask()` 脱敏方法（前 3 后 4）

- [PortalCreatorCertification.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/domain/entity/PortalCreatorCertification.java)：新增加密/脱敏/推导/核验字段，`certNoEnc` 加 `@JsonIgnore` 防密文外泄

- [PortalCreatorCertificationServiceImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/service/impl/PortalCreatorCertificationServiceImpl.java)：

  - apply()：证件号加密 + 脱敏值 + 身份证推导性别生日 + 核验渠道落库，明文 cert\_no 不再写入

  - 统一脱敏：getMy()/getById()/list()/audit() 出口全部走 applyMasking()（新数据用 cert\_no\_mask，存量明文运行时脱敏，密文置空）

- [CmsCreatorCertificationController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/CmsCreatorCertificationController.java)：列表/详情返回新增 derivedGender/derivedBirth/verifyChannel/certImageFront/Back 字段（证件号为脱敏值）

- [CertificationAuditBizHandler.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/system/service/handler/CertificationAuditBizHandler.java)：审核中心详情同步脱敏 + 补充推导/核验/双面照片字段

- application-dev.yaml：新增 `moyun.security.cert-no-encrypt-key`（生产环境须环境变量注入）

**前台（moyun-portal）**

- [api/certification.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/certification.ts)：类型对齐（certNo 脱敏语义注释 + derivedGender/derivedBirth/verifyChannel）

- [CreatorCertificationPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/CreatorCertificationPage.vue)：

  - 证件号输入区新增隐私安全提示（AES-GCM 加密存储说明）

  - 已认证信息区：证件号带盾牌图标（加密标识）+ 性别/出生日期推导展示 + 核验渠道与注销指引说明

- [UserPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/UserPage.vue)：个人中心头部与认证入口新增"已实名 {脱敏姓名}"蓝色徽标（张\* 格式），挂载时并行加载认证记录

- [ResumeEditPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue)：姓名字段新增"使用实名姓名"一键填充按钮（已实名用户专属；主动选择不自动回填，性别/生日仅空值时补全）

### 数据联动设计

- 实名数据仅存认证表（密文+脱敏+推导），不回填 portal\_user 公开资料 → 隐私边界清晰

- 简历页：实名姓名由用户主动一键填充（非自动），满足求职场景真实性需求又不越界

- 个人中心：实名徽标 + 脱敏姓名，与创作者认证徽标（绿色）区分（蓝色）

- 后台审核：全链路脱敏展示，审核员比对身份证照片保障真实性

### 验证

- moyun-server mvn compile ✓ / moyun-portal npm run build ✓

### 需要操作

1. 执行 DDL：portal\_creator\_certification 的 6 个增量 ALTER（202608201435-moyun-db-ddl-moyun-db.sql 末尾认证模块段）
2. 重启后端（加载新实体字段与配置）
3. 前台 /creator/certification 提交身份认证 → 查库确认 cert\_no 为 NULL、cert\_no\_enc 有密文、cert\_no\_mask/derived\_\* 有值
4. 后台认证列表/详情、审核中心确认证件号只显示脱敏值
5. 前台个人中心确认"已实名"徽标与脱敏姓名；简历新建页测试"使用实名姓名"按钮
6. 存量明文数据无需处理（运行时自动脱敏）；如需彻底清理可后续提供迁移脚本

***

## v10.8 (2026-08-25) 门户 AI 内容分析统一接口 + 发布页摘要/SEO 智能提取

### 需求

发布页（/publish）"摘要"与"SEO 设置"字段支持一键 AI 智能提取并自动填充；AI 接口做成统一的内容分析入口，供多处复用。

### 方案设计

- **统一接口**：`POST /portal/ai/analyze` 单端点 + 场景（scene）注册表。新增分析能力（标签提取、分类建议、评论摘要等）只需注册一个 SceneHandler（提示词模板 + 输出解析 + 本地兜底），无需新增接口。

- **不引入前端 NLP 框架**：摘要截取已有本地实现（excerpt.ts）作兜底；关键词/SEO 描述属语义任务，复用后端 AI 模块 LLMService（默认聊天模型）最优。

- **兜底策略**：AI 未配置/失败/解析失败 → 本地兜底（摘要取正文前 160 字、SEO 标题用文章标题），返回 source=fallback 标识，按钮始终可用。

### 变更清单

**后端（moyun-server）**

- 新建 [PortalAiController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalAiController.java)：

  - 统一入口 `POST /portal/ai/analyze`（需登录，Token 消耗型接口不放公开）

  - 场景 `article-meta`：摘要/SEO标题/SEO描述/关键词（四行结构化输出 + 正则解析）

  - 场景 `tags`：内容标签提取（3\~8 个，逗号分隔）

  - 通用能力：HTML/Markdown 统一转纯文本、正文截断 3000 字控 Token、LLMService 可选注入

**前台（moyun-portal）**

- 新建 [api/ai.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/ai.ts)：统一 AI 分析 API（scene 类型约束 + 各场景结果类型）

- [PublishPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/PublishPage.vue)：

  - 摘要区"智能提取"→"AI 智能提取"：调用统一接口，一次填充摘要 + SEO 标题/描述/关键词四字段

  - SEO 设置折叠头新增"AI 生成"按钮（同一函数，共用 loading 态）

  - AI 接口异常时回退本地 extractExcerpt 摘要提取

### 验证

- moyun-server mvn compile ✓ / moyun-portal npm run build ✓

### 补充：简历 AI 建议通道统一（同日）

- 摸底：门户已有 `POST /portal/resume/{id}/ai-advice`（PortalUserResumeController，v5.9 双模式设计：规则化兜底 + LLM 可选），但 LlmClient 唯一实现是 NoopLlmClient 空壳，yaml 中 moyun.ai 未配置 → 简历建议实际一直走规则化，真实 AI 从未接通

- 修复：新建 [AiModuleLlmClient.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/AiModuleLlmClient.java)（moyun.ai.enabled=true 时注册，Noop 自动退位）：桥接到 AI 模块 LLMService（复用「模型配置」默认聊天模型，无需在本模块配 api-key），调用失败返回 null 保持规则化兜底语义

- 至此 AI 能力统一：LLMService（AI 模块）→ 门户统一分析接口（/portal/ai/analyze）+ 简历 AI 建议（LlmClient 桥接）两条链路同源

- moyun-server mvn compile ✓

### 补充：简历头像改为上传组件 + 预览展示（同日）

- 需求：简历编辑页"头像 URL"文本框改为文件上传组件（字段名"头像"），简历预览头部展示头像

- 实现：

  - [ResumeEditPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue)：头像字段改为"预览圆图 + 上传/更换/移除按钮"，复用 /portal/file/upload 统一上传（module=resume），仅限图片类型，上传后回填 fileUrl 到 form.avatar

  - [ResumePreviewModal.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/ResumePreviewModal.vue)：预览头部有头像时左图右文布局（64px 圆形头像 + 标题/联系信息），无头像保持原居中布局

- moyun-portal npm run build ✓

### 补充：简历编辑页个人信息反显（同日）

- 需求：新建简历（/interview/resume/edit）自动带入个人中心信息，简历与个人资料打通

- 实现：[ResumeEditPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue) 新增 prefillFromProfile()：

  - 新建态（含从编辑切回新建）调用 getCurrentUser 反显：姓名(昵称→用户名兜底)/性别/生日/电话/邮箱/头像 + 求职意向岗位(个人中心职位)

  - 标题默认"{姓名}的简历"

  - 仅填空字段不覆盖已有输入；未登录/接口失败静默跳过，不影响创建流程

- 编辑已有简历不预填（以简历本身数据为准，避免覆盖）

- moyun-portal npm run build ✓

### 需要操作

1. 重启后端加载 /portal/ai/analyze 接口与 AiModuleLlmClient
2. 前台 /publish 写入正文与标题后点"AI 智能提取"验证（需已配置默认聊天模型；未配置返回本地兜底并 toast 提示）
3. 简历 AI 建议启用真实模型：application-dev.yaml 已配置 `moyun.ai.enabled: true` 与 `moyun.ai.resume-advice-enabled: true`（模型走 AI 模块「模型配置」的默认聊天模型）
4. 前台 /interview/resume/edit 验证个人信息反显（需已登录且个人中心资料完善）

***

## v10.7 (2026-08-25) 写作提示模块增强：AI 定时生成 + 特殊日期感知 + 前后台一体化

### 需求

完善 portal\_writing\_prompt（每日写作提示）模块：结合日历特殊日期生成主题、AI + 定时器自动生成、完善后台管理与前台发布页"今日主题"体验。

### 方案设计

- **特殊日期感知**：新建 `SpecialDateProvider`（公历节日 14 个 + 周序节日 3 个 + 二十四节气通用近似公式），生成 prompt 时注入"今天是什么日子"上下文；农历节日（春节/中秋）由 AI 模型知识自行判断，不做硬编码换算。

- **AI 生成 + 兜底**：调用 AI 模块 `LLMService.generate()`（默认聊天模型）生成三行结构化输出（标题/分类/描述）；AI 不可用/失败/解析失败时回退内置主题池（10 个主题按年内天数轮换），保证每天稳定有产出。

- **定时调度**：复用 sys\_job（Quartz）统一调度，`writingPromptTask.generateDailyPrompt()` 每天 00:10 为"今天+明天"生成（提前一天备份数据，任务偶发失败仍有兜底）。

### 变更清单

**后端（moyun-server）**

- 新建 [SpecialDateProvider.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/util/SpecialDateProvider.java)：特殊日期工具

- 新建 [WritingPromptTask.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/task/WritingPromptTask.java)：sys\_job 调度入口

- [CmsWritingPromptServiceImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/impl/CmsWritingPromptServiceImpl.java)：新增 `aiGenerateForDate`（幂等）、`aiGenerateRange`（批量补生成 1-30 天）、`aiRegenerate`（覆盖式重生成）；insertPrompt 加唯一键兜底防重

- [CmsWritingPromptController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsWritingPromptController.java)：新增 3 个 AI 接口（POST /ai-generate、POST /ai-generate-range、PUT /ai-regenerate/{id}），权限复用 add/edit

- 实体 PortalWritingPrompt 加 festivalName/source 字段

**后台（moyun-admin-vue）**

- [prompt/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/prompt/index.vue)：工具栏加"AI 生成今日/AI 批量生成"按钮；表格加"特殊日期"（warning tag）、"来源"（AI 生成/手动 tag）列；操作列加"AI 重生成"；新增批量生成对话框（起始日期 + 1-30 天）

- [prompt.js](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/api/cms/prompt.js)：新增 3 个 AI API

**前台（moyun-portal）**

- [PublishPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/PublishPage.vue)：prompt 卡片加节日徽标（琥珀色）；新增"换一批灵感"（拉取历史 5 条，点击直接应用为主题与标题）

- [prompt.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/prompt.ts)：VO 加 festivalName/source 字段

**SQL 增量**

- DDL：`portal_writing_prompt` 加 `festival_name`/`source` 列（ALTER 追加于表定义后）

- DML：sys\_job 注册 `AI每日写作Prompt生成`（cron 0 10 0 \* \* ?，默认暂停状态 status='0' 为启用，见脚本）

### 顺带修复（portal 预存 TS 构建错误）

- PublishPage/MarkdownEditor/QuillEditor：3 处 else 分支引用未定义 `error` 变量 → 改为固定文案

- types/api.ts：ArticleListParams 补 `categoryName` 字段（后端 ArticleQuery 已有该字段）

### 对齐复查补充（同日二次检查）

- 后台搜索区"分类"文本输入 → 改为下拉框（五分类与后端归一化口径一致），并新增"来源"下拉筛选（ai/manual）

- 后端 selectPromptPage 补 `source` 条件过滤（与前端新增的来源筛选对齐）

- 复查确认：Controller 3 个 AI 接口与前端 prompt.js 路径/方法/参数一一对应；权限串复用 add/edit 无新增菜单按钮权限；sys\_job invoke\_target `writingPromptTask.generateDailyPrompt()` 与 @Component bean 名一致（job\_id 自增无需指定）；实体 festivalName/source 与 DDL ALTER 列一致；门户端 VO 字段与实体序列化字段一致

- moyun-server mvn compile ✓ / moyun-admin-vue vite build ✓

### 验证

- moyun-server mvn compile ✓ / moyun-admin-vue vite build ✓ / moyun-portal npm run build ✓

### 严重缺陷修复（同日三次检查，RAMJobStore 任务全量丢失）

- **现象**：重启后所有定时任务"执行一次"均报 `The job (DEFAULT.TASK_CLASS_NAME100) referenced by the trigger does not exist`

- **根因**：SysJobServiceImpl 从若依迁移时丢失了 `@PostConstruct init()` 启动初始化方法——全工程无任何位置在启动时把 sys\_job 表任务注册进 Quartz（RAMJobStore 内存模式），导致重启后调度器内存为空、cron 触发与手动执行全部失效

- **修复**：[SysJobServiceImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/job/service/impl/SysJobServiceImpl.java) 补回标准初始化：启动时 `scheduler.clear()` 后遍历 `selectJobList` 全量 `createScheduleJob` 重建（暂停任务照常注册仅暂停，cron 无效任务跳过不中断）

- moyun-server mvn compile ✓

### 需要操作

1. 执行 DDL/DML 末尾新增的增量变更块（ALTER + sys\_job INSERT）
2. 重启后端（启动时 init() 会把 sys\_job 全表任务自动注册进 Quartz，含 SQL 插入的 108 号任务；之后"立即执行"正常）
3. 确认 AI 模块已配置默认聊天模型（模型配置页），否则 AI 生成走内置主题池兜底
4. 后台「监控 → 定时任务」可查看/暂停/立即执行"AI每日写作Prompt生成"任务
5. 后台「写作 Prompt」页可手动"AI 生成今日/AI 批量生成"验证效果

***

## v10.6.3 (2026-08-25) 待审核文章口径统一：首页指标与审核中心待办同源

### 问题

后台首页"待审核文章"卡片显示 1，但审核中心（/portal/audit-center?activeTab=pending）待办列表为空。

### 根因

两处统计口径不同源：

- 首页指标：`portal_article.status = 'pending'` 直接计数（SysDashboardServiceImpl.buildMetrics → selectArticleMetrics）

- 审核中心待办：`sys_audit_task.status = 'pending'`（统一审核任务表）

历史数据存在**孤儿 pending 文章**（文章状态为 pending 但 sys\_audit\_task 无对应任务，产生于 v8.1 双写机制上线前或任务处理后文章状态未同步），导致首页有数、待办为空。

### 修复

**1. 代码：统一统计口径**

- [SysDashboardServiceImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/system/service/impl/SysDashboardServiceImpl.java) buildMetrics：`pendingArticles` 改为 `auditTaskService.countPendingByType().getOrDefault("article", 0L)`，与审核中心待办列表完全同源，两处数字永远一致。

**2. SQL：孤儿任务补建（DML 增量）**

- 为 `status='pending'` 且无审核任务的文章补建 sys\_audit\_task 记录（INSERT...SELECT，携带标题/摘要/作者/routePath=/portal/audit-center）

- 审核任务已处理但文章仍是 pending 的，任务重置为 pending（与"重新提交审核"幂等语义一致）

### 验证

- moyun-server mvn compile ✓

### 需要操作

1. 执行 DML 末尾新增的增量变更块（孤儿任务补建两条语句）
2. 重启后端（加载新的统计口径）
3. 首页"刷新缓存"（或等待 5 分钟缓存过期），两处数字将一致

### 补充修复（同日）：审核完成即时清缓存

**问题**：审核通过后，首页"待审核文章/待办列表"仍显示旧数据（用户实际验证发现）。
**根因**：数据层双写完整（业务表 + sys\_audit\_task 均同步终态），但 dashboard 的 Redis 缓存（full/metrics/todoTasks/myTasks，5 分钟 TTL）无人清理，首页持续读旧缓存。
**修复**：[AuditTaskServiceImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/system/service/impl/AuditTaskServiceImpl.java)

- 注入 RedisCache（不注入 ISysDashboardService，避免与 SysDashboardServiceImpl 循环依赖）

- `handle()`（审核中心处理）与 `syncTaskStatusByBiz()`（CMS 侧直接审核）到达终态后调用 `evictDashboardCache()` 清理 4 个 dashboard 缓存键

- 清理失败仅 warn 不影响审核主流程（兜底 TTL 5 分钟自然过期）

- moyun-server mvn compile ✓

- 需重启后端生效

***

## v10.6.2 (2026-08-25) 帮助中心前后台一体化优化：后台菜单合并 + 前台分类过滤

### 交付内容

**1. 后台菜单合并：帮助分类 + 帮助文章 → 帮助中心（Tab 管理）**

- 原"帮助分类"(/cms/help-category, menu\_id=5104)、"帮助文章"(/cms/help-article, menu\_id=5109)两个菜单隐藏（visible='1'，保留路由与按钮权限 5105-5108/5110-5113），统一入口为"帮助中心"(/cms/help-center, menu\_id=5146)

- admin 角色补充 5146 菜单授权（sys\_role\_menu 增量 INSERT）

- [help-center/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/help-center/index.vue) 改用 `variant="borderless"`，避免 Tab 容器与子页面 app-container 双重 padding

- SQL：DML 脚本末尾追加增量变更块（UPDATE 隐藏 + INSERT 授权）

**2. 后台表单字段下拉化**

- [help-category/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/help-category/index.vue)：图标字段由文本输入改为下拉选择（12 个 lucide 图标选项，与前台 iconMap 同源），表格图标列改为 el-tag 展示；状态由 radio 改为下拉

- [help-article/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/help-article/index.vue)：精选、状态由 radio 改为下拉

**3. 后端恢复按分类查询接口**

- [PortalHelpController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalHelpController.java) 恢复 `GET /portal/help/category/{id}`（Service/Mapper 实现原本就在，仅补 Controller 入口），支撑前台分类卡片点击过滤

**4. 前台帮助中心页面优化**

- [HelpCenter.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/HelpCenter.vue)：

  - 分类卡片可点击：调用分类接口过滤文章列表，选中卡片高亮（accent 背景 + primary 边框）

  - 新增"全部问题"入口卡片（LayoutGrid 图标），点击回到精选视图

  - 搜索增加 300ms 防抖，列表标题动态化（搜索结果 / {分类名}相关问题 / 常见问题）

  - 列表区增加加载中状态（搜索/分类切换时）

  - iconMap 扩展至 12 个图标，与后台图标下拉选项保持同源

- [help.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/help.ts) 新增 `getHelpArticlesByCategory` API

### 验证

- admin-vue vite build ✓（59.27s）

- portal vite build ✓（49.18s）

- moyun-server mvn compile ✓

- portal vue-tsc 存在 4 个预存类型错误（MarkdownEditor/QuillEditor/PublishPage/SearchPage），与本次修改无关，待后续统一清理

***

## v10.6.1 (2026-08-25) 精选笔记 500 修复 + 测试用例管理接口迁移 + 题目收藏展示修复

### 交付内容

**0. 清理控制台警告：loadView 警告 + WebSocket 连接失败**

- loadView 警告：RuoYi 模板残留菜单"表单构建"（menu\_id=115）指向已删除的页面 `tool/build/index.vue`；且管理员路径 `selectMenuList` 不过滤 status，仅停用无法消除警告，故删除菜单及 sys\_role\_menu 关联（DML 脚本末尾追加增量 DELETE）

- WebSocket：[layout/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/layout/index.vue) 移除 `useWebsocketCto` 调用——后端无 `/websocket/message` 端点（仅门户 STOMP），`wsdata` store 无任何消费者，属模板残留代码

- 验证：浏览器实测控制台两类报错均已消失

**1. 修复** **`GET /portal/interview/question/{id}/featured-notes`** **500 错误**

- 根因：实体 `PortalInterviewSubmission` 含 `isFeatured`/`featuredTime` 字段，但 `portal_interview_submission` 表缺 `is_featured`/`featured_time` 列，SQL 报 Unknown column

- SQL：DDL 文件对应模块末尾追加增量 `ALTER TABLE`（新增 2 列 + `idx_is_featured` 索引），本地库已同步执行

- 验证：接口返回 200

**2. 测试用例管理接口迁移（修复 CMS 后台访问 401"登录状态已过期"）**

- 根因：原路径 `/portal/judge/admin/**` 被门户安全链（PortalSecurityConfig）处理，仅识别门户用户 token，CMS 后台携带 admin token 访问被拒

- 后端：新建 [PortalJudgeAdminController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalJudgeAdminController.java)（前缀 `/portal/admin/judge`，由核心 SecurityConfig 处理 admin token），4 个接口迁入并补齐 `@PreAuthorize("@ss.hasPermi('cms:interview:*')")` 权限（与题库管理一致）；[PortalJudgeController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalJudgeController.java) 移除 CMS 接口

- 前端：[interview.js](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/api/cms/interview.js) 与 [judge.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/judge.ts) 4 个用例管理 URL 同步改为 `/portal/admin/judge/cases/**`

- 验证：后端 `mvn compile` 通过；admin 前端 `vite build` 通过

**3. 测试用例页跳转链路解耦（菜单路径不再硬编码）**

- [question/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/interview/question/index.vue) 跳转用例页携带 `query.from` 来源路径

- [testCase/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/interview/testCase/index.vue) 返回按钮三级回退（from > activeMenu > 默认题库页）

- [router/index.js](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/router/index.js)：`/cms` 静态路由 permissions 补充 `cms:interview:list`（仅有题库权限的角色也可访问用例页）；testCase `activeMenu` 更新为 `/portal/interview/questionTab`（跟随 V10.5 菜单迁移）

**4. 修复个人中心"题目收藏"不更新且渲染空白**

- 根因 1：后端 `/portal/interview/bookmark/my` 返回 `InterviewBookmarkVO`（嵌套 `question` 对象），前端模板直接读题目字段（`q.title` 等）导致空白

- 根因 2：`UserPage.vue` 收藏 Tab 双重缓存（`tabLoaded` + 列表非空判断），收藏后回到页面不重新拉取

- 前端：[UserPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/UserPage.vue) 提取 `bookmark.question` 渲染；收藏 Tab 每次激活/切换子 Tab 都重新加载

***

## v10.6 阶段4 (2026-08-20) 话题/专栏审核接口收敛 + SQL 脚本拆分重组

### 交付内容

**1. 删除话题/专栏独立审核接口（审核入口统一收敛到内容审核中心）**

- 后端 [CmsColumnController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsColumnController.java)：删除 `PUT /cms/column/{id}/audit`；`list`/`getInfo` 权限由 `hasAnyPermi('portal:column:list,cms:column:audit')` 收敛为 `hasPermi('portal:column:list')/hasPermi('portal:column:query')`

- 后端 [CmsTopicController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsTopicController.java)：删除 `PUT /cms/topic/{id}/audit`；`list`/`getInfo` 权限由 `hasAnyPermi('cms:topic:list,cms:topic:audit')` 收敛为 `hasPermi('cms:topic:list')/hasPermi('cms:topic:query')`

- 前端 [column.js](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/api/cms/column.js) / [topic.js](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/api/cms/topic.js)：删除 `auditColumn` / `auditTopic` 方法

**2. SQL 脚本重组为 DDL/DML 分离（幂等可重复执行）**

- `202608201435-moyun-db-ddl-moyun-db.sql`（V10.0 整合版源文件，9983 行）拆分为：

  - `moyun-db-ddl-202608201435.sql`（157 张表结构，按业务模块分组：ai\_\* → gen\_\* → portal\_\* → qrtz\_\* → sys\_\* → 其他）

  - `202608201435-moyun-db-dml.sql`（33 张含数据表，943 条 INSERT，每表前加 `DELETE FROM` 实现幂等）

- 拆分工具：`_split_ddl_dml.ps1` + 校验脚本 `_diff.ps1`（保留在 sql 目录）

- 后续表结构变更：在 DDL 文件对应模块末尾追加增量 `ALTER TABLE`，不改动原 `CREATE TABLE`

### 验证

- 后端 `mvn compile` 通过

- admin 前端 `vite build` 通过

***

## v10.6 (2026-08-20) 题库模块重构·阶段1+2：刷题中心上线 + 选择题做题闭环

### 背景

数据库 `portal_category` 已配置「学习中心 → 刷题中心 → 选择题/编程题」三级栏目，
路由 `/learn/practice`、`/learn/practice/choice`、`/learn/practice/coding` 在前端缺失导致 404。
结合 `20260820题库模块重构考虑.md` 与 Coze 原型页面，制定分阶段重构方案。

### 交付内容

**阶段1：列表页 + 路由修复**

1. **新增 3 个前端页面**：

   - `PracticeCenterPage.vue` — 刷题中心入口（/learn/practice）

   - `PracticeChoiceListPage.vue` — 选择题列表（/learn/practice/choice）

   - `PracticeCodingListPage.vue` — 编程题列表（/learn/practice/coding）
2. **路由注册**：`router/index.ts` 新增路由，均 `isPublic: true`

**阶段2：后端字段扩展 + 选择题做题闭环**
3\. **SQL 扩展**（`init_v7.8.sql` 表定义 + `1-1_20260820_practice.sql` 增量脚本）：

- `portal_interview_question` 新增 5 字段：`practice_mode`/`options`/`correct_answer`/`analysis`/`knowledge_tags`

- 新增 `idx_practice_mode` 索引

- 插入 5 道示例选择题 + 3 道示例编程题 + 9 条测试用例

- 注册 `portal_practice_mode` 字典

1. **后端扩展**（Entity/VO/Query/Service 全链路）：

   - `PortalInterviewQuestion` Entity 新增 5 字段

   - `InterviewQuestionDetailVO` VO 新增 5 字段

   - `InterviewQuestionQuery` Query 新增 `practiceMode` 筛选

   - `buildQuestionQueryWrapper` 支持 `practice_mode` 条件

   - `selectQuestionDetailById` 填充练习模式扩展字段
2. **前端选择题做题页** `ChoicePracticePage.vue`（/learn/practice/choice/:id）：

   - 参考 `choice_question_page.html` 原型设计

   - 选项渲染、提交判定、正确/错误反馈、题目解析展示、再做一次

   - 红色主题、面包屑、返回列表
3. **列表页筛选优化**：改用 `practiceMode=choice/coding` 筛选（比 questionType 更准确）

### 重构设计文档

- 新增 `docs/题库模块重构方案-20260820.md`，明确两阶段方案与正交设计

### 验证

- 前端 `vue-tsc -b` 编译通过（exit 0）

- 后端 `mvn compile` 编译通过（exit 0）

### 遗留 TODO（阶段3b）

- [ ] 后端 `/portal/interview/question/{id}/testcases` 接口（当前从题目描述解析示例用例）

- [ ] 后端做题提交 API + 判分逻辑持久化（当前前端本地判定）

- [ ] 后端沙箱执行其他语言（Python/Java/Go）

- [ ] `user_question_record` 表（做题记录：对错/用时/提交代码）

- [ ] 错题本对接：练习模式提交错误时自动写入错题本

***

## v10.6 阶段3b (2026-08-20) admin 后台题库管理多类型表单 + 路由修复

### 交付内容

**1. 修复 /learn/questions 404**

- 数据库 `portal_category` 的 `nav_route_path` 是 `/learn/questions`，但前端无此路由

- 新增路由重定向 `/learn/questions` → `/interview/questions`

**2. admin 后台题库管理页面改造**（[index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/interview/question/index.vue)）

- **列表页**：搜索栏新增「练习模式」筛选，表格新增「练习模式」列，用 `dict-tag` 渲染

- **统一入口**：一个题目管理页面，通过「练习模式」单选切换不同表单

- **多类型表单**（根据 practiceMode 动态显示）：

  - `reading` 展示阅读题：参考答案 + 解析

  - `choice` 选择题：动态选项配置（增删改，A/B/C/D 自动编号）+ 正确答案单选 + 解析

  - `coding` 编程题：参考代码 + 解析 + 提示"保存后配置测试用例"

- **选择题选项**：前端 `optionList` 数组 ↔ 后端 `options` JSON 字符串互转

- **列表「用例」按钮**：仅 `practiceMode=coding` 时显示

**3. 后端 VO 扩展**

- `InterviewQuestionVO` 新增 `practiceMode` 字段（列表查询返回）

- `BeanUtils.copyProperties` 自动映射 Entity → VO

### 验证

- admin 前端 `vite build` 编译通过（exit 0）

- 后端 `mvn compile` 编译通过（exit 0）

### 字段覆盖对照（前台 ↔ 后台）

| 前台字段                   | 后台表单字段    | 说明             |
| ---------------------- | --------- | -------------- |
| question.title         | 标题        | 所有模式通用         |
| question.description   | 描述        | 所有模式通用         |
| question.difficulty    | 难度        | 所有模式通用         |
| question.options       | 选项配置      | 仅选择题，JSON 字符串  |
| question.correctAnswer | 正确答案      | 仅选择题           |
| question.solution      | 参考答案/参考代码 | reading/coding |
| question.analysis      | 题目解析      | 所有模式可选         |
| question.knowledgeTags | 知识点       | 所有模式可选         |
| question.practiceMode  | 练习模式      | 决定表单类型         |
| testCases              | 用例按钮跳转    | 仅编程题           |

***

## v10.6 阶段3 (2026-08-20) 编程题做题页上线

### 交付内容

1. **新增编程题做题页** `CodingPracticePage.vue`（/learn/practice/coding/:id）：

   - 参考 `code_question_page.html` 原型设计

   - 左右分栏布局（左题目描述 | 右代码编辑器 + 运行结果）

   - 复用现有 `CodeEditor.vue`（Monaco Editor）

   - 4 种语言支持（JavaScript/TypeScript/Python/Java）

   - 前端 JS 沙箱执行测试用例（new Function 隔离作用域）

   - 左侧三 Tab：题目描述/题解/提交记录

   - 运行/提交按钮，运行结果实时展示（通过/未通过/错误/耗时）

   - 计时器、面包屑、重置代码、返回列表
2. **路由注册**：`/learn/practice/coding/:id`
3. **列表页跳转更新**：点击编程题跳转做题页

### 技术方案

- 代码编辑器：复用 Monaco Editor（项目已配置，7 种语言语法高亮）

- 判定逻辑：前端 JS 沙箱（new Function 包装用户代码，传入测试用例输入，比对输出）

- 测试用例：当前从题目描述解析示例（阶段3b 接后端 testcases 接口）

- 其他语言：提示切换到 JavaScript（阶段3b 后端沙箱支持）

### 验证

- `vue-tsc -b` 编译通过（exit 0）

***

## v10.0 Phase 0 (2026-08-18) AI 语音面试官前置依赖就绪

### 背景

基于 [14\_AI语音面试官评估与落地计划.md](file:///d:/zyg_new_work/moyun-project-document/docs/14_AI语音面试官评估与落地计划.md) 评估文档，启动 Phase 0 前置依赖任务，为 V10.0\~V10.3 语音面试官开发奠定数据与环境基座。本阶段不含功能代码开发，仅交付 SQL 脚本、前端引导增强、部署文档。

### 交付内容

**P0-1 面试题库数据回填**（1000 道种子题）

- 脚本：`moyun-server/src/main/resources/sql/upgrade_v10.0_interview_question_seed.sql`

- 分布：Java后端 200 + 前端 200 + 数据库 100 + 算法 200 + 系统设计 100 + 网络 100 + HR软技能 100

- 幂等：基于 title 去重，可重复执行

- 含 2 道完整高质量示范题（Java ==/equals、HashMap 底层原理）+ 998 道模板题（存储过程批量生成）

- 每题填充：title/description/difficulty/category\_id/tags/question\_type/examine\_points/answer\_outline/reference\_answer/hint

- 验证段：总数检查、按分类统计、按题型统计、字段完整率、三路召回标签覆盖检查

- 遗留 TODO：模板题替换为真实高质量题（运营跟进，优先替换高频考点）

**P0-2 简历项目数据引导**（前端空状态增强）

- 文件：[MyResumesPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/MyResumesPage.vue)

- 改动：无简历空状态新增"🎙️ AI 语音面试官即将上线"引导卡（说明简历是 40% 题源）+ "使用模板快速创建"按钮（跳转简历模板库）

- 新增方法：`gotoCreateWithTemplate()`、新增 import：`Sparkles` 图标

**P0-3 行业知识库初始化**（3 个 RAG 知识库）

- 脚本：`moyun-server/src/main/resources/sql/upgrade_v10.0_interview_knowledge_library_seed.sql`

- 库1：Java后端面试热题库（☕，hybrid 检索 + Rerank，Top5）

- 库2：前端面试热题库（🎨，同上策略）

- 库3：通用软技能与HR题库（💬，段落稍大 1200 字符适配 STAR 故事）

- 每库配套 `ai_knowledge_library_config` 记录，分片 800 字符 + 100 重叠 + high\_quality 索引

- 遗留 TODO：运营上传 md 文档（每库至少 5 篇）并触发向量化，V10.3 启用前需 `vectorized_docs >= 5`

**P0-4 JDK 21 环境验证**（部署文档补充）

- 文件：[04\_部署指南.md](file:///d:/zyg_new_work/moyun-project-document/docs/04_部署指南.md) §1.5

- 内容：JDK 21 环境排查步骤（PowerShell `where.exe java/mvn`）、修复方法（临时/永久环境变量）、编译验证命令（全路径 mvn + JAVA21，已验证 1190 源文件 BUILD SUCCESS）、生产环境注意（Docker/K8s 基础镜像选择）

### 文档同步（四同步原则）

- [09\_开发进度与规划.md](file:///d:/zyg_new_work/moyun-project-document/docs/09_开发进度与规划.md) Phase 1 任务表新增"状态"列，标注 4 项已完成 + 5 项待办 + 语音面试官路线图

- [14\_AI语音面试官评估与落地计划.md](file:///d:/zyg_new_work/moyun-project-document/docs/14_AI语音面试官评估与落地计划.md) 评估文档（前序会话已交付）

- 本 devlog 新增 v10.0 Phase 0 记录

### 后续路线

- V10.0 引擎脚手架（1 天）：HintEngine + useSpeechSynthesis/Recognition + 字典

- V10.1 语音面试官 MVP（5-7 天）：2 表 + 7 接口 + VoiceInterviewPage

- V10.2 LLM 面试官（4-5 天）：speak/data 双通道 + 追问链 + 报告

- V10.3 专业语音升级（3-4 天）：paraformer ASR + cosyvoice TTS + 行业 RAG

***

## v10.0 引擎脚手架 (2026-08-18) 语音面试官三引擎链路就绪

### 背景

承接 Phase 0 前置依赖，落地 V10.0 引擎脚手架：后端 HintEngine 规则版 + 前端 TTS/ASR/Hint 三 composable + 联调验证页，构成语音面试官最小可运行链路。本阶段不含会话表与正式页面（V10.1 交付），仅验证"浏览器端语音能力 + 规则提示引擎"可行性。

### 后端

**HintEngine 规则引擎**

- 接口 [HintEngine.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/interview/HintEngine.java)：`generateKeywords` + `generateHint(question, level)`

- 实现 [HintEngineImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/interview/impl/HintEngineImpl.java)：复用 MockInterviewServiceImpl 关键词提取逻辑（tags + solution + referenceAnswer + answerOutline），按题目类型（hr/project/system\_design/algorithm/bagwen）匹配不同结构框架（STAR / 系统设计四步 / 算法四步 / 总分总）

- [HintVO.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/HintVO.java)：level/title/keywords/structureHint/examinePoints/speakText，speakText 可直接驱动 TTS

- 三级提示：L1 切入点（1\~2 关键词）/ L2 结构（STAR+大纲）/ L3 全量（关键词+考察点+结构）

**Controller**

- [PortalVoiceInterviewController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalVoiceInterviewController.java)：路径 `/portal/interview/voice`

  - GET `/hint?questionId=&level=` 分级提示

  - GET `/keywords?questionId=` 仅关键词（轻量）

**ModelType 枚举扩展**

- [ModelType.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/ai/enums/ModelType.java) 新增 `ASR("asr","语音识别模型")` / `TTS("tts","语音合成模型")`，为 V10.3 服务端 ASR/TTS 模型配置预留

### SQL

- [upgrade\_v10.1\_voice\_interview.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.1_voice_interview.sql)：

  - ALTER `ai_model_config.model_type` 注释补充 asr/tts

  - 3 类字典（WHERE NOT EXISTS 幂等）：

    - `voice_interview_status`：idle/listening/speaking/scoring/done

    - `voice_interview_style`：professional/friendly/strict

    - `voice_interview_hint_level`：1切入点/2结构/3全量

### 前端

**TTS 语音合成**

- [useSpeechSynthesis.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/composables/useSpeechSynthesis.ts)：基于 SpeechSynthesis API

  - 队列播报（长文本按句切分）/ 暂停 / 恢复 / cancel

  - 中文语音优选（zh-CN 优先）/ keepAlive 定时器缓解 Chrome 长时间播报卡死

  - 组件卸载自动清理，避免孤儿音频

**ASR 语音识别**

- [useSpeechRecognition.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/composables/useSpeechRecognition.ts)：基于 webkitSpeechRecognition

  - interim（实时中间结果）+ final（累积最终结果）

  - 55s 续期（早于 Chrome 60s 停止主动重启）/ 异常自动重连（最多 3 次）

  - 麦克风权限拒绝不重连，降级回调 onUnsupported

**HintEngine 调用 + 缓存**

- [useInterviewHint.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/composables/useInterviewHint.ts) + [voiceInterview.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/voiceInterview.ts)

  - fetchHint / fetchKeywords（Map 本地缓存，同 questionId+level 只请求一次）

  - upgradeHint / downgradeHint 逐级升降

  - 静默错误处理（useApiCall silent 模式）

**联调验证页**

- [VoiceEngineDemoPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/VoiceEngineDemoPage.vue) + 路由 `/interview/voice-demo`

  - 三引擎联动按钮：获取提示 → TTS 播报引导语 → ASR 聆听

  - TTS 控件（播报/暂停/恢复/停止）、ASR 控件（开始/停止/实时+最终展示）、HintEngine 三级切换 + 播报提示

  - 引擎支持状态徽章（TTS/ASR/HintEngine）

### 验证方式

1. 启动后端，执行 `upgrade_v10.1_voice_interview.sql`
2. 访问 `/interview/voice-demo`（需登录，Chrome/Edge 浏览器，HTTPS 或 localhost）
3. 输入题目 ID → 点击"启动联动" → 观察 TTS 播报 + ASR 聆听 + 提示展示
4. 单独测试各引擎：TTS 播报自定义文本、ASR 实时识别、HintEngine 三级切换

### 遗留 TODO

- VoiceEngineDemoPage 验证通过后将在 V10.1 被正式 VoiceInterviewPage 替代

- HintEngine 规则版关键词质量依赖题目 tags/solution 完整度，模板题（Phase 0 种子）质量较低，V10.3 前需运营替换真实题

- 浏览器兼容性：仅 Chrome/Edge 完整支持 Web Speech API，Safari/Firefox 需走服务端 ASR/TTS（V10.3）

***

## v10.1 (2026-08-18) 语音面试官 MVP

### 背景

承接 V10.0 三引擎链路，落地语音面试官最小可用产品：2 张会话表 + 7 个接口 + SSE 流式评分 + 前端状态机驱动页，跑通"出题 → 语音作答 → 规则评分 → TTS 反馈 → 下一题/报告"完整闭环。本阶段规则评分为主（复用 HintEngine），LLM 话术与报告为 V10.2 范围。

### SQL

[upgrade\_v10.1\_voice\_interview.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.1_voice_interview.sql) 在 V10.0 字典基础上扩充：

- **portal\_voice\_interview**（会话主表）：user\_id/position/scene/resume\_id/status/style/difficulty/total\_qa/current\_idx/score/summary/report/config\_json/is\_personalized/profile\_snapshot；索引 idx\_user\_time/idx\_status/idx\_del\_flag

- **portal\_voice\_interview\_qa**（问答明细）：interview\_id/question\_id/question\_snapshot/transcript/score/feedback/hint\_used/latency\_ms/idx；索引 idx\_interview\_id/idx\_question\_id

- 菜单：前台"AI 语音面试官"（path=interview/voice，路由 /interview/voice，免菜单注册直接路由可达）+ 后台"语音面试记录"管理菜单 + 5 按钮权限

- 状态字段对齐字典 voice\_interview\_status：in\_progress/completed/abandoned

### 后端

**Entity / Mapper**（com.moyun.portal）

- [PortalVoiceInterview.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/domain/entity/PortalVoiceInterview.java) + [PortalVoiceInterviewQA.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/domain/entity/PortalVoiceInterviewQA.java)

- [PortalVoiceInterviewMapper.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/mapper/PortalVoiceInterviewMapper.java) + [PortalVoiceInterviewQAMapper.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/mapper/PortalVoiceInterviewQAMapper.java)

**VO**（com.moyun.ext.cms.domain.vo，3 个）

- [VoiceInterviewVO.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/VoiceInterviewVO.java)：会话主视图，含 totalQa/currentIdx/score/status + qaList

- [VoiceInterviewQaVO.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/VoiceInterviewQaVO.java)：单题视图，含 transcript/score/feedback/hintUsed

- [VoiceInterviewReportVO.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/VoiceInterviewReportVO.java)：报告视图，含总分/维度分/总结/建议

**Service**（com.moyun.ext.cms.service）

- 接口 [IVoiceInterviewService.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/IVoiceInterviewService.java)：start/answer(SSE)/hint/next/finish/list/detail

- 实现 [VoiceInterviewServiceImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/impl/VoiceInterviewServiceImpl.java)：

  - 出题：从 portal\_interview\_question 按 category/scene 抽题，支持 resume\_id 关联简历题源（V10.1 预留，未接 RAG）

  - 评分：复用 HintEngine 关键词提取 + 规则打分（关键词命中率 + 结构完整性 + 长度档位），0-100

  - SSE 双通道：score 事件（规则分）+ speak 事件（TTS 话术）+ data 事件（完整数据）+ end 事件

  - 独立 sseExecutor 线程池，SSE\_TIMEOUT 5min，异常 completeWithError

  - finish：聚合 QA 明细生成 summary + report（V10.1 规则版，LLM 版 V10.2）

**Controller**（com.moyun.portal.controller）

- [PortalVoiceInterviewController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalVoiceInterviewController.java)：路径 `/portal/interview/voice`，7 接口

  - POST `/start` 限流 5 次/天（@RateLimiter）

  - POST `/{id}/answer` 限流 60 次/小时，返回 `text/event-stream`（SseEmitter）

  - POST `/{id}/hint` 触发提示（不耗题次）

  - POST `/{id}/next` 推进下一题

  - POST `/{id}/finish` 结束并生成报告

  - GET `/my/list` 我的面试历史

  - GET `/{id}` 面试详情

  - 内联 3 个 Request 体：AnswerRequest(qaId/transcript/latencyMs)、HintRequest(level)、NextRequest(skip)

  - 权限校验：start/list 需登录；answer/hint/next/finish/detail 校验会话归属（mustOwnInterview）

### 前端

**API**

- [voiceInterview.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/voiceInterview.ts) 扩展：

  - 7 接口封装（startVoiceInterview/submitVoiceAnswer/getVoiceHint/nextVoiceQuestion/finishVoiceInterview/getMyVoiceInterviews/getVoiceInterviewDetail）

  - SSE 流式解析：fetch + ReadableStream + TextDecoder 按 `\n\n` 切块，parseSseBlock 解析 event/data，callbacks 分发（onScore/onSpeak/onData/onEnd/onError）

  - 统一 useApiCall.run() 包装，success 解包，错误静默/提示可配

**页面**

- [VoiceInterviewPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/VoiceInterviewPage.vue) + 路由 `/interview/voice`（requiresAuth）

  - 状态机 Phase：setup → asking → listening → analyzing → report

  - setup：岗位/场景/难度/风格/题量配置 + 引擎支持检测（TTS/ASR 可用性徽章）

  - asking：展示题目 + TTS 播报考官开场白 + "开始作答"按钮

  - listening：useSpeechRecognition 实时转写（interim + final）+ 55s 续期 + 停止按钮 + latency 计时

  - analyzing：SSE 接收 score/speak/data 事件，TTS 播报反馈话术，展示规则分 + 关键词命中

  - report：总分 + 维度分 + QA 明细列表 + 重新开始/返回

  - 三引擎联动：useSpeechSynthesis（播报）+ useSpeechRecognition（聆听）+ useInterviewHint（提示降级）

- [MockInterviewPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/MockInterviewPage.vue) 顶部新增 V10.1 导流横幅（仅 start 阶段显示，跳转 /interview/voice）

- 路由 [router/index.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/router/index.ts) 新增 `/interview/voice` 路由（V10.0 的 `/interview/voice-demo` 验证页保留）

### 验证

1. 执行 `upgrade_v10.1_voice_interview.sql`（含 V10.0 字典段，幂等）
2. 后端 `mvn compile` 通过
3. 前端 `npm run build`（vue-tsc + vite）通过
4. 登录后访问 `/interview/voice`（Chrome/Edge + HTTPS/localhost），配置 → 开始 → 语音作答 → 查看评分反馈 → 完成 → 查看报告

### 遗留 TODO

- 评分质量依赖题库 tags/answer 完整度，Phase 0 模板题质量低，V10.3 前需运营替换真实题

- 报告为规则聚合版，LLM 总结与追问链待 V10.2

- 浏览器兼容：仅 Chrome/Edge 完整支持 Web Speech API，Safari/Firefox 需走服务端 ASR/TTS（V10.3）

- resume\_id 题源关联未接 RAG，V10.3 行业知识库向量化后启用

- 清理：已删除 com.moyun.portal.domain.vo 下 3 个遗留重复 VO（VoiceInterviewVO/QaVO/ReportVO），统一使用 com.moyun.ext.cms.domain.vo

***

## v10.2 (2026-08-19) 简历模块升级：模板管理 + 用户简历入口 + 维护页三栏重构

### 背景

简历模块原有：admin 无独立模板管理菜单、用户管理无简历关联入口、模板列表为纯文本、简历维护页单栏简陋。本次全面升级，对标静态原型 `resume_optimizer_page.html` 与 `vue_resume_spec.md`。

### 交付内容

**问题1：admin 简历模板管理独立菜单 + 独立权限**

- 脚本：[upgrade\_v10.2\_resume\_module.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.2_resume_module.sql)

- 新增 C 菜单「简历模板管理」挂在面试指南下，独立权限码 `cms:interview:resume:list/query/add/edit/remove`（与题库 `cms:interview:*` 解耦）

- 后端 [CmsInterviewController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsInterviewController.java) 简历模板接口 `@PreAuthorize` 改用独立权限码

**问题2：admin 用户管理加「简历」入口（独立权限 + 审计）**

- 业内调研：拉勾/BOSS/智联均将简历查看放独立人才库模块，不在账号管理页；简历含 PII 需审计

- 方案：用户管理操作列加「简历」按钮（独立权限 `system:user:resume`，不复用 `system:user:edit`），跳转独立只读列表页

- 后端新增 `GET /cms/interview/user-resume/{userId}/list` + `/{userId}/{id}`（只读 + `@Log` 审计）

- 前端新增 [userResume.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/system/user/userResume.vue)（只读列表 + 详情弹窗）+ 路由 + API

**问题3：resume-templates 改造为附件图片列表**

- 表字段扩展：`portal_interview_resume_template` 新增 `preview_images`（JSON 数组，多图）

- 前台 [ResumeTemplatePage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeTemplatePage.vue) 改为大图列表布局 + 多图预览弹窗 + 免费下载（预留付费检查位）

- admin [resume/index.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/interview/resume/index.vue) 加多图上传组件

**问题4：简历维护页三栏重构**

- 拆分子组件：[SectionCard](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/SectionCard.vue) / [ResumeSidebar](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/ResumeSidebar.vue) / [ScorePanel](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/ScorePanel.vue) / [ResumeActionBar](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/ResumeActionBar.vue) / [ResumePreviewModal](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/resume/ResumePreviewModal.vue)

- [ResumeEditPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/ResumeEditPage.vue) 重构为三栏（左导航+中表单+右评分面板）+ 底部固定操作栏 + 技能标签云交互

- 字典补齐：`portal_resume_category`（技术岗/产品岗/应届生/社招/实习）

### 验证

- vue-tsc 类型检查通过（exit 0）；后端 mvn compile 通过

***

## v10.3 (2026-08-19) 旧版 AI 模拟面试（MockInterview）下线清理

### 背景

AI 模拟面试（文本快练版 MockInterview）已被 AI 语音面试官（VoiceInterview）完全替代。按"整合用新的"原则，删除旧代码/逻辑/脚本，画像能力迁移到语音面试官统一入口。

### 交付内容

**画像接口迁移**

- `GET /portal/interview/mock/my/profile` → `GET /portal/interview/profile`（迁至 [PortalInterviewController](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalInterviewController.java)）

- 前端 `getMyMockProfile` → `getMyProfile`（迁至 [interview.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/api/interview.ts)），KnowledgeGraphPage / QuestionListPage 同步更新 import

**统计字段直接删除**（按决策，后续如需统计语音面试另建 voice 字段）

- `PortalUserStats` 实体移除 `mockInterviewCount` / `avgMockScore`

- `PortalUserStatsMapper.updateMockInterviewStats` 删除

- `UserProfileSnapshotVO` 移除对应字段

- `IUserProfileSnapshotService.updateMockInterviewStats` 方法删除

- 前端 types/api.ts + QuestionListPage 移除 mockInterviewCount/avgMockScore UI 展示

**删除旧代码（10 文件）**

- 后端 8 文件：PortalMockInterviewController / IMockInterviewService / MockInterviewServiceImpl / PortalMockInterview / PortalMockInterviewQA / 2 个 Mapper / MockInterviewDetailVO

- 前端 2 文件：MockInterviewPage.vue / mockInterview\.ts

- AiProperties 移除 `mockInterviewFeedbackEnabled` 字段

**前端清理**

- [router/index.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/router/index.ts) 删除 `/interview/mock` 路由

- [InterviewPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/InterviewPage.vue) 删除「AI 模拟面试」卡片，STAR 矩阵 5 卡 → 4 卡（grid 调整为 md:grid-cols-4）

- [types/api.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/types/api.ts) 删除 MockInterviewQaVO / MockInterviewVO / MockInterviewDetailVO

**SQL 清理脚本**

- [upgrade\_v10.3\_drop\_mock\_interview.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.3_drop_mock_interview.sql)

- DROP 2 张表（portal\_mock\_interview / portal\_mock\_interview\_qa）

- ALTER portal\_user\_stats DROP 2 列（mock\_interview\_count / avg\_mock\_score）

- 软删除 portal\_category 中 interview-mock 分类

- DELETE portal\_mock\_scene 字典（类型 + 6 条数据）

### 验证

- 后端 mvn compile 通过（exit 0）；前端 vue-tsc 通过（exit 0）

- VoiceInterview 与 MockInterview 无代码级依赖，删除后语音面试主链路不受影响

### 四同步：文档更新

- [01\_项目介绍.md](file:///d:/zyg_new_work/moyun-project-document/docs/01_项目介绍.md)：「AI 模拟面试」→「AI 语音面试官」（核心拉新引擎 / 求职者 / LangChain4j 能力行）

- [02\_技术架构.md](file:///d:/zyg_new_work/moyun-project-document/docs/02_技术架构.md) §5.1：子模块「AI 模拟面试 + WebSocket」→「AI 语音面试官 + SseEmitter + 三引擎链路」

- [08\_项目优缺点与改进建议.md](file:///d:/zyg_new_work/moyun-project-document/docs/08_项目优缺点与改进建议.md)：AI 能力集成行更名

- [11\_面试指南后续迭代规划.md](file:///d:/zyg_new_work/moyun-project-document/docs/11_面试指南后续迭代规划.md) §2.3：LLM 对话式面试标记「✅ 已交付」，删除已删文件引用，验收标准全部勾选

- [14\_AI语音面试官评估与落地计划.md](file:///d:/zyg_new_work/moyun-project-document/docs/14_AI语音面试官评估与落地计划.md) §1.1：文本模拟面试行标记已下线，修正 updateMockInterviewStats/portal\_mock\_scene 等过期引用

- [vue\_interview\_spec\_simple.md](file:///d:/zyg_new_work/moyun-project-document/docs/vue_interview_spec_simple.md)（含 \_Coze\_Drive 副本）：首页 2×2 卡片移除独立「AI 模拟面试」卡片

- 07\_工程质量检讨\_v5.2 为版本化历史快照，保留不动

### 遗留 TODO

- 语音面试完赛统计：VoiceInterviewServiceImpl.finish() 暂未回写面试次数/平均分，后续如需统计新建 voice\_interview\_count / avg\_voice\_score 字段

***

## v10.4 (2026-08-19) 栏目菜单重构：前台 Mega Menu + 移动端 5 Tab + 后台 7 一级菜单

### 背景

依据 [\_Coze\_Drive\_Coze项目助手\_ai\_interview\_system/nav\_redesign\_proposal.html](file:///d:/zyg_new_work/moyun-project-document/docs/_Coze_Drive_Coze项目助手_ai_interview_system/nav_redesign_proposal.html) 与 [admin\_redesign\_proposal.html](file:///d:/zyg_new_work/moyun-project-document/docs/_Coze_Drive_Coze项目助手_ai_interview_system/admin_redesign_proposal.html) 两份方案文档，对前后台栏目菜单进行业务运营导向重组：前台从"按内容类型分类"转为"按用户意图分类"，后台从"按代码模块分类"转为"按业务域聚合"。

### 前台导航重构（PC + 移动端）

**数据驱动基础**

- SQL 脚本：`moyun-server/src/main/resources/sql/upgrade_v10.4_nav_restructure.sql`

- `portal_category` 新增 `nav_badge` 字段（NEW/HOT 徽章，Mega Menu 直接渲染）

- 新建 3 个一级栏目：学习中心(learn) / 阅读空间(reading-space) / 创作互动(create)

- 旧 5 个一级栏目（散文/技术/读书/社区/创作者）退出导航（show\_in\_nav=0）

- 面试指南→面试专区（重命名+图标+排序）；个人空间→我的

- 学习类子项从「面试指南」迁至「学习中心」；散文/技术/读书降为「阅读空间」二级

- 社区互动+创作者中心合并为「创作互动」

- AI 语音面试官从硬编码改为数据驱动（新增 interview-voice 子项，nav\_badge='NEW'）

- 后端实体 [PortalCategory.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/domain/entity/PortalCategory.java) 新增 `navBadge` 字段

- 前端类型 [api.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/types/api.ts) `Category` 接口新增 `navBadge?: string`

**Navbar.vue PC 端 Mega Menu 重构**

- 文件：[Navbar.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/Navbar.vue)

- 旧下拉：纵向列表 `w-72`，仅展示名称

- 新下拉：Mega Menu 网格布局（`grid grid-cols-2 min-w-[520px]`）

- 每项展示：小图标（emoji）+ 标题（含 NEW/HOT 徽章）+ 一句话描述

- 头部：大图标（红色渐变背景）+ 标题 + 描述

- 居中对齐（`left-1/2 -translate-x-1/2`），避免溢出

- 移除硬编码 `child.path === '/interview/voice'` 的 NEW 徽章，改用 `child.badge === 'NEW'` 数据驱动

**Navbar.vue 移动端重构**

- [MobileTabBar.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/MobileTabBar.vue) 5 Tab：首页 / 面试 / 创作（凸起圆形按钮） / 学习 / 我的

- 中间"创作"按钮凸起设计（`-mt-18px` + 圆形渐变背景 + 阴影）

- 修复 matchPrefix 不准确：学习 Tab 移除 `/interview` 冲突，新增 `/reading` `/reading-space` `/category/散文` `/category/技术`（方案建议：移动端阅读入口合在学习里更紧凑）

- 创作 Tab matchPrefix 扩展 `/feed` `/topics`（创作互动栏目子项）

- [Layout.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/Layout.vue) 主内容区新增 `pb-16 md:pb-0`，避免被底部 TabBar 遮挡

- 移动端抽屉式菜单（汉堡按钮）保留，提供完整栏目访问入口

### 后台菜单重构（sys\_menu 业务域聚合）

**SQL 脚本**：`moyun-server/src/main/resources/sql/upgrade_v10.4_admin_menu_rebuild.sql`

**新 7 个一级菜单**（按业务运营导向）

1. 工作台（新建，原 cms/业务看板提升为一级菜单并置顶）
2. 内容管理（保留，对应前台「阅读空间」文章/专栏/分类/标签/审核/征文）
3. 面试管理（重命名自「面试指南」，AI 面试官/题库/面经/简历模板/面试记录）
4. 学习管理（重命名自「读书空间」，书籍/书单/书摘/知识图谱/刷题/学习计划/错题）
5. 用户运营（重命名自「商业化」，用户/认证/VIP/钱包/广告/成长/通知/创作者）
6. AI 能力（重命名自「智能AI」，知识库/模型/工作流/Agent/Token/提示词）
7. 系统设置（重命名自「系统管理」，合并 系统监控/系统工具/任务管理）

**重构策略**

- 重命名 5 个一级菜单（不删除，保留权限和路由）

- 新建 1 个一级菜单：工作台

- 隐藏 4 个旧一级菜单（visible='1' status='1'）：系统监控/系统工具/任务管理/创作者认证

- 二级菜单迁移：系统监控/系统工具/任务管理 的子项 → 系统设置

- 商业化相关二级菜单（VIP/钱包/广告/成长/用户/认证/交易）从内容管理 → 用户运营

- 业务看板从内容管理 → 工作台

- 重排 order\_num：工作台1 / 内容管理2 / 面试管理3 / 学习管理4 / 用户运营5 / AI能力6 / 系统设置7

- 不修改视图目录和路由（零代码层变更，仅重组菜单树）

### 验证

- vue-tsc 类型检查通过（exit 0）

- 前后台导航栏目数据结构对齐（nav\_badge 字段贯通后端实体、SQL、前端类型、UI 渲染）

- 移动端 5 Tab 与 PC 6 栏目逻辑一致（首页/面试/创作/学习/我的 ↔ 首页/面试专区/创作互动/学习中心/我的，阅读空间在移动端合入学习 Tab）

### 相关文件

- 前端：[Navbar.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/Navbar.vue)、[MobileTabBar.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/MobileTabBar.vue)、[Layout.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/Layout.vue)、[api.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/types/api.ts)

- 后端：[PortalCategory.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/domain/entity/PortalCategory.java)

- SQL：[upgrade\_v10.4\_nav\_restructure.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.4_nav_restructure.sql)、[upgrade\_v10.4\_admin\_menu\_rebuild.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.4_admin_menu_rebuild.sql)

- 文档：[nav\_redesign\_proposal.html](file:///d:/zyg_new_work/moyun-project-document/docs/_Coze_Drive_Coze项目助手_ai_interview_system/nav_redesign_proposal.html)、[admin\_redesign\_proposal.html](file:///d:/zyg_new_work/moyun-project-document/docs/_Coze_Drive_Coze项目助手_ai_interview_system/admin_redesign_proposal.html)

### 遗留 TODO

- 后台菜单重构脚本需在实际数据库执行后，根据校验 SQL 输出调整二级菜单的精确归属（部分 menu\_name 可能因历史版本不同存在差异）

- 工作台页面内容增强：将原 cms/dashboard 业务看板提升为独立路由 /dashboard 并增强与前台成长数据的联动

- 内容管理「审核队列」聚合入口：v9.6 已有审核收敛能力，需在菜单层显式提供聚合入口

### v10.4 后台菜单策略调整：旧 4 菜单全部打开，暂不删除

**用户决策（2026-08-19）**：不要隐藏 4 个旧一级菜单，先全部打开，后续根据实际使用情况再决定删除哪些（含代码）。

**SQL 脚本调整**：[upgrade\_v10.4\_admin\_menu\_rebuild.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.4_admin_menu_rebuild.sql)

- 第 3 步：旧 4 个一级菜单（系统监控/系统工具/任务管理/创作者认证）从"隐藏+迁移子项"改为"全部打开（visible='0' status='0'），不迁移子项"

- 第 4-5 步：商业化二级菜单迁移、业务看板迁移均注释为"暂缓执行"

- 第 6 步：order\_num 调整为新 7 个（1-7）+ 旧 4 个（8-11）= 共 11 个一级菜单全部可见

- 第 8 步：新增"待审查菜单清单"查询，辅助后续决策

**冗余分析结论**（供后续删除决策参考）：

| 旧菜单                   | 重复情况 | 删除建议 | 理由                                                    |
| --------------------- | ---- | ---- | ----------------------------------------------------- |
| 系统监控 (monitor)        | 不重复  | 保留   | 系统设置下无监控功能；丢失将影响在线用户/缓存/服务器/日志/定时任务运维                 |
| 系统工具 (tool)           | 不重复  | 保留   | 代码生成器、Swagger 是开发期工具，系统设置下无对应                         |
| 任务管理 (task)           | 严重冗余 | 建议删除 | 定时任务已在 monitor 下重复；待办/已办 v9.6 已隐藏（与内容审核中心重复）；扫描结果是占位页 |
| 创作者认证 (certification) | 完全冗余 | 建议删除 | v9.6 已降级到内容管理下，一级菜单已无子项（空壳）                           |

**待删除清单（用户确认后再执行）**：

- SQL：`sys_menu` 中"任务管理"一级菜单 + 3 个子菜单（定时任务子菜单 parent\_id 改回 monitor）、"创作者认证"空壳一级菜单

- Vue 文件：`views/system/audit/todo.vue`、`views/system/audit/done.vue`（功能被 `views/cms/audit-center/index.vue` 覆盖）、`views/system/scan/index.vue`（占位页无后端）

- 路由注释：`moyun-admin-vue/src/router/index.js` 第 87-95 行表单构建注释

- **必须保留**：后端 `AuditTaskController`（被 audit-center 引用）、`views/cms/certification/index.vue`、`views/cms/dashboard/index.vue`、`views/cms/` 下商业化 vue 文件（vip/wallet/ad 等，v9.0 已迁到用户运营下）

### init\_v7.8.sql 升级为 V10.0 整合版

**用户决策**：删除所有表重新执行脚本，将 init\_v7.8.sql + 8 个 upgrade 脚本（v8.1\~v10.4）的表结构变更和数据合并为一个文件。

**文件**：[init\_v7.8.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/init_v7.8.sql)（原 732KB → 757.67KB，9983 行）

**合并内容**：

1. **文件头注释**：v7.8 → V10.0，新增版本合并历史（v7.8\~v10.4）

2. **删除废弃表**（v9.0/v10.3）：

   - `portal_mock_interview` / `portal_mock_interview_qa`（v10.3 AI 模拟面试下线）

   - `portal_pk_challenge` / `portal_circle*`（v9.0 已在之前版本移除，仅注释）

3. **修改表结构**（4 张表）：

   - `portal_category`：+ `nav_badge varchar(20)` 字段（v10.4 导航徽章）

   - `portal_interview_resume_template`：+ `preview_images text` 字段（v10.2 多图预览）

   - `portal_user_stats`：- `mock_interview_count` / `avg_mock_score` 2 字段（v10.3 删除）

   - `ai_model_config`：`model_type` 注释更新为含 `asr/tts`（v10.1 语音面试官）

4. **追加 5 张新表**（v8.1\~v10.1）：

   - `sys_audit_task`（v8.1 统一审核任务表）

   - `sys_job_scan_issue`（v8.1 定时任务扫描结果表）

   - `portal_import_template_config`（v8.2 导入模板字段配置表）

   - `portal_voice_interview`（v10.1 语音面试会话主表）

   - `portal_voice_interview_qa`（v10.1 语音面试问答表，含追问链）

5. **V10.x 数据合并段**（幂等 INSERT，位于结尾设置之前）：

   - **菜单重组**（v10.4）：5 个一级菜单重命名（智能AI→AI能力 / 系统管理→系统设置 / 面试指南→面试管理 / 读书空间→学习管理 / 商业化→用户运营）+ 新建工作台一级菜单

   - **栏目重构**（v10.4）：新建 3 个一级栏目（学习中心/阅读空间/创作互动）+ AI 语音面试官子项（带 NEW 徽章）

   - **字典注册**（v9.6+v10.1+v10.2）：29 类字典类型注册（sys\_dict\_type）+ 核心字典数据（sys\_dict\_data，含支付状态/支付渠道/语音面试状态/面试官风格/提示级别/简历模板分类等）

**保留的基础数据**：系统配置/部门/角色/用户/岗位/菜单/栏目/标签/字典/友链/成长规则/帮助分类/面试分类/成就/岗位/任务

**备份**：原文件备份为 `init_v7.8.sql.bak`

**遗留 TODO**：

- 完整的 29 类字典数据（v9.6 注册了 27 类，每类有多个 dict\_data 值）当前仅注册了核心几类（支付状态/支付渠道/语音面试/简历分类），其余字典的 dict\_data 值需要从 upgrade\_v9.6\_admin\_optimize.sql 补充

- v8.1\~v10.2 新增的菜单项（VIP/钱包/征文/提示词/语音面试/简历模板/导入模板等二级菜单和按钮）尚未合并到 init 脚本，需要从对应 upgrade 脚本补充

- v10.0 面试题种子数据（1000 道）和知识库种子数据（3 个库）保留为独立脚本，未合并到 init

***

## v9.6 (2026-08-16) 后台全面体检：菜单收敛 + 27类业务字典 + 前台字典化（"前台数据皆有后台管理"）

### 体检发现（三线并行分析）

- 后台 9 个一级目录、\~40 个 C 菜单、8 个 Tab 容器；SQL 注册但 views 缺失 = 0（无空白页风险）

- 完全孤儿页面 2 个：cms/contest（征文活动，前台 /contests 正在消费其数据）、cms/prompt（写作提示词）

- 系统字典仅 15 个 RuoYi 框架自带，业务字典 0；cms/portal/ai 页面 useDict 使用 0 处（全硬编码）

- 后端 PaymentStatus 枚举 5 态 vs 前端订单/打赏下拉仅 3 态（closed/failed 缺失，无法筛选）

### 菜单收敛（upgrade\_v9.6\_admin\_optimize.sql 第一\~四节）

- 🐛 **知识中心 M→C**：init 注册为 M 目录但填了 component → RuoYi M 类型不加载页面，Tab 容器永不渲染，点击空白；改为 C 菜单

- 🗑️ **审核入口三重冗余收敛**：内容审核中心（内部已含待办/已办/全部 3 Tab，查 sys\_audit\_task）vs 任务管理>我的待办/我的已办同表同逻辑 → 后两者隐藏（保留路由权限），唯一入口=内容审核中心

- 🗑️ **创作者认证目录扁平化**：目录下单挂 1 个子菜单冗余 → 认证审核直挂内容管理（path=certification），删空目录

- ✨ **孤儿页面注册菜单**：征文活动（cms:contest:list + 4 按钮）、写作提示词（cms:writing-prompt:list + 4 按钮）挂内容管理——前台 /contests 数据从此有后台维护入口

- ℹ️ 消息中心 vs 通知管理：查证消息中心已是"私信/通知"双 Tab，通知管理独有 CRUD+广播发送，定位不同（用户消息处理 vs 内容发布），**保留不合并**

### 业务字典 27 类（upgrade\_v9.6\_admin\_optimize.sql 第五节，104 条数据）

- 支付/交易域：portal\_pay\_status(5态补全closed/failed)/portal\_pay\_channel/portal\_tip\_target\_type/portal\_wallet\_txn\_type

- 内容域：cms\_article\_status/cms\_column\_status/cms\_topic\_status(5态)/cms\_contest\_status

- 审核域：cms\_audit\_task\_type(8类对齐枚举)/cms\_audit\_task\_status

- 反馈举报域：cms\_feedback\_type/cms\_report\_type/cms\_handle\_status(反馈+举报共用)

- 读书域：portal\_book\_type/portal\_book\_serial\_status/portal\_access\_type/portal\_common\_status

- 学习域：portal\_study\_plan\_type/portal\_study\_plan\_status/portal\_wrong\_question\_status

- 面试域：portal\_question\_difficulty/portal\_question\_type/portal\_resume\_category/portal\_mock\_scene

- 运营域：portal\_ad\_slot\_key/cms\_vip\_status/sys\_login\_type

- 字典值与后端枚举 code 一一对齐（新增状态改字典即可，前后台同步生效）

### 后端新增

- `com.moyun.portal.controller.PortalDictController` — 前台免登录字典接口

  - GET /portal/dict/{dictType}、GET /portal/dict/types?types=a,b,c（批量）

  - 安全：白名单仅放行 portal\_/cms\_ 前缀字典（防泄露系统字典）；@Anonymous + PortalSecurityConfig GET /portal/dict/\*\* permitAll 双通道；走 DictUtils 缓存

- PortalSecurityConfig 增加字典接口白名单

### Admin 字典化改造（18 个页面）

cms/order、tip、vip、wallet、ad、audit-center、feedback、report、article、topic、column、contest、interview/question、portal/book、bookList、studyPlan、wrongQuestion、monitor/logininfor

- 硬编码 el-option/JS 常量数组 → proxy.useDict() + dict-tag，删除各页面本地映射函数（statusLabel/getStatusText/taskTypeOptions 等）

### Portal 前台字典化 + 清理

- 新增 `src/api/dict.ts` + `src/composables/useDictData.ts`（模块级缓存、失败静默、本地 DEFAULT 兜底、dictBadgeClass 色彩映射）

- 6 页面接入字典：QuestionListPage/QuestionDetailPage/InterviewPage（难度+题型，消除 3 处重复硬编码）、ResumeTemplatePage（分类）、ReportFeedback（举报/反馈类型）、MockInterviewPage（场景预设）

- PublishPage 分类：查证已是后端优先（categories.ts 此前零引用），本轮补充三级兜底链路（后端失败/空数据/过滤后为空 → 本地 categories.ts 兜底）

- 删除 `src/data/mockData.ts`（grep 实测零代码引用）

### 验证

- ✅ moyun-server `mvn compile` 通过

- ✅ moyun-admin-vue `npm run build:prod` 通过

- ✅ moyun-portal `npm run build`（vue-tsc + vite）通过

### SQL 执行顺序（线下部署）

`init_v7.8.sql` → `upgrade_v8.1_audit_unified.sql` → `upgrade_v8.2_import_template.sql` → `upgrade_v9.0_admin_refactor.sql` → `upgrade_v9.5_merge.sql` → `upgrade_v9.6_admin_optimize.sql`（均幂等）

***

## v9.5 (2026-08-16) 分支合并：main-dev-article × moyun-dev-kouzi（后续开发基线）

### 合并决策

- **策略**：不使用 `git merge`（避免回引已删除的圈子/PK），采用精确文件抽取

- **定位**：内容先行引流 → 体验留存 → 优质内容促进消费；游客首页必须内容丰富

- **圈子/PK**：保持 v9.0 删除状态，本次未回引任何残留

### 主要变化

- 🏠 **首页回归内容型**（从 main-dev-article 抽取 `HomePage.vue`）

  - 文章轮播 + 精选文章 + 热门文章 + 分类导航 + 作者榜 + 读书/面试双导流 + 友情链接

  - 游客未登录即可看到丰富内容，解决"扣子版首页内容太少且混乱"的问题

- 💰 **打赏流水恢复**（从 v5.2 抽取 Tip Admin，挂载方式变更）

  - 不建独立菜单，作为"交易管理"页第二个 Tab（付费订单 + 打赏流水）

  - 后端：CmsTipController / ICmsTipService / CmsTipServiceImpl（复用现有 PortalTipOrderMapper，未新增 Mapper）

  - 权限：`upgrade_v9.5_merge.sql` 幂等恢复 `portal:tip:list/query` 为"交易管理"菜单下 F 按钮权限

- 🐛 **修复 CodeEditor.vue 构建阻断**（既有 bug，与合并无关但阻断编译）

  - monaco-editor 0.53+ 的 exports 重写子路径：`monaco-editor/esm/vs/...` → `monaco-editor/...`

  - `new URL(裸模块, import.meta.url)` 改为 Vite 官方 `?worker` 动态导入（getWorker 返回 Promise<Worker>）

- 🐛 **修复"学习辅助"菜单 404 + 归属错位**（init\_v7.8 脚本历史遗留，线下验证发现）

  - 根因：第8节兜底在"内容管理"下注册 `path=learn-aux`（perms=portal:learn-aux:list），v7.13/7.15 又在"读书空间"下注册同名 path → RuoYi-Vue3 按 path 生成同名路由（Learn-aux），vue-router 4 同名 addRoute 移除先注册者 → `/cms/learn-aux` 被 `/book/learn-aux` 覆盖，点击 404

  - 修复：init\_v7.8.sql 末尾追加"六(补)"清理段 + upgrade\_v9.5\_merge.sql 第五节，删除重复菜单并将角色授权转移给正式菜单（均幂等）

  - 归属调整：学习辅助（学习计划"每日刷题"为主 + 错题本 100% 源于题库）数据产自面试指南题库体系 → 正式菜单迁至"面试指南"目录（order=9），路由变为 `/interview/learn-aux`

- 🐛 **去重交易入口：删除"财务/付费订单"独立菜单**（线下验证发现）

  - 根因：init 第十六节注册"财务(finance)/付费订单"（component=cms/order/index，已下线），v9.0 第七节迁移条件为 parent\_id=内容管理，而其挂财务目录 → 未迁移成死角；"交易管理"Tab 第一个 Tab 引用同一组件 → 功能完全重复

  - 修复（v9.5 第六节）：`portal:order:list/query` 由独立菜单转为"交易管理"下 F 按钮（CmsOrderController 接口依赖，Tab 内付费订单 Tab 需要，非超管 403 防护）；物理删除"财务"目录及残余 C 菜单

  - 附带修正：v9.5 第一节 tip 按钮挂载点由"付费订单"改为"交易管理"菜单（原挂载点为已下线菜单，非超管授权入口不可达）

  - 前端组件无冗余：cms/order/index.vue 与 cms/tip/index.vue 均被交易管理 Tab 容器引用（共用），不删

### 新增文件

- `moyun-portal/src/pages/HomePage.vue` — 内容型首页（替换）

- `moyun-admin-vue/src/api/cms/tip.js` — 打赏流水 API

- `moyun-admin-vue/src/views/cms/tip/index.vue` — 打赏流水管理页（Tab）

- `moyun-server/.../ext/cms/controller/CmsTipController.java` — 打赏流水查询接口

- `moyun-server/.../ext/cms/service/ICmsTipService.java` + `impl/CmsTipServiceImpl.java`

- `moyun-server/src/main/resources/sql/upgrade_v9.5_merge.sql` — 权限恢复脚本（幂等）

- `docs/05_测试清单.md`、`docs/07_工程质量检讨与开发进度_v5.2.md`、`docs/08_项目优缺点与改进建议.md`（自 main-dev-article 抽取）

### 修改文件

- `moyun-admin-vue/src/views/cms/transaction/index.vue` — 增加打赏流水 Tab

- `moyun-portal/src/components/CodeEditor.vue` — monaco worker 导入修复

### 验证

- ✅ moyun-server `mvn compile` 通过

- ✅ moyun-admin-vue `npm run build:prod` 通过

- ✅ moyun-portal `npm run build`（vue-tsc + vite）通过

- ✅ 遗漏检查 8/8：首页依赖 18 文件/12 方法、7 个后端接口、商业化三件套、无圈子/PK 残留路由与引用

### SQL 执行顺序（线下部署）

`init_v7.8.sql` → `upgrade_v8.1_audit_unified.sql` → `upgrade_v8.2_import_template.sql` → `upgrade_v9.0_admin_refactor.sql` → `upgrade_v9.5_merge.sql` → `upgrade_v9.6_admin_optimize.sql`（均幂等；v9.6 起补齐 27 类业务字典）

***

## v9.0 (2026-08-15) 平台重构：删除PK/圈子 + 首页改版 + 认证分级 + Admin增强

### 主要变化

- 🗑️ **模块删除**（彻底删除：代码 + 数据库表 + 菜单）

  - PK 对战模块：5个Java文件 + learnStats.ts PK代码

  - 圈子社交模块：17个Java文件（Controller/Service/Entity/Mapper/VO/Query）

  - Tip Admin 残留：3个Java文件 + 2个前端文件（保留Portal端付费阅读）

- 🎨 **首页5屏改版**（从9屏精简为5屏）

  - 双态 Hero（游客看平台价值3卡片+CTA / 登录看成长仪表盘4指标+CTA）

  - 面试+学习左右并列（AI模拟面试入口+热门题目+刷题日历+知识图谱+排行榜）

  - 读书+文章+话题三栏内容空间

  - 成长时间线（情感锚点，登录看月度数据+时间线，游客看注册引导）

  - 社区动态+热门标签+友情链接精简收尾

  - 删除：名家录（冷启动无数据）、按主题探索（合并到文章）

- 📱 **MobileTabBar 改版**

  - "读书"标签改为"面试"标签（path: /interview, icon: briefcase）

- 🔐 **认证分级**

  - 话题发布从"需创作者认证"降级为"仅需登录"

  - 文章/专栏/面试经验保持创作者认证要求不变

- 🛠️ **Admin 后台新增**

  - VIP套餐管理：CmsVipController + vip.js + vip/index.vue

  - 钱包管理：CmsWalletController + wallet.js + wallet/index.vue（双Tab：钱包列表+交易流水）

  - 业务仪表板：dashboard/index.vue（核心指标+登录趋势+发布趋势+分类排名+热门文章+审计待办）

  - growth-config 5 Tab 统一：规则+成就+徽章+日志+用户

- 🗃️ **SQL 彻底清理**

  - DROP TABLE：portal\_pk\_challenge / portal\_circle\_post / portal\_circle\_member / portal\_circle

  - 物理删除 sys\_menu + sys\_role\_menu（tip/pk/circle）

  - init\_v7.8.sql 清理PK建表语句

  - 新增菜单：VIP/钱包/仪表板 + 按钮权限 + 角色分配 + 菜单重新排序

### 新增文件

- `moyun-server/.../ext/cms/controller/CmsVipController.java` — VIP套餐CRUD

- `moyun-server/.../ext/cms/controller/CmsWalletController.java` — 钱包+交易流水查询

- `moyun-admin-vue/src/api/cms/vip.js` — VIP API

- `moyun-admin-vue/src/api/cms/wallet.js` — 钱包 API

- `moyun-admin-vue/src/views/cms/vip/index.vue` — VIP套餐管理页

- `moyun-admin-vue/src/views/cms/wallet/index.vue` — 钱包管理页（双Tab）

- `moyun-admin-vue/src/views/cms/dashboard/index.vue` — 业务仪表板

- `moyun-server/src/main/resources/sql/upgrade_v9.0_admin_refactor.sql` — v9.0升级脚本（14部分，幂等）

- `REVIEW_REPORT.md` — 全栈代码评审报告（10章）

- `moyun-admin-refactor-v9.patch` — v9.0完整变更补丁（10985行，43个文件）

### 删除文件

- PK模块：PortalPkController / IPkService / PortalPkChallengeServiceImpl / PortalPkChallenge / PortalPkChallengeMapper

- 圈子模块：PortalCircleController / CmsCircleController / ICircleService / CircleServiceImpl / PortalCircle / PortalCircleMember / PortalCirclePost / PortalCircleMapper / PortalCircleMemberMapper / PortalCirclePostMapper / PortalCircleMapper.xml / PortalCirclePostMapper.xml / CircleVO / CircleListItemVO / CircleMemberVO / CirclePostVO / CircleQuery

- Tip Admin：CmsTipController / ICmsTipService / CmsTipServiceImpl / tip.js / tip/index.vue

### 主要变化

- 🐳 Docker 沙箱判题引擎（生产环境替换 ProcessJudgeEngine）

  - CPU/内存/PID 限制（`--cpus` / `--memory` / `--pids-limit`）

  - 网络隔离（`--network=none`）、只读根文件系统（`--read-only` + tmpfs）

  - 全 capability 丢弃（`--cap-drop=ALL`）+ no-new-privileges

  - 通过 `@ConditionalOnProperty(moyun.judge.engine-type=docker)` 切换，无需改代码

- ⚡ 异步判题（Redis 队列 + Worker + 前端轮询）

  - `JudgeQueueService`：Redis List LPUSH/BLPOP

  - `JudgeAsyncWorker`：固定线程池消费，支持重试与 SE 兜底

  - PENDING 状态缓存（TTL 600s）避免每次回查 DB

  - 复用既有 `GET /portal/judge/result/{id}` 轮询入口

- 🛠️ CMS 用例管理界面（admin 仓库）

  - 隐式路由 `/cms/interview/testCase/:questionId`

  - 题目列表新增「用例」按钮跳转

  - 用例增删改查 + 是否样例切换 + 排序

- 🐛 异常报错修复

  - `ProcessJudgeEngine` 9 处 JudgeResult private 字段直接访问 → setter 调用（编译错误）

  - `JudgeQueueService` 反序列化回退路径（防御性 LinkedHashMap → POJO）

  - `DockerJudgeEngine` 移除 `runCasesInContainer` 未使用的 `image` 参数

### 新增文件

- `moyun-server/.../portal/judge/JudgeProperties.java` — 判题配置（@ConfigurationProperties）

- `moyun-server/.../portal/judge/JudgeTask.java` — 队列任务负载

- `moyun-server/.../portal/judge/JudgeQueueService.java` — Redis 队列封装

- `moyun-server/.../portal/judge/DockerJudgeEngine.java` — Docker 沙箱判题引擎

- `moyun-server/.../portal/judge/JudgeAsyncWorker.java` — 异步判题 Worker

- `moyun-admin-vue/src/views/cms/interview/testCase/index.vue` — CMS 用例管理页

### 修改文件

- `moyun-server/.../portal/judge/JudgeEngine.java` — 接口文档更新

- `moyun-server/.../portal/judge/ProcessJudgeEngine.java` — 编译错误修复 + @ConditionalOnProperty

- `moyun-server/.../portal/service/impl/PortalJudgeServiceImpl.java` — 同步/异步分支

- `moyun-server/.../portal/service/IPortalJudgeService.java` — 接口文档

- `moyun-server/.../portal/controller/PortalJudgeController.java` — Swagger 文档

- `moyun-admin-vue/src/api/cms/interview.js` — 用例 CRUD API

- `moyun-admin-vue/src/router/index.js` — 用例管理路由

- `moyun-admin-vue/src/views/cms/interview/question/index.vue` — 「用例」按钮

- `moyun-server/src/main/resources/application-dev.yaml` — moyun.judge 完整配置

- `moyun-server/src/main/resources/application-prod.yaml.example` — 同上（生产环境变量化）

- `docs/11_面试指南后续迭代规划.md` — 验收标准勾选 + v8.0 已交付清单

### 配置示例（生产环境）

```yaml
moyun:
  judge:
    engine-type: docker            # 切换 Docker 沙箱
    timeout-ms: 2000
    memory-limit-mb: 256
    async-enabled: true            # 启用异步判题
    worker:
      concurrency: 2
    docker:
      images:
        javascript: node:20-alpine
        python: python:3.11-slim
        # ...
```

***

## v8.2 (2026-08-14) 通用导入模板 + 题库导入导出

- 新建 `portal_import_template_config` 表：各业务（题库/标签/文章/面经/笔记）导入模板字段动态配置，无需改代码即可调整列名/必填/下拉值

- 新增题库导入导出按钮权限：`cms:interview:import` / `cms:interview:export`

- 失败行可重导：ImportResult 返回成功/失败统计 + 失败明细，修正后可重导

- 脚本：`upgrade_v8.2_import_template.sql`（幂等）

## v8.1 (2026-08-14) 审核模块统一整合

- 新建统一审核任务表 `sys_audit_task`（替代分散的各业务表 status 聚合），提交/处理审核时与业务表 status 双写

- 新建定时任务扫描结果表 `sys_job_scan_issue`（定时任务异常/待处理项记录）

- 数据回填：各业务表现有 pending 记录回填至 sys\_audit\_task

- 菜单调整：新增「任务管理」一级菜单（定时任务/我的待办/我的已办/扫描结果），audit-center 保留为「全部审核」入口

- 依赖提示：Admin 业务仪表板"审计待办"依赖 sys\_audit\_task，**不执行此脚本仪表板会报错**

- 脚本：`upgrade_v8.1_audit_unified.sql`（幂等）

## v5.2 (2026-07-19) 安全加固 + SQL 整理 + 文档重建

### 主要变化

- 🔒 安全加固：修复 5 项致命 + 17 项高级安全问题

- 🗂️ SQL 整理：90 个原始脚本归类为 12 个整理文件，169 张表合并 ALTER 到最终 CREATE

- 📚 文档重建：新增 01\_项目介绍.md / 08\_项目优缺点与改进建议.md / 09\_开发进度.md

### 致命问题修复（5 项）

- C-1 在线代码执行 RCE → 临时下线 `/portal/code/run` 返回 503

- C-2 门户密码明文存储 → 全链路 BCrypt（8 处）+ Controller 层加密

- C-3 JWT 硬编码密钥 → 删 DEV\_FALLBACK\_SECRET，fail-fast，阈值 64 字符（HS512 要求）

- C-4 文章 IDOR → 6 处 checkOwnership + 版本接口 3 处

- C-5 凭据硬编码 → 全部 ${ENV\_VAR} 化 + application-prod.yaml.example

### 高级问题修复（17 项）

- H-1 付费购买未扣费 → 抛异常拦截（待支付通道）

- H-2 计数器非原子 → 面试 8 处 + 评论 3 处 + 金句 1 处 + 文章 4 处改原子 Mapper

- H-3 Controller @Transactional → 3 处移除

- H-4 删除不级联 → cascadeDeleteByArticleId 清理 5 张关联表

- H-5 评论越权 → SecurityConfig + Controller + Service 三层修复

- H-6 版本接口越权 → 3 处 checkOwnership

- H-7 文件上传无白名单 → 19 扩展名白名单

- H-8 Flowable 吞异常 → 4 处改 error

- H-10 异常脱敏 → GlobalExceptionHandler 通用提示

- H-11 SSRF → validateUrl 拦截内网

- H-12 无限流 → login/register/comment 三处 @RateLimiter

- H-13 Druid/Swagger 暴露 → knife4j.production 环境变量化 + druid ADMIN 限制

- H-14 MarkdownRenderer XSS → sanitizeHTML + 白名单

- H-15 路由不刷新 → ExperiencePublishPage 补 watch

- H-16 SQL 非幂等 → 19 号脚本 42 条 INSERT IGNORE

- H-17 测试数据混入 → 警告 + README 标注

### SQL 整理

- 90 个原始脚本 → 12 个整理文件（按 9 业务域 + 菜单 + 数据分类）

- 169 张表合并 ALTER 到最终 CREATE TABLE IF NOT EXISTS

- 深度复查补齐 16 张遗漏表（7 面试 + 2 系统 + 4 商业化 + 2 帮助中心 + 1 通知不一致）

- 菜单 INSERT IGNORE 幂等，种子数据与测试数据分离

### 文档重建

- 新增 docs/01\_项目介绍.md（项目全貌，9 大业务领域）

- 新增 docs/08\_项目优缺点与改进建议.md（后期开发指导）

- 新增 docs/09\_开发进度.md（各模块进度保留）

### 相关文件

- 后端：\~18 个 Java 文件修改（Controller/Service/Mapper/Config/Handler/Util）

- 后端配置：3 个文件（application.yaml + application-dev.yaml + application-prod.yaml.example）

- 前端：4 个文件（MarkdownRenderer + MarkdownEditor + security.ts + ExperiencePublishPage）

- SQL：sql-organized/ 目录 12 文件 + README

- 文档：docs/ 3 个新文件 + devlog + README 更新

***

## v5.0 (2026-07-01) 积分打赏 MVP + 相关推荐 + 广告位基建

### 主要变化

- 💰 积分打赏 MVP：用积分替代真实资金，绕开支付资质

- 📌 详情页相关推荐：复用 RelatedArticleCard

- 📢 自研广告位基建：AdCard + 后端全套 + admin CRUD

### 相关文件

- PortalTipServiceImpl.java（积分扣减闭环）

- TipModal.vue（公共组件抽取）

- ArticleDetailPage.vue / ColumnDetailPage.vue（替换为 TipModal）

- RelatedArticleCard.vue（复用）

- AdCard.vue + api/ad.ts

- PortalAdSlot 全套（实体/Mapper/Service/Controller）

- CmsAdSlotController + cms/ad/index.vue

- SQL 89（成长规则）+ SQL 90（广告位）

***

## v4.0.5 (2026-06-26) 全面功能排查清单

### 主要变化

- 📋 新增：[10\_功能排查清单.md](./10_功能排查清单.md) - 全面功能细节排查清单

  - 按"模块 > 页面 > 接口 > 组件"4 层级组织

  - 覆盖前台门户（17 模块 153 项）、后台管理（20 模块 178 项）、后端服务（9 模块 55 项），共 386 项测试点

  - 每项含核心业务规则、问题描述、测试结果、测试时间、遗留问题、关联问题

  - 汇总 12 个已知问题（BUG-A \~ BUG-L），按严重度分级

  - 提供测试执行顺序与修复优先级建议

### 调研中发现的新问题（BUG-A \~ BUG-L）

- 🟠 BUG-A：首页聚合 VO 字段错位（hotArticles 被赋值为 latestArticles）

- 🟡 BUG-B：作者列表关注按钮占位未接入

- 🟡 BUG-C：搜索页走 articleApi 而非 /portal/search

- 🟠 BUG-D：短信登录前端模拟未实现

- 🟡 BUG-E：第三方登录未实现

- 🟠 BUG-F：注册手机号字段语义错位（填入 email 字段）

- 🟡 BUG-G：账号注销仅前端模拟

- 🟠 BUG-H：tag 路由实际传 categoryName 参数

- 🟠 BUG-I：举报图片上传 UI 占位未接入

- 🟠 BUG-J：审核通知硬编码 userType=portal

- 🟠 BUG-K：关注成长事件 module 错误（传 article 而非 user）

- 🟠 BUG-L：VIP 加成仅对当前登录用户生效

### 相关文件

- `docs/10_功能排查清单.md`（新建）

***

## v4.0.4 (2026-06-26) 功能细节修复（审核通知 + 草稿自动保存 + 配置缓存 + 标签推荐）

### 主要变化

- 🐛 修复：审核结果通知作者闭环（BUG-002 遗留）

  - `CmsArticleServiceImpl.auditArticle` 审核通过/拒绝后发送站内信通知作者

  - 非阻塞设计：通知失败不影响审核主流程

  - `SysNotification` 类型 system，scope=user，userType=portal

- ✨ 新增：前台发布页草稿自动保存

  - `PublishPage.vue` 启动 30 秒定时器自动调用 `saveDraft(isAuto=true)`

  - 仅当标题或内容非空时才触发，避免空页面频繁保存

  - `onUnmounted` 清理定时器

- 🐛 修复：SysConfig 缓存加载/清空（TODO 空实现）

  - `selectConfigByKey` 先查 Redis 缓存（`sys:config:{key}`），未命中回源 DB 并回填

  - `loadingConfigCache`：全量加载 sys\_config 到 Redis

  - `clearConfigCache`：删除所有 `sys:config:*` 键

- ⚡ 优化：标签推荐逻辑利用 title/category 参数

  - `TagQuery` 新增 `categoryId` 字段

  - `PortalTagMapper.xml` 添加 `categoryId` 关联查询（EXISTS portal\_entity\_tag + portal\_article）

  - `PortalTagController.getRecommendTags` 动态构建缓存键，title 模糊匹配标签名，category 筛选该分类下文章使用过的标签

### 相关文件

- `moyun-server/.../CmsArticleServiceImpl.java`

- `moyun-portal/src/pages/PublishPage.vue`

- `moyun-server/.../SysConfigServiceImpl.java`

- `moyun-server/.../PortalTagController.java`

- `moyun-server/.../TagQuery.java`

- `moyun-server/.../PortalTagMapper.xml`

***

## v4.0.3 (2026-06-26) 文章模块优化（发布链路完整性 + 轮播图联动）

### 主要变化

- ⚡ 优化：前台发布页标签推荐防抖

  - `PublishPage.vue` 标签推荐 `watch` 每输入一字符即触发 `getRecommendTags` 请求，过于频繁

  - 新增 `scheduleTagSuggestions`（500ms 防抖 + 标题≥5 字符或已选分类才触发），`onUnmounted` 清理 pending timer

- ⚡ 优化：后端标签推荐 Redis 缓存

  - `PortalTagController.getRecommendTags` 每次查库且忽略 title/category 参数

  - 单一缓存键 `portal:tag:recommend:top10`，TTL 10 分钟；标签增/改/删时主动清除缓存

- 🐛 修复（P0）：后台发布作者信息空实现导致插入失败

  - `CmsArticleServiceImpl.fillAdminAuthorInfo` 原为空实现，DB `author_id NOT NULL`，未选作者时插入失败

  - 实现：前端已传 `authorId` 则尊重；否则 `SecurityUtils.getUserId()` 反查 `portal_user.user_id`，未关联则自动建户（携带 sys\_user 基础信息，role=admin）；`insertArticle` 加 `@Transactional`

- ✨ 新增：后台发布 slug / category\_path 维护（与前台链路对齐）

  - `fillCategoryPath`（复用 `IPortalCategoryService`）+ `fillSlug`/`generateSlugFromTitle`/`sanitizeSlug`/`ensureSlugUnique`（保证 `uk_slug` 唯一）

- ✨ 新增：后台 CMS 文章编辑页字段丰富化（`cms/article/edit.vue`）

  - 新增维护字段：`publishedAt`、`slug`、`isCategoryRecommended`

  - 轮播图动态联动：`isCarousel=true` 时 `cover` 动态必填 + 1920×600 提示；`false` 时可选 + 800×450 提示

  - `rules` 改为 `computed`，`cover` 必填性随 `isCarousel` 联动；`watch(isCarousel)` 切换时重校验封面

- 📊 评估：文章模块用户/管理员发布链路完整性（详见 `docs/07_更新日志.md` v4.0 评估章节）

### 验证

- 后端 `mvn -o compile -DskipTests`：BUILD SUCCESS

- Portal `npm run check`（vue-tsc -b）：exit 0，零类型错误

- Admin `npm run build:prod`：exit 0

### 相关文件

- `moyun-portal/src/pages/PublishPage.vue`

- `moyun-server/src/main/java/com/moyun/portal/controller/PortalTagController.java`

- `moyun-server/src/main/java/com/moyun/ext/cms/service/impl/CmsArticleServiceImpl.java`

- `moyun-admin-vue/src/views/cms/article/edit.vue`

- `docs/07_更新日志.md` / `docs/devlog.md`

***

## v4.0.2 (2026-06-26) 三端深度复查 P1 修复 + 文档维护

### 主要变化

- 🐛 修复：三端深度复查发现的 4 个 P1 问题

  - `38_init_report_feedback_menu.sql`：`portal_report.description` 列长 `varchar(1000)` → `varchar(2000)`，与实体 `@Size(max=2000)` 对齐（避免 1001-2000 字举报描述写入失败）

  - `40_fix_bugs_v4.sql`：新增 `ALTER TABLE portal_report MODIFY COLUMN description varchar(2000)` 供已部署环境升级

  - `AuthorPage.vue`：`loadAuthorData` 中已登录且非自己主页时调用 `followApi.checkFollow` 初始化 `isFollowing` 状态（修复已关注用户按钮显示错误 + 粉丝数不同步）

  - `notification/index.vue`：`remoteUserSearch` 的 `response.rows` → `response.data.records`（修复用户搜索下拉永远为空）

  - `notification/index.vue`：移除"系统用户（后台管理员）"选项，下拉固定为"门户用户"并 disabled（后端 `/cms/user/list` 只查 portal\_user 表）

- 🧹 清理：删除 `api/cms/comment.js` 中无调用方的 `addComment` / `updateComment` 死代码

- 📚 文档：全面更新 `docs/` 目录文档（07\_更新日志、06\_问题修复、bugs/bug-list、devlog、05\_测试清单），补充 `sql/README.md` 的 25-40 号脚本说明

### 验证

- 后端 `mvn clean compile -DskipTests`：BUILD SUCCESS

- Portal `npm run build`（含 vue-tsc 类型检查）：built in 23.22s，零类型错误

- Admin `npm run build:prod`：exit 0

### 相关文件

- `moyun-server/src/main/resources/sql/38_init_report_feedback_menu.sql`

- `moyun-server/src/main/resources/sql/40_fix_bugs_v4.sql`

- `moyun-portal/src/pages/AuthorPage.vue`

- `moyun-admin-vue/src/views/cms/notification/index.vue`

- `moyun-admin-vue/src/api/cms/comment.js`

- `docs/07_更新日志.md` / `docs/06_问题修复.md` / `docs/bugs/bug-list.md` / `docs/devlog.md` / `docs/05_测试清单.md`

- `moyun-server/src/main/resources/sql/README.md`

***

## v4.0.1 (2026-06-26) 关注/取消关注接口整合

### 主要变化

- 🐛 修复：`POST /portal/follow/{userId}` 返回 500 "Request method 'POST' is not supported"

  - 根因：前后端接口契约不匹配。前端 `follow.ts` 调用语义化 RESTful 路径，后端 `PortalFollowController` 只有 CRUD 接口（`@PostMapping` 无路径 + `@DeleteMapping("/{ids}")` 批量删除）

- 🔧 重构：`PortalFollowController` 改为语义化 RESTful 契约（与前端 `follow.ts` 完全对齐）

  - `POST /portal/follow/{userId}` 关注（幂等）

  - `DELETE /portal/follow/{userId}` 取消关注（幂等）

  - `GET /portal/follow/check/{userId}` 检查是否已关注

  - `GET /portal/follow/{userId}/followers` 粉丝列表（分页）

  - `GET /portal/follow/{userId}/following` 关注列表（分页）

- ✅ `IPortalFollowService` 新增 `follow` / `unfollow` / `selectFollowerPage` / `selectFollowingPage` 方法

- ✅ `PortalFollowServiceImpl` 实现：

  - 事务一致性：`@Transactional(rollbackFor = Exception.class)`，关注记录 + 统计计数 + 成长事件原子操作

  - 幂等性：已关注再关注/未关注再取消均不报错，直接返回当前状态

  - 复用 `toggleFollow` 的统计更新和成长事件逻辑

- ✅ `PortalSecurityConfig` 放开 `GET /portal/follow/check/**` 及 followers/following 公开访问（允许游客浏览作者主页）

- ✅ 保留旧接口 `toggle/{userId}` / `isFollowing/{userId}` 向后兼容

### 路由冲突分析

- `/check/{userId}`、`/toggle/{userId}`、`/isFollowing/{userId}` 为字面量段，Spring AntPathMatcher 优先匹配字面量

- `/{userId}` 为变量段，优先级低于字面量段

- 无路由冲突

### 验证

- 后端 `mvn clean compile -DskipTests`：BUILD SUCCESS

### 相关文件

- `moyun-server/src/main/java/com/moyun/portal/controller/PortalFollowController.java`

- `moyun-server/src/main/java/com/moyun/portal/service/IPortalFollowService.java`

- `moyun-server/src/main/java/com/moyun/portal/service/impl/PortalFollowServiceImpl.java`

- `moyun-server/src/main/java/com/moyun/portal/config/PortalSecurityConfig.java`

***

## v4.0.0 (2026-06-26) Dashboard 改造 + 举报反馈模块 + 4 个 Bug 修复

### 主要变化

- ✅ 新增：Dashboard 首页改造

  - `SysDashboardServiceImpl.buildTodoTasks()` 新增举报/反馈待办构建逻辑

  - 待办跳转路径规范化：`/cms/article/edit?id={id}`（query 形式）、通知 `/cms/notification`

  - `businessType` 从数字编码改为枚举名

  - `refreshCache()` 增加 ZSet 键清理 + `@PreAuthorize` 权限校验

  - 前端 `index.vue` 增加 loading 绑定和错误提示

- ✅ 新增：举报反馈模块（后台管理）

  - `CmsReportController` / `CmsFeedbackController`

  - `list` 返回 `TableDataInfo`，`handle` 用 `LambdaUpdateWrapper` 防篡改

  - `PortalReport` / `PortalFeedback` 实体增加校验注解

  - SQL `38_init_report_feedback_menu.sql` 创建菜单及权限

- 🐛 修复：4 个用户反馈 Bug

  - **Bug1**：个人中心页 mock 数据 → 改用 `growthApi.getUserStatsById` 真实聚合接口

  - **Bug2**：友情链接表格字段不显示 → `CmsFriendLinkVO` 补 `@Data` 注解 + SQL 统一 status 为 0/1

  - **Bug3**：通知菜单重复 → 删除 `SysNoticeController`/`SysNotificationController` 及残留菜单，保留 `cms/notification` 唯一入口

  - **Bug4**：评论管理 404 → SQL 修正菜单 path + 统一 perms + 补齐菜单及授权

- 📦 打包：v4.0 源码包（zip 格式，4.9M，排除 node\_modules/target/dist/.git）

### 需执行的 SQL 脚本

1. `38_init_report_feedback_menu.sql` — 举报/反馈菜单
2. `39_init_flowable_menu.sql` — 流程管理菜单
3. `40_fix_bugs_v4.sql` — v4.0 综合 Bug 修复（友情链接 status + 通知菜单清理 + 评论菜单修复 + PortalReport 列长升级）

### 相关文件

- 后端：`SysDashboardServiceImpl.java`、`SysDashboardController.java`、`CmsReportController.java`、`CmsFeedbackController.java`、`PortalReport.java`、`PortalFeedback.java`、`CmsFriendLinkVO.java`

- Portal：`AuthorPage.vue`、`index.vue`

- Admin：`index.vue`、`report/index.vue`、`feedback/index.vue`、`friend-link/index.vue`

- SQL：`38_init_report_feedback_menu.sql`、`40_fix_bugs_v4.sql`、`01_moyun_init.sql`、`03_portal_init.sql`、`application.yaml`

***

## v2.1.1 (2026-06-16) Mapper XML resultMap 分层 + @Slf4j 编译修复

### 主要变化

- 🔧 修复：Cms\*Result 混用前台 Entity 问题

  - `PortalArticleMapper.xml` 的 `CmsArticleResult` type 由 `PortalArticle` 改为 `CmsArticleVO`（CMS 后台视图对象，含 `authorNickname/authorUsername/authorAvatar/categoryName/categorySlug` 等 CMS 扩展字段）

  - 同步修改 `PortalArticleMapper` 接口：`selectCmsArticlePage` 返回 `Page<CmsArticleVO>`，`selectCmsArticleList` 返回 `List<CmsArticleVO>`，`selectCmsArticleById` 返回 `CmsArticleVO`

  - **修复参数类型不匹配**：`selectCmsArticlePage/List` 参数类型由 `ArticleQuery` 改为 `CmsArticleQuery`（与 Controller/Service 一致），参数名 `@Param("params")` 保持不变，XML 无需改动

  - 同步修改 `ICmsArticleService.selectArticleById` 返回 `CmsArticleVO`

  - 同步修改 `CmsArticleServiceImpl`：去掉 `BeanUtil.copyToList` 中转，mapper 直接返回 `Page<CmsArticleVO>`；同步清理 `cn.hutool.core.bean.BeanUtil` 冗余 import

  - **设计原则**：前台查询 resultMap 用前台 Entity（如 `PortalArticleResult`），CMS 后台管理查询 resultMap 用 CMS 专用视图对象（如 `CmsArticleResult` 对应 `CmsArticleVO`），不再混用

- 🐛 修复：`PortalInterviewResumeTemplateMapper.xml` 重复块

  - 删除行 54 起的重复 `resultMap id="PortalInterviewResumeTemplateResult"`（与行 7 重复）

  - 删除行 77 起的重复 `sql id="selectPortalInterviewResumeTemplateVo"`（与行 28 重复）

  - 保留行 7 的不完整 resultMap（含 19 个字段）和行 28 的不完整 sql 块（不含 `usage_guide/tags` 列）

  - 说明：当前 `PortalInterviewServiceImpl` 不调用 mapper XML 的自定义方法（全部走 MyBatis-Plus `selectPage/selectById`），mapper XML 方法为"死代码"，保留无副作用

- 🐛 修复：6 个 `@Slf4j` 缺 import 的真编译错误

  - `HttpHelper.java`：补 `import org.slf4j.Logger;` + `import org.slf4j.LoggerFactory;`（手动声明了 `LOGGER` 字段但缺 import）

  - `SysUserServiceImpl.java` / `UserDetailsServiceImpl.java` / `GenTableServiceImpl.java` / `FlowTaskListener.java` / `FlowableUtils.java`：补 `import lombok.extern.slf4j.Slf4j;`（注：不影响用户要求的"system 稳定模块不乱改"，仅补一行 import 让 `@Slf4j` 注解生效）

### 全链路比对结果

- **service 实际调用的 mapper XML 自定义方法**（排除 BaseMapper 通用方法）：

  - 前台：`selectPortalArticlePage/List/ById`、`selectHotArticles/FeaturedArticles/CarouselArticles/RelatedArticles/LatestArticles` → 全部映射 `PortalArticleResult`（type=`PortalArticle`）✓

  - 后台：`selectCmsArticlePage/List/ById` → 映射 `CmsArticleResult`（type=`CmsArticleVO`）✓

- **其他 11 个面试 mapper XML**（Question/Experience/Category/Company/Attempt/Comment/Submission/Bookmark/各种 Like/QuestionCompany/ResumeTemplate）：service 实际未调用，全部走 MyBatis-Plus，resultMap 混用风险**不存在**

- **其他 portal mapper**（Book/Category/Tag/Notification/Order/Wallet 等）：仅暴露 `selectPortalXxxPage/List/ById` 前台方法，无 Cms\*Result 混用 ✓

### 验证

- `@Slf4j` 全量扫描：所有 40 个使用 `@Slf4j` 的文件都已正确 import `lombok.extern.slf4j.Slf4j`

- 手动 `Logger` 声明扫描：所有使用 `private static final Logger` 的文件都已正确 import `org.slf4j.Logger` + `org.slf4j.LoggerFactory`

- mapper XML resultMap type 扫描：所有 resultMap type 引用都已通过 javac 静态可达性验证

### 相关文件

- `moyun-server/src/main/resources/mapper/portal/PortalArticleMapper.xml`

- `moyun-server/src/main/resources/mapper/portal/PortalInterviewResumeTemplateMapper.xml`

- `moyun-server/src/main/java/com/moyun/portal/mapper/PortalArticleMapper.java`

- `moyun-server/src/main/java/com/moyun/ext/cms/service/ICmsArticleService.java`

- `moyun-server/src/main/java/com/moyun/ext/cms/service/impl/CmsArticleServiceImpl.java`

- `moyun-server/src/main/java/com/moyun/util/http/HttpHelper.java`

- `moyun-server/src/main/java/com/moyun/system/service/impl/SysUserServiceImpl.java`

- `moyun-server/src/main/java/com/moyun/core/security/auth/UserDetailsServiceImpl.java`

- `moyun-server/src/main/java/com/moyun/ext/generator/service/GenTableServiceImpl.java`

- `moyun-server/src/main/java/com/moyun/ext/flowable/listener/FlowTaskListener.java`

- `moyun-server/src/main/java/com/moyun/ext/flowable/flow/FlowableUtils.java`

***

## v2.1.0 (2026-06-15) 面试空间 + 通用标签模块

### 主要变化

- ✅ 新增：面试空间 13 张核心表

  - `portal_interview_category`（面试分类）

  - `portal_interview_question`（面试题目）

  - `portal_interview_submission`（提交记录）

  - `portal_interview_bookmark`（题目书签）

  - `portal_interview_question_like`（题目点赞）

  - `portal_interview_attempt`（答题尝试）

  - `portal_interview_experience`（面经文章）

  - `portal_interview_experience_like`（面经点赞）

  - `portal_interview_comment`（面试评论）

  - `portal_interview_comment_like`（评论点赞）

  - `portal_interview_resume_template`（简历模板）

  - `portal_interview_company`（面试公司）

  - `portal_interview_question_company`（题目-公司关联）

- ✅ 新增：通用标签系统

  - 新增表 `portal_entity_tag`（tag\_id + entity\_type + entity\_id 多实体绑定）

  - 扩展表 `portal_tag`，新增字段 `module` / `reference_count` 及对应索引

  - 支持按模块（article/book/common）分类与热门标签查询

- 🔧 重构：分页工具链全局统一

  - `PortalInterviewController` 继承 `BaseController`，分页调用统一为 `startPage()` / `getDataTable()`

  - 删除 `CmsInterviewController` 中私有 `PageUtils` 内部类，统一调用 `com.moyun.util.bean.PageUtils`

  - 字段命名：表名统一为 `portal_interview_question`（单数），避免复数混用

- 🐛 修复：分页参数 `page` / `pageSize` / `pageNum` 兼容对齐

- 📚 文档：`docs/08_面试空间模块设计文档.md` 新增分页设计规范与通用标签系统章节

- 🔒 安全：所有敏感操作保持 `@PreAuthorize` + `@Log` 注解

### 相关文件

- `docs/sql/portal_module_ddl_v2.sql`

- `docs/sql/portal_module_init_v2.sql`

- `docs/08_面试空间模块设计文档.md`

***

## v2.0.0 (2026-06-01) 门户内容模块基线

- ✅ 新增：`portal_article`、`portal_article_tag`、`portal_article_view` 等核心表

- ✅ 新增：`portal_category`、`portal_tag`、`portal_comment`、`portal_like`、`portal_bookmark` 基础模块

- ✅ 新增：`portal_user`、`portal_follow` 等用户与社交基础表

- 🔧 统一：Entity 使用 `@Data` + `@TableName` 的 MyBatis-Plus 风格

