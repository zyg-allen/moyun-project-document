-- =============================================================================
-- 简历模块重构补丁（增量，对应设计文档 docs/简历编辑和优化模块重构设计-20260826.md 阶段一/阶段五）
-- 背景：
--   1. 阶段一模板套用打通：模板表无结构化内容字段，"基于此模板创建简历"只能预填标题，
--      无法填充教育/工作/项目等结构化数据 → 新增 sample_data JSON 字段（可选）
--   2. 阶段五评分报告存档：scoreResume 仅写 portal_user_resume.score/score_detail/scored_time，
--      不入历史 → 新建 portal_resume_score_report 表保存每次评分结果为可追溯报告
-- 幂等：DDL 用 IF NOT EXISTS / 列存在判断，可重复执行
-- =============================================================================

-- 1) 模板表增加结构化示例数据字段（幂等：先检查列是否存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_interview_resume_template' AND COLUMN_NAME = 'sample_data');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `portal_interview_resume_template` ADD COLUMN `sample_data` json DEFAULT NULL COMMENT ''模板结构化示例数据（JSON：name/phone/email/jobIntention/educations/works/projects/skills/selfIntro，用于一键套用填充编辑页）'' AFTER `tags`',
  'SELECT ''sample_data 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 评分报告表（幂等：IF NOT EXISTS）
CREATE TABLE IF NOT EXISTS `portal_resume_score_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `resume_id` bigint NOT NULL COMMENT '简历ID（portal_user_resume.id）',
  `job_target_id` bigint DEFAULT NULL COMMENT '关联岗位目标ID（可选，纯规则评分时为空）',
  `position_snapshot` varchar(100) DEFAULT NULL COMMENT '评分时的目标岗位快照（便于报告独立解读）',
  `score` int NOT NULL COMMENT '综合评分 0-100',
  `score_detail` text COMMENT '各维度评分明细 JSON（基本信息/求职意向/教育/工作/项目/技能/自我评价/岗位匹配度）',
  `source` varchar(20) DEFAULT 'manual' COMMENT '评分来源：manual 单独评分 / optimize 优化后重新评分 / template 模板套用评分',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历优化-评分报告存档表';
