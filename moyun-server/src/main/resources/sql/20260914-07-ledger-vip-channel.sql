-- =====================================================================
-- 20260914-07 记账VIP订阅接入公共支付通道（v11.81）
-- 依据：docs/05-方案设计-分模块/07-支付模块/支付问题以及解决方案v-2.md §8.3
--
-- 链路（平台直收类：用户 → 平台公账，无第三方收款人）：
--   App 会员页选套餐 → POST /portal/ledger/vip/subscribe（快照套餐名/时长，
--   clientUuid 幂等）→ 落 pending 订单 → payGateway.createOrder(bizType='ledger_vip',
--   platform='ledger_app') → 收银台（扫码 / mock 模拟支付）→ 网关回调
--   LedgerVipPayCallbackHandler：pending→paid + 权益顺延
--   （vip_expire = max(now, 现有到期) + duration_days，续费不折损）+
--   settlePlatform 平台全额分账 → 站内通知。
--
-- 变更：
--   1. ledger_vip_package 套餐表（价格/时长后台可配，骨架默认套餐占位可改）
--   2. ledger_vip_order 订阅订单表（快照 + 幂等 + 权益起止）
--   3. 菜单：5400 记账管理下新增 5405 VIP套餐（后台维护价格）
-- =====================================================================

-- 1. 套餐表
CREATE TABLE IF NOT EXISTS `moyun-db`.ledger_vip_package (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
    name          VARCHAR(32)  NOT NULL COMMENT '套餐名（如：月度VIP/年度VIP）',
    price         DECIMAL(18,2) NOT NULL COMMENT '售价（元，后台可配）',
    original_price DECIMAL(18,2) NULL COMMENT '划线原价（元，可空）',
    duration_days INT          NOT NULL COMMENT '时长（天）',
    description   VARCHAR(255) NULL COMMENT '权益说明',
    popular       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否推荐：1=是 0=否',
    sort          INT          NOT NULL DEFAULT 0 COMMENT '排序（小在前）',
    status        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '上架状态：1=上架 0=下架',
    create_time   DATETIME     NULL COMMENT '创建时间',
    update_time   DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '记账VIP套餐（v11.81 平台直收类，价格后台可配）';

-- 2. 订阅订单表
CREATE TABLE IF NOT EXISTS `moyun-db`.ledger_vip_order (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    user_id       BIGINT       NOT NULL COMMENT '门户用户ID（portal_user.id）',
    package_id    BIGINT       NOT NULL COMMENT '套餐ID（ledger_vip_package.id）',
    package_name  VARCHAR(32)  NOT NULL COMMENT '套餐名快照（下单时冻结）',
    duration_days INT          NOT NULL COMMENT '时长快照（天）',
    amount        DECIMAL(18,2) NOT NULL COMMENT '支付金额（元）',
    pay_channel   VARCHAR(16)  NOT NULL DEFAULT 'wechat' COMMENT '支付渠道：wechat/alipay（v11.79 统一命名）',
    pay_no        VARCHAR(64)  NULL COMMENT '关联公共通道单据号（pay_order.pay_no）',
    client_uuid   VARCHAR(64)  NULL COMMENT '客户端幂等号（防重复提交）',
    status        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT '状态：pending=待支付 paid=已支付（权益已发放） refunded=已退款 closed=已关闭（v11.79 统一枚举）',
    vip_start     DATETIME     NULL COMMENT '权益起始（回调置 paid 时计算）',
    vip_expire    DATETIME     NULL COMMENT '权益到期（现有时长顺延：max(now,现有到期)+duration_days）',
    create_time   DATETIME     NULL COMMENT '下单时间',
    paid_time     DATETIME     NULL COMMENT '支付完成时间（网关回调置 paid 时写入）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_client_uuid (client_uuid),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '记账VIP订单（v11.81 接公共通道，bizType=ledger_vip 平台直收类）';

-- 3. 默认套餐（骨架占位价格，后台「记账管理→VIP套餐」可随时调整）
INSERT INTO `moyun-db`.ledger_vip_package (name, price, original_price, duration_days, description, popular, sort, status, create_time)
VALUES ('月度VIP', 15.00, 20.00, 30, 'AI 分析报告、多账本、Excel 导出、云备份（30 天）', 0, 1, 1, NOW()),
       ('年度VIP', 128.00, 180.00, 365, 'AI 分析报告、多账本、Excel 导出、云备份（365 天）', 1, 2, 1, NOW());

-- 4. 后台菜单：记账管理（5400）下新增 VIP套餐（5405）及按钮权限
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag)
VALUES (5405, 'VIP套餐', 5400, 5, 'vipPackage', 'cms/ledger/vipPackage/index', NULL, '', 1, 0, 'C', '0', '0', 'cms:ledgerVip:list', 'crown', 'admin', NOW(), '', NULL, '记账VIP套餐维护（价格/时长/上下架，平台直收类）', '0'),
       (5415, '套餐查询', 5405, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'cms:ledgerVip:query', '#', 'admin', NOW(), '', NULL, '', '0'),
       (5416, '套餐新增', 5405, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'cms:ledgerVip:add', '#', 'admin', NOW(), '', NULL, '', '0'),
       (5417, '套餐修改', 5405, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'cms:ledgerVip:edit', '#', 'admin', NOW(), '', NULL, '', '0'),
       (5418, '套餐删除', 5405, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'cms:ledgerVip:remove', '#', 'admin', NOW(), '', NULL, '', '0');

INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id)
VALUES (1, 5405), (1, 5415), (1, 5416), (1, 5417), (1, 5418);

-- 5. App「我的」页功能宫格新增"记账VIP"入口（v11.81，与内置兜底清单同步）
DELETE FROM `moyun-db`.ledger_app_feature_config WHERE feature_key = 'vip';
INSERT INTO `moyun-db`.ledger_app_feature_config
    (feature_key, feature_name, icon, icon_color, group_type, sort_num, visible, status, badge, remark)
VALUES ('vip', '记账VIP', '👑', '#e6a23c', 'main', 7, 1, 'done', 'VIP', 'VIP订阅（月/年套餐，平台直收）');
