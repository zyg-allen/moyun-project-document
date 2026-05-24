-- Flowable 工作流模块相关表结构

-- sys_deploy_form definition
CREATE TABLE `sys_deploy_form`
(
    `id`        bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `form_id`   bigint(20) DEFAULT NULL COMMENT '表单主键',
    `deploy_id` varchar(50) DEFAULT NULL COMMENT '流程实例主键',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='流程实例关联表单';

-- sys_expression definition
CREATE TABLE `sys_expression`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '表单主键',
    `name`        varchar(50)  DEFAULT NULL COMMENT '表达式名称',
    `expression`  varchar(255) DEFAULT NULL COMMENT '表达式内容',
    `data_type`   varchar(255) DEFAULT NULL COMMENT '表达式类型',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    `create_by`   bigint(20) DEFAULT NULL COMMENT '创建人员',
    `update_by`   bigint(20) DEFAULT NULL COMMENT '更新人员',
    `status`      tinyint(2) DEFAULT '0' COMMENT '状态',
    `remark`      varchar(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='流程表达式';


-- sys_form definition
CREATE TABLE `sys_form`
(
    `form_id`      bigint(20) NOT NULL AUTO_INCREMENT COMMENT '表单主键',
    `form_name`    varchar(50)  DEFAULT NULL COMMENT '表单名称',
    `form_content` longtext COMMENT '表单内容',
    `create_time`  datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`  datetime     DEFAULT NULL COMMENT '更新时间',
    `create_by`    bigint(20) DEFAULT NULL COMMENT '创建人员',
    `update_by`    bigint(20) DEFAULT NULL COMMENT '更新人员',
    `remark`       varchar(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`form_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='流程表单';


-- sys_listener definition
CREATE TABLE `sys_listener`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '表单主键',
    `name`        varchar(128) DEFAULT NULL COMMENT '名称',
    `type`        varchar(64)  DEFAULT NULL COMMENT '监听类型',
    `event_type`  varchar(64)  DEFAULT NULL COMMENT '监听事件类型',
    `value_type`  varchar(64)  DEFAULT NULL COMMENT '监听值类型',
    `value`       varchar(255) DEFAULT NULL COMMENT '监听值',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    `create_by`   bigint(20) DEFAULT NULL COMMENT '创建人员',
    `update_by`   bigint(20) DEFAULT NULL COMMENT '更新人员',
    `status`      tinyint(2) DEFAULT '0' COMMENT '状态',
    `remark`      varchar(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='流程监听';

-- 流程菜单数据
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES('流程管理', 0, 10, 'flowable', NULL, 1, 0, 'M', '0', '0', '', 'tree', 'admin', NOW(), '流程管理目录');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES('流程定义', (SELECT menu_id FROM sys_menu WHERE menu_name = '流程管理' LIMIT 1), 1, 'definition', 'flowable/definition/index', 1, 0, 'C', '0', '0', 'flowable:definition:list', 'tree', 'admin', NOW(), '流程定义菜单');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES('流程任务', (SELECT menu_id FROM sys_menu WHERE menu_name = '流程管理' LIMIT 1), 2, 'task', 'flowable/task/index', 1, 0, 'C', '0', '0', 'flowable:task:list', 'tree', 'admin', NOW(), '流程任务菜单');

-- 流程相关字典类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES('表达式类型', 'exp_data_type', '0', 'admin', NOW(), '表达式类型');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES('监听类型', 'sys_listener_type', '0', 'admin', NOW(), '监听类型');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES('监听值类型', 'sys_listener_value_type', '0', 'admin', NOW(), '监听值类型');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES('监听属性', 'sys_listener_event_type', '0', 'admin', NOW(), '监听属性');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES('流程分类', 'sys_process_category', '0', 'admin', NOW(), '流程分类');

-- 流程相关字典数据
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES(0, '系统指定', 'fixed', 'exp_data_type', NULL, 'default', 'N', '0', 'admin', NOW(), NULL);
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES(1, '动态选择', 'dynamic', 'exp_data_type', NULL, 'default', 'N', '0', 'admin', NOW(), NULL);
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES(0, '任务监听', '1', 'sys_listener_type', NULL, 'default', 'N', '0', 'admin', NOW(), NULL);
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES(2, '执行监听', '2', 'sys_listener_type', NULL, 'default', 'N', '0', 'admin', NOW(), NULL);
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES(0, 'JAVA类', 'classListener', 'sys_listener_value_type', NULL, 'default', 'N', '0', 'admin', NOW(), NULL);
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES(1, '表达式', 'expressionListener', 'sys_listener_value_type', NULL, 'default', 'N', '0', 'admin', NOW(), NULL);
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES(2, '代理表达式', 'delegateExpressionListener', 'sys_listener_value_type', NULL, 'default', 'N', '0', 'admin', NOW(), NULL);
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES(0, '请假', 'leave', 'sys_process_category', NULL, 'default', 'N', '0', 'admin', NOW(), NULL);
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES(1, '报销', 'expense', 'sys_process_category', NULL, 'default', 'N', '0', 'admin', NOW(), NULL);