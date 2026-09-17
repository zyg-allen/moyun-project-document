#  AI 面试全链路重构 · 完整实施文档（V2 · 强化版）

> **文档版本**：v2.0
> **基线版本**：v11.55（2026-09-11）
> **文档定位**：可直接作为开发任务书，突出**你的要求**，聚焦**数据流 + 业务流**，覆盖**交互 + 排版 + UI**
> **核心原则**：一问一答流畅优先，后台异步分析，结束时批量出报告

---

# 第一部分 · 核心要求（你的主张）

## 1.1 五条铁律

| #     | 要求                 | 说明                           |
| ----- | ------------------ | ---------------------------- |
| **1** | **不做每题同步分析**       | 面试中不做实时评分，不打断一问一答            |
| **2** | **会话开启前先生成上下文**    | 岗位 + JD + 简历 + 配置 → 上下文 → 题单 |
| **3** | **一问一答优先，其余异步**    | 只有"提交确认"和"下一题"是同步，其余全异步      |
| **4** | **原始数据边做边存，分析最后算** | 防丢失 + 保全面                    |
| **5** | **结束才出全面报告，有进度条**  | 批量分析 → 进度条 → 报告              |

## 1.2 三条设计原则

| 原则      | 说明             |
| ------- | -------------- |
| **真实感** | 模拟真实面试，不因分析卡顿  |
| **不丢失** | 意外关闭有确认，数据有快照  |
| **有引导** | 报告给可执行路径，不只是诊断 |

---

# 第二部分 · 业务流 + 数据流

## 2.1 全景业务流

```
┌─────────────────────────────────────────────────────────────────────┐
│  阶段 1：准备（用户点击"开始准备面试"后）                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ① 设备检测                                                         │
│     麦克风 → 扬声器 → 耳机 → 环境噪声                               │
│     ↓ 全部通过                                                       │
│  ② 加载上下文                                                       │
│     简历解析 → 岗位 JD 解析 → 用户画像 → 合并为 ResumeContext       │
│     ↓                                                               │
│  ③ 生成题单                                                         │
│     基于上下文 + 出题规则 → 生成 N 道题（不一定是技术题）           │
│     ↓                                                               │
│  ④ 会话初始化                                                       │
│     生成 sessionId → 存入 Redis → 进入面试页                        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  阶段 2：面试（一问一答）                                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  循环 N 次：                                                         │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │  ① 面试官出题（同步，从题单取）                                │ │
│  │     ↓                                                         │ │
│  │  ② 用户作答（语音/文字）                                       │ │
│  │     ↓                                                         │ │
│  │  ③ 提交答案                                                    │ │
│  │     ├── 【同步 <200ms】确认收到 + 返回下一题                   │ │
│  │     ├── 【异步】单题评分 + 上下文累积                          │ │
│  │     └── 【实时】原始数据落库（防丢失）                         │ │
│  │     ↓                                                         │ │
│  │  ④ 下一题                                                      │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  结束触发：                                                         │
│  ├── 用户主动点"结束面试"                                           │
│  ├── 题单耗尽（自动）                                               │
│  ├── 时间耗尽（提示）                                               │
│  ├── 连续跳过 3 题（提示）                                          │
│  └── 5 分钟无响应（提示）                                           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│  阶段 3：报告（结束后）                                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ① 批量分析（进度条）                                               │
│     逐题评分 → 能力画像 → 强弱项 → 提升建议 → 知识点关联            │
│     ↓                                                               │
│  ② 报告落库                                                         │
│     portal_voice_interview.report（JSON）                          │
│     ↓                                                               │
│  ③ 报告展示                                                         │
│     三段式（简历/岗位/Tab）                                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 2.2 数据流图（关键）

```
┌──────────────────────────────────────────────────────────────────────┐
│  数据源                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │ 简历表   │  │ 岗位 JD  │  │ 用户画像 │  │ 配置     │           │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘           │
│       └────────────┴────────────┴────────────┘                    │
│                         ↓ 阶段1：准备时加载                         │
│              ┌─────────────────────────┐                          │
│              │  会话上下文（Redis）     │                          │
│              │  ├── resumeContext      │                          │
│              │  ├── jobContext         │                          │
│              │  ├── config             │                          │
│              │  ├── questionPaper      │                          │
│              │  ├── qaHistory          │                          │
│              │  └── currentIndex       │                          │
│              └────────────┬────────────┘                          │
└───────────────────────────┼──────────────────────────────────────┘
                            ↓ 阶段2：面试中读写
┌───────────────────────────┼──────────────────────────────────────┐
│  面试中数据流                                                      │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  用户提交答案                                               │  │
│  │  ├── 【同步】→ 前端确认 → 返回下一题                        │  │
│  │  ├── 【实时】→ MySQL（qa 表原始数据）                       │  │
│  │  └── 【异步】→ 后台分析 → Redis（草稿评分）                 │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
└───────────────────────────┼──────────────────────────────────────┘
                            ↓ 阶段3：结束后
┌───────────────────────────┼──────────────────────────────────────┐
│  报告生成数据流                                                    │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  批量分析                                                   │  │
│  │  ├── 从 MySQL 读全部 QA                                     │  │
│  │  ├── 从 Redis 读草稿评分（可选）                            │  │
│  │  ├── 批量 LLM 分析 → 六维评分 + 强弱项 + 建议               │  │
│  │  └── 报告 → MySQL（report JSON）                            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

## 2.3 关键数据表变更

```sql
-- 1. portal_voice_interview（会话主表）新增字段
ALTER TABLE portal_voice_interview
  ADD COLUMN context_snapshot JSON COMMENT '会话上下文快照（准备时生成）',
  ADD COLUMN question_paper JSON COMMENT '题单快照',
  ADD COLUMN analysis_status TINYINT DEFAULT 0 COMMENT '0未分析 1分析中 2已完成',
  ADD COLUMN analysis_progress INT DEFAULT 0 COMMENT '分析进度 0-100',
  ADD COLUMN closed_reason VARCHAR(50) COMMENT '结束原因：user/auto/timeout/skip';

-- 2. portal_voice_interview_qa（问答明细）新增字段
ALTER TABLE portal_voice_interview_qa
  ADD COLUMN answer_raw TEXT COMMENT '用户原始回答（未分析）',
  ADD COLUMN answer_time DATETIME COMMENT '回答时间',
  ADD COLUMN latency_ms INT COMMENT '回答耗时（毫秒）',
  ADD COLUMN score_draft INT COMMENT '草稿评分（异步算）',
  ADD COLUMN analysis_status TINYINT DEFAULT 0 COMMENT '0未分析 1分析中 2已分析';

-- 3. 新建表：会话事件日志（用于追踪流程）
CREATE TABLE portal_voice_interview_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interview_id BIGINT NOT NULL,
  event_type VARCHAR(50) NOT NULL COMMENT 'start/answer/finish/close/refresh',
  event_data JSON COMMENT '事件数据',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_interview (interview_id)
) COMMENT '面试会话事件日志';
```

