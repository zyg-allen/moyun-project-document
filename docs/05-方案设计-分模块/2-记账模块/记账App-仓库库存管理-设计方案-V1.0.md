# 记账 App · 仓库库存管理 · 设计方案 V1.0

> 配套文档：`记账App-资产管理-设计方案-补充更正-V1.4.md`（八种记账类型与资产联动）
> 适用范围：`moyun-ledger-app` 仓库页、`moyun-server` 记账-库存模块、`moyun-admin-vue` 后台
> 原则：**不做企业级 ERP**，只做散户/个体户/夫妻店真正需要的能力；操作三步内完成

---

## 一、现状盘点

### 1.1 已有表

| 表 | 关键字段 | 问题 |
| --- | --- | --- |
| `ledger_warehouse` | user_id, name, warehouse_type(personal/family/partner), is_active | 只有仓库，没有更细的存放位置 |
| `ledger_warehouse_member` | warehouse_id, user_id, role | **已建但服务层完全未使用**（合伙仓形同虚设） |
| `ledger_product` | warehouse_id, user_id, name, barcode, category, unit, cost_price, sell_price, **image_urls(text)**, is_secondhand, condition_grade, source_channel | 图片是 JSON 文本，不是 1:N 子表 |
| `ledger_stock` | warehouse_id, product_id, quantity, cost_value, **UNIQUE(warehouse_id, product_id)** | 一个商品只能有一条库存，无法按货架拆分 |
| `ledger_stock_flow` | warehouse_id, product_id, flow_type, qty_before/change/after, cost_before/after, operator_id, related_order | 无货架维度，无法追溯"在哪搬的" |

### 1.2 已有接口（`PortalLedgerInventoryController`，`/portal/ledger/inventory`）

```
GET    /warehouses                       仓库列表
POST   /warehouses                       新增仓库
PUT    /warehouses/{id}                  修改仓库
DELETE /warehouses/{id}                  删除仓库
GET    /warehouses/{warehouseId}/products 商品列表（带库存数量）
POST   /products                         新增商品（自动建库存行 qty=0）
PUT    /products/{id}                    修改商品
DELETE /products/{id}                    删除商品
POST   /stock/in                         入库
POST   /stock/out                        出库
GET    /warehouses/{warehouseId}/flows   库存流水
```

### 1.3 已有 UI

`pages/warehouse/index.vue`（357 行）：仓库切换 / 商品列表（带库存）/ 新建仓库 / 新增商品 / 出入库 / 下单。
**没有货架概念。**

---

## 二、差距清单（本次要解决）

| # | 差距 | 优先级 | 处理 |
| --- | --- | --- | --- |
| G1 | 无「货架（存放位置）」 | P0 | 新建 `ledger_shelf` |
| G2 | 图片不是独立 1:N 子表 | P0 | 复用 `sys_file`（business_type/business_id） |
| G3 | 无「归属」信息 | P0 | 货架带 warehouse_id + user_id + zone/区域 |
| G4 | 库存不能按货架拆分 | P0 | `ledger_stock` 加 `shelf_id`，唯一键改 `(product_id, shelf_id)` |
| G5 | 无移库/上架操作 | P0 | `POST /stock/transfer` |
| G6 | 流水无货架维度 | P1 | `ledger_stock_flow` 加 `shelf_id` + `to_shelf_id` |
| G7 | 合伙仓成员表未使用 | P2 | 本次只做归属字段，成员权限留 P2 |
| G8 | 商品库存=单行 | P0 | 改为 `SUM(quantity)` 聚合 |

---

## 三、数据模型

### 3.1 新表 `ledger_shelf`（货架）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint PK | |
| `warehouse_id` | bigint NOT NULL | **归属**：所属仓库 |
| `user_id` | bigint NOT NULL | **归属**：归属人 |
| `code` | varchar(50) NOT NULL | 货架编号（仓库内唯一，如 `A-01`） |
| `name` | varchar(100) NOT NULL | 货架名称（如「A区第1排」） |
| `zone` | varchar(50) | 区域/分区（如 A区 / 冷藏区 / 保险柜） |
| `row_no` | int | 排 |
| `layer_no` | int | 层 |
| `position_no` | int | 位 |
| `location_text` | varchar(200) | 位置描述（冗余，列表直接展示） |
| `shelf_type` | varchar(20) | `normal`/`refrigerated`/`locked`/`temp` |
| `capacity` | int | 容量（件，选填，超出仅提示不阻断） |
| `sort_order` | int | 排序 |
| `remark` | varchar(500) | 备注 |
| `version` | int | 乐观锁（与账户表同口径） |
| `status` | tinyint | 1=启用 0=停用 |
| `create_by/create_time/update_time` | | |

