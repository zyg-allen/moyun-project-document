# AI 能力架构完整设计文档_v4

> **文档版本**：v1.0
> **基线版本**：v11.90（2026-09-16）
> **文档定位**：整合 Agent 表 / 场景表 / Chat 接口三套配置体系，覆盖上下文绑定、消息模型、工具调用、多模态、结构化输出，面向后续记账模块（账单截图识别）等扩展
> **用途**：AI 能力架构的完整设计基线，存档

---

# 第一部分 · 背景与问题

## 1.1 现状：三套配置体系并存

| #   | 体系      | 位置                          | 职责                          |
| --- | ------- | --------------------------- | --------------------------- |
| 1   | 场景配置表   | `ai_scene_config`           | 场景 → Agent/模型/知识库/工具/工作流 绑定 |
| 2   | Agent 表 | `ai_agent`                  | Agent → 模型/系统提示词/工具/知识库     |
| 3   | Chat 接口 | `/ai/chat/index?agentId=48` | 直接选 Agent 聊天                |

**问题**：

- 职责重叠（提示词、模型、知识库、工具都配了两处）
- 提示词来源不唯一
- Chat 接口绕过治理
- 难调试

## 1.2 核心矛盾

| 矛盾        | 说明                  |
| --------- | ------------------- |
| 提示词配在哪    | 场景表配了，但实际用 Agent 的  |
| 绑定关系重复    | 场景和 Agent 都能绑模型/知识库 |
| Chat 绕过场景 | 无限流/缓存/降级/日志        |

## 1.3 扩展需求

| 需求    | 场景             |
| ----- | -------------- |
| 工具调用  | 记账查询、题库检索、订单查询 |
| 多模态   | 账单截图识别、简历图片解析  |
| 结构化输出 | 面试计划、评分结果、账单解析 |

---

# 第二部分 · 核心设计原则

## 2.1 三层分离

```
┌─────────────────────────────────────────────┐
│  场景层（ai_scene_config）                   │
│  = 业务入口 + 治理                           │
│  ├── scene_code：业务声明                    │
│  ├── agent_id：绑定 Agent                    │
│  ├── config_json：场景配置（task/mode）      │
│  ├── rate_limit：限流                        │
│  ├── fallback：降级                          │
│  ├── output_schema：输出结构                 │
│  └── version/weight：灰度                    │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  Agent 层（ai_agent）                        │
│  = 能力容器 + 提示词                         │
│  ├── system_prompt：提示词（唯一来源）       │
│  ├── welcome_message：开场白                 │
│  ├── model_id：模型                          │
│  ├── tools：工具                             │
│  ├── knowledge_base_ids：知识库              │
│  └── workflow_id：工作流                     │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  底座层                                      │
│  ├── 模型配置（ai_model_config）             │
│  ├── 知识库（ai_knowledge_library）          │
│  ├── 工具（ai_tool）                         │
│  └── 工作流（ai_workflow）                   │
└─────────────────────────────────────────────┘
```

## 2.2 职责边界

| 内容         | 归属          | 理由       |
| ---------- | ----------- | -------- |
| 场景代码       | 场景表         | 业务声明     |
| 绑定哪个 Agent | 场景表         | 业务路由     |
| 限流/降级/缓存   | 场景表         | 治理       |
| 输出结构约束     | 场景表         | 场景相关     |
| 灰度/版本      | 场景表         | 发布相关     |
| **系统提示词**  | **Agent 表** | **能力定义** |
| **模型**     | **Agent 表** | **能力定义** |
| **工具**     | **Agent 表** | **能力定义** |
| **知识库**    | **Agent 表** | **能力定义** |
| **工作流**    | **Agent 表** | **能力定义** |

**关键**：场景表**不配提示词、模型、工具、知识库**（只配 agent_id）。

## 2.3 调用链

