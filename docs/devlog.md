# 墨韵智库 · 开发日志

本文件记录墨韵智库项目核心版本变更，作为代码提交与发布的参考记录。
日期采用 `YYYY-MM-DD` 格式。

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

