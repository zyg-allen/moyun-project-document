# 多人协作共享库存 · 设计方案 V1.0

> 目标：合作卖同一批货时，A 卖出后 B 立刻知道，不会重复卖。
> 这是本平台区别于普通记账 App 的核心价值。

---

## 一、现状盘点

| 项 | 状态 |
|---|---|
| `ledger_warehouse_member` 表 | ✅ **已建好**（warehouse_id / user_id / role / invited_by / is_active） |
| 表里有数据吗 | ❌ **0 条，从未使用** |
| 访问控制 | ❌ 全部是 `warehouse.user_id = 当前用户`（`getOwnedWarehouse`） |
| 库存隔离 | ❌ `ledger_stock.user_id`，成员看不到别人的库存 |
| 实时性 | ❌ 每次进页面才拉一次，别人卖了不会推送 |

**结论：地基有，整栋楼没盖。**

---

## 二、成员模型

### 角色（3 级，够用了）

| 角色 | 查看 | 出入库 | 改商品/货架 | 删仓库 | 管成员 |
|---|---|---|---|---|---|
| `owner` 创建者 | ✅ | ✅ | ✅ | ✅ | ✅ |
| `member` 成员 | ✅ | ✅ | ✅ | ❌ | ❌ |
| `viewer` 只读 | ✅ | ❌ | ❌ | ❌ | ❌ |

**默认邀请进 `member`** —— 合伙人要能卖货、能记账。

### 仓库类型与共享的关系

```
warehouse_type = personal  → 仅自己，不邀请
warehouse_type = partner   → 可邀请成员（合作卖货）
warehouse_type = family    → 可邀请成员（家人共用）
```

---

## 三、访问控制改造（最核心的工程）

### 3.1 统一的权限入口

现在每个方法都调 `getOwnedWarehouse(userId, warehouseId)`，改成：

```java
/** 校验当前用户对该仓库的访问权，返回仓库实体 + 角色 */
private WarehouseAccess requireAccess(Long userId, Long warehouseId, String minRole) {
    LedgerWarehouse w = warehouseMapper.selectById(warehouseId);
    if (w == null) throw new ServiceException("仓库不存在");
    if (userId.equals(w.getUserId())) {
        return new WarehouseAccess(w, ROLE_OWNER);   // 创建者直通
    }
    LedgerWarehouseMember m = memberMapper.selectOne(... warehouseId + userId + is_active=1 ...);
    if (m == null) throw new ServiceException("无权访问该仓库");
    if (!roleAtLeast(m.getRole(), minRole)) throw new ServiceException("权限不足");
    return new WarehouseAccess(w, m.getRole());
}
```

| 操作 | 最低角色 |
|---|---|
| 查列表 / 详情 / 报表 | `viewer` |
| 出入库 / 移库 / 盘点 | `member` |
| 改商品 / 改货架 / 改类目 | `member` |
| 删仓库 / 删商品 / 管成员 | `owner` |

### 3.2 需要改造的方法（全部）

```
listShelves / createShelf / updateShelf / deleteShelf
listProducts / createProduct / updateProduct / deleteProduct
stockIn / stockOut / transferStock / stocktake
warehouseOverview / listStockFlows
getWarehouse / updateWarehouse / deleteWarehouse
```

**约 18 个方法，统一换成 `requireAccess`。**

### 3.3 数据归属保持不变

```
ledger_warehouse.user_id      = owner（不变）
ledger_product.user_id        = owner（不变）
ledger_stock.user_id          = owner（不变）
ledger_stock_flow.operator_id = 实际操作人（已有字段，正好用来记「谁卖的」）
```

**不改归属** —— 成员是通过「仓库成员」获得访问权，不是数据搬家。
这样迁移成本最低，历史数据零影响。

---

## 四、邀请机制

### 邀请码（推荐，最简单）

```
① owner 在「仓库设置 → 成员」点「邀请成员」
        ↓
② 生成 6 位邀请码，有效期 7 天，可撤销
        ↓
③ 对方在「我的 → 加入仓库」输入邀请码
        ↓
④ 校验有效 → 插入 ledger_warehouse_member(role=member)
```

### 新表 `ledger_warehouse_invite`

```sql
id            bigint PK
warehouse_id  bigint
code          varchar(16)   邀请码（唯一）
role          varchar(20)   加入后的角色
created_by    bigint
expire_at     datetime
used_count    int           已使用次数
max_use       int           上限（默认 10）
status        tinyint
UNIQUE KEY uk_code (code)
```

**为什么不用二维码/手机号**：轻量场景，6 位码口述/微信发一下最快。

---

## 五、防重复卖出（用户最核心的诉求）

### 5.1 三层保障

| 层 | 手段 | 效果 |
|---|---|---|
| **实时刷新** | 进页面 + 每 30 秒轮询一次库存快照 | 别人卖了，你 30 秒内看到 |
| **乐观锁** | `ledger_stock.version`（已有字段） | 两人同时扣，只有一个成功 |
| **友好提示** | 冲突时提示「已被 XX 卖出，当前库存 N」 | 不会静默失败 |