约束：`UNIQUE(warehouse_id, code)`、`KEY(warehouse_id)`、`KEY(user_id)`、`decimal(18,2)` 无金额字段。
`location_text` 由后端按 `zone + row_no排 + layer_no层 + position_no位` 自动拼装，前端不必手填。

### 3.2 `ledger_stock` 改造

```sql
ALTER TABLE ledger_stock
  ADD COLUMN shelf_id bigint NOT NULL DEFAULT 0 AFTER product_id;  -- 0 = 未上架
-- 唯一键：一个商品在一个货架上一条库存
ALTER TABLE ledger_stock DROP INDEX uk_warehouse_product;
ALTER TABLE ledger_stock ADD UNIQUE KEY uk_product_shelf (product_id, shelf_id);
```

**为什么用 `0` 而不是 `NULL`**：MySQL 唯一索引对 NULL 视为互不相等，
若 `shelf_id` 可空，同一商品的多个「未上架」行会重复。用 `0` 表示未上架，唯一性成立。

**商品总库存** = `SUM(quantity) WHERE product_id=?`（跨货架聚合）。

### 3.3 `ledger_stock_flow` 改造

```sql
ALTER TABLE ledger_stock_flow
  ADD COLUMN shelf_id bigint NOT NULL DEFAULT 0 COMMENT '本次操作货架（移库时为源货架）',
  ADD COLUMN to_shelf_id bigint NOT NULL DEFAULT 0 COMMENT '移库目标货架（非移库为0）';
```

### 3.4 图片 1:N —— 复用 `sys_file`

`sys_file` 已具备 `business_type` + `business_id`，正是通用 1:N 附件表，**不新建表**：

| business_type | business_id | 用途 |
| --- | --- | --- |
| `ledger_product` | 商品 id | 商品图片（多张） |
| `ledger_shelf` | 货架 id | 货架图片（多张） |
| `ledger_warehouse` | 仓库 id | 仓库图片（预留） |

- 排序：`ORDER BY id ASC`，**首张即封面**（无需新增 `is_cover` 列，避免改动公共表）
- 上传复用既有 `uploadFile(filePath, businessType)`（`api/ledger.js` 已具备）
- `ledger_product.image_urls` 保留但**不再读取**（历史数据不迁移，按项目约定不做存量迁移）；
  新图片一律写 `sys_file`

### 3.5 归属（Ownership）口径

| 层级 | 归属依据 |
| --- | --- |
| 仓库 | `ledger_warehouse.user_id`（创建者即归属人） |
| 货架 | `ledger_shelf.warehouse_id`（属于哪个仓）+ `user_id`（谁建/归谁） |
| 商品 | `ledger_product.warehouse_id` + `user_id` |
| 库存 | 通过 `product_id` → 商品归属；通过 `shelf_id` → 货架归属 |

访问控制：所有接口先校验 `warehouse.user_id == 当前用户`（复用 `getOwnedWarehouse`），
货架/商品再校验其所属仓库归属。合伙仓成员权限（`ledger_warehouse_member`）列 P2。

---

## 四、接口设计

### 4.1 货架

```
GET    /inventory/warehouses/{warehouseId}/shelves   货架列表（含商品数、库存件数）
POST   /inventory/shelves                            新增货架
PUT    /inventory/shelves/{id}                       修改货架
DELETE /inventory/shelves/{id}                       删除（有库存时拒绝）
GET    /inventory/shelves/{id}                       详情（含图片 + 该货架商品明细）
GET    /inventory/shelves/{id}/images                货架图片列表
POST   /inventory/shelves/{id}/images                追加图片 { url }
DELETE /inventory/images/{fileId}                    删除图片（校验归属）
```

### 4.2 商品图片

```
GET    /inventory/products/{id}/images
POST   /inventory/products/{id}/images               追加图片 { url }
DELETE /inventory/images/{fileId}
```

### 4.3 库存

```
POST   /inventory/stock/in       { warehouseId, productId, shelfId?, quantity, remark }
POST   /inventory/stock/out      { warehouseId, productId, shelfId?, quantity, remark }
POST   /inventory/stock/transfer { warehouseId, productId, fromShelfId, toShelfId, quantity, remark }
GET    /inventory/warehouses/{warehouseId}/stocks   按商品聚合的库存分布（含各货架明细）
```

`shelfId` 缺省 = `0`（未上架），保持旧调用向后兼容。

### 4.4 业务规则