---

# 第三部分 · 交互设计

## 3.1 准备页交互

### 3.1.1 页面结构

```
┌─────────────────────────────────────────────────────────────┐
│  AI 语音面试准备                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  【步骤 1】设备与环境检测                                    │
│  ├── 🎤 麦克风 [已授权 ✓] [测试]                            │
│  ├── 🔊 扬声器 [已检测 ✓] [测试]                            │
│  ├── 🎧 耳机   [已检测 ✓]                                   │
│  └── 🌊 环境噪声 [检测中...] → 结果：环境安静 ✓             │
│      ⚠ 嘈杂时黄色提示：建议在安静环境面试                    │
│                                                             │
│  【步骤 2】岗位与要求                                        │
│  ├── 岗位名称 [Java 后端开发 ▼]                             │
│  └── 岗位要求（JD）                                         │
│      ┌───────────────────────────────────────────────┐      │
│      │ 1. 5年以上Java开发经验，精通Spring Boot       │      │
│      │ 2. 熟悉微服务架构，有分布式系统设计经验        │      │
│      │ 3. 熟悉MySQL、Redis，有数据库调优经验          │      │
│      └───────────────────────────────────────────────┘      │
│      ⓘ AI 将根据岗位要求调整提问方向和深度                   │
│                                                             │
│  【步骤 3】简历（用户画像）                                  │
│  ├── 选择简历 [钟永国的简历 · 更新 2026-09-07 ▼]            │
│  ├── [上传新简历] [管理简历库]                              │
│  └── ⓘ 简历将作为 AI 了解你的依据，用于深挖项目经历         │
│                                                             │
│  【步骤 4】其他设置（可选）                                  │
│  ├── 面试官风格 [标准型 ▼]                                  │
│  ├── 难度等级   [中级 ▼]                                    │
│  └── 题目数量   [5 题 ▼]                                    │
│                                                             │
│  [开始准备面试]                                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.1.2 准备进度条（关键交互）

```
用户点击"开始准备面试"
    ↓
弹窗显示进度条（不可关闭）
    ┌─────────────────────────────────────────────┐
    │  🔄 正在准备面试...                         │
    │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 60%       │
    │                                             │
    │  ✅ 加载简历画像（1/5）                     │
    │  ✅ 解析岗位要求（2/5）                     │
    │  ✅ 生成会话上下文（3/5）                   │
    │  🔄 生成面试题单（4/5）                     │
    │  ⏳ 准备面试环境（5/5）                     │
    │                                             │
    │  预计剩余：约 5 秒                          │
    └─────────────────────────────────────────────┘
    ↓ 完成
自动进入面试页（无需用户点击）
```

### 3.1.3 环境噪声检测交互

```
检测中：
    🌊 环境噪声 [检测中...]

检测结果（安静）：
    ✅ 环境安静，适合面试

检测结果（嘈杂）：
    ⚠ 环境较嘈杂（65dB）
    建议：
    ├── 换到安静环境
    └── 或佩戴耳机减少回声
```

## 3.2 面试页交互

### 3.2.1 页面结构

```
┌──────────────────────────────────────────────────────────────────────┐
│ ⏱ 00:04:18   [⏸ 暂停] [💡 提示] [⏭ 下一题]              [🔴 结束面试] │
├────────────┬──────────────────────────────────────┬──────────────────┤
│ 左栏 20%   │ 中栏 55%                             │ 右栏 25%         │
│            │                                      │                  │
│ ┌────────┐ │ ┌────────────────────────────────┐  │ ┌──────────────┐ │
│ │  头像  │ │ │ 💬 面试对话                     │  │ │ 📋 面试背景   │ │
│ │        │ │ │                                │  │ │              │ │
│ │财务分析师│ │ │ [面试官] 你好，我是今天的面试官  │  │ │ 岗位：Java   │ │
│ │标准型   │ │ │         先做个自我介绍吧        │  │ │ 后端开发     │ │
│ │         │ │ │                                │  │ │              │ │
│ └────────┘ │ │ [用户] 好的，我叫钟永国...       │  │ │ 难度：中级   │ │
│            │ │                                │  │ │              │ │
│ ┌────────┐ │ │ [面试官] 好的，我们开始技术问题  │  │ │ 题量：5 题   │ │
│ │题目进度│ │ │                                │  │ │              │ │
│ │        │ │ │ [问题2] 给定单链表头节点，请反转 │  │ │ 进度：2/5    │ │
│ │① ✅    │ │ │        并返回反转后头节点        │  │ │              │ │
│ │② ●    │ │ │                                │  │ │ ─────────    │ │
│ │③ ○    │ │ │ [用户] 输入中...                │  │ │              │ │
│ │④ ○    │ │ │                                │  │ │ 💡 面试提示   │ │
│ │⑤ ○    │ │ │                                │  │ │              │ │
│ └────────┘ │ ├────────────────────────────────┘  │ │ 保持 STAR    │ │
│            │ │ ⏱ 90s [____________] [🎤]      │  │ │ 结构表达     │ │
│            │ │ [清空] [💡 提示] [✓ 回答完毕]   │  │ │              │ │
│            │ └────────────────────────────────┘  │ │ [获取提示]   │ │
│            │                                      │ └──────────────┘ │
└────────────┴──────────────────────────────────────┴──────────────────┘
```

### 3.2.2 一问一答交互流（关键）

```
【出题】
面试官气泡出现（带打字机效果）：
    [面试官] 给定单链表头节点，请反转...
    （TTS 同步播报）
    ↓
【用户作答】
输入框激活 + 麦克风脉动
    ├── 语音输入（ASR 实时转写）
    └── 文字输入
    ↓
【提交答案】
用户点"回答完毕"
    ├── 【同步】按钮变灰，显示"已提交"
    ├── 【同步】立即显示下一题（气泡出现）
    └── 【异步】后台评分（用户无感）
    ↓
【下一题】
自动进入下一题
```

**关键**：用户感知是"答完立即出下一题"，**没有任何等待**。

### 3.2.3 意外关闭交互

| 场景      | 交互                              |
| ------- | ------------------------------- |
| 点"结束面试" | 弹确认框："确定结束面试？将生成面试报告" [取消] [确定] |
| 刷新页面    | 弹确认框："刷新将中断面试，确定？" [取消] [确定]    |
| 关闭浏览器   | `beforeunload` 弹确认框             |
| 网络断开    | 顶部显示黄条："网络异常，正在重连..."           |

### 3.2.4 结束分析交互

```
用户确认结束
    ↓
