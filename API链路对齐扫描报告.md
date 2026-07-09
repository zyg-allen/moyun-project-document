# 墨韵智库前后端 API 链路对齐扫描报告

**扫描日期**: 2026-07-03  
**扫描范围**: 前台 Portal、后台 Admin、后端 Controller、路由页面

---

## 一、总体数据概览

| 类别 | 数量 |
|------|------|
| 前台 API 调用数 | 182 |
| 后台 API 调用数 | 409 |
| 后端接口总数 | 656 |
| **P0 阻断问题** | **6** |
| **P1 严重问题** | **1** |
| **P2 中等问题** | **119+** |
| 路由-页面问题 | 0 |

**后端接口分布**:
- Portal 前缀接口: 289 个
- CMS 前缀接口: 161 个
- System 前缀接口: 93 个
- 其他接口 (file/upload/flowable/tool 等): 113 个

---

## 二、P0 阻断问题（前端调用不存在的后端接口）

### 2.1 前台 Portal - 缺失接口

| 序号 | 前端函数名 | HTTP方法 | URL | 前端文件 | 说明 |
|------|-----------|----------|-----|----------|------|
| 1 | `search` | GET | `/portal/search` | `moyun-portal/src/api/search.ts:9` | 全站搜索接口，后端不存在 |
| 2 | `uploadFile` | POST | `/upload/file` | `moyun-portal/src/api/upload.ts:8` | 文件上传路径不匹配，后端是 `/portal/file/upload` 或 `/file/upload` |
| 3 | `uploadImage` | POST | `/upload/image` | `moyun-portal/src/api/upload.ts:13` | 图片上传路径不匹配，后端不存在 `/upload/image` |
| 4 | `sendSmsCode` | POST | `/portal/user/send-sms` | `moyun-portal/src/api/user.ts:47` | 发送短信验证码接口，后端不存在 |
| 5 | `updateBookshelfLastChapter` | PUT | `/portal/reading/bookshelf/:bookId/last-chapter` | `moyun-portal/src/api/reading.ts:128` | **需核实** - URL 模板字符串解析问题，实际路径可能正确 |

> **注意**: 第 5 项可能是扫描器解析模板字符串导致的误报，需人工核实。

### 2.2 后台 Admin - 缺失接口（部分）

后台缺失接口较多，主要分为以下几类：

#### 2.2.1 基础登录/退出接口

| 序号 | 前端函数名 | HTTP方法 | URL | 前端文件 |
|------|-----------|----------|-----|----------|
| 1 | `logout` | POST | `/logout` | `moyun-admin-vue/src/api/login.js` |

> 后端 `SysLoginController` 可能有退出接口，但路径可能不同，需核实。

#### 2.2.2 代码生成模块 (tool/gen)

| 序号 | 前端函数名 | HTTP方法 | URL | 前端文件 |
|------|-----------|----------|-----|----------|
| 1 | `previewTable` | GET | `/tool/gen/preview` | `moyun-admin-vue/src/api/tool/gen.js` |
| 2 | `genCode` | GET | `/tool/gen/genCode` | `moyun-admin-vue/src/api/tool/gen.js` |
| 3 | `synchDb` | GET | `/tool/gen/synchDb` | `moyun-admin-vue/src/api/tool/gen.js` |

#### 2.2.3 App 模块 (app/register, app/class, app/custom)

这些可能是历史遗留的 App 管理模块，后端没有对应 Controller。

| 序号 | 前端函数名 | HTTP方法 | URL | 前端文件 |
|------|-----------|----------|-----|----------|
| 1-5 | listRegister/getRegister/addRegister/updateRegister/delRegister | 各种 | `/app/register/*` | `moyun-admin-vue/src/api/app/register.js` |
| 6-10 | listClass/getClass/addClass/updateClass/delClass | 各种 | `/app/class/*` | `moyun-admin-vue/src/api/app/class.js` |
| 11 | `getVerify` | GET | `/app/custom/verifies` | `moyun-admin-vue/src/api/app/custom.js` |

#### 2.2.4 Flowable 工作流模块

前端有多个 flowable 相关 API，后端路径可能不一致：

