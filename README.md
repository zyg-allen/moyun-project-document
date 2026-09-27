# 旭林知行 — AI 驱动的求职面试与学习成长平台

**项目版本**：v13.18（第二批中危项整改：模块边界 / 数据库口径 / 事务边界 / 回滚口径 / 写路径静默失效 / 收银台与跨域安全 / 数据库默认值与脚本幂等 七项闭环，均带守卫或单测）  
**最后更新**：2026-09-27  
**项目状态**：✅ v13.17 基线就绪（AI 统一网关配置驱动 + 公共支付/VIP/提现闭环 + 记账 App 三端 + Quartz 集群 + 安全/执行器/模块边界/数据库口径/事务边界/回滚口径/增量写 守卫与收银台越权·跨域测试） | ⏳ 后续进入报告 §六剩余中危项与运营数据填充 | 🧭 **变更铁律见 [00-项目现状总结 · 开发铁律](docs/06-规划路线/00-项目现状总结.md)**

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

### SQL 初始化（⚠️ 仅用于**全新库投产初始化**，会清空同名表）

> **重要**：本节脚本是**破坏性初始化脚本，不是增量迁移脚本**。
> `moyun-db-ddl.sql` 对多数表执行 `DROP TABLE IF EXISTS` 后再 `CREATE TABLE`，
> `moyun-menu-redo.sql` 以 `TRUNCATE TABLE sys_menu` 重建菜单。
> **严禁在已有数据的库上重复执行**（会清空业务数据与菜单改动）。
> 已有库的表结构变更请走增量脚本，见本节第 4 步说明。

```bash
cd moyun-server/src/main/resources

# 1. 建表（DDL：187 张表。实测：48 张表为 DROP TABLE IF EXISTS + CREATE TABLE（破坏性重建），
#    其余为裸 CREATE TABLE（重复执行会因"表已存在"报错）；**全脚本 0 处 CREATE TABLE IF NOT EXISTS，
#    因此整体非幂等、只能用于新库初始化一次**）
mysql -u root -p moyun-db < init-sql/moyun-db-ddl.sql

# 2. 基础数据（DML 单文件，70 条 INSERT：字典/参数/角色等；sys_menu 不在其中）
mysql -u root -p moyun-db < init-sql/moyun-db-dml-init.sql

# 3. 菜单初始化（见下方「菜单初始化策略」）
mysql -u root -p moyun-db < init-sql/moyun-menu-redo.sql

# 4. 存量库增量补丁（仅在**已有库**升级时使用，按文件名日期顺序执行，幂等）
mysql -u root -p moyun-db < increment-sql/20260925-01-portal_user唯一索引升级与存量清洗.sql
mysql -u root -p moyun-db < increment-sql/20260925-02-portal_user画像扩展字段-AI财务分析.sql
mysql -u root -p moyun-db < increment-sql/20260927-01-vip_user_card唯一键与发卡原子化.sql
mysql -u root -p moyun-db < increment-sql/20260927-02-ai_execute_log-token估算标记.sql
mysql -u root -p moyun-db < increment-sql/20260927-03-数据库规范统一（金额精度+collation）.sql
mysql -u root -p moyun-db < increment-sql/20260927-04-话题模块软删列统一.sql
mysql -u root -p moyun-db < increment-sql/20260927-05-pay_user_bank_card核验默认值与update_time.sql
```

> **执行方式**：脚本内部用 `DATABASE()` 取当前库，请带库名执行（`-D moyun-db` 或先 `USE`）；
> **全部脚本可重复执行**（v13.18 起由 `IncrementSqlIdempotencyGuardTest` 强制，dev 库已实测 7 脚本 × 2 次全 `exit 0`）。

> `20260927-01` 说明：把 `vip_user_card` 的 `idx_user_platform` 升级为
> `UNIQUE KEY uk_user_platform(user_id, platform_code)`，落实表注释"一端一卡，续费顺延"。
> 代码侧配套 `VipUserCardMapper.renewCard`（单条原子续期 SQL）——
> **该唯一键是发卡逻辑的正确性前提，已有库必须执行本脚本**（否则并发首购会重复插卡）。
> 脚本会先备份 `vip_user_card_bak_20260927` 并合并每组"最优到期/状态"再删冗余行；
> 索引变更带 `information_schema` 前置判断，**重复执行自动跳过**（v13.18 前会在 `ALTER` 处报错中断）。

> `20260927-05` 说明（v13.18）：`pay_user_bank_card.verify_status` 默认值由 `'VERIFIED'` 改 `'PENDING'`
> （fail-open → fail-closed：直插 SQL 不得绕过四要素核验），并给 `update_time` 补 `ON UPDATE CURRENT_TIMESTAMP`。
> **只改列定义、不动存量数据**（已存在的 `VERIFIED` 保持原样）。

#### 菜单初始化策略（`moyun-menu-redo.sql`）

菜单表 `sys_menu` 的 `menu_id` 由脚本**显式指定**（当前 107 条，id 1~108 连续），
菜单项之间通过 `parent_id` 引用这些固定 id。该脚本开头执行 `TRUNCATE TABLE sys_menu`
后以显式 id 全量重插 —— **这是有意的投产初始化设计**，用于保证菜单 id 编号可复现。

