-- =============================================
-- 墨韵智库 - 工作流与定时任务初始化脚本
-- 版本: v1.0
-- 执行顺序: 02 (在 01_moyun_init.sql 之后执行)
-- =============================================
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



INSERT INTO act_ge_bytearray (ID_,REV_,NAME_,DEPLOYMENT_ID_,BYTES_,GENERATED_) VALUES
                                                                                   ('e33dab9c-3d55-11f1-9c2a-745d229dae59',1,'flow_mu6nh7qx.bpmn','e33dab9b-3d55-11f1-9c2a-745d229dae59',0x3C3F786D6C2076657273696F6E3D22312E302220656E636F64696E673D225554462D38223F3E0A3C646566696E6974696F6E7320786D6C6E733D22687474703A2F2F7777772E6F6D672E6F72672F737065632F42504D4E2F32303130303532342F4D4F44454C2220786D6C6E733A7873693D22687474703A2F2F7777772E77332E6F72672F323030312F584D4C536368656D612D696E7374616E63652220786D6C6E733A62706D6E64693D22687474703A2F2F7777772E6F6D672E6F72672F737065632F42504D4E2F32303130303532342F44492220786D6C6E733A6F6D6764633D22687474703A2F2F7777772E6F6D672E6F72672F737065632F44442F32303130303532342F44432220786D6C6E733A62696F633D22687474703A2F2F62706D6E2E696F2F736368656D612F62706D6E2F62696F636F6C6F722F312E302220786D6C6E733A666C6F7761626C653D22687474703A2F2F666C6F7761626C652E6F72672F62706D6E2220786D6C6E733A64693D22687474703A2F2F7777772E6F6D672E6F72672F737065632F44442F32303130303532342F44492220786D6C6E733A7873643D22687474703A2F2F7777772E77332E6F72672F323030312F584D4C536368656D6122207461726765744E616D6573706163653D22687474703A2F2F7777772E666C6F7761626C652E6F72672F70726F63657373646566223E0A20203C70726F636573732069643D22666C6F775F6472386F346D346E22206E616D653D22666C6F775F6D75366E683771782220666C6F7761626C653A70726F6365737343617465676F72793D226C65617665223E0A202020203C73746172744576656E742069643D2273746172745F6576656E7422206E616D653D22E5BC80E5A78B223E0A2020202020203C6F7574676F696E673E466C6F775F30617A646173663C2F6F7574676F696E673E0A202020203C2F73746172744576656E743E0A202020203C757365725461736B2069643D2241637469766974795F31713970696E3022206E616D653D22E7BB84E995BFE5AEA1E6A0B8223E0A2020202020203C696E636F6D696E673E466C6F775F30617A646173663C2F696E636F6D696E673E0A2020202020203C6F7574676F696E673E466C6F775F3074713577326D3C2F6F7574676F696E673E0A202020203C2F757365725461736B3E0A202020203C73657175656E6365466C6F772069643D22466C6F775F30617A646173662220736F757263655265663D2273746172745F6576656E7422207461726765745265663D2241637469766974795F31713970696E3022202F3E0A202020203C757365725461736B2069643D2241637469766974795F317178687A756F22206E616D653D22E7BB8FE79086E5AEA1E6A0B8223E0A2020202020203C696E636F6D696E673E466C6F775F3074713577326D3C2F696E636F6D696E673E0A2020202020203C6F7574676F696E673E466C6F775F307437757378303C2F6F7574676F696E673E0A202020203C2F757365725461736B3E0A202020203C73657175656E6365466C6F772069643D22466C6F775F3074713577326D2220736F757263655265663D2241637469766974795F31713970696E3022207461726765745265663D2241637469766974795F317178687A756F22202F3E0A202020203C656E644576656E742069643D224576656E745F306F69716E6E3522206E616D653D22E7BB93E69D9F223E0A2020202020203C696E636F6D696E673E466C6F775F307437757378303C2F696E636F6D696E673E0A202020203C2F656E644576656E743E0A202020203C73657175656E6365466C6F772069643D22466C6F775F307437757378302220736F757263655265663D2241637469766974795F317178687A756F22207461726765745265663D224576656E745F306F69716E6E3522202F3E0A20203C2F70726F636573733E0A20203C62706D6E64693A42504D4E4469616772616D2069643D2242504D4E4469616772616D5F666C6F77223E0A202020203C62706D6E64693A42504D4E506C616E652069643D2242504D4E506C616E655F666C6F77222062706D6E456C656D656E743D22666C6F775F6472386F346D346E223E0A2020202020203C62706D6E64693A42504D4E53686170652069643D2242504D4E53686170655F73746172745F6576656E74222062706D6E456C656D656E743D2273746172745F6576656E74222062696F633A7374726F6B653D22223E0A20202020202020203C6F6D6764633A426F756E647320783D222D38352220793D22313235222077696474683D22333022206865696768743D22333022202F3E0A20202020202020203C62706D6E64693A42504D4E4C6162656C3E0A202020202020202020203C6F6D6764633A426F756E647320783D222D38332220793D22313632222077696474683D22323322206865696768743D22313422202F3E0A20202020202020203C2F62706D6E64693A42504D4E4C6162656C3E0A2020202020203C2F62706D6E64693A42504D4E53686170653E0A2020202020203C62706D6E64693A42504D4E53686170652069643D2241637469766974795F31713970696E305F6469222062706D6E456C656D656E743D2241637469766974795F31713970696E30223E0A20202020202020203C6F6D6764633A426F756E647320783D22302220793D223930222077696474683D2231303022206865696768743D22383022202F3E0A20202020202020203C62706D6E64693A42504D4E4C6162656C202F3E0A2020202020203C2F62706D6E64693A42504D4E53686170653E0A2020202020203C62706D6E64693A42504D4E53686170652069643D2241637469766974795F317178687A756F5F6469222062706D6E456C656D656E743D2241637469766974795F317178687A756F223E0A20202020202020203C6F6D6764633A426F756E647320783D223137302220793D223930222077696474683D2231303022206865696768743D22383022202F3E0A20202020202020203C62706D6E64693A42504D4E4C6162656C202F3E0A2020202020203C2F62706D6E64693A42504D4E53686170653E0A2020202020203C62706D6E64693A42504D4E53686170652069643D224576656E745F306F69716E6E355F6469222062706D6E456C656D656E743D224576656E745F306F69716E6E35223E0A20202020202020203C6F6D6764633A426F756E647320783D223335322220793D22313132222077696474683D22333622206865696768743D22333622202F3E0A20202020202020203C62706D6E64693A42504D4E4C6162656C3E0A202020202020202020203C6F6D6764633A426F756E647320783D223335392220793D22313535222077696474683D22323322206865696768743D22313422202F3E0A20202020202020203C2F62706D6E64693A42504D4E4C6162656C3E0A2020202020203C2F62706D6E64693A42504D4E53686170653E0A2020202020203C62706D6E64693A42504D4E456467652069643D22466C6F775F30617A646173665F6469222062706D6E456C656D656E743D22466C6F775F30617A64617366223E0A20202020202020203C64693A776179706F696E7420783D222D35352220793D2231343022202F3E0A20202020202020203C64693A776179706F696E7420783D222D32372220793D2231343022202F3E0A20202020202020203C64693A776179706F696E7420783D222D32372220793D2231333022202F3E0A20202020202020203C64693A776179706F696E7420783D22302220793D2231333022202F3E0A2020202020203C2F62706D6E64693A42504D4E456467653E0A2020202020203C62706D6E64693A42504D4E456467652069643D22466C6F775F3074713577326D5F6469222062706D6E456C656D656E743D22466C6F775F3074713577326D223E0A20202020202020203C64693A776179706F696E7420783D223130302220793D2231333022202F3E0A20202020202020203C64693A776179706F696E7420783D223137302220793D2231333022202F3E0A2020202020203C2F62706D6E64693A42504D4E456467653E0A2020202020203C62706D6E64693A42504D4E456467652069643D22466C6F775F307437757378305F6469222062706D6E456C656D656E743D22466C6F775F30743775737830223E0A20202020202020203C64693A776179706F696E7420783D223237302220793D2231333022202F3E0A20202020202020203C64693A776179706F696E7420783D223335322220793D2231333022202F3E0A2020202020203C2F62706D6E64693A42504D4E456467653E0A202020203C2F62706D6E64693A42504D4E506C616E653E0A20203C2F62706D6E64693A42504D4E4469616772616D3E0A3C2F646566696E6974696F6E733E0A,0),
                                                                                   ('e3f33ffd-3d55-11f1-9c2a-745d229dae59',1,'flow_mu6nh7qx.flow_dr8o4m4n.png','e33dab9b-3d55-11f1-9c2a-745d229dae59',0x89504E470D0A1A0A0000000D49484452000001E3000000B40806000000A78B44C30000143449444154785EEDDD79705465BAC7F161ACBA855AEAB8AFB80DEAB896966559586A7955441D755C800E413481203B2EE0522A5E72C13F404A4B70BC6A958EA058F1AA8180C3329880916D181C4016D944162104A2AC1210E1BDEF73CCE97BF2F69B703A397DFA4DF2FD54FD2A49F7E9730E9D87E7EDF7F4E993DFFD0E00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000080E39452FFB16EDDBAFF9D376FDE2FA5A5A56AC68C1924E6E8E7FD707979F996B2B2B2BF98BF1FFC863ACD7EA85320837483FB44FF0753959595AABABA5A1D387080C41C79DEE5F99F356BD62EDDF4FE6CFE8E409DBA10EA14C8209969C87F30F33F1E893F15151555BAC92D307F47A04E5D0A750A64801CF263A6E146E4F7A09BDC7EF37704EAD4A550A74006C87B41E67F3692BDC8EFC3FC1D813A752DD42910B1B04D6EEFCE0AB5EE5F7F53CBBFF86F2FF2BDDC662E471A179A9C1D75EA56A8532062619ADC9E1D9BD5B2E943D492BF3F5D2B729BDC672E4F1A1E9A9C1D75EA56A8532062619ADCA6E593521A9C9F1F964F4E599E343C34393BEAD4AD50A740C4C234B96F678E48696E7EE43E7379D2F0D0E4ECA853B7429D02110BD3E496CD189AD2DCFCC87DE6F2A4E1A1C9D951A76E853A05224693732B34393BEAD4AD50A740C4C23439392BD56C6E7EE43E7379D2F0D0E4ECA853B7429D02110BD3E4D6CC7923A5B9F991FBCCE549C34393B3A34EDD0A750A442C4C93FB71F3376AD93FFE2BA5C1C96D729FB93C6978687276D4A95BA14E818885697292EF178E4B6972729BB91C695C687276D4A95BA14E8188856A72FBF7AB3573FF27A5C9C96D725FCAF2A4C1A1C9D951A76E853A052276A42627572E5A3D674C4A83F323F77175A3E84293B3A34EDD0A750A44ACCE26A76712156BCAD4D269CFA7343633B28C2CCBECA3F1A1C9D951A76E853A0522666B72479A65D415661F8D0F4DCE8E3A752BD42910315B930B33CBA82BF258737D247C687276D4A95BA14E8188D99A9CD9B8D28DB93E123E34393BEAD4AD50A740C46C4D8E642F34393BEAD4AD50A7D9919797F7879C9C9C8E894462B4FE5AAAF3BDCE3E1D55F3557E2EADB9BFA32C6FAE038EA2C9B9159A9C1D75EA56A8D37875E9D2A5BD1E5C8B750ED40CBC6123CB17CBE3CD75C2313439B74293B3A34EDD0A751A0F3D88B6D383E97CCB20DB90CC97F599DB802368726E852667479DBA15EA34B3F2F2F25A271289BFEA01F4B039A83EF1C4136AFCF8F1EAEBAFBF56DF7DF79DFAF1C71F9590AFF2B3DC2EF7CB72E663657DB25E59BFB94D64194DCEADD0E4ECA853B7429D668E1E30DBE819EC92E0209A9B9BABDE7EFB6DF5C30F3F78036F58B2BC3C4E1E6F0CCA8B653BE6B691453439B74293B3A34EDD0A759A197A10FE93CEE6E0C03962C408B569D326739C4D8B3C5ED6630CC89B647BE63E204B68726E852667479DBA15EA347A3533E2E4402CB3D949932699E36AA3C8FA8C59F22666C88EA0C9B9159A9C1D75EA56A8D368C97BB8C143D3F9F9F96AD1A245E6581A0959AFAC3F30202FE63D6407D0E4DC0A4DCE8E3A752BD469B46A4ED64ACE88333510FB64FDC119B26CDFDC27C48C26E756687276D4A95BA14EA353F3F1A543FEC03879F26473ECCC08D94E60767C888F3D65194DCEADD0E4ECA853B7429D462727F0396239C92A4EC6495DF3CD7D438C68726E852667479DBA15EA341A9D3B77BE3D7878BAB1674DA74BB6173C5C2DFB63EE23624293732B34393BEAD4AD50A7D1C8F9ED1297DE40289F07CE06D96E60765C6CEE23624293732B34393BEAD4AD50A78DA767A427E604AE351DF7ACD827DB0D0CC60764BFCC7D6D10BDEEDFCF9B37EFC171E3C6FDA3B0B0B0EAD9679FADEED3A78F7749B1DEBD7B1F1C3C78F06E7DDBDA61C3868DCACFCF6FB69FAFD2FFDE993AB798B79B68726EA5A53539EAB469A6A5D569BAC2D4B5BEBFA33F08CAA52BB3C9B8746647735FD3A2D7D77ADAB469EF0C1932E497828202F5E69B6FAAF9F3E75BAFDD29B7CBFD3D7AF4383460C080753D7BF6EC60AEAFA90B3CB1F516054DCEADB4B426479D36CDB4B43A4D5798BAD6B78FF197FBF0C30F6B8F8E3193ED07F6798CB9AFA1E9C1B59F9EE956CBE8AE67C5EAD0A143E6B6AC6439597EE0C08107FBF6EDFBEF4422F14773DD4D55E089ADB72868726EA5A53539EAB469A6A5D569BAC2D475972E5DCAFCFB172E5C680E4FB192EDFBFB22FB15DCCF50F43A5A4D9F3E7D7CAF5EBDD4942953D4AFBFFE6A6E2314799C3C3E2F2F6F6F73F9BB8F9662B016054DCEADB4B42667A94FEAB409A4A5D569BA2CF59C52D7FAEB46FFF675EBD699C352AC64FB817DDC68FC73EAA71FDFAAB8B8F89F7DFAF4512B57AE34D7DD20B29E8282827DB9B9B93DCCED3535962230E31545B69ADC92254BD49E3D7BBCEFABABAB93B7FFFCF3CF29CB6EDDBA557DFBEDB7B56EDBB2654BADC79959BD7AB577628279BB1979CB22B81E7985682E13675A5A93B3D4A599ACD6A9D4D1F6EDDB536EAFAFF6D20975DA3C59EAD88CD4F5CFFECF3FFDF493391CC54AB6EFEF8B9E90EE36FF3DF5921971EFDEBD556565A5B9DE4691F5E5E7E7EFC9E6E7AD2CBFB88C255B4DEEDE7BEF55CF3DF79CD760AEBDF65AD5B3674F2FD75D779D7AF5D5576B2DBB62C50A75DC71C7797F0E4C7EDEBF7FBFBAEDB6DBE48553ADE5A4A0FCC17CCD9A35EA9C73CE513B77EE4CDE6F6BA077DE79A77AFDF5D7933F8F1A354A3DF2C823DEF7BB77EF4E593ED3317F3FE4B764AB4EE5C5FE830F3EA874BFF1AEE7EBE7924B2E51CB962D4B595EEA4F06D88A8A8A5AB7CBDF98F5BFA74E899983070F9A4351AC64FB81FDF9D51C93EA3477EEDC7E321047352336E9F51ED685BEA353A74E6DCD6DC7419E10F3B67499BFEC4066E664F9F09F0CAA175D7491D7A0A4A19D72CA29C9FBF473EE5DA62DB8BC34B773CF3D37F9F3934F3EA9AEBFFE7AB571E3C65ACB151616AA8E1D3B7A83748F1E3DE439548F3EFAA8EADEBDBBD7402FBDF452B578F1E25A8FB9EFBEFBD4D4A953D53BEFBCE3BD18E8D6AD9B3AE18413BCEF651FA5E90597CF745ADA8CC3529FCED4A91C9169D3A68DDAB06183FAE8A38FD4E6CD9BBDDBA536FAF7EF9F5C4ECE3BF15F4CDE7CF3CDEACA2BAF54D75C734DF23689D4B83F03A64E9B3F4B3DA7D4B5CC40FDDB9BE4CC583FAEF5F0E1C3ABE53DDE4C2A2929D9919B9B3BCDDC7E1CE409316F4B577D4510948D26F7D4534FA94F3FFD540D1E3C583DF4D043EAB4D34E4BDE77A4C1F8E9A79FF61EB363C78E94F55E70C105DE2CDAFFF9A4934E4A36B5D1A347AB214386241BAA7E41E735BEF3CE3BCF6B9C37DE78A3DAB66D9B37E379E38D37BC7D9019B8B98D4CA7A5353997EB5406C771E3C679679A9E7FFEF9DE0BC70F3EF8C03B4A630E96F202F381071E50E5E5E5DE72679F7DB637733DF1C413BD3A0BBE98A44E9BBF30759DD3D4DF33D6AF0EDF96B3A61B7AB25658B27EFDCA757BE7CE9DFFD3DC874C9327C4BC2D5DF5154150DC4D6EC18205AA75EBD6EAAEBBEEF21A8C349DE38F3F3E3983B8F0C20BAD83F1E9A79FEECD065E7BEDB55AF70567C7329391C3D7FEBA8E39E698E4F7D24CD7AE5D5BEBB1BB76EDF25E0848B3959F5F7AE9253568D020AF994A139D356B56CAFE673A2DADC9B95AA763C78EF5EAF4F6DB6FF76A44DEA31D33668CBAE5965BD49C3973D44D37DDA41E7EF8E15A2F0A65302E2B2BF3CE8768DFBEBD575F975D7699779FD4B5BF1C75DAFC85A9EB267D36B57ECCEF5F7CF1C55FE4B0501C66CF9EBD53EFDC57E67E649A3C21E66DE9AAAF0882E26E72F22A7EE2C4895E23FBE4934FBCDBEA9A19CB576950F27C1C7DF4D1DE001E3CF4273306B95D66D9FEE34F3DF5D4E4FB71679D7556F2F6E036FC7CF5D557DE214299C5C840DFAA552B75C71D7778EB901991BC9F673E26D369694DCED53A95D9676969A957633208CB61643914BC6FDF3EEFFEBD7BF7AA5B6FBD554D9B364D2D5DBA543DF6D863DE8C5766AC575D75953AE38C336ACD8C83B528A14E9BB730759DD3943F673C77EEDC07A40987FD1C7163C976BA76EDBA530F10679BFB9249510CC661C5DDE42472228A34136924D2F06466EC9F18230DCD1F8CA5F1C9E1BFE061EABE7DFBAA6FBEF926659D7EA409E6E5E579EB921987BF5EF9DE5C56D625871CE57D3839FC28EF61CBB243870E5523478E546DDBB64D1E2E8C2B3439BBB8EB5466A332FB9419ECCB2FBFEC5DA148065CFF85A09CD425271E56555579CBCBFBC1F7DF7FBF77987AD5AA55EABDF7DEAB3533968B0C05D74F9D22C7A12B703DFEF8E3DE552A6B72E42B708D1D3B768614759C0A0B0B972512893EE6BE6452731E8CA569C82B7AFD9CAAE2E2626F5651D7CCD84F7030968F38C97B67E6A13C3F279F7CB277628C34CC638F3D36D93CE590A33FAB91C8E1ED0E1D3A244F8C913352E5A4C0575E79C5FB599AA5F9BE601CA1C9D9C55DA732C8CA216339C14A065839C359065C79012903A1B9BCC47FCF584EF6929A93F79CFD99B19C312DB5EE2F4B9DA2495F9B7AD8B061DBE33A44EDFBE28B2F96E7C4FC972C9AF3602C4D4DB6291F6B929985FCC510733096415A2E5DEADF161C8CE5909C9C00264D4866D8E6C73A64A6E09FD022CBF8B7CBB2C18F8CC87B7B32EB9626F7F9E79F7B87FF66CE9CA93EFBEC3375F9E597CB9F12B37EBE34D3A1C9D9C55DA772185A7ACDBBEFBE2BB306D5AF5F3FAF7E6466EA0FC6F239797FE094F789E56379EDDAB553C3870FF7CEC00ECE8CA54EE5677FFDD429848C2DFE40D8A4FE6AD333CF3C531DF75967AB56AD5AAF77F05FE6BE6452731E8C4B4A4AD451471D25972055EFBFFFBE371B90F77DFD99819CE822B308990DF88D6ED1A245DE0C433E9B2C87F6E4FAE2B2DF3240CB4C42660AE676243203376F3373F7DD777B4D4E1A9EAC47F6414ECA99306182B72FE6E745331D9A9C5DDC752ADBBBF8E28BBD41B3A8A828599FF23EB1BC2F2CDFDF70C30DDE7BC932433DF3CC33BD81F4ADB7DE4A1E720EBE672C1FC5BBE79E7BAC17B5A14E5B2E17FE9E71972E5D9287A8435F5F4317E1E1B83F8FA567687BF44E6E36F725939AF3602C914379F2551A539826221F03B9E28A2BBC5942F07699997CFCF1C7B50EEB0573F5D557A7DC6646661672628ECC7ECCAB1ABDF0C20BDEACDC7C4C264393B3CB469D063F32E45F70A6AEC8095FF25566C0E9CE54A9D3964DF7FBF9FE603862C4087308CA28D95E60563CDFDCB73AC9219AB8AF54A2B797FC60769C31FFED99928D26D79034E4B394750DD2E92E13676872764DA54E1B92303518669938439D4647CF4CDBE99E7FC8EFFD72CE4C1C643B8131E790EC87B96F75EAD5ABD7C1B867C6FA55EEDA9C9867C6716ACE4DAE29862667479DBA15EA345A8944E2AFFEC02887ABE5ADB94C92F5070F4FCBF6CD7DAAD7A0418376C7FD9EF18A152B16E6C4FC9E719C68726E852667479DBA15EA345A797979ADF538B3D81F1CE53C834C0DC8B25EBDBDE44C5CB62BDB37F7A95ECF3FFFFCEAB8CFA69E3061C2A49CB0679835413439B74293B3A34EDD0A751A3D3DCEB4D1D9149C214F9A34C91C921A45D6179C11D76CAF8DB92F47545858382AEECF19F7EFDF7F4ADC9F338E134DCEADD0E4ECA853B7429D66861E28FF141C902572925563CFB296C71B276B7903B16CCFDC8750F4D4BD4D4141C1A118AFC0B555EFF0F6B8AFC015279A9C5BA1C9D951A76E853ACD1C99A9E6040E594B64962C9F0796B3FAD321CBCBE38CD9B077685AB6636E3B2D03060CF83EAE43D54545451FE664E1DAD471A2C9B9159A9C1D75EA56A8D3CC92F7706B4EEA320751EFD299E3C78FF72E8B2AD75C900B2409F92A3FCBED72BF71894B3F8765BD69BF476CD3B367CF0E03070E3C18C35F6D9243056BB2F1579BE24493732B34393BEAD4AD50A7F1A8F9D853F273C88DCCFCB43EBE1446FFFEFDFF3D65CA94C3E6001AA1C323478EFC9BDEF929E6B69B1B9A9C5BA1C9D951A76E853A8D971E44DBE7FC76E9CCE4B5AC4346962F96C79BEB8C44A74E9DDAE6E7E7EF59B972A5398846E2CB2FBF1CABFF0115B21D73DBCD0D4DCEADD0E4ECA853B7429D66475E5EDE1FF4D8D43191488CD65F4B75BED7D95733F0CA57F9B9B4E6FE8EB2BCB98EC8C948DFBD7BF77D959595E658DA28EBD7AF9FACFF115B425FA3B389A3C9B9159A9C1D75EA56A853D4929B9BDB43CF9077473543AE99116FD6E96E6EABB9A2C9B9159A9C1D75EA56A853A490196CB76EDD76949494EC68E8495D72B256CD7BC45B5BCA8CD84793732B34393BEAD4AD50A7B04A24127FD4B3E4E9050505DB67CF9EBD33ECE790F57295454545E3F520BC4667AAACC75C77734793732B34393BEAD4AD50A7A8977C0C490FAAE55DBB76DD3574E8D0A5A5A5A5CB56AD5AB5B1AAAA6AAF1E7BF76CDBB66DDDF2E5CB174E9C38B164C0800153F5B255B27C73FFF8527D68726E852667479DBA15EA14A1C815B3F420DB5BCF74E5BAD20B72FEFFF262F27541CDEDBD9BF395B5C2A2C9B9159A9C1D75EA56A85320623439B74293B3A34EDD0A750A448C26E756687276D4A95BA14E8188D1E4DC0A4DCE8E3A752BD42910319A9C5BA1C9D951A76E853A05224693732B34393BEAD4AD50A740C468726E852667479DBA15EA1488184DCEADD0E4ECA853B7429D0211A3C9B9159A9C1D75EA56A85320623439B74293B3A34EDD0A750A448C26E756687276D4A95BA14E8188D1E4DC0A4DCE8E3A752BD42910B1D2D2D2C3D5D5D529FFD948FCD1BF872DBAC9ED377F47A04E5D0A750A64407979F996CACACA94FF7024FE6CD8B0A14837B905E6EF08D4A94BA14E810C282B2BFBCBAC59B3765554545431F3C84EF4F35EB17EFDFA8F7483DBA8F367F37704EAD48550A74086C97F2C79A5AB7340DE0B22B1479E7779FE6970F590E7A7E679A24EB313EA1400000000000000000000000000000000000000000000000000B2E1FF0090EED43C839DFFBF0000000049454E44AE426082,1);
