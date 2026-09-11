create table if not exists `moyun-db`.ai_agent_tool
(
    id              bigint auto_increment comment '工具ID'
        primary key,
    name            varchar(100)                          not null comment '工具标识（英文）',
    display_name    varchar(100)                          not null comment '显示名称（中文）',
    description     text                                  not null comment '工具描述（给LLM理解用）',
    category        varchar(50) default 'general'         null comment '工具分类：general/information/utility/action/data',
    tool_type       varchar(20)                           not null comment '工具类型：builtin/http/database',
    icon            varchar(50) default 'fa-wrench'       null comment '图标（FontAwesome）',
    config          json                                  null comment '工具配置（API地址、认证信息等）',
    parameters      json                                  not null comment '参数定义（JSON Schema格式）',
    timeout_seconds int         default 30                null comment '超时时间（秒）',
    enabled         tinyint(1)  default 1                 null comment '是否启用',
    is_system       tinyint(1)  default 0                 null comment '是否系统内置',
    create_time     datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         tinyint(1)  default 0                 not null comment '删除标记: 0-未删除, 1-已删除',
    constraint uk_name
        unique (name)
)
    comment '智能体工具定义表' charset = utf8mb4
                               row_format = DYNAMIC;

create index idx_category
    on `moyun-db`.ai_agent_tool (category);

create index idx_deleted
    on `moyun-db`.ai_agent_tool (deleted);

create index idx_enabled
    on `moyun-db`.ai_agent_tool (enabled);

create table if not exists `moyun-db`.ai_analysis_report
(
    id                bigint auto_increment comment '主键ID'
        primary key,
    datasource_id     bigint                                not null comment '数据源ID',
    user_id           bigint                                null comment '用户ID',
    report_name       varchar(200)                          not null comment '报告名称',
    report_type       varchar(50) default 'auto'            null comment '报告类型: auto, custom, scheduled',
    table_name        varchar(200)                          null comment '分析的表名',
    analysis_config   text                                  null comment '分析配置(JSON格式)',
    executive_summary text                                  null comment '执行摘要(AI生成)',
    data_overview     text                                  null comment '数据概览(JSON格式)',
    analysis_results  longtext                              null comment '分析结果(JSON格式)',
    insights          text                                  null comment '数据洞察(JSON格式)',
    charts            text                                  null comment '图表配置(JSON格式)',
    conclusion        text                                  null comment '结论与建议(AI生成)',
    report_status     varchar(20) default 'draft'           null comment '报告状态: draft, completed, archived',
    file_path         varchar(500)                          null comment '导出文件路径',
    generate_time     int         default 0                 null comment '生成耗时(秒)',
    view_count        int         default 0                 null comment '查看次数',
    create_time       datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time       datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '分析报告表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_create_time
    on `moyun-db`.ai_analysis_report (create_time);

create index idx_datasource_id
    on `moyun-db`.ai_analysis_report (datasource_id);

create index idx_report_type
    on `moyun-db`.ai_analysis_report (report_type);

create index idx_user_id
    on `moyun-db`.ai_analysis_report (user_id);

create table if not exists `moyun-db`.ai_chart_recommendation_rule
(
    id                   bigint auto_increment comment '主键ID'
        primary key,
    rule_name            varchar(100)                         not null comment '规则名称',
    data_pattern         varchar(100)                         not null comment '数据模式: time_series, distribution, category, correlation',
    field_types          varchar(200)                         null comment '字段类型组合(JSON)',
    data_characteristics text                                 null comment '数据特征条件(JSON)',
    recommended_chart    varchar(50)                          not null comment '推荐图表类型',
    priority             int        default 50                null comment '优先级(0-100)',
    reason               varchar(500)                         null comment '推荐理由',
    min_data_points      int        default 0                 null comment '最小数据点数',
    max_data_points      int        default 999999            null comment '最大数据点数',
    enabled              tinyint(1) default 1                 null comment '是否启用',
    create_time          datetime   default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '图表推荐规则表' charset = utf8mb4
                             row_format = DYNAMIC;

create index idx_data_pattern
    on `moyun-db`.ai_chart_recommendation_rule (data_pattern);

create index idx_priority
    on `moyun-db`.ai_chart_recommendation_rule (priority);

create table if not exists `moyun-db`.ai_data_insight
(
    id                bigint auto_increment comment '主键ID'
        primary key,
    datasource_id     bigint                                not null comment '数据源ID',
    query_id          bigint                                null comment '查询ID',
    report_id         bigint                                null comment '报告ID',
    insight_type      varchar(50)                           not null comment '洞察类型: anomaly, trend, correlation, pattern',
    severity          varchar(20) default 'medium'          null comment '严重程度: low, medium, high',
    title             varchar(200)                          not null comment '洞察标题',
    description       text                                  not null comment '洞察描述',
    affected_fields   varchar(500)                          null comment '影响的字段',
    statistical_value decimal(20, 4)                        null comment '统计值',
    confidence        decimal(5, 4)                         null comment '置信度(0-1)',
    actionable        tinyint(1)  default 0                 null comment '是否可执行',
    recommendation    text                                  null comment '建议措施',
    is_acknowledged   tinyint(1)  default 0                 null comment '是否已确认',
    acknowledged_by   bigint                                null comment '确认人ID',
    acknowledged_time datetime                              null comment '确认时间',
    create_time       datetime    default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '智能洞察表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_create_time
    on `moyun-db`.ai_data_insight (create_time);

create index idx_datasource_id
    on `moyun-db`.ai_data_insight (datasource_id);

create index idx_insight_type
    on `moyun-db`.ai_data_insight (insight_type);

create index idx_query_id
    on `moyun-db`.ai_data_insight (query_id);

create index idx_report_id
    on `moyun-db`.ai_data_insight (report_id);

create index idx_severity
    on `moyun-db`.ai_data_insight (severity);

create table if not exists `moyun-db`.ai_datasource_config
(
    id                bigint auto_increment comment '主键ID'
        primary key,
    name              varchar(100)                          not null comment '数据源名称',
    type              varchar(20)                           not null comment '数据源类型: mysql, elasticsearch, mongodb',
    host              varchar(255)                          not null comment '主机地址',
    port              int                                   not null comment '端口号',
    database_name     varchar(100)                          null comment '数据库名称',
    username          varchar(100)                          null comment '用户名',
    password          varchar(255)                          null comment '密码(加密存储)',
    connection_params text                                  null comment '额外连接参数(JSON格式)',
    description       varchar(500)                          null comment '描述',
    enabled           tinyint(1)  default 1                 null comment '是否启用: 0-禁用, 1-启用',
    health_status     varchar(20) default 'unknown'         null comment '健康状态: healthy, unhealthy, unknown',
    last_check_time   datetime                              null comment '最后检查时间',
    create_user_id    bigint                                null comment '创建人ID',
    create_time       datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time       datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted           tinyint(1)  default 0                 null comment '删除标记: 0-未删除, 1-已删除'
)
    comment '数据源配置表' charset = utf8mb4
                           row_format = DYNAMIC;

create index idx_create_time
    on `moyun-db`.ai_datasource_config (create_time);

create index idx_enabled
    on `moyun-db`.ai_datasource_config (enabled);

create index idx_type
    on `moyun-db`.ai_datasource_config (type);

create table if not exists `moyun-db`.ai_document_chunk_metadata
(
    id               bigint auto_increment comment '分片ID'
        primary key,
    segment_id       bigint                               not null comment '文档分片ID（关联document_segment表）',
    knowledge_id     bigint                               not null comment '知识库ID',
    chunk_index      int                                  not null comment '分片序号',
    chunk_text       text                                 not null comment '分片文本内容',
    chunk_length     int                                  not null comment '分片长度',
    parent_chunk_id  bigint                               null comment '父分片ID（父子分段模式使用）',
    embedding_model  varchar(100)                         null comment '使用的嵌入模型',
    vector_dimension int                                  null comment '向量维度',
    original_length  int                                  null comment '预处理前长度',
    preprocessed     tinyint(1) default 0                 null comment '是否经过预处理',
    hit_count        int        default 0                 null comment '被检索命中次数',
    last_hit_time    timestamp                            null comment '最后命中时间',
    created_at       timestamp  default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '文档分片元数据表' charset = utf8mb4
                               row_format = DYNAMIC;

create index idx_hit_count
    on `moyun-db`.ai_document_chunk_metadata (hit_count);

create index idx_knowledge_id
    on `moyun-db`.ai_document_chunk_metadata (knowledge_id);

create index idx_parent_chunk
    on `moyun-db`.ai_document_chunk_metadata (parent_chunk_id);

create index idx_segment_id
    on `moyun-db`.ai_document_chunk_metadata (segment_id);

create table if not exists `moyun-db`.ai_document_image
(
    id                   bigint auto_increment comment '主键ID'
        primary key,
    knowledge_base_id    bigint                                not null comment '关联的知识库ID',
    image_path           varchar(500)                          not null comment '图片文件路径',
    page_number          int                                   null comment '所在页码',
    image_index          int                                   null comment '图片在页面中的索引',
    embedding_id         varchar(100)                          null comment '向量ID',
    vector_dimension     int                                   null comment '向量维度',
    width                int                                   null comment '图片宽度',
    height               int                                   null comment '图片高度',
    create_time          datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    description          text                                  null comment '图片内容描述（多模态模型生成）',
    description_language varchar(10) default 'zh'              null comment '描述语言(zh/en)'
)
    comment '文档图片表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_embedding_id
    on `moyun-db`.ai_document_image (embedding_id);

create index idx_knowledge_base_id
    on `moyun-db`.ai_document_image (knowledge_base_id);

create table if not exists `moyun-db`.ai_domain_dictionary
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    keyword       varchar(100)                          not null comment '核心词',
    related_terms text                                  not null comment '相关词列表（逗号分隔）',
    category      varchar(50) default 'general'         null comment '分类（服务器、架构、模型、通用等）',
    description   varchar(500)                          null comment '词典说明',
    is_global     tinyint(1)  default 1                 null comment '是否全局词典（全局词典默认对所有智能体生效）',
    enabled       tinyint(1)  default 1                 null comment '是否启用',
    priority      int         default 0                 null comment '优先级（数字越大优先级越高）',
    create_time   datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted       tinyint(1)  default 0                 not null comment '删除标记: 0-未删除, 1-已删除',
    constraint uk_keyword
        unique (keyword)
)
    comment '领域词典表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_category
    on `moyun-db`.ai_domain_dictionary (category);

create index idx_deleted
    on `moyun-db`.ai_domain_dictionary (deleted);

create index idx_enabled
    on `moyun-db`.ai_domain_dictionary (enabled);

create index idx_global
    on `moyun-db`.ai_domain_dictionary (is_global);

create table if not exists `moyun-db`.ai_execute_log
(
    id             bigint auto_increment comment '主键'
        primary key,
    request_id     varchar(64)                           not null comment '请求ID',
    scene_code     varchar(50)                           not null comment '场景代码',
    handler_name   varchar(100)                          null comment 'Handler名称',
    bind_type      varchar(20)                           null comment '绑定类型',
    model_used     varchar(100)                          null comment '使用的模型',
    agent_used     varchar(100)                          null comment '使用的Agent',
    token_used     int         default 0                 null comment 'Token消耗',
    cost_yuan      decimal(12, 6)                        null comment '本次调用成本（元，细分token×模型单价）',
    tool_calls     json                                  null comment '工具调用记录',
    input_summary  varchar(500)                          null comment '输入摘要',
    output_summary varchar(500)                          null comment '输出摘要',
    status         varchar(20) default 'success'         null comment 'success/fail/timeout',
    error_msg      text                                  null comment '错误信息',
    elapsed_ms     bigint                                null comment '耗时(毫秒)',
    create_time    datetime    default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment 'AI能力调用日志表（可观测性）' charset = utf8mb4;

create index idx_create_time
    on `moyun-db`.ai_execute_log (create_time);

create index idx_request_id
    on `moyun-db`.ai_execute_log (request_id);

create index idx_scene_code
    on `moyun-db`.ai_execute_log (scene_code);

create table if not exists `moyun-db`.ai_knowledge_config_template
(
    id             bigint auto_increment comment '模板ID'
        primary key,
    template_name  varchar(100)                         not null comment '模板名称',
    template_desc  varchar(500)                         null comment '模板描述',
    template_type  varchar(20)                          not null comment '模板类型：general(通用), technical(技术文档), legal(法律), medical(医疗)',
    config_json    json                                 not null comment '配置JSON',
    is_system      tinyint(1) default 0                 null comment '是否系统预设模板',
    use_count      int        default 0                 null comment '使用次数',
    is_recommended tinyint(1) default 0                 not null comment '是否推荐模板',
    created_at     timestamp  default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at     timestamp  default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '知识库配置模板表' charset = utf8mb4
                               row_format = DYNAMIC;

create index idx_template_type
    on `moyun-db`.ai_knowledge_config_template (template_type);

create index idx_use_count
    on `moyun-db`.ai_knowledge_config_template (use_count);

create table if not exists `moyun-db`.ai_knowledge_library
(
    id             bigint auto_increment comment '知识库ID'
        primary key,
    name           varchar(200)                          not null comment '知识库名称',
    description    varchar(1000)                         null comment '知识库描述',
    icon           varchar(50) default '?'               null comment '知识库图标',
    category       varchar(100)                          null comment '知识库分类（如：技术文档、产品手册、FAQ等）',
    tags           text                                  null comment '标签（JSON数组格式）',
    document_count int         default 0                 null comment '文档数量',
    total_segments int         default 0                 null comment '总分段数',
    total_size     bigint      default 0                 null comment '总文件大小（字节）',
    usage_count    int         default 0                 null comment '使用次数（被检索次数）',
    hit_count      int         default 0                 null comment '命中次数',
    last_used_time datetime                              null comment '最后使用时间',
    status         varchar(20) default 'active'          null comment '状态：active(正常), disabled(禁用), archived(归档)',
    is_public      tinyint(1)  default 1                 null comment '是否公开（预留多租户）',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        tinyint(1)  default 0                 not null comment '删除标记: 0-未删除, 1-已删除'
)
    comment '知识库主表' charset = utf8mb4
                         row_format = DYNAMIC;

create table if not exists `moyun-db`.ai_knowledge_base
(
    id                  bigint auto_increment comment '主键ID'
        primary key,
    library_id          bigint                                null comment '所属知识库ID',
    file_name           varchar(255)                          not null comment '文件名',
    file_path           varchar(500)                          not null comment '文件路径',
    pdf_file_path       varchar(500)                          null comment 'PDF文件路径（用于预览）',
    file_size           bigint                                null comment '文件大小（字节）',
    file_type           varchar(50)                           null comment '文件类型',
    vector_id           varchar(100)                          null comment '向量ID（Pinecone中的ID）',
    segment_count       int                                   null comment '文档分段数量',
    vector_dimension    int                                   null comment '向量维度',
    status              int         default 0                 null comment '处理状态：0-待处理，1-处理中，2-处理成功，3-处理失败',
    processing_status   varchar(20) default 'pending'         null comment '处理状态：pending(待配置), configured(已配置), processing(处理中), completed(已完成), failed(失败)',
    config_completed    tinyint(1)  default 0                 null comment '是否完成配置',
    error_message       text                                  null comment '错误信息',
    create_time         datetime    default CURRENT_TIMESTAMP null comment '上传时间',
    update_time         datetime                              null comment '处理时间',
    category            varchar(100)                          null comment '知识库分组',
    tags                text                                  null comment '知识库标签（JSON数组）',
    description         varchar(500)                          null comment '知识库描述',
    usage_count         int         default 0                 null comment '使用次数',
    hit_count           int         default 0                 null comment '命中次数',
    last_used_time      datetime                              null comment '最后使用时间',
    parse_method        varchar(50)                           null comment '文档解析方式: POI, PDFBox, Text',
    content_hash        varchar(64)                           null comment '文件内容SHA-256哈希值（用于增量更新检测）',
    last_processed_time datetime                              null comment '上次处理时间',
    need_reprocess      tinyint(1)  default 0                 null comment '是否需要重新处理',
    deleted             tinyint(1)  default 0                 not null comment '删除标记: 0-未删除, 1-已删除',
    constraint fk_kb_library
        foreign key (library_id) references `moyun-db`.ai_knowledge_library (id)
            on update cascade on delete set null
)
    comment '知识库表' charset = utf8mb4
                       row_format = DYNAMIC;

create table if not exists `moyun-db`.ai_document_segment
(
    id                bigint auto_increment comment '主键ID'
        primary key,
    knowledge_base_id bigint                             not null comment '关联的知识库ID',
    segment_index     int                                not null comment '分片索引（第几个分片）',
    page_number       int                                null comment 'PDF页码',
    line_start        int                                null comment '起始行号',
    line_end          int                                null comment '结束行号',
    char_start        int                                null comment '起始字符位置',
    char_end          int                                null comment '结束字符位置',
    chapter_title     varchar(500)                       null comment '章节标题',
    content           text                               not null comment '分片内容',
    content_length    int                                null comment '分片内容长度',
    embedding_id      varchar(100)                       null comment '向量ID（在Pinecone中的ID）',
    vector_dimension  int                                null comment '向量维度',
    vector_data       longtext                           null comment '向量数据（JSON格式）',
    create_time       datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint fk_ds_kb
        foreign key (knowledge_base_id) references `moyun-db`.ai_knowledge_base (id)
            on update cascade on delete cascade
)
    comment '文档分片表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_embedding_id
    on `moyun-db`.ai_document_segment (embedding_id);

create index idx_knowledge_base_id
    on `moyun-db`.ai_document_segment (knowledge_base_id);

create index idx_category
    on `moyun-db`.ai_knowledge_base (category);

create index idx_create_time
    on `moyun-db`.ai_knowledge_base (create_time);

create index idx_deleted
    on `moyun-db`.ai_knowledge_base (deleted);

create index idx_kb_content_hash
    on `moyun-db`.ai_knowledge_base (content_hash);

create index idx_last_used_time
    on `moyun-db`.ai_knowledge_base (last_used_time);

create index idx_library_id
    on `moyun-db`.ai_knowledge_base (library_id);

create index idx_status
    on `moyun-db`.ai_knowledge_base (status);

create index idx_usage_count
    on `moyun-db`.ai_knowledge_base (usage_count);

create table if not exists `moyun-db`.ai_knowledge_config
(
    id                               bigint auto_increment comment '配置ID'
        primary key,
    knowledge_id                     bigint                                not null comment '知识库ID',
    segment_mode                     varchar(20) default 'general'         not null comment '分段模式：general(通用), parent_child(父子分段)',
    segment_separator                varchar(50) default '\n\n'            null comment '分段标识符',
    segment_max_length               int         default 800               not null comment '分段最大长度（字符数，800字符确保题库问答对完整，技术文档可用500，小说可用1500）',
    segment_overlap_length           int         default 100               not null comment '分段重叠长度（字符数，100字符保证上下文连贯性）',
    chunking_strategy                varchar(50) default 'fixed'           null comment '分片策略: fixed(固定大小), adaptive(自适应), document_type(按文档类型)',
    document_type                    varchar(50) default 'general'         null comment '文档类型: general(通用), faq(问答), table(表格), code(代码), technical(技术文档)',
    faq_chunk_size                   int         default 400               null comment 'FAQ分片大小(字符)',
    table_chunk_strategy             varchar(50) default 'by_row'          null comment '表格分片策略: by_row(按行), by_table(整表), by_cell(按单元格)',
    code_chunk_strategy              varchar(50) default 'by_function'     null comment '代码分片策略: by_function(按函数), by_class(按类), by_file(按文件)',
    technical_chunk_size             int         default 1200              null comment '技术文档分片大小(字符)',
    enable_smart_boundary            tinyint(1)  default 1                 null comment '启用智能边界检测(避免切断句子)',
    preprocess_replace_spaces        tinyint(1)  default 1                 null comment '替换连续空格、换行、制表符',
    preprocess_remove_urls           tinyint(1)  default 1                 null comment '删除URL和邮箱地址',
    preprocess_remove_extra_newlines tinyint(1)  default 1                 null comment '删除多余换行',
    index_mode                       varchar(20) default 'high_quality'    not null comment '索引方式：high_quality(高质量), economy(经济)',
    embedding_model                  varchar(100)                          null comment '嵌入模型名称',
    retrieval_mode                   varchar(20) default 'vector'          not null comment '检索模式：vector(向量), keyword(关键词), hybrid(混合)',
    retrieval_top_k                  int         default 3                 null comment '检索Top K数量',
    rerank_enabled                   tinyint(1)  default 0                 null comment '是否启用重排序',
    rerank_model                     varchar(100)                          null comment '重排序模型',
    qa_mode                          tinyint(1)  default 0                 null comment '是否启用Q&A模式',
    qa_extraction_prompt             text                                  null comment 'Q&A提取提示词',
    preprocess_remove_special_chars  tinyint(1)  default 0                 null comment '删除特殊字符',
    preprocess_remove_table_desc     tinyint(1)  default 0                 null comment '删除表格描述',
    preprocess_remove_header_footer  tinyint(1)  default 0                 null comment '删除页眉页脚',
    created_at                       timestamp   default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at                       timestamp   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_knowledge_id
        unique (knowledge_id),
    constraint fk_kc_knowledge
        foreign key (knowledge_id) references `moyun-db`.ai_knowledge_base (id)
            on update cascade on delete cascade
)
    comment '知识库配置表 - 包含分片策略、文档类型识别、预处理规则等配置' charset = utf8mb4
                                                                        row_format = DYNAMIC;

create index idx_chunking_strategy
    on `moyun-db`.ai_knowledge_config (chunking_strategy);

create index idx_document_type
    on `moyun-db`.ai_knowledge_config (document_type);

create index idx_index_mode
    on `moyun-db`.ai_knowledge_config (index_mode);

create index idx_segment_mode
    on `moyun-db`.ai_knowledge_config (segment_mode);

create index idx_category
    on `moyun-db`.ai_knowledge_library (category);

create index idx_create_time
    on `moyun-db`.ai_knowledge_library (create_time);

create index idx_deleted
    on `moyun-db`.ai_knowledge_library (deleted);

create index idx_status
    on `moyun-db`.ai_knowledge_library (status);

create index idx_usage_count
    on `moyun-db`.ai_knowledge_library (usage_count);

create table if not exists `moyun-db`.ai_knowledge_library_config
(
    id                               bigint auto_increment comment '配置ID'
        primary key,
    library_id                       bigint                                not null comment '知识库ID',
    segment_mode                     varchar(20) default 'general'         not null comment '分段模式：general(通用), qa(问答), code(代码)',
    segment_separator                varchar(50) default '\n\n'            null comment '分段标识符',
    segment_max_length               int         default 800               not null comment '分段最大长度',
    segment_overlap_length           int         default 100               not null comment '分段重叠长度',
    preprocess_replace_spaces        tinyint(1)  default 1                 null comment '替换连续空格',
    preprocess_remove_urls           tinyint(1)  default 1                 null comment '删除URL',
    preprocess_remove_extra_newlines tinyint(1)  default 1                 null comment '删除多余换行',
    index_mode                       varchar(20) default 'high_quality'    not null comment '索引模式：high_quality, economy',
    embedding_model                  varchar(100)                          null comment 'Embedding模型',
    retrieval_mode                   varchar(20) default 'hybrid'          null comment '检索模式：vector, keyword, hybrid',
    retrieval_top_k                  int         default 10                null comment '检索返回数量',
    rerank_enabled                   tinyint(1)  default 0                 null comment '是否启用Rerank',
    rerank_model                     varchar(100)                          null comment 'Rerank模型',
    created_at                       datetime    default CURRENT_TIMESTAMP null,
    updated_at                       datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_library_id
        unique (library_id),
    constraint fk_library_config
        foreign key (library_id) references `moyun-db`.ai_knowledge_library (id)
            on delete cascade
)
    comment '知识库配置表' charset = utf8mb4
                           row_format = DYNAMIC;

create table if not exists `moyun-db`.ai_model_config
(
    id                  bigint auto_increment comment '主键ID'
        primary key,
    name                varchar(100)                             not null comment '配置名称',
    provider            varchar(50)                              not null comment '模型提供商(openai/ollama/dashscope)',
    model_type          varchar(20)    default 'chat'            null comment '模型类型(chat/embedding/multimodal/reranker/asr/tts)，asr=语音识别，tts=语音合成，V10.0 语音面试官使用',
    model_name          varchar(100)                             not null comment '模型名称',
    api_key             varchar(500)                             null comment 'API密钥',
    base_url            varchar(500)                             null comment 'API基础URL',
    temperature         double         default 0.7               null comment '温度参数(0-2)',
    max_tokens          int            default 2000              null comment '最大Token数',
    timeout             int            default 60                null comment '超时时间(秒)',
    streaming_supported tinyint(1)     default 1                 null comment '是否支持流式输出',
    enabled             tinyint(1)     default 1                 null comment '是否启用',
    is_default          tinyint(1)     default 0                 null comment '是否为默认模型',
    description         text                                     null comment '备注说明',
    create_time         datetime       default CURRENT_TIMESTAMP null comment '创建时间',
    update_time         datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    input_price         decimal(10, 6) default 0.001000          null comment '输入价格（元/1000 tokens）',
    output_price        decimal(10, 6) default 0.002000          null comment '输出价格（元/1000 tokens）',
    deleted             tinyint(1)     default 0                 not null comment '删除标记: 0-未删除, 1-已删除'
)
    comment '模型配置表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_deleted
    on `moyun-db`.ai_model_config (deleted);

create index idx_enabled
    on `moyun-db`.ai_model_config (enabled);

create index idx_is_default
    on `moyun-db`.ai_model_config (is_default);

create index idx_provider
    on `moyun-db`.ai_model_config (provider);

create table if not exists `moyun-db`.ai_provider
(
    id                 bigint auto_increment comment '主键'
        primary key,
    code               varchar(32)                             not null comment '提供商编码（model_config.provider 关联值，小写唯一）',
    name               varchar(64)                             not null comment '显示名称',
    api_style          varchar(32) default 'openai_compatible' not null comment 'API 风格：openai_compatible=OpenAI兼容 / ollama_native=Ollama原生',
    default_base_url   varchar(255)                            null comment '默认 Base URL（模型配置留空时兜底）',
    supports_streaming tinyint(1)  default 1                   not null comment '该提供商 chat 模型是否支持流式输出',
    requires_api_key   tinyint(1)  default 1                   not null comment '是否必须配置 API Key',
    enabled            tinyint(1)  default 1                   not null comment '是否启用（停用后不可选/不可用）',
    sort_order         int         default 0                   not null comment '排序（小在前）',
    remark             varchar(255)                            null comment '备注',
    create_time        datetime    default CURRENT_TIMESTAMP   not null comment '创建时间',
    update_time        datetime                                null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted            tinyint(1)  default 0                   not null comment '逻辑删除',
    constraint uk_code
        unique (code)
)
    comment 'AI 提供商注册表' charset = utf8mb4;

create index idx_enabled
    on `moyun-db`.ai_provider (enabled);

create table if not exists `moyun-db`.ai_query_history
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    datasource_id   bigint                                not null comment '数据源ID',
    user_id         bigint                                null comment '用户ID',
    session_id      varchar(100)                          null comment '会话ID',
    natural_query   text                                  not null comment '自然语言查询',
    generated_sql   text                                  null comment '生成的SQL语句',
    query_type      varchar(20)                           null comment '查询类型: select, aggregate, join, analysis',
    tables_involved varchar(500)                          null comment '涉及的表(逗号分隔)',
    result_count    int         default 0                 null comment '结果行数',
    execution_time  int         default 0                 null comment '执行时间(毫秒)',
    status          varchar(20) default 'success'         null comment '执行状态: success, failed, timeout',
    error_message   text                                  null comment '错误信息',
    analysis_type   varchar(50)                           null comment '分析类型: basic, trend, correlation, ranking',
    chart_type      varchar(50)                           null comment '图表类型',
    has_insight     tinyint(1)  default 0                 null comment '是否生成洞察',
    token_used      int         default 0                 null comment 'LLM消耗的Token数',
    create_time     datetime    default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '查询历史表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_create_time
    on `moyun-db`.ai_query_history (create_time);

create index idx_datasource_id
    on `moyun-db`.ai_query_history (datasource_id);

create index idx_session_id
    on `moyun-db`.ai_query_history (session_id);

create index idx_status
    on `moyun-db`.ai_query_history (status);

create index idx_user_id
    on `moyun-db`.ai_query_history (user_id);

create table if not exists `moyun-db`.ai_reference_feedback
(
    id                bigint auto_increment comment '主键ID'
        primary key,
    knowledge_base_id bigint                             null comment '知识库ID',
    file_name         varchar(500)                       null comment '文件名',
    page_number       int                                null comment '页码',
    segment_index     int                                null comment '分片索引',
    user_query        text                               null comment '用户查询',
    rerank_score      double                             null comment '重排分数',
    vector_score      double                             null comment '向量相似度',
    feedback_type     varchar(50)                        null comment '反馈类型：accurate(准确), inaccurate(不准确)',
    agent_id          bigint                             null comment '智能体ID',
    memory_id         varchar(100)                       null comment '会话ID',
    create_time       datetime default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '参考来源反馈表' collate = utf8mb4_unicode_ci
                             row_format = DYNAMIC;

create index idx_create_time
    on `moyun-db`.ai_reference_feedback (create_time);

create index idx_feedback_type
    on `moyun-db`.ai_reference_feedback (feedback_type);

create index idx_knowledge_base_id
    on `moyun-db`.ai_reference_feedback (knowledge_base_id);

create table if not exists `moyun-db`.ai_scene_config
(
    id                     bigint auto_increment comment '主键'
        primary key,
    scene_code             varchar(50)                             not null comment '场景代码：voice_interview/resume_optimize/question_generate',
    scene_name             varchar(100)                            not null comment '场景名称',
    description            varchar(500)                            null comment '场景描述',
    scene_category         varchar(30)                             null comment '场景分类: chat/analysis/generation/classification',
    agent_id               bigint                                  null comment '绑定的智能体（ai_agent.id，启用状态才生效）',
    model_config_id        bigint                                  null comment '直接绑定模型（ai_model_config.id，agent_id 为空时生效）',
    knowledge_library_ids  varchar(500)                            null comment '知识库ID列表 JSON数组',
    tool_ids               varchar(500)                            null comment '工具ID列表 JSON数组',
    workflow_id            bigint                                  null comment '绑定工作流（ai_workflow.id）',
    config_json            text                                    null comment '场景策略配置 JSON（如 dynamicMode）',
    handler_bean_name      varchar(100)                            null comment '对应Spring Bean名称（统一网关路由）',
    handler_method         varchar(50)   default 'execute'         null comment '执行方法名',
    system_prompt_template text                                    null comment '系统提示词模板（支持占位符 {{variable}}）',
    user_prompt_template   text                                    null comment '用户提示词模板',
    prompt_placeholders    json                                    null comment '占位符说明 {key: description}',
    output_mode            varchar(20)   default 'sync'            null comment '输出模式: sync/stream/both',
    output_schema          json                                    null comment '输出结构定义',
    output_parser          varchar(50)                             null comment '解析器: json/markdown/custom',
    max_tokens             int           default 2048              null comment '最大Token数',
    temperature            decimal(2, 1) default 0.7               null comment '温度参数',
    timeout_seconds        int           default 30                null comment '超时秒数',
    retry_count            int           default 3                 null comment '重试次数',
    rate_limit_key         varchar(50)                             null comment '限流Key',
    rate_limit_count       int           default 100               null comment '限流次数',
    rate_limit_time        int           default 60                null comment '限流时间窗口(秒)',
    daily_token_limit      int                                     null comment '场景日Token上限（当日累计超限拒绝调用；null/0=不限）',
    enable_output_filter   tinyint(1)    default 0                 null comment '是否启用输出内容过滤（响应data文本经DFA词树脱敏；0=关闭）',
    fallback_model_id      bigint                                  null comment '备用模型ID',
    fallback_response      text                                    null comment '兜底回复（AI不可用时返回）',
    enable_cache           tinyint(1)    default 0                 null comment '是否启用缓存',
    cache_ttl              int           default 3600              null comment '缓存时间(秒)',
    version                varchar(20)   default 'v1'              not null comment '版本号',
    weight                 int           default 100               not null comment '灰度权重（同场景多版本按权重轮盘赌）',
    priority               int           default 0                 not null comment '优先级（全 0 时取 is_default，再按 priority DESC）',
    is_default             tinyint(1)    default 0                 not null comment '是否默认版本',
    enabled                tinyint(1)    default 1                 not null comment '是否启用',
    open_api               tinyint(1)    default 0                 not null comment '是否开放通用入口调用（1=可经 /api/ai/execute 外部调用）',
    create_time            datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time            datetime                                null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted                tinyint(1)    default 0                 not null comment '逻辑删除: 0-未删除 1-已删除',
    constraint uk_scene_version
        unique (scene_code, version)
)
    comment 'AI场景配置表（业务场景与Agent/模型/知识库/工作流动态绑定）' charset = utf8mb4;

create index idx_agent_id
    on `moyun-db`.ai_scene_config (agent_id);

create index idx_deleted
    on `moyun-db`.ai_scene_config (deleted);

create index idx_scene_code
    on `moyun-db`.ai_scene_config (scene_code);

create table if not exists `moyun-db`.ai_sql_template
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    template_name   varchar(200)                          not null comment '模板名称',
    natural_query   text                                  not null comment '自然语言示例',
    ai_sql_template text                                  not null comment 'SQL模板',
    query_type      varchar(50)                           null comment '查询类型',
    complexity      varchar(20) default 'simple'          null comment '复杂度: simple, medium, complex',
    table_pattern   varchar(200)                          null comment '表名模式',
    usage_count     int         default 0                 null comment '使用次数',
    success_rate    decimal(5, 2)                         null comment '成功率(%)',
    enabled         tinyint(1)  default 1                 null comment '是否启用',
    create_time     datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment 'SQL模板表' charset = utf8mb4
                        row_format = DYNAMIC;

create index idx_complexity
    on `moyun-db`.ai_sql_template (complexity);

create index idx_query_type
    on `moyun-db`.ai_sql_template (query_type);

create index idx_usage_count
    on `moyun-db`.ai_sql_template (usage_count);

create table if not exists `moyun-db`.ai_table_metadata
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    datasource_id   bigint                               not null comment '数据源ID',
    table_name      varchar(200)                         not null comment '表名',
    table_comment   varchar(500)                         null comment '表注释',
    table_schema    text                                 null comment '表结构(JSON格式)',
    column_count    int        default 0                 null comment '字段数量',
    row_count       bigint     default 0                 null comment '行数(估算)',
    data_size       bigint     default 0                 null comment '数据大小(字节)',
    has_primary_key tinyint(1) default 0                 null comment '是否有主键',
    has_time_field  tinyint(1) default 0                 null comment '是否有时间字段',
    time_field_name varchar(100)                         null comment '时间字段名',
    numeric_fields  text                                 null comment '数值型字段列表(JSON)',
    category_fields text                                 null comment '类别型字段列表(JSON)',
    indexed_fields  text                                 null comment '索引字段列表(JSON)',
    last_sync_time  datetime                             null comment '最后同步时间',
    create_time     datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_datasource_table
        unique (datasource_id, table_name)
)
    comment '表元数据缓存表' charset = utf8mb4
                             row_format = DYNAMIC;

create index idx_datasource_id
    on `moyun-db`.ai_table_metadata (datasource_id);

create index idx_last_sync_time
    on `moyun-db`.ai_table_metadata (last_sync_time);

create table if not exists `moyun-db`.ai_token_usage_summary
(
    id                  bigint auto_increment comment '主键ID'
        primary key,
    agent_id            bigint                             null comment '智能体ID',
    user_id             varchar(100)                       null comment '用户ID',
    stat_date           date                               null comment '统计日期',
    model_name          varchar(100)                       null comment '模型名称',
    total_requests      int      default 0                 null comment '请求次数',
    total_input_tokens  bigint   default 0                 null comment '总输入token',
    total_output_tokens bigint   default 0                 null comment '总输出token',
    total_tokens        bigint   default 0                 null comment '总token',
    total_cost          decimal(12, 6)                     null comment '总费用',
    create_time         datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time         datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_agent_user_date_model
        unique (agent_id, user_id, stat_date, model_name)
)
    comment 'Token使用统计汇总表' charset = utf8mb4
                                  row_format = DYNAMIC;

create index idx_stat_date
    on `moyun-db`.ai_token_usage_summary (stat_date);

create table if not exists `moyun-db`.ai_tool_call_log
(
    id              bigint auto_increment comment '日志ID'
        primary key,
    conversation_id bigint                                null comment '会话ID',
    message_id      bigint                                null comment '消息ID',
    agent_id        bigint                                null comment '智能体ID',
    tool_id         bigint                                null comment '工具ID',
    tool_name       varchar(100)                          not null comment '工具名称',
    input_params    json                                  null comment '输入参数',
    output_result   text                                  null comment '输出结果',
    status          varchar(20) default 'success'         null comment '状态：pending/running/success/failed/timeout',
    error_message   text                                  null comment '错误信息',
    duration_ms     int                                   null comment '执行耗时（毫秒）',
    create_time     datetime    default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '工具调用日志表' charset = utf8mb4
                             row_format = DYNAMIC;

create index idx_agent_id
    on `moyun-db`.ai_tool_call_log (agent_id);

create index idx_conversation_id
    on `moyun-db`.ai_tool_call_log (conversation_id);

create index idx_create_time
    on `moyun-db`.ai_tool_call_log (create_time);

create index idx_status
    on `moyun-db`.ai_tool_call_log (status);

create index idx_tool_name
    on `moyun-db`.ai_tool_call_log (tool_name);

create table if not exists `moyun-db`.ai_workflow
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    name        varchar(100)                          not null comment '工作流名称',
    description varchar(500)                          null comment '工作流描述',
    graph_data  longtext                              null comment '工作流图定义(JSON)',
    variables   text                                  null comment '全局变量定义(JSON)',
    status      varchar(20) default 'draft'           null comment '状态: draft-草稿, published-已发布, disabled-已禁用',
    version     int         default 1                 null comment '版本号',
    enabled     tinyint(1)  default 0                 null comment '是否启用',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     tinyint(1)  default 0                 not null comment '删除标记: 0-未删除, 1-已删除'
)
    comment '工作流定义表' collate = utf8mb4_unicode_ci
                           row_format = DYNAMIC;

create table if not exists `moyun-db`.ai_agent
(
    id                         bigint auto_increment comment '主键ID'
        primary key,
    name                       varchar(100)                          not null comment '智能体名称',
    description                varchar(500)                          null comment '智能体描述',
    system_prompt              text                                  null comment '系统提示词',
    knowledge_library_ids      text                                  null comment '关联的知识库ID列表（JSON数组）',
    knowledge_base_weights     text                                  null comment '知识库权重配置（JSON格式：{"1": 1.0, "2": 0.8}，权重范围0.1-1.0）',
    model_config_id            bigint                                null comment '模型配置ID(关联model_config表,NULL则使用默认模型)',
    model_name                 varchar(50) default 'qwen-plus'       null comment '模型名称',
    temperature                double      default 0.7               null comment '温度参数',
    max_tokens                 int         default 2000              null comment '最大token数',
    rag_min_score              double                                null comment 'RAG检索相似度阈值(0.5-1.0,推荐0.7-0.75,NULL则使用全局配置)',
    rag_max_results            int                                   null comment 'RAG检索最大结果数量(1-10,推荐3-5,NULL则使用全局配置)',
    enabled                    tinyint(1)  default 1                 null comment '是否启用',
    welcome_message            text                                  null comment '开场白',
    suggested_questions        text                                  null comment '预设问题(JSON数组)',
    show_citations             tinyint(1)  default 1                 null comment '是否显示引用来源',
    max_history_turns          int         default 10                null comment '最大历史轮数',
    api_enabled                tinyint(1)  default 0                 null comment '是否启用API',
    api_key                    varchar(64)                           null comment 'API Key',
    workflow_id                bigint                                null comment '关联工作流ID',
    workflow_trigger_mode      varchar(20) default 'manual'          null comment '工作流触发模式: manual/auto/keyword',
    workflow_trigger_keywords  text                                  null comment '触发关键词(JSON数组)',
    publish_enabled            tinyint(1)  default 0                 null comment '是否发布为应用',
    publish_token              varchar(64)                           null comment '发布访问Token',
    publish_settings           text                                  null comment '发布设置(JSON)',
    create_time                datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time                datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    rag_recall_multiplier      double                                null comment '第一阶段召回倍数（1.5-3.0，推荐2.0，NULL时使用全局配置）',
    rag_enable_hybrid_search   tinyint(1)  default 1                 null comment '是否启用混合检索（向量+BM25）',
    rag_enable_query_expansion tinyint(1)  default 1                 null comment '是否启用查询扩展',
    rag_bm25_weight            double      default 0.3               null comment 'BM25检索权重（0-1）',
    rag_vector_weight          double      default 0.7               null comment '向量检索权重（0-1）',
    enable_self_reflection     tinyint(1)  default 0                 null comment '是否启用自我反思',
    deleted                    tinyint(1)  default 0                 not null comment '删除标记: 0-未删除, 1-已删除',
    constraint fk_agent_model_config
        foreign key (model_config_id) references `moyun-db`.ai_model_config (id)
            on update cascade on delete set null,
    constraint fk_agent_workflow
        foreign key (workflow_id) references `moyun-db`.ai_workflow (id)
            on update cascade on delete set null
)
    comment '智能体表' charset = utf8mb4
                       row_format = DYNAMIC;

create index idx_deleted
    on `moyun-db`.ai_agent (deleted);

create index idx_enabled
    on `moyun-db`.ai_agent (enabled);

create index idx_model_config_id
    on `moyun-db`.ai_agent (model_config_id);

create table if not exists `moyun-db`.ai_agent_dictionary_relation
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    agent_id      bigint                               not null comment '智能体ID',
    dictionary_id bigint                               not null comment '词典ID',
    enabled       tinyint(1) default 1                 null comment '是否启用',
    create_time   datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_agent_dict
        unique (agent_id, dictionary_id),
    constraint fk_adr_agent
        foreign key (agent_id) references `moyun-db`.ai_agent (id)
            on update cascade on delete cascade,
    constraint fk_adr_dict
        foreign key (dictionary_id) references `moyun-db`.ai_domain_dictionary (id)
            on update cascade on delete cascade
)
    comment '智能体词典关联表' charset = utf8mb4
                               row_format = DYNAMIC;

create index idx_agent_id
    on `moyun-db`.ai_agent_dictionary_relation (agent_id);

create index idx_dictionary_id
    on `moyun-db`.ai_agent_dictionary_relation (dictionary_id);

create table if not exists `moyun-db`.ai_agent_tool_relation
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    agent_id      bigint                               not null comment '智能体ID',
    tool_id       bigint                               not null comment '工具ID',
    custom_config json                                 null comment '针对该智能体的自定义配置',
    enabled       tinyint(1) default 1                 null comment '是否启用',
    create_time   datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_agent_tool
        unique (agent_id, tool_id),
    constraint fk_atr_agent
        foreign key (agent_id) references `moyun-db`.ai_agent (id)
            on update cascade on delete cascade,
    constraint fk_atr_tool
        foreign key (tool_id) references `moyun-db`.ai_agent_tool (id)
            on update cascade on delete cascade
)
    comment '智能体工具关联表' charset = utf8mb4
                               row_format = DYNAMIC;

create index idx_agent_id
    on `moyun-db`.ai_agent_tool_relation (agent_id);

create index idx_tool_id
    on `moyun-db`.ai_agent_tool_relation (tool_id);

create table if not exists `moyun-db`.ai_agent_workflow_relation
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    agent_id    bigint                               not null comment '智能体ID',
    workflow_id bigint                               not null comment '工作流ID',
    enabled     tinyint(1) default 1                 null comment '是否启用',
    sort_order  int        default 0                 null comment '排序',
    create_time datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_agent_workflow
        unique (agent_id, workflow_id),
    constraint fk_awr_agent
        foreign key (agent_id) references `moyun-db`.ai_agent (id)
            on update cascade on delete cascade,
    constraint fk_awr_workflow
        foreign key (workflow_id) references `moyun-db`.ai_workflow (id)
            on update cascade on delete cascade
)
    comment '智能体-工作流关联表' collate = utf8mb4_unicode_ci
                                  row_format = DYNAMIC;

create index idx_agent_id
    on `moyun-db`.ai_agent_workflow_relation (agent_id);

create index idx_workflow_id
    on `moyun-db`.ai_agent_workflow_relation (workflow_id);

create table if not exists `moyun-db`.ai_chat_history
(
    id                bigint auto_increment
        primary key,
    agent_id          bigint                             not null comment '智能体ID',
    session_id        varchar(64)                        not null comment '会话ID',
    user_message      text                               null comment '用户消息',
    assistant_message text                               null comment '助手回复',
    tokens_used       int      default 0                 null comment 'Token消耗',
    retrieval_results text                               null comment '检索结果JSON',
    retrieval_count   int      default 0                 null comment '检索命中数',
    response_time     int      default 0                 null comment '响应时间(毫秒)',
    create_time       datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint fk_ch_agent
        foreign key (agent_id) references `moyun-db`.ai_agent (id)
            on update cascade on delete cascade
)
    comment '对话历史表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_agent_id
    on `moyun-db`.ai_chat_history (agent_id);

create index idx_create_time
    on `moyun-db`.ai_chat_history (create_time);

create index idx_session_id
    on `moyun-db`.ai_chat_history (session_id);

create table if not exists `moyun-db`.ai_conversation
(
    id                 bigint auto_increment comment '会话ID'
        primary key,
    agent_id           bigint                                 not null comment '智能体ID',
    title              varchar(200) default '新对话'          null comment '会话标题（自动生成或用户修改）',
    user_id            varchar(100)                           null comment '用户ID（预留字段，支持多用户）',
    message_count      int          default 0                 null comment '消息数量',
    create_time        datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time        datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '最后更新时间',
    summary            text                                   null comment '对话摘要',
    summary_updated_at datetime                               null comment '摘要更新时间',
    deleted            tinyint(1)   default 0                 not null comment '删除标记: 0-未删除, 1-已删除',
    constraint fk_c_agent
        foreign key (agent_id) references `moyun-db`.ai_agent (id)
            on update cascade on delete cascade
)
    comment '对话会话表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_agent_id
    on `moyun-db`.ai_conversation (agent_id);

create index idx_deleted
    on `moyun-db`.ai_conversation (deleted);

create index idx_update_time
    on `moyun-db`.ai_conversation (update_time);

create index idx_user_id
    on `moyun-db`.ai_conversation (user_id);

create table if not exists `moyun-db`.ai_conversation_message
(
    id                bigint auto_increment comment '消息ID'
        primary key,
    conversation_id   bigint                             not null comment '会话ID',
    role              varchar(20)                        not null comment '角色：user/assistant',
    content           text                               not null comment '消息内容',
    reference_sources text                               null comment '参考来源（JSON格式）',
    create_time       datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint conversation_message_ibfk_1
        foreign key (conversation_id) references `moyun-db`.ai_conversation (id)
            on delete cascade
)
    comment '对话消息表' charset = utf8mb4
                         row_format = DYNAMIC;

create index idx_conversation_id
    on `moyun-db`.ai_conversation_message (conversation_id);

create index idx_create_time
    on `moyun-db`.ai_conversation_message (create_time);

create table if not exists `moyun-db`.ai_token_usage_log
(
    id                    bigint auto_increment comment '主键ID'
        primary key,
    conversation_id       bigint                             null comment '会话ID',
    message_id            bigint                             null comment '消息ID',
    agent_id              bigint                             null comment '智能体ID',
    workflow_id           bigint                             null comment '工作流ID',
    workflow_execution_id bigint                             null comment '工作流执行ID',
    workflow_node_id      varchar(100)                       null comment '工作流节点ID',
    user_id               varchar(100)                       null comment '用户ID',
    model_name            varchar(100)                       null comment '模型名称',
    model_provider        varchar(50)                        null comment '模型提供商',
    input_tokens          int      default 0                 null comment '输入token数',
    output_tokens         int      default 0                 null comment '输出token数',
    total_tokens          int      default 0                 null comment '总token数',
    cost                  decimal(10, 6)                     null comment '费用（元）',
    request_type          varchar(50)                        null comment '请求类型：chat/embedding_query/embedding_document/workflow_llm/workflow_classifier/workflow_extractor/workflow_question',
    create_time           datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint fk_tul_agent
        foreign key (agent_id) references `moyun-db`.ai_agent (id)
            on update cascade on delete set null,
    constraint fk_tul_conversation
        foreign key (conversation_id) references `moyun-db`.ai_conversation (id)
            on update cascade on delete set null,
    constraint fk_tul_message
        foreign key (message_id) references `moyun-db`.ai_conversation_message (id)
            on update cascade on delete set null
)
    comment 'Token使用记录表' charset = utf8mb4
                              row_format = DYNAMIC;

create index idx_agent_id
    on `moyun-db`.ai_token_usage_log (agent_id);

create index idx_conversation_id
    on `moyun-db`.ai_token_usage_log (conversation_id);

create index idx_create_time
    on `moyun-db`.ai_token_usage_log (create_time);

create index idx_model_name
    on `moyun-db`.ai_token_usage_log (model_name);

create index idx_user_id
    on `moyun-db`.ai_token_usage_log (user_id);

create index idx_workflow_execution_id
    on `moyun-db`.ai_token_usage_log (workflow_execution_id);

create index idx_workflow_id
    on `moyun-db`.ai_token_usage_log (workflow_id);

create index idx_deleted
    on `moyun-db`.ai_workflow (deleted);

create index idx_enabled
    on `moyun-db`.ai_workflow (enabled);

create index idx_status
    on `moyun-db`.ai_workflow (status);

create table if not exists `moyun-db`.ai_workflow_execution
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    workflow_id     bigint                                not null comment '工作流ID',
    status          varchar(20) default 'running'         null comment '执行状态: running-执行中, completed-已完成, failed-失败, cancelled-已取消',
    input_data      text                                  null comment '输入参数(JSON)',
    output_data     longtext                              null comment '输出结果(JSON)',
    execution_log   longtext                              null comment '执行日志(JSON数组)',
    error_message   text                                  null comment '错误信息',
    current_node_id varchar(100)                          null comment '当前执行到的节点ID',
    duration_ms     bigint                                null comment '执行耗时(毫秒)',
    start_time      datetime                              null comment '开始时间',
    end_time        datetime                              null comment '结束时间',
    create_time     datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    constraint fk_we_workflow
        foreign key (workflow_id) references `moyun-db`.ai_workflow (id)
            on update cascade on delete cascade
)
    comment '工作流执行记录表' collate = utf8mb4_unicode_ci
                               row_format = DYNAMIC;

create index idx_create_time
    on `moyun-db`.ai_workflow_execution (create_time);

create index idx_status
    on `moyun-db`.ai_workflow_execution (status);

create index idx_workflow_id
    on `moyun-db`.ai_workflow_execution (workflow_id);

create table if not exists `moyun-db`.ai_workflow_version
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    workflow_id bigint                             not null comment '工作流ID',
    version     int                                not null comment '版本号',
    description varchar(200)                       null comment '版本描述',
    graph_data  longtext                           null comment '工作流图数据快照(JSON)',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint fk_wv_workflow
        foreign key (workflow_id) references `moyun-db`.ai_workflow (id)
            on update cascade on delete cascade
)
    comment '工作流版本表' collate = utf8mb4_unicode_ci
                           row_format = DYNAMIC;