| 序号 | 前端函数名 | HTTP方法 | URL | 前端文件 |
|------|-----------|----------|-----|----------|
| 1 | `deployStart` | GET | `/flowable/process/startFlow` | `moyun-admin-vue/src/api/flowable/process.js` |
| 2-6 | getDeployment/addDeployment/updateDeployment/delDeployment/exportDeployment | 各种 | `/system/deployment/*` | `moyun-admin-vue/src/api/flowable/process.js` |
| 7 | `definitionStart` | POST | `/flowable/definition/start` | `moyun-admin-vue/src/api/flowable/definition.js` |
| 8 | `getProcessVariables` | GET | `/flowable/task/processVariables` | `moyun-admin-vue/src/api/flowable/definition.js` |
| 9-10 | readXml/readImage | GET | `/flowable/definition/*` | `moyun-admin-vue/src/api/flowable/definition.js` |
| 11 | `getFlowViewer` | GET | `/flowable/task/flowViewer` | `moyun-admin-vue/src/api/flowable/definition.js` |
| 12 | `delegate` | POST | `/flowable/task/delegate` | `moyun-admin-vue/src/api/flowable/todo.js` |
| 13 | `delDeployment` (instance) | DELETE | `/flowable/instance/delete` | `moyun-admin-vue/src/api/flowable/finished.js` |
| 14 | `exportForm` | GET | `/flowable/form/export` | `moyun-admin-vue/src/api/flowable/form.js` |

> **说明**: 后端 Flowable 相关 Controller 确实存在，但路径可能是 `/flowable/` 开头而不是 `/system/` 开头，需要统一前缀。

#### 2.2.5 阅读模块后台管理 (portal/admin/*)

| 序号 | 前端函数名 | HTTP方法 | URL | 前端文件 |
|------|-----------|----------|-----|----------|
| 1-2 | getBookshelf/delBookshelf | GET/DELETE | `/portal/admin/bookshelf` | `moyun-admin-vue/src/api/portal/bookshelf.js` |
| 3 | `delBookChapterBatch` | DELETE | `/portal/admin/book-chapters/ids` | `moyun-admin-vue/src/api/portal/bookChapter.js` |
| 4 | `recountBookStats` | POST | `/portal/admin/book-chapters/recount` | `moyun-admin-vue/src/api/portal/bookChapter.js` |
| 5 | `delBookQuoteBatch` | DELETE | `/portal/admin/book-quotes/ids` | `moyun-admin-vue/src/api/portal/bookQuote.js` |
| 6 | `delBookBatch` | DELETE | `/portal/admin/books/ids` | `moyun-admin-vue/src/api/portal/book.js` |
| 7 | `delBookListBatch` | DELETE | `/portal/admin/book-lists/ids` | `moyun-admin-vue/src/api/portal/bookList.js` |

> **说明**: 后端对应 Controller 是 `PortalBookAdminController`、`PortalBookshelfAdminController` 等，类路径可能不是 `/portal/admin/` 开头，需核实。

---

## 三、P1 严重问题（路径/方法不匹配）

### 3.1 前台 Portal - 方法不匹配

| 序号 | 前端函数 | 前端方法 | 后端方法 | URL | 前端文件 | 后端文件 |
|------|---------|---------|---------|-----|----------|----------|
| 1 | `createArticle` | **POST** | **PUT** | `/portal/article` | `moyun-portal/src/api/article.ts:37` | `moyun-server/.../PortalArticleController.java:281` |

**问题说明**:
- 前端 `createArticle` 使用 POST 方法创建文章
- 后端 `PortalArticleController` 的无路径方法是 `@PutMapping`（对应 `edit` 方法）
- 后端创建文章是 `@PostMapping("/publish")` 和 `@PostMapping("/draft")`
- **结论**: 前后端创建文章的 API 设计不一致，前端缺少可用的创建文章接口

### 3.2 后台 Admin - 方法不匹配（说明）

后台有大量"方法不匹配"的报告，**其中绝大多数是扫描器误报**。

原因：后台 API 使用字符串拼接路径参数（如 `'/cms/article/' + id`），扫描器将路径参数部分截断后，URL 变成了 `/cms/article`，然后匹配到了 `POST /cms/article`（新增接口），而不是 `GET /cms/article/{id}`（详情接口）。

**需要人工核实的真实 P1 问题**:

| 序号 | 前端函数 | 前端方法 | URL | 说明 |
|------|---------|---------|-----|------|
| 1 | `getAuthRole` | GET | `/system/user/authRole/` | 前端 GET 获取角色，后端可能是 PUT 分配角色，需核实 |
| 2 | `auditCertification` | PUT | `/cms/creator/certification/` | 审核认证，需核实后端方法 |

---

## 四、P2 中等问题（可能的废弃接口 / 未使用的 API）

### 4.1 后端 Portal 接口但前台未调用（共 119 个）

以下是部分典型未使用接口（可能是废弃的或为未来预留的）：

