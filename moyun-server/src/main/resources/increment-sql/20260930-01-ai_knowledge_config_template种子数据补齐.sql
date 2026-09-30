-- ============================================================
-- 增量脚本：20260930-01-ai_knowledge_config_template 种子数据补齐（v13.39）
-- 背景：知识库「快速配置（推荐）」Tab 的模板列表来自 ai_knowledge_config_template，
--       该表种子数据从未进 DML 初始化脚本——新库/重灌库模板列表为空，
--       用户只能走「自定义配置」（正是 v13.39 NPE 的触发路径）。
-- 内容：5 个系统模板（与 moyun-db 存量数据一致），DELETE+INSERT 幂等。
-- ============================================================

DELETE FROM ai_knowledge_config_template WHERE is_system = 1;

INSERT INTO ai_knowledge_config_template (template_name,template_desc,template_type,config_json,is_system,use_count,is_recommended) VALUES
('标准文档','适用于一般文档、技术手册等，平衡性能和准确度','general','{"indexMode": "high_quality", "segmentMode": "general", "rerankEnabled": true, "retrievalMode": "vector", "retrievalTopK": 10, "segmentMaxLength": 800, "preprocessRemoveUrls": false, "segmentOverlapLength": 100, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": true}',1,0,1),
('题库/QA精准模式','适用于题库、问答对等短文本，确保每道题独立检索','general','{"indexMode": "high_quality", "segmentMode": "qa", "rerankEnabled": true, "retrievalMode": "vector", "retrievalTopK": 15, "segmentMaxLength": 400, "preprocessRemoveUrls": true, "segmentOverlapLength": 50, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": true}',1,4,0),
('长文档深度模式','适用于长篇文章、研究报告等，保留更多上下文','general','{"indexMode": "high_quality", "segmentMode": "general", "rerankEnabled": true, "retrievalTopK": 8, "segmentMaxLength": 1200, "preprocessRemoveUrls": false, "segmentOverlapLength": 200, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": false}',1,1,0),
('代码技术文档','适用于代码、API文档等技术内容','technical','{"indexMode": "high_quality", "segmentMode": "code", "rerankEnabled": false, "retrievalMode": "vector", "retrievalTopK": 12, "segmentMaxLength": 600, "preprocessRemoveUrls": false, "segmentOverlapLength": 80, "preprocessReplaceSpaces": false, "preprocessRemoveExtraNewlines": false}',1,0,0),
('经济快速模式','降低资源消耗，适合大批量文档或测试环境','general','{"indexMode": "economy", "segmentMode": "general", "rerankEnabled": false, "retrievalMode": "vector", "retrievalTopK": 5, "segmentMaxLength": 500, "preprocessRemoveUrls": true, "segmentOverlapLength": 50, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": true}',1,76,0);

-- 复核：应返回 5 行（general 4 + technical 1）
SELECT id, template_name, template_type, is_recommended FROM ai_knowledge_config_template ORDER BY id;
