-- =====================================================================
-- 20260915-05 AI 面试全链路重构 P0（v11.88 · V2 文档第一批）
--
-- 核心原则：一问一答流畅优先（不同步 LLM 分析），原始数据边做边存，
-- 结束后批量分析出报告（进度条）。本脚本补齐数据底座：
--   1. 主表：会话上下文快照 / 题单快照 / 分析状态与进度 / 结束原因
--   2. QA 表：原始回答 / 回答时间 / 草稿评分 / 单题分析状态
--   3. 新表：会话事件日志（start/answer/next/finish/close 全链路可追溯）
--
-- 规范：增量 ALTER 追加（不动原建表语句）；新表显式 COLLATE utf8mb4_general_ci
-- =====================================================================

-- 1. portal_voice_interview（会话主表）新增字段
ALTER TABLE `moyun-db`.portal_voice_interview
  ADD COLUMN context_snapshot JSON NULL COMMENT '会话上下文快照（准备时生成：简历摘要+画像+配置）',
  ADD COLUMN question_paper JSON NULL COMMENT '题单快照（preset 模式：题目ID序列+题目快照）',
  ADD COLUMN analysis_status TINYINT NOT NULL DEFAULT 0 COMMENT '报告分析状态：0未分析 1分析中 2已完成',
  ADD COLUMN analysis_progress INT NOT NULL DEFAULT 0 COMMENT '报告分析进度 0-100（前端进度条轮询）',
  ADD COLUMN closed_reason VARCHAR(50) NULL COMMENT '结束原因：user主动/auto题单耗尽/timeout超时/skip连续跳过';

-- 2. portal_voice_interview_qa（问答明细）新增字段
ALTER TABLE `moyun-db`.portal_voice_interview_qa
  ADD COLUMN answer_raw TEXT NULL COMMENT '用户原始回答（提交即落库，未分析，防丢失）',
  ADD COLUMN answer_time DATETIME NULL COMMENT '回答时间（提交时刻）',
  ADD COLUMN score_draft INT NULL COMMENT '草稿评分（异步 LLM 分析写回，报告批量分析融合）',
  ADD COLUMN analysis_status TINYINT NOT NULL DEFAULT 0 COMMENT '单题分析状态：0未分析 1分析中 2已分析';

-- 3. 新建表：会话事件日志（全链路可追溯）
CREATE TABLE IF NOT EXISTS `moyun-db`.portal_voice_interview_event (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    interview_id  BIGINT      NOT NULL COMMENT '面试会话ID（portal_voice_interview.id）',
    event_type    VARCHAR(50) NOT NULL COMMENT '事件类型：start/answer/next/finish/close/error',
    event_data    JSON        NULL COMMENT '事件数据（题目索引/动作/原因等）',
    create_time   DATETIME    NULL COMMENT '事件时间',
    PRIMARY KEY (id),
    KEY idx_interview (interview_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI语音面试会话事件日志（v11.88）';
