-- ============================================================
-- 20260914-02 记账后台管理扩展：用户维度管理 + 小程序功能可视化配置
-- 内容：
--   1) ai_execute_log 补 user_id 列（AI 消费按用户统计）
--   2) ledger_app_feature_config 表（ledger-app"我的"页功能入口配置）
--   3) 菜单：记账管理下新增"用户管理"/"功能配置"
-- ============================================================

-- 1. AI 执行日志补用户维度（网关 request.getUserId() 直取，存量留空）
ALTER TABLE `moyun-db`.ai_execute_log
    ADD COLUMN user_id bigint DEFAULT NULL COMMENT '发起用户ID（网关请求方）' AFTER scene_code,
    ADD INDEX idx_user_scene (user_id, scene_code);

-- 2. ledger-app 功能入口配置表（"我的"页 main/recommend 两组宫格）
CREATE TABLE IF NOT EXISTS `moyun-db`.ledger_app_feature_config (
    id           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    feature_key  varchar(50)  NOT NULL COMMENT '功能标识（前端路由映射键）',
    feature_name varchar(50)  NOT NULL COMMENT '功能名称',
    icon         varchar(50)  DEFAULT NULL COMMENT '图标（emoji/符号）',
    icon_color   varchar(20)  DEFAULT NULL COMMENT '图标颜色',
    group_type   varchar(20)  NOT NULL DEFAULT 'main' COMMENT '分组: main-主功能宫格/recommend-推荐小功能',
    sort_num     int          NOT NULL DEFAULT 0 COMMENT '组内排序（升序）',
    visible      tinyint      NOT NULL DEFAULT 1 COMMENT '是否展示: 1-展示 0-隐藏',
    status       varchar(10)  NOT NULL DEFAULT 'done' COMMENT '状态: done-已上线/dev-开发中',
    badge        varchar(20)  DEFAULT NULL COMMENT '角标文案（NEW 等）',
    create_by    varchar(64)  DEFAULT '' COMMENT '创建者',
    create_time  datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by    varchar(64)  DEFAULT '' COMMENT '更新者',
    update_time  datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark       varchar(200) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_feature_key (feature_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '记账小程序功能入口配置（可视化运营）';

-- 3. 种子数据：与 mine 页面内置清单对齐；开发中默认 visible=0（不展示）
INSERT INTO `moyun-db`.ledger_app_feature_config
    (feature_key, feature_name, icon, icon_color, group_type, sort_num, visible, status, badge, remark) VALUES
-- 主功能宫格（已上线 8 项）
('category',   '分类管理', '☰',  '#7fbf94', 'main', 1, 1, 'done', NULL, '用户自定义收支分类'),
('setting',    '记账设置', '⚙️', '#7fbf94', 'main', 2, 1, 'done', NULL, ''),
('savings',    '存钱计划', '🏦', '#7fbf94', 'main', 3, 1, 'done', NULL, ''),
('schedule',   '定时记账', '⏰', '#7fbf94', 'main', 4, 1, 'done', NULL, ''),
('feedback',   '意见反馈', '💬', '#7fbf94', 'main', 5, 1, 'done', NULL, ''),
('tip',        '赞赏',     '🎁', '#7fbf94', 'main', 6, 1, 'done', NULL, ''),
('personalize','个性化',   '🎨', '#e57373', 'main', 7, 1, 'done', 'NEW', '主题/外观个性化'),
('catIcon',    '分类图标', '🎭', '#7fbf94', 'main', 8, 1, 'done', 'NEW', '分类图标选择'),
-- 主功能宫格（开发中 11 项：默认隐藏，上线后由后台开启）
('auto',       '自动记账', '🗒️', '#7fbf94', 'main', 20, 0, 'dev', NULL, ''),
('backup',     '数据备份', '☁️', '#7fbf94', 'main', 21, 0, 'dev', NULL, ''),
('import',     '导入数据', '⬇️', '#7fbf94', 'main', 22, 0, 'dev', NULL, ''),
('export',     '导出数据', '⬆️', '#7fbf94', 'main', 23, 0, 'dev', NULL, ''),
('widget',     '小组件',   '▦',  '#7fbf94', 'main', 24, 0, 'dev', NULL, ''),
('tag',        '标签管理', '🏷️', '#7fbf94', 'main', 25, 0, 'dev', NULL, ''),
('remind',     '记账提醒', '🔔', '#7fbf94', 'main', 26, 0, 'dev', NULL, ''),
('reimburse',  '报销账单', '🧾', '#7fbf94', 'main', 27, 0, 'dev', NULL, ''),
('share',      '分享应用', '📤', '#7fbf94', 'main', 28, 0, 'dev', NULL, ''),
('rate',       '给个好评', '⭐', '#7fbf94', 'main', 29, 0, 'dev', NULL, ''),
('qq',         'QQ群',    '👥', '#7fbf94', 'main', 30, 0, 'dev', NULL, ''),
-- 推荐小功能（已上线 2 项）
('memo',       '备忘录',   '📝', '#7fbf94', 'recommend', 1, 1, 'done', NULL, ''),
('list',       '清单',     '☑',  '#7fbf94', 'recommend', 2, 1, 'done', NULL, ''),
-- 推荐小功能（开发中 4 项：默认隐藏）
('translate',  '翻译',     '文A', '#7fbf94', 'recommend', 20, 0, 'dev', NULL, ''),
('stock',      '库存管理', '📦', '#7fbf94', 'recommend', 21, 0, 'dev', NULL, ''),
('gold',       '记黄金',   '💰', '#7fbf94', 'recommend', 22, 0, 'dev', NULL, ''),
('coupon',     '优惠券',   '🎫', '#7fbf94', 'recommend', 23, 0, 'dev', NULL, '');

-- 4. 菜单（记账管理 5400 下：预设分类 order1 / 运营统计 order2 / 用户管理 order3 / 功能配置 order4）
DELETE FROM `moyun-db`.sys_menu WHERE menu_id IN (5403, 5404, 54031, 54041);
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                                 is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5403, '用户管理', 5400, 3, 'users', 'cms/ledger/users/index', NULL, '', 1, 0, 'C', '0', '0',
        'cms:ledgerUsers:list', 'peoples', 'admin', NOW(), '记账用户维度：流水/AI使用/token消费（脱敏）'),
       (54031, '用户查询', 5403, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
        'cms:ledgerUsers:query', '#', 'admin', NOW(), ''),
       (5404, '功能配置', 5400, 4, 'app-feature', 'cms/ledger/appFeature/index', NULL, '', 1, 0, 'C', '0', '0',
        'cms:ledgerAppFeature:list', 'component', 'admin', NOW(), '小程序"我的"页功能入口可视化配置'),
       (54041, '配置修改', 5404, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
        'cms:ledgerAppFeature:edit', '#', 'admin', NOW(), '');

-- 5. 管理员角色授权
DELETE FROM `moyun-db`.sys_role_menu WHERE menu_id IN (5403, 54031, 5404, 54041) AND role_id = 1;
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id) VALUES (1, 5403), (1, 54031), (1, 5404), (1, 54041);
