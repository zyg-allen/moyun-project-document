# 增量脚本目录（increment-sql）

本目录存放**存量库**的增量补丁：按文件名日期顺序执行（`YYYYMMDD-NN-描述.sql`）。
**全新库**请使用 `../init-sql/` 的初始化脚本（DDL + DML + 菜单 redo），不要执行本目录。

---

## 铁律：菜单增量脚本**不得写死现网 `menu_id` / `parent_id`**

### 为什么

- `init-sql/moyun-menu-redo.sql` 在全新库上会把 `sys_menu` **重编号**（显式 id 到 397，其余自增）；
- 而增量脚本若是按**现网 id**（如 `parent_id = 5068`、`menu_id = 5152`、`SELECT 397,'名称',26,...`）编写，
  在全新库上就会**挂到错误菜单或直接悬空**——这就是"菜单双轨"问题。

### 正确写法

父菜单/目标菜单一律**按业务键反查**，不引用任何数字 id。示例（本目录 `20261001-01` 即此写法）：

```sql
-- 新增按钮权限：父菜单按 perms 反查；幂等（NOT EXISTS）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `perms`, ...)
SELECT '银行卡人工核实', m.`menu_id`, 2, 'F', 'cms:payBankCard:verify', ...
FROM `sys_menu` m
WHERE m.`perms` = 'cms:payBankCard:list'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms` = 'cms:payBankCard:verify')
LIMIT 1;
```

要点：**不指定 `menu_id`**（交给自增）、**不写死 `parent_id`**（用 `SELECT ... FROM sys_menu WHERE perms=...` 反查）。
新增的菜单内容同时要**补进 `init-sql/moyun-menu-redo.sql`**，保证全新库与存量库两条路径等价（四同步）。

### 由守卫强制

`IncrementSqlIdempotencyGuardTest#newMenuScriptsMustNotHardcodeMenuIds` 会扫描本目录，
命中以下任一形态即**测试失败**：

1. `parent_id = <数字>` / `menu_id = <数字>` / `menu_id IN (<数字>...)`；
2. `INSERT INTO sys_menu (... menu_id ...)`（脚本自行分配主键）；
3. `INSERT ... SELECT` 中出现**裸数字 id 字面量**（按列位置写死，如 `SELECT '名称', 5068, 99`）。

### 历史脚本豁免（仅限登记在册的 6 个）

以下脚本编写于本规则确立之前，且**内容已逐条核对、等价行均在 `moyun-menu-redo.sql` 中**，
故保留现网 id（改写已执行过的迁移脚本风险更高）：

| 脚本 | 说明 |
|---|---|
| `20260928-03-补齐无菜单入口（竞赛广告提示词导入模板）.sql` | 竞赛管理菜单（redo 已含 `contest`） |
| `20260928-04-菜单图标归位失效图标修正.sql` | 图标修正（纯 UPDATE） |
| `20260928-05-审核中心菜单归并为一条.sql` | 审核中心（redo 已含 `auditTask`/`audit-center`） |
| `20260928-06-恢复被误删的分组页菜单.sql` | `feedback-center` / `help-center`（redo 已含） |
| `20260928-07-系统监控菜单归并去重.sql` | `server-panel`（redo 已含） |
| `20260929-03-简历解析配置菜单.sql` | 简历解析配置 397-401（redo 第 58 行与 396-399 行已含） |

豁免清单由 `legacyMenuScriptAllowlistStillExists` 自检：脚本若被删除，登记必须同步删除（防豁免腐烂）。

---

## 其它既有约定（由 `IncrementSqlIdempotencyGuardTest` 一并强制）

- 结构性 DDL（`ADD COLUMN` / `DROP INDEX` / `CHANGE COLUMN` 等）必须带 `information_schema` 前置判断，保证**可重复执行**；
- 破坏性清洗（`SET <列> = NULL`）必须同期创建 `..._bak_...` 备份表。
