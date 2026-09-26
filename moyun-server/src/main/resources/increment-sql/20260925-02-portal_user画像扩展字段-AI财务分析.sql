-- =============================================================
-- 记账财务分析整改 v2：portal_user 画像扩展字段（AI 财务分析基础数据）
-- 日期：2026-09-25
-- 依据：docs/05-方案设计-分模块/2-记账模块/记账财务分析整改方案_0925_v2.md
--       3.2 用户画像查询 + 七、整改清单 #5（扩展用户画像字段）
-- 说明：画像字段为 AI 财务分析个性化上下文（风险偏好/行业/家庭负担/
--       负债分析/收入结构）与前端「个人信息维护」表单的数据基础；
--       全部可空，不迁移存量数据。
-- =============================================================

ALTER TABLE portal_user
    ADD COLUMN industry        VARCHAR(100) NULL COMMENT '行业（AI财务分析画像：行业分析）' AFTER school,
    ADD COLUMN marital_status  VARCHAR(20)  NULL COMMENT '婚姻状况（AI财务分析画像：家庭负担；single/married/other）' AFTER industry,
    ADD COLUMN has_mortgage    TINYINT      NULL COMMENT '是否有房贷（AI财务分析画像：负债分析；1=是 0=否）' AFTER marital_status,
    ADD COLUMN has_side_income TINYINT      NULL COMMENT '是否有副业收入（AI财务分析画像：收入结构；1=是 0=否）' AFTER has_mortgage,
    ADD COLUMN income_types    VARCHAR(200) NULL COMMENT '收入类型（AI财务分析画像：逗号分隔，如 salary,investment,rent）' AFTER has_side_income;
