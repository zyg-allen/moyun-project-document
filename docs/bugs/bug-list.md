# 功能测试问题清单

## BUG-001: [首页] ✅ 已修复

- **模块**：  前台首页-获取热门标签列表接口 http://localhost:8080/portal/tag/hot?limit=20
- **严重程度**： 🟡 一般
- **复现步骤**：
  1. 打开 首页页面
  2. 点击热门标签更多 按钮
- **预期结果**：应该 引用次数最多的top 20
- **实际结果**：返回空
- sql: select * from portal_tag WHERE (reference_count is null or reference_count > 0) order by reference_count desc, sort asc, id asc limit 20
  
  问题描述和要求：reference_count引用次数统计没有实现，现在是作为几个模块公共标签，请在使用的模块实现新增修改删除等方法中加入绑定和解绑的动作，并追加或减少这个字段，注意要做好线程安全

  **修复说明**：
  根因：文章模块（前台发布 + 后台管理）完全没有接入标签绑定逻辑，且数据库 portal_tag 表缺少 module/reference_count 字段，portal_entity_tag 表未创建。
  
  修复内容：
  1. **数据库层**：
     - `03_portal_init.sql`：portal_tag 表 DDL 补充 module / reference_count 字段 + 索引
     - `30_alter_portal_tag_fields.sql`（新建）：为已存在的数据库 ALTER 补充字段（安全添加，可重复执行）
     - `31_create_portal_entity_tag.sql`（新建）：创建通用实体标签关联表（含 BaseEntity 审计字段）
  2. **实体层**：
     - `PortalArticle.java`：添加 tagIds / tagNames 非持久化字段（@TableField(exist = false)）
     - `ArticlePublishDTO.java`：添加 tagNames 字段（tagIds 已有）
  3. **前台 Controller**（PortalArticleController）：
     - publish：发布成功后调用 portalTagService.bindTags("article", ...)
     - edit：修改成功后调用 bindTags（内部计算差集，只增减变化的 tag）
     - remove：删除成功后调用 unbindTags（解绑并减少 reference_count）
  4. **后台 Controller**（CmsArticleController）：
     - add：新增成功后调用 bindTags
     - edit：修改成功后调用 bindTags
     - removeBatch / remove：删除成功后调用 unbindTags
  5. **线程安全**：
     - bindTags/unbindTags 已标注 @Transactional(rollbackFor = Exception.class)
     - reference_count 更新使用 COALESCE(reference_count, 0) + #{delta} 原子 SQL，MySQL 行锁保证并发安全
  
  **需执行的 SQL 脚本**（按顺序）：
  1. `30_alter_portal_tag_fields.sql` — 补充 portal_tag 表字段
  2. `31_create_portal_entity_tag.sql` — 创建 portal_entity_tag 表
  
  修复文件清单（8 个）：
  - moyun-server/src/main/resources/sql/03_portal_init.sql（修改）
  - moyun-server/src/main/resources/sql/30_alter_portal_tag_fields.sql（新建）
  - moyun-server/src/main/resources/sql/31_create_portal_entity_tag.sql（新建）
  - moyun-server/src/main/java/com/moyun/portal/domain/entity/PortalArticle.java（修改）
  - moyun-server/src/main/java/com/moyun/portal/domain/dto/ArticlePublishDTO.java（修改）
  - moyun-server/src/main/java/com/moyun/portal/controller/PortalArticleController.java（修改）
  - moyun-server/src/main/java/com/moyun/ext/cms/controller/CmsArticleController.java（修改）


## BUG-002: [发布页] ✅ 已修复
- **模块**：  前台发布页，
- **严重程度**： 🟢 低
- **复现步骤**：
  1. 打开 发布页页面   
