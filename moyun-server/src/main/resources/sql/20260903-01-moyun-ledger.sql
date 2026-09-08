-- =====================================================================
-- 记账模块（个人资产管理）Phase 1
-- 墨韵智库 · moyun
-- 执行库：moyun-db（与既有业务同库）
-- 幂等：建表 IF NOT EXISTS；分类/菜单先 DELETE 再 INSERT，可重复执行
-- 设计文档：docs/11-记账模块需求分析/记账模块设计方案V1.2.md
-- 说明：
--   1) 金额单位统一为「元」DECIMAL(18,2)（V1.34 变更，见 20260904-03 迁移脚本）；原 BIGINT 分方案已废止
--   2) 单账本模型，不预留 book_id（V1.2 决策）
--   3) 实体类不继承 BaseEntity（表无 create_by/update_by/del_flag，流水自管 status）
--   4) user_id 口径 = portal_user.id（门户用户主键，严禁误用关联的 sys_user.user_id）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 资产账户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_asset_account` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资产账户ID',
  `user_id`          BIGINT       NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `name`             VARCHAR(100) NOT NULL COMMENT '账户名称，如"招商银行储蓄卡"',
  `type`             VARCHAR(20)  NOT NULL COMMENT '类型：cash/savings/ewallet/stored_value/investment/fixed_asset/receivable/other',
  `balance`          BIGINT       NOT NULL DEFAULT 0 COMMENT '当前余额（分）',
  `valuation`        BIGINT       NULL COMMENT '估值（分；投资/固定资产用，可≠balance）',
  `include_in_total` TINYINT      NOT NULL DEFAULT 1 COMMENT '是否计入总资产：1=是 0=否',
  `icon`             VARCHAR(50)  NULL COMMENT '图标',
  `hide_balance`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否隐藏余额（隐私模式）：1=是 0=否',
  `sort_order`       INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=停用归档（删除即归档，流水永久保留）',
  `version`          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-资产账户';

-- ---------------------------------------------------------------------
-- 2. 负债账户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_liability_account` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '负债账户ID',
  `user_id`          BIGINT        NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `name`             VARCHAR(100)  NOT NULL COMMENT '负债名称，如"招行信用卡"',
  `type`             VARCHAR(20)   NOT NULL COMMENT '类型：credit_card/consumer_loan/bank_loan/personal_loan/other',
  `balance`          BIGINT        NOT NULL DEFAULT 0 COMMENT '当前欠款（分）',
  `principal`        BIGINT        NULL COMMENT '初始本金（分）',
  `annual_rate`      DECIMAL(10,4) NULL COMMENT '年利率（%）',
  `total_terms`      INT           NULL COMMENT '总期数（月）',
  `paid_terms`       INT           NULL COMMENT '已还期数（月）',
  `monthly_payment`  BIGINT        NULL COMMENT '每期还款额（分）',
  `repayment_day`    TINYINT       NULL COMMENT '还款日（每月几号，1-28）',
  `due_date`         DATE          NULL COMMENT '到期日',
  `include_in_total` TINYINT       NOT NULL DEFAULT 1 COMMENT '是否计入总负债：1=是 0=否',
  `icon`             VARCHAR(50)   NULL COMMENT '图标',
  `sort_order`       INT           NOT NULL DEFAULT 0 COMMENT '排序',
  `status`           TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=停用归档（手动删除）',
  `settle_flag`      TINYINT       NOT NULL DEFAULT 0 COMMENT '已结清：1=是 0=否（还款至0自动置位；归档展示、不计入当前总负债）',
  `version`          INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-负债账户';

