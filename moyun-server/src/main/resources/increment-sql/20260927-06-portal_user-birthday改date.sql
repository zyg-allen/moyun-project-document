-- =============================================================================
-- 20260927-06 portal_user.birthday 由 varchar(20) 改为 date（v13.23）
-- =============================================================================
-- 背景（报告六 §6.5：「portal_user.birthday varchar(20) 存日期 → 导致 STR_TO_DATE 查询」）：
--   `birthday` 语义是日期，却以 varchar(20) 存储 → 比较/排序按字符串、无法建有效索引、
--   年龄统计必须逐行 `STR_TO_DATE(u.birthday,'%Y-%m-%d')`（`PortalCreatorMapper.xml` 读者年龄画像即如此），
--   且非法值（''、'1995/01/01'、'未知'）会静默留在列里。
--
-- 影响面：仅 `portal_user.birthday` 一列的**类型**（+ 存量非法值清空）；Java 侧 `PortalUser.birthday`
--   仍为 String（Connector/J 对 DATE 列 getString 返回 'YYYY-MM-DD'），**JSON 契约与前端表单不变**。
-- 幂等：先按 information_schema 判断当前类型；已是 date 则整体跳过；可任意次重跑。
-- 执行：需选择库（脚本用 DATABASE()），建议先看第 3 步复核输出。
-- =============================================================================

-- 0. 备份生日列（仅首次创建；保留原始字符串值，便于回滚）
CREATE TABLE IF NOT EXISTS `portal_user_bak_20260927_birthday` AS
SELECT `user_id`, `birthday`, NOW() AS `backup_time` FROM `portal_user`;

-- 1. 若当前仍是字符串类型：把空串与非法日期清成 NULL（否则严格模式下 MODIFY 会因 1292 失败）
SET @cur_type := (SELECT DATA_TYPE FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND COLUMN_NAME = 'birthday');

SET @sql := IF(@cur_type IN ('varchar', 'char'),
    'UPDATE `portal_user` SET `birthday` = NULL WHERE `birthday` IS NOT NULL AND (`birthday` = '''' OR STR_TO_DATE(`birthday`, ''%Y-%m-%d'') IS NULL)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 改类型（仅当当前不是 date）
SET @sql := IF(@cur_type <> 'date',
    'ALTER TABLE `portal_user` MODIFY COLUMN `birthday` date DEFAULT NULL COMMENT ''生日（v13.23 由 varchar(20) 改为 date：可索引比较、可直接 TIMESTAMPDIFF 算年龄）''',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 复核（期望：DATA_TYPE=date；invalid_remaining=0；future_dates=0）
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND COLUMN_NAME = 'birthday';

SELECT (SELECT COUNT(*) FROM `portal_user` WHERE `birthday` IS NOT NULL
        AND (`birthday` < '1900-01-01')) AS invalid_remaining,
       (SELECT COUNT(*) FROM `portal_user` WHERE `birthday` IS NOT NULL
        AND `birthday` > CURDATE()) AS future_dates,
       (SELECT COUNT(*) FROM `portal_user` WHERE `birthday` IS NOT NULL) AS with_birthday;
