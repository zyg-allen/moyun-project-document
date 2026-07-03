-- =====================================================
-- 学习中心聚合页 DDL 脚本（任务 3.1）
-- 学习中心聚合页本身不依赖独立业务表，仅做聚合查询：
--   - 学习计划来自 portal_study_plan / portal_study_plan_log（见 65_study_plan_init.sql）
--   - 错题本来自 portal_wrong_question（见 66_wrong_question_init.sql）
--   - 答题统计来自 portal_interview_submission（已有表）
-- 本脚本仅初始化后台管理菜单（学习计划只读、错题本只读）
-- 幂等设计：先清理再插入
-- 执行顺序：本脚本在 03_portal_init.sql 之后执行
-- =====================================================

-- =====================================================
-- 后台管理菜单初始化：学习者中心
-- 路径：/portal/studyPlan、/portal/wrongQuestion
-- =====================================================

-- 0. 清理旧的菜单数据（避免重复执行时报错）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN ('学习者中心', '学习计划', '错题本')
);
DELETE FROM `sys_menu` WHERE `menu_name` IN ('学习者中心', '学习计划', '错题本');

-- 1. 插入一级菜单：学习者中心（一级目录）
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '学习者中心', 0, 15, 'portal', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'education', 'admin', NOW(), '', NULL, '学习者中心目录'
);

SET @learn_menu_id = LAST_INSERT_ID();

-- 2. 二级菜单：学习计划（只读查看）
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '学习计划', @learn_menu_id, 1, 'studyPlan', 'portal/studyPlan/index', NULL, 1, 0, 'C', '0', '0', 'portal:studyPlan:list', 'edit', 'admin', NOW(), '', NULL, '学习计划只读查看菜单'
);

SET @study_plan_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('计划查询', @study_plan_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:studyPlan:query', '#', 'admin', NOW(), '', NULL, '');

-- 3. 二级菜单：错题本（只读查看）
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '错题本', @learn_menu_id, 2, 'wrongQuestion', 'portal/wrongQuestion/index', NULL, 1, 0, 'C', '0', '0', 'portal:wrongQuestion:list', 'edit', 'admin', NOW(), '', NULL, '错题本只读查看菜单'
);

SET @wrong_question_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('错题查询', @wrong_question_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:wrongQuestion:query', '#', 'admin', NOW(), '', NULL, '');

-- 4. 为管理员角色分配菜单权限
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN ('学习者中心', '学习计划', '错题本')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN ('学习者中心', '学习计划', '错题本'));

SELECT '学习中心聚合页初始化脚本执行完成！' AS message;
