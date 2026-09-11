-- =====================================================================
-- v11.65 P1-4+ 知识问答场景接入统一网关（2026-09-11）
--
-- 依据《AI底座企业级评估》v11.64 实测修正后续动作：知识问答从 admin chat 专属
-- 收编进统一网关（ai2），通过 Agent 实现检索与多路召回。
--
-- 场景能力：查询改写 + 混合检索（向量+BM25+RRF 融合+知识库权重）+ Rerank 重排
--           + 相邻分片合并 + 引用溯源（references 含 fileName/页码/分片/文档ID/相似度）
-- 知识库绑定优先级：input.knowledgeLibraryIds > 场景配置 > Agent 绑定
-- RAG 参数（minScore/maxResults/召回倍数/混合开关/权重）读 Agent 配置，与 chat 链一致
--
-- 使用前提（二选一）：
--   ① 场景绑定知识库：UPDATE ai_scene_config SET knowledge_library_ids='[1]' WHERE scene_code='knowledge_qa'
--   ② 绑定 Agent（推荐——RAG 参数+人设+知识库一体）：UPDATE ai_scene_config SET agent_id=<id> WHERE scene_code='knowledge_qa'
--   ③ 调用时指定：POST /api/ai/execute {"sceneCode":"knowledge_qa","userInput":"问题","input":{"knowledgeLibraryIds":[1]}}
-- =====================================================================
select * from `moyun-db`.ai_scene_config WHERE scene_code = 'knowledge_qa' AND deleted = 0;
-- 1. 注册场景（幂等）
INSERT INTO `moyun-db`.ai_scene_config
    (scene_code, scene_name, description, scene_category, handler_bean_name,
     output_mode, output_parser, version, weight, priority, is_default, enabled,
     open_api, enable_cache, enable_output_filter, daily_token_limit, create_time, update_time, deleted)
SELECT 'knowledge_qa', '知识问答', '知识库检索问答：多路召回（向量+BM25+RRF+Rerank）+ Agent 人设 + 引用溯源',
       'chat', 'knowledgeQaHandler', 'sync', 'json', 'v1', 100, 0, 0, 1,
       1, 0, 1, 500000, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `moyun-db`.ai_scene_config WHERE scene_code = 'knowledge_qa' AND deleted = 0);

-- 说明：
--   open_api=1            统一入口 /api/ai/execute 直接可用
--   enable_cache=0        知识库内容变更会使缓存语义复杂，默认关闭
--   enable_output_filter=1 C 端直出文本场景，启用 DFA 输出过滤（v11.62 P1-3）
--   daily_token_limit=500000 开放入口的保护性成本上限（管理页可调；null=不限）
--   agent_id/knowledge_library_ids 留空，部署时按需绑定（上方使用前提）

-- 2. 绑定示例（确认后取消注释执行；Agent 推荐先在管理页建好并配 RAG 参数）
-- UPDATE `moyun-db`.ai_scene_config SET agent_id = 1 WHERE scene_code = 'knowledge_qa' AND deleted = 0;
-- UPDATE `moyun-db`.ai_scene_config SET knowledge_library_ids = '[1]' WHERE scene_code = 'knowledge_qa' AND deleted = 0;

-- 3. 验证
SELECT scene_code, scene_name, scene_category, handler_bean_name, output_mode,
       open_api, enable_cache, enable_output_filter, daily_token_limit,
       agent_id, knowledge_library_ids, enabled
FROM `moyun-db`.ai_scene_config WHERE scene_code = 'knowledge_qa' AND deleted = 0;
