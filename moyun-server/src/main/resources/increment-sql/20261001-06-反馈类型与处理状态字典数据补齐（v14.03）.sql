-- ============================================================
-- 增量脚本：20261001-06-反馈类型与处理状态字典数据补齐（v14.03）
-- 背景：与 v13.91 的举报类型同源问题 ——
--   `cms_handle_status`（处理状态）与 `cms_feedback_type`（反馈类型）
--   在 sys_dict_type 中有类型行，但 sys_dict_data **没有任何数据行**。
--   后果：后台「字典管理」里这两类下拉为空；门户「我的反馈」只能把 4 个处理状态与
--         4 个反馈类型（含颜色）写死在组件里，与后台口径必然漂移。
--
-- 内容：
--   · cms_handle_status  ：此前 v13.91 已补齐 4 行（pending/processing/resolved/rejected），
--                          本脚本**判重后幂等补齐**，不覆盖既有行；
--   · cms_feedback_type  ：补齐 4 行（suggestion/bug/experience/other）。
-- 取值口径：沿用门户既有硬编码口径（MyFeedbackPage / ReportFeedback 两处一致），**不新造枚举值**。
-- 幂等性：INSERT ... SELECT ... WHERE NOT EXISTS（按 dict_type + dict_value 判重）。
-- DDL/DML 初始化脚本已同步补齐（moyun-db-dml-init.sql）。
-- 执行后清理 Redis 字典缓存：
--   redis-cli DEL "sys_dict:cms_feedback_type" "sys_dict:cms_handle_status"
--   或调管理端「缓存监控 → 数据字典」清理接口
-- ============================================================

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 1,'功能建议','suggestion','cms_feedback_type','','primary','N','0','admin','2026-10-01 00:00:00','',NULL,'反馈类型：suggestion','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_feedback_type' AND dict_value='suggestion');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 2,'Bug反馈','bug','cms_feedback_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'反馈类型：bug','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_feedback_type' AND dict_value='bug');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 3,'体验问题','experience','cms_feedback_type','','warning','N','0','admin','2026-10-01 00:00:00','',NULL,'反馈类型：experience','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_feedback_type' AND dict_value='experience');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 4,'其他','other','cms_feedback_type','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'反馈类型：other','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_feedback_type' AND dict_value='other');

-- cms_handle_status 判重补齐（v13.91 已插入的行不会重复）
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 1,'待处理','pending','cms_handle_status','','warning','N','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：pending','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_handle_status' AND dict_value='pending');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 2,'处理中','processing','cms_handle_status','','primary','N','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：processing','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_handle_status' AND dict_value='processing');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 3,'已解决','resolved','cms_handle_status','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：resolved','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_handle_status' AND dict_value='resolved');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 4,'已驳回','rejected','cms_handle_status','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：rejected','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_handle_status' AND dict_value='rejected');

-- 复核：cms_feedback_type 应为 4、cms_handle_status 应为 4
SELECT dict_type, COUNT(*) AS rows_cnt FROM sys_dict_data
 WHERE dict_type IN ('cms_feedback_type','cms_handle_status') GROUP BY dict_type ORDER BY dict_type;
