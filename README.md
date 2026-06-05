# 墨韵·智库 - 文学+技术社区平台

**项目名称**: 墨韵·智库 (Moyun Wisdom)  
**文档版本**: v3.1  
**更新时间**: 2026-06-04  
**项目类型**: 前后端分离的社区平台  
**项目状态**: ✅ 第一阶段完成 | ✅ 第二阶段完成 | ⏳ 第三阶段规划中

---

## 📊 项目阶段进度

| 阶段 | 名称 | 状态 | 完成时间 | 说明 |
|------|------|------|----------|------|
| **第一阶段** | 前台基础功能 | ✅ 已完成 | 2026-05-24 | 用户认证、文章浏览、互动等 |
| **第二阶段** | 后台管理系统 | ✅ 已完成 | 2026-05-27 | CMS内容管理、用户管理、审核等 |
| **第三阶段** | 性能与SEO优化 | ⏳ 规划中 | 待定 | 系统配置、数据统计、SEO优化 |
| **第四阶段** | Nuxt 3 SSR | ⏳ 规划中 | 待定 | 服务端渲染、SEO效果最大化 |
| **第五阶段** | 第三方集成 | ⏳ 规划中 | 待定 | 微信登录、支付宝登录 |
| **第六阶段** | 高级功能 | ⏳ 规划中 | 待定 | 推荐算法、用户成长、钱包打赏 |

**[查看完整项目规划](./PROJECT_PLAN.md)**

---

## 🎯 项目简介

墨韵·智库是一个面向年轻创作者、技术学习者、文学爱好者的**文学+技术双内容形态共存的社区平台**。项目采用现代化技术栈，构建完整的内容生产、分发推荐、用户成长、商业化体系。

### 核心特性

- **用户前台**: 清新美观的社区门户，支持文章发布、阅读、评论、互动
- **独立认证体系**: 前后台用户完全独立，双 SecurityFilterChain 配置
- **后台管理**: 功能强大的管理系统，支持内容审核、用户管理、系统监控
- **工作流引擎**: Flowable 集成，支持复杂的业务审批流程
- **多主题支持**: 日间、夜间、护眼三种主题自由切换
- **响应式设计**: 完美适配各种屏幕尺寸
- **内容双模式**: 技术流和文学派两种内容展示风格

---

## 🛠️ 技术栈总览

### 后端服务 (moyun-server)

| 技术 | 版本 | 说明 |
|------|------|------|
| **框架** | Spring Boot 3.3.2 | 核心框架 |
| **语言** | Java 21 | 编程语言 |
| **持久层** | MyBatis-Plus 3.5.7 | ORM 框架 |
| **数据库** | MySQL 8.0+ | 关系型数据库 |
| **缓存** | Redis | 缓存中间件 |
| **工作流** | Flowable 7.1.0 | 工作流引擎 |
| **安全** | Spring Security 6.3.1 | 安全框架 |

### 前台门户 (moyun-portal)

| 技术 | 版本 | 说明 |
|------|------|------|
| **框架** | Vue 3.4.15 | 前端框架 |
| **构建** | Vite 5.0.12 | 构建工具 |
| **状态** | Pinia 3.0.4 | 状态管理 |
| **样式** | Tailwind CSS 3.4.1 | CSS 框架 |
| **语言** | TypeScript 5.3.3 | 编程语言 |

### 后台管理 (moyun-admin-vue)

| 技术 | 版本 | 说明 |
|------|------|------|
| **框架** | Vue 3.4.15 | 前端框架 |
| **UI** | Element Plus 2.5.0 | UI 组件库 |
| **构建** | Vite 5.0.12 | 构建工具 |
| **状态** | Pinia 3.0.4 | 状态管理 |

---

## 📁 项目结构

