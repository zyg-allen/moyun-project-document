# 墨韵·智库 - 后台管理系统

**项目版本**: v3.1
**最后更新**: 2026-06-15

---

## 一、项目简介

墨韵·智库后台管理系统是一个基于 **RuoYi-Vue** 和 **Flowable 工作流引擎** 的内容社区管理平台，为"一纸墨"前台应用提供完整的后台管理能力。

基于 Vue 3 + Element Plus 构建，采用前后端分离架构，支持灵活的权限管理和工作流配置。

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
| bpmn-js | 11.1.0 | BPMN 流程设计器 |
| @vueup/vue-quill | 1.2.0 | 富文本编辑器 |

---

## 三、功能模块

### 3.1 CMS 内容管理 (views/cms/)

| 模块 | 页面 | 说明 |
|------|------|------|
| **用户管理** | `cms/user/index.vue` | 用户列表、编辑、禁用、删除 |
| **文章管理** | `cms/article/index.vue` | 文章列表、审核、上下架、推荐 |
| **文章编辑** | `cms/article/edit.vue` | 文章创建、编辑、封面上传 |
| **分类管理** | `cms/category/index.vue` | 分类树形结构、增删改 |
| **标签管理** | `cms/tag/index.vue` | 标签列表、增删改 |
| **评论管理** | `cms/comment/index.vue` | 评论列表、审核、删除 |
| **通知管理** | `cms/notification/index.vue` | 通知列表、发送通知 |
| **友情链接** | `cms/friend-link/index.vue` | 友链列表、增删改 |

### 3.2 系统管理 (views/system/)

| 模块 | 页面 | 说明 |
|------|------|------|
| **用户管理** | `system/user/index.vue` | 管理员列表、角色分配 |
| **角色管理** | `system/role/index.vue` | 角色权限配置 |
| **菜单管理** | `system/menu/index.vue` | 菜单树形配置 |
| **部门管理** | `system/dept/index.vue` | 组织架构管理 |
| **岗位管理** | `system/post/index.vue` | 岗位配置 |
| **字典管理** | `system/dict/` | 字典类型和数据管理 |
| **参数管理** | `system/config/index.vue` | 系统参数配置 |
| **通知公告** | `system/notice/index.vue` | 公告发布管理 |
| **文件管理** | `system/file/index.vue` | 文件列表管理 |
| **个人中心** | `system/user/profile/` | 个人信息、密码重置 |

### 3.3 工作流管理 (views/flowable/)

| 模块 | 页面 | 说明 |
|------|------|------|
| **流程定义** | `flowable/definition/` | 流程模型、BPMN 设计器 |
| **流程实例** | `flowable/task/` | 运行实例、已办任务 |
| **待办任务** | `flowable/task/todo/` | 我的待办、审批 |
| **表单管理** | `flowable/task/form/` | 表单配置 |
| **表达式** | `flowable/expression/` | 流程表达式管理 |
| **监听器** | `flowable/listener/` | 流程监听器 |

### 3.4 系统监控 (views/monitor/)

| 模块 | 页面 | 说明 |
|------|------|------|
| **在线用户** | `monitor/online/index.vue` | 会话管理、强退 |
| **定时任务** | `monitor/job/` | 任务配置、Cron 表达式 |
| **数据监控** | `monitor/druid/` | Druid 连接池监控 |
| **服务监控** | `monitor/server/` | CPU、内存、JVM |
| **缓存监控** | `monitor/cache/` | Redis 缓存状态 |
| **操作日志** | `monitor/operlog/` | 操作记录查询 |
| **登录日志** | `monitor/logininfor/` | 登录记录查询 |

### 3.5 系统工具 (views/tool/)

| 模块 | 页面 | 说明 |
|------|------|------|
| **代码生成** | `tool/gen/` | 数据库表生成代码 |
| **接口文档** | `tool/swagger/` | Swagger API 文档 |

### 3.6 读书管理 (views/portal/)

| 模块 | 页面 | 说明 |
|------|------|------|
| **读书管理** | `portal/book/index.vue` | 读书数据管理 |
| **书单管理** | `portal/bookList/index.vue` | 书单列表 |
| **摘录管理** | `portal/bookQuote/index.vue` | 读书摘录 |

### 3.7 应用管理 (views/app/)

| 模块 | 页面 | 说明 |
|------|------|------|
| **班级管理** | `app/class/index.vue` | 班级信息管理 |
| **报名管理** | `app/register/index.vue` | 报名记录 |

---

## 四、项目结构

```
moyun-admin-vue/
├── src/
│   ├── api/                  # API 接口
│   │   ├── app/             # 应用管理
│   │   ├── cms/             # CMS 内容管理
│   │   ├── flowable/        # 工作流
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
│   │   ├── Process/         # 流程相关
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
│   │   ├── cms/
│   │   ├── system/
│   │   ├── flowable/
│   │   ├── monitor/
│   │   ├── tool/
│   │   └── ...
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
| 开发文档 | [../docs/03_开发指南.md](../docs/03_开发指南.md) | 开发规范与最佳实践 |
| 部署文档 | [../docs/04_部署指南.md](../docs/04_部署指南.md) | 部署与验证指南 |
| 后端架构 | [../moyun-server/ARCHITECTURE.md](../moyun-server/ARCHITECTURE.md) | 后端详细架构 |
| 认证方案 | [../moyun-server/独立认证方案说明.md](../moyun-server/独立认证方案说明.md) | 前后台独立认证说明 |

---

## 九、参考项目

- [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue)
- [RuoYi-Flowable](https://gitee.com/tony2y/RuoYi-flowable)

---

**项目维护者**: 墨韵·智库开发团队
**最后更新**: 2026-06-15