-- ============================================================
-- 增量脚本：20260925-01-portal_user 唯一索引升级与存量清洗（v13.18 重写为可重跑）
-- 目的：portal_user 的 phone / email 由普通索引升级为唯一索引
--       （uk_username 已存在），与注册/资料修改的业务校验同源：
--       校验含已注销账号（del_flag='2'），唯一索引同样对注销记录生效。
--
-- ── v13.18 修订（报告 §6.4「增量脚本不可重跑」）─────────────────────────────
-- 原实现有两个不可重跑/破坏性问题：
--   1) `ALTER TABLE ... DROP KEY idx_email` 无前置判断：库已迁移（uk_* 已建、idx_* 已删）时
--      重跑直接报 ERROR 1091，脚本无法二次执行；
--   2) 两处 `SET p.phone/email = NULL` 属破坏性清洗，却**没有任何备份步骤**，
--      且"校验清洗结果"只是注释（不执行、不阻断）。
-- 现改为：① 先建备份表（仅首次创建，保留最早快照）；② 索引变更按"idx 是否存在 / uk 是否已建"
-- 四种组合走 information_schema 前置判断 + 预处理语句（已达标即 DO 0 跳过）；
-- ③ 清洗 UPDATE 天然幂等（无重复即 0 行），并在文件末尾给出可复核 SQL。
-- 执行：需选择库（脚本用 DATABASE()），脚本可重复执行。
-- ============================================================

-- 0. 备份前置（仅首次创建；已存在则保留最初快照，不覆盖）
CREATE TABLE IF NOT EXISTS `portal_user_bak_20260925` AS SELECT * FROM `portal_user`;

-- 1. 清洗存量重复手机号（每组保留 id 最大的一条，其余置空 phone）
--    幂等：清洗后再执行匹配 0 行；破坏性操作的安全性由第 0 步备份保证
UPDATE portal_user p JOIN (
  SELECT phone, MAX(id) AS keep_id
  FROM portal_user
  WHERE phone IS NOT NULL AND phone <> ''
  GROUP BY phone
  HAVING COUNT(*) > 1
) t ON p.phone = t.phone AND p.id <> t.keep_id
SET p.phone = NULL;

-- 2. 清洗存量重复邮箱（同上）
UPDATE portal_user p JOIN (
  SELECT email, MAX(id) AS keep_id
  FROM portal_user
  WHERE email IS NOT NULL AND email <> ''
  GROUP BY email
  HAVING COUNT(*) > 1
) t ON p.email = t.email AND p.id <> t.keep_id
SET p.email = NULL;

-- 3. 索引升级：idx_email → uk_email（唯一索引，NULL 可重复）
--    四种组合：已建 uk 且已删 idx / 已建 uk 但 idx 尚在 / 未建 uk 但 idx 在 / 都没有
SET @has_idx_email := (SELECT COUNT(*) FROM information_schema.STATISTICS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND INDEX_NAME = 'idx_email');
SET @has_uk_email := (SELECT COUNT(*) FROM information_schema.STATISTICS
                      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND INDEX_NAME = 'uk_email');
SET @sql := IF(@has_uk_email > 0,
    IF(@has_idx_email > 0, 'ALTER TABLE `portal_user` DROP KEY `idx_email`', 'DO 0'),
    IF(@has_idx_email > 0,
       'ALTER TABLE `portal_user` DROP KEY `idx_email`, ADD UNIQUE KEY `uk_email` (`email`)',
       'ALTER TABLE `portal_user` ADD UNIQUE KEY `uk_email` (`email`)'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 索引升级：idx_phone → uk_phone
SET @has_idx_phone := (SELECT COUNT(*) FROM information_schema.STATISTICS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND INDEX_NAME = 'idx_phone');
SET @has_uk_phone := (SELECT COUNT(*) FROM information_schema.STATISTICS
                      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND INDEX_NAME = 'uk_phone');
SET @sql := IF(@has_uk_phone > 0,
    IF(@has_idx_phone > 0, 'ALTER TABLE `portal_user` DROP KEY `idx_phone`', 'DO 0'),
    IF(@has_idx_phone > 0,
       'ALTER TABLE `portal_user` DROP KEY `idx_phone`, ADD UNIQUE KEY `uk_phone` (`phone`)',
       'ALTER TABLE `portal_user` ADD UNIQUE KEY `uk_phone` (`phone`)'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. 复核（期望：uk_email / uk_phone 唯一，且无 idx_email / idx_phone；重复组数均为 0）
SELECT INDEX_NAME, NON_UNIQUE, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user'
  AND COLUMN_NAME IN ('phone', 'email') ORDER BY INDEX_NAME;

SELECT (SELECT COUNT(*) FROM (SELECT phone FROM portal_user WHERE phone IS NOT NULL AND phone <> ''
        GROUP BY phone HAVING COUNT(*) > 1) a) AS dup_phone_groups,
       (SELECT COUNT(*) FROM (SELECT email FROM portal_user WHERE email IS NOT NULL AND email <> ''
        GROUP BY email HAVING COUNT(*) > 1) b) AS dup_email_groups;

-- 6. 同步 init-sql 全量建表脚本（moyun-db-ddl.sql 已为 UNIQUE KEY，此处仅为线上库迁移）
