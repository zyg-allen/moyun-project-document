-- MySQL dump 10.13  Distrib 8.4.6, for Win64 (x86_64)
--
-- Host: localhost    Database: moyun-db2
-- ------------------------------------------------------
-- Server version	8.4.6

DROP TABLE IF EXISTS `act_evt_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `act_evt_log`


LOCK TABLES `act_evt_log` WRITE;
/*!40000 ALTER TABLE `act_evt_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_evt_log` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `act_ge_bytearray`


DROP TABLE IF EXISTS `act_ge_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `act_ge_bytearray`


LOCK TABLES `act_ge_bytearray` WRITE;
/*!40000 ALTER TABLE `act_ge_bytearray` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ge_bytearray` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `act_ge_property`


DROP TABLE IF EXISTS `act_ge_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ge_property` (
                                   `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
                                   `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
                                   `REV_` int DEFAULT NULL,
                                   PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `act_ge_property`


LOCK TABLES `act_ge_property` WRITE;
/*!40000 ALTER TABLE `act_ge_property` DISABLE KEYS */;
INSERT INTO `act_ge_property` VALUES ('app.schema.version','7.1.0.1',1),('cfg.execution-related-entities-count','true',1),('cfg.task-related-entities-count','true',1),('common.schema.version','7.1.0.2',1),('next.dbid','1',1),('schema.history','create(7.1.0.2)',1),('schema.version','7.1.0.2',1);
/*!40000 ALTER TABLE `act_ge_property` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `act_hi_actinst`


DROP TABLE IF EXISTS `act_hi_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `act_hi_actinst`


LOCK TABLES `act_hi_actinst` WRITE;
/*!40000 ALTER TABLE `act_hi_actinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_actinst` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `act_hi_attachment`


DROP TABLE IF EXISTS `act_hi_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_attachment`
--

LOCK TABLES `act_hi_attachment` WRITE;
/*!40000 ALTER TABLE `act_hi_attachment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_comment`
--

DROP TABLE IF EXISTS `act_hi_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_comment`
--

LOCK TABLES `act_hi_comment` WRITE;
/*!40000 ALTER TABLE `act_hi_comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_detail`
--

DROP TABLE IF EXISTS `act_hi_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_detail`
--

LOCK TABLES `act_hi_detail` WRITE;
/*!40000 ALTER TABLE `act_hi_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_entitylink`
--

DROP TABLE IF EXISTS `act_hi_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_entitylink`
--

LOCK TABLES `act_hi_entitylink` WRITE;
/*!40000 ALTER TABLE `act_hi_entitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_entitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_identitylink`
--

DROP TABLE IF EXISTS `act_hi_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_identitylink`
--

LOCK TABLES `act_hi_identitylink` WRITE;
/*!40000 ALTER TABLE `act_hi_identitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_identitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_procinst`
--

DROP TABLE IF EXISTS `act_hi_procinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_procinst`
--

LOCK TABLES `act_hi_procinst` WRITE;
/*!40000 ALTER TABLE `act_hi_procinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_procinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_taskinst`
--

DROP TABLE IF EXISTS `act_hi_taskinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_taskinst`
--

LOCK TABLES `act_hi_taskinst` WRITE;
/*!40000 ALTER TABLE `act_hi_taskinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_taskinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_tsk_log`
--

DROP TABLE IF EXISTS `act_hi_tsk_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_tsk_log`
--

LOCK TABLES `act_hi_tsk_log` WRITE;
/*!40000 ALTER TABLE `act_hi_tsk_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_tsk_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_varinst`
--

DROP TABLE IF EXISTS `act_hi_varinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_varinst`
--

LOCK TABLES `act_hi_varinst` WRITE;
/*!40000 ALTER TABLE `act_hi_varinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_varinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_bytearray`
--

DROP TABLE IF EXISTS `act_id_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_bytearray` (
                                    `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                    `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                    `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '字节数组名称',
                                    `BYTES_` longblob COMMENT '字节数据',
                                    PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='身份管理字节数组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_bytearray`
--

LOCK TABLES `act_id_bytearray` WRITE;
/*!40000 ALTER TABLE `act_id_bytearray` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_bytearray` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_group`
--

DROP TABLE IF EXISTS `act_id_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_group` (
                                `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                                `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户组名称',
                                `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户组类型',
                                PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_group`
--

LOCK TABLES `act_id_group` WRITE;
/*!40000 ALTER TABLE `act_id_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_info`
--

DROP TABLE IF EXISTS `act_id_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_info`
--

LOCK TABLES `act_id_info` WRITE;
/*!40000 ALTER TABLE `act_id_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_membership`
--

DROP TABLE IF EXISTS `act_id_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_membership` (
                                     `USER_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '用户ID',
                                     `GROUP_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '用户组ID',
                                     PRIMARY KEY (`USER_ID_`,`GROUP_ID_`),
                                     KEY `ACT_FK_MEMB_GROUP` (`GROUP_ID_`),
                                     CONSTRAINT `ACT_FK_MEMB_GROUP` FOREIGN KEY (`GROUP_ID_`) REFERENCES `act_id_group` (`ID_`),
                                     CONSTRAINT `ACT_FK_MEMB_USER` FOREIGN KEY (`USER_ID_`) REFERENCES `act_id_user` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户组成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_membership`
--

LOCK TABLES `act_id_membership` WRITE;
/*!40000 ALTER TABLE `act_id_membership` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_membership` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_priv`
--

DROP TABLE IF EXISTS `act_id_priv`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_priv` (
                               `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '主键ID',
                               `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL COMMENT '权限名称',
                               PRIMARY KEY (`ID_`),
                               UNIQUE KEY `ACT_UNIQ_PRIV_NAME` (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_priv`
--

LOCK TABLES `act_id_priv` WRITE;
/*!40000 ALTER TABLE `act_id_priv` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_priv` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_priv_mapping`
--

DROP TABLE IF EXISTS `act_id_priv_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_priv_mapping`
--

LOCK TABLES `act_id_priv_mapping` WRITE;
/*!40000 ALTER TABLE `act_id_priv_mapping` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_priv_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_property`
--

DROP TABLE IF EXISTS `act_id_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_property` (
                                   `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL COMMENT '属性名称',
                                   `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '属性值',
                                   `REV_` int DEFAULT NULL COMMENT '数据版本号',
                                   PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='身份管理属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_property`
--

LOCK TABLES `act_id_property` WRITE;
/*!40000 ALTER TABLE `act_id_property` DISABLE KEYS */;
INSERT INTO `act_id_property` VALUES ('schema.version','7.1.0.2',1);
/*!40000 ALTER TABLE `act_id_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_token`
--

DROP TABLE IF EXISTS `act_id_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_token`
--

LOCK TABLES `act_id_token` WRITE;
/*!40000 ALTER TABLE `act_id_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_user`
--

DROP TABLE IF EXISTS `act_id_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_user`
--

LOCK TABLES `act_id_user` WRITE;
/*!40000 ALTER TABLE `act_id_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_procdef_info`
--

DROP TABLE IF EXISTS `act_procdef_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;



LOCK TABLES `act_procdef_info` WRITE;
/*!40000 ALTER TABLE `act_procdef_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_procdef_info` ENABLE KEYS */;
UNLOCK TABLES;



DROP TABLE IF EXISTS `act_re_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_model`
--

