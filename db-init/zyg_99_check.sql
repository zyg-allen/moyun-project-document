-- ==============================================================================
-- 墨云项目 · 初始化结果自检脚本（zyg_99_check.sql）
-- 用途：执行完 zyg_ddl_01/02 与 zyg_dml_01~06 后运行，逐项核对表数量与关键数据量
-- 期望：所有 check_result 均为 OK
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 <库名> < zyg_99_check.sql
-- ==============================================================================
SET NAMES utf8mb4;

SELECT 'DDL 表数量(=200)' AS check_item,
       COUNT(*) AS actual,
       IF(COUNT(*) = 200, 'OK', 'FAIL') AS check_result
  FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE();

SELECT '外键数量(=5，qrtz 任务链)' AS check_item, COUNT(*) AS actual,
       IF(COUNT(*) = 5, 'OK', 'FAIL') AS check_result
  FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE();

-- 关键预设数据量核对
SELECT 'sys_menu 行数(=402)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 402, 'OK', 'FAIL') AS check_result FROM `sys_menu`;
SELECT 'sys_role_menu 行数(=402)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 402, 'OK', 'FAIL') AS check_result FROM `sys_role_menu`;
SELECT 'sys_dict_type 行数(=43)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 43, 'OK', 'FAIL') AS check_result FROM `sys_dict_type`;
SELECT 'sys_dict_data 行数(=119)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 119, 'OK', 'FAIL') AS check_result FROM `sys_dict_data`;
SELECT 'sys_config 行数(=19)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 19, 'OK', 'FAIL') AS check_result FROM `sys_config`;
SELECT 'sys_user 行数(=2)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 2, 'OK', 'FAIL') AS check_result FROM `sys_user`;
SELECT 'sys_role 行数(=2)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 2, 'OK', 'FAIL') AS check_result FROM `sys_role`;
SELECT 'sys_job 行数(=12)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 12, 'OK', 'FAIL') AS check_result FROM `sys_job`;
SELECT 'ai_scene_config 行数(=25)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 25, 'OK', 'FAIL') AS check_result FROM `ai_scene_config`;
SELECT 'ai_agent 行数(=2)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 2, 'OK', 'FAIL') AS check_result FROM `ai_agent`;
SELECT 'ai_model_config 行数(=4)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 4, 'OK', 'FAIL') AS check_result FROM `ai_model_config`;
SELECT 'ai_provider 行数(=5)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 5, 'OK', 'FAIL') AS check_result FROM `ai_provider`;
SELECT 'portal_category 行数(=49)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 49, 'OK', 'FAIL') AS check_result FROM `portal_category`;
SELECT 'portal_tag 行数(=38)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 38, 'OK', 'FAIL') AS check_result FROM `portal_tag`;
SELECT 'portal_user 行数(=3)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 3, 'OK', 'FAIL') AS check_result FROM `portal_user`;
SELECT 'ledger_category 行数(=51)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 51, 'OK', 'FAIL') AS check_result FROM `ledger_category`;
SELECT 'ledger_app_feature_config 行数(=29)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 29, 'OK', 'FAIL') AS check_result FROM `ledger_app_feature_config`;
SELECT 'ledger_schedule_task 行数(=14)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) = 14, 'OK', 'FAIL') AS check_result FROM `ledger_schedule_task`;

-- 空表清单（知识库/日志/运行时表按设计应为空）
SELECT '空表数量(<=160)' AS check_item, COUNT(*) AS actual, IF(COUNT(*) <= 160, 'OK', 'FAIL') AS check_result
  FROM information_schema.TABLES t WHERE t.TABLE_SCHEMA = DATABASE() AND t.TABLE_ROWS = 0;