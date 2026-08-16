# 墨韵·智库 — AI 驱动的个人成长平台

**项目版本**：v9.5（main-dev-article × moyun-dev-kouzi 合并版，后续开发基线）  
**最后更新**：2026-08-16  
**项目状态**：✅ v9.5 分支合并完成（三端编译通过） | ⏳ 数据填充与商业化待接入

---

## 项目简介

**墨韵·智库**是一个以优质内容优先吸引流量、再驱动用户成长的平台——游客首页即可浏览丰富的文章信息流（精选/热门/分类/作者榜/读书与面试导流），登录后解锁学习、刷题、面试、阅读、写作整合的成长时间线，并通过付费阅读/VIP/打赏实现内容变现。

**品牌口号**：让成长有迹可循。

**产品策略（v9.5 确立）**：内容先行引流 → 体验留存 → 优质内容促进消费。

### 核心差异化

- **内容型首页**：游客可见文章轮播/精选/热门/分类导航/作者榜/友链，SEO 友好，第一屏即有内容留存
- **AI 模拟面试**：基于 LangChain4j 的多轮对话式面试，AI 智能评分与弱项分析
- **OJ 在线判题**：Docker 沙箱 + 异步队列，支持多语言的真实判题系统
- **成长时间线**：统一事件追踪，学习/面试/阅读/写作全量记录，可视化回溯

### 保留模块

| 模块 | 定位 | 说明 |
|------|------|------|
| 文章系统 | **核心（引流）** | 原创/转载/专栏/付费阅读/版本管理，首页信息流主体 |
| 面试指南 | **核心** | 题库 + OJ判题 + 面经复盘 + AI模拟面试 |
| 学习工具 | **核心** | 刷题日历 + 知识图谱 + 排行榜 + 错题本 |
| 成长系统 | **核心** | 成长规则 + 徽章 + 时间线 + 等级 |
| 话题讨论 | **互动** | 话题广场 + 观点/评论（登录即可发起） |
| 读书空间 | **内容** | 书籍/书单/金句/书架（版权待处理） |
| AI 模块 | **赋能** | 知识库RAG + 图表分析 + 工作流 + Agent |
| 商业化 | **变现** | 付费订单 + 打赏流水（交易管理Tab）+ VIP + 钱包 + 广告位 |

### 已移除模块（保持删除，勿回引）

| 模块 | 原因 |
|------|------|
| PK 对战 | 业务定义不清晰，v9.0 彻底删除（代码+表+菜单） |
| 圈子社交 | 无前端实现，v9.0 彻底删除（代码+表+菜单） |

---

## 技术栈

### 后端 (moyun-server)

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.2 | 核心框架 |
| Java | 21 | 编程语言 |
| MyBatis-Plus | 3.5.11 | ORM |
| Spring Security | 6.3.1 | 双FilterChain认证 |
| LangChain4j | 1.0.0-beta3 | AI能力（面试/RAG/工作流） |
| Redis | 6.0+ | 缓存/限流/异步队列 |
| Docker | — | OJ判题沙箱 |
| MinIO | 8.5.12 | 对象存储 |

### 前端门户 (moyun-portal)

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4 | Composition API + script setup |
| Vite | 5.0 | 构建工具 |
| TypeScript | 5.3 | 类型安全 |
| Pinia | 3.0 | 状态管理 |
| Tailwind CSS | — | 原子化CSS + 主题变量 |

### 管理后台 (moyun-admin-vue)

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4 | 前端框架 |
| Element Plus | 2.4.3 | 企业级UI |
| ECharts | 5.4.3 | 数据可视化 |
| 基础框架 | RuoYi-Vue 3.8.7 | 后台脚手架 |

---

## 快速开始

### 环境要求

| 软件 | 版本 |
|------|------|
| JDK | 21+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Maven | 3.8+ |
| Docker | 20+（OJ判题需要） |

### 启动

```bash
# 后端
cd moyun-server
export TOKEN_SECRET="your-secret-key-at-least-64-characters-long"
mvn spring-boot:run

# 前端门户
cd moyun-portal
pnpm install && pnpm run dev

# 管理后台
cd moyun-admin-vue
pnpm install && pnpm run dev
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前台门户 | http://localhost:3000 |
| 后台管理 | http://localhost:80 |
| 后端API | http://localhost:8080 |
| Swagger | http://localhost:8080/doc.html |

**默认后台账号**：admin / admin123

### SQL 初始化（按顺序执行，全部幂等可重复执行）

```bash
# 1. 基础库（用户/菜单/角色 + 门户全部业务表）
mysql -u root -p moyun < moyun-server/src/main/resources/sql/init_v7.8.sql

