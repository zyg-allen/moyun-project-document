package com.moyun.ext.ai.prompt;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 架构感知层
 * 分析用户需求，提供上下文增强
 */
public class ArchitectureAwareness {

    /**
     * 知名项目知识库
     */
    private static final Map<String, ProjectInfo> PROJECT_KNOWLEDGE = new HashMap<>();
    
    static {
        // Dify
        PROJECT_KNOWLEDGE.put("dify", new ProjectInfo(
            "Dify 是一个 LLM 应用开发平台",
            Arrays.asList("API Server", "Web 前端", "Worker", "Sandbox", "PostgreSQL", "Redis", "Weaviate", "Celery"),
            "architecture"
        ));
        
        // Kubernetes
        PROJECT_KNOWLEDGE.put("kubernetes", new ProjectInfo(
            "Kubernetes 是容器编排平台",
            Arrays.asList("API Server", "etcd", "Scheduler", "Controller Manager", "Kubelet", "Kube Proxy"),
            "architecture"
        ));
        PROJECT_KNOWLEDGE.put("k8s", PROJECT_KNOWLEDGE.get("kubernetes"));
        
        // LangChain
        PROJECT_KNOWLEDGE.put("langchain", new ProjectInfo(
            "LangChain 是一个 LLM 应用开发框架",
            Arrays.asList("Application", "LLM Provider", "Vector Store", "Memory", "Tools", "Chains"),
            "architecture"
        ));
        
        // Docker
        PROJECT_KNOWLEDGE.put("docker", new ProjectInfo(
            "Docker 是容器化平台",
            Arrays.asList("Docker Client", "Docker Daemon", "Registry", "Container", "Image", "Network"),
            "architecture"
        ));
        
        // Spring Cloud
        PROJECT_KNOWLEDGE.put("spring cloud", new ProjectInfo(
            "Spring Cloud 是微服务架构框架",
            Arrays.asList("API Gateway", "Eureka/Nacos", "Config Server", "Service A", "Service B", "Feign Client", "Ribbon", "Hystrix"),
            "architecture"
        ));
        PROJECT_KNOWLEDGE.put("springcloud", PROJECT_KNOWLEDGE.get("spring cloud"));
        
        // 电商系统
        PROJECT_KNOWLEDGE.put("电商", new ProjectInfo(
            "电商系统是典型的分布式架构",
            Arrays.asList("用户端", "商家端", "API网关", "用户服务", "商品服务", "订单服务", "支付服务", "库存服务", "MySQL", "Redis", "Elasticsearch", "消息队列"),
            "architecture"
        ));
        PROJECT_KNOWLEDGE.put("ecommerce", PROJECT_KNOWLEDGE.get("电商"));
        
        // 微服务
        PROJECT_KNOWLEDGE.put("微服务", new ProjectInfo(
            "微服务架构将应用拆分为独立服务",
            Arrays.asList("API Gateway", "Service Discovery", "Config Center", "User Service", "Order Service", "Product Service", "Database", "Cache", "Message Queue"),
            "architecture"
        ));
        PROJECT_KNOWLEDGE.put("microservice", PROJECT_KNOWLEDGE.get("微服务"));
        
        // Redis
        PROJECT_KNOWLEDGE.put("redis", new ProjectInfo(
            "Redis 是高性能内存数据库",
            Arrays.asList("Client", "Redis Sentinel", "Master", "Slave 1", "Slave 2", "Redis Cluster"),
            "architecture"
        ));
        
        // Kafka
        PROJECT_KNOWLEDGE.put("kafka", new ProjectInfo(
            "Kafka 是分布式消息队列",
            Arrays.asList("Producer", "Consumer Group", "Broker 1", "Broker 2", "Broker 3", "ZooKeeper", "Topic Partition"),
            "architecture"
        ));
        
        // Elasticsearch
        PROJECT_KNOWLEDGE.put("elasticsearch", new ProjectInfo(
            "Elasticsearch 是分布式搜索引擎",
            Arrays.asList("Client", "Coordinator Node", "Master Node", "Data Node 1", "Data Node 2", "Index", "Shard"),
            "architecture"
        ));
        PROJECT_KNOWLEDGE.put("es", PROJECT_KNOWLEDGE.get("elasticsearch"));
        
        // MySQL
        PROJECT_KNOWLEDGE.put("mysql", new ProjectInfo(
            "MySQL 主从复制架构",
            Arrays.asList("Application", "MySQL Proxy", "Master", "Slave 1", "Slave 2", "MHA/Orchestrator"),
            "architecture"
        ));
        
        // Nginx
        PROJECT_KNOWLEDGE.put("nginx", new ProjectInfo(
            "Nginx 负载均衡架构",
            Arrays.asList("Client", "Nginx LB", "Upstream Server 1", "Upstream Server 2", "Upstream Server 3", "Static Files"),
            "architecture"
        ));
    }