弹窗显示分析进度条（不可关闭）
    ┌─────────────────────────────────────────────┐
    │  🔄 正在分析面试结果...                     │
    │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 40%       │
    │                                             │
    │  ✅ 收集面试数据（1/5）                     │
    │  ✅ 逐题评分（2/5）                         │
    │  🔄 生成能力画像（3/5）                     │
    │  ⏳ 生成提升建议（4/5）                     │
    │  ⏳ 生成最终报告（5/5）                     │
    │                                             │
    │  预计剩余：约 10 秒                         │
    └─────────────────────────────────────────────┘
    ↓ 完成
自动跳转报告页
```

### 3.2.5 v11.96 修订：时长制结束控制 + 报告生成全链路异步化

> **背景**：V3 实测暴露两个问题——①问满 N 题即结束，候选人一句话没说完就被切断；②`finish()` 事务内提交异步批量分析任务，异步线程读库时事务未提交（读到 `analysis_status=0`），守卫直接 return，**报告从未生成**。v11.96 一并修复。

#### 1. 结束控制改为时长制（替代题数制）

| 项       | 说明                                                         |
| ------- | ------------------------------------------------------------ |
| 时长配置   | `sys_config`：`voice.interview.durationMinutes`（缺省 20 分钟） |
| 唯一结束条件 | ①用户点击"结束面试"按钮；②口头明确说"结束面试/我想结束/到此为止"（正则匹配，命中后当前话术轮收尾即结束）；③倒计时归零 |
| 题数不再触发结束 | `runAgentTurn` 删除"问满 total 即 finished"分支，题数仅作为题单耗尽后自然换题/收口参考 |
| 倒计时 UI  | 面试页顶栏全场倒计时 `⏳ mm:ss`（剩余 ≤5 分钟警示色）；恢复面试时扣除中断前已用时 |
| 归零自动收尾 | 前端归零时：若面试官话术生成中或有未提交作答 → 先提交作答挂起标记，待本轮 `end` 事件后统一收尾；否则直接结束 |
| 超时兜底   | 后端 `submitAnswer` 落库后检测超时（时长+2 分钟宽限）→ `finishQuietly` 收口（`closed_reason=timeout`）+ SSE `end{finished:true}` |
| 软收口提示  | 剩余 ≤2 分钟时下一题指令提示面试官"自然收口"；归零提示收尾致谢 |

#### 2. 报告生成 P0 竞态修复 + 自愈

```
finish()/start()（@Transactional）
    └── triggerBatchAnalysis()
        └── TransactionSynchronizationManager.registerSynchronization(afterCommit)
            └── 事务提交后才 submitBatchAnalysis()（异步线程读库必见最新状态）
```

- **防重入**：`RUNNING_ANALYSIS`（内存 Set）标记运行中的分析，避免重复提交。
- **断链自愈**：`getAnalysisStatus` 检测"已结束 + analysisStatus=1 + 无运行任务"→ 自动重新触发批量分析——存量卡住的数据无需手工修库，进页面轮询即自愈。

#### 3. 报告进度可见化（面试页 + 历史页轮询）

| 页面     | 行为                                                             |
| ------- | --------------------------------------------------------------- |
| 面试页（当场） | 结束后报告区显示进度条（既有能力保留）                                 |
| 面试页（刷新重进） | `?id=` 进入：finished + analysisStatus<2 → 显示进度轮询；=2 → 拉取完整报告；in_progress → 恢复面试横幅。**删除前端拼凑伪报告逻辑**，一律以后端真报告为准 |
| 历史页    | 状态徽标旁显示"报告生成中 x%"（Loader2 旋转），5 秒批量轮询进行中记录，全部完成自动重载列表 |

#### 4. 状态字段下发

`VoiceInterviewVO` 新增：`durationMinutes`（本场时长）、`analysisStatus`（0 未分析/1 分析中/2 已完成）、`analysisProgress`（0-100）。

#### 5. 配套变更

- SQL：`20260916-07-voice-interview-duration-and-report.sql`（`voice.interview.durationMinutes` 幂等插入）
- 报告分析口径不变：基于**用户画像 + 岗位/简历上下文 + 问答表现**三源评分并给出评价建议

## 3.3 报告页交互

### 3.3.1 三段式结构

```
┌─────────────────────────────────────────────────────────────┐
│  【第一栏】面试者简介                                        │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  👤 钟永国                                            │ │
│  │  本科 · 5年经验 · Java开发                            │ │
│  │  技能：Java / Spring Boot / MySQL / Redis             │ │
│  │                                                       │ │
│  │  📝 自我介绍                                          │ │
│  │  "我叫钟永国，5年Java开发经验..."                     │ │
│  │                                                       │ │
│  │  🤖 AI 摘要                                           │ │
│  │  "候选人具备 5 年 Java 开发经验，技术栈完整..."       │ │
│  └───────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  【第二栏】岗位信息                                          │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  🎯 Java 后端开发                                     │ │
│  │                                                       │ │
│  │  岗位要求：                                           │ │
│  │  1. 5年以上Java开发经验，精通Spring Boot              │ │
│  │  2. 熟悉微服务架构...                                 │ │
│  │                                                       │ │
│  │  匹配度：75% ████████░░                               │ │
│  └───────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  【第三栏】Tab 分析                                          │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ [📋 面试概要] [💬 对话回放] [🔍 问题分析]              │ │
│  │ [💡 面试官剖析] [📚 相关知识点]                        │ │
│  ├───────────────────────────────────────────────────────┤ │
│  │                                                       │ │
│  │  📋 面试概要                                          │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │  综合得分：32 / 100                             │ │ │
│  │  │  等级：待提升                                    │ │ │
│  │  │                                                 │ │ │
│  │  │  [六维雷达图]                                   │ │ │
│  │  │                                                 │ │ │
│  │  │  ✅ 优势（Top 3）                               │ │ │
│  │  │  · 表达流畅度 20 分                             │ │ │
│  │  │                                                 │ │ │
│  │  │  ⚠ 待提升（Top 3）                              │ │ │
│  │  │  · 回答相关性 0 分                              │ │ │
│  │  │  · 专业度 0 分                                  │ │ │
│  │  │  · 逻辑清晰 0 分                                │ │ │
│  │  │                                                 │ │ │
│  │  │  [查看详细分析 →]（跳问题分析 Tab）             │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  │                                                       │ │
│  └───────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  [薄弱点入错题本] [下载 PDF] [分享报告] [开始新的面试]      │
└─────────────────────────────────────────────────────────────┘
```

### 3.3.2 Tab 交互

| Tab      | 交互                     |
| -------- | ---------------------- |
| 📋 面试概要  | 默认 Tab，点击"查看详细分析"跳问题分析 |
| 💬 对话回放  | 对话气泡流，可点击评分查看详情        |
| 🔍 问题分析  | 逐题分析，每题可展开详情           |
| 💡 面试官剖析 | 面试官视角点评 + 重难点          |
| 📚 相关知识点 | 知识点卡片，点击跳题库            |

---

# 第四部分 · 排版与 UI

## 4.1 准备页 UI

| 元素     | 规格                                      |
| ------ | --------------------------------------- |
| 页面背景   | `var(--theme-surface)`                  |
| 卡片     | 圆角 12px，阴影 `0 2px 8px rgba(0,0,0,0.05)` |
| 步骤标题   | 16px 加粗，左侧色条                            |
| 检测项    | 图标 + 名称 + 状态 + 操作按钮                     |
| 状态徽标   | 绿色 ✓ 通过，黄色 ⚠ 警告，灰色 ○ 未检测                |
| JD 输入框 | 等宽字体，高度 120px，可拖拽调整                     |
| 主按钮    | 主题色，高度 48px，圆角 8px                      |

## 4.2 面试页 UI

| 元素    | 规格               |
| ----- | ---------------- |
| 顶部栏   | 固定，高 48px，白底阴影   |
| 计时器   | 等宽字体，18px        |
| 左栏头像  | 80px 圆形，红色渐变背景   |
| 人设标签  | 圆角胶囊，12px        |
| 题目进度  | 每个题目 32px 圆形，状态色 |
| 对话气泡  | 面试官左对齐灰底，用户右对齐蓝底 |
| 问题气泡  | 黄色背景，带 📌 图标     |
| 输入框   | 高度 120px，圆角 8px  |
| 麦克风按钮 | 圆形 48px，录音时脉动    |
| 主按钮   | 红色，高度 40px       |

## 4.3 报告页 UI

| 元素    | 规格                |
| ----- | ----------------- |
| 三段卡片  | 白色圆角卡片，间距 24px    |
| 综合得分  | 48px 大号数字，颜色按等级   |
| 雷达图   | 300x300，主题色       |
| Tab 栏 | 下划线式，激活主题色        |
| 缺点卡片  | 左侧色条（红/橙/黄），优先级标识 |
| 知识点卡片 | 网格布局，2 列          |
| 底部按钮  | 固定底部，主次分明         |

## 4.4 状态色规范

| 状态  | 颜色        | 用途      |
| --- | --------- | ------- |
| 成功  | `#22c55e` | 已完成、通过  |
| 警告  | `#f59e0b` | 需注意、待提升 |
| 错误  | `#ef4444` | 失败、不及格  |
| 信息  | `#3b82f6` | 进行中、提示  |
| 禁用  | `#9ca3af` | 未激活     |

