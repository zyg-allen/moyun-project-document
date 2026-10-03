-- ============================================================================
-- 收费功能归档脚本（2026-10-03）
--
-- 背景：平台全站免费化，下线会员/订阅/支付/打赏/提现/结算等全部有偿能力
--       （个体工商户主体无法取得增值电信业务经营许可证，依《互联网信息服务管理办法》
--        第 11 条非经营性互联网信息服务提供者不得从事有偿服务）。
--
-- 处理方式：**归档而非删除** —— 16 张表统一重命名加前缀 `archived_20261003_`，
--           保留历史订单与资金流水（审计需要），业务侧已无任何代码引用。
--           确认长期无需要后可再执行文件末尾的 DROP 语句彻底删除。
--
-- 执行前请务必备份：mysqldump -u<user> -p <db> vip_tier vip_tier_benefit vip_benefit vip_benefit_usage vip_user_card vip_api_registry pay_order pay_notify_log pay_notification pay_user_account pay_user_bank_card pay_withdraw_order pay_ledger_entry portal_tip_order ledger_tip_order portal_creator_settlement > backup_20261003.sql
-- ============================================================================

-- ---------- 一、归档（重命名） ----------
RENAME TABLE
  `vip_tier` TO `archived_20261003_vip_tier`,
  `vip_tier_benefit` TO `archived_20261003_vip_tier_benefit`,
  `vip_benefit` TO `archived_20261003_vip_benefit`,
  `vip_benefit_usage` TO `archived_20261003_vip_benefit_usage`,
  `vip_user_card` TO `archived_20261003_vip_user_card`,
  `vip_api_registry` TO `archived_20261003_vip_api_registry`,
  `pay_order` TO `archived_20261003_pay_order`,
  `pay_notify_log` TO `archived_20261003_pay_notify_log`,
  `pay_notification` TO `archived_20261003_pay_notification`,
  `pay_user_account` TO `archived_20261003_pay_user_account`,
  `pay_user_bank_card` TO `archived_20261003_pay_user_bank_card`,
  `pay_withdraw_order` TO `archived_20261003_pay_withdraw_order`,
  `pay_ledger_entry` TO `archived_20261003_pay_ledger_entry`,
  `portal_tip_order` TO `archived_20261003_portal_tip_order`,
  `ledger_tip_order` TO `archived_20261003_ledger_tip_order`,
  `portal_creator_settlement` TO `archived_20261003_portal_creator_settlement`;

-- ---------- 二、下线后台菜单与字典（收费相关） ----------
-- 后台菜单：支付/钱包/提现/收入/营收/流水/会员 等
DELETE FROM `sys_menu` WHERE `perms` LIKE 'cms:pay%' OR `perms` LIKE 'cms:vip%' OR `perms` LIKE '%:payWithdraw:%'
   OR `menu_name` IN ('支付配置','支付订单','支付流水','收入管理','营收统计','钱包管理','提现审核','会员管理','会员权益','创作者结算');
-- 字典：会员/支付/提现/结算相关
DELETE FROM `sys_dict_data` WHERE `dict_type` IN ('vip_tier','vip_benefit','pay_channel','pay_status','withdraw_status','settlement_status');
DELETE FROM `sys_dict_type` WHERE `dict_type` IN ('vip_tier','vip_benefit','pay_channel','pay_status','withdraw_status','settlement_status');

