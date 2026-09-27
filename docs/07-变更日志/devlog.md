# 开发日志（devlog）

> 2026-09-17 v11.98 后瘦身：历史条目仅保留「版本 + 修改类目 + 简介」，实施细节沉淀于方案文档与《00-项目现状总结》。v12 起新条目同样只记类目+简介。

## v13.24 (2026-09-27) 第三批③：交付与验收清单（给验收人的一页速查）

> 第三批收尾：把 v13.11~v13.24 十三批的**问题 → 改动 → 验证命令 → 关联守卫**汇成一份可复跑的清单，
> 并明确"未完成项/需人工输入项"，便于人工复核而不是只看结论。

- 新增 `docs/09-临时报告/整改交付与验证清单（v13.11~v13.24）.md`：① 一分钟速查（3 条命令）② 逐批表格
  （含每批的单独复跑命令与预期）③ 关键设计取舍（请求级记忆化 vs TTL / WS 为何用票据 / 防腐层落点 / 脚本幂等判定标准）
  ④ 未完成与需人工输入项 ⑤ 验收建议顺序（含"反向验证"做法：改回缺陷看守卫是否变红）；
- README 文档导航加入该清单；`mvn -o test` **422 例全绿**。

## v13.23 (2026-09-27) 第三批②：WS Origin 收紧 + `birthday` 改 `date`

**① WebSocket 握手 Origin 校验**（§6.4 / 附录 AE 残留项）

- 背景：`/ws-asr` 与 `/ws-message` 原本 `setAllowedOriginPatterns("*")`。WebSocket 不受同源策略保护，
  浏览器允许任意站点发起握手，服务端必须自判 Origin；
- 做法：新增 `ResourcesConfig.isWsOriginAllowed(origin)`（口径与 CORS 完全一致：**无 Origin 放行**
  （小程序/原生/服务端）→ 回环 → 环境变量精确白名单 → 显式开启的私网 IP 字面量；其余拒绝），
  在 `PortalWebSocketAuthInterceptor` **最早**执行（先于票据消费）；`isPrivateNetworkOrigin` 提升为 `public`
  供 WS 复用（CORS 侧行为不变）；
- 4 例新单测：`192.168.evil.com`（带合法票据也拒绝）、`localhost:5173` 放行、无 Origin 放行、
  合法 Origin 但无凭证仍 401（Origin 不替代鉴权）。

**② `portal_user.birthday`：`varchar(20)` → `date`**（§6.5）

- 问题：日期语义却按字符串存 → 比较/排序按字符串、无法有效索引，年龄统计必须逐行
  `STR_TO_DATE(u.birthday,'%Y-%m-%d')`；非法值（`''`/`1995/01/01`/`未知`）静默留在列里；
- 改动：DDL 改 `date`；新增幂等增量脚本 `20260927-06`（**先建备份表** → 仅当当前仍是字符串类型时
  把空串/非法日期清成 NULL → `MODIFY COLUMN` 为 `date` → 可执行复核 SQL）；
  `PortalCreatorMapper.xml` 的年龄分桶去掉 `STR_TO_DATE`，直接用 `u.birthday`（`IS NULL` 归 unknown 桶）；
  **Java 侧 `PortalUser.birthday` 仍为 String**（Connector/J 对 DATE 列 `getString` 返回 `YYYY-MM-DD`）→ JSON 契约与前端表单不变；
- 验证：dev 库执行两次均 `exit 0`，复核 `DATA_TYPE=date`、`invalid_remaining=0`、`future_dates=0`；
- 备注：管理端"生日"当前是文本输入（占位"如 1995-01-01"），改 date 后**非法输入会被 MySQL 拒绝**（更安全），
  后续可换成日期选择器（已记入清单）。

**同步**：报告 §6.4 行（WS 行补 Origin 收口）+ §6.5 birthday 行改判 + 总览第 41 项 + 附录 AF；
`项目开发规范` §3.3.4 补"WebSocket Origin"与"日期语义列用 date"；`00-项目现状总结` 铁律 18 扩展；README；本条目。

## v13.22 (2026-09-27) 第三批①：`system→portal` 防腐层（审核中心 8 个 Handler，24→6 条边）

> 报告 §6.3："`system`（管理端）直连 `portal`（用户端）数据层 8 处无防腐层"。
> 实测 `system -> portal` 共 **24 条 import 边**，其中 **8 个 `*AuditBizHandler` 占 18 条**
> （每个 = 门户实体 + 门户 Mapper，两个还带门户服务）。

**做法（依赖倒置，同 v13.11 的 `core.security.principal` 手法）**

| 位置 | 内容 |
|---|---|
| `core` 新增端口 | `com.moyun.core.portal.AuditContentPort`：只暴露中性类型 —— `Map<String,Object> loadDetail(taskType, bizId)` 与 `applyAudit(taskType, bizId, approved, auditorId, auditorName, opinion)`，**门户实体/Mapper 不出现在签名里** |
| `portal` 新增适配器 | `com.moyun.portal.audit.AuditContentAdapter`：按 taskType 分派到 8 类业务（文章/专栏/认证/反馈/面试评论/面试经验/举报/话题），**逐条搬运**原 Handler 的详情字段与状态取值（published/rejected/active/resolved…）与日志文案 |
| 8 个 Handler 瘦身 | 只保留 `supportedTaskType()` 与三个端口调用（约 50 行 → 30 行），**不再 import 任何 `com.moyun.portal.*`** |

**边数变化（守卫实测）**：`system -> portal` **24 → 6**（余 6 条：看板聚合 2、通知收件人 1、管理员代发私信 3，
已在 `ModuleDependencyGuardTest` 登记为后续批次）；`portal -> ext.cms` **78 → 82**（+4：适配器需调用 CMS 的
文章/专栏/面试/举报下架服务，属 ACL 的合理代价，理由已登记）；`system -> ext.cms` 的 2 条（文章/下架服务）随之消失。

**验证**：`ModuleDependencyGuardTest` 5/5（冻结清单精确计数）✓；`mvn -o test` 见"同步"行内实测计数；
`grep '^import com.moyun.portal.' system/**` 实测仅剩 6 条（即上面登记的三处）。

**同步**：报告 §6.3 行改判 + 总览第 40 项 + 附录 AF；`项目开发规范` §1.3.1 补"防腐层端口约定"；
`00-项目现状总结` 铁律 10 扩展（模块依赖方向）；README 版本历史；本条目。

## v13.21 (2026-09-27) 第二批⑩：WebSocket 握手改「一次性短时效票据」（§6.4 最后一行）

> §6.4 最后一行："WebSocket token 走 URL query"。浏览器 {@code new WebSocket()} **不能自定义请求头**，
> 所以这条路只能"换凭证形态"，不能靠加 header 解决。方案取舍随后端取证定案。

**取证**

- 服务端 `PortalWebSocketAuthInterceptor` 原本 `?token=` 优先、`Authorization` 兜底 —— **头方式后端早就支持**，
  问题纯在前端（浏览器限制）；
- 两个前端建连点：`utils/websocket.ts:104`（`/ws-message` STOMP 私信）、
  `composables/useSpeechRecognition.ts:380`（`/ws-asr` ASR 实时流）；记账小程序/后台**无** WS 客户端；
- 门户链 `/portal/**` 是 `anyRequest().authenticated()`，新端点放这里默认需登录（**不能**放 `/portal/interview/**`，那是 permitAll）；
- 前端 `utils/websocket.ts` 类注释里其实早写着"生产环境建议：后端为 ws 握手单独签发短时效的一次性 token"。

**方案选择（两个候选）**

| 方案 | 优点 | 为何未选 |
|---|---|---|
| A. 子协议 `Sec-WebSocket-Protocol` 传 token | 零新增状态、不占 URL | 依赖"服务端必须回显子协议"与"代理转发该头"，失败是**连接级失败**；小程序/原生支持不一致 |
| **B. 一次性短时效票据（选定）** | 与协议/代理解耦、浏览器/小程序/原生一致、可单测 | URL 里仍有凭证（但一次性 + 60s，价值从"账号接管"降到"极短窗口内一次握手"） |

**改动**

| 层 | 内容 |
|---|---|
| 后端 | 新增 `WsTicketService`（`SecureRandom` 16 字节 → 32 hex；Redis `ws:ticket:{t}` 存 userId，**TTL 60s**；消费走 **Lua GET+DEL 原子取并删** → 只能用一次）；新增 `POST /portal/ws-ticket`（需登录，返回 ticket + expiresIn）；`PortalWebSocketAuthInterceptor` 改为 ①`?ticket=` ②**显式拒绝 `?token=`**（告警 + 401，防老客户端把漏洞带回来）③`Authorization` 头 |
| 前端 | `utils/websocket.ts` 新增并导出 `requestWsTicket()`；两处建连改为先换票据再 `?ticket=`；票据获取失败**保持轮询降级**（不退回明文 token） |

**残留风险（如实记录）**：票据仍出现在 URL，访问日志留痕依旧存在，但一次性 + 60 秒；
彻底消除需改子协议传参或 Cookie 会话，属独立议题。另记：`/ws-asr` 的 `setAllowedOriginPatterns("*")`
未收紧（鉴权独立于 Origin，风险低），已列入待办。

**验证**：`WsTicketServiceTest` 5 例（随机性/32hex/必须带 TTL/原子脚本键/空票据不碰 Redis）；
`PortalWebSocketAuthInterceptorTest` 8 例（票据放行、重放/过期拒绝、**`?token=` 一律拒绝的回归锁**、
Authorization 可用、无凭证 401、票据优先、非 Servlet 请求安全拒绝）；门户 `vue-tsc + vite build` 通过；
后端 `mvn -o test` 见"同步"行内实测计数。

**同步**：报告 §6.4 行改判 ✅ + 总览第 39 项 + 新增附录 AE；`项目开发规范` §3.3.4 补"WebSocket 握手凭证"；
`00-项目现状总结` 铁律 18；README 版本历史；本条目。

## v13.20 (2026-09-27) 第二批⑨：LLM JSON 提取收敛为唯一实现（守卫比报告多抓 1 处，且守住了提示词模板）

> §6.1 那行原文："`LlmJsonExtractor` 死代码，同类逻辑 5 份实现"。取证后：**死代码属实**，
> 但"5 份"是误并——报告列的 `WechatPayChannel:263` 其实是 javadoc 里的 fail-closed 说明，
> 跟 JSON 提取无关；真实是 **4 处抠取 + 2 处剥围栏**。

**取证与收敛（4 处抠取 + 2 处剥围栏 → 1 个 util 的 3 个方法）**

| 原实现 | 问题 | 处置 |
|---|---|---|
| `ext/cms/service/LlmJsonExtractor` | **零调用方死代码**；且只处理"围栏在首行"、不支持数组 | **删除** |
| `AbstractAiSceneHandler.extractJson` | 另内联一份"首个 `[` 到最后一个 `]`"数组分支；用 `lastIndexOf` 切尾（内容含 `}` 会切错） | 删私有方法，`hasJsonBody` 直接用 `extractNode`（顺带删掉第 5 个内联分支） |
| `VoiceInterviewServiceImpl.extractJsonObject` | 无围栏处理（注释却写"剥 Markdown 围栏"） | 委托 `extractNode` |
| `WorkflowGeneratorServiceImpl.extractJson` | 什么都匹配不上时返回 null；三段式围栏判断 | 委托 `extract`（空→null，保持调用方语义） |
| `AbstractAiSceneHandler.cleanLlmText` | 内联剥围栏（仅"以围栏开头"） | 委托 `stripCodeFence` |
| `PromptGeneratorServiceImpl.cleanResponse` | 正则剥围栏 | 委托 `stripCodeFence` |
| **`ext/ai/util/SqlUtils.cleanSql`**（**守卫新抓**） | 两行 `replaceAll("```sql\\s*","")` 手写剥围栏（语言标注不止 sql） | 委托 `stripCodeFence` |

**唯一实现** `com.moyun.util.json.LlmJsonExtractor`：`extract`（围栏任意位置 + 对象/数组谁先取谁 +
**括号配平扫描**（跳过字符串字面量与转义，`{"a":"}"}` 不会被切错）+ 括号未配平时"首个左括号→末右括号"兜底，
完全不闭合返回空串）、`extractNode`（解析失败返回 null，不抛异常）、`stripCodeFence`（只去围栏，不做 JSON 抠取）。

**新增守卫 `LlmJsonExtractionGuardTest`**：禁止业务代码再手写 `lastIndexOf('}')` 抠取或 ``` 围栏处理。
写这条守卫踩了**两个坑**（都固化成 fixture）：
1. 复用 `TransactionRemoteIoGuardTest.stripCommentsAndStrings` 会**把字符串字面量内容抹掉**，
   而"围栏"恰好写在字符串里 → 规则永远匹配不到（**守卫空转**）。改为只剥注释、保留字符串，
   且 fixture 改为**走与主扫描完全相同的管线**（第一版 fixture 直接测正则，才掩盖了空转）；
2. 提示词模板里写 `sb.append("请输出 ```json\n")` 是**合法用法**（告诉模型用围栏包裹），
   与"解析时剥围栏"是两件事 → 规则收窄为"围栏 + 同一行有处理调用（indexOf/startsWith/replaceAll/…）"。

**验证**：守卫全量 **1436 文件 / 唯一实现存在 / 违规 0**；**红证** = 往 `PromptGeneratorServiceImpl`
塞回一行手写围栏处理 → 守卫精确报出（同时抓出 `SqlUtils` 这个真实残留），删除后归零；
`LlmJsonExtractorTest` 6 例（围栏任意位置/数组/混排/配平/截断/非法输入/散文场景），
后端 `mvn -o test` 见"同步"行内实测计数。

**同步**：报告 §6.1 行改判 ✅（含"5 份"订正）+ 总览第 38 项 + 新增附录 AD；
`项目开发规范` §2.5 增"LLM 输出解析只有一个入口"；`00-项目现状总结` 铁律 17；README 版本历史；本条目。

## v13.19 (2026-09-27) 第二批⑧：AI 网关场景配置改「请求级记忆化」（§6.2 最后一行）

> §6.2 剩的最后一行："每请求 2-3 次同表 DB 查询"。取证后是 **2~4 次**（下面有实测路径）。
> 关键在**怎么修不破坏语义**：`AiSceneRegistry` 类注释原本写着"配置不再内存缓存——每次直查，
> 管理端改配置**下次调用立即生效**"。所以不能随手加 TTL 缓存（会把"立即生效"变成"最多 TTL 后生效"）。

**取证：一次通用入口请求查几次 `ai_scene_config`**

| 顺序 | 位置 | 查询 |
|---|---|---|
| ① | `AiGatewayController.rejectIfNotOpen(sceneCode)` | 主码 1 次 |
| ② | `AiGatewayService.execute` → `registry.getConfig(sceneCode, task)` | 全码 1 次；未命中再回退主码 **又 1 次**（这次与 ① 完全重复） |
| ③ | 意图澄清路径 `registry.getConfig(intent.getSuggestedScene())` | 再 1 次 |
| — | 流式路径（`:278`）与场景参数裁剪（`:531`） | 同样各查 1 次 |

→ 纯同步无 task 时 **2 次**，带 task 且全码未配置时 **3 次**，走意图澄清可到 **4 次**。

**修法：请求级记忆化（零 TTL、零跨请求残留）**

- `getConfig(sceneCode)` 改为：先看**当前请求**的 `RequestAttributes`（键 `aigateway.sceneConfig.{sceneCode}`）→
  命中即返回；未命中才查库，并把结果写入请求属性；
- **负结果同样记忆化**（用哨兵对象表示"查过且不存在"，因为 `setAttribute(null)` 等于删除属性）；
- 缓存随请求销毁，"管理端改配置**下次调用立即生效**"的语义**完全保留**（比 TTL 缓存更强）；
- 非 HTTP 上下文（启动一致性检查、异步线程、单测直调）自动退化为直查，行为与改动前一致；
- 顺带在类注释写明**调用约定**：调用方只读返回对象（已核全仓调用方均为 `config.getXxx()`，无 `config.set`）。

**效果**：真实链路（控制器查主码 → 网关解析 `scene:task`）从 **3 次降到 2 次**（主码回退改为缓存命中）；
后续任何重复读取 0 次新增查询。全码查询本身是另一行数据，2 次是下界。

**验证**

- 新增 `AiSceneRegistryRequestCacheTest`（6 例）：同场景同请求只查 1 次且返回同一实例；**真实链路全程 2 次**；
  回退语义不变；**跨请求即时生效**（同一 sceneCode 两个请求分别读到"旧配置/新配置"，专门锁死"无 TTL 残留"）；
  负结果记忆化；无请求上下文时退化为直查且不抛异常；
- **红证**：临时让 `getConfig` 绕过记忆化 → 3 例失败（`Wanted 1 time but was 2 times` / 全码回退断言），还原后全绿；
- 后端 `mvn -o test`：见"同步"行内实测计数。

**同步**：报告 §6.2 最后一行改判 ✅ + 总览第 37 项 + 新增附录 AC；`项目开发规范` §2.6 增"配置读取：请求级记忆化"；
`00-项目现状总结` 铁律 16；README 版本历史；本条目。

## v13.18 (2026-09-27) 第二批⑦：fail-open 默认值 + 增量脚本可重跑（守卫比报告多挖出 3 个脚本）

> §6.4 那行原文只点了一个脚本（`20260925-01`）："`DROP KEY` 无守卫、`SET phone=NULL` 破坏性"。
> 写成守卫后**多挖出 3 个同样不可重跑的脚本**——正是"把结论固化成检查"的价值。

**改动**

| 项 | 修复前（dev 库实证） | 修复后 |
|---|---|---|
| `pay_user_bank_card.verify_status` | `NOT NULL DEFAULT 'VERIFIED'`（**fail-open**：绕过 `BankCardServiceImpl` 四要素核验的直插 SQL 直接拿到"已验证"，而提现闸门只认 `VERIFIED`） | DDL 默认值改 `'PENDING'`；新增幂等增量脚本 `20260927-05`（只改列定义，**不动存量数据**） |
| `pay_user_bank_card.update_time` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` 缺 `ON UPDATE`（看着像自动维护，实际更新不刷新；全仓仅此 1 处） | 补 `ON UPDATE CURRENT_TIMESTAMP`（同脚本） |
| `20260925-01`（报告点名） | 裸 `DROP KEY idx_email, ADD UNIQUE uk_email`：dev 库已迁移（`uk_*` 已建、`idx_*` 已删）→ 重跑必报 **1091**；两处 `SET phone/email = NULL` 无备份、复核只在注释 | 索引变更按"idx 是否存在 / uk 是否已建"四组合走 `information_schema` 守卫；**先建备份表**再做清洗；末尾给出可执行复核 SQL |
| `20260925-02`（守卫新挖出） | 单条多列 `ADD COLUMN` ×5 → 重跑报 **1060** 中断 | 拆为 5 个逐列守卫（也容忍"上次中途失败"的部分迁移状态） |
| `20260927-02`（守卫新挖出） | 裸 `ADD COLUMN token_estimated` → 重跑报 **1060**；原注释还写着"报错属预期" | 加守卫；并把"报错属预期"改为明确的幂等说明 |
| `20260927-01`（守卫新挖出） | 裸 `DROP KEY idx_user_platform + ADD UNIQUE uk_user_platform` → 重跑报 **1091**（它有备份表但没有索引守卫） | 加四组合守卫 + 可执行复核 SQL |

