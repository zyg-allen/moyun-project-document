-- 支付模块下线：清理菜单与角色授权（可重复执行）
DELETE rm FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id
 WHERE m.menu_id IN (109,110,111,112,113,114,115,116,606,607,608,609,610,611,612,613,614,677)
    OR m.path = 'pay' OR m.component LIKE 'cms/pay/%';
DELETE FROM sys_menu
 WHERE menu_id IN (109,110,111,112,113,114,115,116,606,607,608,609,610,611,612,613,614,677)
    OR path = 'pay' OR component LIKE 'cms/pay/%';
