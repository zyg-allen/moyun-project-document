
# 开发规范指南
为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目概览

- **项目名称**：moyun-project-document (旭林知行)
- **代码作者**：Lenovo
- **工作目录**：`E:\zyg_new_work\moyun-project-document`
- **构建工具**：Maven
- **SDK 版本**：JDK 21
- **注释语言**：中文（简体）

## 二、目录结构

项目采用多模块结构，后端核心位于 `moyun-server`。

```text
moyun-project-document
├── docs                            # 项目文档
├── moyun-admin-vue                 # 后台管理前端
├── moyun-ledger-app                # 记账 App 前端
├── moyun-portal                    # 门户前端
└── moyun-server                    # 后端服务
    └── src
        ├── main
        │   ├── java
        │   │   └── com.moyun
        │   │       ├── common      # 通用模块 (注解/常量/枚举/异常/过滤器)
        │   │       ├── core        # 核心模块 (切面/基类/配置/安全/MVC)
        │   │       ├── ext         # 扩展模块 (AI/CMS/文件/代码生成/定时任务)
        │   │       ├── ledger      # 记账业务模块
        │   │       ├── pay         # 支付业务模块
        │   │       ├── portal      # 门户业务模块
        │   │       ├── system      # 系统管理模块
        │   │       └── util        # 工具类
        │   └── resources
        │       ├── mapper          # MyBatis XML 映射文件
        │       └── application.yaml
        └── test
```

## 三、技术栈要求

- **主框架**：Spring Boot 3.3.2
- **语言版本**：Java 21
- **ORM 框架**：MyBatis-Plus 3.5.11 (替换了原 JPA)
- **数据库连接池**：Druid 1.2.23
- **核心依赖**：
  - `spring-boot-starter-web`
  - `spring-boot-starter-security` + `jjwt` (权限认证)
  - `lombok` 1.18.42 (适配 JDK 21+)
  - `hutool-all` 5.8.44 (工具库)
  - `langchain4j` 1.0.0-beta3 (AI 大模型整合)
  - `knife4j` 4.4.0 (API 文档)

## 四、分层架构规范

| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|----------------------------------------------------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口 | 使用 `@RestController`；不得包含业务逻辑，仅负责参数校验与结果封装 |
| **Service**    | 实现业务逻辑、事务管理与数据校验   | 接口定义在 `service` 包，实现类放在 `impl` 子包；事务注解 `@Transactional` 仅在此层使用 |
| **Mapper**     | 数据库访问与持久化操作             | 继承 `BaseMapper`；复杂 SQL 写在 XML 中；避免 N+1 查询         |
| **Entity/DTO** | 数据对象映射                       | Entity 对应数据库表；DTO/VO 用于前后端交互；禁止 Entity 直接暴露给前端 |

### 接口与实现分离

- 所有 Service 接口实现类需放在接口所在包下的 `impl` 子包中。

## 五、安全与性能规范

### 输入校验

- 使用 `@Valid` 与 JSR-303 校验注解（位于 `jakarta.validation.constraints.*`）。
- 禁止手动拼接 SQL 字符串，防止 SQL 注入；MyBatis-Plus 查询使用 Wrapper 或 XML 参数绑定。

### 事务管理

- `@Transactional` 注解仅用于 **Service 层**方法。
- 涉及 AI 调用、HTTP 请求等外部依赖的方法，需注意事务传播行为与超时设置，避免长事务锁定数据库。

### 权限控制

- 使用 Spring Security + JWT 进行认证授权。
- 敏感接口需配置权限注解（如 `@PreAuthorize`）。

## 六、代码风格规范

### 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`     |
| 方法/变量  | lowerCamelCase       | `saveUser()`          |
| 常量       | UPPER_SNAKE_CASE     | `MAX_RETRY_COUNT`     |
| 包名       | 全小写               | `com.moyun.ledger`    |

### 注释规范

- **强制要求**：所有类、接口、公共方法必须添加 **中文 Javadoc** 注释。
- 注释需说明参数、返回值及可能抛出的异常。

```java
/**
 * 用户服务实现类
 * @author Lenovo
 */
public class UserServiceImpl implements UserService {
    /**
     * 根据ID查询用户信息
     * @param id 用户ID
     * @return 用户视图对象
     */
    public UserVO selectUserById(Long id) {
        // ...
    }
}
```

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明                     | 示例         |
|------|------------------------------|--------------|
| DTO  | 数据传输对象                 | `UserDTO`    |
| VO   | 视图展示对象                 | `UserVO`     |
| BO   | 业务逻辑封装对象             | `UserBO`     |
| Query| 查询参数封装对象             | `UserQuery`  |

### 实体类简化工具

- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：
  - `@Data` (Getter/Setter/ToString/EqualsAndHashCode)
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`
  - `@Builder` (推荐用于 DTO/VO 构建复杂场景)

## 七、依赖与中间件规范

### MyBatis-Plus 规范

- 单表操作优先使用 `IService` 与 `BaseMapper` 提供的方法。
- 分页查询使用 `Page` 对象，配置 `MybatisPlusInterceptor` 分页插件。
- 逻辑删除字段统一配置（如 `del_flag` 或 `is_deleted`）。

### AI 模块规范

- AI 相关逻辑位于 `com.moyun.ext.ai` 包。
- 使用 LangChain4j 进行模型调用，配置统一管理。
- Prompt 模板建议独立管理，避免硬编码在代码中。

### 工具类使用

- 优先使用 Hutool 工具库（`cn.hutool.*`）处理日期、字符串、加密、HTTP 等常用操作。
- 文件操作优先使用 `commons-io` 或 Hutool 的 `FileUtil`。

### 日志记录

- 使用 `@Slf4j` 注解代替 `System.out.println`。
- 日志级别遵循 `application.yaml` 配置（com.moyun: debug）。

## 八、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |
