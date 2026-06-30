# 墨韵智库 · 开发日志

本文件记录墨韵智库项目核心版本变更，作为代码提交与发布的参考记录。
日期采用 `YYYY-MM-DD` 格式。

---

## v4.0.5 (2026-06-26) 全面功能排查清单

### 主要变化

- 📋 新增：[10_功能排查清单.md](./10_功能排查清单.md) - 全面功能细节排查清单
  - 按"模块 > 页面 > 接口 > 组件"4 层级组织
  - 覆盖前台门户（17 模块 153 项）、后台管理（20 模块 178 项）、后端服务（9 模块 55 项），共 386 项测试点
  - 每项含核心业务规则、问题描述、测试结果、测试时间、遗留问题、关联问题
  - 汇总 12 个已知问题（BUG-A ~ BUG-L），按严重度分级
  - 提供测试执行顺序与修复优先级建议

### 调研中发现的新问题（BUG-A ~ BUG-L）

- 🟠 BUG-A：首页聚合 VO 字段错位（hotArticles 被赋值为 latestArticles）
- 🟡 BUG-B：作者列表关注按钮占位未接入
- 🟡 BUG-C：搜索页走 articleApi 而非 /portal/search
- 🟠 BUG-D：短信登录前端模拟未实现
- 🟡 BUG-E：第三方登录未实现
- 🟠 BUG-F：注册手机号字段语义错位（填入 email 字段）
- 🟡 BUG-G：账号注销仅前端模拟
- 🟠 BUG-H：tag 路由实际传 categoryName 参数
- 🟠 BUG-I：举报图片上传 UI 占位未接入
- 🟠 BUG-J：审核通知硬编码 userType=portal
- 🟠 BUG-K：关注成长事件 module 错误（传 article 而非 user）
- 🟠 BUG-L：VIP 加成仅对当前登录用户生效

### 相关文件

- `docs/10_功能排查清单.md`（新建）


---

## v4.0.4 (2026-06-26) 功能细节修复（审核通知 + 草稿自动保存 + 配置缓存 + 标签推荐）

### 主要变化

- 🐛 修复：审核结果通知作者闭环（BUG-002 遗留）
  - `CmsArticleServiceImpl.auditArticle` 审核通过/拒绝后发送站内信通知作者
  - 非阻塞设计：通知失败不影响审核主流程
  - `SysNotification` 类型 system，scope=user，userType=portal
- ✨ 新增：前台发布页草稿自动保存
  - `PublishPage.vue` 启动 30 秒定时器自动调用 `saveDraft(isAuto=true)`
  - 仅当标题或内容非空时才触发，避免空页面频繁保存
  - `onUnmounted` 清理定时器
- 🐛 修复：SysConfig 缓存加载/清空（TODO 空实现）
  - `selectConfigByKey` 先查 Redis 缓存（`sys:config:{key}`），未命中回源 DB 并回填
  - `loadingConfigCache`：全量加载 sys_config 到 Redis
  - `clearConfigCache`：删除所有 `sys:config:*` 键
- ⚡ 优化：标签推荐逻辑利用 title/category 参数
  - `TagQuery` 新增 `categoryId` 字段
  - `PortalTagMapper.xml` 添加 `categoryId` 关联查询（EXISTS portal_entity_tag + portal_article）
  - `PortalTagController.getRecommendTags` 动态构建缓存键，title 模糊匹配标签名，category 筛选该分类下文章使用过的标签

### 相关文件

- `moyun-server/.../CmsArticleServiceImpl.java`
- `moyun-portal/src/pages/PublishPage.vue`
- `moyun-server/.../SysConfigServiceImpl.java`
- `moyun-server/.../PortalTagController.java`
- `moyun-server/.../TagQuery.java`
- `moyun-server/.../PortalTagMapper.xml`

---

## v4.0.3 (2026-06-26) 文章模块优化（发布链路完整性 + 轮播图联动）

### 主要变化

- ⚡ 优化：前台发布页标签推荐防抖
  - `PublishPage.vue` 标签推荐 `watch` 每输入一字符即触发 `getRecommendTags` 请求，过于频繁
  - 新增 `scheduleTagSuggestions`（500ms 防抖 + 标题≥5 字符或已选分类才触发），`onUnmounted` 清理 pending timer
- ⚡ 优化：后端标签推荐 Redis 缓存
  - `PortalTagController.getRecommendTags` 每次查库且忽略 title/category 参数
  - 单一缓存键 `portal:tag:recommend:top10`，TTL 10 分钟；标签增/改/删时主动清除缓存
