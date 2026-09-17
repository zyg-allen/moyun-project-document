# 旭林知行 - 用户前台

**项目版本**: v11.98
**最后更新**: 2026-09-17

---

## 一、项目简介

**旭林知行**是一个 **AI 驱动的求职面试与学习成长平台**前台门户，为求职者提供 AI 语音面试（ASR 实时转写 + TTS 流式播报 + 整场 LLM 复盘报告）、简历深度优化、题库刷题、OJ 判题、面经复盘、专栏/话题社区等能力。基于 Vue 3 + TypeScript + Tailwind CSS 构建，响应式设计，适配各种屏幕尺寸。

**品牌口号**: 知行合一，助你上岸

---

## 二、技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.15 | 前端框架 |
| Vite | 5.0.12 | 构建工具 |
| TypeScript | 5.3.3 | 编程语言 |
| Pinia | 3.0.4 | 状态管理 |
| Vue Router | 4.2.5 | 路由管理 |
| Tailwind CSS | 3.4.1 | CSS 框架 |
| Quill | 2.0.2 | 富文本编辑器 |
| Marked | 12.0.0 | Markdown 解析 |
| Lucide Vue | 0.511.0 | 图标库 |
| VueUse | 10.9.0 | 组合式工具库 |

---

## 三、功能模块

### 3.1 页面模块（45+ 页面，按业务域分组）

**内容社区**

| 页面 | 说明 |
|------|------|
| `HomePage.vue` | 首页（Hero 轮播、精选/热门文章、分类导航、作者榜、友链） |
| `FeedPage.vue` | 关注流信息流 |
| `ArticleDetailPage.vue` | 文章详情（评论区、作者信息、收藏点赞） |
| `ListPage.vue` / `SearchPage.vue` | 文章列表（分类/标签筛选）、全文搜索 |
| `ColumnsPage.vue` / `ColumnDetailPage.vue` / `ColumnEditPage.vue` / `MyColumnsPage.vue` | 专栏（浏览/详情/编辑/我的） |
| `TopicListPage.vue` / `TopicDetailPage.vue` / `TopicCreatePage.vue` / `TopicEditPage.vue` / `MyTopicsPage.vue` / `MyTopicPostsPage.vue` | 话题讨论 |
| `MyArticlesPage.vue` / `PublishPage.vue` | 我的文章、发布（富文本/Markdown 双编辑器） |

**面试与简历（interview/）**

| 页面 | 说明 |
|------|------|
| `InterviewPage.vue` | 面试指南（面经/指南导流） |
| `voiceInterview/` | AI 语音面试（准备页 + 对话页：ASR/TTS 音浪可视化、时长制倒计时、简历面板） |
| `MyVoiceInterviewsPage.vue` | 历史面试（对话回放、报告进度轮询、重新生成报告） |
| `VoiceInterviewReportPage` | 面试报告（LLM 复盘总评/六维雷达/逐题分析/结构化亮点薄弱点） |
| `InterviewReportSharePage` | 报告分享（token 免登录公开访问） |
| `resume/` | 简历中心（上传解析、岗位匹配、深度优化采纳 diff、AI 建议、VIP 付费点） |

**学习工具（learn/、tools/）**

| 页面 | 说明 |
|------|------|
| `learn/` | 刷题中心（题库练习、ACM 编程题接真实 OJ、刷题日历、错题本、知识图谱、排行榜） |
| `ContestListPage.vue` / `ContestDetailPage.vue` | 竞赛列表/详情 |
| `tools/` | 实用工具箱（在线运行等） |

**成长与激励**

| 页面 | 说明 |
|------|------|
| `GrowthTimelinePage.vue` | 成长时间线（学习/面试/阅读/写作统一事件） |
| `GrowthRankingPage.vue` | 成长排行榜 |
| `AchievementsPage.vue` | 成就徽章 |

**交易与钱包（pay/）**

| 页面 | 说明 |
|------|------|
| `pay/` | 付费阅读收银台、VIP 会员（面试/简历）、打赏、我的钱包/账单 |

**读书空间（reading/）**

| 页面 | 说明 |
|------|------|
| `ReadingPage.vue` + `reading/` | 书籍/书单/金句/书架阅读专区 |

**用户与认证**

| 页面 | 说明 |
|------|------|
| `LoginPage.vue` / `RegisterPage.vue` / `ForgotPasswordPage.vue` | 登录/注册/找回密码（邮箱验证码） |
| `UserPage.vue` / `UserProfilePage.vue` / `UserSettingsPage.vue` | 用户中心/个人资料/账号设置 |
| `AuthorPage.vue` / `AuthorsPage.vue` / `FollowListPage.vue` | 作者主页/作家名录/关注列表 |
| `CreatorCertificationPage.vue` | 创作者认证 |
| `MessagesPage.vue` / `NotificationBell` | 私信/系统通知 |
| `MyReportsPage.vue` / `MyFeedbackPage.vue` / `ReportFeedback.vue` | 举报/反馈 |
| `AboutUs.vue` / `HelpCenter.vue` / `UserAgreement.vue` / `NotFoundPage.vue` | 静态页/404 |

### 3.2 公共组件

`Layout.vue`（统一布局）/ `Navbar.vue` / `SiteFooter.vue` / `ArticleCard.vue` / `Avatar.vue` / `Pagination.vue` / `Breadcrumb.vue` / `Empty.vue` / `BackToTop.vue` / `ErrorBoundary.vue` / `LazyImage.vue` / `QuillEditor.vue` / `MarkdownEditor.vue` / `MarkdownRenderer.vue` / `NotificationBell.vue` 等，位于 `src/components/`。

