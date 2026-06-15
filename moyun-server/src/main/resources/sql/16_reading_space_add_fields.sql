-- =====================================================
-- 读书空间模块 - 新增字段增量脚本
-- 说明：为支持商业化预留、精选推荐等功能，在现有表上新增字段
-- 版本: v1.0
-- 日期: 2026-06-02
-- 执行方式：在已有 moyun 数据库中执行，注意表前缀
-- =====================================================

-- =====================================================
-- 一、书籍表 portal_book 新增字段
-- =====================================================

-- 访问级别：free=免费公开, vip=会员专享, preview=试读（前30%免费）
ALTER TABLE `portal_book`
    ADD COLUMN `access_level` VARCHAR(20) DEFAULT 'free' COMMENT '访问级别:free,vip,preview' AFTER `status`;

-- 免费试读比例（0-100，当 access_level=preview 时生效）
ALTER TABLE `portal_book`
    ADD COLUMN `preview_ratio` INT DEFAULT 30 COMMENT '免费试读比例（0-100）' AFTER `access_level`;

-- 书籍单价（元，预留：未来付费单本购买）
ALTER TABLE `portal_book`
    ADD COLUMN `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '书籍单价（元）' AFTER `preview_ratio`;

-- 是否精选（首页展示用）
ALTER TABLE `portal_book`
    ADD COLUMN `is_featured` TINYINT(1) DEFAULT 0 COMMENT '是否精选' AFTER `price`;

-- 是否推荐（首页展示用）
ALTER TABLE `portal_book`
    ADD COLUMN `is_recommended` TINYINT(1) DEFAULT 0 COMMENT '是否推荐' AFTER `is_featured`;

-- 简介（纯文本，区别于可能的富文本 description）
ALTER TABLE `portal_book`
    ADD COLUMN `summary` TEXT COMMENT '简介（纯文本）' AFTER `is_recommended`;

-- 作者简介
ALTER TABLE `portal_book`
    ADD COLUMN `author_bio` TEXT COMMENT '作者简介' AFTER `summary`;

-- 为新增字段建立索引
ALTER TABLE `portal_book` ADD INDEX `idx_access_level` (`access_level`);
ALTER TABLE `portal_book` ADD INDEX `idx_is_featured` (`is_featured`);
ALTER TABLE `portal_book` ADD INDEX `idx_is_recommended` (`is_recommended`);

-- =====================================================
-- 二、书单表 portal_book_list 新增字段
-- =====================================================

-- 是否精选
ALTER TABLE `portal_book_list`
    ADD COLUMN `is_featured` TINYINT(1) DEFAULT 0 COMMENT '是否精选' AFTER `status`;

-- 访问级别: free=免费公开, vip=会员专享
ALTER TABLE `portal_book_list`
    ADD COLUMN `access_level` VARCHAR(20) DEFAULT 'free' COMMENT '访问级别:free,vip' AFTER `is_featured`;

-- 标签（逗号分隔）
ALTER TABLE `portal_book_list`
    ADD COLUMN `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）' AFTER `access_level`;

-- 为新增字段建立索引
ALTER TABLE `portal_book_list` ADD INDEX `idx_is_featured` (`is_featured`);

-- =====================================================
-- 三、金句摘录表 portal_book_quote 新增字段
-- =====================================================

-- 是否精选（首页展示用）
ALTER TABLE `portal_book_quote`
    ADD COLUMN `is_featured` TINYINT(1) DEFAULT 0 COMMENT '是否精选' AFTER `is_public`;

-- 章节标题/位置描述
ALTER TABLE `portal_book_quote`
    ADD COLUMN `location` VARCHAR(200) DEFAULT NULL COMMENT '章节标题/位置描述' AFTER `is_featured`;

-- 为新增字段建立索引
ALTER TABLE `portal_book_quote` ADD INDEX `idx_is_featured` (`is_featured`);

-- =====================================================
-- 四、模拟数据 - 书籍表新增字段填充示例
-- =====================================================

-- 为已有书籍数据设置默认精选
UPDATE `portal_book` SET `is_featured` = 1 WHERE `id` IN (1, 2, 3, 4, 5);
UPDATE `portal_book` SET `is_recommended` = 1 WHERE `id` IN (1, 2, 3);
UPDATE `portal_book` SET `access_level` = 'free' WHERE `access_level` IS NULL;

-- =====================================================
-- 脚本执行完成
-- =====================================================
