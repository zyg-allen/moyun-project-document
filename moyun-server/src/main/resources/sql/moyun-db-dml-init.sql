INSERT INTO `moyun-db`.ai_agent (name,description,system_prompt,knowledge_library_ids,knowledge_base_weights,model_config_id,model_name,temperature,max_tokens,rag_min_score,rag_max_results,enabled,welcome_message,suggested_questions,show_citations,max_history_turns,api_enabled,api_key,workflow_id,workflow_trigger_mode,workflow_trigger_keywords,publish_enabled,publish_token,publish_settings,create_time,update_time,rag_recall_multiplier,rag_enable_hybrid_search,rag_enable_query_expansion,rag_bm25_weight,rag_vector_weight,enable_self_reflection,deleted) VALUES
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
INSERT INTO `moyun-db`.ai_agent_dictionary_relation (agent_id,dictionary_id,enabled,create_time) VALUES
	 (47,4,1,'2026-09-11 16:16:36'),
	 (47,8,1,'2026-09-11 16:16:36'),
	 (48,4,1,'2026-09-16 14:04:15'),
	 (48,6,1,'2026-09-16 14:04:15'),
	 (48,8,1,'2026-09-16 14:04:15');
INSERT INTO `moyun-db`.ai_agent_tool (name,display_name,description,category,tool_type,icon,config,parameters,timeout_seconds,enabled,is_system,create_time,update_time,deleted) VALUES
	 ('current_time','当前时间','获取当前的日期和时间，可指定时区和格式','utility','builtin','fa-clock',NULL,'{"type": "object", "required": [], "properties": {"format": {"type": "string", "default": "yyyy-MM-dd HH:mm:ss", "description": "时间格式，默认yyyy-MM-dd HH:mm:ss"}, "timezone": {"type": "string", "default": "Asia/Shanghai", "description": "时区，如Asia/Shanghai，默认北京时间"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('calculator','数学计算','执行数学计算，支持加减乘除、幂运算、开方、三角函数等','utility','builtin','fa-calculator',NULL,'{"type": "object", "required": ["expression"], "properties": {"expression": {"type": "string", "description": "数学表达式，如(1+2)*3、sqrt(16)、sin(30)"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('weather_query','天气查询','查询指定城市的实时天气和未来天气预报（真实数据，Open-Meteo免费接口），包括温度、湿度、风向、天气状况等，预报最多7天','information','builtin','fa-cloud-sun','{"data_source": "open-meteo"}','{"type": "object", "required": ["city"], "properties": {"city": {"type": "string", "description": "城市名称，如北京、上海、广州"}, "days": {"type": "integer", "default": 1, "description": "预报天数1-7，默认1天"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('web_search','网络搜索','搜索互联网获取最新信息（真实数据，必应免费接口），适用于查询新闻、事件、知识等实时内容','information','builtin','fa-search','{"data_source": "bing"}','{"type": "object", "required": ["query"], "properties": {"count": {"type": "integer", "default": 5, "description": "返回结果数量，默认5条"}, "query": {"type": "string", "description": "搜索关键词"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('url_reader','网页读取','读取指定URL的网页内容，提取主要文本信息','information','http','fa-globe','{"timeout": 10}','{"type": "object", "required": ["url"], "properties": {"url": {"type": "string", "description": "要读取的网页URL"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('translator','文本翻译','将文本翻译成指定语言，支持中英日韩等多种语言互译','utility','http','fa-language','{"api_type": "aliyun"}','{"type": "object", "required": ["text"], "properties": {"to": {"type": "string", "default": "zh", "description": "目标语言代码，如zh/en/ja"}, "from": {"type": "string", "default": "auto", "description": "源语言代码，如zh/en/ja，可设为auto自动检测"}, "text": {"type": "string", "description": "要翻译的文本"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('send_email','发送邮件','发送邮件，支持纯文本和HTML格式，支持多收件人、抄送、密送','action','builtin','fa-envelope','{}','{"type": "object", "required": ["to", "subject", "content"], "properties": {"cc": {"type": "string", "description": "抄送人邮箱，多个用英文逗号分隔，可选"}, "to": {"type": "string", "description": "收件人邮箱，多个用英文逗号分隔"}, "bcc": {"type": "string", "description": "密送人邮箱，多个用英文逗号分隔，可选"}, "isHtml": {"type": "boolean", "default": false, "description": "正文是否为HTML格式，默认false"}, "content": {"type": "string", "description": "邮件正文，纯文本或HTML"}, "subject": {"type": "string", "description": "邮件主题"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0),
	 ('database_query','数据库查询','查询数据库并返回智能分析结果。使用自然语言提问，系统自动生成SQL执行，返回查询结果、统计分析和图表推荐。支持MySQL数据源。','data','database','fa-database','{"max_rows": 100}','{"type": "object", "required": ["datasource_id", "query"], "properties": {"query": {"type": "string", "description": "自然语言查询问题，如：统计工具表中已启用的工具数量"}, "need_chart": {"type": "boolean", "default": true, "description": "是否需要图表推荐，默认true"}, "datasource_id": {"type": "integer", "description": "数据源ID，须为数据源管理中已配置的数据源"}, "need_analysis": {"type": "boolean", "default": true, "description": "是否需要智能分析，默认true"}}}',30,1,1,'2025-11-25 15:06:30','2025-11-25 15:06:30',0);
INSERT INTO `moyun-db`.ai_agent_tool_relation (agent_id,tool_id,custom_config,enabled,create_time) VALUES
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
INSERT INTO `moyun-db`.ai_chart_recommendation_rule (rule_name,data_pattern,field_types,data_characteristics,recommended_chart,priority,reason,min_data_points,max_data_points,enabled,create_time) VALUES
	 ('时间序列-折线图','time_series',NULL,NULL,'line',95,'时间趋势最适合用折线图展示',2,999999,1,'2025-11-29 14:21:54'),
	 ('分类占比-饼图','category',NULL,NULL,'pie',85,'少量分类适合饼图',2,6,1,'2025-11-29 14:21:54'),
	 ('分类对比-柱状图','category',NULL,NULL,'bar',90,'多分类对比适合柱状图',3,999999,1,'2025-11-29 14:21:54'),
	 ('数值分布-直方图','distribution',NULL,NULL,'histogram',90,'数值分布最适合用直方图',10,999999,1,'2025-11-29 14:21:54'),
	 ('排名-条形图','ranking',NULL,NULL,'bar',90,'排名对比适合条形图',3,50,1,'2025-11-29 14:21:54'),
	 ('相关性-散点图','correlation',NULL,NULL,'scatter',85,'相关性分析适合散点图',10,999999,1,'2025-11-29 14:21:54'),
	 ('多维对比-雷达图','multi_dimension',NULL,NULL,'radar',75,'多维度对比适合雷达图',3,8,1,'2025-11-29 14:21:54');
INSERT INTO `moyun-db`.ai_conversation (agent_id,title,user_id,message_count,create_time,update_time,summary,summary_updated_at,deleted) VALUES
	 (47,'继续',NULL,12,'2026-09-01 15:32:21','2026-09-07 17:26:08',NULL,NULL,0),
	 (47,'我薪资15900，欠债10万，',NULL,8,'2026-09-10 15:25:28','2026-09-15 15:47:01',NULL,NULL,0),
	 (48,'你好我是钟永国',NULL,26,'2026-09-11 14:56:04','2026-09-11 15:54:01',NULL,NULL,0);
INSERT INTO `moyun-db`.ai_knowledge_library_config (library_id,segment_mode,segment_separator,segment_max_length,segment_overlap_length,preprocess_replace_spaces,preprocess_remove_urls,preprocess_remove_extra_newlines,index_mode,embedding_model,retrieval_mode,retrieval_top_k,rerank_enabled,rerank_model,created_at,updated_at) VALUES
	 (12,'general','

',800,100,1,1,1,'high_quality',NULL,'hybrid',10,0,NULL,'2026-09-10 14:40:14','2026-09-10 14:40:14'),
	 (13,'general','

',800,100,1,1,1,'high_quality',NULL,'hybrid',10,0,NULL,'2026-09-16 13:45:09','2026-09-16 13:45:09');
INSERT INTO `moyun-db`.ai_model_config (name,provider,model_type,model_name,api_key,base_url,temperature,max_tokens,timeout,streaming_supported,supports_json_mode,enabled,is_default,description,create_time,update_time,input_price,output_price,deleted) VALUES
	 ('通义千问-多模态Embedding','dashscope','embedding','text-embedding-v3','ENC:7/3JCnmmCxrmLHVDvy06rR87in5AZZVyXi9FeMzSH5uoHBnBg12lP55YHw5J/RdzOqFIiZRg2gzOS0K5zW2raIt/erTxKeQzvDu/HAbvLr1XkOBD+uORu9tBFkBFXjj0Gf/vmKdE/9nY4vwnTVMx45DaKfyk3YGo9aczA8MHOwQaJohrApt3azoyjpKe6Ggt','https://dashscope.aliyuncs.com/compatible-mode/v1',0.3,4089,60,0,0,1,1,'通义千问多模态 Embedding 模型，支持图片和文本的联合向量化，用于图文混合搜索','2025-11-21 17:12:03','2026-09-07 09:13:40',0.000500,0.000000,0),
	 ('通义千问-VL-Plus','dashscope','chat','qwen-vl-plus','ENC:etybEZmvkCnWb5NTpGzl4XdvyqfUn+Pef7mBT8X7yw==',NULL,0.7,2000,60,1,0,0,0,'通义千问视觉理解模型Plus版本，支持图片内容识别和描述，用于文档图片的多模态理解','2025-11-22 12:16:42','2026-09-07 09:29:14',0.001000,0.002000,0),
	 ('qwen3.8-max','dashscope','chat','qwen3.8-max','ENC:/ZTXHHvxIY3PkQ/TAQ5h1c//aQw/5pn41SL5kRp9/VSUw3RXNqsp21jKi1C8ReLdMNW+AQ+E8om7II/SzM6PsmFKZso93y5rBsRp8D62AoL9/G8cRHoIukzIbaZV9kTKQ8t5rYFVoD5/ZELu1KV6tBFKDErFK6GqN+OrROoDgSWn7kYDbU7Cln+I9Yr6oP1O','https://dashscope.aliyuncs.com/compatible-mode/v1',NULL,4000,180,1,0,1,0,'Qwen3 重排序模型，用于提升检索结果的相关性排序，支持中英文等100+语言','2026-01-22 15:34:36','2026-09-07 09:29:14',0.000100,0.000000,0),
	 ('deepSeekV4','deepseek','chat','deepseek-v4-pro','ENC:L9tek34q0dxfmrtaB7aHg1VKJeZvLRUKUOyhCQuCyo6wLUIuHDI3qJVQIuQ8p2gq2vR0ej2qfNnOaSFlL3DV','https://api.deepseek.com',0.7,2000,180,1,0,1,1,'','2026-09-07 09:29:09','2026-09-11 09:47:51',0.001000,0.002000,0);
INSERT INTO `moyun-db`.ai_provider (code,name,api_style,default_base_url,supports_streaming,requires_api_key,enabled,sort_order,remark,create_time,update_time,deleted) VALUES
	 ('openai','OpenAI','openai_compatible','https://api.openai.com/v1',1,1,1,1,'OpenAI 官方及兼容端点','2026-09-07 09:15:27',NULL,0),
	 ('dashscope','通义千问(百炼)','openai_compatible','https://dashscope.aliyuncs.com/compatible-mode/v1',1,1,1,2,'阿里百炼，运行时走 OpenAI 兼容模式','2026-09-07 09:15:27',NULL,0),
	 ('ollama','Ollama','ollama_native','http://localhost:11434',1,0,1,3,'本地 Ollama 服务，无需 API Key','2026-09-07 09:15:27',NULL,0),
	 ('deepseek','DeepSeek','openai_compatible','https://api.deepseek.com',1,1,1,4,'示例：OpenAI 兼容，后台一键启用','2026-09-07 09:15:27',NULL,0),
	 ('moonshot','Moonshot Kimi','openai_compatible','https://api.moonshot.cn/v1',1,1,1,5,'示例：OpenAI 兼容，后台一键启用','2026-09-07 09:15:27',NULL,0);
INSERT INTO `moyun-db`.ai_scene_config (scene_code,scene_name,description,scene_category,agent_id,model_config_id,knowledge_library_ids,tool_ids,workflow_id,config_json,handler_bean_name,handler_method,system_prompt_template,user_prompt_template,prompt_placeholders,output_mode,output_schema,output_parser,max_tokens,temperature,timeout_seconds,retry_count,rate_limit_key,rate_limit_count,rate_limit_time,daily_token_limit,enable_output_filter,fallback_model_id,fallback_response,enable_cache,cache_ttl,version,weight,priority,is_default,enabled,open_api,create_time,update_time,deleted) VALUES
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
INSERT INTO `moyun-db`.ledger_app_feature_config (feature_key,feature_name,icon,icon_color,group_type,sort_num,visible,status,badge,create_by,create_time,update_by,update_time,remark) VALUES
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
INSERT INTO `moyun-db`.ledger_app_feature_config (feature_key,feature_name,icon,icon_color,group_type,sort_num,visible,status,badge,create_by,create_time,update_by,update_time,remark) VALUES
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
INSERT INTO `moyun-db`.ledger_app_feature_config (feature_key,feature_name,icon,icon_color,group_type,sort_num,visible,status,badge,create_by,create_time,update_by,update_time,remark) VALUES
	 ('list','账单','☑','#7fbf94','recommend',2,1,'done',NULL,'','2026-09-14 13:08:02','admin','2026-09-14 13:10:57',''),
	 ('translate','翻译','文A','#7fbf94','recommend',20,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('stock','库存管理','📦','#7fbf94','recommend',21,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('gold','记黄金','💰','#7fbf94','recommend',22,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('coupon','优惠券','🎫','#7fbf94','recommend',23,0,'dev',NULL,'','2026-09-14 13:08:02','','2026-09-14 13:08:02',''),
	 ('portal','墨韵社区','🌐','#7fbf94','recommend',3,1,'done',NULL,'','2026-09-14 14:04:28','','2026-09-14 14:04:28','http://localhost:3000');
INSERT INTO `moyun-db`.ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
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
INSERT INTO `moyun-db`.ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
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
INSERT INTO `moyun-db`.ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
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
INSERT INTO `moyun-db`.ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
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
INSERT INTO `moyun-db`.ledger_category (user_id,name,`type`,group_name,parent_id,icon,color,sort_order,is_system,status,create_time,update_time) VALUES
	 (0,'理财收益','income',NULL,NULL,'invest','#16A085',4,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'红包','income',NULL,NULL,'redpacket','#E74C3C',5,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'退款','income',NULL,NULL,'refund','#5DADE2',6,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'借入','income',NULL,NULL,'borrow-in','#7F8C8D',7,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'二手闲置','income',NULL,NULL,'secondhand','#AF7AC5',8,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'其他收入','income',NULL,NULL,'other','#BDC3C7',9,1,1,'2026-09-08 13:37:08','2026-09-08 13:37:08'),
	 (0,'士大夫但是','adjust',NULL,17,'','#6a4fd4',0,1,1,'2026-09-14 13:23:47','2026-09-14 13:23:47'),
	 (6,'哈哈哈','expense',NULL,NULL,NULL,NULL,0,0,1,'2026-09-14 13:47:13','2026-09-14 13:47:13');
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.portal_category (name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('面试指南','interview','面试中心主入口',NULL,0,52,'0',1,'static','/interview',NULL,'special',0,'','2026-09-02 13:23:29','','2026-09-02 13:23:29',NULL,'0');
INSERT INTO `moyun-db`.portal_friend_link (name,url,description,logo,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('中国作家网','https://www.chinawriter.com.cn','中国作家协会官方网站',NULL,1,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0'),
	 ('起点中文网','https://www.qidian.com','阅文集团旗下网站',NULL,2,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0'),
	 ('掘金','https://juejin.cn','帮助开发者成长的社区',NULL,3,'0','admin','2026-07-28 15:44:22','','2026-07-28 15:44:22',NULL,'0');
INSERT INTO `moyun-db`.portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
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
INSERT INTO `moyun-db`.portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
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
INSERT INTO `moyun-db`.portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
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
INSERT INTO `moyun-db`.portal_growth_rule (module,`action`,growth_delta,daily_limit,description,status,sort,create_by,create_time,update_by,update_time,remark) VALUES
	 ('interview','read_question',1,20,'阅读题目','0',19,'admin','2026-08-31 09:11:49','',NULL,'题库阅读学习行为，同题每日仅记一次');
INSERT INTO `moyun-db`.portal_help_category (name,icon,description,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('发布与编辑','BookOpen','文章发布、编辑、删除等操作指南',1,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),
	 ('账号与安全','HelpCircle','登录、注册、密码、安全设置',2,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),
	 ('互动功能','MessageSquare','评论、点赞、关注等互动功能',3,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0'),
	 ('社区规则','Shield','使用规范、违规处理、隐私政策',4,'active','','2026-07-28 15:52:20','','2026-07-28 15:52:20',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_attempt (question_id,user_id,attempt_count,last_attempt_at,status,first_solved_at,last_solved_at,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (1,7,3,'2026-08-28 13:44:24','solved','2026-08-28 13:44:02','2026-08-28 13:44:24','','2026-08-28 13:44:02','','2026-08-28 13:44:24',NULL,'0'),
	 (5,7,1,'2026-08-28 13:44:45','solved','2026-08-28 13:44:45','2026-08-28 13:44:45','','2026-08-28 13:44:44','','2026-08-28 13:44:44',NULL,'0'),
	 (1,6,3,'2026-09-15 11:00:18','solved','2026-09-07 17:08:33','2026-09-15 11:00:18','','2026-08-28 16:27:46','','2026-09-15 11:00:17',NULL,'0'),
	 (2,6,1,'2026-09-15 11:00:47','solved','2026-09-15 11:00:47','2026-09-15 11:00:47','','2026-09-15 11:00:46','','2026-09-15 11:00:46',NULL,'0'),
	 (3,6,1,'2026-09-15 11:01:33','solved','2026-09-15 11:01:33','2026-09-15 11:01:33','','2026-09-15 11:01:33','','2026-09-15 11:01:33',NULL,'0'),
	 (4,6,1,'2026-09-15 11:01:48','solved','2026-09-15 11:01:48','2026-09-15 11:01:48','','2026-09-15 11:01:48','','2026-09-15 11:01:48',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_category (name,slug,description,icon,sort,question_count,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('算法与数据结构','algorithm','算法题、数据结构相关面试题','fa-code',1,150,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
	 ('系统设计','system-design','系统架构设计、分布式系统等面试题','fa-sitemap',2,60,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
	 ('前端开发','frontend','JavaScript、CSS、Vue、React等前端技术面试题','fa-laptop-code',3,120,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
	 ('后端开发','backend','Java、Python、Go等后端技术面试题','fa-server',4,130,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0'),
	 ('数据库','database','MySQL、Redis等数据库相关面试题','fa-database',5,80,'active','','2026-07-28 15:46:12','','2026-07-28 15:46:12',NULL,'0');
INSERT INTO `moyun-db`.portal_interview_config (config_name,persona_type,prompt_template,scoring_weights,question_weights,max_followups,followup_triggers,enable_self_intro,self_intro_duration,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('默认面试配置','professional','','{"selfIntro":{"structure":30,"awareness":25,"matching":25,"fluency":20},"llmRatio":70,"total":{"intro":20,"tech":80}}','{"job":40,"resume":30,"weak":20,"random":10}',4,'["vague_answer","contradiction","depth_needed"]',1,180,1,'active','admin','2026-09-07 16:37:36','','2026-09-07 16:42:04','系统默认：llmRatio=LLM融合比例(0-100)；selfIntro=自我介绍4维权重；total=总分权重(intro/tech)；关闭自我介绍以兼容旧流程','0');
INSERT INTO `moyun-db`.portal_interview_position (code,name,industry,`level`,required_skills,hot_companies,description,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('java_backend','Java后端工程师','互联网','mid','["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]','["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]','Java 后端工程师岗位，重点考察 Java 基础、Spring 全家桶、MySQL/Redis、分布式与微服务、JVM 与并发编程',1,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0'),
	 ('frontend','前端工程师','互联网','mid','["JavaScript","TypeScript","Vue","React","HTML","CSS","Node.js","Webpack","Vite","性能优化","浏览器原理","HTTP"]','["阿里","腾讯","字节跳动","美团","京东","百度","网易","小米","Shopee","滴滴"]','前端工程师岗位，重点考察 JS/TS 基础、Vue/React 框架、工程化、浏览器原理、性能优化、HTTP 与网络',2,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0'),
	 ('algorithm','算法工程师','互联网','mid','["算法","数据结构","动态规划","图论","字符串","数组","链表","树","递归","排序","机器学习","深度学习","数学"]','["阿里","腾讯","字节跳动","百度","美团","快手","小红书","华为","商汤","旷视"]','算法工程师岗位，重点考察数据结构与算法、动态规划、图论、字符串算法、机器学习与深度学习基础',3,'active','','2026-07-28 16:39:34','','2026-07-28 16:39:34',NULL,'0');
INSERT INTO `moyun-db`.portal_tag (name,slug,sort,status,module,reference_count,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.portal_tag (name,slug,sort,status,module,reference_count,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.portal_tag (name,slug,sort,status,module,reference_count,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('新手入门','beginner-guide',21,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('进阶提升','advanced-improvement',22,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('面试备战','interview-prep',23,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('读书心得','reading-notes',24,'0',NULL,1,'admin','2026-08-19 18:01:44','','2026-08-28 11:13:21','通用类','0'),
	 ('写作技巧','writing-tips',25,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('学习方法','learning-methods-tag',26,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('职场经验','career-experience',27,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('个人成长','personal-growth',28,'0',NULL,0,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44','通用类','0'),
	 ('java springboot',NULL,0,'0','interview_experience',2,'','2026-08-31 10:42:07','','2026-09-07 10:03:06',NULL,'0');
INSERT INTO `moyun-db`.portal_user (user_id,username,nickname,email,phone,password,avatar,bio,`position`,identity_tag,wechat,gender,birthday,location,website,github,company,school,`language`,timezone,notify_like,notify_comment,notify_follow,notify_system,privacy_follow,privacy_bookmark,privacy_email,privacy_phone,privacy_profile,`role`,is_certified_creator,vip_expire_at,is_phone_verified,is_wechat_verified,two_factor_enabled,status,del_flag,login_ip,login_date,create_by,create_time,update_by,update_time,remark) VALUES
	 (NULL,'zhangsan','','19987671567@163.com','','$2a$10$iOnd69MDurSwpUGCSYsIn.sn8Ki1S3xLFBWntjZCXmJSEbFsBhEFK','http://127.0.0.1:9001/moyun/2026/09/02/a65c86424ed64626a710b4ecdc638a0c.jpg','','工程师','office_worker','','','','','','','慧博云通','',NULL,NULL,1,1,1,1,1,1,0,0,1,'user',0,NULL,0,0,0,'0','0','127.0.0.1','2026-09-17 17:13:22','','2026-08-20 08:56:07','','2026-09-17 17:13:22',NULL),
	 (NULL,'libai','李白','13164798225@163.com','13164798225','$2a$10$gvpTjHffDLB1nRO6AoJ8g.ysZXm8tvn2ZaUOkJZkn7ec1M.pNZJj2','http://127.0.0.1:9001/moyun/2026/08/28/0d0edb42422c464cbfd548fcbe470efb.jpg','11111111111',NULL,NULL,NULL,'male',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,1,1,1,1,0,0,1,'user',1,NULL,0,0,0,'0','0','127.0.0.1','2026-09-04 12:50:56','','2026-08-28 10:01:26','','2026-09-04 12:50:55',NULL),
	 (1,'admin','若依','ry@163.com','15888888888',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,1,1,1,1,0,0,1,'admin',0,NULL,0,0,0,'0','0','',NULL,'','2026-08-28 11:22:25','','2026-08-28 11:22:25',NULL),
	 (NULL,'lisi',NULL,NULL,'18218361923','$2a$10$Dwc04XACTC0JjfIT1ljyCuYySRq0Sd4.f7b0UaPg4hrp89hizxgUi',NULL,NULL,'程序员','office_worker',NULL,NULL,NULL,NULL,NULL,NULL,'慧博云通',NULL,NULL,NULL,1,1,1,1,1,1,0,0,1,'user',0,NULL,0,0,0,'0','0','127.0.0.1','2026-09-09 15:31:20','','2026-09-09 14:54:37','','2026-09-09 15:31:20',NULL),
	 (NULL,'moyun_official','墨云官方',NULL,NULL,NULL,NULL,'墨云官方账号 · 每日话题由 AI 生成',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,1,0,0,0,0,'admin',1,NULL,0,0,0,'0','0','',NULL,'admin','2026-09-11 14:53:02','','2026-09-11 14:53:02','系统账号：AI 生成话题专用，SQL 20260911-04 初始化');
INSERT INTO `moyun-db`.sys_audit_task (task_type,biz_type,biz_id,title,description,submitter_id,submitter_name,status,auditor_id,auditor_name,audit_opinion,audit_action,submit_time,audit_time,priority,route_path,extra_data,create_time,update_time) VALUES
	 ('article',NULL,1,'22222222222222222222222222','222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222',6,'zhangsan','approved',1,'admin','11','approve','2026-08-28 09:58:10','2026-08-28 09:58:30','medium','/portal/audit-center',NULL,'2026-08-28 09:58:10','2026-08-28 09:58:30'),
	 ('certification',NULL,1,'创作者认证申请-李白','',7,'libai','approved',1,'admin','','approve','2026-08-28 10:03:46','2026-08-28 10:04:19','medium','/portal/audit-center',NULL,'2026-08-28 10:03:46','2026-08-28 10:04:19'),
	 ('article',NULL,13,'李白文章解析','李白（701年—762年），字太白，号青莲居士，被后世尊称为“诗仙”。他不仅是唐代伟大的浪漫主义诗人，更是一位具有宏大政治抱负和深邃思想体系的思想家。他的文章与诗歌，既是个人情感的宣泄，也是盛唐时代精神的缩影。 一、 思想内核：融汇百川的“思想家” 李白并非单纯的隐士或狂客，他构建了一个以“建功济世”为主导，以“安社稷',7,'libai','approved',1,'admin','上帝发誓是 ','approve','2026-08-28 13:12:23','2026-08-28 17:55:00','medium','/portal/audit-center',NULL,'2026-08-28 13:12:23','2026-08-28 17:55:00'),
	 ('article',NULL,7,'web 端如何实现一个语音转文字的','Web 端实现语音转文字，主要有以下几种方案，从简单到复杂依次介绍：方案一：Web Speech API（最简单，推荐先用这个）这是浏览器原生提供的能力，零依赖、无需后端，几行代码就能跑起来。完整示例代码html预览<!',NULL,NULL,'approved',1,'admin','全额付清我','approve','2026-08-28 13:12:39','2026-08-28 17:54:45','medium','/portal/audit-center',NULL,'2026-08-28 13:12:39','2026-08-28 17:54:45'),
	 ('article',NULL,16,'32242432','本文围绕标题“32242432”与正文“斯巴达”等有限信息，生成内容摘要与SEO优化信息，适合测试或占位页面使用。',6,'zhangsan','approved',1,'admin','撒旦v撒','approve','2026-08-28 17:50:27','2026-08-28 17:58:00','medium','/portal/audit-center',NULL,'2026-08-28 17:50:27','2026-08-28 17:58:00'),
	 ('interview_exp',NULL,1,'java面试','面试',6,'zhangsan','approved',1,'admin','好的','approve','2026-08-31 10:42:07','2026-08-31 10:42:47','medium','/portal/audit-center',NULL,'2026-08-31 10:42:07','2026-08-31 10:42:47'),
	 ('article',NULL,14,'22222222','22222222',NULL,NULL,'approved',1,'admin','','approve','2026-08-31 16:22:50','2026-08-31 16:23:19','medium','/portal/audit-center',NULL,'2026-08-31 16:22:50','2026-08-31 16:23:19'),
	 ('article',NULL,17,'处暑过后，傍晚的风开始凉了','写一次傍晚散步或下班路上，暑气退场，蝉声变稀，水果摊葡萄成堆。抓住一个微小变化，写出夏天悄悄退场的感觉。',6,'zhangsan','approved',1,'admin','','approve','2026-09-07 10:03:07','2026-09-07 10:05:37','medium','/portal/audit-center',NULL,'2026-09-07 10:03:07','2026-09-07 10:05:37'),
	 ('article',NULL,18,'手机端发起的第一个文章','<p>http://localhost:3000/前台首页需要做一次ui布局样式等重构，请根据原型html 格局语义修改布局，其中包括头部，中部，尾部，因为重构的原型可能缺少一些元素，在改造过程中，缺失的内容需要补上，放在合理位置，尤其是头部的 帮助中心，主题切换，搜索，消息等小的按钮，原型在moyun-project-document\\docs\\08-原型设计目录下，其中有两个文件，一个V1是用...',6,'zhangsan','approved',1,'admin','','approve','2026-09-07 10:15:28','2026-09-07 10:15:49','medium','/portal/audit-center',NULL,'2026-09-07 10:15:28','2026-09-07 10:15:49'),
	 ('topic',NULL,1,'哈哈哈，世界太变态了？','世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？世界太变态了？',6,'zhangsan','approved',1,'admin','','approve','2026-09-07 17:52:17','2026-09-07 17:52:45','medium','/portal/audit-center',NULL,'2026-09-07 17:52:17','2026-09-07 17:52:45');
INSERT INTO `moyun-db`.sys_audit_task (task_type,biz_type,biz_id,title,description,submitter_id,submitter_name,status,auditor_id,auditor_name,audit_opinion,audit_action,submit_time,audit_time,priority,route_path,extra_data,create_time,update_time) VALUES
	 ('feedback','suggestion',1,'连注册都没有','没有注册功能',6,'zhangsan','approved',1,'admin','好的，我处理','approve','2026-09-08 14:23:49','2026-09-08 14:34:40','medium','/portal/audit-center',NULL,'2026-09-08 14:23:49','2026-09-08 14:34:40'),
	 ('feedback','suggestion',2,'解决一个问题吧','大v阿萨俺的大俺的大啊',6,'zhangsan','approved',1,'admin','高的成','approve','2026-09-08 17:36:32','2026-09-15 10:33:15','medium','/portal/audit-center',NULL,'2026-09-08 17:36:32','2026-09-15 10:33:15');
INSERT INTO `moyun-db`.sys_config (config_name,config_key,config_value,platform_code,config_type,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('主框架页-默认皮肤样式名称','sys.index.skinName','skin-blue',NULL,'Y','admin','2026-08-19 18:01:44','admin',NULL,'蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow','0'),
	 ('用户管理-账号初始密码','sys.user.initPassword','123456',NULL,'Y','admin','2026-08-19 18:01:44','',NULL,'初始化密码 123456','0'),
	 ('主框架页-侧边栏主题','sys.index.sideTheme','theme-dark',NULL,'Y','admin','2026-08-19 18:01:44','',NULL,'深色主题theme-dark，浅色主题theme-light','0'),
	 ('账号自助-验证码开关','sys.account.captchaEnabled','true',NULL,'Y','admin','2026-08-19 18:01:44','',NULL,'是否开启验证码功能（true开启，false关闭）','0'),
	 ('账号自助-是否开启用户注册功能','sys.account.registerUser','false',NULL,'Y','admin','2026-08-19 18:01:44','',NULL,'是否开启注册用户功能（true开启，false关闭）','0'),
	 ('用户登录-黑名单列表','sys.login.blackIPList','',NULL,'Y','admin','2026-08-19 18:01:44','',NULL,'设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）','0'),
	 ('平台服务费率','pay.platform.fee-rate','0.10',NULL,'Y','admin','2026-08-28 09:24:22','',NULL,'V11.0 支付分账：平台抽成比例（0.10=10%），运行时生效','0'),
	 ('语音面试默认面试官AgentID','voice.interview.defaultAgentId','48',NULL,'Y','admin','2026-09-07 09:15:33','',NULL,'语音面试绑定的 ai_agent 主键；编辑「AI模块→智能体管理」对应 agent 的人设/提示词/模型即动态生效','0'),
	 ('AI网关面试主干灰度开关','ai.gateway.interview.enabled','false',NULL,'Y','admin','2026-09-16 17:01:55','',NULL,'true=语音面试主干对话前置网关治理（voice_interview 场景限流+Token熔断+执行日志），false=直连（默认，行为与历史一致）','0'),
	 ('语音面试时长（分钟）','voice.interview.durationMinutes','20',NULL,'Y','admin','2026-09-17 08:52:14','',NULL,'语音面试全场倒计时时长（分钟，范围5-120）：结束仅由用户主动（按钮/口头）或倒计时归零触发，题数仅作软参考','0');
INSERT INTO `moyun-db`.sys_config (config_name,config_key,config_value,platform_code,config_type,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('AI能力全局开关','ai.global.enabled','true',NULL,'Y','admin','2026-09-17 09:05:37','',NULL,'AI 能力运行时总开关（网关/Agent/简历/面试全链路），true=开启（默认），false=关闭走规则兜底；管理台修改即时生效','0'),
	 ('简历AI建议开关','ai.resume.advice.enabled','true',NULL,'Y','admin','2026-09-17 09:05:37','',NULL,'简历模块 AI 建议子开关（解析/岗位匹配/深度优化/AI建议），true=开启（默认），false=关闭走规则兜底；管理台修改即时生效','0'),
	 ('VIP体系开关','vip.enabled','false',NULL,'Y','admin','2026-09-17 17:53:21','',NULL,'统一VIP体系总开关：true启用校验 false全员放行（灰度上线用）；端级可覆盖（platform_code=portal/ledger）','0');
INSERT INTO `moyun-db`.sys_dept (parent_id,ancestors,dept_name,order_num,leader,phone,email,status,del_flag,create_by,create_time,update_by,update_time,remark) VALUES
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
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (2,'首页-VIP推广位','home_vip_banner','portal_ad_slot_key','','primary','N','0','admin','2026-08-28 13:16:38','',NULL,'首页右侧/移动端下方的VIP推广位','0');
INSERT INTO `moyun-db`.sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_dict_type (dict_name,dict_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('简历到岗时间','portal_available_time','0','admin','2026-08-25 00:00:00','',NULL,'v10.10 简历求职意向-到岗时间（值为中文文本，直接入库）','0'),
	 ('记账-身份标签','ledger_identity_tag','0','admin','2026-09-04 11:03:26','',NULL,'AI 财务分析用户画像身份标签','0');
INSERT INTO `moyun-db`.sys_file (file_name,file_ext,file_type,file_size,file_url,file_path,storage_type,bucket_name,object_name,fallback,local_path,file_md5,upload_user_id,upload_user_name,status,business_type,business_id,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('可爱.jpg','jpg','image',36835,'http://127.0.0.1:9001/moyun/2026/08/28/0d0edb42422c464cbfd548fcbe470efb.jpg','http://127.0.0.1:9001/moyun/2026/08/28/0d0edb42422c464cbfd548fcbe470efb.jpg','minio','moyun','2026/08/28/0d0edb42422c464cbfd548fcbe470efb.jpg',0,NULL,'b39afe6c9cadbe4708327429b847b1f9',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('可爱.jpg','jpg','image',36835,'http://127.0.0.1:9001/moyun/2026/08/28/7eaf5ae9c5034a819ba7e86dd32d4061.jpg','http://127.0.0.1:9001/moyun/2026/08/28/7eaf5ae9c5034a819ba7e86dd32d4061.jpg','minio','moyun','2026/08/28/7eaf5ae9c5034a819ba7e86dd32d4061.jpg',0,NULL,'b39afe6c9cadbe4708327429b847b1f9',7,'libai','0','creator_certification',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('熊大.png','png','image',27324,'http://127.0.0.1:9001/moyun/2026/08/28/10ada7c421034447a681e632f2f164a1.png','http://127.0.0.1:9001/moyun/2026/08/28/10ada7c421034447a681e632f2f164a1.png','minio','moyun','2026/08/28/10ada7c421034447a681e632f2f164a1.png',0,NULL,'b78ca092240c0631d496f75cf362d268',7,'libai','0','creator_certification',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('可爱.jpg','jpg','image',36835,'http://127.0.0.1:9001/moyun/2026/08/28/08c050c645394a3d98e19b681e18da59.jpg','http://127.0.0.1:9001/moyun/2026/08/28/08c050c645394a3d98e19b681e18da59.jpg','minio','moyun','2026/08/28/08c050c645394a3d98e19b681e18da59.jpg',0,NULL,'b39afe6c9cadbe4708327429b847b1f9',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'1'),
	 ('城市.png','png','image',60522,'http://127.0.0.1:9001/moyun/2026/08/28/9c9f3bb92f9a4c66810a04618a158597.png','http://127.0.0.1:9001/moyun/2026/08/28/9c9f3bb92f9a4c66810a04618a158597.png','minio','moyun','2026/08/28/9c9f3bb92f9a4c66810a04618a158597.png',0,NULL,'8f6f7563ab5a7fe5719cb875798b09d7',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('脱毛.png','png','image',20066,'http://127.0.0.1:9001/moyun/2026/08/28/3c8e81614b5b4e3391a82f5d24c49f65.png','http://127.0.0.1:9001/moyun/2026/08/28/3c8e81614b5b4e3391a82f5d24c49f65.png','minio','moyun','2026/08/28/3c8e81614b5b4e3391a82f5d24c49f65.png',0,NULL,'6ba26bf100d7c93743da9eab10a77d6d',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('轮播图1-正常.png','png','image',165435,'http://127.0.0.1:9001/moyun/2026/08/28/df35cc1281ec40dba5f3edbce706a3df.png','http://127.0.0.1:9001/moyun/2026/08/28/df35cc1281ec40dba5f3edbce706a3df.png','minio','moyun','2026/08/28/df35cc1281ec40dba5f3edbce706a3df.png',0,NULL,'557db10957d1faa37326be900eb6ba90',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('tutu.png','png','image',94236,'http://127.0.0.1:9001/moyun/2026/08/28/58f3433eea964d85a54954e19cea8187.png','http://127.0.0.1:9001/moyun/2026/08/28/58f3433eea964d85a54954e19cea8187.png','minio','moyun','2026/08/28/58f3433eea964d85a54954e19cea8187.png',0,NULL,'2216d21e52571c2e52b778d7fd55c23e',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('熊大.png','png','image',27324,'http://127.0.0.1:9001/moyun/2026/08/28/64959c2a7418433fbf6a940ee2b2d4c7.png','http://127.0.0.1:9001/moyun/2026/08/28/64959c2a7418433fbf6a940ee2b2d4c7.png','minio','moyun','2026/08/28/64959c2a7418433fbf6a940ee2b2d4c7.png',0,NULL,'b78ca092240c0631d496f75cf362d268',6,'zhangsan','0','article_cover',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('可爱.jpg','jpg','image',36835,'http://127.0.0.1:9001/moyun/2026/08/28/db4fb76bcdf94586b8cc73a03880254c.jpg','http://127.0.0.1:9001/moyun/2026/08/28/db4fb76bcdf94586b8cc73a03880254c.jpg','minio','moyun','2026/08/28/db4fb76bcdf94586b8cc73a03880254c.jpg',0,NULL,'b39afe6c9cadbe4708327429b847b1f9',6,'zhangsan','0','article_content',NULL,'',NULL,'',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_file (file_name,file_ext,file_type,file_size,file_url,file_path,storage_type,bucket_name,object_name,fallback,local_path,file_md5,upload_user_id,upload_user_name,status,business_type,business_id,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('tutu.png','png','image',94236,'http://127.0.0.1:9001/moyun/2026/08/28/5b6a82da1d7b443cb6e36cc66fc90b36.png','http://127.0.0.1:9001/moyun/2026/08/28/5b6a82da1d7b443cb6e36cc66fc90b36.png','minio','moyun','2026/08/28/5b6a82da1d7b443cb6e36cc66fc90b36.png',0,NULL,'2216d21e52571c2e52b778d7fd55c23e',6,'zhangsan','0','article_content',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('简历封面-01.png','png','image',335995,'http://127.0.0.1:9001/moyun/2026/08/31/d88a580a882f460e9f6f2e762097ae3f.png','http://127.0.0.1:9001/moyun/2026/08/31/d88a580a882f460e9f6f2e762097ae3f.png','minio','moyun','2026/08/31/d88a580a882f460e9f6f2e762097ae3f.png',0,NULL,'43c611a0b83b26b4f5d8248ad65763e2',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('简历封面-01.png','png','image',335995,'http://127.0.0.1:9001/moyun/2026/08/31/de169a8619444c9380cae4a20f5f9c50.png','http://127.0.0.1:9001/moyun/2026/08/31/de169a8619444c9380cae4a20f5f9c50.png','minio','moyun','2026/08/31/de169a8619444c9380cae4a20f5f9c50.png',0,NULL,'43c611a0b83b26b4f5d8248ad65763e2',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/08/31/df797bbb6ca044c1a06359cc08fe0e25.pdf','http://127.0.0.1:9001/moyun/2026/08/31/df797bbb6ca044c1a06359cc08fe0e25.pdf','minio','moyun','2026/08/31/df797bbb6ca044c1a06359cc08fe0e25.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',1,'admin','0','common',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/08/31/4c0903c5f4ad44858b0018c374b0aac0.pdf','http://127.0.0.1:9001/moyun/2026/08/31/4c0903c5f4ad44858b0018c374b0aac0.pdf','minio','moyun','2026/08/31/4c0903c5f4ad44858b0018c374b0aac0.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0',NULL,NULL,'',NULL,'',NULL,NULL,'0'),
	 ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/08/31/f495d202049b4b558c843c662a4f0f0b.pdf','http://127.0.0.1:9001/moyun/2026/08/31/f495d202049b4b558c843c662a4f0f0b.pdf','minio','moyun','2026/08/31/f495d202049b4b558c843c662a4f0f0b.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0',NULL,NULL,'',NULL,'',NULL,NULL,'0'),
	 ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/01/28c7643599bd45eebccf0763f74c69d9.pdf','http://127.0.0.1:9001/moyun/2026/09/01/28c7643599bd45eebccf0763f74c69d9.pdf','minio','moyun','2026/09/01/28c7643599bd45eebccf0763f74c69d9.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0',NULL,NULL,'',NULL,'',NULL,NULL,'0'),
	 ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/01/aa1e3ae4b59f47d8baba55d49f49630c.pdf','http://127.0.0.1:9001/moyun/2026/09/01/aa1e3ae4b59f47d8baba55d49f49630c.pdf','minio','moyun','2026/09/01/aa1e3ae4b59f47d8baba55d49f49630c.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0',NULL,NULL,'',NULL,'',NULL,NULL,'0'),
	 ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/01/420a1affcc1848b0bb95d9105f918cd0.pdf','http://127.0.0.1:9001/moyun/2026/09/01/420a1affcc1848b0bb95d9105f918cd0.pdf','minio','moyun','2026/09/01/420a1affcc1848b0bb95d9105f918cd0.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0','resume_attachment',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/01/f362589317454f638cc5e36100323981.pdf','http://127.0.0.1:9001/moyun/2026/09/01/f362589317454f638cc5e36100323981.pdf','minio','moyun','2026/09/01/f362589317454f638cc5e36100323981.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0','resume_attachment',NULL,'',NULL,'',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_file (file_name,file_ext,file_type,file_size,file_url,file_path,storage_type,bucket_name,object_name,fallback,local_path,file_md5,upload_user_id,upload_user_name,status,business_type,business_id,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/01/62b2e18f18354643bbbe1430dc284f80.pdf','http://127.0.0.1:9001/moyun/2026/09/01/62b2e18f18354643bbbe1430dc284f80.pdf','minio','moyun','2026/09/01/62b2e18f18354643bbbe1430dc284f80.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0','resume_attachment',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/02/f5830e65f81146ab8e4cbc09ea7c16f5.pdf','http://127.0.0.1:9001/moyun/2026/09/02/f5830e65f81146ab8e4cbc09ea7c16f5.pdf','minio','moyun','2026/09/02/f5830e65f81146ab8e4cbc09ea7c16f5.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0','resume_attachment',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('半身像1.jpg','jpg','image',614342,'http://127.0.0.1:9001/moyun/2026/09/02/a65c86424ed64626a710b4ecdc638a0c.jpg','http://127.0.0.1:9001/moyun/2026/09/02/a65c86424ed64626a710b4ecdc638a0c.jpg','minio','moyun','2026/09/02/a65c86424ed64626a710b4ecdc638a0c.jpg',0,NULL,'b7b2a77ef8c3afa447e7c8c4287bfc98',6,'zhangsan','0','avatar','6','',NULL,'',NULL,NULL,'0'),
	 ('20260903栏目设置.jpeg','jpeg','image',334381,'http://127.0.0.1:9001/moyun/2026/09/07/37619d86ff7b474e9aacd4f737c84d9c.jpeg','http://127.0.0.1:9001/moyun/2026/09/07/37619d86ff7b474e9aacd4f737c84d9c.jpeg','minio','moyun','2026/09/07/37619d86ff7b474e9aacd4f737c84d9c.jpeg',0,NULL,'fe13d3b3d051b703c4c13e20d9087045',6,'zhangsan','0','article_cover',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('钟永国-boss在线简历-Java.pdf','pdf','document',228301,'http://127.0.0.1:9001/moyun/2026/09/07/6e7b6794cfae4fdcab20c6a39bf72ed1.pdf','http://127.0.0.1:9001/moyun/2026/09/07/6e7b6794cfae4fdcab20c6a39bf72ed1.pdf','minio','moyun','2026/09/07/6e7b6794cfae4fdcab20c6a39bf72ed1.pdf',0,NULL,'4fa1428e2e85f4b577edbb089b5667b8',6,'zhangsan','0','resume_attachment',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('弄谷村暮色.jpg','jpg','image',1688202,'http://127.0.0.1:9001/moyun/2026/09/07/59caaf2c79f94d9c804ddb6618cbfe7e.jpg','http://127.0.0.1:9001/moyun/2026/09/07/59caaf2c79f94d9c804ddb6618cbfe7e.jpg','minio','moyun','2026/09/07/59caaf2c79f94d9c804ddb6618cbfe7e.jpg',0,NULL,'b00b64d415166363ad23057f534d40e6',6,'zhangsan','0','topic_cover',NULL,'',NULL,'',NULL,NULL,'0'),
	 ('熊大.png','png','image',27324,'http://127.0.0.1:9001/moyun/2026/09/16/0a0924494cb6492b8836577427976d71.png','http://127.0.0.1:9001/moyun/2026/09/16/0a0924494cb6492b8836577427976d71.png','minio','moyun','2026/09/16/0a0924494cb6492b8836577427976d71.png',0,NULL,'b78ca092240c0631d496f75cf362d268',6,'zhangsan','0',NULL,NULL,'',NULL,'',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_job (job_name,job_group,invoke_target,cron_expression,misfire_policy,concurrent,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
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
INSERT INTO `moyun-db`.sys_job (job_name,job_group,invoke_target,cron_expression,misfire_policy,concurrent,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('敏感词扫描-面经评论','DEFAULT','sensitiveScanTask.scanInterviewComments()','0 15 3 * * ?','3','1','0','admin','2026-08-19 18:01:45','',NULL,'扫描已发布面经评论(portal_interview_comment.status=published)，命中敏感词转rejected并通知作者','0'),
	 ('AI每日写作Prompt生成','DEFAULT','writingPromptTask.generateDailyPrompt()','0 10 0 * * ?','3','1','0','admin','2026-08-25 00:00:00','',NULL,'每天00:10为今天+明天生成写作提示（结合节日节气，AI失败回退内置主题池）','0');
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-28 09:33:49','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-08-28 09:59:09','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-08-28 09:59:25','',NULL,'',NULL,NULL),
	 ('libai','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 10:02:39','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-08-28 10:56:52','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-28 10:57:45','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-08-28 11:42:08','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-28 13:09:07','',NULL,'',NULL,NULL),
	 ('libai','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 13:10:44','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 13:49:11','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 14:01:15','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 14:37:17','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-28 16:45:49','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-31 09:14:39','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-31 09:15:21','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-31 10:08:45','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-31 13:04:01','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-31 13:47:01','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-31 15:15:13','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-08-31 15:15:39','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-08-31 16:19:30','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 08:54:22','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-01 09:27:19','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 09:46:42','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 09:47:08','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 10:37:43','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-01 10:50:36','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-01 13:12:06','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 13:13:50','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 14:30:41','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-01 15:23:14','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 15:49:00','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-01 16:27:19','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-02 10:16:12','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-02 13:17:49','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-02 13:21:06','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-03 11:27:04','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 13:28:16','',NULL,'',NULL,NULL),
	 ('test01','127.0.0.1','内网IP','Chrome 14','Windows 10','1','portal','用户名或密码错误','2026-09-03 13:44:01','',NULL,'',NULL,NULL),
	 ('test01','127.0.0.1','内网IP','Chrome 14','Windows 10','1','portal','用户名或密码错误','2026-09-03 13:44:25','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('test01','127.0.0.1','内网IP','Chrome 14','Windows 10','1','portal','用户名或密码错误','2026-09-03 13:44:42','',NULL,'',NULL,NULL),
	 ('test01','127.0.0.1','内网IP','Chrome 14','Windows 10','1','portal','用户名或密码错误','2026-09-03 13:46:05','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 14:29:53','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-03 15:06:53','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-03 15:09:49','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 15:10:24','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 15:38:59','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-03 15:48:12','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-03 15:49:39','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 16:15:41','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 17:05:24','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-03 17:08:48','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-03 17:25:21','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-03 17:25:34','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-03 17:25:47','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-04 09:05:48','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-04 09:05:48','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-04 09:45:07','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-04 09:57:12','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:04:42','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','1','portal','用户名或密码错误','2026-09-04 11:29:08','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','1','portal','用户名或密码错误','2026-09-04 11:29:19','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','1','portal','用户名或密码错误','2026-09-04 11:29:19','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','1','portal','用户名或密码错误','2026-09-04 11:29:19','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','1','portal','用户名或密码错误','2026-09-04 11:29:19','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:32:38','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:32:52','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:33:02','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:40:23','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:40:32','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:40:39','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:40:48','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:40:54','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:41:28','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Mozilla','Windows 10','0','portal','门户登录成功','2026-09-04 11:42:06','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 12:45:11','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 12:45:21','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 12:45:30','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 12:49:21','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 12:49:56','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('libai','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 12:50:55','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','1','portal','用户名或密码错误','2026-09-04 14:34:30','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 14:35:50','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 14:38:25','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 14','Windows 10','0','portal','门户登录成功','2026-09-04 14:39:14','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-04 16:29:54','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 16:47:48','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 17:19:38','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-04 17:35:58','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-07 09:17:49','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-07 09:52:18','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 09:53:49','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-07 11:18:32','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 11:18:59','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 11:27:30','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 11:29:05','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 11:30:41','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 11:40:00','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-07 14:33:49','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 15:35:00','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 16:15:23','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-07 17:17:17','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-07 17:38:22','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-08 09:06:13','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-08 10:10:32','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-08 11:40:48','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-08 11:42:12','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-08 13:08:08','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-08 14:16:06','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-08 14:24:08','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-08 14:24:25','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-08 17:08:25','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-09 11:32:15','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-09 11:37:39','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-09 13:56:00','',NULL,'',NULL,NULL),
	 ('lisi','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-09 15:31:20','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-09 17:54:05','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-09 17:56:03','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-10 09:02:17','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-10 10:19:44','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-10 10:48:57','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-10 13:06:49','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-10 13:11:58','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-10 15:25:59','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-10 17:00:57','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome Mobile','Android 1.x','0','portal','门户登录成功','2026-09-10 18:12:18','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-11 08:56:43','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-11 09:22:48','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-11 13:32:21','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-11 16:23:28','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('admin','127.0.0.1','内网IP','Chrome 14','Windows 10','0','sys','登录成功','2026-09-11 18:17:45','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-14 09:04:44','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','1','sys','验证码已失效','2026-09-14 11:17:43','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-14 11:17:52','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-14 13:07:03','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','1','sys','验证码错误','2026-09-14 13:09:34','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','1','sys','验证码错误','2026-09-14 13:09:42','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-14 13:09:51','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-14 17:28:37','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-14 18:25:25','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-15 10:28:04','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-15 10:36:27','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-15 10:43:00','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-15 14:00:38','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-15 14:25:45','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-15 16:04:51','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-16 09:40:58','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','1','sys','验证码错误','2026-09-16 10:17:25','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','1','sys','验证码错误','2026-09-16 10:17:30','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-16 10:17:37','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_logininfor (user_name,ipaddr,login_location,browser,os,status,user_type,msg,login_time,create_by,create_time,update_by,update_time,remark) VALUES
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-16 10:42:27','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-16 13:33:33','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-16 14:04:55','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-16 16:38:45','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-17 09:07:55','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','1','sys','验证码已失效','2026-09-17 09:10:13','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-17 09:10:20','',NULL,'',NULL,NULL),
	 ('admin','127.0.0.1','内网IP','Chrome 15','Windows 10','0','sys','登录成功','2026-09-17 13:39:07','',NULL,'',NULL,NULL),
	 ('zhangsan','127.0.0.1','内网IP','Chrome 15','Windows 10','0','portal','门户登录成功','2026-09-17 17:13:21','',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('系统设置',0,7,'system',NULL,'','',1,0,'M','0','0','','system','admin','2026-08-19 18:01:45','',NULL,'系统管理目录','0'),
	 ('系统监控',1,5,'monitor',NULL,'','',1,0,'M','0','0','','monitor','admin','2026-08-19 18:01:45','',NULL,'系统监控目录','0'),
	 ('系统工具',1,6,'tool',NULL,'','',1,0,'M','0','0','','tool','admin','2026-08-19 18:01:45','',NULL,'系统工具目录','0'),
	 ('用户管理',5243,1,'user','system/user/index','','',1,0,'C','0','0','system:user:list','user','admin','2026-08-19 18:01:45','',NULL,'用户管理菜单','0'),
	 ('角色管理',5243,2,'role','system/role/index','','',1,0,'C','0','0','system:role:list','peoples','admin','2026-08-19 18:01:45','',NULL,'角色管理菜单','0'),
	 ('菜单管理',5243,3,'menu','system/menu/index','','',1,0,'C','0','0','system:menu:list','tree-table','admin','2026-08-19 18:01:45','',NULL,'菜单管理菜单','0'),
	 ('部门管理',5243,4,'dept','system/dept/index','','',1,0,'C','0','0','system:dept:list','tree','admin','2026-08-19 18:01:45','',NULL,'部门管理菜单','0'),
	 ('岗位管理',5243,5,'post','system/post/index','','',1,0,'C','0','0','system:post:list','post','admin','2026-08-19 18:01:45','',NULL,'岗位管理菜单','0'),
	 ('字典管理',5244,1,'dict','system/dict/index','','',1,0,'C','0','0','system:dict:list','dict','admin','2026-08-19 18:01:45','',NULL,'字典管理菜单','0'),
	 ('参数设置',5244,2,'config','system/config/index','','',1,0,'C','0','0','system:config:list','edit','admin','2026-08-19 18:01:45','',NULL,'参数设置菜单','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('日志管理',1,3,'log','','','',1,0,'M','0','0','','log','admin','2026-08-19 18:01:45','',NULL,'日志管理菜单','0'),
	 ('在线用户',2,1,'online','monitor/online/index','','',1,0,'C','0','0','monitor:online:list','online','admin','2026-08-19 18:01:45','',NULL,'在线用户菜单','0'),
	 ('定时任务',2,2,'job','monitor/job/index','','',1,0,'C','0','0','monitor:job:list','job','admin','2026-08-19 18:01:45','',NULL,'定时任务菜单','0'),
	 ('数据监控',2,3,'druid','monitor/druid/index','','',1,0,'C','0','0','monitor:druid:list','druid','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','数据监控菜单','0'),
	 ('服务监控',2,4,'server','monitor/server/index','','',1,0,'C','0','0','monitor:server:list','server','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','服务监控菜单','0'),
	 ('缓存监控',2,5,'cache','monitor/cache/index','','',1,0,'C','0','0','monitor:cache:list','redis','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','缓存监控菜单','0'),
	 ('缓存列表',2,6,'cacheList','monitor/cache/list','','',1,0,'C','0','0','monitor:cache:list','redis-list','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','缓存列表菜单','0'),
	 ('代码生成',3,2,'gen','tool/gen/index','','',1,0,'C','0','0','tool:gen:list','code','admin','2026-08-19 18:01:45','',NULL,'代码生成菜单','0'),
	 ('接口文档',3,3,'swagger','tool/swagger/index','','',1,0,'C','0','0','tool:swagger:list','swagger','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','系统接口菜单','0'),
	 ('操作日志',108,1,'operlog','monitor/operlog/index','','',1,0,'C','0','0','monitor:operlog:list','form','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','操作日志菜单','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('登录日志',108,2,'logininfor','monitor/logininfor/index','','',1,0,'C','0','0','monitor:logininfor:list','logininfor','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','登录日志菜单','0'),
	 ('用户查询',100,1,'','','','',1,0,'F','0','0','system:user:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('用户新增',100,2,'','','','',1,0,'F','0','0','system:user:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('用户修改',100,3,'','','','',1,0,'F','0','0','system:user:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('用户删除',100,4,'','','','',1,0,'F','0','0','system:user:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('用户导出',100,5,'','','','',1,0,'F','0','0','system:user:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('用户导入',100,6,'','','','',1,0,'F','0','0','system:user:import','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('重置密码',100,7,'','','','',1,0,'F','0','0','system:user:resetPwd','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('角色查询',101,1,'','','','',1,0,'F','0','0','system:role:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('角色新增',101,2,'','','','',1,0,'F','0','0','system:role:add','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('角色修改',101,3,'','','','',1,0,'F','0','0','system:role:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('角色删除',101,4,'','','','',1,0,'F','0','0','system:role:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('角色导出',101,5,'','','','',1,0,'F','0','0','system:role:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('菜单查询',102,1,'','','','',1,0,'F','0','0','system:menu:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('菜单新增',102,2,'','','','',1,0,'F','0','0','system:menu:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('菜单修改',102,3,'','','','',1,0,'F','0','0','system:menu:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('菜单删除',102,4,'','','','',1,0,'F','0','0','system:menu:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('部门查询',103,1,'','','','',1,0,'F','0','0','system:dept:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('部门新增',103,2,'','','','',1,0,'F','0','0','system:dept:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('部门修改',103,3,'','','','',1,0,'F','0','0','system:dept:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('部门删除',103,4,'','','','',1,0,'F','0','0','system:dept:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('岗位查询',104,1,'','','','',1,0,'F','0','0','system:post:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('岗位新增',104,2,'','','','',1,0,'F','0','0','system:post:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('岗位修改',104,3,'','','','',1,0,'F','0','0','system:post:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('岗位删除',104,4,'','','','',1,0,'F','0','0','system:post:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('岗位导出',104,5,'','','','',1,0,'F','0','0','system:post:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('字典查询',105,1,'#','','','',1,0,'F','0','0','system:dict:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('字典新增',105,2,'#','','','',1,0,'F','0','0','system:dict:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('字典修改',105,3,'#','','','',1,0,'F','0','0','system:dict:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('字典删除',105,4,'#','','','',1,0,'F','0','0','system:dict:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('字典导出',105,5,'#','','','',1,0,'F','0','0','system:dict:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('参数查询',106,1,'#','','','',1,0,'F','0','0','system:config:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('参数新增',106,2,'#','','','',1,0,'F','0','0','system:config:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('参数修改',106,3,'#','','','',1,0,'F','0','0','system:config:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('参数删除',106,4,'#','','','',1,0,'F','0','0','system:config:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('参数导出',106,5,'#','','','',1,0,'F','0','0','system:config:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('操作查询',500,1,'#','','','',1,0,'F','0','0','monitor:operlog:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('操作删除',500,2,'#','','','',1,0,'F','0','0','monitor:operlog:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('日志导出',500,3,'#','','','',1,0,'F','0','0','monitor:operlog:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('登录查询',501,1,'#','','','',1,0,'F','0','0','monitor:logininfor:query','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('登录删除',501,2,'#','','','',1,0,'F','0','0','monitor:logininfor:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('日志导出',501,3,'#','','','',1,0,'F','0','0','monitor:logininfor:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('账号解锁',501,4,'#','','','',1,0,'F','0','0','monitor:logininfor:unlock','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('在线查询',109,1,'#','','','',1,0,'F','0','0','monitor:online:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('批量强退',109,2,'#','','','',1,0,'F','0','0','monitor:online:batchLogout','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('单条强退',109,3,'#','','','',1,0,'F','0','0','monitor:online:forceLogout','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('任务查询',110,1,'#','','','',1,0,'F','0','0','monitor:job:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('任务新增',110,2,'#','','','',1,0,'F','0','0','monitor:job:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('任务修改',110,3,'#','','','',1,0,'F','0','0','monitor:job:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('任务删除',110,4,'#','','','',1,0,'F','0','0','monitor:job:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('状态修改',110,5,'#','','','',1,0,'F','0','0','monitor:job:changeStatus','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('任务导出',110,6,'#','','','',1,0,'F','0','0','monitor:job:export','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('生成查询',116,1,'#','','','',1,0,'F','0','0','tool:gen:query','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
	 ('生成修改',116,2,'#','','','',1,0,'F','0','0','tool:gen:edit','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
	 ('生成删除',116,3,'#','','','',1,0,'F','0','0','tool:gen:remove','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
	 ('导入代码',116,4,'#','','','',1,0,'F','0','0','tool:gen:import','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
	 ('预览代码',116,5,'#','','','',1,0,'F','0','0','tool:gen:preview','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
	 ('生成代码',116,6,'#','','','',1,0,'F','0','0','tool:gen:code','#','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:46','','0'),
	 ('AI智能中心',0,6,'ai',NULL,NULL,'',1,0,'M','0','0','','chart','admin','2026-08-19 18:01:40','admin','2026-09-15 10:39:25','智能AI目录','0'),
	 ('智能体管理',5000,1,'agent','ai/agent/index',NULL,'',1,0,'C','0','0','cms:ai:agent:list','edit','admin','2026-08-19 18:01:40','',NULL,'智能体管理菜单','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('智能体查询',5001,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:agent:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('智能体新增',5001,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:agent:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('智能体修改',5001,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:agent:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('智能体删除',5001,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:agent:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('智能体测试',5001,5,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:agent:test','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('知识库管理',5237,1,'knowledge-base','ai/knowledge-base/index',NULL,'',1,0,'C','0','0','cms:ai:knowledge-base:list','documentation','admin','2026-08-19 18:01:40','',NULL,'知识库管理菜单 [v7.16 已整合到知识中心 Tab 容器]','0'),
	 ('知识库查询',5007,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-base:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('知识库新增',5007,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-base:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('知识库修改',5007,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-base:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('知识库删除',5007,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-base:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('文档上传',5007,5,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-base:upload','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('知识文库',5237,2,'knowledge-library','ai/knowledge-library/index',NULL,'',1,0,'C','0','0','cms:ai:knowledge-library:list','tree-table','admin','2026-08-19 18:01:40','',NULL,'知识文库菜单 [v7.16 已整合到知识中心 Tab 容器]','0'),
	 ('文库查询',5013,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-library:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('文库新增',5013,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-library:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('文库修改',5013,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-library:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('文库删除',5013,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:knowledge-library:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('模型配置',5238,1,'model-config','ai/model-config/index',NULL,'',1,0,'C','0','0','cms:ai:model-config:list','monitor','admin','2026-08-19 18:01:40','',NULL,'模型配置菜单','0'),
	 ('模型查询',5018,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:model-config:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('模型新增',5018,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:model-config:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('模型修改',5018,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:model-config:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('模型删除',5018,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:model-config:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('连接测试',5018,5,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:model-config:test','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('工具管理',5238,2,'tool','ai/tool/index',NULL,'',1,0,'C','0','0','cms:ai:tool:list','tool','admin','2026-08-19 18:01:40','',NULL,'工具管理菜单','0'),
	 ('工具查询',5024,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:tool:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('工具新增',5024,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:tool:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('工具修改',5024,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:tool:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('工具删除',5024,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:tool:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('工作流管理',5000,3,'workflow','ai/workflow/index',NULL,'',1,0,'C','0','0','cms:ai:workflow:list','chart','admin','2026-08-19 18:01:40','',NULL,'工作流管理菜单','0'),
	 ('工作流查询',5029,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('工作流新增',5029,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow:add','#','admin','2026-08-19 18:01:40','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('工作流修改',5029,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('工作流删除',5029,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('工作流执行',5029,5,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow:execute','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('AI生成工作流',5029,6,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:workflow-generator:generate','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('领域词典',5000,5,'dictionary','ai/dictionary/index',NULL,'',1,0,'C','0','0','cms:ai:domain-dictionary:list','dict','admin','2026-08-19 18:01:40','',NULL,'领域词典菜单','0'),
	 ('词典查询',5036,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:domain-dictionary:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('词典新增',5036,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:domain-dictionary:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('词典修改',5036,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:domain-dictionary:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('词典删除',5036,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:domain-dictionary:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('数据源管理',5238,3,'datasource','ai/datasource/index',NULL,'',1,0,'C','0','0','cms:ai:datasource:list','druid','admin','2026-08-19 18:01:40','',NULL,'数据源管理菜单','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('数据源查询',5041,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('数据源新增',5041,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:add','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('数据源修改',5041,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:edit','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('数据源删除',5041,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:remove','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('连接测试',5041,5,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:test','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('元数据同步',5041,6,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:datasource:sync','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('Token统计',5239,2,'token-usage','ai/token-usage/index',NULL,'',1,0,'C','0','0','cms:ai:token-usage:list','money','admin','2026-08-19 18:01:40','',NULL,'Token统计菜单','0'),
	 ('统计查询',5048,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:token-usage:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('统计导出',5048,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:token-usage:export','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('AI数据分析',5000,6,'query','ai/query/index',NULL,'',1,0,'C','0','0','cms:ai:data-analysis:list','icon','admin','2026-08-19 18:01:40','',NULL,'智能数据分析菜单','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('查询查询',5051,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:data-analysis:query','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('SQL生成',5051,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:data-analysis:sql','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('报告生成',5051,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:data-analysis:report','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('图表生成',5051,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:diagram:generate','#','admin','2026-08-19 18:01:40','',NULL,'','0'),
	 ('概览大屏',5239,1,'ai-dashboard','ai/dashboard/index',NULL,'',1,0,'C','0','0','cms:ai:dashboard:list','chart','admin','2026-08-19 18:01:40','',NULL,'AI数据大屏菜单','0'),
	 ('架构图生成',5000,7,'diagram/chat','ai/diagram/chat',NULL,'',1,0,'C','0','0','cms:ai:diagram:list','build','admin','2026-08-19 18:01:40','',NULL,'AI架构图生成菜单','0'),
	 ('智能对话使用',5001,10,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:chat:list','#','admin','2026-08-19 18:01:40','',NULL,'AI对话/中断/重新生成接口权限','0'),
	 ('话题管理',5068,14,'topic','cms/topic/index',NULL,'',1,0,'C','0','0','cms:topic:list','message','admin','2026-08-19 18:01:41','admin','2026-08-20 13:39:28','话题列表与状态管理','0'),
	 ('话题查询',5059,1,'#','',NULL,'',1,0,'F','0','0','cms:topic:query','#','admin','2026-08-19 18:01:41','',NULL,NULL,'0'),
	 ('敏感词管理',5244,3,'sensitiveWord','system/sensitiveWord/index',NULL,'',1,0,'C','0','0','system:sensitiveWord:list','dict','admin','2026-08-19 18:01:41','admin','2026-08-20 13:34:56','敏感词库维护与词树刷新','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('敏感词查询',5063,1,'#','',NULL,'',1,0,'F','0','0','system:sensitiveWord:query','#','admin','2026-08-19 18:01:41','',NULL,NULL,'0'),
	 ('敏感词新增',5063,2,'#','',NULL,'',1,0,'F','0','0','system:sensitiveWord:add','#','admin','2026-08-19 18:01:41','',NULL,NULL,'0'),
	 ('敏感词修改',5063,3,'#','',NULL,'',1,0,'F','0','0','system:sensitiveWord:edit','#','admin','2026-08-19 18:01:41','',NULL,NULL,'0'),
	 ('敏感词删除',5063,4,'#','',NULL,'',1,0,'F','0','0','system:sensitiveWord:remove','#','admin','2026-08-19 18:01:41','',NULL,NULL,'0'),
	 ('内容管理',5241,1,'cms',NULL,NULL,'',1,0,'M','0','0',NULL,'documentation','admin','2026-08-19 18:01:45','',NULL,'内容管理目录','0'),
	 ('门户用户',5068,1,'portal-user','cms/user/index',NULL,'',1,0,'C','0','0','cms:user:list','user','admin','2026-08-19 18:01:45','',NULL,'门户用户管理菜单','0'),
	 ('用户查询',5069,1,'',NULL,NULL,'',1,0,'F','0','0','cms:user:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('用户新增',5069,2,'',NULL,NULL,'',1,0,'F','0','0','cms:user:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('用户修改',5069,3,'',NULL,NULL,'',1,0,'F','0','0','cms:user:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('用户删除',5069,4,'',NULL,NULL,'',1,0,'F','0','0','cms:user:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('用户状态',5069,5,'',NULL,NULL,'',1,0,'F','0','0','cms:user:status','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('重置密码',5069,6,'',NULL,NULL,'',1,0,'F','0','0','cms:user:resetPwd','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('绑定系统用户',5069,7,'',NULL,NULL,'',1,0,'F','0','0','cms:user:bind','#','admin','2026-08-19 18:01:45','',NULL,'身份桥接：绑定/解绑后台系统用户','0'),
	 ('文章管理',5068,2,'article','cms/article/index',NULL,'',1,0,'C','0','0','cms:article:list','edit','admin','2026-08-19 18:01:45','',NULL,'文章管理菜单','0'),
	 ('文章查询',5077,1,'',NULL,NULL,'',1,0,'F','0','0','cms:article:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('文章新增',5077,2,'',NULL,NULL,'',1,0,'F','0','0','cms:article:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('文章修改',5077,3,'',NULL,NULL,'',1,0,'F','0','0','cms:article:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('文章删除',5077,4,'',NULL,NULL,'',1,0,'F','0','0','cms:article:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('文章审核',5077,5,'',NULL,NULL,'',1,0,'F','0','0','cms:article:audit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('文章上架',5077,6,'',NULL,NULL,'',1,0,'F','0','0','cms:article:publish','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('文章推荐',5077,7,'',NULL,NULL,'',1,0,'F','0','0','cms:article:featured','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('分类管理',5068,3,'/category','cms/category/index',NULL,'',1,0,'C','0','0','cms:category:list','tree','admin','2026-08-19 18:01:45','',NULL,'分类管理菜单','0'),
	 ('分类查询',5085,1,'',NULL,NULL,'',1,0,'F','0','0','cms:category:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('分类新增',5085,2,'',NULL,NULL,'',1,0,'F','0','0','cms:category:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('分类修改',5085,3,'',NULL,NULL,'',1,0,'F','0','0','cms:category:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('分类删除',5085,4,'',NULL,NULL,'',1,0,'F','0','0','cms:category:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('标签管理',5068,4,'tag','cms/tag/index',NULL,'',1,0,'C','0','0','cms:tag:list','tab','admin','2026-08-19 18:01:45','',NULL,'标签管理菜单','0'),
	 ('标签查询',5090,1,'',NULL,NULL,'',1,0,'F','0','0','cms:tag:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('标签新增',5090,2,'',NULL,NULL,'',1,0,'F','0','0','cms:tag:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('标签修改',5090,3,'',NULL,NULL,'',1,0,'F','0','0','cms:tag:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('标签删除',5090,4,'',NULL,NULL,'',1,0,'F','0','0','cms:tag:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('评论管理',5068,5,'comment','cms/comment/index',NULL,'',1,0,'C','0','0','cms:comment:list','message','admin','2026-08-19 18:01:45','',NULL,'评论管理菜单','0'),
	 ('评论查询',5095,1,'',NULL,NULL,'',1,0,'F','0','0','cms:comment:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('评论审核',5095,2,'',NULL,NULL,'',1,0,'F','0','0','cms:comment:audit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('评论删除',5095,3,'',NULL,NULL,'',1,0,'F','0','0','cms:comment:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('友情链接',5068,7,'friend-link','cms/friend-link/index',NULL,'',1,0,'C','0','0','cms:friend-link:list','link','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','友情链接管理菜单','0'),
	 ('友情链接查询',5099,1,'',NULL,NULL,'',1,0,'F','0','0','cms:friend-link:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('友情链接新增',5099,2,'',NULL,NULL,'',1,0,'F','0','0','cms:friend-link:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('友情链接修改',5099,3,'',NULL,NULL,'',1,0,'F','0','0','cms:friend-link:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('友情链接删除',5099,4,'',NULL,NULL,'',1,0,'F','0','0','cms:friend-link:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('帮助分类',5068,8,'help-category','cms/help-category/index',NULL,'',1,0,'C','1','0','cms:help-category:list','tree','admin','2026-08-19 18:01:45','admin','2026-08-25 00:00:00','已合并至帮助中心(5146)，菜单隐藏保留路由','0'),
	 ('分类查询',5104,1,'',NULL,NULL,'',1,0,'F','0','0','cms:help-category:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('分类新增',5104,2,'',NULL,NULL,'',1,0,'F','0','0','cms:help-category:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('分类修改',5104,3,'',NULL,NULL,'',1,0,'F','0','0','cms:help-category:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('分类删除',5104,4,'',NULL,NULL,'',1,0,'F','0','0','cms:help-category:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('帮助文章',5068,9,'help-article','cms/help-article/index',NULL,'',1,0,'C','1','0','cms:help-article:list','documentation','admin','2026-08-19 18:01:45','admin','2026-08-25 00:00:00','已合并至帮助中心(5146)，菜单隐藏保留路由','0'),
	 ('文章查询',5109,1,'',NULL,NULL,'',1,0,'F','0','0','cms:help-article:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('文章新增',5109,2,'',NULL,NULL,'',1,0,'F','0','0','cms:help-article:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('文章修改',5109,3,'',NULL,NULL,'',1,0,'F','0','0','cms:help-article:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('文章删除',5109,4,'',NULL,NULL,'',1,0,'F','0','0','cms:help-article:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('举报管理',5068,10,'report','cms/report/index',NULL,'',1,0,'C','0','0','cms:report:list','warning','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','用户举报记录管理','0'),
	 ('举报查询',5114,1,'',NULL,NULL,'',1,0,'F','0','0','cms:report:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('处理举报',5114,2,'',NULL,NULL,'',1,0,'F','0','0','cms:report:handle','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('删除举报',5114,3,'',NULL,NULL,'',1,0,'F','0','0','cms:report:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('反馈管理',5068,11,'feedback','cms/feedback/index',NULL,'',1,0,'C','0','0','cms:feedback:list','message','admin','2026-08-19 18:01:45','admin','2026-08-19 18:01:45','用户意见反馈管理','0'),
	 ('反馈查询',5118,1,'',NULL,NULL,'',1,0,'F','0','0','cms:feedback:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('处理反馈',5118,2,'',NULL,NULL,'',1,0,'F','0','0','cms:feedback:handle','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('删除反馈',5118,3,'',NULL,NULL,'',1,0,'F','0','0','cms:feedback:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('专栏管理',5068,12,'column','cms/column/index',NULL,'',1,0,'C','1','0','portal:column:list','documentation','admin','2026-08-19 18:01:45','admin','2026-08-31 15:03:16','专栏后台管理菜单 [v8.1 隐藏：合并到文章管理 Tab，路由保留供 Tab 组件复用]','0'),
	 ('专栏查询',5122,1,'',NULL,NULL,'',1,0,'F','0','0','portal:column:query','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('专栏新增',5122,2,'',NULL,NULL,'',1,0,'F','0','0','portal:column:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('专栏修改',5122,3,'',NULL,NULL,'',1,0,'F','0','0','portal:column:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('专栏删除',5122,4,'',NULL,NULL,'',1,0,'F','0','0','portal:column:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('创作者认证',5241,2,'certification',NULL,NULL,'',1,0,'M','1','0',NULL,'user','admin','2026-08-19 18:01:45','',NULL,'创作者认证目录','0'),
	 ('认证审核',5127,1,'audit','cms/certification/index',NULL,'',1,0,'C','1','0','cms:certification:audit','edit','admin','2026-08-19 18:01:45','',NULL,'创作者认证审核菜单','0'),
	 ('认证查询',5128,1,'',NULL,NULL,'',1,0,'F','1','0','cms:certification:list','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('认证审核',5128,2,'',NULL,NULL,'',1,0,'F','1','0','cms:certification:audit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('消息中心',5245,1,'message','system/message/index',NULL,'',1,0,'C','0','0','system:message:list','message','admin','2026-08-19 18:01:45','',NULL,'消息中心菜单（私信+通知双Tab）','0'),
	 ('私信查询',5134,1,'',NULL,NULL,'',1,0,'F','0','0','system:message:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('私信发送',5134,2,'',NULL,NULL,'',1,0,'F','0','0','system:message:send','#','admin','2026-08-19 18:01:45','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('通知查询',5134,3,'',NULL,NULL,'',1,0,'F','0','0','system:notification:list','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('通知管理',5245,2,'notification','system/notification/index',NULL,'',1,0,'C','0','0','system:notification:list','email','admin','2026-08-19 18:01:45','',NULL,'通知管理菜单（台账）','0'),
	 ('通知查询',5138,1,'',NULL,NULL,'',1,0,'F','0','0','system:notification:query','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('通知新增',5138,2,'',NULL,NULL,'',1,0,'F','0','0','system:notification:add','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('通知修改',5138,3,'',NULL,NULL,'',1,0,'F','0','0','system:notification:edit','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('通知删除',5138,4,'',NULL,NULL,'',1,0,'F','0','0','system:notification:remove','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('发送广播通知',5138,5,'',NULL,NULL,'',1,0,'F','0','0','system:notification:sendAll','#','admin','2026-08-19 18:01:45','',NULL,'','0'),
	 ('推广位管理',5068,20,'promotion','cms/promotion/index',NULL,'',1,0,'C','0','0','cms:promotion:list','component','admin','2026-08-19 18:01:45','',NULL,'广告位与友情链接合并管理（Tab）','0'),
	 ('用户反馈处理',5068,21,'feedback-center','cms/feedback-center/index',NULL,'',1,0,'C','0','0','cms:feedback-center:list','message','admin','2026-08-19 18:01:45','',NULL,'反馈与举报合并处理（Tab）','0'),
	 ('帮助中心',5068,22,'help-center','cms/help-center/index',NULL,'',1,0,'C','0','0','cms:help-center:list','question','admin','2026-08-19 18:01:45','',NULL,'帮助分类与文章合并管理（Tab）','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('成长配置',5068,23,'growth-config','cms/growth-config/index',NULL,'',1,0,'C','0','0','cms:growth-config:list','star','admin','2026-08-19 18:01:45','',NULL,'成长规则与成就合并配置（Tab）','0'),
	 ('缓存管理',2,7,'cache-manage','monitor/cache-manage/index',NULL,'',1,0,'C','0','0','monitor:cache-manage:list','redis','admin','2026-08-19 18:01:45','',NULL,'缓存监控与列表合并管理（Tab）','0'),
	 ('日志审计',108,3,'log-audit','monitor/log-audit/index',NULL,'',1,0,'C','0','0','monitor:log-audit:list','log','admin','2026-08-19 18:01:45','',NULL,'操作日志与登录日志合并查询（Tab）','0'),
	 ('内容审核中心',5068,25,'audit-center','cms/audit-center/index',NULL,'',1,0,'C','1','0','cms:audit-center:list','check','admin','2026-08-19 18:01:45','',NULL,'文章/专栏/话题审核合并入口（Tab，嵌入模式）','0'),
	 ('服务监控',2,3,'server-panel','monitor/server-panel/index',NULL,'',1,0,'C','0','0','monitor:server-panel:list','monitor','admin','2026-08-19 18:01:45','',NULL,'服务器监控与数据监控(Druid)合并查看（Tab）','0'),
	 ('面试管理',5241,4,'interview','',NULL,'',1,0,'M','0','0',NULL,'guide','admin','2026-08-19 18:01:46','',NULL,'面试指南一级目录：题库/面经/简历/笔记 | V10.5: 降级为门户管理下二级目录，重命名为 面试管理','0'),
	 ('题库资源',5192,1,'questionTab','cms/interview/questionTab/index',NULL,'',1,0,'C','0','0','cms:interview:list','tree-table','admin','2026-08-19 18:01:46','',NULL,'面试题库+分类+公司标签+简历模板 Tab 容器','0'),
	 ('面经运营',5192,2,'experienceTab','cms/interview/experienceTab/index',NULL,'',1,0,'C','0','0','cms:interview:experience:list','edit','admin','2026-08-19 18:01:46','',NULL,'面经管理+评论管理 Tab 容器；审核入口在内容审核中心','0'),
	 ('精选笔记',5192,3,'submission','cms/interview/submission/index',NULL,'',1,0,'C','0','0','cms:interview:submission:list','star','admin','2026-08-19 18:01:46','',NULL,'精选笔记采纳与取消','0'),
	 ('题库查询',5193,1,'#','',NULL,'',1,0,'F','0','0','cms:interview:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('题库新增',5193,2,'#','',NULL,'',1,0,'F','0','0','cms:interview:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('题库修改',5193,3,'#','',NULL,'',1,0,'F','0','0','cms:interview:edit','#','admin','2026-08-19 18:01:46','',NULL,'含：审核/置顶/精选采纳等运营操作','0'),
	 ('题库删除',5193,4,'#','',NULL,'',1,0,'F','0','0','cms:interview:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('面经查询',5194,1,'#','',NULL,'',1,0,'F','0','0','cms:interview:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('面经修改',5194,2,'#','',NULL,'',1,0,'F','0','0','cms:interview:edit','#','admin','2026-08-19 18:01:46','',NULL,'含：审核/置顶/评论管理','0'),
	 ('面经删除',5194,3,'#','',NULL,'',1,0,'F','0','0','cms:interview:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('笔记查询',5195,1,'#','',NULL,'',1,0,'F','0','0','cms:interview:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('笔记修改',5195,2,'#','',NULL,'',1,0,'F','0','0','cms:interview:edit','#','admin','2026-08-19 18:01:46','',NULL,'含：采纳/取消精选','0'),
	 ('学习管理',5241,5,'book','',NULL,'',1,0,'M','0','0',NULL,'education','admin','2026-08-19 18:01:46','',NULL,'读书空间一级目录：书籍/书单/金句/学习 | V10.5: 降级为门户管理下二级目录，重命名为 学习管理','0'),
	 ('书籍管理',5205,1,'book-index','portal/book/index',NULL,'',1,0,'C','0','0','portal:book:list','documentation','admin','2026-08-19 18:01:46','',NULL,'书籍CRUD + 章节导入向导（章节管理为隐藏子路由）','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('章节管理',5205,2,'bookChapter','portal/bookChapter/index',NULL,'',1,0,'C','0','0','portal:bookChapter:list','#','admin','2026-08-19 18:01:46','',NULL,'书籍章节CRUD + 发布/批量导入（隐藏菜单，从书籍详情跳转）','0'),
	 ('书单&推荐位',5205,3,'bookListTab','portal/bookListTab/index',NULL,'',1,0,'C','0','0','portal:bookList:list','list','admin','2026-08-19 18:01:46','',NULL,'书单管理+推荐位管理 Tab 容器','0'),
	 ('用户内容',5205,4,'userContent','portal/userContent/index',NULL,'',1,0,'C','0','0','portal:bookQuote:list','peoples','admin','2026-08-19 18:01:46','',NULL,'金句摘录+书架管理 Tab 容器','0'),
	 ('学习辅助',5205,9,'learn-aux','portal/learn-aux/index',NULL,'',1,0,'C','0','0','portal:learn:list','skill','admin','2026-08-19 18:01:46','admin','2026-08-19 18:01:46','v9.5: 迁移至面试指南（错题本/学习计划数据源于题库刷题）','0'),
	 ('书籍查询',5206,1,'#','',NULL,'',1,0,'F','0','0','portal:book:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('书籍新增',5206,2,'#','',NULL,'',1,0,'F','0','0','portal:book:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('书籍修改',5206,3,'#','',NULL,'',1,0,'F','0','0','portal:book:edit','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('书籍删除',5206,4,'#','',NULL,'',1,0,'F','0','0','portal:book:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('章节查询',5207,1,'#','',NULL,'',1,0,'F','0','0','portal:bookChapter:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('章节新增',5207,2,'#','',NULL,'',1,0,'F','0','0','portal:bookChapter:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('章节修改',5207,3,'#','',NULL,'',1,0,'F','0','0','portal:bookChapter:edit','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('章节删除',5207,4,'#','',NULL,'',1,0,'F','0','0','portal:bookChapter:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('章节发布',5207,5,'#','',NULL,'',1,0,'F','0','0','portal:bookChapter:publish','#','admin','2026-08-19 18:01:46','',NULL,'章节发布/撤回','0'),
	 ('书单查询',5208,1,'#','',NULL,'',1,0,'F','0','0','portal:bookList:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('书单新增',5208,2,'#','',NULL,'',1,0,'F','0','0','portal:bookList:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('书单修改',5208,3,'#','',NULL,'',1,0,'F','0','0','portal:bookList:edit','#','admin','2026-08-19 18:01:46','',NULL,'含：管理书籍（增删排序）','0'),
	 ('书单删除',5208,4,'#','',NULL,'',1,0,'F','0','0','portal:bookList:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('推荐位列表',5208,5,'#','',NULL,'',1,0,'F','0','0','portal:bookRecommend:list','#','admin','2026-08-19 18:01:46','',NULL,'Tab 内推荐位面板列表权限','0'),
	 ('推荐位查询',5208,6,'#','',NULL,'',1,0,'F','0','0','portal:bookRecommend:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('推荐位新增',5208,7,'#','',NULL,'',1,0,'F','0','0','portal:bookRecommend:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('推荐位修改',5208,8,'#','',NULL,'',1,0,'F','0','0','portal:bookRecommend:edit','#','admin','2026-08-19 18:01:46','',NULL,'含：上下架/排序','0'),
	 ('推荐位删除',5208,9,'#','',NULL,'',1,0,'F','0','0','portal:bookRecommend:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('金句查询',5209,1,'#','',NULL,'',1,0,'F','0','0','portal:bookQuote:query','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('金句新增',5209,2,'#','',NULL,'',1,0,'F','0','0','portal:bookQuote:add','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('金句修改',5209,3,'#','',NULL,'',1,0,'F','0','0','portal:bookQuote:edit','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('金句删除',5209,4,'#','',NULL,'',1,0,'F','0','0','portal:bookQuote:remove','#','admin','2026-08-19 18:01:46','',NULL,NULL,'0'),
	 ('书架列表',5209,5,'#','',NULL,'',1,0,'F','0','0','portal:bookshelf:list','#','admin','2026-08-19 18:01:46','',NULL,'Tab 内书架面板列表权限','0'),
	 ('书架移除',5209,6,'#','',NULL,'',1,0,'F','0','0','portal:bookshelf:remove','#','admin','2026-08-19 18:01:46','',NULL,'移出书架','0'),
	 ('学习计划查询',5210,1,'#','',NULL,'',1,0,'F','0','0','portal:studyPlan:list','#','admin','2026-08-19 18:01:46','',NULL,'学习计划只读列表','0'),
	 ('错题本查询',5210,2,'#','',NULL,'',1,0,'F','0','0','portal:wrongQuestion:list','#','admin','2026-08-19 18:01:46','',NULL,'错题本只读列表','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('知识中心',5000,2,'knowledge-center','ai/knowledge-center/index',NULL,'',1,0,'M','0','0','','documentation','admin','2026-08-19 18:01:46','',NULL,'知识中心目录（知识库管理+知识文库 Tab）','0'),
	 ('AI基础配置',5000,4,'ai-config',NULL,NULL,'',1,0,'M','0','0','','system','admin','2026-08-19 18:01:46','',NULL,'AI基础配置目录（模型配置+工具管理+数据源管理）','0'),
	 ('运营监控',5000,9,'ai-monitor',NULL,NULL,'',1,0,'M','0','0','','monitor','admin','2026-08-19 18:01:46','',NULL,'运营监控目录（概览大屏+Token统计）','0'),
	 ('门户管理',0,2,'portal',NULL,NULL,'',1,0,'M','0','0','','job','admin','2026-08-19 18:01:46','admin','2026-08-20 14:04:35','V10.5: 门户管理一级目录（聚合内容/用户/审核/面试/学习）','0'),
	 ('审核中心',5241,3,'audit-center','cms/audit-center/index',NULL,'',1,0,'C','0','0','system:auditTask:list','eye-open','admin','2026-08-19 18:01:46','admin','2026-08-20 14:05:25','V10.5: 审核中心（统一审核入口，Tab 容器，权限 system:auditTask:*）','0'),
	 ('基础管理',1,1,'base',NULL,NULL,'',1,0,'M','0','0','','tree','admin','2026-08-19 18:01:46','admin','2026-08-21 00:00:00','V10.6: 系统设置-基础管理（用户/角色/菜单/部门/岗位）','0'),
	 ('配置管理',1,2,'/systemConfig',NULL,NULL,'',1,0,'M','0','0','','edit','admin','2026-08-19 18:01:46','admin','2026-09-07 11:37:15','V10.6: 系统设置-配置管理（字典/参数/敏感词）','0'),
	 ('消息通知',1,4,'notice',NULL,NULL,'',1,0,'M','0','0','','message','admin','2026-08-19 18:01:46','admin','2026-08-21 00:00:00','V10.6: 系统设置-消息通知（消息中心/通知管理）','0'),
	 ('会话列表',5001,11,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:conversation:list','#','admin','2026-08-24 00:00:00','',NULL,'AI会话列表接口权限','0'),
	 ('会话查询',5001,12,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:conversation:query','#','admin','2026-08-24 00:00:00','',NULL,'AI会话历史消息接口权限','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('会话新增',5001,13,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:conversation:add','#','admin','2026-08-24 00:00:00','',NULL,'AI会话创建接口权限','0'),
	 ('会话修改',5001,14,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:conversation:edit','#','admin','2026-08-24 00:00:00','',NULL,'AI会话标题修改接口权限','0'),
	 ('会话删除',5001,15,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:conversation:remove','#','admin','2026-08-24 00:00:00','',NULL,'AI会话删除接口权限','0'),
	 ('语音面试',5192,4,'voiceInterview','cms/voiceInterview/index',NULL,'',1,0,'C','0','0','cms:voiceInterview:list','microphone','admin','2026-08-31 09:12:21','',NULL,'AI语音面试会话管理：列表/详情/评分报告查看','0'),
	 ('语音面试查询',5253,1,'',NULL,NULL,'',1,0,'F','0','0','cms:voiceInterview:query','#','admin','2026-08-31 09:12:21','',NULL,'','0'),
	 ('语音面试删除',5253,2,'',NULL,NULL,'',1,0,'F','0','0','cms:voiceInterview:remove','#','admin','2026-08-31 09:12:21','',NULL,'','0'),
	 ('语音面试导出',5253,3,'',NULL,NULL,'',1,0,'F','0','0','cms:voiceInterview:export','#','admin','2026-08-31 09:12:21','',NULL,'','0'),
	 ('收入管理',0,7,'pay',NULL,NULL,'',1,0,'M','0','0','','money','admin','2026-08-28 09:24:22','',NULL,'v11.79 全平台支付汇集：公账+单钱包+提现闭环','0'),
	 ('支付订单',5300,5,'order','cms/pay/order/index',NULL,'',1,0,'C','0','0','cms:payOrder:list','list','admin','2026-08-28 09:24:22','',NULL,'支付单查询/详情/关单','0'),
	 ('用户银行卡',5300,6,'bankcard','cms/pay/bankcard/index',NULL,'',1,0,'C','0','0','cms:payBankCard:list','card','admin','2026-08-28 09:24:22','',NULL,'脱敏审计视角','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('支付配置',5300,7,'pay-config','cms/pay/config/index',NULL,'',1,0,'C','0','0','cms:payConfig:view','edit','admin','2026-08-28 09:24:23','',NULL,'通道状态/费率在线调整','0'),
	 ('收入总览',5300,1,'revenue','cms/pay/revenue/index',NULL,'',1,0,'C','0','0','cms:payRevenue:view','chart','admin','2026-09-14 14:04:32','',NULL,'v11.78 全平台收入汇集：平台×渠道两级总览','0'),
	 ('收入订单',5300,2,'income-order','cms/pay/income-order/index',NULL,'',1,0,'C','0','0','cms:payIncomeOrder:list','shopping','admin','2026-09-14 17:26:30','',NULL,'v11.79 全平台业务订单统一视图（平台/渠道/状态筛选）','0'),
	 ('用户钱包',5300,4,'wallet','cms/pay/wallet/index',NULL,'',1,0,'C','0','0','cms:payWallet:list','peoples','admin','2026-09-14 17:26:30','',NULL,'v11.79 单钱包：余额列表 + 资金流水 + 守恒对账','0'),
	 ('提现审核',5300,3,'withdraw','cms/pay/withdraw/index',NULL,'',1,0,'C','0','0','cms:payWithdraw:list','validCode','admin','2026-09-14 17:26:30','',NULL,'v11.79 提现闭环：auditing→paid/rejected','0'),
	 ('支付订单查询',5301,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payOrder:query','#','admin','2026-08-28 09:24:22','',NULL,'','0'),
	 ('支付订单关单',5301,2,'',NULL,NULL,'',1,0,'F','0','0','cms:payOrder:close','#','admin','2026-08-28 09:24:22','',NULL,'','0'),
	 ('银行卡详情',5303,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payBankCard:query','#','admin','2026-08-28 09:24:23','',NULL,'','0'),
	 ('费率调整',5304,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payConfig:edit','#','admin','2026-08-28 09:24:23','',NULL,'','0'),
	 ('收入总览查询',5305,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payRevenue:view','#','admin','2026-09-14 14:04:32','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('收入订单查询',5306,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payIncomeOrder:list','#','admin','2026-09-14 17:26:30','',NULL,'','0'),
	 ('钱包查询',5307,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payWallet:list','#','admin','2026-09-14 17:26:30','',NULL,'','0'),
	 ('提现单列表',5308,1,'',NULL,NULL,'',1,0,'F','0','0','cms:payWithdraw:list','#','admin','2026-09-14 17:26:30','',NULL,'','0'),
	 ('提现审核操作',5308,2,'',NULL,NULL,'',1,0,'F','0','0','cms:payWithdraw:audit','#','admin','2026-09-14 17:26:30','',NULL,'','0'),
	 ('记账管理',0,8,'ledger-app',NULL,NULL,'',1,0,'M','0','0','','monitor','admin','2026-09-03 10:57:30','admin','2026-09-14 17:38:27','记账模块（个人资产管理）','0'),
	 ('预设分类',5400,1,'category','cms/ledger/category/index',NULL,'',1,0,'C','0','0','cms:ledgerCategory:list','tree','admin','2026-09-03 10:57:30','',NULL,'系统预设收支分类维护','0'),
	 ('运营统计',5400,2,'stats','cms/ledger/stats/index',NULL,'',1,0,'C','0','0','cms:ledgerStats:list','chart','admin','2026-09-03 10:57:31','',NULL,'记账用户/活跃度/类型分布（脱敏聚合）','0'),
	 ('用户管理',5400,3,'users','cms/ledger/users/index',NULL,'',1,0,'C','0','0','cms:ledgerUsers:list','peoples','admin','2026-09-14 13:08:02','',NULL,'记账用户维度：流水/AI使用/token消费（脱敏）','0'),
	 ('功能配置',5400,4,'app-feature','cms/ledger/appFeature/index',NULL,'',1,0,'C','0','0','cms:ledgerAppFeature:list','component','admin','2026-09-14 13:08:02','',NULL,'小程序"我的"页功能入口可视化配置','0'),
	 ('分类查询',5401,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ledgerCategory:query','#','admin','2026-09-03 10:57:30','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('分类新增',5401,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ledgerCategory:add','#','admin','2026-09-03 10:57:30','',NULL,'','0'),
	 ('分类修改',5401,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ledgerCategory:edit','#','admin','2026-09-03 10:57:30','',NULL,'','0'),
	 ('提供商管理',5238,4,'provider','ai/provider/index',NULL,'',1,0,'C','0','0','cms:ai:model-config:list','server','admin','2026-09-07 09:15:27','',NULL,'AI提供商注册表（V11.0.2 配置驱动）','0'),
	 ('场景配置',5238,10,'scene','ai/scene/index',NULL,'',1,0,'C','0','0','cms:ai:scene:list','component','admin','2026-09-07 15:07:32','',NULL,'AI场景配置中心：业务场景与Agent/模型/知识库/工作流动态绑定（版本+灰度权重）','0'),
	 ('场景查询',5450,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:scene:query','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
	 ('场景新增',5450,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:scene:create','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
	 ('场景修改',5450,3,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:scene:update','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
	 ('场景删除',5450,4,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:scene:remove','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
	 ('岗位模板',5192,5,'jobTemplate','cms/interview/jobTemplate/index',NULL,'',1,0,'C','0','0','cms:interview:jobTemplate:list','dict','admin','2026-09-07 15:07:32','',NULL,'岗位模板管理：JD/关键词（LLM提取）/出题权重/关联题目','0'),
	 ('岗位模板查询',5455,1,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:jobTemplate:query','#','admin','2026-09-07 15:07:32','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('岗位模板新增',5455,2,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:jobTemplate:create','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
	 ('岗位模板修改',5455,3,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:jobTemplate:update','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
	 ('岗位模板删除',5455,4,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:jobTemplate:remove','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
	 ('面试配置',5192,6,'interviewConfig','cms/interview/interviewConfig/index',NULL,'',1,0,'C','0','0','cms:interview:config:list','edit','admin','2026-09-07 15:07:32','',NULL,'面试配置管理：人设/提示词模板/评分权重/追问策略/自我介绍环节','0'),
	 ('面试配置查询',5460,1,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:config:query','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
	 ('面试配置新增',5460,2,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:config:create','#','admin','2026-09-07 15:07:32','',NULL,'','0'),
	 ('面试配置修改',5460,3,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:config:update','#','admin','2026-09-07 15:07:33','',NULL,'','0'),
	 ('面试配置删除',5460,4,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:config:remove','#','admin','2026-09-07 15:07:33','',NULL,'','0'),
	 ('内容安全检测',5238,11,'safety','ai/safety/index',NULL,'',1,0,'C','0','0','cms:ai:safety:detect','shield','admin','2026-09-11 14:53:02','',NULL,'LLM 级文本敏感内容复核工具（经统一网关 sensitive_word 场景，限流/成本熔断自动生效）','0'),
	 ('文本检测',5470,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:safety:detect','#','admin','2026-09-11 14:53:02','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('AI执行日志',5238,12,'execute-log','ai/execute-log/index',NULL,'',1,0,'C','0','0','cms:ai:execute-log:list','log','admin','2026-09-11 14:53:08','',NULL,'统一网关全量调用日志：场景/模型/Token/成本/耗时筛选与详情（v11.60 P1-1 可观测性）','0'),
	 ('日志查询',5472,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:execute-log:query','#','admin','2026-09-11 14:53:08','',NULL,'','0'),
	 ('日志删除',5472,2,'',NULL,NULL,'',1,0,'F','0','0','cms:ai:execute-log:remove','#','admin','2026-09-11 14:53:08','',NULL,'过期数据清理（物理删除，日志只增不改）','0'),
	 ('文件管理',5243,6,'file','system/file/index',NULL,'',1,0,'C','0','0','system:file:list','upload','admin','2026-09-11 17:45:42','',NULL,'系统文件管理：MinIO/本地存储的文件列表、上传、预览、下载、删除与存储模式切换','0'),
	 ('文件查询',5475,1,'',NULL,NULL,'',1,0,'F','0','0','system:file:query','#','admin','2026-09-11 17:45:42','',NULL,'文件详情查看（GET /system/file/{id}）','0'),
	 ('文件上传',5475,2,'',NULL,NULL,'',1,0,'F','0','0','system:file:add','#','admin','2026-09-11 17:45:42','',NULL,'文件上传与存储模式切换（POST /system/file/upload、PUT /storage/switch）','0'),
	 ('文件删除',5475,3,'',NULL,NULL,'',1,0,'F','0','0','system:file:remove','#','admin','2026-09-11 17:45:42','',NULL,'单删/批删/按URL删（删除同时清理存储与记录，不可恢复）','0'),
	 ('端管理',1,8,'platform','system/platform/index',NULL,'',1,0,'C','0','0','system:platform:list','tree','admin','2026-09-17 17:53:22','',NULL,'全局端定义管理（门户/记账/管理/人格分析），用户/支付/VIP/配置/统计统一引用','0'),
	 ('VIP管理',1,9,'vip',NULL,NULL,'',1,0,'M','0','0','','crown','admin','2026-09-17 17:53:22','',NULL,'统一VIP体系管理目录','0'),
	 ('等级管理',5501,1,'tier','system/vip/tier/index',NULL,'',1,0,'C','0','0','system:vip:tier:list','peoples','admin','2026-09-17 17:53:22','',NULL,'VIP等级（一端一套，价格/时长/上下架）','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('权益管理',5501,2,'benefit','system/vip/benefit/index',NULL,'',1,0,'C','0','0','system:vip:benefit:list','button','admin','2026-09-17 17:53:22','',NULL,'VIP权益定义','0'),
	 ('等级权益配置',5501,3,'tierBenefit','system/vip/tierBenefit/index',NULL,'',1,0,'C','0','0','system:vip:tierBenefit:list','checkbox','admin','2026-09-17 17:53:22','',NULL,'等级×权益额度矩阵（free 计数替代免费体验）','0'),
	 ('接口注册管理',5501,4,'registry','system/vip/registry/index',NULL,'',1,0,'C','0','0','system:vip:registry:list','monitor','admin','2026-09-17 17:53:22','',NULL,'@VipOnly 接口注册表（启动扫描生成，可禁用/重扫）','0'),
	 ('用户会员卡',5501,5,'card','system/vip/card/index',NULL,'',1,0,'C','0','0','system:vip:card:list','idcard','admin','2026-09-17 17:53:22','',NULL,'用户会员卡查询/管理','0'),
	 ('权益使用统计',5501,6,'usage','system/vip/usage/index',NULL,'',1,0,'C','0','0','system:vip:usage:list','chart','admin','2026-09-17 17:53:22','',NULL,'权益使用记录统计','0'),
	 ('等级新增',5502,1,'',NULL,NULL,'',1,0,'F','0','0','system:vip:tier:add','#','admin','2026-09-17 17:53:22','',NULL,'','0'),
	 ('等级修改',5502,2,'',NULL,NULL,'',1,0,'F','0','0','system:vip:tier:edit','#','admin','2026-09-17 17:53:22','',NULL,'','0'),
	 ('等级删除',5502,3,'',NULL,NULL,'',1,0,'F','0','0','system:vip:tier:remove','#','admin','2026-09-17 17:53:22','',NULL,'','0'),
	 ('权益新增',5503,1,'',NULL,NULL,'',1,0,'F','0','0','system:vip:benefit:add','#','admin','2026-09-17 17:53:22','',NULL,'','0'),
	 ('权益修改',5503,2,'',NULL,NULL,'',1,0,'F','0','0','system:vip:benefit:edit','#','admin','2026-09-17 17:53:22','',NULL,'','0');
INSERT INTO `moyun-db`.sys_menu (menu_name,parent_id,order_num,`path`,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('权益删除',5503,3,'',NULL,NULL,'',1,0,'F','0','0','system:vip:benefit:remove','#','admin','2026-09-17 17:53:22','',NULL,'','0'),
	 ('权益配置保存',5504,1,'',NULL,NULL,'',1,0,'F','0','0','system:vip:tierBenefit:edit','#','admin','2026-09-17 17:53:22','',NULL,'','0'),
	 ('接口校验启停',5505,1,'',NULL,NULL,'',1,0,'F','0','0','system:vip:registry:edit','#','admin','2026-09-17 17:53:22','',NULL,'','0'),
	 ('接口重新扫描',5505,2,'',NULL,NULL,'',1,0,'F','0','0','system:vip:registry:scan','#','admin','2026-09-17 17:53:22','',NULL,'','0'),
	 ('会员卡作废',5506,1,'',NULL,NULL,'',1,0,'F','0','0','system:vip:card:remove','#','admin','2026-09-17 17:53:22','',NULL,'','0'),
	 ('用户查询',5403,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ledgerUsers:query','#','admin','2026-09-14 13:08:02','',NULL,'','0'),
	 ('配置修改',5404,1,'',NULL,NULL,'',1,0,'F','0','0','cms:ledgerAppFeature:edit','#','admin','2026-09-14 13:08:02','',NULL,'','0');
INSERT INTO `moyun-db`.sys_notification (`type`,title,content,`data`,`scope`,user_id,user_type,notice_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('todo','新文章待审核：22222222222222222222222222','作者 zhangsan 提交了文章《22222222222222222222222222》，请尽快审核','{"id": 1, "bizType": "article"}','user',1,'sys','1','1','','2026-08-28 09:58:10','','2026-08-28 09:58:30',NULL,'0'),
	 ('todo','新文章待审核：22222222222222222222222222','作者 zhangsan 提交了文章《22222222222222222222222222》，请尽快审核','{"id": 1, "bizType": "article"}','user',2,'sys','1','1','','2026-08-28 09:58:10','','2026-08-28 09:58:30',NULL,'0'),
	 ('system','文章审核通过：22222222222222222222222222','您的文章《22222222222222222222222222》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 1, "status": "published", "bizType": "article"}','user',6,'portal','1','0','','2026-08-28 09:58:30','','2026-08-28 09:58:30',NULL,'0'),
	 ('todo','新创作者认证申请待审核','用户 李白（userId=7）提交了创作者认证申请，请尽快审核','{"id": 1, "bizType": "creator_certification"}','user',1,'sys','1','1','','2026-08-28 10:03:46','','2026-08-28 10:04:18',NULL,'0'),
	 ('todo','新创作者认证申请待审核','用户 李白（userId=7）提交了创作者认证申请，请尽快审核','{"id": 1, "bizType": "creator_certification"}','user',2,'sys','1','1','','2026-08-28 10:03:46','','2026-08-28 10:04:18',NULL,'0'),
	 ('system','创作者认证已通过','恭喜您，您的创作者认证申请已通过审核，现已获得创作者标识。','{"id": 1, "status": "approved", "bizType": "creator_certification"}','user',7,'portal','1','0','','2026-08-28 10:04:19','','2026-08-28 10:04:19',NULL,'0'),
	 ('todo','新文章待审核：李白文章解析','作者 libai 提交了文章《李白文章解析》，请尽快审核','{"id": 13, "bizType": "article"}','user',1,'sys','1','1','','2026-08-28 13:12:23','','2026-08-28 17:55:00',NULL,'0'),
	 ('todo','新文章待审核：李白文章解析','作者 libai 提交了文章《李白文章解析》，请尽快审核','{"id": 13, "bizType": "article"}','user',2,'sys','1','1','','2026-08-28 13:12:23','','2026-08-28 17:55:00',NULL,'0'),
	 ('todo','新文章待审核：李白文章解析','作者 libai 提交了文章《李白文章解析》，请尽快审核','{"id": 13, "bizType": "article"}','user',8,'portal','1','1','','2026-08-28 13:12:23','','2026-08-28 17:55:00',NULL,'0'),
	 ('todo','新文章待审核：web 端如何实现一个语音转文字的','作者 用户#null 提交了文章《web 端如何实现一个语音转文字的》，请尽快审核','{"id": 7, "bizType": "article"}','user',1,'sys','1','1','','2026-08-28 13:12:39','','2026-08-28 17:54:44',NULL,'0');
INSERT INTO `moyun-db`.sys_notification (`type`,title,content,`data`,`scope`,user_id,user_type,notice_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('todo','新文章待审核：web 端如何实现一个语音转文字的','作者 用户#null 提交了文章《web 端如何实现一个语音转文字的》，请尽快审核','{"id": 7, "bizType": "article"}','user',2,'sys','1','1','','2026-08-28 13:12:39','','2026-08-28 17:54:44',NULL,'0'),
	 ('todo','新文章待审核：web 端如何实现一个语音转文字的','作者 用户#null 提交了文章《web 端如何实现一个语音转文字的》，请尽快审核','{"id": 7, "bizType": "article"}','user',8,'portal','1','1','','2026-08-28 13:12:39','','2026-08-28 17:54:44',NULL,'0'),
	 ('bookmark','zhangsan 收藏了你的文章','zhangsan 收藏了你的文章《视频渲染服务器配置评估》','{"bizType": "bookmark", "articleId": 2, "fromUserId": 6}','user',7,'portal','1','0','','2026-08-28 13:53:47','','2026-08-28 13:53:46',NULL,'0'),
	 ('like','zhangsan 赞了你的文章','zhangsan 赞了你的文章《视频渲染服务器配置评估》','{"bizType": "like", "articleId": 2, "fromUserId": 6}','user',7,'portal','1','0','','2026-08-28 13:53:48','','2026-08-28 13:53:47',NULL,'0'),
	 ('comment','zhangsan 评论了你的文章','zhangsan 评论了你的文章《本企业是否属小型企业或微型企业： 是和否的区别》','{"bizType": "comment", "articleId": 9, "fromUserId": 6}','user',7,'portal','1','0','','2026-08-28 16:02:10','','2026-08-28 16:02:09',NULL,'0'),
	 ('todo','新文章待审核：32242432','作者 zhangsan 提交了文章《32242432》，请尽快审核','{"id": 16, "bizType": "article"}','user',1,'sys','1','1','','2026-08-28 17:50:28','','2026-08-28 17:57:59',NULL,'0'),
	 ('todo','新文章待审核：32242432','作者 zhangsan 提交了文章《32242432》，请尽快审核','{"id": 16, "bizType": "article"}','user',2,'sys','1','1','','2026-08-28 17:50:28','','2026-08-28 17:57:59',NULL,'0'),
	 ('todo','新文章待审核：32242432','作者 zhangsan 提交了文章《32242432》，请尽快审核','{"id": 16, "bizType": "article"}','user',8,'portal','1','1','','2026-08-28 17:50:28','','2026-08-28 17:57:59',NULL,'0'),
	 ('system','文章审核通过：web 端如何实现一个语音转文字的','您的文章《web 端如何实现一个语音转文字的》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 7, "status": "published", "bizType": "article"}','user',7,'portal','1','0','','2026-08-28 17:54:45','','2026-08-28 17:54:44',NULL,'0'),
	 ('system','文章审核通过：李白文章解析','您的文章《李白文章解析》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 13, "status": "published", "bizType": "article"}','user',7,'portal','1','0','','2026-08-28 17:55:00','','2026-08-28 17:55:00',NULL,'0');
INSERT INTO `moyun-db`.sys_notification (`type`,title,content,`data`,`scope`,user_id,user_type,notice_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('system','文章审核通过：32242432','您的文章《32242432》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 16, "status": "published", "bizType": "article"}','user',6,'portal','1','0','','2026-08-28 17:58:00','','2026-08-28 17:57:59',NULL,'0'),
	 ('notice','面经审核结果','您的面经《java面试》审核通过，原因：好的',NULL,'user',6,'portal',NULL,'0','','2026-08-31 10:42:46','','2026-08-31 10:42:46',NULL,'0'),
	 ('todo','新文章待审核：22222222','作者 用户#null 提交了文章《22222222》，请尽快审核','{"id": 14, "bizType": "article"}','user',1,'sys','1','1','','2026-08-31 16:22:50','','2026-08-31 16:23:19',NULL,'0'),
	 ('todo','新文章待审核：22222222','作者 用户#null 提交了文章《22222222》，请尽快审核','{"id": 14, "bizType": "article"}','user',2,'sys','1','1','','2026-08-31 16:22:50','','2026-08-31 16:23:19',NULL,'0'),
	 ('todo','新文章待审核：22222222','作者 用户#null 提交了文章《22222222》，请尽快审核','{"id": 14, "bizType": "article"}','user',8,'portal','1','1','','2026-08-31 16:22:50','','2026-08-31 16:23:19',NULL,'0'),
	 ('system','文章审核通过：22222222','您的文章《22222222》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 14, "status": "published", "bizType": "article"}','user',6,'portal','1','0','','2026-08-31 16:23:19','','2026-08-31 16:23:19',NULL,'0'),
	 ('bookmark',' 收藏了你的文章',' 收藏了你的文章《岁月是一场无声的雪》','{"bizType": "bookmark", "articleId": 5, "fromUserId": 6}','user',7,'portal','1','0','','2026-09-02 14:22:40','','2026-09-02 14:22:39',NULL,'0'),
	 ('todo','新文章待审核：处暑过后，傍晚的风开始凉了','作者 zhangsan 提交了文章《处暑过后，傍晚的风开始凉了》，请尽快审核','{"id": 17, "bizType": "article"}','user',1,'sys','1','1','','2026-09-07 10:03:07','','2026-09-07 10:05:37',NULL,'0'),
	 ('todo','新文章待审核：处暑过后，傍晚的风开始凉了','作者 zhangsan 提交了文章《处暑过后，傍晚的风开始凉了》，请尽快审核','{"id": 17, "bizType": "article"}','user',2,'sys','1','1','','2026-09-07 10:03:07','','2026-09-07 10:05:37',NULL,'0'),
	 ('todo','新文章待审核：处暑过后，傍晚的风开始凉了','作者 zhangsan 提交了文章《处暑过后，傍晚的风开始凉了》，请尽快审核','{"id": 17, "bizType": "article"}','user',8,'portal','1','1','','2026-09-07 10:03:07','','2026-09-07 10:05:37',NULL,'0');
INSERT INTO `moyun-db`.sys_notification (`type`,title,content,`data`,`scope`,user_id,user_type,notice_type,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('system','文章审核通过：处暑过后，傍晚的风开始凉了','您的文章《处暑过后，傍晚的风开始凉了》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 17, "status": "published", "bizType": "article"}','user',6,'portal','1','0','','2026-09-07 10:05:37','','2026-09-07 10:05:37',NULL,'0'),
	 ('todo','新文章待审核：手机端发起的第一个文章','作者 zhangsan 提交了文章《手机端发起的第一个文章》，请尽快审核','{"id": 18, "bizType": "article"}','user',1,'sys','1','1','','2026-09-07 10:15:28','','2026-09-07 10:15:49',NULL,'0'),
	 ('todo','新文章待审核：手机端发起的第一个文章','作者 zhangsan 提交了文章《手机端发起的第一个文章》，请尽快审核','{"id": 18, "bizType": "article"}','user',2,'sys','1','1','','2026-09-07 10:15:28','','2026-09-07 10:15:49',NULL,'0'),
	 ('todo','新文章待审核：手机端发起的第一个文章','作者 zhangsan 提交了文章《手机端发起的第一个文章》，请尽快审核','{"id": 18, "bizType": "article"}','user',8,'portal','1','1','','2026-09-07 10:15:28','','2026-09-07 10:15:49',NULL,'0'),
	 ('system','文章审核通过：手机端发起的第一个文章','您的文章《手机端发起的第一个文章》已通过审核并发布。可在「我的文章」中查看详情。','{"id": 18, "status": "published", "bizType": "article"}','user',6,'portal','1','0','','2026-09-07 10:15:49','','2026-09-07 10:15:49',NULL,'0'),
	 ('system','话题审核通过：哈哈哈，世界太变态了？','您发起的话题《哈哈哈，世界太变态了？》已通过审核并发布。可在「我的话题」中查看详情。','{"id": 1, "status": "active", "bizType": "topic"}','user',6,'portal','1','0','','2026-09-07 17:52:45','','2026-09-07 17:52:45',NULL,'0');
INSERT INTO `moyun-db`.sys_notification_read (notification_id,user_id,user_type,read_time,create_time) VALUES
	 (15,7,'portal','2026-08-28 10:27:36','2026-08-28 10:27:36'),
	 (19,1,'sys','2026-08-28 13:13:53','2026-08-28 13:13:53'),
	 (16,1,'sys','2026-08-28 13:13:56','2026-08-28 13:13:56'),
	 (12,6,'portal','2026-08-28 13:50:29','2026-08-28 13:50:29'),
	 (30,6,'portal','2026-08-31 10:13:21','2026-08-31 10:13:21'),
	 (31,6,'portal','2026-08-31 10:44:50','2026-08-31 10:44:50'),
	 (35,6,'portal','2026-08-31 16:24:48','2026-08-31 16:24:48'),
	 (40,6,'portal','2026-09-07 10:13:39','2026-09-07 10:13:39'),
	 (44,6,'portal','2026-09-07 11:17:01','2026-09-07 11:17:01'),
	 (45,6,'portal','2026-09-07 17:59:32','2026-09-07 17:59:32');
INSERT INTO `moyun-db`.sys_platform (platform_code,platform_name,platform_type,description,`domain`,icon,sort_order,status,create_time) VALUES
	 ('portal','门户端','c端','求职、学习、成长','www.xulin.com','portal',1,1,'2026-09-17 17:53:20'),
	 ('ledger','记账端','c端','个人资产管理','ledger.xulin.com','ledger',2,1,'2026-09-17 17:53:20'),
	 ('admin','管理端','b端','后台管理','admin.xulin.com','admin',3,1,'2026-09-17 17:53:20'),
	 ('personality','人格分析端','c端','AI人格分析（预留）','me.xulin.com','peoples',4,1,'2026-09-17 17:53:20');
INSERT INTO `moyun-db`.sys_post (post_code,post_name,post_sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('ceo','董事长',1,'0','admin','2026-08-19 18:01:44','',NULL,'','0'),
	 ('se','项目经理',2,'0','admin','2026-08-19 18:01:44','',NULL,'','0'),
	 ('hr','人力资源',3,'0','admin','2026-08-19 18:01:44','',NULL,'','0'),
	 ('user','普通员工',4,'0','admin','2026-08-19 18:01:44','',NULL,'','0');
INSERT INTO `moyun-db`.sys_role (role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,update_by,update_time,remark) VALUES
	 ('超级管理员','admin',1,'1',1,1,'0','0','admin','2026-08-19 18:01:44','',NULL,'超级管理员'),
	 ('普通角色','common',2,'2',1,1,'0','0','admin','2026-08-19 18:01:44','',NULL,'普通角色');
INSERT INTO `moyun-db`.sys_role_dept (role_id,dept_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (2,100,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),
	 (2,101,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),
	 (2,105,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,3,'',NULL,'',NULL,NULL),
	 (1,116,'',NULL,'',NULL,NULL),
	 (1,1055,'',NULL,'',NULL,NULL),
	 (1,1056,'',NULL,'',NULL,NULL),
	 (1,1057,'',NULL,'',NULL,NULL),
	 (1,1058,'',NULL,'',NULL,NULL),
	 (1,1059,'',NULL,'',NULL,NULL),
	 (1,1060,'',NULL,'',NULL,NULL),
	 (1,5000,'',NULL,'',NULL,NULL),
	 (1,5001,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5002,'',NULL,'',NULL,NULL),
	 (1,5003,'',NULL,'',NULL,NULL),
	 (1,5004,'',NULL,'',NULL,NULL),
	 (1,5005,'',NULL,'',NULL,NULL),
	 (1,5006,'',NULL,'',NULL,NULL),
	 (1,5007,'',NULL,'',NULL,NULL),
	 (1,5008,'',NULL,'',NULL,NULL),
	 (1,5009,'',NULL,'',NULL,NULL),
	 (1,5010,'',NULL,'',NULL,NULL),
	 (1,5011,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5012,'',NULL,'',NULL,NULL),
	 (1,5013,'',NULL,'',NULL,NULL),
	 (1,5014,'',NULL,'',NULL,NULL),
	 (1,5015,'',NULL,'',NULL,NULL),
	 (1,5016,'',NULL,'',NULL,NULL),
	 (1,5017,'',NULL,'',NULL,NULL),
	 (1,5018,'',NULL,'',NULL,NULL),
	 (1,5019,'',NULL,'',NULL,NULL),
	 (1,5020,'',NULL,'',NULL,NULL),
	 (1,5021,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5022,'',NULL,'',NULL,NULL),
	 (1,5023,'',NULL,'',NULL,NULL),
	 (1,5024,'',NULL,'',NULL,NULL),
	 (1,5025,'',NULL,'',NULL,NULL),
	 (1,5026,'',NULL,'',NULL,NULL),
	 (1,5027,'',NULL,'',NULL,NULL),
	 (1,5028,'',NULL,'',NULL,NULL),
	 (1,5029,'',NULL,'',NULL,NULL),
	 (1,5030,'',NULL,'',NULL,NULL),
	 (1,5031,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5032,'',NULL,'',NULL,NULL),
	 (1,5033,'',NULL,'',NULL,NULL),
	 (1,5034,'',NULL,'',NULL,NULL),
	 (1,5035,'',NULL,'',NULL,NULL),
	 (1,5036,'',NULL,'',NULL,NULL),
	 (1,5037,'',NULL,'',NULL,NULL),
	 (1,5038,'',NULL,'',NULL,NULL),
	 (1,5039,'',NULL,'',NULL,NULL),
	 (1,5040,'',NULL,'',NULL,NULL),
	 (1,5041,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5042,'',NULL,'',NULL,NULL),
	 (1,5043,'',NULL,'',NULL,NULL),
	 (1,5044,'',NULL,'',NULL,NULL),
	 (1,5045,'',NULL,'',NULL,NULL),
	 (1,5046,'',NULL,'',NULL,NULL),
	 (1,5047,'',NULL,'',NULL,NULL),
	 (1,5048,'',NULL,'',NULL,NULL),
	 (1,5049,'',NULL,'',NULL,NULL),
	 (1,5050,'',NULL,'',NULL,NULL),
	 (1,5051,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5052,'',NULL,'',NULL,NULL),
	 (1,5053,'',NULL,'',NULL,NULL),
	 (1,5054,'',NULL,'',NULL,NULL),
	 (1,5055,'',NULL,'',NULL,NULL),
	 (1,5056,'',NULL,'',NULL,NULL),
	 (1,5057,'',NULL,'',NULL,NULL),
	 (1,5058,'',NULL,'',NULL,NULL),
	 (1,5059,'admin','2026-08-19 18:01:41','',NULL,NULL),
	 (1,5060,'admin','2026-08-19 18:01:41','',NULL,NULL),
	 (1,5063,'admin','2026-08-19 18:01:41','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5064,'admin','2026-08-19 18:01:41','',NULL,NULL),
	 (1,5065,'admin','2026-08-19 18:01:41','',NULL,NULL),
	 (1,5066,'admin','2026-08-19 18:01:41','',NULL,NULL),
	 (1,5067,'admin','2026-08-19 18:01:41','',NULL,NULL),
	 (1,5068,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5069,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5070,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5071,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5072,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5073,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5074,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5075,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5076,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5077,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5078,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5079,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5080,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5081,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5082,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5083,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5084,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5085,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5086,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5087,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5088,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5089,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5090,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5091,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5092,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5093,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5094,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5095,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5096,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5097,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5098,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5099,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5100,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5101,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5102,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5103,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5104,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5105,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5106,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5107,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5108,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5109,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5110,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5111,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5112,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5113,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5114,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5115,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5116,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5117,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5118,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5119,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5120,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5121,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5122,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5123,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5124,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5125,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5126,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5127,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5128,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5129,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5130,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5134,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5135,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5136,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5137,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5138,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5139,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5140,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5141,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5142,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5143,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (1,5146,'admin','2026-08-25 00:00:00','',NULL,NULL),
	 (1,5192,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5193,'admin','2026-08-19 18:01:46','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5194,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5195,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5196,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5197,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5198,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5199,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5200,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5201,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5202,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5203,'admin','2026-08-19 18:01:46','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5204,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5205,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5206,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5207,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5208,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5209,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5210,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5211,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5212,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5213,'admin','2026-08-19 18:01:46','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5214,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5215,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5216,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5217,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5218,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5219,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5220,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5221,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5222,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5223,'admin','2026-08-19 18:01:46','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5224,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5225,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5226,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5227,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5228,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5229,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5230,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5231,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5232,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5233,'admin','2026-08-19 18:01:46','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5234,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5235,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5236,'admin','2026-08-19 18:01:46','',NULL,NULL),
	 (1,5237,'',NULL,'',NULL,NULL),
	 (1,5238,'',NULL,'',NULL,NULL),
	 (1,5239,'',NULL,'',NULL,NULL),
	 (1,5248,'admin','2026-08-24 00:00:00','',NULL,NULL),
	 (1,5249,'admin','2026-08-24 00:00:00','',NULL,NULL),
	 (1,5250,'admin','2026-08-24 00:00:00','',NULL,NULL),
	 (1,5251,'admin','2026-08-24 00:00:00','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5252,'admin','2026-08-24 00:00:00','',NULL,NULL),
	 (1,5300,'',NULL,'',NULL,NULL),
	 (1,5301,'',NULL,'',NULL,NULL),
	 (1,5303,'',NULL,'',NULL,NULL),
	 (1,5304,'',NULL,'',NULL,NULL),
	 (1,5305,'',NULL,'',NULL,NULL),
	 (1,5306,'',NULL,'',NULL,NULL),
	 (1,5307,'',NULL,'',NULL,NULL),
	 (1,5308,'',NULL,'',NULL,NULL),
	 (1,5311,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5312,'',NULL,'',NULL,NULL),
	 (1,5313,'',NULL,'',NULL,NULL),
	 (1,5314,'',NULL,'',NULL,NULL),
	 (1,5317,'',NULL,'',NULL,NULL),
	 (1,5318,'',NULL,'',NULL,NULL),
	 (1,5319,'',NULL,'',NULL,NULL),
	 (1,5320,'',NULL,'',NULL,NULL),
	 (1,5321,'',NULL,'',NULL,NULL),
	 (1,5400,'',NULL,'',NULL,NULL),
	 (1,5401,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5402,'',NULL,'',NULL,NULL),
	 (1,5403,'',NULL,'',NULL,NULL),
	 (1,5404,'',NULL,'',NULL,NULL),
	 (1,5411,'',NULL,'',NULL,NULL),
	 (1,5412,'',NULL,'',NULL,NULL),
	 (1,5413,'',NULL,'',NULL,NULL),
	 (1,5415,'',NULL,'',NULL,NULL),
	 (1,5416,'',NULL,'',NULL,NULL),
	 (1,5417,'',NULL,'',NULL,NULL),
	 (1,5418,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5450,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5451,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5452,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5453,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5454,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5455,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5456,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5457,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5458,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5459,'admin','2026-09-07 15:07:33','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5460,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5461,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5462,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5463,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5464,'admin','2026-09-07 15:07:33','',NULL,NULL),
	 (1,5466,'',NULL,'',NULL,NULL),
	 (1,5467,'',NULL,'',NULL,NULL),
	 (1,5468,'',NULL,'',NULL,NULL),
	 (1,5469,'',NULL,'',NULL,NULL),
	 (1,5470,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5471,'',NULL,'',NULL,NULL),
	 (1,5472,'',NULL,'',NULL,NULL),
	 (1,5473,'',NULL,'',NULL,NULL),
	 (1,5474,'',NULL,'',NULL,NULL),
	 (1,5475,'',NULL,'',NULL,NULL),
	 (1,5476,'',NULL,'',NULL,NULL),
	 (1,5477,'',NULL,'',NULL,NULL),
	 (1,5478,'',NULL,'',NULL,NULL),
	 (1,5480,'',NULL,'',NULL,NULL),
	 (1,5481,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5482,'',NULL,'',NULL,NULL),
	 (1,5483,'',NULL,'',NULL,NULL),
	 (1,5500,'',NULL,'',NULL,NULL),
	 (1,5501,'',NULL,'',NULL,NULL),
	 (1,5502,'',NULL,'',NULL,NULL),
	 (1,5503,'',NULL,'',NULL,NULL),
	 (1,5504,'',NULL,'',NULL,NULL),
	 (1,5505,'',NULL,'',NULL,NULL),
	 (1,5506,'',NULL,'',NULL,NULL),
	 (1,5507,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,5510,'',NULL,'',NULL,NULL),
	 (1,5511,'',NULL,'',NULL,NULL),
	 (1,5512,'',NULL,'',NULL,NULL),
	 (1,5513,'',NULL,'',NULL,NULL),
	 (1,5514,'',NULL,'',NULL,NULL),
	 (1,5515,'',NULL,'',NULL,NULL),
	 (1,5516,'',NULL,'',NULL,NULL),
	 (1,5517,'',NULL,'',NULL,NULL),
	 (1,5518,'',NULL,'',NULL,NULL),
	 (1,5519,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,54031,'',NULL,'',NULL,NULL),
	 (1,54041,'',NULL,'',NULL,NULL),
	 (2,1,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,2,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,3,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,100,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,101,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,102,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,103,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,104,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (2,105,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,106,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,108,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,109,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,110,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,111,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,112,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,113,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,114,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,116,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (2,117,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,500,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,501,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1000,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1001,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1002,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1003,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1004,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1005,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1006,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (2,1007,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1008,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1009,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1010,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1011,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1012,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1013,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1014,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1015,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1016,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (2,1017,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1018,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1019,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1020,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1021,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1022,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1023,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1024,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1025,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1026,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (2,1027,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1028,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1029,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1030,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1031,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1032,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1033,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1034,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1039,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1040,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (2,1041,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1042,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1043,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1044,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1045,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1046,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1047,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1048,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1049,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1050,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (2,1051,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1052,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1053,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1054,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1055,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1056,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1057,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1058,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1059,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,1060,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_role_menu (role_id,menu_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (2,5134,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,5135,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,5137,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,5138,'admin','2026-08-19 18:01:45','',NULL,NULL),
	 (2,5139,'admin','2026-08-19 18:01:45','',NULL,NULL);
INSERT INTO `moyun-db`.sys_sensitive_word (word,category,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('示例敏感词1','other','0','admin','2026-08-19 18:01:41',NULL,NULL,'示例词，生产环境请替换为真实词库','0'),
	 ('示例敏感词2','ad','0','admin','2026-08-19 18:01:41',NULL,NULL,'示例词，生产环境请替换为真实词库','0'),
	 ('示例-广告','ad','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
	 ('示例-辱骂','insult','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
	 ('示例-色情','porn','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
	 ('示例-政治','politics','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0'),
	 ('示例-其他','other','0','admin','2026-08-19 18:01:41',NULL,NULL,NULL,'0');
INSERT INTO `moyun-db`.sys_user (dept_id,user_name,nick_name,user_type,email,phonenumber,sex,avatar,password,status,del_flag,login_ip,login_date,create_by,create_time,update_by,update_time,remark) VALUES
	 (103,'admin','若依','00','ry@163.com','15888888888','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1','2026-09-17 13:39:08','admin','2026-08-19 18:01:44','','2026-09-17 13:39:07','管理员'),
	 (105,'ry','若依','00','ry@qq.com','15666666666','1','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1','2026-08-19 18:01:44','admin','2026-08-19 18:01:44','admin','2026-09-17 13:40:22','测试员');
INSERT INTO `moyun-db`.sys_user_post (user_id,post_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,1,'admin','2026-07-28 15:42:34','','2026-07-28 15:42:34',NULL),
	 (2,2,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.sys_user_role (user_id,role_id,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,1,'admin','2026-08-19 18:01:44','','2026-08-19 18:01:44',NULL),
	 (2,2,'',NULL,'',NULL,NULL);
INSERT INTO `moyun-db`.vip_api_registry (api_path,http_method,controller_class,method_name,platform_code,benefit_code,consume,message,api_desc,enabled,scan_time,create_time,update_time) VALUES
	 ('/portal/interview/voice/start','POST','com.moyun.portal.controller.PortalVoiceInterviewController','start','portal','interview_unlimited',1,'免费面试次数已用完，语音面试为会员专属功能，请开通会员',NULL,1,'2026-09-17 18:03:47','2026-09-17 18:03:47','2026-09-17 18:03:47'),
	 ('/portal/ledger/ai/analysis/task','POST','com.moyun.ledger.controller.PortalLedgerAiController','submitTask','ledger','ai_analysis',1,'本月免费分析次数已用完，请开通记账VIP',NULL,1,'2026-09-17 18:03:47','2026-09-17 18:03:47','2026-09-17 18:03:47'),
	 ('/portal/resume/optimize/deep/{resumeId}/{jobTargetId}','POST','com.moyun.portal.controller.PortalResumeOptimizeController','deepOptimize','portal','resume_optimize',1,'简历深度优化次数已用完，请开通会员',NULL,1,'2026-09-17 18:03:47','2026-09-17 18:03:47','2026-09-17 18:03:47'),
	 ('/portal/resume/optimize/deep/{resumeId}/{jobTargetId}/async','POST','com.moyun.portal.controller.PortalResumeOptimizeController','deepOptimizeAsync','portal','resume_optimize',1,'简历深度优化次数已用完，请开通会员',NULL,1,'2026-09-17 18:03:47','2026-09-17 18:03:47','2026-09-17 18:03:47');
INSERT INTO `moyun-db`.vip_benefit (platform_code,benefit_code,benefit_name,description,sort_order,create_time) VALUES
	 ('portal','interview_unlimited','语音面试','不限次 AI 语音面试',1,'2026-09-17 17:53:20'),
	 ('portal','resume_optimize','简历深度优化','AI 逐项建议/前后对比/采纳保存',2,'2026-09-17 17:53:20'),
	 ('portal','report_share','报告分享','面试报告分享导出',3,'2026-09-17 17:53:20'),
	 ('portal','priority_queue','优先队列','面试优先调度',4,'2026-09-17 17:53:20'),
	 ('portal','reading_unlimited','读书空间','不限次阅读',5,'2026-09-17 17:53:20'),
	 ('portal','article_paid','付费文章','免费阅读付费文章',6,'2026-09-17 17:53:20'),
	 ('ledger','bill_parse','账单识别','每月账单截图识别次数',1,'2026-09-17 17:53:20'),
	 ('ledger','ai_analysis','AI 分析','AI 财务分析次数',2,'2026-09-17 17:53:20');
INSERT INTO `moyun-db`.vip_tier (platform_code,tier_code,tier_name,duration_days,price,original_price,popular,description,sort_order,status,create_time) VALUES
	 ('portal','free','免费版',0,0.00,NULL,0,'基础体验额度',1,1,'2026-09-17 17:53:20'),
	 ('portal','monthly','月卡会员',30,49.00,69.00,0,'全功能月度畅用',2,1,'2026-09-17 17:53:20'),
	 ('portal','yearly','年卡会员',365,399.00,588.00,1,'最受欢迎，全年畅用',3,1,'2026-09-17 17:53:20'),
	 ('portal','permanent','永久会员',-1,1299.00,1999.00,0,'一次买断终身可用',4,1,'2026-09-17 17:53:20'),
	 ('ledger','free','免费版',0,0.00,NULL,0,'基础记账体验',1,1,'2026-09-17 17:53:20'),
	 ('ledger','yearly','年卡会员',365,199.00,299.00,1,'智能账单识别 + AI 分析',2,1,'2026-09-17 17:53:20');
INSERT INTO `moyun-db`.vip_tier_benefit (platform_code,tier_code,benefit_code,benefit_value,period,create_time) VALUES
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
INSERT INTO `moyun-db`.vip_tier_benefit (platform_code,tier_code,benefit_code,benefit_value,period,create_time) VALUES
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
INSERT INTO `moyun-db`.vip_tier_benefit (platform_code,tier_code,benefit_code,benefit_value,period,create_time) VALUES
	 ('ledger','free','bill_parse','5','month','2026-09-17 17:53:21'),
	 ('ledger','free','ai_analysis','3','month','2026-09-17 17:53:21'),
	 ('ledger','yearly','bill_parse','100','month','2026-09-17 17:53:21'),
	 ('ledger','yearly','ai_analysis','unlimited','unlimited','2026-09-17 17:53:21');
