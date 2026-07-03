-- =====================================================
-- VIP 会员中心 DDL/菜单脚本（阶段四 任务 4.6）
-- 说明：复用现有 portal_vip_package、portal_order 表及 PortalUser.vip_expire_at 字段，
--       本脚本仅补充后台 VIP 套餐管理菜单（如已存在则跳过）。
-- @author moyun
-- =====================================================

-- 清理旧菜单（保证可重复执行）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = 'VIP套餐管理'
);
DELETE FROM `sys_menu` WHERE `menu_name` = 'VIP套餐管理';

-- 一级目录：商业化（若已存在则复用，避免重复插入）
SET @biz_menu_id = (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = '商业化' LIMIT 1);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '商业化', 0, 15, 'biz', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'money', 'admin', NOW(), '', NULL, '商业化目录'
FROM DUAL WHERE @biz_menu_id IS NULL;
SET @biz_menu_id = (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = '商业化' LIMIT 1);

-- 二级菜单：VIP套餐管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    'VIP套餐管理', @biz_menu_id, 1, 'vipPackage', 'portal/vipPackage/index', NULL, 1, 0, 'C', '0', '0', 'portal:vipPackage:list', 'vip', 'admin', NOW(), '', NULL, 'VIP套餐管理菜单'
);
SET @vip_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
('套餐查询', @vip_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:vipPackage:query', '#', 'admin', NOW(), '', NULL, ''),
('套餐新增', @vip_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:vipPackage:add', '#', 'admin', NOW(), '', NULL, ''),
('套餐修改', @vip_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:vipPackage:edit', '#', 'admin', NOW(), '', NULL, ''),
('套餐删除', @vip_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:vipPackage:remove', '#', 'admin', NOW(), '', NULL, '');

-- 为管理员角色分配菜单权限
SET @admin_role_id = 1;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN ('商业化', 'VIP套餐管理')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = 'VIP套餐管理');

SELECT 'VIP会员中心菜单初始化脚本执行完成！' AS message;
