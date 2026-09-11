-- =====================================================================
-- v11.66 P1-5 output_parser/output_mode 配置接线（2026-09-11）
--
-- 依据《AI底座企业级评估-代码实测结论与改进清单》P1-5：两个字段此前配置可编辑零消费
-- （管理页与实际脱节）。v11.66 代码接线后配置即刻生效：
--   * output_parser：基类 parseOutput 按 json(默认)/markdown|text(原文包装)/未知容错 分派，
--     与 system_prompt_template 组合使用（管理员改模板为自由文本输出 + parser 同步改）
--   * output_mode：网关路由校验——'stream' 拒绝同步入口；'sync' 拒绝流式端点；both/null 放行
--
-- 本脚本对齐存量配置与 Handler 实际能力（配置此前零消费，voice_interview 拍脑袋填 sync）：
-- =====================================================================

-- 1. voice_interview：Handler 已实现流式（getSupportedOutputMode=both 且实现 executeStream），
--    配置从 sync 对齐为 both——否则新的流式校验（output_mode=sync 拒流式）会误拒现有流式请求
UPDATE `moyun-db`.ai_scene_config
SET output_mode = 'both', update_time = NOW()
WHERE scene_code = 'voice_interview' AND deleted = 0 AND output_mode = 'sync';

-- 2. 其余场景核对（预期值，无需变更）：
--    sensitive_word/daily_topic/finance_analysis/resume_parse/resume_optimize/
--    question_generate/knowledge_qa = 'sync'（Handler 仅同步实现，一致）
--    output_parser：finance_analysis='text'（综述原文透传，不经 JSON 解析，一致）；
--    其余='json'（既有行为，接线后语义不变）

-- 3. 验证
SELECT scene_code, output_mode, output_parser, enabled
FROM `moyun-db`.ai_scene_config
WHERE deleted = 0
ORDER BY scene_code;
