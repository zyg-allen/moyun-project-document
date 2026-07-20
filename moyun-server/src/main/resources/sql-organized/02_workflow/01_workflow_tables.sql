-- =============================================
-- 墨韵智库 - 工作流模块建表 DDL（最终合并版）
-- =============================================
-- 合并来源脚本：
--   02_workflow_init.sql （Flowable 7.x 工作流引擎全部 act_* 建表 + 索引）
-- 涉及表（共 39 张 Flowable 引擎表）：
--   act_evt_log, act_ge_property,
--   act_hi_actinst, act_hi_attachment, act_hi_comment, act_hi_detail, act_hi_entitylink,
--   act_hi_identitylink, act_hi_procinst, act_hi_taskinst, act_hi_tsk_log, act_hi_varinst,
--   act_id_bytearray, act_id_group, act_id_info, act_id_priv, act_id_priv_mapping,
--   act_id_property, act_id_token, act_id_user, act_id_membership,
--   act_re_deployment, act_ge_bytearray, act_re_model, act_re_procdef, act_procdef_info,
--   act_ru_actinst, act_ru_entitylink, act_ru_execution,
--   act_ru_deadletter_job, act_ru_event_subscr, act_ru_external_job, act_ru_history_job,
--   act_ru_job, act_ru_suspended_job, act_ru_task, act_ru_identitylink,
--   act_ru_timer_job, act_ru_variable
-- 说明：
--   - 本文件仅包含 act_* 表的建表 DDL 与 CREATE INDEX 语句
--   - 02 号脚本中的 QRTZ_* （Quartz 定时任务表）不在本文件范围，单独管理
--   - 02 号脚本中的 INSERT（act_ge_bytearray/act_ge_property/act_re_deployment/act_re_procdef 部署示例数据）属于运行时数据，不在本文件
--   - 所有 act_* 表均无 ALTER 历史，直接保留 02 号原始结构
--   - 已统一为 CREATE TABLE IF NOT EXISTS（02 号原脚本即为 IF NOT EXISTS）
--   - ⚠️ 字符集说明：act_* 表保留 Flowable 官方 utf8mb3_bin 字符集，未强制 utf8mb4
--     原因：Flowable 引擎对字符集有兼容性考量，统一改为 utf8mb4 可能导致引擎异常
--     跨字符集 JOIN 时若出现 "Illegal mix of collations" 错误，可在 act_* 表上单独 ALTER CONVERT TO
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

create table if not exists act_evt_log
(
    LOG_NR_       bigint auto_increment
        primary key,
    TYPE_         varchar(64)                               null,
    PROC_DEF_ID_  varchar(64)                               null,
    PROC_INST_ID_ varchar(64)                               null,
    EXECUTION_ID_ varchar(64)                               null,
    TASK_ID_      varchar(64)                               null,
    TIME_STAMP_   timestamp(3) default CURRENT_TIMESTAMP(3) not null,
    USER_ID_      varchar(255)                              null,
    DATA_         longblob                                  null,
    LOCK_OWNER_   varchar(255)                              null,
    LOCK_TIME_    timestamp(3)                              null,
    IS_PROCESSED_ tinyint      default 0                    null
)
    collate = utf8mb3_bin;

create table if not exists act_ge_property
(
    NAME_  varchar(64)  not null,
    VALUE_ varchar(300) null,
    REV_   int          null,
    primary key (NAME_)
)
    collate = utf8mb3_bin;

