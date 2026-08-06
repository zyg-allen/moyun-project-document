-- =====================================================================
-- 墨韵·智库 v7.5 AI 模块升级脚本
-- 说明：新增 AI 智能体/知识库/工作流/数据分析 模块全部表结构
-- 执行前请确保数据库为 v6.8+ 版本
-- Date: 2026-01-23
-- =====================================================================

-- ----------------------------
-- Table structure for agent
-- ----------------------------
CREATE TABLE IF NOT EXISTS `agent`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '智能体名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '智能体描述',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '系统提示词',
  `knowledge_base_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联的知识库ID（多个用逗号分隔）',
  `knowledge_library_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '关联的知识库ID列表（JSON数组）',
  `knowledge_base_weights` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '知识库权重配置（JSON格式：{\"1\": 1.0, \"2\": 0.8}，权重范围0.1-1.0）',
  `model_config_id` bigint NULL DEFAULT NULL COMMENT '模型配置ID(关联model_config表,NULL则使用默认模型)',
  `model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'qwen-plus' COMMENT '模型名称',
  `temperature` double NULL DEFAULT 0.7 COMMENT '温度参数',
  `max_tokens` int NULL DEFAULT 2000 COMMENT '最大token数',
  `rag_min_score` double NULL DEFAULT NULL COMMENT 'RAG检索相似度阈值(0.5-1.0,推荐0.7-0.75,NULL则使用全局配置)',
  `rag_max_results` int NULL DEFAULT NULL COMMENT 'RAG检索最大结果数量(1-10,推荐3-5,NULL则使用全局配置)',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `welcome_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '开场白',
  `suggested_questions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '预设问题(JSON数组)',
  `show_citations` tinyint(1) NULL DEFAULT 1 COMMENT '是否显示引用来源',
  `max_history_turns` int NULL DEFAULT 10 COMMENT '最大历史轮数',
  `api_enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用API',
  `api_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API Key',
  `workflow_id` bigint NULL DEFAULT NULL COMMENT '关联工作流ID',
  `workflow_trigger_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'manual' COMMENT '工作流触发模式: manual/auto/keyword',
  `workflow_trigger_keywords` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '触发关键词(JSON数组)',
  `publish_enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否发布为应用',
  `publish_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发布访问Token',
  `publish_settings` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '发布设置(JSON)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `rag_recall_multiplier` double NULL DEFAULT NULL COMMENT '第一阶段召回倍数（1.5-3.0，推荐2.0，NULL时使用全局配置）',
  `rag_enable_hybrid_search` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用混合检索（向量+BM25）',
  `rag_enable_query_expansion` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用查询扩展',
  `rag_bm25_weight` double NULL DEFAULT 0.3 COMMENT 'BM25检索权重（0-1）',
  `rag_vector_weight` double NULL DEFAULT 0.7 COMMENT '向量检索权重（0-1）',
  `enable_self_reflection` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用自我反思',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_model_config_id`(`model_config_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent
-- ----------------------------

