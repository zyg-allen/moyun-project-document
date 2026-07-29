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

-- sys_notice_bak 种子数据（2 条）
LOCK TABLES `sys_notice_bak` WRITE;
INSERT INTO `sys_notice_bak` VALUES (1,'温馨提醒：2018-07-01 若依新版本发布啦','2',_binary '新版本内容','0','admin','2026-07-28 15:42:36','',NULL,'管理员'),(2,'维护通知：2018-07-01 若依系统凌晨维护','1',_binary '维护内容','0','admin','2026-07-28 15:42:36','',NULL,'管理员');
UNLOCK TABLES;