INSERT INTO act_ge_property (NAME_,VALUE_,REV_) VALUES
                                                    ('app.schema.version','7.1.0.1',1),
                                                    ('cfg.execution-related-entities-count','true',1),
                                                    ('cfg.task-related-entities-count','true',1),
                                                    ('common.schema.version','7.1.0.2',1),
                                                    ('next.dbid','1',1),
                                                    ('schema.history','create(7.1.0.2)',1),
                                                    ('schema.version','7.1.0.2',1);
INSERT INTO act_id_property (NAME_,VALUE_,REV_) VALUES
    ('schema.version','7.1.0.2',1);
INSERT INTO act_re_deployment (ID_,NAME_,CATEGORY_,KEY_,TENANT_ID_,DEPLOY_TIME_,DERIVED_FROM_,DERIVED_FROM_ROOT_,PARENT_DEPLOYMENT_ID_,ENGINE_VERSION_) VALUES
    ('e33dab9b-3d55-11f1-9c2a-745d229dae59','flow_mu6nh7qx','leave',NULL,'','2026-04-21 15:44:11.918',NULL,NULL,'e33dab9b-3d55-11f1-9c2a-745d229dae59',NULL);
INSERT INTO act_re_procdef (ID_,REV_,CATEGORY_,NAME_,KEY_,VERSION_,DEPLOYMENT_ID_,RESOURCE_NAME_,DGRM_RESOURCE_NAME_,DESCRIPTION_,HAS_START_FORM_KEY_,HAS_GRAPHICAL_NOTATION_,SUSPENSION_STATE_,TENANT_ID_,ENGINE_VERSION_,DERIVED_FROM_,DERIVED_FROM_ROOT_,DERIVED_VERSION_) VALUES
    ('flow_dr8o4m4n:1:e3f3dc3e-3d55-11f1-9c2a-745d229dae59',2,'leave','flow_mu6nh7qx','flow_dr8o4m4n',1,'e33dab9b-3d55-11f1-9c2a-745d229dae59','flow_mu6nh7qx.bpmn','flow_mu6nh7qx.flow_dr8o4m4n.png',NULL,0,1,1,'',NULL,NULL,NULL,0);


