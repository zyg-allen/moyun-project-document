-- =====================================================================
-- 20260915-04 会员付费点免费体验机制（v11.85）
--
-- 产品口径：简历深度优化、语音面试两个会员付费点，非会员均可免费体验 2 次，
-- 用完引导开通会员（前端 status 接口返回 freeTrialLeft 展示剩余次数）。
--
-- 校验规则（后端统一）：
--   会员（MAX(vip_expire) 有效）→ 不限次放行；
--   非会员 → portal_free_trial 按场景原子消耗（UPDATE 条件自增 / INSERT 冲突回退），
--   用完返回 402 引导开通。
--
-- 场景（scene）：
--   resume_deep       简历深度优化（POST /portal/resume/optimize/deep 与 /deep/async）
--   voice_interview   语音面试（POST /portal/interview/voice/start）
-- =====================================================================

CREATE TABLE IF NOT EXISTS `moyun-db`.portal_free_trial (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT      NOT NULL COMMENT '门户用户ID（portal_user.id）',
    scene       VARCHAR(32) NOT NULL COMMENT '场景编码：resume_deep=简历深度优化 voice_interview=语音面试',
    used_count  INT         NOT NULL DEFAULT 0 COMMENT '已使用免费体验次数（上限 2，代码常量 FREE_TRIAL_TIMES）',
    create_time DATETIME    NULL COMMENT '首次使用时间',
    update_time DATETIME    NULL COMMENT '最近使用时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_scene (user_id, scene)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '会员付费点免费体验次数记录（v11.85：非会员每场景 2 次）';