create index idx_version
    on `moyun-db`.ai_workflow_version (version);

create index idx_workflow_id
    on `moyun-db`.ai_workflow_version (workflow_id);

create table if not exists `moyun-db`.gen_table
(
    table_id          bigint auto_increment comment '编号'
        primary key,
    table_name        varchar(200) default ''     null comment '表名称',
    table_comment     varchar(500) default ''     null comment '表描述',
    sub_table_name    varchar(64)                 null comment '关联子表的表名',
    sub_table_fk_name varchar(64)                 null comment '子表关联的外键名',
    class_name        varchar(100) default ''     null comment '实体类名称',
    tpl_category      varchar(200) default 'crud' null comment '使用的模板（crud单表操作 tree树表操作）',
    tpl_web_type      varchar(30)  default ''     null comment '前端模板类型（element-ui模版 element-plus模版）',
    package_name      varchar(100)                null comment '生成包路径',
    module_name       varchar(30)                 null comment '生成模块名',
    business_name     varchar(30)                 null comment '生成业务名',
    function_name     varchar(50)                 null comment '生成功能名',
    function_author   varchar(50)                 null comment '生成功能作者',
    gen_type          char         default '0'    null comment '生成代码方式（0zip压缩包 1自定义路径）',
    gen_path          varchar(200) default '/'    null comment '生成路径（不填默认项目路径）',
    options           varchar(1000)               null comment '其它生成选项',
    create_by         varchar(64)  default ''     null comment '创建者',
    create_time       datetime                    null comment '创建时间',
    update_by         varchar(64)  default ''     null comment '更新者',
    update_time       datetime                    null comment '更新时间',
    remark            varchar(500)                null comment '备注'
)
    comment '代码生成业务表' charset = utf8mb4;

create table if not exists `moyun-db`.gen_table_column
(
    column_id      bigint auto_increment comment '编号'
        primary key,
    table_id       bigint                    null comment '归属表编号',
    column_name    varchar(200)              null comment '列名称',
    column_comment varchar(500)              null comment '列描述',
    column_type    varchar(100)              null comment '列类型',
    java_type      varchar(500)              null comment 'JAVA类型',
    java_field     varchar(200)              null comment 'JAVA字段名',
    is_pk          char                      null comment '是否主键（1是）',
    is_increment   char                      null comment '是否自增（1是）',
    is_required    char                      null comment '是否必填（1是）',
    is_insert      char                      null comment '是否为插入字段（1是）',
    is_edit        char                      null comment '是否编辑字段（1是）',
    is_list        char                      null comment '是否列表字段（1是）',
    is_query       char                      null comment '是否查询字段（1是）',
    query_type     varchar(200) default 'EQ' null comment '查询方式（等于、不等于、大于、小于、范围）',
    html_type      varchar(200)              null comment '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
    dict_type      varchar(200) default ''   null comment '字典类型',
    sort           int                       null comment '排序',
    create_by      varchar(64)  default ''   null comment '创建者',
    create_time    datetime                  null comment '创建时间',
    update_by      varchar(64)  default ''   null comment '更新者',
    update_time    datetime                  null comment '更新时间',
    remark         varchar(500)              null comment '备注'
)
    comment '代码生成业务表字段' charset = utf8mb4;

create table if not exists `moyun-db`.ledger_ai_analysis_report
(
    id               bigint auto_increment comment '报告ID'
        primary key,
    user_id          bigint                             not null comment '门户用户ID（portal_user.id）',
    period varchar (7) not null comment '报告月份 yyyy-MM（每月一条，刷新覆盖）',
    health_score     int      default 0                 not null comment '财务健康分 0-100',
    metrics_json     text                               null comment '核心指标 JSON（资产负债率/月均收支/储蓄率等）',
    income_json      text                               null comment '收入来源占比 JSON',
    risk_json        text                               null comment '债务风险列表 JSON',
    advice_json      text                               null comment '建议列表 JSON',
    ai_summary       text                               null comment 'LLM 综述（失败时为模板文案）',
    ai_enabled       tinyint  default 0                 not null comment '综述是否为 LLM 生成：1=是 0=模板降级',
    profile_snapshot varchar(200)                       null comment '当次画像快照（身份/职位/公司，用于历史回看）',
    data_fingerprint varchar(200)                       null comment '数据指纹（流水/资产/负债/画像变更痕迹，变化自动失效快照）',
    create_time      datetime default CURRENT_TIMESTAMP not null,
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
)
    comment 'AI 财务分析报告月度快照';

create index idx_user_period
    on `moyun-db`.ledger_ai_analysis_report (user_id, period);

