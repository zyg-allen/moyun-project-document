-- =============================================================
-- v11.36 AI 财务分析报告持久化（ledger_ai_analysis_report）
-- 策略：每月一条快照（UNIQUE user_id+period）。
--   - 进入分析页：当月已有报告直接返回（零 LLM token）
--   - 显式刷新（refresh=1）：重新分析并覆盖当月
--   - 历史报告分页可查，作为后续月/年趋势对比的数据基础
-- 幂等：IF NOT EXISTS 可重复执行。
-- =============================================================

CREATE TABLE IF NOT EXISTS `moyun-db`.ledger_ai_analysis_report
(
    id            bigint auto_increment comment '报告ID' primary key,
    user_id       bigint                                 not null comment '门户用户ID（portal_user.id）',
    period        varchar(7)                             not null comment '报告月份 yyyy-MM（每月一条，刷新覆盖）',
    health_score  int          default 0                 not null comment '财务健康分 0-100',
    metrics_json  text                                   null comment '核心指标 JSON（资产负债率/月均收支/储蓄率等）',
    income_json   text                                   null comment '收入来源占比 JSON',
    risk_json     text                                   null comment '债务风险列表 JSON',
    advice_json   text                                   null comment '建议列表 JSON',
    ai_summary    text                                   null comment 'LLM 综述（失败时为模板文案）',
    ai_enabled    tinyint      default 0                 not null comment '综述是否为 LLM 生成：1=是 0=模板降级',
    profile_snapshot varchar(200)                        null comment '当次画像快照（身份/职位/公司，用于历史回看）',
    create_time   datetime     default CURRENT_TIMESTAMP not null,
    update_time   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_user_period unique (user_id, period)
) comment 'AI 财务分析报告月度快照';

-- -------------------------------------------------------------
-- v11.40 增量：数据指纹（命中快照前比对流水/资产/负债/画像变更，变化自动重算，无需手动 refresh）
-- 幂等说明：重复执行会报 Duplicate column，可忽略（或执行前确认列已存在）
-- -------------------------------------------------------------
ALTER TABLE `moyun-db`.ledger_ai_analysis_report
    ADD COLUMN data_fingerprint varchar(200) null comment '数据指纹（流水/资产/负债/画像变更痕迹，变化自动失效快照）' AFTER profile_snapshot;