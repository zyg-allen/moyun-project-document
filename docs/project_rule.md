
# 墨韵·智库项目开发规范指南

为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下项目规范。

## 一、项目基本信息

| 项目 | 内容 |
|---|---|
| **项目名称** | moyun-project-document |
| **项目描述** | 墨韵·智库后端服务及前端应用 |
| **代码作者** | 19987 |
| **操作系统** | Windows 11 |
| **工作目录** | `D:\zyg_new_work\moyun-project-document` |
| **后端目录** | `D:\zyg_new_work\moyun-project-document\moyun-server` |
| **后端构建工具** | Maven |
| **后端主框架** | Spring Boot 3.3.2 |
| **Java 版本** | JDK 21.0.9 |
| **默认服务端口** | `8080` |
| **默认 Spring Profile** | `dev` |
| **字符编码** | UTF-8 |
| **时区** | `Asia/Shanghai` |

所有命令默认在以下目录执行：

```text
D:\zyg_new_work\moyun-project-document
```

后端 Maven 命令默认在以下目录执行：

```text
D:\zyg_new_work\moyun-project-document\moyun-server
```

## 二、项目目录结构

当前项目主要由管理端、门户端和后端服务组成：

```text
moyun-project-document/
├── .trae/
│   └── rules/                         # 项目规则文件
├── docs/                              # 项目文档
├── logs/                              # 日志文件
├── moyun-admin-vue/                   # 后台管理前端
│   ├── html/
│   ├── public/
│   ├── src/
│   │   ├── api/                       # 后台接口
│   │   ├── assets/                    # 静态资源
│   │   ├── components/                # 通用组件
│   │   ├── constants/
│   │   ├── directive/
│   │   ├── layout/
│   │   ├── plugins/
│   │   ├── router/
│   │   ├── store/
│   │   ├── utils/
│   │   └── views/                     # 页面模块
│   │       ├── ai/
│   │       ├── cms/
│   │       ├── monitor/
│   │       ├── portal/
│   │       ├── system/
│   │       └── tool/
│   └── vite/
│       └── plugins/
├── moyun-portal/                      # 门户前端
│   ├── public/
│   └── src/
│       ├── api/
│       ├── assets/
│       ├── components/
│       ├── composables/
│       ├── data/
│       ├── lib/
│       ├── pages/
│       │   ├── interview/
│       │   ├── learn/
│       │   ├── reading/
│       │   └── tools/
│       ├── router/
│       ├── stores/
│       ├── types/
│       └── utils/
└── moyun-server/                      # Spring Boot 后端服务
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/moyun/
        │   │   ├── common/             # 公共注解、常量、异常、过滤器、配置
        │   │   ├── core/               # 核心框架、数据源、MVC、安全、基础模型
        │   │   ├── ext/                # 扩展业务模块
        │   │   │   ├── ai/              # AI、知识库、工作流、智能对话
        │   │   │   ├── cms/             # 内容管理
        │   │   │   ├── file/            # 文件管理
        │   │   │   ├── generator/       # 代码生成
        │   │   │   └── job/             # 定时任务
        │   │   ├── portal/              # 门户业务
        │   │   ├── system/              # 系统管理
        │   │   └── util/                # 公共工具类
        │   └── resources/
        │       ├── fonts/
        │       ├── i18n/                # 国际化资源
        │       ├── mapper/              # MyBatis XML 映射文件
        │       ├── sql/
        │       ├── static/
        │       └── vm/                  # Velocity 代码生成模板
        └── test/
            └── java/com/moyun/          # 测试代码
```

### 后端业务模块通用结构

新增或扩展业务模块时，优先遵循以下结构：

```text
module/
├── config/          # 模块配置
├── controller/      # HTTP 接口
├── domain/
│   ├── dto/         # 数据传输对象
│   ├── entity/      # 数据库实体
│   ├── model/       # 业务模型
│   ├── query/       # 查询参数
│   └── vo/          # 返回视图对象
├── enums/           # 枚举
├── exception/       # 模块异常
├── mapper/          # MyBatis-Plus Mapper
├── service/         # 业务接口
│   └── impl/        # 业务实现
└── util/            # 模块工具类
```

## 三、技术栈与版本要求

### 3.1 核心技术栈

- **Java**：JDK 21.0.9
- **Spring Boot**：3.3.2
- **Spring Framework**：使用 Spring Boot 3.3.2 管理的兼容版本
- **构建工具**：Apache Maven
- **数据库访问**：MyBatis-Plus 3.5.11
- **关系型数据库**：
  - MySQL：生产环境
  - H2：开发和测试环境
