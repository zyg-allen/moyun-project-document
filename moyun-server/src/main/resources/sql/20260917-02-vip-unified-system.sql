-- ============================================================
-- VIP 统一体系（端级粒度 + 全局公共端 + 注解驱动）
-- 依据：《VIP 体系完整设计方案 v2.1》docs/05-方案设计-分模块/3-vip体系/
-- 版本：v12.0 | 日期：2026-09-17
-- 内容：
--   1. sys_platform 端定义（全局公共）+ 门户/记账/管理/人格分析 4 端初始化
--   2. VIP 七表：vip_tier / vip_benefit / vip_tier_benefit / vip_user_card
--      / vip_benefit_usage / vip_api_registry
--   3. 初始化数据：门户端 4 等级 6 权益、记账端 2 等级 2 权益、等级权益矩阵
--   4. pay_ledger_entry 补 platform 列（pay_order.platform 复用，不重复加列）
--   5. sys_config 补 platform_code 列（NULL=全局）+ vip.enabled 全局开关（默认 false）
--   6. 旧体系清理（开发阶段直接删除，不做迁移）：
--      DROP 8 张旧表（三套套餐/订单 + portal_free_trial + 预留 portal_vip_package）
--   7. 菜单：删旧三个套餐管理菜单（5405/5465/5479 及子按钮），
--      新增 端管理 + VIP管理（等级/权益/等级权益/接口注册/会员卡/使用统计）
-- 回调约定：bizType='vip'，bizNo='platform:tier:clientUuid'（网关按 bizType+bizNo
--   幂等复用，唯一段保证重复购买可再下单；回调 split(":", 3) 解析）
-- ============================================================