```
业务调用
    ↓
aiGatewayService.execute(sceneCode, input)
    ↓
场景表：查 agent_id + 治理配置
    ↓
Agent 表：查 system_prompt + 能力
    ↓
组装 ChatMessage 列表
    ↓
AI 底座：调模型/知识库/工具
    ↓
返回结果
```

---

# 第三部分 · 消息模型（ChatMessage）

## 3.1 核心概念

**大模型接收的不是"一段话"，而是"消息列表"。**

```java
List<ChatMessage> messages = List.of(
    SystemMessage.from("你是资深技术面试官..."),  // 系统提示词
    UserMessage.from("候选人简历：..."),           // 用户消息
    AiMessage.from("好的，我了解了"),              // AI 历史回复
    UserMessage.from("请生成面试计划")             // 当前任务
);
```

## 3.2 消息类型

| 类型                           | 角色        | 用途                  |
| ---------------------------- | --------- | ------------------- |
| `SystemMessage`              | system    | 系统提示词（只能 1 条，在最前）   |
| `UserMessage`                | user      | 用户输入 / 业务上下文 / 任务指令 |
| `AiMessage`                  | assistant | AI 历史回复             |
| `ToolExecutionResultMessage` | tool      | 工具执行结果              |

## 3.3 消息顺序规则

```
System → User → Ai → User → Ai → ... → User
```

**规则**：

- SystemMessage 只能 1 条，必须在第一条
- User 和 Ai 交替（部分模型不接受连续 User）
- 最后一条通常是 User（当前任务）

## 3.4 上下文组装

**三层结构**：

```
┌─────────────────────────────────────┐
│  第 1 层：Agent 人设（系统提示词）    │  ← Agent 表
│  "你是资深技术面试官..."              │     稳定不变
├─────────────────────────────────────┤
│  第 2 层：业务上下文（简历/JD）       │  ← 业务数据
│  "候选人简历：..."                    │     每次会话不同
├─────────────────────────────────────┤
│  第 3 层：任务指令（当前要做什么）     │  ← 场景/代码
│  "请生成面试计划..."                  │     每轮不同
└─────────────────────────────────────┘
```

**组装方式**：

```java
List<ChatMessage> messages = List.of(
    // 第 1 层：Agent 表
    SystemMessage.from(agent.getSystemPrompt()),

    // 第 2 层 + 第 3 层：业务数据 + 任务
    UserMessage.from(
        "【候选人简历】\n" + resumeDigest +
        "\n\n【岗位要求】\n" + jobContext +
        "\n\n【参考知识】\n" + knowledge +
        "\n\n【任务】\n" + taskPrompt
    )
);
```

## 3.5 上下文长度控制

| 部分            | 策略                      |
| ------------- | ----------------------- |
| SystemMessage | 固定（短）                   |
| 简历            | 只发 ResumeDigest（≤500 字） |
| JD            | 只发 JobContext（≤300 字）   |
| 知识库           | 只发 Top 3（≤1000 字）       |
| 历史对话          | 只保留最近 N 轮               |
| 更早历史          | 摘要成一条                   |
| AI 理解         | 完整发（核心）                 |

**总控制**：单轮 ≤ 3000 tokens。

---

# 第四部分 · 工具调用（Function Calling）

## 4.1 设计目标

让 AI 能"自主调用"业务能力：

- 记账查询（"我上个月花了多少"）
- 题库检索（"找几道链表题"）
- 订单查询（"我的订单到哪了"）

## 4.2 工具定义

**工具表（ai_tool）**：

```sql
CREATE TABLE ai_tool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_code VARCHAR(50) NOT NULL COMMENT '工具代码',
    tool_name VARCHAR(100) COMMENT '工具名称',
    description TEXT COMMENT '工具描述（给 LLM 看）',
    parameters_schema JSON COMMENT '参数 JSON Schema',
    executor_bean VARCHAR(100) COMMENT '执行器 Bean',
    enabled TINYINT DEFAULT 1,
    del_flag CHAR(1) DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tool_code (tool_code)
) COMMENT 'AI 工具定义';
```

**工具定义示例（记账查询）**：