- **预期结果**：应该 显示标签列表 前端加#展示，
  - 封面图片：应该宽高要适中，不是轮播图，而是单张图片，方正显示，限制图片大小和格式，现在的太长了，影响用户体验。
  - 标签：应该显示标签列表，前端加#展示，标签列表应该可以搜索，可以输入，可以删除，可以添加，
  - 文章内容，编辑器为markdown格式，应该支持markdown格式。并且有预览功能。不是代码模式，而是markdown模式。用户可以直观看到效果；
  - 保存草稿，要能够保存草稿，真实入库并返回草稿包含id，标题，内容，标签，封面图片，创建时间，更新时间，删除时间，在草稿列表展示，用户可以查看草稿详情，也可以删除草稿。，继续编辑
  - 发布文章，要能够发布文章，真实入库并返回文章包含id，标题，内容，标签，封面图片，创建时间，更新时间，删除时间，在文章列表展示，用户可以查看文章详情，也可以删除文章。，继续编辑；
  - 发布文章加上后台人工审核，审核通过后，文章状态变更为已发布，否则状态变更为已拒绝。通知方式告知用户，状态为草稿状态；后期加AI审核
  - 文章状态：草稿状态，已发布状态，已拒绝状态
  - 还有哪些功能需要完善？？帮给出建议
  - 所有修改的内容基于现在的功能代码，涉及到数据库操作，注意要好做好线程安全、记录好脚本；涉及到后端代码要修改或者补充接口；涉及到后台管理要修改页面或者补充接口。
- 

  **修复说明**：
  根因：发布页标签展示未拼接 `#`、封面图比例过长、Markdown 编辑器无预览、保存草稿为模拟未真实入库、发布直接为 published 无审核流程。

  修复内容：
  1. **标签展示拼接 `#`**（前端）：
     - `PublishPage.vue`：已选标签、搜索结果、智能推荐、热门标签 4 处展示统一拼接 `#` 前缀
     - 配合 BUG-001 的标签 `#` 前缀清理（数据库存纯文本，前端按需拼接 `#`）
  2. **封面图方正显示**（前端）：
     - `PublishPage.vue`：`h-40 sm:h-48`（长条形）→ `aspect-square max-w-xs`（1:1 方正，最大宽度限制）
     - `accept` 限制为 `image/jpeg,image/png,image/webp,image/gif`
     - 文件大小限制 5MB → 2MB（方正封面无需大图）
     - 增加格式与尺寸提示文案
  3. **Markdown 编辑器实时预览**（前端）：
     - `MarkdownEditor.vue`：新增"预览"切换按钮，支持分栏（左编辑 / 右预览）模式
     - 内置简易 Markdown 渲染函数（支持标题、粗体、斜体、删除线、引用、代码块、行内代码、有序/无序列表、链接、图片、分割线）
     - 移动端自动改为上下堆叠布局
  4. **保存草稿真实入库**（后端 + 前端）：
     - 后端 `IPortalArticleService.saveDraft`（新增）：真实入库，返回含 id 的实体
     - 后端 `PortalArticleController.saveDraft`（新增 `/portal/article/draft` 接口）：草稿保存并绑定标签
     - 前端 `article.ts`：新增 `saveDraft` API
     - 前端 `PublishPage.vue`：`saveDraft` 改为真实调用接口，记录 `draftId` 实现新建/更新
  5. **发布文章审核流程**（后端 + 前端）：
     - 后端 `publishArticle`：默认状态 `published` → `pending`（待审核）
     - 后端 `PortalArticleController.publish`：返回 `{id, status, publishedAt, message}` 详情
     - 前端 `PublishPage.vue`：发布后状态显示为"审核中"，提示"发布成功，等待审核"
  6. **状态枚举补充**（数据库）：
     - `33_alter_article_status_enum.sql`（新建）：`status` 字段注释补充 `draft/pending/published/rejected/archived` 枚举说明
     - 状态流转：draft → pending → published/rejected → pending（重新提交）→ archived

  **需执行的 SQL 脚本**：
  1. `33_alter_article_status_enum.sql` — 补充 status 字段状态枚举注释

  修复文件清单（8 个）：
  - moyun-portal/src/pages/PublishPage.vue（修改：标签拼接#、封面方正、草稿/发布接入真实接口）
  - moyun-portal/src/components/MarkdownEditor.vue（修改：新增实时预览分栏模式）
  - moyun-portal/src/api/article.ts（修改：新增 publishArticle / saveDraft API）
  - moyun-server/src/main/java/com/moyun/portal/service/IPortalArticleService.java（修改：新增 saveDraft 方法）
  - moyun-server/src/main/java/com/moyun/portal/service/impl/PortalArticleServiceImpl.java（修改：实现 saveDraft，publishArticle 改为 pending）
  - moyun-server/src/main/java/com/moyun/portal/controller/PortalArticleController.java（修改：新增 /draft 接口，/publish 返回详情）
  - moyun-server/src/main/resources/sql/33_alter_article_status_enum.sql（新建）

  **后续待完善（建议）**：
  - ✅ 草稿列表页：已新建 `MyArticlesPage.vue`（`/my/articles`），支持状态筛选（全部/草稿/待审核/已发布/已拒绝）、查看、继续编辑、重新提交、删除
  - ✅ 后台审核接口：已优化 `auditArticle`（状态校验 + 乐观锁 + remark 拒绝原因），admin 端 `article/index.vue` 新增"通过/拒绝"按钮 + 拒绝原因弹窗
  - ✅ 前台"我的文章"接口：新增 `GET /portal/article/my`（按 authorId + status 查询，authorId 强制取当前登录用户防越权）
  - ⏳ 审核结果通知：可通过站内信 / 邮件通知用户审核结果（待实现）
  - ⏳ 自动保存：当前 saveDraft 支持手动调用，可加定时器实现自动保存草稿（待实现）