**口径（写入规范 §2.8）**：会报错的结构变更（`DROP KEY/INDEX`/`ADD COLUMN`/`ADD KEY/UNIQUE`/`DROP COLUMN`/`CHANGE COLUMN`）
必须 `information_schema` 前置判断；`MODIFY COLUMN` 天然幂等**不在此列**但也不得当作挡箭牌；
破坏性清洗必须先建备份表 + 末尾给**可执行**复核 SQL；落地验证 = **在已迁移库上连跑两次全 exit 0**。

**验证**

- 新增 `IncrementSqlIdempotencyGuardTest`（2 例）：扫描全部 **7 个脚本**（5 个含结构变更 / 1 个含破坏性清洗）→ 违规 0；
  **红证** = 首次运行即报出 4 个未加守卫的真实脚本（含报告未提及的 3 个），逐个修复后归零；6 组 fixture
  （含"注释里提到 `SET x = NULL` 不得误报"——第一版没剥离 SQL 注释时正是被自己的注释绊倒）；
- `DdlConventionGuardTest` 增第 7 例：**核验/审核类状态列默认值不得是放行语义**（`VERIFIED/APPROVED/PASSED/...`）
  + `update_time` 有 `DEFAULT CURRENT_TIMESTAMP` 必须带 `ON UPDATE`；全量 **187 表 / 2537 列 / 违规 0**，含 fixture；
- **dev 库实证**：`moyun-db` 上把 `increment-sql/` **全部 7 个脚本各连跑两次 = 14/14 exit 0**（修复前其中 4 个会中断）；
  复核 `verify_status` 默认已 `PENDING`、`update_time` EXTRA 已含 `on update`、`uk_email/uk_phone/uk_user_platform` 唯一且重复组 0。

**同步**：报告 §6.4 两行改判 ✅ + 总览第 36 项 + 新增附录 AB；`项目开发规范` §2.8 增"增量脚本必须可重跑"硬约束；
`00-项目现状总结` 铁律 4/11 扩展；README 版本历史与增量脚本清单；本条目。

## v13.17 (2026-09-27) 第二批⑥：收银台越权（IDOR）+ CORS 白名单收口（含 §6.6 过期行订正）

> §6.4 两行"中危安全"回源码复核后，**一行比报告写的更重，一行比报告写的更危险**；
> §6.6 那行则已在前批修完（报告未回写）。

**取证与修复**

| 报告行 | 实测 | 处置 |
|---|---|---|
| mock 支付端点不校验订单归属（`PortalPayController:70-80`） | ✅ 属实，且**同一控制器的 `/status/{payNo}` 也漏归属**——只判"已登录"，`payNo` 可枚举 → 他人订单**金额/支付链接/过期时间**泄露（IDOR）。`/mock` 更重：可把**他人订单**刷成已支付，触发真实后续链路（发卡/记账/打赏到账） | 新增 `ownOrderOrNull(payNo, userId)` **唯一归属校验**，`/status` 与 `/mock` 共用；查不到与不属于自己返回同一个 `403`（不泄露存在性）；`/mock` 的归属校验**早于** `mockPaySuccess`。新增 7 例单测（含"他人订单不泄露金额/链接""`/mock` 绝不调用 `mockPaySuccess`""未登录不触碰查询""后台登录态不构成门户身份"） |
| CORS 默认放行内网段 + `allowCredentials(true)`（`ResourcesConfig:100-113`） | ✅ 属实，且**比报告更危险**：`addAllowedOriginPattern("http://192.168.*")` 中的 `*` 是通配符 → 会匹配 **`http://192.168.evil.com`**（可注册域名）；内网放行还无"仅开发"约束，生产漏配即生效 | 抽出可单测的 `buildCorsConfiguration(envOrigins, prodProfile)`：环境变量改**精确** `addAllowedOrigin`（不再用 pattern）；非生产默认只放行**回环**（含端口通配，host 段固定不可伪造）；**生产未配置即 fail-closed 不放行任何源**；局域网设备改为按 Origin **精确放行一次**且要求 host 是私网 IP 字面量（`10/8`、`172.16/12`、`192.168/16`、回环），域名一律拒绝，并需显式 `CORS_ALLOW_LAN_DEV_ORIGINS=true` 且非生产。新增 4 例单测（含 `192.168.evil.com`、`portal.moyun.com.evil.com` 必须拒绝，`localhost:5173` 必须放行） |
| §6.6 打赏幂等查询缺 `userId` → 跨用户订单泄露（`LedgerTipServiceImpl.createTipOrder:62-78`） | ❌ **已过期**：现实现已带 `.eq(LedgerTipOrder::getUserId, userId)`，源码注释还记录了当时的漏洞说明 | **零代码改动**，仅改判报告 ✅ |

**验证**：`mvn -o test` **387 例全绿**（376 → 387，本批 +11）；新增两个测试类均含"修复前会失败"的断言
（`192.168.evil.com` 放行、`/mock` 刷他人订单）。

**同步**：报告 §6.4 两行改判 ✅ + §6.6 行改判 ✅ + 总览新增第 35 项 + 新增附录 AA；
`项目开发规范` 新增 §3.3.4「越权（IDOR）与跨域白名单」；`00-项目现状总结` 铁律 15；README 版本历史；本条目。

## v13.16 (2026-09-27) 第二批⑤：写路径"静默 0 行"收口 + 精选笔记数三处写入冲突（含报告计数订正）

> §6.2 原文"`insertIfNotExists` 返回值 10 个调用点无一检查"。取证后分两层：**返回值不检查本身不丢数据**
> （`INSERT IGNORE` 幂等），真风险在**紧随其后的增量写命中 0 行**；另外报告漏了一类更硬的缺陷——
> 同一个统计列被**三处写入**、且拿成长值增量当"篇数"。

**取证（19 个调用点 / 6 文件，逐点判定；报告记 10 处）**

| 判定 | 位置 |
|---|---|
| 🔴 增量写裸调用 → 修（7 处关键路径） | `PortalGrowthServiceImpl`（`recordEventWithTarget` 的 `addGrowth`+`updateStats` 13 分支、成就奖励 `addGrowth`、`checkin` 的 `updateById`）、`PortalTipServiceImpl`（作者 `addPoints`）、`PortalFollowServiceImpl`（8 处关注/粉丝增减）、`PortalArticleServiceImpl`（`addArticleWordSum`） |
| 🔴 行缺失即 NPE → 修 | `PortalGrowthServiceImpl.checkin`：`insertIfNotExists` 后直接 `stats.getLastCheckinDate()` |
| ✅ 已检查（同文件先例） | `deductGrowth`、`deductPoints`、`CmsGrowthUserController`（`toAjax(返回值)`） |
| ✅ 读路径兜底，不改 | `getUserGrowth` / `getUserStats`（select 后 null 兜底） |
| 🟡 显式最佳努力 → 0 行记 `log.error`（可见、不回滚） | `UserProfileSnapshotServiceImpl.refreshWeakTags` |

**顺带发现并修掉：`note_adopted`（精选笔记数）三处写入冲突**

- `updateStats` 的 `case "note_adopted"` 用 **`delta`（成长规则 growthDelta）当"篇数"**写；
- 控制器 `featureSubmission` 再显式 `+1`（**双重计数**），且 `updateFeatured(true)` 无条件返回 >0 → **重复采纳持续累加**；
- `unfeatureSubmission` 只 `-1`（与采纳侧不对称）→ 计数单调虚高。

**修法**：`updateStats` 删除该分支；新增 `IPortalGrowthService#updateNoteAdoptedCount(userId, ±1)`
（`@Transactional(rollbackFor=Exception.class)` + `insertIfNotExists` + 影响行数校验）作为**唯一写入源**；
`PortalInterviewServiceImpl.adoptSubmission` 补事务并只在**状态真正翻转**时写计数；
两个控制器端点收敛为调用 `adoptSubmission`（消除与 Service 重复的实现），删除因此失效的两个注入
→ 模块边 `ext.cms→portal` **280 → 278**（冻结清单同步）。

**口径（写入规范）**：① 增量写必须校验影响行数，0 行即 fail-closed 回滚；② 只对**懒创建的聚合行**
（stats/growth/badge）强制，内容行计数不扩张；③ 最佳努力路径必须 `log.error` 让 0 行可见；
④ "篇数/次数"类列固定 ±1，不得复用 `growthDelta`。

**验证**

- 新增 `AggregateIncrementGuardTest`（2 例）：全量 **29 个聚合表增量写调用点 / 违规 0**（正向控制 ≥ 8）+
  7 组 fixture（裸调用、赋值不校验、`require*` 包裹、内容计数不报、集合 `addAll` 不报、`baseMapper` 泛型解析）；
- **红证**：把 `PortalGrowthServiceImpl:551` 还原成裸调用 → 守卫精确报该行，还原后绿；
- 回归：`PortalTipServiceTest` 新增"作者积分入账 0 行 → `POINTS_CREDIT_FAILED`、不落订单"（原有 3 例补 `addPoints` 桩）；
- 后端 `mvn -o test`：**376 例全绿**（373 → 376）。

**同步**：报告 §6.2 行改判 ✅（附录 Z）+ 总览第 34 项 + §6.3/附录 U 模块边计数 280→278；
`项目开发规范` §2.7 增"增量写口径"；`00-项目现状总结` 铁律 14；README 版本历史；本条目。

## v13.15 (2026-09-27) 第二批④：事务回滚口径统一（10 处 rollbackFor + 结构守卫 + 两处报告计数订正）

> §6.2 有两行"老账"：`Redis 锁无 owner 校验即 DELETE` 与 `221 个 @Transactional 中 readOnly=0、约 205 处缺 rollbackFor`。
> 照例先回源码取证——**一行早已修完（报告未回写），一行的计数错了 20 倍**。

**取证（先量后改）**

| 报告原文 | 实测 |
|---|---|
| `Redis 锁无 owner 校验即 DELETE`（`KnowledgeProcessProgressServiceImpl.java:55-80`，"全仓仅此 1 处 `setIfAbsent`"） | ❌ **已过期**：该实现现已走统一 `DistributedLockUtil.tryLockWithWatchdog`（token 归属唯一 + Lua 比较-删除），并用 `ThreadLocal<Map<Long, Lock>>` 持有句柄，`releaseLock` 在"非本线程持有"时**拒绝删除**（注释明确写了历史实现会误删他人锁）。全仓已无裸 `setIfAbsent` 锁模式 → 本批不写代码，只在报告改判 ✅ 并加防回归说明 |
| `约 205 处缺 rollbackFor` | ⚠️ **计数订正**：实测方法级 `@Transactional` **211 个，其中 201 已带 `rollbackFor`，仅 10 处缺**（"205"与实际差 20 倍）。同族订正见附录 O-1 / W-1 |

**改动（10 处补齐，全部是多步写方法）**

| 文件 | 方法 | 为何必须回滚 |
|---|---|---|
| `AiSceneConfigVersionService` | `createWithSnapshot` / `updateWithSnapshot` / `rollback` | 配置行 + 版本快照两步写，半截即"配置生效但无快照/版本号错位" |
| `KnowledgeConfigServiceImpl` | `applyConfiguration` / `createDefaultConfig` / `updateConfig` | 知识库配置多表写（含分段/检索参数） |
| `ConversationServiceImpl` | `addMessage` / `deleteConversation` | 消息写入 + 会话统计更新 / 会话级联删除 |
| `ToolServiceImpl` | `bindToolsToAgent` | 先删后插的关联关系，半截即"工具全解绑" |
| `ModelConfigServiceImpl` | `setDefault` | 先清旧默认再置新默认，半截即"没有默认模型" |

**新增结构守卫 `TransactionRollbackRuleGuardTest`（3 例，四条规则）**

1. 方法级 `@Transactional` **必须显式声明回滚口径**（`rollbackFor`/`rollbackForClassName`，或 `readOnly = true`）；
2. **禁止 `noRollbackFor`**（等于主动放弃回滚，需白名单说明理由）；
3. **注解不得标在代理看不见的方法上**：`private`/`protected`/`static`/方法级 `final`（CGLIB/JDK 代理都拦不到 → 静默无事务），
   与 `AsyncSelfInvocationGuardTest` 属同一"静默失效"家族；
4. **禁止类级 `@Transactional`**（会把只读方法拖进写事务，与"事务边界只包 DB 写"的收窄口径冲突）。

> 第 3 条踩过一个自己写的坑：判定修饰符时必须**先剥掉注解再找方法自己的 `(`**——
> 否则 `decl.indexOf('(')` 会命中 `@Transactional(...)` 的括号，把注解**之后**的
> `private`/`static`/`final` 全部漏掉（fixture 6/6b 就是为这条坑写的回归）。

**验证**

- 守卫全量：**1437 文件 / 211 个方法级 `@Transactional` / 违规 0**（含正向控制"识别数 ≥ 150"，防解析器失效假绿）；
- **红证**：临时回退 `ToolServiceImpl.bindToolsToAgent` 的 `rollbackFor` → 守卫报
  `ToolServiceImpl.java:76 bindToolsToAgent() @Transactional 未声明回滚口径 … expected: <true> but was: <false>`，还原后绿；
- 后端 `mvn -o test`：**373 例全绿**（370 → 373，本批新增 3 例）。

**同步**：报告 §6.2 两行改判/订正（附录 Y）+ 新增附录 Y；`项目开发规范` §2.7 增"回滚口径硬约束"（含
`TransactionTemplate` 默认只回滚 `RuntimeException`/`Error` 的口径）；`00-项目现状总结` 铁律 12 扩展；README 版本历史；本条目。

## v13.14 (2026-09-27) 第二批③：事务内远程 IO 收口（事务只包 DB 写 + 双层守卫）

> 报告 §6.2 那条"事务内做远程 IO（MinIO / LLM）"只点了两个文件两处行号。
> 本批先做**全项目取证**（剥离注释 + 同文件调用图二阶扫描），实测出**4 处真实命中 / 9 个方法**，
> 报告行号已是旧版（代码漂移）——**只有 2 处能对上，另外 7 个方法报告没提**。

**取证（先量化再动手）**

| 手段 | 结果 |
|---|---|
| 一阶：`@Transactional` 方法体内直接出现远程/磁盘 IO 标记 | `SysFileServiceImpl.uploadBytes/deleteFileById/deleteFileByUrl`（MinIO 上传/删除、本地磁盘写删） |
| 二阶：`@Transactional` 方法调用**同文件内含 IO 的方法**（私有 helper / 自调用） | `SysFileServiceImpl.uploadFile/uploadFileForPortal`（IO 在私有重载里）、`deleteFileByIds`（自调用 `deleteFileById`） |
| 已在前序批次修掉 | `KnowledgeBaseServiceImpl.uploadFileOnly`（MinIO）、`VoiceInterviewServiceImpl.start`（RAG+warmup LLM+开场白 LLM）、`VoiceInterviewServiceImpl.requestHint`（LLM） |
| 复核后**判定不修** | `PortalJobTemplateServiceImpl`（LLM 在**非事务**私有方法 `extractByLlm` 内）、`PortalTopicServiceImpl:542`（所在方法无 `@Transactional`）、`GenTableServiceImpl`（`FileUtils` 在**无事务**的 `generatorCode`，且只写文件不改库）、`VoiceInterviewServiceImpl.finish/regenerateReport`（事务内只有 DB 写 + `afterCommit` 触发异步分析） |

**改动（3 个 impl / 9 个方法，一律"远程 IO 出事务、DB 写进事务"）**

| 文件 | 改动 |
|---|---|
| `KnowledgeBaseServiceImpl#uploadFileOnly` | 去掉方法级 `@Transactional`；MinIO 上传 + 内容哈希前置到事务外，末尾 `save()` + `createDefaultConfig()` 用 `transactionTemplate.executeWithoutResult` 框住（两步仍同一事务） |
| `VoiceInterviewServiceImpl#start` | 去掉方法级 `@Transactional`；RAG 检索 / warmup LLM / 开场白 LLM 全部前置，事务块内只剩 `closeStaleInterviews` + `interviewMapper.insert` + `memoryService.initFirstTurn` + `qaMapper.insert` + `recordEvent`；`final String openingText` 供 lambda 捕获 |
| `VoiceInterviewServiceImpl#requestHint` | 去掉方法级 `@Transactional`；额度占用改**单条原子 SQL**（`COALESCE(hint_used,0) < 3` + `setSql("hint_used = ... + 1")`，0 行即"已用完"）替代"读→判→写"，LLM 调用在事务外，删掉 `updateById` 整行写回 |
| `SysFileServiceImpl`（6 个入口） | 上传：`uploadFile`/`uploadFileForPortal`/`uploadBytes` 去掉 `@Transactional`，MinIO/本地磁盘写前置，仅 `insert` 在事务内；删除：`deleteFileById`/`deleteFileByIds`/`deleteFileByUrl` 去掉 `@Transactional`，存储删除在事务外、仅 `deleteById` 在事务内。**异常语义与旧实现逐条对齐**（存储抛错 → DB 不变；存储成功 + DB 失败 → 与旧回滚后状态一致），`deleteFileByUrl` 用 `final Long fileId` 供 lambda 捕获 |

**口径（写进守卫类注释，避免以后当成遗漏）**

- 纳入：对象存储（MinIO/OSS）、HTTP 客户端、LLM/RAG/向量、邮件短信、**本地磁盘写删**；
- 排除：DB 自身（那正是事务要保护的）、**Redis/缓存**（毫秒级；且 `start()` 里的滑窗初始化**有意保留在事务内**——Redis 失败就该回滚"建会话"，否则会留下"有会话无记忆"的降级态）、纯 CPU；
- 已知边界：**跨类调用链**静态守卫不解析（需跨文件符号解析），由运行时探针兜底。

**验证（红→绿 + 双向 + 灵敏度自检，不是"跑通就算"）**

