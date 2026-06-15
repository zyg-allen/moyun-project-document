# 一纸墨 - 用户前台

**项目版本**: v3.1
**最后更新**: 2026-06-15

---

## 一、项目简介

**一纸墨**是一个面向年轻创作者、技术学习者、文学爱好者的**文学+技术双内容形态共存的社区平台**前台门户。基于 Vue 3 + TypeScript + Tailwind CSS 构建，响应式设计，完美适配各种屏幕尺寸。

**品牌口号**: 在浮躁的世界，留一页纸给灵魂。

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

### 3.1 已完成功能

| 模块 | 页面/功能 | 说明 |
|------|-----------|------|
| **首页** | `HomePage.vue` | Hero 轮播、文章列表、分类导航、热门标签、友情链接 |
| **文章详情** | `ArticleDetailPage.vue` | 文章展示、评论区、作者信息、收藏点赞 |
| **文章列表** | `ListPage.vue` | 分类筛选、标签筛选、分页加载 |
| **搜索页** | `SearchPage.vue` | 关键词搜索、筛选功能 |
| **发布页** | `PublishPage.vue` | 富文本/Markdown 双编辑器、分类标签选择、封面上传 |
| **用户中心** | `UserPage.vue` | 用户信息、我的文章、收藏、设置 |
| **个人资料** | `UserProfilePage.vue` | 头像编辑、个人信息修改 |
| **账号设置** | `UserSettingsPage.vue` | 账号安全、偏好设置 |
| **登录页** | `LoginPage.vue` | 用户登录、Token 认证 |
| **注册页** | `RegisterPage.vue` | 用户注册、信息验证 |
| **作者页** | `AuthorPage.vue` | 作者主页、作者文章列表 |
| **作家名录** | `AuthorsPage.vue` | 优秀作者展示 |
| **读书空间** | `ReadingPage.vue` | 阅读专区 |
| **面试指南** | `InterviewPage.vue` | 面试经验分享 |
| **关于我们** | `AboutUs.vue` | 平台介绍 |
| **帮助中心** | `HelpCenter.vue` | 使用帮助 |
| **用户协议** | `UserAgreement.vue` | 用户协议 |
| **反馈建议** | `ReportFeedback.vue` | 意见反馈 |
| **404页面** | `NotFoundPage.vue` | 友好错误页 |

### 3.2 公共组件

| 组件 | 说明 |
|------|------|
| `Layout.vue` | 统一布局组件（头部+内容+底部） |
| `Navbar.vue` | 导航栏组件 |
| `SiteFooter.vue` | 页脚组件 |
| `ArticleCard.vue` | 文章卡片组件 |
| `RelatedArticleCard.vue` | 相关文章卡片 |
| `Avatar.vue` | 用户头像组件 |
| `Pagination.vue` | 分页组件 |
| `TagList.vue` | 标签列表组件 |
| `Breadcrumb.vue` | 面包屑导航 |
| `Empty.vue` | 空状态组件 |
| `BackToTop.vue` | 返回顶部组件 |
| `ErrorBoundary.vue` | 错误边界组件 |
| `LazyImage.vue` | 懒加载图片组件 |
| `QuillEditor.vue` | Quill 富文本编辑器封装 |
| `MarkdownEditor.vue` | Markdown 编辑器组件 |
| `MarkdownRenderer.vue` | Markdown 渲染组件 |
| `NotificationBell.vue` | 通知铃铛组件 |
| `BackButton.vue` | 返回按钮组件 |

### 3.3 状态管理

| Store | 文件 | 说明 |
|--------|------|------|
| 用户状态 | `stores/user.ts` | 用户信息、登录状态、Token 管理 |
| 文章状态 | `stores/article.ts` | 文章列表、详情缓存 |

### 3.4 API 接口

| 模块 | 文件 | 说明 |
|------|------|------|
| 用户认证 | `api/user.ts` | 登录、注册、用户信息 |
| 文章管理 | `api/article.ts` | 文章 CRUD、点赞、收藏 |
| 分类管理 | `api/category.ts` | 分类列表、详情 |
| 标签管理 | `api/tag.ts` | 标签列表、详情 |
| 评论管理 | `api/comment.ts` | 评论 CRUD |
| 收藏管理 | `api/bookmark.ts` | 收藏操作 |
| 关注管理 | `api/follow.ts` | 关注/取关 |
| 文件上传 | `api/upload.ts` | 图片/文件上传 |
| 搜索功能 | `api/search.ts` | 搜索接口 |
| 通知功能 | `api/notification.ts` | 通知列表 |
| 读书空间 | `api/reading.ts` | 读书相关接口 |
| 面试指南 | `api/interview.ts` | 面试相关接口 |
| 客户端封装 | `api/client.ts` | HTTP 请求封装、拦截器 |

---

## 四、项目结构

```
moyun-portal/
├── src/
│   ├── api/              # API 接口定义
│   │   ├── client.ts     # HTTP 客户端封装
│   │   ├── user.ts       # 用户接口
│   │   ├── article.ts    # 文章接口
│   │   ├── category.ts   # 分类接口
│   │   ├── tag.ts        # 标签接口
│   │   ├── comment.ts     # 评论接口
│   │   └── ...
│   ├── assets/           # 静态资源
│   │   └── images/       # 图片资源
│   ├── components/       # Vue 组件
│   │   ├── ArticleCard.vue
│   │   ├── Layout.vue
│   │   ├── Navbar.vue
│   │   ├── SiteFooter.vue
│   │   └── ...
│   ├── composables/       # 组合式函数
│   │   ├── useAuth.ts    # 认证逻辑
│   │   ├── useTheme.ts   # 主题管理
│   │   └── useLazyImage.ts
│   ├── data/             # 静态数据
│   │   ├── categories.ts # 分类数据
│   │   └── mockData.ts  # 模拟数据
│   ├── lib/              # 工具库
│   │   └── utils.ts
│   ├── pages/            # 页面组件
│   │   ├── HomePage.vue
│   │   ├── ArticleDetailPage.vue
│   │   ├── ListPage.vue
│   │   ├── LoginPage.vue
│   │   ├── RegisterPage.vue
│   │   ├── PublishPage.vue
│   │   ├── UserPage.vue
│   │   └── ...
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # Pinia 状态管理
│   │   ├── user.ts
│   │   └── article.ts
│   ├── types/            # TypeScript 类型定义
│   │   ├── index.ts
│   │   └── api.ts
│   ├── utils/            # 工具函数
│   │   ├── theme.ts      # 主题工具
│   │   ├── seo.ts        # SEO 工具
│   │   ├── validation.ts  # 验证工具
│   │   └── ...
│   ├── App.vue           # 根组件
│   ├── main.ts           # 入口文件
│   └── style.css         # 全局样式
├── public/               # 公共资源
│   ├── favicon.svg
│   ├── robots.txt
│   └── sitemap.xml
├── index.html
├── package.json
├── vite.config.ts
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
| 开发文档 | [../docs/03_开发指南.md](../docs/03_开发指南.md) | 开发规范与最佳实践 |
| 部署文档 | [../docs/04_部署指南.md](../docs/04_部署指南.md) | 部署与验证指南 |
| API 文档 | [API文档.md](./API文档.md) | 详细 API 接口文档 |
| 品牌设计 | [网站名称设计.md](./网站名称设计.md) | 品牌定位与文案 |
| 类型检查报告 | [STRICT_TYPE_CHECK_REPORT.md](./STRICT_TYPE_CHECK_REPORT.md) | TypeScript 严格模式检查报告 |

---

**项目维护者**: 一纸墨开发团队
**最后更新**: 2026-06-15