```json
{
  "tool_code": "ledger_query",
  "tool_name": "记账查询",
  "description": "查询用户的记账数据，支持按时间、分类、类型筛选",
  "parameters_schema": {
    "type": "object",
    "properties": {
      "userId": {"type": "integer", "description": "用户ID"},
      "startDate": {"type": "string", "format": "date", "description": "开始日期"},
      "endDate": {"type": "string", "format": "date", "description": "结束日期"},
      "category": {"type": "string", "description": "分类（可选）"},
      "type": {"type": "string", "enum": ["expense", "income"], "description": "类型"}
    },
    "required": ["userId", "startDate", "endDate"]
  },
  "executor_bean": "ledgerQueryTool"
}
```

## 4.3 工具执行器

**接口**：

```java
public interface AiToolExecutor {
    String getToolCode();
    Object execute(Map<String, Object> params);
}
```

**实现**：

```java
@Component
public class LedgerQueryTool implements AiToolExecutor {

    @Autowired private ILedgerTransactionService txnService;

    @Override
    public String getToolCode() {
        return "ledger_query";
    }

    @Override
    public Object execute(Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        // ... 查询
        return result;
    }
}
```

## 4.4 工具调用流程

```
用户提问："我上个月花了多少"
    ↓
组装消息列表：
    SystemMessage(Agent 提示词)
    UserMessage(用户提问)
    + tools 参数（可用工具列表）
    ↓
调大模型
    ↓
大模型返回：tool_call(ledger_query, {startDate, endDate})
    ↓
执行工具
    ↓
把工具结果加入消息列表：
    ToolExecutionResultMessage(result)
    ↓
再次调大模型
    ↓
大模型返回：最终答案
```

**关键**：工具调用是**多轮消息交互**，不是一次调用。

## 4.5 LangChain4j 实现

```java
// 方式 1：注解式（推荐）
@Component
public class LedgerTools {

    @Tool("查询记账数据")
    public String queryLedger(
        @P("用户ID") Long userId,
        @P("开始日期") String startDate,
        @P("结束日期") String endDate
    ) {
        // 查询逻辑
        return result;
    }
}

// 方式 2：编程式
ToolSpecification spec = ToolSpecification.builder()
    .name("ledger_query")
    .description("查询记账数据")
    .parameters(JsonObjectSchema.builder()
        .addIntegerProperty("userId", "用户ID")
        .addStringProperty("startDate", "开始日期")
        .build())
    .build();

// 调用
ChatResponse response = model.generate(messages, List.of(spec));
```

## 4.6 多模态 + 工具调用

**当需要同时支持**：

```java
List<ChatMessage> messages = List.of(
    SystemMessage.from(agent.getSystemPrompt()),
    UserMessage.from(
        TextContent.from("请识别这张账单截图并解析"),
        ImageContent.from(imageUrl)
    )
);

List<ToolSpecification> tools = List.of(ledgerParseTool);

ChatResponse response = model.generate(messages, tools);
```

---

# 第五部分 · 多模态（图片/音频）

## 5.1 设计目标

支持图片/音频输入：

- 账单截图 → 自动解析 → 反显到记账表单
- 简历图片 → 解析 → 填充表单
- 语音 → ASR → 文本

## 5.2 多模态消息

**LangChain4j 的多模态消息**：

```java
// 图片 + 文本
UserMessage.from(
    TextContent.from("请识别这张账单截图，提取金额、商户、时间、分类"),
    ImageContent.from(imageUrl)  // 或 base64
)

// 音频 + 文本
UserMessage.from(
    TextContent.from("请转写这段语音"),
    AudioContent.from(audioUrl)
)
```

## 5.3 账单截图识别场景

**完整流程**：

