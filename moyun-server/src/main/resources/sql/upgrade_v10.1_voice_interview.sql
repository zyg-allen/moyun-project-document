-- ============================================================
-- upgrade_v10.1_voice_interview.sql
-- V10.1 语音面试官 MVP：2 表 + 菜单 + 权限 + ModelType 注释 + 3 类字典
--
-- 变更内容：
--   1. ai_model_config.model_type 注释补充 asr/tts（对齐 ModelType 枚举）
--   2. 字典：voice_interview_status / voice_interview_style / voice_interview_hint_level
--   3. CREATE TABLE portal_voice_interview（语音面试会话主表）
--   4. CREATE TABLE portal_voice_interview_qa（语音面试问答表，含追问链 parent_qa_id）
--   5. 菜单 + 权限：后台 / 内容管理 / 语音面试管理（只读复盘）
--
-- 兼容性：纯标准 MySQL 8.0 语法，无 DELIMITER / 存储过程，兼容 DataGrip/Navicat/DBeaver
-- 幂等性：CREATE TABLE IF NOT EXISTS / ALTER MODIFY / WHERE NOT EXISTS 全幂等
--
-- 前置依赖：init_v7.8.sql 已建表 ai_model_config / sys_dict_type / sys_dict_data / sys_menu
-- 执行顺序：init_v7.8.sql → upgrade_v9.* → upgrade_v10.0_* → upgrade_v10.1_voice_interview.sql
-- ============================================================