- 新增 **`TransactionRemoteIoGuardTest`**（静态结构守卫，3 例）：扫描 `src/main/java` **1437 文件 / 5823 方法 / 211 个 `@Transactional` / 违规 0**，含**正向控制**断言（防"解析器失效 → 零违规假绿"）、11 个扫描器 fixture（注释内注解名、字符串字面量、控制流块、多行签名+`throws`、类级注解、Redis 排除、直接/间接命中、白名单分区）；
- **红证**：向 `src/main/java` 投放临时探针类（`@Transactional` + MinIO 直连 + 私有 helper 间接）→ 守卫报 **2 处违规**（`TxRedProofProbe.java:19` 直接、`:25` 间接），删除后恢复绿；
- 新增 **`TransactionRemoteIoRuntimeProbeTest`**（运行时探针，6 例）：用**真实** `DataSourceTransactionManager`（假 DataSource，不连库）构造真实 `TransactionTemplate`，在打桩点观测 `TransactionSynchronizationManager.isActualTransactionActive()`——三处修复各断言两次（远程 IO 处 = `false`、DB 写处 = `true`）；另**为报告点名但复核为非缺陷的两处补运行态结论**（见下）；并含**灵敏度自检**（事务块内必须观测到 `true`，否则探针等于空转）与**证伪实验**（同一段代码包进真实事务后探针必须翻转，否则断言没有鉴别力）；
- **运行时红证（生产代码级）**：把 `SysFileServiceImpl.uploadFile` 的 MinIO 调用临时改回事务内（`transactionTemplate.execute(status -> minioUtils.uploadFile(file))`，等价于修复前形态）→ 探针失败 `MinIO 上传必须发生在事务外 ==> expected: <false> but was: <true>`（`TransactionRemoteIoRuntimeProbeTest.java:173`），还原后绿。这条证明探针不是"永远 false"的摆设；
- **报告 §6.2 原表点名之外的 2 处（`PortalJobTemplateServiceImpl`、`PortalTopicServiceImpl`）判定为"非缺陷"并给出可证伪证据**：两处 LLM 调用本来就在**无 `@Transactional`** 的方法里，且全仓唯一调用方分别是 `CmsJobTemplateController#extractKeywords`、`CmsTopicController#aiGenerateTopicDraft`（均无事务），同类 `@Transactional` 方法（`bindQuestions`/`auditTopic` 等）不调用它们；新增 2 条探针断言"LLM 调用时刻无活跃事务"并各带证伪实验。**故本批真实命中是 4 处 / 9 个方法，不是 5 处**；
- 后端 `mvn -o test`：**370 例全绿**（361 → 370，本批新增 9 例：静态守卫 3 + 运行时探针 6）。

**同步**：报告 §6.2"事务内做远程 IO"行改 ✅（附录 X）+ 总览新增第 32 项；`项目开发规范` §2.7 增"事务边界硬约束"（含 Redis 口径、检测方式与探针须带灵敏度/证伪自检）；`00-项目现状总结` 开发铁律新增第 12 条；本条目。

## v13.13 (2026-09-27) 第二批②（收尾）：话题模块软删列统一 is_deleted → del_flag

> 第二批第二项的**最后一块**：v13.12 判定出的唯一真实偏差（话题 2 表），本批全链路迁移完毕，
> 并从守卫的"债务白名单"里**删掉登记**——债务清单闭环。

**改动（SQL + 实体 + Mapper + 服务 + VO + 前端，一处不留）**

| 层 | 改动 |
|---|---|
| DDL | `portal_topic_post` / `portal_topic_comment`：`is_deleted tinyint NOT NULL DEFAULT '0'` → `del_flag char(1) … DEFAULT '0'`（与全库标准写法一致：显式 collation + 同注释），列位置沿用原序 |
| 增量脚本 | 新增 `increment-sql/20260927-04-话题模块软删列统一.sql`：**幂等**（`information_schema` 前置判断 + 预处理语句，重复执行自动跳过）ADD → 数据映射（`is_deleted=1 → '2'`，否则 `'0'`）→ DROP；含复核 SQL |
| 实体 | `PortalTopicPost` / `PortalTopicComment`：删除 `isDeleted` 字段 + 实体级 `@TableLogic` 覆盖 + `delFlag` 的 `@TableField(exist=false)` 覆盖 → **回归 `BaseEntity.delFlag`**（全局 `logic-delete-field=delFlag` 负责过滤/置删），两个实体各净减 ~10 行 workaround |
| Mapper XML | 10 条手写语句：`is_deleted = 0/1` → `del_flag = '0'/'2'`（`PortalTopicCommentMapper` 8 条 + `PortalTopicPostMapper` 2 条） |
| 服务/任务 | 6 个文件 21 处：`PortalTopicPost/CommentServiceImpl`（wrapper `eq`/`set`、`getIsDeleted()==1` 判定 → `"2".equals(getDelFlag())`）、`CmsPortalUserServiceImpl`、`ReportTakedownServiceImpl`、`SensitiveScanTask` |
| VO | `TopicPostVO.isDeleted(Integer)` → **`delFlag(String)`**（API 字段随语义改名） |
| 前端 | 管理端 `cms/topic/post.vue`、`comment.vue`：`row.isDeleted` → `row.delFlag === '2'`；门户 `types/api.ts` 两处类型同步 |
| 注释 | `AiBaseEntity` 的"Phase 3 待后续窗口"改为**已完成**并说明 AI 自身仍沿用 `deleted`（有意保留）；`IReportTakedownService` / `ReportTakedownServiceImpl` / `SensitiveScanTask` 的 `is_deleted=1` 表述改为 `del_flag='2'` |

**验证**

- **dev 库执行复核**：两表 `del_flag char(1)` 默认 `'0'`、collation `utf8mb4_0900_ai_ci`、列位置不变；
  全库 `is_deleted` 列计数 **0**；**幂等复核**：脚本连跑两次均 exit 0（无报错、无副作用）
- **结构守卫**：从 `DdlConventionGuardTest` 白名单**移除话题 2 表登记**后守卫仍全绿 ——
  证明 DDL 已真正合规（不是"登记了就放过"）
- **后端**：`mvn -o test` **361** 例全绿（无新增用例，属既有回归）
- **前端**：管理端 `build:prod`、门户 `build`（含 `vue-tsc`）均通过，产物中已无 `isDeleted`

**同步**：报告 §6.5 逻辑删除行改判为 ✅、总览第 31 项更新、新增**附录 W**；`项目开发规范` §2.4 例外仅剩 AI 模块；
`00-项目现状总结` 开发铁律第 11 条同步；README 增量脚本清单补 `20260927-04`。

## v13.12 (2026-09-27) 第二批②：数据库规范统一（金额精度 + collation 归一 + DDL 约定守卫）

> 第二批（报告 §六中危组）第二项。照例先量后改：把"三套逻辑删除 / 四套 collation / 金额 precision 混用"
> 逐项量化，结论是**只有一项是真缺陷**、一项是**有意设计**、一项是**已登记债务**。

**取证与判定订正（先量化，再决定动不动）**

| §6.5 原文口径 | 实测 | 判定 |
|---|---|---|
| 金额 precision 混用（`DECIMAL(10,2)`/`(18,2)`/`(12,6)`） | 名字像金额的 decimal 列共 44 个：**12 个仍是 (10,2)**；其余 (18,2)；**5 个 AI 计费列是 6 位小数** | ✅ 12 列**宽化**；(12,6)/(10,6) 5 列**有意保留**（按 token 单价计价） |
| 逻辑删除三套（`del_flag`/`deleted`/`is_deleted`） | `del_flag` 159 表；`deleted` **11 张 AI 表**；`is_deleted` **2 张话题表**（原文的"20/2 处"是裸出现次数，非表数） | AI 的 `deleted` 是 **`AiBaseEntity` 写明的有意设计**（AI 表无 `create_by/remark/del_flag` 列 + `@TableLogic` 显式声明）→ **冻结**；话题 2 表是**自己标注的 Phase 3 债务** → 下一批迁移 |
| collation 四种并存 | 表级：0900_ai_ci 152 / general_ci 29 / unicode_ci **5** / bin 1；列级 general_ci 124 + unicode_ci 17 | ✅ 把 general_ci/unicode_ci（34 表）统一到 0900_ai_ci；`ledger_ai_analysis_report` 的 **bin 是有意设计**（`data_fingerprint`/JSON 需精确匹配，建表语句即写明）→ 保留 |

**修复（DDL + 增量脚本，不改业务代码）**

1. `init-sql/moyun-db-ddl.sql`：12 个金额列 `decimal(10,2)` → `decimal(18,2)`；34 张表的表级/列级
   `general_ci`/`unicode_ci` → `utf8mb4_0900_ai_ci`。
   **改动面已证明唯一**：把 diff 两侧把 5 个 token 归一后比对，**剩余差异为 0 行**。
2. 新增 `increment-sql/20260927-03-数据库规范统一（金额精度+collation）.sql`（12 × MODIFY + 34 × CONVERT
   + 复核 SQL），**已在 dev 库执行并复核**：
   - 表 collation：`0900_ai_ci 187 / bin 1`（原 152/29/5/1）
   - 列 collation：`0900_ai_ci 1193 / bin 9`（无 general_ci/unicode_ci 残留）
   - 金额列：12 列全部 `decimal(18,2)`；全库 `decimal(10,2)` 计数 **0**
3. `README` 增量脚本清单补该脚本。

**新增结构守卫 `DdlConventionGuardTest`（5 例）**

- **金额列**：名字像金额的 decimal 列必须 (18,2)，白名单 = 5 个 AI 计费列（含理由）；
- **软删列**：每表最多一个且必须 `del_flag`，白名单 = 11 张 AI 表（理由）+ 2 张话题表（债务理由）；
- **collation**：只允许 `utf8mb4_0900_ai_ci`，白名单 = `ledger_ai_analysis_report`（bin，理由）；
- **清单卫生**：三个白名单每条必须有理由、且**不得过期**（登记对象必须仍存在且仍处于该偏差）；
- **解析器自检**：列类型/精度解析、软删列识别、列级 collation 采集。

**红→绿验证**：注入三处扰动（`price` 退回 (10,2)、`sys_dept.del_flag` 改名 `is_deleted`、
该表 collation 换 `general_ci`）→ 守卫**三条规则同时失败**并精确点名；还原后 5/5 通过（文件已按备份恢复并复核）。

**测试**：新增 5 例，全量 **356 → 361** 例全绿（DDL/DB 变更后回归无破坏）。

**同步**：报告 §6.5 相关行改判 + 总览第 31 项 + 新增**附录 V**；`项目开发规范` §2.3.1 补"金额列精度硬约束"
（含 AI 计费例外）、§2.4 补"软删列与 collation 硬约束"；`00-项目现状总结` 开发铁律第 11 条；README 脚本清单。

## v13.11 (2026-09-27) 第二批①：模块边界/循环依赖（core 反向依赖归零 + 依赖方向守卫）

> P1 队列收口后的**第二批**（报告 §六中危组）。本批先量后改：用脚本统计全仓**跨模块 import 边**，
> 得到可执行的事实清单，再挑"方向明确错误"的边清掉，其余登记为**冻结债务**并由守卫看住。

**取证：全仓跨模块依赖边（v13.11 实测，节选）**

| 边 | 计数 | 判定 |
|---|---|---|
| `ext.cms -> portal` / `portal -> ext.cms` | 278 / 78 | ⚠️ 双向咬合（架构级改造，冻结） |
| `portal -> core` / `system -> core` | 238 / 126 | ✅ 正常方向（上层依赖基础设施） |
| `core -> system` | 13 | ⚠️ RuoYi 认证/审计层遗留（冻结 + 理由） |
| **`core -> portal`** | **3** | ❌ **反向依赖**（本批清零） |
| **`core -> ext.file`** | **2** | ❌ **反向依赖**（本批清零） |
| **`util -> portal`** | **1** | ❌ **反向依赖**（本批清零） |
| `system -> portal` | 24 | ⚠️ 管理端直连门户数据层（防腐层待建，冻结） |
| `ledger -> pay` / `vip -> pay` / `ledger -> vip` | 12 / 4 / 4 | ⚠️ 业务域咬合（冻结） |
| `common -> core` | 2 → **1** | 清掉重复类后仅剩「注解→序列化器」1 处（结构上不可消除） |

**修复（4 处，全部为"方向明确错误"）**

1. **`core → portal` 归零**：新增 `core.security.principal` 依赖倒置三件套
   （`PrincipalInfo` / `PrincipalProvider` / `PrincipalResolver`），后台 `LoginUser` 与门户
   `PortalLoginUser` 各自实现 `PrincipalProvider`；`LogAspect`、`RateLimiterAspect`
   改用 `PrincipalResolver`（不再 import 门户主体类与 `PortalSecurityUtils`）。
   —— 语义保持：门户请求的操作日志仍写 `oper_name=账号名`、`dept_name=门户用户:昵称`
   （昵称为空时不再写 "门户用户:null"）。
2. **`core → ext.file` 归零**：`core/web/common/CommonController`（映射 `/common`）移到
   `ext/file/controller/CommonController`（URL 不变）。
3. **控制器归位**：`core/sms/PortalSmsController`（映射 `/portal/sms`）移到 `portal/controller/`。
4. **`util → portal` 归零 + 死代码清理**：`util/file/ImportExportHelper` 依赖门户实体且仅 CMS 使用
   → 迁到 `ext/cms/util/`（3 处调用点 FQN 同步）；删除 `common/config/SensitiveJsonSerializer`
   （与 `core` 版逐字节重复且**零引用**的副本，注解指向 core 版）。

**新增结构守卫 `ModuleDependencyGuardTest`（5 例）**

- **硬零规则**：`core`/`util` 不得依赖 `portal`/`ext.*`/`ledger`/`pay`/`vip`；
- **冻结清单**：已登记债务按**精确计数 + 理由**锁定（`core→system` 13、`common→core` 1、
  `system→portal` 24、`portal⇄ext.cms` 78/278、`ledger→pay` 12、`vip→pay` 4、`ledger→vip` 4）——
  **计数增加即失败**（新增跨模块依赖），**减少也失败**（要求同步下调，防止清单腐烂）；
- **清单自检**：冻结边不得是硬零规则已禁止的边、每条必须写明理由；
- **扫描器自检**：只统计 `import`（编译期契约），方法体内的 FQN 不计入；`ext.cms` 按子模块归类；同模块不计。

**红→绿验证**：注入一个 `core` 包下的临时类（同时 import `portal` 实体与 `system` 实体）后，
守卫**两条规则同时失败**并给出精确信息——
`core -> portal（1 处 import）`、`core -> system：期望 13，实际 14（…理由…）`；删除后 5/5 通过。

**实测踩到的坑（已写入规范）**：**移动/删除类后不清理 `target/classes` 会导致启动失败**——
残留的旧 `CommonController.class` 与新类 bean 同名，抛
`ConflictingBeanDefinitionException: bean name 'commonController' ... conflicts`（30 个测试上下文全挂）。
`mvn clean test` 或删除旧 `.class` 即恢复。

**测试**：新增 5 例，全量 **351 → 356** 例全绿。

**同步**：报告 §6.3 表 + 总览第 30 项 + 新增**附录 U**；`项目开发规范` 新增 **§1.3.1 模块依赖方向**
（含依赖方向图、硬约束、守卫说明、编译产物清理告诫）并修正 §1.3 包结构树中的过期条目；
`00-项目现状总结` 开发铁律新增第 10 条。无 SQL/DDL、无前端变更。

## v13.10 (2026-09-27) P1 第八项（收尾）：收银台二维码两端通用渲染（服务端出图方案被依赖卡住的实证）

> P1 队列第 8 项最后一处代码可改项（报告附录 B4 的"支付二维码"行）。

**取证（先看清坏在哪）**

| 事实 | 证据 |
|---|---|
| 二维码**只在 H5 渲染** | `pages/mine/{vip,tip}/index.vue` 的 `renderQr` 整体包在 `// #ifdef H5` 内，用 `document.getElementById` + `QRCode.toCanvas(HTMLCanvasElement)` → 小程序端**从不绘制**，异常被 `catch` 吞掉，界面只剩"请使用微信扫一扫"的空框 |
| `qrcode` 走的是 Node 入口 | 包 `main` = `lib/index.js`（含 `fs`/`stream`），进小程序包占体积且无意义 |
| 服务端出图本轮不可行 | 报告建议"后端返回二维码图片 URL"，但服务端 QR 需编码器：Hutool 的 `QrCodeUtil` 依赖 **ZXing**，而 `com.google.zxing` 在本地依赖库 `D:\mvn-repository` **不存在**（已递归检索）→ 离线无法编译验证，**不擅自加依赖** |

**修复：两端同一套绘制，零新增依赖**

- 新增 `src/utils/qrcode.js`：只深引用 `qrcode` 的**纯计算**模块 `qrcode/lib/core/qrcode`
  （内部仅同级纯 JS 模块，无 `fs`/canvas），取模块矩阵后用 `uni.createCanvasContext` + `fillRect` 自行绘制
  → H5 与小程序共用同一实现；删除两处 `#ifdef H5` 与 `document` 依赖。
- 两页 `renderQr` 收敛为 `drawQrCode({ canvasId, text, instance: this })`。

**验证**

- **API 契约**（Node 侧）：`QrCore.create('weixin://wxpay/bizpayurl?pr=TEST123')` → `modules.size=29`、
  `data.length=841=29²`（与绘制循环的 `size/ count²` 假设一致）
- **小程序构建**：`build:mp-weixin` 成功；产物 `dist/build/mp-weixin/utils/qrcode.js` 存在、
  vendor 内含 qrcode 纯计算模块（`errorCorrectionLevel`/`reed-solomon`/`maskPattern`）；
  **无** `HTMLCanvasElement`/`querySelector(` 旧路径、**无** `fs`/`stream`（深引用生效）
- **H5 构建**：`build:h5` 成功（同一份代码）
- **后端**：`mvn -o test` **351** 例全绿（本轮未改后端）

**未做（如实记录，不假装完成）**

- **服务端渲染二维码图片**：需新增 ZXing 依赖（离线不可得），且需真实商户 `codeUrl` 才有意义。
- **小程序原生支付（`uni.requestPayment`）**：手机端扫自己屏幕上的二维码本就不可行，正解是
  JSAPI/小程序支付；但需微信商户号 + 后端预支付下单接口，属功能改造，**无法在本环境验证，故不擅自实现**。
- 小程序 `appid`、tabBar 图标：仍需人工提供（v13.9 起列为待人工输入，本轮未变）。

**同步**：报告附录 B4 该行改判 + 新增**附录 T** + 总览第 29 项描述更新；
`项目开发规范` 新增 **§4.1.3 uni-app 跨端硬约束**（禁止 DOM + 条件编译不得掩盖"某端未实现"）。
无 DDL、无后端代码变更。

## v13.9 (2026-09-27) P1 第八项（下半）：B4 前端资源与占位域名（构建期注入 + 死代码清理）

> P1 队列第 8 项下半（上半是 SQL `${}`，见 v13.8）。B4 共 5 行，本批处理**代码可改的 3 行**；
> 另 2 行需要人工提供资源/账号，在报告附录 B4 明确标注为"待人工输入"（不假装完成）。

