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

LOCK TABLES `ai_agent` WRITE;
/*!40000 ALTER TABLE `ai_agent` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_agent_dictionary_relation`
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

LOCK TABLES `ai_agent_dictionary_relation` WRITE;
/*!40000 ALTER TABLE `ai_agent_dictionary_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent_dictionary_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_agent_tool`
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
select  * from `ai_agent_tool`;

INSERT INTO `moyun-db`.ai_agent_tool (id, name, display_name, description, category, tool_type, icon, config, parameters, timeout_seconds, enabled, is_system, create_time, update_time, deleted) VALUES (1, 'current_time', '当前时间', '获取当前的日期和时间，可指定时区和格式', 'utility', 'builtin', 'fa-clock', null, '{"type": "object", "required": [], "properties": {"format": {"type": "string", "default": "yyyy-MM-dd HH:mm:ss", "description": "时间格式，默认yyyy-MM-dd HH:mm:ss"}, "timezone": {"type": "string", "default": "Asia/Shanghai", "description": "时区，如Asia/Shanghai，默认北京时间"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30', 0);
INSERT INTO `moyun-db`.ai_agent_tool (id, name, display_name, description, category, tool_type, icon, config, parameters, timeout_seconds, enabled, is_system, create_time, update_time, deleted) VALUES (2, 'calculator', '数学计算', '执行数学计算，支持加减乘除、幂运算、开方、三角函数等', 'utility', 'builtin', 'fa-calculator', null, '{"type": "object", "required": ["expression"], "properties": {"expression": {"type": "string", "description": "数学表达式，如(1+2)*3、sqrt(16)、sin(30)"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30', 0);
INSERT INTO `moyun-db`.ai_agent_tool (id, name, display_name, description, category, tool_type, icon, config, parameters, timeout_seconds, enabled, is_system, create_time, update_time, deleted) VALUES (3, 'weather_query', '天气查询', '查询指定城市的实时天气和未来天气预报，包括温度、湿度、风向、天气状况等', 'information', 'http', 'fa-cloud-sun', '{"api_type": "seniverse"}', '{"type": "object", "required": ["city"], "properties": {"city": {"type": "string", "description": "城市名称，如北京、上海、广州"}, "days": {"type": "integer", "default": 1, "description": "预报天数1-7，默认1天"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30', 0);
INSERT INTO `moyun-db`.ai_agent_tool (id, name, display_name, description, category, tool_type, icon, config, parameters, timeout_seconds, enabled, is_system, create_time, update_time, deleted) VALUES (4, 'web_search', '网络搜索', '搜索互联网获取最新信息，适用于查询新闻、事件、知识等实时内容', 'information', 'http', 'fa-search', '{"api_type": "bing"}', '{"type": "object", "required": ["query"], "properties": {"count": {"type": "integer", "default": 5, "description": "返回结果数量，默认5条"}, "query": {"type": "string", "description": "搜索关键词"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30', 0);
INSERT INTO `moyun-db`.ai_agent_tool (id, name, display_name, description, category, tool_type, icon, config, parameters, timeout_seconds, enabled, is_system, create_time, update_time, deleted) VALUES (5, 'url_reader', '网页读取', '读取指定URL的网页内容，提取主要文本信息', 'information', 'http', 'fa-globe', '{"timeout": 10}', '{"type": "object", "required": ["url"], "properties": {"url": {"type": "string", "description": "要读取的网页URL"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30', 0);
INSERT INTO `moyun-db`.ai_agent_tool (id, name, display_name, description, category, tool_type, icon, config, parameters, timeout_seconds, enabled, is_system, create_time, update_time, deleted) VALUES (6, 'translator', '文本翻译', '将文本翻译成指定语言，支持中英日韩等多种语言互译', 'utility', 'http', 'fa-language', '{"api_type": "aliyun"}', '{"type": "object", "required": ["text"], "properties": {"to": {"type": "string", "default": "zh", "description": "目标语言代码，如zh/en/ja"}, "from": {"type": "string", "default": "auto", "description": "源语言代码，如zh/en/ja，可设为auto自动检测"}, "text": {"type": "string", "description": "要翻译的文本"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30', 0);
INSERT INTO `moyun-db`.ai_agent_tool (id, name, display_name, description, category, tool_type, icon, config, parameters, timeout_seconds, enabled, is_system, create_time, update_time, deleted) VALUES (7, 'send_email', '发送邮件', '发送电子邮件到指定邮箱地址', 'action', 'builtin', 'fa-envelope', '{}', '{"type": "object", "required": ["to", "subject", "content"], "properties": {"to": {"type": "string", "description": "收件人邮箱地址"}, "content": {"type": "string", "description": "邮件正文内容"}, "subject": {"type": "string", "description": "邮件主题"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30', 0);
INSERT INTO `moyun-db`.ai_agent_tool (id, name, display_name, description, category, tool_type, icon, config, parameters, timeout_seconds, enabled, is_system, create_time, update_time, deleted) VALUES (8, 'database_query', '数据库查询', '执行SQL查询获取业务数据，仅支持SELECT查询语句', 'data', 'database', 'fa-database', '{"max_rows": 100}', '{"type": "object", "required": ["sql"], "properties": {"sql": {"type": "string", "description": "SQL查询语句，仅支持SELECT"}, "database": {"type": "string", "default": "default", "description": "数据库名称，默认使用配置的业务库"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30', 0);


--
-- Table structure for table `ai_agent_tool_relation`
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

LOCK TABLES `ai_agent_tool_relation` WRITE;
/*!40000 ALTER TABLE `ai_agent_tool_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent_tool_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_agent_workflow_relation`
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

LOCK TABLES `ai_agent_workflow_relation` WRITE;
/*!40000 ALTER TABLE `ai_agent_workflow_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_agent_workflow_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_analysis_report`
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

LOCK TABLES `ai_analysis_report` WRITE;
/*!40000 ALTER TABLE `ai_analysis_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_analysis_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chart_recommendation_rule`
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

select * from ai_chart_recommendation_rule;

INSERT INTO `moyun-db`.ai_chart_recommendation_rule (id, rule_name, data_pattern, field_types, data_characteristics, recommended_chart, priority, reason, min_data_points, max_data_points, enabled, create_time) VALUES (1, '时间序列-折线图', 'time_series', null, null, 'line', 95, '时间趋势最适合用折线图展示', 2, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_chart_recommendation_rule (id, rule_name, data_pattern, field_types, data_characteristics, recommended_chart, priority, reason, min_data_points, max_data_points, enabled, create_time) VALUES (2, '分类占比-饼图', 'category', null, null, 'pie', 85, '少量分类适合饼图', 2, 6, 1, '2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_chart_recommendation_rule (id, rule_name, data_pattern, field_types, data_characteristics, recommended_chart, priority, reason, min_data_points, max_data_points, enabled, create_time) VALUES (3, '分类对比-柱状图', 'category', null, null, 'bar', 90, '多分类对比适合柱状图', 3, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_chart_recommendation_rule (id, rule_name, data_pattern, field_types, data_characteristics, recommended_chart, priority, reason, min_data_points, max_data_points, enabled, create_time) VALUES (4, '数值分布-直方图', 'distribution', null, null, 'histogram', 90, '数值分布最适合用直方图', 10, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_chart_recommendation_rule (id, rule_name, data_pattern, field_types, data_characteristics, recommended_chart, priority, reason, min_data_points, max_data_points, enabled, create_time) VALUES (5, '排名-条形图', 'ranking', null, null, 'bar', 90, '排名对比适合条形图', 3, 50, 1, '2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_chart_recommendation_rule (id, rule_name, data_pattern, field_types, data_characteristics, recommended_chart, priority, reason, min_data_points, max_data_points, enabled, create_time) VALUES (6, '相关性-散点图', 'correlation', null, null, 'scatter', 85, '相关性分析适合散点图', 10, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_chart_recommendation_rule (id, rule_name, data_pattern, field_types, data_characteristics, recommended_chart, priority, reason, min_data_points, max_data_points, enabled, create_time) VALUES (7, '多维对比-雷达图', 'multi_dimension', null, null, 'radar', 75, '多维度对比适合雷达图', 3, 8, 1, '2025-11-29 14:21:54');


--
-- Table structure for table `ai_chat_history`
--

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

LOCK TABLES `ai_chat_history` WRITE;
/*!40000 ALTER TABLE `ai_chat_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_chat_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_conversation`
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

LOCK TABLES `ai_conversation` WRITE;
/*!40000 ALTER TABLE `ai_conversation` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_conversation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_conversation_message`
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

LOCK TABLES `ai_conversation_message` WRITE;
/*!40000 ALTER TABLE `ai_conversation_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_conversation_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_data_insight`
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

LOCK TABLES `ai_data_insight` WRITE;
/*!40000 ALTER TABLE `ai_data_insight` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_data_insight` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_datasource_config`
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

LOCK TABLES `ai_datasource_config` WRITE;
/*!40000 ALTER TABLE `ai_datasource_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_datasource_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_document_chunk_metadata`
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

LOCK TABLES `ai_document_chunk_metadata` WRITE;
/*!40000 ALTER TABLE `ai_document_chunk_metadata` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_document_chunk_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_document_image`
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

LOCK TABLES `ai_document_image` WRITE;
/*!40000 ALTER TABLE `ai_document_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_document_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_document_segment`
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

LOCK TABLES `ai_document_segment` WRITE;
/*!40000 ALTER TABLE `ai_document_segment` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_document_segment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_domain_dictionary`
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

select * from ai_domain_dictionary;

INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (1, '服务器', 'cpu,gpu,npu,内存,存储,硬盘,系统盘,数据盘,鲲鹏,昇腾,算力,主机,机器,配置,规格', '硬件', '服务器相关术语', 0, 1, 10, '2025-11-24 13:47:33', '2025-11-24 13:56:57', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (2, '架构', '系统架构,技术架构,平台架构,设计,模块,组件,层次,结构,框架', '技术', '架构相关术语', 0, 1, 8, '2025-11-24 13:47:33', '2025-11-24 13:57:43', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (3, '模型', '大模型,embedding,向量,llm,ai模型,算法,训练,推理', 'AI', '模型相关术语', 0, 1, 9, '2025-11-24 13:47:33', '2025-11-24 13:57:43', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (4, '知识库', '文档,向量库,rag,检索,知识管理,知识图谱', 'AI', '知识库相关术语', 0, 1, 7, '2025-11-24 13:47:33', '2025-11-24 13:57:43', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (5, '部署', '安装,配置,环境,运维,上线,发布', '运维', '部署相关术语', 0, 1, 6, '2025-11-24 13:47:33', '2025-11-24 13:57:43', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (6, '性能', '速度,效率,吞吐量,延迟,响应时间,优化', '技术', '性能相关术语', 0, 1, 5, '2025-11-24 13:47:33', '2025-11-24 13:57:43', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (7, '安全', '权限,认证,授权,加密,防护,隔离', '安全', '安全相关术语', 0, 1, 8, '2025-11-24 13:47:33', '2025-11-24 13:57:43', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (8, '数据库', 'MySQL,PostgreSQL,MongoDB,Redis,Oracle,SQL,NoSQL,索引,事务,主从,分库分表,读写分离', '技术', '数据库相关术语，包含关系型和非关系型数据库', 0, 1, 9, '2025-11-01 15:55:38', '2025-11-26 15:55:38', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (9, '微服务', 'SpringCloud,Dubbo,gRPC,服务注册,服务发现,负载均衡,熔断,限流,网关,配置中心', '技术', '微服务架构相关术语', 0, 1, 8, '2025-11-04 15:55:38', '2025-11-26 15:55:38', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (10, '容器', 'Docker,Kubernetes,K8s,Pod,容器编排,镜像,Harbor,Helm,Service,Deployment', '运维', '容器化和容器编排相关术语', 0, 1, 8, '2025-11-06 15:55:38', '2025-11-26 15:55:38', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (11, '前端', 'Vue,React,Angular,JavaScript,TypeScript,CSS,HTML,Webpack,Vite,组件,路由,状态管理', '技术', '前端开发相关术语', 0, 1, 7, '2025-11-08 15:55:38', '2025-11-26 15:55:38', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (12, '测试', '单元测试,集成测试,压力测试,自动化测试,测试用例,Bug,缺陷,回归测试,冒烟测试,UAT', '质量', '软件测试相关术语', 0, 1, 6, '2025-11-11 15:55:38', '2025-11-26 15:55:38', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (13, 'DevOps', 'CI/CD,Jenkins,GitLab,流水线,自动化部署,监控,日志,告警,SRE,可观测性', '运维', 'DevOps和持续集成相关术语', 0, 1, 7, '2025-11-14 15:55:38', '2025-11-26 15:55:38', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (14, '网络', 'TCP,UDP,HTTP,HTTPS,DNS,CDN,负载均衡,防火墙,VPN,代理,带宽,延迟', '基础设施', '网络通信相关术语', 0, 1, 6, '2025-11-16 15:55:38', '2025-11-26 15:55:38', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (15, '产品', '需求,PRD,原型,用户故事,MVP,迭代,版本,上线,灰度,AB测试,用户体验,交互设计', '产品', '产品管理相关术语', 1, 1, 5, '2025-11-18 15:55:38', '2025-11-26 15:55:38', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (16, '财务', '预算,成本,利润,营收,ROI,现金流,资产负债,损益表,审计,税务,发票,报销', '财务', '财务管理相关术语', 1, 1, 5, '2025-11-21 15:55:38', '2025-11-26 15:55:38', 0);
INSERT INTO `moyun-db`.ai_domain_dictionary (id, keyword, related_terms, category, description, is_global, enabled, priority, create_time, update_time, deleted) VALUES (17, '人力资源', '招聘,面试,入职,离职,绩效,考核,薪酬,福利,培训,晋升,组织架构,人才盘点', '人力', '人力资源管理相关术语', 1, 1, 5, '2025-11-24 15:55:38', '2025-11-26 15:55:38', 0);


--
-- Table structure for table `ai_knowledge_base`
--

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

LOCK TABLES `ai_knowledge_base` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_base` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_base` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_knowledge_config`
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

LOCK TABLES `ai_knowledge_config` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_knowledge_config_template`
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

select * from ai_knowledge_config_template;

INSERT INTO `moyun-db`.ai_knowledge_config_template (id, template_name, template_desc, template_type, config_json, is_system, use_count, is_recommended, created_at, updated_at) VALUES (1, '标准文档', '适用于一般文档、技术手册等，平衡性能和准确度', 'general', '{"indexMode": "high_quality", "segmentMode": "general", "rerankEnabled": true, "retrievalMode": "vector", "retrievalTopK": 10, "segmentMaxLength": 800, "preprocessRemoveUrls": false, "segmentOverlapLength": 100, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": true}', 1, 0, 1, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `moyun-db`.ai_knowledge_config_template (id, template_name, template_desc, template_type, config_json, is_system, use_count, is_recommended, created_at, updated_at) VALUES (2, '题库/QA精准模式', '适用于题库、问答对等短文本，确保每道题独立检索', 'general', '{"indexMode": "high_quality", "segmentMode": "qa", "rerankEnabled": true, "retrievalMode": "vector", "retrievalTopK": 15, "segmentMaxLength": 400, "preprocessRemoveUrls": true, "segmentOverlapLength": 50, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": true}', 1, 0, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `moyun-db`.ai_knowledge_config_template (id, template_name, template_desc, template_type, config_json, is_system, use_count, is_recommended, created_at, updated_at) VALUES (3, '长文档深度模式', '适用于长篇文章、研究报告等，保留更多上下文', 'general', '{"indexMode": "high_quality", "segmentMode": "general", "rerankEnabled": true, "retrievalMode": "vector", "retrievalTopK": 8, "segmentMaxLength": 1200, "preprocessRemoveUrls": false, "segmentOverlapLength": 200, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": false}', 1, 1, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `moyun-db`.ai_knowledge_config_template (id, template_name, template_desc, template_type, config_json, is_system, use_count, is_recommended, created_at, updated_at) VALUES (4, '代码技术文档', '适用于代码、API文档等技术内容', 'technical', '{"indexMode": "high_quality", "segmentMode": "code", "rerankEnabled": false, "retrievalMode": "vector", "retrievalTopK": 12, "segmentMaxLength": 600, "preprocessRemoveUrls": false, "segmentOverlapLength": 80, "preprocessReplaceSpaces": false, "preprocessRemoveExtraNewlines": false}', 1, 0, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `moyun-db`.ai_knowledge_config_template (id, template_name, template_desc, template_type, config_json, is_system, use_count, is_recommended, created_at, updated_at) VALUES (5, '经济快速模式', '降低资源消耗，适合大批量文档或测试环境', 'general', '{"indexMode": "economy", "segmentMode": "general", "rerankEnabled": false, "retrievalMode": "vector", "retrievalTopK": 5, "segmentMaxLength": 500, "preprocessRemoveUrls": true, "segmentOverlapLength": 50, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": true}', 1, 71, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');


--
-- Table structure for table `ai_knowledge_library`
--

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

LOCK TABLES `ai_knowledge_library` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_library` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_library` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_knowledge_library_config`
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

LOCK TABLES `ai_knowledge_library_config` WRITE;
/*!40000 ALTER TABLE `ai_knowledge_library_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_knowledge_library_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model_config`
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

select * from ai_model_config;

INSERT INTO `moyun-db`.ai_model_config (id, name, provider, model_type, model_name, api_key, base_url, temperature, max_tokens, timeout, streaming_supported, enabled, is_default, description, create_time, update_time, input_price, output_price, deleted) VALUES (10, '通义千问-多模态Embedding', 'dashscope', 'embedding', 'text-embedding-v3', null, null, null, null, 60, 0, 1, 1, '通义千问多模态 Embedding 模型，支持图片和文本的联合向量化，用于图文混合搜索', '2025-11-21 17:12:03', '2026-01-23 15:51:50', 0.000500, 0.000000, 0);
INSERT INTO `moyun-db`.ai_model_config (id, name, provider, model_type, model_name, api_key, base_url, temperature, max_tokens, timeout, streaming_supported, enabled, is_default, description, create_time, update_time, input_price, output_price, deleted) VALUES (11, '通义千问-VL-Plus', 'dashscope', 'chat', 'qwen-vl-plus', null, null, 0.7, 2000, 60, 1, 1, 1, '通义千问视觉理解模型Plus版本，支持图片内容识别和描述，用于文档图片的多模态理解', '2025-11-22 12:16:42', '2026-01-23 15:51:50', 0.001000, 0.002000, 0);
INSERT INTO `moyun-db`.ai_model_config (id, name, provider, model_type, model_name, api_key, base_url, temperature, max_tokens, timeout, streaming_supported, enabled, is_default, description, create_time, update_time, input_price, output_price, deleted) VALUES (15, 'Qwen3-Reranker', 'dashscope', 'reranker', 'qwen3-rerank', null, 'https://dashscope.aliyuncs.com/api/v1', null, null, 60, 0, 1, 1, 'Qwen3 重排序模型，用于提升检索结果的相关性排序，支持中英文等100+语言', '2026-01-22 15:34:36', '2026-01-23 15:51:50', 0.000100, 0.000000, 0);

--
-- Table structure for table `ai_query_history`
--

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

LOCK TABLES `ai_query_history` WRITE;
/*!40000 ALTER TABLE `ai_query_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_query_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_reference_feedback`
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

LOCK TABLES `ai_reference_feedback` WRITE;
/*!40000 ALTER TABLE `ai_reference_feedback` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_reference_feedback` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_sql_template`
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

select * from ai_sql_template;

INSERT INTO `moyun-db`.ai_sql_template (id, template_name, natural_query, ai_sql_template, query_type, complexity, table_pattern, usage_count, success_rate, enabled, create_time, update_time) VALUES (1, '简单查询-TOP N', '查询销售额最高的10个产品', 'SELECT * FROM {table} ORDER BY {metric_field} DESC LIMIT {limit}', 'ranking', 'simple', null, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_sql_template (id, template_name, natural_query, ai_sql_template, query_type, complexity, table_pattern, usage_count, success_rate, enabled, create_time, update_time) VALUES (2, '时间范围查询', '查询上个月的数据', 'SELECT * FROM {table} WHERE {time_field} >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH) AND {time_field} < CURDATE()', 'time_range', 'simple', null, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_sql_template (id, template_name, natural_query, ai_sql_template, query_type, complexity, table_pattern, usage_count, success_rate, enabled, create_time, update_time) VALUES (3, '聚合统计', '统计每个类别的总数', 'SELECT {category_field}, COUNT(*) as count FROM {table} GROUP BY {category_field}', 'aggregate', 'simple', null, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_sql_template (id, template_name, natural_query, ai_sql_template, query_type, complexity, table_pattern, usage_count, success_rate, enabled, create_time, update_time) VALUES (4, '平均值计算', '计算平均销售额', 'SELECT AVG({metric_field}) as avg_value FROM {table}', 'aggregate', 'simple', null, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_sql_template (id, template_name, natural_query, ai_sql_template, query_type, complexity, table_pattern, usage_count, success_rate, enabled, create_time, update_time) VALUES (5, '多条件筛选', '查询价格大于100且库存小于50的商品', 'SELECT * FROM {table} WHERE {field1} > {value1} AND {field2} < {value2}', 'filter', 'medium', null, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');

--
-- Table structure for table `ai_table_metadata`
--

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

LOCK TABLES `ai_table_metadata` WRITE;
/*!40000 ALTER TABLE `ai_table_metadata` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_table_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_token_usage_log`
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

LOCK TABLES `ai_token_usage_log` WRITE;
/*!40000 ALTER TABLE `ai_token_usage_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_token_usage_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_token_usage_summary`
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

LOCK TABLES `ai_token_usage_summary` WRITE;
/*!40000 ALTER TABLE `ai_token_usage_summary` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_token_usage_summary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_tool_call_log`
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

LOCK TABLES `ai_tool_call_log` WRITE;
/*!40000 ALTER TABLE `ai_tool_call_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_tool_call_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_workflow`
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

LOCK TABLES `ai_workflow` WRITE;
/*!40000 ALTER TABLE `ai_workflow` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_workflow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_workflow_execution`
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

LOCK TABLES `ai_workflow_execution` WRITE;
/*!40000 ALTER TABLE `ai_workflow_execution` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_workflow_execution` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_workflow_version`
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

LOCK TABLES `ai_workflow_version` WRITE;
/*!40000 ALTER TABLE `ai_workflow_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_workflow_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gen_table`
--

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

select * from portal_achievement;

INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (1, 'first_article', '初露锋芒', '发布第一篇文章', null, 'article', '{"action":"publish_article","count":1}', 20, 1, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (2, 'article_10', '勤勉作者', '发布10篇文章', null, 'article', '{"action":"publish_article","count":10}', 50, 2, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (3, 'article_50', '高产作者', '发布50篇文章', null, 'article', '{"action":"publish_article","count":50}', 200, 3, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (4, 'article_featured', '精华创作者', '文章被精选', null, 'article', '{"action":"article_featured","count":1}', 100, 4, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (5, 'article_100_likes', '人气作者', '单篇文章获赞100', null, 'article', '{"action":"receive_like","count":100}', 50, 5, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (6, 'first_book', '开卷有益', '完成阅读第一本书', null, 'reading', '{"action":"finish_book","count":1}', 20, 10, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (7, 'book_worm_10', '书虫', '完成阅读10本书', null, 'reading', '{"action":"finish_book","count":10}', 100, 11, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (8, 'book_worm_50', '阅读达人', '完成阅读50本书', null, 'reading', '{"action":"finish_book","count":50}', 300, 12, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (9, 'first_booklist', '书单策划', '创建第一个书单', null, 'reading', '{"action":"create_booklist","count":1}', 20, 13, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (10, 'quote_master', '金句达人', '发布20条金句', null, 'reading', '{"action":"write_quote","count":20}', 50, 14, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (11, 'first_solve', '初试身手', '解答第一道面试题', null, 'interview', '{"action":"solve_question","count":1}', 10, 20, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (12, 'solve_50', '刷题能手', '解答50道面试题', null, 'interview', '{"action":"solve_question","count":50}', 100, 21, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (13, 'solve_200', '面试达人', '解答200道面试题', null, 'interview', '{"action":"solve_question","count":200}', 300, 22, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (14, 'first_note', '笔记新手', '撰写第一篇笔记', null, 'interview', '{"action":"write_note","count":1}', 15, 23, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (15, 'note_adopted', '知识贡献者', '笔记被精选', null, 'interview', '{"action":"note_adopted","count":1}', 50, 24, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (16, 'first_experience', '面经分享者', '发布第一篇面经', null, 'interview', '{"action":"publish_experience","count":1}', 30, 25, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (17, 'experience_10', '面经达人', '发布10篇面经', null, 'interview', '{"action":"publish_experience","count":10}', 100, 26, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (18, 'checkin_7', '坚持一周', '连续签到7天', null, 'all', '{"action":"daily_checkin","count":7}', 10, 30, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (19, 'checkin_30', '坚持一月', '连续签到30天', null, 'all', '{"action":"daily_checkin","count":30}', 50, 31, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (20, 'level_5', '渐入佳境', '达到5级', null, 'all', '{"action":"level","count":5}', 0, 32, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (21, 'level_8', '登峰造极', '达到8级', null, 'all', '{"action":"level","count":8}', 0, 33, '0', '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (22, 'first_tip_received', '初获鼓励', '首次收到打赏', null, 'article', '{"action":"receive_tip","count":1}', 10, 7, '0', '', '2026-07-28 16:31:27', '', '2026-07-28 16:31:27', null);
INSERT INTO `moyun-db`.portal_achievement (id, code, name, description, icon, module, condition_json, growth_reward, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (23, 'generous_tipper', '慷慨鼓励', '累计打赏他人 10 次', null, 'article', '{"action":"tip_others","count":10}', 30, 8, '0', '', '2026-07-28 16:31:27', '', '2026-07-28 16:31:27', null);

--
-- Table structure for table `portal_ad_slot`
--

DROP TABLE IF EXISTS `portal_ad_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_ad_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '广告位ID',
  `slot_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '广告位标识，如 article_detail_bottom',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '广告标题',
  `image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '广告图片URL',
  `link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '点击跳转链接',
  `open_target` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '_blank' COMMENT '链接打开方式：_blank=新窗口（默认），_self=当前页',
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

LOCK TABLES `portal_article` WRITE;
/*!40000 ALTER TABLE `portal_article` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_article` ENABLE KEYS */;
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
  PRIMARY KEY (`id`),
  KEY `idx_article_version` (`article_id`,`version_no`)
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

