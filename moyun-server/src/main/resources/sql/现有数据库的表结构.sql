-- MySQL dump 10.13  Distrib 8.0.37, for Win64 (x86_64)
--
-- Host: localhost    Database: ecom_publish
-- ------------------------------------------------------
-- Server version	8.0.37

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ËßíËâ≤ID',
  `role_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ËßíËâ≤ÂêçÁß∞',
  `role_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ËßíËâ≤ÊùÉÈôêÂ≠óÁ¨¶‰∏≤',
  `role_sort` int NOT NULL COMMENT 'ÊòæÁ§∫È°∫Â∫è',
  `data_scope` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT 'Êï∞ÊçÆËåÉÂõ¥Ôºà1ÔºöÂÖ®ÈÉ®Êï∞ÊçÆÊùÉÈôê 2ÔºöËá™ÂÆöÊï∞ÊçÆÊùÉÈôê 3ÔºöÊú¨ÈÉ®Èó®Êï∞ÊçÆÊùÉÈôê 4ÔºöÊú¨ÈÉ®Èó®Âèä‰ª•‰∏ãÊï∞ÊçÆÊùÉÈôêÔºâ',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT 'ËèúÂçïÊ†ëÈÄâÊã©È°πÊòØÂê¶ÂÖ≥ËÅîÊòæÁ§∫',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT 'ÈÉ®Èó®Ê†ëÈÄâÊã©È°πÊòØÂê¶ÂÖ≥ËÅîÊòæÁ§∫',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ËßíËâ≤Áä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÂÅúÁî®Ôºâ',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'Âà†Èô§Ê†áÂøóÔºà0‰ª£Ë°®Â≠òÂú® 2‰ª£Ë°®Âà†Èô§Ôºâ',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_sys_role_key` (`role_key`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ËßíËâ≤‰ø°ÊÅØË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'Ë∂ÖÁ∫ßÁÆ°ÁêÜÂëò','admin',1,'1',1,1,'0','0','admin','2026-04-22 01:46:28','',NULL,'Ë∂ÖÁ∫ßÁÆ°ÁêÜÂëò'),(2,'ÊôÆÈÄöËßíËâ≤','common',2,'2',1,1,'0','0','admin','2026-04-22 01:46:28','',NULL,'ÊôÆÈÄöËßíËâ≤');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_dmn_deployment`
--

DROP TABLE IF EXISTS `act_dmn_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_deployment` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_dmn_deployment`
--

LOCK TABLES `act_dmn_deployment` WRITE;
/*!40000 ALTER TABLE `act_dmn_deployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_dmn_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT 'Áî®Êà∑ID',
  `post_id` bigint NOT NULL COMMENT 'Â≤ó‰ΩçID',
  PRIMARY KEY (`user_id`,`post_id`),
  KEY `idx_sys_user_post_user` (`user_id`),
  KEY `idx_sys_user_post_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Áî®Êà∑‰∏éÂ≤ó‰ΩçÂÖ≥ËÅîË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_post`
--