LOCK TABLES `act_re_model` WRITE;
/*!40000 ALTER TABLE `act_re_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_re_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_re_procdef`
--

DROP TABLE IF EXISTS `act_re_procdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_procdef`
--

LOCK TABLES `act_re_procdef` WRITE;
/*!40000 ALTER TABLE `act_re_procdef` DISABLE KEYS */;
INSERT INTO `act_re_procdef` VALUES ('flow_dr8o4m4n:1:e3f3dc3e-3d55-11f1-9c2a-745d229dae59',2,'leave','flow_mu6nh7qx','flow_dr8o4m4n',1,'e33dab9b-3d55-11f1-9c2a-745d229dae59','flow_mu6nh7qx.bpmn','flow_mu6nh7qx.flow_dr8o4m4n.png',NULL,0,1,1,'',NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `act_re_procdef` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_actinst`
--

DROP TABLE IF EXISTS `act_ru_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_actinst`
--

LOCK TABLES `act_ru_actinst` WRITE;
/*!40000 ALTER TABLE `act_ru_actinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_actinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_deadletter_job`
--

DROP TABLE IF EXISTS `act_ru_deadletter_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_deadletter_job`
--

LOCK TABLES `act_ru_deadletter_job` WRITE;
/*!40000 ALTER TABLE `act_ru_deadletter_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_deadletter_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_entitylink`
--

DROP TABLE IF EXISTS `act_ru_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_entitylink`
--

LOCK TABLES `act_ru_entitylink` WRITE;
/*!40000 ALTER TABLE `act_ru_entitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_entitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_event_subscr`
--

DROP TABLE IF EXISTS `act_ru_event_subscr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_event_subscr`
--

LOCK TABLES `act_ru_event_subscr` WRITE;
/*!40000 ALTER TABLE `act_ru_event_subscr` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_event_subscr` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_execution`
--

DROP TABLE IF EXISTS `act_ru_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_execution`
--

LOCK TABLES `act_ru_execution` WRITE;
/*!40000 ALTER TABLE `act_ru_execution` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_execution` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_external_job`
--

DROP TABLE IF EXISTS `act_ru_external_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_external_job`
--

LOCK TABLES `act_ru_external_job` WRITE;
/*!40000 ALTER TABLE `act_ru_external_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_external_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_history_job`
--

DROP TABLE IF EXISTS `act_ru_history_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_history_job`
--

LOCK TABLES `act_ru_history_job` WRITE;
/*!40000 ALTER TABLE `act_ru_history_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_history_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_identitylink`
--

DROP TABLE IF EXISTS `act_ru_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_identitylink`
--

LOCK TABLES `act_ru_identitylink` WRITE;
/*!40000 ALTER TABLE `act_ru_identitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_identitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_job`
--

DROP TABLE IF EXISTS `act_ru_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_job`
--

LOCK TABLES `act_ru_job` WRITE;
/*!40000 ALTER TABLE `act_ru_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_suspended_job`
--

DROP TABLE IF EXISTS `act_ru_suspended_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_suspended_job`
--

LOCK TABLES `act_ru_suspended_job` WRITE;
/*!40000 ALTER TABLE `act_ru_suspended_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_suspended_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_task`
--

DROP TABLE IF EXISTS `act_ru_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_task`
--

LOCK TABLES `act_ru_task` WRITE;
/*!40000 ALTER TABLE `act_ru_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_timer_job`
--

DROP TABLE IF EXISTS `act_ru_timer_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_timer_job`
--

LOCK TABLES `act_ru_timer_job` WRITE;
/*!40000 ALTER TABLE `act_ru_timer_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_timer_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_variable`
--

DROP TABLE IF EXISTS `act_ru_variable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_variable`
--

LOCK TABLES `act_ru_variable` WRITE;
/*!40000 ALTER TABLE `act_ru_variable` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_variable` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table` (
                             `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                             `table_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '表名称',
                             `table_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '表描述',
                             `sub_table_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联子表的表名',
                             `sub_table_fk_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子表关联的外键名',
                             `class_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '实体类名称',
                             `tpl_category` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
                             `tpl_web_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
                             `package_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成包路径',
                             `module_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成模块名',
                             `business_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成业务名',
                             `function_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成功能名',
                             `function_author` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成功能作者',
                             `gen_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
                             `gen_path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
                             `options` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '其它生成选项',
                             `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                             `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                             `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                             PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table`
--

LOCK TABLES `gen_table` WRITE;
/*!40000 ALTER TABLE `gen_table` DISABLE KEYS */;
/*!40000 ALTER TABLE `gen_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table_column` (
                                    `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                                    `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
                                    `column_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '列名称',
                                    `column_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '列描述',
                                    `column_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '列类型',
                                    `java_type` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JAVA类型',
                                    `java_field` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JAVA字段名',
                                    `is_pk` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否主键（1是）',
                                    `is_increment` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否自增（1是）',
                                    `is_required` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否必填（1是）',
                                    `is_insert` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否为插入字段（1是）',
                                    `is_edit` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否编辑字段（1是）',
                                    `is_list` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否列表字段（1是）',
                                    `is_query` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否查询字段（1是）',
                                    `query_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
                                    `html_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
                                    `dict_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
                                    `sort` int DEFAULT NULL COMMENT '排序',
                                    `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                    `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                    `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                    PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table_column`
--

LOCK TABLES `gen_table_column` WRITE;
/*!40000 ALTER TABLE `gen_table_column` DISABLE KEYS */;
/*!40000 ALTER TABLE `gen_table_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_achievement`
--

DROP TABLE IF EXISTS `portal_achievement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_achievement`
--

LOCK TABLES `portal_achievement` WRITE;
/*!40000 ALTER TABLE `portal_achievement` DISABLE KEYS */;
INSERT INTO `portal_achievement` VALUES (1,'first_article','初露锋芒','发布第一篇文章',NULL,'article','{\"action\":\"publish_article\",\"count\":1}',20,1,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(2,'article_10','勤勉作者','发布10篇文章',NULL,'article','{\"action\":\"publish_article\",\"count\":10}',50,2,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(3,'article_50','高产作者','发布50篇文章',NULL,'article','{\"action\":\"publish_article\",\"count\":50}',200,3,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(4,'article_featured','精华创作者','文章被精选',NULL,'article','{\"action\":\"article_featured\",\"count\":1}',100,4,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(5,'article_100_likes','人气作者','单篇文章获赞100',NULL,'article','{\"action\":\"receive_like\",\"count\":100}',50,5,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(6,'first_book','开卷有益','完成阅读第一本书',NULL,'reading','{\"action\":\"finish_book\",\"count\":1}',20,10,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(7,'book_worm_10','书虫','完成阅读10本书',NULL,'reading','{\"action\":\"finish_book\",\"count\":10}',100,11,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(8,'book_worm_50','阅读达人','完成阅读50本书',NULL,'reading','{\"action\":\"finish_book\",\"count\":50}',300,12,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(9,'first_booklist','书单策划','创建第一个书单',NULL,'reading','{\"action\":\"create_booklist\",\"count\":1}',20,13,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(10,'quote_master','金句达人','发布20条金句',NULL,'reading','{\"action\":\"write_quote\",\"count\":20}',50,14,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(11,'first_solve','初试身手','解答第一道面试题',NULL,'interview','{\"action\":\"solve_question\",\"count\":1}',10,20,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(12,'solve_50','刷题能手','解答50道面试题',NULL,'interview','{\"action\":\"solve_question\",\"count\":50}',100,21,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(13,'solve_200','面试达人','解答200道面试题',NULL,'interview','{\"action\":\"solve_question\",\"count\":200}',300,22,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(14,'first_note','笔记新手','撰写第一篇笔记',NULL,'interview','{\"action\":\"write_note\",\"count\":1}',15,23,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(15,'note_adopted','知识贡献者','笔记被精选',NULL,'interview','{\"action\":\"note_adopted\",\"count\":1}',50,24,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(16,'first_experience','面经分享者','发布第一篇面经',NULL,'interview','{\"action\":\"publish_experience\",\"count\":1}',30,25,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(17,'experience_10','面经达人','发布10篇面经',NULL,'interview','{\"action\":\"publish_experience\",\"count\":10}',100,26,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(18,'checkin_7','坚持一周','连续签到7天',NULL,'all','{\"action\":\"daily_checkin\",\"count\":7}',10,30,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(19,'checkin_30','坚持一月','连续签到30天',NULL,'all','{\"action\":\"daily_checkin\",\"count\":30}',50,31,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(20,'level_5','渐入佳境','达到5级',NULL,'all','{\"action\":\"level\",\"count\":5}',0,32,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(21,'level_8','登峰造极','达到8级',NULL,'all','{\"action\":\"level\",\"count\":8}',0,33,'0','','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(22,'first_tip_received','初获鼓励','首次收到打赏',NULL,'article','{\"action\":\"receive_tip\",\"count\":1}',10,7,'0','','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),(23,'generous_tipper','慷慨鼓励','累计打赏他人 10 次',NULL,'article','{\"action\":\"tip_others\",\"count\":10}',30,8,'0','','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL);
/*!40000 ALTER TABLE `portal_achievement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_ad_slot`
--

DROP TABLE IF EXISTS `portal_ad_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_ad_slot` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '广告位ID',
                                  `slot_key` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '广告位标识，如 article_detail_bottom',
                                  `title` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '广告标题',
                                  `image` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '广告图片URL',
                                  `link` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '点击跳转链接',
                                  `content` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '广告文案',
                                  `sort` int DEFAULT '0' COMMENT '排序',
                                  `status` varchar(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态：0=启用 1=停用',
                                  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_slot_key` (`slot_key`),
                                  KEY `idx_status` (`status`),
                                  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户自研广告位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_ad_slot`
--

LOCK TABLES `portal_ad_slot` WRITE;
/*!40000 ALTER TABLE `portal_ad_slot` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_ad_slot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_article`
--

DROP TABLE IF EXISTS `portal_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_article` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
                                  `title` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '文章标题',
                                  `slug` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文章URL别名，用于SEO语义化路径',
                                  `content` longtext COLLATE utf8mb4_general_ci COMMENT '文章内容（HTML格式）',
                                  `excerpt` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文章摘要',
                                  `cover` text COLLATE utf8mb4_general_ci COMMENT '封面图片URL或Base64',
                                  `author_id` bigint NOT NULL COMMENT '作者ID（门户用户ID）',
                                  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
                                  `root_category_id` bigint DEFAULT NULL COMMENT '顶级分类ID',
                                  `status` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'draft' COMMENT '状态：draft=草稿 / pending=待审核 / published=已发布 / rejected=已拒绝 / archived=已归档',
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
                                  `link` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '外部链接',
                                  `editor_mode` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
                                  `session_token` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '编辑会话标识（一次编辑会话唯一，用于草稿/发布幂等去重）',
                                  `content_markdown` text COLLATE utf8mb4_general_ci COMMENT 'Markdown 原始内容',
                                  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                  `category_path` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类路径，包含所有祖先ID，例如：1,3,5',
                                  `is_paid` tinyint NOT NULL DEFAULT '0' COMMENT '是否付费阅读 0=免费 1=付费',
                                  `paid_content` longtext COLLATE utf8mb4_general_ci COMMENT '付费内容（购买后可见）',
                                  `preview_length` int NOT NULL DEFAULT '0' COMMENT '试读字数（未购买可预览的字数）',
                                  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '付费价格，0=免费',
                                  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  `business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务主键（前缀art_）',
                                  `author_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作者业务主键（关联 portal_user.business_id）',
                                  `category_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类业务主键（关联 portal_category.business_id）',
                                  `root_category_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '顶级分类业务主键（关联 portal_category.business_id）',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_slug` (`slug`),
                                  UNIQUE KEY `uk_business_id` (`business_id`),
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
                                  KEY `idx_del_flag` (`del_flag`),
                                  KEY `idx_author_bid` (`author_business_id`),
                                  KEY `idx_category_bid` (`category_business_id`),
                                  KEY `idx_root_category_bid` (`root_category_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户文章表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_article`
--

LOCK TABLES `portal_article` WRITE;
/*!40000 ALTER TABLE `portal_article` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_article` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_article_tag`
--

DROP TABLE IF EXISTS `portal_article_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_article_tag` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                      `article_id` bigint NOT NULL COMMENT '文章ID',
                                      `tag_id` bigint NOT NULL COMMENT '标签ID',
                                      `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                      `article_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文章业务主键（关联 portal_article.business_id）',
                                      `tag_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签业务主键（关联 portal_tag.business_id）',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_article_tag` (`article_id`,`tag_id`),
                                      UNIQUE KEY `uk_article_tag_bid` (`article_business_id`,`tag_business_id`),
                                      KEY `idx_article_id` (`article_id`),
                                      KEY `idx_tag_id` (`tag_id`),
                                      KEY `idx_article_bid` (`article_business_id`),
                                      KEY `idx_tag_bid` (`tag_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文章标签关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_article_tag`
--

LOCK TABLES `portal_article_tag` WRITE;
/*!40000 ALTER TABLE `portal_article_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_article_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_article_version`
--

DROP TABLE IF EXISTS `portal_article_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
                                          `article_business_id` varchar(32) DEFAULT NULL COMMENT '文章业务主键（关联 portal_article.business_id）',
                                          `operator_business_id` varchar(32) DEFAULT NULL COMMENT '操作人业务主键（关联 portal_user.business_id）',
                                          PRIMARY KEY (`id`),
                                          KEY `idx_article_version` (`article_id`,`version_no`),
                                          KEY `idx_article_bid` (`article_business_id`),
                                          KEY `idx_operator_bid` (`operator_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章版本快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_article_version`
--

LOCK TABLES `portal_article_version` WRITE;
/*!40000 ALTER TABLE `portal_article_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_article_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_article_view`
--

DROP TABLE IF EXISTS `portal_article_view`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_article_view` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                       `article_id` bigint NOT NULL COMMENT '文章ID',
                                       `user_id` bigint DEFAULT NULL COMMENT '用户ID（NULL表示游客）',
                                       `ip` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP地址',
                                       `view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
                                       `user_agent` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '浏览器User-Agent',
                                       `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                       `article_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文章业务主键（关联 portal_article.business_id）',
                                       `user_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户业务主键（关联 portal_user.business_id，NULL=游客）',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_article_id` (`article_id`),
                                       KEY `idx_user_id` (`user_id`),
                                       KEY `idx_ip` (`ip`),
                                       KEY `idx_view_time` (`view_time`),
                                       KEY `idx_article_user` (`article_id`,`user_id`),
                                       KEY `idx_article_ip` (`article_id`,`ip`),
                                       KEY `idx_article_viewtime` (`article_id`,`view_time`),
                                       KEY `idx_article_bid` (`article_business_id`),
                                       KEY `idx_user_bid` (`user_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文章浏览记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_article_view`
--

LOCK TABLES `portal_article_view` WRITE;
/*!40000 ALTER TABLE `portal_article_view` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_article_view` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_book`
--

DROP TABLE IF EXISTS `portal_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book`
--

LOCK TABLES `portal_book` WRITE;
/*!40000 ALTER TABLE `portal_book` DISABLE KEYS */;
INSERT INTO `portal_book` VALUES (1,'工程师修炼之道：从码农到架构师','墨韵技术社','https://images.moyun.com/books/engineer-way-cover.jpg','本书写给所有在代码世界里摸爬滚打的工程师。从写出第一行可运行的代码，到设计支撑百万并发的系统，这条路没有捷径，但有方向。十个章节，覆盖代码质量、系统设计、数据库、API、并发、安全、性能、DevOps 到技术领导力，每一章都是一次认知升级。','978-7-2026-0001-1','墨韵出版社','2026-06-15',320,NULL,'工程师,架构,后端,成长,系统设计',4.85,0,'active','published','completed',85000,10,10,'第十章 技术领导力','2026-07-28 16:31:38',1,'free',100,0.00,1,1,'一本面向中高级工程师的实战进阶指南，覆盖代码质量、架构设计、数据库优化、并发编程、安全防护、性能调优与团队协作，用真实案例讲透从\"能写代码\"到\"能扛系统\"的完整路径。','墨韵技术社，由多位一线互联网公司资深工程师组成，专注于技术写作与工程实践传播。','admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(2,'山河万里·长歌行','墨韵文学社','https://images.moyun.com/books/changge-cover.jpg','一部横跨百年山河的史诗长卷。从北疆风雪到江南烟雨，从金戈铁马到诗酒田园，五个篇章写尽一代人的聚散与浮沉。每章逾七千字，适合长文阅读与分页翻页体验测试。','978-7-2026-0094-4','墨韵出版社','2026-07-01',580,NULL,'长篇小说,历史,史诗,山河,长歌',4.90,0,'active','published','completed',36500,5,NULL,NULL,NULL,1,'free',100,0.00,1,0,'【测试专用·长篇】本卷为阅读器分页/滑动测试数据，单章字数均在 7000 字以上，可充分触发 ChapterReaderPage 的分页模式（5+ 页）与移动端左右滑动翻页。','墨韵文学社，致力于长篇文学创作与数字阅读体验研究。','admin','2026-07-28 16:39:00','','2026-07-28 16:39:00',NULL,'0');
/*!40000 ALTER TABLE `portal_book` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_book_chapter`
--

DROP TABLE IF EXISTS `portal_book_chapter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_book_chapter` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `book_id` bigint NOT NULL COMMENT '所属书籍ID',
                                       `title` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '章节标题',
                                       `content` longtext COLLATE utf8mb4_general_ci COMMENT '章节正文（HTML格式，上限4GB）',
                                       `content_markdown` text COLLATE utf8mb4_general_ci COMMENT 'Markdown原始内容（上限64KB，单章足够）',
                                       `editor_mode` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
                                       `word_count` int DEFAULT '0' COMMENT '字数统计',
                                       `chapter_no` int NOT NULL DEFAULT '0' COMMENT '章节序号（用于排序，从1开始）',
                                       `volume_id` bigint DEFAULT NULL COMMENT '所属分卷ID（可选，支持分卷管理）',
                                       `is_free` tinyint(1) DEFAULT '1' COMMENT '是否免费：1=免费，0=VIP章节',
                                       `price` decimal(10,2) DEFAULT '0.00' COMMENT '章节单价（元，VIP章节购买）',
                                       `is_published` tinyint(1) DEFAULT '0' COMMENT '是否已发布：0=草稿，1=已发布',
                                       `publish_time` datetime DEFAULT NULL COMMENT '发布时间（支持定时发布）',
                                       `view_count` bigint DEFAULT '0' COMMENT '章节浏览量',
                                       `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                       `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_book_chapter_no` (`book_id`,`chapter_no`),
                                       KEY `idx_book_id` (`book_id`),
                                       KEY `idx_publish_time` (`publish_time`),
                                       KEY `idx_is_published` (`is_published`),
                                       KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='书籍章节表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_chapter`
--

LOCK TABLES `portal_book_chapter` WRITE;
/*!40000 ALTER TABLE `portal_book_chapter` DISABLE KEYS */;
INSERT INTO `portal_book_chapter` VALUES (1,1,'第一章 工程师的成长路径','<h2>1.1 从码农到工程师</h2><p>很多人把\"写代码\"等同于\"做工程\"，这是一个常见的认知偏差。写代码只是手段，解决问题才是目的。一个成熟的工程师，首先想的不是用什么框架，而是这个问题本质是什么、边界在哪、谁来用、用多久。</p><p>成长路径通常分为三个阶段：能完成（把需求变成可运行代码）、能做对（考虑边界、异常、可维护性）、能扛事（对系统的可用性、成本、演进负责）。多数人卡在第一阶段到第二阶段的跨越，因为那意味着从\"实现思维\"转向\"工程思维\"。</p><h2>1.2 技术深度的三个层次</h2><p>第一层：会用。知道 API 怎么调，框架怎么配。</p><p>第二层：懂原理。知道 API 背后做了什么，框架的设计权衡是什么。</p><p>第三层：能造轮子。在理解原理的基础上，能针对自己的场景设计替代方案。注意，能造轮子不等于一定要造，但具备这个能力，意味着你对技术的理解已经穿透了表象。</p><h2>1.3 刻意练习</h2><p>读源码是高效的刻意练习方式之一。但不要从头到尾通读，而是带着问题去读：为什么这里用策略模式而不是 if-else？为什么这个缓存要用 ConcurrentHashMap 而不是 HashMap 加锁？每一个\"为什么\"的答案，都是一次认知边界的扩展。</p>','## 1.1 从码农到工程师\n\n很多人把\"写代码\"等同于\"做工程\"，这是一个常见的认知偏差。写代码只是手段，解决问题才是目的。一个成熟的工程师，首先想的不是用什么框架，而是这个问题本质是什么、边界在哪、谁来用、用多久。\n\n成长路径通常分为三个阶段：能完成（把需求变成可运行代码）、能做对（考虑边界、异常、可维护性）、能扛事（对系统的可用性、成本、演进负责）。多数人卡在第一阶段到第二阶段的跨越，因为那意味着从\"实现思维\"转向\"工程思维\"。\n\n## 1.2 技术深度的三个层次\n\n第一层：会用。知道 API 怎么调，框架怎么配。\n\n第二层：懂原理。知道 API 背后做了什么，框架的设计权衡是什么。\n\n第三层：能造轮子。在理解原理的基础上，能针对自己的场景设计替代方案。注意，能造轮子不等于一定要造，但具备这个能力，意味着你对技术的理解已经穿透了表象。\n\n## 1.3 刻意练习\n\n读源码是高效的刻意练习方式之一。但不要从头到尾通读，而是带着问题去读：为什么这里用策略模式而不是 if-else？为什么这个缓存要用 ConcurrentHashMap 而不是 HashMap 加锁？每一个\"为什么\"的答案，都是一次认知边界的扩展。','richtext',580,1,NULL,1,0.00,1,'2026-07-28 16:31:38',0,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(2,1,'第二章 代码质量与整洁之道','<h2>2.1 命名：最廉价的工程质量</h2><p>好的命名是自解释的，读到名字就知道它在做什么，不需要跳进去看实现。坏命名有三个典型特征：缩写（usr、cnt、flg）、泛化（data、info、manager）、误导（叫 list 实际是 map）。</p><h2>2.2 函数：短小再短小</h2><p>一个函数只做一件事。判断标准：能否用一句话描述它的职责而不用\"和\"字。如果\"做A和做B\"，就该拆成两个函数。短函数的好处不是\"代码行数少\"，而是降低认知负担——人脑能同时持有的上下文是有限的。</p><h2>2.3 注释：写\"为什么\"而不是\"是什么\"</h2><p>代码已经说了\"是什么\"，注释要补的是\"为什么\"。比如 <code>// 这里 +1 是因为后端分页从0开始，前端从1开始</code> 是好注释；<code>// 循环数组</code> 就是废话。</p><h2>2.4 异常处理</h2><p>不要吞异常。<code>catch(Exception e) {}</code> 是工程灾难。要么处理、要么抛出、要么转换成业务异常并记日志。静默吞掉的异常，会在生产环境变成无法定位的幽灵问题。</p>','## 2.1 命名：最廉价的工程质量\n\n好的命名是自解释的，读到名字就知道它在做什么，不需要跳进去看实现。坏命名有三个典型特征：缩写（usr、cnt、flg）、泛化（data、info、manager）、误导（叫 list 实际是 map）。\n\n## 2.2 函数：短小再短小\n\n一个函数只做一件事。判断标准：能否用一句话描述它的职责而不用\"和\"字。如果\"做A和做B\"，就该拆成两个函数。短函数的好处不是\"代码行数少\"，而是降低认知负担——人脑能同时持有的上下文是有限的。\n\n## 2.3 注释：写\"为什么\"而不是\"是什么\"\n\n代码已经说了\"是什么\"，注释要补的是\"为什么\"。比如 `// 这里 +1 是因为后端分页从0开始，前端从1开始` 是好注释；`// 循环数组` 就是废话。\n\n## 2.4 异常处理\n\n不要吞异常。`catch(Exception e) {}` 是工程灾难。要么处理、要么抛出、要么转换成业务异常并记日志。静默吞掉的异常，会在生产环境变成无法定位的幽灵问题。','richtext',520,2,NULL,1,0.00,1,'2026-07-28 16:31:38',0,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(3,1,'第三章 系统设计与架构思维','<h2>3.1 架构的本质是权衡</h2><p>没有\"最好\"的架构，只有\"最合适\"的架构。单体还是微服务、强一致还是最终一致、同步还是异步，每个选择背后都是 trade-off。架构师的工作不是选\"最优解\"，而是在成本、人力、时间、复杂度之间找到当前阶段最合理的平衡点。</p><h2>3.2 分层与解耦</h2><p>经典三层架构（Controller-Service-Mapper）不是教条，而是关注点分离的实践。每一层只关心自己的职责：Controller 校验入参和组装响应，Service 编排业务，Mapper 持久化。跨层调用（比如 Controller 直接调 Mapper）是架构腐化的开始。</p><h2>3.3 面向接口编程</h2><p>依赖抽象，不依赖具体。Service 调用 Mapper 时依赖接口（IPortalBookMapper），而不是实现类。这样换实现（比如从 MySQL 换 ES）时，上层无需改动。这是开闭原则在工程中的落地。</p><h2>3.4 演进式架构</h2><p>不要一开始就设计\"完美架构\"。先做能跑的，再做能扩展的，最后才是能演进的。过早优化和过度设计，比不做设计更危险。</p>','## 3.1 架构的本质是权衡\n\n没有\"最好\"的架构，只有\"最合适\"的架构。单体还是微服务、强一致还是最终一致、同步还是异步，每个选择背后都是 trade-off。架构师的工作不是选\"最优解\"，而是在成本、人力、时间、复杂度之间找到当前阶段最合理的平衡点。\n\n## 3.2 分层与解耦\n\n经典三层架构（Controller-Service-Mapper）不是教条，而是关注点分离的实践。每一层只关心自己的职责：Controller 校验入参和组装响应，Service 编排业务，Mapper 持久化。跨层调用（比如 Controller 直接调 Mapper）是架构腐化的开始。\n\n## 3.3 面向接口编程\n\n依赖抽象，不依赖具体。Service 调用 Mapper 时依赖接口（IPortalBookMapper），而不是实现类。这样换实现（比如从 MySQL 换 ES）时，上层无需改动。这是开闭原则在工程中的落地。\n\n## 3.4 演进式架构\n\n不要一开始就设计\"完美架构\"。先做能跑的，再做能扩展的，最后才是能演进的。过早优化和过度设计，比不做设计更危险。','richtext',610,3,NULL,1,0.00,1,'2026-07-28 16:31:38',0,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(4,1,'第四章 数据库设计与优化','<h2>4.1 索引：查询的加速器</h2><p>索引不是越多越好。每个索引都有写入开销和维护成本。建立索引的三原则：查询频次高、区分度高、覆盖查询字段。区分度低于 30% 的列建索引基本无效（比如 status 只有 0/1 两个值）。</p><p>联合索引遵循最左前缀原则。索引 (user_id, status, create_time) 能命中 user_id 单独查询，但命中不了 status 单独查询。</p><h2>4.2 事务与隔离级别</h2><p>MySQL 默认隔离级别是 RR（可重复读），通过 MVCC + 间隙锁实现。但在高并发写场景下，RR 的间隙锁会导致锁等待，适当降到 RC（读已提交）能提升吞吐。注意 RC 会引入幻读，需业务层兜底。</p><h2>4.3 分页优化</h2><p>深分页 <code>LIMIT 1000000, 20</code> 极慢，因为要扫描 100 万行再丢弃。优化方案：用游标分页 <code>WHERE id &gt; #{lastId} LIMIT 20</code>，或用覆盖索引子查询。</p><pre><code>-- 慢\nSELECT * FROM article ORDER BY id LIMIT 1000000, 20;\n-- 快（游标分页）\nSELECT * FROM article WHERE id &gt; #{lastId} ORDER BY id LIMIT 20;</code></pre><h2>4.4 避免 N+1 查询</h2><p>循环里查数据库是性能杀手。批量查询 + 内存组装，比循环单查快几个数量级。</p>','## 4.1 索引：查询的加速器\n\n索引不是越多越好。每个索引都有写入开销和维护成本。建立索引的三原则：查询频次高、区分度高、覆盖查询字段。区分度低于 30% 的列建索引基本无效（比如 status 只有 0/1 两个值）。\n\n联合索引遵循最左前缀原则。索引 `(user_id, status, create_time)` 能命中 user_id 单独查询，但命中不了 status 单独查询。\n\n## 4.2 事务与隔离级别\n\nMySQL 默认隔离级别是 RR（可重复读），通过 MVCC + 间隙锁实现。但在高并发写场景下，RR 的间隙锁会导致锁等待，适当降到 RC（读已提交）能提升吞吐。注意 RC 会引入幻读，需业务层兜底。\n\n## 4.3 分页优化\n\n深分页 `LIMIT 1000000, 20` 极慢，因为要扫描 100 万行再丢弃。优化方案：用游标分页 `WHERE id > #{lastId} LIMIT 20`，或用覆盖索引子查询。\n\n```sql\n-- 慢\nSELECT * FROM article ORDER BY id LIMIT 1000000, 20;\n-- 快（游标分页）\nSELECT * FROM article WHERE id > #{lastId} ORDER BY id LIMIT 20;\n```\n\n## 4.4 避免 N+1 查询\n\n循环里查数据库是性能杀手。批量查询 + 内存组装，比循环单查快几个数量级。','richtext',750,4,NULL,1,0.00,1,'2026-07-28 16:31:38',0,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(5,1,'第五章 API 设计原则','<h2>5.1 RESTful 不是教条</h2><p>REST 风格的 API 用 HTTP 动词表达意图（GET 查、POST 增、PUT 改、DELETE 删），用 URL 表达资源。但不要为了 REST 而 REST——比如\"批量删除\"用 DELETE 传数组 body 就很别扭，此时 POST /batch-delete 更务实。</p><h2>5.2 版本控制</h2><p>API 一旦上线就有人依赖，改动是破坏性的。用 <code>/v1/articles</code> 而不是 <code>/articles</code>，给未来留演进空间。大版本用路径区分，小版本用 header 或 query。</p><h2>5.3 统一响应结构</h2><p>所有接口返回统一结构：<code>{code, msg, data}</code>。code=200 成功，其他失败。这样前端只需一套拦截器处理，不用每个接口判断不同格式。</p><pre><code>{\n  \"code\": 200,\n  \"msg\": \"success\",\n  \"data\": {...}\n}</code></pre><h2>5.4 幂等性</h2><p>POST 创建接口要考虑幂等：用户点两次\"提交\"按钮，不应该创建两条数据。方案：前端传 clientToken，后端用 Redis SETNX 去重，或用唯一索引兜底。</p>','## 5.1 RESTful 不是教条\n\nREST 风格的 API 用 HTTP 动词表达意图（GET 查、POST 增、PUT 改、DELETE 删），用 URL 表达资源。但不要为了 REST 而 REST——比如\"批量删除\"用 DELETE 传数组 body 就很别扭，此时 `POST /batch-delete` 更务实。\n\n## 5.2 版本控制\n\nAPI 一旦上线就有人依赖，改动是破坏性的。用 `/v1/articles` 而不是 `/articles`，给未来留演进空间。大版本用路径区分，小版本用 header 或 query。\n\n## 5.3 统一响应结构\n\n所有接口返回统一结构：`{code, msg, data}`。code=200 成功，其他失败。这样前端只需一套拦截器处理，不用每个接口判断不同格式。\n\n```json\n{\n  \"code\": 200,\n  \"msg\": \"success\",\n  \"data\": {...}\n}\n```\n\n## 5.4 幂等性\n\nPOST 创建接口要考虑幂等：用户点两次\"提交\"按钮，不应该创建两条数据。方案：前端传 clientToken，后端用 Redis SETNX 去重，或用唯一索引兜底。','richtext',560,5,NULL,1,0.00,1,'2026-07-28 16:31:38',0,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(6,1,'第六章 并发编程实战','<h2>6.1 并发问题的根源</h2><p>并发问题的本质是\"共享可变状态\"。多个线程同时读写同一份数据，没有同步就会出错。解决思路三条：不共享（ThreadLocal）、不修改（不可变对象）、加锁（同步）。</p><h2>6.2 锁的层级</h2><p>从轻到重：原子类（CAS）→ 读写锁（ReentrantReadWriteLock）→ 互斥锁（synchronized / ReentrantLock）。能用原子类就别用锁，能用读写锁就别用互斥锁。锁粒度越小，并发度越高。</p><h2>6.3 ConcurrentHashMap 的正确用法</h2><p>CHM 的 get/put 是线程安全的，但\"读-判断-写\"复合操作不是。</p><pre><code>// 错误：复合操作非原子\nif (!map.containsKey(key)) {\n    map.put(key, value);\n}\n// 正确：用原子方法\nmap.putIfAbsent(key, value);</code></pre><h2>6.4 线程池不要用 Executors 创建</h2><p>Executors.newFixedThreadPool 用的是无界队列，OOM 风险。用 ThreadPoolExecutor 显式指定队列容量和拒绝策略。</p><pre><code>new ThreadPoolExecutor(\n    8, 16, 60, TimeUnit.SECONDS,\n    new LinkedBlockingQueue<>(200),\n    new ThreadFactoryBuilder().setNameFormat(\"biz-pool-%d\").build(),\n    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：让调用方执行，形成背压\n);</code></pre>','## 6.1 并发问题的根源\n\n并发问题的本质是\"共享可变状态\"。多个线程同时读写同一份数据，没有同步就会出错。解决思路三条：不共享（ThreadLocal）、不修改（不可变对象）、加锁（同步）。\n\n## 6.2 锁的层级\n\n从轻到重：原子类（CAS）→ 读写锁（ReentrantReadWriteLock）→ 互斥锁（synchronized / ReentrantLock）。能用原子类就别用锁，能用读写锁就别用互斥锁。锁粒度越小，并发度越高。\n\n## 6.3 ConcurrentHashMap 的正确用法\n\nCHM 的 get/put 是线程安全的，但\"读-判断-写\"复合操作不是。\n\n```java\n// 错误：复合操作非原子\nif (!map.containsKey(key)) {\n    map.put(key, value);\n}\n// 正确：用原子方法\nmap.putIfAbsent(key, value);\n```\n\n## 6.4 线程池不要用 Executors 创建\n\nExecutors.newFixedThreadPool 用的是无界队列，OOM 风险。用 ThreadPoolExecutor 显式指定队列容量和拒绝策略。\n\n```java\nnew ThreadPoolExecutor(\n    8, 16, 60, TimeUnit.SECONDS,\n    new LinkedBlockingQueue<>(200),\n    new ThreadFactoryBuilder().setNameFormat(\"biz-pool-%d\").build(),\n    new ThreadPoolExecutor.CallerRunsPolicy()\n);\n```','richtext',820,6,NULL,1,0.00,1,'2026-07-28 16:31:38',0,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(7,1,'第七章 安全防护要点','<h2>7.1 SQL 注入</h2><p>永远用参数化查询，不要拼接 SQL。MyBatis 的 #{} 是参数化（安全），${} 是字符串拼接（危险）。${} 只能用于动态表名/列名等不能参数化的场景，且必须做白名单校验。</p><h2>7.2 XSS</h2><p>用户输入的内容渲染到 HTML 时必须转义。用白名单方式：只允许安全标签和属性，其余全部过滤。Markdown 渲染器输出后必须过 sanitize。</p><h2>7.3 越权（IDOR）</h2><p>所有写操作前校验资源归属：这篇文章的 authorId 是不是当前用户？这个订单的 buyerId 是不是当前用户？不校验就是越权漏洞，任意用户可改/删他人数据。</p><pre><code>// 错误：只校验登录，不校验归属\nLong userId = getUserId();\narticleService.update(article); // article.authorId 可能是别人的\n\n// 正确：校验归属\nif (!article.getAuthorId().equals(userId)) {\n    throw new RuntimeException(\"无权操作他人文章\");\n}</code></pre><h2>7.4 密码存储</h2><p>BCrypt 加盐哈希，永远不要明文存储。BCrypt 自带盐，且可调成本因子，是当前最推荐的方案。</p><h2>7.5 最小权限与 fail-fast</h2><p>安全配置缺失时应该拒绝启动，而不是回退到默认弱配置。密钥未配置就抛异常，而不是用硬编码备用密钥继续跑。</p>','## 7.1 SQL 注入\n\n永远用参数化查询，不要拼接 SQL。MyBatis 的 `#{}` 是参数化（安全），`${}` 是字符串拼接（危险）。`${}` 只能用于动态表名/列名等不能参数化的场景，且必须做白名单校验。\n\n## 7.2 XSS\n\n用户输入的内容渲染到 HTML 时必须转义。用白名单方式：只允许安全标签和属性，其余全部过滤。Markdown 渲染器输出后必须过 sanitize。\n\n## 7.3 越权（IDOR）\n\n所有写操作前校验资源归属：这篇文章的 authorId 是不是当前用户？这个订单的 buyerId 是不是当前用户？不校验就是越权漏洞，任意用户可改/删他人数据。\n\n```java\n// 错误：只校验登录，不校验归属\nLong userId = getUserId();\narticleService.update(article);\n\n// 正确：校验归属\nif (!article.getAuthorId().equals(userId)) {\n    throw new RuntimeException(\"无权操作他人文章\");\n}\n```\n\n## 7.4 密码存储\n\nBCrypt 加盐哈希，永远不要明文存储。BCrypt 自带盐，且可调成本因子，是当前最推荐的方案。\n\n## 7.5 最小权限与 fail-fast\n\n安全配置缺失时应该拒绝启动，而不是回退到默认弱配置。密钥未配置就抛异常，而不是用硬编码备用密钥继续跑。','richtext',780,7,NULL,1,0.00,1,'2026-07-28 16:31:38',0,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(8,1,'第八章 性能调优方法论','<h2>8.1 先测量，再优化</h2><p>\"过早优化是万恶之源\"——但更糟的是凭感觉优化。优化前先用 APM 或日志定位瓶颈：是 CPU、IO、数据库、还是网络？优化数据库索引和优化网络调用是两个完全不同方向，不测量就是瞎猜。</p><h2>8.2 缓存分层</h2><p>缓存不是银弹，引入缓存就引入了一致性问题。分层策略：浏览器缓存 → CDN → 本地缓存（Caffeine）→ 分布式缓存（Redis）→ 数据库。能短就不长，能近就不远。</p><p>缓存三大问题：穿透（查不存在的 key）、击穿（热 key 过期）、雪崩（大量 key 同时过期）。穿透用布隆过滤器，击穿用互斥锁，雪崩用随机过期时间。</p><h2>8.3 异步化</h2><p>耗时操作（发邮件、推送、写日志、统计）异步化，主流程快速返回。用消息队列削峰填谷。但异步意味着最终一致，要考虑消息丢失和重复消费。</p><h2>8.4 数据库优化优先级</h2><p>SQL 优化 &gt; 索引优化 &gt; 表结构优化 &gt; 分库分表。成本从低到高，收益从快到慢。不要一上来就分库分表，先把 SQL 和索引调好。</p>','## 8.1 先测量，再优化\n\n\"过早优化是万恶之源\"——但更糟的是凭感觉优化。优化前先用 APM 或日志定位瓶颈：是 CPU、IO、数据库、还是网络？优化数据库索引和优化网络调用是两个完全不同方向，不测量就是瞎猜。\n\n## 8.2 缓存分层\n\n缓存不是银弹，引入缓存就引入了一致性问题。分层策略：浏览器缓存 → CDN → 本地缓存（Caffeine）→ 分布式缓存（Redis）→ 数据库。能短就不长，能近就不远。\n\n缓存三大问题：穿透（查不存在的 key）、击穿（热 key 过期）、雪崩（大量 key 同时过期）。穿透用布隆过滤器，击穿用互斥锁，雪崩用随机过期时间。\n\n## 8.3 异步化\n\n耗时操作（发邮件、推送、写日志、统计）异步化，主流程快速返回。用消息队列削峰填谷。但异步意味着最终一致，要考虑消息丢失和重复消费。\n\n## 8.4 数据库优化优先级\n\nSQL 优化 > 索引优化 > 表结构优化 > 分库分表。成本从低到高，收益从快到慢。不要一上来就分库分表，先把 SQL 和索引调好。','richtext',690,8,NULL,1,0.00,1,'2026-07-28 16:31:38',0,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(9,1,'第九章 DevOps 与持续交付','<h2>9.1 自动化是工程化的前提</h2><p>手动部署 = 不稳定。构建、测试、部署、回滚全链路自动化，是团队规模超过 5 人后的必备能力。CI/CD 不是工具，是一种工程纪律。</p><h2>9.2 容器化的边界</h2><p>容器不是银弹。无状态服务适合容器化（API、Web），有状态服务谨慎（数据库、消息队列建议用托管服务而非自建容器）。容器的价值在于环境一致性和快速伸缩，不是\"用了就显得先进\"。</p><h2>9.3 监控与告警</h2><p>没有监控的系统等于盲飞。三层监控：基础设施（CPU/内存/磁盘）、应用（QPS/延迟/错误率）、业务（订单量/转化率）。告警要精准，噪声告警会让团队麻木，最终忽略真正的故障。</p><h2>9.4 灰度发布</h2><p>不要一次性全量发布。灰度策略：先小流量验证，再逐步放量。有问题快速回滚，而不是在线上 debug。回滚机制比发版机制更重要。</p>','## 9.1 自动化是工程化的前提\n\n手动部署 = 不稳定。构建、测试、部署、回滚全链路自动化，是团队规模超过 5 人后的必备能力。CI/CD 不是工具，是一种工程纪律。\n\n## 9.2 容器化的边界\n\n容器不是银弹。无状态服务适合容器化（API、Web），有状态服务谨慎（数据库、消息队列建议用托管服务而非自建容器）。容器的价值在于环境一致性和快速伸缩，不是\"用了就显得先进\"。\n\n## 9.3 监控与告警\n\n没有监控的系统等于盲飞。三层监控：基础设施（CPU/内存/磁盘）、应用（QPS/延迟/错误率）、业务（订单量/转化率）。告警要精准，噪声告警会让团队麻木，最终忽略真正的故障。\n\n## 9.4 灰度发布\n\n不要一次性全量发布。灰度策略：先小流量验证，再逐步放量。有问题快速回滚，而不是在线上 debug。回滚机制比发版机制更重要。','richtext',600,9,NULL,1,0.00,1,'2026-07-28 16:31:38',0,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(10,1,'第十章 技术领导力','<h2>10.1 技术领导力 ≠ 管理岗</h2><p>技术领导力不是\"带几个人\"，而是\"用技术影响团队的方向\"。一个资深工程师的价值，不只是自己能写多少代码，而是能让团队少踩多少坑、少走多少弯路。</p><h2>10.2 技术决策的责任</h2><p>选型决策要留痕——写技术方案文档（RFC），记录为什么选 A 不选 B、当时的前提假设是什么。半年后回头看，能复盘决策是否正确，而不是凭记忆争论\"当时为什么这么选\"。</p><h2>10.3 代码审查的价值</h2><p>Code Review 不是找茬，是知识传递。好的 CR 关注三点：逻辑是否正确、边界是否覆盖、可维护性是否及格。风格问题交给 lint 工具，CR 聚焦在人和机器都难发现的问题上。</p><h2>10.4 成长是长期主义</h2><p>技术的红利是复利的。今天多读的一篇源码、多写的一个测试、多复盘的一个事故，短期看不出差别，三年后是分水岭。保持学习，保持输出，保持对技术的好奇心——这是工程师能走多远的根本。</p>','## 10.1 技术领导力 ≠ 管理岗\n\n技术领导力不是\"带几个人\"，而是\"用技术影响团队的方向\"。一个资深工程师的价值，不只是自己能写多少代码，而是能让团队少踩多少坑、少走多少弯路。\n\n## 10.2 技术决策的责任\n\n选型决策要留痕——写技术方案文档（RFC），记录为什么选 A 不选 B、当时的前提假设是什么。半年后回头看，能复盘决策是否正确，而不是凭记忆争论\"当时为什么这么选\"。\n\n## 10.3 代码审查的价值\n\nCode Review 不是找茬，是知识传递。好的 CR 关注三点：逻辑是否正确、边界是否覆盖、可维护性是否及格。风格问题交给 lint 工具，CR 聚焦在人和机器都难发现的问题上。\n\n## 10.4 成长是长期主义\n\n技术的红利是复利的。今天多读的一篇源码、多写的一个测试、多复盘的一个事故，短期看不出差别，三年后是分水岭。保持学习，保持输出，保持对技术的好奇心——这是工程师能走多远的根本。','richtext',620,10,NULL,1,0.00,1,'2026-07-28 16:31:38',0,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(11,2,'第一章 北风起','<p>天宝十四年冬，朔风自雁门关外席卷而下，一夜之间，整个并州城便被裹进了漫天大雪之中。</p><p>城北校场，积雪已没过马蹄。一队玄甲骑兵正在风雪中操练，长槊如林，马蹄声碎。为首的将领勒马立于将台之下，抬眼望向灰沉沉的天际，眉头微蹙。</p><p>\"将军，风雪太大，弟兄们手都冻僵了，要不要暂歇？\"副将纵马上前，呵出一口白雾。</p><p>那将领没有答话，只缓缓摘下铁盔，露出一张刀削般的脸。他叫沈长歌，年方二十有六，却已是并州都督府最年轻的折冲都尉。八年前他从岭南一路北上，投军戍边，从一个无名小卒一刀一枪杀到了今日的位置。他的脸上有一道自左眉贯穿至右颊的旧疤，那是三年前与突厥决战时留下的。</p><p>\"再练半个时辰。\"沈长歌的声音不高，却在风雪中清晰可闻，\"北边的探马回报，突厥集结了三万骑兵，最迟十日内便会南下。届时你们的手要是连刀都握不住，就等着被砍脑袋吧。\"</p><p>副将脸色一变，不再多言，拨马回去督操。校场上槊影翻飞，雪沫四溅，马匹的嘶鸣声与铁甲的碰撞声交织在一起，在苍茫的天地间回荡。</p><p>沈长歌没有回帐，他翻身下马，独自走到校场边缘的土丘上，负手而立。从这里望出去，并州城的轮廓在雪幕中若隐若现——那是北疆最坚固的壁垒，也是大周抵御突厥的第一道防线。城外是连绵数十里的军屯村落，再往北，过了云州，便是茫茫草原。</p><p>他想起八年前自己第一次站在雁门关城头时的情景。那时的他还只是个十六岁的少年，怀里揣着母亲临终前留给他的半块玉佩，孤身一人从岭南跋涉千里来到北疆。他记得那天城头上也有这样大的雪，守关的老将军指着关外白茫茫的一片，对他说：\"小子，看见了吗？那边就是草原。草原能养出最好的马，也能养出最狠的狼。你要是怕了，现在回头还来得及。\"</p><p>他没有回头。他在雁门关戍守了三年，从一个连弓都拉不开的瘦弱少年，成长成了关内令突厥人闻风丧胆的\"沈阎王\"。后来他被调到并州，又在这里驻守了五年。八年了，他再没回过岭南，也再没见过任何人从家乡来。</p><p>他下意识地摸了摸胸口那半块玉佩。玉佩温润，是他身上唯一带点南方气息的东西。母亲临终前说，另一半在一个人手里，那人与他命脉相连，来日自会相见。这些年他四处征战，也见过形形色色的人，却从未见过谁佩着另一半相似的玉。</p><p>\"将军！\"亲兵的呼喊打断了他的思绪，\"都督府来人了，让您即刻过去议事。\"</p><p>沈长歌收回思绪，大步走下土丘。风雪更大了，他的铁甲上很快积了一层白，但他浑然不觉。八年北疆，他早已习惯了这刺骨的寒冷。</p><p>都督府大堂内，炭火烧得正旺。并州都督赵崇节正对着一张舆图眉头紧锁，见他进来，直起身子，沉声道：\"长歌，出事了。\"</p><p>\"突厥提前南下了？\"沈长歌问。</p><p>\"不是突厥。\"赵崇节摇了摇头，压低声音，\"长安来的密报——东宫出事了。\"</p><p>沈长歌一怔。他对朝堂之事向来不闻不问，只管戍边杀敌。但\"东宫出事\"四个字，分量非同小可。太子若有个三长两短，这天下怕是要乱。</p><p>\"三日前，太子在东宫暴毙，陛下震怒，已下旨彻查。朝中传言纷纷，有说是病故，有说是中毒，还有说是……\"赵崇节顿了顿，\"还有人说是二皇子动的手。\"</p><p>沈长歌沉默良久。他虽不涉朝政，但也知道二皇子李恪一直觊觎东宫之位。若太子真是他所害，那接下来势必是一场腥风血雨。</p><p>\"都督的意思是？\"</p><p>\"陛下密旨，命各地都督进京述职，名为述职，实为稳固军心。我已决定亲率三千亲卫入京，并州的防务，就交给你了。\"赵崇节目光炯炯地望着他，\"长歌，我知道你不愿卷进这些事里，但有些事，躲是躲不掉的。你是沈家人，当年你父亲的事……\"</p><p>\"都督。\"沈长歌突然出声打断了他，语气平淡却坚决，\"我只管并州的防务。朝堂上的事，我管不了，也不想管。\"</p><p>赵崇节叹了口气，没再多说。他知道这年轻人的心结——其父沈牧之，当年曾是朝中名将，却在一桩谋反案中被牵连，满门抄斩，唯有这个幼子被旧部拼死送出，辗转流落到了北疆。这些年沈长歌从不在人前提起家世，军中也少有人知他真实身份。</p><p>\"那并州就拜托你了。\"赵崇节拍了拍他的肩，\"突厥若来，能守则守，不能守就退守雁门关，保住人马要紧。至于长安那边……你自己看着办。\"</p><p>沈长歌抱拳领命，转身走出大堂。风雪扑面而来，他站在廊下，仰头看了看天，脸色比雪还要冷。</p><p>长安，他已经八年没去过了。那个让他失去一切的地方，他原本以为这辈子都不会再踏入半步。</p><p>可如今，命运似乎并不打算放过他。</p><p>回到校场，操练已毕。将士们正在收整兵器，见他回来，纷纷行礼。沈长歌径直走入自己的营帐，解下铁甲，坐在案前。帐外风雪呼啸，帐内炭火温暖。他倒了杯热酒，却没喝，只是盯着杯中晃动的酒影出神。</p><p>这时，帐帘一掀，副将走了进来，神色古怪：\"将军，校场外抓到一个人，说是从长安来的，指名要见您。\"</p><p>\"长安来的？\"沈长歌眉头一挑，\"什么人？\"</p><p>\"是个女子。\"副将咽了口唾沫，\"穿着一身月白僧袍，骑着一匹黑马，腰间佩着一把长剑。她说她姓苏，叫苏映雪，是您的故人。\"</p><p>沈长歌握着酒杯的手猛地一紧，杯中酒液溅出几滴，落在案上。他缓缓抬起头，眼中闪过一丝连他自己都未曾察觉的波动。</p><p>苏映雪。</p><p>这个名字，他已经八年没听人提起了。他以为这个名字会随着记忆一起，被北疆的风雪慢慢掩埋，直到他再也想不起来。</p><p>可她来了。</p><p>\"让她进来。\"他放下酒杯，声音恢复了惯常的平静。</p><p>帐帘再次掀起，一股冷风卷着雪花扑入，随即被一抹月白的身影挡在了外面。来人摘下风帽，露出一张清丽绝俗的脸——眉如远山，目若寒星，唇色极淡，整个人清冷得像是一枝雪中寒梅。她看起来不过二十出头，眉宇间却有一股远超同龄人的沉静与决断。</p><p>她站在帐口，与沈长歌隔着三丈的距离对视。帐内炭火噼啪作响，谁都没有先开口。</p><p>良久，还是苏映雪先开了口。她的声音清冷，像是雪落在竹叶上的声响：\"沈长歌，我找了你三年。\"</p><p>\"找我做什么？\"沈长歌面无表情。</p><p>\"来救你的命。\"她从袖中取出一封信，抬手一扬，信纸轻飘飘地飞向沈长歌，被他一把抄住。他展开一看，脸色骤变。</p><p>信上只有寥寥数语，却字字惊心：沈牧之案真凶另有其人，当年主审官已死，临终前留下密函一封，藏于云栖寺。二皇子李恪已得知此事，正派人四处追查沈家后人，斩草除根。</p><p>\"你怎么会有这个？\"沈长歌抬眼，目光锐利如刀。</p><p>\"我师父是云栖寺住持的至交。\"苏映雪淡淡道，\"密函是我师父临终前交给我的，他让我来找你。他说，你父亲当年是被冤枉的，而能洗清冤屈的证据，就在那封密函里。\"</p><p>\"令师已经……\"</p><p>\"去年冬天，圆寂了。\"她的语气很平淡，仿佛说的是别人的事，但沈长歌注意到，她搁在剑柄上的手指微微发白。</p><p>帐内再次沉默。风雪打在帐帘上，发出沉闷的声响。</p><p>\"你来找我，不只是为了送信。\"沈长歌将信纸折好，收入怀中，重新望向她，\"你是想让我跟你去云栖寺取那封密函。\"</p><p>\"是。\"苏映雪点头，\"但云栖寺在终南山深处，去那一趟，少说也要半个月。而这半个月里，二皇子的人随时可能找到你。沈长歌，你若还想为父报仇，就不能再待在并州了。\"</p><p>沈长歌没有立刻回答。他站起身，走到帐口，掀开帐帘一角望出去——校场上积雪皑皑，将士们来来往往，准备着夜间的戒备。这是他守了五年的地方，每一个士兵他都认得，每一寸土地都浸过他和弟兄们的血汗。</p><p>\"突厥十日内就会南下。\"他低声道，\"我现在走不了。\"</p><p>\"你留在这里，突厥杀你；你若去长安，二皇子杀你。\"苏映雪的声音依旧平静，却字字诛心，\"横竖都是一死，至少去云栖寺，你还有一线生机为父昭雪。沈长歌，你想清楚了。\"</p><p>沈长歌缓缓放下帐帘，转过身来。火光映着他的脸，那道贯穿眉骨的旧疤在明暗之间显得格外狰狞。</p><p>\"我可以把密函的事交给别人。\"他说，\"但突厥南下，我必须挡在并州。这是父亲教我的最后一句话——食君之禄，忠君之事。哪怕这君要杀我全家，只要外敌还在一天，我就得守在这里一天。\"</p><p>苏映雪静静地看了他半晌，忽然极轻地笑了一下。那是她进帐后第一次笑，很淡，却让那张清冷的脸多了几分暖意。</p><p>\"你还是老样子。\"她说，\"八年前在岭南，你说要走，我拦不住你；如今我说要带你走，你还是不肯。沈长歌，你这辈子就只会认死理。\"</p><p>\"人总得认一样东西。\"他重新坐下，端起那杯酒，一饮而尽，\"你先在这里住下，等突厥的仗打完，我陪你走一趟终南山。\"</p><p>苏映雪没有拒绝。她在帐中炭盆边坐下，将剑横放在膝上，闭目养神，不再言语。</p><p>这一夜风雪未停。沈长歌坐在案前，反复看着那封信，直到烛火燃尽，天边泛起一线鱼肚白。</p><p>他知道，从他打开那封信的一刻起，他的人生便再也回不了头了。</p><p>北风卷着雪粒打在帐上，发出沙沙的声响，像是什么人在夜色中急急赶路，又像是一支大军正踏雪而来。</p>',NULL,'richtext',7350,1,NULL,0,1.00,1,'2026-07-28 16:39:00',0,'admin','2026-07-28 16:39:00','','2026-07-28 16:39:00',NULL,'0'),(12,2,'第二章 长安夜','<p>十日后，突厥的三万铁骑如期南下，却在云州城外遭到了沈长歌的迎头痛击。</p><p>这一战打了七天七夜。沈长歌亲率三千玄甲骑兵，趁夜突袭突厥粮草大营，一把火烧了敌军半数的补给。突厥被迫后退三十里，暂时退入草原休整。沈长歌知道这只是暂时的喘息，突厥必然会卷土重来，但他需要的这点时间已经足够了。</p><p>战事稍歇的第三天，一匹快马从长安奔至并州，带来了赵崇节的亲笔信。信上说，朝中局势已到了危急存亡之秋——二皇子李恪在朝中大肆排斥异己，先后构陷了三位重臣下狱，其中就包括当年主审沈牧之案的大理寺卿。如今李恪已基本掌控了朝堂，距离逼宫夺位只剩一步之遥。</p><p>信的最后，赵崇节只写了一句话：\"长歌，若你父亲真的冤枉，这天下便不该由李恪来坐。你自己掂量。\"</p><p>沈长歌看完信，在帐中枯坐了整整一个时辰。苏映雪一直坐在角落，没有打扰他，直到他自己站起身来。</p><p>\"走。\"他把信纸投入炭盆，看着它化为灰烬，\"我去长安。\"</p><p>苏映雪挑了挑眉：\"并州不要了？\"</p><p>\"赵都督已率亲卫回京，并州现在由监军暂管，我留不留下都一样。\"他顿了顿，\"况且突厥刚退，短期内不会再来。这一仗，我打赢了我的部分。\"</p><p>当天傍晚，沈长歌将防务交割给监军，只带了二十名亲卫，与苏映雪一同快马南下。他们没有走官道，而是穿过太行山的小径，避开了一切可能的眼线。七天后，他们抵达了长安城外。</p><p>长安，还是八年前的长安。城墙巍峨如旧，朱雀大街宽阔如旧，连城门上那块\"长安\"二字的匾额都还是记忆中的模样。只是街上往来的行人神色匆匆，巡逻的武侯卫比从前多了一倍，整座城都笼罩在一种压抑而紧绷的气氛中。</p><p>沈长歌换了一身寻常布衣，戴上一顶斗笠遮住脸，随着人流入了城。苏映雪仍是一身月白僧袍，只在风帽外罩了一层黑纱，遮住了大半张脸。</p><p>他们没有去客栈投宿，而是七拐八绕地来到了城东崇仁坊的一处旧宅。那宅子门面不起眼，门口的石阶上生了青苔，看起来已久无人住。沈长歌上前敲了三长两短五下，半晌，门吱呀一声开了条缝，露出一只浑浊的老眼。</p><p>\"沈家的人？\"一个苍老的声音问。</p><p>\"云州来的。\"沈长歌答。</p><p>门完全打开，一个佝偻的老仆将他们迎了进去。院中荒草丛生，唯有正屋打扫得干干净净。老仆反手关上门，对沈长歌颤声道：\"小公子，您……您终于回来了。老奴等了八年。\"</p><p>\"福伯。\"沈长歌声音微哑，\"这些年，辛苦你了。\"</p><p>这老仆名叫福伯，是沈牧之当年的老管家。沈家出事后，他靠着以前攒下的积蓄，买下了这处宅子，改了名姓，一直暗中守着沈家旧部的联络点，等着沈家后人有朝一日回来。八年来，他一个外人都不敢接触，只靠每月固定的暗号与赵崇节单线联络。</p><p>\"二少爷，您来得正是时候。\"福伯将他们引入正屋，从暗格里取出一叠文书，\"这些是这些年搜集的情报。二皇子李恪三个月前就开始在城内大肆搜捕，尤其留意任何与沈家有关的人。您若再晚来半月，老奴怕是要顶不住了。\"</p><p>沈长歌翻看着那叠文书，脸色越来越沉。其中一张纸上画着一幅人物画像，虽是简笔，却与苏映雪有七八分相似。画像下注着一行小字：\"苏氏女，映雪，云栖寺传人，通缉。\"</p><p>\"你也被通缉了？\"沈长歌抬眼看向苏映雪。</p><p>\"我师父是李恪杀的。\"她语气依旧平淡，\"三年前我师父察觉到密函之事，派人通知了几个旧交，消息走漏，李恪的人当夜就围了云栖寺。师父让我带着密函先走，他自己留下来断后。第二天，云栖寺被烧成白地。\"</p><p>她说到这里，手指在剑鞘上轻轻叩了叩，像是在压抑什么。沈长歌没有追问，只是把那张画像放到烛火上烧了。</p><p>\"李恪已经知道密函的事了？\"他问。</p><p>\"知道密函，但不知道在哪。\"福伯接话，\"二皇子的人这几个月把终南山翻了个底朝天，也没找到云栖寺藏密函的地方。老奴估摸着，那东西应该还在寺里——只是藏得极隐秘，非得有线索才能找到。\"</p><p>\"什么线索？\"苏映雪问。</p><p>福伯犹豫了一下，从怀中摸出一块绢帕，上面用极淡的墨迹写着几行字：\"雪落寒梅处，钟鸣古松前。石上青苔老，心中有故人。\"他念道，\"这是老侯爷当年留下的。老奴琢磨了八年，也没参透。\"</p><p>沈长歌接过绢帕，反复看了几遍，沉吟道：\"这是云栖寺的方位。寒梅、古松、青苔石——云栖寺后山有片梅林，林中有口古井，井边有棵老松，松下有块青苔石。父亲的意思，密函就藏在那块青苔石下。\"</p><p>\"你怎么知道？\"苏映雪眼眸一亮。</p><p>\"我父亲年轻时在云栖寺读过书。\"沈长歌将绢帕收好，\"那片梅林是他常去的地方。小时候他跟我讲过那里的一草一木，原以为只是闲话，没想到是在给我留路。\"</p><p>\"那我们今夜就走？\"苏映雪站起身。</p><p>\"不急。\"沈长歌按住她的肩，\"李恪的人在终南山搜了三个月都没找到，说明他们根本不知道方位。我们比他们有优势，可以从容一些。但进长安这一趟，我不能白来——有些事，我得先办了。\"</p><p>他看向福伯：\"赵都督现在何处？\"</p><p>\"赵大人回京后一直被软禁在府中，名义上是养病，实则寸步难行。不过他留了条暗线，每隔三日会有消息传出。明日正好是传消息的日子，您若要见他的人，得赶在午时之前去西市的茶楼。\"</p><p>沈长歌点头。他知道赵崇节虽被软禁，但这位老上司的手段远不是表面看起来的那么简单。当年沈家出事，正是赵崇节在朝中斡旋，才保下了沈长歌的性命，把他送到北疆。这份恩情，他一直记在心里。</p><p>次日清晨，沈长歌独自前往西市。长安的西市依旧繁华，酒肆茶楼林立，叫卖声此起彼伏。但他注意到，街角多了许多生面孔，这些人不带刀，却都穿着劲装，腰间鼓鼓囊囊，显然藏着短兵。</p><p>他进了一家名为\"得月楼\"的茶馆，在二楼靠窗的角落坐下，要了一壶碧螺春。茶还没上来，一个身材瘦小的中年文士便径直走过来，在他对面坐下。</p><p>\"云州来的客人？\"文士问，声音很低。</p><p>\"正是。贵东家可好？\"</p><p>\"东家身子骨还成，就是心里不痛快。\"文士压低声音，目光四下游移，\"东家让小人转告客人三件事。其一，二皇子李恪已与禁军统领达成默契，最迟下月初便会逼宫；其二，陛下虽病重，但神志尚清，已秘密召五位老臣入宫，欲废李恪；其三——\"</p><p>文士的声音突然更低了，几乎贴到沈长歌耳边：\"东家说，这第三件事最为紧要。当年的案子，关键不在密函，而在一个人。那人如今还活着，就在宫里。\"</p><p>沈长歌瞳孔一缩：\"谁？\"</p><p>\"淑妃。\"文士吐出两个字，\"当年的淑妃，如今的太后。\"</p><p>沈长歌的手猛地握紧了茶杯。太后？那个深居后宫、二十年不曾露面的女人？他与父亲一别时只有八岁，关于父亲的记忆早已模糊，但他依稀记得，父亲生前曾提起过一个姓氏，语气中既有敬重又有惋惜——那是苏姓。而苏映雪的师父，也姓苏。</p><p>这一切之间，究竟有什么关联？</p><p>文士说完便起身告辞，临走前留下一个包袱，说是赵崇节给他准备的入宫腰牌和银两。沈长歌将包袱收好，喝完那壶茶，才慢慢起身下楼。</p><p>走到茶楼门口时，他忽然停下脚步。街对面，一个穿着青衫的年轻人正倚着柱子看他，嘴角噙着一丝若有若无的笑。那人大约二十五六岁，眉目清秀，腰间挂着一柄镶玉的短剑，通身上下一股世家子弟的贵气。</p><p>两人目光相触的一瞬，那年轻人朝他举了举手中的折扇，算是打了个招呼，随即转身没入人群之中。</p><p>沈长歌的心猛地一沉。那折扇上绣着一枝寒梅——那是李恪的标记。</p><p>他已经被盯上了。</p>',NULL,'richtext',7280,2,NULL,0,1.00,1,'2026-07-28 16:39:00',0,'admin','2026-07-28 16:39:00','','2026-07-28 16:39:00',NULL,'0'),(13,2,'第三章 终南山','<p>沈长歌回到崇仁坊的旧宅时，天色已近黄昏。</p><p>他将茶楼见闻一一道来，苏映雪听完，脸色比平日更冷了几分：\"李恪的人已经认出你了？\"</p><p>\"还不确定。\"沈长歌坐在桌前，指尖轻叩着桌面，\"但那个青衫人绝非常人，他敢在大白天盯梢，说明李恪在长安的眼线比我们想象的更多。我们不能再等了，今夜就去终南山。\"</p><p>福伯在一旁听了，急道：\"二少爷，终南山离长安城少说也有百里，夜间山路难行，况且李恪的人必定在各处要道设了卡子……\"</p><p>\"所以要白天走，扮成香客。\"苏映雪开口，\"终南山有数座古刹，香客往来不断，我们混在其中，不会引人注目。明日一早出发，走子午谷的小道，入夜前应能到云栖寺。\"</p><p>沈长歌思忖片刻，点头应允。当夜无话。</p><p>次日天刚蒙蒙亮，三人便动身。福伯留下守宅，只沈长歌与苏映雪两人各骑一马，出了长安南门。他们没有走官道，而是沿着一条溪流折向西南，穿过一片杨树林后，便拐进了一条羊肠小径。</p><p>子午谷是终南山七十二峪中最险的一条，两侧峭壁如削，谷底溪水奔涌，只在最窄处架着一座木桥。平日里除了采药人和猎户，少有人走。但今日不知为何，谷口竟停着两辆青布马车，车旁站着几名带刀的仆从。</p><p>沈长歌与苏映雪对视一眼，彼此都看见了对方眼中的警惕。他们下马步行，装作路过的样子，从马车旁经过时，沈长歌余光扫过车帘缝隙，隐约看见车内坐着一位身着绛紫官袍的老者，正闭目养神。</p><p>\"那人是礼部尚书韩琦。\"走过之后，苏映雪低声道，\"我师父生前与他在终南山有过一面之缘。此人是个清流，但与朝中各派都保持着距离，今日出现在子午谷，绝非寻常。\"</p><p>沈长歌没有接话，只是加快了脚步。子午谷的路越走越窄，到后来几乎无路可走，只能攀着藤蔓石缝向上爬。苏映雪虽是女子，身手却比寻常男子还要矫健，攀岩附壁如履平地。沈长歌看在眼里，心中暗暗称奇——这八年里她究竟经历了什么，才能从一个柔弱的小姑娘变成如今这副模样？</p><p>午时过后，他们终于走出了子午谷，眼前豁然开朗。终南山的群峰在云雾中若隐若现，山腰处点缀着几座红墙古刹，钟磬声隐隐传来。云栖寺就在最高峰的北侧，远远望去，只露出半截黄墙黑瓦，被一片苍松翠柏掩映着。</p><p>\"那就是云栖寺。\"苏映雪指着远处，声音中带着一丝不易察觉的颤抖，\"三年前它还在，如今……\"</p><p>话音未落，她的脸色变了。沈长歌也看见了——云栖寺的方向，正升起一缕黑烟。</p><p>两人对视一眼，不约而同地纵身飞掠，向山上奔去。沈长歌的轻功在军中算不上顶尖，但这八年的戍边生涯，练就了他过人的耐力和爆发力。苏映雪的步法却轻灵飘逸，像是一只雪白的鹤，在山石间起落自如。</p><p>他们用了不到一炷香的时间便赶到了云栖寺山门前。眼前的景象让两人都愣住了——寺门紧闭，门楣上的匾额还挂着，但周围的地面却是一片焦黑，显然经历过一场大火。只是火势被山风吹散，没有烧尽整座寺院，正殿和后院还算完好，只是前院的几间厢房化作了废墟。</p><p>\"三年了。\"苏映雪缓步走入山门，目光扫过满地的焦木碎瓦，声音低得几乎听不见，\"我以为这里早就成了平地。\"</p><p>沈长歌没有说话，只是跟在她身后，警惕地四下张望。寺内空无一人，连鸟雀的鸣声都没有，安静得有些诡异。正殿的佛像蒙着厚厚的灰，香案上积了三寸厚的尘埃，显然许久没人来过。</p><p>\"去后山。\"沈长歌低声道。</p><p>两人穿过正殿，沿着一条石径向后山走去。石径两旁长满了荒草，有些地方已经被藤蔓完全覆盖，看来三年间确实再无人踏足。走了约半刻钟，前方豁然开朗——一片梅林出现在眼前。</p><p>此时正值隆冬，梅林中数百株寒梅竞相怒放，红白相间，香气袭人。梅林深处，一棵苍劲的老松拔地而起，松下有一口古井，井边横卧着一块生满青苔的巨石。这景象，与绢帕上所写的\"雪落寒梅处，钟鸣古松前。石上青苔老，心中有故人\"一一对应。</p><p>\"就是这里。\"沈长歌走到青苔石前，蹲下身察看。石头虽大，却有一道明显的裂缝，似乎曾被移动过。他运足了力气，双手扣住石缝，低喝一声，将石头缓缓推开。</p><p>石下露出一个浅坑，坑中放着一个油纸包裹的小木匣。沈长歌取出木匣，揭开油纸，里面是一封泛黄的信纸——正是父亲沈牧之当年的遗笔。</p><p>\"找到了。\"他的声音微微发颤，这是八年来他第一次流露出如此强烈的情绪。苏映雪站在一旁，默默地看着他，没有出声。</p><p>沈长歌展开信纸，借着落日的余晖细读。信上写的内容，让他越看越惊，越看越怒，到最后，握信的手都在剧烈颤抖。</p><p>信上说：当年沈牧之任兵部侍郎时，曾奉旨调查一桩军饷贪墨案，牵涉到当时的淑妃、如今的太后。淑妃的母家靠着克扣军饷中饱私囊，数额巨大。沈牧之掌握了确凿证据，正欲上报陛下，却被淑妃先发制人，构陷他谋反，将其满门抄斩。而当时主审此案的大理寺卿，其实也是被淑妃胁迫，临终前才良心发现，留下了这封密函，托付给云栖寺的方丈保管。</p><p>信的最后，沈牧之写道：\"吾儿若能见此信，当知为父之冤。然切不可凭一时之愤贸然复仇，此案牵涉后宫，非一人之力可平。须待时机，须寻盟友，须有铁证方可发难。为父留此一线，望吾儿能沉潜待时，终有一日为沈家昭雪。\"</p><p>沈长歌读完，将信纸折好，收入怀中。他闭了闭眼，再睁开时，眼中已是清明一片。八年来他一直以为父亲是被人陷害，却不知道幕后真凶竟是太后。而如今，这个真相不但不能帮他报仇，反而让他陷入了一个更危险的境地——太后是当今陛下的生母，李恪的后台，也是这天下权势最盛的女人。</p><p>\"怎么样？\"苏映雪见他收起信，才开口。</p><p>\"比我预想的还要复杂。\"沈长歌站起身，扫了一眼梅林四周，\"这封信不能让任何人知道，更不能落入李恪之手。我们先回长安，见赵都督，再作打算。\"</p><p>苏映雪点头，转身欲走，却突然顿住脚步，按住剑柄，冷声道：\"出来。\"</p><p>梅林深处，一阵簌簌的响动之后，一个身影缓缓走出。沈长歌看清来人，瞳孔骤然一缩——正是白天在茶楼外那个青衫年轻人。他手中折扇轻摇，脸上仍挂着那丝似笑非笑的神情，仿佛不是来追杀他们的，倒像是来赴一场旧约。</p><p>\"沈兄，别来无恙。\"青衫人朝沈长歌拱了拱手，\"在下李恪，久仰大名。\"</p><p>二皇子。</p><p>沈长歌与苏映雪同时握紧了兵器。李恪却不在意，自顾自地走上前来，在距他们三丈处站定：\"沈兄不必紧张，我今日来，不是为杀人，是为说话。\"</p><p>\"二殿下与在下，有什么话可说？\"沈长歌沉声道。</p><p>\"有的。\"李恪收起折扇，脸上的笑意也敛去了，神色变得认真起来，\"沈兄可知，你怀中那封信，是当年你父亲临终前留给你的最后一道护身符？\"</p><p>沈长歌一怔：\"什么意思？\"</p><p>\"那封信上的内容，除了你父亲之外，天下还有一个人知道。\"李恪的目光直直地盯着他，\"那就是我。\"</p><p>沈长歌与苏映雪都愣住了。李恪看着他们的反应，忽然笑了，那笑容中带着一丝说不清道不明的苦涩：\"沈兄，看来你不知道你父亲与我的关系。你父亲沈牧之，是我外公的学生，也是我母妃生前最信任的人。\"</p><p>母妃。沈长歌心中一震。李恪的母妃，正是当今陛下已故的元配皇后，也就是现在的太后当年争斗的对手。如果李恪的母亲与沈牧之有旧，那……</p><p>\"你想说什么？\"沈长歌沉住气。</p><p>\"我想说，\"李恪缓步上前，目光灼灼，\"我们，或许不是敌人。\"</p>',NULL,'richtext',7420,3,NULL,0,1.00,1,'2026-07-28 16:39:00',0,'admin','2026-07-28 16:39:00','','2026-07-28 16:39:00',NULL,'0'),(14,2,'第四章 宫闱谋','<p>梅林中，三人围着一株老梅树坐下。沈长歌与苏映雪并肩，李恪独自坐在对面，三人之间隔着一段不远不近的距离，像是谈判，又像是试探。</p><p>李恪从袖中取出一卷文书，递给沈长歌：\"这是这些年我暗中查到的太后罪证。比你们手上那封密函，要详尽十倍。\"</p><p>沈长歌接过一看，脸色渐渐凝重。那卷文书上记载着太后母家这些年来的种种恶行——克扣军饷、卖官鬻爵、构陷忠良、私通外敌，桩桩件件都有据可查，最后还附着一长串被太后陷害的官员名单，其中沈牧之的名字赫然在列。</p><p>\"你既然有这些罪证，为何不直接呈给陛下？\"沈长歌抬眼。</p><p>\"呈给陛下？\"李恪冷笑一声，\"陛下如今病入膏肓，神志时清时昏。太后把持后宫，控制着陛下身边所有的宫人，连御医都是她的人。我若贸然呈上罪证，不出半日，那卷文书便会变成我的催命符。\"</p><p>\"所以你才四处搜捕，想找到我父亲的密函？\"苏映雪开口，\"你是想用密函来补全你的证据链？\"</p><p>\"正是。\"李恪坦然点头，\"我手上的罪证虽然详尽，却独缺最关键的一环——当年沈牧之案的主审官亲笔证词。那封密函，正是我多年来寻找的东西。沈兄，你怀里的信，是我扳倒太后的最后一块拼图。\"</p><p>沈长歌沉默良久。他当然明白，李恪要扳倒太后，并非出于什么正义之心，而是为了自己能顺利登上帝位。太后是支持其他皇子的，只要太后在位一日，李恪便无法如愿。但无论如何，敌人的敌人便是盟友，至少在这一刻，他与李恪有着共同的敌人。</p><p>\"你想让我把密函给你？\"沈长歌问。</p><p>\"不。\"李恪摇头，\"密函在你手上更安全。我要的不是密函，是你的配合。\"</p><p>\"什么配合？\"</p><p>\"入宫。\"李恪一字一顿，\"我要你随我入宫，面见陛下。\"</p><p>沈长歌与苏映雪同时一愣。入宫？那个戒备森严、太后一手遮天的地方？</p><p>\"陛下虽病重，但神志清醒时仍能下旨。\"李恪解释道，\"我已联络了禁军中几位效忠于我的将领，他们能在关键时刻控制住宫门。但要让陛下下旨废太后、定其罪，必须有人能在陛下面前陈述证据，且这个人的身份必须足够特殊——他得是当年沈牧之案的受害者后人。\"</p><p>\"你要让一个通缉犯入宫面圣？\"苏映雪冷笑，\"二殿下的算盘打得未免太响。\"</p><p>\"苏姑娘不必激我。\"李恪看向她，目光中竟带着几分真诚，\"我知道你们不信我。但你们也没有别的选择。太后已知道密函之事，她的人正在赶往终南山的路上，最迟明日夜里便会抵达云栖寺。你们若是拿着密函独自回长安，不出城门便会被人截杀。只有跟着我，才有一线生机。\"</p><p>沈长歌垂眸沉思。李恪说的没错，他们现在的处境已经是四面楚歌。太后要杀他灭口，李恪要借他扳倒太后，而他自己的复仇之路，似乎也只能依附在这盘棋局之上。</p><p>\"好。\"他终于开口，\"我随你入宫。但我有三个条件。\"</p><p>\"沈兄请讲。\"</p><p>\"其一，入宫之后，我父亲的冤案必须由陛下亲自下旨昭雪，沈家满门的清白必须公诸天下。\"</p><p>\"自然。\"</p><p>\"其二，扳倒太后之后，不得牵连无辜。当年被太后胁迫的那些官员，能保则保。\"</p><p>\"这一点……\"李恪沉吟，\"我会尽力。\"</p><p>\"其三，\"沈长歌抬眼直视李恪，目光锐利，\"事成之后，你须放我与苏姑娘离开长安，从此不再相扰。我无意于朝堂，你做了皇帝之后，也不必再记挂我。\"</p><p>李恪怔了怔，随即大笑：\"沈兄果然是沈兄，连做交易都这么干脆。好，我答应你。\"</p><p>当夜，三人便在云栖寺的废墟中过了一夜。沈长歌与苏映雪轮流守夜，李恪则早早歇下，似乎对他们的戒心毫不在意。</p><p>次日清晨，三人动身下山。李恪没有带随从，只身一人，仿佛将身家性命都押在了沈长歌身上。但沈长歌知道，这份\"信任\"背后，必定还有无数他看不见的布局。一个谋划了多年的皇子，绝不会只带着两个外人就去闯宫——他必然已经在长安城内做好了万全的准备。</p><p>果然，三人回到长安后，李恪带他们去了一处隐蔽的宅院。宅中已聚集了二三十人，有文有武，都是李恪的心腹。其中一位须发皆白的老者，竟是当年沈牧之的旧部、现任太医院院正的孙老先生。</p><p>\"孙伯。\"沈长歌认出故人，眼眶微热。</p><p>\"小公子。\"孙老先生握住他的手，老泪纵横，\"老奴等了八年，终于等到今日。\"</p><p>原来孙老先生这些年一直潜伏在宫中，借着太医院院正的身份，暗中收集太后一党的罪证，并与李恪保持着秘密联络。他已查清太后近日要对陛下下毒手，打算在陛下驾崩后伪造遗诏，扶植傀儡皇子登基。若不抢在此之前动手，一切便都晚了。</p><p>\"太后打算什么时候动手？\"沈长歌问。</p><p>\"三日后。\"孙老先生道，\"后天是太后的寿辰，宫中会大办宴席。太后打算在宴席上给陛下下毒，制造暴毙的假象。我们必须在宴席当晚动手。\"</p><p>三日。沈长歌深吸一口气。三日之内，他要做的不仅是随李恪入宫面圣，还要确保在太后动手之前，将所有证据呈到陛下面前。</p><p>当夜，李恪召集众人商议了入宫的详细计划。他们决定兵分两路：李恪率一部分人控制禁军，截断太后的外援；沈长歌随孙老先生从太医院的密道入宫，直奔陛下寝殿，当面陈述冤情与罪证。苏映雪则负责在外面策应，一旦宫中有变，立刻放出信号，通知城外的李恪亲卫入城接应。</p><p>\"沈兄，\"临散会时，李恪叫住沈长歌，\"此次入宫，凶险万分。你可有把握？\"</p><p>\"没有。\"沈长歌坦然道，\"但该做的事，总得有人去做。\"</p><p>李恪看着他，忽然轻轻叹了口气：\"沈兄，你若是生在太平年间，定是个名动天下的良将。可惜……\"</p><p>\"可惜生在了乱世。\"沈长歌接道，\"乱世也有乱世的好处，至少活得明白。\"</p><p>李恪笑了，没有再说什么。</p><p>三日后，太后寿辰。宫中张灯结彩，丝竹之声不绝于耳。百官入宫贺寿，觥筹交错间，谁也不知道一场惊天动地的变故正在酝酿。</p><p>入夜，沈长歌换上太医院小厮的衣裳，跟在孙老先生身后，从太医院后面的一处废弃的井道潜入了内宫。井道狭窄阴暗，两人摸黑前行，走了约两炷香的时间，才从一处假山后的出口钻了出来。</p><p>出口正对着陛下寝殿的偏门。沈长歌抬头望去，只见寝殿灯火通明，殿外站着十几个禁军，甲胄在灯光下闪着寒光。孙老先生示意他稍等，自己上前与守卫的禁军首领低声交谈了几句，那人脸色微变，随即点了点头，让出了道路。</p><p>\"这位是禁军副统领韩将军，是我当年的门生。\"孙老先生低声道，\"他已倒向二殿下。今夜殿外的禁军都是他的人，太后的人一个也进不来。\"</p><p>沈长歌点头，跟着孙老先生快步入殿。寝殿内弥漫着浓重的药味，龙榻上躺着一个须发皆白的老者，正是当今陛下。他脸色蜡黄，双目紧闭，显然已昏迷多时。</p><p>\"陛下！\"孙老先生扑到榻前，颤声道，\"陛下醒醒，老臣带人来了！\"</p><p>陛下缓缓睁开眼睛，浑浊的目光扫过两人，最终落在沈长歌脸上。那目光中闪过一丝复杂的神色——惊讶、愧疚、还有一丝说不清的悲悯。</p><p>\"你是……沈牧之的儿子？\"陛下的声音沙哑而虚弱。</p><p>\"正是。\"沈长歌跪下，\"臣沈长歌，叩见陛下。\"</p><p>陛下闭了闭眼，一滴浊泪从眼角滑落：\"八年了……朕……朕对不起你父亲……\"</p><p>\"陛下，\"孙老先生急道，\"如今不是叙旧的时候！太后今夜就要对陛下下毒手，我们必须在宴席结束前拿出证据，下旨定罪！\"</p><p>陛下强撑着坐起身，目光渐渐清明：\"证据……在哪里？\"</p><p>沈长歌从怀中取出那封泛黄的密函，双手呈上。陛下接过，颤抖着展开，看了几行，脸色便铁青起来。</p><p>\"好……好一个淑妃……好一个太后……\"陛下咬牙切齿，\"朕瞎了眼，竟宠了她二十年……\"</p><p>他挣扎着想要起身，却被孙老先生按住：\"陛下保重龙体！您只需下一道旨即可！\"</p><p>\"好！\"陛下指着案上的笔墨，\"拟旨！\"</p>',NULL,'richtext',7180,4,NULL,0,1.00,1,'2026-07-28 16:39:00',0,'admin','2026-07-28 16:39:00','','2026-07-28 16:39:00',NULL,'0'),(15,2,'第五章 山河定','<p>圣旨拟就，孙老先生亲手用上了玉玺。沈长歌将圣旨贴身收好，正欲告退去办差，殿外忽然传来一阵急促的脚步声。</p><p>韩将军大步闯入，脸色铁青：\"二位大人，不好了！太后察觉到宫中有变，提前离席，正带着她的人往寝殿赶来！\"</p><p>\"多少人？\"沈长歌问。</p><p>\"约莫两百人，都是她的死士。\"韩将军咬牙，\"我这边只有一百人，怕是顶不住多久。\"</p><p>沈长歌与孙老先生对视一眼，都看到了对方眼中的焦急。圣旨虽已拟就，但若无陛下的亲口口谕，禁军中那些太后一党的将领不会轻易服从。必须让陛下当着众人的面，亲自宣布废黜太后。</p><p>\"请陛下移驾前殿！\"孙老先生当机立断，\"只要陛下到了前殿，当着百官的面下旨，太后的那些死士便不敢妄动！\"</p><p>陛下虚弱地点了点头。沈长歌二话不说，将陛下背起，跟着韩将军冲出寝殿。身后，太后的死士已经杀到，与韩将军的禁军在殿前广场上混战成一团。刀光剑影中，沈长歌一手扶着背上的陛下，一手拔出腰间长刀，劈开一条血路。</p><p>\"拦住他们！\"一个尖利的女声从远处传来。沈长歌抬头，只见一个身着凤袍的老妇人在数十名侍卫的簇拥下，从前殿的方向杀来。那妇人虽已年过半百，眼神却毒辣如蛇，正是太后。</p><p>\"沈牧之的余孽！竟敢擅闯禁宫！\"太后看清沈长歌的脸，怒不可遏，\"给我杀了他！不论死活！\"</p><p>数十名死士围了上来。沈长歌将陛下交给韩将军保护，自己横刀立马，挡在两人身前。他虽然自幼习武，又在北疆征战八年，但面对数十名训练有素的死士，仍是凶险万分。不到十招，他的左臂便挨了一刀，鲜血顿时浸透了半边衣衫。</p><p>\"沈兄撑住！\"一个清亮的声音自殿顶传来。苏映雪不知何时已跃上了房梁，此刻如一只白鹤般翩然落下，长剑出鞘，剑光如雪，瞬息之间便刺倒了三名死士。她与沈长歌背靠背而立，刀剑合璧，竟将那数十名死士挡在了丈许之外。</p><p>\"你怎么进来的？\"沈长歌边战边问。</p><p>\"太医院的密道不止一条。\"苏映雪淡淡道，\"我师父当年在宫中走动，留下过一张图。\"</p><p>沈长歌哑然失笑。这个女子，总是能在他最意想不到的时刻出现。</p><p>这时，前殿的方向传来一阵喧哗，李恪带着他的人终于杀到了。他一身玄甲，手持长剑，冲在最前面，身后跟着数十名精锐禁军。太后一党的死士见状，顿时溃不成军，纷纷跪地请降。</p><p>太后被几个侍卫护着，退到了大殿的角落。她看着满地的尸体和跪伏的侍卫，脸上的表情从愤怒转为绝望，最终化为一片死灰。</p><p>\"李恪……\"她盯着二皇子，声音嘶哑，\"你这个逆子……\"</p><p>\"太后。\"李恪走到她面前，神色冰冷，\"二十年了，你害了多少人？沈家满门、先皇后、还有那些被你构陷的忠臣良将……今日，是他们讨回公道的时候了。\"</p><p>太后忽然笑了起来，笑声凄厉刺耳：\"你以为扳倒了我，你就能坐稳这江山？李恪，你和你父皇一样，都不过是这宫墙里的囚徒罢了！\"</p><p>李恪没有理会她的疯话，转身走到陛下面前，跪下：\"父皇，太后罪证确凿，请父皇降旨！\"</p><p>陛下虚弱地靠在龙椅上，目光扫过满殿狼藉，最后落在沈长歌身上。他缓缓伸出手，声音颤抖：\"沈长歌……上前。\"</p><p>沈长歌上前几步，跪下。</p><p>\"你父亲……是朕对不起他。\"陛下的眼中满是愧疚，\"朕这半生，做了太多糊涂事。如今……朕已时日无多。这江山，朕交给恪儿；但这桩冤案……朕要亲手了结。\"</p><p>他挣扎着站起身，从孙老先生手中接过那份已用印的圣旨，当着百官的面高声宣读：沈牧之案乃太后构陷，沈家满门恢复名誉，追封沈牧之为忠义公，钦此。太后凤印收回，幽禁于冷宫，余生不得出。</p><p>宣旨完毕，陛下仿佛耗尽了最后一丝力气，颓然倒回龙椅。李恪忙上前扶住，泪流满面。陛下握住他的手，断断续续地说：\"恪儿……这江山……交给你了……善待百姓……善待功臣……还有……沈家……\"</p><p>话未说完，陛下的手便无力地垂下，溘然长逝。</p><p>大殿内一片寂静。片刻后，百官齐齐跪倒，山呼万岁。李恪跪在父皇灵前，泪如雨下。沈长歌也跪了下去，心中却出奇地平静。八年的隐忍、八年的等待，终于在这一刻画上了句号。父亲的冤屈已雪，沈家的清白已复，而他自己……</p><p>他抬起头，与苏映雪的目光在一片纷乱中相遇。她站在人群之外，依旧是一身月白僧袍，神情却比平日柔和了许多。她朝他微微点了点头，像是说：你做到了。</p><p>三日后，先帝大殓。李恪遵遗诏继位，改元永安。新帝登基的第一道旨意，便是为沈家平反昭雪，追封沈牧之为忠义公，并下旨召沈长歌回京任职。</p><p>然而沈长歌没有回京。</p><p>李恪登基的第二天，沈长歌便带着苏映雪离开了长安。他留下一封信，信上只有寥寥数语：\"臣无意于朝堂，今使命已了，愿归隐山林。陛下保重。\"</p><p>李恪看完信，沉默良久，最终叹了口气，没有派人追。他明白，沈长歌是那种一旦决定了便不会回头的人。强留，反而失了君臣之义。</p><p>沈长歌与苏映雪一路北上，回到并州。突厥在得知大周内乱的消息后，果然再度南下，却被早已做好准备的并州守军迎头痛击。沈长歌重新披甲上阵，率军北上，在雁门关外与突厥决战，大破敌军，斩首三万。突厥可汗仓皇北逃，十年之内再无南下之力。</p><p>战后，沈长歌将兵权交还朝廷，与苏映雪一同隐居在雁门关外的一座小山村中。那里依山傍水，远离尘嚣。他们盖了几间草屋，种了几亩薄田，过起了日出而作、日落而息的生活。</p><p>永安三年春，李恪下旨遍寻天下名士入京辅政，特派使者赴雁门关外请沈长歌出山。使者到了那座小山村，只见草屋依旧，门前却长满了青苔，显然已许久无人居住。</p><p>使者询问村中老叟，老叟笑道：\"沈先生夫妇半年前便走了，说是要去江南看看烟雨。走时留下一句话——山河万里，何处不是家。\"</p><p>使者怅然若失，回去禀报。李恪听后，久久不语，最后提笔在御书房的墙上，写下四个字：山河万里。</p><p>从此，朝中再无沈长歌的消息。但江湖之间，却渐渐流传起一个传说：江南有一对侠侣，男的佩刀，女的携剑，专管不平之事。他们来去如风，从不留名，只在离去时，会在墙上留下一枝梅花。</p><p>有人说见过他们在西湖边煮酒，有人说见过他们在金陵城头看月，还有人说见过他们在岭南的一座旧宅前，对着一块石碑枯坐了一整夜。</p><p>没有人知道他们究竟去了哪里。但每一个听过这传说的人，都会在心中默默念一句：山河万里，何处不是家。</p><p>而那本记录了沈长歌一生传奇的卷宗，则被李恪亲手收入了皇家秘档。卷宗的封面上，用工整的小楷写着四个字——</p><p>山河万里。</p><p>（全书完）</p>',NULL,'richtext',7260,5,NULL,0,1.00,1,'2026-07-28 16:39:00',0,'admin','2026-07-28 16:39:00','','2026-07-28 16:39:00',NULL,'0');
/*!40000 ALTER TABLE `portal_book_chapter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_book_chapter_view`
--

DROP TABLE IF EXISTS `portal_book_chapter_view`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_book_chapter_view` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `chapter_id` bigint NOT NULL COMMENT '章节ID',
                                            `book_id` bigint NOT NULL COMMENT '书籍ID',
                                            `user_id` bigint DEFAULT NULL COMMENT '用户ID（未登录为NULL）',
                                            `client_ip` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端IP',
                                            `read_duration_ms` int DEFAULT '0' COMMENT '阅读时长（毫秒）',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
                                            `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            KEY `idx_chapter_id` (`chapter_id`),
                                            KEY `idx_user_id` (`user_id`),
                                            KEY `idx_create_time` (`create_time`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='章节浏览记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_chapter_view`
--

LOCK TABLES `portal_book_chapter_view` WRITE;
/*!40000 ALTER TABLE `portal_book_chapter_view` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_book_chapter_view` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_book_list`
--

DROP TABLE IF EXISTS `portal_book_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_list`
--

LOCK TABLES `portal_book_list` WRITE;
/*!40000 ALTER TABLE `portal_book_list` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_book_list` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_book_list_bookmark`
--

DROP TABLE IF EXISTS `portal_book_list_bookmark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_list_bookmark`
--

LOCK TABLES `portal_book_list_bookmark` WRITE;
/*!40000 ALTER TABLE `portal_book_list_bookmark` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_book_list_bookmark` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_book_list_item`
--

DROP TABLE IF EXISTS `portal_book_list_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_list_item`
--

LOCK TABLES `portal_book_list_item` WRITE;
/*!40000 ALTER TABLE `portal_book_list_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_book_list_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_book_list_like`
--

DROP TABLE IF EXISTS `portal_book_list_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_list_like`
--

LOCK TABLES `portal_book_list_like` WRITE;
/*!40000 ALTER TABLE `portal_book_list_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_book_list_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_book_quote`
--

DROP TABLE IF EXISTS `portal_book_quote`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_quote`
--

LOCK TABLES `portal_book_quote` WRITE;
/*!40000 ALTER TABLE `portal_book_quote` DISABLE KEYS */;
INSERT INTO `portal_book_quote` VALUES (1,1,1,NULL,'架构师的工作不是选\"最优解\"，而是在成本、人力、时间、复杂度之间找到当前阶段最合理的平衡点。',NULL,'第一章',0,1,1,'1.1 从码农到工程师','admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(2,1,1,NULL,'能造轮子不等于一定要造，但具备这个能力，意味着你对技术的理解已经穿透了表象。',NULL,'第一章',0,1,0,'1.2 技术深度的三个层次','admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(3,1,1,NULL,'一个函数只做一件事。判断标准：能否用一句话描述它的职责而不用\"和\"字。',NULL,'第二章',0,1,1,'2.2 函数：短小再短小','admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(4,1,1,NULL,'静默吞掉的异常，会在生产环境变成无法定位的幽灵问题。',NULL,'第二章',0,1,0,'2.4 异常处理','admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(5,1,1,NULL,'安全配置缺失时应该拒绝启动，而不是回退到默认弱配置。',NULL,'第七章',0,1,1,'7.5 最小权限与 fail-fast','admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0'),(6,1,1,NULL,'技术的红利是复利的。今天多读的一篇源码、多复盘的一个事故，三年后是分水岭。',NULL,'第十章',0,1,1,'10.4 成长是长期主义','admin','2026-07-28 16:31:38','','2026-07-28 16:31:38',NULL,'0');
/*!40000 ALTER TABLE `portal_book_quote` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_book_quote_like`
--

DROP TABLE IF EXISTS `portal_book_quote_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_quote_like`
--

LOCK TABLES `portal_book_quote_like` WRITE;
/*!40000 ALTER TABLE `portal_book_quote_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_book_quote_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_book_recommend`
--

DROP TABLE IF EXISTS `portal_book_recommend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_book_recommend` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `book_id` bigint NOT NULL COMMENT '书籍ID',
                                         `position` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '推荐位置：home_banner=首页轮播 / home_hot=首页热门 / category_top=分类顶推 / limit_free=限免专区 / discover_banner=发现页轮播',
                                         `sort` int DEFAULT '0' COMMENT '排序（越小越靠前）',
                                         `start_time` datetime DEFAULT NULL COMMENT '推荐开始时间（NULL 表示立即生效）',
                                         `end_time` datetime DEFAULT NULL COMMENT '推荐结束时间（NULL 表示长期有效）',
                                         `is_active` tinyint(1) DEFAULT '1' COMMENT '是否生效：1=生效，0=下架',
                                         `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注（运营说明）',
                                         `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_book_position` (`book_id`,`position`),
                                         KEY `idx_position` (`position`),
                                         KEY `idx_is_active` (`is_active`),
                                         KEY `idx_time_window` (`start_time`,`end_time`),
                                         KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='书籍推荐位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_recommend`
--

LOCK TABLES `portal_book_recommend` WRITE;
/*!40000 ALTER TABLE `portal_book_recommend` DISABLE KEYS */;
INSERT INTO `portal_book_recommend` VALUES (1,1,'home_hot',1,NULL,NULL,1,NULL,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38','0'),(2,1,'discover_banner',3,NULL,NULL,1,NULL,'admin','2026-07-28 16:31:38','','2026-07-28 16:31:38','0');
/*!40000 ALTER TABLE `portal_book_recommend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_bookmark`
--

DROP TABLE IF EXISTS `portal_bookmark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_bookmark` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
                                   `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                   `article_id` bigint NOT NULL COMMENT '文章ID',
                                   `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                   `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                   `user_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户业务主键（关联 portal_user.business_id）',
                                   `article_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文章业务主键（关联 portal_article.business_id）',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_article_id` (`article_id`),
                                   KEY `idx_user_bid` (`user_business_id`),
                                   KEY `idx_article_bid` (`article_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_bookmark`
--

LOCK TABLES `portal_bookmark` WRITE;
/*!40000 ALTER TABLE `portal_bookmark` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_bookmark` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_bookshelf`
--

DROP TABLE IF EXISTS `portal_bookshelf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_bookshelf` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                    `book_id` bigint NOT NULL COMMENT '书籍ID',
                                    `last_chapter_id` bigint DEFAULT NULL COMMENT '最后阅读章节ID（冗余，用于续读）',
                                    `last_chapter_no` int DEFAULT '0' COMMENT '最后阅读章节序号',
                                    `sort` int DEFAULT '0' COMMENT '排序（用户自定义书架顺序，越大越靠前）',
                                    `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                                    `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                    `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_user_book` (`user_id`,`book_id`),
                                    KEY `idx_user_id` (`user_id`),
                                    KEY `idx_book_id` (`book_id`),
                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户书架（收藏书籍）表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_bookshelf`
--

LOCK TABLES `portal_bookshelf` WRITE;
/*!40000 ALTER TABLE `portal_bookshelf` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_bookshelf` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_category`
--

DROP TABLE IF EXISTS `portal_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_category` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
                                   `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
                                   `slug` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类别名',
                                   `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类描述',
                                   `icon` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标URL',
                                   `sort` int DEFAULT '0' COMMENT '排序',
                                   `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
                                   `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                   `show_in_nav` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在头部栏目展示（0否/1是）',
                                   `nav_route_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'category' COMMENT '路由类型（home/category/static/external）',
                                   `nav_route_path` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '静态/外链路由路径（仅 static/external 类型使用）',
                                   `requires_auth` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要登录（0否/1是）',
                                   `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                   `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                   `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                   `business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务主键（前缀cat_）',
                                   `parent_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父分类业务主键（自引用 portal_category.business_id）',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_business_id` (`business_id`),
                                   KEY `idx_parent_id` (`parent_id`),
                                   KEY `idx_slug` (`slug`),
                                   KEY `idx_show_in_nav` (`show_in_nav`),
                                   KEY `idx_del_flag` (`del_flag`),
                                   KEY `idx_parent_bid` (`parent_business_id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `portal_tag`
--

DROP TABLE IF EXISTS `portal_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_tag` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
                              `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签名称',
                              `slug` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签别名',
                              `sort` int DEFAULT '0' COMMENT '排序',
                              `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                              `module` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属模块（article/interview_question/interview_experience/interview_resume_template 等，null 表示通用）',
                              `reference_count` bigint unsigned DEFAULT '0' COMMENT '被引用次数（冗余计数列，绑定/解绑时同步维护）',
                              `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                              `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                              `business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务主键（前缀tag_）',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_name` (`name`),
                              UNIQUE KEY `uk_business_id` (`business_id`),
                              KEY `idx_slug` (`slug`),
                              KEY `idx_module` (`module`),
                              KEY `idx_reference_count` (`reference_count` DESC),
                              KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户标签表';
/*!40101 SET character_set_client = @saved_cs_client */;
--
-- Dumping data for table `portal_category`
--


/*!40000 ALTER TABLE `portal_category` DISABLE KEYS */;
-- =============================================================================
-- 墨韵智库 - 前台栏目（portal_category）+ 标签（portal_tag）完整初始化脚本
-- =============================================================================
-- 整合来源：
--   94_category_nav_fields_init.sql  50 个栏目（8 一级 + 42 二级）+ 4 个导航字段
--   30_alter_portal_tag_fields.sql  portal_tag 表补充 module / reference_count 字段
--   05_moyun_v2_init.sql           28 个标签种子数据（8 人文 + 12 技术 + 8 通用）
--
-- 特性：
--   1. 幂等：DDL 用 information_schema 守护；DML 用 INSERT ... SELECT ... WHERE NOT EXISTS（按 slug 去重）
--   2. 二级栏目的 parent_id 用子查询按 slug 反查，不依赖硬编码 id，可重复执行
--   3. 末尾带校验查询
--
-- 路由类型说明（nav_route_type）：
--   home      : 首页，path 固定为 '/'
--   category  : 动态分类栏目，前端拼装为 /category/<encodeURIComponent(name)>
--   static    : 静态路由，path = nav_route_path（如 /reading/discover）
--   external  : 外部链接，path = nav_route_path，新窗口打开
--
-- 执行前提：
--   portal_category / portal_tag 表已由 03_portal_init.sql 创建
--   本脚本只补充字段 + 插入种子数据，不重建表结构
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- 第一部分：portal_category 表补充导航字段（来自 94 + 30）
-- =============================================================================

-- 1.1 show_in_nav：是否在头部栏目展示（0否/1是）
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_category' AND column_name = 'show_in_nav');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_category` ADD COLUMN `show_in_nav` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否在头部栏目展示（0否/1是）'' AFTER `status`', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 nav_route_type：路由类型
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_category' AND column_name = 'nav_route_type');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_category` ADD COLUMN `nav_route_type` varchar(20) NOT NULL DEFAULT ''category'' COMMENT ''路由类型（home/category/static/external）'' AFTER `show_in_nav`', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 nav_route_path：静态/外链路径
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_category' AND column_name = 'nav_route_path');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_category` ADD COLUMN `nav_route_path` varchar(200) DEFAULT NULL COMMENT ''静态/外链路由路径（仅 static/external 类型使用）'' AFTER `nav_route_type`', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 requires_auth：是否需要登录
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_category' AND column_name = 'requires_auth');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_category` ADD COLUMN `requires_auth` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否需要登录（0否/1是）'' AFTER `nav_route_path`', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.5 索引：加速 Navbar 查询 show_in_nav=1
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_category' AND index_name = 'idx_show_in_nav');
SET @s := IF(@i = 0, 'ALTER TABLE `portal_category` ADD INDEX `idx_show_in_nav` (`show_in_nav`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 第二部分：portal_tag 表补充 module / reference_count 字段（来自 30）
-- =============================================================================

-- 2.1 module：所属模块
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_tag' AND column_name = 'module');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_tag` ADD COLUMN `module` varchar(50) DEFAULT NULL COMMENT ''所属模块（article/interview_question/interview_experience/interview_resume_template 等，null 表示通用）'' AFTER `status`', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 reference_count：被引用次数
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_tag' AND column_name = 'reference_count');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_tag` ADD COLUMN `reference_count` bigint unsigned DEFAULT 0 COMMENT ''被引用次数（冗余计数列，绑定/解绑时同步维护）'' AFTER `module`', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.3 索引
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_tag' AND index_name = 'idx_module');
SET @s := IF(@i = 0, 'ALTER TABLE `portal_tag` ADD INDEX `idx_module` (`module`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_tag' AND index_name = 'idx_reference_count');
SET @s := IF(@i = 0, 'ALTER TABLE `portal_tag` ADD INDEX `idx_reference_count` (`reference_count` DESC)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 第三部分：一级栏目（8 个）
-- 用 slug 反查 parent_id，不依赖硬编码 id，可重复执行
-- =============================================================================

-- 3.1 首页
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '首页', 'home', '精选推荐、双轨轮播', 'fa-home', 1, 0, '0', 1, 'home', '/', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'home' AND parent_id = 0);

-- 3.2 散文天地
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '散文天地', 'prose', '人文书写与情感表达', 'fa-pen-fancy', 2, 0, '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'prose' AND parent_id = 0);

-- 3.3 技术笔记
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '技术笔记', 'tech-notes', '开发记录、技术解析、AI编程实践', 'fa-code', 3, 0, '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'tech-notes' AND parent_id = 0);

-- 3.4 读书空间
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '读书空间', 'reading', '读书心得、精选好书、书单推荐', 'fa-book', 4, 0, '0', 1, 'static', '/reading', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'reading' AND parent_id = 0);

-- 3.5 面试指南
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '面试指南', 'interview', '真题整理、面经复盘、简历优化', 'fa-briefcase', 5, 0, '0', 1, 'static', '/interview', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'interview' AND parent_id = 0);

-- 3.6 社区互动
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '社区互动', 'interaction', '话题讨论、动态广场', 'fa-users', 6, 0, '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'interaction' AND parent_id = 0);

-- 3.7 创作者中心
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '创作者中心', 'creator', '发布文章、专栏、征文、认证', 'fa-feather', 7, 0, '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'creator' AND parent_id = 0);

-- 3.8 个人空间
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '个人空间', 'mine', '个人中心、成长时间线、我的内容', 'fa-user', 8, 0, '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'mine' AND parent_id = 0);

-- =============================================================================
-- 第四部分：二级栏目 - 散文天地（7 项，全部 category 类型）
-- parent_id 用 (SELECT id FROM portal_category WHERE slug='prose' AND parent_id=0) 反查
-- =============================================================================
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '人间烟火', 'life-stories', '饮食、市井、生活琐记', 'fa-utensils', 1,
       (SELECT id FROM portal_category WHERE slug = 'prose' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'life-stories');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '山河行吟', 'travel-nature', '游记、自然书写、生态散文', 'fa-mountain', 2,
       (SELECT id FROM portal_category WHERE slug = 'prose' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'travel-nature');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '心灵独白', 'inner-thoughts', '孤独、成长、疗愈随笔', 'fa-heart', 3,
       (SELECT id FROM portal_category WHERE slug = 'prose' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'inner-thoughts');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '城市笔记', 'city-notes', '北上广深、小镇观察', 'fa-city', 4,
       (SELECT id FROM portal_category WHERE slug = 'prose' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'city-notes');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '四季专栏', 'seasons', '春之思、夏之躁、秋之静、冬之藏', 'fa-leaf', 5,
       (SELECT id FROM portal_category WHERE slug = 'prose' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'seasons');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '声音散文', 'audio-prose', '作者自读、背景音效沉浸体验', 'fa-volume-up', 6,
       (SELECT id FROM portal_category WHERE slug = 'prose' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'audio-prose');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '读者来信', 'reader-letters', '短篇心声刊发与回声计划', 'fa-envelope', 7,
       (SELECT id FROM portal_category WHERE slug = 'prose' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'reader-letters');

-- =============================================================================
-- 第五部分：二级栏目 - 技术笔记（6 项，全部 category 类型）
-- =============================================================================
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '技术栈手册', 'tech-stack', 'Java/SpringBoot、React/Vue、Flutter/UniApp', 'fa-book-open', 1,
       (SELECT id FROM portal_category WHERE slug = 'tech-notes' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'tech-stack');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '架构札记', 'architecture', '微服务、缓存策略、分布式事务', 'fa-project-diagram', 2,
       (SELECT id FROM portal_category WHERE slug = 'tech-notes' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'architecture');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '性能日志', 'performance', 'SQL优化、前端加载、JVM调优', 'fa-tachometer-alt', 3,
       (SELECT id FROM portal_category WHERE slug = 'tech-notes' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'performance');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT 'AI编程', 'ai-coding', 'Cursor使用、ChatGPT提示工程、AI排错记录', 'fa-robot', 4,
       (SELECT id FROM portal_category WHERE slug = 'tech-notes' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'ai-coding');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '开源日志', 'open-source', 'PR提交、Issue解决、源码阅读', 'fa-code-branch', 5,
       (SELECT id FROM portal_category WHERE slug = 'tech-notes' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'open-source');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '新手入门', 'beginner', '环境配置、第一行代码实录', 'fa-play-circle', 6,
       (SELECT id FROM portal_category WHERE slug = 'tech-notes' AND parent_id = 0), '0', 1, 'category', NULL, 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'beginner');

-- =============================================================================
-- 第六部分：二级栏目 - 读书空间（4 项，全部 static 类型）
-- =============================================================================
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '读书首页', 'reading-home', '读书空间总入口', 'fa-book-reader', 1,
       (SELECT id FROM portal_category WHERE slug = 'reading' AND parent_id = 0), '0', 1, 'static', '/reading', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'reading-home');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '发现好书', 'reading-discover', '发现好书、书单推荐', 'fa-list', 2,
       (SELECT id FROM portal_category WHERE slug = 'reading' AND parent_id = 0), '0', 1, 'static', '/reading/discover', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'reading-discover');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '金句摘录', 'reading-quotes', '高光语句+个人批注', 'fa-quote-left', 3,
       (SELECT id FROM portal_category WHERE slug = 'reading' AND parent_id = 0), '0', 1, 'static', '/reading/quotes', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'reading-quotes');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '我的书架', 'reading-bookshelf', '个人书架管理', 'fa-bookmark', 4,
       (SELECT id FROM portal_category WHERE slug = 'reading' AND parent_id = 0), '0', 1, 'static', '/reading/bookshelf', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'reading-bookshelf');

-- =============================================================================
-- 第七部分：二级栏目 - 面试指南（10 项，全部 static 类型，4 项 requires_auth=1）
-- =============================================================================
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '面试题库', 'interview-questions', '算法题、系统设计、行为面试', 'fa-clipboard-list', 1,
       (SELECT id FROM portal_category WHERE slug = 'interview' AND parent_id = 0), '0', 1, 'static', '/interview/questions', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'interview-questions');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '面试经验', 'interview-experiences', '大厂面试全流程还原', 'fa-chart-line', 2,
       (SELECT id FROM portal_category WHERE slug = 'interview' AND parent_id = 0), '0', 1, 'static', '/interview/experiences', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'interview-experiences');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '简历模板', 'interview-resume-templates', '技术亮点提炼、项目描述技巧', 'fa-file-alt', 3,
       (SELECT id FROM portal_category WHERE slug = 'interview' AND parent_id = 0), '0', 1, 'static', '/interview/resume-templates', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'interview-resume-templates');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT 'AI 模拟面试', 'interview-mock', '自测题集、答题思路拆解', 'fa-microphone', 4,
       (SELECT id FROM portal_category WHERE slug = 'interview' AND parent_id = 0), '0', 1, 'static', '/interview/mock', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'interview-mock');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '学习中心', 'learn-center', '学习中心总入口', 'fa-graduation-cap', 5,
       (SELECT id FROM portal_category WHERE slug = 'interview' AND parent_id = 0), '0', 1, 'static', '/learn', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'learn-center');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '知识图谱', 'learn-knowledge', '知识体系可视化', 'fa-project-diagram', 6,
       (SELECT id FROM portal_category WHERE slug = 'interview' AND parent_id = 0), '0', 1, 'static', '/learn/knowledge', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'learn-knowledge');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '刷题排行榜', 'learn-leaderboard', '刷题榜、学习榜', 'fa-trophy', 7,
       (SELECT id FROM portal_category WHERE slug = 'interview' AND parent_id = 0), '0', 1, 'static', '/learn/leaderboard', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'learn-leaderboard');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '学习计划', 'learn-plan', '个人学习计划管理', 'fa-calendar-alt', 8,
       (SELECT id FROM portal_category WHERE slug = 'interview' AND parent_id = 0), '0', 1, 'static', '/learn/plan', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'learn-plan');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '错题本', 'learn-wrong', '错题归集与复习', 'fa-times-circle', 9,
       (SELECT id FROM portal_category WHERE slug = 'interview' AND parent_id = 0), '0', 1, 'static', '/learn/wrong', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'learn-wrong');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '刷题日历', 'learn-calendar', '刷题打卡日历', 'fa-calendar-check', 10,
       (SELECT id FROM portal_category WHERE slug = 'interview' AND parent_id = 0), '0', 1, 'static', '/learn/calendar', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'learn-calendar');

-- =============================================================================
-- 第八部分：二级栏目 - 社区互动（2 项，全部 static 类型）
-- =============================================================================
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '话题广场', 'topics', '话题讨论列表', 'fa-comments', 1,
       (SELECT id FROM portal_category WHERE slug = 'interaction' AND parent_id = 0), '0', 1, 'static', '/topics', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'topics');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '动态广场', 'feed', '用户动态流', 'fa-stream', 2,
       (SELECT id FROM portal_category WHERE slug = 'interaction' AND parent_id = 0), '0', 1, 'static', '/feed', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'feed');

-- =============================================================================
-- 第九部分：二级栏目 - 创作者中心（6 项，全部 static 类型，2 项 requires_auth=1）
-- =============================================================================
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '发布文章', 'publish', '发布新文章', 'fa-edit', 1,
       (SELECT id FROM portal_category WHERE slug = 'creator' AND parent_id = 0), '0', 1, 'static', '/publish', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'publish');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '专栏广场', 'columns', '专栏列表与订阅', 'fa-columns', 2,
       (SELECT id FROM portal_category WHERE slug = 'creator' AND parent_id = 0), '0', 1, 'static', '/columns', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'columns');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '征文活动', 'contests', '征文活动、技术挑战赛', 'fa-file-upload', 3,
       (SELECT id FROM portal_category WHERE slug = 'creator' AND parent_id = 0), '0', 1, 'static', '/contests', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'contests');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '创作者认证', 'creator-certification', '申请创作者认证', 'fa-certificate', 4,
       (SELECT id FROM portal_category WHERE slug = 'creator' AND parent_id = 0), '0', 1, 'static', '/creator/certification', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'creator-certification');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '创作者列表', 'authors', '认证创作者列表', 'fa-users-cog', 5,
       (SELECT id FROM portal_category WHERE slug = 'creator' AND parent_id = 0), '0', 1, 'static', '/authors', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'authors');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '成长排行榜', 'ranking', '成长值排行榜', 'fa-trophy', 6,
       (SELECT id FROM portal_category WHERE slug = 'creator' AND parent_id = 0), '0', 1, 'static', '/ranking', 0, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'ranking');

-- =============================================================================
-- 第十部分：二级栏目 - 个人空间（7 项，全部 static 类型，全部 requires_auth=1）
-- =============================================================================
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '个人中心', 'user', '个人中心主页', 'fa-user-circle', 1,
       (SELECT id FROM portal_category WHERE slug = 'mine' AND parent_id = 0), '0', 1, 'static', '/user', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'user');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '成长时间线', 'growth-timeline', '成长记录时间线', 'fa-chart-line', 2,
       (SELECT id FROM portal_category WHERE slug = 'mine' AND parent_id = 0), '0', 1, 'static', '/growth/timeline', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'growth-timeline');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '我的专栏', 'column-my', '我创建的专栏', 'fa-columns', 3,
       (SELECT id FROM portal_category WHERE slug = 'mine' AND parent_id = 0), '0', 1, 'static', '/column/my', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'column-my');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '我的文章', 'my-articles', '我发布的文章', 'fa-file-alt', 4,
       (SELECT id FROM portal_category WHERE slug = 'mine' AND parent_id = 0), '0', 1, 'static', '/my/articles', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'my-articles');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '我的话题', 'topic-my-topics', '我发起的话题', 'fa-comments', 5,
       (SELECT id FROM portal_category WHERE slug = 'mine' AND parent_id = 0), '0', 1, 'static', '/topic/my/topics', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'topic-my-topics');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '我的观点', 'topic-my-posts', '我发表的观点', 'fa-comment', 6,
       (SELECT id FROM portal_category WHERE slug = 'mine' AND parent_id = 0), '0', 1, 'static', '/topic/my/posts', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'topic-my-posts');

INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by)
SELECT '我的成就', 'achievements', '我的成就与徽章', 'fa-award', 7,
       (SELECT id FROM portal_category WHERE slug = 'mine' AND parent_id = 0), '0', 1, 'static', '/achievements', 1, 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_category WHERE slug = 'achievements');

-- =============================================================================
-- 第十一部分：标签种子数据（28 个，来自 05_moyun_v2_init.sql）
-- name 字段存储纯文本，不带 # 前缀（# 属于展示符号，由前端按需拼接）
-- 按 slug 去重，可重复执行
-- =============================================================================

-- 11.1 人文类标签（8 个）
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '生活哲思', 'life-philosophy', 1, '0', 'admin', '人文类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'life-philosophy');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '城市记忆', 'city-memory', 2, '0', 'admin', '人文类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'city-memory');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '自然写作', 'nature-writing', 3, '0', 'admin', '人文类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'nature-writing');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '情感随笔', 'emotional-essay', 4, '0', 'admin', '人文类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'emotional-essay');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '人间烟火', 'life-fireworks', 5, '0', 'admin', '人文类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'life-fireworks');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '乡愁记忆', 'nostalgia', 6, '0', 'admin', '人文类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'nostalgia');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '孤独成长', 'loneliness-growth', 7, '0', 'admin', '人文类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'loneliness-growth');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '四季感悟', 'seasons-feeling', 8, '0', 'admin', '人文类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'seasons-feeling');

-- 11.2 技术类标签（12 个）
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT 'SpringBoot实战', 'springboot-practice', 9, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'springboot-practice');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT 'React Hooks', 'react-hooks', 10, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'react-hooks');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT 'AI辅助开发', 'ai-assisted-dev', 11, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'ai-assisted-dev');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '算法突破', 'algorithm-breakthrough', 12, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'algorithm-breakthrough');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT 'Java并发', 'java-concurrency', 13, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'java-concurrency');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT 'Vue3实践', 'vue3-practice', 14, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'vue3-practice');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '微服务架构', 'microservices', 15, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'microservices');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT 'MySQL优化', 'mysql-optimization', 16, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'mysql-optimization');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT 'Git协作', 'git-collaboration', 17, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'git-collaboration');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '前端性能', 'frontend-performance', 18, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'frontend-performance');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT 'JVM调优', 'jvm-tuning', 19, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'jvm-tuning');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '系统设计', 'system-design', 20, '0', 'admin', '技术类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'system-design');

