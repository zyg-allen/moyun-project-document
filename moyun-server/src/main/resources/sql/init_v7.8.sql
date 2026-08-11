-- =====================================================================
-- 墨韵智库 v7.8 · 数据库初始化脚本（整合版）
-- =====================================================================
-- 用途：空库一键初始化（建表 + 字段补齐 + 基础数据）
-- 适配：MySQL 8.0+（utf8mb4 / utf8mb4_0900_ai_ci）
-- 执行方式：
--   mysql -uroot -p<密码> <数据库名> < init_v7.8.sql
-- 内容：
--   1. 设置段（关闭 FK 检查）
--   2. DDL 建表（CREATE TABLE IF NOT EXISTS，幂等）
--   3. 字段补齐（ALTER TABLE，幂等，兼容 MySQL 8.0）
--   4. 基础数据（用户/角色/菜单/栏目/标签/字典/部门/岗位，不含业务测试数据）
--   5. 校验段
--   6. 结尾设置（恢复 FK 检查）
-- 保留的基础数据：
--   - 系统：sys_config/sys_dept/sys_dict_type/sys_dict_data/sys_post/sys_role/
--           sys_user/sys_role_dept/sys_role_menu/sys_user_post/sys_user_role/sys_job
--   - 门户：portal_category(50栏目)/portal_tag(28标签)/portal_friend_link(3友链)/
--           portal_growth_rule(30成长规则)/portal_help_category(4帮助分类)/
--           portal_interview_category(5面试分类)/portal_achievement(23成就)/
--           portal_interview_position(3岗位)/portal_task(7任务)
--   - 菜单：sys_menu（RuoYi+CMS+消息中心+v7.7~v7.24 菜单注册）
-- 删除的业务测试数据：
--   portal_book/portal_book_chapter/portal_book_quote/portal_book_recommend/
--   portal_help_article/portal_shop_item/portal_user/portal_writing_prompt
-- 访问信息：
--   后台管理：http://localhost:80   账号 admin / admin123
--   前台门户：http://localhost:5173  账号 admin / 123456
-- =====================================================================

-- 统一会话字符集与排序规则，避免 DATABASE() 返回值（库定义 collation）
-- 与字符串字面量（连接 collation）在 CONCAT/比较时报 1271
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SELECT '================================================' AS info;
SELECT '墨韵智库 v7.8 数据库初始化开始' AS info;
SELECT CONCAT('数据库: ', CONVERT(DATABASE() USING utf8mb4) COLLATE utf8mb4_0900_ai_ci, ' | 时间: ', NOW()) AS info;
SELECT '================================================' AS info;


-- =====================================================================
-- 一、设置段：关闭外键检查
-- =====================================================================
-- 墨韵智库全库 DDL 初始化脚本（拆分版）
-- 源文件：all-db-ddl.sql
-- 用途：空库初始化，按编号顺序执行
SET FOREIGN_KEY_CHECKS=0;

-- =====================================================================
-- 二、DDL 建表段（CREATE TABLE IF NOT EXISTS，幂等）
-- =====================================================================


-- ---------------------------------------------------------------
-- 来源: 10_基础系统表.sql
-- ---------------------------------------------------------------
-- v6.1 合并：原 60_基础系统表 + 61_系统关联表_菜单与角色（合并为一个文件，减少分散）
-- 来源：all-db-ddl.sql 行4031-4447 + 行5044-5323 + 行4984-4998
-- 用途：基础系统表 DDL（sys_config / sys_dept / sys_dict_* / sys_menu / sys_role / sys_role_menu / sys_user* 等 24 张表）
-- 注意：INSERT 种子数据见 80 段；sys_menu INSERT 见 90/91 段

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
                              `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
                              `config_name` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数名称',
                              `config_key` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数键名',
                              `config_value` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数键值',
                              `config_type` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
                              `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                              `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                              `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                              `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                              `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                              `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                              PRIMARY KEY (`config_id`),
                              KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';


-- Table structure for table `sys_dept`


DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
                            `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
                            `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
                            `ancestors` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '祖级列表',
                            `dept_name` varchar(30) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '部门名称',
                            `order_num` int DEFAULT '0' COMMENT '显示顺序',
                            `leader` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '负责人',
                            `phone` varchar(11) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
                            `email` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
                            `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
                            `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                            `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';


-- Table structure for table `sys_dict_data`


DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
                                 `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
                                 `dict_sort` int DEFAULT '0' COMMENT '字典排序',
                                 `dict_label` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典标签',
                                 `dict_value` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典键值',
                                 `dict_type` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
                                 `css_class` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
                                 `list_class` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表格回显样式',
                                 `is_default` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
                                 `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                 `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`dict_code`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';

-- Table structure for table `sys_dict_type`


DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
                                 `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
                                 `dict_name` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典名称',
                                 `dict_type` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
                                 `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                 `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`dict_id`),
                                 UNIQUE KEY `uk_dict_type` (`dict_type`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';


-- Table structure for table `sys_file`


DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
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


-- Table structure for table `sys_job`


DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job` (
                           `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
                           `job_name` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '任务名称',
                           `job_group` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
                           `invoke_target` varchar(500) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
                           `cron_expression` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT 'cron执行表达式',
                           `misfire_policy` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
                           `concurrent` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
                           `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1暂停）',
                           `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                           `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                           `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                           `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                           `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '备注信息',
                           `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                           PRIMARY KEY (`job_id`,`job_name`,`job_group`),
                           KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度表';


-- Table structure for table `sys_job_log`


DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log` (
                               `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
                               `job_name` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
                               `job_group` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务组名',
                               `invoke_target` varchar(500) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
                               `job_message` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '日志信息',
                               `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
                               `exception_info` varchar(2000) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '异常信息',
                               `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                               `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                               `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                               `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                               PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度日志表';


-- Table structure for table `sys_logininfor`


DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor` (
                                  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
                                  `user_name` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '用户账号',
                                  `ipaddr` varchar(128) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '登录IP地址',
                                  `login_location` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '登录地点',
                                  `browser` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '浏览器类型',
                                  `os` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作系统',
                                  `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
                                  `user_type` varchar(10) COLLATE utf8mb4_0900_ai_ci DEFAULT 'sys' COMMENT '登录来源类型（sys=后台用户 portal=门户用户）',
                                  `msg` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '提示消息',
                                  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
                                  `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                  PRIMARY KEY (`info_id`),
                                  KEY `idx_sys_logininfor_s` (`status`),
                                  KEY `idx_sys_logininfor_lt` (`login_time`),
                                  KEY `idx_sys_logininfor_ut` (`user_type`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统访问记录';


-- Table structure for table `sys_menu`


DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
                            `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
                            `menu_name` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
                            `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
                            `order_num` int DEFAULT '0' COMMENT '显示顺序',
                            `path` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '路由地址',
                            `component` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '组件路径',
                            `query` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '路由参数',
                            `route_name` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '路由名称',
                            `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
                            `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
                            `menu_type` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
                            `visible` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
                            `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
                            `perms` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '权限标识',
                            `icon` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '#' COMMENT '菜单图标',
                            `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '备注',
                            `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                            PRIMARY KEY (`menu_id`),
                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=2229 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';
--
-- Table structure for table `sys_notice_bak`
--

DROP TABLE IF EXISTS `sys_notice_bak`;
CREATE TABLE `sys_notice_bak` (
                                  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
                                  `notice_title` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
                                  `notice_type` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
                                  `notice_content` longblob COMMENT '公告内容',
                                  `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
                                  `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                  `remark` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';

--
-- Table structure for table `sys_notification`
--

DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
                                    `type` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：system/comment/like/follow/order/notice/announcement',
                                    `title` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '通知标题',
                                    `content` text COLLATE utf8mb4_0900_ai_ci COMMENT '通知内容',
                                    `data` json DEFAULT NULL COMMENT '通知数据（JSON格式）',
                                    `scope` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'user' COMMENT '范围：user=个人通知 / all=全局广播',
                                    `user_id` bigint DEFAULT NULL COMMENT '接收用户ID（scope=user 时必填，scope=all 时为 NULL）',
                                    `user_type` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '接收用户类型：portal=门户用户 / sys=系统用户（scope=user 时生效）',
                                    `notice_type` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '通知/公告分类：1=通知 / 2=公告（兼容 sys_notice 字典 sys_notice_type）',
                                    `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0=正常 / 1=关闭（兼容 sys_notice 字典 sys_notice_status）',
                                    `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                    `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_type` (`type`),
                                    KEY `idx_scope` (`scope`),
                                    KEY `idx_user_id` (`user_id`),
                                    KEY `idx_status` (`status`),
                                    KEY `idx_create_time` (`create_time`),
                                    KEY `idx_user_type_user_id` (`user_type`,`user_id`),
                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知主体表（合并 portal_notification + sys_notice）';


--
-- Table structure for table `sys_notification_read`
--

DROP TABLE IF EXISTS `sys_notification_read`;
CREATE TABLE `sys_notification_read` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `notification_id` bigint NOT NULL COMMENT '通知ID（关联 sys_notification.id）',
                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                         `user_type` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '已读用户类型：portal=门户用户 / sys=系统用户',
                                         `read_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_notif_user_type` (`notification_id`,`user_id`,`user_type`),
                                         KEY `idx_user_id` (`user_id`),
                                         KEY `idx_notification_id` (`notification_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知用户已读关系表';


--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
                                `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
                                `title` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '模块标题',
                                `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
                                `method` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '方法名称',
                                `request_method` varchar(10) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求方式',
                                `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
                                `oper_name` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作人员',
                                `dept_name` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '部门名称',
                                `oper_url` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求URL',
                                `oper_ip` varchar(128) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '主机地址',
                                `oper_location` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作地点',
                                `oper_param` varchar(2000) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求参数',
                                `json_result` varchar(2000) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '返回参数',
                                `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
                                `error_msg` varchar(2000) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '错误消息',
                                `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
                                `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
                                `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                PRIMARY KEY (`oper_id`),
                                KEY `idx_sys_oper_log_bt` (`business_type`),
                                KEY `idx_sys_oper_log_s` (`status`),
                                KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志记录';


--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
                            `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
                            `post_code` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
                            `post_name` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
                            `post_sort` int NOT NULL COMMENT '显示顺序',
                            `status` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态（0正常 1停用）',
                            `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                            `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                            PRIMARY KEY (`post_id`),
                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息表';

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
                            `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                            `role_name` varchar(30) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
                            `role_key` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符串',
                            `role_sort` int NOT NULL COMMENT '显示顺序',
                            `data_scope` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
                            `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
                            `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
                            `status` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
                            `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                            `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色信息表';

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
                                 `role_id` bigint NOT NULL COMMENT '角色ID',
                                 `dept_id` bigint NOT NULL COMMENT '部门ID',
                                 `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和部门关联表';


--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                            `user_name` varchar(30) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
                            `nick_name` varchar(30) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
                            `user_type` varchar(2) COLLATE utf8mb4_0900_ai_ci DEFAULT '00' COMMENT '用户类型（00系统用户）',
                            `email` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '用户邮箱',
                            `phonenumber` varchar(11) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '手机号码',
                            `sex` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
                            `avatar` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '头像地址',
                            `password` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '密码',
                            `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
                            `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                            `login_ip` varchar(128) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '最后登录IP',
                            `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
                            `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';


-- Table structure for table `sys_user_post`


DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post` (
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `post_id` bigint NOT NULL COMMENT '岗位ID',
                                 `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与岗位关联表';

-- Table structure for table `sys_user_role`


DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `role_id` bigint NOT NULL COMMENT '角色ID',
                                 `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户和角色关联表';
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



-- ---------------------------------------------------------------
-- 来源: 30_定时任务表_qrtz.sql
-- ---------------------------------------------------------------
-- 来源：all-db-ddl.sql 行3838-4030
-- 用途：Quartz 全部 11 张 qrtz_* 表 DDL


DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers` (
                                      `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                                      `trigger_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                      `trigger_group` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                      `blob_data` blob COMMENT '存放持久化Trigger对象',
                                      PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                      CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Blob类型的触发器表';


--
-- Table structure for table `qrtz_calendars`
--

DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars` (
                                  `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                                  `calendar_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日历名称',
                                  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
                                  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日历信息表';


--
-- Table structure for table `qrtz_cron_triggers`
--

DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers` (
                                      `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                                      `trigger_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                      `trigger_group` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                      `cron_expression` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'cron表达式',
                                      `time_zone_id` varchar(80) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '时区',
                                      PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                      CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cron类型的触发器表';


--
-- Table structure for table `qrtz_fired_triggers`
--

DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers` (
                                       `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                                       `entry_id` varchar(95) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例id',
                                       `trigger_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                       `trigger_group` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                       `instance_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例名',
                                       `fired_time` bigint NOT NULL COMMENT '触发的时间',
                                       `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
                                       `priority` int NOT NULL COMMENT '优先级',
                                       `state` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态',
                                       `job_name` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任务名称',
                                       `job_group` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任务组名',
                                       `is_nonconcurrent` varchar(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否并发',
                                       `requests_recovery` varchar(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否接受恢复执行',
                                       PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已触发的触发器表';


--
-- Table structure for table `qrtz_job_details`
--

DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details` (
                                    `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                                    `job_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
                                    `job_group` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务组名',
                                    `description` varchar(250) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '相关介绍',
                                    `job_class_name` varchar(250) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行任务类名称',
                                    `is_durable` varchar(1) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否持久化',
                                    `is_nonconcurrent` varchar(1) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否并发',
                                    `is_update_data` varchar(1) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否更新数据',
                                    `requests_recovery` varchar(1) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否接受恢复执行',
                                    `job_data` blob COMMENT '存放持久化job对象',
                                    PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务详细信息表';


--
-- Table structure for table `qrtz_locks`
--

DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks` (
                              `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                              `lock_name` varchar(40) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '悲观锁名称',
                              PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储的悲观锁信息表';


--
-- Table structure for table `qrtz_paused_trigger_grps`
--

DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps` (
                                            `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                                            `trigger_group` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                            PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='暂停的触发器表';


--
-- Table structure for table `qrtz_scheduler_state`
--

DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state` (
                                        `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                                        `instance_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实例名称',
                                        `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
                                        `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
                                        PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调度器状态表';


--
-- Table structure for table `qrtz_simple_triggers`
--

DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers` (
                                        `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                                        `trigger_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                        `trigger_group` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                        `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
                                        `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
                                        `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
                                        PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                        CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简单触发器的信息表';


-- Table structure for table `qrtz_simprop_triggers`


DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers` (
                                         `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                                         `trigger_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                         `trigger_group` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                         `str_prop_1` varchar(512) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
                                         `str_prop_2` varchar(512) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
                                         `str_prop_3` varchar(512) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
                                         `int_prop_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
                                         `int_prop_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
                                         `long_prop_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
                                         `long_prop_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
                                         `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
                                         `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
                                         `bool_prop_1` varchar(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
                                         `bool_prop_2` varchar(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
                                         PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                         CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同步机制的行锁表';


-- Table structure for table `qrtz_triggers`


DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers` (
                                 `sched_name` varchar(120) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
                                 `trigger_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的名字',
                                 `trigger_group` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器所属组的名字',
                                 `job_name` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
                                 `job_group` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
                                 `description` varchar(250) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '相关介绍',
                                 `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
                                 `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
                                 `priority` int DEFAULT NULL COMMENT '优先级',
                                 `trigger_state` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器状态',
                                 `trigger_type` varchar(8) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的类型',
                                 `start_time` bigint NOT NULL COMMENT '开始时间',
                                 `end_time` bigint DEFAULT NULL COMMENT '结束时间',
                                 `calendar_name` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '日程表名称',
                                 `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
                                 `job_data` blob COMMENT '存放持久化job对象',
                                 PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                 KEY `sched_name` (`sched_name`,`job_name`,`job_group`),
                                 CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='触发器详细信息表';


-- Table structure for table `sys_config`




-- ---------------------------------------------------------------
-- 来源: 40_代码生成表_gen.sql
-- ---------------------------------------------------------------
-- 来源：all-db-ddl.sql 行1161-1224
-- 用途：代码生成业务表（gen_table + gen_table_column）DDL

DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table` (
                             `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                             `table_name` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '表名称',
                             `table_comment` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '表描述',
                             `sub_table_name` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联子表的表名',
                             `sub_table_fk_name` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '子表关联的外键名',
                             `class_name` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '实体类名称',
                             `tpl_category` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
                             `tpl_web_type` varchar(30) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
                             `package_name` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成包路径',
                             `module_name` varchar(30) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成模块名',
                             `business_name` varchar(30) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成业务名',
                             `function_name` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成功能名',
                             `function_author` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成功能作者',
                             `gen_type` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
                             `gen_path` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
                             `options` varchar(1000) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '其它生成选项',
                             `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                             `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                             `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                             PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表';


--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column` (
                                    `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                                    `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
                                    `column_name` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列名称',
                                    `column_comment` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列描述',
                                    `column_type` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列类型',
                                    `java_type` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'JAVA类型',
                                    `java_field` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'JAVA字段名',
                                    `is_pk` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否主键（1是）',
                                    `is_increment` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否自增（1是）',
                                    `is_required` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否必填（1是）',
                                    `is_insert` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为插入字段（1是）',
                                    `is_edit` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否编辑字段（1是）',
                                    `is_list` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否列表字段（1是）',
                                    `is_query` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否查询字段（1是）',
                                    `query_type` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
                                    `html_type` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
                                    `dict_type` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
                                    `sort` int DEFAULT NULL COMMENT '排序',
                                    `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                    `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                    `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                    PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表字段';


--
-- Table structure for table `portal_achievement`
--



-- ---------------------------------------------------------------
-- 来源: 50_门户核心表.sql
-- ---------------------------------------------------------------
-- 来源：all-db-ddl.sql 行1225-1853（已剔除 INSERT 种子数据，种子数据见 80 段）
-- 用途：门户核心表 DDL（portal_achievement / portal_ad_slot / portal_article* / portal_book* / portal_bookmark / portal_bookshelf / portal_category / portal_tag）

DROP TABLE IF EXISTS `portal_achievement`;
CREATE TABLE `portal_achievement` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `code` varchar(64) NOT NULL COMMENT '成就编码',
                                      `name` varchar(100) NOT NULL COMMENT '成就名称',
                                      `description` varchar(255) DEFAULT NULL COMMENT '成就描述',
                                      `icon` varchar(500) DEFAULT NULL COMMENT '图标URL',
                                      `module` varchar(32) DEFAULT NULL COMMENT '所属模块: article/reading/interview/all',
                                      `condition_json` text COMMENT '达成条件JSON',
                                      `growth_reward` int DEFAULT '0' COMMENT '达成奖励成长值',
                                      `sort` int DEFAULT '0' COMMENT '排序',
                                      `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
                                      `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成就定义表';

--
-- Table structure for table `portal_ad_slot`
--

DROP TABLE IF EXISTS `portal_ad_slot`;
CREATE TABLE `portal_ad_slot` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '广告位ID',
                                  `slot_key` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '广告位标识，如 article_detail_bottom',
                                  `title` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '广告标题',
                                  `image` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '广告图片URL',
                                  `link` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '点击跳转链接',
                                  `content` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '广告文案',
                                  `sort` int DEFAULT '0' COMMENT '排序',
                                  `status` varchar(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0=启用 1=停用',
                                  `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                  `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_slot_key` (`slot_key`),
                                  KEY `idx_status` (`status`),
                                  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户自研广告位表';


--
-- Table structure for table `portal_article`
--

DROP TABLE IF EXISTS `portal_article`;
CREATE TABLE `portal_article` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
                                  `title` varchar(500) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文章标题',
                                  `slug` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文章URL别名，用于SEO语义化路径',
                                  `content` longtext COLLATE utf8mb4_0900_ai_ci COMMENT '文章内容（HTML格式）',
                                  `excerpt` varchar(1000) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文章摘要',
                                  `cover` text COLLATE utf8mb4_0900_ai_ci COMMENT '封面图片URL或Base64',
                                  `author_id` bigint NOT NULL COMMENT '作者ID（门户用户ID）',
                                  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
                                  `root_category_id` bigint DEFAULT NULL COMMENT '顶级分类ID',
                                  `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'draft' COMMENT '状态：draft=草稿 / pending=待审核 / published=已发布 / rejected=已拒绝 / archived=已归档',
                                  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选',
                                  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
                                  `is_carousel` tinyint(1) DEFAULT '0' COMMENT '是否轮播',
                                  `is_category_recommended` tinyint(1) DEFAULT '0' COMMENT '是否栏目推荐',
                                  `views` bigint DEFAULT '0' COMMENT '浏览量',
                                  `likes` bigint DEFAULT '0' COMMENT '点赞数',
                                  `comments` bigint DEFAULT '0' COMMENT '评论数',
                                  `share_count` bigint DEFAULT '0' COMMENT '分享数',
                                  `bookmark_count` bigint DEFAULT '0' COMMENT '收藏数',
                                  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
                                  `link` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '外部链接',
                                  `editor_mode` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
                                  `session_token` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '编辑会话标识（一次编辑会话唯一，用于草稿/发布幂等去重）',
                                  `content_markdown` text COLLATE utf8mb4_0900_ai_ci COMMENT 'Markdown 原始内容',
                                  `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                  `category_path` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类路径，包含所有祖先ID，例如：1,3,5',
                                  `is_paid` tinyint NOT NULL DEFAULT '0' COMMENT '是否付费阅读 0=免费 1=付费',
                                  `paid_content` longtext COLLATE utf8mb4_0900_ai_ci COMMENT '付费内容（购买后可见）',
                                  `preview_length` int NOT NULL DEFAULT '0' COMMENT '试读字数（未购买可预览的字数）',
                                  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '付费价格，0=免费',
                                  `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_slug` (`slug`),
                                  KEY `idx_author_id` (`author_id`),
                                  KEY `idx_category_id` (`category_id`),
                                  KEY `idx_status` (`status`),
                                  KEY `idx_is_featured` (`is_featured`),
                                  KEY `idx_is_top` (`is_top`),
                                  KEY `idx_published_at` (`published_at`),
                                  KEY `idx_views` (`views`),
                                  KEY `idx_likes` (`likes`),
                                  KEY `idx_is_category_recommended` (`is_category_recommended`),
                                  KEY `idx_root_category_id` (`root_category_id`),
                                  KEY `idx_category_path` (`category_path`(100)),
                                  KEY `idx_session_token` (`session_token`),
                                  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户文章表';


-- [v7.8 已删除] portal_article_tag 表（历史遗留老表）
-- 实际标签关联走通用表 portal_entity_tag（entity_type='article'），见下方 portal_entity_tag 建表
-- 相关代码：PortalTagServiceImpl.bindTags() / PortalArticleController / CmsArticleServiceImpl


--
-- Table structure for table `portal_article_version`
--

DROP TABLE IF EXISTS `portal_article_version`;
CREATE TABLE `portal_article_version` (
                                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `article_id` bigint NOT NULL COMMENT '文章ID',
                                          `version_no` int NOT NULL COMMENT '版本号（同一文章内自增）',
                                          `title` varchar(256) NOT NULL COMMENT '版本标题快照',
                                          `content` longtext COMMENT '版本内容快照（HTML）',
                                          `content_markdown` longtext COMMENT '版本 Markdown 原始内容快照',
                                          `excerpt` varchar(500) DEFAULT NULL COMMENT '版本摘要快照',
                                          `operator_id` bigint DEFAULT NULL COMMENT '操作人ID（保存/回滚的执行者）',
                                          `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
                                          PRIMARY KEY (`id`),
                                          KEY `idx_article_version` (`article_id`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章版本快照';


--
-- Table structure for table `portal_article_view`
--

DROP TABLE IF EXISTS `portal_article_view`;
CREATE TABLE `portal_article_view` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                       `article_id` bigint NOT NULL COMMENT '文章ID',
                                       `user_id` bigint DEFAULT NULL COMMENT '用户ID（NULL表示游客）',
                                       `ip` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'IP地址',
                                       `view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
                                       `user_agent` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '浏览器User-Agent',
                                       `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_article_id` (`article_id`),
                                       KEY `idx_user_id` (`user_id`),
                                       KEY `idx_ip` (`ip`),
                                       KEY `idx_view_time` (`view_time`),
                                       KEY `idx_article_user` (`article_id`,`user_id`),
                                       KEY `idx_article_ip` (`article_id`,`ip`),
                                       KEY `idx_article_viewtime` (`article_id`,`view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章浏览记录表';


--
-- Table structure for table `portal_book`
--

DROP TABLE IF EXISTS `portal_book`;
CREATE TABLE `portal_book` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `title` varchar(500) NOT NULL COMMENT '书名',
                               `author` varchar(200) NOT NULL COMMENT '作者',
                               `cover` varchar(500) DEFAULT NULL COMMENT '封面URL',
                               `description` text COMMENT '简介',
                               `isbn` varchar(50) DEFAULT NULL COMMENT 'ISBN',
                               `publisher` varchar(200) DEFAULT NULL COMMENT '出版社',
                               `publish_date` date DEFAULT NULL COMMENT '出版日期',
                               `page_count` int DEFAULT '0' COMMENT '页数',
                               `category_id` bigint DEFAULT NULL COMMENT '分类ID',
                               `tags` varchar(500) DEFAULT NULL COMMENT '标签，逗号分隔',
                               `rating` decimal(3,2) DEFAULT '0.00' COMMENT '评分',
                               `reading_count` bigint DEFAULT '0' COMMENT '阅读人数',
                               `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                               `type` varchar(20) DEFAULT 'published' COMMENT '书籍类型：published=出版物，novel=网络小说，longform=长文',
                               `serial_status` varchar(20) DEFAULT 'completed' COMMENT '连载状态：ongoing=连载中，completed=已完结，hiatus=暂停更新',
                               `word_count` bigint DEFAULT '0' COMMENT '总字数（章节字数之和）',
                               `chapter_count` int DEFAULT '0' COMMENT '总章节数',
                               `latest_chapter_id` bigint DEFAULT NULL COMMENT '最新章节ID（用于追更展示）',
                               `latest_chapter_title` varchar(500) DEFAULT NULL COMMENT '最新章节标题',
                               `last_update_time` datetime DEFAULT NULL COMMENT '最后更新时间（章节发布时同步）',
                               `is_finished` tinyint(1) DEFAULT '1' COMMENT '是否完结：1=完结，0=连载中（冗余字段，便于查询）',
                               `access_level` varchar(20) DEFAULT 'free' COMMENT '访问级别:free,vip,preview',
                               `preview_ratio` int DEFAULT '30' COMMENT '免费试读比例（0-100）',
                               `price` decimal(10,2) DEFAULT '0.00' COMMENT '书籍单价（元）',
                               `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选',
                               `is_recommended` tinyint(1) DEFAULT '0' COMMENT '是否推荐',
                               `summary` text COMMENT '简介（纯文本）',
                               `author_bio` text COMMENT '作者简介',
                               `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                               `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                               PRIMARY KEY (`id`),
                               KEY `idx_category_id` (`category_id`),
                               KEY `idx_status` (`status`),
                               KEY `idx_title` (`title`),
                               KEY `idx_access_level` (`access_level`),
                               KEY `idx_is_featured` (`is_featured`),
                               KEY `idx_is_recommended` (`is_recommended`),
                               KEY `idx_type` (`type`),
                               KEY `idx_serial_status` (`serial_status`),
                               KEY `idx_is_finished` (`is_finished`),
                               KEY `idx_word_count` (`word_count`),
                               KEY `idx_last_update_time` (`last_update_time`),
                               KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍表';

--
-- Table structure for table `portal_book_chapter`
--

DROP TABLE IF EXISTS `portal_book_chapter`;
CREATE TABLE `portal_book_chapter` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `book_id` bigint NOT NULL COMMENT '所属书籍ID',
                                       `title` varchar(500) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '章节标题',
                                       `content` longtext COLLATE utf8mb4_0900_ai_ci COMMENT '章节正文（HTML格式，上限4GB）',
                                       `content_markdown` text COLLATE utf8mb4_0900_ai_ci COMMENT 'Markdown原始内容（上限64KB，单章足够）',
                                       `editor_mode` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
                                       `word_count` int DEFAULT '0' COMMENT '字数统计',
                                       `chapter_no` int NOT NULL DEFAULT '0' COMMENT '章节序号（用于排序，从1开始）',
                                       `volume_id` bigint DEFAULT NULL COMMENT '所属分卷ID（可选，支持分卷管理）',
                                       `is_free` tinyint(1) DEFAULT '1' COMMENT '是否免费：1=免费，0=VIP章节',
                                       `price` decimal(10,2) DEFAULT '0.00' COMMENT '章节单价（元，VIP章节购买）',
                                       `is_published` tinyint(1) DEFAULT '0' COMMENT '是否已发布：0=草稿，1=已发布',
                                       `publish_time` datetime DEFAULT NULL COMMENT '发布时间（支持定时发布）',
                                       `view_count` bigint DEFAULT '0' COMMENT '章节浏览量',
                                       `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                       `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_book_chapter_no` (`book_id`,`chapter_no`),
                                       KEY `idx_book_id` (`book_id`),
                                       KEY `idx_publish_time` (`publish_time`),
                                       KEY `idx_is_published` (`is_published`),
                                       KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍章节表';

--
-- Table structure for table `portal_book_chapter_view`
--

DROP TABLE IF EXISTS `portal_book_chapter_view`;
CREATE TABLE `portal_book_chapter_view` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `chapter_id` bigint NOT NULL COMMENT '章节ID',
                                            `book_id` bigint NOT NULL COMMENT '书籍ID',
                                            `user_id` bigint DEFAULT NULL COMMENT '用户ID（未登录为NULL）',
                                            `client_ip` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端IP',
                                            `read_duration_ms` int DEFAULT '0' COMMENT '阅读时长（毫秒）',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
                                            `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            KEY `idx_chapter_id` (`chapter_id`),
                                            KEY `idx_user_id` (`user_id`),
                                            KEY `idx_create_time` (`create_time`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='章节浏览记录表';


--
-- Table structure for table `portal_book_list`
--

DROP TABLE IF EXISTS `portal_book_list`;
CREATE TABLE `portal_book_list` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `title` varchar(500) NOT NULL COMMENT '书单标题',
                                    `description` text COMMENT '书单简介',
                                    `cover` varchar(500) DEFAULT NULL COMMENT '封面URL',
                                    `user_id` bigint NOT NULL COMMENT '创建者ID',
                                    `category_id` bigint DEFAULT NULL COMMENT '分类ID',
                                    `is_public` tinyint(1) DEFAULT '1' COMMENT '是否公开',
                                    `book_count` int DEFAULT '0' COMMENT '书籍数量',
                                    `view_count` bigint DEFAULT '0' COMMENT '浏览数',
                                    `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                    `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                                    `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选',
                                    `access_level` varchar(20) DEFAULT 'free' COMMENT '访问级别:free,vip',
                                    `tags` varchar(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_user_id` (`user_id`),
                                    KEY `idx_category_id` (`category_id`),
                                    KEY `idx_status` (`status`),
                                    KEY `idx_is_featured` (`is_featured`),
                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书单表';


--
-- Table structure for table `portal_book_list_bookmark`
--

DROP TABLE IF EXISTS `portal_book_list_bookmark`;
CREATE TABLE `portal_book_list_bookmark` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `booklist_id` bigint NOT NULL COMMENT '书单ID',
                                             `user_id` bigint NOT NULL COMMENT '用户ID',
                                             `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                                             `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_booklist_user` (`booklist_id`,`user_id`),
                                             KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书单收藏表';


--
-- Table structure for table `portal_book_list_item`
--

DROP TABLE IF EXISTS `portal_book_list_item`;
CREATE TABLE `portal_book_list_item` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `book_list_id` bigint NOT NULL COMMENT '书单ID',
                                         `book_id` bigint NOT NULL COMMENT '书籍ID',
                                         `sort` int DEFAULT '0' COMMENT '排序',
                                         `note` text COMMENT '添加说明',
                                         `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
                                         `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                         PRIMARY KEY (`id`),
                                         KEY `idx_book_list_id` (`book_list_id`),
                                         KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书单-书籍关联表';


--
-- Table structure for table `portal_book_list_like`
--

DROP TABLE IF EXISTS `portal_book_list_like`;
CREATE TABLE `portal_book_list_like` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `book_list_id` bigint NOT NULL COMMENT '书单ID',
                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                         `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                         `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_list_user` (`book_list_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书单点赞表';


--
-- Table structure for table `portal_book_quote`
--

DROP TABLE IF EXISTS `portal_book_quote`;
CREATE TABLE `portal_book_quote` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                     `book_id` bigint NOT NULL COMMENT '书籍ID',
                                     `chapter_id` bigint DEFAULT NULL COMMENT '章节ID（关联 portal_book_chapter）',
                                     `content` text NOT NULL COMMENT '金句内容',
                                     `page` varchar(100) DEFAULT NULL COMMENT '页码',
                                     `chapter` varchar(200) DEFAULT NULL COMMENT '章节',
                                     `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                     `is_public` tinyint(1) DEFAULT '1' COMMENT '是否公开',
                                     `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选',
                                     `location` varchar(200) DEFAULT NULL COMMENT '章节标题/位置描述',
                                     `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                     `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_id` (`user_id`),
                                     KEY `idx_book_id` (`book_id`),
                                     KEY `idx_is_public` (`is_public`),
                                     KEY `idx_is_featured` (`is_featured`),
                                     KEY `idx_chapter_id` (`chapter_id`),
                                     KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='金句摘录表';

--
-- Table structure for table `portal_book_quote_like`
--

DROP TABLE IF EXISTS `portal_book_quote_like`;
CREATE TABLE `portal_book_quote_like` (
                                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `quote_id` bigint NOT NULL COMMENT '金句ID',
                                          `user_id` bigint NOT NULL COMMENT '用户ID',
                                          `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                          `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                          `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                          `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_quote_user` (`quote_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='金句点赞表';


--
-- Table structure for table `portal_book_recommend`
--

DROP TABLE IF EXISTS `portal_book_recommend`;
CREATE TABLE `portal_book_recommend` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `book_id` bigint NOT NULL COMMENT '书籍ID',
                                         `position` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推荐位置：home_banner=首页轮播 / home_hot=首页热门 / category_top=分类顶推 / limit_free=限免专区 / discover_banner=发现页轮播',
                                         `sort` int DEFAULT '0' COMMENT '排序（越小越靠前）',
                                         `start_time` datetime DEFAULT NULL COMMENT '推荐开始时间（NULL 表示立即生效）',
                                         `end_time` datetime DEFAULT NULL COMMENT '推荐结束时间（NULL 表示长期有效）',
                                         `is_active` tinyint(1) DEFAULT '1' COMMENT '是否生效：1=生效，0=下架',
                                         `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注（运营说明）',
                                         `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_book_position` (`book_id`,`position`),
                                         KEY `idx_position` (`position`),
                                         KEY `idx_is_active` (`is_active`),
                                         KEY `idx_time_window` (`start_time`,`end_time`),
                                         KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍推荐位表';

--
-- Table structure for table `portal_bookmark`
--

DROP TABLE IF EXISTS `portal_bookmark`;
CREATE TABLE `portal_bookmark` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
                                   `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                   `article_id` bigint NOT NULL COMMENT '文章ID',
                                   `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                   `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户收藏表';


--
-- Table structure for table `portal_bookshelf`
--

DROP TABLE IF EXISTS `portal_bookshelf`;
CREATE TABLE `portal_bookshelf` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                    `book_id` bigint NOT NULL COMMENT '书籍ID',
                                    `last_chapter_id` bigint DEFAULT NULL COMMENT '最后阅读章节ID（冗余，用于续读）',
                                    `last_chapter_no` int DEFAULT '0' COMMENT '最后阅读章节序号',
                                    `sort` int DEFAULT '0' COMMENT '排序（用户自定义书架顺序，越大越靠前）',
                                    `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                                    `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                    `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_user_book` (`user_id`,`book_id`),
                                    KEY `idx_user_id` (`user_id`),
                                    KEY `idx_book_id` (`book_id`),
                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户书架（收藏书籍）表';


--
-- Table structure for table `portal_category`
--

DROP TABLE IF EXISTS `portal_category`;
CREATE TABLE `portal_category` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
                                   `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
                                   `slug` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类别名',
                                   `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类描述',
                                   `icon` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标URL',
                                   `sort` int DEFAULT '0' COMMENT '排序',
                                   `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
                                   `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                   `show_in_nav` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在头部栏目展示（0否/1是）',
                                   `nav_route_type` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'category' COMMENT '路由类型（home/category/static/external）',
                                   `nav_route_path` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '静态/外链路由路径（仅 static/external 类型使用）',
                                   `category_type` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'article' COMMENT '栏目内容类型（article=文章栏目可发布文章 special=特殊页面不发布文章）',
                                   `requires_auth` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要登录（0否/1是）',
                                   `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                   `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                   `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_parent_id` (`parent_id`),
                                   KEY `idx_slug` (`slug`),
                                   KEY `idx_show_in_nav` (`show_in_nav`),
                                   KEY `idx_category_type` (`category_type`),
                                   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户分类表';

--
-- Table structure for table `portal_tag`
--

DROP TABLE IF EXISTS `portal_tag`;
CREATE TABLE `portal_tag` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
                              `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
                              `slug` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标签别名',
                              `sort` int DEFAULT '0' COMMENT '排序',
                              `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                              `module` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属模块（article/interview_question/interview_experience/interview_resume_template 等，null 表示通用）',
                              `reference_count` bigint unsigned DEFAULT '0' COMMENT '被引用次数（冗余计数列，绑定/解绑时同步维护）',
                              `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                              `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_name` (`name`),
                              KEY `idx_slug` (`slug`),
                              KEY `idx_module` (`module`),
                              KEY `idx_reference_count` (`reference_count` DESC),
                              KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户标签表';


-- ---------------------------------------------------------------
-- 来源: 51_门户扩展表.sql
-- ---------------------------------------------------------------
-- 来源：all-db-ddl.sql 行1948-2471（已剔除 INSERT 种子数据，种子数据见 80 段）
-- 用途：门户扩展表 DDL（portal_code_run / portal_column* / portal_comment* / portal_contest* / portal_creator_* / portal_entity_tag / portal_feed_* / portal_feedback / portal_follow / portal_friend_link / portal_growth_* / portal_help_*）

DROP TABLE IF EXISTS `portal_code_run`;
CREATE TABLE `portal_code_run` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `user_id` bigint NOT NULL COMMENT '运行者用户ID',
                                   `language` varchar(16) NOT NULL COMMENT '编程语言 java/python/javascript',
                                   `code` mediumtext NOT NULL COMMENT '用户提交的源代码',
                                   `stdin` text COMMENT '标准输入内容',
                                   `output` mediumtext COMMENT '标准输出（截断至 1MB）',
                                   `error_msg` mediumtext COMMENT '错误输出 / 编译错误信息',
                                   `status` varchar(16) NOT NULL DEFAULT 'running' COMMENT '运行状态 running/success/failed/timeout',
                                   `runtime_ms` int DEFAULT NULL COMMENT '运行耗时（毫秒）',
                                   `mem_kb` int DEFAULT NULL COMMENT '内存占用（KB，粗略估算）',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码运行记录';


--
-- Table structure for table `portal_column`
--

DROP TABLE IF EXISTS `portal_column`;
CREATE TABLE `portal_column` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                 `user_id` bigint NOT NULL COMMENT '创作者',
                                 `title` varchar(128) NOT NULL COMMENT '专栏名',
                                 `subtitle` varchar(256) DEFAULT NULL COMMENT '副标题',
                                 `description` text COMMENT '专栏简介',
                                 `cover` varchar(500) DEFAULT NULL COMMENT '封面',
                                 `category_id` bigint DEFAULT NULL COMMENT '分类',
                                 `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT 'draft/published/archived',
                                 `article_count` int NOT NULL DEFAULT '0' COMMENT '文章数',
                                 `subscribe_count` int NOT NULL DEFAULT '0' COMMENT '订阅数',
                                 `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览数',
                                 `is_finished` tinyint NOT NULL DEFAULT '0' COMMENT '是否完结',
                                 `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '专栏会员价，0=免费',
                                 `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_user` (`user_id`),
                                 KEY `idx_status` (`status`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏';


--
-- Table structure for table `portal_column_article`
--

DROP TABLE IF EXISTS `portal_column_article`;
CREATE TABLE `portal_column_article` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `column_id` bigint NOT NULL COMMENT '专栏ID',
                                         `article_id` bigint NOT NULL COMMENT '文章ID',
                                         `sort_order` int NOT NULL DEFAULT '0' COMMENT '专栏内顺序',
                                         `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_column_article` (`column_id`,`article_id`),
                                         KEY `idx_column_sort` (`column_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏-文章关联';


--
-- Table structure for table `portal_column_subscribe`
--

DROP TABLE IF EXISTS `portal_column_subscribe`;
CREATE TABLE `portal_column_subscribe` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `column_id` bigint NOT NULL COMMENT '专栏ID',
                                           `user_id` bigint NOT NULL COMMENT '订阅用户ID',
                                           `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `uk_column_user` (`column_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏订阅';


--
-- Table structure for table `portal_comment`
--

DROP TABLE IF EXISTS `portal_comment`;
CREATE TABLE `portal_comment` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
                                  `article_id` bigint NOT NULL COMMENT '文章ID',
                                  `author_id` bigint NOT NULL COMMENT '评论者ID（门户用户ID）',
                                  `content` text COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
                                  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
                                  `root_id` bigint DEFAULT '0' COMMENT '根评论ID（一级评论ID）',
                                  `reply_to` bigint DEFAULT NULL COMMENT '回复的用户ID',
                                  `reply_to_content` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '被回复的内容摘要',
                                  `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                  `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                  `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                  `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_article_id` (`article_id`),
                                  KEY `idx_author_id` (`author_id`),
                                  KEY `idx_parent_id` (`parent_id`),
                                  KEY `idx_article_root` (`article_id`,`root_id`),
                                  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户评论表';


--
-- Table structure for table `portal_comment_like`
--

DROP TABLE IF EXISTS `portal_comment_like`;
CREATE TABLE `portal_comment_like` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                       `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                       `comment_id` bigint NOT NULL COMMENT '评论ID',
                                       `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_user_comment` (`user_id`,`comment_id`),
                                       KEY `idx_user_id` (`user_id`),
                                       KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户评论点赞表';


--
-- Table structure for table `portal_contest_submission`
--

DROP TABLE IF EXISTS `portal_contest_submission`;
CREATE TABLE `portal_contest_submission` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `contest_id` bigint NOT NULL COMMENT '活动ID',
                                             `user_id` bigint NOT NULL COMMENT '投稿用户ID',
                                             `article_id` bigint NOT NULL COMMENT '投稿文章ID',
                                             `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/shortlisted/eliminated/winner',
                                             `vote_count` int NOT NULL DEFAULT '0' COMMENT '投票数',
                                             `rank` int DEFAULT NULL COMMENT '排名',
                                             `remark` varchar(500) DEFAULT NULL COMMENT '备注（评审意见等）',
                                             `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_contest_user` (`contest_id`,`user_id`),
                                             UNIQUE KEY `uk_contest_article` (`contest_id`,`article_id`),
                                             KEY `idx_contest` (`contest_id`),
                                             KEY `idx_user` (`user_id`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动投稿';


--
-- Table structure for table `portal_contest_vote`
--

DROP TABLE IF EXISTS `portal_contest_vote`;
CREATE TABLE `portal_contest_vote` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `submission_id` bigint NOT NULL COMMENT '投稿ID',
                                       `user_id` bigint NOT NULL COMMENT '投票用户ID',
                                       `contest_id` bigint NOT NULL COMMENT '活动ID（冗余便于按活动统计）',
                                       `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_submission_user` (`submission_id`,`user_id`),
                                       KEY `idx_submission` (`submission_id`),
                                       KEY `idx_contest` (`contest_id`),
                                       KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动投稿投票记录';


--
-- Table structure for table `portal_creator_certification`
--

DROP TABLE IF EXISTS `portal_creator_certification`;
CREATE TABLE `portal_creator_certification` (
                                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                `user_id` bigint NOT NULL COMMENT '申请用户ID',
                                                `real_name` varchar(64) NOT NULL COMMENT '真实姓名',
                                                `cert_type` varchar(32) NOT NULL COMMENT '认证类型 identity/creator/expert',
                                                `cert_no` varchar(64) DEFAULT NULL COMMENT '证件号',
                                                `cert_image` varchar(500) DEFAULT NULL COMMENT '证件照URL（兼容字段：旧单图或新流程中的「人像面」URL 别名）',
                                                `cert_image_front` varchar(500) DEFAULT NULL COMMENT '身份证正面（人像面）URL',
                                                `cert_image_back` varchar(500) DEFAULT NULL COMMENT '身份证背面（国徽面）URL',
                                                `intro` text COMMENT '自我介绍',
                                                `works` varchar(500) DEFAULT NULL COMMENT '代表作链接',
                                                `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '审核状态 pending/approved/rejected',
                                                `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID',
                                                `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
                                                `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
                                                `audited_time` datetime DEFAULT NULL COMMENT '审核时间',
                                                `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                                PRIMARY KEY (`id`),
                                                KEY `idx_user` (`user_id`),
                                                KEY `idx_status` (`status`),
                                                KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创作者认证';


--
-- Table structure for table `portal_creator_settlement`
--

DROP TABLE IF EXISTS `portal_creator_settlement`;
CREATE TABLE `portal_creator_settlement` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `creator_id` bigint NOT NULL COMMENT '创作者用户ID',
                                             `period` varchar(16) NOT NULL COMMENT '结算周期，格式 yyyy-MM，如 2026-07',
                                             `tip_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '打赏收入（当月已支付打赏总额）',
                                             `paid_read_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '付费阅读收入（当月已支付购买总额）',
                                             `column_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '专栏订阅收入（当月已支付订阅总额）',
                                             `total_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总收入（三项之和）',
                                             `platform_fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '平台抽成（total_income * platform_fee_rate）',
                                             `creator_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '创作者实得（total_income - platform_fee）',
                                             `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '状态 pending/confirmed/paid',
                                             `paid_time` datetime DEFAULT NULL COMMENT '打款时间',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_creator_period` (`creator_id`,`period`),
                                             KEY `idx_creator` (`creator_id`),
                                             KEY `idx_period` (`period`),
                                             KEY `idx_status` (`status`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创作者分成结算';


--
-- Table structure for table `portal_entity_tag`
--

DROP TABLE IF EXISTS `portal_entity_tag`;
CREATE TABLE `portal_entity_tag` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `tag_id` bigint unsigned NOT NULL COMMENT '标签ID（引用 portal_tag.id）',
                                     `entity_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实体类型（article/interview_question/interview_experience/interview_resume_template/book 等）',
                                     `entity_id` bigint unsigned NOT NULL COMMENT '实体ID',
                                     `sort` int DEFAULT '0' COMMENT '排序',
                                     `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_tag_entity` (`tag_id`,`entity_type`,`entity_id`),
                                     KEY `idx_entity` (`entity_type`,`entity_id`),
                                     KEY `idx_entity_create` (`entity_type`,`create_time`),
                                     KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用实体标签关联表';


--
-- Table structure for table `portal_feed_event`
--

DROP TABLE IF EXISTS `portal_feed_event`;
CREATE TABLE `portal_feed_event` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `user_id` bigint NOT NULL COMMENT '事件发布者',
                                     `event_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'publish_article/publish_experience/new_column/checkin等',
                                     `target_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'article/experience/column/book等',
                                     `target_id` bigint NOT NULL COMMENT '目标对象ID',
                                     `title` varchar(256) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标标题',
                                     `summary` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '动态摘要',
                                     `cover` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '封面图',
                                     `created_time` datetime NOT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_time` (`user_id`,`created_time`),
                                     KEY `idx_type_time` (`event_type`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态事件流';


--
-- Table structure for table `portal_feed_inbox`
--

DROP TABLE IF EXISTS `portal_feed_inbox`;
CREATE TABLE `portal_feed_inbox` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `user_id` bigint NOT NULL COMMENT '接收者',
                                     `event_id` bigint NOT NULL COMMENT '动态事件ID',
                                     `created_time` datetime NOT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态收件箱';


--
-- Table structure for table `portal_feedback`
--

DROP TABLE IF EXISTS `portal_feedback`;
CREATE TABLE `portal_feedback` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
                                   `feedback_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈类型：suggestion/bug/experience/other',
                                   `subject` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈主题',
                                   `description` varchar(2000) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈详细描述',
                                   `contact` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系方式（可选）',
                                   `user_id` bigint DEFAULT NULL COMMENT '反馈人用户ID',
                                   `username` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈人用户名（冗余）',
                                   `ip` varchar(128) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈人IP',
                                   `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
                                   `handler` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理人',
                                   `handle_result` varchar(1000) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理结果说明',
                                   `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                   `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_feedback_type` (`feedback_type`),
                                   KEY `idx_status` (`status`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_create_time` (`create_time`),
                                   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户意见反馈表';


--
-- Table structure for table `portal_follow`
--

DROP TABLE IF EXISTS `portal_follow`;
CREATE TABLE `portal_follow` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注ID',
                                 `follower_id` bigint NOT NULL COMMENT '关注者ID（门户用户ID）',
                                 `following_id` bigint NOT NULL COMMENT '被关注者ID（门户用户ID）',
                                 `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_follower_following` (`follower_id`,`following_id`),
                                 KEY `idx_follower_id` (`follower_id`),
                                 KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户关注表';


--
-- Table structure for table `portal_friend_link`
--

DROP TABLE IF EXISTS `portal_friend_link`;
CREATE TABLE `portal_friend_link` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '链接ID',
                                      `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '链接名称',
                                      `url` varchar(500) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '链接地址',
                                      `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '链接描述',
                                      `logo` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Logo URL',
                                      `sort` int DEFAULT '0' COMMENT '排序',
                                      `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0正常 1停用',
                                      `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                      `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0=存在 2=删除，与全局逻辑删除配置一致）',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户友情链接表';

--
-- Table structure for table `portal_growth_log`
--

DROP TABLE IF EXISTS `portal_growth_log`;
CREATE TABLE `portal_growth_log` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint unsigned NOT NULL COMMENT '获得成长值的用户ID',
                                     `target_user_id` bigint unsigned DEFAULT NULL COMMENT '目标用户ID（如被点赞的内容作者）',
                                     `module` varchar(32) NOT NULL COMMENT '来源模块: article/reading/interview/all',
                                     `action` varchar(64) NOT NULL COMMENT '行为: publish_article/solve_question/finish_book/...',
                                     `entity_type` varchar(32) DEFAULT NULL COMMENT '实体类型: article/book/question/note/experience',
                                     `entity_id` bigint DEFAULT NULL COMMENT '实体ID',
                                     `growth_delta` int NOT NULL COMMENT '成长值变化（正数增加，负数减少）',
                                     `description` varchar(255) DEFAULT NULL COMMENT '描述',
                                     `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_time` (`user_id`,`create_time`),
                                     KEY `idx_module_action` (`module`,`action`),
                                     KEY `idx_entity` (`entity_type`,`entity_id`),
                                     KEY `idx_target_user` (`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长事件流水表';


--
-- Table structure for table `portal_growth_rule`
--

DROP TABLE IF EXISTS `portal_growth_rule`;
CREATE TABLE `portal_growth_rule` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `module` varchar(32) NOT NULL COMMENT '模块: article/reading/interview/all',
                                      `action` varchar(64) NOT NULL COMMENT '行为编码',
                                      `growth_delta` int NOT NULL COMMENT '成长值',
                                      `daily_limit` int DEFAULT '0' COMMENT '每日上限（0=不限）',
                                      `description` varchar(255) DEFAULT NULL COMMENT '描述',
                                      `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
                                      `sort` int DEFAULT '0' COMMENT '排序',
                                      `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_module_action` (`module`,`action`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长规则配置表';

--
-- Table structure for table `portal_help_article`
--

DROP TABLE IF EXISTS `portal_help_article`;
CREATE TABLE `portal_help_article` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
                                       `category_id` bigint NOT NULL COMMENT '分类ID',
                                       `title` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题标题',
                                       `content` text COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '答案内容（支持纯文本）',
                                       `view_count` int DEFAULT '0' COMMENT '查看次数',
                                       `like_count` int DEFAULT '0' COMMENT '点赞次数',
                                       `sort` int DEFAULT '0' COMMENT '排序（升序）',
                                       `is_featured` tinyint DEFAULT '0' COMMENT '是否精选：0=否 1=是',
                                       `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'published' COMMENT '状态：published/draft',
                                       `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                       `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_category_id` (`category_id`),
                                       KEY `idx_status` (`status`),
                                       KEY `idx_is_featured` (`is_featured`),
                                       KEY `idx_sort` (`sort`),
                                       KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帮助中心文章表';

--
-- Table structure for table `portal_help_category`
--

DROP TABLE IF EXISTS `portal_help_category`;
CREATE TABLE `portal_help_category` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
                                        `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
                                        `icon` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标（lucide 图标名）',
                                        `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类描述',
                                        `sort` int DEFAULT '0' COMMENT '排序（升序）',
                                        `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态：active/inactive',
                                        `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                        `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                        `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_status` (`status`),
                                        KEY `idx_sort` (`sort`),
                                        KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帮助中心分类表';

--
-- Table structure for table `portal_interview_attempt`
--



-- ---------------------------------------------------------------
-- 来源: 52_门户面试学习表.sql
-- ---------------------------------------------------------------
-- 来源：all-db-ddl.sql 行2472-2871（已剔除 INSERT 种子数据，种子数据见 80 段）
-- 用途：门户面试学习表 DDL（portal_interview_* 全系列）

DROP TABLE IF EXISTS `portal_interview_attempt`;
CREATE TABLE `portal_interview_attempt` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `question_id` bigint NOT NULL COMMENT '题目ID',
                                            `user_id` bigint NOT NULL COMMENT '用户ID',
                                            `attempt_count` int DEFAULT '1' COMMENT '尝试次数',
                                            `last_attempt_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后尝试时间',
                                            `status` varchar(30) DEFAULT 'attempted' COMMENT '状态:not_attempted,attempted,solved',
                                            `first_solved_at` datetime DEFAULT NULL COMMENT '首次解决时间',
                                            `last_solved_at` datetime DEFAULT NULL COMMENT '最后解决时间',
                                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                            `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            UNIQUE KEY `uk_question_user` (`question_id`,`user_id`),
                                            KEY `idx_user_id` (`user_id`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='做题记录表';


--
-- Table structure for table `portal_interview_bookmark`
--

DROP TABLE IF EXISTS `portal_interview_bookmark`;
CREATE TABLE `portal_interview_bookmark` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `question_id` bigint NOT NULL COMMENT '题目ID',
                                             `user_id` bigint NOT NULL COMMENT '用户ID',
                                             `note` text COMMENT '笔记',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                                             `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                             `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_question_user` (`question_id`,`user_id`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目收藏表';


--
-- Table structure for table `portal_interview_category`
--

DROP TABLE IF EXISTS `portal_interview_category`;
CREATE TABLE `portal_interview_category` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `name` varchar(200) NOT NULL COMMENT '分类名称',
                                             `slug` varchar(200) DEFAULT NULL COMMENT '分类标识',
                                             `description` text COMMENT '分类描述',
                                             `icon` varchar(500) DEFAULT NULL COMMENT '图标URL',
                                             `sort` int DEFAULT '0' COMMENT '排序',
                                             `question_count` int DEFAULT '0' COMMENT '题目数量',
                                             `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                                             `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             KEY `idx_status` (`status`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题目分类表';

--
-- Table structure for table `portal_interview_comment`
--

DROP TABLE IF EXISTS `portal_interview_comment`;
CREATE TABLE `portal_interview_comment` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `experience_id` bigint NOT NULL COMMENT '面经ID',
                                            `user_id` bigint NOT NULL COMMENT '评论用户ID',
                                            `parent_id` bigint DEFAULT NULL COMMENT '父评论ID（支持两级回复）',
                                            `reply_to_user_id` bigint DEFAULT NULL COMMENT '回复目标用户ID',
                                            `content` text NOT NULL COMMENT '评论内容',
                                            `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                            `status` varchar(20) DEFAULT 'published' COMMENT '状态:pending,published,rejected',
                                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                            `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            KEY `idx_experience_id` (`experience_id`),
                                            KEY `idx_user_id` (`user_id`),
                                            KEY `idx_parent_id` (`parent_id`),
                                            KEY `idx_status` (`status`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经评论表';


--
-- Table structure for table `portal_interview_comment_like`
--

DROP TABLE IF EXISTS `portal_interview_comment_like`;
CREATE TABLE `portal_interview_comment_like` (
                                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                 `comment_id` bigint NOT NULL COMMENT '评论ID',
                                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                                 PRIMARY KEY (`id`),
                                                 UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经评论点赞表';


--
-- Table structure for table `portal_interview_company`
--

DROP TABLE IF EXISTS `portal_interview_company`;
CREATE TABLE `portal_interview_company` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `name` varchar(200) NOT NULL COMMENT '公司名称',
                                            `slug` varchar(200) DEFAULT NULL COMMENT '公司标识',
                                            `logo` varchar(500) DEFAULT NULL COMMENT '公司Logo URL',
                                            `description` text COMMENT '公司描述',
                                            `industry` varchar(100) DEFAULT NULL COMMENT '所属行业',
                                            `question_count` int DEFAULT '0' COMMENT '相关题目数',
                                            `sort` int DEFAULT '0' COMMENT '排序',
                                            `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                            `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            UNIQUE KEY `uk_slug` (`slug`),
                                            KEY `idx_status` (`status`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试公司标签表';


--
-- Table structure for table `portal_interview_experience`
--

DROP TABLE IF EXISTS `portal_interview_experience`;
CREATE TABLE `portal_interview_experience` (
                                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                               `user_id` bigint NOT NULL COMMENT '用户ID',
                                               `title` varchar(500) NOT NULL COMMENT '面经标题',
                                               `company` varchar(200) NOT NULL COMMENT '公司',
                                               `position` varchar(200) DEFAULT NULL COMMENT '岗位',
                                               `year` int DEFAULT NULL COMMENT '年份',
                                               `month` int DEFAULT NULL COMMENT '月份',
                                               `summary` varchar(500) DEFAULT NULL COMMENT '内容摘要',
                                               `content` text NOT NULL COMMENT '面经内容',
                                               `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图URL',
                                               `tags` varchar(500) DEFAULT NULL COMMENT '标签',
                                               `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
                                               `view_count` bigint DEFAULT '0' COMMENT '浏览数',
                                               `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                               `comment_count` bigint DEFAULT '0' COMMENT '评论数',
                                               `status` varchar(20) DEFAULT 'published' COMMENT '状态:draft,pending,published,rejected,archived',
                                               `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                               `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                               PRIMARY KEY (`id`),
                                               KEY `idx_user_id` (`user_id`),
                                               KEY `idx_company` (`company`),
                                               KEY `idx_status` (`status`),
                                               KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经表';


--
-- Table structure for table `portal_interview_experience_like`
--

DROP TABLE IF EXISTS `portal_interview_experience_like`;
CREATE TABLE `portal_interview_experience_like` (
                                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                    `experience_id` bigint NOT NULL COMMENT '面经ID',
                                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                                    PRIMARY KEY (`id`),
                                                    UNIQUE KEY `uk_experience_user` (`experience_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经点赞表';


--
-- Table structure for table `portal_interview_position`
--

DROP TABLE IF EXISTS `portal_interview_position`;
CREATE TABLE `portal_interview_position` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `code` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码（如 java_backend）',
                                             `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称（如 Java后端工程师）',
                                             `industry` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属行业（如 互联网/金融/制造）',
                                             `level` varchar(32) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '岗位级别（junior/mid/senior）',
                                             `required_skills` text COLLATE utf8mb4_0900_ai_ci COMMENT '必备技能 JSON 数组（如 ["Spring","MySQL","Redis"]，与 portal_tag.name 对齐）',
                                             `hot_companies` text COLLATE utf8mb4_0900_ai_ci COMMENT '热门公司 JSON 数组（如 ["阿里","腾讯","字节"]）',
                                             `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '岗位描述',
                                             `sort` int DEFAULT '0' COMMENT '排序',
                                             `status` varchar(16) COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态 active/inactive',
                                             `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_code` (`code`),
                                             KEY `idx_status_sort` (`status`,`sort`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试岗位字典表';

--
-- Table structure for table `portal_interview_question`
--

DROP TABLE IF EXISTS `portal_interview_question`;
CREATE TABLE `portal_interview_question` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `title` varchar(500) NOT NULL COMMENT '题目标题',
                                             `description` text COMMENT '题目描述',
                                             `difficulty` varchar(20) DEFAULT 'medium' COMMENT '难度:easy,medium,hard',
                                             `category_id` bigint DEFAULT NULL COMMENT '分类ID',
                                             `tags` varchar(500) DEFAULT NULL COMMENT '标签，逗号分隔',
                                             `companies` varchar(500) DEFAULT NULL COMMENT '公司，逗号分隔',
                                             `acceptance_rate` decimal(5,2) DEFAULT '0.00' COMMENT '通过率',
                                             `submission_count` bigint DEFAULT '0' COMMENT '提交次数',
                                             `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                             `hint` text COMMENT '提示',
                                             `solution` text COMMENT '参考答案',
                                             `sort` int DEFAULT '0' COMMENT '排序',
                                             `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                                             `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             KEY `idx_category_id` (`category_id`),
                                             KEY `idx_difficulty` (`difficulty`),
                                             KEY `idx_status` (`status`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题目表';


--
-- Table structure for table `portal_interview_question_company`
--

DROP TABLE IF EXISTS `portal_interview_question_company`;
CREATE TABLE `portal_interview_question_company` (
                                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                     `question_id` bigint NOT NULL COMMENT '题目ID',
                                                     `company_id` bigint NOT NULL COMMENT '公司ID',
                                                     `sort` int DEFAULT '0' COMMENT '排序',
                                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                     PRIMARY KEY (`id`),
                                                     UNIQUE KEY `uk_question_company` (`question_id`,`company_id`),
                                                     KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目-公司关联表';


--
-- Table structure for table `portal_interview_question_like`
--

DROP TABLE IF EXISTS `portal_interview_question_like`;
CREATE TABLE `portal_interview_question_like` (
                                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                  `question_id` bigint NOT NULL COMMENT '题目ID',
                                                  `user_id` bigint NOT NULL COMMENT '用户ID',
                                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                                  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                                  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                                  PRIMARY KEY (`id`),
                                                  UNIQUE KEY `uk_question_user` (`question_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目点赞表';


--
-- Table structure for table `portal_interview_resume_template`
--

DROP TABLE IF EXISTS `portal_interview_resume_template`;
CREATE TABLE `portal_interview_resume_template` (
                                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                    `title` varchar(500) NOT NULL COMMENT '模板标题',
                                                    `description` text COMMENT '模板描述',
                                                    `cover` varchar(500) DEFAULT NULL COMMENT '封面URL',
                                                    `download_url` varchar(500) DEFAULT NULL COMMENT '下载地址',
                                                    `category` varchar(200) DEFAULT NULL COMMENT '分类',
                                                    `file_type` varchar(20) DEFAULT NULL COMMENT '文件类型：docx/pdf/psd',
                                                    `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
                                                    `is_premium` tinyint(1) DEFAULT '0' COMMENT '是否付费模板',
                                                    `usage_guide` text COMMENT '使用指南',
                                                    `tags` varchar(500) DEFAULT NULL COMMENT '标签，逗号分隔',
                                                    `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                                    `download_count` bigint DEFAULT '0' COMMENT '下载次数',
                                                    `sort` int DEFAULT '0' COMMENT '排序',
                                                    `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                                    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                                    PRIMARY KEY (`id`),
                                                    KEY `idx_status` (`status`),
                                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历模板表';


--
-- Table structure for table `portal_interview_resume_template_like`
--

DROP TABLE IF EXISTS `portal_interview_resume_template_like`;
CREATE TABLE `portal_interview_resume_template_like` (
                                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                         `template_id` bigint NOT NULL COMMENT '简历模板ID',
                                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                                         `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                                         `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                         `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                                         PRIMARY KEY (`id`),
                                                         UNIQUE KEY `uk_template_user` (`template_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历模板点赞表';


--
-- Table structure for table `portal_interview_submission`
--

DROP TABLE IF EXISTS `portal_interview_submission`;
CREATE TABLE `portal_interview_submission` (
                                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                               `question_id` bigint NOT NULL COMMENT '题目ID',
                                               `user_id` bigint NOT NULL COMMENT '用户ID',
                                               `code` text COMMENT '提交的代码',
                                               `content` text COMMENT '提交的文字答案',
                                               `language` varchar(50) DEFAULT 'java' COMMENT '编程语言',
                                               `answer_type` varchar(20) DEFAULT 'code' COMMENT '答案类型：code/text/design',
                                               `status` varchar(50) DEFAULT 'pending' COMMENT '状态:accepted,wrong_answer,time_limit,compile_error',
                                               `is_success` tinyint(1) DEFAULT '0' COMMENT '是否通过',
                                               `runtime` int DEFAULT NULL COMMENT '运行时间（毫秒）',
                                               `memory_usage` int DEFAULT NULL COMMENT '内存使用（KB）',
                                               `note` text COMMENT '备注/笔记',
                                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
                                               `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                               `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                               `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                               PRIMARY KEY (`id`),
                                               KEY `idx_question_id` (`question_id`),
                                               KEY `idx_user_id` (`user_id`),
                                               KEY `idx_status` (`status`),
                                               KEY `idx_user_question` (`user_id`,`question_id`),
                                               KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目提交记录表';


--
-- Table structure for table `portal_like`
--



-- ---------------------------------------------------------------
-- 来源: 53_门户互动社交表.sql
-- ---------------------------------------------------------------
-- 来源：all-db-ddl.sql 行2872-3835（已剔除 INSERT 种子数据，种子数据见 80 段）
-- 用途：门户互动社交表 DDL（portal_like / portal_message* / portal_mock_interview* / portal_order / portal_pk_challenge / portal_reading_* / portal_report / portal_shop_* / portal_study_plan* / portal_task / portal_tip_order / portal_topic_* / portal_user* / portal_vip_package / portal_wallet* / portal_writing_* / portal_wrong_question）
-- [v7.8 已删除] portal_notification_bak 表（历史遗留备份表，消息中心合并时创建，代码已无引用，下方第 5300 行有 DROP 语句）

DROP TABLE IF EXISTS `portal_like`;
CREATE TABLE `portal_like` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
                               `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                               `article_id` bigint NOT NULL COMMENT '文章ID',
                               `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
                               KEY `idx_user_id` (`user_id`),
                               KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户点赞表（文章）';


--
-- Table structure for table `portal_message`
--

DROP TABLE IF EXISTS `portal_message`;
CREATE TABLE `portal_message` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `session_id` bigint NOT NULL COMMENT '会话ID',
                                  `sender_id` bigint NOT NULL COMMENT '发送者',
                                  `sender_type` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '发送者类型 portal/sys',
                                  `receiver_id` bigint NOT NULL COMMENT '接收者',
                                  `receiver_type` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '接收者类型 portal/sys',
                                  `content` text COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
                                  `msg_type` varchar(16) COLLATE utf8mb4_0900_ai_ci DEFAULT 'text' COMMENT 'text/image/file',
                                  `is_read` tinyint DEFAULT '0' COMMENT '是否已读',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_session_time` (`session_id`,`create_time`),
                                  KEY `idx_receiver_type_read` (`receiver_id`,`receiver_type`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信消息';


--
-- Table structure for table `portal_message_session`
--

DROP TABLE IF EXISTS `portal_message_session`;
CREATE TABLE `portal_message_session` (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `user_a` bigint NOT NULL COMMENT '用户A（较小ID）',
                                          `user_a_type` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT 'A方用户类型 portal/sys',
                                          `user_b` bigint NOT NULL COMMENT '用户B（较大ID）',
                                          `user_b_type` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT 'B方用户类型 portal/sys',
                                          `last_message_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
                                          `last_message_content` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最后消息内容预览',
                                          `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
                                          `unread_a` int DEFAULT '0' COMMENT 'A未读数',
                                          `unread_b` int DEFAULT '0' COMMENT 'B未读数',
                                          `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                          `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_users_type` (`user_a`,`user_b`,`user_a_type`,`user_b_type`),
                                          KEY `idx_user_a` (`user_a`),
                                          KEY `idx_user_b` (`user_b`),
                                          KEY `idx_last_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信会话';


--
-- Table structure for table `portal_mock_interview`
--

DROP TABLE IF EXISTS `portal_mock_interview`;
CREATE TABLE `portal_mock_interview` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `user_id` bigint NOT NULL COMMENT '面试用户ID',
                                         `position` varchar(64) DEFAULT NULL COMMENT '面试岗位（如 后端开发/前端开发）',
                                         `scene` varchar(64) DEFAULT NULL COMMENT '面试场景（如 算法/系统设计/项目深挖，对应题目分类）',
                                         `status` varchar(16) NOT NULL DEFAULT 'in_progress' COMMENT '状态 in_progress/finished',
                                         `total_qa` int NOT NULL DEFAULT '0' COMMENT '题目总数',
                                         `score` int DEFAULT NULL COMMENT '面试总分（0-100，结束面试时计算）',
                                         `summary` text COMMENT 'AI 生成的面试总结',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `is_personalized` tinyint(1) DEFAULT '0' COMMENT '是否基于画像抽题（0随机 1画像驱动）',
                                         `profile_snapshot` text COMMENT '抽题时的画像快照 JSON（含薄弱点列表，便于回溯分析）',
                                         `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                         PRIMARY KEY (`id`),
                                         KEY `idx_user_time` (`user_id`,`create_time`),
                                         KEY `idx_status` (`status`),
                                         KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模拟面试会话';


--
-- Table structure for table `portal_mock_interview_qa`
--

DROP TABLE IF EXISTS `portal_mock_interview_qa`;
CREATE TABLE `portal_mock_interview_qa` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `interview_id` bigint NOT NULL COMMENT '面试会话ID',
                                            `question_id` bigint DEFAULT NULL COMMENT '关联题目ID（portal_interview_question.id）',
                                            `question_idx` int NOT NULL COMMENT '题目序号（从 0 开始）',
                                            `question` varchar(1000) NOT NULL COMMENT '面试问题（快照自题目标题）',
                                            `user_answer` text COMMENT '用户回答',
                                            `ai_feedback` text COMMENT 'AI 反馈（规则化生成）',
                                            `score` int DEFAULT NULL COMMENT '本题评分（0-100）',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            KEY `idx_interview` (`interview_id`),
                                            KEY `idx_question_idx` (`interview_id`,`question_idx`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模拟面试问答';

-- [v7.8 已删除] portal_notification_bak 表（历史遗留备份表，消息中心合并时创建，代码已无引用）
-- 该表的 DROP 语句保留在第 5302 行（属于"历史遗留表清理"段，DROP IF EXISTS 对新库幂等无害）


DROP TABLE IF EXISTS `portal_order`;
CREATE TABLE `portal_order` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                                `order_no` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
                                `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                `type` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：vip/recharge/product',
                                `product_id` bigint DEFAULT NULL COMMENT '商品ID',
                                `amount` decimal(10,2) NOT NULL COMMENT '金额',
                                `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '状态：pending/paid/cancelled/refunded',
                                `pay_method` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '支付方式：wechat/alipay',
                                `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
                                `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_order_no` (`order_no`),
                                KEY `idx_user_id` (`user_id`),
                                KEY `idx_type` (`type`),
                                KEY `idx_status` (`status`),
                                KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户订单表';


--
-- Table structure for table `portal_pk_challenge`
--

DROP TABLE IF EXISTS `portal_pk_challenge`;
CREATE TABLE `portal_pk_challenge` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `challenger_id` bigint NOT NULL COMMENT '发起方用户ID',
                                       `opponent_id` bigint NOT NULL COMMENT '应战方用户ID',
                                       `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态:pending/accepted/declined/ongoing/finished',
                                       `winner_id` bigint DEFAULT NULL COMMENT '胜者用户ID（平局为NULL）',
                                       `challenger_score` int NOT NULL DEFAULT '0' COMMENT '发起方得分（通过题数）',
                                       `opponent_score` int NOT NULL DEFAULT '0' COMMENT '应战方得分（通过题数）',
                                       `question_ids` varchar(500) NOT NULL COMMENT '题目ID列表，逗号分隔',
                                       `scene` varchar(20) NOT NULL DEFAULT '1v1' COMMENT '场景:1v1=好友PK / company=公司题目挑战',
                                       `company_id` bigint DEFAULT NULL COMMENT '公司ID（scene=company 时关联 portal_interview_company）',
                                       `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
                                       `finished_time` datetime DEFAULT NULL COMMENT '结束时间',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_challenger_id` (`challenger_id`),
                                       KEY `idx_opponent_id` (`opponent_id`),
                                       KEY `idx_status` (`status`),
                                       KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PK 对战表（异步对战）';


--
-- Table structure for table `portal_reading_preference`
--

DROP TABLE IF EXISTS `portal_reading_preference`;
CREATE TABLE `portal_reading_preference` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `user_id` bigint NOT NULL COMMENT '用户ID',
                                             `font_size` int DEFAULT '18' COMMENT '正文字号（px，12-32）',
                                             `line_height` decimal(3,1) DEFAULT '1.8' COMMENT '行距（倍，1.2-3.0）',
                                             `theme` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'default' COMMENT '阅读主题：default=跟随 / light=亮色 / dark=暗色 / sepia=护眼黄',
                                             `font_family` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT 'system' COMMENT '字体：system=系统默认 / serif=衬线 / song=宋体 / hei=黑体',
                                             `letter_spacing` decimal(3,1) DEFAULT '0.0' COMMENT '字间距（px，-1.0-5.0）',
                                             `paragraph_spacing` decimal(4,1) DEFAULT '1.2' COMMENT '段间距（em，0.5-5.0）',
                                             `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_user_id` (`user_id`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户阅读偏好表';

--


-- Table structure for table `portal_reading_progress`


DROP TABLE IF EXISTS `portal_reading_progress`;
CREATE TABLE `portal_reading_progress` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `user_id` bigint NOT NULL COMMENT '用户ID',
                                           `book_id` bigint NOT NULL COMMENT '书籍ID',
                                           `current_chapter_id` bigint DEFAULT NULL COMMENT '当前阅读章节ID',
                                           `current_chapter_no` int DEFAULT '0' COMMENT '当前章节序号',
                                           `chapter_offset` int DEFAULT '0' COMMENT '章节内滚动偏移（像素）',
                                           `last_read_time` datetime DEFAULT NULL COMMENT '最后阅读时间',
                                           `reading_duration_ms` bigint DEFAULT '0' COMMENT '累计阅读时长（毫秒）',
                                           `status` varchar(30) DEFAULT 'want_to_read' COMMENT '状态:want_to_read,reading,finished',
                                           `progress` int DEFAULT '0' COMMENT '阅读进度百分比',
                                           `pages_read` int DEFAULT '0' COMMENT '已读页数',
                                           `start_date` date DEFAULT NULL COMMENT '开始阅读日期',
                                           `finish_date` date DEFAULT NULL COMMENT '完成日期',
                                           `note` text COMMENT '阅读笔记',
                                           `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                           `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                           `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `uk_user_book` (`user_id`,`book_id`),
                                           KEY `idx_user_id` (`user_id`),
                                           KEY `idx_status` (`status`),
                                           KEY `idx_last_read_time` (`last_read_time`),
                                           KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阅读进度表';


-- Table structure for table `portal_report`


DROP TABLE IF EXISTS `portal_report`;
CREATE TABLE `portal_report` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '举报ID',
                                 `report_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '举报类型：spam/inappropriate/infringement/fraud/other',
                                 `target_url` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报目标URL',
                                 `target_type` varchar(32) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报目标类型：comment/article/user 等，为空表示通用举报（仅 target_url）',
                                 `target_id` bigint DEFAULT NULL COMMENT '举报目标ID（评论/文章/用户ID，配合 target_type 使用）',
                                 `description` varchar(2000) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题描述',
                                 `contact` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系方式（可选）',
                                 `images` varchar(1000) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图片证据（JSON数组，最多3张）',
                                 `user_id` bigint DEFAULT NULL COMMENT '举报人用户ID',
                                 `username` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报人用户名（冗余）',
                                 `ip` varchar(128) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报人IP',
                                 `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
                                 `handler` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理人',
                                 `handle_result` varchar(1000) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理结果说明',
                                 `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_report_type` (`report_type`),
                                 KEY `idx_status` (`status`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_create_time` (`create_time`),
                                 KEY `idx_target` (`target_type`,`target_id`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户举报记录表';


-- Table structure for table `portal_shop_exchange`


DROP TABLE IF EXISTS `portal_shop_exchange`;
CREATE TABLE `portal_shop_exchange` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                        `user_id` bigint NOT NULL COMMENT '兑换用户ID',
                                        `item_id` bigint NOT NULL COMMENT '商品ID',
                                        `points_cost` int NOT NULL COMMENT '消耗积分（冗余，便于查询）',
                                        `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '状态 pending/fulfilled/failed',
                                        `address` varchar(500) DEFAULT NULL COMMENT '收货地址（实物商品）',
                                        `exchange_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间',
                                        `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                        `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                        `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_user` (`user_id`),
                                        KEY `idx_item` (`item_id`),
                                        KEY `idx_status` (`status`),
                                        KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换记录表';


-- Table structure for table `portal_shop_item`


DROP TABLE IF EXISTS `portal_shop_item`;
CREATE TABLE `portal_shop_item` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `name` varchar(128) NOT NULL COMMENT '商品名称',
                                    `description` varchar(500) DEFAULT NULL COMMENT '商品描述',
                                    `cover` varchar(500) DEFAULT NULL COMMENT '商品封面URL',
                                    `type` varchar(32) NOT NULL DEFAULT 'virtual' COMMENT '商品类型 virtual/physical',
                                    `points_cost` int NOT NULL DEFAULT '0' COMMENT '兑换所需积分',
                                    `stock` int NOT NULL DEFAULT '0' COMMENT '库存（-1表示不限）',
                                    `status` varchar(16) NOT NULL DEFAULT 'active' COMMENT '状态 active/inactive',
                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_type_status` (`type`,`status`),
                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分商城商品表';

--
-- Table structure for table `portal_study_plan`
--

DROP TABLE IF EXISTS `portal_study_plan`;
CREATE TABLE `portal_study_plan` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                     `title` varchar(128) NOT NULL COMMENT '计划标题',
                                     `plan_type` varchar(32) DEFAULT NULL COMMENT '计划类型 daily_question/weekly_reading/custom',
                                     `target_count` int DEFAULT NULL COMMENT '目标数量',
                                     `target_category` varchar(64) DEFAULT NULL COMMENT '目标分类',
                                     `start_date` date DEFAULT NULL COMMENT '开始日期',
                                     `end_date` date DEFAULT NULL COMMENT '结束日期',
                                     `status` varchar(16) NOT NULL DEFAULT 'active' COMMENT '状态 active/completed/abandoned',
                                     `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user` (`user_id`),
                                     KEY `idx_status` (`status`),
                                     KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习计划';


--
-- Table structure for table `portal_study_plan_log`
--

DROP TABLE IF EXISTS `portal_study_plan_log`;
CREATE TABLE `portal_study_plan_log` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `plan_id` bigint NOT NULL COMMENT '计划ID',
                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                         `log_date` date NOT NULL COMMENT '日志日期',
                                         `done_count` int NOT NULL DEFAULT '0' COMMENT '当日完成数量',
                                         `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_plan_date` (`plan_id`,`log_date`),
                                         KEY `idx_user_date` (`user_id`,`log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计划每日进度';


--
-- Table structure for table `portal_task`
--

DROP TABLE IF EXISTS `portal_task`;
CREATE TABLE `portal_task` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `code` varchar(64) NOT NULL COMMENT '任务编码（唯一，用于埋点触发，如 daily_checkin）',
                               `name` varchar(128) NOT NULL COMMENT '任务名称',
                               `description` varchar(500) DEFAULT NULL COMMENT '任务描述',
                               `task_type` varchar(32) NOT NULL DEFAULT 'daily' COMMENT '任务类型 daily/once/achievement',
                               `reward_points` int NOT NULL DEFAULT '0' COMMENT '完成奖励积分',
                               `target_count` int NOT NULL DEFAULT '1' COMMENT '目标完成次数',
                               `icon` varchar(500) DEFAULT NULL COMMENT '任务图标URL',
                               `status` varchar(16) NOT NULL DEFAULT 'active' COMMENT '状态 active/inactive',
                               `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                               `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_code` (`code`),
                               KEY `idx_type_status` (`task_type`,`status`),
                               KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务定义表';

--
-- Table structure for table `portal_tip_order`
--

DROP TABLE IF EXISTS `portal_tip_order`;
CREATE TABLE `portal_tip_order` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `user_id` bigint NOT NULL COMMENT '打赏者用户ID',
                                    `author_id` bigint NOT NULL COMMENT '被打赏者用户ID',
                                    `target_type` varchar(32) NOT NULL COMMENT '打赏对象类型 article/column/article_paid',
                                    `target_id` bigint NOT NULL COMMENT '打赏对象ID',
                                    `amount` decimal(10,2) NOT NULL COMMENT '打赏金额',
                                    `message` varchar(200) DEFAULT NULL COMMENT '打赏留言',
                                    `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '状态 pending/paid/refunded',
                                    `pay_method` varchar(32) DEFAULT NULL COMMENT '支付方式',
                                    `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
                                    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_author` (`author_id`),
                                    KEY `idx_target` (`target_type`,`target_id`),
                                    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打赏订单（复用为付费阅读购买记录，target_type=article_paid）';


--
-- Table structure for table `portal_topic`
--

DROP TABLE IF EXISTS `portal_topic`;
CREATE TABLE `portal_topic` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `title` varchar(128) NOT NULL COMMENT '话题标题',
                                `description` varchar(500) DEFAULT NULL COMMENT '话题描述/导语',
                                `cover` varchar(500) DEFAULT NULL COMMENT '封面图 URL',
                                `creator_id` bigint NOT NULL COMMENT '发起人 portal_user.id（必须是认证创作者）',
                                `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active 活跃/archived 归档/deleted 删除',
                                `pinned` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶：0 否/1 是',
                                `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览数',
                                `post_count` int NOT NULL DEFAULT '0' COMMENT '观点数',
                                `like_count` int NOT NULL DEFAULT '0' COMMENT '话题被赞数',
                                `is_featured` tinyint NOT NULL DEFAULT '0' COMMENT '是否精选：0 否/1 是',
                                `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论数（一级评论）',
                                `last_post_time` datetime DEFAULT NULL COMMENT '最后观点时间',
                                `last_poster_id` bigint DEFAULT NULL COMMENT '最后观点用户',
                                `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                PRIMARY KEY (`id`),
                                KEY `idx_creator_time` (`creator_id`,`created_time`),
                                KEY `idx_status_pinned_last` (`status`,`pinned`,`last_post_time`),
                                KEY `idx_last_post` (`last_post_time`),
                                KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题主表';


--
-- Table structure for table `portal_topic_comment`
--

DROP TABLE IF EXISTS `portal_topic_comment`;
CREATE TABLE `portal_topic_comment` (
                                        `id` bigint NOT NULL AUTO_INCREMENT,
                                        `target_type` varchar(20) NOT NULL COMMENT '目标类型：topic 话题评论 / post 观点评论',
                                        `target_id` bigint NOT NULL COMMENT '目标 ID',
                                        `author_id` bigint NOT NULL COMMENT '评论者 portal_user.id',
                                        `content` varchar(2000) NOT NULL COMMENT '评论内容',
                                        `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父评论 ID（0=一级评论）',
                                        `root_id` bigint NOT NULL DEFAULT '0' COMMENT '根评论 ID（一级评论 root_id=0）',
                                        `reply_to` bigint DEFAULT NULL COMMENT '被回复的用户 ID',
                                        `reply_to_content` varchar(200) DEFAULT '' COMMENT '被回复内容摘要',
                                        `like_count` int NOT NULL DEFAULT '0',
                                        `reply_count` int NOT NULL DEFAULT '0' COMMENT '回复数（仅一级评论维护）',
                                        `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '软删',
                                        `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                        PRIMARY KEY (`id`),
                                        KEY `idx_target_type_id_parent` (`target_type`,`target_id`,`parent_id`,`created_time`),
                                        KEY `idx_root` (`root_id`,`created_time`),
                                        KEY `idx_author_time` (`author_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题评论（多态）';


--
-- Table structure for table `portal_topic_comment_like`
--

DROP TABLE IF EXISTS `portal_topic_comment_like`;
CREATE TABLE `portal_topic_comment_like` (
                                             `id` bigint NOT NULL AUTO_INCREMENT,
                                             `comment_id` bigint NOT NULL,
                                             `user_id` bigint NOT NULL,
                                             `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`),
                                             KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题评论点赞';


--
-- Table structure for table `portal_topic_like`
--

DROP TABLE IF EXISTS `portal_topic_like`;
CREATE TABLE `portal_topic_like` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `topic_id` bigint NOT NULL,
                                     `user_id` bigint NOT NULL,
                                     `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_topic_user` (`topic_id`,`user_id`),
                                     KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题点赞';


--
-- Table structure for table `portal_topic_post`
--

DROP TABLE IF EXISTS `portal_topic_post`;
CREATE TABLE `portal_topic_post` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `topic_id` bigint NOT NULL COMMENT '所属话题',
                                     `user_id` bigint NOT NULL COMMENT '发布者 portal_user.id',
                                     `content` text NOT NULL COMMENT '观点内容（Markdown）',
                                     `images` json DEFAULT NULL COMMENT '图片 URL 列表，最多 9 张',
                                     `parent_post_id` bigint DEFAULT NULL COMMENT '父观点 ID（楼中楼，NULL 为一级观点）',
                                     `reply_to_user_id` bigint DEFAULT NULL COMMENT '回复的用户 ID',
                                     `floor` int NOT NULL DEFAULT '0' COMMENT '楼层号',
                                     `like_count` int NOT NULL DEFAULT '0',
                                     `comment_count` int NOT NULL DEFAULT '0',
                                     `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '软删：0 否/1 是',
                                     `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_topic_floor` (`topic_id`,`floor`),
                                     KEY `idx_topic_time` (`topic_id`,`created_time`),
                                     KEY `idx_user_time` (`user_id`,`created_time`),
                                     KEY `idx_parent` (`parent_post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题观点（楼层）';


--
-- Table structure for table `portal_topic_post_like`
--

DROP TABLE IF EXISTS `portal_topic_post_like`;
CREATE TABLE `portal_topic_post_like` (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `post_id` bigint NOT NULL,
                                          `user_id` bigint NOT NULL,
                                          `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
                                          KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题观点点赞';


--
-- Table structure for table `portal_user`
--

DROP TABLE IF EXISTS `portal_user`;
CREATE TABLE `portal_user` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                               `user_id` bigint DEFAULT NULL COMMENT '关联后台用户ID',
                               `username` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
                               `nickname` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '昵称',
                               `email` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
                               `phone` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
                               `password` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '密码',
                               `avatar` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像URL',
                               `bio` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '个人简介',
                               `position` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '职位',
                               `wechat` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '微信号',
                               `gender` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '性别：male-男，female-女，other-其他',
                               `birthday` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生日：YYYY-MM-DD格式',
                               `location` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所在城市：如北京市',
                               `website` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '个人网站URL',
                               `github` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'GitHub用户名或完整URL',
                               `company` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '公司名称',
                               `school` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '学校名称',
                               `language` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '语言偏好：zh-CN，en-US等',
                               `timezone` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '时区：如Asia/Shanghai',
                               `notify_like` tinyint(1) DEFAULT '1' COMMENT '是否接收点赞通知',
                               `notify_comment` tinyint(1) DEFAULT '1' COMMENT '是否接收评论通知',
                               `notify_follow` tinyint(1) DEFAULT '1' COMMENT '是否接收关注通知',
                               `notify_system` tinyint(1) DEFAULT '1' COMMENT '是否接收系统通知',
                               `privacy_follow` tinyint(1) DEFAULT '1' COMMENT '是否允许被关注',
                               `privacy_bookmark` tinyint(1) DEFAULT '1' COMMENT '是否公开收藏夹',
                               `privacy_email` tinyint(1) DEFAULT '0' COMMENT '是否公开邮箱',
                               `privacy_phone` tinyint(1) DEFAULT '0' COMMENT '是否公开手机号',
                               `privacy_profile` tinyint(1) DEFAULT '1' COMMENT '是否公开主页（是否在名家录/作者列表展示）：1=公开，0=不公开',
                               `role` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'user' COMMENT '角色：user/admin',
                               `is_certified_creator` tinyint NOT NULL DEFAULT '0' COMMENT '是否认证创作者：0 否/1 是',
                               `vip_expire_at` datetime DEFAULT NULL COMMENT 'VIP过期时间',
                               `is_phone_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证手机号',
                               `is_wechat_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证微信',
                               `two_factor_enabled` tinyint(1) DEFAULT '0' COMMENT '是否开启两步验证',
                               `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
                               `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                               `login_ip` varchar(128) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '最后登录IP',
                               `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
                               `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_username` (`username`),
                               KEY `idx_user_id` (`user_id`),
                               KEY `idx_email` (`email`),
                               KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户用户表';

--
-- Table structure for table `portal_user_badge`
--

DROP TABLE IF EXISTS `portal_user_badge`;
CREATE TABLE `portal_user_badge` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
                                     `achievement_id` bigint unsigned NOT NULL COMMENT '成就ID',
                                     `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
                                     `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_user_achievement` (`user_id`,`achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户徽章记录表';


--
-- Table structure for table `portal_user_growth`
--

DROP TABLE IF EXISTS `portal_user_growth`;
CREATE TABLE `portal_user_growth` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `user_id` bigint unsigned NOT NULL COMMENT '门户用户ID（portal_user.id）',
                                      `growth_value` int unsigned DEFAULT '0' COMMENT '成长值（累计，只增不减）',
                                      `level` int DEFAULT '1' COMMENT '当前等级',
                                      `title` varchar(50) DEFAULT '初出茅庐' COMMENT '当前头衔',
                                      `season_value` int unsigned DEFAULT '0' COMMENT '本季成长值（赛季排名用）',
                                      `points` bigint NOT NULL DEFAULT '0' COMMENT '积分余额（可消耗，与成长值解耦）',
                                      `supplement_card_count` int NOT NULL DEFAULT '0' COMMENT '补签卡数量（每月赠送1张，补签消耗）',
                                      `last_card_grant_month` varchar(7) DEFAULT NULL COMMENT '最后赠送补签卡月份（YYYY-MM，幂等控制）',
                                      `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_user` (`user_id`),
                                      KEY `idx_season` (`season_value` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户成长值总表';


--
-- Table structure for table `portal_user_resume`
--

DROP TABLE IF EXISTS `portal_user_resume`;
CREATE TABLE `portal_user_resume` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `user_id` bigint NOT NULL COMMENT '用户ID',
                                      `title` varchar(100) NOT NULL DEFAULT '我的简历' COMMENT '简历名称',
                                      `parent_id` bigint DEFAULT NULL COMMENT '父简历ID（版本历史关联，首次创建为 NULL）',
                                      `version_no` int NOT NULL DEFAULT '1' COMMENT '版本号',
                                      `name` varchar(50) DEFAULT NULL COMMENT '姓名',
                                      `gender` varchar(10) DEFAULT NULL COMMENT '性别：男/女',
                                      `birth_date` date DEFAULT NULL COMMENT '出生日期',
                                      `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
                                      `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                                      `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
                                      `job_intention` text COMMENT '求职意向（JSON：期望职位/城市/薪资/类型）',
                                      `educations` text COMMENT '教育经历（JSON 数组：学校/专业/学历/时间/描述）',
                                      `works` text COMMENT '工作经历（JSON 数组：公司/职位/时间/描述）',
                                      `projects` text COMMENT '项目经历（JSON 数组：名称/角色/时间/描述/链接）',
                                      `skills` text COMMENT '技能列表（JSON 数组：名称/等级/分类）',
                                      `self_intro` text COMMENT '自我介绍',
                                      `score` int DEFAULT NULL COMMENT '评分（0-100）',
                                      `score_detail` text COMMENT '评分明细（JSON 数组）',
                                      `scored_time` datetime DEFAULT NULL COMMENT '评分时间',
                                      `file_url` varchar(255) DEFAULT NULL COMMENT 'PDF 导出文件URL',
                                      `export_time` datetime DEFAULT NULL COMMENT '最后导出时间',
                                      `status` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/archived',
                                      `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                      `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_parent_version` (`parent_id`,`version_no`),
                                      KEY `idx_user_id` (`user_id`),
                                      KEY `idx_parent_id` (`parent_id`),
                                      KEY `idx_user_status` (`user_id`,`status`),
                                      KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户简历';


--
-- Table structure for table `portal_user_stats`
--

DROP TABLE IF EXISTS `portal_user_stats`;
CREATE TABLE `portal_user_stats` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint unsigned NOT NULL COMMENT '门户用户ID',
                                     `article_count` int DEFAULT '0' COMMENT '发布文章数',
                                     `article_view_sum` bigint DEFAULT '0' COMMENT '文章总浏览量',
                                     `article_like_sum` bigint DEFAULT '0' COMMENT '文章总获赞数',
                                     `article_bookmark_sum` bigint DEFAULT '0' COMMENT '文章总收藏数',
                                     `article_word_sum` bigint DEFAULT '0' COMMENT '累计创作字数',
                                     `book_finished` int DEFAULT '0' COMMENT '读完的书',
                                     `booklist_count` int DEFAULT '0' COMMENT '创建书单数',
                                     `quote_count` int DEFAULT '0' COMMENT '发布金句数',
                                     `reading_minutes` bigint DEFAULT '0' COMMENT '累计阅读时长(分钟)',
                                     `question_solved` int DEFAULT '0' COMMENT '解题数',
                                     `note_count` int DEFAULT '0' COMMENT '笔记数',
                                     `experience_count` int DEFAULT '0' COMMENT '面经数',
                                     `note_adopted` int DEFAULT '0' COMMENT '笔记被精选数',
                                     `follower_count` int DEFAULT '0' COMMENT '粉丝数',
                                     `following_count` int DEFAULT '0' COMMENT '关注数',
                                     `comment_count` int DEFAULT '0' COMMENT '跨模块评论总数',
                                     `total_like_received` bigint DEFAULT '0' COMMENT '跨模块总获赞',
                                     `checkin_streak` int DEFAULT '0' COMMENT '连续签到天数',
                                     `last_checkin_date` date DEFAULT NULL COMMENT '最后签到日期',
                                     `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                     `mock_interview_count` int DEFAULT '0' COMMENT '模拟面试次数',
                                     `avg_mock_score` int DEFAULT '0' COMMENT '模拟面试平均分',
                                     `weak_tags` text COMMENT '薄弱知识点 JSON 数组（如 [{"tagId":1,"tagName":"Spring","failRate":0.6}]）',
                                     `weak_tags_updated_time` datetime DEFAULT NULL COMMENT '薄弱点最后计算时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户用户统计聚合表';


--
-- Table structure for table `portal_user_task`
--

DROP TABLE IF EXISTS `portal_user_task`;
CREATE TABLE `portal_user_task` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                    `task_id` bigint NOT NULL COMMENT '任务ID',
                                    `progress` int NOT NULL DEFAULT '0' COMMENT '当前进度',
                                    `completed` tinyint NOT NULL DEFAULT '0' COMMENT '是否已完成 0/1',
                                    `claimed` tinyint NOT NULL DEFAULT '0' COMMENT '是否已领取奖励 0/1',
                                    `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_user_task` (`user_id`,`task_id`),
                                    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户任务进度表';


--
-- Table structure for table `portal_vip_package`
--

DROP TABLE IF EXISTS `portal_vip_package`;
CREATE TABLE `portal_vip_package` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
                                      `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '套餐名称',
                                      `price` decimal(10,2) NOT NULL COMMENT '价格',
                                      `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
                                      `duration` int NOT NULL COMMENT '有效期（天）',
                                      `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '套餐描述',
                                      `features` json DEFAULT NULL COMMENT '功能列表（JSON数组）',
                                      `popular` tinyint(1) DEFAULT '0' COMMENT '是否热门',
                                      `sort` int DEFAULT '0' COMMENT '排序',
                                      `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态：active/inactive',
                                      `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                      `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_status` (`status`),
                                      KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户VIP套餐表';


DROP TABLE IF EXISTS `portal_wallet`;
CREATE TABLE `portal_wallet` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
                                 `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                 `balance` decimal(10,2) DEFAULT '0.00' COMMENT '余额',
                                 `frozen_balance` decimal(10,2) DEFAULT '0.00' COMMENT '冻结余额',
                                 `total_recharge` decimal(10,2) DEFAULT '0.00' COMMENT '累计充值',
                                 `total_withdraw` decimal(10,2) DEFAULT '0.00' COMMENT '累计提现',
                                 `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_id` (`user_id`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户钱包表';


--
-- Table structure for table `portal_wallet_transaction`
--

DROP TABLE IF EXISTS `portal_wallet_transaction`;
CREATE TABLE `portal_wallet_transaction` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '交易ID',
                                             `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                             `type` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：recharge/consume/refund/withdraw',
                                             `amount` decimal(10,2) NOT NULL COMMENT '金额',
                                             `balance_before` decimal(10,2) NOT NULL COMMENT '交易前余额',
                                             `balance_after` decimal(10,2) NOT NULL COMMENT '交易后余额',
                                             `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
                                             `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
                                             `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                             PRIMARY KEY (`id`),
                                             KEY `idx_user_id` (`user_id`),
                                             KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户钱包交易记录表';


--
-- Table structure for table `portal_writing_contest`
--

DROP TABLE IF EXISTS `portal_writing_contest`;
CREATE TABLE `portal_writing_contest` (
                                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `title` varchar(128) NOT NULL COMMENT '活动标题',
                                          `description` text COMMENT '活动描述',
                                          `theme` varchar(128) DEFAULT NULL COMMENT '征文主题',
                                          `cover` varchar(500) DEFAULT NULL COMMENT '封面',
                                          `start_time` datetime DEFAULT NULL COMMENT '活动开始时间',
                                          `end_time` datetime DEFAULT NULL COMMENT '投稿截止时间',
                                          `vote_end_time` datetime DEFAULT NULL COMMENT '投票截止时间',
                                          `prize` varchar(500) DEFAULT NULL COMMENT '奖品说明',
                                          `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT 'draft/collecting/voting/ended',
                                          `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                          PRIMARY KEY (`id`),
                                          KEY `idx_status` (`status`),
                                          KEY `idx_start_time` (`start_time`),
                                          KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创作挑战/征文活动';


--
-- Table structure for table `portal_writing_prompt`
--

DROP TABLE IF EXISTS `portal_writing_prompt`;
CREATE TABLE `portal_writing_prompt` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `prompt_date` date NOT NULL COMMENT 'prompt 日期（唯一）',
                                         `title` varchar(128) NOT NULL COMMENT 'prompt 标题',
                                         `description` text COMMENT 'prompt 描述',
                                         `category` varchar(32) DEFAULT NULL COMMENT '分类（如：生活/职场/情感/虚构/哲思）',
                                         `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_prompt_date` (`prompt_date`),
                                         KEY `idx_category` (`category`),
                                         KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日写作 prompt';

--
-- Table structure for table `portal_wrong_question`
--

DROP TABLE IF EXISTS `portal_wrong_question`;
CREATE TABLE `portal_wrong_question` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                         `question_id` bigint NOT NULL COMMENT '题目ID',
                                         `attempt_id` bigint DEFAULT NULL COMMENT '最近一次答题ID',
                                         `status` varchar(16) NOT NULL DEFAULT 'wrong' COMMENT '状态 wrong/reviewing/mastered',
                                         `wrong_count` int NOT NULL DEFAULT '1' COMMENT '答错次数',
                                         `last_wrong_time` datetime DEFAULT NULL COMMENT '最近答错时间',
                                         `next_review_time` datetime DEFAULT NULL COMMENT '下次复习时间（艾宾浩斯）',
                                         `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_user_question` (`user_id`,`question_id`),
                                         KEY `idx_user_status` (`user_id`,`status`),
                                         KEY `idx_user_review` (`user_id`,`next_review_time`),
                                         KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本';


--


-- ---------------------------------------------------------------
-- 来源: 54_AI模块表_v7.5.sql
-- ---------------------------------------------------------------
-- =====================================================================
-- 墨韵·智库 v7.5 AI 模块升级脚本
-- 说明：新增 AI 智能体/知识库/工作流/数据分析 模块全部表结构
-- 执行前请确保数据库为 v6.8+ 版本
-- Date: 2026-01-23
-- =====================================================================

-- ----------------------------
-- Table structure for agent
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_agent`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '智能体名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '智能体描述',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '系统提示词',
  `knowledge_base_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联的知识库ID（多个用逗号分隔）',
  `knowledge_library_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '关联的知识库ID列表（JSON数组）',
  `knowledge_base_weights` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '知识库权重配置（JSON格式：{\"1\": 1.0, \"2\": 0.8}，权重范围0.1-1.0）',
  `model_config_id` bigint NULL DEFAULT NULL COMMENT '模型配置ID(关联model_config表,NULL则使用默认模型)',
  `model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'qwen-plus' COMMENT '模型名称',
  `temperature` double NULL DEFAULT 0.7 COMMENT '温度参数',
  `max_tokens` int NULL DEFAULT 2000 COMMENT '最大token数',
  `rag_min_score` double NULL DEFAULT NULL COMMENT 'RAG检索相似度阈值(0.5-1.0,推荐0.7-0.75,NULL则使用全局配置)',
  `rag_max_results` int NULL DEFAULT NULL COMMENT 'RAG检索最大结果数量(1-10,推荐3-5,NULL则使用全局配置)',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `welcome_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '开场白',
  `suggested_questions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '预设问题(JSON数组)',
  `show_citations` tinyint(1) NULL DEFAULT 1 COMMENT '是否显示引用来源',
  `max_history_turns` int NULL DEFAULT 10 COMMENT '最大历史轮数',
  `api_enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用API',
  `api_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API Key',
  `workflow_id` bigint NULL DEFAULT NULL COMMENT '关联工作流ID',
  `workflow_trigger_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'manual' COMMENT '工作流触发模式: manual/auto/keyword',
  `workflow_trigger_keywords` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '触发关键词(JSON数组)',
  `publish_enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否发布为应用',
  `publish_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发布访问Token',
  `publish_settings` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '发布设置(JSON)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `rag_recall_multiplier` double NULL DEFAULT NULL COMMENT '第一阶段召回倍数（1.5-3.0，推荐2.0，NULL时使用全局配置）',
  `rag_enable_hybrid_search` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用混合检索（向量+BM25）',
  `rag_enable_query_expansion` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用查询扩展',
  `rag_bm25_weight` double NULL DEFAULT 0.3 COMMENT 'BM25检索权重（0-1）',
  `rag_vector_weight` double NULL DEFAULT 0.7 COMMENT '向量检索权重（0-1）',
  `enable_self_reflection` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用自我反思',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_model_config_id`(`model_config_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent
-- ----------------------------

-- ----------------------------
-- Table structure for agent_dictionary_relation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_agent_dictionary_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `dictionary_id` bigint NOT NULL COMMENT '词典ID',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_dict`(`agent_id` ASC, `dictionary_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_dictionary_id`(`dictionary_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体词典关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent_dictionary_relation
-- ----------------------------

-- ----------------------------
-- Table structure for agent_tool
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_agent_tool`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具标识（英文）',
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '显示名称（中文）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具描述（给LLM理解用）',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'general' COMMENT '工具分类：general/information/utility/action/data',
  `tool_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具类型：builtin/http/database',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'fa-wrench' COMMENT '图标（FontAwesome）',
  `config` json NULL COMMENT '工具配置（API地址、认证信息等）',
  `parameters` json NOT NULL COMMENT '参数定义（JSON Schema格式）',
  `timeout_seconds` int NULL DEFAULT 30 COMMENT '超时时间（秒）',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `is_system` tinyint(1) NULL DEFAULT 0 COMMENT '是否系统内置',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体工具定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent_tool
-- ----------------------------
INSERT INTO `ai_agent_tool` VALUES (1, 'current_time', '当前时间', '获取当前的日期和时间，可指定时区和格式', 'utility', 'builtin', 'fa-clock', NULL, '{\"type\": \"object\", \"required\": [], \"properties\": {\"format\": {\"type\": \"string\", \"default\": \"yyyy-MM-dd HH:mm:ss\", \"description\": \"时间格式，默认yyyy-MM-dd HH:mm:ss\"}, \"timezone\": {\"type\": \"string\", \"default\": \"Asia/Shanghai\", \"description\": \"时区，如Asia/Shanghai，默认北京时间\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `ai_agent_tool` VALUES (2, 'calculator', '数学计算', '执行数学计算，支持加减乘除、幂运算、开方、三角函数等', 'utility', 'builtin', 'fa-calculator', NULL, '{\"type\": \"object\", \"required\": [\"expression\"], \"properties\": {\"expression\": {\"type\": \"string\", \"description\": \"数学表达式，如(1+2)*3、sqrt(16)、sin(30)\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `ai_agent_tool` VALUES (3, 'weather_query', '天气查询', '查询指定城市的实时天气和未来天气预报，包括温度、湿度、风向、天气状况等', 'information', 'http', 'fa-cloud-sun', '{\"api_type\": \"seniverse\"}', '{\"type\": \"object\", \"required\": [\"city\"], \"properties\": {\"city\": {\"type\": \"string\", \"description\": \"城市名称，如北京、上海、广州\"}, \"days\": {\"type\": \"integer\", \"default\": 1, \"description\": \"预报天数1-7，默认1天\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `ai_agent_tool` VALUES (4, 'web_search', '网络搜索', '搜索互联网获取最新信息，适用于查询新闻、事件、知识等实时内容', 'information', 'http', 'fa-search', '{\"api_type\": \"bing\"}', '{\"type\": \"object\", \"required\": [\"query\"], \"properties\": {\"count\": {\"type\": \"integer\", \"default\": 5, \"description\": \"返回结果数量，默认5条\"}, \"query\": {\"type\": \"string\", \"description\": \"搜索关键词\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `ai_agent_tool` VALUES (5, 'url_reader', '网页读取', '读取指定URL的网页内容，提取主要文本信息', 'information', 'http', 'fa-globe', '{\"timeout\": 10}', '{\"type\": \"object\", \"required\": [\"url\"], \"properties\": {\"url\": {\"type\": \"string\", \"description\": \"要读取的网页URL\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `ai_agent_tool` VALUES (6, 'translator', '文本翻译', '将文本翻译成指定语言，支持中英日韩等多种语言互译', 'utility', 'http', 'fa-language', '{\"api_type\": \"aliyun\"}', '{\"type\": \"object\", \"required\": [\"text\"], \"properties\": {\"to\": {\"type\": \"string\", \"default\": \"zh\", \"description\": \"目标语言代码，如zh/en/ja\"}, \"from\": {\"type\": \"string\", \"default\": \"auto\", \"description\": \"源语言代码，如zh/en/ja，可设为auto自动检测\"}, \"text\": {\"type\": \"string\", \"description\": \"要翻译的文本\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `ai_agent_tool` VALUES (7, 'send_email', '发送邮件', '发送电子邮件到指定邮箱地址', 'action', 'builtin', 'fa-envelope', '{}', '{\"type\": \"object\", \"required\": [\"to\", \"subject\", \"content\"], \"properties\": {\"to\": {\"type\": \"string\", \"description\": \"收件人邮箱地址\"}, \"content\": {\"type\": \"string\", \"description\": \"邮件正文内容\"}, \"subject\": {\"type\": \"string\", \"description\": \"邮件主题\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `ai_agent_tool` VALUES (8, 'database_query', '数据库查询', '执行SQL查询获取业务数据，仅支持SELECT查询语句', 'data', 'database', 'fa-database', '{\"max_rows\": 100}', '{\"type\": \"object\", \"required\": [\"sql\"], \"properties\": {\"sql\": {\"type\": \"string\", \"description\": \"SQL查询语句，仅支持SELECT\"}, \"database\": {\"type\": \"string\", \"default\": \"default\", \"description\": \"数据库名称，默认使用配置的业务库\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');

-- ----------------------------
-- Table structure for agent_tool_relation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_agent_tool_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `tool_id` bigint NOT NULL COMMENT '工具ID',
  `custom_config` json NULL COMMENT '针对该智能体的自定义配置',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_tool`(`agent_id` ASC, `tool_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_tool_id`(`tool_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体工具关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent_tool_relation
-- ----------------------------

-- ----------------------------
-- Table structure for agent_workflow_relation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_agent_workflow_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_workflow`(`agent_id` ASC, `workflow_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '智能体-工作流关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent_workflow_relation
-- ----------------------------

-- ----------------------------
-- Table structure for analysis_report
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_analysis_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `report_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报告名称',
  `report_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'auto' COMMENT '报告类型: auto, custom, scheduled',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分析的表名',
  `analysis_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '分析配置(JSON格式)',
  `executive_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '执行摘要(AI生成)',
  `data_overview` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数据概览(JSON格式)',
  `analysis_results` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '分析结果(JSON格式)',
  `insights` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数据洞察(JSON格式)',
  `charts` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图表配置(JSON格式)',
  `conclusion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '结论与建议(AI生成)',
  `report_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'draft' COMMENT '报告状态: draft, completed, archived',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '导出文件路径',
  `generate_time` int NULL DEFAULT 0 COMMENT '生成耗时(秒)',
  `view_count` int NULL DEFAULT 0 COMMENT '查看次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_datasource_id`(`datasource_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_report_type`(`report_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分析报告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of analysis_report
-- ----------------------------

-- ----------------------------
-- Table structure for chart_recommendation_rule
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_chart_recommendation_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名称',
  `data_pattern` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据模式: time_series, distribution, category, correlation',
  `field_types` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '字段类型组合(JSON)',
  `data_characteristics` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数据特征条件(JSON)',
  `recommended_chart` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推荐图表类型',
  `priority` int NULL DEFAULT 50 COMMENT '优先级(0-100)',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推荐理由',
  `min_data_points` int NULL DEFAULT 0 COMMENT '最小数据点数',
  `max_data_points` int NULL DEFAULT 999999 COMMENT '最大数据点数',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_data_pattern`(`data_pattern` ASC) USING BTREE,
  INDEX `idx_priority`(`priority` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图表推荐规则表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chart_recommendation_rule
-- ----------------------------
INSERT INTO `ai_chart_recommendation_rule` VALUES (1, '时间序列-折线图', 'time_series', NULL, NULL, 'line', 95, '时间趋势最适合用折线图展示', 2, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` VALUES (2, '分类占比-饼图', 'category', NULL, NULL, 'pie', 85, '少量分类适合饼图', 2, 6, 1, '2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` VALUES (3, '分类对比-柱状图', 'category', NULL, NULL, 'bar', 90, '多分类对比适合柱状图', 3, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` VALUES (4, '数值分布-直方图', 'distribution', NULL, NULL, 'histogram', 90, '数值分布最适合用直方图', 10, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` VALUES (5, '排名-条形图', 'ranking', NULL, NULL, 'bar', 90, '排名对比适合条形图', 3, 50, 1, '2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` VALUES (6, '相关性-散点图', 'correlation', NULL, NULL, 'scatter', 85, '相关性分析适合散点图', 10, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `ai_chart_recommendation_rule` VALUES (7, '多维对比-雷达图', 'multi_dimension', NULL, NULL, 'radar', 75, '多维度对比适合雷达图', 3, 8, 1, '2025-11-29 14:21:54');

-- ----------------------------
-- Table structure for chat_history
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `user_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用户消息',
  `assistant_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '助手回复',
  `tokens_used` int NULL DEFAULT 0 COMMENT 'Token消耗',
  `retrieval_results` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '检索结果JSON',
  `retrieval_count` int NULL DEFAULT 0 COMMENT '检索命中数',
  `response_time` int NULL DEFAULT 0 COMMENT '响应时间(毫秒)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 129 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对话历史表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_history
-- ----------------------------

-- ----------------------------
-- Table structure for conversation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_conversation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '新对话' COMMENT '会话标题（自动生成或用户修改）',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户ID（预留字段，支持多用户）',
  `message_count` int NULL DEFAULT 0 COMMENT '消息数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '对话摘要',
  `summary_updated_at` datetime NULL DEFAULT NULL COMMENT '摘要更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_update_time`(`update_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 53 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对话会话表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of conversation
-- ----------------------------

-- ----------------------------
-- Table structure for conversation_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_conversation_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色：user/assistant',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `reference_sources` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '参考来源（JSON格式）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `conversation_message_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 454 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对话消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of conversation_message
-- ----------------------------

-- ----------------------------
-- Table structure for data_insight
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_data_insight`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `query_id` bigint NULL DEFAULT NULL COMMENT '查询ID',
  `report_id` bigint NULL DEFAULT NULL COMMENT '报告ID',
  `insight_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '洞察类型: anomaly, trend, correlation, pattern',
  `severity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'medium' COMMENT '严重程度: low, medium, high',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '洞察标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '洞察描述',
  `affected_fields` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '影响的字段',
  `statistical_value` decimal(20, 4) NULL DEFAULT NULL COMMENT '统计值',
  `confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '置信度(0-1)',
  `actionable` tinyint(1) NULL DEFAULT 0 COMMENT '是否可执行',
  `recommendation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '建议措施',
  `is_acknowledged` tinyint(1) NULL DEFAULT 0 COMMENT '是否已确认',
  `acknowledged_by` bigint NULL DEFAULT NULL COMMENT '确认人ID',
  `acknowledged_time` datetime NULL DEFAULT NULL COMMENT '确认时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_datasource_id`(`datasource_id` ASC) USING BTREE,
  INDEX `idx_query_id`(`query_id` ASC) USING BTREE,
  INDEX `idx_report_id`(`report_id` ASC) USING BTREE,
  INDEX `idx_insight_type`(`insight_type` ASC) USING BTREE,
  INDEX `idx_severity`(`severity` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能洞察表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of data_insight
-- ----------------------------

-- ----------------------------
-- Table structure for datasource_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_datasource_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源类型: mysql, elasticsearch, mongodb',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口号',
  `database_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据库名称',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码(加密存储)',
  `connection_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '额外连接参数(JSON格式)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `health_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'unknown' COMMENT '健康状态: healthy, unhealthy, unknown',
  `last_check_time` datetime NULL DEFAULT NULL COMMENT '最后检查时间',
  `create_user_id` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据源配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of datasource_config
-- ----------------------------

-- ----------------------------
-- Table structure for document_chunk_metadata
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_document_chunk_metadata`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分片ID',
  `segment_id` bigint NOT NULL COMMENT '文档分片ID（关联document_segment表）',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `chunk_index` int NOT NULL COMMENT '分片序号',
  `chunk_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分片文本内容',
  `chunk_length` int NOT NULL COMMENT '分片长度',
  `parent_chunk_id` bigint NULL DEFAULT NULL COMMENT '父分片ID（父子分段模式使用）',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '使用的嵌入模型',
  `vector_dimension` int NULL DEFAULT NULL COMMENT '向量维度',
  `original_length` int NULL DEFAULT NULL COMMENT '预处理前长度',
  `preprocessed` tinyint(1) NULL DEFAULT 0 COMMENT '是否经过预处理',
  `hit_count` int NULL DEFAULT 0 COMMENT '被检索命中次数',
  `last_hit_time` timestamp NULL DEFAULT NULL COMMENT '最后命中时间',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_segment_id`(`segment_id` ASC) USING BTREE,
  INDEX `idx_knowledge_id`(`knowledge_id` ASC) USING BTREE,
  INDEX `idx_parent_chunk`(`parent_chunk_id` ASC) USING BTREE,
  INDEX `idx_hit_count`(`hit_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文档分片元数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of document_chunk_metadata
-- ----------------------------

-- ----------------------------
-- Table structure for document_image
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_document_image`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '关联的知识库ID',
  `image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片文件路径',
  `page_number` int NULL DEFAULT NULL COMMENT '所在页码',
  `image_index` int NULL DEFAULT NULL COMMENT '图片在页面中的索引',
  `embedding_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量ID',
  `vector_dimension` int NULL DEFAULT NULL COMMENT '向量维度',
  `width` int NULL DEFAULT NULL COMMENT '图片宽度',
  `height` int NULL DEFAULT NULL COMMENT '图片高度',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图片内容描述（多模态模型生成）',
  `description_language` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'zh' COMMENT '描述语言(zh/en)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_base_id`(`knowledge_base_id` ASC) USING BTREE,
  INDEX `idx_embedding_id`(`embedding_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2592 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文档图片表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of document_image
-- ----------------------------

-- ----------------------------
-- Table structure for document_segment
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_document_segment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '关联的知识库ID',
  `segment_index` int NOT NULL COMMENT '分片索引（第几个分片）',
  `page_number` int NULL DEFAULT NULL COMMENT 'PDF页码',
  `line_start` int NULL DEFAULT NULL COMMENT '起始行号',
  `line_end` int NULL DEFAULT NULL COMMENT '结束行号',
  `char_start` int NULL DEFAULT NULL COMMENT '起始字符位置',
  `char_end` int NULL DEFAULT NULL COMMENT '结束字符位置',
  `chapter_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '章节标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分片内容',
  `content_length` int NULL DEFAULT NULL COMMENT '分片内容长度',
  `embedding_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量ID（在Pinecone中的ID）',
  `vector_dimension` int NULL DEFAULT NULL COMMENT '向量维度',
  `vector_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '向量数据（JSON格式）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_base_id`(`knowledge_base_id` ASC) USING BTREE,
  INDEX `idx_embedding_id`(`embedding_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4595 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文档分片表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of document_segment
-- ----------------------------

-- ----------------------------
-- Table structure for domain_dictionary
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_domain_dictionary`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '核心词',
  `related_terms` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '相关词列表（逗号分隔）',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'general' COMMENT '分类（服务器、架构、模型、通用等）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '词典说明',
  `is_global` tinyint(1) NULL DEFAULT 1 COMMENT '是否全局词典（全局词典默认对所有智能体生效）',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `priority` int NULL DEFAULT 0 COMMENT '优先级（数字越大优先级越高）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_keyword`(`keyword` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_global`(`is_global` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '领域词典表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of domain_dictionary
-- ----------------------------
INSERT INTO `ai_domain_dictionary` VALUES (1, '服务器', 'cpu,gpu,npu,内存,存储,硬盘,系统盘,数据盘,鲲鹏,昇腾,算力,主机,机器,配置,规格', '硬件', '服务器相关术语', 0, 1, 10, '2025-11-24 13:47:33', '2025-11-24 13:56:57');
INSERT INTO `ai_domain_dictionary` VALUES (2, '架构', '系统架构,技术架构,平台架构,设计,模块,组件,层次,结构,框架', '技术', '架构相关术语', 0, 1, 8, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `ai_domain_dictionary` VALUES (3, '模型', '大模型,embedding,向量,llm,ai模型,算法,训练,推理', 'AI', '模型相关术语', 0, 1, 9, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `ai_domain_dictionary` VALUES (4, '知识库', '文档,向量库,rag,检索,知识管理,知识图谱', 'AI', '知识库相关术语', 0, 1, 7, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `ai_domain_dictionary` VALUES (5, '部署', '安装,配置,环境,运维,上线,发布', '运维', '部署相关术语', 0, 1, 6, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `ai_domain_dictionary` VALUES (6, '性能', '速度,效率,吞吐量,延迟,响应时间,优化', '技术', '性能相关术语', 0, 1, 5, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `ai_domain_dictionary` VALUES (7, '安全', '权限,认证,授权,加密,防护,隔离', '安全', '安全相关术语', 0, 1, 8, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `ai_domain_dictionary` VALUES (8, '数据库', 'MySQL,PostgreSQL,MongoDB,Redis,Oracle,SQL,NoSQL,索引,事务,主从,分库分表,读写分离', '技术', '数据库相关术语，包含关系型和非关系型数据库', 0, 1, 9, '2025-11-01 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `ai_domain_dictionary` VALUES (9, '微服务', 'SpringCloud,Dubbo,gRPC,服务注册,服务发现,负载均衡,熔断,限流,网关,配置中心', '技术', '微服务架构相关术语', 0, 1, 8, '2025-11-04 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `ai_domain_dictionary` VALUES (10, '容器', 'Docker,Kubernetes,K8s,Pod,容器编排,镜像,Harbor,Helm,Service,Deployment', '运维', '容器化和容器编排相关术语', 0, 1, 8, '2025-11-06 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `ai_domain_dictionary` VALUES (11, '前端', 'Vue,React,Angular,JavaScript,TypeScript,CSS,HTML,Webpack,Vite,组件,路由,状态管理', '技术', '前端开发相关术语', 0, 1, 7, '2025-11-08 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `ai_domain_dictionary` VALUES (12, '测试', '单元测试,集成测试,压力测试,自动化测试,测试用例,Bug,缺陷,回归测试,冒烟测试,UAT', '质量', '软件测试相关术语', 0, 1, 6, '2025-11-11 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `ai_domain_dictionary` VALUES (13, 'DevOps', 'CI/CD,Jenkins,GitLab,流水线,自动化部署,监控,日志,告警,SRE,可观测性', '运维', 'DevOps和持续集成相关术语', 0, 1, 7, '2025-11-14 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `ai_domain_dictionary` VALUES (14, '网络', 'TCP,UDP,HTTP,HTTPS,DNS,CDN,负载均衡,防火墙,VPN,代理,带宽,延迟', '基础设施', '网络通信相关术语', 0, 1, 6, '2025-11-16 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `ai_domain_dictionary` VALUES (15, '产品', '需求,PRD,原型,用户故事,MVP,迭代,版本,上线,灰度,AB测试,用户体验,交互设计', '产品', '产品管理相关术语', 1, 1, 5, '2025-11-18 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `ai_domain_dictionary` VALUES (16, '财务', '预算,成本,利润,营收,ROI,现金流,资产负债,损益表,审计,税务,发票,报销', '财务', '财务管理相关术语', 1, 1, 5, '2025-11-21 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `ai_domain_dictionary` VALUES (17, '人力资源', '招聘,面试,入职,离职,绩效,考核,薪酬,福利,培训,晋升,组织架构,人才盘点', '人力', '人力资源管理相关术语', 1, 1, 5, '2025-11-24 15:55:38', '2025-11-26 15:55:38');

-- ----------------------------
-- Table structure for knowledge_base
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_knowledge_base`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `library_id` bigint NULL DEFAULT NULL COMMENT '所属知识库ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件路径',
  `pdf_file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'PDF文件路径（用于预览）',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件类型',
  `vector_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量ID（Pinecone中的ID）',
  `segment_count` int NULL DEFAULT NULL COMMENT '文档分段数量',
  `vector_dimension` int NULL DEFAULT NULL COMMENT '向量维度',
  `status` int NULL DEFAULT 0 COMMENT '处理状态：0-待处理，1-处理中，2-处理成功，3-处理失败',
  `processing_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '处理状态：pending(待配置), configured(已配置), processing(处理中), completed(已完成), failed(失败)',
  `config_completed` tinyint(1) NULL DEFAULT 0 COMMENT '是否完成配置',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `process_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库分组',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '知识库标签（JSON数组）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库描述',
  `usage_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `hit_count` int NULL DEFAULT 0 COMMENT '命中次数',
  `last_used_time` datetime NULL DEFAULT NULL COMMENT '最后使用时间',
  `parse_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文档解析方式: POI, PDFBox, Text',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件内容SHA-256哈希值（用于增量更新检测）',
  `last_processed_time` datetime NULL DEFAULT NULL COMMENT '上次处理时间',
  `need_reprocess` tinyint(1) NULL DEFAULT 0 COMMENT '是否需要重新处理',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_upload_time`(`upload_time` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_usage_count`(`usage_count` ASC) USING BTREE,
  INDEX `idx_last_used_time`(`last_used_time` ASC) USING BTREE,
  INDEX `idx_library_id`(`library_id` ASC) USING BTREE,
  INDEX `idx_kb_content_hash`(`content_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 134 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_base
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_knowledge_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `segment_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'general' COMMENT '分段模式：general(通用), parent_child(父子分段)',
  `segment_separator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '\n\n' COMMENT '分段标识符',
  `segment_max_length` int NOT NULL DEFAULT 800 COMMENT '分段最大长度（字符数，800字符确保题库问答对完整，技术文档可用500，小说可用1500）',
  `segment_overlap_length` int NOT NULL DEFAULT 100 COMMENT '分段重叠长度（字符数，100字符保证上下文连贯性）',
  `chunking_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'fixed' COMMENT '分片策略: fixed(固定大小), adaptive(自适应), document_type(按文档类型)',
  `document_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'general' COMMENT '文档类型: general(通用), faq(问答), table(表格), code(代码), technical(技术文档)',
  `faq_chunk_size` int NULL DEFAULT 400 COMMENT 'FAQ分片大小(字符)',
  `table_chunk_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'by_row' COMMENT '表格分片策略: by_row(按行), by_table(整表), by_cell(按单元格)',
  `code_chunk_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'by_function' COMMENT '代码分片策略: by_function(按函数), by_class(按类), by_file(按文件)',
  `technical_chunk_size` int NULL DEFAULT 1200 COMMENT '技术文档分片大小(字符)',
  `enable_smart_boundary` tinyint(1) NULL DEFAULT 1 COMMENT '启用智能边界检测(避免切断句子)',
  `preprocess_replace_spaces` tinyint(1) NULL DEFAULT 1 COMMENT '替换连续空格、换行、制表符',
  `preprocess_remove_urls` tinyint(1) NULL DEFAULT 1 COMMENT '删除URL和邮箱地址',
  `preprocess_remove_extra_newlines` tinyint(1) NULL DEFAULT 1 COMMENT '删除多余换行',
  `index_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'high_quality' COMMENT '索引方式：high_quality(高质量), economy(经济)',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '嵌入模型名称',
  `retrieval_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'vector' COMMENT '检索模式：vector(向量), keyword(关键词), hybrid(混合)',
  `retrieval_top_k` int NULL DEFAULT 3 COMMENT '检索Top K数量',
  `rerank_enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用重排序',
  `rerank_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '重排序模型',
  `qa_mode` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用Q&A模式',
  `qa_extraction_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'Q&A提取提示词',
  `preprocess_remove_special_chars` tinyint(1) NULL DEFAULT 0 COMMENT '删除特殊字符',
  `preprocess_remove_table_desc` tinyint(1) NULL DEFAULT 0 COMMENT '删除表格描述',
  `preprocess_remove_header_footer` tinyint(1) NULL DEFAULT 0 COMMENT '删除页眉页脚',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_knowledge_id`(`knowledge_id` ASC) USING BTREE,
  INDEX `idx_segment_mode`(`segment_mode` ASC) USING BTREE,
  INDEX `idx_index_mode`(`index_mode` ASC) USING BTREE,
  INDEX `idx_chunking_strategy`(`chunking_strategy` ASC) USING BTREE,
  INDEX `idx_document_type`(`document_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 97 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库配置表 - 包含分片策略、文档类型识别、预处理规则等配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_config
-- ----------------------------
INSERT INTO `ai_knowledge_config` VALUES (85, 122, 'general', '\n\n', 1024, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'high_quality', NULL, 'vector', 3, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 09:48:23', '2026-01-23 09:48:23');
INSERT INTO `ai_knowledge_config` VALUES (86, 123, 'general', '\n\n', 1200, 200, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 0, 0, 'high_quality', NULL, 'vector', 8, 1, NULL, 0, NULL, 0, 0, 0, '2026-01-23 09:49:31', '2026-01-23 09:49:32');
INSERT INTO `ai_knowledge_config` VALUES (87, 124, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 09:49:49', '2026-01-23 09:49:50');
INSERT INTO `ai_knowledge_config` VALUES (88, 125, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 09:51:21', '2026-01-23 09:51:22');
INSERT INTO `ai_knowledge_config` VALUES (89, 126, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:02:20', '2026-01-23 10:02:21');
INSERT INTO `ai_knowledge_config` VALUES (90, 127, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:02:31', '2026-01-23 10:02:31');
INSERT INTO `ai_knowledge_config` VALUES (91, 128, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:11:02', '2026-01-23 10:11:03');
INSERT INTO `ai_knowledge_config` VALUES (92, 129, 'general', '\n\n', 1024, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'high_quality', NULL, 'vector', 3, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:11:24', '2026-01-23 10:11:25');
INSERT INTO `ai_knowledge_config` VALUES (93, 130, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:12:00', '2026-01-23 10:12:01');
INSERT INTO `ai_knowledge_config` VALUES (94, 131, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:15:54', '2026-01-23 10:15:55');
INSERT INTO `ai_knowledge_config` VALUES (95, 132, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:53:04', '2026-01-23 10:53:05');
INSERT INTO `ai_knowledge_config` VALUES (96, 133, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 11:47:50', '2026-01-23 11:47:51');

-- ----------------------------
-- Table structure for knowledge_config_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_knowledge_config_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `template_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模板描述',
  `template_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板类型：general(通用), technical(技术文档), legal(法律), medical(医疗)',
  `config_json` json NOT NULL COMMENT '配置JSON',
  `is_system` tinyint(1) NULL DEFAULT 0 COMMENT '是否系统预设模板',
  `use_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `is_recommended` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否推荐模板',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_template_type`(`template_type` ASC) USING BTREE,
  INDEX `idx_use_count`(`use_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库配置模板表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_config_template
-- ----------------------------
INSERT INTO `ai_knowledge_config_template` VALUES (1, '标准文档', '适用于一般文档、技术手册等，平衡性能和准确度', 'general', '{\"indexMode\": \"high_quality\", \"segmentMode\": \"general\", \"rerankEnabled\": true, \"retrievalMode\": \"vector\", \"retrievalTopK\": 10, \"segmentMaxLength\": 800, \"preprocessRemoveUrls\": false, \"segmentOverlapLength\": 100, \"preprocessReplaceSpaces\": true, \"preprocessRemoveExtraNewlines\": true}', 1, 0, 1, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `ai_knowledge_config_template` VALUES (2, '题库/QA精准模式', '适用于题库、问答对等短文本，确保每道题独立检索', 'general', '{\"indexMode\": \"high_quality\", \"segmentMode\": \"qa\", \"rerankEnabled\": true, \"retrievalMode\": \"vector\", \"retrievalTopK\": 15, \"segmentMaxLength\": 400, \"preprocessRemoveUrls\": true, \"segmentOverlapLength\": 50, \"preprocessReplaceSpaces\": true, \"preprocessRemoveExtraNewlines\": true}', 1, 0, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `ai_knowledge_config_template` VALUES (3, '长文档深度模式', '适用于长篇文章、研究报告等，保留更多上下文', 'general', '{\"indexMode\": \"high_quality\", \"segmentMode\": \"general\", \"rerankEnabled\": true, \"retrievalMode\": \"vector\", \"retrievalTopK\": 8, \"segmentMaxLength\": 1200, \"preprocessRemoveUrls\": false, \"segmentOverlapLength\": 200, \"preprocessReplaceSpaces\": true, \"preprocessRemoveExtraNewlines\": false}', 1, 1, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `ai_knowledge_config_template` VALUES (4, '代码技术文档', '适用于代码、API文档等技术内容', 'technical', '{\"indexMode\": \"high_quality\", \"segmentMode\": \"code\", \"rerankEnabled\": false, \"retrievalMode\": \"vector\", \"retrievalTopK\": 12, \"segmentMaxLength\": 600, \"preprocessRemoveUrls\": false, \"segmentOverlapLength\": 80, \"preprocessReplaceSpaces\": false, \"preprocessRemoveExtraNewlines\": false}', 1, 0, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `ai_knowledge_config_template` VALUES (5, '经济快速模式', '降低资源消耗，适合大批量文档或测试环境', 'general', '{\"indexMode\": \"economy\", \"segmentMode\": \"general\", \"rerankEnabled\": false, \"retrievalMode\": \"vector\", \"retrievalTopK\": 5, \"segmentMaxLength\": 500, \"preprocessRemoveUrls\": true, \"segmentOverlapLength\": 50, \"preprocessReplaceSpaces\": true, \"preprocessRemoveExtraNewlines\": true}', 1, 71, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');

-- ----------------------------
-- Table structure for knowledge_library
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_knowledge_library`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库描述',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '?' COMMENT '知识库图标',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库分类（如：技术文档、产品手册、FAQ等）',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '标签（JSON数组格式）',
  `document_count` int NULL DEFAULT 0 COMMENT '文档数量',
  `total_segments` int NULL DEFAULT 0 COMMENT '总分段数',
  `total_size` bigint NULL DEFAULT 0 COMMENT '总文件大小（字节）',
  `usage_count` int NULL DEFAULT 0 COMMENT '使用次数（被检索次数）',
  `hit_count` int NULL DEFAULT 0 COMMENT '命中次数',
  `last_used_time` datetime NULL DEFAULT NULL COMMENT '最后使用时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'active' COMMENT '状态：active(正常), disabled(禁用), archived(归档)',
  `is_public` tinyint(1) NULL DEFAULT 1 COMMENT '是否公开（预留多租户）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_usage_count`(`usage_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_library
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_library_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_knowledge_library_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `library_id` bigint NOT NULL COMMENT '知识库ID',
  `segment_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'general' COMMENT '分段模式：general(通用), qa(问答), code(代码)',
  `segment_separator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '\n\n' COMMENT '分段标识符',
  `segment_max_length` int NOT NULL DEFAULT 800 COMMENT '分段最大长度',
  `segment_overlap_length` int NOT NULL DEFAULT 100 COMMENT '分段重叠长度',
  `preprocess_replace_spaces` tinyint(1) NULL DEFAULT 1 COMMENT '替换连续空格',
  `preprocess_remove_urls` tinyint(1) NULL DEFAULT 1 COMMENT '删除URL',
  `preprocess_remove_extra_newlines` tinyint(1) NULL DEFAULT 1 COMMENT '删除多余换行',
  `index_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'high_quality' COMMENT '索引模式：high_quality, economy',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Embedding模型',
  `retrieval_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'hybrid' COMMENT '检索模式：vector, keyword, hybrid',
  `retrieval_top_k` int NULL DEFAULT 10 COMMENT '检索返回数量',
  `rerank_enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用Rerank',
  `rerank_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Rerank模型',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_library_id`(`library_id` ASC) USING BTREE,
  CONSTRAINT `fk_library_config` FOREIGN KEY (`library_id`) REFERENCES `ai_knowledge_library` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_library_config
-- ----------------------------

-- ----------------------------
-- Table structure for model_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_model_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型提供商(openai/ollama/dashscope)',
  `model_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'chat' COMMENT '模型类型(chat/embedding/multimodal/reranker)',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型名称',
  `api_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API密钥',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API基础URL',
  `temperature` double NULL DEFAULT 0.7 COMMENT '温度参数(0-2)',
  `max_tokens` int NULL DEFAULT 2000 COMMENT '最大Token数',
  `timeout` int NULL DEFAULT 60 COMMENT '超时时间(秒)',
  `streaming_supported` tinyint(1) NULL DEFAULT 1 COMMENT '是否支持流式输出',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `is_default` tinyint(1) NULL DEFAULT 0 COMMENT '是否为默认模型',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注说明',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `input_price` decimal(10, 6) NULL DEFAULT 0.001000 COMMENT '输入价格（元/1000 tokens）',
  `output_price` decimal(10, 6) NULL DEFAULT 0.002000 COMMENT '输出价格（元/1000 tokens）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_provider`(`provider` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_is_default`(`is_default` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模型配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of model_config
-- ----------------------------
INSERT INTO `ai_model_config` VALUES (10, '通义千问-多模态Embedding', 'dashscope', 'embedding', 'text-embedding-v3', NULL, NULL, NULL, NULL, 60, 0, 1, 1, '通义千问多模态 Embedding 模型，支持图片和文本的联合向量化，用于图文混合搜索', '2025-11-21 17:12:03', '2026-01-23 15:51:50', 0.000500, 0.000000);
INSERT INTO `ai_model_config` VALUES (11, '通义千问-VL-Plus', 'dashscope', 'chat', 'qwen-vl-plus', NULL, NULL, 0.7, 2000, 60, 1, 1, 1, '通义千问视觉理解模型Plus版本，支持图片内容识别和描述，用于文档图片的多模态理解', '2025-11-22 12:16:42', '2026-01-23 15:51:50', 0.001000, 0.002000);
INSERT INTO `ai_model_config` VALUES (15, 'Qwen3-Reranker', 'dashscope', 'reranker', 'qwen3-rerank', NULL, 'https://dashscope.aliyuncs.com/api/v1', NULL, NULL, 60, 0, 1, 1, 'Qwen3 重排序模型，用于提升检索结果的相关性排序，支持中英文等100+语言', '2026-01-22 15:34:36', '2026-01-23 15:51:50', 0.000100, 0.000000);

-- ----------------------------
-- Table structure for query_history
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_query_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话ID',
  `natural_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '自然语言查询',
  `generated_sql` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '生成的SQL语句',
  `query_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '查询类型: select, aggregate, join, analysis',
  `tables_involved` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '涉及的表(逗号分隔)',
  `result_count` int NULL DEFAULT 0 COMMENT '结果行数',
  `execution_time` int NULL DEFAULT 0 COMMENT '执行时间(毫秒)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'success' COMMENT '执行状态: success, failed, timeout',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `analysis_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分析类型: basic, trend, correlation, ranking',
  `chart_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图表类型',
  `has_insight` tinyint(1) NULL DEFAULT 0 COMMENT '是否生成洞察',
  `token_used` int NULL DEFAULT 0 COMMENT 'LLM消耗的Token数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_datasource_id`(`datasource_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '查询历史表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of query_history
-- ----------------------------

-- ----------------------------
-- Table structure for reference_feedback
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_reference_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint NULL DEFAULT NULL COMMENT '知识库ID',
  `file_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件名',
  `page_number` int NULL DEFAULT NULL COMMENT '页码',
  `segment_index` int NULL DEFAULT NULL COMMENT '分片索引',
  `user_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '用户查询',
  `rerank_score` double NULL DEFAULT NULL COMMENT '重排分数',
  `vector_score` double NULL DEFAULT NULL COMMENT '向量相似度',
  `feedback_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '反馈类型：accurate(准确), inaccurate(不准确)',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '智能体ID',
  `memory_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会话ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_base_id`(`knowledge_base_id` ASC) USING BTREE,
  INDEX `idx_feedback_type`(`feedback_type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '参考来源反馈表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of reference_feedback
-- ----------------------------

-- ----------------------------
-- Table structure for sql_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_sql_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `natural_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '自然语言示例',
  `ai_sql_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SQL模板',
  `query_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '查询类型',
  `complexity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'simple' COMMENT '复杂度: simple, medium, complex',
  `table_pattern` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表名模式',
  `usage_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `success_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '成功率(%)',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_query_type`(`query_type` ASC) USING BTREE,
  INDEX `idx_complexity`(`complexity` ASC) USING BTREE,
  INDEX `idx_usage_count`(`usage_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SQL模板表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sql_template
-- ----------------------------
INSERT INTO `ai_sql_template` VALUES (1, '简单查询-TOP N', '查询销售额最高的10个产品', 'SELECT * FROM {table} ORDER BY {metric_field} DESC LIMIT {limit}', 'ranking', 'simple', NULL, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `ai_sql_template` VALUES (2, '时间范围查询', '查询上个月的数据', 'SELECT * FROM {table} WHERE {time_field} >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH) AND {time_field} < CURDATE()', 'time_range', 'simple', NULL, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `ai_sql_template` VALUES (3, '聚合统计', '统计每个类别的总数', 'SELECT {category_field}, COUNT(*) as count FROM {table} GROUP BY {category_field}', 'aggregate', 'simple', NULL, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `ai_sql_template` VALUES (4, '平均值计算', '计算平均销售额', 'SELECT AVG({metric_field}) as avg_value FROM {table}', 'aggregate', 'simple', NULL, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `ai_sql_template` VALUES (5, '多条件筛选', '查询价格大于100且库存小于50的商品', 'SELECT * FROM {table} WHERE {field1} > {value1} AND {field2} < {value2}', 'filter', 'medium', NULL, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');



-- ----------------------------
-- Table structure for table_metadata
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_table_metadata`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '表名',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表注释',
  `table_schema` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '表结构(JSON格式)',
  `column_count` int NULL DEFAULT 0 COMMENT '字段数量',
  `row_count` bigint NULL DEFAULT 0 COMMENT '行数(估算)',
  `data_size` bigint NULL DEFAULT 0 COMMENT '数据大小(字节)',
  `has_primary_key` tinyint(1) NULL DEFAULT 0 COMMENT '是否有主键',
  `has_time_field` tinyint(1) NULL DEFAULT 0 COMMENT '是否有时间字段',
  `time_field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '时间字段名',
  `numeric_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数值型字段列表(JSON)',
  `category_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '类别型字段列表(JSON)',
  `indexed_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '索引字段列表(JSON)',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后同步时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_datasource_table`(`datasource_id` ASC, `table_name` ASC) USING BTREE,
  INDEX `idx_datasource_id`(`datasource_id` ASC) USING BTREE,
  INDEX `idx_last_sync_time`(`last_sync_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '表元数据缓存表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of table_metadata
-- ----------------------------

-- ----------------------------
-- Table structure for token_usage_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_token_usage_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint NULL DEFAULT NULL COMMENT '会话ID',
  `message_id` bigint NULL DEFAULT NULL COMMENT '消息ID',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '智能体ID',
  `workflow_id` bigint NULL DEFAULT NULL COMMENT '工作流ID',
  `workflow_execution_id` bigint NULL DEFAULT NULL COMMENT '工作流执行ID',
  `workflow_node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工作流节点ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户ID',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名称',
  `model_provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型提供商',
  `input_tokens` int NULL DEFAULT 0 COMMENT '输入token数',
  `output_tokens` int NULL DEFAULT 0 COMMENT '输出token数',
  `total_tokens` int NULL DEFAULT 0 COMMENT '总token数',
  `cost` decimal(10, 6) NULL DEFAULT NULL COMMENT '费用（元）',
  `request_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求类型：chat/embedding_query/embedding_document/workflow_llm/workflow_classifier/workflow_extractor/workflow_question',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_model_name`(`model_name` ASC) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE,
  INDEX `idx_workflow_execution_id`(`workflow_execution_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 390 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Token使用记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of token_usage_log
-- ----------------------------

-- ----------------------------
-- Table structure for token_usage_summary
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_token_usage_summary`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '智能体ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户ID',
  `stat_date` date NULL DEFAULT NULL COMMENT '统计日期',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名称',
  `total_requests` int NULL DEFAULT 0 COMMENT '请求次数',
  `total_input_tokens` bigint NULL DEFAULT 0 COMMENT '总输入token',
  `total_output_tokens` bigint NULL DEFAULT 0 COMMENT '总输出token',
  `total_tokens` bigint NULL DEFAULT 0 COMMENT '总token',
  `total_cost` decimal(12, 6) NULL DEFAULT NULL COMMENT '总费用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_user_date_model`(`agent_id` ASC, `user_id` ASC, `stat_date` ASC, `model_name` ASC) USING BTREE,
  INDEX `idx_stat_date`(`stat_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Token使用统计汇总表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of token_usage_summary
-- ----------------------------

-- ----------------------------
-- Table structure for tool_call_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_tool_call_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `conversation_id` bigint NULL DEFAULT NULL COMMENT '会话ID',
  `message_id` bigint NULL DEFAULT NULL COMMENT '消息ID',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '智能体ID',
  `tool_id` bigint NULL DEFAULT NULL COMMENT '工具ID',
  `tool_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具名称',
  `input_params` json NULL COMMENT '输入参数',
  `output_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '输出结果',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'success' COMMENT '状态：pending/running/success/failed/timeout',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `duration_ms` int NULL DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_tool_name`(`tool_name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工具调用日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tool_call_log
-- ----------------------------

-- ----------------------------
-- Table structure for workflow
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_workflow`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作流名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工作流描述',
  `graph_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工作流图定义(JSON)',
  `variables` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '全局变量定义(JSON)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'draft' COMMENT '状态: draft-草稿, published-已发布, disabled-已禁用',
  `version` int NULL DEFAULT 1 COMMENT '版本号',
  `enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作流定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of workflow
-- ----------------------------

-- ----------------------------
-- Table structure for workflow_execution
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_workflow_execution`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'running' COMMENT '执行状态: running-执行中, completed-已完成, failed-失败, cancelled-已取消',
  `input_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '输入参数(JSON)',
  `output_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '输出结果(JSON)',
  `execution_log` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '执行日志(JSON数组)',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `current_node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当前执行到的节点ID',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作流执行记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of workflow_execution
-- ----------------------------

-- ----------------------------
-- Table structure for workflow_version
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_workflow_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `version` int NOT NULL COMMENT '版本号',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '版本描述',
  `graph_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工作流图数据快照(JSON)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE,
  INDEX `idx_version`(`version` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作流版本表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of workflow_version
-- ----------------------------

-- =====================================================================
-- AI 模块菜单权限数据（占位，完整菜单权限见 91_菜单权限_CMS.sql 补充）
-- =====================================================================
-- AI 模块菜单由 91_菜单权限_CMS.sql 或后续版本补充，本脚本仅包含表结构

-- =====================================================================
-- AI 模块菜单权限（墨韵 v7.5 新增）
-- 菜单 ID 起点：5000（与 RuoYi 1-1060 / CMS 2000+ / 其他模块错开）
-- =====================================================================

ALTER TABLE sys_menu AUTO_INCREMENT = 5000;

-- ---------------------------------------------------------------------
-- 一、智能AI（一级目录 M）
-- 统一一级菜单名"智能AI"，整合所有 AI 子模块
-- ---------------------------------------------------------------------
-- 兼容已部署环境：若存在旧名"AI智能中心"则改名为"智能AI"
UPDATE sys_menu SET menu_name = '智能AI', remark = '智能AI目录'
WHERE menu_name = 'AI智能中心' AND parent_id = 0;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能AI', 0, 11, 'ai', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'chart', 'admin', NOW(), '智能AI目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0);
SELECT @ai_parent_id := menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1;

-- ---------------------------------------------------------------------
-- 二、智能体管理（二级菜单 C + F 按钮权限）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体管理', @ai_parent_id, 1, 'agent', 'ai/agent/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:agent:list', 'edit', 'admin', NOW(), '智能体管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:list');
SELECT @agent_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:agent:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体查询', @agent_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:agent:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体新增', @agent_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:agent:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体修改', @agent_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:agent:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体删除', @agent_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:agent:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体测试', @agent_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:agent:test', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:test');

-- ---------------------------------------------------------------------
-- 三、知识库管理（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识库管理', @ai_parent_id, 2, 'knowledge-base', 'ai/knowledge-base/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:knowledge-base:list', 'documentation', 'admin', NOW(), '知识库管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:list');
SELECT @kb_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识库查询', @kb_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识库新增', @kb_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识库修改', @kb_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识库删除', @kb_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文档上传', @kb_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:upload', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:upload');

-- ---------------------------------------------------------------------
-- 四、知识文库（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识文库', @ai_parent_id, 3, 'knowledge-library', 'ai/knowledge-library/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:knowledge-library:list', 'tree-table', 'admin', NOW(), '知识文库菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:list');
SELECT @kbl_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文库查询', @kbl_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文库新增', @kbl_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文库修改', @kbl_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文库删除', @kbl_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:remove');

-- ---------------------------------------------------------------------
-- 五、模型配置（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模型配置', @ai_parent_id, 4, 'model-config', 'ai/model-config/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:model-config:list', 'monitor', 'admin', NOW(), '模型配置菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:list');
SELECT @model_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:model-config:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模型查询', @model_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:model-config:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模型新增', @model_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:model-config:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模型修改', @model_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:model-config:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模型删除', @model_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:model-config:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '连接测试', @model_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:model-config:test', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:test');

-- ---------------------------------------------------------------------
-- 六、工具管理（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工具管理', @ai_parent_id, 5, 'tool', 'ai/tool/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:tool:list', 'tool', 'admin', NOW(), '工具管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:tool:list');
SELECT @tool_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:tool:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工具查询', @tool_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:tool:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:tool:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工具新增', @tool_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:tool:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:tool:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工具修改', @tool_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:tool:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:tool:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工具删除', @tool_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:tool:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:tool:remove');

-- ---------------------------------------------------------------------
-- 七、工作流管理（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流管理', @ai_parent_id, 6, 'workflow', 'ai/workflow/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:workflow:list', 'chart', 'admin', NOW(), '工作流管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:list');
SELECT @wf_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:workflow:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流查询', @wf_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流新增', @wf_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流修改', @wf_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流删除', @wf_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流执行', @wf_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow:execute', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:execute');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'AI生成工作流', @wf_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow-generator:generate', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow-generator:generate');

-- ---------------------------------------------------------------------
-- 八、领域词典（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '领域词典', @ai_parent_id, 7, 'dictionary', 'ai/dictionary/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:domain-dictionary:list', 'dict', 'admin', NOW(), '领域词典菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:list');
SELECT @dict_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '词典查询', @dict_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '词典新增', @dict_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '词典修改', @dict_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '词典删除', @dict_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:remove');

-- ---------------------------------------------------------------------
-- 九、数据源管理（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据源管理', @ai_parent_id, 8, 'datasource', 'ai/datasource/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:datasource:list', 'druid', 'admin', NOW(), '数据源管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:list');
SELECT @ds_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:datasource:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据源查询', @ds_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据源新增', @ds_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据源修改', @ds_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据源删除', @ds_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '连接测试', @ds_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:test', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:test');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '元数据同步', @ds_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:sync', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:sync');

-- ---------------------------------------------------------------------
-- 十、Token统计（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Token统计', @ai_parent_id, 9, 'token-usage', 'ai/token-usage/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:token-usage:list', 'money', 'admin', NOW(), 'Token统计菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:token-usage:list');
SELECT @token_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:token-usage:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '统计查询', @token_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:token-usage:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:token-usage:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '统计导出', @token_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:token-usage:export', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:token-usage:export');

-- ---------------------------------------------------------------------
-- 十一、智能查询/数据分析（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据分析', @ai_parent_id, 10, 'query', 'ai/query/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:data-analysis:list', 'icon', 'admin', NOW(), '智能数据分析菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:data-analysis:list');
SELECT @da_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:data-analysis:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询查询', @da_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:data-analysis:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:data-analysis:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'SQL生成', @da_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:data-analysis:sql', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:data-analysis:sql');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告生成', @da_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:data-analysis:report', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:data-analysis:report');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '图表生成', @da_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:diagram:generate', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:diagram:generate');

-- ---------------------------------------------------------------------
-- 十二、AI数据大屏（二级菜单 C）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'AI数据大屏', @ai_parent_id, 11, 'dashboard', 'ai/dashboard/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:dashboard:list', 'chart', 'admin', NOW(), 'AI数据大屏菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:dashboard:list');

-- ---------------------------------------------------------------------
-- 十三、AI图表对话（二级菜单 C）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '架构图生成', @ai_parent_id, 12, 'diagram/chat', 'ai/diagram/chat', NULL, 1, 0, 'C', '0', '0', 'cms:ai:diagram:list', 'build', 'admin', NOW(), 'AI架构图生成菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:diagram:list');

-- ---------------------------------------------------------------------
-- 十四、超级管理员角色自动关联全部 AI 菜单（admin 角色 role_id=1）
-- ---------------------------------------------------------------------
-- 对 role_id=1 的超级管理员，自动授予所有以 cms:ai: 开头的权限串对应的菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms LIKE 'cms:ai:%'
  AND m.menu_type IN ('C', 'F')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
-- 同时授予一级目录 M 菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.menu_name = '智能AI' AND m.parent_id = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

-- ---------------------------------------------------------------------
-- 十五、AI 对话权限码补全（v7.5.1）
-- 后端 ChatController 的 /stream、/abort、/regenerate 接口需要 cms:ai:chat:list 权限。
-- 该权限串挂在"智能体管理"二级菜单（perms='cms:ai:agent:list'）下作为按钮权限 F。
-- ---------------------------------------------------------------------
SET @agent_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:ai:agent:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能对话使用', @agent_menu_id, 10, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:chat:list', '#', 'admin', NOW(), 'AI对话/中断/重新生成接口权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:chat:list');

-- 超级管理员自动授权新增 chat 权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms = 'cms:ai:chat:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

-- =====================================================================
-- v7.5 AI 模块菜单权限 — 完成
-- =====================================================================


-- =====================================================================
-- 三、字段补齐段（ALTER TABLE，幂等，兼容 MySQL 8.0）
-- 说明：升级脚本中的表结构变更，对已建表做字段补齐
-- =====================================================================

-- ---------------------------------------------------------------
-- 来源: 95_升级脚本_v6.4.sql
-- ---------------------------------------------------------------
-- =============================================================================
-- v6.4 升级脚本：首页运营指标修复
--   1. sys_logininfor 加 user_type 字段（区分后台/门户登录来源）
--   2. portal_category 加 category_type 字段（区分文章栏目/特殊页面）
--   3. portal_category 种子数据回填 category_type（散文天地/技术笔记=article，其余按 nav_route_type 判定）
-- 说明：使用 information_schema 判断列是否存在，幂等可重复执行
-- =============================================================================

-- ---------- 1. sys_logininfor.user_type ----------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_logininfor'
      AND COLUMN_NAME = 'user_type'
);
SET @sql := IF(@col_exists = 0,
               'ALTER TABLE sys_logininfor ADD COLUMN user_type varchar(10) DEFAULT ''sys'' COMMENT ''登录来源类型（sys=后台用户 portal=门户用户）'' AFTER status',
               'SELECT ''sys_logininfor.user_type 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 补索引（幂等）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_logininfor' AND INDEX_NAME = 'idx_sys_logininfor_ut'
);
SET @sql := IF(@idx_exists = 0,
               'ALTER TABLE sys_logininfor ADD KEY idx_sys_logininfor_ut (user_type)',
               'SELECT ''idx_sys_logininfor_ut 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ---------- 2. portal_category.category_type ----------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'portal_category'
      AND COLUMN_NAME = 'category_type'
);
SET @sql := IF(@col_exists = 0,
               'ALTER TABLE portal_category ADD COLUMN category_type varchar(20) NOT NULL DEFAULT ''article'' COMMENT ''栏目内容类型（article=文章栏目可发布文章 special=特殊页面不发布文章）'' AFTER nav_route_path',
               'SELECT ''portal_category.category_type 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 补索引（幂等）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_category' AND INDEX_NAME = 'idx_category_type'
);
SET @sql := IF(@idx_exists = 0,
               'ALTER TABLE portal_category ADD KEY idx_category_type (category_type)',
               'SELECT ''idx_category_type 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ---------- 3. 回填 portal_category.category_type ----------
-- 规则：nav_route_type='category' 视为文章栏目（article）；home/static/external 视为特殊页面（special）
-- 幂等：仅对 category_type 仍为默认值 'article' 但 nav_route_type<>'category' 的行做修正
UPDATE portal_category
SET category_type = 'special'
WHERE nav_route_type <> 'category'
  AND (category_type IS NULL OR category_type = 'article');

-- 二次兜底：nav_route_type='category' 但被误标为 special 的，修正回 article
UPDATE portal_category
SET category_type = 'article'
WHERE nav_route_type = 'category'
  AND (category_type IS NULL OR category_type = '' OR category_type = 'special');

-- 校验
SELECT '=== portal_category.category_type 分布 ===' AS info;
SELECT category_type, nav_route_type, COUNT(*) AS cnt
FROM portal_category
GROUP BY category_type, nav_route_type
ORDER BY category_type, nav_route_type;


-- ---------------------------------------------------------------
-- 来源: 96_升级脚本_v6.7.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v6.7 升级脚本：统一审核字段 + 话题审核流 + 举报扩展 + 敏感词过滤
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（使用 IF NOT EXISTS / information_schema 判断）
-- 幂等性核查（P2-1）：✅ 已幂等 — DDL 用 information_schema+IF+PREPARE 守护；INSERT 均有 NOT EXISTS/ON DUPLICATE 守护；
--   @var 仅传递父菜单 ID 到 INSERT 值位，NOT EXISTS 条件仅基于 perms，@var 丢失不产生重复（仅 parent_id 可能写 NULL，已有 UPDATE 兜底修复）。
-- ====================================================================

-- ----------------------------------------------------------------
-- 1. 统一审核字段：为 portal_article / portal_topic / portal_column 增加审核字段
--    字段：auditor_id（审核人）、audit_remark（审核意见/驳回原因）、audit_time（审核时间）
--    设计：独立于 BaseEntity.remark，专用于审核记录，语义清晰、可追溯
-- ----------------------------------------------------------------

-- 1.1 portal_article
SET @db := DATABASE();
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_article' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_article ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID）'' AFTER status',
  'SELECT ''portal_article.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_article' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_article ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因'' AFTER auditor_id',
  'SELECT ''portal_article.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_article' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_article ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_article.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 portal_topic
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_topic' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_topic ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID）'' AFTER status',
  'SELECT ''portal_topic.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_topic' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_topic ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因'' AFTER auditor_id',
  'SELECT ''portal_topic.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_topic' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_topic ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_topic.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 portal_column
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_column' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_column ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID）'' AFTER status',
  'SELECT ''portal_column.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_column' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_column ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因'' AFTER auditor_id',
  'SELECT ''portal_column.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_column' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_column ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_column.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 话题状态默认值改为 pending（新发起话题需审核）
-- 注意：仅修改 DEFAULT 值，不影响存量数据
ALTER TABLE portal_topic MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending 待审核/active 活跃/archived 归档/deleted 删除/rejected 审核驳回';

-- 1.5 专栏状态注释更新（默认仍为 draft，专栏可由认证创作者直发 published）
ALTER TABLE portal_column MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft 草稿/pending 待审核/published 已发布/archived 归档/rejected 审核驳回';

-- ----------------------------------------------------------------
-- 2. 举报扩展：portal_report.target_type 支持 topic / topic_post / column
--    说明：target_type 为 VARCHAR，无需 DDL 变更，仅更新注释
-- ----------------------------------------------------------------
ALTER TABLE portal_report MODIFY COLUMN target_type VARCHAR(32) NULL DEFAULT NULL COMMENT '举报目标类型：article=文章/comment=评论/user=用户/topic=话题/topic_post=话题观点/topic_comment=话题评论/column=专栏（为空表示通用举报，仅 target_url）';

-- ----------------------------------------------------------------
-- 3. 敏感词过滤基础设施
-- 3.1 敏感词库表：sys_sensitive_word（管理员可维护，运行时加载到内存）
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_sensitive_word (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  word         VARCHAR(128) NOT NULL COMMENT '敏感词',
  category     VARCHAR(32)  NULL DEFAULT NULL COMMENT '分类：politics=政治/porn=色情/ad=广告/insult=辱骂/other=其他',
  status       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0=启用 1=禁用',
  create_by    VARCHAR(64)  NULL DEFAULT NULL COMMENT '创建者',
  create_time  DATETIME     NULL DEFAULT NULL COMMENT '创建时间',
  update_by    VARCHAR(64)  NULL DEFAULT NULL COMMENT '更新者',
  update_time  DATETIME     NULL DEFAULT NULL COMMENT '更新时间',
  remark       VARCHAR(255) NULL DEFAULT NULL COMMENT '备注',
  del_flag     CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标记：0=存在 2=删除（BaseEntity 逻辑删除）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_word (word),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词库';

-- 3.2 敏感词命中记录表：sys_sensitive_word_log（用于审计与误判复核）
CREATE TABLE IF NOT EXISTS sys_sensitive_word_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  biz_type      VARCHAR(32)  NOT NULL COMMENT '业务类型：article/column/topic/topic_post/topic_comment/report',
  biz_id        BIGINT       NULL DEFAULT NULL COMMENT '业务主键ID',
  user_id       BIGINT       NULL DEFAULT NULL COMMENT '提交人ID（portal_user.id）',
  content       TEXT         NULL COMMENT '被检测的原始内容片段（截断）',
  hit_words     VARCHAR(500) NULL DEFAULT NULL COMMENT '命中的敏感词列表（逗号分隔）',
  hit_count     INT          NOT NULL DEFAULT 0 COMMENT '命中数量',
  action        VARCHAR(16)  NOT NULL DEFAULT 'block' COMMENT '处理动作：block=拦截/pending=转待审核/flag=标记',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  PRIMARY KEY (id),
  KEY idx_biz (biz_type, biz_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词命中记录';

-- 3.3 初始化少量示例敏感词（生产环境请导入完整词库）
INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time, remark) VALUES
  ('示例敏感词1', 'other', '0', 'admin', NOW(), '示例词，生产环境请替换为真实词库'),
  ('示例敏感词2', 'ad',     '0', 'admin', NOW(), '示例词，生产环境请替换为真实词库')
ON DUPLICATE KEY UPDATE word = VALUES(word);

-- ----------------------------------------------------------------
-- 4. 话题审核菜单与权限（CMS 后台）
--    归类方案（对齐 91_菜单权限_CMS.sql 现有结构）：
--    - 话题审核   → 内容管理目录（@cms_parent_id），与文章/专栏管理平级
--    - 专栏审核   → 内容管理目录（@cms_parent_id），与专栏管理平级
--    - 敏感词管理 → 系统管理目录（@system_parent_id），系统配置类
--    修复历史 NPE：原脚本父菜单子查询条件不可靠（'话题管理'菜单不存在 /
--    系统管理目录 perms 为 NULL），返回 NULL 后写入 parent_id=NULL，
--    导致 SysMenuServiceImpl#getChildPerms 自动拆箱 NPE。
--    本节使用 path+parent_id+menu_type 可靠条件 + IFNULL 兜底，
--    并补 UPDATE 语句修复已存在的脏数据。
-- ----------------------------------------------------------------

-- 4.0 解析父菜单 ID（与 91 范式一致，使用 @变量传递）
-- 内容管理目录：CMS 顶级目录，menu_name='内容管理'，parent_id=0，menu_type='M'
SELECT @cms_parent_id := menu_id FROM sys_menu
 WHERE menu_name = '内容管理' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;
-- 系统管理目录：若依标准目录，path='system'，parent_id=0，menu_type='M'
-- 注意：若依顶级目录 perms 字段为 NULL，不能用 perms='system' 查询
SELECT @system_parent_id := menu_id FROM sys_menu
 WHERE path = 'system' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;

-- 兜底：父菜单查不到时回退到顶级（0），绝不写入 NULL，避免 getRouters NPE
SET @cms_parent_id := IFNULL(@cms_parent_id, 0);
SET @system_parent_id := IFNULL(@system_parent_id, 0);

-- 4.1 话题管理菜单（挂在 内容管理 下，与文章/专栏管理平级）
--     历史遗漏：CMS 仅有审核入口，缺话题列表管理菜单，导致审核页 goBack(/cms/topic) 路由 404。
--     此菜单提供 /cms/topic 路由，承载 cms/topic/index.vue，对应后端 CmsTopicController#list。
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '话题管理', @cms_parent_id, 14, 'topic', 'cms/topic/index', 1, 0, 'C', '0', '0', 'cms:topic:list', 'message', 'admin', NOW(), '话题列表与状态管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:topic:list');
UPDATE sys_menu SET parent_id = @cms_parent_id, path = 'topic', component = 'cms/topic/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:topic:list' AND parent_id IS NULL;

-- 话题管理按钮权限（查询/详情）
SELECT @topic_list_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:topic:list' LIMIT 1;
SET @topic_list_menu_id := IFNULL(@topic_list_menu_id, @cms_parent_id);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '话题查询', @topic_list_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'cms:topic:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:topic:query');
UPDATE sys_menu SET parent_id = @topic_list_menu_id WHERE perms = 'cms:topic:query' AND parent_id IS NULL;

-- 4.2 话题审核菜单（挂在 内容管理 下，与文章/专栏管理平级）
--     path='topic-audit'，避免与其他菜单 path 冲突导致前端动态路由 route name 撞车
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '话题审核', @cms_parent_id, 15, 'topic-audit', 'cms/topic/audit', 1, 0, 'C', '0', '0', 'cms:topic:audit', 'checkbox', 'admin', NOW(), '话题审核（通过/驳回）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:topic:audit');
-- 修复历史脏数据：parent_id IS NULL 的话题审核菜单归类到内容管理目录
UPDATE sys_menu SET parent_id = @cms_parent_id, path = 'topic-audit', component = 'cms/topic/audit', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:topic:audit' AND parent_id IS NULL;

-- 4.3 专栏审核菜单（挂在 内容管理 下，与专栏管理平级）
--     path='column-audit'，避免与专栏管理 path='column' 冲突
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏审核', @cms_parent_id, 16, 'column-audit', 'cms/column/audit', 1, 0, 'C', '0', '0', 'cms:column:audit', 'checkbox', 'admin', NOW(), '专栏审核（通过/驳回）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:column:audit');
-- 修复历史脏数据
UPDATE sys_menu SET parent_id = @cms_parent_id, path = 'column-audit', component = 'cms/column/audit', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:column:audit' AND parent_id IS NULL;

-- 4.3 敏感词管理菜单（挂在 系统管理 下）
--     path='sensitiveWord'，component='system/sensitiveWord/index'
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '敏感词管理', @system_parent_id, 6, 'sensitiveWord', 'system/sensitiveWord/index', 1, 0, 'C', '0', '0', 'system:sensitiveWord:list', 'dict', 'admin', NOW(), '敏感词库维护与词树刷新'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:sensitiveWord:list');
-- 修复历史脏数据
UPDATE sys_menu SET parent_id = @system_parent_id, update_by = 'admin', update_time = NOW()
 WHERE perms = 'system:sensitiveWord:list' AND parent_id IS NULL;

-- 敏感词管理按钮权限（查询/新增/修改/删除），挂在敏感词管理菜单下
SELECT @sensitive_word_menu_id := menu_id FROM sys_menu WHERE perms = 'system:sensitiveWord:list' LIMIT 1;
SET @sensitive_word_menu_id := IFNULL(@sensitive_word_menu_id, @system_parent_id);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '敏感词查询', @sensitive_word_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:sensitiveWord:query');
UPDATE sys_menu SET parent_id = @sensitive_word_menu_id WHERE perms = 'system:sensitiveWord:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '敏感词新增', @sensitive_word_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:sensitiveWord:add');
UPDATE sys_menu SET parent_id = @sensitive_word_menu_id WHERE perms = 'system:sensitiveWord:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '敏感词修改', @sensitive_word_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:sensitiveWord:edit');
UPDATE sys_menu SET parent_id = @sensitive_word_menu_id WHERE perms = 'system:sensitiveWord:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '敏感词删除', @sensitive_word_menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:sensitiveWord:remove');
UPDATE sys_menu SET parent_id = @sensitive_word_menu_id WHERE perms = 'system:sensitiveWord:remove' AND parent_id IS NULL;

-- ----------------------------------------------------------------
-- 5. 定时任务：话题/观点轻量扫描（RuoYi Quartz 范式，需在 sys_job 配置）
--    示例 invoke_target: sensitiveScanTask.scanTopics()
--    示例 cron: 0 0 3 * * ?  （每天凌晨3点）
-- ----------------------------------------------------------------
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '敏感词扫描-话题', 'DEFAULT', 'sensitiveScanTask.scanTopics()', '0 0 3 * * ?', '3', '1', '0', 'admin', NOW(), '定时扫描话题内容，命中则转待审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'sensitiveScanTask.scanTopics()');

INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '敏感词扫描-观点', 'DEFAULT', 'sensitiveScanTask.scanTopicPosts()', '0 5 3 * * ?', '3', '1', '0', 'admin', NOW(), '定时扫描话题观点，命中则标记'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'sensitiveScanTask.scanTopicPosts()');

-- ----------------------------------------------------------------
-- 6. 角色菜单关联（参照 92/93 范式，为 admin 角色补关联）
--    说明：admin 用户在 SysMenuServiceImpl 中走 isAdmin 旁路，本身能看到所有菜单；
--    补 sys_role_menu 关联是为了"角色菜单分配"界面观感一致，以及让非 admin 角色
--    （如运营、内容审核员）能通过角色分配获得这些菜单权限。
-- ----------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, menu_id, 'admin', NOW()
FROM sys_menu
WHERE perms IN (
    'cms:topic:list', 'cms:topic:query',
    'cms:topic:audit',
    'cms:column:audit',
    'system:sensitiveWord:list', 'system:sensitiveWord:query',
    'system:sensitiveWord:add', 'system:sensitiveWord:edit', 'system:sensitiveWord:remove'
  )
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
  );

-- ----------------------------------------------------------------
-- 6.5 永久禁用 RuoYi 自带的 3 个 ryTask 测试任务
--     原因：ryTask.ryNoParams / ryParams / ryMultipleParams 是 RuoYi 框架演示任务，
--     每 10/15/20 秒执行一次，生产环境若误启用会持续刷日志污染。
--     status: 0=正常 1=暂停（即使误启用也不会执行）
-- ----------------------------------------------------------------
UPDATE sys_job SET status = '1', remark = CONCAT(IFNULL(remark, ''), ' [v6.7已永久禁用-测试任务]')
WHERE invoke_target LIKE 'ryTask.%' AND status = '0';

-- ----------------------------------------------------------------
-- 6.6 补充审核相关索引
--     背景：v6.7 新增 auditor_id 字段，CMS 审核员维度查询走全表扫描；
--     话题/专栏待审核列表按 created_time 排序无联合索引会 filesort。
--     使用 information_schema 判断索引是否存在，确保幂等。
-- ----------------------------------------------------------------

-- portal_article: 审核人索引 + 状态+发布时间联合索引（首页最高频查询）
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_article' AND INDEX_NAME = 'idx_auditor_id') = 0,
  'ALTER TABLE portal_article ADD INDEX idx_auditor_id (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_article' AND INDEX_NAME = 'idx_status_published_at') = 0,
  'ALTER TABLE portal_article ADD INDEX idx_status_published_at (status, published_at)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_topic: 审核人索引 + 状态+创建时间联合索引（待审核列表）
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_topic' AND INDEX_NAME = 'idx_auditor_id') = 0,
  'ALTER TABLE portal_topic ADD INDEX idx_auditor_id (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_topic' AND INDEX_NAME = 'idx_status_created_time') = 0,
  'ALTER TABLE portal_topic ADD INDEX idx_status_created_time (status, created_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_column: 审核人索引 + 状态+创建时间联合索引 + 分类索引
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_column' AND INDEX_NAME = 'idx_auditor_id') = 0,
  'ALTER TABLE portal_column ADD INDEX idx_auditor_id (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_column' AND INDEX_NAME = 'idx_status_created_time') = 0,
  'ALTER TABLE portal_column ADD INDEX idx_status_created_time (status, created_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_column' AND INDEX_NAME = 'idx_category_id') = 0,
  'ALTER TABLE portal_column ADD INDEX idx_category_id (category_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_sensitive_word_log: 用户维度索引
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_sensitive_word_log' AND INDEX_NAME = 'idx_user_id') = 0,
  'ALTER TABLE sys_sensitive_word_log ADD INDEX idx_user_id (user_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------------------------------------------
-- 6.7 敏感词词库扩充（示例占位词，生产环境需导入完整词库）
--     说明：以下为分类示例词，仅用于验证 DFA 过滤链路是否畅通。
--     ⚠️ 生产环境投产前必须替换为完整敏感词库（政治/色情/广告/辱骂等），
--     可从第三方词库导入或通过 CMS 后台「敏感词管理」页面批量录入。
-- ----------------------------------------------------------------
INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time)
SELECT '示例-广告', 'ad', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_sensitive_word WHERE word = '示例-广告');

INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time)
SELECT '示例-辱骂', 'insult', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_sensitive_word WHERE word = '示例-辱骂');

INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time)
SELECT '示例-色情', 'porn', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_sensitive_word WHERE word = '示例-色情');

INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time)
SELECT '示例-政治', 'politics', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_sensitive_word WHERE word = '示例-政治');

INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time)
SELECT '示例-其他', 'other', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_sensitive_word WHERE word = '示例-其他');

-- ====================================================================
-- 升级完成
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 97_升级脚本_v6.8.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v6.8 升级脚本：评论审核对齐文章审核模式
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（使用 information_schema 判断列/索引是否存在）
-- 幂等性核查（P2-1）：✅ 已幂等 — DDL 用 information_schema+IF+PREPARE 守护；
--   INSERT 权限项用 SET @var:=(子查询) + IF(COUNT=0, CONCAT动态SQL, SELECT) + PREPARE/EXECUTE 模式，符合 112 脚本范式（@var 仅用于条件判断不跨语句传递业务状态）。
-- 背景：
--   1. portal_comment 表 status 字段语义不一致（DDL 注释为"0正常 1停用"，
--      代码使用为"0待审核 1已发布"），CMS 评论审核接口仅更新 status，
--      缺审核人/审核时间/审核意见，无法追溯。
--   2. 本脚本：
--      a) 为 portal_comment 增加 auditor_id / audit_time / audit_remark 字段，
--         对齐 portal_article / portal_topic / portal_column 审核字段设计；
--      b) 修正 status 字段注释，明确"0=待审核 1=已发布 2=审核驳回"语义；
--      c) 增加审核人索引。
--   3. 业务层（CmsCommentServiceImpl.auditComment）将同时升级为：
--      状态白名单校验、乐观锁、审核人/时间/意见写入、事务化，
--      与 CmsArticleServiceImpl.auditArticle 保持一致。
-- ====================================================================

SET @db := DATABASE();

-- 1.1 portal_comment.auditor_id
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_comment ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID，CMS审核时写入）'' AFTER status',
  'SELECT ''portal_comment.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 portal_comment.audit_remark
SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_comment ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因（独立字段）'' AFTER auditor_id',
  'SELECT ''portal_comment.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 portal_comment.audit_time
SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_comment ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_comment.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 修正 status 字段注释（语义对齐文章审核模式，仅修改注释，不影响存量数据）
--     0=待审核（敏感词扫描后转人工审核场景），1=已发布（默认值，未命中敏感词直接发布），
--     2=审核驳回（CMS 后台驳回）
ALTER TABLE portal_comment MODIFY COLUMN status CHAR(1) DEFAULT '1' COMMENT '状态：0=待审核 1=已发布 2=审核驳回';

-- 1.5 审核人索引（CMS 审核员维度查询）
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_comment' AND INDEX_NAME = 'idx_auditor_id') = 0,
  'ALTER TABLE portal_comment ADD INDEX idx_auditor_id (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ====================================================================
-- 2. 审核权限独立化（cms:article:audit / cms:comment:audit）
-- 背景：
--   历史上 CmsArticleController#audit 与 CmsCommentController#audit 复用 cms:*:edit
--   权限标识，导致审核员必须被授予 edit 权限才能审核，无法实现"审核员/编辑员"
--   角色分离。本批次已将 Controller 端权限标识切换为独立的 cms:article:audit /
--   cms:comment:audit（与 91_菜单权限_CMS.sql 已定义的权限项一致）。
--   此处补幂等保障：若权限项缺失则补建（防止历史库未运行 91 脚本导致 hasPermi 始终 false）。
-- ====================================================================

-- 2.1 文章审核权限项（幂等）
SET @article_menu_id := (SELECT menu_id FROM sys_menu WHERE perms = 'cms:article:list' LIMIT 1);
SET @sql := IF(
  @article_menu_id IS NOT NULL
  AND (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:article:audit') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''文章审核'', ', @article_menu_id, ', 5, '''', NULL, NULL, 1, 0, ''F'', ''0'', ''0'', ''cms:article:audit'', ''#'', ''admin'', NOW(), '''')'),
  'SELECT ''cms:article:audit 已存在或父菜单缺失'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 评论审核权限项（幂等）
SET @comment_menu_id := (SELECT menu_id FROM sys_menu WHERE perms = 'cms:comment:list' LIMIT 1);
SET @sql := IF(
  @comment_menu_id IS NOT NULL
  AND (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:comment:audit') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''评论审核'', ', @comment_menu_id, ', 2, '''', NULL, NULL, 1, 0, ''F'', ''0'', ''0'', ''cms:comment:audit'', ''#'', ''admin'', NOW(), '''')'),
  'SELECT ''cms:comment:audit 已存在或父菜单缺失'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 提示：admin 超级管理员角色通过 role_key='admin' 通配权限自动放行，无需额外分配；
-- 普通审核员角色请在系统管理 → 角色管理 中勾选 cms:article:audit / cms:comment:audit 权限项。

-- ====================================================================
-- 升级完成
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 109_5_升级脚本_v7.26_AI表名添加ai_前缀.sql
-- ---------------------------------------------------------------
-- =====================================================================
-- 升级脚本 v7.26 · AI 表名添加 ai_ 前缀
-- =====================================================================
-- 目的：将 AI 模块全部 32 张表的表名统一添加 `ai_` 前缀，
--       消除与 portal_*/sys_*/qrtz_*/gen_* 等其他模块表的命名歧义，
--       使 AI 模块表在 DB 层面一眼可辨识。
--
-- ⚠️ 执行顺序说明（关键）：
--   本脚本编号为 109_5，必须在 110/112/113/117 之前执行！
--   原因：110/112/113/117 脚本已统一引用 ai_ 前缀表名，
--         若旧环境（v7.5 表名为 agent）先执行 110，会因 ai_agent 表不存在而报错。
--   执行顺序：
--     全新部署：54 建 ai_ 表 → 109_5 检测旧表不存在跳过 → 110+ 引用 ai_ 表 ✅
--     旧环境升级：54 IF NOT EXISTS 跳过 → 109_5 RENAME agent→ai_agent → 110+ 引用 ai_ 表 ✅
--
-- 策略：
--   1. 对每张表用 information_schema 校验：旧表存在且新表不存在 → 执行 RENAME
--   2. MySQL 的 RENAME TABLE 会自动更新所有 FK 引用（CONSTRAINT 名称不变）
--   3. 全程幂等，可重复执行：第二次执行时旧表不存在 → 跳过
--
-- 影响范围：
--   - 27 张有 Java 实体（@TableName）的 AI 核心表
--   - 5 张仅 DDL 定义无 Java 实体的数据分析辅助表
--   - FK 约束（由 112 脚本添加的 18 个 FK）自动跟随更新
--
-- 同步修改：
--   - 54_AI模块表_v7.5.sql（CREATE TABLE / INSERT / FK REFERENCES → ai_*）
--   - 110/112/113/117 升级脚本（ALTER TABLE / information_schema 校验 → ai_*）
--   - 27 个实体类 @TableName 注解 → ai_*
--   - 6 个 mapper 接口 @Select/@Update 硬编码表名 → ai_*
--
-- 幂等性核查（P2-1）：✅ 已保证（information_schema 双重校验）
-- =====================================================================

-- ==================== 27 张有 Java 实体的 AI 核心表 ====================

-- 1. agent → ai_agent
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent') = 0, 'RENAME TABLE `agent` TO `ai_agent`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. agent_dictionary_relation → ai_agent_dictionary_relation
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_dictionary_relation') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_dictionary_relation') = 0, 'RENAME TABLE `agent_dictionary_relation` TO `ai_agent_dictionary_relation`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 3. agent_tool → ai_agent_tool
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_tool') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_tool') = 0, 'RENAME TABLE `agent_tool` TO `ai_agent_tool`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 4. agent_tool_relation → ai_agent_tool_relation
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_tool_relation') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_tool_relation') = 0, 'RENAME TABLE `agent_tool_relation` TO `ai_agent_tool_relation`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 5. agent_workflow_relation → ai_agent_workflow_relation
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_workflow_relation') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_workflow_relation') = 0, 'RENAME TABLE `agent_workflow_relation` TO `ai_agent_workflow_relation`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 6. chat_history → ai_chat_history
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_history') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_history') = 0, 'RENAME TABLE `chat_history` TO `ai_chat_history`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 7. conversation → ai_conversation
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversation') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_conversation') = 0, 'RENAME TABLE `conversation` TO `ai_conversation`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 8. conversation_message → ai_conversation_message
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversation_message') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_conversation_message') = 0, 'RENAME TABLE `conversation_message` TO `ai_conversation_message`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 9. datasource_config → ai_datasource_config
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'datasource_config') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_datasource_config') = 0, 'RENAME TABLE `datasource_config` TO `ai_datasource_config`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 10. document_image → ai_document_image
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'document_image') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_document_image') = 0, 'RENAME TABLE `document_image` TO `ai_document_image`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 11. document_segment → ai_document_segment
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'document_segment') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_document_segment') = 0, 'RENAME TABLE `document_segment` TO `ai_document_segment`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 12. domain_dictionary → ai_domain_dictionary
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'domain_dictionary') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_domain_dictionary') = 0, 'RENAME TABLE `domain_dictionary` TO `ai_domain_dictionary`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 13. knowledge_base → ai_knowledge_base
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_base') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_base') = 0, 'RENAME TABLE `knowledge_base` TO `ai_knowledge_base`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 14. knowledge_config → ai_knowledge_config
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_config') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_config') = 0, 'RENAME TABLE `knowledge_config` TO `ai_knowledge_config`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 15. knowledge_config_template → ai_knowledge_config_template
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_config_template') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_config_template') = 0, 'RENAME TABLE `knowledge_config_template` TO `ai_knowledge_config_template`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 16. knowledge_library → ai_knowledge_library
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_library') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_library') = 0, 'RENAME TABLE `knowledge_library` TO `ai_knowledge_library`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 17. knowledge_library_config → ai_knowledge_library_config
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_library_config') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_library_config') = 0, 'RENAME TABLE `knowledge_library_config` TO `ai_knowledge_library_config`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 18. model_config → ai_model_config
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'model_config') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_model_config') = 0, 'RENAME TABLE `model_config` TO `ai_model_config`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 19. query_history → ai_query_history
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'query_history') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_query_history') = 0, 'RENAME TABLE `query_history` TO `ai_query_history`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 20. reference_feedback → ai_reference_feedback
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reference_feedback') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_reference_feedback') = 0, 'RENAME TABLE `reference_feedback` TO `ai_reference_feedback`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 21. table_metadata → ai_table_metadata
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'table_metadata') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_table_metadata') = 0, 'RENAME TABLE `table_metadata` TO `ai_table_metadata`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 22. tool_call_log → ai_tool_call_log
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tool_call_log') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_tool_call_log') = 0, 'RENAME TABLE `tool_call_log` TO `ai_tool_call_log`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 23. token_usage_log → ai_token_usage_log
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'token_usage_log') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_token_usage_log') = 0, 'RENAME TABLE `token_usage_log` TO `ai_token_usage_log`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 24. token_usage_summary → ai_token_usage_summary
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'token_usage_summary') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_token_usage_summary') = 0, 'RENAME TABLE `token_usage_summary` TO `ai_token_usage_summary`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 25. workflow → ai_workflow
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_workflow') = 0, 'RENAME TABLE `workflow` TO `ai_workflow`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 26. workflow_execution → ai_workflow_execution
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_execution') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_workflow_execution') = 0, 'RENAME TABLE `workflow_execution` TO `ai_workflow_execution`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 27. workflow_version → ai_workflow_version
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_version') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_workflow_version') = 0, 'RENAME TABLE `workflow_version` TO `ai_workflow_version`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== 5 张仅 DDL 定义无 Java 实体的数据分析辅助表 ====================

-- 28. analysis_report → ai_analysis_report
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_report') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_analysis_report') = 0, 'RENAME TABLE `analysis_report` TO `ai_analysis_report`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 29. chart_recommendation_rule → ai_chart_recommendation_rule
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chart_recommendation_rule') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chart_recommendation_rule') = 0, 'RENAME TABLE `chart_recommendation_rule` TO `ai_chart_recommendation_rule`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 30. data_insight → ai_data_insight
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_insight') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_data_insight') = 0, 'RENAME TABLE `data_insight` TO `ai_data_insight`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 31. document_chunk_metadata → ai_document_chunk_metadata
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'document_chunk_metadata') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_document_chunk_metadata') = 0, 'RENAME TABLE `document_chunk_metadata` TO `ai_document_chunk_metadata`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 32. sql_template → ai_sql_template
SET @sql := IF((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_template') = 1 AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_sql_template') = 0, 'RENAME TABLE `sql_template` TO `ai_sql_template`', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== 验证查询 ====================

-- 验证：AI 模块表全部已添加 ai_ 前缀（应返回 32 行）
-- SELECT TABLE_NAME FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE 'ai\_%'
-- ORDER BY TABLE_NAME;

-- 验证：无残留的旧表名（应返回 0 行）
-- SELECT TABLE_NAME FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN (
--     'agent', 'agent_dictionary_relation', 'agent_tool', 'agent_tool_relation', 'agent_workflow_relation',
--     'chat_history', 'conversation', 'conversation_message',
--     'datasource_config', 'document_image', 'document_segment', 'domain_dictionary',
--     'knowledge_base', 'knowledge_config', 'knowledge_config_template',
--     'knowledge_library', 'knowledge_library_config', 'model_config',
--     'query_history', 'reference_feedback', 'table_metadata',
--     'tool_call_log', 'token_usage_log', 'token_usage_summary',
--     'workflow', 'workflow_execution', 'workflow_version',
--     'analysis_report', 'chart_recommendation_rule', 'data_insight',
--     'document_chunk_metadata', 'sql_template'
--   );


-- ---------------------------------------------------------------
-- 来源: 110_升级脚本_v7.18_新老字段表清理.sql
-- ---------------------------------------------------------------
-- ============================================================
-- 升级脚本 v7.18: 新老字段/表清理
-- 描述: 清理备份表和已废弃字段
-- 幂等性: 用 information_schema 校验 + PREPARE/EXECUTE 实现幂等
--       （MySQL 8.0 不支持 ALTER TABLE ... DROP COLUMN IF EXISTS，
--        该语法为 MariaDB 扩展，须改用动态 SQL）
-- 注意: ai_chat_history 表和 ai_agent.workflow_id 字段仍在被代码使用，
--       待 DynamicChatServiceImpl 重构后才能清理（降为 P2 任务）
-- ============================================================

-- 1. 删除 sys_notice_bak 备份表（93脚本消息中心迁移时创建的备份）
--    代码中已无引用（grep "notice_bak" 0结果）
DROP TABLE IF EXISTS `sys_notice_bak`;

-- 2. 删除 portal_notification_bak 备份表（消息中心合并时创建的备份）
--    代码中已无引用（grep "notification_bak" 0结果）
DROP TABLE IF EXISTS `portal_notification_bak`;

-- 3. 删除 ai_agent 表的 knowledge_base_ids 字段（旧逗号字符串，已被 knowledge_library_ids JSON 替代）
--    Entity 中已不存在此字段，代码中已无引用（grep "knowledge_base_ids" 0结果）
--    使用 information_schema 校验实现幂等（兼容 MySQL 8.0）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent' AND COLUMN_NAME = 'knowledge_base_ids');
SET @sql := IF(@col_exists = 1, 'ALTER TABLE `ai_agent` DROP COLUMN `knowledge_base_ids`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 以下字段/表仍在被代码使用，待重构后清理（P2 级别）
-- ============================================================
-- ai_chat_history 表: 仍被 AgentController（统计/会话列表/清除历史）和 DynamicChatServiceImpl 使用
--   → 待 ChatHistoryService 迁移到 ai_conversation_message 后才能 DROP
--
-- ai_agent.workflow_id 字段: 仍被 DynamicChatServiceImpl 用于触发工作流执行
--   → 待 DynamicChatServiceImpl 改为从 ai_agent_workflow_relation 关系表获取工作流ID后才能 DROP COLUMN
-- ============================================================

-- 验证查询
SELECT '=== 清理后验证 ===' AS info;
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('sys_notice_bak', 'portal_notification_bak');
-- 期望: 0行（两张备份表已删除）

SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent' AND COLUMN_NAME = 'knowledge_base_ids';
-- 期望: 0行（字段已删除）


-- ---------------------------------------------------------------
-- 来源: 111_升级脚本_v7.19_Scheduled迁移至Quartz.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.19 升级脚本：@Scheduled 任务迁移至 Quartz 统一调度
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（WHERE NOT EXISTS 防重）
-- 背景：
--   1. 历史代码中存在 4 处 Spring @Scheduled(fixedRate=...) 注解任务，
--      调度入口分散在 ai 模块的多个 Service/Task 中，无法在后台统一管控。
--   2. 已删除 4 处 @Scheduled 注解，将调度入口统一收口至 sys_job 表：
--      - cacheCleanupTask.cleanupExpiredCache()           每5分钟清理表结构缓存
--      - cacheCleanupTask.cleanupRateLimiter()            每2分钟清理限流器过期数据
--      - dataAnalysisConversationServiceImpl.cleanExpiredSessions()  每30分钟清理过期会话
--      - tokenUsageServiceImpl.flushLogsToDB()            每1分钟批量落盘 Redis 中的 Token 日志
--   3. 与既有敏感词扫描任务（102 脚本）错峰，避免同时段争抢资源。
--   4. 任务默认启用（status='0'），misfire_policy='3'（放弃补偿，避免堆积），
--      concurrent='1'（禁止并发，防止上一批未扫完就启动下一批）。
--      唯一例外：tokenUsageServiceImpl.flushLogsToDB 是日志落盘任务，
--      允许补偿执行（misfire_policy='1'）以防日志丢失。
-- ====================================================================

-- 1. 缓存清理 - 表结构缓存（原 @Scheduled(fixedRate = 5 * 60 * 1000)）
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '缓存清理-表结构', 'DEFAULT', 'cacheCleanupTask.cleanupExpiredCache()', '0 */5 * * * ?', '3', '1', '0', 'admin', NOW(),
       '每5分钟清理 DataSourceService 的过期表结构缓存（原 CacheCleanupTask @Scheduled，迁移至 Quartz 统一调度）'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'cacheCleanupTask.cleanupExpiredCache()');

-- 2. 缓存清理 - 限流器过期数据（原 @Scheduled(fixedRate = 2 * 60 * 1000)）
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '缓存清理-限流器', 'DEFAULT', 'cacheCleanupTask.cleanupRateLimiter()', '0 */2 * * * ?', '3', '1', '0', 'admin', NOW(),
       '每2分钟清理 RateLimiter 过期数据（原 CacheCleanupTask @Scheduled，迁移至 Quartz 统一调度）'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'cacheCleanupTask.cleanupRateLimiter()');

-- 3. 数据分析会话清理（原 @Scheduled(fixedRate = 1800000)）
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '会话清理-数据分析', 'DEFAULT', 'dataAnalysisConversationServiceImpl.cleanExpiredSessions()', '0 */30 * * * ?', '3', '1', '0', 'admin', NOW(),
       '每30分钟清理超过1小时未访问的数据分析对话会话（原 DataAnalysisConversationServiceImpl @Scheduled，迁移至 Quartz 统一调度）'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'dataAnalysisConversationServiceImpl.cleanExpiredSessions()');

-- 4. Token 日志批量落盘（原 @Scheduled(fixedRate = 60000)）
--    misfire_policy='1' 立即触发补偿执行，避免漏写日志
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '日志落盘-Token使用', 'DEFAULT', 'tokenUsageServiceImpl.flushLogsToDB()', '0 * * * * ?', '1', '1', '0', 'admin', NOW(),
       '每1分钟将 Redis 中的 Token 使用日志批量写入 DB（原 TokenUsageServiceImpl @Scheduled，迁移至 Quartz 统一调度；misfire=1 立即补偿避免日志丢失）'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'tokenUsageServiceImpl.flushLogsToDB()');

-- ====================================================================
-- 升级完成
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 112_升级脚本_v7.20_AI核心关联外键补齐.sql
-- ---------------------------------------------------------------
-- =====================================================================
-- v7.20 升级脚本：AI 模块核心关联外键补齐（16 个 FK）
-- 适配 MySQL 8.0.29+
-- 说明：本脚本幂等，可重复执行（INFORMATION_SCHEMA 校验 + 孤儿预清理）
-- 背景：
--   1. 54_AI模块表_v7.5.sql 仅在 conversation_message 与 knowledge_library_config
--      两张表声明了 FOREIGN KEY，其余 16 个核心关联只有 INDEX 没有 CONSTRAINT，
--      导致孤儿数据可静默累积（如：删除 agent 后 chat_history 仍残留）。
--   2. 本脚本为以下 16 个核心关联补齐 FK：
--      ┌─ 顶层实体引用（SET NULL：父表删除时子表外键置 NULL，保留子表记录）
--      │  1. agent.model_config_id             → model_config.id
--      │  2. agent.workflow_id                 → workflow.id
--      │  3. knowledge_base.library_id         → knowledge_library.id
--      │  4. token_usage_log.agent_id          → agent.id（保留审计日志）
--      │  5. token_usage_log.conversation_id   → conversation.id（保留审计日志）
--      │  6. token_usage_log.message_id        → conversation_message.id（保留审计日志）
--      ├─ 配置/分片子表（CASCADE：父表删除时级联删除子表）
--      │  7. knowledge_config.knowledge_id     → knowledge_base.id
--      │  8. document_segment.knowledge_base_id → knowledge_base.id
--      ├─ 关联表（CASCADE：父表删除时级联删除关联记录）
--      │  9.  agent_dictionary_relation.agent_id      → agent.id
--      │  10. agent_dictionary_relation.dictionary_id → domain_dictionary.id
--      │  11. agent_tool_relation.agent_id            → agent.id
--      │  12. agent_tool_relation.tool_id             → agent_tool.id
--      │  13. agent_workflow_relation.agent_id        → agent.id
--      │  14. agent_workflow_relation.workflow_id     → workflow.id
--      ├─ 工作流子表（CASCADE）
--      │  15. workflow_execution.workflow_id     → workflow.id
--      │  16. workflow_version.workflow_id      → workflow.id
--      └─ 对话/历史（CASCADE：与既有 conversation_message_ibfk_1 行为一致）
--         * conversation.agent_id           → agent.id（CASCADE）
--         * chat_history.agent_id           → agent.id（CASCADE）
--      （上述 2 个对话关联已在脚本中通过 CASCADE 方式补齐，但不在 16 个核心 FK 计数内
--       因为它们与既有 conversation_message_ibfk_1 在同一业务链上）
--   3. ON UPDATE 全部 CASCADE：父表主键更新时同步（项目用 bigint 自增主键，
--      实际不更新主键，但 CASCADE 是更安全的默认）
--   4. 执行顺序：先清理孤儿数据，再按依赖顺序（父表→子表）补齐 FK，
--      避免外键约束在添加时因孤儿数据失败。
-- =====================================================================

-- -----------------------------------------------------------------
-- 阶段 1：孤儿数据预清理
-- 说明：使用 LEFT JOIN 检测并处理外键字段指向不存在父记录的数据
--       - 可空外键字段：UPDATE 置 NULL（保留子记录）
--       - 非空外键字段：DELETE 删除（避免 FK 创建失败）
-- -----------------------------------------------------------------

-- 1.1 顶层实体引用（可空字段 → 置 NULL）
UPDATE `ai_agent` a LEFT JOIN `ai_model_config` m ON a.`model_config_id` = m.`id`
   SET a.`model_config_id` = NULL
 WHERE a.`model_config_id` IS NOT NULL AND m.`id` IS NULL;

UPDATE `ai_agent` a LEFT JOIN `ai_workflow` w ON a.`workflow_id` = w.`id`
   SET a.`workflow_id` = NULL
 WHERE a.`workflow_id` IS NOT NULL AND w.`id` IS NULL;

UPDATE `ai_knowledge_base` kb LEFT JOIN `ai_knowledge_library` kl ON kb.`library_id` = kl.`id`
   SET kb.`library_id` = NULL
 WHERE kb.`library_id` IS NOT NULL AND kl.`id` IS NULL;

UPDATE `ai_token_usage_log` t LEFT JOIN `ai_agent` a ON t.`agent_id` = a.`id`
   SET t.`agent_id` = NULL
 WHERE t.`agent_id` IS NOT NULL AND a.`id` IS NULL;

UPDATE `ai_token_usage_log` t LEFT JOIN `ai_conversation` c ON t.`conversation_id` = c.`id`
   SET t.`conversation_id` = NULL
 WHERE t.`conversation_id` IS NOT NULL AND c.`id` IS NULL;

UPDATE `ai_token_usage_log` t LEFT JOIN `ai_conversation_message` cm ON t.`message_id` = cm.`id`
   SET t.`message_id` = NULL
 WHERE t.`message_id` IS NOT NULL AND cm.`id` IS NULL;

-- 1.2 配置/分片子表（非空字段 → 删除孤儿）
DELETE kc FROM `ai_knowledge_config` kc
LEFT JOIN `ai_knowledge_base` kb ON kc.`knowledge_id` = kb.`id`
WHERE kb.`id` IS NULL;

DELETE ds FROM `ai_document_segment` ds
LEFT JOIN `ai_knowledge_base` kb ON ds.`knowledge_base_id` = kb.`id`
WHERE kb.`id` IS NULL;

-- 1.3 关联表（非空字段 → 删除孤儿）
DELETE adr FROM `ai_agent_dictionary_relation` adr
LEFT JOIN `ai_agent` a ON adr.`agent_id` = a.`id`
LEFT JOIN `ai_domain_dictionary` dd ON adr.`dictionary_id` = dd.`id`
WHERE a.`id` IS NULL OR dd.`id` IS NULL;

DELETE atr FROM `ai_agent_tool_relation` atr
LEFT JOIN `ai_agent` a ON atr.`agent_id` = a.`id`
LEFT JOIN `ai_agent_tool` at ON atr.`tool_id` = at.`id`
WHERE a.`id` IS NULL OR at.`id` IS NULL;

DELETE awr FROM `ai_agent_workflow_relation` awr
LEFT JOIN `ai_agent` a ON awr.`agent_id` = a.`id`
LEFT JOIN `ai_workflow` w ON awr.`workflow_id` = w.`id`
WHERE a.`id` IS NULL OR w.`id` IS NULL;

-- 1.4 工作流子表（非空字段 → 删除孤儿）
DELETE we FROM `ai_workflow_execution` we
LEFT JOIN `ai_workflow` w ON we.`workflow_id` = w.`id`
WHERE w.`id` IS NULL;

DELETE wv FROM `ai_workflow_version` wv
LEFT JOIN `ai_workflow` w ON wv.`workflow_id` = w.`id`
WHERE w.`id` IS NULL;

-- 1.5 对话/历史子表（非空字段 → 删除孤儿）
-- 说明：conversation.agent_id NOT NULL，删除指向不存在 agent 的对话
DELETE c FROM `ai_conversation` c
LEFT JOIN `ai_agent` a ON c.`agent_id` = a.`id`
WHERE a.`id` IS NULL;

DELETE ch FROM `ai_chat_history` ch
LEFT JOIN `ai_agent` a ON ch.`agent_id` = a.`id`
WHERE a.`id` IS NULL;


-- -----------------------------------------------------------------
-- 阶段 2：补齐外键约束（幂等：INFORMATION_SCHEMA 校验 + 动态 EXECUTE）
-- 说明：MySQL 不支持 ADD CONSTRAINT IF NOT EXISTS，
--       用 INFORMATION_SCHEMA 检查 + PREPARE/EXECUTE 实现幂等
-- -----------------------------------------------------------------

-- 通用模式说明（每个 FK 重复此结构）：
-- SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
--             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?
--             AND CONSTRAINT_TYPE = 'FOREIGN KEY');
-- SET @sql := IF(@fk = 0, 'ALTER TABLE ... ADD CONSTRAINT ...', 'SELECT 1');
-- PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- =====================================================================
-- 2.1 顶层实体引用（SET NULL）
-- =====================================================================

-- FK-1: agent.model_config_id → model_config.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent'
              AND CONSTRAINT_NAME = 'fk_agent_model_config' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_agent` ADD CONSTRAINT `fk_agent_model_config` FOREIGN KEY (`model_config_id`) REFERENCES `ai_model_config`(`id`) ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-2: agent.workflow_id → workflow.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent'
              AND CONSTRAINT_NAME = 'fk_agent_workflow' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_agent` ADD CONSTRAINT `fk_agent_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow`(`id`) ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-3: knowledge_base.library_id → knowledge_library.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_base'
              AND CONSTRAINT_NAME = 'fk_kb_library' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_knowledge_base` ADD CONSTRAINT `fk_kb_library` FOREIGN KEY (`library_id`) REFERENCES `ai_knowledge_library`(`id`) ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-4: token_usage_log.agent_id → agent.id（SET NULL 保留审计日志）
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_token_usage_log'
              AND CONSTRAINT_NAME = 'fk_tul_agent' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_token_usage_log` ADD CONSTRAINT `fk_tul_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent`(`id`) ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-5: token_usage_log.conversation_id → conversation.id（SET NULL 保留审计日志）
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_token_usage_log'
              AND CONSTRAINT_NAME = 'fk_tul_conversation' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_token_usage_log` ADD CONSTRAINT `fk_tul_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation`(`id`) ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-6: token_usage_log.message_id → conversation_message.id（SET NULL 保留审计日志）
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_token_usage_log'
              AND CONSTRAINT_NAME = 'fk_tul_message' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_token_usage_log` ADD CONSTRAINT `fk_tul_message` FOREIGN KEY (`message_id`) REFERENCES `ai_conversation_message`(`id`) ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- =====================================================================
-- 2.2 配置/分片子表（CASCADE）
-- =====================================================================

-- FK-7: knowledge_config.knowledge_id → knowledge_base.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_config'
              AND CONSTRAINT_NAME = 'fk_kc_knowledge' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_knowledge_config` ADD CONSTRAINT `fk_kc_knowledge` FOREIGN KEY (`knowledge_id`) REFERENCES `ai_knowledge_base`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-8: document_segment.knowledge_base_id → knowledge_base.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_document_segment'
              AND CONSTRAINT_NAME = 'fk_ds_kb' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_document_segment` ADD CONSTRAINT `fk_ds_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `ai_knowledge_base`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- =====================================================================
-- 2.3 关联表（CASCADE）
-- =====================================================================

-- FK-9: agent_dictionary_relation.agent_id → agent.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_dictionary_relation'
              AND CONSTRAINT_NAME = 'fk_adr_agent' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_agent_dictionary_relation` ADD CONSTRAINT `fk_adr_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-10: agent_dictionary_relation.dictionary_id → domain_dictionary.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_dictionary_relation'
              AND CONSTRAINT_NAME = 'fk_adr_dict' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_agent_dictionary_relation` ADD CONSTRAINT `fk_adr_dict` FOREIGN KEY (`dictionary_id`) REFERENCES `ai_domain_dictionary`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-11: agent_tool_relation.agent_id → agent.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_tool_relation'
              AND CONSTRAINT_NAME = 'fk_atr_agent' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_agent_tool_relation` ADD CONSTRAINT `fk_atr_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-12: agent_tool_relation.tool_id → agent_tool.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_tool_relation'
              AND CONSTRAINT_NAME = 'fk_atr_tool' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_agent_tool_relation` ADD CONSTRAINT `fk_atr_tool` FOREIGN KEY (`tool_id`) REFERENCES `ai_agent_tool`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-13: agent_workflow_relation.agent_id → agent.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_workflow_relation'
              AND CONSTRAINT_NAME = 'fk_awr_agent' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_agent_workflow_relation` ADD CONSTRAINT `fk_awr_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-14: agent_workflow_relation.workflow_id → workflow.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_workflow_relation'
              AND CONSTRAINT_NAME = 'fk_awr_workflow' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_agent_workflow_relation` ADD CONSTRAINT `fk_awr_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- =====================================================================
-- 2.4 工作流子表（CASCADE）
-- =====================================================================

-- FK-15: workflow_execution.workflow_id → workflow.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_workflow_execution'
              AND CONSTRAINT_NAME = 'fk_we_workflow' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_workflow_execution` ADD CONSTRAINT `fk_we_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-16: workflow_version.workflow_id → workflow.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_workflow_version'
              AND CONSTRAINT_NAME = 'fk_wv_workflow' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_workflow_version` ADD CONSTRAINT `fk_wv_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- =====================================================================
-- 2.5 对话/历史子表（CASCADE，与既有 conversation_message_ibfk_1 行为一致）
-- =====================================================================

-- FK-17: conversation.agent_id → agent.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_conversation'
              AND CONSTRAINT_NAME = 'fk_c_agent' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_conversation` ADD CONSTRAINT `fk_c_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- FK-18: chat_history.agent_id → agent.id
SET @fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_history'
              AND CONSTRAINT_NAME = 'fk_ch_agent' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@fk = 0,
  'ALTER TABLE `ai_chat_history` ADD CONSTRAINT `fk_ch_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent`(`id`) ON DELETE CASCADE ON UPDATE CASCADE',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;


-- =====================================================================
-- 阶段 3：校验查询
-- 用途：升级后执行此查询，应返回 18 行（16 核心 + 2 既有）
-- =====================================================================
-- SELECT TABLE_NAME, CONSTRAINT_NAME, DELETE_RULE, UPDATE_RULE
-- FROM information_schema.REFERENTIAL_CONSTRAINTS
-- WHERE CONSTRAINT_SCHEMA = DATABASE()
--   AND TABLE_NAME IN (
--     'ai_agent', 'ai_knowledge_base', 'ai_token_usage_log',
--     'ai_knowledge_config', 'ai_document_segment',
--     'ai_agent_dictionary_relation', 'ai_agent_tool_relation', 'ai_agent_workflow_relation',
--     'ai_workflow_execution', 'ai_workflow_version',
--     'ai_conversation', 'ai_chat_history', 'ai_conversation_message',
--     'ai_knowledge_library_config'
--   )
-- ORDER BY TABLE_NAME, CONSTRAINT_NAME;

-- =====================================================================
-- 升级完成
-- =====================================================================


-- ---------------------------------------------------------------
-- 来源: 113_升级脚本_v7.21_AI表软删除字段补齐.sql
-- ---------------------------------------------------------------
-- =====================================================================
-- v7.21 升级脚本：AI 核心表软删除字段补齐（9 张核心表）
-- 适配 MySQL 8.0+（不使用 MariaDB 扩展语法 ADD COLUMN IF NOT EXISTS / ADD INDEX IF NOT EXISTS，
--                  改用 information_schema 校验 + PREPARE/EXECUTE 动态 SQL 实现幂等）
-- 背景：
--   1. AI 模块 9 张核心表中，仅 ai_datasource_config 已有 `deleted` 字段，
--      其余 8 张（ai_agent / ai_model_config / ai_workflow / ai_knowledge_base /
--      ai_knowledge_library / ai_domain_dictionary / ai_agent_tool / ai_conversation）
--      缺少软删除字段，目前只能物理删除，存在误删恢复困难、审计追溯缺失的问题。
--   2. 本脚本为上述 8 张表补齐 `deleted` 字段（tinyint(1) NOT NULL DEFAULT 0）。
--   3. 与全局 MyBatis-Plus 配置（logic-delete-field=delFlag）解耦：
--      AI 实体均不继承 BaseEntity，使用 @TableLogic 注解显式声明本表逻辑删除字段。
--   4. 软删除与既有 FK ON DELETE CASCADE 的协作关系：
--        - 软删除（UPDATE deleted=1）：不触发 FK CASCADE，子表记录保留，可恢复；
--        - 物理删除（DELETE）：触发 FK CASCADE，级联清理子表，不可恢复。
-- =====================================================================

-- 1. ai_agent 表
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent' AND COLUMN_NAME = 'deleted');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `ai_agent` ADD COLUMN `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''删除标记: 0-未删除, 1-已删除'' AFTER `enable_self_reflection`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent' AND INDEX_NAME = 'idx_deleted');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE `ai_agent` ADD INDEX `idx_deleted`(`deleted`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. ai_model_config 表
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_model_config' AND COLUMN_NAME = 'deleted');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `ai_model_config` ADD COLUMN `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''删除标记: 0-未删除, 1-已删除'' AFTER `output_price`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_model_config' AND INDEX_NAME = 'idx_deleted');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE `ai_model_config` ADD INDEX `idx_deleted`(`deleted`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. ai_workflow 表
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_workflow' AND COLUMN_NAME = 'deleted');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `ai_workflow` ADD COLUMN `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''删除标记: 0-未删除, 1-已删除'' AFTER `update_time`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_workflow' AND INDEX_NAME = 'idx_deleted');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE `ai_workflow` ADD INDEX `idx_deleted`(`deleted`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. ai_knowledge_base 表
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_base' AND COLUMN_NAME = 'deleted');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `ai_knowledge_base` ADD COLUMN `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''删除标记: 0-未删除, 1-已删除'' AFTER `need_reprocess`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_base' AND INDEX_NAME = 'idx_deleted');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE `ai_knowledge_base` ADD INDEX `idx_deleted`(`deleted`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. ai_knowledge_library 表
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_library' AND COLUMN_NAME = 'deleted');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `ai_knowledge_library` ADD COLUMN `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''删除标记: 0-未删除, 1-已删除'' AFTER `updated_at`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_knowledge_library' AND INDEX_NAME = 'idx_deleted');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE `ai_knowledge_library` ADD INDEX `idx_deleted`(`deleted`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. ai_domain_dictionary 表
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_domain_dictionary' AND COLUMN_NAME = 'deleted');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `ai_domain_dictionary` ADD COLUMN `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''删除标记: 0-未删除, 1-已删除'' AFTER `update_time`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_domain_dictionary' AND INDEX_NAME = 'idx_deleted');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE `ai_domain_dictionary` ADD INDEX `idx_deleted`(`deleted`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. ai_agent_tool 表
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_tool' AND COLUMN_NAME = 'deleted');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `ai_agent_tool` ADD COLUMN `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''删除标记: 0-未删除, 1-已删除'' AFTER `update_time`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_tool' AND INDEX_NAME = 'idx_deleted');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE `ai_agent_tool` ADD INDEX `idx_deleted`(`deleted`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 8. ai_conversation 表
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_conversation' AND COLUMN_NAME = 'deleted');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `ai_conversation` ADD COLUMN `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''删除标记: 0-未删除, 1-已删除'' AFTER `summary_updated_at`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_conversation' AND INDEX_NAME = 'idx_deleted');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE `ai_conversation` ADD INDEX `idx_deleted`(`deleted`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 校验查询
-- 升级后执行，应返回 9 行（含 ai_datasource_config 既有 deleted 列）
-- =====================================================================
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND COLUMN_NAME = 'deleted'
  AND TABLE_NAME IN (
    'ai_agent', 'ai_model_config', 'ai_workflow', 'ai_knowledge_base', 'ai_knowledge_library',
    'ai_domain_dictionary', 'ai_agent_tool', 'ai_conversation', 'ai_datasource_config'
  )
ORDER BY TABLE_NAME;


-- ---------------------------------------------------------------
-- 来源: 115_升级脚本_v7.23_支付订单字段补齐.sql
-- ---------------------------------------------------------------
-- =============================================================================
-- v7.23 升级脚本：支付订单字段补齐（为未来接入真实支付渠道预留）
-- 适配 MySQL 8.0+（不使用 MariaDB 扩展 ADD COLUMN IF NOT EXISTS / ADD INDEX IF NOT EXISTS，
--                  改用 information_schema 校验 + PREPARE/EXECUTE 动态 SQL 实现幂等）
-- 背景：
--   P2-6 评审项：当前项目仅采用积分体系（不接入真实支付），
--   portal_order / portal_tip_order 表已有 status / pay_method 字段，
--   但缺少真实支付链路所需的：第三方交易号、回调ID、退款号等字段。
--   本脚本补齐这些字段，为未来接入支付宝/微信支付做表结构准备。
--   当前业务不受影响（积分打赏路径不写入这些字段）。
-- 字段说明：
--   trade_no      第三方交易号（支付宝/微信返回的交易号）
--   pay_channel   支付渠道：points(积分)/alipay(支付宝)/wechat(微信支付)
--   notify_id     支付回调ID（用于回调验签与幂等去重）
--   notify_time   支付回调时间
--   refund_no     退款单号（业务生成的退款流水号）
--   refund_amount 退款金额
--   refund_time   退款时间
--   refund_reason 退款原因
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 通用幂等工具：为指定表追加一列（若不存在）
-- 调用方式：CALL add_column_if_missing('table_name', 'col_name', 'col_def_sql', 'after_col');
-- 其中 col_def_sql 是完整的列定义（含类型/默认值/注释），after_col 为 AFTER 子句的列名（NULL 表示不加 AFTER）
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `p_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `p_add_column_if_missing`(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_def TEXT,
    IN p_after VARCHAR(64)
)
BEGIN
    DECLARE col_count INT;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column;
    IF col_count = 0 THEN
        IF p_after IS NULL OR p_after = '' THEN
            SET @ddl := CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_def);
        ELSE
            SET @ddl := CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_def, ' AFTER `', p_after, '`');
        END IF;
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- -----------------------------------------------------------------------------
-- 通用幂等工具：为指定表追加一个索引（若不存在）
-- 调用方式：CALL add_index_if_missing('table_name', 'index_name', 'index_columns');
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `p_add_index_if_missing`;
DELIMITER $$
CREATE PROCEDURE `p_add_index_if_missing`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    DECLARE idx_count INT;
    SELECT COUNT(*) INTO idx_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index;
    IF idx_count = 0 THEN
        SET @ddl := CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '`(', p_columns, ')');
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- =============================================================================
-- 1. portal_order 表：补齐支付/退款相关字段
-- =============================================================================
CALL p_add_column_if_missing('portal_order', 'trade_no',       "varchar(64) DEFAULT NULL COMMENT '第三方交易号（支付宝/微信返回的交易号）'", 'pay_method');
CALL p_add_column_if_missing('portal_order', 'pay_channel',    "varchar(20) DEFAULT 'points' COMMENT '支付渠道：points-积分/alipay-支付宝/wechat-微信支付'", 'trade_no');
CALL p_add_column_if_missing('portal_order', 'notify_id',       "varchar(64) DEFAULT NULL COMMENT '支付回调ID（用于回调验签与幂等去重）'", 'pay_channel');
CALL p_add_column_if_missing('portal_order', 'notify_time',     "datetime DEFAULT NULL COMMENT '支付回调时间'", 'notify_id');
CALL p_add_column_if_missing('portal_order', 'refund_no',        "varchar(64) DEFAULT NULL COMMENT '退款单号'", 'notify_time');
CALL p_add_column_if_missing('portal_order', 'refund_amount',    "decimal(10,2) DEFAULT NULL COMMENT '退款金额'", 'refund_no');
CALL p_add_column_if_missing('portal_order', 'refund_time',     "datetime DEFAULT NULL COMMENT '退款时间'", 'refund_amount');
CALL p_add_column_if_missing('portal_order', 'refund_reason',   "varchar(255) DEFAULT NULL COMMENT '退款原因'", 'refund_time');

-- 1.1 为常用查询场景建立索引
CALL p_add_index_if_missing('portal_order', 'idx_trade_no',          '`trade_no`');
CALL p_add_index_if_missing('portal_order', 'idx_pay_channel_status', '`pay_channel`, `status`');

-- =============================================================================
-- 2. portal_tip_order 表：补齐支付/退款相关字段（与 portal_order 对齐）
-- =============================================================================
CALL p_add_column_if_missing('portal_tip_order', 'trade_no',       "varchar(64) DEFAULT NULL COMMENT '第三方交易号（支付宝/微信返回的交易号）'", 'pay_method');
CALL p_add_column_if_missing('portal_tip_order', 'pay_channel',    "varchar(20) DEFAULT 'points' COMMENT '支付渠道：points-积分/alipay-支付宝/wechat-微信支付'", 'trade_no');
CALL p_add_column_if_missing('portal_tip_order', 'notify_id',       "varchar(64) DEFAULT NULL COMMENT '支付回调ID（用于回调验签与幂等去重）'", 'pay_channel');
CALL p_add_column_if_missing('portal_tip_order', 'notify_time',     "datetime DEFAULT NULL COMMENT '支付回调时间'", 'notify_id');
CALL p_add_column_if_missing('portal_tip_order', 'refund_no',        "varchar(64) DEFAULT NULL COMMENT '退款单号'", 'notify_time');
CALL p_add_column_if_missing('portal_tip_order', 'refund_amount',    "decimal(10,2) DEFAULT NULL COMMENT '退款金额'", 'refund_no');
CALL p_add_column_if_missing('portal_tip_order', 'refund_time',     "datetime DEFAULT NULL COMMENT '退款时间'", 'refund_amount');
CALL p_add_column_if_missing('portal_tip_order', 'refund_reason',   "varchar(255) DEFAULT NULL COMMENT '退款原因'", 'refund_time');

CALL p_add_index_if_missing('portal_tip_order', 'idx_trade_no',          '`trade_no`');
CALL p_add_index_if_missing('portal_tip_order', 'idx_pay_channel_status', '`pay_channel`, `status`');

-- =============================================================================
-- 3. portal_wallet_transaction 表：补齐第三方交易号与渠道字段
-- =============================================================================
CALL p_add_column_if_missing('portal_wallet_transaction', 'trade_no',   "varchar(64) DEFAULT NULL COMMENT '第三方交易号（充值/提现场景的渠道方流水号）'", 'order_id');
CALL p_add_column_if_missing('portal_wallet_transaction', 'channel',    "varchar(20) DEFAULT NULL COMMENT '资金渠道：alipay/wechat/bank'", 'trade_no');
CALL p_add_column_if_missing('portal_wallet_transaction', 'refund_no',  "varchar(64) DEFAULT NULL COMMENT '退款单号'", 'channel');

CALL p_add_index_if_missing('portal_wallet_transaction', 'idx_trade_no', '`trade_no`');

-- 清理临时存储过程
DROP PROCEDURE IF EXISTS `p_add_column_if_missing`;
DROP PROCEDURE IF EXISTS `p_add_index_if_missing`;

-- =============================================================================
-- 字段补齐后，应用代码（PortalOrder / PortalTipOrder / PortalWalletTransaction 实体）
-- 需在后续迭代中补充对应字段映射，当前业务不受影响（积分路径不写入这些字段）。
-- 后续接入支付渠道时的工作清单：
--   1. 引入支付 SDK（如 alipay-sdk-java、weixin-java-pay）到 pom.xml；
--   2. 创建 PayConfig / AlipayConfig / WechatPayConfig 配置类（@ConfigurationProperties）；
--   3. 创建 PayService 接口与各渠道实现（统一下单 / 回调验签 / 状态查询 / 退款）；
--   4. 在 application-*.yaml 中配置支付密钥（生产环境通过环境变量注入，不写入文件）；
--   5. 实体类补充 trade_no / pay_channel / notify_id 等字段映射；
--   6. 启用 PortalTipServiceImpl 中 article_paid 分支的占位代码。
-- =============================================================================


-- ---------------------------------------------------------------
-- 来源: 117_升级脚本_v7.25_知识库表时间字段统一.sql
-- ---------------------------------------------------------------
-- =====================================================================
-- v7.25 升级脚本：知识库表时间字段统一（P3-2 Phase 2）
-- 适配 MySQL 8.0+
-- 说明：本脚本幂等，可重复执行（基于 information_schema 判断后操作）
-- 背景：
--   1. P3-2 Phase 1 已将 7 个 AI 核心实体迁移继承 AiBaseEntity，
--      统一使用 createTime / updateTime / deleted 字段命名。
--   2. 知识库两张核心表仍使用旧命名：
--        - knowledge_library: created_at / updated_at
--        - knowledge_base:     upload_time / process_time
--      实体侧 KnowledgeLibrary / KnowledgeBase 即将继承 AiBaseEntity，
--      其字段映射为 create_time / update_time / deleted 列，故需先统一列名。
--   3. 本脚本将上述 2 张表的时间列重命名：
--        knowledge_library.created_at  → create_time
--        knowledge_library.updated_at  → update_time
--        knowledge_base.upload_time    → create_time
--        knowledge_base.process_time   → update_time
--      并同步重命名相关索引（idx_created_at / idx_upload_time → idx_create_time）。
--   4. 语义说明（knowledge_base）：
--        - 原 upload_time 表示"上传时间"，重命名为 create_time 后语义保持
--          （记录创建=文档上传时刻，由 DEFAULT CURRENT_TIMESTAMP 兜底）。
--        - 原 process_time 表示"处理时间"，重命名为 update_time 后语义扩展为
--          "最后修改时间"（含处理、状态变更、配置更新等）。
--          ServiceImpl 在处理路径仍显式 setUpdateTime(now)，保留"最近处理"语义；
--          详细的"上次处理时间"由独立的 last_processed_time 列承载，不受影响。
--   5. knowledge_base.deleted 列由 113 脚本已添加（AFTER need_reprocess），
--      本脚本不涉及；knowledge_library.deleted 由 113 脚本添加（原 AFTER updated_at），
--      RENAME COLUMN 保持列位置不变，deleted 列继续存在。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 一、knowledge_library 表：created_at → create_time，updated_at → update_time
-- ---------------------------------------------------------------------

-- 1.1 重命名列：created_at → create_time（仅当 created_at 存在时）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_knowledge_library'
    AND COLUMN_NAME = 'created_at'
);
SET @sql := IF(@col_exists = 1,
  'ALTER TABLE `ai_knowledge_library` RENAME COLUMN `created_at` TO `create_time`',
  'SELECT "knowledge_library.created_at 已重命名或不存在，跳过" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 重命名列：updated_at → update_time（仅当 updated_at 存在时）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_knowledge_library'
    AND COLUMN_NAME = 'updated_at'
);
SET @sql := IF(@col_exists = 1,
  'ALTER TABLE `ai_knowledge_library` RENAME COLUMN `updated_at` TO `update_time`',
  'SELECT "knowledge_library.updated_at 已重命名或不存在，跳过" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 索引重命名：idx_created_at → idx_create_time
--     先删旧索引（若存在），再建新索引（动态 SQL 守护幂等，兼容 MySQL 8.0）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_knowledge_library'
    AND INDEX_NAME = 'idx_created_at'
);
SET @sql := IF(@idx_exists = 1,
  'ALTER TABLE `ai_knowledge_library` DROP INDEX `idx_created_at`',
  'SELECT "knowledge_library.idx_created_at 不存在，跳过删除" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 新建 idx_create_time（若不存在）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_knowledge_library'
    AND INDEX_NAME = 'idx_create_time'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE `ai_knowledge_library` ADD INDEX `idx_create_time`(`create_time`)',
  'SELECT "knowledge_library.idx_create_time 已存在，跳过" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 二、knowledge_base 表：upload_time → create_time，process_time → update_time
-- ---------------------------------------------------------------------

-- 2.1 重命名列：upload_time → create_time（仅当 upload_time 存在时）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_knowledge_base'
    AND COLUMN_NAME = 'upload_time'
);
SET @sql := IF(@col_exists = 1,
  'ALTER TABLE `ai_knowledge_base` RENAME COLUMN `upload_time` TO `create_time`',
  'SELECT "knowledge_base.upload_time 已重命名或不存在，跳过" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 重命名列：process_time → update_time（仅当 process_time 存在时）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_knowledge_base'
    AND COLUMN_NAME = 'process_time'
);
SET @sql := IF(@col_exists = 1,
  'ALTER TABLE `ai_knowledge_base` RENAME COLUMN `process_time` TO `update_time`',
  'SELECT "knowledge_base.process_time 已重命名或不存在，跳过" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.3 索引重命名：idx_upload_time → idx_create_time
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_knowledge_base'
    AND INDEX_NAME = 'idx_upload_time'
);
SET @sql := IF(@idx_exists = 1,
  'ALTER TABLE `ai_knowledge_base` DROP INDEX `idx_upload_time`',
  'SELECT "knowledge_base.idx_upload_time 不存在，跳过删除" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 新建 idx_create_time（若不存在）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_knowledge_base'
    AND INDEX_NAME = 'idx_create_time'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE `ai_knowledge_base` ADD INDEX `idx_create_time`(`create_time`)',
  'SELECT "knowledge_base.idx_create_time 已存在，跳过" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 三、校验查询（升级后执行，确认列与索引就位）
-- ---------------------------------------------------------------------
-- SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
-- FROM information_schema.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('ai_knowledge_library', 'ai_knowledge_base')
--   AND COLUMN_NAME IN ('create_time', 'update_time', 'deleted')
-- ORDER BY TABLE_NAME, COLUMN_NAME;
--
-- SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME
-- FROM information_schema.STATISTICS
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('ai_knowledge_library', 'ai_knowledge_base')
--   AND INDEX_NAME = 'idx_create_time'
-- ORDER BY TABLE_NAME;

-- =====================================================================
-- 升级完成
-- 后续配套：
--   - 实体 KnowledgeLibrary / KnowledgeBase 继承 AiBaseEntity，
--     删除自有的 createdAt/updatedAt 或 uploadTime/processTime/deleted 字段
--   - KnowledgeLibraryServiceImpl / KnowledgeBaseServiceImpl /
--     StartupTaskRunner 同步替换字段访问方法
--   - VO（KnowledgeLibraryVO / KnowledgeBaseVO）字段名保持不变以维持前端 API 契约，
--     仅更新 Entity→VO 的映射代码
-- =====================================================================


-- =====================================================================
-- 四、基础数据段（用户/角色/菜单/栏目/标签/字典，不含业务测试数据）
-- =====================================================================

-- ============================================================--
-- 4.1 系统基础数据（来源: 80_系统种子数据.sql，剔除 sys_notice_bak）
-- ============================================================--
-- 来源：all-db-ddl.sql（多段提取，已调整依赖顺序）
-- 用途：系统基础表种子数据（INSERT 语句，保留 LOCK/UNLOCK）
-- 依赖顺序：sys_dict_type → sys_dict_data；sys_role → sys_user → sys_user_role

-- sys_config 种子数据（6 条，无 LOCK/UNLOCK）
INSERT INTO sys_config VALUES(1, '主框架页-默认皮肤样式名称',     'sys.index.skinName',            'skin-blue',     'Y', 'admin', NOW(), '', NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow', '0');
INSERT INTO sys_config VALUES(2, '用户管理-账号初始密码',         'sys.user.initPassword',         '123456',        'Y', 'admin', NOW(), '', NULL, '初始化密码 123456', '0');
INSERT INTO sys_config VALUES(3, '主框架页-侧边栏主题',           'sys.index.sideTheme',           'theme-dark',    'Y', 'admin', NOW(), '', NULL, '深色主题theme-dark，浅色主题theme-light', '0');
INSERT INTO sys_config VALUES(4, '账号自助-验证码开关',           'sys.account.captchaEnabled',    'true',          'Y', 'admin', NOW(), '', NULL, '是否开启验证码功能（true开启，false关闭）', '0');
INSERT INTO sys_config VALUES(5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser',      'false',         'Y', 'admin', NOW(), '', NULL, '是否开启注册用户功能（true开启，false关闭）', '0');
INSERT INTO sys_config VALUES(6, '用户登录-黑名单列表',           'sys.login.blackIPList',         '',              'Y', 'admin', NOW(), '', NULL, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）', '0');

-- sys_dept 种子数据（10 条）
LOCK TABLES `sys_dept` WRITE;
-- 初始化-部门表数据
INSERT INTO sys_dept VALUES(100,  0,   '0',          '若依科技',   0, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '',NOW(), NULL);
INSERT INTO sys_dept VALUES(101,  100, '0,100',      '深圳总公司', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '',NOW(), NULL);
INSERT INTO sys_dept VALUES(102,  100, '0,100',      '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '',NOW(), NULL);
INSERT INTO sys_dept VALUES(103,  101, '0,100,101',  '研发部门',   1, '若依', '15888888888', 'ry@qq.com', '0', '0','admin', NOW(), '',NOW(), NULL);
INSERT INTO sys_dept VALUES(104,  101, '0,100,101',  '市场部门',   2, '若依', '15888888888', 'ry@qq.com', '0', '0','admin', NOW(), '',NOW(), NULL);
INSERT INTO sys_dept VALUES(105,  101, '0,100,101',  '测试部门',   3, '若依', '15888888888', 'ry@qq.com', '0', '0','admin', NOW(), '',NOW(), NULL);
INSERT INTO sys_dept VALUES(106,  101, '0,100,101',  '财务部门',   4, '若依', '15888888888', 'ry@qq.com', '0', '0','admin', NOW(), '',NOW(), NULL);
INSERT INTO sys_dept VALUES(107,  101, '0,100,101',  '运维部门',   5, '若依', '15888888888', 'ry@qq.com', '0', '0','admin', NOW(), '',NOW(), NULL);
INSERT INTO sys_dept VALUES(108,  102, '0,100,102',  '市场部门',   1, '若依', '15888888888', 'ry@qq.com', '0', '0','admin', NOW(), '',NOW(), NULL);
INSERT INTO sys_dept VALUES(109,  102, '0,100,102',  '财务部门',   2, '若依', '15888888888', 'ry@qq.com', '0', '0','admin', NOW(), '',NOW(), NULL);

UNLOCK TABLES;

-- sys_dict_type 种子数据（15 条，须先于 sys_dict_data）
LOCK TABLES `sys_dict_type` WRITE;
INSERT INTO sys_dict_type VALUES(1,  '用户性别', 'sys_user_sex',        '0', 'admin', NOW(), '', NULL, '用户性别列表', '0');
INSERT INTO sys_dict_type VALUES(2,  '菜单状态', 'sys_show_hide',       '0', 'admin', NOW(), '', NULL, '菜单状态列表', '0');
INSERT INTO sys_dict_type VALUES(3,  '系统开关', 'sys_normal_disable',  '0', 'admin', NOW(), '', NULL, '系统开关列表', '0');
INSERT INTO sys_dict_type VALUES(4,  '任务状态', 'sys_job_status',      '0', 'admin', NOW(), '', NULL, '任务状态列表', '0');
INSERT INTO sys_dict_type VALUES(5,  '任务分组', 'sys_job_group',       '0', 'admin', NOW(), '', NULL, '任务分组列表', '0');
INSERT INTO sys_dict_type VALUES(6,  '系统是否', 'sys_yes_no',          '0', 'admin', NOW(), '', NULL, '系统是否列表', '0');
INSERT INTO sys_dict_type VALUES(7,  '通知类型', 'sys_notice_type',     '0', 'admin', NOW(), '', NULL, '通知类型列表', '0');
INSERT INTO sys_dict_type VALUES(8,  '通知状态', 'sys_notice_status',   '0', 'admin', NOW(), '', NULL, '通知状态列表', '0');
INSERT INTO sys_dict_type VALUES(9,  '操作类型', 'sys_oper_type',       '0', 'admin', NOW(), '', NULL, '操作类型列表', '0');
INSERT INTO sys_dict_type VALUES(10, '系统状态', 'sys_common_status',   '0', 'admin', NOW(), '', NULL, '登录状态列表', '0');
INSERT INTO sys_dict_type VALUES(11, '表达式类型', 'exp_data_type',       '0', 'admin', NOW(), '', NULL, '表达式类型', '0');
INSERT INTO sys_dict_type VALUES(12, '监听类型', 'sys_listener_type',    '0', 'admin', NOW(), '', NULL, '监听类型', '0');
INSERT INTO sys_dict_type VALUES(13, '监听值类型', 'sys_listener_value_type', '0', 'admin', NOW(), '', NULL, '监听值类型', '0');
INSERT INTO sys_dict_type VALUES(14, '监听属性', 'sys_listener_event_type', '0', 'admin', NOW(), '', NULL, '监听属性', '0');
INSERT INTO sys_dict_type VALUES(15, '流程分类', 'sys_process_category', '0', 'admin', NOW(), '', NULL, '流程分类', '0');

UNLOCK TABLES;

-- sys_dict_data 种子数据（38 条）
LOCK TABLES `sys_dict_data` WRITE;

INSERT INTO sys_dict_data VALUES(1,  1,  '男',       '0',       'sys_user_sex',        '',   '',        'Y', '0', 'admin', NOW(), '', NULL, '性别男', '0');
INSERT INTO sys_dict_data VALUES(2,  2,  '女',       '1',       'sys_user_sex',        '',   '',        'N', '0', 'admin', NOW(), '', NULL, '性别女', '0');
INSERT INTO sys_dict_data VALUES(3,  3,  '未知',     '2',       'sys_user_sex',        '',   '',        'N', '0', 'admin', NOW(), '', NULL, '性别未知', '0');
INSERT INTO sys_dict_data VALUES(4,  1,  '显示',     '0',       'sys_show_hide',       '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '显示菜单', '0');
INSERT INTO sys_dict_data VALUES(5,  2,  '隐藏',     '1',       'sys_show_hide',       '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '隐藏菜单', '0');
INSERT INTO sys_dict_data VALUES(6,  1,  '正常',     '0',       'sys_normal_disable',  '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '正常状态', '0');
INSERT INTO sys_dict_data VALUES(7,  2,  '停用',     '1',       'sys_normal_disable',  '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '停用状态', '0');
INSERT INTO sys_dict_data VALUES(8,  1,  '正常',     '0',       'sys_job_status',      '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '正常状态', '0');
INSERT INTO sys_dict_data VALUES(9,  2,  '暂停',     '1',       'sys_job_status',      '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '停用状态', '0');
INSERT INTO sys_dict_data VALUES(10, 1,  '默认',     'DEFAULT', 'sys_job_group',       '',   '',        'Y', '0', 'admin', NOW(), '', NULL, '默认分组', '0');
INSERT INTO sys_dict_data VALUES(11, 2,  '系统',     'SYSTEM',  'sys_job_group',       '',   '',        'N', '0', 'admin', NOW(), '', NULL, '系统分组', '0');
INSERT INTO sys_dict_data VALUES(12, 1,  '是',       'Y',       'sys_yes_no',          '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '系统默认是', '0');
INSERT INTO sys_dict_data VALUES(13, 2,  '否',       'N',       'sys_yes_no',          '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '系统默认否', '0');
INSERT INTO sys_dict_data VALUES(14, 1,  '通知',     '1',       'sys_notice_type',     '',   'warning', 'Y', '0', 'admin', NOW(), '', NULL, '通知', '0');
INSERT INTO sys_dict_data VALUES(15, 2,  '公告',     '2',       'sys_notice_type',     '',   'success', 'N', '0', 'admin', NOW(), '', NULL, '公告', '0');
INSERT INTO sys_dict_data VALUES(16, 1,  '正常',     '0',       'sys_notice_status',   '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '正常状态', '0');
INSERT INTO sys_dict_data VALUES(17, 2,  '关闭',     '1',       'sys_notice_status',   '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '关闭状态', '0');
INSERT INTO sys_dict_data VALUES(18, 99, '其他',     '0',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', NOW(), '', NULL, '其他操作', '0');
INSERT INTO sys_dict_data VALUES(19, 1,  '新增',     '1',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', NOW(), '', NULL, '新增操作', '0');
INSERT INTO sys_dict_data VALUES(20, 2,  '修改',     '2',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', NOW(), '', NULL, '修改操作', '0');
INSERT INTO sys_dict_data VALUES(21, 3,  '删除',     '3',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '删除操作', '0');
INSERT INTO sys_dict_data VALUES(22, 4,  '授权',     '4',       'sys_oper_type',       '',   'primary', 'N', '0', 'admin', NOW(), '', NULL, '授权操作', '0');
INSERT INTO sys_dict_data VALUES(23, 5,  '导出',     '5',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', NOW(), '', NULL, '导出操作', '0');
INSERT INTO sys_dict_data VALUES(24, 6,  '导入',     '6',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', NOW(), '', NULL, '导入操作', '0');
INSERT INTO sys_dict_data VALUES(25, 7,  '强退',     '7',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '强退操作', '0');
INSERT INTO sys_dict_data VALUES(26, 8,  '生成代码', '8',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', NOW(), '', NULL, '生成操作', '0');
INSERT INTO sys_dict_data VALUES(27, 9,  '清空数据', '9',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '清空操作', '0');
INSERT INTO sys_dict_data VALUES(28, 1,  '成功',     '0',       'sys_common_status',   '',   'primary', 'N', '0', 'admin', NOW(), '', NULL, '正常状态', '0');
INSERT INTO sys_dict_data VALUES(29, 2,  '失败',     '1',       'sys_common_status',   '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '停用状态', '0');
INSERT INTO sys_dict_data VALUES(30, 0,  '系统指定', 'fixed',    'exp_data_type',       NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '表达式类型', '0');
INSERT INTO sys_dict_data VALUES(31, 1,  '动态选择', 'dynamic',  'exp_data_type',       NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '表达式类型', '0');
INSERT INTO sys_dict_data VALUES(32, 0,  '任务监听', '1',        'sys_listener_type',   NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '监听类型', '0');
INSERT INTO sys_dict_data VALUES(33, 2,  '执行监听', '2',        'sys_listener_type',   NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '监听类型', '0');
INSERT INTO sys_dict_data VALUES(34, 0,  'JAVA类',  'classListener',     'sys_listener_value_type', NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '监听值类型', '0');
INSERT INTO sys_dict_data VALUES(35, 1,  '表达式',  'expressionListener','sys_listener_value_type', NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '监听值类型', '0');
INSERT INTO sys_dict_data VALUES(36, 2,  '代理表达式', 'delegateExpressionListener','sys_listener_value_type', NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '监听值类型', '0');
INSERT INTO sys_dict_data VALUES(37, 0,  '请假',     'leave',    'sys_process_category', NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '流程分类', '0');
INSERT INTO sys_dict_data VALUES(38, 1,  '报销',     'expense',  'sys_process_category', NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '流程分类', '0');

UNLOCK TABLES;

-- sys_job 种子数据（3 条）
LOCK TABLES `sys_job` WRITE;
INSERT INTO `sys_job` VALUES (1,'系统默认（无参）','DEFAULT','ryTask.ryNoParams','0/10 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),(2,'系统默认（有参）','DEFAULT','ryTask.ryParams(\'ry\')','0/15 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),(3,'系统默认（多参）','DEFAULT','ryTask.ryMultipleParams(\'ry\', true, 2000, 316.50, 100)','0/20 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0');
UNLOCK TABLES;

-- sys_post 种子数据（4 条）
LOCK TABLES `sys_post` WRITE;
-- 初始化-岗位信息表数据
INSERT INTO sys_post VALUES(1, 'ceo',  '董事长',    1, '0', 'admin', NOW(), '', NULL, '', '0');;
INSERT INTO sys_post VALUES(2, 'se',   '项目经理',  2, '0', 'admin', NOW(), '', NULL, '' , '0');
INSERT INTO sys_post VALUES(3, 'hr',   '人力资源',  3, '0', 'admin', NOW(), '', NULL, '' , '0');
INSERT INTO sys_post VALUES(4, 'user', '普通员工',  4, '0', 'admin', NOW(), '', NULL, '' , '0');
UNLOCK TABLES;

-- sys_role 种子数据（2 条，须先于 sys_user）
LOCK TABLES `sys_role` WRITE;
INSERT INTO sys_role VALUES('1', '超级管理员',  'admin',  1, 1, 1, 1, '0', '0', 'admin', NOW(), '', NULL, '超级管理员');
INSERT INTO sys_role VALUES('2', '普通角色',    'common', 2, 2, 1, 1, '0', '0', 'admin', NOW(), '', NULL, '普通角色');
UNLOCK TABLES;

-- sys_role_dept 种子数据（3 条）
LOCK TABLES `sys_role_dept` WRITE;
INSERT INTO `sys_role_dept` VALUES (2,100,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,101,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,105,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL);
UNLOCK TABLES;

-- sys_user 种子数据（2 条，须先于 sys_user_role）
LOCK TABLES `sys_user` WRITE;
-- 初始化-用户信息表数据
INSERT INTO sys_user VALUES(1,  103, 'admin', '若依', '00', 'ry@163.com', '15888888888', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '', NULL, '管理员');
INSERT INTO sys_user VALUES(2,  105, 'ry',    '若依', '00', 'ry@qq.com',  '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '', NULL, '测试员');
UNLOCK TABLES;

-- sys_user_post 种子数据（2 条）
LOCK TABLES `sys_user_post` WRITE;
INSERT INTO `sys_user_post` VALUES (1,1,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,2,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL);
UNLOCK TABLES;

-- sys_user_role 种子数据（2 条）
LOCK TABLES `sys_user_role` WRITE;
INSERT INTO `sys_user_role` VALUES (1, 1,'admin', NOW(), '', NOW(), NULL);
INSERT INTO `sys_user_role` VALUES (2, 2,'admin', NOW(), '', NOW(), NULL);

UNLOCK TABLES;


-- ============================================================--
-- 4.2 门户基础数据（来源: 81_门户种子数据.sql，剔除业务测试数据）
-- 保留: portal_category/portal_tag/portal_friend_link/portal_growth_rule/
--       portal_help_category/portal_interview_category/portal_achievement/
--       portal_interview_position/portal_task
-- 删除: portal_book*/portal_help_article/portal_shop_item/portal_user/portal_writing_prompt
-- ============================================================--
-- 来源：all-db-ddl.sql（多段提取，按原文件顺序）
-- 用途：门户业务表种子数据（INSERT 语句，保留 LOCK/UNLOCK）

-- portal_achievement 种子数据
LOCK TABLES `portal_achievement` WRITE;
INSERT INTO `portal_achievement` VALUES (1,'first_article','初露锋芒','发布第一篇文章',NULL,'article','{\"action\":\"publish_article\",\"count\":1}',20,1,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(2,'article_10','勤勉作者','发布10篇文章',NULL,'article','{\"action\":\"publish_article\",\"count\":10}',50,2,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(3,'article_50','高产作者','发布50篇文章',NULL,'article','{\"action\":\"publish_article\",\"count\":50}',200,3,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(4,'article_featured','精华创作者','文章被精选',NULL,'article','{\"action\":\"article_featured\",\"count\":1}',100,4,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(5,'article_100_likes','人气作者','单篇文章获赞100',NULL,'article','{\"action\":\"receive_like\",\"count\":100}',50,5,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(6,'first_book','开卷有益','完成阅读第一本书',NULL,'reading','{\"action\":\"finish_book\",\"count\":1}',20,10,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(7,'book_worm_10','书虫','完成阅读10本书',NULL,'reading','{\"action\":\"finish_book\",\"count\":10}',100,11,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(8,'book_worm_50','阅读达人','完成阅读50本书',NULL,'reading','{\"action\":\"finish_book\",\"count\":50}',300,12,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(9,'first_booklist','书单策划','创建第一个书单',NULL,'reading','{\"action\":\"create_booklist\",\"count\":1}',20,13,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(10,'quote_master','金句达人','发布20条金句',NULL,'reading','{\"action\":\"write_quote\",\"count\":20}',50,14,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(11,'first_solve','初试身手','解答第一道面试题',NULL,'interview','{\"action\":\"solve_question\",\"count\":1}',10,20,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(12,'solve_50','刷题能手','解答50道面试题',NULL,'interview','{\"action\":\"solve_question\",\"count\":50}',100,21,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(13,'solve_200','面试达人','解答200道面试题',NULL,'interview','{\"action\":\"solve_question\",\"count\":200}',300,22,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(14,'first_note','笔记新手','撰写第一篇笔记',NULL,'interview','{\"action\":\"write_note\",\"count\":1}',15,23,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(15,'note_adopted','知识贡献者','笔记被精选',NULL,'interview','{\"action\":\"note_adopted\",\"count\":1}',50,24,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(16,'first_experience','面经分享者','发布第一篇面经',NULL,'interview','{\"action\":\"publish_experience\",\"count\":1}',30,25,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(17,'experience_10','面经达人','发布10篇面经',NULL,'interview','{\"action\":\"publish_experience\",\"count\":10}',100,26,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(18,'checkin_7','坚持一周','连续签到7天',NULL,'all','{\"action\":\"daily_checkin\",\"count\":7}',10,30,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(19,'checkin_30','坚持一月','连续签到30天',NULL,'all','{\"action\":\"daily_checkin\",\"count\":30}',50,31,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(20,'level_5','渐入佳境','达到5级',NULL,'all','{\"action\":\"level\",\"count\":5}',0,32,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(21,'level_8','登峰造极','达到8级',NULL,'all','{\"action\":\"level\",\"count\":8}',0,33,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(22,'first_tip_received','初获鼓励','首次收到打赏',NULL,'article','{\"action\":\"receive_tip\",\"count\":1}',10,7,'0','','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),(23,'generous_tipper','慷慨鼓励','累计打赏他人 10 次',NULL,'article','{\"action\":\"tip_others\",\"count\":10}',30,8,'0','','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL);
UNLOCK TABLES;





-- portal_category（50 条栏目）+ portal_tag（28 条标签）种子数据
-- -----------------------------------------------------------------------------
-- 种子数据：portal_category（50 项：8 一级 + 42 二级）+ portal_tag（28 项）
-- 说明：二级栏目 parent_id 通过 slug 反查一级栏目 id，无需硬编码；直接初始化，不做幂等判断
-- -----------------------------------------------------------------------------

-- 一级栏目（8 项，parent_id = 0）
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) VALUES ('首页', 'home', '精选推荐、双轨轮播', 'fa-home', 1, 0, '0', 1, 'home', '/', 0, 'admin');
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) VALUES ('散文天地', 'prose', '人文书写与情感表达', 'fa-pen-fancy', 2, 0, '0', 1, 'category', NULL, 0, 'admin');
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) VALUES ('技术笔记', 'tech-notes', '开发记录、技术解析、AI编程实践', 'fa-code', 3, 0, '0', 1, 'category', NULL, 0, 'admin');
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) VALUES ('读书空间', 'reading', '读书心得、精选好书、书单推荐', 'fa-book', 4, 0, '0', 1, 'static', '/reading', 0, 'admin');
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) VALUES ('面试指南', 'interview', '真题整理、面经复盘、简历优化', 'fa-briefcase', 5, 0, '0', 1, 'static', '/interview', 0, 'admin');
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) VALUES ('社区互动', 'interaction', '话题讨论、动态广场', 'fa-users', 6, 0, '0', 1, 'category', NULL, 0, 'admin');
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) VALUES ('创作者中心', 'creator', '发布文章、专栏、征文、认证', 'fa-feather', 7, 0, '0', 1, 'category', NULL, 0, 'admin');
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) VALUES ('个人空间', 'mine', '个人中心、成长时间线、我的内容', 'fa-user', 8, 0, '0', 1, 'category', NULL, 0, 'admin');

INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '人间烟火', 'life-stories', '饮食、市井、生活琐记', 'fa-utensils', 1, (SELECT `id` FROM `portal_category` WHERE `slug` = 'prose' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '山河行吟', 'travel-nature', '游记、自然书写、生态散文', 'fa-mountain', 2, (SELECT `id` FROM `portal_category` WHERE `slug` = 'prose' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '心灵独白', 'inner-thoughts', '孤独、成长、疗愈随笔', 'fa-heart', 3, (SELECT `id` FROM `portal_category` WHERE `slug` = 'prose' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '城市笔记', 'city-notes', '北上广深、小镇观察', 'fa-city', 4, (SELECT `id` FROM `portal_category` WHERE `slug` = 'prose' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '四季专栏', 'seasons', '春之思、夏之躁、秋之静、冬之藏', 'fa-leaf', 5, (SELECT `id` FROM `portal_category` WHERE `slug` = 'prose' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '声音散文', 'audio-prose', '作者自读、背景音效沉浸体验', 'fa-volume-up', 6, (SELECT `id` FROM `portal_category` WHERE `slug` = 'prose' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '读者来信', 'reader-letters', '短篇心声刊发与回声计划', 'fa-envelope', 7, (SELECT `id` FROM `portal_category` WHERE `slug` = 'prose' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '技术栈手册', 'tech-stack', 'Java/SpringBoot、React/Vue、Flutter/UniApp', 'fa-book-open', 1, (SELECT `id` FROM `portal_category` WHERE `slug` = 'tech-notes' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '架构札记', 'architecture', '微服务、缓存策略、分布式事务', 'fa-project-diagram', 2, (SELECT `id` FROM `portal_category` WHERE `slug` = 'tech-notes' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '性能日志', 'performance', 'SQL优化、前端加载、JVM调优', 'fa-tachometer-alt', 3, (SELECT `id` FROM `portal_category` WHERE `slug` = 'tech-notes' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select 'AI编程', 'ai-coding', 'Cursor使用、ChatGPT提示工程、AI排错记录', 'fa-robot', 4, (SELECT `id` FROM `portal_category` WHERE `slug` = 'tech-notes' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '开源日志', 'open-source', 'PR提交、Issue解决、源码阅读', 'fa-code-branch', 5, (SELECT `id` FROM `portal_category` WHERE `slug` = 'tech-notes' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '新手入门', 'beginner', '环境配置、第一行代码实录', 'fa-play-circle', 6, (SELECT `id` FROM `portal_category` WHERE `slug` = 'tech-notes' AND `parent_id` = 0), '0', 1, 'category', NULL, 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '读书首页', 'reading-home', '读书空间总入口', 'fa-book-reader', 1, (SELECT `id` FROM `portal_category` WHERE `slug` = 'reading' AND `parent_id` = 0), '0', 1, 'static', '/reading', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '发现好书', 'reading-discover', '发现好书、书单推荐', 'fa-list', 2, (SELECT `id` FROM `portal_category` WHERE `slug` = 'reading' AND `parent_id` = 0), '0', 1, 'static', '/reading/discover', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '金句摘录', 'reading-quotes', '高光语句+个人批注', 'fa-quote-left', 3, (SELECT `id` FROM `portal_category` WHERE `slug` = 'reading' AND `parent_id` = 0), '0', 1, 'static', '/reading/quotes', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '我的书架', 'reading-bookshelf', '个人书架管理', 'fa-bookmark', 4, (SELECT `id` FROM `portal_category` WHERE `slug` = 'reading' AND `parent_id` = 0), '0', 1, 'static', '/reading/bookshelf', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '面试题库', 'interview-questions', '算法题、系统设计、行为面试', 'fa-clipboard-list', 1, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interview' AND `parent_id` = 0), '0', 1, 'static', '/interview/questions', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '面试经验', 'interview-experiences', '大厂面试全流程还原', 'fa-chart-line', 2, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interview' AND `parent_id` = 0), '0', 1, 'static', '/interview/experiences', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '简历模板', 'interview-resume-templates', '技术亮点提炼、项目描述技巧', 'fa-file-alt', 3, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interview' AND `parent_id` = 0), '0', 1, 'static', '/interview/resume-templates', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select 'AI 模拟面试', 'interview-mock', '自测题集、答题思路拆解', 'fa-microphone', 4, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interview' AND `parent_id` = 0), '0', 1, 'static', '/interview/mock', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '学习中心', 'learn-center', '学习中心总入口', 'fa-graduation-cap', 5, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interview' AND `parent_id` = 0), '0', 1, 'static', '/learn', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '知识图谱', 'learn-knowledge', '知识体系可视化', 'fa-project-diagram', 6, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interview' AND `parent_id` = 0), '0', 1, 'static', '/learn/knowledge', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '刷题排行榜', 'learn-leaderboard', '刷题榜、学习榜', 'fa-trophy', 7, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interview' AND `parent_id` = 0), '0', 1, 'static', '/learn/leaderboard', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '学习计划', 'learn-plan', '个人学习计划管理', 'fa-calendar-alt', 8, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interview' AND `parent_id` = 0), '0', 1, 'static', '/learn/plan', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '错题本', 'learn-wrong', '错题归集与复习', 'fa-times-circle', 9, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interview' AND `parent_id` = 0), '0', 1, 'static', '/learn/wrong', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '刷题日历', 'learn-calendar', '刷题打卡日历', 'fa-calendar-check', 10, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interview' AND `parent_id` = 0), '0', 1, 'static', '/learn/calendar', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '话题广场', 'topics', '话题讨论列表', 'fa-comments', 1, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interaction' AND `parent_id` = 0), '0', 1, 'static', '/topics', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '动态广场', 'feed', '用户动态流', 'fa-stream', 2, (SELECT `id` FROM `portal_category` WHERE `slug` = 'interaction' AND `parent_id` = 0), '0', 1, 'static', '/feed', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '发布文章', 'publish', '发布新文章', 'fa-edit', 1, (SELECT `id` FROM `portal_category` WHERE `slug` = 'creator' AND `parent_id` = 0), '0', 1, 'static', '/publish', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '专栏广场', 'columns', '专栏列表与订阅', 'fa-columns', 2, (SELECT `id` FROM `portal_category` WHERE `slug` = 'creator' AND `parent_id` = 0), '0', 1, 'static', '/columns', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '征文活动', 'contests', '征文活动、技术挑战赛', 'fa-file-upload', 3, (SELECT `id` FROM `portal_category` WHERE `slug` = 'creator' AND `parent_id` = 0), '0', 1, 'static', '/contests', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '创作者认证', 'creator-certification', '申请创作者认证', 'fa-certificate', 4, (SELECT `id` FROM `portal_category` WHERE `slug` = 'creator' AND `parent_id` = 0), '0', 1, 'static', '/creator/certification', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '创作者列表', 'authors', '认证创作者列表', 'fa-users-cog', 5, (SELECT `id` FROM `portal_category` WHERE `slug` = 'creator' AND `parent_id` = 0), '0', 1, 'static', '/authors', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '成长排行榜', 'ranking', '成长值排行榜', 'fa-trophy', 6, (SELECT `id` FROM `portal_category` WHERE `slug` = 'creator' AND `parent_id` = 0), '0', 1, 'static', '/ranking', 0, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '个人中心', 'user', '个人中心主页', 'fa-user-circle', 1, (SELECT `id` FROM `portal_category` WHERE `slug` = 'mine' AND `parent_id` = 0), '0', 1, 'static', '/user', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '成长时间线', 'growth-timeline', '成长记录时间线', 'fa-chart-line', 2, (SELECT `id` FROM `portal_category` WHERE `slug` = 'mine' AND `parent_id` = 0), '0', 1, 'static', '/growth/timeline', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '我的专栏', 'column-my', '我创建的专栏', 'fa-columns', 3, (SELECT `id` FROM `portal_category` WHERE `slug` = 'mine' AND `parent_id` = 0), '0', 1, 'static', '/column/my', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '我的文章', 'my-articles', '我发布的文章', 'fa-file-alt', 4, (SELECT `id` FROM `portal_category` WHERE `slug` = 'mine' AND `parent_id` = 0), '0', 1, 'static', '/my/articles', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '我的话题', 'topic-my-topics', '我发起的话题', 'fa-comments', 5, (SELECT `id` FROM `portal_category` WHERE `slug` = 'mine' AND `parent_id` = 0), '0', 1, 'static', '/topic/my/topics', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '我的观点', 'topic-my-posts', '我发表的观点', 'fa-comment', 6, (SELECT `id` FROM `portal_category` WHERE `slug` = 'mine' AND `parent_id` = 0), '0', 1, 'static', '/topic/my/posts', 1, 'admin';
INSERT INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `show_in_nav`, `nav_route_type`, `nav_route_path`, `requires_auth`, `create_by`) select '我的成就', 'achievements', '我的成就与徽章', 'fa-award', 7, (SELECT `id` FROM `portal_category` WHERE `slug` = 'mine' AND `parent_id` = 0), '0', 1, 'static', '/achievements', 1, 'admin';

-- 标签种子数据（28 项：8 人文 + 12 技术 + 8 通用）
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('生活哲思', 'life-philosophy', 1, '0', 'admin', '人文类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('城市记忆', 'city-memory', 2, '0', 'admin', '人文类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('自然写作', 'nature-writing', 3, '0', 'admin', '人文类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('情感随笔', 'emotional-essay', 4, '0', 'admin', '人文类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('人间烟火', 'life-fireworks', 5, '0', 'admin', '人文类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('乡愁记忆', 'nostalgia', 6, '0', 'admin', '人文类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('孤独成长', 'loneliness-growth', 7, '0', 'admin', '人文类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('四季感悟', 'seasons-feeling', 8, '0', 'admin', '人文类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('SpringBoot实战', 'springboot-practice', 9, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('React Hooks', 'react-hooks', 10, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('AI辅助开发', 'ai-assisted-dev', 11, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('算法突破', 'algorithm-breakthrough', 12, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('Java并发', 'java-concurrency', 13, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('Vue3实践', 'vue3-practice', 14, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('微服务架构', 'microservices', 15, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('MySQL优化', 'mysql-optimization', 16, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('Git协作', 'git-collaboration', 17, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('前端性能', 'frontend-performance', 18, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('JVM调优', 'jvm-tuning', 19, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('系统设计', 'system-design', 20, '0', 'admin', '技术类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('新手入门', 'beginner-guide', 21, '0', 'admin', '通用类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('进阶提升', 'advanced-improvement', 22, '0', 'admin', '通用类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('面试备战', 'interview-prep', 23, '0', 'admin', '通用类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('读书心得', 'reading-notes', 24, '0', 'admin', '通用类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('写作技巧', 'writing-tips', 25, '0', 'admin', '通用类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('学习方法', 'learning-methods-tag', 26, '0', 'admin', '通用类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('职场经验', 'career-experience', 27, '0', 'admin', '通用类');
INSERT INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES ('个人成长', 'personal-growth', 28, '0', 'admin', '通用类');

-- portal_friend_link 种子数据
LOCK TABLES `portal_friend_link` WRITE;
INSERT INTO `portal_friend_link` VALUES (1,'中国作家网','https://www.chinawriter.com.cn','中国作家协会官方网站',NULL,1,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0'),(2,'起点中文网','https://www.qidian.com','阅文集团旗下网站',NULL,2,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0'),(3,'掘金','https://juejin.cn','帮助开发者成长的社区',NULL,3,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0');
UNLOCK TABLES;

-- portal_growth_rule 种子数据
LOCK TABLES `portal_growth_rule` WRITE;
INSERT INTO `portal_growth_rule` VALUES (1,'article','publish_article',50,3,'发布文章','0',1,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(2,'article','receive_like',2,0,'文章被点赞','0',2,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(3,'article','receive_bookmark',3,0,'文章被收藏','0',3,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(4,'article','receive_follow',5,0,'被关注','0',4,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(5,'article','article_featured',100,0,'文章被精选','0',5,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(6,'article','receive_comment',2,0,'文章被评论','0',6,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(7,'reading','finish_book',20,1,'完成阅读一本书','0',10,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(8,'reading','write_quote',15,0,'发布金句','0',11,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(9,'reading','create_booklist',20,0,'创建书单','0',12,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(10,'reading','quote_liked',5,0,'金句被点赞','0',13,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(11,'reading','booklist_liked',5,0,'书单被点赞','0',14,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(12,'reading','booklist_bookmarked',10,0,'书单被收藏','0',15,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(13,'interview','solve_question',10,20,'解题','0',20,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(14,'interview','write_note',15,0,'写笔记','0',21,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(15,'interview','note_adopted',50,0,'笔记被精选','0',22,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(16,'interview','publish_experience',30,0,'发布面经','0',23,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(17,'interview','experience_liked',2,0,'面经被点赞','0',24,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(18,'interview','experience_bookmarked',3,0,'面经被收藏','0',25,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(19,'all','daily_checkin',1,1,'每日签到','0',30,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(20,'all','daily_login',1,1,'每日登录','0',31,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(21,'article','receive_tip',3,0,'文章/专栏被打赏','0',7,'','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),(22,'article','tip_others',1,3,'打赏他人','0',8,'','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),(23,'topic','create_topic',10,0,'发起话题','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(24,'topic','post_opinion',2,10,'发表观点','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(25,'topic','receive_topic_like',2,0,'话题被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(26,'topic','receive_post_like',2,0,'观点被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(27,'topic','receive_topic_comment',2,0,'话题被评论','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(28,'topic','receive_post_comment',2,0,'观点被评论','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(29,'topic','receive_comment_like',2,0,'评论被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(30,'topic','topic_featured',50,0,'话题被精选','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL);
UNLOCK TABLES;


-- portal_help_category 种子数据
LOCK TABLES `portal_help_category` WRITE;
INSERT INTO `portal_help_category` VALUES (1,'发布与编辑','BookOpen','文章发布、编辑、删除等操作指南',1,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(2,'账号与安全','HelpCircle','登录、注册、密码、安全设置',2,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(3,'互动功能','MessageSquare','评论、点赞、关注等互动功能',3,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(4,'社区规则','Shield','使用规范、违规处理、隐私政策',4,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0');
UNLOCK TABLES;

-- portal_interview_category 种子数据
LOCK TABLES `portal_interview_category` WRITE;
INSERT INTO `portal_interview_category` VALUES (1,'算法与数据结构','algorithm','算法题、数据结构相关面试题','fa-code',1,150,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),(2,'系统设计','system-design','系统架构设计、分布式系统等面试题','fa-sitemap',2,60,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),(3,'前端开发','frontend','JavaScript、CSS、Vue、React等前端技术面试题','fa-laptop-code',3,120,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),(4,'后端开发','backend','Java、Python、Go等后端技术面试题','fa-server',4,130,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),(5,'数据库','database','MySQL、Redis等数据库相关面试题','fa-database',5,80,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0');
UNLOCK TABLES;

-- portal_interview_position 种子数据
LOCK TABLES `portal_interview_position` WRITE;
INSERT INTO `portal_interview_position` VALUES (1,'java_backend','Java后端工程师','互联网','mid','[\"Java\",\"Spring\",\"SpringBoot\",\"MyBatis\",\"MySQL\",\"Redis\",\"MQ\",\"JVM\",\"并发编程\",\"分布式\",\"微服务\",\"设计模式\"]','[\"阿里\",\"腾讯\",\"字节跳动\",\"美团\",\"京东\",\"百度\",\"拼多多\",\"网易\",\"滴滴\",\"快手\"]','Java 后端工程师岗位，重点考察 Java 基础、Spring 全家桶、MySQL/Redis、分布式与微服务、JVM 与并发编程',1,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0'),(2,'frontend','前端工程师','互联网','mid','[\"JavaScript\",\"TypeScript\",\"Vue\",\"React\",\"HTML\",\"CSS\",\"Node.js\",\"Webpack\",\"Vite\",\"性能优化\",\"浏览器原理\",\"HTTP\"]','[\"阿里\",\"腾讯\",\"字节跳动\",\"美团\",\"京东\",\"百度\",\"网易\",\"小米\",\"Shopee\",\"滴滴\"]','前端工程师岗位，重点考察 JS/TS 基础、Vue/React 框架、工程化、浏览器原理、性能优化、HTTP 与网络',2,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0'),(3,'algorithm','算法工程师','互联网','mid','[\"算法\",\"数据结构\",\"动态规划\",\"图论\",\"字符串\",\"数组\",\"链表\",\"树\",\"递归\",\"排序\",\"机器学习\",\"深度学习\",\"数学\"]','[\"阿里\",\"腾讯\",\"字节跳动\",\"百度\",\"美团\",\"快手\",\"小红书\",\"华为\",\"商汤\",\"旷视\"]','算法工程师岗位，重点考察数据结构与算法、动态规划、图论、字符串算法、机器学习与深度学习基础',3,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0');
UNLOCK TABLES;


-- portal_task 种子数据
LOCK TABLES `portal_task` WRITE;
INSERT INTO `portal_task` VALUES (1,'daily_checkin','每日签到','每天签到一次，保持活跃','daily',10,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(2,'daily_publish','每日发文','每日发布 1 篇文章','daily',20,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(3,'daily_comment','每日互动','每日评论 3 次','daily',15,3,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(4,'daily_like','每日点赞','每日点赞 5 次','daily',10,5,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(5,'daily_solve','每日刷题','每日解答 3 道面试题','daily',20,3,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(6,'first_article','初露锋芒','发布第一篇文章','achievement',50,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(7,'solve_50','刷题能手','累计解答 50 道面试题','achievement',200,50,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
UNLOCK TABLES;



-- =====================================================================
-- 五、菜单权限数据段（sys_menu + sys_role_menu）
-- =====================================================================

-- ============================================================--
-- 5.1 RuoYi 框架菜单（来源: 90_菜单权限_RuoYi.sql）
-- ============================================================--
-- 来源：all-db-ddl.sql 行4453-4578（已删除行4577 SHOW OPEN TABLES 诊断语句）
-- 用途：sys_menu 第一段——RuoYi 框架自带菜单 INSERT 种子数据

LOCK TABLES `sys_menu` WRITE;
-- 初始化-菜单信息表数据
-- 一级菜单
INSERT INTO sys_menu VALUES('1', '系统管理', '0', '1', 'system',           NULL, '', '', 1, 0, 'M', '0', '0', '', 'system',   'admin', NOW(), '', NULL, '系统管理目录', '0');
INSERT INTO sys_menu VALUES('2', '系统监控', '0', '2', 'monitor',          NULL, '', '', 1, 0, 'M', '0', '0', '', 'monitor',  'admin', NOW(), '', NULL, '系统监控目录', '0');
INSERT INTO sys_menu VALUES('3', '系统工具', '0', '3', 'tool',             NULL, '', '', 1, 0, 'M', '0', '0', '', 'tool',     'admin', NOW(), '', NULL, '系统工具目录', '0');
-- 二级菜单
INSERT INTO sys_menu VALUES('100',  '用户管理', '1',   '1', 'user',       'system/user/index',        '', '', 1, 0, 'C', '0', '0', 'system:user:list',        'user',          'admin', NOW(), '', NULL, '用户管理菜单', '0');
INSERT INTO sys_menu VALUES('101',  '角色管理', '1',   '2', 'role',       'system/role/index',        '', '', 1, 0, 'C', '0', '0', 'system:role:list',        'peoples',       'admin', NOW(), '', NULL, '角色管理菜单', '0');
INSERT INTO sys_menu VALUES('102',  '菜单管理', '1',   '3', 'menu',       'system/menu/index',        '', '', 1, 0, 'C', '0', '0', 'system:menu:list',        'tree-table',    'admin', NOW(), '', NULL, '菜单管理菜单', '0');
INSERT INTO sys_menu VALUES('103',  '部门管理', '1',   '4', 'dept',       'system/dept/index',        '', '', 1, 0, 'C', '0', '0', 'system:dept:list',        'tree',          'admin', NOW(), '', NULL, '部门管理菜单', '0');
INSERT INTO sys_menu VALUES('104',  '岗位管理', '1',   '5', 'post',       'system/post/index',        '', '', 1, 0, 'C', '0', '0', 'system:post:list',        'post',          'admin', NOW(), '', NULL, '岗位管理菜单', '0');
INSERT INTO sys_menu VALUES('105',  '字典管理', '1',   '6', 'dict',       'system/dict/index',        '', '', 1, 0, 'C', '0', '0', 'system:dict:list',        'dict',          'admin', NOW(), '', NULL, '字典管理菜单', '0');
INSERT INTO sys_menu VALUES('106',  '参数设置', '1',   '7', 'config',     'system/config/index',      '', '', 1, 0, 'C', '0', '0', 'system:config:list',      'edit',          'admin', NOW(), '', NULL, '参数设置菜单', '0');
INSERT INTO sys_menu VALUES('108',  '日志管理', '1',   '9', 'log',        '',                         '', '', 1, 0, 'M', '0', '0', '',                        'log',           'admin', NOW(), '', NULL, '日志管理菜单', '0');
INSERT INTO sys_menu VALUES('109',  '在线用户', '2',   '1', 'online',     'monitor/online/index',     '', '', 1, 0, 'C', '0', '0', 'monitor:online:list',     'online',        'admin', NOW(), '', NULL, '在线用户菜单', '0');
INSERT INTO sys_menu VALUES('110',  '定时任务', '2',   '2', 'job',        'monitor/job/index',        '', '', 1, 0, 'C', '0', '0', 'monitor:job:list',        'job',           'admin', NOW(), '', NULL, '定时任务菜单', '0');
INSERT INTO sys_menu VALUES('111',  '数据监控', '2',   '3', 'druid',      'monitor/druid/index',      '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list',      'druid',         'admin', NOW(), '', NULL, '数据监控菜单', '0');
INSERT INTO sys_menu VALUES('112',  '服务监控', '2',   '4', 'server',     'monitor/server/index',     '', '', 1, 0, 'C', '0', '0', 'monitor:server:list',     'server',        'admin', NOW(), '', NULL, '服务监控菜单', '0');
INSERT INTO sys_menu VALUES('113',  '缓存监控', '2',   '5', 'cache',      'monitor/cache/index',      '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',      'redis',         'admin', NOW(), '', NULL, '缓存监控菜单', '0');
INSERT INTO sys_menu VALUES('114',  '缓存列表', '2',   '6', 'cacheList',  'monitor/cache/list',       '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',      'redis-list',    'admin', NOW(), '', NULL, '缓存列表菜单', '0');
INSERT INTO sys_menu VALUES('115',  '表单构建', '3',   '1', 'build',      'tool/build/index',         '', '', 1, 0, 'C', '0', '0', 'tool:build:list',         'build',         'admin', NOW(), '', NULL, '表单构建菜单', '0');
INSERT INTO sys_menu VALUES('116',  '代码生成', '3',   '2', 'gen',        'tool/gen/index',           '', '', 1, 0, 'C', '0', '0', 'tool:gen:list',           'code',          'admin', NOW(), '', NULL, '代码生成菜单', '0');
INSERT INTO sys_menu VALUES('117',  '系统接口', '3',   '3', 'swagger',    'tool/swagger/index',       '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list',       'swagger',       'admin', NOW(), '', NULL, '系统接口菜单', '0');
-- 三级菜单
INSERT INTO sys_menu VALUES('500',  '操作日志', '108', '1', 'operlog',    'monitor/operlog/index',    '', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list',    'form',          'admin', NOW(), '', NULL, '操作日志菜单', '0');
INSERT INTO sys_menu VALUES('501',  '登录日志', '108', '2', 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor',    'admin', NOW(), '', NULL, '登录日志菜单', '0');
-- 用户管理按钮
INSERT INTO sys_menu VALUES('1000', '用户查询', '100', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query',          '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1001', '用户新增', '100', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add',            '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1002', '用户修改', '100', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit',           '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1003', '用户删除', '100', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove',         '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1004', '用户导出', '100', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export',         '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1005', '用户导入', '100', '6',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import',         '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1006', '重置密码', '100', '7',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd',       '#', 'admin', NOW(), '', NULL, '', '0');
-- 角色管理按钮
INSERT INTO sys_menu VALUES('1007', '角色查询', '101', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query',          '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1008', '角色新增', '101', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add',            '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1009', '角色修改', '101', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit',           '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1010', '角色删除', '101', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove',         '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1011', '角色导出', '101', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export',         '#', 'admin', NOW(), '', NULL, '', '0');
-- 菜单管理按钮
INSERT INTO sys_menu VALUES('1012', '菜单查询', '102', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query',          '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1013', '菜单新增', '102', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add',            '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1014', '菜单修改', '102', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit',           '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1015', '菜单删除', '102', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove',         '#', 'admin', NOW(), '', NULL, '', '0');
-- 部门管理按钮
INSERT INTO sys_menu VALUES('1016', '部门查询', '103', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query',          '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1017', '部门新增', '103', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add',            '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1018', '部门修改', '103', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit',           '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1019', '部门删除', '103', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove',         '#', 'admin', NOW(), '', NULL, '', '0');
-- 岗位管理按钮
INSERT INTO sys_menu VALUES('1020', '岗位查询', '104', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:query',          '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1021', '岗位新增', '104', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:add',            '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1022', '岗位修改', '104', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit',           '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1023', '岗位删除', '104', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove',         '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1024', '岗位导出', '104', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:export',         '#', 'admin', NOW(), '', NULL, '', '0');
-- 字典管理按钮
INSERT INTO sys_menu VALUES('1025', '字典查询', '105', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query',          '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1026', '字典新增', '105', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add',            '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1027', '字典修改', '105', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit',           '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1028', '字典删除', '105', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove',         '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1029', '字典导出', '105', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:export',         '#', 'admin', NOW(), '', NULL, '', '0');
-- 参数设置按钮
INSERT INTO sys_menu VALUES('1030', '参数查询', '106', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:query',        '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1031', '参数新增', '106', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:add',          '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1032', '参数修改', '106', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:edit',         '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1033', '参数删除', '106', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:remove',       '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1034', '参数导出', '106', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:export',       '#', 'admin', NOW(), '', NULL, '', '0');
-- 操作日志按钮
INSERT INTO sys_menu VALUES('1039', '操作查询', '500', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query',      '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1040', '操作删除', '500', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove',     '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1041', '日志导出', '500', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export',     '#', 'admin', NOW(), '', NULL, '', '0');
-- 登录日志按钮
INSERT INTO sys_menu VALUES('1042', '登录查询', '501', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query',   '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1043', '登录删除', '501', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove',  '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1044', '日志导出', '501', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export',  '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1045', '账号解锁', '501', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock',  '#', 'admin', NOW(), '', NULL, '', '0');
-- 在线用户按钮
INSERT INTO sys_menu VALUES('1046', '在线查询', '109', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query',       '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1047', '批量强退', '109', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1048', '单条强退', '109', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', NOW(), '', NULL, '', '0');
-- 定时任务按钮
INSERT INTO sys_menu VALUES('1049', '任务查询', '110', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query',          '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1050', '任务新增', '110', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add',            '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1051', '任务修改', '110', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit',           '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1052', '任务删除', '110', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove',         '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1053', '状态修改', '110', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus',   '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1054', '任务导出', '110', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export',         '#', 'admin', NOW(), '', NULL, '', '0');
-- 代码生成按钮
INSERT INTO sys_menu VALUES('1055', '生成查询', '116', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query',             '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1056', '生成修改', '116', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit',              '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1057', '生成删除', '116', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove',            '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1058', '导入代码', '116', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import',            '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1059', '预览代码', '116', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview',           '#', 'admin', NOW(), '', NULL, '', '0');
INSERT INTO sys_menu VALUES('1060', '生成代码', '116', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code',              '#', 'admin', NOW(), '', NULL, '', '0');


-- =============================================================================
-- 墨韵智库 - CMS 内容管理系统菜单完整初始化脚本（整合版）
-- =============================================================================
-- 菜单结构总览：
--   一级：内容管理 (cms)
--     ├─ 门户用户       (cms:user)            order 1
--     ├─ 文章管理       (cms:article)         order 2
--     ├─ 分类管理       (cms:category)        order 3   path=/category（绝对路径）
--     ├─ 标签管理       (cms:tag)             order 4
--     ├─ 评论管理       (cms:comment)         order 5
--     ├─ 通知管理       (cms:notification)    order 6
--     ├─ 友情链接       (cms:friend-link)     order 7
--     ├─ 帮助分类       (cms:help-category)   order 8
--     ├─ 帮助文章       (cms:help-article)    order 9
--     ├─ 举报管理       (cms:report)          order 10
--     ├─ 反馈管理       (cms:feedback)        order 11
--     ├─ 专栏管理       (portal:column)       order 12
--     └─ 打赏管理       (portal:tip)          order 13  visible=1 隐藏（已下线）
--   一级：创作者认证 (certification)
--     └─ 认证审核       (cms:certification)   order 1
--   一级：财务 (finance)  visible=1 隐藏（已下线）
--     └─ 付费订单       (portal:order)        order 1   visible=1 隐藏（已下线）
-- =============================================================================
UNLOCK TABLES;

-- ============================================================--
-- 5.2 CMS 菜单（来源: 91_菜单权限_CMS.sql）
-- ============================================================--
-- 来源：all-db-ddl.sql 行4582-4975
-- 用途：sys_menu 第二段——CMS 内容管理菜单完整初始化（INSERT 种子数据）
-- v6.1 修复：将 CMS 菜单 ID 起点设为 2000，与 RuoYi 菜单（1-1060）区分，便于维护和角色关联

ALTER TABLE sys_menu AUTO_INCREMENT = 2000;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '内容管理', 0, 10, 'cms', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'documentation', 'admin', NOW(), '内容管理目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0);
SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;

-- =============================================================================
-- 二、门户用户管理（cms:user）
--    path=portal-user，避免与"系统用户管理"菜单 path=user 冲突
--    （两者子 path 相同会导致 RuoYi 前端动态路由 route name 撞车，门户用户菜单点击无法跳转）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门户用户', @cms_parent_id, 1, 'portal-user', 'cms/user/index', NULL, 1, 0, 'C', '0', '0', 'cms:user:list', 'user', 'admin', NOW(), '门户用户管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:list');
SELECT @user_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:user:list' LIMIT 1;
-- 修复历史 path（若已存在但 path 不是 portal-user，统一修正，避免与系统用户菜单 path=user 冲突）
UPDATE sys_menu SET path = 'portal-user', update_by = 'admin', update_time = NOW()
WHERE perms = 'cms:user:list' AND path != 'portal-user';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户查询', @user_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户新增', @user_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户修改', @user_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户删除', @user_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户状态', @user_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:status', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:status');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '重置密码', @user_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:resetPwd', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:resetPwd');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '绑定系统用户', @user_menu_id, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:bind', '#', 'admin', NOW(), '身份桥接：绑定/解绑后台系统用户'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:bind');

-- =============================================================================
-- 三、文章管理（cms:article）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章管理', @cms_parent_id, 2, 'article', 'cms/article/index', NULL, 1, 0, 'C', '0', '0', 'cms:article:list', 'edit', 'admin', NOW(), '文章管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:list');
SELECT @article_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:article:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章查询', @article_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章新增', @article_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章修改', @article_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章删除', @article_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章审核', @article_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:audit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:audit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章上架', @article_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:publish', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:publish');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章推荐', @article_menu_id, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:featured', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:featured');

-- =============================================================================
-- 四、分类管理（cms:category）
--    path=/category 绝对路径，避免前端拼接父 path 后 /cms/cms/category 404（来自 49 修复）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类管理', @cms_parent_id, 3, '/category', 'cms/category/index', NULL, 1, 0, 'C', '0', '0', 'cms:category:list', 'tree', 'admin', NOW(), '分类管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:category:list');
SELECT @category_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:category:list' LIMIT 1;
-- 修复历史 path（若已存在但 path 不是 /category，统一修正）
UPDATE sys_menu SET path = '/category', update_by = 'admin', update_time = NOW()
WHERE menu_name = '分类管理' AND perms = 'cms:category:list' AND path != '/category';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类查询', @category_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:category:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类新增', @category_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:category:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类修改', @category_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:category:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类删除', @category_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:category:remove');

-- =============================================================================
-- 五、标签管理（cms:tag）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '标签管理', @cms_parent_id, 4, 'tag', 'cms/tag/index', NULL, 1, 0, 'C', '0', '0', 'cms:tag:list', 'tab', 'admin', NOW(), '标签管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:tag:list');
SELECT @tag_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:tag:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '标签查询', @tag_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:tag:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '标签新增', @tag_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:tag:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '标签修改', @tag_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:tag:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '标签删除', @tag_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:tag:remove');

-- =============================================================================
-- 六、评论管理（cms:comment）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '评论管理', @cms_parent_id, 5, 'comment', 'cms/comment/index', NULL, 1, 0, 'C', '0', '0', 'cms:comment:list', 'message', 'admin', NOW(), '评论管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:comment:list');
SELECT @comment_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:comment:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '评论查询', @comment_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:comment:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '评论审核', @comment_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:audit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:comment:audit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '评论删除', @comment_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:comment:remove');

-- v6.2 通知管理已移出 CMS，归属"系统管理"目录，权限码 system:notification:*，见 93_菜单权限_消息中心.sql
-- 原 cms:notification:* 菜单不再在此创建，避免与系统管理菜单重复

-- =============================================================================
-- 八、友情链接（cms:friend-link）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '友情链接', @cms_parent_id, 7, 'friend-link', 'cms/friend-link/index', NULL, 1, 0, 'C', '0', '0', 'cms:friend-link:list', 'link', 'admin', NOW(), '友情链接管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:friend-link:list');
SELECT @friend_link_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:friend-link:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '友情链接查询', @friend_link_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:friend-link:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '友情链接新增', @friend_link_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:friend-link:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '友情链接修改', @friend_link_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:friend-link:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '友情链接删除', @friend_link_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:friend-link:remove');

-- =============================================================================
-- 九、帮助分类（cms:help-category）  来自 38
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '帮助分类', @cms_parent_id, 8, 'help-category', 'cms/help-category/index', NULL, 1, 0, 'C', '0', '0', 'cms:help-category:list', 'tree', 'admin', NOW(), '帮助中心分类管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:list');
SELECT @help_category_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:help-category:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类查询', @help_category_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类新增', @help_category_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类修改', @help_category_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类删除', @help_category_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:remove');

-- =============================================================================
-- 十、帮助文章（cms:help-article）  来自 38
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '帮助文章', @cms_parent_id, 9, 'help-article', 'cms/help-article/index', NULL, 1, 0, 'C', '0', '0', 'cms:help-article:list', 'documentation', 'admin', NOW(), '帮助中心文章管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:list');
SELECT @help_article_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:help-article:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章查询', @help_article_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章新增', @help_article_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章修改', @help_article_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章删除', @help_article_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:remove');

-- =============================================================================
-- 十一、举报管理（cms:report）  来自 38
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '举报管理', @cms_parent_id, 10, 'report', 'cms/report/index', NULL, 1, 0, 'C', '0', '0', 'cms:report:list', 'warning', 'admin', NOW(), '用户举报记录管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:list');
SELECT @report_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:report:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '举报查询', @report_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:report:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '处理举报', @report_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:report:handle', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:handle');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除举报', @report_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:report:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:remove');

-- =============================================================================
-- 十二、反馈管理（cms:feedback）  来自 38
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '反馈管理', @cms_parent_id, 11, 'feedback', 'cms/feedback/index', NULL, 1, 0, 'C', '0', '0', 'cms:feedback:list', 'message', 'admin', NOW(), '用户意见反馈管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:list');
SELECT @feedback_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:feedback:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '反馈查询', @feedback_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:feedback:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '处理反馈', @feedback_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:feedback:handle', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:handle');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除反馈', @feedback_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:feedback:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:remove');

-- =============================================================================
-- 十三、专栏管理（portal:column）  来自 78  挂内容管理下
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏管理', @cms_parent_id, 12, 'column', 'cms/column/index', NULL, 1, 0, 'C', '0', '0', 'portal:column:list', 'documentation', 'admin', NOW(), '专栏后台管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:list');
SELECT @column_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:column:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏查询', @column_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏新增', @column_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏修改', @column_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏删除', @column_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:remove');

-- =============================================================================
-- 十四、打赏管理（portal:tip）  来自 79  已下线 visible=1 隐藏
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '打赏管理', @cms_parent_id, 13, 'tip', 'cms/tip/index', NULL, 1, 0, 'C', '1', '0', 'portal:tip:list', 'money', 'admin', NOW(), '【已下线】前台打赏功能移除，菜单隐藏保留以兼容历史数据'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:tip:list');
SELECT @tip_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:tip:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '打赏查询', @tip_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '1', '0', 'portal:tip:query', '#', 'admin', NOW(), '【已下线】'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:tip:query');

-- =============================================================================
-- 十五、独立一级目录：创作者认证（certification）  来自 63
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '创作者认证', 0, 14, 'certification', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'user', 'admin', NOW(), '创作者认证目录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '创作者认证' AND parent_id = 0);
SELECT @cert_menu_id := menu_id FROM sys_menu WHERE menu_name = '创作者认证' AND parent_id = 0 LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '认证审核', @cert_menu_id, 1, 'audit', 'cms/certification/index', NULL, 1, 0, 'C', '0', '0', 'cms:certification:audit', 'edit', 'admin', NOW(), '创作者认证审核菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:certification:audit');
SELECT @cert_audit_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:certification:audit' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '认证查询', @cert_audit_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:certification:list', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:certification:list');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '认证审核', @cert_audit_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:certification:audit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:certification:audit' AND menu_type = 'F');

-- =============================================================================
-- 十六、独立一级目录：财务（finance）  来自 80  已下线 visible=1 隐藏
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '财务', 0, 20, 'finance', NULL, NULL, 1, 0, 'M', '1', '0', NULL, 'money', 'admin', NOW(), '【已下线】财务目录，前台消费记录入口已移除'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '财务' AND parent_id = 0);
SELECT @finance_menu_id := menu_id FROM sys_menu WHERE menu_name = '财务' AND parent_id = 0 LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '付费订单', @finance_menu_id, 1, 'order', 'cms/order/index', NULL, 1, 0, 'C', '1', '0', 'portal:order:list', 'shopping', 'admin', NOW(), '【已下线】前台消费记录入口移除，菜单隐藏保留以兼容历史数据'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:order:list');
SELECT @order_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:order:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订单查询', @order_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '1', '0', 'portal:order:query', '#', 'admin', NOW(), '【已下线】'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:order:query');

-- =============================================================================

-- ============================================================--
-- 5.3 角色菜单关联（来源: 92_角色菜单关联.sql）
-- ============================================================--
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

-- ============================================================--
-- 5.4 消息中心菜单（来源: 93_菜单权限_消息中心.sql）
-- ============================================================--
-- =============================================================================
-- 消息中心 + 通知管理 菜单与权限初始化
-- =============================================================================
-- 用途：
--   1. 补齐 /system/message 动态路由所需菜单（修复 MessageBell 点击无反应）
--   2. 通知管理从 CMS 移出，归属"系统管理"目录，权限码 cms:notification:* → system:notification:*
--      （通知是全局系统级能力，不应归属 CMS 业务模块）
--   3. 通知收件箱权限（控制后台消息中心"我的通知"Tab 显示）
-- 归属：全部挂在"系统管理"目录（menu_name='系统管理' AND parent_id=0，RuoYi 默认 menu_id=1）
-- 关联：role_id=1（admin 超管）通过代码旁路已拥有 *:*:*，此处关联仅为后台菜单分配界面观感一致
-- 执行顺序：91 之后执行；自带 role_id=1 关联，NOT EXISTS 幂等，可重复执行
-- =============================================================================

-- 公共：系统管理目录 ID
SELECT @sys_parent_id := menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 LIMIT 1;

-- =============================================================================
-- 一、消息中心菜单（C 类型，私信+通知双 Tab，路由 /system/message）
--    与 MessageBell 的 router.push('/system/message') 一致
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '消息中心', @sys_parent_id, 20, 'message', 'system/message/index', NULL, 1, 0,
       'C', '0', '0', 'system:message:list', 'message', 'admin', NOW(), '消息中心菜单（私信+通知双Tab）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:list' AND menu_type = 'C');
SELECT @msg_center_menu_id := menu_id FROM sys_menu WHERE perms = 'system:message:list' AND menu_type = 'C' LIMIT 1;

-- 消息中心-私信功能权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '私信查询', @msg_center_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '私信发送', @msg_center_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:send', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:send');

-- 消息中心-通知收件箱权限（控制"我的通知"Tab 显示）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知查询', @msg_center_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:list', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:list');

-- =============================================================================
-- 二、通知管理菜单（C 类型，台账：管理全部通知记录，路由 /system/notification）
--    原 cms/notification 已迁移至此，归属系统管理，权限码 system:notification:*
--    前端页面：system/notification/index（原 cms/notification/index）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知管理', @sys_parent_id, 21, 'notification', 'system/notification/index', NULL, 1, 0,
       'C', '0', '0', 'system:notification:list', 'email', 'admin', NOW(), '通知管理菜单（台账）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:list' AND menu_type = 'C' AND component = 'system/notification/index');
SELECT @notification_admin_menu_id := menu_id FROM sys_menu WHERE perms = 'system:notification:list' AND menu_type = 'C' AND component = 'system/notification/index' LIMIT 1;

-- 通知管理-台账功能权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知查询', @notification_admin_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知新增', @notification_admin_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知修改', @notification_admin_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知删除', @notification_admin_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:remove');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '发送广播通知', @notification_admin_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:sendAll', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:sendAll');

-- =============================================================================
-- 三、role_id=1（admin 超管）关联消息中心+通知管理全部菜单（精确匹配 perms，NOT EXISTS 防重复）
--    说明：admin 走代码旁路（*:*:*）实际不依赖此关联，仅保证后台"角色菜单分配"界面观感一致
-- =============================================================================
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, menu_id, 'admin', NOW()
FROM sys_menu
WHERE perms IN (
    'system:message:list', 'system:message:query', 'system:message:send',
    'system:notification:list', 'system:notification:query', 'system:notification:add',
    'system:notification:edit', 'system:notification:remove', 'system:notification:sendAll'
  )
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
  );

-- =============================================================================
-- 四、role_id=2（普通角色）关联消息中心+通知管理菜单（只读：list + query，不含 send/add/edit/remove）
-- =============================================================================
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 2, menu_id, 'admin', NOW()
FROM sys_menu
WHERE perms IN (
    'system:message:list', 'system:message:query',
    'system:notification:list', 'system:notification:query'
  )
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 2 AND rm.menu_id = sys_menu.menu_id
  );

SELECT '消息中心 + 通知管理菜单初始化完成！' AS message;

-- ============================================================--
-- 5.5 v7.7~v7.24 菜单注册与重构（来源: 100-108/109/114/116 升级脚本）
-- 说明：这些升级脚本中包含菜单 INSERT/UPDATE，幂等可重复执行
-- ============================================================--

-- ---------------------------------------------------------------
-- 来源: 100_升级脚本_v7.7_菜单合并.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.7 升级脚本：菜单合并（第一批 - Tab 容器页）
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（使用 information_schema / sys_menu 存在性判断）
-- 幂等性核查（P2-1）：✅ 已幂等 — INSERT 用 IF(COUNT(perms)=0, CONCAT动态SQL, SELECT) + PREPARE/EXECUTE 守护；
--   @var 传递 parent_id 到 CONCAT 值位，IF 条件基于 COUNT 查询不依赖 @var，@var 丢失时 IF 仍正确跳过已存在菜单。
-- 背景：
--   将同构、同业务域的二级菜单合并为 Tab 容器页，减少菜单数量。
--   合并后菜单 component 指向新的容器页（如 cms/promotion/index），
--   旧菜单（如 cms/ad/index、cms/friend-link/index）设为隐藏（visible=1），
--   保留旧菜单与权限项以兼容历史角色分配，支持回滚。
--   前端容器页通过懒加载方式复用原页面组件，各 Tab 保留独立权限控制。
--
--   合并清单（8 项）：
--     1. 推广位管理  ← 广告位 + 友情链接
--     2. 用户反馈处理 ← 反馈管理 + 举报管理
--     3. 缓存管理    ← 缓存监控 + 缓存列表
--     4. 日志审计    ← 操作日志 + 登录日志
--     5. 学习辅助    ← 学习计划 + 错题本
--     6. 成长配置    ← 成长规则 + 成就管理
--     7. 交易管理    ← 付费订单 + 打赏管理
--     8. 帮助中心    ← 帮助分类 + 帮助文章
-- ====================================================================

SET @db := DATABASE();

-- ====================================================================
-- 工具函数：幂等插入菜单（基于 perms 去重）
-- 调用：SELECT register_menu(parentId, orderNum, path, component, name, perms, icon, remark);
-- ====================================================================

-- ====================================================================
-- 1. 推广位管理（广告位 + 友情链接）
-- ====================================================================
SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;
SET @cms_parent_id := IFNULL(@cms_parent_id, 0);

-- 1.1 注册合并菜单（幂等）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:promotion:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''推广位管理'', ', @cms_parent_id, ', 20, ''promotion'', ''cms/promotion/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''cms:promotion:list'', ''component'', ''admin'', NOW(), ''广告位与友情链接合并管理（Tab）'')'),
  'SELECT ''cms:promotion:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 隐藏旧菜单（广告位、友情链接）- visible=1 隐藏，status=0 正常，保留权限
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('portal:ad:list', 'cms:friend-link:list') AND visible = '0';

-- ====================================================================
-- 2. 用户反馈处理（反馈管理 + 举报管理）
-- ====================================================================
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:feedback-center:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''用户反馈处理'', ', @cms_parent_id, ', 21, ''feedback-center'', ''cms/feedback-center/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''cms:feedback-center:list'', ''message'', ''admin'', NOW(), ''反馈与举报合并处理（Tab）'')'),
  'SELECT ''cms:feedback-center:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 隐藏旧菜单
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('cms:feedback:list', 'cms:report:list') AND visible = '0';

-- ====================================================================
-- 3. 帮助中心（帮助分类 + 帮助文章）
-- ====================================================================
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:help-center:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''帮助中心'', ', @cms_parent_id, ', 22, ''help-center'', ''cms/help-center/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''cms:help-center:list'', ''question'', ''admin'', NOW(), ''帮助分类与文章合并管理（Tab）'')'),
  'SELECT ''cms:help-center:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.2 隐藏旧菜单
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('cms:help-category:list', 'cms:help-article:list') AND visible = '0';

-- ====================================================================
-- 4. 成长配置（成长规则 + 成就管理）
-- 注：成长体系菜单若未注册（如运行时手动添加），此合并菜单仍可独立注册；
--     旧菜单隐藏采用 perms 模糊匹配，无匹配则跳过（幂等）。
-- ====================================================================
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:growth-config:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''成长配置'', ', @cms_parent_id, ', 23, ''growth-config'', ''cms/growth-config/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''cms:growth-config:list'', ''star'', ''admin'', NOW(), ''成长规则与成就合并配置（Tab）'')'),
  'SELECT ''cms:growth-config:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.2 隐藏旧菜单（成长规则、成就管理 - perms 可能是 cms:growth:rule:list / cms:growth:achievement:list）
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('cms:growth:rule:list', 'cms:growth:achievement:list',
                  'cms:growth-rule:list', 'cms:growth-achievement:list')
  AND visible = '0';

-- ====================================================================
-- 5. 交易管理（付费订单 + 打赏管理）
-- 注：原菜单已 visible=1 隐藏（已下线），此处注册合并菜单并保持隐藏状态，
--     便于未来如恢复交易功能时统一入口。
-- ====================================================================
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:transaction:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''交易管理'', ', @cms_parent_id, ', 24, ''transaction'', ''cms/transaction/index'', NULL, 1, 0, ''C'', ''1'', ''0'', ''cms:transaction:list'', ''money'', ''admin'', NOW(), ''订单与打赏合并查询（Tab，当前隐藏）'')'),
  'SELECT ''cms:transaction:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5.2 旧菜单（portal:tip:list / portal:order:list）已是 visible=1，无需重复更新

-- ====================================================================
-- 6. 缓存管理（缓存监控 + 缓存列表）- 系统监控目录
-- ====================================================================
SELECT @monitor_parent_id := menu_id FROM sys_menu WHERE menu_name = '系统监控' AND parent_id = 0 LIMIT 1;
SET @monitor_parent_id := IFNULL(@monitor_parent_id, 0);

-- 6.1 注册合并菜单（幂等）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'monitor:cache-manage:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''缓存管理'', ', @monitor_parent_id, ', 7, ''cache-manage'', ''monitor/cache-manage/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''monitor:cache-manage:list'', ''redis'', ''admin'', NOW(), ''缓存监控与列表合并管理（Tab）'')'),
  'SELECT ''monitor:cache-manage:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6.2 隐藏旧菜单（缓存监控 113、缓存列表 114）
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE menu_id IN (113, 114) AND visible = '0';

-- ====================================================================
-- 7. 日志审计（操作日志 + 登录日志）- 系统管理 > 日志管理目录
-- ====================================================================
SELECT @log_parent_id := menu_id FROM sys_menu WHERE menu_name = '日志管理' AND parent_id != 0 LIMIT 1;
SET @log_parent_id := IFNULL(@log_parent_id, 108);

-- 7.1 注册合并菜单（幂等）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'monitor:log-audit:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''日志审计'', ', @log_parent_id, ', 3, ''log-audit'', ''monitor/log-audit/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''monitor:log-audit:list'', ''log'', ''admin'', NOW(), ''操作日志与登录日志合并查询（Tab）'')'),
  'SELECT ''monitor:log-audit:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7.2 隐藏旧菜单（操作日志 500、登录日志 501）
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE menu_id IN (500, 501) AND visible = '0';

-- ====================================================================
-- 8. 学习辅助（学习计划 + 错题本）- Portal 读书目录
-- 注：portal 读书模块菜单可能未注册，此合并菜单独立注册；
--     若旧菜单存在则隐藏，无匹配则跳过（幂等）。
-- ====================================================================
SELECT @portal_parent_id := menu_id FROM sys_menu WHERE menu_name IN ('读书门户', '门户读书', '读书管理', '读书') AND parent_id = 0 LIMIT 1;
SET @portal_parent_id := IFNULL(@portal_parent_id, 0);

-- 8.1 注册合并菜单（幂等）
--     若 portal 顶级目录不存在，则挂到内容管理目录下作为兜底
SET @final_portal_parent := IF(@portal_parent_id = 0, @cms_parent_id, @portal_parent_id);
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'portal:learn-aux:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''学习辅助'', ', @final_portal_parent, ', 30, ''learn-aux'', ''portal/learn-aux/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''portal:learn-aux:list'', ''education'', ''admin'', NOW(), ''学习计划与错题本合并查看（Tab）'')'),
  'SELECT ''portal:learn-aux:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 8.2 隐藏旧菜单（学习计划、错题本 - perms 可能未注册，幂等跳过）
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('portal:studyPlan:list', 'portal:wrongQuestion:list')
  AND visible = '0';

-- ====================================================================
-- 9. 角色权限分配提示
-- ====================================================================
-- 新合并菜单的权限项（cms:promotion:list / cms:feedback-center:list / ...）
-- 需要在角色管理中分配给对应角色。admin 超级管理员通过 role_key='admin'
-- 通配权限自动放行，无需额外分配。
-- 旧菜单虽隐藏，但其按钮权限（如 cms:friend-link:add）仍保留，
-- 容器页内各 Tab 通过 v-hasPermi 复用原权限控制。

-- ====================================================================
-- 升级完成 - 合并后菜单结构
-- ====================================================================
-- 内容管理 (cms)
--   ├─ ... 原有保留菜单 ...
--   ├─ 推广位管理 (cms:promotion:list)         ← 广告位 + 友情链接（隐藏旧）
--   ├─ 用户反馈处理 (cms:feedback-center:list) ← 反馈 + 举报（隐藏旧）
--   ├─ 帮助中心 (cms:help-center:list)         ← 帮助分类 + 文章（隐藏旧）
--   ├─ 成长配置 (cms:growth-config:list)      ← 成长规则 + 成就（隐藏旧）
--   └─ 交易管理 (cms:transaction:list,隐藏)    ← 订单 + 打赏（原已隐藏）
--
-- 系统监控 (monitor)
--   └─ 缓存管理 (monitor:cache-manage:list)    ← 缓存监控 + 列表（隐藏旧）
--
-- 系统管理 > 日志管理
--   └─ 日志审计 (monitor:log-audit:list)       ← 操作 + 登录日志（隐藏旧）
--
-- 读书门户/内容管理
--   └─ 学习辅助 (portal:learn-aux:list)        ← 学习计划 + 错题本（隐藏旧）
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 101_升级脚本_v7.8_面经评论审核字段.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.8 升级脚本：面经评论审核字段补齐 + 存量数据迁移
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（使用 information_schema 判断列是否存在）
-- 幂等性核查（P2-1）：✅ 已幂等 — DDL 用 information_schema+IF+PREPARE 守护；UPDATE pending→published 用 WHERE status='pending' 守护，重复执行 0 行受影响。
-- 背景：
--   1. portal_interview_comment 表仅有 status 字段，缺审核人/审核时间/审核意见，
--      无法追溯审核轨迹，与 portal_comment（v6.8 已补齐）不一致。
--   2. 本脚本：
--      a) 为 portal_interview_comment 增加 auditor_id / audit_remark / audit_time 字段，
--         对齐 portal_comment 审核字段设计；
--      b) 增加审核人索引；
--      c) 存量 pending 状态数据迁移为 published（评论类不审核策略，仅定时扫描兜底）。
--   3. 业务层（PortalInterviewServiceImpl.auditComment）已同步升级为：
--      状态白名单校验、乐观锁、审核人/时间/意见写入、事务化。
--   4. 发布侧（PortalInterviewServiceImpl.insertComment）已接入敏感词检查。
-- ====================================================================

SET @db := DATABASE();

-- 1.1 portal_interview_comment.auditor_id
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_comment ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID，CMS审核或定时扫描命中时写入）'' AFTER status',
  'SELECT ''portal_interview_comment.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 portal_interview_comment.audit_remark
SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_comment ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因'' AFTER auditor_id',
  'SELECT ''portal_interview_comment.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 portal_interview_comment.audit_time
SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_comment ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_interview_comment.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 审核人索引（CMS 审核员维度查询）
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_comment' AND INDEX_NAME = 'idx_auditor_id') = 0,
  'ALTER TABLE portal_interview_comment ADD INDEX idx_auditor_id (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ====================================================================
-- 2. 存量数据迁移：pending → published
-- 背景：评论类统一为"不审核 + 定时扫描兜底"策略，
--       历史遗留的 pending 状态评论需迁移为 published，避免被定时扫描误判或滞留待审。
-- 注意：deleted 状态保持不变（已删除），仅迁移 pending。
-- ====================================================================
UPDATE portal_interview_comment SET status = 'published', update_time = NOW()
  WHERE status = 'pending';

-- ====================================================================
-- 升级完成
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 102_升级脚本_v7.9_评论扫描定时任务.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.9 升级脚本：评论类敏感词定时扫描任务注册
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（WHERE NOT EXISTS 防重）
-- 幂等性核查（P2-1）：✅ 已幂等 — INSERT 用 WHERE NOT EXISTS(invoke_target) 守护，无 @var 使用。
-- 背景：
--   1. 评论类统一为"不审核 + 定时扫描兜底"策略（见 v6.7 话题/观点扫描范式）。
--   2. SensitiveScanTask 已新增 scanArticleComments / scanInterviewComments 两个方法
--      （分别对应 portal_comment 与 portal_interview_comment 表）。
--   3. 本脚本将这两个方法注册为 sys_job 定时任务，cron 错峰避开既有扫描任务：
--      - scanTopics()             0 0  3 * * ?  （v6.7 已注册，每天 03:00）
--      - scanTopicPosts()         0 5  3 * * ?  （v6.7 已注册，每天 03:05）
--      - scanArticleComments()    0 10 3 * * ?  （本脚本新增，每天 03:10）
--      - scanInterviewComments() 0 15 3 * * ?  （本脚本新增，每天 03:15）
--   4. 任务默认启用（status='0'），misfire_policy='3'（放弃补偿，避免堆积），
--      concurrent='1'（禁止并发，防止上一批未扫完就启动下一批）。
-- ====================================================================

-- 1. 文章评论敏感词扫描
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '敏感词扫描-文章评论', 'DEFAULT', 'sensitiveScanTask.scanArticleComments()', '0 10 3 * * ?', '3', '1', '0', 'admin', NOW(),
       '扫描已发布文章评论(portal_comment.status=1)，命中敏感词转驳回(status=2)并通知作者'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'sensitiveScanTask.scanArticleComments()');

-- 2. 面经评论敏感词扫描
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '敏感词扫描-面经评论', 'DEFAULT', 'sensitiveScanTask.scanInterviewComments()', '0 15 3 * * ?', '3', '1', '0', 'admin', NOW(),
       '扫描已发布面经评论(portal_interview_comment.status=published)，命中敏感词转rejected并通知作者'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'sensitiveScanTask.scanInterviewComments()');

-- ====================================================================
-- 升级完成
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 103_升级脚本_v7.10_内容审核中心菜单.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.10 升级脚本：内容审核中心菜单注册（文章+专栏+话题审核 Tab 合并）
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（基于 perms 去重）
-- 幂等性核查（P2-1）：✅ 已幂等 — INSERT 用 IF(COUNT(perms)=0, CONCAT动态SQL, SELECT) + PREPARE/EXECUTE 守护，符合 112 脚本范式。
-- 背景：
--   将分散在 文章/专栏/话题 各自 index.vue 的「审核」入口整合为统一「内容审核中心」菜单，
--   通过 Tab 容器（cms/audit-center/index）聚合三个审核页，嵌入模式(:embedded)隐藏各自返回头。
--   审核闭环不变：提交→待审→通过/驳回→通知→操作日志保留（各 audit.vue 逻辑保持原样）。
--   原 /cms/article/audit 等静态路由与各 index.vue 的审核按钮保留，作为单条快捷入口兼容历史。
-- ====================================================================

SET @db := DATABASE();

SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;
SET @cms_parent_id := IFNULL(@cms_parent_id, 0);

-- 注册「内容审核中心」合并菜单（幂等）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:audit-center:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''内容审核中心'', ', @cms_parent_id, ', 25, ''audit-center'', ''cms/audit-center/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''cms:audit-center:list'', ''check'', ''admin'', NOW(), ''文章/专栏/话题审核合并入口（Tab，嵌入模式）'')'),
  'SELECT ''cms:audit-center:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 角色权限分配提示：
--   admin 超级管理员通过 role_key='admin' 通配权限自动放行，无需额外分配。
--   内容审核员等角色需在「角色管理」中分配 cms:audit-center:list 权限。
--   各审核 Tab 内的操作权限（cms:article:audit / cms:column:audit / cms:topic:audit）仍沿用各模块原有权限项。

-- ====================================================================
-- 升级完成
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 104_升级脚本_v7.11_监控接口菜单整合.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.11 升级脚本：监控与接口菜单整合
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行
-- 幂等性核查（P2-1）：✅ 已幂等 — UPDATE 用 WHERE menu_name<>'接口文档'/visible='0' 守护；INSERT 用 IF(COUNT(perms)=0,...)+PREPARE/EXECUTE 守护。
-- 内容：
--   1. swagger 菜单改名：系统接口 → 接口文档（P3-5）
--   2. 服务监控合并：服务器监控 + 数据监控(Druid) → 「服务监控」Tab 容器（P3-6）
-- ====================================================================

SET @db := DATABASE();

-- ====================================================================
-- 1. swagger 菜单改名：系统接口 → 接口文档
-- 背景：原菜单名「系统接口」语义模糊，统一为「接口文档」更直观。
-- 按 perms 定位（幂等：已改名则跳过）。
-- ====================================================================
UPDATE sys_menu SET menu_name = '接口文档', update_by = 'admin', update_time = NOW()
  WHERE perms = 'tool:swagger:list' AND menu_name <> '接口文档';

-- ====================================================================
-- 2. 服务监控合并（服务器监控 + 数据监控 Druid）
-- 背景：原「系统监控」目录下「数据监控」(111, monitor:druid:list) 与
--      「服务监控」(112, monitor:server:list) 两个二级菜单高度相关，
--      合并为「服务监控」Tab 容器（monitor/server-panel/index），减少菜单数量。
-- 旧菜单设为隐藏（visible=1），保留权限项以兼容历史角色分配，支持回滚。
-- ====================================================================
SELECT @monitor_parent_id := menu_id FROM sys_menu WHERE menu_name = '系统监控' AND parent_id = 0 LIMIT 1;
SET @monitor_parent_id := IFNULL(@monitor_parent_id, 2);

-- 2.1 注册合并菜单（幂等，基于 perms 去重）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'monitor:server-panel:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''服务监控'', ', @monitor_parent_id, ', 3, ''server-panel'', ''monitor/server-panel/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''monitor:server-panel:list'', ''monitor'', ''admin'', NOW(), ''服务器监控与数据监控(Druid)合并查看（Tab）'')'),
  'SELECT ''monitor:server-panel:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 隐藏旧菜单（数据监控 111、服务监控 112）- visible=1 隐藏，status=0 正常，保留权限
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('monitor:druid:list', 'monitor:server:list') AND visible = '0';

-- ====================================================================
-- 角色权限分配提示：
--   admin 超级管理员通过 role_key='admin' 通配权限自动放行，无需额外分配。
--   非 admin 角色需分配 monitor:server-panel:list 权限；容器内各 Tab 的
--   操作权限（monitor:server:* / monitor:druid:*）仍沿用各页原有权限项。
-- ====================================================================

-- ====================================================================
-- 升级完成
-- ====================================================================
-- 系统监控 (monitor, id=2)
--   ├─ 在线用户 (109)
--   ├─ 定时任务 (110)
--   ├─ 服务监控 (monitor:server-panel:list)  ← 服务器 + Druid（隐藏旧 111/112）
--   └─ 缓存管理 (monitor:cache-manage:list)  ← 缓存监控 + 列表（v7.7 已合并，隐藏旧 113/114）
--
-- 系统工具 (tool)
--   └─ 接口文档 (tool:swagger:list)          ← 原「系统接口」改名
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 105_升级脚本_v7.12_清理Flowable.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.12 升级脚本：清理 Flowable 工作流（运行库）
-- 适配 MySQL 8.x
-- 说明：本脚本幂等（DROP TABLE IF EXISTS / DELETE 无副作用）
-- 幂等性核查（P2-1）：✅ 已幂等 — DROP TABLE IF EXISTS 天然幂等；DELETE 重复执行 0 行受影响不报错。无 @var 使用。
-- 背景：Flowable 工作流模块已下线（前端/后端代码、act_* 建表脚本、菜单种子均已移除）。
--       本脚本清理运行库中的残留：
--         - 39 张 act_* 引擎表
--         - 5 张 sys_* 流程辅助表（sys_deploy_form/sys_expression/sys_form/sys_listener/sys_task_form）
--         - 5 类 Flowable 相关字典（exp_data_type/sys_listener_type/sys_listener_value_type/sys_listener_event_type/sys_process_category）
--         - 3 条 sys_menu（4/118/119）+ 角色菜单关联
-- 执行前请确认无在用的流程实例；act_* 表为 Flowable 引擎独占，与业务表无外键关联。
-- ====================================================================

-- 1. 删除 Flowable 引擎表（39 张，按依赖无关顺序，IF EXISTS 幂等）
DROP TABLE IF EXISTS act_evt_log;
DROP TABLE IF EXISTS act_ge_bytearray;
DROP TABLE IF EXISTS act_ge_property;
DROP TABLE IF EXISTS act_hi_actinst;
DROP TABLE IF EXISTS act_hi_attachment;
DROP TABLE IF EXISTS act_hi_comment;
DROP TABLE IF EXISTS act_hi_detail;
DROP TABLE IF EXISTS act_hi_entitylink;
DROP TABLE IF EXISTS act_hi_identitylink;
DROP TABLE IF EXISTS act_hi_procinst;
DROP TABLE IF EXISTS act_hi_taskinst;
DROP TABLE IF EXISTS act_hi_tsk_log;
DROP TABLE IF EXISTS act_hi_varinst;
DROP TABLE IF EXISTS act_id_bytearray;
DROP TABLE IF EXISTS act_id_group;
DROP TABLE IF EXISTS act_id_info;
DROP TABLE IF EXISTS act_id_membership;
DROP TABLE IF EXISTS act_id_priv;
DROP TABLE IF EXISTS act_id_priv_mapping;
DROP TABLE IF EXISTS act_id_property;
DROP TABLE IF EXISTS act_id_token;
DROP TABLE IF EXISTS act_id_user;
DROP TABLE IF EXISTS act_procdef_info;
DROP TABLE IF EXISTS act_re_deployment;
DROP TABLE IF EXISTS act_re_model;
DROP TABLE IF EXISTS act_re_procdef;
DROP TABLE IF EXISTS act_ru_actinst;
DROP TABLE IF EXISTS act_ru_deadletter_job;
DROP TABLE IF EXISTS act_ru_entitylink;
DROP TABLE IF EXISTS act_ru_event_subscr;
DROP TABLE IF EXISTS act_ru_execution;
DROP TABLE IF EXISTS act_ru_external_job;
DROP TABLE IF EXISTS act_ru_history_job;
DROP TABLE IF EXISTS act_ru_identitylink;
DROP TABLE IF EXISTS act_ru_job;
DROP TABLE IF EXISTS act_ru_suspended_job;
DROP TABLE IF EXISTS act_ru_task;
DROP TABLE IF EXISTS act_ru_timer_job;
DROP TABLE IF EXISTS act_ru_variable;

-- 2. 删除 Flowable 流程辅助表（5 张 sys_* 表，曾放在 system 包下，IF EXISTS 幂等）
DROP TABLE IF EXISTS sys_deploy_form;
DROP TABLE IF EXISTS sys_expression;
DROP TABLE IF EXISTS sys_form;
DROP TABLE IF EXISTS sys_listener;
DROP TABLE IF EXISTS sys_task_form;

-- 3. 删除 Flowable 相关字典数据
--    字典类型：exp_data_type / sys_listener_type / sys_listener_value_type / sys_listener_event_type / sys_process_category
DELETE FROM sys_dict_data WHERE dict_type IN ('exp_data_type', 'sys_listener_type', 'sys_listener_value_type', 'sys_listener_event_type', 'sys_process_category');
DELETE FROM sys_dict_type WHERE dict_type IN ('exp_data_type', 'sys_listener_type', 'sys_listener_value_type', 'sys_listener_event_type', 'sys_process_category');

-- 4. 删除 Flowable 菜单（流程管理 4 / 流程定义 118 / 流程任务 119）
DELETE FROM sys_role_menu WHERE menu_id IN (4, 118, 119);
DELETE FROM sys_menu WHERE menu_id IN (4, 118, 119);

-- ====================================================================
-- 升级完成
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 106_升级脚本_v7.13_面试指南与读书空间菜单注册.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.13 升级脚本：注册「面试指南」与「读书空间」菜单（历史遗漏补注册）
-- 适配 MySQL 8.x
-- 说明：本脚本幂等（FROM DUAL WHERE NOT EXISTS / IFNULL 兜底）
-- 幂等性核查（P2-1）：🔧 已修复 — 3 处 INSERT（面试指南/题库资源/读书空间）原用 @var 传递 parent_id，
--   @var 丢失时 NOT EXISTS 条件中 parent_id=@var 变为 NULL 比较恒假，导致防重失效产生重复；
--   已改用字面量 0 / 子查询替代 @var。其余 INSERT 均仅以 perms 做 NOT EXISTS 守护，@var 丢失不产生重复。
--
-- 背景：面试指南（cms/interview）与读书空间（portal/book*）后端 Controller、
--       前端页面均已完整实现，但 sys_menu 从未注册 → 后台管理员无法通过菜单
--       进入管理页面，属于功能可访问性阻塞问题。
--
-- 注册清单（按方案：保持两个独立一级目录，内部合并为 Tab 容器）：
--   ┌─ 面试指南（interview，一级目录，order=15）
--   │   ├─ 1.1 题库资源（Tab 容器：题目/分类/公司/简历）
--   │   ├─ 1.2 面经运营（Tab 容器：面经/评论，含审核）
--   │   └─ 1.3 精选笔记（采纳/取消）
--   │   配套按钮权限：cms:interview:query/add/edit/remove
--   │
--   └─ 读书空间（book，一级目录，order=16）
--       ├─ 2.1 书籍管理（主入口，章节管理为隐藏子路由）
--       ├─ 2.2 书单&推荐位（Tab 容器：书单/推荐位）
--       ├─ 2.3 用户内容（Tab 容器：金句摘录/书架）
--       └─ 2.4 学习辅助（Tab 容器：学习计划/错题本，已存在）
--       配套按钮权限：各子模块独立 *:query/add/edit/remove
--
-- 注：Tab 容器前端页面在后续低优任务中实现；现阶段先注册「C 菜单」
--     指向现有单模块页面，保证后台可进入。
-- ====================================================================

-- --------------------------------------------------------------
-- 0. 变量准备：获取顶级父目录
-- --------------------------------------------------------------
-- 面试指南、读书空间均为一级目录（parent_id=0）
SET @root_parent_id := 0;

-- --------------------------------------------------------------
-- 1. 面试指南（一级目录 M + 3 个 C 菜单 + 按钮权限）
-- --------------------------------------------------------------

-- 1.1 顶级目录：面试指南
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试指南', 0, 15, 'interview', '', 1, 0, 'M', '0', '0', NULL, 'guide', 'admin', NOW(), '面试指南一级目录：题库/面经/简历/笔记'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M');
-- 获取面试指南顶级目录 menu_id
SELECT @interview_mid := menu_id FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;
SET @interview_mid := IFNULL(@interview_mid, 0);

-- 1.1.1 题库资源（C 菜单：当前指向 question/index，Tab 容器实现后改 component）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库资源', (SELECT menu_id FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M' LIMIT 1), 1, 'question', 'cms/interview/question/index', 1, 0, 'C', '0', '0', 'cms:interview:list', 'tree', 'admin', NOW(), '题目/分类/公司/简历 资源管理（Tab 容器待实现）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C');
UPDATE sys_menu SET parent_id = @interview_mid, path = 'question', component = 'cms/interview/question/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:interview:list' AND parent_id IS NULL;
SELECT @interview_question_mid := menu_id FROM sys_menu WHERE perms = 'cms:interview:list' LIMIT 1;
SET @interview_question_mid := IFNULL(@interview_question_mid, @interview_mid);

-- 1.1.2 面经运营（C 菜单：指向 experience/index，含审核）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面经运营', @interview_mid, 2, 'experience', 'cms/interview/experience/index', 1, 0, 'C', '0', '0', 'cms:interview:experience:list', 'edit', 'admin', NOW(), '面经审核与评论审核（Tab 容器待实现，复用 AuditWorkbench）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:experience:list');
UPDATE sys_menu SET parent_id = @interview_mid, path = 'experience', component = 'cms/interview/experience/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:interview:experience:list' AND parent_id IS NULL;
SELECT @interview_exp_mid := menu_id FROM sys_menu WHERE perms = 'cms:interview:experience:list' LIMIT 1;
SET @interview_exp_mid := IFNULL(@interview_exp_mid, @interview_mid);

-- 1.1.3 精选笔记（C 菜单：submission）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '精选笔记', @interview_mid, 3, 'submission', 'cms/interview/submission/index', 1, 0, 'C', '0', '0', 'cms:interview:submission:list', 'star', 'admin', NOW(), '精选笔记采纳与取消'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:submission:list');
UPDATE sys_menu SET parent_id = @interview_mid, path = 'submission', component = 'cms/interview/submission/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:interview:submission:list' AND parent_id IS NULL;

-- 1.2 面试指南按钮权限（F 类型，统一挂在「题库资源」下，因该模块所有子页面共用权限码）
--     注意：原项目全模块共用 cms:interview:*，此处不拆分保持一致；
--           若后续需子模块级控制，可在此基础上新增 cms:interview:experience:audit 等。
-- 注意：先用 perms='cms:interview:list' 拿到正确的父菜单 ID
SELECT @btn_parent_id := menu_id FROM sys_menu WHERE perms = 'cms:interview:list' LIMIT 1;
SET @btn_parent_id := IFNULL(@btn_parent_id, @interview_mid);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试查询', @btn_parent_id, 1, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:query');
UPDATE sys_menu SET parent_id = @btn_parent_id WHERE perms = 'cms:interview:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试新增', @btn_parent_id, 2, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:add');
UPDATE sys_menu SET parent_id = @btn_parent_id WHERE perms = 'cms:interview:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试修改', @btn_parent_id, 3, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '含：审核/置顶/精选采纳等运营操作'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:edit');
UPDATE sys_menu SET parent_id = @btn_parent_id WHERE perms = 'cms:interview:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试删除', @btn_parent_id, 4, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:remove');
UPDATE sys_menu SET parent_id = @btn_parent_id WHERE perms = 'cms:interview:remove' AND parent_id IS NULL;

-- --------------------------------------------------------------
-- 2. 读书空间（一级目录 M + 4 个 C 菜单 + 按钮权限）
-- --------------------------------------------------------------

-- 2.1 顶级目录：读书空间
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '读书空间', 0, 16, 'book', '', 1, 0, 'M', '0', '0', NULL, 'book', 'admin', NOW(), '读书空间一级目录：书籍/章节/书单/摘录/书架'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M');
SELECT @book_mid := menu_id FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;
SET @book_mid := IFNULL(@book_mid, 0);

-- 2.1.1 书籍管理（C 菜单，主入口）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍管理', @book_mid, 1, 'book-index', 'portal/book/index', 1, 0, 'C', '0', '0', 'portal:book:list', 'documentation', 'admin', NOW(), '书籍CRUD + 章节导入向导（章节管理为隐藏子路由）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:book:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'book-index', component = 'portal/book/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:book:list' AND parent_id IS NULL;
SELECT @book_list_mid := menu_id FROM sys_menu WHERE perms = 'portal:book:list' LIMIT 1;
SET @book_list_mid := IFNULL(@book_list_mid, @book_mid);

-- 2.1.1.1 章节管理（C 菜单，隐藏 visible=1，作为隐藏子路由，activeMenu 指向书籍管理）
--           注意：前端 router 已静态注册隐藏路由，此处同步注册 sys_menu 供动态路由解析
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节管理', @book_mid, 2, 'bookChapter', 'portal/bookChapter/index', 1, 0, 'C', '1', '0', 'portal:bookChapter:list', '#', 'admin', NOW(), '书籍章节CRUD + 发布/批量导入（隐藏菜单，从书籍详情跳转）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'bookChapter', component = 'portal/bookChapter/index', visible = '1', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:bookChapter:list' AND parent_id IS NULL;

-- 2.1.2 书单&推荐位（C 菜单：当前指向 bookList/index，Tab 容器待实现）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单&推荐位', @book_mid, 3, 'bookList', 'portal/bookList/index', 1, 0, 'C', '0', '0', 'portal:bookList:list', 'list', 'admin', NOW(), '书单管理 + 推荐位（Tab 容器待实现）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'bookList', component = 'portal/bookList/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:bookList:list' AND parent_id IS NULL;
SELECT @booklist_mid := menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' LIMIT 1;
SET @booklist_mid := IFNULL(@booklist_mid, @book_mid);

-- 2.1.2.1 推荐位管理（C 菜单，隐藏 visible=1，Tab 容器实现后从书单&推荐位进入）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位管理', @book_mid, 4, 'bookRecommend', 'portal/bookRecommend/index', 1, 0, 'C', '1', '0', 'portal:bookRecommend:list', '#', 'admin', NOW(), '书籍推荐位上下架/排序（隐藏，从书单&推荐位 Tab 进入）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookRecommend:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'bookRecommend', component = 'portal/bookRecommend/index', visible = '1', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:bookRecommend:list' AND parent_id IS NULL;

-- 2.1.3 用户内容（C 菜单：当前指向 bookQuote/index，Tab 容器待实现）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户内容', @book_mid, 5, 'bookQuote', 'portal/bookQuote/index', 1, 0, 'C', '0', '0', 'portal:bookQuote:list', 'peoples', 'admin', NOW(), '金句摘录 + 书架（Tab 容器待实现）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'bookQuote', component = 'portal/bookQuote/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:bookQuote:list' AND parent_id IS NULL;
SELECT @bookquote_mid := menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' LIMIT 1;
SET @bookquote_mid := IFNULL(@bookquote_mid, @book_mid);

-- 2.1.3.1 书架管理（C 菜单，隐藏 visible=1，Tab 容器实现后从用户内容 Tab 进入）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书架管理', @book_mid, 6, 'bookshelf', 'portal/bookshelf/index', 1, 0, 'C', '1', '0', 'portal:bookshelf:list', '#', 'admin', NOW(), '用户书架查看/移除（隐藏，从用户内容 Tab 进入）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookshelf:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'bookshelf', component = 'portal/bookshelf/index', visible = '1', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:bookshelf:list' AND parent_id IS NULL;

-- 2.1.4 学习辅助（C 菜单：learn-aux Tab 容器已存在，直接指向）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '学习辅助', @book_mid, 7, 'learn-aux', 'portal/learn-aux/index', 1, 0, 'C', '0', '0', 'portal:learn:list', 'skill', 'admin', NOW(), '学习计划 + 错题本（Tab 容器已存在）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:learn:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'learn-aux', component = 'portal/learn-aux/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:learn:list' AND parent_id IS NULL;

-- 2.2 读书空间按钮权限（F 类型，按子模块挂在对应 C 菜单下）
-- 2.2.1 书籍管理按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍查询', @book_list_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:book:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:book:query');
UPDATE sys_menu SET parent_id = @book_list_mid WHERE perms = 'portal:book:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍新增', @book_list_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:book:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:book:add');
UPDATE sys_menu SET parent_id = @book_list_mid WHERE perms = 'portal:book:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍修改', @book_list_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:book:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:book:edit');
UPDATE sys_menu SET parent_id = @book_list_mid WHERE perms = 'portal:book:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍删除', @book_list_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:book:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:book:remove');
UPDATE sys_menu SET parent_id = @book_list_mid WHERE perms = 'portal:book:remove' AND parent_id IS NULL;

-- 2.2.2 章节管理按钮权限（含 publish 发布）
SELECT @bookchapter_mid := menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' LIMIT 1;
SET @bookchapter_mid := IFNULL(@bookchapter_mid, @book_mid);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节查询', @bookchapter_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:query');
UPDATE sys_menu SET parent_id = @bookchapter_mid WHERE perms = 'portal:bookChapter:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节新增', @bookchapter_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:add');
UPDATE sys_menu SET parent_id = @bookchapter_mid WHERE perms = 'portal:bookChapter:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节修改', @bookchapter_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:edit');
UPDATE sys_menu SET parent_id = @bookchapter_mid WHERE perms = 'portal:bookChapter:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节删除', @bookchapter_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:remove');
UPDATE sys_menu SET parent_id = @bookchapter_mid WHERE perms = 'portal:bookChapter:remove' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节发布', @bookchapter_mid, 5, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:publish', '#', 'admin', NOW(), '章节发布/撤回'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:publish');
UPDATE sys_menu SET parent_id = @bookchapter_mid WHERE perms = 'portal:bookChapter:publish' AND parent_id IS NULL;

-- 2.2.3 书单管理按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单查询', @booklist_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:query');
UPDATE sys_menu SET parent_id = @booklist_mid WHERE perms = 'portal:bookList:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单新增', @booklist_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:add');
UPDATE sys_menu SET parent_id = @booklist_mid WHERE perms = 'portal:bookList:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单修改', @booklist_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:edit', '#', 'admin', NOW(), '含：管理书籍（增删排序）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:edit');
UPDATE sys_menu SET parent_id = @booklist_mid WHERE perms = 'portal:bookList:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单删除', @booklist_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:remove');
UPDATE sys_menu SET parent_id = @booklist_mid WHERE perms = 'portal:bookList:remove' AND parent_id IS NULL;

-- 2.2.4 推荐位管理按钮权限
SELECT @bookrecommend_mid := menu_id FROM sys_menu WHERE perms = 'portal:bookRecommend:list' LIMIT 1;
SET @bookrecommend_mid := IFNULL(@bookrecommend_mid, @book_mid);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位查询', @bookrecommend_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookRecommend:query');
UPDATE sys_menu SET parent_id = @bookrecommend_mid WHERE perms = 'portal:bookRecommend:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位新增', @bookrecommend_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookRecommend:add');
UPDATE sys_menu SET parent_id = @bookrecommend_mid WHERE perms = 'portal:bookRecommend:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位修改', @bookrecommend_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:edit', '#', 'admin', NOW(), '含：上下架/排序'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookRecommend:edit');
UPDATE sys_menu SET parent_id = @bookrecommend_mid WHERE perms = 'portal:bookRecommend:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位删除', @bookrecommend_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookRecommend:remove');
UPDATE sys_menu SET parent_id = @bookrecommend_mid WHERE perms = 'portal:bookRecommend:remove' AND parent_id IS NULL;

-- 2.2.5 金句摘录按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句查询', @bookquote_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:query');
UPDATE sys_menu SET parent_id = @bookquote_mid WHERE perms = 'portal:bookQuote:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句新增', @bookquote_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:add');
UPDATE sys_menu SET parent_id = @bookquote_mid WHERE perms = 'portal:bookQuote:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句修改', @bookquote_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:edit');
UPDATE sys_menu SET parent_id = @bookquote_mid WHERE perms = 'portal:bookQuote:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句删除', @bookquote_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:remove');
UPDATE sys_menu SET parent_id = @bookquote_mid WHERE perms = 'portal:bookQuote:remove' AND parent_id IS NULL;

-- 2.2.6 书架按钮权限（仅 remove，无 add/edit）
SELECT @bookshelf_mid := menu_id FROM sys_menu WHERE perms = 'portal:bookshelf:list' LIMIT 1;
SET @bookshelf_mid := IFNULL(@bookshelf_mid, @book_mid);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书架移除', @bookshelf_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookshelf:remove', '#', 'admin', NOW(), '移出书架'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookshelf:remove');
UPDATE sys_menu SET parent_id = @bookshelf_mid WHERE perms = 'portal:bookshelf:remove' AND parent_id IS NULL;

-- --------------------------------------------------------------
-- 3. 角色菜单关联（为 role_id=1 管理员补全）
--    参照 93 / 96 / 99 范式，让角色分配界面观感一致，
--    以及让非 admin 角色可通过角色分配获得菜单权限。
-- --------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, m.menu_id, 'admin', NOW()
FROM sys_menu m
WHERE m.perms IN (
    -- 面试指南
    'cms:interview:list',
    'cms:interview:experience:list',
    'cms:interview:submission:list',
    -- 面试按钮
    'cms:interview:query', 'cms:interview:add', 'cms:interview:edit', 'cms:interview:remove',
    -- 读书空间
    'portal:book:list',
    'portal:bookChapter:list',
    'portal:bookList:list',
    'portal:bookRecommend:list',
    'portal:bookQuote:list',
    'portal:bookshelf:list',
    'portal:learn:list',
    -- 书籍按钮
    'portal:book:query', 'portal:book:add', 'portal:book:edit', 'portal:book:remove',
    -- 章节按钮
    'portal:bookChapter:query', 'portal:bookChapter:add', 'portal:bookChapter:edit', 'portal:bookChapter:remove', 'portal:bookChapter:publish',
    -- 书单按钮
    'portal:bookList:query', 'portal:bookList:add', 'portal:bookList:edit', 'portal:bookList:remove',
    -- 推荐位按钮
    'portal:bookRecommend:query', 'portal:bookRecommend:add', 'portal:bookRecommend:edit', 'portal:bookRecommend:remove',
    -- 金句按钮
    'portal:bookQuote:query', 'portal:bookQuote:add', 'portal:bookQuote:edit', 'portal:bookQuote:remove',
    -- 书架按钮
    'portal:bookshelf:remove'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- ====================================================================
-- 升级完成
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 107_升级脚本_v7.14_面经审核字段补齐.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.14 升级脚本：补面经审核字段 + 修复 Service 持久化
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行
-- 幂等性核查（P2-1）：✅ 已幂等 — DDL 用 information_schema+IF+PREPARE 守护；UPDATE 回填用 WHERE auditor_id IS NULL 守护；@var 仅用于 information_schema 查询+IF 条件+PREPARE 模式，符合 112 脚本范式。
--
-- 背景：
--   1. 面经表（portal_interview_experience）缺 auditor_id / audit_remark /
--      audit_time 字段；Service 层 auditExperience() 接收 remark 参数却未落库，
--      导致审核轨迹无法追溯（与文章/专栏/话题/评论的审核闭环不一致）。
--   2. 本脚本：
--      a) 补列：auditor_id / audit_remark / audit_time（对齐文章审核模式）
--      b) 数据回填：对存量已审核状态（published/rejected/archived）回填
--         auditor_id=1(admin)、audit_time=update_time，保证历史数据可查询
--      c) 加索引：idx_experience_auditor
-- ====================================================================

SET @db := DATABASE();

-- --------------------------------------------------------------
-- 1. 面经表：补审核字段
-- --------------------------------------------------------------

-- 1.1 auditor_id
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_experience' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_experience ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID，CMS审核时写入）'' AFTER status',
  'SELECT ''portal_interview_experience.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 audit_remark
SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_experience' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_experience ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因'' AFTER auditor_id',
  'SELECT ''portal_interview_experience.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 audit_time
SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_experience' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_experience ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_interview_experience.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- --------------------------------------------------------------
-- 2. 存量数据回填：仅对已审核状态设置默认值
--    status in ('published', 'rejected', 'archived') 视为"已经被处理过"，
--    回填 auditor_id=1(admin)、audit_time=update_time，保证历史查询不乱
-- --------------------------------------------------------------
UPDATE portal_interview_experience
   SET auditor_id = IFNULL(auditor_id, 1),
       audit_time = IFNULL(audit_time, update_time)
 WHERE status IN ('published', 'rejected', 'archived')
   AND auditor_id IS NULL;

-- --------------------------------------------------------------
-- 3. 审核人索引（按审核员维度查询待办/历史）
-- --------------------------------------------------------------
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_experience' AND INDEX_NAME = 'idx_experience_auditor') = 0,
  'ALTER TABLE portal_interview_experience ADD INDEX idx_experience_auditor (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ====================================================================
-- 升级完成
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 108_升级脚本_v7.15_面试指南与读书空间菜单重构.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.15 升级脚本：面试指南 & 读书空间 菜单重构（Tab 容器化 + 审核入口对齐）
-- 适配 MySQL 8.x
-- 性质：重构脚本（先删后建），但已改造为完全幂等可重复执行
-- 幂等性核查（P2-1）：✅ 已幂等 — INSERT 全部使用子查询替代 @var 获取 parent_id，NOT EXISTS 守护（符合规范）；
--   阶段 0 清理 DELETE 仍用 @old_interview_mid/@old_book_mid 传递业务状态，@var 丢失时清理不生效但 DELETE 天然幂等不报错不产生重复（INSERT 受 NOT EXISTS 保护不会重复创建）。
-- 说明：
--   1. 删除 106 号脚本注册的旧菜单（C 菜单 + F 按钮 + 隐藏子菜单），重新注册
--      ★ 修复点：递归清理孙级 F 按钮（避免孤儿残留）
--   2. C 菜单组件路径指向 Tab 容器页面（questionTab / experienceTab / bookListTab / userContent）
--   3. Tab 容器页面嵌入原有单模块页面，实现「减少可见菜单数量 + 解决无入口页面」
--   4. 审核类 Tab（面经审核/评论审核）已整合到「内容审核中心」，此处仅注册管理菜单
--   5. 子模块级 F 按钮权限按 C 菜单分组注册，含 Tab 内嵌面板的 list 权限码
--   6. 角色关联自动补全到 role_id=1（管理员）
--
-- 幂等保障：
--   ★ 1. INSERT 全部使用 `INSERT ... SELECT ... WHERE NOT EXISTS` 模式（基于 path/perms 唯一性）
--   ★ 2. parent_id 通过子查询动态获取，不依赖 LAST_INSERT_ID() 会话状态
--   ★ 3. DELETE 子树递归清理（孙级 F 按钮），不留孤儿
--   ★ 4. 多次执行结果一致：旧记录被清干净 → 新记录按 NOT EXISTS 插入 → 角色关联按 NOT EXISTS 绑定
--
-- 前台栏目 → 后台菜单 对照：
--   【读书空间】
--     读书首页   → 书籍管理 + 书单&推荐位 + 用户内容（数据聚合）
--     发现好书   → 书籍管理（推荐位+书单支撑）
--     金句摘录   → 用户内容 → 金句摘录 Tab
--     我的书架   → 用户内容 → 书架管理 Tab
--   【面试指南】
--     面试题库   → 题库资源（题库+分类+公司+简历 Tab）
--     面试经验   → 面经运营（面经+评论 Tab）
--     简历模板   → 题库资源 → 简历模板 Tab
--     AI 模拟面试 → 前台用户功能，无后台管理页
--     学习中心   → 学习辅助（学习计划+错题本 Tab）
--     知识图谱   → 统计聚合数据，由内容管理支撑
--     刷题排行榜 → 统计聚合数据，由内容管理支撑
--     学习计划   → 学习辅助 → 学习计划 Tab
--     错题本     → 学习辅助 → 错题本 Tab
--     刷题日历   → 统计聚合数据，由内容管理支撑
-- ====================================================================

SET @db := DATABASE();

-- --------------------------------------------------------------
-- 0. 清理旧菜单（106 号脚本注册的所有面试指南/读书空间菜单）
--    ★ 修复点：递归清理三级子树（M 目录 → C 菜单 → F 按钮）
--      旧脚本仅删 parent_id = M 的直系 C 菜单，遗漏孙级 F 按钮，导致孤儿残留
-- --------------------------------------------------------------
-- 0.1 获取面试指南/读书空间顶级目录 ID
SELECT @old_interview_mid := menu_id FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;
SELECT @old_book_mid := menu_id FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;

-- 0.2 ★ 递归清理孙级 F 按钮（其 parent_id 指向将被删除的 C 菜单）
--     思路：先找 C 菜单 ID 集合，再删其下的 F 按钮 + 对应 sys_role_menu
--     注意：MySQL 不允许 DELETE FROM t WHERE ... IN (SELECT ... FROM t)（错误 1093），
--           改用 JOIN 别名形式：DELETE m FROM sys_menu m INNER JOIN sys_menu p ON ...
DELETE rm FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
INNER JOIN sys_menu p ON m.parent_id = p.menu_id
WHERE p.parent_id IN (@old_interview_mid, @old_book_mid)
  AND p.menu_type IN ('C', 'F')
  AND m.menu_type = 'F';

DELETE m FROM sys_menu m
INNER JOIN sys_menu p ON m.parent_id = p.menu_id
WHERE p.parent_id IN (@old_interview_mid, @old_book_mid)
  AND p.menu_type IN ('C', 'F')
  AND m.menu_type = 'F';

-- 0.3 删除直系子菜单 C 的角色关联 + C 菜单本身
--     用会话变量缓存 ID 列表，避免同表子查询报 1093
SELECT GROUP_CONCAT(menu_id) INTO @c_menu_ids
FROM sys_menu
WHERE parent_id IN (@old_interview_mid, @old_book_mid) AND menu_type = 'C';

SET @del_role_sql := IF(@c_menu_ids IS NOT NULL,
    CONCAT('DELETE FROM sys_role_menu WHERE menu_id IN (', @c_menu_ids, ')'),
    'SELECT 1');
PREPARE stmt FROM @del_role_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @del_menu_sql := IF(@c_menu_ids IS NOT NULL,
    CONCAT('DELETE FROM sys_menu WHERE menu_id IN (', @c_menu_ids, ')'),
    'SELECT 1');
PREPARE stmt FROM @del_menu_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 0.4 删除顶级目录的角色关联 + 顶级目录本身
DELETE FROM sys_role_menu WHERE menu_id IN (@old_interview_mid, @old_book_mid);
DELETE FROM sys_menu WHERE menu_id = @old_interview_mid;
DELETE FROM sys_menu WHERE menu_id = @old_book_mid;

-- --------------------------------------------------------------
-- 1. 面试指南（重新注册：1 M + 3 C + 4 组 F 按钮）
--    ★ 所有 INSERT 使用 `WHERE NOT EXISTS` 守护，parent_id 通过子查询获取
-- --------------------------------------------------------------

-- 1.1 顶级目录（按 path+parent_id+menu_type 唯一）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试指南', 0, 15, 'interview', '', 1, 0, 'M', '0', '0', NULL, 'guide', 'admin', NOW(), '面试指南一级目录：题库/面经/简历/笔记'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M'
);

-- 1.2.1 题库资源（C：Tab 容器 4 个 Tab）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库资源',
       (SELECT menu_id FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M' LIMIT 1),
       1, 'questionTab', 'cms/interview/questionTab/index', 1, 0, 'C', '0', '0', 'cms:interview:list', 'tree-table', 'admin', NOW(), '面试题库+分类+公司标签+简历模板 Tab 容器'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C'
);

-- 1.2.2 面经运营（C：Tab 容器 2 个 Tab）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面经运营',
       (SELECT menu_id FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M' LIMIT 1),
       2, 'experienceTab', 'cms/interview/experienceTab/index', 1, 0, 'C', '0', '0', 'cms:interview:experience:list', 'edit', 'admin', NOW(), '面经管理+评论管理 Tab 容器；审核入口在内容审核中心'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:experience:list' AND menu_type = 'C'
);

-- 1.2.3 精选笔记（C：独立页，采纳/取消精选）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '精选笔记',
       (SELECT menu_id FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M' LIMIT 1),
       3, 'submission', 'cms/interview/submission/index', 1, 0, 'C', '0', '0', 'cms:interview:submission:list', 'star', 'admin', NOW(), '精选笔记采纳与取消'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:submission:list' AND menu_type = 'C'
);

-- 1.3.1 题库资源 按钮（按 perms+parent_id 唯一）
-- 注意：相同 perms 可挂在不同 C 菜单下，所以守护条件为 perms+parent_id
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C' LIMIT 1),
       1, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'cms:interview:query'
    AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库新增',
       (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C' LIMIT 1),
       2, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'cms:interview:add'
    AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库修改',
       (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C' LIMIT 1),
       3, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '含：审核/置顶/精选采纳等运营操作'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'cms:interview:edit'
    AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库删除',
       (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C' LIMIT 1),
       4, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'cms:interview:remove'
    AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C' LIMIT 1)
);

-- 1.3.2 面经运营 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面经查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:experience:list' AND menu_type = 'C' LIMIT 1),
       1, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'cms:interview:query'
    AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:experience:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面经修改',
       (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:experience:list' AND menu_type = 'C' LIMIT 1),
       2, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '含：审核/置顶/评论管理'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'cms:interview:edit'
    AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:experience:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面经删除',
       (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:experience:list' AND menu_type = 'C' LIMIT 1),
       3, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'cms:interview:remove'
    AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:experience:list' AND menu_type = 'C' LIMIT 1)
);

-- 1.3.3 精选笔记 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '笔记查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:submission:list' AND menu_type = 'C' LIMIT 1),
       1, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'cms:interview:query'
    AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:submission:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '笔记修改',
       (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:submission:list' AND menu_type = 'C' LIMIT 1),
       2, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '含：采纳/取消精选'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'cms:interview:edit'
    AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:submission:list' AND menu_type = 'C' LIMIT 1)
);

-- --------------------------------------------------------------
-- 2. 读书空间（重新注册：1 M + 4 C + 多组 F 按钮）
-- --------------------------------------------------------------

-- 2.1 顶级目录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '读书空间', 0, 16, 'book', '', 1, 0, 'M', '0', '0', NULL, 'book', 'admin', NOW(), '读书空间一级目录：书籍/书单/金句/学习'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M'
);

-- 2.2.1 书籍管理（C）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍管理',
       (SELECT menu_id FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M' LIMIT 1),
       1, 'book-index', 'portal/book/index', 1, 0, 'C', '0', '0', 'portal:book:list', 'documentation', 'admin', NOW(), '书籍CRUD + 章节导入向导（章节管理为隐藏子路由）'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'portal:book:list' AND menu_type = 'C'
);

-- 2.2.1.1 章节管理（隐藏 C 菜单，从书籍详情跳转）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节管理',
       (SELECT menu_id FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M' LIMIT 1),
       2, 'bookChapter', 'portal/bookChapter/index', 1, 0, 'C', '1', '0', 'portal:bookChapter:list', '#', 'admin', NOW(), '书籍章节CRUD + 发布/批量导入（隐藏菜单，从书籍详情跳转）'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C'
);

-- 2.2.2 书单&推荐位（C：Tab 容器 2 个 Tab）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单&推荐位',
       (SELECT menu_id FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M' LIMIT 1),
       3, 'bookListTab', 'portal/bookListTab/index', 1, 0, 'C', '0', '0', 'portal:bookList:list', 'list', 'admin', NOW(), '书单管理+推荐位管理 Tab 容器'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C'
);

-- 2.2.3 用户内容（C：Tab 容器 2 个 Tab，金句+书架）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户内容',
       (SELECT menu_id FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M' LIMIT 1),
       4, 'userContent', 'portal/userContent/index', 1, 0, 'C', '0', '0', 'portal:bookQuote:list', 'peoples', 'admin', NOW(), '金句摘录+书架管理 Tab 容器'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C'
);

-- 2.2.4 学习辅助（C：Tab 容器，学习计划+错题本）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '学习辅助',
       (SELECT menu_id FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M' LIMIT 1),
       5, 'learn-aux', 'portal/learn-aux/index', 1, 0, 'C', '0', '0', 'portal:learn:list', 'skill', 'admin', NOW(), '学习计划+错题本 Tab 容器'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'portal:learn:list' AND menu_type = 'C'
);

-- 2.3.1 书籍管理 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:book:list' AND menu_type = 'C' LIMIT 1),
       1, '#', '', 1, 0, 'F', '0', '0', 'portal:book:query', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:book:query' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:book:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍新增',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:book:list' AND menu_type = 'C' LIMIT 1),
       2, '#', '', 1, 0, 'F', '0', '0', 'portal:book:add', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:book:add' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:book:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍修改',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:book:list' AND menu_type = 'C' LIMIT 1),
       3, '#', '', 1, 0, 'F', '0', '0', 'portal:book:edit', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:book:edit' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:book:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍删除',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:book:list' AND menu_type = 'C' LIMIT 1),
       4, '#', '', 1, 0, 'F', '0', '0', 'portal:book:remove', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:book:remove' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:book:list' AND menu_type = 'C' LIMIT 1)
);

-- 2.3.2 章节管理 按钮（含 publish）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C' LIMIT 1),
       1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:query', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookChapter:query' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节新增',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C' LIMIT 1),
       2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:add', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookChapter:add' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节修改',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C' LIMIT 1),
       3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:edit', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookChapter:edit' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节删除',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C' LIMIT 1),
       4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:remove', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookChapter:remove' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节发布',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C' LIMIT 1),
       5, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:publish', '#', 'admin', NOW(), '章节发布/撤回'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookChapter:publish' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' AND menu_type = 'C' LIMIT 1)
);

-- 2.3.3 书单&推荐位 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1),
       1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:query', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookList:query' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单新增',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1),
       2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:add', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookList:add' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单修改',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1),
       3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:edit', '#', 'admin', NOW(), '含：管理书籍（增删排序）'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookList:edit' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单删除',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1),
       4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:remove', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookList:remove' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位列表',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1),
       5, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:list', '#', 'admin', NOW(), 'Tab 内推荐位面板列表权限'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookRecommend:list' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1),
       6, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:query', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookRecommend:query' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位新增',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1),
       7, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:add', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookRecommend:add' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位修改',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1),
       8, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:edit', '#', 'admin', NOW(), '含：上下架/排序'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookRecommend:edit' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位删除',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1),
       9, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:remove', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookRecommend:remove' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' AND menu_type = 'C' LIMIT 1)
);

-- 2.3.4 用户内容 按钮（金句 + 书架）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1),
       1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:query', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookQuote:query' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句新增',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1),
       2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:add', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookQuote:add' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句修改',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1),
       3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:edit', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookQuote:edit' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句删除',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1),
       4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:remove', '#', 'admin', NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookQuote:remove' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书架列表',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1),
       5, '#', '', 1, 0, 'F', '0', '0', 'portal:bookshelf:list', '#', 'admin', NOW(), 'Tab 内书架面板列表权限'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookshelf:list' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书架移除',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1),
       6, '#', '', 1, 0, 'F', '0', '0', 'portal:bookshelf:remove', '#', 'admin', NOW(), '移出书架'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:bookshelf:remove' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' AND menu_type = 'C' LIMIT 1)
);

-- 2.3.5 学习辅助 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '学习计划查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:learn:list' AND menu_type = 'C' LIMIT 1),
       1, '#', '', 1, 0, 'F', '0', '0', 'portal:studyPlan:list', '#', 'admin', NOW(), '学习计划只读列表'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:studyPlan:list' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:learn:list' AND menu_type = 'C' LIMIT 1)
);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '错题本查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'portal:learn:list' AND menu_type = 'C' LIMIT 1),
       2, '#', '', 1, 0, 'F', '0', '0', 'portal:wrongQuestion:list', '#', 'admin', NOW(), '错题本只读列表'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu m
  WHERE m.perms = 'portal:wrongQuestion:list' AND m.menu_type = 'F'
    AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:learn:list' AND menu_type = 'C' LIMIT 1)
);

-- --------------------------------------------------------------
-- 3. 角色菜单关联（为 role_id=1 管理员补全）
--    使用子查询定位 interview/book 顶级目录，避免依赖 @interview_mid/@book_mid 变量
-- --------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, m.menu_id, 'admin', NOW()
FROM sys_menu m
WHERE m.perms IN (
    -- 面试指南 C 菜单
    'cms:interview:list',
    'cms:interview:experience:list',
    'cms:interview:submission:list',
    -- 面试指南 F 按钮
    'cms:interview:query', 'cms:interview:add', 'cms:interview:edit', 'cms:interview:remove',
    -- 读书空间 C 菜单
    'portal:book:list',
    'portal:bookChapter:list',
    'portal:bookList:list',
    'portal:bookRecommend:list',
    'portal:bookQuote:list',
    'portal:bookshelf:list',
    'portal:learn:list',
    -- 书籍按钮
    'portal:book:query', 'portal:book:add', 'portal:book:edit', 'portal:book:remove',
    -- 章节按钮
    'portal:bookChapter:query', 'portal:bookChapter:add', 'portal:bookChapter:edit', 'portal:bookChapter:remove', 'portal:bookChapter:publish',
    -- 书单&推荐位按钮
    'portal:bookList:query', 'portal:bookList:add', 'portal:bookList:edit', 'portal:bookList:remove',
    'portal:bookRecommend:query', 'portal:bookRecommend:add', 'portal:bookRecommend:edit', 'portal:bookRecommend:remove',
    -- 用户内容按钮
    'portal:bookQuote:query', 'portal:bookQuote:add', 'portal:bookQuote:edit', 'portal:bookQuote:remove',
    'portal:bookshelf:remove',
    -- 学习辅助内嵌面板权限
    'portal:studyPlan:list',
    'portal:wrongQuestion:list'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- 为 role_id=1 管理员补全顶级目录的关联
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, m.menu_id, 'admin', NOW()
FROM sys_menu m
WHERE (m.path = 'interview' AND m.parent_id = 0 AND m.menu_type = 'M'
    OR m.path = 'book' AND m.parent_id = 0 AND m.menu_type = 'M')
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- ====================================================================
-- 升级完成
-- 变更清单：
--   1. 面试指南：3 个可见菜单（题库资源/面经运营/精选笔记），
--      题库资源 = 题库+分类+公司+简历（4 Tab）
--      面经运营 = 面经+评论（2 Tab）
--   2. 读书空间：4 个可见菜单（书籍管理/书单&推荐位/用户内容/学习辅助），
--      书单&推荐位 = 书单+推荐位（2 Tab，名实一致）
--      用户内容 = 金句+书架（2 Tab，消除隐藏菜单）
--      章节管理保留为隐藏子路由（从书籍详情跳转）
--   3. 审核类 Tab 已在内容审核中心统一管理（面经审核/评论审核）
--      审核中心菜单由 103 号脚本管理，本脚本不重复注册
--
-- ★ 幂等性保障（v2 改进）：
--   1. 所有 INSERT 使用 INSERT...SELECT...WHERE NOT EXISTS 守护（基于 path/perms+parent_id 唯一）
--   2. parent_id 通过子查询动态获取（移除 LAST_INSERT_ID() 依赖）
--   3. DELETE 子树递归清理孙级 F 按钮（避免孤儿残留）
--   4. 角色关联使用 NOT EXISTS 守护，避免重复绑定
--   5. 多次执行结果一致：清旧 → 按需新建 → 角色按需绑定
-- ====================================================================


-- ---------------------------------------------------------------
-- 来源: 109_升级脚本_v7.16_AI模块菜单重构.sql
-- ---------------------------------------------------------------
-- =============================================================================
-- 109_升级脚本_v7.16_AI模块菜单重构.sql（修复版 v2）
--
-- 修复内容：
--   1. 去除所有 @变量，改用子查询，避免 SQL 客户端会话变量不保持导致 UPDATE 不生效
--   2. 修复运营监控 INSERT 语句的 LIMIT 1 位置错误
--   3. 确保每条语句独立可执行（即使分段执行也不会出错）
-- 幂等：可重复执行
-- 幂等性核查（P2-1）：✅ 已幂等 — 已全面去除 @var，INSERT 用 FROM DUAL WHERE NOT EXISTS + 子查询获取 parent_id；
--   UPDATE 用 INNER JOIN 子查询定位 + WHERE 守护（parent_id=ai.menu_id 防重复 concat remark）；INSERT sys_role_menu 用 NOT EXISTS 守护。
-- =============================================================================

-- =============================================================================
-- 一、创建 3 个 M 目录（parent_id 用子查询动态获取，避免变量丢失）
-- =============================================================================

-- 1. 知识中心 M 目录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识中心',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1),
       2, 'knowledge-center', 'ai/knowledge-center/index', NULL, 1, 0, 'M', '0', '0', '', 'documentation', 'admin', NOW(), '知识中心目录（知识库管理+知识文库 Tab）'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu mc
    WHERE mc.menu_name = '知识中心'
      AND mc.parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1)
);

-- 2. AI基础配置 M 目录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'AI基础配置',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1),
       4, 'ai-config', NULL, NULL, 1, 0, 'M', '0', '0', '', 'system', 'admin', NOW(), 'AI基础配置目录（模型配置+工具管理+数据源管理）'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu mc
    WHERE mc.menu_name = 'AI基础配置'
      AND mc.parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1)
);

-- 3. 运营监控 M 目录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '运营监控',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1),
       9, 'ai-monitor', NULL, NULL, 1, 0, 'M', '0', '0', '', 'monitor', 'admin', NOW(), '运营监控目录（概览大屏+Token统计）'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu mc
    WHERE mc.menu_name = '运营监控'
      AND mc.parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1)
);

-- =============================================================================
-- 二、调整子菜单 parent_id 和 order_num（全部用子查询获取目标 parent_id）
-- =============================================================================

-- 知识库管理 → 挂到知识中心目录下，设为隐藏（由 Tab 容器承载）
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
INNER JOIN sys_menu kc ON kc.menu_name = '知识中心' AND kc.parent_id = ai.menu_id
SET m.parent_id = kc.menu_id, m.order_num = 1, m.visible = '1',
    m.remark = CONCAT(IFNULL(m.remark, ''), ' [v7.16 已整合到知识中心 Tab 容器]')
WHERE m.perms = 'cms:ai:knowledge-base:list'
  AND m.parent_id = ai.menu_id;

-- 知识文库 → 挂到知识中心目录下，设为隐藏（由 Tab 容器承载）
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
INNER JOIN sys_menu kc ON kc.menu_name = '知识中心' AND kc.parent_id = ai.menu_id
SET m.parent_id = kc.menu_id, m.order_num = 2, m.visible = '1',
    m.remark = CONCAT(IFNULL(m.remark, ''), ' [v7.16 已整合到知识中心 Tab 容器]')
WHERE m.perms = 'cms:ai:knowledge-library:list'
  AND m.parent_id = ai.menu_id;

-- 模型配置 → 挂到 AI基础配置目录下
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
INNER JOIN sys_menu cfg ON cfg.menu_name = 'AI基础配置' AND cfg.parent_id = ai.menu_id
SET m.parent_id = cfg.menu_id, m.order_num = 1
WHERE m.perms = 'cms:ai:model-config:list'
  AND m.parent_id = ai.menu_id;

-- 工具管理 → 挂到 AI基础配置目录下
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
INNER JOIN sys_menu cfg ON cfg.menu_name = 'AI基础配置' AND cfg.parent_id = ai.menu_id
SET m.parent_id = cfg.menu_id, m.order_num = 2
WHERE m.perms = 'cms:ai:tool:list'
  AND m.parent_id = ai.menu_id;

-- 数据源管理 → 挂到 AI基础配置目录下
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
INNER JOIN sys_menu cfg ON cfg.menu_name = 'AI基础配置' AND cfg.parent_id = ai.menu_id
SET m.parent_id = cfg.menu_id, m.order_num = 3
WHERE m.perms = 'cms:ai:datasource:list'
  AND m.parent_id = ai.menu_id;

-- 工作流管理 → order=3（保持顶级）
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
SET m.order_num = 3
WHERE m.perms = 'cms:ai:workflow:list'
  AND m.parent_id = ai.menu_id;

-- 领域词典 → order=5（保持顶级）
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
SET m.order_num = 5
WHERE m.perms = 'cms:ai:domain-dictionary:list'
  AND m.parent_id = ai.menu_id;

-- 数据分析 → order=6，改名为 AI数据分析
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
SET m.order_num = 6, m.menu_name = 'AI数据分析'
WHERE m.perms = 'cms:ai:data-analysis:list'
  AND m.parent_id = ai.menu_id
  AND m.menu_name != 'AI数据分析';

-- 架构图生成 → order=7（保持顶级）
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
SET m.order_num = 7
WHERE m.perms = 'cms:ai:diagram:list'
  AND m.parent_id = ai.menu_id;

-- AI数据大屏 → 挂到运营监控目录下，改名为 概览大屏
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
INNER JOIN sys_menu mon ON mon.menu_name = '运营监控' AND mon.parent_id = ai.menu_id
SET m.parent_id = mon.menu_id, m.order_num = 1, m.menu_name = '概览大屏'
WHERE m.perms = 'cms:ai:dashboard:list'
  AND m.parent_id = ai.menu_id
  AND m.menu_name != '概览大屏';

-- 如果大屏已不在智能AI下（比如重复执行），确保改名生效
UPDATE sys_menu SET menu_name = '概览大屏'
WHERE perms = 'cms:ai:dashboard:list' AND menu_name != '概览大屏';

-- Token统计 → 挂到运营监控目录下
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
INNER JOIN sys_menu mon ON mon.menu_name = '运营监控' AND mon.parent_id = ai.menu_id
SET m.parent_id = mon.menu_id, m.order_num = 2
WHERE m.perms = 'cms:ai:token-usage:list'
  AND m.parent_id = ai.menu_id;

-- 智能体管理 → order=1（保持顶级）
UPDATE sys_menu m
INNER JOIN sys_menu ai ON ai.menu_name = '智能AI' AND ai.parent_id = 0
SET m.order_num = 1
WHERE m.perms = 'cms:ai:agent:list'
  AND m.parent_id = ai.menu_id;

-- =============================================================================
-- 三、超级管理员角色自动关联新增的 M 目录菜单（用子查询获取 menu_id）
-- =============================================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.menu_name IN ('知识中心', 'AI基础配置', '运营监控')
  AND m.parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1)
  AND m.menu_type = 'M'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

-- =============================================================================
-- 四、校验输出
-- =============================================================================
SELECT '=== AI 模块菜单结构（重构后）===' AS info;
SELECT
    CASE
        WHEN p.menu_type = 'M' AND p.parent_id = 0 THEN CONCAT('■ ', p.menu_name)
        WHEN c.menu_type = 'M' THEN CONCAT('  └─ ■ ', c.menu_name, ' (M目录)')
        ELSE CONCAT('     ├─ ', c.menu_name)
    END AS 菜单层级,
    c.perms AS 权限标识,
    c.order_num AS 排序,
    c.visible AS 可见
FROM sys_menu p
LEFT JOIN sys_menu c ON c.parent_id = p.menu_id AND c.menu_type IN ('C', 'M')
WHERE p.menu_name = '智能AI' AND p.parent_id = 0
ORDER BY c.order_num;

SELECT '=== 重构完成 ===' AS info;


-- ---------------------------------------------------------------
-- 来源: 114_升级脚本_v7.22_代码生成菜单修复.sql
-- ---------------------------------------------------------------
-- =============================================================================
-- v7.22 升级脚本：代码生成菜单（/tool/gen）404 修复
-- 适配 MySQL 8.0.29+
-- 说明：本脚本幂等，可重复执行
-- 背景：
--   P2-5 评审项：访问 /tool/gen 出现 404，前端 loadView 找不到 component。
--   根因：DB sys_menu 表中 menu_id=116 记录缺失或 component 字段为空，
--         导致后端 getRouters() 不返回该菜单，前端 store/modules/permission.js
--         无法通过 loadView() 注册动态路由，最终被 router 兜底到 404 页面。
-- 代码侧已确认：
--   1. GenController.java 完整存在（@RequestMapping("/tool/gen")）；
--   2. 前端 api/tool/gen.js、views/tool/gen/*、router/index.js 动态路由均存在；
--   3. 菜单种子 INSERT 在 90_菜单权限_RuoYi.sql 行 26 已定义。
-- 本脚本职责：
--   1. 补齐父目录 menu_id=3（系统工具目录，通常已存在）；
--   2. 补齐或修复 menu_id=116（代码生成页面，component='tool/gen/index'）；
--   3. 补齐或修复子按钮权限 menu_id 1055-1060（tool:gen:query/edit/remove/import/preview/code）；
--   4. 绑定 admin 角色到上述菜单（避免角色无权限导致菜单不可见）。
-- 验收：
--   - SELECT 后 menu_id=116 component='tool/gen/index' 且 visible='0' status='0'；
--   - 前端 /tool/gen 页面可正常访问，控制台无 missing component 警告。
-- =============================================================================

-- ============================================================================
-- 一、诊断：查看当前 menu_id=116 及子菜单状态
-- ============================================================================
SELECT '========== 修复前：menu_id=116 代码生成菜单现状 ==========' AS info;
SELECT menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms
FROM sys_menu
WHERE menu_id IN (3, 116, 1055, 1056, 1057, 1058, 1059, 1060)
ORDER BY menu_id;

-- ============================================================================
-- 二、补齐父目录 menu_id=3（系统工具目录）
--    多数环境已存在，使用 NOT EXISTS 跳过
-- ============================================================================
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon,
                      create_by, create_time, update_by, update_time, remark, del_flag)
SELECT '3', '系统工具', '0', '3', 'tool', NULL, '', '',
       1, 0, 'M', '0', '0', '', 'tool',
       'admin', NOW(), '', NULL, '系统工具目录', '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3);

-- ============================================================================
-- 三、补齐或修复 menu_id=116（代码生成页面）
--    策略：
--      a) 若不存在，则插入完整记录；
--      b) 若存在但 component 为空，则仅更新 component/visible/status/path；
--      c) 若存在且 component 已正确，则跳过。
-- ============================================================================
-- 3.1 不存在则插入
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon,
                      create_by, create_time, update_by, update_time, remark, del_flag)
SELECT '116', '代码生成', '3', '2', 'gen', 'tool/gen/index', '', '',
       1, 0, 'C', '0', '0', 'tool:gen:list', 'code',
       'admin', NOW(), '', NULL, '代码生成菜单', '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 116);

-- 3.2 存在但 component 为空/NULL，仅更新关键字段（保留原 create_by/create_time）
UPDATE sys_menu
SET component = 'tool/gen/index',
    path      = 'gen',
    parent_id = 3,
    order_num = 2,
    menu_type = 'C',
    visible   = '0',
    status    = '0',
    perms     = COALESCE(NULLIF(perms, ''), 'tool:gen:list'),
    icon      = COALESCE(NULLIF(icon, ''), 'code'),
    update_by = 'admin',
    update_time = NOW()
WHERE menu_id = 116
  AND (component IS NULL OR component = '');

-- ============================================================================
-- 四、补齐或修复子按钮权限 menu_id 1055-1060
--    使用 ON DUPLICATE KEY UPDATE 保证幂等
-- ============================================================================
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon,
                      create_by, create_time, update_by, update_time, remark, del_flag)
VALUES
  ('1055', '生成查询', '116', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query',   '#', 'admin', NOW(), '', NULL, '', '0'),
  ('1056', '生成修改', '116', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit',    '#', 'admin', NOW(), '', NULL, '', '0'),
  ('1057', '生成删除', '116', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove',  '#', 'admin', NOW(), '', NULL, '', '0'),
  ('1058', '导入代码', '116', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import',  '#', 'admin', NOW(), '', NULL, '', '0'),
  ('1059', '预览代码', '116', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 'admin', NOW(), '', NULL, '', '0'),
  ('1060', '生成代码', '116', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code',    '#', 'admin', NOW(), '', NULL, '', '0')
ON DUPLICATE KEY UPDATE
  parent_id  = VALUES(parent_id),
  order_num  = VALUES(order_num),
  menu_type  = VALUES(menu_type),
  visible    = VALUES(visible),
  status     = VALUES(status),
  perms      = VALUES(perms),
  update_by  = 'admin',
  update_time = NOW();

-- ============================================================================
-- 五、绑定 admin 角色（role_id=1）到上述菜单
--    避免角色无权限导致 getRouters() 不返回该菜单
-- ============================================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.menu_id IN (3, 116, 1055, 1056, 1057, 1058, 1059, 1060)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

-- ============================================================================
-- 六、修复后验证
-- ============================================================================
SELECT '========== 修复后：menu_id=116 代码生成菜单现状 ==========' AS info;
SELECT menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms
FROM sys_menu
WHERE menu_id IN (3, 116, 1055, 1056, 1057, 1058, 1059, 1060)
ORDER BY menu_id;

SELECT '========== admin 角色对上述菜单的绑定情况 ==========' AS info;
SELECT m.menu_id, m.menu_name, m.component,
       CASE WHEN rm.role_id IS NOT NULL THEN '✓ 已授权' ELSE '⚠️ 未授权' END AS admin授权状态
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON rm.menu_id = m.menu_id AND rm.role_id = 1
WHERE m.menu_id IN (3, 116, 1055, 1056, 1057, 1058, 1059, 1060)
ORDER BY m.menu_id;

-- ============================================================================
-- 七、运维提示（注释，不执行）
-- ============================================================================
-- 1. 若执行后访问 /tool/gen 仍 404：
--    a) 清除浏览器 LocalStorage 中的 Admin-roles/Admin-Token 缓存，重新登录获取新路由表；
--    b) 前端 dev 模式需重启 `npm run dev`，让 import.meta.glob 重新求值缓存；
--    c) 部署版需 `npm run build:prod` 重新构建以纳入新文件。
-- 2. 若其他菜单（如 monitor、cms）也出现 component 为空导致 404，可参考本脚本模式批量修复。
-- =============================================================================


-- ---------------------------------------------------------------
-- 来源: 116_升级脚本_v7.24_旧菜单全隐藏.sql
-- ---------------------------------------------------------------
-- ====================================================================
-- v7.24 升级脚本：旧菜单全隐藏（P2-3 闭环）
-- 适配 MySQL 8.x
-- 性质：整合脚本（合并 100_v7.7 + 104_v7.11 + fix_monitor_legacy_menu_hide.sql 的隐藏操作）
-- 说明：
--   背景：被 Tab 容器替代的旧独立菜单在历史脚本中已多次隐藏，
--         但因 menu_id 被重分配、perms 命名差异、脚本未执行等场景，
--         部分生产环境仍存在重复侧边栏入口。
--   策略：多重定位（menu_id + perms + path + component），强制 visible='1'
--         不删除旧菜单记录，保留权限项以兼容历史角色分配，支持回滚
--   覆盖范围：16 条已确认旧菜单 + 1 条疑似虚构项（dashboard-old）核查
--
-- 幂等保障：
--   ★ 1. 所有 UPDATE 带 `WHERE visible = '0'` 守护，已隐藏的不重复更新
--   ★ 2. 使用多重 OR 条件，覆盖 menu_id / perms / path+component 三种定位方式
--   ★ 3. 不影响 Tab 容器菜单本身（容器菜单的 perms 与旧菜单不同）
--   ★ 4. 包含修复前后两次 SELECT 校验，便于审计
--
-- 旧菜单清单（16 条已确认 + 1 条疑似虚构）：
--   监控模块（6 条）：druid / server / cache / cacheList / operlog / logininfor
--   CMS 模块（8 条）：ad / friend-link / help-category / help-article / feedback / report / growth-rule / growth-achievement
--   Portal 模块（2 条）：order / tip（已是 visible=1，无需重复更新）
--   AI 模块（1 条）：dashboard-old（疑似虚构，仅 SELECT 核查，存在则隐藏）
-- ====================================================================

SET @db := DATABASE();

-- ====================================================================
-- 一、诊断：查看当前旧菜单的可见状态（修复前快照）
-- ====================================================================
SELECT '========== 修复前：所有待隐藏旧菜单现状 ==========' AS info;
SELECT menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms,
       CASE WHEN visible = '0' THEN '⚠️ 显示中（应隐藏）'
            WHEN visible = '1' THEN '✓ 已隐藏'
            ELSE '❓ 异常值' END AS 状态
FROM sys_menu
WHERE menu_type = 'C'
  AND (
    -- 监控模块（6 条）
    menu_id IN (111, 112, 113, 114, 500, 501)
    OR perms IN ('monitor:druid:list', 'monitor:server:list',
                 'monitor:cache:list', 'monitor:operlog:list', 'monitor:logininfor:list')
    OR (path IN ('druid', 'server', 'cache', 'cacheList', 'operlog', 'logininfor')
        AND component LIKE 'monitor/%')
    -- CMS 模块（8 条）
    OR perms IN ('portal:ad:list', 'cms:friend-link:list',
                 'cms:help-category:list', 'cms:help-article:list',
                 'cms:feedback:list', 'cms:report:list',
                 'cms:growth:rule:list', 'cms:growth:achievement:list',
                 'cms:growth-rule:list', 'cms:growth-achievement:list')
    OR (path IN ('ad', 'friend-link', 'help-category', 'help-article',
                 'feedback', 'report', 'growth-rule', 'growth-achievement')
        AND component LIKE 'cms/%')
    -- Portal 模块（2 条）
    OR perms IN ('portal:order:list', 'portal:tip:list')
    OR (path IN ('order', 'tip') AND component LIKE 'portal/%')
    -- AI 模块疑似虚构项（dashboard-old）
    OR path = 'dashboard-old'
    OR (menu_name = 'AI数据大屏' AND perms = 'cms:ai:dashboard:list' AND path != 'dashboard')
  )
ORDER BY menu_id;

-- ====================================================================
-- 二、强制隐藏旧独立菜单（visible=1 隐藏，status=0 正常保留权限）
-- ====================================================================

-- 2.1 监控模块（6 条）：被 server-panel / cache-manage / log-audit Tab 容器替代
UPDATE sys_menu
SET visible = '1',
    status = '0',
    update_by = 'admin',
    update_time = NOW(),
    remark = CONCAT(IFNULL(remark, ''), ' [v7.24 已合并到 Tab 容器，隐藏]')
WHERE menu_type = 'C'
  AND visible = '0'
  AND (
    -- 1. 按 menu_id（RuoYi 原版 ID）
    menu_id IN (111, 112, 113, 114, 500, 501)
    -- 2. 按 perms
    OR perms IN ('monitor:druid:list', 'monitor:server:list',
                 'monitor:cache:list', 'monitor:operlog:list', 'monitor:logininfor:list')
    -- 3. 按 path + component 联合（应对 menu_id 被重分配场景）
    OR (path IN ('druid', 'server', 'cache', 'cacheList', 'operlog', 'logininfor')
        AND component IN ('monitor/druid/index', 'monitor/server/index',
                          'monitor/cache/index', 'monitor/cache/list',
                          'monitor/operlog/index', 'monitor/logininfor/index'))
  );

-- 2.2 CMS 模块（8 条）：被 promotion / feedback-center / help-center / growth-config Tab 容器替代
UPDATE sys_menu
SET visible = '1',
    status = '0',
    update_by = 'admin',
    update_time = NOW(),
    remark = CONCAT(IFNULL(remark, ''), ' [v7.24 已合并到 Tab 容器，隐藏]')
WHERE menu_type = 'C'
  AND visible = '0'
  AND (
    -- 1. 按 perms（包含历史命名变体）
    perms IN ('portal:ad:list', 'cms:friend-link:list',
              'cms:help-category:list', 'cms:help-article:list',
              'cms:feedback:list', 'cms:report:list',
              'cms:growth:rule:list', 'cms:growth:achievement:list',
              'cms:growth-rule:list', 'cms:growth-achievement:list')
    -- 2. 按 path + component 联合（应对 perms 命名差异）
    OR (path IN ('ad', 'friend-link', 'help-category', 'help-article',
                 'feedback', 'report', 'growth-rule', 'growth-achievement')
        AND component LIKE 'cms/%')
  );

-- 2.3 Portal 模块（2 条）：被 transaction Tab 容器替代
--     注：91_菜单权限_CMS.sql 创建时已 visible='1'，此处兜底确保隐藏
UPDATE sys_menu
SET visible = '1',
    status = '0',
    update_by = 'admin',
    update_time = NOW(),
    remark = CONCAT(IFNULL(remark, ''), ' [v7.24 已合并到 Tab 容器，隐藏]')
WHERE menu_type = 'C'
  AND visible = '0'
  AND (
    perms IN ('portal:order:list', 'portal:tip:list')
    OR (path IN ('order', 'tip') AND component LIKE 'portal/%')
  );

-- 2.4 AI 模块疑似虚构项（dashboard-old）兜底隐藏
--     调研结论：SQL 仓库中无 path='dashboard-old' 的 INSERT 注册，
--     但若生产环境存在历史未追踪的副本，则一并隐藏
UPDATE sys_menu
SET visible = '1',
    status = '0',
    update_by = 'admin',
    update_time = NOW(),
    remark = CONCAT(IFNULL(remark, ''), ' [v7.24 已合并到 dashboard Tab 容器，隐藏]')
WHERE menu_type = 'C'
  AND visible = '0'
  AND (
    path = 'dashboard-old'
    -- 同时处理"原 AI 数据大屏"菜单：若 path 不是 dashboard 而是其他变体（如 ai-dashboard、aiDashboard），则隐藏
    OR (menu_name = 'AI数据大屏' AND perms = 'cms:ai:dashboard:list' AND path NOT IN ('dashboard', 'dashboard-old'))
  );

-- ====================================================================
-- 三、确保 Tab 容器菜单 visible='0'（显示）
-- 防止之前修复时误把容器菜单也隐藏了
-- ====================================================================
UPDATE sys_menu
SET visible = '0',
    status = '0',
    update_by = 'admin',
    update_time = NOW()
WHERE menu_type = 'C'
  AND visible != '0'
  AND perms IN (
    -- 监控 Tab 容器（3 个）
    'monitor:server-panel:list', 'monitor:cache-manage:list', 'monitor:log-audit:list',
    -- CMS Tab 容器（4 个）
    'cms:promotion:list', 'cms:feedback-center:list', 'cms:help-center:list', 'cms:growth-config:list',
    -- Portal Tab 容器（1 个，注：transaction 容器按设计保持隐藏）
    'portal:learn-aux:list'
  );

-- 注：cms:transaction:list 按设计 visible='1'（已下线），不在恢复显示范围内

-- ====================================================================
-- 四、验证修复结果
-- ====================================================================
SELECT '========== 修复后：所有旧菜单 + Tab 容器菜单 ==========' AS info;
SELECT
    menu_id, menu_name, path, component, perms, visible,
    CASE
        -- Tab 容器（应显示 visible='0'）
        WHEN perms IN ('monitor:server-panel:list', 'monitor:cache-manage:list', 'monitor:log-audit:list',
                       'cms:promotion:list', 'cms:feedback-center:list', 'cms:help-center:list',
                       'cms:growth-config:list', 'portal:learn-aux:list')
            AND visible = '0'
            THEN '✓ Tab容器（显示中）'
        WHEN perms IN ('monitor:server-panel:list', 'monitor:cache-manage:list', 'monitor:log-audit:list',
                       'cms:promotion:list', 'cms:feedback-center:list', 'cms:help-center:list',
                       'cms:growth-config:list', 'portal:learn-aux:list')
            AND visible != '0'
            THEN '⚠️ Tab容器被隐藏（异常）'
        -- 旧菜单（应隐藏 visible='1'）
        WHEN visible = '1'
            THEN '✓ 旧菜单（已隐藏）'
        WHEN visible = '0'
            THEN '⚠️ 旧菜单仍显示（异常）'
        ELSE '❓ 异常'
    END AS 状态
FROM sys_menu
WHERE menu_type = 'C'
  AND (
    -- 旧菜单定位（与上方 UPDATE 条件一致）
    menu_id IN (111, 112, 113, 114, 500, 501)
    OR perms IN ('monitor:druid:list', 'monitor:server:list',
                 'monitor:cache:list', 'monitor:operlog:list', 'monitor:logininfor:list',
                 'portal:ad:list', 'cms:friend-link:list',
                 'cms:help-category:list', 'cms:help-article:list',
                 'cms:feedback:list', 'cms:report:list',
                 'cms:growth:rule:list', 'cms:growth:achievement:list',
                 'cms:growth-rule:list', 'cms:growth-achievement:list',
                 'portal:order:list', 'portal:tip:list')
    OR path IN ('druid', 'server', 'cache', 'cacheList', 'operlog', 'logininfor',
                'ad', 'friend-link', 'help-category', 'help-article',
                'feedback', 'report', 'growth-rule', 'growth-achievement',
                'order', 'tip', 'dashboard-old')
    -- Tab 容器定位
    OR perms IN ('monitor:server-panel:list', 'monitor:cache-manage:list', 'monitor:log-audit:list',
                 'cms:promotion:list', 'cms:feedback-center:list', 'cms:help-center:list',
                 'cms:growth-config:list', 'cms:transaction:list', 'portal:learn-aux:list')
  )
ORDER BY
    CASE
        WHEN perms IN ('monitor:server-panel:list', 'monitor:cache-manage:list', 'monitor:log-audit:list',
                       'cms:promotion:list', 'cms:feedback-center:list', 'cms:help-center:list',
                       'cms:growth-config:list', 'cms:transaction:list', 'portal:learn-aux:list') THEN 0
        ELSE 1
    END,
    menu_id;

-- ====================================================================
-- 五、修复后预期菜单结构（侧边栏不再出现重复入口）
-- ====================================================================
SELECT '========== 预期结构 ==========' AS info;
SELECT '内容管理 (cms)' AS 菜单结构
UNION ALL SELECT '  ├─ 推广位管理 (promotion)         ← Tab容器（显示）'
UNION ALL SELECT '  │   ├─ 广告位 (ad, 已隐藏)            ← 旧'
UNION ALL SELECT '  │   └─ 友情链接 (friend-link, 已隐藏) ← 旧'
UNION ALL SELECT '  ├─ 用户反馈处理 (feedback-center)   ← Tab容器（显示）'
UNION ALL SELECT '  │   ├─ 反馈管理 (feedback, 已隐藏)   ← 旧'
UNION ALL SELECT '  │   └─ 举报管理 (report, 已隐藏)     ← 旧'
UNION ALL SELECT '  ├─ 帮助中心 (help-center)           ← Tab容器（显示）'
UNION ALL SELECT '  │   ├─ 帮助分类 (help-category, 已隐藏) ← 旧'
UNION ALL SELECT '  │   └─ 帮助文章 (help-article, 已隐藏) ← 旧'
UNION ALL SELECT '  ├─ 成长配置 (growth-config)         ← Tab容器（显示）'
UNION ALL SELECT '  │   ├─ 成长规则 (growth-rule, 已隐藏)  ← 旧'
UNION ALL SELECT '  │   └─ 成就管理 (growth-achievement, 已隐藏) ← 旧'
UNION ALL SELECT '  └─ 交易管理 (transaction, 已隐藏)   ← Tab容器（按设计隐藏）'
UNION ALL SELECT '      ├─ 订单 (order, 已隐藏)          ← 旧（原已隐藏）'
UNION ALL SELECT '      └─ 打赏管理 (tip, 已隐藏)        ← 旧（原已隐藏）'
UNION ALL SELECT ''
UNION ALL SELECT '系统监控 (monitor)'
UNION ALL SELECT '  ├─ 在线用户 (109, 显示)'
UNION ALL SELECT '  ├─ 定时任务 (110, 显示)'
UNION ALL SELECT '  ├─ 服务监控 (server-panel, 显示)    ← Tab容器'
UNION ALL SELECT '  │   ├─ 服务器监控 (112, 已隐藏)      ← 旧'
UNION ALL SELECT '  │   └─ 数据监控 (111, 已隐藏)        ← 旧'
UNION ALL SELECT '  └─ 缓存管理 (cache-manage, 显示)     ← Tab容器'
UNION ALL SELECT '      ├─ 缓存监控 (113, 已隐藏)         ← 旧'
UNION ALL SELECT '      └─ 缓存列表 (114, 已隐藏)         ← 旧'
UNION ALL SELECT ''
UNION ALL SELECT '系统管理 > 日志管理 (id=108)'
UNION ALL SELECT '  └─ 日志审计 (log-audit, 显示)        ← Tab容器'
UNION ALL SELECT '      ├─ 操作日志 (500, 已隐藏)         ← 旧'
UNION ALL SELECT '      └─ 登录日志 (501, 已隐藏)         ← 旧';

SELECT '========== 修复完成 ==========' AS info;
SELECT '提示：如仍看到重复菜单，请刷新浏览器（Ctrl+Shift+R 清缓存）或重新登录' AS tip;


-- =====================================================================
-- 六、校验段
-- =====================================================================
-- =============================================================================
-- 菜单完整性校验查询（v7.15 更新）
-- 用途：执行后查看输出，对比预期菜单结构，快速定位缺失/异常菜单
-- =============================================================================

-- 1. 按顶级目录分组统计菜单数量（visible=0 可见菜单）
SELECT
    p.menu_name AS 顶级目录,
    p.order_num AS 目录排序,
    COUNT(c.menu_id) AS 可见菜单数,
    SUM(CASE WHEN c.menu_type = 'C' THEN 1 ELSE 0 END) AS C菜单数,
    SUM(CASE WHEN c.menu_type = 'F' THEN 1 ELSE 0 END) AS F按钮数
FROM sys_menu p
LEFT JOIN sys_menu c ON c.parent_id = p.menu_id AND c.visible = '0' AND c.status = '0'
WHERE p.parent_id = 0 AND p.menu_type = 'M' AND p.visible = '0' AND p.status = '0'
GROUP BY p.menu_id, p.menu_name, p.order_num
ORDER BY p.order_num;

-- 2. 检查关键菜单是否已注册（缺失的菜单会显示 0）
SELECT '=== 关键菜单存在性检查 ===' AS info;
SELECT
    '敏感词管理' AS 菜单名,
    COUNT(*) AS 数量,
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行96脚本)') AS 状态
FROM sys_menu WHERE perms = 'system:sensitiveWord:list' AND menu_type = 'C'
UNION ALL
SELECT '消息中心', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行93脚本)')
FROM sys_menu WHERE perms = 'system:message:list' AND menu_type = 'C'
UNION ALL
SELECT '通知管理', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行93脚本)')
FROM sys_menu WHERE perms = 'system:notification:list' AND menu_type = 'C'
UNION ALL
SELECT '日志审计', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行100脚本)')
FROM sys_menu WHERE perms = 'monitor:log-audit:list' AND menu_type = 'C'
UNION ALL
SELECT '缓存管理', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行100脚本)')
FROM sys_menu WHERE perms = 'monitor:cache-manage:list' AND menu_type = 'C'
UNION ALL
SELECT '服务监控', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行104脚本)')
FROM sys_menu WHERE perms = 'monitor:server-panel:list' AND menu_type = 'C'
UNION ALL
SELECT '推广位管理', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行100脚本)')
FROM sys_menu WHERE perms = 'cms:promotion:list' AND menu_type = 'C'
UNION ALL
SELECT '用户反馈处理', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行100脚本)')
FROM sys_menu WHERE perms = 'cms:feedback-center:list' AND menu_type = 'C'
UNION ALL
SELECT '帮助中心', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行100脚本)')
FROM sys_menu WHERE perms = 'cms:help-center:list' AND menu_type = 'C'
UNION ALL
SELECT '成长配置', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行100脚本)')
FROM sys_menu WHERE perms = 'cms:growth-config:list' AND menu_type = 'C'
UNION ALL
SELECT '内容审核中心', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行103脚本)')
FROM sys_menu WHERE perms = 'cms:audit-center:list' AND menu_type = 'C'
UNION ALL
SELECT '面试指南目录', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行108脚本)')
FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M'
UNION ALL
SELECT '读书空间目录', COUNT(*),
    IF(COUNT(*) > 0, '✓ 已注册', '✗ 缺失(执行108脚本)')
FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M';

-- 3. 检查 Flowable 残留（应全部为 0）
SELECT '=== Flowable 残留检查（应全部为0）===' AS info;
SELECT
    'sys_menu残留' AS 检查项,
    COUNT(*) AS 数量
FROM sys_menu WHERE perms LIKE '%flow%' OR path LIKE '%flow%' OR menu_name LIKE '%flow%'
UNION ALL
SELECT 'sys_role_menu残留',
    COUNT(*)
FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.perms LIKE '%flow%' OR m.path LIKE '%flow%'
UNION ALL
SELECT 'act_表残留',
    COUNT(*)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE 'act_%';

-- 4. 检查隐藏菜单（visible=1）列表 - 确认已合并的旧菜单被正确隐藏
SELECT '=== 已隐藏菜单列表（visible=1）===' AS info;
SELECT menu_name, perms, order_num, remark
FROM sys_menu
WHERE visible = '1' AND menu_type = 'C' AND parent_id IN (
    SELECT menu_id FROM sys_menu WHERE menu_name IN ('内容管理', '系统监控', '系统管理')
)
ORDER BY parent_id, order_num;

-- 5. 完整菜单树（可视化层级，只显示 visible=0 的菜单）
SELECT '=== 完整菜单树（可见菜单）===' AS info;
SELECT
    CASE
        WHEN p.menu_type = 'M' AND p.parent_id = 0 THEN CONCAT('■ ', p.menu_name)
        ELSE CONCAT('  ├─ ', c.menu_name)
    END AS 菜单层级,
    c.perms AS 权限标识,
    c.order_num AS 排序,
    c.path AS 路由路径,
    c.component AS 组件路径
FROM sys_menu p
LEFT JOIN sys_menu c ON c.parent_id = p.menu_id AND c.visible = '0' AND c.status = '0'
WHERE p.parent_id = 0 AND p.menu_type = 'M' AND p.visible = '0' AND p.status = '0'
ORDER BY p.order_num, c.order_num;

-- 6. admin 角色(role_id=1)菜单关联数量检查
SELECT '=== 角色菜单关联检查 ===' AS info;
SELECT
    r.role_name AS 角色,
    COUNT(rm.menu_id) AS 关联菜单数
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.role_id = rm.role_id
WHERE r.role_id IN (1, 2)
GROUP BY r.role_id, r.role_name;

-- =====================================================================
-- 七、结尾设置：恢复外键检查
-- =====================================================================
-- 来源：all-db-ddl.sql 行5335
-- 用途：恢复外键检查，所有 DDL 与种子数据执行完毕后执行
SET FOREIGN_KEY_CHECKS=1;

-- =====================================================================
-- 初始化完成
-- =====================================================================
SELECT '================================================' AS info;
SELECT '墨韵智库 v7.8 数据库初始化完成' AS info;
SELECT CONCAT('完成时间: ', NOW()) AS info;
SELECT '================================================' AS info;
SELECT '后台管理: http://localhost:80  账号 admin / admin123' AS info;
SELECT '前台门户: http://localhost:5173 账号 admin / 123456' AS info;
SELECT '================================================' AS info;