LOCK TABLES `sys_user_post` WRITE;
/*!40000 ALTER TABLE `sys_user_post` DISABLE KEYS */;
INSERT INTO `sys_user_post` VALUES (1,1),(2,2);
/*!40000 ALTER TABLE `sys_user_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_re_deployment`
--

DROP TABLE IF EXISTS `act_re_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_deployment` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `DEPLOY_TIME_` timestamp(3) NULL DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_deployment`
--

LOCK TABLES `act_re_deployment` WRITE;
/*!40000 ALTER TABLE `act_re_deployment` DISABLE KEYS */;
INSERT INTO `act_re_deployment` VALUES ('891a4294-305c-11f1-82b6-8c1645e938b5','flow_1hxp265d','leave',NULL,'','2026-04-04 19:29:02.052',NULL,NULL,'891a4294-305c-11f1-82b6-8c1645e938b5',NULL);
/*!40000 ALTER TABLE `act_re_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT 'ËßíËâ≤ID',
  `dept_id` bigint NOT NULL COMMENT 'ÈÉ®Èó®ID',
  PRIMARY KEY (`role_id`,`dept_id`),
  KEY `idx_sys_role_dept_role` (`role_id`),
  KEY `idx_sys_role_dept_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ËßíËâ≤ÂíåÈÉ®Èó®ÂÖ≥ËÅîË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_dept`
--

LOCK TABLES `sys_role_dept` WRITE;
/*!40000 ALTER TABLE `sys_role_dept` DISABLE KEYS */;
INSERT INTO `sys_role_dept` VALUES (2,100),(2,101),(2,105);
/*!40000 ALTER TABLE `sys_role_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `operation_logs`
--

DROP TABLE IF EXISTS `operation_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_logs` (
  `id` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Êó•ÂøóID',
  `admin_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Êìç‰ΩúÁÆ°ÁêÜÂëòID',
  `user_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Êìç‰ΩúÁî®Êà∑ID',
  `module` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ê®°Âùó',
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Êìç‰Ωú',
  `request_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ËØ∑Ê±ÇÊñπÊ≥ï',
  `request_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ËØ∑Ê±ÇURL',
  `request_params` json DEFAULT NULL COMMENT 'ËØ∑Ê±ÇÂèÇÊï∞',
  `response_status` int DEFAULT NULL COMMENT 'ÂìçÂ∫îÁä∂ÊÄÅ',
  `response_data` json DEFAULT NULL COMMENT 'ÂìçÂ∫îÊï∞ÊçÆ',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IPÂú∞ÂùÄ',
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Áî®Êà∑‰ª£ÁêÜ',
  `duration` int DEFAULT NULL COMMENT 'ËÄóÊó∂(ÊØ´Áßí)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_module` (`module`),
  KEY `idx_action` (`action`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Êìç‰ΩúÊó•ÂøóË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `operation_logs`
--

LOCK TABLES `operation_logs` WRITE;
/*!40000 ALTER TABLE `operation_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_history_job`
--

DROP TABLE IF EXISTS `act_ru_history_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_history_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ADV_HANDLER_CFG_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_history_job`
--

LOCK TABLES `act_ru_history_job` WRITE;
/*!40000 ALTER TABLE `act_ru_history_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_history_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_hi_mil_inst`
--

DROP TABLE IF EXISTS `act_cmmn_hi_mil_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_hi_mil_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TIME_STAMP_` datetime(3) NOT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_hi_mil_inst`
--

LOCK TABLES `act_cmmn_hi_mil_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_hi_mil_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_hi_mil_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_dmn_decision`
--

DROP TABLE IF EXISTS `act_dmn_decision`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_decision` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DECISION_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_DMN_DEC_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_dmn_decision`
--

LOCK TABLES `act_dmn_decision` WRITE;
/*!40000 ALTER TABLE `act_dmn_decision` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_dmn_decision` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_comment`
--

DROP TABLE IF EXISTS `act_hi_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_comment` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `MESSAGE_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `FULL_MSG_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_comment`
--

LOCK TABLES `act_hi_comment` WRITE;
/*!40000 ALTER TABLE `act_hi_comment` DISABLE KEYS */;
INSERT INTO `act_hi_comment` VALUES ('8cad9e9d-3063-11f1-8e5a-8c1645e938b5','1','2026-04-05 04:19:14.527','1','8c9b00fc-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','AddComment','Ëã•‰æùÂèëËµ∑ÊµÅÁ®ãÁî≥ËØ∑',_binary 'Ëã•‰æùÂèëËµ∑ÊµÅÁ®ãÁî≥ËØ∑'),('8ce7254f-3063-11f1-8e5a-8c1645e938b5','1','2026-04-05 04:19:14.905','1','8ce21c3e-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','AddComment','Ëã•‰æùÂèëËµ∑ÊµÅÁ®ãÁî≥ËØ∑',_binary 'Ëã•‰æùÂèëËµ∑ÊµÅÁ®ãÁî≥ËØ∑');
/*!40000 ALTER TABLE `act_hi_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_info`
--

DROP TABLE IF EXISTS `act_id_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_info` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PASSWORD_` longblob,
  `PARENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_info`
--

LOCK TABLES `act_id_info` WRITE;
/*!40000 ALTER TABLE `act_id_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_app_appdef`
--

DROP TABLE IF EXISTS `act_app_appdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_appdef` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `VERSION_` int NOT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_APP_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`),
  KEY `ACT_IDX_APP_DEF_DPLY` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_APP_DEF_DPLY` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_app_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_app_appdef`
--

LOCK TABLES `act_app_appdef` WRITE;
/*!40000 ALTER TABLE `act_app_appdef` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_app_appdef` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT 'ÂÖ¨ÂëäID',
  `notice_title` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÂÖ¨ÂëäÊ†áÈ¢ò',
  `notice_type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÂÖ¨ÂëäÁ±ªÂûãÔºà1ÈÄöÁü• 2ÂÖ¨ÂëäÔºâ',
  `notice_content` longblob COMMENT 'ÂÖ¨ÂëäÂÜÖÂÆπ',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'ÂÖ¨ÂëäÁä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÂÖ≥Èó≠Ôºâ',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ÈÄöÁü•ÂÖ¨ÂëäË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notice`
--

LOCK TABLES `sys_notice` WRITE;
/*!40000 ALTER TABLE `sys_notice` DISABLE KEYS */;
INSERT INTO `sys_notice` VALUES (1,'Ê∏©È¶®ÊèêÈÜíÔºö2018-07-01 Â¢®ÈüµÊñ∞ÁâàÊú¨ÂèëÂ∏ÉÂï¶','2',_binary 'Êñ∞ÁâàÊú¨ÂÜÖÂÆπ','0','admin','2026-04-22 01:46:30','',NULL,'ÁÆ°ÁêÜÂëò'),(2,'Áª¥Êä§ÈÄöÁü•Ôºö2018-07-01 Â¢®ÈüµÁ≥ªÁªüÂáåÊô®Áª¥Êä§','1',_binary 'Áª¥Êä§ÂÜÖÂÆπ','0','admin','2026-04-22 01:46:30','',NULL,'ÁÆ°ÁêÜÂëò');
/*!40000 ALTER TABLE `sys_notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_job_details`
--

DROP TABLE IF EXISTS `qrtz_job_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_job_details` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `job_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '‰ªªÂä°ÂêçÁß∞',
  `job_group` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '‰ªªÂä°ÁªÑÂêç',
  `description` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Áõ∏ÂÖ≥‰ªãÁªç',
  `job_class_name` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÊâßË°å‰ªªÂä°Á±ªÂêçÁß∞',
  `is_durable` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÊòØÂê¶ÊåÅ‰πÖÂåñ',
  `is_nonconcurrent` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÊòØÂê¶Âπ∂Âèë',
  `is_update_data` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÊòØÂê¶Êõ¥Êñ∞Êï∞ÊçÆ',
  `requests_recovery` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÊòØÂê¶Êé•ÂèóÊÅ¢Â§çÊâßË°å',
  `job_data` blob COMMENT 'Â≠òÊîæÊåÅ‰πÖÂåñjobÂØπË±°',
  PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='‰ªªÂä°ËØ¶ÁªÜ‰ø°ÊÅØË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_job_details`
--

LOCK TABLES `qrtz_job_details` WRITE;
/*!40000 ALTER TABLE `qrtz_job_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_job_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Â≤ó‰ΩçID',
  `post_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Â≤ó‰ΩçÁºñÁ†Å',
  `post_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Â≤ó‰ΩçÂêçÁß∞',
  `post_sort` int NOT NULL COMMENT 'ÊòæÁ§∫È°∫Â∫è',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Áä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÂÅúÁî®Ôºâ',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`post_id`),
  UNIQUE KEY `uk_sys_post_code` (`post_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Â≤ó‰Ωç‰ø°ÊÅØË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_post`
--

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post` DISABLE KEYS */;
INSERT INTO `sys_post` VALUES (1,'ceo','Ëë£‰∫ãÈïø',1,'0','admin','2026-04-22 01:46:27','',NULL,''),(2,'se','È°πÁõÆÁªèÁêÜ',2,'0','admin','2026-04-22 01:46:27','',NULL,''),(3,'hr','‰∫∫ÂäõËµÑÊ∫ê',3,'0','admin','2026-04-22 01:46:27','',NULL,''),(4,'user','ÊôÆÈÄöÂëòÂ∑•',4,'0','admin','2026-04-22 01:46:27','',NULL,'');
/*!40000 ALTER TABLE `sys_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_hi_case_inst`
--

DROP TABLE IF EXISTS `act_cmmn_hi_case_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_hi_case_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_REACTIVATION_TIME_` datetime(3) DEFAULT NULL,
  `LAST_REACTIVATION_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_CASE_INST_END` (`END_TIME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_hi_case_inst`
--

LOCK TABLES `act_cmmn_hi_case_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_hi_case_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_hi_case_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_dmn_hi_decision_execution`
--

DROP TABLE IF EXISTS `act_dmn_hi_decision_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_hi_decision_execution` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `DECISION_DEFINITION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `INSTANCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXECUTION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ACTIVITY_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `FAILED_` tinyint DEFAULT '0',
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXECUTION_JSON_` longtext COLLATE utf8mb4_unicode_ci,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_DMN_INSTANCE_ID` (`INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_dmn_hi_decision_execution`
--

LOCK TABLES `act_dmn_hi_decision_execution` WRITE;
/*!40000 ALTER TABLE `act_dmn_hi_decision_execution` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_dmn_hi_decision_execution` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_event_resource`
--

DROP TABLE IF EXISTS `flw_event_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_resource` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_event_resource`
--

LOCK TABLES `flw_event_resource` WRITE;
/*!40000 ALTER TABLE `flw_event_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_event_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_ru_case_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_case_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_case_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LOCK_TIME_` datetime(3) DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_COMPLETEABLE_` tinyint DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_REACTIVATION_TIME_` datetime(3) DEFAULT NULL,
  `LAST_REACTIVATION_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_CASE_INST_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_CASE_INST_PARENT` (`PARENT_ID_`),
  KEY `ACT_IDX_CASE_INST_REF_ID_` (`REFERENCE_ID_`),
  CONSTRAINT `ACT_FK_CASE_INST_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_ru_case_inst`
--

LOCK TABLES `act_cmmn_ru_case_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_ru_case_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_ru_case_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_detail`
--

DROP TABLE IF EXISTS `act_hi_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_detail` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_TIME` (`TIME_`),
  KEY `ACT_IDX_HI_DETAIL_NAME` (`NAME_`),
  KEY `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_detail`
--

LOCK TABLES `act_hi_detail` WRITE;
/*!40000 ALTER TABLE `act_hi_detail` DISABLE KEYS */;
INSERT INTO `act_hi_detail` VALUES ('8c97576d-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'formJson','serializable',0,'2026-04-05 04:19:14.382','8c97576c-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL),('8c97576f-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'input20238','string',0,'2026-04-05 04:19:14.382',NULL,NULL,NULL,'Âú∞Êñπ ',NULL),('8c977e81-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'textarea77059','string',0,'2026-04-05 04:19:14.383',NULL,NULL,NULL,'Âú∞ÊñπÊòØ   ',NULL),('8c977e83-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'input18200','string',0,'2026-04-05 04:19:14.383',NULL,NULL,NULL,'a ÂèëÁöÑfa',NULL),('8c977e85-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'input86112','string',0,'2026-04-05 04:19:14.383',NULL,NULL,NULL,'aÁöÑÂèëÁöÑa\'d',NULL),('8c977e87-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'INITIATOR','long',0,'2026-04-05 04:19:14.383',NULL,NULL,1,'1',NULL),('8cb4555f-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'8c9841db-3063-11f1-8e5a-8c1645e938b5','formJson','serializable',1,'2026-04-05 04:19:14.572','8cb4555e-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL),('8cb4a380-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'8c9841db-3063-11f1-8e5a-8c1645e938b5','input20238','string',1,'2026-04-05 04:19:14.574',NULL,NULL,NULL,'Âú∞Êñπ ',NULL),('8cb4a381-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'8c9841db-3063-11f1-8e5a-8c1645e938b5','textarea77059','string',1,'2026-04-05 04:19:14.574',NULL,NULL,NULL,'Âú∞ÊñπÊòØ   ',NULL),('8cb4ca92-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'8c9841db-3063-11f1-8e5a-8c1645e938b5','input18200','string',1,'2026-04-05 04:19:14.575',NULL,NULL,NULL,'a ÂèëÁöÑfa',NULL),('8cb4f1a3-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'8c9841db-3063-11f1-8e5a-8c1645e938b5','input86112','string',1,'2026-04-05 04:19:14.576',NULL,NULL,NULL,'aÁöÑÂèëÁöÑa\'d',NULL),('8cb518b4-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'8c9841db-3063-11f1-8e5a-8c1645e938b5','INITIATOR','long',1,'2026-04-05 04:19:14.577',NULL,NULL,1,'1',NULL),('8ce1f51f-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'formJson','serializable',0,'2026-04-05 04:19:14.871','8ce1f51e-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL),('8ce1f521-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'input20238','string',0,'2026-04-05 04:19:14.871',NULL,NULL,NULL,'Âú∞Êñπ ',NULL),('8ce1f523-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'textarea77059','string',0,'2026-04-05 04:19:14.871',NULL,NULL,NULL,'Âú∞ÊñπÊòØ   ',NULL),('8ce1f525-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'input18200','string',0,'2026-04-05 04:19:14.871',NULL,NULL,NULL,'a ÂèëÁöÑfa',NULL),('8ce1f527-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'input86112','string',0,'2026-04-05 04:19:14.871',NULL,NULL,NULL,'aÁöÑÂèëÁöÑa\'d',NULL),('8ce21c39-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'INITIATOR','long',0,'2026-04-05 04:19:14.872',NULL,NULL,1,'1',NULL),('8ce8d301-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'8ce21c3d-3063-11f1-8e5a-8c1645e938b5','formJson','serializable',1,'2026-04-05 04:19:14.916','8ce8d300-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL),('8ce8fa12-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'8ce21c3d-3063-11f1-8e5a-8c1645e938b5','input20238','string',1,'2026-04-05 04:19:14.917',NULL,NULL,NULL,'Âú∞Êñπ ',NULL),('8ce94833-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'8ce21c3d-3063-11f1-8e5a-8c1645e938b5','textarea77059','string',1,'2026-04-05 04:19:14.919',NULL,NULL,NULL,'Âú∞ÊñπÊòØ   ',NULL),('8ce96f44-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'8ce21c3d-3063-11f1-8e5a-8c1645e938b5','input18200','string',1,'2026-04-05 04:19:14.920',NULL,NULL,NULL,'a ÂèëÁöÑfa',NULL),('8ce96f45-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'8ce21c3d-3063-11f1-8e5a-8c1645e938b5','input86112','string',1,'2026-04-05 04:19:14.920',NULL,NULL,NULL,'aÁöÑÂèëÁöÑa\'d',NULL),('8ce99656-3063-11f1-8e5a-8c1645e938b5','VariableUpdate','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'8ce21c3d-3063-11f1-8e5a-8c1645e938b5','INITIATOR','long',1,'2026-04-05 04:19:14.921',NULL,NULL,1,'1',NULL);
/*!40000 ALTER TABLE `act_hi_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_event_deployment`
--

DROP TABLE IF EXISTS `flw_event_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_deployment` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_event_deployment`
--

LOCK TABLES `flw_event_deployment` WRITE;
/*!40000 ALTER TABLE `flw_event_deployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_event_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ËèúÂçïID',
  `menu_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ËèúÂçïÂêçÁß∞',
  `parent_id` bigint DEFAULT '0' COMMENT 'Áà∂ËèúÂçïID',
  `order_num` int DEFAULT '0' COMMENT 'ÊòæÁ§∫È°∫Â∫è',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Ë∑ØÁî±Âú∞ÂùÄ',
  `component` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÁªÑ‰ª∂Ë∑ØÂæÑ',
  `query` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ë∑ØÁî±ÂèÇÊï∞',
  `route_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Ë∑ØÁî±ÂêçÁß∞',
  `is_frame` int DEFAULT '1' COMMENT 'ÊòØÂê¶‰∏∫Â§ñÈìæÔºà0ÊòØ 1Âê¶Ôºâ',
  `is_cache` int DEFAULT '0' COMMENT 'ÊòØÂê¶ÁºìÂ≠òÔºà0ÁºìÂ≠ò 1‰∏çÁºìÂ≠òÔºâ',
  `menu_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ËèúÂçïÁ±ªÂûãÔºàMÁõÆÂΩï CËèúÂçï FÊåâÈíÆÔºâ',
  `visible` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'ËèúÂçïÁä∂ÊÄÅÔºà0ÊòæÁ§∫ 1ÈöêËóèÔºâ',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'ËèúÂçïÁä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÂÅúÁî®Ôºâ',
  `perms` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊùÉÈôêÊ†áËØÜ',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '#' COMMENT 'ËèúÂçïÂõæÊ†á',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`menu_id`),
  KEY `idx_sys_menu_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200703 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ËèúÂçïÊùÉÈôêË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,'Á≥ªÁªüÁÆ°ÁêÜ',0,1,'system',NULL,'','',1,0,'M','0','0','','system','admin','2026-04-22 01:46:28','',NULL,'Á≥ªÁªüÁÆ°ÁêÜÁõÆÂΩï'),(2,'Á≥ªÁªüÁõëÊéß',0,2,'monitor',NULL,'','',1,0,'M','0','0','','monitor','admin','2026-04-22 01:46:28','',NULL,'Á≥ªÁªüÁõëÊéßÁõÆÂΩï'),(3,'Á≥ªÁªüÂ∑•ÂÖ∑',0,3,'tool',NULL,'','',1,0,'M','0','0','','tool','admin','2026-04-22 01:46:28','',NULL,'Á≥ªÁªüÂ∑•ÂÖ∑ÁõÆÂΩï'),(4,'Â¢®ÈüµÂÆòÁΩë',0,4,'http://localhost:3000/',NULL,'','',0,0,'M','0','0','','guide','admin','2026-04-22 01:46:28','admin','2026-05-27 02:35:53','Â¢®ÈüµÂÆòÁΩëÂú∞ÂùÄ'),(10,'ÊµÅÁ®ãÁÆ°ÁêÜ',0,10,'flowable',NULL,'','',1,0,'M','0','0','','tree','admin','2026-04-22 01:46:52','',NULL,'ÊµÅÁ®ãÁÆ°ÁêÜÁõÆÂΩï'),(100,'Áî®Êà∑ÁÆ°ÁêÜ',1,1,'user','system/user/index','','',1,0,'C','0','0','system:user:list','user','admin','2026-04-22 01:46:28','',NULL,'Áî®Êà∑ÁÆ°ÁêÜËèúÂçï'),(101,'ËßíËâ≤ÁÆ°ÁêÜ',1,2,'role','system/role/index','','',1,0,'C','0','0','system:role:list','peoples','admin','2026-04-22 01:46:28','',NULL,'ËßíËâ≤ÁÆ°ÁêÜËèúÂçï'),(102,'ËèúÂçïÁÆ°ÁêÜ',1,3,'menu','system/menu/index','','',1,0,'C','0','0','system:menu:list','tree-table','admin','2026-04-22 01:46:28','',NULL,'ËèúÂçïÁÆ°ÁêÜËèúÂçï'),(103,'ÈÉ®Èó®ÁÆ°ÁêÜ',1,4,'dept','system/dept/index','','',1,0,'C','0','0','system:dept:list','tree','admin','2026-04-22 01:46:28','',NULL,'ÈÉ®Èó®ÁÆ°ÁêÜËèúÂçï'),(104,'Â≤ó‰ΩçÁÆ°ÁêÜ',1,5,'post','system/post/index','','',1,0,'C','0','0','system:post:list','post','admin','2026-04-22 01:46:28','',NULL,'Â≤ó‰ΩçÁÆ°ÁêÜËèúÂçï'),(105,'Â≠óÂÖ∏ÁÆ°ÁêÜ',1,6,'dict','system/dict/index','','',1,0,'C','0','0','system:dict:list','dict','admin','2026-04-22 01:46:28','',NULL,'Â≠óÂÖ∏ÁÆ°ÁêÜËèúÂçï'),(106,'ÂèÇÊï∞ËÆæÁΩÆ',1,7,'config','system/config/index','','',1,0,'C','0','0','system:config:list','edit','admin','2026-04-22 01:46:28','',NULL,'ÂèÇÊï∞ËÆæÁΩÆËèúÂçï'),(107,'ÈÄöÁü•ÂÖ¨Âëä',1,8,'notice','system/notice/index','','',1,0,'C','0','0','system:notice:list','message','admin','2026-04-22 01:46:28','',NULL,'ÈÄöÁü•ÂÖ¨ÂëäËèúÂçï'),(108,'Êó•ÂøóÁÆ°ÁêÜ',1,9,'log','','','',1,0,'M','0','0','','log','admin','2026-04-22 01:46:28','',NULL,'Êó•ÂøóÁÆ°ÁêÜËèúÂçï'),(109,'Âú®Á∫øÁî®Êà∑',2,1,'online','monitor/online/index','','',1,0,'C','0','0','monitor:online:list','online','admin','2026-04-22 01:46:28','',NULL,'Âú®Á∫øÁî®Êà∑ËèúÂçï'),(110,'ÂÆöÊó∂‰ªªÂä°',2,2,'job','monitor/job/index','','',1,0,'C','0','0','monitor:job:list','job','admin','2026-04-22 01:46:28','',NULL,'ÂÆöÊó∂‰ªªÂä°ËèúÂçï'),(111,'Êï∞ÊçÆÁõëÊéß',2,3,'druid','monitor/druid/index','','',1,0,'C','0','0','monitor:druid:list','druid','admin','2026-04-22 01:46:28','',NULL,'Êï∞ÊçÆÁõëÊéßËèúÂçï'),(112,'ÊúçÂä°ÁõëÊéß',2,4,'server','monitor/server/index','','',1,0,'C','0','0','monitor:server:list','server','admin','2026-04-22 01:46:28','',NULL,'ÊúçÂä°ÁõëÊéßËèúÂçï'),(113,'ÁºìÂ≠òÁõëÊéß',2,5,'cache','monitor/cache/index','','',1,0,'C','0','0','monitor:cache:list','redis','admin','2026-04-22 01:46:28','',NULL,'ÁºìÂ≠òÁõëÊéßËèúÂçï'),(114,'ÁºìÂ≠òÂàóË°®',2,6,'cacheList','monitor/cache/list','','',1,0,'C','0','0','monitor:cache:list','redis-list','admin','2026-04-22 01:46:28','',NULL,'ÁºìÂ≠òÂàóË°®ËèúÂçï'),(115,'Ë°®ÂçïÊûÑÂª∫',3,1,'build','tool/build/index','','',1,0,'C','0','0','tool:build:list','build','admin','2026-04-22 01:46:28','',NULL,'Ë°®ÂçïÊûÑÂª∫ËèúÂçï'),(116,'‰ª£Á†ÅÁîüÊàê',3,2,'gen','tool/gen/index','','',1,0,'C','0','0','tool:gen:list','code','admin','2026-04-22 01:46:28','',NULL,'‰ª£Á†ÅÁîüÊàêËèúÂçï'),(117,'Á≥ªÁªüÊé•Âè£',3,3,'swagger','tool/swagger/index','','',1,0,'C','0','0','tool:swagger:list','swagger','admin','2026-04-22 01:46:28','',NULL,'Á≥ªÁªüÊé•Âè£ËèúÂçï'),(118,'ÊµÅÁ®ãÂÆö‰πâ',10,1,'definition','flowable/definition/index','','',1,0,'C','0','0','flowable:definition:list','tree-table','admin','2026-04-22 01:46:52','',NULL,'ÊµÅÁ®ãÂÆö‰πâËèúÂçï'),(119,'ÊµÅÁ®ã‰ªªÂä°',10,2,'task','flowable/task/index','','',1,0,'C','0','0','flowable:task:list','peoples','admin','2026-04-22 01:46:52','',NULL,'ÊµÅÁ®ã‰ªªÂä°ËèúÂçï'),(500,'Êìç‰ΩúÊó•Âøó',108,1,'operlog','monitor/operlog/index','','',1,0,'C','0','0','monitor:operlog:list','form','admin','2026-04-22 01:46:28','',NULL,'Êìç‰ΩúÊó•ÂøóËèúÂçï'),(501,'ÁôªÂΩïÊó•Âøó',108,2,'logininfor','monitor/logininfor/index','','',1,0,'C','0','0','monitor:logininfor:list','logininfor','admin','2026-04-22 01:46:28','',NULL,'ÁôªÂΩïÊó•ÂøóËèúÂçï'),(1000,'Áî®Êà∑Êü•ËØ¢',100,1,'','','','',1,0,'F','0','0','system:user:query','#','admin','2026-04-22 01:46:28','',NULL,''),(1001,'Áî®Êà∑Êñ∞Â¢û',100,2,'','','','',1,0,'F','0','0','system:user:add','#','admin','2026-04-22 01:46:28','',NULL,''),(1002,'Áî®Êà∑‰øÆÊîπ',100,3,'','','','',1,0,'F','0','0','system:user:edit','#','admin','2026-04-22 01:46:28','',NULL,''),(1003,'Áî®Êà∑Âà†Èô§',100,4,'','','','',1,0,'F','0','0','system:user:remove','#','admin','2026-04-22 01:46:28','',NULL,''),(1004,'Áî®Êà∑ÂØºÂá∫',100,5,'','','','',1,0,'F','0','0','system:user:export','#','admin','2026-04-22 01:46:28','',NULL,''),(1005,'Áî®Êà∑ÂØºÂÖ•',100,6,'','','','',1,0,'F','0','0','system:user:import','#','admin','2026-04-22 01:46:28','',NULL,''),(1006,'ÈáçÁΩÆÂØÜÁ†Å',100,7,'','','','',1,0,'F','0','0','system:user:resetPwd','#','admin','2026-04-22 01:46:28','',NULL,'');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_ru_mil_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_mil_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_mil_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TIME_STAMP_` datetime(3) NOT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_MIL_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_MIL_CASE_INST` (`CASE_INST_ID_`),
  CONSTRAINT `ACT_FK_MIL_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`),
  CONSTRAINT `ACT_FK_MIL_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `act_cmmn_ru_case_inst` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_ru_mil_inst`
--

LOCK TABLES `act_cmmn_ru_mil_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_ru_mil_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_ru_mil_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_event_definition`
--

DROP TABLE IF EXISTS `flw_event_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_definition` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_EVENT_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_event_definition`
--

LOCK TABLES `flw_event_definition` WRITE;
/*!40000 ALTER TABLE `flw_event_definition` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_event_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ËÆøÈóÆID',
  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Áî®Êà∑Ë¥¶Âè∑',
  `ipaddr` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÁôªÂΩïIPÂú∞ÂùÄ',
  `login_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÁôªÂΩïÂú∞ÁÇπ',
  `browser` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÊµèËßàÂô®Á±ªÂûã',
  `os` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êìç‰ΩúÁ≥ªÁªü',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'ÁôªÂΩïÁä∂ÊÄÅÔºà0ÊàêÂäü 1Â§±Ë¥•Ôºâ',
  `msg` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÊèêÁ§∫Ê∂àÊÅØ',
  `login_time` datetime DEFAULT NULL COMMENT 'ËÆøÈóÆÊó∂Èó¥',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Á≥ªÁªüËÆøÈóÆËÆ∞ÂΩï';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_logininfor`
--

LOCK TABLES `sys_logininfor` WRITE;
/*!40000 ALTER TABLE `sys_logininfor` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_logininfor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_variable`
--

DROP TABLE IF EXISTS `act_ru_variable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_variable` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_RU_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`),
  KEY `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`),
  KEY `ACT_FK_VAR_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_VAR_BYTEARRAY` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_variable`
--

LOCK TABLES `act_ru_variable` WRITE;
/*!40000 ALTER TABLE `act_ru_variable` DISABLE KEYS */;
INSERT INTO `act_ru_variable` VALUES ('8c97305a-3063-11f1-8e5a-8c1645e938b5',1,'serializable','formJson','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,'8c970949-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL),('8c97576e-3063-11f1-8e5a-8c1645e938b5',1,'string','input20238','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Âú∞Êñπ ',NULL,NULL),('8c975770-3063-11f1-8e5a-8c1645e938b5',1,'string','textarea77059','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Âú∞ÊñπÊòØ   ',NULL,NULL),('8c977e82-3063-11f1-8e5a-8c1645e938b5',1,'string','input18200','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'a ÂèëÁöÑfa',NULL,NULL),('8c977e84-3063-11f1-8e5a-8c1645e938b5',1,'string','input86112','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'aÁöÑÂèëÁöÑa\'d',NULL,NULL),('8c977e86-3063-11f1-8e5a-8c1645e938b5',1,'long','INITIATOR','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,1,'1',NULL,NULL),('8ce1f51c-3063-11f1-8e5a-8c1645e938b5',1,'serializable','formJson','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,'8ce1f51b-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL),('8ce1f520-3063-11f1-8e5a-8c1645e938b5',1,'string','input20238','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Âú∞Êñπ ',NULL,NULL),('8ce1f522-3063-11f1-8e5a-8c1645e938b5',1,'string','textarea77059','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Âú∞ÊñπÊòØ   ',NULL,NULL),('8ce1f524-3063-11f1-8e5a-8c1645e938b5',1,'string','input18200','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'a ÂèëÁöÑfa',NULL,NULL),('8ce1f526-3063-11f1-8e5a-8c1645e938b5',1,'string','input86112','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'aÁöÑÂèëÁöÑa\'d',NULL,NULL),('8ce1f528-3063-11f1-8e5a-8c1645e938b5',1,'long','INITIATOR','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,1,'1',NULL,NULL);
/*!40000 ALTER TABLE `act_ru_variable` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_attachment`
--

DROP TABLE IF EXISTS `act_hi_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_attachment` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `URL_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONTENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_attachment`
--

LOCK TABLES `act_hi_attachment` WRITE;
/*!40000 ALTER TABLE `act_hi_attachment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_identitylink`
--

DROP TABLE IF EXISTS `act_ru_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_identitylink` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_TSKASS_TASK` (`TASK_ID_`),
  KEY `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_IDL_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_TSKASS_TASK` FOREIGN KEY (`TASK_ID_`) REFERENCES `act_ru_task` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_identitylink`
--

LOCK TABLES `act_ru_identitylink` WRITE;
/*!40000 ALTER TABLE `act_ru_identitylink` DISABLE KEYS */;
INSERT INTO `act_ru_identitylink` VALUES ('8c95f7d8-3063-11f1-8e5a-8c1645e938b5',1,NULL,'starter','1',NULL,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL),('8cb518b5-3063-11f1-8e5a-8c1645e938b5',1,NULL,'participant','1',NULL,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL),('8ce1f51a-3063-11f1-8e5a-8c1645e938b5',1,NULL,'starter','1',NULL,'8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL),('8ce99657-3063-11f1-8e5a-8c1645e938b5',1,NULL,'participant','1',NULL,'8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `act_ru_identitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_simprop_triggers`
--

DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simprop_triggers` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `trigger_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_nameÁöÑÂ§ñÈîÆ',
  `trigger_group` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_groupÁöÑÂ§ñÈîÆ',
  `str_prop_1` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'StringÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∏Ä‰∏™ÂèÇÊï∞',
  `str_prop_2` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'StringÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∫å‰∏™ÂèÇÊï∞',
  `str_prop_3` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'StringÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∏â‰∏™ÂèÇÊï∞',
  `int_prop_1` int DEFAULT NULL COMMENT 'intÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∏Ä‰∏™ÂèÇÊï∞',
  `int_prop_2` int DEFAULT NULL COMMENT 'intÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∫å‰∏™ÂèÇÊï∞',
  `long_prop_1` bigint DEFAULT NULL COMMENT 'longÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∏Ä‰∏™ÂèÇÊï∞',
  `long_prop_2` bigint DEFAULT NULL COMMENT 'longÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∫å‰∏™ÂèÇÊï∞',
  `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimalÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∏Ä‰∏™ÂèÇÊï∞',
  `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimalÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∫å‰∏™ÂèÇÊï∞',
  `bool_prop_1` varchar(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BooleanÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∏Ä‰∏™ÂèÇÊï∞',
  `bool_prop_2` varchar(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BooleanÁ±ªÂûãÁöÑtriggerÁöÑÁ¨¨‰∫å‰∏™ÂèÇÊï∞',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ÂêåÊ≠•Êú∫Âà∂ÁöÑË°åÈîÅË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_simprop_triggers`
--

LOCK TABLES `qrtz_simprop_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_simprop_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_simprop_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_hi_plan_item_inst`
--

DROP TABLE IF EXISTS `act_cmmn_hi_plan_item_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_hi_plan_item_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STAGE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_STAGE_` tinyint DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_AVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_ENABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_DISABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_STARTED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_TIME_` datetime(3) DEFAULT NULL,
  `OCCURRED_TIME_` datetime(3) DEFAULT NULL,
  `TERMINATED_TIME_` datetime(3) DEFAULT NULL,
  `EXIT_TIME_` datetime(3) DEFAULT NULL,
  `ENDED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ENTRY_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `SHOW_IN_OVERVIEW_` tinyint DEFAULT NULL,
  `EXTRA_VALUE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DERIVED_CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_UNAVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PLAN_ITEM_INST_CASE` (`CASE_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_hi_plan_item_inst`
--

LOCK TABLES `act_cmmn_hi_plan_item_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_hi_plan_item_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_hi_plan_item_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_membership`
--

DROP TABLE IF EXISTS `act_id_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_membership` (
  `USER_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`USER_ID_`,`GROUP_ID_`),
  KEY `ACT_FK_MEMB_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_MEMB_GROUP` FOREIGN KEY (`GROUP_ID_`) REFERENCES `act_id_group` (`ID_`),
  CONSTRAINT `ACT_FK_MEMB_USER` FOREIGN KEY (`USER_ID_`) REFERENCES `act_id_user` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_membership`
--

LOCK TABLES `act_id_membership` WRITE;
/*!40000 ALTER TABLE `act_id_membership` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_membership` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_varinst`
--

DROP TABLE IF EXISTS `act_hi_varinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_varinst` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(100) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_EXE` (`EXECUTION_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_varinst`
--

LOCK TABLES `act_hi_varinst` WRITE;
/*!40000 ALTER TABLE `act_hi_varinst` DISABLE KEYS */;
INSERT INTO `act_hi_varinst` VALUES ('8c97305a-3063-11f1-8e5a-8c1645e938b5',1,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'formJson','serializable',NULL,NULL,NULL,'8c97576b-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,'2026-04-05 04:19:14.382','2026-04-05 04:19:14.572'),('8c97576e-3063-11f1-8e5a-8c1645e938b5',1,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'input20238','string',NULL,NULL,NULL,NULL,NULL,NULL,'Âú∞Êñπ ',NULL,NULL,'2026-04-05 04:19:14.382','2026-04-05 04:19:14.574'),('8c975770-3063-11f1-8e5a-8c1645e938b5',1,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'textarea77059','string',NULL,NULL,NULL,NULL,NULL,NULL,'Âú∞ÊñπÊòØ   ',NULL,NULL,'2026-04-05 04:19:14.383','2026-04-05 04:19:14.574'),('8c977e82-3063-11f1-8e5a-8c1645e938b5',1,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'input18200','string',NULL,NULL,NULL,NULL,NULL,NULL,'a ÂèëÁöÑfa',NULL,NULL,'2026-04-05 04:19:14.383','2026-04-05 04:19:14.575'),('8c977e84-3063-11f1-8e5a-8c1645e938b5',1,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'input86112','string',NULL,NULL,NULL,NULL,NULL,NULL,'aÁöÑÂèëÁöÑa\'d',NULL,NULL,'2026-04-05 04:19:14.383','2026-04-05 04:19:14.576'),('8c977e86-3063-11f1-8e5a-8c1645e938b5',1,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'INITIATOR','long',NULL,NULL,NULL,NULL,NULL,1,'1',NULL,NULL,'2026-04-05 04:19:14.383','2026-04-05 04:19:14.577'),('8ce1f51c-3063-11f1-8e5a-8c1645e938b5',1,'8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'formJson','serializable',NULL,NULL,NULL,'8ce1f51d-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,'2026-04-05 04:19:14.871','2026-04-05 04:19:14.916'),('8ce1f520-3063-11f1-8e5a-8c1645e938b5',1,'8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'input20238','string',NULL,NULL,NULL,NULL,NULL,NULL,'Âú∞Êñπ ',NULL,NULL,'2026-04-05 04:19:14.871','2026-04-05 04:19:14.917'),('8ce1f522-3063-11f1-8e5a-8c1645e938b5',1,'8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'textarea77059','string',NULL,NULL,NULL,NULL,NULL,NULL,'Âú∞ÊñπÊòØ   ',NULL,NULL,'2026-04-05 04:19:14.871','2026-04-05 04:19:14.919'),('8ce1f524-3063-11f1-8e5a-8c1645e938b5',1,'8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'input18200','string',NULL,NULL,NULL,NULL,NULL,NULL,'a ÂèëÁöÑfa',NULL,NULL,'2026-04-05 04:19:14.871','2026-04-05 04:19:14.920'),('8ce1f526-3063-11f1-8e5a-8c1645e938b5',1,'8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'input86112','string',NULL,NULL,NULL,NULL,NULL,NULL,'aÁöÑÂèëÁöÑa\'d',NULL,NULL,'2026-04-05 04:19:14.871','2026-04-05 04:19:14.920'),('8ce1f528-3063-11f1-8e5a-8c1645e938b5',1,'8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'INITIATOR','long',NULL,NULL,NULL,NULL,NULL,1,'1',NULL,NULL,'2026-04-05 04:19:14.871','2026-04-05 04:19:14.921');
/*!40000 ALTER TABLE `act_hi_varinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Áî®Êà∑ID',
  `dept_id` bigint DEFAULT NULL COMMENT 'ÈÉ®Èó®ID',
  `user_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Áî®Êà∑Ë¥¶Âè∑',
  `nick_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Áî®Êà∑ÊòµÁß∞',
  `user_type` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT '00' COMMENT 'Áî®Êà∑Á±ªÂûãÔºà00Á≥ªÁªüÁî®Êà∑Ôºâ',
  `user_source` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'admin' COMMENT 'Áî®Êà∑Êù•Ê∫êÔºàadminÂêéÂè∞/systemÁ≥ªÁªü/appÂâçÂè∞Ôºâ',
  `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Áî®Êà∑ÈÇÆÁÆ±',
  `phonenumber` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÊâãÊú∫Âè∑Á†Å',
  `sex` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'Áî®Êà∑ÊÄßÂà´Ôºà0Áî∑ 1Â•≥ 2Êú™Áü•Ôºâ',
  `avatar` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Â§¥ÂÉèÂú∞ÂùÄ',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂØÜÁ†Å',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'Â∏êÂè∑Áä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÂÅúÁî®Ôºâ',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'Âà†Èô§Ê†áÂøóÔºà0‰ª£Ë°®Â≠òÂú® 2‰ª£Ë°®Âà†Èô§Ôºâ',
  `login_ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÊúÄÂêéÁôªÂΩïIP',
  `login_date` datetime DEFAULT NULL COMMENT 'ÊúÄÂêéÁôªÂΩïÊó∂Èó¥',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_sys_user_name` (`user_name`),
  KEY `idx_sys_user_dept` (`dept_id`),
  KEY `idx_sys_user_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Áî®Êà∑‰ø°ÊÅØË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,103,'admin','Â¢®Èüµ','00','admin','ry@163.com','15888888888','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1','2026-05-27 02:32:48','admin','2026-04-22 01:46:27','','2026-05-27 02:32:48','ÁÆ°ÁêÜÂëò'),(2,105,'ry','Â¢®Èüµ','00','admin','ry@qq.com','15666666666','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1','2026-04-22 01:46:27','admin','2026-04-22 01:46:27','admin','2026-05-27 02:37:09','ÊµãËØïÂëò');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_user`
--

DROP TABLE IF EXISTS `act_id_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_user` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `FIRST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LAST_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DISPLAY_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EMAIL_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PWD_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PICTURE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_user`
--

LOCK TABLES `act_id_user` WRITE;
/*!40000 ALTER TABLE `act_id_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_token`
--

DROP TABLE IF EXISTS `act_id_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_token` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TOKEN_VALUE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATE_` timestamp(3) NULL DEFAULT NULL,
  `IP_ADDRESS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_AGENT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATA_` varchar(2000) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_token`
--

LOCK TABLES `act_id_token` WRITE;
/*!40000 ALTER TABLE `act_id_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ÁºñÂè∑',
  `table_id` bigint DEFAULT NULL COMMENT 'ÂΩíÂ±ûË°®ÁºñÂè∑',
  `column_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÂàóÂêçÁß∞',
  `column_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÂàóÊèèËø∞',
  `column_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÂàóÁ±ªÂûã',
  `java_type` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JAVAÁ±ªÂûã',
  `java_field` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JAVAÂ≠óÊÆµÂêç',
  `is_pk` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòØÂê¶‰∏ªÈîÆÔºà1ÊòØÔºâ',
  `is_increment` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòØÂê¶Ëá™Â¢ûÔºà1ÊòØÔºâ',
  `is_required` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòØÂê¶ÂøÖÂ°´Ôºà1ÊòØÔºâ',
  `is_insert` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòØÂê¶‰∏∫ÊèíÂÖ•Â≠óÊÆµÔºà1ÊòØÔºâ',
  `is_edit` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòØÂê¶ÁºñËæëÂ≠óÊÆµÔºà1ÊòØÔºâ',
  `is_list` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòØÂê¶ÂàóË°®Â≠óÊÆµÔºà1ÊòØÔºâ',
  `is_query` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòØÂê¶Êü•ËØ¢Â≠óÊÆµÔºà1ÊòØÔºâ',
  `query_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT 'EQ' COMMENT 'Êü•ËØ¢ÊñπÂºèÔºàÁ≠â‰∫é„ÄÅ‰∏çÁ≠â‰∫é„ÄÅÂ§ß‰∫é„ÄÅÂ∞è‰∫é„ÄÅËåÉÂõ¥Ôºâ',
  `html_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòæÁ§∫Á±ªÂûãÔºàÊñáÊú¨Ê°Ü„ÄÅÊñáÊú¨Âüü„ÄÅ‰∏ãÊãâÊ°Ü„ÄÅÂ§çÈÄâÊ°Ü„ÄÅÂçïÈÄâÊ°Ü„ÄÅÊó•ÊúüÊéß‰ª∂Ôºâ',
  `dict_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Â≠óÂÖ∏Á±ªÂûã',
  `sort` int DEFAULT NULL COMMENT 'ÊéíÂ∫è',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  PRIMARY KEY (`column_id`),
  KEY `idx_gen_table_column_table` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='‰ª£Á†ÅÁîüÊàê‰∏öÂä°Ë°®Â≠óÊÆµ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table_column`
--

LOCK TABLES `gen_table_column` WRITE;
/*!40000 ALTER TABLE `gen_table_column` DISABLE KEYS */;
/*!40000 ALTER TABLE `gen_table_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_channel_definition`
--

DROP TABLE IF EXISTS `flw_channel_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_channel_definition` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IMPLEMENTATION_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_CHANNEL_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_channel_definition`
--

LOCK TABLES `flw_channel_definition` WRITE;
/*!40000 ALTER TABLE `flw_channel_definition` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_channel_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_job`
--

DROP TABLE IF EXISTS `act_ru_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_JOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_job`
--

LOCK TABLES `act_ru_job` WRITE;
/*!40000 ALTER TABLE `act_ru_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_scheduler_state`
--

DROP TABLE IF EXISTS `qrtz_scheduler_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_scheduler_state` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `instance_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÂÆû‰æãÂêçÁß∞',
  `last_checkin_time` bigint NOT NULL COMMENT '‰∏äÊ¨°Ê£ÄÊü•Êó∂Èó¥',
  `checkin_interval` bigint NOT NULL COMMENT 'Ê£ÄÊü•Èó¥ÈöîÊó∂Èó¥',
  PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ë∞ÉÂ∫¶Âô®Áä∂ÊÄÅË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_scheduler_state`
--

LOCK TABLES `qrtz_scheduler_state` WRITE;
/*!40000 ALTER TABLE `qrtz_scheduler_state` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_scheduler_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_event_subscr`
--

DROP TABLE IF EXISTS `act_ru_event_subscr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_event_subscr` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EVENT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EVENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTIVITY_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CONFIGURATION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATED_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`),
  KEY `ACT_IDX_EVENT_SUBSCR_EXEC_ID` (`EXECUTION_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_PROC_ID` (`PROC_INST_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_SCOPEREF_` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EVENT_EXEC` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_event_subscr`
--

LOCK TABLES `act_ru_event_subscr` WRITE;
/*!40000 ALTER TABLE `act_ru_event_subscr` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_event_subscr` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT 'ËßíËâ≤ID',
  `menu_id` bigint NOT NULL COMMENT 'ËèúÂçïID',
  PRIMARY KEY (`role_id`,`menu_id`),
  KEY `idx_sys_role_menu_role` (`role_id`),
  KEY `idx_sys_role_menu_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ËßíËâ≤ÂíåËèúÂçïÂÖ≥ËÅîË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,1),(1,2),(1,3),(1,4),(1,10),(1,100),(1,101),(1,102),(1,103),(1,104),(1,105),(1,106),(1,107),(1,108),(1,109),(1,110),(1,111),(1,112),(1,113),(1,114),(1,115),(1,116),(1,117),(1,118),(1,119),(1,500),(1,501),(1,1000),(1,1001),(1,1002),(1,1003),(1,1004),(1,1005),(1,1006);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ge_property`
--

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

--
-- Dumping data for table `act_ge_property`
--

LOCK TABLES `act_ge_property` WRITE;
/*!40000 ALTER TABLE `act_ge_property` DISABLE KEYS */;
INSERT INTO `act_ge_property` VALUES ('app.schema.version','7.1.0.2',1),('cfg.execution-related-entities-count','true',1),('cfg.task-related-entities-count','true',1),('cmmn.schema.version','7.1.0.2',1),('common.schema.version','7.1.0.2',1),('dmn.schema.version','7.1.0.2',1),('eventregistry.schema.version','7.1.0.2',1),('next.dbid','1',1),('schema.history','create(7.1.0.2)',1),('schema.version','7.1.0.2',1);
/*!40000 ALTER TABLE `act_ge_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_bytearray`
--

DROP TABLE IF EXISTS `act_id_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_bytearray` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_bytearray`
--

LOCK TABLES `act_id_bytearray` WRITE;
/*!40000 ALTER TABLE `act_id_bytearray` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_bytearray` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Êó•Âøó‰∏ªÈîÆ',
  `title` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Ê®°ÂùóÊ†áÈ¢ò',
  `business_type` int DEFAULT '0' COMMENT '‰∏öÂä°Á±ªÂûãÔºà0ÂÖ∂ÂÆÉ 1Êñ∞Â¢û 2‰øÆÊîπ 3Âà†Èô§Ôºâ',
  `method` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÊñπÊ≥ïÂêçÁß∞',
  `request_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ËØ∑Ê±ÇÊñπÂºè',
  `operator_type` int DEFAULT '0' COMMENT 'Êìç‰ΩúÁ±ªÂà´Ôºà0ÂÖ∂ÂÆÉ 1ÂêéÂè∞Áî®Êà∑ 2ÊâãÊú∫Á´ØÁî®Êà∑Ôºâ',
  `oper_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êìç‰Ωú‰∫∫Âëò',
  `dept_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÈÉ®Èó®ÂêçÁß∞',
  `oper_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ËØ∑Ê±ÇURL',
  `oper_ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '‰∏ªÊú∫Âú∞ÂùÄ',
  `oper_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êìç‰ΩúÂú∞ÁÇπ',
  `oper_param` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ËØ∑Ê±ÇÂèÇÊï∞',
  `json_result` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ËøîÂõûÂèÇÊï∞',
  `status` int DEFAULT '0' COMMENT 'Êìç‰ΩúÁä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÂºÇÂ∏∏Ôºâ',
  `error_msg` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÈîôËØØÊ∂àÊÅØ',
  `oper_time` datetime DEFAULT NULL COMMENT 'Êìç‰ΩúÊó∂Èó¥',
  `cost_time` bigint DEFAULT '0' COMMENT 'Ê∂àËÄóÊó∂Èó¥',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Êìç‰ΩúÊó•ÂøóËÆ∞ÂΩï';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_group`
--

DROP TABLE IF EXISTS `act_id_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_group` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_group`
--

LOCK TABLES `act_id_group` WRITE;
/*!40000 ALTER TABLE `act_id_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_property`
--

DROP TABLE IF EXISTS `act_id_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_property` (
  `NAME_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
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
-- Table structure for table `qrtz_simple_triggers`
--

DROP TABLE IF EXISTS `qrtz_simple_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simple_triggers` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `trigger_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_nameÁöÑÂ§ñÈîÆ',
  `trigger_group` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_groupÁöÑÂ§ñÈîÆ',
  `repeat_count` bigint NOT NULL COMMENT 'ÈáçÂ§çÁöÑÊ¨°Êï∞ÁªüËÆ°',
  `repeat_interval` bigint NOT NULL COMMENT 'ÈáçÂ§çÁöÑÈó¥ÈöîÊó∂Èó¥',
  `times_triggered` bigint NOT NULL COMMENT 'Â∑≤ÁªèËß¶ÂèëÁöÑÊ¨°Êï∞',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ÁÆÄÂçïËß¶ÂèëÂô®ÁöÑ‰ø°ÊÅØË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_simple_triggers`
--

LOCK TABLES `qrtz_simple_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_simple_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_simple_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_form`
--

DROP TABLE IF EXISTS `sys_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_form` (
  `form_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Ë°®Âçï‰∏ªÈîÆ',
  `form_name` varchar(50) DEFAULT NULL COMMENT 'Ë°®ÂçïÂêçÁß∞',
  `form_content` longtext COMMENT 'Ë°®ÂçïÂÜÖÂÆπ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `create_by` bigint DEFAULT NULL COMMENT 'ÂàõÂª∫‰∫∫Âëò',
  `update_by` bigint DEFAULT NULL COMMENT 'Êõ¥Êñ∞‰∫∫Âëò',
  `remark` varchar(255) DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ÊµÅÁ®ãË°®Âçï';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_form`
--

LOCK TABLES `sys_form` WRITE;
/*!40000 ALTER TABLE `sys_form` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_form` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_job`
--

DROP TABLE IF EXISTS `sys_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '‰ªªÂä°ID',
  `job_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '‰ªªÂä°ÂêçÁß∞',
  `job_group` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '‰ªªÂä°ÁªÑÂêç',
  `invoke_target` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÁî®ÁõÆÊ†áÂ≠óÁ¨¶‰∏≤',
  `cron_expression` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'cronÊâßË°åË°®ËææÂºè',
  `misfire_policy` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '3' COMMENT 'ËÆ°ÂàíÊâßË°åÈîôËØØÁ≠ñÁï•Ôºà1Á´ãÂç≥ÊâßË°å 2ÊâßË°å‰∏ÄÊ¨° 3ÊîæÂºÉÊâßË°åÔºâ',
  `concurrent` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT 'ÊòØÂê¶Âπ∂ÂèëÊâßË°åÔºà0ÂÖÅËÆ∏ 1Á¶ÅÊ≠¢Ôºâ',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'Áä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÊöÇÂÅúÔºâ',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Â§áÊ≥®‰ø°ÊÅØ',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`),
  KEY `idx_sys_job_group` (`job_group`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ÂÆöÊó∂‰ªªÂä°Ë∞ÉÂ∫¶Ë°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_job`
--

LOCK TABLES `sys_job` WRITE;
/*!40000 ALTER TABLE `sys_job` DISABLE KEYS */;
INSERT INTO `sys_job` VALUES (1,'Á≥ªÁªüÈªòËÆ§ÔºàÊó†ÂèÇÔºâ','DEFAULT','ryTask.ryNoParams','0/10 * * * * ?','3','1','1','admin','2026-04-22 01:46:41','',NULL,''),(2,'Á≥ªÁªüÈªòËÆ§ÔºàÊúâÂèÇÔºâ','DEFAULT','ryTask.ryParams(\'ry\')','0/15 * * * * ?','3','1','1','admin','2026-04-22 01:46:41','',NULL,''),(3,'Á≥ªÁªüÈªòËÆ§ÔºàÂ§öÂèÇÔºâ','DEFAULT','ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)','0/20 * * * * ?','3','1','1','admin','2026-04-22 01:46:41','',NULL,'');
/*!40000 ALTER TABLE `sys_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_calendars`
--

DROP TABLE IF EXISTS `qrtz_calendars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_calendars` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `calendar_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Êó•ÂéÜÂêçÁß∞',
  `calendar` blob NOT NULL COMMENT 'Â≠òÊîæÊåÅ‰πÖÂåñcalendarÂØπË±°',
  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Êó•ÂéÜ‰ø°ÊÅØË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_calendars`
--

LOCK TABLES `qrtz_calendars` WRITE;
/*!40000 ALTER TABLE `qrtz_calendars` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_calendars` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_profile`
--

DROP TABLE IF EXISTS `sys_user_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_profile` (
  `user_id` bigint NOT NULL COMMENT 'Áî®Êà∑ID',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòµÁß∞',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â§¥ÂÉèURL',
  `bio` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '‰∏™‰∫∫ÁÆÄ‰ªã',
  `gender` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'ÊÄßÂà´Ôºà0Êú™Áü• 1Áî∑ 2Â•≥Ôºâ',
  `birthday` date DEFAULT NULL COMMENT 'ÁîüÊó•',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊâÄÂú®Âú∞',
  `website` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '‰∏™‰∫∫ÁΩëÁ´ô',
  `social_links` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Á§æ‰∫§ÈìæÊé•JSON',
  `points` int DEFAULT '0' COMMENT 'ÁßØÂàÜ',
  `level` int DEFAULT '1' COMMENT 'Á≠âÁ∫ß',
  `ink_balance` decimal(10,2) DEFAULT '0.00' COMMENT 'Â¢®Ê∞¥‰ΩôÈ¢ù',
  `total_read_time` bigint DEFAULT '0' COMMENT 'Á¥ØËÆ°ÈòÖËØªÊó∂ÈïøÔºàÂàÜÈíüÔºâ',
  `total_word_count` bigint DEFAULT '0' COMMENT 'Á¥ØËÆ°Âàõ‰ΩúÂ≠óÊï∞',
  `article_count` int DEFAULT '0' COMMENT 'ÊñáÁ´†Êï∞Èáè',
  `follower_count` int DEFAULT '0' COMMENT 'Á≤â‰∏ùÊï∞',
  `following_count` int DEFAULT '0' COMMENT 'ÂÖ≥Ê≥®Êï∞',
  `like_count` bigint DEFAULT '0' COMMENT 'Á¥ØËÆ°Ëé∑ËµûÊï∞',
  `article_view_count` bigint DEFAULT '0' COMMENT 'Á¥ØËÆ°ÊñáÁ´†ÈòÖËØªÊï∞',
  `is_author` tinyint(1) DEFAULT '0' COMMENT 'ÊòØÂê¶ËÆ§ËØÅ‰ΩúËÄÖÔºà0Âê¶ 1ÊòØÔºâ',
  `author_level` int DEFAULT '0' COMMENT '‰ΩúËÄÖÁ≠âÁ∫ß',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Áî®Êà∑Êâ©Â±ï‰ø°ÊÅØË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_profile`
--

LOCK TABLES `sys_user_profile` WRITE;
/*!40000 ALTER TABLE `sys_user_profile` DISABLE KEYS */;
INSERT INTO `sys_user_profile` VALUES (1,'ÁÆ°ÁêÜÂëò',NULL,'Á≥ªÁªüÁÆ°ÁêÜÂëò','0',NULL,NULL,NULL,NULL,1000,10,100.00,0,0,0,0,0,0,0,0,0,'2026-04-22 01:46:58','2026-04-22 01:46:58'),(2,'Â¢®Èüµ',NULL,'ÊµãËØïÁî®Êà∑','0',NULL,NULL,NULL,NULL,100,1,10.00,0,0,0,0,0,0,0,0,0,'2026-04-22 01:46:58','2026-04-22 01:46:58');
/*!40000 ALTER TABLE `sys_user_profile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_priv`
--

DROP TABLE IF EXISTS `act_id_priv`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_priv` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PRIV_NAME` (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_priv`
--

LOCK TABLES `act_id_priv` WRITE;
/*!40000 ALTER TABLE `act_id_priv` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_priv` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_fired_triggers`
--

DROP TABLE IF EXISTS `qrtz_fired_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_fired_triggers` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `entry_id` varchar(95) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶Âô®ÂÆû‰æãid',
  `trigger_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_nameÁöÑÂ§ñÈîÆ',
  `trigger_group` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_groupÁöÑÂ§ñÈîÆ',
  `instance_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶Âô®ÂÆû‰æãÂêç',
  `fired_time` bigint NOT NULL COMMENT 'Ëß¶ÂèëÁöÑÊó∂Èó¥',
  `sched_time` bigint NOT NULL COMMENT 'ÂÆöÊó∂Âô®Âà∂ÂÆöÁöÑÊó∂Èó¥',
  `priority` int NOT NULL COMMENT '‰ºòÂÖàÁ∫ß',
  `state` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Áä∂ÊÄÅ',
  `job_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '‰ªªÂä°ÂêçÁß∞',
  `job_group` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '‰ªªÂä°ÁªÑÂêç',
  `is_nonconcurrent` varchar(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòØÂê¶Âπ∂Âèë',
  `requests_recovery` varchar(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊòØÂê¶Êé•ÂèóÊÅ¢Â§çÊâßË°å',
  PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Â∑≤Ëß¶ÂèëÁöÑËß¶ÂèëÂô®Ë°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_fired_triggers`
--

LOCK TABLES `qrtz_fired_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_fired_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_fired_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_deadletter_job`
--

DROP TABLE IF EXISTS `act_ru_deadletter_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_deadletter_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_DJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_DEADLETTER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_deadletter_job`
--

LOCK TABLES `act_ru_deadletter_job` WRITE;
/*!40000 ALTER TABLE `act_ru_deadletter_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_deadletter_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_ru_batch_part`
--

DROP TABLE IF EXISTS `flw_ru_batch_part`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_ru_batch_part` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `BATCH_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESULT_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `FLW_IDX_BATCH_PART` (`BATCH_ID_`),
  CONSTRAINT `FLW_FK_BATCH_PART_PARENT` FOREIGN KEY (`BATCH_ID_`) REFERENCES `flw_ru_batch` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_ru_batch_part`
--

LOCK TABLES `flw_ru_batch_part` WRITE;
/*!40000 ALTER TABLE `flw_ru_batch_part` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_ru_batch_part` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_entitylink`
--

DROP TABLE IF EXISTS `act_hi_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_entitylink` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_entitylink`
--

LOCK TABLES `act_hi_entitylink` WRITE;
/*!40000 ALTER TABLE `act_hi_entitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_entitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_priv_mapping`
--

DROP TABLE IF EXISTS `act_id_priv_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_priv_mapping` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PRIV_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_PRIV_MAPPING` (`PRIV_ID_`),
  KEY `ACT_IDX_PRIV_USER` (`USER_ID_`),
  KEY `ACT_IDX_PRIV_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_PRIV_MAPPING` FOREIGN KEY (`PRIV_ID_`) REFERENCES `act_id_priv` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_priv_mapping`
--

LOCK TABLES `act_id_priv_mapping` WRITE;
/*!40000 ALTER TABLE `act_id_priv_mapping` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_priv_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_task`
--

DROP TABLE IF EXISTS `act_ru_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_task` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELEGATION_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `SUB_TASK_COUNT_` int DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`),
  KEY `ACT_IDX_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TASK_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TASK_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_task`
--

LOCK TABLES `act_ru_task` WRITE;
/*!40000 ALTER TABLE `act_ru_task` DISABLE KEYS */;
INSERT INTO `act_ru_task` VALUES ('8cb60318-3063-11f1-8e5a-8c1645e938b5',1,'8c977e88-3063-11f1-8e5a-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,'created','Â§çÂÆ°',NULL,NULL,'Activity_0v17nhy',NULL,NULL,NULL,50,'2026-04-04 20:19:14.583',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,'',NULL,1,0,0,0),('8cea59aa-3063-11f1-8e5a-8c1645e938b5',1,'8ce21c3a-3063-11f1-8e5a-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,NULL,'created','Â§çÂÆ°',NULL,NULL,'Activity_0v17nhy',NULL,NULL,NULL,50,'2026-04-04 20:19:14.926',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,'',NULL,1,0,0,0);
/*!40000 ALTER TABLE `act_ru_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_procdef_info`
--

DROP TABLE IF EXISTS `act_procdef_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_procdef_info` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `INFO_JSON_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`),
  CONSTRAINT `ACT_FK_INFO_JSON_BA` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_INFO_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_procdef_info`
--

LOCK TABLES `act_procdef_info` WRITE;
/*!40000 ALTER TABLE `act_procdef_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_procdef_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_entitylink`
--

DROP TABLE IF EXISTS `act_ru_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_entitylink` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_entitylink`
--

LOCK TABLES `act_ru_entitylink` WRITE;
/*!40000 ALTER TABLE `act_ru_entitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_entitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_listener`
--

DROP TABLE IF EXISTS `sys_listener`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_listener` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '‰∏ªÈîÆ',
  `name` varchar(128) DEFAULT NULL COMMENT 'ÂêçÁß∞',
  `type` varchar(64) DEFAULT NULL COMMENT 'ÁõëÂê¨Á±ªÂûã',
  `event_type` varchar(64) DEFAULT NULL COMMENT 'ÁõëÂê¨‰∫ã‰ª∂Á±ªÂûã',
  `value_type` varchar(64) DEFAULT NULL COMMENT 'ÁõëÂê¨ÂÄºÁ±ªÂûã',
  `value` varchar(255) DEFAULT NULL COMMENT 'ÁõëÂê¨ÂÄº',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `create_by` bigint DEFAULT NULL COMMENT 'ÂàõÂª∫‰∫∫Âëò',
  `update_by` bigint DEFAULT NULL COMMENT 'Êõ¥Êñ∞‰∫∫Âëò',
  `status` tinyint DEFAULT '0' COMMENT 'Áä∂ÊÄÅ',
  `remark` varchar(255) DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`id`),
  KEY `idx_sys_listener_type` (`type`),
  KEY `idx_sys_listener_event` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ÊµÅÁ®ãÁõëÂê¨Âô®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_listener`
--

LOCK TABLES `sys_listener` WRITE;
/*!40000 ALTER TABLE `sys_listener` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_listener` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_tsk_log`
--

DROP TABLE IF EXISTS `act_hi_tsk_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_tsk_log` (
  `ID_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ACT_HI_TSK_LOG_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_tsk_log`
--

LOCK TABLES `act_hi_tsk_log` WRITE;
/*!40000 ALTER TABLE `act_hi_tsk_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_tsk_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT 'ÂèÇÊï∞‰∏ªÈîÆ',
  `config_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂèÇÊï∞ÂêçÁß∞',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂèÇÊï∞ÈîÆÂêç',
  `config_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂèÇÊï∞ÈîÆÂÄº',
  `config_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT 'Á≥ªÁªüÂÜÖÁΩÆÔºàYÊòØ NÂê¶Ôºâ',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_sys_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ÂèÇÊï∞ÈÖçÁΩÆË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` VALUES (1,'‰∏ªÊ°ÜÊû∂È°µ-ÈªòËÆ§ÁöÆËÇ§Ê†∑ÂºèÂêçÁß∞','sys.index.skinName','skin-blue','Y','admin','2026-04-22 01:46:29','',NULL,'ËìùËâ≤ skin-blue„ÄÅÁªøËâ≤ skin-green„ÄÅÁ¥´Ëâ≤ skin-purple„ÄÅÁ∫¢Ëâ≤ skin-red„ÄÅÈªÑËâ≤ skin-yellow'),(2,'Áî®Êà∑ÁÆ°ÁêÜ-Ë¥¶Âè∑ÂàùÂßãÂØÜÁ†Å','sys.user.initPassword','123456','Y','admin','2026-04-22 01:46:29','',NULL,'ÂàùÂßãÂåñÂØÜÁ†Å 123456'),(3,'‰∏ªÊ°ÜÊû∂È°µ-‰æßËæπÊ†è‰∏ªÈ¢ò','sys.index.sideTheme','theme-dark','Y','admin','2026-04-22 01:46:29','',NULL,'Ê∑±Ëâ≤‰∏ªÈ¢òtheme-darkÔºåÊµÖËâ≤‰∏ªÈ¢òtheme-light'),(4,'Ë¥¶Âè∑Ëá™Âä©-È™åËØÅÁ†ÅÂºÄÂÖ≥','sys.account.captchaEnabled','true','Y','admin','2026-04-22 01:46:30','',NULL,'ÊòØÂê¶ÂºÄÂêØÈ™åËØÅÁ†ÅÂäüËÉΩÔºàtrueÂºÄÂêØÔºåfalseÂÖ≥Èó≠Ôºâ'),(5,'Ë¥¶Âè∑Ëá™Âä©-ÊòØÂê¶ÂºÄÂêØÁî®Êà∑Ê≥®ÂÜåÂäüËÉΩ','sys.account.registerUser','false','Y','admin','2026-04-22 01:46:30','',NULL,'ÊòØÂê¶ÂºÄÂêØÊ≥®ÂÜåÁî®Êà∑ÂäüËÉΩÔºàtrueÂºÄÂêØÔºåfalseÂÖ≥Èó≠Ôºâ'),(6,'Áî®Êà∑ÁôªÂΩï-ÈªëÂêçÂçïÂàóË°®','sys.login.blackIPList','','Y','admin','2026-04-22 01:46:30','',NULL,'ËÆæÁΩÆÁôªÂΩïIPÈªëÂêçÂçïÈôêÂà∂ÔºåÂ§ö‰∏™ÂåπÈÖçÈ°π‰ª•;ÂàÜÈöîÔºåÊîØÊåÅÂåπÈÖçÔºà*ÈÄöÈÖç„ÄÅÁΩëÊÆµÔºâ');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_external_job`
--

DROP TABLE IF EXISTS `act_ru_external_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_external_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_EJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_external_job`
--

LOCK TABLES `act_ru_external_job` WRITE;
/*!40000 ALTER TABLE `act_ru_external_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_external_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Â≠óÂÖ∏‰∏ªÈîÆ',
  `dict_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Â≠óÂÖ∏ÂêçÁß∞',
  `dict_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Â≠óÂÖ∏Á±ªÂûã',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'Áä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÂÅúÁî®Ôºâ',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_sys_dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Â≠óÂÖ∏Á±ªÂûãË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
INSERT INTO `sys_dict_type` VALUES (1,'Áî®Êà∑ÊÄßÂà´','sys_user_sex','0','admin','2026-04-22 01:46:29','',NULL,'Áî®Êà∑ÊÄßÂà´ÂàóË°®'),(2,'ËèúÂçïÁä∂ÊÄÅ','sys_show_hide','0','admin','2026-04-22 01:46:29','',NULL,'ËèúÂçïÁä∂ÊÄÅÂàóË°®'),(3,'Á≥ªÁªüÂºÄÂÖ≥','sys_normal_disable','0','admin','2026-04-22 01:46:29','',NULL,'Á≥ªÁªüÂºÄÂÖ≥ÂàóË°®'),(4,'‰ªªÂä°Áä∂ÊÄÅ','sys_job_status','0','admin','2026-04-22 01:46:29','',NULL,'‰ªªÂä°Áä∂ÊÄÅÂàóË°®'),(5,'‰ªªÂä°ÂàÜÁªÑ','sys_job_group','0','admin','2026-04-22 01:46:29','',NULL,'‰ªªÂä°ÂàÜÁªÑÂàóË°®'),(6,'Á≥ªÁªüÊòØÂê¶','sys_yes_no','0','admin','2026-04-22 01:46:29','',NULL,'Á≥ªÁªüÊòØÂê¶ÂàóË°®'),(7,'ÈÄöÁü•Á±ªÂûã','sys_notice_type','0','admin','2026-04-22 01:46:29','',NULL,'ÈÄöÁü•Á±ªÂûãÂàóË°®'),(8,'ÈÄöÁü•Áä∂ÊÄÅ','sys_notice_status','0','admin','2026-04-22 01:46:29','',NULL,'ÈÄöÁü•Áä∂ÊÄÅÂàóË°®'),(9,'Êìç‰ΩúÁ±ªÂûã','sys_oper_type','0','admin','2026-04-22 01:46:29','',NULL,'Êìç‰ΩúÁ±ªÂûãÂàóË°®'),(10,'Á≥ªÁªüÁä∂ÊÄÅ','sys_common_status','0','admin','2026-04-22 01:46:29','',NULL,'ÁôªÂΩïÁä∂ÊÄÅÂàóË°®'),(100,'Ë°®ËææÂºèÁ±ªÂûã','exp_data_type','0','admin','2026-04-22 01:46:52','',NULL,'Ë°®ËææÂºèÁ±ªÂûã'),(101,'ÁõëÂê¨Á±ªÂûã','sys_listener_type','0','admin','2026-04-22 01:46:52','',NULL,'ÁõëÂê¨Á±ªÂûã'),(102,'ÁõëÂê¨ÂÄºÁ±ªÂûã','sys_listener_value_type','0','admin','2026-04-22 01:46:52','',NULL,'ÁõëÂê¨ÂÄºÁ±ªÂûã'),(103,'ÁõëÂê¨Â±ûÊÄß','sys_listener_event_type','0','admin','2026-04-22 01:46:52','',NULL,'ÁõëÂê¨Â±ûÊÄß'),(104,'ÊµÅÁ®ãÂàÜÁ±ª','sys_process_category','0','admin','2026-04-22 01:46:52','',NULL,'ÊµÅÁ®ãÂàÜÁ±ª');
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_ru_plan_item_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_plan_item_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_plan_item_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STAGE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_STAGE_` tinyint DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ITEM_DEFINITION_TYPE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IS_COMPLETEABLE_` tinyint DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `SENTRY_PART_INST_COUNT_` int DEFAULT NULL,
  `LAST_AVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_ENABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_DISABLED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_STARTED_TIME_` datetime(3) DEFAULT NULL,
  `LAST_SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_TIME_` datetime(3) DEFAULT NULL,
  `OCCURRED_TIME_` datetime(3) DEFAULT NULL,
  `TERMINATED_TIME_` datetime(3) DEFAULT NULL,
  `EXIT_TIME_` datetime(3) DEFAULT NULL,
  `ENDED_TIME_` datetime(3) DEFAULT NULL,
  `ENTRY_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_CRITERION_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXTRA_VALUE_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DERIVED_CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_UNAVAILABLE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_PLAN_ITEM_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_PLAN_ITEM_CASE_INST` (`CASE_INST_ID_`),
  KEY `ACT_IDX_PLAN_ITEM_STAGE_INST` (`STAGE_INST_ID_`),
  CONSTRAINT `ACT_FK_PLAN_ITEM_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`),
  CONSTRAINT `ACT_FK_PLAN_ITEM_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `act_cmmn_ru_case_inst` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_ru_plan_item_inst`
--

LOCK TABLES `act_cmmn_ru_plan_item_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_ru_plan_item_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_ru_plan_item_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_cron_triggers`
--

DROP TABLE IF EXISTS `qrtz_cron_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_cron_triggers` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `trigger_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_nameÁöÑÂ§ñÈîÆ',
  `trigger_group` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_groupÁöÑÂ§ñÈîÆ',
  `cron_expression` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'cronË°®ËææÂºè',
  `time_zone_id` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Êó∂Âå∫',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CronÁ±ªÂûãÁöÑËß¶ÂèëÂô®Ë°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_cron_triggers`
--

LOCK TABLES `qrtz_cron_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_cron_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_cron_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ge_bytearray`
--

DROP TABLE IF EXISTS `act_ge_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ge_bytearray` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  `GENERATED_` tinyint DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_BYTEARR_DEPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_BYTEARR_DEPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ge_bytearray`
--

LOCK TABLES `act_ge_bytearray` WRITE;
/*!40000 ALTER TABLE `act_ge_bytearray` DISABLE KEYS */;
INSERT INTO `act_ge_bytearray` VALUES ('891a4295-305c-11f1-82b6-8c1645e938b5',1,'flow_1hxp265d.bpmn','891a4294-305c-11f1-82b6-8c1645e938b5',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:bioc=\"http://bpmn.io/schema/bpmn/biocolor/1.0\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.flowable.org/processdef\">\n  <process id=\"flow_cl1udrgp\" name=\"flow_1hxp265d\" flowable:processCategory=\"leave\">\n    <startEvent id=\"start_event\" name=\"ÂºÄÂßã\">\n      <outgoing>Flow_1dd8wjs</outgoing>\n    </startEvent>\n    <userTask id=\"Activity_1kbxzqu\" name=\"ÂàùÂÆ°\">\n      <incoming>Flow_1dd8wjs</incoming>\n      <outgoing>Flow_1ytg1zm</outgoing>\n    </userTask>\n    <sequenceFlow id=\"Flow_1dd8wjs\" sourceRef=\"start_event\" targetRef=\"Activity_1kbxzqu\" />\n    <userTask id=\"Activity_0v17nhy\" name=\"Â§çÂÆ°\">\n      <incoming>Flow_1ytg1zm</incoming>\n      <outgoing>Flow_0l7uvbx</outgoing>\n    </userTask>\n    <sequenceFlow id=\"Flow_1ytg1zm\" sourceRef=\"Activity_1kbxzqu\" targetRef=\"Activity_0v17nhy\" />\n    <endEvent id=\"Event_0jjdo1e\" name=\"ÁªìÊùü\">\n      <incoming>Flow_0l7uvbx</incoming>\n    </endEvent>\n    <sequenceFlow id=\"Flow_0l7uvbx\" sourceRef=\"Activity_0v17nhy\" targetRef=\"Event_0jjdo1e\" />\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_flow\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_flow\" bpmnElement=\"flow_cl1udrgp\">\n      <bpmndi:BPMNShape id=\"BPMNShape_start_event\" bpmnElement=\"start_event\" bioc:stroke=\"\">\n        <omgdc:Bounds x=\"35\" y=\"215\" width=\"30\" height=\"30\" />\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds x=\"37\" y=\"252\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1kbxzqu_di\" bpmnElement=\"Activity_1kbxzqu\">\n        <omgdc:Bounds x=\"120\" y=\"190\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_0v17nhy_di\" bpmnElement=\"Activity_0v17nhy\">\n        <omgdc:Bounds x=\"290\" y=\"190\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Event_0jjdo1e_di\" bpmnElement=\"Event_0jjdo1e\">\n        <omgdc:Bounds x=\"452\" y=\"212\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds x=\"459\" y=\"255\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_1dd8wjs_di\" bpmnElement=\"Flow_1dd8wjs\">\n        <di:waypoint x=\"65\" y=\"230\" />\n        <di:waypoint x=\"120\" y=\"230\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1ytg1zm_di\" bpmnElement=\"Flow_1ytg1zm\">\n        <di:waypoint x=\"220\" y=\"230\" />\n        <di:waypoint x=\"290\" y=\"230\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0l7uvbx_di\" bpmnElement=\"Flow_0l7uvbx\">\n        <di:waypoint x=\"390\" y=\"230\" />\n        <di:waypoint x=\"452\" y=\"230\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>\n',0),('8926c5b6-305c-11f1-82b6-8c1645e938b5',1,'flow_1hxp265d.flow_cl1udrgp.png','891a4294-305c-11f1-82b6-8c1645e938b5',_binary 'âPNG\r\n\Z\n\0\0\0\rIHDR\0\0\Ú\0\0\0\0\0ßNt4\0\0hIDATx^\Ì\›}å\\eΩp\ıèõ™\Ò\ﬂ#íhîD˝Cç1\Z\‚K∏Ç1ëDº◊ñ›∂A∑\Ì\"≠X4ë´	°π\ÙB45¸#^ºÅ?0TäíB\€P\ \À\÷\⁄• ØW*/Ö∂R⁄•∂µ\Â≠[,\Ìπ\Á7\›Yßœúvá\Óv˚\Ã\Ã\Áì|≥ª3g\Œ93\Û\€ﬂ≥œô3≥ox\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0*ä\‚_6n\‹x\„Ω\˜\ﬁ˚èÅÅÅ\‚\Œ;\Ôî	N˘∏\\≥fÕ∂U´Vùù>?¢NO|\‘)d™lé7ïøú\≈\ˆ\Ì€ã°°°\‚\’W_ï	N<\Ó\Ò¯Ø^Ω˙Ö≤aûï>G®\”¢N!S1√â_\Œ\ÙóV&>ÉÉÉ;\Ày˙°Nsä:Ö\Ã\ƒaJ3ú<\œC\Ÿ \˜•\œ\Í4ß®S\»Lº\ˆï˛¢ âK<\ÈsÑ:\Õ-\Í2\“jÉ|y\œ`±\ÒOø)\÷\ﬂ\ı_µ\ƒ\˜qY∫úå-\Zd5uöW\‘)d§ï˘\“\Ó≠\≈c∑\œ+Ω\ı?K\\◊•\ÀÀ±GÉ¨¶N\Ûä:Öå¥\“ ∑¨_\÷\‘\Îyn˝\Ú¶\Â\Âÿ£AVSßyEùBFZiêèˇaASc¨\'ÆKóócèYMù\Êu\ni•A>v\Á•Mç±û∏.]^é=\Zd5uöW\‘)dDÉ\Ã+\Zd5uöW\‘)d§ïgˇ¶ç±û∏.]^é=\Zd5uöW\‘)d§ïπ\·ûˇnjå\ı\ƒu\È\Úr\Ï\— ´©”º¢N!#≠4\»][ˇØx\ÏéˇljéqY\\ó./\«\r≤ö:\Õ+\Í2\“JÉålz`aSÉå\À\“\Âdl\— ´©”º¢N!#-5\»}˚ä\rkˇß©A\∆eq]\”\Úr\Ã\— ´©”º¢N!#£5\»¯D¨ß\Ó˘USs¨\'Æ\Û©Y\„\r≤ö:\Õ+\Í2r\ƒY\Œ`7¨*˛º\Ú‚¶¶ò&ñâe\Õz\∆\r≤ö:\Õ+\Í2R\’ Gõ\›)f=cèYMù\Êu\n©jê≠\Ãnéî∏m∫>i=\Zd5uöW\‘)d§™A¶M\Ô\ı&]ü¥\r≤ö:\Õ+\Í2R\’ \Â\ƒEÉ¨¶N\Ûä:mM__\ﬂ;{{{\'\˜\Ù\Ù\\Y~(≥©\Ã\ﬁ2\≈\\◊¯y`¯˙…±|∫ïôW4\»j\Í4Ø®”£õ:u\Í\◊ ÅyIôWá\ÌV\À/â€ß\ÎÑ#\“ \ÛäYMù\ÊuZ≠ÄO+\‚u\Ù±d]¨/\›4\— \ÛäYMù\Êuz∏æææI===WïÉ\Ô¡t@˛—è~T,Z¥®x\¡ãgûy¶ÿµkW\‚k¸ó\«\ı±\\z\€X_¨7÷ün≥£ïèœõ\ÓΩ\˜\ﬁ_∏p\·\Û\Á\œ\ﬂ˘”ü˛thŒú9µw\ˆ\Ï\Ÿ˚/∫\Ë¢\ÀÀûæ\Ï≤ÀÆò1c\∆)\È\ÌªçôW4\»j\Í4Ø®\”*«ñS ô\Û£ç\¥i”ä´Øæ∫x\Óπ\ÁjÉv´b˘∏]\‹>\–â\Ì§\€\Ó8\Âc0i\Â ï\◊Ãõ7\Ô˝˝˝≈Ø˝\Îb›∫uï\≈\Âq˝¨Y≥Ãù;w\„y\Áù\˜\ıt}\›BÉ\Ã+\Zd5uöW\‘\È!\Â\0˛\Ò2[\›[∂l9|Ñ~ù\‚\ˆ±ûd0\ﬂ\€K\˜°cî\Û\Â{(Mî≥\Ò\‚¿Å\È\„R)ñã\Â/º\\¬˝\ﬂˇ˛\˜\Í\È\È˘h∫\ÓNßA\Ê\r≤ö:\Õ+\Ítd&>2à\«,zŸ≤e\È03&±ædvæ•\„f\Ê\Â˝|\„\Ì∑ﬂæ\Ë¸\Û\œ/V¨XQº\ˆ\⁄k\È\„–í∏]‹æØØ\Ô\Ân;[PÉ\Ã+\Zd5uöW∫ΩN\„5\Î\∆\√\È3f\Ã(~¯\·th±\ﬁX\√`˛H«ºf^ﬁø7.Y≤\‰æ9s\ÊO<\ÒDzﬂèI¨ßøøo˘–¨t{ùJÉ\Ã+\›\ﬁ èDù\ÊïnØ\”\·\€Ff\‚\«kØã\ı7\Œ\Ãc˚\È>µ•òâœû=ªÿæ}{zü\«$\÷W˛\ı\Û\“9\Áúsf∫\ÕN§A\ÊïnoêG¢N\ÛJ7\◊\È\[\Ã\‘\’\ÂÀóß\√\»q\€iòïh˚∑¶≠]ª\ˆÇ\ƒ\«k&û*\◊{\;\ﬂ˘\Œ\Ó)S¶|,\›vß\— \ÛJ77»£Qßy•õÎ¥∑\·}\‚qB\⁄DJNÄ[ó\Ó[\€(\ÔÀ§\À/ø|(^\”>ûñ.]∫{⁄¥i+\”\Ìw\Z\r2ØtsÉ<\ZuöW∫µN\„Hm\„!\ı±ûù˛z\≈\ˆ\Z±∑\Ìë\„\€nª\Ì\Í8;˝XOlkU¨øøøG˘@˝k∫ùDÉ\Ã+\›\⁄ G£N\ÛJ∑\÷iÔ°è]≠\r¢\Ò~\Ô!∂\€0+_í\Óc\ˆ\ ˚\¶K.π\‰Òñ±âp\˜\›w\Ô)®ˇM\˜£ìhêy•[\‰h\‘i^\È\∆:-g\¬\'\ı6|v˙D\œ\∆\Îbª\r˘´±_\Èæfm\Ì⁄µˇv\ﬁy\Áµ¸>\Ò±ä\ÌLü>}œî)SNN\˜%w\Â¸á2ßßóß4»º\“m\rRù∂g:≠N[©\√\Ú˙\…\ı4é\nüH\…«πNN\˜5k\◊]w›ù\Òâli˛¸˘è\ı\Ù\Ù\ÃI\˜%w\rO\ÚQTÉ\Ã+ù\÷ G£N\€3ùVß≠\‘ay˘Ø\Í\À]˝\ı\ÈP1°b˚\r˚¸´t_≥v\ŸeóÌò®\√\Íuw\›u\◊˙\ﬁ6|¢\·I>jÅjêy•\”\Z\‰h\‘i{¶\”Í¥ï:ú:u\Í™˙\ı<\@:TL®\ÿ~}_bø\Z\˜3{?˘\…OÜ6n‹òﬁß\„\Í\…\'ü\‹\\>XJ\˜%wÖYY†\Zd^\È¥9öä˙TßmêN´”ä˙k™\√\Ú\Î≥\ı\À\'zJ\≈\ˆ\ˆ\Ò\Ÿ\‰\Ó\‰m\ˆ\Ï\Ÿˇ˛\˜øß\˜\È∏⁄µk\◊K\Âµ5›ó\‹UdöZÅ∂CÉº\Ôæ˚ö.k%O=\ıT\Ìƒê\Ù\Úú\”i\rr4uô¶m\Í4\Ú\»#è\‘>T*Ω|pp∞\È≤z\‘\ÈâWQwi¢_©ˇ<\—\„P*∂_ﬂórF˛bz≤v\Óπ\Á˚\˜\ÔO\Ô\”qUn\Ô≈ä\'µc\“\r\Ú¢ã.*æ\˜Ω\Ô˚\ˆ\Ìk∫nhhË∞ü£¿_y\Âï\⁄\˜6l(>\Ù°{\ˆ\Ï9\‚\Úπ%}~\‰P⁄°N\„_Pæ\Ô}\Ô+\ﬁ\Ûû\˜\ƒ[Wkô5kV\Ò\Ìoªx\Ô{\ﬂ[;Z_Vù∂w&zJ\≈\ˆ\ˆ\Áµt¨\Ã\⁄˘Áüø¢ˇ⁄±c\«”Ωù5#ˇCoõ≤\\Ωzu\Ìüú}\ˆ\Ÿ\Ò\◊\Ò\Â/π∏\‡Çäx\ÁB=ßü~zq\Ò\≈è\‹f˛¸˘\≈\‰…ìGöh\‹\Óª\ﬂ˝n1s\Ê\Ã⁄∫>\ÒâO\‘fM\È∂rIß\ÕtFSQümWßë›ªwgûyf±r\Â ¶\Î™euöóä˙k™√ò˘\÷/ü\Ëq(\’\÷3\Úˇ¯\«/N\Ùk˘\À_\Ë\Ìå\◊\»kåuπ7»ò\Â\ƒL¸_¯B±p\·\¬Z√å\Ÿ˘o˚\€\⁄\ıe\◊\Œ\‡¨/ˇëè|$û≥ëü\ﬂ\ıÆwç4\ƒ+Øº≤ò7o^±u\Î÷¶\Ì\‰íNkê£\ÈÑ:çz:\Áúsä5k\÷ü˝\Ïgkq=q1\Âm€∂vuöóV\Í∞\◊k\‰„£úy=5\—g≠\ﬂ|\Û\Õ\Àz\€˚¨\ı¶ÇlîsÉ¨Áóø¸em\ˆüØ\…%ó\ﬂ˙÷∑ä;Ó∏£v\›7æ\Òç\‚\÷[oY\ˆ˘\Áü/\Œ8„åë˚[\ﬁ\Úñë\Ô?¸\·O?˝t\”˙sJß5\»\—tBù\∆!\Ò;v‘æèzãº^sQ∑ü˙‘ßön£N\Û\“J:k}úÃü?ˇäâ~˘~\Ém˙>\Ú#d£úd\‰û{\Ó)\ﬁ˝\Ów_˙“óä≥\Œ:´ÿπsg\Ì˚˙\Ï\Âã_¸bmô\∆\€\ƒ\Îë\ı\◊?¯¡é\\Ø_¶\Î\œ-ù\÷ G\”)uZœõ\ﬂ¸Ê¶Å¸\‘SOmZ.¢N\Û\—J\ˆz˘¯(AN\È\Ô\Ô?0Åü\Ï\ˆ|˘ \Ìh\«OvkU\Œ\r\Ú°á*æ˘\ÕoW]uU\Òπ\œ}Æxˇ˚\ﬂ_<¯\‡É\≈\…\'ü\\;\ƒ\À|\Úìü,¸\Ò\√n\˜Å|†\Ë\Î\Î´5\‘˙©˛}∫ç\‹\“i\rrº\‰\\ßç9È§ìjØwGΩÕô3ßVøüˇ¸\ÁG\Ï∆®\”\ˆ“õ\—\'ª˝\á?<\ÿ0ê∑\◊\'ªÖπs\Ánö®\√\Îã/ææ\◊g≠ü∞\ƒ\Û¸\Ú\À/ø˚\›\Ô\‚\»H\Ò˚\ﬂˇæ∏\·ÜäØ~\ı´#\À\ƒ\‡^?¨YO\Ã\‡\„$¢òΩ\ı≠oôMö4©ÿªwo\”vrJ76\»V\‰\\ß\ıD≠~\Â+_©˝°πt\È\“xYÆVã7\ﬁxc”≤u\⁄^|\÷˙8*˝\Î^x\·˛	¯\Ôg[\ iÉˇ~v\‚S\»\„˚8©®˛öxú\˜éwº£i˘_¸\‚#oUãYO˝\Úx≠Ω\Í\‚ú“ç\r≤π\◊\È/ºP˚\˜í7\›tS\Ì•û∑ø˝\Ì\≈\€\ﬁ\ˆ∂ës9™¢N\€OØˇ~6~ ¶˛–ä+¶wp,\…~S>H+\“mwö\‹d$\ŒPè∑ù]s\Õ5qH©\ˆÅ?ˇ˘\œk˘\Ùß?›¥|c\‚u\»\Ù≤ú”≠\rr4π\◊i\Ï_ú†\ˆË£è\÷j4Nd;\Ì¥\”j˘\Ÿ\œ~V\‹r\À-≈∫u\ÎönWè:m9¸?\Ú©SßéVo\€ˇG¶Lô\Ú±3fº\Ù\ƒO§\˜s\\¸\ÒèºÆ|êc;\È∂;M\Ó\r2r\Ìµ\◊\÷f\‚W\\q\≈\»e\Ò~\›x}<\Zd∫|c>\Ûô\œ4]ñs∫µAé&\Á:›¥iSm\û>}z\Ì√ã÷Æ];r]=ä∑K∆ô\Î1\”No[è:m\Âÿ∞Æ>ê.X∞ >é´\ÿ^\√l|]∫omß¸´\‰k3g\Œ\‹≥≥\Ò¥y\Û\Ê\Â\Â¥≠≠ˇ\“yrnê\ıå\Â0c\ÓØ5¶\È\Êy4\ÌPßcâ:m\Â\ÿsZ9F®®Àó/Oáë\„\"∂\”0àà˝H\˜≠-Mõ6mV93qºf\Ê\√3\Ò≠ef¶\€\ÍTù\ﬁ \€-\›\‹ èFù\ÊïnØ”ûûû´\ÍÉjb¯\·á\”\·d\\\≈˙©\«\ˆ\”}jk1s>\˜\‹sw/]∫t\˜±û\0\'∂\rø&˛|∑\Ã\ƒ\Î4»º\“\Ì\r\ÚH\‘i^\È\ˆ:\Ì\Î\ÎõTéè\‘\÷x\€\‡\Ò\Z\ÃcΩ\Â\ˆFé\0\ƒvc˚\È>µΩÚØìèñ\›\ﬁ\ﬂﬂø\„\Óª\Ô\ﬁ\”\Í˚\Ã\À\Â∂/^ºxQ˘¿l(s[¨\']wß\” \ÛJ∑7\»#QßyEù\÷^+?•LºªidfælŸ≤tòìX_\„L|x{ß§˚\“Q\‚≠b\Âù\\3}˙\Ù.Ω\Ù\“?<\ˆ\‰ìO>ªs\ÁŒó\À\«‰•ø˝\Ìo◊Ø_ˇ¿-∑‹≤t\Ó‹π∑ï\À\Óå\Â;˝-fG£A\Ê\r≤ö:\Õ+\Í\Ùêrê˝x\„`â\“\∆z6{\‹>9±≠6à\«\ˆ\“}\ËX\ÒIlÂùû]Œ∞\„s\“\Ôox†\„\Î˝√ó\œ\Ó\‰OlkïôW4\»j\Í4Ø®\”\Í=439\Ãâ\Ÿyº\ﬂ;˛\È\”\Î\À\«\ÌíYx\Ìpzl\'\›6\‘hêyEÉ¨¶N\Ûä:=\\ºf=|\\:\0\◊>\Œu—¢Eµèó~\Êôgä]ªv\’\Ì¯\Z?\«\Âq}\Ú±´\ıå\ıv\‰k\‚å\r2Øhê\’\‘i^Qß’Üﬂö6\Ú>\Û1f]«º≈å\„KÉ\Ã+\Zd5uöW\‘\È\—\≈\Áú\Ù˙8◊ë\œfo1±¸í∏}∫N8\"\r2Øhê\’\‘i^Qß≠\È\Î\Î{g90O\Ó\ÈÈπ≤¸:PfSôΩ√Év|çüÜØüÀß\ÎÄQiêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:ÖåhêyEÉ¨¶N\Ûä:Öå\Z\Zj˙EïâO˘<l+\‰æ\Ù9Bù\Êu\nôY≥fÕ∂\Ì€∑7˝≤\ \ƒ\ÁØ˝\Î\‚≤Aﬁü>G®”ú¢N!3´V≠:{\ı\Í\’/\Ó4\„91)\˜¡Õõ7\ﬂP6\«gÀúï>G®\”¢N!c\ÒKaóy5^˚í	O<\Ó\Ò¯kéGè\œ\\„§NOL\‘)\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0å\≈ˇª∏[Vgv\Ÿ?\0\0\0\0IENDÆB`Ç',1),('8c970949-3063-11f1-8e5a-8c1645e938b5',1,'var-formJson',NULL,_binary '¨\Ì\0sr\0java.util.LinkedHashMap4¿N\\l¿˚\0Z\0accessOrderxr\0java.util.HashMap\⁄¡\√`\—\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\nwidgetListsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0t\0typet\0inputt\0icont\0\ntext-fieldt\0formItemFlagsr\0java.lang.Boolean\Õ rÄ’ú˙\Ó\0Z\0valuexpt\0optionssq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)t\0namet\0\ninput20238t\0labelt\0inputt\0\nlabelAlignt\0\0q\0~\0t\0textt\0defaultValueq\0~\0t\0placeholderq\0~\0t\0columnWidtht\0200pxt\0sizeq\0~\0t\0\nlabelWidthpt\0labelHiddensq\0~\0\0t\0readonlyq\0~\0t\0disabledq\0~\0t\0hiddenq\0~\0t\0	clearableq\0~\0\rt\0showPasswordq\0~\0t\0requiredq\0~\0t\0requiredHintq\0~\0t\0\nvalidationq\0~\0t\0validationHintq\0~\0t\0customClasssq\0~\0\0\0\0\0w\0\0\0\0xt\0labelIconClasspt\0labelIconPositiont\0reart\0labelTooltippt\0	minLengthpt\0	maxLengthpt\0\rshowWordLimitq\0~\0t\0\nprefixIconq\0~\0t\0\nsuffixIconq\0~\0t\0appendButtonq\0~\0t\0appendButtonDisabledq\0~\0t\0\nbuttonIcont\0el-icon-searcht\0	onCreatedq\0~\0t\0	onMountedq\0~\0t\0onInputq\0~\0t\0onChangeq\0~\0t\0onFocusq\0~\0t\0onBlurq\0~\0t\0\nonValidateq\0~\0t\0onAppendButtonClickq\0~\0t\0prependTextq\0~\0t\0\nappendTextq\0~\0x\0t\0idt\0\ninput20238x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0textareaq\0~\0	t\0textarea-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0q\0~\0t\0\rtextarea77059q\0~\0t\0textareaq\0~\0q\0~\0t\0rowssr\0java.lang.Integer‚†§\˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0q\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0x\0q\0~\0At\0\rtextarea77059x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput18200q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput18200x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput86112q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput86112x\0xt\0\nformConfigsq\0~\0\0?@\0\0\0\0\0w\0\0\0 \0\0\0t\0	modelNamet\0formDatat\0refNamet\0vFormt\0	rulesNamet\0rulesq\0~\0sq\0~\0J\0\0\0Pt\0\rlabelPositiont\0leftq\0~\0q\0~\0q\0~\0t\0label-left-alignt\0cssCodeq\0~\0q\0~\0(q\0~\0t\0	functionsq\0~\0t\0\nlayoutTypet\0PCt\0\ronFormCreatedq\0~\0t\0\ronFormMountedq\0~\0t\0onFormDataChangeq\0~\0t\0onFormValidateq\0~\0x\0x\0',NULL),('8c97576b-3063-11f1-8e5a-8c1645e938b5',1,'hist.var-formJson',NULL,_binary '¨\Ì\0sr\0java.util.LinkedHashMap4¿N\\l¿˚\0Z\0accessOrderxr\0java.util.HashMap\⁄¡\√`\—\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\nwidgetListsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0t\0typet\0inputt\0icont\0\ntext-fieldt\0formItemFlagsr\0java.lang.Boolean\Õ rÄ’ú˙\Ó\0Z\0valuexpt\0optionssq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)t\0namet\0\ninput20238t\0labelt\0inputt\0\nlabelAlignt\0\0q\0~\0t\0textt\0defaultValueq\0~\0t\0placeholderq\0~\0t\0columnWidtht\0200pxt\0sizeq\0~\0t\0\nlabelWidthpt\0labelHiddensq\0~\0\0t\0readonlyq\0~\0t\0disabledq\0~\0t\0hiddenq\0~\0t\0	clearableq\0~\0\rt\0showPasswordq\0~\0t\0requiredq\0~\0t\0requiredHintq\0~\0t\0\nvalidationq\0~\0t\0validationHintq\0~\0t\0customClasssq\0~\0\0\0\0\0w\0\0\0\0xt\0labelIconClasspt\0labelIconPositiont\0reart\0labelTooltippt\0	minLengthpt\0	maxLengthpt\0\rshowWordLimitq\0~\0t\0\nprefixIconq\0~\0t\0\nsuffixIconq\0~\0t\0appendButtonq\0~\0t\0appendButtonDisabledq\0~\0t\0\nbuttonIcont\0el-icon-searcht\0	onCreatedq\0~\0t\0	onMountedq\0~\0t\0onInputq\0~\0t\0onChangeq\0~\0t\0onFocusq\0~\0t\0onBlurq\0~\0t\0\nonValidateq\0~\0t\0onAppendButtonClickq\0~\0t\0prependTextq\0~\0t\0\nappendTextq\0~\0x\0t\0idt\0\ninput20238x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0textareaq\0~\0	t\0textarea-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0q\0~\0t\0\rtextarea77059q\0~\0t\0textareaq\0~\0q\0~\0t\0rowssr\0java.lang.Integer‚†§\˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0q\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0x\0q\0~\0At\0\rtextarea77059x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput18200q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput18200x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput86112q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput86112x\0xt\0\nformConfigsq\0~\0\0?@\0\0\0\0\0w\0\0\0 \0\0\0t\0	modelNamet\0formDatat\0refNamet\0vFormt\0	rulesNamet\0rulesq\0~\0sq\0~\0J\0\0\0Pt\0\rlabelPositiont\0leftq\0~\0q\0~\0q\0~\0t\0label-left-alignt\0cssCodeq\0~\0q\0~\0(q\0~\0t\0	functionsq\0~\0t\0\nlayoutTypet\0PCt\0\ronFormCreatedq\0~\0t\0\ronFormMountedq\0~\0t\0onFormDataChangeq\0~\0t\0onFormValidateq\0~\0x\0x\0',NULL),('8c97576c-3063-11f1-8e5a-8c1645e938b5',1,'hist.detail.var-formJson',NULL,_binary '¨\Ì\0sr\0java.util.LinkedHashMap4¿N\\l¿˚\0Z\0accessOrderxr\0java.util.HashMap\⁄¡\√`\—\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\nwidgetListsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0t\0typet\0inputt\0icont\0\ntext-fieldt\0formItemFlagsr\0java.lang.Boolean\Õ rÄ’ú˙\Ó\0Z\0valuexpt\0optionssq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)t\0namet\0\ninput20238t\0labelt\0inputt\0\nlabelAlignt\0\0q\0~\0t\0textt\0defaultValueq\0~\0t\0placeholderq\0~\0t\0columnWidtht\0200pxt\0sizeq\0~\0t\0\nlabelWidthpt\0labelHiddensq\0~\0\0t\0readonlyq\0~\0t\0disabledq\0~\0t\0hiddenq\0~\0t\0	clearableq\0~\0\rt\0showPasswordq\0~\0t\0requiredq\0~\0t\0requiredHintq\0~\0t\0\nvalidationq\0~\0t\0validationHintq\0~\0t\0customClasssq\0~\0\0\0\0\0w\0\0\0\0xt\0labelIconClasspt\0labelIconPositiont\0reart\0labelTooltippt\0	minLengthpt\0	maxLengthpt\0\rshowWordLimitq\0~\0t\0\nprefixIconq\0~\0t\0\nsuffixIconq\0~\0t\0appendButtonq\0~\0t\0appendButtonDisabledq\0~\0t\0\nbuttonIcont\0el-icon-searcht\0	onCreatedq\0~\0t\0	onMountedq\0~\0t\0onInputq\0~\0t\0onChangeq\0~\0t\0onFocusq\0~\0t\0onBlurq\0~\0t\0\nonValidateq\0~\0t\0onAppendButtonClickq\0~\0t\0prependTextq\0~\0t\0\nappendTextq\0~\0x\0t\0idt\0\ninput20238x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0textareaq\0~\0	t\0textarea-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0q\0~\0t\0\rtextarea77059q\0~\0t\0textareaq\0~\0q\0~\0t\0rowssr\0java.lang.Integer‚†§\˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0q\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0x\0q\0~\0At\0\rtextarea77059x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput18200q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput18200x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput86112q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput86112x\0xt\0\nformConfigsq\0~\0\0?@\0\0\0\0\0w\0\0\0 \0\0\0t\0	modelNamet\0formDatat\0refNamet\0vFormt\0	rulesNamet\0rulesq\0~\0sq\0~\0J\0\0\0Pt\0\rlabelPositiont\0leftq\0~\0q\0~\0q\0~\0t\0label-left-alignt\0cssCodeq\0~\0q\0~\0(q\0~\0t\0	functionsq\0~\0t\0\nlayoutTypet\0PCt\0\ronFormCreatedq\0~\0t\0\ronFormMountedq\0~\0t\0onFormDataChangeq\0~\0t\0onFormValidateq\0~\0x\0x\0',NULL),('8cb4555e-3063-11f1-8e5a-8c1645e938b5',1,'hist.detail.var-formJson',NULL,_binary '¨\Ì\0sr\0java.util.LinkedHashMap4¿N\\l¿˚\0Z\0accessOrderxr\0java.util.HashMap\⁄¡\√`\—\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\nwidgetListsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0t\0typet\0inputt\0icont\0\ntext-fieldt\0formItemFlagsr\0java.lang.Boolean\Õ rÄ’ú˙\Ó\0Z\0valuexpt\0optionssq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)t\0namet\0\ninput20238t\0labelt\0inputt\0\nlabelAlignt\0\0q\0~\0t\0textt\0defaultValueq\0~\0t\0placeholderq\0~\0t\0columnWidtht\0200pxt\0sizeq\0~\0t\0\nlabelWidthpt\0labelHiddensq\0~\0\0t\0readonlyq\0~\0t\0disabledq\0~\0t\0hiddenq\0~\0t\0	clearableq\0~\0\rt\0showPasswordq\0~\0t\0requiredq\0~\0t\0requiredHintq\0~\0t\0\nvalidationq\0~\0t\0validationHintq\0~\0t\0customClasssq\0~\0\0\0\0\0w\0\0\0\0xt\0labelIconClasspt\0labelIconPositiont\0reart\0labelTooltippt\0	minLengthpt\0	maxLengthpt\0\rshowWordLimitq\0~\0t\0\nprefixIconq\0~\0t\0\nsuffixIconq\0~\0t\0appendButtonq\0~\0t\0appendButtonDisabledq\0~\0t\0\nbuttonIcont\0el-icon-searcht\0	onCreatedq\0~\0t\0	onMountedq\0~\0t\0onInputq\0~\0t\0onChangeq\0~\0t\0onFocusq\0~\0t\0onBlurq\0~\0t\0\nonValidateq\0~\0t\0onAppendButtonClickq\0~\0t\0prependTextq\0~\0t\0\nappendTextq\0~\0x\0t\0idt\0\ninput20238x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0textareaq\0~\0	t\0textarea-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0q\0~\0t\0\rtextarea77059q\0~\0t\0textareaq\0~\0q\0~\0t\0rowssr\0java.lang.Integer‚†§\˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0q\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0x\0q\0~\0At\0\rtextarea77059x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput18200q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput18200x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput86112q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput86112x\0xt\0\nformConfigsq\0~\0\0?@\0\0\0\0\0w\0\0\0 \0\0\0t\0	modelNamet\0formDatat\0refNamet\0vFormt\0	rulesNamet\0rulesq\0~\0sq\0~\0J\0\0\0Pt\0\rlabelPositiont\0leftq\0~\0q\0~\0q\0~\0t\0label-left-alignt\0cssCodeq\0~\0q\0~\0(q\0~\0t\0	functionsq\0~\0t\0\nlayoutTypet\0PCt\0\ronFormCreatedq\0~\0t\0\ronFormMountedq\0~\0t\0onFormDataChangeq\0~\0t\0onFormValidateq\0~\0x\0x\0',NULL),('8ce1f51b-3063-11f1-8e5a-8c1645e938b5',1,'var-formJson',NULL,_binary '¨\Ì\0sr\0java.util.LinkedHashMap4¿N\\l¿˚\0Z\0accessOrderxr\0java.util.HashMap\⁄¡\√`\—\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\nwidgetListsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0t\0typet\0inputt\0icont\0\ntext-fieldt\0formItemFlagsr\0java.lang.Boolean\Õ rÄ’ú˙\Ó\0Z\0valuexpt\0optionssq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)t\0namet\0\ninput20238t\0labelt\0inputt\0\nlabelAlignt\0\0q\0~\0t\0textt\0defaultValueq\0~\0t\0placeholderq\0~\0t\0columnWidtht\0200pxt\0sizeq\0~\0t\0\nlabelWidthpt\0labelHiddensq\0~\0\0t\0readonlyq\0~\0t\0disabledq\0~\0t\0hiddenq\0~\0t\0	clearableq\0~\0\rt\0showPasswordq\0~\0t\0requiredq\0~\0t\0requiredHintq\0~\0t\0\nvalidationq\0~\0t\0validationHintq\0~\0t\0customClasssq\0~\0\0\0\0\0w\0\0\0\0xt\0labelIconClasspt\0labelIconPositiont\0reart\0labelTooltippt\0	minLengthpt\0	maxLengthpt\0\rshowWordLimitq\0~\0t\0\nprefixIconq\0~\0t\0\nsuffixIconq\0~\0t\0appendButtonq\0~\0t\0appendButtonDisabledq\0~\0t\0\nbuttonIcont\0el-icon-searcht\0	onCreatedq\0~\0t\0	onMountedq\0~\0t\0onInputq\0~\0t\0onChangeq\0~\0t\0onFocusq\0~\0t\0onBlurq\0~\0t\0\nonValidateq\0~\0t\0onAppendButtonClickq\0~\0t\0prependTextq\0~\0t\0\nappendTextq\0~\0x\0t\0idt\0\ninput20238x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0textareaq\0~\0	t\0textarea-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0q\0~\0t\0\rtextarea77059q\0~\0t\0textareaq\0~\0q\0~\0t\0rowssr\0java.lang.Integer‚†§\˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0q\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0x\0q\0~\0At\0\rtextarea77059x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput18200q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput18200x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput86112q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput86112x\0xt\0\nformConfigsq\0~\0\0?@\0\0\0\0\0w\0\0\0 \0\0\0t\0	modelNamet\0formDatat\0refNamet\0vFormt\0	rulesNamet\0rulesq\0~\0sq\0~\0J\0\0\0Pt\0\rlabelPositiont\0leftq\0~\0q\0~\0q\0~\0t\0label-left-alignt\0cssCodeq\0~\0q\0~\0(q\0~\0t\0	functionsq\0~\0t\0\nlayoutTypet\0PCt\0\ronFormCreatedq\0~\0t\0\ronFormMountedq\0~\0t\0onFormDataChangeq\0~\0t\0onFormValidateq\0~\0x\0x\0',NULL),('8ce1f51d-3063-11f1-8e5a-8c1645e938b5',1,'hist.var-formJson',NULL,_binary '¨\Ì\0sr\0java.util.LinkedHashMap4¿N\\l¿˚\0Z\0accessOrderxr\0java.util.HashMap\⁄¡\√`\—\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\nwidgetListsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0t\0typet\0inputt\0icont\0\ntext-fieldt\0formItemFlagsr\0java.lang.Boolean\Õ rÄ’ú˙\Ó\0Z\0valuexpt\0optionssq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)t\0namet\0\ninput20238t\0labelt\0inputt\0\nlabelAlignt\0\0q\0~\0t\0textt\0defaultValueq\0~\0t\0placeholderq\0~\0t\0columnWidtht\0200pxt\0sizeq\0~\0t\0\nlabelWidthpt\0labelHiddensq\0~\0\0t\0readonlyq\0~\0t\0disabledq\0~\0t\0hiddenq\0~\0t\0	clearableq\0~\0\rt\0showPasswordq\0~\0t\0requiredq\0~\0t\0requiredHintq\0~\0t\0\nvalidationq\0~\0t\0validationHintq\0~\0t\0customClasssq\0~\0\0\0\0\0w\0\0\0\0xt\0labelIconClasspt\0labelIconPositiont\0reart\0labelTooltippt\0	minLengthpt\0	maxLengthpt\0\rshowWordLimitq\0~\0t\0\nprefixIconq\0~\0t\0\nsuffixIconq\0~\0t\0appendButtonq\0~\0t\0appendButtonDisabledq\0~\0t\0\nbuttonIcont\0el-icon-searcht\0	onCreatedq\0~\0t\0	onMountedq\0~\0t\0onInputq\0~\0t\0onChangeq\0~\0t\0onFocusq\0~\0t\0onBlurq\0~\0t\0\nonValidateq\0~\0t\0onAppendButtonClickq\0~\0t\0prependTextq\0~\0t\0\nappendTextq\0~\0x\0t\0idt\0\ninput20238x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0textareaq\0~\0	t\0textarea-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0q\0~\0t\0\rtextarea77059q\0~\0t\0textareaq\0~\0q\0~\0t\0rowssr\0java.lang.Integer‚†§\˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0q\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0x\0q\0~\0At\0\rtextarea77059x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput18200q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput18200x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput86112q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput86112x\0xt\0\nformConfigsq\0~\0\0?@\0\0\0\0\0w\0\0\0 \0\0\0t\0	modelNamet\0formDatat\0refNamet\0vFormt\0	rulesNamet\0rulesq\0~\0sq\0~\0J\0\0\0Pt\0\rlabelPositiont\0leftq\0~\0q\0~\0q\0~\0t\0label-left-alignt\0cssCodeq\0~\0q\0~\0(q\0~\0t\0	functionsq\0~\0t\0\nlayoutTypet\0PCt\0\ronFormCreatedq\0~\0t\0\ronFormMountedq\0~\0t\0onFormDataChangeq\0~\0t\0onFormValidateq\0~\0x\0x\0',NULL),('8ce1f51e-3063-11f1-8e5a-8c1645e938b5',1,'hist.detail.var-formJson',NULL,_binary '¨\Ì\0sr\0java.util.LinkedHashMap4¿N\\l¿˚\0Z\0accessOrderxr\0java.util.HashMap\⁄¡\√`\—\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\nwidgetListsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0t\0typet\0inputt\0icont\0\ntext-fieldt\0formItemFlagsr\0java.lang.Boolean\Õ rÄ’ú˙\Ó\0Z\0valuexpt\0optionssq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)t\0namet\0\ninput20238t\0labelt\0inputt\0\nlabelAlignt\0\0q\0~\0t\0textt\0defaultValueq\0~\0t\0placeholderq\0~\0t\0columnWidtht\0200pxt\0sizeq\0~\0t\0\nlabelWidthpt\0labelHiddensq\0~\0\0t\0readonlyq\0~\0t\0disabledq\0~\0t\0hiddenq\0~\0t\0	clearableq\0~\0\rt\0showPasswordq\0~\0t\0requiredq\0~\0t\0requiredHintq\0~\0t\0\nvalidationq\0~\0t\0validationHintq\0~\0t\0customClasssq\0~\0\0\0\0\0w\0\0\0\0xt\0labelIconClasspt\0labelIconPositiont\0reart\0labelTooltippt\0	minLengthpt\0	maxLengthpt\0\rshowWordLimitq\0~\0t\0\nprefixIconq\0~\0t\0\nsuffixIconq\0~\0t\0appendButtonq\0~\0t\0appendButtonDisabledq\0~\0t\0\nbuttonIcont\0el-icon-searcht\0	onCreatedq\0~\0t\0	onMountedq\0~\0t\0onInputq\0~\0t\0onChangeq\0~\0t\0onFocusq\0~\0t\0onBlurq\0~\0t\0\nonValidateq\0~\0t\0onAppendButtonClickq\0~\0t\0prependTextq\0~\0t\0\nappendTextq\0~\0x\0t\0idt\0\ninput20238x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0textareaq\0~\0	t\0textarea-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0q\0~\0t\0\rtextarea77059q\0~\0t\0textareaq\0~\0q\0~\0t\0rowssr\0java.lang.Integer‚†§\˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0q\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0x\0q\0~\0At\0\rtextarea77059x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput18200q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput18200x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput86112q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput86112x\0xt\0\nformConfigsq\0~\0\0?@\0\0\0\0\0w\0\0\0 \0\0\0t\0	modelNamet\0formDatat\0refNamet\0vFormt\0	rulesNamet\0rulesq\0~\0sq\0~\0J\0\0\0Pt\0\rlabelPositiont\0leftq\0~\0q\0~\0q\0~\0t\0label-left-alignt\0cssCodeq\0~\0q\0~\0(q\0~\0t\0	functionsq\0~\0t\0\nlayoutTypet\0PCt\0\ronFormCreatedq\0~\0t\0\ronFormMountedq\0~\0t\0onFormDataChangeq\0~\0t\0onFormValidateq\0~\0x\0x\0',NULL),('8ce8d300-3063-11f1-8e5a-8c1645e938b5',1,'hist.detail.var-formJson',NULL,_binary '¨\Ì\0sr\0java.util.LinkedHashMap4¿N\\l¿˚\0Z\0accessOrderxr\0java.util.HashMap\⁄¡\√`\—\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\nwidgetListsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0t\0typet\0inputt\0icont\0\ntext-fieldt\0formItemFlagsr\0java.lang.Boolean\Õ rÄ’ú˙\Ó\0Z\0valuexpt\0optionssq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)t\0namet\0\ninput20238t\0labelt\0inputt\0\nlabelAlignt\0\0q\0~\0t\0textt\0defaultValueq\0~\0t\0placeholderq\0~\0t\0columnWidtht\0200pxt\0sizeq\0~\0t\0\nlabelWidthpt\0labelHiddensq\0~\0\0t\0readonlyq\0~\0t\0disabledq\0~\0t\0hiddenq\0~\0t\0	clearableq\0~\0\rt\0showPasswordq\0~\0t\0requiredq\0~\0t\0requiredHintq\0~\0t\0\nvalidationq\0~\0t\0validationHintq\0~\0t\0customClasssq\0~\0\0\0\0\0w\0\0\0\0xt\0labelIconClasspt\0labelIconPositiont\0reart\0labelTooltippt\0	minLengthpt\0	maxLengthpt\0\rshowWordLimitq\0~\0t\0\nprefixIconq\0~\0t\0\nsuffixIconq\0~\0t\0appendButtonq\0~\0t\0appendButtonDisabledq\0~\0t\0\nbuttonIcont\0el-icon-searcht\0	onCreatedq\0~\0t\0	onMountedq\0~\0t\0onInputq\0~\0t\0onChangeq\0~\0t\0onFocusq\0~\0t\0onBlurq\0~\0t\0\nonValidateq\0~\0t\0onAppendButtonClickq\0~\0t\0prependTextq\0~\0t\0\nappendTextq\0~\0x\0t\0idt\0\ninput20238x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0textareaq\0~\0	t\0textarea-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0q\0~\0t\0\rtextarea77059q\0~\0t\0textareaq\0~\0q\0~\0t\0rowssr\0java.lang.Integer‚†§\˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0q\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0x\0q\0~\0At\0\rtextarea77059x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput18200q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput18200x\0sq\0~\0\0?@\0\0\0\0\0w\0\0\0\0\0\0q\0~\0t\0inputq\0~\0	t\0\ntext-fieldq\0~\0q\0~\0\rq\0~\0sq\0~\0\0?@\0\0\0\0\00w\0\0\0@\0\0\0)q\0~\0t\0\ninput86112q\0~\0t\0inputq\0~\0q\0~\0q\0~\0t\0textq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0t\0200pxq\0~\0q\0~\0q\0~\0pq\0~\0q\0~\0q\0~\0q\0~\0q\0~\0 q\0~\0q\0~\0!q\0~\0q\0~\0\"q\0~\0\rq\0~\0#q\0~\0q\0~\0$q\0~\0q\0~\0%q\0~\0q\0~\0&q\0~\0q\0~\0\'q\0~\0q\0~\0(q\0~\0q\0~\0*pq\0~\0+t\0rearq\0~\0-pq\0~\0.pq\0~\0/pq\0~\00q\0~\0q\0~\01q\0~\0q\0~\02q\0~\0q\0~\03q\0~\0q\0~\04q\0~\0q\0~\05t\0el-icon-searchq\0~\07q\0~\0q\0~\08q\0~\0q\0~\09q\0~\0q\0~\0:q\0~\0q\0~\0;q\0~\0q\0~\0<q\0~\0q\0~\0=q\0~\0q\0~\0>q\0~\0q\0~\0?q\0~\0q\0~\0@q\0~\0x\0q\0~\0At\0\ninput86112x\0xt\0\nformConfigsq\0~\0\0?@\0\0\0\0\0w\0\0\0 \0\0\0t\0	modelNamet\0formDatat\0refNamet\0vFormt\0	rulesNamet\0rulesq\0~\0sq\0~\0J\0\0\0Pt\0\rlabelPositiont\0leftq\0~\0q\0~\0q\0~\0t\0label-left-alignt\0cssCodeq\0~\0q\0~\0(q\0~\0t\0	functionsq\0~\0t\0\nlayoutTypet\0PCt\0\ronFormCreatedq\0~\0t\0\ronFormMountedq\0~\0t\0onFormDataChangeq\0~\0t\0onFormValidateq\0~\0x\0x\0',NULL);
/*!40000 ALTER TABLE `act_ge_bytearray` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu_bk1`
--

DROP TABLE IF EXISTS `sys_menu_bk1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu_bk1` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ËèúÂçïID',
  `menu_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ËèúÂçïÂêçÁß∞',
  `parent_id` bigint DEFAULT '0' COMMENT 'Áà∂ËèúÂçïID',
  `order_num` int DEFAULT '0' COMMENT 'ÊòæÁ§∫È°∫Â∫è',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Ë∑ØÁî±Âú∞ÂùÄ',
  `component` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÁªÑ‰ª∂Ë∑ØÂæÑ',
  `query` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ë∑ØÁî±ÂèÇÊï∞',
  `route_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Ë∑ØÁî±ÂêçÁß∞',
  `is_frame` int DEFAULT '1' COMMENT 'ÊòØÂê¶‰∏∫Â§ñÈìæÔºà0ÊòØ 1Âê¶Ôºâ',
  `is_cache` int DEFAULT '0' COMMENT 'ÊòØÂê¶ÁºìÂ≠òÔºà0ÁºìÂ≠ò 1‰∏çÁºìÂ≠òÔºâ',
  `menu_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ËèúÂçïÁ±ªÂûãÔºàMÁõÆÂΩï CËèúÂçï FÊåâÈíÆÔºâ',
  `visible` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'ËèúÂçïÁä∂ÊÄÅÔºà0ÊòæÁ§∫ 1ÈöêËóèÔºâ',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'ËèúÂçïÁä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÂÅúÁî®Ôºâ',
  `perms` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊùÉÈôêÊ†áËØÜ',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '#' COMMENT 'ËèúÂçïÂõæÊ†á',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ËèúÂçïÊùÉÈôêË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu_bk1`
--

LOCK TABLES `sys_menu_bk1` WRITE;
/*!40000 ALTER TABLE `sys_menu_bk1` DISABLE KEYS */;
INSERT INTO `sys_menu_bk1` VALUES (1,'Á≥ªÁªüÁÆ°ÁêÜ',0,1,'system',NULL,'','',1,0,'M','0','0','','system','admin','2026-03-28 23:42:43','',NULL,'Á≥ªÁªüÁÆ°ÁêÜÁõÆÂΩï'),(2,'Á≥ªÁªüÁõëÊéß',0,2,'monitor',NULL,'','',1,0,'M','0','0','','monitor','admin','2026-03-28 23:42:43','',NULL,'Á≥ªÁªüÁõëÊéßÁõÆÂΩï'),(3,'Á≥ªÁªüÂ∑•ÂÖ∑',0,3,'tool',NULL,'','',1,0,'M','0','0','','tool','admin','2026-03-28 23:42:43','',NULL,'Á≥ªÁªüÂ∑•ÂÖ∑ÁõÆÂΩï'),(4,'Ëã•‰æùÂÆòÁΩë',0,4,'http://ruoyi.vip',NULL,'','',0,0,'M','0','0','','guide','admin','2026-03-28 23:42:43','',NULL,'Ëã•‰æùÂÆòÁΩëÂú∞ÂùÄ'),(100,'Áî®Êà∑ÁÆ°ÁêÜ',1,1,'user','system/user/index','','',1,0,'C','0','0','system:user:list','user','admin','2026-03-28 23:42:44','',NULL,'Áî®Êà∑ÁÆ°ÁêÜËèúÂçï'),(101,'ËßíËâ≤ÁÆ°ÁêÜ',1,2,'role','system/role/index','','',1,0,'C','0','0','system:role:list','peoples','admin','2026-03-28 23:42:44','',NULL,'ËßíËâ≤ÁÆ°ÁêÜËèúÂçï'),(102,'ËèúÂçïÁÆ°ÁêÜ',1,3,'menu','system/menu/index','','',1,0,'C','0','0','system:menu:list','tree-table','admin','2026-03-28 23:42:44','',NULL,'ËèúÂçïÁÆ°ÁêÜËèúÂçï'),(103,'ÈÉ®Èó®ÁÆ°ÁêÜ',1,4,'dept','system/dept/index','','',1,0,'C','0','0','system:dept:list','tree','admin','2026-03-28 23:42:44','',NULL,'ÈÉ®Èó®ÁÆ°ÁêÜËèúÂçï'),(104,'Â≤ó‰ΩçÁÆ°ÁêÜ',1,5,'post','system/post/index','','',1,0,'C','0','0','system:post:list','post','admin','2026-03-28 23:42:44','',NULL,'Â≤ó‰ΩçÁÆ°ÁêÜËèúÂçï'),(105,'Â≠óÂÖ∏ÁÆ°ÁêÜ',1,6,'dict','system/dict/index','','',1,0,'C','0','0','system:dict:list','dict','admin','2026-03-28 23:42:45','',NULL,'Â≠óÂÖ∏ÁÆ°ÁêÜËèúÂçï'),(106,'ÂèÇÊï∞ËÆæÁΩÆ',1,7,'config','system/config/index','','',1,0,'C','0','0','system:config:list','edit','admin','2026-03-28 23:42:45','',NULL,'ÂèÇÊï∞ËÆæÁΩÆËèúÂçï'),(107,'ÈÄöÁü•ÂÖ¨Âëä',1,8,'notice','system/notice/index','','',1,0,'C','0','0','system:notice:list','message','admin','2026-03-28 23:42:45','',NULL,'ÈÄöÁü•ÂÖ¨ÂëäËèúÂçï'),(108,'Êó•ÂøóÁÆ°ÁêÜ',1,9,'log','','','',1,0,'M','0','0','','log','admin','2026-03-28 23:42:45','',NULL,'Êó•ÂøóÁÆ°ÁêÜËèúÂçï'),(109,'Âú®Á∫øÁî®Êà∑',2,1,'online','monitor/online/index','','',1,0,'C','0','0','monitor:online:list','online','admin','2026-03-28 23:42:45','',NULL,'Âú®Á∫øÁî®Êà∑ËèúÂçï'),(110,'ÂÆöÊó∂‰ªªÂä°',2,2,'job','monitor/job/index','','',1,0,'C','0','0','monitor:job:list','job','admin','2026-03-28 23:42:45','',NULL,'ÂÆöÊó∂‰ªªÂä°ËèúÂçï'),(111,'Êï∞ÊçÆÁõëÊéß',2,3,'druid','monitor/druid/index','','',1,0,'C','0','0','monitor:druid:list','druid','admin','2026-03-28 23:42:45','',NULL,'Êï∞ÊçÆÁõëÊéßËèúÂçï'),(112,'ÊúçÂä°ÁõëÊéß',2,4,'server','monitor/server/index','','',1,0,'C','0','0','monitor:server:list','server','admin','2026-03-28 23:42:46','',NULL,'ÊúçÂä°ÁõëÊéßËèúÂçï'),(113,'ÁºìÂ≠òÁõëÊéß',2,5,'cache','monitor/cache/index','','',1,0,'C','0','0','monitor:cache:list','redis','admin','2026-03-28 23:42:46','',NULL,'ÁºìÂ≠òÁõëÊéßËèúÂçï'),(114,'ÁºìÂ≠òÂàóË°®',2,6,'cacheList','monitor/cache/list','','',1,0,'C','0','0','monitor:cache:list','redis-list','admin','2026-03-28 23:42:46','',NULL,'ÁºìÂ≠òÂàóË°®ËèúÂçï'),(115,'Ë°®ÂçïÊûÑÂª∫',3,1,'build','tool/build/index','','',1,0,'C','0','0','tool:build:list','build','admin','2026-03-28 23:42:46','',NULL,'Ë°®ÂçïÊûÑÂª∫ËèúÂçï'),(116,'‰ª£Á†ÅÁîüÊàê',3,2,'gen','tool/gen/index','','',1,0,'C','0','0','tool:gen:list','code','admin','2026-03-28 23:42:46','',NULL,'‰ª£Á†ÅÁîüÊàêËèúÂçï'),(117,'Á≥ªÁªüÊé•Âè£',3,3,'swagger','tool/swagger/index','','',1,0,'C','0','0','tool:swagger:list','swagger','admin','2026-03-28 23:42:46','',NULL,'Á≥ªÁªüÊé•Âè£ËèúÂçï'),(500,'Êìç‰ΩúÊó•Âøó',108,1,'operlog','monitor/operlog/index','','',1,0,'C','0','0','monitor:operlog:list','form','admin','2026-03-28 23:42:46','',NULL,'Êìç‰ΩúÊó•ÂøóËèúÂçï'),(501,'ÁôªÂΩïÊó•Âøó',108,2,'logininfor','monitor/logininfor/index','','',1,0,'C','0','0','monitor:logininfor:list','logininfor','admin','2026-03-28 23:42:46','',NULL,'ÁôªÂΩïÊó•ÂøóËèúÂçï'),(1000,'Áî®Êà∑Êü•ËØ¢',100,1,'','','','',1,0,'F','0','0','system:user:query','#','admin','2026-03-28 23:42:46','',NULL,''),(1001,'Áî®Êà∑Êñ∞Â¢û',100,2,'','','','',1,0,'F','0','0','system:user:add','#','admin','2026-03-28 23:42:46','',NULL,''),(1002,'Áî®Êà∑‰øÆÊîπ',100,3,'','','','',1,0,'F','0','0','system:user:edit','#','admin','2026-03-28 23:42:46','',NULL,''),(1003,'Áî®Êà∑Âà†Èô§',100,4,'','','','',1,0,'F','0','0','system:user:remove','#','admin','2026-03-28 23:42:46','',NULL,''),(1004,'Áî®Êà∑ÂØºÂá∫',100,5,'','','','',1,0,'F','0','0','system:user:export','#','admin','2026-03-28 23:42:46','',NULL,''),(1005,'Áî®Êà∑ÂØºÂÖ•',100,6,'','','','',1,0,'F','0','0','system:user:import','#','admin','2026-03-28 23:42:47','',NULL,''),(1006,'ÈáçÁΩÆÂØÜÁ†Å',100,7,'','','','',1,0,'F','0','0','system:user:resetPwd','#','admin','2026-03-28 23:42:47','',NULL,''),(1007,'ËßíËâ≤Êü•ËØ¢',101,1,'','','','',1,0,'F','0','0','system:role:query','#','admin','2026-03-28 23:42:47','',NULL,''),(1008,'ËßíËâ≤Êñ∞Â¢û',101,2,'','','','',1,0,'F','0','0','system:role:add','#','admin','2026-03-28 23:42:47','',NULL,''),(1009,'ËßíËâ≤‰øÆÊîπ',101,3,'','','','',1,0,'F','0','0','system:role:edit','#','admin','2026-03-28 23:42:47','',NULL,''),(1010,'ËßíËâ≤Âà†Èô§',101,4,'','','','',1,0,'F','0','0','system:role:remove','#','admin','2026-03-28 23:42:47','',NULL,''),(1011,'ËßíËâ≤ÂØºÂá∫',101,5,'','','','',1,0,'F','0','0','system:role:export','#','admin','2026-03-28 23:42:47','',NULL,''),(1012,'ËèúÂçïÊü•ËØ¢',102,1,'','','','',1,0,'F','0','0','system:menu:query','#','admin','2026-03-28 23:42:47','',NULL,''),(1013,'ËèúÂçïÊñ∞Â¢û',102,2,'','','','',1,0,'F','0','0','system:menu:add','#','admin','2026-03-28 23:42:47','',NULL,''),(1014,'ËèúÂçï‰øÆÊîπ',102,3,'','','','',1,0,'F','0','0','system:menu:edit','#','admin','2026-03-28 23:42:47','',NULL,''),(1015,'ËèúÂçïÂà†Èô§',102,4,'','','','',1,0,'F','0','0','system:menu:remove','#','admin','2026-03-28 23:42:47','',NULL,''),(1016,'ÈÉ®Èó®Êü•ËØ¢',103,1,'','','','',1,0,'F','0','0','system:dept:query','#','admin','2026-03-28 23:42:47','',NULL,''),(1017,'ÈÉ®Èó®Êñ∞Â¢û',103,2,'','','','',1,0,'F','0','0','system:dept:add','#','admin','2026-03-28 23:42:47','',NULL,''),(1018,'ÈÉ®Èó®‰øÆÊîπ',103,3,'','','','',1,0,'F','0','0','system:dept:edit','#','admin','2026-03-28 23:42:48','',NULL,''),(1019,'ÈÉ®Èó®Âà†Èô§',103,4,'','','','',1,0,'F','0','0','system:dept:remove','#','admin','2026-03-28 23:42:48','',NULL,''),(1020,'Â≤ó‰ΩçÊü•ËØ¢',104,1,'','','','',1,0,'F','0','0','system:post:query','#','admin','2026-03-28 23:42:48','',NULL,''),(1021,'Â≤ó‰ΩçÊñ∞Â¢û',104,2,'','','','',1,0,'F','0','0','system:post:add','#','admin','2026-03-28 23:42:48','',NULL,''),(1022,'Â≤ó‰Ωç‰øÆÊîπ',104,3,'','','','',1,0,'F','0','0','system:post:edit','#','admin','2026-03-28 23:42:48','',NULL,''),(1023,'Â≤ó‰ΩçÂà†Èô§',104,4,'','','','',1,0,'F','0','0','system:post:remove','#','admin','2026-03-28 23:42:48','',NULL,''),(1024,'Â≤ó‰ΩçÂØºÂá∫',104,5,'','','','',1,0,'F','0','0','system:post:export','#','admin','2026-03-28 23:42:48','',NULL,''),(1025,'Â≠óÂÖ∏Êü•ËØ¢',105,1,'#','','','',1,0,'F','0','0','system:dict:query','#','admin','2026-03-28 23:42:48','',NULL,''),(1026,'Â≠óÂÖ∏Êñ∞Â¢û',105,2,'#','','','',1,0,'F','0','0','system:dict:add','#','admin','2026-03-28 23:42:48','',NULL,''),(1027,'Â≠óÂÖ∏‰øÆÊîπ',105,3,'#','','','',1,0,'F','0','0','system:dict:edit','#','admin','2026-03-28 23:42:48','',NULL,''),(1028,'Â≠óÂÖ∏Âà†Èô§',105,4,'#','','','',1,0,'F','0','0','system:dict:remove','#','admin','2026-03-28 23:42:48','',NULL,''),(1029,'Â≠óÂÖ∏ÂØºÂá∫',105,5,'#','','','',1,0,'F','0','0','system:dict:export','#','admin','2026-03-28 23:42:49','',NULL,''),(1030,'ÂèÇÊï∞Êü•ËØ¢',106,1,'#','','','',1,0,'F','0','0','system:config:query','#','admin','2026-03-28 23:42:49','',NULL,''),(1031,'ÂèÇÊï∞Êñ∞Â¢û',106,2,'#','','','',1,0,'F','0','0','system:config:add','#','admin','2026-03-28 23:42:49','',NULL,''),(1032,'ÂèÇÊï∞‰øÆÊîπ',106,3,'#','','','',1,0,'F','0','0','system:config:edit','#','admin','2026-03-28 23:42:49','',NULL,''),(1033,'ÂèÇÊï∞Âà†Èô§',106,4,'#','','','',1,0,'F','0','0','system:config:remove','#','admin','2026-03-28 23:42:49','',NULL,''),(1034,'ÂèÇÊï∞ÂØºÂá∫',106,5,'#','','','',1,0,'F','0','0','system:config:export','#','admin','2026-03-28 23:42:49','',NULL,''),(1035,'ÂÖ¨ÂëäÊü•ËØ¢',107,1,'#','','','',1,0,'F','0','0','system:notice:query','#','admin','2026-03-28 23:42:49','',NULL,''),(1036,'ÂÖ¨ÂëäÊñ∞Â¢û',107,2,'#','','','',1,0,'F','0','0','system:notice:add','#','admin','2026-03-28 23:42:49','',NULL,''),(1037,'ÂÖ¨Âëä‰øÆÊîπ',107,3,'#','','','',1,0,'F','0','0','system:notice:edit','#','admin','2026-03-28 23:42:49','',NULL,''),(1038,'ÂÖ¨ÂëäÂà†Èô§',107,4,'#','','','',1,0,'F','0','0','system:notice:remove','#','admin','2026-03-28 23:42:49','',NULL,''),(1039,'Êìç‰ΩúÊü•ËØ¢',500,1,'#','','','',1,0,'F','0','0','monitor:operlog:query','#','admin','2026-03-28 23:42:49','',NULL,''),(1040,'Êìç‰ΩúÂà†Èô§',500,2,'#','','','',1,0,'F','0','0','monitor:operlog:remove','#','admin','2026-03-28 23:42:49','',NULL,''),(1041,'Êó•ÂøóÂØºÂá∫',500,3,'#','','','',1,0,'F','0','0','monitor:operlog:export','#','admin','2026-03-28 23:42:49','',NULL,''),(1042,'ÁôªÂΩïÊü•ËØ¢',501,1,'#','','','',1,0,'F','0','0','monitor:logininfor:query','#','admin','2026-03-28 23:42:49','',NULL,''),(1043,'ÁôªÂΩïÂà†Èô§',501,2,'#','','','',1,0,'F','0','0','monitor:logininfor:remove','#','admin','2026-03-28 23:42:50','',NULL,''),(1044,'Êó•ÂøóÂØºÂá∫',501,3,'#','','','',1,0,'F','0','0','monitor:logininfor:export','#','admin','2026-03-28 23:42:50','',NULL,''),(1045,'Ë¥¶Êà∑Ëß£ÈîÅ',501,4,'#','','','',1,0,'F','0','0','monitor:logininfor:unlock','#','admin','2026-03-28 23:42:50','',NULL,''),(1046,'Âú®Á∫øÊü•ËØ¢',109,1,'#','','','',1,0,'F','0','0','monitor:online:query','#','admin','2026-03-28 23:42:50','',NULL,''),(1047,'ÊâπÈáèÂº∫ÈÄÄ',109,2,'#','','','',1,0,'F','0','0','monitor:online:batchLogout','#','admin','2026-03-28 23:42:50','',NULL,''),(1048,'ÂçïÊù°Âº∫ÈÄÄ',109,3,'#','','','',1,0,'F','0','0','monitor:online:forceLogout','#','admin','2026-03-28 23:42:50','',NULL,''),(1049,'‰ªªÂä°Êü•ËØ¢',110,1,'#','','','',1,0,'F','0','0','monitor:job:query','#','admin','2026-03-28 23:42:50','',NULL,''),(1050,'‰ªªÂä°Êñ∞Â¢û',110,2,'#','','','',1,0,'F','0','0','monitor:job:add','#','admin','2026-03-28 23:42:50','',NULL,''),(1051,'‰ªªÂä°‰øÆÊîπ',110,3,'#','','','',1,0,'F','0','0','monitor:job:edit','#','admin','2026-03-28 23:42:50','',NULL,''),(1052,'‰ªªÂä°Âà†Èô§',110,4,'#','','','',1,0,'F','0','0','monitor:job:remove','#','admin','2026-03-28 23:42:50','',NULL,''),(1053,'Áä∂ÊÄÅ‰øÆÊîπ',110,5,'#','','','',1,0,'F','0','0','monitor:job:changeStatus','#','admin','2026-03-28 23:42:50','',NULL,''),(1054,'‰ªªÂä°ÂØºÂá∫',110,6,'#','','','',1,0,'F','0','0','monitor:job:export','#','admin','2026-03-28 23:42:51','',NULL,''),(1055,'ÁîüÊàêÊü•ËØ¢',116,1,'#','','','',1,0,'F','0','0','tool:gen:query','#','admin','2026-03-28 23:42:51','',NULL,''),(1056,'ÁîüÊàê‰øÆÊîπ',116,2,'#','','','',1,0,'F','0','0','tool:gen:edit','#','admin','2026-03-28 23:42:51','',NULL,''),(1057,'ÁîüÊàêÂà†Èô§',116,3,'#','','','',1,0,'F','0','0','tool:gen:remove','#','admin','2026-03-28 23:42:51','',NULL,''),(1058,'ÂØºÂÖ•‰ª£Á†Å',116,4,'#','','','',1,0,'F','0','0','tool:gen:import','#','admin','2026-03-28 23:42:51','',NULL,''),(1059,'È¢ÑËßà‰ª£Á†Å',116,5,'#','','','',1,0,'F','0','0','tool:gen:preview','#','admin','2026-03-28 23:42:51','',NULL,''),(1060,'ÁîüÊàê‰ª£Á†Å',116,6,'#','','','',1,0,'F','0','0','tool:gen:code','#','admin','2026-03-28 23:42:51','',NULL,'');
/*!40000 ALTER TABLE `sys_menu_bk1` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_deployment_resource`
--

DROP TABLE IF EXISTS `act_cmmn_deployment_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_deployment_resource` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  `GENERATED_` tinyint DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_CMMN_RSRC_DPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_CMMN_RSRC_DPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_cmmn_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_deployment_resource`
--

LOCK TABLES `act_cmmn_deployment_resource` WRITE;
/*!40000 ALTER TABLE `act_cmmn_deployment_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_deployment_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_re_model`
--

DROP TABLE IF EXISTS `act_re_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_model` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
  KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
  KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_model`
--

LOCK TABLES `act_re_model` WRITE;
/*!40000 ALTER TABLE `act_re_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_re_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_actinst`
--

DROP TABLE IF EXISTS `act_hi_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_actinst` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_actinst`
--

LOCK TABLES `act_hi_actinst` WRITE;
/*!40000 ALTER TABLE `act_hi_actinst` DISABLE KEYS */;
INSERT INTO `act_hi_actinst` VALUES ('8c977e89-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5','start_event',NULL,NULL,'ÂºÄÂßã','startEvent',NULL,'2026-04-05 04:19:14.383','2026-04-05 04:19:14.386',1,3,NULL,''),('8c9841da-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5','Flow_1dd8wjs',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-04-05 04:19:14.388','2026-04-05 04:19:14.388',2,0,NULL,''),('8c9841db-3063-11f1-8e5a-8c1645e938b5',2,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5','Activity_1kbxzqu','8c9b00fc-3063-11f1-8e5a-8c1645e938b5',NULL,'ÂàùÂÆ°','userTask',NULL,'2026-04-05 04:19:14.388','2026-04-05 04:19:14.581',3,193,NULL,''),('8cb5dc06-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5','Flow_1ytg1zm',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-04-05 04:19:14.582','2026-04-05 04:19:14.582',1,0,NULL,''),('8cb60317-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5','Activity_0v17nhy','8cb60318-3063-11f1-8e5a-8c1645e938b5',NULL,'Â§çÂÆ°','userTask',NULL,'2026-04-05 04:19:14.583',NULL,2,NULL,NULL,''),('8ce21c3b-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5','start_event',NULL,NULL,'ÂºÄÂßã','startEvent',NULL,'2026-04-05 04:19:14.872','2026-04-05 04:19:14.872',1,0,NULL,''),('8ce21c3c-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5','Flow_1dd8wjs',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-04-05 04:19:14.872','2026-04-05 04:19:14.872',2,0,NULL,''),('8ce21c3d-3063-11f1-8e5a-8c1645e938b5',2,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5','Activity_1kbxzqu','8ce21c3e-3063-11f1-8e5a-8c1645e938b5',NULL,'ÂàùÂÆ°','userTask',NULL,'2026-04-05 04:19:14.872','2026-04-05 04:19:14.923',3,51,NULL,''),('8cea0b88-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5','Flow_1ytg1zm',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-04-05 04:19:14.924','2026-04-05 04:19:14.924',1,0,NULL,''),('8cea59a9-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5','Activity_0v17nhy','8cea59aa-3063-11f1-8e5a-8c1645e938b5',NULL,'Â§çÂÆ°','userTask',NULL,'2026-04-05 04:19:14.926',NULL,2,NULL,NULL,'');
/*!40000 ALTER TABLE `act_hi_actinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_casedef`
--

DROP TABLE IF EXISTS `act_cmmn_casedef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_casedef` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `VERSION_` int NOT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_CASE_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`),
  KEY `ACT_IDX_CASE_DEF_DPLY` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_CASE_DEF_DPLY` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_cmmn_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_casedef`
--

LOCK TABLES `act_cmmn_casedef` WRITE;
/*!40000 ALTER TABLE `act_cmmn_casedef` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_casedef` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_taskinst`
--

DROP TABLE IF EXISTS `act_hi_taskinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_taskinst` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `FORM_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_taskinst`
--

LOCK TABLES `act_hi_taskinst` WRITE;
/*!40000 ALTER TABLE `act_hi_taskinst` DISABLE KEYS */;
INSERT INTO `act_hi_taskinst` VALUES ('8c9b00fc-3063-11f1-8e5a-8c1645e938b5',2,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',NULL,'Activity_1kbxzqu','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,'completed','ÂàùÂÆ°',NULL,NULL,NULL,NULL,'2026-04-05 04:19:14.389',NULL,NULL,NULL,NULL,NULL,NULL,'2026-04-05 04:19:14.578',NULL,189,NULL,50,NULL,NULL,NULL,NULL,'','2026-04-05 04:19:14.578'),('8cb60318-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',NULL,'Activity_0v17nhy','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,'created','Â§çÂÆ°',NULL,NULL,NULL,NULL,'2026-04-05 04:19:14.583',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,50,NULL,NULL,NULL,NULL,'','2026-04-05 04:19:14.583'),('8ce21c3e-3063-11f1-8e5a-8c1645e938b5',2,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',NULL,'Activity_1kbxzqu','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,'completed','ÂàùÂÆ°',NULL,NULL,NULL,NULL,'2026-04-05 04:19:14.872',NULL,NULL,NULL,NULL,NULL,NULL,'2026-04-05 04:19:14.922',NULL,50,NULL,50,NULL,NULL,NULL,NULL,'','2026-04-05 04:19:14.922'),('8cea59aa-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',NULL,'Activity_0v17nhy','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL,NULL,'created','Â§çÂÆ°',NULL,NULL,NULL,NULL,'2026-04-05 04:19:14.926',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,50,NULL,NULL,NULL,NULL,'','2026-04-05 04:19:14.926');
/*!40000 ALTER TABLE `act_hi_taskinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_identitylink`
--

DROP TABLE IF EXISTS `act_hi_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_identitylink` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_identitylink`
--

LOCK TABLES `act_hi_identitylink` WRITE;
/*!40000 ALTER TABLE `act_hi_identitylink` DISABLE KEYS */;
INSERT INTO `act_hi_identitylink` VALUES ('8c95f7d8-3063-11f1-8e5a-8c1645e938b5',NULL,'starter','1',NULL,'2026-04-05 04:19:14.373','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL),('8cb518b5-3063-11f1-8e5a-8c1645e938b5',NULL,'participant','1',NULL,'2026-04-05 04:19:14.577','8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL),('8ce1f51a-3063-11f1-8e5a-8c1645e938b5',NULL,'starter','1',NULL,'2026-04-05 04:19:14.871','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL),('8ce99657-3063-11f1-8e5a-8c1645e938b5',NULL,'participant','1',NULL,'2026-04-05 04:19:14.921','8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `act_hi_identitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_procinst`
--

DROP TABLE IF EXISTS `act_hi_procinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_procinst` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `END_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDX_HI_PRO_SUPER_PROCINST` (`SUPER_PROCESS_INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_procinst`
--

LOCK TABLES `act_hi_procinst` WRITE;
/*!40000 ALTER TABLE `act_hi_procinst` DISABLE KEYS */;
INSERT INTO `act_hi_procinst` VALUES ('8c95d0c7-3063-11f1-8e5a-8c1645e938b5',1,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','2026-04-05 04:19:14.372',NULL,NULL,'1','start_event',NULL,NULL,NULL,'',NULL,NULL,NULL,NULL,NULL,NULL,NULL),('8ce1f519-3063-11f1-8e5a-8c1645e938b5',1,'8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','2026-04-05 04:19:14.871',NULL,NULL,'1','start_event',NULL,NULL,NULL,'',NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `act_hi_procinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_re_procdef`
--

DROP TABLE IF EXISTS `act_re_procdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_procdef` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `VERSION_` int NOT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `ENGINE_VERSION_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_VERSION_` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_procdef`
--

LOCK TABLES `act_re_procdef` WRITE;
/*!40000 ALTER TABLE `act_re_procdef` DISABLE KEYS */;
INSERT INTO `act_re_procdef` VALUES ('flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',2,'leave','flow_1hxp265d','flow_cl1udrgp',2,'891a4294-305c-11f1-82b6-8c1645e938b5','flow_1hxp265d.bpmn','flow_1hxp265d.flow_cl1udrgp.png',NULL,0,1,1,'',NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `act_re_procdef` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_app_deployment_resource`
--

DROP TABLE IF EXISTS `act_app_deployment_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_deployment_resource` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_APP_RSRC_DPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_APP_RSRC_DPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_app_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_app_deployment_resource`
--

LOCK TABLES `act_app_deployment_resource` WRITE;
/*!40000 ALTER TABLE `act_app_deployment_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_app_deployment_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ÈÉ®Èó®ID',
  `parent_id` bigint DEFAULT '0' COMMENT 'Áà∂ÈÉ®Èó®ID',
  `ancestors` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Á•ñÁ∫ßÂàóË°®',
  `dept_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÈÉ®Èó®ÂêçÁß∞',
  `order_num` int DEFAULT '0' COMMENT 'ÊòæÁ§∫È°∫Â∫è',
  `leader` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ë¥üË¥£‰∫∫',
  `phone` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ËÅîÁ≥ªÁîµËØù',
  `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÈÇÆÁÆ±',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'ÈÉ®Èó®Áä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÂÅúÁî®Ôºâ',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'Âà†Èô§Ê†áÂøóÔºà0‰ª£Ë°®Â≠òÂú® 2‰ª£Ë°®Âà†Èô§Ôºâ',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  PRIMARY KEY (`dept_id`),
  KEY `idx_sys_dept_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ÈÉ®Èó®Ë°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
INSERT INTO `sys_dept` VALUES (100,0,'0','Â¢®ÈüµÁßëÊäÄ',0,'Â¢®Èüµ','15888888888','ry@qq.com','0','0','admin','2026-04-22 01:46:27','',NULL),(101,100,'0,100','Ê∑±Âú≥ÊÄªÂÖ¨Âè∏',1,'Â¢®Èüµ','15888888888','ry@qq.com','0','0','admin','2026-04-22 01:46:27','',NULL),(102,100,'0,100','ÈïøÊ≤ôÂàÜÂÖ¨Âè∏',2,'Â¢®Èüµ','15888888888','ry@qq.com','0','0','admin','2026-04-22 01:46:27','',NULL),(103,101,'0,100,101','Á†îÂèëÈÉ®Èó®',1,'Â¢®Èüµ','15888888888','ry@qq.com','0','0','admin','2026-04-22 01:46:27','admin','2026-05-27 02:36:47'),(104,101,'0,100,101','Â∏ÇÂú∫ÈÉ®Èó®',2,'Â¢®Èüµ','15888888888','ry@qq.com','0','0','admin','2026-04-22 01:46:27','',NULL),(105,101,'0,100,101','ÊµãËØïÈÉ®Èó®',3,'Â¢®Èüµ','15888888888','ry@qq.com','0','0','admin','2026-04-22 01:46:27','',NULL),(106,101,'0,100,101','Ë¥¢Âä°ÈÉ®Èó®',4,'Â¢®Èüµ','15888888888','ry@qq.com','0','0','admin','2026-04-22 01:46:27','',NULL),(107,101,'0,100,101','ËøêÁª¥ÈÉ®Èó®',5,'Â¢®Èüµ','15888888888','ry@qq.com','0','0','admin','2026-04-22 01:46:27','',NULL),(108,102,'0,100,102','Â∏ÇÂú∫ÈÉ®Èó®',1,'Â¢®Èüµ','15888888888','ry@qq.com','0','0','admin','2026-04-22 01:46:27','',NULL),(109,102,'0,100,102','Ë¥¢Âä°ÈÉ®Èó®',2,'Â¢®Èüµ','15888888888','ry@qq.com','0','0','admin','2026-04-22 01:46:27','',NULL);
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_paused_trigger_grps`
--

DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_paused_trigger_grps` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `trigger_group` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_groupÁöÑÂ§ñÈîÆ',
  PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ÊöÇÂÅúÁöÑËß¶ÂèëÂô®Ë°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_paused_trigger_grps`
--

LOCK TABLES `qrtz_paused_trigger_grps` WRITE;
/*!40000 ALTER TABLE `qrtz_paused_trigger_grps` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_paused_trigger_grps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_app_deployment`
--

DROP TABLE IF EXISTS `act_app_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_app_deployment` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_app_deployment`
--

LOCK TABLES `act_app_deployment` WRITE;
/*!40000 ALTER TABLE `act_app_deployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_app_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_evt_log`
--

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

--
-- Dumping data for table `act_evt_log`
--

LOCK TABLES `act_evt_log` WRITE;
/*!40000 ALTER TABLE `act_evt_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_evt_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_deploy_form`
--

DROP TABLE IF EXISTS `sys_deploy_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_deploy_form` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '‰∏ªÈîÆ',
  `form_id` bigint DEFAULT NULL COMMENT 'Ë°®Âçï‰∏ªÈîÆ',
  `deploy_id` varchar(50) DEFAULT NULL COMMENT 'ÊµÅÁ®ãÂÆû‰æã‰∏ªÈîÆ',
  PRIMARY KEY (`id`),
  KEY `idx_sys_deploy_form_form` (`form_id`),
  KEY `idx_sys_deploy_form_deploy` (`deploy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ÊµÅÁ®ãÂÆû‰æãÂÖ≥ËÅîË°®Âçï';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_deploy_form`
--

LOCK TABLES `sys_deploy_form` WRITE;
/*!40000 ALTER TABLE `sys_deploy_form` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_deploy_form` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT 'Â≠óÂÖ∏ÁºñÁ†Å',
  `dict_sort` int DEFAULT '0' COMMENT 'Â≠óÂÖ∏ÊéíÂ∫è',
  `dict_label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Â≠óÂÖ∏Ê†áÁ≠æ',
  `dict_value` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Â≠óÂÖ∏ÈîÆÂÄº',
  `dict_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Â≠óÂÖ∏Á±ªÂûã',
  `css_class` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ê†∑ÂºèÂ±ûÊÄßÔºàÂÖ∂‰ªñÊ†∑ÂºèÊâ©Â±ïÔºâ',
  `list_class` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ë°®Ê†ºÂõûÊòæÊ†∑Âºè',
  `is_default` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT 'ÊòØÂê¶ÈªòËÆ§ÔºàYÊòØ NÂê¶Ôºâ',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'Áä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1ÂÅúÁî®Ôºâ',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`dict_code`),
  KEY `idx_sys_dict_data_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Â≠óÂÖ∏Êï∞ÊçÆË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
INSERT INTO `sys_dict_data` VALUES (1,1,'Áî∑','0','sys_user_sex','','','Y','0','admin','2026-04-22 01:46:29','',NULL,'ÊÄßÂà´Áî∑'),(2,2,'Â•≥','1','sys_user_sex','','','N','0','admin','2026-04-22 01:46:29','',NULL,'ÊÄßÂà´Â•≥'),(3,3,'Êú™Áü•','2','sys_user_sex','','','N','0','admin','2026-04-22 01:46:29','',NULL,'ÊÄßÂà´Êú™Áü•'),(4,1,'ÊòæÁ§∫','0','sys_show_hide','','primary','Y','0','admin','2026-04-22 01:46:29','',NULL,'ÊòæÁ§∫ËèúÂçï'),(5,2,'ÈöêËóè','1','sys_show_hide','','danger','N','0','admin','2026-04-22 01:46:29','',NULL,'ÈöêËóèËèúÂçï'),(6,1,'Ê≠£Â∏∏','0','sys_normal_disable','','primary','Y','0','admin','2026-04-22 01:46:29','',NULL,'Ê≠£Â∏∏Áä∂ÊÄÅ'),(7,2,'ÂÅúÁî®','1','sys_normal_disable','','danger','N','0','admin','2026-04-22 01:46:29','',NULL,'ÂÅúÁî®Áä∂ÊÄÅ'),(8,1,'Ê≠£Â∏∏','0','sys_job_status','','primary','Y','0','admin','2026-04-22 01:46:29','',NULL,'Ê≠£Â∏∏Áä∂ÊÄÅ'),(9,2,'ÊöÇÂÅú','1','sys_job_status','','danger','N','0','admin','2026-04-22 01:46:29','',NULL,'ÂÅúÁî®Áä∂ÊÄÅ'),(10,1,'ÈªòËÆ§','DEFAULT','sys_job_group','','','Y','0','admin','2026-04-22 01:46:29','',NULL,'ÈªòËÆ§ÂàÜÁªÑ'),(11,2,'Á≥ªÁªü','SYSTEM','sys_job_group','','','N','0','admin','2026-04-22 01:46:29','',NULL,'Á≥ªÁªüÂàÜÁªÑ'),(12,1,'ÊòØ','Y','sys_yes_no','','primary','Y','0','admin','2026-04-22 01:46:29','',NULL,'Á≥ªÁªüÈªòËÆ§ÊòØ'),(13,2,'Âê¶','N','sys_yes_no','','danger','N','0','admin','2026-04-22 01:46:29','',NULL,'Á≥ªÁªüÈªòËÆ§Âê¶'),(14,1,'ÈÄöÁü•','1','sys_notice_type','','warning','Y','0','admin','2026-04-22 01:46:29','',NULL,'ÈÄöÁü•'),(15,2,'ÂÖ¨Âëä','2','sys_notice_type','','success','N','0','admin','2026-04-22 01:46:29','',NULL,'ÂÖ¨Âëä'),(16,1,'Ê≠£Â∏∏','0','sys_notice_status','','primary','Y','0','admin','2026-04-22 01:46:29','',NULL,'Ê≠£Â∏∏Áä∂ÊÄÅ'),(17,2,'ÂÖ≥Èó≠','1','sys_notice_status','','danger','N','0','admin','2026-04-22 01:46:29','',NULL,'ÂÖ≥Èó≠Áä∂ÊÄÅ'),(18,99,'ÂÖ∂‰ªñ','0','sys_oper_type','','info','N','0','admin','2026-04-22 01:46:29','',NULL,'ÂÖ∂‰ªñÊìç‰Ωú'),(19,1,'Êñ∞Â¢û','1','sys_oper_type','','info','N','0','admin','2026-04-22 01:46:29','',NULL,'Êñ∞Â¢ûÊìç‰Ωú'),(20,2,'‰øÆÊîπ','2','sys_oper_type','','info','N','0','admin','2026-04-22 01:46:29','',NULL,'‰øÆÊîπÊìç‰Ωú'),(21,3,'Âà†Èô§','3','sys_oper_type','','danger','N','0','admin','2026-04-22 01:46:29','',NULL,'Âà†Èô§Êìç‰Ωú'),(22,4,'ÊéàÊùÉ','4','sys_oper_type','','primary','N','0','admin','2026-04-22 01:46:29','',NULL,'ÊéàÊùÉÊìç‰Ωú'),(23,5,'ÂØºÂá∫','5','sys_oper_type','','warning','N','0','admin','2026-04-22 01:46:29','',NULL,'ÂØºÂá∫Êìç‰Ωú'),(24,6,'ÂØºÂÖ•','6','sys_oper_type','','warning','N','0','admin','2026-04-22 01:46:29','',NULL,'ÂØºÂÖ•Êìç‰Ωú'),(25,7,'Âº∫ÈÄÄ','7','sys_oper_type','','danger','N','0','admin','2026-04-22 01:46:29','',NULL,'Âº∫ÈÄÄÊìç‰Ωú'),(26,8,'ÁîüÊàê‰ª£Á†Å','8','sys_oper_type','','warning','N','0','admin','2026-04-22 01:46:29','',NULL,'ÁîüÊàêÊìç‰Ωú'),(27,9,'Ê∏ÖÁ©∫Êï∞ÊçÆ','9','sys_oper_type','','danger','N','0','admin','2026-04-22 01:46:29','',NULL,'Ê∏ÖÁ©∫Êìç‰Ωú'),(28,1,'ÊàêÂäü','0','sys_common_status','','primary','N','0','admin','2026-04-22 01:46:29','',NULL,'Ê≠£Â∏∏Áä∂ÊÄÅ'),(29,2,'Â§±Ë¥•','1','sys_common_status','','danger','N','0','admin','2026-04-22 01:46:29','',NULL,'ÂÅúÁî®Áä∂ÊÄÅ'),(100,0,'Á≥ªÁªüÊåáÂÆö','fixed','exp_data_type',NULL,'default','N','0','admin','2026-04-22 01:46:52','',NULL,NULL),(101,1,'Âä®ÊÄÅÈÄâÊã©','dynamic','exp_data_type',NULL,'default','N','0','admin','2026-04-22 01:46:52','',NULL,NULL),(102,0,'‰ªªÂä°ÁõëÂê¨','1','sys_listener_type',NULL,'default','N','0','admin','2026-04-22 01:46:52','',NULL,NULL),(103,2,'ÊâßË°åÁõëÂê¨','2','sys_listener_type',NULL,'default','N','0','admin','2026-04-22 01:46:52','',NULL,NULL),(104,0,'JAVAÁ±ª','classListener','sys_listener_value_type',NULL,'default','N','0','admin','2026-04-22 01:46:52','',NULL,NULL),(105,1,'Ë°®ËææÂºè','expressionListener','sys_listener_value_type',NULL,'default','N','0','admin','2026-04-22 01:46:52','',NULL,NULL),(106,2,'‰ª£ÁêÜË°®ËææÂºè','delegateExpressionListener','sys_listener_value_type',NULL,'default','N','0','admin','2026-04-22 01:46:52','',NULL,NULL),(107,0,'ËØ∑ÂÅá','leave','sys_process_category',NULL,'default','N','0','admin','2026-04-22 01:46:52','',NULL,NULL),(108,1,'Êä•ÈîÄ','expense','sys_process_category',NULL,'default','N','0','admin','2026-04-22 01:46:52','',NULL,NULL);
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_blob_triggers`
--

DROP TABLE IF EXISTS `qrtz_blob_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_blob_triggers` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `trigger_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_nameÁöÑÂ§ñÈîÆ',
  `trigger_group` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_triggersË°®trigger_groupÁöÑÂ§ñÈîÆ',
  `blob_data` blob COMMENT 'Â≠òÊîæÊåÅ‰πÖÂåñTriggerÂØπË±°',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BlobÁ±ªÂûãÁöÑËß¶ÂèëÂô®Ë°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_blob_triggers`
--

LOCK TABLES `qrtz_blob_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_blob_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_blob_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT 'Áî®Êà∑ID',
  `role_id` bigint NOT NULL COMMENT 'ËßíËâ≤ID',
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_sys_user_role_user` (`user_id`),
  KEY `idx_sys_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Áî®Êà∑ÂíåËßíËâ≤ÂÖ≥ËÅîË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1),(2,2);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_locks`
--

DROP TABLE IF EXISTS `qrtz_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_locks` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `lock_name` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÊÇ≤ËßÇÈîÅÂêçÁß∞',
  PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Â≠òÂÇ®ÁöÑÊÇ≤ËßÇÈîÅ‰ø°ÊÅØË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_locks`
--

LOCK TABLES `qrtz_locks` WRITE;
/*!40000 ALTER TABLE `qrtz_locks` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_locks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `admin_users`
--

DROP TABLE IF EXISTS `admin_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_users` (
  `id` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÁÆ°ÁêÜÂëòID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Áî®Êà∑Âêç',
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ÂØÜÁ†ÅÂìàÂ∏å',
  `real_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÁúüÂÆûÂßìÂêç',
  `role` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ËßíËâ≤',
  `permissions` json DEFAULT NULL COMMENT 'ÊùÉÈôêÂàóË°®',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÈÇÆÁÆ±',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊâãÊú∫Âè∑',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â§¥ÂÉè',
  `last_login_time` datetime DEFAULT NULL COMMENT 'ÊúÄÂêéÁôªÂΩïÊó∂Èó¥',
  `last_login_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÊúÄÂêéÁôªÂΩïIP',
  `status` tinyint DEFAULT '1' COMMENT 'Áä∂ÊÄÅ: 0-Á¶ÅÁî® 1-Ê≠£Â∏∏',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ÁÆ°ÁêÜÂëòË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_users`
--

LOCK TABLES `admin_users` WRITE;
/*!40000 ALTER TABLE `admin_users` DISABLE KEYS */;
INSERT INTO `admin_users` VALUES ('4ebf61dd208411f1b6208c1645e938b5','admin','$2a$10$4N0x.xQyQzQzQzQzQzQzQzQzQzQzQzQzQzQzQzQzQzQzQzQzQzQzQ','Á≥ªÁªüÁÆ°ÁêÜÂëò','SUPER_ADMIN','[\"user:manage\", \"order:audit\", \"system:config\"]','admin@fortune.com','19987671567',NULL,NULL,NULL,1,'2026-03-15 23:33:25','2026-03-16 09:30:10');
/*!40000 ALTER TABLE `admin_users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_suspended_job`
--

DROP TABLE IF EXISTS `act_ru_suspended_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_suspended_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_SJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_SUSPENDED_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_suspended_job`
--

LOCK TABLES `act_ru_suspended_job` WRITE;
/*!40000 ALTER TABLE `act_ru_suspended_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_suspended_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_ru_sentry_part_inst`
--

DROP TABLE IF EXISTS `act_cmmn_ru_sentry_part_inst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_ru_sentry_part_inst` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `REV_` int NOT NULL,
  `CASE_DEF_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CASE_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PLAN_ITEM_INST_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ON_PART_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IF_PART_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TIME_STAMP_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_SENTRY_CASE_DEF` (`CASE_DEF_ID_`),
  KEY `ACT_IDX_SENTRY_CASE_INST` (`CASE_INST_ID_`),
  KEY `ACT_IDX_SENTRY_PLAN_ITEM` (`PLAN_ITEM_INST_ID_`),
  CONSTRAINT `ACT_FK_SENTRY_CASE_DEF` FOREIGN KEY (`CASE_DEF_ID_`) REFERENCES `act_cmmn_casedef` (`ID_`),
  CONSTRAINT `ACT_FK_SENTRY_CASE_INST` FOREIGN KEY (`CASE_INST_ID_`) REFERENCES `act_cmmn_ru_case_inst` (`ID_`),
  CONSTRAINT `ACT_FK_SENTRY_PLAN_ITEM` FOREIGN KEY (`PLAN_ITEM_INST_ID_`) REFERENCES `act_cmmn_ru_plan_item_inst` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_ru_sentry_part_inst`
--

LOCK TABLES `act_cmmn_ru_sentry_part_inst` WRITE;
/*!40000 ALTER TABLE `act_cmmn_ru_sentry_part_inst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_ru_sentry_part_inst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_execution`
--

DROP TABLE IF EXISTS `act_ru_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_execution` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_EXEC_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_ACTIVE_` tinyint DEFAULT NULL,
  `IS_CONCURRENT_` tinyint DEFAULT NULL,
  `IS_SCOPE_` tinyint DEFAULT NULL,
  `IS_EVENT_SCOPE_` tinyint DEFAULT NULL,
  `IS_MI_ROOT_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `CACHED_ENT_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `EVT_SUBSCR_COUNT_` int DEFAULT NULL,
  `TASK_COUNT_` int DEFAULT NULL,
  `JOB_COUNT_` int DEFAULT NULL,
  `TIMER_JOB_COUNT_` int DEFAULT NULL,
  `SUSP_JOB_COUNT_` int DEFAULT NULL,
  `DEADLETTER_JOB_COUNT_` int DEFAULT NULL,
  `EXTERNAL_WORKER_JOB_COUNT_` int DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDC_EXEC_ROOT` (`ROOT_PROC_INST_ID_`),
  KEY `ACT_IDX_EXEC_REF_ID_` (`REFERENCE_ID_`),
  KEY `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_EXE_PARENT` (`PARENT_ID_`),
  KEY `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`),
  KEY `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_EXE_PARENT` FOREIGN KEY (`PARENT_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE,
  CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_EXE_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `ACT_FK_EXE_SUPER` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_execution`
--

LOCK TABLES `act_ru_execution` WRITE;
/*!40000 ALTER TABLE `act_ru_execution` DISABLE KEYS */;
INSERT INTO `act_ru_execution` VALUES ('8c95d0c7-3063-11f1-8e5a-8c1645e938b5',1,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',NULL,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,1,0,1,0,0,1,NULL,'',NULL,'start_event','2026-04-05 04:19:14.372','1',NULL,NULL,1,0,0,0,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL),('8c977e88-3063-11f1-8e5a-8c1645e938b5',2,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5',NULL,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5','flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',NULL,'8c95d0c7-3063-11f1-8e5a-8c1645e938b5','Activity_0v17nhy',1,0,0,0,0,1,NULL,'',NULL,NULL,'2026-04-05 04:19:14.383',NULL,NULL,NULL,1,0,1,0,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL),('8ce1f519-3063-11f1-8e5a-8c1645e938b5',1,'8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,NULL,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',NULL,'8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,1,0,1,0,0,1,NULL,'',NULL,'start_event','2026-04-05 04:19:14.871','1',NULL,NULL,1,0,0,0,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL),('8ce21c3a-3063-11f1-8e5a-8c1645e938b5',2,'8ce1f519-3063-11f1-8e5a-8c1645e938b5',NULL,'8ce1f519-3063-11f1-8e5a-8c1645e938b5','flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5',NULL,'8ce1f519-3063-11f1-8e5a-8c1645e938b5','Activity_0v17nhy',1,0,0,0,0,1,NULL,'',NULL,NULL,'2026-04-05 04:19:14.872',NULL,NULL,NULL,1,0,1,0,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `act_ru_execution` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_dmn_deployment_resource`
--

DROP TABLE IF EXISTS `act_dmn_deployment_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_dmn_deployment_resource` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_dmn_deployment_resource`
--

LOCK TABLES `act_dmn_deployment_resource` WRITE;
/*!40000 ALTER TABLE `act_dmn_deployment_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_dmn_deployment_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_job_log`
--

DROP TABLE IF EXISTS `sys_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '‰ªªÂä°Êó•ÂøóID',
  `job_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '‰ªªÂä°ÂêçÁß∞',
  `job_group` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '‰ªªÂä°ÁªÑÂêç',
  `invoke_target` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÁî®ÁõÆÊ†áÂ≠óÁ¨¶‰∏≤',
  `job_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Êó•Âøó‰ø°ÊÅØ',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'ÊâßË°åÁä∂ÊÄÅÔºà0Ê≠£Â∏∏ 1Â§±Ë¥•Ôºâ',
  `exception_info` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂºÇÂ∏∏‰ø°ÊÅØ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  PRIMARY KEY (`job_log_id`),
  KEY `idx_sys_job_log_create` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ÂÆöÊó∂‰ªªÂä°Ë∞ÉÂ∫¶Êó•ÂøóË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_job_log`
--

LOCK TABLES `sys_job_log` WRITE;
/*!40000 ALTER TABLE `sys_job_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_job_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ÁºñÂè∑',
  `table_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Ë°®ÂêçÁß∞',
  `table_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Ë°®ÊèèËø∞',
  `sub_table_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÂÖ≥ËÅîÂ≠êË°®ÁöÑË°®Âêç',
  `sub_table_fk_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â≠êË°®ÂÖ≥ËÅîÁöÑÂ§ñÈîÆÂêç',
  `class_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂÆû‰ΩìÁ±ªÂêçÁß∞',
  `tpl_category` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT 'crud' COMMENT '‰ΩøÁî®ÁöÑÊ®°ÊùøÔºàcrudÂçïË°®Êìç‰Ωú treeÊ†ëË°®Êìç‰ΩúÔºâ',
  `tpl_web_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂâçÁ´ØÊ®°ÊùøÁ±ªÂûãÔºàelement-uiÊ®°Áâà element-plusÊ®°ÁâàÔºâ',
  `package_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÁîüÊàêÂåÖË∑ØÂæÑ',
  `module_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÁîüÊàêÊ®°ÂùóÂêç',
  `business_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÁîüÊàê‰∏öÂä°Âêç',
  `function_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÁîüÊàêÂäüËÉΩÂêç',
  `function_author` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÁîüÊàêÂäüËÉΩ‰ΩúËÄÖ',
  `gen_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'ÁîüÊàê‰ª£Á†ÅÊñπÂºèÔºà0zipÂéãÁº©ÂåÖ 1Ëá™ÂÆö‰πâË∑ØÂæÑÔºâ',
  `gen_path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '/' COMMENT 'ÁîüÊàêË∑ØÂæÑÔºà‰∏çÂ°´ÈªòËÆ§È°πÁõÆË∑ØÂæÑÔºâ',
  `options` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ÂÖ∂ÂÆÉÁîüÊàêÈÄâÈ°π',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'ÂàõÂª∫ËÄÖ',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Êõ¥Êñ∞ËÄÖ',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='‰ª£Á†ÅÁîüÊàê‰∏öÂä°Ë°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table`
--

LOCK TABLES `gen_table` WRITE;
/*!40000 ALTER TABLE `gen_table` DISABLE KEYS */;
/*!40000 ALTER TABLE `gen_table` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_expression`
--

DROP TABLE IF EXISTS `sys_expression`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_expression` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '‰∏ªÈîÆ',
  `name` varchar(50) DEFAULT NULL COMMENT 'Ë°®ËææÂºèÂêçÁß∞',
  `expression` varchar(255) DEFAULT NULL COMMENT 'Ë°®ËææÂºèÂÜÖÂÆπ',
  `data_type` varchar(255) DEFAULT NULL COMMENT 'Ë°®ËææÂºèÁ±ªÂûã',
  `create_time` datetime DEFAULT NULL COMMENT 'ÂàõÂª∫Êó∂Èó¥',
  `update_time` datetime DEFAULT NULL COMMENT 'Êõ¥Êñ∞Êó∂Èó¥',
  `create_by` bigint DEFAULT NULL COMMENT 'ÂàõÂª∫‰∫∫Âëò',
  `update_by` bigint DEFAULT NULL COMMENT 'Êõ¥Êñ∞‰∫∫Âëò',
  `status` tinyint DEFAULT '0' COMMENT 'Áä∂ÊÄÅ',
  `remark` varchar(255) DEFAULT NULL COMMENT 'Â§áÊ≥®',
  PRIMARY KEY (`id`),
  KEY `idx_sys_expression_type` (`data_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ÊµÅÁ®ãË°®ËææÂºè';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_expression`
--

LOCK TABLES `sys_expression` WRITE;
/*!40000 ALTER TABLE `sys_expression` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_expression` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_timer_job`
--

DROP TABLE IF EXISTS `act_ru_timer_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_timer_job` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_TIMER_JOB_DUEDATE` (`DUEDATE_`),
  KEY `ACT_IDX_TJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TIMER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_timer_job`
--

LOCK TABLES `act_ru_timer_job` WRITE;
/*!40000 ALTER TABLE `act_ru_timer_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_timer_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_triggers`
--

DROP TABLE IF EXISTS `qrtz_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_triggers` (
  `sched_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ë∞ÉÂ∫¶ÂêçÁß∞',
  `trigger_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ëß¶ÂèëÂô®ÁöÑÂêçÂ≠ó',
  `trigger_group` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ëß¶ÂèëÂô®ÊâÄÂ±ûÁªÑÁöÑÂêçÂ≠ó',
  `job_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_job_detailsË°®job_nameÁöÑÂ§ñÈîÆ',
  `job_group` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qrtz_job_detailsË°®job_groupÁöÑÂ§ñÈîÆ',
  `description` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Áõ∏ÂÖ≥‰ªãÁªç',
  `next_fire_time` bigint DEFAULT NULL COMMENT '‰∏ä‰∏ÄÊ¨°Ëß¶ÂèëÊó∂Èó¥ÔºàÊØ´ÁßíÔºâ',
  `prev_fire_time` bigint DEFAULT NULL COMMENT '‰∏ã‰∏ÄÊ¨°Ëß¶ÂèëÊó∂Èó¥ÔºàÈªòËÆ§‰∏∫-1Ë°®Á§∫‰∏çËß¶ÂèëÔºâ',
  `priority` int DEFAULT NULL COMMENT '‰ºòÂÖàÁ∫ß',
  `trigger_state` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ëß¶ÂèëÂô®Áä∂ÊÄÅ',
  `trigger_type` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ëß¶ÂèëÂô®ÁöÑÁ±ªÂûã',
  `start_time` bigint NOT NULL COMMENT 'ÂºÄÂßãÊó∂Èó¥',
  `end_time` bigint DEFAULT NULL COMMENT 'ÁªìÊùüÊó∂Èó¥',
  `calendar_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Êó•Á®ãË°®ÂêçÁß∞',
  `misfire_instr` smallint DEFAULT NULL COMMENT 'Ë°•ÂÅøÊâßË°åÁöÑÁ≠ñÁï•',
  `job_data` blob COMMENT 'Â≠òÊîæÊåÅ‰πÖÂåñjobÂØπË±°',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  KEY `idx_qrtz_triggers_job` (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ëß¶ÂèëÂô®ËØ¶ÁªÜ‰ø°ÊÅØË°®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_triggers`
--

LOCK TABLES `qrtz_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_ru_batch`
--

DROP TABLE IF EXISTS `flw_ru_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_ru_batch` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `SEARCH_KEY_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `BATCH_DOC_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_ru_batch`
--

LOCK TABLES `flw_ru_batch` WRITE;
/*!40000 ALTER TABLE `flw_ru_batch` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_ru_batch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_cmmn_deployment`
--

DROP TABLE IF EXISTS `act_cmmn_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_cmmn_deployment` (
  `ID_` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_cmmn_deployment`
--

LOCK TABLES `act_cmmn_deployment` WRITE;
/*!40000 ALTER TABLE `act_cmmn_deployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_cmmn_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_actinst`
--

DROP TABLE IF EXISTS `act_ru_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_actinst` (
  `ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_ACTI_START` (`START_TIME_`),
  KEY `ACT_IDX_RU_ACTI_END` (`END_TIME_`),
  KEY `ACT_IDX_RU_ACTI_PROC` (`PROC_INST_ID_`),
  KEY `ACT_IDX_RU_ACTI_PROC_ACT` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC` (`EXECUTION_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC_ACT` (`EXECUTION_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_actinst`
--

LOCK TABLES `act_ru_actinst` WRITE;
/*!40000 ALTER TABLE `act_ru_actinst` DISABLE KEYS */;
INSERT INTO `act_ru_actinst` VALUES ('8c977e89-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5','start_event',NULL,NULL,'ÂºÄÂßã','startEvent',NULL,'2026-04-05 04:19:14.383','2026-04-05 04:19:14.386',3,1,NULL,''),('8c9841da-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5','Flow_1dd8wjs',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-04-05 04:19:14.388','2026-04-05 04:19:14.388',0,2,NULL,''),('8c9841db-3063-11f1-8e5a-8c1645e938b5',2,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5','Activity_1kbxzqu','8c9b00fc-3063-11f1-8e5a-8c1645e938b5',NULL,'ÂàùÂÆ°','userTask',NULL,'2026-04-05 04:19:14.388','2026-04-05 04:19:14.581',193,3,NULL,''),('8cb5dc06-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5','Flow_1ytg1zm',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-04-05 04:19:14.582','2026-04-05 04:19:14.582',0,1,NULL,''),('8cb60317-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8c95d0c7-3063-11f1-8e5a-8c1645e938b5','8c977e88-3063-11f1-8e5a-8c1645e938b5','Activity_0v17nhy','8cb60318-3063-11f1-8e5a-8c1645e938b5',NULL,'Â§çÂÆ°','userTask',NULL,'2026-04-05 04:19:14.583',NULL,NULL,2,NULL,''),('8ce21c3b-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5','start_event',NULL,NULL,'ÂºÄÂßã','startEvent',NULL,'2026-04-05 04:19:14.872','2026-04-05 04:19:14.872',0,1,NULL,''),('8ce21c3c-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5','Flow_1dd8wjs',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-04-05 04:19:14.872','2026-04-05 04:19:14.872',0,2,NULL,''),('8ce21c3d-3063-11f1-8e5a-8c1645e938b5',2,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5','Activity_1kbxzqu','8ce21c3e-3063-11f1-8e5a-8c1645e938b5',NULL,'ÂàùÂÆ°','userTask',NULL,'2026-04-05 04:19:14.872','2026-04-05 04:19:14.923',51,3,NULL,''),('8cea0b88-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5','Flow_1ytg1zm',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-04-05 04:19:14.924','2026-04-05 04:19:14.924',0,1,NULL,''),('8cea59a9-3063-11f1-8e5a-8c1645e938b5',1,'flow_cl1udrgp:2:892713d7-305c-11f1-82b6-8c1645e938b5','8ce1f519-3063-11f1-8e5a-8c1645e938b5','8ce21c3a-3063-11f1-8e5a-8c1645e938b5','Activity_0v17nhy','8cea59aa-3063-11f1-8e5a-8c1645e938b5',NULL,'Â§çÂÆ°','userTask',NULL,'2026-04-05 04:19:14.926',NULL,NULL,2,NULL,'');
/*!40000 ALTER TABLE `act_ru_actinst` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-27  2:43:09
