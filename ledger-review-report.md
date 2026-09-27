# 记账 App（com.moyun.ledger）数据库 / 并发 / 资金与权限评审

## 结论
1. 9 张表实体与 DDL 字段名/类型/金额单位（元，decimal(18,2)）**逐列核对一致**，无「DDL 有列实体缺失」或反向问题；唯一类型偏差：`transaction_time`。
2. 金额运算全部 BigDecimal；转账双边在同事务内完成；但 **version 乐观锁未生效**，账户更新接口存在丢失更新（严重）。
3. 门户 Controller 全部从登录态取 userId，逐个 Service 均有归属校验 → **未发现水平越权**；唯一例外是打赏幂等查询漏 user_id。
4. 定时记账任务**无任何并发/分布式保护，且未接入 sys_job(Quartz)** → 会重复扣钱（严重）。
5. **无 SQL 注入点**（全模块无 `${}`，setSql 传 BigDecimal，last() 为字面量/整型）。
6. 索引：跨 `portal_`/`pay_` 的收益 UNION 查询不可走索引；CMS 统计走全表扫描（中/低）。

## 证据

### 1. 实体 ↔ DDL 一致性（低）
- `ledger_transaction`：DDL `moyun-db-ddl.sql:783 transaction_time time`；实体 `LedgerTransaction.java:125 private String transactionTime;` —— 类型不一致（String 写 TIME 列），`LedgerScheduleServiceImpl.java:357 dto.setTransactionTime(task.getExecTime())`（`"HH:mm"`）+ 事务服务兜底 `LedgerTransactionServiceImpl.java:406` 生产 `"HH:mm:ss"`，两种格式混写。其余列（含 `create_by` 789/实体155、`is_budget` 786/实体140、无 del_flag 用 status）一致。
- 其余表一致：`ledger_asset_account(533)/LedgerAssetAccount.java:46-83`、`ledger_liability_account(592)/LedgerLiabilityAccount.java:41-96`、`ledger_budget(555)/LedgerBudget.java:20-40`、`ledger_category(571)/LedgerCategory.java:30-65`、`ledger_saving_plan(657)/LedgerSavingPlan.java:46-87`、`ledger_saving_record(680)/LedgerSavingRecord.java:31-63`、`ledger_schedule_task(720)/LedgerScheduleTask.java:45-101`、`ledger_memo(620)/LedgerMemo.java:46-81`。审计字段：各表仅有 create_time/update_time，实体同；`create_by` 只有 ledger_transaction 与 ledger_app_feature_config 有，均一致。
- 附带：`LedgerCategory.java:39` 注释称 type 可为 transfer/repayment/borrow/adjust，DDL `:575` 仅 income/expense（仅注释错误）。

### 2. 金额精度与转账（严重）
- 精度：`LedgerTransactionServiceImpl.java:329-334` 校验 `abs<=99999999.99` 且 `scale()>2 拒绝`；冲正/重放用 `amount.negate()`（:430/496）不丢分。存钱计划**缺 scale 校验**：`LedgerSavingServiceImpl.java:47` 只判 >0，`periodAmount` 5 位小数会被 decimal(18,2) 静默截断 → 期次合计 ≠ targetAmount（低）。
- 转账双边同事务：`:75 @Transactional` → `:434-439` 先扣转出再入目标，任一步抛异常整体回滚，一致性成立。
- **乐观锁形同虚设**：全项目仅 `pay/domain/entity/UserAccount.java:40` 有 `@Version`；`MyBatisConfig.java:145-152` 只注册 BlockAttack + Pagination，**无 OptimisticLockerInnerInterceptor**。故：
  - `LedgerAssetAccountServiceImpl.java:80-88`：读 exist(version=5) → `updateById(account)`（MP 默认 NOT_NULL 策略会把 `balance` 一起 SET）。若期间有记账把 balance 100→50 并 version=6，本接口用陈旧 balance=100 回写，**资金被回滚**。`deleteAccount :99 updateById(exist)` 同理。
  - `LedgerLiabilityAccountServiceImpl.java:75-84` 同型问题。
  - 记账主链路靠手写版本条件（`:542-548 balance = balance + (delta)`，`eq(version)`）才是真保护；`setSql` 拼的是 BigDecimal（`validate` 已限 scale），无注入面。
- 幂等：`:80-88` clientUuid 查重带 `eq(userId)`，并发极端场景由 `uk_client_uuid`（DDL:793，**全局唯一，非 user+uuid**）兜底 → 不同用户同 uuid 会 DuplicateKeyException 500（低）。`LedgerTransactionServiceImpl.java:600-637` 快照 upsert 为读后写，`uk_user_date`（DDL:651）兜底。

### 3. 越权（水平权限）：未发现漏洞
- Controller 全部 `PortalSecurityUtils.getUserId()`（如 `PortalLedgerTransactionController.java:35/51/58/66`、`PortalLedgerAssetController.java:39/47/59/80/89`、`PortalLedgerMemoController.java:42/50/61/76/84`），无前端传 userId 的入参。
- Service 每个写路径均有归属校验：`LedgerTransactionServiceImpl.java:591-595`、`LedgerAssetAccountServiceImpl.java:114-121`、`LedgerLiabilityAccountServiceImpl.java:108-116`、`LedgerCategoryServiceImpl.java:117-126`、`LedgerMemoServiceImpl.java:238-244`、`LedgerSavingServiceImpl.java:225-240`、`LedgerScheduleServiceImpl.java:380-387`、`LedgerAiAnalysisServiceImpl.java:729-744`；查询均带 `eq(user_id)`（如 `pageTransactions :229`、`listByUser :104-111`）。
- **例外（中）** `LedgerTipServiceImpl.java:63-78`：幂等查询只有 `.eq(clientUuid)` 无 userId，命中他人 pending 单后回显其 payNo/amount（`cashierParams(existing)`），造成跨用户订单信息泄露并复用他人单据。

### 4. 定时任务（严重）
- `LedgerScheduleExecuteTask.java:30 @Scheduled(cron="0 */10 * * * ?")`、`LedgerNetWorthSnapshotTask.java:45`、`LedgerMemoRemindTask.java:28`：仅 Spring 本地调度，无 Redis/ShedLock/DB 锁（pom 无 shedlock/redisson，仅 quartz）。
- `LedgerScheduleServiceImpl.java:198-228 runDueTasks` 无事务、无锁：多实例或本轮未跑完下一轮再扫，`nextExecDate` 仍 ≤ today → **重复生成流水并重复扣余额**；`buildDto :348-360` **未设置 clientUuid**，唯一的幂等闸门（uk_client_uuid）失效。
- `executeOnce :236-263` 先记账（独立事务）后 `updateById` 推进 `next_exec_date`；若在两步之间宕机，下次重扫同一 exec_date 再记一笔；`ledger_schedule_log` 只有 `KEY idx_task(task_id,exec_date)`（DDL:713）**非唯一**，无 DB 级去重。
- 未接入 Quartz：`sys_job` 种子（`moyun-db-dml-init.sql:622-635`）无任何 ledger 任务，而同文件 :628-631 明确把其他模块「原 @Scheduled，迁移至 Quartz 统一调度」；全仓搜 `ledgerScheduleExecuteTask` 仅命中 @Component 本身 → 无并发策略、无调度日志、无手动触发/补偿。

### 5. SQL 注入：无（低）
- 全模块 grep `\$\{` 无命中；`setSql("balance = balance + (" + delta + ")")`（Transaction :546/575）为 BigDecimal；`last()` 均为字面量：`:84`、`DashboardServiceImpl:103/138`、`ScheduleServiceImpl:148`、`TipServiceImpl:66`，或 `LedgerMemoServiceImpl.java:71 q.last("LIMIT " + (limit*3))` —— 整型但**未限幅**（`PortalLedgerMemoController.java:41` 任意 limit，负数/溢出会得到非法 `LIMIT` → SQL 报错）。

### 6. 索引与慢查询（中/低）
- `mapper/ext/cms/CmsIncomeOrderMapper.xml:5-33`：`ledger_tip_order` ∪ `portal_tip_order` ∪ `pay_order` 派生表 + `pay_time=COALESCE(paid_time,create_time)` + `ORDER BY pay_time DESC LIMIT`：三表全扫 + filesort，过滤条件 `startTime/endTime` 在 COALESCE 上不可用索引（portal_tip_order 仅 `idx_user/idx_pay_channel_status`，DDL:2737-2739）。
- `CmsLedgerStatsController.java:105-107` `select DISTINCT user_id ... ge(transaction_date, 30天前)`：`idx_user_date(user_id,transaction_date)` 前导列缺失 → 全表扫描；`:104 selectCount(null)`、`:121-133` 7 张表 `DISTINCT user_id` 全扫。
- `LedgerScheduleServiceImpl.java:363-377 fillNames` N+1 selectById；`LedgerDashboardServiceImpl.java:145-160 sumAmount` 注释称 SQL 聚合实则 selectList 内存累加（`is_budget` 无索引列）。

## 修复建议（按优先级）
1. **严重**：ledger 三个 job 迁移到 `sys_job`（`concurrent=1`，invoke_target 指向 `ledgerScheduleExecuteTask.execute()`），并在 `runDueTasks` 加 Redis 分布式锁 + `executeOnce` 内 `buildDto` 填 `clientUuid = "sched:"+taskId+":"+execDate`，同时给 `ledger_schedule_log` 加唯一键 `uk_task_date(task_id,exec_date)`（以"插入日志成功"作为执行令牌）。
2. **严重**：给 ledger_asset_account / ledger_liability_account 的 `version` 加 `@Version` 并注册 `OptimisticLockerInnerInterceptor`（或把 `updateAccount` 改为 `eq(version)` 条件更新 + 失败重试），禁止 `updateById` 回写 balance。
3. **中**：`LedgerTipServiceImpl` 幂等查询补 `eq(userId)`；`uk_client_uuid` 改 `uk_user_client(user_id,client_uuid)`（含历史重复数据清洗）。
4. **中**：存钱计划 targetAmount/periodAmount/actualAmount 增加 scale≤2 与上限校验；`LedgerSavingServiceImpl.deposit` 用条件更新（`eq(status,待存)` + 行数判断）防并发重复累计 `current_amount`。
5. **中/低**：收益 UNION 查询改为按平台分支查询或补 `paid_time/create_time` 冗余列+索引；CMS 统计改 SQL SUM/GROUP BY；`limit` 参数限幅（1~200）。
6. **低**：`transaction_time` 实体改 `LocalTime`，Schedule 的 `execTime` 统一转 `HH:mm:ss`。

> 未验证：以上并发/索引风险基于代码与 DDL 静态阅读，未执行压测或 EXPLAIN；MyBatis-Plus `updateById` 生成的最终 SET 列表按默认 NOT_NULL 策略推断。
