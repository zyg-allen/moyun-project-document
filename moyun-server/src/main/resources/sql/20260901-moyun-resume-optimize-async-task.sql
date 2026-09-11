-- =============================================================================
-- 简历深度优化异步任务化补丁（增量，v10.19）
-- 背景：
--   简历深度优化调用大模型时频繁超时（dashscope compatible-mode timeout），
--   原同步接口阻塞 HTTP 线程 60s 仍返回失败，用户体验差。
-- 解决方案：
--   1. 新建 portal_resume_optimize_task 异步任务表，任务状态持久化，
--      前端轮询查询进度，支持关闭页面后回来查看。
--   2. 调大 chat 模型 timeout 到 180s、max_tokens 到 4000（兜底，避免任务内部仍超时）。
-- 幂等：DDL 用 IF NOT EXISTS，DML 用 UPDATE 可重复执行
-- 部署：执行本脚本后重启 moyun-server 后端；如已通过后台修改过模型配置则跳过第 2 步
-- =============================================================================

-- 1) 简历深度优化异步任务表（幂等：IF NOT EXISTS）
CREATE TABLE IF NOT EXISTS `portal_resume_optimize_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `resume_id` bigint NOT NULL COMMENT '简历ID（portal_user_resume.id）',
  `job_target_id` bigint NOT NULL COMMENT '岗位目标ID',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '任务状态：pending(已提交)/running(执行中)/success(成功)/failed(失败)',
  `result_json` json DEFAULT NULL COMMENT '优化结果 JSON（status=success 时填充，对应 ResumeDeepOptimizeVO 序列化）',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '失败原因（status=failed 时填充）',
  `ai_powered` tinyint(1) DEFAULT '1' COMMENT '是否 LLM 生成：0=规则兜底 1=LLM',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '任务提交时间',
  `start_time` datetime DEFAULT NULL COMMENT '任务开始执行时间（pending→running 时写入）',
  `finish_time` datetime DEFAULT NULL COMMENT '任务结束时间（成功或失败时写入）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历深度优化异步任务表（v10.19）';

-- 2) 调大 chat 模型 timeout 与 max_tokens（兜底：避免异步任务内部 60s 仍超时）
--    说明：qwen-plus/qwen-max 等模型生成 1500-3000 tokens 的结构化 JSON 通常需要 30-90 秒，
--          默认 60s 偏紧；调到 180s 留足余量。max_tokens 2000 容易截断，调到 4000。
UPDATE `ai_model_config`
SET `timeout` = 180,
    `max_tokens` = 4000
WHERE `model_type` = 'chat'
  AND `enabled` = 1
  AND `deleted` = 0
  AND (`timeout` IS NULL OR `timeout` < 180 OR `max_tokens` IS NULL OR `max_tokens` < 4000);

-- 校验查询（部署后可执行确认）
-- SELECT id, name, provider, model_name, timeout, max_tokens FROM ai_model_config WHERE model_type='chat' AND deleted=0;
