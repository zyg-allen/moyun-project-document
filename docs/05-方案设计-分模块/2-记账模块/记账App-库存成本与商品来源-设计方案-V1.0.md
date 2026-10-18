# 库存成本与商品来源 · 设计方案 V1.0

> 目标：让「采购 → 库存 → 销售 → 利润」形成**闭合链条**，且与记账模块**不重复计钱**。

---

## 一、已确认的三条口径

| # | 决策 | 理由 |
|---|---|---|
| 1 | **权责发生制**（方案 B） | 采购不算消费支出，卖出时才结转成本；毛利才真实 |
| 2 | 二手分**「进货二手」**与**「自用转卖」** | 后者是资产处置，有折旧，混进库存成本会失真 |
| 3 | 赠品**公允价值选填**；物流/包装/时间成本**选填，不填为 0** | 用户自己决定精细度 |

---

## 二、来源类型 = 固定枚举（用户不可增删）

来源决定**成本怎么算**，属于业务规则。开放自定义会让成本算法失序。

| 枚举 | 名称 | 成本算法 | 用户需填 | 财务联动 |
|---|---|---|---|---|
| `purchase` | 采购 | `(总额 + 附加成本) ÷ 数量` | 关联支出流水 或 手填总额 | **生成/关联支出流水** |
| `self_made` | 自产 | `(材料 + 人工 + 其他 + 附加成本) ÷ 数量` | 成本构成 或 手填总成本 | **不产生支出**（原材料已记过） |
| `gift` | 赠品 | `(公允价值 + 附加成本) ÷ 数量`，都不填则为 **0** | 公允价值、赠送方（选填） | 无 |
| `second_hand` | 二手（进货） | `(购入价 + 附加成本) ÷ 数量` | 购入价 | 无 |
| `other` | 其他 | `(手填总额 + 附加成本) ÷ 数量` | 总成本 | 无 |

**系统生成、不在下拉里出现**：

| 枚举 | 触发 | 成本 |
|---|---|---|
| `transfer_in` | 移库转入 | 沿用原成本 |
| `stocktake_gain` | 盘盈 | 同商品加权平均成本 |
| `return_in` | 销售退货入库 | 沿用原成本 |

### 可自定义的字段（不是来源）

| 字段 | 说明 |
|---|---|
| `source_party` | 供应商 / 赠送方 / 平台，自由文本 |
| `remark` | 备注 |

---

## 三、成本以「总额」为准，单价是派生值

```
存：cost_total      decimal(18,2)   成本总额（权威值）
存：cost_price      decimal(18,2)   派生 = cost_total ÷ quantity
```

**原因**：500 ÷ 3 = 166.666…，若以单价为准，`166.67 × 3 = 500.01`，库存价值会比实际多 1 分。
以总额为准则永远对得上。

---

## 四、权责发生制的具体落地

### 4.1 采购入库

```
① 用户选「采购」，填总支出 500（或关联已有支出流水）
        ↓
② 生成一条记账流水：支出 500，标记 stock_in = 1
        ↓
③ 商品成本 cost_total = 500 + 附加成本
        ↓
④ 财务报表统计「消费支出」时 **排除 stock_in = 1 的流水**
   （或单独归为「经营性支出」，不计入生活消费）
```

### 4.2 销售出库

```
卖出 3 件，成交价 200/件
        ↓
收入 600 进现金账户
成本 = 3 × cost_price 从库存扣除
毛利 = 600 - 成本   → 计入「经营利润」，不塞进资产账户
```

### 4.3 一票多品（必须支持）

超市小票 500 元买了牛奶 + 面包 + 纸巾：

```
ledger_product_source（关联表，商品 × 流水，带分摊金额）
  ├─ 牛奶   txn_id=123   amount=200
  ├─ 面包   txn_id=123   amount=180
  └─ 纸巾   txn_id=123   amount=120
             合计 500 ✓
```

因此**不能**只在商品表放一个 `source_txn_id`。

---

## 五、自用转卖（资产处置，不进货架）

