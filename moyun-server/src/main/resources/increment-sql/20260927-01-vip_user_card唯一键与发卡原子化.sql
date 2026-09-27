-- ============================================================
-- 增量脚本：20260927-01-vip_user_card 唯一键与发卡原子化
-- 目的：把 vip_user_card 的普通索引 idx_user_platform 升级为唯一索引
--       uk_user_platform(user_id, platform_code)，落实表注释"一端一卡，续费顺延"。
--
-- 配套代码（必须与脚本同批上线，否则行为不一致）：
--   VipUserCardMapper.renewCard   —— 单条原子续期 SQL（消除 read-modify-write 丢更新）
--   VipServiceImpl.grantCard      —— 先续期(0行则插卡)，插入撞唯一键则转为续期
--   init-sql/moyun-db-ddl.sql     —— 新建库已直接是 UNIQUE KEY（本脚本仅供存量库）
--
-- 为什么必须加这个唯一键（两条独立原因）：
--   ① 并发首购：两笔订单同时"无卡 → INSERT"会产出两行，而 selectActiveCard 只取
--      ORDER BY id DESC LIMIT 1，另一行成为幽灵卡（数据不一致）；
--   ② 代码依赖：grantCard 现以"renewCard 返回 0 行 = 无卡"为分支依据，
--      唯一键是这条判据的正确性前提（否则并发下会重复插卡）。
--
-- ⚠️ 行为变化（有意的，需知悉）：
--   · 过期卡续费由"插入新行"改为"就地续期"（否则会撞唯一键）；
--   · 后台"作废会员卡"（status=0）后再购买，会在**同一行**恢复 status=1，不再新增行。
--   两者都与"一端一卡"的表注释一致；后台会员卡列表因此不再出现同一用户多行。
--
-- 前置条件：执行前请先备份
--   CREATE TABLE vip_user_card_bak_20260927 AS SELECT * FROM vip_user_card;
-- 幂等性说明：重复执行会在第 5 步 ALTER 处报 "Duplicate key name"，
--   属预期（索引已存在即无需再迁移）；第 1~4 步为幂等清洗。
-- ============================================================

-- 1. 备份（若已按前置条件备份，可跳过；保留一份便于回溯被合并的行）
CREATE TABLE IF NOT EXISTS vip_user_card_bak_20260927 AS SELECT * FROM vip_user_card;

-- 2. 计算每组保留行：取 id 最大者（最近一次创建），并汇总该组"最优状态"：
--    best_expire_null=1 表示组内存在永久卡；best_status=1 表示组内存在有效卡。
DROP TEMPORARY TABLE IF EXISTS tmp_vip_card_keep;
CREATE TEMPORARY TABLE tmp_vip_card_keep AS
SELECT user_id,
       platform_code,
       MAX(id) AS keep_id,
       MAX(CASE WHEN expire_time IS NULL THEN 1 ELSE 0 END) AS has_permanent,
       MAX(expire_time) AS max_expire,
       MAX(status)      AS best_status
FROM vip_user_card
GROUP BY user_id, platform_code;

-- 3. 把"最优到期/状态"合并进保留行，避免删掉更长有效期或有效状态
UPDATE vip_user_card c
JOIN tmp_vip_card_keep k ON c.id = k.keep_id
SET c.expire_time = IF(k.has_permanent = 1, NULL, k.max_expire),
    c.status      = IF(k.best_status = 1, 1, 0);

-- 4. 删除被合并的冗余行（已备份于 vip_user_card_bak_20260927）
DELETE c
FROM vip_user_card c
JOIN tmp_vip_card_keep k
  ON c.user_id = k.user_id
 AND c.platform_code = k.platform_code
 AND c.id <> k.keep_id;

DROP TEMPORARY TABLE IF EXISTS tmp_vip_card_keep;

-- 5. 校验：以下查询必须返回 0 行，否则禁止执行第 6 步
-- SELECT user_id, platform_code, COUNT(*) FROM vip_user_card GROUP BY user_id, platform_code HAVING COUNT(*) > 1;

-- 6. 索引升级：idx_user_platform -> uk_user_platform（唯一索引）
--    v13.18 修订：原实现是裸 ALTER（DROP KEY + ADD UNIQUE KEY），库已迁移时重跑报
--    ERROR 1091 (Can't DROP 'idx_user_platform') 并中断脚本。现按
--    "idx 是否存在 / uk 是否已建" 四种组合走 information_schema 前置判断，可任意次重跑。
--    注意：若线上索引名不一致，请先 SHOW INDEX FROM vip_user_card 确认后再调整
SET @has_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vip_user_card' AND INDEX_NAME = 'idx_user_platform');
SET @has_uk := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vip_user_card' AND INDEX_NAME = 'uk_user_platform');
SET @sql := IF(@has_uk > 0,
    IF(@has_idx > 0, 'ALTER TABLE `vip_user_card` DROP KEY `idx_user_platform`', 'DO 0'),
    IF(@has_idx > 0,
       'ALTER TABLE `vip_user_card` DROP KEY `idx_user_platform`, ADD UNIQUE KEY `uk_user_platform` (`user_id`, `platform_code`)',
       'ALTER TABLE `vip_user_card` ADD UNIQUE KEY `uk_user_platform` (`user_id`, `platform_code`)'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. 复核：期望 uk_user_platform 唯一（NON_UNIQUE=0），且无 idx_user_platform；重复组数 0
SELECT INDEX_NAME, NON_UNIQUE, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vip_user_card'
  AND INDEX_NAME IN ('uk_user_platform', 'idx_user_platform') ORDER BY INDEX_NAME;

SELECT user_id, platform_code, COUNT(*) AS dup
FROM vip_user_card GROUP BY user_id, platform_code HAVING COUNT(*) > 1;
