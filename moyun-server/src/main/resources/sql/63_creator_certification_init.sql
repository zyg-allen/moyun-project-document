-- =====================================================
-- 创作者认证功能 DDL 脚本（任务 2.8）
-- 支持创作者认证申请、审核状态机（pending/approved/rejected）
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 03_portal_init.sql 之后执行
-- =====================================================

-- 创作者认证申请表
CREATE TABLE IF NOT EXISTS `portal_creator_certification` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT        NOT NULL                COMMENT '申请用户ID',
    `real_name`      VARCHAR(64)   NOT NULL                COMMENT '真实姓名',
    `cert_type`      VARCHAR(32)   NOT NULL                COMMENT '认证类型 identity/creator/expert',
    `cert_no`        VARCHAR(64)   DEFAULT NULL            COMMENT '证件号',
    `cert_image`     VARCHAR(500)  DEFAULT NULL            COMMENT '证件照URL',
    `intro`          TEXT                                  COMMENT '自我介绍',
    `works`          VARCHAR(500)  DEFAULT NULL            COMMENT '代表作链接',
    `status`         VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审核状态 pending/approved/rejected',
    `auditor_id`     BIGINT        DEFAULT NULL            COMMENT '审核人ID',
    `audit_remark`   VARCHAR(500)  DEFAULT NULL            COMMENT '审核备注',
    `created_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `audited_time`   DATETIME      DEFAULT NULL            COMMENT '审核时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作者认证';

-- =====================================================
-- 后台管理菜单初始化：创作者认证审核
-- 路径：/cms/certification，组件：cms/certification/index
-- 权限标识：cms:certification:audit
-- =====================================================

-- 0. 清理旧的菜单数据（避免重复执行时报错）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN ('创作者认证', '认证审核')
);
DELETE FROM `sys_menu` WHERE `menu_name` IN ('创作者认证', '认证审核');

-- 1. 插入一级菜单：创作者认证（一级目录，与"成长体系"同级）
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '创作者认证', 0, 14, 'certification', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'user', 'admin', NOW(), '', NULL, '创作者认证目录'
);

SET @cert_menu_id = LAST_INSERT_ID();

-- 2. 二级菜单：认证审核
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '认证审核', @cert_menu_id, 1, 'audit', 'cms/certification/index', NULL, 1, 0, 'C', '0', '0', 'cms:certification:audit', 'edit', 'admin', NOW(), '', NULL, '创作者认证审核菜单'
);

SET @cert_audit_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('认证查询', @cert_audit_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:certification:list', '#', 'admin', NOW(), '', NULL, ''),
('认证审核', @cert_audit_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:certification:audit', '#', 'admin', NOW(), '', NULL, '');

-- 3. 为管理员角色分配认证菜单权限
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN ('创作者认证', '认证审核')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN ('创作者认证', '认证审核'));

SELECT '创作者认证初始化脚本执行完成！' AS message;
