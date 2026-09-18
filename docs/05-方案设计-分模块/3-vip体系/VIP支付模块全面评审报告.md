# VIP 与支付模块全面评审报告

> 评审基准：moyun-dev-kouzi 分支 cd8d39c  
> 评审日期：2026-09-18  
> 评审范围：**文档 + 代码**，**前端 + 后端**，**前台 + 后台**  
> - 后端：vip 包（22 文件）+ pay 包（36 文件）+ 3 个回调 Handler + 2 个 Portal VIP Controller + 6 个 CMS Pay Controller + Mapper XML + DDL/迁移脚本  
> - 前端：moyun-portal（7 文件）+ moyun-admin-vue（10 文件）+ moyun-ledger-app（3 文件）  
> - 文档：VIP 设计方案 v2.1 + 技术架构 + 项目介绍 + 功能排查清单 + 项目现状总结 + devlog + 三端 README + 第三方服务指导 + 开发规范  
> 评审目标：发现问题 + 端（platform）维度补全 + 前后端一致性 + 文档与代码对齐 + 运营层次整合

---

## 一、模块全貌

### 1.1 VIP 模块架构（已实现端维度）

```
sys_platform（端定义，全局公共）
  ├── vip_tier（等级，一端一套，platform_code）
  │   ├── vip_tier_benefit（等级权益关联，platform_code）
  │   └── vip_user_card（用户会员卡，一端一卡，platform_code）
  ├── vip_benefit（权益定义，platform_code）
  │   └── vip_benefit_usage（使用记录，platform_code）
  └── vip_api_registry（接口注册表，@VipOnly 扫描生成，platform_code）

@VipOnly 注解 → VipApiScanner 启动扫描 → VipOnlyAspect 运行时切面
  → IVipService.isVipEnabled(端级开关) → hasBenefit / consumeBenefit
  → Redis 原子计数 + DB upsert 兜底 → 周期段自动滚动
```

### 1.2 支付模块架构（端维度不完整）

```
pay_order（统一支付单，有 platform 字段）
  ├── pay_ledger_entry（分账流水，有 platform 列但实体无映射 ← P0）
  ├── pay_notification（支付通知，无端维度 ← P1）
  ├── pay_notify_log（通知日志，无端维度 ← P1）
  │
  ├── user_account（单钱包，userId 主键，无端维度 ← 设计决策）
  ├── withdraw_order（提现单，无端维度 ← P1）
  ├── user_bank_card（银行卡，无端维度 ← 合理）
  │
  └── PayCallbackHandler（回调分发接口）
      ├── VipPayCallbackHandler（bizType=vip，bizNo=platform:tier:uuid）
      ├── TipPayCallbackHandler（bizType=tip，portal 打赏）
      └── LedgerTipPayCallbackHandler（bizType=ledger_tip，ledger 打赏）
```

### 1.3 前端三端全貌

| 端 | 项目 | VIP 文件 | 支付文件 | 端维度传递 |
|------|------|----------|----------|------------|
| C 端前台 | moyun-portal | 1 API + 1 页面 | 2 API + 2 页面 + 1 组件 | 不传（合理，单端应用） |
| B 端后台 | moyun-admin-vue | 1 API + 6 页面 + 1 平台管理页 | 1 API + 4 页面 | VIP 有端筛选，支付无端筛选 |
| C 端 App | moyun-ledger-app | 1 页面 | 1 API + 1 页面 | 不传（合理，单端应用） |

### 1.4 文档全貌

| 文档 | VIP 相关 | 支付相关 | 端维度 |
|------|----------|----------|--------|
| VIP 设计方案 v2.1 | 完整设计（16 章） | §8 支付改造 | 核心设计：端为公共概念 |
| 技术架构 v11.98 | 无 | §2.2 pay 模块 | 无端维度 |
| 项目介绍 v11.98 | §3.3 商业化 | §3.3 支付通道 | 无端维度 |
| 功能排查清单 v10.6 | **无 VIP 测试** | L84 钱包订单（4 节，引用旧 Controller） | 无端维度 |
| 项目现状总结 v11.98 | §5 支付商业化 | §5 支付与商业化 | 提及端概念但未展开 |
| devlog v12.2.3 | v12.0 VIP 统一 | v11.79 支付统一标准 | 有记录 |
| 三端 README | portal/admin/ledger 各有 | 各有 | admin README 路径不一致 |

---

## 二、端（platform）维度覆盖矩阵

### 2.1 后端表/实体端维度覆盖

