-- ============================================
-- 墨韵智库 - 读书空间建表 DDL（最终合并版）
-- ============================================
-- 合并来源脚本：
--   07_reading_interview_init.sql                   （读书空间初始建表：portal_book/portal_book_list/portal_book_list_item/portal_book_list_like/portal_book_quote/portal_book_quote_like/portal_reading_progress/portal_book_club_activity/portal_book_club_participant/portal_book_club_record/portal_book_club_record_like）
--   16_reading_space_add_fields.sql                 （portal_book 新增 access_level/preview_ratio/price/is_featured/is_recommended/summary/author_bio 字段及索引；portal_book_list 新增 is_featured/access_level/tags 字段及索引；portal_book_quote 新增 is_featured/location 字段及索引）
--   22_booklist_bookmark.sql                        （portal_book_list_bookmark 建表）
--   42_portal_book_chapter_init.sql                （portal_book_chapter/portal_book_chapter_view 建表；portal_book 新增 type/serial_status/word_count/chapter_count/latest_chapter_id/latest_chapter_title/last_update_time/is_finished 字段及索引；portal_reading_progress 新增 current_chapter_id/current_chapter_no/chapter_offset/last_read_time/reading_duration_ms 字段及索引；portal_book_quote 新增 chapter_id 字段及索引）
--   44_portal_bookshelf_preference_init.sql         （portal_bookshelf/portal_reading_preference 建表）
--   46_portal_book_recommend_init.sql               （portal_book_recommend 建表）
--   53_reading_like_and_booklist_fix.sql            （幂等兜底，07 建表已包含 like_count/remark，本文件无新增字段）
--   54_book_club_activate.sql                       （portal_book_club_record 新增 record_type 字段及索引；portal_book_club_activity.status 注释更新）
-- 涉及表：
--   portal_book, portal_book_chapter, portal_book_chapter_view,
--   portal_book_list, portal_book_list_item, portal_book_list_like, portal_book_list_bookmark,
--   portal_book_quote, portal_book_quote_like,
--   portal_reading_progress, portal_bookshelf, portal_reading_preference, portal_book_recommend,
--   portal_book_club_activity, portal_book_club_participant,
--   portal_book_club_record, portal_book_club_record_like
-- 说明：
--   - 本文件仅包含建表 DDL（CREATE TABLE IF NOT EXISTS），不含 INSERT 数据、菜单注入语句
--   - 所有 ALTER TABLE 已合并入对应的 CREATE TABLE
--   - 53 号脚本为幂等兜底（如缺失则补齐 like_count/remark），07 建表已包含，无新增字段
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 书籍表
-- 来源：07 建表 + 16 扩展 + 42 扩展
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(500) NOT NULL COMMENT '书名',
    `author` VARCHAR(200) NOT NULL COMMENT '作者',
    `cover` VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
    `description` TEXT COMMENT '简介',
    `isbn` VARCHAR(50) DEFAULT NULL COMMENT 'ISBN',
    `publisher` VARCHAR(200) DEFAULT NULL COMMENT '出版社',
    `publish_date` DATE DEFAULT NULL COMMENT '出版日期',
    `page_count` INT DEFAULT 0 COMMENT '页数',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
    `rating` DECIMAL(3,2) DEFAULT 0.00 COMMENT '评分',
    `reading_count` BIGINT DEFAULT 0 COMMENT '阅读人数',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态:active,inactive',
    -- 16 扩展：商业化预留、精选推荐
    `access_level` VARCHAR(20) DEFAULT 'free' COMMENT '访问级别:free,vip,preview',
    `preview_ratio` INT DEFAULT 30 COMMENT '免费试读比例（0-100）',
    `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '书籍单价（元）',
    `is_featured` TINYINT(1) DEFAULT 0 COMMENT '是否精选',
    `is_recommended` TINYINT(1) DEFAULT 0 COMMENT '是否推荐',
    `summary` TEXT COMMENT '简介（纯文本）',
    `author_bio` TEXT COMMENT '作者简介',
    -- 42 扩展：书籍类型、连载状态、字数、章节数
    `type` VARCHAR(20) DEFAULT 'published' COMMENT '书籍类型：published=出版物，novel=网络小说，longform=长文',
    `serial_status` VARCHAR(20) DEFAULT 'completed' COMMENT '连载状态：ongoing=连载中，completed=已完结，hiatus=暂停更新',
    `word_count` BIGINT DEFAULT 0 COMMENT '总字数（章节字数之和）',
    `chapter_count` INT DEFAULT 0 COMMENT '总章节数',
    `latest_chapter_id` BIGINT DEFAULT NULL COMMENT '最新章节ID（用于追更展示）',
    `latest_chapter_title` VARCHAR(500) DEFAULT NULL COMMENT '最新章节标题',
    `last_update_time` DATETIME DEFAULT NULL COMMENT '最后更新时间（章节发布时同步）',
    `is_finished` TINYINT(1) DEFAULT 1 COMMENT '是否完结：1=完结，0=连载中（冗余字段，便于查询）',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_title` (`title`),
    -- 16 扩展索引
    KEY `idx_access_level` (`access_level`),
    KEY `idx_is_featured` (`is_featured`),
    KEY `idx_is_recommended` (`is_recommended`),
    -- 42 扩展索引
    KEY `idx_type` (`type`),
    KEY `idx_serial_status` (`serial_status`),
    KEY `idx_is_finished` (`is_finished`),
    KEY `idx_word_count` (`word_count`),
    KEY `idx_last_update_time` (`last_update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍表';

-- ----------------------------
-- 书籍章节表
-- 来源：42 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_chapter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id` BIGINT NOT NULL COMMENT '所属书籍ID',
    `title` VARCHAR(500) NOT NULL COMMENT '章节标题',
    `content` LONGTEXT COMMENT '章节正文（HTML格式，上限4GB）',
    `content_markdown` TEXT COMMENT 'Markdown原始内容（上限64KB，单章足够）',
    `editor_mode` VARCHAR(20) DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
    `word_count` INT DEFAULT 0 COMMENT '字数统计',
    `chapter_no` INT NOT NULL DEFAULT 0 COMMENT '章节序号（用于排序，从1开始）',
    `volume_id` BIGINT DEFAULT NULL COMMENT '所属分卷ID（可选，支持分卷管理）',
    `is_free` TINYINT(1) DEFAULT 1 COMMENT '是否免费：1=免费，0=VIP章节',
    `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '章节单价（元，VIP章节购买）',
    `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否已发布：0=草稿，1=已发布',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间（支持定时发布）',
    `view_count` BIGINT DEFAULT 0 COMMENT '章节浏览量',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_chapter_no` (`book_id`, `chapter_no`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_publish_time` (`publish_time`),
    KEY `idx_is_published` (`is_published`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='书籍章节表';

-- ----------------------------
-- 章节浏览记录表
-- 来源：42 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_chapter_view` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `chapter_id` BIGINT NOT NULL COMMENT '章节ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（未登录为NULL）',
    `client_ip` VARCHAR(50) DEFAULT NULL COMMENT '客户端IP',
    `read_duration_ms` INT DEFAULT 0 COMMENT '阅读时长（毫秒）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    PRIMARY KEY (`id`),
    KEY `idx_chapter_id` (`chapter_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='章节浏览记录表';

-- ----------------------------
-- 书单表
-- 来源：07 建表 + 16 扩展
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_list` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(500) NOT NULL COMMENT '书单标题',
    `description` TEXT COMMENT '书单简介',
    `cover` VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
    `user_id` BIGINT NOT NULL COMMENT '创建者ID',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `is_public` TINYINT(1) DEFAULT 1 COMMENT '是否公开',
    `book_count` INT DEFAULT 0 COMMENT '书籍数量',
    `view_count` BIGINT DEFAULT 0 COMMENT '浏览数',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态:active,inactive',
    -- 16 扩展：精选、访问级别、标签
    `is_featured` TINYINT(1) DEFAULT 0 COMMENT '是否精选',
    `access_level` VARCHAR(20) DEFAULT 'free' COMMENT '访问级别:free,vip',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    -- 16 扩展索引
    KEY `idx_is_featured` (`is_featured`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书单表';

-- ----------------------------
-- 书单-书籍关联表
-- 来源：07 建表 + 53 幂等兜底（07 建表已包含 remark，无新增字段）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_list_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_list_id` BIGINT NOT NULL COMMENT '书单ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `note` TEXT COMMENT '添加说明',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_book_list_id` (`book_list_id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书单-书籍关联表';

-- ----------------------------
-- 书单点赞表
-- 来源：07 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_list_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_list_id` BIGINT NOT NULL COMMENT '书单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_list_user` (`book_list_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书单点赞表';

-- ----------------------------
-- 书单收藏表
-- 来源：22 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_list_bookmark` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `booklist_id` BIGINT NOT NULL COMMENT '书单ID',
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `create_by`   VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_by`   VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_booklist_user` (`booklist_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书单收藏表';

-- ----------------------------
-- 金句摘录表
-- 来源：07 建表 + 16 扩展 + 42 扩展
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_quote` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    -- 42 扩展：关联章节
    `chapter_id` BIGINT DEFAULT NULL COMMENT '章节ID（关联 portal_book_chapter）',
    `content` TEXT NOT NULL COMMENT '金句内容',
    `page` VARCHAR(100) DEFAULT NULL COMMENT '页码',
    `chapter` VARCHAR(200) DEFAULT NULL COMMENT '章节',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `is_public` TINYINT(1) DEFAULT 1 COMMENT '是否公开',
    -- 16 扩展：精选、位置描述
    `is_featured` TINYINT(1) DEFAULT 0 COMMENT '是否精选',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '章节标题/位置描述',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_is_public` (`is_public`),
    -- 16 扩展索引
    KEY `idx_is_featured` (`is_featured`),
    -- 42 扩展索引
    KEY `idx_chapter_id` (`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='金句摘录表';

-- ----------------------------
-- 金句点赞表
-- 来源：07 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_quote_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `quote_id` BIGINT NOT NULL COMMENT '金句ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quote_user` (`quote_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='金句点赞表';

-- ----------------------------
-- 阅读进度表
-- 来源：07 建表 + 42 扩展（章节级进度记忆）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_reading_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    -- 42 扩展：章节级进度记忆
    `current_chapter_id` BIGINT DEFAULT NULL COMMENT '当前阅读章节ID',
    `current_chapter_no` INT DEFAULT 0 COMMENT '当前章节序号',
    `chapter_offset` INT DEFAULT 0 COMMENT '章节内滚动偏移（像素）',
    `last_read_time` DATETIME DEFAULT NULL COMMENT '最后阅读时间',
    `reading_duration_ms` BIGINT DEFAULT 0 COMMENT '累计阅读时长（毫秒）',
    `status` VARCHAR(30) DEFAULT 'want_to_read' COMMENT '状态:want_to_read,reading,finished',
    `progress` INT DEFAULT 0 COMMENT '阅读进度百分比',
    `pages_read` INT DEFAULT 0 COMMENT '已读页数',
    `start_date` DATE DEFAULT NULL COMMENT '开始阅读日期',
    `finish_date` DATE DEFAULT NULL COMMENT '完成日期',
    `note` TEXT COMMENT '阅读笔记',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    -- 42 扩展索引
    KEY `idx_last_read_time` (`last_read_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阅读进度表';

-- ----------------------------
-- 用户书架（收藏书籍）表
-- 来源：44 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_bookshelf` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `last_chapter_id` BIGINT DEFAULT NULL COMMENT '最后阅读章节ID（冗余，用于续读）',
    `last_chapter_no` INT DEFAULT 0 COMMENT '最后阅读章节序号',
    `sort` INT DEFAULT 0 COMMENT '排序（用户自定义书架顺序，越大越靠前）',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户书架（收藏书籍）表';

-- ----------------------------
-- 用户阅读偏好表
-- 来源：44 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_reading_preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `font_size` INT DEFAULT 18 COMMENT '正文字号（px，12-32）',
    `line_height` DECIMAL(3,1) DEFAULT 1.8 COMMENT '行距（倍，1.2-3.0）',
    `theme` VARCHAR(20) DEFAULT 'default' COMMENT '阅读主题：default=跟随 / light=亮色 / dark=暗色 / sepia=护眼黄',
    `font_family` VARCHAR(50) DEFAULT 'system' COMMENT '字体：system=系统默认 / serif=衬线 / song=宋体 / hei=黑体',
    `letter_spacing` DECIMAL(3,1) DEFAULT 0.0 COMMENT '字间距（px，-1.0-5.0）',
    `paragraph_spacing` DECIMAL(4,1) DEFAULT 1.2 COMMENT '段间距（em，0.5-5.0）',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户阅读偏好表';

-- ----------------------------
-- 书籍推荐位表
-- 来源：46 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_recommend` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `position` VARCHAR(50) NOT NULL COMMENT '推荐位置：home_banner=首页轮播 / home_hot=首页热门 / category_top=分类顶推 / limit_free=限免专区 / discover_banner=发现页轮播',
    `sort` INT DEFAULT 0 COMMENT '排序（越小越靠前）',
    `start_time` DATETIME DEFAULT NULL COMMENT '推荐开始时间（NULL 表示立即生效）',
    `end_time` DATETIME DEFAULT NULL COMMENT '推荐结束时间（NULL 表示长期有效）',
    `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否生效：1=生效，0=下架',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注（运营说明）',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_position` (`book_id`, `position`),
    KEY `idx_position` (`position`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_time_window` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='书籍推荐位表';

-- ----------------------------
-- 共读活动表
-- 来源：07 建表 + 54 扩展（status 注释更新）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_club_activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(500) NOT NULL COMMENT '活动标题',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `description` TEXT COMMENT '活动描述',
    `cover` VARCHAR(500) DEFAULT NULL COMMENT '活动封面',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `max_participants` INT DEFAULT 100 COMMENT '最大参与人数',
    `current_participants` INT DEFAULT 0 COMMENT '当前参与人数',
    `created_by` BIGINT NOT NULL COMMENT '创建者ID',
    `status` VARCHAR(20) DEFAULT 'upcoming' COMMENT '状态:upcoming-未开始,ongoing-进行中,ended-已结束',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='共读活动表';

-- ----------------------------
-- 共读参与表
-- 来源：07 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_club_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='共读参与表';

-- ----------------------------
-- 共读打卡记录表
-- 来源：07 建表 + 54 扩展（record_type 字段及索引；content/like_count 已在 07 建表包含）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_club_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `day` INT NOT NULL COMMENT '第几天',
    `content` TEXT COMMENT '打卡内容',
    `images` TEXT COMMENT '图片，逗号分隔',
    -- 54 扩展：记录类型（读后感/摘抄）
    `record_type` VARCHAR(20) DEFAULT 'reflection' COMMENT '记录类型:reflection-读后感,excerpt-摘抄',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_user_id` (`user_id`),
    -- 54 扩展索引
    KEY `idx_record_type` (`record_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='共读打卡记录表';

-- ----------------------------
-- 打卡点赞表
-- 来源：07 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_book_club_record_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `record_id` BIGINT NOT NULL COMMENT '打卡记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_user` (`record_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡点赞表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 读书空间建表 DDL 合并完成
-- ============================================