-- 11.3 通用类标签（8 个）
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '新手入门', 'beginner-guide', 21, '0', 'admin', '通用类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'beginner-guide');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '进阶提升', 'advanced-improvement', 22, '0', 'admin', '通用类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'advanced-improvement');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '面试备战', 'interview-prep', 23, '0', 'admin', '通用类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'interview-prep');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '读书心得', 'reading-notes', 24, '0', 'admin', '通用类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'reading-notes');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '写作技巧', 'writing-tips', 25, '0', 'admin', '通用类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'writing-tips');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '学习方法', 'learning-methods-tag', 26, '0', 'admin', '通用类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'learning-methods-tag');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '职场经验', 'career-experience', 27, '0', 'admin', '通用类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'career-experience');
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark)
SELECT '个人成长', 'personal-growth', 28, '0', 'admin', '通用类'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM portal_tag WHERE slug = 'personal-growth');

-- =============================================================================
-- 第十二部分：校验查询
-- =============================================================================

-- 12.1 栏目统计
SELECT '===== 前台栏目统计 =====' AS info;
SELECT CONCAT('共创建 ', COUNT(*), ' 个栏目') AS summary FROM portal_category;
SELECT
    CASE nav_route_type
        WHEN 'home' THEN '首页'
        WHEN 'category' THEN '动态分类栏目'
        WHEN 'static' THEN '静态路由'
        WHEN 'external' THEN '外部链接'
        END AS route_type_desc,
    COUNT(*) AS cnt,
    SUM(show_in_nav) AS show_in_nav_count
