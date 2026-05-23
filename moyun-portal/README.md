# ZYG Web Portal - 文学+技术社区门户

一个基于 Vue 3 + Vite 5 + TypeScript + Tailwind CSS 构建的现代化文学+技术社区门户网站。

## 技术栈

- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite 5
- **路由**: Vue Router 4
- **样式**: Tailwind CSS 3
- **语言**: TypeScript
- **图标**: Lucide Vue Next

## 项目功能

### 已完成功能
- ✅ **首页**: Hero 轮播、文章列表、分类导航、热门标签
- ✅ **文章详情页**: 文章展示、评论区、相关推荐
- ✅ **搜索页**: 关键词搜索、分类筛选、标签筛选
- ✅ **个人中心**: 用户信息、我的文章、设置
- ✅ **登录/注册页**: 用户认证
- ✅ **发布页**: 富文本/Markdown 编辑器切换、分类标签选择
- ✅ **404 页面**: 友好的错误页面
- ✅ **主题切换**: 日间主题、夜间主题、护眼主题
- ✅ **响应式设计**: 适配各种屏幕尺寸
- ✅ **公共布局组件**: 统一的头部和底部

## 项目结构

```
/workspace
├── src/
│   ├── assets/            # 静态资源
│   ├── components/        # 组件
│   │   ├── ArticleCard.vue    # 文章卡片
│   │   ├── Empty.vue          # 空状态组件
│   │   ├── Layout.vue         # 公共布局组件
│   │   └── Navbar.vue         # 导航栏
│   ├── composables/       # 组合式函数
│   │   └── useTheme.ts        # 主题管理
│   ├── data/             # 数据层
│   │   └── mockData.ts       # 模拟数据（后续替换为真实 API）
│   ├── lib/              # 工具库
│   │   └── utils.ts          # 工具函数
│   ├── pages/            # 页面组件
│   │   ├── HomePage.vue         # 首页
│   │   ├── ArticleDetailPage.vue # 文章详情
│   │   ├── SearchPage.vue       # 搜索页
│   │   ├── UserPage.vue         # 用户中心
│   │   ├── LoginPage.vue        # 登录页
│   │   ├── RegisterPage.vue     # 注册页
│   │   ├── PublishPage.vue      # 发布页
│   │   └── NotFoundPage.vue     # 404页
│   ├── router/           # 路由
│   │   └── index.ts
│   ├── types/            # TypeScript 类型定义
│   │   └── index.ts
│   ├── utils/            # 工具
│   │   └── theme.ts
│   ├── App.vue           # 根组件
│   ├── main.ts           # 入口文件
│   └── style.css         # 全局样式
├── public/               # 公共资源
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
└── postcss.config.js
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 推送到码云

项目已成功初始化 git 仓库，你可以按以下步骤推送到码云：

```bash
# 设置你的 git 用户名和邮箱（如果还没有设置）
git config user.name "你的名字"
git config user.email "你的邮箱"

# 添加远程仓库（已添加）
git remote add origin https://gitee.com/zygbo/zyg-web-portal.git

# 切换到 main 分支（已切换）
git branch -M main

# 推送代码（需要认证）
git push -u origin main
```

### 如果提示认证问题，你可以：

1. **使用个人访问令牌**: 在码云设置中生成个人访问令牌，在提示输入密码时输入令牌
2. **使用 SSH 方式**: 
   ```bash
   git remote set-url origin git@gitee.com:zygbo/zyg-web-portal.git
   git push -u origin main
   ```

## API 替换指南

当前使用模拟数据，后续替换为真实 API 时：

1. 修改 `src/data/mockData.ts` 中的函数，改为调用真实 API
2. 推荐使用 `axios` 或 `fetch` 进行 HTTP 请求
3. 可以考虑添加状态管理（Pinia 或 Vuex）来管理全局状态

## 主题系统

项目内置三种主题：
- **日间主题** (`light`): 明亮的白色背景
- **夜间主题** (`dark`): 深色模式，保护眼睛
- **护眼主题** (`eye`): 暖黄色调，适合长时间阅读

## 开发说明

- 所有页面都使用 `Layout.vue` 作为公共布局（登录/注册页除外）
- 主题切换通过 CSS 变量实现
- 响应式设计采用 Tailwind 的响应式断点
- 代码遵循 Vue 3 最佳实践

## 后续优化计划

- [ ] 添加状态管理 (Pinia)
- [ ] 完善登录/注册逻辑
- [ ] 添加更多动画效果
- [ ] 优化 SEO
- [ ] 添加单元测试
- [ ] 性能优化

## License

MIT