- **非关系型数据库**：MongoDB
- **缓存**：Redis
- **连接池**：Druid 1.2.23
- **代码简化工具**：Lombok 1.18.42
- **定时任务**：Quartz
- **接口文档**：Knife4j OpenAPI 3 4.4.0
- **认证授权**：Spring Security 6.3.1、JWT 0.12.3
- **AI 框架**：LangChain4j 1.0.0-beta3
- **表达式引擎**：Aviator 5.3.3
- **对象存储**：MinIO 8.5.12

### 3.2 主要依赖版本

| 依赖 | 版本 |
|---|---:|
| MyBatis-Plus | 3.5.11 |
| Lombok | 1.18.42 |
| Hutool | 5.8.44 |
| Druid | 1.2.23 |
| LangChain4j | 1.0.0-beta3 |
| JJWT | 0.12.3 |
| Spring Security | 6.3.1 |
| Apache POI | 5.2.5 |
| FastJSON2 | 2.0.53 |
| PDFBox | 3.0.3 |
| Knife4j | 4.4.0 |
| Elasticsearch Java Client | 8.14.3 |
| Docx4j | 11.4.9 |
| JodConverter | 4.4.6 |
| Jsoup | 1.17.2 |
| OpenCSV | 5.9 |
| OSHI | 6.6.5 |
| Quartz | Spring Boot 管理版本 |

版本应优先统一在 `moyun-server/pom.xml` 的 `<properties>` 中管理，禁止在业务代码中随意引入未验证版本。

## 四、构建与运行规范

### 4.1 Maven 构建

在后端目录执行：

```powershell
cd D:\zyg_new_work\moyun-project-document\moyun-server
mvn clean compile
```

执行测试：

```powershell
mvn test
```

打包：

```powershell
mvn clean package
```

跳过测试打包仅允许用于明确的本地调试场景：

```powershell
mvn clean package -DskipTests
```

### 4.2 构建配置要求

- 使用 `spring-boot-maven-plugin` 进行 Spring Boot 打包。
- 使用 `maven-compiler-plugin` 配置 Lombok 注解处理器。
- Java 编译目标必须保持为 Java 21。
- 源码编码必须为 UTF-8。
- 不得提交本地构建产物、临时文件或敏感配置。
- Maven 依赖版本应保持可追溯，新增依赖必须说明用途。

## 五、后端分层与模块规范

### 5.1 Controller 层

Controller 负责：

- 接收和处理 HTTP 请求。
- 定义接口路径、请求参数和响应结构。
- 使用 `@Valid` 或 `@Validated` 完成参数校验。
- 调用 Service 层完成业务处理。

约束：

- 不得在 Controller 中直接访问数据库。
- 不得编写复杂业务逻辑。
- 不得直接返回数据库 Entity，优先返回 VO 或统一响应对象。
- 文件上传接口必须校验文件类型、文件大小和文件名。
- SSE、WebSocket 等流式接口应明确连接生命周期和异常处理。

### 5.2 Service 层

Service 负责：

- 实现核心业务逻辑。
- 完成业务校验、事务控制和权限判断。
- 组合多个 Mapper 或外部服务调用。
- 对外返回 DTO、VO 或业务模型。

约束：

- 事务注解 `@Transactional` 原则上只用于 Service 层。
- Service 接口放在 `service` 包中，实现类放在 `service.impl` 包中。
- 避免在循环中频繁开启或提交事务。
- 外部 AI、文件、邮件、对象存储等调用应设置超时、异常处理和必要的重试策略。
- 不得将 Controller、Mapper 逻辑复制到 Service 中。

### 5.3 Mapper 与数据访问层

本项目以 **MyBatis-Plus** 为主要关系型数据库访问方案，不使用基础规则中的 JPA 作为默认持久化框架。

约束：

- Mapper 优先继承 `BaseMapper<T>` 或项目已有的统一 Mapper。
- 复杂 SQL 使用 XML Mapper，文件统一放置在：

```text
moyun-server/src/main/resources/mapper/
```

- Mapper XML 按业务模块划分目录，例如：
  - `mapper/system`
  - `mapper/portal`
  - `mapper/generator`
  - `mapper/quartz`