### 3.3 状态管理

| Store | 文件 | 说明 |
|--------|------|------|
| 用户状态 | `stores/user.ts` | 用户信息、登录状态、Token 管理 |
| 文章状态 | `stores/article.ts` | 文章列表、详情缓存 |
| 简历状态 | `stores/resume.ts` | 简历选择/优化任务进度 |
| 消息状态 | `stores/message.ts` | 未读消息/通知计数 |

### 3.4 API 接口（41 个模块，`src/api/`）

| 业务域 | 模块 |
|--------|------|
| 内容社区 | `article` / `articleVersion` / `column` / `comment` / `topic` / `category` / `tag` / `feed` / `follow` / `friendLink` / `search`(client) |
| 面试简历 | `interview` / `voiceInterview` / `resumeOptimize` / `resumeOptimizeVip` / `interviewVip` / `company` |
| 学习工具 | `learn` / `learnStats` / `judge` / `codeRun` / `contest` / `dict` |
| 成长激励 | `growth` / `report` / `creator` / `certification` |
| 交易钱包 | `pay` / `tip` / `ad` |
| AI 能力 | `ai` / `aiTask` / `prompt` |
| 用户消息 | `user` / `message` / `notification` / `sms` / `upload` / `file` / `help` / `reading` / `client`（HTTP 封装） |

---

## 四、项目结构

```
moyun-portal/
├── src/
│   ├── api/              # API 接口定义（41 个模块 + client.ts HTTP 封装）
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件（Layout/Navbar/ArticleCard/编辑器…）
│   ├── composables/      # 组合式函数（useAuth/useTheme…）
│   ├── data/             # 静态数据
│   ├── lib/              # 工具库
│   ├── pages/            # 页面组件（45+ 页面，按业务域子目录分组）
│   │   ├── interview/    # 面试（voiceInterview 语音面试 / resume 简历）
│   │   ├── learn/        # 学习（刷题/日历/错题本/知识图谱）
│   │   ├── pay/          # 交易（收银台/VIP/打赏/钱包）
│   │   ├── reading/      # 读书空间
│   │   ├── tools/        # 工具箱
│   │   └── *.vue         # 内容社区/用户/静态页
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia 状态管理（user/article/resume/message）
│   ├── types/            # TypeScript 类型定义
│   ├── utils/            # 工具函数（theme/seo/validation…）
│   ├── App.vue           # 根组件
│   ├── main.ts           # 入口文件
│   └── style.css         # 全局样式（CSS 变量主题）
├── public/               # 公共资源（favicon/robots.txt/sitemap.xml）
├── index.html
├── package.json
├── vite.config.ts        # Vite 配置（含 /api 代理）
├── tailwind.config.js
├── tsconfig.json
├── postcss.config.js
└── README.md
```

---

## 五、快速开始

### 5.1 安装依赖

```bash
cd moyun-portal
npm install
# 或使用 pnpm
pnpm install
```

### 5.2 开发模式

```bash
npm run dev -- --host 0.0.0.0 --port 3000
```

访问地址: http://localhost:3000

### 5.3 构建生产版本

```bash
npm run build
```

### 5.4 代码检查

```bash
# 类型检查
npm run check

# 代码检查
npm run lint

# 自动修复
npm run lint:fix
```

---

## 六、主题系统

项目内置三种主题：

| 主题 | CSS 变量 | 说明 |
|------|----------|------|
| 日间主题 | `--color-bg` = #ffffff | 明亮的白色背景 |
| 夜间主题 | `--color-bg` = #1a1a2e | 深色模式，保护眼睛 |
| 护眼主题 | `--color-bg` = #fdf6e3 | 暖黄色调，适合长时间阅读 |

主题切换通过 `composables/useTheme.ts` 管理。

---

## 七、API 代理配置

开发环境通过 Vite 代理连接后端：

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

生产环境需要配置 Nginx 反向代理。

---

## 八、开发规范

### 8.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 组件 | PascalCase | `ArticleCard.vue` |
| 页面 | PascalCase | `HomePage.vue` |
| 变量 | camelCase | `articleList` |
| 常量 | UPPER_SNAKE | `API_BASE_URL` |
| 函数 | camelCase | `getArticleList()` |
| CSS 类 | kebab-case | `article-card` |

### 8.2 API 调用规范

```typescript
// 1. 定义接口 (src/api/article.ts)
export const getArticleList = (params: ArticleQuery) => {
  return httpGetList('/portal/article/list', params);
};

// 2. 页面中调用
import { getArticleList } from '@/api/article';

const loadArticles = async () => {
  const res = await getArticleList({ pageNum: 1, pageSize: 10 });
  articles.value = res.list;
};
```

### 8.3 组件使用规范

```vue
<template>
  <div class="article-card">
    <h3>{{ title }}</h3>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

defineProps<{
  title: string;
}>();

const count = ref(0);
</script>

<style scoped>
.article-card {
  padding: 16px;
}
</style>
```

---

## 九、相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目总览 | [../README.md](../README.md) | 项目整体介绍 |
| 项目文档索引 | [../docs/README.md](../docs/README.md) | 全量文档索引（01-09 分类） |
| 开发文档 | [../docs/02-开发指南/开发指南.md](../docs/02-开发指南/开发指南.md) | 开发规范与最佳实践 |
| 部署文档 | [../docs/03-部署运维/部署指南.md](../docs/03-部署运维/部署指南.md) | 部署与验证指南 |

---

**项目维护者**: 旭林知行开发团队
**最后更新**: 2026-09-17