| 模块 | 表名 | 列名 | 实体字段 | DDL | 代码写入 | 代码查询 | 覆盖率 |
|------|------|------|----------|-----|----------|----------|--------|
| **VIP** | sys_platform | platform_code | ✅ | ✅ | ✅ | ✅ | 100% |
| | vip_tier | platform_code | ✅ | ✅ | ✅ | ✅ | 100% |
| | vip_benefit | platform_code | ✅ | ✅ | ✅ | ✅ | 100% |
| | vip_tier_benefit | platform_code | ✅ | ✅ | ✅ | ✅ | 100% |
| | vip_user_card | platform_code | ✅ | ✅ | ✅ | ✅ | 100% |
| | vip_benefit_usage | platform_code | ✅ | ✅ | ✅ | ✅ | 100% |
| | vip_api_registry | platform_code | ✅ | ✅ | ✅ | ✅ | 100% |
| **支付** | pay_order | platform | ✅ | ✅ | ✅ | ✅ | 100% |
| | pay_ledger_entry | platform | **❌ 无字段** | ✅ 有列 | **❌ 从不写入** | N/A | **0%** |
| | pay_notification | — | **❌ 无字段** | ❌ 无列 | N/A | N/A | **0%** |
| | pay_notify_log | — | **❌ 无字段** | ❌ 无列 | N/A | N/A | **0%** |
| | user_account | — | **❌ 无字段** | ❌ 无列 | N/A | N/A | **0%（设计决策）** |
| | withdraw_order | — | **❌ 无字段** | ❌ 无列 | N/A | N/A | **0%** |
| | user_bank_card | — | **❌ 无字段** | ❌ 无列 | N/A | N/A | **0%（合理）** |

### 2.2 前端端维度覆盖

| 端 | VIP 管理端筛选 | 支付管理端筛选 | 收入总览端分组 |
|------|----------------|----------------|----------------|
| moyun-portal | N/A（单端） | N/A（单端） | N/A |
| moyun-admin-vue | ✅ 等级/权益/矩阵/会员卡/使用统计均有端筛选 | **❌ 订单/钱包/提现无端筛选** | ✅ 后端返回 platforms 数组 |
| moyun-ledger-app | N/A（单端） | N/A（单端） | N/A |

### 2.3 文档端维度覆盖

| 文档 | 端维度设计 | 端维度实现对照 |
|------|------------|----------------|
| VIP 设计方案 v2.1 | ✅ 完整（§2 端定义/§3 数据模型/§9 配置/§10 用户） | §10 用户体系改造（portal_user 加 platform_code）**未实现** |
| 功能排查清单 | ❌ 无 VIP 模块 | L84 仍引用旧 PortalWalletController |
| 技术架构 | ❌ 无端维度 | pay 模块描述无端概念 |
| 项目介绍 | ❌ 无端维度 | 商业化描述无端概念 |
| admin README | ❌ 路径不一致 | 写 `cms/vip/`，实际是 `system/vip/` |

---

## 三、问题清单

### 3.1 P0 — 严重缺陷（阻断运营链路）

#### BP0-1: pay_ledger_entry.platform 有列无映射 — 流水表端维度断裂

**现象**：DDL 迁移 `20260917-02-vip-unified-system.sql` 添加了 `ALTER TABLE pay_ledger_entry ADD COLUMN platform VARCHAR(16)`，但：
- `LedgerEntry.java` 实体**没有** platform/platformCode 字段
- `LedgerServiceImpl.settle()` 和 `settlePlatform()` 创建 LedgerEntry 时**从不写入** platform
- 结果：`pay_ledger_entry.platform` 列在数据库中**始终为 NULL**

**影响**：
- 后台"分账流水"页（`/cms/pay/ledger`）无法按端筛选流水
- 收入总览的"平台→渠道"两级聚合依赖 pay_order.platform，而非流水本身
- 运营无法追踪"某端某笔订单产生了多少平台抽成/用户所得"

**修复**：
1. `LedgerEntry.java` 增加 `private String platformCode;` 字段 + getter/setter
2. `LedgerServiceImpl.settle()` 签名增加 `platformCode` 参数，创建 LedgerEntry 时写入
3. `VipPayCallbackHandler.settlePlatform()` 传入 `platformCode`（从 bizNo 解析的 parts[0]）
4. `TipPayCallbackHandler` / `LedgerTipPayCallbackHandler` 传入对应端常量
5. `CmsPayLedgerController.list()` 增加 platformCode 筛选参数

---

#### BP0-2: 命名不一致 — platform vs platformCode

