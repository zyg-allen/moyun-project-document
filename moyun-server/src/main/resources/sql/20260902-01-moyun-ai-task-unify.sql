-- ============================================================
-- 通用 AI 异步任务基础设施（v10.23）
-- 背景：门户 LLM 接口多为同步调用，大输入场景（简历解析/岗位匹配/
--       空字段草稿/深度优化）易超时。统一改为异步任务 + 前端轮询。
-- 说明：新建通用任务表，四类任务统一走该表；原 portal_resume_optimize_task
--       仅存瞬时任务状态，无历史价值，保留表不再写入（不 DROP）。
-- 日期：2026-09-02
-- ============================================================

CREATE TABLE IF NOT EXISTS `portal_ai_task` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id`      bigint       NOT NULL COMMENT '所属用户',
  `task_type`    varchar(50) NOT NULL COMMENT '任务类型：resume_parse（简历解析）/ job_match（岗位匹配）/ ai_draft（空字段草稿）/ deep_optimize（深度优化）',
  `biz_ref`      varchar(500) DEFAULT NULL COMMENT '业务参数JSON，如 {"resumeId":1,"jobTargetId":2}',
  `status`       varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending（排队）/ running（执行中）/ success（成功）/ failed（失败）',
  `progress_msg` varchar(500) DEFAULT NULL COMMENT '进度提示文案（轮询时返回给前端展示）',
  `result`       mediumtext   COMMENT '任务结果JSON（success 时有值）',
  `error`        varchar(1000) DEFAULT NULL COMMENT '失败原因（failed 时有值）',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `finish_time`  datetime     DEFAULT NULL COMMENT '完成时间（成功或失败）',
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`, `task_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通用AI异步任务表';

-- 原 portal_resume_optimize_task 停止写入（代码已切换到 portal_ai_task），
-- 按增量原则不 DROP，保留存量数据供回溯。
