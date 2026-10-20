# 墨云项目 · 数据库表清单与初始化口径（基于 moyun-db2 实际库）

> 逐表记录 `moyun-db2` 中每张表的模块归属、表注释、源库行数、是否进入 DDL / DML 及原因。
> 数量核对：源库 **205** 张表 = 进入 DDL **200** 张 + 线上备份表 **5** 张（`_bak_*` / `*_bak_*`）。
> 行数取自 `COUNT(*)` 实测值（非统计估算）。

## 一、统计总览

| 模块 / 分节 | DDL 顺序 | 表数 | 进入 DDL | 进入 DML |
| --- | --- | --- | --- | --- |
| 系统基础 sys_/gen_ | 1 | 25 | 25 | 14 |
| AI ai_ | 2 | 31 | 31 | 9 |
| 门户内容 portal_ | 3 | 88 | 88 | 12 |
| 记账 ledger_ | 4 | 28 | 28 | 6 |
| 已下线归档 vip_/pay_ | 5 | 16 | 16 | 0 |
| 定时任务 sys_job + qrtz_ | 6 | 12 | 12 | 1 |
| 备份表（不纳入） | 7 | 5 | 0 | 0 |

## 二、逐表明细

> 排序即脚本内建表顺序（模块优先级：系统基础 → AI → 门户内容 → 记账 → 已下线归档 → 定时任务），定时任务含外键故置尾。