```
用户上传账单截图
    ↓
【前端】预览 + 上传
    ↓
【后端】接收图片
    ↓
【AI 调用】多模态识别：
    SystemMessage(账单解析 Agent 提示词)
    UserMessage(
        TextContent("请识别这张账单截图，提取：金额、商户、时间、分类，输出 JSON"),
        ImageContent(imageUrl)
    )
    + response_format(JSON Schema)
    ↓
【AI 返回】结构化 JSON：
    {
      "amount": 128.50,
      "merchant": "星巴克",
      "date": "2026-09-16",
      "category": "餐饮",
      "type": "expense"
    }
    ↓
【前端】反显到记账表单
    ↓
用户确认/修改
    ↓
【后端】保存
```

## 5.4 多模态模型配置

**模型配置表**：

```sql
-- ai_model_config 新增多模态支持
ALTER TABLE ai_model_config
  ADD COLUMN supports_vision TINYINT DEFAULT 0 COMMENT '是否支持图片',
  ADD COLUMN supports_audio TINYINT DEFAULT 0 COMMENT '是否支持音频',
  ADD COLUMN multimodal_config JSON COMMENT '多模态配置';
```

**模型配置示例**：

```json
{
  "model_name": "qwen-vl-max",
  "model_type": "multimodal",
  "provider": "dashscope",
  "supports_vision": 1,
  "supports_audio": 0,
  "model_code": "qwen-vl-max"
}
```

## 5.5 场景注册

**新增场景**：

```
scene_code = "ledger_bill_parse"
scene_name = "账单截图解析"
agent_id = 账单解析 Agent
config_json = {
    "task": "bill_parse",
    "response_format": "json_schema",
    "output_schema": {
        "type": "object",
        "properties": {
            "amount": {"type": "number"},
            "merchant": {"type": "string"},
            "date": {"type": "string"},
            "category": {"type": "string"},
            "type": {"type": "string", "enum": ["expense", "income"]}
        }
    }
}
```

---

# 第六部分 · 结构化输出（强约束）

## 6.1 设计目标

让 AI 输出的**格式可控**，便于程序解析：

- 面试计划（JSON）
- 评分结果（JSON）
- 账单解析（JSON）
- 任何需要程序处理的输出

## 6.2 三种实现方式

| 方式              | 说明                | 可靠性   |
| --------------- | ----------------- | ----- |
| **Prompt 约束**   | 在提示词里要求"输出 JSON"  | ★★    |
| **JSON Mode**   | API 参数强制 JSON     | ★★★★  |
| **JSON Schema** | API 参数强制符合 Schema | ★★★★★ |

## 6.3 场景表配置

**在场景表配置输出结构**：

```json
{
  "scene_code": "voice_interview",
  "config_json": {
    "task": "warmup",
    "response_format": "json_schema",
    "output_schema": {
      "type": "object",
      "properties": {
        "understanding": {
          "type": "object",
          "properties": {
            "candidateProfile": {"type": "string"},
            "strengths": {"type": "array", "items": {"type": "string"}},
            "concerns": {"type": "array", "items": {"type": "string"}}
          }
        },
        "interviewPlan": {"type": "object"},
        "opening": {"type": "string"},
        "firstQuestion": {"type": "object"}
      },
      "required": ["understanding", "interviewPlan", "opening"]
    }
  }
}
```

## 6.4 实现

**LangChain4j 方式**：

```java
// 方式 1：AiServices 声明式
interface InterviewPlanner {
    @UserMessage("生成面试计划")
    InterviewPlan plan(@V("resume") String resume, @V("job") String job);
}

InterviewPlanner planner = AiServices.builder(InterviewPlanner.class)
    .chatLanguageModel(model)
    .build();

InterviewPlan plan = planner.plan(resume, job);
// 自动反序列化为 POJO

// 方式 2：编程式
ChatResponse response = model.generate(messages, 
    ResponseFormat.jsonSchema(InterviewPlan.class));
String json = response.content().text();
InterviewPlan plan = objectMapper.readValue(json, InterviewPlan.class);
```

## 6.5 兜底策略