---

## BUG-003: [文章审核闭环] ✅ 已修复
- **模块**：后台审核 + 前台我的文章
- **严重程度**：🟡 中
- **背景**：BUG-002 引入了文章审核流程（draft → pending → published/rejected），但后台无审核入口、前台作者无法查看自己所有状态的文章。
- **修复内容**：

  ### 后端（4 个文件）
  1. **`CmsArticleServiceImpl.auditArticle`**（修改）：
     - 新增状态校验：仅允许审核为 `published` / `rejected`
     - 新增前置校验：仅 `pending` 状态的文章可被审核（防止重复审核）
     - 乐观锁：`WHERE status = 'pending'` 防止并发审核冲突
     - 审核通过设置 `publishedAt`；拒绝时将原因写入 `remark` 字段
  2. **`PortalArticleMapper`**（修改）：
     - 新增 `selectMyArticlesPage` 方法 + `queryWhereMy` 条件片段
     - 强制 `author_id` 过滤，`status` 可选，不强制 `published`（作者可查看自己所有状态文章）
  3. **`IPortalArticleService` / `PortalArticleServiceImpl`**（修改）：
     - 新增 `selectMyArticlesPage`，校验 `authorId` 必填
  4. **`PortalArticleController`**（修改）：
     - 新增 `GET /portal/article/my` 接口，`authorId` 强制取当前登录用户（防越权）

  ### 前端 admin 端（1 个文件）
  - **`moyun-admin-vue/src/views/cms/article/index.vue`**（修改）：
    - 状态枚举补充 `pending`（待审核）/ `rejected`（已拒绝）
    - 操作列新增"通过 / 拒绝"按钮（仅 `pending` 状态显示）
    - 拒绝时弹窗输入原因，写入 `remark` 字段
    - 复用现有 `auditArticle` API（`/cms/article/audit`）

  ### 前端 portal 端（3 个文件）
  - **`moyun-portal/src/pages/MyArticlesPage.vue`**（新建）：
    - 独立"我的文章"页面，路由 `/my/articles`
    - 状态 Tab 筛选（全部/草稿/待审核/已发布/已拒绝）
    - 文章卡片：封面、标题、状态徽章、摘要、拒绝原因、时间、分类、浏览/点赞
    - 操作：查看、继续编辑、重新提交审核、删除
  - **`moyun-portal/src/router/index.ts`**（修改）：注册 `/my/articles` 路由（需登录）
  - **`moyun-portal/src/api/article.ts`**（修改）：新增 `getMyArticles` API
  - **`moyun-portal/src/pages/UserPage.vue`**（修改）：个人中心"我的文章"区块新增"查看全部"入口

  **审核状态流转**：
  ```
  draft（草稿）→ pending（待审核）→ published（已发布）/ rejected（已拒绝）
                                              ↓
                                    rejected → pending（重新提交）
  ```

---

