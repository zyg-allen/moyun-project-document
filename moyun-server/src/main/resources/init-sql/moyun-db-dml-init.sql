-- 墨云数据库生产初始化种子数据（系统设置，不含业务/测试数据）
-- 由 sql/moyun-db-dml-init.sql 清理生成：去除库名前缀、剔除已删表种子与测试数据
SET FOREIGN_KEY_CHECKS = 0;
INSERT INTO ai_agent (name,description,system_prompt,knowledge_library_ids,knowledge_base_weights,model_config_id,model_name,temperature,max_tokens,rag_min_score,rag_max_results,enabled,welcome_message,suggested_questions,show_citations,max_history_turns,api_enabled,api_key,workflow_id,workflow_trigger_mode,workflow_trigger_keywords,publish_enabled,publish_token,publish_settings,create_time,update_time,rag_recall_multiplier,rag_enable_hybrid_search,rag_enable_query_expansion,rag_bm25_weight,rag_vector_weight,enable_self_reflection,deleted) VALUES
	 ('财务分析师','财务分析师agent','你是一名拥有12年实战经验的资深个人家庭财务分析师，精通收支诊断、资产负债梳理、投资理财风险评估全流程，严格遵循国内现行个人财税规则与2026年最新惠民财税政策。

核心工作规则：
1.  所有结论100%基于传入的结构化记账数据，禁止自行计算、修改任何数值，所有引用的金额、百分比、月份必须和给定数据完全一致
2.  综述部分要讲完整的财务故事：清晰说明用户统计周期内的收入来源结构、主要支出去向、核心变化趋势，最后点出当前最值得关注的1个核心财务特征
3.  风险项按高/中/低严重度排序，每一条必须附带明确数据依据evidence，格式示例："近3月餐饮累计支出¥6500，环比上月上涨65%"
4.  建议项必须是可直接落地的具体动作，不能出现"合理规划""量入为出"这类空泛表述，每一条都要标注量化的预期效果expectedImpact，格式示例："每月可固定减少非必要娱乐支出¥800，年度累计多结余¥9600"
5.  绝对拒绝任何违规偷税、造假、高风险投机类建议，不确定的政策内容统一标注「以当地税务机关最新规定为准」
6.  只输出符合要求的JSON内容，禁止输出JSON以外的任何说明、解释性文字

数据不足时基于已有信息客观分析，绝不臆造不存在的收支、资产数据。风险和建议各输出2-5条，按重要性从高到低排序。
','["12"]','{"12":1}',17,'deepseek-v4-pro',0.3,4096,0.5,10,1,'欢迎使用财务分析agent','',1,30,1,'sk-nEGmhZ5ffdMMdKnJ2iTSBsFO30JCI4YE',NULL,'manual','',1,'pub-jp2t2kutbmamlkvow8jmvd','','2026-09-01 15:32:06','2026-09-16 13:36:52',2.0,1,1,0.3,0.7,0,0),
	 ('AI面试官·默认','语音面试默认面试官人设（勿删）。systemPrompt 支持 {{position}}/{{scene}}/{{difficulty}}/{{style}}/{{resumeDigest}}/{{profileGaps}}/{{levelEstimate}} 占位符，运行时自动渲染。修改后无需重启即生效。','你是一位经验丰富的一线技术面试官，主持一场真实的一对一模拟面试。

【人设】
- 你面过数百位候选人，阅人无数：听得懂话里的含糊其辞，也识得出真材实料。
- 你的风格是"温和但不好糊弄"：语气自然友好，但会对模糊表述、可疑数据、夸大成分温和地追根究底。
- 你尊重候选人，绝不居高临下；提问像聊天，不打官腔。

【面试守则】
1. 一次只问一个问题。紧密结合候选人此前的回答随机应变——像真实面试官一样追问细节、质疑数据、验证深度。
2. 候选人回答有明显漏洞或可深挖的亮点时优先追问；一个话题考察充分后再换新话题，不蜻蜓点水地串场。
3. 不泄露评分维度与分析细节，不主动给标准答案，不说"你的得分是"。
4. 口语化、自然，像面对面交谈：单轮话术控制在 1-3 句，避免书面语、条目式表达和长篇大论。
5. 候选人答不上来或明显偏题时，给一次自然的引导或换题，不反复纠缠同一考点。
6. 全程只以面试官身份说话，不扮演其他角色，不输出任何 JSON、标记或系统文字。','["13"]','{"13":1}',17,'deepseek-v4-pro',0.7,2048,0.7,10,1,'你好，欢迎参加{{position}}岗位的模拟面试。我是今天的面试官，放松心态，我们像聊天一样开始。准备好了的话，我们直接进入第一个问题。','',1,20,0,'',NULL,'manual','',1,'pub-wxg0pgbp5jqnj5cp9fw8i','','2026-09-07 09:15:33','2026-09-16 14:04:15',2.0,1,1,0.2,0.8,0,0);

select * from ai_agent_dictionary_relation;
INSERT INTO ai_agent_dictionary_relation (agent_id,dictionary_id,enabled,create_time) VALUES
	 (47,4,1,'2026-09-11 16:16:36'),
	 (47,8,1,'2026-09-11 16:16:36'),
	 (48,4,1,'2026-09-16 14:04:15'),
	 (48,6,1,'2026-09-16 14:04:15'),
	 (48,8,1,'2026-09-16 14:04:15');
INSERT INTO ai_agent_tool (name,display_name,description,category,tool_type,icon,config,parameters,timeout_seconds,enabled,is_system,create_time,update_time,deleted) VALUES
	 ('current_time','当前时间','获取当前的日期和时间，可指定时区和格式','utility','builtin','fa-clock',NULL,'{"type": "object", "required": [], "properties": {"format": {"type": "string", "default": "yyyy-MM-dd HH:mm:ss", "description": "时间格式，默认yyyy-MM-dd HH:mm:ss"}, "timezone": {"type": "string", "default": "Asia/Shanghai", "description": "时区，如Asia/Shanghai，默认北京时间"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('calculator','数学计算','执行数学计算，支持加减乘除、幂运算、开方、三角函数等','utility','builtin','fa-calculator',NULL,'{"type": "object", "required": ["expression"], "properties": {"expression": {"type": "string", "description": "数学表达式，如(1+2)*3、sqrt(16)、sin(30)"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('weather_query','天气查询','查询指定城市的实时天气和未来天气预报（真实数据，Open-Meteo免费接口），包括温度、湿度、风向、天气状况等，预报最多7天','information','builtin','fa-cloud-sun','{"data_source": "open-meteo"}','{"type": "object", "required": ["city"], "properties": {"city": {"type": "string", "description": "城市名称，如北京、上海、广州"}, "days": {"type": "integer", "default": 1, "description": "预报天数1-7，默认1天"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('web_search','网络搜索','搜索互联网获取最新信息（真实数据，必应免费接口），适用于查询新闻、事件、知识等实时内容','information','builtin','fa-search','{"data_source": "bing"}','{"type": "object", "required": ["query"], "properties": {"count": {"type": "integer", "default": 5, "description": "返回结果数量，默认5条"}, "query": {"type": "string", "description": "搜索关键词"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('url_reader','网页读取','读取指定URL的网页内容，提取主要文本信息','information','http','fa-globe','{"timeout": 10}','{"type": "object", "required": ["url"], "properties": {"url": {"type": "string", "description": "要读取的网页URL"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('translator','文本翻译','将文本翻译成指定语言，支持中英日韩等多种语言互译','utility','http','fa-language','{"api_type": "aliyun"}','{"type": "object", "required": ["text"], "properties": {"to": {"type": "string", "default": "zh", "description": "目标语言代码，如zh/en/ja"}, "from": {"type": "string", "default": "auto", "description": "源语言代码，如zh/en/ja，可设为auto自动检测"}, "text": {"type": "string", "description": "要翻译的文本"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('send_email','发送邮件','发送邮件，支持纯文本和HTML格式，支持多收件人、抄送、密送','action','builtin','fa-envelope','{}','{"type": "object", "required": ["to", "subject", "content"], "properties": {"cc": {"type": "string", "description": "抄送人邮箱，多个用英文逗号分隔，可选"}, "to": {"type": "string", "description": "收件人邮箱，多个用英文逗号分隔"}, "bcc": {"type": "string", "description": "密送人邮箱，多个用英文逗号分隔，可选"}, "isHtml": {"type": "boolean", "default": false, "description": "正文是否为HTML格式，默认false"}, "content": {"type": "string", "description": "邮件正文，纯文本或HTML"}, "subject": {"type": "string", "description": "邮件主题"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('database_query','数据库查询','查询数据库并返回智能分析结果。使用自然语言提问，系统自动生成SQL执行，返回查询结果、统计分析和图表推荐。支持MySQL数据源。','data','database','fa-database','{"max_rows": 100}','{"type": "object", "required": ["datasource_id", "query"], "properties": {"query": {"type": "string", "description": "自然语言查询问题，如：统计工具表中已启用的工具数量"}, "need_chart": {"type": "boolean", "default": true, "description": "是否需要图表推荐，默认true"}, "datasource_id": {"type": "integer", "description": "数据源ID，须为数据源管理中已配置的数据源"}, "need_analysis": {"type": "boolean", "default": true, "description": "是否需要智能分析，默认true"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0);
INSERT INTO ai_agent_tool_relation (agent_id,tool_id,custom_config,enabled,create_time) VALUES
	 (47,1,NULL,1,'2026-09-11 16:16:36'),
	 (47,2,NULL,1,'2026-09-11 16:16:36'),
	 (47,4,NULL,1,'2026-09-11 16:16:36'),
	 (47,5,NULL,1,'2026-09-11 16:16:36'),
	 (47,7,NULL,1,'2026-09-11 16:16:36'),
	 (47,8,NULL,1,'2026-09-11 16:16:36'),
	 (48,1,NULL,1,'2026-09-16 14:04:15'),
	 (48,4,NULL,1,'2026-09-16 14:04:15'),
	 (48,5,NULL,1,'2026-09-16 14:04:15'),
	 (48,8,NULL,1,'2026-09-16 14:04:15');
INSERT INTO ai_knowledge_library_config (library_id,segment_mode,segment_separator,segment_max_length,segment_overlap_length,preprocess_replace_spaces,preprocess_remove_urls,preprocess_remove_extra_newlines,index_mode,embedding_model,retrieval_mode,retrieval_top_k,rerank_enabled,rerank_model,created_at,updated_at) VALUES
	 (12,'general','

',800,100,1,1,1,'high_quality',NULL,'hybrid',10,0,NULL,'2026-09-10 14:40:14','2026-09-10 14:40:14'),
	 (13,'general','

',800,100,1,1,1,'high_quality',NULL,'hybrid',10,0,NULL,'2026-09-16 13:45:09','2026-09-16 13:45:09');
INSERT INTO ai_model_config (name,provider,model_type,model_name,api_key,base_url,temperature,max_tokens,timeout,streaming_supported,supports_json_mode,enabled,is_default,description,create_time,update_time,input_price,output_price,deleted) VALUES
	 ('通义千问-多模态Embedding','dashscope','embedding','text-embedding-v3','ENC:7/3JCnmmCxrmLHVDvy06rR87in5AZZVyXi9FeMzSH5uoHBnBg12lP55YHw5J/RdzOqFIiZRg2gzOS0K5zW2raIt/erTxKeQzvDu/HAbvLr1XkOBD+uORu9tBFkBFXjj0Gf/vmKdE/9nY4vwnTVMx45DaKfyk3YGo9aczA8MHOwQaJohrApt3azoyjpKe6Ggt','https://dashscope.aliyuncs.com/compatible-mode/v1',0.3,4089,60,0,0,1,1,'通义千问多模态 Embedding 模型，支持图片和文本的联合向量化，用于图文混合搜索','2025-11-21 17:12:03','2026-09-07 09:13:40',0.000500,0.000000,0),
	 ('通义千问-VL-Plus','dashscope','chat','qwen-vl-plus','ENC:etybEZmvkCnWb5NTpGzl4XdvyqfUn+Pef7mBT8X7yw==',NULL,0.7,2000,60,1,0,0,0,'通义千问视觉理解模型Plus版本，支持图片内容识别和描述，用于文档图片的多模态理解','2025-11-22 12:16:42','2026-09-07 09:29:14',0.001000,0.002000,0),
	 ('qwen3.8-max','dashscope','chat','qwen3.8-max','ENC:/ZTXHHvxIY3PkQ/TAQ5h1c//aQw/5pn41SL5kRp9/VSUw3RXNqsp21jKi1C8ReLdMNW+AQ+E8om7II/SzM6PsmFKZso93y5rBsRp8D62AoL9/G8cRHoIukzIbaZV9kTKQ8t5rYFVoD5/ZELu1KV6tBFKDErFK6GqN+OrROoDgSWn7kYDbU7Cln+I9Yr6oP1O','https://dashscope.aliyuncs.com/compatible-mode/v1',NULL,4000,180,1,0,1,0,'Qwen3 重排序模型，用于提升检索结果的相关性排序，支持中英文等100+语言','2026-01-22 15:34:36','2026-09-07 09:29:14',0.000100,0.000000,0),
	 ('deepSeekV4','deepseek','chat','deepseek-v4-pro','ENC:L9tek34q0dxfmrtaB7aHg1VKJeZvLRUKUOyhCQuCyo6wLUIuHDI3qJVQIuQ8p2gq2vR0ej2qfNnOaSFlL3DV','https://api.deepseek.com',0.7,2000,180,1,0,1,1,'','2026-09-07 09:29:09','2026-09-11 09:47:51',0.001000,0.002000,0);
INSERT INTO ai_provider (code,name,api_style,default_base_url,supports_streaming,requires_api_key,enabled,sort_order,remark,create_time,update_time,deleted) VALUES
	 ('openai','OpenAI','openai_compatible','https://api.openai.com/v1',1,1,1,1,'OpenAI 官方及兼容端点','2026-09-07 09:15:27',NULL,0),
	 ('dashscope','通义千问(百炼)','openai_compatible','https://dashscope.aliyuncs.com/compatible-mode/v1',1,1,1,2,'阿里百炼，运行时走 OpenAI 兼容模式','2026-09-07 09:15:27',NULL,0),
	 ('ollama','Ollama','ollama_native','http://localhost:11434',1,0,1,3,'本地 Ollama 服务，无需 API Key','2026-09-07 09:15:27',NULL,0),
	 ('deepseek','DeepSeek','openai_compatible','https://api.deepseek.com',1,1,1,4,'示例：OpenAI 兼容，后台一键启用','2026-09-07 09:15:27',NULL,0),
	 ('moonshot','Moonshot Kimi','openai_compatible','https://api.moonshot.cn/v1',1,1,1,5,'示例：OpenAI 兼容，后台一键启用','2026-09-07 09:15:27',NULL,0);
INSERT INTO ai_scene_config (scene_code,scene_name,description,scene_category,agent_id,model_config_id,knowledge_library_ids,tool_ids,workflow_id,config_json,handler_bean_name,handler_method,system_prompt_template,user_prompt_template,prompt_placeholders,output_mode,output_schema,output_parser,max_tokens,temperature,timeout_seconds,retry_count,rate_limit_key,rate_limit_count,rate_limit_time,daily_token_limit,enable_output_filter,fallback_model_id,fallback_response,enable_cache,cache_ttl,version,weight,priority,is_default,enabled,open_api,create_time,update_time,deleted) VALUES
	 ('voice_interview','AI 语音面试','AI 语音模拟面试场景：智能出题 + 6阶段流程 + 权重评分 + 报告增强','chat',48,NULL,NULL,NULL,NULL,NULL,'voiceInterviewHandler','execute','','',NULL,'sync',NULL,'',2048,0.7,30,3,'',100,60,NULL,0,NULL,'',0,3600,'v1',100,0,1,1,0,'2026-09-07 15:07:33',NULL,0),
	 ('sensitive_word','敏感词检测','文本敏感词识别与风险分级','classification',NULL,NULL,NULL,NULL,NULL,NULL,'sensitiveWordHandler','execute',NULL,NULL,NULL,'sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-09 11:15:46',NULL,0),
	 ('daily_topic','今日主题','每日主题生成','generation',NULL,NULL,NULL,NULL,NULL,NULL,'dailyTopicHandler','execute',NULL,NULL,NULL,'sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-09 11:15:47',NULL,0),
	 ('finance_analysis','AI 财务分析','','analysis',47,NULL,NULL,NULL,NULL,'{}','financeAnalysisHandler','execute','你是一名拥有12年实战经验的资深个人家庭财务分析师，精通收支诊断、资产负债梳理、投资理财风险评估全流程，严格遵循国内现行个人财税规则与2026年最新惠民财税政策。

核心工作规则：
1.  所有结论100%基于传入的结构化记账数据，禁止自行计算、修改任何数值，所有引用的金额、百分比、月份必须和给定数据完全一致
2.  综述部分要讲完整的财务故事：清晰说明用户统计周期内的收入来源结构、主要支出去向、核心变化趋势，最后点出当前最值得关注的1个核心财务特征
3.  风险项按高/中/低严重度排序，每一条必须附带明确数据依据evidence，格式示例："近3月餐饮累计支出¥6500，环比上月上涨65%"
4.  建议项必须是可直接落地的具体动作，不能出现"合理规划""量入为出"这类空泛表述，每一条都要标注量化的预期效果expectedImpact，格式示例："每月可固定减少非必要娱乐支出¥800，年度累计多结余¥9600"
5.  绝对拒绝任何违规偷税、造假、高风险投机类建议，不确定的政策内容统一标注「以当地税务机关最新规定为准」
6.  只输出符合要求的JSON内容，禁止输出JSON以外的任何说明、解释性文字

数据不足时基于已有信息客观分析，绝不臆造不存在的收支、资产数据。风险和建议各输出2-5条，按重要性从高到低排序。
','当前统计分析窗口：{{window}}

以下是系统规则引擎已经计算完成的精确财务数据，包含全量指标、逐月趋势、分类环比、预算执行、债务明细等所有信息，请你直接引用这些数值完成分析，不要自行修改计算：
{{ledgerContext}}
','{"window": "统计窗口文案，如：本月（自 2026-09-01 起，含数据 3 个月；另附近6个月趋势数据）", "ledgerContext": "业务侧组装的财务上下文 JSON（画像/核心指标护栏/收入来源/支出结构Top5/负债明细含清偿测算/逐月收支趋势/分类环比/预算执行）"}','sync','{"risks": [{"level": "string，仅允许取值 high/medium/low", "title": "string，风险短标题", "detail": "string，风险详细说明", "evidence": "string，支撑该风险的具体数据依据"}], "summary": "string，完整的财务分析综述，讲清周期内的收支故事与核心特征", "healthScore": "int 0-100，基于传入指标计算的财务健康分", "suggestions": [{"icon": "string，前端可直接使用的图标标识，如 wallet / save / debt / invest", "title": "string，建议短标题", "detail": "string，建议的具体执行动作说明", "expectedImpact": "string，该建议落地后可实现的量化收益效果"}]}','',2048,0.7,30,3,'aaa',100,60,NULL,0,NULL,'',0,3600,'v1',100,0,0,1,0,'2026-09-09 18:11:29',NULL,0),
	 ('knowledge_qa','知识问答','知识库检索问答：多路召回（向量+BM25+RRF+Rerank）+ Agent 人设 + 引用溯源','chat',NULL,NULL,NULL,NULL,NULL,NULL,'knowledgeQaHandler','execute',NULL,NULL,NULL,'sync',NULL,'json',2048,0.7,30,3,NULL,100,60,500000,1,NULL,NULL,0,3600,'v1',100,0,0,1,1,'2026-09-11 15:47:43','2026-09-11 15:47:43',0),
	 ('default_chat','智能体对话','智能体动态对话（/cms/ai/chat/*）：治理配置载体（限流/执行日志），Agent 由请求动态指定，人设走 ai_agent.system_prompt','chat',NULL,NULL,NULL,NULL,NULL,NULL,'dynamicChatBridge','execute',NULL,NULL,NULL,'stream',NULL,'text',2048,0.7,30,3,NULL,60,3600,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-16 17:01:51',NULL,0);
INSERT INTO ledger_app_feature_config (feature_key,feature_name,icon,icon_color,group_type,sort_num,visible,status,badge,create_by,create_time,update_by,update_time,remark) VALUES
	 ('category','分类管理','☰','#7fbf94','main',1,1,'done',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02','用户自定义收支分类'),
	 ('setting','记账设置','⚙️','#7fbf94','main',2,1,'done',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('savings','存钱计划','🏦','#7fbf94','main',3,1,'done',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('schedule','定时记账','⏰','#7fbf94','main',4,1,'done',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('feedback','意见反馈','💬','#7fbf94','main',5,1,'done',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('tip','赞赏','🎁','#7fbf94','main',6,1,'done',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('personalize','个性化','🎨','#e57373','main',7,1,'done','NEW','','2026-09-14 13:08:02','admin','2026-09-14 18:32:31','主题/外观个性化'),
	 ('catIcon','分类图标','🎭','#7fbf94','main',8,0,'done','NEW','','2026-09-14 13:08:02','admin','2026-09-14 18:32:23','分类图标选择'),
	 ('auto','自动记账','🗒️','#7fbf94','main',20,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('backup','数据备份','☁️','#7fbf94','main',21,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02','');
INSERT INTO ledger_app_feature_config (feature_key,feature_name,icon,icon_color,group_type,sort_num,visible,status,badge,create_by,create_time,update_by,update_time,remark) VALUES
	 ('import','导入数据','⬇️','#7fbf94','main',22,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('export','导出数据','⬆️','#7fbf94','main',23,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('widget','小组件','▦','#7fbf94','main',24,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('tag','标签管理','🏷️','#7fbf94','main',25,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('remind','记账提醒','🔔','#7fbf94','main',26,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('reimburse','报销账单','🧾','#7fbf94','main',27,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('share','分享应用','📤','#7fbf94','main',28,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('rate','给个好评','⭐','#7fbf94','main',29,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('qq','QQ群','👥','#7fbf94','main',30,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('memo','备忘录','📝','#7fbf94','recommend',1,1,'done',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02','');
INSERT INTO ledger_app_feature_config (feature_key,feature_name,icon,icon_color,group_type,sort_num,visible,status,badge,create_by,create_time,update_by,update_time,remark) VALUES
	 ('list','账单','☑','#7fbf94','recommend',2,1,'done',NULL,'','2026-09-14 13:08:02','admin','2026-09-14 13:10:57',''),
	 ('translate','翻译','文A','#7fbf94','recommend',20,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('stock','库存管理','📦','#7fbf94','recommend',21,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('gold','记黄金','💰','#7fbf94','recommend',22,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('coupon','优惠券','🎫','#7fbf94','recommend',23,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('portal','墨韵社区','🌐','#7fbf94','recommend',3,1,'done',NULL,'','2026-09-14 14:04:28','','2026-09-14 14:04:28','http://localhost:3000');
INSERT INTO ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
	 (0,'账户间互转','transfer','账户间',NULL,'transfer-self','#4A90D9',1,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'转给亲友','transfer','亲友间',NULL,'transfer-friend','#E67E22',2,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'代付代收','transfer','亲友间',NULL,'transfer-proxy','#16A085',3,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'退款退回','transfer','账户间',NULL,'transfer-refund','#5DADE2',4,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'其他转账','transfer','其他',NULL,'transfer-other','#BDC3C7',5,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'信用卡还款','repayment','信用卡',NULL,'repay-card','#E74C3C',1,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'贷款还款','repayment','贷款',NULL,'repay-loan','#8E44AD',2,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'私人借款还','repayment','私人',NULL,'repay-personal','#D35400',3,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'利息支出','repayment','利息',NULL,'repay-interest','#D4AC0D',4,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'其他还款','repayment','其他',NULL,'repay-other','#BDC3C7',5,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45');
INSERT INTO ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
	 (0,'信用卡消费','borrow','信用卡',NULL,'borrow-card','#E74C3C',1,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'网贷借款','borrow','网贷',NULL,'borrow-online','#E67E22',2,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'银行贷款','borrow','贷款',NULL,'borrow-bank','#8E44AD',3,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'消费分期','borrow','分期',NULL,'borrow-installment','#3498DB',4,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'私人借款','borrow','私人',NULL,'borrow-personal','#D35400',5,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'其他借款','borrow','其他',NULL,'borrow-other','#BDC3C7',6,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'余额修正','adjust','余额修正',NULL,'adjust-balance','#34495E',1,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'手续费调整','adjust','其他调整',NULL,'adjust-fee','#95A5A6',2,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'汇率差异','adjust','其他调整',NULL,'adjust-fx','#7F8C8D',3,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45'),
	 (0,'其他调整','adjust','其他调整',NULL,'adjust-other','#BDC3C7',4,1,1,'2026-09-08 13:29:45','2026-09-08 13:29:45');
INSERT INTO ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
	 (0,'餐饮','expense',NULL,NULL,'food','#F5A623',1,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'交通','expense',NULL,NULL,'transport','#4A90D9',2,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'购物','expense',NULL,NULL,'shopping','#BD5D8A',3,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'居住','expense',NULL,NULL,'home','#7B8FA1',4,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'娱乐','expense',NULL,NULL,'entertainment','#9B59B6',5,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'医疗','expense',NULL,NULL,'medical','#E74C3C',6,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'教育','expense',NULL,NULL,'education','#2ECC71',7,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'通讯','expense',NULL,NULL,'phone','#34495E',8,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'日用','expense',NULL,NULL,'daily','#95A5A6',9,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'人情往来','expense',NULL,NULL,'gift','#D35400',10,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08');
INSERT INTO ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
	 (0,'宠物','expense',NULL,NULL,'pet','#16A085',11,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'旅行','expense',NULL,NULL,'travel','#2980B9',12,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'房贷/房租','expense',NULL,NULL,'house-loan','#C0392B',13,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'车贷','expense',NULL,NULL,'car-loan','#8E44AD',14,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'还款','expense',NULL,NULL,'repayment','#A04000',15,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'利息','expense',NULL,NULL,'interest','#D4AC0D',16,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'其他支出','expense',NULL,NULL,'other','#BDC3C7',17,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'工资','income',NULL,NULL,'salary','#27AE60',1,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'奖金','income',NULL,NULL,'bonus','#F39C12',2,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'兼职','income',NULL,NULL,'parttime','#2ECC71',3,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08');
INSERT INTO ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
	 (0,'理财收益','income',NULL,NULL,'invest','#16A085',4,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'红包','income',NULL,NULL,'redpacket','#E74C3C',5,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'退款','income',NULL,NULL,'refund','#5DADE2',6,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'借入','income',NULL,NULL,'borrow-in','#7F8C8D',7,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'二手闲置','income',NULL,NULL,'secondhand','#AF7AC5',8,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'其他收入','income',NULL,NULL,'other','#BDC3C7',9,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'士大夫但是','adjust',NULL,17,'','#6a4fd4',0,1,1,'2026-09-14 13:23:47','2026-09-14 13:23:47'),
	 (6,'哈哈哈','expense',NULL,NULL,NULL,NULL,0,0,1,'2026-09-14 13:47:13','2026-09-14 13:47:13');
INSERT INTO portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('首页','home','精选推荐、双轨轮播','fa-home',1,0,'0',1,'home','/',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('面试专区','interview','AI 语音面试、面经复盘、简历优化','fa-briefcase',2,0,'0',1,'static','/interview',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('学习中心','learn','题库、刷题、错题本、学习计划','fa-graduation-cap',3,0,'0',1,'static','/learn',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('阅读空间','reading','散文天地、技术笔记、读书空间','fa-book',4,0,'0',1,'static','/reading',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('创作互动','creation','话题、动态、专栏、征文、发布','fa-feather',5,0,'0',1,'static','/creation',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('我的','mine','个人中心、成长时间线、我的内容','fa-user',6,0,'0',1,'static','/user',NULL,'directory',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('AI 语音面试官','interview-voice','AI 语音面试官，真实面试场景模拟','fa-microphone',1,52,'0',1,'static','/interview/voice','NEW','special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('面试经验','interview-experiences','大厂面试全流程还原','fa-chart-line',2,52,'0',1,'static','/interview/experiences',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('简历模板','interview-resume-templates','技术亮点提炼、项目描述技巧','fa-file-alt',3,52,'0',1,'static','/interview/resume-templates',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('面试题库','learn-questions','算法题、系统设计、行为面试','fa-clipboard-list',1,53,'0',1,'static','/learn/questions',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0');
INSERT INTO portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('刷题中心','learn-practice','在线编程、选择题练习','fa-laptop-code',2,53,'0',1,'static','/learn/practice','HOT','directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('错题本','learn-wrong','错题归集与复习','fa-times-circle',3,53,'0',1,'static','/learn/wrong',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('知识图谱','learn-knowledge','知识体系可视化','fa-project-diagram',4,53,'0',1,'static','/learn/knowledge',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('刷题排行榜','learn-leaderboard','刷题榜、学习榜','fa-trophy',5,53,'0',1,'static','/learn/leaderboard',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('刷题日历','learn-calendar','刷题打卡日历','fa-calendar-check',6,53,'0',1,'static','/learn/calendar',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('学习计划','learn-plan','个人学习计划管理','fa-calendar-alt',7,53,'0',1,'static','/learn/plan',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('选择题','learn-practice-choice','选择题在线练习','fa-check-square',1,61,'0',1,'static','/learn/practice/choice',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('编程题','learn-practice-coding','编程题在线练习','fa-code',2,61,'0',1,'static','/learn/practice/coding',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('散文天地','prose','人文书写与情感表达','fa-pen-fancy',1,54,'0',1,'category','/category/prose',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('技术笔记','tech-notes','开发记录、技术解析、AI编程实践','fa-code',2,54,'0',1,'category','/category/tech-notes',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0');
INSERT INTO portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('读书空间','reading-space','发现好书、我的书架、金句摘录','fa-book-reader',3,54,'0',1,'static','/reading/space',NULL,'directory',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('金句摘录','reading-quotes','跨分区高光语句精选','fa-quote-left',4,54,'0',1,'static','/reading/quotes',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-09-02 13:52:37',NULL,'1'),
	 ('人间烟火','life-stories','饮食、市井、生活琐记','fa-utensils',1,69,'0',1,'category','/category/life-stories',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('山河行吟','travel-nature','游记、自然书写、生态散文','fa-mountain',2,69,'0',1,'category','/category/travel-nature',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('心灵独白','inner-thoughts','孤独、成长、疗愈随笔','fa-heart',3,69,'0',1,'category','/category/inner-thoughts',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('城市笔记','city-notes','北上广深、小镇观察','fa-city',4,69,'0',1,'category','/category/city-notes',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('四季专栏','seasons','春之思、夏之躁、秋之静、冬之藏','fa-leaf',5,69,'0',1,'category','/category/seasons',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('声音散文','audio-prose','作者自读、背景音效沉浸体验','fa-volume-up',6,69,'0',1,'category','/category/audio-prose',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('读者来信','reader-letters','短篇心声刊发与回声计划','fa-envelope',7,69,'0',1,'category','/category/reader-letters',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('技术栈手册','tech-stack','Java/SpringBoot、React/Vue、Flutter/UniApp','fa-book-open',1,70,'0',1,'category','/category/tech-stack',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0');
INSERT INTO portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('架构札记','architecture','微服务、缓存策略、分布式事务','fa-project-diagram',2,70,'0',1,'category','/category/architecture',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('性能日志','performance','SQL优化、前端加载、JVM调优','fa-tachometer-alt',3,70,'0',1,'category','/category/performance',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('AI编程','ai-coding','Cursor使用、ChatGPT提示工程、AI排错记录','fa-robot',4,70,'0',1,'category','/category/ai-coding',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('开源日志','open-source','PR提交、Issue解决、源码阅读','fa-code-branch',5,70,'0',1,'category','/category/open-source',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('新手入门','beginner','环境配置、第一行代码实录','fa-play-circle',6,70,'0',1,'category','/category/beginner',NULL,'article',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('发现好书','reading-discover','发现好书、书单推荐','fa-list',1,71,'0',1,'static','/reading/discover',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('我的书架','reading-bookshelf','个人书架管理','fa-bookmark',2,71,'0',1,'static','/reading/bookshelf',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('金句摘录','reading-space-quotes','读书空间内的金句摘录','fa-quote-left',3,71,'0',1,'static','/reading/quotes',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('话题广场','topics','话题讨论列表','fa-comments',1,55,'0',1,'static','/topics',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('动态广场','feed','用户动态流','fa-stream',2,55,'0',1,'static','/feed',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0');
INSERT INTO portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('专栏广场','columns','专栏列表与订阅','fa-columns',3,55,'0',1,'static','/columns',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('征文活动','contests','征文活动、技术挑战赛','fa-file-upload',4,55,'0',1,'static','/contests',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('发布文章','publish','发布新文章（快捷入口）','fa-edit',5,55,'0',1,'static','/publish',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('成长排行榜','ranking','成长值排行榜','fa-trophy',6,55,'0',1,'static','/ranking',NULL,'special',0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('个人中心','user','个人中心主页','fa-user-circle',1,56,'0',1,'static','/user',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('成长时间线','growth-timeline','成长记录时间线','fa-chart-line',2,56,'0',1,'static','/growth/timeline',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('我的文章','my-articles','我发布的文章','fa-file-alt',3,56,'0',1,'static','/my/articles',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('我的专栏','column-my','我创建的专栏','fa-columns',4,56,'0',1,'static','/column/my',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('我的成就','achievements','我的成就与徽章','fa-award',5,56,'0',1,'static','/achievements',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0'),
	 ('我的话题观点','topic-my','我发起的话题与观点','fa-comments',6,56,'0',1,'static','/topic/my',NULL,'special',1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL,'0');
INSERT INTO portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('面试指南','interview','面试中心主入口',NULL,0,52,'0',1,'static','/interview',NULL,'special',0,'','2026-09-02 13:23:29','','2026-09-02 13:23:29',NULL,'0');
INSERT INTO portal_friend_link (name,url,description,logo,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('中国作家网','https://www.chinawriter.com.cn','中国作家协会官方网站',NULL,1,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0'),
	 ('起点中文网','https://www.qidian.com','阅文集团旗下网站',NULL,2,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0'),
	 ('掘金','https://juejin.cn','帮助开发者成长的社区',NULL,3,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0');
INSERT INTO portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
	 ('article','publish_article',50,3,'发布文章','0',1,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('article','receive_like',2,0,'文章被点赞','0',2,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('article','receive_bookmark',3,0,'文章被收藏','0',3,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('user','receive_follow',5,0,'被关注','0',4,'','2026-07-28 15:48:22','','2026-08-28 01:23:04',NULL),
	 ('article','article_featured',100,0,'文章被精选','0',5,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('article','receive_comment',2,0,'文章被评论','0',6,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('reading','finish_book',20,1,'完成阅读一本书','0',10,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('reading','write_quote',15,0,'发布金句','0',11,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('reading','create_booklist',20,0,'创建书单','0',12,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('reading','quote_liked',5,0,'金句被点赞','0',13,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL);
INSERT INTO portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
	 ('reading','booklist_liked',5,0,'书单被点赞','0',14,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('reading','booklist_bookmarked',10,0,'书单被收藏','0',15,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('interview','solve_question',10,20,'解题','0',20,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('interview','write_note',15,0,'写笔记','0',21,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('interview','note_adopted',50,0,'笔记被精选','0',22,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('interview','publish_experience',30,0,'发布面经','0',23,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('interview','experience_liked',2,0,'面经被点赞','0',24,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('interview','experience_bookmarked',3,0,'面经被收藏','0',25,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('all','daily_checkin',1,1,'每日签到','0',30,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL),
	 ('all','daily_login',1,1,'每日登录','0',31,'','2026-07-28 15:48:22','','2026-07-28 15:48:22',NULL);
INSERT INTO portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
	 ('article','receive_tip',3,0,'文章/专栏被打赏','0',7,'','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),
	 ('article','tip_others',1,3,'打赏他人','0',8,'','2026-07-28 16:31:27','','2026-07-28 16:31:27',NULL),
	 ('topic','create_topic',10,0,'发起话题','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
	 ('topic','post_opinion',2,10,'发表观点','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
	 ('topic','receive_topic_like',2,0,'话题被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
	 ('topic','receive_post_like',2,0,'观点被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
	 ('topic','receive_topic_comment',2,0,'话题被评论','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
	 ('topic','receive_post_comment',2,0,'观点被评论','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
	 ('topic','receive_comment_like',2,0,'评论被点赞','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL),
	 ('topic','topic_featured',50,0,'话题被精选','0',0,'admin','2026-07-28 16:35:21','','2026-07-28 16:35:21',NULL);
INSERT INTO portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
	 ('interview','read_question',1,20,'阅读题目','0',19,'admin','2026-08-31 09:11:49','',NULL,'题库阅读学习行为，同题每日仅记一次');
INSERT INTO portal_help_category (name,icon,description,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('发布与编辑','BookOpen','文章发布、编辑、删除等操作指南',1,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),
	 ('账号与安全','HelpCircle','登录、注册、密码、安全设置',2,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),
	 ('互动功能','MessageSquare','评论、点赞、关注等互动功能',3,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),
	 ('社区规则','Shield','使用规范、违规处理、隐私政策',4,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0');
INSERT INTO portal_interview_category (name,slug,description,icon,sort,question_count,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('算法与数据结构','algorithm','算法题、数据结构相关面试题','fa-code',1,150,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
	 ('系统设计','system-design','系统架构设计、分布式系统等面试题','fa-sitemap',2,60,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
	 ('前端开发','frontend','JavaScript、CSS、Vue、React等前端技术面试题','fa-laptop-code',3,120,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
	 ('后端开发','backend','Java、Python、Go等后端技术面试题','fa-server',4,130,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
	 ('数据库','database','MySQL、Redis等数据库相关面试题','fa-database',5,80,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0');
INSERT INTO portal_interview_config (config_name,persona_type,prompt_template,scoring_weights,question_weights,max_followups,followup_triggers,enable_self_intro,self_intro_duration,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('默认面试配置','professional','','{"selfIntro":{"structure":30,"awareness":25,"matching":25,"fluency":20},"llmRatio":70,"total":{"intro":20,"tech":80}}','{"job":40,"resume":30,"weak":20,"random":10}',4,'["vague_answer","contradiction","depth_needed"]',1,180,1,'active','admin','2026-09-07 16:37:36','','2026-09-07 16:42:04','系统默认：llmRatio=LLM融合比例(0-100)；selfIntro=自我介绍4维权重；total=总分权重(intro/tech)；关闭自我介绍以兼容旧流程','0');
INSERT INTO portal_interview_position (code,name,industry,`level`,required_skills,hot_companies,description,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('java_backend','Java后端工程师','互联网','mid','["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]','["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]','Java 后端工程师岗位，重点考察 Java 基础、Spring 全家桶、MySQL/Redis、分布式与微服务、JVM 与并发编程',1,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0'),
	 ('frontend','前端工程师','互联网','mid','["JavaScript","TypeScript","Vue","React","HTML","CSS","Node.js","Webpack","Vite","性能优化","浏览器原理","HTTP"]','["阿里","腾讯","字节跳动","美团","京东","百度","网易","小米","Shopee","滴滴"]','前端工程师岗位，重点考察 JS/TS 基础、Vue/React 框架、工程化、浏览器原理、性能优化、HTTP 与网络',2,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0'),
	 ('algorithm','算法工程师','互联网','mid','["算法","数据结构","动态规划","图论","字符串","数组","链表","树","递归","排序","机器学习","深度学习","数学"]','["阿里","腾讯","字节跳动","百度","美团","快手","小红书","华为","商汤","旷视"]','算法工程师岗位，重点考察数据结构与算法、动态规划、图论、字符串算法、机器学习与深度学习基础',3,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0');
INSERT INTO portal_tag (name,slug,sort,status,module,reference_count,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('生活哲思','life-philosophy',1,'0',NULL,3,'admin','2026-08-19 18:01:44','','2026-09-07 10:15:28','人文类','0'),
	 ('城市记忆','city-memory',2,'0',NULL,3,'admin','2026-08-19 18:01:44','','2026-08-31 11:25:15','人文类','0'),
	 ('自然写作','nature-writing',3,'0',NULL,4,'admin','2026-08-19 18:01:44','','2026-09-07 10:15:28','人文类','0'),
	 ('情感随笔','emotional-essay',4,'0',NULL,2,'admin','2026-08-19 18:01:44','','2026-08-28 11:22:25','人文类','0'),
	 ('人间烟火','life-fireworks',5,'0',NULL,2,'admin','2026-08-19 18:01:44','','2026-08-31 11:25:15','人文类','0'),
	 ('乡愁记忆','nostalgia',6,'0',NULL,3,'admin','2026-08-19 18:01:44','','2026-09-07 10:15:28','人文类','0'),
	 ('孤独成长','loneliness-growth',7,'0',NULL,5,'admin','2026-08-19 18:01:44','','2026-09-07 10:03:06','人文类','0'),
	 ('四季感悟','seasons-feeling',8,'0',NULL,2,'admin','2026-08-19 18:01:44','','2026-09-07 10:15:28','人文类','0'),
	 ('SpringBoot实战','springboot-practice',9,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
	 ('React Hooks','react-hooks',10,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0');
INSERT INTO portal_tag (name,slug,sort,status,module,reference_count,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('AI辅助开发','ai-assisted-dev',11,'0',NULL,1,'admin','2026-08-19 18:01:44','','2026-09-07 10:03:06','技术类','0'),
	 ('算法突破','algorithm-breakthrough',12,'0',NULL,1,'admin','2026-08-19 18:01:44','','2026-09-07 10:03:06','技术类','0'),
	 ('Java并发','java-concurrency',13,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
	 ('Vue3实践','vue3-practice',14,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
	 ('微服务架构','microservices',15,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
	 ('MySQL优化','mysql-optimization',16,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
	 ('Git协作','git-collaboration',17,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
	 ('前端性能','frontend-performance',18,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
	 ('JVM调优','jvm-tuning',19,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0'),
	 ('系统设计','system-design',20,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','技术类','0');
INSERT INTO portal_tag (name,slug,sort,status,module,reference_count,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('新手入门','beginner-guide',21,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('进阶提升','advanced-improvement',22,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('面试备战','interview-prep',23,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('读书心得','reading-notes',24,'0',NULL,1,'admin','2026-08-19 18:01:44','','2026-08-28 11:13:21','通用类','0'),
	 ('写作技巧','writing-tips',25,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('学习方法','learning-methods-tag',26,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('职场经验','career-experience',27,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('个人成长','personal-growth',28,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('java springboot',NULL,0,'0','interview_experience',2,'','2026-08-31 10:42:07','','2026-09-07 10:03:06',NULL,'0');
INSERT INTO portal_user (user_id,username,nickname,email,phone,password,avatar,bio,`position`,identity_tag,wechat,gender,birthday,location,website,github,company,school,`language`,timezone,notify_like,notify_comment,notify_follow,notify_system,privacy_follow,privacy_bookmark,privacy_email,privacy_phone,privacy_profile,`role`,is_certified_creator,vip_expire_at,is_phone_verified,is_wechat_verified,two_factor_enabled,status,del_flag,login_ip,login_date,create_by,create_time,update_by,update_time,remark) VALUES
	 (NULL,'moyun_official','墨云官方',NULL,NULL,NULL,NULL,'墨云官方账号 · 每日话题由 AI 生成',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,1,0,0,0,0,'admin',1,NULL,0,0,0,'0','0','',NULL,'admin','2026-09-11 14:53:02','','2026-09-11 14:53:02','系统账号：AI 生成话题专用，SQL 20260911-04 初始化');
INSERT INTO sys_config (config_name,config_key,config_value,platform_code,config_type,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('主框架页-默认皮肤样式名称','sys.index.skinName','skin-blue',NULL,'Y','admin','2026-08-19 18:01:44','admin',NULL,'蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow','0'),
	 ('用户管理-账号初始密码','sys.user.initPassword','123456',NULL,'Y','admin','2026-08-19 18:01:44','',NULL,'初始化密码 123456','0'),
	 ('主框架页-侧边栏主题','sys.index.sideTheme','theme-dark',NULL,'Y','admin','2026-08-19 18:01:44','',NULL,'深色主题theme-dark，浅色主题theme-light','0'),
	 ('账号自助-验证码开关','sys.account.captchaEnabled','true',NULL,'Y','admin','2026-08-19 18:01:44','',NULL,'是否开启验证码功能（true开启，false关闭）','0'),
	 ('账号自助-是否开启用户注册功能','sys.account.registerUser','false',NULL,'Y','admin','2026-08-19 18:01:44','',NULL,'是否开启注册用户功能（true开启，false关闭）','0'),
	 ('用户登录-黑名单列表','sys.login.blackIPList','',NULL,'Y','admin','2026-08-19 18:01:44','',NULL,'设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）','0'),
	 ('平台服务费率','pay.platform.fee-rate','0.10',NULL,'Y','admin','2026-08-28 09:24:22','',NULL,'V11.0 支付分账：平台抽成比例（0.10=10%），运行时生效','0'),
	 ('语音面试默认面试官AgentID','voice.interview.defaultAgentId','48',NULL,'Y','admin','2026-09-07 09:15:33','',NULL,'语音面试绑定的 ai_agent 主键；编辑「AI模块→智能体管理」对应 agent 的人设/提示词/模型即动态生效','0'),
	 ('语音面试时长（分钟）','voice.interview.durationMinutes','20',NULL,'Y','admin','2026-09-17 08:52:14','',NULL,'语音面试全场倒计时时长（分钟，范围5-120）：结束仅由用户主动（按钮/口头）或倒计时归零触发，题数仅作软参考','0');
INSERT INTO sys_config (config_name,config_key,config_value,platform_code,config_type,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('AI能力全局开关','ai.global.enabled','true',NULL,'Y','admin','2026-09-17 09:05:37','',NULL,'AI 能力运行时总开关（网关/Agent/简历/面试全链路），true=开启（默认），false=关闭走规则兜底；管理台修改即时生效','0'),
	 ('简历AI建议开关','ai.resume.advice.enabled','true',NULL,'Y','admin','2026-09-17 09:05:37','',NULL,'简历模块 AI 建议子开关（解析/岗位匹配/深度优化/AI建议），true=开启（默认），false=关闭走规则兜底；管理台修改即时生效','0'),
	 ('VIP体系开关','vip.enabled','false',NULL,'Y','admin','2026-09-17 17:53:21','',NULL,'统一VIP体系总开关：true启用校验 false全员放行（灰度上线用）；端级可覆盖（platform_code=portal/ledger）','0');
INSERT INTO sys_dept (parent_id,ancestors,dept_name,order_num,leader,phone,email,status,del_flag,create_by,create_time,update_by,update_time,remark) VALUES
	 (0,'0','若依科技',0,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (100,'0,100','深圳总公司',1,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (100,'0,100','长沙分公司',2,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (101,'0,100,101','研发部门',1,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (101,'0,100,101','市场部门',2,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (101,'0,100,101','测试部门',3,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (101,'0,100,101','财务部门',4,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (101,'0,100,101','运维部门',5,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (102,'0,100,102','市场部门',1,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (102,'0,100,102','财务部门',2,'若依','15888888888','ry@qq.com','0','0','admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL);
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (1,'男','0','sys_user_sex','','','Y','0','admin','2026-08-19 18:01:44','',NULL,'性别男','0'),
	 (2,'女','1','sys_user_sex','','','N','0','admin','2026-08-19 18:01:44','',NULL,'性别女','0'),
	 (3,'未知','2','sys_user_sex','','','N','0','admin','2026-08-19 18:01:44','',NULL,'性别未知','0'),
	 (1,'显示','0','sys_show_hide','','primary','Y','0','admin','2026-08-19 18:01:44','',NULL,'显示菜单','0'),
	 (2,'隐藏','1','sys_show_hide','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'隐藏菜单','0'),
	 (1,'正常','0','sys_normal_disable','','primary','Y','0','admin','2026-08-19 18:01:44','',NULL,'正常状态','0'),
	 (2,'停用','1','sys_normal_disable','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'停用状态','0'),
	 (1,'正常','0','sys_job_status','','primary','Y','0','admin','2026-08-19 18:01:44','',NULL,'正常状态','0'),
	 (2,'暂停','1','sys_job_status','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'停用状态','0'),
	 (1,'默认','DEFAULT','sys_job_group','','','Y','0','admin','2026-08-19 18:01:44','',NULL,'默认分组','0');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (2,'系统','SYSTEM','sys_job_group','','','N','0','admin','2026-08-19 18:01:44','',NULL,'系统分组','0'),
	 (1,'是','Y','sys_yes_no','','primary','Y','0','admin','2026-08-19 18:01:44','',NULL,'系统默认是','0'),
	 (2,'否','N','sys_yes_no','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'系统默认否','0'),
	 (1,'通知','1','sys_notice_type','','warning','Y','0','admin','2026-08-19 18:01:44','',NULL,'通知','0'),
	 (2,'公告','2','sys_notice_type','','success','N','0','admin','2026-08-19 18:01:44','',NULL,'公告','0'),
	 (1,'正常','0','sys_notice_status','','primary','Y','0','admin','2026-08-19 18:01:44','',NULL,'正常状态','0'),
	 (2,'关闭','1','sys_notice_status','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'关闭状态','0'),
	 (99,'其他','0','sys_oper_type','','info','N','0','admin','2026-08-19 18:01:44','',NULL,'其他操作','0'),
	 (1,'新增','1','sys_oper_type','','info','N','0','admin','2026-08-19 18:01:44','',NULL,'新增操作','0'),
	 (2,'修改','2','sys_oper_type','','info','N','0','admin','2026-08-19 18:01:44','',NULL,'修改操作','0');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (3,'删除','3','sys_oper_type','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'删除操作','0'),
	 (4,'授权','4','sys_oper_type','','primary','N','0','admin','2026-08-19 18:01:44','',NULL,'授权操作','0'),
	 (5,'导出','5','sys_oper_type','','warning','N','0','admin','2026-08-19 18:01:44','',NULL,'导出操作','0'),
	 (6,'导入','6','sys_oper_type','','warning','N','0','admin','2026-08-19 18:01:44','',NULL,'导入操作','0'),
	 (7,'强退','7','sys_oper_type','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'强退操作','0'),
	 (8,'生成代码','8','sys_oper_type','','warning','N','0','admin','2026-08-19 18:01:44','',NULL,'生成操作','0'),
	 (9,'清空数据','9','sys_oper_type','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'清空操作','0'),
	 (1,'成功','0','sys_common_status','','primary','N','0','admin','2026-08-19 18:01:44','',NULL,'正常状态','0'),
	 (2,'失败','1','sys_common_status','','danger','N','0','admin','2026-08-19 18:01:44','',NULL,'停用状态','0'),
	 (1,'待支付','pending','portal_pay_status','','warning','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (2,'已支付','paid','portal_pay_status','','success','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (3,'已退款','refunded','portal_pay_status','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (4,'已关闭','closed','portal_pay_status','','danger','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (5,'支付失败','failed','portal_pay_status','','danger','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (1,'积分','points','portal_pay_channel','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (2,'支付宝','alipay','portal_pay_channel','','primary','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (3,'微信','wechat','portal_pay_channel','','success','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (4,'钱包','wallet','portal_pay_channel','','warning','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (1,'待开始','idle','voice_interview_status','','info','Y','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (2,'聆听中','listening','voice_interview_status','','primary','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (3,'播报中','speaking','voice_interview_status','','success','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (4,'评分中','scoring','voice_interview_status','','warning','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (5,'已结束','done','voice_interview_status','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (1,'专业','professional','voice_interview_style','','primary','Y','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (2,'亲和','friendly','voice_interview_style','','success','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (3,'严格','strict','voice_interview_style','','danger','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (1,'切入点提示','1','voice_interview_hint_level','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (2,'结构提示','2','voice_interview_hint_level','','warning','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (3,'全量提示','3','voice_interview_hint_level','','danger','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (1,'技术岗','tech','portal_resume_category','','primary','Y','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (2,'产品岗','product','portal_resume_category','','success','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (3,'应届生','fresh','portal_resume_category','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (4,'社招','social','portal_resume_category','','warning','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (5,'实习','intern','portal_resume_category','','info','N','0','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 (1,'展示阅读','reading','portal_practice_mode','','default','Y','0','admin','2026-08-20 10:56:17','',NULL,'看题+答案','0'),
	 (2,'选择题','choice','portal_practice_mode','','success','N','0','admin','2026-08-20 10:56:17','',NULL,'选项作答+判分','0'),
	 (3,'编程题','coding','portal_practice_mode','','primary','N','0','admin','2026-08-20 10:56:17','',NULL,'代码作答+测试用例判定','0'),
	 (1,'随时到岗','随时到岗','portal_available_time','','primary','Y','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0'),
	 (2,'一周内到岗','一周内到岗','portal_available_time','','success','N','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0'),
	 (3,'两周内到岗','两周内到岗','portal_available_time','','info','N','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (4,'一个月内到岗','一个月内到岗','portal_available_time','','warning','N','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0'),
	 (5,'三个月内到岗','三个月内到岗','portal_available_time','','info','N','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0'),
	 (6,'面议','面议','portal_available_time','','default','N','0','admin','2026-08-25 00:00:00','',NULL,NULL,'0'),
	 (1,'学生','student','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'学生群体','0'),
	 (2,'上班族','office_worker','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'固定职业收入','0'),
	 (3,'自由职业','freelancer','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'非固定收入','0'),
	 (4,'个体经营者','business_owner','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'经营性收入','0'),
	 (5,'退休','retired','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'退休群体','0'),
	 (6,'其他','other','ledger_identity_tag','','default','N','0','admin','2026-09-04 11:03:26','',NULL,'其他身份','0'),
	 (1,'首页-旭林广告位','home_xulin_ad','portal_ad_slot_key','','success','N','0','admin','2026-08-28 13:16:38','',NULL,'首页-热门推荐上方的旭林广告位','0');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (2,'首页-VIP推广位','home_vip_banner','portal_ad_slot_key','','primary','N','0','admin','2026-08-28 13:16:38','',NULL,'首页右侧/移动端下方的VIP推广位','0');
INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('用户性别','sys_user_sex','0','admin','2026-08-19 18:01:44','',NULL,'用户性别列表','0'),
	 ('菜单状态','sys_show_hide','0','admin','2026-08-19 18:01:44','',NULL,'菜单状态列表','0'),
	 ('系统开关','sys_normal_disable','0','admin','2026-08-19 18:01:44','',NULL,'系统开关列表','0'),
	 ('任务状态','sys_job_status','0','admin','2026-08-19 18:01:44','',NULL,'任务状态列表','0'),
	 ('任务分组','sys_job_group','0','admin','2026-08-19 18:01:44','',NULL,'任务分组列表','0'),
	 ('系统是否','sys_yes_no','0','admin','2026-08-19 18:01:44','',NULL,'系统是否列表','0'),
	 ('通知类型','sys_notice_type','0','admin','2026-08-19 18:01:44','',NULL,'通知类型列表','0'),
	 ('通知状态','sys_notice_status','0','admin','2026-08-19 18:01:44','',NULL,'通知状态列表','0'),
	 ('操作类型','sys_oper_type','0','admin','2026-08-19 18:01:44','',NULL,'操作类型列表','0'),
	 ('系统状态','sys_common_status','0','admin','2026-08-19 18:01:44','',NULL,'登录状态列表','0');
INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('支付状态','portal_pay_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 支付状态','0'),
	 ('支付渠道','portal_pay_channel','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 支付渠道','0'),
	 ('打赏目标类型','portal_tip_target_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 打赏目标类型','0'),
	 ('钱包交易类型','portal_wallet_txn_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 钱包交易类型','0'),
	 ('文章状态','cms_article_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 文章状态','0'),
	 ('专栏状态','cms_column_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 专栏状态','0'),
	 ('话题状态','cms_topic_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 话题状态','0'),
	 ('征文活动状态','cms_contest_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 征文活动状态','0'),
	 ('审核任务类型','cms_audit_task_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 审核任务类型','0'),
	 ('审核任务状态','cms_audit_task_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 审核任务状态','0');
INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('反馈类型','cms_feedback_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 反馈类型','0'),
	 ('举报类型','cms_report_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 举报类型','0'),
	 ('处理状态','cms_handle_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 处理状态','0'),
	 ('书籍类型','portal_book_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 书籍类型','0'),
	 ('书籍连载状态','portal_book_serial_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 书籍连载状态','0'),
	 ('访问级别','portal_access_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 访问级别','0'),
	 ('业务通用状态','portal_common_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 业务通用状态','0'),
	 ('学习计划类型','portal_study_plan_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 学习计划类型','0'),
	 ('学习计划状态','portal_study_plan_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 学习计划状态','0'),
	 ('错题状态','portal_wrong_question_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 错题状态','0');
INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('题目难度','portal_question_difficulty','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 题目难度','0'),
	 ('题目类型','portal_question_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 题目类型','0'),
	 ('简历模板分类','portal_resume_category','0','admin','2026-08-19 18:01:46','',NULL,'v10.2 简历模板分类（英文值）','0'),
	 ('广告位标识','portal_ad_slot_key','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 广告位标识','0'),
	 ('VIP套餐状态','cms_vip_status','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 VIP套餐状态','0'),
	 ('登录端类型','sys_login_type','0','admin','2026-08-19 18:01:46','',NULL,'v9.6 登录端类型','0'),
	 ('语音面试状态','voice_interview_status','0','admin','2026-08-19 18:01:46','',NULL,'v10.1 语音面试状态','0'),
	 ('面试官风格','voice_interview_style','0','admin','2026-08-19 18:01:46','',NULL,'v10.1 面试官风格','0'),
	 ('提示级别','voice_interview_hint_level','0','admin','2026-08-19 18:01:46','',NULL,'v10.1 提示级别','0'),
	 ('练习模式','portal_practice_mode','0','admin','2026-08-20 10:56:17','',NULL,'题目练习模式：reading/choice/coding','0');
INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('简历到岗时间','portal_available_time','0','admin','2026-08-25 00:00:00','',NULL,'v10.10 简历求职意向-到岗时间（值为中文文本，直接入库）','0'),
	 ('记账-身份标签','ledger_identity_tag','0','admin','2026-09-04 11:03:26','',NULL,'AI 财务分析用户画像身份标签','0');
INSERT INTO sys_job (job_name,job_group,invoke_target,cron_expression,misfire_policy,concurrent,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('系统默认（无参）','DEFAULT','ryTask.ryNoParams','0/10 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),
	 ('系统默认（有参）','DEFAULT','ryTask.ryParams(''ry'')','0/15 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),
	 ('系统默认（多参）','DEFAULT','ryTask.ryMultipleParams(''ry'', true, 2000, 316.50, 100)','0/20 * * * * ?','3','1','1','admin','2026-07-28 15:42:36','',NULL,'','0'),
	 ('敏感词扫描-话题','DEFAULT','sensitiveScanTask.scanTopics()','0 0 3 * * ?','3','1','0','admin','2026-08-19 18:01:41','',NULL,'定时扫描话题内容，命中则转待审核','0'),
	 ('敏感词扫描-观点','DEFAULT','sensitiveScanTask.scanTopicPosts()','0 5 3 * * ?','3','1','0','admin','2026-08-19 18:01:41','',NULL,'定时扫描话题观点，命中则标记','0'),
	 ('缓存清理-表结构','DEFAULT','cacheCleanupTask.cleanupExpiredCache()','0 */5 * * * ?','3','1','0','admin','2026-08-19 18:01:42','',NULL,'每5分钟清理 DataSourceService 的过期表结构缓存（原 CacheCleanupTask @Scheduled，迁移至 Quartz 统一调度）','0'),
	 ('缓存清理-限流器','DEFAULT','cacheCleanupTask.cleanupRateLimiter()','0 */2 * * * ?','3','1','0','admin','2026-08-19 18:01:42','',NULL,'每2分钟清理 RateLimiter 过期数据（原 CacheCleanupTask @Scheduled，迁移至 Quartz 统一调度）','0'),
	 ('会话清理-数据分析','DEFAULT','dataAnalysisConversationServiceImpl.cleanExpiredSessions()','0 */30 * * * ?','3','1','0','admin','2026-08-19 18:01:42','',NULL,'每30分钟清理超过1小时未访问的数据分析对话会话（原 DataAnalysisConversationServiceImpl @Scheduled，迁移至 Quartz 统一调度）','0'),
	 ('日志落盘-Token使用','DEFAULT','tokenUsageServiceImpl.flushLogsToDB()','0 * * * * ?','1','1','0','admin','2026-08-19 18:01:42','',NULL,'每1分钟将 Redis 中的 Token 使用日志批量写入 DB（原 TokenUsageServiceImpl @Scheduled，迁移至 Quartz 统一调度；misfire=1 立即补偿避免日志丢失）','0'),
	 ('敏感词扫描-文章评论','DEFAULT','sensitiveScanTask.scanArticleComments()','0 10 3 * * ?','3','1','0','admin','2026-08-19 18:01:45','',NULL,'扫描已发布文章评论(portal_comment.status=1)，命中敏感词转驳回(status=2)并通知作者','0');
INSERT INTO sys_job (job_name,job_group,invoke_target,cron_expression,misfire_policy,concurrent,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('敏感词扫描-面经评论','DEFAULT','sensitiveScanTask.scanInterviewComments()','0 15 3 * * ?','3','1','0','admin','2026-08-19 18:01:45','',NULL,'扫描已发布面经评论(portal_interview_comment.status=published)，命中敏感词转rejected并通知作者','0'),
	 ('AI每日写作Prompt生成','DEFAULT','writingPromptTask.generateDailyPrompt()','0 10 0 * * ?','3','1','0','admin','2026-08-25 00:00:00','',NULL,'每天00:10为今天+明天生成写作提示（结合节日节气，AI失败回退内置主题池）','0');
-- sys_menu 种子已移至 moyun-menu-redo.sql（新版菜单体系，含平台管理/渠道管理）
INSERT INTO sys_platform (platform_code,platform_name,platform_type,description,`domain`,icon,sort_order,status,create_time) VALUES
	 ('portal','门户端','c端','求职、学习、成长','www.xulin.com','portal',1,1,'2026-09-17 17:53:20'),
	 ('ledger','记账端','c端','个人资产管理','ledger.xulin.com','ledger',2,1,'2026-09-17 17:53:20'),
	 ('admin','管理端','b端','后台管理','admin.xulin.com','admin',3,1,'2026-09-17 17:53:20'),
	 ('personality','人格分析端','c端','AI人格分析（预留）','me.xulin.com','peoples',4,1,'2026-09-17 17:53:20');
INSERT INTO sys_post (post_code,post_name,post_sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('ceo','董事长',1,'0','admin','2026-08-19 18:01:44','',NULL,'','0'),
	 ('se','项目经理',2,'0','admin','2026-08-19 18:01:44','',NULL,'','0'),
	 ('hr','人力资源',3,'0','admin','2026-08-19 18:01:44','',NULL,'','0'),
	 ('user','普通员工',4,'0','admin','2026-08-19 18:01:44','',NULL,'','0');
INSERT INTO sys_role (role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,update_by,update_time,remark) VALUES
	 ('超级管理员','admin',1,'1',1,1,'0','0','admin','2026-08-19 18:01:44','',NULL,'超级管理员'),
	 ('普通角色','common',2,'2',1,1,'0','0','admin','2026-08-19 18:01:44','',NULL,'普通角色');
INSERT INTO sys_role_dept (role_id,dept_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (2,100,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),
	 (2,101,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),
	 (2,105,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL);
-- 旧 sys_role_menu 已移除：旧授权引用 dev 库原 menu_id，与新版菜单不兼容；超管全量授权见 moyun-menu-redo.sql
INSERT INTO sys_sensitive_word (word,category,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('示例敏感词1','other','0','admin','2026-08-19 18:01:41',NULL,NULL,'示例词，生产环境请替换为真实词库','0'),
	 ('示例敏感词2','ad','0','admin','2026-08-19 18:01:41',NULL,NULL,'示例词，生产环境请替换为真实词库','0'),
	 ('示例-广告','ad','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
	 ('示例-辱骂','insult','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
	 ('示例-色情','porn','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
	 ('示例-政治','politics','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
	 ('示例-其他','other','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0');
INSERT INTO sys_user (dept_id,user_name,nick_name,user_type,email,phonenumber,sex,avatar,password,status,del_flag,login_ip,login_date,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,'admin','若依','00','ry@163.com','15888888888','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1','2026-09-17 13:39:08','admin','2026-08-19 18:01:44','','2026-09-17 13:39:07','管理员'),
	 (1,'ry','若依','00','ry@qq.com','15666666666','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1','2026-08-19 18:01:44','admin','2026-08-19 18:01:44','admin','2026-09-17 13:40:22','测试员');
INSERT INTO sys_user_post (user_id,post_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,1,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),
	 (2,2,'',NULL,'',NULL,NULL);
INSERT INTO sys_user_role (user_id,role_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (2,2,'',NULL,'',NULL,NULL);
INSERT INTO vip_api_registry (api_path,http_method,controller_class,method_name,platform_code,benefit_code,consume,message,api_desc,enabled,scan_time,create_time,update_time) VALUES
	 ('/portal/interview/voice/start','POST','com.moyun.portal.controller.PortalVoiceInterviewController','start','portal','interview_unlimited',1,'免费面试次数已用完，语音面试为会员专属功能，请开通会员',NULL,1,'2026-09-17 18:03:47','2026-09-17 18:03:47','2026-09-17 18:03:47'),
	 ('/portal/ledger/ai/analysis/task','POST','com.moyun.ledger.controller.PortalLedgerAiController','submitTask','ledger','ai_analysis',1,'本月免费分析次数已用完，请开通记账VIP',NULL,1,'2026-09-17 18:03:47','2026-09-17 18:03:47','2026-09-17 18:03:47'),
	 ('/portal/resume/optimize/deep/{resumeId}/{jobTargetId}','POST','com.moyun.portal.controller.PortalResumeOptimizeController','deepOptimize','portal','resume_optimize',1,'简历深度优化次数已用完，请开通会员',NULL,1,'2026-09-17 18:03:47','2026-09-17 18:03:47','2026-09-17 18:03:47'),
	 ('/portal/resume/optimize/deep/{resumeId}/{jobTargetId}/async','POST','com.moyun.portal.controller.PortalResumeOptimizeController','deepOptimizeAsync','portal','resume_optimize',1,'简历深度优化次数已用完，请开通会员',NULL,1,'2026-09-17 18:03:47','2026-09-17 18:03:47','2026-09-17 18:03:47');
INSERT INTO vip_benefit (platform_code,benefit_code,benefit_name,description,sort_order,create_time) VALUES
	 ('portal','interview_unlimited','语音面试','不限次 AI 语音面试',1,'2026-09-17 17:53:20'),
	 ('portal','resume_optimize','简历深度优化','AI 逐项建议/前后对比/采纳保存',2,'2026-09-17 17:53:20'),
	 ('portal','report_share','报告分享','面试报告分享导出',3,'2026-09-17 17:53:20'),
	 ('portal','priority_queue','优先队列','面试优先调度',4,'2026-09-17 17:53:20'),
	 ('portal','reading_unlimited','读书空间','不限次阅读',5,'2026-09-17 17:53:20'),
	 ('portal','article_paid','付费文章','免费阅读付费文章',6,'2026-09-17 17:53:20'),
	 ('ledger','bill_parse','账单识别','每月账单截图识别次数',1,'2026-09-17 17:53:20'),
	 ('ledger','ai_analysis','AI 分析','AI 财务分析次数',2,'2026-09-17 17:53:20');
INSERT INTO vip_tier (platform_code,tier_code,tier_name,duration_days,price,original_price,popular,description,sort_order,status,create_time) VALUES
	 ('portal','free','免费版',0,0.00,NULL,0,'基础体验额度',1,1,'2026-09-17 17:53:20'),
	 ('portal','monthly','月卡会员',30,49.00,69.00,0,'全功能月度畅用',2,1,'2026-09-17 17:53:20'),
	 ('portal','yearly','年卡会员',365,399.00,588.00,1,'最受欢迎，全年畅用',3,1,'2026-09-17 17:53:20'),
	 ('portal','permanent','永久会员',-1,1299.00,1999.00,0,'一次买断终身可用',4,1,'2026-09-17 17:53:20'),
	 ('ledger','free','免费版',0,0.00,NULL,0,'基础记账体验',1,1,'2026-09-17 17:53:20'),
	 ('ledger','yearly','年卡会员',365,199.00,299.00,1,'智能账单识别 + AI 分析',2,1,'2026-09-17 17:53:20');
INSERT INTO vip_tier_benefit (platform_code,tier_code,benefit_code,benefit_value,period,create_time) VALUES
	 ('portal','free','interview_unlimited','2','unlimited','2026-09-17 17:53:20'),
	 ('portal','free','resume_optimize','1','month','2026-09-17 17:53:20'),
	 ('portal','free','reading_unlimited','3','month','2026-09-17 17:53:20'),
	 ('portal','monthly','interview_unlimited','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','monthly','resume_optimize','5','month','2026-09-17 17:53:20'),
	 ('portal','monthly','report_share','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','monthly','reading_unlimited','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','monthly','article_paid','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','yearly','interview_unlimited','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','yearly','resume_optimize','20','month','2026-09-17 17:53:20');
INSERT INTO vip_tier_benefit (platform_code,tier_code,benefit_code,benefit_value,period,create_time) VALUES
	 ('portal','yearly','report_share','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','yearly','priority_queue','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','yearly','reading_unlimited','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','yearly','article_paid','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','permanent','interview_unlimited','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','permanent','resume_optimize','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','permanent','report_share','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','permanent','priority_queue','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','permanent','reading_unlimited','unlimited','unlimited','2026-09-17 17:53:20'),
	 ('portal','permanent','article_paid','unlimited','unlimited','2026-09-17 17:53:20');
INSERT INTO vip_tier_benefit (platform_code,tier_code,benefit_code,benefit_value,period,create_time) VALUES
	 ('ledger','free','bill_parse','5','month','2026-09-17 17:53:21'),
	 ('ledger','free','ai_analysis','3','month','2026-09-17 17:53:21'),
	 ('ledger','yearly','bill_parse','100','month','2026-09-17 17:53:21'),
	 ('ledger','yearly','ai_analysis','unlimited','unlimited','2026-09-17 17:53:21');
SET FOREIGN_KEY_CHECKS = 1;
