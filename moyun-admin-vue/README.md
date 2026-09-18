# 旭林知行 - 后台管理系统

**项目版本**: v11.98
**最后更新**: 2026-09-17

---

## 一、项目简介

旭林知行后台管理系统是一个基于 **RuoYi-Vue** 的内容社区 + AI 平台管理后台，为"旭林知行"前台门户、记账 App 提供完整的后台管理能力：内容审核、AI 统一网关治理（场景/模型/Agent/执行日志）、支付与收入管理、记账运营、用户成长等。

基于 Vue 3 + Element Plus 构建，采用前后端分离架构，支持灵活的权限管理。

---

## 二、技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.0 | 前端框架 |
| Vite | 5.0.4 | 构建工具 |
| Element Plus | 2.4.3 | UI 组件库 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.2.5 | 路由管理 |
| axios | 0.27.2 | HTTP 请求库 |
| ECharts | 5.4.3 | 数据可视化 |
| @vueup/vue-quill | 1.2.0 | 富文本编辑器 |

---

## 三、功能模块

### 3.1 CMS 内容与业务管理 (views/cms/)

| 模块 | 页面 | 说明 |
|------|------|------|
| **仪表板** | `cms/dashboard/` | 内容/用户/交易运营总览 |
| **用户管理** | `cms/user/` | 用户列表、编辑、禁用 |
| **文章管理** | `cms/article/` | 文章列表、审核、上下架、推荐、编辑（封面上传） |
| **专栏管理** | `cms/column/` | 专栏列表、专栏内文章维护 |
| **分类/标签** | `cms/category/` / `cms/tag/` | 分类树、标签管理 |
| **评论管理** | `cms/comment/` | 评论列表、审核、删除 |
| **话题管理** | `cms/topic/` | 话题广场内容治理 |
| **审核中心** | `cms/audit-center/` | 统一审核任务（文章/话题/认证等，sys_audit_task） |
| **反馈体系** | `cms/feedback/` / `cms/feedback-center/` / `cms/help-article/` / `cms/help-category/` / `cms/help-center/` | 用户反馈、帮助中心 |
| **创作者认证** | `cms/certification/` | 创作者认证审核 |
| **导入模板** | `cms/importTemplate/` | 通用导入模板（题库导入导出） |
| **面试管理** | `cms/interview/` / `cms/voiceInterview/` | 面试题库、语音面试场次/报告管理 |
| **成长体系** | `cms/growth/` / `cms/growth-config/` | 成长事件、规则与徽章配置 |
| **运营位** | `cms/ad/` / `cms/promotion/` / `cms/friend-link/` | 广告位、推荐位、友链 |
| **通知** | `cms/notification`（system） | 站内通知发送 |
| **举报** | `cms/report/` | 举报处理 |
| **打赏** | `cms/tip/` | 打赏流水（交易管理 Tab） |
| **支付管理** | `cms/pay/` | 公共支付通道：订单/分账/提现审核/收入总览（平台×渠道） |
| **钱包** | `cms/wallet/` | 单钱包公账体系管理 |
| **VIP 会员** | `system/vip/` | 统一会员等级（端级粒度）、等级/权益/会员卡/使用记录管理 |
| **记账管理** | `cms/ledger/` | 用户维度管理（脱敏）、小程序功能配置、AI 消费归属、运营统计（模块使用/AI Token 成本/收益） |
| **提示词** | `cms/prompt/` | 写作提示词管理 |

### 3.2 AI 平台管理 (views/ai/)

| 模块 | 页面 | 说明 |
|------|------|------|
| **AI 仪表板** | `ai/dashboard/` | AI 能力使用总览 |
| **场景配置** | `ai/scene/` | ai_scene_config：场景绑定 Agent/模型/限流/Token 熔断/输出模式（统一网关核心） |
| **模型配置** | `ai/model-config/` | ai_model_config：模型接入（密钥加密存储、JSON Mode、超时参数） |
| **服务商** | `ai/provider/` | 模型服务商注册表 |
| **Agent 管理** | `ai/agent/` | Agent 人设/参数/知识库绑定 |
| **知识库** | `ai/knowledge-base/` / `ai/knowledge-library/` / `ai/knowledge-center/` | 知识库/知识库分组/知识中心（RAG） |
| **执行日志** | `ai/execute-log/` | ai_execute_log 网关执行日志（success/fail/耗时/模型） |
| **Token 用量** | `ai/token-usage/` | Token 成本统计 |
| **安全治理** | `ai/safety/` | 注入防护/成本熔断/输出过滤配置 |
| **工作流** | `ai/workflow/` | AI 工作流编排 |
| **工具** | `ai/tool/` | 工具注册（JSON Schema 参数校验） |
| **对话/其他** | `ai/chat/` / `ai/diagram/` / `ai/query/` / `ai/datasource/` / `ai/dictionary/` | 对话测试、图表分析、Text-to-SQL、数据源、字典 |

### 3.3 系统管理 (views/system/)

用户/角色/菜单/部门/岗位/字典/参数配置（sys_config 热开关：AI 全局开关等）/通知公告/文件管理（sys_file）/审核任务/敏感词/仪表板/个人中心。

