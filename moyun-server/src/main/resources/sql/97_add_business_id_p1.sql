-- ============================================
-- 墨韵智库 - 业务主键 business_id 改造迁移脚本（v5.9 P1）
-- ============================================
-- 目的：
--   为核心父表新增 business_id 业务主键列（VARCHAR(32)），并回填历史数据；
--   为子表新增 *_business_id 外键列，回填与父表的关联。
--   双轨过渡：保留自增 id 作为物理主键，business_id 作为业务关联键。
--   避免父表 TRUNCATE 后自增 ID 重置导致子表关联错乱。
--
-- 改造范围（P1 第一批 - 用户体系 + 文章体系全链路）：
--   父表（6 张，加 business_id 列 + 唯一索引）：
--     - portal_user      门户用户（核心，被 46 张表引用）
--     - sys_user         后台用户
--     - portal_article   文章（被 10 张表引用）
--     - portal_category  分类（自引用树）
--     - portal_comment    评论（自引用树）
--     - portal_tag       标签（多对多）
--
--   子表（13 张，新增 *_business_id 外键列 + 索引，回填关联）：
--     - portal_article          author_business_id, category_business_id, root_category_business_id
--     - portal_comment          article_business_id, author_business_id, parent_business_id, root_business_id
--     - portal_like             user_business_id, article_business_id
--     - portal_bookmark         user_business_id, article_business_id
--     - portal_comment_like     comment_business_id, user_business_id
--     - portal_article_view     article_business_id, user_business_id
--     - portal_article_version  article_business_id, operator_business_id
--     - portal_article_tag      article_business_id, tag_business_id
--     - portal_category         parent_business_id（自引用）
--     - portal_tip_order        user_business_id, author_business_id, target_business_id（多态）
--     - portal_feed_event       user_business_id, target_business_id（多态）
--     - portal_report           user_business_id, target_business_id（多态）
--     - sys_user                dept_id 已存在，本批次不改 sys_user 关联
--
-- business_id 格式：
--   {前缀}_{13位毫秒时间戳}_{6位Base62随机后缀}
--   示例：art_1751234567890_a3b2c1
--   长度：约 25-27 字符，VARCHAR(32) 足够
--
-- 幂等性：
--   所有 ADD COLUMN 前先检查 information_schema.columns，已存在则跳过；
--   所有 ADD INDEX 前先检查 information_schema.statistics，已存在则跳过；
--   UPDATE 回填前判断字段是否已存在且不为空。
--   脚本可重复执行，不会报错。
--
-- 兼容性：
--   本脚本不使用存储过程和 DELIMITER 命令，兼容所有 MySQL 客户端
--   （mysql CLI / Navicat / DataGrip / DBeaver 等），可直接整段执行。
--   使用预处理语句（PREPARE/EXECUTE）实现幂等性，可重复执行。
--
-- 执行方式：
--   mysql -u root -p moyun-db < 97_add_business_id_p1.sql
--   或在 DataGrip / Navicat / DBeaver 中直接整段执行（无需配置分隔符）
--
-- 回滚方式：
--   执行 97_rollback_business_id_p1.sql
--
-- 性能建议：
--   - 数据量 < 10 万行：直接执行本脚本，事务内完成
--   - 数据量 >= 10 万行：建议分批回填（每次 1 万行），避免长事务锁表
--   - 索引创建在大数据量下建议用 ALGORITHM=INPLACE, LOCK=NONE
-- ============================================

SET NAMES utf8mb4;
-- 注意：DDL（ALTER TABLE / CREATE INDEX）在 MySQL 中会隐式提交当前事务，
--       因此本脚本不使用显式事务包裹 DDL 部分。回填 UPDATE 由各语句自身原子性保证。

-- ============================================
-- 第一阶段：为父表添加 business_id 列 + 唯一索引
-- ============================================

