-- ============================================================
-- 增量脚本：20261001-07-列表页与搜索页侧栏广告位登记（v14.28）
-- 背景：ListPage.vue / SearchPage.vue 侧栏的「合作推广」卡片是**写死文案 + 写死跳转**
--   （渐变 #4f46e5→#7c3aed、链接 /creator/certification），注释自认"预留后端接口位置"；
--   而项目已有完整的广告位体系（AdCard 组件 + GET /portal/ad/list?slotKey= + 后台广告位管理菜单），
--   文章详情页已在用 article_detail_sidebar / article_detail_bottom。
--
-- 内容：把两个新广告位登记进数据字典 portal_ad_slot_key，使后台「广告位管理」下拉可选、前台可按 slotKey 取广告。
--   · article_list_sidebar  ：文章列表-侧边栏
--   · search_sidebar        ：搜索结果-侧边栏
-- 幂等性：INSERT ... SELECT ... WHERE NOT EXISTS（按 dict_type + dict_value 判重）。
-- 执行后清理字典缓存：redis-cli DEL "sys_dict:portal_ad_slot_key"（或后台「缓存监控」清理）
-- ============================================================

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 5,'文章列表-侧边栏','article_list_sidebar','portal_ad_slot_key','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'广告位：列表页右侧栏','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='portal_ad_slot_key' AND dict_value='article_list_sidebar');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag)
SELECT 6,'搜索结果-侧边栏','search_sidebar','portal_ad_slot_key','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'广告位：搜索页右侧栏','0'
 WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='portal_ad_slot_key' AND dict_value='search_sidebar');

-- 复核：portal_ad_slot_key 应为 6 条
SELECT dict_value, dict_label FROM sys_dict_data WHERE dict_type='portal_ad_slot_key' ORDER BY dict_sort;