**现象**：
| 位置 | 列名 | 值域注释 | 实际代码写入 |
|------|------|----------|-------------|
| VIP 全表（7 张） | `platform_code` | portal/ledger | portal/ledger ✅ |
| pay_order | `platform` | ledger_app/portal（DDL 注释） | portal/ledger（代码常量） |
| pay_ledger_entry | `platform` | portal/ledger（DDL 注释） | NULL（从不写入） |
| sys_config | `platform_code` | NULL=全局 | NULL 或 portal ✅ |

**三处矛盾**：
1. DDL 注释 `20260914-05` 写 `ledger_app/portal`，但代码常量写 `ledger/portal`
2. DDL 注释 `20260917-02` 写 `portal/ledger`，与 `20260914-05` 矛盾
3. 列名 `platform`（pay）vs `platform_code`（VIP/sys_config）不统一

**影响**：
- 新开发者困惑：`platform` 和 `platform_code` 是同一个概念吗？
- 跨表 JOIN 需要额外映射，不能直接 `ON a.platform_code = b.platform`
- Revenue 后端硬编码 `ledger` → `ledger_app` 映射，与实际数据值域矛盾

**修复**：
1. 统一列名为 `platform_code`（与 VIP 全表对齐）
2. 统一值域为 `portal/ledger`（与 `sys_platform.platform_code` 对齐）
3. DDL 迁移：`ALTER TABLE pay_order CHANGE platform platform_code VARCHAR(50)`
4. `PayOrder.java` 字段名 `platform` → `platformCode`
5. `LedgerEntry.java` 新增字段直接用 `platformCode`

---

#### BP0-3: CmsPayRevenueController 硬编码端映射

**现象**：`CmsPayRevenueController.java` 第 119 行：
```java
String plat = parts.length > 0 && "ledger".equals(parts[0]) ? "ledger_app" : "portal";
```
将 bizNo 中的 `ledger` 映射为 `ledger_app`，但实际数据中 pay_order.platform 存的是 `ledger`（不是 `ledger_app`）。

**影响**：
- 收入总览页面端名称与实际数据不匹配
- 如果前端按 `ledger_app` 做筛选，查不到数据
- 硬编码不可维护，新增端需要改代码

**修复**：
1. 去除硬编码映射，直接使用 `sys_platform` 表查询端名称
2. 或直接使用 pay_order.platform 值，与 sys_platform.platform_code JOIN 获取端名称

---

#### BP0-4: 功能排查清单无 VIP 模块测试 — 文档与代码严重脱节

**现象**：`docs/04-测试验收/功能排查清单.md`（v10.6）：
- **没有** VIP 模块测试链路（无 VIP 订阅/发卡/权益消耗/到期失效测试）
- L84"钱包与订单"仅 4 节，引用 `PortalWalletController.list` / `PortalOrderController.list` — 这些 Controller 在 v12.0 VIP 统一重构后**可能已不存在**
- v12.0 删除了三套旧 VIP 全链路 31 文件，但功能排查清单未同步更新

**影响**：
- 测试团队无 VIP 功能测试依据
- 旧测试用例引用不存在的接口，执行即失败
- 文档"四同步"铁律（代码/文档/SQL/菜单同步）被违反

**修复**：
1. 新增"模块十九：VIP 会员体系"测试链路（L95-L99）
   - L95: VIP 订阅支付 → 发卡 → 权益生效
   - L96: 权益消耗 → 次数递减 → 超限拦截
   - L97: 会员续费 → 到期顺延
   - L98: VIP 开关 → 端级启停
   - L99: @VipOnly 注解 → 接口注册表 → 后台禁用
2. 修正 L84 引用的旧 Controller 为现行 Controller
3. 补充支付模块测试链路（mock 支付全链路/分账/提现审核）

---

### 3.2 P1 — 重要缺陷（影响运营完整性）

#### BP1-1: withdraw_order 缺端维度

**现象**：`WithdrawOrder.java` 无 platformCode 字段，DDL 也无此列。

**影响**：提现审核时无法知道收益来自哪个端，无法按端统计提现额。

**修复**：DDL 加列 + 实体加字段 + `WithdrawOrderServiceImpl.auditPass()` 写入端维度。

---

#### BP1-2: pay_notification / pay_notify_log 缺端维度

**现象**：两个实体均无 platformCode，DDL 也无此列。

**影响**：支付通知无法按端筛选，通知日志无法按端归档。

**修复**：DDL 加列 + 实体加字段 + `PayGatewayImpl.handleNotification()` 写入端维度（从 pay_order 关联获取）。

---

