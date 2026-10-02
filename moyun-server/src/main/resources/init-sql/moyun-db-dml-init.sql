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
2. 追问有度：同一话题连续追问不超过 4 轮；已验证深度或候选人已无新信息时，立即切换到下一个考察方向，不恋战、也不蜻蜓点水地串场。
3. 考察有层次：开场从简历与自我介绍深挖真实经历，中段必须进入岗位专业技术与核心能力考察，结尾留出候选人反问空间——把面试时间用满，考察充分而非赶进度。
4. 不泄露评分维度与分析细节，不主动给标准答案，不说"你的得分是"。
5. 口语化、自然，像面对面交谈：单轮话术控制在 1-3 句，避免书面语、条目式表达和长篇大论。
6. 候选人答不上来或明显偏题时，给一次自然的引导或换题，不反复纠缠同一考点。
7. 全程只以面试官身份说话，不扮演其他角色，不输出任何 JSON、标记或系统文字。','["13"]','{"13":1}',17,'deepseek-v4-pro',0.7,2048,0.7,10,1,'你好，欢迎参加{{position}}岗位的模拟面试。我是今天的面试官，放松心态，我们像聊天一样开始。准备好了的话，我们直接进入第一个问题。','',1,20,0,'',NULL,'manual','',1,'pub-wxg0pgbp5jqnj5cp9fw8i','','2026-09-07 09:15:33','2026-09-16 14:04:15',2.0,1,1,0.2,0.8,0,0);

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
-- 知识库配置模板（v13.39 补齐：支撑「快速配置（推荐）」Tab，缺失时用户只能走自定义配置）
INSERT INTO ai_knowledge_config_template (template_name,template_desc,template_type,config_json,is_system,use_count,is_recommended) VALUES
	 ('标准文档','适用于一般文档、技术手册等，平衡性能和准确度','general','{"indexMode": "high_quality", "segmentMode": "general", "rerankEnabled": true, "retrievalMode": "vector", "retrievalTopK": 10, "segmentMaxLength": 800, "preprocessRemoveUrls": false, "segmentOverlapLength": 100, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": true}',1,0,1),
	 ('题库/QA精准模式','适用于题库、问答对等短文本，确保每道题独立检索','general','{"indexMode": "high_quality", "segmentMode": "qa", "rerankEnabled": true, "retrievalMode": "vector", "retrievalTopK": 15, "segmentMaxLength": 400, "preprocessRemoveUrls": true, "segmentOverlapLength": 50, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": true}',1,4,0),
	 ('长文档深度模式','适用于长篇文章、研究报告等，保留更多上下文','general','{"indexMode": "high_quality", "segmentMode": "general", "rerankEnabled": true, "retrievalTopK": 8, "segmentMaxLength": 1200, "preprocessRemoveUrls": false, "segmentOverlapLength": 200, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": false}',1,1,0),
	 ('代码技术文档','适用于代码、API文档等技术内容','technical','{"indexMode": "high_quality", "segmentMode": "code", "rerankEnabled": false, "retrievalMode": "vector", "retrievalTopK": 12, "segmentMaxLength": 600, "preprocessRemoveUrls": false, "segmentOverlapLength": 80, "preprocessReplaceSpaces": false, "preprocessRemoveExtraNewlines": false}',1,0,0),
	 ('经济快速模式','降低资源消耗，适合大批量文档或测试环境','general','{"indexMode": "economy", "segmentMode": "general", "rerankEnabled": false, "retrievalMode": "vector", "retrievalTopK": 5, "segmentMaxLength": 500, "preprocessRemoveUrls": true, "segmentOverlapLength": 50, "preprocessReplaceSpaces": true, "preprocessRemoveExtraNewlines": true}',1,76,0);