| 序号 | 后端方法名 | HTTP方法 | URL | 后端文件 |
|------|-----------|----------|-----|----------|
| 1 | `purchaseArticle` | POST | `/portal/article/{id}/purchase` | `PortalArticleController.java` |
| 2 | `getMyPurchasedArticles` | GET | `/portal/article/my/purchased` | `PortalArticleController.java` |
| 3 | `getBookList` | GET | `/portal/reading/book-lists/{listId}` | `PortalBookListAdminController.java` |
| 4 | `likeQuote` | POST | `/portal/reading/quote/{quoteId}/like` | `PortalBookQuoteAdminController.java` |
| 5 | `joinBookClub` | POST | `/portal/reading/club/{id}/join` | `PortalBookClubController.java` |
| 6 | `leaveBookClub` | DELETE | `/portal/reading/club/{id}/leave` | `PortalBookClubController.java` |
| 7 | `addBookClubRecord` | POST | `/portal/reading/club/{id}/records` | `PortalBookClubController.java` |
| 8 | `likeClubRecord` | POST | `/portal/reading/club/records/{recordId}/like` | `PortalBookClubController.java` |
| 9 | `getGrowthLevel` | GET | `/portal/growth/level` | `PortalGrowthController.java` |
| 10 | `getGrowthRecords` | GET | `/portal/growth/records` | `PortalGrowthController.java` |
| ... | 还有约 109 个 | | | |

> **说明**: 这些接口可能是：
> 1. 已开发但前端尚未接入的功能
> 2. 历史废弃但未删除的接口
> 3. 为后台管理或其他系统预留的接口
> 
> 建议逐一确认是否需要保留。

### 4.2 后端 CMS 接口但后台未调用（共 71 个）

部分典型未使用接口：

| 序号 | 后端方法名 | HTTP方法 | URL | 后端文件 |
|------|-----------|----------|-----|----------|
| 1 | `exportArticle` | POST | `/cms/article/export` | `CmsArticleController.java` |
| 2 | `importArticle` | POST | `/cms/article/importData` | `CmsArticleController.java` |
| 3 | `listArticleSimple` | GET | `/cms/article/simple/list` | `CmsArticleController.java` |
| 4 | `getGrowthConfig` | GET | `/cms/growth/config` | `CmsGrowthController.java` |
| 5 | `updateGrowthConfig` | PUT | `/cms/growth/config` | `CmsGrowthController.java` |
| ... | 还有约 66 个 | | | |

---

## 五、路由与页面对齐情况

### 5.1 总体情况

- 路由总数: 约 60+ 个
- 页面文件总数: 约 65 个
- **有路由但无页面: 0 个** ✓
- **有页面但无路由: 1 个** (组件类)

### 5.2 页面文件清单

所有路由引用的页面文件均存在，分布如下：

```
src/pages/
├── HomePage.vue
├── ArticleDetailPage.vue
├── SearchPage.vue
├── ListPage.vue
├── UserPage.vue
├── UserProfilePage.vue
├── UserSettingsPage.vue
├── AuthorPage.vue
├── AuthorsPage.vue
├── HelpCenter.vue
├── AboutUs.vue
├── UserAgreement.vue
├── ReportFeedback.vue
├── LoginPage.vue
├── RegisterPage.vue
├── PublishPage.vue
├── MyArticlesPage.vue
├── MyConsumptionPage.vue
├── NotFoundPage.vue
├── ReadingPage.vue
├── GrowthRankingPage.vue
├── AchievementsPage.vue
├── FollowListPage.vue
├── MessagesPage.vue
├── FeedPage.vue
├── ColumnsPage.vue
├── ColumnDetailPage.vue
├── ColumnEditPage.vue
├── MyColumnsPage.vue
├── ContestListPage.vue
├── ContestDetailPage.vue
├── CreatorCenterPage.vue
├── CreatorCertificationPage.vue
├── InterviewPage.vue
├── reading/
│   ├── BookDetailPage.vue
│   ├── BookListDetailPage.vue
│   ├── ChapterReaderPage.vue
│   ├── MyBookshelfPage.vue
│   ├── DiscoverPage.vue
│   ├── BookClubListPage.vue
│   └── BookClubDetailPage.vue
├── interview/
│   ├── QuestionDetailPage.vue
│   ├── ExperienceDetailPage.vue
│   ├── ResumeTemplatePage.vue
│   ├── QuestionListPage.vue
│   ├── ExperienceListPage.vue
│   ├── ExperiencePublishPage.vue
│   ├── MyAttemptsPage.vue
│   ├── MyBookmarksPage.vue
│   ├── MyExperiencesPage.vue
│   ├── MyResumesPage.vue
│   ├── ResumeEditPage.vue
│   ├── CompanyPage.vue
│   ├── JobListPage.vue
│   ├── JobDetailPage.vue
│   └── MockInterviewPage.vue
├── learn/
│   ├── LearnCenterPage.vue
│   ├── StudyPlanPage.vue
│   ├── WrongBookPage.vue
│   ├── StudyCalendarPage.vue
│   ├── StudyCalendarCard.vue (组件，非页面)
│   ├── KnowledgeGraphPage.vue
│   ├── LeaderboardPage.vue
│   └── PkPage.vue
└── tools/
    └── CodeRunnerPage.vue
```