- 🐛 修复（P0）：后台发布作者信息空实现导致插入失败
  - `CmsArticleServiceImpl.fillAdminAuthorInfo` 原为空实现，DB `author_id NOT NULL`，未选作者时插入失败
  - 实现：前端已传 `authorId` 则尊重；否则 `SecurityUtils.getUserId()` 反查 `portal_user.user_id`，未关联则自动建户（携带 sys_user 基础信息，role=admin）；`insertArticle` 加 `@Transactional`
- ✨ 新增：后台发布 slug / category_path 维护（与前台链路对齐）
  - `fillCategoryPath`（复用 `IPortalCategoryService`）+ `fillSlug`/`generateSlugFromTitle`/`sanitizeSlug`/`ensureSlugUnique`（保证 `uk_slug` 唯一）
- ✨ 新增：后台 CMS 文章编辑页字段丰富化（`cms/article/edit.vue`）
  - 新增维护字段：`publishedAt`、`slug`、`isCategoryRecommended`
  - 轮播图动态联动：`isCarousel=true` 时 `cover` 动态必填 + 1920×600 提示；`false` 时可选 + 800×450 提示
  - `rules` 改为 `computed`，`cover` 必填性随 `isCarousel` 联动；`watch(isCarousel)` 切换时重校验封面
- 📊 评估：文章模块用户/管理员发布链路完整性（详见 `docs/07_更新日志.md` v4.0 评估章节）

### 验证

- 后端 `mvn -o compile -DskipTests`：BUILD SUCCESS
- Portal `npm run check`（vue-tsc -b）：exit 0，零类型错误
- Admin `npm run build:prod`：exit 0

### 相关文件

- `moyun-portal/src/pages/PublishPage.vue`
- `moyun-server/src/main/java/com/moyun/portal/controller/PortalTagController.java`
- `moyun-server/src/main/java/com/moyun/ext/cms/service/impl/CmsArticleServiceImpl.java`
- `moyun-admin-vue/src/views/cms/article/edit.vue`
- `docs/07_更新日志.md` / `docs/devlog.md`

---

## v4.0.2 (2026-06-26) 三端深度复查 P1 修复 + 文档维护

### 主要变化

- 🐛 修复：三端深度复查发现的 4 个 P1 问题
  - `38_init_report_feedback_menu.sql`：`portal_report.description` 列长 `varchar(1000)` → `varchar(2000)`，与实体 `@Size(max=2000)` 对齐（避免 1001-2000 字举报描述写入失败）
  - `40_fix_bugs_v4.sql`：新增 `ALTER TABLE portal_report MODIFY COLUMN description varchar(2000)` 供已部署环境升级
  - `AuthorPage.vue`：`loadAuthorData` 中已登录且非自己主页时调用 `followApi.checkFollow` 初始化 `isFollowing` 状态（修复已关注用户按钮显示错误 + 粉丝数不同步）
  - `notification/index.vue`：`remoteUserSearch` 的 `response.rows` → `response.data.records`（修复用户搜索下拉永远为空）
  - `notification/index.vue`：移除"系统用户（后台管理员）"选项，下拉固定为"门户用户"并 disabled（后端 `/cms/user/list` 只查 portal_user 表）
- 🧹 清理：删除 `api/cms/comment.js` 中无调用方的 `addComment` / `updateComment` 死代码
- 📚 文档：全面更新 `docs/` 目录文档（07_更新日志、06_问题修复、bugs/bug-list、devlog、05_测试清单），补充 `sql/README.md` 的 25-40 号脚本说明

### 验证

- 后端 `mvn clean compile -DskipTests`：BUILD SUCCESS
- Portal `npm run build`（含 vue-tsc 类型检查）：built in 23.22s，零类型错误
- Admin `npm run build:prod`：exit 0

### 相关文件

- `moyun-server/src/main/resources/sql/38_init_report_feedback_menu.sql`
- `moyun-server/src/main/resources/sql/40_fix_bugs_v4.sql`
- `moyun-portal/src/pages/AuthorPage.vue`
- `moyun-admin-vue/src/views/cms/notification/index.vue`
- `moyun-admin-vue/src/api/cms/comment.js`
- `docs/07_更新日志.md` / `docs/06_问题修复.md` / `docs/bugs/bug-list.md` / `docs/devlog.md` / `docs/05_测试清单.md`
- `moyun-server/src/main/resources/sql/README.md`

