-- =====================================================
-- 读书空间 & 面试指南 - 数据库初始化脚本
-- 版本: v1.0
-- 日期: 2026-06-02
-- =====================================================

-- =====================================================
-- 一、读书空间 - 表结构
-- =====================================================

-- 书籍表
DROP TABLE IF EXISTS `portal_book`;
CREATE TABLE `portal_book` (
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
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书籍表';

-- 书单表
DROP TABLE IF EXISTS `portal_book_list`;
CREATE TABLE `portal_book_list` (
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
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书单表';

-- 书单-书籍关联表
DROP TABLE IF EXISTS `portal_book_list_item`;
CREATE TABLE `portal_book_list_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_list_id` BIGINT NOT NULL COMMENT '书单ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `note` TEXT COMMENT '添加说明',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    PRIMARY KEY (`id`),
    KEY `idx_book_list_id` (`book_list_id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书单-书籍关联表';

-- 阅读进度表
DROP TABLE IF EXISTS `portal_reading_progress`;
CREATE TABLE `portal_reading_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `status` VARCHAR(30) DEFAULT 'want_to_read' COMMENT '状态:want_to_read,reading,finished',
    `progress` INT DEFAULT 0 COMMENT '阅读进度百分比',
    `pages_read` INT DEFAULT 0 COMMENT '已读页数',
    `start_date` DATE DEFAULT NULL COMMENT '开始阅读日期',
    `finish_date` DATE DEFAULT NULL COMMENT '完成日期',
    `note` TEXT COMMENT '阅读笔记',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读进度表';

-- 金句摘录表
DROP TABLE IF EXISTS `portal_book_quote`;
CREATE TABLE `portal_book_quote` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `content` TEXT NOT NULL COMMENT '金句内容',
    `page` VARCHAR(100) DEFAULT NULL COMMENT '页码',
    `chapter` VARCHAR(200) DEFAULT NULL COMMENT '章节',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `is_public` TINYINT(1) DEFAULT 1 COMMENT '是否公开',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_is_public` (`is_public`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金句摘录表';

-- 金句点赞表
DROP TABLE IF EXISTS `portal_book_quote_like`;
CREATE TABLE `portal_book_quote_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `quote_id` BIGINT NOT NULL COMMENT '金句ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quote_user` (`quote_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金句点赞表';

-- 书单点赞表
DROP TABLE IF EXISTS `portal_book_list_like`;
CREATE TABLE `portal_book_list_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_list_id` BIGINT NOT NULL COMMENT '书单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_list_user` (`book_list_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书单点赞表';

-- 共读活动表
DROP TABLE IF EXISTS `portal_book_club_activity`;
CREATE TABLE `portal_book_club_activity` (
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
    `status` VARCHAR(20) DEFAULT 'upcoming' COMMENT '状态:upcoming,ongoing,ended',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共读活动表';

-- 共读参与表
DROP TABLE IF EXISTS `portal_book_club_participant`;
CREATE TABLE `portal_book_club_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共读参与表';

-- 共读打卡记录表
DROP TABLE IF EXISTS `portal_book_club_record`;
CREATE TABLE `portal_book_club_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `day` INT NOT NULL COMMENT '第几天',
    `content` TEXT COMMENT '打卡内容',
    `images` TEXT COMMENT '图片，逗号分隔',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共读打卡记录表';

-- 打卡点赞表
DROP TABLE IF EXISTS `portal_book_club_record_like`;
CREATE TABLE `portal_book_club_record_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `record_id` BIGINT NOT NULL COMMENT '打卡记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_user` (`record_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡点赞表';

-- =====================================================
-- 二、面试指南 - 表结构
-- =====================================================

-- 面试题目分类表
DROP TABLE IF EXISTS `portal_interview_category`;
CREATE TABLE `portal_interview_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(200) NOT NULL COMMENT '分类名称',
    `slug` VARCHAR(200) DEFAULT NULL COMMENT '分类标识',
    `description` TEXT COMMENT '分类描述',
    `icon` VARCHAR(500) DEFAULT NULL COMMENT '图标URL',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `question_count` INT DEFAULT 0 COMMENT '题目数量',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态:active,inactive',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题目分类表';

-- 面试题目表
DROP TABLE IF EXISTS `portal_interview_question`;
CREATE TABLE `portal_interview_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(500) NOT NULL COMMENT '题目标题',
    `description` TEXT COMMENT '题目描述',
    `difficulty` VARCHAR(20) DEFAULT 'medium' COMMENT '难度:easy,medium,hard',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
    `companies` VARCHAR(500) DEFAULT NULL COMMENT '公司，逗号分隔',
    `acceptance_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '通过率',
    `submission_count` BIGINT DEFAULT 0 COMMENT '提交次数',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `hint` TEXT COMMENT '提示',
    `solution` TEXT COMMENT '参考答案',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态:active,inactive',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_difficulty` (`difficulty`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题目表';

-- 题目提交记录表
DROP TABLE IF EXISTS `portal_interview_submission`;
CREATE TABLE `portal_interview_submission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `code` TEXT COMMENT '提交的代码',
    `language` VARCHAR(50) DEFAULT 'java' COMMENT '编程语言',
    `status` VARCHAR(50) DEFAULT 'pending' COMMENT '状态:accepted,wrong_answer,time_limit,compile_error',
    `runtime` INT DEFAULT NULL COMMENT '运行时间（毫秒）',
    `memory_usage` INT DEFAULT NULL COMMENT '内存使用（KB）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (`id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目提交记录表';

-- 题目收藏表
DROP TABLE IF EXISTS `portal_interview_bookmark`;
CREATE TABLE `portal_interview_bookmark` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `note` TEXT COMMENT '笔记',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_user` (`question_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目收藏表';

-- 题目点赞表
DROP TABLE IF EXISTS `portal_interview_question_like`;
CREATE TABLE `portal_interview_question_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_user` (`question_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目点赞表';

-- 做题记录表
DROP TABLE IF EXISTS `portal_interview_attempt`;
CREATE TABLE `portal_interview_attempt` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `attempt_count` INT DEFAULT 1 COMMENT '尝试次数',
    `last_attempt_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后尝试时间',
    `status` VARCHAR(30) DEFAULT 'attempted' COMMENT '状态:not_attempted,attempted,solved',
    `first_solved_at` DATETIME DEFAULT NULL COMMENT '首次解决时间',
    `last_solved_at` DATETIME DEFAULT NULL COMMENT '最后解决时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_user` (`question_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='做题记录表';

-- 面经表
DROP TABLE IF EXISTS `portal_interview_experience`;
CREATE TABLE `portal_interview_experience` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(500) NOT NULL COMMENT '面经标题',
    `company` VARCHAR(200) NOT NULL COMMENT '公司',
    `position` VARCHAR(200) DEFAULT NULL COMMENT '岗位',
    `year` INT DEFAULT NULL COMMENT '年份',
    `month` INT DEFAULT NULL COMMENT '月份',
    `content` TEXT NOT NULL COMMENT '面经内容',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签',
    `view_count` BIGINT DEFAULT 0 COMMENT '浏览数',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `comment_count` BIGINT DEFAULT 0 COMMENT '评论数',
    `status` VARCHAR(20) DEFAULT 'published' COMMENT '状态:draft,published,archived',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_company` (`company`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面经表';

-- 面经点赞表
DROP TABLE IF EXISTS `portal_interview_experience_like`;
CREATE TABLE `portal_interview_experience_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `experience_id` BIGINT NOT NULL COMMENT '面经ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_experience_user` (`experience_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面经点赞表';

-- 简历模板表
DROP TABLE IF EXISTS `portal_interview_resume_template`;
CREATE TABLE `portal_interview_resume_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(500) NOT NULL COMMENT '模板标题',
    `description` TEXT COMMENT '模板描述',
    `cover` VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
    `download_url` VARCHAR(500) DEFAULT NULL COMMENT '下载地址',
    `category` VARCHAR(200) DEFAULT NULL COMMENT '分类',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `download_count` BIGINT DEFAULT 0 COMMENT '下载次数',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态:active,inactive',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历模板表';

-- =====================================================
-- 三、模拟数据
-- =====================================================

-- 书籍分类 - 复用现有的portal_category表

-- 插入模拟书籍数据
INSERT INTO `portal_book` (`title`, `author`, `cover`, `description`, `isbn`, `publisher`, `publish_date`, `page_count`, `category_id`, `tags`, `rating`, `reading_count`, `status`) VALUES
('代码整洁之道', 'Robert C. Martin', 'https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=400&h=300&fit=crop', '本书是软件工程领域的经典著作，阐述了如何编写整洁、可维护的代码', '9787115216872', '人民邮电出版社', '2010-01-01', 350, 1, '编程,代码质量,软件工程', 4.8, 1520, 'active'),
('深入理解计算机系统', 'Randal E. Bryant', 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=300&fit=crop', '本书从程序员视角深入解析计算机系统工作原理', '9787111544937', '机械工业出版社', '2016-11-01', 719, 1, '计算机系统,编程基础,操作系统', 4.9, 2340, 'active'),
('活着', '余华', 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=400&h=300&fit=crop', '讲述了福贵一生的悲欢故事，深刻反映人生的苦难与希望', '9787506365437', '作家出版社', '2012-08-01', 191, 3, '文学,小说,经典', 4.7, 5680, 'active'),
('人类简史', '尤瓦尔·赫拉利', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400&h=300&fit=crop', '从认知革命到科学革命，讲述人类演化的宏大历史', '9787508647357', '中信出版社', '2014-11-01', 414, 4, '历史,人文,社科', 4.6, 4230, 'active'),
('设计模式：可复用面向对象软件的基础', 'Erich Gamma', 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=400&h=300&fit=crop', '经典设计模式著作，收录23种设计模式', '9787111075752', '机械工业出版社', '2000-09-01', 395, 1, '设计模式,软件工程,编程', 4.5, 2890, 'active');

-- 插入模拟书单数据
INSERT INTO `portal_book_list` (`title`, `description`, `cover`, `user_id`, `category_id`, `is_public`, `book_count`, `view_count`, `like_count`, `status`) VALUES
('程序员必读书单', '精选编程领域的经典著作，提升技术水平', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400&h=300&fit=crop', 1, 1, 1, 3, 1250, 234, 'active'),
('经典文学作品', '经典文学著作合集，感受文学魅力', 'https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=400&h=300&fit=crop', 1, 3, 1, 5, 890, 156, 'active');

-- 插入书单-书籍关联数据
INSERT INTO `portal_book_list_item` (`book_list_id`, `book_id`, `sort`, `note`) VALUES
(1, 1, 1, '必读经典'),
(1, 2, 2, '深入理解计算机底层'),
(1, 5, 3, '设计模式必备'),
(2, 3, 1, '余华代表作');

-- 插入模拟金句数据
INSERT INTO `portal_book_quote` (`user_id`, `book_id`, `content`, `page`, `chapter`, `like_count`, `is_public`) VALUES
(1, 1, '代码是写给人看的，顺便给机器执行。', '第15页', '第一章', 156, 1),
(1, 3, '人是为活着本身而活着的，而不是为了活着之外的任何事物所活着。', '第50页', '第一章', 423, 1);

-- 插入模拟面试题目分类数据
INSERT INTO `portal_interview_category` (`name`, `slug`, `description`, `icon`, `sort`, `question_count`, `status`) VALUES
('算法与数据结构', 'algorithm', '算法题、数据结构相关面试题', 'fa-code', 1, 150, 'active'),
('系统设计', 'system-design', '系统架构设计、分布式系统等面试题', 'fa-sitemap', 2, 60, 'active'),
('前端开发', 'frontend', 'JavaScript、CSS、Vue、React等前端技术面试题', 'fa-laptop-code', 3, 120, 'active'),
('后端开发', 'backend', 'Java、Python、Go等后端技术面试题', 'fa-server', 4, 130, 'active'),
('数据库', 'database', 'MySQL、Redis等数据库相关面试题', 'fa-database', 5, 80, 'active');

-- 插入模拟面试题目数据
INSERT INTO `portal_interview_question` (`title`, `description`, `difficulty`, `category_id`, `tags`, `companies`, `acceptance_rate`, `submission_count`, `like_count`, `hint`, `solution`, `sort`, `status`) VALUES
('两数之和', '给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值的那两个整数，并返回它们的数组下标。', 'easy', 1, '数组,哈希表', '字节跳动,阿里巴巴,腾讯,美团', 65.5, 12500, 2580, '可以使用哈希表降低时间复杂度', '使用哈希表存储已经访问过的数字及其索引，每次查找 target - nums[i] 是否在哈希表中', 1, 'active'),
('最长无重复子串', '给定一个字符串 s ，请你找出其中不含有重复字符的最长子串的长度。', 'medium', 1, '字符串,滑动窗口', '字节跳动,阿里巴巴', 48.2, 8900, 1680, '使用滑动窗口维护当前无重复子串', '使用滑动窗口，用哈希表记录字符最后出现的位置', 2, 'active'),
('LRU缓存机制', '请你设计并实现一个满足 LRU (最近最少使用) 缓存约束的数据结构。', 'medium', 1, '设计,哈希表,双向链表', '字节跳动,腾讯,美团', 35.8, 6500, 1250, '需要O(1)时间复杂度的get和put操作', '使用哈希表+双向链表的组合，哈希表存储键到节点的映射', 3, 'active'),
('实现二叉树的前序遍历', '给你二叉树的根节点 root ，返回它节点值的前序遍历。', 'easy', 1, '二叉树,树,递归,迭代', '阿里巴巴,腾讯', 72.3, 4500, 890, '可以使用递归或迭代方式', '递归法：根-左-右；迭代法：使用栈', 4, 'active'),
('实现单例模式', '请设计一个线程安全的单例模式', 'easy', 2, '设计模式,单例', '阿里巴巴,腾讯', 68.5, 3200, 760, '考虑双重检查锁定', '使用双重检查锁定模式或枚举单例', 5, 'active');

-- 插入模拟面经数据
INSERT INTO `portal_interview_experience` (`user_id`, `title`, `company`, `position`, `year`, `month`, `content`, `tags`, `view_count`, `like_count`, `comment_count`, `status`) VALUES
(1, '字节跳动后端开发面试经验分享', '字节跳动', '后端开发工程师', 2026, 5, '今天分享一下我在字节跳动的后端开发面试经验。一共三轮技术面+一轮HR面...', '后端,Java,字节跳动', 5600, 234, 45, 'published'),
(1, '腾讯前端开发面试总结', '腾讯', '前端开发工程师', 2026, 4, '分享一下腾讯前端开发岗的面试过程，主要考察了基础、算法和项目经验...', '前端,Vue,腾讯', 3800, 189, 32, 'published');

-- 插入模拟简历模板数据
INSERT INTO `portal_interview_resume_template` (`title`, `description`, `cover`, `download_url`, `category`, `like_count`, `download_count`, `sort`, `status`) VALUES
('技术岗通用简历模板', '适用于技术岗位的通用简历模板，简洁大方', 'https://images.unsplash.com/photo-1586281380349-632531db7ed4?w=400&h=300&fit=crop', '/download/resume/tech-general.docx', '技术岗', 456, 1234, 1, 'active'),
('应届生简洁简历模板', '适合应届生使用的简洁风格简历模板', 'https://images.unsplash.com/photo-1553877522-43269d4ea984?w=400&h=300&fit=crop', '/download/resume/student-simple.docx', '应届生', 389, 856, 2, 'active');

-- =====================================================
-- 脚本执行完成
-- =====================================================
