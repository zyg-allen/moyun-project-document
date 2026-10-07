-- ============================================================
-- 20261005-02 AI统一收口治理（批次5）：RAG 参数运行时化
-- sys_config 优先 + yaml（moyun-ai.rag.*）兜底
-- ============================================================
-- 背景：RAG 开关/阈值原固化在 RagConfig（yaml），改配置必须重启。
-- 新增 RagSettingsService：管理台「参数设置」改 ai.rag.* 即时生效
-- （ISysConfigService Redis 缓存 + 保存即刷新），键缺失/非法回落 yaml 默认。
-- 本脚本只预置 6 个布尔开关（在线止血能力）；数值阈值类参数
-- （minRecallCount/bm25Weight/vectorWeight/summaryThreshold 等）
-- 走 yaml 默认，需要调优时再按同键约定自行添加。
-- 幂等性：INSERT ... SELECT ... WHERE NOT EXISTS（按 config_key 判存），
-- 重复执行只补缺失键，可安全重跑。

INSERT INTO sys_config (config_name, config_key, config_value, platform_code, config_type, create_by, create_time, update_by, update_time, remark, del_flag)
SELECT v.config_name, v.config_key, v.config_value, NULL, 'Y', 'admin', NOW(), '', NULL, v.remark, '0'
FROM (
    SELECT 'RAG-混合检索开关' AS config_name, 'ai.rag.enableHybridSearch' AS config_key, 'true' AS config_value,
           'RAG 混合检索（向量+BM25）总开关；true=开启（默认），false=仅向量检索；管理台修改即时生效（yaml 兜底）' AS remark
    UNION ALL SELECT 'RAG-查询扩展开关', 'ai.rag.enableQueryExpansion', 'true',
           'RAG 查询扩展（同义词/关联词扩充召回）开关；Agent 级 ragEnableQueryExpansion 优先于本全局值'
    UNION ALL SELECT 'RAG-查询改写开关', 'ai.rag.enableQueryRewriting', 'true',
           'RAG 查询改写（LLM 优化检索 query，额外消耗 Token）开关；故障/成本异常时可在线关闭'
    UNION ALL SELECT 'RAG-SelfRAG验证开关', 'ai.rag.enableSelfRag', 'true',
           'Self-RAG 检索结果相关性 LLM 验证（过滤噪声召回，每条候选消耗 Token）开关；Agent 级 ragMinScore 阈值仍生效'
    UNION ALL SELECT 'RAG-对话摘要开关', 'ai.rag.enableConversationSummary', 'true',
           '长对话历史滑窗摘要压缩开关（节省上下文 Token）；false=超窗直接丢弃最旧消息'
    UNION ALL SELECT 'RAG-意图识别开关', 'ai.rag.enableIntentRecognition', 'true',
           '动态对话链路意图识别（日志/智能路由用，额外一次轻量分类调用）开关'
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = v.config_key);

-- 复核（期望 6）
SELECT '复核：ai.rag.* 配置行（期望 6）' AS stage, COUNT(*) AS cnt
  FROM sys_config WHERE config_key LIKE 'ai.rag.%';