-- =============================================
-- 一、Quartz 定时任务表
-- =============================================

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

-- 1、存储每一个已配置的 jobDetail 的详细信息
CREATE TABLE QRTZ_JOB_DETAILS (
                                  sched_name           varchar(120)    not null            comment '调度名称',
                                  job_name             varchar(200)    not null            comment '任务名称',
                                  job_group            varchar(200)    not null            comment '任务组名',
                                  description          varchar(250)    null                comment '相关介绍',
                                  job_class_name       varchar(250)    not null            comment '执行任务类名称',
                                  is_durable           varchar(1)      not null            comment '是否持久化',
                                  is_nonconcurrent     varchar(1)      not null            comment '是否并发',
                                  is_update_data       varchar(1)      not null            comment '是否更新数据',
                                  requests_recovery    varchar(1)      not null            comment '是否接受恢复执行',
                                  job_data             blob            null                comment '存放持久化job对象',
                                  primary key (sched_name, job_name, job_group)
) engine=innodb comment = '任务详细信息表';

-- 2、存储已配置的 Trigger 的信息
CREATE TABLE QRTZ_TRIGGERS (
                               sched_name           varchar(120)    not null            comment '调度名称',
                               trigger_name         varchar(200)    not null            comment '触发器的名字',
                               trigger_group        varchar(200)    not null            comment '触发器所属组的名字',
                               job_name             varchar(200)    not null            comment 'qrtz_job_details表job_name的外键',
                               job_group            varchar(200)    not null            comment 'qrtz_job_details表job_group的外键',
                               description          varchar(250)    null                comment '相关介绍',
                               next_fire_time       bigint(13)      null                comment '上一次触发时间（毫秒）',
                               prev_fire_time       bigint(13)      null                comment '下一次触发时间（默认为-1表示不触发）',
                               priority             integer         null                comment '优先级',
                               trigger_state        varchar(16)     not null            comment '触发器状态',
                               trigger_type         varchar(8)      not null            comment '触发器的类型',
                               start_time           bigint(13)      not null            comment '开始时间',
                               end_time             bigint(13)      null                comment '结束时间',
                               calendar_name        varchar(200)    null                comment '日程表名称',
                               misfire_instr        smallint(2)     null                comment '补偿执行的策略',
                               job_data             blob            null                comment '存放持久化job对象',
                               primary key (sched_name, trigger_name, trigger_group),
                               foreign key (sched_name, job_name, job_group) references QRTZ_JOB_DETAILS(sched_name, job_name, job_group)
) engine=innodb comment = '触发器详细信息表';

