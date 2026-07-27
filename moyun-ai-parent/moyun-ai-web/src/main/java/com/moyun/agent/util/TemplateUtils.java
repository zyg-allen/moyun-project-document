package com.moyun.agent.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板工具类
 * 
 * <p>提供统一的模板变量替换功能，支持{{variable}}格式</p>
 * 
 * <p>核心功能：</p>
 * <ul>
 *     <li>变量替换：将模板中的{{key}}替换为实际值</li>
 *     <li>安全处理：未找到的变量保持原样或替换为空</li>
 *     <li>嵌套支持：支持对象属性访问（如{{user.name}}）</li>
 * </ul>
 * 
 * <p>线程安全：所有方法都是无状态的，线程安全</p>
 * 
 * @author laomao
 * @since 2025-11-30
 */
public final class TemplateUtils {
    
    /** 变量模式：匹配{{variableName}} */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*\\}\\}");
    
    private TemplateUtils() {
        // 工具类禁止实例化
    }
    
    /**
     * 替换模板中的所有变量
     * 
     * <p>将模板中的{{key}}替换为variables中对应的值</p>
     * <p>如果变量未找到，保持原样（{{key}}）</p>
     * 
     * @param template 模板字符串
     * @param variables 变量Map
     * @return 替换后的字符串
     */
    public static String render(String template, Map<String, Object> variables) {
        return render(template, variables, false);
    }
    
    /**
     * 替换模板中的所有变量（支持未找到变量时替换为空）
     * 
     * @param template 模板字符串
     * @param variables 变量Map
     * @param replaceWithEmptyIfNotFound 如果变量未找到，是否替换为空字符串
     * @return 替换后的字符串
     */
    public static String render(String template, Map<String, Object> variables, boolean replaceWithEmptyIfNotFound) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        
        if (variables == null || variables.isEmpty()) {
            return replaceWithEmptyIfNotFound ? removeAllVariables(template) : template;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = getVariableValue(variableName, variables);
            
            String replacement;
            if (value != null) {
                replacement = Matcher.quoteReplacement(String.valueOf(value));
            } else {
                replacement = replaceWithEmptyIfNotFound ? "" : Matcher.quoteReplacement(matcher.group(0));
            }
            
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 获取变量值（支持嵌套属性）
     * 
     * <p>支持格式：</p>
     * <ul>
     *     <li>简单变量：input</li>
     *     <li>嵌套属性：user.name（需要variables中user是Map）</li>
     * </ul>
     * 
     * @param variableName 变量名
     * @param variables 变量Map
     * @return 变量值，未找到返回null
     */
    @SuppressWarnings("unchecked")
    private static Object getVariableValue(String variableName, Map<String, Object> variables) {
        if (!variableName.contains(".")) {
            // 简单变量
            return variables.get(variableName);
        }
        
        // 嵌套属性
        String[] parts = variableName.split("\\.");
        Object current = variables;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                // 不支持的类型
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * 移除模板中的所有变量占位符
     * 
     * @param template 模板字符串
     * @return 移除变量后的字符串
     */
    public static String removeAllVariables(String template) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        return VARIABLE_PATTERN.matcher(template).replaceAll("");
    }
    
    /**
     * 提取模板中的所有变量名
     * 
     * @param template 模板字符串
     * @return 变量名列表
     */
    public static java.util.List<String> extractVariableNames(String template) {
        java.util.List<String> variables = new java.util.ArrayList<>();
        
        if (template == null || template.isEmpty()) {
            return variables;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!variables.contains(variableName)) {
                variables.add(variableName);
            }
        }
        
        return variables;
    }
    
    /**
     * 检查模板中是否包含变量
     * 
     * @param template 模板字符串
     * @return true表示包含变量
     */
    public static boolean hasVariables(String template) {
        if (template == null || template.isEmpty()) {
            return false;
        }
        return VARIABLE_PATTERN.matcher(template).find();
    }
    
    /**
     * 验证模板中的所有变量是否都有对应的值
     * 
     * @param template 模板字符串
     * @param variables 变量Map
     * @return true表示所有变量都有值
     */
    public static boolean validateVariables(String template, Map<String, Object> variables) {
        java.util.List<String> requiredVars = extractVariableNames(template);
        
        for (String var : requiredVars) {
            Object value = getVariableValue(var, variables);
            if (value == null) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 快速替换单个变量
     * 
     * <p>性能优化：如果只需要替换一个变量，使用此方法更高效</p>
     * 
     * @param template 模板字符串
     * @param variableName 变量名
     * @param value 变量值
     * @return 替换后的字符串
     */
    public static String replaceSingle(String template, String variableName, Object value) {
        if (template == null || variableName == null) {
            return template;
        }
        
        String placeholder = "{{" + variableName + "}}";
        String valueStr = value != null ? String.valueOf(value) : "";
        
        return template.replace(placeholder, valueStr);
    }
}