---

## v4.0.1 (2026-06-26) 关注/取消关注接口整合

### 主要变化

- 🐛 修复：`POST /portal/follow/{userId}` 返回 500 "Request method 'POST' is not supported"
  - 根因：前后端接口契约不匹配。前端 `follow.ts` 调用语义化 RESTful 路径，后端 `PortalFollowController` 只有 CRUD 接口（`@PostMapping` 无路径 + `@DeleteMapping("/{ids}")` 批量删除）
- 🔧 重构：`PortalFollowController` 改为语义化 RESTful 契约（与前端 `follow.ts` 完全对齐）
  - `POST /portal/follow/{userId}` 关注（幂等）
  - `DELETE /portal/follow/{userId}` 取消关注（幂等）
  - `GET /portal/follow/check/{userId}` 检查是否已关注
  - `GET /portal/follow/{userId}/followers` 粉丝列表（分页）
  - `GET /portal/follow/{userId}/following` 关注列表（分页）
- ✅ `IPortalFollowService` 新增 `follow` / `unfollow` / `selectFollowerPage` / `selectFollowingPage` 方法
- ✅ `PortalFollowServiceImpl` 实现：
  - 事务一致性：`@Transactional(rollbackFor = Exception.class)`，关注记录 + 统计计数 + 成长事件原子操作
  - 幂等性：已关注再关注/未关注再取消均不报错，直接返回当前状态
  - 复用 `toggleFollow` 的统计更新和成长事件逻辑
- ✅ `PortalSecurityConfig` 放开 `GET /portal/follow/check/**` 及 followers/following 公开访问（允许游客浏览作者主页）
- ✅ 保留旧接口 `toggle/{userId}` / `isFollowing/{userId}` 向后兼容

### 路由冲突分析

- `/check/{userId}`、`/toggle/{userId}`、`/isFollowing/{userId}` 为字面量段，Spring AntPathMatcher 优先匹配字面量
- `/{userId}` 为变量段，优先级低于字面量段
- 无路由冲突

### 验证

- 后端 `mvn clean compile -DskipTests`：BUILD SUCCESS

### 相关文件

- `moyun-server/src/main/java/com/moyun/portal/controller/PortalFollowController.java`
- `moyun-server/src/main/java/com/moyun/portal/service/IPortalFollowService.java`
- `moyun-server/src/main/java/com/moyun/portal/service/impl/PortalFollowServiceImpl.java`
- `moyun-server/src/main/java/com/moyun/portal/config/PortalSecurityConfig.java`

---

## v4.0.0 (2026-06-26) Dashboard 改造 + 举报反馈模块 + 4 个 Bug 修复

### 主要变化

- ✅ 新增：Dashboard 首页改造
  - `SysDashboardServiceImpl.buildTodoTasks()` 新增举报/反馈待办构建逻辑
  - 待办跳转路径规范化：`/cms/article/edit?id={id}`（query 形式）、通知 `/cms/notification`
  - `businessType` 从数字编码改为枚举名
  - `refreshCache()` 增加 ZSet 键清理 + `@PreAuthorize` 权限校验
  - 前端 `index.vue` 增加 loading 绑定和错误提示

- ✅ 新增：举报反馈模块（后台管理）
  - `CmsReportController` / `CmsFeedbackController`
  - `list` 返回 `TableDataInfo`，`handle` 用 `LambdaUpdateWrapper` 防篡改
  - `PortalReport` / `PortalFeedback` 实体增加校验注解
  - SQL `38_init_report_feedback_menu.sql` 创建菜单及权限

- 🐛 修复：4 个用户反馈 Bug
  - **Bug1**：个人中心页 mock 数据 → 改用 `growthApi.getUserStatsById` 真实聚合接口
  - **Bug2**：友情链接表格字段不显示 → `CmsFriendLinkVO` 补 `@Data` 注解 + SQL 统一 status 为 0/1
  - **Bug3**：通知菜单重复 → 删除 `SysNoticeController`/`SysNotificationController` 及残留菜单，保留 `cms/notification` 唯一入口
  - **Bug4**：评论管理 404 → SQL 修正菜单 path + 统一 perms + 补齐菜单及授权

- 📦 打包：v4.0 源码包（zip 格式，4.9M，排除 node_modules/target/dist/.git）

### 需执行的 SQL 脚本