-- 3、存储简单的 Trigger，包括重复次数，间隔，以及已触发的次数
CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
                                      sched_name           varchar(120)    not null            comment '调度名称',
                                      trigger_name         varchar(200)    not null            comment 'qrtz_triggers表trigger_name的外键',
                                      trigger_group        varchar(200)    not null            comment 'qrtz_triggers表trigger_group的外键',
                                      repeat_count         bigint(7)       not null            comment '重复的次数统计',
                                      repeat_interval      bigint(12)      not null            comment '重复的间隔时间',
                                      times_triggered      bigint(10)      not null            comment '已经触发的次数',
                                      primary key (sched_name, trigger_name, trigger_group),
                                      foreign key (sched_name, trigger_name, trigger_group) references QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
) engine=innodb comment = '简单触发器的信息表';

-- 4、存储 Cron Trigger，包括 Cron 表达式和时区信息
CREATE TABLE QRTZ_CRON_TRIGGERS (
                                    sched_name           varchar(120)    not null            comment '调度名称',
                                    trigger_name         varchar(200)    not null            comment 'qrtz_triggers表trigger_name的外键',
                                    trigger_group        varchar(200)    not null            comment 'qrtz_triggers表trigger_group的外键',
                                    cron_expression      varchar(200)    not null            comment 'cron表达式',
                                    time_zone_id         varchar(80)                         comment '时区',
                                    primary key (sched_name, trigger_name, trigger_group),
                                    foreign key (sched_name, trigger_name, trigger_group) references QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
) engine=innodb comment = 'Cron类型的触发器表';

