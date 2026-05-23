
# 墨韵智库 - 后台管理系统

<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">Moyun Admin</h1>
<h4 align="center">基于 RuoYi-Vue + Flowable 的墨韵智库后台管理平台</h4>

<p align="center">
  <img src="https://img.shields.io/badge/vue-3.4.0-brightgreen.svg" alt="vue">
  <img src="https://img.shields.io/badge/element--plus-2.4.3-brightgreen.svg" alt="element-plus">
  <img src="https://img.shields.io/badge/vite-5.0.4-brightgreen.svg" alt="vite">
  <img src="https://img.shields.io/badge/pinia-2.1.7-brightgreen.svg" alt="pinia">
</p>

---

## 📋 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [功能模块](#功能模块)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [开发规范](#开发规范)
- [部署发布](#部署发布)
- [常见问题](#常见问题)
- [开发日志](#开发日志)

---

## 项目简介

墨韵智库后台管理系统是一个基于 **RuoYi-Vue** 和 **Flowable 工作流引擎** 的内容社区管理平台，为"墨韵·智库"前台应用提供完整的后台管理能力。

### 核心特性

✅ **完善的权限管理**: 用户、角色、菜单、数据权限  
✅ **强大的工作流**: 基于 Flowable 的流程引擎，支持内容审核  
✅ **内容管理**: 文章审核、分类管理、标签管理  
✅ **社区运营**: 成长体系配置、钱包管理、数据统计  
✅ **系统监控**: 日志管理、服务监控、在线用户  
✅ **代码生成**: 一键生成前后端代码，提高开发效率  
✅ **富文本编辑**: 集成 Quill 编辑器，支持 Markdown  
✅ **流程设计**: BPMN 流程设计器，可视化配置工作流  

### 应用场景

- 📝 **内容审核**: 文章发布审核、评论管理
- 👥 **用户管理**: 用户信息管理、权限分配
- 📊 **数据分析**: 内容统计、用户行为分析
- 💰 **运营管理**: 积分配置、勋章管理、钱包管理
- 🔧 **系统配置**: 字典管理、参数配置、定时任务

---

## 技术栈

### 核心框架

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.0 | 渐进式 JavaScript 框架 |
| Vite | 5.0.4 | 下一代前端构建工具 |
| Element Plus | 2.4.3 | Vue 3 组件库 |
| Pinia | 2.1.7 | Vue 状态管理 |
| Vue Router | 4.2.5 | Vue 官方路由 |

### UI 组件

| 技术 | 版本 | 说明 |
|------|------|------|
| @element-plus/icons-vue | 2.3.1 | Element Plus 图标库 |
| @vueup/vue-quill | 1.2.0 | 富文本编辑器 |
| vue-cropper | 1.1.1 | 图片裁剪组件 |
| vuedraggable | 4.1.0 | 拖拽排序组件 |

### 工作流引擎

| 技术 | 版本 | 说明 |
|------|------|------|
| bpmn-js | 11.1.0 | BPMN 2.0 流程图渲染 |
| bpmn-js-bpmnlint | 0.15.0 | BPMN 规则校验 |
| bpmn-js-color-picker | 0.5.0 | BPMN 颜色选择器 |

### 工具库

| 技术 | 版本 | 说明 |
|------|------|------|
| axios | 0.27.2 | HTTP 请求库 |
| echarts | 5.4.3 | 数据可视化图表 |
| highlight.js | 11.11.1 | 代码高亮 |
| js-cookie | 3.0.5 | Cookie 操作 |
| jsencrypt | 3.3.2 | RSA 加密 |
| nprogress | 0.2.0 | 进度条 |

### 开发工具

| 技术 | 版本 | 说明 |
|------|------|------|
| sass | 1.69.5 | CSS 预处理器 |
| unplugin-auto-import | 0.17.1 | 自动导入 API |
| vite-plugin-compression | 0.5.1 | Gzip 压缩插件 |
| vite-plugin-svg-icons | 2.0.1 | SVG 图标插件 |

---

## 功能模块

### 1. 系统管理 (system)

#### 1.1 用户管理
- 用户列表查询、新增、编辑、删除
- 用户角色分配
- 用户状态管理（启用/禁用）
- 密码重置

#### 1.2 角色管理
- 角色列表管理
- 角色权限配置
- 数据权限范围设置
- 角色用户关联

#### 1.3 菜单管理
- 菜单树形结构展示
- 菜单新增、编辑、删除
- 菜单权限标识配置
- 菜单图标选择

#### 1.4 部门管理
- 部门树形结构
- 部门负责人设置
- 部门排序

#### 1.5 岗位管理
- 岗位列表管理
- 岗位编码配置

#### 1.6 字典管理
- 字典类型管理
- 字典数据管理
- 字典缓存刷新

#### 1.7 参数设置
- 系统参数配置
- 参数键值对管理

#### 1.8 通知公告
- 公告发布
- 公告类型管理
- 公告状态控制

---

### 2. 社区管理 (community)

#### 2.1 文章管理
- 文章列表查询（支持多条件筛选）
- 文章详情查看
- 文章审核（通过/驳回）
- 文章上下架
- 文章推荐配置
- 文章删除

#### 2.2 分类管理
- 栏目分类树形结构
- 分类新增、编辑、删除
- 分类排序
- 分类状态管理

#### 2.3 审核管理
- 待审核文章列表
- 审核历史记录
- 批量审核
- 审核意见填写

---

### 3. 应用管理 (app)

#### 3.1 班级管理
- 班级列表管理
- 班级信息配置
- 班级成员管理

#### 3.2 报名管理
- 报名列表查询
- 报名审核
- 报名统计

---

### 4. 工作流管理 (flowable)

#### 4.1 流程定义
- 流程模型列表
- BPMN 流程设计器
- 流程部署
- 流程版本管理

#### 4.2 流程实例
- 运行中的流程实例
- 已结束的流程实例
- 流程实例详情查看
- 流程实例终止

#### 4.3 待办任务
- 我的待办列表
- 任务审批（通过/驳回）
- 任务转办
- 任务委派

#### 4.4 已办任务
- 我的已办列表
- 审批历史查看
- 流程轨迹图

---

### 5. 系统监控 (monitor)

#### 5.1 在线用户
- 在线用户列表
- 强退用户
- 会话管理

#### 5.2 定时任务
- 任务列表管理
- 任务执行状态
- Cron 表达式配置
- 任务日志查看

#### 5.3 数据监控
- Druid 数据源监控
- SQL 性能分析
- 连接池状态

#### 5.4 服务监控
- CPU 使用率
- 内存使用情况
- JVM 信息
- 磁盘空间

#### 5.5 缓存监控
- Redis 信息查看
- 缓存命中率
- Key 数量统计

#### 5.6 操作日志
- 操作记录查询
- 操作详情查看
- 日志导出

#### 5.7 登录日志
- 登录记录查询
- 登录地点显示
- 登录状态统计

---

### 6. 系统工具 (tool)

#### 6.1 代码生成
- 数据库表导入
- 代码预览
- 一键生成前后端代码
- 代码下载

#### 6.2 表单构建
- 拖拽式表单设计
- 表单预览
- 表单代码生成

---

## 项目结构

```
moyun-admin-vue/
├── public/                    # 静态资源
│   └── favicon.ico           # 网站图标
├── src/                      # 源代码
│   ├── api/                  # API 接口
│   │   ├── app/              # 应用管理接口
│   │   │   ├── class.js      # 班级管理
│   │   │   ├── custom.js     # 自定义
│   │   │   └── register.js   # 报名管理
│   │   ├── community/        # 社区管理接口
│   │   │   ├── article.js    # 文章管理
│   │   │   └── audit.js      # 审核管理
│   │   ├── flowable/         # 工作流接口
│   │   │   ├── definition.js # 流程定义
│   │   │   ├── instance.js   # 流程实例
│   │   │   ├── task.js       # 任务管理
│   │   │   └── ...
│   │   ├── system/           # 系统管理接口
│   │   │   ├── user.js       # 用户管理
│   │   │   ├── role.js       # 角色管理
│   │   │   ├── menu.js       # 菜单管理
│   │   │   └── ...
│   │   ├── monitor/          # 监控管理接口
│   │   ├── tool/             # 工具接口
│   │   ├── login.js          # 登录接口
│   │   └── menu.js           # 菜单接口
│   │
│   ├── assets/               # 静态资源
│   │   ├── icons/            # SVG 图标
│   │   ├── images/           # 图片资源
│   │   ├── logo/             # Logo
│   │   └── styles/           # 全局样式
│   │
│   ├── components/           # 公共组件 (22个)
│   │   ├── Breadcrumb/       # 面包屑导航
│   │   ├── Editor/           # 富文本编辑器
│   │   ├── FileUpload/       # 文件上传
│   │   ├── ImageUpload/      # 图片上传
│   │   ├── Pagination/       # 分页组件
│   │   ├── Process/          # 流程相关组件
│   │   ├── SvgIcon/          # SVG 图标
│   │   └── ...
│   │
│   ├── directive/            # 自定义指令
│   │   ├── permission/       # 权限指令
│   │   └── ...
│   │
│   ├── layout/               # 布局组件
│   │   ├── components/       # 布局子组件
│   │   │   ├── Sidebar/      # 侧边栏
│   │   │   ├── Navbar/       # 顶部导航
│   │   │   ├── TagsView/     # 标签页
│   │   │   └── ...
│   │   └── index.vue         # 布局入口
│   │
│   ├── plugins/              # 插件
│   │   ├── auth.js           # 权限插件
│   │   ├── cache.js          # 缓存插件
│   │   ├── modal.js          # 弹窗插件
│   │   └── ...
│   │
│   ├── router/               # 路由配置
│   │   └── index.js
│   │
│   ├── store/                # Pinia 状态管理
│   │   ├── modules/          # 模块化 Store
│   │   │   ├── app.js        # 应用配置
│   │   │   ├── user.js       # 用户信息
│   │   │   ├── permission.js # 权限管理
│   │   │   └── ...
│   │   └── index.js
│   │
│   ├── utils/                # 工具类 (15个)
│   │   ├── request.js        # 请求封装
│   │   ├── auth.js           # 认证工具
│   │   ├── dict.js           # 字典工具
│   │   ├── validate.js       # 验证工具
│   │   └── ...
│   │
│   ├── views/                # 页面视图
│   │   ├── app/              # 应用管理页面
│   │   ├── community/        # 社区管理页面
│   │   ├── flowable/         # 工作流页面
│   │   ├── system/           # 系统管理页面
│   │   ├── monitor/          # 监控管理页面
│   │   ├── tool/             # 工具页面
│   │   ├── login.vue         # 登录页
│   │   ├── register.vue      # 注册页
│   │   └── index.vue         # 首页
│   │
│   ├── App.vue               # 根组件
│   ├── main.js               # 入口文件
│   ├── permission.js         # 权限控制
│   └── settings.js           # 系统配置
│
├── vite/                     # Vite 插件配置
│   └── plugins/              # 插件集合
│       ├── auto-import.js    # 自动导入
│       ├── compression.js    # 压缩插件
│       ├── svg-icon.js       # SVG 图标
│       └── ...
│
├── .env.development          # 开发环境变量
├── .env.production           # 生产环境变量
├── .env.staging              # 测试环境变量
├── vite.config.js            # Vite 配置
├── package.json              # 依赖配置
└── README.md                 # 项目说明
```

---

## 快速开始

### 前置要求

确保已安装：
- Node.js >= 16.x
- pnpm >= 7.x (推荐) 或 npm >= 8.x

### 安装依赖

```bash
cd moyun-admin-vue
pnpm install
```

或使用 npm：
```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问地址: `http://localhost:80`

### 构建生产版本

```bash
# 生产环境构建
npm run build:prod

# 测试环境构建
npm run build:stage
```

输出目录: `dist/`

### 后端接口配置

当前配置的代理：
- `/dev-api/*` → `http://localhost:8080/*`

**确保后端服务在 8080 端口运行。**

如需修改后端地址，编辑 `.env.development`:
```bash
VITE_APP_BASE_API = '/dev-api'
```

并在 `vite.config.js` 中修改 proxy 配置。

---

## 开发规范

### 目录命名规范

- 文件夹: kebab-case (如 `user-management`)
- 文件: PascalCase (如 `UserList.vue`) 或 kebab-case (如 `user-list.vue`)

### 组件命名规范

```vue
<!-- ✅ 推荐 -->
<template>
  <div class="user-list">
    <!-- 内容 -->
  </div>
</template>

<script setup name="UserList">
// 组件逻辑
</script>
```

### API 调用规范

```javascript
// 定义接口 (src/api/system/user.js)
import request from '@/utils/request'

export function listUser(query) {
  return request({
    url: '/system/user/list',
    method: 'get',
    params: query
  })
}

export function getUser(userId) {
  return request({
    url: `/system/user/${userId}`,
    method: 'get'
  })
}

// 页面中调用
import { listUser } from '@/api/system/user'

const handleQuery = () => {
  listUser(queryParams.value).then(response => {
    userList.value = response.rows
    total.value = response.total
  })
}
```

### 权限控制

```vue
<template>
  <!-- 按钮权限控制 -->
  <el-button 
    v-hasPermi="['system:user:add']"
    @click="handleAdd"
  >
    新增
  </el-button>
  
  <!-- 角色权限控制 -->
  <el-button 
    v-hasRole="['admin']"
    @click="handleDelete"
  >
    删除
  </el-button>
</template>
```

### 路由配置

```javascript
// 动态路由由后端返回，前端只需配置常量路由
{
  path: '/community',
  component: Layout,
  redirect: '/community/article',
  children: [
    {
      path: 'article',
      component: () => import('@/views/community/article/index'),
      name: 'Article',
      meta: { 
        title: '文章管理', 
        icon: 'documentation' 
      }
    }
  ]
}
```

---

## 部署发布

### Nginx 配置

```nginx
server {
    listen       80;
    server_name  your-domain.com;
    
    root /usr/share/nginx/html;
    index index.html;
    
    # 前端路由 history 模式
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 后端接口代理
    location /prod-api/ {
        proxy_pass http://backend-server:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### Docker 部署

```dockerfile
FROM node:16-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build:prod

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 常见问题

### 1. 依赖安装失败

**解决方案**:
```bash
# 清除缓存
pnpm store prune

# 删除 node_modules 和 lock 文件
rm -rf node_modules pnpm-lock.yaml

# 重新安装
pnpm install
```

### 2. 端口被占用

**解决方案**: 修改 `vite.config.js` 中的端口配置
```javascript
server: {
  port: 8080,  // 改为其他端口
}
```

### 3. 登录后白屏

**检查项**:
- ✅ 后端服务是否正常运行
- ✅ 浏览器控制台是否有错误
- ✅ 网络请求是否成功
- ✅ Token 是否正确保存

### 4. 菜单不显示

**检查项**:
- ✅ 用户是否有对应角色
- ✅ 角色是否有菜单权限
- ✅ 菜单状态是否为启用
- ✅ 清除浏览器缓存

### 5. 工作流设计器无法加载

**检查项**:
- ✅ bpmn-js 依赖是否正确安装
- ✅ 浏览器控制台是否有错误
- ✅ 流程定义 XML 格式是否正确

---

## 开发日志

### 📅 2026-04-20 后台管理文档初始化

#### 工作内容

1. **创建完整的 README.md**
   - 项目简介与技术栈说明
   - 详细的功能模块介绍（6大模块）
   - 完整的项目结构说明
   - 快速开始指南
   - 开发规范与最佳实践
   - 部署发布方案
   - 常见问题解答

2. **功能模块梳理**
   - 系统管理 (8个子模块)
   - 社区管理 (3个子模块)
   - 应用管理 (2个子模块)
   - 工作流管理 (4个子模块)
   - 系统监控 (7个子模块)
   - 系统工具 (2个子模块)

3. **技术栈整理**
   - 核心框架: Vue 3.4.0 + Vite 5.0.4
   - UI 组件: Element Plus 2.4.3
   - 工作流: bpmn-js 11.1.0
   - 状态管理: Pinia 2.1.7
   - 其他工具库完整清单

#### 项目现状

**已完成功能**:
- ✅ 用户权限管理体系
- ✅ 工作流引擎集成
- ✅ 内容审核管理
- ✅ 系统监控功能
- ✅ 代码生成工具
- ✅ 富文本编辑器
- ✅ BPMN 流程设计器

**技术特色**:
- 🎯 基于 RuoYi-Vue 成熟框架
- 🔄 Flowable 工作流引擎
- 📊 ECharts 数据可视化
- 🎨 Element Plus 现代化 UI
- ⚡ Vite 极速开发体验

---

## 相关文档

- [前台项目开发指南](../moyun-portal/doc/README.md)
- [主项目开发指导手册](../docs/4墨韵智库-项目开发指导手册.md)
- [后端模块化工程设计方案](../docs/8墨韵·智库 后端模块化单体工程设计方案.md)
- [业务流程说明](../docs/1墨韵智库 前后台模块对应与业务流程说明-完整流程图.md)

---

## 参考项目

- [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue)
- [RuoYi-Flowable](https://gitee.com/tony2y/RuoYi-flowable)

---

**维护人员**: AI Assistant  
**最后更新**: 2026-04-20  
**文档版本**: v1.0  
**技术支持**: 墨韵智库开发团队


