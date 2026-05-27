# 墨韵·智库 - 文学+技术社区平台

**项目名称**: 墨韵·智库 (Moyun Wisdom)  
**文档版本**: v3.0  
**更新时间**: 2026-05-26  
**项目类型**: 前后端分离的社区平台  
**项目状态**: ✅ 第一阶段完成 | 🚧 第二阶段开发中

---

## 📊 项目阶段进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| **第一阶段** | ✅ 已完成 | 前台基础功能（用户认证、文章浏览、互动等） |
| **第二阶段** | 🚧 进行中 | 后台管理系统开发 |
| **第三阶段** | ⏳ 规划中 | 第三方登录集成（微信、支付宝） |
| **第四阶段** | ⏳ 规划中 | 高级功能（推荐算法、用户成长体系等） |

---

## 🎯 项目简介

墨韵·智库是一个面向年轻创作者、技术学习者、文学爱好者的**文学+技术双内容形态共存的社区平台**。项目采用现代化技术栈，构建完整的内容生产、分发推荐、用户成长、商业化体系。

### 核心特性

- **用户前台**: 清新美观的社区门户，支持文章发布、阅读、评论、互动
- **独立认证体系**: 前后台用户完全独立，双 SecurityFilterChain 配置
- **后台管理**: 功能强大的管理系统，支持内容审核、用户管理、系统监控（第二阶段开发中）
- **工作流引擎**: Flowable 集成，支持复杂的业务审批流程
- **多主题支持**: 日间、夜间、护眼三种主题自由切换
- **响应式设计**: 完美适配各种屏幕尺寸
- **内容双模式**: 技术流和文学派两种内容展示风格

---

## 🛠️ 技术栈总览

### 后端服务 (moyun-server)

| 技术 | 版本 | 说明 |
|------|------|------|
| **框架** | Spring Boot 3.3.2 | 核心框架 |
| **语言** | Java 21 | 编程语言 |
| **持久层** | MyBatis-Plus 3.5.7 | ORM 框架 |
| **数据库** | MySQL 8.0+ | 关系型数据库 |
| **缓存** | Redis | 缓存中间件 |
| **NoSQL** | MongoDB | 非关系型数据库 |
| **工作流** | Flowable 7.1.0 | 工作流引擎 |
| **安全** | Spring Security 6.3.1 | 安全框架 |
| **文档** | Knife4j 4.4.0 | API 文档 |
| **连接池** | Druid 1.2.23 | 数据库连接池 |
| **工具库** | Hutool 5.8.44 | 工具类库 |

### 用户前台 (moyun-portal)

| 技术 | 版本 | 说明 |
|------|------|------|
| **框架** | Vue 3.4.15 | 前端框架 |
| **构建工具** | Vite 5.0.12 | 构建工具 |
| **路由** | Vue Router 4.2.5 | 路由管理 |
| **状态管理** | Pinia 3.0.4 | 状态管理 |
| **样式** | Tailwind CSS 3.4.1 | CSS 框架 |
| **语言** | TypeScript 5.3.3 | 编程语言 |
| **富文本** | Vue Quill 1.2.0 | 编辑器 |
| **图标** | Lucide Vue Next 0.511.0 | 图标库 |
| **工具库** | Dayjs 1.11.10 | 日期处理 |
| **Markdown** | Marked 12.0.0 | Markdown 解析 |

### 后台管理 (moyun-admin-vue)

| 技术 | 版本 | 说明 |
|------|------|------|
| **框架** | Vue 3.4.0 | 前端框架 |
| **构建工具** | Vite 5.0.4 | 构建工具 |
| **UI 组件** | Element Plus 2.4.3 | UI 组件库 |
| **状态管理** | Pinia 2.1.7 | 状态管理 |
| **工作流** | bpmn-js 11.1.0 | BPMN 设计器 |
| **图表** | ECharts 5.4.3 | 数据可视化 |
| **表单设计** | vform3-builds 3.0.10 | 表单设计器 |
| **富文本** | Vue Quill 1.2.0 | 编辑器 |
| **路由** | Vue Router 4.2.5 | 路由管理 |
| **HTTP 客户端** | Axios 0.27.2 | HTTP 请求 |

