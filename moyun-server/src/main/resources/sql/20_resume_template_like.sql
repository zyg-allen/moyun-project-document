-- =============================================
-- V20: 简历模板点赞表（修复点赞无防重缺陷）
-- @author moyun
-- =============================================

DROP TABLE IF EXISTS `portal_interview_resume_template_like`;
CREATE TABLE `portal_interview_resume_template_like` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id` BIGINT NOT NULL COMMENT '简历模板ID',
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_user` (`template_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历模板点赞表';