-- 5、Trigger 作为 Blob 类型存储
CREATE TABLE QRTZ_BLOB_TRIGGERS (
                                    sched_name           varchar(120)    not null            comment '调度名称',
                                    trigger_name         varchar(200)    not null            comment 'qrtz_triggers表trigger_name的外键',
                                    trigger_group        varchar(200)    not null            comment 'qrtz_triggers表trigger_group的外键',
                                    blob_data            blob            null                comment '存放持久化Trigger对象',
                                    primary key (sched_name, trigger_name, trigger_group),
                                    foreign key (sched_name, trigger_name, trigger_group) references QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
) engine=innodb comment = 'Blob类型的触发器表';

-- 6、以 Blob 类型存储存放日历信息
CREATE TABLE QRTZ_CALENDARS (
                                sched_name           varchar(120)    not null            comment '调度名称',
                                calendar_name        varchar(200)    not null            comment '日历名称',
                                calendar             blob            not null            comment '存放持久化calendar对象',
                                primary key (sched_name, calendar_name)
) engine=innodb comment = '日历信息表';

-- 7、存储已暂停的 Trigger 组的信息
CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
                                          sched_name           varchar(120)    not null            comment '调度名称',
                                          trigger_group        varchar(200)    not null            comment 'qrtz_triggers表trigger_group的外键',
                                          primary key (sched_name, trigger_group)
) engine=innodb comment = '暂停的触发器表';