---

## 📁 项目结构

```
moyun-project-document/
├── docs/                      # 项目文档
│   ├── 开发记录.md           # 开发历程、技术决策、问题解决
│   ├── 需求记录.md           # 功能需求、待开发功能清单
│   └── 部署与验证指南.md     # 环境搭建、功能验证、问题排查
│
├── moyun-server/              # 后端服务
│   ├── src/main/java/com/moyun/
│   │   ├── common/            # 通用基础模块
│   │   ├── core/              # 核心框架模块
│   │   ├── util/              # 工具模块
│   │   ├── modules/           # 业务模块
│   │   │   ├── system/        # 系统管理模块
│   │   │   └── portal/        # 门户系统模块
│   │   └── ext/               # 扩展模块
│   ├── src/main/resources/sql/
│   │   ├── 01_moyun_init.sql  # 系统管理基础表
│   │   ├── 02_workflow_init.sql # 工作流与定时任务表
│   │   ├── 03_portal_init.sql # 门户系统表
│   │   └── README.md
│   └── pom.xml
│
├── moyun-portal/              # 用户前台
│   ├── src/
│   │   ├── api/               # API 接口（统一 /portal/ 前缀）
│   │   ├── components/        # 组件
│   │   ├── pages/             # 页面
│   │   ├── router/            # 路由
│   │   ├── stores/            # 状态管理
│   │   └── types/             # 类型定义
│   ├── API文档.md
│   └── package.json
│
├── moyun-admin-vue/           # 后台管理（待开发）
│   ├── src/
│   │   ├── api/               # API 接口
│   │   ├── views/             # 页面视图
│   │   ├── components/        # 公共组件
│   │   ├── store/             # 状态管理
│   │   └── router/            # 路由
│   └── package.json
│
└── .trae/rules/               # 项目规则
    └── git-commit-message.md  # Git 提交规范
```

---

## 🚀 快速开始

详细的部署步骤和功能验证请参考 [部署与验证指南.md](./docs/部署与验证指南.md)

### 环境要求

- **JDK**: 21+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Maven**: 3.8+

### 数据库初始化

按顺序执行以下 SQL 脚本：

```bash
# 1. 系统管理基础表
mysql -u root -p moyun < moyun-server/src/main/resources/sql/01_moyun_init.sql

# 2. 工作流与定时任务表
mysql -u root -p moyun < moyun-server/src/main/resources/sql/02_workflow_init.sql

# 3. 门户系统表
mysql -u root -p moyun < moyun-server/src/main/resources/sql/03_portal_init.sql
```

### 后端服务启动

```bash
cd moyun-server
# 修改 application.yaml 中的数据库和 Redis 配置
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动  
Swagger 文档: `http://localhost:8080/doc.html`  
默认后台账号: `admin` / `admin123`

### 用户前台启动

```bash
cd moyun-portal
npm install
npm run dev
```

前台将在 `http://localhost:5173` 启动

### 后台管理启动

（第二阶段开发中）

---

## ✅ 第一阶段已完成功能

### 用户前台 (moyun-portal)

| 模块 | 功能描述 | 状态 |
|------|----------|------|
| **用户认证** | 用户注册、登录、登出、Token认证 | ✅ |
| **文章浏览** | 文章列表、文章详情、分类筛选、搜索 | ✅ |
| **文章互动** | 点赞、收藏、关注作者 | ✅ |
| **评论系统** | 查看评论、发表评论 | ✅ |
| **分类标签** | 分类列表、标签列表 | ✅ |

### 技术架构改进

- 前后台独立认证体系（双 SecurityFilterChain）
- 独立用户表（sys_user 和 portal_user）
- 统一前台 API 路径前缀 /portal/
- 包结构优化（common/core/util/modules/ext）
- 移除前台模拟数据，使用真实 API

---

## 🚧 第二阶段待开发功能（后台管理）

