> 注意：本版 V1.0 的强制转账与只进不出拦截已被 V1.1 替代，请以 V1.1 为准。

# 记账 App · 存钱计划：日历标记 + 转账流水联动 设计方案

> 版本：V1.0（2026-10-26）
> 范围：存钱计划详情页（日/周/月标记）、存入业务（工资卡 → 存钱账户）、与流水的关系
> 结论：推荐 **方案 A（账户绑定 + 转账流水）**，辅以 **方案 C（存钱账户只进不出）** 的约束。

---

## 1. 现状与问题

| 现状 | 问题 |
|---|---|
| `ledger_saving_plan` 只有目标/方式/金额，**没有账户字段** | 用户不知道"钱从哪来、存到哪去" |
| `ledger_saving_record` 只有 `status(0/1/2)`、`amount`、`recordDate` | 存入不产生流水，账户余额不变 |
| `ledger_transaction` 已有 `type=transfer` + `target_account_id` | 转账能力现成，但存钱计划没用上 |
| 详情页只有按期的列表 | 无法按日/周/月看"哪天该存、哪天没存" |
| 失败只能"放弃" | 缺少"跳过/补存/撤销"语义 |

核心矛盾：**存钱计划是"目标视图"，账户余额是"资金事实"**。两者不联动，就会出现"计划显示已存 5000，账户里却没这笔钱"。

---

## 2. 四个方案对比

### 方案 A：账户绑定 + 转账流水（推荐）

每个计划绑定两个账户：

- `source_account_id`：转出账户（工资卡/零钱）
- `savings_account_id`：转入账户（存钱账户，资产账户的一种）

每次"存入"生成一条 `type=transfer` 的流水（`account_id=source`，`target_account_id=savings`），同时更新 `saving_record.status=1` 和 `transaction_id`。

| 维度 | 评价 |
|---|---|
| 资金一致性 | ★★★★★ 流水即事实，余额可推导 |
| 用户理解 | ★★★★★ "从工资卡转 500 到买房基金" |
| 实现成本 | ★★★☆☆ 需改计划表、记录表、存入接口 |
| 撤销/对账 | ★★★★☆ 反向转账或冲正 |
| 历史数据 | 老记录无流水，需标记 legacy |

### 方案 B：只记计划，不写流水（现状增强）

`saving_record` 独立记录，账户余额不动；详情页只展示计划进度。

- 优点：改动最小，一天能上线；
- 缺点：钱没有真正"移动"，报表/账户对不上；用户会问"我存的钱去哪了"；
- 适用：纯"心理账户"打卡，不涉及真实资金。

### 方案 C：存钱账户 = 真实子账户，只进不出

在方案 A 基础上，给 `ledger_asset_account` 加 `is_savings` 标记：

- 存钱账户只能通过"转入"增加；
- 支出/转出接口遇到 `is_savings=1` 时**拒绝或二次确认**；
- 账户详情页只显示"存入/转出到原账户"。

| 维度 | 评价 |
|---|---|
| 语义 | ★★★★★ 真正"只进不出"，符合存钱心智 |
| 风险 | 用户急用钱时无法支出，需要"转出"通道 + 提示 |
| 实现 | ★★★★☆ 账户类型 + 若干校验 |

### 方案 D：自动定投批次

按周/月定时任务批量生成待存记录，到期自动从源账户转账（类似基金定投），失败可重试；记录带 `batch_id`。

- 优点：省事，适合"每月固定存"；
- 缺点：自动扣款涉及资金安全，必须可暂停/可撤销/有通知；
- 建议：作为 P1，先做手动确认版。

### 对比结论

| 方案 | 一致性 | 成本 | 推荐阶段 |
|---|---|---|---|
| A 转账流水 | 高 | 中 | **P0 必做** |
| C 只进不出 | 高 | 中 | **P0 一起做**（仅账户标记 + 校验） |
| B 纯计划 | 低 | 低 | 不推荐（除非明确是虚拟打卡） |
| D 自动定投 | 高 | 高 | P1 |

---

## 3. 推荐方案：A + C 的具体设计

### 3.1 数据模型

**`ledger_saving_plan` 新增：**

```sql
ALTER TABLE ledger_saving_plan
  ADD COLUMN source_account_id  bigint NULL COMMENT '转出账户（工资卡）',
  ADD COLUMN savings_account_id bigint NULL COMMENT '转入账户（存钱账户）',
  ADD COLUMN auto_deduct        tinyint NOT NULL DEFAULT 0 COMMENT '是否自动转入';
```

**`ledger_saving_record` 新增：**

```sql
ALTER TABLE ledger_saving_record
  ADD COLUMN transaction_id bigint NULL COMMENT '关联流水ID（transfer）',
  ADD COLUMN paid_at        datetime NULL COMMENT '实际存入时间',
  ADD COLUMN reversed_at    datetime NULL COMMENT '撤销时间',
  ADD UNIQUE KEY uk_record_txn (transaction_id);
```

- `status`：`0=待存`、`1=已存`、`2=失败`、`3=跳过`、`4=已撤销`；
- 部分存入：`status=1` 且 `amount < target_amount`，前端标注"部分存入"。

**`ledger_asset_account` 新增：**

```sql
ALTER TABLE ledger_asset_account
  ADD COLUMN is_savings tinyint NOT NULL DEFAULT 0 COMMENT '存钱账户：只进不出';
```