- 禁止手动拼接用户输入形成 SQL。
- 优先使用 MyBatis-Plus 条件构造器。
- 分页查询使用 MyBatis-Plus 分页能力及已引入的 `mybatis-plus-jsqlparser`。
- 注意避免 N+1 查询、无条件全表查询和循环单条查询。
- SQL 字段、表名和参数命名应与现有数据库规范保持一致。
- 数据库实体不得直接作为前端响应对象。

### 5.4 Entity、DTO、Query、VO、Model

| 类型 | 用途 |
|---|---|
| `Entity` / `DO` | 数据库表映射对象 |
| `DTO` | 接收或传输业务数据 |
| `Query` | 封装查询条件和分页参数 |
| `VO` | 面向前端的展示对象 |
| `BO` | 复杂业务操作对象 |
| `Model` | 内部业务模型或领域模型 |

约束：

- Entity 只负责持久化映射，不承担接口展示职责。
- Query 不得直接复用 Entity。
- VO 不得暴露密码、Token、内部状态、数据库审计字段等敏感信息。
- 对象转换应集中处理，避免在多个 Controller 中重复转换。
- 命名必须保持项目现有风格，避免同一含义使用多个名称。

## 六、数据源与缓存规范

项目同时使用 MySQL、H2、MongoDB、Redis 及可选 Elasticsearch，新增功能必须明确数据存储位置。

### 6.1 MySQL 与 H2

- MySQL 用于生产环境，H2 仅用于开发测试。
- 不得依赖 H2 特有语法影响生产 SQL 兼容性。
- 数据库结构变更必须同步更新 SQL 脚本或迁移方案。
- 生产环境不得使用默认弱密码或硬编码数据库凭据。

### 6.2 Redis

- Redis 用于缓存、会话、验证码、限流或向量存储等场景时，应明确 Key 前缀。
- Key 必须具有业务隔离性和可读性。
- 缓存必须设置合理过期时间，避免永久缓存。
- 写缓存失败时应根据业务重要性决定降级策略。
- 不得将密码、JWT 密钥等敏感信息明文写入 Redis。

### 6.3 MongoDB

- MongoDB 文档结构应使用明确的领域对象或数据模型。
- 查询条件必须经过参数校验。
- 对高频查询字段建立合理索引。
- 避免一次性加载超大文档或无限制查询。

### 6.4 Elasticsearch

- Elasticsearch 为可选能力，仅在配置启用时使用。
- 通过条件配置控制 Elasticsearch 相关组件加载。
- Redis 向量存储模式下不得强制连接 Elasticsearch。
- Elasticsearch 连接失败不得影响不依赖该能力的基础功能。

## 七、AI、文件与外部服务规范

### 7.1 LangChain4j 与 AI 模块

- LangChain4j 统一使用 `1.0.0-beta3`。
- AI 模型、Ollama、OpenAI、通义千问等配置必须通过配置文件或环境变量注入。
- 禁止在源码中硬编码 API Key、Token、模型密钥和服务地址。
- 流式响应使用 Reactor `Flux` 时，应正确处理取消、超时、异常和资源释放。
- AI 工具调用必须限制输入范围、执行权限和执行时长。
- 工作流节点应保持单一职责，避免在节点中耦合持久化、网络和复杂编排逻辑。
- 对用户上传文档、URL、CSV、HTML 等外部数据必须进行大小、格式和内容安全校验。

### 7.2 文件与文档处理

项目使用 Apache POI、PDFBox、Docx4j、JodConverter、XDocReport、iTextPDF 等组件处理文档。

约束：

- 文件上传大小遵循配置限制：
  - 单文件最大 `50MB`
  - 单次请求最大 `50MB`
- 必须校验扩展名、MIME 类型和实际文件内容。
- 文件名不得直接拼接到服务器路径中，防止路径穿越。
- 上传文件应使用随机文件名或业务唯一 ID 保存。
- 文档转换过程应设置超时，并及时释放临时文件。
- LibreOffice 转换能力不可用时，应提供明确的异常信息或降级方案。
- PDF、Office、HTML 解析过程需防范恶意文件和资源消耗攻击。
- 用户文件不得未经授权直接暴露为公开静态资源。

### 7.3 MinIO

- MinIO 访问凭据必须通过环境变量或安全配置注入。
- Bucket、对象 Key 和访问权限应按业务模块隔离。
- 对外下载应校验用户权限和资源归属。
- 不得将 MinIO Secret、Access Key 提交到代码仓库。