1. `38_init_report_feedback_menu.sql` — 举报/反馈菜单
2. `39_init_flowable_menu.sql` — 流程管理菜单
3. `40_fix_bugs_v4.sql` — v4.0 综合 Bug 修复（友情链接 status + 通知菜单清理 + 评论菜单修复 + PortalReport 列长升级）

### 相关文件

- 后端：`SysDashboardServiceImpl.java`、`SysDashboardController.java`、`CmsReportController.java`、`CmsFeedbackController.java`、`PortalReport.java`、`PortalFeedback.java`、`CmsFriendLinkVO.java`
- Portal：`AuthorPage.vue`、`index.vue`
- Admin：`index.vue`、`report/index.vue`、`feedback/index.vue`、`friend-link/index.vue`
- SQL：`38_init_report_feedback_menu.sql`、`40_fix_bugs_v4.sql`、`01_moyun_init.sql`、`03_portal_init.sql`、`application.yaml`

---

## v2.1.1 (2026-06-16) Mapper XML resultMap 分层 + @Slf4j 编译修复

### 主要变化

- 🔧 修复：Cms*Result 混用前台 Entity 问题
  - `PortalArticleMapper.xml` 的 `CmsArticleResult` type 由 `PortalArticle` 改为 `CmsArticleVO`（CMS 后台视图对象，含 `authorNickname/authorUsername/authorAvatar/categoryName/categorySlug` 等 CMS 扩展字段）
  - 同步修改 `PortalArticleMapper` 接口：`selectCmsArticlePage` 返回 `Page<CmsArticleVO>`，`selectCmsArticleList` 返回 `List<CmsArticleVO>`，`selectCmsArticleById` 返回 `CmsArticleVO`
  - **修复参数类型不匹配**：`selectCmsArticlePage/List` 参数类型由 `ArticleQuery` 改为 `CmsArticleQuery`（与 Controller/Service 一致），参数名 `@Param("params")` 保持不变，XML 无需改动
  - 同步修改 `ICmsArticleService.selectArticleById` 返回 `CmsArticleVO`
  - 同步修改 `CmsArticleServiceImpl`：去掉 `BeanUtil.copyToList` 中转，mapper 直接返回 `Page<CmsArticleVO>`；同步清理 `cn.hutool.core.bean.BeanUtil` 冗余 import
  - **设计原则**：前台查询 resultMap 用前台 Entity（如 `PortalArticleResult`），CMS 后台管理查询 resultMap 用 CMS 专用视图对象（如 `CmsArticleResult` 对应 `CmsArticleVO`），不再混用

- 🐛 修复：`PortalInterviewResumeTemplateMapper.xml` 重复块
  - 删除行 54 起的重复 `resultMap id="PortalInterviewResumeTemplateResult"`（与行 7 重复）
  - 删除行 77 起的重复 `sql id="selectPortalInterviewResumeTemplateVo"`（与行 28 重复）
  - 保留行 7 的不完整 resultMap（含 19 个字段）和行 28 的不完整 sql 块（不含 `usage_guide/tags` 列）
  - 说明：当前 `PortalInterviewServiceImpl` 不调用 mapper XML 的自定义方法（全部走 MyBatis-Plus `selectPage/selectById`），mapper XML 方法为"死代码"，保留无副作用

- 🐛 修复：6 个 `@Slf4j` 缺 import 的真编译错误
  - `HttpHelper.java`：补 `import org.slf4j.Logger;` + `import org.slf4j.LoggerFactory;`（手动声明了 `LOGGER` 字段但缺 import）
  - `SysUserServiceImpl.java` / `UserDetailsServiceImpl.java` / `GenTableServiceImpl.java` / `FlowTaskListener.java` / `FlowableUtils.java`：补 `import lombok.extern.slf4j.Slf4j;`（注：不影响用户要求的"system 稳定模块不乱改"，仅补一行 import 让 `@Slf4j` 注解生效）

### 全链路比对结果

- **service 实际调用的 mapper XML 自定义方法**（排除 BaseMapper 通用方法）：
  - 前台：`selectPortalArticlePage/List/ById`、`selectHotArticles/FeaturedArticles/CarouselArticles/RelatedArticles/LatestArticles` → 全部映射 `PortalArticleResult`（type=`PortalArticle`）✓
  - 后台：`selectCmsArticlePage/List/ById` → 映射 `CmsArticleResult`（type=`CmsArticleVO`）✓
