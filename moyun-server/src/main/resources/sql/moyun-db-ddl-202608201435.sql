-- MySQL dump 10.13  Distrib 8.4.6, for Win64 (x86_64)
--
-- Host: localhost    Database: moyun-db
-- ------------------------------------------------------
-- Server version	8.4.6

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
-- Table structure for table `ai_agent`
--


-- =============================================================
-- DDL (Data Definition Language): table structures, grouped by module
-- Source: moyun-db-ddl-moyun-db-202608201435.sql
-- For future schema changes, keep the original DDL below and append incremental ALTER TABLE statements to the matching module
-- =============================================================

-- ------------------------------------------------------------
-- Module: AI Agent / Knowledge Base (ai_*)
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `ai_agent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '智能体名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '智能体描述',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '系统提示词',
  `knowledge_library_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '关联的知识库ID列表（JSON数组）',
  `knowledge_base_weights` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '知识库权重配置（JSON格式：{"1": 1.0, "2": 0.8}，权重范围0.1-1.0）',
  `model_config_id` bigint DEFAULT NULL COMMENT '模型配置ID(关联model_config表,NULL则使用默认模型)',
  `model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'qwen-plus' COMMENT '模型名称',
  `temperature` double DEFAULT '0.7' COMMENT '温度参数',
  `max_tokens` int DEFAULT '2000' COMMENT '最大token数',
  `rag_min_score` double DEFAULT NULL COMMENT 'RAG检索相似度阈值(0.5-1.0,推荐0.7-0.75,NULL则使用全局配置)',
  `rag_max_results` int DEFAULT NULL COMMENT 'RAG检索最大结果数量(1-10,推荐3-5,NULL则使用全局配置)',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `welcome_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '开场白',
  `suggested_questions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '预设问题(JSON数组)',
  `show_citations` tinyint(1) DEFAULT '1' COMMENT '是否显示引用来源',
  `max_history_turns` int DEFAULT '10' COMMENT '最大历史轮数',
  `api_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用API',
  `api_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'API Key',
  `workflow_id` bigint DEFAULT NULL COMMENT '关联工作流ID',
  `workflow_trigger_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'manual' COMMENT '工作流触发模式: manual/auto/keyword',
  `workflow_trigger_keywords` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '触发关键词(JSON数组)',
  `publish_enabled` tinyint(1) DEFAULT '0' COMMENT '是否发布为应用',
  `publish_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发布访问Token',
  `publish_settings` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '发布设置(JSON)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `rag_recall_multiplier` double DEFAULT NULL COMMENT '第一阶段召回倍数（1.5-3.0，推荐2.0，NULL时使用全局配置）',
  `rag_enable_hybrid_search` tinyint(1) DEFAULT '1' COMMENT '是否启用混合检索（向量+BM25）',
  `rag_enable_query_expansion` tinyint(1) DEFAULT '1' COMMENT '是否启用查询扩展',
  `rag_bm25_weight` double DEFAULT '0.3' COMMENT 'BM25检索权重（0-1）',
  `rag_vector_weight` double DEFAULT '0.7' COMMENT '向量检索权重（0-1）',
  `enable_self_reflection` tinyint(1) DEFAULT '0' COMMENT '是否启用自我反思',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_model_config_id` (`model_config_id`) USING BTREE,
  KEY `fk_agent_workflow` (`workflow_id`),
  KEY `idx_deleted` (`deleted`),
  CONSTRAINT `fk_agent_model_config` FOREIGN KEY (`model_config_id`) REFERENCES `ai_model_config` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_agent_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能体表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_agent`
--


DROP TABLE IF EXISTS `ai_agent_dictionary_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent_dictionary_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `dictionary_id` bigint NOT NULL COMMENT '词典ID',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_agent_dict` (`agent_id`,`dictionary_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_dictionary_id` (`dictionary_id`) USING BTREE,
  CONSTRAINT `fk_adr_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_adr_dict` FOREIGN KEY (`dictionary_id`) REFERENCES `ai_domain_dictionary` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能体词典关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_agent_dictionary_relation`
--


DROP TABLE IF EXISTS `ai_agent_tool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent_tool` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具标识（英文）',
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '显示名称（中文）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具描述（给LLM理解用）',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'general' COMMENT '工具分类：general/information/utility/action/data',
  `tool_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具类型：builtin/http/database',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'fa-wrench' COMMENT '图标（FontAwesome）',
  `config` json DEFAULT NULL COMMENT '工具配置（API地址、认证信息等）',
  `parameters` json NOT NULL COMMENT '参数定义（JSON Schema格式）',
  `timeout_seconds` int DEFAULT '30' COMMENT '超时时间（秒）',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统内置',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`name`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能体工具定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_agent_tool`
--