LOCK TABLES `portal_book_chapter` WRITE;
/*!40000 ALTER TABLE `portal_book_chapter` DISABLE KEYS */;
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

LOCK TABLES `portal_book_recommend` WRITE;
/*!40000 ALTER TABLE `portal_book_recommend` DISABLE KEYS */;
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
select portal_category.* from portal_category;
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (51, '首页', 'home', '精选推荐、双轨轮播', 'fa-home', 1, 0, '0', 1, 'home', '/', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (52, '面试专区', 'interview', 'AI 语音面试、面经复盘、简历优化', 'fa-briefcase', 2, 0, '0', 1, 'static', '/interview', null, 'directory', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (53, '学习中心', 'learn', '题库、刷题、错题本、学习计划', 'fa-graduation-cap', 3, 0, '0', 1, 'static', '/learn', null, 'directory', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (54, '阅读空间', 'reading', '散文天地、技术笔记、读书空间', 'fa-book', 4, 0, '0', 1, 'static', '/reading', null, 'directory', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (55, '创作互动', 'creation', '话题、动态、专栏、征文、发布', 'fa-feather', 5, 0, '0', 1, 'static', '/creation', null, 'directory', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (56, '我的', 'mine', '个人中心、成长时间线、我的内容', 'fa-user', 6, 0, '0', 1, 'static', '/user', null, 'directory', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (57, 'AI 语音面试官', 'interview-voice', 'AI 语音面试官，真实面试场景模拟', 'fa-microphone', 1, 52, '0', 1, 'static', '/interview/voice', 'NEW', 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (58, '面试经验', 'interview-experiences', '大厂面试全流程还原', 'fa-chart-line', 2, 52, '0', 1, 'static', '/interview/experiences', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (59, '简历模板', 'interview-resume-templates', '技术亮点提炼、项目描述技巧', 'fa-file-alt', 3, 52, '0', 1, 'static', '/interview/resume-templates', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (60, '面试题库', 'learn-questions', '算法题、系统设计、行为面试', 'fa-clipboard-list', 1, 53, '0', 1, 'static', '/learn/questions', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (61, '刷题中心', 'learn-practice', '在线编程、选择题练习', 'fa-laptop-code', 2, 53, '0', 1, 'static', '/learn/practice', 'HOT', 'directory', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (62, '错题本', 'learn-wrong', '错题归集与复习', 'fa-times-circle', 3, 53, '0', 1, 'static', '/learn/wrong', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (63, '知识图谱', 'learn-knowledge', '知识体系可视化', 'fa-project-diagram', 4, 53, '0', 1, 'static', '/learn/knowledge', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (64, '刷题排行榜', 'learn-leaderboard', '刷题榜、学习榜', 'fa-trophy', 5, 53, '0', 1, 'static', '/learn/leaderboard', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (65, '刷题日历', 'learn-calendar', '刷题打卡日历', 'fa-calendar-check', 6, 53, '0', 1, 'static', '/learn/calendar', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (66, '学习计划', 'learn-plan', '个人学习计划管理', 'fa-calendar-alt', 7, 53, '0', 1, 'static', '/learn/plan', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (67, '选择题', 'learn-practice-choice', '选择题在线练习', 'fa-check-square', 1, 61, '0', 1, 'static', '/learn/practice/choice', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (68, '编程题', 'learn-practice-coding', '编程题在线练习', 'fa-code', 2, 61, '0', 1, 'static', '/learn/practice/coding', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (69, '散文天地', 'prose', '人文书写与情感表达', 'fa-pen-fancy', 1, 54, '0', 1, 'category', '/category/prose', null, 'directory', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (70, '技术笔记', 'tech-notes', '开发记录、技术解析、AI编程实践', 'fa-code', 2, 54, '0', 1, 'category', '/category/tech-notes', null, 'directory', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (71, '读书空间', 'reading-space', '发现好书、我的书架、金句摘录', 'fa-book-reader', 3, 54, '0', 1, 'static', '/reading/space', null, 'directory', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (72, '金句摘录', 'reading-quotes', '跨分区高光语句精选', 'fa-quote-left', 4, 54, '0', 1, 'static', '/reading/quotes', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (73, '人间烟火', 'life-stories', '饮食、市井、生活琐记', 'fa-utensils', 1, 69, '0', 1, 'category', '/category/life-stories', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (74, '山河行吟', 'travel-nature', '游记、自然书写、生态散文', 'fa-mountain', 2, 69, '0', 1, 'category', '/category/travel-nature', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (75, '心灵独白', 'inner-thoughts', '孤独、成长、疗愈随笔', 'fa-heart', 3, 69, '0', 1, 'category', '/category/inner-thoughts', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (76, '城市笔记', 'city-notes', '北上广深、小镇观察', 'fa-city', 4, 69, '0', 1, 'category', '/category/city-notes', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (77, '四季专栏', 'seasons', '春之思、夏之躁、秋之静、冬之藏', 'fa-leaf', 5, 69, '0', 1, 'category', '/category/seasons', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (78, '声音散文', 'audio-prose', '作者自读、背景音效沉浸体验', 'fa-volume-up', 6, 69, '0', 1, 'category', '/category/audio-prose', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (79, '读者来信', 'reader-letters', '短篇心声刊发与回声计划', 'fa-envelope', 7, 69, '0', 1, 'category', '/category/reader-letters', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (80, '技术栈手册', 'tech-stack', 'Java/SpringBoot、React/Vue、Flutter/UniApp', 'fa-book-open', 1, 70, '0', 1, 'category', '/category/tech-stack', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (81, '架构札记', 'architecture', '微服务、缓存策略、分布式事务', 'fa-project-diagram', 2, 70, '0', 1, 'category', '/category/architecture', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (82, '性能日志', 'performance', 'SQL优化、前端加载、JVM调优', 'fa-tachometer-alt', 3, 70, '0', 1, 'category', '/category/performance', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (83, 'AI编程', 'ai-coding', 'Cursor使用、ChatGPT提示工程、AI排错记录', 'fa-robot', 4, 70, '0', 1, 'category', '/category/ai-coding', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (84, '开源日志', 'open-source', 'PR提交、Issue解决、源码阅读', 'fa-code-branch', 5, 70, '0', 1, 'category', '/category/open-source', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (85, '新手入门', 'beginner', '环境配置、第一行代码实录', 'fa-play-circle', 6, 70, '0', 1, 'category', '/category/beginner', null, 'article', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (86, '发现好书', 'reading-discover', '发现好书、书单推荐', 'fa-list', 1, 71, '0', 1, 'static', '/reading/discover', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (87, '我的书架', 'reading-bookshelf', '个人书架管理', 'fa-bookmark', 2, 71, '0', 1, 'static', '/reading/bookshelf', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (88, '金句摘录', 'reading-space-quotes', '读书空间内的金句摘录', 'fa-quote-left', 3, 71, '0', 1, 'static', '/reading/space/quotes', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (89, '话题广场', 'topics', '话题讨论列表', 'fa-comments', 1, 55, '0', 1, 'static', '/topics', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (90, '动态广场', 'feed', '用户动态流', 'fa-stream', 2, 55, '0', 1, 'static', '/feed', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (91, '专栏广场', 'columns', '专栏列表与订阅', 'fa-columns', 3, 55, '0', 1, 'static', '/columns', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (92, '征文活动', 'contests', '征文活动、技术挑战赛', 'fa-file-upload', 4, 55, '0', 1, 'static', '/contests', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (93, '发布文章', 'publish', '发布新文章（快捷入口）', 'fa-edit', 5, 55, '0', 1, 'static', '/publish', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (94, '成长排行榜', 'ranking', '成长值排行榜', 'fa-trophy', 6, 55, '0', 1, 'static', '/ranking', null, 'special', 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (95, '个人中心', 'user', '个人中心主页', 'fa-user-circle', 1, 56, '0', 1, 'static', '/user', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (96, '成长时间线', 'growth-timeline', '成长记录时间线', 'fa-chart-line', 2, 56, '0', 1, 'static', '/growth/timeline', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (97, '我的文章', 'my-articles', '我发布的文章', 'fa-file-alt', 3, 56, '0', 1, 'static', '/my/articles', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (98, '我的专栏', 'column-my', '我创建的专栏', 'fa-columns', 4, 56, '0', 1, 'static', '/column/my', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (99, '我的成就', 'achievements', '我的成就与徽章', 'fa-award', 5, 56, '0', 1, 'static', '/achievements', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');
INSERT INTO `moyun-db`.portal_category (id, name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, nav_badge, category_type, requires_auth, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (100, '我的话题观点', 'topic-my', '我发起的话题与观点', 'fa-comments', 6, 56, '0', 1, 'static', '/topic/my', null, 'special', 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null, '0');

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态收件箱';
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

select * from portal_friend_link;

INSERT INTO `moyun-db`.portal_friend_link (id, name, url, description, logo, sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, '中国作家网', 'https://www.chinawriter.com.cn', '中国作家协会官方网站', null, 1, '0', 'admin', '2026-07-28 15:44:22', '', '2026-07-28 15:44:22', null, '0');
INSERT INTO `moyun-db`.portal_friend_link (id, name, url, description, logo, sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, '起点中文网', 'https://www.qidian.com', '阅文集团旗下网站', null, 2, '0', 'admin', '2026-07-28 15:44:22', '', '2026-07-28 15:44:22', null, '0');
INSERT INTO `moyun-db`.portal_friend_link (id, name, url, description, logo, sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, '掘金', 'https://juejin.cn', '帮助开发者成长的社区', null, 3, '0', 'admin', '2026-07-28 15:44:22', '', '2026-07-28 15:44:22', null, '0');


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

select * from portal_growth_rule;
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (1, 'article', 'publish_article', 50, 3, '发布文章', '0', 1, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (2, 'article', 'receive_like', 2, 0, '文章被点赞', '0', 2, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (3, 'article', 'receive_bookmark', 3, 0, '文章被收藏', '0', 3, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (4, 'article', 'receive_follow', 5, 0, '被关注', '0', 4, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (5, 'article', 'article_featured', 100, 0, '文章被精选', '0', 5, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (6, 'article', 'receive_comment', 2, 0, '文章被评论', '0', 6, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (7, 'reading', 'finish_book', 20, 1, '完成阅读一本书', '0', 10, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (8, 'reading', 'write_quote', 15, 0, '发布金句', '0', 11, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (9, 'reading', 'create_booklist', 20, 0, '创建书单', '0', 12, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (10, 'reading', 'quote_liked', 5, 0, '金句被点赞', '0', 13, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (11, 'reading', 'booklist_liked', 5, 0, '书单被点赞', '0', 14, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (12, 'reading', 'booklist_bookmarked', 10, 0, '书单被收藏', '0', 15, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (13, 'interview', 'solve_question', 10, 20, '解题', '0', 20, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (14, 'interview', 'write_note', 15, 0, '写笔记', '0', 21, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (15, 'interview', 'note_adopted', 50, 0, '笔记被精选', '0', 22, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (16, 'interview', 'publish_experience', 30, 0, '发布面经', '0', 23, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (17, 'interview', 'experience_liked', 2, 0, '面经被点赞', '0', 24, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (18, 'interview', 'experience_bookmarked', 3, 0, '面经被收藏', '0', 25, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (19, 'all', 'daily_checkin', 1, 1, '每日签到', '0', 30, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (20, 'all', 'daily_login', 1, 1, '每日登录', '0', 31, '', '2026-07-28 15:48:22', '', '2026-07-28 15:48:22', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (21, 'article', 'receive_tip', 3, 0, '文章/专栏被打赏', '0', 7, '', '2026-07-28 16:31:27', '', '2026-07-28 16:31:27', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (22, 'article', 'tip_others', 1, 3, '打赏他人', '0', 8, '', '2026-07-28 16:31:27', '', '2026-07-28 16:31:27', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (23, 'topic', 'create_topic', 10, 0, '发起话题', '0', 0, 'admin', '2026-07-28 16:35:21', '', '2026-07-28 16:35:21', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (24, 'topic', 'post_opinion', 2, 10, '发表观点', '0', 0, 'admin', '2026-07-28 16:35:21', '', '2026-07-28 16:35:21', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (25, 'topic', 'receive_topic_like', 2, 0, '话题被点赞', '0', 0, 'admin', '2026-07-28 16:35:21', '', '2026-07-28 16:35:21', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (26, 'topic', 'receive_post_like', 2, 0, '观点被点赞', '0', 0, 'admin', '2026-07-28 16:35:21', '', '2026-07-28 16:35:21', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (27, 'topic', 'receive_topic_comment', 2, 0, '话题被评论', '0', 0, 'admin', '2026-07-28 16:35:21', '', '2026-07-28 16:35:21', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (28, 'topic', 'receive_post_comment', 2, 0, '观点被评论', '0', 0, 'admin', '2026-07-28 16:35:21', '', '2026-07-28 16:35:21', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (29, 'topic', 'receive_comment_like', 2, 0, '评论被点赞', '0', 0, 'admin', '2026-07-28 16:35:21', '', '2026-07-28 16:35:21', null);
INSERT INTO `moyun-db`.portal_growth_rule (id, module, action, growth_delta, daily_limit, description, status, sort, create_by, create_time, update_by, update_time, remark) VALUES (30, 'topic', 'topic_featured', 50, 0, '话题被精选', '0', 0, 'admin', '2026-07-28 16:35:21', '', '2026-07-28 16:35:21', null);

--
-- Table structure for table `portal_help_article`
--

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

LOCK TABLES `portal_help_article` WRITE;
/*!40000 ALTER TABLE `portal_help_article` DISABLE KEYS */;
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

SELECT portal_help_category.* FROM portal_help_category;

INSERT INTO `moyun-db`.portal_help_category (id, name, icon, description, sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, '发布与编辑', 'BookOpen', '文章发布、编辑、删除等操作指南', 1, 'active', '', '2026-07-28 15:52:20', '', '2026-07-28 15:52:20', null, '0');
INSERT INTO `moyun-db`.portal_help_category (id, name, icon, description, sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, '账号与安全', 'HelpCircle', '登录、注册、密码、安全设置', 2, 'active', '', '2026-07-28 15:52:20', '', '2026-07-28 15:52:20', null, '0');
INSERT INTO `moyun-db`.portal_help_category (id, name, icon, description, sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, '互动功能', 'MessageSquare', '评论、点赞、关注等互动功能', 3, 'active', '', '2026-07-28 15:52:20', '', '2026-07-28 15:52:20', null, '0');
INSERT INTO `moyun-db`.portal_help_category (id, name, icon, description, sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (4, '社区规则', 'Shield', '使用规范、违规处理、隐私政策', 4, 'active', '', '2026-07-28 15:52:20', '', '2026-07-28 15:52:20', null, '0');

--
-- Table structure for table `portal_import_template_config`
--

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
SELECT * FROM portal_import_template_config;
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (1, 'interview_question', 'title', '题目标题', '必填，不超过500字', 'TCP三次握手的目的是什么？', 1, 'string', null, null, 30, 1, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (2, 'interview_question', 'description', '题目描述', '可选，题目详细描述', 'TCP协议建立连接时使用三次握手，请选择其主要目的。', 0, 'string', null, null, 40, 2, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (3, 'interview_question', 'difficulty', '难度', '可选，下拉选择', 'easy', 0, 'dict', null, 'easy,medium,hard', 12, 3, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (4, 'interview_question', 'categoryId', '分类ID', '可选，数字类型，对应面试分类表ID', '1', 0, 'number', null, null, 12, 4, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (5, 'interview_question', 'tags', '标签', '可选，逗号分隔', 'TCP,网络,三次握手', 0, 'string', null, null, 20, 5, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (6, 'interview_question', 'companies', '公司', '可选，逗号分隔', '字节跳动,腾讯,阿里', 0, 'string', null, null, 20, 6, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (7, 'interview_question', 'hint', '提示', '可选，做题时的提示信息', '思考序列号同步的作用', 0, 'string', null, null, 25, 7, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (8, 'interview_question', 'solution', '参考答案', '可选，编程题参考代码片段', 'function twoSum(nums, target) { ... }', 0, 'string', null, null, 40, 8, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (9, 'interview_question', 'status', '状态', '可选，默认published', 'published', 0, 'dict', null, 'draft,published,archived', 14, 9, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (10, 'interview_question', 'sort', '排序', '可选，整数，越小越靠前', '1', 0, 'number', null, null, 10, 10, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (11, 'interview_question', 'questionType', '题目类型', '可选，下拉选择内容分类', 'bagwen', 0, 'dict', null, 'bagwen,algorithm,system_design,project,hr', 16, 11, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (12, 'interview_question', 'examinePoints', '考察点', '可选，JSON数组字符串', '["TCP三次握手","序列号同步"]', 0, 'string', null, null, 30, 12, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (13, 'interview_question', 'answerOutline', '答题大纲', '可选，Markdown格式答题思路', '1. 建立连接
2. 同步序列号
3. 确认应答', 0, 'string', null, null, 35, 13, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (14, 'interview_question', 'scoringCriteria', '评分标准', '可选，JSON数组字符串', '[{"dimension":"准确性","weight":60}]', 0, 'string', null, null, 30, 14, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (15, 'interview_question', 'referenceAnswer', '官方参考答案', '可选，Markdown格式完整答案（八股/设计/项目/HR类）', '三次握手的核心目的是同步双方的初始序列号...', 0, 'string', null, null, 40, 15, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (16, 'interview_question', 'prerequisiteIds', '前置题目ID', '可选，逗号分隔的题目ID', '1,2,3', 0, 'string', null, null, 16, 16, '0', 'admin', '2026-08-20 13:21:05', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (17, 'interview_question', 'practiceMode', '练习模式', '可选，默认reading，下拉选择作答方式', 'choice', 0, 'dict', null, 'reading,choice,coding', 14, 17, '0', 'admin', '2026-08-20 13:21:06', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (18, 'interview_question', 'options', '选择题选项', 'practiceMode=choice时必填，JSON数组格式', '[{"label":"A","text":"建立网络路由","is_correct":false},{"label":"B","text":"同步双方的初始序列号","is_correct":true}]', 0, 'string', null, null, 50, 18, '0', 'admin', '2026-08-20 13:21:06', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (19, 'interview_question', 'correctAnswer', '正确答案', '可选，选择题填选项label（如B），编程题留空', 'B', 0, 'string', null, null, 12, 19, '0', 'admin', '2026-08-20 13:21:06', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (20, 'interview_question', 'analysis', '题目解析', '可选，做题后展示的解析说明', '三次握手的核心目的是让通信双方同步初始序列号（ISN）...', 0, 'string', null, null, 40, 20, '0', 'admin', '2026-08-20 13:21:06', '', null, null);
INSERT INTO `moyun-db`.portal_import_template_config (id, business_key, field_name, column_name, description, example_value, required, field_type, dict_type, combo_values, column_width, sort, status, create_by, create_time, update_by, update_time, remark) VALUES (21, 'interview_question', 'knowledgeTags', '知识点标签', '可选，逗号分隔，用于错题本/学习路径聚类', 'TCP,三次握手,序列号,可靠传输', 0, 'string', null, null, 25, 21, '0', 'admin', '2026-08-20 13:21:06', '', null, null);


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

SELECT portal_interview_category.* FROM portal_interview_category;

INSERT INTO `moyun-db`.portal_interview_category (id, name, slug, description, icon, sort, question_count, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, '算法与数据结构', 'algorithm', '算法题、数据结构相关面试题', 'fa-code', 1, 150, 'active', '', '2026-07-28 15:46:12', '', '2026-07-28 15:46:12', null, '0');
INSERT INTO `moyun-db`.portal_interview_category (id, name, slug, description, icon, sort, question_count, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, '系统设计', 'system-design', '系统架构设计、分布式系统等面试题', 'fa-sitemap', 2, 60, 'active', '', '2026-07-28 15:46:12', '', '2026-07-28 15:46:12', null, '0');
INSERT INTO `moyun-db`.portal_interview_category (id, name, slug, description, icon, sort, question_count, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, '前端开发', 'frontend', 'JavaScript、CSS、Vue、React等前端技术面试题', 'fa-laptop-code', 3, 120, 'active', '', '2026-07-28 15:46:12', '', '2026-07-28 15:46:12', null, '0');
INSERT INTO `moyun-db`.portal_interview_category (id, name, slug, description, icon, sort, question_count, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (4, '后端开发', 'backend', 'Java、Python、Go等后端技术面试题', 'fa-server', 4, 130, 'active', '', '2026-07-28 15:46:12', '', '2026-07-28 15:46:12', null, '0');
INSERT INTO `moyun-db`.portal_interview_category (id, name, slug, description, icon, sort, question_count, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5, '数据库', 'database', 'MySQL、Redis等数据库相关面试题', 'fa-database', 5, 80, 'active', '', '2026-07-28 15:46:12', '', '2026-07-28 15:46:12', null, '0');

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

SELECT portal_interview_position.* FROM portal_interview_position;

INSERT INTO `moyun-db`.portal_interview_position (id, code, name, industry, level, required_skills, hot_companies, description, sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, 'java_backend', 'Java后端工程师', '互联网', 'mid', '["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]', '["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]', 'Java 后端工程师岗位，重点考察 Java 基础、Spring 全家桶、MySQL/Redis、分布式与微服务、JVM 与并发编程', 1, 'active', '', '2026-07-28 16:39:34', '', '2026-07-28 16:39:34', null, '0');
INSERT INTO `moyun-db`.portal_interview_position (id, code, name, industry, level, required_skills, hot_companies, description, sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, 'frontend', '前端工程师', '互联网', 'mid', '["JavaScript","TypeScript","Vue","React","HTML","CSS","Node.js","Webpack","Vite","性能优化","浏览器原理","HTTP"]', '["阿里","腾讯","字节跳动","美团","京东","百度","网易","小米","Shopee","滴滴"]', '前端工程师岗位，重点考察 JS/TS 基础、Vue/React 框架、工程化、浏览器原理、性能优化、HTTP 与网络', 2, 'active', '', '2026-07-28 16:39:34', '', '2026-07-28 16:39:34', null, '0');
INSERT INTO `moyun-db`.portal_interview_position (id, code, name, industry, level, required_skills, hot_companies, description, sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, 'algorithm', '算法工程师', '互联网', 'mid', '["算法","数据结构","动态规划","图论","字符串","数组","链表","树","递归","排序","机器学习","深度学习","数学"]', '["阿里","腾讯","字节跳动","百度","美团","快手","小红书","华为","商汤","旷视"]', '算法工程师岗位，重点考察数据结构与算法、动态规划、图论、字符串算法、机器学习与深度学习基础', 3, 'active', '', '2026-07-28 16:39:34', '', '2026-07-28 16:39:34', null, '0');


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

SELECT portal_interview_question.* FROM portal_interview_question;

INSERT INTO `moyun-db`.portal_interview_question (id, title, description, difficulty, category_id, tags, companies, acceptance_rate, submission_count, like_count, hint, solution, sort, status, question_type, examine_points, answer_outline, scoring_criteria, reference_answer, prerequisite_ids, practice_mode, options, correct_answer, analysis, knowledge_tags, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, 'TCP三次握手的目的是什么？', 'TCP协议建立连接时使用三次握手，请选择其主要目的。', 'easy', null, 'TCP,网络,三次握手', null, 85.50, 120, 18, '思考序列号同步的作用', null, 1, 'published', 'bagwen', null, null, null, null, null, 'choice', '[{"text": "建立网络路由", "label": "A", "is_correct": false}, {"text": "同步双方的初始序列号", "label": "B", "is_correct": true}, {"text": "加密传输数据", "label": "C", "is_correct": false}, {"text": "压缩数据包", "label": "D", "is_correct": false}]', 'B', '三次握手的核心目的是让通信双方同步初始序列号（ISN），确保后续数据传输的有序性和可靠性。序列号用于确认应答和重传控制，是 TCP 可靠传输的基础。', 'TCP,三次握手,序列号,可靠传输', 'admin', '2026-08-20 10:56:16', '', '2026-08-20 10:56:16', null, '0');
INSERT INTO `moyun-db`.portal_interview_question (id, title, description, difficulty, category_id, tags, companies, acceptance_rate, submission_count, like_count, hint, solution, sort, status, question_type, examine_points, answer_outline, scoring_criteria, reference_answer, prerequisite_ids, practice_mode, options, correct_answer, analysis, knowledge_tags, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, 'HTTP与HTTPS的主要区别是什么？', 'HTTPS 相比 HTTP 增加了安全性，请选择其核心区别。', 'easy', null, 'HTTP,HTTPS,SSL,TLS', null, 78.30, 95, 12, '思考加密层的作用', null, 2, 'published', 'bagwen', null, null, null, null, null, 'choice', '[{"text": "HTTPS使用了不同的端口", "label": "A", "is_correct": false}, {"text": "HTTPS加入了SSL/TLS加密层", "label": "B", "is_correct": true}, {"text": "HTTPS传输速度更快", "label": "C", "is_correct": false}, {"text": "HTTPS不需要证书", "label": "D", "is_correct": false}]', 'B', 'HTTPS 在 HTTP 基础上加入了 SSL/TLS 协议层，对传输内容进行加密，保证数据机密性和完整性。HTTP 默认端口 80，HTTPS 默认端口 443。HTTPS 需要CA证书。', 'HTTP,HTTPS,SSL,TLS,加密', 'admin', '2026-08-20 10:56:16', '', '2026-08-20 10:56:16', null, '0');
INSERT INTO `moyun-db`.portal_interview_question (id, title, description, difficulty, category_id, tags, companies, acceptance_rate, submission_count, like_count, hint, solution, sort, status, question_type, examine_points, answer_outline, scoring_criteria, reference_answer, prerequisite_ids, practice_mode, options, correct_answer, analysis, knowledge_tags, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, '进程和线程的本质区别是什么？', '进程和线程是操作系统的核心概念，请选择其本质区别。', 'medium', null, '进程,线程,操作系统', null, 65.20, 88, 15, '思考资源分配单位', null, 3, 'published', 'bagwen', null, null, null, null, null, 'choice', '[{"text": "线程是进程内的执行单元，共享进程资源", "label": "A", "is_correct": true}, {"text": "进程比线程轻量", "label": "B", "is_correct": false}, {"text": "线程不能独立存在", "label": "C", "is_correct": false}, {"text": "进程间通信比线程间快", "label": "D", "is_correct": false}]', 'A', '进程是资源分配的最小单位，线程是CPU调度的最小单位。线程存在于进程内，共享进程的内存空间和资源，线程间通信成本低于进程间通信，但需要同步机制保证数据一致性。', '进程,线程,资源分配,CPU调度', 'admin', '2026-08-20 10:56:17', '', '2026-08-20 10:56:17', null, '0');
INSERT INTO `moyun-db`.portal_interview_question (id, title, description, difficulty, category_id, tags, companies, acceptance_rate, submission_count, like_count, hint, solution, sort, status, question_type, examine_points, answer_outline, scoring_criteria, reference_answer, prerequisite_ids, practice_mode, options, correct_answer, analysis, knowledge_tags, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (4, '以下哪种情况会产生死锁？', '死锁是并发编程的常见问题，请选择产生死锁的必要条件组合。', 'medium', null, '死锁,并发,操作系统', null, 58.40, 72, 10, '思考死锁的四个必要条件', null, 4, 'published', 'bagwen', null, null, null, null, null, 'choice', '[{"text": "互斥、占有并等待、不剥夺、循环等待", "label": "A", "is_correct": true}, {"text": "共享、抢占、可剥夺、线性等待", "label": "B", "is_correct": false}, {"text": "互斥、释放、剥夺、无等待", "label": "C", "is_correct": false}, {"text": "并发、并行、同步、异步", "label": "D", "is_correct": false}]', 'A', '死锁产生的四个必要条件：互斥条件、占有并等待、不剥夺条件、循环等待条件。只有四个条件同时满足才会发生死锁，破坏任一条件即可预防死锁。', '死锁,并发,互斥,资源竞争', 'admin', '2026-08-20 10:56:17', '', '2026-08-20 10:56:17', null, '0');
INSERT INTO `moyun-db`.portal_interview_question (id, title, description, difficulty, category_id, tags, companies, acceptance_rate, submission_count, like_count, hint, solution, sort, status, question_type, examine_points, answer_outline, scoring_criteria, reference_answer, prerequisite_ids, practice_mode, options, correct_answer, analysis, knowledge_tags, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5, 'Redis中以下哪种数据类型不支持？', 'Redis 提供多种数据结构，请选择不支持的类型。', 'medium', null, 'Redis,数据结构,缓存', null, 62.10, 65, 8, '回顾Redis五大基本类型', null, 5, 'published', 'bagwen', null, null, null, null, null, 'choice', '[{"text": "String 字符串", "label": "A", "is_correct": false}, {"text": "Hash 哈希表", "label": "B", "is_correct": false}, {"text": "Tree 树结构", "label": "C", "is_correct": true}, {"text": "Set 集合", "label": "D", "is_correct": false}]', 'C', 'Redis 五大基本数据类型：String（字符串）、Hash（哈希表）、List（列表）、Set（集合）、ZSet（有序集合）。Tree 不是 Redis 的基本类型（Redis 内部用跳表实现 ZSet，但不直接暴露 Tree 结构）。', 'Redis,数据结构,缓存,NoSQL', 'admin', '2026-08-20 10:56:17', '', '2026-08-20 10:56:17', null, '0');
INSERT INTO `moyun-db`.portal_interview_question (id, title, description, difficulty, category_id, tags, companies, acceptance_rate, submission_count, like_count, hint, solution, sort, status, question_type, examine_points, answer_outline, scoring_criteria, reference_answer, prerequisite_ids, practice_mode, options, correct_answer, analysis, knowledge_tags, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (6, '两数之和', '给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值的那两个整数，并返回它们的数组下标。

你可以假设每种输入只会对应一个答案。但是，数组中同一个元素不能在答案里重复出现。

你可以按任意顺序返回答案。

示例 1：
输入：nums = [2,7,11,15], target = 9
输出：[0,1]
解释：因为 nums[0] + nums[1] == 9 ，返回 [0, 1] 。', 'easy', null, '数组,哈希表', '字节跳动,腾讯,阿里', 52.30, 156, 22, '考虑使用哈希表一次遍历', 'function twoSum(nums, target) {
  const map = new Map();
  for (let i = 0; i < nums.length; i++) {
    const complement = target - nums[i];
    if (map.has(complement)) return [map.get(complement), i];
    map.set(nums[i], i);
  }
}', 10, 'published', 'algorithm', null, null, null, null, null, 'coding', null, null, '哈希表一次遍历解法：遍历数组时，对每个元素计算其补数（target - nums[i]），若补数已在哈希表中则直接返回两个下标，否则将当前元素存入哈希表。时间复杂度 O(n)，空间复杂度 O(n)。', '数组,哈希表,一次遍历', 'admin', '2026-08-20 10:56:17', '', '2026-08-20 10:56:17', null, '0');
INSERT INTO `moyun-db`.portal_interview_question (id, title, description, difficulty, category_id, tags, companies, acceptance_rate, submission_count, like_count, hint, solution, sort, status, question_type, examine_points, answer_outline, scoring_criteria, reference_answer, prerequisite_ids, practice_mode, options, correct_answer, analysis, knowledge_tags, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (7, '反转链表', '给你单链表的头节点 head，请你反转链表，并返回反转后的链表。

示例：
输入：head = [1,2,3,4,5]
输出：[5,4,3,2,1]', 'easy', null, '链表,指针', '美团,字节跳动', 68.50, 98, 15, '考虑迭代或递归两种解法', 'function reverseList(head) {
  let prev = null, curr = head;
  while (curr) {
    const next = curr.next;
    curr.next = prev;
    prev = curr;
    curr = next;
  }
  return prev;
}', 11, 'published', 'algorithm', null, null, null, null, null, 'coding', null, null, '迭代解法：使用三个指针 prev/curr/next，逐个节点反转指向。时间复杂度 O(n)，空间复杂度 O(1)。递归解法也可，但空间复杂度为 O(n)（递归栈）。', '链表,指针,迭代', 'admin', '2026-08-20 10:56:17', '', '2026-08-20 10:56:17', null, '0');
INSERT INTO `moyun-db`.portal_interview_question (id, title, description, difficulty, category_id, tags, companies, acceptance_rate, submission_count, like_count, hint, solution, sort, status, question_type, examine_points, answer_outline, scoring_criteria, reference_answer, prerequisite_ids, practice_mode, options, correct_answer, analysis, knowledge_tags, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (8, '有效的括号', '给定一个只包括 (、)、{、}、[、] 的字符串 s，判断字符串是否有效。

有效字符串需满足：
1. 左括号必须用相同类型的右括号闭合。
2. 左括号必须以正确的顺序闭合。

示例：
输入：s = "()[]{}"
输出：true
输入：s = "(]"
输出：false', 'medium', null, '栈,字符串', '字节跳动,腾讯', 44.80, 110, 18, '考虑使用栈结构', 'function isValid(s) {
  const stack = [];
  const map = {")":"(", "]":"[", "}":"{"};
  for (const c of s) {
    if (c in map) {
      if (stack.pop() !== map[c]) return false;
    } else stack.push(c);
  }
  return stack.length === 0;
}', 12, 'published', 'algorithm', null, null, null, null, null, 'coding', null, null, '栈解法：遍历字符串，遇到左括号入栈，遇到右括号弹出栈顶判断是否匹配。最终栈为空则有效。时间复杂度 O(n)，空间复杂度 O(n)。', '栈,字符串,匹配', 'admin', '2026-08-20 10:56:17', '', '2026-08-20 10:56:17', null, '0');

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
-- Table structure for table `portal_interview_question_test_case`
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

SELECT * FROM portal_interview_question_test_case;

INSERT INTO `moyun-db`.portal_interview_question_test_case (id, question_id, input, expected_output, is_sample, order_num, explanation, create_time, update_time) VALUES (1, 6, '[2,7,11,15]
9', '[0,1]', 1, 1, '基本示例：2+7=9', '2026-08-20 10:56:17', '2026-08-20 10:56:17');
INSERT INTO `moyun-db`.portal_interview_question_test_case (id, question_id, input, expected_output, is_sample, order_num, explanation, create_time, update_time) VALUES (2, 6, '[3,2,4]
6', '[1,2]', 1, 2, '2+4=6', '2026-08-20 10:56:17', '2026-08-20 10:56:17');
INSERT INTO `moyun-db`.portal_interview_question_test_case (id, question_id, input, expected_output, is_sample, order_num, explanation, create_time, update_time) VALUES (3, 6, '[3,3]
6', '[0,1]', 0, 3, '相同值不同下标', '2026-08-20 10:56:17', '2026-08-20 10:56:17');
INSERT INTO `moyun-db`.portal_interview_question_test_case (id, question_id, input, expected_output, is_sample, order_num, explanation, create_time, update_time) VALUES (4, 7, '[1,2,3,4,5]', '[5,4,3,2,1]', 1, 1, '基本示例', '2026-08-20 10:56:17', '2026-08-20 10:56:17');
INSERT INTO `moyun-db`.portal_interview_question_test_case (id, question_id, input, expected_output, is_sample, order_num, explanation, create_time, update_time) VALUES (5, 7, '[1,2]', '[2,1]', 0, 2, '两个节点', '2026-08-20 10:56:17', '2026-08-20 10:56:17');
INSERT INTO `moyun-db`.portal_interview_question_test_case (id, question_id, input, expected_output, is_sample, order_num, explanation, create_time, update_time) VALUES (6, 7, '[]', '[]', 0, 3, '空链表', '2026-08-20 10:56:17', '2026-08-20 10:56:17');
INSERT INTO `moyun-db`.portal_interview_question_test_case (id, question_id, input, expected_output, is_sample, order_num, explanation, create_time, update_time) VALUES (7, 8, '()[]{}', 'true', 1, 1, '基本有效示例', '2026-08-20 10:56:17', '2026-08-20 10:56:17');
INSERT INTO `moyun-db`.portal_interview_question_test_case (id, question_id, input, expected_output, is_sample, order_num, explanation, create_time, update_time) VALUES (8, 8, '(]', 'false', 1, 2, '基本无效示例', '2026-08-20 10:56:17', '2026-08-20 10:56:17');
INSERT INTO `moyun-db`.portal_interview_question_test_case (id, question_id, input, expected_output, is_sample, order_num, explanation, create_time, update_time) VALUES (9, 8, '([)]', 'false', 0, 3, '嵌套但不合法', '2026-08-20 10:56:17', '2026-08-20 10:56:17');

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

LOCK TABLES `portal_interview_submission` WRITE;
/*!40000 ALTER TABLE `portal_interview_submission` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_interview_submission` ENABLE KEYS */;
UNLOCK TABLES;

-- ==================== 增量变更（2026-08-25） ====================
-- 精选笔记功能：portal_interview_submission 增加精选标记与精选时间
-- 修复：实体 PortalInterviewSubmission 含 isFeatured/featuredTime 字段，但表缺列导致
--       GET /portal/interview/question/{id}/featured-notes 报 500（Unknown column）
ALTER TABLE `portal_interview_submission`
  ADD COLUMN `is_featured` tinyint(1) DEFAULT 0 COMMENT '是否精选（后台采纳为优质笔记）：0=否 1=是' AFTER `note`,
  ADD COLUMN `featured_time` datetime DEFAULT NULL COMMENT '精选时间' AFTER `is_featured`,
  ADD KEY `idx_is_featured` (`is_featured`);

-- ==================== 增量变更：创作者认证实名合规改造（2026-08-25） ====================
-- 背景：portal_creator_certification.cert_no 原为明文存储，不符合个保法最小必要与敏感信息安全要求。
-- 方案（合规基线）：
--   1) 新数据证件号只存密文（cert_no_enc，AES-GCM）+ 脱敏展示值（cert_no_mask）；
--      原 cert_no 字段仅兼容存量明文数据，新写入一律置 NULL。
--   2) 由身份证号推导性别（derived_gender）与出生日期（derived_birth），存于认证表，
--      不回填 portal_user（实名数据仅限审核/风控使用，不进入公开资料）。
--   3) 预留第三方实名核验字段：verify_channel（manual=人工审核）/ verify_serial（核验流水号），
--      后期接入阿里云/腾讯云实名核验 API 时填充。
ALTER TABLE `portal_creator_certification`
  ADD COLUMN `cert_no_enc` varchar(512) DEFAULT NULL COMMENT '证件号密文（AES-GCM，格式 enc:v1:iv:cipher，base64）' AFTER `cert_no`,
  ADD COLUMN `cert_no_mask` varchar(32) DEFAULT NULL COMMENT '证件号脱敏展示值（如 110***********1234）' AFTER `cert_no_enc`,
  ADD COLUMN `derived_gender` varchar(8) DEFAULT NULL COMMENT '由证件号推导的性别（男/女），仅身份认证类型' AFTER `cert_no_mask`,
  ADD COLUMN `derived_birth` date DEFAULT NULL COMMENT '由证件号推导的出生日期' AFTER `derived_gender`,
  ADD COLUMN `verify_channel` varchar(32) DEFAULT 'manual' COMMENT '实名核验渠道：manual=人工审核（默认），后期可扩展 aliyun/tencent 等' AFTER `derived_birth`,
  ADD COLUMN `verify_serial` varchar(64) DEFAULT NULL COMMENT '第三方实名核验流水号（预留，接入核验API后填充）' AFTER `verify_channel`;

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

LOCK TABLES `portal_message_session` WRITE;
/*!40000 ALTER TABLE `portal_message_session` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_message_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_order`
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

LOCK TABLES `portal_order` WRITE;
/*!40000 ALTER TABLE `portal_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_order` ENABLE KEYS */;
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

LOCK TABLES `portal_reading_preference` WRITE;
/*!40000 ALTER TABLE `portal_reading_preference` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_reading_preference` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_reading_progress`
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

LOCK TABLES `portal_reading_progress` WRITE;
/*!40000 ALTER TABLE `portal_reading_progress` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_reading_progress` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_report`
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

LOCK TABLES `portal_report` WRITE;
/*!40000 ALTER TABLE `portal_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_shop_exchange`
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

LOCK TABLES `portal_shop_exchange` WRITE;
/*!40000 ALTER TABLE `portal_shop_exchange` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_shop_exchange` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_shop_item`
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

LOCK TABLES `portal_shop_item` WRITE;
/*!40000 ALTER TABLE `portal_shop_item` DISABLE KEYS */;
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

SELECT * FROM `portal_tag`;

INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (29, '生活哲思', 'life-philosophy', 1, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '人文类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (30, '城市记忆', 'city-memory', 2, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '人文类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (31, '自然写作', 'nature-writing', 3, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '人文类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (32, '情感随笔', 'emotional-essay', 4, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-20 18:24:41', '人文类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (33, '人间烟火', 'life-fireworks', 5, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-20 18:24:41', '人文类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (34, '乡愁记忆', 'nostalgia', 6, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '人文类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (35, '孤独成长', 'loneliness-growth', 7, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '人文类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (36, '四季感悟', 'seasons-feeling', 8, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '人文类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (37, 'SpringBoot实战', 'springboot-practice', 9, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (38, 'React Hooks', 'react-hooks', 10, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (39, 'AI辅助开发', 'ai-assisted-dev', 11, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (40, '算法突破', 'algorithm-breakthrough', 12, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (41, 'Java并发', 'java-concurrency', 13, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (42, 'Vue3实践', 'vue3-practice', 14, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (43, '微服务架构', 'microservices', 15, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (44, 'MySQL优化', 'mysql-optimization', 16, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (45, 'Git协作', 'git-collaboration', 17, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (46, '前端性能', 'frontend-performance', 18, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (47, 'JVM调优', 'jvm-tuning', 19, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (48, '系统设计', 'system-design', 20, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '技术类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (49, '新手入门', 'beginner-guide', 21, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '通用类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (50, '进阶提升', 'advanced-improvement', 22, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '通用类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (51, '面试备战', 'interview-prep', 23, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '通用类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (52, '读书心得', 'reading-notes', 24, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '通用类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (53, '写作技巧', 'writing-tips', 25, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '通用类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (54, '学习方法', 'learning-methods-tag', 26, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '通用类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (55, '职场经验', 'career-experience', 27, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '通用类', '0');
INSERT INTO `moyun-db`.portal_tag (id, name, slug, sort, status, module, reference_count, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (56, '个人成长', 'personal-growth', 28, '0', null, 0, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', '通用类', '0');

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

SELECT * FROM portal_task;

INSERT INTO `moyun-db`.portal_task (id, code, name, description, task_type, reward_points, target_count, icon, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, 'daily_checkin', '每日签到', '每天签到一次，保持活跃', 'daily', 10, 1, null, 'active', 'admin', '2026-07-28 16:29:45', '', '2026-07-28 16:29:45', null, '0');
INSERT INTO `moyun-db`.portal_task (id, code, name, description, task_type, reward_points, target_count, icon, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, 'daily_publish', '每日发文', '每日发布 1 篇文章', 'daily', 20, 1, null, 'active', 'admin', '2026-07-28 16:29:45', '', '2026-07-28 16:29:45', null, '0');
INSERT INTO `moyun-db`.portal_task (id, code, name, description, task_type, reward_points, target_count, icon, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, 'daily_comment', '每日互动', '每日评论 3 次', 'daily', 15, 3, null, 'active', 'admin', '2026-07-28 16:29:45', '', '2026-07-28 16:29:45', null, '0');
INSERT INTO `moyun-db`.portal_task (id, code, name, description, task_type, reward_points, target_count, icon, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (4, 'daily_like', '每日点赞', '每日点赞 5 次', 'daily', 10, 5, null, 'active', 'admin', '2026-07-28 16:29:45', '', '2026-07-28 16:29:45', null, '0');
INSERT INTO `moyun-db`.portal_task (id, code, name, description, task_type, reward_points, target_count, icon, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5, 'daily_solve', '每日刷题', '每日解答 3 道面试题', 'daily', 20, 3, null, 'active', 'admin', '2026-07-28 16:29:45', '', '2026-07-28 16:29:45', null, '0');
INSERT INTO `moyun-db`.portal_task (id, code, name, description, task_type, reward_points, target_count, icon, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (6, 'first_article', '初露锋芒', '发布第一篇文章', 'achievement', 50, 1, null, 'active', 'admin', '2026-07-28 16:29:45', '', '2026-07-28 16:29:45', null, '0');
INSERT INTO `moyun-db`.portal_task (id, code, name, description, task_type, reward_points, target_count, icon, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (7, 'solve_50', '刷题能手', '累计解答 50 道面试题', 'achievement', 200, 50, null, 'active', 'admin', '2026-07-28 16:29:45', '', '2026-07-28 16:29:45', null, '0');


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

LOCK TABLES `portal_user` WRITE;
/*!40000 ALTER TABLE `portal_user` DISABLE KEYS */;
INSERT INTO `portal_user` VALUES (6,NULL,'zhangsan',NULL,'19987671567@163.com',NULL,'$2a$10$DK9a7y2EjUbX4/zGe9ZWsOCjGiNRSZflq9MFlUqM1Jb2.S9XUI2L.',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,1,1,1,1,0,0,1,'user',0,NULL,0,0,0,'0','0','127.0.0.1','2026-08-20 14:13:08','','2026-08-20 08:56:07','','2026-08-20 14:13:08',NULL);
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

LOCK TABLES `portal_vip_package` WRITE;
/*!40000 ALTER TABLE `portal_vip_package` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_vip_package` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_voice_interview`
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

LOCK TABLES `portal_voice_interview` WRITE;
/*!40000 ALTER TABLE `portal_voice_interview` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_voice_interview` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_voice_interview_qa`
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

LOCK TABLES `portal_voice_interview_qa` WRITE;
/*!40000 ALTER TABLE `portal_voice_interview_qa` DISABLE KEYS */;
/*!40000 ALTER TABLE `portal_voice_interview_qa` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portal_wallet`
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
  `festival_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联特殊日期名称（节日/节气/纪念日，AI生成时自动识别）',
  `source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'ai' COMMENT '来源：ai=AI生成 / manual=手动创建',
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
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日历信息表';
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
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储的悲观锁信息表';
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
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='暂停的触发器表';
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

LOCK TABLES `qrtz_simple_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_simple_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_simple_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_simprop_triggers`
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

LOCK TABLES `qrtz_simprop_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_simprop_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_simprop_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_triggers`
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

LOCK TABLES `qrtz_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_audit_task`
--

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

LOCK TABLES `sys_audit_task` WRITE;
/*!40000 ALTER TABLE `sys_audit_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_audit_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
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
SELECT * FROM sys_config;

INSERT INTO `moyun-db`.sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', '2026-08-19 18:01:44', '', null, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow', '0');
INSERT INTO `moyun-db`.sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', '2026-08-19 18:01:44', '', null, '初始化密码 123456', '0');
INSERT INTO `moyun-db`.sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', '2026-08-19 18:01:44', '', null, '深色主题theme-dark，浅色主题theme-light', '0');
INSERT INTO `moyun-db`.sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (4, '账号自助-验证码开关', 'sys.account.captchaEnabled', 'true', 'Y', 'admin', '2026-08-19 18:01:44', '', null, '是否开启验证码功能（true开启，false关闭）', '0');
INSERT INTO `moyun-db`.sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'admin', '2026-08-19 18:01:44', '', null, '是否开启注册用户功能（true开启，false关闭）', '0');
INSERT INTO `moyun-db`.sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (6, '用户登录-黑名单列表', 'sys.login.blackIPList', '', 'Y', 'admin', '2026-08-19 18:01:44', '', null, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）', '0');

--
-- Table structure for table `sys_dept`
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

SELECT * FROM sys_dept;

INSERT INTO `moyun-db`.sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (100, 0, '0', '若依科技', 0, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);
INSERT INTO `moyun-db`.sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (101, 100, '0,100', '深圳总公司', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);
INSERT INTO `moyun-db`.sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (102, 100, '0,100', '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);
INSERT INTO `moyun-db`.sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (103, 101, '0,100,101', '研发部门', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);
INSERT INTO `moyun-db`.sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (104, 101, '0,100,101', '市场部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);
INSERT INTO `moyun-db`.sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (105, 101, '0,100,101', '测试部门', 3, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);
INSERT INTO `moyun-db`.sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (106, 101, '0,100,101', '财务部门', 4, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);
INSERT INTO `moyun-db`.sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (107, 101, '0,100,101', '运维部门', 5, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);
INSERT INTO `moyun-db`.sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (108, 102, '0,100,102', '市场部门', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);
INSERT INTO `moyun-db`.sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (109, 102, '0,100,102', '财务部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);



--
-- Table structure for table `sys_dict_type`
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
SELECT  * FROM `sys_dict_type` ;

INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, '用户性别', 'sys_user_sex', '0', 'admin', '2026-08-19 18:01:44', '', null, '用户性别列表', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, '菜单状态', 'sys_show_hide', '0', 'admin', '2026-08-19 18:01:44', '', null, '菜单状态列表', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, '系统开关', 'sys_normal_disable', '0', 'admin', '2026-08-19 18:01:44', '', null, '系统开关列表', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (4, '任务状态', 'sys_job_status', '0', 'admin', '2026-08-19 18:01:44', '', null, '任务状态列表', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5, '任务分组', 'sys_job_group', '0', 'admin', '2026-08-19 18:01:44', '', null, '任务分组列表', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (6, '系统是否', 'sys_yes_no', '0', 'admin', '2026-08-19 18:01:44', '', null, '系统是否列表', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (7, '通知类型', 'sys_notice_type', '0', 'admin', '2026-08-19 18:01:44', '', null, '通知类型列表', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (8, '通知状态', 'sys_notice_status', '0', 'admin', '2026-08-19 18:01:44', '', null, '通知状态列表', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (9, '操作类型', 'sys_oper_type', '0', 'admin', '2026-08-19 18:01:44', '', null, '操作类型列表', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (10, '系统状态', 'sys_common_status', '0', 'admin', '2026-08-19 18:01:44', '', null, '登录状态列表', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (100, '支付状态', 'portal_pay_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 支付状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (101, '支付渠道', 'portal_pay_channel', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 支付渠道', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (102, '打赏目标类型', 'portal_tip_target_type', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 打赏目标类型', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (103, '钱包交易类型', 'portal_wallet_txn_type', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 钱包交易类型', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (104, '文章状态', 'cms_article_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 文章状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (105, '专栏状态', 'cms_column_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 专栏状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (106, '话题状态', 'cms_topic_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 话题状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (107, '征文活动状态', 'cms_contest_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 征文活动状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (108, '审核任务类型', 'cms_audit_task_type', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 审核任务类型', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (109, '审核任务状态', 'cms_audit_task_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 审核任务状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (110, '反馈类型', 'cms_feedback_type', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 反馈类型', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (111, '举报类型', 'cms_report_type', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 举报类型', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (112, '处理状态', 'cms_handle_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 处理状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (113, '书籍类型', 'portal_book_type', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 书籍类型', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (114, '书籍连载状态', 'portal_book_serial_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 书籍连载状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (115, '访问级别', 'portal_access_type', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 访问级别', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (116, '业务通用状态', 'portal_common_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 业务通用状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (117, '学习计划类型', 'portal_study_plan_type', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 学习计划类型', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (118, '学习计划状态', 'portal_study_plan_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 学习计划状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (119, '错题状态', 'portal_wrong_question_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 错题状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (120, '题目难度', 'portal_question_difficulty', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 题目难度', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (121, '题目类型', 'portal_question_type', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 题目类型', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (122, '简历模板分类', 'portal_resume_category', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v10.2 简历模板分类（英文值）', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (123, '广告位标识', 'portal_ad_slot_key', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 广告位标识', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (124, 'VIP套餐状态', 'cms_vip_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 VIP套餐状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (125, '登录端类型', 'sys_login_type', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v9.6 登录端类型', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (126, '语音面试状态', 'voice_interview_status', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v10.1 语音面试状态', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (127, '面试官风格', 'voice_interview_style', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v10.1 面试官风格', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (128, '提示级别', 'voice_interview_hint_level', '0', 'admin', '2026-08-19 18:01:46', '', null, 'v10.1 提示级别', '0');
INSERT INTO `moyun-db`.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (129, '练习模式', 'portal_practice_mode', '0', 'admin', '2026-08-20 10:56:17', '', null, '题目练习模式：reading/choice/coding', '0');


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
SELECT * FROM sys_dict_data;

INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, 1, '男', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', '2026-08-19 18:01:44', '', null, '性别男', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, 2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '性别女', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '性别未知', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', '2026-08-19 18:01:44', '', null, '显示菜单', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '隐藏菜单', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', '2026-08-19 18:01:44', '', null, '正常状态', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '停用状态', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (8, 1, '正常', '0', 'sys_job_status', '', 'primary', 'Y', '0', 'admin', '2026-08-19 18:01:44', '', null, '正常状态', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (9, 2, '暂停', '1', 'sys_job_status', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '停用状态', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (10, 1, '默认', 'DEFAULT', 'sys_job_group', '', '', 'Y', '0', 'admin', '2026-08-19 18:01:44', '', null, '默认分组', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (11, 2, '系统', 'SYSTEM', 'sys_job_group', '', '', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '系统分组', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', '2026-08-19 18:01:44', '', null, '系统默认是', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '系统默认否', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', '2026-08-19 18:01:44', '', null, '通知', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '公告', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', '2026-08-19 18:01:44', '', null, '正常状态', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '关闭状态', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (18, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '其他操作', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (19, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '新增操作', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (20, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '修改操作', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (21, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '删除操作', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (22, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '授权操作', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (23, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '导出操作', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (24, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '导入操作', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (25, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '强退操作', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (26, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '生成操作', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (27, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '清空操作', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (28, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '正常状态', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (29, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:44', '', null, '停用状态', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (100, 1, '待支付', 'pending', 'portal_pay_status', '', 'warning', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (101, 2, '已支付', 'paid', 'portal_pay_status', '', 'success', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (102, 3, '已退款', 'refunded', 'portal_pay_status', '', 'info', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (103, 4, '已关闭', 'closed', 'portal_pay_status', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (104, 5, '支付失败', 'failed', 'portal_pay_status', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (105, 1, '积分', 'points', 'portal_pay_channel', '', 'info', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (106, 2, '支付宝', 'alipay', 'portal_pay_channel', '', 'primary', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (107, 3, '微信', 'wechat', 'portal_pay_channel', '', 'success', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (108, 4, '钱包', 'wallet', 'portal_pay_channel', '', 'warning', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (109, 1, '待开始', 'idle', 'voice_interview_status', '', 'info', 'Y', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (110, 2, '聆听中', 'listening', 'voice_interview_status', '', 'primary', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (111, 3, '播报中', 'speaking', 'voice_interview_status', '', 'success', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (112, 4, '评分中', 'scoring', 'voice_interview_status', '', 'warning', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (113, 5, '已结束', 'done', 'voice_interview_status', '', 'info', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (114, 1, '专业', 'professional', 'voice_interview_style', '', 'primary', 'Y', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (115, 2, '亲和', 'friendly', 'voice_interview_style', '', 'success', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (116, 3, '严格', 'strict', 'voice_interview_style', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (117, 1, '切入点提示', '1', 'voice_interview_hint_level', '', 'info', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (118, 2, '结构提示', '2', 'voice_interview_hint_level', '', 'warning', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (119, 3, '全量提示', '3', 'voice_interview_hint_level', '', 'danger', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (120, 1, '技术岗', 'tech', 'portal_resume_category', '', 'primary', 'Y', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (121, 2, '产品岗', 'product', 'portal_resume_category', '', 'success', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (122, 3, '应届生', 'fresh', 'portal_resume_category', '', 'info', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (123, 4, '社招', 'social', 'portal_resume_category', '', 'warning', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (124, 5, '实习', 'intern', 'portal_resume_category', '', 'info', 'N', '0', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (125, 1, '展示阅读', 'reading', 'portal_practice_mode', '', 'default', 'Y', '0', 'admin', '2026-08-20 10:56:17', '', null, '看题+答案', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (126, 2, '选择题', 'choice', 'portal_practice_mode', '', 'success', 'N', '0', 'admin', '2026-08-20 10:56:17', '', null, '选项作答+判分', '0');
INSERT INTO `moyun-db`.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (127, 3, '编程题', 'coding', 'portal_practice_mode', '', 'primary', 'N', '0', 'admin', '2026-08-20 10:56:17', '', null, '代码作答+测试用例判定', '0');



--
-- Table structure for table `sys_file`
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

SELECT * FROM sys_job;

INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?', '3', '1', '1', 'admin', '2026-07-28 15:42:36', '', null, '', '0');
INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')', '0/15 * * * * ?', '3', '1', '1', 'admin', '2026-07-28 15:42:36', '', null, '', '0');
INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000, 316.50, 100)', '0/20 * * * * ?', '3', '1', '1', 'admin', '2026-07-28 15:42:36', '', null, '', '0');
INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (100, '敏感词扫描-话题', 'DEFAULT', 'sensitiveScanTask.scanTopics()', '0 0 3 * * ?', '3', '1', '0', 'admin', '2026-08-19 18:01:41', '', null, '定时扫描话题内容，命中则转待审核', '0');
INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (101, '敏感词扫描-观点', 'DEFAULT', 'sensitiveScanTask.scanTopicPosts()', '0 5 3 * * ?', '3', '1', '0', 'admin', '2026-08-19 18:01:41', '', null, '定时扫描话题观点，命中则标记', '0');
INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (102, '缓存清理-表结构', 'DEFAULT', 'cacheCleanupTask.cleanupExpiredCache()', '0 */5 * * * ?', '3', '1', '0', 'admin', '2026-08-19 18:01:42', '', null, '每5分钟清理 DataSourceService 的过期表结构缓存（原 CacheCleanupTask @Scheduled，迁移至 Quartz 统一调度）', '0');
INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (103, '缓存清理-限流器', 'DEFAULT', 'cacheCleanupTask.cleanupRateLimiter()', '0 */2 * * * ?', '3', '1', '0', 'admin', '2026-08-19 18:01:42', '', null, '每2分钟清理 RateLimiter 过期数据（原 CacheCleanupTask @Scheduled，迁移至 Quartz 统一调度）', '0');
INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (104, '会话清理-数据分析', 'DEFAULT', 'dataAnalysisConversationServiceImpl.cleanExpiredSessions()', '0 */30 * * * ?', '3', '1', '0', 'admin', '2026-08-19 18:01:42', '', null, '每30分钟清理超过1小时未访问的数据分析对话会话（原 DataAnalysisConversationServiceImpl @Scheduled，迁移至 Quartz 统一调度）', '0');
INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (105, '日志落盘-Token使用', 'DEFAULT', 'tokenUsageServiceImpl.flushLogsToDB()', '0 * * * * ?', '1', '1', '0', 'admin', '2026-08-19 18:01:42', '', null, '每1分钟将 Redis 中的 Token 使用日志批量写入 DB（原 TokenUsageServiceImpl @Scheduled，迁移至 Quartz 统一调度；misfire=1 立即补偿避免日志丢失）', '0');
INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (106, '敏感词扫描-文章评论', 'DEFAULT', 'sensitiveScanTask.scanArticleComments()', '0 10 3 * * ?', '3', '1', '0', 'admin', '2026-08-19 18:01:45', '', null, '扫描已发布文章评论(portal_comment.status=1)，命中敏感词转驳回(status=2)并通知作者', '0');
INSERT INTO `moyun-db`.sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (107, '敏感词扫描-面经评论', 'DEFAULT', 'sensitiveScanTask.scanInterviewComments()', '0 15 3 * * ?', '3', '1', '0', 'admin', '2026-08-19 18:01:45', '', null, '扫描已发布面经评论(portal_interview_comment.status=published)，命中敏感词转rejected并通知作者', '0');

--
-- Table structure for table `sys_job_log`
--

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

LOCK TABLES `sys_job_log` WRITE;
/*!40000 ALTER TABLE `sys_job_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_job_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_job_scan_issue`
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

LOCK TABLES `sys_job_scan_issue` WRITE;
/*!40000 ALTER TABLE `sys_job_scan_issue` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_job_scan_issue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_logininfor`
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

SELECT * FROM sys_menu;

INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, '系统设置', 0, 7, 'system', null, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', '2026-08-19 18:01:45', '', null, '系统管理目录', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, '系统监控', 1, 8, 'monitor', null, '', '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', '2026-08-19 18:01:45', '', null, '系统监控目录', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, '系统工具', 1, 9, 'tool', null, '', '', 1, 0, 'M', '0', '0', '', 'tool', 'admin', '2026-08-19 18:01:45', '', null, '系统工具目录', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (100, '用户管理', 1, 1, 'user', 'system/user/index', '', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', '2026-08-19 18:01:45', '', null, '用户管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', '2026-08-19 18:01:45', '', null, '角色管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', '2026-08-19 18:01:45', '', null, '菜单管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', '2026-08-19 18:01:45', '', null, '部门管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post', 'admin', '2026-08-19 18:01:45', '', null, '岗位管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', '2026-08-19 18:01:45', '', null, '字典管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 'admin', '2026-08-19 18:01:45', '', null, '参数设置菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (108, '日志管理', 1, 9, 'log', '', '', '', 1, 0, 'M', '0', '0', '', 'log', 'admin', '2026-08-19 18:01:45', '', null, '日志管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 'admin', '2026-08-19 18:01:45', '', null, '在线用户菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (110, '定时任务', 2, 2, 'job', 'monitor/job/index', '', '', 1, 0, 'C', '0', '0', 'monitor:job:list', 'job', 'admin', '2026-08-19 18:01:45', '', null, '定时任务菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (111, '数据监控', 2, 3, 'druid', 'monitor/druid/index', '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list', 'druid', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '数据监控菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (112, '服务监控', 2, 4, 'server', 'monitor/server/index', '', '', 1, 0, 'C', '0', '0', 'monitor:server:list', 'server', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '服务监控菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '缓存监控菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (114, '缓存列表', 2, 6, 'cacheList', 'monitor/cache/list', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis-list', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '缓存列表菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (115, '表单构建', 3, 1, 'build', 'tool/build/index', '', '', 1, 0, 'C', '0', '0', 'tool:build:list', 'build', 'admin', '2026-08-19 18:01:45', '', null, '表单构建菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (116, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 'admin', '2026-08-19 18:01:45', '', null, '代码生成菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (117, '接口文档', 3, 3, 'swagger', 'tool/swagger/index', '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list', 'swagger', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '系统接口菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '操作日志菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '登录日志菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1000, '用户查询', 100, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1001, '用户新增', 100, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1002, '用户修改', 100, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1003, '用户删除', 100, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1004, '用户导出', 100, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1005, '用户导入', 100, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1006, '重置密码', 100, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1007, '角色查询', 101, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1008, '角色新增', 101, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1009, '角色修改', 101, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1010, '角色删除', 101, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1011, '角色导出', 101, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1012, '菜单查询', 102, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1013, '菜单新增', 102, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1014, '菜单修改', 102, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1015, '菜单删除', 102, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1016, '部门查询', 103, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1017, '部门新增', 103, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1018, '部门修改', 103, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1019, '部门删除', 103, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1020, '岗位查询', 104, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1021, '岗位新增', 104, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1022, '岗位修改', 104, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1023, '岗位删除', 104, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1024, '岗位导出', 104, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1025, '字典查询', 105, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1026, '字典新增', 105, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1027, '字典修改', 105, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1028, '字典删除', 105, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1029, '字典导出', 105, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1030, '参数查询', 106, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1031, '参数新增', 106, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1032, '参数修改', 106, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1033, '参数删除', 106, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1034, '参数导出', 106, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1039, '操作查询', 500, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1040, '操作删除', 500, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1041, '日志导出', 500, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1042, '登录查询', 501, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1043, '登录删除', 501, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1044, '日志导出', 501, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1045, '账号解锁', 501, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1046, '在线查询', 109, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1047, '批量强退', 109, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1048, '单条强退', 109, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1049, '任务查询', 110, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1050, '任务新增', 110, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1051, '任务修改', 110, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1052, '任务删除', 110, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1053, '状态修改', 110, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1054, '任务导出', 110, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1055, '生成查询', 116, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query', '#', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:46', '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1056, '生成修改', 116, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit', '#', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:46', '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1057, '生成删除', 116, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove', '#', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:46', '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1058, '导入代码', 116, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import', '#', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:46', '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1059, '预览代码', 116, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:46', '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1060, '生成代码', 116, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code', '#', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:46', '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5000, 'AI 能力', 0, 6, 'ai', null, null, '', 1, 0, 'M', '0', '0', null, 'chart', 'admin', '2026-08-19 18:01:40', '', null, '智能AI目录', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5001, '智能体管理', 5000, 1, 'agent', 'ai/agent/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:agent:list', 'edit', 'admin', '2026-08-19 18:01:40', '', null, '智能体管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5002, '智能体查询', 5001, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:agent:query', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5003, '智能体新增', 5001, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:agent:add', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5004, '智能体修改', 5001, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:agent:edit', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5005, '智能体删除', 5001, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:agent:remove', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5006, '智能体测试', 5001, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:agent:test', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5007, '知识库管理', 5237, 1, 'knowledge-base', 'ai/knowledge-base/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:knowledge-base:list', 'documentation', 'admin', '2026-08-19 18:01:40', '', null, '知识库管理菜单 [v7.16 已整合到知识中心 Tab 容器]', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5008, '知识库查询', 5007, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:query', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5009, '知识库新增', 5007, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:add', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5010, '知识库修改', 5007, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:edit', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5011, '知识库删除', 5007, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:remove', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5012, '文档上传', 5007, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:upload', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5013, '知识文库', 5237, 2, 'knowledge-library', 'ai/knowledge-library/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:knowledge-library:list', 'tree-table', 'admin', '2026-08-19 18:01:40', '', null, '知识文库菜单 [v7.16 已整合到知识中心 Tab 容器]', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5014, '文库查询', 5013, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:query', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5015, '文库新增', 5013, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:add', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5016, '文库修改', 5013, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:edit', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5017, '文库删除', 5013, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:remove', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5018, '模型配置', 5238, 1, 'model-config', 'ai/model-config/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:model-config:list', 'monitor', 'admin', '2026-08-19 18:01:40', '', null, '模型配置菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5019, '模型查询', 5018, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:model-config:query', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5020, '模型新增', 5018, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:model-config:add', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5021, '模型修改', 5018, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:model-config:edit', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5022, '模型删除', 5018, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:model-config:remove', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5023, '连接测试', 5018, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:model-config:test', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5024, '工具管理', 5238, 2, 'tool', 'ai/tool/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:tool:list', 'tool', 'admin', '2026-08-19 18:01:40', '', null, '工具管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5025, '工具查询', 5024, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:tool:query', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5026, '工具新增', 5024, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:tool:add', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5027, '工具修改', 5024, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:tool:edit', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5028, '工具删除', 5024, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:tool:remove', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5029, '工作流管理', 5000, 3, 'workflow', 'ai/workflow/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:workflow:list', 'chart', 'admin', '2026-08-19 18:01:40', '', null, '工作流管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5030, '工作流查询', 5029, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:workflow:query', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5031, '工作流新增', 5029, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:workflow:add', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5032, '工作流修改', 5029, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:workflow:edit', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5033, '工作流删除', 5029, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:workflow:remove', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5034, '工作流执行', 5029, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:workflow:execute', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5035, 'AI生成工作流', 5029, 6, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:workflow-generator:generate', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5036, '领域词典', 5000, 5, 'dictionary', 'ai/dictionary/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:domain-dictionary:list', 'dict', 'admin', '2026-08-19 18:01:40', '', null, '领域词典菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5037, '词典查询', 5036, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:query', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5038, '词典新增', 5036, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:add', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5039, '词典修改', 5036, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:edit', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5040, '词典删除', 5036, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:remove', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5041, '数据源管理', 5238, 3, 'datasource', 'ai/datasource/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:datasource:list', 'druid', 'admin', '2026-08-19 18:01:40', '', null, '数据源管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5042, '数据源查询', 5041, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:datasource:query', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5043, '数据源新增', 5041, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:datasource:add', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5044, '数据源修改', 5041, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:datasource:edit', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5045, '数据源删除', 5041, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:datasource:remove', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5046, '连接测试', 5041, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:datasource:test', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5047, '元数据同步', 5041, 6, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:datasource:sync', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5048, 'Token统计', 5239, 2, 'token-usage', 'ai/token-usage/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:token-usage:list', 'money', 'admin', '2026-08-19 18:01:40', '', null, 'Token统计菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5049, '统计查询', 5048, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:token-usage:query', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5050, '统计导出', 5048, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:token-usage:export', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5051, 'AI数据分析', 5000, 6, 'query', 'ai/query/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:data-analysis:list', 'icon', 'admin', '2026-08-19 18:01:40', '', null, '智能数据分析菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5052, '查询查询', 5051, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:data-analysis:query', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5053, 'SQL生成', 5051, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:data-analysis:sql', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5054, '报告生成', 5051, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:data-analysis:report', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5055, '图表生成', 5051, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:diagram:generate', '#', 'admin', '2026-08-19 18:01:40', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5056, '概览大屏', 5239, 1, 'ai-dashboard', 'ai/dashboard/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:dashboard:list', 'chart', 'admin', '2026-08-19 18:01:40', '', null, 'AI数据大屏菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5057, '架构图生成', 5000, 7, 'diagram/chat', 'ai/diagram/chat', null, '', 1, 0, 'C', '0', '0', 'cms:ai:diagram:list', 'build', 'admin', '2026-08-19 18:01:40', '', null, 'AI架构图生成菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5058, '智能对话使用', 5001, 10, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:chat:list', '#', 'admin', '2026-08-19 18:01:40', '', null, 'AI对话/中断/重新生成接口权限', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5059, '话题管理', 5068, 14, 'topic', 'cms/topic/index', null, '', 1, 0, 'C', '0', '0', 'cms:topic:list', 'message', 'admin', '2026-08-19 18:01:41', 'admin', '2026-08-20 13:39:28', '话题列表与状态管理', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5060, '话题查询', 5059, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'cms:topic:query', '#', 'admin', '2026-08-19 18:01:41', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5063, '敏感词管理', 1, 6, 'sensitiveWord', 'system/sensitiveWord/index', null, '', 1, 0, 'C', '0', '0', 'system:sensitiveWord:list', 'dict', 'admin', '2026-08-19 18:01:41', 'admin', '2026-08-20 13:34:56', '敏感词库维护与词树刷新', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5064, '敏感词查询', 5063, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:query', '#', 'admin', '2026-08-19 18:01:41', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5065, '敏感词新增', 5063, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:add', '#', 'admin', '2026-08-19 18:01:41', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5066, '敏感词修改', 5063, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:edit', '#', 'admin', '2026-08-19 18:01:41', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5067, '敏感词删除', 5063, 4, '#', '', null, '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:remove', '#', 'admin', '2026-08-19 18:01:41', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5068, '内容管理', 5241, 1, 'cms', null, null, '', 1, 0, 'M', '0', '0', null, 'documentation', 'admin', '2026-08-19 18:01:45', '', null, '内容管理目录', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5069, '门户用户', 5068, 1, 'portal-user', 'cms/user/index', null, '', 1, 0, 'C', '0', '0', 'cms:user:list', 'user', 'admin', '2026-08-19 18:01:45', '', null, '门户用户管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5070, '用户查询', 5069, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:user:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5071, '用户新增', 5069, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:user:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5072, '用户修改', 5069, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:user:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5073, '用户删除', 5069, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:user:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5074, '用户状态', 5069, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:user:status', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5075, '重置密码', 5069, 6, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:user:resetPwd', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5076, '绑定系统用户', 5069, 7, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:user:bind', '#', 'admin', '2026-08-19 18:01:45', '', null, '身份桥接：绑定/解绑后台系统用户', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5077, '文章管理', 5068, 2, 'article', 'cms/article/index', null, '', 1, 0, 'C', '0', '0', 'cms:article:list', 'edit', 'admin', '2026-08-19 18:01:45', '', null, '文章管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5078, '文章查询', 5077, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:article:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5079, '文章新增', 5077, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:article:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5080, '文章修改', 5077, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:article:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5081, '文章删除', 5077, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:article:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5082, '文章审核', 5077, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:article:audit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5083, '文章上架', 5077, 6, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:article:publish', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5084, '文章推荐', 5077, 7, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:article:featured', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5085, '分类管理', 5068, 3, '/category', 'cms/category/index', null, '', 1, 0, 'C', '0', '0', 'cms:category:list', 'tree', 'admin', '2026-08-19 18:01:45', '', null, '分类管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5086, '分类查询', 5085, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:category:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5087, '分类新增', 5085, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:category:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5088, '分类修改', 5085, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:category:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5089, '分类删除', 5085, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:category:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5090, '标签管理', 5068, 4, 'tag', 'cms/tag/index', null, '', 1, 0, 'C', '0', '0', 'cms:tag:list', 'tab', 'admin', '2026-08-19 18:01:45', '', null, '标签管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5091, '标签查询', 5090, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:tag:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5092, '标签新增', 5090, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:tag:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5093, '标签修改', 5090, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:tag:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5094, '标签删除', 5090, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:tag:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5095, '评论管理', 5068, 5, 'comment', 'cms/comment/index', null, '', 1, 0, 'C', '0', '0', 'cms:comment:list', 'message', 'admin', '2026-08-19 18:01:45', '', null, '评论管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5096, '评论查询', 5095, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:comment:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5097, '评论审核', 5095, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:comment:audit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5098, '评论删除', 5095, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:comment:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5099, '友情链接', 5068, 7, 'friend-link', 'cms/friend-link/index', null, '', 1, 0, 'C', '0', '0', 'cms:friend-link:list', 'link', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '友情链接管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5100, '友情链接查询', 5099, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:friend-link:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5101, '友情链接新增', 5099, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:friend-link:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5102, '友情链接修改', 5099, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:friend-link:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5103, '友情链接删除', 5099, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:friend-link:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5104, '帮助分类', 5068, 8, 'help-category', 'cms/help-category/index', null, '', 1, 0, 'C', '0', '0', 'cms:help-category:list', 'tree', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '帮助中心分类管理', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5105, '分类查询', 5104, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:help-category:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5106, '分类新增', 5104, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:help-category:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5107, '分类修改', 5104, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:help-category:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5108, '分类删除', 5104, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:help-category:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5109, '帮助文章', 5068, 9, 'help-article', 'cms/help-article/index', null, '', 1, 0, 'C', '0', '0', 'cms:help-article:list', 'documentation', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '帮助中心文章管理', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5110, '文章查询', 5109, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:help-article:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5111, '文章新增', 5109, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:help-article:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5112, '文章修改', 5109, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:help-article:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5113, '文章删除', 5109, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:help-article:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5114, '举报管理', 5068, 10, 'report', 'cms/report/index', null, '', 1, 0, 'C', '0', '0', 'cms:report:list', 'warning', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '用户举报记录管理', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5115, '举报查询', 5114, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:report:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5116, '处理举报', 5114, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:report:handle', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5117, '删除举报', 5114, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:report:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5118, '反馈管理', 5068, 11, 'feedback', 'cms/feedback/index', null, '', 1, 0, 'C', '0', '0', 'cms:feedback:list', 'message', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:45', '用户意见反馈管理', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5119, '反馈查询', 5118, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:feedback:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5120, '处理反馈', 5118, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:feedback:handle', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5121, '删除反馈', 5118, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:feedback:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5122, '专栏管理', 5068, 12, 'column', 'cms/column/index', null, '', 1, 0, 'C', '0', '0', 'portal:column:list', 'documentation', 'admin', '2026-08-19 18:01:45', '', null, '专栏后台管理菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5123, '专栏查询', 5122, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'portal:column:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5124, '专栏新增', 5122, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'portal:column:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5125, '专栏修改', 5122, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'portal:column:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5126, '专栏删除', 5122, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'portal:column:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5127, '创作者认证', 1, 11, 'certification', null, null, '', 1, 0, 'M', '0', '0', null, 'user', 'admin', '2026-08-19 18:01:45', '', null, '创作者认证目录', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5128, '认证审核', 5127, 1, 'audit', 'cms/certification/index', null, '', 1, 0, 'C', '0', '0', 'cms:certification:audit', 'edit', 'admin', '2026-08-19 18:01:45', '', null, '创作者认证审核菜单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5129, '认证查询', 5128, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:certification:list', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5130, '认证审核', 5128, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:certification:audit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5132, '付费订单Tab查询', 5148, 10, '', null, null, '', 1, 0, 'F', '0', '0', 'portal:order:list', 'shopping', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:46', 'v9.5: 交易管理Tab-付费订单组件查询权限', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5133, '付费订单Tab详情', 5148, 11, '', null, null, '', 1, 0, 'F', '0', '0', 'portal:order:query', '#', 'admin', '2026-08-19 18:01:45', 'admin', '2026-08-19 18:01:46', 'v9.5: 交易管理Tab-付费订单组件详情权限', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5134, '消息中心', 1, 20, 'message', 'system/message/index', null, '', 1, 0, 'C', '0', '0', 'system:message:list', 'message', 'admin', '2026-08-19 18:01:45', '', null, '消息中心菜单（私信+通知双Tab）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5135, '私信查询', 5134, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'system:message:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5136, '私信发送', 5134, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'system:message:send', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5137, '通知查询', 5134, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'system:notification:list', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5138, '通知管理', 1, 21, 'notification', 'system/notification/index', null, '', 1, 0, 'C', '0', '0', 'system:notification:list', 'email', 'admin', '2026-08-19 18:01:45', '', null, '通知管理菜单（台账）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5139, '通知查询', 5138, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'system:notification:query', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5140, '通知新增', 5138, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'system:notification:add', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5141, '通知修改', 5138, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'system:notification:edit', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5142, '通知删除', 5138, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'system:notification:remove', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5143, '发送广播通知', 5138, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'system:notification:sendAll', '#', 'admin', '2026-08-19 18:01:45', '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5144, '推广位管理', 5068, 20, 'promotion', 'cms/promotion/index', null, '', 1, 0, 'C', '0', '0', 'cms:promotion:list', 'component', 'admin', '2026-08-19 18:01:45', '', null, '广告位与友情链接合并管理（Tab）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5145, '用户反馈处理', 5068, 21, 'feedback-center', 'cms/feedback-center/index', null, '', 1, 0, 'C', '0', '0', 'cms:feedback-center:list', 'message', 'admin', '2026-08-19 18:01:45', '', null, '反馈与举报合并处理（Tab）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5146, '帮助中心', 5068, 22, 'help-center', 'cms/help-center/index', null, '', 1, 0, 'C', '0', '0', 'cms:help-center:list', 'question', 'admin', '2026-08-19 18:01:45', '', null, '帮助分类与文章合并管理（Tab）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5147, '成长配置', 5068, 23, 'growth-config', 'cms/growth-config/index', null, '', 1, 0, 'C', '0', '0', 'cms:growth-config:list', 'star', 'admin', '2026-08-19 18:01:45', '', null, '成长规则与成就合并配置（Tab）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5148, '交易管理', 5068, 24, 'transaction', 'cms/transaction/index', null, '', 1, 0, 'C', '0', '0', 'cms:transaction:list', 'money', 'admin', '2026-08-19 18:01:45', '', null, '订单与打赏合并查询（Tab，当前隐藏）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5149, '缓存管理', 2, 7, 'cache-manage', 'monitor/cache-manage/index', null, '', 1, 0, 'C', '0', '0', 'monitor:cache-manage:list', 'redis', 'admin', '2026-08-19 18:01:45', '', null, '缓存监控与列表合并管理（Tab）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5150, '日志审计', 108, 3, 'log-audit', 'monitor/log-audit/index', null, '', 1, 0, 'C', '0', '0', 'monitor:log-audit:list', 'log', 'admin', '2026-08-19 18:01:45', '', null, '操作日志与登录日志合并查询（Tab）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5152, '内容审核中心', 5068, 25, 'audit-center', 'cms/audit-center/index', null, '', 1, 0, 'C', '0', '0', 'cms:audit-center:list', 'check', 'admin', '2026-08-19 18:01:45', '', null, '文章/专栏/话题审核合并入口（Tab，嵌入模式）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5153, '服务监控', 2, 3, 'server-panel', 'monitor/server-panel/index', null, '', 1, 0, 'C', '0', '0', 'monitor:server-panel:list', 'monitor', 'admin', '2026-08-19 18:01:45', '', null, '服务器监控与数据监控(Druid)合并查看（Tab）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5192, '面试管理', 5241, 4, 'interview', '', null, '', 1, 0, 'M', '0', '0', null, 'guide', 'admin', '2026-08-19 18:01:46', '', null, '面试指南一级目录：题库/面经/简历/笔记 | V10.5: 降级为门户管理下二级目录，重命名为 面试管理', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5193, '题库资源', 5192, 1, 'questionTab', 'cms/interview/questionTab/index', null, '', 1, 0, 'C', '0', '0', 'cms:interview:list', 'tree-table', 'admin', '2026-08-19 18:01:46', '', null, '面试题库+分类+公司标签+简历模板 Tab 容器', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5194, '面经运营', 5192, 2, 'experienceTab', 'cms/interview/experienceTab/index', null, '', 1, 0, 'C', '0', '0', 'cms:interview:experience:list', 'edit', 'admin', '2026-08-19 18:01:46', '', null, '面经管理+评论管理 Tab 容器；审核入口在内容审核中心', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5195, '精选笔记', 5192, 3, 'submission', 'cms/interview/submission/index', null, '', 1, 0, 'C', '0', '0', 'cms:interview:submission:list', 'star', 'admin', '2026-08-19 18:01:46', '', null, '精选笔记采纳与取消', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5196, '题库查询', 5193, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5197, '题库新增', 5193, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5198, '题库修改', 5193, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', '2026-08-19 18:01:46', '', null, '含：审核/置顶/精选采纳等运营操作', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5199, '题库删除', 5193, 4, '#', '', null, '', 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5200, '面经查询', 5194, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5201, '面经修改', 5194, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', '2026-08-19 18:01:46', '', null, '含：审核/置顶/评论管理', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5202, '面经删除', 5194, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5203, '笔记查询', 5195, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5204, '笔记修改', 5195, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', '2026-08-19 18:01:46', '', null, '含：采纳/取消精选', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5205, '学习管理', 5241, 5, 'book', '', null, '', 1, 0, 'M', '0', '0', null, 'education', 'admin', '2026-08-19 18:01:46', '', null, '读书空间一级目录：书籍/书单/金句/学习 | V10.5: 降级为门户管理下二级目录，重命名为 学习管理', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5206, '书籍管理', 5205, 1, 'book-index', 'portal/book/index', null, '', 1, 0, 'C', '0', '0', 'portal:book:list', 'documentation', 'admin', '2026-08-19 18:01:46', '', null, '书籍CRUD + 章节导入向导（章节管理为隐藏子路由）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5207, '章节管理', 5205, 2, 'bookChapter', 'portal/bookChapter/index', null, '', 1, 0, 'C', '0', '0', 'portal:bookChapter:list', '#', 'admin', '2026-08-19 18:01:46', '', null, '书籍章节CRUD + 发布/批量导入（隐藏菜单，从书籍详情跳转）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5208, '书单&推荐位', 5205, 3, 'bookListTab', 'portal/bookListTab/index', null, '', 1, 0, 'C', '0', '0', 'portal:bookList:list', 'list', 'admin', '2026-08-19 18:01:46', '', null, '书单管理+推荐位管理 Tab 容器', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5209, '用户内容', 5205, 4, 'userContent', 'portal/userContent/index', null, '', 1, 0, 'C', '0', '0', 'portal:bookQuote:list', 'peoples', 'admin', '2026-08-19 18:01:46', '', null, '金句摘录+书架管理 Tab 容器', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5210, '学习辅助', 5205, 9, 'learn-aux', 'portal/learn-aux/index', null, '', 1, 0, 'C', '0', '0', 'portal:learn:list', 'skill', 'admin', '2026-08-19 18:01:46', 'admin', '2026-08-19 18:01:46', 'v9.5: 迁移至面试指南（错题本/学习计划数据源于题库刷题）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5211, '书籍查询', 5206, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:book:query', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5212, '书籍新增', 5206, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:book:add', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5213, '书籍修改', 5206, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:book:edit', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5214, '书籍删除', 5206, 4, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:book:remove', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5215, '章节查询', 5207, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookChapter:query', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5216, '章节新增', 5207, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookChapter:add', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5217, '章节修改', 5207, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookChapter:edit', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5218, '章节删除', 5207, 4, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookChapter:remove', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5219, '章节发布', 5207, 5, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookChapter:publish', '#', 'admin', '2026-08-19 18:01:46', '', null, '章节发布/撤回', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5220, '书单查询', 5208, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookList:query', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5221, '书单新增', 5208, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookList:add', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5222, '书单修改', 5208, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookList:edit', '#', 'admin', '2026-08-19 18:01:46', '', null, '含：管理书籍（增删排序）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5223, '书单删除', 5208, 4, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookList:remove', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5224, '推荐位列表', 5208, 5, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:list', '#', 'admin', '2026-08-19 18:01:46', '', null, 'Tab 内推荐位面板列表权限', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5225, '推荐位查询', 5208, 6, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:query', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5226, '推荐位新增', 5208, 7, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:add', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5227, '推荐位修改', 5208, 8, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:edit', '#', 'admin', '2026-08-19 18:01:46', '', null, '含：上下架/排序', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5228, '推荐位删除', 5208, 9, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:remove', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5229, '金句查询', 5209, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookQuote:query', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5230, '金句新增', 5209, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookQuote:add', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5231, '金句修改', 5209, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookQuote:edit', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5232, '金句删除', 5209, 4, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookQuote:remove', '#', 'admin', '2026-08-19 18:01:46', '', null, null, '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5233, '书架列表', 5209, 5, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookshelf:list', '#', 'admin', '2026-08-19 18:01:46', '', null, 'Tab 内书架面板列表权限', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5234, '书架移除', 5209, 6, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:bookshelf:remove', '#', 'admin', '2026-08-19 18:01:46', '', null, '移出书架', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5235, '学习计划查询', 5210, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:studyPlan:list', '#', 'admin', '2026-08-19 18:01:46', '', null, '学习计划只读列表', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5236, '错题本查询', 5210, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'portal:wrongQuestion:list', '#', 'admin', '2026-08-19 18:01:46', '', null, '错题本只读列表', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5237, '知识中心', 5000, 2, 'knowledge-center', 'ai/knowledge-center/index', null, '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', '2026-08-19 18:01:46', '', null, '知识中心目录（知识库管理+知识文库 Tab）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5238, 'AI基础配置', 5000, 4, 'ai-config', null, null, '', 1, 0, 'M', '0', '0', '', 'system', 'admin', '2026-08-19 18:01:46', '', null, 'AI基础配置目录（模型配置+工具管理+数据源管理）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5239, '运营监控', 5000, 9, 'ai-monitor', null, null, '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', '2026-08-19 18:01:46', '', null, '运营监控目录（概览大屏+Token统计）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5241, '门户管理', 0, 2, 'portal', null, null, '', 1, 0, 'M', '0', '0', '', 'job', 'admin', '2026-08-19 18:01:46', 'admin', '2026-08-20 14:04:35', 'V10.5: 门户管理一级目录（聚合内容/用户/审核/面试/学习）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5242, '审核中心', 5241, 3, 'audit-center', 'cms/audit-center/index', null, '', 1, 0, 'C', '0', '0', 'system:auditTask:list', 'eye-open', 'admin', '2026-08-19 18:01:46', 'admin', '2026-08-20 14:05:25', 'V10.5: 审核中心（统一审核入口，Tab 容器，权限 system:auditTask:*）', '0');


--
-- Table structure for table `sys_notification`
--

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

SELECT  * FROM sys_post;

INSERT INTO `moyun-db`.sys_post (post_id, post_code, post_name, post_sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, 'ceo', '董事长', 1, '0', 'admin', '2026-08-19 18:01:44', '', null, '', '0');
INSERT INTO `moyun-db`.sys_post (post_id, post_code, post_name, post_sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, 'se', '项目经理', 2, '0', 'admin', '2026-08-19 18:01:44', '', null, '', '0');
INSERT INTO `moyun-db`.sys_post (post_id, post_code, post_name, post_sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, 'hr', '人力资源', 3, '0', 'admin', '2026-08-19 18:01:44', '', null, '', '0');
INSERT INTO `moyun-db`.sys_post (post_id, post_code, post_name, post_sort, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (4, 'user', '普通员工', 4, '0', 'admin', '2026-08-19 18:01:44', '', null, '', '0');


--
-- Table structure for table `sys_role`
--

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

SELECT  * FROM sys_role;


INSERT INTO `moyun-db`.sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', '2026-08-19 18:01:44', '', null, '超级管理员');
INSERT INTO `moyun-db`.sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (2, '普通角色', 'common', 2, '2', 1, 1, '0', '0', 'admin', '2026-08-19 18:01:44', '', null, '普通角色');


--
-- Table structure for table `sys_role_dept`
--

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

SELECT  * FROM sys_role_dept;

INSERT INTO `moyun-db`.sys_role_dept (role_id, dept_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 100, 'admin', '2026-07-28 15:42:34', '', '2026-07-28 15:42:34', null);
INSERT INTO `moyun-db`.sys_role_dept (role_id, dept_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 101, 'admin', '2026-07-28 15:42:34', '', '2026-07-28 15:42:34', null);
INSERT INTO `moyun-db`.sys_role_dept (role_id, dept_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 105, 'admin', '2026-07-28 15:42:34', '', '2026-07-28 15:42:34', null);


--
-- Table structure for table `sys_role_menu`
--

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

SELECT  * FROM sys_role_menu;

INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 3, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 116, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 1055, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 1056, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 1057, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 1058, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 1059, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 1060, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5000, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5001, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5002, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5003, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5004, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5005, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5006, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5007, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5008, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5009, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5010, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5011, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5012, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5013, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5014, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5015, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5016, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5017, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5018, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5019, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5020, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5021, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5022, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5023, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5024, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5025, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5026, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5027, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5028, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5029, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5030, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5031, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5032, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5033, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5034, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5035, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5036, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5037, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5038, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5039, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5040, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5041, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5042, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5043, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5044, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5045, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5046, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5047, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5048, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5049, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5050, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5051, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5052, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5053, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5054, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5055, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5056, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5057, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5058, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5059, 'admin', '2026-08-19 18:01:41', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5060, 'admin', '2026-08-19 18:01:41', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5063, 'admin', '2026-08-19 18:01:41', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5064, 'admin', '2026-08-19 18:01:41', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5065, 'admin', '2026-08-19 18:01:41', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5066, 'admin', '2026-08-19 18:01:41', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5067, 'admin', '2026-08-19 18:01:41', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5068, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5069, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5070, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5071, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5072, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5073, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5074, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5075, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5076, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5077, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5078, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5079, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5080, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5081, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5082, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5083, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5084, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5085, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5086, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5087, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5088, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5089, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5090, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5091, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5092, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5093, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5094, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5095, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5096, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5097, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5098, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5099, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5100, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5101, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5102, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5103, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5104, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5105, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5106, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5107, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5108, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5109, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5110, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5111, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5112, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5113, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5114, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5115, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5116, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5117, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5118, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5119, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5120, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5121, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5122, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5123, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5124, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5125, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5126, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5127, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5128, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5129, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5130, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5132, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5133, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5134, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5135, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5136, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5137, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5138, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5139, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5140, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5141, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5142, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5143, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5192, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5193, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5194, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5195, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5196, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5197, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5198, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5199, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5200, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5201, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5202, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5203, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5204, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5205, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5206, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5207, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5208, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5209, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5210, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5211, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5212, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5213, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5214, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5215, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5216, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5217, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5218, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5219, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5220, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5221, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5222, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5223, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5224, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5225, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5226, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5227, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5228, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5229, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5230, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5231, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5232, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5233, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5234, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5235, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5236, 'admin', '2026-08-19 18:01:46', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5237, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5238, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 5239, '', null, '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 2, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 3, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 100, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 101, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 102, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 103, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 104, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 105, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 106, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 108, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 109, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 110, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 111, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 112, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 113, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 114, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 115, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 116, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 117, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 500, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 501, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1000, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1001, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1002, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1003, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1004, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1005, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1006, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1007, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1008, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1009, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1010, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1011, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1012, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1013, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1014, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1015, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1016, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1017, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1018, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1019, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1020, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1021, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1022, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1023, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1024, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1025, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1026, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1027, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1028, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1029, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1030, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1031, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1032, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1033, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1034, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1039, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1040, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1041, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1042, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1043, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1044, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1045, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1046, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1047, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1048, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1049, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1050, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1051, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1052, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1053, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1054, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1055, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1056, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1057, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1058, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1059, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 1060, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 5134, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 5135, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 5137, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 5138, 'admin', '2026-08-19 18:01:45', '', null, null);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 5139, 'admin', '2026-08-19 18:01:45', '', null, null);


--
-- Table structure for table `sys_sensitive_word`
--

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
SELECT * FROM `sys_sensitive_word`;

INSERT INTO `moyun-db`.sys_sensitive_word (id, word, category, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (1, '示例敏感词1', 'other', '0', 'admin', '2026-08-19 18:01:41', null, null, '示例词，生产环境请替换为真实词库', '0');
INSERT INTO `moyun-db`.sys_sensitive_word (id, word, category, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (2, '示例敏感词2', 'ad', '0', 'admin', '2026-08-19 18:01:41', null, null, '示例词，生产环境请替换为真实词库', '0');
INSERT INTO `moyun-db`.sys_sensitive_word (id, word, category, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (3, '示例-广告', 'ad', '0', 'admin', '2026-08-19 18:01:41', null, null, null, '0');
INSERT INTO `moyun-db`.sys_sensitive_word (id, word, category, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (4, '示例-辱骂', 'insult', '0', 'admin', '2026-08-19 18:01:41', null, null, null, '0');
INSERT INTO `moyun-db`.sys_sensitive_word (id, word, category, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5, '示例-色情', 'porn', '0', 'admin', '2026-08-19 18:01:41', null, null, null, '0');
INSERT INTO `moyun-db`.sys_sensitive_word (id, word, category, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (6, '示例-政治', 'politics', '0', 'admin', '2026-08-19 18:01:41', null, null, null, '0');
INSERT INTO `moyun-db`.sys_sensitive_word (id, word, category, status, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (7, '示例-其他', 'other', '0', 'admin', '2026-08-19 18:01:41', null, null, null, '0');


--
-- Table structure for table `sys_sensitive_word_log`
--

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

LOCK TABLES `sys_sensitive_word_log` WRITE;
/*!40000 ALTER TABLE `sys_sensitive_word_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_sensitive_word_log` ENABLE KEYS */;
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

SELECT * FROM sys_USER;

INSERT INTO `moyun-db`.sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark) VALUES (1, 103, 'admin', '若依', '00', 'ry@163.com', '15888888888', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-08-20 13:20:33', 'admin', '2026-08-19 18:01:44', '', '2026-08-20 13:20:33', '管理员');
INSERT INTO `moyun-db`.sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark) VALUES (2, 105, 'ry', '若依', '00', 'ry@qq.com', '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-08-19 18:01:44', 'admin', '2026-08-19 18:01:44', '', null, '测试员');

--
-- Table structure for table `sys_user_post`
--

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

SELECT * FROM sys_user_post;

INSERT INTO `moyun-db`.sys_user_post (user_id, post_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 1, 'admin', '2026-07-28 15:42:34', '', '2026-07-28 15:42:34', null);
INSERT INTO `moyun-db`.sys_user_post (user_id, post_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 2, 'admin', '2026-07-28 15:42:34', '', '2026-07-28 15:42:34', null);


--
-- Table structure for table `sys_user_role`
--

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

SELECT * FROM sys_user_role;

INSERT INTO `moyun-db`.sys_user_role (user_id, role_id, create_by, create_time, update_by, update_time, remark) VALUES (1, 1, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);
INSERT INTO `moyun-db`.sys_user_role (user_id, role_id, create_by, create_time, update_by, update_time, remark) VALUES (2, 2, 'admin', '2026-08-19 18:01:44', '', '2026-08-19 18:01:44', null);



-- Dump completed on 2026-08-20 14:35:02

-- ==================== 增量变更：简历优化重构——岗位目标/匹配报告/优化历史（2026-08-26） ====================
-- 设计文档：docs/简历编辑和优化模块重构设计-20260826.md
-- 链路：选岗位(填JD) → 选简历 → AI岗位匹配评分(存报告) → 深度优化(前后对比逐项采纳) → 预览微调 → 保存新版本 → 重新评分

-- 1) 岗位目标表：用户管理的目标岗位与 JD（岗位匹配评分核心输入）
CREATE TABLE `portal_resume_job_target` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位目标ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `position` varchar(100) NOT NULL COMMENT '目标岗位名称（如 Java开发工程师）',
  `company` varchar(100) DEFAULT NULL COMMENT '目标公司（选填）',
  `city` varchar(50) DEFAULT NULL COMMENT '期望城市（选填）',
  `job_type` varchar(20) DEFAULT NULL COMMENT '岗位类型（全职/兼职/实习）',
  `jd_text` text NOT NULL COMMENT '岗位描述/JD原文（匹配分析核心输入）',
  `jd_keywords` varchar(1000) DEFAULT NULL COMMENT 'AI提取的JD核心关键词（逗号分隔，冗余加速展示）',
  `is_default` tinyint(1) DEFAULT 0 COMMENT '是否默认岗位：0=否 1=是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历优化-岗位目标表';

-- 2) 岗位匹配报告表：每次匹配分析结果存档（可追溯历史评分）
CREATE TABLE `portal_resume_job_match` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `resume_id` bigint NOT NULL COMMENT '简历ID（portal_user_resume.id）',
  `job_target_id` bigint NOT NULL COMMENT '岗位目标ID',
  `match_score` int NOT NULL COMMENT '综合匹配度 0-100',
  `grade` varchar(20) DEFAULT NULL COMMENT '评级：excellent/good/medium/poor',
  `matched_keywords` varchar(1000) DEFAULT NULL COMMENT '已匹配关键词（逗号分隔）',
  `missing_keywords` varchar(1000) DEFAULT NULL COMMENT '缺失关键词（逗号分隔）',
  `dimensions` json DEFAULT NULL COMMENT '各维度评分明细（关键词/经验/技能/结构匹配）',
  `summary` text COMMENT 'AI分析总结',
  `ai_powered` tinyint(1) DEFAULT 0 COMMENT '是否LLM生成：0=规则 1=LLM',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历优化-岗位匹配报告表';

-- 3) 优化历史表：深度优化采纳后记录轨迹（版本对比数据）
CREATE TABLE `portal_resume_optimize_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '优化记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `resume_id` bigint NOT NULL COMMENT '被优化的简历ID',
  `from_resume_id` bigint NOT NULL COMMENT '优化前简历ID（同一简历冗余记录，便于追溯）',
  `job_target_id` bigint DEFAULT NULL COMMENT '关联岗位目标ID',
  `score_before` int DEFAULT NULL COMMENT '优化前评分',
  `score_after` int DEFAULT NULL COMMENT '优化后评分',
  `match_score_before` int DEFAULT NULL COMMENT '优化前匹配度',
  `match_score_after` int DEFAULT NULL COMMENT '优化后匹配度',
  `adopted_count` int DEFAULT 0 COMMENT '采纳建议数',
  `total_count` int DEFAULT 0 COMMENT '生成建议总数',
  `optimize_data` json DEFAULT NULL COMMENT '优化明细快照（逐项 original/optimized/status）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历优化-优化历史表';
