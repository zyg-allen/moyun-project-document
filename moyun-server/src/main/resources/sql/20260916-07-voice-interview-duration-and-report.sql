-- =====================================================================
-- 20260916-07 v11.96 · 语音面试时长制 + 报告生成链路修复
--
-- 变更内容：
--   1. 新增 sys_config：voice.interview.durationMinutes（缺省 20，范围 5-120）
--      语音面试时长制——面试不再按题数问满收尾，改为全场倒计时：
--        - 结束仅三种触发：用户点击"结束面试" / 口头明确提出结束（严格短语检测）
--          / 倒计时归零（自动保存当前作答并触发总结报告）
--        - 题数（questionCount）仅作软参考，进度时间线动态扩展
--        - 服务端守卫：超过时长+2分钟宽限后提交作答自动收口（closed_reason=timeout）
--   2. 报告生成链路修复（代码级，无 DDL）：
--        - P0 竞态：finish()/start() 事务内异步任务先于提交执行、读到
--          analysisStatus=0 直接跳过 → 改为事务提交后（afterCommit）触发
--        - 断链自愈：analysis 轮询接口检测"已结束+分析中但无运行任务"自动重触发
--        - 历史页/面试页 ?id= 进入改为拉取库中真实报告（移除前端伪报告拼凑）
--
-- 说明：
--   - 本脚本仅新增配置键；存量会话（configJson 无 durationMinutes）回退读取
--     sys_config 当前值，无需数据回填。
--   - 老的"报告从未生成"数据（analysisStatus 卡 1）：部署后进入历史页或面试页
--     ?id=xx 触发轮询即自愈重生成。
-- =====================================================================

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
SELECT '语音面试时长（分钟）', 'voice.interview.durationMinutes', '20', 'Y',
       '语音面试全场倒计时时长（分钟，范围5-120）：结束仅由用户主动（按钮/口头）或倒计时归零触发，题数仅作软参考',
       'admin', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'voice.interview.durationMinutes');

-- 校验
SELECT config_key, config_value FROM sys_config WHERE config_key = 'voice.interview.durationMinutes';
