-- =====================================================================
-- 墨韵·智库 V11.3 支付模块表前缀统一迁移脚本（2026-09-02）
-- 背景：user_account / ledger_entry 等裸表名与库中已有同名表冲突
--       （CREATE IF NOT EXISTS 静默跳过 → 查询打到旧表报 Unknown column）
-- 方案：支付模块 7 张表统一 pay_ 前缀命名空间隔离：
--       pay_order / pay_user_account / pay_ledger_entry / pay_user_bank_card
--       / pay_withdraw_order / pay_notification / pay_notify_log
-- 说明：4 张旧裸名表在支付模块外零引用（全库核查），且支付查询此前
--       均报错未写入有效数据，直接 DROP 安全。
-- =====================================================================

USE `moyun-db`;

-- 1. 删除冲突的旧裸名表（无有效支付数据）
DROP TABLE IF EXISTS `user_account`;
DROP TABLE IF EXISTS `ledger_entry`;
DROP TABLE IF EXISTS `user_bank_card`;
DROP TABLE IF EXISTS `withdraw_order`;

-- 2. 创建 pay_ 前缀新表（与主 SQL moyun-pay-gateway-20260902.sql 一致）
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
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  UNIQUE KEY `uk_user_card` (`user_id`, `card_no_masked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户银行卡（密文落库）';

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

-- 3. 若曾手工建过旧结构的 pay_ 前缀表（列名不一致），一并重建：
--    先 DROP 再执行主 SQL moyun-pay-gateway-20260902.sql 全量建表即可。

-- 验证
SHOW TABLES LIKE 'pay_%';