---

# 第五部分 · 开发任务清单

## 5.1 阶段 1：准备页（P0）

| #   | 任务               | 模块    | 工作量 | 依赖  |
| --- | ---------------- | ----- | --- | --- |
| 1.1 | 加环境噪声检测          | 前端    | 4h  | -   |
| 1.2 | 岗位要求（JD）输入       | 前端+后端 | 6h  | -   |
| 1.3 | 生成上下文 + 题单的进度条   | 前端+后端 | 8h  | 1.2 |
| 1.4 | 精简配置项（8→5）       | 前端    | 3h  | -   |
| 1.5 | 设备检测失败引导         | 前端    | 2h  | -   |
| 1.6 | 上下文生成服务          | 后端    | 8h  | 1.2 |
| 1.7 | 题单生成服务（基于上下文）    | 后端    | 12h | 1.6 |
| 1.8 | 会话初始化 + Redis 存储 | 后端    | 4h  | 1.6 |

## 5.2 阶段 2：面试页（P0）

| #    | 任务              | 模块    | 工作量 | 依赖  |
| ---- | --------------- | ----- | --- | --- |
| 2.1  | 去掉实时六维评分面板      | 前端    | 2h  | -   |
| 2.2  | 加面试背景（岗位/难度/进度） | 前端    | 3h  | -   |
| 2.3  | 加面试提示（STAR 结构）  | 前端    | 2h  | -   |
| 2.4  | 题目进度状态化（✅/●/○）  | 前端    | 2h  | -   |
| 2.5  | 问答异步化（提交即返回）    | 前端+后端 | 8h  | -   |
| 2.6  | 原始数据实时落库        | 后端    | 4h  | -   |
| 2.7  | 意外关闭确认框         | 前端    | 3h  | -   |
| 2.8  | 多个结束触发点         | 前端+后端 | 6h  | 2.6 |
| 2.9  | 单题异步评分          | 后端    | 6h  | 2.5 |
| 2.10 | 上下文异步累积         | 后端    | 4h  | 2.5 |
| 2.11 | 会话事件日志表         | 后端    | 2h  | -   |

## 5.3 阶段 3：报告页（P0）

| #    | 任务               | 模块    | 工作量 | 依赖  |
| ---- | ---------------- | ----- | --- | --- |
| 3.1  | 三段式结构（简历/岗位/Tab） | 前端    | 6h  | -   |
| 3.2  | 缺点移到问题分析         | 前端    | 2h  | -   |
| 3.3  | 面试概要优化（等级+雷达+优势） | 前端    | 4h  | -   |
| 3.4  | 问题分析增强（表现+打分+建议） | 前端+后端 | 8h  | -   |
| 3.5  | 面试官剖析（新增）        | 前端+后端 | 8h  | -   |
| 3.6  | 相关知识点（关联知识库）     | 前端+后端 | 10h | -   |
| 3.7  | 综合得分加等级          | 前端    | 1h  | -   |
| 3.8  | 批量分析服务（结束时）      | 后端    | 12h | 2.9 |
| 3.9  | 报告进度条            | 前端    | 3h  | 3.8 |
| 3.10 | 报告落库             | 后端    | 3h  | 3.8 |

## 5.4 工作量汇总

| 阶段     | 任务数    | 总工时               |
| ------ | ------ | ----------------- |
| 准备页    | 8      | 47h               |
| 面试页    | 11     | 42h               |
| 报告页    | 10     | 57h               |
| **合计** | **29** | **146h（约 18 人日）** |

---

# 第六部分 · 验收标准

> **实施状态**（v11.88~v11.91，2026-09-15）：除标注外全部完成。

## 6.1 准备页验收

- [x] 环境噪声检测正常，嘈杂时有提示（v11.91：3 秒采样平均电平，≥0.12 判嘈杂黄色提示）
- [x] JD 输入框可输入，保存后生效（v11.91：start payload 携带 jobRequirements，注入系统提示词 + 落 contextSnapshot）
- [x] 点击"开始准备"后有进度条，5 步可见（v11.91：简历画像→岗位要求→会话上下文→题单→环境，请求返回即 100%）
- [x] 题单生成基于简历 + JD + 配置（v11.90：contextSnapshot + questionPaper）
- [x] 配置项精简为 5 项（v11.91：核心岗位/JD/简历，其余收进"高级设置"折叠卡片）
- [x] 设备检测失败有引导（v11.88/v11.89：权限拒绝/设备缺失 toast 引导）
- [x] 会话上下文持久化（v11.90：context_snapshot 落 MySQL + Agent 模式 Redis 滑窗记忆）

## 6.2 面试页验收

