# 🔧 Lynx AI - 后端技术文档

<div align="center">

**基于 Spring Boot 3 + LangChain4j 的企业级AI应用后端**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.0.0--beta3-blueviolet.svg)](https://github.com/langchain4j/langchain4j)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.11-blue.svg)](https://baomidou.com/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](../LICENSE)

</div>

---

## 📑 目录

- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [核心模块](#-核心模块)
- [配置说明](#-配置说明)
- [启动指南](#-启动指南)
- [API文档](#-api文档)
- [性能指标](#-性能指标)

---

## 🛠 技术栈

### 核心框架
```xml
Spring Boot         3.2.6       # 企业级应用框架
Spring Web MVC      3.2.6       # Web层框架
Spring Data Redis   3.2.6       # Redis集成
LangChain4j         1.0.0-beta3 # AI应用开发框架
```

### 数据存储
```xml
MySQL               8.0+        # 关系型数据库（业务数据）
MyBatis-Plus        3.5.11      # ORM框架
Redis               7.0+        # 缓存、会话、对话记忆
Elasticsearch       8.14.3      # 向量检索 + AI数据分析
MinIO               latest      # 对象存储（文档与图片）
```

### AI相关
```xml
LangChain4j-DashScope  1.0.0-beta3  # 阿里百炼（通义千问）
LangChain4j-OpenAI     1.0.0-beta3  # OpenAI GPT系列
LangChain4j-Ollama     1.0.0-beta3  # Ollama本地模型
```

### 文档处理
```xml
Apache POI          5.2.5       # Office文档（Excel/Word/PPT）
Apache PDFBox       3.0.2       # PDF处理
JodConverter        4.4.6       # LibreOffice文档转换
```

### 安全与工具
```xml
Sa-Token            1.39.0      # 权限认证
Lombok              1.18.30     # 简化Java代码
Jackson             2.17.1      # JSON处理
Hutool              5.8.27      # Java工具集
Knife4j             4.3.0       # API文档
```

---

## 📁 项目结构

```
lynx-ai-parent/
├── lynx-ai-common/                  # 公共模块
│   ├── entity/                     # 实体类
│   │   ├── Agent.java             # 智能体
│   │   ├── KnowledgeLibrary.java  # 知识库
│   │   ├── Workflow.java          # 工作流
│   │   ├── DataSourceConfig.java  # 数据源配置
│   │   └── ...
│   ├── dto/                        # 数据传输对象
│   ├── vo/                         # 视图对象
│   ├── mapper/                     # MyBatis Mapper
│   └── enums/                      # 枚举类
│
└── lynx-ai-web/                     # Web模块
    ├── config/                     # 配置类
    │   ├── ElasticsearchConfig.java      # ES配置
    │   ├── RedisConfig.java              # Redis配置
    │   ├── MinioConfig.java              # MinIO配置
    │   ├── LibreOfficeConfig.java        # LibreOffice配置
    │   └── ...
    │
    ├── controller/                 # 控制器层
    │   ├── AgentController.java          # 智能体API
    │   ├── ChatController.java           # 对话API
    │   ├── KnowledgeLibraryController.java  # 知识库API
    │   ├── WorkflowController.java       # 工作流API
    │   ├── DataAnalysisController.java   # AI数据分析API
    │   └── ...
    │
    ├── service/                    # 服务层
    │   ├── impl/
    │   │   ├── AgentServiceImpl.java
    │   │   ├── DynamicChatServiceImpl.java
    │   │   ├── KnowledgeLibraryServiceImpl.java
    │   │   ├── DataQueryServiceImpl.java      # 数据查询服务
    │   │   ├── SQLGeneratorServiceImpl.java   # SQL生成服务
    │   │   └── ...
    │
    ├── engine/                     # 引擎层（核心）
    │   ├── tool/                  # Function Calling
    │   │   ├── ToolRegistry.java
    │   │   └── builtin/
    │   │       ├── WeatherTool.java
    │   │       ├── CalculatorTool.java
    │   │       ├── DatabaseQueryTool.java
    │   │       └── ...
    │   │
    │   └── workflow/              # 工作流引擎
    │       ├── WorkflowEngine.java
    │       └── node/
    │           ├── LLMNodeExecutor.java
    │           ├── ConditionNodeExecutor.java
    │           └── ...
    │
    ├── store/                      # 存储适配器
    │   ├── ElasticsearchEmbeddingStore.java  # 向量存储
    │   └── RedisChatMemoryStore.java         # 对话记忆
    │
    └── util/                       # 工具类
        ├── SqlUtils.java
        ├── TextProcessingUtils.java
        └── ...
```

---

## 🎯 核心模块

### 1. 智能体管理模块

**核心类：**
- `AgentService` - 智能体业务逻辑
- `AgentController` - REST API
- `Agent` - 智能体实体

**主要功能：**
- 智能体CRUD操作
- 模型配置管理
- RAG参数配置
- 工具和知识库绑定

### 2. 对话服务模块

**核心类：**
- `DynamicChatService` - 对话核心服务
- `ChatController` - 流式对话接口
- `RedisChatMemoryStore` - 分布式记忆

**技术亮点：**
- ✅ 流式输出（Server-Sent Events）
- ✅ 分布式记忆（Redis）
- ✅ RAG自动增强
- ✅ Function Calling集成

### 3. 知识库管理模块

**核心类：**
- `KnowledgeLibraryService` - 知识库服务
- `DocumentSegmentService` - 文档处理服务
- `ElasticsearchEmbeddingStore` - 向量存储

**技术亮点：**
- ✅ 多格式文档支持（PDF/DOCX/TXT/MD/HTML等）
- ✅ 智能分块算法
- ✅ 混合检索（向量+关键词）
- ✅ 重排序优化

### 4. AI数据分析模块 ⭐

**核心类：**
- `DataQueryService` - 智能查询服务
- `SQLGeneratorService` - SQL生成服务
- `DataSourceService` - 数据源管理

**技术亮点：**
- ✅ 自然语言转SQL/DSL
- ✅ 四层安全防护
- ✅ 智能表结构分析
- ✅ Token统计

### 5. 工作流引擎模块

**核心类：**
- `WorkflowEngine` - 工作流执行引擎
- `WorkflowContext` - 执行上下文
- `NodeExecutor` - 节点执行器

**技术亮点：**
- ✅ 拓扑排序执行
- ✅ 变量传递和表达式
- ✅ 条件分支和循环
- ✅ 错误处理和重试

---

## ⚙️ 配置说明

### application.properties

```properties
# 服务端口
server.port=8080

# MySQL 数据库（注意：数据库名是 lynx-ai）
spring.datasource.url=jdbc:mysql://localhost:3306/lynx-ai?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Redis（对话历史）
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=your_password
spring.data.redis.database=0

# Elasticsearch（向量数据库）
elasticsearch.host=localhost
elasticsearch.port=9200
elasticsearch.username=elastic
elasticsearch.password=your_password
elasticsearch.index.vector=ai-vectors

# MinIO 对象存储
minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket.knowledge=knowledge-files
minio.bucket.images=knowledge-images

# LibreOffice 本地转换
libreoffice.home=D:/Program Files/LibreOffice
libreoffice.timeout=120
libreoffice.max-tasks=5

# AI模型配置（阿里百炼）
dashscope.api-key=your_dashscope_api_key

# 或使用OpenAI
# openai.api-key=your_openai_api_key

# RAG 系统级配置
rag.min-recall-count=15
rag.verbose-logging=true
rag.enable-hybrid-search=true
rag.enable-query-expansion=true
rag.bm25-weight=0.3
rag.vector-weight=0.7

# Sa-Token 配置
sa-token.token-name=Authorization
sa-token.timeout=604800
sa-token.active-timeout=7200
```

---

## 🚀 启动指南

### 1. 环境准备

```bash
# 安装JDK 17+
java -version

# 启动MySQL
mysql -u root -p

# 启动Redis
redis-server

# 启动Elasticsearch
./bin/elasticsearch

# 启动MinIO（可选，用于文档存储）
./minio server /data

# 安装LibreOffice（推荐，用于文档转PDF实现原文定位）
# 下载地址：https://www.libreoffice.org/download/download/
```

**可选依赖说明：**
- **MinIO** - 用于存储上传的文档和图片，不使用可跳过
- **LibreOffice** - ⭐推荐安装，用于文档转PDF实现精准原文定位

### 2. 数据库初始化

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE `lynx-ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `lynx-ai`;

# 执行初始化脚本
SOURCE lynx-ai-web/src/main/resources/sql/lynx-ai.sql;
# 或者在命令行直接执行
mysql -u root -p lynx-ai < lynx-ai-web/src/main/resources/sql/lynx-ai.sql
```

### 3. 配置文件

```bash
# 编辑配置文件
vi lynx-ai-web/src/main/resources/application.properties
```

**必填配置：**
- MySQL连接信息（数据库名：lynx-ai）
- Redis连接信息（如果没有密码则留空）
- Elasticsearch连接信息
- AI模型API Key（dashscope.api-key 或 openai.api-key）

**可选配置：**
- MinIO配置（不使用文档上传功能可以注释掉）
- LibreOffice配置（推荐安装，用于文档转PDF实现原文定位）

详细配置说明请查看 `application.properties` 文件中的注释。

### 4. 启动项目

```bash
# 方式1：Maven启动（推荐用于开发）
cd lynx-ai-parent
mvn clean install
cd lynx-ai-web
mvn spring-boot:run

# 方式2：打包运行（推荐用于生产）
cd lynx-ai-parent
mvn clean package -DskipTests
cd lynx-ai-web/target
java -jar lynx-ai-web-1.0.jar

# 方式3：IDE运行
# 直接运行 LynxAiApplication.java 主类
```

### 5. 验证启动

```bash
# 访问Swagger文档
http://localhost:8080/doc.html

# 访问健康检查
http://localhost:8080/actuator/health
```

---

## 📖 API文档

启动项目后，访问 Swagger UI：

```
http://localhost:8080/doc.html
```

主要API分组：
- **智能体管理** - `/api/agent/**`
- **对话服务** - `/api/chat/**`
- **知识库管理** - `/api/knowledge-library/**`
- **工作流管理** - `/api/workflow/**`
- **AI数据分析** - `/api/data-analysis/**`
- **数据源管理** - `/api/datasource/**`
- **Token统计** - `/api/token-usage/**`

---

## 📊 性能指标

| 指标 | 数值 | 说明 |
|------|------|------|
| 对话响应延迟 | < 500ms | P95，不含LLM推理时间 |
| RAG检索延迟 | < 200ms | P95，包含重排序 |
| SQL生成延迟 | < 2s | 包含LLM推理 |
| DSL生成延迟 | < 3s | 包含索引分析 |
| 工作流执行 | < 1s | 简单流程（3-5节点） |
| 并发支持 | 1000+ | 单实例并发对话数 |
| 向量检索QPS | 5000+ | ES集群 |

---

## 🔗 相关链接

- [主文档](../README.md)
- [前端文档](../lynx-ai-ui/README.md)
- [LangChain4j文档](https://docs.langchain4j.dev/)
- [Spring Boot文档](https://spring.io/projects/spring-boot)

---

<div align="center">

**🔧 专注技术实现，打造企业级AI平台！**

Made with ❤️ by Lynx AI Team

</div>
