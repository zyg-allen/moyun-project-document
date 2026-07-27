package com.moyun.agent.service.impl;

import com.moyun.agent.service.DiagramService;
import com.moyun.agent.exception.BusinessException;
import com.moyun.agent.exception.ErrorCode;
import com.moyun.agent.service.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 架构图生成服务实现
 *
 * <p>核心功能：</p>
 * <ul>
 *     <li>分析用户输入，识别系统类型和复杂度</li>
 *     <li>构建专业的 Prompt</li>
 *     <li>调用 LLM 生成架构图 JSON</li>
 *     <li>解析和提取 JSON 数据</li>
 * </ul>
 *
 * @author laomao
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagramServiceImpl implements DiagramService {

    private final LLMService llmService;

    @Override
    public String generateDiagram(String content, String style) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("架构描述不能为空");
        }

        log.info("🎨 开始生成架构图, 输入长度: {}, 风格: {}", content.length(), style);

        // 1. 分析用户输入
        InputAnalysis analysis = analyzeUserInput(content);
        
        // 2. 根据前端传入的风格覆盖复杂度
        if ("enterprise".equals(style)) {
            analysis.complexity = "detailed";
            analysis.needSidebar = true;  // 企业级默认带侧边栏
        } else if ("normal".equals(style)) {
            analysis.complexity = "normal";
        }
        
        log.info("📊 输入分析: 系统类型={}, 复杂度={}, 需要侧边栏={}", 
                analysis.systemType, analysis.complexity, analysis.needSidebar);

        // 3. 构建 Prompt
        String prompt = buildPrompt(content, analysis);

        // 4. 调用 LLM
        String response = llmService.generate(prompt);

        // 5. 提取 JSON
        String jsonResult = extractJson(response);
        
        log.info("✅ 架构图生成成功, JSON长度: {}", jsonResult.length());
        return jsonResult;
    }

    /**
     * 分析用户输入
     */
    private InputAnalysis analyzeUserInput(String content) {
        String text = content.toLowerCase();
        InputAnalysis analysis = new InputAnalysis();

        // 系统类型识别
        if (containsAny(text, "智能体", "agent平台", "agent", "自主agent", "多agent", "ai平台", "llm平台")) {
            analysis.systemType = "agent";
        } else if (containsAny(text, "ai", "llm", "dify", "chatgpt", "gpt", "大模型", "rag", "向量", "对话", "问答", "知识库")) {
            analysis.systemType = "ai";
        } else if (containsAny(text, "电商", "商城", "购物", "订单", "支付", "商品", "库存", "物流")) {
            analysis.systemType = "ecommerce";
        } else if (containsAny(text, "微服务", "分布式", "spring cloud", "dubbo", "k8s", "kubernetes", "容器")) {
            analysis.systemType = "microservice";
        } else if (containsAny(text, "iot", "物联网", "传感器", "设备", "边缘", "mqtt")) {
            analysis.systemType = "iot";
        } else if (containsAny(text, "大数据", "hadoop", "spark", "flink", "数据仓库", "数据湖", "etl")) {
            analysis.systemType = "bigdata";
        } else {
            analysis.systemType = "general";
        }

        // 复杂度识别
        if (containsAny(text, "详细", "完整", "完善", "全面", "技术架构", "企业级", "生产级", "深入")) {
            analysis.complexity = "detailed";
        } else if (containsAny(text, "简单", "简洁", "基础", "入门", "示例", "演示")) {
            analysis.complexity = "simple";
        } else {
            analysis.complexity = "normal";
        }

        // 侧边栏需求
        analysis.needSidebar = containsAny(text, "监控", "日志", "追踪", "告警", "指标", "prometheus", "grafana",
                "治理", "配置", "注册", "nacos", "apollo", "consul", "服务发现");

        return analysis;
    }

    /**
     * 构建 Prompt
     */
    private String buildPrompt(String content, InputAnalysis analysis) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一位资深的系统架构师，请根据以下描述生成一个专业的软件架构图。\n\n");
        sb.append("【用户需求】\n").append(content).append("\n\n");

        // 添加系统类型提示
        sb.append("【系统类型】").append(getSystemTypeName(analysis.systemType)).append("\n\n");

        // JSON 格式规范
        sb.append("【输出格式】请严格按以下 JSON 格式输出，不要输出任何其他内容：\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"type\": \"layered\",  // 图表类型: layered(分层架构) / layered-sidebar(带侧边栏) / org(组织架构) / flow(流程图)\n");
        sb.append("  \"title\": \"系统架构图\",  // 标题\n");
        sb.append("  \"layers\": [  // 层级数组，从上到下排列\n");
        sb.append("    {\n");
        sb.append("      \"id\": \"layer-1\",\n");
        sb.append("      \"name\": \"接入层\",  // 层名称，建议2-4个字\n");
        sb.append("      \"nodes\": [  // 该层的节点\n");
        sb.append("        { \"id\": \"node-1\", \"label\": \"Web端\", \"icon\": \"browser\" },\n");
        sb.append("        { \"id\": \"node-2\", \"label\": \"移动端\", \"icon\": \"mobile\" }\n");
        sb.append("      ],\n");
        sb.append("      \"blocks\": [  // 可选，用于分组显示\n");
        sb.append("        {\n");
        sb.append("          \"name\": \"前端应用\",\n");
        sb.append("          \"color\": \"blue\",  // 颜色: blue/green/orange/purple/cyan/pink/gray\n");
        sb.append("          \"nodes\": [...]\n");
        sb.append("        }\n");
        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("  ],\n");

        if (analysis.needSidebar) {
            sb.append("  \"leftSidebar\": {  // 左侧边栏（监控）\n");
            sb.append("    \"title\": \"可观测性\",\n");
            sb.append("    \"nodes\": [\n");
            sb.append("      { \"id\": \"monitor-1\", \"label\": \"链路追踪\", \"icon\": \"monitor\" }\n");
            sb.append("    ]\n");
            sb.append("  },\n");
            sb.append("  \"rightSidebar\": {  // 右侧边栏（治理）\n");
            sb.append("    \"title\": \"服务治理\",\n");
            sb.append("    \"nodes\": [\n");
            sb.append("      { \"id\": \"gov-1\", \"label\": \"配置中心\", \"icon\": \"config\" }\n");
            sb.append("    ]\n");
            sb.append("  },\n");
        }

        sb.append("  \"edges\": []  // 可选，数据流连线\n");
        sb.append("}\n");
        sb.append("```\n\n");

        // 图标提示
        sb.append("【可用图标】\n");
        sb.append("- 终端: browser, mobile, wechat, terminal\n");
        sb.append("- 网关: gateway, nginx, api\n");
        sb.append("- 安全: auth, security\n");
        sb.append("- 服务: server, service, config, registry\n");
        sb.append("- AI: ai, chat, bot, knowledge, intent, memory, tool\n");
        sb.append("- 存储: database, mysql, redis, cache, elasticsearch, storage, file, oss, mq, kafka\n");
        sb.append("- 监控: monitor, log, alert\n");
        sb.append("- 容器: docker, k8s, cloud\n\n");

        // 专业建议
        sb.append("【专业建议】\n");
        sb.append("1. 层级数量建议 4-6 层，从用户接入到基础设施\n");
        sb.append("2. 每层节点 3-8 个为宜，过多请使用 blocks 分组\n");
        sb.append("3. 节点标签简洁专业，2-6 个字最佳\n");
        sb.append("4. 合理使用图标增强可读性\n");
        sb.append("5. 颜色用于区分模块类型\n\n");

        // 复杂度调整
        if ("detailed".equals(analysis.complexity)) {
            sb.append("【企业级模式】这是企业级架构图，请遵循以下要求：\n");
            sb.append("1. 必须包含 6-8 层完整架构（用户接入层→网关层→业务服务层→能力层→数据层→基础设施层）\n");
            sb.append("2. 每层 5-8 个节点，使用真实技术栈名称（如 Nginx、Kong、Nacos、Redis Cluster）\n");
            sb.append("3. 使用 blocks 对复杂层按功能域分组\n");
            sb.append("4. 必须包含左侧边栏（监控体系：Prometheus、Grafana、ELK）\n");
            sb.append("5. 必须包含右侧边栏（服务治理：配置中心、注册中心、链路追踪）\n");
            sb.append("6. 体现生产级别的技术深度和专业性\n");
        } else if ("simple".equals(analysis.complexity)) {
            sb.append("【简洁模式】请生成简洁架构：3-4 层，每层 2-3 个节点，无侧边栏\n");
        } else {
            sb.append("【普通模式】请生成标准架构：4-5 层，每层 3-4 个节点\n");
        }

        sb.append("\n请直接输出 JSON，不要添加任何解释文字。");

        return sb.toString();
    }

    /**
     * 从 LLM 响应中提取 JSON
     */
    private String extractJson(String response) {
        if (!StringUtils.hasText(response)) {
            throw new BusinessException(ErrorCode.CHAT_FAILED, "LLM 返回为空");
        }

        // 尝试提取 markdown 代码块中的 JSON
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher matcher = codeBlockPattern.matcher(response);
        if (matcher.find()) {
            String json = matcher.group(1).trim();
            validateJson(json);
            return json;
        }

        // 尝试直接提取 JSON 对象
        Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}");
        matcher = jsonPattern.matcher(response);
        if (matcher.find()) {
            String json = matcher.group().trim();
            validateJson(json);
            return json;
        }

        throw new BusinessException(ErrorCode.CHAT_FAILED, "无法从 LLM 响应中提取有效的 JSON");
    }

    /**
     * 简单验证 JSON 格式
     */
    private void validateJson(String json) {
        if (!json.contains("\"layers\"")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "JSON 缺少 layers 字段");
        }
        // 基本的括号匹配检查
        int braceCount = 0;
        for (char c : json.toCharArray()) {
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
        }
        if (braceCount != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "JSON 格式不完整");
        }
    }

    /**
     * 检查文本是否包含任意关键词
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取系统类型名称
     */
    private String getSystemTypeName(String type) {
        switch (type) {
            case "agent": return "AI智能体平台";
            case "ai": return "AI/LLM系统";
            case "ecommerce": return "电商系统";
            case "microservice": return "微服务架构";
            case "iot": return "物联网系统";
            case "bigdata": return "大数据系统";
            default: return "通用架构";
        }
    }

    /**
     * 输入分析结果
     */
    private static class InputAnalysis {
        String systemType = "general";
        String complexity = "normal";
        boolean needSidebar = false;
    }
}
