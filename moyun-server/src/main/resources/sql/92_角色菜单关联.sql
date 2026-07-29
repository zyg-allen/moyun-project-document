-- 来源：all-db-ddl.sql 行4999-5001
-- 用途：sys_role_menu 种子数据（INSERT）
-- v6.1 修复：role_id=1 改为动态子查询（匹配 menu_id >= 2000 的 CMS 菜单），不再依赖硬编码 ID
--          原硬编码 2000-2228 与 91 自增 ID 不匹配导致前端空白（parentNode null）

-- =============================================================================
-- 一、超级管理员（role_id=1）：关联全部 CMS 菜单（由 91 创建，ID >= 2000）
-- =============================================================================
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, menu_id, 'admin', NOW()
FROM sys_menu
WHERE menu_id >= 2000
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
  );

-- =============================================================================
-- 二、普通角色（role_id=2）：关联 RuoYi 基础菜单（menu_id 1-1060，由 90 创建，ID 固定）
-- =============================================================================
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(2,1,'admin',NOW(),'',NULL,NULL),
(2,2,'admin',NOW(),'',NULL,NULL),
(2,3,'admin',NOW(),'',NULL,NULL),
(2,4,'admin',NOW(),'',NULL,NULL),
(2,100,'admin',NOW(),'',NULL,NULL),
(2,101,'admin',NOW(),'',NULL,NULL),
(2,102,'admin',NOW(),'',NULL,NULL),
(2,103,'admin',NOW(),'',NULL,NULL),
(2,104,'admin',NOW(),'',NULL,NULL),
(2,105,'admin',NOW(),'',NULL,NULL),
(2,106,'admin',NOW(),'',NULL,NULL),
(2,108,'admin',NOW(),'',NULL,NULL),
(2,109,'admin',NOW(),'',NULL,NULL),
(2,110,'admin',NOW(),'',NULL,NULL),
(2,111,'admin',NOW(),'',NULL,NULL),
(2,112,'admin',NOW(),'',NULL,NULL),
(2,113,'admin',NOW(),'',NULL,NULL),
(2,114,'admin',NOW(),'',NULL,NULL),
(2,115,'admin',NOW(),'',NULL,NULL),
(2,116,'admin',NOW(),'',NULL,NULL),
(2,117,'admin',NOW(),'',NULL,NULL),
(2,118,'admin',NOW(),'',NULL,NULL),
(2,119,'admin',NOW(),'',NULL,NULL),
(2,500,'admin',NOW(),'',NULL,NULL),
(2,501,'admin',NOW(),'',NULL,NULL),
(2,1000,'admin',NOW(),'',NULL,NULL),
(2,1001,'admin',NOW(),'',NULL,NULL),
(2,1002,'admin',NOW(),'',NULL,NULL),
(2,1003,'admin',NOW(),'',NULL,NULL),
(2,1004,'admin',NOW(),'',NULL,NULL),
(2,1005,'admin',NOW(),'',NULL,NULL),
(2,1006,'admin',NOW(),'',NULL,NULL),
(2,1007,'admin',NOW(),'',NULL,NULL),
(2,1008,'admin',NOW(),'',NULL,NULL),
(2,1009,'admin',NOW(),'',NULL,NULL),
(2,1010,'admin',NOW(),'',NULL,NULL),
(2,1011,'admin',NOW(),'',NULL,NULL),
(2,1012,'admin',NOW(),'',NULL,NULL),
(2,1013,'admin',NOW(),'',NULL,NULL),
(2,1014,'admin',NOW(),'',NULL,NULL),
(2,1015,'admin',NOW(),'',NULL,NULL),
(2,1016,'admin',NOW(),'',NULL,NULL),
(2,1017,'admin',NOW(),'',NULL,NULL),
(2,1018,'admin',NOW(),'',NULL,NULL),
(2,1019,'admin',NOW(),'',NULL,NULL),
(2,1020,'admin',NOW(),'',NULL,NULL),
(2,1021,'admin',NOW(),'',NULL,NULL),
(2,1022,'admin',NOW(),'',NULL,NULL),
(2,1023,'admin',NOW(),'',NULL,NULL),
(2,1024,'admin',NOW(),'',NULL,NULL),
(2,1025,'admin',NOW(),'',NULL,NULL),
(2,1026,'admin',NOW(),'',NULL,NULL),
(2,1027,'admin',NOW(),'',NULL,NULL),
(2,1028,'admin',NOW(),'',NULL,NULL),
(2,1029,'admin',NOW(),'',NULL,NULL),
(2,1030,'admin',NOW(),'',NULL,NULL),
(2,1031,'admin',NOW(),'',NULL,NULL),
(2,1032,'admin',NOW(),'',NULL,NULL),
(2,1033,'admin',NOW(),'',NULL,NULL),
(2,1034,'admin',NOW(),'',NULL,NULL),
(2,1039,'admin',NOW(),'',NULL,NULL),
(2,1040,'admin',NOW(),'',NULL,NULL),
(2,1041,'admin',NOW(),'',NULL,NULL),
(2,1042,'admin',NOW(),'',NULL,NULL),
(2,1043,'admin',NOW(),'',NULL,NULL),
(2,1044,'admin',NOW(),'',NULL,NULL),
(2,1045,'admin',NOW(),'',NULL,NULL),
(2,1046,'admin',NOW(),'',NULL,NULL),
(2,1047,'admin',NOW(),'',NULL,NULL),
(2,1048,'admin',NOW(),'',NULL,NULL),
(2,1049,'admin',NOW(),'',NULL,NULL),
(2,1050,'admin',NOW(),'',NULL,NULL),
(2,1051,'admin',NOW(),'',NULL,NULL),
(2,1052,'admin',NOW(),'',NULL,NULL),
(2,1053,'admin',NOW(),'',NULL,NULL),
(2,1054,'admin',NOW(),'',NULL,NULL),
(2,1055,'admin',NOW(),'',NULL,NULL),
(2,1056,'admin',NOW(),'',NULL,NULL),
(2,1057,'admin',NOW(),'',NULL,NULL),
(2,1058,'admin',NOW(),'',NULL,NULL),
(2,1059,'admin',NOW(),'',NULL,NULL),
(2,1060,'admin',NOW(),'',NULL,NULL);