-- ---------------------------------------------------------------------
-- 3. 记账流水表（核心）
--    balance_after 系列快照：修改/删除流水时余额冲正重放的依据
--    status=0 逻辑删除（冲正后归档），不物理删除，保证历史可追溯
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_transaction` (
  `id`                      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id`                 BIGINT        NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `type`                    VARCHAR(20)   NOT NULL COMMENT '类型：income/expense/transfer/repayment/borrow/adjust',
  `amount`                  BIGINT        NOT NULL COMMENT '金额（分；adjust 可为负表示调减，其余恒为正，方向由type决定）',
  `category_id`             BIGINT        NULL COMMENT '分类ID（ledger_category）',
  `account_id`              BIGINT        NULL COMMENT '关联资产账户（支出/收入/转出方/还款扣款方/校准账户）',
  `liability_id`            BIGINT        NULL COMMENT '关联负债账户（还款/借款）',
  `target_account_id`       BIGINT        NULL COMMENT '转账目标资产账户',
  `balance_after`           BIGINT        NULL COMMENT '主账户交易后余额快照（分）',
  `target_balance_after`    BIGINT        NULL COMMENT '转账目标账户交易后余额快照（分）',
  `liability_balance_after` BIGINT        NULL COMMENT '关联负债交易后欠款快照（分）',
  `description`             VARCHAR(200)  NULL COMMENT '备注',
  `transaction_date`        DATE          NOT NULL COMMENT '交易日期（默认当天）',
  `transaction_time`        TIME          NULL COMMENT '交易时间',
  `merchant`                VARCHAR(100)  NULL COMMENT '商户名称',
  `is_budget`               TINYINT       NOT NULL DEFAULT 1 COMMENT '是否计入预算：1=是 0=否（adjust 默认0）',
  `status`                  TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=已删除（冲正后归档）',
  `client_uuid`             VARCHAR(64)   NULL COMMENT '客户端幂等键（Phase 4 离线同步防重复提交）',
  `create_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status_id` (`user_id`, `status`, `id`),
  KEY `idx_user_date` (`user_id`, `transaction_date`),
  KEY `idx_account` (`account_id`),
  KEY `idx_liability` (`liability_id`),
  UNIQUE KEY `uk_client_uuid` (`client_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-流水';

-- ---------------------------------------------------------------------
-- 4. 分类表（user_id=0 + is_system=1 为系统预设，后台统一维护）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_category` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `user_id`      BIGINT      NOT NULL DEFAULT 0 COMMENT '0=系统预设，>0=用户自定义（portal_user.id）',
  `name`         VARCHAR(50) NOT NULL COMMENT '分类名称',
  `type`         VARCHAR(10) NOT NULL COMMENT '类型：income/expense',
  `parent_id`    BIGINT      NULL COMMENT '父分类ID（支持二级分类）',
  `icon`         VARCHAR(50) NULL COMMENT '图标',
  `color`        VARCHAR(20) NULL COMMENT '颜色',
  `sort_order`   INT         NOT NULL DEFAULT 0 COMMENT '排序',
  `is_system`    TINYINT     NOT NULL DEFAULT 0 COMMENT '系统预设：1=是（仅后台可维护） 0=自定义',
  `status`       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=停用',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`, `type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-分类（系统预设+用户自定义）';

-- ---------------------------------------------------------------------
-- 5. 预算表（category_id NULL=月度总预算，非空=分类预算）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_budget` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '预算ID',
  `user_id`      BIGINT   NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `category_id`  BIGINT   NULL COMMENT '分类ID（NULL=月度总预算）',
  `year`         INT      NOT NULL COMMENT '年份',
  `month`        INT      NOT NULL COMMENT '月份（1-12）',
  `amount`       BIGINT   NOT NULL COMMENT '预算金额（分）',
  `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_period` (`user_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-预算';

-- ---------------------------------------------------------------------
-- 6. 净资产每日快照表（趋势图与首页涨跌标识的数据来源）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_net_worth_snapshot` (
  `id`               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  `user_id`          BIGINT   NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `snap_date`        DATE     NOT NULL COMMENT '快照日期（每日定时任务生成；当日有记账实时upsert）',
  `total_asset`      BIGINT   NOT NULL COMMENT '总资产（分）',
  `total_liability`  BIGINT   NOT NULL COMMENT '总负债（分）',
  `net_worth`        BIGINT   NOT NULL COMMENT '净资产（分）= total_asset - total_liability',
  `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `snap_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-净资产每日快照';

-- ---------------------------------------------------------------------
-- 7. 系统预设分类（user_id=0 + is_system=1，幂等：先 DELETE 再 INSERT）
-- ---------------------------------------------------------------------
DELETE FROM `moyun-db`.ledger_category WHERE user_id = 0 AND is_system = 1;

INSERT INTO `moyun-db`.ledger_category (user_id, name, type, parent_id, icon, color, sort_order, is_system, status) VALUES
-- 支出分类（一级）
(0, '餐饮',     'expense', NULL, 'food',      '#F5A623', 1,  1, 1),
(0, '交通',     'expense', NULL, 'transport', '#4A90D9', 2,  1, 1),
(0, '购物',     'expense', NULL, 'shopping',  '#BD5D8A', 3,  1, 1),
(0, '居住',     'expense', NULL, 'home',      '#7B8FA1', 4,  1, 1),
(0, '娱乐',     'expense', NULL, 'entertainment', '#9B59B6', 5, 1, 1),
(0, '医疗',     'expense', NULL, 'medical',   '#E74C3C', 6,  1, 1),
(0, '教育',     'expense', NULL, 'education','#2ECC71', 7,  1, 1),
(0, '通讯',     'expense', NULL, 'phone',    '#34495E', 8,  1, 1),
(0, '日用',     'expense', NULL, 'daily',    '#95A5A6', 9,  1, 1),
(0, '人情往来', 'expense', NULL, 'gift',     '#D35400', 10, 1, 1),
(0, '宠物',     'expense', NULL, 'pet',      '#16A085', 11, 1, 1),
(0, '旅行',     'expense', NULL, 'travel',   '#2980B9', 12, 1, 1),
(0, '房贷/房租','expense', NULL, 'house-loan','#C0392B', 13, 1, 1),
(0, '车贷',     'expense', NULL, 'car-loan', '#8E44AD', 14, 1, 1),
(0, '还款',     'expense', NULL, 'repayment','#A04000', 15, 1, 1),
(0, '利息',     'expense', NULL, 'interest', '#D4AC0D', 16, 1, 1),
(0, '其他支出', 'expense', NULL, 'other',    '#BDC3C7', 17, 1, 1),
-- 收入分类（一级）
(0, '工资',     'income',  NULL, 'salary',   '#27AE60', 1,  1, 1),
(0, '奖金',     'income',  NULL, 'bonus',    '#F39C12', 2,  1, 1),
(0, '兼职',     'income',  NULL, 'parttime', '#2ECC71', 3,  1, 1),
(0, '理财收益', 'income',  NULL, 'invest',   '#16A085', 4,  1, 1),
(0, '红包',     'income',  NULL, 'redpacket','#E74C3C', 5,  1, 1),
(0, '退款',     'income',  NULL, 'refund',   '#5DADE2', 6,  1, 1),
(0, '借入',     'income',  NULL, 'borrow-in','#7F8C8D', 7,  1, 1),
(0, '二手闲置', 'income',  NULL, 'secondhand','#AF7AC5', 8,  1, 1),
(0, '其他收入', 'income',  NULL, 'other',    '#BDC3C7', 9,  1, 1);

-- ---------------------------------------------------------------------
-- 8. 后台管理菜单（一级目录「记账管理」，菜单ID 5400-5413，避开 52xx CMS 与 53xx 支付区段）
-- 权限标识：cms:ledgerCategory:* / cms:ledgerStats:*
-- ---------------------------------------------------------------------
DELETE FROM `moyun-db`.sys_menu WHERE menu_id IN (5400, 5401, 5402, 5411, 5412, 5413);

-- 一级目录
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5400, '记账管理', 0, 8, 'ledger', null, null, '', 1, 0, 'M', '0', '0', '', 'bookkeeping', 'admin', NOW(), '', null, '记账模块（个人资产管理）', '0');

-- 预设分类管理（页面）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5401, '预设分类', 5400, 1, 'category', 'cms/ledger/category/index', null, '', 1, 0, 'C', '0', '0', 'cms:ledgerCategory:list', 'tree', 'admin', NOW(), '', null, '系统预设收支分类维护', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5411, '分类查询', 5401, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ledgerCategory:query', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5412, '分类新增', 5401, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ledgerCategory:add', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5413, '分类修改', 5401, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ledgerCategory:edit', '#', 'admin', NOW(), '', null, '', '0');

-- 运营统计（页面，脱敏聚合）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5402, '运营统计', 5400, 2, 'stats', 'cms/ledger/stats/index', null, '', 1, 0, 'C', '0', '0', 'cms:ledgerStats:list', 'chart', 'admin', NOW(), '', null, '记账用户/活跃度/类型分布（脱敏聚合）', '0');

-- 菜单授权给超级管理员（role_id=1）
DELETE FROM `moyun-db`.sys_role_menu WHERE role_id = 1 AND menu_id IN (5400, 5401, 5402, 5411, 5412, 5413);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id) VALUES
  (1, 5400), (1, 5401), (1, 5402), (1, 5411), (1, 5412), (1, 5413);