-- ---------- 三、归档后结构参考（如需恢复：先执行本段，再执行 rename 的逆操作） ----------
/*
CREATE TABLE `vip_tier` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '等级ID',
                            `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码（sys_platform.platform_code）',
                            `tier_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级代码',
                            `tier_name` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '等级名称',
                            `duration_days` int DEFAULT NULL COMMENT '有效天数（-1=永久，0=免费tier）',
                            `price` decimal(18,2) DEFAULT NULL COMMENT '价格（元）',
                            `original_price` decimal(18,2) DEFAULT NULL COMMENT '划线原价（元，可空）',
                            `popular` tinyint DEFAULT '0' COMMENT '是否推荐（1=售卖页推荐展示）',
                            `description` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '等级说明',
                            `sort_order` int DEFAULT '0' COMMENT '排序',
                            `status` tinyint DEFAULT '1' COMMENT '状态（1上架 0下架）',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_platform_tier` (`platform_code`,`tier_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='VIP等级（一端一套，类比 sys_role）';

CREATE TABLE `vip_tier_benefit` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码',
                                    `tier_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级代码',
                                    `benefit_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权益代码',
                                    `benefit_value` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '额度（unlimited 或数字）',
                                    `period` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'month' COMMENT '统计周期（day/month/year/unlimited）',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_tier_benefit` (`platform_code`,`tier_code`,`benefit_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='等级权益关联（类比 sys_role_menu）';

CREATE TABLE `vip_benefit` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权益ID',
                               `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码',
                               `benefit_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权益代码（统一 {action} 命名，端级隔离）',
                               `benefit_name` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '权益名称',
                               `description` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
                               `sort_order` int DEFAULT '0' COMMENT '排序',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_platform_benefit` (`platform_code`,`benefit_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='VIP权益定义（类比 sys_menu）';

CREATE TABLE `vip_benefit_usage` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                     `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码',
                                     `benefit_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权益代码',
                                     `usage_count` int DEFAULT '1' COMMENT '当日累计使用次数',
                                     `usage_date` date DEFAULT NULL COMMENT '使用日期',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_user_benefit_date` (`user_id`,`platform_code`,`benefit_code`,`usage_date`),
                                     KEY `idx_user_date` (`user_id`,`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权益使用记录（consume=true 的次数统计）';

CREATE TABLE `vip_user_card` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员卡ID',
                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                 `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码',
                                 `tier_code` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当前等级代码',
                                 `order_id` bigint DEFAULT NULL COMMENT '最近一次支付订单（pay_order.id）',
                                 `start_time` datetime DEFAULT NULL COMMENT '生效时间',
                                 `expire_time` datetime DEFAULT NULL COMMENT '过期时间（永久为 NULL）',
                                 `status` tinyint DEFAULT '1' COMMENT '状态（1有效 0过期）',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_platform` (`user_id`,`platform_code`),
                                 KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户会员卡（类比 sys_user_role，一端一卡续费顺延）';

CREATE TABLE `vip_api_registry` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `api_path` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接口路径',
                                    `http_method` varchar(10) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'HTTP方法',
                                    `controller_class` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Controller类全名',
                                    `method_name` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '方法名',
                                    `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '端代码',
                                    `benefit_code` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '权益代码',
                                    `consume` tinyint DEFAULT '1' COMMENT '是否消耗次数（1消耗 0仅校验）',
                                    `message` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '校验失败提示',
                                    `api_desc` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '接口描述（运营填写）',
                                    `enabled` tinyint DEFAULT '1' COMMENT '是否启用校验（0=后台禁用该接口校验）',
                                    `scan_time` datetime DEFAULT NULL COMMENT '最近扫描时间',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_api` (`api_path`,`http_method`),
                                    KEY `idx_platform` (`platform_code`),
                                    KEY `idx_benefit` (`benefit_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='VIP接口注册表（@VipOnly 启动扫描生成）';

CREATE TABLE `pay_order` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                             `pay_no` varchar(40) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付单号（全局唯一，如 PAY20260902xxxx）',
                             `biz_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型：tip=打赏 / member=会员 / course=课程（后续扩展）',
                             `biz_no` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务单号（如打赏单ID）',
                             `user_id` bigint DEFAULT NULL COMMENT '下单用户（portal_user.id，v11.79 对账维度）',
                             `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '归属端代码（sys_platform.platform_code）',
                             `channel` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付渠道：wechat / alipay（预留）',
                             `amount` decimal(18,2) NOT NULL COMMENT '支付金额（元）',
                             `subject` varchar(128) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品描述',
                             `status` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CREATED' COMMENT '状态机：CREATED→PAID→SETTLED / CREATED→CLOSED',
                             `code_url` varchar(512) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '微信 native 支付二维码链接（code_url）',
                             `channel_order_no` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '三方交易单号（微信 transaction_id）',
                             `trade_state` varchar(32) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '三方交易状态（微信 trade_state：SUCCESS/NOTPAY/CLOSED等）',
                             `expire_time` datetime DEFAULT NULL COMMENT '订单过期时间（超时未支付自动关单依据）',
                             `pay_success_time` datetime DEFAULT NULL COMMENT '支付成功时间',
                             `settle_time` datetime DEFAULT NULL COMMENT '分账完成时间',
                             `closed_time` datetime DEFAULT NULL COMMENT '关单时间',
                             `close_reason` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关单原因：TIMEOUT / ADMIN_MANUAL_CLOSE',
                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `pay_no` (`pay_no`),
                             KEY `idx_biz` (`biz_type`,`biz_no`),
                             KEY `idx_status_expire` (`status`,`expire_time`),
                             KEY `idx_channel_order` (`channel_order_no`),
                             KEY `idx_pay_order_user` (`user_id`),
                             KEY `idx_pay_order_platform` (`platform_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单（公共支付通道）';

CREATE TABLE `pay_notify_log` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                  `channel` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道：wechat',
                                  `pay_no` varchar(40) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联支付单（解析成功后回填）',
                                  `raw_body` varchar(2048) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '回调报文（截断留存）',
                                  `verify_ok` tinyint NOT NULL DEFAULT '0' COMMENT '验签结果：1=通过 0=失败',
                                  `handled` tinyint NOT NULL DEFAULT '0' COMMENT '业务处理：1=成功 0=失败',
                                  `error_msg` varchar(512) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '失败原因',
                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '归属端代码',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_pay_no` (`pay_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='渠道回调日志';

CREATE TABLE `pay_notification` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
                                    `user_id` bigint NOT NULL COMMENT '接收用户',
                                    `notify_type` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知类型：pay=支付结果 / withdraw=提现 / account=账户',
                                    `ref_no` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联单号（支付单/提现单）',
                                    `title` varchar(128) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
                                    `content` varchar(512) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知内容',
                                    `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '已读：0=未读 1=已读',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '归属端代码',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_user_read` (`user_id`,`read_flag`,`create_time`),
                                    KEY `idx_pay_notification_platform` (`platform_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付站内通知';

CREATE TABLE `pay_user_account` (
                                    `user_id` bigint NOT NULL COMMENT '用户ID（sys_user.user_id）',
                                    `balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '可用余额（元）',
                                    `total_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计收入（元，含打赏所得）',
                                    `total_withdraw` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计提现（元）',
                                    `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户资金账户（钱包）';

CREATE TABLE `pay_user_bank_card` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '银行卡ID',
                                      `user_id` bigint NOT NULL COMMENT '所属用户',
                                      `holder_name` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '持卡人姓名',
                                      `card_no_encrypted` varchar(512) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡号密文（AES-GCM）',
                                      `card_no_masked` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡号脱敏（6217 **** **** 1234）',
                                      `phone_encrypted` varchar(512) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '预留手机号密文（AES-GCM）',
                                      `bank_code` varchar(32) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '银行编码（如 ICBC）',
                                      `bank_name` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '银行名称（如 中国工商银行）',
                                      `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认卡：1=是 0=否',
                                      `verify_status` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '验证状态：PENDING=待验证 / VERIFIED=已验证 / REJECTED=已驳回（v13.18 默认值改 fail-closed：直插 SQL 不得绕过四要素核验）',
                                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_user_card` (`user_id`,`card_no_masked`),
                                      KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户银行卡（密文落库）';

CREATE TABLE `pay_withdraw_order` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '提现单ID',
                                      `withdraw_no` varchar(40) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现单号',
                                      `user_id` bigint NOT NULL COMMENT '用户ID',
                                      `bank_card_id` bigint NOT NULL COMMENT '收款银行卡ID',
                                      `amount` decimal(18,2) NOT NULL COMMENT '提现金额（元）',
                                      `fee` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '手续费（元）',
                                      `status` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'auditing' COMMENT '状态：auditing=审核中 paid=已打款 rejected=已驳回（v11.79 统一小写）',
                                      `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
                                      `reject_reason` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '驳回原因',
                                      `paid_time` datetime DEFAULT NULL COMMENT '打款完成时间（真实出金到账）',
                                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '归属端代码',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_withdraw_no` (`withdraw_no`),
                                      KEY `idx_user` (`user_id`,`create_time`),
                                      KEY `idx_pay_withdraw_platform` (`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提现订单（预留）';

CREATE TABLE `pay_ledger_entry` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
                                    `pay_no` varchar(40) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联支付单号',
                                    `biz_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型：tip / withdraw',
                                    `biz_no` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务单号',
                                    `account_role` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账户角色：USER=用户 / PLATFORM=平台',
                                    `user_id` bigint DEFAULT NULL COMMENT '用户ID（PLATFORM 分录为 NULL）',
                                    `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '归属端代码',
                                    `direction` varchar(8) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '方向：credit=收入 / debit=支出',
                                    `amount` decimal(18,2) NOT NULL COMMENT '金额（元）',
                                    `balance_after` decimal(18,2) DEFAULT NULL COMMENT '交易后余额（元；PLATFORM 分录不追踪余额，为 NULL）',
                                    `summary` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务摘要，如"打赏收入-作者所得" / "平台服务费"',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_pay_no` (`pay_no`),
                                    KEY `idx_user` (`user_id`,`create_time`),
                                    KEY `idx_role` (`account_role`,`direction`),
                                    KEY `idx_pay_ledger_platform` (`platform_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分账流水（复式记账）';

CREATE TABLE `portal_tip_order` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `user_id` bigint NOT NULL COMMENT '打赏者用户ID',
                                    `author_id` bigint NOT NULL COMMENT '被打赏者用户ID',
                                    `target_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '打赏对象类型 article/column/article_paid',
                                    `target_id` bigint NOT NULL COMMENT '打赏对象ID',
                                    `amount` decimal(18,2) NOT NULL COMMENT '打赏金额',
                                    `message` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '打赏留言',
                                    `status` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '状态 pending/paid/refunded',
                                    `pay_method` varchar(32) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '支付方式',
                                    `trade_no` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '第三方交易号（支付宝/微信返回的交易号）',
                                    `pay_channel` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'points' COMMENT '支付渠道：points-积分/alipay-支付宝/wechat-微信支付',
                                    `notify_id` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '支付回调ID（用于回调验签与幂等去重）',
                                    `notify_time` datetime DEFAULT NULL COMMENT '支付回调时间',
                                    `refund_no` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '退款单号',
                                    `refund_amount` decimal(18,2) DEFAULT NULL COMMENT '退款金额',
                                    `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
                                    `refund_reason` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '退款原因',
                                    `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
                                    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_author` (`author_id`),
                                    KEY `idx_target` (`target_type`,`target_id`),
                                    KEY `idx_user` (`user_id`),
                                    KEY `idx_trade_no` (`trade_no`),
                                    KEY `idx_pay_channel_status` (`pay_channel`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打赏订单（复用为付费阅读购买记录，target_type=article_paid）';

CREATE TABLE `ledger_tip_order` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '打赏单ID',
                                    `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
                                    `amount` decimal(18,2) NOT NULL COMMENT '打赏金额（元）',
                                    `target` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'developer' COMMENT '打赏对象：developer=开发者 platform=平台',
                                    `reason` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '打赏理由（可选）',
                                    `pay_channel` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'wechat' COMMENT '支付渠道：wechat/alipay（v11.79 与 portal_tip_order 统一命名）',
                                    `pay_no` varchar(40) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联公共通道单据号（pay_order.pay_no；演示模式为空）',
                                    `client_uuid` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端幂等号（防重复提交，v11.80 对齐记一笔机制）',
                                    `status` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '状态：pending=待支付 paid=已支付（网关回调推进） refunded=已退款 closed=已关闭（v11.80 接公共通道）',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打赏时间',
                                    `paid_time` datetime DEFAULT NULL COMMENT '支付完成时间（网关回调置 paid 时写入，v11.80）',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_user_client` (`user_id`,`client_uuid`),
                                    KEY `idx_user` (`user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-打赏记录';

CREATE TABLE `portal_creator_settlement` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `creator_id` bigint NOT NULL COMMENT '创作者用户ID',
                                             `period` varchar(16) NOT NULL COMMENT '结算周期，格式 yyyy-MM，如 2026-07',
                                             `tip_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '打赏收入（当月已支付打赏总额）',
                                             `paid_read_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '付费阅读收入（当月已支付购买总额）',
                                             `column_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '专栏订阅收入（当月已支付订阅总额）',
                                             `total_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '总收入（三项之和）',
                                             `platform_fee` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '平台抽成（total_income * platform_fee_rate）',
                                             `creator_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '创作者实得（total_income - platform_fee）',
                                             `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '状态 pending/confirmed/paid',
                                             `paid_time` datetime DEFAULT NULL COMMENT '打款时间',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_creator_period` (`creator_id`,`period`),
                                             KEY `idx_creator` (`creator_id`),
                                             KEY `idx_period` (`period`),
                                             KEY `idx_status` (`status`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创作者分成结算';

*/