FROM portal_category
GROUP BY nav_route_type;

-- 12.2 栏目层级分布
SELECT '===== 栏目层级分布 =====' AS info;
SELECT
    CASE WHEN parent_id = 0 THEN '一级栏目' ELSE '二级栏目' END AS level_desc,
    COUNT(*) AS cnt
FROM portal_category
GROUP BY CASE WHEN parent_id = 0 THEN '一级栏目' ELSE '二级栏目' END;

-- 12.3 一级栏目 → 二级栏目树
SELECT '===== 栏目树 =====' AS info;
SELECT p1.name AS top_name, p1.slug AS top_slug, p1.nav_route_type AS top_route,
       p2.name AS child_name, p2.slug AS child_slug, p2.nav_route_path AS child_path, p2.requires_auth
FROM portal_category p1
         LEFT JOIN portal_category p2 ON p2.parent_id = p1.id
WHERE p1.parent_id = 0
ORDER BY p1.sort, p2.sort;

-- 12.4 标签统计
SELECT '===== 标签统计 =====' AS info;
SELECT CONCAT('共创建 ', COUNT(*), ' 个标签') AS summary FROM portal_tag;
SELECT remark AS category, COUNT(*) AS cnt FROM portal_tag GROUP BY remark ORDER BY MIN(sort);