-- 1.1 portal_user（门户用户）
-- portal_user.business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_user' AND column_name = 'business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_user` ADD COLUMN `business_id` VARCHAR(32) NULL COMMENT ''业务主键（前缀usr_，TRUNCATE后仍可关联子表）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_user uk_business_id
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_user' AND index_name = 'uk_business_id');
SET @s := IF(@i = 0, 'CREATE UNIQUE INDEX `uk_business_id` ON `portal_user` (`business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 sys_user（后台用户）
-- sys_user.business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_user` ADD COLUMN `business_id` VARCHAR(32) NULL COMMENT ''业务主键（前缀sysu_）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- sys_user uk_business_id
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'uk_business_id');
SET @s := IF(@i = 0, 'CREATE UNIQUE INDEX `uk_business_id` ON `sys_user` (`business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 portal_article（文章）
-- portal_article.business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article' AND column_name = 'business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article` ADD COLUMN `business_id` VARCHAR(32) NULL COMMENT ''业务主键（前缀art_）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article uk_business_id
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article' AND index_name = 'uk_business_id');
SET @s := IF(@i = 0, 'CREATE UNIQUE INDEX `uk_business_id` ON `portal_article` (`business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 portal_category（分类）
-- portal_category.business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_category' AND column_name = 'business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_category` ADD COLUMN `business_id` VARCHAR(32) NULL COMMENT ''业务主键（前缀cat_）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_category uk_business_id
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_category' AND index_name = 'uk_business_id');
SET @s := IF(@i = 0, 'CREATE UNIQUE INDEX `uk_business_id` ON `portal_category` (`business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.5 portal_comment（评论）
-- portal_comment.business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND column_name = 'business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_comment` ADD COLUMN `business_id` VARCHAR(32) NULL COMMENT ''业务主键（前缀com_）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment uk_business_id
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND index_name = 'uk_business_id');
SET @s := IF(@i = 0, 'CREATE UNIQUE INDEX `uk_business_id` ON `portal_comment` (`business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.6 portal_tag（标签）
-- portal_tag.business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_tag' AND column_name = 'business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_tag` ADD COLUMN `business_id` VARCHAR(32) NULL COMMENT ''业务主键（前缀tag_）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_tag uk_business_id
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_tag' AND index_name = 'uk_business_id');
SET @s := IF(@i = 0, 'CREATE UNIQUE INDEX `uk_business_id` ON `portal_tag` (`business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 第二阶段：为子表添加 *_business_id 外键列 + 普通索引
-- ============================================
-- 注意：子表外键列不加 UNIQUE，因为多个子表记录可能引用同一父表（如一篇文章有多条评论）
-- 多对多中间表（article_tag）的联合唯一索引需单独处理

-- 2.1 portal_article（作为子表：作者 + 分类 + 顶级分类）
-- portal_article.author_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article' AND column_name = 'author_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article` ADD COLUMN `author_business_id` VARCHAR(32) NULL COMMENT ''作者业务主键（关联 portal_user.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article.category_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article' AND column_name = 'category_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article` ADD COLUMN `category_business_id` VARCHAR(32) NULL COMMENT ''分类业务主键（关联 portal_category.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article.root_category_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article' AND column_name = 'root_category_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article` ADD COLUMN `root_category_business_id` VARCHAR(32) NULL COMMENT ''顶级分类业务主键（关联 portal_category.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article idx_author_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article' AND index_name = 'idx_author_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_author_bid` ON `portal_article` (`author_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article idx_category_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article' AND index_name = 'idx_category_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_category_bid` ON `portal_article` (`category_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article idx_root_category_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article' AND index_name = 'idx_root_category_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_root_category_bid` ON `portal_article` (`root_category_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 portal_comment（作为子表：文章 + 作者 + 父评论 + 根评论）
-- portal_comment.article_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND column_name = 'article_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_comment` ADD COLUMN `article_business_id` VARCHAR(32) NULL COMMENT ''文章业务主键（关联 portal_article.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment.author_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND column_name = 'author_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_comment` ADD COLUMN `author_business_id` VARCHAR(32) NULL COMMENT ''作者业务主键（关联 portal_user.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment.parent_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND column_name = 'parent_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_comment` ADD COLUMN `parent_business_id` VARCHAR(32) NULL COMMENT ''父评论业务主键（自引用 portal_comment.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment.root_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND column_name = 'root_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_comment` ADD COLUMN `root_business_id` VARCHAR(32) NULL COMMENT ''根评论业务主键（自引用 portal_comment.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment idx_article_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND index_name = 'idx_article_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_article_bid` ON `portal_comment` (`article_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment idx_author_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND index_name = 'idx_author_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_author_bid` ON `portal_comment` (`author_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment idx_parent_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND index_name = 'idx_parent_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_parent_bid` ON `portal_comment` (`parent_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment idx_root_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND index_name = 'idx_root_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_root_bid` ON `portal_comment` (`root_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.3 portal_category（作为子表：父分类 - 自引用）
-- portal_category.parent_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_category' AND column_name = 'parent_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_category` ADD COLUMN `parent_business_id` VARCHAR(32) NULL COMMENT ''父分类业务主键（自引用 portal_category.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_category idx_parent_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_category' AND index_name = 'idx_parent_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_parent_bid` ON `portal_category` (`parent_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.4 portal_like（点赞：用户 + 文章）
-- portal_like.user_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_like' AND column_name = 'user_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_like` ADD COLUMN `user_business_id` VARCHAR(32) NULL COMMENT ''用户业务主键（关联 portal_user.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_like.article_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_like' AND column_name = 'article_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_like` ADD COLUMN `article_business_id` VARCHAR(32) NULL COMMENT ''文章业务主键（关联 portal_article.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_like idx_user_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_like' AND index_name = 'idx_user_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_user_bid` ON `portal_like` (`user_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_like idx_article_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_like' AND index_name = 'idx_article_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_article_bid` ON `portal_like` (`article_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.5 portal_bookmark（收藏：用户 + 文章）
-- portal_bookmark.user_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_bookmark' AND column_name = 'user_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_bookmark` ADD COLUMN `user_business_id` VARCHAR(32) NULL COMMENT ''用户业务主键（关联 portal_user.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_bookmark.article_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_bookmark' AND column_name = 'article_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_bookmark` ADD COLUMN `article_business_id` VARCHAR(32) NULL COMMENT ''文章业务主键（关联 portal_article.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_bookmark idx_user_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_bookmark' AND index_name = 'idx_user_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_user_bid` ON `portal_bookmark` (`user_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_bookmark idx_article_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_bookmark' AND index_name = 'idx_article_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_article_bid` ON `portal_bookmark` (`article_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.6 portal_comment_like（评论点赞：评论 + 用户）
-- portal_comment_like.comment_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_comment_like' AND column_name = 'comment_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_comment_like` ADD COLUMN `comment_business_id` VARCHAR(32) NULL COMMENT ''评论业务主键（关联 portal_comment.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment_like.user_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_comment_like' AND column_name = 'user_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_comment_like` ADD COLUMN `user_business_id` VARCHAR(32) NULL COMMENT ''用户业务主键（关联 portal_user.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment_like idx_comment_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_comment_like' AND index_name = 'idx_comment_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_comment_bid` ON `portal_comment_like` (`comment_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_comment_like idx_user_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_comment_like' AND index_name = 'idx_user_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_user_bid` ON `portal_comment_like` (`user_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.7 portal_article_view（浏览：文章 + 用户）
-- portal_article_view.article_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article_view' AND column_name = 'article_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article_view` ADD COLUMN `article_business_id` VARCHAR(32) NULL COMMENT ''文章业务主键（关联 portal_article.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article_view.user_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article_view' AND column_name = 'user_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article_view` ADD COLUMN `user_business_id` VARCHAR(32) NULL COMMENT ''用户业务主键（关联 portal_user.business_id，NULL=游客）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article_view idx_article_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article_view' AND index_name = 'idx_article_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_article_bid` ON `portal_article_view` (`article_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article_view idx_user_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article_view' AND index_name = 'idx_user_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_user_bid` ON `portal_article_view` (`user_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.8 portal_article_version（版本：文章 + 操作人）
-- portal_article_version.article_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article_version' AND column_name = 'article_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article_version` ADD COLUMN `article_business_id` VARCHAR(32) NULL COMMENT ''文章业务主键（关联 portal_article.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article_version.operator_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article_version' AND column_name = 'operator_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article_version` ADD COLUMN `operator_business_id` VARCHAR(32) NULL COMMENT ''操作人业务主键（关联 portal_user.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article_version idx_article_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article_version' AND index_name = 'idx_article_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_article_bid` ON `portal_article_version` (`article_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article_version idx_operator_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article_version' AND index_name = 'idx_operator_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_operator_bid` ON `portal_article_version` (`operator_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.9 portal_article_tag（多对多中间表：文章 + 标签）
-- 注意：原 uk_article_tag(article_id, tag_id) 唯一索引保留，新增 business_id 列的联合唯一索引
-- portal_article_tag.article_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article_tag' AND column_name = 'article_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article_tag` ADD COLUMN `article_business_id` VARCHAR(32) NULL COMMENT ''文章业务主键（关联 portal_article.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article_tag.tag_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article_tag' AND column_name = 'tag_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article_tag` ADD COLUMN `tag_business_id` VARCHAR(32) NULL COMMENT ''标签业务主键（关联 portal_tag.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article_tag idx_article_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article_tag' AND index_name = 'idx_article_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_article_bid` ON `portal_article_tag` (`article_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article_tag idx_tag_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article_tag' AND index_name = 'idx_tag_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_tag_bid` ON `portal_article_tag` (`tag_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_article_tag uk_article_tag_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_article_tag' AND index_name = 'uk_article_tag_bid');
SET @s := IF(@i = 0, 'CREATE UNIQUE INDEX `uk_article_tag_bid` ON `portal_article_tag` (`article_business_id`, `tag_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.10 portal_tip_order（打赏/付费阅读订单：用户 + 作者 + 目标多态）
-- target_type=article/article_paid 时 target_business_id 关联 portal_article.business_id
-- target_type=column 时 target_business_id 关联 portal_column.business_id（P2 批次补）
-- portal_tip_order.user_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_tip_order' AND column_name = 'user_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_tip_order` ADD COLUMN `user_business_id` VARCHAR(32) NULL COMMENT ''打赏者业务主键（关联 portal_user.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_tip_order.author_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_tip_order' AND column_name = 'author_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_tip_order` ADD COLUMN `author_business_id` VARCHAR(32) NULL COMMENT ''被打赏者业务主键（关联 portal_user.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_tip_order.target_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_tip_order' AND column_name = 'target_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_tip_order` ADD COLUMN `target_business_id` VARCHAR(32) NULL COMMENT ''目标业务主键（多态：根据 target_type 关联不同父表）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_tip_order idx_user_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_tip_order' AND index_name = 'idx_user_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_user_bid` ON `portal_tip_order` (`user_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_tip_order idx_author_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_tip_order' AND index_name = 'idx_author_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_author_bid` ON `portal_tip_order` (`author_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_tip_order idx_target_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_tip_order' AND index_name = 'idx_target_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_target_bid` ON `portal_tip_order` (`target_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.11 portal_feed_event（动态事件：用户 + 目标多态）
-- target_type=article/experience/column/book/topic 时 target_business_id 关联对应父表
-- portal_feed_event.user_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_feed_event' AND column_name = 'user_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_feed_event` ADD COLUMN `user_business_id` VARCHAR(32) NULL COMMENT ''发布者业务主键（关联 portal_user.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_feed_event.target_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_feed_event' AND column_name = 'target_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_feed_event` ADD COLUMN `target_business_id` VARCHAR(32) NULL COMMENT ''目标业务主键（多态：根据 target_type 关联不同父表）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_feed_event idx_user_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_feed_event' AND index_name = 'idx_user_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_user_bid` ON `portal_feed_event` (`user_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_feed_event idx_target_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_feed_event' AND index_name = 'idx_target_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_target_bid` ON `portal_feed_event` (`target_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.12 portal_report（举报：用户 + 目标多态）
-- target_type=comment/article/user 时 target_business_id 关联对应父表
-- portal_report.user_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_report' AND column_name = 'user_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_report` ADD COLUMN `user_business_id` VARCHAR(32) NULL COMMENT ''举报人业务主键（关联 portal_user.business_id）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_report.target_business_id
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_report' AND column_name = 'target_business_id');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_report` ADD COLUMN `target_business_id` VARCHAR(32) NULL COMMENT ''目标业务主键（多态：根据 target_type 关联不同父表）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_report idx_user_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_report' AND index_name = 'idx_user_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_user_bid` ON `portal_report` (`user_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- portal_report idx_target_bid
SET @i := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'portal_report' AND index_name = 'idx_target_bid');
SET @s := IF(@i = 0, 'CREATE INDEX `idx_target_bid` ON `portal_report` (`target_business_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 第三阶段：回填父表 business_id（仅回填 NULL 的行，避免覆盖已生成值）
-- ============================================
-- 回填格式：{prefix}_{id padded}_{6位hex}
-- 使用行 id 作为时间戳部分（保证全局唯一，避免 RAND() 冲突），
-- 配合 6 位 hex 随机后缀增加熵，与 Java 端 BusinessIdGenerator 格式兼容（查询时不依赖具体值）

-- 3.1 portal_user（前缀 usr_）
UPDATE portal_user
SET business_id = CONCAT('usr_',
                         LPAD(id, 13, '0'), '_',
                         LPAD(HEX(FLOOR(RAND() * 16777216)), 6, '0'))
WHERE business_id IS NULL;

-- 3.2 sys_user（前缀 sysu_）
-- 注意：sys_user 主键列名为 user_id（非 id），RuoYi 框架惯例
UPDATE sys_user
SET business_id = CONCAT('sysu_',
                         LPAD(user_id, 13, '0'), '_',
                         LPAD(HEX(FLOOR(RAND() * 16777216)), 6, '0'))
WHERE business_id IS NULL;

-- 3.3 portal_article（前缀 art_）
UPDATE portal_article
SET business_id = CONCAT('art_',
                         LPAD(id, 13, '0'), '_',
                         LPAD(HEX(FLOOR(RAND() * 16777216)), 6, '0'))
WHERE business_id IS NULL;

-- 3.4 portal_category（前缀 cat_）
UPDATE portal_category
SET business_id = CONCAT('cat_',
                         LPAD(id, 13, '0'), '_',
                         LPAD(HEX(FLOOR(RAND() * 16777216)), 6, '0'))
WHERE business_id IS NULL;

-- 3.5 portal_comment（前缀 com_）
UPDATE portal_comment
SET business_id = CONCAT('com_',
                         LPAD(id, 13, '0'), '_',
                         LPAD(HEX(FLOOR(RAND() * 16777216)), 6, '0'))
WHERE business_id IS NULL;

-- 3.6 portal_tag（前缀 tag_）
UPDATE portal_tag
SET business_id = CONCAT('tag_',
                         LPAD(id, 13, '0'), '_',
                         LPAD(HEX(FLOOR(RAND() * 16777216)), 6, '0'))
WHERE business_id IS NULL;

-- ============================================
-- 第四阶段：回填子表 *_business_id 外键列
-- ============================================
-- 通过 JOIN 父表回填，仅回填 NULL 的行
-- 注意：UPDATE JOIN 语法在 MySQL 中支持，且使用 LEFT JOIN 保证未关联到的行保持 NULL

-- 4.1 portal_article 关联回填
UPDATE portal_article a
    LEFT JOIN portal_user u ON a.author_id = u.id
    LEFT JOIN portal_category c ON a.category_id = c.id
    LEFT JOIN portal_category rc ON a.root_category_id = rc.id
SET a.author_business_id = u.business_id,
    a.category_business_id = c.business_id,
    a.root_category_business_id = rc.business_id
WHERE a.author_business_id IS NULL
   OR a.category_business_id IS NULL
   OR a.root_category_business_id IS NULL;

-- 4.2 portal_comment 关联回填
UPDATE portal_comment cm
    LEFT JOIN portal_article a ON cm.article_id = a.id
    LEFT JOIN portal_user u ON cm.author_id = u.id
    LEFT JOIN portal_comment parent ON cm.parent_id = parent.id
    LEFT JOIN portal_comment root ON cm.root_id = root.id
SET cm.article_business_id = a.business_id,
    cm.author_business_id = u.business_id,
    cm.parent_business_id = parent.business_id,
    cm.root_business_id = root.business_id
WHERE cm.article_business_id IS NULL
   OR cm.author_business_id IS NULL
   OR cm.parent_business_id IS NULL
   OR cm.root_business_id IS NULL;

-- 4.3 portal_category 自引用回填
UPDATE portal_category c
    LEFT JOIN portal_category parent ON c.parent_id = parent.id
SET c.parent_business_id = parent.business_id
WHERE c.parent_business_id IS NULL;

-- 4.4 portal_like 关联回填
UPDATE portal_like l
    LEFT JOIN portal_user u ON l.user_id = u.id
    LEFT JOIN portal_article a ON l.article_id = a.id
SET l.user_business_id = u.business_id,
    l.article_business_id = a.business_id
WHERE l.user_business_id IS NULL
   OR l.article_business_id IS NULL;

-- 4.5 portal_bookmark 关联回填
UPDATE portal_bookmark b
    LEFT JOIN portal_user u ON b.user_id = u.id
    LEFT JOIN portal_article a ON b.article_id = a.id
SET b.user_business_id = u.business_id,
    b.article_business_id = a.business_id
WHERE b.user_business_id IS NULL
   OR b.article_business_id IS NULL;

-- 4.6 portal_comment_like 关联回填
UPDATE portal_comment_like cl
    LEFT JOIN portal_comment c ON cl.comment_id = c.id
    LEFT JOIN portal_user u ON cl.user_id = u.id
SET cl.comment_business_id = c.business_id,
    cl.user_business_id = u.business_id
WHERE cl.comment_business_id IS NULL
   OR cl.user_business_id IS NULL;

-- 4.7 portal_article_view 关联回填
UPDATE portal_article_view av
    LEFT JOIN portal_article a ON av.article_id = a.id
    LEFT JOIN portal_user u ON av.user_id = u.id
SET av.article_business_id = a.business_id,
    av.user_business_id = u.business_id
WHERE av.article_business_id IS NULL
   OR (av.user_id IS NOT NULL AND av.user_business_id IS NULL);

-- 4.8 portal_article_version 关联回填
UPDATE portal_article_version av
    LEFT JOIN portal_article a ON av.article_id = a.id
    LEFT JOIN portal_user u ON av.operator_id = u.id
SET av.article_business_id = a.business_id,
    av.operator_business_id = u.business_id
WHERE av.article_business_id IS NULL
   OR av.operator_business_id IS NULL;

-- 4.9 portal_article_tag 多对多回填
UPDATE portal_article_tag at
    LEFT JOIN portal_article a ON at.article_id = a.id
    LEFT JOIN portal_tag t ON at.tag_id = t.id
SET at.article_business_id = a.business_id,
    at.tag_business_id = t.business_id
WHERE at.article_business_id IS NULL
   OR at.tag_business_id IS NULL;

-- 4.10 portal_tip_order 关联回填
-- target_id 根据 target_type 多态关联：
--   article / article_paid → portal_article
--   column → portal_column（P2 批次，本批不回填 target_business_id）
UPDATE portal_tip_order t
    LEFT JOIN portal_user u ON t.user_id = u.id
    LEFT JOIN portal_user au ON t.author_id = au.id
    LEFT JOIN portal_article a ON t.target_id = a.id AND t.target_type IN ('article', 'article_paid')
SET t.user_business_id = u.business_id,
    t.author_business_id = au.business_id,
    t.target_business_id = COALESCE(a.business_id, t.target_business_id)
WHERE t.user_business_id IS NULL
   OR t.author_business_id IS NULL
   OR (t.target_type IN ('article', 'article_paid') AND t.target_business_id IS NULL);

-- 4.11 portal_feed_event 关联回填
-- target_id 根据 target_type 多态关联：
--   article → portal_article（本批回填）
--   experience/column/book/topic 等 → P2/P3 批次
UPDATE portal_feed_event fe
    LEFT JOIN portal_user u ON fe.user_id = u.id
    LEFT JOIN portal_article a ON fe.target_id = a.id AND fe.target_type = 'article'
SET fe.user_business_id = u.business_id,
    fe.target_business_id = COALESCE(a.business_id, fe.target_business_id)
WHERE fe.user_business_id IS NULL
   OR (fe.target_type = 'article' AND fe.target_business_id IS NULL);

-- 4.12 portal_report 关联回填
-- target_id 根据 target_type 多态关联：
--   article → portal_article
--   comment → portal_comment
--   user → portal_user
UPDATE portal_report r
    LEFT JOIN portal_user u ON r.user_id = u.id
    LEFT JOIN portal_article a ON r.target_id = a.id AND r.target_type = 'article'
    LEFT JOIN portal_comment c ON r.target_id = c.id AND r.target_type = 'comment'
    LEFT JOIN portal_user tu ON r.target_id = tu.id AND r.target_type = 'user'
SET r.user_business_id = u.business_id,
    r.target_business_id = COALESCE(a.business_id, c.business_id, tu.business_id)
WHERE r.user_business_id IS NULL
   OR r.target_business_id IS NULL;

-- ============================================
-- 第五阶段：验证回填结果
-- ============================================
SELECT '=== 父表 business_id 回填统计 ===' AS section;
SELECT 'portal_user' AS tbl, COUNT(*) AS total, SUM(business_id IS NULL) AS null_count FROM portal_user
UNION ALL SELECT 'sys_user', COUNT(*), SUM(business_id IS NULL) FROM sys_user
UNION ALL SELECT 'portal_article', COUNT(*), SUM(business_id IS NULL) FROM portal_article
UNION ALL SELECT 'portal_category', COUNT(*), SUM(business_id IS NULL) FROM portal_category
UNION ALL SELECT 'portal_comment', COUNT(*), SUM(business_id IS NULL) FROM portal_comment
UNION ALL SELECT 'portal_tag', COUNT(*), SUM(business_id IS NULL) FROM portal_tag;

SELECT '=== 子表 *_business_id 回填统计（NULL 表示未关联到父表） ===' AS section;
SELECT 'portal_article.author_business_id' AS col, COUNT(*) AS total, SUM(author_business_id IS NULL) AS null_count FROM portal_article
UNION ALL SELECT 'portal_comment.article_business_id', COUNT(*), SUM(article_business_id IS NULL) FROM portal_comment
UNION ALL SELECT 'portal_like.user_business_id', COUNT(*), SUM(user_business_id IS NULL) FROM portal_like
UNION ALL SELECT 'portal_bookmark.user_business_id', COUNT(*), SUM(user_business_id IS NULL) FROM portal_bookmark
UNION ALL SELECT 'portal_comment_like.comment_business_id', COUNT(*), SUM(comment_business_id IS NULL) FROM portal_comment_like
UNION ALL SELECT 'portal_article_tag.article_business_id', COUNT(*), SUM(article_business_id IS NULL) FROM portal_article_tag
UNION ALL SELECT 'portal_tip_order.user_business_id', COUNT(*), SUM(user_business_id IS NULL) FROM portal_tip_order
UNION ALL SELECT 'portal_feed_event.user_business_id', COUNT(*), SUM(user_business_id IS NULL) FROM portal_feed_event
UNION ALL SELECT 'portal_report.user_business_id', COUNT(*), SUM(user_business_id IS NULL) FROM portal_report;

-- ============================================
-- 迁移完成说明
-- ============================================
-- 1. 父表已具备 business_id 业务主键列 + 唯一索引，可保证全局唯一
-- 2. 子表已具备 *_business_id 外键列 + 普通索引，可高效 JOIN
-- 3. 历史 NULL 数据已通过 JOIN 回填
-- 4. 后续应用层在 INSERT 父表时生成 business_id（调用 BusinessIdGenerator），
--    INSERT 子表时同步写入 *_business_id 外键列
-- 5. 旧自增 id 列保留，作为物理主键，旧查询逻辑不受影响
-- 6. 双轨过渡完成后，后续可分批将 JOIN 条件从 id 切换到 business_id
-- ============================================
