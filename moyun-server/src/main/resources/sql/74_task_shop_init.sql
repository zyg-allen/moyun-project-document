-- =====================================================
-- 任务系统 + 积分商城 DDL 脚本（阶段四 任务 4.4）
-- 包含：portal_task、portal_user_task、portal_shop_item、portal_shop_exchange 4 张表
-- 扩展：portal_user_growth 新增 points 字段（积分余额，与 growth_value 解耦，避免兑换影响等级）
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 69_mock_interview_init.sql 之后执行
-- @author moyun
-- =====================================================

-- ----------------------------
-- 1. 任务定义表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_task` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`           VARCHAR(64)   NOT NULL                COMMENT '任务编码（唯一，用于埋点触发，如 daily_checkin）',
    `name`           VARCHAR(128)  NOT NULL                COMMENT '任务名称',
    `description`    VARCHAR(500)  DEFAULT NULL            COMMENT '任务描述',
    `task_type`      VARCHAR(32)   NOT NULL DEFAULT 'daily' COMMENT '任务类型 daily/once/achievement',
    `reward_points`  INT           NOT NULL DEFAULT 0      COMMENT '完成奖励积分',
    `target_count`   INT           NOT NULL DEFAULT 1      COMMENT '目标完成次数',
    `icon`           VARCHAR(500)  DEFAULT NULL            COMMENT '任务图标URL',
    `status`         VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态 active/inactive',
    `create_by`      VARCHAR(64)   DEFAULT ''              COMMENT '创建者',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(64)   DEFAULT ''              COMMENT '更新者',
    `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`         VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_type_status` (`task_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务定义表';

-- ----------------------------
-- 2. 用户任务进度表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_user_task` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT      NOT NULL                COMMENT '用户ID',
    `task_id`         BIGINT      NOT NULL                COMMENT '任务ID',
    `progress`        INT         NOT NULL DEFAULT 0      COMMENT '当前进度',
    `completed`       TINYINT     NOT NULL DEFAULT 0      COMMENT '是否已完成 0/1',
    `claimed`         TINYINT     NOT NULL DEFAULT 0      COMMENT '是否已领取奖励 0/1',
    `completed_time`  DATETIME    DEFAULT NULL            COMMENT '完成时间',
    `create_by`       VARCHAR(64) DEFAULT ''             COMMENT '创建者',
    `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64) DEFAULT ''             COMMENT '更新者',
    `update_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_task` (`user_id`, `task_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户任务进度表';

-- ----------------------------
-- 3. 积分商城商品表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_shop_item` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          VARCHAR(128)  NOT NULL                COMMENT '商品名称',
    `description`   VARCHAR(500)  DEFAULT NULL            COMMENT '商品描述',
    `cover`         VARCHAR(500)  DEFAULT NULL            COMMENT '商品封面URL',
    `type`          VARCHAR(32)   NOT NULL DEFAULT 'virtual' COMMENT '商品类型 virtual/physical',
    `points_cost`   INT           NOT NULL DEFAULT 0      COMMENT '兑换所需积分',
    `stock`         INT           NOT NULL DEFAULT 0      COMMENT '库存（-1表示不限）',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态 active/inactive',
    `create_by`     VARCHAR(64)   DEFAULT ''              COMMENT '创建者',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)   DEFAULT ''              COMMENT '更新者',
    `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`        VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分商城商品表';

-- ----------------------------
-- 4. 积分兑换记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_shop_exchange` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT        NOT NULL                COMMENT '兑换用户ID',
    `item_id`       BIGINT        NOT NULL                COMMENT '商品ID',
    `points_cost`   INT           NOT NULL                COMMENT '消耗积分（冗余，便于查询）',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '状态 pending/fulfilled/failed',
    `address`       VARCHAR(500)  DEFAULT NULL            COMMENT '收货地址（实物商品）',
    `exchange_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间',
    `create_by`     VARCHAR(64)   DEFAULT ''              COMMENT '创建者',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)   DEFAULT ''              COMMENT '更新者',
    `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`        VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_item` (`item_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分兑换记录表';