-- 8、存储与已触发的 Trigger 相关的状态信息
CREATE TABLE QRTZ_FIRED_TRIGGERS (
                                     sched_name           varchar(120)    not null            comment '调度名称',
                                     entry_id             varchar(95)     not null            comment '调度器实例id',
                                     trigger_name         varchar(200)    not null            comment 'qrtz_triggers表trigger_name的外键',
                                     trigger_group        varchar(200)    not null            comment 'qrtz_triggers表trigger_group的外键',
                                     instance_name        varchar(200)    not null            comment '调度器实例名',
                                     fired_time           bigint(13)      not null            comment '触发的时间',
                                     sched_time           bigint(13)      not null            comment '定时器制定的时间',
                                     priority             integer         not null            comment '优先级',
                                     state                varchar(16)     not null            comment '状态',
                                     job_name             varchar(200)    null                comment '任务名称',
                                     job_group            varchar(200)    null                comment '任务组名',
                                     is_nonconcurrent     varchar(1)      null                comment '是否并发',
                                     requests_recovery    varchar(1)      null                comment '是否接受恢复执行',
                                     primary key (sched_name, entry_id)
) engine=innodb comment = '已触发的触发器表';

-- 9、存储少量的有关 Scheduler 的状态信息
CREATE TABLE QRTZ_SCHEDULER_STATE (
                                      sched_name           varchar(120)    not null            comment '调度名称',
                                      instance_name        varchar(200)    not null            comment '实例名称',
                                      last_checkin_time    bigint(13)      not null            comment '上次检查时间',
                                      checkin_interval     bigint(13)      not null            comment '检查间隔时间',
                                      primary key (sched_name, instance_name)
) engine=innodb comment = '调度器状态表';

