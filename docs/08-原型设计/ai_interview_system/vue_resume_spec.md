# AI 智能面试系统 - 简历模块 Vue 开发规格

## 一、项目概述

基于 Vue 3 全家桶开发 AI 智能面试系统的简历模块，包含两个核心页面：**简历维护页**和**简历优化页**。UI 风格统一、红色主题、企业级质感。

## 二、技术栈

- Vue 3 + TypeScript + Vite
- Vue Router（路由管理）
- Pinia（状态管理）
- TailwindCSS 或 UnoCSS（原子化样式）
- 图标：Font Awesome 6 或 Iconify
- 图表：ECharts（评分仪表盘）或手写 SVG

## 三、设计系统（Design Tokens）

```
// 主色
--primary: #DC2626
--primary-light: #EF4444
--primary-dark: #B91C1C
--primary-bg: #FEF2F2 (red-50)
--primary-border: #FECACA (red-200)

// 中性色
--gray-50 ~ --gray-900: Tailwind gray 色阶

// 语义色
--success: #059669    --success-bg: #ECFDF5
--warning: #D97706    --warning-bg: #FFFBEB
--info: #2563EB       --info-bg: #EFF6FF

// 圆角
radius-sm: 6px   radius: 10px   radius-lg: 14px   radius-xl: 20px

// 阴影
shadow-xs: 0 1px 2px rgba(0,0,0,0.04)
shadow: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)
shadow-md: 0 4px 6px -1px rgba(0,0,0,0.07), 0 2px 4px -2px rgba(0,0,0,0.05)
shadow-lg: 0 10px 15px -3px rgba(0,0,0,0.08), 0 4px 6px -4px rgba(0,0,0,0.04)

// 动画
ease: cubic-bezier(0.4, 0, 0.2, 1)
ease-bounce: cubic-bezier(0.34, 1.56, 0.64, 1)
```

## 四、路由结构

```
/resume
  /resume/edit       → ResumeEditPage.vue      简历维护页
  /resume/optimize   → ResumeOptimizePage.vue   简历优化页
```

## 五、页面一：简历维护页 ResumeEditPage

### 布局：三栏结构

| 区域 | 宽度 | 说明 |
|------|------|------|
| 左侧栏 | 228px, sticky | 锚点导航 + 完善进度 + CTA 入口 |
| 中间主区 | flex:1, max 820px | 表单内容，各模块为卡片 |
| 右侧栏 | 268px, sticky | 评分面板 + 文件列表 |
| 底部操作栏 | fixed bottom | 撤销/预览/下载/保存/AI优化 |

### 5.1 左侧栏组件 `ResumeSidebar`

- **锚点导航**：7 个模块项（个人信息/求职意向/教育背景/工作经历/项目经历/专业技能/自我评价）+ 1 个上传入口
- 每项右侧有状态小圆点：绿色(已填)/橙色(待完善)/灰色(未填)
- 当前滚动到哪个区域，对应项高亮（scroll spy）
- **进度条**：显示 `X/7 已完成`，红色渐变填充
- **CTA 卡片**：显示当前评分 + "立即 AI 优化" 按钮

### 5.2 表单内容区

每个模块用 `SectionCard` 组件包裹：
- 头部：图标(带颜色) + 标题 + 描述 + 状态标签(已填写/待完善/未上传)
- 主体：表单内容

#### 组件清单

| 组件名 | 用途 | 说明 |
|--------|------|------|
| `SectionCard` | 模块卡片容器 | header + body，hover 变边框色 |
| `FormField` | 表单项 | label + input/select/textarea + hint/error/counter |
| `EntryCard` | 经历条目 | 可折叠的卡片，含编辑/删除按钮 |
| `SkillCloud` | 技能标签云 | chip 样式，Enter 添加，点击 x 删除 |
| `UploadZone` | 上传区域 | 拖拽上传，支持 PDF/DOCX，≤10MB |
| `FileItem` | 已上传文件 | 图标 + 名称 + 大小 + 预览/同步按钮 |

#### 各模块字段

**个人信息**（2列网格）
- 姓名* / 手机号* / 邮箱* / 所在城市 / 工作年限(select) / 最高学历(select) / GitHub

**求职意向**（3列网格）
- 期望岗位* / 期望城市 / 期望薪资(select)

**教育背景**（EntryCard，3列字段）
- 学校* / 专业* / 学历(select) / 入学时间(month) / 毕业时间(month) / GPA
- 支持"添加教育经历"

**工作经历**（EntryCard，2列+全宽）
- 公司* / 职位* / 入职时间 / 离职时间(+至今checkbox) / 工作描述(textarea，字数统计)

**项目经历**（EntryCard，2列+全宽）
- 项目名* / 角色 / 起止时间 / 项目描述(textarea)

**专业技能**
- 标签云 + 输入框(Enter添加)

**自我评价**
- textarea，底部字数统计 `n/300`

**上传简历**
- UploadZone（拖拽区域）
- 上传后显示 FileItem 列表

### 5.3 右侧栏组件 `ScorePanel`

- **评分仪表盘**：SVG 环形图，显示分数(如62/100)，等级描述
- **四维度进度条**：内容完整度/关键词/专业表达/数据量化
- **洞察提示**：黄色提示框，显示可优化项数 + "立即优化"按钮
- **文件列表**：在线简历(当前标签) + 已上传文件列表

### 5.4 底部操作栏 `ActionBar`

- 左侧：自动保存状态指示器(绿色闪烁圆点)
- 右侧：撤销 / 预览 / 下载 / 保存 / AI优化(主按钮)
- fixed 定位，毛玻璃背景

## 六、页面二：简历优化页 ResumeOptimizePage

