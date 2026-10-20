-- ==============================================================================
-- 墨云项目 · 数据库初始化 DDL（第 1 / 2 部分）
-- 覆盖模块：系统基础 sys_* → 代码生成 gen_* → AI 智能体 ai_*
-- 生成基准：MySQL 8.4 / moyun-db2 实际库结构；字符集统一 utf8mb4 + utf8mb4_0900_ai_ci
-- 幂等重建：每张表 = DROP TABLE IF EXISTS + CREATE TABLE，可反复执行
-- 执行顺序：先本文件，再执行 zyg_ddl_02_biz_job.sql（定时任务含外键，必须最后建）
-- 风险提示：DROP TABLE 会清空同名表；仅用于全新库初始化或整库重建，务必先备份
-- 执行命令：mysql -uroot -p --default-character-set=utf8mb4 < zyg_ddl_01_sys_ai.sql
-- ==============================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';
SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0;


-- ==============================================================================
-- 一、系统基础模块（sys_*）：平台/部门/岗位/角色/菜单/用户/字典/参数
-- 模块优先级：最高。所有业务表与权限体系依赖本段。
-- ==============================================================================

-- ----------------------------------------------------------------------------
-- 表：sys_platform    [端定义（全局公共，用户/支付/VIP/配置/菜单/统计统一引用）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_platform`;
CREATE TABLE `sys_platform` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '端ID',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码',
  `platform_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '端名称',
  `platform_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '端类型：c端/b端',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `domain` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '绑定域名',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_code` (`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='端定义（全局公共，用户/支付/VIP/配置/菜单/统计统一引用）';


-- ----------------------------------------------------------------------------
-- 表：sys_dept    [部门表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';


-- ----------------------------------------------------------------------------
-- 表：sys_post    [岗位信息表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`post_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息表';


-- ----------------------------------------------------------------------------
-- 表：sys_role    [角色信息表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色信息表';


-- ----------------------------------------------------------------------------
-- 表：sys_menu    [菜单权限表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '路由名称',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`menu_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';


-- ----------------------------------------------------------------------------
-- 表：sys_role_menu    [角色和菜单关联表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和菜单关联表';


-- ----------------------------------------------------------------------------
-- 表：sys_user    [用户信息表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';


-- ----------------------------------------------------------------------------
-- 表：sys_user_role    [用户和角色关联表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户和角色关联表';


-- ----------------------------------------------------------------------------
-- 表：sys_user_post    [用户与岗位关联表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与岗位关联表';


-- ----------------------------------------------------------------------------
-- 表：sys_role_dept    [角色和部门关联表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和部门关联表';


-- ----------------------------------------------------------------------------
-- 表：sys_dict_type    [字典类型表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '端代码（sys_platform.platform_code，空=全局）',
  `is_tree` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '结构类型（0=普通字典 1=树形字典）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';


-- ----------------------------------------------------------------------------
-- 表：sys_dict_data    [字典数据表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '端代码（sys_platform.platform_code，空=全局）',
  `parent_dict_id` bigint NOT NULL DEFAULT '0' COMMENT '父字典编码（sys_dict_data.dict_code，0=一级）',
  `is_first_level` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '是否一级（0=否 1=是）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`dict_code`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';


-- ----------------------------------------------------------------------------
-- 表：sys_config    [参数配置表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数键值',
  `platform_code` varchar(50) DEFAULT NULL COMMENT '归属端（NULL=全局，端级优先全局兜底）',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`config_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';


-- ----------------------------------------------------------------------------
-- 表：sys_config_log    [参数配置变更日志表（sys_config 审计留痕）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config_log`;
CREATE TABLE `sys_config_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `config_id` bigint NOT NULL COMMENT '参数主键（逻辑关联 sys_config.config_id，无物理外键）',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数键名（冗余快照，便于审计检索）',
  `old_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '变更前键值',
  `new_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '变更后键值',
  `operate_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'UPDATE' COMMENT '操作类型（UPDATE）',
  `oper_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作人员',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作IP',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_config_id` (`config_id`),
  KEY `idx_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置变更日志表（sys_config 审计留痕）';


-- ----------------------------------------------------------------------------
-- 表：sys_notification    [系统通知主体表（合并 portal_notification + sys_notice）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：system/comment/like/follow/order/notice/announcement',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '通知内容',
  `data` json DEFAULT NULL COMMENT '通知数据（JSON格式）',
  `scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'user' COMMENT '范围：user=个人通知 / all=全局广播',
  `user_id` bigint DEFAULT NULL COMMENT '接收用户ID（scope=user 时必填，scope=all 时为 NULL）',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '接收用户类型：portal=门户用户 / sys=系统用户（scope=user 时生效）',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '通知/公告分类：1=通知 / 2=公告（兼容 sys_notice 字典 sys_notice_type）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0=正常 / 1=关闭（兼容 sys_notice 字典 sys_notice_status）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_scope` (`scope`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_type_user_id` (`user_type`,`user_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知主体表（合并 portal_notification + sys_notice）';


-- ----------------------------------------------------------------------------
-- 表：sys_notification_read    [系统通知用户已读关系表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_notification_read`;
CREATE TABLE `sys_notification_read` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notification_id` bigint NOT NULL COMMENT '通知ID（关联 sys_notification.id）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '已读用户类型：portal=门户用户 / sys=系统用户',
  `read_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notif_user_type` (`notification_id`,`user_id`,`user_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_notification_id` (`notification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知用户已读关系表';


-- ----------------------------------------------------------------------------
-- 表：sys_audit_task    [统一审核任务表（v8.1）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_audit_task`;
CREATE TABLE `sys_audit_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_type` varchar(32) NOT NULL COMMENT '任务类型：article/column/topic/interview_exp/interview_comment/certification/feedback/report',
  `biz_type` varchar(32) DEFAULT NULL COMMENT '业务子类型（如 report 的 spam/infringement）',
  `biz_id` bigint NOT NULL COMMENT '业务记录ID',
  `title` varchar(255) NOT NULL COMMENT '任务标题',
  `description` text COMMENT '任务描述/摘要',
  `submitter_id` bigint DEFAULT NULL COMMENT '提交人ID（门户用户ID）',
  `submitter_name` varchar(64) DEFAULT NULL COMMENT '提交人用户名',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
  `auditor_id` bigint DEFAULT NULL COMMENT '处理人ID（系统用户ID）',
  `auditor_name` varchar(64) DEFAULT NULL COMMENT '处理人用户名',
  `audit_opinion` varchar(1000) DEFAULT NULL COMMENT '审核意见（驳回时必填）',
  `audit_action` varchar(20) DEFAULT NULL COMMENT '审核操作类型：approve/reject',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `audit_time` datetime DEFAULT NULL COMMENT '处理时间',
  `priority` varchar(10) NOT NULL DEFAULT 'medium' COMMENT '优先级：high/medium/low',
  `route_path` varchar(255) DEFAULT NULL COMMENT '查看详情跳转路径',
  `extra_data` text COMMENT '扩展数据 JSON',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_status` (`status`),
  KEY `idx_audit_auditor` (`auditor_id`),
  KEY `idx_audit_submitter` (`submitter_id`),
  KEY `idx_audit_biz` (`biz_type`,`biz_id`),
  KEY `idx_audit_task_type` (`task_type`,`status`),
  KEY `idx_audit_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一审核任务表（v8.1）';


-- ----------------------------------------------------------------------------
-- 表：sys_sensitive_word    [敏感词库]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_sensitive_word`;
CREATE TABLE `sys_sensitive_word` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word` varchar(128) NOT NULL COMMENT '敏感词',
  `category` varchar(32) DEFAULT NULL COMMENT '分类：politics=政治/porn=色情/ad=广告/insult=辱骂/other=其他',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态：0=启用 1=禁用',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记：0=存在 2=删除（BaseEntity 逻辑删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感词库';


-- ----------------------------------------------------------------------------
-- 表：sys_sensitive_word_log    [敏感词命中记录]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_sensitive_word_log`;
CREATE TABLE `sys_sensitive_word_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型：article/column/topic/topic_post/topic_comment/report',
  `biz_id` bigint DEFAULT NULL COMMENT '业务主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '提交人ID（portal_user.id）',
  `content` text COMMENT '被检测的原始内容片段（截断）',
  `hit_words` varchar(500) DEFAULT NULL COMMENT '命中的敏感词列表（逗号分隔）',
  `hit_count` int NOT NULL DEFAULT '0' COMMENT '命中数量',
  `action` varchar(16) NOT NULL DEFAULT 'block' COMMENT '处理动作：block=拦截/pending=转待审核/flag=标记',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感词命中记录';


-- ----------------------------------------------------------------------------
-- 表：sys_file    [文件管理表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(500) NOT NULL COMMENT '文件名称',
  `file_ext` varchar(100) DEFAULT NULL COMMENT '文件扩展名',
  `file_type` varchar(100) DEFAULT NULL COMMENT '文件类型（image/document/video/audio/other）',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_url` varchar(1000) NOT NULL COMMENT '文件访问URL',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `storage_type` varchar(100) DEFAULT NULL COMMENT '存储类型（minio/local）',
  `bucket_name` varchar(100) DEFAULT NULL COMMENT '存储桶名称',
  `object_name` varchar(500) DEFAULT NULL COMMENT '对象名称',
  `fallback` tinyint(1) DEFAULT '0' COMMENT '是否降级存储（0=正常 1=因MinIO不可用降级到本地）',
  `local_path` varchar(500) DEFAULT NULL COMMENT '本地备份绝对路径（MinIO可用时也记录，便于降级访问）',
  `file_md5` varchar(500) DEFAULT NULL COMMENT '文件MD5值',
  `upload_user_id` bigint DEFAULT NULL COMMENT '上传用户ID',
  `upload_user_name` varchar(100) DEFAULT NULL COMMENT '上传用户名称',
  `status` varchar(20) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `business_type` varchar(500) DEFAULT NULL COMMENT '业务类型',
  `business_id` varchar(100) DEFAULT NULL COMMENT '业务ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_file_type` (`file_type`),
  KEY `idx_storage_type` (`storage_type`),
  KEY `idx_business_type` (`business_type`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_upload_user_id` (`upload_user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_fallback` (`fallback`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件管理表';


-- ----------------------------------------------------------------------------
-- 表：sys_job_log    [定时任务调度日志表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '日志信息',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '异常信息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度日志表';


-- ----------------------------------------------------------------------------
-- 表：sys_logininfor    [系统访问记录]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `user_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'sys' COMMENT '登录来源类型（sys=后台用户 portal=门户用户）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`),
  KEY `idx_sys_logininfor_ut` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统访问记录';


-- ----------------------------------------------------------------------------
-- 表：sys_oper_log    [操作日志记录]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志记录';


-- ==============================================================================
-- 二、代码生成模块（gen_*）：表与字段元数据
-- 无业务依赖，随系统模块一并创建。
-- ==============================================================================

-- ----------------------------------------------------------------------------
-- 表：gen_table    [代码生成业务表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表';


-- ----------------------------------------------------------------------------
-- 表：gen_table_column    [代码生成业务表字段]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表字段';


-- ==============================================================================
-- 三、AI 模块（ai_*）：智能体/工具/场景/模型/知识库/会话/日志
-- 模块优先级：次高。AI 场景配置为业务模块提供统一网关能力。
-- ==============================================================================

-- ----------------------------------------------------------------------------
-- 表：ai_provider    [AI 提供商注册表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_provider`;
CREATE TABLE `ai_provider` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(32) NOT NULL COMMENT '提供商编码（model_config.provider 关联值，小写唯一）',
  `name` varchar(64) NOT NULL COMMENT '显示名称',
  `api_style` varchar(32) NOT NULL DEFAULT 'openai_compatible' COMMENT 'API 风格：openai_compatible=OpenAI兼容 / ollama_native=Ollama原生',
  `default_base_url` varchar(255) DEFAULT NULL COMMENT '默认 Base URL（模型配置留空时兜底）',
  `supports_streaming` tinyint(1) NOT NULL DEFAULT '1' COMMENT '该提供商 chat 模型是否支持流式输出',
  `requires_api_key` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否必须配置 API Key',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用（停用后不可选/不可用）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序（小在前）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 提供商注册表';


-- ----------------------------------------------------------------------------
-- 表：ai_model_config    [模型配置表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_model_config`;
CREATE TABLE `ai_model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型提供商(openai/ollama/dashscope)',
  `model_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'chat' COMMENT '模型类型(chat/embedding/multimodal/reranker/asr/tts)，asr=语音识别，tts=语音合成，V10.0 语音面试官使用',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型名称',
  `api_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'API密钥',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'API基础URL',
  `temperature` double DEFAULT '0.7' COMMENT '温度参数(0-2)',
  `max_tokens` int DEFAULT '2000' COMMENT '最大Token数',
  `timeout` int DEFAULT '60' COMMENT '超时时间(秒)',
  `streaming_supported` tinyint(1) DEFAULT '1' COMMENT '是否支持流式输出',
  `supports_json_mode` tinyint(1) DEFAULT '0' COMMENT '是否支持JSON Mode（结构化场景下发response_format强制JSON输出）',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否为默认模型',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '备注说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `input_price` decimal(10,6) DEFAULT '0.001000' COMMENT '输入价格（元/1000 tokens）',
  `output_price` decimal(10,6) DEFAULT '0.002000' COMMENT '输出价格（元/1000 tokens）',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_provider` (`provider`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_is_default` (`is_default`) USING BTREE,
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='模型配置表';


-- ----------------------------------------------------------------------------
-- 表：ai_agent    [智能体表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '智能体名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '智能体描述',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '系统提示词',
  `knowledge_library_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '关联的知识库ID列表（JSON数组）',
  `knowledge_base_weights` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '知识库权重配置（JSON格式：{"1": 1.0, "2": 0.8}，权重范围0.1-1.0）',
  `model_config_id` bigint DEFAULT NULL COMMENT '模型配置ID(关联model_config表,NULL则使用默认模型)',
  `model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'qwen-plus' COMMENT '模型名称',
  `temperature` double DEFAULT '0.7' COMMENT '温度参数',
  `max_tokens` int DEFAULT '2000' COMMENT '最大token数',
  `rag_min_score` double DEFAULT NULL COMMENT 'RAG检索相似度阈值(0.5-1.0,推荐0.7-0.75,NULL则使用全局配置)',
  `rag_max_results` int DEFAULT NULL COMMENT 'RAG检索最大结果数量(1-10,推荐3-5,NULL则使用全局配置)',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `welcome_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '开场白',
  `suggested_questions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '预设问题(JSON数组)',
  `show_citations` tinyint(1) DEFAULT '1' COMMENT '是否显示引用来源',
  `max_history_turns` int DEFAULT '10' COMMENT '最大历史轮数',
  `api_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用API',
  `api_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'API Key',
  `workflow_id` bigint DEFAULT NULL COMMENT '关联工作流ID',
  `workflow_trigger_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'manual' COMMENT '工作流触发模式: manual/auto/keyword',
  `workflow_trigger_keywords` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '触发关键词(JSON数组)',
  `publish_enabled` tinyint(1) DEFAULT '0' COMMENT '是否发布为应用',
  `publish_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发布访问Token',
  `publish_settings` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '发布设置(JSON)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `rag_recall_multiplier` double DEFAULT NULL COMMENT '第一阶段召回倍数（1.5-3.0，推荐2.0，NULL时使用全局配置）',
  `rag_enable_hybrid_search` tinyint(1) DEFAULT '1' COMMENT '是否启用混合检索（向量+BM25）',
  `rag_enable_query_expansion` tinyint(1) DEFAULT '1' COMMENT '是否启用查询扩展',
  `rag_bm25_weight` double DEFAULT '0.3' COMMENT 'BM25检索权重（0-1）',
  `rag_vector_weight` double DEFAULT '0.7' COMMENT '向量检索权重（0-1）',
  `enable_self_reflection` tinyint(1) DEFAULT '0' COMMENT '是否启用自我反思',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_model_config_id` (`model_config_id`) USING BTREE,
  KEY `fk_agent_workflow` (`workflow_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能体表';


-- ----------------------------------------------------------------------------
-- 表：ai_agent_tool    [智能体工具定义表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_tool`;
CREATE TABLE `ai_agent_tool` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具标识（英文）',
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '显示名称（中文）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具描述（给LLM理解用）',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'general' COMMENT '工具分类：general/information/utility/action/data',
  `tool_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具类型：builtin/http/database',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'fa-wrench' COMMENT '图标（FontAwesome）',
  `config` json DEFAULT NULL COMMENT '工具配置（API地址、认证信息等）',
  `parameters` json NOT NULL COMMENT '参数定义（JSON Schema格式）',
  `timeout_seconds` int DEFAULT '30' COMMENT '超时时间（秒）',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统内置',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`name`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能体工具定义表';


-- ----------------------------------------------------------------------------
-- 表：ai_agent_tool_relation    [智能体工具关联表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_tool_relation`;
CREATE TABLE `ai_agent_tool_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `tool_id` bigint NOT NULL COMMENT '工具ID',
  `custom_config` json DEFAULT NULL COMMENT '针对该智能体的自定义配置',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_agent_tool` (`agent_id`,`tool_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_tool_id` (`tool_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能体工具关联表';


-- ----------------------------------------------------------------------------
-- 表：ai_agent_dictionary_relation    [智能体词典关联表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_dictionary_relation`;
CREATE TABLE `ai_agent_dictionary_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `dictionary_id` bigint NOT NULL COMMENT '词典ID',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_agent_dict` (`agent_id`,`dictionary_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_dictionary_id` (`dictionary_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能体词典关联表';


-- ----------------------------------------------------------------------------
-- 表：ai_agent_workflow_relation    [智能体-工作流关联表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_workflow_relation`;
CREATE TABLE `ai_agent_workflow_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_agent_workflow` (`agent_id`,`workflow_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_workflow_id` (`workflow_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能体-工作流关联表';


-- ----------------------------------------------------------------------------
-- 表：ai_domain_dictionary    [领域词典表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_domain_dictionary`;
CREATE TABLE `ai_domain_dictionary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '核心词',
  `related_terms` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '相关词列表（逗号分隔）',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'general' COMMENT '分类（服务器、架构、模型、通用等）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '词典说明',
  `is_global` tinyint(1) DEFAULT '1' COMMENT '是否全局词典（全局词典默认对所有智能体生效）',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `priority` int DEFAULT '0' COMMENT '优先级（数字越大优先级越高）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_keyword` (`keyword`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_global` (`is_global`) USING BTREE,
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='领域词典表';


-- ----------------------------------------------------------------------------
-- 表：ai_scene_config    [AI场景配置表（业务场景与Agent/模型/知识库/工作流动态绑定）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_scene_config`;
CREATE TABLE `ai_scene_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` varchar(50) NOT NULL COMMENT '场景代码：voice_interview/resume_optimize/question_generate',
  `scene_name` varchar(100) NOT NULL COMMENT '场景名称',
  `description` varchar(500) DEFAULT NULL COMMENT '场景描述',
  `scene_category` varchar(30) DEFAULT NULL COMMENT '场景分类: chat/analysis/generation/classification',
  `agent_id` bigint DEFAULT NULL COMMENT '绑定的智能体（ai_agent.id，启用状态才生效）',
  `model_config_id` bigint DEFAULT NULL COMMENT '直接绑定模型（ai_model_config.id，agent_id 为空时生效）',
  `knowledge_library_ids` varchar(500) DEFAULT NULL COMMENT '知识库ID列表 JSON数组',
  `tool_ids` varchar(500) DEFAULT NULL COMMENT '工具ID列表 JSON数组',
  `workflow_id` bigint DEFAULT NULL COMMENT '绑定工作流（ai_workflow.id）',
  `config_json` text COMMENT '场景策略配置 JSON（如 dynamicMode）',
  `handler_bean_name` varchar(100) DEFAULT NULL COMMENT '对应Spring Bean名称（统一网关路由）',
  `handler_method` varchar(50) DEFAULT 'execute' COMMENT '执行方法名',
  `system_prompt_template` text COMMENT '系统提示词模板（支持占位符 {{variable}}）',
  `user_prompt_template` text COMMENT '用户提示词模板',
  `prompt_placeholders` json DEFAULT NULL COMMENT '占位符说明 {key: description}',
  `output_mode` varchar(20) DEFAULT 'sync' COMMENT '输出模式: sync/stream/both',
  `output_schema` json DEFAULT NULL COMMENT '输出结构定义',
  `output_parser` varchar(50) DEFAULT NULL COMMENT '解析器: json/markdown/custom',
  `max_tokens` int DEFAULT '2048' COMMENT '最大Token数',
  `temperature` decimal(2,1) DEFAULT '0.7' COMMENT '温度参数',
  `timeout_seconds` int DEFAULT '30' COMMENT '超时秒数',
  `retry_count` int DEFAULT '3' COMMENT '重试次数',
  `rate_limit_key` varchar(50) DEFAULT NULL COMMENT '限流Key',
  `rate_limit_count` int DEFAULT '100' COMMENT '限流次数',
  `rate_limit_time` int DEFAULT '60' COMMENT '限流时间窗口(秒)',
  `daily_token_limit` int DEFAULT NULL COMMENT '场景日Token上限（当日累计超限拒绝调用；null/0=不限）',
  `max_input_tokens` int DEFAULT NULL COMMENT '输入token上限（提示词渲染后估算超限按truncate_strategy截断；null=不限）',
  `max_output_tokens` int DEFAULT NULL COMMENT '输出token上限（下发模型max_tokens模型侧截断；null=不限/沿用Agent设置）',
  `truncate_strategy` varchar(20) DEFAULT 'head_tail' COMMENT '输入截断策略: head(保头部)/tail(保尾部)/head_tail(保两端去中间)',
  `enable_output_filter` tinyint(1) DEFAULT '0' COMMENT '是否启用输出内容过滤（响应data文本经DFA词树脱敏；0=关闭）',
  `enable_intent_routing` tinyint(1) DEFAULT '0' COMMENT '是否启用意图分类路由：1=开启（userInput自由文本参与意图分类与场景路由），0=关闭（默认，网关跳过分类）',
  `fallback_model_id` bigint DEFAULT NULL COMMENT '备用模型ID',
  `fallback_response` text COMMENT '兜底回复（AI不可用时返回）',
  `enable_cache` tinyint(1) DEFAULT '0' COMMENT '是否启用缓存',
  `cache_ttl` int DEFAULT '3600' COMMENT '缓存时间(秒)',
  `version` varchar(20) NOT NULL DEFAULT 'v1' COMMENT '版本号',
  `config_version` int NOT NULL DEFAULT '1' COMMENT '配置版本号（保存自动+1，回滚安全网，区别于灰度version）',
  `weight` int NOT NULL DEFAULT '100' COMMENT '灰度权重（同场景多版本按权重轮盘赌）',
  `priority` int NOT NULL DEFAULT '0' COMMENT '优先级（全 0 时取 is_default，再按 priority DESC）',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认版本',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `open_api` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开放通用入口调用（1=可经 /api/ai/execute 外部调用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_version` (`scene_code`,`version`),
  KEY `idx_scene_code` (`scene_code`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI场景配置表（业务场景与Agent/模型/知识库/工作流动态绑定）';


-- ----------------------------------------------------------------------------
-- 表：ai_scene_config_history    [AI场景配置版本快照表（保存自动快照，一键回滚）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_scene_config_history`;
CREATE TABLE `ai_scene_config_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id` bigint NOT NULL COMMENT '源配置主键（逻辑关联 ai_scene_config.id，无物理外键）',
  `scene_code` varchar(50) NOT NULL COMMENT '场景代码（冗余快照，支持 scene:task 全码）',
  `config_version` int NOT NULL COMMENT '快照版本号（快照时源行的 config_version）',
  `snapshot` json NOT NULL COMMENT '配置行完整快照(JSON)',
  `operator` varchar(64) DEFAULT '' COMMENT '操作人（保存/回滚触发者）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
  PRIMARY KEY (`id`),
  KEY `idx_config_id` (`config_id`),
  KEY `idx_scene_version` (`scene_code`,`config_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI场景配置版本快照表（保存自动快照，一键回滚）';


-- ----------------------------------------------------------------------------
-- 表：ai_knowledge_library    [知识库主表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_knowledge_library`;
CREATE TABLE `ai_knowledge_library` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库描述',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '?' COMMENT '知识库图标',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库分类（如：技术文档、产品手册、FAQ等）',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '标签（JSON数组格式）',
  `document_count` int DEFAULT '0' COMMENT '文档数量',
  `total_segments` int DEFAULT '0' COMMENT '总分段数',
  `total_size` bigint DEFAULT '0' COMMENT '总文件大小（字节）',
  `usage_count` int DEFAULT '0' COMMENT '使用次数（被检索次数）',
  `hit_count` int DEFAULT '0' COMMENT '命中次数',
  `last_used_time` datetime DEFAULT NULL COMMENT '最后使用时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态：active(正常), disabled(禁用), archived(归档)',
  `is_public` tinyint(1) DEFAULT '1' COMMENT '是否公开（预留多租户）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_usage_count` (`usage_count`) USING BTREE,
  KEY `idx_deleted` (`deleted`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库主表';


-- ----------------------------------------------------------------------------
-- 表：ai_knowledge_library_config    [知识库配置表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_knowledge_library_config`;
CREATE TABLE `ai_knowledge_library_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `library_id` bigint NOT NULL COMMENT '知识库ID',
  `segment_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'general' COMMENT '分段模式：general(通用), qa(问答), code(代码)',
  `segment_separator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '\n\n' COMMENT '分段标识符',
  `segment_max_length` int NOT NULL DEFAULT '800' COMMENT '分段最大长度',
  `segment_overlap_length` int NOT NULL DEFAULT '100' COMMENT '分段重叠长度',
  `preprocess_replace_spaces` tinyint(1) DEFAULT '1' COMMENT '替换连续空格',
  `preprocess_remove_urls` tinyint(1) DEFAULT '1' COMMENT '删除URL',
  `preprocess_remove_extra_newlines` tinyint(1) DEFAULT '1' COMMENT '删除多余换行',
  `index_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'high_quality' COMMENT '索引模式：high_quality, economy',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Embedding模型',
  `retrieval_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'hybrid' COMMENT '检索模式：vector, keyword, hybrid',
  `retrieval_top_k` int DEFAULT '10' COMMENT '检索返回数量',
  `rerank_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用Rerank',
  `rerank_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Rerank模型',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_library_id` (`library_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库配置表';


-- ----------------------------------------------------------------------------
-- 表：ai_knowledge_base    [知识库表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_knowledge_base`;
CREATE TABLE `ai_knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `library_id` bigint DEFAULT NULL COMMENT '所属知识库ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件路径',
  `pdf_file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'PDF文件路径（用于预览）',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文件类型',
  `vector_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '向量ID（Pinecone中的ID）',
  `segment_count` int DEFAULT NULL COMMENT '文档分段数量',
  `vector_dimension` int DEFAULT NULL COMMENT '向量维度',
  `status` int DEFAULT '0' COMMENT '处理状态：0-待处理，1-处理中，2-处理成功，3-处理失败',
  `processing_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '处理状态：pending(待配置), configured(已配置), processing(处理中), completed(已完成), failed(失败)',
  `config_completed` tinyint(1) DEFAULT '0' COMMENT '是否完成配置',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `update_time` datetime DEFAULT NULL COMMENT '处理时间',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库分组',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '知识库标签（JSON数组）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库描述',
  `usage_count` int DEFAULT '0' COMMENT '使用次数',
  `hit_count` int DEFAULT '0' COMMENT '命中次数',
  `last_used_time` datetime DEFAULT NULL COMMENT '最后使用时间',
  `parse_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文档解析方式: POI, PDFBox, Text',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文件内容SHA-256哈希值（用于增量更新检测）',
  `last_processed_time` datetime DEFAULT NULL COMMENT '上次处理时间',
  `need_reprocess` tinyint(1) DEFAULT '0' COMMENT '是否需要重新处理',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_usage_count` (`usage_count`) USING BTREE,
  KEY `idx_last_used_time` (`last_used_time`) USING BTREE,
  KEY `idx_library_id` (`library_id`) USING BTREE,
  KEY `idx_kb_content_hash` (`content_hash`) USING BTREE,
  KEY `idx_deleted` (`deleted`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库表';


-- ----------------------------------------------------------------------------
-- 表：ai_knowledge_config    [知识库配置表 - 包含分片策略、文档类型识别、预处理规则等配置]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_knowledge_config`;
CREATE TABLE `ai_knowledge_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `segment_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'general' COMMENT '分段模式：general(通用), parent_child(父子分段)',
  `segment_separator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '\n\n' COMMENT '分段标识符',
  `segment_max_length` int NOT NULL DEFAULT '800' COMMENT '分段最大长度（字符数，800字符确保题库问答对完整，技术文档可用500，小说可用1500）',
  `segment_overlap_length` int NOT NULL DEFAULT '100' COMMENT '分段重叠长度（字符数，100字符保证上下文连贯性）',
  `chunking_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'fixed' COMMENT '分片策略: fixed(固定大小), adaptive(自适应), document_type(按文档类型)',
  `document_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'general' COMMENT '文档类型: general(通用), faq(问答), table(表格), code(代码), technical(技术文档)',
  `faq_chunk_size` int DEFAULT '400' COMMENT 'FAQ分片大小(字符)',
  `table_chunk_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'by_row' COMMENT '表格分片策略: by_row(按行), by_table(整表), by_cell(按单元格)',
  `code_chunk_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'by_function' COMMENT '代码分片策略: by_function(按函数), by_class(按类), by_file(按文件)',
  `technical_chunk_size` int DEFAULT '1200' COMMENT '技术文档分片大小(字符)',
  `enable_smart_boundary` tinyint(1) DEFAULT '1' COMMENT '启用智能边界检测(避免切断句子)',
  `preprocess_replace_spaces` tinyint(1) DEFAULT '1' COMMENT '替换连续空格、换行、制表符',
  `preprocess_remove_urls` tinyint(1) DEFAULT '1' COMMENT '删除URL和邮箱地址',
  `preprocess_remove_extra_newlines` tinyint(1) DEFAULT '1' COMMENT '删除多余换行',
  `index_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'high_quality' COMMENT '索引方式：high_quality(高质量), economy(经济)',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '嵌入模型名称',
  `retrieval_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'vector' COMMENT '检索模式：vector(向量), keyword(关键词), hybrid(混合)',
  `retrieval_top_k` int DEFAULT '3' COMMENT '检索Top K数量',
  `rerank_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用重排序',
  `rerank_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '重排序模型',
  `qa_mode` tinyint(1) DEFAULT '0' COMMENT '是否启用Q&A模式',
  `qa_extraction_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'Q&A提取提示词',
  `preprocess_remove_special_chars` tinyint(1) DEFAULT '0' COMMENT '删除特殊字符',
  `preprocess_remove_table_desc` tinyint(1) DEFAULT '0' COMMENT '删除表格描述',
  `preprocess_remove_header_footer` tinyint(1) DEFAULT '0' COMMENT '删除页眉页脚',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_knowledge_id` (`knowledge_id`) USING BTREE,
  KEY `idx_segment_mode` (`segment_mode`) USING BTREE,
  KEY `idx_index_mode` (`index_mode`) USING BTREE,
  KEY `idx_chunking_strategy` (`chunking_strategy`) USING BTREE,
  KEY `idx_document_type` (`document_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库配置表 - 包含分片策略、文档类型识别、预处理规则等配置';


-- ----------------------------------------------------------------------------
-- 表：ai_knowledge_config_template    [知识库配置模板表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_knowledge_config_template`;
CREATE TABLE `ai_knowledge_config_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `template_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模板描述',
  `template_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板类型：general(通用), technical(技术文档), legal(法律), medical(医疗)',
  `config_json` json NOT NULL COMMENT '配置JSON',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统预设模板',
  `use_count` int DEFAULT '0' COMMENT '使用次数',
  `is_recommended` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否推荐模板',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_template_type` (`template_type`) USING BTREE,
  KEY `idx_use_count` (`use_count`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库配置模板表';


-- ----------------------------------------------------------------------------
-- 表：ai_document_segment    [文档分片表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_document_segment`;
CREATE TABLE `ai_document_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '关联的知识库ID',
  `segment_index` int NOT NULL COMMENT '分片索引（第几个分片）',
  `page_number` int DEFAULT NULL COMMENT 'PDF页码',
  `line_start` int DEFAULT NULL COMMENT '起始行号',
  `line_end` int DEFAULT NULL COMMENT '结束行号',
  `char_start` int DEFAULT NULL COMMENT '起始字符位置',
  `char_end` int DEFAULT NULL COMMENT '结束字符位置',
  `chapter_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '章节标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分片内容',
  `content_length` int DEFAULT NULL COMMENT '分片内容长度',
  `embedding_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '向量ID（在Pinecone中的ID）',
  `vector_dimension` int DEFAULT NULL COMMENT '向量维度',
  `vector_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '向量数据（JSON格式）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_knowledge_base_id` (`knowledge_base_id`) USING BTREE,
  KEY `idx_embedding_id` (`embedding_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='文档分片表';


-- ----------------------------------------------------------------------------
-- 表：ai_document_image    [文档图片表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_document_image`;
CREATE TABLE `ai_document_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '关联的知识库ID',
  `image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片文件路径',
  `page_number` int DEFAULT NULL COMMENT '所在页码',
  `image_index` int DEFAULT NULL COMMENT '图片在页面中的索引',
  `embedding_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '向量ID',
  `vector_dimension` int DEFAULT NULL COMMENT '向量维度',
  `width` int DEFAULT NULL COMMENT '图片宽度',
  `height` int DEFAULT NULL COMMENT '图片高度',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '图片内容描述（多模态模型生成）',
  `description_language` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'zh' COMMENT '描述语言(zh/en)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_knowledge_base_id` (`knowledge_base_id`) USING BTREE,
  KEY `idx_embedding_id` (`embedding_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='文档图片表';


-- ----------------------------------------------------------------------------
-- 表：ai_datasource_config    [数据源配置表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_datasource_config`;
CREATE TABLE `ai_datasource_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源类型: mysql, elasticsearch, mongodb',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口号',
  `database_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '数据库名称',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '密码(加密存储)',
  `connection_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '额外连接参数(JSON格式)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用: 0-禁用, 1-启用',
  `health_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'unknown' COMMENT '健康状态: healthy, unhealthy, unknown',
  `last_check_time` datetime DEFAULT NULL COMMENT '最后检查时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='数据源配置表';


-- ----------------------------------------------------------------------------
-- 表：ai_table_metadata    [表元数据缓存表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_table_metadata`;
CREATE TABLE `ai_table_metadata` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '表名',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表注释',
  `table_schema` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '表结构(JSON格式)',
  `column_count` int DEFAULT '0' COMMENT '字段数量',
  `row_count` bigint DEFAULT '0' COMMENT '行数(估算)',
  `data_size` bigint DEFAULT '0' COMMENT '数据大小(字节)',
  `has_primary_key` tinyint(1) DEFAULT '0' COMMENT '是否有主键',
  `has_time_field` tinyint(1) DEFAULT '0' COMMENT '是否有时间字段',
  `time_field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '时间字段名',
  `numeric_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '数值型字段列表(JSON)',
  `category_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '类别型字段列表(JSON)',
  `indexed_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '索引字段列表(JSON)',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最后同步时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_datasource_table` (`datasource_id`,`table_name`) USING BTREE,
  KEY `idx_datasource_id` (`datasource_id`) USING BTREE,
  KEY `idx_last_sync_time` (`last_sync_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='表元数据缓存表';


-- ----------------------------------------------------------------------------
-- 表：ai_workflow    [工作流定义表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_workflow`;
CREATE TABLE `ai_workflow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作流名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '工作流描述',
  `graph_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '工作流图定义(JSON)',
  `variables` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '全局变量定义(JSON)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'draft' COMMENT '状态: draft-草稿, published-已发布, disabled-已禁用',
  `version` int DEFAULT '1' COMMENT '版本号',
  `enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流定义表';


-- ----------------------------------------------------------------------------
-- 表：ai_workflow_version    [工作流版本表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_workflow_version`;
CREATE TABLE `ai_workflow_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `version` int NOT NULL COMMENT '版本号',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '版本描述',
  `graph_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '工作流图数据快照(JSON)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_workflow_id` (`workflow_id`) USING BTREE,
  KEY `idx_version` (`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流版本表';


-- ----------------------------------------------------------------------------
-- 表：ai_workflow_execution    [工作流执行记录表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_workflow_execution`;
CREATE TABLE `ai_workflow_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'running' COMMENT '执行状态: running-执行中, completed-已完成, failed-失败, cancelled-已取消',
  `input_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '输入参数(JSON)',
  `output_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '输出结果(JSON)',
  `execution_log` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行日志(JSON数组)',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `current_node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '当前执行到的节点ID',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_workflow_id` (`workflow_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流执行记录表';


-- ----------------------------------------------------------------------------
-- 表：ai_conversation    [对话会话表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_conversation`;
CREATE TABLE `ai_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '新对话' COMMENT '会话标题（自动生成或用户修改）',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户ID（预留字段，支持多用户）',
  `message_count` int DEFAULT '0' COMMENT '消息数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '对话摘要',
  `summary_updated_at` datetime DEFAULT NULL COMMENT '摘要更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_update_time` (`update_time`) USING BTREE,
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='对话会话表';


-- ----------------------------------------------------------------------------
-- 表：ai_conversation_message    [对话消息表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_conversation_message`;
CREATE TABLE `ai_conversation_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色：user/assistant',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `reference_sources` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '参考来源（JSON格式）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_conversation_id` (`conversation_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='对话消息表';


-- ----------------------------------------------------------------------------
-- 表：ai_chat_history    [对话历史表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_chat_history`;
CREATE TABLE `ai_chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `user_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '用户消息',
  `assistant_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '助手回复',
  `tokens_used` int DEFAULT '0' COMMENT 'Token消耗',
  `retrieval_results` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '检索结果JSON',
  `retrieval_count` int DEFAULT '0' COMMENT '检索命中数',
  `response_time` int DEFAULT '0' COMMENT '响应时间(毫秒)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_session_id` (`session_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='对话历史表';


-- ----------------------------------------------------------------------------
-- 表：ai_query_history    [查询历史表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_query_history`;
CREATE TABLE `ai_query_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '会话ID',
  `natural_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '自然语言查询',
  `generated_sql` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '生成的SQL语句',
  `query_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '查询类型: select, aggregate, join, analysis',
  `tables_involved` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '涉及的表(逗号分隔)',
  `result_count` int DEFAULT '0' COMMENT '结果行数',
  `execution_time` int DEFAULT '0' COMMENT '执行时间(毫秒)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'success' COMMENT '执行状态: success, failed, timeout',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `analysis_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分析类型: basic, trend, correlation, ranking',
  `chart_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图表类型',
  `has_insight` tinyint(1) DEFAULT '0' COMMENT '是否生成洞察',
  `token_used` int DEFAULT '0' COMMENT 'LLM消耗的Token数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_datasource_id` (`datasource_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_session_id` (`session_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='查询历史表';


-- ----------------------------------------------------------------------------
-- 表：ai_reference_feedback    [参考来源反馈表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_reference_feedback`;
CREATE TABLE `ai_reference_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint DEFAULT NULL COMMENT '知识库ID',
  `file_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文件名',
  `page_number` int DEFAULT NULL COMMENT '页码',
  `segment_index` int DEFAULT NULL COMMENT '分片索引',
  `user_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '用户查询',
  `rerank_score` double DEFAULT NULL COMMENT '重排分数',
  `vector_score` double DEFAULT NULL COMMENT '向量相似度',
  `feedback_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈类型：accurate(准确), inaccurate(不准确)',
  `agent_id` bigint DEFAULT NULL COMMENT '智能体ID',
  `memory_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '会话ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_knowledge_base_id` (`knowledge_base_id`) USING BTREE,
  KEY `idx_feedback_type` (`feedback_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='参考来源反馈表';


-- ----------------------------------------------------------------------------
-- 表：ai_execute_log    [AI能力调用日志表（可观测性）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_execute_log`;
CREATE TABLE `ai_execute_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `request_id` varchar(64) NOT NULL COMMENT '请求ID',
  `scene_code` varchar(50) NOT NULL COMMENT '场景代码',
  `user_id` bigint DEFAULT NULL COMMENT '发起用户ID（网关请求方）',
  `handler_name` varchar(100) DEFAULT NULL COMMENT 'Handler名称',
  `bind_type` varchar(20) DEFAULT NULL COMMENT '绑定类型',
  `model_used` varchar(100) DEFAULT NULL COMMENT '使用的模型',
  `agent_used` varchar(100) DEFAULT NULL COMMENT '使用的Agent',
  `token_used` int DEFAULT '0' COMMENT 'Token消耗',
  `input_tokens` int DEFAULT NULL COMMENT '输入Token（模型回传细分；未回传为NULL）',
  `output_tokens` int DEFAULT NULL COMMENT '输出Token（模型回传细分；未回传为NULL）',
  `token_estimated` tinyint(1) DEFAULT '0' COMMENT 'Token是否为本地估算：1=估算（服务端未回传usage，本地分词得出），0/NULL=服务端真实值',
  `degraded` tinyint(1) DEFAULT '0' COMMENT '是否降级响应：1=降级（重试失败回落默认模型/网关兜底），0/NULL=正常',
  `cost_yuan` decimal(12,6) DEFAULT NULL COMMENT '本次调用成本（元，细分token×模型单价）',
  `tool_calls` json DEFAULT NULL COMMENT '工具调用记录',
  `input_summary` varchar(500) DEFAULT NULL COMMENT '输入摘要',
  `output_summary` varchar(500) DEFAULT NULL COMMENT '输出摘要',
  `status` varchar(20) DEFAULT 'success' COMMENT 'success/fail/timeout',
  `error_msg` text COMMENT '错误信息',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '耗时(毫秒)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_scene_code` (`scene_code`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_scene` (`user_id`,`scene_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI能力调用日志表（可观测性）';


-- ----------------------------------------------------------------------------
-- 表：ai_tool_call_log    [工具调用日志表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_tool_call_log`;
CREATE TABLE `ai_tool_call_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `conversation_id` bigint DEFAULT NULL COMMENT '会话ID',
  `message_id` bigint DEFAULT NULL COMMENT '消息ID',
  `agent_id` bigint DEFAULT NULL COMMENT '智能体ID',
  `tool_id` bigint DEFAULT NULL COMMENT '工具ID',
  `tool_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具名称',
  `input_params` json DEFAULT NULL COMMENT '输入参数',
  `output_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '输出结果',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'success' COMMENT '状态：pending/running/success/failed/timeout',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `duration_ms` int DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_conversation_id` (`conversation_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_tool_name` (`tool_name`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工具调用日志表';


-- ----------------------------------------------------------------------------
-- 表：ai_token_usage_log    [Token使用记录表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_token_usage_log`;
CREATE TABLE `ai_token_usage_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint DEFAULT NULL COMMENT '会话ID',
  `message_id` bigint DEFAULT NULL COMMENT '消息ID',
  `agent_id` bigint DEFAULT NULL COMMENT '智能体ID',
  `workflow_id` bigint DEFAULT NULL COMMENT '工作流ID',
  `workflow_execution_id` bigint DEFAULT NULL COMMENT '工作流执行ID',
  `workflow_node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '工作流节点ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户ID',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型名称',
  `model_provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型提供商',
  `input_tokens` int DEFAULT '0' COMMENT '输入token数',
  `output_tokens` int DEFAULT '0' COMMENT '输出token数',
  `total_tokens` int DEFAULT '0' COMMENT '总token数',
  `cost` decimal(10,6) DEFAULT NULL COMMENT '费用（元）',
  `request_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求类型：chat/embedding_query/embedding_document/workflow_llm/workflow_classifier/workflow_extractor/workflow_question',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_conversation_id` (`conversation_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_model_name` (`model_name`) USING BTREE,
  KEY `idx_workflow_id` (`workflow_id`) USING BTREE,
  KEY `idx_workflow_execution_id` (`workflow_execution_id`) USING BTREE,
  KEY `fk_tul_message` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='Token使用记录表';


-- ----------------------------------------------------------------------------
-- 表：ai_token_usage_summary    [Token使用统计汇总表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_token_usage_summary`;
CREATE TABLE `ai_token_usage_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint DEFAULT NULL COMMENT '智能体ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户ID',
  `stat_date` date DEFAULT NULL COMMENT '统计日期',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型名称',
  `total_requests` int DEFAULT '0' COMMENT '请求次数',
  `total_input_tokens` bigint DEFAULT '0' COMMENT '总输入token',
  `total_output_tokens` bigint DEFAULT '0' COMMENT '总输出token',
  `total_tokens` bigint DEFAULT '0' COMMENT '总token',
  `total_cost` decimal(12,6) DEFAULT NULL COMMENT '总费用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_agent_user_date_model` (`agent_id`,`user_id`,`stat_date`,`model_name`) USING BTREE,
  KEY `idx_stat_date` (`stat_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='Token使用统计汇总表';


-- ==============================================================================
-- 收尾：恢复会话变量
-- ==============================================================================
SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
SET SQL_NOTES = @OLD_SQL_NOTES;

-- 本部分结束。请继续执行：zyg_ddl_02_biz_job.sql