- **其他 11 个面试 mapper XML**（Question/Experience/Category/Company/Attempt/Comment/Submission/Bookmark/各种 Like/QuestionCompany/ResumeTemplate）：service 实际未调用，全部走 MyBatis-Plus，resultMap 混用风险**不存在**
- **其他 portal mapper**（Book/Category/Tag/Notification/Order/Wallet 等）：仅暴露 `selectPortalXxxPage/List/ById` 前台方法，无 Cms*Result 混用 ✓

### 验证

- `@Slf4j` 全量扫描：所有 40 个使用 `@Slf4j` 的文件都已正确 import `lombok.extern.slf4j.Slf4j`
- 手动 `Logger` 声明扫描：所有使用 `private static final Logger` 的文件都已正确 import `org.slf4j.Logger` + `org.slf4j.LoggerFactory`
- mapper XML resultMap type 扫描：所有 resultMap type 引用都已通过 javac 静态可达性验证

### 相关文件

- `moyun-server/src/main/resources/mapper/portal/PortalArticleMapper.xml`
- `moyun-server/src/main/resources/mapper/portal/PortalInterviewResumeTemplateMapper.xml`
- `moyun-server/src/main/java/com/moyun/portal/mapper/PortalArticleMapper.java`
- `moyun-server/src/main/java/com/moyun/ext/cms/service/ICmsArticleService.java`
- `moyun-server/src/main/java/com/moyun/ext/cms/service/impl/CmsArticleServiceImpl.java`
- `moyun-server/src/main/java/com/moyun/util/http/HttpHelper.java`
- `moyun-server/src/main/java/com/moyun/system/service/impl/SysUserServiceImpl.java`
- `moyun-server/src/main/java/com/moyun/core/security/auth/UserDetailsServiceImpl.java`
- `moyun-server/src/main/java/com/moyun/ext/generator/service/GenTableServiceImpl.java`
- `moyun-server/src/main/java/com/moyun/ext/flowable/listener/FlowTaskListener.java`
- `moyun-server/src/main/java/com/moyun/ext/flowable/flow/FlowableUtils.java`

---

## v2.1.0 (2026-06-15) 面试空间 + 通用标签模块

### 主要变化

- ✅ 新增：面试空间 13 张核心表
  - `portal_interview_category`（面试分类）
  - `portal_interview_question`（面试题目）
  - `portal_interview_submission`（提交记录）
  - `portal_interview_bookmark`（题目书签）
  - `portal_interview_question_like`（题目点赞）
  - `portal_interview_attempt`（答题尝试）
  - `portal_interview_experience`（面经文章）
  - `portal_interview_experience_like`（面经点赞）
  - `portal_interview_comment`（面试评论）
  - `portal_interview_comment_like`（评论点赞）
  - `portal_interview_resume_template`（简历模板）
  - `portal_interview_company`（面试公司）
  - `portal_interview_question_company`（题目-公司关联）

- ✅ 新增：通用标签系统
  - 新增表 `portal_entity_tag`（tag_id + entity_type + entity_id 多实体绑定）
  - 扩展表 `portal_tag`，新增字段 `module` / `reference_count` 及对应索引
  - 支持按模块（article/book/common）分类与热门标签查询

- 🔧 重构：分页工具链全局统一
  - `PortalInterviewController` 继承 `BaseController`，分页调用统一为 `startPage()` / `getDataTable()`
  - 删除 `CmsInterviewController` 中私有 `PageUtils` 内部类，统一调用 `com.moyun.util.bean.PageUtils`
  - 字段命名：表名统一为 `portal_interview_question`（单数），避免复数混用

- 🐛 修复：分页参数 `page` / `pageSize` / `pageNum` 兼容对齐
- 📚 文档：`docs/08_面试空间模块设计文档.md` 新增分页设计规范与通用标签系统章节
- 🔒 安全：所有敏感操作保持 `@PreAuthorize` + `@Log` 注解

### 相关文件

- `docs/sql/portal_module_ddl_v2.sql`
- `docs/sql/portal_module_init_v2.sql`
- `docs/08_面试空间模块设计文档.md`

---

## v2.0.0 (2026-06-01) 门户内容模块基线

- ✅ 新增：`portal_article`、`portal_article_tag`、`portal_article_view` 等核心表
- ✅ 新增：`portal_category`、`portal_tag`、`portal_comment`、`portal_like`、`portal_bookmark` 基础模块
- ✅ 新增：`portal_user`、`portal_follow` 等用户与社交基础表
- 🔧 统一：Entity 使用 `@Data` + `@TableName` 的 MyBatis-Plus 风格