## BUG-004: [通知系统] ✅ 已修复
- **模块**：系统通知（前台门户 + 后台 CMS + 后台系统公告）
- **严重程度**：🔴 高
- **背景**：
  原有两套独立通知表，模型冲突且功能缺陷：
  1. `portal_notification`（per-user 模型）：被前台 `PortalNotificationController` 和后台 `CmsNotificationController` 共享操作
  2. `sys_notice`（全局广播模型）：RuoYi 内置，`markRead`/`markReadAll` 为 TODO 空实现，无 per-user 已读能力
  3. 前台 `notification.ts` 调用了 3 个后端不存在的端点（`unread-count`、`/{id}/read`、`mark-all-read`）→ 标记已读功能 404
  4. `CmsNotificationServiceImpl.sendSystemNotification` 用 for 循环逐条 insert（性能差）

- **修复方案**：采用主流平台（掘金/知乎/GitHub）的两表结构
  - `sys_notification`（通知主体表）：存内容，`scope=user` 个人 / `scope=all` 广播
  - `sys_notification_read`（用户已读关系表）：存已读记录，`NOT EXISTS` 计算未读
  - 废弃 `portal_notification` 和 `sys_notice`（数据迁移后重命名为 `_bak` 备份）

- **修复内容**：

  ### 数据库（1 个脚本）
  - **`34_merge_notification_tables.sql`**（新建）：
    1. 创建 `sys_notification`（主体表）+ `sys_notification_read`（已读关系表）
    2. 迁移 `portal_notification` 历史数据（→ scope=user，保留 user_id，is_read=1 同步写入已读关系表）
    3. 迁移 `sys_notice` 历史数据（→ scope=all，user_id=NULL，notice_type 保留）
    4. 旧表重命名为 `_bak` 备份（不直接 DROP，便于回滚）
    5. 校验结果

  ### 后端新建（6 个文件）
  - **`SysNotification.java`**（Entity）：合并字段并集（type/title/content/data/scope/user_id/notice_type/status + BaseEntity）
  - **`SysNotificationRead.java`**（Entity）：notification_id + user_id + read_time + 唯一索引
  - **`SysNotificationMapper.java` + XML**：含 `selectUnreadByUserId`（NOT EXISTS）、`selectAllByUserId`（LEFT JOIN 计算已读）、`countUnreadByUserId`
  - **`SysNotificationReadMapper.java` + XML**：`markAsRead`（INSERT IGNORE 防重复）、`markAllAsRead`（批量 INSERT IGNORE）
  - **`ISysNotificationService` / `SysNotificationServiceImpl`**：统一服务层，含后台管理 + 前台用户方法

  ### 后端改造（3 个 Controller）
  - **`PortalNotificationController`**（改造）：
    - 内部改用 `ISysNotificationService`
    - 补齐 3 个缺失端点：`GET /unread-count`、`POST /{id}/read`、`POST /mark-all-read`
    - 用户身份强制取当前登录用户（防越权）
  - **`CmsNotificationController`**（改造）：
    - 内部改用 `ISysNotificationService`
    - `send-all` 群发改为 `scope=all` 单条记录（替代原逐条 insert，性能优化）
    - 新增 `scope` 筛选支持
  - **`SysNoticeController`**（改造）：
    - 内部改用 `ISysNotificationService`（操作 `sys_notification` 表）
    - 保留原 `/system/notice` 路径和权限点（对 RuoYi 框架透明）
    - 实现 `markRead` / `markReadAll`（原为 TODO 空实现）
    - 新增公告强制 `scope=all`

  ### 后端清理（删除 13 个废弃文件）
  - `PortalNotification.java` / `PortalNotificationMapper.java` + XML / `IPortalNotificationService.java` / `PortalNotificationServiceImpl.java`
  - `SysNotice.java` / `SysNoticeMapper.java` + XML / `ISysNoticeService.java` / `SysNoticeServiceImpl.java`
  - `ICmsNotificationService.java` / `CmsNotificationServiceImpl.java` / `CmsNotificationVO.java` / `CmsNotificationQuery.java`

  ### 前端（2 个文件）
  - **`moyun-admin-vue/src/views/system/notice/index.vue`**（修改）：
    - 字段映射：`noticeId` → `id`、`noticeTitle` → `title`、`noticeContent` → `content`
    - 查询参数、表格列、表单、重置、增删改查全部适配新字段
  - **`moyun-admin-vue/src/views/cms/notification/index.vue`**（修改）：
    - 查询条件：移除 `isRead`，新增 `scope`（个人/广播筛选）
    - 表格列：`已读状态` → `通知范围` + `接收用户`（广播显示"所有用户"）
    - 通知类型新增 `notice`/`announcement` 选项
    - 查看详情：移除已读状态，改为通知范围 + 接收用户

  **核心查询逻辑**：
  ```sql
  -- 未读通知（个人 + 广播，排除已读）
  SELECT n.* FROM sys_notification n
  WHERE n.status = '0'
    AND ((n.scope = 'user' AND n.user_id = #{userId}) OR n.scope = 'all')
    AND NOT EXISTS (
      SELECT 1 FROM sys_notification_read r
      WHERE r.notification_id = n.id AND r.user_id = #{userId}
    )
  ORDER BY n.create_time DESC

  -- 标记已读（INSERT IGNORE 防重复）
  INSERT IGNORE INTO sys_notification_read (notification_id, user_id, read_time, create_time)
  VALUES (#{notificationId}, #{userId}, NOW(), NOW())
  ```

  **需执行的 SQL 脚本**：
  1. `34_merge_notification_tables.sql` — 建表 + 迁移 + 备份旧表

  **架构对比**：
  | 维度 | 修复前 | 修复后 |
  |------|--------|--------|
  | 表数量 | 2 张（portal_notification + sys_notice） | 2 张（sys_notification + sys_notification_read） |
  | 广播已读 | ❌ 无法实现 | ✅ NOT EXISTS 计算 |
  | 群发性能 | 逐条 insert（N 条） | 单条主体记录（1 条） |
  | 前台标记已读 | 404（端点不存在） | ✅ INSERT IGNORE |
  | 后台 markRead | TODO 空实现 | ✅ 已实现 |
  | RuoYi 兼容 | - | ✅ 路径/权限/字典保留 |

