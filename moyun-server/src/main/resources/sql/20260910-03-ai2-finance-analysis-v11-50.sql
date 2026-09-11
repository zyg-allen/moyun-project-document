-- v11.50 finance_analysis 架构调整配套（ai_scene_config）
-- 背景：数据组装下沉 FinanceAnalysisHandler（ledger 包），网关返回 data 含实时指标（indicators 等）。
-- 网关 TTL 语义缓存会在数据变化后返回过期指标，与 Service 层数据指纹快照（数据/配置变化自动失效）
-- 职责重叠且口径不准——显式关闭，缓存统一由业务快照承担。幂等可重复执行。
UPDATE `moyun-db`.ai_scene_config
SET enable_cache = 0,
    update_time = NOW()
WHERE scene_code = 'finance_analysis';
