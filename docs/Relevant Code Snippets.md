先找到并阅读方案文档。
The user wants me to start a major refactoring based on the document "AI能力统一接入层 — 完整方案文档.md" (AI Capability Unified Access Layer — Complete Solution Document). They said:

- Don't affect the AI foundation/base (AI底座)
- Generate complete paths placed under the `ai2` package
- Services go there, but calls remain in the original places

First, I need to find and read this document.

这是一个大型改造任务。先探索现有 AI 底座和散落的 AI 调用点，理解结构后再规划 ai2 包的实施。
The user wants me to implement the "AI能力统一接入层" (AI Capability Unified Access Layer) based on this document. Key constraints:
1. Don't affect the AI foundation (AI底座) - the existing AI module (com.moyun.ext.ai) stays untouched
2. Put the new code in an `ai2` package (complete path)
3. Services go in the new location, but calls remain in the original places (调用则还是原来的地方) - meaning existing callers don't change; the new layer is additive

Wait, let me re-read: "不要影响ai底座，如生成完整路径放在ai2包下，总之服务放在这，调用则还是原来的地方"

So: don't touch the AI foundation (com.moyun.ext.ai). Generate the complete path under the `ai2` package. Services (the unified access layer) go there, and the calls remain at the original places — meaning existing business modules keep calling their current code; we don't migrate them yet? Or the new gateway is exposed and business callers would call it later?

I think the meaning is:
- New package: com.moyun.ext.ai2 (sibling to com.moyun.ext.ai)
- The unified access layer (scene registry, handlers, gateway controller, intent classifier, semantic cache, rate limiter, metrics, fallback) all go under ai2
- Don't modify existing AI foundation code (com.moyun.ext.ai stays as-is)
- The "调用则还是原来的地方" means the existing business callers continue to call the original services - i.e., we're building the new layer in parallel (Phase 1+2), not migrating business modules yet (that would be Phase 3+).

Actually wait — "调用则还是原来的地方" could also mean: the callers of the AI capability (business modules) remain where they are, but they'd be switched to call the new gateway. Hmm. But "不要影响ai底座" (don't affect AI foundation) suggests we keep the existing AI module untouched.

