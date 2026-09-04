-- =============================================================
-- 记账模块 AI 财务分析 V11.21
-- 1. portal_user 新增身份标签字段（AI 分析画像维度，与门户共用同一张表）
-- 2. 身份标签字典（后台字典管理可维护，前台分析页可选）
-- 依赖：20260903-01-moyun-ledger.sql
-- =============================================================

-- ----------------------------
-- 1. portal_user 增量字段（不动原建表语句，追加 ALTER）
-- ----------------------------
ALTER TABLE `portal_user`
  ADD COLUMN `identity_tag` VARCHAR(32) NULL DEFAULT NULL COMMENT '身份标签（字典 ledger_identity_tag，AI 财务分析画像维度）' AFTER `position`;
--
select * from portal_user;
select * from sys_dict_data where dict_code = 200;
-- ----------------------------
-- 2. 身份标签字典类型
-- ----------------------------
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag)
VALUES (200, '记账-身份标签', 'ledger_identity_tag', '0', 'admin', NOW(), '', NULL, 'AI 财务分析用户画像身份标签', '0');

-- ----------------------------
-- 3. 身份标签字典数据
-- ----------------------------
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES
(200, 1, '学生',       'student',        'ledger_identity_tag', '', 'default', 'N', '0', 'admin', NOW(), '', NULL, '学生群体', '0'),
(201, 2, '上班族',     'office_worker',  'ledger_identity_tag', '', 'default', 'N', '0', 'admin', NOW(), '', NULL, '固定职业收入', '0'),
(202, 3, '自由职业',   'freelancer',     'ledger_identity_tag', '', 'default', 'N', '0', 'admin', NOW(), '', NULL, '非固定收入', '0'),
(203, 4, '个体经营者', 'business_owner', 'ledger_identity_tag', '', 'default', 'N', '0', 'admin', NOW(), '', NULL, '经营性收入', '0'),
(204, 5, '退休',       'retired',        'ledger_identity_tag', '', 'default', 'N', '0', 'admin', NOW(), '', NULL, '退休群体', '0'),
(205, 6, '其他',       'other',          'ledger_identity_tag', '', 'default', 'N', '0', 'admin', NOW(), '', NULL, '其他身份', '0');