#### BP1-3: CmsPayOrderController 订单管理不支持按端筛选

**现象**：`CmsPayOrderController.list()` 只支持 status/bizType/payNo 筛选，无 platformCode 参数。前端 `order/index.vue` 也无端下拉。

**影响**：管理员无法筛选"门户端订单"或"记账端订单"。

**修复**：后端 list 方法增加 platformCode 参数 + 前端增加端下拉筛选组件。

---

#### BP1-4: PortalLedgerVipController 绕过 VipService 直接查 Mapper

**现象**：`PortalLedgerVipController.java` 中部分查询直接调用 `vipUserCardMapper.selectList()` 而非通过 `IVipService` 接口。

**影响**：违反分层架构，VipService 的缓存/逻辑被绕过，后续维护困难。

**修复**：将直接 Mapper 调用改为 IVipService 方法调用。

---

#### BP1-5: 费率不支持按端配置

**现象**：`LedgerServiceImpl` 使用 `sys_config` 的 `pay.platform.fee-rate` 全局费率，但 `sys_config` 表已有 `platform_code` 列，未利用端级费率。

**影响**：门户端和记账端无法配置不同分账费率。

**修复**：`LedgerServiceImpl` 查询费率时传入 platformCode，优先查端级配置，回退全局。

---

#### BP1-6: PayCashierPage 轮询无超时退避

**现象**：`moyun-portal/src/pages/pay/PayCashierPage.vue` 使用 `setInterval` 每 2 秒轮询支付状态，无最大重试次数，无指数退避。

**影响**：用户关闭支付但未离开页面时，轮询永不停止，浪费服务器资源。

**修复**：增加最大轮询次数（如 60 次=2 分钟）+ 超时后提示"支付超时"。

---

#### BP1-7: admin 订单 bizType 选项不完整

**现象**：`moyun-admin-vue/src/views/cms/pay/order/index.vue` bizType 下拉只有"打赏(tip)"，缺少"VIP订阅(vip)"和"记账打赏(ledger_tip)"。

**影响**：管理员无法按 VIP 订单类型筛选。

**修复**：补充 bizType 选项为 `tip` / `vip` / `ledger_tip`。

---

#### BP1-8: VIP 设计方案 §10 用户体系改造未实现

**现象**：设计方案 v2.1 §10 明确要求 `ALTER TABLE portal_user ADD COLUMN platform_code VARCHAR(50) DEFAULT 'portal' COMMENT '注册来源端'`，但实际代码 `PortalUser.java` 无此字段，DDL 也未执行此 ALTER。

**影响**：
- 无法追踪用户注册来源端
- 设计方案的核心目标"端是公共概念，被用户模块引用"未达成
- 跨端用户分析无法实现

**修复**：DDL 加列 + 实体加字段 + 注册接口写入来源端。

---

### 3.3 P2 — 一般缺陷（改进项）

#### BP2-1: UserAccount 单钱包无端维度 — 设计决策需文档化

**现象**：`UserAccount.java` 使用 `userId` 作为主键，一用户一账户，不支持按端分账。这在设计中是**有意决策**（单钱包公账体系），但：
- 设计方案 v2.1 §2.2 明确写"用户模块引用端"——但 UserAccount 没有引用
- 如果未来需要按端统计"某端用户总收入"，需从 pay_ledger_entry 聚合（但该表 platform 也未写入——见 BP0-1）

**建议**：保持单钱包架构，但必须在 `pay_ledger_entry` 补全端维度（BP0-1 修复后）才能实现按端统计。在文档中明确记录"UserAccount 单钱包是设计决策，按端统计走 ledger_entry 聚合"。

---

#### BP2-2: TipModal 金额硬编码

**现象**：`moyun-portal/src/components/TipModal.vue` 打赏金额选项 6/18/66/88/188 写死在组件中。

**建议**：改为后端 sys_config 可配置，或读取 vip_tier 的价格作为打赏建议金额。

---

#### BP2-3: admin README 路径不一致

**现象**：`moyun-admin-vue/README.md` 写 VIP 管理在 `cms/vip/`，但实际代码在 `system/vip/`。

**修复**：更新 README 路径为 `system/vip/`。

---

#### BP2-4: 技术架构/项目介绍文档无端维度

**现象**：`docs/01-架构设计/技术架构.md` 和 `项目介绍.md` 描述 pay 模块时无端概念。

**建议**：补充端维度说明，对齐 VIP 设计方案 v2.1 的"端为公共概念"设计。

---

#### BP2-5: 银行卡实名校验 TODO 占位

