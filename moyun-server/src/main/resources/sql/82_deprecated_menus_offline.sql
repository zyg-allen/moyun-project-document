-- =====================================================
-- 已下线菜单隐藏脚本（一次性执行，用于已部署的数据库）
-- 作用：将前台已下线功能对应的后台菜单 visible 改为 '1'（隐藏）
-- 下线范围：
--   1. 打赏管理（前台 UserPage 账号 Tab 移除入口）
--   2. 付费订单 + 财务一级目录（前台消费记录入口移除）
--   3. 创作者结算 + 分成结算（依赖打赏/付费订单，一并下线）
--   4. PK 对战（前台 /learn/pk 路由 + PkPage.vue 删除）
-- 注：visible='1' 为隐藏，status='0' 为正常（保留权限分配），
--     后端 Controller/Service/Mapper/Entity 全部保留不动，便于未来恢复。
-- 幂等设计：可重复执行
-- =====================================================

-- 1. 打赏管理 + 打赏查询按钮
UPDATE `sys_menu` SET `visible` = '1', `remark` = '【已下线】前台打赏功能移除，菜单隐藏保留以兼容历史数据'
WHERE `perms` IN ('portal:tip:list', 'portal:tip:query');

-- 2. 财务一级目录 + 付费订单 + 订单查询按钮
UPDATE `sys_menu` SET `visible` = '1', `remark` = '【已下线】财务目录，前台消费记录入口已移除'
WHERE `menu_name` = '财务' AND `parent_id` = 0;
UPDATE `sys_menu` SET `visible` = '1', `remark` = '【已下线】前台消费记录入口移除，菜单隐藏保留以兼容历史数据'
WHERE `perms` IN ('portal:order:list', 'portal:order:query');

-- 3. 创作者结算一级目录 + 分成结算二级菜单
UPDATE `sys_menu` SET `visible` = '1', `remark` = '【已下线】前台打赏/消费记录移除，结算依赖的收入来源不存在'
WHERE `menu_name` IN ('创作者结算', '分成结算')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = '分成结算');

-- 4. PK 对战（前台路由删除，后台本来就没有菜单，此条作为兜底）
UPDATE `sys_menu` SET `visible` = '1', `remark` = '【已下线】前台 PK 对战功能移除'
WHERE `perms` LIKE 'portal:pk%' OR `menu_name` LIKE '%PK%对战%';

-- 校验结果
SELECT '已下线菜单隐藏脚本执行完成！影响行数：' AS message, ROW_COUNT() AS affected;