---

## BUG-005: [通知用户体系] ✅ 已修复
- **模块**：通知系统用户身份区分
- **严重程度**：🔴 高
- **背景**：
  BUG-004 合并通知表后，`sys_notification.user_id` 无法区分 ID 属于 `portal_user` 还是 `sys_user`：
  1. 两表 ID 可能重叠，导致后台管理员无法接收个人通知
  2. 后台管理员走 `sys_user` 体系（`SecurityUtils`），但通知查询全部走 `PortalSecurityUtils`（门户用户）
  3. 系统要给管理员发"审批待办""系统告警"等通知时，没有对应的查询接口

- **修复方案**：新增 `user_type` 字段区分两套用户体系
  - `user_type = portal`：门户用户（portal_user 表）
  - `user_type = sys`：系统用户（sys_user 表，后台管理员）
  - 广播通知（scope=all）对两类用户都可见，已读状态按 user_type 独立计算

- **修复内容**：

  ### 数据库（2 个脚本）
  - **`35_add_notification_user_type.sql`**（新建）：
    1. `sys_notification` 新增 `user_type` 字段（scope=user 时标识接收方类型）
    2. `sys_notification_read` 新增 `user_type` 字段（标识已读记录归属）
    3. 调整唯一索引：`uk_notification_user` → `uk_notification_user_type`（notification_id + user_id + user_type）
    4. 历史数据默认设为 `portal`
  - **`36_init_notification_center_menu.sql`**（新建）：
    后台管理员通知中心菜单初始化（系统管理 > 通知中心）

  ### 后端改造
  - **Entity**：`SysNotification` + `SysNotificationRead` 新增 `userType` 字段
  - **Mapper XML**：
    - `SysNotificationMapper.xml`：resultMap/查询/insert/update 全部加入 user_type
    - 用户查询（selectUnreadByUserId/selectAllByUserId/countUnreadByUserId）加 `#{userType}` 参数
    - `SysNotificationReadMapper.xml`：markAsRead/markAllAsRead/countByNotificationAndUser 加 `#{userType}` 参数
  - **Service**：
    - `ISysNotificationService` + `SysNotificationServiceImpl`：所有用户查询方法加 `userType` 参数
    - `insertNotification`：个人通知默认 `user_type=portal`，广播通知强制 `user_type=null`
    - `sendBroadcastNotification`：广播通知 `user_type=null`（对两类用户都可见）
  - **Controller**：
    - `PortalNotificationController`（改造）：所有调用传 `USER_TYPE_PORTAL = "portal"`
    - `SysNotificationController`（新建）：后台管理员通知中心，`/system/notification` 路径，用 `SecurityUtils.getUserId()` + `USER_TYPE_SYS = "sys"`
    - `CmsNotificationController.add`：个人通知默认 `userType=portal`（兼容旧调用方）

  ### 前端 admin（2 个文件）
  - **`api/system/notification.js`**（新建）：listMyNotification/getUnreadCount/markAsRead/markAllAsRead/getNotification
  - **`views/system/notification/index.vue`**（新建）：后台管理员通知中心页面
    - 通知列表（个人 + 广播，含已读状态）
    - 未读数实时显示
    - 标记单条已读 / 全部已读
    - 查看详情（未读通知查看时自动标记已读）

  **用户体系区分逻辑**：
  ```
  门户用户(portal)：
    登录入口：/portal/auth/login
    身份获取：PortalSecurityUtils.getUser()
    通知查询：GET /portal/notification/list（user_type=portal）
    通知接口：PortalNotificationController

  系统用户(sys)：
    登录入口：/login（RuoYi）
    身份获取：SecurityUtils.getUserId()
    通知查询：GET /system/notification/list（user_type=sys）
    通知接口：SysNotificationController

  广播通知(scope=all)：
    对 portal 和 sys 用户都可见
    已读状态按 user_type 独立计算（互不影响）
  ```

  **需执行的 SQL 脚本**：
  1. `35_add_notification_user_type.sql` — 新增 user_type 字段 + 调整唯一索引
  2. `36_init_notification_center_menu.sql` — 后台通知中心菜单初始化（**注：v4.0 已废弃并删除**，见 BUG-009）