**取证：把"占位域名"当缺陷逐个坐实（不是看着像问题就改）**

| 位置 | 实测结果 |
|---|---|
| `moyun-ledger-app/src/utils/request.js` | 非 H5 分支**硬编码** `http://localhost:8080`，完全忽略 `.env.production` 的 `VITE_API_BASE_URL` → 小程序生产包请求"用户设备自身"，真机必然失败 |
| `moyun-portal/src/utils/seo.ts` | `SITE_URL` 写死 `https://xulin.example.com` |
| `moyun-portal/index.html` | canonical / og:url / twitter:url / JSON-LD 共 **5 处**占位域名 |
| `moyun-portal/public/{robots.txt,sitemap.xml}` | robots 的 Sitemap 行 + sitemap 的 **12 个 `<loc>`** |
| `moyun-admin-vue` | `VITE_APP_BASE_WS='ws://127.0.0.1:8090'`（生产也写 localhost）→ 追查：**全仓无引用**，端点 `/websocket/message` 后端不存在、8090 无监听 → **死代码** |
| 后端 `application.yaml` | `moyun.portal.domain: ${PORTAL_DOMAIN:https://xulin.example.com}`，注释里还写着"同步要求：手工替换前端 robots/sitemap 的占位域名" |

**修复（4 处）**

1. **ledger**：H5 与非 H5 **统一读 `VITE_API_BASE_URL`**（删掉条件编译分支）。
2. **portal 站点域名改构建期注入**：新增 vite 插件 `moyun:site-url`（零新依赖，不改 lockfile）——
   `index.html` / `robots.txt` / `sitemap.xml` 改用 `%SITE_URL%`、`%OG_IMAGE%` 模板变量，
   构建时由 `VITE_SITE_URL` / `VITE_DEFAULT_OG_IMAGE` 注入；**production 构建缺 `VITE_SITE_URL` 直接失败**，
   并在构建结束自检产物（残留模板变量或占位域名即失败）。`seo.ts` 同步改为读环境变量 + 回退当前访问源。
3. **admin**：删除死代码 `src/utils/websocket.js` + `src/store/modules/wsdata.js`，移除两处 `VITE_APP_BASE_WS`。
4. **后端对齐**：`ConfigWiringValidator` 新增 **W-5（仅生产阻断）** 断言——`moyun.portal.domain`
   不得为占位/空域名（动态 sitemap 由后端生成）；`application.yaml` 注释改为描述新机制
   （**不再要求手工替换前端文件**）。

**验证（红→绿 + 三端构建产物核验）**

- portal **红**：无 `VITE_SITE_URL` 执行 `npm run build` → 失败并给出明确指引；
  **绿**：带域名构建成功，`dist/index.html`(canonical/og:url)、`dist/robots.txt`(Sitemap 行)、
  `dist/sitemap.xml`(loc) 均为注入域名，且产物**零残留**（`xulin.example.com` / `%SITE_URL%` 均无命中）
- ledger：`npm run build:mp-weixin` 成功；产物含 `api.example.com`、**不含** `localhost:8080`（正是原缺陷分支）
- admin：`npm run build:prod` 成功；产物无 `127.0.0.1:8090` / `websocket/message` 残留
- 后端 `mvn -o test`：**351** 例全绿（349 → 351，新增 2 例站点域名判定）

**同步**：`部署指南` §4.2 补 `VITE_SITE_URL` 必填与失败行为、后端同源要求；§5.2 记录 WS 死代码清理；
报告附录 B4 逐行改判 + 总览第 29 项；`00-项目现状总结` 开发铁律新增第 9 条（外部地址/域名禁止留占位）。

**仍待人工输入**：小程序 `appid`（微信公众平台申请）、tabBar 图标资源（设计出图）、
支付二维码改后端出图（需后端先加接口）——B4 剩余两行，见报告附录 B4。

## v13.8 (2026-09-27) P1 第八项（上半）：SQL `${}` 模板变量收敛（信任契约 + 真库实证）

> P1 队列第 8 项 = 报告的 B3「`${}` 参数化重构」+ B4 前端资源。本批做 **SQL 半边**（安全项），
> B4 前端半边留下一批。做法沿用既定纪律：**先取证复现 → 再改 → 红绿验证**。

**取证：先清点，再证伪"已经安全"的说法**

全仓 `${}` 共 **24 处**，按信任来源分三类：

| 类别 | 处数 | 原判断 | 实测结论 |
|---|---|---|---|
| `${params.dataScope}` | 9 | A-2.1 已白名单收敛，"剩余只是重构" | ⚠️ **仍可注入**：防线依赖切面被调用；`params` 是 `BaseEntity` 上的 `Map`，Spring 可绑 `?params[dataScope]=...`，**只要哪条 SQL 没经过切面**（直连 mapper / 新方法漏注解）客户端串就原样进 SQL |
| `${params.orderByColumn}` / `${params.isAsc}` | 14 | 报告列为待重构 | ✅ **本就安全**：入参都继承 `PageDomain`（`@Param("params")` 绑定的是查询对象，不是 map），其 setter 自带正则白名单 |
| `${sql}`（代码生成器建表） | 1 | — | 管理端专用，DDL 无法参数化，登记在案 |

**红（真库实测，不是推断）**：直连 `SysUserMapper.selectUserList` 并塞入
`params[dataScope] = " AND (no_such_column_zz = 1)"` → SQL 里原样出现该片段：

```
### SQL: select ... where u.del_flag = '0' AND (no_such_column_zz = 1)
### Cause: java.sql.SQLSyntaxErrorException: Unknown column 'no_such_column_zz' in 'where clause'
```

**修复：把"信任"变成显式契约（结构上只有切面能写）**

- `DataScopeAspect` 只写**受信键** `params[trustedDataScope]`（新增常量），不再直接写 `params[dataScope]`；
- 新增 `core.mybatis.SqlTemplateGuardInterceptor`：语句执行前，有受信键 → 覆盖
  `params[dataScope]`；没有 → **清空**并告警（客户端值一律丢弃）。顺带对 map 形态的
  `orderByColumn`（标识符白名单）/`isAsc`（asc/desc）做 **fail-closed** 校验；
- 注册在 `MyBatisConfig` 的 `setPlugins(mybatisPlusInterceptor, guard)` ——
  **guard 必须最外层**：`${}` 替换发生在 `MappedStatement#getBoundSql`，
  而 `InnerInterceptor#beforeQuery` 拿到的 `BoundSql` 已渲染完毕（首版就是这样：
  告警打了、值也"清"了，SQL 里载荷照旧）。

**绿**：同一真库测试通过；数据权限语义（部门 / 仅本人 / 全部 / 无权限字符）逐个验证未变。

**实测踩到的两个坑（已写进代码注释与规范）**

1. **`MapperMethod.ParamMap` 覆写了 `get()`**：取不存在的键直接抛
   `BindingException: Parameter 'x' not found` → 首版让所有 Wrapper 形态查询在**启动期**就炸；
   必须 `containsKey` 守卫后再取值。
2. **拦截时机**：见上，必须挂在 `Executor` 上做最外层插件。

**新增测试 11 例（全量 338 → 349）**

| 类 | 例 | 覆盖 |
|---|---|---|
| `SqlTemplateInjectionGuardDbTest` | 3 | 客户端 `dataScope` 载荷**不得进 SQL**（红→绿的主证据）；数据权限四态语义不变；`PageDomain` 在绑定入口拒绝注入载荷 |
| `SqlTemplateGuardInterceptorTest` | 5 | `ParamMap` 缺键不抛（启动期回归）；客户端值丢弃；受信值覆盖；嵌套 ParamMap；map 形态 orderBy 合法/非法 |
| `MapperTemplateGuardTest` | 3 | 全量 mapper XML 的 `${}` **只允许登记在册**的 24 处（新增即失败）+ 登记表无过期条目 |

**同步**：`项目开发规范` §3.3.2 新增「`${}` 模板变量的信任契约」三类口径表 + 两个坑；
报告 B3 条目改判、新增**附录 R**、总览第 28 项；`00-项目现状总结` 补规则。无 DDL、无前端变更。

## v13.7 (2026-09-27) P1 第七项：Quartz 切 JDBC 集群 JobStore（多实例实证 + 一条根因订正）

> P1 队列第 7 项，也是报告六 §四 中最后一条"部分完成"的 P0（第 12 条）。上一批（附录 A-9.3）
> 实测受阻后回退、只记录前置条件；本批把 3 条前置条件全部做完，并**用两个真实调度器节点验证**。

**前置条件①：显式 `SchedulerFactoryBean` 注入物理主库（根因修复）**

- 新增 `core.config.QuartzConfig`：`@Qualifier("masterDataSource")` + `PlatformTransactionManager`，
  `LocalDataSourceJobStore` / `isClustered=true` / `instanceId=AUTO` / `instanceName=moyunScheduler` /
  `clusterCheckinInterval=10000` / `acquireTriggersWithinLock=true`。
  根因：`LocalDataSourceJobStore` 要求 Spring 直接注入真实 DataSource，而 Spring Boot 的
  `QuartzAutoConfiguration` 不设置它，本项目 `@Primary` 又是 `dynamicDataSource` 路由数据源。
- **被实测打回的两种写法（记录在案）**：① `org.quartz.jobStore.batchTriggerAcquisitionMaxCount`
  是 **scheduler 级**属性，写成 jobStore 级直接启动失败（`No setter for property`）；
  ② 手工 `new SchedulerFactoryBean()` 只调 `afterPropertiesSet()` **不会启动调度器**
  （`start()` 属 `SmartLifecycle`，容器 refresh 时才调用）→ 探针任务一次都不跑。

**前置条件②：启动注册改幂等同步（去掉 `scheduler.clear()`）**

- 共享 JobStore 下 `clear()` 会删掉其他实例正在用的任务，且两实例同时启动会互相清掉对方刚注册的任务。
- 改为 `sys_job` ↔ JobStore 幂等对齐：缺则建（并发撞 `ObjectAlreadyExistsException` 视为已存在）、
  cron/misfire/concurrent/invokeTarget 变了才重建、一致则**不动**（按 status 幂等暂停/恢复）、
  孤儿只在"`sys_job` 出现过的组"内清理。Redis 锁串行化，但**拿不到锁也继续**（同步本身幂等，
  Quartz 自己的 `qrtz_locks` 保证单条 CRUD 原子）。

**前置条件③：多实例实证（本批核心）**

- 新增 `QuartzClusterSingleExecutionDbTest`：真库起**两个独立节点**（同库、同 `instanceName`、
  `instanceId=AUTO`、独立 `SCHED_NAME=moyunClusterProbe` 以免污染业务调度器），1 秒 cron 观察 7 秒：

  | 形态 | 7 秒内总执行次数 | 结论 |
  |---|---|---|
  | 共享 JDBC 集群 JobStore（新） | **8** | ✅ ≈1 次/秒：同一触发器只被一个节点执行 |
  | 两实例各自 RAMJobStore（负向对照 `-Dprobe.ramstore=true`） | **16** | ❌ ≈2 次/秒：复现 P0-12 重复执行 |

**根因订正**：负向对照首版用"共享 DB + `isClustered=false`"，实测 **8 次、并不翻倍**——
`qrtz_triggers.TRIGGER_STATE` 被原子置为 `ACQUIRED`，另一节点抢不到同一触发。故
**P0-12 重复执行的根因是"每个实例各自一份内存 JobStore"，不是"没开 isClustered"**；
共享 JobStore 本身即已消除重复执行，集群模式额外提供行锁、失效节点接管与死锁规避。

**环境事实（写进测试注释）**：本库 `qrtz_*` 表带**物理外键**（`qrtz_triggers → qrtz_job_details`、
`qrtz_{cron,simple,simprop,blob}_triggers → qrtz_triggers`）→ 清理必须**子表在前**，
否则 `Cannot delete or update a parent row` 并留下残留（首版即踩）。

**测试**：新增 4 例（`QuartzConfigWiringTest` 3 + `QuartzClusterSingleExecutionDbTest` 1），
全量 **338** 例通过；探针按 `SCHED_NAME` 清理并断言零残留。

**同步**：报告六 §四 第 12 条改判 ✅（P0 闭环 13 → **14** 条）、结论行更新、A-9.3 加"
已在 v13.7 完成"指针、新增**附录 Q**；`项目开发规范` §1.12 定时任务规范补集群硬约束；
`00-项目现状总结` 基础平台一节补规则；**DDL 无变更**（11 张 `qrtz_*` 表早已在 DDL 中）。

## v13.6 (2026-09-27) P1 第六项：`/portal/admin/**` 纵深防御（链顺序实证 + 端点注解守卫）

> P1 队列第 6 项。原文判断是"portal 链 `permitAll` + 门户过滤器跳过该前缀 → 保护完全依赖
> `@PreAuthorize`"。本轮**先量链归属再动手**：结论是"现状判断不成立，但风险判断成立"。

**取证（用 `FilterChainProxy` 实测，不靠读配置猜）**

| 项 | 实测结果 |
|---|---|
| 链顺序 | `getFilterChains()` = **[核心链, 门户链]**，与注册顺序一致 |
| 门户配置类上的 `@Order(1)` | **未生效**（生效的话门户链应在前面）→ 类级 `@Order` 不作用于 SecurityFilterChain bean |
| `/portal/admin/**` 匹配 | **两条链都匹配**（核心链经 `shouldApplyTo` 显式包含；门户链经 `securityMatcher("/portal/**")`）→ **核心链胜出**（含 `JwtAuthenticationTokenFilter`，解析 admin token） |
| 门户链那句 `permitAll` | **死规则**，从未生效 |

**判定订正**：不存在"保护仅靠 `@PreAuthorize`"——链级 `anyRequest().authenticated()` 一直生效
（匿名访问 401）。但**风险判断成立**：链归属是**偶然**的（靠注册顺序），顺序一翻转，
那句死规则立即变成**匿名放行后台接口**。

**修复（3 处）**

- `SecurityConfig#filterChain` 显式 `@Order(1)`；`PortalSecurityConfig#portalSecurityFilterChain`
  改 `@Order(2)`；删除门户配置类上的 `@Order(1)`（并写明它为何无效——避免后人再写一遍）。
- 门户链 `/portal/admin/**` 由 `permitAll()` 改为 **`authenticated()`**：
  **无论哪条链生效都 fail-closed**。顺序被改动的后果必须是"拒绝所有人"（响亮的失败），
  不能是"匿名可进"（无声的漏洞）。
- `PortalCategoryAdminController` 类注释原写"无需额外权限校验（登录即可访问）"，与类内两个
  `@PreAuthorize('portal:book:list')` **自相矛盾**，已按代码订正。

**新增结构守卫 `PortalAdminAuthorizationGuardTest`（5 例）**

1. `/portal/admin/**` 命中的链必须含核心 `JwtAuthenticationTokenFilter`、不得含门户过滤器（锁归属）；
2. `/portal/**` 普通路径仍由门户链处理（防误伤门户鉴权）；
3. 匿名访问 `/portal/admin/categories/list` 必须被拒（链级 fail-closed，不依赖注解）；
4. 运行期枚举 `/portal/admin/**` 下**全部真实端点**，逐个断言 `@PreAuthorize`（方法级或类级）；
5. **守卫自检**：扫到的端点数必须 ≥ 40 —— 防止"零违规"其实是"零扫描"（附录 K 的教训）。

**红→绿验证（实证了那条风险）**：篡改成"旧配置"（核心链去掉 `@Order(1)` + 门户链恢复 `permitAll`
+ 新增一个无注解后台端点）后：守卫 **3 条失败**（链归属被夺、漏注解端点被点名、匿名未被拒），
且匿名 `GET /portal/admin/redproof/ping` 实测 **HTTP 200 直达**；恢复后 5/5 通过。

**测试**：新增 5 例，全量 **334** 例通过。

**同步**：报告六新增**附录 P**（含 §6.4 该行订正 + 总览第 27 项）；`项目开发规范` §3.1 新增
「双链重叠路径的硬约束」「后台接口权限」两节、§3.2.1 补必须行为；无 SQL/DDL、无前端变更。

## v13.5 (2026-09-27) P1 第五项：异步执行器收口（4 处默认 commonPool + 裸线程/裸线程池）

> P1 队列第 5 项（承接 v13.4 的 `@Async` 自调用，同一 family：**异步执行器收口**）。
> 本轮先全仓实测点数，**订正了报告 §6.2 的两条计数**（见报告附录 O-1）：
> 不是"5 处全部无执行器"，也不是"3 处裸线程池都无关闭"。

**缺陷（P1 · 正确性/稳定性）**

- **无执行器的 `CompletableFuture.runAsync` 4 处** → 落在 `ForkJoinPool.commonPool()`
  （**全 JVM 共享、并行度 = CPU-1，且同时服务 `parallelStream()`**）：
  `DiagramChatServiceImpl:48`（`latch.await(5, MINUTES)` 长阻塞）、
  `KnowledgeBaseServiceImpl:224/364`（PDF 转换 + 切片 + 向量化，分钟级）、
  `VipServiceImpl:323`（权益统计落库，高频权益校验路径上）。
  症状是**全站并行任务一起变慢/卡住**，且日志里没有任何"线程池满"的痕迹。
- **裸 `new Thread()` 2 处**：`WorkflowController:214`（工作流流式执行）、
  `DataAnalysisController:56`（数据源元数据同步）——无命名、无队列上限、无优雅停机、并发不受控。
- **脱离容器的 `static` 单线程池**：`ContextManager:42`（会话摘要预生成）——**无界队列**，
  LLM 变慢时摘要任务可无限堆积。
- **并发写死且无优雅停机的实例池**：`VoiceInterviewServiceImpl:311`
  `Executors.newScheduledThreadPool(2)`——线程非守护、无 `@PreDestroy`，
  第 3 个并发面试回合**静默排队**（前端等不到首字，与"请求过多"不可区分）。
- **死池字段**：`ParallelNodeExecutor:26` 的 `newFixedThreadPool(10)` **全类无使用点**
  （真正的并行执行在 `WorkflowEngine`，用的是受管池）。

**修复：新增 2 个受管池 + 8 个接线点 + 1 条结构守卫**

- `core.config.AsyncTaskConfig` 新增 **`sseStreamExecutor`**（SSE 长任务：core=2/max=16、
  **queue=0 满即拒绝**、命名 `sse-stream-`、优雅停机）。拒绝策略刻意选 `AbortPolicy`：
  CallerRuns 会把 5 分钟长任务压到 Tomcat 请求线程上，比拒绝更糟。
- `ext.ai.config.AsyncConfig` 新增 **`contextSummaryExecutor`**（会话摘要：core=max=1、
  queue=200、**满则丢弃并告警**——非关键路径，下一轮滑窗超窗会重新触发）。
