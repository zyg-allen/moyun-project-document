package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板节点执行器
 * 
 * <p>使用模板生成文本，支持变量替换和条件渲染</p>
 * <p>线程安全：无状态，继承BaseNodeExecutor</p>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Component
public class TemplateNodeExecutor extends BaseNodeExecutor {

    // 变量替换使用BaseNodeExecutor的replaceVariables方法
    private static final Pattern IF_PATTERN = Pattern.compile("\\{%\\s*if\\s+(\\w+)\\s*%\\}(.+?)\\{%\\s*endif\\s*%\\}", Pattern.DOTALL);
    private static final Pattern FOR_PATTERN = Pattern.compile("\\{%\\s*for\\s+(\\w+)\\s+in\\s+(\\w+)\\s*%\\}(.+?)\\{%\\s*endfor\\s*%\\}", Pattern.DOTALL);

    @Override
    public String getType() {
        return "template";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("模板节点配置为空");
        }

        try {
            String template = (String) config.get("template");
            String outputVariable = (String) config.getOrDefault("outputVariable", "template_output");

            if (template == null || template.isEmpty()) {
                return NodeResult.fail("模板内容为空");
            }

            log.info("📝 模板节点执行: template length={}", template.length());

            // 处理条件语句
            String result = processConditions(template, context);

            // 处理循环语句
            result = processLoops(result, context);

            // 替换变量
            result = replaceVariables(result, context);

            log.info("📝 模板渲染完成: output length={}", result.length());

            context.setVariable(outputVariable, result);

            return NodeResult.success(result);

        } catch (Exception e) {
            log.error("模板渲染失败", e);
            return NodeResult.fail("模板渲染失败: " + e.getMessage());
        }
    }

    /**
     * 处理条件语句 {% if var %}...{% endif %}
     */
    private String processConditions(String template, WorkflowContext context) {
        Matcher matcher = IF_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String content = matcher.group(2);
            Object value = context.getVariable(varName);

            // 判断条件是否为真
            boolean condition = value != null && !"".equals(value.toString())
                    && !"false".equalsIgnoreCase(value.toString())
                    && !"0".equals(value.toString());

            matcher.appendReplacement(result, Matcher.quoteReplacement(condition ? content : ""));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 处理循环语句 {% for item in list %}...{% endfor %}
     */
    private String processLoops(String template, WorkflowContext context) {
        Matcher matcher = FOR_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String itemName = matcher.group(1);
            String listName = matcher.group(2);
            String loopContent = matcher.group(3);

            Object listObj = context.getVariable(listName);
            StringBuilder loopResult = new StringBuilder();

            if (listObj instanceof Iterable) {
                int index = 0;
                for (Object item : (Iterable<?>) listObj) {
                    // 临时设置循环变量
                    context.setVariable(itemName, item);
                    context.setVariable("loop_index", index);

                    String itemResult = replaceVariables(loopContent, context);
                    loopResult.append(itemResult);
                    index++;
                }
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(loopResult.toString()));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