---

## BUG-006: [个人中心页 mock 数据] ✅ 已修复
- **模块**：前台门户 - 个人中心页
- **严重程度**：🔴 高
- **复现步骤**：
  1. 访问 `http://localhost:3000/author/3`
  2. 查看作者统计区（总浏览/获赞/文章数等）
- **预期结果**：展示后端真实聚合数据
- **实际结果**：总浏览等数据为前端随机 mock 或文章列表前端累加，与后端真实值不一致
- **根因**：`AuthorPage.vue` 未调用后端统计聚合接口，而是在前端累加文章列表的 views/likes（仅能反映已加载文章，非全量）；同时 `totalViews=0` 时计算 `totalLikes/totalViews` 产生 NaN%
- **修复内容**：
  1. 移除文章列表前端累加 `totalViews`/`totalLikes` 的逻辑，仅保留 `articles = list.length` 作为初始值
  2. 改用 `growthApi.getUserStatsById(authorId)` 调用真实聚合接口 `GET /portal/growth/stats?userId=xxx`
  3. 字段映射对齐后端 `UserStatsVO`（articles/views/likes/totalLikes/followers/following）
  4. 修复 NaN%：`authorStats.totalViews > 0 ? Math.round((authorStats.totalLikes / authorStats.totalViews) * 100) : 0`
- **修复文件**：
  - `moyun-portal/src/pages/AuthorPage.vue`
- **关联接口**：`GET /portal/growth/stats`（`PortalSecurityConfig` 已 permitAll，游客可访问）

---

## BUG-007: [友情链接表格字段不显示] ✅ 已修复
- **模块**：后台管理 - 友情链接
- **严重程度**：🔴 高
- **复现步骤**：
  1. 访问 `http://localhost/cms/cms/friend-link`
  2. 查看表格列表
- **预期结果**：显示网站名称/地址/描述/Logo/排序/状态等字段
- **实际结果**：表格字段全部空白，仅显示编号和创建时间
- **根因**：`CmsFriendLinkVO` 虽然 `import lombok.Data`，但**类上漏加 `@Data` 注解**。Lombok 的 `@Data` 不会继承到子类，导致 VO 自定义字段（id/name/url/description/logo/sort/status）没有 getter/setter，Jackson 序列化时只输出从 BaseEntity 继承的字段（createBy/createTime/updateBy/updateTime/remark）
- **修复内容**：
  1. `CmsFriendLinkVO` 补充 `@Data` + `@EqualsAndHashCode(callSuper = true)` 注解
  2. SQL `40_fix_bugs_v4.sql` 统一 `portal_friend_link.status` 为 `0/1`（原 `active/inactive`，与前端字典不匹配）
  3. `03_portal_init.sql` 建表脚本同步修改 status 默认值和注释
