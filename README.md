# 旭林知行 — AI 驱动的求职面试与学习成长平台

**项目版本**：v11.98（AI 网关链路根治 + 运行时开关 sys_config 化）  
**最后更新**：2026-09-17  
**项目状态**：✅ v11.98 AI 统一网关全链路收口 + 语音面试报告 LLM 复盘 + 记账 App（三端）+ 公共支付通道完成 | ⏳ 数据填充与商业化接入推进中

---

## 项目简介

**旭林知行**是一个以优质内容优先吸引流量、再驱动用户成长的平台——游客首页即可浏览丰富的文章信息流（精选/热门/分类/作者榜/读书与面试导流），登录后解锁学习、刷题、面试、阅读、写作整合的成长时间线，并通过付费阅读/VIP/打赏实现内容变现。

**品牌口号**：知行合一，助你上岸。

**产品策略（v9.5 确立）**：内容先行引流 → 体验留存 → 优质内容促进消费。

### 核心差异化

- **内容型首页**：游客可见文章轮播/精选/热门/分类导航/作者榜/友链，SEO 友好，第一屏即有内容留存
- **AI 模拟面试**：语音对话式面试（ASR 实时转写 + TTS 流式播报），统一 AI 网关收口（Agent 驱动自由面试 + 滑窗记忆），整场 LLM 复盘报告（基于简历+JD+对话内容），历史面试完整留存（对话回放/报告/重新生成）
- **个人记账 App**：uni-app 三端（小程序/H5/App），记一笔/资产负债/报表中心/预算提醒，AI 财务分析（快照多版本 + 风险/建议量化依据）
- **AI 统一网关**：场景化接入（ai_scene_config 绑定 Agent/模型/限流/Token 熔断），安全三件套（注入防护/成本熔断/输出过滤），执行日志可观测
- **OJ 在线判题**：Docker 沙箱 + 异步队列，支持多语言的真实判题系统
- **成长时间线**：统一事件追踪，学习/面试/阅读/写作全量记录，可视化回溯

### 保留模块

| 模块 | 定位 | 说明 |
|------|------|------|
| 文章系统 | **核心（引流）** | 原创/转载/专栏/付费阅读/版本管理，首页信息流主体 |
| 面试指南 | **核心** | 题库 + OJ判题 + 面经复盘 + AI模拟面试 |
| 简历模块 | **核心** | 上传解析 + 岗位匹配 + 深度优化（异步任务/采纳 diff 回放）+ AI 建议 |
| 学习工具 | **核心** | 刷题日历 + 知识图谱 + 排行榜 + 错题本 |
| 成长系统 | **核心** | 成长规则 + 徽章 + 时间线 + 等级 |
| 话题讨论 | **互动** | 话题广场 + 观点/评论（登录即可发起） |
| 读书空间 | **内容** | 书籍/书单/金句/书架（版权待处理） |
| 个人记账 | **核心（App）** | moyun-ledger-app：记一笔/资产负债/报表/预算/备忘录 + AI 财务分析 |
| AI 模块 | **赋能** | 统一网关（场景配置/限流/熔断/注入防护/执行日志）+ 知识库RAG + 图表分析 + 工作流 + Agent |
| 商业化 | **变现** | 公共支付通道（微信/mock）+ 单钱包公账体系 + 打赏流水 + VIP（面试/简历/记账）+ 提现闭环 |

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

### 记账 App (moyun-ledger-app)

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4 | 前端框架 |
| uni-app | 3.0（dcloudio） | 三端编译（微信小程序/H5/App） |
| Pinia | 2.0 | 状态管理 |
| Vite | 5.2 | 构建工具 |

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

# 记账 App（H5 / 微信小程序）
cd moyun-ledger-app
npm install
npm run dev:h5          # H5 端
npm run dev:mp-weixin   # 小程序端（微信开发者工具导入 dist/dev/mp-weixin）
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
# 1. 建表（基础 DDL：188 张表，create table if not exists 幂等，按业务模块分组）
mysql -u root -p moyun-db < moyun-server/src/main/resources/sql/moyun-db-ddl.sql

