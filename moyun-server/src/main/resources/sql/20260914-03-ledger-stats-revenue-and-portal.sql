-- ============================================================
-- 20260914-03 记账运营统计扩展 + 门户引导入口（v11.75）
-- 内容：
--   1) ledger_app_feature_config 新增"墨韵社区"链接型入口（recommend 组，remark=跳转 URL）
--   2) 无表结构变更（模块/AI/打赏统计均基于既有表结构 SQL 聚合）
-- ============================================================

-- 1. 门户引导入口（生态互导：记账 App 引导使用墨韵门户平台；remark 承载跳转 URL，后台"功能配置"可视化可改）
--    生产部署后请将 remark 更新为门户平台正式域名
DELETE FROM `moyun-db`.ledger_app_feature_config WHERE feature_key = 'portal';
INSERT INTO `moyun-db`.ledger_app_feature_config
    (feature_key, feature_name, icon, icon_color, group_type, sort_num, visible, status, badge, remark)
VALUES ('portal', '墨韵社区', '🌐', '#7fbf94', 'recommend', 3, 1, 'done', NULL, 'http://localhost:3000');