- 接线：架构图对话 / 工作流流式 / 语音面试逐题回合 → `sseStreamExecutor`，
  三处均显式捕获 `RejectedExecutionException` 并回**明确错误事件**（不再静默排队）；
  知识库两处 → 既有 `knowledgeProcessExecutor`（其 javadoc 早已写明就是干这个的，属漏接）；
  VIP 统计 + 元数据同步 → `applicationTaskExecutor`；摘要 → `contextSummaryExecutor`。
- **新增结构守卫 `ExecutorGovernanceGuardTest`**（`src/test/java/com/moyun/`）：全量扫描源码，
  ①`CompletableFuture.runAsync/supplyAsync` **必须显式传执行器**；
  ②`Executors.new*`/`new Thread(` **只允许白名单**（每条须写明"为什么不能池化"，
  条目失效由"白名单自检"抓出）。扫描前统一剥离注释与字符串字面量（避免文档里的历史写法误报）。
- **白名单 4 处**，均为"池化会改变语义或引入更糟后果"：`JudgeAsyncWorker`（常驻 BLPOP
  worker loop + `@PreDestroy` 生命周期完整）、`AsrStreamRelayHandler`（ASR 会话级超时守护）、
  `DistributedLockUtil`（锁看门狗续期）、`CodeExecutorService`（OJ 子进程排水线程——
  池化会让"既不输出也不退出"的子进程永久占死池线程）。

**测试**：新增 5 例（2 条全量守卫 + 白名单自检 + 2 组扫描器 fixture 自检），全量 **329** 例通过。

**环境记录（非代码问题，但会误读成回归）**：本机 Windows 服务 `Redis` 注册的二进制是
`D:\Dev_EN\Redis-x64-5.0.14.1\redis-server.exe`，而该目录已不存在（实际安装在
`D:\Dev_EN\Redis-8.6.6-Windows-x64-cygwin`）→ `Start-Service Redis` 失败（且非管理员无法启动服务）。
此时跑 `mvn -o test` 会出现 **18 个 ApplicationContext 报错**（`Unable to connect to Redis` 冒泡到
`sysDictTypeServiceImpl` 初始化），**不是代码回归**。临时办法：以当前用户直接起
`redis-server.exe redis.conf`（本批验证即如此），Redis 起来后 329 例全绿。

**同步**：报告六新增**附录 O**（含 §6.2 两条订正）；`项目开发规范` §1.12 把"禁止手写 `new Thread()`"
一条扩为完整「异步执行器硬约束」；无 SQL/DDL、无前端、无接口契约变更。

## v13.4 (2026-09-27) P1 第四项：`@Async` 同类自调用静默失效（含全量源码结构守卫）

> P1 队列第 4 项原记作"`LLMServiceImpl` 的 `@Async` 死重载"——**实际位置记错了**。
> 定位后真实情况更严重：不是一个死重载，而是**两处 `@Async` 因同类自调用而静默退化为同步**。

**缺陷（P1 · 正确性/性能，且文档与实现相反）**

- `ToolRegistry#logToolCallAsync`：标了 `@Async`，类头、方法 javadoc、`AsyncConfig`、
  `AsyncTaskConfig` 四处文档都称"异步记录、不阻塞主流程"，但它被**同一个类**的
  `executeTool` 两处直接调用 → **绕过 Spring 代理 → 一直是同步执行**（工具调用日志的
  DB 插入跑在请求线程上）。不报错、无日志，只默默变慢。
- `AiExecuteLogService#record(10 参重载)`：方法体内直接调用 11 参重载（同为同类自调用），
  且该重载**已无任何调用方**（全部调用点都传 userId）→ 死代码 + 自调用陷阱。

**修复**

- 新增 `ToolCallLogWriter`（独立 Bean，`@Async` 落在其中），`ToolRegistry` 注入后调用；
  同步清理 `ToolRegistry` 中不再使用的 `ToolCallLog`/`ToolCallLogMapper`/`@Async`/`LocalDateTime` 与字段。
  此修法与本项目既有先例一致（`AiTaskAsyncExecutor` 的类注释即为该结论）。
- 删除 `AiExecuteLogService` 的 10 参死重载（自调用陷阱随之消失；删除后全量编译通过，
  反证确无调用方）。
- 更正 `AsyncConfig` / `AsyncTaskConfig` 中指向旧类名的文档，并补"自调用会绕过代理"的显式告诫。

**新增结构守卫（防复发，本批最有价值的部分）**

- `AsyncSelfInvocationGuardTest`：静态扫描 `src/main/java` 全部源码，**任何 `@Async` 方法
  不得在其声明文件内被调用**（注释提及不算）。覆盖 `@Async` 与 `@Async("executor")` 两种写法
  （后者是最常见的带独立线程池形式，初版漏检，已修）。
- **红→绿验证**：修复前该守卫**失败**并精确列出 3 处违规
  （`ToolRegistry:245`、`AiExecuteLogService:39`、`AiExecuteLogService:52`）；修复后通过。
- 扫描器本身另有 fixture 自检（自调用 / 重载互调 / 外部调用 / 注释提及 / 带执行器名）。

**同步**

- 报告六：新增**附录 N**；总览新增第 25 项；**订正 §五订正 3 的 `@Async` 清单**
  （原列 4 处，含已删除的死重载；现为 3 处且 `ToolRegistry` → `ToolCallLogWriter`）
- `项目开发规范` §1.12 异步规范新增硬约束：`@Async` 禁止同类自调用，必须抽独立 Bean；
  命名含 `Async` 的方法必须真异步（否则改名）
- 无 SQL/DDL 变更、无前端变更

**测试**：新增 2 例（结构守卫 + 扫描器自检），全量 **324** 例通过。

## v13.3 (2026-09-27) P1 第三项：流式链路 Token 漏计（成本熔断被绕过 + 成本看板失真）

> v13 起点后 P1 队列第 3 项。先定位到**根因在依赖库**，再决定修法（不猜、按证据）。

**缺陷（P1 · 成本治理）**

- langchain4j `1.0.0-beta3` 的 `OpenAiStreamingChatModel` **既不下发
  `stream_options: {"include_usage": true}`，builder 也无该选项**
  （用 `javap` 查 builder 方法 + 检索该 jar 全部 class 常量池确认无相关字样）。
  因此 OpenAI 兼容端点的**流式回调里 `ChatResponse.tokenUsage()` 恒为 null**。
- 而网关原实现只在 `tokenUsage != null` 时才 `tokenCostGuard.consume(...)`：
  ① **流式 Token 全部漏计** → 场景日配额（成本熔断）被绕过；
  ② `ai_execute_log.token_used` 流式恒为 0 → **成本看板/报表失真**。
- 影响面正是**语音面试主干**（`executeConversationStream`，高消耗场景）；网关里那句
  "流式由 Handler 直发 emitter 无汇总——记为已知局限"即是此缺口的自述。

**修复：新增 `TokenMeter`（真实优先，缺失则本地分词估算并显式标记）**

- 规则：服务端 usage 可用 → 真实值 `estimated=false`；不可用 → 用 `OpenAiTokenizer`
  （jtokkit，已是 langchain4j-open-ai 编译期依赖）对**提示词消息 + 完整输出**本地分词，
  `estimated=true`；分词器异常 → CJK/字符粗估兜底并告警。
- 接入两条路径：**会话流式**（`onCompleteResponse` 统一计量后 consume）与**同步路径兜底**
  （Handler 未回传 usage 时估算，避免静默 0）。
- **估算值必须可区分**：`AiMetadata.tokenEstimated`（API 响应可见）+ `ai_execute_log.token_estimated`
  落库 + 日志提示；admin「AI 执行日志」Token 列/详情显示橙色「估算」标记。
- 历史键/数据无需处理：历史行 `token_estimated=0/NULL`，与新语义一致（历史值都是真实回传的）。

**同步**

- DDL：`ai_execute_log` 追加 `token_estimated`（沿用文件既有"末尾 ALTER"惯例）
- 增量脚本：`increment-sql/20260927-02-ai_execute_log-token估算标记.sql`（dev 库已执行并复核）
- admin 前端：「AI 执行日志」列表 Token 列加「估算」标签、详情说明估算来源
- 文档：报告六新增附录 M；`00-项目现状总结` AI 网关章节补 Token 计量规则

**测试（新增 8 例）**

- `TokenMeterTest`（7 例）：真实 usage（含"仅合计""仅输入"两种部分回传）→ 用真实值不标估算；
  usage 缺失 → 估算且 >0；空输入输出 → 0 不伪造；估算单调性
- `AiGatewayStreamTokenAccountingTest`（1 例，**接线回归**）：构造"服务端不回 usage"的流式模型，
  断言网关仍调用 `tokenCostGuard.consume(scene, >0)` —— 直接锁死"流式绕过日配额"这一缺陷

## v13.2 (2026-09-27) P1 第二项：语义缓存跨用户泄漏 + 记账模块遗留项收口

> 两项：① v13 起点后 P1 队列第 2 项（AI 网关语义缓存）；② 把 v13.1 明确列为"待决策/未改"的
> 记账遗留项一并做掉（用户要求"发现问题不要遗留"）。

**① 语义缓存跨用户泄漏（安全 · 潜在 P0）**

- 缺陷：缓存键为 `ai2:cache:{scene}:{md5(input)}`、语义扫描模式为 `ai2:cache:{scene}:*`，
  **都不区分用户**；而缓存存的是**完整响应体**（简历解析/优化、财务分析、面试对话等私有内容）。
  一旦某场景打开 `enable_cache`，A 用户的响应就可能被当作 B 用户的命中山返回
  —— 输入相同即精确命中，输入相似（余弦 > 0.95）即语义命中。
- 现状：`ai_scene_config` 全部 20 个场景 `enable_cache=0`，属**一枚只在管理后台点一下就会引爆的雷**。
- 修复：键与扫描模式统一改为 **`ai2:cache:u:{userId}:{scene}:...`**；**userId 为空则不查也不写**（fail-closed）；
  前缀由 `ai2:cache:` 升为 `ai2:cache:u:`，历史无隔离键不再被读取。
  将来若确有"可跨用户共享"的公共知识场景，应在 `ai_scene_config` 增显式字段放开，**不得收回用户隔离**。
- 同步：`AiGatewayService` 两处调用补传 `request.getUserId()`。
- 测试：`Ai2InfraSupportTest` 新增 3 例（跨用户不命中 / userId 空则完全不落键 / 扫描模式必须带 userId），
  原有 5 例适配新签名，共 19 例通过。

**② 记账模块遗留项收口**

- **还款进度永远 0 期（用户可见）**：App 负债卡片显示 `{paidTerms}/{totalTerms}期`，
  但 `paid_terms` **全项目无写入方**。现于还款的**同一条原子 UPDATE** 中 `paid_terms + 1`
  （`COALESCE` 兜底 null、`GREATEST(...,0)` 防负），冲正（删除）一笔还款 `−1`；借款不计期数。
  `paid_terms` 同时从"可更新列"中移除（与 `balance` 同属记账联动维护，不得由改属性接口改写）。
- **清空静默失效（用户可见）**：App 清空"每期还款额/还款日/总期数"时显式发 `null`，
  旧 `updateById`（null 则跳过）使清空不生效。两个账户更新接口改为
  **`@RequestBody Map` + 白名单显式映射**，并把 `body.keySet()`（显式出现的字段名）透传给 service：
  **可空业务列**显式提供即以传入值为准（null=清空），未提供则保持原值；NOT NULL 列维持"非 null 才更新"。
- **`refreshSnapshot` 同族抹账（低危）**：由"读整行 → `updateById`"改为**只写资产侧两列**
  （`total_asset`/`net_worth`），不再把读到的 `total_liability` 写回。
- 测试：新增 `LedgerLiabilityTermsAndProgressDbTest`（5 例，真库）覆盖上述三项。

**验证**：`mvn -o test` 全绿；无 DDL/SQL 变更、无前端变更（前端字段与接口契约保持不变）。

## v13.1 (2026-09-27) P1 第一项：记账账户"改属性"抹账（P0 资金）——修复 + 自我推翻

> v13 起点后的第一项 P1，沿用 VIP 那套流程：**先真库复现 → 再改代码 → 最后红→绿验证**。
> 过程中推翻了本报告 D-1 / §6.6 的原有结论（这是我第三次纠正自己的结论）。

**缺陷（P0 · 资金）**

- `LedgerAssetAccountServiceImpl.updateAccount` / `LedgerLiabilityAccountServiceImpl.updateAccount`
  为"读整表 → 改字段 → `updateById` 写回整表"：把**读到的** `balance`/`version` 一起写回。
- 后果①**抹账**：改账户名期间并发记账 → 余额被写回记账前旧值（流水记 +50、余额没变，账实不符）；
- 后果②**乐观锁 ABA**：`version` 被写回旧值，"读到的 version"重新可用 → 并发记账可能同时命中 `WHERE version = ?`。
- 同源问题：两处 `deleteAccount` 归档时同样写回整表。

**修复**

- `updateAccount` / `deleteAccount` 改为**列级 UPDATE**（`LambdaUpdateWrapper`）：只写业务属性列，
  永久排除 `balance`（记账联动维护）、`version`（并发控制）、`user_id`/`status`/`settle_flag`（归属与状态）；
- 未提供（null）则跳过 → **零能力回退**；归属校验下沉到 `WHERE user_id = ?`；无列可更新时短路返回；
- `@Version` + 全局乐观锁拦截器**仍然不注册**（与手写 `eq(version)` 机制冲突）。

**测试（新增 5 例，301 → 306 全绿）**

- 新增 `LedgerAccountMetaUpdateIsolationDbTest`：用 **MySQL REPEATABLE READ 一致性快照**把
  service 内部的 TOCTOU 窗口变成**确定性**复现（T1 固定 read view → T2 真实记账链路提交 → T1 改属性 → T1 内读回）；
- **红→绿验证**：临时还原旧实现后测试失败并打印"实际 balance=100.00"，恢复修复后 5/5 通过；
- 第一版测试用 `@SpyBean` 拦 mapper 实测 `fired=false`（装置失效），故改为上述快照方案 —— 教训记入报告 §十。

**同步**

- 报告六：§6.6 尾注与附录 D-1 表**订正**（原判"低危、不涉及资金、不作为待修"错误）；新增**附录 K**；
  总览新增第 21 项；§十 评审纪律新增 2 条（"写前重读≠安全"、"验证装置必须先证有效"）。
- 项目开发规范 §2.7 并发更新硬约束新增"改字段接口不得 `updateById` 写回整表"条款。
- 无 DDL/SQL 变更、无前端变更。

**待决策（未擅自改）**

- 负债"清空"`monthlyPayment`/`repaymentDay`/`totalTerms` 静默不生效：服务层无法区分"未传"与"传 null"，
  需把 Controller 入参改为 `Map`/`containsKey` 判定（接口契约调整）。
- `ledger_liability_account.paid_terms` / `due_date` 全项目**无写入方**（死列）：补写入逻辑或删列。

## v13.0 (2026-09-27) 文档基线校正 + 变更铁律确立（**后续开发起点**）

> 用户决策：**文档一律以代码为准** —— 回审代码后校正全部过期文档；自此以 v13 为起点开发，进入 P1 与后续。
> 本版**不含业务代码变更**（代码基线即 v12.3 整改后的状态），只做文档校正与规则固化。

**文档校正（逐项回审代码后修正）**

- 版本横幅统一 `v11.98` → **`v13.0`（2026-09-27）**：`README.md`、`技术架构`、`项目介绍`、`00-项目现状总结`、`开发进度与规划`、`docs/README.md`
- **AI 网关表述校正**：包名 `com.moyun.ext.ai2` → **`com.moyun.ext.aigateway`**（`ext/ai2` 实际已不存在）；
  "7/7 场景逐一手写 Handler / 全收口"作废 → 实际为**配置驱动**：路由核心 `AiSceneRegistry`（Handler Bean 注册 + `ai_scene_config` 读取，旧 `ai2_scene_registry` 已废弃）+ 通用 `DefaultSceneExecutor`，`aigateway/handler` 下仅 `AbstractAiSceneHandler`/`AiSceneHandler` 两个文件
- **SQL 路径校正**：`resources/sql/`、`cd .../sql`、DML"4 分片 `202608201435-*`"全部作废 → 实际为 `init-sql/`（`moyun-db-ddl` / `moyun-db-dml-init` / `moyun-menu-redo`）+ `increment-sql/`；README 项目结构树同步
- `README.md` 版本历史表补齐 **v12.0 / v12.1~12.2.3 / v12.3 / v13.0** 四行（原表停在 v11.98）
- `部署指南` MinIO 端口按代码统一为 **9001**；`项目开发规范` §3.10 密钥示例按代码真实键重写（v12.3 项）
- `项目开发规范` §2.10 建表自检 / §1.15 提交前自检：补"表结构变更只改 DDL、已有库另交 `increment-sql`"与下方变更管理四项

**规则固化：新增「变更铁律」（`00-项目现状总结` → 开发铁律章节，v13 起强制执行）**

1. **devlog 逐次记录**：每一次修改都必须登记（版本 + 类目 + 简介），不允许改了不记或事后补记
2. **大改动必须同步文档**：接口契约 / 配置键 / 表结构 / 状态机 / 外部通道行为 / 菜单权限 → 更新 README、部署指南、开发规范、方案文档（即四同步：代码、文档、SQL、菜单）
3. **代码是唯一事实来源**：文档与代码冲突，一律**回审代码后改文档**，绝不改代码去迁就文档；校正须在 devlog 写明
4. **提交前对照 git 提交记录自检**：`git log` / `git diff` 与 devlog 相互对照，"提交内容 / devlog / 文档"三者自洽
5. 文档只写可验证行为，不替作者断言设计意图；无法判断的列为待确认项
6. 方案文档按模块归位 `05-方案设计-分模块/`；旧版/评估/排查类文档及时删除或归档

**遗留（不阻塞本版）**

- devlog 在 **2026-09-19 ~ 09-25** 区间仍缺条目（`aigateway` 配置驱动重构、token 拆分、`portal_user` 增量脚本等），待原作者回填
- 报告六 §8.1 中属"历史报告当时结论"的条目（如报告四/五中的旧表述）保留原样，**不追改历史报告**

## v12.3 (2026-09-27) 全项目架构评审整改（三轮验证 · 附录 A~I）

> 依据：`docs/09-临时报告/报告六：全项目架构与代码评审（三轮验证合并版）.md`（含逐项证据、误判订正与方法论）。本条目只记类目，细节以该报告为准。
> 本条目由评审执行者**回溯补记**（评审期间未同步 devlog，属流程缺口，已记入报告 §8.1）。

**资金与安全（P0）**

