# 14 · AI 语音面试官评估与落地计划

> 评估基期：2026-08-18 | 评估基线：moyun-dev-kouzi 分支 HEAD (949a2a8) | 来源方案：`AI语音面试官技术方案.md`（v1.0）
> 文档定位：基于**现有代码实际状况**的可行性评估、价值判断、不合理/风险清单，以及**分阶段可执行落地计划**（四同步原则：代码、SQL、菜单、文档同步规划）

> **实施进度速览（2026-09-01 更新）**：本方案 V10.0~V10.3 已全部交付，详见 [devlog v10.15~v10.18](file:///d:/zyg_new_work/moyun-project-document/docs/07-变更日志/devlog.md)。
>
> | 方案阶段 | 实际交付 | devlog 版本 | 增量内容 |
> |---------|---------|------------|---------|
> | Phase 0 前置依赖 | ✅ | — | 题库数据/简历引导/枚举扩展随各阶段就绪 |
> | V10.0 引擎脚手架 + MVP | ✅ | v10.15 前置轮 | ASR 实时转写（边说边打印）/ SSE 流式打字机 / TTS 队列播报 / 说话打断 / HintEngine / 五维雷达报告 |
> | V10.1 体验补强 | ✅ | v10.15 | 聆听音浪（useAudioLevel）/ 四态状态环 / AI 声波 / 静音切换 / 自动聆听 |
> | V10.2 真实链路 | ✅ | v10.17 | 真实简历库选择器 / resumeId 接通简历深挖出题（项目2+画像2+兜底1）/ 岗位自定义 / 历史面试列表+对话回放 |
> | V10.3 能力深化 | ✅（RAG 出题除外） | v10.18 | LLM 动态追问（漏洞识别/针对性追问/水平画像/引导提示）；**B8 行业 RAG 出题仍未做**（依赖知识库数据回填） |
> | — 计划外增强 | ✅ | v10.16 | 编程题接入真实 OJ（ACM 模板 7 语言+防作弊），题库练习四大断链收口 |

---

## 一、现状全景（评估的事实基座）

### 1.1 面试模块成熟度矩阵

| 能力域 | 当前成熟度 | 核心代码位置 | 关键判断 |
|--------|-----------|-------------|---------|
| 题库（分类/难度/题型/公司/标签） | 95% | `PortalInterviewServiceImpl` + [CmsInterviewController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsInterviewController.java) 题目增删改查 + 动态导入导出 | 完整闭环，支持后台维护，字典驱动 v9.6 已补齐 |
| 文本模拟面试（快练版） | ~~85%~~ 已下线 | **v10.3 已下线清理**：原 `MockInterviewServiceImpl` / `PortalMockInterviewController` / `MockInterviewPage.vue` 全部删除，能力整合进 [AI 语音面试官](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/interview/VoiceInterviewPage.vue)（`/portal/interview/voice/**`）。画像能力迁移至 `/portal/interview/profile` | 三路抽题（薄弱点+必备技能+随机）+ 规则评分逻辑已由 VoiceInterview 继承 |
| 简历管理（含 AI 评分/建议） | 80% | `ResumeEditPage.vue`（4Tab：编辑/预览/AI建议/AI评分）+ `interview.ts scoreResume/getResumeAiAdvice` | 完整 CRUD + 版本 + 导出；AI 建议依赖后端 LLM（目前 Noop） |
| 面经分享/评论/审核 | 85% | `ExperienceList/Detail/Publish/MyExperiences` 4页面 + 后台 [CmsInterviewController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsInterviewController.java) 面经 CRUD + 审核回填 `sys_audit_task` | 完整；最近 commit (949a2a8) 把面经新增/编辑的 userId 获取方式从 `null` → `SecurityUtils.getUserId()`，**这是必须修复的真实 bug，与本方案无关但值得注意** |
| 错题本/答题历史/收藏 | 80% | `MyBookmarksPage.vue` / `MyAttemptsPage.vue` + 对应 API | 可独立运行；薄弱点画像通过 `refreshWeakTags` 回写（语音面试结束时同步触发） |
| OJ 代码判题 | 70% | `QuestionDetailPage.vue` 代码编辑器 + `/portal/judge/submit` 同步返回 | 非本方案主线 |
| **用户画像/薄弱点** | **75%** | `UserProfileSnapshotServiceImpl.buildSnapshot()` + `refreshWeakTags()` | 三路召回已实现（薄弱点×最多3 + 必备技能×最多2 + 随机兜底），已由 VoiceInterview 继承调用；但 "基于简历"（40%配比）与"基于岗位必备"（30%配比）的精确权重尚未对齐方案 §3.1 |
| **AI流式/Agent/RAG** | **85%** | `ext.ai.*`：LangChain4j + `Flux<String>` / `SseEmitter` 双通道（[WorkflowController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/ai/controller/WorkflowController.java) / [DiagramStreamController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/ai/controller/DiagramStreamController.java)）；完整 `WorkflowEngine` 多步骤编排；知识库 RAG（BM25+向量混合+重排）；`TokenUsageServiceImpl` 计费 | AI 基础设施是项目里**最成熟的部分之一**，但与面试模块的 LlmClient 目前**不互通**（面试用独立 `NoopLlmClient`，AI 域用 `ModelConfigServiceImpl.createStreamingChatModel`）——这是方案集成的第一个技术断点 |
| **字典体系** | **95%** | v9.6 已补齐 27 类业务字典，面试域全覆盖：`portal_question_difficulty/type` 等；`portal_mock_scene` 已于 v10.3 随 MockInterview 下线删除；`useDictData` composable 可直接复用 | 方案 §11.4 style/scene/question_source 字典化，只需追加 3~4 个字典类型，不触碰代码 |
| **语音 ASR/TTS** | **0%** | 项目中**零** `useSpeechRecognition` / `useSpeechSynthesis` / Web Speech API 使用；SSE/流式 fetch 面试域也未用 | 浏览器端语音是 MVP 的必建基础；ModelType 枚举目前仅 `CHAT/EMBEDDING/MULTIMODAL/RERANKER`，**缺 ASR/TTS**，升级到 DashScope paraformer/cosyvoice 时需改枚举+DB |
| **限流注解** | 80% | 项目有 `@RateLimiter` 注解（com.moyun.common.annotation.RateLimiter?，注：需核实），面试接口 `/portal/interview/mock/**` 目前未打限流 | 方案 §7 start 10次/h、answer 30次/10min 需落地 |
| **权限/所有权** | 90% | Portal 端使用独立 `PortalSecurityUtils.getUserId()` 而非 `SecurityUtils.getUserId()`（后台）——注意：最近 commit 949a2a8 在面经接口**误用了后台 SecurityUtils**，对 CmsInterviewController（`cms/interview/**`，管理端）正确但 Portal 端要坚持 PortalSecurityUtils | CmsInterviewController 是管理端，SecurityUtils.getUserId 正确；PortalMockInterviewController 已用 PortalSecurityUtils，语音面试 Controller 必须遵循同样方式 |

### 1.2 关键代码断点清单（方案与实际未对齐的技术点）

| # | 方案表述 | 代码现状 | 必须改造点 |
|---|---------|---------|-----------|
| B1 | LlmClient 面试域流式接口 | `LlmClient.chat()` 同步，返回 String | 新接口 `Flux<InterviewRoundEvent> streamRound(List<ChatMessage> messages)` 或直接复用语义更清晰的 ext.ai `DynamicChatService.chat()` + 自定义 InterviewRoundHandler；避免双栈 |
| B2 | ModelConfig 支持 asr/tts | `ModelType` 枚举只有 CHAT/EMBEDDING/MULTIMODAL/RERANKER，无 ASR/TTS | 枚举扩展 + `ModelConfig` 表 upgrade SQL（幂等） + `ai_model_config` 数据录入 |
| B3 | HintEngine 引擎（提词器共享） | `HintEngine` 类**项目中不存在**；`useHintEngine` composable 也不存在 | MVP 可先用规则版（STAR框架+题目的 `tags/solution` 关键词切分即可，不依赖新代码）；正式版作为独立 `interview.engine.HintEngine` Service 交付，语音与提词器（若后做）共享 |
| B4 | portal_user_resume.projects[] 结构化简历 | 现状：`PortalUserResume` 及 VO 的字段形式需核实（目前 MyResumes/ResumeEdit 完整，大概率是 JSON 字段） | 启动面试时必须通过 `resumeService.selectById(resumeId)` + 权限校验（仅本人简历可注入），避免越权注入他人简历内容作为题源 |
| B5 | VoiceKit（useSpeechRecognition + useSpeechSynthesis） | 项目无任何语音 composable | MVP 从零新建 `useSpeechSynthesis.ts` + `useSpeechRecognition.ts`；必须处理 Chrome 60s 静默断连、interim/final 样式分级、自动重连缓冲不丢 |
| B6 | 回合状态机(IDLE→ASKING→LISTENING→ANALYZING→DECIDING→ENDED) | 当前 MockInterview 是 start(抽5题) → 串行 answer(idx×N) → finish 固定流程，**没有回合概念、没有追问链(parent_qa_id)、没有卡壳/提示** | 全新表 `portal_voice_interview` + `portal_voice_interview_qa`（方案 §6），不要尝试改造 `portal_mock_interview`——会破坏快练版稳定性 |
| B7 | 语音面试官 7 个 SSE 流式接口 | 现有 mock 接口全部是同步 POST，门户面试域没有 Flux/SSE | 新建 `PortalVoiceInterviewController`，产出类型复用 `SseEmitter`（DiagramStream/Workflow 模式成熟）而非 `Flux<String>`（与面试域 Spring MVC 栈一致） |
| B8 | 行业关联 RAG 出题（方案 §3.1 Phase 3） | 项目已有完整 `RagRetrievalServiceImpl`（BM25+向量混合重排），`ai_knowledge_library` 表齐全 | 只需在出题器 `pickQuestionsByProfile` 追加第 4 路 "行业RAG"，但前提是后台知识库中实际录入"Java面试热题/前端面试热题"等文档——**没有数据的话RAG是零输出**，需要先解决数据回填 |
| B9 | 面试官人设配置（style: 温和型/压力型/冷淡型） | 字典中有 `sys_login_type`/`cms_vip_status` 但没有 `portal_interviewer_style` | 新建字典（v9.6 规范） + `VoiceInterviewStartConfig.style` 字段；枚举不进代码，全靠字典，UI 下拉直接用 useDictData |
| B10 | 薄弱问题→错题本闭环（方案 §7 `toWrongBook`） | 错题本已有 `portal_wrong_question` 表 + 关联逻辑（`QuestionDetailPage` → 提交答案后可加入错题本）；语音版 QA 表有 `qaId` → `PortalInterviewQuestion.id` 外键 → 直接复用 `WrongQuestionService` 即可 | 只需新增 1 个接口 `POST /portal/interview/voice/qa/{qaId}/toWrongBook`，内部调用现有 `wrongQuestionService.add(userId, questionId)`，不新表 |

---

## 二、可行性评估

### 2.1 总体可行性：**可行（推荐做）**

| 维度 | 评级 | 理由 |
|------|------|------|
| **复用率** | ⭐⭐⭐⭐⭐ (高) | ①文本模拟面试的 start/answer/finish 模式、三路抽题、画像快照、规则评分可直接改造复用（方案 §3.1 40%简历+30%岗位的配比需扩展）；②AI域SSE流式、LangChain4j、WorkflowEngine、Token计费可直接复用；③字典、限流、权限、所有权校验、路由高亮、错题本全链路现成；④前端 composables(useToast/useDictData/useApiCall/useConfirmModal) 直接复用 |
| **实现成本** | ⭐⭐⭐ (中等) | 7 个新接口、2 张新表、1 个新 Controller、1 个新 Service 层、1 个新 Vue 页面(VoiceInterviewPage) + 3 个 composable(useSpeechRecognition/Synthesis/Hint)；ModelType 枚举扩展 + 字典新增 3~4 类；总计约 2500~3500 行新增代码（MVP） |
| **用户价值** | ⭐⭐⭐⭐⭐ (极高) | 文本快练版 80 分的体验提升到 110 分；真实感、反馈即时性、自适应追问链是文本版根本没有的；对"面试学习域"这一核心入口是**强差异化 + 高留存锚点**，可以把"题库→模拟面试→AI语音"升级为明确的漏斗路径：`题库刷题 (60%用户) → 文本模拟面试(25%) → AI语音面试(10%)`，每层转化带来更高 ARPU |
| **技术债务** | ⭐⭐⭐ (可控) | 新增代码量约为现有面试模块的 25%，不会显著冲击主代码；关键是**不要改造已有 portal_mock_interview，独立 2 张新表**，避免破坏存量 |
| **合规风险** | ⭐⭐⭐ (可控) | 隐私策略明确：不存音频，存转写文本；Chrome 麦克风权限需 HTTPS（生产环境要配置，本地开发 localhost 豁免） |
| **硬件依赖** | ⭐⭐⭐ (可降级) | MVP 用浏览器 Web Speech API，零后端成本；如果浏览器不支持或无麦克风，走纯文字模式（textarea 作答，其余回合流不变），保证全栈可用 |

### 2.2 价值分层（与现有面试模块的关系）

```
面试学习域（新漏斗）
├── 面试题库 / 面经 / 错题本 / 我的答题         （入口层，用户60%）
│     └─ 现有，稳定，不改动
├── AI 模拟面试 · 文本快练版 MockInterviewPage   （转化层，用户25%）
│     └─ 现有稳定，保留为"快练模式"，不做语音追问
└── 🆕 AI 语音面试官 VoiceInterviewPage           （体验跃迁层，用户10% → 付费意愿最强）
      └─ 追问链 / 语音 / 自适应 / 报告 / 错题本闭环
         └─ 与提词器共享 HintEngine + VoiceKit（提词器文档 13 当前不存在，
            建议先做语音面试官 MVP 的 HintEngine 1.0 版，后续提词器直接复用，
            而不是"先等提词器做好再启动"——见三·7）
```

**推荐定位**：语音面试官是"旗舰产品"，文本快练版是"日常刷题工具"，两者**不是替代而是互补**。
- 文本快练 = 批量过题（5分钟5题，快速扫知识盲区）
- 语音面试 = 深度练习（15分钟5主问+追问，练表达练心态）

---

## 三、不合理点 / 风险 / 新建议（从方案角度的批判性评估）

### 3.1 方案中不合理或建议调整的部分（共 10 条）

| # | 原方案表述 | 问题 / 不合理 | 建议 |
|---|-----------|--------------|------|
| 1 | **§0 产品矩阵"引擎层三产品共享"** + **§11 里程碑 V10.0 先做提词器 MVP，V10.1 再做语音** | 提词器文档 13 目前**根本不存在**（docs 里没有 `13_*` 文件，Grep "提词器"零命中）——**依赖一个不存在的前置交付物是最大风险**；而且 `useSpeechSynthesis/Recognition/HintEngine` 三个 composable 都是从零建，"V10.0 先做提词器再做语音"会把语音面试官的依赖节点推延至少 2 周 | **调整里程碑顺序**：V10.1 直接启动语音面试官 MVP，同期产出 HintEngine 1.0（规则版） + VoiceKit 1.0（Web Speech API）；提词器如果后续要做，直接复用上述三件套；不再等提词器。三产品共享引擎是对的，但**引擎的第一个消费者是语音面试官，不是提词器** |
| 2 | **§3.1 出题配比：简历 40% + 岗位 30% + 薄弱点 20% + 题库 10%** | 现有 `pickQuestionsByProfile()` 只有三路：薄弱点(最多3题) + 岗位必备(最多2题) + 随机；**没有"简历40%"和"薄弱点20%"的精确配比**；而且当前实现用 `QUESTION_COUNT=5`，40%意味着 2 题来自简历——**如果用户简历中 0 个项目**，会降级为其他路径，但 40% 配比的承诺对用户不成立 | **两阶段实现**：MVP(V10.1) 配比改为三路：**简历深挖 2题 + 薄弱点/岗位 2题 + 兜底 1题**（用户无 resumeId 时自动切回画像驱动三路）；正式版(V10.2) 按配置文件 `moyun.interview.voice.ratios` 动态配比，无简历/无薄弱点时动态调剂到其他路径；前台展示"本场题源分布"气泡图（透明化） |
| 3 | **§11 里程碑中 V10.0 "HintEngine + useSpeechRecognition"是引擎地基（无依赖纯规则可立即开工）** | 表述正确但定位不准——V10.0 不是提词器页面 MVP，而是**引擎脚手架**；建议把 V10.0 改名为 "V10.0 引擎脚手架：规则HintEngine + VoiceKit（Web Speech API）+ 纯文字模式验证"，单独投入 1 天完成 | 采纳：在实施计划 §5 中加 V10.0 预启动阶段 |
| 4 | **§2 回合延迟预算 1.5-2.5s** | `SseEmitter` 从首 token 到前端可播 TTS 的链路实测（参考 DiagramStreamController）约 0.7-1.2s（含 LLM 首 token），但**TTS 本身的启动延迟（speechSynthesis.speak）Chrome 本地 200-800ms**，合计约 2-3s。方案写 1.5-2.5s 偏乐观 | 修正为 P50 ≤ 3s，P90 ≤ 5s；且"规则分即时输出"是实际体验的关键——用户 stop 说话后，先在 UI 上弹出"规则分：72分 ⭐⭐⭐"占位，AI 深评流式跟上，体感零等待 |
| 5 | **§5.2 多轮上下文管理 "最近 6 回合全量 + 更早摘要"** | MVP(V10.1) 5主问 × 2.4平均回合 = 12 QA，远小于6；**V10.1 根本不需要摘要策略**，直接全量 messages；摘要策略推迟到 V10.3（长场景面试） | 采纳：V10.1 跳过摘要；V10.2 如超过 10 回合启用摘要 |
| 6 | **§5.2 面试官人设配置化——Phase 3 字典化** | 字典化对成本为零（v9.6 字典体系已经是 DB 驱动），V10.1 即可启用 3 个人设，**3 个人设的 system prompt 差异是最廉价的体验差异化** | 提前到 V10.1 MVP：3 种人设（温和/标准/压力），字典 + DB 录 system prompt，不进代码 |
| 7 | **§7 限流：answer 30次/10min/user；start 10次/h/user** | 现有文本版 answer 无限流，1 小时可以刷几百题；语音版限流必须更严，但**"30次/10min" = 3次/分钟 × 平均 3~5 分钟一题的节奏 = 实际超额**，不太合理 | 调整为：**start 5次/天/user（一场语音面试 15 分钟，每天 5 场约 75 分钟，已足够重度用户）；answer 60次/小时/user（覆盖 5 场 × 每主问 2~3 追问 × 5 主问 = 约 50 次 + 冗余 10 次）；同时 TokenUsageService 双重记账** |
| 8 | **§10 风险"多轮 token 膨胀" + "长会话 ASR 中断(Chrome 60s)"等级为中** | Chrome 语音识别的 60s 静默中断是**高优先级 bug**——一道系统设计题，用户停顿思考 1 分钟很常见，直接 final 提交会切断思路 | **等级上调为高**，MVP 必须处理：recognition 实例定期 `abort()` + `start()` 静默续期（不超过 55s），interim 结果放入前端缓冲数组，续期后不丢；并在 UI 上提示"思考时间充裕，系统不会自动中断"——减少用户焦虑 |
| 9 | **§2 自适应决策矩阵中 FOLLOWUP depth<2** | 用户 5 主问 + 每题 2 追问 = 15 个回合；**连续 2 次追问会压缩其他题源（简历/薄弱点/岗位的配比）**；如果某一题用户表现特别好可以深挖，但连续 2 题都被追问可能是出题配比失衡 | 改为 **depth 上限 2（保持）但全局追问上限 4**——即 5 主问中最多 4 个主问允许 1 次追问，剩下 1 个主问最多 2 层追问；总体控制总回合数 ≤ 15（与方案 §9 成本估算的 12 回合吻合） |
| 10 | **§5.1 speak/data 分离的 JSON 输出"流式渲染：speak_text 首句到达即启动 TTS"** | SSE 流式返回 JSON 时，首句通常是 JSON 前缀或首 token 的乱序，**在 JSON 未完整闭合前无法解析为对象**，"speak_text 首句即播"在工程上无法实现——除非使用 NDJSON（每行一个事件，事件 type=speak 时即播） | **修正流式协议**：使用 SSE 的 event 命名通道，发送 3 类事件：① `event:speak` `data:{text}`（TTS 即播）② `event:data` `data:{...}`（完整 JSON，渲染面板）③ `event:end` `data:{}`；后端用 `SseEmitter.send(SseEmitter.event().name("speak").data(text))` 分开发送。这是 DiagramStreamController 已有的模式（§4.2 L-130/L-193），无需发明新协议 |

### 3.2 额外建议（方案中未提到的亮点）

| 建议 | 说明 | 价值 |
|------|------|------|
| **W1. 麦克风权限引导 + 耳机自检首屏** | 进入语音面试前，先展示 "①授权麦克风 ②佩戴耳机 ③试说一句"三步骤；首次试说 TTS 会播放 "你好，我是你的AI面试官" 然后 ASR 识别用户 "你好"，双向通道测试通过才能进入 start 表单——把 §10 最高风险（回声循环、识别差）前置解决，避免进入面试后才出问题 | ⭐⭐⭐⭐⭐ 体验提升最大 |
| **W2. 面试可中途暂停 + 续答** | 实际面试场景（尤其是居家练习）经常被打断；状态机新增 `PAUSED`，UI 上一个暂停键；暂停时 TTS 停止、ASR abort，恢复时播报"我们继续，刚才的问题是…"并重读题目 | ⭐⭐⭐⭐ 完赛率 +20% |
| **W3. 纯文字模式也给 TTS 读题** | Firefox 不支持 ASR 时降级为纯文字，但 speechSynthesis 基本全浏览器可用——纯文字模式依然播报题目，不浪费已经建好的 TTS 基础设施 | ⭐⭐⭐ 体验完整度 |
| **W4. 面试报告"逐题点评回放"(点击某题可重播面试官话术 + 用户回答转写)** | 复盘时用户最想对照"我当时怎么答的、当时面试官说我不好的点是什么、现在我觉得怎么改进" | ⭐⭐⭐⭐ 复盘闭环 |
| **W5. 报告页"生成面试总结 + 生成下次练习建议"使用单独 LLM** | 报告生成一次 LLM（方案估算 +¥0.01），如果免费用户每天只有 1 次免费生成报告，也是强变现钩子（VIP 无限次） | ⭐⭐⭐⭐ 商业化抓手 |

---

## 四、风险与对策（改进版，基于代码现状）

| 风险 | 等级（调整后） | 实际代码抓手 |
|------|--------------|------------|
| TTS 被麦克风拾音（回声循环） | **高**（与方案一致） | W1 耳机自检首屏 + "按住说话"可选模式 + ASR start 时立即 `speechSynthesis.cancel()`（代码实现上在 useSpeechRecognition 的 onstart 回调里调用 window.speechSynthesis.cancel()，形成硬互斥） |
| 技术词中英混说识别差 | **高**（与方案一致） | MVP 即提供"转写可编辑后提交"（UI 字幕区是 textarea 可修改，final 后自动暂停等待用户确认/修改）；v10.3 升级 paraformer |
| 追问幻觉（问简历没有的） | **高**（与方案一致） | Prompt 约束 + 抽检日志；**额外工程化校验**：后端 `QuestionValidator.validateFollowup(followupQuestion, anchors)` 检查 followupQuestion 中必须包含至少 1 个 anchor 原词；否则降级为 NEXT（同时记录日志）——这是零幻觉率的最后一道防线 |
| 回合延迟超标 | 中（原方案高，实际没那么糟） | SPEAK/DATA 双通道 SSE + 规则分即时占位；超时话术 TTS "抱歉，我思考得有点久了，我们先进行下一题" + 后台记录慢日志 |
| 多轮 token 膨胀 | 低（原方案中，MVP 无此问题） | V10.2 超 10 回合启用摘要；V10.1 全量 messages |
| **长会话 ASR 中断（Chrome 60s）** | **高**（上调） | 55s 静默续期 + 前端缓冲；UI 明确提示"思考时间充裕" |
| 纯念简历作弊 | 低（原方案一致） | n-gram 重合度检测 + 报告标注嫌疑 |
| **R1. Portal 端误用后台 SecurityUtils.getUserId()（新发现）** | **高（阻断级）** | [CmsInterviewController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsInterviewController.java) 中 `addExperience/editExperience` 新增的 `SecurityUtils.getUserId()`，此控制器路径是 `/cms/interview/**` → 管理端（Spring Security 体系）→ SecurityUtils.getUserId() 正确；但若将同样模式照搬到 Portal 端语音面试 Controller 会 100% 失败。**强制规范：`/portal/**` 控制器必须用 `PortalSecurityUtils.getUserId()`，`/cms/**` 控制器必须用 `SecurityUtils.getUserId()`**；在落地计划中作为 CR-1 硬性检查项 |
| **R2. 题库数据不足导致三路召回全部落空** | **中（实际体验杀手）** | 现有 `pickQuestionsByProfile` 的 queryByTag 走 `tags LIKE '%xxx%'`，如果 tag 匹配不上会退到 RAND()兜底；但 RAND() 拉 5 道"随机题"可能和岗位/简历完全无关（随机出一道"小学古诗词赏析"给后端面试）——用户会秒退出。**方案里没提到的数据回填是事实上的前置依赖**，见 Phase 0 |

---

## 五、改进版实施计划（五阶段，含 SQL/菜单/文档 ——四同步原则）

### Phase 0. 前置依赖（先于开发，可与开发并行但必须在验收前就绪）

| 编号 | 任务 | 产物 / 判定标准 | 责任 |
|------|------|---------------|------|
| P0-1 | **面试题库数据回填**（至少 1000 道） | 后台 `cms:interview:add` 或批量导入：Java 后端 200、前端 200、数据库 100、算法 200、系统设计 100、网络 100、通用软技能 100；每道题 tags、solution、difficulty、questionType 非空；覆盖率统计：`select count(*) from portal_interview_question where status='published'` ≥ 1000 | 运营 / 数据脚本 |
| P0-2 | **简历项目数据引导** | `MyResumesPage.vue` 在用户无简历时，引导创建一份简版简历（至少 1 个项目经历）；否则 VoiceInterview 的 resume 题源（40%配比）会落空——MVP 允许无 resumeId，但不能强制有。此任务的价值是让 40% 的题源实际产出而不是落空 | 前端（低优，可放 V10.1 末尾） |
| P0-3 | **行业知识库数据初始化**（RAG 出题前置） | 后台 AI 知识库至少建 3 个库：`Java后端面试热题`/`前端面试热题`/`通用软技能题`，每个库上传至少 5 篇 md 文档（面试高频考点面经），向量化完成。V10.3 才启用，P0 预留 | 运营 |
| P0-4 | **环境变量：生产 JAVA21 验证** | 当前 dev 环境 `JAVA_HOME` 是 JAVA8，**编译需手动切 JAVA21**（本次 1190 文件 BUILD SUCCESS 实测可行）；生产部署必须切换 JAVA_HOME=JAVA21。这是 Spring Boot 3 的硬基线 | 运维 |

---

### V10.0 引擎脚手架（1 天，可独立提交）

**交付物**：
1. **后端**
   - [ ] `com.moyun.ext.cms.service.interview.HintEngine`（规则版 1.0）
     - `List<String> generateKeywords(PortalInterviewQuestion q)`：复用 MockInterviewServiceImpl.extractKeywords（直接 public 抽成共用工具）
     - `HintVO generateHint(PortalInterviewQuestion q, int level)`：level=1 给 1 个切入点关键词，level=2 给 STAR 框架提示，level=3 给全部关键词 + 结构提示（但不给完整参考答案）
   - [ ] `ModelType` 枚举扩展 `ASR("asr","语音识别")`、`TTS("tts","语音合成")`（enum 只加，不改既有）+ 配套 `upgrade_v10.1_voice_interview.sql` 第一节：枚举注释同步；`ai_model_config` 表中无 asr/tts 模型记录时不报错（MVP 用浏览器 API，不需要模型）
2. **前端**
   - [ ] `src/composables/useSpeechSynthesis.ts`：`speak(text, opts?)`、`cancel()`、`pause/resume`、speaking 状态、队列排队（题目→过渡语排队播）、浏览器检测不支持时回退 null
   - [ ] `src/composables/useSpeechRecognition.ts`：`start()/stop()/abort()`、interim/final 双回调、55s 静默续期、Chrome Session 失效自动重连缓冲、浏览器不支持时回退 null
   - [ ] `src/composables/useInterviewHint.ts`：封装 HintEngine 调用 + 缓存
   - [ ] **字典（v9.6 规范）**：
     - `portal_interviewer_style`（温和/标准/压力）——3 条字典
     - `portal_voice_question_source`（resume_project/required_skill/weak_tag/question_bank/industry_rag）——5 条字典
     - `portal_voice_next_action`（greeting/ask/followup/hint/next/report）——7 条字典
   - [ ] 配套 `upgrade_v10.1_voice_interview.sql` 第二节：INSERT IGNORE 字典数据（幂等）
3. **联调验证**：用 1 个临时 demo 页（不进路由）测试：HINT → TTS 播报关键词 → ASR 识别一段语音→final 转写上屏，三引擎链路跑通。这是 V10.1 的前置验收。

---

### 界面参考（OfferGoose STAR 核心功能页面）

> 参考原型：OfferGoose STAR 实时对话气泡流布局。MVP 以此为视觉蓝本做简化实现（三栏 + 对话流 + 底部输入），避免重新设计。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ⏱ 面试时长：00:04:18        [暂停] [给我提示] [下一题]       [🔴 结束面试] │ 顶部控制条
├────────────┬──────────────────────────────────────────────┬──────────────────┤
│左栏         │ 中央对话流                                     │ 右栏 历史/系统   │
│(占比 20%)   │ (占比 55%，最宽)                               │ (占比 25%)       │
│             │                                              │                  │
│ 🧑 面试官头像 │ ┌─面试官 AI 泡泡（左对齐，浅绿色背景带 icon）─┐ │ ╔ 系统提示═════╗ │
│ ▼ style人设 │ │ 👤 AI: 嗯，这个过程中，我主要是和几个关键…    │ │ 欢迎进入模拟…   │
│ ▼ 题目进度   │ └────────────────────────────────────────────┘ │ ╚══════════════╝ │
│ ▼ 1 自我介绍│ ┌─题目高亮泡泡（黄色标题 + 问题正文）─────────┐ │                  │
│ ▼ 2 项目深挖│ │ 📌 在跨部门或跨团队的项目合作中，你是如何…   │ │ ╔ 面试官═══════╗ │
│ ▼ 3 技术考察│ │   能否举例说明你曾经遇到的挑战…             │ │ 第1题(共5题):   │
│ ▼ 4 软技能  │ └────────────────────────────────────────────┘ │ 深度≤1/2追问     │
│ ▼ 5 综合评价│ ┌─用户回答泡泡（右对齐，淡蓝/白色）──────────┐ │                  │
│ (颜色=得分) │ │ 首先，和我一起工作的有市场部的同事…         │ │ ╔ AI 分析══════╗ │
│             │ └────────────────────────────────────────────┘ │ - 相关度 85      │
│             │ ┌─AI 分析泡泡（左对齐 灰色 分段渲染）────────┐ │ - 深度 70        │
│             │ │ 亮点：双写校验机制清晰                     │ │ - 结构 75        │
│             │ │ 缺口：未提到数据校验的对账频率             │ │ - 诚实度 90      │
│             │ └────────────────────────────────────────────┘ │ ╚══════════════╝ │
│             │ ┌─面试官追问泡泡（左对齐，带 📌 followup）───┐ │                  │
│             │ │ 📌 你提到用了 TCC，Try 失败补偿怎么做的？   │ │                  │
│             │ └────────────────────────────────────────────┘ │                  │
│             │                                              │                  │
│             │ ┌─底部输入区────────────────────────────────┐ │                  │
│             │ │ 🎙 面试官你好，我认为…   (ASR interim 灰) │ │                  │
│             │ │ ─────────────────────────────────────────│ │                  │
│             │ │ ⚠ 69 秒内作答    [清空文本] [✓回答完毕]   │ │                  │
│             │ └────────────────────────────────────────────┘ │                  │
└────────────┴──────────────────────────────────────────────┴──────────────────┘
```

**从参考界面提取并纳入 MVP 强制的 UI 规范（§三·W 建议级升级为要求）**：

| 规范点 | 说明 |
|--------|------|
| **三栏布局固定比例 20% / 55% / 25%**（≤768px 折叠） | 左栏：面试官信息 + style 切换标签 + 题目进度时间线（已答题目颜色按得分渐变色）；中央：对话气泡流（AI出题/用户回答/AI点评/面试官追问 四种气泡类型，严格对齐参考）；右栏：上半区"系统提示" + 中间"面试官元信息" + 下半区"AI 维度分析（雷达占位→数据到后实时渲染 Tag 云 + 亮点/缺口）" |
| **面试官头像 + 风格标签** | 顶部固定显示头像（默认 AI 头像，style 不同可换表情图标）+ style 人设小 badge（"温和型/标准型/压力型"胶囊 Tag） |
| **题目高亮泡泡（单独气泡类型）** | 不是普通 AI 回答，黄色背景带 📌 icon + 问题标题行号（"在跨部门合作项目中…"），下方可选显示"题源：简历项目2"标签（透明化） |
| **回答输入框三按钮** | 左侧「⚠ 69 秒内作答」倒计时 + 中间「清空文本」+ 右侧「✓ 回答完毕」——**倒计时必须存在**，既是紧迫感也是卡壳检测的可感知版本；6s 卡壳自动提示（见 §2） |
| **对话历史右侧单独区域**（参考截图最右侧） | 把"系统/面试官/AI"三段对话文本**同时展示在右侧独立滚动区**——这是纯文字模式降级后用户的"全景视图"，核心信息不丢失 |
| **顶部控制条独立一行** | 面试时长实时计时器 + 暂停(含 W2 续答) / 给我提示 / 下一题 三按钮 + 最右「结束面试」红色按钮；随时可点 |
| **气泡分层与滚动策略** | 新消息自动滚到底；但用户手动向上滚动查看历史时，暂停自动滚动（仅滚动条接近底部时恢复）——参考常见 IM 行为 |
| **ASR interim/final 样式分级** | interim 内容在输入框显示为灰色斜体；final 转正为黑色正文；用户可编辑后再点「回答完毕」 |

**与原方案 §8 前端设计的差异（以上述规范为准覆盖原方案 §8）**：
- 取消原方案「中央麦克风波形球 + 字幕区 + 右侧面板」的三区域布局，改为「左元信息 + 中对话气泡流 + 右全景分析」三栏布局；波形球内化为输入框左侧的小型麦克风状态图标（录音中脉动，停止静止）。
- 维度雷达图从「右侧实时渲染」降级为「AI 分析 Tag 云先行 + 报告页再出雷达」——Tag 云比雷达更符合截图风格，渲染延迟也更低。
- 回答完毕按钮 + 秒数计时器内嵌入输入框行，不是独立底部控制条。
- 暂停按钮从底部控制条移到顶部（与面试时长计时器放在一起更合理）。

---

### V10.1 语音面试官 MVP（5-7 天，核心主体）

**交付物**：

1. **SQL（upgrade_v10.1_voice_interview.sql 完整，幂等）**
   - CREATE TABLE IF NOT EXISTS `portal_voice_interview`（方案 §6，扩 config_json 字段含 hintsEnabled/stuckThreshold/style/difficulty）
   - CREATE TABLE IF NOT EXISTS `portal_voice_interview_qa`（方案 §6，扩 transcription_edited 标记、rule_dimensions_json 规则维度分）
   - INDEX：idx_user_time / idx_interview / idx_parent
   - 菜单 + 权限：`后台 / 内容管理 / 语音面试管理`（只读复盘页 M+C + 按钮 `portal:voice:list/portal:voice:query`）；Portal 端接口白名单走 `PortalSecurityConfig` 登录态
   - 字典 §V10.0 中 3 类字典 INSERT IGNORE

2. **后端**
   - [ ] Entity/Mapper：`PortalVoiceInterview`、`PortalVoiceInterviewQA`（MyBatis-Plus `@TableName` 自动映射）
   - [ ] Service 接口+实现：`IVoiceInterviewService` 核心方法
     - `start(userId, VoiceStartConfig config)` → 生成本场题单 + 首问 + greet 话术 + 返回 `VoiceInterviewVO`
       - 出题配比扩展（改进 §3.1 2条）：`resumeId 非空时`：简历项目(2题) × 画像薄弱/岗位必备(2题) × 兜底(1题)；`无 resumeId`：切回画像驱动三路 + 随机（复用现有 `UserProfileSnapshotService.buildSnapshot` + `pickQuestionsByProfile`）
       - 简历项目深挖题：调用 `ResumeProjectDeepQuestionGenerator.generate(List<ResumeProject> projects)` ——规则版（锚定项目技能关键词从题库匹配，或无匹配时由 LLM 生成 2 个锚定问题）
     - `submitAnswer(interviewId, qaId, transcript, latencyMs)` → **SSE 双通道流**：
       - ① 立即计算规则分（复用 `MockInterviewServiceImpl.scoreAnswer` 提取出的公共方法），先 `emitter.send(event:data 规则分)`
       - ② LLM 流式处理（走 `DynamicChatService.chat`，注入 system prompt=面试官人设 + 约束），生成 `speak_text` 分片 → `emitter.send(event:speak)`；`data` JSON 完整后 → `emitter.send(event:data)`；最后 `event:end`
       - ③ 根据 data.next_action 执行：followup 插入 parent_qa_id=当前 qaId 的新 QA；hint 保存 hints 并返回；next 切换下一题；report 直接结束
     - `requestHint(interviewId, qaId)` → 调用 `HintEngine.generateHint(q, level++)`（hint 使用次数写入 qa.hint_used）
     - `forceNext(interviewId, reason)` → 用户点"下一题"：走 DECIDING 矩阵但强制 NEXT
     - `finish(interviewId)` → 聚合分数（平均分 + 亮点 + 薄弱点清单 + 报告 PDF 占位 JSON）→ `VoiceInterviewReportVO` + 更新 status=finished + 刷新画像薄弱点
     - `listMy(userId, PageDomain)` / `getDetail(id, userId)` / `qaToWrongBook(qaId, userId)`（复用现有 `WrongQuestionService`，不新表）
   - [ ] Controller：`PortalVoiceInterviewController`（7 接口，方案 §7，限流注解 `@RateLimiter`）
     - ⚠️ **CR-1：严格使用 `PortalSecurityUtils.getUserId()`，绝对不能用 `SecurityUtils.getUserId()`**
   - [ ] 限流：start `@RateLimiter(count=5, time=86400, LimitType.USER)`（5次/天）；answer `@RateLimiter(count=60, time=3600, LimitType.USER)`（60次/小时）
   - [ ] Admin 只读复盘页 Controller（`/cms/voice-interview/list`）+ 查询 Mapper

3. **前端**
   - [ ] `src/api/voiceInterview.ts`（7接口 + SSE 连接封装，使用 EventSource 或 fetch().body.getReader() 解析双通道事件）
   - [ ] `src/pages/interview/VoiceInterviewPage.vue`（核心页面，状态机驱动 UI，8 个关键子组件拆分）
     - **状态机组件**：
       - `SetupCard`（入口配置：resumeId 选择 + position 选择 + scene预设 + style人设选择 + 难度选择 + 个性化开关 + hints开关 + 静音阈值滑块；首次进入先 W1 "授权+耳机+试说"三步骤引导）
       - `MicBall`（中央波形球：LISTENING 脉动/ANALYZING 旋转/ASKING 静止/PAUSED 暂停图标）+ 控制条（回答完毕/给我提示/下一题/暂停/结束）
       - `TranscriptPanel`（字幕区：题目卡片 + ASR interim 灰色/final 黑色 → final 后变 textarea 可编辑 + 确认按钮 → 点击确认才 submitAnswer；右侧维度雷达 + 亮点/缺口 Tag 云，data JSON 到即渲染）
       - `InterviewProgress`（左侧时间线：主问 1~N 每个节点，节点颜色=得分高低；点击已结束的节点可 W4 "点击回放"：TTS 重读题目 + 转写重新上屏——只读，不能重答）
       - `HintOverlay`（hint 面板，浮层显示关键词，可关闭，使用次数提示 + "提示消耗了一次机会，建议先自己想"提示文案）
       - `ReportPage`（报告：总分 + 维度雷达 + 薄弱点 Tag + 逐题点评卡片 + W4 逐题回放 + "薄弱点一键入错题本"按钮 + W5 "生成AI逐题建议/下次练习计划"——LLM 再调用一次）
     - **降级链**：`useSpeechSynthesis.supported.value === false` → TTS 关闭（只显示文字题目）；`useSpeechRecognition.supported.value === false` → 直接进入纯文字模式（字幕区 textarea 答题，无 MicBall，改成"提交答案"按钮）
   - [ ] 路由：`/interview/voice` 对应 VoiceInterviewPage，需登录；`MobileTabBar` 的"面试"高亮前缀保持不变（`/interview` 已覆盖）
   - [ ] 入口导流：在 MockInterviewPage.vue 顶部新增横幅："🎙️ AI 语音面试官体验升级" → 跳转 `/interview/voice`
   - [ ] `InterviewPage.vue`（面试首页）新增卡片入口，与"模拟面试"并列

4. **文档（四同步）**
   - 本文档（14_*）从评估版升级为交付版（含验收指标 + 接口清单 + 字段说明）
   - SQL 脚本 `upgrade_v10.1_voice_interview.sql` 入 docs/sql 目录（若不存在则建），执行顺序加入文档

5. **验收指标（V10.1）**
   - 代码层：mvn compile 0 error；moyun-portal `npm run build` 0 error
   - 手动测试：
     - Chrome 下完成 1 场完整面试（start→5主问+追问→finish→报告），无报错
     - Firefox 下纯文字模式跑通（降级链有效）
     - 无麦克风权限/拒绝后，引导页可识别并切换到纯文字
     - 薄弱题一键入错题本 → 错题本列表可见
     - 历史列表可见、详情可见
     - Admin 语音面试管理页可查询、可查看复盘详情（只读）
   - UI 验收（新增，对照 OfferGoose 界面参考 §三栏布局）：
     - 面试进行中：4 类气泡（AI出题/用户回答/AI点评+代码块/面试官追问）全部正确着色与对齐
     - 报告页：4 Tab 全量渲染（概要/问题分析/面试官剖析/相关知识点）
     - 五维雷达 5 个维度数值全部可映射到规则/LLM 分，无空值

---

### 界面参考补充（OfferGoose 深度面试复盘 + 代码气泡）

> 补充两张截图内容：报告页采用 **4 Tab + 三栏弱点/优点卡** 结构；实时对话中需支持 **代码块 Markdown 渲染**（算法题场景核心）。

```
┌──────────────────────────────────────────────────────────────────────────┐
│ [OfferGoose]                               面试时长 00:07:34 🔴 结束面试 │ 顶部（同进行时）
├────────────┬─────────────────────────────────────────────────────────────┤
│左栏卡片     │ 中央区域 4 Tab 切换                                          │
│(20%)       │                                                             │
│ 🟩 实时面试提醒│ [📝 面试对话] [📌 面试概要] [🔍 问题分析] [💡 面试官剖析] [📚 相关知识点]│
│ 🟩 AI模拟面试│ ───────────────────────────────────────────────────────── │
│ 🟩 一键AI简历│ ┌─面试概要─────────────────┐ ┌─缺点与优点──────────────┐ │
│ 🟩 深度面试复盘│ │ 📊 五维雷达              │ │ 🔴 缺点 (卡片×N)       │ │
│ 🟩 多语言面试 │ │      回答相关性 ╱╲  自信度 │ │   · 对本地化了解不够深  │ │
│            │ │  专业度 ✦───●───✦ 表达流畅度│ │   → 追问锚定原话         │ │
│            │ │    面试互动性 ╲╱            │ │   · 服务器维护流程不熟   │ │
│            │ │                           │ │   → 追问锚定原话         │ │
│            │ │ （5维度：回答相关性/专业度 /│ │  🟢 优点 (卡片×N)       │ │
│            │ │   表达流畅度/面试互动性/自信度）│ │  · 沟通协作能力强       │ │
│            │ └───────────────────────────┘ │  → 引用回答段落           │ │
│            │ ┌─本次面试总结（文字段落）─────┐ │  · 快速适应规划能力     │ │
│            │ │ 在这次面试中，候选人介绍了…    │ │  → 引用回答段落         │ │
│            │ │ …持续学习和适应新技术的能力…  │ │  · 心理素质强           │ │
│            │ └─────────────────────────────┘ │  → 引用回答段落         │ │
│            │                                 └─────────────────────────┘ │
└────────────┴─────────────────────────────────────────────────────────────┘
```

**对应实时面试中的代码气泡（算法题/系统设计题核心）**：

```
┌─AI 点评 + 代码代码泡泡（OfferGoose 图2）────────────────────────────────┐
│ AI：实际上，这是一个经典的动态规划问题…到达楼顶的不同方法数就是斐波那契   │
│      数列的第 n 项。简单实现一下的话，可以写成这样：                      │
│ ┌─代码代码块（浅色背景 + 语法高亮 + 等宽字体 + 行号）──────────────────┐│
│ │ def climb_stairs(n):                                                 ││
│ │     if n == 1: return 1                                              ││
│ │     if n == 2: return 2                                              ││
│ │     a, b = 1, 2                                                     ││
│ │     for i in range(3, n+1):                                          ││
│ │         a, b = b, a + b                                              ││
│ │     return b                                                         ││
│ │                                                                      ││
│ │ print(climb_stairs(10))  # 假设10级台阶                                ││
│ └──────────────────────────────────────────────────────────────────────┘│
│ 这样就可以算出有 10 级台阶时，到达楼顶的不同方法数了。                     │
└──────────────────────────────────────────────────────────────────────────┘
```

**补充到 MVP 强制的 UI 规范（从 §界面参考 条目继续编号）**：

| 规范点 | 说明 |
|--------|------|
| 9. **报告页 4 Tab 结构**（OfferGoose 深度面试复盘 Tab 区） | Tab 顺序：`面试概要` → `问题分析` → `面试官剖析` → `相关知识点和概念`。MVP 完整实现前 3 Tab，第 4 Tab 相关知识点占位，V10.2 用知识库 RAG 实打实地生成（从用户答过的题、错误点提取关键词去 ai_knowledge_library 检索 3~5 篇文档摘要卡） |
| 10. **面试概要 Tab 三要素固定** | 左「五维雷达」（**5 维度：回答相关性 / 专业度 / 表达流畅度 / 面试互动性 / 自信度**，与 OfferGoose 完全对齐，这 5 维映射后端 `dimensions_json`：relevance、professionalism、fluency、interactivity、confidence——**方案原 §5.1 的 4 维(relevance/depth/structure/honesty)作废**，用此 5 维覆盖，行业标准且与参考图一致）+ 中「面试总结段落」（报告聚合 LLM 生成） + 右「缺点/优点双列卡片」（每张卡片必须带 `🔴/🟢` icon + 卡片标题 + 引用原话/追问锚点文本，点击引用可回跳对话中对应气泡——深度复盘 W4 核心） |
| 11. **缺点/优点卡引用锚定回跳** | 缺点卡 / 优点卡的引用段落可点击 → 高亮对话流中对应的回答气泡 / 追问气泡并滚动到该位置——这是 OfferGoose "深度复盘"的灵魂交互，MVP 必须有（否则卡片就是空数据列表，价值归零） |
| 12. **对话气泡内 Markdown/代码块渲染**（OfferGoose 图 2） | 中央对话气泡内容**必须是 Markdown 渲染**：普通段落、标题、加粗/斜体/inline code、有序无序列表全部支持；算法题场景尤其关键——**代码块必须单独子组件 + 语法高亮 + 等宽字体 + 复制按钮 + 可选行号**（引入 `highlight.js` 轻量版或直接复用 QuestionDetailPage.vue 中已有的代码编辑器/渲染库，不要自写） |
| 13. **左栏导航卡片**（OfferGoose 图1/2 最左） | 面试首页 `InterviewPage.vue` 的 STAR 核心功能区做 5 张入口卡片：「实时面试提醒」「AI 模拟面试」「一键AI简历」「深度面试复盘」「多语言面试支持」——每张卡片是一个跳转入口（后 2 张 MVP 置灰+敬请期待，对应 V10.2 提词器和 V10.3 多语种 ASR），但框架必须就位 |
| 14. **面试官视频/头像占位**（OfferGoose 图2 中央上方的"真人面试官视频"） | V10.1 MVP 用静态头像 + 环形录音中绿边呼吸动画替代；V10.3 cosyvoice 升级后考虑数字人视频生成（可选，但 MVP 不做，避免视频资源） |

**对后端协议的联动影响**（补充 §V10.1 后端交付）：
- `data.dimensions` JSON 字段键固定为 `{relevance, professionalism, fluency, interactivity, confidence}` 五个（0-100 整数），对应报告页五维雷达；不再使用方案原 4 维命名
- `data.highlights` / `data.gaps` 每项要求附带可选 `quote_anchor_index` 字段（整数，指向 messages 数组中哪条回答/追问的 index）——前端引用锚定回跳依赖这个索引
- LLM 流式输出 `event:speak` 事件中若出现连续三反引号，应允许 Markdown 分段到达时立即渲染（前端流式 Markdown 渲染组件就绪即可）

---

### V10.2 LLM 面试官（4-5 天，差异化体验）

**交付物**：
- [ ] **LlmClient 废弃，统一走 AI 域 DynamicChatService**（消除面试域/AI域双栈断点 B1）：MockInterviewServiceImpl 的 LLM 反馈也切到 DynamicChatService（保持回退规则）
- [ ] **Speak/Data 双通道 Agent Prompt 调优**：
  - System Prompt 模板按 style 人设切换，注入简历摘要/岗位/画像薄弱点
  - 约束追问锚定：输出必须包含 anchor 原文；后端校验 QuestionValidator.validateFollowup（通过→保留，不通过→降级 NEXT + 记录）
  - 输出格式用正则/JSON Schema 双重校验，失败 fallback 规则决策矩阵
- [ ] **多轮上下文裁剪策略（>10 回合生效）**：6 回合全量 + 更早摘要
- [ ] **报告 AI 总结 + 下次练习建议**（W5）：VIP 可生成详细 PDF（占位 HTML 版导出即可，MVP 不做真 PDF 库）
- [ ] **背诵嫌疑检测**：回答 n-gram 与简历原文重合度 > 80% → 报告标注
- [ ] **TokenUsageService 接入**：每次 LLM/报告生成 记录 token，Admin 业务仪表板可见用量

---

### V10.3 专业语音升级（3-4 天，体验跃迁）

**交付物**：
- [ ] ModelConfigServiceImpl.createStreamingChatModel 旁新方法 `createAsrModel(configId)` / `createTtsModel(configId)` —— DashScope paraformer-v2（流式 ASR WebSocket）+ cosyvoice（TTS HTTP/SSML）
- [ ] 升级 useSpeechRecognition：后端 WebSocket 代理 → DashScope paraformer（前端录音分片上传，避免 Firefox/Safari 无 Web Speech API 问题）；转写准确率测试（中英混词 ≥ 90%）
- [ ] 升级 useSpeechSynthesis：cosyvoice 多音色（匹配人设：温和→温柔音色、压力→严厉音色）
- [ ] **行业 RAG 出题**：`VoiceInterviewQuestionPicker` 追加第 4 路 `industry rag`（匹配 P0-3 知识库），出题占比 10%
- [ ] **面试官人设市场**：3 种默认 + 字典可配置 10+，热门人设可按用户数据迭代（后台字典维护 system prompt）

---

## 六、是否建议做的最终结论

**✅ 强烈建议做，按 V10.0 → V10.1 → V10.2 → V10.3 四阶段推进**

**不建议做的情况**：只有一种——如果当前产品的首要目标是"商业化变现/拉新留存的短期指标"而不是"产品力护城河"，那语音面试官是"体验跃迁"非"变现抓手"，应该先做 VIP 体系/支付接入（Phase 2 of 09 规划），语音面试官放后。

但从 09 规划 Phase 1 的 "数据填充与体验打磨" 作为"当前重点"来看，语音面试官**正好就是体验打磨的旗舰项目**，且复用率极高、债务可控。

**投入产出比估算**：V10.0 + V10.1 开发投入 1.5 人周（约 80-120 工程小时），即可交付"可用 MVP"——题库足量时完赛率预计 ≥60%；文本模拟面试的完赛率预估 25-30%，这个提升是立竿见影的。

**关键成功因素（前 3 个）**：
1. **题库与简历数据填充先于开发或至少并行**（P0-1 / P0-2）——三路召回如果都落空，AI 再聪明也没用
2. **严格遵守 PortalSecurityUtils vs SecurityUtils 的 Controller 边界**（R1 风险），这是跨域开发最常见的 500 错误源
3. **MVP 的"降级链"必须第一优先实现**，而不是事后补——让 Chrome/Firefox/Safari/Edge 全浏览器都能跑完流程，哪怕功能降纯文字

---

## 七、提交与文档索引

- 评估基期：2026-08-18
- 基线分支：`moyun-dev-kouzi`（949a2a8+本次代码，包引用清理后）
- 关联文档：
  - [09_开发进度与规划.md](file:///d:/zyg_new_work/moyun-project-document/docs/09_开发进度与规划.md)（开发总进度，本方案对应 Phase 1 v9.1 升级为 v10.x）
  - [devlog.md](file:///d:/zyg_new_work/moyun-project-document/docs/devlog.md)（每个阶段交付后更新一条）
  - `upgrade_v10.1_voice_interview.sql`（SQL 脚本，将入 docs/sql/ 子目录）
- 废弃前置：本文档废弃原方案中"先做提词器再做语音"的前置依赖声明（见 §3.1-1），提词器若后续开发直接复用本文档引擎层产出即可。
