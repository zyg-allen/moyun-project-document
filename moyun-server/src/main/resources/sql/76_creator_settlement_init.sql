-- =====================================================
-- 创作者分成结算功能 DDL 脚本（任务 4.7）
-- 按月聚合打赏/付费阅读/专栏订阅收入，按平台抽成比例结算给创作者
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 58_tip_init.sql 之后执行
-- =====================================================

-- 创作者结算单表
CREATE TABLE IF NOT EXISTS `portal_creator_settlement` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `creator_id`         BIGINT        NOT NULL                COMMENT '创作者用户ID',
    `period`             VARCHAR(16)   NOT NULL                COMMENT '结算周期，格式 yyyy-MM，如 2026-07',
    `tip_income`         DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '打赏收入（当月已支付打赏总额）',
    `paid_read_income`   DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '付费阅读收入（当月已支付购买总额）',
    `column_income`      DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '专栏订阅收入（当月已支付订阅总额）',
    `total_income`       DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '总收入（三项之和）',
    `platform_fee`       DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '平台抽成（total_income * platform_fee_rate）',
    `creator_income`     DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '创作者实得（total_income - platform_fee）',
    `status`             VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '状态 pending/confirmed/paid',
    `paid_time`          DATETIME      DEFAULT NULL            COMMENT '打款时间',
    `create_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_creator_period` (`creator_id`, `period`),
    KEY `idx_creator` (`creator_id`),
    KEY `idx_period` (`period`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作者分成结算';

-- =====================================================
-- 后台管理菜单初始化：创作者分成结算
-- 路径：/portal/settlement，组件：portal/settlement/index
-- 权限标识：portal:settlement:list / portal:settlement:confirm / portal:settlement:pay
-- =====================================================

-- 0. 清理旧的菜单数据（避免重复执行时报错）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN ('创作者结算', '分成结算')
);
DELETE FROM `sys_menu` WHERE `menu_name` IN ('创作者结算', '分成结算');

-- 1. 插入一级菜单：创作者结算（一级目录）
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '创作者结算', 0, 15, 'portal-settlement', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'money', 'admin', NOW(), '', NULL, '创作者分成结算目录'
);

SET @settle_menu_id = LAST_INSERT_ID();

-- 2. 二级菜单：分成结算
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '分成结算', @settle_menu_id, 1, 'settlement', 'portal/settlement/index', NULL, 1, 0, 'C', '0', '0', 'portal:settlement:list', 'edit', 'admin', NOW(), '', NULL, '创作者分成结算列表与操作'
);

SET @settle_list_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('结算查询', @settle_list_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:settlement:list', '#', 'admin', NOW(), '', NULL, ''),
('结算确认', @settle_list_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:settlement:confirm', '#', 'admin', NOW(), '', NULL, ''),
('结算打款', @settle_list_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:settlement:pay', '#', 'admin', NOW(), '', NULL, ''),
('月度生成', @settle_list_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:settlement:generate', '#', 'admin', NOW(), '', NULL, '');

-- 3. 为管理员角色分配结算菜单权限
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN ('创作者结算', '分成结算')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN ('创作者结算', '分成结算'));

SELECT '创作者分成结算初始化脚本执行完成！' AS message;