- SQL 注入止血：`params.dataScope` 拼接面收敛（`DataScopeAspect` 改为按 `BaseEntity` 参数解析 + `SysUser/SysRole` 6 个方法补 `@DataScope`）。
- 支付回调验签可自签 → 修；提现"假打款" → fail-closed；`AesGcmUtils` 空口令由静默回落公开常量改为 fail-fast。
- 客户端可伪造 `userId` → 修；流式方法缺 `return` → 修；回调审计日志列名不匹配 → 修。
- 任意文件读取（简历解析落盘路径穿越）→ 修 + `ResumeParseDiskReadPathTest`。
- admin 端存储型 XSS 消毒；记账 App 接口修复。
- **支付渠道降级不对称**（微信 fail-closed 但代付静默降级 mock）→ 修；**短信默认落到 `MockSmsSender`** → prod 启动阻断（`ConfigWiringValidator`）。

**正确性与一致性**

- 金额口径统一：DDL 5 表 7 列 → `decimal(18,2)`（元）；同表分/元混算一并消除。
- 幂等与并发：新增 `DistributedLockUtil`（Lua 比对 owner + 看门狗）并迁移三处定时任务；`PayGatewayImpl` 并发下单保护（用锁而非唯一键——加唯一键会打坏"关单后重下单"）。
- 唯一键修正：`uk_client_uuid` → `uk_user_client (user_id, client_uuid)`；`ledger_schedule_log` → `uk_task_date`。
- **VIP 发卡**：`duration_days = NULL` 导致支付回调事务内 NPE（"钱收了、卡没发"）→ 修（前端必填 + 后台写入口校验 + 发卡明确报错）；**续费 read-modify-write 丢更新** → 改为单条原子续期 SQL + `uk_user_platform`，存量库走 `increment-sql/20260927-01`。
- Redis `INCR`+`EXPIRE` 非原子（限流器 / Token 熔断）、`ThreadLocal` 残留 → 修。
- 定时记账确定性幂等键 `sched:{taskId}:{execDate}` 补齐。

**配置接线（"被声明但零消费"）**

- 新增 `ConfigWiringValidator` 启动期断言（分必需档/可空档，prod 阻断）。
- `ImageFilter` 接线 `ImageFilterConfig` + 删除 175 行死代码；`GenConfig` 三重缺陷（static 字段 / 顶层键 / properties 语义解析 YAML）根因修复。
- 敏感凭据全部环境变量化：`MOYUN_SECURITY_CERT_NO_ENCRYPT_KEY`、`MOYUN_PAY_SECURITY_BANKCARDENCRYPTKEY`、`MOYUN_AI_API_KEY`、`MINIO_*`、`MAIL_*`；`TOKEN_ADMIN_SECRET`/`TOKEN_PORTAL_SECRET` 真正接线（此前配了不生效）。

**通道抽象与联调可用性**

- 代付做成 `PayoutChannel` 渠道抽象（与收款方向 `PayChannel` 对称）：dev 走 `MockPayoutChannel` 打通提现闭环，生产强制关闭 mock 且未接真实通道时**明确拒绝出金**。
- 邮件通道：新增 `MailChannelStatus` 统一"就绪判定"。**原三处 `mailSender == null` 判据恒假**（Spring Boot 只以 `spring.mail.host` 为条件），导致"服务端未配置"被误报成"请检查邮箱地址"；现按 host/username/password 判定，认证失败与收件人错误分开报，邮件节点不再假成功。
- 明确：**邮件 dev 与生产共用真实 SMTP，不设 mock**；短信/代付/微信支付在 dev 走 mock 且日志显式标注。

**数据模型 / 脚本**

- `moyun-db-ddl.sql` 同步上述全部索引与字段类型；新增 `increment-sql/20260927-01-vip_user_card唯一键与发卡原子化.sql`（含备份/合并/校验/ALTER）。
- `increment-sql/20260925-01`（portal_user 唯一索引 + 存量清洗）、`20260925-02`（画像扩展字段）。

**前端（三端）**

- admin：VIP 等级"有效天数"必填 + 脏数据显式标红；代付渠道展示由布尔开关改为"实际装配渠道"（mock 显示橙字"资金未实际划出"）；存储型 XSS 消毒。
- portal / ledger-app：SSE POST 流式解析、记账 App 契约修复等（见报告 §七）。

**文档（四同步）**

- `README.md`（SQL 初始化与变更惯例、环境变量清单）、`部署指南`（MinIO 端口按代码统一为 9001、邮件变量）、`项目开发规范`（§1.13.1 外部通道就绪判定硬约束 / §2.7 并发更新硬约束（含 MySQL `SET` 求值顺序与 matched-rows 坑）/ §3.10 密钥清单按代码真实键重写）、`报告六`（含对本报告自身错误的订正）。
- VIP 体系设计方案-v2 建表片段同步为 `UNIQUE INDEX uk_user_platform`。

**验证**

- 后端 `mvn -o test`：**301 例全绿 / BUILD SUCCESS**（254 → 301）；admin `vite build` 通过且产物含新增校验文案。
- 关键资金/并发改动配真库测试（`VipGrantCardDbTest` 等），MySQL 8.0.37 实测确认 `SET` 赋值顺序、`INTERVAL ?` 预处理、1062 后可继续更新等语义。

**仍需人工 / 待决策（不阻塞本版）**

- 吊销并轮换已入 git 历史的阿里云 MaaS API Key 与 163 邮箱授权码（代码侧已完成环境变量化；**凭据本身必须人工轮换**）。
- Quartz JDBC 集群：实测受阻已回退，启用前置条件见报告附录 A-9.3。
- VIP 双事实源（成长体系读 `portal_user.vip_expire_at`，购买只写 `vip_user_card`）——属业务语义，待决策。
- devlog 在 **2026-09-19 ~ 09-25** 区间仍缺条目（网关配置驱动重构、token 拆分、portal_user 增量脚本等），本条目未代写（无第一手依据）。

## v12.2.3 (2026-09-18) AI 统一入口落地——业务端直连 LLM 收编

> 前端 AI 调用全貌扫描：3 端 17 端点，8 网关 Handler + 7 直连 LLM + 2 langchain4j 设计决策。综合评分 6.2/10。
> 用户明确：admin 端系统管理（agent/工作流/大模型配置）的 AI 调用合理不改，修改范围只在实际业务端。

**P0 BUG 修复**：KnowledgeQaHandler 场景码 `"knowledge_qa"` 硬编码 → 补入 AiSceneEnum.KNOWLEDGE_QA，Handler 改枚举引用。

**业务端直连 LLM 收编（3 个场景）**：
- PortalAiController（article-meta/tags 自造 SCENES 分发）→ 新建 ArticleMetaHandler + ContentTagsHandler，Controller 改走 `aiGatewayService.execute()`
- CmsWritingPromptServiceImpl（`llmService.generate()` 直调）→ 新建 WritingPromptHandler，Service 改走 `aiGatewayService.execute()`

**基础设施**：GenericSceneData 通用场景数据类 + AiSceneJsonClient `unwrapStructured` 支持 GenericSceneData + FallbackStrategy 通用 GenericSceneData 兜底 + AiGatewayService `executeStream(request, emitter)` 重载（外部传入 emitter）

**admin 侧不动**：PromptGeneratorServiceImpl / WorkflowGeneratorServiceImpl / IntelligentAnalysisServiceImpl / SQLGeneratorServiceImpl / DiagramChatServiceImpl 保持原有 `llmService.generate()` 直调（用户指示）

**验证**：11 个改动文件括号审查全部通过（排除字符串/注释的精准审查器），LlmClient 全局零残留，admin Service LLMService 恢复确认。

## v12.2.2 (2026-09-18) LlmClient 早期自造封装安全删除

> P2 违规点收口。用户明确要求"删除前检查 agent 管理页面对话按钮是否走这里，不能盲目删除"。完整影响面排查确认：agent 对话走 DynamicChatServiceImpl（langchain4j StreamingChatLanguageModel per-agent 路由），完全不经过 LlmClient；6 个 Service 仅用 `llmClient.isEnabled()` 冗余开关（= `llmService != null`，与 aiGlobalSwitch.isEnabled() 重复），实际 AI 调用已走 aiSceneJsonClient 统一入口。删除安全。

- **6 个 Service 清理**：ResumeJobMatchService/ResumeDeepOptimizeGenerator/ResumeAiAdviceService/ResumeParseService/ResumeDeepOptimizeService 删 LlmClient 注入 + import + `&& llmClient.isEnabled()` 条件；PortalJobTemplateServiceImpl 条件改走 `aiGlobalSwitch.isEnabled()`（补注入 + import）。
- **3 文件删除**：LlmClient.java / NoopLlmClient.java / AiModuleLlmClient.java（`com.moyun.ext.cms.service` 包，早期 `@ConditionalOnProperty` 二选一 Bean 装配，已被 aiapp 网关 + AiGlobalSwitch 完全替代）。
- **6 处注释清理**：AbstractAiSceneHandler/AiGlobalSwitch/AiProperties/AiSceneResolver/ResumeAiAdviceService/VoiceInterviewServiceImpl 中 LlmClient javadoc 引用更新为 AI 统一网关描述。
- 验证：13 个改动文件括号配平（排除字符串/注释的精准审查器），LlmClient 全局零残留（grep 0 命中），3 文件确认删除（find 0 结果）。

## v12.2.1 (2026-09-18) AI 场景码枚举化收口
> 依据 v12.2 统一入口原则，将 Handler `getSceneCode()` 与业务层场景码从硬编码字符串统一改为 `AiSceneEnum.XXX.getCode()`，场景码唯一权威来源为 `com.moyun.ext.ai.enums.AiSceneEnum` 枚举。

- **7 个 Handler**：`ResumeParseHandler`/`ResumeOptimizeHandler`/`QuestionGenerateHandler`/`SensitiveWordHandler`/`FinanceAnalysisHandler`/`DailyTopicHandler`/`VoiceInterviewHandler` 的 `getSceneCode()` 改为枚举引用。
- **12 个业务类**：`ScoringEngine`/`InterviewAgentClientImpl`/`ResumeParseService`/`ResumeDeepOptimizeService`/`ResumeJobMatchService`/`ResumeAiAdviceService`/`ResumeDeepOptimizeGenerator`/`PortalJobTemplateServiceImpl`/`LedgerAiAnalysisServiceImpl` 等场景码常量定义改为 `AiSceneEnum.XXX.getCode()`；`AiSafetyController`/`PortalTopicServiceImpl` 直接调用改为枚举。
- **FallbackStrategy**：`switch case "voice_interview"/"sensitive_word"` 重构为 `AiSceneEnum.of(scene)` 枚举比较（case 标签不能用枚举.getCode() 非编译期常量）。
- **IntentClassifier**：`"voice_interview".equals(scene)` 改为 `AiSceneEnum.VOICE_INTERVIEW.getCode().equals(scene)`。
- 不改：`ResumeParseTaskHandler.TASK_TYPE`（任务类型标识非场景码）、`@VipOnly(benefit="resume_optimize")`（VIP 权益标识非场景码）。
- 验证：20 个文件 Java 静态审查通过（括号配平/import 完整/枚举引用正确）。

## v12.2 (2026-09-18) AI 调度层语义化 + 统一入口原则确立
> 背景：原 `com.moyun.ext.ai2`（36 文件统一调度层）包名无语义，且散落调用（FinanceAnalysisHandler 在 ledger 包、面试主干 InterviewAgentClient 直连 langchain4j 绕过网关、PortalAiController/CmsWritingPrompt 自造 LLM 调用）。依据《ai/ai2 包整合评估》用户决策方案 A 整改。

**统一入口原则（本版确立，后续所有 AI 调用遵守）：**
1. 业务层只负责包装上下文（构造 input Map），不直接调 LLM 大模型（LLMService/langchain4j）
2. 每个业务加一个场景 Handler（收口在 `com.moyun.ext.aiapp.handler.impl`），Handler 内做数据组装 + LLM 变换 + 降级
3. 业务调用方绑定 `sceneCode`（+ 可选 agentId），走 `AiGatewayService.execute(AiExecuteRequest)` 统一入口
4. 治理（限流/Token 熔断/语义缓存/意图分类/注入防护/输出过滤/执行日志/降级）由网关统一施加，业务无感

**本版改动：** ① `com.moyun.ext.ai2` → `com.moyun.ext.aiapp`（36 文件目录重命名 + 51 文件 import 全局替换，零残留）。② FinanceAnalysisHandler 从 `ledger.handler` 收编到 `aiapp.handler.impl`（散落 Handler 统一归口）。③ 违规直连整改（PortalAiController 自造 SCENES 分发废弃改走网关 / CmsWritingPrompt 直调 LLMService 改走网关 / AiModuleLlmClient 早期自造封装标注 @Deprecated 过渡）。④ 职责重叠清理：AiSceneJsonClient 定位为"业务方外部便捷入口"、AbstractAiSceneHandler.chatJson 为"Handler 内部方法"，注释明确边界。⑤ 面试主干 InterviewAgentClient 的 langchain4j per-agent 流式直连，因 AiGatewayService 暂不支持流式，本版保留但强制经 aiapp 治理前置检查（灰度开关缺省改 true），统一流式改造留待 AiGatewayService 增加流式 execute 后收口（见《AI 统一入口整改方案》）。

## v12.1 (2026-09-18) Mapper SQL 注解统一外置 XML：消除手写 SQL 注入面（TD-04）
> 依据：《项目评审 TD-04》整改。① 扫描 178 个 Mapper 接口，定位 50 个含 @Select/@Insert/@Update/@Delete 内联 SQL 注解的 Mapper（共 200 个注解方法），全部迁移到 resources/mapper 下按 DAO 包镜像分包的 XML：新建 mapper/ext/ai（7）/ext/cms（1）/pay（1）/vip（2）4 个新包 + portal 追加/新建 16 个 + system 追加 SysLogininfor 1 个。② 迁移保证功能一致：SQL 拼接合并还原、`<script>` 剥壳转 XML 原生 `<if>` 动态标签、`<`/`<>` 比较运算符按 XML 规范转义 `&lt;`、resultType 全限定类名（pay/vip 不在 type-aliases 的用全路径）、namespace=接口全限定名、id=方法名一一对应。③ Java 接口清理：删除 200 个 SQL 注解保留方法签名、清理不再使用的 Select/Insert/Update/Delete import（@Mapper/@Param 保留）。④ 验证：178 个 Mapper 静态审查通过（括号配平/零注解残留/无孤儿 import）；107 个 XML 全部 ET.parse 通过；namespace/id 一致性校验本次迁移 0 错误。

## v12.0 (2026-09-17) 统一 VIP 体系：端级粒度 + 全局公共端 + 注解驱动（替代三套旧 VIP）
> 依据：《VIP 体系完整设计方案 v2.1》评审通过实施。① SQL：新增 sys_platform 端定义（4 端）+ vip_tier/vip_benefit/vip_tier_benefit/vip_user_card/vip_benefit_usage/vip_api_registry 七表及门户 4 等级 6 权益/记账 2 等级 2 权益初始化；sys_config 补 platform_code 端级列 + vip.enabled 全局开关（缺省 false）；pay_ledger_entry 补 platform 列；DROP 8 张旧表（三套套餐/订单 + portal_free_trial + 预留表）+ 旧 bizType 订单清理 + 菜单删旧建新（5500-5519）。② 后端：新增 com.moyun.vip 包（@VipOnly 注解 / VipApiScanner 启动扫描 / VipOnlyAspect 切面 / VipServiceImpl 次数消耗 Redis 原子计数+DB 降级 / VipPayCallbackHandler 发卡续费顺延 / 门户与记账订阅 Controller）；admin 端 SysPlatformController + VipAdminController 六资源管理；付费点 @VipOnly 化（语音面试/简历深度优化/记账 AI 分析）；删除三套旧 VIP 全链路 31 文件；收入总览与收入订单改 pay_order biz_type='vip' 口径。③ 门户前端：新会员页 /membership（等级+权益清单+用量+收银台）替代旧两订阅页；付费点前置校验改统一会员状态（free 档免费额度）。④ admin 前端：VIP 管理模块六页（等级/权益/矩阵/接口注册/会员卡/使用统计）+ 端管理页，删旧四页。⑤ 记账 App 前端零改动（后端契约兼容重写）。全量编译/build 验证通过。

## v11.99 (2026-09-17) 项目瘦身收尾：文档体系重构（总-分-总）+ 版本注释压缩 + 死代码清除
> 类目：① 文档：新增《00-项目现状总结》（总-分-总基线）；devlog 440KB→37KB（只留类目+简介）；删除 16 份阶段性评估/排查/自测/已覆盖旧方案文档；架构/项目介绍/规划文档刷新至 v11.98；五端 README（根+四子项目）全面更新对齐。② 代码注释：清除版本更新记录式注释（265 文件/654 行，保留方案文档锚点引用）。③ 死代码：删除后端无引用且三前端零调用的 Controller/Service（26 文件级 + 34 方法级，shop/task/tip/settlement/reportGeneration 等死链），全路径 mvn 编译验证通过。菜单无孤儿（旧交易管理菜单 v11.1 已清理）；portal_creator_settlement 表保留（DDL 不回退）。

## v11.98 (2026-09-17) AI 网关链路根治：不可变 Map 击穿输入清洗 + 运行时开关全面 sys_config 化
> 需求：v11.97 排查实锤逐题 LLM 分析 27/29 条 fail（error_msg="not supported"）；用户指令：去掉 yaml 里的开关改 sys_config 全局热配置，全链路审查网关代码保证链路正确、参数拼接有效（前端设置正确拼进上下文，agent/大模型拿到正确参数）。

## v11.97 (2026-09-16) 语音面试报告：整场 LLM 复盘（基于简历+对话内容）+报告页重设计+重新生成链路
> 需求：用户实测 v11.96 后反馈报告质量差——所有题 50 分、亮点"暂无数据"、改进建议复述问题原文、技能标签显示原始 JSON、岗位匹配度=总分硬套、文案模板化无参考意义；要求报告基于简历和对话内容生成，优化报告输出质量与页面排版样式，删除关联题库（knowledgePoints）。

## v11.96 (2026-09-16) 语音面试：时长制改造（20分钟倒计时）+报告生成链路P0竞态修复+历史页异步进度可见
> 需求：用户实测反馈两大问题——①面试突然结束不受控（问满题数即 finished，一句话没说完就被切）；要求改为时长制：默认 20 分钟倒计时（sys_config 可配），期间只有用户主动结束（点击按钮/口头提出）才结束，倒计时归零自动保存+触发总结报告。②报告自版本修改后从未生成过：要求全链路检查不遗漏，历史页可见生成进度（轮询），成功后展示完整报告，注意数据保存与后台异步任务一致性。

## v11.95 (2026-09-16) AI能力架构收口：场景提示词废弃+default_chat治理+面试主干网关化灰度+JSON Mode升级
> 需求：v11.95 既定四项架构收口——①场景表系统提示词彻底废弃（用户裁决"场景表的系统提示词就不要了"：人设统一走 `ai_agent.system_prompt`，场景表只留治理配置）；②default_chat 治理（聊天链路此前无限流/无执行日志）；③语音面试主干网关化灰度（T2 生产方案：直连保性能、治理收口网关，灰度键缺省关闭）；④模型表 JSON Mode（结构化场景下发原生 `response_format`，解析失败降级 Prompt 约束重试——修复 deep_optimize 报"AI 服务暂不可用"类解析失败）。多 task 统一（第5项）经评估现状已达成，零改动。

