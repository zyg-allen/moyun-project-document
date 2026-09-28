-- =============================================================================
-- 补齐 3 处索引/唯一键分歧（现网库落后于代码与 DDL）
-- -----------------------------------------------------------------------------
-- 排查方式：用 init-sql 建全新库，与现网库逐索引比对，发现 3 处不一致。
-- **以代码注释为准**判定目标态 = DDL 态（现网库为旧态）：
--
--   LedgerTransactionServiceImpl L81-82：
--     "注意索引是 (user_id, client_uuid) 复合唯一而非 client_uuid 全局唯一——
--      clientUuid 由客户端生成，全局唯一会让两个用户偶然撞同一 uuid 时报错"
--   LedgerTipServiceImpl L62-63：
--     "必须叠加 userId 约束。clientUuid 由客户端传入且无格式约束，
--      仅按 clientUuid 查询会命中他人的 pending 单，随后复用其 userId/amount 下单"
--   LedgerScheduleServiceImpl L364-365：
--     "① 应用层 createTransaction 查到同 (user_id, client_uuid) 即返回已有流水 id；
--      ② 数据层 ledger_transaction 的 uk_user_client(user_id, client_uuid) 唯一索引兜底并发"
--
-- 变更（现网 → 目标）：
--   ledger_transaction   uk_client_uuid(client_uuid)         → uk_user_client(user_id, client_uuid)
--   ledger_tip_order     uk_client_uuid(client_uuid)         → uk_user_client(user_id, client_uuid)
--   ledger_schedule_log  idx_task(task_id,exec_date)（普通） → uk_task_date(task_id,exec_date)（唯一）
--
-- 幂等性：每步均以 information_schema.STATISTICS 前置判断（DROP INDEX 报 1091 / ADD UNIQUE 报 1061）。
-- 前置校验：加唯一键前先确认无冲突数据（重复的 (user_id, client_uuid) / (task_id, exec_date)），
--           有冲突则脚本报错中止，避免"加了唯一键但数据已违例"。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 0. 前置：检查目标唯一键是否已有冲突数据（有则 SIGNAL 中止）
-- ---------------------------------------------------------------------------
SET @dup_tx := (
  SELECT COUNT(*) FROM (
    SELECT user_id, client_uuid FROM ledger_transaction
     WHERE client_uuid IS NOT NULL AND client_uuid <> ''
     GROUP BY user_id, client_uuid HAVING COUNT(*) > 1
  ) t
);
SET @dup_tip := (
  SELECT COUNT(*) FROM (
    SELECT user_id, client_uuid FROM ledger_tip_order
     WHERE client_uuid IS NOT NULL AND client_uuid <> ''
     GROUP BY user_id, client_uuid HAVING COUNT(*) > 1
  ) t
);
SET @dup_sched := (
  SELECT COUNT(*) FROM (
    SELECT task_id, exec_date FROM ledger_schedule_log
     GROUP BY task_id, exec_date HAVING COUNT(*) > 1
  ) t
);
SET @ddl := IF(@dup_tx + @dup_tip + @dup_sched > 0,
  'SELECT ''ABORT: 目标唯一键存在重复数据，请先清洗'' AS msg',
  'SELECT ''OK: 无唯一键冲突数据'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 1. ledger_transaction：uk_client_uuid → uk_user_client(user_id, client_uuid)
-- ---------------------------------------------------------------------------
SET @has_old := (SELECT COUNT(*) FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_transaction' AND INDEX_NAME = 'uk_client_uuid');
SET @has_new := (SELECT COUNT(*) FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_transaction' AND INDEX_NAME = 'uk_user_client');
-- 先加新键（此时旧键仍在，能兜住并发窗口）
SET @ddl := IF(@has_new = 0 AND @dup_tx = 0,
  'ALTER TABLE ledger_transaction ADD UNIQUE KEY uk_user_client (user_id, client_uuid)',
  'SELECT ''skip: uk_user_client 已存在或有冲突数据'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- 再删旧键
SET @ddl := IF(@has_old > 0,
  'ALTER TABLE ledger_transaction DROP INDEX uk_client_uuid',
  'SELECT ''skip: uk_client_uuid 不存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. ledger_tip_order：uk_client_uuid → uk_user_client(user_id, client_uuid)
-- ---------------------------------------------------------------------------
SET @has_old := (SELECT COUNT(*) FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_tip_order' AND INDEX_NAME = 'uk_client_uuid');
SET @has_new := (SELECT COUNT(*) FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_tip_order' AND INDEX_NAME = 'uk_user_client');
SET @ddl := IF(@has_new = 0 AND @dup_tip = 0,
  'ALTER TABLE ledger_tip_order ADD UNIQUE KEY uk_user_client (user_id, client_uuid)',
  'SELECT ''skip: uk_user_client 已存在或有冲突数据'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl := IF(@has_old > 0,
  'ALTER TABLE ledger_tip_order DROP INDEX uk_client_uuid',
  'SELECT ''skip: uk_client_uuid 不存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. ledger_schedule_log：idx_task(普通) → uk_task_date(唯一)
--    说明：定时记账任务的幂等兜底依赖 (task_id, exec_date) 唯一
--          （应用层已按此查重，数据层需同名唯一键兜底并发）。
-- ---------------------------------------------------------------------------
SET @has_uk := (SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_schedule_log' AND INDEX_NAME = 'uk_task_date');
SET @ddl := IF(@has_uk = 0 AND @dup_sched = 0,
  'ALTER TABLE ledger_schedule_log ADD UNIQUE KEY uk_task_date (task_id, exec_date)',
  'SELECT ''skip: uk_task_date 已存在或有冲突数据'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- 旧的普通索引与唯一键列组合重复，删除以免冗余
SET @has_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_schedule_log' AND INDEX_NAME = 'idx_task');
SET @ddl := IF(@has_idx > 0,
  'ALTER TABLE ledger_schedule_log DROP INDEX idx_task',
  'SELECT ''skip: idx_task 不存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4. 复核 SQL（应返回：两表 uk_user_client 存在且 uk_client_uuid 不存在；schedule_log 有 uk_task_date）
-- ---------------------------------------------------------------------------
-- SELECT table_name, index_name, non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index)
--   FROM information_schema.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND table_name IN ('ledger_transaction','ledger_tip_order','ledger_schedule_log')
--  GROUP BY table_name, index_name, non_unique ORDER BY table_name, index_name;
