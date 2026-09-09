package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.LedgerSceneData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI财务分析场景Handler（scene = finance_analysis）
 *
 * <p>职责：记账流水统计 + 用户画像 → 财务分析报告（综述/风险/建议/健康评分）。
 * 与业务模块 LedgerAiAnalysisServiceImpl 的提示词语义对齐。</p>
 *
 * <p>输入参数：ledgerData(流水统计文本或JSON)、window(统计窗口，可选)、
 * userProfile(用户画像，可选)</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component("financeAnalysisHandler")
public class FinanceAnalysisHandler extends AbstractAiSceneHandler {

    @Override
    public String getSceneCode() {
        return "finance_analysis";
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request) {
        // v11.43 业务透传模式：业务侧（如 LedgerAiAnalysisServiceImpl）已完成指标聚合与提示词构建，
        // input.prompt 携带完整提示词 → 直接对话返回综述文本（网关的限流/缓存/日志/降级照常生效）
        String bizPrompt = getInputString(request, "prompt");
        if (bizPrompt != null && !bizPrompt.isBlank()) {
            String system = "你是一位专业、友善的个人财务顾问。请根据用户提供的记账数据，输出中文财务综述（现状概述/问题亮点/行动建议，口语化、引用数据、300字以内、无Markdown标记）。";
            String raw = chat(getSceneCode(), system, bizPrompt);
            if (raw == null || raw.isBlank()) {
                return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
            }
            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("summary", raw.trim());
            return AiExecuteResponse.success(data);
        }

        // 通用结构化模式：原始流水数据 → 结构化财务分析 JSON
        String ledgerData = requireInputString(request, "ledgerData");
        String window = getInputString(request, "window");
        String userProfile = getInputString(request, "userProfile");

        String systemPrompt = """
                你是专业个人财务分析师。基于记账数据分析财务状况，只输出 JSON：
                {"summary": "财务状况综述(3-5句)",
                 "healthScore": 0到100整数（财务健康评分）,
                 "categoryExpenses": [{"category":"分类","amount":金额,"ratio":占比百分比}],
                 "suggestion": "具体的优化建议(2-4条，可执行)"}
                数据不足时基于已有数据客观分析，不臆造。禁止输出 JSON 以外内容。""";

        StringBuilder user = new StringBuilder("记账数据");
        if (window != null && !window.isBlank()) {
            user.append("（").append(window).append("）");
        }
        user.append("：\n").append(ledgerData);
        if (userProfile != null && !userProfile.isBlank()) {
            user.append("\n\n用户画像：").append(userProfile);
        }

        String raw = chat(getSceneCode(), systemPrompt, user.toString());
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseJsonMap(raw);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "财务分析结果解析失败");
        }

        LedgerSceneData data = new LedgerSceneData();
        data.setSummary(str(parsed, "summary"));
        data.setSuggestion(str(parsed, "suggestion"));
        if (parsed.get("healthScore") instanceof Number number) {
            data.setHealthScore(number.intValue());
        }
        if (parsed.get("categoryExpenses") instanceof List<?> categories) {
            List<LedgerSceneData.CategoryExpense> expenses = new ArrayList<>();
            for (Object item : categories) {
                if (item instanceof Map<?, ?> c) {
                    LedgerSceneData.CategoryExpense ce = new LedgerSceneData.CategoryExpense();
                    ce.setCategory(str(c, "category"));
                    if (c.get("amount") instanceof Number amount) {
                        ce.setAmount(amount.doubleValue());
                    }
                    if (c.get("ratio") instanceof Number ratio) {
                        ce.setRatio(ratio.doubleValue());
                    }
                    expenses.add(ce);
                }
            }
            data.setCategoryExpenses(expenses);
        }
        return AiExecuteResponse.success(data);
    }

    private String str(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
