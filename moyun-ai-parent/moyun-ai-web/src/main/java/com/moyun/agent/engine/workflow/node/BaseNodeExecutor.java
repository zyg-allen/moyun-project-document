package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.engine.workflow.NodeExecutor;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.util.TemplateUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点执行器基类
 * 
 * <p>提供所有节点执行器的公共方法：</p>
 * <ul>
 *     <li>变量替换：统一使用TemplateUtils处理{{variable}}格式</li>
 *     <li>配置获取：安全获取配置项，避免类型转换异常</li>
 *     <li>结果构建：统一的成功/失败结果构建</li>
 * </ul>
 * 
 * <p>线程安全：所有方法都是无状态的，线程安全</p>
 * 
 * @author laomao
 * @since 2025-11-30
 */
public abstract class BaseNodeExecutor implements NodeExecutor {
    
    /**
     * 替换模板中的变量
     * 
     * <p>将{{variableName}}替换为context中的变量值</p>
     * <p>使用TemplateUtils统一处理，支持嵌套属性</p>
     * 
     * @param template 模板字符串
     * @param context 工作流上下文
     * @return 替换后的字符串
     */
    protected String replaceVariables(String template, WorkflowContext context) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        
        if (context == null) {
            return TemplateUtils.removeAllVariables(template);
        }
        
        // 构建变量Map，合并input和variables
        Map<String, Object> allVariables = new HashMap<>();
        
        // 先添加input
        if (context.getInput() != null) {
            allVariables.putAll(context.getInput());
        }
        
        // 再添加variables（会覆盖input中的同名变量）
        if (context.getVariables() != null) {
            allVariables.putAll(context.getVariables());
        }
        
        // 使用TemplateUtils替换，未找到的变量替换为空
        return TemplateUtils.render(template, allVariables, true);
    }
    
    /**
     * 安全获取字符串配置
     * 
     * @param config 配置Map
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    protected String getStringConfig(Map<String, Object> config, String key, String defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * 安全获取整数配置
     * 
     * @param config 配置Map
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    protected Integer getIntConfig(Map<String, Object> config, String key, Integer defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全获取双精度配置
     * 
     * @param config 配置Map
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    protected Double getDoubleConfig(Map<String, Object> config, String key, Double defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全获取布尔配置
     * 
     * @param config 配置Map
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    protected Boolean getBooleanConfig(Map<String, Object> config, String key, Boolean defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        if (value != null) {
            String strValue = value.toString().toLowerCase();
            return "true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue);
        }
        
        return defaultValue;
    }
    
    /**
     * 验证配置不为空
     * 
     * @param config 配置Map
     * @param requiredKeys 必需的配置键
     * @throws IllegalArgumentException 如果配置缺失
     */
    protected void validateConfig(Map<String, Object> config, String... requiredKeys) {
        if (config == null) {
            throw new IllegalArgumentException("节点配置为空");
        }
        
        for (String key : requiredKeys) {
            if (!config.containsKey(key) || config.get(key) == null) {
                throw new IllegalArgumentException("缺少必需配置: " + key);
            }
        }
    }
}
