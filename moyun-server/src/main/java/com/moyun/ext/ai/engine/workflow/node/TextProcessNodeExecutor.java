package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.util.JsonUtils;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本处理节点执行器
 *
 * <p>支持多种文本处理操作：分割、合并、替换、提取、截取等</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class TextProcessNodeExecutor extends BaseNodeExecutor {

    @Override
    public String getType() {
        return "text";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("文本处理节点配置为空");
        }

        try {
            String operation = (String) config.getOrDefault("operation", "concat");
            String inputVar = (String) config.getOrDefault("inputVariable", "input");
            String outputVariable = (String) config.getOrDefault("outputVariable", "text_output");

            Object inputValue = context.getVariable(inputVar);
            String input = inputValue != null ? inputValue.toString() : "";

            log.info("文本处理: operation={}, input length={}", operation, input.length());

            Object result;

            switch (operation.toLowerCase()) {
                case "split":
                    result = doSplit(input, config);
                    break;
                case "concat":
                case "join":
                    result = doConcat(config, context);
                    break;
                case "replace":
                    result = doReplace(input, config);
                    break;
                case "extract":
                    result = doExtract(input, config);
                    break;
                case "substring":
                case "slice":
                    result = doSubstring(input, config);
                    break;
                case "trim":
                    result = input.trim();
                    break;
                case "uppercase":
                    result = input.toUpperCase();
                    break;
                case "lowercase":
                    result = input.toLowerCase();
                    break;
                case "length":
                    result = input.length();
                    break;
                case "lines":
                    result = Arrays.asList(input.split("\\r?\\n"));
                    break;
                case "json_parse":
                    result = JsonUtils.fromJson(input, Object.class);
                    if (result == null) {
                        return NodeResult.fail("解析JSON失败");
                    }
                    break;
                case "json_stringify":
                    result = JsonUtils.toJson(inputValue);
                    if (result == null) {
                        return NodeResult.fail("序列化JSON失败");
                    }
                    break;
                case "format":
                    result = doFormat(config, context);
                    break;
                default:
                    return NodeResult.fail("不支持的操作: " + operation);
            }

            log.info("文本处理完成: result type={}", result.getClass().getSimpleName());

            context.setVariable(outputVariable, result);

            return NodeResult.success(result);

        } catch (Exception e) {
            log.error("文本处理失败", e);
            return NodeResult.fail("文本处理失败: " + e.getMessage());
        }
    }

    /**
     * 分割文本
     */
    private List<String> doSplit(String input, Map<String, Object> config) {
        String separator = (String) config.getOrDefault("separator", "\n");
        int limit = config.containsKey("limit") ? ((Number) config.get("limit")).intValue() : -1;

        if (limit > 0) {
            return Arrays.asList(input.split(separator, limit));
        }
        return Arrays.asList(input.split(separator));
    }

    /**
     * 合并文本
     */
    private String doConcat(Map<String, Object> config, WorkflowContext context) {
        @SuppressWarnings("unchecked")
        List<String> variables = (List<String>) config.getOrDefault("variables", new ArrayList<>());
        String separator = (String) config.getOrDefault("separator", "");
        
        List<String> values = new ArrayList<>();
        for (String varName : variables) {
            Object value = context.getVariable(varName);
            if (value != null) {
                values.add(value.toString());
            }
        }
        
        return String.join(separator, values);
    }
    
    /**
     * 替换文本
     */
    private String doReplace(String input, Map<String, Object> config) {
        String pattern = (String) config.get("pattern");
        String replacement = (String) config.getOrDefault("replacement", "");
        boolean isRegex = (boolean) config.getOrDefault("regex", false);
        
        if (pattern == null) {
            return input;
        }
        
        if (isRegex) {
            return input.replaceAll(pattern, replacement);
        } else {
            return input.replace(pattern, replacement);
        }
    }
    
    /**
     * 提取文本（正则）
     */
    private Object doExtract(String input, Map<String, Object> config) {
        String pattern = (String) config.get("pattern");
        if (pattern == null) {
            return input;
        }
        
        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(input);
        
        boolean extractAll = (boolean) config.getOrDefault("extractAll", false);
        
        if (extractAll) {
            List<String> matches = new ArrayList<>();
            while (matcher.find()) {
                matches.add(matcher.group());
            }
            return matches;
        } else {
            if (matcher.find()) {
                return matcher.group();
            }
            return "";
        }
    }
    
    /**
     * 截取文本
     */
    private String doSubstring(String input, Map<String, Object> config) {
        int start = config.containsKey("start") ? ((Number) config.get("start")).intValue() : 0;
        int end = config.containsKey("end") ? ((Number) config.get("end")).intValue() : input.length();
        
        start = Math.max(0, start);
        end = Math.min(input.length(), end);
        
        if (start >= end) {
            return "";
        }
        
        return input.substring(start, end);
    }
    
    /**
     * 格式化文本（模板替换）
     */
    private String doFormat(Map<String, Object> config, WorkflowContext context) {
        String template = (String) config.getOrDefault("template", "");
        return replaceVariables(template, context);
    }
}
