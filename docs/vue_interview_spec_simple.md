---
AIGC:
    Label: "1"
    ContentProducer: 001191110102MACQD9K64018705
    ProduceID: 538124306360332_0-drive/215390244592815511/vue_interview_spec_simple.md
    ReservedCode1: ""
    ContentPropagator: 001191110102MACQD9K64028705
    PropagateID: 538124306360332#1787047308677
    ReservedCode2: ""
---
# AI 语音面试系统 · Vue 前端开发规格

## 技术栈
Vue 3 + TypeScript + Vite + Vue Router + Pinia + UnoCSS（或 Tailwind）

## 主题色
红色系 `#DC2626`，渐变 `linear-gradient(135deg, #DC2626, #991B1B)`

---

## 页面结构（4 页）

### 1. 首页 `/`
- 红色渐变全屏背景 + 白色光晕
- 品牌徽章（半透明胶囊）→ 大标题"面试学习中心"
- 2×2 功能卡片：AI 语音面试官（主推）、AI 模拟面试、一键 AI 简历、深度面试复盘
- 卡片 hover 上浮 + 顶部红色条出现

### 2. 准备页 `/interview/prep`
- 顶部导航栏（Logo + 面包屑 + 返回按钮）
- 三步步骤条：设备检测 → 岗位与简历 → 开始面试
- 卡片 1：设备检测（麦克风/扬声器/耳机，带测试按钮）
- 卡片 2：岗位选择（radio 列表）+ 4 个下拉配置（风格/难度/场景/题数）+ 简历上传区 + 在线简历预览
- 卡片 3：偏好开关（智能提示/静音模式）
- 底部全宽红色"开始面试"按钮

### 3. 面试进行页 `/interview/session`（最复杂）
- 顶栏：Logo + 计时器 + 暂停/下一题/结束按钮
- 三栏布局：左 220px + 中自适应 + 右 260px
- **左栏**：面试官头像卡（脉冲动画）+ 题目进度时间线
- **中栏**：对话气泡流
  - 4 种气泡：题目（黄色左边框）、用户回答（右对齐红底）、AI 分析（绿底）、AI 解答（灰底，可含代码块）
  - 底部输入区：textarea + 麦克风按钮 + 回答计时 + 提交按钮
- **右栏**：SVG 雷达图（6 维度）+ 维度标签 + 面试官提示

### 4. 复盘报告页 `/interview/report/:id`
- 胶囊式 Tab：面试概要 / 问题分析 / 面试官剖析 / 相关知识点
- Tab 1：三栏（雷达图 + 概要文字 + 优缺点列表，优缺点可锚点跳回对话）
- Tab 2：自适应卡片网格，每卡含标题 + 分析 + 分数进度条
- Tab 3：面试官评价段落 + 编号改进建议列表
- Tab 4：知识点卡片网格，顶部红色条，hover 上浮
- 底部操作栏：错题本 / 下载 PDF / 分享 / 开始新面试

---

## 核心组件

| 组件 | 说明 |
|------|------|
| `ChatBubble` | 对话气泡，根据 role 切换 4 种样式 |
| `CodeBlock` | 代码块，深色背景 + 语法高亮 |
| `RadarChart` | SVG 雷达图，5-6 维度 |
| `StepIndicator` | 准备页步骤条 |
| `ScoreBar` | 分数进度条，颜色随分数段变化 |
| `FeatureCard` | 首页功能卡片 |

---

## 关键数据

```typescript
// 对话消息
interface ChatMessage {
  id: string
  role: 'question' | 'user' | 'analysis' | 'ai'
  content: string
  codeBlocks?: { language: string; code: string }[]  // AI 解答可能含代码
  highlights?: string[]   // 分析：亮点
  gaps?: string[]         // 分析：缺口
}

// 维度评分（雷达图数据）
interface DimensionScore {
  key: 'relevance' | 'professionalism' | 'fluency' | 'interactivity' | 'confidence' | 'logic'
  label: string
  score: number  // 0-100
}

// 面试配置
interface InterviewConfig {
  positionId: string
  interviewerStyle: 'gentle' | 'standard' | 'pressure'
  difficulty: 'junior' | 'mid' | 'senior' | 'expert'
  scenario: 'technical' | 'project' | 'hr' | 'comprehensive'
  questionCount: 3 | 5 | 8
}
```

---

## MVP 建议
1. 先跑通 4 页路由 + 静态 mock 数据渲染
2. 面试进行页用假数据模拟对话流，后续接 LLM API
3. 语音能力（ASR/TTS）最后做，先用 textarea 输入
4. 雷达图可手写 SVG 或用 ECharts
5. 代码高亮用 highlight.js 或 shiki

---

> 本内容由 Coze AI 生成，请遵循相关法律法规及《人工智能生成合成内容标识办法》使用与传播。