-- 领域词典（v13.39 补齐：支撑智能体「专业词典」下拉与「领域词典」管理页；专业 14 + 全局 3）
INSERT INTO ai_domain_dictionary (keyword,related_terms,category,description,is_global,enabled,priority) VALUES
	 ('服务器','cpu,gpu,npu,内存,存储,硬盘,系统盘,数据盘,鲲鹏,昇腾,算力,主机,机器,配置,规格','硬件','服务器相关术语',0,1,10),
	 ('架构','系统架构,技术架构,平台架构,设计,模块,组件,层次,结构,框架','技术','架构相关术语',0,1,8),
	 ('模型','大模型,embedding,向量,llm,ai模型,算法,训练,推理','AI','模型相关术语',0,1,9),
	 ('知识库','文档,向量库,rag,检索,知识管理,知识图谱','AI','知识库相关术语',0,1,7),
	 ('部署','安装,配置,环境,运维,上线,发布','运维','部署相关术语',0,1,6),
	 ('性能','速度,效率,吞吐量,延迟,响应时间,优化','技术','性能相关术语',0,1,5),
	 ('安全','权限,认证,授权,加密,防护,隔离','安全','安全相关术语',0,1,8),
	 ('数据库','MySQL,PostgreSQL,MongoDB,Redis,Oracle,SQL,NoSQL,索引,事务,主从,分库分表,读写分离','技术','数据库相关术语，包含关系型和非关系型数据库',0,1,9),
	 ('微服务','SpringCloud,Dubbo,gRPC,服务注册,服务发现,负载均衡,熔断,限流,网关,配置中心','技术','微服务架构相关术语',0,1,8),
	 ('容器','Docker,Kubernetes,K8s,Pod,容器编排,镜像,Harbor,Helm,Service,Deployment','运维','容器化和容器编排相关术语',0,1,8),
	 ('前端','Vue,React,Angular,JavaScript,TypeScript,CSS,HTML,Webpack,Vite,组件,路由,状态管理','技术','前端开发相关术语',0,1,7),
	 ('测试','单元测试,集成测试,压力测试,自动化测试,测试用例,Bug,缺陷,回归测试,冒烟测试,UAT','质量','软件测试相关术语',0,1,6),
	 ('DevOps','CI/CD,Jenkins,GitLab,流水线,自动化部署,监控,日志,告警,SRE,可观测性','运维','DevOps和持续集成相关术语',0,1,7),
	 ('网络','TCP,UDP,HTTP,HTTPS,DNS,CDN,负载均衡,防火墙,VPN,代理,带宽,延迟','基础设施','网络通信相关术语',0,1,6),
	 ('产品','需求,PRD,原型,用户故事,MVP,迭代,版本,上线,灰度,AB测试,用户体验,交互设计','产品','产品管理相关术语',1,1,5),
	 ('财务','预算,成本,利润,营收,ROI,现金流,资产负债,损益表,审计,税务,发票,报销','财务','财务管理相关术语',1,1,5),
	 ('人力资源','招聘,面试,入职,离职,绩效,考核,薪酬,福利,培训,晋升,组织架构,人才盘点','人力','人力资源管理相关术语',1,1,5);
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
	 ('voice_interview','AI 语音面试','AI 语音模拟面试：主干走网关会话流式通道，task 子任务（warmup/answer_analysis/self_intro）拆行配置驱动（2B.5，原 VoiceInterviewHandler 已删；人设走 ai_agent(48)）','chat',(SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1),NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,NULL,NULL,'sync',NULL,'',2048,0.7,30,3,'',100,60,NULL,0,NULL,'',0,3600,'v1',100,0,1,1,0,'2026-09-07 15:07:33',NULL,0),
	 ('sensitive_word','敏感词检测','文本敏感词识别与风险分级（2B.3 配置驱动，原 SensitiveWordHandler 提示词逐字收编；降级兜底数据化 fallback_response）','classification',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是内容安全审核专家。检测文本是否包含敏感内容（涉政/色情/暴恐/辱骂/违法广告等），只输出 JSON：
{"hasSensitive": true/false,
 "words": ["命中的敏感词或类别"],
 "riskLevel": "high/medium/low",
 "suggestion": "处理建议（正常内容给''无风险''）"}
无敏感内容时 hasSensitive=false、words=[]、riskLevel="low"。禁止输出 JSON 以外内容。

{{data:待检测文本|text}}','{"text": "待检测文本（数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,'{"hasSensitive": false, "words": [], "riskLevel": "low", "suggestion": "AI服务暂时不可用，已跳过检测"}',0,3600,'v1',100,0,1,1,0,'2026-09-09 11:15:46',NULL,0),
	 ('daily_topic','今日主题','每日主题生成（2B.2 配置驱动，原 DailyTopicHandler 提示词逐字收编）','generation',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是内容运营专家。为指定日期生成一个当日主题，只输出 JSON：
{"title": "主题标题（15字内，有吸引力）",
 "description": "主题描述（50字内）",
 "category": "分类（技术/职场/生活/热点）"}
标题避免与历史主题重复。禁止输出 JSON 以外内容。

日期：{{date}}
{{data:领域|domain}}
{{data:已生成过的标题（避免重复）|excludeTitles}}','{"date": "生成日期（yyyy-MM-dd）", "domain": "领域（可选，空值自动丢弃）", "excludeTitles": "已生成过的标题（可选，空值自动丢弃）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-09 11:15:47',NULL,0),
	 ('finance_analysis','AI 财务分析','2B.4 查数下沉：LedgerAiAnalysisServiceImpl 组装 window/ledgerContext，DefaultSceneExecutor 配置驱动执行；人设走 ai_agent（按名称解析）','analysis',(SELECT id FROM ai_agent WHERE name='财务分析师' AND deleted=0 ORDER BY id LIMIT 1),NULL,NULL,NULL,NULL,'{}','defaultSceneExecutor','execute',NULL,'当前统计分析窗口：{{window}}

以下是系统规则引擎已经计算完成的精确财务数据，包含全量指标、逐月趋势、分类环比、预算执行、债务明细等所有信息，请你直接引用这些数值完成分析，不要自行修改计算：
{{data:财务数据|ledgerContext}}
','{"window": "统计窗口文案，如：本月（自 2026-09-01 起，含数据 3 个月；另附近6个月趋势数据）", "ledgerContext": "业务 Service 组装的财务上下文 JSON（画像/核心指标护栏/收入来源/支出结构Top5/负债明细含清偿测算/逐月收支趋势/分类环比/预算执行；数据通道隔离）"}','sync','{"risks": [{"level": "string，仅允许取值 high/medium/low", "title": "string，风险短标题", "detail": "string，风险详细说明", "evidence": "string，支撑该风险的具体数据依据"}], "summary": "string，完整的财务分析综述，讲清周期内的收支故事与核心特征", "healthScore": "int 0-100，基于传入指标计算的财务健康分", "suggestions": [{"icon": "string，前端可直接使用的图标标识，如 wallet / save / debt / invest", "title": "string，建议短标题", "detail": "string，建议的具体执行动作说明", "expectedImpact": "string，该建议落地后可实现的量化收益效果"}]}','',2048,0.7,30,3,'aaa',100,60,NULL,0,NULL,'',0,3600,'v1',100,0,0,1,0,'2026-09-09 18:11:29',NULL,0),
	 ('default_chat','智能体对话','智能体动态对话（/cms/ai/chat/*）：治理配置载体（限流/执行日志），Agent 由请求动态指定，人设走 ai_agent.system_prompt','chat',NULL,NULL,NULL,NULL,NULL,NULL,'dynamicChatBridge','execute',NULL,NULL,NULL,'stream',NULL,'text',2048,0.7,30,3,NULL,60,3600,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-16 17:01:51',NULL,0);
-- task 拆行（AI统一网关整改 2B.1）：scene_code 存全码 scene:task，业务调用传主码+input.task；
-- 任务指令与数据全部进 user_prompt_template（systemPromptTemplate 已废弃，人设由 Agent 表承载）；
-- {{data:标签|key}} 为数据通道占位符，值经 PromptInjectionGuard 分隔符隔离，空值自动丢弃；
-- voice_interview 行绑定「AI面试官·默认」agent 以保持模型连续性；resume_optimize 走默认模型。
-- ⚠️ v13.38 修复：原种子硬编码 agent_id=48，但 ai_agent 的 INSERT **不含 id 列**（自增分配，
--    全新库恒为 1/2）→ 48 必然悬空 → 会话链解析面试官失败、面试开不起来。
--    现改为按 name 子查询取 id（执行顺序：ai_model_config L66 → ai_agent L4 → ai_scene_config L77+）。
INSERT INTO ai_scene_config (scene_code,scene_name,description,scene_category,agent_id,model_config_id,knowledge_library_ids,tool_ids,workflow_id,config_json,handler_bean_name,handler_method,system_prompt_template,user_prompt_template,prompt_placeholders,output_mode,output_schema,output_parser,max_tokens,temperature,timeout_seconds,retry_count,rate_limit_key,rate_limit_count,rate_limit_time,daily_token_limit,enable_output_filter,fallback_model_id,fallback_response,enable_cache,cache_ttl,version,weight,priority,is_default,enabled,open_api,create_time,update_time,deleted) VALUES
	 ('resume_optimize:advice','简历优化-改进建议','task 拆行：评分明细→改进建议（原 ResumeOptimizeHandler.advice 提示词逐字收编）','generation',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是一名资深 HR 与简历顾问，擅长基于评分明细给出可执行的改进建议。请返回 JSON 格式，字段：summary(整体总结), advices(数组，每项含 dimension/priority(high/medium/low)/content/type(fill/refine/match)/optimized), missingSkills(字符串数组)。content 为该维度的改进思路说明；optimized 为优化后的完整可用文本（可直接替换简历对应模块内容），必须基于用户简历现有信息改写而非凭空编造，量化数据无依据时可使用占位符如 [X%] 供用户填写；dimension 取值限定：基本信息/求职意向/教育经历/工作经历/项目经历/技能列表/自我介绍/岗位匹配度。建议要具体、可执行，优先关注得分率低于60%的维度与岗位匹配度缺失技能。只输出 JSON 本体，禁止使用 markdown 代码块（```）包裹，禁止在 JSON 前后添加任何说明文字。

{{data:业务数据|context}}','{"context": "业务 Service 组装的评分明细/目标岗位上下文（数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('resume_optimize:job_match','简历优化-岗位匹配','task 拆行：JD×简历→匹配报告（原 ResumeOptimizeHandler.job_match 提示词逐字收编）','generation',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是一名资深技术招聘官，负责评估候选人与岗位的匹配度。请基于目标岗位JD和候选人简历，返回 JSON：matchScore(0-100综合匹配度), grade(excellent/good/medium/poor), matchedKeywords(数组,简历已覆盖的JD核心要求关键词), missingKeywords(数组,简历缺失的JD核心要求关键词), dimensions(对象,含四个维度，每维 score 0-100 与 suggestions 数组: keywordMatch关键词匹配/experienceMatch经验匹配/skillMatch技能匹配/structureMatch结构完整度), summary(2-3句总体评价与改进方向)。评估要客观，基于简历真实内容，缺失项如实指出；关键词控制在20个以内。只输出 JSON 本体，禁止使用 markdown 代码块（```）包裹，禁止在 JSON 前后添加任何说明文字。

{{data:业务数据|context}}','{"context": "业务 Service 组装的岗位JD+候选人简历上下文（数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('resume_optimize:field_assist','简历优化-字段辅助','task 拆行：字段级 3 版本优化（原 ResumeOptimizeHandler.field_assist 提示词逐字收编）','generation',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是一名资深简历优化专家，对简历中的指定字段给出3个不同风格的优化版本。优化原则：STAR法则（情境-任务-行动-结果）、量化数据（无依据数据用[X%][X万]占位符供用户填写）、突出与目标岗位相关的能力、专业商务表达避免口语化、每版50-150字。三个版本风格差异化：版本1侧重成果量化（推荐），版本2侧重技术深度，版本3侧重业务价值。保持与原文语义一致，禁止编造经历。返回 JSON：{"suggestions":[{"text":"优化后完整文本","reason":"一句话优化理由"}]}，恰好3条。只输出 JSON 本体，禁止 markdown 代码块包裹，禁止前后说明文字。

{{data:业务数据|context}}','{"context": "业务 Service 组装的字段原文+字段类型+目标岗位上下文（数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('resume_optimize:draft_empty','简历优化-空字段草稿','task 拆行：空字段初始草稿（原 ResumeOptimizeHandler.draft_empty 提示词逐字收编）','generation',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是一名简历撰写专家。根据用户已有信息，为空缺的字段生成初始草稿。生成原则：STAR 法则（情境-任务-行动-结果）、量化数据（无依据数据用 [X%][X万] 占位符供用户填写）、突出与目标岗位相关的能力、专业商务表达避免口语化。工作经历 2-3 条，每条描述 50-150 字；项目经历 2-3 条，每条描述 50-150 字；自我介绍 100-200 字，突出技能和经验。保持语义合理，禁止编造具体公司名（用 [公司名] 占位符）。返回 JSON：{works:[{company,position,startDate,endDate,description}],projects:[{name,role,startDate,endDate,description}],selfIntro:""}。仅生成空缺字段，已有字段不输出。只输出 JSON 本体，禁止 markdown 代码块包裹，禁止前后说明文字。

{{data:业务数据|context}}','{"context": "业务 Service 组装的用户已有信息+目标岗位上下文（数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('resume_optimize:deep_optimize','简历优化-深度优化','task 拆行：整份简历逐项深度优化（原 ResumeOptimizeHandler.deep_optimize 提示词逐字收编）','generation',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是一名资深简历优化专家，基于目标岗位JD对简历进行逐项深度优化。返回 JSON：summary(总体优化说明，50字内), items(优化建议数组，3-6项)。每项含：section(必为以下枚举之一：objective/education/work/project/skills/selfIntro；严禁使用复数如works/projects，严禁使用experience/introduction 等同义词，必须完全匹配枚举值), index(列表条目索引，从0开始；skills 填 0), field(position/description/name), optimized(优化后完整文本，可直接替换，50-200字), reason(优化理由，一句话，30字内)。不要输出 original 字段（原文由系统回填）。优化原则：STAR法则+量化数据+[X%]占位符（无依据数据用占位符供用户填写）；skills 的 optimized 用"精通：A、B\\n熟练：C"格式；保持语义一致禁止编造经历。只输出 JSON 本体，禁止 markdown 代码块包裹，输出务必完整，禁止中途截断。

{{data:业务数据|context}}','{"context": "业务 Service 组装的目标岗位JD+简历核心内容上下文（数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('voice_interview:warmup','AI 语音面试-预热','task 拆行：候选人画像+考察计划+开场白+首题（原 VoiceInterviewHandler.warmup 提示词逐字收编；v13.62 考察方向数与计划题数联动+类别覆盖约束）','chat',(SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1),NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你正在主持一场模拟面试，请先完成面试预热理解，只输出如下 JSON（不要任何其他文字）：
{
  "understanding": {
    "candidateProfile": "50字内的候选人画像（背景/技术栈/经验层次）",
    "strengths": ["结合简历与岗位判断的1-2个优势"],
    "concerns": ["需要重点验证的1-2个疑点"]
  },
  "interviewPlan": {
    "focusAreas": [{"area": "考察方向", "reason": "为何考察", "depth": "basic或intermediate或deep"}]
  },
  "opening": "1-2句面试官开场白（欢迎+放松提示，口语化）",
  "firstQuestion": "第一个问题：固定为请候选人做自我介绍，并提示结合与应聘岗位相关的经历"
}
考察方向数量与计划问题数一致（3-15 个）。方向必须按类别覆盖、避免全部挤在同一类：项目/经历深挖 1-2 个、技术基础 1-2 个、岗位核心技能 1-2 个、系统设计或场景运用 1 个、软素质/协作 0-1 个；优先来自岗位要求JD与知识库参考片段，其次来自简历项目；depth 结合难度设定。

{{data:面试背景|context}}
{{data:候选人简历摘要|resumeDigest}}
{{data:岗位要求JD|jd}}
{{data:知识库参考片段（出题参考）|kbSnippets}}','{"context": "岗位/难度/计划问题数（业务组装的面试背景，数据通道隔离）", "resumeDigest": "候选人简历摘要（可选，空值自动丢弃）", "jd": "岗位要求JD（可选，空值自动丢弃）", "kbSnippets": "知识库参考片段（可选，空值自动丢弃）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('voice_interview:answer_analysis','AI 语音面试-回答分析','task 拆行：候选人回答深度分析（原 VoiceInterviewHandler.answer_analysis 提示词逐字收编）','chat',(SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1),NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'{{context}}
候选人的语音转写回答见用户消息（可能口语化、有转写噪音）。
请以严格的技术面试官标准分析该回答，只输出如下 JSON（不要任何其他文字）：
{
  "score": 0-100的整数,
  "dimensions": {"relevance": 0-100, "professionalism": 0-100, "fluency": 0-100, "interactivity": 0-100, "confidence": 0-100, "logic": 0-100},
维度定义：relevance=回答与问题的相关性；professionalism=技术深度与专业度；fluency=表达流畅度；interactivity=互动性（举例/对比/坦诚沟通）；confidence=自信笃定程度；logic=逻辑条理与结构。
  "feedback": "两到三句中文点评，先肯定再指出问题",
  "flaws": ["回答中暴露的具体漏洞或模糊点，每条一句话，最多3条，没有则空数组"],
  "level": "junior或mid或senior，对候选人当前真实水平的判断",
  "followupWorth": true或false，该回答是否存在值得追问的漏洞,
  "followupQuestion": "若followupWorth为true，给出一句针对漏洞的追问；必须引用候选人回答中的具体表述",
  "guidance": "若回答明显跑偏，给出一句引导性提示，否则为空字符串"
}
打分参考：完全跑题<30；浅层正确但无细节50-65；有正确框架和部分细节65-80；深入准确有取舍权衡80+。

{{data:候选人语音转写回答|transcript}}','{"context": "面试官人设+题目+考察要点（业务组装）", "transcript": "候选人语音转写回答（外部不可信数据，数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('voice_interview:self_intro','AI 语音面试-自我介绍评分','task 拆行：自我介绍 4 维评分（原 VoiceInterviewHandler.self_intro 提示词逐字收编）','chat',(SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1),NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是一位资深技术面试官，请对候选人的自我介绍进行严格评估。目标岗位：{{context}}。只输出如下 JSON（不要任何其他文字）：
{
  "scores": {"structure": 0-100, "awareness": 0-100, "matching": 0-100, "fluency": 0-100},
  "comment": "两到三句中文总评，先肯定亮点再指出不足",
  "strengths": ["1-2条亮点，每条一句话"],
  "weaknesses": ["1-2条不足，每条一句话"],
  "followupWorth": true或false（自我介绍中是否有值得追问的模糊点）,
  "followupQuestion": "followupWorth 为 true 时给出一句针对性追问，必须引用候选人原话"
}
维度定义：structure=逻辑结构（条理/详略/结构词）；awareness=自我认知（优劣势/职业规划清晰度）；matching=岗位匹配（技术栈/项目经历与目标岗位相关度）；fluency=表达流畅（口语自然度/信息密度）。
打分参考：结构混乱<40；基本连贯50-65；条理清晰有详略70-85；结构完整且亮点突出85+。

{{data:候选人自我介绍|transcript}}','{"context": "目标岗位（可空）", "transcript": "候选人自我介绍转写（外部不可信数据，数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('voice_interview:opening_fallback','AI 语音面试-开场降级','task 拆行：warmup 失败后的简版开场白+首题（v13.44 批次 1 自代码 generateOpening 迁移；V1.1#1「收编不删除」——warmup 失败多为瞬时网络抖动，删兜底=一次抖动一场面试开不了头）','chat',(SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1),NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是一位专业的技术面试官，即将开始一场 {{context}} 岗位的面试（难度：{{difficulty}}）。请用一两句话自然开场，并直接提出第一个问题。要求：① 开场简洁友好，不做冗长自我介绍；② 第一个问题应贴合岗位与难度，可先请候选人做自我介绍或简述与该岗位最相关的经历；③ 只输出开场白与问题本身，不要输出任何解释、标题或 JSON 结构。','{"context": "岗位名称", "difficulty": "难度", "agentPersona": "面试官人设（系统提示词）", "resumeDigest": "候选人简历摘要"}','sync',NULL,'text',512,0.7,60,0,NULL,30,60,NULL,1,NULL,'面试官开场生成失败，请稍后重试',0,NULL,1,1,0,1,1,0,NOW(),NULL,0),
	 ('voice_interview:hint','AI 语音面试-思考提示','task 拆行：一句话思考引导（不泄答案）（v13.44 批次 1 自代码 requestHint 迁移；计费口径=免费，两级配额：每题3次/全场15次）','chat',(SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1),NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'候选人请求思考提示。请以面试官身份给一句简短的思考引导（提示回答方向或组织思路，不直接给出答案），40字以内，只输出这句话。','{"question": "当前题目", "answer": "候选人已作答内容（可空）"}','sync',NULL,'text',128,0.7,30,0,NULL,20,60,NULL,1,NULL,'别着急，可以从你熟悉的相关项目经历入手，按「背景→做法→结果」的思路组织回答。',0,NULL,1,1,0,1,1,0,NOW(),NULL,0),
	 ('voice_interview:report_review','AI 语音面试-整场复盘（含追问预测）','task 拆行：整场复盘 + 追问预测（一次调用双产出，V1.2 §3.2 裁决不单开——两触点输入同源，单开=输入token翻倍）（v13.46 批次 1 自代码 enhanceReportByAgent 迁移；v13.47 批次 2 扩 levelEstimate 结构化 + predictedQuestions 上限6 + perQuestion 瘦身为「仅回填需修正的题」；字段规范进 user_prompt_template——system_prompt_template 已废弃，人设由 Agent 表承载）','chat',(SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1),NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'【任务】你是一位资深技术面试官，面试已结束。请基于下方候选人资料与整场对话记录，输出结构化复盘报告 JSON。\n【字段要求】\n1. overallComment：3-5 句整场总评，结合岗位要求评价整体表现，指出最突出的特点。\n2. levelEstimate：候选人水平定级，**只能是 junior / mid / senior 之一**（依据：回答深度、技术准确度、项目复杂度）。\n3. jobMatch：{rate: 0-100 整数匹配度, reason: 1-2 句依据（对照岗位要求与实际作答）}。\n4. highlights：2-4 条真实亮点数组，每条 {title: 短标题≤12字, detail: 引用作答中的具体内容说明}。\n5. weakPoints：2-4 条薄弱点数组，每条 {title: 短标题≤12字, detail: 具体不足与影响，禁止复述问题原文}。\n6. suggestions：3-5 条可执行改进建议字符串数组，结合简历与岗位，每条不超过 60 字。\n7. perQuestion：逐题分数修正，格式 [{questionIdx: 题号, score: 0-100 整数}]，**仅回填你认为需要修正的题目**，无需逐题全列（逐题点评文本由系统复用逐题分析结果，你不需要重复产出）。\n8. dimensions：{relevance 切题度, professionalism 专业深度, fluency 表达流畅, interactivity 互动质量, confidence 自信度, logic 逻辑结构}，0-100 整数。\n9. predictedQuestions：追问预测，**最多 6 条**（已问 + 未问合计），每条 {question: 预测问题, briefAnswer: 要点式简答≤80字, analysis: 为什么会被问≤60字, knowledgePoint: 考点标签2-6字, askedThisRound: true或false, askedScore: 本场得分（askedThisRound 为 true 时填，否则 null）}。\n   predictedQuestions 硬约束：① 只能基于简历摘要中的**真实内容**提问，禁止推断未提及的经历；② 禁止编造项目名、公司名或技术栈；③ 是否已问**必须以对话记录为准**；④ **未被问到的问题优先覆盖**「简历写了但本场未深挖的条目」与「岗位要求中简历未体现的差距项」。\n只输出 JSON 对象，不要输出任何其他文本。\n\n【候选人资料与对话记录】\n目标岗位：{{position}}\n岗位要求：{{jd}}\n候选人简历摘要：\n{{resumeDigest}}\n整场对话记录（含每题初评分，供参考）：\n{{qaList}}','{"position": "岗位名称", "jd": "岗位要求", "resumeDigest": "候选人简历摘要", "qaList": "整场对话记录（含每题初评分）"}','sync',NULL,'json',2560,0.7,180,0,NULL,10,60,NULL,1,NULL,'报告生成中，请稍后重试',0,NULL,1,3,0,1,1,0,NOW(),NULL,0),
	 ('voice_interview:industry_insight','AI 语音面试-发展方向','task 拆行：发展方向建议（懒生成：首次打开 tab 才调用；输入追加本场报告上下文以与 resume_optimize:job_match 划清边界；预留 knowledge_library_ids 走 RAG 作为真动态来源）','chat',(SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1),NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'【任务】你是一位资深的职业发展顾问。请结合候选人的目标岗位、简历技能与本场面试暴露的真实薄弱点，输出个人化的发展方向分析 JSON。\n【字段要求】\n1. trends：技术趋势 3-4 条，每条 {title: ≤12字, detail: ≤60字, maturity: 成熟期/上升期/早期}。\n2. supplyDemand：{existing: [简历已具备的技能标签 3-8 个], missing: [建议补充的技能标签 3-8 个]}。\n3. actions：3 条行动建议，每条 {content: ≤80字可执行建议, relatedWeakPoint: 必须引用本场报告中的真实薄弱点原文}。\n【硬约束】① 严禁标注具体百分比、薪酬数字或时效性季度数据（无实时数据，编造时效数据会让用户误信）；\n② 每条行动建议必须锚定本场真实薄弱点，禁止「建议学习云原生」这类与个人无关的通用废话；\n③ 措辞为方向性判断，不承诺实时行业动态。只输出 JSON 对象，不要输出任何其他文本。\n\n【候选人资料】\n目标岗位：{{position}}\n岗位要求：{{jd}}\n简历技能：{{skills}}\n本场薄弱点：{{weakPoints}}\n水平定级：{{levelEstimate}}\n低分维度：{{lowDimensions}}','{"position": "岗位名称", "jd": "岗位要求", "skills": "简历技能", "weakPoints": "本场薄弱点", "levelEstimate": "水平定级", "lowDimensions": "低分维度"}','sync',NULL,'json',1536,0.7,120,1,NULL,5,600,NULL,1,NULL,'发展方向分析生成失败，请稍后重试',0,3600,1,1,0,1,1,0,NOW(),NULL,0);
-- 简单场景配置驱动迁移（AI统一网关整改 2B.2）：daily_topic 原行就地收编（上方）；
-- writing_prompt/content_tags 此前无配置行（AI 路径实际不可用，业务静默走兜底），
-- 本批新增后正式启用；输出契约统一为 JSON（业务读 GenericSceneData.structured），
-- 业务上下文组装（星期/特殊日期/内容截断）下沉各自 Service/Controller
INSERT INTO ai_scene_config (scene_code,scene_name,description,scene_category,agent_id,model_config_id,knowledge_library_ids,tool_ids,workflow_id,config_json,handler_bean_name,handler_method,system_prompt_template,user_prompt_template,prompt_placeholders,output_mode,output_schema,output_parser,max_tokens,temperature,timeout_seconds,retry_count,rate_limit_key,rate_limit_count,rate_limit_time,daily_token_limit,enable_output_filter,fallback_model_id,fallback_response,enable_cache,cache_ttl,version,weight,priority,is_default,enabled,open_api,create_time,update_time,deleted) VALUES
	 ('writing_prompt','今日写作主题','每日写作主题生成（2B.2 配置驱动，原 WritingPromptHandler 提示词逐字收编，输出契约改 JSON）','generation',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是社区写作平台的编辑，负责为每天设计一个"今日写作主题"，激励创作者写出真实、有感染力的文章。
要求：
1. 主题必须具体、可写，能激发真实表达，避免"人生","梦想"等大词。
2. 标题在 30 字以内，简洁有力，吸引点击。
3. 描述在 100 字以内，必须包含写作切入点。
4. 分类严格限定为以下之一：生活/职场/情感/虚构/哲思。
只输出 JSON（不要任何其他文字）：{"title": "标题", "category": "分类", "description": "描述"}

{{data:写作主题任务数据|context}}','{"context": "业务组装的日期/星期/特殊日期上下文（数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('content_tags','内容标签提取','标题+正文提取 3~8 个主题标签（2B.2 配置驱动，原 ContentTagsHandler 提示词逐字收编，输出契约改 JSON）','classification',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是内容平台的编辑。请根据下面的内容提取 3~8 个最贴切的主题标签。
只输出 JSON（不要任何其他文字）：{"tags": ["标签1", "标签2", "标签3"]}

{{data:标题|title}}
{{data:内容|content}}','{"title": "内容标题（可选，空值自动丢弃）", "content": "正文纯文本（调用方截断 3000 字，数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0);
-- 简单场景配置驱动迁移（AI统一网关整改 2B.3）：resume_parse / resume_optimize 主场景 / question_generate
-- （主场景 + task=jd_keywords 拆行）/ article_meta 新增配置行，sensitive_word 原行就地收编（上方）；
-- 删除 ResumeParse/ResumeOptimize/QuestionGenerate/ArticleMeta/SensitiveWord/KnowledgeQa 六个 Handler，
-- knowledge_qa 无业务调用方（RAG 问答由 chat 链承担）整行移除；输出契约统一 JSON（业务读 GenericSceneData.structured）
INSERT INTO ai_scene_config (scene_code,scene_name,description,scene_category,agent_id,model_config_id,knowledge_library_ids,tool_ids,workflow_id,config_json,handler_bean_name,handler_method,system_prompt_template,user_prompt_template,prompt_placeholders,output_mode,output_schema,output_parser,max_tokens,temperature,timeout_seconds,retry_count,rate_limit_key,rate_limit_count,rate_limit_time,daily_token_limit,enable_output_filter,fallback_model_id,fallback_response,enable_cache,cache_ttl,version,weight,priority,is_default,enabled,open_api,create_time,update_time,deleted) VALUES
	 ('resume_parse','简历解析','简历文本→结构化 JSON（2B.3 配置驱动，原 ResumeParseHandler 提示词逐字收编，字段语义对齐在线简历表单）','analysis',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'从简历原文抽取结构化JSON。字段：name,gender(男/女),birthDate(yyyy-MM-dd),phone,email,title,jobIntention{position,city,salaryMin,salaryMax,jobType,availableTime},educations[{school,major,degree,startDate(yyyy-MM),endDate(yyyy-MM),description}],works[{company,position,startDate,endDate,description}],projects[{name,role,startDate,endDate,description,url}],skills[{name,level(精通/熟练/了解)}],selfIntro。规则：只抽取原文存在的信息，缺失返回null或空数组，禁止编造。只输出JSON本体，禁止markdown代码块。

{{data:简历原文|text}}','{"text": "简历原文（外部不可信数据，数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('resume_optimize','简历优化','简历优化通用模式（管理台场景调试/开放入口；5 个 task 子任务见 resume_optimize:* 拆行；2B.3 配置驱动，原 ResumeOptimizeHandler 通用模式提示词逐字收编）','generation',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是资深简历优化顾问。结合目标岗位评估简历并给出优化建议，只输出 JSON：
{"score": 0到100整数（岗位匹配度）,
 "suggestions": ["具体可执行的优化建议，按重要性排序，3-6条"],
 "keywords": ["建议补充的关键词"],
 "optimizedText": "优化后的核心内容片段（可选，重点段落改写）"}
建议要具体到 STAR 法则、量化成果、技能匹配。禁止输出 JSON 以外内容。

{{data:简历内容|resumeText}}
{{data:目标岗位|targetPosition}}','{"resumeText": "简历内容（必填，数据通道隔离）", "targetPosition": "目标岗位（可选，空值自动丢弃）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('question_generate','智能出题','岗位+技能标签生成面试题（管理台场景调试/开放入口；JD 关键词提取见 question_generate:jd_keywords 拆行；2B.3 配置驱动，原 QuestionGenerateHandler 通用模式提示词逐字收编）','generation',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是技术面试出题专家。基于岗位和技能生成面试题，只输出 JSON：
{"questions": [{"question": "题目", "type": "八股/算法/场景/项目",
 "difficulty": "easy/medium/hard", "answer": "参考答案要点",
 "knowledgePoints": ["考察点"]}]}
题目要贴合岗位实际要求，覆盖不同层次。禁止输出 JSON 以外内容。

岗位：{{position}}
{{data:技能要求|skills}}
题量：{{count}} 道
{{data:难度|difficulty}}','{"position": "岗位（必填）", "skills": "技能标签，逗号分隔或列表（可选，空值自动丢弃）", "count": "题量，默认 5", "difficulty": "难度 easy/medium/hard（可选，空值自动丢弃）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('question_generate:jd_keywords','智能出题-JD关键词','task 拆行：JD 文本→面试考察关键词（2B.3 配置驱动，原 QuestionGenerateHandler.jd_keywords 提示词逐字收编，输出契约改 JSON 对象包装）','classification',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'从岗位JD中提取面试考察关键词。规则：
1.只提取技术栈、专业能力、业务领域三类实词；
2.每个关键词2-20个字符，保留英文原文大小写（如 Spring Boot）；
3.最多15个，按重要性降序；
4.禁止编造JD中不存在的内容。
只输出JSON对象 {"keywords": ["Java", "MySQL"]}（数组置于 keywords 字段），禁止markdown代码块。

{{data:岗位JD|context}}','{"context": "岗位 JD 原文（外部不可信数据，数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0),
	 ('article_meta','文章元信息','文章摘要/SEO 标题/SEO 描述/关键词（2B.3 配置驱动，原 ArticleMetaHandler+PortalAiController SCENES 提示词收编，输出契约改 JSON）','analysis',NULL,NULL,NULL,NULL,NULL,NULL,'defaultSceneExecutor','execute',NULL,'你是内容平台的 SEO 编辑。请根据下面的文章标题和正文，输出文章的摘要与 SEO 信息，只输出 JSON：
{"summary": "摘要，100字以内，概括文章核心内容",
 "seoTitle": "SEO标题，60字以内，包含核心关键词，比原标题更利于搜索",
 "seoDescription": "SEO描述，150字以内，吸引点击的搜索结果描述",
 "seoKeywords": "关键词，3~6个，用英文逗号分隔，不要带序号"}
禁止输出 JSON 以外内容。

文章标题：{{title}}
{{data:文章正文|content}}','{"title": "文章标题（可空）", "content": "正文纯文本（调用方截断 3000 字，数据通道隔离）"}','sync',NULL,'json',2048,0.7,30,3,NULL,100,60,NULL,0,NULL,NULL,0,3600,'v1',100,0,1,1,0,'2026-09-24 12:00:00',NULL,0);
-- 成本控制种子（AI统一网关整改 阶段三 3.3/3.4）：任务型场景（resume_parse / finance_analysis /
-- resume_optimize*）输入截断 + 输出上限；面试场景仅输出上限（context 固定 full，追问质量红线，
-- 不设输入上限）；其余场景暂不限（列默认 NULL，后续按 3.1 基线数据再定）。
-- 数值口径：3.1 基线显示开发库无真实 token 数据，本批为初始防护值，真实流量后按基线校准。
UPDATE ai_scene_config SET max_input_tokens=8000, max_output_tokens=4000, truncate_strategy='head_tail' WHERE scene_code='resume_parse' AND deleted=0;
UPDATE ai_scene_config SET max_input_tokens=6000, max_output_tokens=4000, truncate_strategy='head_tail' WHERE scene_code='finance_analysis' AND deleted=0;
UPDATE ai_scene_config SET max_input_tokens=8000, max_output_tokens=3000, truncate_strategy='head_tail' WHERE scene_code LIKE 'resume_optimize%' AND deleted=0;
UPDATE ai_scene_config SET max_output_tokens=2000 WHERE scene_code LIKE 'voice_interview%' AND deleted=0;
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
-- 岗位配置唯一来源：portal_job_template（v13.33 起合并原 portal_interview_position）
-- code/industry/level/required_skills/hot_companies/sort 六列由原岗位字典并入；
-- required_skills 驱动「简历岗位匹配评分」与「用户画像必备技能」；jd_text 用于准备页选中岗位后回填「岗位要求」。
INSERT INTO portal_job_template (name,category,position_code,code,industry,`level`,required_skills,hot_companies,sort,description,jd_text,keywords,difficulty,question_count,weights,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('初级 Java 开发工程师','技术','0111','java_backend','互联网','junior','["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]','["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]',1,'java后端 测试','初级 Java 开发工程师（0-2 年经验）
薪资范围：8K - 15K · 13薪
工作地点：深圳市南山区
学历要求：本科及以上，计算机相关专业
岗位职责：
参与公司核心业务系统的功能开发与维护
根据需求文档完成模块编码、单元测试及问题修复
配合前端完成接口联调，确保功能正常交付
编写技术文档，参与代码评审
任职要求：
熟练掌握 Java 基础，理解面向对象编程思想，熟悉集合、多线程、IO 等核心 API
了解 Spring / Spring Boot 框架，能独立完成 CRUD 功能开发
熟悉 MySQL 数据库，能编写基本的 SQL 语句，了解索引概念
了解 Redis 基本使用，知道缓存的常见应用场景
了解 HTTP 协议，能使用 Postman / Swagger 进行接口调试
具备良好的学习能力和沟通协作能力，对技术有热情
加分项：
有个人技术博客或 GitHub 开源项目
了解 Docker 基本操作
有实习或校招项目经验','Java,Spring,SpringBoot,MyBatis,MySQL,Redis,并发编程','easy',10,'{"job":40,"resume":30,"weak":20,"random":10}','active','admin','2026-08-19 18:01:40','','2026-08-19 18:01:40','','0'),
	 ('中级 Java 开发工程师（2-5 年经验）','技术','0222','java_backend_mid','互联网','mid','["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]','["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]',2,'中级 Java 开发工程师（（2-5 年经验）','中级 Java 开发工程师（2-5 年经验）
薪资范围：18K - 30K · 14-16薪
工作地点：深圳市南山区

岗位职责：
1. 独立负责业务模块的设计、开发与上线，对交付质量负责
2. 参与系统性能优化、慢查询治理与线上故障定位
3. 参与技术方案评审，输出设计文档

任职要求：
1. 本科及以上学历，2-5 年 Java 后端开发经验
2. 熟悉 JVM 内存模型与 GC 调优，具备并发编程实战经验
3. 熟悉 MySQL 索引优化与事务隔离级别；熟悉 Redis 缓存设计与穿透/雪崩防护
4. 熟悉消息队列（Kafka/RocketMQ）使用场景与可靠投递
5. 了解分布式与微服务（Spring Cloud/Dubbo）相关组件','Java,JVM,并发编程,MySQL,Redis,消息队列,分布式,微服务','medium',8,'{"job":40,"resume":30,"weak":20,"random":10}','active','admin','2026-08-19 18:01:40','','2026-08-19 18:01:40','','0'),
	 ('高级 / 资深 Java 开发工程师（5 年以上经验）','技术','0333','java_backend_senior','互联网','senior','["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]','["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]',3,'高级 / 资深 Java 开发工程师（5 年以上经验）','薪资范围：35K - 60K · 15-18薪 + 期权
工作地点：深圳市南山区
学历要求：本科及以上，计算机相关专业
岗位职责：
负责核心系统的架构设计与技术选型，主导重大技术难题攻关
规划系统演进方向，推动技术架构升级，保障系统高可用、高并发、高扩展
主导代码评审与技术规范制定，提升团队整体技术水平
跨部门技术协作，与产品、前端、测试、运维等团队高效配合
跟踪行业技术趋势，引入新技术提升研发效率和系统质量
任职要求：
精通 Java 技术栈，深入理解 JVM 原理，有丰富的线上性能调优和故障排查经验
精通 Spring 全家桶，深入理解 Spring 核心原理（IOC / AOP / 事务机制等），阅读过核心源码
精通 MySQL 数据库设计与优化，有分库分表、读写分离实战经验
精通 Redis，深入理解其数据结构、持久化机制、集群方案，有大规模缓存架构设计经验
精通至少一种消息队列，有海量消息场景下的架构设计与调优经验
深入理解分布式系统理论（CAP / BASE），有分布式事务、服务治理、链路追踪等落地经验
熟悉 Elasticsearch 集群架构与调优，有亿级数据搜索场景经验
熟悉云原生技术栈（Docker / K8s / Service Mesh），有容器化部署和运维经验
具备优秀的系统设计能力，能独立输出高质量的技术方案文档
具备技术领导力，能带领 5 人以上技术团队完成复杂项目交付
加分项：
有从 0 到 1 搭建中台或核心系统的经验
有开源项目贡献或核心技术专利
有大型互联网公司（BAT / TMD 等）工作背景
在 QCon / ArchSummit 等技术大会做过分享','Java,架构设计,高并发,JVM调优,分布式,微服务,分库分表,稳定性','hard',5,'{"job":40,"resume":30,"weak":20,"random":10}','active','admin','2026-08-19 18:01:40','','2026-08-19 18:01:40','','0'),
	 ('前端工程师','技术','','frontend','互联网','mid','["JavaScript","TypeScript","Vue","React","HTML","CSS","Node.js","Webpack","Vite","性能优化","浏览器原理","HTTP"]','["阿里","腾讯","字节跳动","美团","京东","百度","网易","小米","Shopee","滴滴"]',4,'前端工程师岗位，重点考察 JS/TS 基础、Vue/React 框架、工程化、浏览器原理、性能优化、HTTP 与网络','前端工程师
薪资范围：18K - 32K · 14-16薪
工作地点：深圳市南山区

岗位职责：
1. 负责公司 Web 端产品的开发与迭代，保障交互体验与性能
2. 参与前端工程化建设（构建、组件库、规范与自动化）
3. 与后端协作完成接口联调与线上问题排查

任职要求：
1. 本科及以上学历，2 年以上前端开发经验
2. 扎实的 JavaScript / TypeScript 基础，熟悉 ES6+ 与异步编程
3. 熟练掌握 Vue 或 React 其一，理解其响应式与渲染机制
4. 熟悉 Webpack / Vite 构建原理与常用优化手段
5. 熟悉浏览器渲染原理、HTTP 缓存与前端性能优化（LCP/CLS 等指标）','JavaScript,TypeScript,Vue,React,工程化,浏览器原理,性能优化,HTTP','medium',8,'{"job":40,"resume":30,"weak":20,"random":10}','active','admin','2026-08-19 18:01:40','','2026-08-19 18:01:40','原 portal_interview_position 迁入','0'),
	 ('算法工程师','技术','','algorithm','互联网','mid','["算法","数据结构","动态规划","图论","字符串","数组","链表","树","递归","排序","机器学习","深度学习","数学"]','["阿里","腾讯","字节跳动","百度","美团","快手","小红书","华为","商汤","旷视"]',5,'算法工程师岗位，重点考察数据结构与算法、动态规划、图论、字符串算法、机器学习与深度学习基础','算法工程师
薪资范围：25K - 45K · 15-16薪
工作地点：深圳市南山区

岗位职责：
1. 负责推荐/搜索/NLP 等方向算法模型的设计、训练与上线
2. 结合业务指标持续迭代模型效果，完成 A/B 实验与归因分析
3. 参与特征工程与数据链路建设

任职要求：
1. 硕士及以上学历，计算机/数学/统计相关专业
2. 扎实的数据结构与算法基础，熟悉动态规划、图论与字符串算法
3. 熟悉机器学习常用模型与评估指标，了解深度学习基本原理
4. 熟练使用 Python 及主流框架（PyTorch/TensorFlow）
5. 有推荐、搜索或 NLP 相关项目经验者优先','算法,数据结构,动态规划,图论,机器学习,深度学习,数学','medium',8,'{"job":40,"resume":30,"weak":20,"random":10}','active','admin','2026-08-19 18:01:40','','2026-08-19 18:01:40','原 portal_interview_position 迁入','0');

-- 简历解析配置（规则解析词表；表为空时 ResumeRuleParser 使用内置默认词典兜底）
-- v13.38：章节标题词典决定「大类划分」；技能词域无需铺满（引擎自动聚合 portal_job_template.required_skills）
INSERT INTO portal_resume_parse_config (config_type,item_key,item_name,keywords,sort,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 ('section','basic','基本信息','基本信息,个人信息,个人资料,基本资料',1,'active','admin','2026-09-29 00:00:00','',NULL,'简历顶部基础信息区','0'),
	 ('section','intention','求职意向','求职意向,求职目标,职业意向,期望职位,期望岗位,目标岗位',2,'active','admin','2026-09-29 00:00:00','',NULL,'求职意向区（抽取 position/city）','0'),
	 ('section','edu','教育背景','教育背景,教育经历,学习经历,教育信息,学历信息,教育与培训',3,'active','admin','2026-09-29 00:00:00','',NULL,'教育经历区（条目：学校/专业/学历/起止时间）','0'),
	 ('section','work','工作经历','工作经历,工作经验,职业经历,实习经历,工作履历,职业背景',4,'active','admin','2026-09-29 00:00:00','',NULL,'工作经历区（条目：公司/职位/起止时间）','0'),
	 ('section','project','项目经历','项目经历,项目经验,项目实践,项目业绩,主要项目',5,'active','admin','2026-09-29 00:00:00','',NULL,'项目经历区（条目：项目名/角色/起止时间）','0'),
	 ('section','skill','专业技能','专业技能,技能特长,技能清单,掌握技能,技能专长,IT技能,计算机技能',6,'active','admin','2026-09-29 00:00:00','',NULL,'技能区（词域匹配，见 config_type=skill）','0'),
	 ('section','self','自我评价','自我评价,个人评价,自我介绍,个人简介,自我描述,个人优势',7,'active','admin','2026-09-29 00:00:00','',NULL,'自评区（整块取，几乎不会错）','0'),
	 ('section','other','其他区块','荣誉奖项,获奖情况,证书,资格证书,校园经历,校内职务,培训经历,语言能力,兴趣爱好',8,'active','admin','2026-09-29 00:00:00','',NULL,'不解析字段，但原文保留在大类块中（内容永不丢失）','0'),
	 ('skill','通用技能','通用技能','Git,SVN,Maven,Gradle,Linux,Shell,Nginx,Tomcat,JUnit,Postman,Swagger,Figma,Axure,Visio,Office,Excel,PPT,数据分析,需求分析,项目管理,敏捷开发,Scrum,单元测试,性能优化,系统设计,微服务,分布式,高并发,负载均衡,消息队列,缓存,容器化,持续集成',1,'active','admin','2026-09-29 00:00:00','',NULL,'通识技能（与岗位必备技能并集使用）','0'),
	 ('degree','学历层级','学历层级','博士,博士研究生,硕士,硕士研究生,研究生,MBA,本科,学士,大学本科,大专,专科,高职,中专,高中',1,'active','admin','2026-09-29 00:00:00','',NULL,'按顺序优先匹配，长词优先','0');
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
	 ('语音面试默认面试官AgentID','voice.interview.defaultAgentId',(SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1),NULL,'Y','admin','2026-09-07 09:15:33','',NULL,'语音面试绑定的 ai_agent 主键；编辑「AI模块→智能体管理」对应 agent 的人设/提示词/模型即动态生效','0'),
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
	 (1,'垃圾内容','spam','cms_report_type','','warning','Y','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：spam','0'),
	 (2,'不当内容','inappropriate','cms_report_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：inappropriate','0'),
	 (3,'侵权内容','infringement','cms_report_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：infringement','0'),
	 (4,'欺诈行为','fraud','cms_report_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：fraud','0'),
	 (5,'其他问题','other','cms_report_type','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'举报类型：other','0'),
	 (1,'待处理','pending','cms_handle_status','','warning','Y','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：pending','0'),
	 (2,'处理中','processing','cms_handle_status','','primary','N','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：processing','0'),
	 (3,'已解决','resolved','cms_handle_status','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：resolved','0'),
	 (4,'已驳回','rejected','cms_handle_status','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'处理状态：rejected','0');
-- v14.03：反馈类型字典此前"有类型无数据"，门户「我的反馈」只能写死枚举；补齐 4 行（与 MyFeedbackPage/ReportFeedback 口径一致）
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (1,'功能建议','suggestion','cms_feedback_type','','primary','N','0','admin','2026-10-01 00:00:00','',NULL,'反馈类型：suggestion','0'),
	 (2,'Bug反馈','bug','cms_feedback_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'反馈类型：bug','0'),
	 (3,'体验问题','experience','cms_feedback_type','','warning','N','0','admin','2026-10-01 00:00:00','',NULL,'反馈类型：experience','0'),
	 (4,'其他','other','cms_feedback_type','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'反馈类型：other','0');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (1,'算法','algorithm','portal_question_type','','primary','Y','0','admin','2026-10-01 00:00:00','',NULL,'题目类型：algorithm','0'),
	 (2,'八股','bagwen','portal_question_type','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'题目类型：bagwen','0'),
	 (3,'系统设计','system_design','portal_question_type','','warning','N','0','admin','2026-10-01 00:00:00','',NULL,'题目类型：system_design','0'),
	 (4,'项目','project','portal_question_type','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'题目类型：project','0'),
	 (5,'HR','hr','portal_question_type','','danger','N','0','admin','2026-10-01 00:00:00','',NULL,'题目类型：hr','0'),
	 (1,'草稿','draft','cms_contest_status','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'征文状态：draft','0'),
	 (2,'征稿中','collecting','cms_contest_status','','success','Y','0','admin','2026-10-01 00:00:00','',NULL,'征文状态：collecting','0'),
	 (3,'投票中','voting','cms_contest_status','','warning','N','0','admin','2026-10-01 00:00:00','',NULL,'征文状态：voting','0'),
	 (4,'已结束','ended','cms_contest_status','','default','N','0','admin','2026-10-01 00:00:00','',NULL,'征文状态：ended','0'),
	 (1,'网络小说','novel','portal_book_type','','primary','Y','0','admin','2026-10-01 00:00:00','',NULL,'书籍类型：novel','0'),
	 (2,'长文文章','longform','portal_book_type','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'书籍类型：longform','0'),
	 (3,'出版书籍','published','portal_book_type','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'书籍类型：published','0'),
	 (1,'连载中','ongoing','portal_book_serial_status','','warning','Y','0','admin','2026-10-01 00:00:00','',NULL,'连载状态：ongoing','0'),
	 (2,'已完结','completed','portal_book_serial_status','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'连载状态：completed','0'),
	 (3,'暂停更新','hiatus','portal_book_serial_status','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'连载状态：hiatus','0'),
	 (1,'打赏','tip','portal_wallet_txn_type','','primary','N','0','admin','2026-10-01 00:00:00','',NULL,'流水类型：tip','0'),
	 (2,'提现','withdraw','portal_wallet_txn_type','','warning','N','0','admin','2026-10-01 00:00:00','',NULL,'流水类型：withdraw','0'),
	 (3,'会员','vip','portal_wallet_txn_type','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'流水类型：vip（后端实际写入值）','0'),
	 (1,'待复习','wrong','portal_wrong_question_status','','warning','Y','0','admin','2026-10-01 00:00:00','',NULL,'错题状态：wrong','0'),
	 (2,'已掌握','mastered','portal_wrong_question_status','','success','N','0','admin','2026-10-01 00:00:00','',NULL,'错题状态：mastered','0');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
		 (2,'首页-VIP推广位','home_vip_banner','portal_ad_slot_key','','primary','N','0','admin','2026-08-28 13:16:38','',NULL,'首页右侧/移动端下方的VIP推广位','0');
-- v13.99：门户文章详情页实际渲染的两个广告位此前**未登记**到 portal_ad_slot_key 字典，
-- 而后台广告管理的"广告位"下拉正是取自该字典 ⇒ 运营**无法为这两个位置投放广告**。
-- 补登记（独立 INSERT 批次，不动既有 home_* 两行）。
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (3,'文章详情-侧边栏','article_detail_sidebar','portal_ad_slot_key','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'文章详情页侧边栏广告位（门户 AdCard 实际使用）','0'),
	 (4,'文章详情-正文下方','article_detail_bottom','portal_ad_slot_key','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'文章详情页正文下方广告位（门户 AdCard 实际使用）','0');
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
		 (1,'简单','easy','portal_question_difficulty','','success','Y','0','admin','2026-09-29 00:00:00','',NULL,'题库难度：easy','0'),
		 (2,'中等','medium','portal_question_difficulty','','warning','N','0','admin','2026-09-29 00:00:00','',NULL,'题库难度：medium','0'),
		 (3,'困难','hard','portal_question_difficulty','','danger','N','0','admin','2026-09-29 00:00:00','',NULL,'题库难度：hard','0');
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

-- v14.28：列表页/搜索页侧栏广告位登记（原为写死推广卡，改由广告位体系投放）
INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,css_class,list_class,is_default,status,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
	 (5,'文章列表-侧边栏','article_list_sidebar','portal_ad_slot_key','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'广告位：列表页右侧栏','0'),
	 (6,'搜索结果-侧边栏','search_sidebar','portal_ad_slot_key','','info','N','0','admin','2026-10-01 00:00:00','',NULL,'广告位：搜索页右侧栏','0');
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
select * from vip_api_registry;
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
