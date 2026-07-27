package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.util.JsonUtils;
import com.moyun.agent.engine.workflow.NodeExecutor;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 迭代器节点执行器
 *
 * <p>对列表数据进行迭代处理，支持批量和并行处理</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class IteratorNodeExecutor implements NodeExecutor {

    @Override
    public String getType() {
        return "iterator";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("迭代器节点配置为空");
        }

        try {
            String inputVar = (String) config.getOrDefault("inputVariable", "input");
            String itemVariable = (String) config.getOrDefault("itemVariable", "item");
            String indexVariable = (String) config.getOrDefault("indexVariable", "index");
            String outputVariable = (String) config.getOrDefault("outputVariable", "iterator_results");
            String mode = (String) config.getOrDefault("mode", "sequential"); // sequential, batch, parallel
            int batchSize = config.containsKey("batchSize") ? ((Number) config.get("batchSize")).intValue() : 10;
            int maxIterations = config.containsKey("maxIterations") ? ((Number) config.get("maxIterations")).intValue() : 100;

            Object inputValue = context.getVariable(inputVar);

            log.info("🔄 迭代器执行: mode={}, inputType={}", mode,
                    inputValue != null ? inputValue.getClass().getSimpleName() : "null");

            // 转换为列表
            List<?> items = toList(inputValue);

            if (items.isEmpty()) {
                context.setVariable(outputVariable, new ArrayList<>());
                return NodeResult.success(new ArrayList<>());
            }

            // 限制最大迭代次数
            if (items.size() > maxIterations) {
                items = items.subList(0, maxIterations);
                log.warn("⚠️ 迭代次数超过限制，截断为 {} 条", maxIterations);
            }

            List<Object> results = new ArrayList<>();

            // 设置迭代元数据
            context.setVariable("iterator_total", items.size());
            context.setVariable("iterator_mode", mode);

            if ("batch".equals(mode)) {
                // 批量处理
                for (int i = 0; i < items.size(); i += batchSize) {
                    int endIdx = Math.min(i + batchSize, items.size());
                    List<?> batch = items.subList(i, endIdx);

                    context.setVariable(itemVariable, batch);
                    context.setVariable(indexVariable, i / batchSize);
                    context.setVariable("batch_size", batch.size());

                    results.add(batch);
                }
                context.setVariable("iterator_batch_count", (items.size() + batchSize - 1) / batchSize);
            } else {
                // 顺序处理（默认）
                for (int i = 0; i < items.size(); i++) {
                    context.setVariable(itemVariable, items.get(i));
                    context.setVariable(indexVariable, i);
                    context.setVariable("iterator_is_first", i == 0);
                    context.setVariable("iterator_is_last", i == items.size() - 1);

                    results.add(items.get(i));
                }
            }

            context.setVariable(outputVariable, results);
            context.setVariable("iterator_count", results.size());

            log.info("🔄 迭代器完成: 共 {} 项", results.size());

            return NodeResult.success(results);

        } catch (Exception e) {
            log.error("迭代器执行失败", e);
            return NodeResult.fail("迭代器执行失败: " + e.getMessage());
        }
    }

    /**
     * 转换为列表
     */
    @SuppressWarnings("unchecked")
    private List<?> toList(Object input) {
        if (input == null) {
            return new ArrayList<>();
        }

        if (input instanceof List) {
            return (List<?>) input;
        }

        if (input instanceof Collection) {
            return new ArrayList<>((Collection<?>) input);
        }

        if (input instanceof Object[]) {
            return Arrays.asList((Object[]) input);
        }

        if (input instanceof String) {
            String str = (String) input;
            // 尝试解析JSON数组
            if (str.trim().startsWith("[")) {
                List<?> parsedList = JsonUtils.fromJson(str, List.class);
                if (parsedList != null) {
                    return parsedList;
                }
            }
            // 按行分割
            if (str.contains("\n")) {
                return Arrays.asList(str.split("\\r?\\n"));
            }
        }

        // 单个元素作为列表
        return Collections.singletonList(input);
    }
}
