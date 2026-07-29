## 墨韵智库 - 数据库初始化脚本（拆分版）

本目录由 `all-db-ddl.sql`（5335 行）按模块拆分为 17 个编号文件，按编号顺序执行即可完成空库初始化。

### 执行顺序

请按以下顺序依次执行 SQL 脚本（DDL 先于种子数据，外键检查首尾包裹）：

| 序号 | 文件名 | 类型 | 说明 |
|------|--------|------|------|
| 00 | **00_开头设置.sql** | 设置 | `SET FOREIGN_KEY_CHECKS=0;` 关闭外键检查，最先执行 |
| 10 | **10_工作流表_act.sql** | DDL | Flowable 全部 39 张 `act_*` 表（含 act_ge_property/act_id_property/act_re_procdef 内嵌配置数据） |
| 20 | **20_定时任务表_qrtz.sql** | DDL | Quartz 全部 11 张 `qrtz_*` 表 |
| 30 | **30_代码生成表_gen.sql** | DDL | 代码生成业务表 `gen_table` + `gen_table_column` |
| 40 | **40_门户核心表.sql** | DDL | portal_achievement / ad_slot / article* / book* / bookmark / bookshelf / category / tag |
| 50 | **50_门户扩展表.sql** | DDL | portal_code_run / column* / comment* / contest* / creator_* / entity_tag / feed_* / feedback / follow / friend_link / growth_* / help_* |
| 51 | **51_门户面试学习表.sql** | DDL | portal_interview_* 全系列 |
| 52 | **52_门户互动社交表.sql** | DDL | portal_like / message* / mock_interview* / notification_bak / order / pk_challenge / reading_* / report / shop_* / study_plan* / task / tip_order / topic_* / user* / vip_package / wallet* / writing_* / wrong_question |
| 60 | **60_基础系统表.sql** | DDL | sys_config / deploy_form / dept / dict_data / dict_type / expression / file / form / job / job_log / listener / logininfor / menu / notice_bak / notification / notification_read / oper_log / post / role / role_dept / user / user_post / user_role |
| 61 | **61_系统关联表_菜单与角色.sql** | DDL | sys_role_menu 关联表 CREATE TABLE |
| 80 | **80_门户种子数据.sql** | 种子数据 | portal_achievement / book / book_chapter / book_quote / book_recommend / category（50 条）/ tag（28 条）/ friend_link / growth_rule / help_article / help_category / interview_category / interview_position / shop_item / task / user / writing_prompt 的 INSERT 数据 |
| 81 | **81_系统种子数据.sql** | 种子数据 | sys_config / dept / dict_type / dict_data / job / post / role / role_dept / user / user_post / user_role / notice_bak 的 INSERT 数据（已调整依赖顺序：dict_type → dict_data；role → user → user_role） |
| 90 | **90_菜单权限_RuoYi.sql** | 种子数据 | sys_menu 第一段：RuoYi 框架自带菜单（INSERT，已删除诊断语句 SHOW OPEN TABLES） |
| 91 | **91_菜单权限_CMS.sql** | 种子数据 | sys_menu 第二段：CMS 内容管理菜单完整初始化（INSERT） |
| 92 | **92_角色菜单关联.sql** | 种子数据 | sys_role_menu 种子数据（INSERT） |
| 98 | **98_校验查询.sql** | 校验 | CMS 菜单完整性校验 SELECT 查询 |
| 99 | **99_结尾设置.sql** | 设置 | `SET FOREIGN_KEY_CHECKS=1;` 恢复外键检查，最后执行 |

### 文件分类说明

#### DDL 表结构（10-61 段）
- **10-30 段**：框架表（Flowable 工作流 / Quartz 定时任务 / 代码生成器）
- **40-52 段**：门户业务表（核心 / 扩展 / 面试学习 / 互动社交）
- **60-61 段**：系统基础表（RuoYi 系统表 + 关联表）
- 所有 DDL 文件**仅包含 CREATE TABLE**，不包含 INSERT 种子数据

#### 种子数据（80-92 段）
- **80 段**：门户业务表种子数据（INSERT，保留 LOCK/UNLOCK）
- **81 段**：系统基础表种子数据（INSERT，已调整依赖顺序）
- **90-91 段**：菜单权限数据（RuoYi 框架菜单 + CMS 内容管理菜单）
- **92 段**：角色菜单关联数据

#### 设置与校验（00 / 98 / 99 段）
- **00 段**：开头设置（关闭外键检查）
- **98 段**：校验查询（CMS 菜单完整性）
- **99 段**：结尾设置（恢复外键检查）

### 依赖关系

1. **DDL 必须先于种子数据**：所有 CREATE TABLE（10-61 段）必须在 INSERT（80-92 段）之前执行
2. **系统种子数据内部依赖**（81 段已调整顺序）：
   - `sys_dict_type` → `sys_dict_data`（字典数据引用字典类型）
   - `sys_role` → `sys_user` → `sys_user_role`（用户角色关联引用角色和用户）
3. **菜单数据依赖**：90/91 段（sys_menu）必须在 92 段（sys_role_menu）之前执行
4. **校验查询**：98 段在所有数据导入后执行，用于验证 CMS 菜单完整性

### 注意事项

1. **数据库要求**：MySQL 8.0+
2. **字符集**：utf8mb4 / utf8mb4_0900_ai_ci（ACT_* 表保留 utf8mb3，由 Flowable 框架约定）
3. **执行前准备**：
   - 确保数据库已创建并具有相应权限
   - 建议在执行前备份现有数据
   - 按编号顺序执行，不要跳跃
4. **源文件**：`all-db-ddl.sql` 为完整源文件（5335 行），本目录所有编号文件均从中提取，未修改任何 SQL 内容
5. **执行指南**：详细执行步骤参见 `SQL_EXECUTION_GUIDE.md`

### 初始化后访问信息

- **后台管理系统地址**：`http://localhost:80`
- **后台管理员账号**：`admin`
- **后台管理员密码**：`admin123`
- **前台地址**：`http://localhost:5173` 或配置的端口
- **前台测试账号**：`admin` / `123456` 或 `zhangsan` / `123456`

### 归档文件

- `all-db-ddl.sql` - 完整源文件（5335 行），拆分前的原始 DDL 脚本，仅供参考
