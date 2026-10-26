# 墨云项目 · 数据库初始化脚本说明（zyg_ 前缀交付）

> ⚠️ **状态说明（2026-10-22）**
> 本目录是 **2026-10-20 的现网快照**，用于交付/取证对照，**不是应用初始化口径**。
> 记账授权与审计表、`ledger_*` 字典、后台菜单（商品类目/渠道/字典/授权审计）等增量 **未同步到本快照**；
> 全新库请以 `moyun-server/src/main/resources/init-sql/` + `increment-sql/` 为准。
> 另：快照中的真实用户定时任务数据已移除。


- 数据基准：`moyun-db2` 实际库（MySQL 8.4.6 / utf8mb4 / utf8mb4_0900_ai_ci）
- 表规模：源库 205 张表 → 脚本内 200 张（剔除 5 张线上备份表）
- 数据规模：42 张表的系统预设数据随 DML 导入，其余表仅建结构
- 生成方式：`mysqldump --no-data` 取结构 + `--no-create-info` 取数据后按模块重排，注释 / 索引 / 字符集 / 排序规则全部保留

## 一、交付物与执行顺序

| 顺序 | 文件 | 内容 | 表数 |
| --- | --- | --- | --- |
| 1 | `zyg_ddl_01_sys_ai.sql` | DDL：系统基础 sys_* + 代码生成 gen_* + AI ai_* | 56 |
| 2 | `zyg_ddl_02_biz_job.sql` | DDL：门户 portal_* + 记账 ledger_* + 已下线归档 + 定时任务 sys_job/qrtz_*（外键，置尾） | 144 |
| 3 | `zyg_dml_01_sys_base.sql` | DML：平台/部门/岗位/角色/用户/字典/参数/敏感词 | 12 |
| 4 | `zyg_dml_02_sys_menu.sql` | DML：后台菜单 402 条 + 超级管理员授权 | 2 |
| 5 | `zyg_dml_03_ai.sql` | DML：供应商/模型/智能体/工具/词典/场景配置 | 9 |
| 6 | `zyg_dml_04_portal.sql` | DML：栏目分类/标签/帮助分类/面试分类/公司标签/岗位模板/解析配置/成长规则/友链/门户用户 | 12 |
| 7 | `zyg_dml_05_ledger.sql` | DML：功能开关/系统分类/商品目录/渠道 | 6 |
| 8 | `zyg_dml_06_job.sql` | DML：定时任务 12 个（sys_job） | 1 |
| 9 | `zyg_99_check.sql` | 自检：表数量 / 外键 / 关键行数核对 | — |
| 附 | `zyg_00_表清单与初始化口径.md` | 逐表清单：模块、行数、是否入 DDL/DML 及原因 | 205 |

## 二、执行方式

```bash
# 1) 建库（字符集必须 utf8mb4，排序规则 utf8mb4_0900_ai_ci）
mysql -uroot -p -e "CREATE DATABASE moyun_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

# 2) 按顺序导入（必须带 --default-character-set=utf8mb4，否则中文注释与数据会乱码）
cd db-init
for f in zyg_ddl_01_sys_ai.sql zyg_ddl_02_biz_job.sql \
         zyg_dml_01_sys_base.sql zyg_dml_02_sys_menu.sql zyg_dml_03_ai.sql \
         zyg_dml_04_portal.sql zyg_dml_05_ledger.sql zyg_dml_06_job.sql; do
  mysql -uroot -p --default-character-set=utf8mb4 moyun_db < "$f" || exit 1
done

# 3) 自检
mysql -uroot -p --default-character-set=utf8mb4 moyun_db < zyg_99_check.sql
```

> Windows 下建议 `mysql --default-character-set=utf8mb4 -uroot -p moyun_db -e "source D:/path/zyg_ddl_01_sys_ai.sql"`，或直接 `mysql ... < 文件`（cmd 重定向）。

## 三、DDL 设计要点