- **修复文件**：
  - `moyun-server/src/main/java/com/moyun/ext/cms/domain/vo/CmsFriendLinkVO.java`
  - `moyun-server/src/main/resources/sql/40_fix_bugs_v4.sql`
  - `moyun-server/src/main/resources/sql/03_portal_init.sql`

---

## BUG-008: [后台通知菜单重复] ✅ 已修复
- **模块**：后台管理 - 通知系统
- **严重程度**：🟠 中
- **复现步骤**：
  1. 登录后台管理系统
  2. 查看左侧菜单
- **预期结果**：只有一个通知管理入口（`/cms/notification`）
- **实际结果**：存在 3 个通知相关菜单：通知公告（`/system/notice`，404）、通知中心（`/system/notification`，404）、通知管理（`/cms/notification`，正常）。前两个为多余入口
- **根因**：BUG-004/005 合并通知表后，旧的 `SysNoticeController`（通知公告）和 `SysNotificationController`（通知中心）的菜单数据未清理，但 Controller 和前端页面已在之前的提交中删除，导致菜单点击 404
- **修复内容**：
  1. 删除残留的 `SysNoticeController` / `SysNotificationController`（确认已不存在）
  2. 删除残留的前端文件：`views/system/notice/`、`views/system/notification/`、`api/system/notice.js`、`api/system/notification.js`（确认已不存在）
  3. 删除 SQL 脚本 `36_init_notification_center_menu.sql`
  4. `01_moyun_init.sql` 清理 menu_id=107（通知公告）及按钮 1035-1038 的 INSERT 和 sys_role_menu 授权
  5. `40_fix_bugs_v4.sql` 中 DELETE 删除数据库中残留的通知公告/通知中心菜单及角色授权
  6. `application.yaml` xss excludes 从 `/system/notice` 改为 `/cms/notification`
  7. `38_init_report_feedback_menu.sql` 更新过时注释
  8. 保留 `cms/notification`（通知管理）作为唯一通知入口
- **修复文件**：
  - `moyun-server/src/main/resources/sql/01_moyun_init.sql`
  - `moyun-server/src/main/resources/sql/38_init_report_feedback_menu.sql`
  - `moyun-server/src/main/resources/sql/40_fix_bugs_v4.sql`
  - `moyun-server/src/main/resources/application.yaml`
- **说明**：`sys_notice` 建表语句保留在 `01_moyun_init.sql` 中，因为 `34_merge_notification_tables.sql` 依赖它做数据迁移（RENAME 为 `sys_notice_bak`）

---

## BUG-009: [评论管理页面 404] ✅ 已修复
- **模块**：后台管理 - 评论管理
- **严重程度**：🔴 高
- **复现步骤**：
  1. 访问 `http://localhost/cms/cms/comment`
- **预期结果**：显示评论管理列表页
- **实际结果**：404
- **根因**：数据库 `sys_menu` 中评论管理菜单的 `path` 字段值不正确（动态路由拼接规则：一级目录 `/{path}`，二级菜单 `/{parentPath}/{childPath}`，path 应为 `comment` 才能拼出 `/cms/comment`）；同时权限标识 `cms:comment:audit` 与 Controller 的 `@PreAuthorize` 不一致
- **修复内容**：
  1. SQL `40_fix_bugs_v4.sql` 修正菜单 path 为 `comment`
  2. 统一权限标识 `cms:comment:audit → cms:comment:edit`（与 `CmsCommentController` 的 `@PreAuthorize` 对齐）
  3. `INSERT ... WHERE NOT EXISTS` 确保评论管理菜单及按钮（query/edit/remove）存在
  4. 为 admin 角色（role_id=1）授权
- **修复文件**：
  - `moyun-server/src/main/resources/sql/40_fix_bugs_v4.sql`
- **关联验证**：`CmsCommentController` 的 `@PreAuthorize`（list/query/edit/remove）与 SQL 菜单 perms 完全一致；前端 `comment.js` 的请求路径与 Controller 一致

---

## BUG-010: [关注/取消关注接口 500] ✅ 已修复
- **模块**：前台门户 - 关注功能
- **严重程度**：🔴 高
- **复现步骤**：
  1. 登录门户
  2. 访问作者主页 `http://localhost:3000/author/2`
  3. 点击"关注"按钮
