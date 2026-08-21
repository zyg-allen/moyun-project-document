# 旭林知行 · 开发日志

本文件记录旭林知行项目核心版本变更，作为代码提交与发布的参考记录。
日期采用 `YYYY-MM-DD` 格式。

---

## v10.6 阶段4 (2026-08-20) 话题/专栏审核接口收敛 + SQL 脚本拆分重组

### 交付内容

**1. 删除话题/专栏独立审核接口（审核入口统一收敛到内容审核中心）**
- 后端 [CmsColumnController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsColumnController.java)：删除 `PUT /cms/column/{id}/audit`；`list`/`getInfo` 权限由 `hasAnyPermi('portal:column:list,cms:column:audit')` 收敛为 `hasPermi('portal:column:list')/hasPermi('portal:column:query')`
- 后端 [CmsTopicController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsTopicController.java)：删除 `PUT /cms/topic/{id}/audit`；`list`/`getInfo` 权限由 `hasAnyPermi('cms:topic:list,cms:topic:audit')` 收敛为 `hasPermi('cms:topic:list')/hasPermi('cms:topic:query')`
- 前端 [column.js](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/api/cms/column.js) / [topic.js](file:///d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/api/cms/topic.js)：删除 `auditColumn` / `auditTopic` 方法

**2. SQL 脚本重组为 DDL/DML 分离（幂等可重复执行）**
- `moyun-db-ddl-moyun-db-202608201435.sql`（V10.0 整合版源文件，9983 行）拆分为：
  - `moyun-db-ddl-202608201435.sql`（157 张表结构，按业务模块分组：ai_* → gen_* → portal_* → qrtz_* → sys_* → 其他）
  - `moyun-db-dml-202608201435.sql`（33 张含数据表，943 条 INSERT，每表前加 `DELETE FROM` 实现幂等）
- 拆分工具：`_split_ddl_dml.ps1` + 校验脚本 `_diff.ps1`（保留在 sql 目录）
- 后续表结构变更：在 DDL 文件对应模块末尾追加增量 `ALTER TABLE`，不改动原 `CREATE TABLE`

### 验证
- 后端 `mvn compile` 通过
- admin 前端 `vite build` 通过

---

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
3. **SQL 扩展**（`init_v7.8.sql` 表定义 + `1-1_20260820_practice.sql` 增量脚本）：
   - `portal_interview_question` 新增 5 字段：`practice_mode`/`options`/`correct_answer`/`analysis`/`knowledge_tags`
   - 新增 `idx_practice_mode` 索引
   - 插入 5 道示例选择题 + 3 道示例编程题 + 9 条测试用例
   - 注册 `portal_practice_mode` 字典
4. **后端扩展**（Entity/VO/Query/Service 全链路）：
   - `PortalInterviewQuestion` Entity 新增 5 字段
   - `InterviewQuestionDetailVO` VO 新增 5 字段
   - `InterviewQuestionQuery` Query 新增 `practiceMode` 筛选
   - `buildQuestionQueryWrapper` 支持 `practice_mode` 条件
   - `selectQuestionDetailById` 填充练习模式扩展字段
5. **前端选择题做题页** `ChoicePracticePage.vue`（/learn/practice/choice/:id）：
   - 参考 `choice_question_page.html` 原型设计
   - 选项渲染、提交判定、正确/错误反馈、题目解析展示、再做一次
   - 红色主题、面包屑、返回列表
6. **列表页筛选优化**：改用 `practiceMode=choice/coding` 筛选（比 questionType 更准确）

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

---

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
| 前台字段 | 后台表单字段 | 说明 |
|---|---|---|
| question.title | 标题 | 所有模式通用 |
| question.description | 描述 | 所有模式通用 |
| question.difficulty | 难度 | 所有模式通用 |
| question.options | 选项配置 | 仅选择题，JSON 字符串 |
| question.correctAnswer | 正确答案 | 仅选择题 |
| question.solution | 参考答案/参考代码 | reading/coding |
| question.analysis | 题目解析 | 所有模式可选 |
| question.knowledgeTags | 知识点 | 所有模式可选 |
| question.practiceMode | 练习模式 | 决定表单类型 |
| testCases | 用例按钮跳转 | 仅编程题 |

---

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

---

## v10.0 Phase 0 (2026-08-18) AI 语音面试官前置依赖就绪

### 背景

基于 [14_AI语音面试官评估与落地计划.md](file:///d:/zyg_new_work/moyun-project-document/docs/14_AI语音面试官评估与落地计划.md) 评估文档，启动 Phase 0 前置依赖任务，为 V10.0~V10.3 语音面试官开发奠定数据与环境基座。本阶段不含功能代码开发，仅交付 SQL 脚本、前端引导增强、部署文档。

### 交付内容

**P0-1 面试题库数据回填**（1000 道种子题）
- 脚本：`moyun-server/src/main/resources/sql/upgrade_v10.0_interview_question_seed.sql`
- 分布：Java后端 200 + 前端 200 + 数据库 100 + 算法 200 + 系统设计 100 + 网络 100 + HR软技能 100
- 幂等：基于 title 去重，可重复执行
- 含 2 道完整高质量示范题（Java ==/equals、HashMap 底层原理）+ 998 道模板题（存储过程批量生成）
- 每题填充：title/description/difficulty/category_id/tags/question_type/examine_points/answer_outline/reference_answer/hint
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
- 每库配套 `ai_knowledge_library_config` 记录，分片 800 字符 + 100 重叠 + high_quality 索引
- 遗留 TODO：运营上传 md 文档（每库至少 5 篇）并触发向量化，V10.3 启用前需 `vectorized_docs >= 5`

**P0-4 JDK 21 环境验证**（部署文档补充）
- 文件：[04_部署指南.md](file:///d:/zyg_new_work/moyun-project-document/docs/04_部署指南.md) §1.5
- 内容：JDK 21 环境排查步骤（PowerShell `where.exe java/mvn`）、修复方法（临时/永久环境变量）、编译验证命令（全路径 mvn + JAVA21，已验证 1190 源文件 BUILD SUCCESS）、生产环境注意（Docker/K8s 基础镜像选择）

### 文档同步（四同步原则）

- [09_开发进度与规划.md](file:///d:/zyg_new_work/moyun-project-document/docs/09_开发进度与规划.md) Phase 1 任务表新增"状态"列，标注 4 项已完成 + 5 项待办 + 语音面试官路线图
- [14_AI语音面试官评估与落地计划.md](file:///d:/zyg_new_work/moyun-project-document/docs/14_AI语音面试官评估与落地计划.md) 评估文档（前序会话已交付）
- 本 devlog 新增 v10.0 Phase 0 记录

### 后续路线

- V10.0 引擎脚手架（1 天）：HintEngine + useSpeechSynthesis/Recognition + 字典
- V10.1 语音面试官 MVP（5-7 天）：2 表 + 7 接口 + VoiceInterviewPage
- V10.2 LLM 面试官（4-5 天）：speak/data 双通道 + 追问链 + 报告
- V10.3 专业语音升级（3-4 天）：paraformer ASR + cosyvoice TTS + 行业 RAG

---

## v10.0 引擎脚手架 (2026-08-18) 语音面试官三引擎链路就绪

### 背景

承接 Phase 0 前置依赖，落地 V10.0 引擎脚手架：后端 HintEngine 规则版 + 前端 TTS/ASR/Hint 三 composable + 联调验证页，构成语音面试官最小可运行链路。本阶段不含会话表与正式页面（V10.1 交付），仅验证"浏览器端语音能力 + 规则提示引擎"可行性。

### 后端

**HintEngine 规则引擎**
- 接口 [HintEngine.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/interview/HintEngine.java)：`generateKeywords` + `generateHint(question, level)`
- 实现 [HintEngineImpl.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/interview/impl/HintEngineImpl.java)：复用 MockInterviewServiceImpl 关键词提取逻辑（tags + solution + referenceAnswer + answerOutline），按题目类型（hr/project/system_design/algorithm/bagwen）匹配不同结构框架（STAR / 系统设计四步 / 算法四步 / 总分总）
- [HintVO.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/HintVO.java)：level/title/keywords/structureHint/examinePoints/speakText，speakText 可直接驱动 TTS
- 三级提示：L1 切入点（1~2 关键词）/ L2 结构（STAR+大纲）/ L3 全量（关键词+考察点+结构）

**Controller**
- [PortalVoiceInterviewController.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/controller/PortalVoiceInterviewController.java)：路径 `/portal/interview/voice`
  - GET `/hint?questionId=&level=` 分级提示
  - GET `/keywords?questionId=` 仅关键词（轻量）

**ModelType 枚举扩展**
- [ModelType.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/ai/enums/ModelType.java) 新增 `ASR("asr","语音识别模型")` / `TTS("tts","语音合成模型")`，为 V10.3 服务端 ASR/TTS 模型配置预留

### SQL

- [upgrade_v10.1_voice_interview.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.1_voice_interview.sql)：
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

---

## v10.1 (2026-08-18) 语音面试官 MVP

### 背景

承接 V10.0 三引擎链路，落地语音面试官最小可用产品：2 张会话表 + 7 个接口 + SSE 流式评分 + 前端状态机驱动页，跑通"出题 → 语音作答 → 规则评分 → TTS 反馈 → 下一题/报告"完整闭环。本阶段规则评分为主（复用 HintEngine），LLM 话术与报告为 V10.2 范围。

### SQL

[upgrade_v10.1_voice_interview.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.1_voice_interview.sql) 在 V10.0 字典基础上扩充：
- **portal_voice_interview**（会话主表）：user_id/position/scene/resume_id/status/style/difficulty/total_qa/current_idx/score/summary/report/config_json/is_personalized/profile_snapshot；索引 idx_user_time/idx_status/idx_del_flag
- **portal_voice_interview_qa**（问答明细）：interview_id/question_id/question_snapshot/transcript/score/feedback/hint_used/latency_ms/idx；索引 idx_interview_id/idx_question_id
- 菜单：前台"AI 语音面试官"（path=interview/voice，路由 /interview/voice，免菜单注册直接路由可达）+ 后台"语音面试记录"管理菜单 + 5 按钮权限
- 状态字段对齐字典 voice_interview_status：in_progress/completed/abandoned

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
  - 出题：从 portal_interview_question 按 category/scene 抽题，支持 resume_id 关联简历题源（V10.1 预留，未接 RAG）
  - 评分：复用 HintEngine 关键词提取 + 规则打分（关键词命中率 + 结构完整性 + 长度档位），0-100
  - SSE 双通道：score 事件（规则分）+ speak 事件（TTS 话术）+ data 事件（完整数据）+ end 事件
  - 独立 sseExecutor 线程池，SSE_TIMEOUT 5min，异常 completeWithError
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
- resume_id 题源关联未接 RAG，V10.3 行业知识库向量化后启用
- 清理：已删除 com.moyun.portal.domain.vo 下 3 个遗留重复 VO（VoiceInterviewVO/QaVO/ReportVO），统一使用 com.moyun.ext.cms.domain.vo

---

## v10.2 (2026-08-19) 简历模块升级：模板管理 + 用户简历入口 + 维护页三栏重构

### 背景
简历模块原有：admin 无独立模板管理菜单、用户管理无简历关联入口、模板列表为纯文本、简历维护页单栏简陋。本次全面升级，对标静态原型 `resume_optimizer_page.html` 与 `vue_resume_spec.md`。

### 交付内容

**问题1：admin 简历模板管理独立菜单 + 独立权限**
- 脚本：[upgrade_v10.2_resume_module.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.2_resume_module.sql)
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

---

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
- 前端 2 文件：MockInterviewPage.vue / mockInterview.ts
- AiProperties 移除 `mockInterviewFeedbackEnabled` 字段

**前端清理**
- [router/index.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/router/index.ts) 删除 `/interview/mock` 路由
- [InterviewPage.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/pages/InterviewPage.vue) 删除「AI 模拟面试」卡片，STAR 矩阵 5 卡 → 4 卡（grid 调整为 md:grid-cols-4）
- [types/api.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/types/api.ts) 删除 MockInterviewQaVO / MockInterviewVO / MockInterviewDetailVO

**SQL 清理脚本**
- [upgrade_v10.3_drop_mock_interview.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.3_drop_mock_interview.sql)
- DROP 2 张表（portal_mock_interview / portal_mock_interview_qa）
- ALTER portal_user_stats DROP 2 列（mock_interview_count / avg_mock_score）
- 软删除 portal_category 中 interview-mock 分类
- DELETE portal_mock_scene 字典（类型 + 6 条数据）

### 验证
- 后端 mvn compile 通过（exit 0）；前端 vue-tsc 通过（exit 0）
- VoiceInterview 与 MockInterview 无代码级依赖，删除后语音面试主链路不受影响

### 四同步：文档更新
- [01_项目介绍.md](file:///d:/zyg_new_work/moyun-project-document/docs/01_项目介绍.md)：「AI 模拟面试」→「AI 语音面试官」（核心拉新引擎 / 求职者 / LangChain4j 能力行）
- [02_技术架构.md](file:///d:/zyg_new_work/moyun-project-document/docs/02_技术架构.md) §5.1：子模块「AI 模拟面试 + WebSocket」→「AI 语音面试官 + SseEmitter + 三引擎链路」
- [08_项目优缺点与改进建议.md](file:///d:/zyg_new_work/moyun-project-document/docs/08_项目优缺点与改进建议.md)：AI 能力集成行更名
- [11_面试指南后续迭代规划.md](file:///d:/zyg_new_work/moyun-project-document/docs/11_面试指南后续迭代规划.md) §2.3：LLM 对话式面试标记「✅ 已交付」，删除已删文件引用，验收标准全部勾选
- [14_AI语音面试官评估与落地计划.md](file:///d:/zyg_new_work/moyun-project-document/docs/14_AI语音面试官评估与落地计划.md) §1.1：文本模拟面试行标记已下线，修正 updateMockInterviewStats/portal_mock_scene 等过期引用
- [vue_interview_spec_simple.md](file:///d:/zyg_new_work/moyun-project-document/docs/vue_interview_spec_simple.md)（含 _Coze_Drive 副本）：首页 2×2 卡片移除独立「AI 模拟面试」卡片
- 07_工程质量检讨_v5.2 为版本化历史快照，保留不动

### 遗留 TODO
- 语音面试完赛统计：VoiceInterviewServiceImpl.finish() 暂未回写面试次数/平均分，后续如需统计新建 voice_interview_count / avg_voice_score 字段

---

## v10.4 (2026-08-19) 栏目菜单重构：前台 Mega Menu + 移动端 5 Tab + 后台 7 一级菜单

### 背景

依据 [_Coze_Drive_Coze项目助手_ai_interview_system/nav_redesign_proposal.html](file:///d:/zyg_new_work/moyun-project-document/docs/_Coze_Drive_Coze项目助手_ai_interview_system/nav_redesign_proposal.html) 与 [admin_redesign_proposal.html](file:///d:/zyg_new_work/moyun-project-document/docs/_Coze_Drive_Coze项目助手_ai_interview_system/admin_redesign_proposal.html) 两份方案文档，对前后台栏目菜单进行业务运营导向重组：前台从"按内容类型分类"转为"按用户意图分类"，后台从"按代码模块分类"转为"按业务域聚合"。

### 前台导航重构（PC + 移动端）

**数据驱动基础**
- SQL 脚本：`moyun-server/src/main/resources/sql/upgrade_v10.4_nav_restructure.sql`
- `portal_category` 新增 `nav_badge` 字段（NEW/HOT 徽章，Mega Menu 直接渲染）
- 新建 3 个一级栏目：学习中心(learn) / 阅读空间(reading-space) / 创作互动(create)
- 旧 5 个一级栏目（散文/技术/读书/社区/创作者）退出导航（show_in_nav=0）
- 面试指南→面试专区（重命名+图标+排序）；个人空间→我的
- 学习类子项从「面试指南」迁至「学习中心」；散文/技术/读书降为「阅读空间」二级
- 社区互动+创作者中心合并为「创作互动」
- AI 语音面试官从硬编码改为数据驱动（新增 interview-voice 子项，nav_badge='NEW'）
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

### 后台菜单重构（sys_menu 业务域聚合）

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
- 重排 order_num：工作台1 / 内容管理2 / 面试管理3 / 学习管理4 / 用户运营5 / AI能力6 / 系统设置7
- 不修改视图目录和路由（零代码层变更，仅重组菜单树）

### 验证

- vue-tsc 类型检查通过（exit 0）
- 前后台导航栏目数据结构对齐（nav_badge 字段贯通后端实体、SQL、前端类型、UI 渲染）
- 移动端 5 Tab 与 PC 6 栏目逻辑一致（首页/面试/创作/学习/我的 ↔ 首页/面试专区/创作互动/学习中心/我的，阅读空间在移动端合入学习 Tab）

### 相关文件

- 前端：[Navbar.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/Navbar.vue)、[MobileTabBar.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/MobileTabBar.vue)、[Layout.vue](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/components/Layout.vue)、[api.ts](file:///d:/zyg_new_work/moyun-project-document/moyun-portal/src/types/api.ts)
- 后端：[PortalCategory.java](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/portal/domain/entity/PortalCategory.java)
- SQL：[upgrade_v10.4_nav_restructure.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.4_nav_restructure.sql)、[upgrade_v10.4_admin_menu_rebuild.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.4_admin_menu_rebuild.sql)
- 文档：[nav_redesign_proposal.html](file:///d:/zyg_new_work/moyun-project-document/docs/_Coze_Drive_Coze项目助手_ai_interview_system/nav_redesign_proposal.html)、[admin_redesign_proposal.html](file:///d:/zyg_new_work/moyun-project-document/docs/_Coze_Drive_Coze项目助手_ai_interview_system/admin_redesign_proposal.html)

### 遗留 TODO
- 后台菜单重构脚本需在实际数据库执行后，根据校验 SQL 输出调整二级菜单的精确归属（部分 menu_name 可能因历史版本不同存在差异）
- 工作台页面内容增强：将原 cms/dashboard 业务看板提升为独立路由 /dashboard 并增强与前台成长数据的联动
- 内容管理「审核队列」聚合入口：v9.6 已有审核收敛能力，需在菜单层显式提供聚合入口

### v10.4 后台菜单策略调整：旧 4 菜单全部打开，暂不删除

**用户决策（2026-08-19）**：不要隐藏 4 个旧一级菜单，先全部打开，后续根据实际使用情况再决定删除哪些（含代码）。

**SQL 脚本调整**：[upgrade_v10.4_admin_menu_rebuild.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/upgrade_v10.4_admin_menu_rebuild.sql)
- 第 3 步：旧 4 个一级菜单（系统监控/系统工具/任务管理/创作者认证）从"隐藏+迁移子项"改为"全部打开（visible='0' status='0'），不迁移子项"
- 第 4-5 步：商业化二级菜单迁移、业务看板迁移均注释为"暂缓执行"
- 第 6 步：order_num 调整为新 7 个（1-7）+ 旧 4 个（8-11）= 共 11 个一级菜单全部可见
- 第 8 步：新增"待审查菜单清单"查询，辅助后续决策

**冗余分析结论**（供后续删除决策参考）：

| 旧菜单 | 重复情况 | 删除建议 | 理由 |
|---|---|---|---|
| 系统监控 (monitor) | 不重复 | 保留 | 系统设置下无监控功能；丢失将影响在线用户/缓存/服务器/日志/定时任务运维 |
| 系统工具 (tool) | 不重复 | 保留 | 代码生成器、Swagger 是开发期工具，系统设置下无对应 |
| 任务管理 (task) | 严重冗余 | 建议删除 | 定时任务已在 monitor 下重复；待办/已办 v9.6 已隐藏（与内容审核中心重复）；扫描结果是占位页 |
| 创作者认证 (certification) | 完全冗余 | 建议删除 | v9.6 已降级到内容管理下，一级菜单已无子项（空壳） |

**待删除清单（用户确认后再执行）**：
- SQL：`sys_menu` 中"任务管理"一级菜单 + 3 个子菜单（定时任务子菜单 parent_id 改回 monitor）、"创作者认证"空壳一级菜单
- Vue 文件：`views/system/audit/todo.vue`、`views/system/audit/done.vue`（功能被 `views/cms/audit-center/index.vue` 覆盖）、`views/system/scan/index.vue`（占位页无后端）
- 路由注释：`moyun-admin-vue/src/router/index.js` 第 87-95 行表单构建注释
- **必须保留**：后端 `AuditTaskController`（被 audit-center 引用）、`views/cms/certification/index.vue`、`views/cms/dashboard/index.vue`、`views/cms/` 下商业化 vue 文件（vip/wallet/ad 等，v9.0 已迁到用户运营下）

### init_v7.8.sql 升级为 V10.0 整合版

**用户决策**：删除所有表重新执行脚本，将 init_v7.8.sql + 8 个 upgrade 脚本（v8.1~v10.4）的表结构变更和数据合并为一个文件。

**文件**：[init_v7.8.sql](file:///d:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/init_v7.8.sql)（原 732KB → 757.67KB，9983 行）

**合并内容**：

1. **文件头注释**：v7.8 → V10.0，新增版本合并历史（v7.8~v10.4）

2. **删除废弃表**（v9.0/v10.3）：
   - `portal_mock_interview` / `portal_mock_interview_qa`（v10.3 AI 模拟面试下线）
   - `portal_pk_challenge` / `portal_circle*`（v9.0 已在之前版本移除，仅注释）

3. **修改表结构**（4 张表）：
   - `portal_category`：+ `nav_badge varchar(20)` 字段（v10.4 导航徽章）
   - `portal_interview_resume_template`：+ `preview_images text` 字段（v10.2 多图预览）
   - `portal_user_stats`：- `mock_interview_count` / `avg_mock_score` 2 字段（v10.3 删除）
   - `ai_model_config`：`model_type` 注释更新为含 `asr/tts`（v10.1 语音面试官）

4. **追加 5 张新表**（v8.1~v10.1）：
   - `sys_audit_task`（v8.1 统一审核任务表）
   - `sys_job_scan_issue`（v8.1 定时任务扫描结果表）
   - `portal_import_template_config`（v8.2 导入模板字段配置表）
   - `portal_voice_interview`（v10.1 语音面试会话主表）
   - `portal_voice_interview_qa`（v10.1 语音面试问答表，含追问链）

5. **V10.x 数据合并段**（幂等 INSERT，位于结尾设置之前）：
   - **菜单重组**（v10.4）：5 个一级菜单重命名（智能AI→AI能力 / 系统管理→系统设置 / 面试指南→面试管理 / 读书空间→学习管理 / 商业化→用户运营）+ 新建工作台一级菜单
   - **栏目重构**（v10.4）：新建 3 个一级栏目（学习中心/阅读空间/创作互动）+ AI 语音面试官子项（带 NEW 徽章）
   - **字典注册**（v9.6+v10.1+v10.2）：29 类字典类型注册（sys_dict_type）+ 核心字典数据（sys_dict_data，含支付状态/支付渠道/语音面试状态/面试官风格/提示级别/简历模板分类等）

**保留的基础数据**：系统配置/部门/角色/用户/岗位/菜单/栏目/标签/字典/友链/成长规则/帮助分类/面试分类/成就/岗位/任务

**备份**：原文件备份为 `init_v7.8.sql.bak`

**遗留 TODO**：
- 完整的 29 类字典数据（v9.6 注册了 27 类，每类有多个 dict_data 值）当前仅注册了核心几类（支付状态/支付渠道/语音面试/简历分类），其余字典的 dict_data 值需要从 upgrade_v9.6_admin_optimize.sql 补充
- v8.1~v10.2 新增的菜单项（VIP/钱包/征文/提示词/语音面试/简历模板/导入模板等二级菜单和按钮）尚未合并到 init 脚本，需要从对应 upgrade 脚本补充
- v10.0 面试题种子数据（1000 道）和知识库种子数据（3 个库）保留为独立脚本，未合并到 init

---

## v9.6 (2026-08-16) 后台全面体检：菜单收敛 + 27类业务字典 + 前台字典化（"前台数据皆有后台管理"）

### 体检发现（三线并行分析）

- 后台 9 个一级目录、~40 个 C 菜单、8 个 Tab 容器；SQL 注册但 views 缺失 = 0（无空白页风险）
- 完全孤儿页面 2 个：cms/contest（征文活动，前台 /contests 正在消费其数据）、cms/prompt（写作提示词）
- 系统字典仅 15 个 RuoYi 框架自带，业务字典 0；cms/portal/ai 页面 useDict 使用 0 处（全硬编码）
- 后端 PaymentStatus 枚举 5 态 vs 前端订单/打赏下拉仅 3 态（closed/failed 缺失，无法筛选）

### 菜单收敛（upgrade_v9.6_admin_optimize.sql 第一~四节）

- 🐛 **知识中心 M→C**：init 注册为 M 目录但填了 component → RuoYi M 类型不加载页面，Tab 容器永不渲染，点击空白；改为 C 菜单
- 🗑️ **审核入口三重冗余收敛**：内容审核中心（内部已含待办/已办/全部 3 Tab，查 sys_audit_task）vs 任务管理>我的待办/我的已办同表同逻辑 → 后两者隐藏（保留路由权限），唯一入口=内容审核中心
- 🗑️ **创作者认证目录扁平化**：目录下单挂 1 个子菜单冗余 → 认证审核直挂内容管理（path=certification），删空目录
- ✨ **孤儿页面注册菜单**：征文活动（cms:contest:list + 4 按钮）、写作提示词（cms:writing-prompt:list + 4 按钮）挂内容管理——前台 /contests 数据从此有后台维护入口
- ℹ️ 消息中心 vs 通知管理：查证消息中心已是"私信/通知"双 Tab，通知管理独有 CRUD+广播发送，定位不同（用户消息处理 vs 内容发布），**保留不合并**

### 业务字典 27 类（upgrade_v9.6_admin_optimize.sql 第五节，104 条数据）

- 支付/交易域：portal_pay_status(5态补全closed/failed)/portal_pay_channel/portal_tip_target_type/portal_wallet_txn_type
- 内容域：cms_article_status/cms_column_status/cms_topic_status(5态)/cms_contest_status
- 审核域：cms_audit_task_type(8类对齐枚举)/cms_audit_task_status
- 反馈举报域：cms_feedback_type/cms_report_type/cms_handle_status(反馈+举报共用)
- 读书域：portal_book_type/portal_book_serial_status/portal_access_type/portal_common_status
- 学习域：portal_study_plan_type/portal_study_plan_status/portal_wrong_question_status
- 面试域：portal_question_difficulty/portal_question_type/portal_resume_category/portal_mock_scene
- 运营域：portal_ad_slot_key/cms_vip_status/sys_login_type
- 字典值与后端枚举 code 一一对齐（新增状态改字典即可，前后台同步生效）

### 后端新增

- `com.moyun.portal.controller.PortalDictController` — 前台免登录字典接口
  - GET /portal/dict/{dictType}、GET /portal/dict/types?types=a,b,c（批量）
  - 安全：白名单仅放行 portal_/cms_ 前缀字典（防泄露系统字典）；@Anonymous + PortalSecurityConfig GET /portal/dict/** permitAll 双通道；走 DictUtils 缓存
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

---

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
- 🐛 **修复"学习辅助"菜单 404 + 归属错位**（init_v7.8 脚本历史遗留，线下验证发现）
  - 根因：第8节兜底在"内容管理"下注册 `path=learn-aux`（perms=portal:learn-aux:list），v7.13/7.15 又在"读书空间"下注册同名 path → RuoYi-Vue3 按 path 生成同名路由（Learn-aux），vue-router 4 同名 addRoute 移除先注册者 → `/cms/learn-aux` 被 `/book/learn-aux` 覆盖，点击 404
  - 修复：init_v7.8.sql 末尾追加"六(补)"清理段 + upgrade_v9.5_merge.sql 第五节，删除重复菜单并将角色授权转移给正式菜单（均幂等）
  - 归属调整：学习辅助（学习计划"每日刷题"为主 + 错题本 100% 源于题库）数据产自面试指南题库体系 → 正式菜单迁至"面试指南"目录（order=9），路由变为 `/interview/learn-aux`
- 🐛 **去重交易入口：删除"财务/付费订单"独立菜单**（线下验证发现）
  - 根因：init 第十六节注册"财务(finance)/付费订单"（component=cms/order/index，已下线），v9.0 第七节迁移条件为 parent_id=内容管理，而其挂财务目录 → 未迁移成死角；"交易管理"Tab 第一个 Tab 引用同一组件 → 功能完全重复
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

---

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
  - DROP TABLE：portal_pk_challenge / portal_circle_post / portal_circle_member / portal_circle
  - 物理删除 sys_menu + sys_role_menu（tip/pk/circle）
  - init_v7.8.sql 清理PK建表语句
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

---

## v8.2 (2026-08-14) 通用导入模板 + 题库导入导出

- 新建 `portal_import_template_config` 表：各业务（题库/标签/文章/面经/笔记）导入模板字段动态配置，无需改代码即可调整列名/必填/下拉值
- 新增题库导入导出按钮权限：`cms:interview:import` / `cms:interview:export`
- 失败行可重导：ImportResult 返回成功/失败统计 + 失败明细，修正后可重导
- 脚本：`upgrade_v8.2_import_template.sql`（幂等）

## v8.1 (2026-08-14) 审核模块统一整合

- 新建统一审核任务表 `sys_audit_task`（替代分散的各业务表 status 聚合），提交/处理审核时与业务表 status 双写
- 新建定时任务扫描结果表 `sys_job_scan_issue`（定时任务异常/待处理项记录）
- 数据回填：各业务表现有 pending 记录回填至 sys_audit_task
- 菜单调整：新增「任务管理」一级菜单（定时任务/我的待办/我的已办/扫描结果），audit-center 保留为「全部审核」入口
- 依赖提示：Admin 业务仪表板"审计待办"依赖 sys_audit_task，**不执行此脚本仪表板会报错**
- 脚本：`upgrade_v8.1_audit_unified.sql`（幂等）

## v5.2 (2026-07-19) 安全加固 + SQL 整理 + 文档重建

### 主要变化

- 🔒 安全加固：修复 5 项致命 + 17 项高级安全问题
- 🗂️ SQL 整理：90 个原始脚本归类为 12 个整理文件，169 张表合并 ALTER 到最终 CREATE
- 📚 文档重建：新增 01_项目介绍.md / 08_项目优缺点与改进建议.md / 09_开发进度.md

### 致命问题修复（5 项）

- C-1 在线代码执行 RCE → 临时下线 `/portal/code/run` 返回 503
- C-2 门户密码明文存储 → 全链路 BCrypt（8 处）+ Controller 层加密
- C-3 JWT 硬编码密钥 → 删 DEV_FALLBACK_SECRET，fail-fast，阈值 64 字符（HS512 要求）
- C-4 文章 IDOR → 6 处 checkOwnership + 版本接口 3 处
- C-5 凭据硬编码 → 全部 ${ENV_VAR} 化 + application-prod.yaml.example

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

- 新增 docs/01_项目介绍.md（项目全貌，9 大业务领域）
- 新增 docs/08_项目优缺点与改进建议.md（后期开发指导）
- 新增 docs/09_开发进度.md（各模块进度保留）

### 相关文件

- 后端：~18 个 Java 文件修改（Controller/Service/Mapper/Config/Handler/Util）
- 后端配置：3 个文件（application.yaml + application-dev.yaml + application-prod.yaml.example）
- 前端：4 个文件（MarkdownRenderer + MarkdownEditor + security.ts + ExperiencePublishPage）
- SQL：sql-organized/ 目录 12 文件 + README
- 文档：docs/ 3 个新文件 + devlog + README 更新

---

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

---

## v4.0.5 (2026-06-26) 全面功能排查清单

### 主要变化

- 📋 新增：[10_功能排查清单.md](./10_功能排查清单.md) - 全面功能细节排查清单
  - 按"模块 > 页面 > 接口 > 组件"4 层级组织
  - 覆盖前台门户（17 模块 153 项）、后台管理（20 模块 178 项）、后端服务（9 模块 55 项），共 386 项测试点
  - 每项含核心业务规则、问题描述、测试结果、测试时间、遗留问题、关联问题
  - 汇总 12 个已知问题（BUG-A ~ BUG-L），按严重度分级
  - 提供测试执行顺序与修复优先级建议

### 调研中发现的新问题（BUG-A ~ BUG-L）

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


---

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
  - `loadingConfigCache`：全量加载 sys_config 到 Redis
  - `clearConfigCache`：删除所有 `sys:config:*` 键
- ⚡ 优化：标签推荐逻辑利用 title/category 参数
  - `TagQuery` 新增 `categoryId` 字段
  - `PortalTagMapper.xml` 添加 `categoryId` 关联查询（EXISTS portal_entity_tag + portal_article）
  - `PortalTagController.getRecommendTags` 动态构建缓存键，title 模糊匹配标签名，category 筛选该分类下文章使用过的标签

### 相关文件

- `moyun-server/.../CmsArticleServiceImpl.java`
- `moyun-portal/src/pages/PublishPage.vue`
- `moyun-server/.../SysConfigServiceImpl.java`
- `moyun-server/.../PortalTagController.java`
- `moyun-server/.../TagQuery.java`
- `moyun-server/.../PortalTagMapper.xml`

---

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
  - 实现：前端已传 `authorId` 则尊重；否则 `SecurityUtils.getUserId()` 反查 `portal_user.user_id`，未关联则自动建户（携带 sys_user 基础信息，role=admin）；`insertArticle` 加 `@Transactional`
- ✨ 新增：后台发布 slug / category_path 维护（与前台链路对齐）
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

---

## v4.0.2 (2026-06-26) 三端深度复查 P1 修复 + 文档维护

### 主要变化

- 🐛 修复：三端深度复查发现的 4 个 P1 问题
  - `38_init_report_feedback_menu.sql`：`portal_report.description` 列长 `varchar(1000)` → `varchar(2000)`，与实体 `@Size(max=2000)` 对齐（避免 1001-2000 字举报描述写入失败）
  - `40_fix_bugs_v4.sql`：新增 `ALTER TABLE portal_report MODIFY COLUMN description varchar(2000)` 供已部署环境升级
  - `AuthorPage.vue`：`loadAuthorData` 中已登录且非自己主页时调用 `followApi.checkFollow` 初始化 `isFollowing` 状态（修复已关注用户按钮显示错误 + 粉丝数不同步）
  - `notification/index.vue`：`remoteUserSearch` 的 `response.rows` → `response.data.records`（修复用户搜索下拉永远为空）
  - `notification/index.vue`：移除"系统用户（后台管理员）"选项，下拉固定为"门户用户"并 disabled（后端 `/cms/user/list` 只查 portal_user 表）
- 🧹 清理：删除 `api/cms/comment.js` 中无调用方的 `addComment` / `updateComment` 死代码
- 📚 文档：全面更新 `docs/` 目录文档（07_更新日志、06_问题修复、bugs/bug-list、devlog、05_测试清单），补充 `sql/README.md` 的 25-40 号脚本说明

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

---

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

---

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

- 📦 打包：v4.0 源码包（zip 格式，4.9M，排除 node_modules/target/dist/.git）

### 需执行的 SQL 脚本

1. `38_init_report_feedback_menu.sql` — 举报/反馈菜单
2. `39_init_flowable_menu.sql` — 流程管理菜单
3. `40_fix_bugs_v4.sql` — v4.0 综合 Bug 修复（友情链接 status + 通知菜单清理 + 评论菜单修复 + PortalReport 列长升级）

### 相关文件

- 后端：`SysDashboardServiceImpl.java`、`SysDashboardController.java`、`CmsReportController.java`、`CmsFeedbackController.java`、`PortalReport.java`、`PortalFeedback.java`、`CmsFriendLinkVO.java`
- Portal：`AuthorPage.vue`、`index.vue`
- Admin：`index.vue`、`report/index.vue`、`feedback/index.vue`、`friend-link/index.vue`
- SQL：`38_init_report_feedback_menu.sql`、`40_fix_bugs_v4.sql`、`01_moyun_init.sql`、`03_portal_init.sql`、`application.yaml`

---

## v2.1.1 (2026-06-16) Mapper XML resultMap 分层 + @Slf4j 编译修复

### 主要变化

- 🔧 修复：Cms*Result 混用前台 Entity 问题
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
- **其他 portal mapper**（Book/Category/Tag/Notification/Order/Wallet 等）：仅暴露 `selectPortalXxxPage/List/ById` 前台方法，无 Cms*Result 混用 ✓

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

---

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
  - 新增表 `portal_entity_tag`（tag_id + entity_type + entity_id 多实体绑定）
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

---

## v2.0.0 (2026-06-01) 门户内容模块基线

- ✅ 新增：`portal_article`、`portal_article_tag`、`portal_article_view` 等核心表
- ✅ 新增：`portal_category`、`portal_tag`、`portal_comment`、`portal_like`、`portal_bookmark` 基础模块
- ✅ 新增：`portal_user`、`portal_follow` 等用户与社交基础表
- 🔧 统一：Entity 使用 `@Data` + `@TableName` 的 MyBatis-Plus 风格