**现象**：`BankCardServiceImpl.java` 第 73/133/136/138 行多处 TODO，实名校验直接放行为 PENDING。

**风险**：生产环境接入前必须补全真实四要素/二要素验证通道。

**建议**：在第三方服务申请指导文档中已列为 P1，保持跟踪。

---

#### BP2-6: 提现真实出金通道预留

**现象**：`WithdrawOrderServiceImpl.java` 第 41/135 行注释"真实出金（商户号转账到银行卡）预留调用点，当前演示环境记账先行"。

**风险**：生产环境接入前必须补全真实转账通道。

**建议**：同 BP2-5，在第三方服务指导文档中保持跟踪。

---

#### BP2-7: mock 支付为默认开启

**现象**：`PayProperties.java` 第 54 行 `private boolean mockEnabled = true;` 默认开启 mock 模拟支付。

**风险**：生产部署时如果忘记配置 `moyun.pay.wechat.mock-enabled=false`，用户可绕过真实支付直接触发支付成功。

**建议**：增加启动时检查 — 当 `COZE_PROJECT_ENV=PROD` 或 `Spring.profiles.active=prod` 时，强制 `mockEnabled=false`，否则启动报错。

---

## 四、前后端一致性分析

### 4.1 C 端前台（portal + ledger-app）

**结论：合理，无问题。**

- portal 不传 platformCode → 后端 `PortalVipController.PLATFORM = "portal"` 常量注入
- ledger-app 不传 platformCode → 后端 `PortalLedgerVipController.PLATFORM = "ledger"` 常量注入
- C 端单端应用，端由后端常量确定，前端不需要传端码

### 4.2 B 端后台（admin-vue）

**结论：VIP 有端维度协同，支付无端维度协同 — 断裂。**

| 管理页 | 后端支持端筛选 | 前端有端下拉 | 一致性 |
|--------|----------------|-------------|--------|
| VIP 等级 | ✅ | ✅ | ✅ 一致 |
| VIP 权益 | ✅ | ✅ | ✅ 一致 |
| 等级权益矩阵 | ✅ 必填 | ✅ | ✅ 一致 |
| 接口注册 | ✅ | ✅ | ✅ 一致 |
| 会员卡 | ✅ | ✅ | ✅ 一致 |
| 使用统计 | ✅ | ✅ | ✅ 一致 |
| 端管理 | ✅ | ✅ | ✅ 一致 |
| **支付订单** | **❌ 无参数** | **❌ 无下拉** | ⚠️ 前后端一致地缺失 |
| **分账流水** | **❌ 无参数** | **❌ 无下拉** | ⚠️ 前后端一致地缺失 |
| **钱包账户** | N/A（UserAccount 无端维度） | ❌ 无下拉 | — |
| **提现审核** | **❌ 无参数** | **❌ 无下拉** | ⚠️ 前后端一致地缺失 |
| 收入总览 | ✅ 后端返回 platforms | ✅ 前端渲染 | ✅ 一致 |

### 4.3 值域一致性

**结论：前端值域与 sys_platform 一致，问题在后端 DDL 注释和 Revenue 硬编码。**

| 来源 | 值域 | 一致性 |
|------|------|--------|
| sys_platform 表数据 | portal/ledger/admin/personality | 基准 |
| VIP 全表代码 | portal/ledger | ✅ 一致 |
| pay_order 代码常量 | portal/ledger | ✅ 一致 |
| pay_order DDL 注释 | ledger_app/portal | ❌ 矛盾 |
| CmsPayRevenueController 硬编码 | ledger → ledger_app | ❌ 矛盾 |
| 前端三端 | 不传端码 | ✅ 合理 |

---

## 五、文档与代码对齐分析

### 5.1 VIP 设计方案 v2.1 与代码对齐