DROP TABLE IF EXISTS `ai_agent_tool_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent_tool_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `tool_id` bigint NOT NULL COMMENT '工具ID',
  `custom_config` json DEFAULT NULL COMMENT '针对该智能体的自定义配置',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_agent_tool` (`agent_id`,`tool_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_tool_id` (`tool_id`) USING BTREE,
  CONSTRAINT `fk_atr_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_atr_tool` FOREIGN KEY (`tool_id`) REFERENCES `ai_agent_tool` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能体工具关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_agent_tool_relation`
--


DROP TABLE IF EXISTS `ai_agent_workflow_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent_workflow_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_agent_workflow` (`agent_id`,`workflow_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_workflow_id` (`workflow_id`) USING BTREE,
  CONSTRAINT `fk_awr_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_awr_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='智能体-工作流关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_agent_workflow_relation`
--


DROP TABLE IF EXISTS `ai_analysis_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_analysis_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `report_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报告名称',
  `report_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'auto' COMMENT '报告类型: auto, custom, scheduled',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分析的表名',
  `analysis_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '分析配置(JSON格式)',
  `executive_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行摘要(AI生成)',
  `data_overview` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '数据概览(JSON格式)',
  `analysis_results` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '分析结果(JSON格式)',
  `insights` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '数据洞察(JSON格式)',
  `charts` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '图表配置(JSON格式)',
  `conclusion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '结论与建议(AI生成)',
  `report_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'draft' COMMENT '报告状态: draft, completed, archived',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '导出文件路径',
  `generate_time` int DEFAULT '0' COMMENT '生成耗时(秒)',
  `view_count` int DEFAULT '0' COMMENT '查看次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_datasource_id` (`datasource_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_report_type` (`report_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='分析报告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_analysis_report`
--


DROP TABLE IF EXISTS `ai_chart_recommendation_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chart_recommendation_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名称',
  `data_pattern` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据模式: time_series, distribution, category, correlation',
  `field_types` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '字段类型组合(JSON)',
  `data_characteristics` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '数据特征条件(JSON)',
  `recommended_chart` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推荐图表类型',
  `priority` int DEFAULT '50' COMMENT '优先级(0-100)',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '推荐理由',
  `min_data_points` int DEFAULT '0' COMMENT '最小数据点数',
  `max_data_points` int DEFAULT '999999' COMMENT '最大数据点数',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_data_pattern` (`data_pattern`) USING BTREE,
  KEY `idx_priority` (`priority`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='图表推荐规则表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `ai_chat_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `user_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '用户消息',
  `assistant_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '助手回复',
  `tokens_used` int DEFAULT '0' COMMENT 'Token消耗',
  `retrieval_results` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '检索结果JSON',
  `retrieval_count` int DEFAULT '0' COMMENT '检索命中数',
  `response_time` int DEFAULT '0' COMMENT '响应时间(毫秒)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_session_id` (`session_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  CONSTRAINT `fk_ch_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=129 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='对话历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_history`
--


DROP TABLE IF EXISTS `ai_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '新对话' COMMENT '会话标题（自动生成或用户修改）',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户ID（预留字段，支持多用户）',
  `message_count` int DEFAULT '0' COMMENT '消息数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '对话摘要',
  `summary_updated_at` datetime DEFAULT NULL COMMENT '摘要更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_update_time` (`update_time`) USING BTREE,
  KEY `idx_deleted` (`deleted`),
  CONSTRAINT `fk_c_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='对话会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_conversation`
--


DROP TABLE IF EXISTS `ai_conversation_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_conversation_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色：user/assistant',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `reference_sources` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '参考来源（JSON格式）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_conversation_id` (`conversation_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  CONSTRAINT `conversation_message_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=454 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='对话消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_conversation_message`
--


DROP TABLE IF EXISTS `ai_data_insight`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_data_insight` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `query_id` bigint DEFAULT NULL COMMENT '查询ID',
  `report_id` bigint DEFAULT NULL COMMENT '报告ID',
  `insight_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '洞察类型: anomaly, trend, correlation, pattern',
  `severity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'medium' COMMENT '严重程度: low, medium, high',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '洞察标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '洞察描述',
  `affected_fields` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '影响的字段',
  `statistical_value` decimal(20,4) DEFAULT NULL COMMENT '统计值',
  `confidence` decimal(5,4) DEFAULT NULL COMMENT '置信度(0-1)',
  `actionable` tinyint(1) DEFAULT '0' COMMENT '是否可执行',
  `recommendation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '建议措施',
  `is_acknowledged` tinyint(1) DEFAULT '0' COMMENT '是否已确认',
  `acknowledged_by` bigint DEFAULT NULL COMMENT '确认人ID',
  `acknowledged_time` datetime DEFAULT NULL COMMENT '确认时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_datasource_id` (`datasource_id`) USING BTREE,
  KEY `idx_query_id` (`query_id`) USING BTREE,
  KEY `idx_report_id` (`report_id`) USING BTREE,
  KEY `idx_insight_type` (`insight_type`) USING BTREE,
  KEY `idx_severity` (`severity`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能洞察表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_data_insight`
--


DROP TABLE IF EXISTS `ai_datasource_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_datasource_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源类型: mysql, elasticsearch, mongodb',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口号',
  `database_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '数据库名称',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '密码(加密存储)',
  `connection_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '额外连接参数(JSON格式)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用: 0-禁用, 1-启用',
  `health_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'unknown' COMMENT '健康状态: healthy, unhealthy, unknown',
  `last_check_time` datetime DEFAULT NULL COMMENT '最后检查时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='数据源配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_datasource_config`
--


DROP TABLE IF EXISTS `ai_document_chunk_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_document_chunk_metadata` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分片ID',
  `segment_id` bigint NOT NULL COMMENT '文档分片ID（关联document_segment表）',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `chunk_index` int NOT NULL COMMENT '分片序号',
  `chunk_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分片文本内容',
  `chunk_length` int NOT NULL COMMENT '分片长度',
  `parent_chunk_id` bigint DEFAULT NULL COMMENT '父分片ID（父子分段模式使用）',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '使用的嵌入模型',
  `vector_dimension` int DEFAULT NULL COMMENT '向量维度',
  `original_length` int DEFAULT NULL COMMENT '预处理前长度',
  `preprocessed` tinyint(1) DEFAULT '0' COMMENT '是否经过预处理',
  `hit_count` int DEFAULT '0' COMMENT '被检索命中次数',
  `last_hit_time` timestamp NULL DEFAULT NULL COMMENT '最后命中时间',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_segment_id` (`segment_id`) USING BTREE,
  KEY `idx_knowledge_id` (`knowledge_id`) USING BTREE,
  KEY `idx_parent_chunk` (`parent_chunk_id`) USING BTREE,
  KEY `idx_hit_count` (`hit_count`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='文档分片元数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_document_chunk_metadata`
--


DROP TABLE IF EXISTS `ai_document_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_document_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '关联的知识库ID',
  `image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片文件路径',
  `page_number` int DEFAULT NULL COMMENT '所在页码',
  `image_index` int DEFAULT NULL COMMENT '图片在页面中的索引',
  `embedding_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '向量ID',
  `vector_dimension` int DEFAULT NULL COMMENT '向量维度',
  `width` int DEFAULT NULL COMMENT '图片宽度',
  `height` int DEFAULT NULL COMMENT '图片高度',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '图片内容描述（多模态模型生成）',
  `description_language` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'zh' COMMENT '描述语言(zh/en)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_knowledge_base_id` (`knowledge_base_id`) USING BTREE,
  KEY `idx_embedding_id` (`embedding_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2592 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='文档图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_document_image`
--


DROP TABLE IF EXISTS `ai_document_segment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_document_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '关联的知识库ID',
  `segment_index` int NOT NULL COMMENT '分片索引（第几个分片）',
  `page_number` int DEFAULT NULL COMMENT 'PDF页码',
  `line_start` int DEFAULT NULL COMMENT '起始行号',
  `line_end` int DEFAULT NULL COMMENT '结束行号',
  `char_start` int DEFAULT NULL COMMENT '起始字符位置',
  `char_end` int DEFAULT NULL COMMENT '结束字符位置',
  `chapter_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '章节标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分片内容',
  `content_length` int DEFAULT NULL COMMENT '分片内容长度',
  `embedding_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '向量ID（在Pinecone中的ID）',
  `vector_dimension` int DEFAULT NULL COMMENT '向量维度',
  `vector_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '向量数据（JSON格式）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_knowledge_base_id` (`knowledge_base_id`) USING BTREE,
  KEY `idx_embedding_id` (`embedding_id`) USING BTREE,
  CONSTRAINT `fk_ds_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `ai_knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4595 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='文档分片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_document_segment`
--


DROP TABLE IF EXISTS `ai_domain_dictionary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_domain_dictionary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '核心词',
  `related_terms` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '相关词列表（逗号分隔）',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'general' COMMENT '分类（服务器、架构、模型、通用等）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '词典说明',
  `is_global` tinyint(1) DEFAULT '1' COMMENT '是否全局词典（全局词典默认对所有智能体生效）',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `priority` int DEFAULT '0' COMMENT '优先级（数字越大优先级越高）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_keyword` (`keyword`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_global` (`is_global`) USING BTREE,
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='领域词典表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `ai_knowledge_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `library_id` bigint DEFAULT NULL COMMENT '所属知识库ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件路径',
  `pdf_file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'PDF文件路径（用于预览）',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文件类型',
  `vector_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '向量ID（Pinecone中的ID）',
  `segment_count` int DEFAULT NULL COMMENT '文档分段数量',
  `vector_dimension` int DEFAULT NULL COMMENT '向量维度',
  `status` int DEFAULT '0' COMMENT '处理状态：0-待处理，1-处理中，2-处理成功，3-处理失败',
  `processing_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '处理状态：pending(待配置), configured(已配置), processing(处理中), completed(已完成), failed(失败)',
  `config_completed` tinyint(1) DEFAULT '0' COMMENT '是否完成配置',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `update_time` datetime DEFAULT NULL COMMENT '处理时间',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库分组',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '知识库标签（JSON数组）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库描述',
  `usage_count` int DEFAULT '0' COMMENT '使用次数',
  `hit_count` int DEFAULT '0' COMMENT '命中次数',
  `last_used_time` datetime DEFAULT NULL COMMENT '最后使用时间',
  `parse_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文档解析方式: POI, PDFBox, Text',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文件内容SHA-256哈希值（用于增量更新检测）',
  `last_processed_time` datetime DEFAULT NULL COMMENT '上次处理时间',
  `need_reprocess` tinyint(1) DEFAULT '0' COMMENT '是否需要重新处理',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_usage_count` (`usage_count`) USING BTREE,
  KEY `idx_last_used_time` (`last_used_time`) USING BTREE,
  KEY `idx_library_id` (`library_id`) USING BTREE,
  KEY `idx_kb_content_hash` (`content_hash`) USING BTREE,
  KEY `idx_deleted` (`deleted`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_kb_library` FOREIGN KEY (`library_id`) REFERENCES `ai_knowledge_library` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=134 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_base`
--


DROP TABLE IF EXISTS `ai_knowledge_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `segment_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'general' COMMENT '分段模式：general(通用), parent_child(父子分段)',
  `segment_separator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '\n\n' COMMENT '分段标识符',
  `segment_max_length` int NOT NULL DEFAULT '800' COMMENT '分段最大长度（字符数，800字符确保题库问答对完整，技术文档可用500，小说可用1500）',
  `segment_overlap_length` int NOT NULL DEFAULT '100' COMMENT '分段重叠长度（字符数，100字符保证上下文连贯性）',
  `chunking_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'fixed' COMMENT '分片策略: fixed(固定大小), adaptive(自适应), document_type(按文档类型)',
  `document_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'general' COMMENT '文档类型: general(通用), faq(问答), table(表格), code(代码), technical(技术文档)',
  `faq_chunk_size` int DEFAULT '400' COMMENT 'FAQ分片大小(字符)',
  `table_chunk_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'by_row' COMMENT '表格分片策略: by_row(按行), by_table(整表), by_cell(按单元格)',
  `code_chunk_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'by_function' COMMENT '代码分片策略: by_function(按函数), by_class(按类), by_file(按文件)',
  `technical_chunk_size` int DEFAULT '1200' COMMENT '技术文档分片大小(字符)',
  `enable_smart_boundary` tinyint(1) DEFAULT '1' COMMENT '启用智能边界检测(避免切断句子)',
  `preprocess_replace_spaces` tinyint(1) DEFAULT '1' COMMENT '替换连续空格、换行、制表符',
  `preprocess_remove_urls` tinyint(1) DEFAULT '1' COMMENT '删除URL和邮箱地址',
  `preprocess_remove_extra_newlines` tinyint(1) DEFAULT '1' COMMENT '删除多余换行',
  `index_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'high_quality' COMMENT '索引方式：high_quality(高质量), economy(经济)',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '嵌入模型名称',
  `retrieval_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'vector' COMMENT '检索模式：vector(向量), keyword(关键词), hybrid(混合)',
  `retrieval_top_k` int DEFAULT '3' COMMENT '检索Top K数量',
  `rerank_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用重排序',
  `rerank_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '重排序模型',
  `qa_mode` tinyint(1) DEFAULT '0' COMMENT '是否启用Q&A模式',
  `qa_extraction_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'Q&A提取提示词',
  `preprocess_remove_special_chars` tinyint(1) DEFAULT '0' COMMENT '删除特殊字符',
  `preprocess_remove_table_desc` tinyint(1) DEFAULT '0' COMMENT '删除表格描述',
  `preprocess_remove_header_footer` tinyint(1) DEFAULT '0' COMMENT '删除页眉页脚',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_knowledge_id` (`knowledge_id`) USING BTREE,
  KEY `idx_segment_mode` (`segment_mode`) USING BTREE,
  KEY `idx_index_mode` (`index_mode`) USING BTREE,
  KEY `idx_chunking_strategy` (`chunking_strategy`) USING BTREE,
  KEY `idx_document_type` (`document_type`) USING BTREE,
  CONSTRAINT `fk_kc_knowledge` FOREIGN KEY (`knowledge_id`) REFERENCES `ai_knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=97 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库配置表 - 包含分片策略、文档类型识别、预处理规则等配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_config`
--


DROP TABLE IF EXISTS `ai_knowledge_config_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_config_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `template_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模板描述',
  `template_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板类型：general(通用), technical(技术文档), legal(法律), medical(医疗)',
  `config_json` json NOT NULL COMMENT '配置JSON',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统预设模板',
  `use_count` int DEFAULT '0' COMMENT '使用次数',
  `is_recommended` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否推荐模板',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_template_type` (`template_type`) USING BTREE,
  KEY `idx_use_count` (`use_count`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库配置模板表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `ai_knowledge_library`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_library` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库描述',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '?' COMMENT '知识库图标',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库分类（如：技术文档、产品手册、FAQ等）',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '标签（JSON数组格式）',
  `document_count` int DEFAULT '0' COMMENT '文档数量',
  `total_segments` int DEFAULT '0' COMMENT '总分段数',
  `total_size` bigint DEFAULT '0' COMMENT '总文件大小（字节）',
  `usage_count` int DEFAULT '0' COMMENT '使用次数（被检索次数）',
  `hit_count` int DEFAULT '0' COMMENT '命中次数',
  `last_used_time` datetime DEFAULT NULL COMMENT '最后使用时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态：active(正常), disabled(禁用), archived(归档)',
  `is_public` tinyint(1) DEFAULT '1' COMMENT '是否公开（预留多租户）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_usage_count` (`usage_count`) USING BTREE,
  KEY `idx_deleted` (`deleted`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_library`
--


DROP TABLE IF EXISTS `ai_knowledge_library_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_library_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `library_id` bigint NOT NULL COMMENT '知识库ID',
  `segment_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'general' COMMENT '分段模式：general(通用), qa(问答), code(代码)',
  `segment_separator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '\n\n' COMMENT '分段标识符',
  `segment_max_length` int NOT NULL DEFAULT '800' COMMENT '分段最大长度',
  `segment_overlap_length` int NOT NULL DEFAULT '100' COMMENT '分段重叠长度',
  `preprocess_replace_spaces` tinyint(1) DEFAULT '1' COMMENT '替换连续空格',
  `preprocess_remove_urls` tinyint(1) DEFAULT '1' COMMENT '删除URL',
  `preprocess_remove_extra_newlines` tinyint(1) DEFAULT '1' COMMENT '删除多余换行',
  `index_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'high_quality' COMMENT '索引模式：high_quality, economy',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Embedding模型',
  `retrieval_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'hybrid' COMMENT '检索模式：vector, keyword, hybrid',
  `retrieval_top_k` int DEFAULT '10' COMMENT '检索返回数量',
  `rerank_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用Rerank',
  `rerank_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Rerank模型',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_library_id` (`library_id`) USING BTREE,
  CONSTRAINT `fk_library_config` FOREIGN KEY (`library_id`) REFERENCES `ai_knowledge_library` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_knowledge_library_config`
--


DROP TABLE IF EXISTS `ai_model_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型提供商(openai/ollama/dashscope)',
  `model_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'chat' COMMENT '模型类型(chat/embedding/multimodal/reranker/asr/tts)，asr=语音识别，tts=语音合成，V10.0 语音面试官使用',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型名称',
  `api_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'API密钥',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'API基础URL',
  `temperature` double DEFAULT '0.7' COMMENT '温度参数(0-2)',
  `max_tokens` int DEFAULT '2000' COMMENT '最大Token数',
  `timeout` int DEFAULT '60' COMMENT '超时时间(秒)',
  `streaming_supported` tinyint(1) DEFAULT '1' COMMENT '是否支持流式输出',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否为默认模型',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '备注说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `input_price` decimal(10,6) DEFAULT '0.001000' COMMENT '输入价格（元/1000 tokens）',
  `output_price` decimal(10,6) DEFAULT '0.002000' COMMENT '输出价格（元/1000 tokens）',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_provider` (`provider`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_is_default` (`is_default`) USING BTREE,
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='模型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `ai_query_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_query_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '会话ID',
  `natural_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '自然语言查询',
  `generated_sql` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '生成的SQL语句',
  `query_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '查询类型: select, aggregate, join, analysis',
  `tables_involved` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '涉及的表(逗号分隔)',
  `result_count` int DEFAULT '0' COMMENT '结果行数',
  `execution_time` int DEFAULT '0' COMMENT '执行时间(毫秒)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'success' COMMENT '执行状态: success, failed, timeout',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `analysis_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分析类型: basic, trend, correlation, ranking',
  `chart_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图表类型',
  `has_insight` tinyint(1) DEFAULT '0' COMMENT '是否生成洞察',
  `token_used` int DEFAULT '0' COMMENT 'LLM消耗的Token数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_datasource_id` (`datasource_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_session_id` (`session_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='查询历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_query_history`
--


DROP TABLE IF EXISTS `ai_reference_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_reference_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint DEFAULT NULL COMMENT '知识库ID',
  `file_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件名',
  `page_number` int DEFAULT NULL COMMENT '页码',
  `segment_index` int DEFAULT NULL COMMENT '分片索引',
  `user_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用户查询',
  `rerank_score` double DEFAULT NULL COMMENT '重排分数',
  `vector_score` double DEFAULT NULL COMMENT '向量相似度',
  `feedback_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '反馈类型：accurate(准确), inaccurate(不准确)',
  `agent_id` bigint DEFAULT NULL COMMENT '智能体ID',
  `memory_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_knowledge_base_id` (`knowledge_base_id`) USING BTREE,
  KEY `idx_feedback_type` (`feedback_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='参考来源反馈表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_reference_feedback`
--


DROP TABLE IF EXISTS `ai_sql_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_sql_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `natural_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '自然语言示例',
  `ai_sql_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SQL模板',
  `query_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '查询类型',
  `complexity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'simple' COMMENT '复杂度: simple, medium, complex',
  `table_pattern` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表名模式',
  `usage_count` int DEFAULT '0' COMMENT '使用次数',
  `success_rate` decimal(5,2) DEFAULT NULL COMMENT '成功率(%)',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_query_type` (`query_type`) USING BTREE,
  KEY `idx_complexity` (`complexity`) USING BTREE,
  KEY `idx_usage_count` (`usage_count`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='SQL模板表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `ai_table_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_table_metadata` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '表名',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表注释',
  `table_schema` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '表结构(JSON格式)',
  `column_count` int DEFAULT '0' COMMENT '字段数量',
  `row_count` bigint DEFAULT '0' COMMENT '行数(估算)',
  `data_size` bigint DEFAULT '0' COMMENT '数据大小(字节)',
  `has_primary_key` tinyint(1) DEFAULT '0' COMMENT '是否有主键',
  `has_time_field` tinyint(1) DEFAULT '0' COMMENT '是否有时间字段',
  `time_field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '时间字段名',
  `numeric_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '数值型字段列表(JSON)',
  `category_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '类别型字段列表(JSON)',
  `indexed_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '索引字段列表(JSON)',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最后同步时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_datasource_table` (`datasource_id`,`table_name`) USING BTREE,
  KEY `idx_datasource_id` (`datasource_id`) USING BTREE,
  KEY `idx_last_sync_time` (`last_sync_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='表元数据缓存表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_table_metadata`
--


DROP TABLE IF EXISTS `ai_token_usage_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_token_usage_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint DEFAULT NULL COMMENT '会话ID',
  `message_id` bigint DEFAULT NULL COMMENT '消息ID',
  `agent_id` bigint DEFAULT NULL COMMENT '智能体ID',
  `workflow_id` bigint DEFAULT NULL COMMENT '工作流ID',
  `workflow_execution_id` bigint DEFAULT NULL COMMENT '工作流执行ID',
  `workflow_node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '工作流节点ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户ID',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型名称',
  `model_provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型提供商',
  `input_tokens` int DEFAULT '0' COMMENT '输入token数',
  `output_tokens` int DEFAULT '0' COMMENT '输出token数',
  `total_tokens` int DEFAULT '0' COMMENT '总token数',
  `cost` decimal(10,6) DEFAULT NULL COMMENT '费用（元）',
  `request_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求类型：chat/embedding_query/embedding_document/workflow_llm/workflow_classifier/workflow_extractor/workflow_question',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_conversation_id` (`conversation_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_model_name` (`model_name`) USING BTREE,
  KEY `idx_workflow_id` (`workflow_id`) USING BTREE,
  KEY `idx_workflow_execution_id` (`workflow_execution_id`) USING BTREE,
  KEY `fk_tul_message` (`message_id`),
  CONSTRAINT `fk_tul_agent` FOREIGN KEY (`agent_id`) REFERENCES `ai_agent` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_tul_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_tul_message` FOREIGN KEY (`message_id`) REFERENCES `ai_conversation_message` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=390 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='Token使用记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_token_usage_log`
--


DROP TABLE IF EXISTS `ai_token_usage_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_token_usage_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint DEFAULT NULL COMMENT '智能体ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户ID',
  `stat_date` date DEFAULT NULL COMMENT '统计日期',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型名称',
  `total_requests` int DEFAULT '0' COMMENT '请求次数',
  `total_input_tokens` bigint DEFAULT '0' COMMENT '总输入token',
  `total_output_tokens` bigint DEFAULT '0' COMMENT '总输出token',
  `total_tokens` bigint DEFAULT '0' COMMENT '总token',
  `total_cost` decimal(12,6) DEFAULT NULL COMMENT '总费用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_agent_user_date_model` (`agent_id`,`user_id`,`stat_date`,`model_name`) USING BTREE,
  KEY `idx_stat_date` (`stat_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='Token使用统计汇总表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_token_usage_summary`
--


DROP TABLE IF EXISTS `ai_tool_call_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_tool_call_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `conversation_id` bigint DEFAULT NULL COMMENT '会话ID',
  `message_id` bigint DEFAULT NULL COMMENT '消息ID',
  `agent_id` bigint DEFAULT NULL COMMENT '智能体ID',
  `tool_id` bigint DEFAULT NULL COMMENT '工具ID',
  `tool_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具名称',
  `input_params` json DEFAULT NULL COMMENT '输入参数',
  `output_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '输出结果',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'success' COMMENT '状态：pending/running/success/failed/timeout',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '错误信息',
  `duration_ms` int DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_conversation_id` (`conversation_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_tool_name` (`tool_name`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工具调用日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_tool_call_log`
--


DROP TABLE IF EXISTS `ai_workflow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_workflow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作流名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作流描述',
  `graph_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工作流图定义(JSON)',
  `variables` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '全局变量定义(JSON)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'draft' COMMENT '状态: draft-草稿, published-已发布, disabled-已禁用',
  `version` int DEFAULT '1' COMMENT '版本号',
  `enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='工作流定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_workflow`
--


DROP TABLE IF EXISTS `ai_workflow_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_workflow_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'running' COMMENT '执行状态: running-执行中, completed-已完成, failed-失败, cancelled-已取消',
  `input_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '输入参数(JSON)',
  `output_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '输出结果(JSON)',
  `execution_log` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '执行日志(JSON数组)',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `current_node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前执行到的节点ID',
  `duration_ms` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_workflow_id` (`workflow_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  CONSTRAINT `fk_we_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='工作流执行记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_workflow_execution`
--


DROP TABLE IF EXISTS `ai_workflow_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_workflow_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `version` int NOT NULL COMMENT '版本号',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '版本描述',
  `graph_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工作流图数据快照(JSON)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_workflow_id` (`workflow_id`) USING BTREE,
  KEY `idx_version` (`version`) USING BTREE,
  CONSTRAINT `fk_wv_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `ai_workflow` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='工作流版本表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_workflow_version`
--


-- ------------------------------------------------------------
-- Module: Code Generator (gen_*)
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table`
--


DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table_column`
--


-- ------------------------------------------------------------
-- Module: Portal Community (portal_*)
-- ------------------------------------------------------------

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


DROP TABLE IF EXISTS `portal_ad_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_ad_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '广告位ID',
  `slot_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '广告位标识，如 article_detail_bottom',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '广告标题',
  `image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '广告图片URL',
  `link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '点击跳转链接',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '广告文案',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0=启用 1=停用',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_slot_key` (`slot_key`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户自研广告位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_ad_slot`
--


DROP TABLE IF EXISTS `portal_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文章标题',
  `slug` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文章URL别名，用于SEO语义化路径',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '文章内容（HTML格式）',
  `excerpt` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文章摘要',
  `cover` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '封面图片URL或Base64',
  `author_id` bigint NOT NULL COMMENT '作者ID（门户用户ID）',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `root_category_id` bigint DEFAULT NULL COMMENT '顶级分类ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'draft' COMMENT '状态：draft=草稿 / pending=待审核 / published=已发布 / rejected=已拒绝 / archived=已归档',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
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
  `link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '外部链接',
  `editor_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
  `session_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '编辑会话标识（一次编辑会话唯一，用于草稿/发布幂等去重）',
  `content_markdown` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'Markdown 原始内容',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `category_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类路径，包含所有祖先ID，例如：1,3,5',
  `is_paid` tinyint NOT NULL DEFAULT '0' COMMENT '是否付费阅读 0=免费 1=付费',
  `paid_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '付费内容（购买后可见）',
  `preview_length` int NOT NULL DEFAULT '0' COMMENT '试读字数（未购买可预览的字数）',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '付费价格，0=免费',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`),
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
  KEY `idx_auditor_id` (`auditor_id`),
  KEY `idx_status_published_at` (`status`,`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户文章表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_article`
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
  PRIMARY KEY (`id`),
  KEY `idx_article_version` (`article_id`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章版本快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_article_version`
--


DROP TABLE IF EXISTS `portal_article_view`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_article_view` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（NULL表示游客）',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'IP地址',
  `view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '浏览器User-Agent',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_ip` (`ip`),
  KEY `idx_view_time` (`view_time`),
  KEY `idx_article_user` (`article_id`,`user_id`),
  KEY `idx_article_ip` (`article_id`,`ip`),
  KEY `idx_article_viewtime` (`article_id`,`view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章浏览记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_article_view`
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


DROP TABLE IF EXISTS `portal_book_chapter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_book_chapter` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `book_id` bigint NOT NULL COMMENT '所属书籍ID',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '章节标题',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '章节正文（HTML格式，上限4GB）',
  `content_markdown` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'Markdown原始内容（上限64KB，单章足够）',
  `editor_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
  `word_count` int DEFAULT '0' COMMENT '字数统计',
  `chapter_no` int NOT NULL DEFAULT '0' COMMENT '章节序号（用于排序，从1开始）',
  `volume_id` bigint DEFAULT NULL COMMENT '所属分卷ID（可选，支持分卷管理）',
  `is_free` tinyint(1) DEFAULT '1' COMMENT '是否免费：1=免费，0=VIP章节',
  `price` decimal(10,2) DEFAULT '0.00' COMMENT '章节单价（元，VIP章节购买）',
  `is_published` tinyint(1) DEFAULT '0' COMMENT '是否已发布：0=草稿，1=已发布',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间（支持定时发布）',
  `view_count` bigint DEFAULT '0' COMMENT '章节浏览量',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_chapter_no` (`book_id`,`chapter_no`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_is_published` (`is_published`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍章节表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_chapter`
--


DROP TABLE IF EXISTS `portal_book_chapter_view`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_book_chapter_view` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `book_id` bigint NOT NULL COMMENT '书籍ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（未登录为NULL）',
  `client_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端IP',
  `read_duration_ms` int DEFAULT '0' COMMENT '阅读时长（毫秒）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='章节浏览记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_chapter_view`
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


DROP TABLE IF EXISTS `portal_book_recommend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_book_recommend` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `book_id` bigint NOT NULL COMMENT '书籍ID',
  `position` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推荐位置：home_banner=首页轮播 / home_hot=首页热门 / category_top=分类顶推 / limit_free=限免专区 / discover_banner=发现页轮播',
  `sort` int DEFAULT '0' COMMENT '排序（越小越靠前）',
  `start_time` datetime DEFAULT NULL COMMENT '推荐开始时间（NULL 表示立即生效）',
  `end_time` datetime DEFAULT NULL COMMENT '推荐结束时间（NULL 表示长期有效）',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否生效：1=生效，0=下架',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注（运营说明）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_position` (`book_id`,`position`),
  KEY `idx_position` (`position`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_time_window` (`start_time`,`end_time`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍推荐位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_book_recommend`
--


DROP TABLE IF EXISTS `portal_bookmark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_bookmark`
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
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_book` (`user_id`,`book_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户书架（收藏书籍）表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_bookshelf`
--


DROP TABLE IF EXISTS `portal_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `slug` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类别名',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类描述',
  `icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标URL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `show_in_nav` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在头部栏目展示（0否/1是）',
  `nav_route_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'category' COMMENT '路由类型（home/category/static/external）',
  `nav_route_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '静态/外链路由路径（仅 static/external 类型使用）',
  `nav_badge` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '导航徽章（NEW/HOT，仅 Mega Menu 展示）',
  `category_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'article' COMMENT '栏目内容类型（article=文章分类可发布文章 directory=目录容器仅组织子栏目不发布文章 special=静态页面不发布文章）',
  `requires_auth` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要登录（0否/1是）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_slug` (`slug`),
  KEY `idx_show_in_nav` (`show_in_nav`),
  KEY `idx_category_type` (`category_type`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

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
  `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft 草稿/pending 待审核/published 已发布/archived 归档/rejected 审核驳回',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
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
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_auditor_id` (`auditor_id`),
  KEY `idx_status_created_time` (`status`,`created_time`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_column`
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


DROP TABLE IF EXISTS `portal_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `author_id` bigint NOT NULL COMMENT '评论者ID（门户用户ID）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
  `root_id` bigint DEFAULT '0' COMMENT '根评论ID（一级评论ID）',
  `reply_to` bigint DEFAULT NULL COMMENT '回复的用户ID',
  `reply_to_content` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '被回复的内容摘要',
  `like_count` bigint DEFAULT '0' COMMENT '点赞数',
  `status` char(1) DEFAULT '1' COMMENT '状态：0=待审核 1=已发布 2=审核驳回',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID，CMS审核时写入）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因（独立字段）',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_article_root` (`article_id`,`root_id`),
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_auditor_id` (`auditor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_comment`
--


DROP TABLE IF EXISTS `portal_comment_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_comment` (`user_id`,`comment_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户评论点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_comment_like`
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


DROP TABLE IF EXISTS `portal_creator_certification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_creator_certification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '申请用户ID',
  `real_name` varchar(64) NOT NULL COMMENT '真实姓名',
  `cert_type` varchar(32) NOT NULL COMMENT '认证类型 identity/creator/expert',
  `cert_no` varchar(64) DEFAULT NULL COMMENT '证件号',
  `cert_image` varchar(500) DEFAULT NULL COMMENT '证件照URL（兼容字段：旧单图或新流程中的「人像面」URL 别名）',
  `cert_image_front` varchar(500) DEFAULT NULL COMMENT '身份证正面（人像面）URL',
  `cert_image_back` varchar(500) DEFAULT NULL COMMENT '身份证背面（国徽面）URL',
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


DROP TABLE IF EXISTS `portal_entity_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_entity_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag_id` bigint unsigned NOT NULL COMMENT '标签ID（引用 portal_tag.id）',
  `entity_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实体类型（article/interview_question/interview_experience/interview_resume_template/book 等）',
  `entity_id` bigint unsigned NOT NULL COMMENT '实体ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_entity` (`tag_id`,`entity_type`,`entity_id`),
  KEY `idx_entity` (`entity_type`,`entity_id`),
  KEY `idx_entity_create` (`entity_type`,`create_time`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用实体标签关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_entity_tag`
--


DROP TABLE IF EXISTS `portal_feed_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_feed_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '事件发布者',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'publish_article/publish_experience/new_column/checkin等',
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'article/experience/column/book等',
  `target_id` bigint NOT NULL COMMENT '目标对象ID',
  `title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标标题',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '动态摘要',
  `cover` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '封面图',
  `created_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`created_time`),
  KEY `idx_type_time` (`event_type`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态事件流';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_feed_event`
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态收件箱';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_feed_inbox`
--


DROP TABLE IF EXISTS `portal_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `feedback_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈类型：suggestion/bug/experience/other',
  `subject` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈主题',
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈详细描述',
  `contact` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系方式（可选）',
  `user_id` bigint DEFAULT NULL COMMENT '反馈人用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈人用户名（冗余）',
  `ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈人IP',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
  `handler` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理人',
  `handle_result` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理结果说明',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_type` (`feedback_type`),
  KEY `idx_status` (`status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户意见反馈表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_feedback`
--


DROP TABLE IF EXISTS `portal_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注ID',
  `follower_id` bigint NOT NULL COMMENT '关注者ID（门户用户ID）',
  `following_id` bigint NOT NULL COMMENT '被关注者ID（门户用户ID）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_following` (`follower_id`,`following_id`),
  KEY `idx_follower_id` (`follower_id`),
  KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户关注表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_follow`
--


DROP TABLE IF EXISTS `portal_friend_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_friend_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '链接ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '链接名称',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '链接地址',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '链接描述',
  `logo` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Logo URL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0正常 1停用',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0=存在 2=删除，与全局逻辑删除配置一致）',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户友情链接表';
/*!40101 SET character_set_client = @saved_cs_client */;


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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长事件流水表';
/*!40101 SET character_set_client = @saved_cs_client */;




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


DROP TABLE IF EXISTS `portal_help_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_help_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '答案内容（支持纯文本）',
  `view_count` int DEFAULT '0' COMMENT '查看次数',
  `like_count` int DEFAULT '0' COMMENT '点赞次数',
  `sort` int DEFAULT '0' COMMENT '排序（升序）',
  `is_featured` tinyint DEFAULT '0' COMMENT '是否精选：0=否 1=是',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'published' COMMENT '状态：published/draft',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_featured` (`is_featured`),
  KEY `idx_sort` (`sort`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帮助中心文章表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_help_article`
--


DROP TABLE IF EXISTS `portal_help_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_help_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标（lucide 图标名）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类描述',
  `sort` int DEFAULT '0' COMMENT '排序（升序）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态：active/inactive',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帮助中心分类表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `portal_import_template_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_import_template_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_key` varchar(64) NOT NULL COMMENT '业务标识（interview_question/interview_experience/article/tag/note）',
  `field_name` varchar(64) NOT NULL COMMENT '实体字段名（Java 属性名）',
  `column_name` varchar(64) NOT NULL COMMENT 'Excel 列名（中文表头）',
  `description` varchar(255) DEFAULT NULL COMMENT '字段说明',
  `example_value` varchar(255) DEFAULT NULL COMMENT '示例值',
  `required` tinyint DEFAULT '0' COMMENT '是否必填：1=必填 0=可选',
  `field_type` varchar(20) DEFAULT 'string' COMMENT '字段类型：string/number/date/dict',
  `dict_type` varchar(64) DEFAULT NULL COMMENT '字典 type（field_type=dict 时生效）',
  `combo_values` varchar(500) DEFAULT NULL COMMENT '下拉可选值（逗号分隔）',
  `column_width` int DEFAULT '20' COMMENT 'Excel 列宽',
  `sort` int DEFAULT '0' COMMENT '列排序号',
  `status` char(1) DEFAULT '0' COMMENT '状态：0=启用 1=停用',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_business_key_status_sort` (`business_key`,`status`,`sort`),
  KEY `idx_business_key_field` (`business_key`,`field_name`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入模板字段配置（动态模板）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_import_template_config`
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
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID，CMS审核或定时扫描命中时写入）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
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
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_auditor_id` (`auditor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_comment`
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
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID，CMS审核时写入）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
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
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_experience_auditor` (`auditor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_interview_experience`
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


DROP TABLE IF EXISTS `portal_interview_position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_interview_position` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码（如 java_backend）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称（如 Java后端工程师）',
  `industry` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属行业（如 互联网/金融/制造）',
  `level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '岗位级别（junior/mid/senior）',
  `required_skills` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '必备技能 JSON 数组（如 ["Spring","MySQL","Redis"]，与 portal_tag.name 对齐）',
  `hot_companies` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '热门公司 JSON 数组（如 ["阿里","腾讯","字节"]）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '岗位描述',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态 active/inactive',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status_sort` (`status`,`sort`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试岗位字典表';
/*!40101 SET character_set_client = @saved_cs_client */;


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
  `solution` text COMMENT '参考答案（代码题参考代码片段）',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
  `question_type` varchar(50) DEFAULT NULL COMMENT '题目类型:bagwen八股/algorithm算法/system_design系统设计/project项目/hr',
  `examine_points` text COMMENT '考察点列表JSON数组',
  `answer_outline` text COMMENT '答题大纲Markdown',
  `scoring_criteria` text COMMENT '评分标准JSON数组',
  `reference_answer` text COMMENT '官方参考答案Markdown（八股/设计/项目/HR类完整答案）',
  `prerequisite_ids` varchar(500) DEFAULT NULL COMMENT '前置题目ID，逗号分隔',
  `practice_mode` varchar(20) DEFAULT 'reading' COMMENT '练习模式：reading=展示阅读/choice=选择题/coding=编程题',
  `options` json DEFAULT NULL COMMENT '选择题选项JSON数组：[{label,text,is_correct}]',
  `correct_answer` varchar(50) DEFAULT NULL COMMENT '正确答案（选择题：选项label如B；编程题：null靠测试用例判定）',
  `analysis` text COMMENT '题目解析（做题后展示，区别于 reference_answer 参考答案）',
  `knowledge_tags` varchar(500) DEFAULT NULL COMMENT '知识点标签，逗号分隔（用于错题本/学习路径聚类）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_question_type` (`question_type`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_practice_mode` (`practice_mode`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题目表';
/*!40101 SET character_set_client = @saved_cs_client */;


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


DROP TABLE IF EXISTS `portal_interview_question_test_case`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_interview_question_test_case` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `input` text COMMENT '标准输入（运行时通过stdin传入）',
  `expected_output` text NOT NULL COMMENT '期望输出',
  `is_sample` tinyint(1) DEFAULT '0' COMMENT '是否样例:1=样例（前端展示）/0=隐藏',
  `order_num` int DEFAULT '0' COMMENT '用例排序',
  `explanation` varchar(1000) DEFAULT NULL COMMENT '用例说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题目测试用例表（OJ判题）';
/*!40101 SET character_set_client = @saved_cs_client */;


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
  `preview_images` text COMMENT '模板预览图 JSON 数组（多图，V10.2 新增）',
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
  `passed_case_count` int DEFAULT NULL COMMENT '通过用例数（v6.3 OJ判题）',
  `total_case_count` int DEFAULT NULL COMMENT '总用例数（v6.3 OJ判题）',
  `failed_case_id` bigint DEFAULT NULL COMMENT '首个失败用例ID（v6.3 OJ判题）',
  `failed_case_input` text COMMENT '首个失败用例输入（v6.3 OJ判题，仅样例可见）',
  `failed_case_expected` text COMMENT '首个失败用例期望输出（v6.3 OJ判题）',
  `failed_case_actual` text COMMENT '首个失败用例实际输出（v6.3 OJ判题）',
  `error_message` text COMMENT '编译/运行错误信息（v6.3 OJ判题）',
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


DROP TABLE IF EXISTS `portal_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户点赞表（文章）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_like`
--


DROP TABLE IF EXISTS `portal_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送者',
  `sender_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '发送者类型 portal/sys',
  `receiver_id` bigint NOT NULL COMMENT '接收者',
  `receiver_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '接收者类型 portal/sys',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `msg_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'text' COMMENT 'text/image/file',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_time` (`session_id`,`create_time`),
  KEY `idx_receiver_type_read` (`receiver_id`,`receiver_type`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_message`
--


DROP TABLE IF EXISTS `portal_message_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_message_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_a` bigint NOT NULL COMMENT '用户A（较小ID）',
  `user_a_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT 'A方用户类型 portal/sys',
  `user_b` bigint NOT NULL COMMENT '用户B（较大ID）',
  `user_b_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT 'B方用户类型 portal/sys',
  `last_message_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最后消息内容预览',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_message_session`
--


DROP TABLE IF EXISTS `portal_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：vip/recharge/product',
  `product_id` bigint DEFAULT NULL COMMENT '商品ID',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '状态：pending/paid/cancelled/refunded',
  `pay_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '支付方式：wechat/alipay',
  `trade_no` varchar(64) DEFAULT NULL COMMENT '第三方交易号（支付宝/微信返回的交易号）',
  `pay_channel` varchar(20) DEFAULT 'points' COMMENT '支付渠道：points-积分/alipay-支付宝/wechat-微信支付',
  `notify_id` varchar(64) DEFAULT NULL COMMENT '支付回调ID（用于回调验签与幂等去重）',
  `notify_time` datetime DEFAULT NULL COMMENT '支付回调时间',
  `refund_no` varchar(64) DEFAULT NULL COMMENT '退款单号',
  `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '退款金额',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `refund_reason` varchar(255) DEFAULT NULL COMMENT '退款原因',
  `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_trade_no` (`trade_no`),
  KEY `idx_pay_channel_status` (`pay_channel`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_order`
--


DROP TABLE IF EXISTS `portal_reading_preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_reading_preference` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `font_size` int DEFAULT '18' COMMENT '正文字号（px，12-32）',
  `line_height` decimal(3,1) DEFAULT '1.8' COMMENT '行距（倍，1.2-3.0）',
  `theme` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'default' COMMENT '阅读主题：default=跟随 / light=亮色 / dark=暗色 / sepia=护眼黄',
  `font_family` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'system' COMMENT '字体：system=系统默认 / serif=衬线 / song=宋体 / hei=黑体',
  `letter_spacing` decimal(3,1) DEFAULT '0.0' COMMENT '字间距（px，-1.0-5.0）',
  `paragraph_spacing` decimal(4,1) DEFAULT '1.2' COMMENT '段间距（em，0.5-5.0）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户阅读偏好表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_reading_preference`
--


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

--
-- Dumping data for table `portal_reading_progress`
--


DROP TABLE IF EXISTS `portal_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `report_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '举报类型：spam/inappropriate/infringement/fraud/other',
  `target_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报目标URL',
  `target_type` varchar(32) DEFAULT NULL COMMENT '举报目标类型：article=文章/comment=评论/user=用户/topic=话题/topic_post=话题观点/topic_comment=话题评论/column=专栏（为空表示通用举报，仅 target_url）',
  `target_id` bigint DEFAULT NULL COMMENT '举报目标ID（评论/文章/用户ID，配合 target_type 使用）',
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题描述',
  `contact` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系方式（可选）',
  `images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图片证据（JSON数组，最多3张）',
  `user_id` bigint DEFAULT NULL COMMENT '举报人用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报人用户名（冗余）',
  `ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报人IP',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
  `handler` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理人',
  `handle_result` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理结果说明',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_report_type` (`report_type`),
  KEY `idx_status` (`status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户举报记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_report`
--


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

--
-- Dumping data for table `portal_shop_exchange`
--


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


DROP TABLE IF EXISTS `portal_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
  `slug` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标签别名',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属模块（article/interview_question/interview_experience/interview_resume_template 等，null 表示通用）',
  `reference_count` bigint unsigned DEFAULT '0' COMMENT '被引用次数（冗余计数列，绑定/解绑时同步维护）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_slug` (`slug`),
  KEY `idx_module` (`module`),
  KEY `idx_reference_count` (`reference_count` DESC),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户标签表';
/*!40101 SET character_set_client = @saved_cs_client */;


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
  `trade_no` varchar(64) DEFAULT NULL COMMENT '第三方交易号（支付宝/微信返回的交易号）',
  `pay_channel` varchar(20) DEFAULT 'points' COMMENT '支付渠道：points-积分/alipay-支付宝/wechat-微信支付',
  `notify_id` varchar(64) DEFAULT NULL COMMENT '支付回调ID（用于回调验签与幂等去重）',
  `notify_time` datetime DEFAULT NULL COMMENT '支付回调时间',
  `refund_no` varchar(64) DEFAULT NULL COMMENT '退款单号',
  `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '退款金额',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `refund_reason` varchar(255) DEFAULT NULL COMMENT '退款原因',
  `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_author` (`author_id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_trade_no` (`trade_no`),
  KEY `idx_pay_channel_status` (`pay_channel`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打赏订单（复用为付费阅读购买记录，target_type=article_paid）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_tip_order`
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
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending 待审核/active 活跃/archived 归档/deleted 删除/rejected 审核驳回',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
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
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_auditor_id` (`auditor_id`),
  KEY `idx_status_created_time` (`status`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_topic`
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


DROP TABLE IF EXISTS `portal_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_id` bigint DEFAULT NULL COMMENT '关联后台用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
  `password` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '密码',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '个人简介',
  `position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '职位',
  `wechat` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '微信号',
  `gender` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '性别：male-男，female-女，other-其他',
  `birthday` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生日：YYYY-MM-DD格式',
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所在城市：如北京市',
  `website` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '个人网站URL',
  `github` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'GitHub用户名或完整URL',
  `company` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '公司名称',
  `school` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '学校名称',
  `language` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '语言偏好：zh-CN，en-US等',
  `timezone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '时区：如Asia/Shanghai',
  `notify_like` tinyint(1) DEFAULT '1' COMMENT '是否接收点赞通知',
  `notify_comment` tinyint(1) DEFAULT '1' COMMENT '是否接收评论通知',
  `notify_follow` tinyint(1) DEFAULT '1' COMMENT '是否接收关注通知',
  `notify_system` tinyint(1) DEFAULT '1' COMMENT '是否接收系统通知',
  `privacy_follow` tinyint(1) DEFAULT '1' COMMENT '是否允许被关注',
  `privacy_bookmark` tinyint(1) DEFAULT '1' COMMENT '是否公开收藏夹',
  `privacy_email` tinyint(1) DEFAULT '0' COMMENT '是否公开邮箱',
  `privacy_phone` tinyint(1) DEFAULT '0' COMMENT '是否公开手机号',
  `privacy_profile` tinyint(1) DEFAULT '1' COMMENT '是否公开主页（是否在名家录/作者列表展示）：1=公开，0=不公开',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'user' COMMENT '角色：user/admin',
  `is_certified_creator` tinyint NOT NULL DEFAULT '0' COMMENT '是否认证创作者：0 否/1 是',
  `vip_expire_at` datetime DEFAULT NULL COMMENT 'VIP过期时间',
  `is_phone_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证手机号',
  `is_wechat_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证微信',
  `two_factor_enabled` tinyint(1) DEFAULT '0' COMMENT '是否开启两步验证',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_user`
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
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户成长值总表';
/*!40101 SET character_set_client = @saved_cs_client */;





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
  `weak_tags` text COMMENT '薄弱知识点 JSON 数组（如 [{"tagId":1,"tagName":"Spring","failRate":0.6}]）',
  `weak_tags_updated_time` datetime DEFAULT NULL COMMENT '薄弱点最后计算时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户用户统计聚合表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_user_stats`
--


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


DROP TABLE IF EXISTS `portal_vip_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_vip_package` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `duration` int NOT NULL COMMENT '有效期（天）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '套餐描述',
  `features` json DEFAULT NULL COMMENT '功能列表（JSON数组）',
  `popular` tinyint(1) DEFAULT '0' COMMENT '是否热门',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态：active/inactive',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户VIP套餐表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_vip_package`
--


DROP TABLE IF EXISTS `portal_voice_interview`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_voice_interview` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '面试用户ID',
  `position` varchar(64) DEFAULT NULL COMMENT '面试岗位',
  `scene` varchar(64) DEFAULT NULL COMMENT '面试场景',
  `resume_id` bigint DEFAULT NULL COMMENT '简历ID（有简历时启用项目深挖题源）',
  `status` varchar(16) NOT NULL DEFAULT 'in_progress' COMMENT '状态 in_progress/finished',
  `style` varchar(20) DEFAULT 'professional' COMMENT '面试官风格（字典 voice_interview_style）',
  `difficulty` varchar(20) DEFAULT 'medium' COMMENT '难度 easy/medium/hard',
  `total_qa` int NOT NULL DEFAULT '0' COMMENT '主问题目总数（不含追问）',
  `current_idx` int NOT NULL DEFAULT '0' COMMENT '当前主问题目序号',
  `score` int DEFAULT NULL COMMENT '面试总分（0-100）',
  `summary` text COMMENT 'AI 生成的面试总结',
  `report` text COMMENT '报告 JSON（含维度分/亮点/薄弱点/逐题点评）',
  `config_json` text COMMENT '配置 JSON（hintsEnabled/stuckThreshold/style/difficulty）',
  `is_personalized` tinyint(1) DEFAULT '0' COMMENT '是否基于画像抽题',
  `profile_snapshot` text COMMENT '抽题时的画像快照 JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='语音面试会话主表（V10.1）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_voice_interview`
--


DROP TABLE IF EXISTS `portal_voice_interview_qa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_voice_interview_qa` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `interview_id` bigint NOT NULL COMMENT '面试会话ID',
  `question_id` bigint DEFAULT NULL COMMENT '关联题目ID（portal_interview_question.id）',
  `question_idx` int NOT NULL COMMENT '主问题目序号（从0开始）',
  `parent_qa_id` bigint DEFAULT NULL COMMENT '追问父问答ID（NULL=主问）',
  `question` varchar(1000) NOT NULL COMMENT '面试问题',
  `user_answer` text COMMENT '用户回答（ASR 转写后可编辑）',
  `transcription_edited` tinyint(1) DEFAULT '0' COMMENT '转写是否被用户编辑',
  `ai_feedback` text COMMENT 'AI 反馈',
  `speak_text` text COMMENT 'AI 面试官话术（TTS 播报内容）',
  `score` int DEFAULT NULL COMMENT '本题评分（0-100）',
  `rule_dimensions_json` text COMMENT '规则维度分 JSON（6维对齐雷达图）',
  `hint_used` int DEFAULT '0' COMMENT '已使用提示次数（0~3）',
  `latency_ms` int DEFAULT NULL COMMENT '答题耗时（毫秒）',
  `next_action` varchar(20) DEFAULT NULL COMMENT '下一步动作 followup/hint/next/report',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_interview` (`interview_id`),
  KEY `idx_question_idx` (`interview_id`,`question_idx`),
  KEY `idx_parent` (`parent_qa_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='语音面试问答表（V10.1，含追问链）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_voice_interview_qa`
--


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
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户钱包表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_wallet`
--


DROP TABLE IF EXISTS `portal_wallet_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_wallet_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：recharge/consume/refund/withdraw',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `balance_before` decimal(10,2) NOT NULL COMMENT '交易前余额',
  `balance_after` decimal(10,2) NOT NULL COMMENT '交易后余额',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `trade_no` varchar(64) DEFAULT NULL COMMENT '第三方交易号（充值/提现场景的渠道方流水号）',
  `channel` varchar(20) DEFAULT NULL COMMENT '资金渠道：alipay/wechat/bank',
  `refund_no` varchar(64) DEFAULT NULL COMMENT '退款单号',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_trade_no` (`trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户钱包交易记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portal_wallet_transaction`
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


-- ------------------------------------------------------------
-- Module: Quartz Scheduler (qrtz_*)
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `qrtz_blob_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_blob_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` blob COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Blob类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_blob_triggers`
--


DROP TABLE IF EXISTS `qrtz_calendars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_calendars` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日历信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_calendars`
--


DROP TABLE IF EXISTS `qrtz_cron_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_cron_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cron类型的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_cron_triggers`
--


DROP TABLE IF EXISTS `qrtz_fired_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_fired_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已触发的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_fired_triggers`
--


DROP TABLE IF EXISTS `qrtz_job_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_job_details` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务组名',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_job_details`
--


DROP TABLE IF EXISTS `qrtz_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_locks` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储的悲观锁信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_locks`
--


DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_paused_trigger_grps` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='暂停的触发器表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_paused_trigger_grps`
--


DROP TABLE IF EXISTS `qrtz_scheduler_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_scheduler_state` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调度器状态表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_scheduler_state`
--


DROP TABLE IF EXISTS `qrtz_simple_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simple_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简单触发器的信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_simple_triggers`
--


DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simprop_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `str_prop_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同步机制的行锁表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_simprop_triggers`
--


DROP TABLE IF EXISTS `qrtz_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  KEY `sched_name` (`sched_name`,`job_name`,`job_group`),
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='触发器详细信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_triggers`
--


-- ------------------------------------------------------------
-- Module: System Administration (sys_*)
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `sys_audit_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_audit_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_type` varchar(32) NOT NULL COMMENT '任务类型：article/column/topic/interview_exp/interview_comment/certification/feedback/report',
  `biz_type` varchar(32) DEFAULT NULL COMMENT '业务子类型（如 report 的 spam/infringement）',
  `biz_id` bigint NOT NULL COMMENT '业务记录ID',
  `title` varchar(255) NOT NULL COMMENT '任务标题',
  `description` text COMMENT '任务描述/摘要',
  `submitter_id` bigint DEFAULT NULL COMMENT '提交人ID（门户用户ID）',
  `submitter_name` varchar(64) DEFAULT NULL COMMENT '提交人用户名',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
  `auditor_id` bigint DEFAULT NULL COMMENT '处理人ID（系统用户ID）',
  `auditor_name` varchar(64) DEFAULT NULL COMMENT '处理人用户名',
  `audit_opinion` varchar(1000) DEFAULT NULL COMMENT '审核意见（驳回时必填）',
  `audit_action` varchar(20) DEFAULT NULL COMMENT '审核操作类型：approve/reject',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `audit_time` datetime DEFAULT NULL COMMENT '处理时间',
  `priority` varchar(10) NOT NULL DEFAULT 'medium' COMMENT '优先级：high/medium/low',
  `route_path` varchar(255) DEFAULT NULL COMMENT '查看详情跳转路径',
  `extra_data` text COMMENT '扩展数据 JSON',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_status` (`status`),
  KEY `idx_audit_auditor` (`auditor_id`),
  KEY `idx_audit_submitter` (`submitter_id`),
  KEY `idx_audit_biz` (`biz_type`,`biz_id`),
  KEY `idx_audit_task_type` (`task_type`,`status`),
  KEY `idx_audit_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一审核任务表（v8.1）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_audit_task`
--


DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`config_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--


DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
                                 `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
                                 `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典名称',
                                 `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
                                 `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                 `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`dict_id`),
                                 UNIQUE KEY `uk_dict_type` (`dict_type`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=130 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`dict_code`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=128 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件管理表';
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `sys_job`
--


DROP TABLE IF EXISTS `sys_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '备注信息',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `sys_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '日志信息',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '异常信息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_job_log`
--


DROP TABLE IF EXISTS `sys_job_scan_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job_scan_issue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `job_id` bigint DEFAULT NULL COMMENT '触发扫描的定时任务ID',
  `job_name` varchar(64) DEFAULT NULL COMMENT '定时任务名称',
  `issue_type` varchar(32) NOT NULL COMMENT '问题类型：sensitive_word/pending_overdue/anomaly/other',
  `issue_desc` varchar(500) NOT NULL COMMENT '问题描述',
  `target_type` varchar(32) DEFAULT NULL COMMENT '目标对象类型',
  `target_id` bigint DEFAULT NULL COMMENT '目标对象ID',
  `target_title` varchar(255) DEFAULT NULL COMMENT '目标对象标题/摘要',
  `log_excerpt` text COMMENT '日志摘要',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/handled/ignored',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(64) DEFAULT NULL COMMENT '处理人用户名',
  `handle_result` varchar(500) DEFAULT NULL COMMENT '处理结果说明',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT NULL COMMENT '扫描发现时间',
  PRIMARY KEY (`id`),
  KEY `idx_scan_status` (`status`),
  KEY `idx_scan_job` (`job_id`),
  KEY `idx_scan_target` (`target_type`,`target_id`),
  KEY `idx_scan_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务扫描结果表（v8.1）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_job_scan_issue`
--


DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `user_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'sys' COMMENT '登录来源类型（sys=后台用户 portal=门户用户）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`),
  KEY `idx_sys_logininfor_ut` (`user_type`)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;






DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '路由名称',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`menu_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=5243 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `sys_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：system/comment/like/follow/order/notice/announcement',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '通知内容',
  `data` json DEFAULT NULL COMMENT '通知数据（JSON格式）',
  `scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'user' COMMENT '范围：user=个人通知 / all=全局广播',
  `user_id` bigint DEFAULT NULL COMMENT '接收用户ID（scope=user 时必填，scope=all 时为 NULL）',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '接收用户类型：portal=门户用户 / sys=系统用户（scope=user 时生效）',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '通知/公告分类：1=通知 / 2=公告（兼容 sys_notice 字典 sys_notice_type）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0=正常 / 1=关闭（兼容 sys_notice 字典 sys_notice_status）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_scope` (`scope`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_type_user_id` (`user_type`,`user_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知主体表（合并 portal_notification + sys_notice）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notification`
--


DROP TABLE IF EXISTS `sys_notification_read`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notification_read` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notification_id` bigint NOT NULL COMMENT '通知ID（关联 sys_notification.id）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '已读用户类型：portal=门户用户 / sys=系统用户',
  `read_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notif_user_type` (`notification_id`,`user_id`,`user_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_notification_id` (`notification_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知用户已读关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notification_read`
--


DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--



--
-- Table structure for table `sys_post`
--


DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`post_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `sys_sensitive_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_sensitive_word` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word` varchar(128) NOT NULL COMMENT '敏感词',
  `category` varchar(32) DEFAULT NULL COMMENT '分类：politics=政治/porn=色情/ad=广告/insult=辱骂/other=其他',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态：0=启用 1=禁用',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记：0=存在 2=删除（BaseEntity 逻辑删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感词库';
/*!40101 SET character_set_client = @saved_cs_client */;

DROP TABLE IF EXISTS `sys_sensitive_word_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_sensitive_word_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型：article/column/topic/topic_post/topic_comment/report',
  `biz_id` bigint DEFAULT NULL COMMENT '业务主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '提交人ID（portal_user.id）',
  `content` text COMMENT '被检测的原始内容片段（截断）',
  `hit_words` varchar(500) DEFAULT NULL COMMENT '命中的敏感词列表（逗号分隔）',
  `hit_count` int NOT NULL DEFAULT '0' COMMENT '命中数量',
  `action` varchar(16) NOT NULL DEFAULT 'block' COMMENT '处理动作：block=拦截/pending=转待审核/flag=标记',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感词命中记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_sensitive_word_log`
--


DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;


DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;