    /**
     * 图表类型配置
     */
    private static final Map<String, DiagramTypeInfo> DIAGRAM_TYPES = new HashMap<>();
    
    static {
        DIAGRAM_TYPES.put("architecture", new DiagramTypeInfo(
            "系统架构图",
            Arrays.asList("架构", "architecture", "系统", "微服务", "组件", "设计"),
            "layered", "DOWN"
        ));
        DIAGRAM_TYPES.put("flowchart", new DiagramTypeInfo(
            "流程图",
            Arrays.asList("流程", "flow", "步骤", "process", "工作流"),
            "layered", "DOWN"
        ));
        DIAGRAM_TYPES.put("aws", new DiagramTypeInfo(
            "AWS架构图",
            Arrays.asList("aws", "amazon", "ec2", "s3", "lambda", "云"),
            "layered", "DOWN"
        ));
        DIAGRAM_TYPES.put("sequence", new DiagramTypeInfo(
            "时序图",
            Arrays.asList("时序", "sequence", "调用链", "交互"),
            "layered", "RIGHT"
        ));
    }

    /**
     * 分析用户请求
     */
    public static AnalysisResult analyze(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return new AnalysisResult();
        }
        
        String lowerMessage = userMessage.toLowerCase();
        AnalysisResult result = new AnalysisResult();
        
        // 1. 检测知名项目
        for (Map.Entry<String, ProjectInfo> entry : PROJECT_KNOWLEDGE.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                result.projectFound = true;
                result.projectName = entry.getKey();
                result.projectInfo = entry.getValue();
                break;
            }
        }
        
        // 2. 推断图表类型
        int maxScore = 0;
        String bestType = "architecture";
        
        for (Map.Entry<String, DiagramTypeInfo> entry : DIAGRAM_TYPES.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue().keywords) {
                if (lowerMessage.contains(keyword.toLowerCase())) {
                    score += keyword.length();
                }
            }
            if (score > maxScore) {
                maxScore = score;
                bestType = entry.getKey();
            }
        }
        result.suggestedType = bestType;
        result.typeInfo = DIAGRAM_TYPES.get(bestType);
        
        // 3. 推断复杂度
        if (lowerMessage.contains("详细") || lowerMessage.contains("完整") || lowerMessage.contains("detailed")) {
            result.complexity = "detailed";
            result.minNodes = 12;
            result.maxNodes = 20;
        } else if (lowerMessage.contains("简单") || lowerMessage.contains("simple")) {
            result.complexity = "simple";
            result.minNodes = 4;
            result.maxNodes = 8;
        } else {
            result.complexity = "medium";
            result.minNodes = 8;
            result.maxNodes = 15;
        }
        
        return result;
    }
    
    /**
     * 生成上下文增强提示
     */
    public static String generateContextPrompt(String userMessage) {
        AnalysisResult analysis = analyze(userMessage);
        StringBuilder sb = new StringBuilder();
        
        // 项目信息
        if (analysis.projectFound) {
            sb.append("## 项目背景\n\n");
            sb.append("这是关于 **").append(analysis.projectName).append("** 的架构图。\n");
            sb.append(analysis.projectInfo.description).append("\n\n");
            sb.append("核心组件包括：").append(String.join("、", analysis.projectInfo.components)).append("。\n\n");
        }
        
        // 复杂度要求
        sb.append("## 复杂度要求\n\n");
        sb.append("- 期望复杂度：").append(analysis.complexity).append("\n");
        sb.append("- 节点数量：").append(analysis.minNodes).append("-").append(analysis.maxNodes).append(" 个\n\n");
        
        // 图表类型建议
        if (analysis.typeInfo != null) {
            sb.append("## 建议类型\n\n");
            sb.append("建议使用 **").append(analysis.typeInfo.name).append("** 类型。\n");
        }
        
        return sb.toString();
    }
    
    // ========== 内部类 ==========
    
    public static class ProjectInfo {
        public final String description;
        public final List<String> components;
        public final String suggestedType;
        
        public ProjectInfo(String description, List<String> components, String suggestedType) {
            this.description = description;
            this.components = components;
            this.suggestedType = suggestedType;
        }
    }
    
    public static class DiagramTypeInfo {
        public final String name;
        public final List<String> keywords;
        public final String layout;
        public final String direction;
        
        public DiagramTypeInfo(String name, List<String> keywords, String layout, String direction) {
            this.name = name;
            this.keywords = keywords;
            this.layout = layout;
            this.direction = direction;
        }
    }
    
    public static class AnalysisResult {
        public boolean projectFound = false;
        public String projectName;
        public ProjectInfo projectInfo;
        public String suggestedType = "architecture";
        public DiagramTypeInfo typeInfo;
        public String complexity = "medium";
        public int minNodes = 8;
        public int maxNodes = 15;
    }
}
