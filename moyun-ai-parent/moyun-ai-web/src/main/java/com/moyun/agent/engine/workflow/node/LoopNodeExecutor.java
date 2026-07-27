package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.util.JsonUtils;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 列表循环节点执行器
 *
 * <p>对列表数据进行循环处理，每次执行处理一个元素</p>
 * <p>返回 "loop" 句柄继续循环，返回 "done" 句柄结束循环</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class LoopNodeExecutor extends BaseNodeExecutor {

    private static final int MAX_ITERATIONS = 1000;

    @Override
    public String getType() {
        return "loop";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("循环节点配置为空");
        }

        try {
            // 获取配置
            String listVariable = (String) config.get("listVariable");
            String itemVariable = (String) config.getOrDefault("itemVariable", "item");
            String indexVariable = (String) config.getOrDefault("indexVariable", "index");
            String outputVariable = (String) config.getOrDefault("outputVariable", "loop_results");
            int maxIterations = config.containsKey("maxIterations") ? 
                ((Number) config.get("maxIterations")).intValue() : MAX_ITERATIONS;

            if (listVariable == null || listVariable.isEmpty()) {
                return NodeResult.fail("未指定循环列表变量");
            }

            // 内部状态变量名（用于跟踪循环进度）
            String loopStateVar = "_loop_" + node.getId() + "_state";
            String loopListVar = "_loop_" + node.getId() + "_list";
            String loopResultsVar = "_loop_" + node.getId() + "_results";

            // 检查是否是首次进入循环
            Integer currentIndex = (Integer) context.getVariable(loopStateVar);
            List<?> list;
            List<Object> results;

            if (currentIndex == null) {
                // 首次进入循环，初始化
                currentIndex = 0;
                
                // 获取并缓存列表数据
                Object listData = context.getVariable(listVariable);
                if (listData == null) {
                    log.warn("🔄 循环变量为空: {}", listVariable);
                    context.setVariable(outputVariable, new ArrayList<>());
                    return NodeResult.success(new ArrayList<>(), "done");
                }

                list = toList(listData);
                results = new ArrayList<>();
                
                // 缓存列表和结果
                context.setVariable(loopListVar, list);
                context.setVariable(loopResultsVar, results);
                
                log.info("🔄 列表循环开始: list={}, size={}", listVariable, list.size());
            } else {
                // 继续循环
                list = (List<?>) context.getVariable(loopListVar);
                results = (List<Object>) context.getVariable(loopResultsVar);
                
                if (list == null || results == null) {
                    return NodeResult.fail("循环状态丢失");
                }
            }

            // 检查是否超过最大迭代次数或列表结束
            if (currentIndex >= list.size() || currentIndex >= maxIterations) {
                // 循环结束
                log.info("🔄 列表循环结束: 共处理 {} 项", currentIndex);
                
                // 保存最终结果
                context.setVariable(outputVariable, results);
                
                // 清理内部状态
                context.removeVariable(loopStateVar);
                context.removeVariable(loopListVar);
                context.removeVariable(loopResultsVar);
                context.removeVariable(itemVariable);
                context.removeVariable(indexVariable);
                
                return NodeResult.success(results, "done");
            }

            // 设置当前元素和索引
            Object currentItem = list.get(currentIndex);
            context.setVariable(itemVariable, currentItem);
            context.setVariable(indexVariable, currentIndex);
            context.setVariable("loop_total", list.size());
            context.setVariable("loop_is_first", currentIndex == 0);
            context.setVariable("loop_is_last", currentIndex == list.size() - 1);
            
            // 收集结果（将当前元素添加到结果中）
            results.add(currentItem);
            
            // 更新循环计数器
            context.setVariable(loopStateVar, currentIndex + 1);
            
            log.info("🔄 列表循环: index={}/{}, item={}", currentIndex, list.size(), 
                currentItem != null ? currentItem.toString().substring(0, Math.min(50, currentItem.toString().length())) : "null");

            // 返回 loop 句柄，继续执行循环体
            return NodeResult.success(currentItem, "loop");

        } catch (Exception e) {
            log.error("循环节点执行失败", e);
            return NodeResult.fail("循环执行失败: " + e.getMessage());
        }
    }

    /**
     * 将输入转换为列表
     */
    @SuppressWarnings("unchecked")
    private List<?> toList(Object input) {
        if (input instanceof List) {
            return (List<?>) input;
        }
        if (input instanceof Collection) {
            return new ArrayList<>((Collection<?>) input);
        }
        if (input instanceof String) {
            String str = (String) input;
            // 尝试解析 JSON 数组
            if (str.trim().startsWith("[")) {
                List<?> parsedList = JsonUtils.fromJson(str, List.class);
                if (parsedList != null) {
                    return parsedList;
                }
            }
            // 按行分割
            if (str.contains("\n")) {
                return List.of(str.split("\\r?\\n"));
            }
        }
        // 单个元素作为列表
        return List.of(input);
    }
}
