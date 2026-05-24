# 墨韵·智库 - 文学+技术社区平台

**项目名称**: 墨韵·智库 (Moyun Wisdom)  
**文档版本**: v2.0  
**更新时间**: 2026-05-24  
**项目类型**: 前后端分离的社区平台  
**项目状态**: 🚧 开发中

---

## 🎯 项目简介

墨韵·智库是一个面向年轻创作者、技术学习者、文学爱好者的**文学+技术双内容形态共存的社区平台**。项目采用现代化技术栈，构建完整的内容生产、分发推荐、用户成长、商业化体系。

### 核心特性

- **用户前台**: 清新美观的社区门户，支持文章发布、阅读、评论、互动
- **后台管理**: 功能强大的管理系统，支持内容审核、用户管理、系统监控
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
├── moyun-server/              # 后端服务
│   ├── src/main/java/com/moyun/
│   │   ├── common/            # 通用模块
│   │   ├── framework/         # 框架层
│   │   ├── system/            # 系统模块
│   │   └── community/         # 社区业务模块
│   └── pom.xml
│
├── moyun-portal/              # 用户前台
│   ├── src/
│   │   ├── api/               # API 接口
│   │   ├── components/        # 组件
│   │   ├── pages/             # 页面
│   │   ├── router/            # 路由
│   │   ├── stores/            # 状态管理
│   │   └── types/             # 类型定义
│   └── package.json
│
├── moyun-admin-vue/           # 后台管理
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

### 环境要求

- **JDK**: 21+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Maven**: 3.8+

### 后端服务启动

```bash
cd moyun-server
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 用户前台启动

```bash
cd moyun-portal
npm install
npm run dev
```

前台将在 `http://localhost:5173` 启动

### 后台管理启动

```bash
cd moyun-admin-vue
npm install
npm run dev
```

后台将在 `http://localhost:5174` 启动

---

## 🎨 功能模块

### 用户前台 (moyun-portal)

| 模块 | 功能描述 |
|------|----------|
| **首页** | Hero 轮播、文章列表、分类导航、热门标签 |
| **文章详情** | 文章展示、评论区、相关推荐、点赞收藏 |
| **搜索** | 关键词搜索、分类筛选、标签筛选 |
| **用户中心** | 用户信息、我的文章、设置 |
| **登录/注册** | 用户认证、注册登录 |
| **发布文章** | 富文本/Markdown 编辑器、分类标签选择 |
| **作者** | 作者主页、作者列表 |
| **帮助/关于** | 帮助中心、关于我们、用户协议、反馈举报 |
| **主题切换** | 日间主题、夜间主题、护眼主题 |

### 后台管理 (moyun-admin-vue)

| 模块 | 子模块 | 功能描述 |
|------|--------|----------|
| **系统管理** | 用户管理 | 用户CRUD、角色分配、状态管理 |
| | 角色管理 | 角色权限配置、数据权限 |
| | 菜单管理 | 菜单树、权限标识 |
| | 部门管理 | 部门树、负责人 |
| | 岗位管理 | 岗位列表 |
| | 字典管理 | 字典类型、字典数据 |
| | 参数设置 | 系统参数配置 |
| | 通知公告 | 公告发布管理 |
| **社区管理** | 文章管理 | 文章审核、上下架、推荐 |
| | 分类管理 | 栏目分类树 |
| | 审核管理 | 待审核列表、批量审核 |
| **工作流管理** | 流程定义 | BPMN设计器、流程部署 |
| | 流程实例 | 实例管理、终止 |
| | 待办任务 | 任务审批、转办、委派 |
| | 已办任务 | 审批历史、流程轨迹 |
| **系统监控** | 在线用户 | 会话管理、强退 |
| | 定时任务 | Cron配置、日志 |
| | 数据监控 | Druid监控 |
| | 服务监控 | CPU、内存、JVM |
| | 缓存监控 | Redis信息 |
| | 操作日志 | 操作记录 |
| | 登录日志 | 登录记录 |
| **系统工具** | 代码生成 | 一键生成代码 |
| | 表单构建 | 拖拽式设计 |

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
| [前台开发指南](./moyun-portal/README.md) | 用户前台项目文档 |
| [后台开发日志](./moyun-admin-vue/doc/开发日志.md) | 后台管理开发记录 |
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

## 🎯 下一步规划

- [ ] 完善推荐算法系统
- [ ] 集成 Elasticsearch 搜索
- [ ] 添加用户成长体系（积分、等级、勋章）
- [ ] 实现钱包打赏功能
- [ ] 完善工作流审批流程
- [ ] 添加性能监控和错误追踪
- [ ] 优化移动端体验
- [ ] 添加单元测试和 E2E 测试

---

## 📞 技术支持

如有问题，请查阅相关文档或联系项目维护人员。

**相关资源**:
- [Vue 3 官方文档](https://cn.vuejs.org/)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Element Plus 官方文档](https://element-plus.org/)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [Flowable 官方文档](https://www.flowable.com/open-source/docs)

---

**维护人员**: 项目团队  
**最后更新**: 2026-05-24  
**文档版本**: v2.0