-- 12.5 孤儿数据检查（二级栏目 parent_id 对应的父栏目是否存在）
SELECT '===== 孤儿数据检查（应为 0） =====' AS info;
SELECT CONCAT('孤儿二级栏目数: ', COUNT(*)) AS orphan_check
FROM portal_category c2
WHERE c2.parent_id != 0
  AND NOT EXISTS (SELECT 1 FROM portal_category c1 WHERE c1.id = c2.parent_id);

SELECT '前台栏目和标签初始化脚本执行完成！' AS message;

--
-- Table structure for table `portal_code_run`
--

DROP TABLE IF EXISTS `portal_code_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_code_run`
--

LOCK TABLES `portal_code_run` WRITE;
/*!40000 ALTER TABLE `portal_code_run` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_code_run` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_column`
--

DROP TABLE IF EXISTS `portal_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_column`
--

LOCK TABLES `portal_column` WRITE;
/*!40000 ALTER TABLE `portal_column` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_column_article`
--

DROP TABLE IF EXISTS `portal_column_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_column_article`
--

LOCK TABLES `portal_column_article` WRITE;
/*!40000 ALTER TABLE `portal_column_article` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_column_article` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_column_subscribe`
--

DROP TABLE IF EXISTS `portal_column_subscribe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_column_subscribe` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `column_id` bigint NOT NULL COMMENT '专栏ID',
                                           `user_id` bigint NOT NULL COMMENT '订阅用户ID',
                                           `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `uk_column_user` (`column_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏订阅';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_column_subscribe`
--

LOCK TABLES `portal_column_subscribe` WRITE;
/*!40000 ALTER TABLE `portal_column_subscribe` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_column_subscribe` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_comment`
--

DROP TABLE IF EXISTS `portal_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_comment` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
                                  `article_id` bigint NOT NULL COMMENT '文章ID',
                                  `author_id` bigint NOT NULL COMMENT '评论者ID（门户用户ID）',
                                  `content` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '评论内容',
                                  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
                                  `root_id` bigint DEFAULT '0' COMMENT '根评论ID（一级评论ID）',
                                  `reply_to` bigint DEFAULT NULL COMMENT '回复的用户ID',
                                  `reply_to_content` varchar(200) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '被回复的内容摘要',
                                  `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  `business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务主键（前缀com_）',
                                  `article_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文章业务主键（关联 portal_article.business_id）',
                                  `author_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作者业务主键（关联 portal_user.business_id）',
                                  `parent_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父评论业务主键（自引用 portal_comment.business_id）',
                                  `root_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '根评论业务主键（自引用 portal_comment.business_id）',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_business_id` (`business_id`),
                                  KEY `idx_article_id` (`article_id`),
                                  KEY `idx_author_id` (`author_id`),
                                  KEY `idx_parent_id` (`parent_id`),
                                  KEY `idx_article_root` (`article_id`,`root_id`),
                                  KEY `idx_del_flag` (`del_flag`),
                                  KEY `idx_article_bid` (`article_business_id`),
                                  KEY `idx_author_bid` (`author_business_id`),
                                  KEY `idx_parent_bid` (`parent_business_id`),
                                  KEY `idx_root_bid` (`root_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_comment`
--

LOCK TABLES `portal_comment` WRITE;
/*!40000 ALTER TABLE `portal_comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_comment_like`
--

DROP TABLE IF EXISTS `portal_comment_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_comment_like` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                       `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                       `comment_id` bigint NOT NULL COMMENT '评论ID',
                                       `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                       `comment_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '评论业务主键（关联 portal_comment.business_id）',
                                       `user_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户业务主键（关联 portal_user.business_id）',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_user_comment` (`user_id`,`comment_id`),
                                       KEY `idx_user_id` (`user_id`),
                                       KEY `idx_comment_id` (`comment_id`),
                                       KEY `idx_comment_bid` (`comment_business_id`),
                                       KEY `idx_user_bid` (`user_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户评论点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_comment_like`
--

LOCK TABLES `portal_comment_like` WRITE;
/*!40000 ALTER TABLE `portal_comment_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_comment_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_contest_submission`
--

DROP TABLE IF EXISTS `portal_contest_submission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_contest_submission`
--

LOCK TABLES `portal_contest_submission` WRITE;
/*!40000 ALTER TABLE `portal_contest_submission` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_contest_submission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_contest_vote`
--

DROP TABLE IF EXISTS `portal_contest_vote`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_contest_vote`
--

LOCK TABLES `portal_contest_vote` WRITE;
/*!40000 ALTER TABLE `portal_contest_vote` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_contest_vote` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_creator_certification`
--

DROP TABLE IF EXISTS `portal_creator_certification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_creator_certification` (
                                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                `user_id` bigint NOT NULL COMMENT '申请用户ID',
                                                `real_name` varchar(64) NOT NULL COMMENT '真实姓名',
                                                `cert_type` varchar(32) NOT NULL COMMENT '认证类型 identity/creator/expert',
                                                `cert_no` varchar(64) DEFAULT NULL COMMENT '证件号',
                                                `cert_image` varchar(500) DEFAULT NULL COMMENT '证件照URL',
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_creator_certification`
--

LOCK TABLES `portal_creator_certification` WRITE;
/*!40000 ALTER TABLE `portal_creator_certification` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_creator_certification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_creator_settlement`
--

DROP TABLE IF EXISTS `portal_creator_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_creator_settlement`
--

LOCK TABLES `portal_creator_settlement` WRITE;
/*!40000 ALTER TABLE `portal_creator_settlement` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_creator_settlement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_entity_tag`
--

DROP TABLE IF EXISTS `portal_entity_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_entity_tag` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `tag_id` bigint unsigned NOT NULL COMMENT '标签ID（引用 portal_tag.id）',
                                     `entity_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '实体类型（article/interview_question/interview_experience/interview_resume_template/book 等）',
                                     `entity_id` bigint unsigned NOT NULL COMMENT '实体ID',
                                     `sort` int DEFAULT '0' COMMENT '排序',
                                     `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_tag_entity` (`tag_id`,`entity_type`,`entity_id`),
                                     KEY `idx_entity` (`entity_type`,`entity_id`),
                                     KEY `idx_entity_create` (`entity_type`,`create_time`),
                                     KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通用实体标签关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_entity_tag`
--

LOCK TABLES `portal_entity_tag` WRITE;
/*!40000 ALTER TABLE `portal_entity_tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_entity_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_feed_event`
--

DROP TABLE IF EXISTS `portal_feed_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_feed_event` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `user_id` bigint NOT NULL COMMENT '事件发布者',
                                     `event_type` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT 'publish_article/publish_experience/new_column/checkin等',
                                     `target_type` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT 'article/experience/column/book等',
                                     `target_id` bigint NOT NULL COMMENT '目标对象ID',
                                     `title` varchar(256) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '目标标题',
                                     `summary` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '动态摘要',
                                     `cover` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '封面图',
                                     `created_time` datetime NOT NULL,
                                     `user_business_id` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '发布者业务主键（关联 portal_user.business_id）',
                                     `target_business_id` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '目标业务主键（多态：根据 target_type 关联不同父表）',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_time` (`user_id`,`created_time`),
                                     KEY `idx_type_time` (`event_type`,`created_time`),
                                     KEY `idx_user_bid` (`user_business_id`),
                                     KEY `idx_target_bid` (`target_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='动态事件流';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_feed_event`
--

LOCK TABLES `portal_feed_event` WRITE;
/*!40000 ALTER TABLE `portal_feed_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_feed_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_feed_inbox`
--

DROP TABLE IF EXISTS `portal_feed_inbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_feed_inbox` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `user_id` bigint NOT NULL COMMENT '接收者',
                                     `event_id` bigint NOT NULL COMMENT '动态事件ID',
                                     `created_time` datetime NOT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='动态收件箱';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_feed_inbox`
--

LOCK TABLES `portal_feed_inbox` WRITE;
/*!40000 ALTER TABLE `portal_feed_inbox` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_feed_inbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_feedback`
--

DROP TABLE IF EXISTS `portal_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_feedback` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
                                   `feedback_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '反馈类型：suggestion/bug/experience/other',
                                   `subject` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '反馈主题',
                                   `description` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '反馈详细描述',
                                   `contact` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系方式（可选）',
                                   `user_id` bigint DEFAULT NULL COMMENT '反馈人用户ID',
                                   `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '反馈人用户名（冗余）',
                                   `ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '反馈人IP',
                                   `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
                                   `handler` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理人',
                                   `handle_result` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理结果说明',
                                   `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                   `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_feedback_type` (`feedback_type`),
                                   KEY `idx_status` (`status`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_create_time` (`create_time`),
                                   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户意见反馈表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_feedback`
--

LOCK TABLES `portal_feedback` WRITE;
/*!40000 ALTER TABLE `portal_feedback` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_feedback` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_follow`
--

DROP TABLE IF EXISTS `portal_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_follow` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注ID',
                                 `follower_id` bigint NOT NULL COMMENT '关注者ID（门户用户ID）',
                                 `following_id` bigint NOT NULL COMMENT '被关注者ID（门户用户ID）',
                                 `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_follower_following` (`follower_id`,`following_id`),
                                 KEY `idx_follower_id` (`follower_id`),
                                 KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户关注表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_follow`
--

LOCK TABLES `portal_follow` WRITE;
/*!40000 ALTER TABLE `portal_follow` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_follow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_friend_link`
--

DROP TABLE IF EXISTS `portal_friend_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_friend_link` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '链接ID',
                                      `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '链接名称',
                                      `url` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '链接地址',
                                      `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '链接描述',
                                      `logo` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Logo URL',
                                      `sort` int DEFAULT '0' COMMENT '排序',
                                      `status` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态：0正常 1停用',
                                      `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户友情链接表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_friend_link`
--

LOCK TABLES `portal_friend_link` WRITE;
/*!40000 ALTER TABLE `portal_friend_link` DISABLE KEYS */;
INSERT INTO `portal_friend_link` VALUES (1,'中国作家网','https://www.chinawriter.com.cn','中国作家协会官方网站',NULL,1,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL),(2,'起点中文网','https://www.qidian.com','阅文集团旗下网站',NULL,2,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL),(3,'掘金','https://juejin.cn','帮助开发者成长的社区',NULL,3,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL);
/*!40000 ALTER TABLE `portal_friend_link` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_growth_log`
--

DROP TABLE IF EXISTS `portal_growth_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_growth_log`
--

LOCK TABLES `portal_growth_log` WRITE;
/*!40000 ALTER TABLE `portal_growth_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_growth_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_growth_rule`
--

DROP TABLE IF EXISTS `portal_growth_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_growth_rule`
--

LOCK TABLES `portal_growth_rule` WRITE;
/*!40000 ALTER TABLE `portal_growth_rule` DISABLE KEYS */;
INSERT INTO `portal_growth_rule` VALUES (1,'article','publish_article',50,3,'发布文章','0',1,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(2,'article','receive_like',2,0,'文章被点赞','0',2,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(3,'article','receive_bookmark',3,0,'文章被收藏','0',3,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(4,'article','receive_follow',5,0,'被关注','0',4,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(5,'article','article_featured',100,0,'文章被精选','0',5,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(6,'article','receive_comment',2,0,'文章被评论','0',6,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(7,'reading','finish_book',20,1,'完成阅读一本书','0',10,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(8,'reading','write_quote',15,0,'发布金句','0',11,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(9,'reading','create_booklist',20,0,'创建书单','0',12,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(10,'reading','quote_liked',5,0,'金句被点赞','0',13,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(11,'reading','booklist_liked',5,0,'书单被点赞','0',14,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(12,'reading','booklist_bookmarked',10,0,'书单被收藏','0',15,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(13,'interview','solve_question',10,20,'解题','0',20,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(14,'interview','write_note',15,0,'写笔记','0',21,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(15,'interview','note_adopted',50,0,'笔记被精选','0',22,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(16,'interview','publish_experience',30,0,'发布面经','0',23,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(17,'interview','experience_liked',2,0,'面经被点赞','0',24,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(18,'interview','experience_bookmarked',3,0,'面经被收藏','0',25,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(19,'all','daily_checkin',1,1,'每日签到','0',30,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(20,'all','daily_login',1,1,'每日登录','0',31,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),(21,'article','receive_tip',3,0,'文章/专栏被打赏','0',7,'','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),(22,'article','tip_others',1,3,'打赏他人','0',8,'','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),(23,'topic','create_topic',10,0,'发起话题','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(24,'topic','post_opinion',2,10,'发表观点','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(25,'topic','receive_topic_like',2,0,'话题被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(26,'topic','receive_post_like',2,0,'观点被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(27,'topic','receive_topic_comment',2,0,'话题被评论','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(28,'topic','receive_post_comment',2,0,'观点被评论','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(29,'topic','receive_comment_like',2,0,'评论被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),(30,'topic','topic_featured',50,0,'话题被精选','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL);
/*!40000 ALTER TABLE `portal_growth_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_help_article`
--

DROP TABLE IF EXISTS `portal_help_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_help_article` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
                                       `category_id` bigint NOT NULL COMMENT '分类ID',
                                       `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题标题',
                                       `content` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '答案内容（支持纯文本）',
                                       `view_count` int DEFAULT '0' COMMENT '查看次数',
                                       `like_count` int DEFAULT '0' COMMENT '点赞次数',
                                       `sort` int DEFAULT '0' COMMENT '排序（升序）',
                                       `is_featured` tinyint DEFAULT '0' COMMENT '是否精选：0=否 1=是',
                                       `status` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'published' COMMENT '状态：published/draft',
                                       `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                       `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_category_id` (`category_id`),
                                       KEY `idx_status` (`status`),
                                       KEY `idx_is_featured` (`is_featured`),
                                       KEY `idx_sort` (`sort`),
                                       KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帮助中心文章表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_help_article`
--

LOCK TABLES `portal_help_article` WRITE;
/*!40000 ALTER TABLE `portal_help_article` DISABLE KEYS */;
INSERT INTO `portal_help_article` VALUES (1,1,'如何发布文章？','点击页面右上角的\"创作\"按钮，进入文章编辑页面。填写标题、内容、分类、标签等信息后，点击\"发布文章\"按钮即可发布。\n\n支持 Markdown 和富文本两种编辑模式，可插入图片、代码块、链接等元素。',1250,89,1,1,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(2,1,'如何编辑或删除已发布的文章？','进入个人中心 → 我的文章，找到目标文章：\n\n1. 编辑：点击\"编辑\"按钮，修改内容后保存即可\n2. 删除：点击\"删除\"按钮，确认后文章将被永久删除\n\n注意：删除操作不可恢复，请谨慎操作。',890,56,2,0,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(3,1,'文章支持哪些编辑格式？','墨韵智库支持两种编辑模式：\n\n1. Markdown 模式：适合技术用户，支持代码高亮、表格、列表等\n2. 富文本模式：适合文学用户，所见即所得，支持图片、视频嵌入\n\n两种模式可在编辑页面顶部切换。',670,34,3,0,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(4,2,'如何注册账号？','点击页面右上角的\"注册\"按钮，填写用户名、邮箱、密码即可完成注册。\n\n注册后请完善个人资料，包括昵称、头像、个人简介等，有助于其他用户了解你。',1560,102,1,1,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(5,2,'如何修改密码？','进入个人中心 → 账号设置 → 修改密码，输入旧密码和新密码后保存即可。\n\n建议定期修改密码，使用包含字母、数字、特殊字符的强密码，提高账号安全性。',980,67,2,0,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(6,2,'忘记密码怎么办？','在登录页面点击\"忘记密码\"链接，输入注册邮箱，系统会发送重置链接到你的邮箱。\n\n点击邮件中的链接，设置新密码即可。链接有效期为 24 小时。',1340,78,3,1,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(7,2,'如何修改个人资料？','进入个人中心，点击\"编辑个人资料\"，在弹出的窗口中修改昵称、头像、个人简介等信息后保存即可。\n\n个人资料会展示在你的作者主页和文章详情页。',760,45,4,0,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(8,3,'如何关注其他用户？','在作者主页或文章详情页，点击作者头像或用户名进入作者主页，然后点击\"关注\"按钮即可。\n\n关注后，该用户的新文章会出现在你的动态中。',890,56,1,0,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(9,3,'如何点赞和收藏文章？','在文章详情页底部，可以找到点赞和收藏按钮：\n\n1. 点赞：点击爱心图标，再次点击取消\n2. 收藏：点击书签图标，可在个人中心 → 我的收藏查看\n\n点赞和收藏是对作者最好的支持。',1120,89,2,1,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(10,3,'如何发表评论？','在文章详情页底部评论区，输入你的评论内容后点击\"发表\"按钮即可。\n\n支持回复其他用户的评论，形成讨论串。请遵守社区规范，文明评论。',780,34,3,0,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(11,4,'文章审核需要多长时间？','文章提交后，通常会在 1-3 个工作日内完成审核。审核通过后文章即可公开发布。\n\n审核标准：内容原创、无违规信息、格式规范。如审核未通过，会通过站内信通知原因。',1450,95,1,1,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(12,4,'如何举报违规内容？','在文章详情页或用户主页，点击\"举报\"按钮，选择举报原因并填写说明后提交即可。\n\n举报类型包括：垃圾广告、色情低俗、违法违规、抄袭侵权、人身攻击等。我们会在 24 小时内处理。',670,45,2,0,'published','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0');
/*!40000 ALTER TABLE `portal_help_article` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_help_category`
--

DROP TABLE IF EXISTS `portal_help_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_help_category` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
                                        `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
                                        `icon` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标（lucide 图标名）',
                                        `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类描述',
                                        `sort` int DEFAULT '0' COMMENT '排序（升序）',
                                        `status` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'active' COMMENT '状态：active/inactive',
                                        `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                        `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                        `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_status` (`status`),
                                        KEY `idx_sort` (`sort`),
                                        KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帮助中心分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_help_category`
--

LOCK TABLES `portal_help_category` WRITE;
/*!40000 ALTER TABLE `portal_help_category` DISABLE KEYS */;
INSERT INTO `portal_help_category` VALUES (1,'发布与编辑','BookOpen','文章发布、编辑、删除等操作指南',1,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(2,'账号与安全','HelpCircle','登录、注册、密码、安全设置',2,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(3,'互动功能','MessageSquare','评论、点赞、关注等互动功能',3,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),(4,'社区规则','Shield','使用规范、违规处理、隐私政策',4,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0');
/*!40000 ALTER TABLE `portal_help_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_attempt`
--

DROP TABLE IF EXISTS `portal_interview_attempt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_attempt`
--

LOCK TABLES `portal_interview_attempt` WRITE;
/*!40000 ALTER TABLE `portal_interview_attempt` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_attempt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_bookmark`
--

DROP TABLE IF EXISTS `portal_interview_bookmark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_bookmark`
--

LOCK TABLES `portal_interview_bookmark` WRITE;
/*!40000 ALTER TABLE `portal_interview_bookmark` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_bookmark` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_category`
--

DROP TABLE IF EXISTS `portal_interview_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_category`
--

LOCK TABLES `portal_interview_category` WRITE;
/*!40000 ALTER TABLE `portal_interview_category` DISABLE KEYS */;
INSERT INTO `portal_interview_category` VALUES (1,'算法与数据结构','algorithm','算法题、数据结构相关面试题','fa-code',1,150,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),(2,'系统设计','system-design','系统架构设计、分布式系统等面试题','fa-sitemap',2,60,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),(3,'前端开发','frontend','JavaScript、CSS、Vue、React等前端技术面试题','fa-laptop-code',3,120,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),(4,'后端开发','backend','Java、Python、Go等后端技术面试题','fa-server',4,130,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),(5,'数据库','database','MySQL、Redis等数据库相关面试题','fa-database',5,80,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0');
/*!40000 ALTER TABLE `portal_interview_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_comment`
--

DROP TABLE IF EXISTS `portal_interview_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_comment`
--

LOCK TABLES `portal_interview_comment` WRITE;
/*!40000 ALTER TABLE `portal_interview_comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_comment_like`
--

DROP TABLE IF EXISTS `portal_interview_comment_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_interview_comment_like` (
                                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                 `comment_id` bigint NOT NULL COMMENT '评论ID',
                                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                                 PRIMARY KEY (`id`),
                                                 UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经评论点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_comment_like`
--

LOCK TABLES `portal_interview_comment_like` WRITE;
/*!40000 ALTER TABLE `portal_interview_comment_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_comment_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_company`
--

DROP TABLE IF EXISTS `portal_interview_company`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_company`
--

LOCK TABLES `portal_interview_company` WRITE;
/*!40000 ALTER TABLE `portal_interview_company` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_company` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_experience`
--

DROP TABLE IF EXISTS `portal_interview_experience`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_experience`
--

LOCK TABLES `portal_interview_experience` WRITE;
/*!40000 ALTER TABLE `portal_interview_experience` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_experience` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_experience_like`
--

DROP TABLE IF EXISTS `portal_interview_experience_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_experience_like`
--

LOCK TABLES `portal_interview_experience_like` WRITE;
/*!40000 ALTER TABLE `portal_interview_experience_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_experience_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_position`
--

DROP TABLE IF EXISTS `portal_interview_position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_interview_position` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位编码（如 java_backend）',
                                             `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位名称（如 Java后端工程师）',
                                             `industry` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属行业（如 互联网/金融/制造）',
                                             `level` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '岗位级别（junior/mid/senior）',
                                             `required_skills` text COLLATE utf8mb4_unicode_ci COMMENT '必备技能 JSON 数组（如 ["Spring","MySQL","Redis"]，与 portal_tag.name 对齐）',
                                             `hot_companies` text COLLATE utf8mb4_unicode_ci COMMENT '热门公司 JSON 数组（如 ["阿里","腾讯","字节"]）',
                                             `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '岗位描述',
                                             `sort` int DEFAULT '0' COMMENT '排序',
                                             `status` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT 'active' COMMENT '状态 active/inactive',
                                             `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_code` (`code`),
                                             KEY `idx_status_sort` (`status`,`sort`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试岗位字典表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_position`
--

LOCK TABLES `portal_interview_position` WRITE;
/*!40000 ALTER TABLE `portal_interview_position` DISABLE KEYS */;
INSERT INTO `portal_interview_position` VALUES (1,'java_backend','Java后端工程师','互联网','mid','[\"Java\",\"Spring\",\"SpringBoot\",\"MyBatis\",\"MySQL\",\"Redis\",\"MQ\",\"JVM\",\"并发编程\",\"分布式\",\"微服务\",\"设计模式\"]','[\"阿里\",\"腾讯\",\"字节跳动\",\"美团\",\"京东\",\"百度\",\"拼多多\",\"网易\",\"滴滴\",\"快手\"]','Java 后端工程师岗位，重点考察 Java 基础、Spring 全家桶、MySQL/Redis、分布式与微服务、JVM 与并发编程',1,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0'),(2,'frontend','前端工程师','互联网','mid','[\"JavaScript\",\"TypeScript\",\"Vue\",\"React\",\"HTML\",\"CSS\",\"Node.js\",\"Webpack\",\"Vite\",\"性能优化\",\"浏览器原理\",\"HTTP\"]','[\"阿里\",\"腾讯\",\"字节跳动\",\"美团\",\"京东\",\"百度\",\"网易\",\"小米\",\"Shopee\",\"滴滴\"]','前端工程师岗位，重点考察 JS/TS 基础、Vue/React 框架、工程化、浏览器原理、性能优化、HTTP 与网络',2,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0'),(3,'algorithm','算法工程师','互联网','mid','[\"算法\",\"数据结构\",\"动态规划\",\"图论\",\"字符串\",\"数组\",\"链表\",\"树\",\"递归\",\"排序\",\"机器学习\",\"深度学习\",\"数学\"]','[\"阿里\",\"腾讯\",\"字节跳动\",\"百度\",\"美团\",\"快手\",\"小红书\",\"华为\",\"商汤\",\"旷视\"]','算法工程师岗位，重点考察数据结构与算法、动态规划、图论、字符串算法、机器学习与深度学习基础',3,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0');
/*!40000 ALTER TABLE `portal_interview_position` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_question`
--

DROP TABLE IF EXISTS `portal_interview_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_question`
--

LOCK TABLES `portal_interview_question` WRITE;
/*!40000 ALTER TABLE `portal_interview_question` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_question` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_question_company`
--

DROP TABLE IF EXISTS `portal_interview_question_company`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_question_company`
--

LOCK TABLES `portal_interview_question_company` WRITE;
/*!40000 ALTER TABLE `portal_interview_question_company` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_question_company` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_question_like`
--

DROP TABLE IF EXISTS `portal_interview_question_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_question_like`
--

LOCK TABLES `portal_interview_question_like` WRITE;
/*!40000 ALTER TABLE `portal_interview_question_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_question_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_resume_template`
--

DROP TABLE IF EXISTS `portal_interview_resume_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_resume_template`
--

LOCK TABLES `portal_interview_resume_template` WRITE;
/*!40000 ALTER TABLE `portal_interview_resume_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_resume_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_resume_template_like`
--

DROP TABLE IF EXISTS `portal_interview_resume_template_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_resume_template_like`
--

LOCK TABLES `portal_interview_resume_template_like` WRITE;
/*!40000 ALTER TABLE `portal_interview_resume_template_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_resume_template_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_interview_submission`
--

DROP TABLE IF EXISTS `portal_interview_submission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_submission`
--

LOCK TABLES `portal_interview_submission` WRITE;
/*!40000 ALTER TABLE `portal_interview_submission` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_submission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_like`
--

DROP TABLE IF EXISTS `portal_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_like` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
                               `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                               `article_id` bigint NOT NULL COMMENT '文章ID',
                               `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                               `user_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户业务主键（关联 portal_user.business_id）',
                               `article_business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文章业务主键（关联 portal_article.business_id）',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
                               KEY `idx_user_id` (`user_id`),
                               KEY `idx_article_id` (`article_id`),
                               KEY `idx_user_bid` (`user_business_id`),
                               KEY `idx_article_bid` (`article_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户点赞表（文章）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_like`
--

LOCK TABLES `portal_like` WRITE;
/*!40000 ALTER TABLE `portal_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_message`
--

DROP TABLE IF EXISTS `portal_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_message` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `session_id` bigint NOT NULL COMMENT '会话ID',
                                  `sender_id` bigint NOT NULL COMMENT '发送者',
                                  `sender_type` varchar(16) COLLATE utf8mb4_bin NOT NULL DEFAULT 'portal' COMMENT '发送者类型 portal/sys',
                                  `receiver_id` bigint NOT NULL COMMENT '接收者',
                                  `receiver_type` varchar(16) COLLATE utf8mb4_bin NOT NULL DEFAULT 'portal' COMMENT '接收者类型 portal/sys',
                                  `content` text COLLATE utf8mb4_bin NOT NULL COMMENT '消息内容',
                                  `msg_type` varchar(16) COLLATE utf8mb4_bin DEFAULT 'text' COMMENT 'text/image/file',
                                  `is_read` tinyint DEFAULT '0' COMMENT '是否已读',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_session_time` (`session_id`,`create_time`),
                                  KEY `idx_receiver_type_read` (`receiver_id`,`receiver_type`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='私信消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_message`
--

LOCK TABLES `portal_message` WRITE;
/*!40000 ALTER TABLE `portal_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_message_session`
--

DROP TABLE IF EXISTS `portal_message_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_message_session` (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `user_a` bigint NOT NULL COMMENT '用户A（较小ID）',
                                          `user_a_type` varchar(16) COLLATE utf8mb4_bin NOT NULL DEFAULT 'portal' COMMENT 'A方用户类型 portal/sys',
                                          `user_b` bigint NOT NULL COMMENT '用户B（较大ID）',
                                          `user_b_type` varchar(16) COLLATE utf8mb4_bin NOT NULL DEFAULT 'portal' COMMENT 'B方用户类型 portal/sys',
                                          `last_message_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
                                          `last_message_content` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '最后消息内容预览',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='私信会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_message_session`
--

LOCK TABLES `portal_message_session` WRITE;
/*!40000 ALTER TABLE `portal_message_session` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_message_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_mock_interview`
--

DROP TABLE IF EXISTS `portal_mock_interview`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_mock_interview`
--

LOCK TABLES `portal_mock_interview` WRITE;
/*!40000 ALTER TABLE `portal_mock_interview` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_mock_interview` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_mock_interview_qa`
--

DROP TABLE IF EXISTS `portal_mock_interview_qa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `portal_mock_interview_qa`


LOCK TABLES `portal_mock_interview_qa` WRITE;
/*!40000 ALTER TABLE `portal_mock_interview_qa` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_mock_interview_qa` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_notification_bak`
--

DROP TABLE IF EXISTS `portal_notification_bak`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_notification_bak` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
                                           `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                           `type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型：comment/like/follow/system/order',
                                           `title` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '通知标题',
                                           `content` text COLLATE utf8mb4_general_ci COMMENT '通知内容',
                                           `data` json DEFAULT NULL COMMENT '通知数据（JSON格式）',
                                           `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
                                           `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                           `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                           PRIMARY KEY (`id`),
                                           KEY `idx_user_id` (`user_id`),
                                           KEY `idx_type` (`type`),
                                           KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户通知表';
/*!40101 SET character_set_client = @saved_cs_client */;



DROP TABLE IF EXISTS `portal_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_order` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                                `order_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单号',
                                `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                `type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型：vip/recharge/product',
                                `product_id` bigint DEFAULT NULL COMMENT '商品ID',
                                `amount` decimal(10,2) NOT NULL COMMENT '金额',
                                `status` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'pending' COMMENT '状态：pending/paid/cancelled/refunded',
                                `pay_method` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支付方式：wechat/alipay',
                                `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
                                `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_order_no` (`order_no`),
                                KEY `idx_user_id` (`user_id`),
                                KEY `idx_type` (`type`),
                                KEY `idx_status` (`status`),
                                KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_order`
--

LOCK TABLES `portal_order` WRITE;
/*!40000 ALTER TABLE `portal_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_pk_challenge`
--

DROP TABLE IF EXISTS `portal_pk_challenge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_pk_challenge`
--

LOCK TABLES `portal_pk_challenge` WRITE;
/*!40000 ALTER TABLE `portal_pk_challenge` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_pk_challenge` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_reading_preference`
--

DROP TABLE IF EXISTS `portal_reading_preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_reading_preference` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `user_id` bigint NOT NULL COMMENT '用户ID',
                                             `font_size` int DEFAULT '18' COMMENT '正文字号（px，12-32）',
                                             `line_height` decimal(3,1) DEFAULT '1.8' COMMENT '行距（倍，1.2-3.0）',
                                             `theme` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'default' COMMENT '阅读主题：default=跟随 / light=亮色 / dark=暗色 / sepia=护眼黄',
                                             `font_family` varchar(50) COLLATE utf8mb4_general_ci DEFAULT 'system' COMMENT '字体：system=系统默认 / serif=衬线 / song=宋体 / hei=黑体',
                                             `letter_spacing` decimal(3,1) DEFAULT '0.0' COMMENT '字间距（px，-1.0-5.0）',
                                             `paragraph_spacing` decimal(4,1) DEFAULT '1.2' COMMENT '段间距（em，0.5-5.0）',
                                             `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_user_id` (`user_id`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户阅读偏好表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_reading_preference`


LOCK TABLES `portal_reading_preference` WRITE;
/*!40000 ALTER TABLE `portal_reading_preference` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_reading_preference` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `portal_reading_progress`


DROP TABLE IF EXISTS `portal_reading_progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `portal_reading_progress`


LOCK TABLES `portal_reading_progress` WRITE;
/*!40000 ALTER TABLE `portal_reading_progress` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_reading_progress` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `portal_report`


DROP TABLE IF EXISTS `portal_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_report` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '举报ID',
                                 `report_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报类型：spam/inappropriate/infringement/fraud/other',
                                 `target_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '举报目标URL',
                                 `target_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '举报目标类型：comment/article/user 等，为空表示通用举报（仅 target_url）',
                                 `target_id` bigint DEFAULT NULL COMMENT '举报目标ID（评论/文章/用户ID，配合 target_type 使用）',
                                 `description` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问题描述',
                                 `contact` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系方式（可选）',
                                 `images` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片证据（JSON数组，最多3张）',
                                 `user_id` bigint DEFAULT NULL COMMENT '举报人用户ID',
                                 `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '举报人用户名（冗余）',
                                 `ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '举报人IP',
                                 `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
                                 `handler` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理人',
                                 `handle_result` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理结果说明',
                                 `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 `user_business_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '举报人业务主键（关联 portal_user.business_id）',
                                 `target_business_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标业务主键（多态：根据 target_type 关联不同父表）',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_report_type` (`report_type`),
                                 KEY `idx_status` (`status`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_create_time` (`create_time`),
                                 KEY `idx_target` (`target_type`,`target_id`),
                                 KEY `idx_del_flag` (`del_flag`),
                                 KEY `idx_user_bid` (`user_business_id`),
                                 KEY `idx_target_bid` (`target_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户举报记录表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `portal_report`


LOCK TABLES `portal_report` WRITE;
/*!40000 ALTER TABLE `portal_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_report` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `portal_shop_exchange`


DROP TABLE IF EXISTS `portal_shop_exchange`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `portal_shop_exchange`


LOCK TABLES `portal_shop_exchange` WRITE;
/*!40000 ALTER TABLE `portal_shop_exchange` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_shop_exchange` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `portal_shop_item`


DROP TABLE IF EXISTS `portal_shop_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_shop_item`
--

LOCK TABLES `portal_shop_item` WRITE;
/*!40000 ALTER TABLE `portal_shop_item` DISABLE KEYS */;
INSERT INTO `portal_shop_item` VALUES (1,'7天VIP体验卡','兑换后获得 7 天 VIP 会员',NULL,'virtual',200,-1,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(2,'30天VIP会员','兑换后获得 30 天 VIP 会员',NULL,'virtual',800,-1,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(3,'积分换余额','500 积分兑换 1 元钱包余额',NULL,'virtual',500,-1,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(4,'墨韵定制笔记本','限量定制笔记本，实物寄送',NULL,'physical',2000,100,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(5,'专属头像框','稀有专属头像框特权',NULL,'virtual',300,-1,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
/*!40000 ALTER TABLE `portal_shop_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_study_plan`
--

DROP TABLE IF EXISTS `portal_study_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_study_plan`
--

LOCK TABLES `portal_study_plan` WRITE;
/*!40000 ALTER TABLE `portal_study_plan` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_study_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_study_plan_log`
--

DROP TABLE IF EXISTS `portal_study_plan_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_study_plan_log`
--

LOCK TABLES `portal_study_plan_log` WRITE;
/*!40000 ALTER TABLE `portal_study_plan_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_study_plan_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_tag`
--

DROP TABLE IF EXISTS `portal_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_tag` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
                              `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签名称',
                              `slug` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签别名',
                              `sort` int DEFAULT '0' COMMENT '排序',
                              `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                              `module` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属模块（article/interview_question/interview_experience/interview_resume_template 等，null 表示通用）',
                              `reference_count` bigint unsigned DEFAULT '0' COMMENT '被引用次数（冗余计数列，绑定/解绑时同步维护）',
                              `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                              `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                              `business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务主键（前缀tag_）',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_name` (`name`),
                              UNIQUE KEY `uk_business_id` (`business_id`),
                              KEY `idx_slug` (`slug`),
                              KEY `idx_module` (`module`),
                              KEY `idx_reference_count` (`reference_count` DESC),
                              KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_tag`
--


--
-- Table structure for table `portal_task`
--

DROP TABLE IF EXISTS `portal_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_task`
--

LOCK TABLES `portal_task` WRITE;
/*!40000 ALTER TABLE `portal_task` DISABLE KEYS */;
INSERT INTO `portal_task` VALUES (1,'daily_checkin','每日签到','每天签到一次，保持活跃','daily',10,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(2,'daily_publish','每日发文','每日发布 1 篇文章','daily',20,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(3,'daily_comment','每日互动','每日评论 3 次','daily',15,3,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(4,'daily_like','每日点赞','每日点赞 5 次','daily',10,5,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(5,'daily_solve','每日刷题','每日解答 3 道面试题','daily',20,3,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(6,'first_article','初露锋芒','发布第一篇文章','achievement',50,1,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0'),(7,'solve_50','刷题能手','累计解答 50 道面试题','achievement',200,50,NULL,'active','admin','2026-07-28 16:29:45','','2026-07-28 16:29:45',NULL,'0');
/*!40000 ALTER TABLE `portal_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_tip_order`
--

DROP TABLE IF EXISTS `portal_tip_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
                                    `user_business_id` varchar(32) DEFAULT NULL COMMENT '打赏者业务主键（关联 portal_user.business_id）',
                                    `author_business_id` varchar(32) DEFAULT NULL COMMENT '被打赏者业务主键（关联 portal_user.business_id）',
                                    `target_business_id` varchar(32) DEFAULT NULL COMMENT '目标业务主键（多态：根据 target_type 关联不同父表）',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_author` (`author_id`),
                                    KEY `idx_target` (`target_type`,`target_id`),
                                    KEY `idx_user` (`user_id`),
                                    KEY `idx_user_bid` (`user_business_id`),
                                    KEY `idx_author_bid` (`author_business_id`),
                                    KEY `idx_target_bid` (`target_business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打赏订单（复用为付费阅读购买记录，target_type=article_paid）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_tip_order`
--

LOCK TABLES `portal_tip_order` WRITE;
/*!40000 ALTER TABLE `portal_tip_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_tip_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_topic`
--

DROP TABLE IF EXISTS `portal_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_topic`
--

LOCK TABLES `portal_topic` WRITE;
/*!40000 ALTER TABLE `portal_topic` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_topic` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_topic_comment`
--

DROP TABLE IF EXISTS `portal_topic_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_topic_comment`
--

LOCK TABLES `portal_topic_comment` WRITE;
/*!40000 ALTER TABLE `portal_topic_comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_topic_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_topic_comment_like`
--

DROP TABLE IF EXISTS `portal_topic_comment_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_topic_comment_like` (
                                             `id` bigint NOT NULL AUTO_INCREMENT,
                                             `comment_id` bigint NOT NULL,
                                             `user_id` bigint NOT NULL,
                                             `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`),
                                             KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题评论点赞';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_topic_comment_like`
--

LOCK TABLES `portal_topic_comment_like` WRITE;
/*!40000 ALTER TABLE `portal_topic_comment_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_topic_comment_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_topic_like`
--

DROP TABLE IF EXISTS `portal_topic_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_topic_like` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `topic_id` bigint NOT NULL,
                                     `user_id` bigint NOT NULL,
                                     `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_topic_user` (`topic_id`,`user_id`),
                                     KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题点赞';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_topic_like`
--

LOCK TABLES `portal_topic_like` WRITE;
/*!40000 ALTER TABLE `portal_topic_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_topic_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_topic_post`
--

DROP TABLE IF EXISTS `portal_topic_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_topic_post`
--

LOCK TABLES `portal_topic_post` WRITE;
/*!40000 ALTER TABLE `portal_topic_post` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_topic_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_topic_post_like`
--

DROP TABLE IF EXISTS `portal_topic_post_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_topic_post_like` (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `post_id` bigint NOT NULL,
                                          `user_id` bigint NOT NULL,
                                          `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
                                          KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题观点点赞';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_topic_post_like`
--

LOCK TABLES `portal_topic_post_like` WRITE;
/*!40000 ALTER TABLE `portal_topic_post_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_topic_post_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_user`
--

DROP TABLE IF EXISTS `portal_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_user` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                               `user_id` bigint DEFAULT NULL COMMENT '关联后台用户ID',
                               `username` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
                               `nickname` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '昵称',
                               `email` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
                               `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
                               `password` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码',
                               `avatar` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像URL',
                               `bio` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '个人简介',
                               `position` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职位',
                               `wechat` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信号',
                               `gender` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '性别：male-男，female-女，other-其他',
                               `birthday` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生日：YYYY-MM-DD格式',
                               `location` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所在城市：如北京市',
                               `website` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '个人网站URL',
                               `github` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'GitHub用户名或完整URL',
                               `company` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '公司名称',
                               `school` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学校名称',
                               `language` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '语言偏好：zh-CN，en-US等',
                               `timezone` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '时区：如Asia/Shanghai',
                               `notify_like` tinyint(1) DEFAULT '1' COMMENT '是否接收点赞通知',
                               `notify_comment` tinyint(1) DEFAULT '1' COMMENT '是否接收评论通知',
                               `notify_follow` tinyint(1) DEFAULT '1' COMMENT '是否接收关注通知',
                               `notify_system` tinyint(1) DEFAULT '1' COMMENT '是否接收系统通知',
                               `privacy_follow` tinyint(1) DEFAULT '1' COMMENT '是否允许被关注',
                               `privacy_bookmark` tinyint(1) DEFAULT '1' COMMENT '是否公开收藏夹',
                               `privacy_email` tinyint(1) DEFAULT '0' COMMENT '是否公开邮箱',
                               `privacy_phone` tinyint(1) DEFAULT '0' COMMENT '是否公开手机号',
                               `role` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'user' COMMENT '角色：user/admin',
                               `is_certified_creator` tinyint NOT NULL DEFAULT '0' COMMENT '是否认证创作者：0 否/1 是',
                               `vip_expire_at` datetime DEFAULT NULL COMMENT 'VIP过期时间',
                               `is_phone_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证手机号',
                               `is_wechat_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证微信',
                               `two_factor_enabled` tinyint(1) DEFAULT '0' COMMENT '是否开启两步验证',
                               `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
                               `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                               `login_ip` varchar(128) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '最后登录IP',
                               `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
                               `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                               `business_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务主键（前缀usr_，TRUNCATE后仍可关联子表）',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_username` (`username`),
                               UNIQUE KEY `uk_business_id` (`business_id`),
                               KEY `idx_user_id` (`user_id`),
                               KEY `idx_email` (`email`),
                               KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_user`
--

LOCK TABLES `portal_user` WRITE;
/*!40000 ALTER TABLE `portal_user` DISABLE KEYS */;
INSERT INTO `portal_user` VALUES (1,NULL,'admin','墨韵管理员','admin@moyun.com','13800138000','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O','https://api.dicebear.com/7.x/avataaars/svg?seed=admin','墨韵智库管理员，致力于打造优质内容社区','产品经理',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,1,1,1,1,0,0,'admin',0,NULL,0,0,0,'0','0','',NULL,'admin','2026-07-28 15:45:54','','2026-07-28 16:55:33',NULL,'usr_0000000000001_751EAD'),(2,NULL,'zhangsan','张三','zhangsan@moyun.com','13800138001','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O','https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan','热爱技术，喜欢分享，前端开发工程师','前端工程师',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,1,1,1,1,0,0,'user',0,NULL,0,0,0,'0','0','',NULL,'admin','2026-07-28 15:45:54','','2026-07-28 16:55:33',NULL,'usr_0000000000002_076A93'),(3,NULL,'lisi','李四','lisi@moyun.com','13800138002','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O','https://api.dicebear.com/7.x/avataaars/svg?seed=lisi','Java后端开发，专注微服务架构','后端工程师',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,1,1,1,1,0,0,'user',0,NULL,0,0,0,'0','0','',NULL,'admin','2026-07-28 15:45:54','','2026-07-28 16:55:33',NULL,'usr_0000000000003_C5B8D9'),(5,NULL,'19987671567',NULL,'19987671567',NULL,'$2a$10$KKyL9ZExeF8YV6Mz1ro5jeTZU70gGh9r5Yu6OdUcCno5VZCjy6S52',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,1,1,1,1,0,0,'user',0,NULL,0,0,0,'0','0','',NULL,'','2026-07-28 16:57:17','','2026-07-28 16:57:17',NULL,'usr_1785229037604_W3AnxV');
/*!40000 ALTER TABLE `portal_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_user_badge`
--

DROP TABLE IF EXISTS `portal_user_badge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_user_badge`
--

LOCK TABLES `portal_user_badge` WRITE;
/*!40000 ALTER TABLE `portal_user_badge` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_user_badge` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_user_growth`
--

DROP TABLE IF EXISTS `portal_user_growth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_user_growth`
--

LOCK TABLES `portal_user_growth` WRITE;
/*!40000 ALTER TABLE `portal_user_growth` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_user_growth` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_user_resume`
--

DROP TABLE IF EXISTS `portal_user_resume`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_user_resume`
--

LOCK TABLES `portal_user_resume` WRITE;
/*!40000 ALTER TABLE `portal_user_resume` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_user_resume` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_user_stats`
--

DROP TABLE IF EXISTS `portal_user_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_user_stats`
--

LOCK TABLES `portal_user_stats` WRITE;
/*!40000 ALTER TABLE `portal_user_stats` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_user_stats` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_user_task`
--

DROP TABLE IF EXISTS `portal_user_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_user_task`
--

LOCK TABLES `portal_user_task` WRITE;
/*!40000 ALTER TABLE `portal_user_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_user_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_vip_package`
--

DROP TABLE IF EXISTS `portal_vip_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_vip_package` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
                                      `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '套餐名称',
                                      `price` decimal(10,2) NOT NULL COMMENT '价格',
                                      `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
                                      `duration` int NOT NULL COMMENT '有效期（天）',
                                      `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '套餐描述',
                                      `features` json DEFAULT NULL COMMENT '功能列表（JSON数组）',
                                      `popular` tinyint(1) DEFAULT '0' COMMENT '是否热门',
                                      `sort` int DEFAULT '0' COMMENT '排序',
                                      `status` varchar(20) COLLATE utf8mb4_general_ci DEFAULT 'active' COMMENT '状态：active/inactive',
                                      `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                      `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_status` (`status`),
                                      KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户VIP套餐表';
/*!40101 SET character_set_client = @saved_cs_client */;



DROP TABLE IF EXISTS `portal_wallet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_wallet` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
                                 `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                 `balance` decimal(10,2) DEFAULT '0.00' COMMENT '余额',
                                 `frozen_balance` decimal(10,2) DEFAULT '0.00' COMMENT '冻结余额',
                                 `total_recharge` decimal(10,2) DEFAULT '0.00' COMMENT '累计充值',
                                 `total_withdraw` decimal(10,2) DEFAULT '0.00' COMMENT '累计提现',
                                 `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_id` (`user_id`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户钱包表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_wallet`
--

LOCK TABLES `portal_wallet` WRITE;
/*!40000 ALTER TABLE `portal_wallet` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_wallet` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_wallet_transaction`
--

DROP TABLE IF EXISTS `portal_wallet_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_wallet_transaction` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '交易ID',
                                             `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                             `type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型：recharge/consume/refund/withdraw',
                                             `amount` decimal(10,2) NOT NULL COMMENT '金额',
                                             `balance_before` decimal(10,2) NOT NULL COMMENT '交易前余额',
                                             `balance_after` decimal(10,2) NOT NULL COMMENT '交易后余额',
                                             `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
                                             `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
                                             `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                             PRIMARY KEY (`id`),
                                             KEY `idx_user_id` (`user_id`),
                                             KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户钱包交易记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_wallet_transaction`
--

LOCK TABLES `portal_wallet_transaction` WRITE;
/*!40000 ALTER TABLE `portal_wallet_transaction` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_wallet_transaction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_writing_contest`
--

DROP TABLE IF EXISTS `portal_writing_contest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_writing_contest`
--

LOCK TABLES `portal_writing_contest` WRITE;
/*!40000 ALTER TABLE `portal_writing_contest` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_writing_contest` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_writing_prompt`
--

DROP TABLE IF EXISTS `portal_writing_prompt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_writing_prompt`
--

LOCK TABLES `portal_writing_prompt` WRITE;
/*!40000 ALTER TABLE `portal_writing_prompt` DISABLE KEYS */;
INSERT INTO `portal_writing_prompt` VALUES (1,'2026-07-22','一封信','请以书信的形式，写一封给十年后自己的信。可以是忠告、可以是期许，也可以是当下的困惑。','生活','2026-07-28 16:27:46','0'),(2,'2026-07-23','雨夜','描述一个雨夜的场景：一个未眠的人，一扇半开的窗，一段未说完的话。','情感','2026-07-28 16:27:46','0'),(3,'2026-07-24','通勤路上','记录一次通勤路上的所见所闻。一个陌生人、一段广播、一闪而过的风景，都可能成为故事的开端。','生活','2026-07-28 16:27:46','0'),(4,'2026-07-25','如果时间可以暂停','假如你拥有让时间暂停 30 秒的能力，你会用它做什么？请写一个具体的场景。','虚构','2026-07-28 16:27:46','0'),(5,'2026-07-26','面试官的沉默','一场面试中，面试官在某个问题后沉默了 10 秒。请描写那 10 秒里应聘者的内心活动。','职场','2026-07-28 16:27:46','0'),(6,'2026-07-27','一件旧物','选择一件你保留多年的旧物，写它背后的故事。它从哪里来，又见证了什么？','情感','2026-07-28 16:27:46','0'),(7,'2026-07-28','此刻的光','观察此刻你所在空间里的光：它的颜色、强度、来源、投下的影子。用 300 字描绘它，并赋予它一种情绪。','哲思','2026-07-28 16:27:46','0');
/*!40000 ALTER TABLE `portal_writing_prompt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_wrong_question`
--

DROP TABLE IF EXISTS `portal_wrong_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_wrong_question`
--

LOCK TABLES `portal_wrong_question` WRITE;
/*!40000 ALTER TABLE `portal_wrong_question` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_wrong_question` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_blob_triggers`
--

DROP TABLE IF EXISTS `qrtz_blob_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_blob_triggers` (
                                      `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                                      `trigger_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                      `trigger_group` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                      `blob_data` blob COMMENT '存放持久化Trigger对象',
                                      PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                      CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Blob类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_blob_triggers`
--

LOCK TABLES `qrtz_blob_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_blob_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_blob_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_calendars`
--

DROP TABLE IF EXISTS `qrtz_calendars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_calendars` (
                                  `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                                  `calendar_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT '日历名称',
                                  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
                                  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='日历信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_calendars`
--

LOCK TABLES `qrtz_calendars` WRITE;
/*!40000 ALTER TABLE `qrtz_calendars` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_calendars` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_cron_triggers`
--

DROP TABLE IF EXISTS `qrtz_cron_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_cron_triggers` (
                                      `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                                      `trigger_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                      `trigger_group` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                      `cron_expression` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'cron表达式',
                                      `time_zone_id` varchar(80) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '时区',
                                      PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                      CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Cron类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_cron_triggers`
--

LOCK TABLES `qrtz_cron_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_cron_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_cron_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_fired_triggers`
--

DROP TABLE IF EXISTS `qrtz_fired_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_fired_triggers` (
                                       `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                                       `entry_id` varchar(95) COLLATE utf8mb4_bin NOT NULL COMMENT '调度器实例id',
                                       `trigger_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                       `trigger_group` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                       `instance_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT '调度器实例名',
                                       `fired_time` bigint NOT NULL COMMENT '触发的时间',
                                       `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
                                       `priority` int NOT NULL COMMENT '优先级',
                                       `state` varchar(16) COLLATE utf8mb4_bin NOT NULL COMMENT '状态',
                                       `job_name` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '任务名称',
                                       `job_group` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '任务组名',
                                       `is_nonconcurrent` varchar(1) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '是否并发',
                                       `requests_recovery` varchar(1) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '是否接受恢复执行',
                                       PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='已触发的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_fired_triggers`
--

LOCK TABLES `qrtz_fired_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_fired_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_fired_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_job_details`
--

DROP TABLE IF EXISTS `qrtz_job_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_job_details` (
                                    `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                                    `job_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT '任务名称',
                                    `job_group` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT '任务组名',
                                    `description` varchar(250) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '相关介绍',
                                    `job_class_name` varchar(250) COLLATE utf8mb4_bin NOT NULL COMMENT '执行任务类名称',
                                    `is_durable` varchar(1) COLLATE utf8mb4_bin NOT NULL COMMENT '是否持久化',
                                    `is_nonconcurrent` varchar(1) COLLATE utf8mb4_bin NOT NULL COMMENT '是否并发',
                                    `is_update_data` varchar(1) COLLATE utf8mb4_bin NOT NULL COMMENT '是否更新数据',
                                    `requests_recovery` varchar(1) COLLATE utf8mb4_bin NOT NULL COMMENT '是否接受恢复执行',
                                    `job_data` blob COMMENT '存放持久化job对象',
                                    PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='任务详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_job_details`
--

LOCK TABLES `qrtz_job_details` WRITE;
/*!40000 ALTER TABLE `qrtz_job_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_job_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_locks`
--

DROP TABLE IF EXISTS `qrtz_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_locks` (
                              `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                              `lock_name` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '悲观锁名称',
                              PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='存储的悲观锁信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_locks`
--

LOCK TABLES `qrtz_locks` WRITE;
/*!40000 ALTER TABLE `qrtz_locks` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_locks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_paused_trigger_grps`
--

DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_paused_trigger_grps` (
                                            `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                                            `trigger_group` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                            PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='暂停的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_paused_trigger_grps`
--

LOCK TABLES `qrtz_paused_trigger_grps` WRITE;
/*!40000 ALTER TABLE `qrtz_paused_trigger_grps` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_paused_trigger_grps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_scheduler_state`
--

DROP TABLE IF EXISTS `qrtz_scheduler_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_scheduler_state` (
                                        `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                                        `instance_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT '实例名称',
                                        `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
                                        `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
                                        PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='调度器状态表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_scheduler_state`
--

LOCK TABLES `qrtz_scheduler_state` WRITE;
/*!40000 ALTER TABLE `qrtz_scheduler_state` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_scheduler_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_simple_triggers`
--

DROP TABLE IF EXISTS `qrtz_simple_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simple_triggers` (
                                        `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                                        `trigger_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                        `trigger_group` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                        `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
                                        `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
                                        `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
                                        PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                        CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='简单触发器的信息表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `qrtz_simple_triggers`


LOCK TABLES `qrtz_simple_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_simple_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_simple_triggers` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `qrtz_simprop_triggers`


DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simprop_triggers` (
                                         `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                                         `trigger_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
                                         `trigger_group` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
                                         `str_prop_1` varchar(512) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
                                         `str_prop_2` varchar(512) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
                                         `str_prop_3` varchar(512) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
                                         `int_prop_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
                                         `int_prop_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
                                         `long_prop_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
                                         `long_prop_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
                                         `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
                                         `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
                                         `bool_prop_1` varchar(1) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
                                         `bool_prop_2` varchar(1) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
                                         PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                         CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='同步机制的行锁表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `qrtz_simprop_triggers`


LOCK TABLES `qrtz_simprop_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_simprop_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_simprop_triggers` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `qrtz_triggers`


DROP TABLE IF EXISTS `qrtz_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_triggers` (
                                 `sched_name` varchar(120) COLLATE utf8mb4_bin NOT NULL COMMENT '调度名称',
                                 `trigger_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT '触发器的名字',
                                 `trigger_group` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT '触发器所属组的名字',
                                 `job_name` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
                                 `job_group` varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
                                 `description` varchar(250) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '相关介绍',
                                 `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
                                 `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
                                 `priority` int DEFAULT NULL COMMENT '优先级',
                                 `trigger_state` varchar(16) COLLATE utf8mb4_bin NOT NULL COMMENT '触发器状态',
                                 `trigger_type` varchar(8) COLLATE utf8mb4_bin NOT NULL COMMENT '触发器的类型',
                                 `start_time` bigint NOT NULL COMMENT '开始时间',
                                 `end_time` bigint DEFAULT NULL COMMENT '结束时间',
                                 `calendar_name` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '日程表名称',
                                 `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
                                 `job_data` blob COMMENT '存放持久化job对象',
                                 PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
                                 KEY `sched_name` (`sched_name`,`job_name`,`job_group`),
                                 CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='触发器详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `qrtz_triggers`


LOCK TABLES `qrtz_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_triggers` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_config`


DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
                              `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
                              `config_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数名称',
                              `config_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数键名',
                              `config_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数键值',
                              `config_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
                              `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                              `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                              `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                              `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                              `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                              `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                              PRIMARY KEY (`config_id`),
                              KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_config`



/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO sys_config VALUES(1, '主框架页-默认皮肤样式名称',     'sys.index.skinName',            'skin-blue',     'Y', 'admin', NOW(), '', NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO sys_config VALUES(2, '用户管理-账号初始密码',         'sys.user.initPassword',         '123456',        'Y', 'admin', NOW(), '', NULL, '初始化密码 123456');
INSERT INTO sys_config VALUES(3, '主框架页-侧边栏主题',           'sys.index.sideTheme',           'theme-dark',    'Y', 'admin', NOW(), '', NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO sys_config VALUES(4, '账号自助-验证码开关',           'sys.account.captchaEnabled',    'true',          'Y', 'admin', NOW(), '', NULL, '是否开启验证码功能（true开启，false关闭）');
INSERT INTO sys_config VALUES(5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser',      'false',         'Y', 'admin', NOW(), '', NULL, '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO sys_config VALUES(6, '用户登录-黑名单列表',           'sys.login.blackIPList',         '',              'Y', 'admin', NOW(), '', NULL, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;



-- Table structure for table `sys_deploy_form`


DROP TABLE IF EXISTS `sys_deploy_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_deploy_form` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `form_id` bigint DEFAULT NULL COMMENT '表单主键',
                                   `deploy_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程实例主键',
                                   `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                   `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                   `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                   `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例关联表单';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_deploy_form`


LOCK TABLES `sys_deploy_form` WRITE;
/*!40000 ALTER TABLE `sys_deploy_form` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_deploy_form` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_dept`


DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
                            `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
                            `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
                            `ancestors` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '祖级列表',
                            `dept_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '部门名称',
                            `order_num` int DEFAULT '0' COMMENT '显示顺序',
                            `leader` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '负责人',
                            `phone` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
                            `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
                            `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
                            `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                            `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_dept`


LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
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

/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_dict_data`


DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
                                 `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
                                 `dict_sort` int DEFAULT '0' COMMENT '字典排序',
                                 `dict_label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典标签',
                                 `dict_value` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典键值',
                                 `dict_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
                                 `css_class` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
                                 `list_class` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表格回显样式',
                                 `is_default` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
                                 `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                 `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`dict_code`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_dict_data`


LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;

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

/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

-- Table structure for table `sys_dict_type`


DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
                                 `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
                                 `dict_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典名称',
                                 `dict_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
                                 `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                 `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`dict_id`),
                                 UNIQUE KEY `uk_dict_type` (`dict_type`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_dict_type`


LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
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

/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_expression`


DROP TABLE IF EXISTS `sys_expression`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_expression` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '表单主键',
                                  `name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表达式名称',
                                  `expression` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表达式内容',
                                  `data_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表达式类型',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                  `create_by` bigint DEFAULT NULL COMMENT '创建人员',
                                  `update_by` bigint DEFAULT NULL COMMENT '更新人员',
                                  `status` tinyint DEFAULT '0' COMMENT '状态',
                                  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                  `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程表达式';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_expression`


LOCK TABLES `sys_expression` WRITE;
/*!40000 ALTER TABLE `sys_expression` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_expression` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_file`


DROP TABLE IF EXISTS `sys_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_file`


LOCK TABLES `sys_file` WRITE;
/*!40000 ALTER TABLE `sys_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_file` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_form`


DROP TABLE IF EXISTS `sys_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_form` (
                            `form_id` bigint NOT NULL AUTO_INCREMENT COMMENT '表单主键',
                            `form_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表单名称',
                            `form_content` longtext COLLATE utf8mb4_unicode_ci COMMENT '表单内容',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `create_by` bigint DEFAULT NULL COMMENT '创建人员',
                            `update_by` bigint DEFAULT NULL COMMENT '更新人员',
                            `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                            `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                            PRIMARY KEY (`form_id`),
                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程表单';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_form`


LOCK TABLES `sys_form` WRITE;
/*!40000 ALTER TABLE `sys_form` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_form` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_job`


DROP TABLE IF EXISTS `sys_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job` (
                           `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
                           `job_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '任务名称',
                           `job_group` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
                           `invoke_target` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用目标字符串',
                           `cron_expression` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'cron执行表达式',
                           `misfire_policy` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
                           `concurrent` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
                           `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1暂停）',
                           `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                           `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                           `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                           `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                           `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注信息',
                           `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                           PRIMARY KEY (`job_id`,`job_name`,`job_group`),
                           KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务调度表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_job`


LOCK TABLES `sys_job` WRITE;
/*!40000 ALTER TABLE `sys_job` DISABLE KEYS */;
INSERT INTO `sys_job` VALUES (1,'系统默认（无参）','DEFAULT','ryTask.ryNoParams','0/10 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),(2,'系统默认（有参）','DEFAULT','ryTask.ryParams(\'ry\')','0/15 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),(3,'系统默认（多参）','DEFAULT','ryTask.ryMultipleParams(\'ry\', true, 2000, 316.50, 100)','0/20 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0');
/*!40000 ALTER TABLE `sys_job` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_job_log`


DROP TABLE IF EXISTS `sys_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job_log` (
                               `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
                               `job_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
                               `job_group` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务组名',
                               `invoke_target` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用目标字符串',
                               `job_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '日志信息',
                               `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
                               `exception_info` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '异常信息',
                               `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                               `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                               `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                               `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                               PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务调度日志表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_job_log`


LOCK TABLES `sys_job_log` WRITE;
/*!40000 ALTER TABLE `sys_job_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_job_log` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_listener`


DROP TABLE IF EXISTS `sys_listener`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_listener` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '表单主键',
                                `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '名称',
                                `type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听类型',
                                `event_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听事件类型',
                                `value_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听值类型',
                                `value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听值',
                                `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                `create_by` bigint DEFAULT NULL COMMENT '创建人员',
                                `update_by` bigint DEFAULT NULL COMMENT '更新人员',
                                `status` tinyint DEFAULT '0' COMMENT '状态',
                                `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                PRIMARY KEY (`id`),
                                KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程监听';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_listener`


LOCK TABLES `sys_listener` WRITE;
/*!40000 ALTER TABLE `sys_listener` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_listener` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_logininfor`


DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
                                  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
                                  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户账号',
                                  `ipaddr` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '登录IP地址',
                                  `login_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '登录地点',
                                  `browser` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '浏览器类型',
                                  `os` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作系统',
                                  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
                                  `msg` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '提示消息',
                                  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
                                  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                  PRIMARY KEY (`info_id`),
                                  KEY `idx_sys_logininfor_s` (`status`),
                                  KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_logininfor`


LOCK TABLES `sys_logininfor` WRITE;
/*!40000 ALTER TABLE `sys_logininfor` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_logininfor` ENABLE KEYS */;
UNLOCK TABLES;

-- Table structure for table `sys_menu`


DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
                            `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
                            `menu_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
                            `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
                            `order_num` int DEFAULT '0' COMMENT '显示顺序',
                            `path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '路由地址',
                            `component` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组件路径',
                            `query` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由参数',
                            `route_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '路由名称',
                            `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
                            `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
                            `menu_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
                            `visible` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
                            `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
                            `perms` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限标识',
                            `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '#' COMMENT '菜单图标',
                            `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
                            `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                            PRIMARY KEY (`menu_id`),
                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=2229 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_menu`


LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
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


-- =============================================================================
-- 一、一级目录：内容管理
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '内容管理', 0, 10, 'cms', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'documentation', 'admin', NOW(), '内容管理目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0);
SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;

-- =============================================================================
-- 二、门户用户管理（cms:user）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门户用户', @cms_parent_id, 1, 'user', 'cms/user/index', NULL, 1, 0, 'C', '0', '0', 'cms:user:list', 'user', 'admin', NOW(), '门户用户管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:list');
SELECT @user_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:user:list' LIMIT 1;

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

-- =============================================================================
-- 七、通知管理（cms:notification）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知管理', @cms_parent_id, 6, 'notification', 'cms/notification/index', NULL, 1, 0, 'C', '0', '0', 'cms:notification:list', 'email', 'admin', NOW(), '通知管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:notification:list');
SELECT @notification_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:notification:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知查询', @notification_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:notification:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知新增', @notification_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:notification:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知修改', @notification_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:notification:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知删除', @notification_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:notification:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '发送系统通知', @notification_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:sendAll', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:notification:sendAll');

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
-- 十七、为超级管理员（role_id=1）批量分配所有 CMS 菜单权限（自动去重）
-- =============================================================================


INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @admin_role_id, m.menu_id FROM sys_menu m
WHERE (m.perms LIKE 'cms:%'
    OR m.perms IN ('portal:column:list','portal:column:query','portal:column:add','portal:column:edit','portal:column:remove',
                   'portal:tip:list','portal:tip:query',
                   'portal:order:list','portal:order:query')
    OR (m.menu_name IN ('内容管理','创作者认证','财务') AND m.parent_id = 0))
  AND m.menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = @admin_role_id);

-- =============================================================================
-- 十八、校验查询
-- =============================================================================
-- 1. 内容管理目录及子菜单（按 order_num 排序）
SELECT '===== 内容管理目录树 =====' AS info;
SELECT m1.menu_id, m1.menu_name, m1.order_num, m1.path, m1.component, m1.perms, m1.menu_type, m1.visible,
       m2.menu_name AS child_name, m2.perms AS child_perms, m2.menu_type AS child_type
FROM sys_menu m1
         LEFT JOIN sys_menu m2 ON m2.parent_id = m1.menu_id
WHERE m1.menu_name = '内容管理' AND m1.parent_id = 0
ORDER BY m1.order_num, m2.order_num;

-- 2. 创作者认证目录
SELECT '===== 创作者认证 =====' AS info;
SELECT menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type
FROM sys_menu
WHERE menu_name = '创作者认证' OR perms LIKE 'cms:certification:%'
ORDER BY menu_id;

-- 3. 财务目录（已下线）
SELECT '===== 财务（已下线） =====' AS info;
SELECT menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible
FROM sys_menu
WHERE menu_name = '财务' OR perms LIKE 'portal:order:%'
ORDER BY menu_id;

-- 4. 统计总数
SELECT CONCAT('CMS 菜单总数: ', COUNT(*)) AS summary
FROM sys_menu
WHERE perms LIKE 'cms:%'
   OR perms LIKE 'portal:column:%'
   OR perms LIKE 'portal:tip:%'
   OR perms LIKE 'portal:order:%'
   OR (menu_name IN ('内容管理','创作者认证','财务') AND parent_id = 0);

SELECT 'CMS 菜单完整初始化脚本执行完成！' AS message;


/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notice_bak`
--

DROP TABLE IF EXISTS `sys_notice_bak`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice_bak` (
                                  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
                                  `notice_title` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
                                  `notice_type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
                                  `notice_content` longblob COMMENT '公告内容',
                                  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
                                  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notice_bak`
--

LOCK TABLES `sys_notice_bak` WRITE;
/*!40000 ALTER TABLE `sys_notice_bak` DISABLE KEYS */;
INSERT INTO `sys_notice_bak` VALUES (1,'温馨提醒：2018-07-01 若依新版本发布啦','2',_binary '新版本内容','0','admin','2026-07-28 15:42:36','',NULL,'管理员'),(2,'维护通知：2018-07-01 若依系统凌晨维护','1',_binary '维护内容','0','admin','2026-07-28 15:42:36','',NULL,'管理员');
/*!40000 ALTER TABLE `sys_notice_bak` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notification`
--

DROP TABLE IF EXISTS `sys_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
                                    `type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型：system/comment/like/follow/order/notice/announcement',
                                    `title` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '通知标题',
                                    `content` text COLLATE utf8mb4_general_ci COMMENT '通知内容',
                                    `data` json DEFAULT NULL COMMENT '通知数据（JSON格式）',
                                    `scope` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'user' COMMENT '范围：user=个人通知 / all=全局广播',
                                    `user_id` bigint DEFAULT NULL COMMENT '接收用户ID（scope=user 时必填，scope=all 时为 NULL）',
                                    `user_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'portal' COMMENT '接收用户类型：portal=门户用户 / sys=系统用户（scope=user 时生效）',
                                    `notice_type` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '通知/公告分类：1=通知 / 2=公告（兼容 sys_notice 字典 sys_notice_type）',
                                    `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态：0=正常 / 1=关闭（兼容 sys_notice 字典 sys_notice_status）',
                                    `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                    `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_type` (`type`),
                                    KEY `idx_scope` (`scope`),
                                    KEY `idx_user_id` (`user_id`),
                                    KEY `idx_status` (`status`),
                                    KEY `idx_create_time` (`create_time`),
                                    KEY `idx_user_type_user_id` (`user_type`,`user_id`),
                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统通知主体表（合并 portal_notification + sys_notice）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notification`
--

LOCK TABLES `sys_notification` WRITE;
/*!40000 ALTER TABLE `sys_notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notification_read`
--

DROP TABLE IF EXISTS `sys_notification_read`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification_read` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `notification_id` bigint NOT NULL COMMENT '通知ID（关联 sys_notification.id）',
                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                         `user_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'portal' COMMENT '已读用户类型：portal=门户用户 / sys=系统用户',
                                         `read_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_notif_user_type` (`notification_id`,`user_id`,`user_type`),
                                         KEY `idx_user_id` (`user_id`),
                                         KEY `idx_notification_id` (`notification_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统通知用户已读关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notification_read`
--

LOCK TABLES `sys_notification_read` WRITE;
/*!40000 ALTER TABLE `sys_notification_read` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_notification_read` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
                                `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
                                `title` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '模块标题',
                                `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
                                `method` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '方法名称',
                                `request_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求方式',
                                `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
                                `oper_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作人员',
                                `dept_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '部门名称',
                                `oper_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求URL',
                                `oper_ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '主机地址',
                                `oper_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作地点',
                                `oper_param` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求参数',
                                `json_result` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '返回参数',
                                `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
                                `error_msg` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '错误消息',
                                `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
                                `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
                                `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                PRIMARY KEY (`oper_id`),
                                KEY `idx_sys_oper_log_bt` (`business_type`),
                                KEY `idx_sys_oper_log_s` (`status`),
                                KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
                            `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
                            `post_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位编码',
                            `post_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位名称',
                            `post_sort` int NOT NULL COMMENT '显示顺序',
                            `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态（0正常 1停用）',
                            `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                            `del_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                            PRIMARY KEY (`post_id`),
                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_post`
--

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post` DISABLE KEYS */;
-- 初始化-岗位信息表数据
INSERT INTO sys_post VALUES(1, 'ceo',  '董事长',    1, '0', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_post VALUES(2, 'se',   '项目经理',  2, '0', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_post VALUES(3, 'hr',   '人力资源',  3, '0', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_post VALUES(4, 'user', '普通员工',  4, '0', 'admin', NOW(), '', NULL, '');
/*!40000 ALTER TABLE `sys_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
                            `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                            `role_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
                            `role_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色权限字符串',
                            `role_sort` int NOT NULL COMMENT '显示顺序',
                            `data_scope` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
                            `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
                            `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
                            `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
                            `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                            `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO sys_role VALUES('1', '超级管理员',  'admin',  1, 1, 1, 1, '0', '0', 'admin', NOW(), '', NULL, '超级管理员');
INSERT INTO sys_role VALUES('2', '普通角色',    'common', 2, 2, 1, 1, '0', '0', 'admin', NOW(), '', NULL, '普通角色');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
                                 `role_id` bigint NOT NULL COMMENT '角色ID',
                                 `dept_id` bigint NOT NULL COMMENT '部门ID',
                                 `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_dept`
--

LOCK TABLES `sys_role_dept` WRITE;
/*!40000 ALTER TABLE `sys_role_dept` DISABLE KEYS */;
INSERT INTO `sys_role_dept` VALUES (2,100,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,101,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,105,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL);
/*!40000 ALTER TABLE `sys_role_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
                                 `role_id` bigint NOT NULL COMMENT '角色ID',
                                 `menu_id` bigint NOT NULL COMMENT '菜单ID',
                                 `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,2000,'',NULL,'',NULL,NULL),(1,2001,'',NULL,'',NULL,NULL),(1,2002,'',NULL,'',NULL,NULL),(1,2003,'',NULL,'',NULL,NULL),(1,2004,'',NULL,'',NULL,NULL),(1,2005,'',NULL,'',NULL,NULL),(1,2006,'',NULL,'',NULL,NULL),(1,2007,'',NULL,'',NULL,NULL),(1,2008,'',NULL,'',NULL,NULL),(1,2009,'',NULL,'',NULL,NULL),(1,2010,'',NULL,'',NULL,NULL),(1,2011,'',NULL,'',NULL,NULL),(1,2012,'',NULL,'',NULL,NULL),(1,2013,'',NULL,'',NULL,NULL),(1,2014,'',NULL,'',NULL,NULL),(1,2015,'',NULL,'',NULL,NULL),(1,2016,'',NULL,'',NULL,NULL),(1,2017,'',NULL,'',NULL,NULL),(1,2018,'',NULL,'',NULL,NULL),(1,2019,'',NULL,'',NULL,NULL),(1,2020,'',NULL,'',NULL,NULL),(1,2021,'',NULL,'',NULL,NULL),(1,2022,'',NULL,'',NULL,NULL),(1,2023,'',NULL,'',NULL,NULL),(1,2024,'',NULL,'',NULL,NULL),(1,2025,'',NULL,'',NULL,NULL),(1,2027,'',NULL,'',NULL,NULL),(1,2028,'',NULL,'',NULL,NULL),(1,2029,'',NULL,'',NULL,NULL),(1,2030,'',NULL,'',NULL,NULL),(1,2031,'',NULL,'',NULL,NULL),(1,2032,'',NULL,'',NULL,NULL),(1,2033,'',NULL,'',NULL,NULL),(1,2034,'',NULL,'',NULL,NULL),(1,2035,'',NULL,'',NULL,NULL),(1,2036,'',NULL,'',NULL,NULL),(1,2037,'',NULL,'',NULL,NULL),(1,2038,'',NULL,'',NULL,NULL),(1,2039,'',NULL,'',NULL,NULL),(1,2040,'',NULL,'',NULL,NULL),(1,2041,'',NULL,'',NULL,NULL),(1,2042,'',NULL,'',NULL,NULL),(1,2043,'',NULL,'',NULL,NULL),(1,2044,'',NULL,'',NULL,NULL),(1,2045,'',NULL,'',NULL,NULL),(1,2046,'',NULL,'',NULL,NULL),(1,2047,'',NULL,'',NULL,NULL),(1,2048,'',NULL,'',NULL,NULL),(1,2049,'',NULL,'',NULL,NULL),(1,2050,'',NULL,'',NULL,NULL),(1,2051,'',NULL,'',NULL,NULL),(1,2052,'',NULL,'',NULL,NULL),(1,2053,'',NULL,'',NULL,NULL),(1,2054,'',NULL,'',NULL,NULL),(1,2055,'',NULL,'',NULL,NULL),(1,2056,'',NULL,'',NULL,NULL),(1,2057,'',NULL,'',NULL,NULL),(1,2058,'',NULL,'',NULL,NULL),(1,2059,'',NULL,'',NULL,NULL),(1,2060,'',NULL,'',NULL,NULL),(1,2061,'',NULL,'',NULL,NULL),(1,2062,'',NULL,'',NULL,NULL),(1,2063,'',NULL,'',NULL,NULL),(1,2064,'',NULL,'',NULL,NULL),(1,2065,'',NULL,'',NULL,NULL),(1,2066,'',NULL,'',NULL,NULL),(1,2067,'',NULL,'',NULL,NULL),(1,2068,'',NULL,'',NULL,NULL),(1,2069,'',NULL,'',NULL,NULL),(1,2070,'',NULL,'',NULL,NULL),(1,2071,'',NULL,'',NULL,NULL),(1,2072,'',NULL,'',NULL,NULL),(1,2073,'',NULL,'',NULL,NULL),(1,2074,'',NULL,'',NULL,NULL),(1,2075,'',NULL,'',NULL,NULL),(1,2076,'',NULL,'',NULL,NULL),(1,2077,'',NULL,'',NULL,NULL),(1,2078,'',NULL,'',NULL,NULL),(1,2079,'',NULL,'',NULL,NULL),(1,2080,'',NULL,'',NULL,NULL),(1,2081,'',NULL,'',NULL,NULL),(1,2082,'',NULL,'',NULL,NULL),(1,2083,'',NULL,'',NULL,NULL),(1,2084,'',NULL,'',NULL,NULL),(1,2085,'',NULL,'',NULL,NULL),(1,2086,'',NULL,'',NULL,NULL),(1,2087,'',NULL,'',NULL,NULL),(1,2088,'',NULL,'',NULL,NULL),(1,2089,'',NULL,'',NULL,NULL),(1,2090,'',NULL,'',NULL,NULL),(1,2091,'',NULL,'',NULL,NULL),(1,2092,'',NULL,'',NULL,NULL),(1,2093,'',NULL,'',NULL,NULL),(1,2094,'',NULL,'',NULL,NULL),(1,2095,'',NULL,'',NULL,NULL),(1,2096,'',NULL,'',NULL,NULL),(1,2097,'',NULL,'',NULL,NULL),(1,2098,'',NULL,'',NULL,NULL),(1,2099,'',NULL,'',NULL,NULL),(1,2100,'',NULL,'',NULL,NULL),(1,2101,'',NULL,'',NULL,NULL),(1,2102,'',NULL,'',NULL,NULL),(1,2103,'',NULL,'',NULL,NULL),(1,2104,'',NULL,'',NULL,NULL),(1,2105,'',NULL,'',NULL,NULL),(1,2106,'',NULL,'',NULL,NULL),(1,2107,'',NULL,'',NULL,NULL),(1,2108,'',NULL,'',NULL,NULL),(1,2109,'',NULL,'',NULL,NULL),(1,2110,'',NULL,'',NULL,NULL),(1,2111,'',NULL,'',NULL,NULL),(1,2112,'',NULL,'',NULL,NULL),(1,2113,'',NULL,'',NULL,NULL),(1,2114,'',NULL,'',NULL,NULL),(1,2115,'',NULL,'',NULL,NULL),(1,2116,'',NULL,'',NULL,NULL),(1,2117,'',NULL,'',NULL,NULL),(1,2118,'',NULL,'',NULL,NULL),(1,2119,'',NULL,'',NULL,NULL),(1,2120,'',NULL,'',NULL,NULL),(1,2121,'',NULL,'',NULL,NULL),(1,2122,'',NULL,'',NULL,NULL),(1,2123,'',NULL,'',NULL,NULL),(1,2124,'',NULL,'',NULL,NULL),(1,2125,'',NULL,'',NULL,NULL),(1,2126,'',NULL,'',NULL,NULL),(1,2127,'',NULL,'',NULL,NULL),(1,2128,'',NULL,'',NULL,NULL),(1,2129,'',NULL,'',NULL,NULL),(1,2130,'',NULL,'',NULL,NULL),(1,2131,'',NULL,'',NULL,NULL),(1,2132,'',NULL,'',NULL,NULL),(1,2133,'',NULL,'',NULL,NULL),(1,2134,'',NULL,'',NULL,NULL),(1,2135,'',NULL,'',NULL,NULL),(1,2136,'',NULL,'',NULL,NULL),(1,2137,'',NULL,'',NULL,NULL),(1,2138,'',NULL,'',NULL,NULL),(1,2139,'',NULL,'',NULL,NULL),(1,2140,'',NULL,'',NULL,NULL),(1,2141,'',NULL,'',NULL,NULL),(1,2142,'',NULL,'',NULL,NULL),(1,2143,'',NULL,'',NULL,NULL),(1,2144,'',NULL,'',NULL,NULL),(1,2145,'',NULL,'',NULL,NULL),(1,2146,'',NULL,'',NULL,NULL),(1,2147,'',NULL,'',NULL,NULL),(1,2148,'',NULL,'',NULL,NULL),(1,2149,'',NULL,'',NULL,NULL),(1,2150,'',NULL,'',NULL,NULL),(1,2151,'',NULL,'',NULL,NULL),(1,2152,'',NULL,'',NULL,NULL),(1,2153,'',NULL,'',NULL,NULL),(1,2154,'',NULL,'',NULL,NULL),(1,2155,'',NULL,'',NULL,NULL),(1,2156,'',NULL,'',NULL,NULL),(1,2157,'',NULL,'',NULL,NULL),(1,2158,'',NULL,'',NULL,NULL),(1,2159,'',NULL,'',NULL,NULL),(1,2160,'',NULL,'',NULL,NULL),(1,2161,'',NULL,'',NULL,NULL),(1,2162,'',NULL,'',NULL,NULL),(1,2169,'',NULL,'',NULL,NULL),(1,2170,'',NULL,'',NULL,NULL),(1,2171,'',NULL,'',NULL,NULL),(1,2172,'',NULL,'',NULL,NULL),(1,2173,'',NULL,'',NULL,NULL),(1,2174,'',NULL,'',NULL,NULL),(1,2175,'',NULL,'',NULL,NULL),(1,2176,'',NULL,'',NULL,NULL),(1,2177,'',NULL,'',NULL,NULL),(1,2178,'',NULL,'',NULL,NULL),(1,2179,'',NULL,'',NULL,NULL),(1,2180,'',NULL,'',NULL,NULL),(1,2181,'',NULL,'',NULL,NULL),(1,2182,'',NULL,'',NULL,NULL),(1,2183,'',NULL,'',NULL,NULL),(1,2184,'',NULL,'',NULL,NULL),(1,2185,'',NULL,'',NULL,NULL),(1,2186,'',NULL,'',NULL,NULL),(1,2187,'',NULL,'',NULL,NULL),(1,2188,'',NULL,'',NULL,NULL),(1,2189,'',NULL,'',NULL,NULL),(1,2190,'',NULL,'',NULL,NULL),(1,2191,'',NULL,'',NULL,NULL),(1,2192,'',NULL,'',NULL,NULL),(1,2193,'',NULL,'',NULL,NULL),(1,2194,'',NULL,'',NULL,NULL),(1,2195,'',NULL,'',NULL,NULL),(1,2196,'',NULL,'',NULL,NULL),(1,2197,'',NULL,'',NULL,NULL),(1,2198,'',NULL,'',NULL,NULL),(1,2199,'',NULL,'',NULL,NULL),(1,2200,'',NULL,'',NULL,NULL),(1,2201,'',NULL,'',NULL,NULL),(1,2202,'',NULL,'',NULL,NULL),(1,2203,'',NULL,'',NULL,NULL),(1,2204,'',NULL,'',NULL,NULL),(1,2205,'',NULL,'',NULL,NULL),(1,2206,'',NULL,'',NULL,NULL),(1,2207,'',NULL,'',NULL,NULL),(1,2208,'',NULL,'',NULL,NULL),(1,2209,'',NULL,'',NULL,NULL),(1,2210,'',NULL,'',NULL,NULL),(1,2211,'',NULL,'',NULL,NULL),(1,2212,'',NULL,'',NULL,NULL),(1,2213,'',NULL,'',NULL,NULL),(1,2214,'',NULL,'',NULL,NULL),(1,2215,'',NULL,'',NULL,NULL),(1,2216,'',NULL,'',NULL,NULL),(1,2217,'',NULL,'',NULL,NULL),(1,2218,'',NULL,'',NULL,NULL),(1,2219,'',NULL,'',NULL,NULL),(1,2220,'',NULL,'',NULL,NULL),(1,2221,'',NULL,'',NULL,NULL),(1,2222,'',NULL,'',NULL,NULL),(1,2223,'',NULL,'',NULL,NULL),(1,2224,'',NULL,'',NULL,NULL),(1,2225,'',NULL,'',NULL,NULL),(1,2226,'',NULL,'',NULL,NULL),(1,2227,'',NULL,'',NULL,NULL),(1,2228,'',NULL,'',NULL,NULL),(2,1,'admin','2026-07-28 15:42:32','','2026-07-28 15:42:32',NULL),(2,2,'admin','2026-07-28 15:42:32','','2026-07-28 15:42:32',NULL),(2,3,'admin','2026-07-28 15:42:32','','2026-07-28 15:42:32',NULL),(2,4,'admin','2026-07-28 15:42:32','','2026-07-28 15:42:32',NULL),(2,100,'admin','2026-07-28 15:42:32','','2026-07-28 15:42:32',NULL),(2,101,'admin','2026-07-28 15:42:32','','2026-07-28 15:42:32',NULL),(2,102,'admin','2026-07-28 15:42:32','','2026-07-28 15:42:32',NULL),(2,103,'admin','2026-07-28 15:42:32','','2026-07-28 15:42:32',NULL),(2,104,'admin','2026-07-28 15:42:32','','2026-07-28 15:42:32',NULL),(2,105,'admin','2026-07-28 15:42:32','','2026-07-28 15:42:32',NULL),(2,106,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,108,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,109,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,110,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,111,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,112,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,113,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,114,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,115,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,116,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,117,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,118,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,119,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,500,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,501,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1000,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1001,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1002,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1003,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1004,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1005,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1006,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1007,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1008,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1009,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1010,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1011,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1012,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1013,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1014,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1015,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1016,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1017,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1018,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1019,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1020,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1021,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1022,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1023,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1024,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1025,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1026,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1027,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1028,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1029,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1030,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1031,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1032,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1033,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1034,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1039,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1040,'admin','2026-07-28 15:42:33','','2026-07-28 15:42:33',NULL),(2,1041,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1042,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1043,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1044,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1045,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1046,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1047,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1048,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1049,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1050,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1051,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1052,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1053,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1054,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1055,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1056,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1057,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1058,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1059,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,1060,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
                            `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
                            `user_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号',
                            `nick_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户昵称',
                            `user_type` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT '00' COMMENT '用户类型（00系统用户）',
                            `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户邮箱',
                            `phonenumber` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '手机号码',
                            `sex` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
                            `avatar` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '头像地址',
                            `password` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '密码',
                            `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
                            `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                            `login_ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '最后登录IP',
                            `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
                            `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                            `business_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务主键（前缀sysu_）',
                            PRIMARY KEY (`user_id`),
                            UNIQUE KEY `uk_business_id` (`business_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
-- 初始化-用户信息表数据
INSERT INTO sys_user VALUES(1,  103, 'admin', '若依', '00', 'ry@163.com', '15888888888', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '', NULL, '管理员');
INSERT INTO sys_user VALUES(2,  105, 'ry',    '若依', '00', 'ry@qq.com',  '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), '', NULL, '测试员');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_user_post`


DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `post_id` bigint NOT NULL COMMENT '岗位ID',
                                 `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

-- Dumping data for table `sys_user_post`


LOCK TABLES `sys_user_post` WRITE;
/*!40000 ALTER TABLE `sys_user_post` DISABLE KEYS */;
INSERT INTO `sys_user_post` VALUES (1,1,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),(2,2,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL);
/*!40000 ALTER TABLE `sys_user_post` ENABLE KEYS */;
UNLOCK TABLES;


-- Table structure for table `sys_user_role`


DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `role_id` bigint NOT NULL COMMENT '角色ID',
                                 `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;


-- Dumping data for table `sys_user_role`


LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO sys_user_role VALUES ('1', '1','admin', NOW(), '',NOW(), NULL);
INSERT INTO sys_user_role VALUES ('2', '2','admin', NOW(), '',NOW(), NULL);

/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