**结论**: 路由与页面对齐情况良好，没有缺失页面的路由。`StudyCalendarCard.vue` 是组件而非独立页面，属于正常情况。

---

## 六、重点问题详细分析

### 6.1 【P0-1】全站搜索接口缺失

- **前端**: `search()` → `GET /portal/search`
- **后端**: 无对应 Controller
- **影响**: 搜索功能不可用
- **建议**: 新增 `PortalSearchController` 或在现有 Controller 中添加搜索接口

### 6.2 【P0-2/3】上传接口路径不匹配

- **前端**: `uploadFile()` → `POST /upload/file`、`uploadImage()` → `POST /upload/image`
- **后端**: 
  - `FileUploadController` → `/file/upload`
  - `PortalFileController` → `/portal/file/upload`
- **影响**: 文件上传和图片上传功能 404
- **建议**: 统一上传路径，前端改为 `/portal/file/upload` 或后端新增 `/upload/*` 路径

### 6.3 【P0-4】短信验证码接口缺失

- **前端**: `sendSmsCode()` → `POST /portal/user/send-sms`
- **后端**: 无对应接口
- **影响**: 手机注册/登录功能不可用
- **建议**: 在 `PortalUserController` 或新增 `PortalSmsController` 中添加发送短信接口

### 6.4 【P1-1】创建文章方法不匹配

- **前端**: `createArticle()` → `POST /portal/article`
- **后端**: 
  - `PUT /portal/article` → `edit()` (更新文章)
  - `POST /portal/article/publish` → 发布文章
  - `POST /portal/article/draft` → 保存草稿
- **影响**: 创建文章功能 405 方法不允许
- **建议**: 
  - 方案一：前端调整为调用 `/portal/article/draft` 或 `/portal/article/publish`
  - 方案二：后端新增 `POST /portal/article` 作为通用创建接口

---

## 七、建议修复优先级

### 高优先级（立即修复）

1. ✅ **上传接口路径统一** - 影响所有上传功能
2. ✅ **创建文章 API 对齐** - 影响文章发布功能
3. ✅ **搜索接口确认** - 影响全站搜索

### 中优先级（近期修复）

1. 🔧 短信验证码接口确认/实现
2. 🔧 Flowable 工作流模块路径统一
3. 🔧 后台阅读管理模块路径确认

### 低优先级（后续清理）

1. 📝 梳理未使用的后端接口，标记废弃或删除
2. 📝 清理后台 App 模块遗留代码
3. 📝 统一代码生成工具接口路径

---

## 八、附录：扫描说明

### 扫描方法

1. **前端 API 提取**: 遍历 `moyun-portal/src/api/` 下所有 `.ts` 文件，提取 `httpGet/httpPost/httpPut/httpDelete/httpGetList/httpUpload` 调用
2. **后台 API 提取**: 遍历 `moyun-admin-vue/src/api/` 下所有 `.js` 文件，提取 `request({ url, method })` 调用
3. **后端接口提取**: 遍历所有 `*Controller.java` 文件，提取 `@RequestMapping` + `@GetMapping/@PostMapping/@PutMapping/@DeleteMapping`
4. **URL 匹配**: 标准化路径参数（`{id}`、`:id`、`${id}` 均统一为 `:param`）后进行匹配

### 已知局限性

1. 后台 API 使用字符串拼接路径参数时，扫描器可能无法正确解析完整路径，导致误报
2. 后端 `@RequestMapping` 不带 method 属性的接口未被统计
3. 部分接口可能通过拦截器/过滤器处理，不在 Controller 中定义
4. 动态路由/页面未被统计

### 报告文件

- 详细 JSON 报告: `api-alignment-report.json`
- 扫描脚本: `scan-api-alignment.py`
