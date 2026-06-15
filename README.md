# 一纸墨 - 文学+技术社区平台

**项目版本**: v3.1
**最后更新**: 2026-06-15
**项目状态**: ✅ 第一阶段完成 | ✅ 第二阶段完成 | ⏳ 第三阶段规划中

---

## 📖 项目简介

**一纸墨**是一个面向年轻创作者、技术学习者、文学爱好者的**文学+技术双内容形态共存的社区平台**。

**品牌口号**: 在浮躁的世界，留一页纸给灵魂。

### 核心特性

- **双形态内容**: 技术流和文学派两种内容展示风格
- **独立认证体系**: 前后台用户完全独立，双 SecurityFilterChain 配置
- **多主题支持**: 日间、夜间、护眼三种主题自由切换
- **工作流引擎**: Flowable 集成，支持复杂的内容审核流程
- **响应式设计**: 完美适配各种屏幕尺寸

---

## 🏗️ 项目结构

```
moyun-project-document/
├── docs/                        # 📚 项目文档（核心文档目录）
│   ├── 01_项目概述.md           # 项目整体介绍
│   ├── 02_技术架构.md           # 技术选型、架构设计
│   ├── 03_开发指南.md           # 开发规范与最佳实践
│   ├── 04_部署指南.md           # 环境部署与验证
│   ├── 05_测试清单.md           # 功能测试用例
│   ├── 06_问题修复.md           # 问题修复记录
│   └── 07_更新日志.md           # 版本更新记录
│
├── moyun-server/               # ☕ 后端服务
│   ├── src/main/java/com/moyun/
│   ├── ARCHITECTURE.md         # 后端架构详细文档
│   └── 独立认证方案说明.md      # 前后台独立认证方案
│
├── moyun-portal/               # 🎨 用户前台
│   ├── src/
│   ├── README.md               # 前台模块文档
│   └── 网站名称设计.md          # 品牌设计文档
│
├── moyun-admin-vue/            # 🔧 后台管理
│   ├── src/
│   └── README.md               # 后台模块文档
│
├── scripts/
│   └── dev.sh                  # 开发环境启动脚本
│
└── README.md                   # 项目总览（本文档）
```

---

## 🛠️ 技术栈

### 后端服务 (moyun-server)

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.3.2 | 核心框架 |
| Java | 21 | 编程语言 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存中间件 |
| Flowable | 7.1.0 | 工作流引擎 |
| Spring Security | 6.3.1 | 安全框架 |

### 前台门户 (moyun-portal)

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.15 | 前端框架 |
| Vite | 5.0.12 | 构建工具 |
| TypeScript | 5.3.3 | 编程语言 |
| Pinia | 3.0.4 | 状态管理 |
| Tailwind CSS | 3.4.1 | CSS 框架 |

### 后台管理 (moyun-admin-vue)

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.0 | 前端框架 |
| Element Plus | 2.4.3 | UI 组件库 |
| Vite | 5.0.4 | 构建工具 |
| Pinia | 2.1.7 | 状态管理 |

---

## 🚀 快速开始

### 环境要求

| 软件 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

### 启动命令

```bash
cd moyun-project-document

# 查看帮助
./scripts/dev.sh status

# 安装前端依赖
./scripts/dev.sh install

# 启动前台 (3000端口)
./scripts/dev.sh portal

# 启动后台 (80端口)
./scripts/dev.sh admin

# 启动后端 (8080端口)
./scripts/dev.sh server

# 同时启动所有服务
./scripts/dev.sh all
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前台门户 | http://localhost:3000 |
| 后台管理 | http://localhost:80 |
| 后端API | http://localhost:8080 |
| Swagger文档 | http://localhost:8080/doc.html |

**默认后台账号**: admin / admin123

---

## 📚 文档导航

### 核心文档（docs/ 目录）

| 文档 | 说明 |
|------|------|
| [docs/01_项目概述.md](docs/01_项目概述.md) | 项目简介、结构、模块划分 |
| [docs/02_技术架构.md](docs/02_技术架构.md) | 技术选型、架构设计、模块依赖 |
| [docs/03_开发指南.md](docs/03_开发指南.md) | 环境配置、代码规范、开发流程 |
| [docs/04_部署指南.md](docs/04_部署指南.md) | 环境准备、部署步骤、问题排查 |
| [docs/05_测试清单.md](docs/05_测试清单.md) | 功能测试用例、数据一致性验证 |
| [docs/06_问题修复.md](docs/06_问题修复.md) | 问题汇总、修复记录 |
| [docs/07_更新日志.md](docs/07_更新日志.md) | 版本更新记录 |

### 子项目文档

| 模块 | 文档 | 说明 |
|------|------|------|
| 后端服务 | [moyun-server/ARCHITECTURE.md](moyun-server/ARCHITECTURE.md) | 后端详细架构、模块依赖图 |
| 后端服务 | [moyun-server/独立认证方案说明.md](moyun-server/独立认证方案说明.md) | 前后台独立认证方案 |
| 前台门户 | [moyun-portal/README.md](moyun-portal/README.md) | 前台功能、组件、API |
| 前台门户 | [moyun-portal/网站名称设计.md](moyun-portal/网站名称设计.md) | 品牌定位、口号、文案 |
| 前台门户 | [moyun-portal/API文档.md](moyun-portal/API文档.md) | 前台 API 接口文档 |
| 后台管理 | [moyun-admin-vue/README.md](moyun-admin-vue/README.md) | 后台功能、模块、开发规范 |

---

## 📊 项目阶段

| 阶段 | 名称 | 状态 | 完成时间 |
|------|------|------|----------|
| 第一阶段 | 前台基础功能 | ✅ 已完成 | 2026-05-24 |
| 第二阶段 | 后台管理系统 | ✅ 已完成 | 2026-05-27 |
| 第三阶段 | 性能与SEO优化 | ⏳ 规划中 | 待定 |
| 第四阶段 | Nuxt 3 SSR | ⏳ 规划中 | 待定 |
| 第五阶段 | 第三方集成 | ⏳ 规划中 | 待定 |
| 第六阶段 | 高级功能 | ⏳ 规划中 | 待定 |

---

## 🔗 相关链接

- **项目仓库**: https://github.com/zyg-allen/moyun-project-document
- **问题反馈**: 通过 GitHub Issues

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

**项目维护者**: 一纸墨开发团队
**最后更新**: 2026-06-15