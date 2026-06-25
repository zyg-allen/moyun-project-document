-- ===================================================================
-- 35_add_notification_user_type.sql
-- 通知系统新增 user_type 字段，区分门户用户(portal)和系统用户(sys)
--
-- 背景：
--   原 sys_notification.user_id 无法区分 ID 属于 portal_user 还是 sys_user
--   两表 ID 可能重叠，导致后台管理员无法接收通知
--
-- 方案：
--   - sys_notification 新增 user_type 字段（scope=user 时标识接收方类型）
--   - sys_notification_read 新增 user_type 字段（标识已读记录归属）
--   - 广播通知（scope=all）对两类用户都可见，已读状态按 user_type 独立计算
--
-- user_type 取值：
--   portal — 门户用户（portal_user 表）
--   sys    — 系统用户（sys_user 表，后台管理员）
-- ===================================================================

-- 1. sys_notification 表新增 user_type 字段
ALTER TABLE `sys_notification`
    ADD COLUMN `user_type` VARCHAR(20) NOT NULL DEFAULT 'portal'
    COMMENT '接收用户类型：portal=门户用户 / sys=系统用户（scope=user 时生效）'
    AFTER `user_id`;

-- 为已有数据补充默认值（历史数据均为门户用户）
UPDATE `sys_notification` SET `user_type` = 'portal' WHERE `user_type` IS NULL OR `user_type` = '';

-- 2. sys_notification_read 表新增 user_type 字段
ALTER TABLE `sys_notification_read`
    ADD COLUMN `user_type` VARCHAR(20) NOT NULL DEFAULT 'portal'
    COMMENT '已读用户类型：portal=门户用户 / sys=系统用户'
    AFTER `user_id`;

-- 为已有数据补充默认值
UPDATE `sys_notification_read` SET `user_type` = 'portal' WHERE `user_type` IS NULL OR `user_type` = '';

-- 3. 调整唯一索引（原 notification_id + user_id，现需加入 user_type）
-- 先删除旧唯一索引（如果存在）
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_notification_read'
      AND index_name = 'uk_notif_user');
SET @sql := IF(@idx_exists > 0,
    'ALTER TABLE `sys_notification_read` DROP INDEX `uk_notif_user`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建新唯一索引（notification_id + user_id + user_type）
ALTER TABLE `sys_notification_read`
    ADD UNIQUE INDEX `uk_notif_user_type` (`notification_id`, `user_id`, `user_type`);

-- 4. 添加索引优化查询（按 user_type + user_id 查询通知）
ALTER TABLE `sys_notification`
    ADD INDEX `idx_user_type_user_id` (`user_type`, `user_id`);

-- 5. 校验结果
SELECT 'sys_notification 字段:' AS info;
SHOW COLUMNS FROM `sys_notification` LIKE 'user_type';
SELECT 'sys_notification_read 字段:' AS info;
SHOW COLUMNS FROM `sys_notification_read` LIKE 'user_type';
SELECT 'sys_notification_read 索引:' AS info;
SHOW INDEX FROM `sys_notification_read` WHERE Key_name = 'uk_notif_user_type';
