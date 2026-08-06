package com.moyun.ext.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.util.JsonUtils;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.KnowledgeBase;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.entity.Workflow;
import com.moyun.ext.ai.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作流自然语言生成服务实现
 * 
 * <p>核心功能：通过自然语言描述自动生成完整的工作流定义</p>
 * 
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowGeneratorServiceImpl implements WorkflowGeneratorService {
    
    private final LLMService llmService;
    private final AgentService agentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final WorkflowService workflowService;
    private final ModelConfigService modelConfigService;
    
    /**
     * 资源目录缓存，避免每次生成都查询数据库
     * 缓存会在资源变更时自动失效
     */
    private volatile String resourceCatalogCache = null;
    private volatile long resourceCacheTime = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5分钟缓存
    
    /** 节点横向间距 */
    private static final int NODE_SPACING_X = 250;
    /** 节点纵向间距 */
    private static final int NODE_SPACING_Y = 150;
    /** 起始X坐标 */
    private static final int START_X = 100;
    /** 起始Y坐标 */
    private static final int START_Y = 200;
    
    @Override
    public GenerateResult generate(String description) {
        StopWatch stopWatch = new StopWatch("WorkflowGeneration");
        
        log.info("🪄 开始生成工作流，描述长度: {} 字符", description.length());
        
        try {
            // 1. 构建完整的Prompt（包含系统prompt和用户prompt）
            stopWatch.start("构建Prompt");
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(description);
            String fullPrompt = systemPrompt + "\n\n" + userPrompt;
            stopWatch.stop();
            
            log.debug("📝 Prompt构建完成，长度: {} 字符", fullPrompt.length());
            
            // 2. 调用LLM生成工作流
            stopWatch.start("LLM调用");
            String response = llmService.generate(fullPrompt);
            stopWatch.stop();
            
            log.info("🤖 LLM响应长度: {} 字符", response.length());
            log.debug("🤖 LLM响应预览: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
            
            // 3. 提取并解析JSON
            stopWatch.start("解析JSON");
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                log.error("❌ JSON提取失败，LLM响应: {}", response);
                return GenerateResult.fail("无法从LLM响应中提取有效的JSON，请重试");
            }
            
            Map<String, Object> workflowData = JsonUtils.fromJson(jsonStr, 
                    new TypeReference<Map<String, Object>>() {});
            if (workflowData == null) {
                log.error("❌ JSON解析失败: {}", jsonStr);
                return GenerateResult.fail("JSON格式错误");
            }
            stopWatch.stop();
            
            // 4. 校验和修复工作流定义
            stopWatch.start("校验修复");
            workflowData = validateAndFix(workflowData);
            stopWatch.stop();
            
            // 5. 自动布局节点位置
            stopWatch.start("自动布局");
            workflowData = autoLayout(workflowData);
            stopWatch.stop();
            
            // 6. 提取工作流元信息
            String workflowName = (String) workflowData.getOrDefault("name", "自动生成的工作流");
            String workflowDesc = (String) workflowData.getOrDefault("description", description);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) workflowData.get("nodes");
            int nodeCount = nodes != null ? nodes.size() : 0;
            
            // 7. 生成用户友好的说明文本
            stopWatch.start("生成说明");
            String explanation = generateExplanation(workflowData);
            stopWatch.stop();
            
            // 8. 构建最终的graphData（只包含nodes和edges）
            Map<String, Object> graphData = new HashMap<>();
            graphData.put("nodes", workflowData.get("nodes"));
            graphData.put("edges", workflowData.get("edges"));
            String graphJson = JsonUtils.toJson(graphData);
            
            log.info("✅ 工作流生成成功: name={}, nodes={}, 耗时={}ms", 
                    workflowName, nodeCount, stopWatch.getTotalTimeMillis());
            log.debug("⏱️  详细耗时: {}", stopWatch.prettyPrint());
            
            return GenerateResult.success(graphJson, workflowName, workflowDesc, nodeCount, explanation);
            
        } catch (Exception e) {
            log.error("❌ 工作流生成失败", e);
            return GenerateResult.fail("生成失败: " + e.getMessage());
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
        }
    }
    
    @Override
    public Long generateAndSave(String description, String workflowName) {
        GenerateResult result = generate(description);
        
        if (!result.isSuccess()) {
            throw new BusinessException(ErrorCode.WORKFLOW_EXECUTE_FAILED, result.getErrorMessage());
        }
        
        // 创建工作流实体
        Workflow workflow = Workflow.builder()
                .name(workflowName != null ? workflowName : result.getWorkflowName())
                .description(result.getWorkflowDescription())
                .graphData(result.getGraphData())
                .status("draft")
                .enabled(true)
                .version(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        
        workflow = workflowService.create(workflow);
        
        log.info("✅ 工作流已保存: id={}, name={}", workflow.getId(), workflow.getName());
        
        return workflow.getId();
    }
    
    @Override
    public GenerateResult optimize(Long workflowId, String instruction) {
        Workflow workflow = workflowService.getById(workflowId);
        if (workflow == null) {
            return GenerateResult.fail("工作流不存在");
        }
        
        String currentGraph = workflow.getGraphData();
        
        String systemPrompt = buildSystemPrompt();
        String userPrompt = String.format("""
            ## 当前工作流
            ```json
            %s
            ```
            
            ## 修改要求
            %s
            
            请根据修改要求，输出修改后的完整工作流JSON。
            """, currentGraph, instruction);
        
        try {
            String response = llmService.generate(systemPrompt + "\n\n" + userPrompt);
            String jsonStr = extractJson(response);
            
            if (jsonStr == null) {
                return GenerateResult.fail("无法从响应中提取有效的JSON");
            }
            
            Map<String, Object> workflowData = JsonUtils.fromJson(jsonStr, 
                    new TypeReference<Map<String, Object>>() {});
            workflowData = validateAndFix(workflowData);
            workflowData = autoLayout(workflowData);
            
            Map<String, Object> graphData = new HashMap<>();
            graphData.put("nodes", workflowData.get("nodes"));
            graphData.put("edges", workflowData.get("edges"));
            
            String graphJson = JsonUtils.toJson(graphData);
            String explanation = "已根据指令修改工作流: " + instruction;
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) workflowData.get("nodes");
            
            return GenerateResult.success(graphJson, workflow.getName(), 
                    workflow.getDescription(), nodes.size(), explanation);
            
        } catch (Exception e) {
            log.error("工作流优化失败", e);
            return GenerateResult.fail("优化失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建系统Prompt
     */
    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("""
            你是专业的工作流设计专家。根据用户的自然语言描述，生成完整的工作流JSON定义。
            
            ## 核心原则（必须遵守）
            1. **逻辑正确性第一**：工作流必须实现用户真正想要的功能
            2. **条件分支语义正确**：仔细思考条件为true/false时应该执行什么操作
            3. **变量传递正确**：确保每个节点使用正确的变量名
            4. **完整性**：流程必须完整，不能有断开的节点
            
            ## 常见场景的正确逻辑设计
            
            ### 🌐 翻译场景（重要）
            用户说"翻译"时，通常期望：中文↔其他语言 互译
            - 先用LLM检测语言，输出语言代码到变量（如detected_lang）
            - 条件判断：`{{detected_lang}} == 'zh'` 或 `{{detected_lang}} contains 'zh'`
            - 如果是中文(true) → 使用**LLM节点**翻译成英文（用户输入中文，想看英文结果）
            - 如果不是中文(false) → 使用**LLM节点**翻译成中文（用户输入外语，想看中文结果）
            
            **重要**：翻译任务请使用LLM节点，不要使用Agent节点（除非用户明确指定了智能体名称）
            **错误示例**：检测到中文却翻译成中文（没有意义）
            **正确逻辑**：输入什么语言，就翻译成另一种语言
            
            ### 💬 客服/问答场景
            - 先分类用户意图（咨询/投诉/建议/其他）
            - 根据分类结果分发到不同处理分支
            - 每个分支有针对性的回复模板或处理逻辑
            
            ### 📝 内容处理场景
            - 内容审核 → 通过(true) → 发布/继续处理
            - 内容审核 → 不通过(false) → 拦截/修改/人工审核
            
            ### 🔍 数据分析场景
            - 提取关键信息 → 验证数据完整性 → 处理/报错
            
            ### 📧 通知场景
            - 判断条件 → 满足条件 → 发送通知
            - 不满足条件 → 记录日志/跳过
            
            ## ⚠️ 常见错误（必须避免）
            1. 翻译工作流中：检测到中文却输出"翻译成中文"（逻辑反了）
            2. 条件分支连错：true分支和false分支接反了
            3. 变量名拼错：引用不存在的变量
            4. 流程断开：某些节点没有连接到结束节点
            5. 循环依赖：节点互相指向造成死循环
            6. **滥用Agent节点**：简单的LLM任务（如翻译、总结）应该用LLM节点，不要用Agent节点
            7. 使用不存在的资源ID：Agent/知识库ID必须从资源目录中选择
            
            ## 🎯 节点选择原则
            - **LLM节点**：用于AI生成、翻译、总结、分析等通用任务（首选）
            - **Agent节点**：仅当用户明确提到某个智能体名称时使用
            - **Knowledge节点**：仅当用户明确提到某个知识库名称时使用
            - **Tool节点**：需要调用外部工具时使用（如搜索、计算）
            
            ## 输出格式要求
            1. 必须输出合法的JSON格式
            2. 必须包含 name, description, nodes, edges 四个字段
            3. 必须有 start 和 end 节点
            4. 节点ID格式: start, end, node_1, node_2...
            5. 边ID格式: edge_1, edge_2...
            6. 条件分支的输出句柄: "true" 或 "false"
            
            ## 可用节点类型
            
            ### 1. start - 开始节点（必须）
            ```json
            {"id": "start", "type": "start", "name": "开始", "config": {}}
            ```
            
            ### 2. end - 结束节点（必须）
            ```json
            {"id": "end", "type": "end", "name": "结束", "config": {"outputVariable": "output"}}
            ```
            
            ### 3. llm - LLM调用节点
            用于AI生成、分析、翻译、总结等任务
            ```json
            {
              "id": "node_1",
              "type": "llm",
              "name": "AI处理",
              "config": {
                "systemPrompt": "你是一个专业的助手",
                "userPrompt": "请处理: {{input}}",
                "outputVariable": "llm_result",
                "temperature": 0.7,
                "maxTokens": 2000
              }
            }
            ```
            
            ### 4. condition - 条件分支节点
            根据条件决定流程走向，输出句柄为 "true" 或 "false"
            ```json
            {
              "id": "node_2",
              "type": "condition",
              "name": "条件判断",
              "config": {
                "expression": "{{score}} > 60"
              }
            }
            ```
            支持的表达式: ==, !=, >, <, >=, <=, contains, startsWith, endsWith, isEmpty, isNotEmpty
            
            ### 5. http - HTTP请求节点
            ```json
            {
              "id": "node_3",
              "type": "http",
              "name": "调用API",
              "config": {
                "url": "https://api.example.com/endpoint",
                "method": "POST",
                "headersJson": "{\\"Content-Type\\": \\"application/json\\"}",
                "body": "{\\"data\\": \\"{{input}}\\"}",
                "outputVariable": "api_response"
              }
            }
            ```
            
            ### 6. code - 代码执行节点
            执行JavaScript代码
            ```json
            {
              "id": "node_4",
              "type": "code",
              "name": "数据处理",
              "config": {
                "language": "javascript",
                "code": "return {{input}}.toUpperCase();",
                "outputVariable": "processed"
              }
            }
            ```
            
            ### 7. knowledge - 知识库检索节点
            从知识库中检索相关内容
            ```json
            {
              "id": "node_5",
              "type": "knowledge",
              "name": "知识检索",
              "config": {
                "knowledgeBaseId": 1,
                "query": "{{input}}",
                "topK": 5,
                "minScore": 0.5,
                "outputVariable": "knowledge_result",
                "outputFormat": "text"
              }
            }
            ```
            
            ### 8. agent - 智能体调用节点
            调用已配置的智能体
            ```json
            {
              "id": "node_6",
              "type": "agent",
              "name": "智能体处理",
              "config": {
                "agentId": 1,
                "userPrompt": "{{input}}",
                "outputVariable": "agent_result"
              }
            }
            ```
            
            ### 9. tool - 工具调用节点
            调用系统注册的工具
            ```json
            {
              "id": "node_7",
              "type": "tool",
              "name": "搜索",
              "config": {
                "toolName": "web_search",
                "paramsJson": "{\\"query\\": \\"{{input}}\\"}",
                "outputVariable": "search_result"
              }
            }
            ```
            
            ### 10. setvar - 变量设置节点
            ```json
            {
              "id": "node_8",
              "type": "setvar",
              "name": "设置变量",
              "config": {
                "variableName": "myVar",
                "value": "{{input}}",
                "outputVariable": "myVar"
              }
            }
            ```
            
            ### 11. template - 模板渲染节点
            ```json
            {
              "id": "node_9",
              "type": "template",
              "name": "生成回复",
              "config": {
                "template": "您好，您的查询结果是：{{result}}",
                "outputVariable": "final_output"
              }
            }
            ```
            
            ### 12. classifier - 分类器节点
            对输入进行分类，输出分类标签作为句柄
            ```json
            {
              "id": "node_10",
              "type": "classifier",
              "name": "意图分类",
              "config": {
                "categories": ["技术问题", "投诉建议", "咨询购买", "其他"],
                "inputVariable": "input",
                "outputVariable": "category"
              }
            }
            ```
            
            ### 13. loop - 循环节点
            ```json
            {
              "id": "node_11",
              "type": "loop",
              "name": "循环处理",
              "config": {
                "items": "{{list}}",
                "itemVariable": "item",
                "indexVariable": "index",
                "maxIterations": 100
              }
            }
            ```
            
            ### 14. parallel - 并行执行节点
            ```json
            {
              "id": "node_12",
              "type": "parallel",
              "name": "并行执行",
              "config": {
                "branches": 2
              }
            }
            ```
            
            ### 15. merge - 合并节点
            ```json
            {
              "id": "node_13",
              "type": "merge",
              "name": "合并结果",
              "config": {
                "waitBranches": 2,
                "mergeStrategy": "all",
                "outputVariable": "merged_result"
              }
            }
            ```
            
            ### 16. delay - 延迟节点
            ```json
            {
              "id": "node_14",
              "type": "delay",
              "name": "延迟",
              "config": {
                "delayMs": 1000
              }
            }
            ```
            
            ### 17. extractor - 信息提取节点
            从文本中提取结构化信息
            ```json
            {
              "id": "node_15",
              "type": "extractor",
              "name": "信息提取",
              "config": {
                "inputVariable": "input",
                "fields": [
                  {"name": "email", "description": "邮箱地址", "type": "string"},
                  {"name": "phone", "description": "电话号码", "type": "string"}
                ],
                "outputVariable": "extracted"
              }
            }
            ```
            
            ### 18. database - 数据库节点
            执行SQL查询或更新操作
            ```json
            {
              "id": "node_16",
              "type": "database",
              "name": "查询数据库",
              "config": {
                "operation": "query",
                "sql": "SELECT * FROM users WHERE name = '{{input}}'",
                "outputVariable": "db_result"
              }
            }
            ```
            operation可选值: query(查询), update(更新), insert(插入), delete(删除)
            
            ### 19. email - 邮件发送节点
            发送邮件通知
            ```json
            {
              "id": "node_17",
              "type": "email",
              "name": "发送邮件",
              "config": {
                "to": "user@example.com",
                "subject": "通知：{{title}}",
                "content": "{{message}}",
                "isHtml": false,
                "outputVariable": "email_result"
              }
            }
            ```
            isHtml: true发送HTML邮件，false发送纯文本
            
            ### 20. cache - 缓存节点
            缓存数据读写操作
            ```json
            {
              "id": "node_18",
              "type": "cache",
              "name": "写入缓存",
              "config": {
                "operation": "set",
                "key": "user_{{userId}}",
                "value": "{{data}}",
                "ttl": 3600,
                "outputVariable": "cache_result"
              }
            }
            ```
            operation可选值: get(读取), set(写入), delete(删除), exists(检查是否存在)
            ttl: 缓存过期时间(秒)，默认3600
            
            ### 21. webhook - Webhook通知节点
            发送Webhook通知到外部系统
            ```json
            {
              "id": "node_19",
              "type": "webhook",
              "name": "发送通知",
              "config": {
                "url": "https://webhook.example.com/notify",
                "eventType": "workflow.completed",
                "dataJson": "{\\"message\\": \\"{{result}}\\"}",
                "maxRetries": 3,
                "timeout": 30,
                "outputVariable": "webhook_result"
              }
            }
            ```
            eventType: 事件类型，默认workflow.completed
            maxRetries: 最大重试次数，默认3
            timeout: 超时时间(秒)，默认30
            
            ### 22. iterator - 迭代器节点
            遍历数组数据
            ```json
            {
              "id": "node_20",
              "type": "iterator",
              "name": "遍历数据",
              "config": {
                "inputVariable": "items",
                "itemVariable": "item",
                "maxIterations": 100,
                "outputVariable": "iterator_results"
              }
            }
            ```
            
            ### 23. aggregator - 聚合节点
            聚合多个变量的结果
            ```json
            {
              "id": "node_21",
              "type": "aggregator",
              "name": "聚合结果",
              "config": {
                "variables": ["result1", "result2"],
                "mode": "array",
                "outputVariable": "aggregated_result"
              }
            }
            ```
            mode可选值: array(数组), concat(拼接), first(取第一个), last(取最后一个)
            
            ### 24. subflow - 子工作流节点
            调用其他工作流
            ```json
            {
              "id": "node_22",
              "type": "subflow",
              "name": "调用子流程",
              "config": {
                "workflowId": 1,
                "inputVariable": "input",
                "outputVariable": "subflow_result"
              }
            }
            ```
            
            ### 25. question - 问答节点
            基于上下文回答问题
            ```json
            {
              "id": "node_23",
              "type": "question",
              "name": "智能问答",
              "config": {
                "mode": "answer",
                "contextVariable": "knowledge_result",
                "questionVariable": "input",
                "outputVariable": "qa_result"
              }
            }
            ```
            mode可选值: answer(回答问题), generate(生成问题)
            
            ### 26. text - 文本处理节点
            文本拼接、格式化等操作
            ```json
            {
              "id": "node_24",
              "type": "text",
              "name": "文本处理",
              "config": {
                "operation": "concat",
                "variables": ["var1", "var2"],
                "separator": "\\n",
                "outputVariable": "text_result"
              }
            }
            ```
            operation可选值: concat(拼接), format(格式化)
            
            ### 27. while - 条件循环节点
            当条件满足时重复执行
            ```json
            {
              "id": "node_25",
              "type": "while",
              "name": "条件循环",
              "config": {
                "condition": "{{counter}} < 10",
                "maxIterations": 100
              }
            }
            ```
            
            ## 边(Edge)定义
            ```json
            {
              "id": "edge_1",
              "source": "start",
              "target": "node_1",
              "sourceHandle": null,
              "targetHandle": null
            }
            ```
            
            条件分支的边需要指定sourceHandle:
            ```json
            {
              "id": "edge_2",
              "source": "node_2",
              "target": "node_3",
              "sourceHandle": "true",
              "label": "是"
            },
            {
              "id": "edge_3",
              "source": "node_2",
              "target": "node_4",
              "sourceHandle": "false",
              "label": "否"
            }
            ```
            
            ## 变量引用
            使用 {{变量名}} 格式引用变量:
            - {{input}} - 工作流输入
            - {{node_1_output}} - 某节点的输出
            - 自定义变量名
            
            """);
        
        // 添加系统资源目录
        sb.append(buildResourceCatalog());
        
        sb.append("""
            
            ## 完整示例：翻译工作流
            ```json
            {
              "name": "智能翻译助手",
              "description": "自动识别语言并进行中英互译",
              "nodes": [
                {"id": "start", "type": "start", "name": "开始", "config": {}},
                {"id": "node_1", "type": "llm", "name": "语言检测", "config": {
                  "systemPrompt": "你是语言检测专家，只输出语言代码，中文输出zh，英文输出en，其他语言输出other",
                  "userPrompt": "检测这段文字的语言：{{input}}",
                  "outputVariable": "detected_lang"
                }},
                {"id": "node_2", "type": "condition", "name": "是否中文", "config": {
                  "expression": "{{detected_lang}} == 'zh'"
                }},
                {"id": "node_3", "type": "llm", "name": "翻译成英文", "config": {
                  "systemPrompt": "你是专业翻译，将中文翻译成地道的英文",
                  "userPrompt": "请翻译：{{input}}",
                  "outputVariable": "translation"
                }},
                {"id": "node_4", "type": "llm", "name": "翻译成中文", "config": {
                  "systemPrompt": "你是专业翻译，将外语翻译成流畅的中文",
                  "userPrompt": "请翻译：{{input}}",
                  "outputVariable": "translation"
                }},
                {"id": "node_5", "type": "template", "name": "输出结果", "config": {
                  "template": "原文：{{input}}\\n译文：{{translation}}",
                  "outputVariable": "output"
                }},
                {"id": "end", "type": "end", "name": "结束", "config": {"outputVariable": "output"}}
              ],
              "edges": [
                {"id": "edge_1", "source": "start", "target": "node_1"},
                {"id": "edge_2", "source": "node_1", "target": "node_2"},
                {"id": "edge_3", "source": "node_2", "target": "node_3", "sourceHandle": "true", "label": "中文→英文"},
                {"id": "edge_4", "source": "node_2", "target": "node_4", "sourceHandle": "false", "label": "外语→中文"},
                {"id": "edge_5", "source": "node_3", "target": "node_5"},
                {"id": "edge_6", "source": "node_4", "target": "node_5"},
                {"id": "edge_7", "source": "node_5", "target": "end"}
              ]
            }
            ```
            注意：上面示例中，检测到中文(true)时执行"翻译成英文"，这是正确的逻辑！
            
            ## 设计思路
            1. 先理解用户真正想要实现什么功能
            2. 设计合理的处理流程
            3. 确保条件分支的语义正确（true/false分别对应什么操作）
            4. 检查变量传递是否正确
            5. 确保所有分支最终都能到达结束节点
            
            只输出JSON，不要其他解释。确保逻辑100%正确！
            """);
        
        return sb.toString();
    }
    
    /**
     * 构建资源目录（带缓存）
     * 
     * <p>资源目录包含系统中可用的智能体、知识库和工具列表</p>
     * <p>为了提升性能，结果会缓存5分钟，避免频繁查询数据库</p>
     * 
     * @return 格式化的资源目录文本
     */
    private String buildResourceCatalog() {
        // 检查缓存是否有效
        long now = System.currentTimeMillis();
        if (resourceCatalogCache != null && (now - resourceCacheTime) < CACHE_TTL_MS) {
            log.debug("✅ 使用缓存的资源目录");
            return resourceCatalogCache;
        }
        
        log.debug("🔄 重新构建资源目录");
        StringBuilder sb = new StringBuilder();
        sb.append("\n## 📋 系统资源目录（请根据名称匹配ID）\n\n");
        
        // 1. 智能体列表
        sb.append("### 可用的智能体\n");
        sb.append("| ID | 名称 | 描述 |\n");
        sb.append("|----|------|------|\n");
        try {
            List<Agent> agents = agentService.list();
            if (agents != null && !agents.isEmpty()) {
                // 使用Stream API优化，只取前20个
                agents.stream()
                        .limit(20)
                        .forEach(agent -> sb.append(String.format("| %d | %s | %s |\n", 
                                agent.getId(), 
                                agent.getName(),
                                truncateDesc(agent.getDescription(), 50))));
            } else {
                sb.append("| - | 暂无智能体 | - |\n");
            }
        } catch (Exception e) {
            log.warn("⚠️  获取智能体列表失败: {}", e.getMessage());
            sb.append("| - | 获取失败 | - |\n");
        }
        
        // 2. 知识库列表
        sb.append("\n### 可用的知识库\n");
        sb.append("| ID | 文件名 | 类别 |\n");
        sb.append("|----|--------|------|\n");
        try {
            List<KnowledgeBase> kbs = knowledgeBaseService.list();
            if (kbs != null && !kbs.isEmpty()) {
                // 只取前20个，避免prompt过长
                kbs.stream()
                        .limit(20)
                        .forEach(kb -> sb.append(String.format("| %d | %s | %s |\n", 
                                kb.getId(), 
                                kb.getFileName() != null ? kb.getFileName() : "未命名",
                                kb.getCategory() != null ? kb.getCategory() : "默认")));
            } else {
                sb.append("| - | 暂无知识库 | - |\n");
            }
        } catch (Exception e) {
            log.warn("⚠️  获取知识库列表失败: {}", e.getMessage());
            sb.append("| - | 获取失败 | - |\n");
        }
        
        // 3. 系统工具列表（硬编码，因为工具相对固定）
        sb.append("\n### 可用的工具\n");
        sb.append("| 工具名 | 功能说明 |\n");
        sb.append("|--------|----------|\n");
        sb.append("| web_search | 搜索网页内容 |\n");
        sb.append("| calculator | 数学计算 |\n");
        sb.append("| current_time | 获取当前时间 |\n");
        sb.append("| weather | 查询天气信息 |\n");
        
        // 更新缓存
        resourceCatalogCache = sb.toString();
        resourceCacheTime = now;
        
        return resourceCatalogCache;
    }
    
    /**
     * 截断描述文本
     * 
     * @param desc 原始描述
     * @param maxLen 最大长度
     * @return 截断后的描述
     */
    private String truncateDesc(String desc, int maxLen) {
        if (desc == null || desc.isEmpty()) {
            return "-";
        }
        if (desc.length() <= maxLen) {
            return desc;
        }
        return desc.substring(0, maxLen) + "...";
    }
    
    /**
     * 构建用户Prompt
     */
    private String buildUserPrompt(String description) {
        return String.format("""
            ## 用户需求
            %s
            
            ## 设计要求
            请根据以上需求生成完整的工作流JSON。
            
            **在生成之前，请先思考：**
            1. 用户真正想要实现什么功能？
            2. 需要哪些处理步骤？
            3. 是否需要条件分支？如果需要，true和false分别应该做什么？
            4. 变量如何在节点之间传递？
            
            **生成时请确保：**
            1. **优先使用LLM节点**：翻译、总结、分析等任务直接用LLM节点，不要用Agent节点
            2. 只有用户明确提到智能体或知识库名称时，才从资源目录中匹配ID使用Agent/Knowledge节点
            3. 合理设计节点之间的数据流转（使用变量引用）
            4. 条件分支的逻辑必须正确（例如：翻译场景中检测到中文应该翻译成英文）
            5. 所有分支最终都要连接到结束节点
            6. LLM节点的提示词要清晰明确，确保输出格式符合后续节点的需要
            7. 不要使用不存在的资源ID（Agent/知识库ID必须从资源目录中选择）
            
            直接输出JSON，不要解释。
            """, description);
    }
    
    /**
     * 从响应中提取JSON
     */
    private String extractJson(String response) {
        // 尝试提取 ```json ... ``` 代码块
        int jsonStart = response.indexOf("```json");
        if (jsonStart >= 0) {
            int contentStart = response.indexOf("\n", jsonStart) + 1;
            int jsonEnd = response.indexOf("```", contentStart);
            if (jsonEnd > contentStart) {
                return response.substring(contentStart, jsonEnd).trim();
            }
        }
        
        // 尝试提取 ``` ... ```
        jsonStart = response.indexOf("```");
        if (jsonStart >= 0) {
            int contentStart = response.indexOf("\n", jsonStart) + 1;
            int jsonEnd = response.indexOf("```", contentStart);
            if (jsonEnd > contentStart) {
                return response.substring(contentStart, jsonEnd).trim();
            }
        }
        
        // 尝试直接解析（响应可能就是纯JSON）
        String trimmed = response.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        
        // 尝试找第一个 { 和最后一个 }
        int firstBrace = response.indexOf("{");
        int lastBrace = response.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return response.substring(firstBrace, lastBrace + 1);
        }
        
        return null;
    }
    
    /**
     * 校验和修复工作流定义
     * 
     * <p>主要功能：</p>
     * <ul>
     *     <li>确保存在start和end节点</li>
     *     <li>为每个节点添加config配置</li>
     *     <li>为LLM节点设置默认模型</li>
     *     <li>移除无效的边（引用不存在的节点）</li>
     *     <li>自动连接孤立的start节点</li>
     * </ul>
     * 
     * @param data 原始工作流数据
     * @return 修复后的工作流数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> validateAndFix(Map<String, Object> data) {
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) data.get("edges");
        
        // 初始化节点和边列表
        if (nodes == null) {
            nodes = new ArrayList<>();
            data.put("nodes", nodes);
        }
        if (edges == null) {
            edges = new ArrayList<>();
            data.put("edges", edges);
        }
        
        // 检查是否有start节点
        boolean hasStart = nodes.stream().anyMatch(n -> "start".equals(n.get("type")));
        if (!hasStart) {
            Map<String, Object> startNode = new HashMap<>();
            startNode.put("id", "start");
            startNode.put("type", "start");
            startNode.put("name", "开始");
            startNode.put("config", new HashMap<>());
            nodes.add(0, startNode);
            log.info("🔧 自动添加start节点");
        }
        
        // 检查是否有end节点
        boolean hasEnd = nodes.stream().anyMatch(n -> "end".equals(n.get("type")));
        if (!hasEnd) {
            Map<String, Object> endNode = new HashMap<>();
            endNode.put("id", "end");
            endNode.put("type", "end");
            endNode.put("name", "结束");
            Map<String, Object> endConfig = new HashMap<>();
            endConfig.put("outputVariable", "output");
            endNode.put("config", endConfig);
            nodes.add(endNode);
            log.info("🔧 自动添加end节点");
        }
        
        // 确保每个节点都有config，并为LLM节点设置默认模型
        for (Map<String, Object> node : nodes) {
            if (node.get("config") == null) {
                node.put("config", new HashMap<>());
            }
            
            // 为LLM节点设置默认模型
            String nodeType = (String) node.get("type");
            if ("llm".equals(nodeType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = (Map<String, Object>) node.get("config");
                if (config.get("model") == null || "".equals(config.get("model"))) {
                    // 获取默认聊天模型
                    try {
                        ModelConfig defaultModel = modelConfigService.getDefaultChatConfig();
                        String modelName = defaultModel != null ? defaultModel.getModelName() : "gpt-4o-mini";
                        config.put("model", modelName);
                        log.info("🔧 为LLM节点{}设置默认模型: {}", node.get("id"), modelName);
                    } catch (Exception e) {
                        config.put("model", "gpt-4o-mini");
                        log.warn("获取默认模型失败，使用备用模型: gpt-4o-mini");
                    }
                }
                // 确保有systemPrompt
                if (config.get("systemPrompt") == null) {
                    config.put("systemPrompt", "你是一个有帮助的AI助手。");
                }
                // 确保有temperature
                if (config.get("temperature") == null) {
                    config.put("temperature", 0.7);
                }
            }
        }
        
        // 确保边的完整性
        Set<String> nodeIds = new HashSet<>();
        for (Map<String, Object> node : nodes) {
            nodeIds.add((String) node.get("id"));
        }
        
        // 移除引用不存在节点的边
        edges.removeIf(edge -> {
            String source = (String) edge.get("source");
            String target = (String) edge.get("target");
            return !nodeIds.contains(source) || !nodeIds.contains(target);
        });
        
        // 确保start节点有出边
        boolean startHasOutEdge = edges.stream()
                .anyMatch(e -> "start".equals(e.get("source")));
        if (!startHasOutEdge && nodes.size() > 2) {
            // 找第一个非start、非end的节点
            for (Map<String, Object> node : nodes) {
                String id = (String) node.get("id");
                if (!"start".equals(id) && !"end".equals(id)) {
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("id", "edge_auto_start");
                    edge.put("source", "start");
                    edge.put("target", id);
                    edges.add(0, edge);
                    log.info("🔧 自动添加start到{}的边", id);
                    break;
                }
            }
        }
        
        return data;
    }
    
    /**
     * 自动布局节点位置
     * 
     * <p>使用拓扑排序算法对节点进行分层，然后计算每个节点的坐标位置</p>
     * 
     * <p>算法流程：</p>
     * <ol>
     *     <li>构建图的邻接表和入度表</li>
     *     <li>使用BFS进行拓扑排序分层</li>
     *     <li>根据层级计算节点的X坐标</li>
     *     <li>在每层内均匀分布节点的Y坐标</li>
     *     <li>处理孤立节点和环形结构</li>
     * </ol>
     * 
     * @param data 工作流数据
     * @return 添加了位置信息的工作流数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> autoLayout(Map<String, Object> data) {
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) data.get("edges");
        
        if (nodes == null || nodes.isEmpty()) {
            log.warn("⚠️  节点列表为空，跳过布局");
            return data;
        }
        
        log.debug("📐 开始自动布局，节点数: {}, 边数: {}", nodes.size(), edges != null ? edges.size() : 0);
        
        // 构建邻接表
        Map<String, List<String>> adjacency = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        
        for (Map<String, Object> node : nodes) {
            String id = (String) node.get("id");
            adjacency.put(id, new ArrayList<>());
            inDegree.put(id, 0);
        }
        
        for (Map<String, Object> edge : edges) {
            String source = (String) edge.get("source");
            String target = (String) edge.get("target");
            if (adjacency.containsKey(source)) {
                adjacency.get(source).add(target);
            }
            inDegree.merge(target, 1, Integer::sum);
        }
        
        // 拓扑排序分层
        List<List<String>> layers = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        
        // 从入度为0的节点开始（通常是start）
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        while (!queue.isEmpty()) {
            List<String> layer = new ArrayList<>();
            int size = queue.size();
            
            for (int i = 0; i < size; i++) {
                String nodeId = queue.poll();
                if (visited.contains(nodeId)) continue;
                
                visited.add(nodeId);
                layer.add(nodeId);
                
                for (String next : adjacency.getOrDefault(nodeId, Collections.emptyList())) {
                    int newDegree = inDegree.get(next) - 1;
                    inDegree.put(next, newDegree);
                    if (newDegree == 0 && !visited.contains(next)) {
                        queue.offer(next);
                    }
                }
            }
            
            if (!layer.isEmpty()) {
                layers.add(layer);
            }
        }
        
        // 处理未访问的节点（可能是孤立节点或环）
        for (Map<String, Object> node : nodes) {
            String id = (String) node.get("id");
            if (!visited.contains(id)) {
                List<String> lastLayer = layers.isEmpty() ? new ArrayList<>() : layers.get(layers.size() - 1);
                lastLayer.add(id);
                if (layers.isEmpty()) {
                    layers.add(lastLayer);
                }
            }
        }
        
        // 根据层级设置位置
        Map<String, double[]> positions = new HashMap<>();
        
        for (int layerIdx = 0; layerIdx < layers.size(); layerIdx++) {
            List<String> layer = layers.get(layerIdx);
            int x = START_X + layerIdx * NODE_SPACING_X;
            
            int totalHeight = (layer.size() - 1) * NODE_SPACING_Y;
            int startY = START_Y - totalHeight / 2;
            
            for (int nodeIdx = 0; nodeIdx < layer.size(); nodeIdx++) {
                String nodeId = layer.get(nodeIdx);
                int y = startY + nodeIdx * NODE_SPACING_Y;
                positions.put(nodeId, new double[]{x, y});
            }
        }
        
        // 应用位置到节点
        for (Map<String, Object> node : nodes) {
            String id = (String) node.get("id");
            double[] pos = positions.get(id);
            if (pos != null) {
                node.put("positionX", pos[0]);
                node.put("positionY", pos[1]);
            }
        }
        
        return data;
    }
    
    /**
     * 生成工作流说明
     */
    @SuppressWarnings("unchecked")
    private String generateExplanation(Map<String, Object> data) {
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
        
        StringBuilder sb = new StringBuilder();
        sb.append("## 工作流说明\n\n");
        sb.append("### 包含的节点\n");
        
        int index = 1;
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            String name = (String) node.get("name");
            
            if (!"start".equals(type) && !"end".equals(type)) {
                sb.append(String.format("%d. **%s** (%s)\n", index++, name, getTypeDisplayName(type)));
            }
        }
        
        // 统计节点类型
        Map<String, Long> typeCounts = new HashMap<>();
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            typeCounts.merge(type, 1L, Long::sum);
        }
        
        sb.append("\n### 节点统计\n");
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            if (!"start".equals(entry.getKey()) && !"end".equals(entry.getKey())) {
                sb.append(String.format("- %s: %d 个\n", getTypeDisplayName(entry.getKey()), entry.getValue()));
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 获取节点类型显示名称
     */
    private String getTypeDisplayName(String type) {
        return switch (type) {
            case "llm" -> "LLM调用";
            case "condition" -> "条件分支";
            case "http" -> "HTTP请求";
            case "code" -> "代码执行";
            case "knowledge" -> "知识库检索";
            case "agent" -> "智能体";
            case "tool" -> "工具调用";
            case "setVariable" -> "设置变量";
            case "template" -> "模板渲染";
            case "classifier" -> "分类器";
            case "extractor" -> "信息提取";
            case "loop" -> "循环";
            case "parallel" -> "并行执行";
            case "merge" -> "合并";
            case "delay" -> "延迟";
            default -> type;
        };
    }
}
