# 开发日志（devlog）

> 2026-09-17 v11.98 后瘦身：历史条目仅保留「版本 + 修改类目 + 简介」，实施细节沉淀于方案文档与《项目现状总结》。v12 起新条目同样只记类目+简介。

## v14.66 (2026-10-01) 全端评审落地（第 107 批）：征文投稿列表**服务端分页**（附 hasSubmitted 口径修正）

| 层 | 改动 |
|---|---|
| **Service** | 新增重载 `getContestDetail(contestId, userId, pageNum, pageSize)`；**旧 2 参方法委托到默认（第 1 页 20 条）**，调用方零改动；投稿查询由 `selectList` → `selectPage`，响应新增 `submissionTotal` / `submissionPage` / `submissionSize` |
| **Controller** | `GET /portal/contest/{id}` 增加 `submissionPage` / `submissionSize`（默认 1/20，页大小上限 50） |
| **前端 API** | `getContestDetail(id, params?)` 支持分页参数并**透传** |
| **前端页面** | `ContestDetailPage` 投稿总数改用**服务端 total**；新增 `loadMoreSubmissions()`（按页追加 + **按 id 去重**）与「**加载更多投稿（还有 N 篇）**」按钮 |

**★ 顺带修掉一个真 bug（口径错位）**：`hasSubmitted` 原先在**当前页记录**里 `anyMatch` ⇒ 改成服务端分页后，用户自己的投稿若**不在第 1 页**就会被判为"未投稿"，页面上仍显示可投稿并**诱导重复投稿**。已改为对该活动 + 当前用户做**独立计数**（与分页解耦）。

**★ 过程自纠 3 次**：
① `hasSubmitted` 锚点带 dump 的 `]` 未命中 ⇒ 按行定位重写；
② 首版把 `loadMoreSubmissions` **插进了 `loadDetail` 的 JSDoc 中间**（把注释块劈开）⇒ `vue-tsc` 报一串 `TS1109/TS1125`；**从备份回滚该文件**后，改为插在 JSDoc 起始 `/**` 之前，注释块完整；
③ **自查发现半成品**：`getContestDetail` 的 `params` 声明后**未透传**（eslint unused warning）⇒ 立即接进请求。

**验证**：后端 `mvn -o -B test` **481/481** · 门户 `vue-tsc`（strict）0 / `eslint` **0 problems** / build ✓（52.48s）· 对账 **已修 459（P0 6 / P1 76 / P2 291 / P3 86）· 订正 20 · 不做 5 · 未修 398 · 有结论率 54.3%**

## v14.65 (2026-10-01) 全端评审落地（第 106 批）：名家列表**服务端分页**（关键词 + 四种排序下沉到 SQL）

**背景**：前端原先是 `getAuthors(100)` 后**在浏览器内**搜索/排序/分页 ⇒ ① **第 101 位之后的作者永不出现**；② "最受欢迎/粉丝最多"只在**已加载的 100 人子集**内排序（名不副实）。这不是"加个加载更多"能解决的，**必须把关键词与排序下沉到 SQL**。

| 层 | 改动 |
|---|---|
| **Mapper XML** | 新增 `authorVisibilityFilter` 片段（原三条件 + 排除自己 + **关键词**模糊匹配 `username`/`bio`）；新增 `countAuthors`；新增 `selectAuthorsPage`，排序用 `<choose>`：`newest`（create_time）/ `fans`（子查询 `portal_follow.following_id` 计数）/ `popular`（子查询 `SUM(views)+SUM(likes)*10`）/ 默认 `works`（发布文章数），均带 `create_time DESC` 兜底，末尾 `LIMIT #{offset}, #{size}` |
| **Mapper 接口** | 新增 `selectAuthorsPage(keyword, sort, offset, size, excludeUserId)` 与 `countAuthors(keyword, excludeUserId)` |
| **Service** | 新增 `selectAuthorsPage(...)`（页码/页大小归一，页大小上限 50），返回 `[list, total, pageNum, pageSize]` |
| **Controller** | `/portal/user/authors` 增加 `pageNum/pageSize/keyword/sort`；**传 `pageNum` 才启用分页并返回 `{list,total,pageNum,pageSize}`，不传则保持旧行为返回数组**（向后兼容） |
| **前端 API** | `getAuthors` 支持 `number`（旧用法）与对象（分页）两种入参 |
| **前端页面** | `AuthorsPage` **删除本地过滤/排序与二次切片**，改用**服务端 `total`** 计算总页数；关键词/排序变化与翻页均**重新请求**；`HomePage` 的旧调用兼容两种返回结构 |

**★ 过程自纠 3 次**：
① 首批锚点又**误带 dump 的 `]`**（3 处未命中）⇒ 按行定位补上；
② `getAuthors` 返回类型变为联合类型后 **`HomePage` 编译失败**（`Property 'map' does not exist`）⇒ 加 `Array.isArray` 兼容分支；
③ **自查发现半成品**：`watch([searchQuery, sortBy])` 只重置了页码、**没有重新请求** ⇒ 搜索/排序不会生效；立即补 `void loadUsers()`（这正是本轮反复抓出的 defect class，不能自己犯）。

**验证**：后端 `mvn -o -B test` **481/481** · 门户 `vue-tsc`（strict）0 / `eslint` **0 problems** / build ✓（1m3s）· 对账 **已修 458（P0 6 / P1 76 / P2 290 / P3 86）· 订正 20 · 不做 5 · 未修 399 · 有结论率 54.2%**

## v14.64 (2026-10-01) 全端评审落地（第 105 批）：话题评论「加载更多」

| 缺陷 | 修复 |
|---|---|
| `TopicDetailPage` 评论**固定 `pageNum=1, pageSize=10`**，返回的 `total` **只存不用**，评论区既无分页也无「加载更多」⇒ **评论超过 10 条，后面的永远看不到** | ① `CommentState` 增加 `page` / `loadingMore`；② `loadComments(..., loadMore)` 支持**按页追加 + 按 id 去重**，追加失败**不清空**已加载数据；③ 新增 `loadMoreComments()`；④ 模板在评论区末尾加「**加载更多评论（还有 N 条）**」按钮（仅在 `list.length < total` 时显示，加载中禁用并显示"加载中…"） |

**验证**：门户 `vue-tsc`（strict）0 / `eslint` **0 problems** · 对账 **已修 457（P0 6 / P1 76 / P2 289 / P3 86）· 订正 20 · 不做 5 · 未修 400 · 有结论率 54.1%**

### 未完成（据实说明，未做半成品）

原列"5 条可改"中，**征文投稿分页**与**作者列表分页**这两条**未做**，原因如下（均为**需要改接口签名 + 查询层**的改造，不是单点修改）：

| 项 | 需要的改造 |
|---|---|
| `ContestDetailPage` 投稿列表 | 后端 `getContestDetail` 用 `selectList` **一次性返回全部未淘汰投稿**（无分页/无上限）⇒ 需改为分页查询（mapper 增加 count + `LIMIT offset,size`）、在详情响应中改/增投稿分页结构，前端改「加载更多」并处理 `total` 口径变更 |
| `AuthorsPage` 作者列表 | 前端 `getAuthors(100)` 后在**浏览器内**搜索/排序/分页 ⇒ **第 101 位之后的作者永不出现**，且"最受欢迎/粉丝最多"只在 100 人子集内排序。需把**搜索与排序一并下沉到服务端**（mapper 增加条件与排序分支 + count），前端改服务端分页 |

> 这两项若只做"前端加个加载更多"而不动后端查询，会形成**半成品**（数据源仍是截断的 100 条/全量列表），因此**没有动手**，留作独立改造。

## v14.63 (2026-10-01) 全端评审落地（第 104 批）：**后端回填 2 项**（观点话题归属 · 面经作者信息）

| # | 缺陷 | 修复 |
|---|---|---|
| 1 | **我的观点看不出属于哪个话题**：`TopicPostVO` 只有 `topicId`，卡片只显示「#N 楼」（而该列表按时间倒序**混合了所有话题**的观点） | ① `TopicPostVO` 新增 `topicTitle`；② `convertToPostVOPage` 按 `topicId` 去重后**一次批量查询** `portal_topic` 回填；③ 前端卡片展示「所属话题：<标题>」并可点击跳转 |
| 2 | **面经作者名/头像恒为空**：`toExperienceVO` 里只有一行注释「作者信息（这里省略用户名查询…）」，**从未填充** ⇒ 列表与详情页作者信息都是空的 | 新增 `fillExperienceAuthors(List<vo>)`：按 `userId` 去重后**一次批量查询**回填 `userNickname`/`userAvatar`；**列表与详情两个入口都调用**（顺带避免逐条查询造成 N+1） |

**★ 过程自纠 1 次**：前端首个版本把类型断言写进模板表达式（`v-if="(post as unknown as {...}).topicTitle"`）并用内联模板字符串跳转 ⇒ Vue 编译报 `TS1109/TS1128`；改为**脚本内 helper**（`postTopicTitle()` / `goTopic()`）后通过。

**验证**：门户 `vue-tsc`（strict）0 / `eslint` **0 problems** / build ✓（1m6s）· 后端 **481/481** · 对账 **已修 456（P0 6 / P1 76 / P2 288 / P3 86）· 订正 20 · 不做 5 · 未修 401 · 有结论率 54.0%**

## v14.62 (2026-10-01) 全端评审落地（第 103 批）：**"可改"组 5 项**（URL 同步 · 分页窗口 · 口径标注 · 分区空态 · 收银台商品信息）

| # | 页面 | 缺陷 | 修复 |
|---|---|---|---|
| 1 | `QuestionListPage` | URL 同步**只单向**（本地 → URL），**没有 watch `route.query`** ⇒ 浏览器后退/前进时地址栏 query 变了、本地筛选不变（列表与 URL 脱节） | 补**反向同步** `watch(() => route.query)`：取值确有变化才更新并重载（避免与 `router.replace` 形成循环） |
| 2 | `ResumeTemplatePage` | 分页 `v-for="p in totalPages()"` **全量渲染**页码，页数多时一排按钮撑爆布局 | 改为**窗口化** `visiblePages`（首尾 + 当前页附近 + 省略号，最多 7 个），模板按 `typeof` 区分数值与占位 |
| 3 | `KnowledgeGraphPage` | 后端只返回**题目数 Top 60** 标签（`ORDER BY question_count DESC LIMIT 60`），页面把这份**截断数据当全量**渲染（汇总卡/标签云/关系图） | 如实标注口径为"Top 60 标签"，不再假装全站全量 |
| 4 | `InterviewPage` | 五个分区（分类/热门题目/热门面经/简历模板/热门公司）**只有 `v-if="length > 0"`**，为空时分区**直接消失**、无任何说明 | 五个分区各补**空态文案** |
| 5 | `PayCashierPage` | 收银台只显示金额与支付单号 ⇒ **用户不知道"买的是什么"**（而 `PayOrder` 下单时就写入了 `subject`/`bizType`） | `/portal/pay/status` **下发 `subject`/`bizType`**；待支付区展示商品名；`PayStatusResult` 类型同步补字段 |

**验证**：门户 `vue-tsc`（strict）0 / `eslint` **0 problems** / build ✓（52.94s）· 后端 **481/481** · 对账 **已修 454（P0 6 / P1 76 / P2 286 / P3 86）· 订正 20 · 不做 5 · 未修 403 · 有结论率 53.7%**

## v14.61 (2026-10-01) 全端评审落地（第 102 批）：**工程/配置类 3 项**（文档暴露面 · 安全链匹配 · SQL 日志）

| # | 缺陷 | 修复 |
|---|---|---|
| 1 | **接口文档暴露面**：原说明称"生产设 `KNIFE4J_PRODUCTION=true` 关闭文档"，但该开关**只关增强 UI**，原始 OpenAPI 端点仍在核心链 `permitAll` 内（`/doc.html`、`/swagger-ui.html`、`/v3/api-docs/**`）⇒ 匿名可枚举接口 | `SecurityConfig` 的文档端点 `permitAll` 改为**跟随 `knife4j.enable`**（注入 `@Value("${knife4j.enable:true}")`）：生产置 `KNIFE4J_ENABLE=false` 后**不再匿名放行**，回落为"已认证可访问" |
| 2 | **安全链匹配**：只在 `startsWith("/portal/admin/")` 时归核心链 ⇒ **裸路径 `/portal/admin`**（不带斜杠）不落核心链，只被门户链 `authenticated()` 兜住（两条链鉴权口径不一致） | 改为 `uri.equals("/portal/admin") \|\| uri.startsWith("/portal/admin/")` |
| 3 | **SQL 日志绕过 logback**：`application-dev/local.yaml` 的 `mybatis.configuration.log-impl` 为 `StdOutImpl` ⇒ 直写 stdout，**无法分级、无法与 traceId 关联、污染容器日志** | 改为 `org.apache.ibatis.logging.slf4j.Slf4jImpl`，由 logback 统一控制级别与格式 |

**验证**：后端 `mvn -o -B test` **481/481** · 对账 **已修 449（P0 6 / P1 76 / P2 281 / P3 86）· 订正 20 · 不做 5 · 未修 408 · 有结论率 53.2%**

## v14.60 (2026-10-01) 全端评审落地（第 100–101 批）：**P2 收尾一批 9 项**（体验/口径/校验类）

| # | 页面 | 缺陷 | 修复 |
|---|---|---|---|
| 1 | `PracticeChoiceListPage` | 难度筛选直接把 `route.query.difficulty` 原文当值，后端**精确匹配** ⇒ `?difficulty=Easy`、`1`、拼错值**都命中 0 条** | 加**白名单**（easy/medium/hard）+ 小写归一，非法值忽略（等同"全部难度"） |
| 2 | `AuthorsPage` | 「加入于」直出后端 **ISO 串** | 走 `formatDate` |
| 3 | `QuoteListPage` | `getQuoteList` 声明并透传 `sort`，但**页面从不传、后端也不用**（Mapper 固定 `ORDER BY like_count DESC`）⇒ 有排序承诺却不生效 | 不再传无意义的 `sort` 并注明"当前仅热度序，排序待后端实现" |
| 4 | `AuthorPage` | `useHead(generateSeo({…}))` **未包 computed** ⇒ setup 期 `author` 恒 null、此后 **SEO 再不更新** | 改 `computed(() => generateSeo({…}))` |
| 5 | `ChoicePracticePage` | 只用 `options.length` 判断"是否选择题" ⇒ 若题目配了选项但 `practiceMode=reading`，**后端走主观题分支而前端提交 answer，必然判失败** | 增加 `isChoiceQuestion`（`practiceMode === 'choice'`）参与判断 |
| 6 | `VoiceEngineDemoPage` | 联动用**裸 `setTimeout(1500)`**，id 未保存也未清理 ⇒ 卸载后 1.5s **仍会 `resetAsr()+startAsr()`**（在已离开的页面重新申请麦克风） | 持有 `pipelineTimer` 并在 `onUnmounted` 清理 |
| 7 | `FollowListPage` | 空态只按 `isOwnPage` 二选一，**未随 `activeType` 区分** ⇒ 自己的「关注」页签显示"还没有人关注你哦"、别人的「粉丝」页签显示"该用户还没有关注的人" | 改为**四象限**文案（followers/following × 本人/他人） |
| 8 | `LoginPage` / `utils/validation.ts` | 登录校验把账号上限定在 **20**，而后端 `loginPreCheck` 允许 **username 2-50 / password 6-50** ⇒ 后台导入、接口创建、历史数据的长凭据账号**在门户无法登录** | 上限与后端**对齐**（仅放宽，不下调下限） |
| 9 | `StudyCalendarPage` | 汇总卡标注"**近 1 年**"，数据实为**所选自然年**（右侧热力图才是"近 365 天"） | 三张卡明确为「{所选年份} 年总提交/通过次数/活跃天数」，与滚动口径区分 |

**★ 过程自纠 3 次**：① 首批 5 条里 2 条锚点又**误带 dump 的 `]`**（`// SEO]`、`// 是否有选择题选项数据]`）未命中 ⇒ 改正后通过；② `AuthorsPage`/`AchievementsPage` 用了 `formatDate` 却**未导入** ⇒ `vue-tsc` 报 `TS2339`，补导入；③ `VoiceEngineDemoPage` 的 `pipelineTimer` **声明锚点猜错**（注释块文本不符）⇒ 按实际注释行定位后插入。

**验证**：门户 `vue-tsc`（strict）0 / `eslint` **0 problems** / build ✓（54.27s）· 对账 **已修 446（P0 6 / P1 76 / P2 278 / P3 86）· 订正 20 · 不做 5 · 未修 411 · 有结论率 52.8%**

## v14.59 (2026-10-01) 全端评审落地（第 99 批）：**"必须处理"收尾 2 项**（书单会员门禁 + 收银台手动查询）

**结论先行**：这两项经核代码**能力都已具备**，无需等产品立项即可落地（只余一处语义待产品定义，已如实标注）。

| # | 原判"需产品/集成决策" | 核查结果 | 实现 |
|---|---|---|---|
| **1** | 书单 `accessLevel` 门禁：需接入 VIP 判定 | **能力已具备**：`IVipService.isVip(userId, platformCode)` 存在，且**同模块** `PortalReadingController:419` 已有 `vipService.isVip(readerId, "portal")` 的用法；控制器已注入 `vipService` | 在 `getBookListById`（公开详情接口）增加门禁：`accessLevel=vip` 且当前用户**非会员**时 —— **不下发书籍列表**（`books` 置空）并回置 `accessLevelLocked=true`，同时返回 `accessLevel`；前端改为渲染「**该书籍列表为会员专属** + 前往开通会员」而不是"空书单" |
| **2** | 收银台手动查询：需确认 `pay/status` 能否作主动查询源 | **可以**：`GET /portal/pay/status/{payNo}` 以网关为准返回最新状态，并**校验订单归属**（`ownOrderOrNull`）；前端 `getPayStatus` 也已存在且轮询就在用它 | 新增「**手动查询支付结果**」按钮（`manualQuery()`：PAID/SETTLED → 成功；CLOSED → 关单提示；其余 → "尚未收到支付结果"）；超时文案改为「自动轮询已停止（**订单有效期 N 分钟**，仍可点下方按钮查询）」，消除"提示手动查询却无入口" |

### 仍需产品定义的一处（已如实标注，未臆造）

- `accessLevel='preview'` 的「**部分可见**」语义（可见前几本？可见目录？）**尚无产品规则**。本次实现为**放行 + 打「可预览」标**，并在代码注释中写明"语义需产品定义"，**不自行发明截断规则**。

**验证**：后端 `mvn -o -B test` **481/481** · 门户 `vue-tsc`（strict）0 / `eslint` **0 problems** / build ✓（50.09s）· 对账 **已修 435（P0 6 / P1 76 / P2 269 / P3 84）· 订正 20 · 不做 5 · 未修 422 · 有结论率 51.6%**

## v14.58 (2026-10-01) 全端评审落地（第 97–98 批）：**"必须处理"组 8 项**（含 SSRF / 越权 / 弱口令 / 编造数据）

**依据**：对剩余 P2 分档后，本批处理其中的**必须处理**组（安全、数据正确性、编造数据类），全部先核验代码再改。

| # | 缺陷 | 性质 | 修复 |
|---|---|---|---|
| **1** | AI 工作流 HTTP / Webhook 节点**裸 `new RestTemplate()` 抓取用户可配 URL** | **安全（SSRF）** | 新增 **`OutboundUrlGuard`**：① 协议白名单 http/https；② `localhost` / `*.internal` / `*.local` / `metadata.google.internal` 主机名拦截；③ 字面量 IP 的内网/保留/链路本地/组播/回环判定（含 **169.254.169.254 云元数据**、10/8、172.16/12、192.168/16、**100.64/10**、198.18/15、240/4、`fc00::/7`）；④ **域名先 DNS 解析再逐地址校验**（防域名指向内网）。两个执行器发请求前统一调用；配套 **`OutboundUrlGuardTest` 5 个用例**（后端测试 476 → **481**） |
| **2** | `AbstractQuartzJob.after()` 定时任务日志 **NPE** | 真 bug（实跑数十次） | `threadLocal.get()` 为 null（`before` 抛异常或未调用）时**兜底为当前时间**，避免 `getStartTime().getTime()` 空指针 |
| **3** | `PaginationInnerInterceptor` **未 `setMaxLimit`** | 性能/稳定 | 统一 `setMaxLimit(100L)`。原先只有走 `PageUtils` 的调用点有 100 上限，`new Page<>(1, 100000)` 仍可**整表拉取** |
| **4** | 举报证据图**无服务端校验** | 数据/滥用 | 新增 `validateEvidenceImages`：**最多 3 张**且每项非空。原先仅前端 `MAX_IMAGES`，实体注释写"最多3张"却无任何校验，绕过前端即可提交任意张数 |
| **5** | 专栏编辑页**越权** | **权限** | 加载详情后**校验作者身份**，非作者直接拒绝并提示，不再渲染他人专栏的编辑表单。原先只拦"非 published 且非作者" ⇒ **已发布专栏对任意登录用户开放**，且公开详情接口会给非作者 **+1 浏览量** |
| **6** | 简历模板**组合搜索 OR 优先级** | 数据正确性 | 关键词改为 `and(w -> like(title).or().like(description))`。原先**裸 `.or()`** 生成 `category=? AND title LIKE ? OR description LIKE ?` ⇒ description 命中即**绕过 category/fileType/isPremium**，分类筛选形同虚设（同文件其他列表早已正确嵌套） |
| **7** | 短信重置密码**弱于邮箱通道** | **安全（弱口令）** | 与邮箱通道对齐：**6-20 位且必须含大小写字母和数字**。原先只判"长度不小于 6"、不校验上限与复杂度 ⇒ 短信通道成为弱口令入口（前端本就按此校验，属"后端弱于前端"） |
| **8** | 首页面试平台统计**凭空放大** | **编造数据** | 去掉 `hotQuestions.length * 500` / `* 1000` 兜底，取不到真实计数即 `0`。原先会编出"5000 道题 / 10000 次提交"的运营数据 |

**★ 过程自纠 3 次**：① 两处包名/依赖想当然被编译器抓出（`com.moyun.common.utils.JsonUtils` → 实际 `com.moyun.ext.ai.util.JsonUtils`；`ColumnEditPage` 未导入 `useUserStore`）；② `AbstractQuartzJob` 路径猜错（实际在 `com.moyun.ext.job.util`）⇒ 按全仓查找定位；③ 首轮索引 **3 条页码键写成了后端文件名**（CSV 用前端页名 `ForgotPasswordPage`/`ResumeTemplatePage`/`ReportFeedback`）⇒ 对账少算 3 条，补正后一致。

**验证**：后端 `mvn -o -B test` **481/481**（+5 SSRF 用例）· 门户 `vue-tsc`（strict）0 / `eslint` **0 problems** / build ✓（51.13s）· 对账 **已修 433（P0 6 / P1 76 / P2 267 / P3 84）· 订正 20 · 不做 5 · 未修 424 · 有结论率 51.4%**

## v14.57 (2026-10-01) 全端评审落地（第 95–96 批）：**"体验明显受损、成本低"一批 17 处**

**依据**：对剩余 P2 逐条复核后分档，本批处理其中"建议处理"组（全部**先看代码再改**，剔除陈旧条目）。

### 一、竞态与失败态（4 处，同一手法）

| 页面 | 问题 | 修法 |
|---|---|---|
| `HelpCenter` | 300ms 防抖只减少请求次数，**不做取消/序号** ⇒ 慢的旧关键词响应覆盖新结果 | 加请求序号，迟到响应丢弃 |
| `CompanyPage` | `page/total/tabLoading` 全局共享且无守卫 ⇒ 切 Tab 与翻页并发时列表被旧响应覆盖，其 `finally` 还会**提前结束 loading** | 加 `tabSeq` 守卫，loading 仅由最新请求收口 |
| `GrowthTimelinePage` | `switchModule` 直接 `await load(true)`，无序号 ⇒ 上个模块响应覆盖当前列表 | 加 `loadSeq`，过期响应丢弃 |
| `UserPage` | 各 Tab 加载失败**只 `console.warn`** 后置空数组 ⇒ 与"暂无内容"同貌，且**无重试** | 统一 `tabError` 提示条 + `retryCurrentTab()` |

### 二、口径/文案与事实不符（4 处）

| 页面 | 问题 | 修法 |
|---|---|---|
| `GrowthRankingPage` | 通篇写「**本季**」，但后端 `season_value` 与 `growth_value` **始终同步**、全仓无赛季重置（Mapper 注释自认）⇒ 实为累计榜 | 榜单说明/我的排名/累计成长/SEO 描述统一改为**累计**口径 |
| `PracticeCenterPage` | 「题库阅读」卡描述承诺"直接查看参考答案与解析"，而列表页**既不渲染答案也不渲染解析** | 描述改为"按分类浏览题目，**进入详情**查看参考答案与解析" |
| `PracticeCenterPage` | 页头承诺"学习行为**均**计入成长记录"，但成长上报接口**需登录**、而三条练习路由对游客开放 | 改为"**登录后**学习行为计入成长记录" |
| `HomePage` | 「AI 简历评分」与「AI 简历优化」**path 相同**（都指 `/interview/resume/optimize`）⇒ 评分这一格**永远到不了**评分入口 | 「评分」改指 `/interview/my/resumes` |

### 三、小 bug / 可用性（9 处）

| 页面 | 问题 | 修法 |
|---|---|---|
| `ArticleDetailPage` | `copyLinkAndNotify` **未 `await`** 就提示"链接已复制"（复制失败也报成功，同文件 `copyLink` 是正确写法） | 改 `async` + `await` |
| `BookListDetailPage` | 切书单（同组件参数变化）**漏重置 `liked`** ⇒ 残留上一个书单点赞态 | 重置 `liked` |
| `QuoteListPage` | 后端按**点赞数排序** + offset 分页 ⇒ 翻页期间点赞变化会**跨页重复/漏项**，重复 id 还触发 **v-for key 冲突** | 追加时按 id **去重** |
| `MembershipPage` | 底部 `fixed bottom-0` 订阅栏（约 70px）压住最后一个等级卡 | 内容末尾预留等高间距（`h-24`） |
| `AchievementsPage` | 达成时间直出后端 **ISO 串**（`2026-01-01T10:00:00 达成`） | 走 `formatDate` |
| `AchievementsPage` | 图标 404 时只把 `<img>` 隐藏，而 `v-else` 组件**只在 icon 为空时**渲染 ⇒ 圆圈内空白 | 加 `brokenIcons` 标记，404 后**回退内置 Award/Lock 图标** |
| `AuthorPage` | 「加入于」直出 ISO 串 | 走 `formatDate` |
| `NotFoundPage` | **完全没有 `useHead`** ⇒ 继承 `index.html` 的 `index,follow`（404 页被索引）；`meta.robots` 全站无消费方 | 补 `useHead` 输出 `robots: noindex,follow` |
| `StudyCalendarPage` | 清单称汇总卡标"近 1 年"却按所选年份推导 | **复核未在代码中定位到该文案**（可能陈旧），本批未改，留待复核 |

### 四、★ 复核发现（避免误报）

清单里这 3 条经代码核对**其实已经正确**，未改：
- `MyArticlesPage` 401 双重处理 → 已于 v14.34 修为 `loadError` + 不再强推登录页；
- `MyTopicsPage` 发起人昵称/头像 → 实际读的**扁平字段** `creatorNickname/creatorAvatar`，与后端 `TopicListVO` 一致；
- `ExperienceDetailPage` 作者信息 → 前端已有 `userNickname/userAvatar` 读取与兜底。

**★ 过程自纠**：本批 3 次被工具链/自查拦住 ——
① `CompanyPage` 引入的 `requestedPage` 未被使用（eslint unused）⇒ 移除（`seq` 已覆盖该场景）；
② `AchievementsPage`/`AuthorPage` 用了 `formatDate` 但**未导入** ⇒ `vue-tsc` 报 `TS2304`/`TS2339`，补导入；
③ 图标兜底首版**只改了写法没加兜底**（仍隐藏后留空）⇒ 重做为 `brokenIcons` + 模板回退。

**验证**：门户 `vue-tsc`（strict）0 / `eslint` **0 problems** / build ✓（55.09s）· 后端 **476/476** · 对账 **已修 425（P0 6 / P1 76 / P2 259 / P3 84）· 订正 20 · 不做 5 · 未修 432**

## v14.56 (2026-10-01) 全端评审落地（第 94 批）：**门户 `strict` 全量开启**（唯一剩余 P1 闭环）

**背景**：清单里最后一条 P1 —— 门户 `tsconfig.json` 的 `strict: false`，原报告记为"实测 74 个类型错误、需独立工程排期"。
本次**实做**：实测 **76 个错误 / 24 个文件**，全部消除后正式开启。

### 一、先修两处**系统性根因**（一次性消掉 19 个错误）

| 根因 | 问题 | 修法 |
|---|---|---|
| `utils/date.ts` | `formatDate` / `formatShortDate` / `formatRelativeTime` 参数为 `string \| Date`，而**函数体第一行本就 `if (!date) return ''`**；调用方常传可选字段 ⇒ 大量 `TS2345` | 参数**放宽为可空**（`string \| Date \| null \| undefined`），行为不变 |
| `api/category.ts` | 分类树/导航树缓存的 Promise 类型含 `null`，返回时未收敛 ⇒ 4 处 `TS2322` | 返回处 `data ?? undefined` |

### 二、再按文件收窄（57 → 0）

| 文件 | 做法 |
|---|---|
| `ResumeEditPage.vue`（15） | 用**交叉类型**把 `jobIntention` 收窄为非空（`UserResumeVO & { jobIntention: NonNullable<…> }`）+ 改用既有 `ensureJobIntention()`；`works/projects` push 前兜底；`filter(Boolean)` → **类型谓词** `filter((n): n is string => !!n)` |
| `Navbar.vue`（9） | `:to` / `:href` 兜底（`\|\| '/'` / `?? '#'`）；头像错误兜底用 `currentUser?.id ?? ''` |
| `ArticleDetailPage.vue`（9） | 断言既有的"非空不变量"（`article.value!`）与可选链（`articleAuthor?.id ?? ''`），SEO 字段兜底空串 |
| `VoiceInterviewPage.vue`（5） | 双向绑定 `computed` 去掉泛型改由 getter 标注返回类型；难度/岗位兜底；`weakestDimension` **显式声明返回类型**（原来被推断成 `null`，模板里成了 `never`） |
| `AuthorPage.vue`（5） | SEO 描述兜底、头像兜底、排序比较器 `(b.likes ?? 0) - (a.likes ?? 0)` |
| `AuthorsPage.vue`（2） | 排序比较器 `createTime` 兜底 |
| 其余 8 个文件各 1~2 处 | 模板可空访问改 `?.`、参数兜底空串、`submissionId!` 等 |

### 三、结果

| 项 | 结果 |
|---|---|
| `tsconfig.json` | ✅ `strict: false` → **`true`**（临时探测配置已删除） |
| 迭代过程 | 76 → 57 → 53 → 27 → 18 → 6 → **0** |
| 门户 `vue-tsc -b`（strict 生效） | ✅ **exit 0** |
| 门户 `eslint` / `npm run build` | ✅ **0 problems** / `✓ built in 56.19s`（构建命令本身含 `vue-tsc -b`） |
| 后端 `mvn -o -B test`（回归） | ✅ **476/476** |
| 对账 | ✅ **P1 已修 76 / 未修 0**（P1 全闭环）；总账 **已修 408 · 订正 20 · 不做 5 · 未修 449** |

**★ 过程自纠**：`Navbar` 的 `:to` 替换串被 shell 引号转义搞坏（拼成 `'"'"'/"'"'"`），`vue-tsc` 立刻报出一串 `TS1005`；**按行重写**后通过 —— 再次印证"改完必须跑类型检查"。

## v14.55 (2026-10-01) 全端评审落地（第 93 批）：钱包 2 处 + 创作者认证 2 处 + 设置页 2 处

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-eq 首屏统计卡恒显示"0 张"** | `bankCards` **只在切到「银行卡/提现」Tab 时**才加载（`onMounted` 只拉 overview+ledger），而统计卡「绑定银行卡」与 Tab 角标直接渲染 `bankCards.length` | 首屏一并加载银行卡（`loadCards()`） |
| **P2-er 空态文案与已上线功能矛盾** | 银行卡空态写"绑定后可发起余额提现（**打款功能即将开放**）"，而同页提现表单/提现记录/后台提现审核**都已上线** | 文案改为「绑定并完成核验后，即可发起余额提现」 |
| **P2-es 认证后回不到原操作** | `creatorPermission.ts` 跳转认证页时携带 `query.redirect=原路径`（打赏/积分兑换等被拦截的操作），但页面**从未 import `useRoute`**、更未读取该参数，`goBack` 只用 `history.back`/`/user` | 读取 `?redirect`，`goBack` 时**优先回跳**原操作；回退用统一的 `redirectPath` |
| **P2-et OCR 承诺大于实现** | 后端 `OcrServiceImpl` 的厂商 SDK **尚未接入**（todo 注释块），任何请求都返回 `success=false + "OCR 服务未接入，请手动填写"`，而页面仍以「OCR 自动识别」为卖点承诺"上传正面后自动触发" | 在 OCR 处理处**如实注明**当前未接入（并保留失败提示"请手动填写"），消除注释/承诺与实现的不一致 |
| **P2-eu 注销文案与实现不符** | 页面称"**所有数据**（文章、评论、收藏等）将被**永久删除**"，而后端实为**软删除**（`del_flag=2`/`status=1`），业务数据一条未删、且重新注册可恢复 | 文案改为「注销后无法登录（不可逆，需重新注册恢复）；已发布内容会同时下线，如需保留请先备份」 |
| **P2-ev 设置回显取自过期缓存** | `onMounted` 只调 `initializeUser()`，而 store 在 `localStorage` 已有 user 时**直接 return**、不回源 ⇒ 各开关取自本地缓存快照 | 回显前显式 `await userStore.fetchCurrentUser()`（失败不阻塞页面） |

**★ 过程自纠（本批 3 次，均由工具链抓出）**：
① `loadBankCards` / `fetchUserInfo` **函数名想当然** → `vue-tsc` 报 `TS2304`/`TS2339`，核实真实名为 `loadCards` / `fetchCurrentUser`；
② `redirectPath` 声明后**未接线**（eslint 报 unused）→ 补进 `goBack`；
③ OCR 注释锚点首次未命中 → 按真实行文本改写。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（54.16s）· 后端 **476/476** · 对账 **已修 407（P0 6 / P1 75 / P2 244 / P3 82）· 订正 20 · 不做 5 · 未修 450**

## v14.54 (2026-10-01) 全端评审落地（第 92 批）：消息页 2 处 + 专栏价格展示（附 1 条复核订正）

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-en 非法 `?tab=` 渲染成空白内容区** | `activeTab` 直接把 `route.query.tab` **断言**成 `TabKey`，而模板只覆盖 `notification/pay/todo/message/announcement` 五个分支 ⇒ `?tab=xxx` **不匹配任何 v-if/v-else-if**，内容区空白、Tab 高亮也缺失 | 收敛到**合法集合**，非法值落回默认 Tab（登录→通知 / 游客→公告） |
| **P2-eo 通知详情展示"调试式文本"且不跳转** | `notificationDataText` 把 data 渲染成「业务类型：article，业务ID：2」，页面对 `bizType/id` **不做任何路由跳转** | ① 新增 `BIZ_TYPE_LABEL` 把 `bizType` 显示为**可读业务名**（文章/面经/题目/话题/专栏/书籍…）；② 新增 `bizTarget` 解析关联目标并加「**查看详情**」按钮（按类型跳转对应详情页，跳转前关闭弹窗） |
| **P2-ep 付费专栏"等同免费订阅"** | `ColumnVO.price`（BigDecimal 单价）后端**已下发**，页面**完全不渲染**：既无价格标签也无付费动作，只有积分打赏 | 在专栏元信息区如实展示价格（`¥xx.xx`，>0 才显示）；**订阅/购买链路**属独立产品需求，已记入对账 |

### 复核订正 1 条

| 条目 | 归位 | 证据 |
|---|---|---|
| `MyTopicPostsPage` 读 `post.user?.nickname`（对象） | **已订正** | 模板实际读取的就是**扁平字段**：`getSafeAvatar(post.avatar, …)` 与 `post.nickname`（该页 L156/L163），与后端 `TopicPostVO` 的 `username/nickname/avatar` 一致；原报告基于旧版本代码 |

**★ 过程自纠**：本批又是"锚点带 dump 的 `]`"（Tab 那条）与"声明未接线"（`bizTarget` 只声明没按钮，eslint 报 unused）两处，均当场修正。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（54.99s）· 后端 **476/476** · 对账 **已修 400（P0 6 / P1 75 / P2 238 / P3 81）· 订正 20 · 不做 5 · 未修 457**

## v14.53 (2026-10-01) 全端评审落地（第 91 批）：岗位目标"默认/编辑"能力补齐 + 读者画像地域数据不再被截断

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-ek "设为默认岗位"根本没入口** | 岗位表单**没有该控件**，`createJobTarget` 从不提交 `isDefault` ⇒ 后端 `clearDefault` 分支**永不触发**（而 `ResumeJobTarget` 本就有 `isDefault` 字段、列表也已在渲染"默认"角标） | 表单新增「**设为默认岗位**」复选框（`isDefault` 进 `jobForm`），创建/更新时随请求提交；成功后提示区分"已创建并设为默认" |
| **P2-el 编辑岗位的能力闲置** | 后端 `PUT /portal/resume/optimize/job-target/{id}` 与前端 `updateJobTarget` **都已具备**，但工作台只提供**新建与删除** | 列表加「编辑」按钮 → `openJobModalForEdit(t)` 回填 → 保存时按 `jobEditId` 走 `updateJobTarget`；弹窗标题随编辑态变化 |
| **P2-em 地域分布基于被截断的数据** | 后端 SQL 是 `GROUP BY v.ip ... LIMIT 10`：**先取访问量最大的 10 个 IP**，再由 Controller 把这 10 个 IP 归并成省份 ⇒ 前端「**全 34 省份热力** + Top10 排行」实际只覆盖 10 个 IP 所属省份 | SQL 去掉 `ORDER BY value DESC LIMIT 10`，改为基于**全部 IP** 归并省份；并注明若作者体量极大应把"IP→省份"映射**下沉到 SQL**（或落维表）后按省份 GROUP BY，而不是截断 IP |

**★ 过程要点**：① `updateJobTarget`/`Pencil` 未导入 ⇒ `vue-tsc` + eslint 直接报 `TS2304`/`no-undef`，补导入后通过（`Pencil` 经核实**原本就有**，故该条无需改动）；② 后端 SQL 改动后跑全量 `476/476` 与门户 build 复核。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（54.00s）· 后端 **476/476** · 对账 **已修 397（P0 6 / P1 75 / P2 235 / P3 81）· 订正 19 · 未修 461**

## v14.52 (2026-10-01) 全端评审落地（第 90 批）：阅读器进度/读完判定 2 处 + 错题本提示与防重

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-eh 进度显示"分子大于分母"** | `progressText` 分子用 `chapter.chapterNo`（**全局章节号**），分母用 `chapters.length` —— 而 `getBookChapterList` **只返回 `isPublished=true`** 的章节，两者口径不同 ⇒ 存在未发布章节时出现「第 30 章 30 / 25」这类错值 | 分子改为取当前章节在**已发布章节列表**中的序号（列表里找不到时才回退全局章节号） |
| **P2-ei 导航加载失败被断言"整本书读完"** | `isLastChapter` 判据是 `if (!nav?.next) return true`，而 `nav` 在请求失败/非 200 时会被**置为 null**（`catch(() => null)` + `navResp` 校验）⇒ **加载失败也会显示"恭喜完成整本书的阅读"** | 改为只有"导航确实加载成功且没有下一章"才算读完；`nav` 未知时**不宣称**已读完 |
| **P2-ej 一次操作弹两条提示 + 批量无防重** | `handleAddToWrongBook` 既传 `successToast:'已加入错题本'` **又**手动 `toast.success('已加入错题本，可在错题本中复习')`；批量入口 `for` 循环调用且**无 loading/防重复** ⇒ 连点重复请求、逐题弹一堆提示 | 统一提示（批量场景由汇总提示接管，单条只弹一条）；加**在途防重**（单条 + 批量），批量结束汇总「已加入 N 题 / 部分失败」 |

**★ 过程要点**：本批两次小事故均被工具链/自查拦住 ——
① `isLastChapter` 的锚点又**误带 dump 的 `]`** 未命中 ⇒ 去掉后一次通过；
② `handleAddToWrongBook` 改为返回布尔后触发 `TS7030`（早退分支没返回值）⇒ 补 `return false` 并显式声明 `Promise<boolean>`。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（57.34s）· 后端 **476/476** · 对账 **已修 394（P0 6 / P1 75 / P2 232 / P3 81）· 订正 19 · 未修 464**

## v14.51 (2026-10-01) 全端评审落地（第 89 批）：发起话题 3 处 + 简历编辑 2 处

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-ec 放弃提交留下孤儿封面** | 封面是"**先上传拿到 fileUrl、再随话题提交**"的两步流程，而原先**只在用户手动点 × 时**才 `deletePortalFile` ⇒ 上传后直接离开页面（不点取消、不点 ×）时**没有任何清理** | 新增 `pendingUploadedCover`，在 `onBeforeRouteLeave`/`onUnmounted` **回收未提交封面**；**提交成功后清空该标记**（封面已入库，不能删） |
| **P2-ed 审核说明与后端行为相反** | 说明写"命中内容会**转人工重点审核**"，而 Controller 在写库前用同一敏感词表**直接 return error 拒绝创建**（Service 里"命中仍 pending、不阻断"的分支因前置拦截永不命中） | 文案改为「提交时会做敏感词校验，**命中将直接拒绝创建**，请修改后重试」 |
| **P2-ee 让用户去一个没有入口的页面** | 驳回引导写"可在「我的话题」查看并修改后重新提交"，而当时**全站没有任何链接**指向 `/topic/my/topics` | 引导文案后加「**前往我的话题**」直达按钮（配合前一批在话题列表页加入口） |
| **P2-ef 一次点击产生两条评分报告** | `handleScore` 先调 `scoreResume`（内部**已存档**一条报告），紧接着又调 `saveScoreReport` —— 而 `/score-report` 内部会**再执行一次 scoreResume 并存档** ⇒ 评分被重复计算、报告重复 | 去掉重复的 `saveScoreReport` 调用，仅刷新报告列表（并清理不再使用的导入） |
| **P2-eg "新建简历"残留上一份内容** | `watch(route.params.id)` 在 id 变空时**只重置 `form.id` 与 `form.title`**，`educations/works/projects/skills/selfIntro` 仍保留上一份简历的数据 | 切回新建态时**清空全部区块**（含评分字段），再反显个人中心信息 |

**★ 过程要点**：本批两次因**导入未同步**被 `vue-tsc`/eslint 拦下（`onUnmounted` 未导入、移除调用后 `saveScoreReport` 变成未使用），当场补齐；
另有两条索引条目因写了**目录前缀**（`interview/ResumeEditPage`）而对不上清单里的纯文件名，已统一修正（对账 +2）。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（1m1s）· 后端 **476/476** · 对账 **已修 391（P0 6 / P1 75 / P2 229 / P3 81）· 订正 19 · 未修 467**

## v14.50 (2026-10-01) 全端评审落地（第 88 批）：发现页 3 处 + 排行榜 3 处

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-dw 有数据却"整页空白且无提示"** | `hotRanking` **只在空态条件里被引用、模板中没有任何区块渲染它**。当后端只返回 `hotRanking`（运营只配了首页热门位，banner/限免为空且无最近更新）时：空态条件因 `hotRanking` 非空**不成立**，其它区块各自 `v-if` 隐藏 ⇒ 页面只剩「排行榜」标题区，**既无内容也无提示** | 从空态条件中**移除 `hotRanking` 子句**（该字段无渲染方），使该情形正确落到"暂无发现内容"提示 |
| **P2-dx 排行 Tab 竞态** | `loadRanking` 直接覆盖写 `rankingList`，**无请求序号/取消**，且 Tab 在 `rankingLoading` 期间仍可点 ⇒ 连点「热门→字数→完结」时先发后到会把**旧类型数据写进当前 Tab**（`client.ts` 去重只对完全相同 URL 生效） | 加 `loadSeq` + `activeRankingTab` 校验，过期响应丢弃 |
| **P2-dy 排行失败与"暂无数据"同态** | `catch` 只 `console.error` 后置空 ⇒ 与「暂无排行数据」同一分支，无重试 | 新增 `rankingError` 失败态（**排在加载/空数据之前** + 重试） |
| **P2-dz 新用户引导卡永不出现** | 后端只要登录就回填 `myValue`/`mySubmitCount`（无提交时为 **0**、`myRank` 为 null），而前端判空写成 `myRank == null && myValue == null` ⇒ `myInfo` **恒为真**，`v-else` 的「去刷第一道题」引导卡永不出现，卡片显示"#— / 0 题" | 判空改为"确实无数据"（无排名 + 值为 0 + 无提交） |
| **P2-ea Tab 切换串榜单** | `watch(activeType, loadLeaderboard)` 无守卫，且**从不校验响应里的 `type`** ⇒ 错序时「刷题积分」页会一直显示通过题目数榜单 | 加 `loadSeq` + 校验响应 `type`（双保险） |
| **P2-eb 前三名头像环颜色全失效** | `:class="`ring-${rankStyle(item.rank)?.ring}`"` —— `rankStyle` 返回的已是完整类名（`ring-yellow-400`），再拼一次前缀得到 **`ring-ring-yellow-400`**，Tailwind 扫不到该字面量 ⇒ 金银铜环色**全部失效**（`null` 时还会拼出 `ring-undefined`） | 直接绑定 `rankStyle(item.rank)?.ring` |

**★ 过程自纠（本批 2 次半成品，均当场修掉）**：
① DiscoverPage 的"过期响应守卫"首次插入时**缩进与结构错乱**，且 `rankingError` 只声明未赋值/未渲染 ⇒ 重写该段并补失败态区块；
② 空态条件那条首版**只写了注释、没改条件本身** ⇒ 复查后真正移除 `hotRanking` 子句。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（1m9s）· 后端 **476/476** · 对账 **已修 386（P0 6 / P1 75 / P2 224 / P3 81）· 订正 19 · 未修 472**

## v14.49 (2026-10-01) 全端评审落地（第 87 批）：语音面试记录 3 处 + 编程练习 3 处

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-dq 覆盖性操作"零确认"** | 历史页按钮只 `router.push({ regenerate:'1' })`，报告页据此走 `handleRegenerateReport(true)` 的 **skipConfirm**（注释写"入口处已确认过"）—— 而**历史页从未弹过任何确认**；该操作会**清空原报告重跑**、后端还带 `@RateLimiter(voice:regenerate, 10/小时)` | 历史页入口加**二次确认**（说明"覆盖当前报告 + 每小时最多 10 次"），确认后才跳转 |
| **P2-dr 失败被当成"还没有面试记录"** | `loadList` 只有 loading 与空态，`catch` 仅 `toast.error` ⇒ 失败后渲染「还没有面试记录 / 开始第一场面试」 | 新增 `loadError` 并在 `catch` 记录（失败态与空态分离） |
| **P2-ds 列表随响应下发整场报告（体积）** | `/portal/interview/voice/my/list` 直接返回 `Page<PortalVoiceInterview>` **实体**，含 `report`（整场报告 JSON，单条数十 KB）、`contextSnapshot`、`profileSnapshot`、`questionPaper`、`configJson`、`introScoreJson`、**`shareToken`** 等；前端只用 10 个轻字段 | 后端 `listMy` 返回前**清空重字段**（含 `shareToken`，避免列表泄露可分享入口）；仅作用于查询结果对象，不回写库 |
| **P2-dt 提交记录刷新即空** | `submissionHistory` 只是组件内 ref，仅 `handleSubmit` 时 `unshift` ⇒ 刷新/切题清空 | 用题目详情接口自带的 `mySubmissions` 回填（跨刷新/切题保留） |
| **P2-du 判题样例"回填错样例"** | 用 `sampleCases.find(tc => tc.orderNum === cr.caseIndex)` 匹配 —— 但 `caseIndex` 是判题引擎**1 起执行序号**（`ProcessJudgeEngine` 用 `i+1`），`orderNum` 是用例表**自身排序字段**，两者口径不同 | 改为按执行序号在样例数组中的**位置**对齐（`sampleCases[caseIndex - 1]`） |
| **P2-dv 游客可写代码后才发现要登录** | 路由标 `isPublic`（游客可浏览），但判题接口要求登录（`PortalJudgeServiceImpl` 在 userId 为空时抛错），页面**没有任何登录判断** | 提交前加登录门禁 + `redirect` 回本页 |

**★ 过程自纠（本批 4 次，均为"半成品/锚点"类）**：
① 后端项第一版**只加了 Javadoc 没写实现** → 撤下重做，真正在 `listMy` 里裁剪字段；
② `M3`/`C3` 锚点分别错在**缺 `* ` 前缀**与**误带 dump 的 `]`** → 修正后命中；
③ 因 `M3` 未命中而 `M4` 已写入 ⇒ 文件一度出现"引用未声明的 `loadError`"，随即补上声明；
④ `SubmissionRecord` 字段与历史提交字段不同名、`isAuthenticated`/`useConfirmModal` 未导入 ⇒ 由 `vue-tsc` 全部抓出并修正。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（53.72s）· 后端 **476/476** · 对账 **已修 379（P0 6 / P1 75 / P2 218 / P3 80）· 订正 19 · 未修 479**

## v14.48 (2026-10-01) 全端评审落地（第 86 批）：发布页 3 处 + 面经详情评论区 2 处

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-dl 手动保存"看起来没在保存"** | `isSaving` **只在 `isAuto=true`（自动保存）时置位**，而手动保存按钮 `:disabled="isSaving"` ⇒ 手动保存期间按钮**始终可点**；后端 `saveDraft` 是"先按 `sessionToken` 查、没有再 insert"的**两步幂等**（无 `@RepeatSubmit`），并发两次存在**都查不到而各插一条**的窗口 | 手动保存也进入保存态（`if (isSaving.value) return;` + `isSaving = true`）⇒ 按钮真正禁用、堵住并发双插窗口 |
| **P2-dm 驳回/归档被显示成"已发布"** | 顶部徽章三元只识别 `draft`/`pending`，其余**一律落"已发布"+ 绿色**；而编辑模式下 `articleStatus` 直接取自 `article.status` ⇒ `rejected`/`archived` 都显示「已发布」，作者误以为文章已上线 | 徽章补齐 4 态（草稿/审核中/已发布/**已驳回**/**已归档**）并各自配色，文案改为映射表 |
| **P2-dn 注释声称"覆盖所有离开路径"，实际只覆盖站内路由** | `onBeforeRouteLeave` 只拦站内跳转，全站 `beforeunload` 仅判断 AI 慢请求、不判断未保存内容 ⇒ **关标签/刷新/直接改地址会静默丢内容**，而注释写着"覆盖面包屑跳转、浏览器后退等**所有**离开路径" | 补 `beforeunload`（`onMounted` 注册 / `onUnmounted` 注销，判空与只读态跳过），并**订正注释**为"路由级 + 浏览器级各覆盖什么" |
| **P2-do 子评论"回复 @某人"恒不显示** | 前端读 `reply.replyToUser.nickname`（**对象**），后端 `InterviewCommentVO` 只有 `replyToUserNickname`（**字符串**），且 `toCommentVO` 仅 `BeanUtils.copyProperties`、实体无该字段 ⇒ 永远不显示 | 按后端实际字段读取（`replyToUserNickname`），保留旧结构兼容 |
| **P2-dp 评论区固定 50 条、计数用已加载条数** | `getCommentList` **固定 `pageSize:50`、不传 `pageNum`**，无分页控件/加载更多；标题计数用 `comments.length`（已加载条数）而非接口 `total` | 改为**分页加载**（20/页 + 「加载更多评论」按钮），标题计数改用接口 `total` |

**★ 过程自纠**：本批再次出现两处"半成品"并当场修掉 ——
① `loadMoreComments()` 只声明**未接线**（没有按钮调用，eslint 报 unused）⇒ 补上「加载更多」按钮；
② 子评论字段对齐的**锚点缩进不符**未命中 ⇒ 改用按行定位改写（脚本按行号精确插入/删除）。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（52.15s）· 后端 **476/476** · 对账 **已修 372（P0 6 / P1 75 / P2 212 / P3 79）· 订正 19 · 未修 486**

## v14.47 (2026-10-01) 全端评审落地（第 85 批）：**分享报告脱敏（免登录路径）** + 最近错题标题 + 计划数封顶

**依据**：继续 A 口径；本批 3 条均在**后端**，且前两条一条是隐私、一条是"永远取不到值"。

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-di 免登录分享接口返回完整报告（隐私）** | `getSharedReport` 落在 `PortalSecurityConfig` 的 `/portal/interview/**` **permitAll 白名单**内、控制器 `@Operation` 也声明"**脱敏**，不含用户信息"，但实现只是 `selectOne` → `parseReport(interview)` **直接返回完整报告**：`VoiceInterviewReportVO` 含 `candidate`（姓名/技能/简历自述/AI 评分）与 `jobInfo`（岗位/JD/匹配度）。页面虽未渲染，**接口本身可被直读**，全仓库无任何裁剪逻辑 | 返回前**裁掉 `candidate` 与 `jobInfo`**（其余评估维度保留，分享卡片信息不缺失） |
| **P2-dj 最近错题的题目标题恒为 null** | `listRecentWrong` 只手工映射 `portal_wrong_question` 自身字段，而其 mapper `selectRecentWrong` **没有 JOIN 题库** ⇒ `questionTitle` 恒 null，前端只能回退显示「题目 #123」 | 改为**一次 `selectBatchIds` 批量取题库**后按 `questionId` 关联，回填 `questionTitle` / `questionDifficulty`（同时消除逐条查询的隐患）；顺带为该 Service 注入题库 Mapper |
| **P2-dk "进行中计划数"永远封顶 5** | `activePlanCount` 由 `listMyPlans(userId, "active", 1, 5)` 的**记录条数**得出 ⇒ 计划超过 5 个时页面永远显示 5 | 改用**分页 `total`**（真实总数），前 5 条仍用于"今日计划"展示 |

### ★ 过程自纠

改 `activePlanCount` 时，第一次只把调用改为取 `Page` 对象、**却漏改下面仍在用 `size()` 的赋值**（典型"半成品"）；
复查时发现并补上 `getTotal()`，否则本项等于没修。

### 三、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B test` | ✅ **476/476** |
| 编译期验证 | ✅ `report.setCandidate/setJobInfo(null)`、`vo.setQuestionTitle/setQuestionDifficulty`、`activePlanPage.getTotal()` 均通过编译（setter 存在性由编译器保证） |
| 门户 `vue-tsc` / `eslint` / build（本批无前端改动，作回归） | ✅ 0 / **0 problems** / build ✓ |
| 对账重算 | ✅ **已修 367（P0 6 / P1 75 / P2 207 / P3 79）· 订正 19 · 未修 491** |

## v14.46 (2026-10-01) 全端评审落地（第 84 批）：分享报告 3 处（含死 UI）+ 学习中心 2 处

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-dd 分享页渲染恒为 null 的字段（死 UI）** | 页面保留「相关知识点」区块与 `kpItems` 的**双形态兼容解析**（string / `{title,desc}`），但后端已在报告生成处**显式移除**该产出（`VoiceInterviewServiceImpl` 注释："题库 tags 聚合对 agent 自由面试无参考意义，前端 Tab 已删"），全仓库再无 `setKnowledgePoints` ⇒ 字段恒为 `null` | 移除该区块与解析逻辑（改注释说明为何删） |
| **P2-de 同一分数两处不同等级** | 分享页 `levelText` 用 **85**/70/60 生成「优秀/良好/合格/待提升」，而**同一份报告**在站内主报告页 `scoreLevel` 用 **80**/70/60 | 阈值统一到 **80/70/60**（与主报告页、`scoreClass` 口径一致）；并注明后端另有结构化 `levelEstimate`（junior/mid/senior），彻底统一属后续项 |
| **P2-df 公开页没有任何 SEO/分享卡片** | 本页是**免登录公开页**（路由 `requiresAuth:false` 且声明 `robots`），但全仓库**没有任何代码读取 `route.meta.robots`**，而本页原先**完全没有 `useHead`** ⇒ 无 robots、无可被社交平台抓取的 title/description/og | 补 `useHead`：title + description + `robots: noindex,nofollow` + `og:*`（noindex 是刻意选择：分享链接不应被搜索引擎收录，但需要社交卡片信息） |
| **P2-dg 错题卡片口径与直觉不符** | 卡片写「错题本」并显示 `dashboard.wrongCount`，而后端用 `countWrong(userId, null)` 统计的是**全部错题（含已掌握）** | 文案改为「**错题总数**」并注明实际口径（避免数字被误读；"仅未掌握"的口径需后端另出字段） |
| **P2-dh 未登录却按真实数据样式显示全 0** | 未登录时后端返回**全 0 骨架**（`loggedIn=false`），但四张统计卡片仍渲染「累计答题 0 / 通过率 0% / 连续打卡 0 天」，只有今日计划与错题区做了"登录后查看" | 未登录时四张卡片统一显示「**登录后查看**」 |

**★ 过程自纠**：补 SEO 时第一版脚本打算**前置一个新的 `<script setup>` 块** —— 而该文件已有 script setup，
那样会造成**重复声明**（report/loading/loadReport 等全部重定义）。执行前发现并撤下该条，
改为在**原有 script 块内**补 `useHead` 导入与调用。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（54.27s）· 后端 **476/476** · 对账 **已修 364（P0 6 / P1 75 / P2 204 / P3 79）· 订正 19 · 未修 494**

## v14.45 (2026-10-01) 全端评审落地（第 83 批）：专栏编辑 3 处 + 举报页 3 处 + **运行库字典补齐**

### 一、专栏编辑页

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-cy 改了封面不保存 ⇒ 旧封面已被删** | 上传新封面/点「清除」时**立即 `deletePortalFile` 物理删除旧文件**，而封面字段只在点「保存」时才落库 ⇒ 用户不保存（或保存失败）时，旧封面已没了，专栏指向不存在的图片 | 改为**延后清理**：先记入 `pendingDeleteCover`，**保存成功后**才真正删除 |
| **P2-cz 等实名提示期间可重复提交** | `submitting` 在 `await promptRealNameOptional()` **之后**才置 true ⇒ 提示弹窗/等待期间按钮与表单仍可再次提交（后端 `save` 也无防重） | 提前置位 + 在途防重 + 早退分支复位 |
| P2 | 分类ID 手填且无校验（后端 `categoryId` 为 `Long`） | 保留手填（全站暂无专栏分类体系）并加注释说明；**非数字会触发后端转换异常**的口径已写明 |

### 二、举报/反馈页

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-da 移除图片不删后端文件** | `removeImage` 只 `splice` 本地数组，**不调用后端删除** ⇒ 已上传文件成为孤儿（占存储、后台文件管理里堆积） | 改为先确认再 `deletePortalFile` 清理，失败仅告警不影响本地移除 |
| **P2-db 举报目标上下文无处可传** | 后端与接口都支持 `targetType/targetId`（`buildReportExtra` 写入审核详情），但页面只有自由文本 `targetUrl`、也不读 `route.query`，仓库内也没有带参跳转 ⇒ 从内容页举报只能手抄 URL | 从 `route.query` 接收 `targetType/targetId`（**收敛到 `ReportTargetType` 联合类型**，非法值忽略）并随提交上报 |
| **P2-dc 字典有类型无字典项** | 运行库中 `cms_report_type`/`cms_feedback_type` **没有任何字典项** ⇒ `useDictData` 永远返回空数组，页面 **100% 走本地兜底常量**，后台也无法维护选项 | 见下（含一处**重要澄清**） |

### 三、★ 重要澄清：初始化脚本本来就是对的，缺的是"运行库"

核对 `init-sql/moyun-db-dml-init.sql` 发现：**这 9 条字典项（举报 5 + 反馈 4）本就在初始化脚本里**（L655-659 / L666-669）。
也就是说，问题不是"种子漏写"，而是**存量运行库没有这些行**（初始化早于这些种子行，或从未重跑种子）。

因此本批新增增量脚本 **`20261001-08-举报与反馈类型字典项登记（v14.45）.sql`**：
① 按 `(dict_type, dict_value)` 幂等补齐缺失项；② 用 `UPDATE` 把取值**对齐到 DML 逐字一致**（避免新装库与存量库漂移）。

| 校验 | 结果 |
|---|---|
| 连续执行两次 | ✅ 均为 **举报 5 项 / 反馈 4 项**（幂等） |
| 与 DML 取值一致性 | ✅ `spam=warning/default=Y`、`bug=danger` 等逐字一致 |
| `init-sql/moyun-db-dml-init.sql` | ✅ **无需修改**（本来就含这 9 条） |

### 四、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 54.04s` |
| 后端 `mvn -o -B test` | ✅ **476/476** |
| 增量 SQL 幂等 + 与 DML 一致性 | ✅ 见上 |
| 对账重算 | ✅ **已修 359（P0 6 / P1 75 / P2 199 / P3 79）· 订正 19 · 未修 499** |

## v14.44 (2026-10-01) 全端评审落地（第 82 批）：编程练习页 3 处 + 语音面试页 3 处

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-ct 编程练习切题无竞态守卫** | `loadQuestion` / `loadSampleCases` / `loadNeighbor` **都没有请求序号或取消机制**，快速连点「上一题／下一题」或浏览器前进后退时多请求并行，**先发慢响应覆盖新题**（题面、样例用例、相邻导航互相错配） | 加 `loadSeq` 序号守卫（响应/catch/finally 收口）+ 相邻导航按**题号**做失效校验 |
| **P2-cu 窄屏被裁且无法滚动** | 根容器固定 `height: calc(100vh - 56px)` 且 `.coding-practice-page{overflow:hidden}`，左右两栏分别 `min-width:320px/400px`，既无横向滚动也不堆叠 ⇒ 小屏下右侧编辑器被裁掉 | 加 `<1024px` 响应式：解除固定高度与裁切、两栏**纵向堆叠**、宽度自适应（给两栏补 `.coding-pane-left/right` 类名以便选择） |
| **P2-cv 等转写期间按钮仍可点** | `submitting` 在 `await whenTranscriptionDone()` **之后**才置 true ⇒ 等待转写期间「回答完毕」仍可点，**连点并发提交** | 提前置位 + 在途防重 + 早退分支复位 |
| **P2-cw 权益"未配置"被当成"已用完"** | `benefitLeft(...) ?? 0` ⇒ 权益未在 free 档配置时返回的 `null` 被当作 **0（已用完）**，直接把用户拦到开通弹窗（与后端 `@VipOnly` 的真实额度不是同一事实来源） | 仅**显式 0** 才提示用完；`null/undefined` 视为"次数未配置"，交由后端门禁判定 |
| **P2-cx "下载 PDF"其实是打印** | 按钮写「📥 下载 PDF 报告」、提示写"正在生成 PDF 报告"，实现却是 `window.print()` ⇒ 得到的是**浏览器打印对话框**，不是文件下载 | 函数注释、提示语与按钮文案统一改为「**打印 / 另存为 PDF**」，与实际行为一致 |

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（54.85s）· 后端 **476/476** · 对账 **已修 353（P0 6 / P1 75 / P2 193 / P3 79）· 订正 19 · 未修 505**

## v14.43 (2026-10-01) 全端评审落地（第 81 批）：搜索页 6 处 P2（"还没搜就显示无结果"等）

**依据**：继续 A 口径，一页多条的页面成批处理。

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-cn 还没搜就显示"未找到相关内容"** | `hasQuery` 直接读 **v-model 绑定的输入框** ⇒ 用户刚敲一个字符（**还没回车、没点搜索**）就为 true，而请求只在回车/点击/路由变化时才发，空态立刻出现 | `hasQuery` 改按**路由参数（真正已提交的检索条件）**判定 |
| **P2-co「浏览全部」清不掉条件** | 空态按钮只清 `searchQuery`/`selectedCategory` 两个局部 ref，**不清 `route.query`** ⇒ 标签分支仍从 URL 取参、`hasQuery` 仍为 true，`performSearch` 提前 return：**列表被清空但 URL、面包屑、检索条件全不变** | 改调 `clearAllFilters()`：同时清本地状态并 `router.push('/search')`（URL 归零） |
| **P2-cp 失败与"无结果"同态** | 三个加载函数 `catch` 后仅 `console.error` 并清空数组 ⇒ 与"确实没有结果"走**同一个空态分支**，无提示无重试 | 新增 `searchError` + **失败态区块**（排在结果/空态之前 + 重试），成功时清空 |
| **P2-cq 搜索按钮可能是"假刷新"** | `handleSearch` 只 `router.push`，靠 watch 触发请求；**query 与当前完全相同时 route.query 不变、watch 不触发** ⇒ 列表还是第 3 页数据，分页控件却显示第 1 页 | 条件未变时**显式 `performSearch()`**；变化时仍走 watch（避免重复请求） |
| **P2-cr 占位文案超出后端能力** | 文案承诺「搜索文章、标签或作者」，但 keyword 分支后端只做 `title like OR excerpt like`，**没有作者维度** ⇒ 按作者名搜必然空结果 | 文案改为「搜索文章标题或摘要...」（与后端能力一致） |
| **P2-cs 侧栏热门拉整包** | 为右侧 5 条热门调用首页聚合接口 `/portal/article/home`（一次 4 次查询：轮播全量 + 精选 8 + 热门 10 + 最新 20，约 40 条只用 5 条） | 改用文章列表接口 `pageSize=5` + `sortBy=views` 倒序 |

**★ 过程自纠**：本批两次脚本事故都已拦住 ——
① 第一版脚本自身**语法错误**（模板串里混入 `*/`）导致整脚本未执行，**文件未被修改**；
② 修正后**结果区锚点缩进差 2 格**未命中，脚本的"原子性"设计（任一未命中则不写文件）使文件保持原样，改锚点后一次通过。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（55s）· 后端 **476/476** · 对账 **已修 348（P0 6 / P1 75 / P2 188 / P3 79）· 订正 19 · 未修 510**

## v14.42 (2026-10-01) 全端评审落地（第 80 批）：书架页 7 处（含**打开书架虚增阅读量**这一数据污染）

**依据**：继续 A 口径。本批集中在 `MyBookshelfPage` —— 一页有 6 条 P2，成批处理。

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-ch 打开书架把每本书的阅读量刷高（数据污染）** | 页面为渲染书架，对当前页每条记录调用 `GET /portal/reading/books/{id}`，而该接口**内部 `incrementReadingCount` 并直接落库**（select→+1→update）⇒ 用户每次打开书架都在给收藏的书刷阅读量 | **后端书架列表接口改为一次 `listByIds` 批量下发 `bookTitle/bookCover/bookAuthor`**，前端不再调用书籍详情 ⇒ **不再触发自增** |
| **P2-ci 书架列表 N+1** | 同上一因：`Promise.all(items.map(getBookDetail))` 每条一次请求 | 同一修复一并解决（1 次列表 + 1 次批量取书） |
| **P2-cj 空态 CTA 从未渲染（组件 API 误用）** | 写的是 `<Empty action-text="去发现" @action="..." />`，但 `Empty.vue` 只声明 `title/description/size`，动作区是**名为 action 的插槽** ⇒ 属性落到根 div 上、事件永不触发，**CTA 永远不出现** | 改用 `<template #action>` + 按钮（真正可渲染可点击） |
| **P2-ck 失败被说成"书架空空如也"** | `catch` 里把 `bookshelfList` 与 `total` 一起清零 ⇒ 失败被渲染成 Empty「书架空空如也」 | 新增 `loadError` + 失败区块（**排在空态之前** + 重试），空态加 `&& !loadError` |
| **P2-cl 私有页未输出 noindex** | 路由 `meta.robots` **全站无消费方**（守卫只读 `requiresAuth/title`），页面 `generateSeo` 未传 robots ⇒ 私有页可被索引 | `generateSeo` 显式传 `robots: 'noindex,nofollow'` |
| **P2-cm 移出后停在越界页** | 移出后直接重载当前 `pageNum`，末页最后一条被移出时停在空页 | 移出时若本页仅剩 1 条则回退一页；`loadBookshelf` 内再加**兜底校正**（列表空、`pageNum>1`、`total>0` 时回退重载） |
| P3 | 移出用原生 `window.confirm`（项目有全局 `useConfirmModal`） | 改用统一确认弹窗（与 ColumnDetailPage/MyResumesPage 一致） |

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（1m7s）· 后端 **476/476** · 对账 **已修 342（P0 6 / P1 75 / P2 182 / P3 79）· 订正 19 · 未修 516**

## v14.41 (2026-10-01) 全端评审落地（第 79 批）：收藏/答题列表 **N+1 消除** + 征文文章 ID 校验 + 「我的话题/我的观点」入口

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-cd 收藏列表 N+1** | `selectBookmarkPage` 在流里**逐条** `questionMapper.selectById` ⇒ 收藏 200 条即 **201 次 SQL** | 改为**一次 `selectBatchIds` 批量取回**再用 `Map` 关联（1+1 次查询）。**说明**：该方法仍是"先全量查出再内存 skip/limit"，要彻底解决需后端分页 SQL，已记入对账（本次先消除 N+1 这一最重项） |
| **P2-ce 我的答题记录同款 N+1** | `selectMySubmissionList` 同样逐条 `selectById` 取题目标题/难度 | 同上，改批量取回 + `Map` 关联 |
| **P2-cf 征文"文章ID"是纯文本框** | `articleIdInput` 无任何格式校验，非数字直接提交 ⇒ 后端 `Long` 参数转换异常 | 增加 `/^\d+$/` 校验，提示中指引可在「我的文章」查看 ID |
| **P2-cg 两个页面全站无入口** | `/topic/my/topics` 与 `/topic/my/posts` **全站无任何链接**（只有路由定义与页面 canonicalPath），用户无处进入 | 话题列表页工具栏上方增加「我的：**我的话题** / **我的观点**」入口 |

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓（1m7s）· 后端 **476/476** · 对账 **已修 335（P0 6 / P1 75 / P2 176 / P3 78）· 订正 19 · 未修 523**

## v14.40 (2026-10-01) 全端评审落地（第 78 批）：章节归属校验 + 禁止给自己的投稿投票

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-cb 章节可以"张冠李戴"** | 后端 `getChapterDetail` **只按 chapterId 查询**，不校验章节是否属于 URL 里的 `bookId` ⇒ `/reading/book/A/chapter/B的章节` 也能打开，出现"书是 A、内容却是 B" | 阅读器拿到章节后**比对真实 `bookId`**，不一致则用**正确地址** `router.replace`（保持在阅读流里，不报错打断） |
| **P2-cc 可以给自己的投稿投票** | 后端 `toggleVote` **只按 submissionId 查重投票记录，没有任何自投限制**；前端也未处理 ⇒ 作者可自投刷票数 | 前端先行拦截：`isOwnSubmission()`（对比 `sub.userId` 与当前用户）+ `handleVote` 拒绝 + 按钮禁用 + 提示语；**后端补门禁已记入对账**（需单独改 Service） |

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓ · 后端 **476/476** · 对账 **已修 331（P0 6 / P1 75 / P2 173 / P3 77）**

## v14.39 (2026-10-01) 全端评审落地（第 77 批）：排行榜"名不副实"两处（后端补类型）+ 通过率错值

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-bz 「最近更新」其实是"新书上架"** | 该区块调用 `getRanking('new')`，后端 `new` = `order by create_time desc`，**并非 `last_update_time`**；标题与注释都按"最近更新"宣传 | 后端补排行类型 **`updated`**：`BookQuery.orderBy='update'` + mapper `order by last_update_time desc`；前端改用它 |
| **P2-ca 「连载中」未过滤连载状态** | 该区块调用 `getRanking('word_count')`（= `type=novel` + 字数倒序），**完全没有 `serial_status='ongoing'` 过滤**，已完结长书会排进"连载中"；`BookQuery` 甚至没有 `serialStatus` 字段 | 后端 `BookQuery` **新增 `serialStatus`** + 排行类型 **`ongoing`**（`serial_status='ongoing'`，排序按最后更新）；前端改用它 |
| **P2-by 通过率显示成 4500%** | 后端 `acceptance_rate` 存的就是 **0-100**（`successSub * 100.0 / totalSub`，已核对源码），而 `UserPage` 又 `* 100` | 去掉重复乘 100，并夹取到 `0-100`（`MyBookmarksPage` 原本直接渲染，口径正确） |

> 新增/变更的前端类型：`RankingType`、`getRanking()` 联合类型均补 `'updated' | 'ongoing'`。
> **SQL 层实测**：新排序与新过滤条件在真实库执行通过（`last_update_time` / `serial_status` 列存在；本库书籍表为空故 0 行）。

## v14.38 (2026-10-01) 全端评审落地（第 76 批）：一批 13 处（书籍详情/读书空间/征文/答题记录/话题/面经/动态）

**依据**：继续 A 口径，**同一轮内跨 7 个文件集中处理**（减少来回）。

| # | 缺陷 | 修复 |
|---|---|---|
| B1 | `BookDetailPage` 出版日期用 `formatShortDate` ⇒ `LocalDate` 显示成无意义的 `00:00` | 改用 `formatDate(date, 'YYYY-MM-DD HH:mm', 'YYYY-MM-DD')`（内置"全 0 时刻回退日期格式"） |
| B3 | 章节目录固定 `slice(0,12)` ⇒ **第 13 章及以后在详情页无法选择** | 增加「展开全部 N 章 / 收起」切换 |
| R1 | 「精选书单」右侧「查看更多」**没有任何 `@click`**（纯装饰按钮） | 补跳转 `/reading/discover` |
| R2 | 首页六个区块全部 `v-if="length > 0"` ⇒ 数据全空时**整页只剩标题与页脚** | 新增首页空态（说明 + 「去发现 / 重试」） |
| C1/C3 | 投票按钮无**在途锁** ⇒ 连点/双击发两次 toggle | 新增 `votingId`，绑到 `disabled` |
| C4 | 投稿成功后 `loadDetail()` 置 `loading=true` ⇒ **整页被加载圈替换** | `loadDetail(silent)` 静默刷新（并把 `@click="loadDetail"` 改为 `loadDetail()`，否则 PointerEvent 会被当作 silent 传入 —— 由 `vue-tsc` 抓出） |
| M1 | `MyAttemptsPage` 难度读 `sub.question?.difficulty`/`sub.difficulty`，而后端字段名是 **`questionDifficulty`** ⇒ 徽章恒"未知" | 取值路径对齐后端 |
| M2 | 通过判定忽略后端已下发的**权威字段 `passed`**，`isSuccess` 为 null 时一律判"未通过" | 优先用 `passed` |
| M3/T1/X3 | 三个列表**不校正越界页码**（total 变小/带 `?page=99` 时返回空页并被当成"无数据"） | 返回后校正页码并重载 |
| X1 | 面经置顶徽标外层容器高度为 0 ⇒ 徽标**浮到标签行上方** | 徽标放进封面容器（有封面叠在封面上） |
| F1/F2 | 动态类型只映射 4 种，而后端实际产出 **10 种**（未映射的一律显示"有了新动态"）；其中 `checkin` **全后端无写入点**（死分支） | 补齐 6 种文案与图标、移除死分支 |

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓ · 后端 **476/476**

## v14.37 (2026-10-01) 全端评审落地（第 75 批）：**竞态/重复提交**一类收口（5 处）

**依据**：继续 A 口径。本批全是"并发下状态错乱/重复写入"，按同一手法处理：**在途互斥 + 过期响应校验**。

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-bp 单字段重新生成无互斥** | `regenerateItem` 全程无"正在生成"判断 ⇒ 连点两张建议卡的「重新生成」会**并发请求**，后发者覆盖 `regenTargetIdx`/`regenCandidates`，候选版本落到**另一条**字段上 | ① 在途（`regenLoading`）直接拒绝并提示；② 响应回来时校验 `regenTargetIdx === idx`，**目标已切换则丢弃** |
| **P2-bq 评论/回复连点重复提交** | 详情页评论与回复提交**全程无"提交中"状态**，按钮只按内容非空禁用 ⇒ 连点重复入库，仅靠后端 `@RepeatSubmit(3s)` 兜底（3 秒窗口外照样重复） | 新增按 **`targetType:targetId`** 维度的 `commentSubmitting`（不同评论框互不影响），提交前判在途、`finally` 释放；按钮 `disabled` 条件**合并**在途与内容非空 |
| **P2-br 上传/OCR 中仍可提交认证** | `handleSubmit` 只防 `submitting`，**未校验 `uploading`/`ocrLoading`** ⇒ 图片上传（含 OCR）进行中可点「提交申请」，把**未回填**的半成品提交 | 在途则提示"图片上传或识别中，请稍候再提交"并中止 |
| **P2-bs 切题时精选笔记/相邻题目被旧响应覆盖** | `loadFeaturedNotes` / `loadNeighbor` 是 fire-and-forget，慢响应回来会覆盖**新题**的笔记与相邻导航 | 请求前记录题号，响应后若 `questionId` 已变则**丢弃** |
| **P2-bt 选择题切题并发覆盖** | `watch(route.params.id) → loadQuestion()` 无序号守卫 ⇒ 先发慢响应后到会覆盖新题，**题干与选项错配** | 加 `loadSeq` 序号守卫，过期响应直接返回 |

### ★ 过程自纠（本批踩了两次，都已修正）

1. **编辑结构错误**：第一版给评论提交加 `finally` 时写了一个"伪函数尾巴"（`async function _unusedCommentTail`），
   会把原 `catch` 块弄坏 —— 复查发现后**整段废弃重做**（先读原函数完整尾部再改）。
2. **重复属性**：给按钮加 `:disabled` 时没注意**该按钮已有 `:disabled`**（内容非空判断），
   产生 `Duplicate attribute 'disabled'`（`vue-tsc` TS1117 + eslint 双报）。修正为**合并成一个条件**；
   合并脚本第一版又因 `findIndex` 遇到已置 `null` 的行而抛错（未写入文件，安全），加空值判断后通过。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 1m 6s` |
| 后端 `mvn -o -B test` | ✅ **476/476** |
| 对账重算 | ✅ **已修 310（P0 6 / P1 75 / P2 154 / P3 75）· 订正 19 · 不做 5 · 未修 548** |

## v14.36 (2026-10-01) 全端评审落地（第 74 批）：打卡口径错位 + 删除后页码 + 切通道验证码残留（另 2 条复核订正）

**依据**：继续 A 口径。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-bm 打卡用"今日"语义去加减"累计"数** | 服务端 `recordTodayProgress` 返回的是**今日完成数**（clamp ≥0），而 `doneCount` / `progressPercent` 是**累计**口径。前端却用 `delta` 直接本地加减累计数 ⇒ **今日完成数为 0 时点减号，服务端不变而前端仍减 1**，累计数与百分比随之漂移 | 打卡成功后**以服务端为准重新拉取列表**（今日数/累计数/百分比/连续天数全部对齐）；并把「减号」在 `todayDoneCount <= 0` 时**禁用**，避免无意义请求 |
| **P2-bn 删除后不纠正页码** | 删除只 `loadPlans()`：删掉**末页最后一条**时 `page` 仍指向超界页（后端分页未开启溢出纠正 ⇒ 空 records），模板进入空态显示「还没有学习计划，开始创建第一个吧」，**用户以为计划全没了** | 删除后先纠正页码（`page > 1` 且本页仅剩 1 条则回退一页）再加载 |
| **P2-bo 切换通道不清验证码** | `switchMethod` 注释明确写"验证码/密码字段保留"，但邮箱与短信通道的验证码在后端是**独立生成、独立限流**的 ⇒ 保留会让用户把**邮箱验证码提交给短信重置接口**（必然失败并浪费一次校验） | 切换即清空 `form.code`，并**复位倒计时**（另一通道有自己的限流窗口，沿用上一个通道剩余秒数会误导"还没到重发时间"） |

### 二、复核后归位（2 条，原报告基于旧代码）

| 条目 | 归位 | 证据 |
|---|---|---|
| `MyResumesPage` 并发操作锁 | **已订正** | 简历操作已有 `actionId` 在途锁：`if (!r.id || actionId.value) return;`，导出/删除/评分/复制共用 |
| `MyResumesPage` 导出后的本地状态更新 | **已订正** | 导出成功后已 `findIndex` + 展开覆盖更新列表项的 `fileUrl`/`exportTime` |

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 54.87s` |
| 后端 `mvn -o -B test` | ✅ **476/476** |
| 对账重算 | ✅ **已修 305（P0 6 / P1 75 / P2 152 / P3 72）· 订正 19 · 不做 5 · 未修 553** |

## v14.35 (2026-10-01) 全端评审落地（第 73 批）：**"自动登录却让你再登录"** + 两处裸定时器 + 假验证徽章 + 长度/日期校验

**依据**：继续 A 口径。本批 6 条集中在注册/找回/个人资料/学习计划。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-bg 注册成功却提示"请使用新账户登录"** | `store.registerWithApi` 成功后**已经 `setToken` 并写入 `user`**（即自动登录），而页面紧接着提示"注册成功，**请使用新账户登录**"并 `router.push('/login')`；LoginPage 又**不会**对已登录用户跳转 ⇒ 用户带着登录态看到登录表单，会以为"注册没生效" | 提示改为「注册成功，已自动登录」，并按 `?redirect=`（无则 `/`）**就近跳转**；配套补 `useRoute` |
| **P2-bh 找回密码成功跳转不受管** | 成功后 `setTimeout(() => router.push('/login'), 1500)` **未保存句柄**，`onUnmounted` 只清理了倒计时 ⇒ 用户 1.5s 内主动离开也会被强推登录页 | 新增 `redirectTimer`：受管 + 每次重置句柄 + `onUnmounted` 一并清理 |
| **P2-bi 个人资料保存跳转不受管** | 同类问题：保存成功 1s 后 `router.push('/user')` 的定时器未清理 | 同上（`saveRedirectTimer` + `onUnmounted`） |
| **P2-bj 邮箱下方的"已验证"用的是手机号字段** | 邮箱输入框下显示「已验证」，判断却是 `userStore.user.isPhoneVerified`（**手机号**验证字段）——`User` 类型中根本没有邮箱验证字段 ⇒ 属**用错字段的假状态** | 移除该徽章（无真实字段支撑）；手机号处保留语义正确的验证展示 |
| **P2-bk 简介"最多 500 字符"只是文案** | textarea **无 `maxlength` 也无字数统计**；后端 `updateProfile` 用 `@RequestBody Map` 且无 `@Valid`，实体上的 `@Size(max=500)` 不会触发 ⇒ 超长简介原样入库 | 加 `maxlength="500"` + **实时字数**（接近上限变红） |
| **P2-bl 学习计划可保存"开始晚于结束"** | 开始/结束日期**无任何顺序校验**，后端 `savePlan` 也是直接 `set` ⇒ 可入库颠倒区间，卡片原样渲染「X 起 至 Y」 | `submitForm` 前置校验：`startDate > endDate` 时提示「开始日期不能晚于结束日期」并中止 |

### 二、过程自纠（又两次）

1. `RegisterPage` 用了 `route.query.redirect` 但**未导入 `useRoute`** —— `vue-tsc` 直接报 `TS2304`，eslint 报 `no-undef`；已补。（这也说明"改完必须跑类型检查"不是形式主义。）
2. 本批第一次补导入时，我用 `node -e` 内联脚本被 PowerShell 引号转义搞坏（把 `import` 当命令执行），
   已回到既定做法：**写脚本文件再执行**，避免内联转义事故。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 1m 2s` |
| 后端 `mvn -o -B test` | ✅ **476/476** |
| 对账重算 | ✅ **已修 302（P0 6 / P1 75 / P2 150 / P3 71）· 订正 17 · 不做 5 · 未修 558** |

## v14.34 (2026-10-01) 全端评审落地（第 72 批）：契约不一致 ×2 + 我的文章三处功能缺陷

**依据**：继续 A 口径（P2 中真正影响功能者）。

### 一、契约不一致

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-bb 发布页硬塞后端没有的字段** | 发布负载里 `status: 'published'` 用 `as any` 绕过类型强塞，而后端 `ArticlePublishDTO` **根本没有 `status` 字段**（已核对其字段清单）⇒ Jackson **静默丢弃**，真实状态由服务端审核流程决定（提交后 `pending`）。注释却写着"后端会转为 pending"，容易让人误以为前端能定状态 | 删除该字段并**订正注释**（说明状态由服务端决定、传了也没用）；随之**去掉已无必要的 `as any`**，使负载与 DTO 显式对齐 |
| **P2-bc 银行卡核验只区分两态** | `card.verifyStatus === 'VERIFIED' ? '已核实' : '待核实'` ⇒ **`REJECTED`（四要素核验不一致）也被渲染成"待核实"**，用户会一直等一个不会到来的结果 | 改**三态**：已核实 / **核验未通过**（danger 配色）/ 待核实 |

### 二、我的文章页：三处功能缺陷

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-bd 失败被说成"暂无文章"** | `loadArticles` 只有 loading 与 empty 两态，`catch` 仅 `console.error`（或跳登录），`articles` 保持空数组 ⇒ 模板立刻渲染「暂无文章 / 去发布第一篇文章」 | 新增 `loadError` + 失败态（**排在空态之前**）+ 重试 |
| **P2-be 401 被"双重处理"** | `catch` 中按 message 含"登录/401"再 `router.push('/login?redirect=…')`，与 `client.ts` 的全局 401 处理（`showAuthExpiredDialog`）**重复** ⇒ 全局确认框刚弹出，页面立刻把用户强推到登录页，"留在当前页/继续浏览"的选择被作废 | 不再强推登录页，仅记录可读错误态（`登录状态已失效，请重新登录后查看`），由全局弹窗负责引导 |
| **P2-bf 末页删空后停在空页** | 删除后只 `loadArticles()` 而不纠正 `pageNum`：删掉**末页最后一条**时页码仍指向已不存在的页 ⇒ 后端返回空 records，页面显示「暂无文章」，用户以为文章全没了 | 新增 `reloadAfterRemove()`：当 `pageNum > 1` 且当前页仅剩 1 条时先回退一页再加载；删除流程改用它 |

### 三、过程自纠

`reloadAfterRemove()` 首次只**声明未接线**（删除流程仍调旧函数）——正是我反复强调的"半成品"陷阱；
复查时补上调用点与模板失败态后才收工。

### 四、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 54.63s` |
| 后端 `mvn -o -B test` | ✅ **476/476** |
| 对账重算 | ✅ **已修 296（P0 6 / P1 75 / P2 145 / P3 70）· 订正 17 · 不做 5 · 未修 564** |

## v14.33 (2026-10-01) 全端评审落地（第 71 批）：**专栏文章排序真 bug** + 三处"失败静默/不可重试"

**依据**：继续 A 口径，先清 P2 中真正影响功能者。

### 一、修复 1（★ 真 bug）：专栏管理文章"上移/下移"改错了元素

**代码事实**：

| 位置 | 内容 |
|---|---|
| 模板遍历 | `sortedArticles` —— `computed` 里 `[...articles].sort(by sortOrder)`，即**按 sortOrder 排序的副本** |
| `moveUp/moveDown(index)` | 却直接交换 `column.value.articles`（**源数组，未排序**）中与显示索引相同的两个元素 |
| 随后 `reassignSortOrder()` | 又按**源数组顺序**重排 `sortOrder` |

由于源数组顺序与排序后顺序通常不一致（后台新增文章 append 到末尾、`sortOrder` 未必连续），
上移/下移会**改错元素**，再叠加 sortOrder 重排，表现为"点了上移但顺序不对、别的文章被换位"。

**修复**：新增 `moveItem(from, to)`，**以显示顺序（`sortedArticles`）为基准**交换，
再把新顺序整体写回 `column.value.articles`（元素引用不变、仅顺序变化）并按新顺序重排 `sortOrder` ⇒ **所见即所改**；
`moveUp/moveDown` 退化为一行委托。

### 二、修复 2~4：三处"失败静默 / 无法重试"

| 页面 | 原缺陷 | 修复 |
|---|---|---|
| `QuestionDetailPage` | 详情加载失败只 toast，`question` 保持 null ⇒ 模板落入「未找到题目信息」，与"这道题真的不存在"**无法区分**，且除「返回题库」外**没有重试入口** | 新增 `detailError`（异常分支记录）；模板在该分支显示**真实原因 + 重试按钮**；接口成功但无数据仍按"不存在"处理 |
| `ReadingPage` | 排行榜区块（最近更新/连载中/完结好评）失败只 `console.error` ⇒ 三个区块**空白**且用户无从判断 | 新增 `rankingError`；三区块全空时显示**提示条 + 重试**（单个区块失败不影响主页面） |
| `KnowledgeGraphPage` | 画像用 `getMyProfile().catch(() => null)` **静默降级** ⇒ 图谱退化为全局标签云，用户只看到"没有个性化标记"，不知道是画像没取到 | 新增 `profileDegraded`；在「我的薄弱点」区块**之前**插入降级说明 + 重试（不改动原区块条件） |

### 三、过程自纠

`KnowledgeGraphPage` 的模板编辑首版写成"**替换**个性化区块的 `v-if` 条件"，那会把整块功能弄坏；
复查时发现并改为**在区块之前插入**新元素（原条件保持不变）。再次印证：**插入与替换必须分清**。

### 四、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 54.89s` |
| 后端 `mvn -o -B test` | ✅ **476/476** |
| 对账重算 | ✅ **已修 292（P0 6 / P1 75 / P2 143 / P3 68）· 订正 17 · 不做 5 · 未修 568** |

## v14.32 (2026-10-01) 全端评审落地（第 70 批）：**两类可批量模式**——路由参数校验 ×4 + 分页请求竞态 ×3

**依据**：继续 A 口径，先清"真正影响功能"的两类（同一模式批量处理，效率最高）。

### 一、模式①：路由参数未校验（非数字 id 直接拼进后端 `Long` 路径变量）

后端这些详情接口的路径变量都是 `Long`（如 `/{id:[0-9]+}`）。前端把 `route.params.id` 原样透传，
`/topic/abc`、`/reading/book/1/chapter/abc` 这类地址会**一路拼到后端**，由后端抛**参数类型转换异常**（500/400 而非友好 404）。

| 页面 | 修复 |
|---|---|
| `TopicDetailPage` | `loadTopic` 起始处判定 `/^\d+$/`，非数字直接置「话题不存在或已被删除」，**不发请求** |
| `ChapterReaderPage` | 原只判空 → 扩展为**格式校验**（`chapterId` 与 `bookId` 均须为数字） |
| `ChoicePracticePage` | 调用详情前判定 `route.params.id` 为数字，非数字置「题目不存在或已被删除」 |
| `ContestDetailPage` | `loadDetail` 起始处判定 `contestId` 为数字，非数字置「活动不存在或已结束」 |

### 二、模式②：分页/筛选请求竞态（统一"请求序号守卫"口径）

快速翻页或切换 Tab 时，**先发出的慢响应可能后到并覆盖新结果**（列表与当前页码/Tab 不一致），
且旧请求的 `finally` 会**提前关掉新请求的 loading**。

| 页面 | 修复 |
|---|---|
| `MyTopicsPage` | `loadTopics` 加 `loadSeq`：响应/`catch`/`finally` 三处同步收口 |
| `MyColumnsPage` | `loadColumns` 加 `loadSeq`（Tab 切换 + 翻页都会触发） |
| `MyAttemptsPage` | `loadSubmissions` 加 `loadSeq` |

> 与 v14.15/v14.27 已修的 `ListPage` / `QuestionListPage` / `MyExperiencesPage` / `StudyCalendarPage` 同一口径，
> 至此"分页竞态"这一类在全站主要列表页已收口。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 1m 3s` |
| 后端 `mvn -o -B test` | ✅ **476/476** |
| 对账重算 | ✅ **已修 288（P0 6 / P1 75 / P2 142 / P3 65）· 订正 17 · 不做 5 · 未修 572** |

## v14.31 (2026-10-01) 全端评审落地（第 69 批）：**SQL 安全三处失效修复**（白名单 / UNION 判定 / 结果集脱敏）+ A 靶区收官

**依据**：A 方案（P2 功能性优先）最后一条；安全项，按"最小可信修复 + 守卫测试固化"处理。

### 一、三处安全控制此前形同虚设（逐条查实）

| # | 问题 | 查实结论 |
|---|---|---|
| 1 | **表名白名单从未生效** | `DataQueryServiceImpl` 调的是 `SqlUtils.validateSql(sql)` → `SqlSecurityValidator.validate(sql)`（**1 参**）⇒ `allowedTables` 恒为 null，第 11 步「表名白名单检查」整条分支被跳过 ⇒ AI 生成的 SQL 可访问**库内任意表** |
| 2 | **UNION 检查恒放行** | `isLegitimateUnion` 的 `beforeUnion.endsWith(")") \|\| beforeUnion.matches(".*\\w$")` —— 任何查询在 UNION 前都以单词字符结尾（如 `... WHERE id = 1`）⇒ **恒为 true**，第 7 步从不拦截 |
| 3 | **结果集脱敏被禁用** | 读取数据处写着「数据脱敏 - 已禁用，显示完整数据」，脱敏调用被整段注释 ⇒ 手机号/身份证/邮箱等**原样带出** |

### 二、修复

| # | 修复 |
|---|---|
| 1 | 白名单来源 = **本次提供给 AI 的表结构（`schemas`）** —— AI 只被允许看到这些表，因此也只允许查询这些表；另把 **CTE（`WITH x AS ...`）名**也算作合法"表"，避免误伤；`allowedTables` 为空时保持原语义（不限制） |
| 2 | `isLegitimateUnion` 改为按**注入特征**判定：① UNION 前单引号**未配对** → 拒绝；② UNION 后有**注释符** → 拒绝；③ UNION 后**必须是 SELECT/ALL SELECT/DISTINCT SELECT** → 否则拒绝；④ 左侧必须是顶层 SELECT |
| 3 | 脱敏**重启**，但改用新增的**严格档** `DataMaskingUtils.autoMaskStrict`：只打码**能由值格式自证**的 PII（手机/固话、身份证、邮箱、银行卡），**不再**沿用原 `needsMasking` 里"字段名含 `name` 就按姓名打码"的过宽规则 —— 那会把**公司名/职位名/标题**等业务名称一起打码，使分析结果失去意义（这也正是它当初被整段禁用的现实原因）。姓名/地址是否需要按列脱敏，应交由后台列策略决定，而不是子串规则一刀切 |

### 三、★ 守卫测试固化（安全修复必须自证，且含负例）

新增 **`SqlSecurityValidatorTest`（5 条）**，全绿并进入全量套件：

| 用例 | 断言 |
|---|---|
| 白名单拒绝未授权表 | `sys_user` 被拒且 risk=HIGH、错误信息含表名 |
| JOIN 中的未授权表 | 同样被拒（防止只查 FROM 漏掉 JOIN） |
| UNION 检查有效 | 未配对引号 / 含注释 / UNION 后非 SELECT **三种必须被拦**；**顶层 UNION SELECT 两表都在白名单内必须放行**（不能把正常功能一起拦掉） |
| **特征化**：1 参重载不限制表 | 明确记录"为什么调用方那行代码重要"：1 参 `validate(sql)` 对任意表放行，故调用方**必须**显式传白名单 |
| 严格档脱敏 | 手机/邮箱/身份证**必须打码**；`company_name` 公司全称、`position_name` 职位名、`address` **不得打码** |

### 四、A 靶区（P2 功能性）收官

**A 靶区剩余：0 条**（起始 19 条）。累计变化：

| 项 | 起始 | 现在 |
|---|---:|---:|
| A 靶区（P2 功能性）剩余 | 19 | **0** |

其中真实修复 16 条，另 8 条经查实归位为**已订正 4 / 不做（需求）4**，已在 v14.30 逐条说明。

### 五、校验

| 项 | 结果 |
|---|---|
| 新增守卫测试 | ✅ **5/5** |
| 后端全量 `mvn -o -B test` | ✅ **476/476**（471 + 5 新增），BUILD SUCCESS |
| 对账重算 | ✅ **已修 281（P0 6 / P1 75 / P2 139 / P3 61）· 订正 17 · 不做 5 · 未修 579** |

## v14.30 (2026-10-01) 全端评审落地（第 68 批）：Mapper **重复 namespace** 修复 + 文档计数订正 + A 靶区归位收官

**依据**：P2 功能性优先（A 方案）+ 对账准确性维护。

### 一、修复：同一 namespace 跨两个 XML（潜在启动失败）

**实测发现**（对全站 mapper 做 namespace 扫描）：

| 文件 | namespace | 语句 |
|---|---|---|
| `portal/PortalGrowthLogMapper.xml` | `…PortalGrowthLogMapper` | countTodayAction / countAction / selectRecentLogs |
| `portal/PortalGrowthTimelineMapper.xml` | **同一个** `…PortalGrowthLogMapper` | selectTimelineVOPage |

两个 XML 共用同一 namespace（第二个**文件名还与 namespace 不符**）。因语句 id 不冲突目前能启动，
但**将来一旦 id 重名，MyBatis 会在启动时直接抛 "Mapped Statements collection already contains value"** —— 属埋雷。

**修复**：把 `selectTimelineVOPage` 并入 `PortalGrowthLogMapper.xml`，**删除**命名误导的 `PortalGrowthTimelineMapper.xml`。

| 指标 | 修复前 | 修复后 |
|---|---:|---:|
| XML mapper 文件数 | 106 | **105** |
| 重复 namespace 数 | 1 | **0** |

**验证**：后端 **471/471**（MyBatis 启动即绑定全部语句，是本项最强验证）；合并后时间线 SQL 在真实库**执行通过**（多表子查询语法与列引用有效）。

### 二、文档计数订正（原报告口径不准）

| 说法 | 原文档 | 实测 |
|---|---|---|
| XML mapper | 107 个 | **106 →（合并后）105** 个 |
| 重复 namespace | 0 处 | **1 处 →（已修）0 处** |
| 测试 Java 文件 | 20 个 | **62 个** |
| 源码 Java 文件 | 1424 个 | **main 1447 个**（src 合计 1509） |

### 三、A 靶区（P2 功能性）归位收官

把"看似未修、实为已修/非缺陷/需求"的条目**准确归位**（对账据此移出"未修"）：

| 条目 | 归位 | 依据 |
|---|---|---|
| `HomePage` 示例卡分数写死 | **已订正** | 源码注释与 UI 均已标注「**示例**」，属营销样例 |
| `TopicCreatePage` `businessType='topic_cover'` | **已订正** | 后端该字段是**自由文本**分组标签（无枚举校验） |
| `PracticeCodingListPage` 难度写死 | **已订正** | v13.91 起已字典驱动，原报告基于旧代码 |
| `GrowthTimelinePage` 概览卡静默 | **已订正** | 已于 v14.25 修复 |
| `HomePage` 后台配置入口缺失 | **不做（需求）** | 首页装修 CMS 立项 |
| `UserAgreement` 正文数据源 | **不做（需求）** | 需先补帮助文章详情端点 + 后台站点页面菜单 |
| 后台【面试配置】口径字段 | **不做（需求）** | 后台可配置化立项，需产品定义口径 |
| ledger-app 离线与登录态 | **不做（需求）** | 需独立离线能力设计，改动面大 |

> 结果：**A 靶区（P2 功能性）由 19 条降至 1 条** —— 仅剩后端「自然语言转 SQL」安全校验问题（单独立批谨慎处理）。

### 四、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B test` | ✅ **471/471** |
| 合并后 SQL 实跑 | ✅ 真实库执行通过 |
| 对账重算 | ✅ **已修 280（P0 6 / P1 75 / P2 138 / P3 61）· 订正 17 · 不做 5 · 未修 580**；**A 靶区剩余 1 条** |

## v14.29 (2026-10-01) 全端评审落地（第 67 批）：反馈入口 Tab 参数 + 刷题中心模式名改字典驱动

**依据**：P2 功能性优先（A 方案）。

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-bd 「提交反馈」落到举报表单** | `MyFeedbackPage.goSubmit()` 执行 `router.push('/report')`，而 `ReportFeedback.vue` 的 `activeTab` **默认写死 `'report'`** 且**全文不读任何 query** ⇒ 用户点"提交反馈"看到的是**举报**表单 | ① `goSubmit` 改推 `/report?tab=feedback`；② `ReportFeedback` 引入 `useRoute`，`activeTab` 初值按 `query.tab === 'feedback'` 判定 |
| **P2-be 刷题中心模式卡片名称写死** | `practiceModules` 的名称硬编码（且带写死的 `HOT`/`NEW` 营销徽标），而后端**已有**字典 `portal_practice_mode`（reading/choice/coding） | 模式**名称**改由字典驱动（`useDictData` + 本地兜底，与题库页/后台题库管理同口径）；**移除 HOT/NEW 徽标**（无任何后端字段支撑，属写死营销标，已在注释说明如需角标应由后台下发） |

> 说明：`desc`/`icon`/`color`/`path` 属**前端呈现与路由**，字典中无对应字段，故保留在组件内（不是"数据"而是"呈现"）——已在代码注释中写明判断依据。

**验证**：门户 `vue-tsc` 0 / `eslint` **0 problems** / `npm run build` ✅ · 后端 **471/471**。

## v14.28 (2026-10-01) 全端评审落地（第 66 批）：写死推广卡改**广告位体系** + 面经状态枚举合一（补 archived）+ 会员等级失败重试

**依据**：P2 功能性优先（A 方案）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-az 列表页侧栏写死推广卡** | `ListPage` 侧栏是**整张写死**的推广卡（文案、渐变 `#4f46e5→#7c3aed`、跳转 `/creator/certification`），注释自认"预留后端接口位置"；而项目**已有**广告位体系（`AdCard` + `GET /portal/ad/list?slotKey=` + 后台广告位管理菜单），详情页已在用 | 改为 `<AdCard slot-key="article_list_sidebar" />`；该 slotKey **登记进数据字典**（增量脚本 `20261001-07` + `moyun-db-dml-init.sql` 同步）；清理因之不再使用的 `Megaphone` 图标导入 |
| **P2-ba 搜索页侧栏同类写死** | 同上（`SearchPage`），文案与跳转逐字重复 | 改为 `<AdCard slot-key="search_sidebar" />`；同步登记字典；清理 `ArrowRight`/`Megaphone` 导入 |
| **P2-bb 面经状态枚举硬编码两份且缺项** | `MyExperiencesPage` 的 `statusTabs` 与 `statusMap` **各写一份**且都**缺 `archived`**，而后端 `InterviewExperienceVO`/实体注释明确为 `draft/pending/published/rejected/**archived**` ⇒ 已归档面经会显示成原始英文状态、也无法按该状态筛选 | 抽出 `EXPERIENCE_STATUS` **单一来源**（含 `archived`），tab 与徽章样式均由它派生，消除两处漂移 |
| **P2-bc 会员等级加载失败伪装成"暂无在售等级"** | `getVipTiers()` 失败只 toast ⇒ 页面落到「暂无在售等级」，与"确实没有在售等级"共用同一文案，**且无重试入口**（同仓库排行榜/日历都有重试）；`loading` 初值 `false` 导致首屏还会闪一下空态 | 新增 `tiersError` + 失败态（**排在空态之前**）与重试 `reloadTiers()`；`loading` 初值改 `true` |

### 二、字典登记（四同步）

- 新增增量脚本 **`20261001-07-列表页与搜索页侧栏广告位登记（v14.28）.sql`**：`portal_ad_slot_key` 补 `article_list_sidebar` / `search_sidebar`（`INSERT ... SELECT ... WHERE NOT EXISTS` 幂等）；
- **实跑 + 幂等验证**：连续执行两次均返回 6 个广告位（4 原有 + 2 新增）；
- 同步 `init-sql/moyun-db-dml-init.sql`。

> 说明：`portal_ad` 表当前为空，故这两个侧栏在**未投放广告时不再渲染任何内容** —— 这是广告位体系的预期行为（展示内容由后台投放决定），已在代码注释中写明。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 54.50s` |
| 后端 `mvn -o -B test`（含新增量脚本与 DML 变更过守卫） | ✅ **471/471** |
| **增量 SQL 幂等实测** | ✅ 两次执行均 6 个广告位 |
| 对账重算 | ✅ **已修 277（P0 6 / P1 75 / P2 135 / P3 61）· 订正 10 · 不做 2 · 未修 593** |
| **A 方案靶区进度** | P2 功能性剩余 16 → **12 条** |

## v14.27 (2026-10-01) 全端评审落地（第 65 批）：他人成就身份展示（含**前端类型缺失**）+ 日历请求竞态 + 收银台关单时间取服务端

**依据**：P2 功能性优先（A 方案）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-av 查看他人成就没有身份信息** | 他人视角只把标题改成写死的「TA的成就」——**不显示对方昵称/头像**，也**没有返回其主页的入口**；且本页 `getSafeAvatar` 未导入 | 展示 `growth.nickname` 与 `getSafeAvatar(growth.avatar, targetUserId)`，标题变为「XXX 的成就」，并加「← 返回 TA 的主页」；补 `getSafeAvatar` 导入与 `useRouter` |
| **P2-aw 前端类型缺字段（连带缺陷，类型检查抓出）** | 后端 `UserGrowthVO` **已有** `nickname`/`avatar`，但**前端 `types/api.ts` 的 `UserGrowthVO` 没有这两个字段** ⇒ 直接写 `growth.nickname` 报 `TS2339` | 给前端类型补 `nickname?`/`avatar?`（**契约补齐**，与后端对齐） |
| **P2-ax 日历请求竞态** | `loadCalendar` 无请求序号/AbortController，快速切年份或连点「刷新」会并发多请求 ⇒ **先发的慢响应后到会覆盖**（出现"选 2026 年却显示 2024 年数据"）；刷新与重试按钮在 loading 期间**未禁用** | 加 `loadSeq` 序号守卫（过期响应丢弃、`catch`/`finally` 同步收口）；刷新按钮 `:disabled="loading"`（含禁用态样式） |
| **P2-ay 收银台关单时间与后端不一致** | 关单分钟数只用 URL 里写死的 `expireMinutes`（默认 30）显示，而状态响应**本就带 `expireTime`** 却没用 | 每次轮询按服务端 `expireTime` 计算剩余分钟（`Math.ceil`，过期显示 0） |

### 二、★ 过程要点

1. **类型检查再次抓到真实契约缺口**：本次不是"少写导入"，而是"**后端有、前端类型没有**"（`nickname`/`avatar`）——
   这正是把"补类型"与"改用法"放在同一批的好处：`vue-tsc` 直接指出字段不存在，避免我按报告描述臆断。
2. 原报告称"页面 import 了 `getSafeAvatar` 却未使用"，实测是**根本没导入**；已按实际代码处理（导入并使用）。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 54.22s` |
| 后端 `mvn -o -B test`（本批无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 270（P0 6 / P1 75 / P2 130 / P3 59）· 订正 10 · 不做 2 · 未修 600** |
| **A 方案靶区进度** | P2 功能性剩余 19 → **16 条** |

## v14.26 (2026-10-01) 全端评审落地（第 64 批）：**撤下占位假内容与编造数据** + 备案号收敛单一来源 + 面包屑被导航遮挡

**依据**：P2 功能性优先（A 方案）。本批集中处理 `AboutUs` / `UserAgreement` / `ReportFeedback` / 页脚登录注册的**内容可信度与重复实现**。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-ao 公开页展示虚构人物** | 「关于我们」的团队三宫格写死**占位假成员**「张三/李四/王五」，头像还是 `images.unsplash.com` **外链热链**（境内常不可达，且 `img` 无 `loading`/宽高） | 整块撤下（含数据），并写明：真实团队信息待后台 CMS 维护后展示 —— **不展示虚构人物** |
| **P2-ap 编造运营数据当事实** | 发展历程写死「2023年1月正式上线」「**用户数突破10万**」「**文章数超过100万篇**」——**未接任何数据源**的运营结论 | 改为**不虚构的阶段性能力描述**（题库与面经 → AI 能力 → 成长体系），去掉无据的日期与数字 |
| **P2-aq 面包屑被全局导航遮挡** | 两页面包屑条用 `sticky top-0 z-30`，而全局导航是 `sticky top-0 z-50` ⇒ **滚动后滑到导航下方被覆盖** | 去掉 `sticky`（面包屑无需常驻），`AboutUs` 与 `UserAgreement` 同步 |
| **P2-ar 协议页联系方式与页脚不一致 + 占位号码** | 协议末尾写死「邮箱 support@xulin.com / 客服热线 **400-888-8888**」，与页脚公布的 `contact@xulin.com` 等**不一致**，且 400 号是**明显占位号** | 去掉占位热线，邮箱改用与页脚**同源常量** `CONTACT_EMAIL` |
| **P2-as 举报页同一假号 + 服务承诺时限** | 「温馨提示」写死「**3 个工作日内**处理」这一**对外承诺时限**与**同一占位热线 400-888-8888** | 时限改为不计期限表述（"尽快核实处理，结果可在我的举报查看"），联系方式改用同源邮箱常量 |
| **P2-at ICP 备案号三处重复写死** | `京ICP备xxxxxxxx号-2` **逐字写死在 3 个文件**（`SiteFooter` / `LoginPage` / `RegisterPage`），上线替换真实备案号时**极易漏改** | 新建 `src/constants/site.ts` 作为**单一来源**（`ICP_LICENSE`/`SITE_NAME`/`SITE_SLOGAN`/`CONTACT_EMAIL`），三处改为引用；并在常量注释里标注"**上线前必须替换**" |
| **P2-au 协议无版本号** | 「最后更新时间：2024年1月1日」写死在模板里，协议**无版本号** | 抽出 `AGREEMENT_VERSION` / `AGREEMENT_UPDATED_AT` 文件内常量，模板显示「版本 v1.0 · 最后更新时间 …」 |

### 二、★ 全站占位内容复核（避免只修被点名的页面）

修完上述后我做了一次**全站扫描**（`张三|李四|王五|400-888-8888|京ICP备xxxxxxxx|用户数突破10万|文章数超过100万`），
正因如此才发现 `ReportFeedback.vue:466` 还有**同一占位客服热线**（原清单第 10 条点到、但不在本批计划内）并一并修掉。

复核后仅剩**合规命中**：
- `site.ts`：常量定义本身 + 说明注释（**故意保留占位值并标注须替换**）；
- `ResumeEditPage` 两处 `placeholder="如：张三 - Java 工程师简历"`：**输入框示例文本**，属正常用法；
- `AboutUs` / `ReportFeedback` / `UserPage`：**说明性注释**（记录撤下了什么、脱敏规则）。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 54.13s` |
| 后端 `mvn -o -B test`（本批无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 267（P0 6 / P1 75 / P2 127 / P3 59）· 订正 10 · 不做 2 · 未修 603** |
| **A 方案靶区进度** | P2 未修 205 条中，**功能性剩余 19 条**（上批 27 → 本批 19） |

## v14.25 (2026-10-01) 全端评审落地（第 63 批）：成长时间线三态与触底重发 + 面经正文暗色主题不可读

**依据**：P2 功能性优先（A 方案）。本批含**一页 3 条**（`GrowthTimelinePage`）+ 1 条暗色主题可读性。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-ak 成长时间线把"加载失败"说成"没有记录"** | `load()` 的 `catch {}` 为空 **且 `code!==200` 没有 else 分支** ⇒ 失败与"确实没有记录"都落到 `Empty`「还没有成长记录，去阅读题目…」 | 新增 `loadError`（业务失败与异常都记录），失败态**排在空态之前** + 重试 |
| **P2-al 触底失败会反复重发同一请求** | 失败时 `noMore` 保持 false、`page` 也不递增（自增只在成功分支），而 `handleScroll` 的守卫只判断 `loading/loadingMore/noMore` ⇒ **停在底部会持续重发同一个失败请求** | `handleScroll` 增加 `loadError.value` 守卫：**失败即暂停触底加载**，需用户显式重试 |
| **P2-am 成长概览失败整块静默消失** | `getMyGrowth()` 的 `catch {}` 静默，失败时 `growthInfo` 保持 null ⇒ 概览卡片 `v-if` 直接消失，用户分不清"没数据"还是"加载失败" | 新增 `growthFailed` + 概览区**失败提示条与重试**（抽出 `reloadGrowth()`） |
| **P2-an 面经正文在暗色主题下几乎不可读** | `.article-content :deep(...)` 用**浅色主题硬编码**：`h1/h2/h3 { color:#1f2937 }`、`code { background:#f3f4f6 }`、`blockquote { color:#6b7280; background:#f9fafb }` —— 暗色主题下标题与引用几乎不可见 | 全部改用站点主题变量（`var(--theme-text)` / `var(--theme-text-secondary)` / `var(--theme-accent)` / `var(--theme-bg)` / `var(--theme-primary)`），随主题自适应（`pre` 深色代码块**保留**，两种主题下都合适） |

### 二、核减（诚实记录，本批未做）

原计划的 `HomePage` 两条经核查后**核减**：

1. **示例卡分数写死**：该卡在源码注释与 UI 上**均已明确标注「示例」**（`多维度智能分析 · 示例`）⇒ 属营销样例，**不是把假数据当真实用户数据展示**，无需修改；
2. **"后台配置入口缺失"**：Hero 文案/宫格/阈值硬编码 + 后台无「首页装修」菜单 —— 这是**功能需求（CMS 立项）**而非缺陷，应作为产品需求排期，不计入本清单的"修复"。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 55.15s` |
| 后端 `mvn -o -B test`（本批无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 255（P0 6 / P1 75 / P2 119 / P3 55）· 订正 10 · 不做 2 · 未修 615** |
| **A 方案靶区进度** | P2 未修 213 条中，**功能性剩余仅 27 条**（较上批 30 条继续下降） |

## v14.24 (2026-10-01) 全端评审落地（第 62 批）：会员状态"没查到"说成"没开通" + 收银台轮询吞异常 + 反馈空态 + 难度参数未归一

**依据**：P2 功能性优先（延续 A 方案）。

### 一、修复清单（5 条）

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-af 会员页把"加载失败"说成"尚未开通会员"** | `getVipStatus().catch(() => null)` 静默兜底，而 `vip` 初值是 `{isVip:false}` ⇒ 失败后状态卡渲染「尚未开通会员 / 部分功能可免费体验」，**权益用量区（`v-if="vip.benefits?.length"`）整块消失** | 新增 `statusError`：失败时显示「⚠️ 会员状态加载失败 / 暂时无法获取你的会员状态」+ **重试**（抽出 `reloadStatus()`）；与"确实未开通"明确区分 |
| **P2-ag 收银台轮询吞掉致命错误** | `catch {}` 吞掉全部异常 ⇒ 后端 403「订单不存在或无权操作」、401 登录过期、网络中断**都静默重试到 100 次（约 5 分钟）**，用户既不知原因也无提示 | 区分处理：**鉴权/权限/订单不存在类错误立即停止轮询并提示**；其余保留重试但**连续 3 次失败即停止**并提示"请稍后刷新确认"；成功一次复位计数 |
| **P2-ah 反馈列表失败被说成"暂无反馈"** | `loadList` 失败只在 catch 弹 toast、**无 error 状态** ⇒ 列表为空落到 `Empty`「暂无反馈记录」 | 新增 `loadError` + 失败态（**排在空态之前**）+ 重试 |
| **P2-ai 反馈空态不区分筛选** | 空态固定「暂无反馈记录 / 您还没有提交过反馈」+「去提交反馈」，不看 `statusFilter/typeFilter` 是否生效 | 新增 `isFiltering`：筛选无结果时显示「没有符合筛选条件的反馈 / 试试放宽筛选条件」+ **重置筛选**按钮 |
| **P2-aj 编程练习列表参数未兜底** | ① `activeDifficulty = route.query.difficulty \|\| ''` **未归一** ⇒ `?difficulty=Hard` 与后端小写枚举精确匹配失败，**命中 0 条且 4 个难度按钮全不高亮**；② `parseInt(page) \|\| 1` 不兜负数（`?page=-5` 渲染「-5 / n」并把 `pageNum=-5` 发后端） | ① 声明期**小写归一** + 字典就绪后 `watch` 校验非法值归零（回到"全部"）；② `Math.max(1, …)` |

> 说明：本批原计划的「`TopicCreatePage` 写死 `businessType='topic_cover'`」经核查 —— 后端 `SysFileController.upload` 的
> `businessType` 是**自由文本**（仅作分组标签，无枚举校验），因此不是"假字段"，属"命名未登记"的低价值项，**本批不做**，留待与文件管理规范化一并处理。

### 二、过程自纠

生产脚本前先核对各页 `vue` 导入（`computed`/`ref` 均已存在），避免又一次"缺导入"；本轮 11 处编辑一次命中。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 54.85s` |
| 后端 `mvn -o -B test`（本批无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 252（P0 6 / P1 75 / P2 116 / P3 55）· 订正 10 · 不做 2 · 未修 618** |

## v14.23 (2026-10-01) 全端评审落地（第 61 批）：**可用余额口径纠正**（资金）+ 三处"失败伪装成空态" + 金句点赞竞态

**依据**：P2 功能性优先（资金口径 1 条 + 失败态 3 条 + 竞态 1 条）。

### 一、修复 1（★ 资金口径）：把"总余额"当"可用余额"

**事实核查**：后端提现校验用的是 `可用余额 = balance - frozen_amount`（`WithdrawOrderServiceImpl` L44/L107-110，
审核中提现单占用的金额被冻结），而账户总览 `userSummary` **只回 `balance`**；
前端把它标成「可用余额」并作为提现上限校验 ⇒ **用户看到/能填的金额大于真正可提现金额**（填完才被后端拒绝）。

| 层 | 改动 |
|---|---|
| 后端 | `userSummary` 增发 `availableBalance`（= balance − frozen，与 `apply()` 同口径）与 `frozenAmount` |
| 类型 | `PayAccountOverview` 增 `availableBalance?` / `frozenAmount?` |
| 前端 | 概览卡与提现表单改显示**可用余额**，并另行说明「其中审核中占用 ¥X（余额合计 ¥Y）」；提现上限校验改用 `availableBalance`（提示里带上具体可用金额） |

### 二、修复 2~4：三处"失败被渲染成空态"

| 页面 | 原缺陷 | 修复 |
|---|---|---|
| `CompanyPage` | `loadTabData` 的 catch 把错误写进 `error`，但模板渲染条件是 `error && !company`，而该函数在 `!company` 时已 `return` ⇒ **Tab 加载失败永远不显示**，列表清空后直接落空态 | 新增 `tabError`（每次加载清空），在 **Tab 区**渲染失败态 + 重试 |
| `GrowthRankingPage` | `loadRanking` 的 catch 只 `console.error` ⇒ 失败与"真的无人上榜"共用同一段「暂无排行数据」空态，**无重试入口** | 新增 `loadError`（含 `code!==200` 分支），失败态**排在空态之前** + 重试 |
| `QuoteListPage` | `load()` 的 `catch {}` 为空 **且 `code!==200` 没有 else 分支** ⇒ 网络/服务端错误一律渲染成「暂无金句摘录」 | 新增 `loadError`（业务失败与异常都记录），失败态**排在 `Empty` 之前** + 重试 |

### 三、修复 5：金句点赞的连点与"业务失败不回滚"

- **在途去重**：原实现请求期间不禁用按钮、无在途标记 ⇒ 连点两次发两次 toggle（一次点赞一次取消），响应乱序时 UI 与后端状态相反；
  现按 `quote.id` 在 `likePending` 中标记，在途直接 return。
- **业务失败处理**：原 `if (resp.code === 200 && resp.data) {...}` **没有 else** ⇒ `code!==200`/空 data 时既不回滚也不报错；
  现统一 `rollback()` 并按文案提示（异常分支同样回滚+提示）。

### 四、过程自纠（本轮再次命中同一坑）

- `CompanyPage` 首次编辑的锚点里混入了 PowerShell dump 的 `]` 符号 ⇒ 未命中；修正后重跑。
- `QuoteListPage` 需要 `toast` 但**页面并未引入** `useToast` ⇒ 补 import 与实例化（否则 `vue-tsc` 会直接报 TS2304）。
- `CompanyPage` 的 `tabError` 一度"只声明不渲染"（半成品）⇒ 补模板块后才收工。
> 结论：**脚本 + 模板 + 依赖导入，三者缺一都不算修完**；本轮靠"编辑清单逐条对照"与类型检查兜住了两处。

### 五、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` / `test` | ✅ exit 0 / **471/471** |
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / ✅ build 通过 |
| 对账重算 | ✅ **已修 246（P0 6 / P1 75 / P2 111 / P3 54）· 订正 10 · 不做 2 · 未修 624** |

## v14.22 (2026-10-01) 全端评审落地（第 60 批）：提现额度改为配置驱动（前后端同源）+ 学习计划单位不再写死"题"

**依据**：P2「硬编码阈值未落配置 / 前端不知上限」「单位写死与计划类型不符」。

### 一、修复 1：提现额度从**硬编码常量**改为配置驱动并下发前端

原状：`WithdrawOrderServiceImpl.apply` 里写死 `new BigDecimal("1")` 与 `new BigDecimal("50000")`，
前端**完全不知道上限**（`WalletPage` 只校验 `amount>0` 与 `<= balance`）⇒ 用户填超了才被后端拒绝，运营改额度必须改代码。

| 层 | 改动 |
|---|---|
| 配置 | `PayProperties.Payout` 新增 `withdrawMin`（默认 1）、`withdrawMax`（默认 50000），可经 `moyun.pay.payout.*` 覆盖 |
| 服务 | `apply()` 改为读配置并据此报错（新增 `toPlain()` 去掉尾零，提示"不可低于 1 元"而不是"1.00 元"） |
| 下发 | `userSummary()`（`GET /portal/pay/account/overview`）增加 `withdrawMin` / `withdrawMax` |
| 前端 | `PayAccountOverview` 类型补两字段；`WalletPage` 前置校验上下限 + 表单提示「单笔 X ~ Y 元（不超过可用余额）」 |

> 额度**前后端同源**：同一份 `PayProperties` 既做后端校验也下发前端提示，不会再出现"两边各写一个数"的漂移。

### 二、修复 2：学习计划目标量单位写死"题"

`StudyPlanVO` 的 `planType` 含 `daily_question` / `weekly_reading` / `custom`，但列表三处模板都写死「题」
⇒ 阅读类计划显示"目标 10 题"。改为 `planUnit(planType)` 映射：`daily_question→题`、`weekly_reading→篇`、其它→**项**（中性词，不臆测）。

### 三、过程自纠

`WalletPage` 的模板提示首轮**未命中锚点**（脚本只改了脚本部分），导致 `withdrawRangeText` 定义后**没有任何使用处**
（正是我一直在防的"半成品"）。复查发现后直接补上模板引用与提示行。
> 教训再次生效：**脚本 + 模板是一体的，任一侧未落地都算未修**。

### 四、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` / `test` | ✅ exit 0 / **471/471** |
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / ✅ build 通过 |
| 对账重算 | ✅ **已修 240（P0 6 / P1 75 / P2 105 / P3 54）· 订正 10 · 不做 2 · 未修 630** |

## v14.21 (2026-10-01) 全端评审落地（第 59 批）：限免封面"永远 loading"（后端补 JOIN）+ ASR 错误码可读化 + 分享页无重试 + 轮询无限

**依据**：P2「接口缺字段导致前端只能传空值 / 内部错误码直出 / 无重试 / 无限轮询」。

### 一、修复清单（4 条，含一处后端补字段）

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-ab 限免专区封面永远是空** | 前端 `<LazyImage :src="''" />` **恒传空串** —— `LazyImage` 的 loading 初值为 `true`，空 src 下 `img` 不触发 load ⇒ 卡片**永远停在 loading 遮罩**。根因是后端推荐查询只 JOIN 了书名，没取封面 | ① 后端：`selectBookRecommendVo` 增加 `b.cover as book_cover` + resultMap 映射 + 实体 `bookCover` 字段（与既有 `bookTitle` 同为 **JOIN 字段，不参与 insert/update**）；② 前端类型补 `bookCover?`；③ 模板改用**真实封面**，确实无封面时给占位块（而不是空图） |
| **P2-ac ASR 内部错误码直出** | 页面把 composable 的 `errorMessage` **原文渲染**（`not-allowed`、`server-asr-failed`，以及浏览器的 `network`/`no-speech`/`audio-capture`/`aborted`）⇒ 用户看到英文码，得不到任何可操作信息 | 新增 `asrErrorText` 映射表（8 个已知码 → 中文可操作文案）；**已是中文的文案原样展示**；未知码给通用文案并把**原始码放进 `title`** 便于排查（不臆测含义） |
| **P2-ad 分享报告页无重试、把"加载失败"说成"链接失效"** | `onMounted` 的 `catch { report.value = null }` **不区分错误类型**，模板只显示「分享链接不存在或已过期」，**且没有任何重试入口** | 抽出可重试的 `loadReport()`；区分「接口成功但无数据」（确为失效）与「异常」（展示真实原因）；仅后者给**重试按钮**（正则识别 `不存在/已过期/无效/未找到` 判为失效） |
| **P2-ae 报告轮询无上限、失败静默** | `setInterval` 每 5s 执行，**无最大轮数/超时/失败退出**；单条查询异常被空 `catch` 吞掉只让轮询继续 ⇒ 用户永远看不到「生成失败」 | 加上限 **60 轮（≈5 分钟）** 与**连续失败退出（5 轮）**：超时提示"请稍后刷新查看"，连续失败提示"已停止自动刷新"；单轮失败不再清零计数，成功则复位 |

### 二、验证

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` / `test` | ✅ exit 0 / **471/471** |
| **推荐 JOIN 实测（事务造数 + 回滚）** | ✅ 插入书（含 cover）+ 限免推荐行后，与 Mapper **完全一致**的 JOIN 投影返回 `book_title=测试书`、`book_cover=https://cdn.example.com/cover.jpg`；回滚后两表均 0 行 |
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 49.66s` |
| 对账重算 | ✅ **已修 237（P0 6 / P1 75 / P2 103 / P3 53）· 订正 10 · 不做 2 · 未修 633** |

> 本批又出现一次"前端只能传空值"的根因在**后端缺字段**——与 v14.13（简历模板分类）、v14.19（TS 模板）同属"契约两侧不一致"，
> 处理口径统一为：**先查清数据源，能在后端补字段就补（而不是在前端糊一个占位）**。

## v14.20 (2026-10-01) 全端评审落地（第 58 批）：发布面经重复提交窗口 + 「操作成功」当错误 + 三处"失败伪装成空态" + 页码兜底

**依据**：P2「竞态 / 错误文案 / 失败与空态混同 / 参数越界」。

### 一、修复清单（5 条）

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-v 发布面经重复提交窗口** | `submitting` 在 **`await promptRealNameOptional()` 之后**才置 true ⇒ 实名弹窗及其网络往返期间按钮仍可点；编辑走 PUT、后端未加防重 ⇒ **可重复提交** | 函数入口先 `if (submitting.value) return`，并**把上锁提前到实名提示之前**；实名提示移入 `try`，保证任何提前 `return` 都由 `finally` 解锁 |
| **P2-w 详情不存在却把「操作成功」当错误** | 详情不存在时后端返回 `code=200 + data=null`，而 `message` 是成功文案「操作成功」；页面 `pageError = res.message` ⇒ 页面显示**自相矛盾**的错误"操作成功" | 区分三种情况：`code===200 && data` 正常；`code===200 && !data` → **「面经不存在或已被删除」**；否则用 `message` |
| **P2-x 举报列表失败被说成"暂无记录"** | `loadList` 失败只在 catch 弹 toast、**无 error 状态** ⇒ 列表为空落到 `Empty`「暂无举报记录 / 您还没有提交过举报」 | 新增 `loadError` + 失败态（**排在空态之前**，含重试） |
| **P2-y 帮助中心搜索/分类失败被说成"没有找到"** | 两处 catch 只 `console.error`、无错误态也不清旧数据 ⇒ 落到「没有找到相关问题 / 该分类下暂无问题」 | 新增独立 `helpError`（**不复用页面级 `error`**，避免"搜索失败"把整页替换成错误页）+ 失败态与重试；切换分类/输入搜索时清空该状态 |
| **P2-z 成就页失败与"筛选无结果"混同** | `loadData` 的 catch 只 `console.error` ⇒ 加载失败与"某模块筛选后 0 条"落到同一块「暂无成就数据」 | 新增 `loadError` + 失败态（排在筛选空态之前）+ 重试 |
| **P2-aa 练习列表页码越界** | `parseInt(route.query.page) \|\| 1` 只兜住 `NaN/0`，**负数原样保留** ⇒ `?page=-5` 把 `pageNum=-5` 发给后端 | 统一夹到 `Math.max(1, …)` |

### 二、★ 本轮自纠（三次锚点问题，都被工具兜住）

1. `MyReportsPage` / `AchievementsPage` 的**模板锚点缩进写错**导致脚本改动生效、模板未生效 ⇒ `loadError` 变成"只写不显示"的**半成品**；
   两处补上正确缩进的模板块后，`vue-tsc`/`eslint`/构建全绿才收工。
2. 教训固化：**同一次编辑里"脚本 + 模板"必须都落到位**，否则就是新的"假修复"——本例正是靠"脚本成功、模板未命中"的对比输出发现的。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems** |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 50.34s`） |
| 后端 `mvn -o -B test`（本批无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 232（P0 6 / P1 75 / P2 99 / P3 52）· 订正 10 · 不做 2 · 未修 638** |

## v14.19 (2026-10-01) 全端评审落地（第 57 批）：**TS 模板在判题机上根本编译不过**（实测复现）+ 阅读首页收藏状态 N+1 消除

**依据**：P2「模板与判题环境不匹配」「N 次请求 + 静默吞错」。

### 一、修复 1：TypeScript 模板在判题机上编译失败（★ 实测复现 + 实测修复）

**判题机真实编译参数**（`LanguageRuntime.typescript()`，镜像 `node:20-alpine`）：
```
tsc --target ES2020 --module CommonJS --moduleResolution node --skipLibCheck <SRC> --outDir <OUTDIR>
```
镜像内**没有 `@types/node`**，而 `--skipLibCheck` 只跳过**库**检查、并不声明 `require`/`process`。

**修复前实测**（用门户自带 tsc + 判题机同参数，忠实抽取模板求值后编译）：
```
Solution.ts(2,15): error TS2580: Cannot find name 'require'. Do you need to install type definitions for node? ...
Solution.ts(11,5): error TS2580: Cannot find name 'process'. ...
tsc exit: 2
```
⇒ 用户选中 TypeScript、一个字没改点"运行"，**直接吃 COMPILE_ERROR**。

**修复**：在 TS 模板顶部加**最小环境声明**（不引入依赖、不改变判题参数）：
```ts
declare const require: (id: string) => { readFileSync: (fd: number, enc: string) => string };
declare const process: { exit: (code?: number) => void };
```

**修复后实测**：

| 步骤 | 结果 |
|---|---|
| `tsc`（判题机同参数） | ✅ **exit 0** |
| `node dist/Solution.js`（判题机 runCommand）+ 样例输入 `[2,7,11,15]` / `9` | ✅ 输出 `[0,1]` |
| JavaScript 模板对照运行（回归） | ✅ 输出 `[0,1]` |

> 过程自纠：第一版验证脚本直接抓**源码文本**（含转义 `\\n`）当模板内容，导致运行期报 `JSON.parse` 错误 ——
> 实为**验证脚本**的错（未对模板字面量求值），不是模板的问题。改为 `new Function('return \`...\`')` 求值后即与判题机一致。

### 二、修复 2：阅读首页收藏状态 N+1 + 静默吞错

原实现：`bookLists.forEach(bl => checkBookListBookmark(bl.id).then(...).catch(() => {}))`
⇒ **N 个书单 N 次请求 + N 次查库**，且失败**完全静默**（书店的收藏态会整体错显为"未收藏"而无从发现）。

**修复（新增批量接口，四层对齐）**：

| 层 | 内容 |
|---|---|
| Mapper XML | `selectBookmarkedIds`：`where user_id = #{userId} and booklist_id in <foreach>`（**仅 `#{}`，双轨合规**） |
| Mapper 接口 | `List<Long> selectBookmarkedIds(@Param userId, @Param Collection<Long> booklistIds)` |
| 控制器 | 新增 `GET /portal/reading/book-lists/bookmarks?ids=1,2,3` → `{bookmarkedIds:[...]}`；游客/空集合直接返回空；**去重 + 过滤 null** |
| 前端 | `getBookListBookmarkIds(ids)`；ReadingPage 由 forEach 改为**一次**请求，失败打印可诊断 `console.warn`（不静默） |

**批量 SQL 实测（事务内造数 + 回滚）**：用户 777 查 `[11,22,33]` → 只返回 `11,22`（**33 属他人，正确排除**）；查 `[44,55]` → **空集**；回滚后 0 行残留。

### 三、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` / `test` | ✅ exit 0 / **471/471** |
| **TS 模板判题机仿真** | ✅ 编译 exit 0、运行输出 `[0,1]`（JS 模板同步回归通过） |
| **批量 SQL 实测** | ✅ 他人收藏被排除、空集正确、回滚零残留 |
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 53.06s` |
| 对账重算 | ✅ **已修 226（P0 6 / P1 75 / P2 93 / P3 52）· 订正 10 · 不做 2 · 未修 644** |

## v14.18 (2026-10-01) 全端评审落地（第 56 批）：首页丢弃真实头像 + Hero「0+ 道」假数据条

**依据**：P2「接口已返回的数据被丢弃 / 无数据却渲染 0」。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-t 名家录与动态广场丢弃真实头像** | ① `loadAuthors` 把接口返回的 `avatar` **替换成昵称首字母**（`(user.nickname \|\| user.username \|\| 'A').charAt(0)`），模板也只渲染这个字母 ⇒ 有头像的作者全显示成字母；② 动态广场同样只渲染 `(feed.userNickname \|\| 'A').charAt(0)`，**完全忽略**接口已返回的 `feed.userAvatar` | ① 名家录保留真实头像（空值用既有 `getSafeAvatar(user.avatar, id)` 兜底），并额外带 `initial` 字段；模板用新增的 `isImageUrl()` 判定，是图片地址就渲染 `<img loading="lazy">`（`object-cover` + `overflow-hidden`），否则回退首字母；② 动态广场同样改为优先 `<img :src="feed.userAvatar">`，无头像才用首字母 |
| **P2-u Hero「0+ 道精选面试题」** | `heroStats` 把「面试题库」**无条件**放进数组 ⇒ 接口失败/冷启动（`interviewTotalQuestions = 0`）时渲染出「**0+ 道**」；Hero 副标题也**无条件**显示「0+ 道精选面试题持续更新」 | 与其它数据条统一口径：`interviewTotalQuestions > 0` 才 push；副标题加 `v-if="interviewTotalQuestions > 0"` |

### 二、★ 过程中的两次自纠（都被工具抓住）

1. **锚点里的反引号**：目标代码含 `` `${interviewTotalQuestions.value}+` ``，直接把反引号写进 JS 模板字符串会**提前结束字符串**；
   改用 `'\x60'` 拼接构造锚点后匹配成功。
2. **PowerShell `Replace` 波及 `new` 字段**：我用 `$raw.Replace($bad,$good)` 修正锚点时，`$bad` 文本在脚本里**同时出现在 `old` 与 `new` 两处**，于是把 `new` 里的 `= []` 也改成了 `= [`，
   产出 `const stats: ... = [` + `if (...) {` ⇒ `vue-tsc` 报 `TS1137: Expression or comma expected`、`eslint` 报 Parsing error。
   > 教训：**脚本自改要用"唯一匹配"的方式**（或只改指定行），不要对整段文本做无差别 `Replace`；同时**类型检查/解析报错就是这类手误的安全网**。
3. 顺带修正：插入 `isImageUrl` 时占用了 `countText` 的 JSDoc 注释位置（注释悬空），已把注释归位。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems** |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 53.03s`） |
| 后端 `mvn -o -B test`（本批无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 224（P0 6 / P1 75 / P2 91 / P3 52）· 订正 10 · 不做 2 · 未修 646** |

## v14.17 (2026-10-01) 全端评审落地（第 55 批）：**402 会员引导被通用文案吃掉**（根因收敛到一处）+ 书单收藏静默 + 个人页无限加载

**依据**：P2「关键后端文案被前端通用文案覆盖 / 静默失败 / 无失败态」。

### 一、修复 1（★ 一处根治一类问题）：业务错误优先展示**服务端文案**

**缺陷**：`useApiCall.run` 里 `const message = errorToast || getErrorMessage(err);` —— 页面传的固定 `errorToast`
**永远优先**。于是 `VoiceInterviewPage` 的 `{ errorToast: '开始失败' }` 会把后端 `@VipOnly` 的
**402 会员引导**（"语音面试次数已用完，请开通会员"）覆盖成一句无信息量的"开始失败"
⇒ 用户既不知道失败原因，也看不到开通入口。

**修复（收敛在 helper，而非逐个页面改文案）**：

| 层 | 改动 |
|---|---|
| `api/client.ts`（fetch + upload 两条路径） | 业务失败（`!response.ok \|\| code !== 200`）抛出的 Error **附上 `code`**，使上层能区分"业务失败"与"网络/未知失败" |
| `composables/useApiCall.ts` | 文案优先级改为：**① 业务错误 → 服务端文案原样展示**；② 页面 `errorToast`（网络/未知失败时的场景化提示）；③ 通用兜底 |

> 这样**所有**页面的权益/校验类后端提示（402/400 等）都能到达用户，而不是只有改过的那个页面。

### 二、修复 2：书单收藏"点了没反应"

| 子问题 | 修复 |
|---|---|
| `handleToggleBookmark` 业务失败（`code!==200`）**静默**、异常也只 `console.error` ⇒ 用户以为已收藏成功，实际未写入 | 失败给出可见提示（业务失败用服务端 message，异常用异常 message） |
| 初始态查询 `checkBookListBookmark` / `checkBookListLike` 用 `.catch(() => {})` **完全静默**，且**未做失效校验**（晚到响应会覆盖"已切到另一个书单"后的状态） | 失败改为可诊断的 `console.warn`（初始态查询失败不打断浏览，故不弹 toast）；并以**书单 id 校验**丢弃过期响应 |

### 三、修复 3：个人页永久停在"加载中..."

模板只有 `v-if="!currentUser"` 一种状态；未登录时 `loadUserData` 直接 `return`，
而 `fetchCurrentUser` 失败会把 store user 置 null 且**不抛错** ⇒ `currentUser` 永远为空，
页面**永久转圈**，既无错误提示也无重试。

**修复**：新增 `loadError` 状态 + 模板失败态（图标 + 文案 + **重试按钮**），与"加载中"分支用 `v-if/v-else-if/v-else` 明确区分。

### 四、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems** |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 53.22s`） |
| 后端 `mvn -o -B test`（本批无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 222（P0 6 / P1 75 / P2 89 / P3 52）· 订正 10 · 不做 2 · 未修 648** |

> 本批把"失败不可见"这条线从**逐页修**推进到**根因收敛**：`useApiCall` 一处修正后，
> 全站所有带业务 code 的失败都会展示后端真实原因，不再依赖每个页面是否记得传对文案。

## v14.16 (2026-10-01) 全端评审落地（第 54 批）：动态流"加载更多"失败不可见 + 专栏文章关联假成功 + 在线运行失败无处可查

**依据**：P2「失败不可见 / 部分失败被报成完全成功」类（延续 v14.13–v14.15 同一治理口径）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-q 动态流「加载更多」失败永不可见** | ① `loadMore` 对**业务失败**（`code!==200`）**直接跳过** —— 既不回滚页码也不报错 ⇒ 页码已 +1 却无数据追加，下次会**跳过一页**；② 错误块条件是 `error && list.length === 0`，被前面"列表非空"分支屏蔽 ⇒ **加载更多失败永远不显示** | ① 业务失败分支补**页码回滚 + 错误赋值**；② 在哨兵区补**内联错误 + 重试**（列表非空时也能看到） |
| **P2-r 专栏文章关联"假成功"** | `syncArticleRelations` 中 `addArticle`/`removeArticle` 失败只 `console.warn` 吞掉，调用方随后仍 `toast.success('专栏创建成功/已更新')` 并跳转 ⇒ 用户以为文章都关联好了 | 改为**统计失败数并返回**；调用方按结果提示：有失败 → `warning`「专栏已保存，但有 N 篇文章关联失败，请到"管理文章"确认」；唯一的索引冲突（已加入过）仍按**幂等成功**处理，不计失败 |
| **P2-s 在线运行失败"无处可查"** | `runCode` 失败时 `result` 保持 null ⇒ 右侧面板回落成"点击「运行代码」查看输出结果"的**空态**，失败原因只存在于一瞬的 toast | 新增常驻 `runError`：失败时在输出面板顶部渲染**持久错误区**（含图标与原因），不再依赖 toast |

> **本批统一口径的第三批落地**：**部分失败不得报成完全成功；失败原因必须常驻可见，而不是只存在于一瞬的提示里。**

### 二、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems** |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 52.91s`） |
| 后端 `mvn -o -B test`（本批无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 217（P0 6 / P1 75 / P2 85 / P3 51）· 订正 10 · 不做 2 · 未修 653** |

## v14.15 (2026-10-01) 全端评审落地（第 53 批）：两处分页竞态守卫 + 专栏价格"不生效"明确告知 + 金句点赞静默失败

**依据**：P2「竞态 / 失败静默 / 不生效设置」类。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-m 文章列表分页竞态** | `watch(currentPage, …)` 直接发请求，**无请求序号、无 AbortController**；`loading`/`error` 为共享状态 ⇒ 快速翻页/切分类时**先发的慢响应会覆盖新结果**，且旧请求的 `finally` 会**提前关掉新请求的 loading** | 加 `loadSeq` 序号守卫：过期响应 `if (seq !== loadSeq) return` 丢弃；`catch` 同样丢弃过期异常；`finally` 仅在 `seq === loadSeq` 时关 loading |
| **P2-n 我的面经筛选竞态** | 快速连点「草稿→已发布→已驳回」时无任何竞态防护（且 `httpGetList` 有 10s 结果缓存）⇒ 慢响应覆盖新列表 | 同一序号守卫口径（成功/异常/`finally` 三处同步收口） |
| **P2-o 专栏「价格」只入库不生效** | `portal_column.price` 列与接口都支持写入，但 **`toggleSubscribe` 完全不读 `price`**、门户无专栏支付链路 ⇒ 作者设了价也**收不到钱**（与 v14.13 简历模板"假价格"同类，但这里是"真入库、无效果"） | 保留字段（便于后续接入付费），在价格输入下**明确告知**："专栏付费链路尚未开通：价格会保存，但当前订阅免费、暂不生效" |
| **P2-p 金句点赞业务失败静默** | `resp.code !== 200`（或 `data` 为空）时**不提示、不改本地态**，只有抛异常才有 toast ⇒ 用户点了没反应，以为赞上了 | 业务失败分支与异常分支一致给出提示 |

> 口径延续 v14.13/v14.14：**失败要可见、空态与失败态分离、写操作以服务端返回为准、不生效的设置必须明说**。

### 二、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems** |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 52.70s`） |
| 后端 `mvn -o -B test`（本批无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 213（P0 6 / P1 75 / P2 81 / P3 51）· 订正 10 · 不做 2 · 未修 657** |

## v14.14 (2026-10-01) 全端评审落地（第 52 批）：名家录排除自己 + 关注结果以服务端为准 + 两处"失败伪装成空态"

**依据**：P2「状态一致性 / 静默失败」类（延续 v14.13 的同类治理）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-j 名家录包含本人 + 关注结果不看服务端** | ① `selectAuthors` 只按「公开主页 + 认证创作者 + 至少 1 篇已发布」筛选，**未排除当前登录用户** ⇒ 用户在名家录里看到自己；② `handleToggleFollow` **完全忽略后端返回值**，只按本地布尔翻转 —— 后端对"关注自己"返回 `code=200 + data.followed=false`（**不抛异常**），前端却置为"已关注"，用户看到成功但实际没关注上 | ① Mapper/SQL/服务/控制器四层增加 `excludeUserId`（=当前登录用户，**游客不传不过滤**）；② 前端以**服务端返回的 `followed`** 为准，`false` 时保持未关注并提示后端 message |
| **P2-k 错题本统计失败静默** | `loadStats` 是**空 catch**，失败时统计卡片因 `v-if="stats"` **整块不渲染** ⇒ 页面看起来"没问题"，用户既不知道数据没出来也无重试入口 | 新增 `statsError`：失败渲染**可见提示条 + 重试按钮**（仍不阻断列表） |
| **P2-l 面经详情把"加载失败"说成"没有评论"** | `loadComments` 失败只 `console.error`，而模板在 `loading=false && comments.length===0` 时**无条件**渲染「还没有评论，来抢沙发吧~」 | 新增 `commentError`，**失败态排在空态之前** + 重试按钮 |

### 二、验证

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` / `test` | ✅ exit 0 / **471/471**（`selectAuthors` 签名变更后全绿） |
| **名家录 SQL 两视图实测** | ✅ 游客视图（不追加排除）与登录视图（追加 `AND u.id != ?`）在真实库均**正常执行**；库中唯一认证创作者 `privacy_profile=0` 被既有的公开主页条件正确过滤（0 行符合预期） |
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 52.70s` |
| 对账重算 | ✅ **已修 204（P0 6 / P1 75 / P2 74 / P3 49）· 订正 10 · 不做 2 · 未修 666** |

> 本批与 v14.13 同属一类根因：**"失败/被拒"被静默处理，然后由 UI 编造一个看起来正常的状态**（假装成功、假装没有数据）。
> 治理口径统一为：**失败要可见、空态与失败态分离、写操作以服务端返回为准**。

## v14.13 (2026-10-01) 全端评审落地（第 51 批）：简历模板分类字段对齐 + 未公开书单越权读取 + 列表失败态 + 上传假成功

**依据**：P1 剩余「简历模板分类字段三方错配」+ P2 功能性缺陷（越权/误导/假成功）。

### 一、P1：后台简历模板「分类 / 价格」是假字段（提交后被静默丢弃）

**查实的完整链路**：

| 层 | 实际 | 结果 |
|---|---|---|
| 表 `portal_interview_resume_template` | 只有 **`category varchar(200)`**、`is_premium` | **无 `category_id`、无 `price`** |
| 后端接口 | `addResume/editResume(@RequestBody PortalInterviewResumeTemplate)` **以实体接收** | Jackson 忽略未知字段 ⇒ `categoryId`/`price` **静默丢弃** |
| 查询对象 | 字段是 `category` / `isPremium` | 前端传 `categoryId` ⇒ **筛了等于没筛** |
| 前端列表列 | 读 `row.categoryName` / `row.isPaid` | 后端返回实体无此二字段 ⇒ 分类恒 `-`、付费恒"免费" |
| **真实数据佐证** | 库中 2 条模板的 `category` **均为 NULL** | 证明"管理员设过分类却从未落库" |

**修复（对齐真实模型，不新增假列）**：分类改为编辑**分类名字符串** `form.category`（既可选既有分类名，也可 `allow-create` 直接输入）；是否付费改绑真实字段 `isPremium`；**移除无列的「价格」输入**（并在原处写明：若要做付费模板需先补 `price` 列与购买流程）；列表列改读 `category` / `isPremium`；查询参数改 `category`。

### 二、P2：未公开书单可被任何人按 id 读取（越权）

`GET /portal/reading/book-lists/{id}` 标注 **`@Anonymous`**（公开接口），而 `selectPortalBookListById` **只有 `where id = #{id}`** —— 后台设置的「是否公开 `is_public`」「状态 `active/inactive`」**完全未生效** ⇒ 私密书单及其书籍，未登录访客只要猜到 id 就能读。

**修复（fail-closed）**：仅 `is_public=1 && status='active'` 对所有人可见；其余**仅创建者本人**可见，且返回与"不存在"**相同文案**（不泄露存在性）；并把 `incrementViewCount` **移到校验之后**（未公开书单被偷访问不应产生浏览量）。

**谓词实测（事务内造数 + 回滚）**：

| 场景 | `is_public` | `status` | 判定 |
|---|---|---|---|
| 私密书单 | 0 | active | 仅创建者可读 ✓ |
| 公开书单 | 1 | active | 任何人可读 ✓ |
| 已下架书单 | 1 | inactive | 仅创建者可读 ✓ |
| 公开但状态为 NULL | 1 | NULL | 仅创建者可读 ✓（Java 用 `"active".equals(...)` 空安全，无 NPE） |

回滚后 0 行残留。

### 三、P2：两处"误导性反馈"

| # | 缺陷 | 修复 |
|---|---|---|
| 关注/粉丝列表 | `loadList` 失败只 `console.error` 并清空 ⇒ 模板渲染成「**暂无关注/暂无粉丝**」空态，无提示无重试；且「加载更多」失败**不回滚页码**会跳页 | 新增 `loadError` + **失败态与重试按钮**（与"真的没有"区分）；`catch` 回滚页码 |
| 举报反馈图片上传 | 业务失败时 `httpUpload` 仍 resolve 返回信封，页面只 `console.warn` 跳过，**循环结束却无条件 `toast.success('图片上传成功')`** ⇒ 用户以为传上去了，实际图片丢失 | 加成功/失败计数，按真实结果提示：全成功 → 成功（含张数）；全失败 → 失败；部分失败 → **warning 说明比例** |

### 四、★ 本批两次自查（都被工具抓住，非目视发现）

1. **补 `<AlertCircle>` 图标忘了 import** —— 与 v14.09 同一类问题（Vue 对未注册组件只在运行时告警）。因本次引用的是**图标组件**，被我提前发现并补上导入；
2. **`loadError` 脚本改动因缩进不匹配未应用，而模板改动已应用** ⇒ `vue-tsc` 立刻报 `TS2339: Property 'loadError' does not exist`。
   > 教训：**同一文件的多处锚点必须逐条核对缩进**；本次幸好有类型检查兜住"半应用"状态。

### 五、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` / `test` | ✅ exit 0 / **471/471** |
| **书单可见性谓词实测** | ✅ 四场景判定正确；回滚零残留 |
| 后台 `npm run build:prod` | ✅ exit 0（2830 modules / 52.35s） |
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 52.85s` |
| 对账重算 | ✅ **已修 200（P0 6 / P1 75 / P2 71 / P3 48）· 订正 10 · 不做 2 · 未修 670** |

> **P1 未修仅剩 1 条**：门户 `strict:false`（实测 74 个错误，需分模块重构，属独立工程）。

## v14.12 (2026-10-01) 全端评审落地（第 50 批）：**整改收尾总结**（交付物定稿）

**依据**：目标 ④ 的最终交付与"完成度如实说明"。

### 一、新增交付物

**`docs/09-临时报告/全端-评审-收尾总结-20261001.md`**（与对账主文档、对账明细 CSV 配套），含七节：

| 节 | 内容 |
|---|---|
| §1 | **一句话结论 + 最终数字**：已修 **194**（P0 6/6 · P1 74 · P2 66 · P3 48）· 订正 10 · 不做 2 · 未修 676 |
| §2 | 分波次执行记录（A 波 P0 → F 波 P3 机械批 + 对账波），49 批、每批四道验证 |
| §3 | **铁律遵守逐条对应证据**（devlog 登记、四同步、字典驱动、SQL 双轨、模块方向、金额口径、事务边界、fail-closed、后台可维护、守卫交互） |
| §4 | **★ 过程自查纠错集（10 条）**：对账虚高 558→194、通知筛选回归、批量误伤导入、加锁不放锁、类型检查抓不到的未导入组件、守卫漏判、YAML 缩进自伤、把守卫失败误当故障、SQL 互关语义单向、管道杀进程致落盘不一致 |
| §5 | 未修 676 条的**成因分类与后续建议**（逐页缺陷/可批量/需口径决策/需独立工程/风险高于收益），并指明"按 CSV 过滤未修即待办清单" |
| §6 | 验证与复现（四端全量 + 一次性专项的可复核清单）+ **尚需人工处理的 4 项**（轮换泄露 SMTP 授权码、后台 TS 工具链决策、门户 strict 排期、线上执行 6 个增量脚本） |
| §7 | **目标完成度如实说明**：流程性要求 ①②③ 与对账 ④ ✅；核心"882 条全修" ⚠️ 未完成（余 676，P0=0、P1=2） |

### 二、本次收尾的自我约束（写入文档，供后续接手者检验）

- **不把未修写成已修**：判断有误的 10 条单列「已订正」，**不计入已修**；
- **不把建议类改写成"已修复"**：评估后决定不做的（死 CSS 等）标注「不做」并附理由；
- **每条"已修"都必须可回溯**到 devlog 对应批次的代码改动 + 当时验证结果。

### 三、校验

| 项 | 结果 |
|---|---|
| 交付物齐备 | ✅ 收尾总结 + 对账主文档（33KB）+ 对账明细 CSV（883 行 × 8 列） |
| 三者数字一致 | ✅ 文档表格 = CSV 机器解析 = 生成脚本统计（**194 / 10 / 2 / 676**，合计 882） |
| 四端验证 | ✅ 后端 **471/471** · 门户 `vue-tsc` 0 / `eslint` **0 problems** / build ✓ · 后台 build ✓ · 记账端 build ✓ |
| devlog 链完整 | ✅ v13.71 – v14.12 共 **50 个版本**，逐批可追溯 |

## v14.11 (2026-10-01) 全端评审落地（第 49 批）：**四端全量验证 + 对账最终刷新**

**依据**：目标 ② 「每波改完即跑编译/类型检查/构建/守卫测试」的收尾核对，以及目标 ④ 的对账定稿。

### 一、四端全量验证（本轮一次性跑齐）

| 端 | 命令 | 结果 |
|---|---|---|
| 后端 | `mvn -o -B test` | ✅ **471/471，BUILD SUCCESS** |
| 门户 | `vue-tsc -b --force` / `eslint . --ext .ts,.vue` / `npm run build` | ✅ exit 0 / ✅ **0 problems** / ✅ `built in 49.45s` |
| 后台 | `npm run build:prod` | ✅ exit 0（2830 modules，`built in 49.92s`） |
| 记账端 | `npm run build:h5` | ✅ 真实网关域名 → `DONE Build complete.` |

**★ 记账端"失败"是我自己加的守卫在正常工作（如实记录）**：不带 `VITE_API_BASE_URL`（或仍为示例域名）时构建**按设计失败**并打印可操作提示：
> `[api-base-url] 生产构建的 VITE_API_BASE_URL 是示例域名 example.com：https://api.example.com`
> `占位/本地域名上线会导致所有请求失败；请改为真实网关域名。`

换成真实域名后立即 `DONE Build complete.` 退出码 0 ⇒ **守卫两向行为均符合预期**（v13.73 引入，防止占位域名上线）。

### 二、对账定稿（三者一致）

| 来源 | 已修 | 已订正 | 不做 | 未修 | 合计 |
|---|---:|---:|---:|---:|---:|
| 对账文档 §1 | 194 | 10 | 2 | 676 | 882 |
| 明细 CSV（机器解析） | 194 | 10 | 2 | 676 | 882 |
| 生成脚本统计 | 194 | 10 | 2 | 676 | 882 |

分严重度：**P0 6/6 全修** · P1 74 已修（+5 订正，余 2 未修）· P2 66 已修 · P3 48 已修。
CSV 结构：**883 行（882+表头）× 8 列，状态仅 4 种取值，0 行异常**。

**文档同步增强**：§6「验证证据」表补齐**四端构建**、**守卫负例（含初始化脚本守卫两向）**、
**关系字段语义四场景**、**通知类型过滤（3 → 2/1）** 三类一次性验证记录 —— 让"已修"可被独立复核。

### 三、关于目标完成度的如实说明（写给后续接手者）

- **P0（6 条）与 P1（82 条）已实质收口**：P0 全修；P1 已修 74 + 订正 5，**余 2 条**（简历模板分类字段需数据模型对齐、门户 `strict` 需分模块重构）。
- **P2/P3 仍有大量未修**（266 + 408 = 674 条）：其中约六成是**逐页独立缺陷**（每条需单独读码定位），
  已在对账明细 CSV 中**逐条列明模块/页面/功能/问题/严重度/状态/建议**，可直接作为后续迭代表使用。
- **本轮不存在"假修复"**：所有"已修"均可回溯到 devlog 对应批次的代码改动与验证结果；
  判断有误的条目单列 §3「已订正」（10 条），不计入已修。

## v14.10 (2026-10-01) 全端评审落地（第 48 批）：学习计划"清空不落库" + 认证页静默失败 + 首页去写死兜底

**依据**：P2「状态/数据一致性」与「写死兜底」两类。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-g 学习计划清空可选字段不生效** | 编辑计划时清空「目标数量/目标分类/开始日期/结束日期」，前端按 `x \|\| undefined` 提交 ⇒ JSON 丢弃该键 ⇒ VO 为 null ⇒ MyBatis-Plus 默认 **`NOT_NULL`** 策略**不把该字段写进 UPDATE** ⇒ **永远清不掉**（与 v13.80 话题 C-30 同一类问题，但根因在后端更新策略） | 实体这 4 个可空字段显式声明 **`@TableField(updateStrategy = FieldStrategy.ALWAYS)`** ⇒ null 也写入（即置 NULL）。**前端语义随之自动正确**，无需改动 |
| **P2-h 创作者认证页加载失败静默** | `loadMy()` 的 `catch` 只 `console.warn`（注释称"由路由守卫负责"），但守卫只管未登录 —— 真正的接口/网络失败会让页面渲染**空白申请表**，用户可能在"其实已提交过"的情况下**再次提交** | 新增 `loadError` 状态并记录可读错误（不再静默） |
| **P2-i 首页两处"写死兜底"** | ① 主题 Tab：`themes` 为空时渲染写死的「散文」主题，点击会把写死的 `rootCategoryId='1'` 传给后端 ⇒ 展示与主题无关的内容；② 热门标签：为空时渲染「文学/散文/随笔」三个假标签，点击进入**无数据的 `/tag` 页** | 均改为**无数据不渲染假数据** + 中性提示（"主题/标签暂不可用，稍后再试"），既不误导用户也不产生错误请求 |

### 二、★ 改后端更新策略前的前置风险评估（避免"修一处坏一处"）

`FieldStrategy.ALWAYS` 的副作用是：**部分更新时未加载的字段会被置 NULL**。故先排查本实体的全部写入路径：

| 写入点 | 是否完整实体 | 结论 |
|---|---|---|
| `savePlan()` 的 `updateById`（L107） | ✅ 由 VO 逐字段 set（含这 4 个字段） | 安全 |
| `changeStatus()` 的 `updateById`（L195） | ✅ 先 `mustOwnPlan()` **selectById 加载完整实体**再改 status | 安全（4 个字段按库中原值回写） |
| 其它 | 无（仅 `insert` / `deleteById`） | — |

**列可空性实测**：`portal_study_plan` 的 `target_count / target_category / start_date / end_date` **四列全部 `IS_NULLABLE=YES`** ⇒ 写 NULL 合法。

### 三、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test` | ✅ **471/471，BUILD SUCCESS** |
| 列可空性实测 | ✅ 四列均可空 |
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 49.45s` |
| 对账重算 | ✅ **已修 194（P0 6 / P1 74 / P2 66 / P3 48）· 订正 10 · 不做 2 · 未修 676** |

## v14.09 (2026-10-01) 全端评审落地（第 47 批）：文章 Tab 服务端分页 + 题库列表竞态守卫

**依据**：P2「前端交互/状态一致性」类。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-e 个人页文章 Tab 只有第一页** | `loadArticlesTab()` 只请求一次 `pageNum:1, pageSize:10`，再用 `slice` 在前端"分页"，且模板**从未渲染任何翻页控件** ⇒ **第 11 篇之后的文章永远看不到**（`currentPage/itemsPerPage` 形同虚设） | 改为按 `currentPage/itemsPerPage` **请求后端**，记录 `total` 计算真实页数，并接入既有 `Pagination` 组件 + `handleArticlesPageChange()` 翻页重取 |
| **P2-f 题库列表竞态** | `watch([...])` 每次筛选变化立即 `loadQuestions()`，而函数内**既无请求序号守卫也无 AbortController** ⇒ 快速切换筛选/翻页时，**先发出的响应可能后到达并覆盖新结果**（列表与筛选条件不一致） | 加请求序号 `loadSeq`：进入时 `const seq = ++loadSeq`，响应回来后 `if (seq !== loadSeq) return;` **丢弃过期响应** |

### 二、★ 一次"类型检查抓不到"的问题（自查发现）

给文章 Tab 补 `<Pagination>` 后，`vue-tsc` 与 `eslint` **都通过**了 —— 但**该组件从未被 import**
（UserPage 原本没有引用过它）。Vue 对未注册组件只在**运行时**告警（"Failed to resolve component"），
模板仍会渲染、控件静默消失 ⇒ **类型检查不等于模板正确**。

**处置**：补 `import Pagination from '@/components/Pagination.vue';` 后重新跑三道验证。
> 教训：**新增模板标签必须同时确认已导入**；本项目 `vue-tsc` 不会对未知组件报错，不能只依赖类型检查。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems** |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 51.86s`） |
| 后端 `mvn -o -B test`（本轮无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 186（P0 6 / P1 74 / P2 61 / P3 45）· 订正 10 · 不做 2 · 未修 684** |

## v14.08 (2026-10-01) 全端评审落地（第 46 批）：隐私开关"只写不读"治理（privacy_follow 强制生效）

**依据**：P2「隐私设置四个开关只写库、全后端无读取点」。

### 一、核查：五个隐私开关逐个查清（不靠猜）

| 开关 | DDL 语义 | 是否存在公开暴露面 | 结论 |
|---|---|---|---|
| `privacy_profile` | 是否公开主页（名家录/作者列表展示） | 是 | **早已生效**（`selectAuthors` 等按 `privacy_profile=1` 过滤） |
| `privacy_follow` | **是否允许被关注** | 是（`POST /portal/follow/{userId}`） | ✅ **本批修复**（见下） |
| `privacy_email` | 是否公开邮箱 | **否** | 公开资料 `UserProfileVO` 本就是**白名单**（不含 email/phone/登录信息，v13.76 已改）⇒ 无泄露面，开关冗余但无害 |
| `privacy_phone` | 是否公开手机号 | **否** | 同上 |
| `privacy_bookmark` | 是否公开收藏夹 | **否** | 收藏接口只有 `/portal/bookmark/my`（**仅本人**），不存在"看他人收藏夹"的入口 ⇒ 当前无暴露面 |

> 结论：原报告"四个开关全无读取点"**方向正确**，但需要区分——只有 `privacy_follow` 真的有可被违反的承诺；
> 另两个（邮箱/手机）因公开 VO 是白名单而**不存在泄露路径**；收藏夹则**没有公开入口**。

### 二、修复：`privacy_follow`（是否允许被关注）强制生效

**为什么选在服务层**：门户所有关注入口最终都走 `IPortalFollowService`（`toggleFollow` 与 `follow` 两个方法），
在此收口即可覆盖全部入口，避免"这个入口拦了、那个没拦"（与本轮 v14.06 的 VIP 门禁同一思路）。

| 内容 | 说明 |
|---|---|
| 注入 | `PortalFollowServiceImpl` 注入 `PortalUserMapper`（同模块，无新增跨模块依赖） |
| 判定 | 新增私有 `followBlockedReason(followingId)`：目标用户 `privacy_follow=false` 时返回提示文案 |
| 拦截点 | `toggleFollow` 与 `follow` **两处**均在被关注前校验（**共 2 处，已核对无第三处**） |
| 语义边界 | **只在"建立关注"时拦截**；取消关注、`isFollowing` 查询不受影响（否则用户无法解除关注） |
| 空值口径 | `Boolean.FALSE.equals(...)`：列为 NULL（历史数据）或 true 均视为**允许**，与 DDL 默认 1 一致 |

**关键前置校验（否则改动无效）**：确认 `selectPortalUserById` 所用的
`selectPortalUserVo` 列清单**确实包含 `privacy_follow`**、且 resultMap 有 `privacyFollow → privacy_follow` 映射
—— 否则实体里该字段恒为 null、判断永不触发。实测：列清单包含 ✓、resultMap 映射 ✓、库列 `DEFAULT 1` / 可空 ✓。

### 三、过程自纠

插入辅助方法时，原本属于 `isFollowing` 的 `@Override` 被**落在了辅助方法之前**（悬空注解）⇒ 编译报
"方法不覆盖或实现超类型的方法"。已把注解归位，并复查 `isFollowing` 前**有且仅有一个** `@Override`。

### 四、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test` | ✅ **471/471，BUILD SUCCESS** |
| **列/映射前置校验** | ✅ `selectPortalUserVo` 含 `privacy_follow`；resultMap 有映射；库列 `DEFAULT 1` 可空 |
| 门户 `vue-tsc` / `eslint` | ✅ exit 0 / **0 problems** |
| 对账重算 | ✅ **已修 183（P0 6 / P1 74 / P2 59 / P3 44）· 订正 10 · 不做 2 · 未修 687** |

## v14.07 (2026-10-01) 全端评审落地（第 45 批）：四处 P2 实修 + 一处 P1 复核（P1 未修降至 2）

**依据**：P2 中"功能性错误/静默失败"类。

### 一、实修四处（均为用户可感知的功能性问题）

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-a 优化页关键操作静默无反应** | `generateOptimize()` 开头 `if (!selectedResumeId \|\| !selectedTargetId) return;` ⇒ 用户点"生成优化建议"后**毫无反应**，分不清是缺简历还是缺岗位目标 | 拆成两条前置判断，分别提示「请先选择要优化的简历」/「请先选择目标岗位」 |
| **P2-b 钱包"加载更多"失败会跳页** | `loadMoreLedger` / `loadMoreWithdraw` **先 +1 页码再请求**，失败只 `toast` 而不回滚 ⇒ 用户重试时**跳过一整页**数据 | 失败分支回滚页码（`Math.max(1, current - 1)`），重试即重取同一页 |
| **P2-c 题目详情游客被误弹"登录已过期"** | 停留 60s 的阅读埋点调用 `recordQuestionRead`，该接口**非 `@Anonymous`**，游客会 401；客户端全局 401 处理会弹出"登录已过期/请先登录"确认框 —— 与代码注释"未登录静默失败"的意图**直接矛盾** | `reportRead()` 前置 `if (!isAuthenticated()) return;`：游客**不发请求**，自然不弹窗 |
| **P2-d 作者关注可连点、计数会变负** | 关注请求**无锁、按钮无 disabled** ⇒ 快速连点会重复调用 follow 且每次都 `followers++`；取消关注**无条件 `--`**，而统计接口失败时 `followers` 初值为 0 会被减成负数 | 加 `followLoading` 请求锁 + 按钮 `:disabled`（含 loading 图标）+ **`finally` 释放**；取消关注计数用 `Math.max(0, …)` 兜底 |

> **过程自纠（重要）**：我第一版加锁时**只设置了 `followLoading = true` 却没有 `finally` 释放** ——
> 那会让按钮在**一次失败后永久不可点**（比原缺陷更糟）。复查时补上 finally 与按钮 disabled 绑定。
> 教训：**加锁必须成对**（获取 + 释放），且释放要放在 `finally`。

### 二、一处 P1 经查码确认**已修**（据实改判）

`MyBookmarksPage` 收藏列表题目信息：页面在加载时已把嵌套的 `b.question` **展开为"题目扁平字段 + bookmarkTime"**
（`{ ...b.question, bookmarkTime: b.createTime }`），因此模板读 `item.title/difficulty/...` 与跳转用 `item.id`（题目 id）**都是对的** ⇒ 原报告所述"字段取不到"不成立。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems** |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 51.60s`） |
| 后端 `mvn -o -B test`（本轮无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 182（P0 6 / P1 74 / P2 58 / P3 44）· 订正 10 · 不做 2 · 未修 688** |

> **P1 未修仅剩 2 条**：`ResumeTemplatePage` 分类字段三方错配（需数据模型对齐：表/实体无 `category_id`，后台表单维护的 `categoryId` 提交后被静默丢弃）、门户 `strict`（已实测 74 错、属重构）。

## v14.06 (2026-10-01) 全端评审落地（第 44 批）：通用 AI 任务补会员门禁 + 关注列表关系字段（P1 未修降至 3）

**依据**：P1 剩余项中两处需后端改动者。

### 一、修复 1：通用 AI 任务入口可**绕过**深度优化的会员校验（营收/安全）

`@VipOnly` 只作用于被标注的**那个方法**。深度优化的两个专用入口都标了
`@VipOnly(platform="portal", benefit="resume_optimize")`，但通用入口 `POST /portal/ai/task/submit`
接受**任意 taskType** ⇒ 直接提交 `taskType=deep_optimize` 即可**完全绕过**会员校验与次数扣减（付费能力被白嫖）。

**修复**：在 `PortalAiTaskController` 增加**会员权益门禁登记表** `VIP_GATED_TASKS`（taskType → platform/benefit/consume/文案），
提交前按表校验；取值与专用入口注解**逐项对齐**（`consume=true` + **402** 语义，前端据此弹开通引导）。

> 设计取舍：把门禁收敛到**一处登记表**，新增受权益保护的异步任务类型只需登记一行 ——
> 避免"每个入口各写一遍校验"再次漏掉某个入口（本次漏洞的成因正是如此）。

### 二、修复 2：关注/粉丝列表缺"观察者视角"字段

`FollowListPage` 依赖 `item.following / mutualFollow / isMe`（前端类型早已声明），
但后端 `FollowUserVO` 只有 `id/userId/username/nickname/avatar/bio/position/createdAt`，
两条列表 SQL 也不返回关系字段 ⇒ 关注状态、互关标识、"自己"标识**永远为空**。

**修复（服务端权威，四层）**：

| 层 | 内容 |
|---|---|
| VO | 补 `following` / `mutualFollow` / `isMe`；**游客（viewerId 为 null）时三个字段为 `null`**，与"未关注(false)"区分开 |
| Mapper XML | 两条列表 SQL 增加三列计算（`CASE` + `EXISTS`）；新增 `viewerId` 参数 |
| Mapper/服务 | 方法加 `viewerId` 参数并透传 |
| 控制器 | 以 `PortalSecurityUtils.getUserId()` 作为观察者传入（游客为 null） |

**★ 自查抓出并修正的一处语义错误**：我第一版的 `mutual_follow` **只判断了"对方是否关注我"单向**，
而"互相关注"必须**双向**。已修正为两个 `EXISTS` 相与。

### 三、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test` | ✅ **471/471，BUILD SUCCESS**（含 `ModuleDependencyGuardTest`：新增 `portal → vip` 引用未触犯冻结边） |
| **关系字段语义实测（事务内造数 + 回滚）** | ✅ 见下表 |
| 门户 `vue-tsc` / `eslint` | ✅ exit 0 / **0 problems** |
| 对账重算 | ✅ **已修 171（P0 6 / P1 73 / P2 51 / P3 41）· 订正 10 · 不做 2 · 未修 699** |

**关系字段实测**（造数：101→102、102→101、101→103、104→101）：

| 场景 | 目标 | is_me | following | mutual_follow | 判定 |
|---|---|---|---|---|---|
| 粉丝列表（目标 101，观察者 101） | 102 | 0 | **1** | **1** | 101↔102 双向 ✓ |
| 粉丝列表（目标 101，观察者 101） | 104 | 0 | **0** | **0** | 104→101 仅单向 ✓ |
| 关注列表（目标 101，观察者 103） | 103 | **1** | 0 | 0 | 目标即观察者本人 ✓ |
| 游客视角（viewerId=NULL） | 102/104 | **NULL** | **NULL** | — | 游客与"未关注"可区分 ✓ |

`ROLLBACK` 后 `portal_follow` 剩余 **0 行**（零持久改动）。

> **P1 未修仅剩 3 条**：简历模板分类字段三方错配、收藏列表题目信息（后端 VO 缺字段）、门户 `strict`（已实测 74 错、属重构）。

## v14.05 (2026-10-01) 全端评审落地（第 43 批）：资料页服务端刷新 + 去假字段 + **初始化脚本加"非空库即中断"守卫**（P1 未修降至 5）

**依据**：P1 剩余项继续逐条处理。

### 一、实修三处

| # | 缺陷 | 修复 |
|---|---|---|
| **P1-C 个人资料页只吃本地快照** | `UserProfilePage` 挂载时只读 `userStore.user`（**localStorage 快照**）；store 在 `isUserInitialized` 为 true 时**直接返回快照、不再请求后端** ⇒ 多设备登录或后台改过的资料，本页永远是旧值 | 挂载时先 `await userStore.fetchCurrentUser()`（请求 `GET /portal/user/me` 并回写 store）再回填表单；刷新失败则退回本地快照并 `warn`（不阻断页面） |
| **P1-D 面经列表读"假字段"** | `expName/expAvatar` 先读 `(exp as any).user` —— 后端实体与 VO **都没有** `user` 对象（是 `types/api.ts` 单方面声明的假字段），永远 `undefined`，只是靠 `&&` 回退到真实字段才没出事（"看着在取值、其实走的是回退"） | 直接用 VO 真实扁平字段 `userNickname` / `userAvatar` + 兜底 |
| **P1-E 初始化脚本无环境守卫** | `moyun-db-ddl.sql` 含 **49 处 `DROP TABLE` + 3 处 `TRUNCATE`**，对已有数据的库执行会直接删表/清表且**不可逆**，脚本自身没有任何拦截 | 文件开头新增**失败即中断**守卫：以 `sys_menu` 为哨兵查 `information_schema`，库非空则构造一条**引用不存在表**的语句 ⇒ MySQL 直接 `ERROR 1146` 中断，**不会执行到任何 DROP** |

**守卫的两向实测（关键：安全机制必须自证）**：

| 场景 | 期望 | 实测 |
|---|---|---|
| 在**非空库** `moyun-db` 执行守卫 | 拒绝并中断 | ✅ `ERROR 1146 ... '__refuse__init_sql_on_non_empty_db__' doesn't exist`，exit 1 |
| 在**空库**（临时建 `moyun_guard_test`）执行 | 放行 | ✅ 输出 `ok: 空库，可安全执行初始化`，exit 0 |
| 测试库清理 | — | ✅ `DROP DATABASE moyun_guard_test` 已执行 |

> 设计说明：这是**有意的摩擦** —— 确需重建时应先备份并显式删掉哨兵判断，而不是让脚本默默把表删掉。

### 二、三处 P1 经查码确认**已修**（据实改判）

| 清单行 | 结论 |
|---|---|
| `MessagesPage` 待办 Tab 类型参数 | 已由 **v14.02** 的后端 `type` 过滤真正生效 |
| `ResumeTemplatePage` 分页参数名 | 已改用 `pageNum`（原 `page` 被 Spring 忽略） |
| `ContestListPage` 状态枚举 | 已于 **v14.02** 改字典驱动 + 未命中不再误标"全部" |

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems** |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 51.94s`） |
| 后端 `mvn -o -B test` | ✅ **471/471**（`moyun-db-ddl.sql` 变更后 `DdlConventionGuardTest` 仍通过） |
| **DDL 守卫两向实测** | ✅ 非空库中断 / 空库放行 |
| 对账重算 | ✅ **已修 169（P0 6 / P1 71 / P2 51 / P3 41）· 订正 10 · 不做 2 · 未修 701** |

> **P1 未修仅剩 5 条**：深度优化 VIP 门禁（通用任务接口绕过）、简历模板分类字段三方错配、关注列表关系字段（后端 VO 缺）、收藏列表题目信息（后端 VO 缺）、门户 `strict`（已实测 74 错，属重构）。

## v14.04 (2026-10-01) 全端评审落地（第 42 批）：两处 P1 实修 + 三处 P1 复核改判（P1 未修降至 11）

**依据**：P1 剩余项逐条处理（能修的修、已修的据实改判）。

### 一、实修两处（都是"看着有、其实没用"的缺陷）

| # | 缺陷 | 修复 |
|---|---|---|
| **P1-A 作者页关注状态是空壳** | `AuthorsPage.loadFollowingStates()` 只把每位作者写成 `false`，注释自认"假设…如果不存在则跳过"，而 `GET /portal/follow/check/{userId}` **早已存在**（`followApi.checkFollow` 也已封装） ⇒ 已关注的作者仍显示"关注"，点击后由后端按"已关注"处理，观感像"点了没用" | 改为**真实调用**：未登录直接跳过（关注状态按当前登录用户判定）；**分批并发**（每批 8 个）控制瞬时请求数；单个失败不影响其它项 |
| **P1-B 语音面试历史「报告生成中」判定错** | `isGenerating` 用 `(analysisStatus ?? 0) < 2`，而后端语义是 **0=从未提交分析 / 1=进行中 / 2=完成**（`VoiceInterviewServiceImpl` 分别置 0/1/2）⇒ **从未分析的记录也显示"报告生成中"并持续轮询** | 改为只认 `analysisStatus === 1`；`null`（历史数据）也不再误判为生成中 |

### 二、三处 P1 经查码确认**已修**（据实改判，不计入"已修"虚增）

| 清单行 | 查码结论 |
|---|---|
| `PortalLoginController` 注册直接绑实体（可伪造 role/vip_expire_at） | **已有 `sanitizeRegisterFields()`**（L240-259）：强制 `role=user`、清空 `vipExpireAt`/`isCertifiedCreator`/`platformCode`/`loginIp`/`loginDate`、认证标记按验证码流程判定、`birthday` 只接受 `yyyy-MM-dd` 其余归 null ⇒ 越权字段不可注入 |
| `SearchPage` 排序参数后端不认 | **已改**为 PageDomain 真实列名 `orderByColumn/isAsc`（热门→`views`、最新→`create_time`，推荐不传走后端默认序） |
| `ResumeOptimizePage` 优化历史跳转用了不存在的路由名 | **已改**为路径跳转 `/interview/resume/edit/{id}`，不再依赖路由名 |

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems** |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 51.87s`） |
| 后端 `mvn -o -B test`（本轮无 Java 改动，作回归守卫） | ✅ **471/471** |
| 对账重算 | ✅ **已修 161（P0 6 / P1 65 / P2 49 / P3 41）· 订正 10 · 不做 2 · 未修 709**；CSV 883 行 × 8 列、0 行异常 |

> **P1 未修已降至 11 条**，且剩余多为"需后端补字段/产品口径/独立工程投入"（如 `ResumeTemplatePage` 分类字段三方错配、`MyBookmarksPage` 收藏题信息字段、`PortalUser.birthday` 类型、DDL 幂等性、门户 strict）。

## v14.03 (2026-10-01) 全端评审落地（第 41 批）：反馈字典补齐 + 反馈页字典化 + 两处 P1 复核

**依据**：P1 剩余项（`MyFeedbackPage` 枚举写死 + 字典空转；另两条经查码确认已修）。

### 一、修复：反馈类型/处理状态字典"有类型无数据" → 补齐 + 前端字典化

`cms_handle_status` / `cms_feedback_type` 在 `sys_dict_type` 中有类型行，`sys_dict_data` **一行数据都没有**
⇒ 后台字典管理下拉为空；门户「我的反馈」只能把 4 个状态 + 4 个类型（含颜色）写死在组件里，与后台口径必然漂移。

| 层 | 内容 |
|---|---|
| 字典数据 | 新增增量脚本 `20261001-06-反馈类型与处理状态字典数据补齐（v14.03）.sql`：**判重补齐**（`INSERT ... SELECT ... WHERE NOT EXISTS`），`cms_feedback_type` 4 行 + `cms_handle_status` 判重 4 行；同步 `init-sql/moyun-db-dml-init.sql` |
| 前端 | `MyFeedbackPage` 的 `statusOptions`/`typeOptions` 改为 `useDictData` 驱动 + **本地兜底**（与「我的举报」同写法）；`computed` 化后同步修正脚本内 `getStatusMeta`/`getTypeLabel` 的 `.value` 访问 |

### 二、两条 P1 经查码确认**已修**（据实改判，避免虚增）

| 清单行 | 查码结论 |
|---|---|
| `PayGatewayImpl` 支付回调金额校验 | **已实现**：`PayNotifyMessage` 有 `amount` 字段；`parseNotify` 提取金额（微信 `amount.total` 为**分**，`movePointLeft(2)` 归一到元）；`handleNotify` 金额不一致**拒绝处理**，未携带金额则 `warn` 留痕（不把"没校验"当成"校验通过"） |
| `FollowListPage` 行点击跳作者主页 | 模板已用 `String(item.userId)`（与面包屑同口径） |

### 三、★ 对账匹配修正：页面匹配改为**大小写不敏感**

`PayGatewayImpl.java` 这类页面名此前因大小写（关键字写的是 `pay`）未命中 ⇒ 已修项被算作"未修"。
改为大小写不敏感后，**已修 143 → 151**（这点提升来自"修正漏算"，不是放宽标准：仍要求**页面 + 关键词**双命中）。

### 四、★ 一次工具使用教训（如实记录）

生成对账时我用 `node gen-reconciliation.js | Select-Object -First 6` 只看前几行输出 ——
**`Select-Object -First` 会提前关闭管道**，node 进程在打印统计后、写 JSON/CSV 前被终止
⇒ 屏幕上显示的是**新数字（154）**，而落盘的 CSV/JSON 仍是**旧数字（143）**，两者短暂不一致。

**处置**：重跑生成器（重定向到 `$null` 而非截断管道），随后用独立脚本读取落盘文件复核
⇒ 三者一致（CSV 分布 = JSON 统计 = 文档表格 = **154/10/2/716**）。
> 教训：**"看输出"与"看产物"要分开验证**；截断管道会杀掉生产者，别用它读有副作用的脚本输出。

### 五、校验

| 项 | 结果 |
|---|---|
| **字典补齐实跑 + 幂等** | ✅ 首次 `cms_feedback_type=4` / `cms_handle_status=4`；**二次执行行数不变** |
| 后端 `mvn -o -B test` | ✅ **471/471，BUILD SUCCESS**（新增量脚本过守卫） |
| 门户 `vue-tsc` / `eslint` / `npm run build` | ✅ exit 0 / **0 problems** / `✓ built in 49.84s` |
| 对账重算 | ✅ **已修 154（P0 6 / P1 60 / P2 49 / P3 39）· 订正 10 · 不做 2 · 未修 716**；CSV 883 行 × 8 列、0 行异常 |

## v14.02 (2026-10-01) 全端评审落地（第 40 批）：P1 复核 + 通知类型过滤补齐（**含修正我自己引入的一处回归**）

**依据**：对账产出后回到 P1 剩余项逐条复核，并修复其中真实缺陷。

### 一、逐条复核：10 条 P1 实为"报告已过时/此前已修"（据实改判）

为避免"看起来修了"，逐条查码确认，并把它们从"未修"改判为"已修"（附 devlog 依据）：

| 清单行 | 查码结论 |
|---|---|
| `TopicEditPage` 清空描述/封面无效 | 已改为提交空串（C-30） |
| `TopicEditPage` 移除封面真删文件 | 已改为"标记待删"`pendingCoverDeletion`（C-31） |
| `TopicEditPage` 描述无长度校验 | 已加 `maxlength` + 后端校验（C-32） |
| `TopicEditPage` 被驳回后重新编辑 | 已重置为 pending 并重建审核任务 |
| `AuthorPage` 未登录关注静默 | 已提示并跳登录（带回跳） |
| `TopicListPage` 发起人字段嵌套 | 已用平铺 `creatorNickname/creatorAvatar` |
| `MembershipPage` durationDays 为空 | 已显示"暂不可购买（未配置有效期）"且不可下单 |
| `WalletPage` 提现状态缺 `paying` | 已含「打款中」，与后端四值状态机对齐 |
| `AuthorsPage` 搜索不重置页码 | `watch([searchQuery, sortBy])` 已重置 |
| `MyExperiencesPage` 时间逗号表达式 | 实际为 `createTime \|\| updateTime` 两参调用，**原报告所述不存在** |

> 结果：**P1 已修 45 → 55**，P1 未修 31 → **21**。对账明细与 CSV 已同步重算。

### 二、修复 `ContestListPage` 状态枚举（同时闭合相关 P2）

页面把 `collecting/voting/ended` 与 4 个十六进制色值写死，与后台（用字典 `cms_contest_status`）口径不一致；
且 `statusMeta` 未命中时 `return statusOptions[0]`（即「全部」）⇒ 出现未登记状态时卡片被标成"全部"。

**修复**：改字典驱动（字典已由 `20261001-04` 补齐）+ 本地兜底；**未命中时原样显示状态码**并给中性色（不再误标"全部"）；
筛选项排除 `draft`（列表接口本就不返回草稿）。

### 三、★ 修复一个"筛选看起来生效、其实没生效"的缺陷（**并修正我 v13.98 引入的回归**）

**发现过程**：复核「待办 Tab」条目时去查后端，发现 `PortalNotificationController.list` 虽然收到 `NotificationQuery`，
但 `selectUserNotifications(page, userId, userType)` **根本不把 query 传下去** —— `selectAllByUserId` 的 SQL **没有任何类型条件**。这意味着：

1. **既有缺陷**：「通知」Tab 会**混入 `type='todo'` 的待办**（而未读数统计 SQL 早已排除 todo ⇒ 同一页面两处口径不一致）；
2. **我在 v13.98 引入的回归**：那批把通知筛选改为"服务端筛选"（前端传 `type`），但后端**忽略该参数** ⇒
   筛选**从"本地过滤已加载数据（至少能用）"退化为"完全不过滤"**。这是我造成的，必须修正。

**修复（服务端权威，四层）**：

| 层 | 内容 |
|---|---|
| Mapper XML | `selectAllByUserId` 增加 `<if test="type...">and n.type = #{type}</if>` 与 `<if test="excludeTodo...">and n.type != 'todo'</if>` |
| Mapper 接口 | 新增 `@Param("type") String type, @Param("excludeTodo") Boolean excludeTodo` |
| 服务接口/实现 | 新增五参重载；**保留原三参方法并委托**（系统用户收件箱行为不变） |
| 门户控制器 | 把 `query.getType()` 真正传下去；未指定类型时 `excludeTodo=true`（「通知」Tab 语义） |

**真实库验证（事务内造数 + 回滚，零持久改动）**：

```
插入 3 条（like / comment / todo）
修复前(无类型过滤)          → 3   ← 待办混进通知列表（复现既有缺陷）
通知Tab(type 为空⇒排除todo) → 2   ← 修复后正确
待办Tab(type=todo)          → 1   ← 修复后正确
ROLLBACK 后剩余行数          → 0
```

### 四、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test` | ✅ **471/471，BUILD SUCCESS** |
| **类型过滤真实库验证** | ✅ 3 → 2/1，`ROLLBACK` 后 0 行残留 |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems** |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 49.87s`） |
| 对账重算 | ✅ 已修 **143**（P0 6 / P1 55 / P2 45 / P3 37）· 未修 728 |

## v14.01 (2026-10-01) 全端评审落地（第 39 批）：**产出「已修/未修/不做」完整对账**（目标 ④）

**依据**：目标 ④「每批完成后更新问题清单的处理状态，最终给出"已修/未修/不做"的完整对账」。

### 一、交付物

| 文件 | 内容 |
|---|---|
| `docs/09-临时报告/全端-评审-对账-20261001.md` | 对账主文档（口径声明 / 总览 / 已修按批次 / 已订正 / 不做 / 未修分布与成因 / 验证证据 / 结论） |
| `docs/09-临时报告/全端-评审-对账明细-20261001.csv` | **882 行逐条明细**（模块,页面,功能,问题,严重度,状态,devlog,说明），可直接筛选排序 |

### 二、对账结果（882 条）

| 状态 | 合计 | P0 | P1 | P2 | P3 |
|---|---:|---:|---:|---:|---:|
| **已修** | **131** | **6（全）** | 45 | 45 | 35 |
| 已订正（原判断有误/环境相关） | 10 | 0 | 5 | 4 | 1 |
| 不做（附理由） | 1 | 0 | 1 | 0 | 0 |
| **未修** | **740** | 0 | 31 | 287 | 422 |
| 合计 | 882 | 6 | 82 | 336 | 458 |

### 三、★ 关于"已修"数字的自我纠偏（本批最重要的过程记录）

对账第一版**只用关键词**匹配修复记录，算出"已修 **558** 条" —— **明显虚高**：
`未使用`/`死代码`/`点赞`/`金额`/`轮询` 这类泛词会把大量**并未修复**的行匹配进来。

我随后**三次收紧**，每次都只降不升：

| 版本 | 匹配规则 | 已修 |
|---|---|---:|
| 第 1 版 | 仅关键词 | 558（**虚高，弃用**） |
| 第 2 版 | 页面 + 关键词 | 146 |
| 第 3 版 | 逐条抽查剔除误命中（`router`/`残留`/`toggleVote`/`head` 等泛词） | 134 |
| **第 4 版（定稿）** | 再收窄日历/征文等条目 | **131** |

> **原则**：宁可少算也不多算 —— **未匹配上的行一律计为"未修"**。对账若虚增成果，比不对账更糟。
> 反之，为把"未修"改成"已修"而放宽匹配，是本末倒置。

### 四、已验证的抽查（避免"看起来修了"）

逐条查码确认了若干**报告已过时**或**确已修复**的条目，并据此标注：
`HelpCenter`「联系客服」按钮（已有 `@click`）、`CodingPracticePage` 异步判题轮询（已有 `getJudgeResult` + `JUDGE_IN_PROGRESS`）、
`MyArticlesPage` 的 `audit_remark`（select/resultMap 已含）、`MyArticlesPage` 重提清空标签（`edit` 已有 `tagsProvided` 守卫）、
`CompanyPage` 公司筛选（已落地）。**P0 因此确认为 6/6 闭环。**

### 五、质量校验

| 项 | 结果 |
|---|---|
| CSV 结构 | ✅ **883 行**（882 + 表头）、每行 **8 列**、状态仅 4 种取值、**0 行异常** |
| 主文档 | ✅ 335 行 / 8 个章节（总览、已修、已订正、不做、未修、证据、结论） |
| 一致性 | ✅ 文档表格数字与 CSV 分布、脚本输出**三者一致**（131/10/1/740） |

## v14.00 (2026-10-01) 全端评审落地（第 38 批）：P3 机械批 · 未使用导入/变量清零（门户 ESLint 归零）

**依据**：P3 中「死代码/未使用」桶（34 条）＋**门户 ESLint 基线 38 条 warning**（两者是同一批问题：ESLint 报的正是评审记的未使用项）。

### 一、成果：门户 ESLint 从 **38 warnings** → **0 problems（exit 0）**

### 二、处理明细（共 30 处，覆盖 21 个文件）

| 类别 | 处理 | 例 |
|---|---|---|
| 未使用图标/导入（20 处） | 从具名导入中精确移除该标识符 | `Navbar` 的 `Plus/Mic`、`ResumeEditPage` 的 `Save/Download/Star/Code/XCircle`、`HomePage` 的 `Book`、`SiteFooter`/`LoginPage` 的 `useToast` … |
| `props` 变量未用（4 处） | `const props = withDefaults(defineProps<Props>(), {…})` → **去掉赋值**（默认值仍生效） | `Empty` / `LoadingSpinner` / `RelatedArticleCard` / `ArticleCard` |
| 返回值未用的副作用调用（1 处） | `const head = useHead(…)` → `useHead(…)`（**保留调用**，副作用是注册 head） | `ArticleDetailPage` |
| 解构成员未用（3 处） | 从解构中移除 | `VoiceEngineDemoPage` 的 `reset: resetHint`、`VoiceInterviewPage` 的 `refreshDevices`/`requestMicPermission` |
| 未使用变量＋随之无用的导入（4 处） | 删变量、同步删导入 | `AchievementsPage` / `CodeRunnerPage` / `StudyPlanPage` 的 `router` + `useRouter` |
| 重复定义（1 处） | 只删**未使用**的那个 | `MessageChat` 有两个 `pad`：62 行在用、82 行未用 ⇒ 只删 82 行 |
| 未使用参数（1 处） | 按 eslint 约定改前缀 `_msg`（**保留公开签名与参数位置**） | `utils/authDialog.ts#handleUnauthorized` |

### 三、★ 一次自查抓出的"批量误伤"（如实记录）

我的批量脚本按「标识符 → 从 import 中移除」工作，但在 `StudyPlanPage` 上发生了**误判**：
ESLint 报的是 **变量** `'router' is assigned a value but never used`，而我把 **导入** `useRouter` 一并删掉了，
文件里却还留着 `const router = useRouter();` ⇒ `vue-tsc` 立刻报 `TS2304: Cannot find name 'useRouter'`。

**修正**：删掉那个真正未使用的变量（与已删导入一致）。
> 教训：**"未使用"要分清是导入、变量还是参数** —— 按名字批量删容易删错层；本次由 `vue-tsc` 在提交前拦下。

### 四、校验

| 项 | 结果 |
|---|---|
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 problems（exit 0）**（此前 0 errors / **38 warnings**） |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 49.73s`） |
| 后端 `mvn -o -B test`（无 Java 改动，作回归守卫） | ✅ **471/471** |

## v13.99 (2026-10-01) 全端评审落地（第 37 批）：P2 第十波 · 文章详情广告位登记 + 支付通知详情弹窗

**依据**：P2（`ArticleDetailPage` 广告位 slot-key 与后台字典不一致；`MessagesPage` 支付通知无详情弹窗）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-14 文章详情的两个广告位无法投放** | 门户详情页实际渲染 `article_detail_sidebar` / `article_detail_bottom` 两个广告位（`<AdCard slot-key=...>`），但字典 `portal_ad_slot_key` 中**只登记了首页两个位**（`home_xulin_ad` / `home_vip_banner`）。而后台「广告管理」的**广告位下拉正是取自该字典**（`cms/ad/index.vue` → `useDict("portal_ad_slot_key")`）⇒ 运营**根本无法为这两个真实存在的位置投放广告**，接口按 slotKey 查询自然永远为空 | 新增增量脚本 `20261001-05-文章详情广告位字典登记（v13.99）.sql`（**追加式**：`INSERT ... SELECT ... WHERE NOT EXISTS`，不删除/不覆盖既有两行，幂等），并同步 `init-sql/moyun-db-dml-init.sql` |
| **P2-15 支付通知看不到详情** | 通知与公告点击后都有详情弹窗，**支付通知点击只 `markPayRead`**、没有任何详情（卡片内容还被 `line-clamp-2` 截断）⇒ 关键信息（完整内容、关联单号、类型、时间）看不到 | 新增 `showPayModal` / `selectedPayNotif` / `openPayDetail()` / `closePayDetail()`，弹窗沿用公告弹窗的结构与无障碍属性（`role="dialog"` / `aria-modal` / `aria-labelledby` / `Esc` 关闭），展示完整内容 + 关联单号 + 类型 + 时间；点击卡片即打开详情并标记已读（与通知/公告行为一致） |

### 二、校验

| 项 | 结果 |
|---|---|
| **字典增量实跑 + 幂等** | ✅ 4 行（含新增 2 行）；**二次执行仍为 4 行**（未重复插入、未覆盖既有行） |
| 后端 `mvn -o -B test` | ✅ **471/471，BUILD SUCCESS**（新增量脚本已被守卫扫描） |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 49.73s`） |

> **口径说明**：本批**没有**删除或改写既有的两个首页广告位字典行 —— 采用追加 + 判重，避免"修一处坏一处"。

## v13.98 (2026-10-01) 全端评审落地（第 36 批）：P2 第九波 · 消息中心四条列表分页 + 通知类型改服务端筛选

**依据**：P2（`MessagesPage` 通知/公告/会话/待办固定 `pageNum=1&pageSize=50`、无翻页；类型筛选只对已加载数据生效）。

### 一、缺陷

| 列表 | 缺陷 |
|---|---|
| 通知 / 待办 / 公告 / 会话 | 均固定 `pageNum: 1, pageSize: 50` 且**无翻页或"加载更多"** ⇒ **第 50 条之后的数据永远看不到**（用户以为只有 50 条）。注：同期**支付通知**早已实现分页（`payPage/payTotal/payHasMore` + 「加载更多」），说明这是遗漏而非设计 |
| 通知类型筛选 | `filteredNotifications` 只是对**已加载的 50 条**做前端过滤 ⇒ 筛选结果不完整，且与分页天然冲突 |

### 二、修复（沿用**已有的**支付通知分页写法，保持全页一致）

| 内容 | 说明 |
|---|---|
| 四条列表分页 | 各加 `xxxPage / xxxTotal / xxxHasMore`（`notif` / `todo` / `annPage` / `sess`）+ 加载器接受 `page` 参数（`page===1` 覆盖、否则追加）；**pageSize 仍为 50**（首屏行为不变，只多出"加载更多"） |
| 加载更多 UI | 按既有支付通知样式，在四个列表容器内插入「加载更多通知/待办/公告/会话」按钮（含 loading 态） |
| 通知筛选 | 改为**服务端筛选**（`getNotificationList` 支持 `type`）：`changeNotifFilter()` 重置分页并重取；`notifFilter` 与选项数组显式标注为 `NotificationType \| ''` |
| 公告筛选 | 核查后**保留前端过滤**并在注释中说明依据：`getBroadcastList` 服务端**不支持 `type` 参数**（其入参未含该过滤能力），故不做假的服务端筛选；但**分页已补齐**，筛选可覆盖到后续加载的数据 |

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 50.08s`） |
| 后端 `mvn -o -B test`（无 Java 改动，作回归守卫） | ✅ **471/471** |

> **过程记录（三次类型修正）**：① `notifFilter` 原为 `ref<string>`，与 API 的 `NotificationType` 不兼容 → 改为 `NotificationType | ''`；
> ② 我把 `NotificationType` 从 `@/types/api` 导入，实际它声明在 `@/types/index.ts` → 改对导入源；
> ③ 选项数组字面量把 `value` 推断为 `string` → 显式标注数组类型。
> 三次都由 `vue-tsc` 在提交前拦下 —— 也说明**类型收紧能把"能跑但契约不对"的地方暴露出来**。

## v13.97 (2026-10-01) 全端评审落地（第 35 批）：P2 第八波 · 驳回原因取不到 / 摘要截断切标签 / 点赞静默失败

**依据**：P2"逐页独立缺陷"桶（续 v13.96）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-11 驳回原因永远看不到** | 审核链路统一写入 **`audit_remark`**（`CmsArticleServiceImpl`：「审核意见统一写入 audit_remark，独立字段，**不再复用通用 remark**」；`AuditContentAdapter:305/308` 亦然）。但详情 `ArticleVO` **只暴露了通用 `remark`**，页面又读 `article.remark` ⇒ 被驳回的文章**看不到任何驳回原因**（用户不知道要改什么） | `ArticleVO` 补 `auditRemark`（实体本就有该字段，`ArticleConvertUtil` 用 `BeanUtils` **自动映射**，无需改转换代码）；页面改读 `article.auditRemark`，并**兼容旧数据回退 `remark`** |
| **P2-12 摘要把 HTML 标签切成两半** | `PublishPage` 发布时若摘要为空，兜底用 `content.substring(0, 200) + '...'`。**富文本模式下 `content` 是 HTML** ⇒ 按字符截断会切在标签中间，并把标签源码当摘要展示 | 复用项目**早已存在**的 `extractExcerpt(content, editorMode)`（按编辑器模式剥离标签/语法，本文件"自动生成摘要"也在用它），在构造载荷前先算出 `finalExcerpt` |
| **P2-13 点赞/收藏失败毫无反馈** | store 的 `likeArticleWithApi` / `bookmarkArticleWithApi` **不抛异常**，而是返回 `{ success, message }`；详情页 `await ...` 后**直接丢弃返回值** ⇒ 失败（限流/禁言/网络异常）时界面无任何变化，用户以为"点了没反应" | 两处均检查 `success`，失败时 `toast.error(res.message || '…失败，请稍后重试')` |

### 二、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **471/471，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 52.53s`） |

> **过程记录**：`ArticleDetailPage` 的模板锚点缩进我写错了（12 空格 vs 实际 16 空格），批量脚本报"匹配 0 次"未写入；
> 重新读取该段后用精确锚点补上。批量编辑的"未命中即中止该文件"策略保证了**不会留下半成品**。

## v13.96 (2026-10-01) 全端评审落地（第 34 批）：P2 第七波 · 首页分区静默失败可见化

**依据**：P2"静默失败无 error 态"（清单：`HomePage` 8 个分区只 `console.error`，无任何用户可见提示）。

### 一、缺陷

首页 8 个独立分区（分类 / 热门标签 / 入驻名家 / 读书空间 / 面试空间 / 简历模板数 / 刷题排行榜 / 社区动态）
各自 `try/catch` 后**只打 `console.error` 并把数组置空**。接口挂了用户看到的是**空白区块**——
既分不清"本来就没数据"还是"加载失败"，也没有任何重试入口。只有主数据（`mainDataError`）有可见错误态。

### 二、修复（可操作、且只重试失败项）

| 内容 | 说明 |
|---|---|
| 失败清单 | 新增 `failedSections` + `markSectionFailed()` / `markSectionOk()`；各分区 loader 在成功/失败时打标（7 个分区，**简历模板数只影响 hero 数字、无独立区块，故不打标**） |
| 提示条 | 页面顶部（`<template v-else>` 内）新增 `role="status" aria-live="polite"` 提示条：「部分内容加载失败（分类、入驻名家），其余内容不受影响」+「重试失败项」按钮 |
| 重试 | `retryFailedSections()` 通过**分区 → loader 注册表**只重跑失败的分区（不整页重拉，避免把已成功的内容再拉一遍）；注册表集中维护并注明"新增分区需同步登记" |

**设计取舍**：没有为 8 个分区各写一套内联错误块（约 7 倍改动面、且样式易走样），
而是**一条汇总提示 + 按分区重试**：用户能明确知道"哪几块失败了""可以重试"，改动面也可控。

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings，与基线一致） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 52.84s`） |
| 后端 `mvn -o -B test`（无 Java 改动，作回归守卫） | ✅ **471/471** |

> **过程记录（自纠）**：首轮 apply 脚本里我漏写了每个 edit 的 `file` 字段，脚本在 `path.join` 处崩溃（未产生任何半成品写入）；
> 补齐 `file: HOME` 后 9 条编辑全部一次命中。另：提示条尚未加时 eslint 警告数 38→39（`retryFailedSections` 未使用），加完 UI 即回到 38 —— 说明"先加函数后加 UI"的中间态会被 lint 如实抓出来。

### 四、本批**判定不做**的一项（并说明理由）

清单另列 `UserPage`「各 Tab 加载失败无 error 态与重试」。核查后**本批不改**：
该页各 Tab 的 loader 是**有意的优雅降级**（`console.warn('…使用默认值')`，展示默认值而非空白），
与外层 `loadTabData` 的"失败允许下次重试"（`tabLoaded[tabId] = false`）配合，用户并不会看到"坏掉的空白区"。
其严重度明显低于首页"整块空白"，且要加错误态需先决定"哪些是主数据、哪些可降级"，
属于**需要产品口径**的改动 —— 故列入"待定"，而不是为凑数硬改。

## v13.95 (2026-10-01) 全端评审落地（第 33 批）：P2 第六波 · 轮询"无限等待+静默吞错"治理

**依据**：P2"静默失败/无终态"类缺陷（清单：`ResumeOptimizePage` 轮询无最大失败次数、无总超时、无"任务超时"终态）。

### 一、缺陷（两条轮询链路各有一处）

| 链路 | 缺陷 |
|---|---|
| `ResumeOptimizePage.pollOptimizeTaskStatus`（深度优化，**页面自建 setInterval**） | ① `catch` 只 `console.warn` 后**无限重试**；② 状态接口返回非 200 时 `if (res.code !== 200 \|\| !res.data) return;` **静默跳过** ⇒ 接口持续异常同样无限轮询；③ **无总超时**。三者叠加的结果：服务端卡住或网络异常时，用户**永远停在进度动画上**，既没有结果也没有失败提示 |
| `api/aiTask.ts#pollAiTask`（岗位匹配等复用） | 单次请求失败会 reject（这点没问题），但**只在出错时才有终态**：任务若永远停在 `pending/running`，Promise 永不 settle ⇒ 同样无限轮询 |

### 二、修复

| 文件 | 内容 |
|---|---|
| `api/aiTask.ts` | `PollAiTaskOptions` 新增 **`timeoutMs`（默认 10 分钟）**；引入 `deadline` 与 `settled` 标志，用统一的 `finish()` 收口 ⇒ **成功 / 失败 / 超时三者竞争时只 settle 一次**（原先 `stop()+resolve/reject` 在超时引入后会产生重复 settle 风险） |
| `ResumeOptimizePage.vue` | 新增 `MAX_OPTIMIZE_POLL_FAILURES = 5` 与 `OPTIMIZE_POLL_TIMEOUT_MS = 10min`；每次新任务**重置**计数与截止时间；状态查询成功即**重置连续失败计数**；连续失败到上限或超时 → 统一的 `abortOptimizePolling(message)`：停止定时器、退出 loading、**清空持久化的 `asyncTaskId`**（否则刷新后又被"恢复轮询"接上、再次陷入同一无望等待）、`toast.error` 明确告知；非 200 响应也计入失败 |

### 三、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 52.44s`） |
| 后端 `mvn -o -B test`（本轮无 Java 改动，作回归守卫） | ✅ **471/471** |

> **设计要点**：终止时**必须清掉持久化的 taskId**，否则"刷新恢复轮询"会把用户重新拖回同一个无望等待——
> 这是"前端有恢复机制"时必须一并考虑的收尾动作。

## v13.94 (2026-10-01) 全端评审落地（第 32 批）：P2 第五波 · 首页数据口径 / 预览残留 / 全部已读（含新增服务端批量接口）

**依据**：P2"逐页独立缺陷"桶（续 v13.93）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-7 首页"精选好书"数字偏小** | `heroStats` 用 `readingBooks.length + readingBookLists.length` 算总数，而这两个数组在加载时已被 `.slice(0,3)` / `.slice(0,4)` **截断** ⇒ 无论库里有多少本，首页永远显示 7 本。**后端 `GET /portal/reading/home` 本来就返回真实 `bookCount` / `bookListCount`（分页 total），前端却忽略不用** | 记录并使用后端真实总数（后端未返回时才回退为已加载条数） |
| **P2-8 首页"入驻名家/热门标签"谎报精确数** | `getAuthors(10)` / `getHotTags()` 只返回列表、**不返回总数**，长度为请求上限时并非真实总数 | 引入 `countText(len, limit)`：达到请求上限显示 **"N+"**（如实表达"至少"），未达上限才显示精确值；不再用魔法数字 `>= 10` 判定 |
| **P2-9 就地预览显示上一份简历** | `ResumeOptimizePage` 的 `quickPreviewBaseResume` 只在"首次展开"时加载一次，而 `watch(selectedResumeId)` **不清理它** ⇒ 切换简历后展开预览，看到的是**上一份简历的内容** | 抽出 `loadQuickPreview()`；新增 `resetQuickPreview()` 在切换简历时作废缓存，且若预览正处于展开态则**立即按新简历重载**（不留空白） |
| **P2-10「全部已读」只读了已加载的那一页** | `markAllPayRead` 对 `payNotifs`（当前已加载页）逐条调 `markRead`，未加载的仍是未读；随后又**直接清零角标** ⇒ 清完又回来，等于对用户撒谎。后端原 `markAllAsRead` 已作为死接口移除，无批量能力 | **新增服务端批量接口** `POST /portal/pay/notifications/read-all` → `INotificationService.markAllRead(userId)`（一条 `UPDATE ... WHERE user_id=? AND read_flag=0`，仅影响未读）→ 前端改调该接口，并按返回的影响条数提示「已全部标记为已读（N 条）」 |

### 二、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **471/471，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 53.70s`） |

> **设计取舍（P2-10）**：没有选择"前端循环拉全部分页再逐条 markRead"——那会产生 N 次请求且有并发竞态；
> 改为**服务端一条 UPDATE**，语义明确、幂等，并返回影响条数（前端可如实提示，而不是无脑清零角标）。

## v13.93 (2026-10-01) 全端评审落地（第 31 批）：P2 第四波 · 三个"用户会踩到"的真实缺陷

**依据**：P2 中"逐页独立缺陷"桶。本批专挑**用户可感知的功能性错误**（非观感项）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-4 签到状态误判（时区）** | `UserPage` 用 `new Date().toISOString().slice(0,10)`（**UTC 日期**）与后端 `lastCheckinDate`（**服务器本地日期** `LocalDate`）比较。东八区本地 **00:00–08:00** 期间 UTC 日期仍是"昨天" ⇒ **今天已签到却显示未签到**（按钮可点、点了被后端以"今日已签到"拒绝） | **把判定移到服务端**：`UserStatsVO` 新增 `checkedInToday`，由 `PortalGrowthServiceImpl` 用与 `checkin()` **完全相同的判据**（`lastCheckinDate.equals(LocalDate.now())`）填充；前端直接消费该字段。旧版后端兜底也由 UTC 字符串改为 `dayjs` 本地时区 `isToday()`。<br>**根因是"客户端时区不可信"**，故不做客户端补丁而是收敛到服务端 |
| **P2-5 切话题后列表空白** | `TopicDetailPage` 组件实例被复用（`/topic/:id` 参数变化），`watch(topicId)` 只调 `loadAll()`，**不重置 `postsPage`** ⇒ 在第 3 页切到新话题时按"第 3 页"请求新话题 ⇒ 列表空白 | 切话题**重置到第 1 页**再加载；并**去掉 `watch(postsPage)` 这层隐式触发**，改为在 3 处改页码的地方（翻页 / 发表后跳末页 / 删空回退）**显式** `loadPosts()` —— 否则"重置页码"会与 `loadAll()` 重复请求，且行为不可读 |
| **P2-6 评论数不是真实总数** | `ArticleDetailPage` 的「评论 (N)」用**前端已加载**的评论+回复条数累加，完全忽略后端返回的 `total` ⇒ 首屏只加载 20 条时数字偏小、点"加载更多"后数字又跳变 | 记录并使用后端 `total`（与服务端 `pages` 分页口径一致：只统计一级评论）；后端未返回该字段时才退回本地统计 |

### 二、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **471/471，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 53.92s`） |

> **口径说明（P2-6）**：后端 `total` 只统计**一级评论**，与该接口的 `pages`/`hasMore` 同口径。
> 若把回复也计入，会出现"总数 12 但只有 1 页 8 条"的不一致，故选**与分页一致**的口径。

## v13.92 (2026-10-01) 全端评审落地（第 30 批）：P2 第三波 · 6 个字典类型补数据 + 4 页切驱动（含一个从未命中的分支）

**依据**：P2 字典化批次（续 v13.91）。

### 一、本批补数据的字典（**全部是"有类型无数据"**）

| 字典类型 | 补齐行数 | 取值来源 |
|---|---|---|
| `portal_question_type` | 0 → **5**（algorithm/bagwen/system_design/project/hr） | `QuestionDetailPage.DEFAULT_QUESTION_TYPE_MAP`（既有口径） |
| `cms_contest_status` | 0 → **4**（draft/collecting/voting/ended） | `ContestDetailPage.statusMeta` |
| `portal_book_type` | 0 → **3**（novel/longform/published） | `BookDetailPage.bookTypeText` |
| `portal_book_serial_status` | 0 → **3**（ongoing/completed/hiatus） | `BookDetailPage.serialStatusText` |
| `portal_wallet_txn_type` | 0 → **3**（tip/withdraw/**vip**） | 后端 `WithdrawOrderServiceImpl` 等实际写入值 |
| `portal_wrong_question_status` | 0 → **2**（wrong/mastered） | v13.84 已确认 reviewing 无写入点 |

新增幂等增量脚本 `20261001-04-征文题型书籍钱包错题字典数据补齐（v13.92）.sql`，并同步 `init-sql/moyun-db-dml-init.sql`。

### 二、★ 顺带修掉一个"从未命中"的分支（#11）

`WalletPage.bizTypeLabel` 的映射写的是 `member: '会员'`，而后端 VIP 记账实际写入的 `bizType` 是 **`vip`**
⇒ **"会员"这两个字从来没显示过**，用户在资金流水里一直看到原始英文 `vip`。
本批按后端真实值落字典（`vip=会员`），并把兜底映射一并改为 `vip`。

### 三、页面切换（字典驱动 + 本地兜底）

| 页面 | 变化 |
|---|---|
| `QuestionDetailPage` | **无需改代码** —— 该页 v13.x 已按字典驱动（`useDictData(['portal_question_type'])` + 本地兜底）。此前因字典**无数据**而一直走兜底分支，本批补数据后**自动生效** |
| `ContestDetailPage` | 活动状态文案改为 `cms_contest_status` 驱动（颜色保留本地色板：色值属展示样式，与字典 `list_class` 的徽章类名口径不同，不强行合并） |
| `WalletPage` | 流水类型改为 `portal_wallet_txn_type` 驱动 + 兜底（并修正上述 vip） |
| `BookDetailPage` | 书籍类型 / 连载状态改为 `portal_book_type` / `portal_book_serial_status` 驱动 + 兜底 |

### 四、校验

| 项 | 结果 |
|---|---|
| **字典补齐实跑 + 幂等** | ✅ 首次 5/4/3/3/3/2；**二次执行行数不变** |
| 后端 `mvn -o -B test` | ✅ **471/471，BUILD SUCCESS**（两个新增量脚本均被 `IncrementSqlIdempotencyGuardTest` 扫描通过） |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 52.78s`） |

> **过程记录（自纠）**：首轮 apply 有三处未通过 —— ① `BookDetailPage` 漏加 `useDictData` import（连带 `i` 隐式 any 两个报错，补 import 后一并消失）；② `WalletPage` 锚点缩进与我写的差 2 空格（匹配 0 次，改锚点后成功）。两者都由 `vue-tsc`/脚本自查抓出，未流入构建产物。

## v13.91 (2026-10-01) 全端评审落地（第 29 批）：P2 第二波 · 字典化（清单 #28/#30/#31，含字典数据补齐）

**依据**：P2「写死/硬编码（字典化）」桶（45 条）中的一批。

### 一、★ 先说一个把整批性质改变的核查结果

评审清单多处写「改用已存在的字典 `cms_report_type` / `cms_handle_status` / `cms_contest_status` /
`portal_book_type` / `portal_question_type` / `portal_wallet_txn_type`」。**实查数据库后发现**：

| 字典类型 | 类型行 | **数据行** |
|---|---|---|
| `portal_question_difficulty` | ✅ 有 | **3**（此前已由 `20260929-01` 补齐） |
| `cms_report_type` / `cms_handle_status` / `cms_contest_status` | ✅ 有 | **0** |
| `portal_book_type` / `portal_question_type` / `portal_wallet_txn_type` / `cms_article_status` | ✅ 有 | **0** |

即：**这些字典"存在"只是指 `sys_dict_type` 有类型行，`sys_dict_data` 一行数据都没有**。
若照建议直接把前端改成字典驱动，页面会**变成空白下拉**——是倒退而不是修复。
（这也解释了为什么 `ReportFeedback.vue` 虽写了字典驱动，实际**一直走本地兜底分支**。）

**因此本批的正确做法是"两件事一起做"：先补字典数据，再切字典驱动。**

### 二、本批落地

| # | 内容 |
|---|---|
| **字典数据补齐** | 新增 `increment-sql/20261001-03-举报类型与处理状态字典数据补齐（v13.91）.sql`：`DELETE + INSERT` 全量补齐 `cms_report_type`（5 行）/ `cms_handle_status`（4 行），**幂等**；同步写入 `init-sql/moyun-db-dml-init.sql`（全新库）。取值**沿用既有口径**（spam/inappropriate/infringement/fraud/other；pending/processing/resolved/rejected），**不新造枚举值** |
| **#30/#31 练习页难度** | `PracticeChoiceListPage` / `PracticeCodingListPage`：难度选项与徽章色板由写死改为 `useDictData('portal_question_difficulty')` 驱动（徽章样式取 `list_class` → `dictBadgeClass`），并保留**本地兜底** |
| **#28 我的举报** | `MyReportsPage`：状态与类型下拉/标签由写死改为 `useDictData(['cms_handle_status','cms_report_type'])` 驱动 + 本地兜底；`statusOptions`/`typeOptions` 变为 computed 后，脚本内的 `getStatusMeta`/`getTypeLabel` 同步补 `.value` |

**兜底策略说明**：三个页面都保留原写死口径作为**兜底**（字典未加载/为空/请求失败时退回），
与仓库既有 `ReportFeedback.vue` 的写法一致 —— 字典驱动**不应引入新的空白态风险**。

### 三、校验

| 项 | 结果 |
|---|---|
| **字典补齐实跑 + 幂等** | ✅ 首次 `cms_report_type=5`、`cms_handle_status=4`；**二次执行行数不变** |
| 前缀放行核查 | ✅ `PortalDictController.ALLOWED_PREFIXES = {"portal_","cms_"}` ⇒ 两个 `cms_*` 类型门户可取 |
| 后端 `mvn -o -B test` | ✅ **471/471，BUILD SUCCESS**（新增增量脚本已被 `IncrementSqlIdempotencyGuardTest` 扫描） |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 49.34s`） |

### 四、给后续波次的结论

字典化这批**不能只看"字典类型是否存在"**，必须查 `sys_dict_data` 行数；
凡是"有类型无数据"的，都要**先补数据**（并遵守增量脚本铁律：不写死现网 id、可重复执行、同步 init-sql）。
后续剩余类型（`cms_contest_status` / `portal_question_type` / `portal_book_type` / `portal_wallet_txn_type` / `cms_article_status`）
按同一套路处理。

## v13.90 (2026-10-01) 全端评审落地（第 28 批）：P2 首波 · 显示口径与误导标识（清单 #2/#33/#17/#1）

**依据**：转入 **P2（336 条）**。首批优先挑"**用户会看到错**"的项，而非纯观感项。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **P2-1（#2/#33）** | **成长进度条算错**：`UserPage` 与 `GrowthTimelinePage` 都按「每级 100 成长值」硬算（`(level-1)*100`、`growthValue % 100`），而后端阈值是**非线性**的 `0/100/300/700/1500/3000/6000/10000/20000` ⇒ 进度条百分比与"还差多少"**都是错的** | 后端补齐**权威口径**：`UserGrowthVO` 新增 `levelBaseGrowth`（本级起点）与 `levelProgress`（0..100，服务端按阈值区间计算），并在满级时进度=100、`nextLevelGrowth=null`；两个页面改为**直接消费**后端字段，删除各自的假算 |
| **P2-2（#17）** | 首页「立即开始面试」挂死 **FREE** 徽标，而语音面试后端是 `@VipOnly(benefit="interview_unlimited")`：free 档仅有限次数、用完 402 引导开通 ⇒ 「FREE」让人误以为**不限次** | 徽标改为**接口驱动**：复用既有 `getVipStatus()` + `benefitLeft()`（额度来自 `vip_tier_benefit` 表，不写死数字）⇒ 会员显示「会员不限次」/ 免费显示「免费剩余 N 次」/ 用完显示「需开通会员」/ **状态未知则不显示徽标**（宁可不显示也不误导） |
| **P2-3（#1）** | `VoiceInterviewPage` 把岗位写死为 `'Java 后端开发'`，导致 `loadJobTemplates()` 里 `if (!config.value.position)` **恒为 false** ⇒ 后台配置的**首个岗位模板回填分支永不执行**，且写死值未必对得上任何模板名 | 初始化改为**留空**，由首个模板回填（`handleStart()` 对空岗位本就有明确校验与提示，留空安全） |

> **一处细节**：`levelProgress` 用**整数向下取整**而非四舍五入 —— 本级内最大值为 `nextThreshold-1`，四舍五入会在"还差 1 点"时显示 100%，与实际语义不符；向下取整保证 100% 只在真正达到升级阈值时出现。

### 二、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **471/471，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings，与基线一致） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 49.68s`） |

### 三、P2 分类结论（用于后续波次排期）

对 336 条 P2 做**问题类型聚类**后：写死/硬编码（含字典化）**45** · 前端交互与提示缺失 **22** ·
后端缺字段/接口不可用 **15** · 死代码/未使用 **13** · 空值与异常处理 **13** · SQL/索引/性能 **12** ·
安全/权限 **7** · 类型契约不一致 **5** · 缺失后台菜单 **4** · 其它**（逐页独立缺陷）200**。

> **结论**：P2 里"可批量机械处理"的只有约 60–80 条（写死类 + 死代码类），其余 200 条是**逐页独立缺陷**，
> 每条都需单独读码定位。这决定了后续波次以"**按页打包、逐页精修**"为主，无法靠一条规则批量清空。

## v13.89 (2026-10-01) 全端评审落地（第 27 批）：发布页「高级选项」补齐 SEO / 移除假开关（清单 #4）

**依据**：清单 #4（PublishPage 高级选项采集后**不提交**、后端无对应字段）。

### 一、原缺陷（两端都缺）

发布页「高级选项」采集 7 项设置，`saveDraft`/`handlePublish` **一项都不发**，后端 `portal_article` 也**没有对应列**：
SEO 标题/描述/关键词、允许评论、评论需审核、可见性（公开/仅自己/密码）、访问密码。

**严重性分层**（这是本批做取舍的依据）：
- SEO 三项：填了等于没填 —— **无落地、但无害**；
- 评论/可见性四项：**假隐私开关** —— 用户选"仅自己可见/密码保护"，文章实际仍**完全公开**
  （详情、列表、检索、Feed 全都没有按可见性过滤的逻辑）。"以为已私密"比"没有这功能"危险得多。

### 二、本批处理（分而治之）

| 项 | 处理 | 理由 |
|---|---|---|
| **SEO 标题/描述/关键词** | ✅ **端到端补齐**：`portal_article` 加 3 列（DDL + 增量脚本）→ 实体 → `ArticlePublishDTO` → 发布映射 → `PortalArticleMapper.xml`（resultMap/查询列/insert 列/insert 值/update）→ `ArticleVO`（`BeanUtils` 自动回显）→ 前端类型与两处载荷 | 加列即可生效，无鉴权语义，风险低、价值实 |
| **允许评论 / 评论需审核** | ⛔ **移除 UI**，改为说明文案 | 后端无列、评论侧无读取点，开关写了也不生效 |
| **可见性 / 访问密码** | ⛔ **移除 UI**，改为说明文案 | 需要详情+列表+检索+Feed 全链路鉴权才能兑现；**先移除假承诺**，待全链路落地再放出 |

### 三、四同步落地明细

| 层 | 文件 |
|---|---|
| 全新库 DDL | `init-sql/moyun-db-ddl.sql`：`seo_title varchar(200)` / `seo_description varchar(500)` / `seo_keywords varchar(300)`（紧邻 `preview_length`） |
| 存量库增量 | **新增** `increment-sql/20261001-02-portal_article-SEO三列（v13.89）.sql`：**每列 information_schema 前置判断**（满足 `IncrementSqlIdempotencyGuardTest`）、不写死任何现网 id |
| 实体/DTO/VO | `PortalArticle`、`ArticlePublishDTO`、`ArticleVO` 各 +3 字段 |
| Mapper（SQL 双轨·XML 轨） | resultMap、`selectPortalArticleVo` 列、insert 列、insert 值、update set 五处 |
| 前端 | `types/api.ts`（`CreateArticleParams` + `Article`）、`PublishPage.vue` 两处载荷 + 移除两组假开关 UI |

### 四、验证（含真实库往返）

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **471/471，BUILD SUCCESS**（Mapper XML 由上下文启动一并校验；DDL 由 `DdlConventionGuardTest` 校验） |
| **增量脚本实跑 + 幂等** | ✅ 首次 `seo_columns=3`；**二次执行全部 skip**（"已存在"）⇒ 可重复执行 |
| **真实库列往返** | ✅ 事务内插入 `T/D/k1,k2` → 查询原样返回 → `ROLLBACK` 后 `remaining_after_rollback=0`（零持久改动） |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings，与基线一致） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 49.67s`） |

### 五、顺带清理

移除两组 UI 后，`allowComments`/`commentModeration`/`visibility`/`articlePassword`/`showCommentSettings`/`showPermissionSettings`
与图标 `Globe`/`Lock` 共 **8 个标识符成为死代码**（eslint 警告数 38 → 46 暴露出来）——已一并删除，警告数回到基线 38。

> **过程说明**：`vue-tsc` 先报 `'seoTitle' does not exist in type 'Partial<Article>'` —— 草稿接口签名用的是 `Partial<Article>`
> 而非 `CreateArticleParams`，说明**发布与草稿两条载荷路径的类型不同源**；本批按实际签名两处都补。

## v13.88 (2026-10-01) 全端评审落地（第 26 批）：mock 回调 HMAC 验签 + 菜单双轨规则守卫（清单 #12、#13）

**依据**：清单 #12（mock 验签无密钥）、#13（菜单双轨一致性）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **C-43** | mock 渠道"验签"退化为 **`sha256(body)` 且无任何密钥** ⇒ 任何能构造 body 的人都能伪造支付成功回调（回调端点 `/portal/pay/callback/wechat` 可直达），把订单置为已支付 | ① `PayProperties.Wechat` 新增 `mockSignatureSecret`；② mock 验签改为 **`hex(HMAC-SHA256(secret, body))`**，与请求头 `X-Mock-Signature` 用 `MessageDigest.isEqual` **恒时比较**；③ **密钥留空即 fail-closed 拒绝**并打印可操作的配置指引（不再静默接受）；④ `application-dev.yaml` 提供 dev 默认值 `moyun-dev-mock-pay-secret`（可用 `PAY_MOCK_SIGNATURE_SECRET` 覆盖），同值已加入 `ConfigWiringValidator` 开发占位凭据黑名单 |
| **C-44** | **菜单双轨**：6 个增量菜单脚本写死现网 id（`parent_id=5068`、`menu_id=5152/111-114/397-401`…），而 `moyun-menu-redo.sql` 为全新库重编号 1..406 | ① **逐条核对**：这些脚本涉及的菜单在 redo 脚本中**均有等价行**（竞赛 / `feedback-center` / `help-center` / `server-panel` / `auditTask` / 简历解析配置 397-401 + 4 个 F 行）—— 全新库路径实为**完整**；② 新增**守卫测试** `IncrementSqlIdempotencyGuardTest#newMenuScriptsMustNotHardcodeMenuIds`：新增菜单脚本命中 4 类写死形态即失败，并附**白名单自检**（防豁免腐烂）；③ 新增 `increment-sql/README.md` 把规则写成明文铁律 + 正确写法示例 |

### 二、★ 守卫的**自证**（本批最重要的过程证据）

新守卫**第一版漏判**：我用"负例脚本"验证时，写的是
`INSERT INTO sys_menu (menu_name, parent_id, ...) SELECT '负例', 5068, ...` —— 而规则只匹配 `parent_id = 5068`
这种**赋值式**，于是**负例没被拦下**（假阴性）。这正是历史脚本的真实形态，说明规则太窄。

修正为覆盖 4 类形态，并**逐个负例实测**：

| 负例形态 | 期望 | 实测 |
|---|---|---|
| `INSERT INTO sys_menu (menu_id, ...) SELECT 397,'x',26 ...` | 拦下 | ✅ BUILD FAILURE |
| `... (menu_name, parent_id, ...) SELECT 'x', 5068, 99 ...` | 拦下 | ✅ BUILD FAILURE |
| 目录内无违规脚本 | 通过 | ✅ 471/471 |

规则含**前后否定环视**排除带引号的日期（`'2026-10-01'`）与负数，避免把合法字面量误判。

### 三、一处自伤与修复（如实记录）

给 `application-dev.yaml` 插入密钥时，我的替换脚本用了**4 空格**锚点，而实际行是 **6 空格**（子键），
结果匹配命中行内子串、插入的行**少了 2 个空格** ⇒ YAML 层级错乱 ⇒ `@SpringBootTest` 上下文启动失败
（**30 errors**）。已修正缩进，套件恢复 **471/471**。

> **教训（已固化到我的批处理脚本习惯）**：跨行/带缩进的锚点必须包含**行首与完整缩进**，
> 不能只写"看起来一样"的前缀——本次是测试套件（而非人工目视）把它抓出来的。

### 四、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **471/471，BUILD SUCCESS**（469 + 本批 2 条守卫） |
| 新守卫负例实测 | ✅ 两种写死形态均被拦下 |
| YAML 修复后上下文启动 | ✅ 30 errors → 0 |

## v13.87 (2026-10-01) 全端评审落地（第 25 批）：代码运行维护态 / 评分报告入口 / 隐私政策页（清单 #1、#8、#11）

**依据**：清单 #1（CodeRunnerPage 主体功能 100% 不可用却看似可用）、#8（评分报告弹窗不可达）、#11（站内不存在隐私政策页却被引用）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **C-40** | 在线代码运行**整页不可用**：`POST /portal/code/run` 因 RCE 风险被**无条件**返回 503（安全上的有意决定），而前端只在该次点击后弹一个 toast，页面主体、按钮、超时说明都像正常功能 ⇒ 用户以为是自己代码的问题 | ① 新增配置 `moyun.code-run.enabled`（**默认 false**，与原先硬编码 503 行为一致）+ `CodeRunProperties`；② `/run` 改为**按配置 gate**，并新增 `GET /portal/code/config` 下发开关；③ 前端置灰「运行代码」按钮、常驻"功能维护中 · 沙箱升级期间暂停执行，历史记录仍可查看"、`handleRun` 前置拦截。<br>**配置驱动**：沙箱就绪后把开关置 true，后端与前端同时恢复，无需改代码 |
| **C-41** | 评分报告存档弹窗**永不可达**：`ScoreReportDialog`、`loadScoreReports`、`refreshScoreReports` 都已实现，但 `scoreReportVisible` **从未被置 true**（全文件仅声明与绑定各一处） | 新增「历史评分报告」按钮（工具栏内），并补 `openScoreReportDialog()`：先按当前简历 `loadScoreReports` 再置 `visible=true`；未保存时给出提示而非无响应 |
| **C-42** | **站内不存在隐私政策页**：页脚「隐私政策」与注册页勾选区的「隐私政策」都指向 `/agreement`（用户服务协议），而协议正文 5.3 又写"详情请参阅我们的《隐私政策》"——引用了一份不存在的文档 | ① 新建 `PrivacyPolicy.vue`（7 章：收集范围/使用目的/保护措施/共享边界/你的权利/Cookie/更新与联系，与站内实际做法对齐——BCrypt 密码、AES-GCM 卡号、公开接口不下发联系方式、注销前需清零资金等）；② 注册 `/privacy` 路由；③ 页脚与注册页链接改指 `/privacy`；④ 协议 5.3 的《隐私政策》变为**可点链接** |

### 二、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 49.56s`） |

> **口径说明（C-40）**：本次**没有**重新启用代码执行——503 是"沙箱未就绪时的正确取舍"，本批只把"安全策略"如实呈现在界面上，并把是否放行收敛到一个配置项。

## v13.86 (2026-10-01) 全端评审落地（第 24 批）：通知偏好生效 + 付费阅读入口诚实化（清单 #36/#13）

**依据**：清单 #36（通知偏好从未被读取）、#13（付费解锁按钮必然失败）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **C-38** | 通知偏好**形同虚设**：`portal_user` 的 `notify_like/notify_comment/notify_follow/notify_system` 在**全后端没有任何读取点**（grep 命中 0 处），设置页写了个寂寞 | 在**门户个人通知的唯一落库入口** `SysNotificationServiceImpl.insertNotification` 收口：`scope=user && userType=portal` 时按 `type` 映射偏好（`like→notifyLike`、`comment/reply→notifyComment`、`follow→notifyFollow`、其余→`notifySystem`），关闭则**不写库**并留 `info` 日志。**null 视为开启**（历史用户字段可空，缺省保持原有行为，避免静默丢通知）。<br>按 finding "在通知落库前统一校验" 实现，覆盖全部 5 个门户调用点（文章互动/收藏/评论/认证/话题），无需逐点改 |
| **C-39** | 付费阅读「解锁全文」是**虚假承诺**：后端 `PortalTipServiceImpl` 对 `target_type=article_paid` 直接抛"付费阅读功能正在接入支付通道，暂不可用"（占位逻辑被注释在 throw 之后），而前端让用户走完确认弹窗才报错 | ① `PayProperties` 新增 **`articlePaidEnabled`（默认 false）**；② 详情接口在 `ArticleVO` 回填 `paidPurchaseEnabled`；③ 前端按钮据该开关**置灰 + 说明"付费阅读功能即将开放"**，`handlePurchase` 再前置拦截一次（双保险）。**配置驱动而非前端写死**：支付通道接入后只改配置即恢复可点，无需改前端 |

### 二、★ 又一次"不新增跨模块耦合"的处理（C-38）

C-38 初版在 `system` 的 `SysNotificationServiceImpl` 里 `import com.moyun.portal.domain.entity.PortalUser`，这会把
`ModuleDependencyGuardTest` 冻结的 **`system -> portal` 计数由 6 推到 7**（该计数注明"待各自建端口"）。

处理后**未上调计数**：改用 `var` 承接查询结果（本类**早已** `import com.moyun.portal.mapper.PortalUserMapper`），
既不加新的 import、也不引入 FQN 硬编码，实测 **469/469 通过**、冻结计数保持 6。
> 这是本会话第三次与模块依赖守卫的正面交互（v13.77 改实现、v13.80 判定"守卫冻结的是缺陷"、本批避免新增）。

### 三、顺带发现（已登记，未在本批处理）

- `com.moyun.portal.domain.vo.ArticleDetailVO` 是**死类**（全仓仅自身声明，详情接口实际返回 `ArticleVO`）。
  C-39 初版误把字段加在它上面，核对后已撤回并改加到 `ArticleVO`；该类本身作为死代码列入后续清理项。

### 四、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS**（模块依赖守卫保持 6） |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 52.43s`） |

## v13.85 (2026-10-01) 全端评审落地（第 23 批）：专栏主动送审入口（清单 #15）+ 一处**建议撤回**

**依据**：清单 #15（新建专栏被强制 draft，门户侧无任何送审入口）。

### 一、缺陷

门户 `saveColumn` 出于安全考虑**强制 `draft`**（防前端直传 `published` 绕过审核），只有"命中敏感词"才会被动转 `pending` 并提交审核任务。**正常内容的专栏因此永远停在 `draft`**：他人不可见、后台审核中心也没有待办 —— 用户"建了专栏却什么也没发生"，无任何补救路径。

### 二、修复（新增主动送审）

| 层 | 内容 |
|---|---|
| 服务接口 | `IColumnService.submitForAudit(id, userId)` |
| 服务实现 | 作者归属校验 → **状态机收敛**（`pending` 拒绝重复送审以免审核中心重复待办；`published` 拒绝；仅允许 `draft`/`rejected`）→ 置 `pending` → 复用既有 `submitColumnAuditTask` 建审核任务 → 返回最新详情；整方法 `@Transactional(rollbackFor = Exception.class)`（与既有的"双写一致"约定一致） |
| Controller | `PUT /portal/column/{id}/submit`（`@Log(title="门户专栏", businessType=UPDATE)`） |
| 前端 | `api/column.ts` 增 `submitColumnForAudit`；`MyColumnsPage` 卡片新增**审核状态徽标**（草稿（未送审）/审核中/已发布/已驳回）与「提交审核」按钮（仅 `draft`/`rejected` 显示），成功后本地状态置 `pending` 并提示"通过后将在专栏广场公开展示" |

### 三、★ 撤回一条原建议（诚实披露）

清单 #15 的处理建议中含「**后台把专栏管理菜单 `visible` 由 1 改为 0**」。核对后**不予采纳，建议撤回**：

- `moyun-admin-vue/src/views/cms/article/index.vue` 顶部注释明确写着「文章管理与专栏管理**合并为同一菜单下的 Tab**……原专栏管理菜单**按项目铁律改为隐藏**，路由仍保留，用户从文章管理页的 Tab 进入专栏管理」；
- 该 Tab 复用的正是 `cms/column/index.vue`，而该页**已有**「审核」按钮（`v-if="status === 'pending'"`，权限 `system:auditTask:list`）。

即：菜单隐藏是 **v8.1 的有意设计**（避免同功能双入口），审核入口**并未缺失**；原建议若执行反而会造成同一功能两个菜单入口。真正缺失的只是**门户侧送审**这一步，本批已补。

### 四、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 52.40s`） |

## v13.84 (2026-10-01) 全端评审落地（第 22 批）：错题状态机与选择题判分（清单 #31/#30）

**依据**：清单 #31（「复习中」状态无写入点）、#30（历史选择题数据判分）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **C-36** | **「今日待复习」恒为空**：`listTodayReview` 设 `status='reviewing'` 后走分页列表，而全仓**没有任何写入点**会产生该状态（只有 `wrong → mastered`）⇒ 该接口永远返回空；前端还多了一个恒空的「复习中」页签与恒 0 的统计卡 | ① `WrongQuestionQuery` 新增 `reviewOnly`，Mapper 按**时间口径**过滤（`status != 'mastered' AND next_review_time <= NOW()`，与既有 `countTodayReview` 的 SQL **完全同判据**）；② `listTodayReview` 改设 `reviewOnly=true`；③ 前端**移除死状态**：页签、类型联合、文案/颜色映射、恒 0 统计卡一并删除，统计卡栅格 4 列 → 3 列 |
| **C-37** | 历史选择题数据存在"**`correct_answer` 为空、仅 `options` JSON 内 `is_correct:true` 标答案**"的形态（后台编辑页专门写了兼容分支）；此时 `isChoiceAnswerCorrect` 因正确答案为空**直接判错** ⇒ **用户选对也判错**，且服务端不留任何痕迹 | 判分前若 `correct_answer` 为空，则用既有 `OBJECT_MAPPER` 从 `options` 的 `is_correct` **反推**答案（多选按 label 升序拼接，与判分侧归一化口径一致）再比对；反推成功记 `info`、彻底缺失记 `warn`（便于数据治理定位），**不再静默判错** |

> **口径说明**：C-36 采用"时间口径"而非"新造一个 reviewing 状态"——错题本已有 `next_review_time` 字段且 `countTodayReview` 早就在用它，让**列表与计数同判据**比引入一个永不被写入的状态更小、更一致。

### 二、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 52.46s`） |

## v13.83 (2026-10-01) 全端评审落地（第 21 批）：后台银行卡人工核实入口（清单 #52，**四层贯通**）

**依据**：清单 #52 —— 后台「收入管理 → 用户银行卡」只有 `list`/`detail` 两个**只读**接口，页面也只有核验状态筛选与展示，**没有任何把 PENDING 置为终态的入口**。后果：四要素实名通道无法自动判定的卡永远卡在"审核中"，用户绑卡后**提现路径实际不可用**（且 v13.71 A5 刚把 certNo 补上，正是为了让这张卡有机会被核验）。

### 一、四层改动（代码 + SQL + 菜单 + 前端）

| 层 | 内容 |
|---|---|
| **服务接口** | `IBankCardService.verifyByAdmin(cardId, verifyStatus)`：**状态白名单**只接受 `VERIFIED`/`REJECTED`（不允许把卡"核实"回 PENDING，也不允许写入任意字符串）；卡不存在则报错；成功写操作日志（含变更前后状态与操作者） |
| **Controller** | `POST /cms/pay/bank-card/{cardId}/verify`：`@PreAuthorize("@ss.hasPermi('cms:payBankCard:verify')")` + `@Log(title="用户银行卡", businessType=UPDATE)`；异常统一走 `ServiceException` 以保证提示能透给操作者（沿用 v13.76 的口径） |
| **菜单/权限（四同步）** | ① `moyun-menu-redo.sql`（全新库）新增 F 行「银行卡人工核实」`cms:payBankCard:verify`（挂在菜单 115 下，与既有 `:query` 同风格）；② 新增增量脚本 `increment-sql/20261001-01-银行卡人工核实权限（v13.83）.sql`，**父菜单按 `perms='cms:payBankCard:list'` 反查、不引用任何现网 id**（这正是清单 #62「菜单双轨」教训的直接应用），并给已具该权限的角色补齐授权 |
| **后台前端** | `cms/pay/bankcard/index.vue` 新增「操作」列：仅 `PENDING` 显示「人工核实」（终态卡不重复改写资金相关状态），弹窗可选"人工确认通过/判定不通过"并提示先核对姓名/卡号/预留手机号；按钮带 `v-hasPermi="['cms:payBankCard:verify']"` |

### 二、SQL 的验证方式（重要）

增量脚本**没有靠"看"来验证**：把它包在 `START TRANSACTION; … ROLLBACK;` 中在本地 `moyun-db` 实跑，实测：

```
verify_perm_rows = 1     ← 权限行按 perms 反查父菜单插入成功
role_menu_rows   = 1     ← 角色授权补齐成功
```

随后确认 **`SELECT COUNT(*) … perms='cms:payBankCard:verify'` 仍为 0** ⇒ 事务已回滚，**未对开发库产生任何持久改动**。
另核对 `moyun-menu-redo.sql` 新增行的**字段数 20**，与同区段兄弟行（`银行卡详情` / `费率调整`）完全一致，且与该 INSERT 的列清单一致。

### 三、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 增量 SQL（事务内实跑 + 回滚） | ✅ `verify_perm_rows=1`、`role_menu_rows=1`，回滚后归零 |
| 菜单 redo 行字段数 | ✅ 20，与兄弟行一致 |
| 后台 `vite build` | ✅ exit 0（`✓ built in 49.82s`） |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS** |

## v13.82 (2026-10-01) 全端评审落地（第 20 批）：VIP 章节付费墙（清单 #55/#56）

**依据**：清单 #55（ChapterReaderPage VIP 章节预览）、#56（BookDetailPage 章节列表 VIP 门禁）。两条**同一根因**，本批一并修复。

### 一、缺陷（实测）

`GET /portal/reading/chapters/{chapterId}` 是 `@Anonymous`，且**不做任何访问级别校验**：只看"章节是否已发布"就返回**完整正文**。于是：

| 事实 | 说明 |
|---|---|
| 数据库**有**门禁字段 | `portal_book.access_level`（`free,vip,preview`）+ `portal_book_chapter.is_free` / `price` |
| 前端**以为**有门禁 | `ChapterReaderPage` 在 `chapter.isFree === false` 时展示"本章为 VIP 章节当前为预览模式，完整内容需开通 VIP" |
| 后端**实际没有** | 正文照发全文 ⇒ **横幅与内容自相矛盾**，未登录/非会员可直接读完付费章节（内容与营收双损失） |

### 二、修复

**后端（服务端强制，fail-closed）**

- 章节详情增加访问级别判定：**章节 `is_free=false` 即受限**；`is_free` 未设置（null）时**继承书籍** `access_level=vip`；显式的 `is_free=true` 优先（单章免费是有意为之）。
- 受限时以**会员卡**为准判定（`IVipService.isVip(userId, "portal")`，而非登录态里的静态角色）：
  - 会员 → 全文；
  - 非会员/未登录 → 正文裁剪为**试读片段**（前 500 字）并置 `preview=true`。
- 裁剪策略：markdown 直接截断并追加"试读结束"引用块；**富文本先剥标签再截断**（直接截断 HTML 会把标签截成半截、破坏页面结构），试读文本经 `EscapeUtil.escape` 转义后包 `<p>` 返回。
- `PortalBookChapter` 新增 `@TableField(exist = false) private Boolean preview`（非持久化），保持响应结构稳定（只增字段，不改形状）。

**前端**

- `BookChapter` 类型补 `preview?: boolean`；
- 横幅判据由 `chapter.isFree === false` 改为 **`chapter.preview === true`**（服务端下发的权威信号，避免"客户端猜"），并补「开通 VIP」入口（跳 `/membership`）、订正原先语序不通的文案。

### 三、依赖方向

新增 `portal → vip.service` 依赖。核对：`ModuleDependencyGuardTest` 的 `FROZEN_EDGES` 中**无 `portal -> vip`**，硬零规则也只约束 `core/util → 业务`；且 portal 侧本已依赖 vip（`PortalVipController`、`PortalResumeOptimizeController`/`PortalVoiceInterviewController` 的 `@VipOnly`）。实测 **469/469 通过**。

### 四、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 49.79s`） |

> **遗留（如实记录，非本次范围）**：章节**试读字数**目前是常量 500；若要按书籍/章节配置，需在 `portal_book` 增列（属 P2「字典/表驱动」类，已登记）。

## v13.81 (2026-10-01) 全端评审落地（第 19 批）：波次 C 资金/动态/日历（3 条 P1）

**依据**：清单 #37（注销不校验资金）、#16（FeedPage 目标类型映射缺失）、#35（学习日历年份错配）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **C-33** | **注销账号不校验资金**：`deactivate` 只做 `del_flag=2 / status=1` 软删 ⇒ 账户还有余额或在途提现（冻结）时，用户一点就把钱"注销没了"，**既无提示也无申诉入口**（资损） | ① **后端 fail-closed**：注销前读 `pay_user_account`，`balance` 或 `frozenAmount` 非 0 一律阻断，并返回可操作文案（含两个金额与"先提现并等待到账"指引）；② 前端在注销确认区**展示实时可用余额**（`getAccountOverview`）并给出「前往钱包提现」链接，避免用户填完确认文案才被拒 |
| **C-34** | 动态广场**「查看详情」对两类动态整块不渲染**：`targetPath` 只映射 `article/experience/column`，而 `IFeedService#publishEvent` 实际会写 `question`（刷题）与 `topic`（发话题） | 补齐 `question → /interview/question/:id`、`topic → /topic/:id`。<br>**取值口径经逐处核对**：全仓 `publishEvent` 实参只有 `article/column/question/experience/topic` **5 类**（报告提到的 `book/booklist/quote` 并无写入点），故只补真实存在的两类，不添加永不命中的映射 |
| **C-35** | 学习日历**年份错配**：年份选择器只改请求参数，热力图却恒渲染"今天往前 365 天"，标题写"近 1 年" ⇒ 选 2024 时统计是 2024、图形是近一年，三者互相矛盾 | `StudyCalendarCard` 新增 **`year` 必填 prop**：按该年 **1/1 ~ 12/31** 构建列（首尾各自对齐周日/周六），年份外的补齐格标 `outside`（透明、不参与最大值、不打月份标签、不显示 tooltip）；统计文案由"近 1 年"改为"`{year}` 年"；页面传入 `selectedYear` |

### 二、模块依赖说明（C-33）

C-33 在 `PortalUserController` 新增了 `com.moyun.pay.service.IUserAccountService` 依赖。核对结论：

- `ModuleDependencyGuardTest` 的**硬零规则**只约束 `core/util → 业务`，`portal → pay` 不在其列；
- `FROZEN_EDGES` 中与 pay 相关的只有 `ledger -> pay(12)`、`vip -> pay(4)`，**未冻结 portal**；
- portal 侧**本就已有** `com.moyun.pay.*` 依赖（`PortalVipController`、`PortalTipServiceImpl`、`TipPayCallbackHandler` 等十余处 import）。

故本处依赖与现状一致，且**实测 469/469 通过**（模块依赖守卫未被触发）。记账口径统一为“元”，与 `IUserAccountService` 的既有约定一致。

### 三、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS**（含模块依赖守卫） |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 50.04s`） |

## v13.80 (2026-10-01) 全端评审落地（第 18 批）：波次 C 话题模块（6 条 P1）+ 结构守卫口径订正

**依据**：清单 #22（清空描述/封面无效）、#23（移除封面立刻真删文件）、#24/#27（描述无长度上限）、#25（被驳回话题编辑后状态不回退）、#26（编辑绕过敏感词扫描）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **C-28** | **编辑话题完全绕过敏感词扫描**（创建路径有扫描 + 审计日志，编辑路径**一行都没有**）——等于"先发正常内容过审、再改成违规内容"的通道，且命中不写审计 | `updateTopic` 增加与创建侧同口径的扫描：按**生效后**内容（仅改封面时也要覆盖原有标题/描述）检测，命中即写 `detectAndLog` 审计并抛 `ServiceException` 阻断 |
| **C-29** | 被驳回/待审核的话题编辑后**状态不回退、也不重建审核任务**（对比 `createTopic` 会 `auditTaskService.submit`）⇒ 用户改完了却没有任何"再次送审"的路径，状态恒为 `rejected` | 命中 `rejected`/`pending` 时把状态复位为 `pending` 并**重新提交审核任务**（标题/描述取生效后的值）；前端保存提示据返回状态改为"已提交审核，审核通过后公开展示"，避免用户误以为已发布 |
| **C-30** | **清空描述/封面无效**：提交体写 `description.value.trim() \|\| undefined`，而 `undefined` 会被 `JSON.stringify` **整个丢弃** ⇒ 用户以为删掉了，旧内容其实还在 | 改为显式传空串（后端语义为 `null`=不改 / `''`=清空，本就支持） |
| **C-31** | **移除封面立刻真删存储文件**（连 `sys_file` 记录一起删），但"保存"未必发生——用户随后取消/离开，话题记录仍指向已删除的文件 ⇒ 封面永久 404 | 改为**标记待删**：点 × 仅置 `pendingCoverDeletion` 并从 UI 移除，**保存成功后**才调 `deletePortalFile`（失败只告警，不影响保存结果） |
| **C-32** | 话题描述**无任何长度上限**：`MarkdownEditor` 只有 `modelValue/placeholder` 两个 prop，而后端列是 `varchar(500)`，超长内容会在落库时截断或报错 | ① 编辑器新增 `maxlength` prop + **字数计数器**（超限标红）+ 原生 `maxlength` 双保险 + 样式；② 两个话题页传 500 并在**提交口再拦一次**；③ 后端 `createTopic`/`updateTopic`/`createOfficialTopic` 统一走新增的 `validateDescriptionLength()`（与列宽同口径，返回可读文案而非 500） |

### 二、★ 结构守卫口径订正（跨批次，诚实记录）

本批跑全量测试时，`FrontendTemplateStructureGuardTest` **失败 2 处**——它抓的是**上一批（v13.79）C-23** 的改动：

| 断言 | 原口径 | 问题 |
|---|---|---|
| `reportTabValuesAreConsistent` | 内容 div 数 == 按钮数 **+ 1** | 注释称「对话回放」是**无按钮 tab**"由 finish 后自动切换展示" |
| `reportTabsContainerIsClosed` | `.report-tabs` 内按钮**恰为 4** | 同上 |

**核对后确认：那句"自动切换展示"是未兑现的设计意图**——页面中当时**不存在**任何把 `reportTab` 置为 `'dialog'` 的赋值（实测计数为 0），即该 tab **不可达**，正是清单 #44 所述缺陷。因此：

- 保留 C-23 的修复（补「💬 对话回放」按钮，与清单建议及页面第 2687 行"复盘区：概要→归因→**证据殿后**"的设计注释一致）；
- 按"冻结清单必须保持真实"的原则**同步收紧守卫口径**：等式由 `内容 = 按钮 + 1` 改为 **`内容 == 按钮`**（每个 tab 都必须有按钮入口，无按钮 tab 即"用户到不了的死内容"），按钮数 4 → 5；
- 两处注释一并改写，**写明变更原因**（避免下一个人读到已失效的理由）。

> 这是本会话**第二次**由项目自带守卫拦下我的改动（第一次见 v13.77 的模块依赖守卫）。两次的正确处理都不是"把数字改掉了事"，而是先判断"守卫冻结的到底是正确状态还是历史缺陷"。

### 三、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS**（含更新后的前端结构守卫） |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build` | ✅ exit 0（`✓ built in 49.91s`） |

## v13.79 (2026-10-01) 全端评审落地（第 17 批）：波次 C 可达性与静默失败（6 条 P1，纯前端）

**依据**：清单 #33（公司主页无人可达）、#44（对话回放无入口）、#5（附件源文件下载双重不可用）、#38/#39（发送验证码静默失败）、#43（登录验证码静默降级）。本批未改后端。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **C-22** | **公司主页是死页面**：全仓只注册了 `/interview/company/:id`，却**没有任何跳转**——面试首页「热门公司」卡片带 `cursor-pointer` 与 hover 位移但**无 `@click`**，题目详情的公司标签是**纯 `<span>`** | 首页公司卡片改为 `<router-link :to="/interview/company/{id}">`（获得真实链接语义：可中键打开、可键盘聚焦、利于 SEO）；题目详情「出现公司」标签同样改为 `<router-link>` 并加 `title` 说明 |
| **C-23** | 语音面试报告**「对话回放」整块不可达**：`reportTab` 联合类型含 `'dialog'`、模板也实现了完整回放，但 tab 按钮栏只有 4 个按钮、`reportTab` 永远不会等于 `'dialog'` | 在复盘区「问题分析」之后补「💬 对话回放」按钮——与第 2687 行设计注释"复盘区（概要→归因→**证据殿后**）"一致 |
| **C-24** | 附件简历「下载源文件」**双重不可用**：`window.open` 裸路径既无 `/api` 前缀（vite 只代理 `/api`、`/moyun`）也不带 `Authorization`；且后端在 `/parse/confirm` 后**不再持久化源文件**（`sourceFileUrl` 恒为 null） | ① 抽出通用 `downloadFileAuth()`（fetch blob + a 标签，带 token 与 `/api` 前缀），PDF 与附件共用；② 附件下载改走它并接错误提示；③ 按钮加 `v-if="... && r.sourceFileUrl"`，无原件时**直接不展示**（而非点了必 404） |
| **C-25** | 注册页发送邮箱验证码**静默失败**：`captchaEnabled=false` 时走"直接发送"，失败信息却只写进**不可见的** `captchaModal.error`（用户只看到 console.warn） | 按弹窗可见性分流：可见→留在弹窗刷新图形码；**不可见→`toast.error`** |
| **C-26** | 找回密码页发送验证码**同一缺陷** | 同一改法（邮件/短信两条通道共用该分支） |
| **C-27** | 登录页验证码**静默降级**：拉取失败即 `captchaEnabled=false`（不展示验证码），但登录是否校验验证码由**后端全局开关**决定 ⇒ 用户提交必被判"验证码错误/为空"且看不到原因 | ① 失败时**保持展示**并提示"验证码加载失败，请点击重试"（新增 `captchaError` + 模板内联重试按钮）；② `uuid` 缺失时**先行拦截提交**（fail-closed）；③ `getCaptchaImage` 增加**结构校验**：HTTP 非 2xx 或开关为开但缺 `uuid/img` 一律抛错，不再用 `??` 把失败伪装成"看似成功"的对象 |

### 二、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |
| 门户 `npm run build`（模板改动的主要风险面） | ✅ exit 0（`✓ built in 49.64s`） |
| 后端 | 本批**未改后端代码**，故未复跑；上一批（v13.78）为 ✅ 469/469 |

## v13.78 (2026-10-01) 全端评审落地（第 16 批）：波次 C 内容社区与学习工具（3 条 P1 + 1 处连带修复）

**依据**：清单 #14（驳回原因回显）、#28（异步判题无轮询）、#17（活动投票无门禁）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **C-19** | 我的文章「已拒绝」卡片**永远不显示拒绝原因**：页面读 `article.remark`，而后端驳回原因写在 `audit_remark`（`PortalArticleResult` 的注释明确"不再复用通用 remark"）。**根因是类型失守**——`getMyArticles` 声明为 `httpGet<any>`、页面 `ref<any[]>` ⇒ TS 拦不住任何字段名错误 | ① 新增 `MyArticlesResult` 类型并把 `getMyArticles` 改为 `httpGet<MyArticlesResult>`（去掉 `any`）；② 页面改 `ref<Article[]>`；③ `Article` 补 `auditRemark` 并把误导性注释（`remark // 审核意见`）订正；④ 页面改读 `article.auditRemark`。<br>**后端无需改动**：`audit_remark` 的 select 与 resultMap 映射经核对**已存在** |
| **C-20** | 异步判题**永远停在"判题中"**：`moyun.judge.async-enabled=true` 时 `submitJudge` 首包返回 `status=PENDING` + submissionId，需轮询 `GET /portal/judge/result/{submissionId}` 才拿得到终态，而页面直接把首包当结果显示 | 实现 `awaitJudgeResult()`：识别进行中状态（PENDING/QUEUED/JUDGING/RUNNING/COMPILING）→ 每 1.2s 轮询、最多 60 次（≈72s）；**代次（generation）机制**保证切题/重跑/卸载时在途轮询立即退出、不把过期结果写回；单次轮询失败继续重试；超时给出"请在提交记录中查看"的可操作提示。`handleRun`/`handleSubmit` 均已接入 |
| **C-21** | 活动投票**无任何门禁**：`toggleVote` 只校验登录 ⇒ `draft`（未开始）与 `ended`（已结束）的活动同样可投**也可取消票**，而详情页却在展示"投票截止"——门禁与展示口径不一致（可刷票、结束后可改结果） | ① 后端：校验活动存在 + `status == 'voting'` + `now <= voteEndTime`，任一不满足即拒绝；门禁提示**用 `ServiceException`** 以保证能透给用户（本类其余分支用裸 `RuntimeException`，message 不会到前端——见 v13.76 C-12）；② 前端：新增 `canVote` 与 `voteClosedHint`（与后端同口径），按钮禁用并就地展示关闭原因，`handleVote` 先行拦截 |

### 二、连带修复（由 C-19 的类型收紧暴露）

`Article` 类型声明的是 `updatedAt`，而后端 `BaseEntity` 实际返回 `updateTime`（resultMap 映射 `update_time → updateTime`）。

- `MyArticlesPage` 一直在读 `article.updateTime`（此前 `any[]` 掩盖）；
- **`ArticleDetailPage` 的 JSON-LD 读的是 `article.updatedAt`** ⇒ `modifiedTime` 恒为 `undefined`（**结构化数据/SEO 静默失效**）。

已把 `Article.updatedAt` 订正为 `updateTime`，并修正 `ArticleDetailPage` 两处 JSON-LD 取值。

### 三、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0（C-19 收紧类型时**先报出** `updateTime` 不存在，据此定位到 JSON-LD 缺陷） |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |

> **本批第二次印证**（首次见 v13.75）：把 `any` / 错误的字段名改成**与后端 VO 一致的类型**，`vue-tsc`
> 会立刻把人工评审漏掉的同类缺陷翻出来——本次翻出的是 SEO JSON-LD 的静默失效。

## v13.77 (2026-10-01) 全端评审落地（第 15 批）：波次 C 简历中心与题库（4 条 P1）

**依据**：清单 #10（简历模板草稿对外可见）、#7（归档简历不可达）、#1/#6（导出 PDF 下载端点不存在）、#32（公司页 companyId 被忽略）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **C-15** | 简历模板分页查询**完全忽略 `query.status`** ⇒ 面试首页（第 157 行明确设置 `status="active"`）与门户列表都会把后台**草稿（draft）**模板一并展示、可下载；详情/下载端点（`@Anonymous`）也不校验状态，知道 id 即可取草稿 | ① `selectResumeTemplatePage` 应用 status 条件；② 门户列表**强制覆写** `status="active"`（不信任客户端传参，避免 `?status=draft` 枚举草稿）；③ 门户详情端点校验状态；④ `downloadResumeTemplate` 仅放行 `active`（CMS 走 `selectResumeTemplateById`，不受影响） |
| **C-16** | 归档后的简历在页面上**完全不可见**：后端 `selectMyResumePage` 不传 status 时默认排除 `archived`，而页面既不传 status 也无筛选控件 ⇒ 模板里的「恢复」按钮（`v-if="r.status === 'archived'"`）**永不可达** | 后端**本就支持** `status=archived`（含 `ne`+`eq` 互斥的注释说明），故为**纯前端**修复：新增状态筛选（全部（不含归档）/草稿/已发布/已归档）+ 切换时归 1 页 + 按筛选传 status |
| **C-17** | 简历导出 PDF：后端返回的下载地址 `/portal/interview/resume/user/file/{id}`（**有意**做成认证端点以避免 PDF 经公开目录泄露）在控制器中**从未实现** ⇒ 前端带 token 下载必然 404 | 实现 `GET /{id:[0-9]+}/file`：要求登录、归属校验由 service 保证、读**磁盘路径**（`entity.file_url`）流式返回 `application/pdf`；与既有 `download-attachment` 同一写法与资源前缀折算逻辑 |
| **C-18** | 公司页「公司题目」传 `companyId`，但 `buildQuestionQueryWrapper` **没有该分支** ⇒ 参数被 Spring 绑定后**静默忽略**，实际返回全量题目（前端当成本公司题目展示） | 新增 `applyCompanyFilter`：经关联表取题目 id 集合后 `IN`（**不用 JOIN**，避免放大行数破坏分页 count）；该公司无关联题目时**显式恒假**，防止退化成"不过滤=全量"；分页与导出两条路径同时生效 |

### 二、★ 结构守卫拦下的一次越界（过程证据）

本轮 C-18 初版在 `ext.cms` 的 `PortalInterviewServiceImpl` 里**新增**了
`import com.moyun.portal.mapper.PortalInterviewQuestionCompanyMapper`，随即
`ModuleDependencyGuardTest.frozenEdgesMatchExactly` **失败**（`ext.cms -> portal` 期望 277、实际 278）。

按该守卫自己的规程（"计数增加 = 新增了跨模块依赖，请先判断方向是否正确，必要时改为依赖倒置"），
我**没有直接上调清单数字**，而是改为：把查询方法
`selectQuestionIdsByCompanyId(companyId)` 加到该文件**早已依赖**的 `PortalInterviewCompanyMapper`
（+ 对应 XML `select`），复用现成 `companyMapper` 字段 ⇒ **不新增任何跨模块 import**，
冻结计数保持 277 不变，套件恢复 **469/469**。

> 这一条恰好演示了"冻结清单"的价值：它不阻止修改，但**强制**每一次跨模块耦合的增加都被显式审视，
> 并优先选择"不扩大耦合"的实现方式。

### 三、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS**（含 `ModuleDependencyGuardTest`） |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |

> **SQL 双轨遵守**：C-18 的查询落在 `mapper/portal/PortalInterviewCompanyMapper.xml`（XML 轨道），
> 未使用注解内联 SQL，也未拼接 `${}`。

## v13.76 (2026-10-01) 全端评审落地（第 14 批）：波次 C 后端隐私与契约（4 条 P1）

**依据**：清单 #41（公开资料接口泄露实体全字段）、#54（业务异常提示不达用户）、#45（发表评论返回类型不符）、#65（未配置有效期的会员等级仍可购买）。

### 一、修复清单

| # | 缺陷 | 修复 |
|---|---|---|
| **C-11** | `GET /portal/user/{id}` 直接返回 **PortalUser 实体全字段**：未登录即可拉到目标用户的 `email / phone / wechat / loginIp / loginDate / maritalStatus / hasMortgage / hasSideIncome / incomeTypes` 等隐私与画像字段（Service 已清密码，其余照发） | 改为**白名单 VO** `UserProfileVO`（id/username/nickname/avatar/bio/position/company/school/location/website/github/identityTag/gender/certifiedCreator/createTime）；**联系方式一律不下发**（公开主页无消费方，最小权限）。前端补 `UserProfileVO` 类型，`AuthorPage`/`FollowListPage` 的 ref 改用该类型并把 6 处隐式转换显式化（`String(id)`），`AuthorPage` 去掉 `as any` 与不存在的 `createdAt` |
| **C-12** | `BusinessException` 的业务提示**到不了用户**：它直接继承 `RuntimeException` ⇒ 落到全局兜底分支，统一回"操作失败，请稍后重试"。**更深一层**：其 `getMessage()` 格式化的是 `errorCode/errorMessage`，而这两个字段**从未被任何构造函数赋值** ⇒ 恒返回 `"[null] null"`（即"即便透出也是乱码"） | ① 修正 `getMessage()` 返回构造入参的真实提示（仅缺失时回退父类）；② `GlobalExceptionHandler` 增加 `@ExceptionHandler(BusinessException.class)` 分支透出该 message（与 `ServiceException` 同口径，仍记完整堆栈），缺失时保守回退通用提示。已有测试 `PortalTipServiceTest` 断言的是 `getCode()`，不受影响 |
| **C-13** | 发表面经评论：后端 `insertComment` 返回 **`int`（影响行数）**，Controller `AjaxResult.success(int)`；前端 `publishComment` 声明 `InterviewCommentVO` 并把 `res.data` 当评论对象插入列表 ⇒ 新评论**渲染成空白** | 服务端改为返回**新建评论的 VO**（`toCommentVO`，插入失败返回 null）；接口签名同步。仅 2 处调用点（Controller/接口），改动面可控 |
| **C-14** | 后台"未配置有效期"（`duration_days` 为空）的 VIP 等级**仍可下单支付**，而发卡侧 `grantCard` 对 null 会抛异常且运行在**支付回调事务内** ⇒ "**钱收了、卡没发**"（渠道重试仍失败，需人工补配置） | ① 后端在 `subscribe` **下单阶段**即校验 `durationDays != null`（与发卡侧同一判据）并返回可读错误；② 前端类型 `durationDays: number \| null`，等级卡片对不可售等级**禁用选择**＋文案"暂不可购买（未配置有效期）"，`doSubscribe` 加兜底校验 |

> **C-11 附带发现**：`com.moyun.portal.domain.vo.UserProfileVO` **早已存在但零引用**（死类，自 6e487c3e 起），且其字段集包含 `email/phone/wechat/role/loginIp/loginDate/vipExpireAt` 等——比实体更"方便泄露"。本批**复用该文件并重写为白名单**（而非另建新类），顺手清掉一个死类。

### 二、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B -q compile` | ✅ exit 0 |
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |

> **过程记录**：本批 eslint 一度出现 2 个 error（我在会员卡片上同时写了 `class` 与 `:class`，`vue-eslint-parser` 判为 duplicate-attribute）——**是 lint 门禁拦住了**，已合并为单个 `:class` 三元表达式；另清理了一处因类型切换而失效的 `UserType` 导入，warning 计数回到 38。

## v13.75 (2026-10-01) 全端评审落地（第 13 批）：波次 C 话题模块（#20/#21 + 由类型修正**连带揪出 7 处同类残留**）

**依据**：清单 #20（TopicDetailPage 作者信息）、#21（TopicListPage 发起人信息）。

### 一、根因

前端 `types/api.ts` 把 `Topic.creator`、`TopicPost.user`、`TopicComment.author` 声明为**嵌套对象**，
而后端实际是**扁平字段**：

| 后端 VO | 实际字段（实测源码） |
|---|---|
| `TopicVO` | `creatorId` / `creatorUsername` / `creatorNickname` / `creatorAvatar` |
| `TopicListVO` | `creatorId` / `creatorNickname` / `creatorAvatar` |
| `TopicPostVO` | `nickname` / `avatar` / `replyToNickname` |
| `TopicCommentVO` | `authorNickname` / `authorAvatar` / `replyToNickname` |

⇒ 所有 `creator?.nickname / user?.avatar / author?.nickname` 取值**恒为 undefined**，页面显示"匿名用户"、头像走兜底图。
**类型声明与后端不一致，正是 vue-tsc 拦不住这类错误的原因**（嵌套可选对象让任意取值都"合法"）。

### 二、修复

1. **类型对齐后端**：`Topic` 改平铺 `creatorNickname/creatorAvatar/creatorCertified`；`TopicPost` 改 `nickname/avatar/replyToNickname`；`TopicComment` 改 `authorNickname/authorAvatar/replyToNickname`。
2. **两个报告点名页面**全部改读扁平字段（`TopicDetailPage` 7 处 + `TopicListPage` 3 处）。
3. **✅ 由类型修正连带揪出并修复另外 7 处同类残留**（报告未列，靠 `vue-tsc` 报错发现——这正是"类型与后端不一致"的连带代价）：
   | 文件 | 处数 | 症状 |
   |---|---|---|
   | `MyTopicsPage.vue` | 3 | 我发起的话题：发起人昵称/头像恒为兜底 |
   | `MyTopicPostsPage.vue` | 3 | 我的观点：作者昵称/头像恒为兜底 |
   | `TopicDetailPage.vue:899` | 1 | 观点"回复 @xx"提示的 `v-if` 读 `post.replyToUser`（恒 undefined）⇒ **回复提示永不显示** |
4. **认证徽章**：详情页 `v-if="topic.creator?.isCertifiedCreator"` 读的是后端**从未下发**的字段 ⇒ 徽章永不显示。后端 `TopicVO` 已补 `creatorCertified`，并在 `PortalTopicServiceImpl` 用**已经查出的** `PortalUser creator` 直接赋值（**无额外查询**），门户改读 `topic.creatorCertified`——既修好显示，也避免删掉产品 UI。

> 说明：`TopicListVO` 未加该字段——列表页不使用徽章，不做无谓的契约扩张。

### 三、校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o -B test`（默认 profile） | ✅ **469/469，BUILD SUCCESS** |
| 门户 `vue-tsc -b --force` | ✅ exit 0（**过程中先报错拦住了我第一版修法**，并直接列出上述 7 处残留，逐条修完才通过） |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings） |

## v13.74 (2026-10-01) 全端评审落地（第 12 批）：波次 C 前端契约/交互（10 条 P1，含 1 处**比报告更严重**的缺陷）

**依据**：用户授权"这些找出的问题全部执行"，进入波次 C（P1 前端）。本批只动前端，逐条按"后端真实契约"核对后再改。

### 一、修复清单

| # | 页面 | 缺陷 | 修复 |
|---|---|---|---|
| **C-1** | `MyBookmarksPage.vue`（清单 #34） | 收藏列表**跳题用收藏记录主键** `item.id`，且**把 `InterviewBookmarkVO` 整体当题目用**：后端把题目嵌在 `question` 下（`PortalInterviewServiceImpl.selectBookmarkPage` 实测 `vo.setQuestion(...)`），故 `item.title / categoryName / tags / description` **全部渲染为空**（比报告描述的"仅跳转错题"更严重，属可见功能失效） | 列出类型改为"题目字段 + 收藏时间"（`BookmarkedQuestion`）；加载时按 `UserPage.vue:441` **既有正确写法**展开 `b.question` 并把 `b.createTime` 记为 `bookmarkTime`；跳转恢复 `item.id`（展开后即题目 id）；`bookmarkTime()` 去掉 `any` 与错误的 `createTime` 回退；`api/interview.ts` 的 `getMyBookmarkList` 泛型由 `InterviewQuestionVO` 改为真实的 `InterviewBookmarkVO` |
| **C-2** | `MyExperiencesPage.vue`（#46） | `formatDate((exp.createTime \|\| exp.updateTime, 'YYYY-MM-DD'), …)` **逗号表达式**导致首参恒为字面量 `'YYYY-MM-DD'`，第二参（格式串）被丢弃 → 时间列恒为 Invalid Date | `v-if` 改为直接判断 `exp.createTime \|\| exp.updateTime`；取值改为 `formatDate(exp.createTime \|\| exp.updateTime, 'YYYY-MM-DD HH:mm')` |
| **C-3** | `MessagesPage.vue` + `types/api.ts`（#63） | 会话预览读 `session.lastMessage \|\| session.lastContent`，**后端 `MessageSessionVO` 只有 `lastMessageContent`** → 列表预览恒为"暂无消息" | 前端改读 `lastMessageContent`；类型里**删除不存在的** `lastMessage`/`lastContent` 并补 `lastMessageContent` |
| **C-4** | `ResumeTemplatePage.vue`（#8） | 分页参数传 `page`，而 `PageDomain` 只接受 `pageNum` → 被 Spring 静默忽略、**页码恒为 1** | 改为 `pageNum` 并加注释说明 |
| **C-5** | `SearchPage.vue`（#64） | 排序传 `sortBy`，`ArticleQuery`（extends `PageDomain`）**无此字段** → 静默丢弃，排序恒为默认序；且"推荐"与"最新"实际同效 | 改传后端真实契约 `orderByColumn`/`isAsc`（列名用**真实列** `views` / `create_time`）；「推荐」不传排序参数，显式走 mapper 的 `defaultOrderBy`（置顶优先 + 发布时间倒序），语义写进注释 |
| **C-6** | `ResumeOptimizePage.vue`（#2） | 优化历史跳转用 `name: 'ResumeEdit'`，**路由表中不存在该名称**；且把简历 id 放在 `query`，而编辑页读的是 **path param**（同文件 1093 行才是正确写法） | 改为 `router.push(\`/interview/resume/edit/${h.resumeId}\`)`，与既有写法统一 |
| **C-7** | `WalletPage.vue`（#53） | 提现状态映射缺 `paying`（状态机为 auditing→**paying**→paid），"说明"列还会把打款中错显成"等待平台审核" | 补 `paying: '打款中'`；说明列按状态机四值分别给文案 |
| **C-8** | `AuthorsPage.vue`（#42） | 搜索词/排序变化**不重置页码**：在第 N 页输入搜索词 → 过滤结果变短 → 切片为空数组 → 误显"没有找到作者"，且分页控件因 `totalPages<=1` 被隐藏（用户无法返回） | 引入 `watch`，`watch([searchQuery, sortBy], () => currentPage.value = 1)` |
| **C-9** | `AuthorPage.vue`（#40） | 游客点"关注"**静默无响应**（`if (!author.value \|\| !currentUser.value) return;`），同页"私信"却有登录引导 | 未登录时 `toast.info('登录后才能关注作者')` + 跳登录并带 `redirect` 回跳 |
| **C-10** | `HelpCenter.vue`（#57） | "联系客服"按钮**既无 `@click` 也无 `href`**，点击完全没有反应 | 接入站内已有的 `/report` 意见反馈页（`useRouter` 导入 + `router.push('/report')`），未新增死链 |

### 二、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0（过程中曾报 `questionId` 不存在，**正是类型检查拦住了错误修法**，据此改为按真实 VO 展开） |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings，与上一批持平） |

**做法说明**：本批 10 条全部先核对**后端真实契约**（`PageDomain` 字段、`ArticleQuery` 继承关系与 mapper 的 `orderByColumn` 分支、`MessageSessionVO`/`InterviewBookmarkVO` 字段、路由表实际 path/name）再改，避免"照报告字面改"引入新的字段错配；C-1 亦因此发现报告低估了影响面。

## v13.73 (2026-10-01) 全端评审落地（第 11 批）：波次 D 质量门（CI + ESLint + 记账构建守卫）

**依据**：《全端-评审-问题清单-20261001》「全端-质量门」P1 组。这是**元问题**——守卫与铁律再完善，没有自动执行点就等于没有约束。

### 一、新增 CI（原"无任何自动执行点"）

新增 `.github/workflows/ci.yml`（两个 job）：

| Job | 内容 |
|---|---|
| **backend** | service 容器起 **MySQL 8.0（库名 `moyun-db`）+ Redis 7** → 按部署指南顺序导入 **init-sql 四件套**（ddl / dml-init / menu-redo / portal-category-redo）→ `mvn -B -q compile` → **`mvn -B test`（469 例，含 21 个守卫类）** → 上传 surefire 报告 |
| **frontend** | 门户：`npm ci` → `vue-tsc -b --force` → **`eslint`（error 级必须为 0）** → `npm run build`（`VITE_SITE_URL` 由 env 注入）；后台：`npm ci` + `build:prod`；记账 App：`npm ci` + `build:h5`（同时验证"接口地址构建期守卫"不误报） |

**已验证**：YAML 可被 `js-yaml` 正常解析（jobs/services/steps 结构符合预期）；流水线引用的 4 个 init-sql 文件均存在；该文件未被 `.gitignore` 排除。

### 二、ESLint 从"完全不可用"到"可用且绿"

**原状**：声明了 `lint`/`lint:fix` 并装了 eslint 全家桶，但**没有任何配置文件**，实跑报
`ESLint couldn't find a configuration file`（退出码 -1）——门禁形同不存在。

- 新增 `moyun-portal/.eslintrc.cjs`：`vue-eslint-parser` + `@typescript-eslint/parser`，extends `eslint:recommended` / `plugin:vue/vue3-essential` / `@typescript-eslint/recommended`；**只把确定性高且当前干净的规则设为 error**，与现有风格冲突大的规则显式关掉或降 warn（否则门禁一开即红、必被绕过），并注明后续可逐步提升。
- **首跑 8 个 error 已全部修掉**（不是靠降级规则）：
  | 位置 | 问题 | 处理 |
  |---|---|---|
  | `api/client.ts:232`（`fetchListPage`） | `no-async-promise-executor` | 改为 `async` 函数直接 `return`/`throw`；**顺带补上缺失的 JSON 解析守卫**（原裸 `await response.json()`，网关返回 HTML 时抛 SyntaxError，用户看到原始报错）——即清单 A1，一并关闭 |
  | `api/category.ts:138,183` | 同上（单飞加载 Promise） | 改为 async IIFE，保留"并发共用同一 Promise + finally 清空"语义；**顺带消除悬挂风险**（Promise 构造器忽略 async executor 的返回 promise，异常会导致永不 settle） |
  | `pages/ReadingPage.vue:197,259,414` | `vue/no-unused-vars` | `v-for="(x, index) in …"` → `v-for="x in …"`（index 确实未使用） |
  | `utils/excerpt.ts:28` | `prefer-const` | `let text` → `const text` |
  | `utils/websocket.ts:191` | `no-control-regex` | STOMP 帧按协议以 NUL(`\x00`) 结尾，此处是在**剥离**终止符，属有意为之 → 加 `eslint-disable-next-line` 并写明理由（而非改规则） |
- **结果**：`eslint . --ext .ts,.vue` → **0 errors / 38 warnings，退出码 0**（38 条 warn 多为"未使用的变量/导入"，属后续可清理项）。

### 三、记账 App 生产包"占位域名 + 静默回落 localhost"（发布阻断项）

**原状**：`.env.production` 是占位域名 `https://api.example.com`，且 `request.js` 有
`|| 'http://localhost:8080'` 的**静默回落** ⇒ 生产包要么指向不存在的域名，要么在缺配置时请求"用户设备自身"（小程序真机必然失败），两种都极难排查。

- `vite.config.js` 新增**构建期守卫** `moyun:api-base-url-guard`（与门户 site-url 插件同思路）：生产构建要求 `VITE_API_BASE_URL` **非空、https、且主机名不是占位/本地**；判断按 **hostname** 而非子串（避免 `api.example.invalid` 这类漏判）。
- `request.js` 去掉 localhost 回落，改为**缺失即抛错**（fail-closed）。
- `.env.production` 头部写明"不替换则生产构建会失败"。
- **三例实测**：① `.env.production` 的占位域名 → **构建失败**并给出明确原因；② `https://api.example.invalid` → **构建失败**（证明 hostname 判定生效）；③ `https://ledger.moyun.cn` → **构建成功**，且产物中只含该真实域名、**不含**占位或 localhost。

### 四、门户构建配置模板

新增 `moyun-portal/.env.production.example`（说明 `VITE_SITE_URL` **必填**、为何留空会构建失败、`VITE_API_BASE_URL` 走同源 `/api`）——原先只有注释提醒"部署时替换"，结果占位域名真的随产物上线过。

### 五、本批**测量后决定暂不做**的项（附实测依据，非搁置）

| 项 | 实测 | 结论 |
|---|---|---|
| 门户 `strict: true` | 用 `vue-tsc --noEmit --strict` 实测 **74 个错误**（TS18049/18047/18048 共 31 个"可能为 null/undefined"；TS2345/TS2322 共 38 个类型不匹配） | **可做但属重构**：建议单独批次，先开 `strictNullChecks` 并按模块分 3 批清零，避免一次性改动引入行为变化 |
| 后台 `moyun-admin-vue` 类型检查 | 该端**未安装任何 TypeScript 工具链**（无 `typescript`/`vue-tsc`、无 `tsconfig.json`），144 个 JS 文件中仅 1 个 `.ts` | **需先引入工具链**（含 lockfile 变更）→ 单独批次，用 `allowJs` 渐进接入 |

### 六、校验

| 项 | 结果 |
|---|---|
| 门户 `vue-tsc -b --force` | ✅ exit 0 |
| 门户 `eslint . --ext .ts,.vue` | ✅ **0 errors**（38 warnings），exit 0 |
| 门户 `npm run build`（`VITE_SITE_URL` 注入） | ✅ exit 0；产物自检：无 `%SITE_URL%`、无历史占位域名，含注入域名 |
| 记账 `build:h5` | ✅ 占位域名**按设计失败**并给出明确原因；注入真实域名后**成功**且产物正确 |
| CI 工作流 | ✅ YAML 解析通过；引用的 4 个 init-sql 文件存在；未被 gitignore |
| 后端 | 本批未改 Java 代码；上一批已验证 `mvn -o -B test`（默认 profile）**469/469** |

**四同步**：新增 CI 与 lint 配置属工程基建；`.env.production.example` 与记账 `.env.production` 注释为部署说明更新。

## v13.72 (2026-10-01) 全端评审落地（第 10 批）：P1 后端 5 项（含 1 项**误判订正**）

**依据**：用户授权"这些找出的问题全部执行"，按波次 B（P1 后端）推进。

### 一、完成的修复

| # | 缺陷 | 处理 |
|---|---|---|
| **B-1** | **会员额度可被"缓存丢失"重置**（⚠️ **原报告机制有误，已订正**，见下） | `VipServiceImpl.getUsedCount`：**Redis 未命中不再直接返回 0**，改为回退 `dbUsedCount` 并尽力回填 Redis（复用既有 `touchUsageTtl` 的 TTL 策略）。新增 `reseedUsedCountFromDb()` |
| **B-2** | 注册接口**实体直绑越权**（mass assignment）：可自证手机/微信已验证、自填 `vipExpireAt`、自选 `role`、自改 `isCertifiedCreator`/`status`/`delFlag`、写 `loginIp`/`loginDate` | 新增 `sanitizeRegisterFields(PortalUser)`：**13 个列复位**（id/userId/role/platformCode/isCertifiedCreator/vipExpireAt/isPhoneVerified/isWechatVerified/twoFactorEnabled/status/delFlag/loginIp/loginDate），`role` 由"仅空时兜底"改为**一律强制 `user`**；复活路径本就只白名单更新，未受影响 |
| **B-3** | 支付回调**不校验金额** | `PayNotifyMessage` 新增 `amount`（元）；`WechatPayChannel.parseNotify` 解析金额（扁平 `amount` 视为元；微信 `amount.total` 按**分**→元 `movePointLeft(2)`；新增数值提取器 `extractJsonNumber`，原 `extractJsonField` 只能读带引号字符串）；`PayGatewayImpl.handleNotify` **fail-closed 比对**：订单与回调金额不等 → 记 error 并抛异常拒绝处理；**回调未带金额时告警留痕但不阻断**（真实通道未接入，避免误伤） |
| **B-4** | `birthday` 写入空串到 **date 列**（严格模式报错或产生 `0000-00-00`） | 注册白名单 + `PortalUserController` 资料更新两处：空串/`"null"`/非法格式一律归一为 `null`，非法格式返回明确提示（与 `LedgerAiAnalysisServiceImpl` 既有口径一致） |
| **B-5** | **真实 163 授权码**作为默认值提交（且文件注释已自称"已移除"，实测未移除） | ① `application-dev.yaml` 默认值清空为 `${MAIL_PASSWORD:}`（空 = 邮件通道未就绪，接口明确提示"暂未开启"）；② `ConfigWiringValidator.looksLikeDevPlaceholder` 增加该泄露值判定（**只比对前 8 位**，避免把完整口令再写进源码），生产注入该值即**阻断启动**；③ 评审文档中的完整值**已脱敏**，避免二次扩散 |

> ⚠️ **B-5 仍需人工收尾**：授权码已进 git 历史，**必须在 163 邮箱后台重新生成并作废旧值**——这一步我无法代做。

### 二、★ 一处**误判订正**（诚实披露）

原《问题清单》P1-a 记为「`@VipOnly` 注册表查询异常时 **fail-open**，付费点变免费」。本轮逐行核对后**判定该机制不成立**：

- `VipOnlyAspect` 的语义是 **`isDisabledInRegistry()==true` → 绕过付费墙**；异常时它返回 `false` ⇒ **继续执行权益校验**，对**付费墙而言是 fail-closed**（丢掉的是"后台禁用接口"这个逃生口，属可用性而非越权）。
- `consumeBenefit`/`hasBenefit` 的异常路径均回退 `dbConsume`/`dbUsedCount`（DB 兜底），同样不 fail-open。

**但同一条目指向的"付费墙可被绕过"确有其事，只是根因不同**：`getUsedCount` 把 **Redis 未命中**当成 0（而非回退 DB），因此 Redis 被清空/重启/提前淘汰时，本周期已用次数归零 ⇒ 额度被无限重置。已按 B-1 修复。清单条目已同步订正。

### 三、校验

| 项 | 结果 |
|---|---|
| `mvn -o -B -q compile` | ✅ exit 0 |
| `mvn -o -B test`（**默认 profile，即文档给出的命令**） | ✅ **Tests run: 469, Failures: 0, Errors: 0 → BUILD SUCCESS** |

**顺带关闭一条 P1**：「全端-质量门 / 测试可执行性」（默认 profile 库名 `moyun-db2` 不存在导致 `mvn -o test` 为红）现已**实测通过**——`application-dev.yaml` 的 JDBC 默认库名已改为 `moyun-db`（该改动**非本轮所为**，来自会话外，本轮据实验证并关闭）。

### 四、环境事实

三端 `node_modules` 已于 v13.71 用 `npm ci` 重建（本轮未再变化）。

## v13.71 (2026-10-01) 全端评审落地（第 9 批）：**P0 全部修复**（6/6）+ 判题 2 项 P1

**依据**：《全端-评审-问题清单-20261001》**全部 P0（6 条）**，另附带修复判题 2 项 P1。用户已授权"这些找出的问题全部执行"。

### 一、P0 逐条修复

| # | 模块 | 缺陷（原状） | 修复 | 关键证据 |
|---|---|---|---|---|
| **A1** | 门户-内容社区 | 专栏**订阅**：后端 `toggleSubscribe` 返回 `boolean`，前端读 `res.data.subscribed/subscribeCount` → 订阅成功反被判为"已取消订阅"，订阅数被清零 | 后端改为返回**切换后的专栏详情 VO**；前端 `column.ts` 类型改 `ColumnVO`、`handleSubscribe` 读 `isSubscribed/subscribeCount` 并回填 `column.value`；顺带删除失效类型 `SubscribeToggleResult` | `PortalColumnController.java`（toggleSubscribe）、`api/column.ts`、`ColumnDetailPage.vue`、`types/api.ts` |
| **A2** | 门户-内容社区 | 专栏**完结**：后端返回影响行数 `int`，前端 `column.value = res.data` → 整页专栏数据被覆盖成数字 | 后端改为返回切换后的**详情 VO**（前端注释本就假定如此，是后端未兑现） | `PortalColumnController.java`（toggleFinish） |
| **A3** | 门户-内容社区 | 文章"**重新提交审核**"会**清空该文章全部标签**（数据丢失）：`/portal/article/my` 不返回 tag_names ⇒ 前端传 `tagNames: undefined`（JSON 丢弃）⇒ 后端 `edit` **无条件** `bindTags(null,null)` ⇒ `finalIds` 为空 → 旧标签全解绑 | 后端加**显式提供才更新**判定：`tagIds != null \|\| tagNames != null`。语义精确区分——**省略字段=不改标签**（修数据丢失）、**传 `[]`=显式清空**（保留"清空标签"能力，`PublishPage` 恒传 `tagNames`） | `PortalArticleController.java` edit、`PublishPage.vue:531/624`、`MyArticlesPage.vue:263-274` |
| **A4** | 门户-用户与账号 | 关注/取关用错 ID：列表 SQL 取 `f.id`（portal_follow 主键）与 `following_id AS user_id`，前端却用 `item.id` 调 `POST /portal/follow/{userId}` → 关注到错误对象 | `FollowUserItem` 补 `userId` 字段（注明 `id` 不是用户 ID）；`FollowListPage` 的关注/取关/跳主页/头像种子/按钮 pending 全改用 `userId`；`UserPage` 的关注与粉丝两个列表同样修正 | `FollowUserVO.java:23`、`PortalFollowMapper.xml`（`AS user_id`）、`FollowListPage.vue`、`UserPage.vue` |
| **A5** | 门户-支付与钱包 | 绑定银行卡表单**从不采集也不提交 `certNo`** ⇒ 后端走"未提供身份证号→四要素核验不发起"分支 ⇒ 卡**永远 PENDING** | `bindForm` 增加 `certNo` + 模板输入项 + 提交字段；复用既有 `@/utils/idCard` 的 `validateIdCard` 校验；`BankCardForm` 把 `certNo` 标为**必填**并注明用途 | `WalletPage.vue`、`types/api.ts` BankCardForm、`BankCardServiceImpl.java:96-120` |
| **A6** | 后端-支付与资金 | 出金 mock 的"生产强制关闭"**守卫实际无效**：`PayProperties.init()` 只改已绑定对象字段，而 `MockPayoutChannel` 是否装配由 `@ConditionalOnProperty` 在 **bean 定义期读 Environment** 决定，且 `resolvePayoutChannel()` **从不读该字段** ⇒ 生产漏配 prod yaml 时"扣款却不出金" | ① 新增 **`core/config/EnvironmentProfile`**：全项目唯一环境判定，**非生产白名单 + 默认按生产（fail-closed）**（`dev/local/test/...` 之外一律按生产，`prd`/`gray`/`k8s-prod` 不再静默失效）；② `PayProperties.init()` 改用该判定并暴露 `isProductionEnvironment()`；③ **真正出金前做运行时断言**：渠道为 mock 且判定为生产 → 抛 `WITHDRAW_PAYOUT_MOCK_FORBIDDEN` 拒绝出金 | `EnvironmentProfile.java`（新增）、`PayProperties.java`、`WithdrawOrderServiceImpl.resolvePayoutChannel()` |

> **A6 取向**：让"profile 写错"的后果变成**守卫生效（拒绝出金）**，而不是**守卫失效（资金受损）**；非生产环境（dev/local/test）行为不变。

### 二、附带修复（判题 2 项 P1）

| # | 缺陷 | 修复 |
|---|---|---|
| **B1** | **生产引擎泄露隐藏用例**：`DockerJudgeEngine` 失败时**无条件**回填 `failedCaseInput/Expected/Actual`，而 `ProcessJudgeEngine` 有 `isSample` 守卫；生产默认引擎正是 docker ⇒ 生产泄露判题资产 | 补齐同一 `isSample` 守卫（仅样例用例回填），并把"为什么生产更危险"写进注释 |
| **B2** | **TLE 永不生效**：两引擎都**先 `readAllBytes()`（阻塞至 stdout EOF）再 `waitFor(timeout)`** ⇒ 用户代码不退出时判题线程被永久占用，既不判 TLE 也不 `destroyForcibly` | 改为把 stdout/stderr **重定向到文件**，先 `waitFor(timeout)`、超时 `destroyForcibly`、**之后**才读文件（读文件不阻塞）。四处（两引擎 × 编译/运行）统一；抽出 `readTruncated()` 说明成因 |

### 三、校验

| 项 | 结果 |
|---|---|
| `mvn -o -B -q compile` | ✅ exit 0 |
| `mvn -o -B test "-Dspring.profiles.active=local"` | ✅ **Tests run: 469, Failures: 0, Errors: 0 → BUILD SUCCESS** |
| `vue-tsc -b --force`（门户） | ✅ exit 0 |

**四同步**：涉及**接口契约**（专栏订阅/完结返回体、文章标签语义）与新增安全配置类，已记录；`README`/`部署指南`/方案文档同步列入后续批次。

### 四、环境事实（如实记录，非本批代码问题）

本轮开始时发现三端（门户/后台/记账）的 **`node_modules` 与 `dist` 全部不存在**（上一轮验证时仍在；本轮未执行任何删除操作，判定为会话外变化）。为恢复可验证性，已用 `npm ci` 重建三端依赖（门户已复跑类型检查通过）。

## v13.70 (2026-10-01) 全端评审落地（第 8 批）：用项目自有守卫验证全部清理结果（469/469 全绿）

**目的**：本轮 7 个清理批次（v13.63~v13.69）共改动约 350 个文件。仅靠"编译 + 构建"不足以证明没破坏**守卫所强制的架构不变量**，故跑**全量测试套件**做最终验收。

**实跑命令与结果**

```bash
cd moyun-server && mvn -o -B test "-Dspring.profiles.active=local"
→ Tests run: 469, Failures: 0, Errors: 0, Skipped: 0
→ BUILD SUCCESS
```

> 说明：用 `local` profile 是因为默认 `dev` profile 指向不存在的库 `moyun-db2`（该问题已登记为 P1，见问题清单「全端-质量门 / 测试可执行性」）。`local` 指向已存在的 `moyun-db`。

**★ 守卫确实拦下了一处真实漂移（这就是它存在的意义）**

首跑 **468/469**，唯一失败是 `ModuleDependencyGuardTest.frozenEdgesMatchExactly`：

```
ext.cms -> portal：期望 280，实际 277
  · 计数增加 = 新增跨模块依赖；· 计数减少 = 债务已偿还，请同步下调清单数字
```

- **根因**：本轮清理删掉了 `ext.cms` 侧 3 处**已无引用的** portal 依赖 import —— `VoiceInterviewServiceImpl` 的 `PortalInterviewQuestion`、`FeedServiceImpl` 的 `PortalArticle` 与 `PortalUser`（已用改动前备份逐一核对，确为 3 处、与 280→277 完全吻合）。
- **处理**：按守卫自身要求**同步下调冻结计数** 280 → 277，并在 `FROZEN_REASONS` 中按既有体例续写本次来源与原因（守卫规定"减少也要同步下调，防止清单腐烂"）。
- **复跑**：**469/469 全绿，BUILD SUCCESS**。

**结论**：本次全量清理 **未破坏任何守卫强制的架构不变量**（模块依赖方向与冻结计数、`${}` 信任契约、DDL 口径、事务边界与回滚口径、执行器治理、异步自调用、增量写影响行数、LLM 输出解析收敛、前端模板结构、权限收口等 21 个守卫类全部通过）。

**四同步**：本次仅更新守卫测试中的冻结计数与其理由注释，**不涉及**接口契约/配置键/表结构/状态机/菜单权限 → 无 SQL、无菜单变更。

## v13.69 (2026-10-01) 全端评审落地（第 7 批）：死 CSS 判定 + import 长尾收尾（B6 收口，含明确"不做"项）

**依据**：《全端-评审-问题清单-20261001》P3「多余代码」类；B6 收尾。**本批重点在"哪些不能自动删、为什么"**。

### 一、死 CSS：只删 4 条，其余转为人工复核（不自动删）

扫描 346 个 `.vue`（169 个含 `<style scoped>`，其中 **74 个文件因存在动态类名拼接被整体跳过**），得到候选 41 条。**最终只删除 4 条**，其余 37 条**明确不自动删**。

**为什么不做自动删除**：CSS **没有构建期安全网** —— `vue-tsc` 与 `vite build` 都不会因"删掉仍在使用的类"而失败，删错只造成**静默 UI 回归**。扫描过程中实测确认了 **4 类系统性误判**，每一类都足以造成回归：

| # | 误判类 | 实例 | 性质 |
|---|---|---|---|
| 1 | Vue **过渡动画类** | `.fade-enter-active`、`.modal-leave-to`、`.sidebarLogoFade-enter` | 由 `<transition name="fade">` **运行期生成**，模板中永不出现（已加排除规则） |
| 2 | **第三方组件内部类** | `.el-input-number--small`、`.el-scrollbar__view`、`.el-tree-node__content` | Element Plus 子组件**根元素会带父组件 scoped 属性**，故这些 scoped 规则**确实生效**，无法静态判定 |
| 3 | **解析器误读声明值** | 从 `url(".../login-background.jpg")` 解析出类名 `.jpg` | 选择器解析 bug（已修：声明行不再参与选择器解析） |
| 4 | **复合选择器 / 字符串拼接** | `.el-select-dropdown__item.selected`、`:class="'txn-icon ' + t.type"` | 类名仅为复合选择器一部分，或由字符串拼接产生 |

**已删（4 条规则 / 25 行，均经人工确认"全文件仅自身选择器一处出现"）**：
`.screenfull-svg`（Screenfull）、`.errLog-container`（layout/Navbar）、`.custom-img`（Settings）、`.login-tip`（login）—— 均为 RuoYi 脚手架/旧功能残留。删后 `vite build`（admin）exit 0。

**产出**：`docs/09-临时报告/全端-评审-死CSS人工复核清单-20261001.md`（37 条带风险标记，供浏览器目视后决定）。

### 二、import 长尾：再删 5 个 specifier，其余 21 条**明确保留**

- 处理 `import { … } from 'x'` 的**多行 clause**：删除"独占一行"的未使用 specifier → 实删 5 个（`file.ts` 去 `UploadFileParams`；`pay.ts` 去 `PayLedgerEntry`/`PayNotification`；`JudgeResultPanel.vue` 去 `Play` 等）。
- **主动不做的 21 条**：这些多行 import 是"**一行塞多个 specifier**"风格（如 `Search, Plus, LogOut, Menu, …` 一行 10 个）。要删其中 1~2 个必须**重排整段**（拆行或并成超长单行），收益（1~2 个标识符）远小于格式churn 与误伤风险 → **保留现状**。
- 另有 **9 条整条 import** 被"名字仍出现在注释中"的保守门持续拦下（每次重扫都会重现），同样**保留**。

### 三、本批"明确不做"的项（附理由，避免后续反复尝试）

1. **注释掉的代码块**（早前扫描约 84 段）：其中相当比例是**有意保留**的第三方接入骨架（`WechatPayChannel` 微信 SDK 骨架、`OcrServiceImpl`、`AliyunSmsSender`、`FourElementsRealNameVerifier` 的"接入时放开"注释）。**逐处判断成本高于收益，且误删会破坏接入路径** → 不做；如需清理应作为独立任务逐文件确认。
2. **死 CSS 批量删除**：见上，无安全网 → 不自动删。

### 校验

| 项 | 结果 |
|---|---|
| `vite build`（admin，删 CSS 后） | ✅ exit 0 |
| `vue-tsc -b --force`（门户，剪 import 后） | ✅ exit 0 |
| 结果一致性 | ✅ CSS 删除器与 import 剪裁器均断言"结果 = 原文减去且仅减去这些行" |
| 死 CSS 复扫 | 41 → **37**（余者全部带风险标记，已在清单中登记） |

**四同步**：纯代码/样式卫生，无接口契约/配置键/表结构/菜单权限变更。

**B6 状态：收口**。已完成：未使用 import（121 整条 + 9 specifier）、死声明（88 前端 + 11 Java，1400 行）、死 CSS（4 条规则）。已明确不做的 3 类（多行 specifier 重排、注释掉的代码骨架、死 CSS 自动删）均记录理由并留存清单。

## v13.68 (2026-10-01) 全端评审落地（第 6 批）：死声明清理 1400 行（B6 收口）

**依据**：《全端-评审-问题清单-20261001》P3「多余代码」类；B6 第二步（承接 v13.67 的 import 清理）。

**方法：先造工具 + 四道硬门，再做删除**

自研扫描器识别两类"死声明"，判定标准极保守 —— **名字在整份文件（含注释与字符串）中，除自身声明外零出现**：
- **Java**：`private` 方法（私有 ⇒ 作用域仅限本文件，判定可靠）；带注解的一律跳过（`@Override`/`@PostConstruct`/`@EventListener`/`@Scheduled` 等可能被框架按名调用）。
- **前端**：`<script setup>` / 模块内**非 export** 的函数与常量（导出的不删，可能被他处引用）。

**四道硬门（全部在"真实犯错"之后补上）**

| 门 | 作用 | 为什么必须有 |
|---|---|---|
| ① 跨行声明边界修正 | 无括号的声明（如 `const isLt = a / b < c;`）**必须单行结束** | 旧实现会继续向后扫描直到遇到括号，**吞掉后面的活代码**（实例：`isLt` 被判成 5 行） |
| ② 名字在 span 内只能出现 1 次 | 出现 ≥2 次说明 span 吞了别处代码 ⇒ 拒绝 | 这是对 ① 的兜底，能拦住所有"过度吞并" |
| ③ 整段不含 Vue 编译宏 | `defineProps/defineEmits/defineExpose/defineSlots/defineOptions/defineModel` 出现即跳过 | 宏有**声明副作用**：`const props = withDefaults(defineProps<Props>(), {…})` 即使 `props` 未被读取也必须保留 |
| ④ Java 注解跨行扫描 | 向前扫最多 6 行（跳过空行与注释行）找注解 | 旧实现只看紧邻一行，多注解方法会被误判 |

**⚠️ 本轮一次真实失误与处理（如实记录）**

第一次应用前端批次时，门 ③ 只检查了"`= defineProps`"这一形态，**漏掉 `withDefaults(defineProps<…>(), {…})` 这种嵌套写法** ⇒ 删掉了 `Empty.vue`/`LoadingSpinner.vue`/`RelatedArticleCard.vue` 等的 props 声明 ⇒ `vue-tsc` 报 20+ 处 `Property 'size' does not exist on $props`。

处理：**立即用改动前备份回滚"我自己刚做的这一批"**（只回滚 42 个前端文件，保留已通过 `mvn compile` 的 Java 批次），把门 ③ 改为"检查**整个 span**是否含宏"，重扫后 `props` 类误报归零，再重新应用 → `vue-tsc` exit 0。

**结果**

| 项 | 数 |
|---|---|
| 删除的 Java `private` 方法 | **11**（`DataQueryServiceImpl` 独占 8 个，含 121 行的 `inferFieldDescription`、95 行的 `generateQueryExample`；另 `KnowledgeBaseController`、`ChatContextBuilderImpl`、`SqlUtils` 各 1） |
| 删除的前端死声明 | **88**（`diagramRenderer.js` 11 个含 95/101 行的大函数、`ai/workflow/index.vue` 9 个、`ResumeOptimizePage.vue` 7 个等） |
| 删除行数 | **1400**（Java 556 + 前端 844，其中连带的注释行 106） |
| 涉及文件 | **46**（Java 4 + 前端 42；后台/门户为主，含 1 个记账 App 文件） |
| 连带删除的注释 | ✅ 归属该声明的 Javadoc / `//` 注释块一并删除（不留孤儿注释） |
| **传递闭包** | ✅ 清完第一轮后**又暴露出 7 个**（其唯一调用方正是刚被删的死函数），第二轮清完；**最终复扫 = 0 条** |

**校验（每一步都跑）**

| 项 | 结果 |
|---|---|
| `mvn -o -B -q compile` | ✅ exit 0（两轮各跑一次） |
| `vue-tsc -b --force`（门户） | ✅ exit 0（回滚后、重应用后、第二轮后各一次） |
| `vite build`（后台） | ✅ exit 0 |
| `npm run build:h5`（记账App） | ✅ exit 0 |
| span 结构自检 | ✅ 每轮应用前 100% 通过（`findings valid: N/N, problems: 0`） |
| 结果一致性 | ✅ 应用器断言"结果 = 原文减去且仅减去这些 span" |
| 最终复扫 | ✅ **0 条死声明残留** |

**四同步**：纯代码删除（均为零引用声明），不涉及接口契约/配置键/表结构/状态机/菜单权限 → 无 SQL、无菜单变更。

**B6 状态**：import 与死声明两个维度**已收口**。剩余未做：多行 clause 的部分 import 裁剪（19 条）、**死 CSS 段**、**注释掉的代码块**（后者需逐处判断，如 `WechatPayChannel` 的第三方接入骨架属有意保留）。

## v13.67 (2026-10-01) 全端评审落地（第 5 批）：未使用 import 清理（4 端 90 文件 / 121 条）

**依据**：《全端-评审-问题清单-20261001》P3「多余代码」类；对应汇总文档 §五 的 **B6 死代码清理**第一步。

**方法：先造工具、再做清理（不采信"看起来没用"的判断）**

自研精确扫描器（覆盖 2029 个源文件：后端 Java + 门户/后台/记账 TS·JS·Vue）：
判定某 import 绑定"未使用"的条件是——**该名字在"除 import 语句与纯注释行之外的全文件正文"中零出现**。

**扫描器自身修掉 3 个真实误判（否则会删掉在用的 import，属会直接打断功能的错误）**

| # | 误判 | 根因 | 实例 |
|---|---|---|---|
| 1 | 真实使用被吞 | import 语句正则尾部用 `\s*`，**吞掉后续空行**，把紧随其后的真实使用行划入"import 区"而排除出正文 | `ConfirmModal.vue`：`useConfirmModal` 在 import 下一行就被 `= useConfirmModal()` 调用，却被判未使用 |
| 2 | 模板标签未识别 | Vue 单单词 PascalCase 的 kebab 变体判断被 `kebab !== lower` 条件跳过 | `admin Navbar.vue`：`Breadcrumb`/`Hamburger`/`Screenfull` 在模板中正是 `<breadcrumb>`/`<hamburger>`/`<screenfull>`，却判未使用 |
| 3 | 关键字被当成标识符 | `import type { … }` 的 **`type` 关键字**被解析成默认导入名，因而"从未在正文出现" | 门户 `api/*.ts` 等 70+ 处全部误报；修正后候选由 77 → 28 |

修正后候选数由 **178 → 125**；并用 5 个已知用例做回归（4 个必须**不报**、1 个必须**报**）→ **5/5 PASS**。
另加两道 Java/Vue 安全阀：Javadoc `{@link X}` 视为真实使用；提交前对每个文件二次校验"被删名字在剩余正文中仍零出现"。

**结果**

| 项 | 数 |
|---|---|
| 删除整条 import | **121** |
| 从 `{ a, b }` 中裁掉部分未使用名 | **4**（`CreateKnowledgeDialog.vue` 去 `watch`；`JudgeResultPanel.vue` 去 `JudgeStatusCode`；`ResumeOptimizePage.vue` 去 `scoreResume`；`ChapterReaderPage.vue` 去 `ReadingPreference`） |
| 涉及文件 | **94**（后端 / 门户 / 后台 / 记账 四端） |
| 被"仍被引用"二次门拦下（保守跳过） | 4（名字仅出现在注释中，如 `Excel.java` 的 `BigDecimal`、`KnowledgeBaseServiceImpl` 的 `Transactional`） |
| 多行 clause 或结构复杂而**主动跳过** | 19（工具已识别并打印原因，留作后续批次） |

**校验**

| 项 | 结果 |
|---|---|
| `mvn -o -B -q compile` | ✅ exit 0 |
| `vue-tsc -b --force`（门户） | ✅ exit 0 |
| `vite build`（后台） | ✅ exit 0 |
| `npm run build:h5`（记账App） | ✅ exit 0 |
| 抽样复核 | ✅ 被删名字在文件中剩余出现次数为 0；**未改动的文件保持原样**（如 `ToastContainer.vue` 的 `useToast` 仍在、仍被调用） |
| 裁剪前后逐行比对 | ✅ 4 处 prune 均为"仅移除指定名"，其余 specifier 与来源路径不变 |
| 行尾 | ✅ 逐文件按原行尾写回，未转换任何文件 |

**四同步**：纯代码卫生清理，不涉及接口契约/配置键/表结构/状态机/菜单权限 → 无 SQL、无菜单变更。

**未做（如实记录，留给后续批次）**：多行 clause 的部分裁剪（19 条，工具已定位）；死函数/死常量；死 CSS 段；注释掉的代码块。**B6 尚未收口**。

## v13.66 (2026-10-01) 全端评审落地（第 4 批）：版本标签剥离 + 注释清理收口（待处理项 684 → 0）

**依据**：《全端-评审-问题清单-汇总-20261001》§五 B5 批次，承接 v13.63/v13.64/v13.65。本批含两个子批次，并**收口**整个"过期注释"清理。

**C4：长尾 208 个文件 / 283 条候选**（6 路并行分类）
→ 落地 **384 条指令**：删除 108 · 改写 213 · 保留 63。（指令数 > 候选数，差额为"相邻行连带指令"。）

**C5：新增第二轮扫描——"句中版本标签"**（本轮最重要的补漏）

- **为什么要有第二轮**：首轮候选正则只覆盖"版本号 + 改动动词"的组合模式（如 `v13.44 起…`、`（v10.23 …）`），**漏掉了句中裸标签**（如 `审核业务内容端口实现（门户侧适配器，v13.22）`、`/** V11.0：候选人心态状态标签 … */`、`// ============ v11.79 提现闭环 ============`）。
- 用宽松规则重扫全部 2029 个源文件，得到 **262 行 / 98 个文件** → 6 路并行分类 → 落地 **267 条**：删除 7 · 改写 234 · 保留 26。
- 剥离示例：`/** V11.0：候选人心态状态标签（对齐后端 InterviewTurnResult.sentiment.state） */` → `/** 候选人心态状态标签（对齐后端 …） */`；`// ===== v11.90 V2：环境噪声检测（3 秒采样取平均） =====` → `// ===== 环境噪声检测（3 秒采样取平均） =====`。

**本批新增第三道防线：注释结构配对自检（抓出并修掉 1 处真实损伤）**

- 做法：对每个被改文件，用**改动前备份 vs 现文件**逐对比较 `<!--`/`-->`、`/**`/`*/`、`<p>`/`</p>`、`<li>`/`</li>` 的**配对平衡**，任一项"失衡加剧"即标记。
- **它抓出一处此前遗漏的真实损伤**：C2 删掉 `types/api.ts` 中 `<p>原「面试岗位字典」…</p>` 整段后，**遗留一个未闭合的 `<p>`**（0 → +1）。已当场补上 `</p>` 修复。
- 顺带修正 `CacheCleanupTask.java` 两处**既有**未闭合 `<p>`（非本次引入），并确认 C4 对 `CacheCleanupTask` 的改写实际**改善**了结构（4 → 1）。
- 复跑结果：**0 处失衡加剧**。

**手工处理的多行 HTML 注释（2 处，安全门主动报出）**

- `moyun-admin-vue/src/views/ai/scene/index.vue:111-113`、`moyun-portal/src/pages/interview/VoiceInterviewPage.vue:2236-2237`：均为跨行 `<!-- … -->`。**只改首行再删续行会让注释内容渲染成页面可见文字**，故整块替换为一行当前事实说明；安全门累计因此跳过 4 条越界指令。

**明确保留、不再清理的项（含理由，避免后续批次反复误判）**

1. **26 条版本标签属"当前契约"**：文档/规范版本引用（如"见《…方案 V1.3》"）、第三方 API 版本等。
2. **9 条"此前/曾用 X"是反模式说明**：解释当前为何禁用裸线程、未指定执行器的 `runAsync`、`static` 线程池等，与 `ExecutorGovernanceGuardTest` 等守卫配套，属"当前为什么这么写" → 按既定口径保留。
3. 仍在生效的**兼容性说明**（兼容旧数据格式、兜底存量数据、避免 TDZ）。

**校验**

| 项 | 结果 |
|---|---|
| `mvn -o -B -q compile` | ✅ exit 0 |
| `vue-tsc -b --force`（门户） | ✅ exit 0 |
| `vite build`（后台） | ✅ exit 0 |
| `npm run build:h5`（记账App） | ✅ exit 0 |
| 注释结构配对自检 | ✅ 0 处失衡加剧 |
| 改动行性质（`git diff --diff-filter=M`，排除 docs） | ✅ 仅 3 行非注释，均为上述 2 处多行 HTML 注释的续行 |
| 行尾未被改写 | ✅ 逐文件与改动前备份比对：CRLF 文件仍 CRLF，原 LF 文件仍 LF（**无任何文件被转换**） |

**收口结论**：全仓"改动史 / 版本标签"注释 **684 条 → 0 条待处理**（全部处理完毕或经判定应保留）。四批累计修改 **990 行注释**（删除 157 · 改写 833），涉及 **241 个文件**，覆盖门户 / 后台 / 记账App / 后端四端。

**四同步**：纯注释清理，不涉及接口契约/配置键/表结构/状态机/菜单权限 → 无 SQL、无菜单变更。

**顺带发现（本批未改，登记备查）**：仓库行尾 CRLF/LF 混用，`core.autocrlf=true` 且**无 `.gitattributes`**，git 对多个 LF 文件持续告警（如 `ConfigWiringValidator.java`、`qrcode.js`）。统一行尾会产生全文件 diff，属独立变更，建议单独排期。

## v13.65 (2026-10-01) 全端评审落地（第 3 批）：长尾 22 个文件过期"改动史"注释清理

**依据**：《全端-评审-问题清单-汇总-20261001》§五 B5 批次，承接 v13.63/v13.64。本批进入长尾（单文件 ≤8 条），**首次覆盖后台管理端与记账 App**。

**范围（24 个文件候选 122 条 → 落地 149 条指令，含 30 条相邻行连带指令）**

| 端 | 文件数 | 说明 |
|---|---|---|
| 门户 | 11 | `UserPage.vue`、`MyVoiceInterviewsPage.vue`、`MyResumesPage.vue`、`ResumeTemplatePage.vue`、`QuestionListPage.vue`、`OptimizeCompare.vue`、`api/reading.ts`、`router/index.ts` 等 |
| 后端 | 12 | `AiSceneConfigController`、`AiSceneTasks`、`AiGatewayService`、`AbstractAiSceneHandler`、`CmsPortalUserServiceImpl`、`PortalInterviewServiceImpl`、`InterviewSessionSupport`、`WechatPayChannel`、`PortalWebSocketAuthInterceptor`、`SysDashboardServiceImpl`、`PortalGrowthServiceImpl`、`VoiceInterviewReportVO` |
| 后台 | 3 | `views/ai/scene/index.vue`、`views/ai/agent/index.vue`、`views/ai/diagram/chat.vue` |
| 记账App | 1 | `pages/mine/index.vue` |

合计落地 **105 改写 / 29 删除 / 15 保留（误报）**，跳过 0 条。

**本批暴露并修掉的两类新问题**

1. **多行 HTML 注释不能拆删**：`views/ai/scene/index.vue:111-113` 是一条三行 `<!-- … -->`。若只改首行并删续行，剩余文本会**渲染成页面可见文字**。改为把三行整体替换为一行当前事实说明（`<!-- 场景代码由「主场景 + 子任务」两级下拉拼成两段式 -->`）。—— 安全门在此**主动跳过了 2 条**越界指令，是靠 skip 报告发现的。
2. **`<p>`/`</p>` 配对与悬空连接符**：30 条相邻行连带指令正是为"删完不留半句、不留重复闭合标签"而补；`WechatPayChannel.java` 类级 Javadoc 原有 4 个 `<p>` 只闭合 2 个，属**改动前既有状态**，本次未加剧、也未顺手改（不扩大范围）。

**安全门与校验**

| 项 | 结果 |
|---|---|
| `mvn -o -B -q compile`（moyun-server） | ✅ exit 0 |
| `vue-tsc -b --force`（moyun-portal） | ✅ exit 0 |
| `vite build`（moyun-admin-vue） | ✅ exit 0 |
| `npm run build:h5`（moyun-ledger-app） | ✅ DONE, exit 0 |
| 改动行性质（`git diff -U0 --diff-filter=M`，排除 docs） | ✅ 除被手工处理的那条多行注释外，**全部为纯注释行** |
| 三端构建产物 | ✅ 无异常嵌套目录；`dist/` 均被 gitignore |

**四同步**：纯注释清理，无接口/配置/表结构/菜单变更。

**进度**：全仓"改动史"候选 **684 → 291 条**；三批累计清理 **428 行注释**、涉及 **34 个文件**。剩余 291 条中约 20 条已被判定为误报（keep，如 `views/ai/agent/index.vue`、`diagram/chat.vue` 各 4 条），实际待清理约 271 条，且**单文件最多 3 条**，已属纯长尾。

## v13.64 (2026-10-01) 全端评审落地（第 2 批）：后端与前端类型层过期"改动史"注释清理

**依据**：《全端-评审-问题清单-汇总-20261001》§五 B5 批次，承接 v13.63。本批首次覆盖**后端 Java** 与**前端类型/接口层**。

**范围（8 个文件 / 146 条候选逐行分类 → 实际落地 151 条指令）**

| 文件 | 候选 | 落地（改写 / 删除 / 保留） |
|---|---|---|
| `moyun-server/.../ext/cms/service/impl/VoiceInterviewServiceImpl.java` | 50 | 49 / 0 / 1 |
| `moyun-portal/src/types/api.ts` | 28 | 27 / 1 / 0 |
| `moyun-server/.../ext/ai/service/impl/KnowledgeBaseServiceImpl.java` | 17 | 13 / 3 / 2 |
| `moyun-server/.../ext/file/service/impl/SysFileServiceImpl.java` | 13 | 13 / 0 / 0 |
| `moyun-portal/src/api/interview.ts` | 12 | 11 / 2 / 0 |
| `moyun-server/.../ext/cms/service/ResumeParseService.java` | 9 | 7 / 0 / 2 |
| `moyun-server/.../portal/domain/entity/PortalJobTemplate.java` | 9 | 7 / 3 / 0 |
| `moyun-portal/src/api/resumeOptimize.ts` | 8 | 7 / 0 / 1 |
| **合计** | **146** | **134 / 10 / 6**（另含 7 条相邻行补充指令） |

**本批新增的两道防线（都是上一批暴露出来的真实问题）**

1. **相邻行连带处理**：分类阶段发现 5 处"只删本行会让相邻行留下半句/悬空 `</p>`"（`types/api.ts:1091-1092`、`api/interview.ts:41-43`、`PortalJobTemplate.java:16-19`、`KnowledgeBaseServiceImpl.java:1237-1238`）。改为**把整段历史叙事连根删除**（或把首行改写为完整的当前事实句），而不是机械删单行。
2. **Javadoc 结构自检**：修正后复核发现 `KnowledgeBaseServiceImpl:1236` 出现重复 `</p>`（改写句已闭合、下一行又闭合），已手工修正为 `<p>` 只在段末闭合 —— **"改写"必须保证注释块结构仍成立**。

**安全门（沿用 v13.63 并加固）**

- 行号 + 原文双重校验；只处理纯注释行；匹配不上即跳过。
- 非注释行不变式：本批把"裸 `*`/`*/`"明确归入注释脚手架（它在本项目语言里不可能是可执行代码）——**该不变式在第 1 次 dry-run 时确实拦下了 `PortalJobTemplate.java` 的误判并中止了写入**，属有效防线而非摆设。
- 保持 CRLF 行尾：本批 5 个抽检文件 `bareLF=0`。

**校验**

| 项 | 结果 |
|---|---|
| `mvn -o -B -q compile`（moyun-server） | ✅ exit 0 |
| `vue-tsc -b --force`（moyun-portal） | ✅ exit 0 |
| 改动行性质 | ✅ `git diff -U0` 过滤后**无任何非注释行** |
| diff 规模 | 8 个目标文件，**+128 −138**（其余为上一批 4 文件） |
| 跳过条数 | 0 |

**四同步**：纯注释清理，不涉及接口契约/配置键/表结构/状态机/菜单权限 → 无 SQL、无菜单变更。

**剩余清理面**：全仓"改动史"候选由 535 条降至 **396 条 / 225 个文件**（单文件最多 8 条，已进入长尾）。两批累计清理 **295 条注释**。

## v13.63 (2026-10-01) 全端评审落地（第 1 批）：门户过期"改动史"注释清理

**依据**：《全端-评审-问题清单-20261001》（882 条）中的 P3 类"过期历史版本注释"；对应《全端-评审-问题清单-汇总-20261001》§五 的 B5 批次。

**范围（4 个门户文件 / 150 条候选逐行分类）**

| 文件 | 候选 | 处理 |
|---|---|---|
| `pages/interview/VoiceInterviewPage.vue` | 68 | 改写 64 · 删除 3 · 保留 1（误报） |
| `pages/interview/ResumeOptimizePage.vue` | 37 | 全部改写（剥版本号，留当前事实） |
| `pages/interview/ResumeEditPage.vue` | 26 | 全部改写 |
| `api/voiceInterview.ts` | 19 | 全部改写 |
| **合计** | **150** | **删除 3 · 改写 146 · 保留 1** |

- 删除样例：`// v11.97：报告 Tab 收窄（…）`、`// v11.88 V2：analysis 实时分析气泡已移除（…）`、`* 原为前端硬编码 POSITION_OPTIONS；原「岗位字典」portal_interview_position 已并入岗位模板表。`
- 改写样例：`/** v10.23：岗位匹配异步任务 ID（用于 URL 参数化与刷新恢复轮询） */` → `/** 岗位匹配异步任务 ID（用于 URL 同步与刷新恢复轮询） */`
- 保留样例（误报，属**当前**仍在生效的兼容逻辑）：`// 兼容字符串或对象两种后端历史数据格式`

**口径（写入本次清理的判定标准，供后续批次沿用）**

1. **只删"过去怎么改的"**（版本号 + 已被删除/已废弃/曾经是什么），**保留"当前为什么这么写"**（原因、约束、坑）。
2. 一条注释同时含历史叙事与当前事实 → **剥掉历史，保留事实**，不整条删。
3. 仍在生效的**兼容性说明**（兼容旧数据格式、兜底存量数据、避免 TDZ 等）一律保留。
4. 注释掉的代码行不属本批范围。

**手段与安全门（防"批量按文本切割"事故）**

- 不按文本模式切割，改为**行号 + 原文双重校验**：只处理"trim 后以 `//`、`/*`、`*`、`<!--` 开头"的**纯注释行**；原文与记录不一致（±5 行内找不到唯一匹配）即跳过，不猜。
- 每个文件应用后**断言"所有非注释行逐字节不变且顺序一致"**，不满足即整体中止（本次 4/4 通过）。
- **保持原始行尾**（4 个文件均为 CRLF）与末尾换行，避免整文件 diff 噪声；改动前逐文件备份到评审缓存目录。

**校验**

| 项 | 结果 |
|---|---|
| `vue-tsc -b --force`（moyun-portal） | ✅ exit 0 |
| `git diff --stat` | 仅 4 个目标文件（**+146 −149**），无其它文件被改动 |
| 行尾 | ✅ 仍为 CRLF、`bareLF=0` |
| 跳过条数 | 0（150/150 全部按分类落地） |

**四同步**：本次为纯注释清理，**不涉及**接口契约/配置键/表结构/状态机/菜单权限 → 无 SQL、无菜单、README 与部署指南无需变更。

**未做（如实记录）**：本批**未改任何业务逻辑、未删任何死代码、未处理后端与后台注释**；「死代码清理（B6）」「后端/后台/记账App 注释清理（B5 其余 232 个文件中的剩余部分）」「动态化改造（B7）」留待后续批次。

**剩余清理面（供后续批次排期）**：全仓扫描共 **684** 条改动史候选、分布在 **232** 个文件；本批消化 150 条（4 个文件），剩余 534 条，主要集中在 `moyun-server/ext/cms/service/impl/VoiceInterviewServiceImpl.java`(50)、`moyun-portal/src/types/api.ts`(28)、`ext/ai/service/impl/KnowledgeBaseServiceImpl.java`(17)、`ext/file/service/impl/SysFileServiceImpl.java`(13) 等。

## v13.62 (2026-09-30) 语音面试体验三连修：口头结束收口 + 题数口径界定 + 出题结构升级

**用户反馈三问题**：① 对话中说"结束面试"不自动结束；② 预设题数太少且口径不清（一问一答算一题？自我介绍算吗？）；③ 面试官全程围绕第一个话题追问、几轮对话草草收场，出题不充分。

### 修复 1：结束链路收口（代码）

| 改动 | 位置 | 说明 |
|---|---|---|
| verbal_end 服务端收口 | `submitAnswer` | 原只发 SSE `finished` 事件、status 仍 in_progress（前端不回调 /finish 则会话悬挂、报告永不生成）；现同步 `finishQuietly(interview, "verbal_end")` 触发报告 |
| `finishQuietly` 参数化 | closedReason 入参 | 原 hardcoded "timeout"，现支持 verbal_end / timeout 两来源 |
| 时间到轮次后自动收口 | `runAgentTurn` onComplete | 面试官按指令说完收尾话术后（remainMin≤0），end 载荷带 `finished:true` 并服务端收口，不再依赖用户点结束 |
| 幂等锁泄漏修复 | 超时/口头结束两分支 | 提前 return 前 `turnLock.close()`（原只能等 TTL 过期，TTL 内同题重试被误拒）——即评审报告 P1-1/P1-2 |

### 修复 2：题数口径界定（代码 + 前端 + SQL）

- **口径统一**：题数 = **考察方向数**；1 方向 = 1 主问题 + 1~2 轮追问；自我介绍是固定开场环节不计入；实际对话轮数 ≈ 方向数 × 2
- 前端选项 3/5/8 → **5/8/12/15**（快速/标准/深度/沉浸），默认 5 → **8**，下拉下加口径说明（`config-hint`）
- 后端 `QUESTION_COUNT` 默认 5 → 8（原 5 方向 ≈ 7-8 轮对话，20 分钟场约 10 分钟即冷场）
- `buildInterviewerSystemPrompt` 明确写入题数口径 + "把时间用满，考察充分而非赶进度"

### 修复 3：出题结构升级（代码 + SQL 提示词）

- **四阶段段序约束**（替换原三段式）：开场自我介绍 → 简历深挖（每点最多 2 轮）→ 专业技术考察（本场主体，覆盖技术基础/项目实战/系统设计等类别）→ 反问收尾；附【轮换纪律】同一话题连续问答 ≤3 轮必须切换
- **`buildTurnDirective` 分阶段动态引导**：doneRounds 2-3 提示转入专业考察、≥4 提示方向轮换防恋战
- **Agent 人设升级**（DML init + 增量 `20260930-06`）：守则 2 由"考察充分后再换话题"（无上限）→ "同一话题追问 ≤2 轮 + 考察有层次（中段必须进入专业考察）"
- **warmup 配置行升级**：考察方向数由固定"3-5 个"→ 与计划问题数联动（3-15），并加类别覆盖约束（项目深挖 1-2 + 技术基础 1-2 + 岗位核心技能 1-2 + 系统设计 1 + 软素质 0-1），防方向全挤一类

### 四同步

| 项 | 内容 |
|---|---|
| 代码 | `VoiceInterviewServiceImpl`（4 处）、`InterviewSessionSupport.buildTurnDirective` |
| 前端 | `VoiceInterviewPage.vue`（选项/默认值/口径说明） |
| SQL | `moyun-db-dml-init.sql`（agent 人设 + warmup 种子）、增量 `20260930-06-面试官人设与warmup出题结构升级（v13.62）.sql`（幂等 UPDATE + 快照复核 + 备份建议） |
| 菜单 | 无新页面/字段，无需变更 |

### 校验

`mvn compile` ✅（产物时间戳已验证）；提示词改动无需重启（agent/场景配置运行时生效），已部署库执行增量 20260930-06。

## v13.61 (2026-09-30) 批次 4（四）Service 拆分 第③步：抽会话纯函数 → `InterviewSessionSupport`

**收口第③步**（承接第①②步的 `InterviewTextUtils` / `InterviewReportFormatter`）。
本步按"**只抽不依赖实例成员的方法**"原则，把会话编排里散落的**换算/拼装/映射**逻辑收口，
**不动**真正的编排主流程（SSE 生命周期、事务、锁、线程池）。

### 抽出内容（11 个方法 + 3 个常量 → 新类 `InterviewSessionSupport`，234 行）

| 分组 | 方法 |
|---|---|
| **Redis 键** | `qaTurnLockKey` / `analysisLockKey` / `hintCounterKey`（消灭散落的魔法前缀） |
| **时长换算** | `resolveDurationMinutes`（会话配置→全局→默认三级回落）/ `clampDuration` / `remainMinutesOf` / `isInterviewTimedOut` |
| **口头结束识别** | `matchesVerbalEnd`（`VERBAL_END_PATTERN` 常量随迁） |
| **配置解析** | `readConfigKey` |
| **指令拼装** | `buildTurnDirective`（签名改为 `(skip, doneRounds, remainMin)` —— 参数在调用点求值，去掉对实例方法的依赖）/ `appendPlanList` |
| **VO 映射** | `toQaVO` |

**保留为 Service 薄委托的 3 个**（因需注入实例依赖，但逻辑已下沉）：
`readConfigKey`（注入 `objectMapper`）、`resolveDurationMinutes`（注入 `sysConfigService`）、
`durationOf`（装配两级配置后委托）。

### 成果（三步累计）

| 指标 | 起点 | 现在 | 累计 |
|---|---|---|---|
| `VoiceInterviewServiceImpl` | **2943 行** | **2669 行** | **−274（−9.3%）** |
| 私有方法数 | 64 | ~50 | −14 |
| 支持类 | 0 | **3 个**（共 544 行） | +3 |
| 可单测纯函数 | 0 | **20+** | — |

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **469 例全绿** |
| 模块依赖守卫 | ✅ 已同步 `ext.cms -> portal` 278 → **280**（新类引用 2 个 portal 实体）并记录理由 |
| `npm run check`（vue-tsc） | ✅ 无类型错误 |
| admin `build:prod` | ✅ 成功 |

### ⚠️ 过程留痕（本轮一次失误）

批量删除时把 `sendEvent` 一并删掉，但其调用点遍布 SSE 链路 ⇒ 编译报 20+ 处「找不到符号」。
**已恢复**（该方法依赖 `log` 与 `SseEmitter`，属实例方法，本不该迁）。
教训：**批量删除前应按"是否被其它方法调用"再过一遍清单**，不能只看"是否 PURE"。


## v13.60 (2026-09-30) 前端模板结构守卫（补齐 v13.59 暴露的质量盲区）

v13.59 的缺陷（tab 重复/按钮截断/容器未闭合）**当时 vue-tsc 与 465 例后端测试全部放行**，
是靠"统计 tab 数量 = 6"偶然发现的。本次把该排查经验固化为 4 条窄而确定的断言：

| 守卫 | 钉死的契约 |
|---|---|
| tab 按钮值无重复 | 同一 tab 不得重复插入（v13.59：insight 出现两次） |
| 三层五 tab 数量 | `tab-content` 恰为 5；内容数 = 按钮数 + 1（对话回放无按钮） |
| 多行标签不截断 | `<button` 等行后必须跟属性行（v13.59：按钮属性全丢） |
| 容器闭合 | `report-tabs` 必须有同缩进闭合 `</div>`（v13.59：闭合被吞） |

**反事实验证**（关键）：临时注入一份重复 tab → 守卫**立即失败 2 例**；恢复后通过。
⇒ 证明守卫**有鉴别力**，不是"永远绿灯"的摆设。

**写守卫时踩到并修正的 3 处自身错误**（留痕）：
1. 用**去重后**的列表比较 size ⇒ 重复检测恒真（无效断言）；
2. 按**裸 `@click`** 识别 tab 按钮 ⇒ 把「查看逐题分析 →」跳转按钮误认为 tab；
3. 「逐行 + 向后 N 行窗口」找 `@click` ⇒ 窗口跨到下一按钮，产生假重复。
最终改为**按 `<button>...</button>` 元素整体解析**。

校验：`mvn -o test` **469 例全绿**（本次 +4）。


## v13.59 (2026-09-30) ⚠️ 修复我在批次 3 引入的报告页结构缺陷（重复 tab + 按钮截断 + 容器未闭合）

**性质**：这是**我在 v13.50（批次 3 重写发展方向 tab）引入的缺陷**，本次自查时发现并修复。
**暴露路径**：做进度盘点时统计 `tab-content` 数量，得到 **6**（应为 5）→ 顺藤摸瓜查出三处结构损坏。

### 缺陷详情（3 处，同一根因）

| # | 症状 | 根因 |
|---|---|---|
| 1 | **`insight` tab 重复**（两份：完整版 L2706-2809 + 旧占位 L3065-3086） | 重写时的**替换锚点匹配过宽** —— 正则匹配到了 `<button` 片段（因其后紧跟 `reportTab === 'insight'` 的类名），于是把"整块 insight tab"插进了**按钮区中间**，而**旧占位块未被删除** |
| 2 | **「🧭 发展方向」按钮被截断** | 同上：`<button` 行之后紧跟插入的注释块，按钮的 `:class` / `@click` / 文本行**全部丢失** |
| 3 | **`.report-tabs` 容器缺少闭合 `</div>`** | 同上：插入点吞掉了原本的闭合标签 |

**后果**：报告页 tab 区渲染异常（发展方向按钮缺失/错位）、`insight` tab 同时存在新旧两版
（旧版仍显示"即将开通"且按钮 `disabled`）。**注意 `vue-tsc -b` 与后端 465 例测试都不会报错**
—— Vue 模板对这类结构问题极其宽容，属"静默退化"。

### 修复

1. 取出**正确的批次 3 版本块**（104 行）；
2. 用**从块内标题行提取的 label**（`🧭 发展方向`）重建被截断的按钮
   —— 避免手打 emoji 在 PowerShell 中的编码坑；
3. 删除**旧占位块**；
4. 把正确块插入 `report-actions` 之前（其应有位置）；
5. 补齐 `.report-tabs` 的闭合 `</div>` 并**统一缩进**（div=8 空格，子元素=10，与兄弟元素一致）。

### 校验

| 项 | 结果 |
|---|---|
| `tab-content` 数量 | **6 → 5**（与五 tab 一致） |
| 截断的 `<button` | **0**（脚本逐个校验每个 `<button` 的下一行是否为属性行） |
| `insight` 出现位置 | **2 处 = 按钮 + tab 内容**（正确，非重复） |
| `.report-tabs` 缩进 | ✅ 与兄弟元素（`<div class="report-meta">`）一致 |
| `npm run check`（`vue-tsc -b`） | ✅ 无类型错误 |
| `mvn -o test` | ✅ **465 例全绿** |
| Portal 生产构建 | ⚠️ 既有 SEO 守卫缺 `VITE_SITE_URL`（历史问题，与本次无关） |

### ⚠️ 教训（写进本日志以警后人）

1. **替换锚点必须唯一且紧凑**：本次用了「能找到 `insight` 就算命中」的宽锚点，
   结果匹配到按钮片段。**正确做法：锚点应包含足以唯一确定位置的上下文，
   并在替换后校验"替换行数是否符合预期"**。
2. **替换后必须复核结构计数**：`tab-content` 从 5 变 6 是**唯一的外部信号**
   —— 若没做这个盘点，该缺陷会一直潜伏（编译/测试/类型检查全部通过）。
3. **前端结构缺陷不会被现有质量门拦下**：`vue-tsc` 只查类型，不查模板结构完整性。
   ⇒ 本项目**缺少"报告页结构守卫"**这类检查，建议后续补（可作为新的守卫测试方向）。


## v13.58 (2026-09-30) 时长制守卫补测（超时口径双向验证）

**背景**：批次 0 统一了超时口径（`SSE_TIMEOUT` 120s→210s），但**时长制守卫**
（`isInterviewTimedOut`：超过「配置时长 + 宽限」自动收口）同样没有测试。
该守卫若**过松**会放任超时面试继续烧 token；若**过严**会把正常面试误收口 ——
两个方向都是事故，故做**双向验证**。

### 新增 2 例（并入 `AnswerIdempotencyGuardTest`，该类现 7 例）

| 用例 | 方向 | 钉死的契约 |
|---|---|---|
| `closesInterviewAfterDurationAndGrace` | **过松检测** | 30 分钟前开面（阈值 = 20 + 宽限 2 = 22 分钟）→ 记 `timeout_close` + 会话收口为 `finished` + `closedReason=timeout` + **原始作答仍必须先落库**（超时也不丢数据） |
| `doesNotCloseBeforeDuration` | **过严检测** | 3 分钟前开面 → **不得**记 `timeout_close`、状态不得被改（否则正常面试被误收口） |

> **为什么"过严"也要测**：只测"超时会被收口"的守卫测试，无法发现
> "阈值算错导致正常面试被提前掐断"的回归 —— 而这恰恰是用户最敏感的一类 bug。

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **465 例全绿**（本次 +2） |


## v13.57 (2026-09-30) 答题幂等守卫补测 —— 补上「唯一会损坏数据」修复的测试

**为什么必须补**：批次 0 的 T2.1 是《报告七再评审》**R1** 的修复 ——
原实现「读后不锁」会导致 ①**答案后写覆盖前写** ②**双倍 token**
③**同一 `questionIdx` 插入两条主问**。这是本次整改中**唯一会损坏数据**的问题，
却**只有实现、没有测试**。DB 层虽已有唯一约束兜底（v13.53），
但应用层守卫若失效，用户仍会看到"该题已作答却又被覆盖"的错误行为。

### 新增 `AnswerIdempotencyGuardTest`（5 例）

| 用例 | 钉死的契约 |
|---|---|
| `rejectsWhenLockNotAcquired` | **守卫①分布式锁**：抢不到锁（=已有同题请求在飞）→ 抛「正在处理中」+ 记 `answer_dup_rejected` + **不得写入 `userAnswer`**（防答案覆盖） |
| `rejectsWhenAlreadyAnswered` | **守卫②状态机**：该题已作答 → 抛「该题已作答」+ **原作答不被覆盖** + **必须释放锁**（否则该题 TTL 内无法重试） |
| `allowsNormalSubmission` | **正常路径**：未作答 + 抢到锁 → 放行且 `userAnswer`/`answerRaw` 双写 + 记 `answer` 事件（**防守卫过严误杀**） |
| `rejectsEmptyAnswerBeforeLock` | 空答案在**取锁之前**就拒绝（不白占一次锁与事件） |
| `rejectsOtherUsersInterview` | 越权：非本人面试直接拒绝，不进入后续守卫 |

**测试环境设计**：两道守卫都在**提交 SSE 线程之前**抛出
⇒ 无需真实 SSE / Redis / 线程池，用 mock 的 `DistributedLockUtil` 精确控制
"抢到锁 / 抢不到锁"，事件通过 mock `eventMapper.insert` 捕获后断言。

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **463 例全绿**（本次 +5） |

> **测试价值实证**：`allowsNormalSubmission` 这类"正向路径"断言同样重要 ——
> 只有"拒绝"用例的守卫测试，无法发现"守卫过严把正常请求也拒了"的回归。


## v13.56 (2026-09-30) 报告契约集成测试 —— 为后续重构建立安全网（批次 4 四 / 先建网再动刀）

**动因**：批次 1（收编）/批次 2（扩字段）/批次 4（拆分）都在动报告链路，
但此前**没有任何测试覆盖「报告最终产出长什么样」**。后果是：字段静默丢失
（如 V1.3 发现的 `system_prompt_template` 废弃列问题）时，**编译与既有测试都不会报错**。

> **决策留痕**：批次 4（四）第③步（编排主流程拆分）**风险最高**，我建议**暂缓**，
> 先按"先建安全网再动刀"的方式补本测试 —— 否则是裸奔重构。

### 1) 为什么能隔离测试（关键发现）

`aggregateAndStoreReport`（187 行的报告聚合核心）**只依赖 4 个实例成员**：
`interviewMapper` / `qaMapper` / `scoringEngine` / `objectMapper`；
其余增强链（LLM 复盘、错题本、场景工作流）**全部自带 try-catch 降级**。
⇒ 纯 mock 环境即可驱动**确定性的规则兜底路径**，不碰 LLM / DB / Redis。

### 2) 新增 `InterviewReportContractTest`（5 例）

| 用例 | 钉死的契约 |
|---|---|
| `ruleBasedReportHasRequiredFields` | 顶层必需字段（interviewId/totalScore/questionReviews/dimensions/summary/improvementSuggestions）+ 逐题字段（questionIdx/question/**qaId**/userAnswer）——**qaId 缺失会让「加入错题本」按钮失效** |
| `dimensionsKeysAlignWithFrontend` | 六维 key **必须与前端 `DIMENSION_META` 一致**（`relevance/professionalism/fluency/interactivity/confidence/logic`）—— 历史缺陷正是旧 key（`coverage/length/structure`）导致雷达图断链 |
| `deepReviewAggregatesFromQaAnalysis` | 心态趋势/可疑信号/流畅度均分从逐题 `llm_analysis_json` 汇总（前端 deep-review 依赖）；含数值断言 `(60+70+80)/3 = 70` |
| `aggregateIsIdempotentWhenAlreadyDone` | `analysisStatus=2` 时**直接返回**：不重算、不覆盖已有报告（并发触发保护） |
| `degradesGracefullyWhenLlmUnavailable` | LLM 复盘不可用（依赖全 null）时**仍产出完整规则报告**（链路永不失败） |

### 3) 覆盖到的真实结构性缺陷（测试价值实证）

写测试过程中被测试自己抓出**我对链路的三处误判**：
1. 忘记 `totalQa` → `buildSummary` **NPE**（证明该字段是硬依赖，不能为 null）
2. 忘记 `rule_dimensions_json` → `dimensions` **为空**（暴露"六维聚合的唯一来源是逐题规则维度 JSON"这一事实）
3. 幂等用例的探测对象构造错误 → 暴露"守卫读的是自己 `selectById` 拿到的 fresh，与传入入参**不是同一对象**"这一实现细节

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **458 例全绿**（本次 +5） |

> **后续**：有了本安全网，批次 4（四）第③步（编排主流程拆分）才具备可控前提。
> 另：**真实 3 场冒烟的等价性验证**仍需环境配合（本测试覆盖的是**结构契约**，
> 不能替代真实链路观测 `ai_execute_log`）。


## v13.55 (2026-09-30) 批次 4（四）Service 拆分 第②步：抽报告纯格式化 → `InterviewReportFormatter`

**接续第①步**（`InterviewTextUtils`）。本步继续只抽**不读实例字段**的方法，
但按**关注点**再分一类：`InterviewTextUtils` 收「文本/JSON 通用处理」，
本类收「**报告语义的纯格式化**」（避免"文本工具"承担业务措辞）。

### 抽出内容（4 方法 → 新类 `com.moyun.ext.cms.support.InterviewReportFormatter`）

| 方法 | 说明 | 关键上限 |
|---|---|---|
| `parsePointViews(arr, setter)` | 亮点/薄弱点解析：**结构化视图回填 + 标题列表双写**（新旧字段兼容） | `MAX_POINT_VIEWS = 4` |
| `buildImprovementSuggestions(weak, intro)` | 规则兜底改进建议（薄弱点 → 自介不足 → 均衡文案三段降级） | 薄弱点 ≤3、总 ≤5 |
| `buildSummary(total, answered, avg)` | 概要文案三档（≥80 优秀 / ≥60 良好 / 其余一般） | — |
| `buildSuggestion(avg, weak)` | 总结建议（有薄弱点列出 / 否则正向文案） | — |

**同第①步口径**：全 static、无状态、无 IO、无实例依赖；逻辑**逐字迁移**（只把"方法"改为"静态方法"）。

### 成果（两步累计）

| 指标 | 起点 | 现在 | 累计 |
|---|---|---|---|
| `VoiceInterviewServiceImpl` 行数 | 2943 | **2785** | **−158** |
| 私有方法数 | 64 | 55 | −9 |
| 可单测纯函数 | 0 | **9**（2 个工具类） | +9 |

### 新增单测 `InterviewReportFormatterTest`（6 例）

覆盖**上限裁剪**（决定报告板块不爆版）与**空值降级**（决定报告永不空白）：
- `parsePointViews`：双写正确、**上限 4 条裁剪**、缺/空 title 跳过、**全无效时不回填视图**、null/非数组安全
- `buildImprovementSuggestions`：薄弱点 ≤3 裁掉第 4 个、自介不足补足到 5、**两者皆无时给正向文案（永不空白）**
- `buildSummary`：**三档边界**（80/60 两侧）与题数文案
- `buildSuggestion`：多点顿号连接、空/null **不 NPE**

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **453 例全绿**（本次 +6） |

### ⚠️ 过程留痕（两次测试失败与纠正）

本步写测试时**连续两处断言写错**，均被测试自己抓到：
1. 误以为「纯空格不足项」会被过滤 → 实测原实现用 `StringUtils.isNotEmpty`（**不 trim**）算有效项。
   **处理：修正断言为 2 条，并如实记录该既有小瑕疵**（会产出「自我介绍改进：  」这类无意义条目）
   —— 本步原则是"**只搬不改**"，不擅自修改报告产出行为。
2. 断言依赖列表顺序（`s.get(0)`）→ 改为 `anyMatch` 不依赖顺序。

> **后续步骤（未做）**：③ 才动编排主流程（含 SSE/事务/锁，最危险）与评分链路 —— 建议单独排期。


## v13.54 (2026-09-30) 批次 4（四）Service 拆分 第①步：抽无状态纯函数 → `InterviewTextUtils`

**背景**：`VoiceInterviewServiceImpl` 曾达 **2943 行 / 64 私有方法 / 24 依赖**，四职责
（编排 / 提示词装配 / 评分 / 报告解析）混杂。拆分按**风险从低到高**渐进，本批做**第①步**：
只抽**不读实例字段**的纯函数 —— 抽走后行为**逐字不变**、且**同时提升可测性**。

### 抽出内容（5 方法 + 1 常量 → 新类 `com.moyun.ext.cms.support.InterviewTextUtils`）

| 方法 | 依赖 | 说明 |
|---|---|---|
| `clamp(v,min,max)` | 无 | 数值钳制 |
| `truncateText(text,maxLen)` | 无 | 空白压缩 + 超长加省略号；null → 空串（免调用方判空） |
| `formatSkills(mapper,raw)` | 入参 mapper | 技能 JSON → 可读串；非法 JSON **原样返回不抛** |
| `extractJsonObject(mapper,raw)` | 入参 mapper | 容错提 JSON（剥围栏/杂文本），走 `LlmJsonExtractor` |
| `parsePredictedQuestions(arr)` | 无 | 追问预测解析 + 上限 6 条兜底 |
| `MAX_PREDICTED_QUESTIONS` | — | 常量随方法迁移 |

**设计要点**：需要 `ObjectMapper` 的方法由**调用方传入**，不在工具类内 new
—— 避免与 Spring 配置的序列化特性分叉。本类**只放纯函数**（全 static、无状态、无 IO）；
任何读实例字段或做远程 IO 的方法**留在 Service**，待后续步骤按职责抽取。

### 成果

| 指标 | 前 | 后 |
|---|---|---|
| `VoiceInterviewServiceImpl` 行数 | 2943 | **2852**（−91） |
| 私有方法数 | 64 | 59 |
| 可单测的纯函数 | 0（埋在 Service 内） | **5**（独立类） |

### 新增单测 `InterviewTextUtilsTest`（7 例）

重点覆盖**边界与降级**（根因是"LLM 输出不可信"）：
- `clamp`：区间内/上下越界/边界值
- `truncateText`：null→空串、空白压缩、截断加省略号、刚好等于上限不截断
- `formatSkills`：对象/纯字符串/无 level/**非法 JSON 原样返回**/null/空
- `extractJsonObject`：裸 JSON、**markdown 围栏**、前后杂文本、无法解析返回 null
- `parsePredictedQuestions`：字段解析、askedThisRound 分组、**空 question 跳过**、
  **上限 6 条兜底**、缺失/非数组/元素非对象**一律安全降级为空列表（永不为 null）**

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **447 例全绿**（本次 +7） |

### ⚠️ 过程留痕（一次失败与纠正）

首次抽取我用「按方法签名文本切割」的方式，**吞掉了相邻的 `MAX_HINT_*` 常量并导致缩进整体左移**，
编译报大量"找不到符号"。处理：**`git checkout` 恢复文件**（保住已提交成果），
改用**精确行号范围**删除（从后往前，避免行号漂移），一次通过。
**教训：批量删改 Java 源码不要按文本模式切割 —— 大括号/注释边界不可靠，要用行号并复核行数差。**

> **后续步骤（未做）**：② 抽评分与报告聚合（有明确输入输出）；③ 才动编排主流程（含 SSE/事务/锁，最危险）。


## v13.53 (2026-09-30) 批次 4（三）问答表唯一约束 —— 清偿「同题号插两条主问」

**背景**：《报告七再评审》**R1** 指出 —— 答题链原先无并发保护，双发同一 `qaId` 会导致
**同一 `questionIdx` 插入两条主问（无任何 DB 约束）**。批次 0 已从应用层加了两道防线
（答题幂等分布式锁 + 建下题前判重），本批补上**最终一道 DB 层防线**：
即使应用层守卫被绕过（并发、锁 TTL 到期、直连写库），数据库也会拒绝重复主问。

### ⚠️ 为什么不能直接加唯一索引（关键：先实测再设计）

追问（follow-up）与主问**共享同一 `question_idx`**，仅靠 `parent_qa_id` 区分。
我先在临时库做了**语义实测**：

| 方案 | 实测结果 |
|---|---|
| `UNIQUE(interview_id, question_idx, parent_qa_id)` | ❌ **拦不住重复主问** —— MySQL 唯一索引**对 NULL 不去重**，两条 `parent_qa_id=NULL` 可共存 |
| 含 `parent_qa_id` 的普通唯一键（同上） | ❌ 同上：主问完全不受约束 |
| **STORED 生成列 + 唯一索引** | ✅ 重复主问**被拒**；同题号不同 parent 的追问**正常允许** |

### ✅ 最终方案

```sql
`uk_qa` varchar(48) GENERATED ALWAYS AS (
  concat(`interview_id`, ':', `question_idx`, ':', ifnull(`parent_qa_id`, 0))
) STORED
UNIQUE KEY `uk_qa_main` (`uk_qa`)
```

**实测行为**（在真实表上、事务内验证后回滚，无残留）：

| 场景 | 结果 |
|---|---|
| 插入第一条主问（`parent=NULL`） | ✅ 成功 |
| 同题号插入两个不同 `parent` 的追问 | ✅ 均成功（**未误伤追问**） |
| 同题号插入**第二条主问** | ✅ **被拒** `Duplicate entry '999902:5:0' for key 'uk_qa_main'` |

**额外收益**：同一 `(会话, 题号, parent)` 的重复追问也会被拒。

### 交付

| 项 | 说明 |
|---|---|
| `moyun-db-ddl.sql` | QA 表加生成列 `uk_qa` + 唯一索引 `uk_qa_main` |
| 增量 `20260930-05-面试问答表唯一约束（批次4三）.sql` | `information_schema` 预检 + `PREPARE/EXECUTE`，**幂等**（run2 正确输出"已存在，跳过"）；含**前置体检 SQL**（暴露既有重复主问，避免加约束时才失败）与回滚说明 |
| live `moyun-db2` | ✅ 已应用（生成列 1 / 唯一索引 1 / 重复主问 0） |

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **440 例全绿** |
| 增量守卫 | ✅ 通过；连跑两次 exit 0 |
| 全新库 | ✅ 三文件全绿；**185 表**；生成列 1 / 唯一索引 1 |
| 约束行为实测 | ✅ 重复主问被拒 · 追问不误伤 · 测试数据零残留 |


## v13.52 (2026-09-30) 批次 4（二）统一 LLM 调用端口 —— 审计确认 + 守卫固化

**结论先行**：批次 1 的 3 处收编完成后，「统一 LLM 端口」目标**已事实达成**，
本批**无需改代码**，改为**审计确认 + 用守卫测试固化**，防止后人重引直连。

### 1) 全仓审计结果

| 入口 | 文件数 | 调用点 | 是否走网关 |
|---|---|---|---|
| `AiSceneJsonClient#executeForJson` | 9 | 15 | ✅ 同步/JSON 任务型场景唯一入口 |
| `AiGatewayService#executeConversationStream` | 3 | 3 | ✅ 会话流式唯一入口 |
| `AiGatewayService#execute` | 1 | 1 | ✅ 网关本体 |
| `agentClient.chat(` | 1 | 3 | ✅ **仅注释提及**（批次 1 收编说明），**代码零调用** |

并核实 `executeConversationStream` **确实经过治理三件套**：
`resolveSessionConfig`（按首轮锁定版本读快照）+ `SceneRateLimiter`（限流）+ `TokenCostGuard`（成本熔断）。

### 2) 新增守卫测试 `LlmCallPortGuardTest`（3 例）

| 用例 | 钉死的契约 |
|---|---|
| `noDirectModelChatInBusinessCode` | 扫描 `ext/cms` 与 `portal` 全部 Java 源码，**剥离注释后**不得出现 `agentClient.chat(`；违规时打印 `文件:行号` |
| `gatewayEntryPointsExist` | 两个网关入口方法名不得被改名/删除；且会话流式**必须**经 `resolveSessionConfig` / `rateLimiter` / `tokenCostGuard`（否则"走网关"名不副实） |
| `stripCommentsWorks` | **自我验证**：确认注释剥离器真能剔除行注释与块注释中的调用、并保留真实调用 —— 防止守卫被注释骗过或产生误报 |

> **为什么这类守卫值得写**：绕过网关**编译不报错、既有测试不失败**（静默退化：
> 该次调用不进 `ai_execute_log`、不受限流与成本熔断、不走版本锁）。
> 只靠代码评审维持这种约束是脆弱的，故钉成可回归断言。
> 守卫自带**自我验证用例**（`stripCommentsWorks`），避免"守卫本身有 bug 却一直绿灯"。

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **440 例全绿**（本次 +3） |


## v13.51 (2026-09-30) 批次 4（一）场景配置双下拉 —— 清偿报告七 P0-4「子场景只能靠 SQL 维护」

**背景**：场景代码支持两段式 `主场景:子任务`（如 `voice_interview:warmup`），
但管理端原先只有**一个整串下拉**，且后端 `validate()` 用 `AiSceneEnum.of()` 做**整串精确匹配**
⇒ 合法的 `main:task` 子场景**根本存不了**，只能靠 SQL 维护。
后果就是批次 1 落库的 4 行新配置在管理端「**看得见、改不了**」。

### 1) 后端

| 变更 | 说明 |
|---|---|
| **`AiSceneTasks.all()` / `isValid()`** ★新增 | task 短码**白名单**，由本类 `public static final String` 常量**反射派生** ⇒ 新增子任务只需加常量，白名单自动跟随（不会再出现"加了常量但校验不认"的漂移） |
| **`validate()` 改两段式** | 有 `:` 时拆两段：主场景校验 `AiSceneEnum`、task 校验 `AiSceneTasks`；并拒绝三段式与空段 |
| **`sceneName` 派生口径** | 子场景名 = 枚举名 + `·子任务`（父名称仍来自注册表，不脱离登记制） |
| **`GET /cms/ai/scene/tasks`** ★新增 | 返回 `{ tasks: [...], scenes: {code: name} }`，供双下拉第二级使用 |

### 2) 管理端（`views/ai/scene/index.vue`）

- 「场景代码」由单下拉改为 **`主场景` : `子任务` 双下拉**（子任务可清空 = 主场景本身）；
- 表单下方**实时显示将要提交的完整代码**（`formSceneCode`），避免用户猜；
- **编辑态两个下拉均 disabled**（沿用原「场景代码不可改」口径，防止误改导致绑定失效）；
- `handleEdit` 把既有 `sceneCode` 按 `:` 拆开回填；`handleAdd` 复位；
- `submitForm` 改为**以双下拉为准**生成 `sceneCode` / `sceneName`；
- `/tasks` 加载失败降级为空数组（仅影响"可选性"，不阻塞页面）。

### 3) 新增守卫测试 `AiSceneTasksTest`（3 例）

| 用例 | 钉死的契约 |
|---|---|
| `allContainsEveryConstant` | 白名单必须**恰好等于**常量个数（既不漏，也不混入非常量值） |
| `isValidGuardsWhitelist` | 批次 1 新增的 4 个面试族 task 必须在内；未注册/空串/null 必须拒绝 |
| `twoSegmentSplitContract` | 复刻 `validate()` 的两段式判定，防止口径漂移；三段式应被拒绝 |

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **437 例全绿**（新增 3 例；原 434） |
| `npm run build:prod`（admin） | ✅ 成功 |
| 后端编译 | ✅ BUILD SUCCESS |

> **收益**：批次 1 落库的 4 行面试族配置（`:opening_fallback` / `:hint` / `:report_review` / `:industry_insight`）
> 以及既有的 12 行 `main:task` 配置，**现在都能在管理端正常编辑**，不再依赖手写 SQL
> —— 同时消除了「40 列超宽 INSERT 手写易错」这一实际踩过的坑。


## v13.50 (2026-09-30) 批次 3：发展方向 tab（懒生成 + 分布式锁幂等 + 个人化锚定）

**依据**：方案 V1.2 §5.5 / §6.2 / §6.3。**至此方案批次 0/1/2/3 全部实施**（批次 4 为中长线）。

### 1) 后端：`POST /portal/interview/voice/{id}/insight`

| 设计点 | 实现 |
|---|---|
| **懒生成** | 只在用户点按钮时调用，**不在报告主链路**（V1.2 §1 原则 3「成本按需发生」）—— 用户从不看该 tab 则为零成本 |
| **幂等（防双击双花）** | 分布式锁 `voice:insight:{id}`（TTL 3 分钟）+ 锁内双检；已生成且未满 **7 天**直接返回缓存（`INSIGHT_CACHE_TTL_MS`） |
| **强制刷新** | `?refresh=true` 忽略缓存重算（前端「🔄 刷新」按钮） |
| **输入差异化（V1.1#3）** | 岗位 + JD + **简历技能** + **本场报告上下文**（薄弱点 title 列表 / `levelEstimate` / 雷达中 < 70 的低分维度）—— 使每条建议锚定真实短板，与 `resume_optimize:job_match` 划清边界 |
| **通道** | 走场景配置行 `voice_interview:industry_insight`（网关：限流/成本熔断/`ai_execute_log` 全走这里） |
| **落库** | 结果写入报告 JSON 的 `industryInsight` 字段（报告是整段 JSON 存储 → 新增字段自动持久化） |

**限流**：端点 `@RateLimiter(key="voice:insight", 3600, 20)`。

### 2) VO 扩展：`IndustryInsightView`

```
IndustryInsightView { generatedAt, trends[], supplyDemand{existing[],missing[]}, actions[] }
TrendView           { title, detail, maturity }
SupplyDemandView    { existing[], missing[] }
ActionView          { content, relatedWeakPoint }
```

### 3) 前端：tab「🧭 发展方向」从占位 → 完整渲染

- **未生成**：占位卡（「约需 10 秒」+「开始生成」按钮，**不自动触发**）；
- **已生成**：
  - ① 技术趋势卡片（标题 + **成熟度徽章**：成熟期=绿 / 上升期=蓝 / 早期=灰 + 说明）；
  - ② 技能供需结构（`✅ 已具备` 绿标 / `⚠️ 建议补充` 黄标 标签云；简历无技能时提示"未提取到技能标签"）；
  - ③ 行动建议（序号卡 + **「↳ 对应本场薄弱点：…」锚点关联**，落实 V1.1#3 的强制引用）；
- **头部元信息**：`生成于 X 月 X 日` + 超 7 天提示「建议刷新」+「🔄 刷新」按钮；
- **口径诚实声明**（固定在底部）：「本分析为方向性判断（基于你本场的简历、岗位与表现），**不包含实时行业数据**」。

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **434 例全绿** |
| `npm run check`（`vue-tsc -b`） | ✅ 无类型错误 |
| 场景配置行 | ✅ `voice_interview:industry_insight` 已存在且 `enabled=1`（批次 1 落库） |

> **实施记录**：重写 insight tab 时我的「收尾 `</div>` 探测」命中过早（只替换 8 行），
> 复核时发现并改为完整替换（8 行 → 104 行）。**教训：批量替换后必须复核替换行数**，
> 本次正是靠"替换行数异常"这一信号发现的。


## v13.49 (2026-09-30) 批次 2（前端·续）概要四区重排 + 问题分析维度聚类 + 方案升 V1.3

**依据**：方案 V1.2 §6.2 逐 tab 内容契约（本批补齐批次 2 剩余两项）。

### 1) 概要 tab 四区重排（原 11 板块无主次 → 有层次的四区）

```
A 首屏（结论）  总分大数字 + 六维雷达 + 匹配度 + 定级徽章 + 维度速览 chip
B 叙事区        整场总评 + 匹配依据 + 亮点
C 行动区        薄弱点 + 改进建议（**相邻** —— 问题挨着答案）
D 归档区        折叠：自我介绍评分 + 过程信号（心态趋势/流畅度/可疑信号）
```

- **设计意图**：原布局把「自介分/心态趋势/可疑信号」等**资料类**内容与结论平级铺开
  ⇒ 首屏被资料占满、结论要滚动才见。现按「结论 → 叙事 → 行动 → 资料」降权排序，
  资料类默认折叠（`archiveOpen=false`）；
- **行动区把薄弱点与建议放在同一栏相邻**：用户看完"哪不行"紧接着就是"怎么补"；
- 首屏新增**六维速览 chip**（维度名 + 数值），弥补雷达图读数不精确；
- **`candidate` / `jobInfo` 未纳入归档**：核实后发现这两个字段**从未在模板渲染**
  （仅 `jobInfo.matchRate` 被 `jobMatchRate` 作旧报告回退读取）⇒ 放进归档会是空区块，
  故归档只放**实际有数据**的内容（自介分 + 过程信号）；
- 窄屏（≤900px）回落单栏。

### 2) 问题分析：按维度聚类（诚实口径）

**先澄清一个数据事实**：后端**不产出逐题维度分**（`questionReviews` 只有总分与点评文本），
故**不能**做「把某题归到某维度」的映射 —— 那是无依据的编造。

实际落地为「**归因锚点 + 低分题置顶**」：

- 顶部归因横幅显示**最低维度**（雷达六维最低者，含数值）+ 低于 80 分的题数；
- 逐题列表改为按**分数升序**（低分置顶），空分排最后，便于优先复盘；
- 文案明确写出「已置顶按分数升序排列」，不暗示虚假的题目↔维度关联。

### 3) 方案升版 V1.2 → **V1.3**（3 处修正，来自实施验证）

| # | 修正 | 理由 |
|---|---|---|
| **1** | §3.3/§4「系统提示词与 output_schema 全进配置行」**未指明列归属** → 明确：**`system_prompt_template` 是废弃列**，任务指令与字段规范必须全部进 `user_prompt_template` | `DefaultSceneExecutor#execute` 组装为 `mergePersona(request, DEFAULT_SYSTEM_PROMPT) + schemaConstraint`，**不读该列**。照原文实施会**静默丢失**提示词（日志无异常、模型收不到约束）——这是本方案自身的缺陷 |
| **2** | §3.2「perQuestion 瘦身」补说明：**总分均分计算口径必须同步改** | 原口径全量求和；瘦身后只回填部分题 ⇒ 均分被"仅被修正的几题"代表，**统计失真** |
| **3** | §3.3 补 `output_parser` 取值语义：纯文本=`text`（`output_schema` NULL）、JSON=`json` | 查库确认 `default_chat`（对话主链）=`text`；若给纯文本任务配 JSON schema 会误导执行器 |

同时更新 §8 批次表实施状态（批次 0 ✅ v13.44 / 批次 1 ✅ v13.45~46 / 批次 2 ✅ v13.47~49）。

### 校验

| 项 | 结果 |
|---|---|
| `npm run check`（`vue-tsc -b`） | ✅ **无类型错误** |
| tab-content 数量 | ✅ 5（与五 tab 一致） |
| Vue 文件规模 | 3982 行 |
| 文档链接自查 | ✅ **0 失效** |
| `candidate`/`jobInfo` 归档决策 | ✅ 已核实「从未渲染」故不纳入（避免空区块） |

> **修正记录**：实施中我犯过 1 处类型错误（把 `reportScores` 当 Record 按 key 索引，
> 实为 number[] 与 `DIMENSION_META` 同序）—— 已改为索引访问并复检通过。


## v13.48 (2026-09-30) 批次 2（前端）：报告页三层五 tab 重排 + 新增「🔮 追问预测」tab

**依据**：方案 V1.2 §6.1/§6.2/§6.3（信息架构重排 + 逐 tab 内容契约）。

### 1) 三层五 tab 信息架构

```
第一层【复盘区】这场我打得怎么样（向后看）
  📋 面试概要（结论）→ 🔍 问题分析（归因）→ 💬 对话回放（证据，殿后）
第二层【备战区·个人】下次真面试会怎样
  🔮 追问预测
第三层【备战区·环境】我该往哪走
  🧭 发展方向
主线：表现 → 归因 → 证据 → 个人预测 → 环境导航
```

- **回放从第 2 位移到最后**：回放是**原始证据**不是消费内容（用户只在对某题不服时才翻原文）；
  证据层的正确姿势是**被引用**，不是排在第 2 位挡道；
- tab 栏新增 `复盘` / `备战` **分组标签**（细竖线分隔），把层次关系显式化；
- `reportTab` 类型：`'summary' | 'dialog' | 'analysis'` → `'summary' | 'analysis' | 'predict' | 'insight' | 'dialog'`。

### 2) 新增「🔮 追问预测」tab（本批主体）

**双分组**（数据源 = 批次 2 后端新增的 `report.predictedQuestions`）：

| 分组 | 视角 | 内容 |
|---|---|---|
| **A ✅ 本次已问** | 复盘（答得对不对） | 问题 + **你的得分**（按 `scoreClass` 着色）+ 考点标签 + 要点参考 + 提问动机 |
| **B ⭐ 未被问到** | **预警（下次押题清单）★核心价值** | 问题 + 考点标签 + 要点参考 + 提问动机；卡片**视觉加重**（warning 边框/底色）以突出价值 |

- 顶部导语明确「**未被问到的部分是下次面试的重点准备方向**」；
- **整 tab 缺失即隐藏**：`showPredictTab` 依据 `predictedQuestions` 是否非空，
  tab 按钮用 `v-if` 控制（对齐 V1.2 §6.3「字段为空按缺失隐藏」惯例，无简历场次同样适用）。

### 3) 新增「🧭 发展方向」tab（占位形态）

- 本批只交付**占位卡**（`即将开通` 按钮 disabled + 定位说明），**不自动触发**任何调用；
- 口径诚实写在 UI 上：「约需 10 秒 · 含技术趋势/技能供需结构/3 条针对本场薄弱点的行动建议」；
- 生成接口（`POST /{id}/insight`）+ Redis 锁幂等 + 内容渲染在**批次 3** 交付。

### 4) 水平定级徽章（概要 tab）

- `report.levelEstimate` → 概要横幅「综合得分」旁新增 `定级 · 初级/中级/高级` 徽章
  （按 junior/mid/senior 三色 + `title` 悬浮说明「基础需夯实 / 框架完整，深度待补 / 具备体系化思维」）；
- 缺失或非法值**不展示**（`levelBadge` computed 返回 null）。

### 校验

| 项 | 结果 |
|---|---|
| `npm run check`（`vue-tsc -b`） | ✅ **无类型错误** |
| 前端类型同步 | ✅ `PredictedQuestionView` + `levelEstimate` 已加入 `api/voiceInterview.ts` |
| Portal 生产构建 | ⚠️ 因**既有 SEO 守卫**缺 `VITE_SITE_URL` 失败（历史已知问题，与本次改动无关） |

> **本批未做（留待后续，避免单批过大）**：
> ① 概要 tab 四区瘦身（首屏/叙事/行动/归档折叠）；
> ② 问题分析「按维度聚类」视图 + 跳转回放锚点；
> ③ 批次 3 的发展方向生成能力。


## v13.47 (2026-09-30) 批次 2（后端）：复盘 prompt 扩字段 + VO 扩 levelEstimate/predictedQuestions + perQuestion 瘦身

**依据**：方案 V1.2 §3.2 / §5.4 / §0.1#3。本批只做**后端契约**，前端三层五 tab 重排随后。

### 1) `report_review` 提示词升级（init-sql + 增量 `20260930-02` 双轨）

| 字段 | 变更 |
|---|---|
| `levelEstimate` ★新增 | 从「拼进 summary 文本」改为**结构化输出**（junior/mid/senior），供概要 tab 定级徽章与发展方向 tab 个人化锚点 |
| `predictedQuestions` ★新增 | **上限 6 条**，每条 `{question, briefAnswer≤80字, analysis≤60字, knowledgePoint, askedThisRound, askedScore}`；含 4 条硬约束（只能基于简历真实内容/禁编造/以对话记录判定是否已问/优先覆盖未深挖与差距项） |
| `perQuestion` **瘦身** | 由「逐题全量 `{questionIdx,score,comment}`」改为「**仅回填需修正的题目** `{questionIdx,score}`」；逐题点评文本复用 `answer_analysis` 已落库结果（V1.2#2 输出防爆） |

### 2) VO 扩展（`VoiceInterviewReportVO`）

- 新增 `levelEstimate`（String）
- 新增 `predictedQuestions`（`List<PredictedQuestionView>`）+ 内部类 `PredictedQuestionView`
  （6 字段，`askedThisRound=true` → 前端分组 A「本次已问」，否则分组 B「未被问到」）

### 3) 解析实现（`enhanceReportByAgent`）

- **取值校验**：`levelEstimate` 仅接受 `junior/mid/senior`；不合法**不覆盖**
  （保留聚合流程已写入的基础定级）——避免模型输出中文或其它值时脏值进前端；
- **新增 `parsePredictedQuestions`**：解析失败/空数组返回空列表 → 不设字段 → 前端**整 tab 隐藏**
  （对齐「字段为空按缺失隐藏」惯例，报告其余部分照常）；代码再次兜底 6 条上限；
- **⚠️ 总分口径修正（瘦身的连带影响）**：原实现把 `perQuestion` 分数**全量求和**算均分；
  瘦身后只回填部分题 ⇒ 若沿用原口径，均分会被「仅被修正的那几题」代表，**属统计失真**。
  现改为：**在全部已作答题目的现有分数上应用修正，再求均分**
  （遍历 `report.getQuestionReviews()` 中 `score != null` 的项）。

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **434 例全绿** |
| 增量 `20260930-02` 幂等 | ✅ 守卫通过；连跑两次 exit 0 |
| live `moyun-db2` | ✅ `tpl=2306`、含 `predictedQuestions` 与 `levelEstimate`、`enabled=1` |
| init-sql 4 个 INSERT 块列数 | ✅ 40/40 异常 0 |

> **待办**：批次 2 前端（概要瘦身四区 / 问题分析维度聚类 / 回放殿后 / 新增「🔮 追问预测」tab 双分组）。
> 原计划「布局重排」为纯前端工作，随后开展。


## v13.46 (2026-09-30) 批次 1（三）3 处直连收编：面试链 LLM 调用 100% 走网关

**目标**：消灭面试链残留的 3 处「直连模型」调用，使 `ai_execute_log` 可按 task 全码留痕（成本可观测的前提）。

### 收编成果

| # | 原调用点 | 收编后 | 说明 |
|---|---|---|---|
| T3.5 | `generateOpening`（`agentClient.chat` + 代码拼提示词） | `voice_interview:opening_fallback` | 纯文本（`parser=text`）；V1.1#1「收编不删除」 |
| T3.6 | `requestHint`（`agentClient.chat` + 滑窗拼装） | `voice_interview:hint` | 纯文本；计费=免费，两级配额（每题3/全场15） |
| T3.7 | `enhanceReportByAgent`（硬编码系统提示词 + 直连 + 手写重试） | `voice_interview:report_review` | JSON（`parser=json`）；重试由网关 `chatJsonOutcome` 统一提供 |

**验收**：`VoiceInterviewServiceImpl` 中 `agentClient.chat` **代码零命中**（仅剩 4 处注释说明）；
面试链全部 LLM 入口收敛为 `aiSceneJsonClient.executeForJson`（配置驱动）+ `aiGatewayService.executeConversationStream`（会话流式）。

### ⚠️ 架构发现（推翻了本方案最初的迁移写法）

**`ai_scene_config.system_prompt_template` 是废弃列** —— `DefaultSceneExecutor` 的系统提示词组装为：

```
systemPrompt = mergePersona(request, DEFAULT_SYSTEM_PROMPT) + schemaConstraint(meta)
             = input.agentPersona（Agent 表人设）  + 最小默认提示词 + （有 output_schema 时追加约束）
```

即**人设由 Agent 表承载，任务指令与字段规范必须全部进 `user_prompt_template`**。
本方案 V1.2 §4 描述"整体迁 `:report_review` 配置行"时未指明列归属，
我最初把报告字段规范写进了 `system_prompt_template` ⇒ **会静默丢失**（该列不参与组装）。
已改为全部进 `user_prompt_template`，并在配置行 `description` 中留档该约定。

### ⚠️ 保留原提示词结构（等价性优先）

`report_review` 的提示词**逐字保留原实现的 7 字段结构**（含
`perQuestion{questionIdx,score,comment}`、`jobMatch{rate,reason}`）——
因当前解析代码正是按此结构读取，若同时引入 `levelEstimate` 结构化与
`predictedQuestions`（那是 V1.2 §5.4 / 批次 2 的范围）会破坏等价性。
批次 1 只做「**通道迁移**」，不改输出契约。

### 同步更新

- **测试**：`TransactionRemoteIoRuntimeProbeTest#voiceInterviewStartRemoteIoOutsideDbWriteInside`
  原本打桩 `agentClient.chat` 并断言其被调用；收编后该打桩失效。
  已改为按 `input.task` 区分打桩：`warmup` 返回 null（触发降级）→ `opening_fallback` 返回开场白，
  断言改为「网关被调用」且**仍在事务外**（测试职责是验证事务边界，与走哪条 LLM 通道无关）。
- **清理**：移除已无用的 `SystemMessage` 导入（该类内不再直接构造系统消息）。
- **SQL 双轨**：`moyun-db-dml-init.sql` 4 行 `enabled=0 → 1`；
  新增增量 `20260930-03-面试族4行场景配置（批次1收编配套）.sql`（4 行 `INSERT ... WHERE NOT EXISTS`，
  `agent_id` 用 `name` 子查询，幂等）。

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o test` | ✅ **434 例全绿**（含幂等守卫、事务边界守卫） |
| 全新库（DDL+DML+menu-redo） | ✅ 三文件全绿；8 行面试族配置全 `enabled=1`；悬空 agent=0；表数 185 |
| 增量脚本幂等 | ✅ 连跑两次 exit 0 |
| live `moyun-db2` | ✅ 4 行齐备、全 `enabled=1`、悬空 agent=0 |
| 面试 Service 直连残留 | ✅ `agentClient.chat` 代码零命中 |

> **待办**：等价性冒烟验证（收编前后各 3 场，对比 `ai_execute_log` 输出结构）需真实开面；
> 待环境具备时执行。批次 2（追问预测 tab + 三层五 tab 重排）随后开展。


## v13.45 (2026-09-30) 批次 1（二）4 行面试族配置落库（提示词自代码迁入配置行）

**依据**：方案 V1.2 §3.3/§4。把 3 处**直连代码**的提示词迁进配置行，为「批次 1（三）收编」准备配置。

### 新增 4 行 `ai_scene_config`（与 init-sql 双轨）

| scene_code | parser | max_tokens | timeout | enabled | 说明 |
|---|---|---|---|---|---|
| `voice_interview:opening_fallback` | `text` | 512 | 60 | **0** | 自 `generateOpening` 迁移（V1.1#1 收编不删除） |
| `voice_interview:hint` | `text` | 128 | 30 | **0** | 自 `requestHint` 迁移（计费口径=免费，两级配额每题3/全场15） |
| `voice_interview:report_review` | `json` | 2560 | 180 | **0** | 自 `enhanceReportByAgent` 迁移，含 `levelEstimate` 结构化 + 追问预测 |
| `voice_interview:industry_insight` | `json` | 1536 | 120 | **0** | 懒生成；输入含本场报告上下文；预留 RAG 通道 |

- **`agent_id` 一律用子查询** `(SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1)`
  —— 沿用 v13.38 修复的写法，防双库 id 分叉再次悬空；
- **`enabled=0`**：本批只落配置**不改行为**；待「批次 1（三）收编」时随代码一起置 1，保证可回滚；
- `remark`/`description` 留档迁移来源与裁决理由（V1.1#1、V1.2 §3.2）。

### ⚠️ 过程中发现并修正的 3 个自身错误（留痕）

| # | 错误 | 症状 | 修正 |
|---|---|---|---|
| 1 | **字段与列清单错位** | `INSERT` 报 `Column count doesn't match`（39 vs 40） | 逐字段打印定位：`output_schema` 被写成 parser 值、**末尾漏 `deleted`**（把 `NOW()` 放在 `open_api` 导致尾部少一值）。按权威列清单（DDL 41 列 − id = 40）逐行对齐 |
| 2 | **`output_parser` 语义用错** | 纯文本任务带了 JSON `output_schema` | 查库中现有取值确认语义：`default_chat`=**`text`**、JSON 场景=`json`。`opening_fallback`/`hint` 改为 `output_schema=NULL` + `output_parser='text'` |
| 3 | **`prompt_placeholders` 写成纯文本** | `ERROR 3140 Invalid JSON text ... for column 'prompt_placeholders'` | 该列是 **JSON 列**，补为合法 JSON 对象（含 `resumeDigest`/`position`/`jd`/`qaList` 等键） |

> **教训**：40 列的超宽 `INSERT` **不能靠肉眼或文本替换**维护。本次为此专门写了一个
> **SQL 感知的字段计数器**（引号/括号状态机 + 顶层逗号切分），逐行比对列数后才定位到根因。
> 建议后续新增场景行沿用该检查方式（已可作为批次 4「场景配置 UI 双下拉」的动因之一）。

### 校验

| 项 | 结果 |
|---|---|
| 全新库（DDL + DML + menu-redo） | ✅ **三文件全绿**（此前 DML 报 ERROR 3140） |
| 4 个 `INSERT` 块逐行列数 | ✅ **40/40，异常行 = 0** |
| voice_interview 族行数 | 4 → **8**（主码 + 7 task） |
| 场景总数 | 20 → **24** |
| 悬空 agent 引用 | ✅ **0** |
| 表数 | ✅ **185**（与基线一致） |
| `default_chat` / `question_generate:jd_keywords` | ✅ 各 1 行 |

> **待办（批次 1 三）**：4 行配置的 `enabled` 置 1 + 3 处直连收编（`generateOpening` L673 / `requestHint` L1016 / `enhanceReportByAgent` L1648+1654）+ 等价性验证（收编前后各 3 场冒烟对比 `ai_execute_log` 输出结构）。


## v13.44 (2026-09-30) 批次 0「修地基」：面试链并发保护 / 超时统一 / 断连中止 / 事务 / 自介接线 / hint 配额 / 多实例

**背景**：方案 V1.2 §8 新增的**批次 0**。依据《全端-评审-报告七再评审-20260930》的 **R1~R6 六项运行期隐患** ——
V1.2 是「加功能 + 收口入口」，但它跑在一条**无并发保护、无事务、断连不收 token** 的链路上，
故**先修漏斗再加楼层**。全部改动集中在 `VoiceInterviewServiceImpl`（单文件）。

### T2.1 答题并发保护（R1 —— 唯一会损坏数据的问题）

**原缺陷**：`submitAnswer` 唯一守卫是 `status=finished` 判断且**读后不锁**，双标签页/连点/重试会同时通过校验 ⇒
① 两次都写 `userAnswer`（**后写覆盖前写**）；② 两次都提交线程（**双倍 token**）；
③ 两边各自 `insert(nextQa)` 且 `questionIdx` 相同（**同一 idx 插两条 QA**，无唯一约束）。

**修法**（两级）：
- **入口幂等**：`DistributedLockUtil.tryLock("voice:qa-turn:{qaId}")`（非阻塞，TTL 5 分钟），
  抢不到即抛「该题正在处理中，请勿重复提交」并记 `answer_dup_rejected` 事件；轮次结束（成功/异常）在 `finally` 释放；
- **状态机守卫**：`userAnswer` 非空即拒绝重复消费同一 QA（抛「该题已作答，请回答当前题目」）；
- **落库判重**：建下一题前查「同 `interviewId` + 同 `questionIdx` + `parentQaId IS NULL`」是否已存在，
  存在则跳过并记 `next_dup_skipped`（锁 TTL 到期后重放的兜底）。

### T2.2 超时口径统一（R3）

`SSE_TIMEOUT` **120s → 210s**。原值**小于** `ai_model_config.timeout` 种子值 180s ⇒
模型还在推理、SSE 已超时断开（用户看到超时，服务端继续烧 token）。
现取 **210s = 模型 180s + 30s 缓冲**，保证 SSE 超时一定是模型真超时后的兜底；并在注释写明**联动关系**。

### T2.3 断连中止 LLM（R4）

**原缺陷**：只有 `onTimeout`/`onError` 且**只打日志**，全仓**无 `onCompletion`** ⇒
客户端关页面后上游流式订阅不被取消，`onCompleteResponse` 照常执行：
**继续耗 token、继续 `tokenCostGuard.consume`、继续写滑窗与执行日志**。

**修法**：注册 `onCompletion` 置 `AtomicBoolean clientGone`；`onToken` 检测到标记即停止下发；
`onComplete` 检测到标记则**跳建下一题**（不为已离开的用户造题）并记 `turn_aborted`。

### T2.4 事务边界（R2）

本轮「更新话术 + 预建下一题」收进 `transactionTemplate.executeWithoutResult`
（与 `start()` 同范式：**只包 DB 写，不含 LLM/SSE**）。
原实现两条写各自自动提交 ⇒ 若「更新话术」成功而「插下一题」失败，
会话停在「已播报但无下一题 QA」的状态（用户无题可答，只能刷新）。

### T2.5 自我介绍评分接线（R1 之外的**功能缺失**）

**这是报告七判错、V1.2 §0.1 修正的那条**：`ScoringEngine.evaluateSelfIntro` 与
`portal_voice_interview.intro_score_json` 早已实现，但**全仓无调用方** ⇒ 该列恒 NULL ⇒
`fuseTotalScore` 从未纳入自介分、报告 `introScore` 恒空（而 V4 段序已把第 1 问固定为自我介绍，等于「问了却不评」）。

**修法（D1 裁决：接线启用）**：新增 `maybeScoreSelfIntro`，在第 1 题（`questionIdx=0`）作答后
**异步**评估（LLM 秒级不阻塞 SSE 收尾）；**幂等** = 已有 `intro_score_json` 直接返回 +
分布式锁 `voice:intro:{id}`（跨实例）；**降级** = 评分器内部已是「LLM→规则」两级，异常只记日志不影响主链路。
落库后记 `self_intro_scored` 事件。

### T2.6 hint 计费口径 + 两级配额（D3 裁决：免费）

- **计费**：提示**免费**（定位是「引导思考」辅助功能，单次输出 ≤128 token，成本可控；收费会抑制使用）；
- **限流两级**：① 每题 ≤3（原已实现，原子 SQL）；② **全场 ≤15**（新增，Redis `INCR` 原子 + 跨实例，
  不可用时降级按事件表计数）—— 原实现只有每题上限，10 题×3=30 次仍可刷；
- **顺序修正**：**先判每题配额、再占全场配额**（原写法反了会「失败的请求白吃全场额度」），
  且全场超额时**回滚刚占的每题额度**，保持两级计数一致。

### T2.7 多实例守卫（R5 之外的**横向扩展硬阻塞**）

**原缺陷**：`RUNNING_ANALYSIS` 是 JVM 内 `ConcurrentHashMap.newKeySet()` 静态集合 ——
单体单实例正确，但**多实例部署时每个实例各持一份** ⇒ 同一场面试被重复分析
（重复烧 token + 报告互相覆盖）。

**修法**：改为「**分布式锁为准 + 内存 Map 作查询镜像**」：
互斥判定用 `DistributedLockUtil.tryLock("voice:analysis:{id}", 10min)`（跨实例、带 TTL）；
Map 仅记录本实例持有的锁句柄，供 `regenerateReport` 等只读判断使用（避免为此再打 Redis）。
新增 `isAnalysisRunning(id)` 封装替换原 `contains`；跳过时记 `analysis_dup_skipped` 并区分 `local`/`remote`。

### 校验

| 项 | 结果 |
|---|---|
| `mvn -o clean test` | ✅ **434 例全绿**（含 `IncrementSqlIdempotencyGuardTest` 等守卫） |
| 编译 | ✅ BUILD SUCCESS |
| 改动范围 | 单文件 `VoiceInterviewServiceImpl`（+ 无 DDL、无前端、无 SQL 变更） |
| 自查发现并修正 | hint 两级配额**顺序缺陷**（先占全场后判每题 → 失败请求白吃额度） |

> **未纳入本批（避免范围膨胀）**：QA 表 `(interview_id, question_idx)` 唯一约束 ——
> 因**追问会共享同一 `questionIdx`**（追问题是同 `question_idx` + 非空 `parent_qa_id`），
> 简单 `UNIQUE(interview_id, question_idx)` 会**误伤追问**；需设计带 `parent_qa_id` 的复合唯一键
> （MySQL 唯一索引对 NULL 不去重，需生成列或改用非空默认值）。当前已由「入口幂等锁 + 落库判重」
> 双层防护覆盖主问重复场景，**唯一约束列为批次 4 待办**。


## v13.43 (2026-09-30) 面试线文档收口：报告七勘误 + 补《报告七再评审》 + 方案 V1.1→V1.2

**背景**：语音面试线存在**三份文档互不咬合**的问题 —— 报告七（问题分析）基于**旧库 `moyun-db`**、
V1.1 方案（施工图）引用了**一份并不存在的《报告七再评审》**、且 V1.1 采信了报告七的**一条错判**。
本次把文档链补齐并修正，作为后续「批次 0/1」的决策依据。

### ① 报告七勘误（7 处）

| # | 位置 | 修正 |
|---|---|---|
| 1 | 头部 | 新增 **⚠️ 勘误与现状更新** 区块：数据库口径已由 `moyun-db` 变为 **`moyun-db2`**；悬空根因已定位并修复；两处判定更正；**报告定位澄清**（是"配置与提示词专项审计"，非四视角评审） |
| 2 | §0.1 结论 2 | "三条链引用全悬空 / **从未成功开面**" → 标注**表述不准确**：该结论**仅对新库成立**，旧库实测有 **18 场**面试记录 |
| 3 | §0.1 结论 5 | "自我介绍**被评两次**" → **改判为「从未接线」**（`evaluateSelfIntro` 零调用、`setIntroScoreJson(` 0 处、`intro_score_json` **恒 NULL**、总分从不含自介分） |
| 4 | §2.5 小节 | 标题改为「**⚠️ 不是"双评"，是"从未接线"**」+ 逐条调用点证据 + **修法差异说明**（应做接线/删代码二选一，而非"去重"） |
| 5 | §4 清单 P1-2 | 「双评 + 维度不一致」→「**评分从未接线**」，补"后续在该字段上加消费方必踩空" |
| 6 | §4 清单 P2-4 | 「默认模型二义」→ **已撤销（非问题）**：`AgentModelRouter:72` 只按 `ModelType.CHAT` 查默认，两行 `is_default=1` 分属 `embedding`/`chat`，**按类型各一，符合设计** |
| 7 | §0.3 / §3.5 | 补现状说明（`model_config_id=17` 悬空**不影响运行**；0 行属新库未开面，非"从未成功"） |

### ② 新增《全端-评审-报告七再评审-20260930》（220 行）

**补齐 V1.1 §关联 所引用但缺失的评审文档**：

- **10 条点名论断逐条判定**：成立 **8** / 部分成立 **2** / **不成立 0**（附 `文件:行号`）
- **数据库口径纠偏**：两库对照实测（`ai_agent` 旧库 7 行 / 新库 2 行；agent id 分叉 47-53 vs 1-2）
- **★ 根因首次查明**：`init-sql` 种子**硬编码来源库 agent id**，而 `ai_agent` INSERT 不含 `id` 列
- **报告完全未覆盖的 6 项运行期隐患**：

| # | 隐患 | 级别 |
|---|---|---|
| **R1** | **答题链无任何并发保护** → 双发同 `qaId` = 答案覆盖 + 双倍 token + **同 idx 插两条 QA**（无唯一约束）——**唯一会损坏数据的问题，严重度高于报告全部 P1** | 🔴 |
| **R2** | 答题与报告聚合**无 `@Transactional`** → 中途失败留半成品（`start()` 事务收窄做得好，长连接不持事务） | 🔴 |
| **R3** | **LLM 无重试**；**超时口径冲突** `SSE_TIMEOUT=120s` ＜ 模型 `180s` | 🔴 |
| **R4** | `onTimeout`/`onError` **只打日志**、**无 `onCompletion`** → **客户端断开后继续烧 token / 写滑窗 / 计量** | 🔴 |
| **R5** | 面试场景 `daily_token_limit=NULL` → **无成本熔断**；逐题分析传 `userId=null` → 落 `"anonymous"` 限流桶 | 🟠 |
| **R6** | 性能未量化：基线 **2N+2 次 LLM/场**（5 题=12 次）；报告 prompt 体积随题数线性增长 | 🟠 |

- **四视角结论**：功能完整性 ⚠️ / 扩展性 ⚠️（**横向多实例不可用**：`RUNNING_ANALYSIS` 是静态内存 Set）/ **系统性能 ❌ 基本空白** / 架构优化 ✅ 方向对但漏 3 点（2303 行四职责混杂、无统一 LLM 端口、报告链**直连模型绕过网关**）
- **既有资产对照**：5 项发现均对应项目**已有**基础设施（`DistributedLockUtil`/`TokenCostGuard`/登记制/版本锁）→ 结论是"**补配置与接线，不推翻制度、不重复造**"

### ③ 方案 V1.1 → **V1.2**（4 处修正）

| # | 修正 |
|---|---|
| 1 | §2.1 触点④「自我介绍评分 **✅ 已收编**」→ **⚠️ 未接线（死代码）**（在恒空字段上加消费方必踩空） |
| 2 | §5.3 ④「落 `intro_score_json`」→ 补「**批次 0 接线后才真正生效**」 |
| 3 | §3.3 新增 4 行 task 配置 → 补「**可维护性说明**」：Q3 不做双下拉 ⇒ 只能 SQL 维护、管理端**可见不可改**，故 `remark` 必须标注 |
| 4 | §2.2 补 **悬空根因与修复全过程**（种子硬编码 → 子查询写法 → 增量 `20260929-04` → 现网归零） |

**同时**：`§8 实施批次` 新增**批次 0（修地基）**并补其 6 项验收；`docs/README.md` 链接由 V1.1 改为 **V1.2** 并新增《报告七再评审》条目。

### ④ 顺带修复：devlog 版本号重复

devlog 中 **`v13.38` 出现两条**（一条本会话所加、一条并行所加）。已**合并为一条**并保留双方独有内容
（新增「⑧ AI 绑定链修复」段 + 移入「⑨ 顺带发现」段），`v13.39` 让位给既有的知识库修复条目。

### 校验

| 项 | 结果 |
|---|---|
| 文档链接自查 | ✅ **0 失效** |
| 报告七 | 570 → **597** 行（7 处勘误） |
| 《报告七再评审》 | ✅ 新增 **220** 行 |
| 方案 V1.2 | 297 → **326** 行（V1.1 文件按铁律 7 重命名升版） |
| devlog v13.38 | ✅ 重复条目已合并（删 53 行，补 2 段） |


## v13.42 (2026-09-30) 方案 V1.0→V1.1：两轮评审 9 处修正整合 + 4 项裁决落锤

**背景**：V1.0 方案经用户两轮质询评审（"是否最优解""tab 层次/布局/命名专业评审"），修正 9 处并升版
V1.1（文件已重命名，§0 为升级清单）：

- **两处推翻性修正**：①开场降级链路**收编不删除**（V1.0 原判"删除"是可用性回退——warmup 失败
  多为瞬时抖动，删兜底=一次抖动一场面试开不了头；新增 `:opening_fallback` 配置行）；②复盘输出
  **防爆瘦身**（合并预测后单次输出 ~3.2K 贴 max_tokens 天花板，JSON 截断=整场复盘降级；perQuestion
  复用逐题落库分析仅回填分数、预测上限 6 条、max_tokens 压 2560，输出回到 ~2.2K）；
- **内容差异化**：洞察输入追加本场报告上下文（weakPoints/levelEstimate，与 job_match 划清边界）；
  配置行预留 knowledge_library_ids（RAG 为真动态第一优先来源）；
- **信息架构重排**：5 tab 三层结构（复盘区：概要→问题分析→**回放殿后**；备战区：追问预测→发展方向），
  主线=表现→归因→证据→个人预测→环境导航；**改名**：「简历分析」→「🔮 追问预测」（避免与简历优化
  模块语义冲突）、「行业洞察」→「🧭 发展方向」（诚实定位：把本场短板放进行业坐标系）；
- **写档防误判**（§3.4）：追问流式与 answer_analysis **明确不合并**（每场可省 ~14K 曾被评估——
  会话流式与 JSON 结构化是两种输出形态，合并互相污染）；
- **4 项裁决落锤**（§9）：预测上限 6 条 / 洞察缓存 7 天提示刷新 / UI 双下拉不并入 / 不合并流式分析；
- **防悬空写法**：配置行 agent_id 一律 name 子查询（双库 id 分叉教训）；批次 1 加等价性验证
  （前后各 3 场冒烟 + 旧提示词进 remark 留档）。

README 索引同步；SQL/菜单无变更。

## v13.41 (2026-09-30) 语音面试全链路场景收口与报告增强方案（顶层设计，待裁决）

**背景**：用户提出报告增加「简历预测问题」「行业洞察」两 tab，同时要求先做全旅程场景顶层设计
（成本考虑 + 统一入口）。**新增方案文档**《全端-语音面试-全链路场景收口与报告增强方案》
（`docs/05-方案设计-分模块/4-语音面试/`，当日升版 V1.1 见 v13.42），核心裁决：

- **触点底账**：面试全旅程 6 个 AI 触点——3 已收编网关（warmup/answer_analysis/self_intro + 会话流式）、
  **3 处仍是代码拼提示词直连**（generateOpening 降级链路 L673 / requestHint L1016 /
  enhanceReportByAgent L1648——报告七 P1-6 活例），批次 1 全部收编；
- **成本裁决**：预测问题**并入** report_review 复盘调用（输入同源不单开，每场仅 +~1K 输出 token）；
  行业洞察**懒生成**（不开 tab 不花钱）+ 口径诚实化（"洞察与方向建议"，不承诺实时动态，搜索 API 列 TODO）；
  每场总预算 ~25-30K 输入 / ~11K 输出，对比现状 +~3K 且可收敛为零；
- **场景地图**：全部挂 `voice_interview` 主场景拆 task（不新增枚举），新增 3 配置行
  （:hint/:report_review/:industry_insight）；顺带清偿报告七 P0-3（default_chat 枚举）、P0-4（jd_keywords 常量）欠账；
- **报告终态 5 tab**：概要/对话回放/问题分析（现状）+ 📄 简历分析（预测问题，已问/未问双分组）+
  🧭 行业洞察（占位卡+时间戳+刷新）；每 tab 独立容错，缺数据整 tab 隐藏；
- **三批次交付**：①收编+清欠（行为零变化）→ ②预测 tab → ③洞察 tab，每批独立 commit + 四同步。

**附带核实**：db2 的 `voice.interview.defaultAgentId=2` 指向存在的 agent（报告七 P0-1 悬空已修复），
报告 LLM 增强通道现为活链路。SQL/菜单无变更；README 索引已登记。

## v13.40 (2026-09-30) 语音面试作答不限时：去掉 90 秒自动提交，改正计时 + 软提醒

**用户裁决**：模拟面试应允许充分思考，25/90 秒自动提交不合适——可友好提示，不能替用户交卷；
思考时长可后台统计。

**改动**（`VoiceInterviewPage.vue`，纯前端）：
- 删除每题 90 秒倒计时与"作答超时，自动提交"强制提交逻辑
- 改为**正计时**：输入栏显示「⏱️ 已用时 N 秒 · 不限时，想好再提交」
- **软提醒**（每题一次）：90 秒 toast「已作答 90 秒——不用急，想好再提交也完全可以」，不阻断不提交
- **后台统计已有**：提交答案时 `latencyMs`（题目展示→提交的思考耗时）随 `/answer` 上报，
  报告链路可用；本场总时长倒计时（`voice.interview.durationMinutes`）不受影响，全场时间到仍收口生成报告
- 卡壳自动提示（30 秒无作答给一级提示）保留不变

**验证**：`vue-tsc -b` exit 0；`answerRemain`/`秒内作答` 全项目零残留。SQL/菜单无变更。

## v13.39 (2026-09-30) fix：知识库/智能体配置链路四连修（MinIO 本地降级 / 半空配置 NPE / 模板与词典种子缺失）

### ① AI 存储 MinIO 本地自动降级（MinioServiceImpl）

**现象**：知识库上传报 `Failed to connect to /127.0.0.1:9001`。**核查结论：并非 git 回退**——AI 模块的
`MinioServiceImpl` 自首个集成版本起从未有过降级逻辑；此前做的降级在通用文件链路
（`SysFileServiceImpl`，`sys_config[file.storage.mode]` 三级降级），知识库走独立存储链路从未打通。

**修法**：与 SysFileServiceImpl 同口径——上传（文档/图片）/读取/删除在 MinIO 异常且
`minio.auto-fallback=true`（dev 默认）时自动降级本地 `profile/ai/{knowledge|images}/`，
返回 `local:` 前缀路径，读写删按前缀识别；流先读全量再上传，降级复用同一份字节。
用户实测：上传成功进入处理阶段。

### ② 半空配置 NPE（KnowledgeConfigServiceImpl.applyConfiguration）

**现象**：处理阶段 `getSegmentMaxLength() is null` NPE。
**根因**：自定义配置分支 `BeanUtils.copyProperties(request, config)` 未覆盖字段全 null 落库并返回，
Controller 直接把半空对象传 `processKnowledge`（绕过了 `resolveEffectiveConfig` 三层合并兜底）。
**修法**：保存前 `mergeWithDefaults` 补 KnowledgeDefaults 基线（16 字段，与 resolveEffectiveConfig 同口径），
模板分支（模板 JSON 缺字段）同样受益。

### ③ 配置模板种子数据补齐（ai_knowledge_config_template）

**现象**：「快速配置（推荐）」Tab 模板列表为空。**根因**：表种子数据从未进 DML 初始化脚本，
新库/重灌库为空（运行库 moyun-db2 即空表），用户只能走自定义配置（恰是 ② 的触发路径）。
**修法**：5 个系统模板双轨补齐（DML 初始化脚本 + 增量脚本
`increment-sql/20260930-01-ai_knowledge_config_template种子数据补齐.sql`，后者已执行于 moyun-db2）。

### ④ 领域词典种子数据补齐（ai_domain_dictionary）

**现象**：智能体编辑弹窗「专业词典」下拉无数据。**根因**：同 ③——`ai_domain_dictionary`
种子数据从未进 DML 初始化脚本，运行库 moyun-db2 为空表（接口查 `is_global=0 AND enabled=1`）。
**修法**：17 条词典双轨补齐（专业 14 + 全局 3，与 moyun-db 存量一致；DML 初始化脚本 +
增量脚本 `increment-sql/20260930-02-ai_domain_dictionary种子数据补齐.sql`，后者已执行于 moyun-db2）。

**验证**：编译通过；db2 模板 5 行（general 4 + technical 1）、词典 17 行（专业 14 + 全局 3）；SQL 双轨同步，菜单无变更。
**附带**：v13.35 已记录的双库分叉问题再次暴露——moyun-db 与 moyun-db2 数据持续漂移，
后续数据修复须明确单一运行库或双库同步。

## v13.38 (2026-09-29) 简历解析重构落地：纯 Java 规则引擎 + 预览校对 + 配置化词表

**方案依据**：《全端-简历-方案-简历解析重构方案-V1.0》（`docs/09-临时报告/`），本次为 **Phase 1（大类划分 + 排版保真 + 内容零丢失）+ Phase 2（条目化）** 的完整落地。
**两项裁决**：**Q1** 规则路径**同步返回**（毫秒级，不再依赖异步/轮询）；**Q4** 做 **U1 左右对照**校对页。

### ① 规则解析引擎 `ResumeRuleParser`（新增，纯 Java 零 AI）

| 规则 | 内容 |
|---|---|
| **R0** 归一化 | 去零宽字符/BOM、全角空格、压缩空行 |
| **R1** 章节分桶 | 词典驱动；支持「一、」「【】」「◆」装饰符与**「标题：内容」同行写法** |
| **R2** 日期锚点 | `2020.03-2023.06` / `2020年3月至今` / `2020/03—2023/06` → 归一 `yyyy-MM` |
| **R3** 条目切分 | 日期行=锚点，下一条日期行=终点 → 得到「1、2、3」 |
| **R4** 字段分配 | 锚点上方紧邻行=名称；**按职位关键词**拆「公司 职位」；余下原文进 `description` |
| **R5** 基本信息 | 手机/邮箱/姓名/性别/出生日期（**只认显式标签，绝不推断**） |
| **R6** 技能 | 词域匹配，**自动聚合 `portal_job_template.required_skills`**（零硬编码） |

**单测 12 例全绿**（`ResumeRuleParserTest`），含 4 个**防误判**用例。测试过程抓到并修掉 6 个真实缺陷，其中最关键：
> 短词「技能」前缀匹配到正文「技术栈：Spring Boot…」→ 被判为章节标题 → **整段内容被切走**。修正为「前缀匹配的余量必须是纯 ASCII」（只用于「教育背景 Education」这类中英混排）。

### ② 「预览 → 确认」两步式（本次核心架构改进）

```
POST /parse/preview  【同步·毫秒级·不落库】请求内就地抽取文本 → 规则结构化 → 返回
                     {previewToken, 解析结果, rawText}   ← rawText 供左右对照
        ↓ 用户校对（前端弹窗，字段可改）
POST /parse/confirm  【唯一写库入口】落库为新简历，返回 {resumeId}
```

- **解析不落库** → 解析失败或用户放弃**零残留**（根治"失败留空简历脏数据"）；
- **同步返回** → 不再需要异步任务与轮询（`/parse` 异步路径保留兼容）；
- **附件不落盘/不进对象存储** → 只读内容，用完即弃；
- 预览令牌 **10 分钟 TTL**（内存 `ConcurrentHashMap`，惰性清理 + 上限防御，不新增表）。

### ③ 文本抽取质量修复（纯收益，与是否用 AI 无关）

| 问题 | 修复 |
|---|---|
| `PDFTextStripper` 默认按内容流顺序输出，**双栏/表格简历文字交错** | 开 `setSortByPosition(true)` |
| `XWPFWordExtractor` 把表格**拍平**，行/列关系丢失（简历大量用表格排版） | 改遍历 body 元素，**表格单元格用 `\t` 连成行** |

### ④ 配置化词表 + 后台管理（新建表 → 铁律双轨）

- **表**：`portal_resume_parse_config`（section 章节词 / skill 技能词 / degree 学历词 / position 岗位词）；
- **init-sql**：DDL 建表（**185 张表**）+ DML 种子（**10 条**）；
- **增量**：`20260929-02`（结构 + 幂等种子）、`20260929-03`（菜单 + 按钮权限），**均连跑两次 exit 0**；
- **后台**：实体/Mapper/Service/Controller（`/cms/interview/resumeParseConfig`，`@PreAuthorize` 五权限）+ 前端页面 `views/cms/interview/resumeParseConfig/index.vue`；
- **降级**：表为空/不可用时引擎使用**内置默认词典**兜底 —— 配置问题绝不导致解析失败。

### ⑤ 前端 U1 左右对照校对页

新增 `components/resume/ResumeParsePreviewModal.vue`：**左原文 / 右可编辑解析结果**，含基本信息、求职意向、教育/工作/项目**逐条（1、2、3）**、技能、自评，支持**增删改**；顶部提示「已识别 N 个内容大类」或降级警告。接入 `ResumeEditPage`（上传即预览）与 `VoiceInterviewPage`（面试准备页同样两步式）。

### ⑥ 命名冲突修复（过程中发现）

`ResumeEditPage` **已有** `components/resume/ResumePreviewModal.vue`（简历**文档预览**，另一种用途）→ 新组件命名 `ResumeParsePreviewModal` 并置于同一目录，样式前缀 `rv-`→`rpp-` 避冲突；状态变量用 `parsePreview*` 前缀消歧。

### ⑦ 守卫同步

`ModuleDependencyGuardTest`：`portal -> ext.cms` 83→**84**（引入 ResumePreviewVO）、`ext.cms -> portal` 277→**278**（ResumeParseService 引入 PortalJobTemplate），均附理由。

### ⑧ 菜单 id 空间重排（⚠️ 过程中的重大坑，已修复）

尝试为「简历解析配置」腾出 M/C 段 id 时，因**幂等判定缺陷 + 目的地被占**导致库一度处于中间状态（91 行悬在大偏移区）。
**已完全恢复**：M/C 121 条全在 1..121、**0 重复、0 悬空父引用**，并同步了 `sys_role_menu`。
最终取**唯一连续空闲块**：菜单 `397` + 按钮 `398..401`（备份见 `.archive/drops-20260928/sys_menu-before-resumeParseConfig-menu.sql`）。
> 教训：**大范围 id 重排必须逐条验证、小步提交**，不能用"看似原子"的批量事务。

### 校验

| 项 | 结果 |
|---|---|
| 后端 `mvn -o test` | ✅ **434 例全绿**（含新增 12 例规则引擎单测） |
| 门户 `vue-tsc -b` | ✅ exit 0 |
| 后台 `npm run build:prod` | ✅ exit 0 |
| init-sql 独立初始化 | ✅ **185 表 / 401 菜单 / 0 重复 / 0 悬空父 / 种子 10 条 / 菜单 5 条** |
| 增量脚本幂等 | ✅ `20260929-02`、`20260929-03` 各连跑两次 exit 0 |
| 数据清理 | ✅ 4 条空草稿 + 4 条 sys_file + 5 份残留 PDF（已备份） |

> 门户 `npm run build`（生产）因**既有 SEO 守卫**缺 `VITE_SITE_URL` 而失败，与本次改动无关（`vue-tsc` 门槛已通过）。

### ⑧ AI 绑定链修复（v13.38/v13.39 · 面试开不起来的**真正根因**）

**根因（首轮报告未定位，本次查明）**：`init-sql` 种子把 `ai_scene_config.agent_id` 与
`sys_config.defaultAgentId` **硬编码成来源库的 id（48 / 47）**，而 `ai_agent` 的 INSERT
**不含 `id` 列**（自增分配，全新库恒为 1/2）⇒ **任何全新库初始化即带悬空引用**
⇒ `start()` 必抛「AI 面试官未配置或不可用」⇒ **面试永远开不起来**。

**这不是"环境数据坏了"，是"种子就有缺陷"**。已修：

| # | 修复 | 验证 |
|---|---|---|
| 1 | `moyun-db-dml-init.sql` **5 处**硬编码 → **按 `name` 子查询取 id**（4 个 voice_interview 场景 + `sys_config.defaultAgentId` + `finance_analysis`） | ✅ 全新库**悬空 = 0**、`defaultAgentId=2`、4 场景绑 agent 2、finance 绑 agent 1 |
| 2 | 新增增量 `20260929-04-AI绑定链修复（消除agent_id硬编码悬空引用）.sql`（幂等 + `_bak_` 备份） | ✅ 连跑两次 exit 0 |
| 3 | 现网 `moyun-db2` 已应用 | ✅ **悬空 = 0**、`defaultAgentId=2 → AI面试官·默认` |
| 4 | 备份可回滚 | ✅ `ai_scene_config_bak_v1338` / `sys_config_bak_v1338` / `ai_scene_config_bak_agent_id_v1338` |

> **过程留痕**：`IncrementSqlIdempotencyGuardTest`（守卫）**拦住了本次提交** —— 破坏性 `SET col = NULL`
> 缺 `_bak_` 备份表。按规约补上后通过。**守卫有效，这是好事。**

### ⑨ 顺带发现（评估留痕，供后续决策）

- **`AiTaskHandler` 体系**：4 个 Handler 实为「**1 个入口型（`resume_parse`）+ 3 个派发型**」
  被塞进同一接口 → 症状：8 行取值/校验样板重复 4 次、**3 种失败语义**（`ai_draft` 用"返回空结果"
  造成**假成功**）、**归属校验放错层**（`ai_draft` 漏校验 = 越权缺口）、
  **Handler 私自写库导致框架无从补偿**（← **原始脏数据 bug 的根因**）；
- **命名/框架重构结论（推翻我自己的前一轮判断）**：
  `portal_ai_task` → `sys_async_task`、`ext.task` 迁包**均建议推迟**。理由：① **零功能收益**
  （真正限制复用是 `user_id NOT NULL` 与缺幂等键，与名字无关）；② 项目已有同款先例 `sys_audit_task`；
  ③ 改名**不可逆地破坏可搜索性**（历史增量脚本按铁律不可改）；④ `RENAME TABLE` 是元数据操作，
  "趁数据少改名"**不成立**。
  并**更正**我此前"改名成本几乎为零"的错判：漏算了实体类 `PortalAiTask` 的 **27 处代码引用**。

## v13.37 (2026-09-29) 岗位配置统一：删 portal_interview_position → 全并入 portal_job_template + 面试页岗位下拉接后端

**需求（用户裁决）**：`portal_job_template` 有后台管理页可配置，`portal_interview_position` **没有任何配置管理入口**，
两者**职责重复且使用混乱** → **全 portal 统一用 `portal_job_template`**，删除 `portal_interview_position`；
需要哪些字段就并入；**代码 / 文档 / 脚本全部同步删除**旧表。

### ① 表合并（旧表 6 列并入新表）

| 并入列 | 用途 | 为何必须迁移 |
|---|---|---|
| `code` | 岗位编码（如 `java_backend`） | 按编码反查 |
| `industry` | 所属行业 | 画像/展示 |
| `level` | 岗位级别 junior/mid/senior | 画像/筛选 |
| **`required_skills`** | 必备技能 JSON 数组 | **驱动「简历岗位匹配评分」与「用户画像必备技能」，不迁会导致匹配度直接为 0** |
| `hot_companies` | 热门公司 JSON 数组 | 展示 |
| `sort` | 排序 | 下拉顺序 |

新表加 `idx_code` / `idx_sort` 索引；表注释改为「全 portal 岗位配置唯一来源」。

### ② 代码改造（8 个 Java 文件）

- **删除**：`PortalInterviewPosition`（实体）、`PortalInterviewPositionMapper`、
  `IPortalInterviewPositionService`、`PortalInterviewPositionServiceImpl`、`PortalInterviewPositionController`；
- **新增**：`PortalJobTemplateController` —— 门户公开接口 `GET /portal/interview/jobTemplate/list`（`@Anonymous`），
  仅暴露前端选岗与回填所需字段（含 `jobDescription` = JD 原文、`difficulty`、`questionCount`、`requiredSkills`）；
- **职责并入**：`IPortalJobTemplateService` 新增 `findActiveByName`（**精确 + 模糊兜底**，逻辑自旧实现原样迁入）与 `findActiveByCode`；
- **消费点改注**：`ResumeScoringService`（简历岗位匹配评分）、`UserProfileSnapshotServiceImpl`（画像必备技能）
  由 `IPortalInterviewPositionService` 改注 `IPortalJobTemplateService`；
- **门户前端**：`api/interview.ts` 的 `getInterviewPositions` → `getJobTemplates`；
  类型 `InterviewPositionVO` → `JobTemplateOptionVO`（迁至 `types/api.ts`）；`UserProfilePage.vue` 同步。

### ③ 面试页（`/interview/voice`）岗位下拉接后端 + 回填

- **删除前端硬编码 `POSITION_OPTIONS`**（5 条写死岗位）→ 改由后台【岗位模板】驱动；
- **选中岗位即回填**：`jdText` → 「岗位要求」、`difficulty` → 难度、`questionCount` → 题量；
- **回填后可自由修改**：加「已按模板『xxx』回填 · 可修改」来源标记；用户改动后**切岗位不再覆盖**；
  提供「↺ 恢复模板值」与「✕ 清空」；
- **不选岗位/自定义则以此处为准**（留空 = 按岗位通用标准出题）；
- 简历求职意向命中模板时，连带回填该模板的 JD/难度/题量；下拉为空时提示去后台配置。

### ④ SQL（双轨同步）

- **DDL**：删除 `portal_interview_position` 定义（**185 → 184 张表**）；`portal_job_template` 加 6 列 + 2 索引；
- **DML**：删除旧表种子，`portal_job_template` 种子**由存量库导出重建**（5 条，逐字一致）；
- **增量脚本**：`20260928-12`（加列 → 数据迁移 → 删旧表）、`20260928-13`（Java 三档合并 + 迁入行补 JD），
  均**幂等**（连跑两次 exit 0）。

> **迁移中修正的存量脏数据**：① 按 `name` 匹配失败导致 Java 后端出现重复行且 3 条 Java 模板**缺 required_skills** →
> 改按业务同一岗位语义合并；② `中级 Java` 的 JD 被错写成「初级 Java」原文 → 已更正；
> ③ 迁入行 `sort` 与 Java 中/高级冲突（2/3）→ 改为 4/5。

### ⑤ 守卫更新

`ModuleDependencyGuardTest` FROZEN_EDGES：`ext.cms -> portal` **278 → 275**
（删除旧表相关类后依赖减少 3 处，按铁律「减少也要同步下调」更新）。

### 校验

- 后端 `mvn -o clean test` → **422 例全绿**；
- 门户 `vue-tsc -b` → **exit 0 无类型错误**；
- **init-sql 独立初始化比对**：全新库与存量库 **表数 184 = 184**、
  `portal_job_template` **5 条逐字段一致**（code / sort / difficulty / question_count / JD 文本长度）；
- 新接口实测 `GET /portal/interview/jobTemplate/list` → **code=200，5 条**（JD 424/325/707/317/290 字）；
  旧接口 `/portal/interview/position/list` 已无映射。

> ⚠️ **过程中我造成并已修复的一次误伤**：编辑 DDL 时用文本 `Replace` 匹配 `idx_status` 锚点，
> 因该锚点非唯一，把 `idx_code`/`idx_sort` 误加到 **17 张表**。已**从 git HEAD 取回 DDL 并按「当前表名」状态机精确重做**
> （仅改 `portal_job_template`），复核索引归属正确。教训：**DDL 编辑必须按表块定位，不能用全局文本替换**。

## v13.36 (2026-09-29) fix：el-radio 单选失效（点一个两个都选上）——value prop 全项目改 label

**现象**：公司标签页（/portal/learn/company）编辑弹窗「状态」单选，点击「启用」两个 radio 同时选中；
多个页面同病灶。

**根因**：项目 Element Plus 为 **2.4.3**，`el-radio` / `el-checkbox` 的 `value` prop 是 **2.6.0+** 才引入；
旧版必须用 `label` 传值。写成 `value=` 时 radio 无绑定值，v-model 匹配失效导致全组联动选中。

**修复**（纯前端，15 处 / 6 文件，`value=` → `label=`，插槽文本保留为显示文案）：
`cms/interview/company`、`cms/interview/category`（4 处）、`cms/interview/resume`（2 处）、
`cms/ledger/category`（`:value` 动态绑定 2 处）、`system/sensitiveWord`（2 处）、`ai/agent`（3 处）。
全项目扫描复核：`<el-radio value=` / `<el-radio :value=` 0 残留；`el-checkbox` 无同类问题。

**验证**（浏览器实测）：company 编辑弹窗启用/停用切换互斥正常。

**四同步**：代码 6 文件；SQL/菜单无变更。

## v13.35 (2026-09-29) fix：题库列表三列反显（难度字典数据缺失 / 分类映射 / 岗位回填）+ useDict 空缓存自愈

**现象**：管理端题库列表「难度 / 分类 / 岗位模板」三列不显示。

**根因与修复（四处）**：

1. **难度字典数据行从未初始化**：`sys_dict_type` 有「题目难度」类型行但 `sys_dict_data` 无数据行
   （查询/编辑表单下拉与 dict-tag 全空）。双轨补齐 easy简单/medium中等/hard困难 3 行：
   `moyun-db-dml-init.sql` + 增量脚本 `20260929-01-portal_question_difficulty字典数据补齐.sql`（幂等，
   附 Redis 字典缓存清理说明）。`portal_question_type` 同样无数据行但全前端无消费者，不补。
2. **useDict 空缓存不自愈**（`moyun-admin-vue/src/utils/dict.js`）：Pinia store 缓存空列表后
   `if (dicts)` 对 `[]` 判 truthy 永不再回源——字典数据后补时页面必须重登才生效。
   改为 `dicts && dicts.length`，空缓存自动回源。
3. **分类列反显**：后端列表 VO 有 `categoryName` 字段但 `toQuestionVO` 从未回填；
   按用户裁决**前端处理**——列表列改为 `row.categoryName || categoryName(row.categoryId)`，
   用已加载的 `categoryOptions` 做 id→name 映射（与岗位模板同口径）。
4. **编辑回填漏 jobTemplateId**：`handleEdit` 表单填充对象缺 `jobTemplateId` 字段，
   编辑弹窗岗位恒显示"不关联"（若保存重选会丢关联语义）；补 `jobTemplateId: data.jobTemplateId ?? null`。

**环境注意（本次排查踩坑，后续排查必读）**：当前运行的后端为 dev profile，连 **moyun-db2**；
local profile 连 moyun-db。两个库结构相同但数据独立，**改数据前先确认目标后端连的哪个库**
（`application.yaml` 的 `spring.profiles.active`）。本次字典数据已同时补入两库。

**验证**（浏览器实测）：难度接口返回 3 条（简单/中等/困难）；列表难度列彩色标签正常显示
（简单绿/中等橙）；分类列显示"后端开发"；编辑弹窗难度下拉 3 选项可正常选择。
岗位模板列显示"-"系存量题目 `job_template_id` 均为 NULL（数据未设置），机制链路已通。

**四同步**：代码（index.vue + dict.js）；SQL（DML 初始化 + 增量脚本，双库执行）；
文档（部署指南 V13.33 增量脚本清单补第 3 行）；菜单无变更。

## v13.34 (2026-09-29) fix：题库编辑提交报 JSON parse error（companies 数组 → String 反序列化失败）

**现象**：管理端题库「修改」提交报 `请求体解析失败: JSON parse error: Cannot deserialize value of type java.lang.String from Array value`；
新增正常、修改必炸（实测抓包确认）。

**根因**：`InterviewQuestionDetailVO.companies` 为 `List<InterviewCompanyVO>`（详情接口返回**关联公司对象数组**，
读 `portal_interview_question_company` 关联表），而编辑表单期望逗号字符串。前端 `handleEdit` 用
`data.companies || ''` 回填——**空数组 `[]` 为 truthy 拦不住**，数组原样进表单并被 PUT 提交，
后端实体 `companies` 为 String → Jackson 反序列化失败。

**修复**（`moyun-admin-vue/src/views/cms/interview/question/index.vue`，纯前端）：
- 新增 `companiesToStr`：对象数组取 `name` / 字符串数组直取，`join(',')` 归一为逗号字符串；
- `handleEdit` 回填改 `companiesToStr(data.companies)`；`submitForm` 提交数据同步归一（双保险）。

**验证**（浏览器实测抓包）：PUT body 中 `companies` 已从 `[]` 变为 `"腾讯,阿里"` 字符串，格式正确。

**遗留断层（另行处理，本次不动）**：题目"公司"存在**双通道**——实体 `companies` 字符串列（编辑保存写这里）
vs 公司关联表（详情/回显/前台按公司筛题读这里）。编辑保存不同步关联表，导致保存后回显丢失、
前台按公司筛选查不到；需产品决策后统一（建议编辑保存时同步 upsert 关联表）。
另：编辑弹窗 submitForm 的空 catch 吞掉后端报错，UI 无失败提示（既有风格，未擅改）。

## v13.33 (2026-09-29) 门户栏目初始化重做：portal_category 显式 id + 重跑安全（下游不悬空）

**需求**：`moyun-portal-category-redo.sql` 旧版依赖 AUTO_INCREMENT（id 从 51 起）+ `parent_id` 硬编码 52~71，
换库重导后自增起点不同导致父子关系全部错位（前台导航树 `/portal/category/nav/tree` 错乱）；
且要求**存量库重跑 TRUNCATE 后文章模块引用不悬空**。

**实现**（`init-sql/moyun-portal-category-redo.sql` 全量重写，menu-redo 同模式）：

- **三段式幂等结构**：① 旧表 `id→slug` 快照到 `_bak_portal_category`（全新库为空快照）→
  ② `TRUNCATE` + **显式 id 1..49** 全量重插（任何库任何次重跑落位恒定，父 id 恒小于子 id）→
  ③ 按 **slug 稳定键**回填下游 `portal_article.category_id/root_category_id`（及 `portal_book` /
  `portal_book_list.category_id`），旧 id 经 slug 映射到新 id；二次重跑回填为同值无操作；
- **归属重归纳**（按前台路由实际上下文）：面试题库归学习中心（/learn/questions）、
  简历模板/面经/AI 面试官归面试专区、读书三入口挂读书空间目录、话题/动态/专栏/征文/发布/成长排行归创作互动；
- **数据修正**：剔除游离行「面试指南」（slug 与顶级 `interview` 重复）与软删行；死路径修正
  `/creation`→`/feed`、`/reading/space`→`/reading`（前台无对应路由）；
- **末尾三项复核 SELECT**：下游悬空引用 / 孤儿父级+父小于子 / slug 重复，重跑后应全为 0。

**dev 库执行验证**（2026-09-29）：13 篇存量文章经 slug 回填全部落位正确
（人间烟火 73→22、山河行吟 74→23、城市笔记 76→25、四季专栏 77→26、首页 51→1）；三项复核全 0。

**四同步**：`部署指南` 初始化顺序补入第 4 步（原清单漏此脚本）+ 幂等说明
（`全端-部署-部署指南` 升 **V13.33**，文内与全部引用同步）；菜单无变更；
归档雷同存根 `全端-规划-项目现状总结-V13.28.md`（内容与 `docs/README.md` 索引重复）至 `.archive/`。

## v13.32 (2026-09-28) 菜单表单：路由地址（path）唯一性校验 + 修改建议

**需求**：不做后端 `getRouteName` 代码加固；改为**在菜单管理的新增/修改表单对 `path` 加校验**——
已存在（唯一性冲突）时给出提示**并给修改建议**。

**实现**（`moyun-admin-vue/src/views/system/menu/index.vue`，纯前端，**未改后端**）：

- **校验数据源**：列表页已加载整棵菜单树（`menuList`），本地拍平即可校验，**无需新增接口**
  （后端 `/system/menu/list` 仅支持按 `menuName` 过滤，不支持按 path 查）；
- **三级校验**（`checkMenuPath`）：
  1. **命名规范**——不得以 `/` 开头（会被 vue-router 当绝对路径提升到顶层 → 父路径丢失 → **404**）；
     不得含 `/`（会使路由 name 变成非法标识符）；
  2. **同级唯一**——同一父节点下路径完全相同 → 直接 404；
  3. **路由 name 全局唯一**——路由 name = `capitalize(path)`，**跨父节点同名段会撞 name**
     （vue-router 丢弃先注册者 → 菜单 404）。此条即 v13.28/v13.31 两次真实缺陷的成因；
- **修改建议**（`suggestPath`）：把非法值归一为**单段 kebab-case**，并**避开全表已用段**
  （生成 `base-2`、`base-3`…）——注意必须按**全表**而非同级避让，否则建议值仍会撞 name；
- **交互**：`el-form` 自定义 validator（`trigger: ['blur','change']`）拦截提交；输入框下方展示红色提示
  **+ 可点击的「采用建议：xxx」按钮**（一键填入并重新校验）；`@input` 清空提示；新增/修改时重置提示；
- 外链（`http(s)://`/`mailto:`/`tel:`）与空值跳过该校验（空值仍由 `required` 拦截）；
- 同时补充「路由地址」的 tooltip，写明**必须为单段相对路径**及两条原因。

**验证**：从组件抽出纯函数，**用现网 121 条真实菜单数据**跑 14 项断言**全部通过**：
同级重复拒绝并建议 `article-2`、跨父同段拒绝并建议 `user-2`、
`/category`→建议 `category`、`diagram/chat`→建议 `diagram-chat-2`（`diagram-chat` 已被占用，正确避让）、
**建议值再校验可通过**、编辑自身不误报、编辑改为同级占用正确拒绝、外链跳过、空值交 required。

**校验**：管理端 `npm run build:prod` **exit 0**（`✓ built in 5m 17s`）。

## v13.31 (2026-09-28) 菜单/页面收尾核查：修 3 处非法 path + 7 处 activeMenu 失效路由

**背景**：回到原始任务「优化首页 → 按前台首页归纳分类重组后台菜单与页面」做**完成度核查**（不凭记忆）。
核查手段：菜单树导出 + **99 个 C 类菜单 component → 逐一对到真实 `.vue` 文件** + 递归全路径/路由 name 推导
+ `activeMenu` 反查 + 孤儿页面扫描。

**核查结论：主体已完成，发现并修复 10 处收尾缺陷**（均属菜单重构时的遗留）：

**① 3 处非法 `path`（会真 404，非风格问题）**

根因：`SysMenuServiceImpl.getRouterPath` **只给顶级 `M` 加前导斜杠**、给 `isMenuFrame` 用 `/`；
**子菜单 path 自带前导斜杠时，vue-router 会把它当绝对路径提升到顶层 → 父路径丢失 → 404**。
而 `getRouteName` = `capitalize(path)`，含 `/` 时生成**非法路由 name**（vue-router 4 要求合法标识符）。

| menu_id | 菜单 | 原 path | 问题 | 现 path |
|---|---|---|---|---|
| 5057 | 架构图生成 | `diagram/chat` | 路由 name = `Diagram/chat`（含 `/`，非法） | `diagram-chat` |
| 5085 | 分类管理 | `/category` | 前导斜杠 → 脱离 `/portal/cms` 父路径 → 404 | `content-category` |
| 5244 | 配置管理 | `/systemConfig` | 同上（父级为 `system`） | `system-config` |

**② 7 处 `activeMenu` 指向菜单重构前的旧路由**（面包屑/侧边栏高亮与返回路径失效）

| 位置 | 原值 | 新值 |
|---|---|---|
| `router/index.js:143` 分配用户 | `/system/role` | `/system/base/role` |
| `:157` 字典数据 | `/system/dict` | `/system/system-config/dict` |
| `:171` 调度日志 | `/monitor/job` | `/system/monitor/job` |
| `:185` 修改生成配置 | `/tool/gen` | `/system/tool/gen` |
| `:205`/`:211` 编辑文章 | `/cms/article` | `/portal/cms/article` |
| `:217` 测试用例管理 | `/portal/interview/question` | `/portal/learn/question`（题库已迁学习管理） |

> 根因：v13.26 菜单重组把 `内容管理` 移入 `门户管理`、`系统监控/工具/配置` 移入 `系统设置`、
> 题库移入 `学习管理`，但**静态路由的 `activeMenu` 未同步**——属"四同步"里的菜单↔代码同步漏项。

**落地**：3 处 path 同步落 **`init-sql/moyun-menu-redo.sql`（单点维护）** 与**现网库**
（`UPDATE ... AND path='旧值'` 条件式，幂等复跑 0 行受影响）；7 处 `activeMenu` 改前端
`src/router/index.js`（经确认为隐藏详情页，仅供高亮/返回，**未被任何业务代码硬编码引用**）。

**核查通过项（无需改动）**：
- **99 个 C 类菜单 component 全部命中真实 `.vue` 文件**；
- 递归全路径 **121 项路由 name 全唯一合法**（无 `/`、无非法字符，无碰撞）；
- 13 处 `activeMenu` **全部命中真实路由**（修复后）；
- 孤儿页面扫描 15 项**全为合法非菜单页**：`ai/chat`（`/ai/chat/index/:agentId?` 静态路由）、
  `cms/interview/testCase`（隐藏详情页）、`login`/`register`/`401`/`404`/`redirect`/`index`/`user/profile`；
  以及 **5 个被 TabContainer 容器 import 的子面板**（`monitor/server`+`monitor/druid` 由 `server-panel` 引入、
  `monitor/cache`+`list` 由 `cache-manage` 引入、`portal/bookQuote`+`bookshelf` 由 `userContent` 引入）
  —— 初扫误报，已逐一确认在用；
- 首页看板 12 条跳转路由**全部命中**（含 `/ai/execute-log` 为 AI智能中心直属一级菜单，无父级前缀）。

**遗留（待定，未改）**：`5237 知识中心` 为 `M` 目录却带 `component='ai/knowledge-center/index'`
——运行时不会用到（`buildMenus` 对 M 只递归 children），属冗余配置，可按需清空。

**校验**：管理端 `npm run build:prod` **exit 0**（`✓ built in 2m 2s`，dist 563 文件）；
后端 `mvn -o test` **422 例全绿**；`git reflog` 无回退类操作。

## v13.30 (2026-09-28) 开发规范评审整改：版本表去硬编码 + 明确"唯一"效力边界

**背景**：对《项目开发规范》做独立评审（与 `pom.xml` / `package.json` 逐项对账），
确认两项风险并整改。**其余评审结论不改**（测试策略/依赖治理/事故响应等标准域缺口、
`§1.15` 缺"跑测试"、铁律覆盖不一致等，用户裁定暂不处理）。

**① R1：版本表是手工镜像 → 改为「版本策略 + 唯一事实来源」**

**问题实证**：`§1.1 技术栈` 与 `§2.1 数据库选型` **两处**都写 `MyBatis-Plus 3.5.7`，
实际 `pom.xml` 为 **3.5.11**（**根 README 也是 3.5.11 → 规范与自家 README 自相矛盾**）；
`§4.1.1` 写 `Axios 0.27.2`，实际 `package.json` 为 **`^1.7.9`**——
**跨大版本**：取消请求 API 在 v1 已由 `CancelToken` 改为 `AbortController`
（代码已在用 `new AbortController()`，见 `stream.js:31`、`chat.vue:344`），
照旧版本号写代码会**直接踩空**。

**整改**：
- 删掉全部具体版本号，表格列由「版本」改为「**版本策略**」，只保留**兼容基线**
  （如 `3.3.x 系，禁止降级`、`由 Boot 依赖管理托管`、`不限`）；
- 三处表格各自加注：后台以 **`moyun-server/pom.xml`** 为唯一事实来源；前端三端以各端
  **`package.json`** 为唯一事实来源；**禁止在文档 / 注释 / README 里复制具体版本号**
  （手工镜像必然漂移），并给出查询命令
  `mvn -o help:evaluate -Dexpression=<artifactId>.version -q -DforceStdout`；
- `§4.1` 补**主版本基线**：Vue 3.x · Vite 5.x · Vue Router 4.x · Pinia 2.x(管理端)/3.x(门户端) ·
  **Axios 1.x（禁用已移除的 `CancelToken`，统一 `AbortController`）**；
- 正文 `§3.1` 的「Spring Security 6.3.1」→「版本由 Spring Boot 依赖管理托管」；
- **保留**两处"漂移实证"作为反面警示（写明"曾写 X、实际 Y"）——这正是禁止硬编码的理由。

**② R4：文档两处自述"唯一"、边界未定义 → 明确分工**

**问题**：本文档写"唯一规范来源"，现状总结写"全项目唯一的铁律清单"，两者对铁律的覆盖不一致
（铁律 34 条完整在现状总结；本文只引用 17 个守卫中的 11 个，"git 回退禁令"仅在 `§1.15` 自检出现）。

**整改**（双向写清，互为指针）：
- **规范头部**新增「📐 效力边界」块：本文 = **唯一「技术实现规范」来源**（"怎么做"）；
  现状总结 = **唯一「项目铁律」来源**（"必须遵守什么、违反即失败"）；
  **铁律是上位规则、规范是下位实现**——判定"是否违规"看铁律，判定"具体怎么写"看规范；
  并给出全项目效力顺序：**代码 > devlog > 项目现状总结（含铁律）> 本规范 > 文档索引 > 其余文档**。
- **现状总结铁律节**同步补同款边界表（互为指针）；铁律 9「分工」改写为
  「**分工与效力顺序（唯一性边界）**」，明确两份文档各管一段、看似冲突时以铁律为准。

**版本升级（按铁律 7）**：`全端-规范-项目开发规范-V13.29.md` → **`-V13.30.md`**；
全仓 **10 个文件共 16 处**引用同步改名（含 `docs/README.md`、主 README、三端 README、现状总结 3 处）。

**校验**：文档内**已无硬编码的具体版本号**（仅保留 2 处"曾写错"的实证引用与 `MySQL 5.x/8.x` 能力说明）；
全仓 `.md` 相对链接 **0 失效**。

## v13.29 (2026-09-28) 开发铁律重新整理为全局统一口径（34 条 · 七组）+ 新增「严禁私自 git 回退」

**需求**：铁律不再"各个版本都保留"——把历史上按版本逐条堆叠的《开发铁律》（18 条）与《变更铁律》（9 条）
**合并重排为一份全局统一口径**，后续一律遵循；并在其中加入**最高优先级**的
**「严禁私自把 git 回退到历史版本」**（必须人工确认，唯一例外是上一步刚提交且已明确确认错误），
避免代码在无形中被回退。

**① 结构重组：18 + 9 条 → 34 条 · 七组（唯一口径）**

| 组 | 条数 | 铁律号 | 内容 |
|---|---|---|---|
| 一、变更与流程 | 5 | 1–5 | **git 回退禁令（新）**、devlog 逐次记录、四同步、提交前对照 git 自检、代码是唯一事实来源 |
| 二、文档 | 4 | 6–9 | 只写可验证行为、版本随修改升级（端-模块-功能-版本）、方案按模块归位/滞后即删、三者分工与效力顺序 |
| 三、SQL 双轨 | 2 | 10–11 | init-sql 与 increment-sql 必须同步修改（含菜单单点维护 + 回查现网）、增量脚本必须可重跑 |
| 四、数据库口径 | 5 | 12–16 | 金额 `decimal(18,2)`（元）、软删 `del_flag`、collation `0900_ai_ci`、默认值 fail-closed、日期列用 `date` |
| 五、代码与模块 | 9 | 17–25 | 统一收口、模块依赖方向/防腐层端口、清 `target/classes`、可变 Map、外部数据隔离、`${}` 信任来源、`sys_config` 热开关、请求级记忆化、LLM 解析单入口 |
| 六、事务 | 2 | 26–27 | 事务只包 DB 写、回滚口径必须显式 |
| 七、并发与安全 | 8 | 28–34 | 增量写校验影响行数、按编号取资源校验归属、CORS 不可伪造 pattern、WS 票据替代 URL token、域名不留占位、资金幂等键叠加 `user_id`、异步自调用走代理 |

**去版本分层**：每条**不再保留"（v13.x）"式版本标记**，改写为长期有效表述；
历史批次仅在必要处保留为溯源说明，并明确"**不表示该条只适用于那个版本**"。
原《变更铁律》9 条与《开发铁律》18 条**不再并存**，全部并入本节。

**② 新增铁律 1：严禁私自把 git 回退到历史版本（最高优先级）**

- **绝对禁止**：`git reset --hard`、回到历史提交的 `git checkout`、`push --force`、`git rebase` 改写历史、
  `git stash` 反向覆盖工作区、`checkout -f` / `clean -fd` 丢弃改动，以及**任何形式用历史版本覆盖当前代码**；
- **唯一例外（纠错窗口）**：上一步刚提交、且**已明确确认该提交是错的**——可撤销，属纠错而非回退；
- **例外之外必须人工确认**：先说明"为什么必须回退 / 影响哪些文件 / 如何验证恢复后正确"，
  经人工确认后才执行；**AI 助手不得自行决定并执行回退，也不得混在其它改动里一起做**；
- **优先"向前修正"**：撤销已确认错误的历史提交用 `git revert`（新增反向提交，历史完整可再恢复）或写修正提交，
  **绝不改写历史**；
- **交付前自查**：`git reflog` 不应出现本次会话的 `reset`/`checkout <commit>`/`rebase`/`revert` 记录
  （除已确认纠错窗口）；有则须在 devlog 说明原因与人工确认过程。
- 本次自查：`git reflog` 近 50 条**无任何回退类操作**（仅有历史分支切换），合规。

**③ 新增「铁律 ↔ 守卫对照表」**

把 17 个守卫/回归测试类映射到对应铁律（如 `DdlConventionGuardTest` → 铁律 12–15、
`TransactionRollbackRuleGuardTest` → 铁律 27、`AggregateIncrementGuardTest` → 铁律 28），
并确立：**新增铁律必须同时提供守卫，或在对照表注明"无守卫（人工评审项）"**——
无守卫的铁律容易在迭代中静默失效；守卫的新增/删除属铁律级变更，需单独登记 devlog。

**④ 口径单点化（避免多处复制导致漂移）**

- `项目开发规范` 的「变更管理四项」→ 改为「**铁律自检**」勾选清单，**唯一口径指向**现状总结，
  并补首项「未做任何 git 回退」；§2.8「SQL 双轨」与 §1.15 自检项改为引用**铁律 10 / 11 / 2**；
- README 横幅「变更铁律见 00-项目现状总结」→「**开发铁律（唯一口径）见 项目现状总结**」。

**⑤ 版本升级（按铁律 7 文件名带版本）**

- `全端-规划-项目现状总结-V13.28.md` → **`-V13.29.md`**（文内基线标注同步 v13.29）；
- `全端-规范-项目开发规范-V13.28.md` → **`-V13.29.md`**（文内"当前对齐"标注 + 最后更新日期同步）；
- 全仓 **10 个文件共 19 处**引用同步改名（含 `docs/README.md`、主 README、三端 README）；
- 顺带把当前文档里的**旧标签** `[00-项目现状总结]` → `[全端-规划-项目现状总结]`（5 处，含 1 处正文明引）。

> 保留不改的两类（按既定口径）：`devlog` 历史条目里的旧文件名（**历史条目保留当时文件名，不追改**）、
> 评审报告中的旧文件名（**属对当时仓库状态的历史叙述**，改了反而失真）。

**校验**：铁律编号 **1..34 连续无跳号**；分组条数 5+4+2+5+9+2+8 = 34 一致；
全仓 `.md` 相对链接 **0 失效**；后端 `mvn -o test` **422 例全绿**。

## v13.28 (2026-09-28) 文档治理二期：全量文档「端-模块-功能-版本」命名 + 雷同/已落地文档归档

**要求**（用户裁决）：保留主干最新版本，雷同/同一修改方向的删除；全部文档按「端-模块-功能-版本」
（端 ∈ 全端/后端/门户/后台/记账App）重命名；**后续文档修改必须同步升版本号**——已写入
《项目现状总结》变更铁律第 8 条与 `docs/README.md` 维护规则第 5 条。

**① 归档 8 份（→ `.archive/docs-obsolete-20260928/`，保留原目录层级）**：

| 文档 | 删除理由 |
|---|---|
| `05-方案设计-分模块/1-AI底座/AI能力统一接入层 — 完整方案文档.md`（V2.0 · 09-08 · 待评审） | 核心设计"每场景独立 Handler + LlmClient 收编前"已被 v13.0 配置驱动否决；与《统一网关演进史》同一方向且被其收敛 |
| `05-方案设计-分模块/2-记账模块/记账财务分析整改方案_0925_v2.md` | 一次性整改方案，已落地（v13.2 收口） |
| `05-方案设计-分模块/3-vip体系/VIP支付模块全面评审报告.md`（09-18） | 评审结论已被三轮验证评审（09-26，含数据库/支付专项）+ v12.3/v13.x 整改覆盖 |
| `09-临时报告/报告一~五`（5 份，09-22） | 与三轮验证评审同一修改方向；后者已逐条订正其过时结论（报告四:91/报告五:45），P1 跟踪由其 §六 + 交付验证清单接管 |
| `08-简历/…修改版 - 副本.docx` | 纯副本，直接删除（未归档） |

**② 全量重命名**（23 份，`端-模块-功能-版本`；devlog 与 docs/README.md 作为日志/索引载体保留原名）：
项目介绍/技术架构/链路导读 → `全端-架构-*` / `全端-AI底座-链路导读-V13.0`；
开发规范 → `全端-规范-项目开发规范-V13.30`（升版：修正 1 处失效旧目录 `docs/11-记账模块需求分析/`）；
部署三件 → `全端-部署-*`；AI 底座三份 → `全端-AI底座-*`；记账三份 → `记账App-*`；
VIP 方案 → `全端-VIP-体系设计方案-V2.1`（对齐文内实标 v2.1，自引用路径同步修正）；
面试实施文档 → `门户-语音面试-全链路实施文档-V6`（头部文档版本 v2.0 → v6.0，V2 基线 + V3~V6 章节累计）；
现状总结/进度规划 → `全端-规划-*-V13.28`（升版：铁律第 8 条新增 + 里程碑补 v13.28）；
三轮验证评审 → `全端-评审-架构代码三轮验证-V20260926`；交付清单 → `全端-整改-交付验证清单-V13.24`；
后台菜单重构 → `后台-菜单-模块划分重构-V13.26`；题集两份 → `全端-题集-*-上/下篇-V1.0`；
perf 基线 → `全端-AI网关-性能成本基线-V20260924`。

**③ 引用同步**：`docs/README.md` 索引按新名重建；《项目现状总结》附·文档索引中已删除的
「AI能力统一接入层方案」改指《统一网关演进史》，P1 清单指引同步新文件名；根 `README.md`、
技术架构/项目介绍/开发规范/部署指南/演进史/交付清单等全部交叉链接批量替换。
**历史条目不追改**：devlog 既有条目与三轮验证评审正文中出现的旧文件名按"不追改历史"原则保留。

## v13.27 (2026-09-27) 文档治理：以版本/日期为准清理滞后文档 + 重建文档索引

**要求**：文档必须与代码**同向演进**；判断文档是否可用先看其「版本 / 修订日期」；
**严重滞后的直接删除**，不得以老版本文档去约束新代码；devlog 作为变更铁律载体必须保留。

**① 建立文档效力顺序**（写入 `docs/README.md`）：

> 代码 > devlog > 00-项目现状总结 > docs/README 索引 > 其余文档；冲突一律改文档。

**② 删除 4 份严重滞后文档**（删除前归档至 `.archive/docs-obsolete-20260928/`，可恢复）：

| 文档 | 自称版本/日期 | 删除理由 |
|---|---|---|
| `docs/02-开发指南/开发指南.md` | v9.0 · 2026-08-15 | 内容为环境要求 + RuoYi 基础模板，与「项目开发规范 + 部署指南」重复且滞后 |
| `docs/04-测试验收/功能排查清单.md` | v10.6 · 2026-08-21 | 未覆盖 v12.0 统一 VIP；`VIP支付模块全面评审报告` 已判定其"与代码严重脱节、仍引用旧 Controller" |
| `docs/09-临时报告/AI统一网关架构复杂度评估.md` | 无版本 · 2026-09-23 | 一次性评估；其结论已被 v13.0「配置驱动」定论覆盖，无任何现行文档引用 |
| `ledger-review-report.md`（仓库根目录） | 无版本 | 记账评审报告，结论已落地为 v13.1/v13.2 整改；文档树内无归属 |

**③ 阶段方案归档的处置**：`报告六` 曾登记 `docs/02-技术方案/` 为「阶段方案归档，**不动**」——
其记录的是 AI 网关改名沿革（`ai2 → aiapp → aigateway`）与整改论证过程。**经用户裁决改为"收敛后归档原件"**：

- 新建 `docs/05-方案设计-分模块/1-AI底座/AI统一网关-演进史与现行实现.md`：时间线（v11.x 问题期 →
  v12.2 统一入口 → v12.2.x 清理收编 → v13.0 配置驱动定案）、**已否决的设计清单**（逐场景 Handler /
  TTL 缓存场景配置 / 业务端 LlmClient / admin 端收编 / 会话意图分类）、**现行实现逐条对照**
  （handler 仅 2 个 SPI 文件、`DefaultSceneExecutor`、`executeStream` 已落地、
  `ai_scene_config.version` + `_history` 快照 + 会话版本锁）、以及抄录的 7 条开发规约；
- 三份原件（`AI统一入口整改方案`、`…整改方案（完整版）-v2`、`…评审结论与最终执行计划`）
  移入 `.archive/docs-obsolete-20260928/docs/02-技术方案/`，`docs/02-技术方案/` 空目录移除；
- 索引改指向演进史；`docs/README.md` 目录结构表同步（去除 `02-技术方案` 行）。

> 归档理由：作为"待执行方案"已失效（描述与现行配置驱动实现不一致，包名 `aiapp` 已不存在），
> 但改名沿革与整改论证有参考价值 → 收敛为一份演进史，原件保留于 `.archive/`。

**④ 库表以代码为准对账（发现并修复 1 处真实缺陷 + 清理 9 张死表）**

对账方法：全仓扫 `@TableName` 实体（**164 个**）↔ 现网库（**200 张 BASE TABLE**）↔ `moyun-db-ddl.sql`（原 **187** 张），
逐表用「实体反查 + 关键字精确统计（Java/XML/前端）」定引用数。

**④-1 修复真实缺陷：`sys_config_log` 漏建（P0，功能不可用）**

- `SysConfigServiceImpl.updateConfig` 在**同一事务内**写审计：`configLogMapper.insert(configLog)`（L193）；
- 但**现网库不存在该表**（DDL 有定义 → 属"库漏建"）；
- 后果：后台【参数设置】任何一次修改都抛 `Table 'moyun-db.sys_config_log' doesn't exist` 并**整体回滚** → 参数改不了；
- 处置：新增 `increment-sql/20260928-08-补建漏建表sys_config_log.sql`（列/索引/注释与 DDL 一致，
  `information_schema.TABLES`/`STATISTICS` 前置判断，连跑两次 exit 0）。

**④-2 删除 9 张死表（表 + 实体 + Mapper + DDL 定义全链路）**

判定：**无实体、或实体无任何引用** → 死表（用户裁决"以代码为主，实体存在没引用也是要删的表"）。

| 表 | 引用数 | 遗留数据 |
|---|---|---|
| `ai_chart_recommendation_rule` | 0 | 7 行 |
| `ai_sql_template` | 0 | 5 行 |
| `portal_task`（实体 `PortalTask` 无引用） | 0 | 7 行 |
| `ai_data_insight` / `ai_document_chunk_metadata` / `portal_book_chapter_view` / `portal_order` / `portal_user_task` | 0 | 0 |
| `sys_job_scan_issue`（实体+Mapper 均无引用；原 `system/scan` 页已删） | 0 | 0 |

- 代码侧同步删除：`PortalTask.java`、`SysJobScanIssue.java`、`SysJobScanIssueMapper.java`（+ 同名 XML）；
- DDL 同步删除 `portal_task` / `sys_job_scan_issue` 两处 `CREATE TABLE`（**187 → 186**；DDL 不含 5 张 `_bak_` 迁移备份表）；
- 新增 `increment-sql/20260928-09-清理死表（无实体或无引用）.sql`（`information_schema` 前置判断 + `DROP TABLE IF EXISTS`，连跑两次 exit 0）；
- 数据可恢复：3 张有数据的表已 `mysqldump` 至 `.archive/drops-20260928/`；
- 现网库：**200 → 192 张**（含 5 张 `_bak_`）⇒ 业务表 187 = DDL 186 + `sys_config_log`（本批补建）。

> ⚠️ 排查过程记录：删除实体/Mapper 后首轮测试出现 30→124 个错误，根因是
> **`target/classes` 残留旧 `SysJobScanIssueMapper.xml`** 导致 MyBatis 解析失败并级联污染上下文；
> `mvn -o clean test` 后 **422 例全绿**。印证《00-项目现状总结》铁律 10 的提示
> （"移动/删除类后必须清 `target/classes`"）。

**⑤ 修正 `开发进度与规划.md` 的滞后表述**：表数 187→以库为准、DML"4 分片"已合并为单文件、
增量脚本最新号、`TD-04` 标记为已完成（v12.1 + v13.8）、路线图由 `v12 Phase` 更新为 `v13 起`、
补 v13.x 关键转折与后台菜单重构里程碑。

**本轮新增脚本**：`20260928-08-补建漏建表sys_config_log.sql`、`20260928-09-清理死表（无实体或无引用）.sql`。

**④ 重建 `docs/README.md` 文档索引**：原索引与实物严重脱节（列了 v9.0 版本文档、漏登记整个
`02-技术方案/` 与 `AI底座与调用场景链路导读`、Elasticsearch 方案链接漏 `1-AI底座/` 层级、
`10-大模型配置相关/` 目录实际不存在）。新索引：

- 按**实际目录**重列，**每份文档标注时效**（版本/日期或"内容随 v13.x 增补"）；
- 修正全部失效链接；新增「维护规则」章节（四同步 + **严重滞后直接删除**）；
- `01-架构设计/项目介绍.md`、`00-项目现状总结.md`、主 `README.md` 中指向已删文档的链接同步修正。

**⑤ 题集归位**：`docs/` 根下两份「项目自检 & 面试问题全集」移入既有空目录 `docs/10-相关题集/`。

**⑥ 修正文档内失效绝对链接**：`项目开发规范` 2 处 `file:///workspace/...`（AI 编辑器生成的
临时路径）改为仓库相对路径。

**校验**：`docs/**` 全部 markdown 相对链接逐个解析 → **0 失效**；主 README 的 docs 链接 → **0 失效**。

**⑦ 文档树对齐「带版本文件名」规范（用户既定规范）**

用户已把 `docs/` 全量重命名为 **`{范围}-{模块}-{类型}-{版本}.md`**（范围如 `全端`/`门户`/`后台`/`记账App`；
版本为 `V13.28`（随项目版本）或 `V1.0`（独立方案自版本））。据此收尾：

- **重写 `docs/README.md` 索引**：按实际文件树逐条重列（原索引仍用旧文件名 → 曾产生 **50 处失效链接**），
  保留「文档效力顺序」「每份标注时效」「维护规则」，并**新增命名规范说明**与规则 5「文件名带版本，升版同步改名」；
- **修正全部交叉引用**：`项目现状总结` 内 AI 网关方案链接改指 `全端-AI底座-统一网关演进史-V13.0.md`；
  `项目介绍` / `项目现状总结` 内部署指南链接改指新文件名；
- **文件名与内容版本对齐**：`全端-部署-部署指南-V13.27.md` → **`-V13.28.md`**（内容已是 v13.28，消除文件名倒挂）；
- **版本号统一为 v13.28**：本条目原记 v13.27，但用户新建文档已用 V13.28，故 README 横幅 / devlog 条目 /
  `项目开发规范` / `部署指南` / `开发进度与规划` 五处统一为 **v13.28**，消除版本倒挂。

**⑧ 按用户定稿的文档树收尾（v13.28 文档治理二期）**

用户随后把 `docs/` 正式定稿（索引含「文档效力顺序」+ 每份时效标注 + 维护规则 5「版本随修改升级、雷同文档只留主干」），
并按细则修正残留：

- **`开发进度与规划` 数字对齐实物**：表数 `198 张 BASE TABLE`（错）→ **DDL 186 张 + `sys_config_log` 由 `20260928-08` 补建**；
  补现网实测口径（**192 张**含 5 张 `*_bak_*` ⇒ 业务表 **187**）；增量脚本最新号 `20260928-07` → **`20260928-09`**；
  补记 v13.28 库表对账删 9 张死表；
- **修正指向上不会被解析的旧文件名**：`项目开发规范` L443 「完整定义见 `00-项目现状总结.md`」→
  `全端-规划-项目现状总结-V13.29.md`（原为失效引用）；L7 删去"原开发指南.md 已删除"的过期旁注；
  `项目现状总结` 文末索引 5 行标签改为新文件名；
- **修正各端 README 的 7 处死链**（共 14 处替换）：`moyun-admin-vue` / `moyun-portal` / `moyun-server` 内
  `../docs/02-开发指南/开发指南.md`、`../docs/02-开发指南/项目开发规范.md`、`../docs/03-部署运维/部署指南.md`、
  `../docs/01-架构设计/技术架构.md` 全部改指新文件名。

**校验**：全仓所有 `README.md`（含 `docs/README.md`）相对链接逐个解析 → **0 失效**。

**校验**：`docs/**`（含 html）相对链接逐个解析 → **0 失效**；主 README 的 docs 链接 → **0 失效**；
后端 `mvn -o compile` → **BUILD SUCCESS**；五处版本号一致。

**⑨ 菜单脚本收敛：删除冗余的 `20260928-01`（菜单结构单点维护于 `moyun-menu-redo.sql`）**

用户裁决：菜单已在 `moyun-menu-redo.sql` 中整理完毕，该增量脚本不再需要。
**删除前逐项验证其终态是否已被 redo 完整包含**（方法：提取脚本全部操作特征 → 在 redo 中查找）：

| 验证项 | 结果 |
|---|---|
| 脚本新增/改写的 **27 条**菜单与权限行 | ✅ redo 中 **38 个**相关菜单名（含 12 个 F 按钮行）**全部存在，缺失 0** |
| 内容管理 `order_num` 1~13 与审核中心 order 重排 | ✅ redo 中 order 一致 |
| 删除的 `5152 内容审核中心` | ✅ redo 中已不存在（由 `05` 归并为唯一「审核中心」） |
| 删除的 `5145/5146` | ✅ redo 中存在（由 `06` 恢复，非重复功能而是 TabContainer 分组页） |
| 监控去重（`111/112/113/114`） | ✅ redo 中「数据监控/缓存监控/缓存列表」均已不存在，仅留「服务监控/缓存管理」 |
| 超管角色授权 | ✅ redo 末尾以动态 `INSERT...SELECT` 全量授权，等效且更强（永不悬空） |
| 非菜单内容（DDL/其他表） | ✅ 无——脚本仅操作 `sys_menu` / `sys_role_menu` |

结论：**无任何未收录内容** → 删除 `increment-sql/20260928-01-后台菜单按前台五大主线重组.sql`。
同步更新 `README` 脚本清单（移除该行）与本节 §⑥ 的「菜单脚本」表述；
`09-临时报告/后台-菜单-模块划分重构-V13.26.md` 的脚本表按次轮一并核对。

> **口径确立（v13.28 起）**：**菜单类变更不再走增量脚本**——菜单结构由 `init-sql/moyun-menu-redo.sql`
> 单点维护（新库一次性全量重建；显式 `menu_id` + F 行自增 + 超管动态授权）。
> 已有库的菜单调整请在后台【菜单管理】直接改，或按需新增一次性脚本；**避免增量脚本与初始化脚本
> 两套 id 空间并存**。`20260928-03/04/05/06/07` 同属菜单类，其终态亦已全部收录于 redo（保留仅作历史留痕）。

**⑩ 修复菜单 path 重复导致的路由冲突（404）**

发现方式：用户给出的重复 path 巡检 SQL

```sql
SELECT a.path, COUNT(*) FROM sys_menu a
 WHERE a.path IS NOT NULL AND a.path <> '' AND a.path <> '#'
 GROUP BY a.path HAVING COUNT(*) > 1;
```

**根因**（不是"同名 path 本身"，而是**路由 name 冲突**）：后端 `SysMenuServiceImpl.getRouteName()`
取 **`capitalize(menu.path)`** 作为 vue-router 的 `name`；**不同父节点下的同名 path 段会产生同一个 name**，
vue-router 4 先注册者被后注册者覆盖 → 该菜单点击 404。递归全路径扫描出 **4 处碰撞**：

| 路由 name | 既有（保留） | 冲突方（改 path） |
|---|---|---|
| `Log` | `108 日志管理`（path=`log`，含 4 个子项） | `5615 成长记录` `log` → **`growth-log`** |
| `User` | `100 用户管理`（path=`user`） | `5614 用户成长` `user` → **`growth-user`** |
| `Tool` | `3 系统工具`（path=`tool`） | `5024 工具管理` `tool` → **`tool-config`** |
| `Category` | `5085 分类管理`（path=`/category`） | `5401 预设分类` `category` → **`ledger-category`** |

**改动**：仅改 `path`（parent_id / order_num / component / perms 全部原样），
同步落两处——**`init-sql/moyun-menu-redo.sql`（单点维护）** 与 **现网库**（4 条 `UPDATE`，带
`AND path='旧值'` 条件，幂等复跑 0 行受影响）。`成长管理` 子项路径统一为 `growth-*` 前缀，与
`growth-config` 命名风格一致。

**校验**：
- `redo` 实测导入临时库 → 用户那条巡检 SQL **返回空**；`menu_id` 总数 **396**、超管授权 **396** 不变；
- 现网库同查询 **返回空**；递归全路径 name 碰撞复检 → **121 项全唯一，0 碰撞**；
- 全仓无 `router.push({name:...})` 用法 → name 碰撞只影响路由注册，`path` 唯一化即为充分修复。

> 附：本次顺带确认两处**不是**碰撞（前端只做两级拼接，不会三级）：
> `5024 工具管理` 真实路由 `/ai/ai-config/tool`、`5614 用户成长` 真实路由 `/system/growth/user`
> —— 但因其 **path 段**与既有项同名，**name 仍会冲突**，故一并改名（上表已含）。

**⑪ 「只跑 init-sql 即可初始化项目」达成 + 现网库结构补漏（SQL 双轨铁律确立）**

用户口径：**保证执行 `moyun-menu-redo.sql` 就能初始化项目使用**——把增量脚本的内容合并进
`init-sql`（建表 / 初始化数据）；后期修改**增量与 init-sql 同步改**：增量用于执行、init-sql 用于同步，
并**写入开发铁律**。

**验证方法（新增标准动作）**：在临时库仅用 `init-sql` 三件套重建，与现网库
**逐表 / 逐列（类型·可空·默认值·collation）/ 逐索引**比对，差异须为 0。

**⑪-1 发现并修复 **168 处**现网库漏迁移（脚本头声称"dev 库已执行"但现网未生效）**

| # | 问题 | 规模 | 处置 |
|---|---|---|---|
| 1 | `collation` 归一未执行（`general_ci`/`unicode_ci` 未转 `0900_ai_ci`） | **160 列** | 补执行既有 `20260927-03`（脚本本就在，只是从未在现网跑过） |
| 2 | `pay_*` 金额列仍是 `bigint`（分），未转 `decimal(18,2)`（元） | **8 列** | 新增 `20260928-11`（无损宽化，值不变：`450` → `450.00`） |
| 3 | 记账幂等唯一键与代码注释要求不一致 | **3 处索引** | 新增 `20260928-10`（见下） |

**⑪-2 记账幂等唯一键对齐（`20260928-10`）**

现网为 `uk_client_uuid(client_uuid)` 全局唯一 / `idx_task` 普通索引；**代码注释即目标态**：

- `LedgerTransactionServiceImpl` L81-82：「索引是 **`(user_id, client_uuid)` 复合唯一而非 `client_uuid` 全局唯一**
  —— clientUuid 由客户端生成，全局唯一会让两个用户偶然撞同一 uuid 时报错」；
- `LedgerTipServiceImpl` L62-63：「**必须叠加 userId 约束**……仅按 clientUuid 查询会命中他人的 pending 单，
  随后复用其 userId/amount 下单」（越权隐患）；
- `LedgerScheduleServiceImpl` L364-365：应用层查重 +「数据层 `uk_user_client` 唯一索引兜底并发」。

→ `ledger_transaction` / `ledger_tip_order`：`uk_client_uuid` → **`uk_user_client(user_id, client_uuid)`**；
`ledger_schedule_log`：`idx_task` → **`uk_task_date(task_id, exec_date)`**。
脚本含「先加新键再删旧键」（不留并发窗口）+ 唯一键冲突数据前置检查（`SIGNAL` 中止），连跑两次 `exit 0`。

**⑪-3 init-sql 完整性核验与收尾**

- **DDL 已包含全部结构增量**（题库分类 4 列、`vip_user_card` 唯一键、`ai_execute_log.token_estimated`、
  `portal_user.birthday` 等逐项抽查命中），**DML 已含 70 条 INSERT 种子**（含 `portal_interview_category`、
  `sys_platform`）——故**无需再搬移**，原有内容已就位；
- **删除 DDL 中唯一残留死表定义** `portal_resume_optimize_task`（185 张）——代码注释明确
  「原 `portal_resume_optimize_task` 表**停止写入**（已切换到 `portal_ai_task`）」，且全仓无实体/Mapper/类；
- **修正 2 处误删（本轮自查发现并已恢复）**：
  `portal_resume_job_match`（`ResumeJobMatchService` 在用，15 行）、
  `portal_resume_optimize_history`（`ResumeDeepOptimizeService` 注入其 Mapper，4 行）
  —— 根因是死表检测只搜 `@TableName` 与文本引用，**漏了"通过 Mapper/Service 注入使用"的表**；
  已建立补充判据：**扫全部 162 个 `BaseMapper` → 实体 → `@TableName`，与现网表集合比对，缺表即报错**
  （现结果为"无缺失"）。数据已从 `.archive/drops-20260928/` 回填。

**⑪-4 最终核验结果（全绿）**

```
表清单      fresh=185  live(去 5 张 _bak_)=185   → 一致
逐列比对    类型/可空/默认值/collation            → 0 差异
索引比对                                          → 0 差异
```

**⑪-5 写入铁律**

- 《项目现状总结》变更铁律**新增第 9 条「初始化脚本与增量脚本必须同步修改」**（含菜单单点维护、
  可重跑、**"已执行"必须回查现网库**）；
- 《项目开发规范》§2.8 新增同款「SQL 双轨制」小节 + 建表自检清单补 3 项
  （两边都改 / 连跑两次 / 已回查现网）；
- README 脚本清单补 `20260928-10`、`20260928-11` 及其说明，DDL 表数 187 → **185**。

**本轮新增脚本**：`20260928-10-记账幂等唯一键对齐（uk_user_client与uk_task_date）.sql`、
`20260928-11-支付金额列类型对齐（bigint转decimal）.sql`。

## v13.26 (2026-09-27) 后台菜单按门户前台五大主线重组（学习/简历/面试/阅读/社区）

**需求**：后台菜单与门户前台首页分类不对齐——题库挂在「面试管理」下（前台 `/learn/questions`
与 `/interview/questions` 双路由都指向它），混装页把多个面板塞进一个菜单，且前台多个区块
**在后台没有任何配置入口**。要求：该独立成菜单就独立、该补页面就补、该清就清。

**① 拆分混装页面（3 个 Tab 容器 → 拆为独立菜单）**

| 原页面 | 拆分结果 |
|---|---|
| `cms/interview/questionTab`（题库资源，4 面板混装） | 题库管理 / 题目分类 / 公司标签 / 简历模板（各自独立菜单） |
| `cms/interview/experienceTab`（面经运营，2 面板） | 面经管理 / 面经评论 |
| `portal/bookListTab`（书单&推荐位，2 面板） | 书单管理 / 推荐位管理 |

三个 Tab 容器组件已删除（`views/cms/interview/questionTab`、`views/cms/interview/experienceTab`、
`views/portal/bookListTab`），无残留引用。

**② 菜单结构调整（对齐前台五大主线）**

| 门户管理 → | 内容 | 说明 |
|---|---|---|
| 内容管理 | 文章/分类/标签/专栏/话题/评论/帮助/友链/举报/反馈/成长配置 | 与前台「社区」对齐，重排 order |
| 创作者认证 | 认证审核 | 不变 |
| **简历管理**（新增父菜单） | 简历模板 | 对齐前台「简历」主线 |
| 面试管理 | 面经管理/面经评论/精选笔记/语音面试/岗位模板/面试配置 | **题库与简历模板已迁出** |
| **学习管理**（原「学习管理/book」改为 learn） | 题库管理/题目分类/公司标签/错题本/学习计划/学习辅助 | 对齐前台「学习」主线 |
| **阅读管理**（新增父菜单） | 书籍/章节/书单/推荐位/金句与用户内容 | 对齐前台「阅读」主线 |
| 审核中心 | 审核中心 | order 后移 |

系统设置下**新增「成长管理」父菜单 + 5 个子页面**（成长规则/徽章/成就/用户成长/成长记录）——
`views/cms/growth/*` 5 个页面与 `CmsGrowthController`(`cms:growth:*`) **早已存在但 sys_menu 中从无菜单**，
除超管（`SysUser.isAdmin` 绕过 role_menu）外任何角色都进不去。

**③ 补齐缺失入口**：错题本（`/learn/wrong`）、学习计划（`/learn/plan`）此前**只有权限码、无菜单**，
页面文件已存在但无法进入；本次补菜单。题库分类页新增「考试类型 / 上级分类 / 职业族群 / 前台展示」字段。

**④ 权限码归位**：`CmsInterviewController` 的分类与公司接口原先复用通用 `cms:interview:list/query/add/edit/remove`，
改为专用 `cms:interview:category:*` 与 `cms:interview:company:*`，并同步补 `sys_menu` 功能权限行（8 条），
使「菜单权限」与「接口权限」一一对应。

**⑤ 题库分类数据结构扩展**（支撑按职业/行业考试类型拓展）

- `portal_interview_category` 新增 4 列：`bank_type`（面试/职业资格/公务员/考研/其他）、
  `parent_id`（两级分类）、`job_family`（职业族群）、`visible`（前台展示）；新增 2 个索引
- 增量脚本 `increment-sql/20260928-02-题库分类扩展考试类型与两级分类.sql`（逐列 `information_schema`
  前置判断，dev 库连跑两次均 exit 0）；同步更新 `init-sql/moyun-db-ddl.sql` 的 `CREATE TABLE`
- 后端 `PortalInterviewCategory` / `InterviewCategoryVO` / `PortalInterviewCategoryMapper.xml` 补字段与筛选条件
- 后台分类页支持按题库类型筛选、两级树展示、上级分类选择、前台展示开关

**⑥ 后台首页「查看更多」跳转修正**

原先多个链接指向不存在的路由，逐个修正并**逐一核对到真实菜单路径**：

| 模块 | 原链接（错） | 现链接 |
|---|---|---|
| AI 语音面试 | `/cms/interview` | `/portal/interview/voiceInterview` |
| 简历中心 | `/cms/resume` | `/portal/resume/template` |
| 学习中心/OJ | `/cms/interview/question` | `/portal/learn/question` |
| 成长体系 | `/cms/growth`（无此菜单） | `/cms/growth/rule` |
| 个人记账 | `/cms/ledger`（无此菜单） | `/ledger-app/category` |
| AI 财务分析 | `/cms/ledger/analysis`（不存在） | `/ledger-app/stats` |

同时修正前端 stale `activeMenu`：`/portal/book` → `/portal/reading/book-index`、
`/portal/interview/questionTab` → `/portal/interview/question`（`router/index.js` 与 `testCase` 页回退路径）。

**菜单脚本**：菜单结构此后由 **`init-sql/moyun-menu-redo.sql`** 单点维护。
原 `increment-sql/20260928-01-后台菜单按前台五大主线重组.sql` 已于 **v13.28 删除**（见末尾「菜单脚本收敛」）。

**⑦ 补齐 4 个「有页面/有接口/有权限码，但无菜单」的模块**（同 §② 成长管理的同类缺陷）

| 模块 | 页面 | 后端 Controller | 权限前缀 | 归位 |
|---|---|---|---|---|
| 竞赛管理 | `cms/contest/index` | `CmsContestController` | `cms:contest:*` | 门户管理/内容管理 |
| 广告位管理 | `cms/ad/index` | `CmsAdSlotController` | `cms:ad:*` | 门户管理/内容管理 |
| 写作提示词 | `cms/prompt/index` | `CmsWritingPromptController` | `cms:writing-prompt:*` | 门户管理/内容管理 |
| 导入模板配置 | `cms/importTemplate/index` | `CmsImportTemplateController` | `cms:importTemplate:*` | 系统设置/系统工具 |

四者页面、前端 API、后端 Controller 与 `@PreAuthorize` 权限码**全部齐备**，唯独 `sys_menu` 无菜单行——
除超管（`SysUser.isAdmin` 绕过 `sys_role_menu`）外任何角色都进不去，且页面内按钮因权限码未下发而全部隐藏。
本次补 17 条菜单/功能权限并给超管授权。竞赛页对齐门户前台 `/contest`（前台 `ContestListPage`/`ContestDetailPage` 已上线）。

**⑧ 菜单图标归位（7 条失效图标）**

侧边栏图标渲染为 `<svg><use href="#icon-{name}"/></svg>`，图标名不存在时**静默渲染空白、无报错**。
全量比对 `sys_menu.icon` 与 `src/assets/icons/svg/` 后修正 7 条：
举报管理 `warning`→`eye`、语音面试 `microphone`→`rate`、用户银行卡 `card`→`money`、
内容安全检测 `shield`→`eye-open`、VIP管理 `crown`→`star`、用户会员卡 `idcard`→`user`、
成就管理 `trophy`→`star`。修正后全库菜单图标零失效。

**新增脚本**：`increment-sql/20260928-03-补齐无菜单入口（竞赛广告提示词导入模板）.sql`、
`increment-sql/20260928-04-菜单图标归位失效图标修正.sql`（均为幂等，dev 库各连跑两次 exit 0）。

**验证**：后端 `mvn -o compile` 通过；前端 `npm run build:prod` 通过；
两个增量脚本各连跑两次 exit 0；菜单树/权限码已按真实 `sys_menu` 与控制器 `@PreAuthorize` 双向核对。

**⑨ 审核中心菜单归并（两条入口 → 一条）+ 修正误删**

`sys_menu` 历史上存在**两条**指向同一页面 `cms/audit-center/index` 的菜单，路径不一致：

| menu_id | parent | 生成路由 | 是否有代码引用 |
|---|---|---|---|
| 5152 | 内容管理(`cms`) | `/portal/cms/audit-center` | ❌ 无任何引用 |
| 5242 | 门户管理(`portal`) | `/portal/audit-center` | ✅ 后端 `AuditTaskType` 8 类型 + 前端 4 处跳转 |

结论：`/portal/audit-center` 是全项目认可路径，5152 是错误重复入口 → **删除 5152，只保留 5242**
（脚本 `20260928-05`）。同时 `5242` 的 perms 归位为 `system:auditTask:list`（与后端一致）。
⚠️ 复盘：`/portal/audit-center` **没有静态路由**，完全依赖该菜单动态生成；早期误删 5152 会
导致首页待办/文章/话题/专栏的「审核中心」跳转 404，已修正。

**⑩ 恢复被误删的两个 TabContainer 分组页**（脚本 `20260928-06`）

`5145 用户反馈处理`（反馈+举报）、`5146 帮助中心`（帮助分类+文章）此前被误判为"重复菜单"删除。
复核 `git show HEAD:...` 确认二者是**分组容器**（同类既有 `monitor/server-panel`、`monitor/cache-manage`），
其子页面本身也各有菜单，故删除不丢功能但丢失分组入口 → 已恢复页面文件与菜单。

**⑪ 系统监控菜单去重**（脚本 `20260928-07`）

监控模块同时保留了分组页与各子页面菜单，形成同一功能两个入口：

| 保留（分组页） | 删除（冗余子菜单行） |
|---|---|
| `5153 服务监控` server-panel（server + druid） | `111 数据监控` druid、`112 服务监控` server |
| `5149 缓存管理` cache-manage（cache + list） | `113 缓存监控` cache、`114 缓存列表` cacheList |

**子页面 `.vue` 保留不动**（仍被分组页组件引用）；删除的菜单行均无子权限行、仅绑定超管。
全仓核查：`/monitor/cache*`、`/monitor/server` 的文本命中**全部是后端 API 前缀**（`api/monitor/*.js` 的 url），
非前端路由；唯一真实路由引用是首页「系统运行概览」的 `goPath('/monitor/server')`，本就指向不存在路由，已修正为 `/monitor/server-panel`。

**⑫ 重导菜单初始化脚本 `init-sql/moyun-menu-redo.sql`（重要）**

旧文件内容停留在 **id 1~108 的早期结构**，与现网 398 条菜单（id 5000+/5205+）**完全不一致**，
导致**新库初始化会得到错误的菜单树**；且文件为 **GBK 编码**（本机 MySQL 客户端默认 `character_set_client=gbk`）。

已用 `mysqldump --default-character-set=utf8mb4` 重新导出并改写为 **UTF-8**，
仅保留 INSERT 行（丢弃 dump 的 `SET`/`LOCK TABLES`/`/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE*/` 等会话语句，
后者会在导入时抛 `ERROR 1231 Variable 'time_zone' can't be set to the value of 'NULL'`）。
**已在临时库实测**：建表 → 导入 → 398 条菜单、中文正常、审核中心唯一、学习管理子树正确。

**⑬ 删除无入口页（2 个）**：`cms/dashboard`（v13.25 新首页已完全取代的旧版首页仪表盘）、
`system/scan`（自述"扫描任务后端接口待接入"的纯占位页）。

**⑭ 复核后确认不存在缺口的项（原 C 清单）**：C2/C3 在线刷题/编程 → 由「题库管理」覆盖；
C5 作者管理 → 由「门户用户」（含创作者认证/VIP/身份标签/资料弹窗）覆盖；
C6 首页运营 → 由「推广位管理 + 广告位管理」覆盖。C1/C4 排行榜与刷题日历为**按用户维度**数据
（`ILearnStatsService#getLeaderboard(type, limit, currentUserId)`），做全局管理页需新增后端接口且语义存疑，**不改**。

**本轮新增脚本**：`20260928-05-审核中心菜单归并为一条.sql`、
`20260928-06-恢复被误删的分组页菜单.sql`、`20260928-07-系统监控菜单归并去重.sql`（均幂等，各连跑两次 exit 0）。

**⑮ 菜单初始化脚本重排编号：id 改为从 1 起、按语义树深度优先（v13.26 收尾）**

原文件是从现网 `mysqldump` 直出的，沿用了**历史累积的 id**（1~501 / 1000~1060 / 5000~5646 / 54031~54041，
396 条却有 53645 个空档）。按要求重排为**可复现的语义编号**：

| 项 | 规则 |
|---|---|
| 侧边栏项（M 目录 / C 菜单，共 **121** 条） | 显式 `menu_id` = **1..121**，按语义树**深度优先**：同父下先父后子、按 `order_num` 排序 |
| 父子关系 | 由语义 `parent_id` 重建，**父 id 恒小于子 id**（已校验 0 例外、0 孤儿） |
| 按钮权限（F，共 **275** 条） | **不写 `menu_id`**，交由 `AUTO_INCREMENT` 分配（紧随 M/C 段之后，122..396） |
| 授权 | 沿用动态 `INSERT...SELECT`，与菜单 id 无关，永不悬空 |

导出为**单个 INSERT 的紧凑元组列表**（与项目既有菜单脚本风格一致）；文件为 **UTF-8**。
**临时库实测通过**：导入后 M=22/C=99/F=275、无孤儿、父恒小于子、超管授权 396 条、中文正常。

> ⚠️ **口径隔离（重要）**：`init-sql/moyun-menu-redo.sql`（新库初始化，id 1..396）与
> `increment-sql/2026xxxx`（**存量库**增量，仍按现网 id 5000+ 精确寻址）**作用于两套 id 空间**，
> 二者**不可交叉使用**：新库不执行 increment，存量库不执行本脚本。此约束已写入脚本头注释。

**⑯ 菜单引用一致性核查（无缺口）**

- `sys_menu.component` 逐个比对 `src/views/**` → **全部存在**
- 后端/前端**无任何硬编码 `menu_id`** → 重编号对代码零影响
- 孤儿页复核：`cms/dashboard` 与 `system/scan` 已删；其余"未被菜单引用"的是**弹窗/子组件**
  （`ai/components/*`、`tool/gen/*Form`、`system/user/profile/*` 等）或**分组页内嵌面板**
  （`monitor/cache/index`、`monitor/server/index`、`monitor/druid/index`、`cms/topic/*`、
  `portal/bookQuote`、`portal/bookshelf`、`system/audit/*`），均由父页面 import 或 TabContainer 引用，**不可删**
- 原 C 清单缺口复核后确认**不存在**（详见 §⑭）

**已知遗留（下批处理，已列清单待确认）**：重复菜单（服务监控 ×2、缓存 ×3、审核入口 ×2 等）、
无入口页面（`cms/dashboard`、`cms/ad`、`cms/importTemplate`、`cms/contest` 未接菜单等）、
前台「排行榜 / 在线刷题 / 在线编程 / 刷题日历 / 竞赛 / 作者 / 首页运营」尚无后台管理页，
`init-sql/moyun-menu-redo.sql` 内容陈旧（仍是 id 1~108 结构，与现网菜单不一致）。

## v13.25 (2026-09-27) 后台首页改版：由通用仪表盘改为「分平台运营概览」

**需求**：后台首页（`/index`）此前是通用 CMS 看板（文章/登录/排行榜堆砌），看不出平台定位，
也未按端拆分。运营需要一眼看清「哪一端、哪个模块有事要处理」。

**信息架构（4 段式）**

| 段 | 内容 |
|---|---|
| ① 平台定位 | 品牌条（旭林知行 / 知行合一，助你上岸 / 定位一句话 / 产品战略）+ 四端定位小卡（点击可跳转，预留端灰显标「预留」） |
| ② 运营警报 | 待办积压 / AI 今日失败 / AI 累计失败率 ≥15% / 未完成面试；**无异常整条隐藏**，不编造数据 |
| ③ 分平台统计 | 端 → 模块 → 指标卡（门户端 5 模块、记账端 2 模块、管理端 4 模块），每端另有端级 KPI |
| ④ 待办/已办 + 趋势榜单 | 保留原有待办、已办（跳审核中心），新增今日运营数据、7 天趋势、内容榜单、平台动态 |

**后端（`moyun-server`）**

- `DashboardVO` 新增 `platformIdentity` / `platformStats`（`PlatformStats` → `ModuleStats` → `MetricCard`）/ `alerts`；
  `MetricCard` 新增 `tone`（主题色）与 `unit`（单位）字段
- 新增 `SysDashboardStatsMapper`（+ 同名额外置 XML `mapper/system/SysDashboardStatsMapper.xml`）：
  只承载首页专用跨模块聚合（记账/账户/AI 执行/语音面试/判题/简历/成长/审核/平台端），
  **不重复**各业务域已有聚合（文章指标复用 `PortalArticleMapper.selectArticleMetrics`，
  待办按类型复用 `IAuditTaskService.countPendingByType`）
- `SysDashboardServiceImpl` 新增 `buildPlatformIdentity` / `buildPlatformStats` / `buildAlerts`；
  各聚合块独立 `safeQuery` 兜底（单块失败不拖垮整页）；沿用 `/system/dashboard/all` 单请求 + 5 分钟 Redis 缓存
- **未新增 `system → vip` 依赖**：端清单直接读 `sys_platform` 表而非 `vip.domain.entity.SysPlatform`，
  避免为非必要的展示需求新增跨模块边（`ModuleDependencyGuardTest` 无需改动）
- 口径说明：涉及逻辑删除的表显式 `del_flag='0'`；`portal_article` 沿用既有口径（不过滤 `del_flag`），
  以保持与文章模块统计一致；记账端**只做运营计数**（笔数/账户数/报告数），**不展示金额**

**前端（`moyun-admin-vue`）**

- 重写 `src/views/index.vue`：品牌渐变条 + 运营警报条 + 分平台区块（端头 + KPI 行 + 模块网格）+ 待办/已办 +
  今日数据 + 双图表 + 内容榜单（热门文章/栏目排行 Tab）+ 平台动态 + 系统运行概览
- 指标卡按 `tone` 上色；预留端以灰色虚线占位并提示「尚未接入业务数据」；空态一律 `el-empty`，不显示假数据
- 移除原先"随便展示"的通用指标卡行（内容已并入门户端「内容社区」模块，信息不丢失）

**验证**

- 后端 `mvn -o compile` 通过；新增聚合 SQL 已逐条在 dev 库实跑，字段与类型符合预期
- 前端 `npm run build:prod` 通过
- ⚠️ 端到端页面验证需重启 `moyun-server`（当前运行实例仍是旧 class）；另 dev 库 `dashboard:full` 缓存
  需在重启后点「刷新缓存」或等待 5 分钟 TTL 过期，否则首页仍读旧结构缓存

**同步**：README 版本横幅与版本历史表；本条目。

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

