-- ============================================================
-- 增量脚本：20260925-01-portal_user 唯一索引升级与存量清洗
-- 目的：portal_user 的 phone / email 由普通索引升级为唯一索引
--       （uk_username 已存在），与注册/资料修改的业务校验同源：
--       校验含已注销账号（del_flag='2'），唯一索引同样对注销记录生效。
-- 前置条件：执行前请先备份 portal_user 表
--   CREATE TABLE portal_user_bak_20260925 AS SELECT * FROM portal_user;
-- 幂等性说明：MySQL 唯一索引允许多个 NULL，置空联系方式不影响账号使用；
--   注销账号重新注册时按"复活原账号"逻辑沿用原记录，不会插入第二条。
-- ============================================================

-- 1. 清洗存量重复手机号（每组保留 id 最大的一条，其余置空 phone）
UPDATE portal_user p JOIN (
  SELECT phone, MAX(id) AS keep_id
  FROM portal_user
  WHERE phone IS NOT NULL AND phone <> ''
  GROUP BY phone
  HAVING COUNT(*) > 1
) t ON p.phone = t.phone AND p.id <> t.keep_id
SET p.phone = NULL;

-- 2. 清洗存量重复邮箱（每组保留 id 最大的一条，其余置空 email）
UPDATE portal_user p JOIN (
  SELECT email, MAX(id) AS keep_id
  FROM portal_user
  WHERE email IS NOT NULL AND email <> ''
  GROUP BY email
  HAVING COUNT(*) > 1
) t ON p.email = t.email AND p.id <> t.keep_id
SET p.email = NULL;

-- 3. 校验清洗结果：以下查询应返回 0 行，否则禁止执行第 4 步
-- SELECT phone, COUNT(*) FROM portal_user WHERE phone IS NOT NULL AND phone <> '' GROUP BY phone HAVING COUNT(*) > 1;
-- SELECT email, COUNT(*) FROM portal_user WHERE email IS NOT NULL AND email <> '' GROUP BY email HAVING COUNT(*) > 1;

-- 4. 索引升级：idx_email/idx_phone -> uk_email/uk_phone（唯一索引，NULL 可重复）
--    注意：若索引名与线上实际不符，请先 SHOW INDEX FROM portal_user 确认后再调整
ALTER TABLE portal_user
  DROP KEY idx_email, ADD UNIQUE KEY uk_email (email),
  DROP KEY idx_phone, ADD UNIQUE KEY uk_phone (phone);

-- 5. 同步 init-sql 全量建表脚本（moyun-db-ddl.sql 已改为 UNIQUE KEY，此处仅为线上库迁移）
