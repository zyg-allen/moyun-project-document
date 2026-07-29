-- 来源：all-db-ddl.sql 行9-1160
-- 用途：Flowable 工作流全部 39 张 act_* 表 DDL（含 act_ge_property/act_id_property/act_re_procdef 的内嵌配置数据）


DROP TABLE IF EXISTS `act_evt_log`;
CREATE TABLE `act_evt_log` (
                               `LOG_NR_` bigint NOT NULL AUTO_INCREMENT,
                               `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
                               `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
                               `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
                               `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
                               `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
                               `TIME_STAMP_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                               `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
                               `DATA_` longblob,
                               `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
                               `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
                               `IS_PROCESSED_` tinyint DEFAULT '0',
                               PRIMARY KEY (`LOG_NR_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;


-- Table structure for table `act_ge_bytearray`


DROP TABLE IF EXISTS `act_ge_bytearray`;
CREATE TABLE `act_ge_bytearray` (
                                    `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                    `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                    `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '字节数组名称',
                                    `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '部署ID',
                                    `BYTES_` longblob COMMENT '字节数据',
                                    `GENERATED_` tinyint DEFAULT NULL COMMENT '是否自动生成',
                                    PRIMARY KEY (`ID_`),
                                    KEY `ACT_FK_BYTEARR_DEPL` (`DEPLOYMENT_ID_`),
                                    CONSTRAINT `ACT_FK_BYTEARR_DEPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='通用字节数组表';


-- Table structure for table `act_ge_property`


DROP TABLE IF EXISTS `act_ge_property`;
CREATE TABLE `act_ge_property` (
                                   `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
                                   `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
                                   `REV_` int DEFAULT NULL,
                                   PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;


-- Dumping data for table `act_ge_property`


LOCK TABLES `act_ge_property` WRITE;
INSERT INTO `act_ge_property` VALUES ('app.schema.version','7.1.0.1',1),('cfg.execution-related-entities-count','true',1),('cfg.task-related-entities-count','true',1),('common.schema.version','7.1.0.2',1),('next.dbid','1',1),('schema.history','create(7.1.0.2)',1),('schema.version','7.1.0.2',1);
UNLOCK TABLES;


-- Table structure for table `act_hi_actinst`


DROP TABLE IF EXISTS `act_hi_actinst`;
CREATE TABLE `act_hi_actinst` (
                                  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                  `REV_` int DEFAULT '1' COMMENT '数据版本号',
                                  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '流程定义ID',
                                  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '流程实例ID',
                                  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '执行ID',
                                  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '活动ID',
                                  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务ID',
                                  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '调用的流程实例ID',
                                  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '活动名称',
                                  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '活动类型',
                                  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行人',
                                  `START_TIME_` datetime(3) NOT NULL COMMENT '开始时间',
                                  `END_TIME_` datetime(3) DEFAULT NULL COMMENT '结束时间',
                                  `TRANSACTION_ORDER_` int DEFAULT NULL COMMENT '事务顺序',
                                  `DURATION_` bigint DEFAULT NULL COMMENT '持续时间(毫秒)',
                                  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '删除原因',
                                  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                  PRIMARY KEY (`ID_`),
                                  KEY `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`),
                                  KEY `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`),
                                  KEY `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`),
                                  KEY `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史活动实例表';


-- Table structure for table `act_hi_attachment`


DROP TABLE IF EXISTS `act_hi_attachment`;
CREATE TABLE `act_hi_attachment` (
                                     `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                     `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                     `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '创建用户ID',
                                     `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '附件名称',
                                     `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '附件描述',
                                     `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '附件类型',
                                     `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务ID',
                                     `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                     `URL_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '附件URL',
                                     `CONTENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '内容ID',
                                     `TIME_` datetime(3) DEFAULT NULL COMMENT '创建时间',
                                     PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史附件表';


--
-- Table structure for table `act_hi_comment`
--

DROP TABLE IF EXISTS `act_hi_comment`;
CREATE TABLE `act_hi_comment` (
                                  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '评论类型',
                                  `TIME_` datetime(3) NOT NULL COMMENT '评论时间',
                                  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '评论用户ID',
                                  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务ID',
                                  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                  `ACTION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '动作',
                                  `MESSAGE_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '消息内容',
                                  `FULL_MSG_` longblob COMMENT '完整消息',
                                  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史评论表';


--
-- Table structure for table `act_hi_detail`
--

DROP TABLE IF EXISTS `act_hi_detail`;
CREATE TABLE `act_hi_detail` (
                                 `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                 `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '变量类型',
                                 `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                 `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                                 `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务ID',
                                 `ACT_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '活动实例ID',
                                 `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '变量名称',
                                 `VAR_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '变量类型',
                                 `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                 `TIME_` datetime(3) NOT NULL COMMENT '操作时间',
                                 `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '字节数组ID',
                                 `DOUBLE_` double DEFAULT NULL COMMENT 'Double类型值',
                                 `LONG_` bigint DEFAULT NULL COMMENT 'Long类型值',
                                 `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '文本类型值',
                                 `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '附加文本值',
                                 PRIMARY KEY (`ID_`),
                                 KEY `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`),
                                 KEY `ACT_IDX_HI_DETAIL_NAME` (`NAME_`),
                                 KEY `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`),
                                 KEY `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`),
                                 KEY `ACT_IDX_HI_DETAIL_TIME` (`TIME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史变量明细记录表';


--
-- Table structure for table `act_hi_entitylink`
--

DROP TABLE IF EXISTS `act_hi_entitylink`;
CREATE TABLE `act_hi_entitylink` (
                                     `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                     `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '链接类型',
                                     `CREATE_TIME_` datetime(3) DEFAULT NULL COMMENT '创建时间',
                                     `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                     `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                     `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                     `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                     `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '父元素ID',
                                     `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引用范围ID',
                                     `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引用范围类型',
                                     `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引用范围定义ID',
                                     `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '根范围ID',
                                     `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '根范围类型',
                                     `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '层级类型',
                                     PRIMARY KEY (`ID_`),
                                     KEY `ACT_IDX_HI_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
                                     KEY `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
                                     KEY `ACT_IDX_HI_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
                                     KEY `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史实体链接表';


--
-- Table structure for table `act_hi_identitylink`
--

DROP TABLE IF EXISTS `act_hi_identitylink`;
CREATE TABLE `act_hi_identitylink` (
                                       `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                       `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户组ID',
                                       `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '关系类型',
                                       `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户ID',
                                       `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务ID',
                                       `CREATE_TIME_` datetime(3) DEFAULT NULL COMMENT '创建时间',
                                       `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                       `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                       `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                       `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                       `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                       PRIMARY KEY (`ID_`),
                                       KEY `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`),
                                       KEY `ACT_IDX_HI_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                                       KEY `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
                                       KEY `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
                                       KEY `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`),
                                       KEY `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史身份关系表';



-- Table structure for table `act_hi_procinst`


DROP TABLE IF EXISTS `act_hi_procinst`;
CREATE TABLE `act_hi_procinst` (
                                   `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                   `REV_` int DEFAULT '1' COMMENT '数据版本号',
                                   `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '流程实例ID',
                                   `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '业务键',
                                   `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '流程定义ID',
                                   `START_TIME_` datetime(3) NOT NULL COMMENT '开始时间',
                                   `END_TIME_` datetime(3) DEFAULT NULL COMMENT '结束时间',
                                   `DURATION_` bigint DEFAULT NULL COMMENT '持续时间(毫秒)',
                                   `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '启动用户ID',
                                   `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '开始活动ID',
                                   `END_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '结束活动ID',
                                   `SUPER_PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '上级流程实例ID',
                                   `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '删除原因',
                                   `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                   `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例名称',
                                   `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '回调ID',
                                   `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '回调类型',
                                   `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引用ID',
                                   `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引用类型',
                                   `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '传播的阶段实例ID',
                                   `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '业务状态',
                                   PRIMARY KEY (`ID_`),
                                   UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`),
                                   KEY `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`),
                                   KEY `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`),
                                   KEY `ACT_IDX_HI_PRO_SUPER_PROCINST` (`SUPER_PROCESS_INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史流程实例表';


--
-- Table structure for table `act_hi_taskinst`
--

DROP TABLE IF EXISTS `act_hi_taskinst`;
CREATE TABLE `act_hi_taskinst` (
                                   `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                   `REV_` int DEFAULT '1' COMMENT '数据版本号',
                                   `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                                   `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务定义ID',
                                   `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务定义键',
                                   `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                   `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                                   `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                   `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                   `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                   `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                   `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '传播的阶段实例ID',
                                   `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务状态',
                                   `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务名称',
                                   `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '父任务ID',
                                   `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务描述',
                                   `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务所有者',
                                   `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务执行人',
                                   `START_TIME_` datetime(3) NOT NULL COMMENT '开始时间',
                                   `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL COMMENT '进行中时间',
                                   `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '进行中开始人',
                                   `CLAIM_TIME_` datetime(3) DEFAULT NULL COMMENT '签收时间',
                                   `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '签收人',
                                   `SUSPENDED_TIME_` datetime(3) DEFAULT NULL COMMENT '挂起时间',
                                   `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '挂起人',
                                   `END_TIME_` datetime(3) DEFAULT NULL COMMENT '结束时间',
                                   `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '完成人',
                                   `DURATION_` bigint DEFAULT NULL COMMENT '持续时间(毫秒)',
                                   `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '删除原因',
                                   `PRIORITY_` int DEFAULT NULL COMMENT '优先级',
                                   `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL COMMENT '进行中截止日期',
                                   `DUE_DATE_` datetime(3) DEFAULT NULL COMMENT '截止日期',
                                   `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '表单键',
                                   `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务分类',
                                   `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                   `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL COMMENT '最后更新时间',
                                   PRIMARY KEY (`ID_`),
                                   KEY `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`),
                                   KEY `ACT_IDX_HI_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                                   KEY `ACT_IDX_HI_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
                                   KEY `ACT_IDX_HI_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史任务实例表';



-- Table structure for table `act_hi_tsk_log`


DROP TABLE IF EXISTS `act_hi_tsk_log`;
CREATE TABLE `act_hi_tsk_log` (
                                  `ID_` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '日志类型',
                                  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '任务ID',
                                  `TIME_STAMP_` timestamp(3) NOT NULL COMMENT '时间戳',
                                  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户ID',
                                  `DATA_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '日志数据',
                                  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                                  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                                  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                  PRIMARY KEY (`ID_`),
                                  KEY `ACT_IDX_ACT_HI_TSK_LOG_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史任务日志表';



-- Table structure for table `act_hi_varinst`

DROP TABLE IF EXISTS `act_hi_varinst`;
CREATE TABLE `act_hi_varinst` (
                                  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                  `REV_` int DEFAULT '1' COMMENT '数据版本号',
                                  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                                  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务ID',
                                  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '变量名称',
                                  `VAR_TYPE_` varchar(100) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '变量类型',
                                  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '字节数组ID',
                                  `DOUBLE_` double DEFAULT NULL COMMENT 'Double类型值',
                                  `LONG_` bigint DEFAULT NULL COMMENT 'Long类型值',
                                  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '文本类型值',
                                  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '附加文本值',
                                  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元信息',
                                  `CREATE_TIME_` datetime(3) DEFAULT NULL COMMENT '创建时间',
                                  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL COMMENT '最后更新时间',
                                  PRIMARY KEY (`ID_`),
                                  KEY `ACT_IDX_HI_PROCVAR_EXE` (`EXECUTION_ID_`),
                                  KEY `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`),
                                  KEY `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`),
                                  KEY `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`),
                                  KEY `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                                  KEY `ACT_IDX_HI_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史变量实例表';



-- Table structure for table `act_id_bytearray`


DROP TABLE IF EXISTS `act_id_bytearray`;
CREATE TABLE `act_id_bytearray` (
                                    `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                    `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                    `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '字节数组名称',
                                    `BYTES_` longblob COMMENT '字节数据',
                                    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='身份管理字节数组表';



-- Table structure for table `act_id_group`


DROP TABLE IF EXISTS `act_id_group`;
CREATE TABLE `act_id_group` (
                                `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户组名称',
                                `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户组类型',
                                PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户组表';



-- Table structure for table `act_id_info`


DROP TABLE IF EXISTS `act_id_info`;
CREATE TABLE `act_id_info` (
                               `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                               `REV_` int DEFAULT NULL COMMENT '数据版本号',
                               `USER_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户ID',
                               `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '信息类型',
                               `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '信息键',
                               `VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '信息值',
                               `PASSWORD_` longblob COMMENT '密码',
                               `PARENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '父ID',
                               PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户信息表';



-- Table structure for table `act_id_membership`


DROP TABLE IF EXISTS `act_id_membership`;
CREATE TABLE `act_id_membership` (
                                     `USER_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '用户ID',
                                     `GROUP_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '用户组ID',
                                     PRIMARY KEY (`USER_ID_`,`GROUP_ID_`),
                                     KEY `ACT_FK_MEMB_GROUP` (`GROUP_ID_`),
                                     CONSTRAINT `ACT_FK_MEMB_GROUP` FOREIGN KEY (`GROUP_ID_`) REFERENCES `act_id_group` (`ID_`),
                                     CONSTRAINT `ACT_FK_MEMB_USER` FOREIGN KEY (`USER_ID_`) REFERENCES `act_id_user` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户组成员表';



-- Table structure for table `act_id_priv`


DROP TABLE IF EXISTS `act_id_priv`;
CREATE TABLE `act_id_priv` (
                               `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                               `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '权限名称',
                               PRIMARY KEY (`ID_`),
                               UNIQUE KEY `ACT_UNIQ_PRIV_NAME` (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='权限表';



-- Table structure for table `act_id_priv_mapping`


DROP TABLE IF EXISTS `act_id_priv_mapping`;
CREATE TABLE `act_id_priv_mapping` (
                                       `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                       `PRIV_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '权限ID',
                                       `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户ID',
                                       `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户组ID',
                                       PRIMARY KEY (`ID_`),
                                       KEY `ACT_FK_PRIV_MAPPING` (`PRIV_ID_`),
                                       KEY `ACT_IDX_PRIV_GROUP` (`GROUP_ID_`),
                                       KEY `ACT_IDX_PRIV_USER` (`USER_ID_`),
                                       CONSTRAINT `ACT_FK_PRIV_MAPPING` FOREIGN KEY (`PRIV_ID_`) REFERENCES `act_id_priv` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='权限映射表';



-- Table structure for table `act_id_property`


DROP TABLE IF EXISTS `act_id_property`;
CREATE TABLE `act_id_property` (
                                   `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '属性名称',
                                   `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '属性值',
                                   `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                   PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='身份管理属性表';


-- Dumping data for table `act_id_property`


LOCK TABLES `act_id_property` WRITE;
INSERT INTO `act_id_property` VALUES ('schema.version','7.1.0.2',1);
UNLOCK TABLES;


-- Table structure for table `act_id_token`


DROP TABLE IF EXISTS `act_id_token`;
CREATE TABLE `act_id_token` (
                                `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                `TOKEN_VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '令牌值',
                                `TOKEN_DATE_` timestamp(3) NULL DEFAULT NULL COMMENT '令牌日期',
                                `IP_ADDRESS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'IP地址',
                                `USER_AGENT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户代理',
                                `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户ID',
                                `TOKEN_DATA_` varchar(2000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '令牌数据',
                                PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='令牌表';


--
-- Table structure for table `act_id_user`
--

DROP TABLE IF EXISTS `act_id_user`;
CREATE TABLE `act_id_user` (
                               `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                               `REV_` int DEFAULT NULL COMMENT '数据版本号',
                               `FIRST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '名',
                               `LAST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '姓',
                               `DISPLAY_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '显示名称',
                               `EMAIL_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '邮箱',
                               `PWD_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '密码',
                               `PICTURE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '头像ID',
                               `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                               PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户表';


--
-- Table structure for table `act_procdef_info`
--

DROP TABLE IF EXISTS `act_procdef_info`;
CREATE TABLE `act_procdef_info` (
                                    `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                    `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '流程定义ID',
                                    `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                    `INFO_JSON_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '信息JSON ID',
                                    PRIMARY KEY (`ID_`),
                                    UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`),
                                    KEY `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`),
                                    KEY `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`),
                                    CONSTRAINT `ACT_FK_INFO_JSON_BA` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                                    CONSTRAINT `ACT_FK_INFO_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='流程定义信息表';


DROP TABLE IF EXISTS `act_re_deployment`;
CREATE TABLE `act_re_deployment` (
                                     `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                     `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '部署名称',
                                     `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '部署分类',
                                     `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '部署键',
                                     `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                     `DEPLOY_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '部署时间',
                                     `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '衍生自',
                                     `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '衍生自根',
                                     `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '父部署ID',
                                     `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引擎版本',
                                     PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='部署表';


DROP TABLE IF EXISTS `act_re_model`;
CREATE TABLE `act_re_model` (
                                `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '模型名称',
                                `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '模型键',
                                `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '模型分类',
                                `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
                                `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '最后更新时间',
                                `VERSION_` int DEFAULT NULL COMMENT '版本号',
                                `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元信息',
                                `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '部署ID',
                                `EDITOR_SOURCE_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '编辑器源值ID',
                                `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '编辑器源附加值ID',
                                `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                PRIMARY KEY (`ID_`),
                                KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
                                KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
                                KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
                                CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`),
                                CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                                CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='模型表';


