# 旭林知行 - 后端服务 (moyun-server)

**项目版本**: v11.98
**最后更新**: 2026-09-17

---

## 一、项目简介

旭林知行平台的后端服务，基于 **Spring Boot 3 + MyBatis-Plus** 构建，为前台门户（moyun-portal）、管理后台（moyun-admin-vue）、记账 App（moyun-ledger-app）三端提供统一 API。

核心能力：

- **AI 统一网关**（ext/ai2）：场景化接入（ai_scene_config 绑定 Agent/模型/限流/Token 熔断/输出模式），安全三件套（提示注入防护/成本熔断/输出过滤），执行日志（ai_execute_log）与通用异步任务基础设施
- **AI 语音面试**：ASR 实时转写 + TTS 流式播报 + Agent 驱动自由面试（滑窗记忆）+ 逐题 LLM 分析与整场复盘报告
- **个人记账**：记一笔/资产负债/报表/预算/备忘录 + AI 财务分析（快照多版本、evidence/expectedImpact 量化契约）
- **内容社区**：文章/专栏/话题/评论/审核中心（sys_audit_task 统一审核）
- **支付通道**：公共支付网关（微信/mock 模拟器）、单钱包公账体系、分账/提现闭环
- **OJ 判题**：ProcessBuilder（开发）/Docker 沙箱（生产）+ 异步队列

## 二、技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.2 | 核心框架（JDK 21） |
| MyBatis-Plus | 3.5.11 | ORM |
| Spring Security | 6.3.1 | 双 FilterChain 认证（portal 免登录白名单 + admin 认证） |
| LangChain4j | 1.0.0-beta3 | AI 能力（模型接入/RAG/Agent） |
| Redis（Lettuce） | 6.0+ | 缓存/限流/异步队列/sys_config 缓存 |
| Druid | — | 连接池 + SQL 监控 |
| MySQL | 8.0+ | 主存储（188+ 张表） |
| MinIO | 8.5.12 | 对象存储 |
| Quartz | — | 定时任务 |
| Knife4j | — | 接口文档（/doc.html） |

## 三、包结构

```
com.moyun/
├── common/                 # 通用返回/常量/注解
├── core/                   # 核心配置（Security/Filter/Base/全局异常）
├── util/                   # 工具类（string/security/uuid…）
├── system/                 # 基础平台：用户/角色/菜单/字典/参数(sys_config)/审核任务/文件
├── portal/                 # 前台门户（App 端接口）
│   ├── controller/         #   /portal/article|learn|judge|growth|feed…
│   ├── service/ + mapper/  #   业务逻辑与数据访问
│   ├── judge/              #   OJ 判题引擎
│   └── security/           #   前台 FilterChain（游客白名单）
├── ext/
│   ├── cms/                # 后台内容管理（管理端接口 /cms/**）
│   │   ├── controller/     #   文章/专栏/话题/审核/支付(pay)/记账(ledger)/面试(interview|voice)/简历(resume)/VIP…
│   │   └── service/        #   VoiceInterviewServiceImpl/支付通道/记账分析…
│   ├── ai/                 # AI 模块：Agent/知识库(RAG)/工作流/场景解析(AiSceneResolver)/全局开关(AiGlobalSwitch)
│   ├── ai2/                # AI 统一网关：AiGatewayService/场景注册表/Handler/限流/熔断/执行日志
│   ├── file/               # 文件存储
│   ├── generator/          # 代码生成
│   └── job/                # 定时任务
├── ledger/                 # 记账核心（账户/流水/预算/AI 财务分析快照）
└── pay/                    # 公共支付通道（微信/mock/回调/分账/提现）
```

**路由约定**：App/前台端 `/api/portal/**`，管理端 `/api/cms/**` + `/api/system|monitor|tool/**`（网关统一加 `/api` 前缀）；支付回调 `/api/pay/callback/**` 免登录验签。

## 四、配置体系（三层）

| 层 | 位置 | 职责 |
|----|------|------|
| 环境变量 | `TOKEN_SECRET`/`MAIL_PASSWORD`/`MYSQL_*` 等 | 敏感信息注入 |
| yaml | `application.yaml`（公共）+ `application-{profile}.yaml` | 结构化配置（数据源/Redis/ASR 等）；`moyun.ai.enabled` 仅承担 bean 装配（固定 true） |
| sys_config | 数据库 + 管理台「参数设置」 | **运行时热配置**（即时生效）：`ai.global.enabled`（AI 总开关）/ `ai.resume.advice.enabled`（简历 AI）/ `voice.interview.durationMinutes`（面试时长）等 |

**开发铁律**：金额统一 RMB 元（DECIMAL(18,2) + BigDecimal，禁用分/浮点）；事务内多表写必须 @Transactional；四同步（代码/文档/SQL/菜单）。

## 五、SQL 脚本（src/main/resources/sql/）

```bash
# 1. 基础建表（create table if not exists 幂等，按业务模块分组）
moyun-db-ddl.sql

# 2. 初始化数据（4 个 DML 分片，每表先 DELETE 再 INSERT）
moyun-db-dml-init.sql ~ -4.sql

# 3. 增量补丁（命名 YYYYMMDD-NN-描述.sql，按日期顺序执行，幂等）
#    当前最新：20260917-01-ai-global-switch-sysconfig.sql
```

> 表结构变更在 DDL 对应模块末尾追加 `ALTER TABLE`（不改原 `CREATE TABLE`）；菜单/配置变更走 `UPDATE` + 少量新增 `INSERT`（不动原始 `INSERT`）。

## 六、快速开始

```bash
# 环境：JDK 21+ / MySQL 8.0+ / Redis 6.0+ / Maven 3.8+
mysql -u root -p moyun-db < sql/moyun-db-ddl.sql   # 及 DML/增量（见上）
mvn spring-boot:run    # 默认 dev profile，http://localhost:8080，文档 /doc.html
```

默认管理账号：admin / admin123（管理台登录后修改）。

## 七、相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目总览 | [../README.md](../README.md) | 项目整体介绍 |
| 项目文档索引 | [../docs/README.md](../docs/README.md) | 全量文档索引（01-09 分类） |
| 技术架构 | [../docs/01-架构设计/技术架构.md](../docs/01-架构设计/技术架构.md) | 系统技术架构 |
| 开发规范 | [../docs/02-开发指南/项目开发规范.md](../docs/02-开发指南/项目开发规范.md) | 全项目代码规范 |
| AI 网关方案 | [../docs/05-方案设计-分模块](../docs/05-方案设计-分模块) | AI 统一网关/面试全链路实施文档 |
| devlog | [../docs/07-变更日志/devlog.md](../docs/07-变更日志/devlog.md) | 版本变更日志 |

---

**项目维护者**: 旭林知行开发团队
**最后更新**: 2026-09-17
