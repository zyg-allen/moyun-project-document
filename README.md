# 墨韵·智库 - 文学+技术社区平台

**项目状态**: ✅ MVP 完成 | ⏳ 真实支付通道待接入

---

## 项目简介

**墨韵·智库**是一个面向年轻创作者、技术学习者、文学爱好者的**文学+技术双内容形态共存的社区平台**。

**品牌口号**: 在浮躁的世界，留一页纸给灵魂。

### 核心特性

- **双形态内容**: 散文创作、读书空间、面试刷题、技术笔记四大模块
- **独立认证体系**: 前后台用户完全独立，双 SecurityFilterChain 配置
- **AI 能力**: 集成 LangChain4j，支持智能体、知识库、RAG、工作流
- **多主题支持**: 日间、夜间、护眼三种主题自由切换
- **响应式设计**: 适配各种屏幕尺寸

---

## 项目结构

```
moyun-project-document/
├── docs/                        # 项目文档（唯一权威文档目录）
│   ├── 01_项目介绍.md
│   ├── 02_技术架构.md
│   ├── 03_开发指南.md
│   ├── 04_部署指南.md
│   ├── 05_测试清单.md
│   ├── 08_项目优缺点与改进建议.md
│   ├── 09_开发进度.md
│   ├── 10_功能排查清单.md
│   ├── devlog.md
│   └── 墨韵·智库项目开发规范.md
│
├── moyun-server/               # 后端服务（Spring Boot 3.3 + JDK 21）
│   ├── src/main/java/com/moyun/
│   ├── ARCHITECTURE.md         # 后端架构详细文档
│   └── src/main/resources/sql/ # 数据库初始化脚本（init_v7.8.sql）
│
├── moyun-portal/               # 用户前台（Vue 3 + Vite + TailwindCSS）
│   ├── src/
│   └── README.md
│
├── moyun-admin-vue/            # 后台管理（Vue 3 + Element Plus）
│   ├── src/
│   └── README.md
│
└── README.md                   # 项目总览（本文档）
```

---

## 技术栈

### 后端服务 (moyun-server)

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.3.2 | 核心框架 |
| Java | 21 | 编程语言（LTS） |
| MyBatis-Plus | 3.5.11 | ORM 框架 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存（核心依赖） |
| Spring Security | 6.3.x | 安全框架 |
| LangChain4j | 1.0.0-beta3 | AI 能力框架 |
| Flowable | 7.1.0 | 工作流引擎 |
| MinIO | 8.5.12 | 对象存储 |
| MongoDB | — | 辅助中间件（可选） |

### 前台门户 (moyun-portal)

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | ^3.4.15 | 前端框架 |
| Vite | ^5.0.12 | 构建工具 |
| TypeScript | ~5.3.3 | 编程语言 |
| Pinia | ^3.0.4 | 状态管理 |
| Tailwind CSS | 3.4.1 | CSS 框架 |

### 后台管理 (moyun-admin-vue)

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.0 | 前端框架 |
| Element Plus | 2.4.3 | UI 组件库 |
| Vite | 5.0.4 | 构建工具 |
| Pinia | 2.1.7 | 状态管理 |

---

## 快速开始

### 环境要求

| 软件 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Maven | 3.8+ |

### 1. 数据库初始化

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS moyun DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -uroot -p moyun < moyun-server/src/main/resources/sql/init_v7.8.sql
```

### 2. 启动后端

```bash
cd moyun-server
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. 启动前端

```bash
# 前台门户（端口 5173）
cd moyun-portal
npm install && npm run dev

# 后台管理（端口 80）
cd moyun-admin-vue
npm install && npm run dev
```

### 访问地址

| 服务 | 地址 | 账号 |
|------|------|------|
| 前台门户 | http://localhost:5173 | admin / 123456 |
| 后台管理 | http://localhost:80 | admin / admin123 |
| 后端 API | http://localhost:8080 | — |
| Swagger 文档 | http://localhost:8080/doc.html | — |

> ⚠️ **默认账号仅用于开发，部署后请立即修改。**

---

## 文档导航

所有项目文档统一在 `docs/` 目录下：

| 文档 | 说明 |
|------|------|
| [01_项目介绍.md](docs/01_项目介绍.md) | 项目简介、结构、模块划分 |
| [02_技术架构.md](docs/02_技术架构.md) | 技术选型、架构设计、模块依赖 |
| [03_开发指南.md](docs/03_开发指南.md) | 环境配置、代码规范、开发流程 |
| [04_部署指南.md](docs/04_部署指南.md) | 环境准备、部署步骤、问题排查 |
| [05_测试清单.md](docs/05_测试清单.md) | 功能测试用例、数据一致性验证 |
| [devlog.md](docs/devlog.md) | 开发日志 |

### 子项目文档

| 模块 | 文档 | 说明 |
|------|------|------|
| 后端服务 | [moyun-server/ARCHITECTURE.md](moyun-server/ARCHITECTURE.md) | 后端详细架构、模块依赖图 |
| 前台门户 | [moyun-portal/README.md](moyun-portal/README.md) | 前台功能、组件、API |
| 后台管理 | [moyun-admin-vue/README.md](moyun-admin-vue/README.md) | 后台功能、模块、开发规范 |

---

## 相关链接

- **项目仓库**: https://github.com/zyg-allen/moyun-project-document
- **问题反馈**: 通过 GitHub Issues

---

**项目维护者**: 墨韵开发团队
