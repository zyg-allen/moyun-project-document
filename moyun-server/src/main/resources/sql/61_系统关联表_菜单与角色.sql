-- 来源：all-db-ddl.sql 行4984-4998
-- 用途：sys_role_menu 关联表 CREATE TABLE（角色与菜单关联）

CREATE TABLE `sys_role_menu` (
                                 `role_id` bigint NOT NULL COMMENT '角色ID',
                                 `menu_id` bigint NOT NULL COMMENT '菜单ID',
                                 `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和菜单关联表';

--
-- Dumping data for table `sys_role_menu`
--

