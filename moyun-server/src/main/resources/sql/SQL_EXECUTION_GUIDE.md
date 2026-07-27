# 墨韵智库 v5.9 P1 - 业务主键 business_id 改造执行说明

> 解决自增 ID 作为表间关联外键的隐患：父表 TRUNCATE 后自增 ID 重置，导致子表历史外键关联错乱。

## 一、改造背景

### 问题
当前所有表使用自增 ID 作为物理主键，并通过自增 ID 进行表间关联（如 `portal_comment.article_id` → `portal_article.id`）。

**隐患场景**：
1. 执行 `TRUNCATE TABLE portal_category` 后，分类表的自增 ID 从 1 重置
2. 新建的分类获得 id=1, 2, 3...
3. 但文章表 `portal_article.category_id` 中历史数据仍指向旧的 id=1, 2, 3
4. 结果：文章关联到了错误的分类，数据错乱

### 解决方案
为父表新增 `business_id` 业务主键（VARCHAR(32)），子表新增 `*_business_id` 外键列。
- **双轨过渡**：保留自增 id 作为物理主键，business_id 作为业务关联键
- 新数据由 Service 层双写两列
- 旧数据由 SQL 迁移脚本回填
- 查询逐步切换到 business_id 后，再考虑废弃旧 id 列

## 二、改造范围（P1 第一批）

### 父表（6 张，加 business_id + 唯一索引）

| 表名 | 前缀 | 示例 business_id |
|---|---|---|
| portal_user | `usr_` | `usr_1751234567890_a3b2c1` |
| sys_user | `sysu_` | `sysu_1751234567890_b2c3d4` |
| portal_article | `art_` | `art_1751234567890_c3d4e5` |
| portal_category | `cat_` | `cat_1751234567890_d4e5f6` |
| portal_comment | `com_` | `com_1751234567890_e5f6a7` |
| portal_tag | `tag_` | `tag_1751234567890_f6a7b8` |

### 子表（12 张，新增 *_business_id 外键列 + 索引）

| 子表 | 新增外键列 |
|---|---|
| portal_article | author_business_id, category_business_id, root_category_business_id |
| portal_comment | article_business_id, author_business_id, parent_business_id, root_business_id |
| portal_category | parent_business_id（自引用） |
| portal_like | user_business_id, article_business_id |
| portal_bookmark | user_business_id, article_business_id |
| portal_comment_like | comment_business_id, user_business_id |
| portal_article_view | article_business_id, user_business_id |
| portal_article_version | article_business_id, operator_business_id |
| portal_article_tag | article_business_id, tag_business_id |
| portal_tip_order | user_business_id, author_business_id, target_business_id（多态） |
| portal_feed_event | user_business_id, target_business_id（多态） |
| portal_report | user_business_id, target_business_id（多态） |

## 三、执行步骤

### 步骤 1：备份数据库

```bash
# 强烈建议先备份，再执行迁移
mysqldump -u root -p moyun-db > backup_before_business_id_$(date +%Y%m%d).sql
```

### 步骤 2：执行迁移脚本

```bash
# 方式一：命令行执行
mysql -u root -p moyun-db < 97_add_business_id_p1.sql

# 方式二：MySQL 客户端中执行
mysql -u root -p moyun-db
mysql> source /path/to/97_add_business_id_p1.sql;
```

**脚本特点**：
- 幂等：可重复执行，已存在的列/索引会跳过
- 事务：整体在一个事务内，失败自动回滚
- 自动回填：历史数据通过 JOIN 回填 business_id

**执行时间预估**：
- 数据量 < 1 万行：< 10 秒
- 数据量 1-10 万行：10-60 秒
- 数据量 > 10 万行：建议分批执行（见下方"大数据量优化"）

### 步骤 3：验证迁移结果

脚本末尾会自动输出验证统计，关注两个指标：

```sql
-- 1. 父表 business_id 是否全部回填（null_count 应为 0）
SELECT 'portal_user' AS tbl, COUNT(*) AS total, SUM(business_id IS NULL) AS null_count FROM portal_user
UNION ALL SELECT 'portal_article', COUNT(*), SUM(business_id IS NULL) FROM portal_article
-- ... 其他父表

-- 2. 子表外键是否回填（null_count 应接近 0，少数 NULL 表示父表记录已删除的孤儿数据）
SELECT 'portal_article.author_business_id' AS col, COUNT(*) AS total, SUM(author_business_id IS NULL) AS null_count FROM portal_article
-- ... 其他子表
```

