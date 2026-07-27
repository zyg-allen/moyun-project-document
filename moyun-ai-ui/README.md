# 🎨 Lynx AI - 前端技术文档

<div align="center">

**基于 Vue 3 + Element Plus 的现代化AI管理平台前端**

[![Vue](https://img.shields.io/badge/Vue-3.5.13-success.svg)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-5.4.8-646CFF.svg)](https://vitejs.dev/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.8.4-409EFF.svg)](https://element-plus.org/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](../LICENSE)

</div>

---

## 📑 目录

- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [核心功能组件](#-核心功能组件)
- [开发指南](#-开发指南)
- [构建部署](#-构建部署)

---

## 🛠 技术栈

### 核心框架
- **Vue 3.5.13** - 渐进式JavaScript框架
- **Vite 5.4.8** - 下一代前端构建工具
- **Vue Router 4.2.5** - Vue官方路由
- **Axios 1.7.7** - HTTP客户端

### UI组件库
- **Element Plus 2.8.4** - Vue 3 UI组件库
- **Element Plus Icons** - 图标库
- **ECharts 5.5.0** - 数据可视化图表
- **Marked 11.1.1** - Markdown解析
- **Highlight.js 11.9.0** - 代码高亮

### 工作流编排
- **@vue-flow/core 1.33.5** - 工作流核心
- **@vue-flow/background** - 背景网格
- **@vue-flow/controls** - 控制面板
- **@vue-flow/minimap** - 小地图

---

## 📁 项目结构

```
lynx-ai-ui/
├── public/                         # 静态资源
├── src/
│   ├── api/                       # API接口
│   │   ├── agent.js              # 智能体API
│   │   ├── chat.js               # 对话API
│   │   ├── knowledge.js          # 知识库API
│   │   ├── workflow.js           # 工作流API
│   │   ├── datasource.js         # 数据源API
│   │   └── request.js            # Axios封装
│   │
│   ├── components/                # 组件目录
│   │   ├── Dashboard.vue                 # 数据大屏
│   │   ├── AgentManage.vue              # 智能体管理
│   │   ├── ChatWindow.vue               # 对话窗口
│   │   ├── KnowledgeLibraryManage.vue   # 知识库管理
│   │   ├── WorkflowManage.vue           # 工作流管理
│   │   ├── IntelligentQuery.vue         # AI数据分析 ⭐
│   │   ├── DataSourceManage.vue         # 数据源管理
│   │   ├── TokenUsageStats.vue          # Token统计
│   │   └── ...
│   │
│   ├── styles/                    # 样式文件
│   ├── utils/                     # 工具函数
│   ├── App.vue                    # 根组件
│   └── main.js                    # 入口文件
│
├── .env.development               # 开发环境配置
├── .env.production                # 生产环境配置
├── vite.config.js                 # Vite配置
└── package.json                   # 依赖配置
```

---

## 🎯 核心功能组件

### 1. 智慧大厅（ChatWindow.vue）
- 智能体选择和快速切换
- 流式输出（打字机效果）
- Markdown + 代码高亮
- 多会话管理
- 工具调用可视化

### 2. 智能体管理（AgentManage.vue）
- 智能体CRUD操作
- 模型配置
- Prompt工程
- 工具和知识库绑定
- 在线测试

### 3. 知识库管理（KnowledgeLibraryManage.vue）
- 文档上传（PDF/DOCX/MD/TXT）
- 批量处理
- 进度显示
- 检索测试

### 4. 工作流编辑器（WorkflowManage.vue）
- 拖拽式节点编排
- 7种节点类型
- 连线管理
- 配置面板
- 执行测试

### 5. AI数据分析（IntelligentQuery.vue）⭐
- 数据源选择（MySQL/Elasticsearch）
- 连接状态检测
- 自然语言输入
- 查询建议
- 结果展示（表格+AI分析+SQL/DSL）
- 历史记录

### 6. 数据源管理（DataSourceManage.vue）
- 数据源CRUD
- 连接测试
- 密码加密显示

### 7. Token统计（TokenUsageStats.vue）
- 今日/本月统计
- Token趋势图
- 请求类型分布
- 详细记录

### 8. 数据大屏（Dashboard.vue）
- 核心指标展示
- Token使用趋势
- 工作流执行排行
- 智能体调用排行

---

## 🚀 开发指南

### 1. 环境准备

```bash
# 安装Node.js 18+
node -v

# 安装pnpm（推荐）
npm install -g pnpm
```

### 2. 安装依赖

```bash
cd lynx-ai-ui
npm install
# 或使用pnpm
pnpm install
```

### 3. 开发运行

```bash
# 启动开发服务器
npm run dev

# 访问：http://localhost:5173
```

### 4. 配置后端API

编辑 `.env.development` 文件：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

---

## 📦 构建部署

### 1. 构建生产版本

```bash
npm run build
# 构建产物在 dist/ 目录
```

### 2. 预览构建结果

```bash
npm run preview
```

### 3. 部署到服务器

```bash
# 方式1：直接部署dist目录
scp -r dist/* user@server:/var/www/html/

# 方式2：使用Nginx
# 将dist目录内容复制到Nginx的html目录
```

### 4. Nginx配置示例

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    
    root /var/www/html;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 🔧 常见问题

### 1. 跨域问题

在 `vite.config.js` 中配置代理：

```javascript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### 2. 构建后白屏

检查 `vite.config.js` 中的 `base` 配置：

```javascript
export default defineConfig({
  base: '/',  // 根路径部署
})
```

### 3. 路由404

配置Nginx的 `try_files`：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

---

## 🔗 相关链接

- [主文档](../README.md)
- [后端文档](../lynx-ai-parent/README.md)
- [Vue 3文档](https://vuejs.org/)
- [Element Plus文档](https://element-plus.org/)
- [Vite文档](https://vitejs.dev/)

---

<div align="center">

**🎨 用心设计，打造极致用户体验！**

Made with ❤️ by Lynx AI Team

</div>