| 场景              | 兜底                    |
| --------------- | --------------------- |
| JSON 解析失败       | 重试 1 次（提示"输出非法 JSON"） |
| Schema 校验失败     | 重试 1 次（提示"缺少字段 X"）    |
| 重试仍失败           | 用规则兜底                 |
| 模型不支持 JSON Mode | 用 Prompt 约束 + 正则提取    |

---

# 第七部分 · 语音面试完整应用

## 7.1 场景与 Agent

| 场景              | Agent   | task            | 状态（v11.94.1 修订） |
| --------------- | ------- | --------------- | ---------------- |
| voice_interview | AI语音面试官 | warmup          | **已激活**（start 一次调用：理解+计划+开场+首题） |
| voice_interview | AI语音面试官 | answer_analysis | **已激活**（滑窗每轮点评+下一问） |
| voice_interview | AI语音面试官 | speak_text      | **已激活（语义重定义）**：question=结构化问题文本（报告回放/分析用），speakText=口语化话术（TTS 优先播、空则 fallback question）；落库同值兼容现状 |
| voice_interview | AI语音面试官 | candidate_ask   | **存量保留（无调用方）**：v11.94 曾激活（askPhase 独立反问段），v11.94.1 实测体验不佳移除独立链路——反问融入对话流（系统提示词承载：口头反问简答后继续，问满口播邀请后自然收尾），task 子任务与开关键一并废弃（SQL：20260916-02 删 sys_config 键） |
| voice_interview | AI语音面试官 | self_intro      | **待激活**（报告第一栏 selfIntro 摘要已有来源；需要 4 维评分时再启用） |
| voice_interview | AI语音面试官 | knowledge_desc  | **已激活**（报告知识点 LLM 简介；v11.94 起检索源 RAG 化） |
| voice_interview | AI语音面试官 | report          | 暂留业务侧（规则融合含权重表逻辑），v11.96+ 再评估迁场景 |

**一个场景 + 一个 Agent + 多个 task。**

> **v11.94 注记**：`sys_config` 残留键 `voice.interview.dynamicMode`（动态出题开关）已删除——纯残留，无消费方（SQL：20260916-01）。

## 7.2 预热（warmup）

**组装**：

```java
List<ChatMessage> messages = List.of(
    SystemMessage.from(agent.getSystemPrompt()),
    UserMessage.from(
        "【候选人简历】\n" + resumeDigest +
        "\n\n【岗位要求】\n" + jobContext +
        "\n\n【参考知识】\n" + knowledge +
        "\n\n【任务】请生成面试计划，输出 JSON"
    )
);
```

**输出**：

```json
{
  "understanding": {
    "candidateProfile": "...",
    "strengths": [...],
    "concerns": [...]
  },
  "interviewPlan": {
    "focusAreas": [...],
    "questionPlan": [...]
  },
  "opening": "...",
  "firstQuestion": {...}
}
```

## 7.3 后续每轮（answer_analysis）

> **v11.94 实现修正**：历史对话层由 **Redis 滑窗记忆承载**（`InterviewChatMemoryService`，MessageWindowChatMemory，N = maxHistoryTurns × 2），**不是每轮重拼 recentQAs**——滑窗零重拼、天然防重复提问，优于本节原始设计。system 常驻（人设 + 段序约束 + warmupPlan 渲染段），每轮只追加 UserMessage（进度指令 + 候选人回答）。

**组装（滑窗实现，V3/v11.94 已落地）**：

```java
// 1. system 一次性注入（start 时 initFirstTurn 幂等保证，常驻滑窗）
SystemMessage.from(buildInterviewerSystemPrompt(agent, 段序约束, warmupPlan渲染段))

// 2. 每轮仅追加当前轮指令（历史由滑窗自动携带，不重拼）
UserMessage.from("【进度】第 x/N 题\n【候选人回答】\n" + currentAnswer + "\n【任务】点评并生成下一问")
```

## 7.4 完整的 task 路由

