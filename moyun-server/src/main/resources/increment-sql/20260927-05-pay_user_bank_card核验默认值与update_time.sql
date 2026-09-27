-- =============================================================================
-- 20260927-05 pay_user_bank_card 核验状态默认值与 update_time（v13.18）
-- =============================================================================
-- 背景（报告六 §6.4 两处中危）：
--   1) `verify_status` NOT NULL DEFAULT 'VERIFIED' 是 **fail-open 默认值**：
--      任何绕过 BankCardServiceImpl 四要素核验的直插 SQL（运维补数、导入、将来新增的写入口）
--      都会默认得到"已验证"，而提现闸门 `WithdrawOrderServiceImpl:100` 只认 VERIFIED
--      → 未核验的卡可直接用于提现。默认值必须 fail-closed（PENDING）。
--   2) `update_time` 只有 DEFAULT CURRENT_TIMESTAMP、缺 ON UPDATE：
--      语义上"看着像自动维护"，实际更新时不刷新（只有创建那一刻的值），排查问题时会被误导。
--
-- 影响面：仅 `pay_user_bank_card` 两列的**列定义**（默认值 / 自动更新属性），
--        **不修改任何存量数据**——已存在的 VERIFIED 是当时真实核验结果，保持原样。
-- 幂等：两处 ALTER 均先用 information_schema 判断当前默认值 / EXTRA，已达标即 DO 0 跳过；
--       可任意次重跑（本脚本在 dev 库连跑两次均无错）。
-- 执行：需选择库（脚本用 DATABASE()），例如
--       mysql -uroot -p -D 'moyun-db' -e "source <绝对路径>/20260927-05-....sql"
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 一、verify_status 默认值：'VERIFIED' → 'PENDING'（fail-closed）
-- -----------------------------------------------------------------------------
SET @cur_default := (SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_user_bank_card'
                       AND COLUMN_NAME = 'verify_status');

SET @sql := IF(@cur_default = 'VERIFIED',
    'ALTER TABLE `pay_user_bank_card` MODIFY COLUMN `verify_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT ''PENDING'' COMMENT ''验证状态：PENDING=待验证 / VERIFIED=已验证 / REJECTED=已驳回（v13.18 默认值改 fail-closed：直插 SQL 不得绕过四要素核验）''',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 二、update_time 补 ON UPDATE CURRENT_TIMESTAMP
-- -----------------------------------------------------------------------------
SET @cur_extra := (SELECT EXTRA FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_user_bank_card'
                     AND COLUMN_NAME = 'update_time');

SET @sql := IF(LOWER(COALESCE(@cur_extra, '')) NOT LIKE '%on update%',
    'ALTER TABLE `pay_user_bank_card` MODIFY COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''修改时间''',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 三、复核（期望：verify_status 默认值为 PENDING；update_time 的 EXTRA 含 on update CURRENT_TIMESTAMP）
-- -----------------------------------------------------------------------------
SELECT COLUMN_NAME, COLUMN_DEFAULT, EXTRA, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_user_bank_card'
  AND COLUMN_NAME IN ('verify_status', 'update_time');

-- 期望 0 行：不得再有 fail-open 默认值的核验/审核类状态列
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (COLUMN_NAME LIKE '%verify_status%' OR COLUMN_NAME LIKE '%audit_status%'
       OR COLUMN_NAME LIKE '%review_status%' OR COLUMN_NAME LIKE '%cert_status%')
  AND UPPER(COLUMN_DEFAULT) IN ('VERIFIED', 'APPROVED', 'PASSED', 'SUCCESS');

-- 期望 0 行：不得再有"NOT NULL DEFAULT CURRENT_TIMESTAMP 却缺 ON UPDATE"的 update_time
SELECT TABLE_NAME, COLUMN_NAME, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'update_time'
  AND COLUMN_TYPE LIKE 'datetime%' AND IS_NULLABLE = 'NO' AND COLUMN_DEFAULT LIKE '%CURRENT_TIMESTAMP%'
  AND LOWER(EXTRA) NOT LIKE '%on update%';