**预期结果**：
- 父表 null_count = 0（全部回填）
- 子表 null_count 接近 0（少数 NULL 是父表记录已删除的孤儿数据，可接受）

### 步骤 4：重启应用

迁移脚本执行完成后，重启应用使新代码生效：
```bash
# 停止应用
# 部署新 jar 包
# 启动应用
```

### 步骤 5：功能验证

重点测试以下场景：
1. **用户注册**：新用户应生成 `usr_xxx` 格式的 business_id
2. **发布文章**：新文章应生成 `art_xxx`，并双写 author/category 的 business_id
3. **发表评论**：新评论应生成 `com_xxx`，并双写 article/author/parent/root 的 business_id
4. **新建分类**：新分类应生成 `cat_xxx`，并双写 parent 的 business_id
5. **点赞/收藏**：新点赞/收藏记录应双写 user/article 的 business_id

验证 SQL：
```sql
-- 检查新数据是否生成 business_id
SELECT id, business_id, username FROM portal_user ORDER BY id DESC LIMIT 5;
SELECT id, business_id, title, author_business_id, category_business_id FROM portal_article ORDER BY id DESC LIMIT 5;
SELECT id, business_id, article_business_id, author_business_id FROM portal_comment ORDER BY id DESC LIMIT 5;
```

## 四、回滚方案

如果改造出现问题，执行回滚脚本：

```bash
mysql -u root -p moyun-db < 97_rollback_business_id_p1.sql
```

**回滚脚本会**：
- 删除所有 P1 新增的 business_id 列（包括父表和子表）
- 列删除时会自动级联删除相关索引

**回滚后**：
- 数据库恢复到改造前状态
- 需要回滚应用代码到 P1 改造前的版本

## 五、大数据量优化（> 10 万行）

如果单表数据量超过 10 万行，直接执行全表 UPDATE 回填可能导致长时间锁表。建议：

### 方案 A：分批回填

将脚本中的 UPDATE 语句改为分批执行，示例：

```sql
-- 分批回填 portal_article 的 business_id（每批 1 万行）
-- 注意：需多次执行，直到 affected_rows = 0
UPDATE portal_article
SET business_id = CONCAT('art_', UNIX_TIMESTAMP(NOW(3)) * 1000 + FLOOR(RAND() * 1000), '_', LPAD(HEX(FLOOR(RAND() * 16777216)), 6, '0'))
WHERE business_id IS NULL
ORDER BY id
LIMIT 10000;
```

### 方案 B：使用 pt-online-schema-change

```bash
# 使用 Percona Toolkit 在线添加列（不锁表）
pt-online-schema-change --alter "ADD COLUMN business_id VARCHAR(32) NULL, ADD UNIQUE INDEX uk_business_id (business_id)" D=moyun-db,t=portal_article --execute
```

### 方案 C：临时关闭 binlog

```sql
-- 在迁移期间临时关闭 binlog（仅大表回填时）
SET SESSION sql_log_bin = 0;
-- 执行迁移
SET SESSION sql_log_bin = 1;
```

## 六、双轨过渡策略

### 当前状态（P1 完成后）
- 父表：自增 id（物理主键）+ business_id（业务主键，唯一索引）
- 子表：旧自增 id 外键（如 article_id）+ 新 business_id 外键（如 article_business_id）
- 查询：仍走旧 id 外键（JOIN ON a.id = c.article_id）
- 写入：Service 层双写两列

### 后续切换路径（P2-P3，本次不做）
1. **P2 查询切换**：Mapper.xml 的 JOIN 条件从 `ON a.id = c.article_id` 改为 `ON a.business_id = c.article_business_id`
2. **P3 子表外键列清理**：确认所有查询都走 business_id 后，删除旧的外键列（如 article_id）
3. **P4 父表主键切换**（可选）：将 business_id 设为新主键，自增 id 降级为普通索引

## 七、影响面评估

