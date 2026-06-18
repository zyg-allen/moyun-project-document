-- =============================================
-- V24: 精选笔记管理菜单初始化（note_adopted 成长事件后台管理）
-- 版本：2.8
-- 日期：2026-06-18
-- 说明：为面试空间的"精选笔记管理"功能添加后台管理菜单和权限
--       后台管理员可在此页面查看用户提交的笔记，并采纳/取消采纳为精选笔记
--       采纳时会自动触发 note_adopted 成长事件（+50 成长值）
-- 前端页面：moyun-admin-vue/src/views/cms/interview/submission/index.vue
-- 后端接口：CmsInterviewController（/cms/interview/submission/*）
-- 依赖：
--   - 18_interview_menu_init.sql（面试空间一级菜单）
--   - 21_submission_featured.sql（is_featured 字段）
--   - 19_growth_system_init.sql（note_adopted 成长规则）
-- =============================================

-- 0. 清理旧的精选笔记菜单数据（如果存在，避免重复执行时报错）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '精选笔记'
) OR `menu_id` IN (
    SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = '精选笔记'
);
DELETE FROM `sys_menu` WHERE `menu_name` = '精选笔记' OR `parent_id` IN (
    SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = '精选笔记'
);

-- 查询面试空间一级菜单ID（由 18_interview_menu_init.sql 创建）
SET @interview_menu_id = (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '面试空间' AND `parent_id` = 0 LIMIT 1
);

-- =============================================
-- 插入"精选笔记"二级菜单（挂在面试空间下，排序在"公司管理"之后）
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '精选笔记', @interview_menu_id, 7, 'submission', 'cms/interview/submission/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'documentation', 'admin', NOW(), '', NULL, '精选笔记管理菜单（采纳/取消采纳用户提交的笔记）'
);

SET @submission_menu_id = LAST_INSERT_ID();

-- =============================================
-- 精选笔记管理按钮权限
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('笔记查询', @submission_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''),
('采纳精选', @submission_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, '采纳为精选笔记（触发 note_adopted 成长事件）'),
('取消精选', @submission_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, '取消精选笔记');

-- =============================================
-- 为管理员角色分配精选笔记菜单权限
-- =============================================

SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` = '精选笔记' OR `parent_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '精选笔记'
);

-- =============================================
-- 脚本执行完成
-- =============================================

SELECT '精选笔记管理菜单初始化脚本执行完成！' AS message;
