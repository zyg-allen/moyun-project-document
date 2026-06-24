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
  


