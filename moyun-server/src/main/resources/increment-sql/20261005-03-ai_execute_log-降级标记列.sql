-- ============================================================
-- 增量脚本：20261005-03-ai_execute_log 降级标记列
-- 目的：ai_execute_log 增加 degraded 列，使降级调用在日志表可辨识、
--       场景×执行器成本看板可统计降级占比。
--
-- 背景（为什么必须加）：
--   v14.72 B2 批次已实现降级语义：AbstractAiSceneHandler.chatDetailed 瞬时
--   异常 500ms 重试 1 次仍失败 → 回落默认模型并标记 metadata.degraded=true；
--   网关 catch 兜底响应同样标记。但落库只写 status/error_msg，降级调用
--   （往往仍是 success 状态）混入正常样本不可区分——治理盲区：降级意味着
--   预期模型未生效、场景人设可能丢失，需要被看见和统计。
--
-- 幂等性说明：information_schema 前置判断 + 预处理语句，列已存在即跳过，
--   可任意次重跑。历史行 NULL 语义与 DEFAULT 0 一致（历史调用未标记降级）。
-- 前置条件：无需备份（仅新增列，不修改既有数据）。
-- 执行：需选择库（脚本用 DATABASE()）。
-- ============================================================

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_execute_log'
                   AND COLUMN_NAME = 'degraded');

SET @sql := IF(@has_col = 0,
    'ALTER TABLE `ai_execute_log` ADD COLUMN `degraded` tinyint(1) DEFAULT 0 COMMENT ''是否降级响应：1=降级（重试失败回落默认模型/网关兜底），0/NULL=正常'' AFTER `token_estimated`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 复核：期望 1 行
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_execute_log' AND COLUMN_NAME = 'degraded';