1. **幂等可重建**：每张表均为 `DROP TABLE IF EXISTS` + `CREATE TABLE`，同一库可反复执行，不会因"表已存在"报错；代价是**会清空同名表数据**，仅用于全新库或整库重建。
2. **模块优先级排序**：`sys_*`（1）→ `ai_*`（2）→ `portal_*`（3）→ `ledger_*`（4）→ 已下线归档（5）→ `sys_job` + `qrtz_*`（6）。
3. **定时任务置尾**：`qrtz_triggers` 外键依赖 `qrtz_job_details`，`qrtz_blob_triggers / qrtz_cron_triggers / qrtz_simple_triggers / qrtz_simprop_triggers` 外键依赖 `qrtz_triggers`，因此 Quartz 11 张表 + `sys_job` 统一放在第 2 个 DDL 文件末尾，并全程 `SET FOREIGN_KEY_CHECKS = 0`。
4. **注释完整**：表注释与每个字段注释全部保留（源库原始中文注释）。
5. **索引完整**：主键 / 唯一键 / 普通索引 / 索引前缀与 `USING BTREE` 全部保留；实测与源库逐表索引数量一致（仅备份表除外）。
6. **字符集对齐**：表级 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`，字段级保留原 `CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`（`ledger_ai_analysis_report` 为 `utf8mb4_bin`，按原样保留）。
7. **自增归一**：去掉线上 `AUTO_INCREMENT=n` 残留，重建后自增从 1 起，由 DML 插入的显式 id 自动抬升。

## 四、DML 取数口径

### 4.1 纳入初始化的内容

- **字典**：`sys_dict_type` 43 类 + `sys_dict_data` 119 条 —— 页面下拉框 / 多选框 / 状态标签的数据来源，全部保留（仅剔除测试字典 `ledger_test_tree`）。
- **菜单与授权**：`sys_menu` 402 条（目录 + 菜单 + 按钮权限）+ `sys_role_menu` 402 条（超级管理员全量授权）。
- **账号体系**：`sys_platform`、`sys_dept`、`sys_post`、`sys_role`、`sys_user`、`sys_user_role`、`sys_user_post`、`sys_role_dept`。
- **参数配置**：`sys_config` 19 条（含 AI 全局开关、RAG 检索开关、语音面试时长、简历 AI 建议开关等）。
- **AI 预设**：供应商 5 / 模型 4 / 智能体 2 / 工具 8 / 工具绑定 15 / 词典绑定 8 / 领域词典 17 / 场景配置 25。
- **分类与标签**：`portal_category` 49、`portal_tag` 38、`portal_help_category` 4、`portal_interview_category` 5、`portal_interview_company` 3、`ledger_category` 51（系统预设）、`ledger_product_category` 18。
- **场景 / 模板 / 规则**：`portal_job_template` 5、`portal_interview_resume_template` 1、`portal_interview_config` 1、`portal_resume_parse_config` 10、`portal_growth_rule` 31、`ledger_app_feature_config` 29、`ledger_schedule_task` 14、`ledger_sale_channel` 7、`ledger_source_channel` 5、`portal_friend_link` 3。
- **定时任务**：`sys_job` 12 个（Quartz 的 `qrtz_*` 由应用启动时按本表重建）。
- **门户用户**：`portal_user` 3 个现网账号。

### 4.2 明确不纳入的内容

| 类别 | 表 | 原因 |
| --- | --- | --- |
| 日志表 | `sys_logininfor` `sys_oper_log` `sys_job_log` `sys_config_log` `sys_sensitive_word_log` `ai_execute_log` `ai_token_usage_log` `ai_tool_call_log` `ledger_schedule_log` | 按需求排除日志 |
| 记录 / 会话表 | `ai_conversation` `ai_conversation_message` `ai_chat_history` `ai_query_history` `ai_reference_feedback` `ai_workflow_execution` `portal_ai_task` `portal_article_view` `portal_voice_interview*` `portal_message*` `portal_growth_log` | 运行时记录，迁移无意义 |
| 知识库内容表 | `ai_knowledge_base` `ai_knowledge_library` `ai_knowledge_library_config` `ai_knowledge_config` `ai_document_segment` `ai_document_image` | 按要求仅保留建表结构，不迁数据；迁移后重新上传文档即可 |
| 用户业务数据 | `ledger_transaction` `ledger_asset_account` `ledger_liability_account` `ledger_saving_*` `ledger_warehouse*` `ledger_shelf` `ledger_stock_flow` `ledger_product` `portal_user_resume` `portal_resume_job_*` `portal_follow` `portal_user_stats` 等 | 属于用户产生的业务数据，不是系统预设 |
| 已下线模块 | `vip_*` `pay_*` `*_tip_order` `portal_creator_settlement` | 2026-10 全站免费化后已无代码引用；DDL 保留结构，DML 不导入 |
| Quartz 运行态 | `qrtz_*` 11 张 | 由应用启动时按 `sys_job` 重建 |
| 线上备份表 | `_bak_portal_category` `ai_scene_config_bak_v1338` `ai_scene_config_bak_agent_id_v1338` `ledger_stock_bak_20261110` `sys_config_bak_v1338` | 备份表，DDL/DML 均不纳入 |

> 逐表口径见 `zyg_00_表清单与初始化口径.md`。

### 4.3 DML 幂等说明

所有 DML 使用 `REPLACE INTO`（按主键 / 唯一键覆盖）：

- 空库执行 = 纯插入；
- 重复执行 = 同主键行被脚本值覆盖，不会产生重复数据；
- 代价：后台手工改过的预置行（如字典标签、菜单名称、场景提示词）会被脚本重置，升级前请先备份这些表的差异。

## 五、部署后必须人工确认的项

| 项 | 位置 | 说明 |
| --- | --- | --- |
| 后台管理员密码 | `sys_user`（admin / ry） | 均为 RuoYi 默认 `admin123` 的 BCrypt 值，**首次登录必须立即修改** |
| AI 密钥 | `ai_model_config.api_key` | 值为 `ENC:` 前缀密文，用**原环境加密密钥**加密；换环境（密钥不同）后必须重新录入，否则 AI 调用鉴权失败 |
| 记账提醒域名 | `sys_config.ledger.schedule.appBaseUrl` | 已从旧内网地址改为 `http://localhost:5173`，按实际前端域名修改 |
| 对象存储 | `sys_file`（未导入） | 文件记录属运行时数据，需随 MinIO / 本地盘一起迁移，否则历史附件 404 |
| 知识库 | `ai_knowledge_*` / `ai_document_*` | 未导入数据，需在新环境重新上传文档并重建向量索引 |
| 定时任务 | `sys_job` → `qrtz_*` | 首次启动后由应用同步；若任务未生效，检查 `invoke_target` 与 `job_group` |
| 门户用户密码 | `portal_user` | `moyun_official` / `admin` 现网即为空密码（走验证码 / 第三方登录），如需密码登录请在后台重置 |
| 站点域名 | `sys_platform.domain` | 现为 `www.xulin.com` / `ledger.xulin.com` / `admin.xulin.com`，按新域名修改 |

