-- =============================================================
-- 私信表扩展：支持跨用户体系（portal 用户 ↔ sys 管理员）
--
-- 背景：原 portal_message_session.user_a/user_b、portal_message.sender_id/receiver_id
--   均为裸 Long，无法区分用户属于 portal_user 还是 sys_user，
--   导致管理员（sys_user）无法接收门户用户的私信。
--
-- 方案：加 user_type 软区分列（portal/sys），默认 'portal' 保持存量数据兼容；
--   唯一键改为 (user_a, user_b, user_a_type, user_b_type)，
--   使 portal(5) 与 sys(5) 这类数值相同但体系不同的用户不会撞会话。
--
-- 注意：user_a/user_b 仍按数值"小ID为A"归一化，user_a_type/user_b_type
--   分别跟随对应一方，查询时必须同时带 userId + userType 过滤。
--
-- 幂等性：本脚本可重复执行，通过 information_schema 检查列/索引是否存在，
--   已存在则跳过对应 ALTER，避免重复执行报错。
-- =============================================================

-- 1) 会话表加双方用户类型列（已存在则跳过）
SET @col_exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'portal_message_session' AND column_name = 'user_a_type');
SET @sql := IF(@col_exist = 0,
    'ALTER TABLE portal_message_session ADD COLUMN user_a_type VARCHAR(16) NOT NULL DEFAULT ''portal'' COMMENT ''A方用户类型 portal/sys'' AFTER user_a, ADD COLUMN user_b_type VARCHAR(16) NOT NULL DEFAULT ''portal'' COMMENT ''B方用户类型 portal/sys'' AFTER user_b',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 会话唯一键升级：加上类型维度，避免不同体系同 ID 撞会话
--    先删旧唯一键 uk_users（存在才删），再建新唯一键 uk_users_type（不存在才建）
SET @idx_exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'portal_message_session' AND index_name = 'uk_users');
SET @sql := IF(@idx_exist > 0, 'ALTER TABLE portal_message_session DROP INDEX uk_users', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'portal_message_session' AND index_name = 'uk_users_type');
SET @sql := IF(@idx_exist = 0,
    'ALTER TABLE portal_message_session ADD UNIQUE KEY uk_users_type (user_a, user_b, user_a_type, user_b_type)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 消息表加发送/接收方类型，便于按"我收到的消息"精确过滤
SET @col_exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'portal_message' AND column_name = 'sender_type');
SET @sql := IF(@col_exist = 0,
    'ALTER TABLE portal_message ADD COLUMN sender_type VARCHAR(16) NOT NULL DEFAULT ''portal'' COMMENT ''发送者类型 portal/sys'' AFTER sender_id, ADD COLUMN receiver_type VARCHAR(16) NOT NULL DEFAULT ''portal'' COMMENT ''接收者类型 portal/sys'' AFTER receiver_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) 辅助索引：按"接收方+类型+已读"查未读
--    先删旧索引 idx_receiver_read（存在才删），再建新索引 idx_receiver_type_read（不存在才建）
SET @idx_exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'portal_message' AND index_name = 'idx_receiver_read');
SET @sql := IF(@idx_exist > 0, 'ALTER TABLE portal_message DROP INDEX idx_receiver_read', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'portal_message' AND index_name = 'idx_receiver_type_read');
SET @sql := IF(@idx_exist = 0,
    'ALTER TABLE portal_message ADD INDEX idx_receiver_type_read (receiver_id, receiver_type, is_read)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