-- 10、存储程序的悲观锁的信息
CREATE TABLE QRTZ_LOCKS (
                            sched_name           varchar(120)    not null            comment '调度名称',
                            lock_name            varchar(40)     not null            comment '悲观锁名称',
                            primary key (sched_name, lock_name)
) engine=innodb comment = '存储的悲观锁信息表';

-- 11、Quartz集群实现同步机制的行锁表
CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
                                       sched_name           varchar(120)    not null            comment '调度名称',
                                       trigger_name         varchar(200)    not null            comment 'qrtz_triggers表trigger_name的外键',
                                       trigger_group        varchar(200)    not null            comment 'qrtz_triggers表trigger_group的外键',
                                       str_prop_1           varchar(512)    null                comment 'String类型的trigger的第一个参数',
                                       str_prop_2           varchar(512)    null                comment 'String类型的trigger的第二个参数',
                                       str_prop_3           varchar(512)    null                comment 'String类型的trigger的第三个参数',
                                       int_prop_1           int             null                comment 'int类型的trigger的第一个参数',
                                       int_prop_2           int             null                comment 'int类型的trigger的第二个参数',
                                       long_prop_1          bigint          null                comment 'long类型的trigger的第一个参数',
                                       long_prop_2          bigint          null                comment 'long类型的trigger的第二个参数',
                                       dec_prop_1           numeric(13,4)   null                comment 'decimal类型的trigger的第一个参数',
                                       dec_prop_2           numeric(13,4)   null                comment 'decimal类型的trigger的第二个参数',
                                       bool_prop_1          varchar(1)      null                comment 'Boolean类型的trigger的第一个参数',
                                       bool_prop_2          varchar(1)      null                comment 'Boolean类型的trigger的第二个参数',
                                       primary key (sched_name, trigger_name, trigger_group),
                                       foreign key (sched_name, trigger_name, trigger_group) references QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
) engine=innodb comment = '同步机制的行锁表';



commit;