### 已改造（P1 范围）
- 6 张父表 + 12 张子表的实体类、Mapper.xml、Service
- 4 张多态关联表（tip_order/feed_event/report + topic_comment 未改，P2 补）

### 未改造（P2/P3 范围，本次不做）
- 其他 38 张父表（书籍、专栏、面试、话题、圈子等模块）
- 多态关联表的 target_id 改造（portal_feed_event 的非 article 类型）
- 查询 JOIN 条件切换

### 兼容性
- **向后兼容**：旧 id 列保留，旧查询逻辑不受影响
- **零停机**：迁移脚本可在线执行（小数据量），不阻塞读写
- **可回滚**：提供完整回滚脚本

## 八、文件清单

### 新增文件
- `sql/97_add_business_id_p1.sql` - 迁移脚本
- `sql/97_rollback_business_id_p1.sql` - 回滚脚本
- `java/com/moyun/util/uuid/BusinessIdGenerator.java` - 业务主键生成器
- `sql/SQL_EXECUTION_GUIDE.md` - 本说明文档

### 修改文件（实体类，共 18 个）
- 父表实体：PortalUser, SysUser, PortalArticle, PortalCategory, PortalComment, PortalTag
- 子表实体：PortalArticle, PortalComment, PortalCategory, PortalLike, PortalBookmark, PortalCommentLike, PortalArticleView, PortalArticleVersion, PortalArticleTag, PortalTipOrder, PortalFeedEvent, PortalReport

### 修改文件（Mapper.xml，共 14 个）
- PortalUserMapper.xml, SysUserMapper.xml
- PortalArticleMapper.xml, PortalCategoryMapper.xml, PortalCommentMapper.xml, PortalTagMapper.xml
- PortalBookmarkMapper.xml, PortalCommentLikeMapper.xml, PortalArticleTagMapper.xml
- PortalTipOrderMapper.xml, PortalFeedEventMapper.xml
- PortalLikeMapper.xml（无自定义 SQL，跳过）

### 修改文件（Service/Controller，共 7 个）
- PortalUserServiceImpl.java - insertPortalUser, registerPortalUser
- PortalCategoryServiceImpl.java - insertPortalCategory
- PortalTagServiceImpl.java - insertPortalTag
- PortalArticleServiceImpl.java - publishArticle, saveDraft
- PortalCommentServiceImpl.java - insertPortalComment, toggleLike
- PortalBookmarkServiceImpl.java - insertPortalBookmark
- PortalArticleController.java - toggleLikeArticle

## 九、常见问题

### Q1：迁移脚本执行报错 "Duplicate entry for key 'uk_business_id'"
**原因**：回填时 RAND() 生成的随机后缀碰撞（概率极低）
**解决**：重新执行脚本（幂等），或手动为冲突行设置唯一值

### Q2：子表外键回填后仍有 NULL
**原因**：父表记录已被逻辑删除（del_flag='2'），但子表外键仍指向旧 id
**解决**：可接受，这些是孤儿数据。如需清理，执行：
```sql
-- 查找孤儿数据
SELECT c.* FROM portal_comment c
LEFT JOIN portal_article a ON c.article_id = a.id
WHERE a.id IS NULL AND c.article_id IS NOT NULL;
```

### Q3：应用启动报错 "Unknown column 'business_id'"
**原因**：迁移脚本未执行，但代码已部署
**解决**：先执行迁移脚本，再部署代码

### Q4：business_id 太长影响性能
**评估**：VARCHAR(32) 实际长度约 26 字符，比 BIGINT(8) 大但可接受
- 索引性能：InnoDB B+ 树对 VARCHAR 索引性能良好
- 半有序设计（时间戳前缀）避免页分裂
- 如性能敏感，后续可考虑改用 BIGINT + 业务编码映射表

### Q5：为什么不直接用 UUID？
**原因**：
- UUID 36 字符太长，索引体积大
- UUID 完全无序，InnoDB 聚簇索引页分裂严重
- UUID 无业务含义，日志排查困难
- 本方案的 `{前缀}_{时间戳}_{随机}` 格式半有序、可读、紧凑

---

**版本**：v5.9 P1
**作者**：墨韵开发团队
**日期**：2026-07-27
