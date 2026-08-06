package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.util.JsonUtils;
import com.moyun.ext.ai.engine.workflow.NodeExecutor;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.util.Map;

/**
 * 代码节点执行器
 *
 * <p>执行JavaScript代码片段</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class CodeNodeExecutor implements NodeExecutor {

    @Override
    public String getType() {
        return "code";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("代码节点配置为空");
        }

        try {
            String code = (String) config.get("code");
            String language = (String) config.getOrDefault("language", "javascript");
            String outputVariable = (String) config.getOrDefault("outputVariable", "code_output");

            if (code == null || code.isEmpty()) {
                return NodeResult.fail("代码为空");
            }

            log.info("💻 代码节点执行: language={}", language);

            Object result;

            if ("javascript".equalsIgnoreCase(language) || "js".equalsIgnoreCase(language)) {
                result = executeJavaScript(code, context);
            } else {
                return NodeResult.fail("不支持的语言: " + language);
            }

            log.info("💻 代码执行结果: {}", result);

            context.setVariable(outputVariable, result);
            return NodeResult.success(result);

        } catch (Exception e) {
            log.error("代码节点执行失败", e);
            return NodeResult.fail("代码执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行JavaScript代码
     */
    private Object executeJavaScript(String code, WorkflowContext context) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");

        if (engine == null) {
            // Java 15+ 没有 nashorn，使用 GraalJS 或简单的变量替换
            return executeSimpleCode(code, context);
        }

        // 绑定变量
        SimpleBindings bindings = new SimpleBindings();
        bindings.putAll(context.getVariables());
        bindings.put("context", context);
        bindings.put("input", context.getInput());

        return engine.eval(code, bindings);
    }

    /**
     * 增强的简单代码执行
     * 
     * <p>支持的操作：</p>
     * <ul>
     *   <li>return 语句</li>
     *   <li>字符串操作：toUpperCase, toLowerCase, trim, split, replace</li>
     *   <li>JSON操作：JSON.parse, JSON.stringify</li>
     *   <li>数组操作：length, join</li>
     *   <li>条件表达式：三元运算符</li>
     * </ul>
     */
    private Object executeSimpleCode(String code, WorkflowContext context) {
        log.info("💻 执行简化代码模式 (Nashorn 不可用)");
        
        // 首先进行变量替换
        String processedCode = replaceVariables(code, context);
        log.debug("变量替换后: {}", processedCode);
        
        // return 语句处理
        if (processedCode.trim().startsWith("return ")) {
            String returnExpr = processedCode.trim().substring(7).trim();
            if (returnExpr.endsWith(";")) {
                returnExpr = returnExpr.substring(0, returnExpr.length() - 1).trim();
            }
            return evaluateExpression(returnExpr, context);
        }
        
        // 多行代码，只处理最后一个 return
        String[] lines = processedCode.split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("return ")) {
                String returnExpr = line.substring(7).trim();
                if (returnExpr.endsWith(";")) {
                    returnExpr = returnExpr.substring(0, returnExpr.length() - 1).trim();
                }
                return evaluateExpression(returnExpr, context);
            }
        }
        
        return processedCode;
    }
    
    /**
     * 替换代码中的变量引用
     */
    private String replaceVariables(String code, WorkflowContext context) {
        String result = code;
        
        // 替换 {{variable}} 格式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{(\\w+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.getVariable(varName);
            String replacement;
            
            if (value == null) {
                replacement = "null";
            } else if (value instanceof String) {
                replacement = "\"" + value.toString().replace("\"", "\\\"") + "\"";
            } else if (value instanceof Number || value instanceof Boolean) {
                replacement = value.toString();
            } else {
                replacement = JsonUtils.toJson(value);
            }
            
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 表达式求值
     */
    private Object evaluateExpression(String expr, WorkflowContext context) {
        expr = expr.trim();
        
        // 字符串字面量
        if ((expr.startsWith("\"") && expr.endsWith("\"")) ||
            (expr.startsWith("'") && expr.endsWith("'"))) {
            return expr.substring(1, expr.length() - 1);
        }
        
        // 数字
        try {
            if (expr.contains(".")) {
                return Double.parseDouble(expr);
            }
            return Long.parseLong(expr);
        } catch (NumberFormatException ignored) {}
        
        // 布尔值
        if ("true".equalsIgnoreCase(expr)) return true;
        if ("false".equalsIgnoreCase(expr)) return false;
        if ("null".equalsIgnoreCase(expr)) return null;
        
        // JSON.parse()
        if (expr.startsWith("JSON.parse(") && expr.endsWith(")")) {
            String jsonStr = expr.substring(11, expr.length() - 1);
            Object evaluated = evaluateExpression(jsonStr, context);
            if (evaluated instanceof String) {
                return JsonUtils.fromJson((String) evaluated, Object.class);
            }
            return evaluated;
        }
        
        // JSON.stringify()
        if (expr.startsWith("JSON.stringify(") && expr.endsWith(")")) {
            String inner = expr.substring(15, expr.length() - 1);
            Object evaluated = evaluateExpression(inner, context);
            return JsonUtils.toJson(evaluated);
        }
        
        // 字符串方法链
        if (expr.contains(".")) {
            return evaluateMethodChain(expr, context);
        }
        
        // 三元运算符
        if (expr.contains("?") && expr.contains(":")) {
            return evaluateTernary(expr, context);
        }
        
        // 简单变量引用
        Object value = context.getVariable(expr);
        if (value != null) {
            return value;
        }
        
        // 数组字面量
        if (expr.startsWith("[") && expr.endsWith("]")) {
            return parseArrayLiteral(expr, context);
        }
        
        return expr;
    }
    
    /**
     * 执行方法链
     */
    private Object evaluateMethodChain(String expr, WorkflowContext context) {
        String[] parts = expr.split("\\.", 2);
        if (parts.length < 2) return expr;
        
        String base = parts[0].trim();
        String method = parts[1].trim();
        
        // 获取基础对象
        Object baseObj = evaluateExpression(base, context);
        
        // 字符串方法
        if (baseObj instanceof String) {
            String str = (String) baseObj;
            
            if (method.equals("toUpperCase()")) return str.toUpperCase();
            if (method.equals("toLowerCase()")) return str.toLowerCase();
            if (method.equals("trim()")) return str.trim();
            if (method.equals("length")) return str.length();
            
            if (method.startsWith("split(") && method.endsWith(")")) {
                String delimiter = method.substring(6, method.length() - 1).replace("\"", "").replace("'", "");
                return java.util.Arrays.asList(str.split(delimiter));
            }
            
            if (method.startsWith("replace(") && method.endsWith(")")) {
                String args = method.substring(8, method.length() - 1);
                String[] argParts = args.split(",");
                if (argParts.length >= 2) {
                    String search = argParts[0].trim().replace("\"", "").replace("'", "");
                    String replacement = argParts[1].trim().replace("\"", "").replace("'", "");
                    return str.replace(search, replacement);
                }
            }
            
            if (method.startsWith("substring(") && method.endsWith(")")) {
                String args = method.substring(10, method.length() - 1);
                String[] argParts = args.split(",");
                int start = Integer.parseInt(argParts[0].trim());
                if (argParts.length >= 2) {
                    int end = Integer.parseInt(argParts[1].trim());
                    return str.substring(start, end);
                }
                return str.substring(start);
            }
        }
        
        // 数组/列表方法
        if (baseObj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) baseObj;
            
            if (method.equals("length")) return list.size();
            if (method.equals("size()")) return list.size();
            
            if (method.startsWith("join(") && method.endsWith(")")) {
                String delimiter = method.substring(5, method.length() - 1).replace("\"", "").replace("'", "");
                return String.join(delimiter, list.stream().map(Object::toString).toArray(String[]::new));
            }
            
            if (method.startsWith("get(") && method.endsWith(")")) {
                int index = Integer.parseInt(method.substring(4, method.length() - 1).trim());
                return list.get(index);
            }
        }
        
        return baseObj;
    }
    
    /**
     * 执行三元运算符
     */
    private Object evaluateTernary(String expr, WorkflowContext context) {
        int qIdx = expr.indexOf('?');
        int cIdx = expr.indexOf(':', qIdx);
        
        if (qIdx < 0 || cIdx < 0) return expr;
        
        String condition = expr.substring(0, qIdx).trim();
        String trueVal = expr.substring(qIdx + 1, cIdx).trim();
        String falseVal = expr.substring(cIdx + 1).trim();
        
        boolean condResult = evaluateCondition(condition, context);
        return evaluateExpression(condResult ? trueVal : falseVal, context);
    }
    
    /**
     * 条件求值
     */
    private boolean evaluateCondition(String condition, WorkflowContext context) {
        // 比较操作
        if (condition.contains("===") || condition.contains("==")) {
            String[] parts = condition.split("===|==");
            if (parts.length == 2) {
                Object left = evaluateExpression(parts[0].trim(), context);
                Object right = evaluateExpression(parts[1].trim(), context);
                return java.util.Objects.equals(left, right);
            }
        }
        
        if (condition.contains("!==") || condition.contains("!=")) {
            String[] parts = condition.split("!==|!=");
            if (parts.length == 2) {
                Object left = evaluateExpression(parts[0].trim(), context);
                Object right = evaluateExpression(parts[1].trim(), context);
                return !java.util.Objects.equals(left, right);
            }
        }
        
        if (condition.contains(">")) {
            String[] parts = condition.split(">");
            if (parts.length == 2) {
                Object left = evaluateExpression(parts[0].trim(), context);
                Object right = evaluateExpression(parts[1].trim(), context);
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() > ((Number) right).doubleValue();
                }
            }
        }
        
        if (condition.contains("<")) {
            String[] parts = condition.split("<");
            if (parts.length == 2) {
                Object left = evaluateExpression(parts[0].trim(), context);
                Object right = evaluateExpression(parts[1].trim(), context);
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() < ((Number) right).doubleValue();
                }
            }
        }
        
        // 布尔值
        Object value = evaluateExpression(condition, context);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return !((String) value).isEmpty();
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        return value != null;
    }
    
    /**
     * 解析数组字面量
     */
    private java.util.List<Object> parseArrayLiteral(String expr, WorkflowContext context) {
        String inner = expr.substring(1, expr.length() - 1).trim();
        if (inner.isEmpty()) return new java.util.ArrayList<>();
        
        java.util.List<Object> result = new java.util.ArrayList<>();
        String[] items = inner.split(",");
        for (String item : items) {
            result.add(evaluateExpression(item.trim(), context));
        }
        return result;
    }
}