| 设计章节 | 设计要求 | 代码实现 | 对齐 |
|----------|----------|----------|------|
| §2.1 端定义 | 4 端（portal/ledger/admin/personality） | ✅ sys_platform 4 行初始化 | ✅ |
| §3 数据模型 | 7 张表 + DDL | ✅ 全部建表 + 实体 | ✅ |
| §4 注解设计 | @VipOnly(platform, benefit, consume, message) | ✅ 完全对齐 | ✅ |
| §5 启动扫描 | VipApiScanner + upsert | ✅ 完全对齐 | ✅ |
| §6 运行时切面 | VipOnlyAspect + 开关/注册表/权益校验 | ✅ 完全对齐 | ✅ |
| §7 VipService | 7 个接口方法 | ✅ 全部实现 | ✅ |
| §8.1 支付改造 | pay_order 复用 platform + pay_ledger_entry 加列 | ⚠️ pay_order ✅ / ledger_entry **有列无映射** | ⚠️ |
| §8.2 支付回调 | VipPayCallbackHandler bizNo 解析 | ✅ 完全对齐 | ✅ |
| §8.3 收支统计 | 按 platform 分组 | ⚠️ SQL 正确但 Revenue Controller 硬编码映射 | ⚠️ |
| §9 配置改造 | sys_config 加 platform_code | ✅ 完全对齐 | ✅ |
| §10 用户改造 | portal_user 加 platform_code | **❌ 未实现** | ❌ |
| §11 后台管理 | 6 页 + 端管理 | ✅ 全部实现 | ✅ |
| §12 12 个欠考虑点 | 12 项 | ✅ 全部处理 | ✅ |
| §15.1 旧三套清理 | DROP 8 表 + 删 31 文件 | ✅ 全部清理 | ✅ |

**对齐率：13/14 = 93%**，唯一未实现项是 §10 用户体系改造（portal_user 加 platform_code）。

### 5.2 功能排查清单与代码对齐

**对齐率：严重不足。**

- v12.0 删除旧三套 VIP 31 文件，但功能排查清单未更新
- 无 VIP 统一体系测试链路
- L84 钱包订单引用的 PortalWalletController/PortalOrderController 可能已不存在
- 无支付回调全链路测试（mock 支付 → 回调 → 分账 → 发卡）
- 无提现审核测试链路

### 5.3 devlog 与代码对齐

**对齐率：良好。**

- v12.0 记录了 VIP 统一体系完整实施（SQL/后端/前端/删除旧体系）
- v11.79 记录了支付统一标准重构
- v11.81-85 记录了三套 VIP 逐步接入公共支付通道到统一
- v11.84 记录了回调 NPE 修复
- devlog 记录完整，与代码一致

### 5.4 三端 README 与代码对齐

| README | VIP 描述 | 支付描述 | 对齐 |
|--------|----------|----------|------|
| moyun-portal | ✅ pay/ 目录含 VIP 会员 | ✅ 打赏/钱包/收银台 | ✅ |
| moyun-admin-vue | ❌ 写 `cms/vip/`，实际 `system/vip/` | ✅ cms/pay/ | ⚠️ 路径不一致 |
| moyun-ledger-app | ✅ vip/ 页面 + webview 内嵌 | ✅ tip/ 页面 | ✅ |

---

## 六、整改方案

### 6.1 修复优先级清单

| 优先级 | 编号 | 问题 | 工作量 | 依赖 |
|--------|------|------|--------|------|
| **P0** | BP0-1 | LedgerEntry 补 platformCode 字段 + 服务联动 | 中 | 无 |
| **P0** | BP0-2 | 统一命名 platform → platform_code | 中 | BP0-1 |
| **P0** | BP0-3 | Revenue 去硬编码映射 | 小 | BP0-2 |
| **P0** | BP0-4 | 功能排查清单补 VIP/支付测试链路 | 中 | 无 |
| **P1** | BP1-1 | WithdrawOrder 加端维度 | 小 | BP0-2 |
| **P1** | BP1-2 | PayNotification/PayNotifyLog 加端维度 | 小 | BP0-2 |
| **P1** | BP1-3 | 订单管理加端筛选（前后端） | 小 | BP0-2 |
| **P1** | BP1-4 | PortalLedgerVipController 走 Service | 小 | 无 |
| **P1** | BP1-5 | 费率按端配置 | 小 | 无 |
| **P1** | BP1-6 | PayCashierPage 轮询超时 | 小 | 无 |
| **P1** | BP1-7 | admin bizType 选项补全 | 小 | 无 |
| **P1** | BP1-8 | portal_user 加 platform_code | 小 | 无 |
| **P2** | BP2-1 | UserAccount 文档化设计决策 | 小 | BP0-1 |
| **P2** | BP2-2 | TipModal 金额可配置 | 小 | 无 |
| **P2** | BP2-3 | admin README 路径修正 | 小 | 无 |
| **P2** | BP2-4 | 架构文档补端维度 | 小 | 无 |
| **P2** | BP2-5 | 银行卡实名校验 TODO | 中 | 生产接入 |
| **P2** | BP2-6 | 提现真实出金 TODO | 中 | 生产接入 |
| **P2** | BP2-7 | mock 默认开启安全风险 | 小 | 无 |

### 6.2 DDL 迁移脚本草案