- **预期结果**：关注成功，按钮变为"已关注"
- **实际结果**：`POST /portal/follow/2` 返回 500 "Request method 'POST' is not supported"
- **根因**：前后端接口契约不匹配。前端 `follow.ts` 调用语义化 RESTful 路径 `POST /portal/follow/{userId}`，但后端 `PortalFollowController` 只有 `@PostMapping`（无路径，期望 body 传 PortalFollow）和 `@DeleteMapping("/{ids}")`（批量删除），没有 `@PostMapping("/{userId}")`
- **修复内容**：
  1. 重构 `PortalFollowController` 为语义化 RESTful 契约：
     - `POST /portal/follow/{userId}` 关注（幂等）
     - `DELETE /portal/follow/{userId}` 取消关注（幂等）
     - `GET /portal/follow/check/{userId}` 检查是否已关注
     - `GET /portal/follow/{userId}/followers` 粉丝列表（分页）
     - `GET /portal/follow/{userId}/following` 关注列表（分页）
  2. `IPortalFollowService` 新增 `follow` / `unfollow` / `selectFollowerPage` / `selectFollowingPage` 方法
  3. `PortalFollowServiceImpl` 实现：事务一致性（关注记录 + 统计计数 + 成长事件原子操作）、幂等性逻辑（已关注再关注/未关注再取消均不报错）
  4. `PortalSecurityConfig` 放开 `GET /portal/follow/check/**` 及 followers/following 公开访问
  5. 保留旧接口 `toggle/{userId}` / `isFollowing/{userId}` 向后兼容
- **修复文件**：
  - `moyun-server/src/main/java/com/moyun/portal/controller/PortalFollowController.java`
  - `moyun-server/src/main/java/com/moyun/portal/service/IPortalFollowService.java`
  - `moyun-server/src/main/java/com/moyun/portal/service/impl/PortalFollowServiceImpl.java`
  - `moyun-server/src/main/java/com/moyun/portal/config/PortalSecurityConfig.java`

---

## BUG-011: [三端深度复查发现的问题] ✅ 已修复
- **模块**：跨模块（后端 + Portal + Admin）
- **严重程度**：🟠 中
- **背景**：v4.0 修复完成后对三端做深度复查，发现 4 个 P1 问题
- **修复内容**：

  ### P1-1：PortalReport.description 校验与 DB 列长不一致
  - **问题**：实体 `@Size(max=2000)`，建表为 `varchar(1000)`，1001-2000 字的举报描述会写入失败
  - **修复**：`38_init_report_feedback_menu.sql` 改为 `varchar(2000)`，`40_fix_bugs_v4.sql` 新增 `ALTER TABLE portal_report MODIFY COLUMN description varchar(2000)`

  ### P1-2：AuthorPage.vue 遗漏 checkFollow 调用
  - **问题**：已关注用户访问作者主页时按钮显示"关注"而非"已关注"，点击后前端 `followers++` 导致显示数比服务端多 1
  - **修复**：`loadAuthorData` 中已登录且非自己主页时调用 `followApi.checkFollow({ userId: authorId })` 初始化 `isFollowing`

  ### P1-3：notification 用户搜索下拉永远为空
  - **问题**：`remoteUserSearch` 用 `response.rows` 取数据，但 `/cms/user/list` 返回的是 `AjaxResult` 包装的 `Page`，正确路径是 `response.data.records`
  - **修复**：`response.rows || []` → `response.data.records || []`

  ### P1-4：notification 用户类型 sys 无后端接口
  - **问题**：表单提供"系统用户"选项，但 `/cms/user/list` 只查 portal_user 表
  - **修复**：移除 sys 选项，下拉固定为"门户用户"并 disabled

  ### P2：comment.js 死代码
  - **问题**：`addComment` / `updateComment` 两个导出函数无调用方，且后端无对应端点
  - **修复**：删除这两个函数

- **修复文件**：
  - `moyun-server/src/main/resources/sql/38_init_report_feedback_menu.sql`
  - `moyun-server/src/main/resources/sql/40_fix_bugs_v4.sql`
  - `moyun-portal/src/pages/AuthorPage.vue`
  - `moyun-admin-vue/src/views/cms/notification/index.vue`
  - `moyun-admin-vue/src/api/cms/comment.js`
