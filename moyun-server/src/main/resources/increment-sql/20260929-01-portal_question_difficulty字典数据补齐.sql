-- ============================================================
-- 增量脚本：20260929-01-portal_question_difficulty 字典数据补齐（v13.34）
-- 背景：sys_dict_type 有「题目难度」类型行，但 sys_dict_data 数据行从未初始化，
--       导致管理端题库列表难度列（dict-tag）与查询/编辑表单难度下拉为空。
--       DDL/DML 初始化脚本已同步补齐（moyun-db-dml-init.sql），本脚本面向存量库。
-- 内容：DELETE + INSERT 全量补齐 3 行（easy/medium/hard），可重复执行（幂等）。
-- 执行后需清理 Redis 字典缓存（否则后端继续命中旧空缓存）：
--   redis-cli DEL "sys_dict:portal_question_difficulty"
--   或调管理端「缓存监控 → 数据字典」清理接口（CacheController /system/dict/dictCache）
-- ============================================================

DELETE FROM sys_dict_data WHERE dict_type = 'portal_question_difficulty';

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
 (1,'简单','easy','portal_question_difficulty','','success','Y','0','admin','2026-09-29 00:00:00','',NULL,'题库难度：easy','0'),
 (2,'中等','medium','portal_question_difficulty','','warning','N','0','admin','2026-09-29 00:00:00','',NULL,'题库难度：medium','0'),
 (3,'困难','hard','portal_question_difficulty','','danger','N','0','admin','2026-09-29 00:00:00','',NULL,'题库难度：hard','0');

-- 复核：应返回 3 行
SELECT dict_label, dict_value, list_class FROM sys_dict_data WHERE dict_type = 'portal_question_difficulty' ORDER BY dict_sort;