```sql
-- 20260918-01-vip-pay-platform-unify.sql

-- ========== P0-2: 统一命名 platform → platform_code ==========
ALTER TABLE pay_order CHANGE COLUMN platform platform_code VARCHAR(50) COMMENT '归属端代码（sys_platform.platform_code）';
ALTER TABLE pay_ledger_entry CHANGE COLUMN platform platform_code VARCHAR(50) COMMENT '归属端代码';

-- ========== P0-1: pay_ledger_entry 端维度已有列，无需加列，仅需代码补字段 ==========

-- ========== P1-1: withdraw_order 加端维度 ==========
ALTER TABLE pay_withdraw_order ADD COLUMN platform_code VARCHAR(50) DEFAULT NULL COMMENT '归属端代码';

-- ========== P1-2: pay_notification / pay_notify_log 加端维度 ==========
ALTER TABLE pay_notification ADD COLUMN platform_code VARCHAR(50) DEFAULT NULL COMMENT '归属端代码';
ALTER TABLE pay_notify_log ADD COLUMN platform_code VARCHAR(50) DEFAULT NULL COMMENT '归属端代码';

-- ========== P1-8: portal_user 加端维度（VIP 设计方案 §10） ==========
ALTER TABLE portal_user ADD COLUMN platform_code VARCHAR(50) DEFAULT 'portal' COMMENT '注册来源端';

-- ========== 索引 ==========
CREATE INDEX idx_pay_ledger_platform ON pay_ledger_entry(platform_code);
CREATE INDEX idx_pay_withdraw_platform ON pay_withdraw_order(platform_code);
CREATE INDEX idx_pay_notification_platform ON pay_notification(platform_code);
CREATE INDEX idx_portal_user_platform ON portal_user(platform_code);
```

### 6.3 代码修改清单

| 文件 | 修改内容 | 关联问题 |
|------|----------|----------|
| LedgerEntry.java | 增加 platformCode 字段 + getter/setter | BP0-1 |
| LedgerServiceImpl.java | settle()/settlePlatform() 增加 platformCode 参数 | BP0-1 |
| VipPayCallbackHandler.java | settlePlatform() 传入 platformCode | BP0-1 |
| TipPayCallbackHandler.java | settle() 传入 "portal" | BP0-1 |
| LedgerTipPayCallbackHandler.java | settle() 传入 "ledger" | BP0-1 |
| PayOrder.java | platform → platformCode（字段名 + getter/setter） | BP0-2 |
| WithdrawOrder.java | 增加 platformCode 字段 | BP1-1 |
| PayNotification.java | 增加 platformCode 字段 | BP1-2 |
| PayNotifyLog.java | 增加 platformCode 字段 | BP1-2 |
| PortalUser.java | 增加 platformCode 字段 | BP1-8 |
| CmsPayRevenueController.java | 去除硬编码，查 sys_platform 获取端名 | BP0-3 |
| CmsPayOrderController.java | list() 增加 platformCode 参数 | BP1-3 |
| CmsPayLedgerController.java | list() 增加 platformCode 参数 | BP0-1 |
| LedgerServiceImpl.java | 费率查询传 platformCode | BP1-5 |
| PortalLedgerVipController.java | 直接 Mapper 调用改为 IVipService | BP1-4 |
| PortalPayController.java | pay_order 相关字段名跟随 | BP0-2 |
| WithdrawOrderServiceImpl.java | 审核通过时写入 platformCode | BP1-1 |
| PayGatewayImpl.java | handleNotification 写入 platformCode | BP1-2 |
| PortalRegistrationService | 注册时写入来源端 platformCode | BP1-8 |
| **前端** | | |
| order/index.vue | 增加端下拉 + bizType 补全 | BP1-3/BP1-7 |
| withdraw/index.vue | 增加端下拉 | BP1-1 |
| PayCashierPage.vue | 轮询增加超时 + 退避 | BP1-6 |
| TipModal.vue | 金额改为可配置 | BP2-2 |
| admin README | cms/vip/ → system/vip/ | BP2-3 |
| **文档** | | |
| 功能排查清单.md | 新增 VIP/支付测试链路 L95-L99 | BP0-4 |
| 技术架构.md | pay 模块补端维度说明 | BP2-4 |
| 项目介绍.md | 商业化补端维度说明 | BP2-4 |

---

## 七、综合评分

### 7.1 评分维度

