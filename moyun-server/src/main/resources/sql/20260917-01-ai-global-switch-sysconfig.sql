-- =====================================================================
-- 20260917-01 v11.98 · AI 运行时开关迁移 sys_config（全局热配置）
--
-- 变更内容：
--   1. 新增 sys_config：ai.global.enabled（AI 能力全局开关，缺省 true）。
--      代码读取方 AiGlobalSwitch.isEnabled()，未配置/查询异常时兜底 true。
--      替代原 yaml 静态开关 moyun.ai.enabled 的运行时职责
--      （yaml 仅保留 bean 装配职责，application.yaml 已固定 true）。
--   2. 新增 sys_config：ai.resume.advice.enabled（简历 AI 建议开关，缺省 true）。
--      替代原 moyun.ai.resume-advice-enabled；关闭后简历解析/岗位匹配/
--      深度优化/AI 建议均降级规则/兜底路径。
--
-- 说明：
--   - 两个键均走 RuoYi 参数设置缓存（管理台「系统管理→参数设置」修改即时生效，
--     无需重启）。取值 true/1（大小写不敏感）为开，其余为关。
--   - 本任务不含表结构变更，无 DDL。
-- =====================================================================

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
SELECT 'AI能力全局开关', 'ai.global.enabled', 'true', 'Y',
       'AI 能力运行时总开关（网关/Agent/简历/面试全链路），true=开启（默认），false=关闭走规则兜底；管理台修改即时生效',
       'admin', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.global.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
SELECT '简历AI建议开关', 'ai.resume.advice.enabled', 'true', 'Y',
       '简历模块 AI 建议子开关（解析/岗位匹配/深度优化/AI建议），true=开启（默认），false=关闭走规则兜底；管理台修改即时生效',
       'admin', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.resume.advice.enabled');

-- 校验
SELECT config_key, config_value FROM sys_config WHERE config_key IN ('ai.global.enabled', 'ai.resume.advice.enabled');
