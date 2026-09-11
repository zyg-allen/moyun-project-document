-- =====================================================================
-- V11.0 公共支付通道（微信支付 + 平台抽成分账）
-- 墨韵智库 · moyun-dev-kouzi 分支
-- 执行库：moyun-db（与既有业务同库）
-- 幂等：建表 IF NOT EXISTS；菜单/配置先 DELETE 再 INSERT，可重复执行
-- 说明：
--   1) 金额单位统一为「元」DECIMAL(18,2)（v11.31 变更，见 20260908-03 迁移脚本）；原 BIGINT 分方案已废止
--   2) 复式记账：pay_ledger_entry 每笔支付拆两条分录（平台抽成 + 用户所得，金额守恒）
--   3) 费率双轨制：sys_config(pay.platform.fee-rate) 运行时配置 优先于 yaml 兜底值
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 支付订单表（公共支付通道核心单据）
-- ---------------------------------------------------------------------
drop table if exists pay_order;
CREATE TABLE IF NOT EXISTS `pay_order` (
                     `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'id',
  `pay_no`           VARCHAR(40)  NOT NULL COMMENT '支付单号（全局唯一，如 PAY20260902xxxx）',
  `biz_type`         VARCHAR(32)  NOT NULL COMMENT '业务类型：tip=打赏 / member=会员 / course=课程（后续扩展）',
  `biz_no`           VARCHAR(64)  NOT NULL COMMENT '业务单号（如打赏单ID）',
  `channel`          VARCHAR(32)  NOT NULL COMMENT '支付渠道：wechat / alipay（预留）',
  `amount`           BIGINT       NOT NULL COMMENT '支付金额（分）',
  `subject`          VARCHAR(128) NOT NULL COMMENT '商品描述',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'CREATED' COMMENT '状态机：CREATED→PAID→SETTLED / CREATED→CLOSED',
  `code_url`         VARCHAR(512) NULL COMMENT '微信 native 支付二维码链接（code_url）',
  `channel_order_no` VARCHAR(64)  NULL COMMENT '三方交易单号（微信 transaction_id）',
  `trade_state`      VARCHAR(32)  NULL COMMENT '三方交易状态（微信 trade_state：SUCCESS/NOTPAY/CLOSED等）',
  `expire_time`      DATETIME     NULL COMMENT '订单过期时间（超时未支付自动关单依据）',
  `pay_success_time` DATETIME     NULL COMMENT '支付成功时间',
  `settle_time`      DATETIME     NULL COMMENT '分账完成时间',
  `closed_time`      DATETIME     NULL COMMENT '关单时间',
  `close_reason`     VARCHAR(64)  NULL COMMENT '关单原因：TIMEOUT / ADMIN_MANUAL_CLOSE',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  primary KEY (`id`),
  unique KEY (`pay_no`),
  KEY `idx_biz` (`biz_type`, `biz_no`),
  KEY `idx_status_expire` (`status`, `expire_time`),
  KEY `idx_channel_order` (`channel_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付订单（公共支付通道）';

-- ---------------------------------------------------------------------
-- 2. 用户资金账户表（钱包余额，单位：分）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_user_account` (
  `user_id`        BIGINT   NOT NULL COMMENT '用户ID（sys_user.user_id）',
  `balance`        BIGINT   NOT NULL DEFAULT 0 COMMENT '可用余额（分）',
  `total_income`   BIGINT   NOT NULL DEFAULT 0 COMMENT '累计收入（分，含打赏所得）',
  `total_withdraw` BIGINT   NOT NULL DEFAULT 0 COMMENT '累计提现（分）',
  `version`        INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户资金账户（钱包）';

-- ---------------------------------------------------------------------
-- 3. 分账流水表（复式记账：平台抽成 / 用户所得）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_ledger_entry` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `pay_no`        VARCHAR(40)  NOT NULL COMMENT '关联支付单号',
  `biz_type`      VARCHAR(32)  NOT NULL COMMENT '业务类型：tip / withdraw',
  `biz_no`        VARCHAR(64)  NOT NULL COMMENT '业务单号',
  `account_role`  VARCHAR(16)  NOT NULL COMMENT '账户角色：USER=用户 / PLATFORM=平台',
  `user_id`       BIGINT       NULL COMMENT '用户ID（PLATFORM 分录为 NULL）',
  `direction`     VARCHAR(8)   NOT NULL COMMENT '方向：credit=收入 / debit=支出',
  `amount`        BIGINT       NOT NULL COMMENT '金额（分）',
  `balance_after` BIGINT       NULL COMMENT '交易后余额（分；PLATFORM 分录不追踪余额，为 NULL）',
  `summary`       VARCHAR(255) NOT NULL COMMENT '业务摘要，如"打赏收入-作者所得" / "平台服务费"',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_pay_no` (`pay_no`),
  KEY `idx_user` (`user_id`, `create_time`),
  KEY `idx_role` (`account_role`, `direction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分账流水（复式记账）';

-- ---------------------------------------------------------------------
-- 4. 用户银行卡表（卡号 AES-GCM 加密存储）
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `pay_user_bank_card` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '银行卡ID',
  `user_id`           BIGINT       NOT NULL COMMENT '所属用户',
  `holder_name`       VARCHAR(64)  NOT NULL COMMENT '持卡人姓名',
  `card_no_encrypted` VARCHAR(512) NOT NULL COMMENT '卡号密文（AES-GCM）',
  `card_no_masked`    VARCHAR(32)  NOT NULL COMMENT '卡号脱敏（6217 **** **** 1234）',
  `phone_encrypted`   VARCHAR(512) NULL COMMENT '预留手机号密文（AES-GCM）',
  `bank_code`         VARCHAR(32)  NULL COMMENT '银行编码（如 ICBC）',
  `bank_name`         VARCHAR(64)  NOT NULL COMMENT '银行名称（如 中国工商银行）',
  `is_default`        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认卡：1=是 0=否',
  `verify_status`     VARCHAR(16)  NOT NULL DEFAULT 'VERIFIED' COMMENT '验证状态：VERIFIED=已验证 / PENDING=待验证',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  UNIQUE KEY `uk_user_card` (`user_id`, `card_no_masked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户银行卡（密文落库）';

-- ---------------------------------------------------------------------
-- 5. 提现订单表（本期预留：钱包余额 → 银行卡）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_withdraw_order` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '提现单ID',
  `withdraw_no`   VARCHAR(40)  NOT NULL COMMENT '提现单号',
  `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
  `bank_card_id`  BIGINT       NOT NULL COMMENT '收款银行卡ID',
  `amount`        BIGINT       NOT NULL COMMENT '提现金额（分）',
  `fee`           BIGINT       NOT NULL DEFAULT 0 COMMENT '手续费（分）',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'AUDITING' COMMENT '状态：AUDITING=审核中 / PAID=已打款 / REJECTED=已驳回',
  `audit_time`    DATETIME     NULL COMMENT '审核时间',
  `reject_reason` VARCHAR(255) NULL COMMENT '驳回原因',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_withdraw_no` (`withdraw_no`),
  KEY `idx_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='提现订单（预留）';

-- ---------------------------------------------------------------------
-- 6. 支付站内通知表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_notification` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id`     BIGINT       NOT NULL COMMENT '接收用户',
  `notify_type` VARCHAR(16)  NOT NULL COMMENT '通知类型：pay=支付结果 / withdraw=提现 / account=账户',
  `ref_no`      VARCHAR(64)  NULL COMMENT '关联单号（支付单/提现单）',
  `title`       VARCHAR(128) NOT NULL COMMENT '通知标题',
  `content`     VARCHAR(512) NOT NULL COMMENT '通知内容',
  `read_flag`   TINYINT      NOT NULL DEFAULT 0 COMMENT '已读：0=未读 1=已读',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `read_flag`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付站内通知';

-- ---------------------------------------------------------------------
-- 7. 渠道回调日志表（排障与审计）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pay_notify_log` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `channel`     VARCHAR(32)   NOT NULL COMMENT '渠道：wechat',
  `pay_no`      VARCHAR(40)   NULL COMMENT '关联支付单（解析成功后回填）',
  `raw_body`    VARCHAR(2048) NULL COMMENT '回调报文（截断留存）',
  `verify_ok`   TINYINT       NOT NULL DEFAULT 0 COMMENT '验签结果：1=通过 0=失败',
  `handled`     TINYINT       NOT NULL DEFAULT 0 COMMENT '业务处理：1=成功 0=失败',
  `error_msg`   VARCHAR(512)  NULL COMMENT '失败原因',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_pay_no` (`pay_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='渠道回调日志';

-- ---------------------------------------------------------------------
-- 平台服务费率（运行时可改，后台"支付配置"页在线调整）
-- 0.10 = 平台抽成 10%，作者得 90%（业内主流 UGC 平台抽成区间 5%~30%）
-- ---------------------------------------------------------------------
DELETE FROM `moyun-db`.sys_config WHERE config_key = 'pay.platform.fee-rate';
INSERT INTO `moyun-db`.sys_config (config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
VALUES ('平台服务费率', 'pay.platform.fee-rate', '0.10', 'Y', 'admin', NOW(), '', null, 'V11.0 支付分账：平台抽成比例（0.10=10%），运行时生效');

-- ---------------------------------------------------------------------
-- 后台管理菜单（一级目录「支付管理」，菜单ID 5300-5314，避开既有 52xx CMS 区段）
-- 权限标识：cms:payOrder:* / cms:payLedger:* / cms:payBankCard:* / cms:payConfig:*
-- ---------------------------------------------------------------------
DELETE FROM `moyun-db`.sys_menu WHERE menu_id IN (5300, 5301, 5302, 5303, 5304, 5311, 5312, 5313, 5314);

-- 一级目录
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5300, '支付管理', 0, 7, 'pay', null, null, '', 1, 0, 'M', '0', '0', '', 'money', 'admin', NOW(), '', null, 'V11.0 公共支付通道', '0');

-- 支付订单（页面）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5301, '支付订单', 5300, 1, 'order', 'cms/pay/order/index', null, '', 1, 0, 'C', '0', '0', 'cms:payOrder:list', 'list', 'admin', NOW(), '', null, '支付单查询/详情/关单', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5311, '支付订单查询', 5301, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payOrder:query', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5312, '支付订单关单', 5301, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payOrder:close', '#', 'admin', NOW(), '', null, '', '0');

-- 分账流水（页面）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5302, '分账流水', 5300, 2, 'ledger', 'cms/pay/ledger/index', null, '', 1, 0, 'C', '0', '0', 'cms:payLedger:list', 'money', 'admin', NOW(), '', null, '平台抽成/用户所得复式记账', '0');

INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5315, '分账明细', 5302, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payLedger:query', '#', 'admin', NOW(), '', null, '单笔支付单分账明细', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5316, '分账汇总', 5302, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payLedger:summary', '#', 'admin', NOW(), '', null, '平台抽成/用户所得汇总卡片', '0');

-- 用户银行卡（页面）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5303, '用户银行卡', 5300, 3, 'bankcard', 'cms/pay/bankcard/index', null, '', 1, 0, 'C', '0', '0', 'cms:payBankCard:list', 'card', 'admin', NOW(), '', null, '脱敏审计视角', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5313, '银行卡详情', 5303, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payBankCard:query', '#', 'admin', NOW(), '', null, '', '0');

-- 支付配置（页面）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5304, '支付配置', 5300, 4, 'config', 'cms/pay/config/index', null, '', 1, 0, 'C', '0', '0', 'cms:payConfig:view', 'edit', 'admin', NOW(), '', null, '通道状态/费率在线调整', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5314, '费率调整', 5304, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payConfig:edit', '#', 'admin', NOW(), '', null, '', '0');

-- 菜单授权给超级管理员（role_id=1）
DELETE FROM `moyun-db`.sys_role_menu WHERE role_id = 1 AND menu_id IN (5300, 5301, 5302, 5303, 5304, 5311, 5312, 5313, 5314, 5315, 5316);
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id) VALUES
  (1, 5300), (1, 5301), (1, 5302), (1, 5303), (1, 5304),
  (1, 5311), (1, 5312), (1, 5313), (1, 5314), (1, 5315), (1, 5316);
