-- ============================================
-- 墨韵智库系统数据库初始化脚本
-- 版本：1.0
-- 执行顺序: 01 (最先执行)
-- 说明：包含系统管理、定时任务、代码生成等基础功能表
-- ============================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1、部门表
-- ============================================
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
  dept_id           bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '部门id',
  parent_id         bigint(20)      DEFAULT 0                 COMMENT '父部门id',
  ancestors         varchar(50)     DEFAULT ''                COMMENT '祖级列表',
  dept_name         varchar(30)     DEFAULT ''                COMMENT '部门名称',
  order_num         int(4)          DEFAULT 0                 COMMENT '显示顺序',
  leader            varchar(20)     DEFAULT NULL              COMMENT '负责人',
  phone             varchar(11)     DEFAULT NULL              COMMENT '联系电话',
  email             varchar(50)     DEFAULT NULL              COMMENT '邮箱',
  status            char(1)         DEFAULT '0'               COMMENT '部门状态（0正常 1停用）',
  del_flag          char(1)         DEFAULT '0'               COMMENT '删除标志（0代表存在 2代表删除）',
  create_by         varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time       datetime                                COMMENT '创建时间',
  update_by         varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time       datetime                                COMMENT '更新时间',
  remark            varchar(500)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (dept_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- 初始化-部门表数据
INSERT INTO sys_dept VALUES(100,  0,   '0',          '若依科技',   0, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT INTO sys_dept VALUES(101,  100, '0,100',      '深圳总公司', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT INTO sys_dept VALUES(102,  100, '0,100',      '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT INTO sys_dept VALUES(103,  101, '0,100,101',  '研发部门',   1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT INTO sys_dept VALUES(104,  101, '0,100,101',  '市场部门',   2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT INTO sys_dept VALUES(105,  101, '0,100,101',  '测试部门',   3, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT INTO sys_dept VALUES(106,  101, '0,100,101',  '财务部门',   4, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT INTO sys_dept VALUES(107,  101, '0,100,101',  '运维部门',   5, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT INTO sys_dept VALUES(108,  102, '0,100,102',  '市场部门',   1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT INTO sys_dept VALUES(109,  102, '0,100,102',  '财务部门',   2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', NOW(), '', NULL);


-- ============================================
-- 2、用户信息表
-- ============================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  user_id           bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  dept_id           bigint(20)      DEFAULT NULL              COMMENT '部门ID',
  user_name         varchar(30)     NOT NULL                COMMENT '用户账号',
  nick_name         varchar(30)     NOT NULL                COMMENT '用户昵称',
  user_type         varchar(2)      DEFAULT '00'            COMMENT '用户类型（00系统用户）',
  email             varchar(50)     DEFAULT ''                COMMENT '用户邮箱',
  phonenumber       varchar(11)     DEFAULT ''                COMMENT '手机号码',
  sex               char(1)         DEFAULT '0'               COMMENT '用户性别（0男 1女 2未知）',
  avatar            varchar(100)    DEFAULT ''                COMMENT '头像地址',
  password          varchar(100)    DEFAULT ''                COMMENT '密码',
  status            char(1)         DEFAULT '0'               COMMENT '账号状态（0正常 1停用）',
  del_flag          char(1)         DEFAULT '0'               COMMENT '删除标志（0代表存在 2代表删除）',
  login_ip          varchar(128)    DEFAULT ''                COMMENT '最后登录IP',
  login_date        datetime                                COMMENT '最后登录时间',
  create_by         varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time       datetime                                COMMENT '创建时间',
  update_by         varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time       datetime                                COMMENT '更新时间',
  remark            varchar(500)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (user_id)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- 初始化-用户信息表数据
INSERT INTO sys_user VALUES(1,  103, 'admin', '若依', '00', 'ry@163.com', '15888888888', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '', NULL, '管理员');
INSERT INTO sys_user VALUES(2,  105, 'ry',    '若依', '00', 'ry@qq.com',  '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '', NULL, '测试员');


-- ============================================
-- 3、岗位信息表
-- ============================================
DROP TABLE IF EXISTS sys_post;
CREATE TABLE sys_post (
  post_id       bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  post_code     varchar(64)     NOT NULL                COMMENT '岗位编码',
  post_name     varchar(50)     NOT NULL                COMMENT '岗位名称',
  post_sort     int(4)          NOT NULL                COMMENT '显示顺序',
  status        char(1)         NOT NULL                COMMENT '状态（0正常 1停用）',
  create_by     varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time   datetime                                COMMENT '创建时间',
  update_by     varchar(64)     DEFAULT ''               COMMENT '更新者',
  update_time   datetime                                COMMENT '更新时间',
  remark        varchar(500)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位信息表';

-- 初始化-岗位信息表数据
INSERT INTO sys_post VALUES(1, 'ceo',  '董事长',    1, '0', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_post VALUES(2, 'se',   '项目经理',  2, '0', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_post VALUES(3, 'hr',   '人力资源',  3, '0', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_post VALUES(4, 'user', '普通员工',  4, '0', 'admin', NOW(), '', NULL, '');


-- ============================================
-- 4、角色信息表
-- ============================================
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  role_id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_name            varchar(30)     NOT NULL                COMMENT '角色名称',
  role_key             varchar(100)    NOT NULL                COMMENT '角色权限字符串',
  role_sort            int(4)          NOT NULL                COMMENT '显示顺序',
  data_scope           char(1)         DEFAULT '1'               COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  menu_check_strictly  tinyint(1)      DEFAULT 1                 COMMENT '菜单树选择项是否关联显示',
  dept_check_strictly  tinyint(1)      DEFAULT 1                 COMMENT '部门树选择项是否关联显示',
  status               char(1)         NOT NULL                COMMENT '角色状态（0正常 1停用）',
  del_flag             char(1)         DEFAULT '0'               COMMENT '删除标志（0代表存在 2代表删除）',
  create_by            varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time          datetime                                COMMENT '创建时间',
  update_by            varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time          datetime                                COMMENT '更新时间',
  remark               varchar(500)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (role_id)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';

-- 初始化-角色信息表数据
INSERT INTO sys_role VALUES('1', '超级管理员',  'admin',  1, 1, 1, 1, '0', '0', 'admin', NOW(), '', NULL, '超级管理员');
INSERT INTO sys_role VALUES('2', '普通角色',    'common', 2, 2, 1, 1, '0', '0', 'admin', NOW(), '', NULL, '普通角色');


-- ============================================
-- 5、菜单权限表
-- ============================================
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
  menu_id           bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  menu_name         varchar(50)     NOT NULL                COMMENT '菜单名称',
  parent_id         bigint(20)      DEFAULT 0                 COMMENT '父菜单ID',
  order_num         int(4)          DEFAULT 0                 COMMENT '显示顺序',
  path              varchar(200)    DEFAULT ''                COMMENT '路由地址',
  component         varchar(255)    DEFAULT NULL              COMMENT '组件路径',
  query             varchar(255)    DEFAULT NULL              COMMENT '路由参数',
  route_name        varchar(50)     DEFAULT ''                COMMENT '路由名称',
  is_frame          int(1)          DEFAULT 1                 COMMENT '是否为外链（0是 1否）',
  is_cache          int(1)          DEFAULT 0                 COMMENT '是否缓存（0缓存 1不缓存）',
  menu_type         char(1)         DEFAULT ''                COMMENT '菜单类型（M目录 C菜单 F按钮）',
  visible           char(1)         DEFAULT 0                 COMMENT '菜单状态（0显示 1隐藏）',
  status            char(1)         DEFAULT 0                 COMMENT '菜单状态（0正常 1停用）',
  perms             varchar(100)    DEFAULT NULL              COMMENT '权限标识',
  icon              varchar(100)    DEFAULT '#'               COMMENT '菜单图标',
  create_by         varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time       datetime                                COMMENT '创建时间',
  update_by         varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time       datetime                                COMMENT '更新时间',
  remark            varchar(500)    DEFAULT ''                COMMENT '备注',
  PRIMARY KEY (menu_id)
) ENGINE=InnoDB AUTO_INCREMENT=2000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

-- 初始化-菜单信息表数据
-- 一级菜单
INSERT INTO sys_menu VALUES('1', '系统管理', '0', '1', 'system',           NULL, '', '', 1, 0, 'M', '0', '0', '', 'system',   'admin', NOW(), '', NULL, '系统管理目录');
INSERT INTO sys_menu VALUES('2', '系统监控', '0', '2', 'monitor',          NULL, '', '', 1, 0, 'M', '0', '0', '', 'monitor',  'admin', NOW(), '', NULL, '系统监控目录');
INSERT INTO sys_menu VALUES('3', '系统工具', '0', '3', 'tool',             NULL, '', '', 1, 0, 'M', '0', '0', '', 'tool',     'admin', NOW(), '', NULL, '系统工具目录');
INSERT INTO sys_menu VALUES('4', '流程管理', '0', '4', 'flowable',         NULL, '', '', 1, 0, 'M', '0', '0', '', 'tree',     'admin', NOW(), '', NULL, '流程管理目录');
-- 二级菜单
INSERT INTO sys_menu VALUES('100',  '用户管理', '1',   '1', 'user',       'system/user/index',        '', '', 1, 0, 'C', '0', '0', 'system:user:list',        'user',          'admin', NOW(), '', NULL, '用户管理菜单');
INSERT INTO sys_menu VALUES('101',  '角色管理', '1',   '2', 'role',       'system/role/index',        '', '', 1, 0, 'C', '0', '0', 'system:role:list',        'peoples',       'admin', NOW(), '', NULL, '角色管理菜单');
INSERT INTO sys_menu VALUES('102',  '菜单管理', '1',   '3', 'menu',       'system/menu/index',        '', '', 1, 0, 'C', '0', '0', 'system:menu:list',        'tree-table',    'admin', NOW(), '', NULL, '菜单管理菜单');
INSERT INTO sys_menu VALUES('103',  '部门管理', '1',   '4', 'dept',       'system/dept/index',        '', '', 1, 0, 'C', '0', '0', 'system:dept:list',        'tree',          'admin', NOW(), '', NULL, '部门管理菜单');
INSERT INTO sys_menu VALUES('104',  '岗位管理', '1',   '5', 'post',       'system/post/index',        '', '', 1, 0, 'C', '0', '0', 'system:post:list',        'post',          'admin', NOW(), '', NULL, '岗位管理菜单');
INSERT INTO sys_menu VALUES('105',  '字典管理', '1',   '6', 'dict',       'system/dict/index',        '', '', 1, 0, 'C', '0', '0', 'system:dict:list',        'dict',          'admin', NOW(), '', NULL, '字典管理菜单');
INSERT INTO sys_menu VALUES('106',  '参数设置', '1',   '7', 'config',     'system/config/index',      '', '', 1, 0, 'C', '0', '0', 'system:config:list',      'edit',          'admin', NOW(), '', NULL, '参数设置菜单');
INSERT INTO sys_menu VALUES('108',  '日志管理', '1',   '9', 'log',        '',                         '', '', 1, 0, 'M', '0', '0', '',                        'log',           'admin', NOW(), '', NULL, '日志管理菜单');
INSERT INTO sys_menu VALUES('109',  '在线用户', '2',   '1', 'online',     'monitor/online/index',     '', '', 1, 0, 'C', '0', '0', 'monitor:online:list',     'online',        'admin', NOW(), '', NULL, '在线用户菜单');
INSERT INTO sys_menu VALUES('110',  '定时任务', '2',   '2', 'job',        'monitor/job/index',        '', '', 1, 0, 'C', '0', '0', 'monitor:job:list',        'job',           'admin', NOW(), '', NULL, '定时任务菜单');
INSERT INTO sys_menu VALUES('111',  '数据监控', '2',   '3', 'druid',      'monitor/druid/index',      '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list',      'druid',         'admin', NOW(), '', NULL, '数据监控菜单');
INSERT INTO sys_menu VALUES('112',  '服务监控', '2',   '4', 'server',     'monitor/server/index',     '', '', 1, 0, 'C', '0', '0', 'monitor:server:list',     'server',        'admin', NOW(), '', NULL, '服务监控菜单');
INSERT INTO sys_menu VALUES('113',  '缓存监控', '2',   '5', 'cache',      'monitor/cache/index',      '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',      'redis',         'admin', NOW(), '', NULL, '缓存监控菜单');
INSERT INTO sys_menu VALUES('114',  '缓存列表', '2',   '6', 'cacheList',  'monitor/cache/list',       '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',      'redis-list',    'admin', NOW(), '', NULL, '缓存列表菜单');
INSERT INTO sys_menu VALUES('115',  '表单构建', '3',   '1', 'build',      'tool/build/index',         '', '', 1, 0, 'C', '0', '0', 'tool:build:list',         'build',         'admin', NOW(), '', NULL, '表单构建菜单');
INSERT INTO sys_menu VALUES('116',  '代码生成', '3',   '2', 'gen',        'tool/gen/index',           '', '', 1, 0, 'C', '0', '0', 'tool:gen:list',           'code',          'admin', NOW(), '', NULL, '代码生成菜单');
INSERT INTO sys_menu VALUES('117',  '系统接口', '3',   '3', 'swagger',    'tool/swagger/index',       '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list',       'swagger',       'admin', NOW(), '', NULL, '系统接口菜单');
INSERT INTO sys_menu VALUES('118',  '流程定义', '4',   '1', 'definition', 'flowable/definition/index', '', '', 1, 0, 'C', '0', '0', 'flowable:definition:list', 'tree',          'admin', NOW(), '', NULL, '流程定义菜单');
INSERT INTO sys_menu VALUES('119',  '流程任务', '4',   '2', 'task',       'flowable/task/index',      '', '', 1, 0, 'C', '0', '0', 'flowable:task:list',      'tree',          'admin', NOW(), '', NULL, '流程任务菜单');
-- 三级菜单
INSERT INTO sys_menu VALUES('500',  '操作日志', '108', '1', 'operlog',    'monitor/operlog/index',    '', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list',    'form',          'admin', NOW(), '', NULL, '操作日志菜单');
INSERT INTO sys_menu VALUES('501',  '登录日志', '108', '2', 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor',    'admin', NOW(), '', NULL, '登录日志菜单');
-- 用户管理按钮
INSERT INTO sys_menu VALUES('1000', '用户查询', '100', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query',          '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1001', '用户新增', '100', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add',            '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1002', '用户修改', '100', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit',           '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1003', '用户删除', '100', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove',         '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1004', '用户导出', '100', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export',         '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1005', '用户导入', '100', '6',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import',         '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1006', '重置密码', '100', '7',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd',       '#', 'admin', NOW(), '', NULL, '');
-- 角色管理按钮
INSERT INTO sys_menu VALUES('1007', '角色查询', '101', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query',          '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1008', '角色新增', '101', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add',            '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1009', '角色修改', '101', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit',           '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1010', '角色删除', '101', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove',         '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1011', '角色导出', '101', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export',         '#', 'admin', NOW(), '', NULL, '');
-- 菜单管理按钮
INSERT INTO sys_menu VALUES('1012', '菜单查询', '102', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query',          '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1013', '菜单新增', '102', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add',            '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1014', '菜单修改', '102', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit',           '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1015', '菜单删除', '102', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove',         '#', 'admin', NOW(), '', NULL, '');
-- 部门管理按钮
INSERT INTO sys_menu VALUES('1016', '部门查询', '103', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query',          '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1017', '部门新增', '103', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add',            '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1018', '部门修改', '103', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit',           '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1019', '部门删除', '103', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove',         '#', 'admin', NOW(), '', NULL, '');
-- 岗位管理按钮
INSERT INTO sys_menu VALUES('1020', '岗位查询', '104', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:query',          '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1021', '岗位新增', '104', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:add',            '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1022', '岗位修改', '104', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit',           '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1023', '岗位删除', '104', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove',         '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1024', '岗位导出', '104', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:export',         '#', 'admin', NOW(), '', NULL, '');
-- 字典管理按钮
INSERT INTO sys_menu VALUES('1025', '字典查询', '105', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query',          '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1026', '字典新增', '105', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add',            '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1027', '字典修改', '105', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit',           '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1028', '字典删除', '105', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove',         '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1029', '字典导出', '105', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:export',         '#', 'admin', NOW(), '', NULL, '');
-- 参数设置按钮
INSERT INTO sys_menu VALUES('1030', '参数查询', '106', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:query',        '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1031', '参数新增', '106', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:add',          '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1032', '参数修改', '106', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:edit',         '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1033', '参数删除', '106', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:remove',       '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1034', '参数导出', '106', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:export',       '#', 'admin', NOW(), '', NULL, '');
-- 操作日志按钮
INSERT INTO sys_menu VALUES('1039', '操作查询', '500', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query',      '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1040', '操作删除', '500', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove',     '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1041', '日志导出', '500', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export',     '#', 'admin', NOW(), '', NULL, '');
-- 登录日志按钮
INSERT INTO sys_menu VALUES('1042', '登录查询', '501', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query',   '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1043', '登录删除', '501', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove',  '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1044', '日志导出', '501', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export',  '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1045', '账号解锁', '501', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock',  '#', 'admin', NOW(), '', NULL, '');
-- 在线用户按钮
INSERT INTO sys_menu VALUES('1046', '在线查询', '109', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query',       '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1047', '批量强退', '109', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1048', '单条强退', '109', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', NOW(), '', NULL, '');
-- 定时任务按钮
INSERT INTO sys_menu VALUES('1049', '任务查询', '110', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query',          '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1050', '任务新增', '110', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add',            '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1051', '任务修改', '110', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit',           '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1052', '任务删除', '110', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove',         '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1053', '状态修改', '110', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus',   '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1054', '任务导出', '110', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export',         '#', 'admin', NOW(), '', NULL, '');
-- 代码生成按钮
INSERT INTO sys_menu VALUES('1055', '生成查询', '116', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query',             '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1056', '生成修改', '116', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit',              '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1057', '生成删除', '116', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove',            '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1058', '导入代码', '116', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import',            '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1059', '预览代码', '116', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview',           '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('1060', '生成代码', '116', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code',              '#', 'admin', NOW(), '', NULL, '');


-- ============================================
-- 6、用户和角色关联表 用户N-1角色
-- ============================================
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
  user_id   bigint(20) NOT NULL COMMENT '用户ID',
  role_id   bigint(20) NOT NULL COMMENT '角色ID',
  create_by   varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime COMMENT '创建时间',
  update_by   varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime COMMENT '更新时间',
  remark      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY(user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';

-- 初始化-用户和角色关联表数据
INSERT INTO sys_user_role VALUES ('1', '1');
INSERT INTO sys_user_role VALUES ('2', '2');


-- ============================================
-- 7、角色和菜单关联表 角色1-N菜单
-- ============================================
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
  role_id   bigint(20) NOT NULL COMMENT '角色ID',
  menu_id   bigint(20) NOT NULL COMMENT '菜单ID',
  create_by   varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime COMMENT '创建时间',
  update_by   varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime COMMENT '更新时间',
  remark      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY(role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和菜单关联表';

-- 初始化-角色和菜单关联表数据
INSERT INTO sys_role_menu VALUES ('2', '1');
INSERT INTO sys_role_menu VALUES ('2', '2');
INSERT INTO sys_role_menu VALUES ('2', '3');
INSERT INTO sys_role_menu VALUES ('2', '4');
INSERT INTO sys_role_menu VALUES ('2', '100');
INSERT INTO sys_role_menu VALUES ('2', '101');
INSERT INTO sys_role_menu VALUES ('2', '102');
INSERT INTO sys_role_menu VALUES ('2', '103');
INSERT INTO sys_role_menu VALUES ('2', '104');
INSERT INTO sys_role_menu VALUES ('2', '105');
INSERT INTO sys_role_menu VALUES ('2', '106');
INSERT INTO sys_role_menu VALUES ('2', '108');
INSERT INTO sys_role_menu VALUES ('2', '109');
INSERT INTO sys_role_menu VALUES ('2', '110');
INSERT INTO sys_role_menu VALUES ('2', '111');
INSERT INTO sys_role_menu VALUES ('2', '112');
INSERT INTO sys_role_menu VALUES ('2', '113');
INSERT INTO sys_role_menu VALUES ('2', '114');
INSERT INTO sys_role_menu VALUES ('2', '115');
INSERT INTO sys_role_menu VALUES ('2', '116');
INSERT INTO sys_role_menu VALUES ('2', '117');
INSERT INTO sys_role_menu VALUES ('2', '118');
INSERT INTO sys_role_menu VALUES ('2', '119');
INSERT INTO sys_role_menu VALUES ('2', '500');
INSERT INTO sys_role_menu VALUES ('2', '501');
INSERT INTO sys_role_menu VALUES ('2', '1000');
INSERT INTO sys_role_menu VALUES ('2', '1001');
INSERT INTO sys_role_menu VALUES ('2', '1002');
INSERT INTO sys_role_menu VALUES ('2', '1003');
INSERT INTO sys_role_menu VALUES ('2', '1004');
INSERT INTO sys_role_menu VALUES ('2', '1005');
INSERT INTO sys_role_menu VALUES ('2', '1006');
INSERT INTO sys_role_menu VALUES ('2', '1007');
INSERT INTO sys_role_menu VALUES ('2', '1008');
INSERT INTO sys_role_menu VALUES ('2', '1009');
INSERT INTO sys_role_menu VALUES ('2', '1010');
INSERT INTO sys_role_menu VALUES ('2', '1011');
INSERT INTO sys_role_menu VALUES ('2', '1012');
INSERT INTO sys_role_menu VALUES ('2', '1013');
INSERT INTO sys_role_menu VALUES ('2', '1014');
INSERT INTO sys_role_menu VALUES ('2', '1015');
INSERT INTO sys_role_menu VALUES ('2', '1016');
INSERT INTO sys_role_menu VALUES ('2', '1017');
INSERT INTO sys_role_menu VALUES ('2', '1018');
INSERT INTO sys_role_menu VALUES ('2', '1019');
INSERT INTO sys_role_menu VALUES ('2', '1020');
INSERT INTO sys_role_menu VALUES ('2', '1021');
INSERT INTO sys_role_menu VALUES ('2', '1022');
INSERT INTO sys_role_menu VALUES ('2', '1023');
INSERT INTO sys_role_menu VALUES ('2', '1024');
INSERT INTO sys_role_menu VALUES ('2', '1025');
INSERT INTO sys_role_menu VALUES ('2', '1026');
INSERT INTO sys_role_menu VALUES ('2', '1027');
INSERT INTO sys_role_menu VALUES ('2', '1028');
INSERT INTO sys_role_menu VALUES ('2', '1029');
INSERT INTO sys_role_menu VALUES ('2', '1030');
INSERT INTO sys_role_menu VALUES ('2', '1031');
INSERT INTO sys_role_menu VALUES ('2', '1032');
INSERT INTO sys_role_menu VALUES ('2', '1033');
INSERT INTO sys_role_menu VALUES ('2', '1034');
INSERT INTO sys_role_menu VALUES ('2', '1039');
INSERT INTO sys_role_menu VALUES ('2', '1040');
INSERT INTO sys_role_menu VALUES ('2', '1041');
INSERT INTO sys_role_menu VALUES ('2', '1042');
INSERT INTO sys_role_menu VALUES ('2', '1043');
INSERT INTO sys_role_menu VALUES ('2', '1044');
INSERT INTO sys_role_menu VALUES ('2', '1045');
INSERT INTO sys_role_menu VALUES ('2', '1046');
INSERT INTO sys_role_menu VALUES ('2', '1047');
INSERT INTO sys_role_menu VALUES ('2', '1048');
INSERT INTO sys_role_menu VALUES ('2', '1049');
INSERT INTO sys_role_menu VALUES ('2', '1050');
INSERT INTO sys_role_menu VALUES ('2', '1051');
INSERT INTO sys_role_menu VALUES ('2', '1052');
INSERT INTO sys_role_menu VALUES ('2', '1053');
INSERT INTO sys_role_menu VALUES ('2', '1054');
INSERT INTO sys_role_menu VALUES ('2', '1055');
INSERT INTO sys_role_menu VALUES ('2', '1056');
INSERT INTO sys_role_menu VALUES ('2', '1057');
INSERT INTO sys_role_menu VALUES ('2', '1058');
INSERT INTO sys_role_menu VALUES ('2', '1059');
INSERT INTO sys_role_menu VALUES ('2', '1060');


-- ============================================
-- 8、角色和部门关联表 角色1-N部门
-- ============================================
DROP TABLE IF EXISTS sys_role_dept;
CREATE TABLE sys_role_dept (
  role_id   bigint(20) NOT NULL COMMENT '角色ID',
  dept_id   bigint(20) NOT NULL COMMENT '部门ID',
  create_by   varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime COMMENT '创建时间',
  update_by   varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime COMMENT '更新时间',
  remark      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY(role_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和部门关联表';

-- 初始化-角色和部门关联表数据
INSERT INTO sys_role_dept VALUES ('2', '100');
INSERT INTO sys_role_dept VALUES ('2', '101');
INSERT INTO sys_role_dept VALUES ('2', '105');


-- ============================================
-- 9、用户与岗位关联表 用户1-N岗位
-- ============================================
DROP TABLE IF EXISTS sys_user_post;
CREATE TABLE sys_user_post (
  user_id   bigint(20) NOT NULL COMMENT '用户ID',
  post_id   bigint(20) NOT NULL COMMENT '岗位ID',
  create_by   varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime COMMENT '创建时间',
  update_by   varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime COMMENT '更新时间',
  remark      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与岗位关联表';

-- 初始化-用户与岗位关联表数据
INSERT INTO sys_user_post VALUES ('1', '1');
INSERT INTO sys_user_post VALUES ('2', '2');


-- ============================================
-- 10、操作日志记录
-- ============================================
DROP TABLE IF EXISTS sys_oper_log;
CREATE TABLE sys_oper_log (
  oper_id           bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  title             varchar(50)     DEFAULT ''                COMMENT '模块标题',
  business_type     int(2)          DEFAULT 0                 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  method            varchar(200)    DEFAULT ''                COMMENT '方法名称',
  request_method    varchar(10)     DEFAULT ''                COMMENT '请求方式',
  operator_type     int(1)          DEFAULT 0                 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  oper_name         varchar(50)     DEFAULT ''                COMMENT '操作人员',
  dept_name         varchar(50)     DEFAULT ''                COMMENT '部门名称',
  oper_url          varchar(255)    DEFAULT ''                COMMENT '请求URL',
  oper_ip           varchar(128)    DEFAULT ''                COMMENT '主机地址',
  oper_location     varchar(255)    DEFAULT ''                COMMENT '操作地点',
  oper_param        varchar(2000)   DEFAULT ''                COMMENT '请求参数',
  json_result       varchar(2000)   DEFAULT ''                COMMENT '返回参数',
  status            int(1)          DEFAULT 0                 COMMENT '操作状态（0正常 1异常）',
  error_msg         varchar(2000)   DEFAULT ''                COMMENT '错误消息',
  oper_time         datetime                                COMMENT '操作时间',
  cost_time         bigint(20)      DEFAULT 0                 COMMENT '消耗时间',
  create_by         varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time       datetime                                  COMMENT '创建时间',
  update_by         varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time       datetime                                  COMMENT '更新时间',
  remark            varchar(500)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (oper_id),
  KEY idx_sys_oper_log_bt (business_type),
  KEY idx_sys_oper_log_s (status),
  KEY idx_sys_oper_log_ot (oper_time)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志记录';


-- ============================================
-- 11、字典类型表
-- ============================================
DROP TABLE IF EXISTS sys_dict_type;
CREATE TABLE sys_dict_type (
  dict_id          bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  dict_name        varchar(100)    DEFAULT ''                COMMENT '字典名称',
  dict_type        varchar(100)    DEFAULT ''                COMMENT '字典类型',
  status           char(1)         DEFAULT '0'               COMMENT '状态（0正常 1停用）',
  create_by        varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time      datetime                                COMMENT '创建时间',
  update_by        varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time      datetime                                COMMENT '更新时间',
  remark           varchar(500)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (dict_id),
  UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

INSERT INTO sys_dict_type VALUES(1,  '用户性别', 'sys_user_sex',        '0', 'admin', NOW(), '', NULL, '用户性别列表');
INSERT INTO sys_dict_type VALUES(2,  '菜单状态', 'sys_show_hide',       '0', 'admin', NOW(), '', NULL, '菜单状态列表');
INSERT INTO sys_dict_type VALUES(3,  '系统开关', 'sys_normal_disable',  '0', 'admin', NOW(), '', NULL, '系统开关列表');
INSERT INTO sys_dict_type VALUES(4,  '任务状态', 'sys_job_status',      '0', 'admin', NOW(), '', NULL, '任务状态列表');
INSERT INTO sys_dict_type VALUES(5,  '任务分组', 'sys_job_group',       '0', 'admin', NOW(), '', NULL, '任务分组列表');
INSERT INTO sys_dict_type VALUES(6,  '系统是否', 'sys_yes_no',          '0', 'admin', NOW(), '', NULL, '系统是否列表');
INSERT INTO sys_dict_type VALUES(7,  '通知类型', 'sys_notice_type',     '0', 'admin', NOW(), '', NULL, '通知类型列表');
INSERT INTO sys_dict_type VALUES(8,  '通知状态', 'sys_notice_status',   '0', 'admin', NOW(), '', NULL, '通知状态列表');
INSERT INTO sys_dict_type VALUES(9,  '操作类型', 'sys_oper_type',       '0', 'admin', NOW(), '', NULL, '操作类型列表');
INSERT INTO sys_dict_type VALUES(10, '系统状态', 'sys_common_status',   '0', 'admin', NOW(), '', NULL, '登录状态列表');
INSERT INTO sys_dict_type VALUES(11, '表达式类型', 'exp_data_type',       '0', 'admin', NOW(), '', NULL, '表达式类型');
INSERT INTO sys_dict_type VALUES(12, '监听类型', 'sys_listener_type',    '0', 'admin', NOW(), '', NULL, '监听类型');
INSERT INTO sys_dict_type VALUES(13, '监听值类型', 'sys_listener_value_type', '0', 'admin', NOW(), '', NULL, '监听值类型');
INSERT INTO sys_dict_type VALUES(14, '监听属性', 'sys_listener_event_type', '0', 'admin', NOW(), '', NULL, '监听属性');
INSERT INTO sys_dict_type VALUES(15, '流程分类', 'sys_process_category', '0', 'admin', NOW(), '', NULL, '流程分类');


-- ============================================
-- 12、字典数据表
-- ============================================
DROP TABLE IF EXISTS sys_dict_data;
CREATE TABLE sys_dict_data (
  dict_code        bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  dict_sort        int(4)          DEFAULT 0                 COMMENT '字典排序',
  dict_label       varchar(100)    DEFAULT ''                COMMENT '字典标签',
  dict_value       varchar(100)    DEFAULT ''                COMMENT '字典键值',
  dict_type        varchar(100)    DEFAULT ''                COMMENT '字典类型',
  css_class        varchar(100)    DEFAULT NULL              COMMENT '样式属性（其他样式扩展）',
  list_class       varchar(100)    DEFAULT NULL              COMMENT '表格回显样式',
  is_default       char(1)         DEFAULT 'N'               COMMENT '是否默认（Y是 N否）',
  status           char(1)         DEFAULT '0'               COMMENT '状态（0正常 1停用）',
  create_by        varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time      datetime                                COMMENT '创建时间',
  update_by        varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time      datetime                                COMMENT '更新时间',
  remark           varchar(500)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (dict_code)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

INSERT INTO sys_dict_data VALUES(1,  1,  '男',       '0',       'sys_user_sex',        '',   '',        'Y', '0', 'admin', NOW(), '', NULL, '性别男');
INSERT INTO sys_dict_data VALUES(2,  2,  '女',       '1',       'sys_user_sex',        '',   '',        'N', '0', 'admin', NOW(), '', NULL, '性别女');
INSERT INTO sys_dict_data VALUES(3,  3,  '未知',     '2',       'sys_user_sex',        '',   '',        'N', '0', 'admin', NOW(), '', NULL, '性别未知');
INSERT INTO sys_dict_data VALUES(4,  1,  '显示',     '0',       'sys_show_hide',       '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '显示菜单');
INSERT INTO sys_dict_data VALUES(5,  2,  '隐藏',     '1',       'sys_show_hide',       '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '隐藏菜单');
INSERT INTO sys_dict_data VALUES(6,  1,  '正常',     '0',       'sys_normal_disable',  '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '正常状态');
INSERT INTO sys_dict_data VALUES(7,  2,  '停用',     '1',       'sys_normal_disable',  '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '停用状态');
INSERT INTO sys_dict_data VALUES(8,  1,  '正常',     '0',       'sys_job_status',      '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '正常状态');
INSERT INTO sys_dict_data VALUES(9,  2,  '暂停',     '1',       'sys_job_status',      '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '停用状态');
INSERT INTO sys_dict_data VALUES(10, 1,  '默认',     'DEFAULT', 'sys_job_group',       '',   '',        'Y', '0', 'admin', NOW(), '', NULL, '默认分组');
INSERT INTO sys_dict_data VALUES(11, 2,  '系统',     'SYSTEM',  'sys_job_group',       '',   '',        'N', '0', 'admin', NOW(), '', NULL, '系统分组');
INSERT INTO sys_dict_data VALUES(12, 1,  '是',       'Y',       'sys_yes_no',          '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '系统默认是');
INSERT INTO sys_dict_data VALUES(13, 2,  '否',       'N',       'sys_yes_no',          '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '系统默认否');
INSERT INTO sys_dict_data VALUES(14, 1,  '通知',     '1',       'sys_notice_type',     '',   'warning', 'Y', '0', 'admin', NOW(), '', NULL, '通知');
INSERT INTO sys_dict_data VALUES(15, 2,  '公告',     '2',       'sys_notice_type',     '',   'success', 'N', '0', 'admin', NOW(), '', NULL, '公告');
INSERT INTO sys_dict_data VALUES(16, 1,  '正常',     '0',       'sys_notice_status',   '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '正常状态');
INSERT INTO sys_dict_data VALUES(17, 2,  '关闭',     '1',       'sys_notice_status',   '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '关闭状态');
INSERT INTO sys_dict_data VALUES(18, 99, '其他',     '0',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', NOW(), '', NULL, '其他操作');
INSERT INTO sys_dict_data VALUES(19, 1,  '新增',     '1',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', NOW(), '', NULL, '新增操作');
INSERT INTO sys_dict_data VALUES(20, 2,  '修改',     '2',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', NOW(), '', NULL, '修改操作');
INSERT INTO sys_dict_data VALUES(21, 3,  '删除',     '3',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '删除操作');
INSERT INTO sys_dict_data VALUES(22, 4,  '授权',     '4',       'sys_oper_type',       '',   'primary', 'N', '0', 'admin', NOW(), '', NULL, '授权操作');
INSERT INTO sys_dict_data VALUES(23, 5,  '导出',     '5',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', NOW(), '', NULL, '导出操作');
INSERT INTO sys_dict_data VALUES(24, 6,  '导入',     '6',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', NOW(), '', NULL, '导入操作');
INSERT INTO sys_dict_data VALUES(25, 7,  '强退',     '7',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '强退操作');
INSERT INTO sys_dict_data VALUES(26, 8,  '生成代码', '8',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', NOW(), '', NULL, '生成操作');
INSERT INTO sys_dict_data VALUES(27, 9,  '清空数据', '9',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '清空操作');
INSERT INTO sys_dict_data VALUES(28, 1,  '成功',     '0',       'sys_common_status',   '',   'primary', 'N', '0', 'admin', NOW(), '', NULL, '正常状态');
INSERT INTO sys_dict_data VALUES(29, 2,  '失败',     '1',       'sys_common_status',   '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '停用状态');
INSERT INTO sys_dict_data VALUES(30, 0,  '系统指定', 'fixed',    'exp_data_type',       NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '表达式类型');
INSERT INTO sys_dict_data VALUES(31, 1,  '动态选择', 'dynamic',  'exp_data_type',       NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '表达式类型');
INSERT INTO sys_dict_data VALUES(32, 0,  '任务监听', '1',        'sys_listener_type',   NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '监听类型');
INSERT INTO sys_dict_data VALUES(33, 2,  '执行监听', '2',        'sys_listener_type',   NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '监听类型');
INSERT INTO sys_dict_data VALUES(34, 0,  'JAVA类',  'classListener',     'sys_listener_value_type', NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '监听值类型');
INSERT INTO sys_dict_data VALUES(35, 1,  '表达式',  'expressionListener','sys_listener_value_type', NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '监听值类型');
INSERT INTO sys_dict_data VALUES(36, 2,  '代理表达式', 'delegateExpressionListener','sys_listener_value_type', NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '监听值类型');
INSERT INTO sys_dict_data VALUES(37, 0,  '请假',     'leave',    'sys_process_category', NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '流程分类');
INSERT INTO sys_dict_data VALUES(38, 1,  '报销',     'expense',  'sys_process_category', NULL, 'default', 'N', '0', 'admin', NOW(), '', NULL, '流程分类');


-- ============================================
-- 13、参数配置表
-- ============================================
DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
  config_id         int(5)          NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  config_name       varchar(100)    DEFAULT ''                COMMENT '参数名称',
  config_key        varchar(100)    DEFAULT ''                COMMENT '参数键名',
  config_value      varchar(500)    DEFAULT ''                COMMENT '参数键值',
  config_type       char(1)         DEFAULT 'N'               COMMENT '系统内置（Y是 N否）',
  create_by         varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time       datetime                                COMMENT '创建时间',
  update_by         varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time       datetime                                COMMENT '更新时间',
  remark            varchar(500)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (config_id)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数配置表';

INSERT INTO sys_config VALUES(1, '主框架页-默认皮肤样式名称',     'sys.index.skinName',            'skin-blue',     'Y', 'admin', NOW(), '', NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO sys_config VALUES(2, '用户管理-账号初始密码',         'sys.user.initPassword',         '123456',        'Y', 'admin', NOW(), '', NULL, '初始化密码 123456');
INSERT INTO sys_config VALUES(3, '主框架页-侧边栏主题',           'sys.index.sideTheme',           'theme-dark',    'Y', 'admin', NOW(), '', NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO sys_config VALUES(4, '账号自助-验证码开关',           'sys.account.captchaEnabled',    'true',          'Y', 'admin', NOW(), '', NULL, '是否开启验证码功能（true开启，false关闭）');
INSERT INTO sys_config VALUES(5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser',      'false',         'Y', 'admin', NOW(), '', NULL, '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO sys_config VALUES(6, '用户登录-黑名单列表',           'sys.login.blackIPList',         '',              'Y', 'admin', NOW(), '', NULL, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');


-- ============================================
-- 14、系统访问记录
-- ============================================
DROP TABLE IF EXISTS sys_logininfor;
CREATE TABLE sys_logininfor (
  info_id        bigint(20)     NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  user_name      varchar(50)    DEFAULT ''                COMMENT '用户账号',
  ipaddr         varchar(128)   DEFAULT ''                COMMENT '登录IP地址',
  login_location varchar(255)   DEFAULT ''                COMMENT '登录地点',
  browser        varchar(50)    DEFAULT ''                COMMENT '浏览器类型',
  os             varchar(50)    DEFAULT ''                COMMENT '操作系统',
  status         char(1)        DEFAULT '0'               COMMENT '登录状态（0成功 1失败）',
  msg            varchar(255)   DEFAULT ''                COMMENT '提示消息',
  login_time     datetime                               COMMENT '访问时间',
  create_by      varchar(64)    DEFAULT ''                COMMENT '创建者',
  create_time    datetime                                 COMMENT '创建时间',
  update_by      varchar(64)    DEFAULT ''                COMMENT '更新者',
  update_time    datetime                                 COMMENT '更新时间',
  remark         varchar(500)   DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (info_id),
  KEY idx_sys_logininfor_s (status),
  KEY idx_sys_logininfor_lt (login_time)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统访问记录';


-- ============================================
-- 15、定时任务调度表
-- ============================================
DROP TABLE IF EXISTS sys_job;
CREATE TABLE sys_job (
  job_id              bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  job_name            varchar(64)   DEFAULT ''                COMMENT '任务名称',
  job_group           varchar(64)   DEFAULT 'DEFAULT'         COMMENT '任务组名',
  invoke_target       varchar(500)  NOT NULL                COMMENT '调用目标字符串',
  cron_expression     varchar(255)  DEFAULT ''                COMMENT 'cron执行表达式',
  misfire_policy      varchar(20)   DEFAULT '3'               COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  concurrent          char(1)       DEFAULT '1'               COMMENT '是否并发执行（0允许 1禁止）',
  status              char(1)       DEFAULT '0'               COMMENT '状态（0正常 1暂停）',
  create_by           varchar(64)   DEFAULT ''                COMMENT '创建者',
  create_time         datetime                               COMMENT '创建时间',
  update_by           varchar(64)   DEFAULT ''                COMMENT '更新者',
  update_time         datetime                               COMMENT '更新时间',
  remark              varchar(500)  DEFAULT ''                COMMENT '备注信息',
  PRIMARY KEY (job_id, job_name, job_group)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务调度表';

INSERT INTO sys_job VALUES(1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams',        '0/10 * * * * ?', '3', '1', '1', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_job VALUES(2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')',  '0/15 * * * * ?', '3', '1', '1', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_job VALUES(3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000, 316.50, 100)',  '0/20 * * * * ?', '3', '1', '1', 'admin', NOW(), '', NULL, '');


-- ============================================
-- 16、定时任务调度日志表
-- ============================================
DROP TABLE IF EXISTS sys_job_log;
CREATE TABLE sys_job_log (
  job_log_id          bigint(20)     NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  job_name            varchar(64)    NOT NULL                COMMENT '任务名称',
  job_group           varchar(64)    NOT NULL                COMMENT '任务组名',
  invoke_target       varchar(500)   NOT NULL                COMMENT '调用目标字符串',
  job_message         varchar(500)                           COMMENT '日志信息',
  status              char(1)        DEFAULT '0'               COMMENT '执行状态（0正常 1失败）',
  exception_info      varchar(2000)  DEFAULT ''                COMMENT '异常信息',
  create_time         datetime                               COMMENT '创建时间',
  create_by           varchar(64)   DEFAULT ''                COMMENT '创建者',
  update_by           varchar(64)   DEFAULT ''                COMMENT '更新者',
  update_time         datetime                               COMMENT '更新时间',
  remark              varchar(500)  DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (job_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务调度日志表';


-- ============================================
-- 17、通知公告表
-- ============================================
DROP TABLE IF EXISTS sys_notice;
CREATE TABLE sys_notice (
  notice_id         int(4)          NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  notice_title      varchar(50)     NOT NULL                COMMENT '公告标题',
  notice_type       char(1)         NOT NULL                COMMENT '公告类型（1通知 2公告）',
  notice_content    longblob        DEFAULT NULL              COMMENT '公告内容',
  status            char(1)         DEFAULT '0'               COMMENT '公告状态（0正常 1关闭）',
  create_by         varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time       datetime                                COMMENT '创建时间',
  update_by         varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time       datetime                                COMMENT '更新时间',
  remark            varchar(255)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (notice_id)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';

-- 初始化-公告信息表数据
INSERT INTO sys_notice VALUES('1', '温馨提醒：2018-07-01 若依新版本发布啦', '2', '新版本内容', '0', 'admin', NOW(), '', NULL, '管理员');
INSERT INTO sys_notice VALUES('2', '维护通知：2018-07-01 若依系统凌晨维护', '1', '维护内容',   '0', 'admin', NOW(), '', NULL, '管理员');


-- ============================================
-- 18、代码生成业务表
-- ============================================
DROP TABLE IF EXISTS gen_table;
CREATE TABLE gen_table (
  table_id          bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '编号',
  table_name        varchar(200)    DEFAULT ''                COMMENT '表名称',
  table_comment     varchar(500)    DEFAULT ''                COMMENT '表描述',
  sub_table_name    varchar(64)     DEFAULT NULL              COMMENT '关联子表的表名',
  sub_table_fk_name varchar(64)     DEFAULT NULL              COMMENT '子表关联的外键名',
  class_name        varchar(100)    DEFAULT ''                COMMENT '实体类名称',
  tpl_category      varchar(200)    DEFAULT 'crud'            COMMENT '使用的模板（crud单表操作 tree树表操作）',
  tpl_web_type      varchar(30)     DEFAULT ''                COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  package_name      varchar(100)                           COMMENT '生成包路径',
  module_name       varchar(30)                            COMMENT '生成模块名',
  business_name     varchar(30)                            COMMENT '生成业务名',
  function_name     varchar(50)                            COMMENT '生成功能名',
  function_author   varchar(50)                            COMMENT '生成功能作者',
  gen_type          char(1)         DEFAULT '0'               COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  gen_path          varchar(200)    DEFAULT '/'               COMMENT '生成路径（不填默认项目路径）',
  options           varchar(1000)                           COMMENT '其它生成选项',
  create_by         varchar(64)     DEFAULT ''                COMMENT '创建者',
  create_time       datetime                                COMMENT '创建时间',
  update_by         varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time       datetime                                COMMENT '更新时间',
  remark            varchar(500)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (table_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成业务表';


-- ============================================
-- 19、代码生成业务表字段
-- ============================================
DROP TABLE IF EXISTS gen_table_column;
CREATE TABLE gen_table_column (
  column_id         bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '编号',
  table_id          bigint(20)                             COMMENT '归属表编号',
  column_name       varchar(200)                           COMMENT '列名称',
  column_comment    varchar(500)                           COMMENT '列描述',
  column_type       varchar(100)                           COMMENT '列类型',
  java_type         varchar(500)                           COMMENT 'JAVA类型',
  java_field        varchar(200)                           COMMENT 'JAVA字段名',
  is_pk             char(1)                                COMMENT '是否主键（1是）',
  is_increment      char(1)                                COMMENT '是否自增（1是）',
  is_required       char(1)                                COMMENT '是否必填（1是）',
  is_insert         char(1)                                COMMENT '是否为插入字段（1是）',
  is_edit           char(1)                                COMMENT '是否编辑字段（1是）',
  is_list           char(1)                                COMMENT '是否列表字段（1是）',
  is_query          char(1)                                COMMENT '是否查询字段（1是）',
  query_type        varchar(200)    DEFAULT 'EQ'            COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  html_type         varchar(200)                           COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  dict_type         varchar(200)    DEFAULT ''               COMMENT '字典类型',
  sort              int                                    COMMENT '排序',
  create_by         varchar(64)     DEFAULT ''               COMMENT '创建者',
  create_time       datetime                               COMMENT '创建时间',
  update_by         varchar(64)     DEFAULT ''               COMMENT '更新者',
  update_time       datetime                               COMMENT '更新时间',
  remark            varchar(500)   DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (column_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成业务表字段';


-- ============================================
-- 20、流程表单表
-- ============================================
DROP TABLE IF EXISTS sys_form;
CREATE TABLE sys_form (
  form_id      bigint(20) NOT NULL AUTO_INCREMENT COMMENT '表单主键',
  form_name    varchar(50)  DEFAULT NULL COMMENT '表单名称',
  form_content longtext COMMENT '表单内容',
  create_time  datetime     DEFAULT NULL COMMENT '创建时间',
  update_time  datetime     DEFAULT NULL COMMENT '更新时间',
  create_by    bigint(20) DEFAULT NULL COMMENT '创建人员',
  update_by    bigint(20) DEFAULT NULL COMMENT '更新人员',
  remark       varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (form_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程表单';


-- ============================================
-- 21、流程表达式表
-- ============================================
DROP TABLE IF EXISTS sys_expression;
CREATE TABLE sys_expression (
  id          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '表单主键',
  name        varchar(50)  DEFAULT NULL COMMENT '表达式名称',
  expression  varchar(255) DEFAULT NULL COMMENT '表达式内容',
  data_type   varchar(255) DEFAULT NULL COMMENT '表达式类型',
  create_time datetime     DEFAULT NULL COMMENT '创建时间',
  update_time datetime     DEFAULT NULL COMMENT '更新时间',
  create_by   bigint(20) DEFAULT NULL COMMENT '创建人员',
  update_by   bigint(20) DEFAULT NULL COMMENT '更新人员',
  status      tinyint(2) DEFAULT '0' COMMENT '状态',
  remark      varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程表达式';


-- ============================================
-- 22、流程监听器表
-- ============================================
DROP TABLE IF EXISTS sys_listener;
CREATE TABLE sys_listener (
  id          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '表单主键',
  name        varchar(128) DEFAULT NULL COMMENT '名称',
  type        varchar(64)  DEFAULT NULL COMMENT '监听类型',
  event_type  varchar(64)  DEFAULT NULL COMMENT '监听事件类型',
  value_type  varchar(64)  DEFAULT NULL COMMENT '监听值类型',
  value       varchar(255) DEFAULT NULL COMMENT '监听值',
  create_time datetime     DEFAULT NULL COMMENT '创建时间',
  update_time datetime     DEFAULT NULL COMMENT '更新时间',
  create_by   bigint(20) DEFAULT NULL COMMENT '创建人员',
  update_by   bigint(20) DEFAULT NULL COMMENT '更新人员',
  status      tinyint(2) DEFAULT '0' COMMENT '状态',
  remark      varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程监听';


-- ============================================
-- 23、流程部署关联表
-- ============================================
DROP TABLE IF EXISTS sys_deploy_form;
CREATE TABLE sys_deploy_form (
  id        bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  form_id   bigint(20) DEFAULT NULL COMMENT '表单主键',
  deploy_id varchar(50) DEFAULT NULL COMMENT '流程实例主键',
  create_by   varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime COMMENT '创建时间',
  update_by   varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime COMMENT '更新时间',
  remark      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例关联表单';

SET FOREIGN_KEY_CHECKS = 1;