# 2. 初始化数据（DML 按 4 个分片执行，幂等：每表先 DELETE FROM 再 INSERT）
mysql -u root -p moyun-db < moyun-server/src/main/resources/sql/202608201435-moyun-db-dml-1.sql
mysql -u root -p moyun-db < moyun-server/src/main/resources/sql/202608201435-moyun-db-dml-2.sql
mysql -u root -p moyun-db < moyun-server/src/main/resources/sql/202608201435-moyun-db-dml-3.sql
mysql -u root -p moyun-db < moyun-server/src/main/resources/sql/202608201435-moyun-db-dml-4.sql

# 3. 增量补丁（20260830 起：记账/支付/AI统一网关/面试VIP 等，按文件名日期顺序执行，均幂等）
#    命名规范：YYYYMMDD-NN-描述.sql
mysql -u root -p moyun-db < moyun-server/src/main/resources/sql/20260830-moyun-interview-growth.sql
# ……依次执行至最新（当前最新：20260917-01-ai-global-switch-sysconfig.sql）
```

> 说明：后续表结构变更，在 DDL 文件对应模块末尾追加增量 `ALTER TABLE`，不改动原 `CREATE TABLE`；独立变更以带日期前缀的增量 SQL 脚本交付（菜单/配置类变更走 `UPDATE` + 少量新增 `INSERT`，不动原始 `INSERT`）。

---

## 项目结构

```
moyun-project-document/
├── moyun-server/               # 后端 Spring Boot 服务
│   ├── src/main/java/com/moyun/
│   │   ├── portal/             # 门户前台（Controller/Service/Mapper/Judge）
│   │   ├── ext/cms/            # 后台内容管理（文章/面试/简历/支付/记账…）
│   │   ├── ext/ai/             # AI 模块（知识库/工作流/Agent/场景解析/全局开关）
│   │   ├── ext/aigateway/            # AI 统一网关（场景配置/限流/熔断/执行日志/Handler）
│   │   ├── system/             # 系统基础
│   │   └── core/               # 核心配置（Security/Filter/Base）
│   └── src/main/resources/
│       ├── sql/                # SQL脚本（基础 DDL + DML 4 分片 + 日期前缀增量补丁）
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
├── moyun-ledger-app/           # 记账 App（Vue3 + uni-app：小程序/H5/App）
│   └── src/pages/              # dashboard/record/asset/liability/report/analysis/mine…
│
├── docs/                       # 项目文档（01-09 分类目录，见 docs/README.md）
└── README.md                   # 本文档
```

---

## 文档导航

完整文档清单见 [docs/README.md](docs/README.md)（文档索引）。

| 文档 | 说明 |
|------|------|
| [docs/README.md](docs/README.md) | 项目文档总索引（按分类组织） |
| [项目介绍](docs/01-架构设计/项目介绍.md) | 项目定位、模块清单、核心特性 |
| [技术架构](docs/01-架构设计/技术架构.md) | 技术选型、架构设计、模块依赖 |
| [开发指南](docs/02-开发指南/开发指南.md) | 环境配置、代码规范 |
| [项目开发规范](docs/02-开发指南/项目开发规范.md) | 全项目代码规范 |
| [部署指南](docs/03-部署运维/部署指南.md) | 环境部署、SQL初始化 |
| [功能排查清单](docs/04-测试验收/功能排查清单.md) | 按业务链路的功能测试 |
| [开发进度与规划](docs/06-规划路线/开发进度与规划.md) | 当前进度、未来路线图 |
| [devlog](docs/07-变更日志/devlog.md) | 版本变更日志 |

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
| **v10.6** | **2026-08-20** | **题库模块重构（刷题中心 + 选择题/编程题闭环）、话题/专栏审核接口收敛、SQL 脚本 DDL/DML 拆分** |
| **v10.15** | **2026-08-31** | **AI 语音面试对话可视化增强（音浪/状态环/自动聆听）** |
| **v10.16** | **2026-08-31** | **编程题接入真实 OJ（ACM 模板 7 语言 + 隐藏用例防泄露），题库练习四大断链收口** |
| **v10.17** | **2026-09-01** | **简历→语音面试全链路打通（真实简历选择/岗位自定义/历史面试列表+对话回放/菜单 SQL 归档）** |
| **v10.18** | **2026-09-01** | **LLM 驱动动态追问体系（漏洞识别/针对性追问/水平画像累积/引导提示）** |
| **v11.11** | **2026-09-01** | **简历模块重构（上传闭环+解析反显+附件版本+AI填空+全文分析+diff回放）** |
| **v11.0** | **2026-09-02** | **微信支付公共通道 + 打赏分账体系（单钱包 + 公账记账）** |
| **v11.12** | **2026-09-02** | **通用 AI 异步任务基础设施 + 全局慢请求保护** |
| **v11.14~17** | **2026-09-03** | **记账模块（个人资产管理）三阶段落地：后端核心 → uni-app 三端前端 → 报表中心/CSV/预算提醒** |
| **v11.19~26** | **2026-09-04** | **记账体验深化：全局主题/记一笔重设计/金额单位统一为元/AI 财务分析接入** |
| **v11.30** | **2026-09-07** | **AI 面试全链路动态配置化 + AI 场景配置中心** |
| **v11.31** | **2026-09-08** | **全项目金额口径统一为元（DECIMAL(18,2)+BigDecimal）** |
| **v11.33~35** | **2026-09-08** | **记账 App「我的」四功能/备忘录增强/注册与意见反馈/后台验证码风控** |
| **v11.39** | **2026-09-08** | **LLM 统一入口：scene_code 全链路接入（AiSceneEnum 场景注册表）** |
| **v11.41~43** | **2026-09-09** | **AI 统一接入层（ai_scene_config + ai_execute_log）+ 财务分析首场景切网关** |
| **v11.49~55** | **2026-09-10** | **财务分析场景标准化（evidence/expectedImpact 契约 + 快照多版本 + 异步任务化）** |
| **v11.57~59** | **2026-09-11** | **网关安全三件套（注入防护/成本熔断/输出过滤）+ 业务场景收口 7/7 全量切统一网关** |
| **v11.60~68** | **2026-09-11** | **网关可观测与治理（执行日志管理页/JSON Schema 校验/知识问答场景）+ 功能闭环自测整改清零** |
| **v11.79** | **2026-09-14** | **全平台支付统一标准（单钱包 + 公账记账 + 提现闭环 + 收入管理）** |
| **v11.81~85** | **2026-09-15** | **记账/面试/简历三 VIP 接入公共支付通道 + 会员付费点免费体验 2 次** |
| **v11.90~94** | **2026-09-15** | **AI 语音面试 V2→V4：即问即答快链路 + 异步批量分析 + JD 输入 + warmup 预热** |
| **v11.95** | **2026-09-16** | **AI 能力架构收口（场景提示词废弃 + default_chat 治理 + 面试主干网关化灰度 + JSON Mode）** |
| **v11.96** | **2026-09-16** | **语音面试时长制（20 分钟倒计时）+ 报告生成 P0 竞态修复 + 历史页异步进度** |
| **v11.97** | **2026-09-16** | **报告整场 LLM 复盘（基于简历+对话内容）+ 报告页重设计 + 重新生成链路** |
| **v11.98** | **2026-09-17** | **AI 网关链路根治（不可变 Map 击穿输入清洗修复）+ 运行时开关全面 sys_config 热配置化** |

---

## 相关链接

- **项目仓库**：https://github.com/zyg-allen/moyun-project-document
- **问题反馈**：通过 GitHub Issues
