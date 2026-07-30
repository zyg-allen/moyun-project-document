## 墨韵智库 - 数据库初始化脚本（拆分版）

本目录由 `all-db-ddl.sql`（5335 行）按模块拆分为 16 个编号文件，按编号顺序执行即可完成空库初始化。

> **v6.1 变更**：调整执行顺序（系统表提前到门户表之前，系统种子提前到门户种子之前）；合并原 60+61 为 10；修复 91/92 的 menu_id 断裂导致前端空白问题。

### 执行顺序

请按以下顺序依次执行 SQL 脚本（系统地基 → 框架表 → 业务表 → 种子数据 → 菜单权限 → 校验）：

| 序号 | 文件名 | 类型 | 说明 |
|------|--------|------|------|
| 00 | **00_开头设置.sql** | 设置 | `SET FOREIGN_KEY_CHECKS=0;` 关闭外键检查，最先执行 |
| 10 | **10_基础系统表.sql** | DDL | 系统基础表（24 张 sys_* 表，含 sys_role_menu）。v6.1 合并原 60+61 |
| 20 | **20_工作流表_act.sql** | DDL | Flowable 全部 39 张 `act_*` 表 |
| 30 | **30_定时任务表_qrtz.sql** | DDL | Quartz 全部 11 张 `qrtz_*` 表 |
| 40 | **40_代码生成表_gen.sql** | DDL | 代码生成业务表 `gen_table` + `gen_table_column` |
| 50 | **50_门户核心表.sql** | DDL | portal_article* / book* / category / tag 等核心业务表 |
| 51 | **51_门户扩展表.sql** | DDL | portal_comment* / feed_* / follow / friend_link / growth_* 等 |
| 52 | **52_门户面试学习表.sql** | DDL | portal_interview_* 全系列 |
| 53 | **53_门户互动社交表.sql** | DDL | portal_like / message* / order / report / topic_* / user* 等 |
| 80 | **80_系统种子数据.sql** | 种子数据 | sys_config / dept / dict_* / role / user / user_role 等。v6.1 提前到门户种子之前 |
| 81 | **81_门户种子数据.sql** | 种子数据 | portal_category（50 条）/ tag（28 条）/ book / task 等门户种子数据 |
| 90 | **90_菜单权限_RuoYi.sql** | 种子数据 | sys_menu 第一段：RuoYi 框架菜单（显式 ID 1-1060） |
| 91 | **91_菜单权限_CMS.sql** | 种子数据 | sys_menu 第二段：CMS 内容管理菜单（v6.1：AUTO_INCREMENT=2000，ID 2000+） |
| 92 | **92_角色菜单关联.sql** | 种子数据 | sys_role_menu 种子数据（v6.1：role_id=1 改为动态子查询，匹配 menu_id >= 2000） |
| 98 | **98_校验查询.sql** | 校验 | CMS 菜单完整性校验 SELECT 查询 |
| 99 | **99_结尾设置.sql** | 设置 | `SET FOREIGN_KEY_CHECKS=1;` 恢复外键检查，最后执行 |

### v6.1 修复说明

#### 问题：前端空白 / parentNode null
- **根因**：91 用自增插入 73 条 CMS 菜单（实际 ID 1061+），但 92 硬编码引用 222 个 menu_id（2000-2228），两者对不上，导致 222 条角色菜单关联全部悬空，admin 登录后拿不到 CMS 菜单 → 前端路由表为空 → 组件渲染失败
- **修复**：
  1. 91 开头加 `ALTER TABLE sys_menu AUTO_INCREMENT = 2000`，CMS 菜单 ID 从 2000 开始
  2. 92 的 role_id=1 改为动态子查询 `SELECT menu_id FROM sys_menu WHERE menu_id >= 2000`，不再依赖硬编码 ID

#### 顺序调整
- 系统基础表（10）提到门户表（50-53）之前：系统是地基，先建 sys_* 再建 portal_*
- 系统种子数据（80）提到门户种子（81）之前：先有 sys_user/admin，门户数据的 create_by 才有语义

### 文件分类说明

#### DDL 表结构（10-53 段）
- **10 段**：系统基础表（RuoYi 系统表 + sys_role_menu 关联表）
- **20-40 段**：框架表（Flowable / Quartz / 代码生成器）
- **50-53 段**：门户业务表（核心 / 扩展 / 面试学习 / 互动社交）
- 所有 DDL 文件**仅包含 CREATE TABLE**，不包含 INSERT 种子数据

#### 种子数据（80-92 段）
- **80 段**：系统基础表种子数据（已调整依赖顺序：dict_type → dict_data；role → user → user_role）
- **81 段**：门户业务表种子数据
- **90-91 段**：菜单权限数据（RuoYi 框架菜单 ID 1-1060 + CMS 菜单 ID 2000+）
- **92 段**：角色菜单关联数据（role_id=1 动态匹配 CMS 菜单，role_id=2 硬编码 RuoYi 菜单）

### 依赖关系

1. **DDL 必须先于种子数据**：所有 CREATE TABLE（10-53 段）必须在 INSERT（80-92 段）之前执行
2. **系统种子数据内部依赖**（80 段已调整顺序）：
   - `sys_dict_type` → `sys_dict_data`
   - `sys_role` → `sys_user` → `sys_user_role`
3. **菜单数据依赖**：90/91（sys_menu）必须在 92（sys_role_menu）之前执行
4. **角色菜单关联**：80（sys_role）必须在 92（sys_role_menu）之前执行
5. **校验查询**：98 在所有数据导入后执行

### 注意事项

1. **数据库要求**：MySQL 8.0+
2. **字符集**：utf8mb4 / utf8mb4_0900_ai_ci（ACT_* 表保留 utf8mb3，由 Flowable 框架约定）
3. **执行前准备**：
   - 确保数据库已创建并具有相应权限
   - 建议在执行前备份现有数据
   - 按编号顺序执行，不要跳跃
4. **源文件**：`all-db-ddl.sql` 为完整源文件（5335 行），本目录所有编号文件均从中提取
5. **幂等性**：91 使用 `WHERE NOT EXISTS` 幂等插入，92 使用 `NOT EXISTS` 防重复，可重复执行

### 初始化后访问信息

- **后台管理系统地址**：`http://localhost:80`
- **后台管理员账号**：`admin`
- **后台管理员密码**：`admin123`
- **前台地址**：`http://localhost:5173` 或配置的端口
- **前台测试账号**：`admin` / `123456` 或 `zhangsan` / `123456`

### 归档文件

- `all-db-ddl.sql` - 完整源文件（5335 行），拆分前的原始 DDL 脚本，仅供参考