### 5.2 冲突场景

```
库存 1 件
   A 点「卖出」      B 点「卖出」
        ↓                ↓
   读到 version=5   读到 version=5
        ↓                ↓
   UPDATE ... version=6   成功
        ↓
   B 的 UPDATE ... WHERE version=5  → 影响 0 行
        ↓
   提示：「这件货刚被 XX 卖掉了，当前库存 0」
        ↓
   自动刷新列表
```

**不做「预留锁」** —— 预留需要超时释放，对轻量场景是过度设计。

### 5.3 轮询 vs WebSocket

| 方案 | 成本 | 建议 |
|---|---|---|
| 轮询（30 秒） | 低，改前端即可 | ✅ **P1 先做这个** |
| WebSocket 推送 | 高，要建连接管理 | P2 再考虑 |

**轻量场景轮询足够** —— 卖货不是秒杀。

---

## 六、收益归属（最需要想清楚的）

### 问题

两人合卖同款 300 元，钱归谁？

### 方案对比

| 方案 | 做法 | 复杂度 | 建议 |
|---|---|---|---|
| **谁卖谁记** | 每笔卖出记 `operator_id`，收益算该成员 | 低（字段已有） | ✅ **P1** |
| 按比例分成 | 预设分成比例，卖出时自动拆 | 高（要结算表） | P2 |
| 共同经营 | 不归个人，进「共同账户」 | 中 | P2 |

### P1 采用「谁卖谁记」

```
ledger_stock_flow.operator_id = 实际卖出的人
        ↓
报表「按成员统计」：每人卖了多少、赚了多少
        ↓
分成线下自行结算（App 不介入）
```

**理由**：符合你的定位 —— 轻量、不碰钱的实际流转。

---

## 七、隐私边界（重要）

| 数据 | 共享给成员 | 说明 |
|---|---|---|
| 仓库 / 货架 / 商品 | ✅ | 共享库存的基础 |
| 库存数量 / 成本 / 流水 | ✅ | 否则无法协作 |
| **个人记账流水** | ❌ | 成员的私人收支，不共享 |
| **个人净资产 / 报表** | ❌ | 只在各自账号 |

**正好落在「库存相对独立于记账」的定位上** —— 库存共享，账本各管各的。

---

## 八、界面改动

### 8.1 仓库切换条

```
[个 个人仓] [合 合伙仓] [＋ 新建]
                ↑ 徽标体现性质，无需额外标记
```

### 8.2 仓库设置新增「成员」

```
┌─ 成员 ──────────────────────┐
│ 你（创建者）        owner     │
│ 张三                member    │
│ 李四                只读      │
│                              │
│ [＋ 邀请成员]                 │
└──────────────────────────────┘
```

### 8.3 「我的」新增入口

```
🏠 加入仓库     ← 输入邀请码
```

### 8.4 流水显示操作人

```
[A-01] 可口可乐 · 销售 · 抖音          -3
       张三 卖的 · 2026-11-03 14:32
        ↑ 共享仓库里显示 operation
```

---

## 九、分阶段落地

| 阶段 | 内容 | 价值 |
|---|---|---|
| **P1-1** | `requireAccess` 改造 18 个方法 + 成员表启用 | 成员能看到共享仓库 |
| **P1-2** | 邀请码：生成 / 输入 / 撤销 | 能加人了 |
| **P1-3** | 前端：成员管理页 + 加入仓库入口 | 可用 |
| **P1-4** | 乐观锁 + 轮询刷新 + 冲突提示 | **防重复卖出（核心）** |
| P2-1 | 按成员统计销售业绩 | 复盘谁卖得多 |
| P2-2 | 按比例分成结算 | 分钱 |
| P2-3 | 消息通知（卖出 / 低库存） | 主动提醒 |

---

## 十、风险与注意事项

| 风险 | 说明 | 对策 |
|---|---|---|
| **权限改造漏改** | 18 个方法，漏一个就是越权漏洞 | 逐个改 + 加集成测试（A 用户访问 B 的仓库必须被拒） |
| 历史数据 | `ledger_stock.user_id` 是 owner，成员操作后归属不变 | 设计上就不改归属，零迁移 |
| 成员退出后数据 | 他卖出的流水仍保留 `operator_id` | 正常，用于追溯 |
| 并发正确性 | 乐观锁要确保 `update ... where version = ?` 真的被判影响行数 | 必须检查返回值 |
| 只读角色 | 容易漏挡写接口 | 用 `minRole` 参数统一挡 |

---

## 十一、最小可验证路径

```sql
-- 准备：两个账号
-- A 创建合伙仓库，生成邀请码
-- B 输入邀请码加入
```

```
① A 建商品「可口可乐」，入库 10 件
② B 登录 → 仓库列表能看到该仓库 → 商品可见、库存 10
③ B 出库 3 件 → A 刷新看到库存 7
④ A、B 同时出库 7 件 → 只有一个成功，另一个提示冲突
⑤ 流水里显示：3 件是 B 卖的，7 件是 A 卖的
```

**这 5 步全过，P1 就算成功。**
