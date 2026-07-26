-- ============================================
-- 墨韵智库 - 下线共读活动 & 求职招聘模块
-- 文件编号：95
-- 说明：
--   本脚本用于已上线库的下线迁移，删除以下已下线功能的残留数据：
--     1. 读书空间 - 共读活动模块（4 张表 + 后台菜单 + 前台分类）
--     2. 面试空间 - 求职招聘模块（2 张表 + 后台菜单）
--   幂等设计，可重复执行；不存在的表/菜单不会报错。
--   简历功能（portal_user_resume / portal_interview_resume_template）保留，不在本脚本范围。
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 一、清理后台菜单（sys_menu + sys_role_menu）
-- =============================================

-- 1. 共读活动菜单（来源：77_book_club_admin_menu.sql，已删除）
--    菜单名：共读活动；权限前缀：cms:bookClub
SET @book_club_menu_ids = NULL;
SELECT GROUP_CONCAT(menu_id) INTO @book_club_menu_ids
FROM `sys_menu`
WHERE `menu_name` = '共读活动' OR `perms` LIKE 'cms:bookClub%';

-- 先删角色-菜单关联，再删菜单本身（含子菜单按钮权限）
DELETE FROM `sys_role_menu` WHERE FIND_IN_SET(`menu_id`, IFNULL(@book_club_menu_ids, 0));
DELETE FROM `sys_menu`       WHERE FIND_IN_SET(`menu_id`, IFNULL(@book_club_menu_ids, 0));

-- 2. 职位管理菜单（来源：67_job_init.sql，已删除）
--    菜单名：职位管理；权限前缀：cms:job
SET @job_menu_ids = NULL;
SELECT GROUP_CONCAT(menu_id) INTO @job_menu_ids
FROM `sys_menu`
WHERE `menu_name` = '职位管理' OR `perms` LIKE 'cms:job%';

DELETE FROM `sys_role_menu` WHERE FIND_IN_SET(`menu_id`, IFNULL(@job_menu_ids, 0));
DELETE FROM `sys_menu`       WHERE FIND_IN_SET(`menu_id`, IFNULL(@job_menu_ids, 0));

-- =============================================
-- 二、清理前台分类（portal_category）
-- =============================================

-- 共读计划（读书空间二级分类，slug = reading-club）
DELETE FROM `portal_category` WHERE `slug` = 'reading-club';

-- =============================================
-- 三、删除共读活动相关表（4 张）
--   来源：07_reading_interview_init.sql（已从合并版 DDL 中移除）
-- =============================================
DROP TABLE IF EXISTS `portal_book_club_record_like`;
DROP TABLE IF EXISTS `portal_book_club_record`;
DROP TABLE IF EXISTS `portal_book_club_participant`;
DROP TABLE IF EXISTS `portal_book_club_activity`;

-- =============================================
-- 四、删除求职招聘相关表（2 张）
--   来源：67_job_init.sql（已删除）
-- =============================================
DROP TABLE IF EXISTS `portal_job_application`;
DROP TABLE IF EXISTS `portal_job`;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 验证（可选，执行后人工核对）
-- =============================================
-- SELECT COUNT(*) AS leftover_book_club_menus FROM sys_menu WHERE menu_name = '共读活动' OR perms LIKE 'cms:bookClub%';
-- SELECT COUNT(*) AS leftover_job_menus      FROM sys_menu WHERE menu_name = '职位管理' OR perms LIKE 'cms:job%';
-- SELECT COUNT(*) AS leftover_reading_club   FROM portal_category WHERE slug = 'reading-club';
-- SHOW TABLES LIKE 'portal_book_club_%';
-- SHOW TABLES LIKE 'portal_job%';