- [x] 无实时六维评分面板（v11.90：雷达/分析气泡移除）
- [x] 右栏显示面试背景 + 面试提示（v11.90）
- [x] 题目进度有 ✅/●/○ 状态（progressItems）
- [x] 提交答案后 <500ms 返回下一题（v11.90 快链路：同步段零 LLM <200ms）
- [x] 后台异步分析不阻塞前端（v11.90：asyncAnalyzeAnswer 草稿写回）
- [x] 原始数据实时落库（v11.90：answer_raw + answer_time 提交即存）
- [x] 意外关闭有确认框（v11.89：beforeunload + 路由守卫）
- [x] 结束面试有多个触发点（v11.91：主动/题单耗尽/连续跳过 3 题/5 分钟无响应，后两者弹确认）
- [x] 结束有分析进度条（v11.90：analysis 接口轮询 5s×120）
- [x] 会话事件日志可追溯（v11.90：portal_voice_interview_event）

## 6.3 报告页验收

- [x] 三段式结构：简历/岗位/Tab（v11.91：candidate/jobInfo 两栏 + 5 Tab）
- [x] 综合得分有等级（v11.91：≥80优秀/≥70良好/≥60合格/待提升徽章）
- [x] 缺点在问题分析 Tab（v11.91：概要聚焦结论，薄弱点收口问题分析顶部）
- [x] 问题分析含表现+打分+建议（逐题分析 + 待提升区块 + 去练习）
- [x] 面试官剖析有内容（自我介绍评分 + 整体评价 + 改进建议 + 反哺简历优化）
- [x] 相关知识点关联知识库（题库 tags 聚合 + 低分加权）
- [x] 报告可下载 PDF（downloadReport）
- [x] 薄弱点可入错题本（单题 + 全部批量）

## 6.4 闭环验收

- [x] 准备 → 面试 → 报告 全流程无阻塞
- [x] 数据全程可追溯（事件日志 + answer_raw 双保险）
- [x] 意外关闭可恢复（v11.91：`/active` 探测 + `/resume` 恢复快照 + 准备页恢复横幅续接；新面试自动收口遗留会话 abandon 并异步生成报告）
- [x] 报告有可执行的提升路径（改进建议 + 去练习 + 入错题本 + 按建议优化简历）
- [x] 一问一答流畅，无等待感

---

# 第七部分 · 风险与应对

| 风险       | 应对                  |
| -------- | ------------------- |
| 异步分析延迟高  | 单题评分用轻量模型，批量分析用强模型  |
| 题单生成慢    | 缓存简历解析结果；题单生成异步化    |
| 用户中途关闭   | 原始数据实时落库 + Redis 快照 |
| 网络中断     | 本地草稿缓存，恢复后同步        |
| 报告生成失败   | 重试机制 + 兜底模板         |
| LLM 调用超时 | 超时降级为规则评分           |
| 环境噪声误判   | 多次采样取平均             |

---

# 第八部分 · 实施顺序

```
第 1 周：准备页（1.1~1.8）
    ↓
第 2 周：面试页核心（2.1~2.6）
    ↓
第 3 周：面试页增强（2.7~2.11）+ 报告页核心（3.1~3.3）
    ↓
第 4 周：报告页增强（3.4~3.10）
    ↓
第 5 周：联调 + 测试 + 修复
```

---

# 附录 A · 关键决策记录

| 决策        | 理由                  |
| --------- | ------------------- |
| 不用实时评分    | 打断面试节奏，不像真实面试       |
| 原始数据边做边存  | 防意外关闭丢失             |
| 分析结果最后批量算 | 上下文完整，分析更全面         |
| 题单提前生成    | 避免面试中卡顿             |
| 多个结束触发点   | 保证流程完整              |
| 报告三段式     | 结构清晰，信息分层           |
| 缺点移到问题分析  | 概要聚焦"结论"，问题分析聚焦"细节" |
| 环境噪声检测    | 提升面试质量              |

# 附录 B · 交互状态机

```
面试会话状态机：

IDLE（未开始）
    ↓ 点击"开始准备面试"
PREPARING（准备中）
    ↓ 准备完成
READY（就绪）
    ↓ 开始面试
IN_PROGRESS（进行中）
    ├── ASKING（出题中）
    ├── LISTENING（聆听中）
    ├── SUBMITTING（提交中）
    └── NEXT（下一题）
    ↓ 结束触发
ANALYZING（分析中）
    ↓ 分析完成
FINISHED（已结束）
    ↓ 查看报告
REPORT（报告页）
```

# 附录 C · 数据一致性保障

| 环节   | 保障                    |
| ---- | --------------------- |
| 准备阶段 | 上下文生成失败 → 回滚，提示重试     |
| 面试阶段 | 原始数据实时落库 → 失败重试 3 次   |
| 会话状态 | Redis 快照 + MySQL 双写   |
| 报告阶段 | 分析失败 → 重试；最终失败 → 兜底模板 |
| 意外关闭 | 确认框 + 草稿缓存 + 恢复续接     |

---

> **文档维护**：本文件随 AI 面试模块迭代更新
> **配套**：`devlog.md`（变更记录）
> **最后更新**：2026-09-15

---

需要我把这份文档再拆成**给 AI 的自测清单**（可勾选版），或者**给开发的详细任务卡**（每个任务含接口、参数、验收）吗？

***

# V3 章节：统一 AI 入口 · 纯 Agent 自由面试（v11.93，2026-09-16）

> 背景：V2 的"规则管决策 + LLM 只管说话"仍保留题单预生成/规则决策/围栏模式等约 2000 行旧链路，且每轮从 DB 重拼上下文（伪多轮）导致面试官重复提问、话术模板化。V3 彻底切换为**统一 AI 入口**模式：面试官 agent 基于 Redis 滑窗记忆自由推进面试，话术流式直出，配置收口。

## 1. 目标架构（V3 定稿）

- **滑窗记忆管上下文**：复用统一 AI 会话的 `RedisChatMemoryStore`（`InterviewChatMemoryService`，memoryId=`voice-interview:{id}`）。首轮一次性注入 system（agent 人设+本场约束）+ user（简历摘要+JD，`PromptInjectionGuard.wrapData` 包裹）+ assistant（开场白首问）；此后每轮只追加问答对，二次会话零重拼。
- **Agent 自由面试**：删除题单预生成、规则决策（decideNextAction）、围栏 JSON 模式、自我介绍阶段机、候选人反问分支——面试官基于上下文自主反馈/追问/换题，收尾时机由每轮指令（已问 x/N）提示。
- **话术流式直出**：`runAgentTurn` 走 `agentClient.chatStream`，delta 事件增量下发（前端打字机 + 分句 TTS），首字即显、不等分析；评分与深度分析全部留到结束批量报告（V2 的异步批量分析机制保留）。

## 2. SSE 协议（V3 定稿，仅 3 类事件）