-- ----------------------------
-- 5. 扩展 portal_user_growth：新增 points 字段（积分余额）
-- 积分与成长值（growth_value）解耦：成长值只增不减用于等级，积分可消耗用于商城
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user_growth' AND COLUMN_NAME = 'points');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `portal_user_growth` ADD COLUMN `points` BIGINT NOT NULL DEFAULT 0 COMMENT ''积分余额（可消耗，与成长值解耦）'' AFTER `season_value`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================
-- 初始化数据：任务定义（与成长规则 action 对应，便于复用埋点）
-- =============================================
INSERT INTO `portal_task` (`code`, `name`, `description`, `task_type`, `reward_points`, `target_count`, `status`, `create_by`) VALUES
('daily_checkin',       '每日签到',   '每天签到一次，保持活跃',     'daily',      10,  1,  'active', 'admin'),
('daily_publish',       '每日发文',   '每日发布 1 篇文章',         'daily',      20,  1,  'active', 'admin'),
('daily_comment',       '每日互动',   '每日评论 3 次',             'daily',      15,  3,  'active', 'admin'),
('daily_like',          '每日点赞',   '每日点赞 5 次',             'daily',      10,  5,  'active', 'admin'),
('daily_solve',         '每日刷题',   '每日解答 3 道面试题',        'daily',      20,  3,  'active', 'admin'),
('first_article',       '初露锋芒',   '发布第一篇文章',            'achievement', 50,  1,  'active', 'admin'),
('solve_50',            '刷题能手',   '累计解答 50 道面试题',       'achievement', 200, 50, 'active', 'admin')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- =============================================
-- 初始化数据：积分商城商品
-- =============================================
INSERT INTO `portal_shop_item` (`name`, `description`, `type`, `points_cost`, `stock`, `status`, `create_by`) VALUES
('7天VIP体验卡',   '兑换后获得 7 天 VIP 会员',          'virtual',  200,  -1, 'active', 'admin'),
('30天VIP会员',    '兑换后获得 30 天 VIP 会员',         'virtual',  800,  -1, 'active', 'admin'),
('积分换余额',     '500 积分兑换 1 元钱包余额',         'virtual',  500,  -1, 'active', 'admin'),
('墨韵定制笔记本', '限量定制笔记本，实物寄送',          'physical', 2000, 100, 'active', 'admin'),
('专属头像框',     '稀有专属头像框特权',                'virtual',  300,  -1, 'active', 'admin')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- =====================================================
-- 后台管理菜单初始化：积分商城（任务/商品/兑换审核）
-- =====================================================

-- 清理旧菜单（保证可重复执行）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN ('积分商城', '任务管理', '商品管理', '兑换审核')
);
DELETE FROM `sys_menu` WHERE `menu_name` IN ('积分商城', '任务管理', '商品管理', '兑换审核');

-- 一级目录：积分商城
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '积分商城', 0, 14, 'shop', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'shopping', 'admin', NOW(), '', NULL, '积分商城目录'
);
SET @shop_menu_id = LAST_INSERT_ID();

-- 二级菜单：任务管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '任务管理', @shop_menu_id, 1, 'task', 'portal/task/index', NULL, 1, 0, 'C', '0', '0', 'portal:task:list', 'edit', 'admin', NOW(), '', NULL, '任务定义管理菜单'
);
SET @task_menu_id = LAST_INSERT_ID();
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
('任务查询', @task_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:task:query', '#', 'admin', NOW(), '', NULL, ''),
('任务新增', @task_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:task:add', '#', 'admin', NOW(), '', NULL, ''),
('任务修改', @task_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:task:edit', '#', 'admin', NOW(), '', NULL, ''),
('任务删除', @task_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:task:remove', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单：商品管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '商品管理', @shop_menu_id, 2, 'shopItem', 'portal/shopItem/index', NULL, 1, 0, 'C', '0', '0', 'portal:shopItem:list', 'shopping', 'admin', NOW(), '', NULL, '积分商品管理菜单'
);
SET @item_menu_id = LAST_INSERT_ID();
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
('商品查询', @item_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:shopItem:query', '#', 'admin', NOW(), '', NULL, ''),
('商品新增', @item_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:shopItem:add', '#', 'admin', NOW(), '', NULL, ''),
('商品修改', @item_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:shopItem:edit', '#', 'admin', NOW(), '', NULL, ''),
('商品删除', @item_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:shopItem:remove', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单：兑换审核
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '兑换审核', @shop_menu_id, 3, 'shopExchange', 'portal/shopExchange/index', NULL, 1, 0, 'C', '0', '0', 'portal:shopExchange:list', 'list', 'admin', NOW(), '', NULL, '积分兑换审核菜单'
);
SET @exchange_menu_id = LAST_INSERT_ID();
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
('兑换查询', @exchange_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:shopExchange:query', '#', 'admin', NOW(), '', NULL, ''),
('兑换审核', @exchange_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:shopExchange:edit', '#', 'admin', NOW(), '', NULL, '');

-- 为管理员角色分配菜单权限
SET @admin_role_id = 1;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN ('积分商城', '任务管理', '商品管理', '兑换审核')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN ('任务管理', '商品管理', '兑换审核'));

SELECT '任务系统+积分商城初始化脚本执行完成！' AS message;