| 规则 | 说明 |
| --- | --- |
| 货架编号唯一 | 同一仓内 `code` 不可重复，重名给明确提示 |
| 删除货架 | 该货架仍有库存（quantity > 0）→ 拒绝，提示先移库 |
| 出库 | 目标货架库存不足 → 拒绝（原有逻辑保留） |
| 移库 | 源货架库存不足 → 拒绝；源=目标 → 拒绝；数量必须 > 0 |
| 容量 | 上架后 `SUM(quantity) > capacity` → **仅提示不阻断**（真实场景常超卖） |
| 流水 | 每次出入库/移库都写 `ledger_stock_flow`，移库写 **1 条**（flow_type=transfer，带 shelf_id + to_shelf_id） |
| 成本 | `cost_value = cost_price × quantity`，按货架行各算各的 |

---

## 五、UI 设计（移动端仓库页）

### 5.1 结构

```
仓库页
├── 仓库切换条（保留）
├── [货架] [商品] 分段切换
│   ├── 货架 Tab
│   │   ├── 货架卡片网格（编号 · 名称 · 区域 · 图片缩略 · 商品数/件数）
│   │   └── ＋新增货架
│   └── 商品 Tab（原商品列表 + 下单入口）
└── 货架详情弹层
    ├── 图片轮播（多张，可上传/删除）
    ├── 位置信息（区域 / 排 / 层 / 位 / 类型 / 容量）
    ├── 归属（仓库名 · 创建人）
    └── 该货架商品明细（商品 · 数量 · 成本值）
```

### 5.2 交互要点

- 新增/编辑货架：编号、名称、区域、排/层/位、类型、容量、备注、图片（可选多张）
- 商品新增/编辑：支持多图上传（第一张为封面）
- 出入库弹层：增加「货架」选择器（默认「未上架」）
- 移库入口：货架详情内「移库」按钮 → 选目标货架 + 数量
- 商品行显示总库存，点开看「分布在哪几个货架」

---

## 六、与记账/资产的联动

| 场景 | 联动 |
| --- | --- |
| 采购入库（赊账） | 库存 +N（指定货架）→ 自动建应付款（已有能力） |
| 销售出库（赊账） | 库存 −N（指定货架）→ 自动建应收款（已有能力） |
| 库存资产计价 | **总资产中的库存 = SUM(ledger_stock.cost_value)**，已有口径不变，只是从「单行」变「多行求和」 |
| 现金现结采购/销售 | **本次不做**（后端现结仍未实现，已有赊账路径不受影响） |

---

## 七、开发分期

| 阶段 | 内容 | 交付 |
| --- | --- | --- |
| **P0-1** | DDL：`ledger_shelf` 新表 + `ledger_stock.shelf_id` + `ledger_stock_flow` 货架维度；init-sql 同步 | 增量脚本可重复执行 |
| **P0-2** | 后端：`LedgerShelf` 实体/Mapper/Service/Controller（CRUD + 图片） | 接口可用 |
| **P0-3** | 后端：出入库带 `shelfId` + 移库 + 按货架聚合 + 图片接口 | 接口可用 |
| **P0-4** | 移动端：货架 Tab + 货架详情弹层 + 图片上传 + 出入库/移库货架选择 | 页面可用 |
| **P1** | 移动端商品详情（图片轮播 + 库存分布） | 页面可用 |
| **P2** | 后台 `moyun-admin-vue` 货架管理 + 合伙仓成员权限 | 可选 |

---

## 八、验收标准

1. 能建多个货架，同一仓内编号重复被拒绝
2. 货架可传多张图片，第一张为封面，可删除
3. 同一商品可在多个货架上有库存，商品总库存 = 各货架之和
4. 入库/出库可选货架；不选即「未上架」
5. 移库后源货架减、目标货架加，流水可追溯（含源/目标货架）
6. 有库存的货架不能删除，提示先移库
7. 货架详情能看到该货架上的所有商品与数量
8. 库存资产总额 = 各货架 `cost_value` 之和（口径与改造前一致）
9. 旧的出入库调用（不传 shelfId）行为不变

---

## 九、明确不做（克制）

- ❌ 批次/序列号/保质期管理
- ❌ 多仓调拨单（跨仓库转移）
- ❌ 货架三维可视化
- ❌ 条形码扫描入库
- ❌ 库存预警阈值（后续可加）
- ❌ 现结采购/销售（后端尚未实现）

---

## 十、一句话总结

**货架是仓库里的"第二层归属"：仓库 → 货架 → 商品 → 库存行。图片走 `sys_file` 通用附件（1:N），
库存按 `(商品, 货架)` 拆分并以和为总量，出入库/移库全部落流水。不引入批次、调拨单等 ERP 概念。**