| 事件 | 载荷 | 说明 |
|---|---|---|
| delta | `{"t":"增量"}` | 面试官话术增量，打字机渲染 + 遇句末标点（。！？；换行）切句入 TTS 队列 |
| end | `{roundDone, nextQaId?, nextQuestion?, finished?}` | 未问满：预创建下一题 QA 并下发 nextQaId/nextQuestion（question=话术全文）；问满：finished=true，前端走 finish |
| error | 文本 | 失败提示（不再维护规则降级链路） |

## 3. 交互收口

- **跳过**：`POST /{id}/answer` body 增加 `skip:true`（transcript 可空），滑窗注入"（候选人表示跳过本题）"，同一 SSE 渲染路径；连续 3 次跳过结束确认保留。原 `/next` 端点与 forceNext 删除。
- **提示**：`POST /{id}/hint` 重写为 agent 滑窗提示（基于完整对话上下文给一句思考引导，不泄露答案），hintUsed 计数保留（3 次上限），失败降级固定话术。
- **配置收口**：前端 `VoiceStartConfig` 仅 5 字段（position/jobRequirements/resumeId/difficulty/questionCount）；面试官 agent 由后台 `sys_config: voice.interview.defaultAgentId` 统一配置，删 /agents、/job-templates、/self-intro 端点与页面五个多余下拉（场景/风格/智能体/动态出题/岗位模板）。

## 4. 死代码删减（v11.93 落地）

- `VoiceInterviewServiceImpl` 3584 → 约 1650 行：删除 73 个死方法 + 6 个内部类（围栏模式/规则链路/阶段机/候选人反问/题库出题/上下文重拼全组）。
- 删除 11 个专属类（QuestionPicker/QuestionWeights/QuestionPickCommand/QuestionPickResult/ResumeContext/WebSearchService/NoopWebSearchServiceImpl/InterviewDecisionPolicy/InterviewPromptAssembler/InterviewAnalysisParser + 3 个对应测试）；InterviewTurnResult 保留（批量分析反序列化在用）。
- 保留链路：报告聚合（runBatchAnalysis/aggregateAndStoreReport/buildSummary/buildSuggestion）、断点续接（/active + /resume + rebuildFromDb）、分享、管理端、题库提示端点（/hint?questionId=、/keywords，语音演示页在用）。

## 5. V3 验收

- [x] 回答提交后 delta 首字即显（打字机 + 分句 TTS 边生成边播报，说完才重新开麦）
- [x] 面试官不重复提问（滑窗完整上下文）；二次会话无重复提示词拼接（首轮一次性注入）
- [x] end 事件携带 nextQaId 正确推进轮次（每问新建 QA 行，不覆盖）
- [x] 跳过/提示走 agent 统一链路；问满自动 finished 收尾
- [x] 准备页仅岗位/JD/简历/难度/题数 5 项配置
- [x] 后端 mvn compile + 前端 vue-tsc + vite build 通过

---

# V4 章节：AI 能力架构 v4 整合 · warmup 预热 + RAG 注入 + 四段式段序（v11.94，2026-09-16）

> 依据《AI 能力架构完整设计文档_v4》《AI能力架构v4整合·现状对比与优化实施计划》落地第一批。
> 核心裁决：人设唯一来源在 agent 表（systemPrompt），任务指令留 handler 代码层；speak_text/candidate_ask"存量保留、重新定义角色"（非死链，是待激活能力）。

## 1. 本批修复与新增

### 1.1 P0 修复（两项）

| 问题 | 根因 | 修复 |
| --- | --- | --- |
| P0-1 报告全空分数 | runBatchAnalysis 只写 scoreDraft 不写 score，aggregateAndStoreReport 跳过 score=null 的题 | LLM 分析成功回写 `qa.setScore(analysis.score)` 主分；失败兜底规则分写主分 |
| P0-2 自我介绍识别失效 | V3 首问 idx=0，buildCandidateProfile 按 idx=1 匹配口头自我介绍 | 匹配放宽 `questionIdx <= 1`（旧数据兼容） |

### 1.2 warmup 预热（一次调用）

- `VoiceInterviewHandler` 新增 task=warmup：一次 LLM 调用产出 JSON——`understanding`（候选人画像50字+优势+疑点）/ `interviewPlan.focusAreas[]`（3-5个考察方向+理由+深度）/ `opening`（开场白）/ `firstQuestion`（固定"请自我介绍"）
- `start()` 重构：RAG 检索 → tryWarmup → warmupPlan 存 configMap（断点续接免重算）→ opening = 开场白 + 首题（失败降级 generateOpening，不阻断开面）
- 理解成本前置：warmupPlan 渲染进 system 常驻滑窗（renderWarmupPlanSection），后续每轮提问贴合画像与考察方向

### 1.3 RAG 预热注入（运行时零检索）

- start 时按"岗位 + 简历摘要"检索 agent 绑定知识库 top-5 片段（retrieveKbSnippets），注入 warmup 上下文
- 运行时每轮不检索（保持 delta 流式低延迟）；报告阶段 buildKnowledgePoints 二次检索（岗位+低分题节选 → top-5 相关知识点），旧 tags 路径兼容回退

### 1.4 段序约束（系统提示词，v11.94.1 修订）

buildInterviewerSystemPrompt 重写：

1. 第 1 问固定自我介绍；第 2-3 问针对自我介绍深挖追问
2. 核心问答随机引申（贴合 warmupPlan 考察方向）
3. 问满自然收尾致谢（v11.94.1：反问不再独立成段，融入对话流——候选人回答中口头反问时面试官简答后继续提问；问满后面试官口播"你还有什么想了解的吗？"，候选人无反问或反问完毕即收尾致谢）

### 1.5 候选人反问（v11.94.1：独立链路已移除，融入对话流）

v11.94 曾落地反问段独立链路（askPhase 协议 + POST /{id}/ask + 前端反问输入模式），实测体验不佳（面试未结束时跳出反问输入框打断节奏）而**整体移除**：

- 删除：`candidateAsk` 服务方法/接口声明/`POST /{id}/ask` 端点、runAgentTurn 的 askPhase 分支与 ask_invite 事件、buildTurnDirective 反问邀请指令、前端 enterAskPhase/handleSubmitAsk/反问输入模式/反问态 chatStatus、sys_config 键 voice.interview.candidateAsk.enabled
- 替代：系统提示词承载（§1.4 第 3 条）——候选人想问的可在回答中自然表达，AI 面试官简答后继续提问；滑窗记忆天然支持
- 存量保留：VoiceInterviewHandler 的 candidate_ask task 子任务（无调用方）；历史 candidate_ask/ask_invite 事件数据（链路追溯）

### 1.6 speak_text 语义重定义

- `question` = 结构化问题文本（报告回放用）；`speakText` = 口语化话术（TTS 优先播、空则 fallback 到 question）
- 前端 start/断点续接路径均为 `speakText || question`

### 1.7 残留清理

- 删 `InterviewAgentClient(Impl).dynamicModeEnabled()` 与 CONFIG_KEY_DYNAMIC_MODE（纯残留键，无调用方）

