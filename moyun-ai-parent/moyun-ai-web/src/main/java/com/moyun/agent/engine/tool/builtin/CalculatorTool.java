package com.moyun.agent.engine.tool.builtin;

import com.moyun.agent.engine.tool.ToolContext;
import com.moyun.agent.engine.tool.ToolExecutor;
import com.moyun.agent.engine.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数学计算工具
 *
 * <p>执行数学计算，支持加减乘除、幂运算、开方、三角函数等</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class CalculatorTool implements ToolExecutor {

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "执行数学计算，支持加减乘除、幂运算、开方、三角函数等";
    }

    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "expression": {
                        "type": "string",
                        "description": "数学表达式，如(1+2)*3、sqrt(16)、sin(30)"
                    }
                },
                "required": ["expression"]
            }
            """;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> params) {
        String expression = (String) params.get("expression");

        if (expression == null || expression.trim().isEmpty()) {
            return ToolResult.fail("表达式不能为空");
        }

        try {
            // 预处理表达式
            String processedExpr = preprocessExpression(expression);

            // 尝试使用JavaScript引擎计算
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");

            // 如果没有JavaScript引擎（JDK 15+），尝试使用Nashorn
            if (engine == null) {
                engine = manager.getEngineByName("nashorn");
            }
            // 再尝试 graal.js
            if (engine == null) {
                engine = manager.getEngineByName("graal.js");
            }

            if (engine == null) {
                // 如果没有脚本引擎，使用简单计算
                log.warn("⚠️ 无可用脚本引擎，使用简单计算模式");
                return calculateSimple(expression);
            }

            Object result = engine.eval(processedExpr);

            // 格式化结果
            String formattedResult;
            if (result instanceof Number) {
                double d = ((Number) result).doubleValue();
                if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < Long.MAX_VALUE) {
                    formattedResult = String.valueOf((long) d);
                } else {
                    formattedResult = new BigDecimal(d)
                            .setScale(6, RoundingMode.HALF_UP)
                            .stripTrailingZeros()
                            .toPlainString();
                }
            } else {
                formattedResult = String.valueOf(result);
            }

            String output = String.format("%s = %s", expression, formattedResult);
            log.info("🧮 计算结果: {}", output);

            return ToolResult.success(output, result);

        } catch (Exception e) {
            log.error("计算失败: {}", expression, e);
            return ToolResult.fail("计算失败: " + e.getMessage());
        }
    }

    /**
     * 预处理表达式，将数学函数转换为JavaScript格式
     */
    private String preprocessExpression(String expr) {
        String result = expr.trim();

        // 替换中文括号
        result = result.replace("（", "(").replace("）", ")");

        // 替换乘号和除号
        result = result.replace("×", "*").replace("÷", "/");
        result = result.replace("x", "*").replace("X", "*");

        // 替换幂运算 ^
        result = result.replaceAll("(\\d+)\\^(\\d+)", "Math.pow($1,$2)");

        // 替换数学函数
        result = result.replaceAll("(?i)sqrt\\(", "Math.sqrt(");
        result = result.replaceAll("(?i)sin\\(", "Math.sin(Math.PI/180*");
        result = result.replaceAll("(?i)cos\\(", "Math.cos(Math.PI/180*");
        result = result.replaceAll("(?i)tan\\(", "Math.tan(Math.PI/180*");
        result = result.replaceAll("(?i)log\\(", "Math.log10(");
        result = result.replaceAll("(?i)ln\\(", "Math.log(");
        result = result.replaceAll("(?i)abs\\(", "Math.abs(");
        result = result.replaceAll("(?i)ceil\\(", "Math.ceil(");
        result = result.replaceAll("(?i)floor\\(", "Math.floor(");
        result = result.replaceAll("(?i)round\\(", "Math.round(");

        // 替换常量
        result = result.replaceAll("(?i)\\bpi\\b", "Math.PI");
        result = result.replaceAll("(?i)\\be\\b", "Math.E");

        return result;
    }

    /**
     * 简单计算（备用方案）
     */
    private ToolResult calculateSimple(String expression) {
        try {
            // 只支持简单的四则运算
            Pattern pattern = Pattern.compile("([\\d.]+)\\s*([+\\-*/])\\s*([\\d.]+)");
            Matcher matcher = pattern.matcher(expression);

            if (matcher.find()) {
                double a = Double.parseDouble(matcher.group(1));
                String op = matcher.group(2);
                double b = Double.parseDouble(matcher.group(3));

                double result = switch (op) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    case "/" -> a / b;
                    default -> throw new IllegalArgumentException("不支持的运算符: " + op);
                };

                return ToolResult.success(expression + " = " + result, result);
            }

            return ToolResult.fail("无法解析表达式: " + expression);
        } catch (Exception e) {
            return ToolResult.fail("计算失败: " + e.getMessage());
        }
    }
}