## 八、安全规范

### 8.1 认证与授权

- 使用 Spring Security 和 JWT 实现认证授权。
- JWT 密钥必须通过环境变量 `TOKEN_SECRET` 注入。
- 生产环境的 `TOKEN_SECRET` 必须使用随机生成的高强度密钥，长度至少满足 HS512 要求。
- 不得在日志、异常信息或接口响应中输出 Token、密码和密钥。
- 所有需要保护的接口必须配置明确的权限校验。
- `/portal/**`、`/external/**`、文档页面等排除路径必须经过安全评估后配置。
- 前后台不同认证域的接口不得混用认证上下文。

### 8.2 输入校验

- 使用 `jakarta.validation.constraints.*` 下的校验注解。
- 使用 `@Valid`、`@Validated` 进行参数校验。
- 对分页参数设置最大页大小，防止恶意大分页。
- 禁止信任前端传入的用户 ID、角色 ID、文件路径和权限字段。
- 禁止手动拼接 SQL、HTML、命令或文件路径。
- 使用 Jsoup 等工具处理 HTML 时，必须配置允许的标签和属性范围。

### 8.3 XSS、CSRF 与敏感信息

- 按现有 `xss.enabled` 配置启用 XSS 防护。
- UGC 内容入口必须覆盖文章、评论、话题、私信及 CMS 内容等场景。
- 富文本内容必须经过白名单过滤。
- 验证码、密码、JWT 和第三方 API Key 不得明文记录日志。
- 生产环境应通过环境变量关闭 Knife4j/Swagger 文档，建议设置：

```text
KNIFE4J_PRODUCTION=true
```

## 九、配置文件规范

公共配置位于：

```text
moyun-server/src/main/resources/application.yaml
```

当前重要配置包括：

- 服务端口：`8080`
- 默认环境：`dev`
- 文件上传限制：`50MB`
- Jackson 日期格式：`yyyy-MM-dd HH:mm:ss`
- Jackson 时区：`Asia/Shanghai`
- Knife4j 中文界面：`zh_cn`
- Token 请求头：`Authorization`
- 验证码类型：`math`
- 默认开启 XSS 防护
- Elasticsearch 健康检查默认关闭

约束：

- 环境差异配置应放入对应 Profile 配置文件，不得修改公共配置来适配个人环境。
- 密钥、密码、域名、第三方服务地址优先使用环境变量。
- 生产环境必须覆盖以下配置：
  - `TOKEN_SECRET`
  - `PORTAL_DOMAIN`
  - 数据库连接信息
  - Redis/MongoDB/MinIO 连接信息
  - AI 服务密钥
  - `KNIFE4J_PRODUCTION`
- 配置项命名必须与现有 YAML 层级保持一致。
- 不得提交包含真实密钥的配置文件。

## 十、代码风格规范

### 10.1 命名规范