```java
public JsonNode executeTask(String sceneCode, String task, 
                            Map<String, Object> input, Long userId) {
    // 1. 查场景配置
    AiSceneConfig scene = registry.getConfig(sceneCode);

    // 2. 查 Agent
    Agent agent = agentClient.resolveAgent(scene.getAgentId());

    // 3. 根据 task 组装消息
    List<ChatMessage> messages = switch (task) {
        case "warmup" -> buildWarmupMessages(agent, input);
        case "answer_analysis" -> buildAnalysisMessages(agent, input);
        case "speak_text" -> buildSpeakMessages(agent, input);
        case "candidate_ask" -> buildCandidateAskMessages(agent, input);
        default -> buildDefaultMessages(agent, input);
    };

    // 4. 查输出约束
    Object outputSchema = scene.getConfigJson().get("output_schema");

    // 5. 调用（带 schema + tools）
    return aiSceneJsonClient.executeForJson(
        sceneCode, messages, outputSchema, 
        getTools(scene), userId);
}
```

---

# 第八部分 · 记账账单识别应用

## 8.1 场景注册

```
scene_code = "ledger_bill_parse"
scene_name = "账单截图解析"
agent_id = 账单解析 Agent
config_json = {
    "task": "bill_parse",
    "response_format": "json_schema",
    "output_schema": {...}
}
```

## 8.2 Agent 配置

```json
{
  "agent_code": "ledger_bill_agent",
  "agent_name": "账单解析助手",
  "system_prompt": "你是账单识别专家。请识别用户上传的账单截图，提取金额、商户、时间、分类，输出 JSON。金额单位为元，分类从[餐饮,交通,购物,娱乐,居住,医疗,教育,其他]中选择。",
  "model_id": 2,
  "knowledge_base_ids": [],
  "tools": []
}
```

## 8.3 调用

```java
List<ChatMessage> messages = List.of(
    SystemMessage.from(agent.getSystemPrompt()),
    UserMessage.from(
        TextContent.from("请识别这张账单截图，提取金额、商户、时间、分类"),
        ImageContent.from(imageUrl)
    )
);

JsonNode result = aiSceneJsonClient.executeForJson(
    "ledger_bill_parse",
    messages,
    outputSchema,
    null,
    userId
);

// 结果
{
  "amount": 128.50,
  "merchant": "星巴克",
  "date": "2026-09-16",
  "category": "餐饮",
  "type": "expense"
}
```

## 8.4 前端反显

```vue
<template>
  <div>
    <input type="file" @change="handleUpload" />
    <div v-if="parsed">
      <input v-model="parsed.amount" placeholder="金额" />
      <input v-model="parsed.merchant" placeholder="商户" />
      <input v-model="parsed.date" placeholder="日期" />
      <select v-model="parsed.category">
        <option v-for="c in categories" :key="c">{{ c }}</option>
      </select>
    </div>
  </div>
</template>
```

---

# 第九部分 · Chat 接口整合

## 9.1 现状

```
/ai/chat/index?agentId=48 → 直接调 Agent（绕过场景）
```

## 9.2 整合方案

**新增默认场景**：

```
scene_code = "default_chat"
scene_name = "通用对话"
agent_id = null（运行时指定）
config_json = {
    "dynamicAgent": true,
    "rate_limit_count": 100,
    "rate_limit_time": 3600
}
```

**Chat 接口改造**：

```java
@PostMapping("/ai/chat")
public AjaxResult chat(@RequestBody ChatRequest request) {
    // 走默认场景
    return aiGatewayService.execute("default_chat", Map.of(
        "agentId", request.getAgentId(),
        "message", request.getMessage(),
        "history", request.getHistory()
    ), userId);
}
```

**好处**：

- 统一治理（限流/缓存/降级/日志）
- 场景配置生效
- 可观测

---

# 第十部分 · 数据模型

## 10.1 场景表（ai_scene_config）

**调整后字段**：