## 2. SSE 协议（v11.94.1 修订）

end 载荷（askPhase 分支已随 v11.94.1 移除）：

```
问满 → { roundDone, finished: true }   // 面试官按提示词口播反问邀请后收尾致谢
未满 → { roundDone, nextQaId, nextQuestion }
```

## 3. 四同步清单

- 代码：VoiceInterviewHandler / VoiceInterviewServiceImpl / IVoiceInterviewService / PortalVoiceInterviewController / InterviewAgentClient(Impl) + voiceInterview.ts / VoiceInterviewPage.vue
- 文档：devlog v11.94 + 本章节
- SQL：`20260916-01-voice-interview-v4-warmup-ask.sql`（sys_config 反问段开关 + 删 voice.interview.dynamicMode；无表结构变更）；`20260916-02-voice-interview-remove-askphase.sql`（v11.94.1 删反问段开关键）
- 菜单：无变更

## 4. V4 验收

- [x] LLM 分析失败也有分数（规则分兜底写主分），报告不再全空
- [x] start 返回开场白+首题（warmup 产出或降级）；二次会话无 warmup 重算（configMap 复用）
- [x] ~~问满后前端进入反问模式（askPhase）~~（v11.94.1 移除，反问融入对话流）
- [x] ~~关闭反问开关后问满直接 finished~~（v11.94.1 开关键已删）
- [x] 后端 mvn compile + 前端 vue-tsc + vite build 通过

## 5. v11.94.1 增量：移除反问段独立链路

- 后端：删 candidateAsk 服务方法/接口声明//ask 端点/askPhase 分支/ask_invite 事件/反问邀请指令/反问段开关读取（sysConfigService 注入随之移除）；buildInterviewerSystemPrompt 段序约束改为"口头反问简答后继续 + 问满口播反问邀请后自然收尾"
- 前端：删 askPhase 全链路（enterAskPhase/handleSubmitAsk/反问输入模式/反问态 chatStatus/跳过按钮禁用/watch 自动开麦拦截）；提交后首字前预建"思考中"占位气泡（T2 体验兜底，delta 到达即续写，失败/中断自动移除空占位）
- SQL：`20260916-02-voice-interview-remove-askphase.sql`（删 voice.interview.candidateAsk.enabled）

# V5 章节：报告质量重设计 · 整场 LLM 复盘 + 报告页重排版（v11.97，2026-09-16）

> 背景：v11.96 修复报告生成链路后，实测报告内容质量不达标（逐题全 50 分兜底、文案模板化、skills 原始 JSON 透出、岗位匹配度=总分硬套、knowledgePoints 无参考价值）。本版从**报告生产方式**与**报告呈现**两层重做。

## 1. 根因与通道选型

| 通道 | 用途 | 实测 |
| --- | --- | --- |
| `aiSceneJsonClient.executeForJson`（voice_interview 场景网关） | 逐题 LLM 分析 | 失败 → 规则兜底 50 分（报告质量差的根因） |
| `agentClient.chatStream/chat`（agent48 直连 ModelConfigService） | 面试主对话 | 可用 |

**决策**：整场复盘复用主对话直连通道（`agentClient.chat`），不再走场景网关；逐题分析失败仍保留规则兜底（链路永远可用）。

## 2. 整场 LLM 复盘架构（enhanceReportByAgent）

```
finish/regenerate → runBatchAnalysis（逐题分析，失败兜底 50 分）
    → aggregateAndStoreReport（规则聚合 + 六维汇总）
    → enhanceReportByAgent（新增）：
        输入 = 岗位 + JD(≤600字) + 简历摘要(≤800字, skills 归一化) + 逐题问答对(题≤150/答≤400字/初评分)
        agentClient.chat → extractJsonObject（剥围栏）→ 解析失败追加严格约束重试 1 次
        逐字段覆盖：overallComment / jobMatch / highlights / weakPoints / suggestions / perQuestion / dimensions
        perQuestion 按 questionIdx 回填 reviews + 回写 QA 表（score/aiFeedback/scoreDraft）
        → fuseTotalScore 同口径重算总分 + summary 同步复盘结论
    → 落库（report/summary/score）
```

**输出 JSON Schema**：

```json
{
  "overallComment": "3-5 句总评（≤每句有信息量）",
  "jobMatch": { "rate": 0-100, "reason": "基于 JD 与简历/问答表现的匹配依据" },
  "highlights": [ { "title": "≤12字", "detail": "结合对话原文的具体证据" } ],
  "weakPoints": [ { "title": "≤12字", "detail": "具体不足与场景" } ],
  "suggestions": [ "每条≤60字可执行建议" ],
  "perQuestion": [ { "questionIdx": 1, "score": 0-100, "aiFeedback": "逐题点评" } ],
  "dimensions": { "relevance/professionalism/fluency/interactivity/confidence/logic": 0-100 }
}
```

**评分区分度约束**（提示词）：优秀 ≥80 / 合格 60-79 / 待提升 <60，禁止平铺兜底分。

**兼容**：新旧字段双写（`highlightViews`/`weakPointViews` 结构化 + 旧 `highlights`/`weakPoints` 标题列表）；旧报告 JSON 反序列化零风险（新字段可空，前端 `?? 回退`）。

**热回退**：sys_config `voice.interview.reportLlm.enabled`（缺省 true，无需 SQL）。

## 3. regenerate 状态机（重新生成报告）

```
POST /api/portal/interview/voice/{id}/regenerate-report（@RateLimiter 10次/h）
  → @Transactional：
      校验归属 + status=finished + RUNNING_ANALYSIS 防重入（"报告正在生成中"）
      主表：analysisStatus=1 / progress=0 / report=null / summary=null
        （report 必须清空——aggregateAndStoreReport 对报告非空仅推进进度直接 return）
      QA 表（已作答）：analysisStatus=null / scoreDraft=null
        （scoreDraft 必须清——二次融合失真；analysisStatus 必须清——runBatchAnalysis 只处理 !=2）
      保留 score/aiFeedback 作参考；recordEvent("regenerate_report")
  → afterCommit 触发 triggerBatchAnalysis（复用 v11.96 事务后回调机制）
  → 前端轮询 5.1 analysis 接口（复用现有进度条链路）
```

## 4. 报告页重排版

- **总览横幅卡**（新增，进度条下方）：综合分（动态色）+ 等级徽章 + LLM 总评 + 岗位匹配度（含依据说明 + 进度条）
- **概要 Tab 三栏**：左列六维雷达（动态色 polygon）+ 自我介绍评分卡（原「面试官剖析」Tab 迁入）；中列 LLM 总评 + AI 深度复盘（心态/流畅度/可疑信号）+ 改进建议（编号列表 + 按建议优化简历入口）；右列结构化亮点/薄弱点卡（title+detail，各取前 4 条）+ 查看详细分析入口
- **问题分析 Tab**：顶部待提升收口区（薄弱点 + 去练习按钮）；逐题卡片补「你的回答」（默认 2 行 line-clamp 折叠，>80 字展开全文）
- **Tab 删减**：「面试官剖析」（迁入概要左列）、「相关知识点」（knowledgePoints 链路整体删除）
- **重新生成入口**：报告页 report-actions 首位按钮（分析中禁用）；历史页卡片按钮 → 跳转 `?id={id}&regenerate=1`，报告页 onMounted 检测参数、报告加载完成后自动触发（skipConfirm，router.replace 清参数防刷新重复触发）

