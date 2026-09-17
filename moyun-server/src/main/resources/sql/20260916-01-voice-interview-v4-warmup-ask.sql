-- =====================================================================
-- 20260916-01 AI 能力架构 v4 整合 · 语音面试 v11.94
--
-- 变更内容：
--   1. 新增反问段开关 sys_config（voice.interview.candidateAsk.enabled）：
--      题目问满后面试官邀请候选人反问（可多次，POST /{id}/ask），
--      关闭后问满直接 finished 收尾（旧行为）
--   2. 清理残留配置键 voice.interview.dynamicMode：
--      v11.94 已删除 dynamicModeEnabled() 及其常量（纯残留，无调用方）
--
-- 规范：增量 DML（不动原 INSERT 语句）；无表结构变更
-- =====================================================================

-- 1. 反问段开关（默认开启；后端缺省视为开启，本条落库便于后台直接改）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
VALUES ('语音面试反问段开关', 'voice.interview.candidateAsk.enabled', 'true', 'Y',
        'true=题目问满后进入候选人反问段（面试官邀请提问，可多次），false=问满直接结束（旧行为）', 'admin', NOW());

-- 2. 删除残留键（读取代码已随 v11.94 清理，无引用）
DELETE FROM sys_config WHERE config_key = 'voice.interview.dynamicMode';

-- 3. 校验
SELECT * FROM sys_config WHERE config_key LIKE 'voice.interview.%';
