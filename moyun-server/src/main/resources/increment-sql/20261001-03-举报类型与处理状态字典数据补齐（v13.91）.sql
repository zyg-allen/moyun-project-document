-- ============================================================
-- 增量脚本：20261001-03-举报类型与处理状态字典数据补齐（v13.91）
-- 背景：sys_dict_type 已有「举报类型 cms_report_type」「处理状态 cms_handle_status」两个类型行，
--       但 sys_dict_data **一行数据都没有**（清单 P2 #28 等）。后果：
--         · 后台字段管理里这两类下拉为空；
--         · 门户「我的举报」只能靠页面内硬编码文案；
--         · 已按字典驱动的「意见反馈」页实际一直走本地兜底分支（字典存在但空）。
-- 内容：DELETE + INSERT 全量补齐（举报类型 5 行 / 处理状态 4 行），可重复执行（幂等）。
-- 取值口径：与现有的前端硬编码口径、后台既有用法一致（spam/inappropriate/infringement/fraud/other；
--           pending/processing/resolved/rejected），**不新造枚举值**。
-- DDL/DML 初始化脚本已同步补齐（moyun-db-dml-init.sql），本脚本面向存量库。
-- 执行后需清理 Redis 字典缓存（否则后端继续命中旧空缓存）：
--   redis-cli DEL "sys_dict:cms_report_type" "sys_dict:cms_handle_status"
--   或调管理端「缓存监控 → 数据字典」清理接口（CacheController /system/dict/dictCache）
-- ============================================================

DELETE FROM sys_dict_data WHERE dict_type IN ('cms_report_type', 'cms_handle_status');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
 (1,'垃圾内容','spam','cms_report_type','','warning','Y','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：spam','0'),
 (2,'不当内容','inappropriate','cms_report_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：inappropriate','0'),
 (3,'侵权内容','infringement','cms_report_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：infringement','0'),
 (4,'欺诈行为','fraud','cms_report_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：fraud','0'),
 (5,'其他问题','other','cms_report_type','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：other','0'),
 (1,'待处理','pending','cms_handle_status','','warning','Y','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：pending','0'),
 (2,'处理中','processing','cms_handle_status','','primary','N','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：processing','0'),
 (3,'已解决','resolved','cms_handle_status','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：resolved','0'),
 (4,'已驳回','rejected','cms_handle_status','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：rejected','0');

-- 复核：应返回 cms_handle_status=4、cms_report_type=5
SELECT dict_type, COUNT(*) AS rows_cnt FROM sys_dict_data
 WHERE dict_type IN ('cms_report_type','cms_handle_status') GROUP BY dict_type;
