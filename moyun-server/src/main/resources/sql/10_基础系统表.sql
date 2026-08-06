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


-- Table structure for table `sys_deploy_form`


DROP TABLE IF EXISTS `sys_deploy_form`;
CREATE TABLE `sys_deploy_form` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `form_id` bigint DEFAULT NULL COMMENT '表单主键',
                                   `deploy_id` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程实例主键',
                                   `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                   `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                   `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                   `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程实例关联表单';


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


-- Table structure for table `sys_expression`


DROP TABLE IF EXISTS `sys_expression`;
CREATE TABLE `sys_expression` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '表单主键',
                                  `name` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表达式名称',
                                  `expression` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表达式内容',
                                  `data_type` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表达式类型',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                  `create_by` bigint DEFAULT NULL COMMENT '创建人员',
                                  `update_by` bigint DEFAULT NULL COMMENT '更新人员',
                                  `status` tinyint DEFAULT '0' COMMENT '状态',
                                  `remark` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                  `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表达式';


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


-- Table structure for table `sys_form`


DROP TABLE IF EXISTS `sys_form`;
CREATE TABLE `sys_form` (
                            `form_id` bigint NOT NULL AUTO_INCREMENT COMMENT '表单主键',
                            `form_name` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表单名称',
                            `form_content` longtext COLLATE utf8mb4_0900_ai_ci COMMENT '表单内容',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `create_by` bigint DEFAULT NULL COMMENT '创建人员',
                            `update_by` bigint DEFAULT NULL COMMENT '更新人员',
                            `remark` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                            `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                            PRIMARY KEY (`form_id`),
                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表单';


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


-- Table structure for table `sys_listener`


DROP TABLE IF EXISTS `sys_listener`;
CREATE TABLE `sys_listener` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '表单主键',
                                `name` varchar(128) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '名称',
                                `type` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '监听类型',
                                `event_type` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '监听事件类型',
                                `value_type` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '监听值类型',
                                `value` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '监听值',
                                `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                `create_by` bigint DEFAULT NULL COMMENT '创建人员',
                                `update_by` bigint DEFAULT NULL COMMENT '更新人员',
                                `status` tinyint DEFAULT '0' COMMENT '状态',
                                `remark` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                PRIMARY KEY (`id`),
                                KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程监听';


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