-- ---------- 1. 端定义（全局公共表） ----------
CREATE TABLE IF NOT EXISTS `moyun-db`.sys_platform (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '端ID',
    platform_code VARCHAR(50) NOT NULL COMMENT '端代码',
    platform_name VARCHAR(50) COMMENT '端名称',
    platform_type VARCHAR(20) COMMENT '端类型：c端/b端',
    description VARCHAR(200) COMMENT '描述',
    domain VARCHAR(100) COMMENT '绑定域名',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态（1启用 0停用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_platform_code (platform_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '端定义（全局公共，用户/支付/VIP/配置/菜单/统计统一引用）';

INSERT INTO `moyun-db`.sys_platform (id, platform_code, platform_name, platform_type, description, domain, icon, sort_order, status, create_time) VALUES
(1, 'portal', '门户端', 'c端', '求职、学习、成长', 'www.xulin.com', 'portal', 1, 1, NOW()),
(2, 'ledger', '记账端', 'c端', '个人资产管理', 'ledger.xulin.com', 'ledger', 2, 1, NOW()),
(3, 'admin', '管理端', 'b端', '后台管理', 'admin.xulin.com', 'admin', 3, 1, NOW()),
(4, 'personality', '人格分析端', 'c端', 'AI人格分析（预留）', 'me.xulin.com', 'peoples', 4, 1, NOW());

-- ---------- 2. VIP 核心表 ----------
-- 等级（类比 sys_role）
CREATE TABLE IF NOT EXISTS `moyun-db`.vip_tier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '等级ID',
    platform_code VARCHAR(50) NOT NULL COMMENT '端代码（sys_platform.platform_code）',
    tier_code VARCHAR(50) NOT NULL COMMENT '等级代码',
    tier_name VARCHAR(50) COMMENT '等级名称',
    duration_days INT COMMENT '有效天数（-1=永久，0=免费tier）',
    price DECIMAL(18,2) COMMENT '价格（元）',
    original_price DECIMAL(18,2) COMMENT '划线原价（元，可空）',
    popular TINYINT DEFAULT 0 COMMENT '是否推荐（1=售卖页推荐展示）',
    description VARCHAR(255) COMMENT '等级说明',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态（1上架 0下架）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_platform_tier (platform_code, tier_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'VIP等级（一端一套，类比 sys_role）';

-- 权益（类比 sys_menu）
CREATE TABLE IF NOT EXISTS `moyun-db`.vip_benefit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权益ID',
    platform_code VARCHAR(50) NOT NULL COMMENT '端代码',
    benefit_code VARCHAR(50) NOT NULL COMMENT '权益代码（统一 {action} 命名，端级隔离）',
    benefit_name VARCHAR(100) COMMENT '权益名称',
    description VARCHAR(200) COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_platform_benefit (platform_code, benefit_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'VIP权益定义（类比 sys_menu）';

-- 等级权益关联（类比 sys_role_menu）
CREATE TABLE IF NOT EXISTS `moyun-db`.vip_tier_benefit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    platform_code VARCHAR(50) NOT NULL COMMENT '端代码',
    tier_code VARCHAR(50) NOT NULL COMMENT '等级代码',
    benefit_code VARCHAR(50) NOT NULL COMMENT '权益代码',
    benefit_value VARCHAR(100) NOT NULL COMMENT '额度（unlimited 或数字）',
    period VARCHAR(20) DEFAULT 'month' COMMENT '统计周期（day/month/year/unlimited）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_tier_benefit (platform_code, tier_code, benefit_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '等级权益关联（类比 sys_role_menu）';

-- 用户会员卡（类比 sys_user_role）
CREATE TABLE IF NOT EXISTS `moyun-db`.vip_user_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会员卡ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    platform_code VARCHAR(50) NOT NULL COMMENT '端代码',
    tier_code VARCHAR(50) NOT NULL COMMENT '当前等级代码',
    order_id BIGINT COMMENT '最近一次支付订单（pay_order.id）',
    start_time DATETIME COMMENT '生效时间',
    expire_time DATETIME COMMENT '过期时间（永久为 NULL）',
    status TINYINT DEFAULT 1 COMMENT '状态（1有效 0过期）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_platform (user_id, platform_code),
    INDEX idx_expire (expire_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户会员卡（类比 sys_user_role，一端一卡续费顺延）';

-- 权益使用记录（Redis 计数异步落库）
CREATE TABLE IF NOT EXISTS `moyun-db`.vip_benefit_usage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    platform_code VARCHAR(50) NOT NULL COMMENT '端代码',
    benefit_code VARCHAR(50) NOT NULL COMMENT '权益代码',
    usage_count INT DEFAULT 1 COMMENT '当日累计使用次数',
    usage_date DATE COMMENT '使用日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_benefit_date (user_id, platform_code, benefit_code, usage_date),
    INDEX idx_user_date (user_id, usage_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权益使用记录（consume=true 的次数统计）';

-- 接口注册表（启动扫描 upsert 生成，运营可编辑）
CREATE TABLE IF NOT EXISTS `moyun-db`.vip_api_registry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    api_path VARCHAR(200) NOT NULL COMMENT '接口路径',
    http_method VARCHAR(10) COMMENT 'HTTP方法',
    controller_class VARCHAR(200) COMMENT 'Controller类全名',
    method_name VARCHAR(100) COMMENT '方法名',
    platform_code VARCHAR(50) COMMENT '端代码',
    benefit_code VARCHAR(50) COMMENT '权益代码',
    consume TINYINT DEFAULT 1 COMMENT '是否消耗次数（1消耗 0仅校验）',
    message VARCHAR(200) COMMENT '校验失败提示',
    api_desc VARCHAR(200) COMMENT '接口描述（运营填写）',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用校验（0=后台禁用该接口校验）',
    scan_time DATETIME COMMENT '最近扫描时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_api (api_path, http_method),
    INDEX idx_platform (platform_code),
    INDEX idx_benefit (benefit_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'VIP接口注册表（@VipOnly 启动扫描生成）';

-- ---------- 3. 初始化数据 ----------
-- 门户端等级
INSERT INTO `moyun-db`.vip_tier (platform_code, tier_code, tier_name, duration_days, price, original_price, popular, description, sort_order, status) VALUES
('portal', 'free', '免费版', 0, 0.00, NULL, 0, '基础体验额度', 1, 1),
('portal', 'monthly', '月卡会员', 30, 49.00, 69.00, 0, '全功能月度畅用', 2, 1),
('portal', 'yearly', '年卡会员', 365, 399.00, 588.00, 1, '最受欢迎，全年畅用', 3, 1),
('portal', 'permanent', '永久会员', -1, 1299.00, 1999.00, 0, '一次买断终身可用', 4, 1);

-- 记账端等级
INSERT INTO `moyun-db`.vip_tier (platform_code, tier_code, tier_name, duration_days, price, original_price, popular, description, sort_order, status) VALUES
('ledger', 'free', '免费版', 0, 0.00, NULL, 0, '基础记账体验', 1, 1),
('ledger', 'yearly', '年卡会员', 365, 199.00, 299.00, 1, '智能账单识别 + AI 分析', 2, 1);

-- 门户端权益
INSERT INTO `moyun-db`.vip_benefit (platform_code, benefit_code, benefit_name, description, sort_order) VALUES
('portal', 'interview_unlimited', '语音面试', '不限次 AI 语音面试', 1),
('portal', 'resume_optimize', '简历深度优化', 'AI 逐项建议/前后对比/采纳保存', 2),
('portal', 'report_share', '报告分享', '面试报告分享导出', 3),
('portal', 'priority_queue', '优先队列', '面试优先调度', 4),
('portal', 'reading_unlimited', '读书空间', '不限次阅读', 5),
('portal', 'article_paid', '付费文章', '免费阅读付费文章', 6);

-- 记账端权益
INSERT INTO `moyun-db`.vip_benefit (platform_code, benefit_code, benefit_name, description, sort_order) VALUES
('ledger', 'bill_parse', '账单识别', '每月账单截图识别次数', 1),
('ledger', 'ai_analysis', 'AI 分析', 'AI 财务分析次数', 2);

-- 门户端等级权益矩阵（free 计数完全替代原 portal_free_trial 免费体验）
INSERT INTO `moyun-db`.vip_tier_benefit (platform_code, tier_code, benefit_code, benefit_value, period) VALUES
('portal', 'free', 'interview_unlimited', '2', 'unlimited'),
('portal', 'free', 'resume_optimize', '1', 'month'),
('portal', 'free', 'reading_unlimited', '3', 'month'),
('portal', 'monthly', 'interview_unlimited', 'unlimited', 'unlimited'),
('portal', 'monthly', 'resume_optimize', '5', 'month'),
('portal', 'monthly', 'report_share', 'unlimited', 'unlimited'),
('portal', 'monthly', 'reading_unlimited', 'unlimited', 'unlimited'),
('portal', 'monthly', 'article_paid', 'unlimited', 'unlimited'),
('portal', 'yearly', 'interview_unlimited', 'unlimited', 'unlimited'),
('portal', 'yearly', 'resume_optimize', '20', 'month'),
('portal', 'yearly', 'report_share', 'unlimited', 'unlimited'),
('portal', 'yearly', 'priority_queue', 'unlimited', 'unlimited'),
('portal', 'yearly', 'reading_unlimited', 'unlimited', 'unlimited'),
('portal', 'yearly', 'article_paid', 'unlimited', 'unlimited'),
('portal', 'permanent', 'interview_unlimited', 'unlimited', 'unlimited'),
('portal', 'permanent', 'resume_optimize', 'unlimited', 'unlimited'),
('portal', 'permanent', 'report_share', 'unlimited', 'unlimited'),
('portal', 'permanent', 'priority_queue', 'unlimited', 'unlimited'),
('portal', 'permanent', 'reading_unlimited', 'unlimited', 'unlimited'),
('portal', 'permanent', 'article_paid', 'unlimited', 'unlimited');

-- 记账端等级权益矩阵
INSERT INTO `moyun-db`.vip_tier_benefit (platform_code, tier_code, benefit_code, benefit_value, period) VALUES
('ledger', 'free', 'bill_parse', '5', 'month'),
('ledger', 'free', 'ai_analysis', '3', 'month'),
('ledger', 'yearly', 'bill_parse', '100', 'month'),
('ledger', 'yearly', 'ai_analysis', 'unlimited', 'unlimited');

-- ---------- 4. 支付体系补列 ----------
-- pay_order.platform 已存在（20260914-05 添加，值 portal/ledger），复用
ALTER TABLE `moyun-db`.pay_ledger_entry
    ADD COLUMN platform VARCHAR(16) NULL COMMENT '归属端：portal/ledger（按端归集统计）' AFTER user_id;

-- ---------- 5. 配置体系补列 + 全局开关 ----------
ALTER TABLE `moyun-db`.sys_config
    ADD COLUMN platform_code VARCHAR(50) DEFAULT NULL COMMENT '归属端（NULL=全局，端级优先全局兜底）' AFTER config_value;

DELETE FROM `moyun-db`.sys_config WHERE config_key = 'vip.enabled';
INSERT INTO `moyun-db`.sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark, platform_code) VALUES
('VIP体系开关', 'vip.enabled', 'false', 'Y', 'admin', NOW(), '统一VIP体系总开关：true启用校验 false全员放行（灰度上线用）；端级可覆盖（platform_code=portal/ledger）', NULL);

-- ---------- 6. 旧体系清理（开发阶段直接删除，不做迁移，不保留旧数据） ----------
DROP TABLE IF EXISTS `moyun-db`.portal_interview_vip_package;
DROP TABLE IF EXISTS `moyun-db`.portal_interview_vip_order;
DROP TABLE IF EXISTS `moyun-db`.portal_resume_optimize_package;
DROP TABLE IF EXISTS `moyun-db`.portal_resume_optimize_order;
DROP TABLE IF EXISTS `moyun-db`.ledger_vip_package;
DROP TABLE IF EXISTS `moyun-db`.ledger_vip_order;
DROP TABLE IF EXISTS `moyun-db`.portal_free_trial;
DROP TABLE IF EXISTS `moyun-db`.portal_vip_package;
-- 旧订单数据随 bizType=interview_vip/resume_optimize/ledger_vip 的 pay_order 记录不迁移（开发库可直接清理）
DELETE FROM `moyun-db`.pay_order WHERE biz_type IN ('interview_vip', 'resume_optimize', 'ledger_vip');

-- ---------- 7. 菜单清理与新增 ----------
-- 7.1 删除旧三个套餐管理菜单及其按钮（配置级表：仅删自身+子按钮，无其他引用）
DELETE FROM `moyun-db`.sys_menu WHERE parent_id IN (5405, 5465, 5479);
DELETE FROM `moyun-db`.sys_menu WHERE menu_id IN (5405, 5465, 5479);
DELETE FROM `moyun-db`.sys_role_menu WHERE menu_id IN (5405, 5465, 5479)
    OR menu_id IN (SELECT t.menu_id FROM (SELECT menu_id FROM `moyun-db`.sys_menu WHERE parent_id IN (5405, 5465, 5479)) t);

-- 7.2 新菜单挂"系统设置"目录（menu_id 运行时解析，不写死）
SET @sys_set = (SELECT menu_id FROM `moyun-db`.sys_menu WHERE menu_name = '系统设置' AND parent_id = 0 AND menu_type = 'M' LIMIT 1);

DELETE FROM `moyun-db`.sys_menu WHERE menu_id BETWEEN 5500 AND 5529;
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES
(5500, '端管理', @sys_set, 8, 'platform', 'system/platform/index', NULL, '', 1, 0, 'C', '0', '0', 'system:platform:list', 'tree', 'admin', NOW(), '', NULL, '全局端定义管理（门户/记账/管理/人格分析），用户/支付/VIP/配置/统计统一引用', '0'),
(5501, 'VIP管理', @sys_set, 9, 'vip', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'crown', 'admin', NOW(), '', NULL, '统一VIP体系管理目录', '0'),
(5502, '等级管理', 5501, 1, 'tier', 'system/vip/tier/index', NULL, '', 1, 0, 'C', '0', '0', 'system:vip:tier:list', 'peoples', 'admin', NOW(), '', NULL, 'VIP等级（一端一套，价格/时长/上下架）', '0'),
(5503, '权益管理', 5501, 2, 'benefit', 'system/vip/benefit/index', NULL, '', 1, 0, 'C', '0', '0', 'system:vip:benefit:list', 'button', 'admin', NOW(), '', NULL, 'VIP权益定义', '0'),
(5504, '等级权益配置', 5501, 3, 'tierBenefit', 'system/vip/tierBenefit/index', NULL, '', 1, 0, 'C', '0', '0', 'system:vip:tierBenefit:list', 'checkbox', 'admin', NOW(), '', NULL, '等级×权益额度矩阵（free 计数替代免费体验）', '0'),
(5505, '接口注册管理', 5501, 4, 'registry', 'system/vip/registry/index', NULL, '', 1, 0, 'C', '0', '0', 'system:vip:registry:list', 'monitor', 'admin', NOW(), '', NULL, '@VipOnly 接口注册表（启动扫描生成，可禁用/重扫）', '0'),
(5506, '用户会员卡', 5501, 5, 'card', 'system/vip/card/index', NULL, '', 1, 0, 'C', '0', '0', 'system:vip:card:list', 'idcard', 'admin', NOW(), '', NULL, '用户会员卡查询/管理', '0'),
(5507, '权益使用统计', 5501, 6, 'usage', 'system/vip/usage/index', NULL, '', 1, 0, 'C', '0', '0', 'system:vip:usage:list', 'chart', 'admin', NOW(), '', NULL, '权益使用记录统计', '0'),
-- 按钮权限：等级管理
(5510, '等级新增', 5502, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:vip:tier:add', '#', 'admin', NOW(), '', NULL, '', '0'),
(5511, '等级修改', 5502, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:vip:tier:edit', '#', 'admin', NOW(), '', NULL, '', '0'),
(5512, '等级删除', 5502, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:vip:tier:remove', '#', 'admin', NOW(), '', NULL, '', '0'),
-- 按钮权限：权益管理
(5513, '权益新增', 5503, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:vip:benefit:add', '#', 'admin', NOW(), '', NULL, '', '0'),
(5514, '权益修改', 5503, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:vip:benefit:edit', '#', 'admin', NOW(), '', NULL, '', '0'),
(5515, '权益删除', 5503, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:vip:benefit:remove', '#', 'admin', NOW(), '', NULL, '', '0'),
-- 按钮权限：等级权益配置 / 接口注册 / 会员卡
(5516, '权益配置保存', 5504, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:vip:tierBenefit:edit', '#', 'admin', NOW(), '', NULL, '', '0'),
(5517, '接口校验启停', 5505, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:vip:registry:edit', '#', 'admin', NOW(), '', NULL, '', '0'),
(5518, '接口重新扫描', 5505, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:vip:registry:scan', '#', 'admin', NOW(), '', NULL, '', '0'),
(5519, '会员卡作废', 5506, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'system:vip:card:remove', '#', 'admin', NOW(), '', NULL, '', '0');

-- 管理员角色授权
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM `moyun-db`.sys_menu WHERE menu_id BETWEEN 5500 AND 5519
ON DUPLICATE KEY UPDATE role_id = 1;

-- ============================================================
-- 执行后验证：
--   1) SELECT * FROM sys_platform;                          -- 4 端
--   2) SELECT platform_code, COUNT(*) FROM vip_tier GROUP BY platform_code;   -- portal 4 / ledger 2
--   3) 重启后端 → vip_api_registry 自动扫描入库
-- ============================================================
