-- =============================================
-- 墨韵智库 - 通用实体标签关联表 portal_entity_tag
-- 执行顺序: 31 (在 30_alter_portal_tag_fields.sql 之后执行)
-- 描述: 创建通用实体标签关联表，支持文章/面试题/面经/简历模板/书籍等多实体打标签。
--       配合 portal_tag.reference_count 实现"热门标签"排行。
-- 兼容: MySQL 8+
-- =============================================

-- ----------------------------
-- 通用实体标签关联表
-- ----------------------------
DROP TABLE IF EXISTS `portal_entity_tag`;
CREATE TABLE `portal_entity_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag_id` bigint unsigned NOT NULL COMMENT '标签ID（引用 portal_tag.id）',
  `entity_type` varchar(32) NOT NULL COMMENT '实体类型（article/interview_question/interview_experience/interview_resume_template/book 等）',
  `entity_id` bigint unsigned NOT NULL COMMENT '实体ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_entity` (`tag_id`, `entity_type`, `entity_id`),
  KEY `idx_entity` (`entity_type`, `entity_id`),
  KEY `idx_entity_create` (`entity_type`, `create_time`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通用实体标签关联表';

-- =============================================
-- 完成提示
-- =============================================
SELECT 'portal_entity_tag 表创建完成！' AS message;
