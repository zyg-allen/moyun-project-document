## 墨韵智库 - 数据库初始化脚本

本目录包含一个整合的初始化脚本 `init_v7.8.sql`（空库一键建表 + 字段补齐 + 基础数据）和 4 个版本升级脚本，**必须按顺序全部执行**。

### 快速开始

```bash
# 1. 创建数据库（MySQL 8.0+）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS moyun DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

# 2. 按顺序执行（全部幂等，可重复执行）
mysql -uroot -p moyun < init_v7.8.sql
mysql -uroot -p moyun < upgrade_v8.1_audit_unified.sql    # 审核模块统一整合（sys_audit_task + 任务管理菜单）
mysql -uroot -p moyun < upgrade_v8.2_import_template.sql  # 通用导入模板 + 题库导入导出权限
mysql -uroot -p moyun < upgrade_v9.0_admin_refactor.sql   # v9.0 后台重构（删PK/圈子菜单，新增VIP/钱包/仪表板）
mysql -uroot -p moyun < upgrade_v9.5_merge.sql            # v9.5 合并版（恢复打赏流水Tab权限）
mysql -uroot -p moyun < upgrade_v9.6_admin_optimize.sql   # v9.6 菜单收敛 + 27类业务字典（前后台字典驱动）
```

### 脚本内容

| 段 | 内容 | 说明 |
|---|---|---|
| 一、设置段 | 关闭外键检查 | 加速批量插入 |
| 二、DDL 建表段 | CREATE TABLE IF NOT EXISTS | 系统表/qrtz/gen/门户/AI 全部表，幂等 |
| 三、字段补齐段 | ALTER TABLE（幂等） | v6.4~v7.25 升级脚本中的表结构变更，兼容 MySQL 8.0 |
| 四、基础数据段 | INSERT 基础数据 | 用户/角色/字典/栏目/标签等，不含业务测试数据 |
| 五、菜单权限段 | sys_menu + sys_role_menu | RuoYi + CMS + 消息中心 + v7.7~v7.24 菜单注册 |
| 六、校验段 | 完整性校验查询 | 表数量/菜单数量/角色用户校验 |
| 七、结尾设置 | 恢复外键检查 | — |

### 保留的基础数据

**系统基础数据**：
- `sys_config`（6条系统配置）
- `sys_dept`（10条部门）
- `sys_dict_type` + `sys_dict_data`（15+38条字典）
- `sys_post`（4条岗位）
- `sys_role`（2条角色：admin/common）
- `sys_user`（2条用户：admin/ry）
- `sys_role_dept` / `sys_user_post` / `sys_user_role`（关联关系）
- `sys_job`（3条定时任务）

**门户基础数据**：
- `portal_category`（50条栏目：8一级 + 42二级）
- `portal_tag`（28条标签：8人文 + 12技术 + 8通用）
- `portal_friend_link`（3条友链）
- `portal_growth_rule`（30条成长规则）
- `portal_help_category`（4条帮助分类）
- `portal_interview_category`（5条面试分类）
- `portal_achievement`（23条成就定义）
- `portal_interview_position`（3条岗位配置）
- `portal_task`（7条任务定义）

**菜单权限数据**：
- `sys_menu`（RuoYi框架菜单 + CMS菜单 + 消息中心菜单 + v7.7~v7.24菜单注册）
- `sys_role_menu`（admin 关联全部菜单）

### 已删除的业务测试数据

以下业务测试数据已从脚本中剔除，部署后系统为干净状态：
- `portal_book` / `portal_book_chapter` / `portal_book_quote` / `portal_book_recommend`（书籍测试数据）
- `portal_help_article`（帮助文章测试数据）
- `portal_shop_item`（商品测试数据）
- `portal_user`（门户测试用户，admin 已在 sys_user 中）
- `portal_writing_prompt`（写作提示测试数据）

### 兼容性说明

- 适配 **MySQL 8.0+**（utf8mb4 / utf8mb4_0900_ai_ci）
- 所有 `ALTER TABLE` 使用 `information_schema 校验 + PREPARE/EXECUTE` 动态 SQL 实现幂等，不使用 MariaDB 扩展语法（`ADD/DROP COLUMN IF EXISTS`）
- 所有 `CREATE TABLE` 使用 `IF NOT EXISTS`，可重复执行

### 部署后访问

| 入口 | 地址 | 账号 |
|---|---|---|
| 后台管理 | http://localhost:80 | admin / admin123 |
| 前台门户 | http://localhost:5173 | admin / 123456 |

### 历史版本说明

本脚本由原 47 个分散 SQL 脚本整合而来（v6.1~v7.26），整合时：
1. 合并全部建表语句（10/30/40/50-54）
2. 合并全部升级脚本中的表结构变更（95/96/97/109_5/110-117）
3. 合并全部菜单注册与重构脚本（100-108/109/114/116）
4. 保留基础数据，剔除业务测试数据
5. 修复 MySQL 8.0 兼容性问题（MariaDB 扩展语法）
