-- ============================================================
-- 增量脚本：20261001-05-文章详情广告位字典登记（v13.99）
-- 背景（清单 P2）：
--   门户文章详情页实际渲染了两个广告位 ——
--     · article_detail_sidebar  （详情侧边栏）
--     · article_detail_bottom   （正文下方）
--   （见 ArticleDetailPage.vue 的 <AdCard slot-key="..."/>）
--   但字典 portal_ad_slot_key 中**只登记了首页两个位**（home_xulin_ad / home_vip_banner）。
--   而后台「广告管理」的"广告位"下拉正是取自该字典（cms/ad/index.vue 用 useDict('portal_ad_slot_key')）
--   ⇒ 运营**无法为这两个实际存在的位置投放广告**，接口按 slotKey 查询自然永远查不到数据。
--
-- 内容：**追加**两个字典项（不删除、不覆盖既有 home_* 两行）。
-- 幂等性：INSERT ... SELECT ... WHERE NOT EXISTS（按 dict_type + dict_value 判重），可重复执行。
-- DDL/DML 初始化脚本已同步补齐（moyun-db-dml-init.sql），本脚本面向存量库。
-- 执行后清理 Redis 字典缓存（否则后端继续命中旧缓存）：
--   redis-cli DEL "sys_dict:portal_ad_slot_key"
--   或调管理端「缓存监控 → 数据字典」清理接口（CacheController /system/dict/dictCache）
-- ============================================================

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 3,'文章详情-侧边栏','article_detail_sidebar','portal_ad_slot_key','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'文章详情页侧边栏广告位（门户 AdCard 实际使用）','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'portal_ad_slot_key' AND dict_value = 'article_detail_sidebar');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 4,'文章详情-正文下方','article_detail_bottom','portal_ad_slot_key','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'文章详情页正文下方广告位（门户 AdCard 实际使用）','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'portal_ad_slot_key' AND dict_value = 'article_detail_bottom');

-- 复核：应返回 4 行（home_xulin_ad / home_vip_banner / article_detail_sidebar / article_detail_bottom）
SELECT dict_value, dict_label FROM sys_dict_data
 WHERE dict_type = 'portal_ad_slot_key' AND status = '0' ORDER BY dict_sort;