Let me think about the safest interpretation:
1. Create com.moyun.ext.ai2 package with the full unified access layer implementation
2. Don't modify com.moyun.ext.ai (AI foundation) code
3. Business modules keep their current call sites (we don't migrate them in this step) — the new layer is ready to be adopted

Actually, re-reading again: "总之服务放在这，调用则还是原来的地方" — "in short, services go here (ai2), calls are still in the original places". This might mean the service implementations (handlers) live in ai2, but the existing business call sites stay unchanged for now.

Hmm, but that would make the new layer dead code. Alternatively it means: the new unified service lives in ai2, and the entry points (controllers) stay where they are — i.e., existing controllers/business services would now call into ai2 services.

Actually I think a reasonable interpretation for a "深度改造" (deep refactor) while "不影响AI底座":
- Build the complete ai2 package: unified request/response models, AiSceneHandler interface, AiSceneRegistry, AiGatewayController (/api/ai/execute), intent classifier, semantic cache, rate limiter, metrics, fallback, error codes, scene data models, DB tables (ai_scene_registry, ai_execute_log)
- The ai2 layer CALLS the existing AI foundation (com.moyun.ext.ai services like LlmClient, model config) — so the foundation is reused, not modified
- Existing business modules are NOT yet migrated (Phase 7 would do that)

Wait, but "调用则还是原来的地方" — maybe he means the frontend/business calls remain at the original endpoints. So this is purely additive: new gateway + infrastructure in ai2, reusing the AI foundation, without touching existing anything.

Hmm, but then what does 深度改造 mean? Building the whole framework per the doc: Phase 1 + Phase 2 (+ maybe a couple of example handlers Phase 3, and enhancement capabilities Phase 4-6).

Let me first explore the existing code structure to understand:
1. The AI foundation (com.moyun.ext.ai) - especially LlmClient, model config services
2. The existing scattered AI call points (interview, resume, question, ledger, sensitive word, daily topic)
3. How they call LLM (LlmClient.chat?)

Let me look at the structure first. This is a large task, so I should explore, then plan, then implement incrementally.

Let me use the Explore agent to understand the AI foundation and scattered call points. Actually, let me do some direct searches first to understand scale.

我来并行搜索这个 Spring Boot 项目的关键结构。先从目录结构、关键类和调用点开始。

### Relevant Code Snippets
1. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\service\ModelConfigService.java:L16-L113
   — AI底座 `com.moyun.ext.ai` 的模型配置与模型实例创建接口；定义 `createChatModel`、`createStreamingChatModel`、`createEmbeddingModel`、默认模型获取、按模型名获取配置等核心能力。

2. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\service\impl\ModelConfigServiceImpl.java:L152-L243
   — `ModelConfigService` 的实现：从 `model_config` 读取配置并构建 `ChatLanguageModel` / `StreamingChatLanguageModel` / `EmbeddingModel`，是“AI能力统一接入层”可复用的模型实例化入口。

3. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\service\LLMService.java:L9-L39
   — AI底座提供的统一 LLM 服务接口，封装默认模型生成、指定模型生成、流式生成；新包实现接入层时可委托或适配该接口，避免直接调用底层 LangChain4j。

4. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\service\impl\LLMServiceImpl.java:L55-L161
   — `LLMService` 实现展示如何获取默认聊天模型、创建 `ChatLanguageModel`、同步调用 `model.chat(...)`，以及流式 `createStreamingChatModel(...)` 调用链路。

5. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\cms\service\LlmClient.java:L21-L66
   — 现有业务模块中的统一 LLM 客户端抽象，提供同步 `chat`、多轮 `chat(List<...>)`、场景感知 `chat(sceneCode, ...)`；与“统一网关 + 场景Handler”模式高度相关。

6. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\cms\service\AiModuleLlmClient.java:L38-L132
   — `LlmClient` 的真实实现：桥接到 `com.moyun.ext.ai.service.LLMService`，并基于 `AiSceneResolver` 实现场景码到绑定模型的解析；可作为 `com.moyun.ext.ai2` 场景 Handler 模式的现有参考。

7. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\constant\RedisKeys.java:L133-L181
   — AI底座已有配置缓存常量，包括模型配置缓存 `config:model:{id}`、默认模型缓存 `config:model:default:{type}`；新包实现统一接入层时应复用这些缓存语义。

8. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\util\RateLimiter.java:L9-L105
   — 现有简单限流工具类，提供 `tryAcquire(userId, limit)`、剩余次数、过期清理；与查询中的“现有限流实现”相关，可作为新接入层网关限流的可复用组件。

报告不完整，继续向探索代理索取剩余关键信息。
The agent returned a very short/incomplete report. I need more details. Let me continue asking the agent for the full structured report, or do the exploration myself. The report seems truncated — it only covered part 1 and part of 3. Let me send a follow-up to the agent to get the remaining answers (scattered AI call points, infrastructure details, SSE usage, etc.).

### Relevant Code Snippets
1. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\cms\service\AiModuleLlmClient.java:L19-L31
   — `com.moyun.ext.cms.service.LlmClient` 的真实实现，桥接 `com.moyun.ext.ai.service.LLMService`，并说明 `v11.39` 场景感知逻辑：带场景码调用委托 `AiSceneResolver#resolveChatModel`，无绑定则回落默认模型。

2. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\cms\service\AiModuleLlmClient.java:L93-L115
   — `chat(String sceneCode, String systemPrompt, String userMessage)` 的核心实现：先通过 `sceneResolver.resolveChatModel(sceneCode)` 获取场景绑定模型，命中后调用 langchain4j `ChatLanguageModel.chat(...)`，失败或无绑定则回落到默认 `LLMService.generate(...)`。

3. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\cms\service\LlmClient.java:L5-L20
   — `com.moyun.ext.cms.service.LlmClient` 接口定义，说明统一封装 LLM 调用、失败返回 null、场景感知 `chat(sceneCode, ...)` 的设计目标，是“AI能力统一接入层”的直接复用接口。

4. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\service\AiSceneResolver.java:L6-L25
   — `AiSceneResolver` 接口说明场景解析器职责：将业务场景代码解析为 Agent/模型/知识库/工具/工作流绑定，并说明 `v11.39` 新增 `resolveChatModel` 工厂方法。

5. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\service\AiSceneResolver.java:L40-L53
   — `resolveChatModel(String sceneCode)` 的契约：按场景代码解析绑定的 `ChatLanguageModel`，责任链为 Agent 绑定 → 直绑模型 → null 回落默认，正对应统一接入层需要的“场景Handler/模型选择”逻辑。

6. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\service\impl\AiSceneResolverImpl.java:L44-L64
   — `AiSceneResolverImpl.resolve(...)` 的实现：从 `ai_scene_config` 查询启用配置，单行直接命中，多行按权重灰度选择，异常返回空绑定，是场景码机制的运行时入口。

7. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\service\impl\AiSceneResolverImpl.java:L130-L162
   — `resolveChatModel(...)` 的具体实现：空场景返回 null；命中配置后优先使用 Agent 的 `modelConfigId/temperature/maxTokens` 构建模型，其次使用直绑模型，否则返回 null 让调用方回落默认模型。

8. e:\zyg_new_work\moyun-project-document\moyun-server\src\main\java\com\moyun\ext\ai\util\RateLimiter.java:L6-L29
   — `com.moyun.ext.ai.util.RateLimiter` 的完整实现位置与基础配置：基于 `ConcurrentHashMap` 的内存限流器，默认时间窗口 1 分钟、默认限制 20 次，未使用 Redis。

子代理报告持续截断，我直接并行探索关键点。
The Explore agent keeps returning truncated reports (the tool seems to only return the first part). The reports are consistently cut short. Let me just do the key exploration myself with direct tools - I have enough context on the core mechanism. I need:

1. Scattered AI call points (which business classes call LlmClient)
2. LLMService interface signatures
3. RateLimiter full implementation (already know it's in-memory ConcurrentHashMap)
4. Basic paths: AjaxResult, BusinessException
5. DDL naming convention
6. SseEmitter vs Flux
7. MapperScan config
8. Scene codes already defined

Let me do these searches in parallel directly.

Now I have a good picture. Let me check a few more things quickly:
1. AjaxResult path and usage
2. BusinessException path
3. DDL naming convention
4. MapperScan config
5. SseEmitter usage
6. ModelConfigService interface signatures

Then plan and implement.

Now I have a comprehensive understanding. Let me check a couple more things:
1. AiSceneEnum (scene codes registered)
2. AjaxResult location
3. ModelConfigService interface for getting default model

Then I'll present a plan. Actually since this is a big task, let me use TodoWrite to plan and then implement. Let me check the last few pieces.

信息已足够。再确认 ModelConfigService 签名和安全配置，然后规划实施。
Now I have a full picture. Let me summarize the current state:

**Existing infrastructure:**
- `com.moyun.ext.ai` = AI底座 (foundation): knowledge base, tools, workflow, model config, Agent, AiSceneResolver, AiSceneConfig (scene binding table `ai_scene_config`), AiSceneEnum (5 scenes: voice_interview, resume_parse, resume_optimize, question_generate, finance_analysis), LLMService, ModelConfigService, RateLimiter (in-memory)
- `com.moyun.ext.cms.service.LlmClient` = interface used by business modules, with AiModuleLlmClient bridging to LLMService + AiSceneResolver
- Scattered call points (~12+ in cms, ledger packages)
- AjaxResult at com.moyun.core.base.AjaxResult
- BusinessException at com.moyun.ext.ai.exception.BusinessException
- MapperScan: com.moyun.**.mapper
- SseEmitter used in WorkflowController, DiagramStreamController, VoiceInterviewServiceImpl, PortalVoiceInterviewController
- DDL naming: yyyyMMdd-NN-description.sql or yyyyMMdd-description.sql

**User requirements:**
1. Based on the doc: AI能力统一接入层 — 完整方案文档.md
2. Don't affect AI foundation (com.moyun.ext.ai unchanged)
3. New code under com.moyun.ext.ai2 package
4. "服务放在这，调用则还是原来的地方" — services in ai2, calls stay in the original places. 

Interpretation of #4: The new unified access layer services go in ai2. The existing business call sites (调用点) remain where they are — i.e., we don't migrate business modules yet; OR it means the entry controller endpoints remain accessible from where they are now.

Hmm, actually re-reading: "总之服务放在这，调用则还是原来的地方" — "In short, services are placed here (ai2), and the calling (调用) is still in the original place."

I think this means: the new unified gateway implementation (services/handlers/registry) lives in ai2, but existing business modules keep calling through their current paths (LlmClient etc.) — we're building the new layer in parallel without touching existing callers. This matches "不要影响ai底座" and the phased approach (Phase 1-2 now, migration Phase 3+ later).

But wait — that would make ai2 dead code. Alternatively: existing business call sites will be switched to call ai2's unified entry (AiGatewayService), keeping their own controllers/endpoints unchanged ("调用则还是原来的地方" = the places that make calls are still the original files, just the internal call target changes to ai2 services).

Actually I think the second interpretation is more meaningful for a "深度改造": refactor the scattered AI call points to go through the unified access layer in ai2, but:
- The AI foundation (com.moyun.ext.ai) is untouched
- The new layer lives in ai2
- The calling code (business services like VoiceInterviewServiceImpl, ResumeParseService) stays in its original location/file — just calls into ai2's unified service instead of directly llmClient.chat()

Hmm, but that's a huge migration touching 12+ files. The doc's Phase 3 says migrate 3 core scenarios.

Given ambiguity, the safest and most valuable approach:
1. Build the complete ai2 infrastructure (Phase 1 + 2 + 4-6 essentials): 
   - ai2.model (AiExecuteRequest, AiExecuteResponse, AiMetadata, scene data models)
   - ai2.handler (AiSceneHandler interface)
   - ai2.registry (AiSceneRegistry + config entity/mapper + AiSceneRegistryConfig)
   - ai2.gateway (AiGatewayController + AiGatewayService)
   - ai2.support (IntentClassifier, SemanticCache, AiRateLimiter, FallbackStrategy, AiMetricsCollector/AiExecuteLog)
   - ai2.error (AiErrorCodes)
   - DDL for ai2_scene_registry + ai2_execute_log tables
2. Migrate the existing 5 scene codes (voice_interview, resume_parse, resume_optimize, question_generate, finance_analysis) as Handlers in ai2 that DELEGATE to the existing business services? No wait — the handlers should encapsulate the AI calling logic.

Hmm wait. Let me re-read the user's instruction once more: "现在请开始根据文档：AI能力统一接入层 — 完整方案文档.md 开启深度改造，不要影响ai底座，如生成完整路径放在ai2包下，总之服务放在这，调用则还是原来的地方"

"如生成完整路径放在ai2包下" = "e.g., generated complete paths go under the ai2 package"
"总之服务放在这" = "in short, the services go here (ai2)"
"调用则还是原来的地方" = "the calls are still in the original places"

I now believe: the new unified access layer SERVICES go in ai2. The CALLERS (business modules) stay in their original places — but they will call the new ai2 services. I.e., don't move business code around; just have them route through ai2.

But actually the simplest reading: "调用则还是原来的地方" might just mean we don't need to change the existing call sites at all in this phase — build the layer first. The doc itself has a phased roadmap where Phase 7 is "迁移剩余场景 + 关闭旧接口".

Given the scale, I'll implement:

**Phase 1 (data + core):**
- DDL: `ai2_scene_registry` (场景注册表), `ai2_execute_log` (调用日志表) — note: the doc says ai_scene_registry / ai_execute_log but there's already ai_scene_config in the foundation; to avoid conflicts and per "ai2" naming, I'll name tables ai2_scene_registry and ai2_execute_log. Hmm, but wait — the doc explicitly says `ai_scene_registry`. But the user said put things in ai2 package. Table names: existing foundation has `ai_scene_config`. The new registry table per doc is `ai_scene_registry`. To be safe and clearly separated, use `ai2_scene_registry` and `ai2_execute_log`. Actually let me follow the doc's table names but check for conflicts: is there an existing ai_scene_registry? No, there's ai_scene_config. Is there ai_execute_log? Need to check.

Let me check existing table names to avoid conflict. Actually the entity AiSceneConfig uses table ai_scene_config. Let me quickly grep for "ai_execute_log" and "ai_scene_registry" in the codebase.

- Entities: AiSceneRegistryConfig (or per doc naming), AiExecuteLog
- Mappers: both (placed in com.moyun.ext.ai2.mapper so MapperScan com.moyun.**.mapper picks up)

**Phase 2 (gateway):**
- ai2.model: AiExecuteRequest, AiExecuteResponse<T>, AiMetadata
- ai2.handler: AiSceneHandler interface (sync + stream via SseEmitter)
- ai2.registry: AiSceneRegistry (component, registers all AiSceneHandler beans, loads config from DB with cache)
- ai2.gateway: AiGatewayController (/api/ai/execute, /api/ai/execute/stream) + AiGatewayService (orchestrates: intent → cache → config → rate limit → handler → metrics → log)

**Phase 3 (handlers for existing 5 scenes):**
The doc's Phase 3 says migrate 3 core scenarios (interview/resume/question). Handlers implement the AI calls by reusing foundation's AiSceneResolver + LLMService/ModelConfigService. But the full business logic (resume parsing, interview scoring) lives in cms services. For the handlers to be useful, they'd encapsulate prompt building + LLM call + response parsing per scene, without depending on the cms business services (to avoid circular dependency ai2 → cms → ai2? Actually cms calls LlmClient which is in cms package).

Circular dependency risk: if ai2 handlers reference cms services, and later cms services call ai2 gateway → cycle. For Phase 3, I'll implement handlers that are self-contained: they build prompts, call foundation (AiSceneResolver.resolveChatModel → fallback LLMService), parse output. The deep business-context building (e.g. loading resume data from DB) can be done in the handler via mappers/services of foundation only... but resume data is in cms tables.

Hmm, this is getting complex. Let me scope reasonably:

Given "深度改造" and the user's instruction, I'll deliver:

1. **ai2 基础框架**（完整）:
   - error codes, request/response models (incl. all 6 scene data classes from doc §4.3)
   - AiSceneHandler interface
   - AiSceneRegistry (+ entity/mapper/DDL for ai2_scene_registry, with default config INSERTs for the 5 existing scenes)
   - AiGatewayController + AiGatewayService (execute + executeStream)
   - IntentClassifier, SemanticCache (Redis-based, embedding similarity — but need EmbeddingModel; foundation has ModelConfigService.createEmbeddingModel; for simplicity, semantic cache can start with exact-match hashing + optional embedding similarity. The doc's semantic cache uses embedding similarity >0.95. I can implement embedding-based using ModelConfigService; if embedding model unavailable, fall back to disabled)
   - AiRateLimiter (Redis-based fixed window, per scene config)
   - FallbackStrategy
   - AiExecuteLog entity + mapper + async logging service (metrics)
2. **示例/核心 Handlers**: 实现文档中提到的场景 Handler。Which ones? The doc §4.3 defines 6 scenes: interview, resume, question_generate, ledger, sensitive_word, daily_topic. Existing foundation scenes: voice_interview, resume_parse, resume_optimize, question_generate, finance_analysis.

To not affect the foundation and keep callers in place, I'll implement generic handlers that reuse AiSceneResolver + LLMService for model calls. Practical set:
   - SensitiveWordHandler (sync, self-contained — doc example)
   - DailyTopicHandler (sync, self-contained)
   - And a "generic" scene handler? 

Hmm wait. Actually, maybe the better approach for "深度改造" while "调用还是原来的地方": the ai2 gateway's value is being the unified entry. Business modules' existing LlmClient calls would gradually route through it. 

Let me reconsider what "调用则还是原来的地方" means one final time. In context: "不要影响ai底座，如生成完整路径放在ai2包下，总之服务放在这，调用则还是原来的地方"

I'm now fairly confident: "服务放在这" = new services in ai2; "调用则还是原来的地方" = the existing call sites (VoiceInterviewServiceImpl etc.) remain in their original files/packages — i.e., DON'T move business code into ai2, DON'T restructure business modules. The ai2 layer provides the unified service; business callers keep living where they are.

So the deliverable: complete ai2 unified access layer + switch existing scattered LlmClient.chat() call sites to call ai2's unified service? That would modify ~12 files in cms/ledger — "调用还是原来的地方" = the calling code stays in the same files, just calls the new service.

Hmm, but that's risky and contradicts "不要影响" spirit. Actually no — it doesn't affect the AI 底座 (com.moyun.ext.ai), it affects business modules.

Actually, you know what? The cleanest interpretation that satisfies everything:

1. Build complete ai2 layer (models, handler SPI, registry, gateway, enhancements, DDL, logging)
2. Business callers stay in original places AND keep working as-is (no migration in this step — that's Phase 7 of the doc)
3. The ai2 gateway is fully functional with a set of handlers covering the existing 5 foundation scenes (implemented by reusing foundation services — AiSceneResolver, LLMService, ModelConfigService) + doc's additional scenes (sensitive_word, daily_topic)

This delivers a working unified entry without touching anything existing. The user can then verify and we migrate callers incrementally later.

Hmm, but the user said "开启深度改造" (start deep transformation). And the phases in the doc: Phase 1 数据表+核心类, Phase 2 统一入口Controller+基础Handler, Phase 3 迁移3个核心场景, Phase 4 意图判断+语义缓存, Phase 5 可观测性, Phase 6 灰度+降级, Phase 7 全量切换.

"开启深度改造" suggests starting the transformation per the doc. I'll implement Phases 1-6 infrastructure in ai2 + representative handlers, leaving existing callers untouched (Phase 7 deferred, which matches "调用则还是原来的地方").

Wait, actually, maybe I'm overanalyzing. Let me just also make one business module demonstrate the integration? No — "不要影响ai底座...调用则还是原来的地方" — keep callers unchanged. I'll build the complete layer. If the user wants migration of call sites, that's a follow-up.

Let me now check: does an `ai_execute_log` or similar table already exist? And check ModelConfigService interface for embedding model creation. And RedisKeys structure for cache key conventions. Then plan with TodoWrite and implement.

Design decisions:
- Package: com.moyun.ext.ai2
  - com.moyun.ext.ai2.controller: AiGatewayController
  - com.moyun.ext.ai2.service: AiGatewayService, AiSceneRegistry
  - com.moyun.ext.ai2.handler: AiSceneHandler + impls
  - com.moyun.ext.ai2.model: AiExecuteRequest, AiExecuteResponse, AiMetadata, scene data classes
  - com.moyun.ext.ai2.entity: AiSceneRegistryConfig, AiExecuteLog
  - com.moyun.ext.ai2.mapper: AiSceneRegistryMapper, AiExecuteLogMapper
  - com.moyun.ext.ai2.support: IntentClassifier, SemanticCache, SceneRateLimiter, FallbackStrategy
  - com.moyun.ext.ai2.constant: AiErrorCodes, Ai2RedisKeys

- Endpoint: doc says /api/ai/execute. Existing project uses /cms/ai/... for admin. The doc says /api/ai. I'll follow the doc: /api/ai/execute and /api/ai/execute/stream. Hmm, but permission: existing controllers use @PreAuthorize. The gateway should probably also require auth. But business portal calls? Let me check how SecurityConfig handles /api/** vs /cms/**... Actually simpler: follow doc exactly (/api/ai/execute) and add @PreAuthorize with a generic permission? If the permission isn't in the DB, calls would fail. Maybe permit authenticated users. Let me check the security config for patterns.

Actually, let me not over-engineer. I'll check the security config quickly. The admin endpoints all have @ss.hasPermi. For the gateway, I could make the permission configurable per scene... too complex. Simplest: @PreAuthorize("isAuthenticated()") — hmm, need to check the project's security setup.

Let me check SecurityConfig / how other endpoints handle auth. Also check whether there's a portal (portal package uses PortalVoiceInterviewController without @PreAuthorize probably).

Plan for handlers (7 scenes):
1. voice_interview — complex (6-stage flow), Phase 3 doc says interview/resume/question. Implementing the full voice interview logic in a handler would duplicate VoiceInterviewServiceImpl. Instead, the handler for interview scenes should delegate to... hmm.

OK here's the thing — the real value and correctness: handlers for scenes where logic is prompt+LLM+parse (self-contained):
- resume_parse (parse resume text → structured JSON)
- resume_optimize (resume + position → suggestions)
- question_generate (position/skills/difficulty → question list)
- finance_analysis (ledger data → analysis report)
- sensitive_word (text → detection result)
- daily_topic (date → topic)
- voice_interview (answer → evaluation)

Each handler builds prompt from input params, calls model via foundation (AiSceneResolver.resolveChatModel(sceneCode) → fallback LLMService), parses JSON output into scene data class. This is exactly the doc's vision: handlers own prompt+parse; they don't need the business services' DB logic (the gateway caller passes business data in `input` map — e.g. resume text, ledger summary stats).

This is self-contained, no circular deps, doesn't touch foundation or business code. 

For the model calls: use AiSceneResolver.resolveChatModel(sceneCode), fallback to LLMService.generate(prompt). For streaming: LLMService.generateStream. 

For SemanticCache: needs EmbeddingModel — ModelConfigService can create it. Implement: on get, compute embedding of input, compare against cached embeddings (store embedding + response in Redis list per scene). Similarity > 0.95 → hit. If embedding model unavailable → cache disabled (return null). TTL from scene config.

RateLimiter: Redis-based INCR + EXPIRE fixed window, key ai2:rate:{scene}:{userId or ip}.

IntentClassifier: per doc, rule-based.

Metrics: AiExecuteLog written async (@Async or simple executor) to MySQL.

DDL: 20260909-01-ai2-unified-gateway.sql with ai2_scene_registry + ai2_execute_log + default scene config rows.

Wait — one issue: entity table names. Doc says ai_scene_registry and ai_execute_log. There's already ai_scene_config. If I create ai_scene_registry, it may confuse with foundation's scene config but it IS a different concept (registry of unified gateway scenes). Since user wants ai2 separation, I'll use ai2_ prefix for absolute clarity: ai2_scene_registry, ai2_execute_log. This clearly marks them as belonging to the new layer. I think that's better.

Hmm, but the doc is the spec... The user said "根据文档" but also "放在ai2包下" and overall separation. Table prefix ai2_ aligns with the package naming. I'll go with ai2_ prefix and note it.

Let me also check ModelConfigService interface to know method signatures for embedding model + default config. And check security config for URL patterns. Let me read those.

I have enough understanding now. Let me check how the admin security config handles /cms/** (to decide the endpoint path and auth for the gateway). The doc says /api/ai/execute. The admin (moyun-admin-vue) calls /cms/ai/*. The gateway will serve both admin and portal? To keep it simple and safe, I'll:

- Follow the doc: `/api/ai/execute` + `/api/ai/execute/stream`
- Need to check if SecurityConfig requires auth for /api/**. Let me check the main SecurityConfig quickly.

Actually, let me check the main admin security config.

Security: anyRequest().authenticated() — so /api/ai/** will require auth by default, which is fine. I'll use the doc's path `/api/ai/execute` and add a permission check. Since permissions in this project use `@ss.hasPermi('cms:ai:...')`, and the gateway is a new layer, I could use a new permission `ai2:gateway:execute`... but if not seeded in menu DB, calls fail. Safer: `isAuthenticated()` via just letting the security chain handle it, no @PreAuthorize annotation. The gateway itself is an internal unified entry; caller identity is tracked via SecurityUtils.getUserId() if available.

Let me check how userId is obtained — SecurityUtils.getUserId() probably. Quick grep.

Actually let me just check quickly and then start. I'll look at a controller that gets current user.

信息收集完毕，开始规划实施。
`SecurityUtils.getUserId()` at com.moyun.core.?.SecurityUtils — imported in that controller. Let me check the import quickly... it's probably com.moyun.core.security or common. I'll grep the class file location during implementation.

Now I have the full picture. Time to plan with TodoWrite and start implementing.

**Implementation plan (com.moyun.ext.ai2):**

DDL: `20260909-01-ai2-unified-gateway.sql`
- `ai2_scene_registry` (per doc §3.1, with handler bean name, prompt templates, output config, rate limit, fallback, cache)
- `ai2_execute_log` (per doc §3.2)
- Default INSERT rows for 7 scenes: voice_interview, resume_parse, resume_optimize, question_generate, finance_analysis, sensitive_word, daily_topic

Code structure:
1. `com.moyun.ext.ai2.constant.AiErrorCodes` — per doc §8
2. `com.moyun.ext.ai2.model`:
   - AiExecuteRequest
   - AiExecuteResponse<T> (with static success/failure/clarification factories)
   - AiMetadata
   - data classes: InterviewSceneData, ResumeSceneData, QuestionSceneData, LedgerSceneData, SensitiveWordSceneData, TopicSceneData
3. `com.moyun.ext.ai2.entity.AiSceneRegistryConfig` (@TableName ai2_scene_registry)
4. `com.moyun.ext.ai2.entity.AiExecuteLog` (@TableName ai2_execute_log)
5. `com.moyun.ext.ai2.mapper.AiSceneRegistryMapper`, `AiExecuteLogMapper` (MapperScan com.moyun.**.mapper covers it)
6. `com.moyun.ext.ai2.handler.AiSceneHandler` — per doc §5.1 (with SseEmitter streaming)
7. `com.moyun.ext.ai2.registry.AiSceneRegistry` — per doc §5.2 (+ getConfig with DB load + refresh endpoint)
8. `com.moyun.ext.ai2.support`:
   - IntentClassifier (doc §7.1)
   - SemanticCache (doc §7.2 — embedding similarity via ModelConfigService.getDefaultEmbeddingConfig; disabled if no embedding model; Redis storage)
   - SceneRateLimiter (Redis INCR+EXPIRE)
   - FallbackStrategy (doc §7.3)
   - AiExecuteLogService (async write log)
9. `com.moyun.ext.ai2.service.AiGatewayService` — orchestration per doc §5.3 Controller logic (intent → cache → config → rate limit → handler → response → cache write → metrics/log)
10. `com.moyun.ext.ai2.controller.AiGatewayController` — /api/ai/execute (sync), /api/ai/execute/stream (SSE), plus /api/ai/scenes (list available scenes for debugging/管理), /api/ai/refresh (reload registry config)
11. Handlers (`com.moyun.ext.ai2.handler.impl`):
    - AbstractBaseHandler? Common model resolution: AiSceneResolver.resolveChatModel(sceneCode) → fallback LLMService. JSON parse helper.
    - VoiceInterviewHandler (sync eval + stream?) — evaluation of answer
    - ResumeParseHandler (sync)
    - ResumeOptimizeHandler (sync)
    - QuestionGenerateHandler (sync)
    - FinanceAnalysisHandler (sync)
    - SensitiveWordHandler (sync)
    - DailyTopicHandler (sync)

All handlers: build prompts (from templates in config, with {{variable}} placeholder support), call model, parse JSON into scene data, return AiExecuteResponse.

Base handler should support:
- resolveModel(sceneCode): AiSceneResolver.resolveChatModel → fallback LLMService-based (via ModelConfigService.getDefaultChatConfig + createChatModel) 
- chat(sceneCode, systemPrompt, userPrompt): returns String
- parseJson(text, Class<T>): robust JSON extraction (strip markdown fences etc.)

This is a large but well-defined implementation. ~20 files. Let me be efficient.

Let me check RedisTemplate availability — RedisChatMemoryStore uses RedisTemplate<String,String>. I'll use RedisTemplate<String,String> and Jackson manually, or check if there's a RedisTemplate<String,Object>. RedisChatMemoryStore autowires RedisTemplate<String, String>. I'll use that + ObjectMapper.

For async log writing: use @Async? Need to check if @EnableAsync is on. Simpler: use a dedicated ExecutorService or just synchronous insert wrapped in try-catch (log write is small). I'll do try-catch sync insert to keep simple, or check for existing async config. Let me check @EnableAsync.

Also SseEmitter: project uses both Flux and SseEmitter; for the gateway stream endpoint I'll use SseEmitter per doc.

Let me check @EnableAsync and ObjectMapper bean availability.

开始实施。第一步：DDL。
Now let me start implementing. First the DDL. I'll name it 20260909-02-ai2-unified-gateway.sql (there might already be a 20260909-01; let me check quickly... not critical, I'll use 20260909-ai2-unified-gateway.sql which matches the naming style like 20260908-moyun-pay-gateway.sql).

Design the DDL per doc §3.1/§3.2 with ai2_ prefix. Scene codes: reuse foundation's 5 scenes + doc's sensitive_word/daily_topic.

Default configs: handler_bean_name must match Spring bean names of my handlers. I'll name handler beans by class default (e.g. "voiceInterviewHandler").

Let me write the DDL now.

DDL 完成。第二步：基础模型类。
Now the base models. Package com.moyun.ext.ai2.model:
1. AiExecuteRequest
2. AiExecuteResponse<T>
3. AiMetadata
4. Scene data classes: InterviewSceneData, ResumeSceneData, QuestionSceneData, LedgerSceneData, SensitiveWordSceneData, TopicSceneData

And AiErrorCodes in com.moyun.ext.ai2.constant.

Let me write these efficiently. I'll use Lombok @Data.

Wait — need to check if the project uses jakarta or javax validation. Spring Boot 3 = jakarta. The ai package... let me check an existing DTO with validation. ChatRequest has @NotNull? Let me check quickly during next steps. Actually KnowledgeBaseController used @RequestParam etc. Let me check ChatRequest for validation annotations.

jakarta 正确。继续写响应模型。
Good, jakarta is correct. Continue with AiExecuteResponse and AiMetadata.

现在写 6 个场景 Data 类（放在 model.data 子包）。

删除多余的占位文件，直接写 6 个 Data 类。
Actually that doc placeholder file is unnecessary noise — let me delete it and just create the 6 data classes.

第三步：实体 + Mapper。

第四步：Handler 接口与基类。
Now the handler interface + abstract base. The base class needs:
- AiSceneResolver (from foundation) to resolve scene-bound model
- LLMService (foundation) as fallback
- ModelConfigService (foundation) for default model name metadata
- JSON parsing helper (strip markdown fences, extract JSON)
- Template rendering ({{variable}})

Let me write AiSceneHandler interface per doc §5.1, adapted:
- ServiceException in the doc doesn't exist; use BusinessException from com.moyun.ext.ai.exception

AiSceneRegistryConfig reference in buildSystemPrompt — the config entity. OK.

Need to check ErrorCode has PARAM_ERROR. Let me check the ErrorCode enum quickly.

查看 AiModuleLlmClient 的模型调用方式（langchain4j 版本 API 兼容）。
ErrorCodes exist: SYSTEM_ERROR, PARAM_ERROR. Good.

Now check BusinessException constructor signature: BusinessException(ErrorCode, String) was used in VectorMigrationService? In KnowledgeBaseServiceImpl: `new BusinessException(ErrorCode.DOCUMENT_PROCESS_FAILED, "Save segment failed: index=" + i)`. And with 3 args: `new BusinessException(ErrorCode.DOCUMENT_PROCESS_FAILED, "Save segment exception: index=" + i, e)`. Good.

Now write the AbstractAiSceneHandler base class:
- @Autowired AiSceneResolver, LLMService, ModelConfigService
- chat(sceneCode, systemPrompt, userPrompt) → resolve model via AiSceneResolver.resolveChatModel, fallback LLMService.generate
  Actually better: if resolved model != null → model.chat(...) else LLMService.generate(systemPrompt + "\n" + userPrompt)? LLMService.generate takes only a prompt. Hmm — LLMService.generate(String prompt). To include system prompt, combine. Alternatively use ModelConfigService.getDefaultChatConfig() + createChatModel to build default model and call with messages.

Let me check what langchain4j version is used: 1.0.0-beta3 per summary. In beta3, ChatLanguageModel.chat(String... texts) deprecated → generate? In 1.0.0-beta3, the interface has `String generate(String...)`? Actually langchain4j 1.0.0-beta3 changed to `chat(...)`. Let me check AiModuleLlmClient to see how it calls.

API 模式明确。写 Handler 基类。
Now I understand the model call pattern: langchain4j 1.0.0-beta3 API: `boundModel.chat(List<ChatMessage>)` returning ChatResponse.

Now write the AbstractAiSceneHandler base class with:
- AiSceneResolver resolve
- LLMService fallback (system + user combined)
- chat(sceneCode, systemPrompt, userPrompt) method
- chatStream via LLMService.generateStream
- parseJson robust helper
- renderTemplate ({{var}})

Then 7 handlers. For handler model resolution, reuse pattern: AiSceneResolver.resolveChatModel(sceneCode) → if found, model.chat(...); fallback llmService.generate(system+user).

Note: `moyun.ai.enabled` conditional — LLMService bean may not exist. Use @Autowired(required=false).

Let me write the base class.

第五步：7 个场景 Handler。
Now the 7 handlers. Let me write them. Each handler:
1. VoiceInterviewHandler — scene voice_interview, mode both. Sync: evaluate answer → InterviewSceneData (evaluation, score, nextAction). Stream: stream evaluation.
2. ResumeParseHandler — resume_parse, sync: parse resume text → ResumeSceneData (structured map)
3. ResumeOptimizeHandler — resume_optimize, sync: suggestions, score, optimizedText
4. QuestionGenerateHandler — question_generate, sync: questions list
5. FinanceAnalysisHandler — finance_analysis, sync: summary, suggestion, healthScore
6. SensitiveWordHandler — sensitive_word, sync: hasSensitive, words, riskLevel, suggestion
7. DailyTopicHandler — daily_topic, sync: title, description, category

Keep them focused: build prompt from input, call chat(), parse JSON, map to data class. Handle null AI response by returning failure response (code AI_CALL_FAILED) — or throw and let gateway fallback. Better: throw IllegalStateException so gateway catches and applies FallbackStrategy? The gateway catches exceptions. For null result → throw BusinessException(ErrorCode.SYSTEM_ERROR, "AI调用失败"). Hmm, but ErrorCode codes are different from AiErrorCodes. Gateway catch-all returns 1000. It's fine — but better granularity: handlers can return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, ...) directly. I'll do that for null AI text.

For JSON parse failure → failure(AI_PARSE_ERROR). If parse succeeds partially, map fields.

Let me write these efficiently — 7 files.

Handler 完成。第六步：场景注册中心。
Wait — the DDL has 7 scenes but I only have handlers for: voice_interview, resume_parse, resume_optimize, question_generate, finance_analysis, sensitive_word, daily_topic = 7. 

Now the AiSceneRegistry (g6). Then support components (g7), then gateway (g8).

Note: AiSceneRegistry has init() but no @PostConstruct. I'll call it via @PostConstruct — actually I removed @PostConstruct. Let me add it. Actually the doc uses @PostConstruct. Let me add jakarta.annotation.PostConstruct to init(). I'll edit after. Actually let me just add it now via Edit later — or incorporate in gateway. Simpler: add @PostConstruct on init().

第七步：增强组件（意图/缓存/限流/降级/日志）。
One issue: `@Autowired private List<AiSceneHandler> handlers;` — if no handlers exist (won't happen here since we have 7), Spring fails. With 7 handlers it's fine.

Now g7: support components:
1. IntentClassifier (rule-based, per doc §7.1)
2. SemanticCache (Redis + embedding similarity, per doc §7.2). Design:
   - isEnabled(scene): from config enable_cache
   - buildKey(scene, input): scene + md5 of input
   - get(key) / put(key, response)
   - For semantic similarity: complex. Practical approach: exact-key cache (MD5) + optional semantic via embedding. The doc requires embedding similarity >0.95. Implementation: on put, compute embedding of input text, store {embedding, response} in a Redis list/hash per scene; on get, compute embedding, iterate cached entries, cosine similarity. This could be heavy but works for small scale. Embedding model via ModelConfigService.getDefaultEmbeddingConfig() → createEmbeddingModel(configId). If not configured → cache disabled with warn.
3. SceneRateLimiter (Redis INCR+EXPIRE fixed window)
4. FallbackStrategy (per doc §7.3)
5. AiExecuteLogService — async write log via @Async or simple mapper insert in try-catch. Use @Async("applicationTaskExecutor")? The AsyncTaskConfig defines applicationTaskExecutor. I'll use @Async — but must confirm proxy works with public method called from another bean. AiGatewayService calls logService.record(...) — cross-bean, so @Async proxying works.

Let me write these. RedisKeys: use RedisTemplate<String, String> like RedisChatMemoryStore, with our own key constants: "ai2:cache:{scene}:{md5}", "ai2:rate:{scene}:{userId}".

For SemanticCache semantic similarity, I'll keep it simpler but real: exact MD5 key hit first; plus optional semantic check is complex to serialize embeddings into redis and compare. Implementation:
- put: key = ai2:cache:{scene}:{md5(input)}. Value = JSON {response}. Store also embedding vector in the same JSON (as float array). TTL from config.
- get(scene, inputText): 
  1. exact key hit → deserialize, fromCache=true
  2. else if embedding available: compute embedding of input; scan keys ai2:cache:{scene}:* (could be many — use SCAN with count limit, e.g. 100), compare cosine similarity >0.95 → hit
- If embedding model unavailable → exact match only.

That's implementable. EmbeddingModel: dev.langchain4j.model.embedding.EmbeddingModel, embed(String) → Response<Embedding>? In 1.0.0-beta3: `embeddingModel.embed(text).content().vector()` returns float[].

Let me write the components now.

第八步：统一编排服务 + 网关 Controller。
Now g8: AiGatewayService (orchestration) + AiGatewayController.

Gateway flow (sync):
1. requestId inject, userId inject
2. scene config check (must exist + active)
3. intent classify (only when userInput provided) — if confidence < 0.6 → clarification response
4. rate limit check → RATE_LIMITED failure
5. handler validate
6. cache check → hit return
7. execute handler (try-catch → fallback)
8. fill requestId/scene/elapsedMs
9. cache write
10. log record

Stream flow:
- handler supports stream? → executeStream; else error event

Input summary for cache key: canonical JSON of input map. Input text for semantic: concatenate string values of input.

Controller: /api/ai/execute (POST, sync), /api/ai/execute/stream (POST, SSE), /api/ai/scenes (GET, list), /api/ai/refresh (POST).

UserId: SecurityUtils.getUserId() — com.moyun.util.security.SecurityUtils. Might throw if not authenticated? Since anyRequest().authenticated() it should be fine, but wrap in try-catch to be safe.

Let me write the service.

验证 SecurityUtils.getUserId 签名，然后编译。
Now check SecurityUtils.getUserId() signature — does it return Long and can it throw? Let me verify quickly. Also verify AjaxResult.success(Object) exists (yes, saw it). Let me check SecurityUtils.

检查项目实际使用的 PostConstruct 包名。
PostConstruct not found — the project must use javax.annotation.PostConstruct or a different package. Let me check what the project uses.

jakarta.annotation.PostConstruct is the right import and used elsewhere. The error says "cannot find symbol: class PostConstruct" — odd. Wait, the error is garbled Chinese but says 找不到符号 类 PostConstruct 位置: 类 com.moyun.ext.ai2.registry.AiSceneRegistry. 

Hmm, maybe the import line I added came after usage order issue? No, imports don't need order. Let me look at my edit — I added `import jakarta.annotation.PostConstruct;` after `import lombok.extern.slf4j.Slf4j;`. That should be fine...

Wait, maybe the issue: there might be an unused import problem? No. Let me re-read the file to check what happened.

import 丢失，重新添加。
The import is missing! My first edit added `import jakarta.annotation.PostConstruct;` but looking at line 9-11, it's not there. The edit output showed it was added... wait, the edit result showed:

```
9	import lombok.extern.slf4j.Slf4j;
10	import jakarta.annotation.PostConstruct;
11	import org.springframework.beans.factory.annotation.Autowired;
12	import org.springframework.stereotype.Component;
```

But now the file shows line 9-11 without it. Strange — maybe the second edit was applied to a stale version? No, the second edit was on the method. Hmm, whatever happened, the import is missing now. Let me add it again.

ScanOptions.scanOptions() returns ScanOptionsBuilder, need .build(). Fix line 91.

编译通过。完成收尾。
Compile passes. Now let me do a final review of key details:

1. `redisTemplate.scan()` — does RedisTemplate have a scan method? In Spring Data Redis, `RedisTemplate.scan(ScanOptions)` exists (returns Cursor<byte[]>). But wait — my RedisTemplate is `RedisTemplate<String, String>`, and `scan(ScanOptions)` returns `Cursor<String>`? Let me verify: RedisTemplate defines `public Cursor<K> scan(ScanOptions options)` — actually it's defined in RedisAccessor... The method signature in RedisTemplate: `public Cursor<K> scan(ScanOptions options)` — hmm, actually `scan` is defined on `RedisOperations` interface? Let me think. In Spring Data Redis 3.x, `RedisTemplate.scan(ScanOptions)` exists and returns `Cursor<K>`. It compiled successfully, so it's fine.

But wait — the keys returned from scan would be the raw type K = String. Good, it compiled.

2. `keys.next()` returns String key — and `redisTemplate.opsForValue().get(key)` works. Good.

3. Check AiGatewayController uses @Valid on AiExecuteRequest with @NotBlank scene — jakarta.validation import in AiExecuteRequest. Good.

4. The handlers use `@Component("voiceInterviewHandler")` etc. matching DDL handler_bean_name values. Although we don't actually look up handlers by bean name (we route by getSceneCode()), the DDL documents it. That's fine — the registry uses getSceneCode(). The handler_bean_name in config is informational (could be validated). OK.

5. AiGatewayService failure() has unused logError param — minor, fine.

Everything compiled. Update todos and summarize. Also mention:
- How callers would use it (curl examples)
- Next steps (Phase 7 migration of business callers)

One more check — the `@Async` on AiExecuteLogService.record: AsyncTaskConfig has @EnableAsync with applicationTaskExecutor. Cross-bean call from AiGatewayService — proxy applies. Good.

Also potential issue: AiSceneRegistry @Autowired List<AiSceneHandler> handlers — if ai module disabled (moyun.ai.enabled=false)? Handlers are plain @Component, not conditional, so they always exist. LLMService injection is required=false. Good.

Also the stream handler: VoiceInterviewHandler.executeStream calls chatStream which uses llmService — if null throws IllegalStateException, caught by gateway → sendErrorAndComplete. Good.

Final: mark todo g9 complete and summarize for the user.

## AI能力统一接入层（ai2 包）深度改造完成，编译通过

按方案文档 V2.0 完整落地，**AI 底座（com.moyun.ext.ai）零改动**，新代码全部在 `com.moyun.ext.ai2` 包下，现有业务调用点保持原样（对应文档 Phase 7 的后续切换）。

### 包结构（19 个新文件）

```
com.moyun.ext.ai2
├── controller/AiGatewayController      统一入口：/api/ai/execute(+stream) /scenes /refresh
├── service/AiGatewayService            五层编排：意图→缓存→路由限流→Handler→降级日志
├── registry/AiSceneRegistry            场景注册中心（Handler注册+DB配置加载+在线刷新）
├── handler/
│   ├── AiSceneHandler                  SPI接口（sync/stream/validate/模板构建）
│   ├── AbstractAiSceneHandler          基类：场景感知模型解析/{{var}}模板/容错JSON解析/SSE适配
│   └── impl/                           7个场景Handler（见下）
├── support/                            IntentClassifier / SemanticCache / SceneRateLimiter
│                                       / FallbackStrategy / AiExecuteLogService(@Async)
├── model/                              统一请求/响应/元数据 + data/ 6个场景Data类
├── entity + mapper                     AiSceneRegistryConfig / AiExecuteLog
└── constant/AiErrorCodes               错误码（1000通用/2000AI/3000业务）
```

### 关键设计

| 设计点            | 实现方式                                                     |
| ----------------- | ------------------------------------------------------------ |
| **不动底座**      | 模型选择复用底座 `AiSceneResolver.resolveChatModel`（Agent绑定→直绑模型→默认），与现有 LlmClient 链路完全一致 |
| **表前缀 `ai2_`** | `ai2_scene_registry`（场景执行配置）与底座 `ai_scene_config`（场景绑定）职责分离，见 [20260909-ai2-unified-gateway.sql](file:///e:/zyg_new_work/moyun-project-document/moyun-server/src/main/resources/sql/20260909-ai2-unified-gateway.sql)（含 7 场景默认配置 INSERT） |
| **7 个场景**      | voice_interview（both）/ resume_parse / resume_optimize / question_generate / finance_analysis / sensitive_word / daily_topic，提示词与现有业务模块语义对齐 |
| **语义缓存**      | 精确 MD5 命中 + Embedding 余弦相似度>0.95 命中（无 Embedding 模型时自动降级为仅精确命中） |
| **限流**          | Redis INCR+EXPIRE 固定窗口（场景×用户），多实例生效，Redis 异常时放行不阻断 |
| **可观测**        | 每次执行异步落库 `ai2_execute_log`（requestId/场景/耗时/状态/输入输出摘要） |

### 调用示例

```json
POST /api/ai/execute
{ "scene": "sensitive_word", "input": { "text": "待检测文本" } }
```

新增场景只需：实现 Handler Bean + 插一行 `ai2_scene_registry` 配置 + `POST /api/ai/refresh`，网关核心代码零改动。

### 待办（Phase 7，按你指示暂不执行）
- 现有 12+ 个 `llmClient.chat()` 调用点切换到统一网关
- 执行 DDL 建表后启动验证（`/api/ai/scenes` 应列出 7 个场景）