```
moyun-project-document/
├── README.md                    # 项目说明文档
├── PROJECT_PLAN.md              # 项目整体规划
├── CHANGELOG.md                 # 更新日志
├── CODE_WIKI.md                # 代码规范
│
├── docs/                        # 公共文档目录
│   ├── 开发记录.md              # 开发历程记录
│   ├── 功能测试验证清单.md      # 测试用例清单
│   ├── 部署与验证指南.md       # 部署说明文档
│   └── 问题修复清单.md          # 问题追踪记录
│
├── moyun-server/               # 后端服务
│   ├── src/main/java/com/moyun/
│   │   ├── common/             # 通用基础模块
│   │   ├── core/               # 核心框架模块
│   │   ├── util/               # 工具模块
│   │   ├── modules/            # 业务模块
│   │   │   └── portal/         # 门户系统模块
│   │   └── ext/                # 扩展模块
│   │       └── cms/             # CMS 内容管理
│   ├── src/main/resources/
│   │   ├── mapper/             # MyBatis XML
│   │   └── sql/                # 数据库脚本
│   ├── ARCHITECTURE.md         # 后端架构文档
│   └── pom.xml
│
├── moyun-portal/               # 用户前台
│   ├── src/
│   │   ├── api/                # API 接口定义
│   │   ├── components/         # Vue 组件
│   │   ├── pages/              # 页面组件
│   │   ├── stores/             # Pinia 状态
│   │   └── utils/              # 工具函数
│   ├── .trae/documents/        # 模块文档
│   │   ├── prd.md              # 产品需求文档
│   │   └── arch.md             # 架构设计文档
│   ├── API文档.md              # API 接口文档
│   └── README.md               # 模块说明
│
└── moyun-admin-vue/            # 后台管理
    ├── src/
    │   ├── api/                # API 接口定义
    │   ├── views/              # 页面组件
    │   ├── components/         # Vue 组件
    │   └── utils/              # 工具函数
    ├── doc/                    # 模块文档
    └── README.md               # 模块说明
```

---

## 🚀 快速开始

### 环境要求

- **JDK**: 21+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Redis**: 6.0+

### 后端启动

```bash
cd moyun-server

# 1. 导入数据库脚本
mysql -u root -p < src/main/resources/sql/01_moyun_init.sql
mysql -u root -p < src/main/resources/sql/02_workflow_init.sql
mysql -u root -p < src/main/resources/sql/03_portal_init.sql

# 2. 修改配置
vim src/main/resources/application-druid.yml
# 修改数据库连接信息

# 3. 启动服务
mvn spring-boot:run
```

### 前台启动

```bash
cd moyun-portal

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

### 后台启动

```bash
cd moyun-admin-vue

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

---

## 📖 文档导航

### 🚶 快速定位

| 文档类型 | 适用场景 | 文档位置 |
|---------|---------|---------|
| **项目概览** | 了解项目整体情况 | [README.md](./README.md) |
| **项目规划** | 查看开发计划和路线图 | [PROJECT_PLAN.md](./PROJECT_PLAN.md) |
| **更新日志** | 查看版本更新历史 | [CHANGELOG.md](./CHANGELOG.md) |
| **代码规范** | 了解代码编写规范 | [CODE_WIKI.md](./CODE_WIKI.md) |

### 📚 开发文档

| 文档 | 说明 | 适用人员 |
|------|------|---------|
| [开发记录.md](./docs/开发记录.md) | 详细的开发历程记录 | 所有开发者 |
| [功能测试验证清单.md](./docs/功能测试验证清单.md) | 功能测试用例清单 | 测试人员、开发者 |
| [部署与验证指南.md](./docs/部署与验证指南.md) | 环境部署和验证指南 | 运维人员、开发者 |
| [问题修复清单.md](./docs/问题修复清单.md) | 已解决问题记录 | 开发者 |

### 🎨 模块文档

| 模块 | 文档 | 说明 |
|------|------|------|
| **前台门户** | [moyun-portal/README.md](./moyun-portal/README.md) | 前台模块说明 |
| **前台门户** | [moyun-portal/API文档.md](./moyun-portal/API文档.md) | 前台 API 文档 |
| **前台门户** | [moyun-portal/.trae/documents/prd.md](./moyun-portal/.trae/documents/prd.md) | 产品需求文档 |
| **后台管理** | [moyun-admin-vue/README.md](./moyun-admin-vue/README.md) | 后台模块说明 |
| **后端服务** | [moyun-server/ARCHITECTURE.md](./moyun-server/ARCHITECTURE.md) | 后端架构文档 |

---

## 🔗 相关链接

- **项目仓库**: https://github.com/zyg-allen/moyun-project-document
- **技术文档**: 参见各模块 README.md
- **问题反馈**: 通过 GitHub Issues

---

## 📝 更新日志

详细更新记录请查看 [CHANGELOG.md](./CHANGELOG.md)

### 最新版本 (v3.1 - 2026-06-04)

**重大更新**:
- ✅ 完成 Portal 模块标准分页改造
- ✅ 完成 CMS 模块标准分页改造
- ✅ 前端分页兼容处理
- ✅ 全链路检查通过
- ✅ 详情页收藏点赞区域优化
- ✅ 文档体系重新整理

**详细记录**: [开发记录.md](./docs/开发记录.md)

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📄 许可证

本项目基于 MIT 许可证 - 参见 [LICENSE](./LICENSE) 文件

---

**项目维护者**: 墨韵·智库开发团队  
**最后更新**: 2026-06-04