## v11.94.1 (2026-09-16) 语音面试：移除反问段独立链路（融入对话流）+思考中占位气泡
> 需求：人工实测 v11.94 后裁决——反问段独立输入模式体验不佳（面试未结束时跳出反问输入框打断节奏，属不完整且不需要的功能），整体移除；反问改为系统提示词承载：候选人想问的可在回答中自然表达，AI 面试官简答后继续提问；问满后面试官口播"你还有什么想了解的吗？"，无反问或反问完毕即收尾致谢。（v11.95 保留给既定范围：场景表字段废弃标注/default_chat 治理/主干网关化灰度/JSON Schema 升级）

## v11.94 (2026-09-16) AI能力架构v4整合：warmup一次调用+RAG预热注入+P0分数断链修复+四段式段序+候选人反问段
> 需求：按《AI 能力架构完整设计文档_v4》落地 v4 整合第一批——①P0 修复报告分数断链（runBatchAnalysis 只写 scoreDraft 不写 score，聚合跳过 score=null 的题导致全空报告）；②面试官系统提示词增加段序约束（第1问固定自我介绍+2-3问深挖）；③warmup 预热一次 LLM 调用产出"AI理解"（候选人画像+考察方向计划+开场白+首题）注入滑窗常驻；④RAG 预热接入（start 时检索 agent 绑定知识库 top-5 片段注入，运行时零检索保持流式低延迟）；⑤候选人反问段（askPhase 协议 + POST /{id}/ask，sys_config 开关）；⑥知识点归纳 RAG 化（报告阶段二次检索）；⑦speak_text 语义重定义（question=结构化问题文本，speakText=口语化话术，TTS 优先播、空则 fallback）。

## v11.93 (2026-09-16) AI语音面试 V3：统一AI入口纯Agent自由面试——滑窗记忆+流式话术+协议补全+死代码彻底删减
> 需求：彻底切换统一 AI 入口模式（参考 /ai/chat 会话实现）——删除题单预生成/规则决策/围栏模式等堆叠代码；话术流式直出消除"回答后等分析"；滑窗记忆消除重复提问与提示词重复拼接；前端配置收口。含一项关键协议修复：V3 首版 runAgentTurn 只发 delta/end 不建下一题、前端仍在等旧 data 事件，第二问会覆盖同一 QA 行。

## v11.92 (2026-09-15) AI语音面试页布局修复：桌面三栏锁视口，消除双重滚动
> 需求：检查并修复面试页布局。核心问题：对话区 `.chat-body` 高度硬编码 `max-height: calc(100vh - 260px)`（magic number 未计入主区 padding/顶栏/聆听音浪高度），且三栏 grid 默认 stretch 无高度约束——题目多时左栏撑高整行，整页滚动 + 对话区内部滚动叠加，聆听音浪出现时输入区被挤出视口。

## v11.91 (2026-09-15) AI语音面试 V2 收口：JD输入+噪声检测+5步准备进度+结束触发点+报告三段式
> 需求：按《V2 强化版》文档补齐差距——①准备页核心只留岗位+JD+简历，其余配置收进高级设置折叠；②设备检测补环境噪声检测（嘈杂黄色提示）；③点击开始面试展示 5 步准备进度条（简历画像→岗位要求→会话上下文→题单→环境），完成后自动进入面试页；④结束触发点补连续跳过 3 题与 5 分钟无响应（均弹确认，确保分析流程收口）；⑤报告页三段式（第一栏面试者简介/第二栏岗位信息/第三栏 Tab），概要聚焦结论（缺点移入问题分析 Tab + 等级徽章 + 优势 Top3 + 详细分析入口）。

## v11.90 (2026-09-15) AI语音面试全链路重构 V2：快链路即问即答 + 异步批量分析报告（化繁为简）
> 需求：按《AI 面试全链路重构 · 完整实施文档（V2 · 强化版）》全面重构语音面试的 UI、数据流与调度时机。核心矛盾：原链路每轮答题同步等 LLM（评分+话术+分析全串行），答完到下一题延迟数秒，且实时评分雷达/分析气泡打断沉浸感。V2 方案参考已上线的 AI 会话模块（/ai/chat/index + /cms/ai/chat/stream 的 fetchStream 流式模式）："化繁为简"——对话进行中零 LLM 阻塞，深度分析全部后置到报告。

## v11.89 (2026-09-15) AI语音面试：简历收起面板 + 评分标准图例 + 页尾补齐 + 退出有始有终
> 需求：①简历选择改可收起/展开按钮；②页面补站点页尾与其他页一致；③"实时维度分析"几个维度词看不懂，本质是评分标准需逐维说明；④面试中临时退出要确认，关闭标签页/结束面试必须释放麦克风与播放器连接。

## v11.88 (2026-09-15) AI语音面试准备页 UI 重构：紧凑下拉化 + 全站主题跟随（含 v11.87 事故重建）
> 需求：用户反馈 /interview/voice 准备页岗位/简历模块占空间过大、颜色不跟随站点主题（?theme=dark 下仍是白底红字）。本次将岗位与面试配置合并为紧凑下拉网格、简历改下拉选择、页面配色全部映射站点 --theme-* 变量（light/dark/eye 三主题实时跟随）。

## v11.86 (2026-09-15) 记账App AI分析任务轮询降频（2s → 5s）
> 需求：用户反馈 /portal/ledger/ai/analysis/task/{taskId} 轮询频率过高，希望间隔加长。

## v11.85 (2026-09-15) 会员付费点免费体验 2 次（简历深度优化 + 语音面试）+ 面试会员付费点落地
> 需求：用户要求"简历和面试都要免费体验 2 次"。两个会员付费点统一体验机制：非会员每场景可免费体验 2 次（portal_free_trial 按场景原子消耗），用完返回 402 引导开通；会员不限次。顺带落地面试会员（v11.82 骨架预留）的付费功能点：语音面试 start 接口。

## v11.84 (2026-09-15) 修复三个VIP订阅回调首单支付 NPE（mock 支付验证暴露）
> 现象：/portal/pay/mock/{payNo} 触发回调时 `LedgerVipPayCallbackHandler` 抛 NullPointerException: Cannot invoke "Map.get(Object)" because "m" is null。

## v11.83 (2026-09-15) 简历优化会员接入公共支付通道（8.3 规范第三个平台直收渠道落地，骨架）
> 需求：用户确认接入简历优化场景。选取墨韵门户「简历优化工作台」深度优化（AI 逐项建议/前后对比/采纳保存）作为付费点，会员制解锁：与面试会员同构：用户 → 平台公账全额，无第三方收款人，不产生用户钱包余额；独立业务表/bizType=resume_optimize/独立回调处理器，复用门户通用收银台。岗位匹配评分、AI 实时辅助编辑等基础功能保持免费。

## v11.82 (2026-09-15) 面试会员订阅接入公共支付通道（8.3 规范第二个平台直收渠道落地，骨架）
> 需求：用户确认继续接入下一个平台直收类场景。选取墨韵门户「面试会员」（语音面试/深度报告/题库权益订阅），与记账VIP 同构：用户 → 平台公账全额，无第三方收款人，不产生用户钱包余额；独立业务表/bizType=interview_vip/独立回调处理器，复用门户通用收银台。

## v11.81 (2026-09-14) 记账VIP订阅接入公共支付通道（8.3 规范首个新渠道落地，骨架）
> 需求：用户确认按"账户模型分类"方案继续收入管理任务，本次接入记账VIP（只搭骨架：通道/订单/权益发放闭环完整，套餐价格后台可配）。为平台直收类首个订阅场景，与打赏/付费阅读（分账类）资金流区分：用户 → 平台公账全额，不产生用户钱包余额。

## v11.80 (2026-09-14) 记账App打赏接入公共支付通道（补齐 v11.79 标准检查发现的半成品链路）
> 需求：按 v11.79 全平台支付标准检查记账App打赏功能完整性。检查结论：枚举/单位/聚合已达标，但资金链路为演示级——直落 paid、无公共通道单据（pay_no 恒空）、无分账记账、无 clientUuid 幂等、无 scale≤2 校验、上限 999999 与标准不一致、用户侧无赞赏历史。用户确认按"完整接公共通道"补齐，文档与脚本链路注释同步按公共通道实现。

## v11.79 (2026-09-14) 全平台支付统一标准：单钱包 + 公账记账 + 提现闭环（支付管理→收入管理）
> 需求依据：docs/05-方案设计-分模块/07-支付模块/支付问题以及解决方案v-2.md。用户指出双钱包并存（社区钱包 portal_wallet 充值/消费 vs 打赏钱包 pay_user_account）不合理，并提议公账模型：用户支付→平台公账商户号 A→数据分账（虚拟余额展示于门户用户与后台）→提现时由 A 出金到目标账户；同时要求全平台支付统一枚举/字段标准（不考虑历史数据，业务逻辑直接改）。

## v11.78 (2026-09-14) 支付管理重定位为收入管理：收入总览（平台×渠道）+ 分账流水昵称关联
> 需求：三件事——1) App 赞赏页去掉 ¥100 快捷金额（过大）；2) `/pay/ledger` 关联用户昵称、金额单位确认元口径（前后台一致，无换算）；3) 支付管理菜单整体优化为全平台支付汇集的收入管理模块，按 平台（记账App/墨韵门户）→ 渠道（App记账打赏/门户文章打赏/付费阅读/面试会员/简历优化等）两级划分。

## v11.77 (2026-09-14) 赞赏页体验优化：快捷金额单选 + 打赏/反馈联合提示语
> 需求：赞赏页（App `pages/mine/tip`）增加快捷金额单选按钮（2/5/10/20/50/100 元 + 其他自定义输入）；提示语改为"创作不易，感谢打赏鼓励"并联动投诉建议入口（"提出你的宝贵意见，与平台大家共同成长"→ 跳转意见反馈页）。其他收益变现点（VIP/广告等）本期暂不考虑，原"累计 9.9 元免广告"提示移除。

## v11.76 (2026-09-14) 分类管理强化：全类型筛选/名称搜索/归属标识 + 删除流水绑定校验 + 记一笔自定义分类
> 需求：后台 `/ledger/category` 类型下拉只有收入/支出且筛选无效，需支持全大类筛选、名称搜索、归属（系统/自定义）标识；删除分类必须校验流水绑定，有则不能删除；表格明确二级结构（大类=收入/支出/转账/借款/还款/校准）并移除"新增子分类"按钮。App 记一笔页去掉分类"选填"，支持在当前大类下新增自定义分类（仅自己可见，长按可删，删除同样校验流水绑定）。

## v11.75 (2026-09-14) 记账运营统计重定位：模块使用 + AI/Token 成本 + 收益现状 + 门户引导入口
> 需求：`/ledger/stats` 页面原内容单薄定位不清；扩展为运营总览（模块用户使用情况、token 消耗、收益现状与轻变现规划），并在用户端增加链接引导使用墨韵门户平台（生态互导，零成本变现起点）。评估结论：轻变现 = VIP 增值（免费功能全保留）+ 激励广告（仅主动解锁场景）+ 门户互导，禁止开屏/插页广告伤害用户认知。

## v11.74 (2026-09-14) 记账预设分类多级化：后台树形管理（展开/收起 + 父分类校验）
> 需求：后台 `/ledger/category` 分类管理支持多级分类，按类型查询、树形展开/收起。基于既有 `parent_id` 字段（DB 已支持，无 SQL 变更）实现两级分类，后端强校验 + 前端树形表格双端落地。

## v11.70 (2026-09-11) 资金链路测试补齐：打赏双链路 22 用例（P1-8，附充值/提现范围澄清）
> 依据《AI底座企业级评估-代码实测结论与改进清单》最后一项 P1：资金链路零测试。**实测澄清**：充值/提现业务逻辑尚未实现（PortalWalletServiceImpl 仅 CRUD，全库无 recharge/withdraw 业务方法，PayCallbackHandler 实现仅 tip 一种）——资金链路现状 = 打赏（积分打赏 + 微信支付 + 回调复式分账），无从测试的部分属功能范围澄清而非测试缺口。纯测试增量，零生产代码改动。维度 9 → 4.4，综合 → 3.84。

## v11.73 (2026-09-14) 记账后台管理建设：用户维度管理（脱敏）+ 小程序功能可视化配置 + AI 消费用户归属
> 需求：后台按用户维度展示流水与使用情况（AI 分析/token 消费，隐私保护）；小程序"我的"页功能入口开发中不展示、改为后台可视化配置。

## v11.72 (2026-09-14) AI 分析报告四维度快照 + 查询优先语义 + 资产负债视觉区分
> 需求：1) portfolio 页资产/负债元素加样式区别；2) ledger_ai_analysis_report 同月多份冗余、无范围字段、切 tab 数据不变、进入页面隐式触发分析。

## v11.71 (2026-09-14) 记账模块六类型全面审查：幂等防重 + 金额边界 + SQL 聚合三处企业级加固
> 承 v11.70 对记账六类型（支出/收入/转账/还款/借款/校准）做全量代码审查，确认既有架构（单事务/联动矩阵/冲正重放/乐观锁/归属校验）符合企业标准，修复三处真问题。

## v11.70 (2026-09-14) 记账资金链校验：支出/转出账户余额不足强制阻断（账目可信度铁律）
> 需求：记支出时若所选账户余额不足，必须先记一笔收入或转账说明资金来源——否则支出凭空出现，"钱从哪来"说不清楚，账目无可信度。设计上对齐既有还款类型的余额校验思路（v 前已有），并将严格度提升为**双端强制阻断**。

