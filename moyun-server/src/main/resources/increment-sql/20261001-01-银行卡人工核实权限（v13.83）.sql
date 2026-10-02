# 20261001-01 银行卡人工核实权限（清单 #52，v13.83）
#
# 背景：
#   CMS 侧「用户银行卡」此前只有 list/detail 两个**只读**接口，页面也只有核验状态筛选与展示，
#   没有任何把 PENDING 置为终态的入口 ⇒ 四要素通道无法自动判定的卡永远卡在"审核中"，
#   用户绑卡后提现路径实际不可用。
#
# 本脚本做的事：
#   1) 新增按钮级权限 cms:payBankCard:verify（挂在「用户银行卡」菜单下）；
#   2) 给已有角色补齐该权限（超管角色 role_id=1 由「全量授权」逻辑覆盖，无需处理）。
#
# ⚠ 幂等且**与菜单 ID 无关**：父菜单通过 perms='cms:payBankCard:list' 反查，
#   不引用任何现网 id（这是《问题清单》#62「菜单双轨」教训的直接应用：
#   增量脚本一旦写死 parent_id，在全新库上就会挂到错误菜单或悬空）。

SET NAMES utf8mb4;

-- 1) 新增按钮权限（父菜单按 perms 反查；重复执行不产生重复行）
INSERT INTO `sys_menu`
  (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
   `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
   `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT
  '银行卡人工核实', m.`menu_id`, 2, '', NULL, NULL, '',
  1, 0, 'F', '0', '0', 'cms:payBankCard:verify', '#',
  'admin', NOW(), '', NULL, '四要素通道无法自动判定时的人工核实入口'
FROM `sys_menu` m
WHERE m.`perms` = 'cms:payBankCard:list'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_menu` x WHERE x.`perms` = 'cms:payBankCard:verify'
  )
LIMIT 1;

-- 2) 给已拥有「用户银行卡-列表」权限的角色补上新按钮权限（超管 role_id=1 通常已全量授权）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_by`, `create_time`, `remark`)
SELECT DISTINCT rm.`role_id`, v.`menu_id`, 'admin', NOW(), 'v13.83 银行卡人工核实权限补齐'
FROM `sys_role_menu` rm
JOIN `sys_menu` base ON base.`menu_id` = rm.`menu_id` AND base.`perms` = 'cms:payBankCard:list'
JOIN `sys_menu` v ON v.`perms` = 'cms:payBankCard:verify'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_role_menu` x
  WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = v.`menu_id`
);

-- 3) 自检：应恰好 1 行
SELECT COUNT(*) AS verify_perm_rows FROM `sys_menu` WHERE `perms` = 'cms:payBankCard:verify';
