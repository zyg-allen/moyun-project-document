-- ============================================================
-- 73_checkin_calendar_init.sql
-- 阶段四 4.5：签到日历 + 补签卡
-- 1. 扩展 portal_user_growth 表，增加补签卡数量 + 每月赠送记录字段
-- 说明：portal_user_growth 表已由 19_growth_system_init.sql 创建，此处仅做增量扩展
-- 签到历史通过 portal_growth_log（action=daily_checkin / supplement_checkin）查询，无需新建表
-- @author moyun
-- ============================================================

-- 补签卡数量（每月赠送1张，补签时消耗）
ALTER TABLE `portal_user_growth`
    ADD COLUMN `supplement_card_count` INT NOT NULL DEFAULT 0 COMMENT '补签卡数量（每月赠送1张，补签消耗）' AFTER `season_value`;

-- 最后一次赠送补签卡的月份（格式 YYYY-MM，用于幂等控制每月只赠送1张）
ALTER TABLE `portal_user_growth`
    ADD COLUMN `last_card_grant_month` VARCHAR(7) DEFAULT NULL COMMENT '最后赠送补签卡月份（YYYY-MM，幂等控制）' AFTER `supplement_card_count`;
