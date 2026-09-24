# AI 网关性能与成本基线

> 阶段三（Token 成本基线与任务型场景缩减）配套文档。基线数据出来后才设定缩减验收目标，不拍脑袋先定数。

## 1. Token 成本基线（3.1，采集日期 2026-09-24）

### 1.1 现状结论

**场景级 token 基线当前无法从历史数据得出**，原因有二：

1. `ai_execute_log` 近 30 天共 88 行（2026-09-14 ~ 09-24），`token_used` 全部为 0、`cost_yuan` 全部为 NULL——开发环境历史调用未回传 token（未接真实计费模型）；
2. 表结构只有合计列 `token_used`，**无 input/output 拆分列**，不满足"区分 input/output"的统计口径。

底座 `ai_token_usage_log` 有真实 token 数据但无场景维度，仅作参考：

| request_type | model | 次数 | input_tokens | output_tokens | total | cost(元) |
|---|---|---|---|---|---|---|
| chat | deepseek-v4-pro | 18 | 19,992 | 9,551 | 29,543 | 0.039094 |
| embedding_document | text-embedding-v3 | 5 | 20,487 | 0 | 20,487 | 0.010245 |
| embedding_query | text-embedding-v3 | 31 | 960 | 0 | 960 | 0.000487 |

### 1.2 基线补齐动作（随 3.2 落地）

- `ai_execute_log` 增列 `input_tokens` / `output_tokens`（DDL 增量 ALTER），`AiExecuteLogService` 写入 AiMetadata 细分 token——此后日志天然满足 input/output 拆分口径；
- 基线采集口径：3.3/3.4（输入截断 + 输出上限）落地**前**，对任务型场景（resume_parse / finance_analysis / resume_optimize）各跑 ≥5 轮真实模型调用，记录 input/output token 分布，作为缩减前基线；
- 缩减目标在上述基线出来后设定（计划示例：任务型场景 input token 减 40-60%）。

### 1.3 各场景调用现状（参考，2026-09-14 ~ 09-24）

| scene_code | success | fail | avg elapsed_ms | max elapsed_ms |
|---|---|---|---|---|
| voice_interview | 35 | 27 | 25,300 | 336,980 |
| default_chat | 17 | 0 | 46,095 | 163,251 |
| finance_analysis | 7 | 0 | 86,771 | 113,569 |
| resume_parse | 2 | 0 | 84,409 | 85,126 |

## 2. 面试 SSE 首字节基线（阶段一配套）

阶段一验收口径：SSE 首字节 P50 劣化 ≤10%、P99 ≤15%（双口径：`ai_execute_log.elapsed_ms` + 前端埋点）。

**状态：待采集**。回滚分支 20 轮 + 主干 20 轮的对照采集需运行真实模型流量，当前开发环境未执行；voice_interview 现有 elapsed_ms 分布见 §1.3（含同步链路，非首字节口径）。上线前（阶段五触发条件）补采。

## 3. 基线维护约定

- 本文件为 AI 网关性能/成本基线唯一记录处，每次采集追加小节并注明日期；
- 老数据不删除，新增对比轮次向下追加。
