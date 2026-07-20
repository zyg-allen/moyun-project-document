# 墨韵智库 SQL 脚本整理目录

本目录是对原 `sql/` 目录 90 个脚本的重新整理合并版，按业务域归类，每张表的所有 ALTER 合并到最终 CREATE TABLE IF NOT EXISTS，可重复执行。

## 目录结构

```
sql-organized/
├── 01_system/              系统基础表（RuoYi 框架）
│   └── 01_system_tables.sql     24 张表：sys_* + gen_* + sys_file + sys_notification
├── 02_workflow/            工作流引擎表（Flowable）
│   └── 01_workflow_tables.sql   39 张表：act_*
├── 03_portal_base/         门户基础表
│   └── 01_portal_base_tables.sql 20 张表：user/article/category/tag/comment 等
├── 04_portal_reading/     读书空间
│   └── 01_reading_tables.sql     18 张表：book/chapter/book_list/quote/bookshelf/club
├── 05_portal_interview/   面试空间
│   └── 01_interview_tables.sql   19 张表：question/experience/resume/mock_interview/job
├── 06_portal_growth/      成长体系
│   └── 01_growth_tables.sql      11 张表：growth/badge/task/shop
├── 07_portal_social/      社交模块
│   └── 01_social_tables.sql      12 张表：feed/message/column/circle/topic
├── 08_portal_commerce/    商业化模块
│   └── 01_commerce_tables.sql    8 张表：tip/contest/certification/settlement/ad
├── 09_portal_learning/    学习工具
│   └── 01_learning_tables.sql    5 张表：study_plan/wrong_question/code_run/pk
├── 10_menus/              菜单初始化
│   └── 01_all_menus.sql         全部 sys_menu INSERT（幂等）+ UPDATE 修复
├── 11_data/               数据初始化
│   ├── 01_seed_data.sql         业务种子数据：分类/标签/成长规则/成就/prompt
│   └── 02_test_data.sql         测试数据（DEV ONLY）
└── README.md              本文件
```

## 执行顺序

### 全新部署（按顺序执行）

```bash
# 1. 建表（顺序不可变）
mysql -u root -p moyun < 01_system/01_system_tables.sql
mysql -u root -p moyun < 02_workflow/01_workflow_tables.sql
mysql -u root -p moyun < 03_portal_base/01_portal_base_tables.sql
mysql -u root -p moyun < 04_portal_reading/01_reading_tables.sql
mysql -u root -p moyun < 05_portal_interview/01_interview_tables.sql
mysql -u root -p moyun < 06_portal_growth/01_growth_tables.sql
mysql -u root -p moyun < 07_portal_social/01_social_tables.sql
mysql -u root -p moyun < 08_portal_commerce/01_commerce_tables.sql
mysql -u root -p moyun < 09_portal_learning/01_learning_tables.sql

# 2. 菜单
mysql -u root -p moyun < 10_menus/01_all_menus.sql

# 3. 业务种子数据（生产环境也要执行）
mysql -u root -p moyun < 11_data/01_seed_data.sql

# 4. 测试数据（仅开发环境，生产严禁执行）
mysql -u root -p moyun < 11_data/02_test_data.sql
```

### 增量更新（已有旧库）

所有建表脚本均使用 `CREATE TABLE IF NOT EXISTS`，可安全重复执行。已存在的表不会被覆盖。

如需同步新字段，仍需使用原 `sql/` 目录中的 ALTER 脚本（按编号执行）。

## 与原 sql/ 目录的关系

| 原 sql/ 目录 | 新 sql-organized/ 目录 |
|---|---|
| 90 个编号脚本（01-91） | 12 个分类文件 |
| 每张表分散在建表+多个 ALTER 脚本 | 每张表合并为最终状态的单个 CREATE |
| 需按编号顺序执行 | 按目录顺序执行 |
| ALTER 脚本无幂等保护（部分） | 全部 IF NOT EXISTS，可重复执行 |
| 测试数据混在业务脚本中 | 测试数据单独隔离 |

## 统计

| 类别 | 表数量 | 脚本行数 |
|---|---|---|
| system | 24 | 543 |
| workflow | 39 | 1379 |
| portal_base | 20 | 511 |
| reading | 18 | 465 |
| interview | 19 | 492 |
| growth | 11 | 263 |
| social | 13 | 248 |
| commerce | 13 | 298 |
| learning | 8 | 122 |
| menus | — | 1079 |
| seed data | — | 255 |
| test data | — | 1325 |
| **合计** | **165 表** | **6980 行** |

## 注意事项

1. **原 sql/ 目录保留**：不删除，作为历史记录和 ALTER 增量参考
2. **工作流表**：Flowable 的 act_* 表通常由引擎自动建表，此处的建表脚本作为备用
3. **测试数据**：`11_data/02_test_data.sql` 含 TRUNCATE 语句，会清空业务数据，**生产环境绝对不能执行**
4. **字段注释**：所有 COMMENT 保留原始定义
5. **幂等性**：建表脚本 IF NOT EXISTS，菜单 INSERT IGNORE，种子数据 INSERT IGNORE