## 5. 四同步清单

- 代码：VoiceInterviewServiceImpl（enhanceReportByAgent/regenerateReport/formatSkills/extractJsonObject/删 RAG 知识点三方法）/ VoiceInterviewReportVO / IVoiceInterviewService / PortalVoiceInterviewController（5.2 端点）+ voiceInterview.ts / VoiceInterviewPage.vue / MyVoiceInterviewsPage.vue
- 文档：devlog v11.97 + 本章节
- SQL：无 DDL；可选热回退键 `voice.interview.reportLlm.enabled`（管理台 sys_config 可配）
- 菜单：无变更

## 6. V5 验收

- [x] 报告逐题分数有区分度（LLM 直连复盘，非全 50 分兜底）
- [x] 总评/亮点/薄弱点/建议基于简历 + JD + 问答内容（非模板文案）
- [x] skills 归一化显示（无原始 JSON 透出）；岗位匹配度独立评估（非总分硬套）
- [x] knowledgePoints 关联题库链路删除（后端三方法 + 前端 Tab）
- [x] 重新生成：报告页按钮 + 历史页入口 + 自动触发；失败/重入有守卫
- [x] 后端 mvn compile + 前端 vue-tsc + vite build 通过

# V6 章节：AI 网关链路根治 · 不可变 Map 击穿输入清洗 + 开关 sys_config 化（v11.98，2026-09-17）

> 背景：v11.97 交付时逐题 LLM 分析仍走规则兜底。DB 取证 `ai_execute_log` 29 条 voice_interview 记录中 27 条 fail（error_msg 逐字为 "not supported"、elapsed 2-20ms、model_used=NULL），2 条 success 均来自 warmup（可变 Map）。JDK 21 实测 `Map.of(...).entrySet()` 的 `setValue()` 抛 `UnsupportedOperationException("not supported")`，消息逐字吻合——根因实锤。

## 1. 根因：sanitizeInputChannel × 不可变 Map

- 网关 `AiGatewayService.sanitizeInputChannel`（v11.57 输入清洗）对 `request.getInput()` 的 String 值原地 `entry.setValue()` 清洗
- 调用方 `VoiceInterviewServiceImpl.analyzeAnswerByLlm` 传 `Map.of("task",..., "context",..., "transcript",...)`（JDK 不可变集合）→ 第一条 entry 即炸 → 网关 catch 记 fail → FallbackStrategy 降级 → `AiSceneJsonClient.executeForJson` 返 null → 逐题 100% 规则兜底 50 分
- 同场对照：`tryWarmup` 用 `new LinkedHashMap<>()` → 2 条 success（25s+ 真实模型调用）；agent48→model17 两通道缓存键相同（`17:0.7:2048:false`），排除模型/JSON Mode 差异

**根治方案（网关侧防御，业务侧统一）**：
- 网关：重建可变 `LinkedHashMap` 而非原地 setValue——任何调用方传任意 Map 实现均安全（String 值过 `PromptInjectionGuard.sanitize`，非 String 原样保留）
- 业务侧：`AiSceneJsonClient.executeForJson` 全部 9 个调用点统一可变 Map（ScoringEngine/analyzeAnswerByLlm/ResumeParseService 本次修复，简历族 6 处原本即 HashMap）

## 2. 运行时开关全面 sys_config 化

- 新建 `com.moyun.ext.ai.service.AiGlobalSwitch`：`isEnabled()`（ai.global.enabled）/ `isResumeAdviceEnabled()`（ai.resume.advice.enabled），缺省 true，true/1 大小写不敏感，异常兜底缺省值；走 RuoYi `ISysConfigService`（Redis 缓存，管理台「参数设置」修改即时失效生效）
- 替换 8 个类的 `aiProperties.isEnabled()/isResumeAdviceEnabled()`：VoiceInterviewServiceImpl（含 tryWarmup）/ InterviewAgentClientImpl（5 处）/ ResumeParseService / ResumeJobMatchService / ResumeDeepOptimizeService（2 处）/ ResumeDeepOptimizeGenerator / ResumeAiAdviceService
- **yaml 职责收缩**：`moyun.ai.enabled` 仅承担 `@ConditionalOnProperty` bean 装配（AiModuleLlmClient/NoopLlmClient 二选一，Noop 的 matchIfMissing=true 陷阱——prod 缺省会误装配 Noop），application.yaml 固定 `true`；application-dev.yaml 删 resume-advice-enabled，运行时开关全部走 sys_config

## 3. 参数拼接契约（业务层→网关→Agent→大模型）

- 业务层按场景拼 input（voice_interview：task=answer_analysis/self_intro 子任务 + context（岗位/题目）+ transcript（转写/回答））；网关 `sanitizeInputChannel` 清洗后进 Handler（VoiceInterviewHandler 校验必填字段、拼系统提示词）→ Agent（agent48 绑定 model17）→ 大模型
- 修正：`analyzeAnswerByLlm` 的 questionTitle 截断 400 字（V3 语义 qa.question 存面试官整段话术含开场寒暄/上轮反馈，非纯题目）
- 残留 `Map.of` 审查：均不在输入清洗路径（recordEvent 事件落库 / ASR HTTP 请求体 / Handler structured 输出 / Controller 响应包装），无雷

## 4. 四同步清单

- 代码：AiGatewayService（sanitizeInputChannel 重建 Map）/ AiGlobalSwitch（新建）/ 8 个业务类开关替换 / application.yaml + application-dev.yaml
- 文档：devlog v11.98 + 本章节
- SQL：`moyun-server/src/main/resources/sql/20260917-01-ai-global-switch-sysconfig.sql`（sys_config 两键幂等 INSERT，WHERE NOT EXISTS）
- 菜单：无变更（管理台「参数设置」原生入口管理）

## 5. 部署与验收

- 部署：执行 SQL `20260917-01` → 重启后端；两开关管理台热调（即时生效无需重启）
- 验收：
  - [ ] 历史页/报告页点「重新生成报告」→ `ai_execute_log` voice_interview 记录全部 success（elapsed 秒级、model_used=17）
  - [ ] 逐题分数有区分度（不再全 50 分兜底）
  - [ ] 管理台关闭 ai.global.enabled → 面试/简历 AI 均走规则兜底；重新开启即恢复
  - [ ] 后端 mvn compile 通过（全路径真编译验证）
