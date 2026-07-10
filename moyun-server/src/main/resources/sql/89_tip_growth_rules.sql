-- =====================================================
-- 积分打赏成长规则与成就初始化
--
-- 作用：将打赏行为接入成长体系闭环
--   - receive_tip：被打赏者获得成长值（鼓励优质内容创作）
--   - tip_others：打赏者获得成长值（鼓励正向互动）
--   - first_tip_received：首次被打赏成就
--   - generous_tipper：累计打赏 10 次成就
--
-- 幂等：使用 INSERT ... WHERE NOT EXISTS 守护，可重复执行
-- =====================================================

-- 1. 成长规则：被打赏者获得成长值
INSERT INTO `portal_growth_rule` (`module`, `action`, `growth_delta`, `daily_limit`, `description`, `status`, `sort`)
SELECT 'article', 'receive_tip', 3, 0, '文章/专栏被打赏', '0', 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `portal_growth_rule` WHERE `module` = 'article' AND `action` = 'receive_tip');

-- 2. 成长规则：打赏者获得成长值（鼓励正向互动）
INSERT INTO `portal_growth_rule` (`module`, `action`, `growth_delta`, `daily_limit`, `description`, `status`, `sort`)
SELECT 'article', 'tip_others', 1, 3, '打赏他人', '0', 8
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `portal_growth_rule` WHERE `module` = 'article' AND `action` = 'tip_others');

-- 3. 成就：首次被打赏
INSERT INTO `portal_achievement` (`code`, `name`, `description`, `icon`, `module`, `condition_json`, `growth_reward`, `sort`, `status`)
SELECT 'first_tip_received', '初获鼓励', '首次收到打赏', NULL, 'article', '{"action":"receive_tip","count":1}', 10, 7, '0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `portal_achievement` WHERE `code` = 'first_tip_received');

-- 4. 成就：累计打赏 10 次（慷慨鼓励者）
INSERT INTO `portal_achievement` (`code`, `name`, `description`, `icon`, `module`, `condition_json`, `growth_reward`, `sort`, `status`)
SELECT 'generous_tipper', '慷慨鼓励', '累计打赏他人 10 次', NULL, 'article', '{"action":"tip_others","count":10}', 30, 8, '0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `portal_achievement` WHERE `code` = 'generous_tipper');

-- 5. 为超级管理员分配菜单权限（本脚本无新菜单，仅数据初始化，无需分配权限）
-- 校验
SELECT '积分打赏成长规则与成就:' AS info;
SELECT `module`, `action`, `growth_delta`, `daily_limit`, `description`
FROM `portal_growth_rule`
WHERE `action` IN ('receive_tip', 'tip_others');

SELECT `code`, `name`, `description`, `condition_json`, `growth_reward`
FROM `portal_achievement`
WHERE `code` IN ('first_tip_received', 'generous_tipper');
