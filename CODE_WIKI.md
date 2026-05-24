# 墨韵·智库 Code Wiki 文档

**文档版本**: v1.0  
**创建日期**: 2026-05-24  
**维护团队**: 项目开发团队

---

## 目录

1. [项目概述](#项目概述)
2. [技术架构](#技术架构)
3. [项目结构](#项目结构)
4. [数据库设计](#数据库设计)
5. [核心模块说明](#核心模块说明)
6. [API 接口规范](#api-接口规范)
7. [开发指南](#开发指南)
8. [部署指南](#部署指南)
9. [常见问题](#常见问题)

---

## 项目概述

### 项目简介

**墨韵·智库 (Moyun Wisdom)** 是一个面向年轻创作者、技术学习者、文学爱好者的文学+技术双内容形态共存的社区平台。

- **用户前台**: 提供清新美观的社区门户，支持文章发布、阅读、评论、互动等功能
- **后台管理**: 提供功能强大的管理系统，支持内容审核、用户管理、系统监控
- **工作流引擎**: 集成 Flowable，支持复杂的业务审批流程

### 核心特性

- ✨ 多主题支持（日间、夜间、护眼）
- 📱 响应式设计，完美适配各种屏幕尺寸
- 📝 富文本和 Markdown 双编辑器模式
- 🔄 工作流审批系统
- 🎯 用户成长体系（积分、等级、徽章）
- 💳 钱包和 VIP 会员系统

---

## 技术架构

### 技术栈总览

#### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.3.2 | 核心框架 |
| **Java** | 21 | 编程语言 |
| **MyBatis-Plus** | 3.5.7 | ORM 框架 |
| **MySQL** | 8.0+ | 关系型数据库 |
| **Redis** | 6.0+ | 缓存中间件 |
| **MongoDB** | 6.0+ | 非关系型数据库 |
| **Flowable** | 7.1.0 | 工作流引擎 |
| **Spring Security** | 6.3.1 | 安全框架 |
| **Knife4j** | 4.4.0 | API 文档 |
| **Druid** | 1.2.23 | 数据库连接池 |
| **Hutool** | 5.8.44 | 工具类库 |

#### 前台技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue 3** | 3.4.15 | 前端框架 |
| **Vite** | 5.0.12 | 构建工具 |
| **Vue Router** | 4.2.5 | 路由管理 |
| **Pinia** | 3.0.4 | 状态管理 |
| **Tailwind CSS** | 3.4.1 | CSS 框架 |
| **TypeScript** | 5.3.3 | 编程语言 |

#### 后台管理技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue 3** | 3.4.0 | 前端框架 |
| **Vite** | 5.0.4 | 构建工具 |
| **Element Plus** | 2.4.3 | UI 组件库 |
| **Pinia** | 2.1.7 | 状态管理 |
| **ECharts** | 5.4.3 | 数据可视化 |

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         前端层                                │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────┐     ┌─────────────────────┐        │
│  │   moyun-portal      │     │  moyun-admin-vue    │        │
│  │   (用户门户)         │     │   (后台管理)         │        │
│  └─────────────────────┘     └─────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
                              ↕ HTTPS
┌─────────────────────────────────────────────────────────────┐
│                        网关层 (Nginx)                         │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                      应用服务层                               │
├─────────────────────────────────────────────────────────────┤
│                    moyun-server                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  认证    │  │  内容    │  │  成长    │  │  钱包    │   │
│  │  模块    │  │  模块    │  │  模块    │  │  模块    │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                        数据存储层                             │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   MySQL     │  │    Redis    │  │     MongoDB         │ │
│  │  (主数据)    │  │   (缓存)     │  │   (日志/文档)       │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 项目结构

### 完整目录结构

```
moyun-project-document/
├── .trae/                          # Trae 配置
│   └── rules/
│       └── git-commit-message.md
├── moyun-server/                   # 后端服务
│   ├── src/main/java/com/moyun/
│   │   ├── common/                 # 通用模块
│   │   │   ├── annotation/         # 自定义注解
│   │   │   ├── config/             # 配置类
│   │   │   ├── constant/           # 常量定义
│   │   │   ├── core/               # 核心组件
│   │   │   │   ├── controller/     # 基础控制器
│   │   │   │   ├── domain/         # 基础实体类
│   │   │   │   ├── page/           # 分页组件
│   │   │   │   ├── redis/          # Redis 工具
│   │   │   │   └── text/           # 文本处理
│   │   │   ├── enums/              # 枚举定义
│   │   │   ├── exception/          # 异常处理
│   │   │   ├── filter/             # 过滤器
│   │   │   ├── utils/              # 工具类
│   │   │   └── xss/                # XSS 防护
│   │   ├── community/              # 社区业务模块
│   │   │   ├── auth/               # 认证模块
│   │   │   │   ├── controller/
│   │   │   │   ├── model/
│   │   │   │   └── service/
│   │   │   ├── content/            # 内容模块
│   │   │   │   ├── controller/
│   │   │   │   │   ├── admin/      # 后台管理接口
│   │   │   │   │   └── app/        # 前台应用接口
│   │   │   │   ├── entity/         # 实体类
│   │   │   │   ├── mapper/         # Mapper
│   │   │   │   ├── model/
│   │   │   │   └── service/        # 服务层
│   │   │   ├── growth/             # 成长模块
│   │   │   │   ├── controller/
│   │   │   │   ├── entity/
│   │   │   │   ├── mapper/
│   │   │   │   └── service/
│   │   │   └── wallet/             # 钱包模块
│   │   │       ├── controller/
│   │   │       ├── entity/
│   │   │       ├── mapper/
│   │   │       └── service/
│   │   ├── framework/              # 框架层
│   │   │   ├── aspectj/            # AOP 切面
│   │   │   ├── config/             # 配置
│   │   │   ├── interceptor/        # 拦截器
│   │   │   ├── manager/            # 异步任务管理
│   │   │   ├── security/           # 安全组件
│   │   │   └── web/                # Web 相关
│   │   ├── system/                 # 系统模块
│   │   │   ├── controller/
│   │   │   ├── domain/
│   │   │   ├── flowable/           # Flowable 工作流
│   │   │   └── service/
│   │   └── MoyunApplication.java   # 启动类
│   └── pom.xml
│
├── moyun-portal/                   # 用户前台
│   ├── src/
│   │   ├── api/                    # API 接口
│   │   ├── components/             # 组件
│   │   ├── composables/            # 组合式函数
│   │   ├── data/                   # 模拟数据
│   │   ├── lib/                    # 库
│   │   ├── pages/                  # 页面
│   │   ├── router/                 # 路由
│   │   ├── stores/                 # 状态管理
│   │   ├── types/                  # 类型定义
│   │   ├── utils/                  # 工具函数
│   │   ├── App.vue
│   │   ├── main.ts
│   │   └── style.css
│   ├── API文档.md
│   ├── package.json
│   └── vite.config.ts
│
├── moyun-admin-vue/                # 后台管理
│   ├── src/
│   │   ├── api/                    # API 接口
│   │   ├── assets/                 # 静态资源
│   │   ├── components/             # 组件
│   │   ├── directive/              # 指令
│   │   ├── layout/                 # 布局
│   │   ├── plugins/                # 插件
│   │   ├── router/                 # 路由
│   │   ├── store/                  # 状态管理
│   │   ├── utils/                  # 工具函数
│   │   ├── views/                  # 页面视图
│   │   ├── App.vue
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
│
├── README.md
└── CODE_WIKI.md                    # 本文档
```

---

## 数据库设计

### 核心数据表

#### 1. 用户相关表

| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户表 |
| `sys_role` | 角色表 |
| `sys_menu` | 菜单表 |
| `sys_dept` | 部门表 |
| `sys_post` | 岗位表 |

#### 2. 内容相关表

| 表名 | 说明 |
|------|------|
| `cms_article` | 文章表 |
| `cms_category` | 分类表 |
| `cms_tag` | 标签表 |
| `cms_article_tag` | 文章标签关联表 |
| `cms_comment` | 评论表 |
| `cms_collect` | 收藏表 |
| `cms_like` | 点赞表 |
| `cms_banner` | 轮播图表 |
| `cms_notification` | 通知表 |

#### 3. 成长相关表

| 表名 | 说明 |
|------|------|
| `cms_level_config` | 等级配置表 |
| `cms_points_rule` | 积分规则表 |
| `cms_points_log` | 积分日志表 |
| `cms_badge` | 徽章表 |
| `cms_user_badge` | 用户徽章关联表 |

#### 4. 钱包相关表

| 表名 | 说明 |
|------|------|
| `cms_wallet` | 钱包表 |
| `cms_transaction` | 交易记录表 |
| `cms_recharge` | 充值记录表 |
| `cms_withdraw` | 提现记录表 |

#### 5. 其他表

| 表名 | 说明 |
|------|------|
| `cms_audit_record` | 审核记录表 |
| `cms_report` | 举报表 |
| `cms_search_history` | 搜索历史表 |
| `cms_search_hotword` | 搜索热词表 |
| `cms_friend_link` | 友情链接表 |

### cms_article 表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `article_id` | BIGINT | 主键 |
| `author_id` | BIGINT | 作者ID |
| `category_id` | BIGINT | 分类ID |
| `title` | VARCHAR | 标题 |
| `summary` | TEXT | 摘要 |
| `cover_url` | VARCHAR | 封面URL |
| `content` | LONGTEXT | 内容 |
| `content_type` | VARCHAR | 内容类型 (richtext/markdown) |
| `article_type` | VARCHAR | 文章类型 |
| `status` | VARCHAR | 状态 (draft/pending/published/archived) |
| `is_top` | INT | 是否置顶 |
| `is_recommend` | INT | 是否推荐 |
| `view_count` | BIGINT | 浏览数 |
| `like_count` | BIGINT | 点赞数 |
| `collect_count` | BIGINT | 收藏数 |
| `comment_count` | INT | 评论数 |
| `share_count` | INT | 分享数 |
| `publish_time` | DATETIME | 发布时间 |
| `audit_time` | DATETIME | 审核时间 |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 更新时间 |

---

## 核心模块说明

### 1. 用户认证模块 (community/auth)

**职责**: 处理用户登录、注册、认证、授权等功能

**主要组件**:
- `AuthController`: 认证接口控制器
- `AuthService`: 认证服务接口
- `LoginDto`: 登录请求DTO
- `RegisterDto`: 注册请求DTO
- `UserVo`: 用户信息VO

**接口**:
- `POST /app/auth/login` - 用户登录
- `POST /app/auth/register` - 用户注册
- `POST /app/auth/logout` - 用户登出
- `GET /app/auth/current` - 获取当前用户信息

### 2. 内容管理模块 (community/content)

#### 2.1 文章子模块

**职责**: 文章的增删改查、审核、推荐等功能

**主要组件**:
- `ArticleAppController`: 前台文章接口
- `CmsArticleAdminController`: 后台文章管理接口
- `CmsArticle`: 文章实体
- `ICmsArticleService`: 文章服务接口

**主要方法**:
- `selectArticleById()` - 根据ID查询文章
- `selectArticleList()` - 查询文章列表
- `selectMyArticleList()` - 查询我的文章
- `incrementViewCount()` - 增加浏览量
- `incrementLikeCount()` - 增加点赞数

#### 2.2 分类子模块

**职责**: 文章分类的管理

**主要组件**:
- `CategoryAppController`: 前台分类接口
- `CmsCategoryAdminController`: 后台分类管理接口
- `CmsCategory`: 分类实体
- `ICmsCategoryService`: 分类服务接口

#### 2.3 标签子模块

**职责**: 文章标签的管理

#### 2.4 评论子模块

**职责**: 文章评论的管理

#### 2.5 收藏子模块

**职责**: 文章收藏功能

#### 2.6 点赞子模块

**职责**: 文章点赞功能

#### 2.7 通知子模块

**职责**: 用户通知的管理

### 3. 成长体系模块 (community/growth)

**职责**: 用户等级、积分、徽章等成长体系的管理

**主要组件**:
- `GrowthAppController`: 前台成长接口
- `CmsLevelConfigAdminController`: 后台等级配置管理
- `CmsBadgeAdminController`: 后台徽章管理
- `CmsLevelConfig`: 等级配置实体
- `CmsBadge`: 徽章实体
- `CmsUserBadge`: 用户徽章关联实体

### 4. 钱包模块 (community/wallet)

**职责**: 用户钱包、充值、提现、交易记录等功能

**主要组件**:
- `WalletAppController`: 前台钱包接口
- `CmsWalletAdminController`: 后台钱包管理
- `CmsRechargeAdminController`: 后台充值管理
- `CmsWallet`: 钱包实体
- `CmsTransaction`: 交易记录实体

### 5. 系统管理模块 (system)

**职责**: 系统级功能管理，包括用户、角色、菜单、部门、字典等

**主要组件**:
- `SysUserController`: 用户管理
- `SysRoleController`: 角色管理
- `SysMenuController`: 菜单管理
- `SysConfigController`: 系统配置管理
- `FlowDefinitionController`: 流程定义管理

---

## API 接口规范

### 通用响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 分页响应格式

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "list": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权/Token无效 |
| 403 | 没有权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 前台接口路径规范

- 前台应用接口: `/app/xxx`
- 后台管理接口: `/admin/xxx`

### 详细 API 文档

完整的前台 API 文档请参考: [moyun-portal/API文档.md](./moyun-portal/API文档.md)

---

## 开发指南

### 环境要求

- **JDK**: 21+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Maven**: 3.8+

### 后端开发

#### 本地启动步骤

1. 配置数据库连接
2. 配置 Redis 连接
3. 运行 `MoyunApplication` 启动类

#### 代码规范

- 遵循 Java 开发规范
- 接口统一返回 `AjaxResult` 或 `TableDataInfo`
- 使用 MyBatis-Plus 进行数据库操作
- Service 层使用接口 + 实现类的方式

#### 添加新模块

1. 在 `community` 目录下创建新模块文件夹
2. 创建 `controller`、`entity`、`mapper`、`service` 子目录
3. 编写相应的代码

### 前台开发

#### 本地启动步骤

```bash
cd moyun-portal
npm install
npm run dev
```

#### 代码规范

- 遵循 Vue 3 最佳实践
- 使用 TypeScript
- 使用 Composition API
- 组件使用 PascalCase 命名
- 变量使用 camelCase 命名

### 后台管理开发

#### 本地启动步骤

```bash
cd moyun-admin-vue
npm install
npm run dev
```

---

## 部署指南

### 后端部署

```bash
# 打包
cd moyun-server
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

## 常见问题

### 1. 数据库连接失败

检查 `application.yml` 中的数据库配置是否正确，确保 MySQL 服务已启动。

### 2. Redis 连接失败

检查 Redis 服务是否启动，配置中的地址和端口是否正确。

### 3. 前端跨域问题

后端已配置 CORS，检查 `WebConfig` 配置是否正确。

---

**文档更新记录**:
- 2026-05-24: 初始版本创建