| 类型 | 命名方式 | 示例 |
|---|---|---|
| 类名 | UpperCamelCase | `UserServiceImpl` |
| 方法、变量 | lowerCamelCase | `saveUser()` |
| 常量 | UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS` |
| 包名 | 全小写 | `com.moyun.system.service` |
| Mapper | 业务名 + `Mapper` | `UserMapper` |
| Service 接口 | 业务名 + `Service` | `UserService` |
| Service 实现 | 业务名 + `ServiceImpl` | `UserServiceImpl` |
| Controller | 业务名 + `Controller` | `UserController` |

### 10.2 注释规范

- 所有新增类、公共方法、复杂字段和关键业务逻辑必须添加 Javadoc 或必要的行级注释。
- 注释必须使用用户第一语言：**中文**。
- 注释应说明“为什么这样做”，而不仅是重复代码含义。
- 对 AI 流程、权限判断、事务边界、文件转换、缓存策略等复杂逻辑必须添加中文说明。
- 禁止保留无意义、过时或与代码不一致的注释。
- 对外接口应补充请求参数、响应结果、权限要求和异常说明。

### 10.3 Lombok 使用规范

项目使用 Lombok 1.18.42，允许使用：

- `@Getter`
- `@Setter`
- `@Data`
- `@Builder`
- `@NoArgsConstructor`
- `@AllArgsConstructor`
- `@RequiredArgsConstructor`
- `@Slf4j`

约束：

- Entity、DTO、VO 使用 Lombok 时应结合实际场景选择注解。
- 不建议对包含复杂业务行为的类直接使用 `@Data`。
- 不得使用 Lombok 生成不符合安全要求的 `toString()`，尤其是密码、Token 等敏感字段。
- 构造器注入优先使用 `@RequiredArgsConstructor`。
- 不得依赖 IDE 自动生成代码替代必要的业务方法。

## 十一、日志与异常规范

### 11.1 日志

- 使用 Lombok 的 `@Slf4j`。
- 禁止使用 `System.out.println`。
- 日志内容必须避免密码、Token、密钥、完整文件内容和个人敏感信息。
- 生产环境避免输出大段 AI Prompt、完整响应和上传文件内容。
- 关键业务操作应记录必要的业务 ID、操作者和结果。
- 异常日志应包含上下文信息，但不得泄露内部实现细节给前端。

### 11.2 异常

- 使用项目统一异常体系和全局异常处理机制。
- 业务异常与系统异常应区分处理。
- Controller 不得直接返回堆栈信息。
- 外部服务异常应转换为明确、可追踪的业务错误。
- 不得捕获异常后静默忽略。
- 资源操作必须在异常场景下正确关闭和清理。

## 十二、接口与响应规范

- 接口路径、参数命名和返回结构遵循项目现有风格。
- 新增接口必须明确 HTTP 方法、权限和参数校验规则。
- 分页接口必须限制最大页大小。
- 时间字段统一遵循项目 Jackson 配置。
- 文件下载接口必须校验权限并设置正确的响应头。
- SSE 接口必须处理客户端断开、超时和异常。
- WebSocket/STOMP 接口必须校验连接用户身份和消息目标权限。
- 不得将数据库 Entity、内部异常对象或敏感配置直接返回前端。

## 十三、测试规范

- 新增核心业务逻辑必须补充测试。
- 测试代码放置在：

```text
moyun-server/src/test/java/com/moyun/
```

- 测试包结构应与被测代码包结构保持一致。
- 测试应覆盖：
  - 正常业务流程
  - 参数校验失败
  - 权限不足
  - 数据不存在
  - 外部服务异常
  - 文件格式或大小异常
  - 事务回滚场景
- 测试不得依赖生产数据库、生产 Redis、生产对象存储或真实 AI 密钥。
- 提交代码前至少执行：

```powershell
mvn test
```

## 十四、代码生成规范

- 使用 Velocity 2.3 作为代码生成模板引擎。
- 模板统一放在：

```text
moyun-server/src/main/resources/vm/
```

- 生成代码必须符合当前模块目录结构和命名规范。
- 生成代码中的注释必须使用中文。
- 修改模板后应验证生成的 Java、XML、JavaScript、Vue 和 SQL 文件。
- 禁止直接覆盖人工维护的业务代码，除非已确认文件为生成产物。

## 十五、编码原则

| 原则 | 说明 |
|---|---|
| **SOLID** | 保持高内聚、低耦合，增强可维护性和可扩展性 |
| **DRY** | 提取公共逻辑，避免重复代码 |
| **KISS** | 保持实现简单、清晰、易理解 |
| **YAGNI** | 不实现当前不需要的功能 |
| **OWASP** | 防范 SQL 注入、XSS、路径穿越、越权、敏感信息泄露等风险 |
| **接口优先** | 业务服务优先通过接口定义，实现类放在 `impl` 包中 |
| **配置外置** | 环境差异和敏感信息通过配置或环境变量注入 |
| **最小权限** | 用户、接口、文件和外部资源均遵循最小权限原则 |
| **失败可控** | 外部依赖不可用时应具备明确的异常、降级或重试策略 |

## 十六、提交前检查清单

提交代码前必须确认：

- [ ] 使用 JDK 21 编译通过。
- [ ] Maven 依赖版本符合项目统一配置。
- [ ] 未引入未经验证的重复依赖或冲突版本。
- [ ] Controller、Service、Mapper 职责清晰。
- [ ] Service 接口实现位于 `impl` 子包。
- [ ] Entity 未直接作为接口响应返回。
- [ ] 新增接口包含参数校验和权限控制。
- [ ] SQL 不存在字符串拼接注入风险。
- [ ] 文件上传、下载和转换经过安全校验。
- [ ] Token、密码、API Key 等敏感信息未硬编码或写入日志。
- [ ] 新增注释使用中文。
- [ ] 未使用 `System.out.println`。
- [ ] 已执行 `mvn test`。
- [ ] 已检查配置文件和日志中不存在敏感信息。