# 2. v8.1 审核模块统一整合（sys_audit_task 统一审核 + 任务管理菜单，仪表板审计待办依赖此表）
mysql -u root -p moyun < moyun-server/src/main/resources/sql/upgrade_v8.1_audit_unified.sql

# 3. v8.2 通用导入模板（portal_import_template_config + 题库导入导出权限）
mysql -u root -p moyun < moyun-server/src/main/resources/sql/upgrade_v8.2_import_template.sql

# 4. v9.0 后台重构（删PK/圈子菜单，新增VIP/钱包/仪表板菜单）
mysql -u root -p moyun < moyun-server/src/main/resources/sql/upgrade_v9.0_admin_refactor.sql

# 5. v9.5 合并版（恢复打赏流水查询权限，挂载为"交易管理"页Tab按钮）
mysql -u root -p moyun < moyun-server/src/main/resources/sql/upgrade_v9.5_merge.sql
```

---

## 项目结构

```
moyun-project-document/
├── moyun-server/               # 后端 Spring Boot 服务
│   ├── src/main/java/com/moyun/
│   │   ├── portal/             # 门户前台（Controller/Service/Mapper/Judge）
│   │   ├── ext/cms/            # 后台内容管理
│   │   ├── ext/ai/             # AI模块（328文件）
│   │   ├── system/             # 系统基础
│   │   └── core/               # 核心配置（Security/Filter/Base）
│   └── src/main/resources/
│       ├── sql/                # SQL脚本（init_v7.8 + upgrade_v8.1/v8.2/v9.0/v9.5/v9.6）
│       └── application*.yaml   # 配置文件
│
├── moyun-portal/               # 前端门户（Vue3 + TS + Tailwind）
│   └── src/
│       ├── pages/              # 40+ 页面
│       ├── components/         # 公共组件
│       ├── api/                # 33 个 API 模块
│       └── stores/             # Pinia 状态管理
│
├── moyun-admin-vue/            # 管理后台（Vue3 + Element Plus）
│   └── src/views/              # cms/ai/portal/system 四大模块
│
├── docs/                       # 项目文档
├── REVIEW_REPORT.md            # 代码评审报告
├── moyun-admin-refactor-v9.patch  # v9.0完整变更补丁
└── README.md                   # 本文档
```

---

## 文档导航

| 文档 | 说明 |
|------|------|
| [docs/01_项目介绍.md](docs/01_项目介绍.md) | 项目定位、模块清单、核心特性 |
| [docs/02_技术架构.md](docs/02_技术架构.md) | 技术选型、架构设计、模块依赖 |
| [docs/03_开发指南.md](docs/03_开发指南.md) | 环境配置、代码规范 |
| [docs/04_部署指南.md](docs/04_部署指南.md) | 环境部署、SQL初始化 |
| [docs/09_开发进度与规划.md](docs/09_开发进度与规划.md) | 当前进度、未来路线图 |
| [docs/10_功能排查清单.md](docs/10_功能排查清单.md) | 按业务链路的功能测试 |
| [docs/11_面试指南后续迭代规划.md](docs/11_面试指南后续迭代规划.md) | 面试模块迭代计划 |
| [docs/devlog.md](docs/devlog.md) | 版本变更日志 |
| [docs/开发规范.md](docs/墨韵·智库项目开发规范.md) | 全项目代码规范 |
| [REVIEW_REPORT.md](REVIEW_REPORT.md) | 全栈代码评审报告 |
| [第三方服务申请指导文档.md](第三方服务申请指导文档.md) | 域名/备案/短信/支付申请指南 |

---

## 版本历史

| 版本 | 日期 | 里程碑 |
|------|------|--------|
| v1.0 | 2026-05-24 | 基础框架搭建 |
| v3.0 | 2026-05-28 | 读书空间 + 面试空间初版 |
| v5.0 | 2026-07-01 | 积分打赏 + 广告位基建 |
| v5.2 | 2026-07-19 | 安全加固（22项修复） |
| v7.8 | 2026-08-13 | OJ判题Docker沙箱 + AI模块完善 |
| v8.1 | 2026-08-14 | 审核模块统一整合（sys_audit_task + 任务管理菜单） |
| v8.2 | 2026-08-14 | 通用导入模板 + 题库导入导出 |
| v9.0 | 2026-08-15 | 重构：删除PK/圈子，首页5屏改版，认证分级，Admin新增VIP/钱包/仪表板 |
| **v9.5** | **2026-08-16** | **main-dev-article × moyun-dev-kouzi 合并：内容型首页回归、打赏流水恢复为交易管理Tab、确立"内容引流→促进消费"定位（后续开发基线）** |

---

## 相关链接

- **项目仓库**：https://github.com/zyg-allen/moyun-project-document
- **问题反馈**：通过 GitHub Issues