## 六、验证结论（已实测）

在独立空库 `zyg_init_test` 上完整执行 8 个脚本，结论如下：

| 验证项 | 结果 |
| --- | --- |
| 8 个脚本依次执行 | 全部 0 错误 0 警告退出 |
| 建表数量 | 200 张（= 源库 205 − 备份表 5） |
| 字段总数 | 2750（源库 2822，差额 72 为 5 张备份表字段） |
| 索引 | 逐表索引数量与源库完全一致（备份表除外） |
| 外键 | 5 个 qrtz 外键全部创建成功 |
| 结构一致性 | 抽样 `sys_menu` / `ai_scene_config` / `ledger_category` / `qrtz_triggers` 的 `SHOW CREATE TABLE` 与源库逐字一致（仅自增起始值归一） |
| 数据一致性 | 关键表行数与源库一致；`sys_dict_data` 119（源 124 − 测试字典 5）、`ledger_category` 51（源 62 − 用户自定义 11） |
| 幂等性 | 8 个脚本连续执行 2 遍，第 2 遍同样 0 错误 |

## 七、回滚

本组脚本为重建型初始化脚本，本身不含回滚逻辑。执行前请务必：

```bash
mysqldump -uroot -p --default-character-set=utf8mb4 --single-transaction --routines --triggers moyun_db > backup_before_init_$(date +%Y%m%d%H%M).sql
```

如需回滚：`mysql -uroot -p moyun_db < backup_before_init_xxxx.sql`。
> **2026-10-26 变更**：`init-sql/` 与 `increment-sql/` 已删除，其内容合入本目录。
> 执行顺序：`zyg_ddl_01` → `zyg_ddl_02` → `zyg_dml_01` → `zyg_dml_02` → `zyg_dml_03` → `zyg_dml_04` → `zyg_dml_05` → `zyg_dml_06` → **`zyg_ddl_04_delta_20261026.sql`** → `zyg_99_check.sql`。
> 说明：delta 文件包含 2026-10-21/22 的授权审计、协议留痕、预设后台、分页索引等增量，可重复执行。