**流水复用现有字段**：`type='transfer'`、`account_id`、`target_account_id`、`balance_after`、`target_balance_after`、`client_uuid`。

### 3.2 存入流程（一个本地事务）

```
1. 校验：record 属于当前用户、status=0、plan 已绑定两个账户
2. 校验：source 余额 >= 本次金额（允许部分存入）
3. 幂等：client_uuid = "saving:" + recordId（唯一索引兜底）
4. 写流水：type=transfer, amount, account_id=source,
           target_account_id=savings, transaction_date=today,
           description="存钱计划-第N期"
5. 更新余额：source -= amount；savings += amount（写 balance_after）
6. 更新记录：status=1, amount, paid_at=now, transaction_id=新流水ID
7. 任一步失败 → 整体回滚；重复请求命中幂等键 → 直接返回已存在流水
```

**撤销**：不允许删流水；生成一条反向 `transfer`（savings → source），记录 `status=4`、`reversed_at`。

### 3.3 "只进不出"约束

| 入口 | 行为 |
|---|---|
| 记一笔-支出，选到存钱账户 | 拦截，提示"存钱账户只进不出，请先转出" |
| 账户转账，从存钱账户转出 | 允许，但需二次确认 + 选择转入账户 |
| 存钱账户详情 | 只显示"存入/转出"，不显示"支出" |
| 报表 | 存钱账户的转出不计入消费，避免虚增支出 |

### 3.4 与流水关联后的展示

- 详情页每期显示"已存 500 元 · 查看流水"；
- 流水详情反查："来自存钱计划-买房基金 第3期"；
- 账户余额 = 初始 + 所有流水，计划进度 = 记录汇总，两者可用对账任务校验。

---

## 4. 日 / 周 / 月 标记设计

详情页顶部加分段控件：**日 | 周 | 月**。

### 日视图（默认）

- 月历 7×6 网格；
- 每个日期格子用**底色 + 角标**表达状态：
  - 已存：绿底/绿点，角标金额（可选）；
  - 待存：虚线边框 + 黄点；
  - 失败：红底/红点；
  - 跳过：灰底 + "–"；
- 点某天 → 下方面板显示当天各期，直接"存入/标记未完成/跳过/撤销"。

### 周视图

- 按月内自然周分组（第1周…第5周），每行：`周一~周日` 7 个小格 + 右侧周汇总（完成 x/y · 金额）；
- 适合 52 周存钱法：一眼看出哪几周没存。

### 月视图

- 一年 12 宫格，每格显示：完成数/总数、进度环、失败数；
- 点某月 → 切回日视图并定位该月。

### 状态标记汇总

| 状态 | 日 | 周 | 月 |
|---|---|---|---|
| 已存 | 绿点/绿底 | 完成 +1 | 进度环 |
| 待存 | 虚线/黄点 | 待完成 +1 | 灰环 |
| 失败 | 红点 | 失败 +1 | 红角标 |
| 跳过 | 灰点 | 跳过 +1 | 灰角标 |

---

## 5. 接口变更

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/portal/ledger/saving-plans/{id}/records/{rid}/deposit` | body: `{amount}`；返回 `transactionId` |
| POST | `/portal/ledger/saving-plans/{id}/records/{rid}/skip` | 标记跳过 |
| POST | `/portal/ledger/saving-plans/{id}/records/{rid}/reverse` | 撤销存入，生成反向转账 |
| GET | `/portal/ledger/saving-plans/{id}/calendar?granularity=day|week|month&month=2026-10` | 聚合数据 |
| PUT | `/portal/ledger/saving-plans/{id}/accounts` | 绑定/修改转出、转入账户 |

---

## 6. 迁移与兼容

1. 存量计划 `source_account_id/savings_account_id` 为空：首次存入时弹窗引导选择，选完再存；
2. 存量 `saving_record` 无 `transaction_id`：标记 `legacy`，列表显示"历史记录（未关联流水）"，不回溯生成转账；
3. 新用户创建计划时默认"新建存钱账户"（`is_savings=1`），并选择工资卡为转出账户；
4. 所有 DDL 幂等（`information_schema` 判断），并入 `db-init` 增量文件。

---

## 7. 分期落地

| 阶段 | 内容 | 预估 |
|---|---|---|
| **P0** | 计划绑定账户、存入生成 transfer、记录关联流水、日/周/月标记、跳过状态、只进不出校验 | 2~3 天 |
| **P1** | 撤销存入（反向转账）、自动定投、对账任务、流水反查入口 | 2 天 |
| **P2** | 多存钱账户、按计划预算、失败自动补存提醒 | 2 天 |

---

## 8. 风险与对策

| 风险 | 对策 |
|---|---|
| 重复点击导致重复转账 | `client_uuid` 唯一索引 + 记录行锁 |
| 转账成功但记录更新失败 | 同一本地事务；失败整体回滚 |
| 存钱账户无法应急支出 | 提供"转出"通道 + 明确提示，不硬禁止 |
| 历史记录与流水对不上 | 标记 legacy，展示上区分，不做假数据 |
| 自动定投误扣 | 默认关闭、可暂停、扣款前通知、支持撤销 |

---

## 9. 需要你拍板的三件事

1. 存钱账户是**新建专用账户**，还是允许选任意资产账户？
2. 只进不出是**硬禁止支出**，还是**允许但二次确认**？
3. 存量记录是否要**回溯补流水**（建议不补，仅标记历史）？