create table if not exists `moyun-db`.ledger_asset_account
(
    id               bigint auto_increment comment '资产账户ID'
        primary key,
    user_id          bigint                                   not null comment '门户用户ID（portal_user.id）',
    name             varchar(100)                             not null comment '账户名称，如"招商银行储蓄卡"',
    type             varchar(20)                              not null comment '类型：cash/savings/ewallet/stored_value/investment/fixed_asset/receivable/other',
    balance          decimal(18, 2) default 0.00              not null comment '当前余额（元）',
    valuation        decimal(18, 2)                           null comment '估值（元；投资/固定资产用，可≠balance）',
    include_in_total tinyint        default 1                 not null comment '是否计入总资产：1=是 0=否',
    icon             varchar(50)                              null comment '图标',
    hide_balance     tinyint        default 0                 not null comment '是否隐藏余额（隐私模式）：1=是 0=否',
    sort_order       int            default 0                 not null comment '排序',
    status           tinyint        default 1                 not null comment '状态：1=启用 0=停用归档（删除即归档，流水永久保留）',
    version          int            default 0                 not null comment '乐观锁版本号',
    create_time      datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '记账-资产账户' collate = utf8mb4_general_ci;

create index idx_user
    on `moyun-db`.ledger_asset_account (user_id, status);

create table if not exists `moyun-db`.ledger_budget
(
    id          bigint auto_increment comment '预算ID'
        primary key,
    user_id     bigint                             not null comment '门户用户ID（portal_user.id）',
    category_id bigint                             null comment '分类ID（NULL=月度总预算）',
    year        int                                not null comment '年份',
    month       int                                not null comment '月份（1-12）',
    amount      decimal(18, 2)                     not null comment '预算金额（元）',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '记账-预算' collate = utf8mb4_general_ci;

create index idx_user_period
    on `moyun-db`.ledger_budget (user_id, year, month);

create table if not exists `moyun-db`.ledger_category
(
    id          bigint auto_increment comment '分类ID'
        primary key,
    user_id     bigint   default 0                 not null comment '0=系统预设，>0=用户自定义（portal_user.id）',
    name        varchar(50)                        not null comment '分类名称',
    type        varchar(10)                        not null comment '类型：income/expense',
    group_name  varchar(50)                        null comment '语义分组（前端展示分组用）',
    parent_id   bigint                             null comment '父分类ID（支持二级分类）',
    icon        varchar(50)                        null comment '图标',
    color       varchar(20)                        null comment '颜色',
    sort_order  int      default 0                 not null comment '排序',
    is_system   tinyint  default 0                 not null comment '系统预设：1=是（仅后台可维护） 0=自定义',
    status      tinyint  default 1                 not null comment '状态：1=启用 0=停用',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '记账-分类（系统预设+用户自定义）' collate = utf8mb4_general_ci;

create index idx_user_type
    on `moyun-db`.ledger_category (user_id, type, status);

create table if not exists `moyun-db`.ledger_liability_account
(
    id               bigint auto_increment comment '负债账户ID'
        primary key,
    user_id          bigint                                   not null comment '门户用户ID（portal_user.id）',
    name             varchar(100)                             not null comment '负债名称，如"招行信用卡"',
    type             varchar(20)                              not null comment '类型：credit_card/consumer_loan/bank_loan/personal_loan/other',
    balance          decimal(18, 2) default 0.00              not null comment '当前欠款（元）',
    principal        decimal(18, 2)                           null comment '初始本金（元）',
    annual_rate      decimal(10, 4)                           null comment '年利率（%）',
    total_terms      int                                      null comment '总期数（月）',
    paid_terms       int                                      null comment '已还期数（月）',
    monthly_payment  decimal(18, 2)                           null comment '每期还款额（元）',
    repayment_day    tinyint                                  null comment '还款日（每月几号，1-28）',
    due_date         date                                     null comment '到期日',
    include_in_total tinyint        default 1                 not null comment '是否计入总负债：1=是 0=否',
    icon             varchar(50)                              null comment '图标',
    sort_order       int            default 0                 not null comment '排序',
    status           tinyint        default 1                 not null comment '状态：1=启用 0=停用归档（手动删除）',
    settle_flag      tinyint        default 0                 not null comment '已结清：1=是 0=否（还款至0自动置位；归档展示、不计入当前总负债）',
    version          int            default 0                 not null comment '乐观锁版本号',
    create_time      datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '记账-负债账户' collate = utf8mb4_general_ci;

create index idx_user
    on `moyun-db`.ledger_liability_account (user_id, status);

create table if not exists `moyun-db`.ledger_memo
(
    id             bigint auto_increment comment '备忘录ID'
        primary key,
    user_id        bigint                                not null comment '门户用户ID（portal_user.id）',
    title          varchar(100)                          not null comment '事项标题',
    content        varchar(500)                          not null comment '待办内容',
    done           tinyint     default 0                 not null comment '完成状态：1=已完成 0=未完成',
    event_time     datetime                              null comment '事项时间（提醒基准时间）',
    remind_enabled tinyint     default 0                 not null comment '是否提醒：1=是 0=否',
    remind_rule    varchar(20)                           null comment '提醒方式：on_time=准时 advance_30m=提前30分钟 advance_1h=提前1小时 advance_2h=提前2小时 advance_1d=提前1天 advance_1d_9am=提前一天上午9点',
    importance     varchar(10) default 'normal'          not null comment '重要程度：low=不重要 normal=一般 high=重要 urgent=紧急',
    reminded       tinyint     default 0                 not null comment '提醒是否已发送：1=已发 0=未发（防重复）',
    todo_date      date                                  null comment '创建日期',
    create_time    datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '记账-备忘录（待办事项）' collate = utf8mb4_general_ci;

create index idx_user_done
    on `moyun-db`.ledger_memo (user_id, done);

create index idx_user_remind
    on `moyun-db`.ledger_memo (remind_enabled, done, reminded);

create table if not exists `moyun-db`.ledger_net_worth_snapshot
(
    id              bigint auto_increment comment '快照ID'
        primary key,
    user_id         bigint                                   not null comment '门户用户ID（portal_user.id）',
    snap_date       date                                     not null comment '快照日期（每日定时任务生成；当日有记账实时upsert）',
    total_asset     decimal(18, 2) default 0.00              not null comment '总资产（元）',
    total_liability decimal(18, 2) default 0.00              not null comment '总负债（元）',
    net_worth       decimal(18, 2) default 0.00              not null comment '净资产（元）= total_asset - total_liability',
    create_time     datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_user_date
        unique (user_id, snap_date)
)
    comment '记账-净资产每日快照' collate = utf8mb4_general_ci;

create table if not exists `moyun-db`.ledger_saving_plan
(
    id             bigint auto_increment comment '计划ID'
        primary key,
    user_id        bigint                                   not null comment '门户用户ID（portal_user.id）',
    name           varchar(100)                             not null comment '计划名称，如"买房基金"',
    method         varchar(20)                              not null comment '存钱方式：52week/fixed/monthly/custom',
    target_amount  decimal(18, 2)                           not null comment '目标金额（元）',
    current_amount decimal(18, 2) default 0.00              not null comment '当前已存金额（元）',
    period_amount  decimal(18, 2)                           null comment '每期金额（元；fixed/monthly 用）',
    period_count   int                                      null comment '总期数（custom 自定义规则期数）',
    increase_step  decimal(18, 2)                           null comment '每期递增金额（元；custom 自定义递增规则用）',
    start_date     date                                     not null comment '开始日期',
    end_date       date                                     null comment '结束日期（可选）',
    remark         varchar(200)                             null comment '备注',
    status         tinyint        default 1                 not null comment '状态：1=进行中 2=成功 3=失败 0=已删除',
    create_time    datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '记账-存钱计划' collate = utf8mb4_general_ci;

create index idx_user_status
    on `moyun-db`.ledger_saving_plan (user_id, status);

create table if not exists `moyun-db`.ledger_saving_record
(
    id            bigint auto_increment comment '流水ID'
        primary key,
    plan_id       bigint                             not null comment '计划ID（ledger_saving_plan.id）',
    user_id       bigint                             not null comment '门户用户ID（冗余，数据隔离校验用）',
    period_index  int                                not null comment '期次序号（第几期/第几周）',
    target_amount decimal(18, 2)                     not null comment '本期应存金额（元）',
    amount        decimal(18, 2)                     null comment '实际存入金额（元；成功时记录）',
    status        tinyint  default 0                 not null comment '状态：0=待存 1=成功 2=失败',
    fail_reason   varchar(200)                       null comment '失败原因（如余额不足）',
    remark        varchar(200)                       null comment '备注',
    record_date   date                               null comment '存入/失败日期',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '记账-存钱流水' collate = utf8mb4_general_ci;

create index idx_plan
    on `moyun-db`.ledger_saving_record (plan_id, period_index);

create index idx_user
    on `moyun-db`.ledger_saving_record (user_id);

create table if not exists `moyun-db`.ledger_schedule_log
(
    id             bigint auto_increment comment '日志ID'
        primary key,
    task_id        bigint                             not null comment '任务ID（ledger_schedule_task.id）',
    user_id        bigint                             not null comment '门户用户ID（冗余）',
    exec_date      date                               not null comment '执行日期',
    amount         decimal(18, 2)                     not null comment '金额（元）',
    status         tinyint                            not null comment '状态：1=成功 2=失败',
    fail_reason    varchar(200)                       null comment '失败原因（如余额不足）',
    transaction_id bigint                             null comment '生成的流水ID（ledger_transaction.id）',
    retry_count    int      default 0                 not null comment '重试次数',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '记账-定时记账执行日志' collate = utf8mb4_general_ci;

create index idx_task
    on `moyun-db`.ledger_schedule_log (task_id, exec_date);

create index idx_user
    on `moyun-db`.ledger_schedule_log (user_id);

create table if not exists `moyun-db`.ledger_schedule_task
(
    id             bigint auto_increment comment '任务ID'
        primary key,
    user_id        bigint                               not null comment '门户用户ID（portal_user.id）',
    name           varchar(100)                         not null comment '任务名称',
    type           varchar(20)                          not null comment '记账类型：income/expense',
    amount         decimal(18, 2)                       not null comment '金额（元）',
    category_id    bigint                               null comment '分类ID（ledger_category）',
    account_id     bigint                               null comment '关联资产账户ID（支出扣款/收入入账）',
    description    varchar(200)                         null comment '备注',
    cycle          varchar(20)                          not null comment '执行周期：daily/weekly/monthly/interval',
    day_of_week    tinyint                              null comment '每周几（1-7，weekly 用）',
    day_of_month   tinyint                              null comment '每月几号（1-28，monthly 用）',
    interval_days  int                                  null comment '间隔天数（interval 用）',
    exec_time      varchar(5) default '08:00'           not null comment '执行时间 HH:mm',
    start_date     date                                 not null comment '开始日期',
    end_date       date                                 null comment '结束日期（可选，到期自动停用）',
    next_exec_date date                                 null comment '下次执行日期',
    enabled        tinyint    default 1                 not null comment '启用状态：1=启用 0=停用',
    status         tinyint    default 1                 not null comment '状态：1=正常 0=已删除',
    create_time    datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '记账-定时记账任务' collate = utf8mb4_general_ci;

create index idx_next_exec
    on `moyun-db`.ledger_schedule_task (enabled, status, next_exec_date);

create index idx_user_status
    on `moyun-db`.ledger_schedule_task (user_id, status);

create table if not exists `moyun-db`.ledger_tip_order
(
    id          bigint auto_increment comment '打赏单ID'
        primary key,
    user_id     bigint                                not null comment '门户用户ID（portal_user.id）',
    amount      decimal(18, 2)                        not null comment '打赏金额（元）',
    target      varchar(20) default 'developer'       not null comment '打赏对象：developer=开发者 platform=平台',
    reason      varchar(200)                          null comment '打赏理由（可选）',
    pay_way     varchar(20) default 'wechat'          not null comment '支付方式：wechat/alipay',
    status      tinyint     default 1                 not null comment '状态：1=成功 0=已撤销',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '打赏时间'
)
    comment '记账-打赏记录' collate = utf8mb4_general_ci;

create index idx_user
    on `moyun-db`.ledger_tip_order (user_id, create_time);

create table if not exists `moyun-db`.ledger_transaction
(
    id                      bigint auto_increment comment '流水ID'
        primary key,
    user_id                 bigint                             not null comment '门户用户ID（portal_user.id）',
    type                    varchar(20)                        not null comment '类型：income/expense/transfer/repayment/borrow/adjust',
    amount                  decimal(18, 2)                     not null comment '金额（元；adjust可为负表示调减，其余恒为正，方向由type决定）',
    category_id             bigint                             null comment '分类ID（ledger_category）',
    account_id              bigint                             null comment '关联资产账户（支出/收入/转出方/还款扣款方/校准账户）',
    liability_id            bigint                             null comment '关联负债账户（还款/借款）',
    target_account_id       bigint                             null comment '转账目标资产账户',
    balance_after           decimal(18, 2)                     null comment '主账户交易后余额快照（元）',
    target_balance_after    decimal(18, 2)                     null comment '转账目标账户交易后余额快照（元）',
    liability_balance_after decimal(18, 2)                     null comment '关联负债交易后欠款快照（元）',
    description             varchar(200)                       null comment '备注',
    transaction_date        date                               not null comment '交易日期（默认当天）',
    transaction_time        time                               null comment '交易时间',
    merchant                varchar(100)                       null comment '商户名称',
    voucher_url             varchar(500)                       null comment '凭证截图URL（门户文件服务地址）',
    is_budget               tinyint  default 1                 not null comment '是否计入预算：1=是 0=否（adjust 默认0）',
    status                  tinyint  default 1                 not null comment '状态：1=正常 0=已删除（冲正后归档）',
    client_uuid             varchar(64)                        null comment '客户端幂等键（Phase 4 离线同步防重复提交）',
    create_by               varchar(64)                        null comment '创建人（门户用户名/admin 代改标识）',
    create_time             datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time             datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_client_uuid
        unique (client_uuid)
)
    comment '记账-流水' collate = utf8mb4_general_ci;

create index idx_account
    on `moyun-db`.ledger_transaction (account_id);

create index idx_liability
    on `moyun-db`.ledger_transaction (liability_id);

create index idx_user_date
    on `moyun-db`.ledger_transaction (user_id, transaction_date);

create index idx_user_status_id
    on `moyun-db`.ledger_transaction (user_id, status, id);

create table if not exists `moyun-db`.pay_ledger_entry
(
    id            bigint auto_increment comment '流水ID'
        primary key,
    pay_no        varchar(40)                        not null comment '关联支付单号',
    biz_type      varchar(32)                        not null comment '业务类型：tip / withdraw',
    biz_no        varchar(64)                        not null comment '业务单号',
    account_role  varchar(16)                        not null comment '账户角色：USER=用户 / PLATFORM=平台',
    user_id       bigint                             null comment '用户ID（PLATFORM 分录为 NULL）',
    direction     varchar(8)                         not null comment '方向：credit=收入 / debit=支出',
    amount        bigint                             not null comment '金额（分）',
    balance_after bigint                             null comment '交易后余额（分；PLATFORM 分录不追踪余额，为 NULL）',
    summary       varchar(255)                       not null comment '业务摘要，如"打赏收入-作者所得" / "平台服务费"',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '分账流水（复式记账）' collate = utf8mb4_general_ci;

create index idx_pay_no
    on `moyun-db`.pay_ledger_entry (pay_no);

create index idx_role
    on `moyun-db`.pay_ledger_entry (account_role, direction);

create index idx_user
    on `moyun-db`.pay_ledger_entry (user_id, create_time);

create table if not exists `moyun-db`.pay_notification
(
    id          bigint auto_increment comment '通知ID'
        primary key,
    user_id     bigint                             not null comment '接收用户',
    notify_type varchar(16)                        not null comment '通知类型：pay=支付结果 / withdraw=提现 / account=账户',
    ref_no      varchar(64)                        null comment '关联单号（支付单/提现单）',
    title       varchar(128)                       not null comment '通知标题',
    content     varchar(512)                       not null comment '通知内容',
    read_flag   tinyint  default 0                 not null comment '已读：0=未读 1=已读',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '支付站内通知' collate = utf8mb4_general_ci;

create index idx_user_read
    on `moyun-db`.pay_notification (user_id, read_flag, create_time);

create table if not exists `moyun-db`.pay_notify_log
(
    id          bigint auto_increment comment '日志ID'
        primary key,
    channel     varchar(32)                        not null comment '渠道：wechat',
    pay_no      varchar(40)                        null comment '关联支付单（解析成功后回填）',
    raw_body    varchar(2048)                      null comment '回调报文（截断留存）',
    verify_ok   tinyint  default 0                 not null comment '验签结果：1=通过 0=失败',
    handled     tinyint  default 0                 not null comment '业务处理：1=成功 0=失败',
    error_msg   varchar(512)                       null comment '失败原因',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '渠道回调日志' collate = utf8mb4_general_ci;

create index idx_pay_no
    on `moyun-db`.pay_notify_log (pay_no);

create table if not exists `moyun-db`.pay_order
(
    id               bigint auto_increment comment 'id'
        primary key,
    pay_no           varchar(40)                           not null comment '支付单号（全局唯一，如 PAY20260902xxxx）',
    biz_type         varchar(32)                           not null comment '业务类型：tip=打赏 / member=会员 / course=课程（后续扩展）',
    biz_no           varchar(64)                           not null comment '业务单号（如打赏单ID）',
    channel          varchar(32)                           not null comment '支付渠道：wechat / alipay（预留）',
    amount           bigint                                not null comment '支付金额（分）',
    subject          varchar(128)                          not null comment '商品描述',
    status           varchar(16) default 'CREATED'         not null comment '状态机：CREATED→PAID→SETTLED / CREATED→CLOSED',
    code_url         varchar(512)                          null comment '微信 native 支付二维码链接（code_url）',
    channel_order_no varchar(64)                           null comment '三方交易单号（微信 transaction_id）',
    trade_state      varchar(32)                           null comment '三方交易状态（微信 trade_state：SUCCESS/NOTPAY/CLOSED等）',
    expire_time      datetime                              null comment '订单过期时间（超时未支付自动关单依据）',
    pay_success_time datetime                              null comment '支付成功时间',
    settle_time      datetime                              null comment '分账完成时间',
    closed_time      datetime                              null comment '关单时间',
    close_reason     varchar(64)                           null comment '关单原因：TIMEOUT / ADMIN_MANUAL_CLOSE',
    create_time      datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint pay_no
        unique (pay_no)
)
    comment '支付订单（公共支付通道）' collate = utf8mb4_general_ci;

create index idx_biz
    on `moyun-db`.pay_order (biz_type, biz_no);

create index idx_channel_order
    on `moyun-db`.pay_order (channel_order_no);

create index idx_status_expire
    on `moyun-db`.pay_order (status, expire_time);

create table if not exists `moyun-db`.pay_user_account
(
    user_id        bigint                             not null comment '用户ID（sys_user.user_id）'
        primary key,
    balance        bigint   default 0                 not null comment '可用余额（分）',
    total_income   bigint   default 0                 not null comment '累计收入（分，含打赏所得）',
    total_withdraw bigint   default 0                 not null comment '累计提现（分）',
    version        int      default 0                 not null comment '乐观锁版本号',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '用户资金账户（钱包）' collate = utf8mb4_general_ci;

create table if not exists `moyun-db`.pay_user_bank_card
(
    id                bigint auto_increment comment '银行卡ID'
        primary key,
    user_id           bigint                                not null comment '所属用户',
    holder_name       varchar(64)                           not null comment '持卡人姓名',
    card_no_encrypted varchar(512)                          not null comment '卡号密文（AES-GCM）',
    card_no_masked    varchar(32)                           not null comment '卡号脱敏（6217 **** **** 1234）',
    phone_encrypted   varchar(512)                          null comment '预留手机号密文（AES-GCM）',
    bank_code         varchar(32)                           null comment '银行编码（如 ICBC）',
    bank_name         varchar(64)                           not null comment '银行名称（如 中国工商银行）',
    is_default        tinyint     default 0                 not null comment '是否默认卡：1=是 0=否',
    verify_status     varchar(16) default 'VERIFIED'        not null comment '验证状态：VERIFIED=已验证 / PENDING=待验证',
    create_time       datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time       datetime    default CURRENT_TIMESTAMP not null comment '修改时间',
    constraint uk_user_card
        unique (user_id, card_no_masked)
)
    comment '用户银行卡（密文落库）' collate = utf8mb4_general_ci;

create index idx_user
    on `moyun-db`.pay_user_bank_card (user_id);

create table if not exists `moyun-db`.pay_withdraw_order
(
    id            bigint auto_increment comment '提现单ID'
        primary key,
    withdraw_no   varchar(40)                           not null comment '提现单号',
    user_id       bigint                                not null comment '用户ID',
    bank_card_id  bigint                                not null comment '收款银行卡ID',
    amount        bigint                                not null comment '提现金额（分）',
    fee           bigint      default 0                 not null comment '手续费（分）',
    status        varchar(16) default 'AUDITING'        not null comment '状态：AUDITING=审核中 / PAID=已打款 / REJECTED=已驳回',
    audit_time    datetime                              null comment '审核时间',
    reject_reason varchar(255)                          null comment '驳回原因',
    create_time   datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_withdraw_no
        unique (withdraw_no)
)
    comment '提现订单（预留）' collate = utf8mb4_general_ci;

create index idx_user
    on `moyun-db`.pay_withdraw_order (user_id, create_time);

create table if not exists `moyun-db`.portal_achievement
(
    id             bigint unsigned auto_increment comment '主键'
        primary key,
    code           varchar(64)                           not null comment '成就编码',
    name           varchar(100)                          not null comment '成就名称',
    description    varchar(255)                          null comment '成就描述',
    icon           varchar(500)                          null comment '图标URL',
    module         varchar(32)                           null comment '所属模块: article/reading/interview/all',
    condition_json text                                  null comment '达成条件JSON',
    growth_reward  int         default 0                 null comment '达成奖励成长值',
    sort           int         default 0                 null comment '排序',
    status         char        default '0'               null comment '状态（0启用 1停用）',
    create_by      varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by      varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                          null comment '备注',
    constraint uk_code
        unique (code)
)
    comment '成就定义表' charset = utf8mb4;

create table if not exists `moyun-db`.portal_ad_slot
(
    id          bigint auto_increment comment '广告位ID'
        primary key,
    slot_key    varchar(64)                           not null comment '广告位标识，如 article_detail_bottom',
    title       varchar(100)                          not null comment '广告标题',
    image       varchar(500)                          null comment '广告图片URL',
    link        varchar(500)                          null comment '点击跳转链接',
    open_target varchar(10) default '_blank'          null comment '链接打开方式：_blank=新窗口（默认），_self=当前页',
    content     varchar(500)                          null comment '广告文案',
    sort        int         default 0                 null comment '排序',
    status      varchar(1)  default '0'               null comment '状态：0=启用 1=停用',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    del_flag    char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '门户自研广告位表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_ad_slot (del_flag);

create index idx_slot_key
    on `moyun-db`.portal_ad_slot (slot_key);

create index idx_status
    on `moyun-db`.portal_ad_slot (status);

create table if not exists `moyun-db`.portal_ai_task
(
    id           bigint auto_increment comment '任务ID'
        primary key,
    user_id      bigint                                not null comment '所属用户',
    task_type    varchar(50)                           not null comment '任务类型：resume_parse（简历解析）/ job_match（岗位匹配）/ ai_draft（空字段草稿）/ deep_optimize（深度优化）',
    biz_ref      varchar(500)                          null comment '业务参数JSON，如 {"resumeId":1,"jobTargetId":2}',
    status       varchar(20) default 'pending'         not null comment '状态：pending（排队）/ running（执行中）/ success（成功）/ failed（失败）',
    progress_msg varchar(500)                          null comment '进度提示文案（轮询时返回给前端展示）',
    result       mediumtext                            null comment '任务结果JSON（success 时有值）',
    error        varchar(1000)                         null comment '失败原因（failed 时有值）',
    create_time  datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    finish_time  datetime                              null comment '完成时间（成功或失败）'
)
    comment '通用AI异步任务表' collate = utf8mb4_general_ci;

create index idx_status
    on `moyun-db`.portal_ai_task (status);

create index idx_user_type
    on `moyun-db`.portal_ai_task (user_id, task_type);

create table if not exists `moyun-db`.portal_article
(
    id                      bigint auto_increment comment '文章ID'
        primary key,
    title                   varchar(500)                             not null comment '文章标题',
    slug                    varchar(500)                             null comment '文章URL别名，用于SEO语义化路径',
    content                 longtext                                 null comment '文章内容（HTML格式）',
    excerpt                 varchar(1000)                            null comment '文章摘要',
    cover                   text                                     null comment '封面图片URL或Base64',
    author_id               bigint                                   not null comment '作者ID（门户用户ID）',
    category_id             bigint                                   null comment '分类ID',
    root_category_id        bigint                                   null comment '顶级分类ID',
    status                  varchar(20)    default 'draft'           null comment '状态：draft=草稿 / pending=待审核 / published=已发布 / rejected=已拒绝 / archived=已归档',
    auditor_id              bigint                                   null comment '审核人ID（系统用户ID）',
    audit_remark            varchar(500)                             null comment '审核意见/驳回原因',
    audit_time              datetime                                 null comment '审核时间',
    is_featured             tinyint(1)     default 0                 null comment '是否精选',
    is_top                  tinyint(1)     default 0                 null comment '是否置顶',
    is_carousel             tinyint(1)     default 0                 null comment '是否轮播',
    is_category_recommended tinyint(1)     default 0                 null comment '是否栏目推荐',
    views                   bigint         default 0                 null comment '浏览量',
    likes                   bigint         default 0                 null comment '点赞数',
    comments                bigint         default 0                 null comment '评论数',
    share_count             bigint         default 0                 null comment '分享数',
    bookmark_count          bigint         default 0                 null comment '收藏数',
    published_at            datetime                                 null comment '发布时间',
    link                    varchar(500)                             null comment '外部链接',
    editor_mode             varchar(20)    default 'richtext'        null comment '编辑器模式：richtext/markdown',
    session_token           varchar(64)                              null comment '编辑会话标识（一次编辑会话唯一，用于草稿/发布幂等去重）',
    content_markdown        text                                     null comment 'Markdown 原始内容',
    create_by               varchar(64)    default ''                null comment '创建者',
    create_time             datetime       default CURRENT_TIMESTAMP null comment '创建时间',
    update_by               varchar(64)    default ''                null comment '更新者',
    update_time             datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark                  varchar(500)                             null comment '备注',
    category_path           varchar(500)                             null comment '分类路径，包含所有祖先ID，例如：1,3,5',
    is_paid                 tinyint        default 0                 not null comment '是否付费阅读 0=免费 1=付费',
    paid_content            longtext                                 null comment '付费内容（购买后可见）',
    preview_length          int            default 0                 not null comment '试读字数（未购买可预览的字数）',
    price                   decimal(10, 2) default 0.00              not null comment '付费价格，0=免费',
    del_flag                char           default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_slug
        unique (slug)
)
    comment '门户文章表' charset = utf8mb4;

create index idx_auditor_id
    on `moyun-db`.portal_article (auditor_id);

create index idx_author_id
    on `moyun-db`.portal_article (author_id);

create index idx_category_id
    on `moyun-db`.portal_article (category_id);

create index idx_category_path
    on `moyun-db`.portal_article (category_path(100));

create index idx_del_flag
    on `moyun-db`.portal_article (del_flag);

create index idx_is_category_recommended
    on `moyun-db`.portal_article (is_category_recommended);

create index idx_is_featured
    on `moyun-db`.portal_article (is_featured);

create index idx_is_top
    on `moyun-db`.portal_article (is_top);

create index idx_likes
    on `moyun-db`.portal_article (likes);

create index idx_published_at
    on `moyun-db`.portal_article (published_at);

create index idx_root_category_id
    on `moyun-db`.portal_article (root_category_id);

create index idx_session_token
    on `moyun-db`.portal_article (session_token);

create index idx_status
    on `moyun-db`.portal_article (status);

create index idx_status_published_at
    on `moyun-db`.portal_article (status, published_at);

create index idx_views
    on `moyun-db`.portal_article (views);

create table if not exists `moyun-db`.portal_article_version
(
    id               bigint auto_increment comment '主键'
        primary key,
    article_id       bigint                             not null comment '文章ID',
    version_no       int                                not null comment '版本号（同一文章内自增）',
    title            varchar(256)                       not null comment '版本标题快照',
    content          longtext                           null comment '版本内容快照（HTML）',
    content_markdown longtext                           null comment '版本 Markdown 原始内容快照',
    excerpt          varchar(500)                       null comment '版本摘要快照',
    operator_id      bigint                             null comment '操作人ID（保存/回滚的执行者）',
    created_time     datetime default CURRENT_TIMESTAMP null comment '版本创建时间'
)
    comment '文章版本快照' charset = utf8mb4;

create index idx_article_version
    on `moyun-db`.portal_article_version (article_id, version_no);

create table if not exists `moyun-db`.portal_article_view
(
    id          bigint auto_increment comment '记录ID'
        primary key,
    article_id  bigint                                not null comment '文章ID',
    user_id     bigint                                null comment '用户ID（NULL表示游客）',
    ip          varchar(50)                           null comment 'IP地址',
    view_time   datetime    default CURRENT_TIMESTAMP null comment '浏览时间',
    user_agent  varchar(500)                          null comment '浏览器User-Agent',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注'
)
    comment '文章浏览记录表' charset = utf8mb4;

create index idx_article_id
    on `moyun-db`.portal_article_view (article_id);

create index idx_article_ip
    on `moyun-db`.portal_article_view (article_id, ip);

create index idx_article_user
    on `moyun-db`.portal_article_view (article_id, user_id);

create index idx_article_viewtime
    on `moyun-db`.portal_article_view (article_id, view_time);

create index idx_ip
    on `moyun-db`.portal_article_view (ip);

create index idx_user_id
    on `moyun-db`.portal_article_view (user_id);

create index idx_view_time
    on `moyun-db`.portal_article_view (view_time);

create table if not exists `moyun-db`.portal_book
(
    id                   bigint auto_increment comment '主键'
        primary key,
    title                varchar(500)                             not null comment '书名',
    author               varchar(200)                             not null comment '作者',
    cover                varchar(500)                             null comment '封面URL',
    description          text                                     null comment '简介',
    isbn                 varchar(50)                              null comment 'ISBN',
    publisher            varchar(200)                             null comment '出版社',
    publish_date         date                                     null comment '出版日期',
    page_count           int            default 0                 null comment '页数',
    category_id          bigint                                   null comment '分类ID',
    tags                 varchar(500)                             null comment '标签，逗号分隔',
    rating               decimal(3, 2)  default 0.00              null comment '评分',
    reading_count        bigint         default 0                 null comment '阅读人数',
    status               varchar(20)    default 'active'          null comment '状态:active,inactive',
    type                 varchar(20)    default 'published'       null comment '书籍类型：published=出版物，novel=网络小说，longform=长文',
    serial_status        varchar(20)    default 'completed'       null comment '连载状态：ongoing=连载中，completed=已完结，hiatus=暂停更新',
    word_count           bigint         default 0                 null comment '总字数（章节字数之和）',
    chapter_count        int            default 0                 null comment '总章节数',
    latest_chapter_id    bigint                                   null comment '最新章节ID（用于追更展示）',
    latest_chapter_title varchar(500)                             null comment '最新章节标题',
    last_update_time     datetime                                 null comment '最后更新时间（章节发布时同步）',
    is_finished          tinyint(1)     default 1                 null comment '是否完结：1=完结，0=连载中（冗余字段，便于查询）',
    access_level         varchar(20)    default 'free'            null comment '访问级别:free,vip,preview',
    preview_ratio        int            default 30                null comment '免费试读比例（0-100）',
    price                decimal(10, 2) default 0.00              null comment '书籍单价（元）',
    is_featured          tinyint(1)     default 0                 null comment '是否精选',
    is_recommended       tinyint(1)     default 0                 null comment '是否推荐',
    summary              text                                     null comment '简介（纯文本）',
    author_bio           text                                     null comment '作者简介',
    create_by            varchar(64)    default ''                null comment '创建者',
    create_time          datetime       default CURRENT_TIMESTAMP null comment '创建时间',
    update_by            varchar(64)    default ''                null comment '更新者',
    update_time          datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark               varchar(500)                             null comment '备注',
    del_flag             char           default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '书籍表' charset = utf8mb4;

create index idx_access_level
    on `moyun-db`.portal_book (access_level);

create index idx_category_id
    on `moyun-db`.portal_book (category_id);

create index idx_del_flag
    on `moyun-db`.portal_book (del_flag);

create index idx_is_featured
    on `moyun-db`.portal_book (is_featured);

create index idx_is_finished
    on `moyun-db`.portal_book (is_finished);

create index idx_is_recommended
    on `moyun-db`.portal_book (is_recommended);

create index idx_last_update_time
    on `moyun-db`.portal_book (last_update_time);

create index idx_serial_status
    on `moyun-db`.portal_book (serial_status);

create index idx_status
    on `moyun-db`.portal_book (status);

create index idx_title
    on `moyun-db`.portal_book (title);

create index idx_type
    on `moyun-db`.portal_book (type);

create index idx_word_count
    on `moyun-db`.portal_book (word_count);

create table if not exists `moyun-db`.portal_book_chapter
(
    id               bigint auto_increment comment '主键'
        primary key,
    book_id          bigint                                   not null comment '所属书籍ID',
    title            varchar(500)                             not null comment '章节标题',
    content          longtext                                 null comment '章节正文（HTML格式，上限4GB）',
    content_markdown text                                     null comment 'Markdown原始内容（上限64KB，单章足够）',
    editor_mode      varchar(20)    default 'richtext'        null comment '编辑器模式：richtext/markdown',
    word_count       int            default 0                 null comment '字数统计',
    chapter_no       int            default 0                 not null comment '章节序号（用于排序，从1开始）',
    volume_id        bigint                                   null comment '所属分卷ID（可选，支持分卷管理）',
    is_free          tinyint(1)     default 1                 null comment '是否免费：1=免费，0=VIP章节',
    price            decimal(10, 2) default 0.00              null comment '章节单价（元，VIP章节购买）',
    is_published     tinyint(1)     default 0                 null comment '是否已发布：0=草稿，1=已发布',
    publish_time     datetime                                 null comment '发布时间（支持定时发布）',
    view_count       bigint         default 0                 null comment '章节浏览量',
    create_by        varchar(64)    default ''                null comment '创建者',
    create_time      datetime       default CURRENT_TIMESTAMP null comment '创建时间',
    update_by        varchar(64)    default ''                null comment '更新者',
    update_time      datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark           varchar(500)                             null comment '备注',
    del_flag         char           default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_book_chapter_no
        unique (book_id, chapter_no)
)
    comment '书籍章节表' charset = utf8mb4;

create index idx_book_id
    on `moyun-db`.portal_book_chapter (book_id);

create index idx_del_flag
    on `moyun-db`.portal_book_chapter (del_flag);

create index idx_is_published
    on `moyun-db`.portal_book_chapter (is_published);

create index idx_publish_time
    on `moyun-db`.portal_book_chapter (publish_time);

create table if not exists `moyun-db`.portal_book_chapter_view
(
    id               bigint auto_increment comment '主键'
        primary key,
    chapter_id       bigint                             not null comment '章节ID',
    book_id          bigint                             not null comment '书籍ID',
    user_id          bigint                             null comment '用户ID（未登录为NULL）',
    client_ip        varchar(50)                        null comment '客户端IP',
    read_duration_ms int      default 0                 null comment '阅读时长（毫秒）',
    create_time      datetime default CURRENT_TIMESTAMP null comment '浏览时间',
    del_flag         char     default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '章节浏览记录表' charset = utf8mb4;

create index idx_chapter_id
    on `moyun-db`.portal_book_chapter_view (chapter_id);

create index idx_create_time
    on `moyun-db`.portal_book_chapter_view (create_time);

create index idx_del_flag
    on `moyun-db`.portal_book_chapter_view (del_flag);

create index idx_user_id
    on `moyun-db`.portal_book_chapter_view (user_id);

create table if not exists `moyun-db`.portal_book_list
(
    id           bigint auto_increment comment '主键'
        primary key,
    title        varchar(500)                          not null comment '书单标题',
    description  text                                  null comment '书单简介',
    cover        varchar(500)                          null comment '封面URL',
    user_id      bigint                                not null comment '创建者ID',
    category_id  bigint                                null comment '分类ID',
    is_public    tinyint(1)  default 1                 null comment '是否公开',
    book_count   int         default 0                 null comment '书籍数量',
    view_count   bigint      default 0                 null comment '浏览数',
    like_count   bigint      default 0                 null comment '点赞数',
    status       varchar(20) default 'active'          null comment '状态:active,inactive',
    is_featured  tinyint(1)  default 0                 null comment '是否精选',
    access_level varchar(20) default 'free'            null comment '访问级别:free,vip',
    tags         varchar(500)                          null comment '标签（逗号分隔）',
    create_by    varchar(64) default ''                null comment '创建者',
    create_time  datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by    varchar(64) default ''                null comment '更新者',
    update_time  datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark       varchar(500)                          null comment '备注',
    del_flag     char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '书单表' charset = utf8mb4;

create index idx_category_id
    on `moyun-db`.portal_book_list (category_id);

create index idx_del_flag
    on `moyun-db`.portal_book_list (del_flag);

create index idx_is_featured
    on `moyun-db`.portal_book_list (is_featured);

create index idx_status
    on `moyun-db`.portal_book_list (status);

create index idx_user_id
    on `moyun-db`.portal_book_list (user_id);

create table if not exists `moyun-db`.portal_book_list_bookmark
(
    id          bigint auto_increment comment '主键'
        primary key,
    booklist_id bigint                                not null comment '书单ID',
    user_id     bigint                                not null comment '用户ID',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '收藏时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    constraint uk_booklist_user
        unique (booklist_id, user_id)
)
    comment '书单收藏表' charset = utf8mb4;

create index idx_user_id
    on `moyun-db`.portal_book_list_bookmark (user_id);

create table if not exists `moyun-db`.portal_book_list_item
(
    id           bigint auto_increment comment '主键'
        primary key,
    book_list_id bigint                                not null comment '书单ID',
    book_id      bigint                                not null comment '书籍ID',
    sort         int         default 0                 null comment '排序',
    note         text                                  null comment '添加说明',
    create_by    varchar(64) default ''                null comment '创建者',
    create_time  datetime    default CURRENT_TIMESTAMP null comment '添加时间',
    update_by    varchar(64) default ''                null comment '更新者',
    update_time  datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark       varchar(500)                          null comment '备注'
)
    comment '书单-书籍关联表' charset = utf8mb4;

create index idx_book_id
    on `moyun-db`.portal_book_list_item (book_id);

create index idx_book_list_id
    on `moyun-db`.portal_book_list_item (book_list_id);

create table if not exists `moyun-db`.portal_book_list_like
(
    id           bigint auto_increment comment '主键'
        primary key,
    book_list_id bigint                                not null comment '书单ID',
    user_id      bigint                                not null comment '用户ID',
    create_by    varchar(64) default ''                null comment '创建者',
    create_time  datetime    default CURRENT_TIMESTAMP null comment '点赞时间',
    update_by    varchar(64) default ''                null comment '更新者',
    update_time  datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark       varchar(500)                          null comment '备注',
    constraint uk_list_user
        unique (book_list_id, user_id)
)
    comment '书单点赞表' charset = utf8mb4;

create table if not exists `moyun-db`.portal_book_quote
(
    id          bigint auto_increment comment '主键'
        primary key,
    user_id     bigint                                not null comment '用户ID',
    book_id     bigint                                not null comment '书籍ID',
    chapter_id  bigint                                null comment '章节ID（关联 portal_book_chapter）',
    content     text                                  not null comment '金句内容',
    page        varchar(100)                          null comment '页码',
    chapter     varchar(200)                          null comment '章节',
    like_count  bigint      default 0                 null comment '点赞数',
    is_public   tinyint(1)  default 1                 null comment '是否公开',
    is_featured tinyint(1)  default 0                 null comment '是否精选',
    location    varchar(200)                          null comment '章节标题/位置描述',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    del_flag    char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '金句摘录表' charset = utf8mb4;

create index idx_book_id
    on `moyun-db`.portal_book_quote (book_id);

create index idx_chapter_id
    on `moyun-db`.portal_book_quote (chapter_id);

create index idx_del_flag
    on `moyun-db`.portal_book_quote (del_flag);

create index idx_is_featured
    on `moyun-db`.portal_book_quote (is_featured);

create index idx_is_public
    on `moyun-db`.portal_book_quote (is_public);

create index idx_user_id
    on `moyun-db`.portal_book_quote (user_id);

create table if not exists `moyun-db`.portal_book_quote_like
(
    id          bigint auto_increment comment '主键'
        primary key,
    quote_id    bigint                                not null comment '金句ID',
    user_id     bigint                                not null comment '用户ID',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '点赞时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    constraint uk_quote_user
        unique (quote_id, user_id)
)
    comment '金句点赞表' charset = utf8mb4;

create table if not exists `moyun-db`.portal_book_recommend
(
    id          bigint auto_increment comment '主键'
        primary key,
    book_id     bigint                                not null comment '书籍ID',
    position    varchar(50)                           not null comment '推荐位置：home_banner=首页轮播 / home_hot=首页热门 / category_top=分类顶推 / limit_free=限免专区 / discover_banner=发现页轮播',
    sort        int         default 0                 null comment '排序（越小越靠前）',
    start_time  datetime                              null comment '推荐开始时间（NULL 表示立即生效）',
    end_time    datetime                              null comment '推荐结束时间（NULL 表示长期有效）',
    is_active   tinyint(1)  default 1                 null comment '是否生效：1=生效，0=下架',
    remark      varchar(500)                          null comment '备注（运营说明）',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    del_flag    char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_book_position
        unique (book_id, position)
)
    comment '书籍推荐位表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_book_recommend (del_flag);

create index idx_is_active
    on `moyun-db`.portal_book_recommend (is_active);

create index idx_position
    on `moyun-db`.portal_book_recommend (position);

create index idx_time_window
    on `moyun-db`.portal_book_recommend (start_time, end_time);

create table if not exists `moyun-db`.portal_bookmark
(
    id          bigint auto_increment comment '收藏ID'
        primary key,
    user_id     bigint                                not null comment '用户ID（门户用户ID）',
    article_id  bigint                                not null comment '文章ID',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    constraint uk_user_article
        unique (user_id, article_id)
)
    comment '门户收藏表' charset = utf8mb4;

create index idx_article_id
    on `moyun-db`.portal_bookmark (article_id);

create index idx_user_id
    on `moyun-db`.portal_bookmark (user_id);

create table if not exists `moyun-db`.portal_bookshelf
(
    id              bigint auto_increment comment '主键'
        primary key,
    user_id         bigint                                not null comment '用户ID',
    book_id         bigint                                not null comment '书籍ID',
    last_chapter_id bigint                                null comment '最后阅读章节ID（冗余，用于续读）',
    last_chapter_no int         default 0                 null comment '最后阅读章节序号',
    sort            int         default 0                 null comment '排序（用户自定义书架顺序，越大越靠前）',
    create_by       varchar(64) default ''                null comment '创建者',
    create_time     datetime    default CURRENT_TIMESTAMP null comment '收藏时间',
    update_by       varchar(64) default ''                null comment '更新者',
    update_time     datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark          varchar(500)                          null comment '备注',
    del_flag        char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_user_book
        unique (user_id, book_id)
)
    comment '用户书架（收藏书籍）表' charset = utf8mb4;

create index idx_book_id
    on `moyun-db`.portal_bookshelf (book_id);

create index idx_del_flag
    on `moyun-db`.portal_bookshelf (del_flag);

create index idx_user_id
    on `moyun-db`.portal_bookshelf (user_id);

create table if not exists `moyun-db`.portal_category
(
    id             bigint auto_increment comment '分类ID'
        primary key,
    name           varchar(100)                          not null comment '分类名称',
    slug           varchar(100)                          null comment '分类别名',
    description    varchar(500)                          null comment '分类描述',
    icon           varchar(500)                          null comment '图标URL',
    sort           int         default 0                 null comment '排序',
    parent_id      bigint      default 0                 null comment '父分类ID',
    status         char        default '0'               null comment '状态（0正常 1停用）',
    show_in_nav    tinyint(1)  default 0                 not null comment '是否在头部栏目展示（0否/1是）',
    nav_route_type varchar(20) default 'category'        not null comment '路由类型（home/category/static/external）',
    nav_route_path varchar(200)                          null comment '静态/外链路由路径（仅 static/external 类型使用）',
    nav_badge      varchar(20)                           null comment '导航徽章（NEW/HOT，仅 Mega Menu 展示）',
    category_type  varchar(20) default 'article'         not null comment '栏目内容类型（article=文章分类可发布文章 directory=目录容器仅组织子栏目不发布文章 special=静态页面不发布文章）',
    requires_auth  tinyint(1)  default 0                 not null comment '是否需要登录（0否/1是）',
    create_by      varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by      varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                          null comment '备注',
    del_flag       char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '门户分类表' charset = utf8mb4;

create index idx_category_type
    on `moyun-db`.portal_category (category_type);

create index idx_del_flag
    on `moyun-db`.portal_category (del_flag);

create index idx_parent_id
    on `moyun-db`.portal_category (parent_id);

create index idx_show_in_nav
    on `moyun-db`.portal_category (show_in_nav);

create index idx_slug
    on `moyun-db`.portal_category (slug);

create table if not exists `moyun-db`.portal_code_run
(
    id          bigint auto_increment comment '主键'
        primary key,
    user_id     bigint                                not null comment '运行者用户ID',
    language    varchar(16)                           not null comment '编程语言 java/python/javascript',
    code        mediumtext                            not null comment '用户提交的源代码',
    stdin       text                                  null comment '标准输入内容',
    output      mediumtext                            null comment '标准输出（截断至 1MB）',
    error_msg   mediumtext                            null comment '错误输出 / 编译错误信息',
    status      varchar(16) default 'running'         not null comment '运行状态 running/success/failed/timeout',
    runtime_ms  int                                   null comment '运行耗时（毫秒）',
    mem_kb      int                                   null comment '内存占用（KB，粗略估算）',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '代码运行记录' charset = utf8mb4;

create index idx_user_time
    on `moyun-db`.portal_code_run (user_id, create_time);

create table if not exists `moyun-db`.portal_column
(
    id              bigint auto_increment comment '主键'
        primary key,
    user_id         bigint                                   not null comment '创作者',
    title           varchar(128)                             not null comment '专栏名',
    subtitle        varchar(256)                             null comment '副标题',
    description     text                                     null comment '专栏简介',
    cover           varchar(500)                             null comment '封面',
    category_id     bigint                                   null comment '分类',
    status          varchar(16)    default 'draft'           not null comment '状态：draft 草稿/pending 待审核/published 已发布/archived 归档/rejected 审核驳回',
    auditor_id      bigint                                   null comment '审核人ID（系统用户ID）',
    audit_remark    varchar(500)                             null comment '审核意见/驳回原因',
    audit_time      datetime                                 null comment '审核时间',
    article_count   int            default 0                 not null comment '文章数',
    subscribe_count int            default 0                 not null comment '订阅数',
    view_count      int            default 0                 not null comment '浏览数',
    is_finished     tinyint        default 0                 not null comment '是否完结',
    price           decimal(10, 2) default 0.00              not null comment '专栏会员价，0=免费',
    created_time    datetime       default CURRENT_TIMESTAMP null comment '创建时间',
    updated_time    datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    del_flag        char           default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '专栏' charset = utf8mb4;

create index idx_auditor_id
    on `moyun-db`.portal_column (auditor_id);

create index idx_category_id
    on `moyun-db`.portal_column (category_id);

create index idx_del_flag
    on `moyun-db`.portal_column (del_flag);

create index idx_status
    on `moyun-db`.portal_column (status);

create index idx_status_created_time
    on `moyun-db`.portal_column (status, created_time);

create index idx_user
    on `moyun-db`.portal_column (user_id);

create table if not exists `moyun-db`.portal_column_article
(
    id           bigint auto_increment comment '主键'
        primary key,
    column_id    bigint                             not null comment '专栏ID',
    article_id   bigint                             not null comment '文章ID',
    sort_order   int      default 0                 not null comment '专栏内顺序',
    created_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_column_article
        unique (column_id, article_id)
)
    comment '专栏-文章关联' charset = utf8mb4;

create index idx_column_sort
    on `moyun-db`.portal_column_article (column_id, sort_order);

create table if not exists `moyun-db`.portal_column_subscribe
(
    id           bigint auto_increment comment '主键'
        primary key,
    column_id    bigint                             not null comment '专栏ID',
    user_id      bigint                             not null comment '订阅用户ID',
    created_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_column_user
        unique (column_id, user_id)
)
    comment '专栏订阅' charset = utf8mb4;

create table if not exists `moyun-db`.portal_comment
(
    id               bigint auto_increment comment '评论ID'
        primary key,
    article_id       bigint                                 not null comment '文章ID',
    author_id        bigint                                 not null comment '评论者ID（门户用户ID）',
    content          text                                   not null comment '评论内容',
    parent_id        bigint       default 0                 null comment '父评论ID',
    root_id          bigint       default 0                 null comment '根评论ID（一级评论ID）',
    reply_to         bigint                                 null comment '回复的用户ID',
    reply_to_content varchar(200) default ''                null comment '被回复的内容摘要',
    like_count       bigint       default 0                 null comment '点赞数',
    status           char         default '1'               null comment '状态：0=待审核 1=已发布 2=审核驳回',
    auditor_id       bigint                                 null comment '审核人ID（系统用户ID，CMS审核时写入）',
    audit_remark     varchar(500)                           null comment '审核意见/驳回原因（独立字段）',
    audit_time       datetime                               null comment '审核时间',
    create_by        varchar(64)  default ''                null comment '创建者',
    create_time      datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    update_by        varchar(64)  default ''                null comment '更新者',
    update_time      datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark           varchar(500)                           null comment '备注',
    del_flag         char         default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '门户评论表' charset = utf8mb4;

create index idx_article_id
    on `moyun-db`.portal_comment (article_id);

create index idx_article_root
    on `moyun-db`.portal_comment (article_id, root_id);

create index idx_auditor_id
    on `moyun-db`.portal_comment (auditor_id);

create index idx_author_id
    on `moyun-db`.portal_comment (author_id);

create index idx_del_flag
    on `moyun-db`.portal_comment (del_flag);

create index idx_parent_id
    on `moyun-db`.portal_comment (parent_id);

create table if not exists `moyun-db`.portal_comment_like
(
    id          bigint auto_increment comment 'ID'
        primary key,
    user_id     bigint                                not null comment '用户ID（门户用户ID）',
    comment_id  bigint                                not null comment '评论ID',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    constraint uk_user_comment
        unique (user_id, comment_id)
)
    comment '门户评论点赞表' charset = utf8mb4;

create index idx_comment_id
    on `moyun-db`.portal_comment_like (comment_id);

create index idx_user_id
    on `moyun-db`.portal_comment_like (user_id);

create table if not exists `moyun-db`.portal_contest_submission
(
    id           bigint auto_increment comment '主键'
        primary key,
    contest_id   bigint                                not null comment '活动ID',
    user_id      bigint                                not null comment '投稿用户ID',
    article_id   bigint                                not null comment '投稿文章ID',
    status       varchar(16) default 'pending'         not null comment 'pending/shortlisted/eliminated/winner',
    vote_count   int         default 0                 not null comment '投票数',
    `rank`       int                                   null comment '排名',
    remark       varchar(500)                          null comment '备注（评审意见等）',
    created_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    del_flag     char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_contest_article
        unique (contest_id, article_id),
    constraint uk_contest_user
        unique (contest_id, user_id)
)
    comment '活动投稿' charset = utf8mb4;

create index idx_contest
    on `moyun-db`.portal_contest_submission (contest_id);

create index idx_del_flag
    on `moyun-db`.portal_contest_submission (del_flag);

create index idx_user
    on `moyun-db`.portal_contest_submission (user_id);

create table if not exists `moyun-db`.portal_contest_vote
(
    id            bigint auto_increment comment '主键'
        primary key,
    submission_id bigint                             not null comment '投稿ID',
    user_id       bigint                             not null comment '投票用户ID',
    contest_id    bigint                             not null comment '活动ID（冗余便于按活动统计）',
    created_time  datetime default CURRENT_TIMESTAMP null comment '投票时间',
    constraint uk_submission_user
        unique (submission_id, user_id)
)
    comment '活动投稿投票记录' charset = utf8mb4;

create index idx_contest
    on `moyun-db`.portal_contest_vote (contest_id);

create index idx_submission
    on `moyun-db`.portal_contest_vote (submission_id);

create index idx_user
    on `moyun-db`.portal_contest_vote (user_id);

create table if not exists `moyun-db`.portal_creator_certification
(
    id               bigint auto_increment comment '主键'
        primary key,
    user_id          bigint                                not null comment '申请用户ID',
    real_name        varchar(64)                           not null comment '真实姓名',
    cert_type        varchar(32)                           not null comment '认证类型 identity/creator/expert',
    cert_no          varchar(64)                           null comment '证件号',
    cert_no_enc      varchar(512)                          null comment '证件号密文（AES-GCM，格式 enc:v1:iv:cipher，base64）',
    cert_no_mask     varchar(32)                           null comment '证件号脱敏展示值（如 110***********1234）',
    derived_gender   varchar(8)                            null comment '由证件号推导的性别（男/女），仅身份认证类型',
    derived_birth    date                                  null comment '由证件号推导的出生日期',
    verify_channel   varchar(32) default 'manual'          null comment '实名核验渠道：manual=人工审核（默认），后期可扩展 aliyun/tencent 等',
    verify_serial    varchar(64)                           null comment '第三方实名核验流水号（预留，接入核验API后填充）',
    cert_image       varchar(500)                          null comment '证件照URL（兼容字段：旧单图或新流程中的「人像面」URL 别名）',
    cert_image_front varchar(500)                          null comment '身份证正面（人像面）URL',
    cert_image_back  varchar(500)                          null comment '身份证背面（国徽面）URL',
    intro            text                                  null comment '自我介绍',
    works            varchar(500)                          null comment '代表作链接',
    status           varchar(16) default 'pending'         not null comment '审核状态 pending/approved/rejected',
    auditor_id       bigint                                null comment '审核人ID',
    audit_remark     varchar(500)                          null comment '审核备注',
    created_time     datetime    default CURRENT_TIMESTAMP null comment '申请时间',
    audited_time     datetime                              null comment '审核时间',
    del_flag         char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '创作者认证' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_creator_certification (del_flag);

create index idx_status
    on `moyun-db`.portal_creator_certification (status);

create index idx_user
    on `moyun-db`.portal_creator_certification (user_id);

create table if not exists `moyun-db`.portal_creator_settlement
(
    id               bigint auto_increment comment '主键'
        primary key,
    creator_id       bigint                                   not null comment '创作者用户ID',
    period varchar (16) not null comment '结算周期，格式 yyyy-MM，如 2026-07',
    tip_income       decimal(10, 2) default 0.00              not null comment '打赏收入（当月已支付打赏总额）',
    paid_read_income decimal(10, 2) default 0.00              not null comment '付费阅读收入（当月已支付购买总额）',
    column_income    decimal(10, 2) default 0.00              not null comment '专栏订阅收入（当月已支付订阅总额）',
    total_income     decimal(10, 2) default 0.00              not null comment '总收入（三项之和）',
    platform_fee     decimal(10, 2) default 0.00              not null comment '平台抽成（total_income * platform_fee_rate）',
    creator_income   decimal(10, 2) default 0.00              not null comment '创作者实得（total_income - platform_fee）',
    status           varchar(16)    default 'pending'         not null comment '状态 pending/confirmed/paid',
    paid_time        datetime                                 null comment '打款时间',
    create_time      datetime       default CURRENT_TIMESTAMP null comment '创建时间',
    update_time      datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    del_flag         char           default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_creator_period
        unique (creator_id, period)
)
    comment '创作者分成结算' charset = utf8mb4;

create index idx_creator
    on `moyun-db`.portal_creator_settlement (creator_id);

create index idx_del_flag
    on `moyun-db`.portal_creator_settlement (del_flag);

create index idx_period
    on `moyun-db`.portal_creator_settlement (period);

create index idx_status
    on `moyun-db`.portal_creator_settlement (status);

create table if not exists `moyun-db`.portal_entity_tag
(
    id          bigint unsigned auto_increment comment '主键'
        primary key,
    tag_id      bigint unsigned                       not null comment '标签ID（引用 portal_tag.id）',
    entity_type varchar(32)                           not null comment '实体类型（article/interview_question/interview_experience/interview_resume_template/book 等）',
    entity_id   bigint unsigned                       not null comment '实体ID',
    sort        int         default 0                 null comment '排序',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    constraint uk_tag_entity
        unique (tag_id, entity_type, entity_id)
)
    comment '通用实体标签关联表' charset = utf8mb4;

create index idx_entity
    on `moyun-db`.portal_entity_tag (entity_type, entity_id);

create index idx_entity_create
    on `moyun-db`.portal_entity_tag (entity_type, create_time);

create index idx_tag_id
    on `moyun-db`.portal_entity_tag (tag_id);

create table if not exists `moyun-db`.portal_feed_event
(
    id           bigint auto_increment
        primary key,
    user_id      bigint       not null comment '事件发布者',
    event_type   varchar(32)  not null comment 'publish_article/publish_experience/new_column/checkin等',
    target_type  varchar(32)  not null comment 'article/experience/column/book等',
    target_id    bigint       not null comment '目标对象ID',
    title        varchar(256) null comment '目标标题',
    summary      varchar(500) null comment '动态摘要',
    cover        varchar(500) null comment '封面图',
    created_time datetime     not null
)
    comment '动态事件流' charset = utf8mb4;

create index idx_type_time
    on `moyun-db`.portal_feed_event (event_type, created_time);

create index idx_user_time
    on `moyun-db`.portal_feed_event (user_id, created_time);

create table if not exists `moyun-db`.portal_feed_inbox
(
    id           bigint auto_increment
        primary key,
    user_id      bigint   not null comment '接收者',
    event_id     bigint   not null comment '动态事件ID',
    created_time datetime not null
)
    comment '动态收件箱' charset = utf8mb4;

create index idx_user_time
    on `moyun-db`.portal_feed_inbox (user_id, created_time);

create table if not exists `moyun-db`.portal_feedback
(
    id            bigint auto_increment comment '反馈ID'
        primary key,
    feedback_type varchar(32)                           not null comment '反馈类型：suggestion/bug/experience/other',
    subject       varchar(200)                          null comment '反馈主题',
    description   varchar(2000)                         not null comment '反馈详细描述',
    contact       varchar(100)                          null comment '联系方式（可选）',
    user_id       bigint                                null comment '反馈人用户ID',
    username      varchar(64)                           null comment '反馈人用户名（冗余）',
    ip            varchar(128)                          null comment '反馈人IP',
    status        varchar(20) default 'pending'         null comment '处理状态：pending/processing/resolved/rejected',
    handler       varchar(64)                           null comment '处理人',
    handle_result varchar(1000)                         null comment '处理结果说明',
    handle_time   datetime                              null comment '处理时间',
    create_time   datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark        varchar(500)                          null comment '备注',
    del_flag      char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '用户意见反馈表' charset = utf8mb4;

create index idx_create_time
    on `moyun-db`.portal_feedback (create_time);

create index idx_del_flag
    on `moyun-db`.portal_feedback (del_flag);

create index idx_feedback_type
    on `moyun-db`.portal_feedback (feedback_type);

create index idx_status
    on `moyun-db`.portal_feedback (status);

create index idx_user_id
    on `moyun-db`.portal_feedback (user_id);

create table if not exists `moyun-db`.portal_follow
(
    id           bigint auto_increment comment '关注ID'
        primary key,
    follower_id  bigint                                not null comment '关注者ID（门户用户ID）',
    following_id bigint                                not null comment '被关注者ID（门户用户ID）',
    create_by    varchar(64) default ''                null comment '创建者',
    create_time  datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by    varchar(64) default ''                null comment '更新者',
    update_time  datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark       varchar(500)                          null comment '备注',
    constraint uk_follower_following
        unique (follower_id, following_id)
)
    comment '门户关注表' charset = utf8mb4;

create index idx_follower_id
    on `moyun-db`.portal_follow (follower_id);

create index idx_following_id
    on `moyun-db`.portal_follow (following_id);

create table if not exists `moyun-db`.portal_friend_link
(
    id          bigint auto_increment comment '链接ID'
        primary key,
    name        varchar(100)                          not null comment '链接名称',
    url         varchar(500)                          not null comment '链接地址',
    description varchar(500)                          null comment '链接描述',
    logo        varchar(500)                          null comment 'Logo URL',
    sort        int         default 0                 null comment '排序',
    status      varchar(20) default '0'               null comment '状态：0正常 1停用',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    del_flag    char        default '0'               null comment '删除标记（0=存在 2=删除，与全局逻辑删除配置一致）'
)
    comment '门户友情链接表' charset = utf8mb4;

create index idx_status
    on `moyun-db`.portal_friend_link (status);

create table if not exists `moyun-db`.portal_growth_log
(
    id             bigint unsigned auto_increment comment '主键'
        primary key,
    user_id        bigint unsigned                       not null comment '获得成长值的用户ID',
    target_user_id bigint unsigned                       null comment '目标用户ID（如被点赞的内容作者）',
    module         varchar(32)                           not null comment '来源模块: article/reading/interview/all',
    action         varchar(64)                           not null comment '行为: publish_article/solve_question/finish_book/...',
    entity_type    varchar(32)                           null comment '实体类型: article/book/question/note/experience',
    entity_id      bigint                                null comment '实体ID',
    growth_delta   int                                   not null comment '成长值变化（正数增加，负数减少）',
    description    varchar(255)                          null comment '描述',
    create_by      varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by      varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                          null comment '备注'
)
    comment '成长事件流水表' charset = utf8mb4;

create index idx_entity
    on `moyun-db`.portal_growth_log (entity_type, entity_id);

create index idx_module_action
    on `moyun-db`.portal_growth_log (module, action);

create index idx_target_user
    on `moyun-db`.portal_growth_log (target_user_id);

create index idx_user_time
    on `moyun-db`.portal_growth_log (user_id, create_time);

create table if not exists `moyun-db`.portal_growth_rule
(
    id           bigint unsigned auto_increment comment '主键'
        primary key,
    module       varchar(32)                           not null comment '模块: article/reading/interview/all',
    action       varchar(64)                           not null comment '行为编码',
    growth_delta int                                   not null comment '成长值',
    daily_limit  int         default 0                 null comment '每日上限（0=不限）',
    description  varchar(255)                          null comment '描述',
    status       char        default '0'               null comment '状态（0启用 1停用）',
    sort         int         default 0                 null comment '排序',
    create_by    varchar(64) default ''                null comment '创建者',
    create_time  datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by    varchar(64) default ''                null comment '更新者',
    update_time  datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark       varchar(500)                          null comment '备注',
    constraint uk_module_action
        unique (module, action)
)
    comment '成长规则配置表' charset = utf8mb4;

create table if not exists `moyun-db`.portal_help_article
(
    id          bigint auto_increment comment '文章ID'
        primary key,
    category_id bigint                                not null comment '分类ID',
    title       varchar(200)                          not null comment '问题标题',
    content     text                                  not null comment '答案内容（支持纯文本）',
    view_count  int         default 0                 null comment '查看次数',
    like_count  int         default 0                 null comment '点赞次数',
    sort        int         default 0                 null comment '排序（升序）',
    is_featured tinyint     default 0                 null comment '是否精选：0=否 1=是',
    status      varchar(20) default 'published'       null comment '状态：published/draft',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    del_flag    char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '帮助中心文章表' charset = utf8mb4;

create index idx_category_id
    on `moyun-db`.portal_help_article (category_id);

create index idx_del_flag
    on `moyun-db`.portal_help_article (del_flag);

create index idx_is_featured
    on `moyun-db`.portal_help_article (is_featured);

create index idx_sort
    on `moyun-db`.portal_help_article (sort);

create index idx_status
    on `moyun-db`.portal_help_article (status);

create table if not exists `moyun-db`.portal_help_category
(
    id          bigint auto_increment comment '分类ID'
        primary key,
    name        varchar(100)                          not null comment '分类名称',
    icon        varchar(100)                          null comment '图标（lucide 图标名）',
    description varchar(500)                          null comment '分类描述',
    sort        int         default 0                 null comment '排序（升序）',
    status      varchar(20) default 'active'          null comment '状态：active/inactive',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    del_flag    char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '帮助中心分类表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_help_category (del_flag);

create index idx_sort
    on `moyun-db`.portal_help_category (sort);

create index idx_status
    on `moyun-db`.portal_help_category (status);

create table if not exists `moyun-db`.portal_import_template_config
(
    id            bigint auto_increment comment '主键'
        primary key,
    business_key  varchar(64)                  not null comment '业务标识（interview_question/interview_experience/article/tag/note）',
    field_name    varchar(64)                  not null comment '实体字段名（Java 属性名）',
    column_name   varchar(64)                  not null comment 'Excel 列名（中文表头）',
    description   varchar(255)                 null comment '字段说明',
    example_value varchar(255)                 null comment '示例值',
    required      tinyint     default 0        null comment '是否必填：1=必填 0=可选',
    field_type    varchar(20) default 'string' null comment '字段类型：string/number/date/dict',
    dict_type     varchar(64)                  null comment '字典 type（field_type=dict 时生效）',
    combo_values  varchar(500)                 null comment '下拉可选值（逗号分隔）',
    column_width  int         default 20       null comment 'Excel 列宽',
    sort          int         default 0        null comment '列排序号',
    status        char        default '0'      null comment '状态：0=启用 1=停用',
    create_by     varchar(64) default ''       null comment '创建者',
    create_time   datetime                     null comment '创建时间',
    update_by     varchar(64) default ''       null comment '更新者',
    update_time   datetime                     null comment '更新时间',
    remark        varchar(500)                 null comment '备注'
)
    comment '导入模板字段配置（动态模板）' charset = utf8mb4;

create index idx_business_key_field
    on `moyun-db`.portal_import_template_config (business_key, field_name);

create index idx_business_key_status_sort
    on `moyun-db`.portal_import_template_config (business_key, status, sort);

create table if not exists `moyun-db`.portal_interview_attempt
(
    id              bigint auto_increment comment '主键'
        primary key,
    question_id     bigint                                not null comment '题目ID',
    user_id         bigint                                not null comment '用户ID',
    attempt_count   int         default 1                 null comment '尝试次数',
    last_attempt_at datetime    default CURRENT_TIMESTAMP null comment '最后尝试时间',
    status          varchar(30) default 'attempted'       null comment '状态:not_attempted,attempted,solved',
    first_solved_at datetime                              null comment '首次解决时间',
    last_solved_at  datetime                              null comment '最后解决时间',
    create_by       varchar(64) default ''                null comment '创建者',
    create_time     datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by       varchar(64) default ''                null comment '更新者',
    update_time     datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark          varchar(500)                          null comment '备注',
    del_flag        char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_question_user
        unique (question_id, user_id)
)
    comment '做题记录表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_interview_attempt (del_flag);

create index idx_user_id
    on `moyun-db`.portal_interview_attempt (user_id);

create table if not exists `moyun-db`.portal_interview_bookmark
(
    id          bigint auto_increment comment '主键'
        primary key,
    question_id bigint                                not null comment '题目ID',
    user_id     bigint                                not null comment '用户ID',
    note        text                                  null comment '笔记',
    create_time datetime    default CURRENT_TIMESTAMP null comment '收藏时间',
    create_by   varchar(64) default ''                null comment '创建者',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    del_flag    char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_question_user
        unique (question_id, user_id)
)
    comment '题目收藏表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_interview_bookmark (del_flag);

create table if not exists `moyun-db`.portal_interview_category
(
    id             bigint auto_increment comment '主键'
        primary key,
    name           varchar(200)                          not null comment '分类名称',
    slug           varchar(200)                          null comment '分类标识',
    description    text                                  null comment '分类描述',
    icon           varchar(500)                          null comment '图标URL',
    sort           int         default 0                 null comment '排序',
    question_count int         default 0                 null comment '题目数量',
    status         varchar(20) default 'active'          null comment '状态:active,inactive',
    create_by      varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by      varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                          null comment '备注',
    del_flag       char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '面试题目分类表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_interview_category (del_flag);

create index idx_status
    on `moyun-db`.portal_interview_category (status);

create table if not exists `moyun-db`.portal_interview_comment
(
    id               bigint auto_increment comment '主键'
        primary key,
    experience_id    bigint                                not null comment '面经ID',
    user_id          bigint                                not null comment '评论用户ID',
    parent_id        bigint                                null comment '父评论ID（支持两级回复）',
    reply_to_user_id bigint                                null comment '回复目标用户ID',
    content          text                                  not null comment '评论内容',
    like_count       bigint      default 0                 null comment '点赞数',
    status           varchar(20) default 'published'       null comment '状态:pending,published,rejected',
    auditor_id       bigint                                null comment '审核人ID（系统用户ID，CMS审核或定时扫描命中时写入）',
    audit_remark     varchar(500)                          null comment '审核意见/驳回原因',
    audit_time       datetime                              null comment '审核时间',
    create_by        varchar(64) default ''                null comment '创建者',
    create_time      datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by        varchar(64) default ''                null comment '更新者',
    update_time      datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark           varchar(500)                          null comment '备注',
    del_flag         char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '面经评论表' charset = utf8mb4;

create index idx_auditor_id
    on `moyun-db`.portal_interview_comment (auditor_id);

create index idx_del_flag
    on `moyun-db`.portal_interview_comment (del_flag);

create index idx_experience_id
    on `moyun-db`.portal_interview_comment (experience_id);

create index idx_parent_id
    on `moyun-db`.portal_interview_comment (parent_id);

create index idx_status
    on `moyun-db`.portal_interview_comment (status);

create index idx_user_id
    on `moyun-db`.portal_interview_comment (user_id);

create table if not exists `moyun-db`.portal_interview_comment_like
(
    id          bigint auto_increment comment '主键'
        primary key,
    comment_id  bigint                             not null comment '评论ID',
    user_id     bigint                             not null comment '用户ID',
    create_time datetime default CURRENT_TIMESTAMP null comment '点赞时间',
    constraint uk_comment_user
        unique (comment_id, user_id)
)
    comment '面经评论点赞表' charset = utf8mb4;

create table if not exists `moyun-db`.portal_interview_company
(
    id             bigint auto_increment comment '主键'
        primary key,
    name           varchar(200)                          not null comment '公司名称',
    slug           varchar(200)                          null comment '公司标识',
    logo           varchar(500)                          null comment '公司Logo URL',
    description    text                                  null comment '公司描述',
    industry       varchar(100)                          null comment '所属行业',
    question_count int         default 0                 null comment '相关题目数',
    sort           int         default 0                 null comment '排序',
    status         varchar(20) default 'active'          null comment '状态:active,inactive',
    create_by      varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by      varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                          null comment '备注',
    del_flag       char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_slug
        unique (slug)
)
    comment '面试公司标签表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_interview_company (del_flag);

create index idx_status
    on `moyun-db`.portal_interview_company (status);

create table if not exists `moyun-db`.portal_interview_config
(
    id                  bigint auto_increment comment '主键'
        primary key,
    config_name         varchar(100)                                                                                                                                                                       not null comment '配置名称（如：标准技术面）',
    persona_type        varchar(20)  default 'professional'                                                                                                                                                not null comment '面试官人设:professional/friendly/strict（对齐 voice_interview_style）',
    prompt_template     text                                                                                                                                                                               null comment '面试官提示词模板（支持 {{position}}/{{resumeDigest}} 等占位符，空则用 Agent 人设）',
    scoring_weights     varchar(500) default '{"relevance":20,"depth":25,"fluency":20,"logic":20,"confidence":15,"fusion":0.7,"selfIntro":{"structure":30,"selfAwareness":25,"jobMatch":25,"fluency":20}}' null comment '评分权重 JSON（5技术维度+LLM融合比例+自我介绍4维度）',
    question_weights    varchar(100) default '{"job":40,"resume":30,"weak":20,"random":10}'                                                                                                                null comment '出题权重 JSON',
    max_followups       int          default 2                                                                                                                                                             not null comment '每题最大追问次数',
    followup_triggers   varchar(200) default '["vague_answer","contradiction","depth_needed"]'                                                                                                             null comment '追问触发条件 JSON（模糊回答/前后矛盾/需深挖）',
    enable_self_intro   tinyint(1)   default 0                                                                                                                                                             not null comment '是否启用自我介绍环节（0=旧流程兼容默认）',
    self_intro_duration int          default 180                                                                                                                                                           not null comment '自我介绍建议时长（秒）',
    is_default          tinyint(1)   default 0                                                                                                                                                             not null comment '是否默认配置（互斥，全局唯一）',
    status              varchar(20)  default 'active'                                                                                                                                                      not null comment '状态:active 启用/inactive 停用',
    create_by           varchar(64)  default ''                                                                                                                                                            null comment '创建者',
    create_time         datetime     default CURRENT_TIMESTAMP                                                                                                                                             null comment '创建时间',
    update_by           varchar(64)  default ''                                                                                                                                                            null comment '更新者',
    update_time         datetime     default CURRENT_TIMESTAMP                                                                                                                                             null on update CURRENT_TIMESTAMP comment '更新时间',
    remark              varchar(500)                                                                                                                                                                       null comment '备注',
    del_flag            char         default '0'                                                                                                                                                           not null comment '删除标记（0=存在 2=删除）'
)
    comment '面试配置表（人设/提示词/评分权重/追问策略/自我介绍）' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_interview_config (del_flag);

create index idx_is_default
    on `moyun-db`.portal_interview_config (is_default);

create index idx_status
    on `moyun-db`.portal_interview_config (status);

create table if not exists `moyun-db`.portal_interview_experience
(
    id            bigint auto_increment comment '主键'
        primary key,
    user_id       bigint                                not null comment '用户ID',
    title         varchar(500)                          not null comment '面经标题',
    company       varchar(200)                          not null comment '公司',
    position      varchar(200)                          null comment '岗位',
    year          int                                   null comment '年份',
    month         int                                   null comment '月份',
    summary       varchar(500)                          null comment '内容摘要',
    content       text                                  not null comment '面经内容',
    cover_image   varchar(500)                          null comment '封面图URL',
    tags          varchar(500)                          null comment '标签',
    is_top        tinyint(1)  default 0                 null comment '是否置顶',
    view_count    bigint      default 0                 null comment '浏览数',
    like_count    bigint      default 0                 null comment '点赞数',
    comment_count bigint      default 0                 null comment '评论数',
    status        varchar(20) default 'published'       null comment '状态:draft,pending,published,rejected,archived',
    auditor_id    bigint                                null comment '审核人ID（系统用户ID，CMS审核时写入）',
    audit_remark  varchar(500)                          null comment '审核意见/驳回原因',
    audit_time    datetime                              null comment '审核时间',
    create_by     varchar(64) default ''                null comment '创建者',
    create_time   datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by     varchar(64) default ''                null comment '更新者',
    update_time   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark        varchar(500)                          null comment '备注',
    del_flag      char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '面经表' charset = utf8mb4;

create index idx_company
    on `moyun-db`.portal_interview_experience (company);

create index idx_del_flag
    on `moyun-db`.portal_interview_experience (del_flag);

create index idx_experience_auditor
    on `moyun-db`.portal_interview_experience (auditor_id);

create index idx_status
    on `moyun-db`.portal_interview_experience (status);

create index idx_user_id
    on `moyun-db`.portal_interview_experience (user_id);

create table if not exists `moyun-db`.portal_interview_experience_like
(
    id            bigint auto_increment comment '主键'
        primary key,
    experience_id bigint                                not null comment '面经ID',
    user_id       bigint                                not null comment '用户ID',
    create_time   datetime    default CURRENT_TIMESTAMP null comment '点赞时间',
    create_by     varchar(64) default ''                null comment '创建者',
    update_by     varchar(64) default ''                null comment '更新者',
    update_time   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark        varchar(500)                          null comment '备注',
    constraint uk_experience_user
        unique (experience_id, user_id)
)
    comment '面经点赞表' charset = utf8mb4;

create table if not exists `moyun-db`.portal_interview_position
(
    id              bigint auto_increment comment '主键'
        primary key,
    code            varchar(64)                           not null comment '岗位编码（如 java_backend）',
    name            varchar(100)                          not null comment '岗位名称（如 Java后端工程师）',
    industry        varchar(50)                           null comment '所属行业（如 互联网/金融/制造）',
    level           varchar(32)                           null comment '岗位级别（junior/mid/senior）',
    required_skills text                                  null comment '必备技能 JSON 数组（如 ["Spring","MySQL","Redis"]，与 portal_tag.name 对齐）',
    hot_companies   text                                  null comment '热门公司 JSON 数组（如 ["阿里","腾讯","字节"]）',
    description     varchar(500)                          null comment '岗位描述',
    sort            int         default 0                 null comment '排序',
    status          varchar(16) default 'active'          null comment '状态 active/inactive',
    create_by       varchar(64) default ''                null comment '创建者',
    create_time     datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by       varchar(64) default ''                null comment '更新者',
    update_time     datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark          varchar(500)                          null comment '备注',
    del_flag        char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_code
        unique (code)
)
    comment '面试岗位字典表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_interview_position (del_flag);

create index idx_status_sort
    on `moyun-db`.portal_interview_position (status, sort);

create table if not exists `moyun-db`.portal_interview_question
(
    id               bigint auto_increment comment '主键'
        primary key,
    title            varchar(500)                            not null comment '题目标题',
    description      text                                    null comment '题目描述',
    difficulty       varchar(20)   default 'medium'          null comment '难度:easy,medium,hard',
    category_id      bigint                                  null comment '分类ID',
    job_template_id  bigint                                  null comment '所属岗位模板ID（portal_job_template.id）',
    tags             varchar(500)                            null comment '标签，逗号分隔',
    companies        varchar(500)                            null comment '公司，逗号分隔',
    acceptance_rate  decimal(5, 2) default 0.00              null comment '通过率',
    submission_count bigint        default 0                 null comment '提交次数',
    like_count       bigint        default 0                 null comment '点赞数',
    hint             text                                    null comment '提示',
    solution         text                                    null comment '参考答案（代码题参考代码片段）',
    sort             int           default 0                 null comment '排序',
    status           varchar(20)   default 'active'          null comment '状态:active,inactive',
    question_type    varchar(50)                             null comment '题目类型:bagwen八股/algorithm算法/system_design系统设计/project项目/hr',
    examine_points   text                                    null comment '考察点列表JSON数组',
    answer_outline   text                                    null comment '答题大纲Markdown',
    scoring_criteria text                                    null comment '评分标准JSON数组',
    reference_answer text                                    null comment '官方参考答案Markdown（八股/设计/项目/HR类完整答案）',
    prerequisite_ids varchar(500)                            null comment '前置题目ID，逗号分隔',
    practice_mode    varchar(20)   default 'reading'         null comment '练习模式：reading=展示阅读/choice=选择题/coding=编程题',
    options          json                                    null comment '选择题选项JSON数组：[{label,text,is_correct}]',
    correct_answer   varchar(50)                             null comment '正确答案（选择题：选项label如B；编程题：null靠测试用例判定）',
    analysis         text                                    null comment '题目解析（做题后展示，区别于 reference_answer 参考答案）',
    knowledge_tags   varchar(500)                            null comment '知识点标签，逗号分隔（用于错题本/学习路径聚类）',
    create_by        varchar(64)   default ''                null comment '创建者',
    create_time      datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    update_by        varchar(64)   default ''                null comment '更新者',
    update_time      datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark           varchar(500)                            null comment '备注',
    del_flag         char          default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '面试题目表' charset = utf8mb4;

create index idx_category_id
    on `moyun-db`.portal_interview_question (category_id);

create index idx_del_flag
    on `moyun-db`.portal_interview_question (del_flag);

create index idx_difficulty
    on `moyun-db`.portal_interview_question (difficulty);

create index idx_job_template_id
    on `moyun-db`.portal_interview_question (job_template_id);

create index idx_practice_mode
    on `moyun-db`.portal_interview_question (practice_mode);

create index idx_question_type
    on `moyun-db`.portal_interview_question (question_type);

create index idx_status
    on `moyun-db`.portal_interview_question (status);

create table if not exists `moyun-db`.portal_interview_question_company
(
    id          bigint auto_increment comment '主键'
        primary key,
    question_id bigint                             not null comment '题目ID',
    company_id  bigint                             not null comment '公司ID',
    sort        int      default 0                 null comment '排序',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_question_company
        unique (question_id, company_id)
)
    comment '题目-公司关联表' charset = utf8mb4;

create index idx_company_id
    on `moyun-db`.portal_interview_question_company (company_id);

create table if not exists `moyun-db`.portal_interview_question_like
(
    id          bigint auto_increment comment '主键'
        primary key,
    question_id bigint                                not null comment '题目ID',
    user_id     bigint                                not null comment '用户ID',
    create_time datetime    default CURRENT_TIMESTAMP null comment '点赞时间',
    create_by   varchar(64) default ''                null comment '创建者',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    constraint uk_question_user
        unique (question_id, user_id)
)
    comment '题目点赞表' charset = utf8mb4;

create table if not exists `moyun-db`.portal_interview_question_test_case
(
    id              bigint auto_increment comment '主键'
        primary key,
    question_id     bigint                               not null comment '题目ID',
    input           text                                 null comment '标准输入（运行时通过stdin传入）',
    expected_output text                                 not null comment '期望输出',
    is_sample       tinyint(1) default 0                 null comment '是否样例:1=样例（前端展示）/0=隐藏',
    order_num       int        default 0                 null comment '用例排序',
    explanation     varchar(1000)                        null comment '用例说明',
    create_time     datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '面试题目测试用例表（OJ判题）' charset = utf8mb4;

create index idx_question_id
    on `moyun-db`.portal_interview_question_test_case (question_id);

create table if not exists `moyun-db`.portal_interview_resume_template
(
    id             bigint auto_increment comment '主键'
        primary key,
    title          varchar(500)                          not null comment '模板标题',
    description    text                                  null comment '模板描述',
    cover          varchar(500)                          null comment '封面URL',
    download_url   varchar(500)                          null comment '下载地址',
    category       varchar(200)                          null comment '分类',
    file_type      varchar(20)                           null comment '文件类型：docx/pdf/psd',
    file_size      bigint                                null comment '文件大小（字节）',
    is_premium     tinyint(1)  default 0                 null comment '是否付费模板',
    usage_guide    text                                  null comment '使用指南',
    tags           varchar(500)                          null comment '标签，逗号分隔',
    sample_data    json                                  null comment '模板结构化示例数据（JSON：name/phone/email/jobIntention/educations/works/projects/skills/selfIntro，用于一键套用填充编辑页）',
    like_count     bigint      default 0                 null comment '点赞数',
    download_count bigint      default 0                 null comment '下载次数',
    sort           int         default 0                 null comment '排序',
    status         varchar(20) default 'active'          null comment '状态:active,inactive',
    create_by      varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by      varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    preview_images text                                  null comment '模板预览图 JSON 数组（多图，V10.2 新增）',
    remark         varchar(500)                          null comment '备注',
    del_flag       char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '简历模板表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_interview_resume_template (del_flag);

create index idx_status
    on `moyun-db`.portal_interview_resume_template (status);

create table if not exists `moyun-db`.portal_interview_resume_template_like
(
    id          bigint auto_increment comment '主键'
        primary key,
    template_id bigint                                not null comment '简历模板ID',
    user_id     bigint                                not null comment '用户ID',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '点赞时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    constraint uk_template_user
        unique (template_id, user_id)
)
    comment '简历模板点赞表' charset = utf8mb4;

create table if not exists `moyun-db`.portal_interview_submission
(
    id                   bigint auto_increment comment '主键'
        primary key,
    question_id          bigint                                not null comment '题目ID',
    user_id              bigint                                not null comment '用户ID',
    code                 text                                  null comment '提交的代码',
    content              text                                  null comment '提交的文字答案',
    language             varchar(50) default 'java'            null comment '编程语言',
    answer_type          varchar(20) default 'code'            null comment '答案类型：code/text/design',
    status               varchar(50) default 'pending'         null comment '状态:accepted,wrong_answer,time_limit,compile_error',
    is_success           tinyint(1)  default 0                 null comment '是否通过',
    runtime              int                                   null comment '运行时间（毫秒）',
    memory_usage         int                                   null comment '内存使用（KB）',
    note                 text                                  null comment '备注/笔记',
    is_featured          tinyint(1)  default 0                 null comment '是否精选（后台采纳为优质笔记）：0=否 1=是',
    featured_time        datetime                              null comment '精选时间',
    passed_case_count    int                                   null comment '通过用例数（v6.3 OJ判题）',
    total_case_count     int                                   null comment '总用例数（v6.3 OJ判题）',
    failed_case_id       bigint                                null comment '首个失败用例ID（v6.3 OJ判题）',
    failed_case_input    text                                  null comment '首个失败用例输入（v6.3 OJ判题，仅样例可见）',
    failed_case_expected text                                  null comment '首个失败用例期望输出（v6.3 OJ判题）',
    failed_case_actual   text                                  null comment '首个失败用例实际输出（v6.3 OJ判题）',
    error_message        text                                  null comment '编译/运行错误信息（v6.3 OJ判题）',
    create_time          datetime    default CURRENT_TIMESTAMP null comment '提交时间',
    create_by            varchar(64) default ''                null comment '创建者',
    update_by            varchar(64) default ''                null comment '更新者',
    update_time          datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark               varchar(500)                          null comment '备注',
    del_flag             char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '题目提交记录表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_interview_submission (del_flag);

create index idx_is_featured
    on `moyun-db`.portal_interview_submission (is_featured);

create index idx_question_id
    on `moyun-db`.portal_interview_submission (question_id);

create index idx_status
    on `moyun-db`.portal_interview_submission (status);

create index idx_user_id
    on `moyun-db`.portal_interview_submission (user_id);

create index idx_user_question
    on `moyun-db`.portal_interview_submission (user_id, question_id);

create table if not exists `moyun-db`.portal_job_template
(
    id             bigint auto_increment comment '主键'
        primary key,
    name           varchar(100)                                                        not null comment '模板名称（如：Java后端工程师）',
    category       varchar(50)                                                         null comment '岗位类别（技术/产品/运营/设计等）',
    position_code  varchar(50)                                                         null comment '岗位编码（对齐 portal_voice_interview.position）',
    description    varchar(500)                                                        null comment '模板描述',
    jd_text        text                                                                null comment '岗位 JD 原文（用于 LLM 关键词提取与出题上下文）',
    keywords       varchar(500)                                                        null comment '岗位关键词，逗号分隔（LLM 提取 + 人工维护）',
    difficulty     varchar(20)  default 'medium'                                       not null comment '难度:easy,medium,hard（对齐 portal_interview_question.difficulty）',
    question_count int          default 5                                              not null comment '默认出题数量',
    weights        varchar(100) default '{"job":40,"resume":30,"weak":20,"random":10}' null comment '出题权重 JSON（job岗位核心/resume简历深挖/weak薄弱点/random随机兜底）',
    status         varchar(20)  default 'active'                                       not null comment '状态:active 启用/inactive 停用',
    create_by      varchar(64)  default ''                                             null comment '创建者',
    create_time    datetime     default CURRENT_TIMESTAMP                              null comment '创建时间',
    update_by      varchar(64)  default ''                                             null comment '更新者',
    update_time    datetime     default CURRENT_TIMESTAMP                              null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                                                        null comment '备注',
    del_flag       char         default '0'                                            not null comment '删除标记（0=存在 2=删除）'
)
    comment '岗位模板表（JD/关键词/出题权重，支撑智能出题）' charset = utf8mb4;

create index idx_category
    on `moyun-db`.portal_job_template (category);

create index idx_del_flag
    on `moyun-db`.portal_job_template (del_flag);

create index idx_status
    on `moyun-db`.portal_job_template (status);

create table if not exists `moyun-db`.portal_like
(
    id          bigint auto_increment comment '点赞ID'
        primary key,
    user_id     bigint                                not null comment '用户ID（门户用户ID）',
    article_id  bigint                                not null comment '文章ID',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    constraint uk_user_article
        unique (user_id, article_id)
)
    comment '门户点赞表（文章）' charset = utf8mb4;

create index idx_article_id
    on `moyun-db`.portal_like (article_id);

create index idx_user_id
    on `moyun-db`.portal_like (user_id);

create table if not exists `moyun-db`.portal_message
(
    id            bigint auto_increment
        primary key,
    session_id    bigint                       not null comment '会话ID',
    sender_id     bigint                       not null comment '发送者',
    sender_type   varchar(16) default 'portal' not null comment '发送者类型 portal/sys',
    receiver_id   bigint                       not null comment '接收者',
    receiver_type varchar(16) default 'portal' not null comment '接收者类型 portal/sys',
    content       text                         not null comment '消息内容',
    msg_type      varchar(16) default 'text'   null comment 'text/image/file',
    is_read       tinyint     default 0        null comment '是否已读',
    create_time   datetime                     null comment '创建时间'
)
    comment '私信消息' charset = utf8mb4;

create index idx_receiver_type_read
    on `moyun-db`.portal_message (receiver_id, receiver_type, is_read);

create index idx_session_time
    on `moyun-db`.portal_message (session_id, create_time);

create table if not exists `moyun-db`.portal_message_session
(
    id                   bigint auto_increment
        primary key,
    user_a               bigint                       not null comment '用户A（较小ID）',
    user_a_type          varchar(16) default 'portal' not null comment 'A方用户类型 portal/sys',
    user_b               bigint                       not null comment '用户B（较大ID）',
    user_b_type          varchar(16) default 'portal' not null comment 'B方用户类型 portal/sys',
    last_message_id      bigint                       null comment '最后一条消息ID',
    last_message_content varchar(500)                 null comment '最后消息内容预览',
    last_message_time    datetime                     null comment '最后消息时间',
    unread_a             int         default 0        null comment 'A未读数',
    unread_b             int         default 0        null comment 'B未读数',
    create_time          datetime                     null comment '创建时间',
    update_time          datetime                     null comment '更新时间',
    constraint uk_users_type
        unique (user_a, user_b, user_a_type, user_b_type)
)
    comment '私信会话' charset = utf8mb4;

create index idx_last_time
    on `moyun-db`.portal_message_session (last_message_time);

create index idx_user_a
    on `moyun-db`.portal_message_session (user_a);

create index idx_user_b
    on `moyun-db`.portal_message_session (user_b);

create table if not exists `moyun-db`.portal_order
(
    id            bigint auto_increment comment '订单ID'
        primary key,
    order_no      varchar(64)                           not null comment '订单号',
    user_id       bigint                                not null comment '用户ID（门户用户ID）',
    type          varchar(50)                           not null comment '类型：vip/recharge/product',
    product_id    bigint                                null comment '商品ID',
    amount        decimal(10, 2)                        not null comment '金额',
    status        varchar(20) default 'pending'         null comment '状态：pending/paid/cancelled/refunded',
    pay_method    varchar(50)                           null comment '支付方式：wechat/alipay',
    trade_no      varchar(64)                           null comment '第三方交易号（支付宝/微信返回的交易号）',
    pay_channel   varchar(20) default 'points'          null comment '支付渠道：points-积分/alipay-支付宝/wechat-微信支付',
    notify_id     varchar(64)                           null comment '支付回调ID（用于回调验签与幂等去重）',
    notify_time   datetime                              null comment '支付回调时间',
    refund_no     varchar(64)                           null comment '退款单号',
    refund_amount decimal(10, 2)                        null comment '退款金额',
    refund_time   datetime                              null comment '退款时间',
    refund_reason varchar(255)                          null comment '退款原因',
    paid_at       datetime                              null comment '支付时间',
    create_by     varchar(64) default ''                null comment '创建者',
    create_time   datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by     varchar(64) default ''                null comment '更新者',
    update_time   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark        varchar(500)                          null comment '备注',
    del_flag      char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_order_no
        unique (order_no)
)
    comment '门户订单表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_order (del_flag);

create index idx_pay_channel_status
    on `moyun-db`.portal_order (pay_channel, status);

create index idx_status
    on `moyun-db`.portal_order (status);

create index idx_trade_no
    on `moyun-db`.portal_order (trade_no);

create index idx_type
    on `moyun-db`.portal_order (type);

create index idx_user_id
    on `moyun-db`.portal_order (user_id);

create table if not exists `moyun-db`.portal_reading_preference
(
    id                bigint auto_increment comment '主键'
        primary key,
    user_id           bigint                                  not null comment '用户ID',
    font_size         int           default 18                null comment '正文字号（px，12-32）',
    line_height       decimal(3, 1) default 1.8               null comment '行距（倍，1.2-3.0）',
    theme             varchar(20)   default 'default'         null comment '阅读主题：default=跟随 / light=亮色 / dark=暗色 / sepia=护眼黄',
    font_family       varchar(50)   default 'system'          null comment '字体：system=系统默认 / serif=衬线 / song=宋体 / hei=黑体',
    letter_spacing    decimal(3, 1) default 0.0               null comment '字间距（px，-1.0-5.0）',
    paragraph_spacing decimal(4, 1) default 1.2               null comment '段间距（em，0.5-5.0）',
    create_by         varchar(64)   default ''                null comment '创建者',
    create_time       datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    update_by         varchar(64)   default ''                null comment '更新者',
    update_time       datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark            varchar(500)                            null comment '备注',
    del_flag          char          default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_user_id
        unique (user_id)
)
    comment '用户阅读偏好表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_reading_preference (del_flag);

create table if not exists `moyun-db`.portal_reading_progress
(
    id                  bigint auto_increment comment '主键'
        primary key,
    user_id             bigint                                not null comment '用户ID',
    book_id             bigint                                not null comment '书籍ID',
    current_chapter_id  bigint                                null comment '当前阅读章节ID',
    current_chapter_no  int         default 0                 null comment '当前章节序号',
    chapter_offset      int         default 0                 null comment '章节内滚动偏移（像素）',
    last_read_time      datetime                              null comment '最后阅读时间',
    reading_duration_ms bigint      default 0                 null comment '累计阅读时长（毫秒）',
    status              varchar(30) default 'want_to_read'    null comment '状态:want_to_read,reading,finished',
    progress            int         default 0                 null comment '阅读进度百分比',
    pages_read          int         default 0                 null comment '已读页数',
    start_date          date                                  null comment '开始阅读日期',
    finish_date         date                                  null comment '完成日期',
    note                text                                  null comment '阅读笔记',
    create_by           varchar(64) default ''                null comment '创建者',
    create_time         datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by           varchar(64) default ''                null comment '更新者',
    update_time         datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark              varchar(500)                          null comment '备注',
    del_flag            char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_user_book
        unique (user_id, book_id)
)
    comment '阅读进度表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_reading_progress (del_flag);

create index idx_last_read_time
    on `moyun-db`.portal_reading_progress (last_read_time);

create index idx_status
    on `moyun-db`.portal_reading_progress (status);

create index idx_user_id
    on `moyun-db`.portal_reading_progress (user_id);

create table if not exists `moyun-db`.portal_report
(
    id            bigint auto_increment comment '举报ID'
        primary key,
    report_type   varchar(32)                           not null comment '举报类型：spam/inappropriate/infringement/fraud/other',
    target_url    varchar(500)                          null comment '举报目标URL',
    target_type   varchar(32)                           null comment '举报目标类型：article=文章/comment=评论/user=用户/topic=话题/topic_post=话题观点/topic_comment=话题评论/column=专栏（为空表示通用举报，仅 target_url）',
    target_id     bigint                                null comment '举报目标ID（评论/文章/用户ID，配合 target_type 使用）',
    description   varchar(2000)                         not null comment '问题描述',
    contact       varchar(100)                          null comment '联系方式（可选）',
    images        varchar(1000)                         null comment '图片证据（JSON数组，最多3张）',
    user_id       bigint                                null comment '举报人用户ID',
    username      varchar(64)                           null comment '举报人用户名（冗余）',
    ip            varchar(128)                          null comment '举报人IP',
    status        varchar(20) default 'pending'         null comment '处理状态：pending/processing/resolved/rejected',
    handler       varchar(64)                           null comment '处理人',
    handle_result varchar(1000)                         null comment '处理结果说明',
    handle_time   datetime                              null comment '处理时间',
    create_time   datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark        varchar(500)                          null comment '备注',
    del_flag      char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '用户举报记录表' charset = utf8mb4;

create index idx_create_time
    on `moyun-db`.portal_report (create_time);

create index idx_del_flag
    on `moyun-db`.portal_report (del_flag);

create index idx_report_type
    on `moyun-db`.portal_report (report_type);

create index idx_status
    on `moyun-db`.portal_report (status);

create index idx_target
    on `moyun-db`.portal_report (target_type, target_id);

create index idx_user_id
    on `moyun-db`.portal_report (user_id);

create table if not exists `moyun-db`.portal_resume_job_match
(
    id               bigint auto_increment comment '报告ID'
        primary key,
    user_id          bigint                               not null comment '用户ID',
    resume_id        bigint                               not null comment '简历ID（portal_user_resume.id）',
    job_target_id    bigint                               not null comment '岗位目标ID',
    match_score      int                                  not null comment '综合匹配度 0-100',
    grade            varchar(20)                          null comment '评级：excellent/good/medium/poor',
    matched_keywords varchar(1000)                        null comment '已匹配关键词（逗号分隔）',
    missing_keywords varchar(1000)                        null comment '缺失关键词（逗号分隔）',
    dimensions       json                                 null comment '各维度评分明细（关键词/经验/技能/结构匹配）',
    summary          text                                 null comment 'AI分析总结',
    ai_powered       tinyint(1) default 0                 null comment '是否LLM生成：0=规则 1=LLM',
    create_time      datetime   default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '简历优化-岗位匹配报告表' charset = utf8mb4;

create index idx_resume_id
    on `moyun-db`.portal_resume_job_match (resume_id);

create index idx_user_id
    on `moyun-db`.portal_resume_job_match (user_id);

create table if not exists `moyun-db`.portal_resume_job_target
(
    id          bigint auto_increment comment '岗位目标ID'
        primary key,
    user_id     bigint                               not null comment '用户ID（门户用户ID）',
    position    varchar(100)                         not null comment '目标岗位名称（如 Java开发工程师）',
    company     varchar(100)                         null comment '目标公司（选填）',
    city        varchar(50)                          null comment '期望城市（选填）',
    job_type    varchar(20)                          null comment '岗位类型（全职/兼职/实习）',
    jd_text     text                                 not null comment '岗位描述/JD原文（匹配分析核心输入）',
    jd_keywords varchar(1000)                        null comment 'AI提取的JD核心关键词（逗号分隔，冗余加速展示）',
    is_default  tinyint(1) default 0                 null comment '是否默认岗位：0=否 1=是',
    create_time datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '简历优化-岗位目标表' charset = utf8mb4;

create index idx_user_id
    on `moyun-db`.portal_resume_job_target (user_id);

create table if not exists `moyun-db`.portal_resume_optimize_history
(
    id                 bigint auto_increment comment '优化记录ID'
        primary key,
    user_id            bigint                             not null comment '用户ID',
    resume_id          bigint                             not null comment '被优化的简历ID',
    from_resume_id     bigint                             not null comment '优化前简历ID（同一简历冗余记录，便于追溯）',
    job_target_id      bigint                             null comment '关联岗位目标ID',
    score_before       int                                null comment '优化前评分',
    score_after        int                                null comment '优化后评分',
    match_score_before int                                null comment '优化前匹配度',
    match_score_after  int                                null comment '优化后匹配度',
    adopted_count      int      default 0                 null comment '采纳建议数',
    total_count        int      default 0                 null comment '生成建议总数',
    optimize_data      json                               null comment '优化明细快照（逐项 original/optimized/status）',
    create_time        datetime default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '简历优化-优化历史表' charset = utf8mb4;

create index idx_resume_id
    on `moyun-db`.portal_resume_optimize_history (resume_id);

create table if not exists `moyun-db`.portal_resume_optimize_task
(
    id            bigint auto_increment comment '任务ID'
        primary key,
    user_id       bigint                                not null comment '用户ID（门户用户ID）',
    resume_id     bigint                                not null comment '简历ID（portal_user_resume.id）',
    job_target_id bigint                                not null comment '岗位目标ID',
    status        varchar(20) default 'pending'         not null comment '任务状态：pending(已提交)/running(执行中)/success(成功)/failed(失败)',
    result_json   json                                  null comment '优化结果 JSON（status=success 时填充，对应 ResumeDeepOptimizeVO 序列化）',
    error_msg     varchar(1000)                         null comment '失败原因（status=failed 时填充）',
    ai_powered    tinyint(1)  default 1                 null comment '是否 LLM 生成：0=规则兜底 1=LLM',
    create_time   datetime    default CURRENT_TIMESTAMP null comment '任务提交时间',
    start_time    datetime                              null comment '任务开始执行时间（pending→running 时写入）',
    finish_time   datetime                              null comment '任务结束时间（成功或失败时写入）'
)
    comment '简历深度优化异步任务表（v10.19）' charset = utf8mb4;

create index idx_resume_id
    on `moyun-db`.portal_resume_optimize_task (resume_id);

create index idx_status
    on `moyun-db`.portal_resume_optimize_task (status);

create index idx_user_id
    on `moyun-db`.portal_resume_optimize_task (user_id);

create table if not exists `moyun-db`.portal_resume_score_report
(
    id                bigint auto_increment comment '报告ID'
        primary key,
    user_id           bigint                                not null comment '用户ID（门户用户ID）',
    resume_id         bigint                                not null comment '简历ID（portal_user_resume.id）',
    job_target_id     bigint                                null comment '关联岗位目标ID（可选，纯规则评分时为空）',
    position_snapshot varchar(100)                          null comment '评分时的目标岗位快照（便于报告独立解读）',
    score             int                                   not null comment '综合评分 0-100',
    score_detail      text                                  null comment '各维度评分明细 JSON（基本信息/求职意向/教育/工作/项目/技能/自我评价/岗位匹配度）',
    source            varchar(20) default 'manual'          null comment '评分来源：manual 单独评分 / optimize 优化后重新评分 / template 模板套用评分',
    create_time       datetime    default CURRENT_TIMESTAMP null comment '评分时间'
)
    comment '简历优化-评分报告存档表' charset = utf8mb4;

create index idx_resume_id
    on `moyun-db`.portal_resume_score_report (resume_id);

create index idx_user_id
    on `moyun-db`.portal_resume_score_report (user_id);

create table if not exists `moyun-db`.portal_shop_exchange
(
    id            bigint auto_increment comment '主键'
        primary key,
    user_id       bigint                                not null comment '兑换用户ID',
    item_id       bigint                                not null comment '商品ID',
    points_cost   int                                   not null comment '消耗积分（冗余，便于查询）',
    status        varchar(16) default 'pending'         not null comment '状态 pending/fulfilled/failed',
    address       varchar(500)                          null comment '收货地址（实物商品）',
    exchange_time datetime    default CURRENT_TIMESTAMP null comment '兑换时间',
    create_by     varchar(64) default ''                null comment '创建者',
    create_time   datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by     varchar(64) default ''                null comment '更新者',
    update_time   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark        varchar(500)                          null comment '备注',
    del_flag      char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '积分兑换记录表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_shop_exchange (del_flag);

create index idx_item
    on `moyun-db`.portal_shop_exchange (item_id);

create index idx_status
    on `moyun-db`.portal_shop_exchange (status);

create index idx_user
    on `moyun-db`.portal_shop_exchange (user_id);

create table if not exists `moyun-db`.portal_shop_item
(
    id          bigint auto_increment comment '主键'
        primary key,
    name        varchar(128)                          not null comment '商品名称',
    description varchar(500)                          null comment '商品描述',
    cover       varchar(500)                          null comment '商品封面URL',
    type        varchar(32) default 'virtual'         not null comment '商品类型 virtual/physical',
    points_cost int         default 0                 not null comment '兑换所需积分',
    stock       int         default 0                 not null comment '库存（-1表示不限）',
    status      varchar(16) default 'active'          not null comment '状态 active/inactive',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    del_flag    char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '积分商城商品表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_shop_item (del_flag);

create index idx_type_status
    on `moyun-db`.portal_shop_item (type, status);

create table if not exists `moyun-db`.portal_study_plan
(
    id              bigint auto_increment comment '主键'
        primary key,
    user_id         bigint                                not null comment '用户ID',
    title           varchar(128)                          not null comment '计划标题',
    plan_type       varchar(32)                           null comment '计划类型 daily_question/weekly_reading/custom',
    target_count    int                                   null comment '目标数量',
    target_category varchar(64)                           null comment '目标分类',
    start_date      date                                  null comment '开始日期',
    end_date        date                                  null comment '结束日期',
    status          varchar(16) default 'active'          not null comment '状态 active/completed/abandoned',
    created_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    del_flag        char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '学习计划' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_study_plan (del_flag);

create index idx_status
    on `moyun-db`.portal_study_plan (status);

create index idx_user
    on `moyun-db`.portal_study_plan (user_id);

create table if not exists `moyun-db`.portal_study_plan_log
(
    id           bigint auto_increment comment '主键'
        primary key,
    plan_id      bigint                             not null comment '计划ID',
    user_id      bigint                             not null comment '用户ID',
    log_date     date                               not null comment '日志日期',
    done_count   int      default 0                 not null comment '当日完成数量',
    created_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_plan_date
        unique (plan_id, log_date)
)
    comment '计划每日进度' charset = utf8mb4;

create index idx_user_date
    on `moyun-db`.portal_study_plan_log (user_id, log_date);

create table if not exists `moyun-db`.portal_tag
(
    id              bigint auto_increment comment '标签ID'
        primary key,
    name            varchar(100)                              not null comment '标签名称',
    slug            varchar(100)                              null comment '标签别名',
    sort            int             default 0                 null comment '排序',
    status          char            default '0'               null comment '状态（0正常 1停用）',
    module          varchar(50)                               null comment '所属模块（article/interview_question/interview_experience/interview_resume_template 等，null 表示通用）',
    reference_count bigint unsigned default '0'               null comment '被引用次数（冗余计数列，绑定/解绑时同步维护）',
    create_by       varchar(64)     default ''                null comment '创建者',
    create_time     datetime        default CURRENT_TIMESTAMP null comment '创建时间',
    update_by       varchar(64)     default ''                null comment '更新者',
    update_time     datetime        default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark          varchar(500)                              null comment '备注',
    del_flag        char            default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_name
        unique (name)
)
    comment '门户标签表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_tag (del_flag);

create index idx_module
    on `moyun-db`.portal_tag (module);

create index idx_reference_count
    on `moyun-db`.portal_tag (reference_count desc);

create index idx_slug
    on `moyun-db`.portal_tag (slug);

create table if not exists `moyun-db`.portal_task
(
    id            bigint auto_increment comment '主键'
        primary key,
    code          varchar(64)                           not null comment '任务编码（唯一，用于埋点触发，如 daily_checkin）',
    name          varchar(128)                          not null comment '任务名称',
    description   varchar(500)                          null comment '任务描述',
    task_type     varchar(32) default 'daily'           not null comment '任务类型 daily/once/achievement',
    reward_points int         default 0                 not null comment '完成奖励积分',
    target_count  int         default 1                 not null comment '目标完成次数',
    icon          varchar(500)                          null comment '任务图标URL',
    status        varchar(16) default 'active'          not null comment '状态 active/inactive',
    create_by     varchar(64) default ''                null comment '创建者',
    create_time   datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by     varchar(64) default ''                null comment '更新者',
    update_time   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark        varchar(500)                          null comment '备注',
    del_flag      char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_code
        unique (code)
)
    comment '任务定义表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_task (del_flag);

create index idx_type_status
    on `moyun-db`.portal_task (task_type, status);

create table if not exists `moyun-db`.portal_tip_order
(
    id            bigint auto_increment comment '主键'
        primary key,
    user_id       bigint                                not null comment '打赏者用户ID',
    author_id     bigint                                not null comment '被打赏者用户ID',
    target_type   varchar(32)                           not null comment '打赏对象类型 article/column/article_paid',
    target_id     bigint                                not null comment '打赏对象ID',
    amount        decimal(10, 2)                        not null comment '打赏金额',
    message       varchar(200)                          null comment '打赏留言',
    status        varchar(16) default 'pending'         not null comment '状态 pending/paid/refunded',
    pay_method    varchar(32)                           null comment '支付方式',
    trade_no      varchar(64)                           null comment '第三方交易号（支付宝/微信返回的交易号）',
    pay_channel   varchar(20) default 'points'          null comment '支付渠道：points-积分/alipay-支付宝/wechat-微信支付',
    notify_id     varchar(64)                           null comment '支付回调ID（用于回调验签与幂等去重）',
    notify_time   datetime                              null comment '支付回调时间',
    refund_no     varchar(64)                           null comment '退款单号',
    refund_amount decimal(10, 2)                        null comment '退款金额',
    refund_time   datetime                              null comment '退款时间',
    refund_reason varchar(255)                          null comment '退款原因',
    paid_time     datetime                              null comment '支付时间',
    created_time  datetime    default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '打赏订单（复用为付费阅读购买记录，target_type=article_paid）' charset = utf8mb4;

create index idx_author
    on `moyun-db`.portal_tip_order (author_id);

create index idx_pay_channel_status
    on `moyun-db`.portal_tip_order (pay_channel, status);

create index idx_target
    on `moyun-db`.portal_tip_order (target_type, target_id);

create index idx_trade_no
    on `moyun-db`.portal_tip_order (trade_no);

create index idx_user
    on `moyun-db`.portal_tip_order (user_id);

create table if not exists `moyun-db`.portal_topic
(
    id             bigint auto_increment comment '主键'
        primary key,
    title          varchar(128)                          not null comment '话题标题',
    description    varchar(500)                          null comment '话题描述/导语',
    cover          varchar(500)                          null comment '封面图 URL',
    creator_id     bigint                                not null comment '发起人 portal_user.id（必须是认证创作者）',
    status         varchar(20) default 'pending'         not null comment '状态：pending 待审核/active 活跃/archived 归档/deleted 删除/rejected 审核驳回',
    auditor_id     bigint                                null comment '审核人ID（系统用户ID）',
    audit_remark   varchar(500)                          null comment '审核意见/驳回原因',
    audit_time     datetime                              null comment '审核时间',
    pinned         tinyint     default 0                 not null comment '是否置顶：0 否/1 是',
    view_count     int         default 0                 not null comment '浏览数',
    post_count     int         default 0                 not null comment '观点数',
    like_count     int         default 0                 not null comment '话题被赞数',
    is_featured    tinyint     default 0                 not null comment '是否精选：0 否/1 是',
    comment_count  int         default 0                 not null comment '评论数（一级评论）',
    last_post_time datetime                              null comment '最后观点时间',
    last_poster_id bigint                                null comment '最后观点用户',
    created_time   datetime    default CURRENT_TIMESTAMP not null,
    updated_time   datetime                              null on update CURRENT_TIMESTAMP,
    del_flag       char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '话题主表' charset = utf8mb4;

create index idx_auditor_id
    on `moyun-db`.portal_topic (auditor_id);

create index idx_creator_time
    on `moyun-db`.portal_topic (creator_id, created_time);

create index idx_del_flag
    on `moyun-db`.portal_topic (del_flag);

create index idx_last_post
    on `moyun-db`.portal_topic (last_post_time);

create index idx_status_created_time
    on `moyun-db`.portal_topic (status, created_time);

create index idx_status_pinned_last
    on `moyun-db`.portal_topic (status, pinned, last_post_time);

create table if not exists `moyun-db`.portal_topic_comment
(
    id               bigint auto_increment
        primary key,
    target_type      varchar(20)                            not null comment '目标类型：topic 话题评论 / post 观点评论',
    target_id        bigint                                 not null comment '目标 ID',
    author_id        bigint                                 not null comment '评论者 portal_user.id',
    content          varchar(2000)                          not null comment '评论内容',
    parent_id        bigint       default 0                 not null comment '父评论 ID（0=一级评论）',
    root_id          bigint       default 0                 not null comment '根评论 ID（一级评论 root_id=0）',
    reply_to         bigint                                 null comment '被回复的用户 ID',
    reply_to_content varchar(200) default ''                null comment '被回复内容摘要',
    like_count       int          default 0                 not null,
    reply_count      int          default 0                 not null comment '回复数（仅一级评论维护）',
    is_deleted       tinyint      default 0                 not null comment '软删',
    created_time     datetime     default CURRENT_TIMESTAMP not null,
    updated_time     datetime                               null on update CURRENT_TIMESTAMP
)
    comment '话题评论（多态）' charset = utf8mb4;

create index idx_author_time
    on `moyun-db`.portal_topic_comment (author_id, created_time);

create index idx_root
    on `moyun-db`.portal_topic_comment (root_id, created_time);

create index idx_target_type_id_parent
    on `moyun-db`.portal_topic_comment (target_type, target_id, parent_id, created_time);

create table if not exists `moyun-db`.portal_topic_comment_like
(
    id           bigint auto_increment
        primary key,
    comment_id   bigint                             not null,
    user_id      bigint                             not null,
    created_time datetime default CURRENT_TIMESTAMP not null,
    constraint uk_comment_user
        unique (comment_id, user_id)
)
    comment '话题评论点赞' charset = utf8mb4;

create index idx_user_time
    on `moyun-db`.portal_topic_comment_like (user_id, created_time);

create table if not exists `moyun-db`.portal_topic_like
(
    id           bigint auto_increment
        primary key,
    topic_id     bigint                             not null,
    user_id      bigint                             not null,
    created_time datetime default CURRENT_TIMESTAMP not null,
    constraint uk_topic_user
        unique (topic_id, user_id)
)
    comment '话题点赞' charset = utf8mb4;

create index idx_user_time
    on `moyun-db`.portal_topic_like (user_id, created_time);

create table if not exists `moyun-db`.portal_topic_post
(
    id               bigint auto_increment
        primary key,
    topic_id         bigint                             not null comment '所属话题',
    user_id          bigint                             not null comment '发布者 portal_user.id',
    content          text                               not null comment '观点内容（Markdown）',
    images           json                               null comment '图片 URL 列表，最多 9 张',
    parent_post_id   bigint                             null comment '父观点 ID（楼中楼，NULL 为一级观点）',
    reply_to_user_id bigint                             null comment '回复的用户 ID',
    floor            int      default 0                 not null comment '楼层号',
    like_count       int      default 0                 not null,
    comment_count    int      default 0                 not null,
    is_deleted       tinyint  default 0                 not null comment '软删：0 否/1 是',
    created_time     datetime default CURRENT_TIMESTAMP not null,
    updated_time     datetime                           null on update CURRENT_TIMESTAMP,
    constraint uk_topic_floor
        unique (topic_id, floor)
)
    comment '话题观点（楼层）' charset = utf8mb4;

create index idx_parent
    on `moyun-db`.portal_topic_post (parent_post_id);

create index idx_topic_time
    on `moyun-db`.portal_topic_post (topic_id, created_time);

create index idx_user_time
    on `moyun-db`.portal_topic_post (user_id, created_time);

create table if not exists `moyun-db`.portal_topic_post_like
(
    id           bigint auto_increment
        primary key,
    post_id      bigint                             not null,
    user_id      bigint                             not null,
    created_time datetime default CURRENT_TIMESTAMP not null,
    constraint uk_post_user
        unique (post_id, user_id)
)
    comment '话题观点点赞' charset = utf8mb4;

create index idx_user_time
    on `moyun-db`.portal_topic_post_like (user_id, created_time);

create table if not exists `moyun-db`.portal_user
(
    id                   bigint auto_increment comment '用户ID'
        primary key,
    user_id              bigint                                 null comment '关联后台用户ID',
    username             varchar(50)                            not null comment '用户名',
    nickname             varchar(50)                            null comment '昵称',
    email                varchar(100)                           null comment '邮箱',
    phone                varchar(20)                            null comment '手机号',
    password             varchar(200)                           null comment '密码',
    avatar               varchar(500)                           null comment '头像URL',
    bio                  varchar(500)                           null comment '个人简介',
    position             varchar(100)                           null comment '职位',
    identity_tag         varchar(32)                            null comment '身份标签（字典 ledger_identity_tag，AI 财务分析画像维度）',
    wechat               varchar(100)                           null comment '微信号',
    gender               varchar(20)                            null comment '性别：male-男，female-女，other-其他',
    birthday             varchar(20)                            null comment '生日：YYYY-MM-DD格式',
    location             varchar(100)                           null comment '所在城市：如北京市',
    website              varchar(200)                           null comment '个人网站URL',
    github               varchar(100)                           null comment 'GitHub用户名或完整URL',
    company              varchar(200)                           null comment '公司名称',
    school               varchar(200)                           null comment '学校名称',
    language             varchar(20)                            null comment '语言偏好：zh-CN，en-US等',
    timezone             varchar(50)                            null comment '时区：如Asia/Shanghai',
    notify_like          tinyint(1)   default 1                 null comment '是否接收点赞通知',
    notify_comment       tinyint(1)   default 1                 null comment '是否接收评论通知',
    notify_follow        tinyint(1)   default 1                 null comment '是否接收关注通知',
    notify_system        tinyint(1)   default 1                 null comment '是否接收系统通知',
    privacy_follow       tinyint(1)   default 1                 null comment '是否允许被关注',
    privacy_bookmark     tinyint(1)   default 1                 null comment '是否公开收藏夹',
    privacy_email        tinyint(1)   default 0                 null comment '是否公开邮箱',
    privacy_phone        tinyint(1)   default 0                 null comment '是否公开手机号',
    privacy_profile      tinyint(1)   default 1                 null comment '是否公开主页（是否在名家录/作者列表展示）：1=公开，0=不公开',
    role                 varchar(20)  default 'user'            null comment '角色：user/admin',
    is_certified_creator tinyint      default 0                 not null comment '是否认证创作者：0 否/1 是',
    vip_expire_at        datetime                               null comment 'VIP过期时间',
    is_phone_verified    tinyint(1)   default 0                 null comment '是否已验证手机号',
    is_wechat_verified   tinyint(1)   default 0                 null comment '是否已验证微信',
    two_factor_enabled   tinyint(1)   default 0                 null comment '是否开启两步验证',
    status               char         default '0'               null comment '帐号状态（0正常 1停用）',
    del_flag             char         default '0'               null comment '删除标记（0=存在 2=删除）',
    login_ip             varchar(128) default ''                null comment '最后登录IP',
    login_date           datetime                               null comment '最后登录时间',
    create_by            varchar(64)  default ''                null comment '创建者',
    create_time          datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    update_by            varchar(64)  default ''                null comment '更新者',
    update_time          datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark               varchar(500)                           null comment '备注',
    constraint uk_username
        unique (username)
)
    comment '门户用户表' charset = utf8mb4;

create index idx_email
    on `moyun-db`.portal_user (email);

create index idx_phone
    on `moyun-db`.portal_user (phone);

create index idx_user_id
    on `moyun-db`.portal_user (user_id);

create table if not exists `moyun-db`.portal_user_badge
(
    id             bigint unsigned auto_increment comment '主键'
        primary key,
    user_id        bigint unsigned                       not null comment '用户ID',
    achievement_id bigint unsigned                       not null comment '成就ID',
    create_by      varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '获得时间',
    update_by      varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                          null comment '备注',
    constraint uk_user_achievement
        unique (user_id, achievement_id)
)
    comment '用户徽章记录表' charset = utf8mb4;

create table if not exists `moyun-db`.portal_user_growth
(
    id                    bigint unsigned auto_increment comment '主键'
        primary key,
    user_id               bigint unsigned                        not null comment '门户用户ID（portal_user.id）',
    growth_value          int unsigned default '0'               null comment '成长值（累计，只增不减）',
    level                 int          default 1                 null comment '当前等级',
    title                 varchar(50)  default '初出茅庐'        null comment '当前头衔',
    season_value          int unsigned default '0'               null comment '本季成长值（赛季排名用）',
    points                bigint       default 0                 not null comment '积分余额（可消耗，与成长值解耦）',
    supplement_card_count int          default 0                 not null comment '补签卡数量（每月赠送1张，补签消耗）',
    last_card_grant_month varchar(7)                             null comment '最后赠送补签卡月份（YYYY-MM，幂等控制）',
    create_by             varchar(64)  default ''                null comment '创建者',
    create_time           datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    update_by             varchar(64)  default ''                null comment '更新者',
    update_time           datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark                varchar(500)                           null comment '备注',
    constraint uk_user
        unique (user_id)
)
    comment '用户成长值总表' charset = utf8mb4;

create index idx_season
    on `moyun-db`.portal_user_growth (season_value desc);

create table if not exists `moyun-db`.portal_user_resume
(
    id               bigint auto_increment comment '主键'
        primary key,
    user_id          bigint                                 not null comment '用户ID',
    title            varchar(100) default '我的简历'        not null comment '简历名称',
    parent_id        bigint                                 null comment '父简历ID（版本历史关联，首次创建为 NULL）',
    version_no       int          default 1                 not null comment '版本号',
    name             varchar(50)                            null comment '姓名',
    gender           varchar(10)                            null comment '性别：男/女',
    birth_date       date                                   null comment '出生日期',
    phone            varchar(20)                            null comment '联系电话',
    email            varchar(100)                           null comment '邮箱',
    avatar           varchar(255)                           null comment '头像URL',
    job_intention    text                                   null comment '求职意向（JSON：期望职位/城市/薪资/类型）',
    educations       text                                   null comment '教育经历（JSON 数组：学校/专业/学历/时间/描述）',
    works            text                                   null comment '工作经历（JSON 数组：公司/职位/时间/描述）',
    projects         text                                   null comment '项目经历（JSON 数组：名称/角色/时间/描述/链接）',
    skills           text                                   null comment '技能列表（JSON 数组：名称/等级/分类）',
    self_intro       text                                   null comment '自我介绍',
    full_text        mediumtext                             null comment '简历全文纯文本（保存时自动拼接结构化字段，供 AI 分析使用）',
    score            int                                    null comment '评分（0-100）',
    score_detail     text                                   null comment '评分明细（JSON 数组）',
    parse_confidence tinyint                                null comment '解析置信度（0-100：LLM 结构化=85，规则兜底=60，NULL=未解析）',
    scored_time      datetime                               null comment '评分时间',
    file_url         varchar(255)                           null comment 'PDF 导出文件URL',
    export_time      datetime                               null comment '最后导出时间',
    status           varchar(20)  default 'draft'           not null comment '状态：draft/published/archived',
    source_type      varchar(20)  default 'online'          not null comment '来源类型：online（在线创建）/ attachment（附件解析）',
    source_file_url  varchar(500)                           null comment '附件源文件URL（source_type=attachment 时有值，支持下载）',
    source_file_name varchar(255)                           null comment '附件原始文件名（上传时的文件名，用于下载时还原文件名）',
    create_by        varchar(64)  default ''                null comment '创建者',
    create_time      datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    update_by        varchar(64)  default ''                null comment '更新者',
    update_time      datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark           varchar(500)                           null comment '备注',
    del_flag         char         default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_parent_version
        unique (parent_id, version_no)
)
    comment '用户简历' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_user_resume (del_flag);

create index idx_parent_id
    on `moyun-db`.portal_user_resume (parent_id);

create index idx_user_id
    on `moyun-db`.portal_user_resume (user_id);

create index idx_user_source
    on `moyun-db`.portal_user_resume (user_id, source_type);

create index idx_user_status
    on `moyun-db`.portal_user_resume (user_id, status);

create table if not exists `moyun-db`.portal_user_stats
(
    id                     bigint unsigned auto_increment comment '主键'
        primary key,
    user_id                bigint unsigned                       not null comment '门户用户ID',
    article_count          int         default 0                 null comment '发布文章数',
    article_view_sum       bigint      default 0                 null comment '文章总浏览量',
    article_like_sum       bigint      default 0                 null comment '文章总获赞数',
    article_bookmark_sum   bigint      default 0                 null comment '文章总收藏数',
    article_word_sum       bigint      default 0                 null comment '累计创作字数',
    book_finished          int         default 0                 null comment '读完的书',
    booklist_count         int         default 0                 null comment '创建书单数',
    quote_count            int         default 0                 null comment '发布金句数',
    reading_minutes        bigint      default 0                 null comment '累计阅读时长(分钟)',
    question_solved        int         default 0                 null comment '解题数',
    note_count             int         default 0                 null comment '笔记数',
    experience_count       int         default 0                 null comment '面经数',
    note_adopted           int         default 0                 null comment '笔记被精选数',
    follower_count         int         default 0                 null comment '粉丝数',
    following_count        int         default 0                 null comment '关注数',
    comment_count          int         default 0                 null comment '跨模块评论总数',
    total_like_received    bigint      default 0                 null comment '跨模块总获赞',
    checkin_streak         int         default 0                 null comment '连续签到天数',
    last_checkin_date      date                                  null comment '最后签到日期',
    create_by              varchar(64) default ''                null comment '创建者',
    create_time            datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by              varchar(64) default ''                null comment '更新者',
    update_time            datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark                 varchar(500)                          null comment '备注',
    weak_tags              text                                  null comment '薄弱知识点 JSON 数组（如 [{"tagId":1,"tagName":"Spring","failRate":0.6}]）',
    weak_tags_updated_time datetime                              null comment '薄弱点最后计算时间',
    constraint uk_user
        unique (user_id)
)
    comment '门户用户统计聚合表' charset = utf8mb4;

create table if not exists `moyun-db`.portal_user_task
(
    id             bigint auto_increment comment '主键'
        primary key,
    user_id        bigint                                not null comment '用户ID',
    task_id        bigint                                not null comment '任务ID',
    progress       int         default 0                 not null comment '当前进度',
    completed      tinyint     default 0                 not null comment '是否已完成 0/1',
    claimed        tinyint     default 0                 not null comment '是否已领取奖励 0/1',
    completed_time datetime                              null comment '完成时间',
    create_by      varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by      varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                          null comment '备注',
    constraint uk_user_task
        unique (user_id, task_id)
)
    comment '用户任务进度表' charset = utf8mb4;

create index idx_user
    on `moyun-db`.portal_user_task (user_id);

create table if not exists `moyun-db`.portal_vip_package
(
    id             bigint auto_increment comment '套餐ID'
        primary key,
    name           varchar(100)                          not null comment '套餐名称',
    price          decimal(10, 2)                        not null comment '价格',
    original_price decimal(10, 2)                        null comment '原价',
    duration       int                                   not null comment '有效期（天）',
    description    varchar(500)                          null comment '套餐描述',
    features       json                                  null comment '功能列表（JSON数组）',
    popular        tinyint(1)  default 0                 null comment '是否热门',
    sort           int         default 0                 null comment '排序',
    status         varchar(20) default 'active'          null comment '状态：active/inactive',
    create_by      varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by      varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                          null comment '备注',
    del_flag       char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '门户VIP套餐表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_vip_package (del_flag);

create index idx_status
    on `moyun-db`.portal_vip_package (status);

create table if not exists `moyun-db`.portal_voice_interview
(
    id                bigint auto_increment comment '主键'
        primary key,
    user_id           bigint                                not null comment '面试用户ID',
    position          varchar(64)                           null comment '面试岗位',
    scene             varchar(64)                           null comment '面试场景',
    resume_id         bigint                                null comment '简历ID（有简历时启用项目深挖题源）',
    agent_id          bigint                                null comment '面试官智能体ID（ai_agent.id，NULL=未绑定走默认逻辑）',
    phase             varchar(30)                           null comment '当前阶段:INTRO_WAITING/INTRO_RECEIVED/INTRO_FOLLOWUP/TECH_QUESTION/PROJECT_DEEP/SYSTEM_DESIGN/CANDIDATE_ASK/FINISHED（NULL=旧流程）',
    intro_score_json  text                                  null comment '自我介绍评分 JSON（4维度+总分+评语，ScoringEngine 产出）',
    status            varchar(16) default 'in_progress'     not null comment '状态 in_progress/finished',
    style             varchar(20) default 'professional'    null comment '面试官风格（字典 voice_interview_style）',
    difficulty        varchar(20) default 'medium'          null comment '难度 easy/medium/hard',
    total_qa          int         default 0                 not null comment '主问题目总数（不含追问）',
    current_idx       int         default 0                 not null comment '当前主问题目序号',
    score             int                                   null comment '面试总分（0-100）',
    summary           text                                  null comment 'AI 生成的面试总结',
    report            text                                  null comment '报告 JSON（含维度分/亮点/薄弱点/逐题点评）',
    share_token       varchar(64)                           null comment '报告分享令牌（NULL=未分享）',
    share_expire_time datetime                              null comment '分享过期时间',
    share_count       int         default 0                 not null comment '分享访问次数',
    config_json       text                                  null comment '配置 JSON（hintsEnabled/stuckThreshold/style/difficulty）',
    is_personalized   tinyint(1)  default 0                 null comment '是否基于画像抽题',
    profile_snapshot  text                                  null comment '抽题时的画像快照 JSON',
    create_time       datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time       datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    del_flag          char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_share_token
        unique (share_token)
)
    comment '语音面试会话主表（V10.1）' charset = utf8mb4;

create index idx_agent
    on `moyun-db`.portal_voice_interview (agent_id);

create index idx_del_flag
    on `moyun-db`.portal_voice_interview (del_flag);

create index idx_status
    on `moyun-db`.portal_voice_interview (status);

create index idx_user_time
    on `moyun-db`.portal_voice_interview (user_id, create_time);

create table if not exists `moyun-db`.portal_voice_interview_qa
(
    id                   bigint auto_increment comment '主键'
        primary key,
    interview_id         bigint                               not null comment '面试会话ID',
    question_id          bigint                               null comment '关联题目ID（portal_interview_question.id）',
    question_source      varchar(20)                          null comment '问题来源 bank=题库/resume_project=简历锚定/llm=智能体生成',
    question_idx         int                                  not null comment '主问题目序号（从0开始）',
    parent_qa_id         bigint                               null comment '追问父问答ID（NULL=主问）',
    question             varchar(1000)                        not null comment '面试问题',
    user_answer          text                                 null comment '用户回答（ASR 转写后可编辑）',
    transcription_edited tinyint(1) default 0                 null comment '转写是否被用户编辑',
    ai_feedback          text                                 null comment 'AI 反馈',
    speak_text           text                                 null comment 'AI 面试官话术（TTS 播报内容）',
    score                int                                  null comment '本题评分（0-100）',
    rule_dimensions_json text                                 null comment '规则维度分 JSON（6维对齐雷达图）',
    llm_score_json       text                                 null comment 'LLM 结构化评分 JSON（scores/total/strengths/weaknesses/comment）',
    llm_analysis_json    text                                 null comment 'LLM深度分析JSON（sentiment/fluency/redFlags/completeness）',
    hint_used            int        default 0                 null comment '已使用提示次数（0~3）',
    latency_ms           int                                  null comment '答题耗时（毫秒）',
    next_action          varchar(20)                          null comment '下一步动作 followup/hint/next/report',
    create_time          datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time          datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    del_flag             char       default '0'               not null comment '删除标记'
)
    comment '语音面试问答表（V10.1，含追问链）' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_voice_interview_qa (del_flag);

create index idx_interview
    on `moyun-db`.portal_voice_interview_qa (interview_id);

create index idx_parent
    on `moyun-db`.portal_voice_interview_qa (parent_qa_id);

create index idx_question_idx
    on `moyun-db`.portal_voice_interview_qa (interview_id, question_idx);

create table if not exists `moyun-db`.portal_wallet
(
    id             bigint auto_increment comment '钱包ID'
        primary key,
    user_id        bigint                                   not null comment '用户ID（门户用户ID）',
    balance        decimal(10, 2) default 0.00              null comment '余额',
    frozen_balance decimal(10, 2) default 0.00              null comment '冻结余额',
    total_recharge decimal(10, 2) default 0.00              null comment '累计充值',
    total_withdraw decimal(10, 2) default 0.00              null comment '累计提现',
    create_by      varchar(64)    default ''                null comment '创建者',
    create_time    datetime       default CURRENT_TIMESTAMP null comment '创建时间',
    update_by      varchar(64)    default ''                null comment '更新者',
    update_time    datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                             null comment '备注',
    del_flag       char           default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_user_id
        unique (user_id)
)
    comment '门户钱包表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_wallet (del_flag);

create table if not exists `moyun-db`.portal_wallet_transaction
(
    id             bigint auto_increment comment '交易ID'
        primary key,
    user_id        bigint                                not null comment '用户ID（门户用户ID）',
    type           varchar(50)                           not null comment '类型：recharge/consume/refund/withdraw',
    amount         decimal(10, 2)                        not null comment '金额',
    balance_before decimal(10, 2)                        not null comment '交易前余额',
    balance_after  decimal(10, 2)                        not null comment '交易后余额',
    description    varchar(500)                          null comment '描述',
    order_id       bigint                                null comment '关联订单ID',
    trade_no       varchar(64)                           null comment '第三方交易号（充值/提现场景的渠道方流水号）',
    channel        varchar(20)                           null comment '资金渠道：alipay/wechat/bank',
    refund_no      varchar(64)                           null comment '退款单号',
    create_by      varchar(64) default ''                null comment '创建者',
    create_time    datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by      varchar(64) default ''                null comment '更新者',
    update_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark         varchar(500)                          null comment '备注'
)
    comment '门户钱包交易记录表' charset = utf8mb4;

create index idx_trade_no
    on `moyun-db`.portal_wallet_transaction (trade_no);

create index idx_type
    on `moyun-db`.portal_wallet_transaction (type);

create index idx_user_id
    on `moyun-db`.portal_wallet_transaction (user_id);

create table if not exists `moyun-db`.portal_writing_contest
(
    id            bigint auto_increment comment '主键'
        primary key,
    title         varchar(128)                          not null comment '活动标题',
    description   text                                  null comment '活动描述',
    theme         varchar(128)                          null comment '征文主题',
    cover         varchar(500)                          null comment '封面',
    start_time    datetime                              null comment '活动开始时间',
    end_time      datetime                              null comment '投稿截止时间',
    vote_end_time datetime                              null comment '投票截止时间',
    prize         varchar(500)                          null comment '奖品说明',
    status        varchar(16) default 'draft'           not null comment 'draft/collecting/voting/ended',
    created_time  datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    updated_time  datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    del_flag      char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '创作挑战/征文活动' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_writing_contest (del_flag);

create index idx_start_time
    on `moyun-db`.portal_writing_contest (start_time);

create index idx_status
    on `moyun-db`.portal_writing_contest (status);

create table if not exists `moyun-db`.portal_writing_prompt
(
    id            bigint auto_increment comment '主键'
        primary key,
    prompt_date   date                                  not null comment 'prompt 日期（唯一）',
    title         varchar(128)                          not null comment 'prompt 标题',
    description   text                                  null comment 'prompt 描述',
    category      varchar(32)                           null comment '分类（如：生活/职场/情感/虚构/哲思）',
    festival_name varchar(64)                           null comment '关联特殊日期名称（节日/节气/纪念日）',
    source        varchar(16) default 'ai'              null comment '来源：ai=AI生成 / manual=手动创建',
    created_time  datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    del_flag      char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_prompt_date
        unique (prompt_date)
)
    comment '每日写作 prompt' charset = utf8mb4;

create index idx_category
    on `moyun-db`.portal_writing_prompt (category);

create index idx_del_flag
    on `moyun-db`.portal_writing_prompt (del_flag);

create table if not exists `moyun-db`.portal_wrong_question
(
    id               bigint auto_increment comment '主键'
        primary key,
    user_id          bigint                                not null comment '用户ID',
    question_id      bigint                                not null comment '题目ID',
    attempt_id       bigint                                null comment '最近一次答题ID',
    status           varchar(16) default 'wrong'           not null comment '状态 wrong/reviewing/mastered',
    wrong_count      int         default 1                 not null comment '答错次数',
    last_wrong_time  datetime                              null comment '最近答错时间',
    next_review_time datetime                              null comment '下次复习时间（艾宾浩斯）',
    created_time     datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    del_flag         char        default '0'               not null comment '删除标记（0=存在 2=删除）',
    constraint uk_user_question
        unique (user_id, question_id)
)
    comment '错题本' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.portal_wrong_question (del_flag);

create index idx_user_review
    on `moyun-db`.portal_wrong_question (user_id, next_review_time);

create index idx_user_status
    on `moyun-db`.portal_wrong_question (user_id, status);

create table if not exists `moyun-db`.qrtz_calendars
(
    sched_name    varchar(120) not null comment '调度名称',
    calendar_name varchar(200) not null comment '日历名称',
    calendar      blob         not null comment '存放持久化calendar对象',
    primary key (sched_name, calendar_name)
)
    comment '日历信息表' charset = utf8mb4;

create table if not exists `moyun-db`.qrtz_fired_triggers
(
    sched_name        varchar(120) not null comment '调度名称',
    entry_id          varchar(95)  not null comment '调度器实例id',
    trigger_name      varchar(200) not null comment 'qrtz_triggers表trigger_name的外键',
    trigger_group     varchar(200) not null comment 'qrtz_triggers表trigger_group的外键',
    instance_name     varchar(200) not null comment '调度器实例名',
    fired_time        bigint       not null comment '触发的时间',
    sched_time        bigint       not null comment '定时器制定的时间',
    priority          int          not null comment '优先级',
    state             varchar(16)  not null comment '状态',
    job_name          varchar(200) null comment '任务名称',
    job_group         varchar(200) null comment '任务组名',
    is_nonconcurrent  varchar(1)   null comment '是否并发',
    requests_recovery varchar(1)   null comment '是否接受恢复执行',
    primary key (sched_name, entry_id)
)
    comment '已触发的触发器表' charset = utf8mb4;

create table if not exists `moyun-db`.qrtz_job_details
(
    sched_name        varchar(120) not null comment '调度名称',
    job_name          varchar(200) not null comment '任务名称',
    job_group         varchar(200) not null comment '任务组名',
    description       varchar(250) null comment '相关介绍',
    job_class_name    varchar(250) not null comment '执行任务类名称',
    is_durable        varchar(1)   not null comment '是否持久化',
    is_nonconcurrent  varchar(1)   not null comment '是否并发',
    is_update_data    varchar(1)   not null comment '是否更新数据',
    requests_recovery varchar(1)   not null comment '是否接受恢复执行',
    job_data          blob         null comment '存放持久化job对象',
    primary key (sched_name, job_name, job_group)
)
    comment '任务详细信息表' charset = utf8mb4;

create table if not exists `moyun-db`.qrtz_locks
(
    sched_name varchar(120) not null comment '调度名称',
    lock_name  varchar(40)  not null comment '悲观锁名称',
    primary key (sched_name, lock_name)
)
    comment '存储的悲观锁信息表' charset = utf8mb4;

create table if not exists `moyun-db`.qrtz_paused_trigger_grps
(
    sched_name    varchar(120) not null comment '调度名称',
    trigger_group varchar(200) not null comment 'qrtz_triggers表trigger_group的外键',
    primary key (sched_name, trigger_group)
)
    comment '暂停的触发器表' charset = utf8mb4;

create table if not exists `moyun-db`.qrtz_scheduler_state
(
    sched_name        varchar(120) not null comment '调度名称',
    instance_name     varchar(200) not null comment '实例名称',
    last_checkin_time bigint       not null comment '上次检查时间',
    checkin_interval  bigint       not null comment '检查间隔时间',
    primary key (sched_name, instance_name)
)
    comment '调度器状态表' charset = utf8mb4;

create table if not exists `moyun-db`.qrtz_triggers
(
    sched_name     varchar(120) not null comment '调度名称',
    trigger_name   varchar(200) not null comment '触发器的名字',
    trigger_group  varchar(200) not null comment '触发器所属组的名字',
    job_name       varchar(200) not null comment 'qrtz_job_details表job_name的外键',
    job_group      varchar(200) not null comment 'qrtz_job_details表job_group的外键',
    description    varchar(250) null comment '相关介绍',
    next_fire_time bigint       null comment '上一次触发时间（毫秒）',
    prev_fire_time bigint       null comment '下一次触发时间（默认为-1表示不触发）',
    priority       int          null comment '优先级',
    trigger_state  varchar(16)  not null comment '触发器状态',
    trigger_type   varchar(8)   not null comment '触发器的类型',
    start_time     bigint       not null comment '开始时间',
    end_time       bigint       null comment '结束时间',
    calendar_name  varchar(200) null comment '日程表名称',
    misfire_instr  smallint     null comment '补偿执行的策略',
    job_data       blob         null comment '存放持久化job对象',
    primary key (sched_name, trigger_name, trigger_group),
    constraint qrtz_triggers_ibfk_1
        foreign key (sched_name, job_name, job_group) references `moyun-db`.qrtz_job_details (sched_name, job_name, job_group)
)
    comment '触发器详细信息表' charset = utf8mb4;

create table if not exists `moyun-db`.qrtz_blob_triggers
(
    sched_name    varchar(120) not null comment '调度名称',
    trigger_name  varchar(200) not null comment 'qrtz_triggers表trigger_name的外键',
    trigger_group varchar(200) not null comment 'qrtz_triggers表trigger_group的外键',
    blob_data     blob         null comment '存放持久化Trigger对象',
    primary key (sched_name, trigger_name, trigger_group),
    constraint qrtz_blob_triggers_ibfk_1
        foreign key (sched_name, trigger_name, trigger_group) references `moyun-db`.qrtz_triggers (sched_name, trigger_name, trigger_group)
)
    comment 'Blob类型的触发器表' charset = utf8mb4;

create table if not exists `moyun-db`.qrtz_cron_triggers
(
    sched_name      varchar(120) not null comment '调度名称',
    trigger_name    varchar(200) not null comment 'qrtz_triggers表trigger_name的外键',
    trigger_group   varchar(200) not null comment 'qrtz_triggers表trigger_group的外键',
    cron_expression varchar(200) not null comment 'cron表达式',
    time_zone_id    varchar(80)  null comment '时区',
    primary key (sched_name, trigger_name, trigger_group),
    constraint qrtz_cron_triggers_ibfk_1
        foreign key (sched_name, trigger_name, trigger_group) references `moyun-db`.qrtz_triggers (sched_name, trigger_name, trigger_group)
)
    comment 'Cron类型的触发器表' charset = utf8mb4;

create table if not exists `moyun-db`.qrtz_simple_triggers
(
    sched_name      varchar(120) not null comment '调度名称',
    trigger_name    varchar(200) not null comment 'qrtz_triggers表trigger_name的外键',
    trigger_group   varchar(200) not null comment 'qrtz_triggers表trigger_group的外键',
    repeat_count    bigint       not null comment '重复的次数统计',
    repeat_interval bigint       not null comment '重复的间隔时间',
    times_triggered bigint       not null comment '已经触发的次数',
    primary key (sched_name, trigger_name, trigger_group),
    constraint qrtz_simple_triggers_ibfk_1
        foreign key (sched_name, trigger_name, trigger_group) references `moyun-db`.qrtz_triggers (sched_name, trigger_name, trigger_group)
)
    comment '简单触发器的信息表' charset = utf8mb4;

create table if not exists `moyun-db`.qrtz_simprop_triggers
(
    sched_name    varchar(120)   not null comment '调度名称',
    trigger_name  varchar(200)   not null comment 'qrtz_triggers表trigger_name的外键',
    trigger_group varchar(200)   not null comment 'qrtz_triggers表trigger_group的外键',
    str_prop_1    varchar(512)   null comment 'String类型的trigger的第一个参数',
    str_prop_2    varchar(512)   null comment 'String类型的trigger的第二个参数',
    str_prop_3    varchar(512)   null comment 'String类型的trigger的第三个参数',
    int_prop_1    int            null comment 'int类型的trigger的第一个参数',
    int_prop_2    int            null comment 'int类型的trigger的第二个参数',
    long_prop_1   bigint         null comment 'long类型的trigger的第一个参数',
    long_prop_2   bigint         null comment 'long类型的trigger的第二个参数',
    dec_prop_1    decimal(13, 4) null comment 'decimal类型的trigger的第一个参数',
    dec_prop_2    decimal(13, 4) null comment 'decimal类型的trigger的第二个参数',
    bool_prop_1   varchar(1)     null comment 'Boolean类型的trigger的第一个参数',
    bool_prop_2   varchar(1)     null comment 'Boolean类型的trigger的第二个参数',
    primary key (sched_name, trigger_name, trigger_group),
    constraint qrtz_simprop_triggers_ibfk_1
        foreign key (sched_name, trigger_name, trigger_group) references `moyun-db`.qrtz_triggers (sched_name, trigger_name, trigger_group)
)
    comment '同步机制的行锁表' charset = utf8mb4;

create index sched_name
    on `moyun-db`.qrtz_triggers (sched_name, job_name, job_group);

create table if not exists `moyun-db`.sys_audit_task
(
    id             bigint auto_increment comment '主键'
        primary key,
    task_type      varchar(32)                   not null comment '任务类型：article/column/topic/interview_exp/interview_comment/certification/feedback/report',
    biz_type       varchar(32)                   null comment '业务子类型（如 report 的 spam/infringement）',
    biz_id         bigint                        not null comment '业务记录ID',
    title          varchar(255)                  not null comment '任务标题',
    description    text                          null comment '任务描述/摘要',
    submitter_id   bigint                        null comment '提交人ID（门户用户ID）',
    submitter_name varchar(64)                   null comment '提交人用户名',
    status         varchar(20) default 'pending' not null comment '状态：pending/approved/rejected',
    auditor_id     bigint                        null comment '处理人ID（系统用户ID）',
    auditor_name   varchar(64)                   null comment '处理人用户名',
    audit_opinion  varchar(1000)                 null comment '审核意见（驳回时必填）',
    audit_action   varchar(20)                   null comment '审核操作类型：approve/reject',
    submit_time    datetime                      null comment '提交时间',
    audit_time     datetime                      null comment '处理时间',
    priority       varchar(10) default 'medium'  not null comment '优先级：high/medium/low',
    route_path     varchar(255)                  null comment '查看详情跳转路径',
    extra_data     text                          null comment '扩展数据 JSON',
    create_time    datetime                      null comment '创建时间',
    update_time    datetime                      null comment '更新时间'
)
    comment '统一审核任务表（v8.1）' charset = utf8mb4;

create index idx_audit_auditor
    on `moyun-db`.sys_audit_task (auditor_id);

create index idx_audit_biz
    on `moyun-db`.sys_audit_task (biz_type, biz_id);

create index idx_audit_status
    on `moyun-db`.sys_audit_task (status);

create index idx_audit_submit_time
    on `moyun-db`.sys_audit_task (submit_time);

create index idx_audit_submitter
    on `moyun-db`.sys_audit_task (submitter_id);

create index idx_audit_task_type
    on `moyun-db`.sys_audit_task (task_type, status);

create table if not exists `moyun-db`.sys_config
(
    config_id    int auto_increment comment '参数主键'
        primary key,
    config_name  varchar(100) default ''  null comment '参数名称',
    config_key   varchar(100) default ''  null comment '参数键名',
    config_value varchar(500) default ''  null comment '参数键值',
    config_type  char         default 'N' null comment '系统内置（Y是 N否）',
    create_by    varchar(64)  default ''  null comment '创建者',
    create_time  datetime                 null comment '创建时间',
    update_by    varchar(64)  default ''  null comment '更新者',
    update_time  datetime                 null comment '更新时间',
    remark       varchar(500)             null comment '备注',
    del_flag     char         default '0' not null comment '删除标记（0=存在 2=删除）'
)
    comment '参数配置表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.sys_config (del_flag);

create table if not exists `moyun-db`.sys_dept
(
    dept_id     bigint auto_increment comment '部门id'
        primary key,
    parent_id   bigint      default 0   null comment '父部门id',
    ancestors   varchar(50) default ''  null comment '祖级列表',
    dept_name   varchar(30) default ''  null comment '部门名称',
    order_num   int         default 0   null comment '显示顺序',
    leader      varchar(20)             null comment '负责人',
    phone       varchar(11)             null comment '联系电话',
    email       varchar(50)             null comment '邮箱',
    status      char        default '0' null comment '部门状态（0正常 1停用）',
    del_flag    char        default '0' null comment '删除标记（0=存在 2=删除）',
    create_by   varchar(64) default ''  null comment '创建者',
    create_time datetime                null comment '创建时间',
    update_by   varchar(64) default ''  null comment '更新者',
    update_time datetime                null comment '更新时间',
    remark      varchar(500)            null comment '备注'
)
    comment '部门表' charset = utf8mb4;

create table if not exists `moyun-db`.sys_dict_data
(
    dict_code   bigint auto_increment comment '字典编码'
        primary key,
    dict_sort   int          default 0   null comment '字典排序',
    dict_label  varchar(100) default ''  null comment '字典标签',
    dict_value  varchar(100) default ''  null comment '字典键值',
    dict_type   varchar(100) default ''  null comment '字典类型',
    css_class   varchar(100)             null comment '样式属性（其他样式扩展）',
    list_class  varchar(100)             null comment '表格回显样式',
    is_default  char         default 'N' null comment '是否默认（Y是 N否）',
    status      char         default '0' null comment '状态（0正常 1停用）',
    create_by   varchar(64)  default ''  null comment '创建者',
    create_time datetime                 null comment '创建时间',
    update_by   varchar(64)  default ''  null comment '更新者',
    update_time datetime                 null comment '更新时间',
    remark      varchar(500)             null comment '备注',
    del_flag    char         default '0' not null comment '删除标记（0=存在 2=删除）'
)
    comment '字典数据表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.sys_dict_data (del_flag);

create table if not exists `moyun-db`.sys_dict_type
(
    dict_id     bigint auto_increment comment '字典主键'
        primary key,
    dict_name   varchar(100) default ''  null comment '字典名称',
    dict_type   varchar(100) default ''  null comment '字典类型',
    status      char         default '0' null comment '状态（0正常 1停用）',
    create_by   varchar(64)  default ''  null comment '创建者',
    create_time datetime                 null comment '创建时间',
    update_by   varchar(64)  default ''  null comment '更新者',
    update_time datetime                 null comment '更新时间',
    remark      varchar(500)             null comment '备注',
    del_flag    char         default '0' not null comment '删除标记（0=存在 2=删除）',
    constraint uk_dict_type
        unique (dict_type)
)
    comment '字典类型表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.sys_dict_type (del_flag);

create table if not exists `moyun-db`.sys_file
(
    id               bigint auto_increment comment '文件ID'
        primary key,
    file_name        varchar(500)            not null comment '文件名称',
    file_ext         varchar(100)            null comment '文件扩展名',
    file_type        varchar(100)            null comment '文件类型（image/document/video/audio/other）',
    file_size        bigint                  null comment '文件大小（字节）',
    file_url         varchar(1000)           not null comment '文件访问URL',
    file_path        varchar(500)            null comment '文件路径',
    storage_type     varchar(100)            null comment '存储类型（minio/local）',
    bucket_name      varchar(100)            null comment '存储桶名称',
    object_name      varchar(500)            null comment '对象名称',
    fallback         tinyint(1)  default 0   null comment '是否降级存储（0=正常 1=因MinIO不可用降级到本地）',
    local_path       varchar(500)            null comment '本地备份绝对路径（MinIO可用时也记录，便于降级访问）',
    file_md5         varchar(500)            null comment '文件MD5值',
    upload_user_id   bigint                  null comment '上传用户ID',
    upload_user_name varchar(100)            null comment '上传用户名称',
    status           varchar(20) default '0' null comment '状态（0正常 1停用）',
    business_type    varchar(500)            null comment '业务类型',
    business_id      varchar(100)            null comment '业务ID',
    create_by        varchar(64) default ''  null comment '创建者',
    create_time      datetime                null comment '创建时间',
    update_by        varchar(64) default ''  null comment '更新者',
    update_time      datetime                null comment '更新时间',
    remark           varchar(500)            null comment '备注',
    del_flag         char        default '0' not null comment '删除标记（0=存在 2=删除）'
)
    comment '文件管理表' charset = utf8mb4;

create index idx_business_id
    on `moyun-db`.sys_file (business_id);

create index idx_business_type
    on `moyun-db`.sys_file (business_type);

create index idx_create_time
    on `moyun-db`.sys_file (create_time);

create index idx_del_flag
    on `moyun-db`.sys_file (del_flag);

create index idx_fallback
    on `moyun-db`.sys_file (fallback);

create index idx_file_type
    on `moyun-db`.sys_file (file_type);

create index idx_storage_type
    on `moyun-db`.sys_file (storage_type);

create index idx_upload_user_id
    on `moyun-db`.sys_file (upload_user_id);

create table if not exists `moyun-db`.sys_job
(
    job_id          bigint auto_increment comment '任务ID',
    job_name        varchar(64)  default ''        not null comment '任务名称',
    job_group       varchar(64)  default 'DEFAULT' not null comment '任务组名',
    invoke_target   varchar(500)                   not null comment '调用目标字符串',
    cron_expression varchar(255) default ''        null comment 'cron执行表达式',
    misfire_policy  varchar(20)  default '3'       null comment '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
    concurrent      char         default '1'       null comment '是否并发执行（0允许 1禁止）',
    status          char         default '0'       null comment '状态（0正常 1暂停）',
    create_by       varchar(64)  default ''        null comment '创建者',
    create_time     datetime                       null comment '创建时间',
    update_by       varchar(64)  default ''        null comment '更新者',
    update_time     datetime                       null comment '更新时间',
    remark          varchar(500) default ''        null comment '备注信息',
    del_flag        char         default '0'       not null comment '删除标记（0=存在 2=删除）',
    primary key (job_id, job_name, job_group)
)
    comment '定时任务调度表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.sys_job (del_flag);

create table if not exists `moyun-db`.sys_job_log
(
    job_log_id     bigint auto_increment comment '任务日志ID'
        primary key,
    job_name       varchar(64)               not null comment '任务名称',
    job_group      varchar(64)               not null comment '任务组名',
    invoke_target  varchar(500)              not null comment '调用目标字符串',
    job_message    varchar(500)              null comment '日志信息',
    status         char          default '0' null comment '执行状态（0正常 1失败）',
    exception_info varchar(2000) default ''  null comment '异常信息',
    create_time    datetime                  null comment '创建时间',
    create_by      varchar(64)   default ''  null comment '创建者',
    update_by      varchar(64)   default ''  null comment '更新者',
    update_time    datetime                  null comment '更新时间',
    remark         varchar(500)              null comment '备注'
)
    comment '定时任务调度日志表' charset = utf8mb4;

create table if not exists `moyun-db`.sys_job_scan_issue
(
    id            bigint auto_increment comment '主键'
        primary key,
    job_id        bigint                        null comment '触发扫描的定时任务ID',
    job_name      varchar(64)                   null comment '定时任务名称',
    issue_type    varchar(32)                   not null comment '问题类型：sensitive_word/pending_overdue/anomaly/other',
    issue_desc    varchar(500)                  not null comment '问题描述',
    target_type   varchar(32)                   null comment '目标对象类型',
    target_id     bigint                        null comment '目标对象ID',
    target_title  varchar(255)                  null comment '目标对象标题/摘要',
    log_excerpt   text                          null comment '日志摘要',
    status        varchar(20) default 'pending' not null comment '状态：pending/handled/ignored',
    handler_id    bigint                        null comment '处理人ID',
    handler_name  varchar(64)                   null comment '处理人用户名',
    handle_result varchar(500)                  null comment '处理结果说明',
    handle_time   datetime                      null comment '处理时间',
    create_time   datetime                      null comment '扫描发现时间'
)
    comment '定时任务扫描结果表（v8.1）' charset = utf8mb4;

create index idx_scan_create_time
    on `moyun-db`.sys_job_scan_issue (create_time);

create index idx_scan_job
    on `moyun-db`.sys_job_scan_issue (job_id);

create index idx_scan_status
    on `moyun-db`.sys_job_scan_issue (status);

create index idx_scan_target
    on `moyun-db`.sys_job_scan_issue (target_type, target_id);

create table if not exists `moyun-db`.sys_logininfor
(
    info_id        bigint auto_increment comment '访问ID'
        primary key,
    user_name      varchar(50)  default ''    null comment '用户账号',
    ipaddr         varchar(128) default ''    null comment '登录IP地址',
    login_location varchar(255) default ''    null comment '登录地点',
    browser        varchar(50)  default ''    null comment '浏览器类型',
    os             varchar(50)  default ''    null comment '操作系统',
    status         char         default '0'   null comment '登录状态（0成功 1失败）',
    user_type      varchar(10)  default 'sys' null comment '登录来源类型（sys=后台用户 portal=门户用户）',
    msg            varchar(255) default ''    null comment '提示消息',
    login_time     datetime                   null comment '访问时间',
    create_by      varchar(64)  default ''    null comment '创建者',
    create_time    datetime                   null comment '创建时间',
    update_by      varchar(64)  default ''    null comment '更新者',
    update_time    datetime                   null comment '更新时间',
    remark         varchar(500)               null comment '备注'
)
    comment '系统访问记录' charset = utf8mb4;

create index idx_sys_logininfor_lt
    on `moyun-db`.sys_logininfor (login_time);

create index idx_sys_logininfor_s
    on `moyun-db`.sys_logininfor (status);

create index idx_sys_logininfor_ut
    on `moyun-db`.sys_logininfor (user_type);

create table if not exists `moyun-db`.sys_menu
(
    menu_id     bigint auto_increment comment '菜单ID'
        primary key,
    menu_name   varchar(50)              not null comment '菜单名称',
    parent_id   bigint       default 0   null comment '父菜单ID',
    order_num   int          default 0   null comment '显示顺序',
    path        varchar(200) default ''  null comment '路由地址',
    component   varchar(255)             null comment '组件路径',
    query       varchar(255)             null comment '路由参数',
    route_name  varchar(50)  default ''  null comment '路由名称',
    is_frame    int          default 1   null comment '是否为外链（0是 1否）',
    is_cache    int          default 0   null comment '是否缓存（0缓存 1不缓存）',
    menu_type   char         default ''  null comment '菜单类型（M目录 C菜单 F按钮）',
    visible     char         default '0' null comment '菜单状态（0显示 1隐藏）',
    status      char         default '0' null comment '菜单状态（0正常 1停用）',
    perms       varchar(100)             null comment '权限标识',
    icon        varchar(100) default '#' null comment '菜单图标',
    create_by   varchar(64)  default ''  null comment '创建者',
    create_time datetime                 null comment '创建时间',
    update_by   varchar(64)  default ''  null comment '更新者',
    update_time datetime                 null comment '更新时间',
    remark      varchar(500) default ''  null comment '备注',
    del_flag    char         default '0' not null comment '删除标记（0=存在 2=删除）'
)
    comment '菜单权限表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.sys_menu (del_flag);

create table if not exists `moyun-db`.sys_notification
(
    id          bigint auto_increment comment '通知ID'
        primary key,
    type        varchar(50)                           not null comment '类型：system/comment/like/follow/order/notice/announcement',
    title       varchar(200)                          null comment '通知标题',
    content     text                                  null comment '通知内容',
    data        json                                  null comment '通知数据（JSON格式）',
    scope       varchar(20) default 'user'            not null comment '范围：user=个人通知 / all=全局广播',
    user_id     bigint                                null comment '接收用户ID（scope=user 时必填，scope=all 时为 NULL）',
    user_type   varchar(20) default 'portal'          not null comment '接收用户类型：portal=门户用户 / sys=系统用户（scope=user 时生效）',
    notice_type char                                  null comment '通知/公告分类：1=通知 / 2=公告（兼容 sys_notice 字典 sys_notice_type）',
    status      char        default '0'               null comment '状态：0=正常 / 1=关闭（兼容 sys_notice 字典 sys_notice_status）',
    create_by   varchar(64) default ''                null comment '创建者',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_by   varchar(64) default ''                null comment '更新者',
    update_time datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    remark      varchar(500)                          null comment '备注',
    del_flag    char        default '0'               not null comment '删除标记（0=存在 2=删除）'
)
    comment '系统通知主体表（合并 portal_notification + sys_notice）' charset = utf8mb4;

create index idx_create_time
    on `moyun-db`.sys_notification (create_time);

create index idx_del_flag
    on `moyun-db`.sys_notification (del_flag);

create index idx_scope
    on `moyun-db`.sys_notification (scope);

create index idx_status
    on `moyun-db`.sys_notification (status);

create index idx_type
    on `moyun-db`.sys_notification (type);

create index idx_user_id
    on `moyun-db`.sys_notification (user_id);

create index idx_user_type_user_id
    on `moyun-db`.sys_notification (user_type, user_id);

create table if not exists `moyun-db`.sys_notification_read
(
    id              bigint auto_increment comment '主键'
        primary key,
    notification_id bigint                                not null comment '通知ID（关联 sys_notification.id）',
    user_id         bigint                                not null comment '用户ID',
    user_type       varchar(20) default 'portal'          not null comment '已读用户类型：portal=门户用户 / sys=系统用户',
    read_time       datetime    default CURRENT_TIMESTAMP null comment '阅读时间',
    create_time     datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_notif_user_type
        unique (notification_id, user_id, user_type)
)
    comment '系统通知用户已读关系表' charset = utf8mb4;

create index idx_notification_id
    on `moyun-db`.sys_notification_read (notification_id);

create index idx_user_id
    on `moyun-db`.sys_notification_read (user_id);

create table if not exists `moyun-db`.sys_oper_log
(
    oper_id        bigint auto_increment comment '日志主键'
        primary key,
    title          varchar(50)   default '' null comment '模块标题',
    business_type  int           default 0  null comment '业务类型（0其它 1新增 2修改 3删除）',
    method         varchar(200)  default '' null comment '方法名称',
    request_method varchar(10)   default '' null comment '请求方式',
    operator_type  int           default 0  null comment '操作类别（0其它 1后台用户 2手机端用户）',
    oper_name      varchar(50)   default '' null comment '操作人员',
    dept_name      varchar(50)   default '' null comment '部门名称',
    oper_url       varchar(255)  default '' null comment '请求URL',
    oper_ip        varchar(128)  default '' null comment '主机地址',
    oper_location  varchar(255)  default '' null comment '操作地点',
    oper_param     varchar(2000) default '' null comment '请求参数',
    json_result    varchar(2000) default '' null comment '返回参数',
    status         int           default 0  null comment '操作状态（0正常 1异常）',
    error_msg      varchar(2000) default '' null comment '错误消息',
    oper_time      datetime                 null comment '操作时间',
    cost_time      bigint        default 0  null comment '消耗时间',
    create_by      varchar(64)   default '' null comment '创建者',
    create_time    datetime                 null comment '创建时间',
    update_by      varchar(64)   default '' null comment '更新者',
    update_time    datetime                 null comment '更新时间',
    remark         varchar(500)             null comment '备注'
)
    comment '操作日志记录' charset = utf8mb4;

create index idx_sys_oper_log_bt
    on `moyun-db`.sys_oper_log (business_type);

create index idx_sys_oper_log_ot
    on `moyun-db`.sys_oper_log (oper_time);

create index idx_sys_oper_log_s
    on `moyun-db`.sys_oper_log (status);

create table if not exists `moyun-db`.sys_post
(
    post_id     bigint auto_increment comment '岗位ID'
        primary key,
    post_code   varchar(64)             not null comment '岗位编码',
    post_name   varchar(50)             not null comment '岗位名称',
    post_sort   int                     not null comment '显示顺序',
    status      char                    not null comment '状态（0正常 1停用）',
    create_by   varchar(64) default ''  null comment '创建者',
    create_time datetime                null comment '创建时间',
    update_by   varchar(64) default ''  null comment '更新者',
    update_time datetime                null comment '更新时间',
    remark      varchar(500)            null comment '备注',
    del_flag    char        default '0' not null comment '删除标记（0=存在 2=删除）'
)
    comment '岗位信息表' charset = utf8mb4;

create index idx_del_flag
    on `moyun-db`.sys_post (del_flag);

create table if not exists `moyun-db`.sys_role
(
    role_id             bigint auto_increment comment '角色ID'
        primary key,
    role_name           varchar(30)             not null comment '角色名称',
    role_key            varchar(100)            not null comment '角色权限字符串',
    role_sort           int                     not null comment '显示顺序',
    data_scope          char        default '1' null comment '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
    menu_check_strictly tinyint(1)  default 1   null comment '菜单树选择项是否关联显示',
    dept_check_strictly tinyint(1)  default 1   null comment '部门树选择项是否关联显示',
    status              char                    not null comment '角色状态（0正常 1停用）',
    del_flag            char        default '0' null comment '删除标记（0=存在 2=删除）',
    create_by           varchar(64) default ''  null comment '创建者',
    create_time         datetime                null comment '创建时间',
    update_by           varchar(64) default ''  null comment '更新者',
    update_time         datetime                null comment '更新时间',
    remark              varchar(500)            null comment '备注'
)
    comment '角色信息表' charset = utf8mb4;

create table if not exists `moyun-db`.sys_role_dept
(
    role_id     bigint                 not null comment '角色ID',
    dept_id     bigint                 not null comment '部门ID',
    create_by   varchar(64) default '' null comment '创建者',
    create_time datetime               null comment '创建时间',
    update_by   varchar(64) default '' null comment '更新者',
    update_time datetime               null comment '更新时间',
    remark      varchar(500)           null comment '备注',
    primary key (role_id, dept_id)
)
    comment '角色和部门关联表' charset = utf8mb4;

create table if not exists `moyun-db`.sys_role_menu
(
    role_id     bigint                 not null comment '角色ID',
    menu_id     bigint                 not null comment '菜单ID',
    create_by   varchar(64) default '' null comment '创建者',
    create_time datetime               null comment '创建时间',
    update_by   varchar(64) default '' null comment '更新者',
    update_time datetime               null comment '更新时间',
    remark      varchar(500)           null comment '备注',
    primary key (role_id, menu_id)
)
    comment '角色和菜单关联表' charset = utf8mb4;

create table if not exists `moyun-db`.sys_sensitive_word
(
    id          bigint auto_increment comment '主键'
        primary key,
    word        varchar(128)     not null comment '敏感词',
    category    varchar(32)      null comment '分类：politics=政治/porn=色情/ad=广告/insult=辱骂/other=其他',
    status      char default '0' not null comment '状态：0=启用 1=禁用',
    create_by   varchar(64)      null comment '创建者',
    create_time datetime         null comment '创建时间',
    update_by   varchar(64)      null comment '更新者',
    update_time datetime         null comment '更新时间',
    remark      varchar(255)     null comment '备注',
    del_flag    char default '0' not null comment '删除标记：0=存在 2=删除（BaseEntity 逻辑删除）',
    constraint uk_word
        unique (word)
)
    comment '敏感词库' charset = utf8mb4;

create index idx_status
    on `moyun-db`.sys_sensitive_word (status);

create table if not exists `moyun-db`.sys_sensitive_word_log
(
    id          bigint auto_increment comment '主键'
        primary key,
    biz_type    varchar(32)                           not null comment '业务类型：article/column/topic/topic_post/topic_comment/report',
    biz_id      bigint                                null comment '业务主键ID',
    user_id     bigint                                null comment '提交人ID（portal_user.id）',
    content     text                                  null comment '被检测的原始内容片段（截断）',
    hit_words   varchar(500)                          null comment '命中的敏感词列表（逗号分隔）',
    hit_count   int         default 0                 not null comment '命中数量',
    action      varchar(16) default 'block'           not null comment '处理动作：block=拦截/pending=转待审核/flag=标记',
    create_time datetime    default CURRENT_TIMESTAMP not null comment '检测时间'
)
    comment '敏感词命中记录' charset = utf8mb4;

create index idx_biz
    on `moyun-db`.sys_sensitive_word_log (biz_type, biz_id);

create index idx_create_time
    on `moyun-db`.sys_sensitive_word_log (create_time);

create index idx_user_id
    on `moyun-db`.sys_sensitive_word_log (user_id);

create table if not exists `moyun-db`.sys_user
(
    user_id     bigint auto_increment comment '用户ID'
        primary key,
    dept_id     bigint                    null comment '部门ID',
    user_name   varchar(30)               not null comment '用户账号',
    nick_name   varchar(30)               not null comment '用户昵称',
    user_type   varchar(2)   default '00' null comment '用户类型（00系统用户）',
    email       varchar(50)  default ''   null comment '用户邮箱',
    phonenumber varchar(11)  default ''   null comment '手机号码',
    sex         char         default '0'  null comment '用户性别（0男 1女 2未知）',
    avatar      varchar(100) default ''   null comment '头像地址',
    password    varchar(100) default ''   null comment '密码',
    status      char         default '0'  null comment '账号状态（0正常 1停用）',
    del_flag    char         default '0'  null comment '删除标记（0=存在 2=删除）',
    login_ip    varchar(128) default ''   null comment '最后登录IP',
    login_date  datetime                  null comment '最后登录时间',
    create_by   varchar(64)  default ''   null comment '创建者',
    create_time datetime                  null comment '创建时间',
    update_by   varchar(64)  default ''   null comment '更新者',
    update_time datetime                  null comment '更新时间',
    remark      varchar(500)              null comment '备注'
)
    comment '用户信息表' charset = utf8mb4;

create table if not exists `moyun-db`.sys_user_post
(
    user_id     bigint                 not null comment '用户ID',
    post_id     bigint                 not null comment '岗位ID',
    create_by   varchar(64) default '' null comment '创建者',
    create_time datetime               null comment '创建时间',
    update_by   varchar(64) default '' null comment '更新者',
    update_time datetime               null comment '更新时间',
    remark      varchar(500)           null comment '备注',
    primary key (user_id, post_id)
)
    comment '用户与岗位关联表' charset = utf8mb4;

create table if not exists `moyun-db`.sys_user_role
(
    user_id     bigint                 not null comment '用户ID',
    role_id     bigint                 not null comment '角色ID',
    create_by   varchar(64) default '' null comment '创建者',
    create_time datetime               null comment '创建时间',
    update_by   varchar(64) default '' null comment '更新者',
    update_time datetime               null comment '更新时间',
    remark      varchar(500)           null comment '备注',
    primary key (user_id, role_id)
)
    comment '用户和角色关联表' charset = utf8mb4;