### 布局：两栏

| 区域 | 宽度 | 说明 |
|------|------|------|
| 左侧栏 | 200px, sticky | 简历选择卡片 |
| 中间主区 | flex:1, max 880px | 评分+优化内容 |

### 6.1 左侧简历选择 `ResumeSelector`

- 卡片列表：在线简历(高亮)/已上传PDF/已上传DOCX
- 每张卡片：图标+名称+时间+标签(当前/已上传)
- 底部"返回编辑"按钮

### 6.2 评分概览 `AnalysisOverview`

- 左侧：大数字评分(56px, 红色渐变文字)
- 右侧：总评标题 + 描述 + 标签(绿色=优势/橙色=待改善)
- 底部：四维度进度条(2x2 网格)

### 6.3 深度优化入口 `DeepOptBanner`

- 渐变背景卡片(红→橙→黄)
- 标题+描述+功能标签(逐段点评/智能改写/关键词优化/一键采纳)
- "开始深度优化" 主按钮

### 6.4 优化进度 `OptProgress`（点击优化后显示）

- 四步进度条：加载简历 → 解析内容 → AI分析 → 生成报告
- 步骤圆点状态：灰(未到)/红色脉冲(进行中)/绿色✓(完成)
- 进度条：红色渐变填充 + 光泽动画(shimmer)
- 文字：当前步骤描述 + 百分比

进度模拟数据：
```
15% 加载简历内容
30% 解析个人信息
45% 分析教育背景
60% 评估工作经历
75% 分析项目经历
88% 生成优化建议
95% 生成分析报告
100% 完成
```

### 6.5 优化结果 `AnalysisResults`（进度完成后显示）

每模块一个 `AnalysisCard`：

**卡片结构**：
- 头部：模块图标+名称 | 单项评分标签
- 内容：
  - 问题反馈块(红底)：指出存在什么问题
  - 建议反馈块(绿底)：给出优化方向
  - Diff 对比块：AI 优化后的结果，关键词高亮(green background)
  - 操作按钮：采纳(同步到编辑区) / 手动修改(展开 textarea)
  - 手动修改区：textarea + 确认替换/取消

**五个模块**：
1. 教育背景 - 78分
2. 工作经历 - 58分（有问题+建议+优化结果）
3. 项目经历 - 60分
4. 专业技能 - 72分
5. 自我评价 - 45分

### 6.6 底部操作区 `OptFooter`

- 左侧：完成提示 + 预计提升分数
- 右侧：下载报告 / 保存同步 / 发布简历(主按钮)

## 七、全局组件

### `TopNav` 顶部导航
- Logo + 系统名 | 面包屑 | Tab切换(面试/简历维护/简历优化) | 用户头像+会员标签
- 红色 Logo 方块图标

### `Toast` 消息提示
- 右上角弹出，卡片式(白底+阴影)，带图标(成功绿/警告橙/错误红)
- 进入动画：从右侧滑入，2.5s 后滑出

### `Modal` 弹窗
- 遮罩层(blur背景)
- 简历预览弹窗：完整简历排版展示
- 发布确认弹窗：警告提示 + 确认/取消

## 八、Pinia Store 设计

```typescript
// stores/resume.ts
interface ResumeState {
  // 基本信息
  personal: { name, phone, email, city, workYears, degree, github }
  // 求职意向
  objective: { position, city, salary }
  // 教育背景[]
  education: EducationItem[]
  // 工作经历[]
  work: WorkItem[]
  // 项目经历[]
  project: ProjectItem[]
  // 技能[]
  skills: string[]
  // 自我评价
  evaluation: string
  // 上传文件[]
  uploadedFiles: FileInfo[]
  // 评分
  score: { total, dimensions: { completeness, keywords, professionalism, quantification } }
  // 优化结果
  optimization: AnalysisResult[] | null
}
```

## 九、关键交互

1. **ScrollSpy**：主区滚动时自动高亮左侧对应导航项
2. **自动保存**：表单变化后 debounce 1s 自动保存，显示"已自动保存"
3. **技能标签**：Enter 添加，点击 x 删除
4. **文件上传**：拖拽高亮，松手触发上传模拟
5. **优化进度**：8 步递进，每步 550ms 间隔，带 shimmer 动画
6. **采纳操作**：按钮变灰+文字变"已采纳"，Toast 提示
7. **手动修改**：展开 textarea 区域，确认后收起
8. **发布确认**：弹窗二次确认，提示未采纳项

## 十、响应式断点

| 断点 | 处理 |
|------|------|
| ≤1280px | 隐藏右侧栏 |
| ≤1024px | 隐藏左侧导航栏，底部操作栏全宽 |
| ≤768px | 单列布局，表单竖排，隐藏优化侧栏 |

## 十一、MVP 开发优先级

1. 路由 + 布局骨架 + TopNav
2. 简历维护页表单（静态数据先跑通）
3. 简历优化页评分 + 优化卡片
4. 进度动画 + 采纳/手动编辑交互
5. Toast + Modal + 预览弹窗
6. ScrollSpy + 自动保存
7. 上传功能 + 文件管理
8. 响应式适配

## 十二、注意事项

- 所有按钮点击有 `scale(0.97)` 按压反馈
- 卡片 hover 边框色变深 + 微阴影
- 输入框 focus 有红色外发光 (box-shadow)
- 动画使用 CSS transition，不用 JS 动画库
- 评分环形图用 SVG stroke-dasharray 实现
- 进度条有 shimmer 光泽动画（CSS ::after 伪元素）
- 弹窗进入有 slideUp + bounce 缓动
- 文件上传区拖拽时 scale(1.01) 放大反馈