-- ---------- 四、确认无需保留后，可彻底删除（谨慎，二选一） ----------
/*
DROP TABLE IF EXISTS `archived_20261003_vip_tier`;
DROP TABLE IF EXISTS `archived_20261003_vip_tier_benefit`;
DROP TABLE IF EXISTS `archived_20261003_vip_benefit`;
DROP TABLE IF EXISTS `archived_20261003_vip_benefit_usage`;
DROP TABLE IF EXISTS `archived_20261003_vip_user_card`;
DROP TABLE IF EXISTS `archived_20261003_vip_api_registry`;
DROP TABLE IF EXISTS `archived_20261003_pay_order`;
DROP TABLE IF EXISTS `archived_20261003_pay_notify_log`;
DROP TABLE IF EXISTS `archived_20261003_pay_notification`;
DROP TABLE IF EXISTS `archived_20261003_pay_user_account`;
DROP TABLE IF EXISTS `archived_20261003_pay_user_bank_card`;
DROP TABLE IF EXISTS `archived_20261003_pay_withdraw_order`;
DROP TABLE IF EXISTS `archived_20261003_pay_ledger_entry`;
DROP TABLE IF EXISTS `archived_20261003_portal_tip_order`;
DROP TABLE IF EXISTS `archived_20261003_ledger_tip_order`;
DROP TABLE IF EXISTS `archived_20261003_portal_creator_settlement`;
*/