| 字段                         | 用途              | 变更           |
| -------------------------- | --------------- | ------------ |
| scene_code                 | 场景代码            | 保留           |
| scene_name                 | 场景名称            | 保留           |
| scene_category             | 分类              | 保留           |
| **agent_id**               | **绑定 Agent**    | **保留（唯一绑定）** |
| ~~model_id~~               | ~~直绑模型~~        | **废弃**       |
| ~~knowledge_base_ids~~     | ~~知识库~~         | **废弃**       |
| ~~tool_ids~~               | ~~工具~~          | **废弃**       |
| ~~system_prompt_template~~ | ~~提示词~~         | **废弃**       |
| ~~user_prompt_template~~   | ~~提示词~~         | **废弃**       |
| config_json                | 场景配置（task/mode） | 保留           |
| output_schema              | 输出结构            | 保留           |
| output_mode                | 输出模式            | 保留           |
| rate_limit_*               | 限流              | 保留           |
| fallback_*                 | 降级              | 保留           |
| enable_cache/cache_ttl     | 缓存              | 保留           |
| version/weight             | 灰度              | 保留           |

> **v11.94 迁移计划注记（存量路径收口）**：直绑模型（modelConfigId→伪Agent）与 systemPromptTemplate/userPromptTemplate 为存量兜底路径，**不是立即物理删除**——v11.95 起 6 个活跃场景全部核对迁 agent 绑定（KnowledgeQaHandler 提示词迁 agent），迁移完成后直绑路径标 `@Deprecated` 保留一个版本再删；期间增量 ALTER 仅加废弃注释、不删列，逐场景回归验证。

## 10.2 Agent 表（ai_agent）

**保持现状**：

| 字段                 | 用途       |
| ------------------ | -------- |
| agent_code         | Agent 代码 |
| agent_name         | 名称       |
| model_id           | 模型       |
| system_prompt      | 系统提示词    |
| welcome_message    | 开场白      |
| tools              | 工具       |
| knowledge_base_ids | 知识库      |
| workflow_id        | 工作流      |
| max_history_turns  | 历史轮数     |

## 10.3 工具表（ai_tool）

**新增**：

```sql
CREATE TABLE ai_tool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_code VARCHAR(50) NOT NULL,
    tool_name VARCHAR(100),
    description TEXT,
    parameters_schema JSON,
    executor_bean VARCHAR(100),
    enabled TINYINT DEFAULT 1,
    del_flag CHAR(1) DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tool_code (tool_code)
) COMMENT 'AI 工具定义';
```

## 10.4 模型表（ai_model_config）

**新增字段**：

```sql
ALTER TABLE ai_model_config
  ADD COLUMN supports_vision TINYINT DEFAULT 0 COMMENT '是否支持图片',
  ADD COLUMN supports_audio TINYINT DEFAULT 0 COMMENT '是否支持音频',
  ADD COLUMN supports_json_mode TINYINT DEFAULT 0 COMMENT '是否支持 JSON Mode',
  ADD COLUMN supports_tool_call TINYINT DEFAULT 0 COMMENT '是否支持工具调用';
```

---

# 第十一部分 · 实施路线

## 11.1 短期（1 周）

| #   | 任务         | 说明                    |
| --- | ---------- | --------------------- |
| 1   | 明确职责       | 场景=绑定+治理，Agent=提示词+能力 |
| 2   | 移除场景的提示词字段 | 或标记废弃                 |
| 3   | 统一提示词来源    | 全部用 Agent             |
| 4   | Chat 走默认场景 | 加治理                   |

## 11.2 中期（2 周）

| #   | 任务                 | 说明                  |
| --- | ------------------ | ------------------- |
| 5   | 工具表 + 执行器          | 支持 Function Calling |
| 6   | 模型表加多模态字段          | supports_vision 等   |
| 7   | 场景表加 output_schema | 结构化输出               |
| 8   | 语音面试多 task 统一      | 共用 Agent            |

## 11.3 长期

| #   | 任务         | 说明                |
| --- | ---------- | ----------------- |
| 9   | 账单识别场景     | 多模态 + 结构化         |
| 10  | Agent 版本管理 | 变更留痕              |
| 11  | 场景灰度       | version/weight 落地 |
| 12  | 执行日志管理页    | ai_execute_log 查询 |