| 模块 | 功能描述 | 优先级 |
|------|----------|--------|
| **门户用户管理** | 用户列表、详情、状态管理 | 🔴 高 |
| **文章管理** | 文章列表、审核、上下架、推荐 | 🔴 高 |
| **分类管理** | 分类增删改查、排序 | 🔴 高 |
| **标签管理** | 标签增删改查 | 🔴 高 |
| **评论管理** | 评论审核、删除 | 🔴 高 |
| **数据统计** | 仪表盘、用户/文章数据统计 | 🟡 中 |

详细需求请参考 [需求记录.md](./docs/需求记录.md)

---

## 📝 开发规范

### Git 提交规范

格式: `type(scope): subject`

**Type 类型**:
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具变更

**示例**:
```bash
git commit -m "feat(article): 添加文章点赞功能"
git commit -m "fix(user): 修复登录验证问题"
```

### 代码规范

- **前端**: 遵循 Vue 3 最佳实践，使用 TypeScript
- **后端**: 遵循 Java 开发规范，接口统一返回 Result 格式
- **命名**: 文件夹使用 kebab-case，组件使用 PascalCase，变量使用 camelCase

---

## 🔧 部署指南

### 后端部署

```bash
# 打包
mvn clean package -DskipTests

# 运行
java -jar target/moyun-server-1.0.0.jar
```

### 前端部署

```bash
# 用户前台
cd moyun-portal
npm run build
# 将 dist 目录部署到 Nginx 或其他 Web 服务器

# 后台管理
cd moyun-admin-vue
npm run build:prod
# 将 dist 目录部署到 Nginx 或其他 Web 服务器
```

---

## 📊 项目统计

| 类别 | 数量 | 说明 |
|------|------|------|
| **后端模块** | 4+ | common、framework、system、community |
| **前台页面** | 15+ | 首页、详情、搜索、用户中心等 |
| **后台模块** | 6+ | 26个子模块，120+功能点 |
| **前端组件** | 50+ | 前台 + 后台公共组件 |
| **API 接口** | 80+ | 前后端 API 接口 |

---

## 📚 相关文档

| 文档 | 说明 |
|------|------|
| [开发记录.md](./docs/开发记录.md) | 开发历程、技术决策、问题解决方案 |
| [需求记录.md](./docs/需求记录.md) | 功能需求清单、待开发功能规划 |
| [部署与验证指南.md](./docs/部署与验证指南.md) | 环境搭建、功能验证、常见问题排查 |
| [前台API文档.md](./moyun-portal/API文档.md) | 前台接口文档 |
| [数据库脚本说明](./moyun-server/src/main/resources/sql/README.md) | SQL脚本说明 |
| [Git 提交规范](./.trae/rules/git-commit-message.md) | Git 提交信息格式规范 |

---

## 🤝 贡献指南

欢迎贡献代码、提交 Issue 或提出改进建议！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 查看 LICENSE 文件了解详情

---

## 🎯 项目路线图

| 阶段 | 状态 | 时间 | 说明 |
|------|------|------|------|
| **第一阶段** | ✅ 已完成 | 2026-05 | 前台基础功能 |
| **第二阶段** | 🚧 进行中 | - | 后台管理系统 |
| **第三阶段** | ⏳ 规划中 | - | 第三方登录集成 |
| **第四阶段** | ⏳ 规划中 | - | 高级功能完善 |

### 第二阶段具体任务

- [ ] 门户用户管理模块
- [ ] 文章管理模块（审核、上下架、推荐）
- [ ] 分类管理模块
- [ ] 标签管理模块
- [ ] 评论管理模块
- [ ] 数据统计模块

---

## 📞 技术支持

如有问题，请先查阅相关文档，特别是 [部署与验证指南.md](./docs/部署与验证指南.md) 中的常见问题排查部分。

**相关资源**:
- [Vue 3 官方文档](https://cn.vuejs.org/)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Element Plus 官方文档](https://element-plus.org/)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [Flowable 官方文档](https://www.flowable.com/open-source/docs)

---

**维护人员**: 项目团队  
**最后更新**: 2026-05-26  
**文档版本**: v3.0
