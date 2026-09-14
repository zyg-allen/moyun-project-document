-- =====================================================================
-- v11.78 支付管理重定位：新增「收入总览」菜单（全平台支付汇集入口）
-- 墨韵智库 · moyun
-- 执行库：moyun-db
-- 幂等：新菜单先 DELETE 再 INSERT；存量菜单仅 UPDATE 排序，不改原 INSERT
-- 说明：
--   1) 收入总览（5305）作为「支付管理」首屏，按 平台（记账App/墨韵门户）→ 渠道（App打赏/门户文章打赏/付费阅读/规划中）两级汇聚
--   2) 存量菜单（支付订单/分账流水/用户银行卡/支付配置）排序后移，页面与权限不动
--   3) 权限标识：cms:payRevenue:view（超管已授权）
-- =====================================================================

-- 1. 新菜单（页面 + 查询按钮）
DELETE FROM `moyun-db`.sys_menu WHERE menu_id IN (5305, 5317);

INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5305, '收入总览', 5300, 1, 'revenue', 'cms/pay/revenue/index', null, '', 1, 0, 'C', '0', '0', 'cms:payRevenue:view', 'chart', 'admin', NOW(), '', null, 'v11.78 全平台收入汇集：平台×渠道两级总览', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5317, '收入总览查询', 5305, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payRevenue:view', '#', 'admin', NOW(), '', null, '', '0');

-- 2. 存量菜单排序后移（增量 UPDATE，不触碰原 INSERT）
UPDATE `moyun-db`.sys_menu SET order_num = 2 WHERE menu_id = 5301 AND parent_id = 5300; -- 支付订单
UPDATE `moyun-db`.sys_menu SET order_num = 3 WHERE menu_id = 5302 AND parent_id = 5300; -- 分账流水
UPDATE `moyun-db`.sys_menu SET order_num = 4 WHERE menu_id = 5303 AND parent_id = 5300; -- 用户银行卡
UPDATE `moyun-db`.sys_menu SET order_num = 5 WHERE menu_id = 5304 AND parent_id = 5300; -- 支付配置

-- 3. 菜单授权给超级管理员（role_id=1）
DELETE FROM `moyun-db`.sys_role_menu WHERE role_id = 1 AND menu_id IN (5305, 5317);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id) VALUES (1, 5305), (1, 5317);

-- 4. 验证
SELECT menu_id, menu_name, parent_id, order_num, path, perms
FROM `moyun-db`.sys_menu
WHERE parent_id = 5300 AND menu_type = 'C' AND del_flag = '0'
ORDER BY order_num;
