# AI 统一入口整改方案

> v12.2 确立。目标：全平台 AI 调用统一收口 `com.moyun.ext.aiapp`（原 ai2），业务层只包装上下文，绑定场景+agent，走 AiGatewayService 统一入口，不直接调 LLM 大模型。

## 一、统一入口原则

1. **业务层只包装上下文**：构造 input Map（{userId, range, title, content...}），不直接调 LLMService / langchain4j
2. **每个业务加一个场景 Handler**：收口在 `com.moyun.ext.aiapp.handler.impl`，Handler 内做数据组装 + LLM 变换 + 降级兜底
3. **调用方绑定 sceneCode（+ 可选 agentId）**：走 `AiGatewayService.execute(AiExecuteRequest)` 或便捷入口 `AiSceneJsonClient.executeForJson(sceneCode, input, userId)`
4. **治理由网关统一施加**：限流 / Token 熔断 / 语义缓存 / 意图分类 / 注入防护 / 输出过滤 / 执行日志 / 降级，业务无感

## 二、已完成整改（v12.2）

| 项 | 改动 | 文件 |
|----|------|------|
| 包名语义化 | `com.moyun.ext.ai2` → `com.moyun.ext.aiapp`（36 文件目录 + 51 文件 import） | 全局 |
| Handler 收编 | FinanceAnalysisHandler 从 `ledger.handler` → `aiapp.handler.impl`（散落 Handler 统一归口） | FinanceAnalysisHandler.java |
| 职责边界明确 | AiSceneJsonClient=业务方外部入口；AbstractAiSceneHandler.chatJson=Handler 内部方法，注释划界 | AiSceneJsonClient.java |
| 面试治理加固 | InterviewAgentClient 灰度开关缺省改 true（langchain4j per-agent 直连暂保留，但治理前置检查默认开启） | InterviewAgentClientImpl.java |

## 三、待整改违规点（按优先级排序）

### ✅ P1 · PortalAiController 自造场景分发（v12.2.3 已完成）

**原状**：自维护 SCENES Map + 直调 `llmService.generate`，与 aiapp 统一网关机制重复。

**完成情况**（v12.2.3）：
1. 新建 `aiapp/handler/impl/ArticleMetaHandler.java`（scene_code=`article_meta`，AiSceneEnum.ARTICLE_META）
2. 新建 `aiapp/handler/impl/ContentTagsHandler.java`（scene_code=`content_tags`，AiSceneEnum.CONTENT_TAGS）
3. PortalAiController 薄化：删除 SCENES/SceneHandler/toPlainText/clip/group，改调 `aiGatewayService.execute(AiExecuteRequest)` + 从 GenericSceneData 提取结果

### ✅ P1 · CmsWritingPromptServiceImpl 直调 LLMService（v12.2.3 已完成）

**原状**：直调 `llmService.generate(aiPrompt)` 生成每日写作主题。

**完成情况**（v12.2.3）：
1. 新建 `aiapp/handler/impl/WritingPromptHandler.java`（scene_code=`writing_prompt`，AiSceneEnum.WRITING_PROMPT）
2. CmsWritingPromptServiceImpl 改调 `aiGatewayService.execute(AiExecuteRequest)`，只传 {date, specialDate} 上下文

### ✅ P0 · KnowledgeQaHandler 场景码不在 AiSceneEnum（v12.2.3 已完成）

**原状**：`getSceneCode()` 返回硬编码 `"knowledge_qa"`，不在 AiSceneEnum 7 个枚举项中，管理端场景总览盲区。

**完成情况**（v12.2.3）：AiSceneEnum 增加 `KNOWLEDGE_QA` 枚举项，KnowledgeQaHandler 改为 `AiSceneEnum.KNOWLEDGE_QA.getCode()`。

### ℹ️ admin 端直连 LLM（不整改）

用户明确（v12.2.3）：admin 端系统管理（agent / 工作流 / 大模型配置管理）的 AI 调用是合理的，修改范围只在实际业务端。以下 5 个 admin 侧 Service 保持原有 `llmService.generate()` 直调：

- PromptGeneratorServiceImpl / WorkflowGeneratorServiceImpl / IntelligentAnalysisServiceImpl / SQLGeneratorServiceImpl / DiagramChatServiceImpl

### ✅ P2 · AiModuleLlmClient / LlmClient 早期自造封装（v12.2.2 已完成）

**原状**：CMS 模块自造 LlmClient 接口 + AiModuleLlmClient 实现（场景感知委托 AiSceneResolver），与 aiapp 统一网关重叠。

**完成情况**（v12.2.2）：
1. 影响面排查：agent 对话走 DynamicChatServiceImpl（langchain4j StreamingChatLanguageModel per-agent 路由），完全不经过 LlmClient；前端无直接调用。6 个 Service（ResumeJobMatchService/ResumeDeepOptimizeGenerator/ResumeAiAdviceService/ResumeParseService/ResumeDeepOptimizeService/PortalJobTemplateServiceImpl）注入 LlmClient 但仅用 `llmClient.isEnabled()` 冗余开关（= `llmService != null`，与 aiGlobalSwitch 重复），实际 AI 调用已走 aiSceneJsonClient
2. 6 个 Service 清理：删 LlmClient 注入 + import + `&& llmClient.isEnabled()` 条件；PortalJobTemplateServiceImpl 改走 aiGlobalSwitch.isEnabled()（补注入 + import）
3. 删除 LlmClient.java + NoopLlmClient.java + AiModuleLlmClient.java
4. 6 处 javadoc/行注释中 LlmClient 引用更新为 AI 统一网关描述
5. 验证：13 文件括号配平，LlmClient 全局零残留

### P2 · 面试主干 InterviewAgentClient langchain4j 直连

**现状**：主干对话用 langchain4j per-agent 模型路由直连，评分走 aiapp 网关。v12.2 已强制治理前置检查默认开启。

**彻底整改前提**：AiGatewayService 增加流式 execute（`executeStream(request, SseEmitter)` 或返回 Flux），支持 per-agent 模型路由（ai_scene_config.agentId → ModelConfig）。

**整改步骤**：
1. AiGatewayService 新增 `executeStream(AiExecuteRequest, StreamCallback)` 流式端点
2. AbstractAiSceneHandler 新增 `chatStream` 经网关流式（已有 protected chatStream，需接通网关流式管道）
3. VoiceInterviewHandler 增加 executeStream 实现（主干对话场景）
4. InterviewAgentClient 改调 `aiGatewayService.executeStream`，删除 langchain4j 直连 + createModel
5. 删除灰度开关（统一走网关，不再有直连旁路）

## 四、整改后架构

```
业务层（Controller/Service）
  │ 只构造 input Map {userId, ...}，绑定 sceneCode
  ▼
AiSceneJsonClient.executeForJson(sceneCode, input, userId)   ← 便捷入口
  │
  ▼
AiGatewayService.execute(AiExecuteRequest)                    ← 统一网关
  │ 治理：注入防护 → 意图分类 → 语义缓存 → 限流 → Token熔断 → 日志
  ▼
AiSceneRegistry.getHandler(sceneCode) → XxxHandler.execute()  ← 场景 SPI
  │ Handler 内：数据组装 + chatJson/chat/chatStream + 解析 + 降级
  ▼
LLMService.generate / generateStream                           ← 底座统一 LLM
  │
  ▼
DashScope / 其他 Provider
```

**唯一例外**：AiGatewayService 支持流式后，面试主干也收口，无例外。