-- ----------------------------
-- Table structure for agent_dictionary_relation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `agent_dictionary_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `dictionary_id` bigint NOT NULL COMMENT '词典ID',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_dict`(`agent_id` ASC, `dictionary_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_dictionary_id`(`dictionary_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体词典关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent_dictionary_relation
-- ----------------------------

-- ----------------------------
-- Table structure for agent_tool
-- ----------------------------
CREATE TABLE IF NOT EXISTS `agent_tool`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具标识（英文）',
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '显示名称（中文）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具描述（给LLM理解用）',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'general' COMMENT '工具分类：general/information/utility/action/data',
  `tool_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具类型：builtin/http/database',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'fa-wrench' COMMENT '图标（FontAwesome）',
  `config` json NULL COMMENT '工具配置（API地址、认证信息等）',
  `parameters` json NOT NULL COMMENT '参数定义（JSON Schema格式）',
  `timeout_seconds` int NULL DEFAULT 30 COMMENT '超时时间（秒）',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `is_system` tinyint(1) NULL DEFAULT 0 COMMENT '是否系统内置',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体工具定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent_tool
-- ----------------------------
INSERT INTO `agent_tool` VALUES (1, 'current_time', '当前时间', '获取当前的日期和时间，可指定时区和格式', 'utility', 'builtin', 'fa-clock', NULL, '{\"type\": \"object\", \"required\": [], \"properties\": {\"format\": {\"type\": \"string\", \"default\": \"yyyy-MM-dd HH:mm:ss\", \"description\": \"时间格式，默认yyyy-MM-dd HH:mm:ss\"}, \"timezone\": {\"type\": \"string\", \"default\": \"Asia/Shanghai\", \"description\": \"时区，如Asia/Shanghai，默认北京时间\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `agent_tool` VALUES (2, 'calculator', '数学计算', '执行数学计算，支持加减乘除、幂运算、开方、三角函数等', 'utility', 'builtin', 'fa-calculator', NULL, '{\"type\": \"object\", \"required\": [\"expression\"], \"properties\": {\"expression\": {\"type\": \"string\", \"description\": \"数学表达式，如(1+2)*3、sqrt(16)、sin(30)\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `agent_tool` VALUES (3, 'weather_query', '天气查询', '查询指定城市的实时天气和未来天气预报，包括温度、湿度、风向、天气状况等', 'information', 'http', 'fa-cloud-sun', '{\"api_type\": \"seniverse\"}', '{\"type\": \"object\", \"required\": [\"city\"], \"properties\": {\"city\": {\"type\": \"string\", \"description\": \"城市名称，如北京、上海、广州\"}, \"days\": {\"type\": \"integer\", \"default\": 1, \"description\": \"预报天数1-7，默认1天\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `agent_tool` VALUES (4, 'web_search', '网络搜索', '搜索互联网获取最新信息，适用于查询新闻、事件、知识等实时内容', 'information', 'http', 'fa-search', '{\"api_type\": \"bing\"}', '{\"type\": \"object\", \"required\": [\"query\"], \"properties\": {\"count\": {\"type\": \"integer\", \"default\": 5, \"description\": \"返回结果数量，默认5条\"}, \"query\": {\"type\": \"string\", \"description\": \"搜索关键词\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `agent_tool` VALUES (5, 'url_reader', '网页读取', '读取指定URL的网页内容，提取主要文本信息', 'information', 'http', 'fa-globe', '{\"timeout\": 10}', '{\"type\": \"object\", \"required\": [\"url\"], \"properties\": {\"url\": {\"type\": \"string\", \"description\": \"要读取的网页URL\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `agent_tool` VALUES (6, 'translator', '文本翻译', '将文本翻译成指定语言，支持中英日韩等多种语言互译', 'utility', 'http', 'fa-language', '{\"api_type\": \"aliyun\"}', '{\"type\": \"object\", \"required\": [\"text\"], \"properties\": {\"to\": {\"type\": \"string\", \"default\": \"zh\", \"description\": \"目标语言代码，如zh/en/ja\"}, \"from\": {\"type\": \"string\", \"default\": \"auto\", \"description\": \"源语言代码，如zh/en/ja，可设为auto自动检测\"}, \"text\": {\"type\": \"string\", \"description\": \"要翻译的文本\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `agent_tool` VALUES (7, 'send_email', '发送邮件', '发送电子邮件到指定邮箱地址', 'action', 'builtin', 'fa-envelope', '{}', '{\"type\": \"object\", \"required\": [\"to\", \"subject\", \"content\"], \"properties\": {\"to\": {\"type\": \"string\", \"description\": \"收件人邮箱地址\"}, \"content\": {\"type\": \"string\", \"description\": \"邮件正文内容\"}, \"subject\": {\"type\": \"string\", \"description\": \"邮件主题\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');
INSERT INTO `agent_tool` VALUES (8, 'database_query', '数据库查询', '执行SQL查询获取业务数据，仅支持SELECT查询语句', 'data', 'database', 'fa-database', '{\"max_rows\": 100}', '{\"type\": \"object\", \"required\": [\"sql\"], \"properties\": {\"sql\": {\"type\": \"string\", \"description\": \"SQL查询语句，仅支持SELECT\"}, \"database\": {\"type\": \"string\", \"default\": \"default\", \"description\": \"数据库名称，默认使用配置的业务库\"}}}', 30, 1, 1, '2025-11-25 15:06:30', '2025-11-25 15:06:30');

-- ----------------------------
-- Table structure for agent_tool_relation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `agent_tool_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `tool_id` bigint NOT NULL COMMENT '工具ID',
  `custom_config` json NULL COMMENT '针对该智能体的自定义配置',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_tool`(`agent_id` ASC, `tool_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_tool_id`(`tool_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体工具关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent_tool_relation
-- ----------------------------

-- ----------------------------
-- Table structure for agent_workflow_relation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `agent_workflow_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_workflow`(`agent_id` ASC, `workflow_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '智能体-工作流关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent_workflow_relation
-- ----------------------------

-- ----------------------------
-- Table structure for analysis_report
-- ----------------------------
CREATE TABLE IF NOT EXISTS `analysis_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `report_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报告名称',
  `report_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'auto' COMMENT '报告类型: auto, custom, scheduled',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分析的表名',
  `analysis_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '分析配置(JSON格式)',
  `executive_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '执行摘要(AI生成)',
  `data_overview` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数据概览(JSON格式)',
  `analysis_results` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '分析结果(JSON格式)',
  `insights` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数据洞察(JSON格式)',
  `charts` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图表配置(JSON格式)',
  `conclusion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '结论与建议(AI生成)',
  `report_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'draft' COMMENT '报告状态: draft, completed, archived',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '导出文件路径',
  `generate_time` int NULL DEFAULT 0 COMMENT '生成耗时(秒)',
  `view_count` int NULL DEFAULT 0 COMMENT '查看次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_datasource_id`(`datasource_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_report_type`(`report_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分析报告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of analysis_report
-- ----------------------------

-- ----------------------------
-- Table structure for chart_recommendation_rule
-- ----------------------------
CREATE TABLE IF NOT EXISTS `chart_recommendation_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名称',
  `data_pattern` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据模式: time_series, distribution, category, correlation',
  `field_types` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '字段类型组合(JSON)',
  `data_characteristics` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数据特征条件(JSON)',
  `recommended_chart` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推荐图表类型',
  `priority` int NULL DEFAULT 50 COMMENT '优先级(0-100)',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推荐理由',
  `min_data_points` int NULL DEFAULT 0 COMMENT '最小数据点数',
  `max_data_points` int NULL DEFAULT 999999 COMMENT '最大数据点数',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_data_pattern`(`data_pattern` ASC) USING BTREE,
  INDEX `idx_priority`(`priority` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图表推荐规则表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chart_recommendation_rule
-- ----------------------------
INSERT INTO `chart_recommendation_rule` VALUES (1, '时间序列-折线图', 'time_series', NULL, NULL, 'line', 95, '时间趋势最适合用折线图展示', 2, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `chart_recommendation_rule` VALUES (2, '分类占比-饼图', 'category', NULL, NULL, 'pie', 85, '少量分类适合饼图', 2, 6, 1, '2025-11-29 14:21:54');
INSERT INTO `chart_recommendation_rule` VALUES (3, '分类对比-柱状图', 'category', NULL, NULL, 'bar', 90, '多分类对比适合柱状图', 3, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `chart_recommendation_rule` VALUES (4, '数值分布-直方图', 'distribution', NULL, NULL, 'histogram', 90, '数值分布最适合用直方图', 10, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `chart_recommendation_rule` VALUES (5, '排名-条形图', 'ranking', NULL, NULL, 'bar', 90, '排名对比适合条形图', 3, 50, 1, '2025-11-29 14:21:54');
INSERT INTO `chart_recommendation_rule` VALUES (6, '相关性-散点图', 'correlation', NULL, NULL, 'scatter', 85, '相关性分析适合散点图', 10, 999999, 1, '2025-11-29 14:21:54');
INSERT INTO `chart_recommendation_rule` VALUES (7, '多维对比-雷达图', 'multi_dimension', NULL, NULL, 'radar', 75, '多维度对比适合雷达图', 3, 8, 1, '2025-11-29 14:21:54');

-- ----------------------------
-- Table structure for chat_history
-- ----------------------------
CREATE TABLE IF NOT EXISTS `chat_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `user_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用户消息',
  `assistant_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '助手回复',
  `tokens_used` int NULL DEFAULT 0 COMMENT 'Token消耗',
  `retrieval_results` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '检索结果JSON',
  `retrieval_count` int NULL DEFAULT 0 COMMENT '检索命中数',
  `response_time` int NULL DEFAULT 0 COMMENT '响应时间(毫秒)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 129 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对话历史表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_history
-- ----------------------------

-- ----------------------------
-- Table structure for conversation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `conversation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '新对话' COMMENT '会话标题（自动生成或用户修改）',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户ID（预留字段，支持多用户）',
  `message_count` int NULL DEFAULT 0 COMMENT '消息数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '对话摘要',
  `summary_updated_at` datetime NULL DEFAULT NULL COMMENT '摘要更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_update_time`(`update_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 53 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对话会话表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of conversation
-- ----------------------------

-- ----------------------------
-- Table structure for conversation_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS `conversation_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色：user/assistant',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `reference_sources` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '参考来源（JSON格式）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `conversation_message_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 454 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对话消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of conversation_message
-- ----------------------------

-- ----------------------------
-- Table structure for data_insight
-- ----------------------------
CREATE TABLE IF NOT EXISTS `data_insight`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `query_id` bigint NULL DEFAULT NULL COMMENT '查询ID',
  `report_id` bigint NULL DEFAULT NULL COMMENT '报告ID',
  `insight_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '洞察类型: anomaly, trend, correlation, pattern',
  `severity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'medium' COMMENT '严重程度: low, medium, high',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '洞察标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '洞察描述',
  `affected_fields` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '影响的字段',
  `statistical_value` decimal(20, 4) NULL DEFAULT NULL COMMENT '统计值',
  `confidence` decimal(5, 4) NULL DEFAULT NULL COMMENT '置信度(0-1)',
  `actionable` tinyint(1) NULL DEFAULT 0 COMMENT '是否可执行',
  `recommendation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '建议措施',
  `is_acknowledged` tinyint(1) NULL DEFAULT 0 COMMENT '是否已确认',
  `acknowledged_by` bigint NULL DEFAULT NULL COMMENT '确认人ID',
  `acknowledged_time` datetime NULL DEFAULT NULL COMMENT '确认时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_datasource_id`(`datasource_id` ASC) USING BTREE,
  INDEX `idx_query_id`(`query_id` ASC) USING BTREE,
  INDEX `idx_report_id`(`report_id` ASC) USING BTREE,
  INDEX `idx_insight_type`(`insight_type` ASC) USING BTREE,
  INDEX `idx_severity`(`severity` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能洞察表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of data_insight
-- ----------------------------

-- ----------------------------
-- Table structure for datasource_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `datasource_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据源类型: mysql, elasticsearch, mongodb',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主机地址',
  `port` int NOT NULL COMMENT '端口号',
  `database_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据库名称',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码(加密存储)',
  `connection_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '额外连接参数(JSON格式)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `health_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'unknown' COMMENT '健康状态: healthy, unhealthy, unknown',
  `last_check_time` datetime NULL DEFAULT NULL COMMENT '最后检查时间',
  `create_user_id` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据源配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of datasource_config
-- ----------------------------

-- ----------------------------
-- Table structure for document_chunk_metadata
-- ----------------------------
CREATE TABLE IF NOT EXISTS `document_chunk_metadata`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分片ID',
  `segment_id` bigint NOT NULL COMMENT '文档分片ID（关联document_segment表）',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `chunk_index` int NOT NULL COMMENT '分片序号',
  `chunk_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分片文本内容',
  `chunk_length` int NOT NULL COMMENT '分片长度',
  `parent_chunk_id` bigint NULL DEFAULT NULL COMMENT '父分片ID（父子分段模式使用）',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '使用的嵌入模型',
  `vector_dimension` int NULL DEFAULT NULL COMMENT '向量维度',
  `original_length` int NULL DEFAULT NULL COMMENT '预处理前长度',
  `preprocessed` tinyint(1) NULL DEFAULT 0 COMMENT '是否经过预处理',
  `hit_count` int NULL DEFAULT 0 COMMENT '被检索命中次数',
  `last_hit_time` timestamp NULL DEFAULT NULL COMMENT '最后命中时间',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_segment_id`(`segment_id` ASC) USING BTREE,
  INDEX `idx_knowledge_id`(`knowledge_id` ASC) USING BTREE,
  INDEX `idx_parent_chunk`(`parent_chunk_id` ASC) USING BTREE,
  INDEX `idx_hit_count`(`hit_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文档分片元数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of document_chunk_metadata
-- ----------------------------

-- ----------------------------
-- Table structure for document_image
-- ----------------------------
CREATE TABLE IF NOT EXISTS `document_image`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '关联的知识库ID',
  `image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片文件路径',
  `page_number` int NULL DEFAULT NULL COMMENT '所在页码',
  `image_index` int NULL DEFAULT NULL COMMENT '图片在页面中的索引',
  `embedding_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量ID',
  `vector_dimension` int NULL DEFAULT NULL COMMENT '向量维度',
  `width` int NULL DEFAULT NULL COMMENT '图片宽度',
  `height` int NULL DEFAULT NULL COMMENT '图片高度',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图片内容描述（多模态模型生成）',
  `description_language` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'zh' COMMENT '描述语言(zh/en)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_base_id`(`knowledge_base_id` ASC) USING BTREE,
  INDEX `idx_embedding_id`(`embedding_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2592 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文档图片表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of document_image
-- ----------------------------

-- ----------------------------
-- Table structure for document_segment
-- ----------------------------
CREATE TABLE IF NOT EXISTS `document_segment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '关联的知识库ID',
  `segment_index` int NOT NULL COMMENT '分片索引（第几个分片）',
  `page_number` int NULL DEFAULT NULL COMMENT 'PDF页码',
  `line_start` int NULL DEFAULT NULL COMMENT '起始行号',
  `line_end` int NULL DEFAULT NULL COMMENT '结束行号',
  `char_start` int NULL DEFAULT NULL COMMENT '起始字符位置',
  `char_end` int NULL DEFAULT NULL COMMENT '结束字符位置',
  `chapter_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '章节标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分片内容',
  `content_length` int NULL DEFAULT NULL COMMENT '分片内容长度',
  `embedding_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量ID（在Pinecone中的ID）',
  `vector_dimension` int NULL DEFAULT NULL COMMENT '向量维度',
  `vector_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '向量数据（JSON格式）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_base_id`(`knowledge_base_id` ASC) USING BTREE,
  INDEX `idx_embedding_id`(`embedding_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4595 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文档分片表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of document_segment
-- ----------------------------

-- ----------------------------
-- Table structure for domain_dictionary
-- ----------------------------
CREATE TABLE IF NOT EXISTS `domain_dictionary`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '核心词',
  `related_terms` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '相关词列表（逗号分隔）',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'general' COMMENT '分类（服务器、架构、模型、通用等）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '词典说明',
  `is_global` tinyint(1) NULL DEFAULT 1 COMMENT '是否全局词典（全局词典默认对所有智能体生效）',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `priority` int NULL DEFAULT 0 COMMENT '优先级（数字越大优先级越高）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_keyword`(`keyword` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_global`(`is_global` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '领域词典表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of domain_dictionary
-- ----------------------------
INSERT INTO `domain_dictionary` VALUES (1, '服务器', 'cpu,gpu,npu,内存,存储,硬盘,系统盘,数据盘,鲲鹏,昇腾,算力,主机,机器,配置,规格', '硬件', '服务器相关术语', 0, 1, 10, '2025-11-24 13:47:33', '2025-11-24 13:56:57');
INSERT INTO `domain_dictionary` VALUES (2, '架构', '系统架构,技术架构,平台架构,设计,模块,组件,层次,结构,框架', '技术', '架构相关术语', 0, 1, 8, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `domain_dictionary` VALUES (3, '模型', '大模型,embedding,向量,llm,ai模型,算法,训练,推理', 'AI', '模型相关术语', 0, 1, 9, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `domain_dictionary` VALUES (4, '知识库', '文档,向量库,rag,检索,知识管理,知识图谱', 'AI', '知识库相关术语', 0, 1, 7, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `domain_dictionary` VALUES (5, '部署', '安装,配置,环境,运维,上线,发布', '运维', '部署相关术语', 0, 1, 6, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `domain_dictionary` VALUES (6, '性能', '速度,效率,吞吐量,延迟,响应时间,优化', '技术', '性能相关术语', 0, 1, 5, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `domain_dictionary` VALUES (7, '安全', '权限,认证,授权,加密,防护,隔离', '安全', '安全相关术语', 0, 1, 8, '2025-11-24 13:47:33', '2025-11-24 13:57:43');
INSERT INTO `domain_dictionary` VALUES (8, '数据库', 'MySQL,PostgreSQL,MongoDB,Redis,Oracle,SQL,NoSQL,索引,事务,主从,分库分表,读写分离', '技术', '数据库相关术语，包含关系型和非关系型数据库', 0, 1, 9, '2025-11-01 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `domain_dictionary` VALUES (9, '微服务', 'SpringCloud,Dubbo,gRPC,服务注册,服务发现,负载均衡,熔断,限流,网关,配置中心', '技术', '微服务架构相关术语', 0, 1, 8, '2025-11-04 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `domain_dictionary` VALUES (10, '容器', 'Docker,Kubernetes,K8s,Pod,容器编排,镜像,Harbor,Helm,Service,Deployment', '运维', '容器化和容器编排相关术语', 0, 1, 8, '2025-11-06 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `domain_dictionary` VALUES (11, '前端', 'Vue,React,Angular,JavaScript,TypeScript,CSS,HTML,Webpack,Vite,组件,路由,状态管理', '技术', '前端开发相关术语', 0, 1, 7, '2025-11-08 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `domain_dictionary` VALUES (12, '测试', '单元测试,集成测试,压力测试,自动化测试,测试用例,Bug,缺陷,回归测试,冒烟测试,UAT', '质量', '软件测试相关术语', 0, 1, 6, '2025-11-11 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `domain_dictionary` VALUES (13, 'DevOps', 'CI/CD,Jenkins,GitLab,流水线,自动化部署,监控,日志,告警,SRE,可观测性', '运维', 'DevOps和持续集成相关术语', 0, 1, 7, '2025-11-14 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `domain_dictionary` VALUES (14, '网络', 'TCP,UDP,HTTP,HTTPS,DNS,CDN,负载均衡,防火墙,VPN,代理,带宽,延迟', '基础设施', '网络通信相关术语', 0, 1, 6, '2025-11-16 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `domain_dictionary` VALUES (15, '产品', '需求,PRD,原型,用户故事,MVP,迭代,版本,上线,灰度,AB测试,用户体验,交互设计', '产品', '产品管理相关术语', 1, 1, 5, '2025-11-18 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `domain_dictionary` VALUES (16, '财务', '预算,成本,利润,营收,ROI,现金流,资产负债,损益表,审计,税务,发票,报销', '财务', '财务管理相关术语', 1, 1, 5, '2025-11-21 15:55:38', '2025-11-26 15:55:38');
INSERT INTO `domain_dictionary` VALUES (17, '人力资源', '招聘,面试,入职,离职,绩效,考核,薪酬,福利,培训,晋升,组织架构,人才盘点', '人力', '人力资源管理相关术语', 1, 1, 5, '2025-11-24 15:55:38', '2025-11-26 15:55:38');

-- ----------------------------
-- Table structure for knowledge_base
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_base`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `library_id` bigint NULL DEFAULT NULL COMMENT '所属知识库ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件路径',
  `pdf_file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'PDF文件路径（用于预览）',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件类型',
  `vector_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量ID（Pinecone中的ID）',
  `segment_count` int NULL DEFAULT NULL COMMENT '文档分段数量',
  `vector_dimension` int NULL DEFAULT NULL COMMENT '向量维度',
  `status` int NULL DEFAULT 0 COMMENT '处理状态：0-待处理，1-处理中，2-处理成功，3-处理失败',
  `processing_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '处理状态：pending(待配置), configured(已配置), processing(处理中), completed(已完成), failed(失败)',
  `config_completed` tinyint(1) NULL DEFAULT 0 COMMENT '是否完成配置',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `process_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库分组',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '知识库标签（JSON数组）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库描述',
  `usage_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `hit_count` int NULL DEFAULT 0 COMMENT '命中次数',
  `last_used_time` datetime NULL DEFAULT NULL COMMENT '最后使用时间',
  `parse_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文档解析方式: POI, PDFBox, Text',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件内容SHA-256哈希值（用于增量更新检测）',
  `last_processed_time` datetime NULL DEFAULT NULL COMMENT '上次处理时间',
  `need_reprocess` tinyint(1) NULL DEFAULT 0 COMMENT '是否需要重新处理',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_upload_time`(`upload_time` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_usage_count`(`usage_count` ASC) USING BTREE,
  INDEX `idx_last_used_time`(`last_used_time` ASC) USING BTREE,
  INDEX `idx_library_id`(`library_id` ASC) USING BTREE,
  INDEX `idx_kb_content_hash`(`content_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 134 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_base
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `segment_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'general' COMMENT '分段模式：general(通用), parent_child(父子分段)',
  `segment_separator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '\n\n' COMMENT '分段标识符',
  `segment_max_length` int NOT NULL DEFAULT 800 COMMENT '分段最大长度（字符数，800字符确保题库问答对完整，技术文档可用500，小说可用1500）',
  `segment_overlap_length` int NOT NULL DEFAULT 100 COMMENT '分段重叠长度（字符数，100字符保证上下文连贯性）',
  `chunking_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'fixed' COMMENT '分片策略: fixed(固定大小), adaptive(自适应), document_type(按文档类型)',
  `document_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'general' COMMENT '文档类型: general(通用), faq(问答), table(表格), code(代码), technical(技术文档)',
  `faq_chunk_size` int NULL DEFAULT 400 COMMENT 'FAQ分片大小(字符)',
  `table_chunk_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'by_row' COMMENT '表格分片策略: by_row(按行), by_table(整表), by_cell(按单元格)',
  `code_chunk_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'by_function' COMMENT '代码分片策略: by_function(按函数), by_class(按类), by_file(按文件)',
  `technical_chunk_size` int NULL DEFAULT 1200 COMMENT '技术文档分片大小(字符)',
  `enable_smart_boundary` tinyint(1) NULL DEFAULT 1 COMMENT '启用智能边界检测(避免切断句子)',
  `preprocess_replace_spaces` tinyint(1) NULL DEFAULT 1 COMMENT '替换连续空格、换行、制表符',
  `preprocess_remove_urls` tinyint(1) NULL DEFAULT 1 COMMENT '删除URL和邮箱地址',
  `preprocess_remove_extra_newlines` tinyint(1) NULL DEFAULT 1 COMMENT '删除多余换行',
  `index_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'high_quality' COMMENT '索引方式：high_quality(高质量), economy(经济)',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '嵌入模型名称',
  `retrieval_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'vector' COMMENT '检索模式：vector(向量), keyword(关键词), hybrid(混合)',
  `retrieval_top_k` int NULL DEFAULT 3 COMMENT '检索Top K数量',
  `rerank_enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用重排序',
  `rerank_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '重排序模型',
  `qa_mode` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用Q&A模式',
  `qa_extraction_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'Q&A提取提示词',
  `preprocess_remove_special_chars` tinyint(1) NULL DEFAULT 0 COMMENT '删除特殊字符',
  `preprocess_remove_table_desc` tinyint(1) NULL DEFAULT 0 COMMENT '删除表格描述',
  `preprocess_remove_header_footer` tinyint(1) NULL DEFAULT 0 COMMENT '删除页眉页脚',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_knowledge_id`(`knowledge_id` ASC) USING BTREE,
  INDEX `idx_segment_mode`(`segment_mode` ASC) USING BTREE,
  INDEX `idx_index_mode`(`index_mode` ASC) USING BTREE,
  INDEX `idx_chunking_strategy`(`chunking_strategy` ASC) USING BTREE,
  INDEX `idx_document_type`(`document_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 97 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库配置表 - 包含分片策略、文档类型识别、预处理规则等配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_config
-- ----------------------------
INSERT INTO `knowledge_config` VALUES (85, 122, 'general', '\n\n', 1024, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'high_quality', NULL, 'vector', 3, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 09:48:23', '2026-01-23 09:48:23');
INSERT INTO `knowledge_config` VALUES (86, 123, 'general', '\n\n', 1200, 200, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 0, 0, 'high_quality', NULL, 'vector', 8, 1, NULL, 0, NULL, 0, 0, 0, '2026-01-23 09:49:31', '2026-01-23 09:49:32');
INSERT INTO `knowledge_config` VALUES (87, 124, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 09:49:49', '2026-01-23 09:49:50');
INSERT INTO `knowledge_config` VALUES (88, 125, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 09:51:21', '2026-01-23 09:51:22');
INSERT INTO `knowledge_config` VALUES (89, 126, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:02:20', '2026-01-23 10:02:21');
INSERT INTO `knowledge_config` VALUES (90, 127, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:02:31', '2026-01-23 10:02:31');
INSERT INTO `knowledge_config` VALUES (91, 128, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:11:02', '2026-01-23 10:11:03');
INSERT INTO `knowledge_config` VALUES (92, 129, 'general', '\n\n', 1024, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'high_quality', NULL, 'vector', 3, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:11:24', '2026-01-23 10:11:25');
INSERT INTO `knowledge_config` VALUES (93, 130, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:12:00', '2026-01-23 10:12:01');
INSERT INTO `knowledge_config` VALUES (94, 131, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:15:54', '2026-01-23 10:15:55');
INSERT INTO `knowledge_config` VALUES (95, 132, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 10:53:04', '2026-01-23 10:53:05');
INSERT INTO `knowledge_config` VALUES (96, 133, 'general', '\n\n', 500, 50, 'fixed', 'general', 400, 'by_row', 'by_function', 1200, 1, 1, 1, 1, 'economy', NULL, 'vector', 5, 0, NULL, 0, NULL, 0, 0, 0, '2026-01-23 11:47:50', '2026-01-23 11:47:51');

-- ----------------------------
-- Table structure for knowledge_config_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_config_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `template_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模板描述',
  `template_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板类型：general(通用), technical(技术文档), legal(法律), medical(医疗)',
  `config_json` json NOT NULL COMMENT '配置JSON',
  `is_system` tinyint(1) NULL DEFAULT 0 COMMENT '是否系统预设模板',
  `use_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `is_recommended` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否推荐模板',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_template_type`(`template_type` ASC) USING BTREE,
  INDEX `idx_use_count`(`use_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库配置模板表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_config_template
-- ----------------------------
INSERT INTO `knowledge_config_template` VALUES (1, '标准文档', '适用于一般文档、技术手册等，平衡性能和准确度', 'general', '{\"indexMode\": \"high_quality\", \"segmentMode\": \"general\", \"rerankEnabled\": true, \"retrievalMode\": \"vector\", \"retrievalTopK\": 10, \"segmentMaxLength\": 800, \"preprocessRemoveUrls\": false, \"segmentOverlapLength\": 100, \"preprocessReplaceSpaces\": true, \"preprocessRemoveExtraNewlines\": true}', 1, 0, 1, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `knowledge_config_template` VALUES (2, '题库/QA精准模式', '适用于题库、问答对等短文本，确保每道题独立检索', 'general', '{\"indexMode\": \"high_quality\", \"segmentMode\": \"qa\", \"rerankEnabled\": true, \"retrievalMode\": \"vector\", \"retrievalTopK\": 15, \"segmentMaxLength\": 400, \"preprocessRemoveUrls\": true, \"segmentOverlapLength\": 50, \"preprocessReplaceSpaces\": true, \"preprocessRemoveExtraNewlines\": true}', 1, 0, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `knowledge_config_template` VALUES (3, '长文档深度模式', '适用于长篇文章、研究报告等，保留更多上下文', 'general', '{\"indexMode\": \"high_quality\", \"segmentMode\": \"general\", \"rerankEnabled\": true, \"retrievalMode\": \"vector\", \"retrievalTopK\": 8, \"segmentMaxLength\": 1200, \"preprocessRemoveUrls\": false, \"segmentOverlapLength\": 200, \"preprocessReplaceSpaces\": true, \"preprocessRemoveExtraNewlines\": false}', 1, 1, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `knowledge_config_template` VALUES (4, '代码技术文档', '适用于代码、API文档等技术内容', 'technical', '{\"indexMode\": \"high_quality\", \"segmentMode\": \"code\", \"rerankEnabled\": false, \"retrievalMode\": \"vector\", \"retrievalTopK\": 12, \"segmentMaxLength\": 600, \"preprocessRemoveUrls\": false, \"segmentOverlapLength\": 80, \"preprocessReplaceSpaces\": false, \"preprocessRemoveExtraNewlines\": false}', 1, 0, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');
INSERT INTO `knowledge_config_template` VALUES (5, '经济快速模式', '降低资源消耗，适合大批量文档或测试环境', 'general', '{\"indexMode\": \"economy\", \"segmentMode\": \"general\", \"rerankEnabled\": false, \"retrievalMode\": \"vector\", \"retrievalTopK\": 5, \"segmentMaxLength\": 500, \"preprocessRemoveUrls\": true, \"segmentOverlapLength\": 50, \"preprocessReplaceSpaces\": true, \"preprocessRemoveExtraNewlines\": true}', 1, 71, 0, '2025-11-23 20:25:42', '2025-11-23 20:25:42');

-- ----------------------------
-- Table structure for knowledge_library
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_library`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库描述',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '?' COMMENT '知识库图标',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库分类（如：技术文档、产品手册、FAQ等）',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '标签（JSON数组格式）',
  `document_count` int NULL DEFAULT 0 COMMENT '文档数量',
  `total_segments` int NULL DEFAULT 0 COMMENT '总分段数',
  `total_size` bigint NULL DEFAULT 0 COMMENT '总文件大小（字节）',
  `usage_count` int NULL DEFAULT 0 COMMENT '使用次数（被检索次数）',
  `hit_count` int NULL DEFAULT 0 COMMENT '命中次数',
  `last_used_time` datetime NULL DEFAULT NULL COMMENT '最后使用时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'active' COMMENT '状态：active(正常), disabled(禁用), archived(归档)',
  `is_public` tinyint(1) NULL DEFAULT 1 COMMENT '是否公开（预留多租户）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_usage_count`(`usage_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_library
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_library_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_library_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `library_id` bigint NOT NULL COMMENT '知识库ID',
  `segment_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'general' COMMENT '分段模式：general(通用), qa(问答), code(代码)',
  `segment_separator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '\n\n' COMMENT '分段标识符',
  `segment_max_length` int NOT NULL DEFAULT 800 COMMENT '分段最大长度',
  `segment_overlap_length` int NOT NULL DEFAULT 100 COMMENT '分段重叠长度',
  `preprocess_replace_spaces` tinyint(1) NULL DEFAULT 1 COMMENT '替换连续空格',
  `preprocess_remove_urls` tinyint(1) NULL DEFAULT 1 COMMENT '删除URL',
  `preprocess_remove_extra_newlines` tinyint(1) NULL DEFAULT 1 COMMENT '删除多余换行',
  `index_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'high_quality' COMMENT '索引模式：high_quality, economy',
  `embedding_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Embedding模型',
  `retrieval_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'hybrid' COMMENT '检索模式：vector, keyword, hybrid',
  `retrieval_top_k` int NULL DEFAULT 10 COMMENT '检索返回数量',
  `rerank_enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用Rerank',
  `rerank_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Rerank模型',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_library_id`(`library_id` ASC) USING BTREE,
  CONSTRAINT `fk_library_config` FOREIGN KEY (`library_id`) REFERENCES `knowledge_library` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_library_config
-- ----------------------------

-- ----------------------------
-- Table structure for model_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `model_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型提供商(openai/ollama/dashscope)',
  `model_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'chat' COMMENT '模型类型(chat/embedding/multimodal/reranker)',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型名称',
  `api_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API密钥',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API基础URL',
  `temperature` double NULL DEFAULT 0.7 COMMENT '温度参数(0-2)',
  `max_tokens` int NULL DEFAULT 2000 COMMENT '最大Token数',
  `timeout` int NULL DEFAULT 60 COMMENT '超时时间(秒)',
  `streaming_supported` tinyint(1) NULL DEFAULT 1 COMMENT '是否支持流式输出',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `is_default` tinyint(1) NULL DEFAULT 0 COMMENT '是否为默认模型',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注说明',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `input_price` decimal(10, 6) NULL DEFAULT 0.001000 COMMENT '输入价格（元/1000 tokens）',
  `output_price` decimal(10, 6) NULL DEFAULT 0.002000 COMMENT '输出价格（元/1000 tokens）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_provider`(`provider` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_is_default`(`is_default` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模型配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of model_config
-- ----------------------------
INSERT INTO `model_config` VALUES (10, '通义千问-多模态Embedding', 'dashscope', 'embedding', 'text-embedding-v3', NULL, NULL, NULL, NULL, 60, 0, 1, 1, '通义千问多模态 Embedding 模型，支持图片和文本的联合向量化，用于图文混合搜索', '2025-11-21 17:12:03', '2026-01-23 15:51:50', 0.000500, 0.000000);
INSERT INTO `model_config` VALUES (11, '通义千问-VL-Plus', 'dashscope', 'chat', 'qwen-vl-plus', NULL, NULL, 0.7, 2000, 60, 1, 1, 1, '通义千问视觉理解模型Plus版本，支持图片内容识别和描述，用于文档图片的多模态理解', '2025-11-22 12:16:42', '2026-01-23 15:51:50', 0.001000, 0.002000);
INSERT INTO `model_config` VALUES (15, 'Qwen3-Reranker', 'dashscope', 'reranker', 'qwen3-rerank', NULL, 'https://dashscope.aliyuncs.com/api/v1', NULL, NULL, 60, 0, 1, 1, 'Qwen3 重排序模型，用于提升检索结果的相关性排序，支持中英文等100+语言', '2026-01-22 15:34:36', '2026-01-23 15:51:50', 0.000100, 0.000000);

-- ----------------------------
-- Table structure for query_history
-- ----------------------------
CREATE TABLE IF NOT EXISTS `query_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话ID',
  `natural_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '自然语言查询',
  `generated_sql` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '生成的SQL语句',
  `query_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '查询类型: select, aggregate, join, analysis',
  `tables_involved` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '涉及的表(逗号分隔)',
  `result_count` int NULL DEFAULT 0 COMMENT '结果行数',
  `execution_time` int NULL DEFAULT 0 COMMENT '执行时间(毫秒)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'success' COMMENT '执行状态: success, failed, timeout',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `analysis_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分析类型: basic, trend, correlation, ranking',
  `chart_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图表类型',
  `has_insight` tinyint(1) NULL DEFAULT 0 COMMENT '是否生成洞察',
  `token_used` int NULL DEFAULT 0 COMMENT 'LLM消耗的Token数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_datasource_id`(`datasource_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '查询历史表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of query_history
-- ----------------------------

-- ----------------------------
-- Table structure for reference_feedback
-- ----------------------------
CREATE TABLE IF NOT EXISTS `reference_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint NULL DEFAULT NULL COMMENT '知识库ID',
  `file_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件名',
  `page_number` int NULL DEFAULT NULL COMMENT '页码',
  `segment_index` int NULL DEFAULT NULL COMMENT '分片索引',
  `user_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '用户查询',
  `rerank_score` double NULL DEFAULT NULL COMMENT '重排分数',
  `vector_score` double NULL DEFAULT NULL COMMENT '向量相似度',
  `feedback_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '反馈类型：accurate(准确), inaccurate(不准确)',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '智能体ID',
  `memory_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会话ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_base_id`(`knowledge_base_id` ASC) USING BTREE,
  INDEX `idx_feedback_type`(`feedback_type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '参考来源反馈表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of reference_feedback
-- ----------------------------

-- ----------------------------
-- Table structure for sql_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sql_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `natural_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '自然语言示例',
  `sql_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SQL模板',
  `query_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '查询类型',
  `complexity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'simple' COMMENT '复杂度: simple, medium, complex',
  `table_pattern` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表名模式',
  `usage_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `success_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '成功率(%)',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_query_type`(`query_type` ASC) USING BTREE,
  INDEX `idx_complexity`(`complexity` ASC) USING BTREE,
  INDEX `idx_usage_count`(`usage_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SQL模板表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sql_template
-- ----------------------------
INSERT INTO `sql_template` VALUES (1, '简单查询-TOP N', '查询销售额最高的10个产品', 'SELECT * FROM {table} ORDER BY {metric_field} DESC LIMIT {limit}', 'ranking', 'simple', NULL, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `sql_template` VALUES (2, '时间范围查询', '查询上个月的数据', 'SELECT * FROM {table} WHERE {time_field} >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH) AND {time_field} < CURDATE()', 'time_range', 'simple', NULL, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `sql_template` VALUES (3, '聚合统计', '统计每个类别的总数', 'SELECT {category_field}, COUNT(*) as count FROM {table} GROUP BY {category_field}', 'aggregate', 'simple', NULL, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `sql_template` VALUES (4, '平均值计算', '计算平均销售额', 'SELECT AVG({metric_field}) as avg_value FROM {table}', 'aggregate', 'simple', NULL, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');
INSERT INTO `sql_template` VALUES (5, '多条件筛选', '查询价格大于100且库存小于50的商品', 'SELECT * FROM {table} WHERE {field1} > {value1} AND {field2} < {value2}', 'filter', 'medium', NULL, 0, 0.00, 1, '2025-11-29 14:21:54', '2025-11-29 14:21:54');



-- ----------------------------
-- Table structure for table_metadata
-- ----------------------------
CREATE TABLE IF NOT EXISTS `table_metadata`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '表名',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表注释',
  `table_schema` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '表结构(JSON格式)',
  `column_count` int NULL DEFAULT 0 COMMENT '字段数量',
  `row_count` bigint NULL DEFAULT 0 COMMENT '行数(估算)',
  `data_size` bigint NULL DEFAULT 0 COMMENT '数据大小(字节)',
  `has_primary_key` tinyint(1) NULL DEFAULT 0 COMMENT '是否有主键',
  `has_time_field` tinyint(1) NULL DEFAULT 0 COMMENT '是否有时间字段',
  `time_field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '时间字段名',
  `numeric_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数值型字段列表(JSON)',
  `category_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '类别型字段列表(JSON)',
  `indexed_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '索引字段列表(JSON)',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后同步时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_datasource_table`(`datasource_id` ASC, `table_name` ASC) USING BTREE,
  INDEX `idx_datasource_id`(`datasource_id` ASC) USING BTREE,
  INDEX `idx_last_sync_time`(`last_sync_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '表元数据缓存表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of table_metadata
-- ----------------------------

-- ----------------------------
-- Table structure for token_usage_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `token_usage_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint NULL DEFAULT NULL COMMENT '会话ID',
  `message_id` bigint NULL DEFAULT NULL COMMENT '消息ID',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '智能体ID',
  `workflow_id` bigint NULL DEFAULT NULL COMMENT '工作流ID',
  `workflow_execution_id` bigint NULL DEFAULT NULL COMMENT '工作流执行ID',
  `workflow_node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工作流节点ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户ID',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名称',
  `model_provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型提供商',
  `input_tokens` int NULL DEFAULT 0 COMMENT '输入token数',
  `output_tokens` int NULL DEFAULT 0 COMMENT '输出token数',
  `total_tokens` int NULL DEFAULT 0 COMMENT '总token数',
  `cost` decimal(10, 6) NULL DEFAULT NULL COMMENT '费用（元）',
  `request_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求类型：chat/embedding_query/embedding_document/workflow_llm/workflow_classifier/workflow_extractor/workflow_question',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_model_name`(`model_name` ASC) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE,
  INDEX `idx_workflow_execution_id`(`workflow_execution_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 390 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Token使用记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of token_usage_log
-- ----------------------------

-- ----------------------------
-- Table structure for token_usage_summary
-- ----------------------------
CREATE TABLE IF NOT EXISTS `token_usage_summary`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '智能体ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户ID',
  `stat_date` date NULL DEFAULT NULL COMMENT '统计日期',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名称',
  `total_requests` int NULL DEFAULT 0 COMMENT '请求次数',
  `total_input_tokens` bigint NULL DEFAULT 0 COMMENT '总输入token',
  `total_output_tokens` bigint NULL DEFAULT 0 COMMENT '总输出token',
  `total_tokens` bigint NULL DEFAULT 0 COMMENT '总token',
  `total_cost` decimal(12, 6) NULL DEFAULT NULL COMMENT '总费用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_user_date_model`(`agent_id` ASC, `user_id` ASC, `stat_date` ASC, `model_name` ASC) USING BTREE,
  INDEX `idx_stat_date`(`stat_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Token使用统计汇总表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of token_usage_summary
-- ----------------------------

-- ----------------------------
-- Table structure for tool_call_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tool_call_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `conversation_id` bigint NULL DEFAULT NULL COMMENT '会话ID',
  `message_id` bigint NULL DEFAULT NULL COMMENT '消息ID',
  `agent_id` bigint NULL DEFAULT NULL COMMENT '智能体ID',
  `tool_id` bigint NULL DEFAULT NULL COMMENT '工具ID',
  `tool_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具名称',
  `input_params` json NULL COMMENT '输入参数',
  `output_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '输出结果',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'success' COMMENT '状态：pending/running/success/failed/timeout',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `duration_ms` int NULL DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_agent_id`(`agent_id` ASC) USING BTREE,
  INDEX `idx_tool_name`(`tool_name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工具调用日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tool_call_log
-- ----------------------------

-- ----------------------------
-- Table structure for workflow
-- ----------------------------
CREATE TABLE IF NOT EXISTS `workflow`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作流名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工作流描述',
  `graph_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工作流图定义(JSON)',
  `variables` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '全局变量定义(JSON)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'draft' COMMENT '状态: draft-草稿, published-已发布, disabled-已禁用',
  `version` int NULL DEFAULT 1 COMMENT '版本号',
  `enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作流定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of workflow
-- ----------------------------

-- ----------------------------
-- Table structure for workflow_execution
-- ----------------------------
CREATE TABLE IF NOT EXISTS `workflow_execution`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'running' COMMENT '执行状态: running-执行中, completed-已完成, failed-失败, cancelled-已取消',
  `input_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '输入参数(JSON)',
  `output_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '输出结果(JSON)',
  `execution_log` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '执行日志(JSON数组)',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `current_node_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当前执行到的节点ID',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作流执行记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of workflow_execution
-- ----------------------------

-- ----------------------------
-- Table structure for workflow_version
-- ----------------------------
CREATE TABLE IF NOT EXISTS `workflow_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `version` int NOT NULL COMMENT '版本号',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '版本描述',
  `graph_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工作流图数据快照(JSON)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE,
  INDEX `idx_version`(`version` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作流版本表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of workflow_version
-- ----------------------------

-- =====================================================================
-- AI 模块菜单权限数据（占位，完整菜单权限见 91_菜单权限_CMS.sql 补充）
-- =====================================================================
-- AI 模块菜单由 91_菜单权限_CMS.sql 或后续版本补充，本脚本仅包含表结构

-- =====================================================================
-- AI 模块菜单权限（墨韵 v7.5 新增）
-- 菜单 ID 起点：5000（与 RuoYi 1-1060 / CMS 2000+ / 其他模块错开）
-- =====================================================================

ALTER TABLE sys_menu AUTO_INCREMENT = 5000;

-- ---------------------------------------------------------------------
-- 一、智能AI（一级目录 M）
-- 统一一级菜单名"智能AI"，整合所有 AI 子模块
-- ---------------------------------------------------------------------
-- 兼容已部署环境：若存在旧名"AI智能中心"则改名为"智能AI"
UPDATE sys_menu SET menu_name = '智能AI', remark = '智能AI目录'
WHERE menu_name = 'AI智能中心' AND parent_id = 0;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能AI', 0, 11, 'ai', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'chart', 'admin', NOW(), '智能AI目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0);
SELECT @ai_parent_id := menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1;

-- ---------------------------------------------------------------------
-- 二、智能体管理（二级菜单 C + F 按钮权限）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体管理', @ai_parent_id, 1, 'agent', 'ai/agent/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:agent:list', 'edit', 'admin', NOW(), '智能体管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:list');
SELECT @agent_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:agent:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体查询', @agent_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:agent:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体新增', @agent_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:agent:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体修改', @agent_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:agent:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体删除', @agent_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:agent:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能体测试', @agent_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:agent:test', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:agent:test');

-- ---------------------------------------------------------------------
-- 三、知识库管理（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识库管理', @ai_parent_id, 2, 'knowledge-base', 'ai/knowledge-base/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:knowledge-base:list', 'documentation', 'admin', NOW(), '知识库管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:list');
SELECT @kb_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识库查询', @kb_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识库新增', @kb_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识库修改', @kb_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识库删除', @kb_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文档上传', @kb_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-base:upload', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-base:upload');

-- ---------------------------------------------------------------------
-- 四、知识文库（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识文库', @ai_parent_id, 3, 'knowledge-library', 'ai/knowledge-library/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:knowledge-library:list', 'tree-table', 'admin', NOW(), '知识文库菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:list');
SELECT @kbl_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文库查询', @kbl_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文库新增', @kbl_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文库修改', @kbl_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文库删除', @kbl_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:knowledge-library:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:knowledge-library:remove');

-- ---------------------------------------------------------------------
-- 五、模型配置（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模型配置', @ai_parent_id, 4, 'model-config', 'ai/model-config/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:model-config:list', 'monitor', 'admin', NOW(), '模型配置菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:list');
SELECT @model_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:model-config:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模型查询', @model_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:model-config:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模型新增', @model_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:model-config:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模型修改', @model_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:model-config:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模型删除', @model_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:model-config:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '连接测试', @model_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:model-config:test', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:model-config:test');

-- ---------------------------------------------------------------------
-- 六、工具管理（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工具管理', @ai_parent_id, 5, 'tool', 'ai/tool/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:tool:list', 'tool', 'admin', NOW(), '工具管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:tool:list');
SELECT @tool_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:tool:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工具查询', @tool_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:tool:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:tool:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工具新增', @tool_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:tool:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:tool:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工具修改', @tool_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:tool:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:tool:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工具删除', @tool_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:tool:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:tool:remove');

-- ---------------------------------------------------------------------
-- 七、工作流管理（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流管理', @ai_parent_id, 6, 'workflow', 'ai/workflow/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:workflow:list', 'chart', 'admin', NOW(), '工作流管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:list');
SELECT @wf_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:workflow:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流查询', @wf_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流新增', @wf_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流修改', @wf_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流删除', @wf_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '工作流执行', @wf_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow:execute', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow:execute');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'AI生成工作流', @wf_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:workflow-generator:generate', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:workflow-generator:generate');

-- ---------------------------------------------------------------------
-- 八、领域词典（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '领域词典', @ai_parent_id, 7, 'dictionary', 'ai/dictionary/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:domain-dictionary:list', 'dict', 'admin', NOW(), '领域词典菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:list');
SELECT @dict_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '词典查询', @dict_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '词典新增', @dict_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '词典修改', @dict_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '词典删除', @dict_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:domain-dictionary:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:domain-dictionary:remove');

-- ---------------------------------------------------------------------
-- 九、数据源管理（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据源管理', @ai_parent_id, 8, 'datasource', 'ai/datasource/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:datasource:list', 'druid', 'admin', NOW(), '数据源管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:list');
SELECT @ds_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:datasource:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据源查询', @ds_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据源新增', @ds_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据源修改', @ds_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据源删除', @ds_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '连接测试', @ds_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:test', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:test');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '元数据同步', @ds_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:datasource:sync', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:datasource:sync');

-- ---------------------------------------------------------------------
-- 十、Token统计（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Token统计', @ai_parent_id, 9, 'token-usage', 'ai/token-usage/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:token-usage:list', 'money', 'admin', NOW(), 'Token统计菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:token-usage:list');
SELECT @token_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:token-usage:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '统计查询', @token_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:token-usage:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:token-usage:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '统计导出', @token_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:token-usage:export', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:token-usage:export');

-- ---------------------------------------------------------------------
-- 十一、智能查询/数据分析（二级菜单 C + F）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据分析', @ai_parent_id, 10, 'query', 'ai/query/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:data-analysis:list', 'icon', 'admin', NOW(), '智能数据分析菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:data-analysis:list');
SELECT @da_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:ai:data-analysis:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询查询', @da_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:data-analysis:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:data-analysis:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'SQL生成', @da_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:data-analysis:sql', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:data-analysis:sql');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告生成', @da_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:data-analysis:report', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:data-analysis:report');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '图表生成', @da_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:diagram:generate', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:diagram:generate');

-- ---------------------------------------------------------------------
-- 十二、AI数据大屏（二级菜单 C）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'AI数据大屏', @ai_parent_id, 11, 'dashboard', 'ai/dashboard/index', NULL, 1, 0, 'C', '0', '0', 'cms:ai:dashboard:list', 'chart', 'admin', NOW(), 'AI数据大屏菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:dashboard:list');

-- ---------------------------------------------------------------------
-- 十三、AI图表对话（二级菜单 C）
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '架构图生成', @ai_parent_id, 12, 'diagram/chat', 'ai/diagram/chat', NULL, 1, 0, 'C', '0', '0', 'cms:ai:diagram:list', 'build', 'admin', NOW(), 'AI架构图生成菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:diagram:list');

-- ---------------------------------------------------------------------
-- 十四、超级管理员角色自动关联全部 AI 菜单（admin 角色 role_id=1）
-- ---------------------------------------------------------------------
-- 对 role_id=1 的超级管理员，自动授予所有以 cms:ai: 开头的权限串对应的菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms LIKE 'cms:ai:%'
  AND m.menu_type IN ('C', 'F')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
-- 同时授予一级目录 M 菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.menu_name = '智能AI' AND m.parent_id = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

-- ---------------------------------------------------------------------
-- 十五、AI 对话权限码补全（v7.5.1）
-- 后端 ChatController 的 /stream、/abort、/regenerate 接口需要 cms:ai:chat:list 权限。
-- 该权限串挂在"智能体管理"二级菜单（perms='cms:ai:agent:list'）下作为按钮权限 F。
-- ---------------------------------------------------------------------
SET @agent_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'cms:ai:agent:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能对话使用', @agent_menu_id, 10, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:ai:chat:list', '#', 'admin', NOW(), 'AI对话/中断/重新生成接口权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:ai:chat:list');

-- 超级管理员自动授权新增 chat 权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms = 'cms:ai:chat:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

-- =====================================================================
-- v7.5 AI 模块菜单权限 — 完成
-- =====================================================================