> **运维注意（重要）**：该脚本会**丢弃所有后台手工调整过的菜单**（新增/改名/排序/权限字符）。
> 因此**只应在初始化时执行一次**；投产后再有菜单变更，请改用
> `UPDATE sys_menu ...` + 少量 `INSERT ...`（并显式指定 `menu_id`），**不要重跑本脚本**。
>
> 关于该脚本的完整设计意图，以 `docs/07-变更日志/devlog.md` 与脚本本身为准；
> 本文档只描述可验证的脚本行为。

#### 变更惯例

后续表结构变更，在 DDL 文件对应模块末尾追加增量 `ALTER TABLE`，不改动原 `CREATE TABLE`；
独立变更以带日期前缀的增量脚本交付到 `increment-sql/`（命名 `YYYYMMDD-NN-描述.sql`）；
菜单/配置类变更走 `UPDATE` + 少量新增 `INSERT`，不动原始 `INSERT`。

### 环境变量清单（生产部署必读）

生产配置模板为 `moyun-server/src/main/resources/application-prod.yaml.example`
（复制为 `application-prod.yaml` 后填值；该文件已被 `.gitignore` 忽略，禁止提交）。
下表列出**必须显式注入**的变量——**缺失会导致启动被 `ConfigWiringValidator` 阻断**：

| 变量 | 用途 | 缺失后果 |
|---|---|---|
| `TOKEN_SECRET` | JWT 签名密钥（≥64 字符） | 启动失败（`TokenConfigValidator` fail-fast） |
| `TOKEN_ADMIN_SECRET` / `TOKEN_PORTAL_SECRET` | 管理端/门户端独立签名密钥 | 生产建议分离；两者相同会被判"未隔离"并阻断启动 |
| `MOYUN_SECURITY_CERT_NO_ENCRYPT_KEY` | 证件号 AES-GCM 口令 | **启动阻断**；且变更后已加密数据不可解密 |
| `MOYUN_PAY_SECURITY_BANKCARDENCRYPTKEY` | 银行卡号/手机号 AES-GCM 口令 | **启动阻断** |
| `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 数据库连接 | 启动失败 |
| `REDIS_PASSWORD` | Redis 密码 | 启动失败 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` / `MINIO_ENDPOINT` / `MINIO_ACCESS_URL` | 对象存储凭据 | 上传功能不可用（`admin123` 等示例值会被判定为凭据不安全） |
| `PAY_PAYOUT_CHANNEL` | 指定代付渠道标识（可空，mock 与真实通道并存时择一） | 留空取装配到的第一个渠道。**代付 mock 在生产由 `PayProperties.init()` 强制关闭**：未接入真实代付通道时，提现审核通过会被拒绝并回滚（防假打款），不会实际出金 |
| `WECHAT_PAY_APP_ID` / `MCH_ID` / `MERCHANT_SERIAL` / `PRIVATE_KEY_PATH` / `API_V3_KEY` / `NOTIFY_URL` | 微信支付商户参数 | 六项须全部非 `todo` 开头，否则回调验签 fail-closed 拒绝 |
| `MOYUN_SMS_ALIYUN_ACCESSKEYID` / `ACCESSKEYSECRET` / `SMS_ALIYUN_TEMPLATE_CODE` | 阿里云短信 | 验证码发送失败（`SMS_MOCK_ENABLED` 必须为 false） |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | 163 SMTP 发件账号与授权码（邮箱注册/找回密码验证码） | 邮件通道判为**未就绪**：接口明确返回"邮件服务暂未开启"，**不会**去连 SMTP（`MailChannelStatus`）。邮箱注册/找回密码不可用；**手机号 + 短信验证码注册仍可用** |

**可选变量**：`MOYUN_AI_API_KEY`（AI/ASR 兜底密钥，留空则视为未配置并由代码给出明确提示）、
`MAIL_HEALTH_ENABLED=true`（配置了真实 SMTP 授权码后再打开 `/actuator/health` 的 mail 指示器；
默认为 `false`，否则健康检查会因 535 认证失败而整体 DOWN，掩盖 MySQL/Redis 的真实故障）、
`KNIFE4J_PRODUCTION=true`（关闭生产 Swagger 文档）、`PORTAL_DOMAIN`（门户站点域名，用于 SEO）。

