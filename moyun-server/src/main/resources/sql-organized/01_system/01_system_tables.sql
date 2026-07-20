-- ============================================
-- 墨韵智库 - 系统模块建表 DDL（最终合并版）
-- ============================================
-- 合并来源脚本：
--   01_moyun_init.sql        （系统模块基础建表，sys_dept/sys_user/sys_role/sys_menu/sys_config/sys_dict_type/sys_dict_data/sys_job/sys_oper_log/sys_logininfor/sys_notice/sys_post/sys_role_menu/sys_role_dept/sys_user_role/sys_user_post/gen_table/gen_table_column）
--   11_create_sys_file_table.sql （sys_file 建表）
--   29_alter_all_tables_base_fields.sql （对 01 中已有审计字段的表无影响，对 sys_file/sys_notification 不涉及；本文件保留 01 原始字段定义）
--   34_merge_notification_tables.sql （sys_notification、sys_notification_read 建表）
--   35_add_notification_user_type.sql （sys_notification、sys_notification_read 新增 user_type 字段及索引调整）
--   85_sys_file_fallback_fields.sql （sys_file 新增 fallback、local_path 字段及 idx_fallback 索引）
-- 涉及表：
--   sys_dept, sys_user, sys_role, sys_menu, sys_config, sys_dict_type, sys_dict_data,
--   sys_job, sys_oper_log, sys_logininfor, sys_notice, sys_post,
--   sys_role_menu, sys_role_dept, sys_user_role, sys_user_post,
--   gen_table, gen_table_column, sys_file, sys_notification, sys_notification_read
-- 说明：
--   - 本文件仅包含建表 DDL（CREATE TABLE IF NOT EXISTS），不含 INSERT 数据、菜单 UPDATE、配置 INSERT 等
--   - 所有 ALTER TABLE 已合并入对应的 CREATE TABLE
--   - 菜单修复类（25/49/50/82/83/39/40 中的菜单部分）与配置类（37 中的 sys_config INSERT）见其他目录
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1、部门表 sys_dept
-- ============================================
CREATE TABLE IF NOT EXISTS sys_dept (
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
  create_time       datetime                                  COMMENT '创建时间',
  update_by         varchar(64)     DEFAULT ''                COMMENT '更新者',
  update_time       datetime                                  COMMENT '更新时间',
  remark            varchar(500)    DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (dept_id)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- ============================================
-- 2、用户信息表 sys_user
-- ============================================
CREATE TABLE IF NOT EXISTS sys_user (
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

-- ============================================
-- 3、角色信息表 sys_role
-- ============================================
CREATE TABLE IF NOT EXISTS sys_role (
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

-- ============================================
-- 4、菜单权限表 sys_menu
-- ============================================
CREATE TABLE IF NOT EXISTS sys_menu (
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

-- ============================================
-- 5、参数配置表 sys_config
-- ============================================
CREATE TABLE IF NOT EXISTS sys_config (
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

-- ============================================
-- 6、字典类型表 sys_dict_type
-- ============================================
CREATE TABLE IF NOT EXISTS sys_dict_type (
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

-- ============================================
-- 7、字典数据表 sys_dict_data
-- ============================================
CREATE TABLE IF NOT EXISTS sys_dict_data (
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

-- ============================================
-- 8、定时任务调度表 sys_job
--    ⚠️ 注意：本项目主键为 (job_id, job_name, job_group) 三字段复合主键，
--    标准 RuoYi 是 (job_id, job_name) 两字段。此为项目自定义调整，已通过业务回归测试。
--    如切换到标准 RuoYi 主键结构，需同步修改 SysJob 实体与 Mapper。
-- ============================================
CREATE TABLE IF NOT EXISTS sys_job (
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

-- ============================================
-- 9、操作日志记录 sys_oper_log
--    （01 原始建表已含 5 个审计字段，29 号补丁无可新增字段，保留原始结构）
-- ============================================
CREATE TABLE IF NOT EXISTS sys_oper_log (
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
-- 10、系统访问记录 sys_logininfor
--    （01 原始建表已含 5 个审计字段，29 号补丁无可新增字段，保留原始结构）
-- ============================================
CREATE TABLE IF NOT EXISTS sys_logininfor (
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
-- 11、通知公告表 sys_notice
--    （34 号脚本将其 RENAME 为 sys_notice_bak 作迁移备份，此处保留原表 DDL；
--     运行时实际通知数据已迁至 sys_notification，是否需要保留 sys_notice 取决于部署需求）
-- ============================================
CREATE TABLE IF NOT EXISTS sys_notice (
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

-- ============================================
-- 12、岗位信息表 sys_post
-- ============================================
CREATE TABLE IF NOT EXISTS sys_post (
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

-- ============================================
-- 13、角色和菜单关联表 sys_role_menu
--    （01 原始建表已含 5 个审计字段，29 号补丁无可新增字段，保留原始结构）
-- ============================================
CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id   bigint(20) NOT NULL COMMENT '角色ID',
  menu_id   bigint(20) NOT NULL COMMENT '菜单ID',
  create_by   varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime COMMENT '创建时间',
  update_by   varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime COMMENT '更新时间',
  remark      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY(role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和菜单关联表';

-- ============================================
-- 14、角色和部门关联表 sys_role_dept
--    （01 原始建表已含 5 个审计字段，29 号补丁无可新增字段，保留原始结构）
-- ============================================
CREATE TABLE IF NOT EXISTS sys_role_dept (
  role_id   bigint(20) NOT NULL COMMENT '角色ID',
  dept_id   bigint(20) NOT NULL COMMENT '部门ID',
  create_by   varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime COMMENT '创建时间',
  update_by   varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime COMMENT '更新时间',
  remark      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY(role_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和部门关联表';

-- ============================================
-- 15、用户和角色关联表 sys_user_role
--    （01 原始建表已含 5 个审计字段，29 号补丁无可新增字段，保留原始结构）
-- ============================================
CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id   bigint(20) NOT NULL COMMENT '用户ID',
  role_id   bigint(20) NOT NULL COMMENT '角色ID',
  create_by   varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime COMMENT '创建时间',
  update_by   varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime COMMENT '更新时间',
  remark      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY(user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';

-- ============================================
-- 16、用户与岗位关联表 sys_user_post
--    （01 原始建表已含 5 个审计字段，29 号补丁无可新增字段，保留原始结构）
-- ============================================
CREATE TABLE IF NOT EXISTS sys_user_post (
  user_id   bigint(20) NOT NULL COMMENT '用户ID',
  post_id   bigint(20) NOT NULL COMMENT '岗位ID',
  create_by   varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime COMMENT '创建时间',
  update_by   varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime COMMENT '更新时间',
  remark      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与岗位关联表';

-- ============================================
-- 17、代码生成业务表 gen_table
-- ============================================
CREATE TABLE IF NOT EXISTS gen_table (
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
-- 18、代码生成业务表字段 gen_table_column
--    （01 原始建表已含 remark，29 号补丁无可新增字段，保留原始结构）
-- ============================================
CREATE TABLE IF NOT EXISTS gen_table_column (
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
-- 19、文件管理表 sys_file
--    合并来源：
--      11_create_sys_file_table.sql （原始建表）
--      85_sys_file_fallback_fields.sql （新增 fallback、local_path 字段及 idx_fallback 索引）
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_file`
(
    `id`               bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `file_name`        varchar(500)  NOT NULL COMMENT '文件名称',
    `file_ext`         varchar(100) DEFAULT NULL COMMENT '文件扩展名',
    `file_type`        varchar(100) DEFAULT NULL COMMENT '文件类型（image/document/video/audio/other）',
    `file_size`        bigint(20)   DEFAULT NULL COMMENT '文件大小（字节）',
    `file_url`         varchar(1000) NOT NULL COMMENT '文件访问URL',
    `file_path`        varchar(500) DEFAULT NULL COMMENT '文件路径',
    `storage_type`     varchar(100) DEFAULT NULL COMMENT '存储类型（minio/local）',
    `bucket_name`      varchar(100) DEFAULT NULL COMMENT '存储桶名称',
    `object_name`      varchar(500) DEFAULT NULL COMMENT '对象名称',
    `fallback`         TINYINT(1)   DEFAULT 0 COMMENT '是否降级存储（0=正常 1=因MinIO不可用降级到本地）',
    `local_path`       VARCHAR(500) DEFAULT NULL COMMENT '本地备份绝对路径（MinIO可用时也记录，便于降级访问）',
    `file_md5`         varchar(500) DEFAULT NULL COMMENT '文件MD5值',
    `upload_user_id`   bigint(20)   DEFAULT NULL COMMENT '上传用户ID',
    `upload_user_name` varchar(100) DEFAULT NULL COMMENT '上传用户名称',
    `status`           varchar(20)  DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `business_type`    varchar(500) DEFAULT NULL COMMENT '业务类型',
    `business_id`      varchar(100) DEFAULT NULL COMMENT '业务ID',
    `create_by`        varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`      datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`        varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`      datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`           varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_storage_type` (`storage_type`),
    KEY `idx_business_type` (`business_type`),
    KEY `idx_business_id` (`business_id`),
    KEY `idx_upload_user_id` (`upload_user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_fallback` (`fallback`)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件管理表';

-- ============================================
-- 20、系统通知主体表 sys_notification
--    合并来源：
--      34_merge_notification_tables.sql （原始建表）
--      35_add_notification_user_type.sql （在 user_id 后新增 user_type 字段；新增 idx_user_type_user_id 索引）
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_notification` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `type`          varchar(50)   NOT NULL                COMMENT '类型：system/comment/like/follow/order/notice/announcement',
  `title`         varchar(200)  DEFAULT NULL            COMMENT '通知标题',
  `content`       text                                  COMMENT '通知内容',
  `data`          json          DEFAULT NULL            COMMENT '通知数据（JSON格式）',
  `scope`         varchar(20)   NOT NULL DEFAULT 'user' COMMENT '范围：user=个人通知 / all=全局广播',
  `user_id`       bigint        DEFAULT NULL            COMMENT '接收用户ID（scope=user 时必填，scope=all 时为 NULL）',
  `user_type`     VARCHAR(20)   NOT NULL DEFAULT 'portal' COMMENT '接收用户类型：portal=门户用户 / sys=系统用户（scope=user 时生效）',
  `notice_type`   char(1)       DEFAULT NULL            COMMENT '通知/公告分类：1=通知 / 2=公告（兼容 sys_notice 字典 sys_notice_type）',
  `status`        char(1)       DEFAULT '0'              COMMENT '状态：0=正常 / 1=关闭（兼容 sys_notice 字典 sys_notice_status）',
  `create_by`     varchar(64)   DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     varchar(64)   DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`        varchar(500)  DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_scope` (`scope`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_type_user_id` (`user_type`, `user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知主体表（合并 portal_notification + sys_notice）';

-- ============================================
-- 21、系统通知用户已读关系表 sys_notification_read
--    合并来源：
--      34_merge_notification_tables.sql （原始建表，唯一索引 uk_notif_user）
--      35_add_notification_user_type.sql （新增 user_type 字段；删除 uk_notif_user，新建 uk_notif_user_type 唯一索引）
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_notification_read` (
  `id`              bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notification_id` bigint   NOT NULL                COMMENT '通知ID（关联 sys_notification.id）',
  `user_id`         bigint   NOT NULL                COMMENT '用户ID',
  `user_type`       VARCHAR(20) NOT NULL DEFAULT 'portal' COMMENT '已读用户类型：portal=门户用户 / sys=系统用户',
  `read_time`       datetime DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `create_time`     datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notif_user_type` (`notification_id`, `user_id`, `user_type`) COMMENT '唯一索引：防止同一用户对同一通知重复标记已读',
  KEY `idx_user_id` (`user_id`),
  KEY `idx_notification_id` (`notification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知用户已读关系表';

-- ============================================
-- 22、定时任务调度日志表 sys_job_log
--    合并来源：
--      01_moyun_init.sql                  （原始建表，已含 create_time/create_by/update_by/update_time/remark）
--      29_alter_all_tables_base_fields.sql（通过存储过程 AddColumnIfNotExists 幂等补 create_by/update_by/update_time/remark 4 个审计字段；
--        因 01 建表已含这些字段，29 脚本为幂等兜底，本 CREATE 无新增字段）
--    说明：之前整理时遗漏本表，现补齐
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_job_log` (
  `job_log_id`     bigint(20)    NOT NULL AUTO_INCREMENT  COMMENT '任务日志ID',
  `job_name`       varchar(64)   NOT NULL                 COMMENT '任务名称',
  `job_group`      varchar(64)   NOT NULL                 COMMENT '任务组名',
  `invoke_target`  varchar(500)  NOT NULL                 COMMENT '调用目标字符串',
  `job_message`    varchar(500)                           COMMENT '日志信息',
  `status`         char(1)        DEFAULT '0'             COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000)  DEFAULT ''              COMMENT '异常信息',
  `create_time`    datetime                               COMMENT '创建时间',
  `create_by`      varchar(64)   DEFAULT ''               COMMENT '创建者',
  `update_by`      varchar(64)   DEFAULT ''               COMMENT '更新者',
  `update_time`    datetime                               COMMENT '更新时间',
  `remark`         varchar(500)  DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务调度日志表';

-- ============================================
-- 23、流程部署关联表 sys_deploy_form
--    合并来源：
--      01_moyun_init.sql                  （原始建表，已含 create_by/create_time/update_by/update_time/remark 全部 5 个审计字段）
--      29_alter_all_tables_base_fields.sql（通过存储过程幂等兜底，因 01 建表已含全部审计字段，无新增）
--    说明：之前整理时遗漏本表，现补齐
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_deploy_form` (
  `id`          bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `form_id`     bigint(20)    DEFAULT NULL            COMMENT '表单主键',
  `deploy_id`   varchar(50)   DEFAULT NULL            COMMENT '流程实例主键',
  `create_by`   varchar(64)   DEFAULT ''              COMMENT '创建者',
  `create_time` datetime                              COMMENT '创建时间',
  `update_by`   varchar(64)   DEFAULT ''              COMMENT '更新者',
  `update_time` datetime                              COMMENT '更新时间',
  `remark`      varchar(500)  DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例关联表单';

SET FOREIGN_KEY_CHECKS = 1;