| # | 表名 | 模块 / 分节 | 表注释 | 源库行数 | 导入行数 | DDL | DML 文件 | 说明 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `sys_platform` | 系统基础 sys_/gen_ | 端定义（全局公共，用户/支付/VIP/配置/菜单/统计统一引用） | 4 | 4 | 建 | `zyg_dml_01_sys_base.sql` | portal/ledger/admin/personality 四端定义 |
| 2 | `sys_dept` | 系统基础 sys_/gen_ | 部门表 | 10 | 10 | 建 | `zyg_dml_01_sys_base.sql` | 系统预设数据，随初始化导入 |
| 3 | `sys_post` | 系统基础 sys_/gen_ | 岗位信息表 | 4 | 4 | 建 | `zyg_dml_01_sys_base.sql` | 系统预设数据，随初始化导入 |
| 4 | `sys_role` | 系统基础 sys_/gen_ | 角色信息表 | 2 | 2 | 建 | `zyg_dml_01_sys_base.sql` | 系统预设数据，随初始化导入 |
| 5 | `sys_menu` | 系统基础 sys_/gen_ | 菜单权限表 | 402 | 402 | 建 | `zyg_dml_02_sys_menu.sql` | 目录/菜单/按钮 402 条，后台导航与按钮鉴权的唯一来源 |
| 6 | `sys_role_menu` | 系统基础 sys_/gen_ | 角色和菜单关联表 | 402 | 402 | 建 | `zyg_dml_02_sys_menu.sql` | 超级管理员全量授权 402 条 |
| 7 | `sys_user` | 系统基础 sys_/gen_ | 用户信息表 | 2 | 2 | 建 | `zyg_dml_01_sys_base.sql` | admin(超管) + ry；密码为 RuoYi 默认 admin123 的 BCrypt |
| 8 | `sys_user_role` | 系统基础 sys_/gen_ | 用户和角色关联表 | 2 | 2 | 建 | `zyg_dml_01_sys_base.sql` | 系统预设数据，随初始化导入 |
| 9 | `sys_user_post` | 系统基础 sys_/gen_ | 用户与岗位关联表 | 2 | 2 | 建 | `zyg_dml_01_sys_base.sql` | 系统预设数据，随初始化导入 |
| 10 | `sys_role_dept` | 系统基础 sys_/gen_ | 角色和部门关联表 | 3 | 3 | 建 | `zyg_dml_01_sys_base.sql` | 系统预设数据，随初始化导入 |
| 11 | `sys_dict_type` | 系统基础 sys_/gen_ | 字典类型表 | 44 | 43 | 建 | `zyg_dml_01_sys_base.sql` | 43 类字典（已剔除测试字典 ledger_test_tree） |
| 12 | `sys_dict_data` | 系统基础 sys_/gen_ | 字典数据表 | 124 | 119 | 建 | `zyg_dml_01_sys_base.sql` | 119 条字典项，页面下拉框/多选框数据源（已剔除测试字典数据） |
| 13 | `sys_config` | 系统基础 sys_/gen_ | 参数配置表 | 19 | 19 | 建 | `zyg_dml_01_sys_base.sql` | 19 项系统参数；ledger.schedule.appBaseUrl 已改为 localhost |
| 14 | `sys_config_log` | 系统基础 sys_/gen_ | 参数配置变更日志表（sys_config 审计留痕） | 0 | 0 | 建 | — | 系统日志表，不纳入初始化 |
| 15 | `sys_notification` | 系统基础 sys_/gen_ | 系统通知主体表（合并 portal_notification + sys_notice） | 0 | 0 | 建 | — | 系统通知主体表，当前为空，无预设数据 |
| 16 | `sys_notification_read` | 系统基础 sys_/gen_ | 系统通知用户已读关系表 | 0 | 0 | 建 | — | 通知已读关系表，当前为空，无预设数据 |
| 17 | `sys_audit_task` | 系统基础 sys_/gen_ | 统一审核任务表（v8.1） | 0 | 0 | 建 | — | 统一审核任务表，当前为空，无预设数据 |
| 18 | `sys_sensitive_word` | 系统基础 sys_/gen_ | 敏感词库 | 7 | 7 | 建 | `zyg_dml_01_sys_base.sql` | 内容审核预设敏感词 |
| 19 | `sys_sensitive_word_log` | 系统基础 sys_/gen_ | 敏感词命中记录 | 0 | 0 | 建 | — | 系统日志表，不纳入初始化 |
| 20 | `sys_file` | 系统基础 sys_/gen_ | 文件管理表 | 17 | 0 | 建 | — | 文件记录属运行时数据，需随对象存储一起迁移，不纳入初始化 |
| 21 | `sys_job_log` | 系统基础 sys_/gen_ | 定时任务调度日志表 | 34931 | 0 | 建 | — | 系统日志表，不纳入初始化 |
| 22 | `sys_logininfor` | 系统基础 sys_/gen_ | 系统访问记录 | 75 | 0 | 建 | — | 系统日志表，不纳入初始化 |
| 23 | `sys_oper_log` | 系统基础 sys_/gen_ | 操作日志记录 | 69 | 0 | 建 | — | 系统日志表，不纳入初始化 |
| 24 | `gen_table` | 系统基础 sys_/gen_ | 代码生成业务表 | 0 | 0 | 建 | — | 代码生成器元数据，空表 |
| 25 | `gen_table_column` | 系统基础 sys_/gen_ | 代码生成业务表字段 | 0 | 0 | 建 | — | 代码生成器元数据，空表 |
| 26 | `ai_provider` | AI ai_ | AI 提供商注册表 | 5 | 5 | 建 | `zyg_dml_03_ai.sql` | 系统预设数据，随初始化导入 |
| 27 | `ai_model_config` | AI ai_ | 模型配置表 | 4 | 4 | 建 | `zyg_dml_03_ai.sql` | api_key 为 ENC: 密文，换环境需重录密钥 |
| 28 | `ai_agent` | AI ai_ | 智能体表 | 2 | 2 | 建 | `zyg_dml_03_ai.sql` | 默认智能体；id 显式指定，被 ai_scene_config.agent_id 引用 |
| 29 | `ai_agent_tool` | AI ai_ | 智能体工具定义表 | 8 | 8 | 建 | `zyg_dml_03_ai.sql` | 系统预设数据，随初始化导入 |
| 30 | `ai_agent_tool_relation` | AI ai_ | 智能体工具关联表 | 15 | 15 | 建 | `zyg_dml_03_ai.sql` | 系统预设数据，随初始化导入 |
| 31 | `ai_agent_dictionary_relation` | AI ai_ | 智能体词典关联表 | 8 | 8 | 建 | `zyg_dml_03_ai.sql` | 系统预设数据，随初始化导入 |
| 32 | `ai_agent_workflow_relation` | AI ai_ | 智能体-工作流关联表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 33 | `ai_domain_dictionary` | AI ai_ | 领域词典表 | 17 | 17 | 建 | `zyg_dml_03_ai.sql` | 系统预设数据，随初始化导入 |
| 34 | `ai_scene_config` | AI ai_ | AI场景配置表（业务场景与Agent/模型/知识库/工作流动态绑定） | 25 | 25 | 建 | `zyg_dml_03_ai.sql` | 25 个业务场景（Agent/模型/提示词/输出契约） |
| 35 | `ai_scene_config_history` | AI ai_ | AI场景配置版本快照表（保存自动快照，一键回滚） | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 36 | `ai_knowledge_library` | AI ai_ | 知识库主表 | 1 | 0 | 建 | — | 知识库内容表：仅建结构，按要求不迁数据 |
| 37 | `ai_knowledge_library_config` | AI ai_ | 知识库配置表 | 3 | 0 | 建 | — | 知识库内容表：仅建结构，按要求不迁数据 |
| 38 | `ai_knowledge_base` | AI ai_ | 知识库表 | 6 | 0 | 建 | — | 知识库内容表：仅建结构，按要求不迁数据 |
| 39 | `ai_knowledge_config` | AI ai_ | 知识库配置表 - 包含分片策略、文档类型识别、预处理规则等配置 | 6 | 0 | 建 | — | 知识库内容表：仅建结构，按要求不迁数据 |
| 40 | `ai_knowledge_config_template` | AI ai_ | 知识库配置模板表 | 5 | 5 | 建 | `zyg_dml_03_ai.sql` | 仅 5 行 is_system 配置模板（可选，不属知识库内容） |
| 41 | `ai_document_segment` | AI ai_ | 文档分片表 | 88 | 0 | 建 | — | 知识库内容表：仅建结构，按要求不迁数据 |
| 42 | `ai_document_image` | AI ai_ | 文档图片表 | 0 | 0 | 建 | — | 知识库内容表：仅建结构，按要求不迁数据 |
| 43 | `ai_datasource_config` | AI ai_ | 数据源配置表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 44 | `ai_table_metadata` | AI ai_ | 表元数据缓存表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 45 | `ai_workflow` | AI ai_ | 工作流定义表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 46 | `ai_workflow_version` | AI ai_ | 工作流版本表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 47 | `ai_workflow_execution` | AI ai_ | 工作流执行记录表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 48 | `ai_conversation` | AI ai_ | 对话会话表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 49 | `ai_conversation_message` | AI ai_ | 对话消息表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 50 | `ai_chat_history` | AI ai_ | 对话历史表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 51 | `ai_query_history` | AI ai_ | 查询历史表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 52 | `ai_reference_feedback` | AI ai_ | 参考来源反馈表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 53 | `ai_execute_log` | AI ai_ | AI能力调用日志表（可观测性） | 57 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 54 | `ai_tool_call_log` | AI ai_ | 工具调用日志表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 55 | `ai_token_usage_log` | AI ai_ | Token使用记录表 | 13 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 56 | `ai_token_usage_summary` | AI ai_ | Token使用统计汇总表 | 0 | 0 | 建 | — | AI 日志/会话记录/空表，不纳入初始化 |
| 57 | `portal_category` | 门户内容 portal_ | 门户分类表 | 49 | 49 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 58 | `portal_article` | 门户内容 portal_ | 门户文章表 | 1 | 0 | 建 | — | 空表，无预设数据 |
| 59 | `portal_article_version` | 门户内容 portal_ | 文章版本快照 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 60 | `portal_article_view` | 门户内容 portal_ | 文章浏览记录表 | 3 | 0 | 建 | — | 空表，无预设数据 |
| 61 | `portal_tag` | 门户内容 portal_ | 门户标签表 | 38 | 38 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 62 | `portal_entity_tag` | 门户内容 portal_ | 通用实体标签关联表 | 7 | 0 | 建 | — | 空表，无预设数据 |
| 63 | `portal_column` | 门户内容 portal_ | 专栏 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 64 | `portal_column_article` | 门户内容 portal_ | 专栏-文章关联 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 65 | `portal_column_subscribe` | 门户内容 portal_ | 专栏订阅 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 66 | `portal_comment` | 门户内容 portal_ | 门户评论表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 67 | `portal_comment_like` | 门户内容 portal_ | 门户评论点赞表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 68 | `portal_like` | 门户内容 portal_ | 门户点赞表（文章） | 0 | 0 | 建 | — | 空表，无预设数据 |
| 69 | `portal_bookmark` | 门户内容 portal_ | 门户收藏表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 70 | `portal_book` | 门户内容 portal_ | 书籍表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 71 | `portal_book_chapter` | 门户内容 portal_ | 书籍章节表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 72 | `portal_book_quote` | 门户内容 portal_ | 金句摘录表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 73 | `portal_book_quote_like` | 门户内容 portal_ | 金句点赞表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 74 | `portal_book_recommend` | 门户内容 portal_ | 书籍推荐位表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 75 | `portal_bookshelf` | 门户内容 portal_ | 用户书架（收藏书籍）表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 76 | `portal_book_list` | 门户内容 portal_ | 书单表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 77 | `portal_book_list_item` | 门户内容 portal_ | 书单-书籍关联表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 78 | `portal_book_list_bookmark` | 门户内容 portal_ | 书单收藏表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 79 | `portal_book_list_like` | 门户内容 portal_ | 书单点赞表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 80 | `portal_reading_preference` | 门户内容 portal_ | 用户阅读偏好表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 81 | `portal_reading_progress` | 门户内容 portal_ | 阅读进度表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 82 | `portal_topic` | 门户内容 portal_ | 话题主表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 83 | `portal_topic_post` | 门户内容 portal_ | 话题观点（楼层） | 0 | 0 | 建 | — | 空表，无预设数据 |
| 84 | `portal_topic_post_like` | 门户内容 portal_ | 话题观点点赞 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 85 | `portal_topic_comment` | 门户内容 portal_ | 话题评论（多态） | 0 | 0 | 建 | — | 空表，无预设数据 |
| 86 | `portal_topic_comment_like` | 门户内容 portal_ | 话题评论点赞 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 87 | `portal_topic_like` | 门户内容 portal_ | 话题点赞 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 88 | `portal_feed_event` | 门户内容 portal_ | 动态事件流 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 89 | `portal_feed_inbox` | 门户内容 portal_ | 动态收件箱 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 90 | `portal_follow` | 门户内容 portal_ | 门户关注表 | 1 | 0 | 建 | — | 空表，无预设数据 |
| 91 | `portal_friend_link` | 门户内容 portal_ | 门户友情链接表 | 3 | 3 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 92 | `portal_achievement` | 门户内容 portal_ | 成就定义表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 93 | `portal_user_badge` | 门户内容 portal_ | 用户徽章记录表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 94 | `portal_growth_rule` | 门户内容 portal_ | 成长规则配置表 | 31 | 31 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 95 | `portal_growth_log` | 门户内容 portal_ | 成长事件流水表 | 2 | 0 | 建 | — | 空表，无预设数据 |
| 96 | `portal_user_growth` | 门户内容 portal_ | 用户成长值总表 | 2 | 0 | 建 | — | 空表，无预设数据 |
| 97 | `portal_help_category` | 门户内容 portal_ | 帮助中心分类表 | 4 | 4 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 98 | `portal_help_article` | 门户内容 portal_ | 帮助中心文章表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 99 | `portal_import_template_config` | 门户内容 portal_ | 导入模板字段配置（动态模板） | 0 | 0 | 建 | — | 空表，无预设数据 |
| 100 | `portal_interview_category` | 门户内容 portal_ | 面试题目分类表 | 5 | 5 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 101 | `portal_interview_company` | 门户内容 portal_ | 面试公司标签表 | 3 | 3 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 102 | `portal_interview_config` | 门户内容 portal_ | 面试配置表（人设/提示词/评分权重/追问策略/自我介绍） | 1 | 1 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 103 | `portal_interview_question` | 门户内容 portal_ | 面试题目表 | 2 | 0 | 建 | — | 空表，无预设数据 |
| 104 | `portal_interview_question_company` | 门户内容 portal_ | 题目-公司关联表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 105 | `portal_interview_question_like` | 门户内容 portal_ | 题目点赞表 | 1 | 0 | 建 | — | 空表，无预设数据 |
| 106 | `portal_interview_question_test_case` | 门户内容 portal_ | 面试题目测试用例表（OJ判题） | 0 | 0 | 建 | — | 空表，无预设数据 |
| 107 | `portal_interview_experience` | 门户内容 portal_ | 面经表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 108 | `portal_interview_experience_like` | 门户内容 portal_ | 面经点赞表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 109 | `portal_interview_comment` | 门户内容 portal_ | 面经评论表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 110 | `portal_interview_comment_like` | 门户内容 portal_ | 面经评论点赞表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 111 | `portal_interview_attempt` | 门户内容 portal_ | 做题记录表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 112 | `portal_interview_bookmark` | 门户内容 portal_ | 题目收藏表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 113 | `portal_interview_submission` | 门户内容 portal_ | 题目提交记录表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 114 | `portal_interview_resume_template` | 门户内容 portal_ | 简历模板表 | 1 | 1 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 115 | `portal_interview_resume_template_like` | 门户内容 portal_ | 简历模板点赞表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 116 | `portal_job_template` | 门户内容 portal_ | 岗位模板表（JD/关键词/出题权重，支撑智能出题） | 5 | 5 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 117 | `portal_resume_parse_config` | 门户内容 portal_ | 简历解析配置（章节词典/技能词域/学历词/岗位词，规则解析用） | 10 | 10 | 建 | `zyg_dml_04_portal.sql` | 系统预设数据，随初始化导入 |
| 118 | `portal_resume_job_target` | 门户内容 portal_ | 简历优化-岗位目标表 | 1 | 0 | 建 | — | 空表，无预设数据 |
| 119 | `portal_resume_job_match` | 门户内容 portal_ | 简历优化-岗位匹配报告表 | 2 | 0 | 建 | — | 空表，无预设数据 |
| 120 | `portal_resume_score_report` | 门户内容 portal_ | 简历优化-评分报告存档表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 121 | `portal_resume_optimize_history` | 门户内容 portal_ | 简历优化-优化历史表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 122 | `portal_user_resume` | 门户内容 portal_ | 用户简历 | 2 | 0 | 建 | — | 空表，无预设数据 |
| 123 | `portal_code_run` | 门户内容 portal_ | 代码运行记录 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 124 | `portal_contest_submission` | 门户内容 portal_ | 活动投稿 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 125 | `portal_contest_vote` | 门户内容 portal_ | 活动投稿投票记录 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 126 | `portal_writing_contest` | 门户内容 portal_ | 创作挑战/征文活动 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 127 | `portal_writing_prompt` | 门户内容 portal_ | 每日写作 prompt | 2 | 0 | 建 | — | 空表，无预设数据 |
| 128 | `portal_creator_certification` | 门户内容 portal_ | 创作者认证 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 129 | `portal_shop_item` | 门户内容 portal_ | 积分商城商品表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 130 | `portal_shop_exchange` | 门户内容 portal_ | 积分兑换记录表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 131 | `portal_study_plan` | 门户内容 portal_ | 学习计划 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 132 | `portal_study_plan_log` | 门户内容 portal_ | 计划每日进度 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 133 | `portal_wrong_question` | 门户内容 portal_ | 错题本 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 134 | `portal_ad_slot` | 门户内容 portal_ | 门户自研广告位表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 135 | `portal_feedback` | 门户内容 portal_ | 用户意见反馈表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 136 | `portal_report` | 门户内容 portal_ | 用户举报记录表 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 137 | `portal_message` | 门户内容 portal_ | 私信消息 | 1 | 0 | 建 | — | 空表，无预设数据 |
| 138 | `portal_message_session` | 门户内容 portal_ | 私信会话 | 1 | 0 | 建 | — | 空表，无预设数据 |
| 139 | `portal_ai_task` | 门户内容 portal_ | 通用AI异步任务表 | 7 | 0 | 建 | — | 空表，无预设数据 |
| 140 | `portal_user` | 门户内容 portal_ | 门户用户表 | 3 | 3 | 建 | `zyg_dml_04_portal.sql` | 3 个现网门户账号（密码保留原 BCrypt 值） |
| 141 | `portal_user_stats` | 门户内容 portal_ | 门户用户统计聚合表 | 2 | 0 | 建 | — | 空表，无预设数据 |
| 142 | `portal_voice_interview` | 门户内容 portal_ | 语音面试会话主表（V10.1） | 4 | 0 | 建 | — | 空表，无预设数据 |
| 143 | `portal_voice_interview_event` | 门户内容 portal_ | AI语音面试会话事件日志（v11.88） | 49 | 0 | 建 | — | 空表，无预设数据 |
| 144 | `portal_voice_interview_qa` | 门户内容 portal_ | 语音面试问答表（V10.1，含追问链） | 16 | 0 | 建 | — | 空表，无预设数据 |
| 145 | `ledger_category` | 记账 ledger_ | 记账-分类（系统预设+用户自定义） | 62 | 51 | 建 | `zyg_dml_05_ledger.sql` | 仅 is_system=1 的 51 条系统预设；11 条用户自定义不纳入 |
| 146 | `ledger_transaction` | 记账 ledger_ | 记账-流水 | 42 | 0 | 建 | — | 空表，无预设数据 |
| 147 | `ledger_memo` | 记账 ledger_ | 记账-备忘录（待办事项） | 1 | 0 | 建 | — | 空表，无预设数据 |
| 148 | `ledger_budget` | 记账 ledger_ | 记账-预算 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 149 | `ledger_saving_plan` | 记账 ledger_ | 记账-存钱计划 | 1 | 0 | 建 | — | 空表，无预设数据 |
| 150 | `ledger_saving_record` | 记账 ledger_ | 记账-存钱流水 | 52 | 0 | 建 | — | 空表，无预设数据 |
| 151 | `ledger_asset_account` | 记账 ledger_ | 记账-资产账户 | 6 | 0 | 建 | — | 空表，无预设数据 |
| 152 | `ledger_liability_account` | 记账 ledger_ | 记账-负债账户 | 3 | 0 | 建 | — | 空表，无预设数据 |
| 153 | `ledger_net_worth_snapshot` | 记账 ledger_ | 记账-净资产每日快照 | 8 | 0 | 建 | — | 空表，无预设数据 |
| 154 | `ledger_schedule_task` | 记账 ledger_ | 记账-定时记账任务 | 14 | 14 | 建 | `zyg_dml_05_ledger.sql` | 系统预设数据，随初始化导入 |
| 155 | `ledger_schedule_log` | 记账 ledger_ | 记账-定时记账执行日志 | 27 | 0 | 建 | — | 空表，无预设数据 |
| 156 | `ledger_app_feature_config` | 记账 ledger_ | 记账小程序功能入口配置（可视化运营） | 29 | 29 | 建 | `zyg_dml_05_ledger.sql` | 系统预设数据，随初始化导入 |
| 157 | `ledger_ai_analysis_report` | 记账 ledger_ | AI 财务分析报告月度快照 | 1 | 0 | 建 | — | 空表，无预设数据 |
| 158 | `ledger_product_category` | 记账 ledger_ | 商品类目（两级树） | 18 | 18 | 建 | `zyg_dml_05_ledger.sql` | 系统预设数据，随初始化导入 |
| 159 | `ledger_product` | 记账 ledger_ | 记账-商品 | 4 | 0 | 建 | — | 空表，无预设数据 |
| 160 | `ledger_product_channel` | 记账 ledger_ | 商品可售渠道 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 161 | `ledger_warehouse` | 记账 ledger_ | 记账-仓库（个人/家庭/合伙） | 4 | 0 | 建 | — | 空表，无预设数据 |
| 162 | `ledger_warehouse_config` | 记账 ledger_ | 仓库常用配置 | 6 | 0 | 建 | — | 空表，无预设数据 |
| 163 | `ledger_warehouse_member` | 记账 ledger_ | 记账-仓库成员 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 164 | `ledger_shelf` | 记账 ledger_ | 仓库货架（存放位置） | 4 | 0 | 建 | — | 空表，无预设数据 |
| 165 | `ledger_stock_flow` | 记账 ledger_ | 记账-库存流水 | 6 | 0 | 建 | — | 空表，无预设数据 |
| 166 | `ledger_purchase_order` | 记账 ledger_ | 记账-采购单 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 167 | `ledger_purchase_order_item` | 记账 ledger_ | 记账-采购明细 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 168 | `ledger_sale_order` | 记账 ledger_ | 记账-销售单 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 169 | `ledger_sale_order_item` | 记账 ledger_ | 记账-销售明细 | 0 | 0 | 建 | — | 空表，无预设数据 |
| 170 | `ledger_sale_channel` | 记账 ledger_ | 售卖渠道 | 7 | 7 | 建 | `zyg_dml_05_ledger.sql` | 系统预设数据，随初始化导入 |
| 171 | `ledger_source_channel` | 记账 ledger_ | 进货渠道 | 5 | 5 | 建 | `zyg_dml_05_ledger.sql` | 系统预设数据，随初始化导入 |
| 172 | `ledger_invite` | 记账 ledger_ | 邀请码 | 1 | 0 | 建 | — | 空表，无预设数据 |
| 173 | `vip_tier` | 已下线归档 vip_/pay_ | VIP等级（一端一套，类比 sys_role） | 6 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 174 | `vip_tier_benefit` | 已下线归档 vip_/pay_ | 等级权益关联（类比 sys_role_menu） | 24 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 175 | `vip_benefit` | 已下线归档 vip_/pay_ | VIP权益定义（类比 sys_menu） | 8 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 176 | `vip_benefit_usage` | 已下线归档 vip_/pay_ | 权益使用记录（consume=true 的次数统计） | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 177 | `vip_user_card` | 已下线归档 vip_/pay_ | 用户会员卡（类比 sys_user_role，一端一卡续费顺延） | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 178 | `vip_api_registry` | 已下线归档 vip_/pay_ | VIP接口注册表（@VipOnly 启动扫描生成） | 4 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 179 | `pay_order` | 已下线归档 vip_/pay_ | 支付订单（公共支付通道） | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 180 | `pay_notify_log` | 已下线归档 vip_/pay_ | 渠道回调日志 | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 181 | `pay_notification` | 已下线归档 vip_/pay_ | 支付站内通知 | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 182 | `pay_user_account` | 已下线归档 vip_/pay_ | 用户资金账户（钱包） | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 183 | `pay_user_bank_card` | 已下线归档 vip_/pay_ | 用户银行卡（密文落库） | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 184 | `pay_withdraw_order` | 已下线归档 vip_/pay_ | 提现订单（预留） | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 185 | `pay_ledger_entry` | 已下线归档 vip_/pay_ | 分账流水（复式记账） | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 186 | `portal_tip_order` | 已下线归档 vip_/pay_ | 打赏订单（复用为付费阅读购买记录，target_type=article_paid） | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 187 | `ledger_tip_order` | 已下线归档 vip_/pay_ | 记账-打赏记录 | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 188 | `portal_creator_settlement` | 已下线归档 vip_/pay_ | 创作者分成结算 | 0 | 0 | 建 | — | 2026-10 下线，无代码引用：DDL 保留结构，DML 不导入 |
| 189 | `sys_job` | 定时任务 sys_job + qrtz_ | 定时任务调度表 | 12 | 12 | 建 | `zyg_dml_06_job.sql` | 12 个定时任务；qrtz_* 由应用启动按本表重建 |
| 190 | `qrtz_job_details` | 定时任务 sys_job + qrtz_ | 任务详细信息表 | 12 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 191 | `qrtz_triggers` | 定时任务 sys_job + qrtz_ | 触发器详细信息表 | 12 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 192 | `qrtz_blob_triggers` | 定时任务 sys_job + qrtz_ | Blob类型的触发器表 | 0 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 193 | `qrtz_simple_triggers` | 定时任务 sys_job + qrtz_ | 简单触发器的信息表 | 0 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 194 | `qrtz_cron_triggers` | 定时任务 sys_job + qrtz_ | Cron类型的触发器表 | 12 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 195 | `qrtz_simprop_triggers` | 定时任务 sys_job + qrtz_ | 同步机制的行锁表 | 0 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 196 | `qrtz_calendars` | 定时任务 sys_job + qrtz_ | 日历信息表 | 0 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 197 | `qrtz_fired_triggers` | 定时任务 sys_job + qrtz_ | 已触发的触发器表 | 0 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 198 | `qrtz_locks` | 定时任务 sys_job + qrtz_ | 存储的悲观锁信息表 | 2 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 199 | `qrtz_paused_trigger_grps` | 定时任务 sys_job + qrtz_ | 暂停的触发器表 | 0 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 200 | `qrtz_scheduler_state` | 定时任务 sys_job + qrtz_ | 调度器状态表 | 3 | 0 | 建 | — | Quartz 运行态，应用启动按 sys_job 重建，不导入 |
| 201 | `_bak_portal_category` | 备份表（不纳入） | （无注释） | 50 | 0 | 不建 | — | 线上备份表：DDL/DML 均不纳入，请单独备份归档 |
| 202 | `ai_scene_config_bak_v1338` | 备份表（不纳入） | （无注释） | 20 | 0 | 不建 | — | 线上备份表：DDL/DML 均不纳入，请单独备份归档 |
| 203 | `ai_scene_config_bak_agent_id_v1338` | 备份表（不纳入） | （无注释） | 20 | 0 | 不建 | — | 线上备份表：DDL/DML 均不纳入，请单独备份归档 |
| 204 | `ledger_stock_bak_20261110` | 备份表（不纳入） | 记账-库存 | 7 | 0 | 不建 | — | 线上备份表：DDL/DML 均不纳入，请单独备份归档 |
| 205 | `sys_config_bak_v1338` | 备份表（不纳入） | （无注释） | 12 | 0 | 不建 | — | 线上备份表：DDL/DML 均不纳入，请单独备份归档 |

## 三、口径说明

1. **DML 只放"系统预设"**：字典、菜单与授权、账号体系、参数、AI 供应商/模型/智能体/工具/词典/场景、分类/标签/模板/规则/渠道、定时任务、门户账号。
2. **用户产生的业务数据不迁**：记账流水、账户、库存、计划，门户文章/评论/点赞/简历/面试记录/消息等，均属于运行数据。
3. **日志与记录表不迁**：`sys_*_log`、`sys_logininfor`、`sys_oper_log`、`ai_*_log`、`ai_conversation*`、`qrtz_*`（Quartz 运行态）。
4. **知识库内容不迁**（按需求）：只保留 `ai_knowledge_*` / `ai_document_*` 的建表结构。
5. **已下线模块保留结构**：`vip_*` / `pay_*` / 打赏 / 创作者结算共 16 张表，2026-10 全站免费化后无代码引用。
6. **备份表不纳入**：`sys_config_bak_v1338`、`ai_scene_config_bak_v1338`、`ai_scene_config_bak_agent_id_v1338`、`ledger_stock_bak_20261110`、`_bak_portal_category`。