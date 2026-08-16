-- ============================================================
-- upgrade_v9.6_admin_optimize.sql
-- 墨韵·智库 v9.6 后台优化迁移脚本
-- 内容: 第一节 知识中心 M→C 类型修复（修空白页）
--       第二节 审核入口收敛（隐藏"我的待办/我的已办"，唯一入口=内容审核中心）
--       第三节 创作者认证目录扁平化（认证审核直挂内容管理）
--       第四节 注册孤儿页面菜单（征文活动 cms/contest + 写作提示词 cms/prompt）
--       第五节 业务字典 27 类（前后台下拉/标签统一由字典驱动）
-- 特性: 全部幂等（可重复执行）
-- 执行顺序: 在 init_v7.8.sql、upgrade_v8.1/v8.2、upgrade_v9.0、upgrade_v9.5 之后执行
-- ============================================================

-- ============================================================
-- 第一节：知识中心 M→C（修复点击空白）
-- 背景: init 注册"知识中心"为 M 目录类型但填了 component=ai/knowledge-center/index，
--       RuoYi 的 M 类型只生成 Layout 不加载页面 → Tab 容器（知识库+知识文库）永不渲染；
--       v9.0 又把两个子菜单隐藏 → 侧边栏"知识中心"点击后空白。
-- 处理: 改为 C 菜单，作为 Tab 容器页渲染；子菜单保持隐藏（被容器引用）。
-- ============================================================
UPDATE sys_menu
SET menu_type = 'C', path = 'knowledge-center', component = 'ai/knowledge-center/index',
    update_by = 'admin', update_time = NOW(), remark = 'v9.6: M→C 修复（Tab容器：知识库管理+知识文库）'
WHERE menu_name = '知识中心' AND menu_type = 'M'
  AND parent_id = (SELECT t.menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1) t);

-- 验证1
SELECT '知识中心类型' AS check_item,
       IF((SELECT menu_type FROM sys_menu WHERE menu_name = '知识中心' LIMIT 1) = 'C', 'OK - 已改为C页面', 'ERROR') AS result;

-- ============================================================
-- 第二节：审核入口收敛（三入口 → 一入口）
-- 背景: 内容审核中心内部已含"待办/我的已办/全部"3个Tab（查 sys_audit_task），
--       任务管理下"我的待办/我的已办"与其完全重复（同表同逻辑同权限标识）。
-- 处理: 隐藏两个 C 菜单（保留路由与权限，URL 直达仍可用）；任务管理目录保留
--       定时任务+扫描结果。
-- ============================================================
UPDATE sys_menu
SET visible = '1', update_by = 'admin', update_time = NOW(),
    remark = 'v9.6: 收敛到内容审核中心（其内部已有待办/已办/全部Tab）'
WHERE perms IN ('system:auditTask:todo', 'system:auditTask:done') AND menu_type = 'C';

-- 验证2
SELECT '审核入口收敛' AS check_item,
       IF((SELECT COUNT(*) FROM sys_menu WHERE perms IN ('system:auditTask:todo','system:auditTask:done') AND menu_type='C' AND visible='0') = 0, 'OK - 待办/已办已隐藏', 'ERROR') AS result;

-- ============================================================
-- 第三节：创作者认证目录扁平化
-- 背景: v8.1 将创作者认证降级为内容管理下二级 M 目录，但其下仅 1 个子菜单
--       "认证审核"，目录层级冗余。
-- 处理: 认证审核 C 菜单直挂内容管理（path 改 certification），删除空目录。
-- ============================================================
SELECT @cms_dir_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;
SELECT @cert_dir_id := menu_id FROM sys_menu WHERE menu_name = '创作者认证' AND parent_id = @cms_dir_id AND menu_type = 'M' LIMIT 1;

-- 认证审核迁移至内容管理（path 改 certification 避免与其他 audit 路由混淆）
UPDATE sys_menu
SET parent_id = @cms_dir_id, path = 'certification', order_num = 12,
    update_by = 'admin', update_time = NOW(), remark = 'v9.6: 认证目录扁平化'
WHERE perms = 'cms:certification:audit' AND menu_type = 'C'
  AND (parent_id <> @cms_dir_id OR path <> 'certification');