---

# 第十二部分 · 完整调用链

```
┌─────────────────────────────────────────────────────────────┐
│  业务调用                                                    │
│  aiGatewayService.execute(sceneCode, task, input)           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  场景层                                                      │
│  1. 查场景配置（agent_id + 治理）                            │
│  2. 限流检查                                                 │
│  3. 缓存检查                                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  Agent 层                                                    │
│  1. 查 Agent（system_prompt + model + tools + knowledge）    │
│  2. 占位符渲染                                               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  消息组装                                                    │
│  1. SystemMessage（Agent 提示词）                            │
│  2. UserMessage（上下文 + 任务）                             │
│  3. 历史对话                                                 │
│  4. 多模态 Content（图片/音频，可选）                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  AI 底座                                                     │
│  1. 模型路由                                                 │
│  2. 工具调用（可选）                                         │
│  3. 结构化输出约束（可选）                                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  结果处理                                                    │
│  1. 解析输出                                                 │
│  2. 降级兜底（失败时）                                       │
│  3. 记录日志（ai_execute_log）                               │
│  4. 写入缓存                                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  返回结果                                                    │
└─────────────────────────────────────────────────────────────┘
```

---

# 第十三部分 · 优缺点

## 13.1 方案优点

| 优点    | 说明                    |
| ----- | --------------------- |
| 职责清晰  | 场景=绑定+治理，Agent=提示词+能力 |
| 提示词唯一 | 全部在 Agent             |
| 统一治理  | 场景统一限流/降级/缓存          |
| 可观测   | ai_execute_log 全链路    |
| 易扩展   | 新增场景只配 agent_id       |
| 支持多模态 | 图片/音频                 |
| 支持工具  | Function Calling      |
| 支持结构化 | JSON Schema           |

## 13.2 方案缺点

| 缺点   | 说明        |
| ---- | --------- |
| 需改造  | 场景表字段废弃   |
| 需迁移  | 现有配置迁移    |
| 复杂度  | 三层结构需理解   |
| 工具开发 | 每个工具要写执行器 |

## 13.3 与现状对比

| 维度    | 现状           | 整合后       |
| ----- | ------------ | --------- |
| 提示词来源 | 两处（场景+Agent） | 一处（Agent） |
| 治理    | 分散           | 统一        |
| Chat  | 绕过治理         | 走场景       |
| 多模态   | 无            | 支持        |
| 工具调用  | 无            | 支持        |
| 结构化输出 | 弱            | 强         |

---

# 第十四部分 · 关键决策记录

| 决策                  | 理由     |
| ------------------- | ------ |
| 场景表不配提示词            | 避免两处冲突 |
| Agent 表配提示词         | 能力定义   |
| 场景表配 output_schema  | 场景相关   |
| 工具表独立               | 可复用    |
| Chat 走默认场景          | 统一治理   |
| 语音面试多 task 共用 Agent | 简化配置   |
| 多模态字段加在模型表          | 模型能力   |
| 结构化输出用 JSON Schema  | 强约束    |

---

# 第十五部分 · 一句话总结

> **三层架构：场景（绑定+治理）→ Agent（提示词+能力）→ 底座（模型/知识库/工具）。**
> 
> **消息模型：SystemMessage（Agent）+ UserMessage（上下文+任务）+ 历史 + 多模态。**
> 
> **扩展能力：**
> 
> - **工具调用**：工具表 + 执行器 + Function Calling
> - **多模态**：模型表加字段 + Content 类型
> - **结构化输出**：场景表配 output_schema + JSON Schema
> 
> **语音面试：一个场景 + 一个 Agent + 多个 task。**
> 
> **记账账单识别：新场景 + 多模态 + 结构化输出。**
> 
> **Chat 接口：走默认场景，统一治理。**

---

需要我把这份文档拆成**可执行的开发任务清单**（含表结构变更、代码改动、接口设计）吗？