-- ============================================================
-- 1. ai_model_config.model_type 注释补充 asr/tts
-- ============================================================
ALTER TABLE `ai_model_config`
    MODIFY COLUMN `model_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'chat' COMMENT '模型类型(chat/embedding/multimodal/reranker/asr/tts)，asr=语音识别，tts=语音合成，V10.0 语音面试官使用';

-- ============================================================
-- 2. 字典：voice_interview_status 语音面试状态
--    对应语音面试官会话生命周期
-- ============================================================
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '语音面试状态', 'voice_interview_status', '0', 'admin', NOW(), 'v10.1: 语音面试官会话状态' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'voice_interview_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'待开始' AS dict_label,'idle' AS dict_value,'voice_interview_status' AS dict_type,'' AS css_class,'default' AS list_class,'Y' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'聆听中','listening','voice_interview_status','','primary','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'播报中','speaking','voice_interview_status','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'评分中','scoring','voice_interview_status','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 5,'已结束','done','voice_interview_status','','info','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- ============================================================
-- 3. 字典：voice_interview_style 面试官风格
--    控制面试官话术语气与追问力度
-- ============================================================
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '面试官风格', 'voice_interview_style', '0', 'admin', NOW(), 'v10.1: 面试官语气风格，影响话术与追问' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'voice_interview_style');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'专业' AS dict_label,'professional' AS dict_value,'voice_interview_style' AS dict_type,'' AS css_class,'primary' AS list_class,'Y' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'亲和','friendly','voice_interview_style','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'严格','strict','voice_interview_style','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- ============================================================
-- 4. 字典：voice_interview_hint_level 提示级别
--    对应 HintEngine 分级提示 1/2/3
-- ============================================================
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '提示级别', 'voice_interview_hint_level', '0', 'admin', NOW(), 'v10.1: HintEngine 分级提示，对应 HintVO.level' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'voice_interview_hint_level');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'切入点提示' AS dict_label,'1' AS dict_value,'voice_interview_hint_level' AS dict_type,'' AS css_class,'info' AS list_class,'Y' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'结构提示','2','voice_interview_hint_level','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'全量提示','3','voice_interview_hint_level','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- ============================================================
-- 验证：查询插入结果
-- ============================================================
-- SELECT dict_type, dict_label, dict_value FROM sys_dict_data WHERE dict_type LIKE 'voice_interview_%' ORDER BY dict_type, dict_sort;

-- ============================================================
-- 5. CREATE TABLE portal_voice_interview 语音面试会话主表
--    区别于 portal_mock_interview：支持回合状态机 + 配置化 + 报告 JSON
-- ============================================================
CREATE TABLE IF NOT EXISTS `portal_voice_interview` (
                                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                        `user_id` bigint NOT NULL COMMENT '面试用户ID',
                                                        `position` varchar(64) DEFAULT NULL COMMENT '面试岗位（如 后端开发/前端开发）',
                                                        `scene` varchar(64) DEFAULT NULL COMMENT '面试场景（如 算法/系统设计/项目深挖）',
                                                        `resume_id` bigint DEFAULT NULL COMMENT '简历ID（有简历时启用项目深挖题源，占40%配比）',
                                                        `status` varchar(16) NOT NULL DEFAULT 'in_progress' COMMENT '状态 in_progress/finished',
                                                        `style` varchar(20) DEFAULT 'professional' COMMENT '面试官风格 professional/friendly/strict（字典 voice_interview_style）',
                                                        `difficulty` varchar(20) DEFAULT 'medium' COMMENT '难度 easy/medium/hard',
                                                        `total_qa` int NOT NULL DEFAULT '0' COMMENT '主问题目总数（不含追问）',
                                                        `current_idx` int NOT NULL DEFAULT '0' COMMENT '当前主问题目序号（从0开始）',
                                                        `score` int DEFAULT NULL COMMENT '面试总分（0-100，结束时计算）',
                                                        `summary` text COMMENT 'AI 生成的面试总结',
                                                        `report` text COMMENT '报告 JSON（含维度分/亮点/薄弱点/逐题点评）',
                                                        `config_json` text COMMENT '配置 JSON（hintsEnabled/stuckThreshold/style/difficulty）',
                                                        `is_personalized` tinyint(1) DEFAULT '0' COMMENT '是否基于画像抽题（0随机 1画像驱动）',
                                                        `profile_snapshot` text COMMENT '抽题时的画像快照 JSON（含薄弱点列表）',
                                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                        `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                        `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                                        PRIMARY KEY (`id`),
                                                        KEY `idx_user_time` (`user_id`,`create_time`),
                                                        KEY `idx_status` (`status`),
                                                        KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='语音面试会话主表（V10.1）';

-- ============================================================
-- 6. CREATE TABLE portal_voice_interview_qa 语音面试问答表
--    含追问链 parent_qa_id + 转写可编辑标记 + 规则维度分
-- ============================================================
CREATE TABLE IF NOT EXISTS `portal_voice_interview_qa` (
                                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                           `interview_id` bigint NOT NULL COMMENT '面试会话ID',
                                                           `question_id` bigint DEFAULT NULL COMMENT '关联题目ID（portal_interview_question.id）',
                                                           `question_idx` int NOT NULL COMMENT '主问题目序号（从0开始，追问与主问共享序号）',
                                                           `parent_qa_id` bigint DEFAULT NULL COMMENT '追问父问答ID（NULL=主问，非NULL=追问）',
                                                           `question` varchar(1000) NOT NULL COMMENT '面试问题（快照自题目标题/追问生成）',
                                                           `user_answer` text COMMENT '用户回答（ASR 转写后可编辑）',
                                                           `transcription_edited` tinyint(1) DEFAULT '0' COMMENT '转写是否被用户编辑（0=原样 1=已编辑）',
                                                           `ai_feedback` text COMMENT 'AI 反馈（规则化生成）',
                                                           `speak_text` text COMMENT 'AI 面试官话术（TTS 播报内容，区别于 question 纯文本）',
                                                           `score` int DEFAULT NULL COMMENT '本题评分（0-100）',
                                                           `rule_dimensions_json` text COMMENT '规则维度分 JSON（6维对齐雷达图：{"relevance":80,"professionalism":70,"fluency":60,"interactivity":65,"confidence":70,"logic":75}）',
                                                           `hint_used` int DEFAULT '0' COMMENT '已使用提示次数（0~3）',
                                                           `latency_ms` int DEFAULT NULL COMMENT '答题耗时（毫秒，从题目展示到提交答案）',
                                                           `next_action` varchar(20) DEFAULT NULL COMMENT '下一步动作 followup/hint/next/report',
                                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                           `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（与主表 portal_voice_interview 对齐）',
                                                           `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                                           PRIMARY KEY (`id`),
                                                           KEY `idx_interview` (`interview_id`),
                                                           KEY `idx_question_idx` (`interview_id`,`question_idx`),
                                                           KEY `idx_parent` (`parent_qa_id`),
                                                           KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='语音面试问答表（V10.1，含追问链）';

-- ============================================================
-- 6.1 升级补列（已执行过旧版 CREATE 的环境，用以下 ALTER 补齐缺失列）
--     说明：两表基础字段统一为 id / create_time / update_time / del_flag
--     已通过 CREATE 获得完整列的新环境，ALTER 可能失败但无影响
-- ============================================================
-- portal_voice_interview_qa 补 update_time（V10.1 初版漏此字段，与主表对齐）
ALTER TABLE `portal_voice_interview_qa`
    ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（与主表对齐）'
        AFTER `create_time`;
-- 注：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS；若该列已存在执行会报错，忽略即可

-- ============================================================
-- 7. 菜单 + 权限：后台 / 内容管理 / 语音面试管理（只读复盘）
-- ============================================================
SELECT @cms_dir_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;

-- C 菜单：语音面试管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '语音面试', @cms_dir_id, 13, 'voice-interview', 'cms/voiceInterview/index', NULL, 1, 0, 'C', '0', '0', 'cms:voice:list', 'mic', 'admin', NOW(), 'v10.1: 语音面试复盘只读入口'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:voice:list' AND menu_type = 'C');
SELECT @voice_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:voice:list' AND menu_type = 'C' LIMIT 1;

-- F 按钮：查询 + 查看详情（只读，无增删改）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '语音面试查询', @voice_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:voice:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:voice:query' AND menu_type = 'F');

-- 验证5
SELECT '语音面试菜单' AS check_item,
       IF((SELECT COUNT(*) FROM sys_menu WHERE perms IN ('cms:voice:list','cms:voice:query')) = 2, 'OK - 菜单已注册', 'ERROR') AS result;