--
-- Table structure for table `act_re_procdef`
--

DROP TABLE IF EXISTS `act_re_procdef`;
CREATE TABLE `act_re_procdef` (
                                  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                  `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程分类',
                                  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程名称',
                                  `KEY_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '流程键',
                                  `VERSION_` int NOT NULL COMMENT '版本号',
                                  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '部署ID',
                                  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '资源名称',
                                  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程图资源名称',
                                  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程描述',
                                  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL COMMENT '是否有开始表单键',
                                  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL COMMENT '是否有图形符号',
                                  `SUSPENSION_STATE_` int DEFAULT NULL COMMENT '挂起状态',
                                  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引擎版本',
                                  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '衍生自',
                                  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '衍生自根',
                                  `DERIVED_VERSION_` int NOT NULL DEFAULT '0' COMMENT '衍生版本',
                                  PRIMARY KEY (`ID_`),
                                  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='流程定义表';

--
-- Dumping data for table `act_re_procdef`
--

LOCK TABLES `act_re_procdef` WRITE;
INSERT INTO `act_re_procdef` VALUES ('flow_dr8o4m4n:1:e3f3dc3e-3d55-11f1-9c2a-745d229dae59',2,'leave','flow_mu6nh7qx','flow_dr8o4m4n',1,'e33dab9b-3d55-11f1-9c2a-745d229dae59','flow_mu6nh7qx.bpmn','flow_mu6nh7qx.flow_dr8o4m4n.png',NULL,0,1,1,'',NULL,NULL,NULL,0);
UNLOCK TABLES;

--
-- Table structure for table `act_ru_actinst`
--

DROP TABLE IF EXISTS `act_ru_actinst`;
CREATE TABLE `act_ru_actinst` (
                                  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                  `REV_` int DEFAULT '1' COMMENT '数据版本号',
                                  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '流程定义ID',
                                  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '流程实例ID',
                                  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '执行ID',
                                  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '活动ID',
                                  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务ID',
                                  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '调用的流程实例ID',
                                  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '活动名称',
                                  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '活动类型',
                                  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行人',
                                  `START_TIME_` datetime(3) NOT NULL COMMENT '开始时间',
                                  `END_TIME_` datetime(3) DEFAULT NULL COMMENT '结束时间',
                                  `DURATION_` bigint DEFAULT NULL COMMENT '持续时间(毫秒)',
                                  `TRANSACTION_ORDER_` int DEFAULT NULL COMMENT '事务顺序',
                                  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '删除原因',
                                  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                  PRIMARY KEY (`ID_`),
                                  KEY `ACT_IDX_RU_ACTI_END` (`END_TIME_`),
                                  KEY `ACT_IDX_RU_ACTI_EXEC` (`EXECUTION_ID_`),
                                  KEY `ACT_IDX_RU_ACTI_EXEC_ACT` (`EXECUTION_ID_`,`ACT_ID_`),
                                  KEY `ACT_IDX_RU_ACTI_PROC` (`PROC_INST_ID_`),
                                  KEY `ACT_IDX_RU_ACTI_PROC_ACT` (`PROC_INST_ID_`,`ACT_ID_`),
                                  KEY `ACT_IDX_RU_ACTI_START` (`START_TIME_`),
                                  KEY `ACT_IDX_RU_ACTI_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='运行时活动实例表';


--
-- Table structure for table `act_ru_deadletter_job`
--

DROP TABLE IF EXISTS `act_ru_deadletter_job`;
CREATE TABLE `act_ru_deadletter_job` (
                                         `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                         `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                         `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '作业分类',
                                         `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '作业类型',
                                         `EXCLUSIVE_` tinyint(1) DEFAULT NULL COMMENT '是否排他',
                                         `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                                         `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                         `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                                         `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元素ID',
                                         `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元素名称',
                                         `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                         `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                         `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                         `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                         `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '关联ID',
                                         `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常堆栈ID',
                                         `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常消息',
                                         `DUEDATE_` timestamp(3) NULL DEFAULT NULL COMMENT '到期时间',
                                         `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '重复表达式',
                                         `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器类型',
                                         `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器配置',
                                         `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '自定义值ID',
                                         `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
                                         `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                         PRIMARY KEY (`ID_`),
                                         KEY `ACT_FK_DEADLETTER_JOB_EXECUTION` (`EXECUTION_ID_`),
                                         KEY `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
                                         KEY `ACT_FK_DEADLETTER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
                                         KEY `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
                                         KEY `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
                                         KEY `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
                                         KEY `ACT_IDX_DJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                                         KEY `ACT_IDX_DJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
                                         KEY `ACT_IDX_DJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
                                         CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                                         CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                                         CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
                                         CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
                                         CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='死信作业表';


--
-- Table structure for table `act_ru_entitylink`
--

DROP TABLE IF EXISTS `act_ru_entitylink`;
CREATE TABLE `act_ru_entitylink` (
                                     `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                     `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                     `CREATE_TIME_` datetime(3) DEFAULT NULL COMMENT '创建时间',
                                     `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '链接类型',
                                     `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                     `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                     `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                     `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                     `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '父元素ID',
                                     `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引用范围ID',
                                     `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引用范围类型',
                                     `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引用范围定义ID',
                                     `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '根范围ID',
                                     `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '根范围类型',
                                     `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '层级类型',
                                     PRIMARY KEY (`ID_`),
                                     KEY `ACT_IDX_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
                                     KEY `ACT_IDX_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
                                     KEY `ACT_IDX_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
                                     KEY `ACT_IDX_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='运行时实体链接表';


--
-- Table structure for table `act_ru_event_subscr`
--

DROP TABLE IF EXISTS `act_ru_event_subscr`;
CREATE TABLE `act_ru_event_subscr` (
                                       `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                       `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                       `EVENT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '事件类型',
                                       `EVENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '事件名称',
                                       `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                                       `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                       `ACTIVITY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '活动ID',
                                       `CONFIGURATION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '配置',
                                       `CREATED_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                                       `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                                       `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                       `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                       `SCOPE_DEFINITION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                       `SCOPE_DEFINITION_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义键',
                                       `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                       `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '锁定时间',
                                       `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '锁持有者',
                                       `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                       PRIMARY KEY (`ID_`),
                                       KEY `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`),
                                       KEY `ACT_IDX_EVENT_SUBSCR_EXEC_ID` (`EXECUTION_ID_`),
                                       KEY `ACT_IDX_EVENT_SUBSCR_PROC_ID` (`PROC_INST_ID_`),
                                       KEY `ACT_IDX_EVENT_SUBSCR_SCOPEREF_` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                                       CONSTRAINT `ACT_FK_EVENT_EXEC` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='事件订阅表';


--
-- Table structure for table `act_ru_execution`
--

DROP TABLE IF EXISTS `act_ru_execution`;
CREATE TABLE `act_ru_execution` (
                                    `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                    `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                    `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                    `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '业务键',
                                    `PARENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '父执行ID',
                                    `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                                    `SUPER_EXEC_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '上级执行ID',
                                    `ROOT_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '根流程实例ID',
                                    `ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '当前活动ID',
                                    `IS_ACTIVE_` tinyint DEFAULT NULL COMMENT '是否活动',
                                    `IS_CONCURRENT_` tinyint DEFAULT NULL COMMENT '是否并发',
                                    `IS_SCOPE_` tinyint DEFAULT NULL COMMENT '是否是范围',
                                    `IS_EVENT_SCOPE_` tinyint DEFAULT NULL COMMENT '是否是事件范围',
                                    `IS_MI_ROOT_` tinyint DEFAULT NULL COMMENT '是否是多实例根',
                                    `SUSPENSION_STATE_` int DEFAULT NULL COMMENT '挂起状态',
                                    `CACHED_ENT_STATE_` int DEFAULT NULL COMMENT '缓存的实体状态',
                                    `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                    `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行名称',
                                    `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '开始活动ID',
                                    `START_TIME_` datetime(3) DEFAULT NULL COMMENT '开始时间',
                                    `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '启动用户ID',
                                    `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '锁定时间',
                                    `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '锁持有者',
                                    `IS_COUNT_ENABLED_` tinyint DEFAULT NULL COMMENT '是否启用计数',
                                    `EVT_SUBSCR_COUNT_` int DEFAULT NULL COMMENT '事件订阅计数',
                                    `TASK_COUNT_` int DEFAULT NULL COMMENT '任务计数',
                                    `JOB_COUNT_` int DEFAULT NULL COMMENT '作业计数',
                                    `TIMER_JOB_COUNT_` int DEFAULT NULL COMMENT '定时器作业计数',
                                    `SUSP_JOB_COUNT_` int DEFAULT NULL COMMENT '挂起作业计数',
                                    `DEADLETTER_JOB_COUNT_` int DEFAULT NULL COMMENT '死信作业计数',
                                    `EXTERNAL_WORKER_JOB_COUNT_` int DEFAULT NULL COMMENT '外部工作者作业计数',
                                    `VAR_COUNT_` int DEFAULT NULL COMMENT '变量计数',
                                    `ID_LINK_COUNT_` int DEFAULT NULL COMMENT '身份关系计数',
                                    `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '回调ID',
                                    `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '回调类型',
                                    `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引用ID',
                                    `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '引用类型',
                                    `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '传播的阶段实例ID',
                                    `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '业务状态',
                                    PRIMARY KEY (`ID_`),
                                    KEY `ACT_FK_EXE_PARENT` (`PARENT_ID_`),
                                    KEY `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`),
                                    KEY `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`),
                                    KEY `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`),
                                    KEY `ACT_IDC_EXEC_ROOT` (`ROOT_PROC_INST_ID_`),
                                    KEY `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`),
                                    KEY `ACT_IDX_EXEC_REF_ID_` (`REFERENCE_ID_`),
                                    CONSTRAINT `ACT_FK_EXE_PARENT` FOREIGN KEY (`PARENT_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE,
                                    CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
                                    CONSTRAINT `ACT_FK_EXE_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
                                    CONSTRAINT `ACT_FK_EXE_SUPER` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='运行时执行表';


--
-- Table structure for table `act_ru_external_job`
--

DROP TABLE IF EXISTS `act_ru_external_job`;
CREATE TABLE `act_ru_external_job` (
                                       `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                       `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                       `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '作业分类',
                                       `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '作业类型',
                                       `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '锁过期时间',
                                       `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '锁持有者',
                                       `EXCLUSIVE_` tinyint(1) DEFAULT NULL COMMENT '是否排他',
                                       `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                                       `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                       `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                                       `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元素ID',
                                       `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元素名称',
                                       `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                       `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                       `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                       `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                       `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '关联ID',
                                       `RETRIES_` int DEFAULT NULL COMMENT '重试次数',
                                       `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常堆栈ID',
                                       `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常消息',
                                       `DUEDATE_` timestamp(3) NULL DEFAULT NULL COMMENT '到期时间',
                                       `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '重复表达式',
                                       `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器类型',
                                       `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器配置',
                                       `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '自定义值ID',
                                       `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
                                       `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                       PRIMARY KEY (`ID_`),
                                       KEY `ACT_IDX_EJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                                       KEY `ACT_IDX_EJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
                                       KEY `ACT_IDX_EJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
                                       KEY `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
                                       KEY `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
                                       KEY `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
                                       CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                                       CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='外部工作者作业表';


--
-- Table structure for table `act_ru_history_job`
--

DROP TABLE IF EXISTS `act_ru_history_job`;
CREATE TABLE `act_ru_history_job` (
                                      `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                      `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                      `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '锁过期时间',
                                      `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '锁持有者',
                                      `RETRIES_` int DEFAULT NULL COMMENT '重试次数',
                                      `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常堆栈ID',
                                      `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常消息',
                                      `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器类型',
                                      `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器配置',
                                      `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '自定义值ID',
                                      `ADV_HANDLER_CFG_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '高级处理器配置ID',
                                      `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
                                      `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                      `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                      PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='历史作业表';


--
-- Table structure for table `act_ru_identitylink`
--

DROP TABLE IF EXISTS `act_ru_identitylink`;
CREATE TABLE `act_ru_identitylink` (
                                       `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                       `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                       `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户组ID',
                                       `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '关系类型',
                                       `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户ID',
                                       `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务ID',
                                       `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                       `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                                       `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                       `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                       `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                       `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                       PRIMARY KEY (`ID_`),
                                       KEY `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`),
                                       KEY `ACT_FK_TSKASS_TASK` (`TASK_ID_`),
                                       KEY `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`),
                                       KEY `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`),
                                       KEY `ACT_IDX_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                                       KEY `ACT_IDX_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
                                       KEY `ACT_IDX_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
                                       KEY `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`),
                                       CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
                                       CONSTRAINT `ACT_FK_IDL_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`),
                                       CONSTRAINT `ACT_FK_TSKASS_TASK` FOREIGN KEY (`TASK_ID_`) REFERENCES `act_ru_task` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='运行时身份关系表';


--
-- Table structure for table `act_ru_job`
--

DROP TABLE IF EXISTS `act_ru_job`;
CREATE TABLE `act_ru_job` (
                              `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                              `REV_` int DEFAULT NULL COMMENT '数据版本号',
                              `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '作业分类',
                              `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '作业类型',
                              `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '锁过期时间',
                              `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '锁持有者',
                              `EXCLUSIVE_` tinyint(1) DEFAULT NULL COMMENT '是否排他',
                              `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                              `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                              `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                              `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元素ID',
                              `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元素名称',
                              `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                              `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                              `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                              `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                              `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '关联ID',
                              `RETRIES_` int DEFAULT NULL COMMENT '重试次数',
                              `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常堆栈ID',
                              `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常消息',
                              `DUEDATE_` timestamp(3) NULL DEFAULT NULL COMMENT '到期时间',
                              `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '重复表达式',
                              `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器类型',
                              `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器配置',
                              `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '自定义值ID',
                              `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
                              `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                              PRIMARY KEY (`ID_`),
                              KEY `ACT_FK_JOB_EXECUTION` (`EXECUTION_ID_`),
                              KEY `ACT_FK_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
                              KEY `ACT_FK_JOB_PROC_DEF` (`PROC_DEF_ID_`),
                              KEY `ACT_IDX_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
                              KEY `ACT_IDX_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
                              KEY `ACT_IDX_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
                              KEY `ACT_IDX_JOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                              KEY `ACT_IDX_JOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
                              KEY `ACT_IDX_JOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
                              CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                              CONSTRAINT `ACT_FK_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                              CONSTRAINT `ACT_FK_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
                              CONSTRAINT `ACT_FK_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
                              CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='运行时作业表';


--
-- Table structure for table `act_ru_suspended_job`
--

DROP TABLE IF EXISTS `act_ru_suspended_job`;
CREATE TABLE `act_ru_suspended_job` (
                                        `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                        `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                        `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '作业分类',
                                        `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '作业类型',
                                        `EXCLUSIVE_` tinyint(1) DEFAULT NULL COMMENT '是否排他',
                                        `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                                        `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                        `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                                        `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元素ID',
                                        `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元素名称',
                                        `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                        `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                        `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                        `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                        `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '关联ID',
                                        `RETRIES_` int DEFAULT NULL COMMENT '重试次数',
                                        `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常堆栈ID',
                                        `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常消息',
                                        `DUEDATE_` timestamp(3) NULL DEFAULT NULL COMMENT '到期时间',
                                        `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '重复表达式',
                                        `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器类型',
                                        `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器配置',
                                        `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '自定义值ID',
                                        `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
                                        `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                        PRIMARY KEY (`ID_`),
                                        KEY `ACT_FK_SUSPENDED_JOB_EXECUTION` (`EXECUTION_ID_`),
                                        KEY `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
                                        KEY `ACT_FK_SUSPENDED_JOB_PROC_DEF` (`PROC_DEF_ID_`),
                                        KEY `ACT_IDX_SJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                                        KEY `ACT_IDX_SJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
                                        KEY `ACT_IDX_SJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
                                        KEY `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
                                        KEY `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
                                        KEY `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
                                        CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                                        CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                                        CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
                                        CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
                                        CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='挂起作业表';


--
-- Table structure for table `act_ru_task`
--

DROP TABLE IF EXISTS `act_ru_task`;
CREATE TABLE `act_ru_task` (
                               `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                               `REV_` int DEFAULT NULL COMMENT '数据版本号',
                               `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                               `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                               `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                               `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务定义ID',
                               `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                               `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                               `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                               `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                               `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '传播的阶段实例ID',
                               `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务状态',
                               `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务名称',
                               `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '父任务ID',
                               `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务描述',
                               `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务定义键',
                               `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务所有者',
                               `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务执行人',
                               `DELEGATION_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '委托状态',
                               `PRIORITY_` int DEFAULT NULL COMMENT '优先级',
                               `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
                               `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL COMMENT '进行中时间',
                               `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '进行中开始人',
                               `CLAIM_TIME_` datetime(3) DEFAULT NULL COMMENT '签收时间',
                               `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '签收人',
                               `SUSPENDED_TIME_` datetime(3) DEFAULT NULL COMMENT '挂起时间',
                               `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '挂起人',
                               `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL COMMENT '进行中截止日期',
                               `DUE_DATE_` datetime(3) DEFAULT NULL COMMENT '截止日期',
                               `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务分类',
                               `SUSPENSION_STATE_` int DEFAULT NULL COMMENT '挂起状态',
                               `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                               `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '表单键',
                               `IS_COUNT_ENABLED_` tinyint DEFAULT NULL COMMENT '是否启用计数',
                               `VAR_COUNT_` int DEFAULT NULL COMMENT '变量计数',
                               `ID_LINK_COUNT_` int DEFAULT NULL COMMENT '身份关系计数',
                               `SUB_TASK_COUNT_` int DEFAULT NULL COMMENT '子任务计数',
                               PRIMARY KEY (`ID_`),
                               KEY `ACT_FK_TASK_EXE` (`EXECUTION_ID_`),
                               KEY `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`),
                               KEY `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`),
                               KEY `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`),
                               KEY `ACT_IDX_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                               KEY `ACT_IDX_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
                               KEY `ACT_IDX_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
                               CONSTRAINT `ACT_FK_TASK_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
                               CONSTRAINT `ACT_FK_TASK_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
                               CONSTRAINT `ACT_FK_TASK_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='运行时任务表';


--
-- Table structure for table `act_ru_timer_job`
--

DROP TABLE IF EXISTS `act_ru_timer_job`;
CREATE TABLE `act_ru_timer_job` (
                                    `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                    `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                    `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '作业分类',
                                    `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '作业类型',
                                    `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '锁过期时间',
                                    `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '锁持有者',
                                    `EXCLUSIVE_` tinyint(1) DEFAULT NULL COMMENT '是否排他',
                                    `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                                    `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                    `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程定义ID',
                                    `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元素ID',
                                    `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元素名称',
                                    `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                    `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                    `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                    `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围定义ID',
                                    `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '关联ID',
                                    `RETRIES_` int DEFAULT NULL COMMENT '重试次数',
                                    `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常堆栈ID',
                                    `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '异常消息',
                                    `DUEDATE_` timestamp(3) NULL DEFAULT NULL COMMENT '到期时间',
                                    `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '重复表达式',
                                    `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器类型',
                                    `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '处理器配置',
                                    `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '自定义值ID',
                                    `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
                                    `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '' COMMENT '租户ID',
                                    PRIMARY KEY (`ID_`),
                                    KEY `ACT_FK_TIMER_JOB_EXECUTION` (`EXECUTION_ID_`),
                                    KEY `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
                                    KEY `ACT_FK_TIMER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
                                    KEY `ACT_IDX_TIMER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
                                    KEY `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
                                    KEY `ACT_IDX_TIMER_JOB_DUEDATE` (`DUEDATE_`),
                                    KEY `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
                                    KEY `ACT_IDX_TJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                                    KEY `ACT_IDX_TJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
                                    KEY `ACT_IDX_TJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
                                    CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                                    CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                                    CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
                                    CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
                                    CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='定时器作业表';


--
-- Table structure for table `act_ru_variable`
--

DROP TABLE IF EXISTS `act_ru_variable`;
CREATE TABLE `act_ru_variable` (
                                   `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                   `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                   `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '变量类型',
                                   `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '变量名称',
                                   `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '执行ID',
                                   `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '流程实例ID',
                                   `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '任务ID',
                                   `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围ID',
                                   `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '子范围ID',
                                   `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '范围类型',
                                   `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '字节数组ID',
                                   `DOUBLE_` double DEFAULT NULL COMMENT 'Double类型值',
                                   `LONG_` bigint DEFAULT NULL COMMENT 'Long类型值',
                                   `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '文本类型值',
                                   `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '附加文本值',
                                   `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '元信息',
                                   PRIMARY KEY (`ID_`),
                                   KEY `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`),
                                   KEY `ACT_FK_VAR_EXE` (`EXECUTION_ID_`),
                                   KEY `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`),
                                   KEY `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
                                   KEY `ACT_IDX_RU_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
                                   KEY `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`),
                                   CONSTRAINT `ACT_FK_VAR_BYTEARRAY` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
                                   CONSTRAINT `ACT_FK_VAR_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
                                   CONSTRAINT `ACT_FK_VAR_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='运行时变量表';


--
-- Table structure for table `gen_table`
--

