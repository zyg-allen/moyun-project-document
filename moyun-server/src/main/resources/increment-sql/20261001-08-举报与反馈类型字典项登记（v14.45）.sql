-- ============================================================
-- 增量脚本：20261001-08-举报与反馈类型字典项补齐到运行库（v14.45）
-- 背景（清单 P2）：后台字典**类型**已登记，但**运行库中没有任何字典项**，
--   因此前端 useDictData(['cms_report_type','cms_feedback_type']) 永远返回空数组，
--   举报/反馈类型 100% 走页面内本地兜底常量 —— "字典驱动"名存实亡，后台也无法维护选项。
--
-- ★ 定位说明：经核对，初始化脚本 init-sql/moyun-db-dml-init.sql **本就包含**这 9 条字典项
--   （L655-659 / L666-669），缺的是**存量运行库**（初始化早于这些种子行，或未重跑种子）。
--   故本脚本的作用是"把种子补齐到运行库"，取值与 DML 保持**逐字一致**，避免新装库与存量库漂移。
-- 幂等性：先按 (dict_type, dict_value) 补齐缺失项，再用 UPDATE 对齐取值。
-- 执行后清理字典缓存：redis-cli DEL "sys_dict:cms_report_type" "sys_dict:cms_feedback_type"
-- ============================================================

-- ① 补齐缺失项（与 DML 取值一致）
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 1,'垃圾内容','spam','cms_report_type','','warning','Y','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：spam','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_report_type' AND dict_value='spam');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 2,'不当内容','inappropriate','cms_report_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：inappropriate','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_report_type' AND dict_value='inappropriate');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 3,'侵权内容','infringement','cms_report_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：infringement','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_report_type' AND dict_value='infringement');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 4,'欺诈行为','fraud','cms_report_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：fraud','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_report_type' AND dict_value='fraud');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 5,'其他问题','other','cms_report_type','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：other','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='cms_report_type' AND dict_value='other');
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

-- ② 对齐取值（把此前用其它取值补进去的行纠正为与 DML 一致）
UPDATE sys_dict_data SET dict_label='垃圾内容', list_class='warning', is_default='Y', remark='举报类型：spam'
 WHERE dict_type='cms_report_type' AND dict_value='spam';
UPDATE sys_dict_data SET dict_label='不当内容', list_class='danger', remark='举报类型：inappropriate'
 WHERE dict_type='cms_report_type' AND dict_value='inappropriate';
UPDATE sys_dict_data SET dict_label='侵权内容', list_class='danger', remark='举报类型：infringement'
 WHERE dict_type='cms_report_type' AND dict_value='infringement';
UPDATE sys_dict_data SET dict_label='欺诈行为', list_class='danger', remark='举报类型：fraud'
 WHERE dict_type='cms_report_type' AND dict_value='fraud';
UPDATE sys_dict_data SET dict_label='其他问题', list_class='info', remark='举报类型：other'
 WHERE dict_type='cms_report_type' AND dict_value='other';
UPDATE sys_dict_data SET dict_label='功能建议', list_class='primary', remark='反馈类型：suggestion'
 WHERE dict_type='cms_feedback_type' AND dict_value='suggestion';
UPDATE sys_dict_data SET dict_label='Bug反馈', list_class='danger', remark='反馈类型：bug'
 WHERE dict_type='cms_feedback_type' AND dict_value='bug';
UPDATE sys_dict_data SET dict_label='体验问题', list_class='warning', remark='反馈类型：experience'
 WHERE dict_type='cms_feedback_type' AND dict_value='experience';
UPDATE sys_dict_data SET dict_label='其他', list_class='info', remark='反馈类型：other'
 WHERE dict_type='cms_feedback_type' AND dict_value='other';

-- 复核：举报类型 5 项、反馈类型 4 项，且取值与 DML 一致
SELECT dict_type, dict_value, dict_label, list_class, is_default FROM sys_dict_data
 WHERE dict_type IN ('cms_report_type','cms_feedback_type') ORDER BY dict_type, dict_sort;
