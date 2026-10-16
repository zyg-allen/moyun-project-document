# 交接文档：记账 App「库存投资管理 + 债权债务」模块

> 本文档是开发状态快照，供新会话无缝接续。方向已由
> `docs/05-方案设计-分模块/2-记账模块/记账App-资产管理-设计方案-补充更正-V1.4.md`
> 全部钉死，照做即可，无需再讨论设计。

---

## 一、文档 V1.4 关键结论（已确认，勿推翻）

### 八种记账类型（只有前两种改净资产，其余是形态转换）

| # | 类型 | 资金方向 | 净资产 | 联动 |
|---|---|---|---|---|
| 1 | 支出 | 出 | **减少** | 资产账户 −X |
| 2 | 收入 | 进 | **增加** | 资产账户 +X |
| 3 | 转账 | 内部 | 不变 | A−X, B+X |
| 4 | 借出 | 出 | 不变 | 资金−X，应收+X |
| 5 | 收款 | 进 | 不变 | 资金+X，应收−X，写核销流水 |
| 6 | 借款 | 进 | 不变 | 资金+X，**应付(payable)+X** |
| 7 | 还款 | 出 | 不变 | 资金−X，**应付(payable)−X**，写核销流水 |
| 8 | 投资 | 出/进 | 不变（盈亏另算） | 买入：资金−X，投资+X；卖出：资金+X，投资−X，差额计收入 |

### 资产/负债分类

```
总资产 = 资金 + 库存 + 应收 + 投资 + 固定资产
总负债 = 应付 + 内部应付
净资产 = 总资产 − 总负债
```

- 资产侧 5 类：资金(cash) / 库存(stock,按成本价) / 应收(receivable) / 投资(investment) / 固定(fixed_asset)
- 负债侧 2 类：应付(payable) / 内部应付(settlement)

### 开发顺序（P0/P1/P2）

```
P0：表 + 八种类型 + 库存赊账联动
P1：应收/应付UI + 导航/仓库页 + 主题配色
P2：投资模块 + 固定资产模块
```

---

## 二、已完成清单（均已提交、测试全绿）

| 提交 | 内容 |
|---|---|
| `2e45515d` 起 | 债务三表 + transaction 扩展 |
| `3dc9e26d` | 七类型 lend/collect 联动 |
| `3c0631f2` | 核销实现（收款/还款落账 + 状态流转） |
| `3a2e77aa` | **应收/应付合并为 `ledger_obligation`（一张表+type）** |
| `e0e36301`/`26a61160` | 建库链路补齐 + 增量脚本幂等 |
| `320df671`/`59b5b383`/`f5ceb497` | 库存 5 表 + 实体/Mapper + 服务/控制器 |
| `43a32327` | 记一笔借出/收款表单 |
| `0b0eee14`/`bc97b9a7` | 仓库页 + 出入库 |
| `f3b3eef6` | 底部导航六项 |
| `1219ad4e` | 主题收入绿色对齐（红绿撞色修复） |

### 关键表（已建，线上两库已同步）

```
ledger_obligation  债权债务统一表（type=receivable/payable，一张表）
ledger_debt_repayment  核销流水（debt_type + debt_id）
ledger_transaction  流水（有 receivable_id / payable_id 两列指向 obligation）
ledger_warehouse / warehouse_member / product / stock / stock_flow  库存5表
```

---

## 三、剩余 P0 三项（本轮未做，需新会话）

### ① 借款/还款切到 payable（文档明确，当前还走负债账户）

**现状**：`LedgerTransactionServiceImpl` 里 `TYPE_BORROW`/`TYPE_REPAYMENT` 走 `liabilityId`
（负债账户 `ledger_liability_account`）。

**文档要求**：借款→ payable+X，还款→ payable−X（都走 `ledger_obligation type=payable`）。

**改动点**：
- `createTransaction` 的 borrow 分支：自动建 obligation(type=payable) 而非负债账户
- repayment 分支：核销 obligation 而非减负债账户余额
- 前端 borrow/repayment 表单：负债选择器 → 应付选择器（或"对方+金额"）
- **注意**：现有负债账户（房贷/车贷/信用卡，带还款日/期数）功能比 payable 丰富，
  需确认是「并存」还是「替换」—— 文档没提负债账户，建议问用户。

### ② 加「投资」类型（第8种）

- 类型选择器里「校准(adjust)」替换为「投资(invest)」（文档八种无校准）
- 投资模块（investment 表 + 买入/卖出/盈亏）是 P2，P0 只需类型入口 + 基础资金联动
- 建议：P0 先加类型占位，P2 再做完整投资表

### ③ 库存赊账联动（需订单表）

- 销售出库：现金→资金+X 库存−X；赊账→obligation(receivable)+X 库存−X（差额=利润）
- 采购入库：现金→资金−X 库存+X；赊账→obligation(payable)+X 库存+X
- 需先建 `ledger_sale_order` / `ledger_purchase_order`（+明细）表
- 参考 `docs/.../库存投资管理模块_核心数据表SQL建表语句.sql.sql`（SQLite 原型，需转 MySQL）

---

## 四、关键约定（新会话务必遵守，都是本会话踩过的坑）

1. **表名**加 `ledger_` 前缀；软删用 `status`(1正常/0删除)，**不用** del_flag（记账域约定）
2. **金额**一律 `decimal(18,2)`（`DdlConventionGuardTest` 强制）
3. **债务余额不落列**：未结 = total − paid；结算状态由唯一推导点 `resolveSettleStatus` 算
4. **收债用 collect、还款核销用 settle**，绝不走 income/expense（收债不是收入）
5. **实体用纯 BaseMapper**，别手写 XML（`PortalUserMapper.xml` 白名单漏列曾致空 SQL）
6. **`Long` 序列化为字符串**（全局 ToStringSerializer）：前端比较 ID 用 `String(...)`，别 `===`
7. **updateById 跳过 null**：清空字段用 `LambdaUpdateWrapper.set(...)`
8. **增量脚本必须幂等**：`CREATE TABLE IF NOT EXISTS` + information_schema 守卫（`IncrementSqlIdempotencyGuardTest` 强制）
9. **提交前必跑**：`mvn -o test`(462 绿) + vue 编译(30 个 0 失败)，绿了才 commit
10. **用户自己的文档**（docs/05-方案设计）**不要提交**；`application-dev.yaml` 的库名是用户本地改动，也不要提交