| 情况 | 归属 |
|---|---|
| 收来旧货直接转卖 | 来源 = `second_hand`，走库存 |
| 买来自用 N 年再卖（婴儿车） | **资产处置**，走 `ledger_asset_disposal` |

### 处置流程

```
① 登记：原值 1000，已用 24 个月，残值率 30%
        ↓
② 估值：净值 = 1000 × 30% = 300
        ↓
③ 卖出 400
        ↓
④ 处置损益 = 400 - 300 = +100 → 计入损益
```

**不进货架**：因此不参与库存价值、不影响库存报表。

---

## 六、附加成本（对所有来源通用）

| 字段 | 说明 | 默认 |
|---|---|---|
| `extra_shipping` | 物流 | 0 |
| `extra_packaging` | 包装 | 0 |
| `extra_time` | 时间成本（手工估算） | 0 |
| `extra_other` | 其他 | 0 |

不填即为 0，全部计入 `cost_total`。

---

## 七、利润勾稽（让报表可验证）

```
期初库存 + 本期采购入库 - 期末库存 = 本期销售成本
```

报表需补两个数：**期初库存价值**、**期末库存价值**，否则成本算错也无从发现。

---

## 八、数据模型

### 8.1 `ledger_product` 新增

```sql
source_type        varchar(20)   NOT NULL DEFAULT 'purchase'  -- 枚举
source_party       varchar(100)  NULL                          -- 供应商/赠送方
cost_total         decimal(18,2) NULL                          -- 成本总额（权威）
extra_shipping     decimal(18,2) NOT NULL DEFAULT 0
extra_packaging    decimal(18,2) NOT NULL DEFAULT 0
extra_time         decimal(18,2) NOT NULL DEFAULT 0
extra_other        decimal(18,2) NOT NULL DEFAULT 0
fair_value         decimal(18,2) NULL                          -- 赠品公允价值（选填）
```

> 现有 `source_channel`（自由文本）废弃，由 `source_type` + `source_party` 取代。

### 8.2 新表 `ledger_product_source`（一票多品）

```sql
id             bigint PK
product_id     bigint      商品
txn_id         bigint      记账流水（可为空：手填总额时为空）
amount         decimal(18,2) 该商品分摊金额
create_time    datetime
UNIQUE KEY uk_product_txn (product_id, txn_id)
KEY idx_txn (txn_id)
```

### 8.3 `ledger_transaction` 新增

```sql
stock_in       tinyint NOT NULL DEFAULT 0   -- 1 = 采购入库产生的支出，不计入消费统计
```

### 8.4 新表 `ledger_asset_disposal`（自用转卖）

```sql
id              bigint PK
user_id         bigint
name            varchar(100)   物品名
original_cost   decimal(18,2)  原值
used_months     int            已用月数
residual_rate   decimal(5,2)   残值率（默认 30）
net_value       decimal(18,2)  净值
sold_price      decimal(18,2)  卖出价
gain_loss       decimal(18,2)  处置损益
sold_at         date
status          tinyint
```

---

## 九、分阶段落地

| 阶段 | 内容 | 是否阻塞 |
|---|---|---|
| **P0** | 来源枚举 + cost_total 改造 + 附加成本 | 阻塞后续 |
| **P0** | 采购生成支出流水（打 stock_in 标记）+ 报表排除 | 阻塞利润口径 |
| **P1** | `ledger_product_source` 一票多品分摊 | — |
| **P1** | 销售出库结转成本 + 经营利润 | — |
| **P2** | 资产处置（自用转卖） | — |
| **P2** | 期初/期末库存 + 勾稽校验 | — |
| **P3** | 退货 / 批次成本 / 供应商主数据 / 押金 | — |

---

## 十、仍未解决（后续再定）

| 项 | 说明 |
|---|---|
| 批次成本法 | 同商品两次进货价不同，卖出按先进先出还是加权平均？当前按「商品唯一成本价」，多批次会失真 |
| 多单位进货 | 买 1 箱 = 24 瓶，是否支持单位换算 |
| 采购退货 | 库存减 + 支出冲减 |
| 销售退货 | 库存回增 + 收入冲减 |