-- =============================================
-- act_hi_actinst: 历史活动实例表
-- 存储流程执行过程中所有活动的历史记录
-- =============================================
create table if not exists act_hi_actinst
(
    ID_                varchar(64)             not null comment '主键ID',
    REV_               int          default 1  null comment '数据版本号',
    PROC_DEF_ID_       varchar(64)             not null comment '流程定义ID',
    PROC_INST_ID_      varchar(64)             not null comment '流程实例ID',
    EXECUTION_ID_      varchar(64)             not null comment '执行ID',
    ACT_ID_            varchar(255)            not null comment '活动ID',
    TASK_ID_           varchar(64)             null comment '任务ID',
    CALL_PROC_INST_ID_ varchar(64)             null comment '调用的流程实例ID',
    ACT_NAME_          varchar(255)            null comment '活动名称',
    ACT_TYPE_          varchar(255)            not null comment '活动类型',
    ASSIGNEE_          varchar(255)            null comment '执行人',
    START_TIME_        datetime(3)             not null comment '开始时间',
    END_TIME_          datetime(3)             null comment '结束时间',
    TRANSACTION_ORDER_ int                     null comment '事务顺序',
    DURATION_          bigint                  null comment '持续时间(毫秒)',
    DELETE_REASON_     varchar(4000)           null comment '删除原因',
    TENANT_ID_         varchar(255) default '' null comment '租户ID',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '历史活动实例表';

create index ACT_IDX_HI_ACT_INST_END
    on act_hi_actinst (END_TIME_);

create index ACT_IDX_HI_ACT_INST_EXEC
    on act_hi_actinst (EXECUTION_ID_, ACT_ID_);

create index ACT_IDX_HI_ACT_INST_PROCINST
    on act_hi_actinst (PROC_INST_ID_, ACT_ID_);

create index ACT_IDX_HI_ACT_INST_START
    on act_hi_actinst (START_TIME_);

-- =============================================
-- act_hi_attachment: 历史附件表
-- 存储流程或任务相关的历史附件信息
-- =============================================
create table if not exists act_hi_attachment
(
    ID_           varchar(64)   not null comment '主键ID',
    REV_          int           null comment '数据版本号',
    USER_ID_      varchar(255)  null comment '创建用户ID',
    NAME_         varchar(255)  null comment '附件名称',
    DESCRIPTION_  varchar(4000) null comment '附件描述',
    TYPE_         varchar(255)  null comment '附件类型',
    TASK_ID_      varchar(64)   null comment '任务ID',
    PROC_INST_ID_ varchar(64)   null comment '流程实例ID',
    URL_          varchar(4000) null comment '附件URL',
    CONTENT_ID_   varchar(64)   null comment '内容ID',
    TIME_         datetime(3)   null comment '创建时间',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '历史附件表';

-- =============================================
-- act_hi_comment: 历史评论表
-- 存储流程或任务相关的历史评论信息
-- =============================================
create table if not exists act_hi_comment
(
    ID_           varchar(64)   not null comment '主键ID',
    TYPE_         varchar(255)  null comment '评论类型',
    TIME_         datetime(3)   not null comment '评论时间',
    USER_ID_      varchar(255)  null comment '评论用户ID',
    TASK_ID_      varchar(64)   null comment '任务ID',
    PROC_INST_ID_ varchar(64)   null comment '流程实例ID',
    ACTION_       varchar(255)  null comment '动作',
    MESSAGE_      varchar(4000) null comment '消息内容',
    FULL_MSG_     longblob      null comment '完整消息',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '历史评论表';

-- =============================================
-- act_hi_detail: 历史变量明细记录表
-- 存储流程变量的历史变更记录
-- =============================================
create table if not exists act_hi_detail
(
    ID_           varchar(64)   not null comment '主键ID',
    TYPE_         varchar(255)  not null comment '变量类型',
    PROC_INST_ID_ varchar(64)   null comment '流程实例ID',
    EXECUTION_ID_ varchar(64)   null comment '执行ID',
    TASK_ID_      varchar(64)   null comment '任务ID',
    ACT_INST_ID_  varchar(64)   null comment '活动实例ID',
    NAME_         varchar(255)  not null comment '变量名称',
    VAR_TYPE_     varchar(255)  null comment '变量类型',
    REV_          int           null comment '数据版本号',
    TIME_         datetime(3)   not null comment '操作时间',
    BYTEARRAY_ID_ varchar(64)   null comment '字节数组ID',
    DOUBLE_       double        null comment 'Double类型值',
    LONG_         bigint        null comment 'Long类型值',
    TEXT_         varchar(4000) null comment '文本类型值',
    TEXT2_        varchar(4000) null comment '附加文本值',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '历史变量明细记录表';

create index ACT_IDX_HI_DETAIL_ACT_INST
    on act_hi_detail (ACT_INST_ID_);

create index ACT_IDX_HI_DETAIL_NAME
    on act_hi_detail (NAME_);

create index ACT_IDX_HI_DETAIL_PROC_INST
    on act_hi_detail (PROC_INST_ID_);

create index ACT_IDX_HI_DETAIL_TASK_ID
    on act_hi_detail (TASK_ID_);

create index ACT_IDX_HI_DETAIL_TIME
    on act_hi_detail (TIME_);

-- =============================================
-- act_hi_entitylink: 历史实体链接表
-- 存储流程与其他实体的历史关联关系
-- =============================================
create table if not exists act_hi_entitylink
(
    ID_                      varchar(64)  not null comment '主键ID',
    LINK_TYPE_               varchar(255) null comment '链接类型',
    CREATE_TIME_             datetime(3)  null comment '创建时间',
    SCOPE_ID_                varchar(255) null comment '范围ID',
    SUB_SCOPE_ID_            varchar(255) null comment '子范围ID',
    SCOPE_TYPE_              varchar(255) null comment '范围类型',
    SCOPE_DEFINITION_ID_     varchar(255) null comment '范围定义ID',
    PARENT_ELEMENT_ID_       varchar(255) null comment '父元素ID',
    REF_SCOPE_ID_            varchar(255) null comment '引用范围ID',
    REF_SCOPE_TYPE_          varchar(255) null comment '引用范围类型',
    REF_SCOPE_DEFINITION_ID_ varchar(255) null comment '引用范围定义ID',
    ROOT_SCOPE_ID_           varchar(255) null comment '根范围ID',
    ROOT_SCOPE_TYPE_         varchar(255) null comment '根范围类型',
    HIERARCHY_TYPE_          varchar(255) null comment '层级类型',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '历史实体链接表';

create index ACT_IDX_HI_ENT_LNK_REF_SCOPE
    on act_hi_entitylink (REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_HI_ENT_LNK_ROOT_SCOPE
    on act_hi_entitylink (ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_HI_ENT_LNK_SCOPE
    on act_hi_entitylink (SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_HI_ENT_LNK_SCOPE_DEF
    on act_hi_entitylink (SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

-- =============================================
-- act_hi_identitylink: 历史身份关系表
-- 存储流程或任务的历史身份关系
-- =============================================
create table if not exists act_hi_identitylink
(
    ID_                  varchar(64)  not null comment '主键ID',
    GROUP_ID_            varchar(255) null comment '用户组ID',
    TYPE_                varchar(255) null comment '关系类型',
    USER_ID_             varchar(255) null comment '用户ID',
    TASK_ID_             varchar(64)  null comment '任务ID',
    CREATE_TIME_         datetime(3)  null comment '创建时间',
    PROC_INST_ID_        varchar(64)  null comment '流程实例ID',
    SCOPE_ID_            varchar(255) null comment '范围ID',
    SUB_SCOPE_ID_        varchar(255) null comment '子范围ID',
    SCOPE_TYPE_          varchar(255) null comment '范围类型',
    SCOPE_DEFINITION_ID_ varchar(255) null comment '范围定义ID',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '历史身份关系表';

create index ACT_IDX_HI_IDENT_LNK_PROCINST
    on act_hi_identitylink (PROC_INST_ID_);

create index ACT_IDX_HI_IDENT_LNK_SCOPE
    on act_hi_identitylink (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_IDENT_LNK_SCOPE_DEF
    on act_hi_identitylink (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_IDENT_LNK_SUB_SCOPE
    on act_hi_identitylink (SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_IDENT_LNK_TASK
    on act_hi_identitylink (TASK_ID_);

create index ACT_IDX_HI_IDENT_LNK_USER
    on act_hi_identitylink (USER_ID_);

-- =============================================
-- act_hi_procinst: 历史流程实例表
-- 存储已完成的流程实例历史信息
-- =============================================
create table if not exists act_hi_procinst
(
    ID_                        varchar(64)             not null comment '主键ID',
    REV_                       int          default 1  null comment '数据版本号',
    PROC_INST_ID_              varchar(64)             not null comment '流程实例ID',
    BUSINESS_KEY_              varchar(255)            null comment '业务键',
    PROC_DEF_ID_               varchar(64)             not null comment '流程定义ID',
    START_TIME_                datetime(3)             not null comment '开始时间',
    END_TIME_                  datetime(3)             null comment '结束时间',
    DURATION_                  bigint                  null comment '持续时间(毫秒)',
    START_USER_ID_             varchar(255)            null comment '启动用户ID',
    START_ACT_ID_              varchar(255)            null comment '开始活动ID',
    END_ACT_ID_                varchar(255)            null comment '结束活动ID',
    SUPER_PROCESS_INSTANCE_ID_ varchar(64)             null comment '上级流程实例ID',
    DELETE_REASON_             varchar(4000)           null comment '删除原因',
    TENANT_ID_                 varchar(255) default '' null comment '租户ID',
    NAME_                      varchar(255)            null comment '流程实例名称',
    CALLBACK_ID_               varchar(255)            null comment '回调ID',
    CALLBACK_TYPE_             varchar(255)            null comment '回调类型',
    REFERENCE_ID_              varchar(255)            null comment '引用ID',
    REFERENCE_TYPE_            varchar(255)            null comment '引用类型',
    PROPAGATED_STAGE_INST_ID_  varchar(255)            null comment '传播的阶段实例ID',
    BUSINESS_STATUS_           varchar(255)            null comment '业务状态',
    primary key (ID_),
    constraint PROC_INST_ID_
        unique (PROC_INST_ID_)
)
    collate = utf8mb3_bin comment = '历史流程实例表';

create index ACT_IDX_HI_PRO_INST_END
    on act_hi_procinst (END_TIME_);

create index ACT_IDX_HI_PRO_I_BUSKEY
    on act_hi_procinst (BUSINESS_KEY_);

create index ACT_IDX_HI_PRO_SUPER_PROCINST
    on act_hi_procinst (SUPER_PROCESS_INSTANCE_ID_);

-- =============================================
-- act_hi_taskinst: 历史任务实例表
-- 存储已完成的任务实例历史信息
-- =============================================
create table if not exists act_hi_taskinst
(
    ID_                       varchar(64)             not null comment '主键ID',
    REV_                      int          default 1  null comment '数据版本号',
    PROC_DEF_ID_              varchar(64)             null comment '流程定义ID',
    TASK_DEF_ID_              varchar(64)             null comment '任务定义ID',
    TASK_DEF_KEY_             varchar(255)            null comment '任务定义键',
    PROC_INST_ID_             varchar(64)             null comment '流程实例ID',
    EXECUTION_ID_             varchar(64)             null comment '执行ID',
    SCOPE_ID_                 varchar(255)            null comment '范围ID',
    SUB_SCOPE_ID_             varchar(255)            null comment '子范围ID',
    SCOPE_TYPE_               varchar(255)            null comment '范围类型',
    SCOPE_DEFINITION_ID_      varchar(255)            null comment '范围定义ID',
    PROPAGATED_STAGE_INST_ID_ varchar(255)            null comment '传播的阶段实例ID',
    STATE_                    varchar(255)            null comment '任务状态',
    NAME_                     varchar(255)            null comment '任务名称',
    PARENT_TASK_ID_           varchar(64)             null comment '父任务ID',
    DESCRIPTION_              varchar(4000)           null comment '任务描述',
    OWNER_                    varchar(255)            null comment '任务所有者',
    ASSIGNEE_                 varchar(255)            null comment '任务执行人',
    START_TIME_               datetime(3)             not null comment '开始时间',
    IN_PROGRESS_TIME_         datetime(3)             null comment '进行中时间',
    IN_PROGRESS_STARTED_BY_   varchar(255)            null comment '进行中开始人',
    CLAIM_TIME_               datetime(3)             null comment '签收时间',
    CLAIMED_BY_               varchar(255)            null comment '签收人',
    SUSPENDED_TIME_           datetime(3)             null comment '挂起时间',
    SUSPENDED_BY_             varchar(255)            null comment '挂起人',
    END_TIME_                 datetime(3)             null comment '结束时间',
    COMPLETED_BY_             varchar(255)            null comment '完成人',
    DURATION_                 bigint                  null comment '持续时间(毫秒)',
    DELETE_REASON_            varchar(4000)           null comment '删除原因',
    PRIORITY_                 int                     null comment '优先级',
    IN_PROGRESS_DUE_DATE_     datetime(3)             null comment '进行中截止日期',
    DUE_DATE_                 datetime(3)             null comment '截止日期',
    FORM_KEY_                 varchar(255)            null comment '表单键',
    CATEGORY_                 varchar(255)            null comment '任务分类',
    TENANT_ID_                varchar(255) default '' null comment '租户ID',
    LAST_UPDATED_TIME_        datetime(3)             null comment '最后更新时间',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '历史任务实例表';

create index ACT_IDX_HI_TASK_INST_PROCINST
    on act_hi_taskinst (PROC_INST_ID_);

create index ACT_IDX_HI_TASK_SCOPE
    on act_hi_taskinst (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_TASK_SCOPE_DEF
    on act_hi_taskinst (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_TASK_SUB_SCOPE
    on act_hi_taskinst (SUB_SCOPE_ID_, SCOPE_TYPE_);

-- =============================================
-- act_hi_tsk_log: 历史任务日志表
-- 记录任务的操作历史日志
-- =============================================
create table if not exists act_hi_tsk_log
(
    ID_                  bigint auto_increment
        primary key comment '主键ID',
    TYPE_                varchar(64)             null comment '日志类型',
    TASK_ID_             varchar(64)             not null comment '任务ID',
    TIME_STAMP_          timestamp(3)            not null comment '时间戳',
    USER_ID_             varchar(255)            null comment '用户ID',
    DATA_                varchar(4000)           null comment '日志数据',
    EXECUTION_ID_        varchar(64)             null comment '执行ID',
    PROC_INST_ID_        varchar(64)             null comment '流程实例ID',
    PROC_DEF_ID_         varchar(64)             null comment '流程定义ID',
    SCOPE_ID_            varchar(255)            null comment '范围ID',
    SCOPE_DEFINITION_ID_ varchar(255)            null comment '范围定义ID',
    SUB_SCOPE_ID_        varchar(255)            null comment '子范围ID',
    SCOPE_TYPE_          varchar(255)            null comment '范围类型',
    TENANT_ID_           varchar(255) default '' null comment '租户ID'
)
    collate = utf8mb3_bin comment = '历史任务日志表';

create index ACT_IDX_ACT_HI_TSK_LOG_TASK
    on act_hi_tsk_log (TASK_ID_);

-- =============================================
-- act_hi_varinst: 历史变量实例表
-- 存储流程变量的历史实例
-- =============================================
create table if not exists act_hi_varinst
(
    ID_                varchar(64)   not null comment '主键ID',
    REV_               int default 1 null comment '数据版本号',
    PROC_INST_ID_      varchar(64)   null comment '流程实例ID',
    EXECUTION_ID_      varchar(64)   null comment '执行ID',
    TASK_ID_           varchar(64)   null comment '任务ID',
    NAME_              varchar(255)  not null comment '变量名称',
    VAR_TYPE_          varchar(100)  null comment '变量类型',
    SCOPE_ID_          varchar(255)  null comment '范围ID',
    SUB_SCOPE_ID_      varchar(255)  null comment '子范围ID',
    SCOPE_TYPE_        varchar(255)  null comment '范围类型',
    BYTEARRAY_ID_      varchar(64)   null comment '字节数组ID',
    DOUBLE_            double        null comment 'Double类型值',
    LONG_              bigint        null comment 'Long类型值',
    TEXT_              varchar(4000) null comment '文本类型值',
    TEXT2_             varchar(4000) null comment '附加文本值',
    META_INFO_         varchar(4000) null comment '元信息',
    CREATE_TIME_       datetime(3)   null comment '创建时间',
    LAST_UPDATED_TIME_ datetime(3)   null comment '最后更新时间',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '历史变量实例表';

create index ACT_IDX_HI_PROCVAR_EXE
    on act_hi_varinst (EXECUTION_ID_);

create index ACT_IDX_HI_PROCVAR_NAME_TYPE
    on act_hi_varinst (NAME_, VAR_TYPE_);

create index ACT_IDX_HI_PROCVAR_PROC_INST
    on act_hi_varinst (PROC_INST_ID_);

create index ACT_IDX_HI_PROCVAR_TASK_ID
    on act_hi_varinst (TASK_ID_);

create index ACT_IDX_HI_VAR_SCOPE_ID_TYPE
    on act_hi_varinst (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_HI_VAR_SUB_ID_TYPE
    on act_hi_varinst (SUB_SCOPE_ID_, SCOPE_TYPE_);

-- =============================================
-- act_id_bytearray: 身份管理字节数组表
-- 存储身份管理相关的二进制数据
-- =============================================
create table if not exists act_id_bytearray
(
    ID_    varchar(64)  not null comment '主键ID',
    REV_   int           null comment '数据版本号',
    NAME_  varchar(255) null comment '字节数组名称',
    BYTES_ longblob      null comment '字节数据',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '身份管理字节数组表';

-- =============================================
-- act_id_group: 用户组表
-- 存储用户组信息
-- =============================================
create table if not exists act_id_group
(
    ID_   varchar(64)  not null comment '主键ID',
    REV_  int           null comment '数据版本号',
    NAME_ varchar(255) null comment '用户组名称',
    TYPE_ varchar(255) null comment '用户组类型',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '用户组表';

-- =============================================
-- act_id_info: 用户信息表
-- 存储用户的扩展信息
-- =============================================
create table if not exists act_id_info
(
    ID_        varchar(64)  not null comment '主键ID',
    REV_       int           null comment '数据版本号',
    USER_ID_   varchar(64)  null comment '用户ID',
    TYPE_      varchar(64)  null comment '信息类型',
    KEY_       varchar(255) null comment '信息键',
    VALUE_     varchar(255) null comment '信息值',
    PASSWORD_  longblob      null comment '密码',
    PARENT_ID_ varchar(255) null comment '父ID',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '用户信息表';

-- =============================================
-- act_id_priv: 权限表
-- 存储权限信息
-- =============================================
create table if not exists act_id_priv
(
    ID_   varchar(64)  not null comment '主键ID',
    NAME_ varchar(255) not null comment '权限名称',
    primary key (ID_),
    constraint ACT_UNIQ_PRIV_NAME
        unique (NAME_)
)
    collate = utf8mb3_bin comment = '权限表';

-- =============================================
-- act_id_priv_mapping: 权限映射表
-- 存储用户/用户组与权限的映射关系
-- =============================================
create table if not exists act_id_priv_mapping
(
    ID_       varchar(64)  not null comment '主键ID',
    PRIV_ID_  varchar(64)  not null comment '权限ID',
    USER_ID_  varchar(255) null comment '用户ID',
    GROUP_ID_ varchar(255) null comment '用户组ID',
    primary key (ID_),
    constraint ACT_FK_PRIV_MAPPING
        foreign key (PRIV_ID_) references act_id_priv (ID_)
)
    collate = utf8mb3_bin comment = '权限映射表';

create index ACT_IDX_PRIV_GROUP
    on act_id_priv_mapping (GROUP_ID_);

create index ACT_IDX_PRIV_USER
    on act_id_priv_mapping (USER_ID_);

-- =============================================
-- act_id_property: 身份管理属性表
-- 存储身份管理的配置属性
-- =============================================
create table if not exists act_id_property
(
    NAME_  varchar(64)  not null comment '属性名称',
    VALUE_ varchar(300) null comment '属性值',
    REV_   int           null comment '数据版本号',
    primary key (NAME_)
)
    collate = utf8mb3_bin comment = '身份管理属性表';

-- =============================================
-- act_id_token: 令牌表
-- 存储身份令牌信息
-- =============================================
create table if not exists act_id_token
(
    ID_          varchar(64)   not null comment '主键ID',
    REV_         int           null comment '数据版本号',
    TOKEN_VALUE_ varchar(255)  null comment '令牌值',
    TOKEN_DATE_  timestamp(3)  null comment '令牌日期',
    IP_ADDRESS_  varchar(255)  null comment 'IP地址',
    USER_AGENT_  varchar(255)  null comment '用户代理',
    USER_ID_     varchar(255)  null comment '用户ID',
    TOKEN_DATA_  varchar(2000) null comment '令牌数据',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '令牌表';

-- =============================================
-- act_id_user: 用户表
-- 存储用户信息
-- =============================================
create table if not exists act_id_user
(
    ID_           varchar(64)             not null comment '主键ID',
    REV_          int                     null comment '数据版本号',
    FIRST_        varchar(255)            null comment '名',
    LAST_         varchar(255)            null comment '姓',
    DISPLAY_NAME_ varchar(255)            null comment '显示名称',
    EMAIL_        varchar(255)            null comment '邮箱',
    PWD_         varchar(255)            null comment '密码',
    PICTURE_ID_   varchar(64)             null comment '头像ID',
    TENANT_ID_    varchar(255) default '' null comment '租户ID',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '用户表';

-- =============================================
-- act_id_membership: 用户组成员表
-- 存储用户与用户组的关系
-- =============================================
create table if not exists act_id_membership
(
    USER_ID_  varchar(64) not null comment '用户ID',
    GROUP_ID_ varchar(64) not null comment '用户组ID',
    primary key (USER_ID_, GROUP_ID_),
    constraint ACT_FK_MEMB_GROUP
        foreign key (GROUP_ID_) references act_id_group (ID_),
    constraint ACT_FK_MEMB_USER
        foreign key (USER_ID_) references act_id_user (ID_)
)
    collate = utf8mb3_bin comment = '用户组成员表';

-- =============================================
-- act_re_deployment: 部署表
-- 存储流程定义的部署信息
-- =============================================
create table if not exists act_re_deployment
(
    ID_                   varchar(64)             not null comment '主键ID',
    NAME_                 varchar(255)            null comment '部署名称',
    CATEGORY_             varchar(255)            null comment '部署分类',
    KEY_                  varchar(255)            null comment '部署键',
    TENANT_ID_            varchar(255) default '' null comment '租户ID',
    DEPLOY_TIME_          timestamp(3)            null comment '部署时间',
    DERIVED_FROM_         varchar(64)             null comment '衍生自',
    DERIVED_FROM_ROOT_    varchar(64)             null comment '衍生自根',
    PARENT_DEPLOYMENT_ID_ varchar(255)            null comment '父部署ID',
    ENGINE_VERSION_       varchar(255)            null comment '引擎版本',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '部署表';

-- =============================================
-- act_ge_bytearray: 通用字节数组表
-- 存储通用的二进制数据，如流程定义文件、图片等
-- =============================================
create table if not exists act_ge_bytearray
(
    ID_            varchar(64)  not null comment '主键ID',
    REV_           int           null comment '数据版本号',
    NAME_          varchar(255) null comment '字节数组名称',
    DEPLOYMENT_ID_ varchar(64)  null comment '部署ID',
    BYTES_         longblob      null comment '字节数据',
    GENERATED_     tinyint      null comment '是否自动生成',
    primary key (ID_),
    constraint ACT_FK_BYTEARR_DEPL
        foreign key (DEPLOYMENT_ID_) references act_re_deployment (ID_)
)
    collate = utf8mb3_bin comment = '通用字节数组表';

-- =============================================
-- act_re_model: 模型表
-- 存储流程模型信息
-- =============================================
create table if not exists act_re_model
(
    ID_                           varchar(64)             not null comment '主键ID',
    REV_                          int                     null comment '数据版本号',
    NAME_                         varchar(255)            null comment '模型名称',
    KEY_                          varchar(255)            null comment '模型键',
    CATEGORY_                     varchar(255)            null comment '模型分类',
    CREATE_TIME_                  timestamp(3)            null comment '创建时间',
    LAST_UPDATE_TIME_             timestamp(3)            null comment '最后更新时间',
    VERSION_                      int                     null comment '版本号',
    META_INFO_                    varchar(4000)           null comment '元信息',
    DEPLOYMENT_ID_                varchar(64)             null comment '部署ID',
    EDITOR_SOURCE_VALUE_ID_       varchar(64)             null comment '编辑器源值ID',
    EDITOR_SOURCE_EXTRA_VALUE_ID_ varchar(64)             null comment '编辑器源附加值ID',
    TENANT_ID_                    varchar(255) default '' null comment '租户ID',
    primary key (ID_),
    constraint ACT_FK_MODEL_DEPLOYMENT
        foreign key (DEPLOYMENT_ID_) references act_re_deployment (ID_),
    constraint ACT_FK_MODEL_SOURCE
        foreign key (EDITOR_SOURCE_VALUE_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_MODEL_SOURCE_EXTRA
        foreign key (EDITOR_SOURCE_EXTRA_VALUE_ID_) references act_ge_bytearray (ID_)
)
    collate = utf8mb3_bin comment = '模型表';

-- =============================================
-- act_re_procdef: 流程定义表
-- 存储流程定义信息
-- =============================================
create table if not exists act_re_procdef
(
    ID_                     varchar(64)             not null comment '主键ID',
    REV_                    int                     null comment '数据版本号',
    CATEGORY_               varchar(255)            null comment '流程分类',
    NAME_                   varchar(255)            null comment '流程名称',
    KEY_                    varchar(255)            not null comment '流程键',
    VERSION_                int                     not null comment '版本号',
    DEPLOYMENT_ID_          varchar(64)             null comment '部署ID',
    RESOURCE_NAME_          varchar(4000)           null comment '资源名称',
    DGRM_RESOURCE_NAME_     varchar(4000)           null comment '流程图资源名称',
    DESCRIPTION_            varchar(4000)           null comment '流程描述',
    HAS_START_FORM_KEY_     tinyint                 null comment '是否有开始表单键',
    HAS_GRAPHICAL_NOTATION_ tinyint                 null comment '是否有图形符号',
    SUSPENSION_STATE_       int                     null comment '挂起状态',
    TENANT_ID_              varchar(255) default '' null comment '租户ID',
    ENGINE_VERSION_       varchar(255)            null comment '引擎版本',
    DERIVED_FROM_         varchar(64)             null comment '衍生自',
    DERIVED_FROM_ROOT_    varchar(64)             null comment '衍生自根',
    DERIVED_VERSION_        int          default 0 not null comment '衍生版本',
    primary key (ID_),
    constraint ACT_UNIQ_PROCDEF
        unique (KEY_, VERSION_, DERIVED_VERSION_, TENANT_ID_)
)
    collate = utf8mb3_bin comment = '流程定义表';

-- =============================================
-- act_procdef_info: 流程定义信息表
-- 存储流程定义的附加信息
-- =============================================
create table if not exists act_procdef_info
(
    ID_           varchar(64) not null comment '主键ID',
    PROC_DEF_ID_  varchar(64) not null comment '流程定义ID',
    REV_          int         null comment '数据版本号',
    INFO_JSON_ID_ varchar(64) null comment '信息JSON ID',
    primary key (ID_),
    constraint ACT_UNIQ_INFO_PROCDEF
        unique (PROC_DEF_ID_),
    constraint ACT_FK_INFO_JSON_BA
        foreign key (INFO_JSON_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_INFO_PROCDEF
        foreign key (PROC_DEF_ID_) references act_re_procdef (ID_)
)
    collate = utf8mb3_bin comment = '流程定义信息表';

create index ACT_IDX_INFO_PROCDEF
    on act_procdef_info (PROC_DEF_ID_);

-- =============================================
-- act_ru_actinst: 运行时活动实例表
-- 存储当前运行的活动实例
-- =============================================
create table if not exists act_ru_actinst
(
    ID_                varchar(64)             not null comment '主键ID',
    REV_               int          default 1 null comment '数据版本号',
    PROC_DEF_ID_       varchar(64)             not null comment '流程定义ID',
    PROC_INST_ID_      varchar(64)             not null comment '流程实例ID',
    EXECUTION_ID_      varchar(64)             not null comment '执行ID',
    ACT_ID_            varchar(255)            not null comment '活动ID',
    TASK_ID_           varchar(64)             null comment '任务ID',
    CALL_PROC_INST_ID_ varchar(64)             null comment '调用的流程实例ID',
    ACT_NAME_          varchar(255)            null comment '活动名称',
    ACT_TYPE_          varchar(255)            not null comment '活动类型',
    ASSIGNEE_          varchar(255)            null comment '执行人',
    START_TIME_        datetime(3)             not null comment '开始时间',
    END_TIME_          datetime(3)             null comment '结束时间',
    DURATION_          bigint                  null comment '持续时间(毫秒)',
    TRANSACTION_ORDER_ int                     null comment '事务顺序',
    DELETE_REASON_     varchar(4000)           null comment '删除原因',
    TENANT_ID_         varchar(255) default '' null comment '租户ID',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '运行时活动实例表';

create index ACT_IDX_RU_ACTI_END
    on act_ru_actinst (END_TIME_);

create index ACT_IDX_RU_ACTI_EXEC
    on act_ru_actinst (EXECUTION_ID_);

create index ACT_IDX_RU_ACTI_EXEC_ACT
    on act_ru_actinst (EXECUTION_ID_, ACT_ID_);

create index ACT_IDX_RU_ACTI_PROC
    on act_ru_actinst (PROC_INST_ID_);

create index ACT_IDX_RU_ACTI_PROC_ACT
    on act_ru_actinst (PROC_INST_ID_, ACT_ID_);

create index ACT_IDX_RU_ACTI_START
    on act_ru_actinst (START_TIME_);

create index ACT_IDX_RU_ACTI_TASK
    on act_ru_actinst (TASK_ID_);

-- =============================================
-- act_ru_entitylink: 运行时实体链接表
-- 存储流程与其他实体的关联关系
-- =============================================
create table if not exists act_ru_entitylink
(
    ID_                      varchar(64)  not null comment '主键ID',
    REV_                     int           null comment '数据版本号',
    CREATE_TIME_             datetime(3)  null comment '创建时间',
    LINK_TYPE_               varchar(255) null comment '链接类型',
    SCOPE_ID_                varchar(255) null comment '范围ID',
    SUB_SCOPE_ID_            varchar(255) null comment '子范围ID',
    SCOPE_TYPE_              varchar(255) null comment '范围类型',
    SCOPE_DEFINITION_ID_     varchar(255) null comment '范围定义ID',
    PARENT_ELEMENT_ID_       varchar(255) null comment '父元素ID',
    REF_SCOPE_ID_            varchar(255) null comment '引用范围ID',
    REF_SCOPE_TYPE_          varchar(255) null comment '引用范围类型',
    REF_SCOPE_DEFINITION_ID_ varchar(255) null comment '引用范围定义ID',
    ROOT_SCOPE_ID_           varchar(255) null comment '根范围ID',
    ROOT_SCOPE_TYPE_         varchar(255) null comment '根范围类型',
    HIERARCHY_TYPE_          varchar(255) null comment '层级类型',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '运行时实体链接表';

create index ACT_IDX_ENT_LNK_REF_SCOPE
    on act_ru_entitylink (REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_ENT_LNK_ROOT_SCOPE
    on act_ru_entitylink (ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_ENT_LNK_SCOPE
    on act_ru_entitylink (SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);

create index ACT_IDX_ENT_LNK_SCOPE_DEF
    on act_ru_entitylink (SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);

-- =============================================
-- act_ru_execution: 运行时执行表
-- 存储流程执行实例
-- =============================================
create table if not exists act_ru_execution
(
    ID_                        varchar(64)             not null comment '主键ID',
    REV_                       int                     null comment '数据版本号',
    PROC_INST_ID_              varchar(64)             null comment '流程实例ID',
    BUSINESS_KEY_              varchar(255)            null comment '业务键',
    PARENT_ID_                 varchar(64)             null comment '父执行ID',
    PROC_DEF_ID_               varchar(64)             null comment '流程定义ID',
    SUPER_EXEC_                varchar(64)             null comment '上级执行ID',
    ROOT_PROC_INST_ID_         varchar(64)             null comment '根流程实例ID',
    ACT_ID_                    varchar(255)            null comment '当前活动ID',
    IS_ACTIVE_                 tinyint                 null comment '是否活动',
    IS_CONCURRENT_             tinyint                 null comment '是否并发',
    IS_SCOPE_                  tinyint                 null comment '是否是范围',
    IS_EVENT_SCOPE_            tinyint                 null comment '是否是事件范围',
    IS_MI_ROOT_                tinyint                 null comment '是否是多实例根',
    SUSPENSION_STATE_       int                     null comment '挂起状态',
    CACHED_ENT_STATE_          int                     null comment '缓存的实体状态',
    TENANT_ID_                 varchar(255) default '' null comment '租户ID',
    NAME_                      varchar(255)            null comment '执行名称',
    START_ACT_ID_              varchar(255)            null comment '开始活动ID',
    START_TIME_                datetime(3)             null comment '开始时间',
    START_USER_ID_             varchar(255)            null comment '启动用户ID',
    LOCK_TIME_                 timestamp(3)            null comment '锁定时间',
    LOCK_OWNER_                varchar(255)            null comment '锁持有者',
    IS_COUNT_ENABLED_          tinyint                 null comment '是否启用计数',
    EVT_SUBSCR_COUNT_          int                     null comment '事件订阅计数',
    TASK_COUNT_                int                     null comment '任务计数',
    JOB_COUNT_                 int                     null comment '作业计数',
    TIMER_JOB_COUNT_           int                     null comment '定时器作业计数',
    SUSP_JOB_COUNT_            int                     null comment '挂起作业计数',
    DEADLETTER_JOB_COUNT_      int                     null comment '死信作业计数',
    EXTERNAL_WORKER_JOB_COUNT_ int                     null comment '外部工作者作业计数',
    VAR_COUNT_                 int                     null comment '变量计数',
    ID_LINK_COUNT_             int                     null comment '身份关系计数',
    CALLBACK_ID_               varchar(255)            null comment '回调ID',
    CALLBACK_TYPE_             varchar(255)            null comment '回调类型',
    REFERENCE_ID_              varchar(255)            null comment '引用ID',
    REFERENCE_TYPE_            varchar(255)            null comment '引用类型',
    PROPAGATED_STAGE_INST_ID_  varchar(255)            null comment '传播的阶段实例ID',
    BUSINESS_STATUS_           varchar(255)            null comment '业务状态',
    primary key (ID_),
    constraint ACT_FK_EXE_PARENT
        foreign key (PARENT_ID_) references act_ru_execution (ID_)
            on delete cascade,
    constraint ACT_FK_EXE_PROCDEF
        foreign key (PROC_DEF_ID_) references act_re_procdef (ID_),
    constraint ACT_FK_EXE_PROCINST
        foreign key (PROC_INST_ID_) references act_ru_execution (ID_)
            on update cascade on delete cascade,
    constraint ACT_FK_EXE_SUPER
        foreign key (SUPER_EXEC_) references act_ru_execution (ID_)
            on delete cascade
)
    collate = utf8mb3_bin comment = '运行时执行表';

-- =============================================
-- act_ru_deadletter_job: 死信作业表
-- 存储执行失败的作业
-- =============================================
create table if not exists act_ru_deadletter_job
(
    ID_                  varchar(64)             not null comment '主键ID',
    REV_                 int                     null comment '数据版本号',
    CATEGORY_            varchar(255)            null comment '作业分类',
    TYPE_                varchar(255)            not null comment '作业类型',
    EXCLUSIVE_           tinyint(1)              null comment '是否排他',
    EXECUTION_ID_        varchar(64)             null comment '执行ID',
    PROCESS_INSTANCE_ID_ varchar(64)             null comment '流程实例ID',
    PROC_DEF_ID_         varchar(64)             null comment '流程定义ID',
    ELEMENT_ID_          varchar(255)            null comment '元素ID',
    ELEMENT_NAME_        varchar(255)            null comment '元素名称',
    SCOPE_ID_            varchar(255)            null comment '范围ID',
    SUB_SCOPE_ID_        varchar(255)            null comment '子范围ID',
    SCOPE_TYPE_          varchar(255)            null comment '范围类型',
    SCOPE_DEFINITION_ID_ varchar(255)            null comment '范围定义ID',
    CORRELATION_ID_      varchar(255)            null comment '关联ID',
    EXCEPTION_STACK_ID_  varchar(64)             null comment '异常堆栈ID',
    EXCEPTION_MSG_       varchar(4000)           null comment '异常消息',
    DUEDATE_             timestamp(3)            null comment '到期时间',
    REPEAT_              varchar(255)            null comment '重复表达式',
    HANDLER_TYPE_        varchar(255)            null comment '处理器类型',
    HANDLER_CFG_         varchar(4000)           null comment '处理器配置',
    CUSTOM_VALUES_ID_    varchar(64)             null comment '自定义值ID',
    CREATE_TIME_         timestamp(3)            null comment '创建时间',
    TENANT_ID_           varchar(255) default '' null comment '租户ID',
    primary key (ID_),
    constraint ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES
        foreign key (CUSTOM_VALUES_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_DEADLETTER_JOB_EXCEPTION
        foreign key (EXCEPTION_STACK_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_DEADLETTER_JOB_EXECUTION
        foreign key (EXECUTION_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE
        foreign key (PROCESS_INSTANCE_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_DEADLETTER_JOB_PROC_DEF
        foreign key (PROC_DEF_ID_) references act_re_procdef (ID_)
)
    collate = utf8mb3_bin comment = '死信作业表';

create index ACT_IDX_DEADLETTER_JOB_CORRELATION_ID
    on act_ru_deadletter_job (CORRELATION_ID_);

create index ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID
    on act_ru_deadletter_job (CUSTOM_VALUES_ID_);

create index ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID
    on act_ru_deadletter_job (EXCEPTION_STACK_ID_);

create index ACT_IDX_DJOB_SCOPE
    on act_ru_deadletter_job (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_DJOB_SCOPE_DEF
    on act_ru_deadletter_job (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_DJOB_SUB_SCOPE
    on act_ru_deadletter_job (SUB_SCOPE_ID_, SCOPE_TYPE_);

-- =============================================
-- act_ru_event_subscr: 事件订阅表
-- 存储事件订阅信息
-- =============================================
create table if not exists act_ru_event_subscr
(
    ID_                   varchar(64)                               not null comment '主键ID',
    REV_                  int                                       null comment '数据版本号',
    EVENT_TYPE_           varchar(255)                              not null comment '事件类型',
    EVENT_NAME_           varchar(255)                              null comment '事件名称',
    EXECUTION_ID_         varchar(64)                               null comment '执行ID',
    PROC_INST_ID_         varchar(64)                               null comment '流程实例ID',
    ACTIVITY_ID_          varchar(64)                               null comment '活动ID',
    CONFIGURATION_        varchar(255)                              null comment '配置',
    CREATED_              timestamp(3) default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    PROC_DEF_ID_          varchar(64)                               null comment '流程定义ID',
    SUB_SCOPE_ID_         varchar(64)                               null comment '子范围ID',
    SCOPE_ID_             varchar(64)                               null comment '范围ID',
    SCOPE_DEFINITION_ID_  varchar(64)                               null comment '范围定义ID',
    SCOPE_DEFINITION_KEY_ varchar(255)                              null comment '范围定义键',
    SCOPE_TYPE_           varchar(64)                               null comment '范围类型',
    LOCK_TIME_            timestamp(3)                              null comment '锁定时间',
    LOCK_OWNER_           varchar(255)                              null comment '锁持有者',
    TENANT_ID_            varchar(255) default ''                   null comment '租户ID',
    primary key (ID_),
    constraint ACT_FK_EVENT_EXEC
        foreign key (EXECUTION_ID_) references act_ru_execution (ID_)
)
    collate = utf8mb3_bin comment = '事件订阅表';

create index ACT_IDX_EVENT_SUBSCR_CONFIG_
    on act_ru_event_subscr (CONFIGURATION_);

create index ACT_IDX_EVENT_SUBSCR_EXEC_ID
    on act_ru_event_subscr (EXECUTION_ID_);

create index ACT_IDX_EVENT_SUBSCR_PROC_ID
    on act_ru_event_subscr (PROC_INST_ID_);

create index ACT_IDX_EVENT_SUBSCR_SCOPEREF_
    on act_ru_event_subscr (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDC_EXEC_ROOT
    on act_ru_execution (ROOT_PROC_INST_ID_);

create index ACT_IDX_EXEC_BUSKEY
    on act_ru_execution (BUSINESS_KEY_);

create index ACT_IDX_EXEC_REF_ID_
    on act_ru_execution (REFERENCE_ID_);

-- =============================================
-- act_ru_external_job: 外部工作者作业表
-- 存储外部工作者作业
-- =============================================
create table if not exists act_ru_external_job
(
    ID_                  varchar(64)             not null comment '主键ID',
    REV_                 int                     null comment '数据版本号',
    CATEGORY_            varchar(255)            null comment '作业分类',
    TYPE_                varchar(255)            not null comment '作业类型',
    LOCK_EXP_TIME_       timestamp(3)            null comment '锁过期时间',
    LOCK_OWNER_          varchar(255)            null comment '锁持有者',
    EXCLUSIVE_           tinyint(1)              null comment '是否排他',
    EXECUTION_ID_        varchar(64)             null comment '执行ID',
    PROCESS_INSTANCE_ID_ varchar(64)             null comment '流程实例ID',
    PROC_DEF_ID_         varchar(64)             null comment '流程定义ID',
    ELEMENT_ID_          varchar(255)            null comment '元素ID',
    ELEMENT_NAME_        varchar(255)            null comment '元素名称',
    SCOPE_ID_            varchar(255)            null comment '范围ID',
    SUB_SCOPE_ID_        varchar(255)            null comment '子范围ID',
    SCOPE_TYPE_          varchar(255)            null comment '范围类型',
    SCOPE_DEFINITION_ID_ varchar(255)            null comment '范围定义ID',
    CORRELATION_ID_      varchar(255)            null comment '关联ID',
    RETRIES_             int                     null comment '重试次数',
    EXCEPTION_STACK_ID_  varchar(64)             null comment '异常堆栈ID',
    EXCEPTION_MSG_       varchar(4000)           null comment '异常消息',
    DUEDATE_             timestamp(3)            null comment '到期时间',
    REPEAT_              varchar(255)            null comment '重复表达式',
    HANDLER_TYPE_        varchar(255)            null comment '处理器类型',
    HANDLER_CFG_         varchar(4000)           null comment '处理器配置',
    CUSTOM_VALUES_ID_    varchar(64)             null comment '自定义值ID',
    CREATE_TIME_         timestamp(3)            null comment '创建时间',
    TENANT_ID_           varchar(255) default '' null comment '租户ID',
    primary key (ID_),
    constraint ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES
        foreign key (CUSTOM_VALUES_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_EXTERNAL_JOB_EXCEPTION
        foreign key (EXCEPTION_STACK_ID_) references act_ge_bytearray (ID_)
)
    collate = utf8mb3_bin comment = '外部工作者作业表';

create index ACT_IDX_EJOB_SCOPE
    on act_ru_external_job (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_EJOB_SCOPE_DEF
    on act_ru_external_job (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_EJOB_SUB_SCOPE
    on act_ru_external_job (SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_EXTERNAL_JOB_CORRELATION_ID
    on act_ru_external_job (CORRELATION_ID_);

create index ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID
    on act_ru_external_job (CUSTOM_VALUES_ID_);

create index ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID
    on act_ru_external_job (EXCEPTION_STACK_ID_);

-- =============================================
-- act_ru_history_job: 历史作业表
-- 存储用于生成历史数据的作业
-- =============================================
create table if not exists act_ru_history_job
(
    ID_                 varchar(64)             not null comment '主键ID',
    REV_                int                     null comment '数据版本号',
    LOCK_EXP_TIME_      timestamp(3)            null comment '锁过期时间',
    LOCK_OWNER_         varchar(255)            null comment '锁持有者',
    RETRIES_            int                     null comment '重试次数',
    EXCEPTION_STACK_ID_ varchar(64)             null comment '异常堆栈ID',
    EXCEPTION_MSG_      varchar(4000)           null comment '异常消息',
    HANDLER_TYPE_       varchar(255)            null comment '处理器类型',
    HANDLER_CFG_        varchar(4000)           null comment '处理器配置',
    CUSTOM_VALUES_ID_   varchar(64)             null comment '自定义值ID',
    ADV_HANDLER_CFG_ID_ varchar(64)             null comment '高级处理器配置ID',
    CREATE_TIME_        timestamp(3)            null comment '创建时间',
    SCOPE_TYPE_         varchar(255)            null comment '范围类型',
    TENANT_ID_          varchar(255) default '' null comment '租户ID',
    primary key (ID_)
)
    collate = utf8mb3_bin comment = '历史作业表';

-- =============================================
-- act_ru_job: 运行时作业表
-- 存储运行时作业
-- =============================================
create table if not exists act_ru_job
(
    ID_                  varchar(64)             not null comment '主键ID',
    REV_                 int                     null comment '数据版本号',
    CATEGORY_            varchar(255)            null comment '作业分类',
    TYPE_                varchar(255)            not null comment '作业类型',
    LOCK_EXP_TIME_       timestamp(3)            null comment '锁过期时间',
    LOCK_OWNER_          varchar(255)            null comment '锁持有者',
    EXCLUSIVE_           tinyint(1)              null comment '是否排他',
    EXECUTION_ID_        varchar(64)             null comment '执行ID',
    PROCESS_INSTANCE_ID_ varchar(64)             null comment '流程实例ID',
    PROC_DEF_ID_         varchar(64)             null comment '流程定义ID',
    ELEMENT_ID_          varchar(255)            null comment '元素ID',
    ELEMENT_NAME_        varchar(255)            null comment '元素名称',
    SCOPE_ID_            varchar(255)            null comment '范围ID',
    SUB_SCOPE_ID_        varchar(255)            null comment '子范围ID',
    SCOPE_TYPE_          varchar(255)            null comment '范围类型',
    SCOPE_DEFINITION_ID_ varchar(255)            null comment '范围定义ID',
    CORRELATION_ID_      varchar(255)            null comment '关联ID',
    RETRIES_             int                     null comment '重试次数',
    EXCEPTION_STACK_ID_  varchar(64)             null comment '异常堆栈ID',
    EXCEPTION_MSG_       varchar(4000)           null comment '异常消息',
    DUEDATE_             timestamp(3)            null comment '到期时间',
    REPEAT_              varchar(255)            null comment '重复表达式',
    HANDLER_TYPE_        varchar(255)            null comment '处理器类型',
    HANDLER_CFG_         varchar(4000)           null comment '处理器配置',
    CUSTOM_VALUES_ID_    varchar(64)             null comment '自定义值ID',
    CREATE_TIME_         timestamp(3)            null comment '创建时间',
    TENANT_ID_           varchar(255) default '' null comment '租户ID',
    primary key (ID_),
    constraint ACT_FK_JOB_CUSTOM_VALUES
        foreign key (CUSTOM_VALUES_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_JOB_EXCEPTION
        foreign key (EXCEPTION_STACK_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_JOB_EXECUTION
        foreign key (EXECUTION_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_JOB_PROCESS_INSTANCE
        foreign key (PROCESS_INSTANCE_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_JOB_PROC_DEF
        foreign key (PROC_DEF_ID_) references act_re_procdef (ID_)
)
    collate = utf8mb3_bin comment = '运行时作业表';

create index ACT_IDX_JOB_CORRELATION_ID
    on act_ru_job (CORRELATION_ID_);

create index ACT_IDX_JOB_CUSTOM_VALUES_ID
    on act_ru_job (CUSTOM_VALUES_ID_);

create index ACT_IDX_JOB_EXCEPTION_STACK_ID
    on act_ru_job (EXCEPTION_STACK_ID_);

create index ACT_IDX_JOB_SCOPE
    on act_ru_job (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_JOB_SCOPE_DEF
    on act_ru_job (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_JOB_SUB_SCOPE
    on act_ru_job (SUB_SCOPE_ID_, SCOPE_TYPE_);

-- =============================================
-- act_ru_suspended_job: 挂起作业表
-- 存储挂起的作业
-- =============================================
create table if not exists act_ru_suspended_job
(
    ID_                  varchar(64)             not null comment '主键ID',
    REV_                 int                     null comment '数据版本号',
    CATEGORY_            varchar(255)            null comment '作业分类',
    TYPE_                varchar(255)            not null comment '作业类型',
    EXCLUSIVE_           tinyint(1)              null comment '是否排他',
    EXECUTION_ID_        varchar(64)             null comment '执行ID',
    PROCESS_INSTANCE_ID_ varchar(64)             null comment '流程实例ID',
    PROC_DEF_ID_         varchar(64)             null comment '流程定义ID',
    ELEMENT_ID_          varchar(255)            null comment '元素ID',
    ELEMENT_NAME_        varchar(255)            null comment '元素名称',
    SCOPE_ID_            varchar(255)            null comment '范围ID',
    SUB_SCOPE_ID_        varchar(255)            null comment '子范围ID',
    SCOPE_TYPE_          varchar(255)            null comment '范围类型',
    SCOPE_DEFINITION_ID_ varchar(255)            null comment '范围定义ID',
    CORRELATION_ID_      varchar(255)            null comment '关联ID',
    RETRIES_             int                     null comment '重试次数',
    EXCEPTION_STACK_ID_  varchar(64)             null comment '异常堆栈ID',
    EXCEPTION_MSG_       varchar(4000)           null comment '异常消息',
    DUEDATE_             timestamp(3)            null comment '到期时间',
    REPEAT_              varchar(255)            null comment '重复表达式',
    HANDLER_TYPE_        varchar(255)            null comment '处理器类型',
    HANDLER_CFG_         varchar(4000)           null comment '处理器配置',
    CUSTOM_VALUES_ID_    varchar(64)             null comment '自定义值ID',
    CREATE_TIME_         timestamp(3)            null comment '创建时间',
    TENANT_ID_           varchar(255) default '' null comment '租户ID',
    primary key (ID_),
    constraint ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES
        foreign key (CUSTOM_VALUES_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_SUSPENDED_JOB_EXCEPTION
        foreign key (EXCEPTION_STACK_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_SUSPENDED_JOB_EXECUTION
        foreign key (EXECUTION_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE
        foreign key (PROCESS_INSTANCE_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_SUSPENDED_JOB_PROC_DEF
        foreign key (PROC_DEF_ID_) references act_re_procdef (ID_)
)
    collate = utf8mb3_bin comment = '挂起作业表';

create index ACT_IDX_SJOB_SCOPE
    on act_ru_suspended_job (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_SJOB_SCOPE_DEF
    on act_ru_suspended_job (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_SJOB_SUB_SCOPE
    on act_ru_suspended_job (SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_SUSPENDED_JOB_CORRELATION_ID
    on act_ru_suspended_job (CORRELATION_ID_);

create index ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID
    on act_ru_suspended_job (CUSTOM_VALUES_ID_);

create index ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID
    on act_ru_suspended_job (EXCEPTION_STACK_ID_);

-- =============================================
-- act_ru_task: 运行时任务表
-- 存储当前运行的任务
-- =============================================
create table if not exists act_ru_task
(
    ID_                       varchar(64)             not null comment '主键ID',
    REV_                      int                     null comment '数据版本号',
    EXECUTION_ID_             varchar(64)             null comment '执行ID',
    PROC_INST_ID_             varchar(64)             null comment '流程实例ID',
    PROC_DEF_ID_              varchar(64)             null comment '流程定义ID',
    TASK_DEF_ID_              varchar(64)             null comment '任务定义ID',
    SCOPE_ID_                 varchar(255)            null comment '范围ID',
    SUB_SCOPE_ID_             varchar(255)            null comment '子范围ID',
    SCOPE_TYPE_               varchar(255)            null comment '范围类型',
    SCOPE_DEFINITION_ID_      varchar(255)            null comment '范围定义ID',
    PROPAGATED_STAGE_INST_ID_ varchar(255)            null comment '传播的阶段实例ID',
    STATE_                    varchar(255)            null comment '任务状态',
    NAME_                     varchar(255)            null comment '任务名称',
    PARENT_TASK_ID_           varchar(64)             null comment '父任务ID',
    DESCRIPTION_              varchar(4000)           null comment '任务描述',
    TASK_DEF_KEY_             varchar(255)            null comment '任务定义键',
    OWNER_                    varchar(255)            null comment '任务所有者',
    ASSIGNEE_                 varchar(255)            null comment '任务执行人',
    DELEGATION_               varchar(64)             null comment '委托状态',
    PRIORITY_                 int                     null comment '优先级',
    CREATE_TIME_              timestamp(3)            null comment '创建时间',
    IN_PROGRESS_TIME_         datetime(3)             null comment '进行中时间',
    IN_PROGRESS_STARTED_BY_   varchar(255)            null comment '进行中开始人',
    CLAIM_TIME_               datetime(3)             null comment '签收时间',
    CLAIMED_BY_               varchar(255)            null comment '签收人',
    SUSPENDED_TIME_           datetime(3)             null comment '挂起时间',
    SUSPENDED_BY_             varchar(255)            null comment '挂起人',
    IN_PROGRESS_DUE_DATE_     datetime(3)             null comment '进行中截止日期',
    DUE_DATE_                 datetime(3)             null comment '截止日期',
    CATEGORY_                 varchar(255)            null comment '任务分类',
    SUSPENSION_STATE_         int                     null comment '挂起状态',
    TENANT_ID_                varchar(255) default '' null comment '租户ID',
    FORM_KEY_                 varchar(255)            null comment '表单键',
    IS_COUNT_ENABLED_         tinyint                 null comment '是否启用计数',
    VAR_COUNT_                int                     null comment '变量计数',
    ID_LINK_COUNT_            int                     null comment '身份关系计数',
    SUB_TASK_COUNT_           int                     null comment '子任务计数',
    primary key (ID_),
    constraint ACT_FK_TASK_EXE
        foreign key (EXECUTION_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_TASK_PROCDEF
        foreign key (PROC_DEF_ID_) references act_re_procdef (ID_),
    constraint ACT_FK_TASK_PROCINST
        foreign key (PROC_INST_ID_) references act_ru_execution (ID_)
)
    collate = utf8mb3_bin comment = '运行时任务表';

-- =============================================
-- act_ru_identitylink: 运行时身份关系表
-- 存储运行时身份关系
-- =============================================
create table if not exists act_ru_identitylink
(
    ID_                  varchar(64)  not null comment '主键ID',
    REV_                 int          null comment '数据版本号',
    GROUP_ID_            varchar(255) null comment '用户组ID',
    TYPE_                varchar(255) null comment '关系类型',
    USER_ID_             varchar(255) null comment '用户ID',
    TASK_ID_             varchar(64)  null comment '任务ID',
    PROC_INST_ID_        varchar(64)  null comment '流程实例ID',
    PROC_DEF_ID_         varchar(64)  null comment '流程定义ID',
    SCOPE_ID_            varchar(255) null comment '范围ID',
    SUB_SCOPE_ID_        varchar(255) null comment '子范围ID',
    SCOPE_TYPE_          varchar(255) null comment '范围类型',
    SCOPE_DEFINITION_ID_ varchar(255) null comment '范围定义ID',
    primary key (ID_),
    constraint ACT_FK_ATHRZ_PROCEDEF
        foreign key (PROC_DEF_ID_) references act_re_procdef (ID_),
    constraint ACT_FK_IDL_PROCINST
        foreign key (PROC_INST_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_TSKASS_TASK
        foreign key (TASK_ID_) references act_ru_task (ID_)
)
    collate = utf8mb3_bin comment = '运行时身份关系表';

create index ACT_IDX_ATHRZ_PROCEDEF
    on act_ru_identitylink (PROC_DEF_ID_);

create index ACT_IDX_IDENT_LNK_GROUP
    on act_ru_identitylink (GROUP_ID_);

create index ACT_IDX_IDENT_LNK_SCOPE
    on act_ru_identitylink (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_IDENT_LNK_SCOPE_DEF
    on act_ru_identitylink (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_IDENT_LNK_SUB_SCOPE
    on act_ru_identitylink (SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_IDENT_LNK_USER
    on act_ru_identitylink (USER_ID_);

create index ACT_IDX_TASK_CREATE
    on act_ru_task (CREATE_TIME_);

create index ACT_IDX_TASK_SCOPE
    on act_ru_task (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_TASK_SCOPE_DEF
    on act_ru_task (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_TASK_SUB_SCOPE
    on act_ru_task (SUB_SCOPE_ID_, SCOPE_TYPE_);

-- =============================================
-- act_ru_timer_job: 定时器作业表
-- 存储定时器作业
-- =============================================
create table if not exists act_ru_timer_job
(
    ID_                  varchar(64)             not null comment '主键ID',
    REV_                 int                     null comment '数据版本号',
    CATEGORY_            varchar(255)            null comment '作业分类',
    TYPE_                varchar(255)            not null comment '作业类型',
    LOCK_EXP_TIME_       timestamp(3)            null comment '锁过期时间',
    LOCK_OWNER_          varchar(255)            null comment '锁持有者',
    EXCLUSIVE_           tinyint(1)              null comment '是否排他',
    EXECUTION_ID_        varchar(64)             null comment '执行ID',
    PROCESS_INSTANCE_ID_ varchar(64)             null comment '流程实例ID',
    PROC_DEF_ID_         varchar(64)             null comment '流程定义ID',
    ELEMENT_ID_          varchar(255)            null comment '元素ID',
    ELEMENT_NAME_        varchar(255)            null comment '元素名称',
    SCOPE_ID_            varchar(255)            null comment '范围ID',
    SUB_SCOPE_ID_        varchar(255)            null comment '子范围ID',
    SCOPE_TYPE_          varchar(255)            null comment '范围类型',
    SCOPE_DEFINITION_ID_ varchar(255)            null comment '范围定义ID',
    CORRELATION_ID_      varchar(255)            null comment '关联ID',
    RETRIES_             int                     null comment '重试次数',
    EXCEPTION_STACK_ID_  varchar(64)             null comment '异常堆栈ID',
    EXCEPTION_MSG_       varchar(4000)           null comment '异常消息',
    DUEDATE_             timestamp(3)            null comment '到期时间',
    REPEAT_              varchar(255)            null comment '重复表达式',
    HANDLER_TYPE_        varchar(255)            null comment '处理器类型',
    HANDLER_CFG_         varchar(4000)           null comment '处理器配置',
    CUSTOM_VALUES_ID_    varchar(64)             null comment '自定义值ID',
    CREATE_TIME_         timestamp(3)            null comment '创建时间',
    TENANT_ID_           varchar(255) default '' null comment '租户ID',
    primary key (ID_),
    constraint ACT_FK_TIMER_JOB_CUSTOM_VALUES
        foreign key (CUSTOM_VALUES_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_TIMER_JOB_EXCEPTION
        foreign key (EXCEPTION_STACK_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_TIMER_JOB_EXECUTION
        foreign key (EXECUTION_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_TIMER_JOB_PROCESS_INSTANCE
        foreign key (PROCESS_INSTANCE_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_TIMER_JOB_PROC_DEF
        foreign key (PROC_DEF_ID_) references act_re_procdef (ID_)
)
    collate = utf8mb3_bin comment = '定时器作业表';

create index ACT_IDX_TIMER_JOB_CORRELATION_ID
    on act_ru_timer_job (CORRELATION_ID_);

create index ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID
    on act_ru_timer_job (CUSTOM_VALUES_ID_);

create index ACT_IDX_TIMER_JOB_DUEDATE
    on act_ru_timer_job (DUEDATE_);

create index ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID
    on act_ru_timer_job (EXCEPTION_STACK_ID_);

create index ACT_IDX_TJOB_SCOPE
    on act_ru_timer_job (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_TJOB_SCOPE_DEF
    on act_ru_timer_job (SCOPE_DEFINITION_ID_, SCOPE_TYPE_);

create index ACT_IDX_TJOB_SUB_SCOPE
    on act_ru_timer_job (SUB_SCOPE_ID_, SCOPE_TYPE_);

-- =============================================
-- act_ru_variable: 运行时变量表
-- 存储运行时变量
-- =============================================
create table if not exists act_ru_variable
(
    ID_           varchar(64)   not null comment '主键ID',
    REV_          int           null comment '数据版本号',
    TYPE_         varchar(255)  not null comment '变量类型',
    NAME_         varchar(255)  not null comment '变量名称',
    EXECUTION_ID_ varchar(64)   null comment '执行ID',
    PROC_INST_ID_ varchar(64)   null comment '流程实例ID',
    TASK_ID_      varchar(64)   null comment '任务ID',
    SCOPE_ID_     varchar(255)  null comment '范围ID',
    SUB_SCOPE_ID_ varchar(255)  null comment '子范围ID',
    SCOPE_TYPE_   varchar(255)  null comment '范围类型',
    BYTEARRAY_ID_ varchar(64)   null comment '字节数组ID',
    DOUBLE_       double        null comment 'Double类型值',
    LONG_         bigint        null comment 'Long类型值',
    TEXT_         varchar(4000) null comment '文本类型值',
    TEXT2_        varchar(4000) null comment '附加文本值',
    META_INFO_    varchar(4000) null comment '元信息',
    primary key (ID_),
    constraint ACT_FK_VAR_BYTEARRAY
        foreign key (BYTEARRAY_ID_) references act_ge_bytearray (ID_),
    constraint ACT_FK_VAR_EXE
        foreign key (EXECUTION_ID_) references act_ru_execution (ID_),
    constraint ACT_FK_VAR_PROCINST
        foreign key (PROC_INST_ID_) references act_ru_execution (ID_)
)
    collate = utf8mb3_bin comment = '运行时变量表';

create index ACT_IDX_RU_VAR_SCOPE_ID_TYPE
    on act_ru_variable (SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_RU_VAR_SUB_ID_TYPE
    on act_ru_variable (SUB_SCOPE_ID_, SCOPE_TYPE_);

create index ACT_IDX_VARIABLE_TASK_ID
    on act_ru_variable (TASK_ID_);

SET FOREIGN_KEY_CHECKS = 1;