-- 删除空目录（子 C 菜单已迁走；防御性清理残余 F 按钮）
DELETE FROM sys_role_menu WHERE menu_id = @cert_dir_id AND @cert_dir_id IS NOT NULL;
DELETE FROM sys_menu WHERE menu_id = @cert_dir_id AND @cert_dir_id IS NOT NULL;

-- 验证3
SELECT '认证扁平化' AS check_item,
       IF((SELECT COUNT(*) FROM sys_menu WHERE menu_name = '创作者认证' AND menu_type = 'M') = 0, 'OK - 目录已删', 'ERROR') AS result;

-- ============================================================
-- 第四节：注册孤儿页面菜单
-- 背景: cms/contest/index.vue（征文活动管理）与 cms/prompt/index.vue（写作提示词）
--       页面与后端 Controller（cms:contest:* / cms:writing-prompt:* 权限注解）齐全，
--       但无任何菜单注册 → 后台无法进入。前台 /contests 创作挑战页正在消费
--       征文活动数据，后台无入口 = 前台数据无人维护（违背"前台数据皆有后台管理"）。
-- 处理: 两个 C 菜单挂内容管理目录 + 标配 CRUD 按钮。
-- ============================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '征文活动', @cms_dir_id, 7, 'contest', 'cms/contest/index', NULL, 1, 0, 'C', '0', '0', 'cms:contest:list', 'award', 'admin', NOW(), 'v9.6: 注册孤儿页面（前台/contests 数据后台维护入口）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:contest:list' AND menu_type = 'C');
SELECT @contest_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:contest:list' AND menu_type = 'C' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '征文查询', @contest_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:contest:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:contest:query' AND menu_type = 'F');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '征文新增', @contest_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:contest:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:contest:add' AND menu_type = 'F');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '征文修改', @contest_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:contest:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:contest:edit' AND menu_type = 'F');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '征文删除', @contest_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:contest:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:contest:remove' AND menu_type = 'F');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '写作提示词', @cms_dir_id, 8, 'writing-prompt', 'cms/prompt/index', NULL, 1, 0, 'C', '0', '0', 'cms:writing-prompt:list', 'edit', 'admin', NOW(), 'v9.6: 注册孤儿页面（写作Prompt管理）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:writing-prompt:list' AND menu_type = 'C');
SELECT @prompt_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:writing-prompt:list' AND menu_type = 'C' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '提示词查询', @prompt_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:writing-prompt:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:writing-prompt:query' AND menu_type = 'F');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '提示词新增', @prompt_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:writing-prompt:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:writing-prompt:add' AND menu_type = 'F');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '提示词修改', @prompt_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:writing-prompt:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:writing-prompt:edit' AND menu_type = 'F');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '提示词删除', @prompt_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:writing-prompt:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:writing-prompt:remove' AND menu_type = 'F');

-- 验证4
SELECT '孤儿页面菜单' AS check_item,
       IF((SELECT COUNT(*) FROM sys_menu WHERE perms IN ('cms:contest:list','cms:writing-prompt:list') AND menu_type='C') = 2, 'OK - 2个菜单已注册', 'ERROR') AS result;

-- ============================================================
-- 第五节：业务字典 27 类（前后台统一下拉/标签数据源）
-- 说明: 值域与后端枚举 code 对齐（PaymentStatus/ArticleStatus/AuditTaskType 等），
--       list_class 为 el-tag 类型（success/warning/danger/info/primary/default）。
--       后台页面 useDict 渲染；前台经 /portal/dict 公开接口渲染。
-- 写法注意: UNION 派生表的首行必须显式列别名（MySQL 派生表列名取自首行
--       表达式，字面量无别名时列名即字面量文本，外层无法引用 t.dict_type）。
-- ============================================================

