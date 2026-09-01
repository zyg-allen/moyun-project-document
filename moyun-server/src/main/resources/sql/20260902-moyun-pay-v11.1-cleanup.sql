-- =====================================================================
-- 墨韵·智库 V11.1 支付中心清理脚本（2026-09-02）
-- 目的：移除旧通用订单模块（portal_order）的菜单入口，统一收敛到
--       V11.0 新支付中心（支付订单/分账流水/银行卡/支付配置）
-- 说明：代码层已删除旧 Order 全链（Portal/Cms Controller+Service+Mapper+
--       实体+XML+Admin 页面），本脚本清理对应 sys_menu 残留。
-- 历史全量 DDL/DML 文件不改写，以本增量脚本为准。
-- =====================================================================

USE `moyun-db`;

-- 1. 删除旧交易管理菜单（5148，v9.5 引入，当前隐藏，功能已被支付中心取代）
DELETE FROM sys_menu WHERE menu_id = 5148;

-- 2. 删除旧付费订单按钮权限（5132/5133，挂在 5148 下）
DELETE FROM sys_menu WHERE menu_id IN (5132, 5133);

-- 3. 清理角色-菜单关联（避免悬挂外键）
DELETE FROM sys_role_menu WHERE menu_id IN (5132, 5133, 5148);

-- 验证：V11.0 支付中心菜单（5301-5330）不受影响
SELECT menu_id, menu_name, path FROM sys_menu
WHERE menu_id BETWEEN 5301 AND 5330 ORDER BY menu_id;