## v11.69 (2026-09-11) sys_file 系统文件管理菜单补齐：后台管理入口上线（附图片预览 bug 修复）
> 现状实测：sys_file 表、后端 SysFileController（/system/file，list/{id}/upload/byUrl/{ids}/storage/*，@PreAuthorize system:file:*）、前端页面（views/system/file/index.vue：搜索/上传/批量删除/预览/下载/分页）与 API（api/system/file.js）均已存在——**唯一缺口是 sys_menu 无菜单记录，管理端无入口可见**。

## v11.68 (2026-09-11) 功能闭环自测整改：三大模块五环补全 + 跨模块四数据流全通（P0/P1/P2 闭环清单清零）
> 依据《20260911-功能闭环自测文档》：三大模块（面试/练习/简历优化）"五环"各断一环（再入口），跨模块四个数据流断三个，硬断链 5 项 + 软断链 3 项。本版实测修正 2 项误判、真修复 5 项，四数据流全通。

## v11.67 (2026-09-11) P1-6 异步任务收敛：双轨制选型规则 + 孤儿任务恢复（附老包存量测试修复）
> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-6："三套异步任务模式并存"实测修正——JudgeAsyncWorker 系 OJ 代码判题专属基础设施（非 LLM 任务），误计入 AI 债；其余两套为合理双轨制，收敛策略为选型规则文档化而非强行统一抽象。维度 9 → 4.3，综合 → 3.83。

## v11.66 (2026-09-11) P1-5 output_parser/output_mode 配置接线：从"可编辑零消费"到消费闭环
> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-5：ai_scene_config.output_parser/output_mode 两字段此前配置可编辑零消费（管理页改了没效果，与实际脱节）。本版接线后配置即刻生效。维度 6 → 4.5，综合 → 3.82。

## v11.65 (2026-09-11) 知识问答接入统一网关：knowledge_qa 场景（Agent 驱动多路召回 + 引用溯源透出）
> 承接 v11.64 P1-4 实测修正的后续动作：知识问答从 admin chat 专属收编进统一网关（ai2），通过 Agent 实现检索与多路召回，C 端/开放入口可消费。网关场景数 7 → 8，维度 4 → 4.6、维度 6 → 4.4，综合 3.80 → 3.81。

## v11.64 (2026-09-11) P1-4 引用溯源：实测修正——完整链路已实装，评估原判"仍缺"系误判，零代码改动
> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-4（原判：检索结果无来源文档/片段/相似度透出到消费端）。逐层调研后确认与 P1-2 同类的**评估误判**：引用溯源完整链路自 2025-12-11 已实装（早于评估文档 2026-09-09 成文），评估时未追到 chat 消费链。本版仅修正评估文档（维度 4 → 4.5，综合 → 3.80），**无代码/SQL/前端改动**。

## v11.63 (2026-09-11) P1-2 工具参数 JSON Schema 校验：执行链从"裸奔"到执行前收口
> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-2。**实测修正**：调研发现 ai_agent_tool.parameters 种子数据本就是合法 JSON Schema（`{"type":"object","required":[...],"properties":{...}}`，与内置工具 getParametersSchema() 同构），原评估判"文本描述"系误判——真正缺口是**执行链零校验**：LLM 生成的参数类型错误（如 days 传字符串 "3"）直接打入执行器，轻则执行失败重则 ClassCastException。本版补齐校验闭环，维度 3 工具调用 3.0 → 3.2，综合 3.77 → 3.78。

## v11.62 (2026-09-11) P1-3 输出内容过滤：网关复用 DFA 词树脱敏，安全三件套（注入/熔断/输出过滤）齐备
> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-3：LLM 返回直出客户端无敏感词过滤。本版复用 `SensitiveWordFilter`（sys_sensitive_word 词库，DFA O(n)）为网关输出过滤钩子，不另建检测体系。至此维度 8 安全治理的三个 P1 前缺口全部落地，评分 2.5 → 4.0。

## v11.61 (2026-09-11) P1-1 AI 执行日志管理页：统一网关日志从"数据在采"到"有人看"
> 依据《AI底座企业级评估-代码实测结论与改进清单》P1-1：ai_execute_log 后端全字段落库（v11.57 起 cost_yuan 成本回填），但无管理端查询页——数据在采没人看。本版补齐查询/汇总/详情/清理闭环，是收口 7/7（v11.59）后网关可观测性的最后一块拼图。

## v11.60 (2026-09-11) P0-4 新链路测试覆盖：评分引擎/财务指标护栏/网关基础设施 37 用例，揪出 2 个真 bug + 修复 mvn PATH 劫持导致的假执行
> 依据《AI底座企业级评估-代码实测结论与改进清单》P0-4：AnswerScoringEngine（v11.47 拆出）零测试、FinanceAnalysisHandler 数值护栏零测试、FallbackStrategy/SceneRateLimiter/SemanticCache 零测试。本版补齐 37 用例并全部转绿，**P0 级四项（注入防护/成本熔断/场景收口/测试覆盖）全部完成**。

## v11.59 (2026-09-11) P0-3 场景业务收口全部完成：resume/question + voice_interview 切统一网关（7/7）
> 依据《AI底座企业级评估-代码实测结论与改进清单》P0-3：v11.55 实测业务收口仅 1/7（全库唯一网关调用点=财务分析），其余 6 场景业务绕行网关直调 LLM，无执行日志/成本核算/限流/注入防护。v11.57 先行收口 sensitive_word/daily_topic；本版完成剩余 4 场景，**收口 7/7，全库 `llmClient.chat(` 清零**。

## v11.58 (2026-09-11) CMS 栏目管理：修复存量三级栏目无法编辑的问题
> 用户反馈：编辑栏目只想修改路由路径（`/reading/space/quotes` → `/reading/quotes`），却被"最多只支持两级栏目，不能设置为三级"拦截。

## v11.57 (2026-09-11) P0-1 注入防护 + P0-2 成本熔断落地（评估整改）
> 

## v11.56 (2026-09-11) AI 底座企业级评估·代码实测结论文档
> 

## v11.55 (2026-09-11) 财务分析异步任务化 + 报告多版本
> 

## v11.54 (2026-09-11) 财务分析"LLM 无返回"根因修复：空响应静默分支 + 模型超时
> 

## v11.53.1 (2026-09-10) 死代码清理：基类孤儿方法 parseJson(String, Class)
> 

## v11.48 (2026-09-10) finance_analysis 场景标准化：风险/建议完全交 LLM + 配置即时生效
> 

## v11.53 (2026-09-10) 财务分析 LLM 链路修复：Agent 人设带偏防呆 + 文本兜底 + Agent 参数调优
> 

## v11.52 (2026-09-10) 入参契约语义分层：userInput 提升为顶层显式字段
> 

## v11.51 (2026-09-10) AI 底座闭环补强：可观测性（metadata）+ 通用入口白名单（open_api）
> 

## v11.50 (2026-09-10) finance_analysis 架构重构：数据组装下沉 Handler，Service 薄化
> 

## v11.49.1 (2026-09-10) 修复 ai_execute_log 落库失败：实体列名与 DDL 不符
> 

## v11.49 (2026-09-10) finance_analysis 深度增强：趋势/预算/应急基金上下文 + evidence/expectedImpact 契约 + Agent 人设打通
> 

## v11.47 (2026-09-10) 上帝类拆分第一步：AnswerScoringEngine 抽取
> 

## v11.46 (2026-09-10) 死代码清理第一轮 + 评审结论入规划
> 

## v11.45 (2026-09-10) 规划路线文档更新至 v11.44 现状
> 

## v11.44 (2026-09-10) docs 目录阶段性规整
> 

## v11.43 (2026-09-09) 财务分析接入 AI 统一网关 + 场景管理页下拉/注册表修复
> 

## v11.41 (2026-09-09) AI 统一接入层：ai_scene_config 增量加列 + ai_execute_log 日志表
> 

## v11.40 (2026-09-08) AI 分析快照数据指纹自动失效 + 借款自动建户欠款双倍修复
> 

## v11.39 (2026-09-08) LLM 统一入口：scene_code 全链路接入（设计模式落地）
> 

## v11.38 (2026-09-08) AI 场景注册表（AiSceneEnum）
> 

## v11.37 (2026-09-08) AI 分析多维度切换 + 收入占比修复
> 

## v11.36 (2026-09-08) AI 财务分析月度快照 + 收入分类修复
> 

## v11.35.2 (2026-09-08) 定时记账「立即执行」日期语义修正 + 审核中心路由双轨
> 

## v11.35.0 (2026-09-08) ledger-app 注册功能 + 意见反馈对接 + 备忘录布局优化
> 

## v11.34.0 (2026-09-08) 备忘录增强：事项/内容/时间/提醒方式/重要程度
> 

## v11.33.0 (2026-09-08) 记账 App「我的」四大功能全链路落地
> 

## v11.32.0 (2026-09-08) 后台登录验证码风控 + 参数配置缓存 TTL
> 

## v11.31.1 (2026-09-08) 金额口径收尾：迁移脚本双方案 + ledger-app 去 cent 命名
> 

## v11.31.0 (2026-09-08) pay 模块金额单位统一为元（全项目金额口径收口）
> 

## v11.30.5 (2026-09-07) 面试报告分享（token 免登录公开访问）
> 

## v11.30.4 (2026-09-07) 报告「相关知识点」Tab 从未生成 → 实现题库 tags 聚合 + LLM 简介
> 

## v11.30.3 (2026-09-07) 面试报告页「对话回放」无数据修复
> 

## v11.30.2 (2026-09-07) 实时分析维度分固定值修复（连续化重构）
> 

## v11.30.1 (2026-09-07) 管理端语音面试 Controller 补建 + 全链路接口核对
> 

## v11.30 (2026-09-07) AI 面试全链路动态配置化 + AI 场景配置中心
> 

## v11.23 (2026-09-04) 记账模块：资产负债列表一键查关联流水 + 金额口径再确认
> 

## v11.24 (2026-09-04) 记账模块：分类体系全类型覆盖 + 语义分组
> 

## v11.25 (2026-09-04) 记账模块：修复凭证上传（api/ledger.js 缺 import 致 ReferenceError）
> 

## v11.26 (2026-09-04) 记账模块：金额单位统一为元 + 借款自动建负债
> 

## v11.22 (2026-09-04) 记账模块：金额安全计算全链路修复 + 负号/浮点/单位边界三重修复
> 触发：用户反馈「总资产出现负数仍显示正数 + ledger_liability_account 存 2200 前端显示 22 元」两个金额 BUG，排查后暴露 centToAmount(Math.abs) 丢符号、yuanToCent(浮点乘 100) 精度、表单回显千分位回写 Number 变 NaN 三处隐患，统一全链路修复。

## v11.21 (2026-09-04) 记账模块：AI 财务分析 + 资产负债合并 Tab 页 + 交互细节优化
> 用户需求 3 项：① 报表/总览加 AI 分析入口（分析资产结构、收入来源、债务风险、给出综述与建议）；② 资产/负债两页合并为「资产负债」Tab 切换页，腾出 Tab 位给「分析」页；③ 交互细节（tips 可关闭、弹层可取消、分类宫格可收起）。涉及前后端 + SQL + 后台字典。

## v11.20 (2026-09-04) 记账模块：资产页UI升级 + 移除原生导航栏改自定义标题
> 用户反馈 2 项：资产页同参考图风格升级；移动端去掉顶部原生"墨韵记账"导航栏（或跟随主题）。纯前端，无 SQL、无后端改动。

## v11.19 (2026-09-04) 记账模块：全局主题系统 + 记一笔页重设计 + Long序列化拼接Bug修复
> 用户反馈 5 项：总负债拼接 Bug、UI 风格按参考图全局优化、记一笔页按类型展示分类并突出核心字段、主题可切换、上传与预览。无表结构变更、无 SQL、无菜单变更。

## v11.18 (2026-09-03) 记账模块体验迭代：分类标签 + 语义提示 + 未登录引导
> 用户试用反馈 8 项的落地。无表结构变更、无 SQL、无菜单变更，纯前端交互与引导优化（复式记账流水在 Phase 1 已实现，本次补语义说明）。

## v11.17 (2026-09-03) 记账模块 Phase 3 收尾：站内预算提醒 + 新手引导
> 设计方案 V1.3 §13 预算提醒的站内通道（订阅消息模板待申请，先落地事务内实时判断）+ Phase 2 遗留的新手引导。无表结构变更。

## v11.16 (2026-09-03) 记账模块 Phase 3：报表中心 + CSV 导出
> 设计方案 V1.3 §12 Phase 3 核心项：报表全量 + 数据导出。无表结构变更（复用既有 6 表数据），纯增量接口与页面。

## v11.15 (2026-09-03) 记账模块 Phase 2：uni-app 前端落地 + 凭证截图 + UI 紧凑化
> 前置修复：门户登录态以 HTTP 200 + code:401 返回时前端拦截器未识别（记一笔页账户/负债列表静默为空）。request.js 增加 body.code===401 分支（清 token + 引导登录），记一笔页 onShow 登录校验 + 空列表引导。

## v11.14 (2026-09-03) 记账模块（个人资产管理）Phase 1：后端核心落地
> 新需求立项：记账 App/小程序，核心价值「一个数字看清全部身家」。设计方案经两轮评审定稿为 V1.3（单账本模型、仅人民币，App 端走 /portal/ledger/** 复用门户安全链）。本阶段交付后端全量：6 张表、资产/负债账户、6 种记账类型的联动事务核心、流水冲正重放、dashboard 总览、净资产每日快照任务。微信登录绑定（用户中心改造）与 uni-app 前端工程属 Phase 1 后续项。

## v11.15 (2026-09-03) 记账模块 Phase 1 收尾：uni-app 工程 + 管理端页面
> 前后端闭环完成：新建 uni-app 工程 moyun-ledger-app（一套代码编译 H5/小程序），管理端补齐预设分类维护与脱敏运营统计，与后端 v11.14 交付的 API 全面对接。

## v11.13 (2026-09-02) 面试题库归属学习中心：路由反转 + 面包屑修正
> 用户反馈：题库页 URL 为 `/interview/questions`，面包屑显示"首页/面试指南/面试题库"，但题库实际归属学习中心（与 portal_category.nav_route_path 及学习中心栏目对齐）。修正为 URL `/learn/questions`、面包屑"首页/学习中心/面试题库"。

## v11.12 (2026-09-02) 通用 AI 异步任务基础设施 + 全局慢请求保护 + URL 状态参数化（v10.23）
> 用户反馈三个问题：① 门户 LLM 接口多为同步调用，大输入场景（简历解析/岗位匹配等）直接超时且前台无感；② 慢接口等待中用户切页/刷新无提示，结果静默丢失；③ 有状态页面（如优化工作台 step/选中岗位）刷新后回到初始步，要求状态参数放 URL 支持多次刷新。评估结论：三个问题全部成立；流式输出（SSE）不适合本项目（LLM 输出为结构化 JSON，半截 JSON 无法渲染），统一采用"异步任务 + 前端轮询"，已在深度优化场景验证过。

## v11.11 (2026-09-01) 简历模块重构：上传闭环+解析反显+附件版本+AI填空+全文分析+diff回放（v10.22）
> 用户反馈：简历模块不合理不完善——上传简历只是临时解析不保存源文件、解析结果没反显表单、空字段无 AI 填充、AI 分析用结构化 JSON 而非全文、优化结果无 diff 回放、编辑页多个 AI 优化入口重复。本次重构按"上传→解析→维护→AI分析→反显修改"完整闭环重新设计。

## v11.10 (2026-09-01) 简历优化页刷新状态恢复（v10.21）
> 用户反馈：简历优化页 `/interview/resume/optimize?resumeId=1` 在 AI 分析或深度优化进度条等待期间，不小心刷新页面会回到 step1 重新开始，之前的分析结果、采纳状态、异步任务全部丢失。

## v11.9 (2026-09-01) 简历深度优化采纳失效修复：section 归一化 + 越界报错（v10.20）
> 用户反馈：工作经历、项目经历采纳后没有保存。根因：LLM 返回的 `section` 值不一致（可能返回 `works`/`projects`/`experience` 等复数或同义词），前后端 `switch case "work"`/`case "project"` 精确匹配走不到，静默走 default 忽略，用户以为采纳了实际没生效。

## v11.8 (2026-09-01) 简历深度优化循环依赖根治：抽离 Generator（v10.19 结构调整）
> v11.6 用 `@Lazy` 绕过 ResumeDeepOptimizeService ↔ ResumeOptimizeAsyncExecutor 循环依赖，但 `@Lazy` 只是推迟注入时机，结构问题未解决。本次从代码结构层面抽离 Generator Bean，彻底消除循环。

## v11.7 (2026-09-01) 简历深度优化采纳交互优化（v10.20）
> 用户反馈：采纳按钮点击后体验割裂——没有"写到对应字段"的视觉反馈，缺单字段重新生成能力，预览必须跳转 step5。本次重构 step4 交互：采纳后卡片折叠为「已应用到[字段]」状态，新增单字段重新生成（3 候选版本弹窗），step4 内增加就地预览面板（不必跳转）。

## v11.6 (2026-09-01) 简历深度优化异步任务化：解决大模型调用超时（v10.19）
> 用户反馈 `/interview/resume/optimize?resumeId=1` 简历深度优化调用大模型频繁超时（dashscope compatible-mode timeout 60s），langchain4j 默认重试 3 次累计 180s 仍失败。根因：①默认 `ai_model_config.timeout=60s` 偏紧；②Prompt 直接序列化整个 `UserResumeVO`（含 id/version/status/时间戳等无关字段）输入 token 过大；③同步阻塞 HTTP 线程，关闭页面即失败。采用「异步任务 + 状态记录 + 调大超时」组合方案，支持关闭页面后回来查看、失败可重试。

## v11.5 (2026-08-31) CMS 文章+专栏管理合并：Tab 入口 + 批量加入专栏 + 维护文章弹窗 + 标签展示
> 用户反馈：①文章管理列表操作栏过宽、缺用户名搜索；②列表/详情未关联 portal\_tag 标签；③缺「批量加入专栏」能力；④文章管理与专栏管理分菜单割裂。统一收口为「文章管理 Tab 入口 + 专栏管理 Tab 复用 + 维护文章弹窗」的合并形态。

## v11.4 (2026-08-31) 简历模块重构补丁：模板套用打通 + 子组件抽离 + 评分报告归档（设计文档 4 处偏差收口）
> 对照《简历编辑和优化模块重构设计-20260826.md》走查，收口 v10.13 落地时遗留的 4 处偏差：①模板套用仅预填标题（无结构化内容）②ResumeOptimizePage 内联 UI 未抽组件 ③缺跨页数据传递 ④评分无归档可追溯。

## v11.3 (2026-09-02) 支付表统一 pay\_ 前缀 + 钱包页重构 + 支付通知并入消息中心
> 补丁：`moyun-pay-v11.3-refactor.patch`

## v11.1 (2026-09-02) 支付闭环走查修复 + 短信验证码 + 旧订单模块清理
> 补丁：`moyun-pay-v11.1-cleanup.patch`

## v11.0 (2026-09-02) 微信支付公共通道 + 打赏分账体系
> 补丁：`moyun-pay-gateway-v11.0.patch`

## v10.18 (2026-09-01) AI 面试官 LLM 驱动动态追问体系
> 补丁：`moyun-llm-followup-v10.4.patch`（交付轮次编号 V10.4，下同）

## v10.17 (2026-09-01) 简历→语音面试全链路打通 + 历史面试留存
> 补丁：`moyun-voice-link-v10.3.patch`

## v10.16 (2026-08-31) 编程题接入真实 OJ 判题（题库练习链路收口）
> 补丁：`moyun-oj-v10.2.patch`

## v10.15 (2026-08-31) AI 语音面试对话可视化增强（对标 HireVue/面试鸭AI）
> 补丁：`moyun-voice-ui-v10.1.patch`

## v10.14 (2026-08-26) 简历编辑页 AI 实时辅助编辑（设计文档 P0 需求#2）+ 编译错误修复
> 

## v10.13 (2026-08-26) 简历优化重构：岗位匹配评分 + 深度优化前后对比闭环（5步工作台）
> 

## v10.12 (2026-08-25) 简历附件上传实装：单文件上传 + 解析覆盖填充到在线简历
> 

## v10.11 (2026-08-25) 简历 AI 建议重构：对齐原型（模块 Tab + 采纳自动填充字段）；LLM 返回 markdown 包裹修复
> 

## v10.10 (2026-08-25) 实名策略分层重构：发布开放+敏感场景强制实名；面经草稿/发布链路修复；简历编辑页增强
> 

## v10.9 (2026-08-25) 创作者认证实名合规改造：证件号加密存储 + 脱敏展示 + 数据联动
> 

## v10.8 (2026-08-25) 门户 AI 内容分析统一接口 + 发布页摘要/SEO 智能提取
> 

## v10.7 (2026-08-25) 写作提示模块增强：AI 定时生成 + 特殊日期感知 + 前后台一体化
> 

## v10.6.3 (2026-08-25) 待审核文章口径统一：首页指标与审核中心待办同源
> 

## v10.6.2 (2026-08-25) 帮助中心前后台一体化优化：后台菜单合并 + 前台分类过滤
> 

## v10.6.1 (2026-08-25) 精选笔记 500 修复 + 测试用例管理接口迁移 + 题目收藏展示修复
> 

## v10.6 (2026-08-20) 题库模块重构·阶段1+2：刷题中心上线 + 选择题做题闭环
> 

## v10.1 (2026-08-18) 语音面试官 MVP
> 

## v10.2 (2026-08-19) 简历模块升级：模板管理 + 用户简历入口 + 维护页三栏重构
> 

## v10.3 (2026-08-19) 旧版 AI 模拟面试（MockInterview）下线清理
> 

## v10.4 (2026-08-19) 栏目菜单重构：前台 Mega Menu + 移动端 5 Tab + 后台 7 一级菜单
> 

## v9.6 (2026-08-16) 后台全面体检：菜单收敛 + 27类业务字典 + 前台字典化（"前台数据皆有后台管理"）
> 

## v9.5 (2026-08-16) 分支合并：main-dev-article × moyun-dev-kouzi（后续开发基线）
> 

## v9.0 (2026-08-15) 平台重构：删除PK/圈子 + 首页改版 + 认证分级 + Admin增强
> 

## v8.2 (2026-08-14) 通用导入模板 + 题库导入导出
> - 脚本：`upgrade_v8.2_import_template.sql`（幂等）

## v8.1 (2026-08-14) 审核模块统一整合
> - 脚本：`upgrade_v8.1_audit_unified.sql`（幂等）

## v5.2 (2026-07-19) 安全加固 + SQL 整理 + 文档重建
> 

## v5.0 (2026-07-01) 积分打赏 MVP + 相关推荐 + 广告位基建
> 

## v4.0.5 (2026-06-26) 全面功能排查清单
> 

## v4.0.4 (2026-06-26) 功能细节修复（审核通知 + 草稿自动保存 + 配置缓存 + 标签推荐）
> 

## v4.0.3 (2026-06-26) 文章模块优化（发布链路完整性 + 轮播图联动）
> 

## v4.0.2 (2026-06-26) 三端深度复查 P1 修复 + 文档维护
> 

## v4.0.1 (2026-06-26) 关注/取消关注接口整合
> 

## v4.0.0 (2026-06-26) Dashboard 改造 + 举报反馈模块 + 4 个 Bug 修复
> 

## v2.1.1 (2026-06-16) Mapper XML resultMap 分层 + @Slf4j 编译修复
> 

## v2.1.0 (2026-06-15) 面试空间 + 通用标签模块
> 

## v2.0.0 (2026-06-01) 门户内容模块基线
> - 🔧 统一：Entity 使用 `@Data` + `@TableName` 的 MyBatis-Plus 风格