> 完整说明见 [部署指南](docs/03-部署运维/部署指南.md)；
> 启动期校验逻辑见 `core/config/ConfigWiringValidator` 与 `core/config/TokenConfigValidator`。

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
│       ├── init-sql/           # 建库初始化脚本（DDL / DML / 菜单重做；破坏性，仅新库执行一次）
│       ├── increment-sql/      # 存量库增量补丁（YYYYMMDD-NN-描述.sql，幂等）
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
| **v12.0** | **2026-09-17** | **统一 VIP 体系（端级粒度 + 全局公共端 + `@VipOnly` 注解驱动），替代三套旧 VIP** |
| **v12.1~12.2.3** | **2026-09-18** | **Mapper 内联 SQL 全量外置 XML（消除手写注入面）→ AI 调度层语义化与统一入口原则 → 业务端直连 LLM 收编** |
| **v12.3** | **2026-09-27** | **全项目架构评审整改（三轮验证 · 报告六附录 A~I）：SQL 注入止血 / 金额口径统一 / 幂等与并发 / 配置接线校验器 / 通道 fail-closed 与 mock 边界 / VIP 发卡原子化 等 20 项 P0 闭环** |
| **v13.0** | **2026-09-27** | **文档基线校正（一律以代码为准）+ 开发铁律确立（devlog 逐次记录 / 大改动同步文档 / 代码保持最新 / 结合 git 提交核对）——自本版起进入 P1 与后续开发** |
| **v13.1** | **2026-09-27** | **P1#1 记账账户"改属性"抹账（P0 资金）：列级 UPDATE 修复 + 真库确定性复现测试** |
| **v13.2** | **2026-09-27** | **P1#2 语义缓存跨用户泄漏修复（键带 userId、空用户不查不写）+ 记账遗留三项收口（还款期数/显式清空/快照抹账）** |
| **v13.3** | **2026-09-27** | **P1#3 流式链路 Token 漏计修复（新增 `TokenMeter`：真实优先/估算可区分），成本熔断不再被绕过** |
| **v13.4** | **2026-09-27** | **P1#4 `@Async` 同类自调用静默失效修复（抽 `ToolCallLogWriter`）+ 结构守卫 `AsyncSelfInvocationGuardTest`** |
| **v13.5** | **2026-09-27** | **P1#5 异步执行器收口：4 处默认 `commonPool` + 裸线程/裸线程池 → `sseStreamExecutor` 等受管池 + 守卫 `ExecutorGovernanceGuardTest`** |
| **v13.6** | **2026-09-27** | **P1#6 `/portal/admin/**` 纵深防御：双链显式 `@Order` + 门户链 `permitAll`→`authenticated` + 守卫 `PortalAdminAuthorizationGuardTest`** |
| **v13.7** | **2026-09-27** | **P1#7 Quartz 切 JDBC 集群 JobStore（显式 `SchedulerFactoryBean` + 启动幂等同步），两节点实证"同一触发器只执行一次"** |
| **v13.8** | **2026-09-27** | **P1#8（上）SQL `${}` 信任契约：数据权限片段只认切面受信键 + 最外层 MyBatis 插件；全仓 24 处 `${}` 登记守卫** |
| **v13.9** | **2026-09-27** | **P1#8（下）占位域名清理：站点域名构建期注入（`VITE_SITE_URL` 缺值即构建失败）+ admin 死 WS 客户端删除 + 后端 W-5 生产阻断断言** |
| **v13.10** | **2026-09-27** | **P1#8（收尾）收银台二维码改两端通用渲染（小程序端原为静默失效）；P1 队列至此全部闭环** |
| **v13.11** | **2026-09-27** | **第二批①模块边界：`core`/`util` 反向依赖归零（`core.security.principal` 依赖倒置）+ 守卫 `ModuleDependencyGuardTest`** |
| **v13.12** | **2026-09-27** | **第二批②数据库规范统一：金额 12 列 → `decimal(18,2)`、34 表 collation 归一、守卫 `DdlConventionGuardTest`** |
| **v13.13** | **2026-09-27** | **第二批②（收尾）话题 2 表软删列 `is_deleted`→`del_flag` 全链路迁移，债务白名单闭环** |
| **v13.14** | **2026-09-27** | **第二批③事务内远程 IO 收口：9 个方法改"事务只包 DB 写"（MinIO/本地磁盘/RAG/LLM）+ 静态守卫与运行时探针双层验证** |
| **v13.15** | **2026-09-27** | **第二批④事务回滚口径统一：10 处补 `rollbackFor`（受检异常不再提交半截数据）+ 守卫 `TransactionRollbackRuleGuardTest`；订正报告两处过期/错误计数** |
| **v13.16** | **2026-09-27** | **第二批⑤写路径"静默 0 行"收口：7 处增量写改 fail-closed（打赏扣了没入账、成长值、关注数…）+ 精选笔记数三处写入冲突收敛为唯一写入源 + 守卫 `AggregateIncrementGuardTest`** |
| **v13.17** | **2026-09-27** | **第二批⑥安全中危：收银台两个端点补归属校验（他人订单金额/支付链接泄露、可被刷成已支付）+ CORS 白名单改精确匹配（原通配 pattern 能匹配 `192.168.evil.com`，生产未配置改 fail-closed）** |
| **v13.18** | **2026-09-27** | **第二批⑦数据库默认值与脚本语义：`verify_status` 默认 `VERIFIED`→`PENDING`（fail-open 修复）、`update_time` 补 `ON UPDATE`；4 个增量脚本改为可重跑（dev 库 7 脚本 × 2 次全 exit 0）+ 两个守卫** |

---

## 相关链接

- **项目仓库**：https://github.com/zyg-allen/moyun-project-document
- **问题反馈**：通过 GitHub Issues