### 3.4 系统监控 (views/monitor/)

在线用户、定时任务、Druid 数据监控、服务监控、缓存监控、操作日志、登录日志。

### 3.5 系统工具 (views/tool/)

代码生成、接口文档（Swagger）。

### 3.6 门户数据管理 (views/portal/)

读书管理（书籍/书单/摘录）。

---

## 四、项目结构

```
moyun-admin-vue/
├── src/
│   ├── api/                  # API 接口
│   │   ├── app/             # 应用管理
│   │   ├── cms/             # CMS 内容管理
│   │   ├── monitor/         # 系统监控
│   │   ├── system/          # 系统管理
│   │   ├── tool/            # 系统工具
│   │   ├── login.js         # 登录接口
│   │   └── menu.js          # 菜单接口
│   ├── assets/               # 静态资源
│   │   ├── icons/
│   │   ├── images/
│   │   ├── logo/
│   │   └── styles/
│   ├── components/           # 公共组件
│   │   ├── Pagination/      # 分页组件
│   │   ├── Editor/          # 富文本编辑器
│   │   ├── FileUpload/      # 文件上传
│   │   ├── ImageUpload/     # 图片上传
│   │   └── SvgIcon/
│   ├── directive/           # 自定义指令
│   ├── layout/              # 布局组件
│   │   ├── components/
│   │   │   ├── Sidebar/
│   │   │   ├── Navbar/
│   │   │   └── TagsView/
│   │   └── index.vue
│   ├── plugins/             # 插件
│   ├── router/             # 路由配置
│   ├── store/              # Pinia 状态
│   ├── utils/              # 工具类
│   │   ├── request.js     # 请求封装
│   │   ├── auth.js        # 认证工具
│   │   └── ...
│   ├── views/             # 页面视图
│   │   ├── ai/            # AI 平台管理（场景/模型/Agent/知识库/执行日志…）
│   │   ├── cms/           # 内容与业务管理（文章/审核/支付/记账/VIP…）
│   │   ├── monitor/       # 系统监控
│   │   ├── portal/       # 门户数据管理（读书）
│   │   ├── system/       # 系统管理
│   │   └── tool/          # 系统工具
│   ├── App.vue
│   ├── main.js
│   ├── permission.js      # 权限控制
│   └── settings.js        # 系统配置
├── public/
├── vite/
│   └── plugins/
├── .env.development
├── .env.production
├── vite.config.js
└── README.md
```

---

## 五、快速开始

### 5.1 安装依赖

```bash
cd moyun-admin-vue
pnpm install
# 或使用 npm
npm install
```

### 5.2 开发模式

```bash
npm run dev -- --host 0.0.0.0 --port 80
```

访问地址: http://localhost:80

**默认后台账号**: admin / admin123

### 5.3 生产构建

```bash
# 生产环境
npm run build:prod

# 测试环境
npm run build:stage
```

---

## 六、API 代理配置

开发环境通过 Vite 代理连接后端：

```javascript
// vite.config.js
server: {
  proxy: {
    '/dev-api/': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

确保后端服务在 **8080 端口**运行。

---

## 七、开发规范

### 7.1 API 调用规范

```javascript
// 定义接口 (src/api/cms/article.js)
import request from '@/utils/request'

export function listArticle(query) {
  return request({
    url: '/cms/article/list',
    method: 'get',
    params: query
  })
}

export function getArticle(id) {
  return request({
    url: `/cms/article/${id}`,
    method: 'get'
  })
}

// 页面中调用
import { listArticle } from '@/api/cms/article'

const handleQuery = () => {
  listArticle(queryParams.value).then(response => {
    articleList.value = response.rows
    total.value = response.total
  })
}
```

### 7.2 权限控制

```vue
<template>
  <!-- 按钮权限 -->
  <el-button v-hasPermi="['cms:article:add']">新增</el-button>
  
  <!-- 角色权限 -->
  <el-button v-hasRole="['admin']">删除</el-button>
</template>
```

### 7.3 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 文件夹 | kebab-case | `article-management` |
| 组件 | PascalCase 或 kebab-case | `ArticleList.vue` |
| API 文件 | kebab-case | `article.js` |
| 变量 | camelCase | `articleList` |

---

## 八、相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目总览 | [../README.md](../README.md) | 项目整体介绍 |
| 项目文档索引 | [../docs/README.md](../docs/README.md) | 全量文档索引（01-09 分类） |
| 开发文档 | [../docs/02-开发指南/开发指南.md](../docs/02-开发指南/开发指南.md) | 开发规范与最佳实践 |
| 部署文档 | [../docs/03-部署运维/部署指南.md](../docs/03-部署运维/部署指南.md) | 部署与验证指南 |
| 技术架构 | [../docs/01-架构设计/技术架构.md](../docs/01-架构设计/技术架构.md) | 系统技术架构 |

---

## 九、参考项目

- [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue)

---

**项目维护者**: 旭林知行开发团队
**最后更新**: 2026-09-17