| 维度 | 评分 | 说明 |
|------|------|------|
| VIP 后端端维度 | **9/10** | 7 表 100% 覆盖，代码完整，仅 §10 用户改造未实现 |
| 支付后端端维度 | **3/10** | 7 表仅 1 表有端维度且流水有列无映射 |
| 前端 C 端端维度传递 | **8/10** | 合理不传，后端常量注入 |
| 前端 B 端端维度管理 | **5/10** | VIP 有端筛选，支付无端筛选，bizType 不全 |
| 前后端值域一致性 | **6/10** | 前端一致，后端 DDL 注释和 Revenue 硬编码矛盾 |
| 前端代码质量 | **6/10** | 轮询无超时、金额硬编码、组件功能完整但缺可配置性 |
| 文档完整性 | **4/10** | 功能排查清单严重脱节，架构文档无端维度 |
| 文档与代码对齐 | **6/10** | VIP 设计方案 93% 对齐，功能排查清单严重不足 |
| 命名一致性 | **4/10** | platform vs platform_code，三处值域矛盾 |
| 安全性 | **5/10** | mock 默认开启，银行卡 TODO，提现 TODO |

### 7.2 综合得分

**5.5 / 10**

- VIP 模块端维度设计优秀（9/10），是全项目端维度最完整的模块
- 支付模块端维度严重不足（3/10），是端维度断裂的核心瓶颈
- 文档与代码脱节（4/10），功能排查清单缺失 VIP 测试
- 命名不一致（4/10），三处矛盾增加维护成本
- 安全风险存在（5/10），mock 默认开启需生产前修复

### 7.3 整改后预期评分

| 阶段 | 修复后评分 | 提升 |
|------|-----------|------|
| P0 全部修复 | 7.0/10 | +1.5 |
| P0+P1 全部修复 | 8.2/10 | +2.7 |
| P0+P1+P2 全部修复 | 9.0/10 | +3.5 |

---

## 八、总结

### 8.1 核心发现

1. **VIP 模块端维度设计优秀但支付侧断裂**：VIP 全链路 7 表 100% 覆盖端维度，但支付侧 7 表仅 pay_order 有端维度，且 pay_ledger_entry 有列无映射导致流水按端统计链路断裂。这是"平台→模块→业务"运营层次在支付侧断链的核心原因。

2. **命名不一致是系统性问题**：`platform` vs `platform_code`，三处值域矛盾，DDL 注释与代码常量不一致，Revenue 硬编码映射。这不是单点错误而是设计时未统一命名规范。

3. **文档与代码脱节严重**：功能排查清单（v10.6）完全未覆盖 v12.0 VIP 统一体系，仍引用旧 Controller；VIP 设计方案 §10 用户改造未实现；架构文档无端维度。违反"四同步"铁律。

4. **安全风险需生产前修复**：mock 支付默认开启，银行卡实名校验 TODO 占位，提现真实出金通道预留。这些在开发阶段合理，但生产部署前必须修复。

### 8.2 整改路径

```
第一步（P0）：打通流水端维度
  └─ LedgerEntry 补字段 → 服务联动 → 统一命名 → Revenue 去硬编码
     → 补功能排查清单

第二步（P1）：补全支付端维度
  └─ WithdrawOrder/PayNotification 加端 → Controller 加筛选
     → 费率按端 → portal_user 加端 → 轮询超时

第三步（P2）：文档对齐 + 安全加固
  └─ README 路径修正 → 架构文档补端 → TipModal 可配置
     → mock 安全 → 生产通道接入
```

### 8.3 运营层次整合目标

修复后可实现：

```
平台维度（sys_platform）
  ├── 模块维度
  │   ├── VIP 模块（vip_tier / vip_benefit / vip_user_card …）→ 按端统计会员数/权益消耗
  │   ├── 支付模块（pay_order / pay_ledger_entry / withdraw_order …）→ 按端统计 GMV/分账/提现
  │   └── 用户模块（portal_user.platform_code）→ 按端统计注册来源
  │       └── 业务维度
  │           ├── 门户端：面试会员/简历优化/文章打赏/付费阅读
  │           ├── 记账端：记账 VIP/账单识别/AI 分析/打赏
  │           └── 管理端：运营管理（无 C 端业务）
  └── 配置维度（sys_config.platform_code）
      ├── 端级 VIP 开关（vip.enabled + platform_code）
      ├── 端级费率（pay.platform.fee-rate + platform_code）
      └── 端级 AI 开关（ai.global.enabled + platform_code）
```

---

> 报告路径：`/workspace/projects/public/VIP支付模块全面评审报告.md`  
> 评审基准：moyun-dev-kouzi cd8d39c  
> 评审覆盖：后端 58 文件 + 前端 20 文件 + 文档 12 份 + DDL 脚本 + Mapper XML