-- ---------- 5.1 支付/交易域 ----------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '支付状态', 'portal_pay_status', '0', 'admin', NOW(), 'v9.6: 订单/打赏状态（对齐PaymentStatus枚举5态）' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_pay_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'待支付' AS dict_label,'pending' AS dict_value,'portal_pay_status' AS dict_type,'' AS css_class,'warning' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'已支付','paid','portal_pay_status','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'已退款','refunded','portal_pay_status','','info','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'已关闭','closed','portal_pay_status','','default','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 5,'支付失败','failed','portal_pay_status','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '支付渠道', 'portal_pay_channel', '0', 'admin', NOW(), 'v9.6: 对齐PaymentChannel枚举' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_pay_channel');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'积分' AS dict_label,'points' AS dict_value,'portal_pay_channel' AS dict_type,'' AS css_class,'default' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'支付宝','alipay','portal_pay_channel','','primary','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'微信','wechat','portal_pay_channel','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'钱包','wallet','portal_pay_channel','','warning','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '打赏目标类型', 'portal_tip_target_type', '0', 'admin', NOW(), 'v9.6: 打赏流水目标' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_tip_target_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'文章打赏' AS dict_label,'article' AS dict_value,'portal_tip_target_type' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'专栏打赏','column','portal_tip_target_type','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'付费阅读','article_paid','portal_tip_target_type','','warning','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '钱包交易类型', 'portal_wallet_txn_type', '0', 'admin', NOW(), 'v9.6: 钱包流水类型' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_wallet_txn_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'充值' AS dict_label,'recharge' AS dict_value,'portal_wallet_txn_type' AS dict_type,'' AS css_class,'success' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'消费','consume','portal_wallet_txn_type','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'退款','refund','portal_wallet_txn_type','','info','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'提现','withdraw','portal_wallet_txn_type','','default','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 5,'打赏','tip','portal_wallet_txn_type','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- ---------- 5.2 内容域状态 ----------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '文章状态', 'cms_article_status', '0', 'admin', NOW(), 'v9.6: 对齐ArticleStatus枚举5态' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'cms_article_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'草稿' AS dict_label,'draft' AS dict_value,'cms_article_status' AS dict_type,'' AS css_class,'info' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'待审核','pending','cms_article_status','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'已发布','published','cms_article_status','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'已拒绝','rejected','cms_article_status','','danger','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 5,'已归档','archived','cms_article_status','','default','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '专栏状态', 'cms_column_status', '0', 'admin', NOW(), 'v9.6: 专栏生命周期' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'cms_column_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'草稿' AS dict_label,'draft' AS dict_value,'cms_column_status' AS dict_type,'' AS css_class,'info' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'已发布','published','cms_column_status','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'已归档','archived','cms_column_status','','default','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '话题状态', 'cms_topic_status', '0', 'admin', NOW(), 'v9.6: 话题生命周期' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'cms_topic_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'待审核' AS dict_label,'pending' AS dict_value,'cms_topic_status' AS dict_type,'' AS css_class,'warning' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'已发布','active','cms_topic_status','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'审核驳回','rejected','cms_topic_status','','danger','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'已归档','archived','cms_topic_status','','default','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 5,'已删除','deleted','cms_topic_status','','info','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '征文活动状态', 'cms_contest_status', '0', 'admin', NOW(), 'v9.6: 征文活动生命周期' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'cms_contest_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'草稿' AS dict_label,'draft' AS dict_value,'cms_contest_status' AS dict_type,'' AS css_class,'info' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'征集中','collecting','cms_contest_status','','primary','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'投票中','voting','cms_contest_status','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'已结束','ended','cms_contest_status','','default','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- ---------- 5.3 审核域 ----------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审核任务类型', 'cms_audit_task_type', '0', 'admin', NOW(), 'v9.6: 对齐AuditTaskType枚举8类' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'cms_audit_task_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'文章审核' AS dict_label,'article' AS dict_value,'cms_audit_task_type' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'专栏审核','column','cms_audit_task_type','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'话题审核','topic','cms_audit_task_type','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'面经审核','interview_exp','cms_audit_task_type','','danger','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 5,'面经评论审核','interview_comment','cms_audit_task_type','','info','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 6,'创作者认证','certification','cms_audit_task_type','','primary','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 7,'意见反馈','feedback','cms_audit_task_type','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 8,'举报','report','cms_audit_task_type','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '审核任务状态', 'cms_audit_task_status', '0', 'admin', NOW(), 'v9.6: 对齐AuditTaskStatus枚举' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'cms_audit_task_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'待审核' AS dict_label,'pending' AS dict_value,'cms_audit_task_status' AS dict_type,'' AS css_class,'warning' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'已通过','approved','cms_audit_task_status','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'已驳回','rejected','cms_audit_task_status','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- ---------- 5.4 反馈/举报域 ----------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '反馈类型', 'cms_feedback_type', '0', 'admin', NOW(), 'v9.6: 前后台共用' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'cms_feedback_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'功能建议' AS dict_label,'suggestion' AS dict_value,'cms_feedback_type' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'Bug反馈','bug','cms_feedback_type','','danger','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'体验问题','experience','cms_feedback_type','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'其他','other','cms_feedback_type','','info','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '举报类型', 'cms_report_type', '0', 'admin', NOW(), 'v9.6: 前后台共用' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'cms_report_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'垃圾内容' AS dict_label,'spam' AS dict_value,'cms_report_type' AS dict_type,'' AS css_class,'warning' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'不当内容','inappropriate','cms_report_type','','danger','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'侵权内容','infringement','cms_report_type','','info','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'欺诈行为','fraud','cms_report_type','','danger','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 5,'其他问题','other','cms_report_type','','default','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '处理状态', 'cms_handle_status', '0', 'admin', NOW(), 'v9.6: 反馈/举报共用' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'cms_handle_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'待处理' AS dict_label,'pending' AS dict_value,'cms_handle_status' AS dict_type,'' AS css_class,'warning' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'处理中','processing','cms_handle_status','','primary','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'已解决','resolved','cms_handle_status','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'已驳回','rejected','cms_handle_status','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- ---------- 5.5 读书空间域 ----------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '书籍类型', 'portal_book_type', '0', 'admin', NOW(), 'v9.6: 书籍分类' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_book_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'出版书籍' AS dict_label,'published' AS dict_value,'portal_book_type' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'网络小说','novel','portal_book_type','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'长文文章','longform','portal_book_type','','info','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '书籍连载状态', 'portal_book_serial_status', '0', 'admin', NOW(), 'v9.6: 连载状态' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_book_serial_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'连载中' AS dict_label,'ongoing' AS dict_value,'portal_book_serial_status' AS dict_type,'' AS css_class,'warning' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'已完结','completed','portal_book_serial_status','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'暂停','hiatus','portal_book_serial_status','','info','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '访问级别', 'portal_access_type', '0', 'admin', NOW(), 'v9.6: 书籍/书单共用' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_access_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'免费公开' AS dict_label,'free' AS dict_value,'portal_access_type' AS dict_type,'' AS css_class,'success' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'会员专享','vip','portal_access_type','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'试读','preview','portal_access_type','','info','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '业务通用状态', 'portal_common_status', '0', 'admin', NOW(), 'v9.6: 书籍/书单等 active/inactive' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_common_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'正常' AS dict_label,'active' AS dict_value,'portal_common_status' AS dict_type,'' AS css_class,'success' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'停用','inactive','portal_common_status','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- ---------- 5.6 学习辅助域 ----------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '学习计划类型', 'portal_study_plan_type', '0', 'admin', NOW(), 'v9.6: 学习计划' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_study_plan_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'每日刷题' AS dict_label,'daily_question' AS dict_value,'portal_study_plan_type' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'每周阅读','weekly_reading','portal_study_plan_type','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'自定义','custom','portal_study_plan_type','','info','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '学习计划状态', 'portal_study_plan_status', '0', 'admin', NOW(), 'v9.6: 学习计划' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_study_plan_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'进行中' AS dict_label,'active' AS dict_value,'portal_study_plan_status' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'已完成','completed','portal_study_plan_status','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'已放弃','abandoned','portal_study_plan_status','','info','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '错题状态', 'portal_wrong_question_status', '0', 'admin', NOW(), 'v9.6: 错题本' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_wrong_question_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'待复习' AS dict_label,'wrong' AS dict_value,'portal_wrong_question_status' AS dict_type,'' AS css_class,'danger' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'复习中','reviewing','portal_wrong_question_status','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'已掌握','mastered','portal_wrong_question_status','','success','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- ---------- 5.7 面试指南域 ----------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '题目难度', 'portal_question_difficulty', '0', 'admin', NOW(), 'v9.6: 前台题库/后台题库共用' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_question_difficulty');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'简单' AS dict_label,'easy' AS dict_value,'portal_question_difficulty' AS dict_type,'' AS css_class,'success' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'中等','medium','portal_question_difficulty','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'困难','hard','portal_question_difficulty','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '题目类型', 'portal_question_type', '0', 'admin', NOW(), 'v9.6: 前台题库/后台题库共用' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_question_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'算法' AS dict_label,'algorithm' AS dict_value,'portal_question_type' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'八股文','bagwen','portal_question_type','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'系统设计','system_design','portal_question_type','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'项目场景','project','portal_question_type','','info','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 5,'HR面','hr','portal_question_type','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '简历模板分类', 'portal_resume_category', '0', 'admin', NOW(), 'v9.6: value为中文与现有数据兼容' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_resume_category');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'技术岗' AS dict_label,'技术岗' AS dict_value,'portal_resume_category' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'产品岗','产品岗','portal_resume_category','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'应届生','应届生','portal_resume_category','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'社招','社招','portal_resume_category','','info','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 5,'实习','实习','portal_resume_category','','default','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 6,'简历模板','简历模板','portal_resume_category','','primary','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT 'AI模拟面试场景', 'portal_mock_scene', '0', 'admin', NOW(), 'v9.6: value为中文（AI对话入参）' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_mock_scene');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'算法' AS dict_label,'算法' AS dict_value,'portal_mock_scene' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'系统设计','系统设计','portal_mock_scene','','success','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 3,'前端','前端','portal_mock_scene','','warning','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 4,'后端','后端','portal_mock_scene','','info','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 5,'数据库','数据库','portal_mock_scene','','default','N','0','admin',NOW() UNION ALL
                                                                                                                                                    SELECT 6,'项目深挖','项目深挖','portal_mock_scene','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- ---------- 5.8 运营位/登录域 ----------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '广告位标识', 'portal_ad_slot_key', '0', 'admin', NOW(), 'v9.6: 广告位槽位（新增槽位改字典即可）' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'portal_ad_slot_key');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'文章详情底部' AS dict_label,'article_detail_bottom' AS dict_value,'portal_ad_slot_key' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'首页侧栏','home_sidebar','portal_ad_slot_key','','success','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT 'VIP套餐状态', 'cms_vip_status', '0', 'admin', NOW(), 'v9.6: 0上架/1下架' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'cms_vip_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'上架' AS dict_label,'0' AS dict_value,'cms_vip_status' AS dict_type,'' AS css_class,'success' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'下架','1','cms_vip_status','','danger','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '登录端类型', 'sys_login_type', '0', 'admin', NOW(), 'v9.6: 登录日志来源' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'sys_login_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, t.css_class, t.list_class, t.is_default, t.status, t.create_by, t.create_time FROM (
                                                                                                                                                    SELECT 1 AS dict_sort,'后台登录' AS dict_label,'sys' AS dict_value,'sys_login_type' AS dict_type,'' AS css_class,'primary' AS list_class,'N' AS is_default,'0' AS status,'admin' AS create_by,NOW() AS create_time UNION ALL
                                                                                                                                                    SELECT 2,'前台登录','portal','sys_login_type','','success','N','0','admin',NOW()
                                                                                                                                                ) t WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.dict_type = t.dict_type AND d.dict_value = t.dict_value);

-- 字典验证（27类 / 104条）
SELECT '业务字典注册' AS check_item,
       IF(COUNT(*) = 27, 'OK - 27类业务字典', CONCAT('WARN - 实际', COUNT(*), '类(人工核对)')) AS result
FROM sys_dict_type WHERE dict_type LIKE 'portal\_%' ESCAPE '\\' OR dict_type LIKE 'cms\_%' ESCAPE '\\' OR dict_type = 'sys_login_type';
SELECT '业务字典数据' AS check_item,
       IF(COUNT(*) = 104, 'OK - 104条字典数据', CONCAT('WARN - 实际', COUNT(*), '条(人工核对)')) AS result
FROM sys_dict_data d
         JOIN sys_dict_type t ON d.dict_type = t.dict_type
WHERE t.dict_type LIKE 'portal\_%' ESCAPE '\\' OR t.dict_type LIKE 'cms\_%' ESCAPE '\\' OR t.dict_type = 'sys_login_type';
