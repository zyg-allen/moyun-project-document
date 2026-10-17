-- ============================================================
-- 记账-字典种子：资产负债账户分类（树形）
--
-- 三层账户体系里的「前两层分类」：
--   第一层 大类   = 资产 / 负债（树根，is_first_level=1）
--   第二层 账户类型 = 资金账户/应收款/投资/固定资产；普通负债/赊账应付（树子节点）
-- 第三层 具体账户 = 用户命名，存 ledger_asset_account / ledger_liability_account
--
-- 幂等：dict_type + dict_value 反查，已存在则跳过。
-- 依赖：sys_dict_type.is_tree / sys_dict_data.parent_dict_id+is_first_level 树形扩展（另一任务）。
-- ============================================================

-- ① 字典类型（树形）
INSERT INTO sys_dict_type (dict_name, dict_type, status, is_tree, create_by, create_time, del_flag)
SELECT '资产负债账户分类', 'ledger_account_category', '0', '1', 'admin', NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'ledger_account_category');

-- ② 资产 根节点
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, status, parent_dict_id, is_first_level, create_by, create_time, del_flag)
SELECT 1, '资产', 'asset', 'ledger_account_category', '0', 0, '1', 'admin', NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='ledger_account_category' AND dict_value='asset');
SET @asset_root := (SELECT dict_code FROM sys_dict_data WHERE dict_type='ledger_account_category' AND dict_value='asset' AND del_flag='0' LIMIT 1);

-- ③ 负债 根节点
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, status, parent_dict_id, is_first_level, create_by, create_time, del_flag)
SELECT 2, '负债', 'liability', 'ledger_account_category', '0', 0, '1', 'admin', NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='ledger_account_category' AND dict_value='liability');
SET @liab_root := (SELECT dict_code FROM sys_dict_data WHERE dict_type='ledger_account_category' AND dict_value='liability' AND del_flag='0' LIMIT 1);

-- ④ 资产子分类（账户类型）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, status, parent_dict_id, is_first_level, create_by, create_time, del_flag)
SELECT 1, '资金账户', 'fund', 'ledger_account_category', '0', @asset_root, '0', 'admin', NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='ledger_account_category' AND dict_value='fund');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, status, parent_dict_id, is_first_level, create_by, create_time, del_flag)
SELECT 2, '应收款', 'receivable', 'ledger_account_category', '0', @asset_root, '0', 'admin', NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='ledger_account_category' AND dict_value='receivable');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, status, parent_dict_id, is_first_level, create_by, create_time, del_flag)
SELECT 3, '投资', 'investment', 'ledger_account_category', '0', @asset_root, '0', 'admin', NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='ledger_account_category' AND dict_value='investment');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, status, parent_dict_id, is_first_level, create_by, create_time, del_flag)
SELECT 4, '固定资产', 'fixed', 'ledger_account_category', '0', @asset_root, '0', 'admin', NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='ledger_account_category' AND dict_value='fixed');

-- ⑤ 负债子分类（账户类型）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, status, parent_dict_id, is_first_level, create_by, create_time, del_flag)
SELECT 1, '普通负债', 'debt', 'ledger_account_category', '0', @liab_root, '0', 'admin', NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='ledger_account_category' AND dict_value='debt');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, status, parent_dict_id, is_first_level, create_by, create_time, del_flag)
SELECT 2, '赊账应付', 'credit', 'ledger_account_category', '0', @liab_root, '0', 'admin', NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='ledger_account_category' AND dict_value='credit');
