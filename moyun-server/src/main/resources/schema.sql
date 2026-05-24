-- ========================================
-- 墨韵智库 MySQL 数据库初始化脚本
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS moyundb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE moyundb;

-- ========================================
-- 系统管理模块表
-- ========================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_id BIGINT,
    user_name VARCHAR(30) NOT NULL,
    nick_name VARCHAR(30) NOT NULL,
    user_type VARCHAR(2) DEFAULT '00',
    email VARCHAR(50),
    phonenumber VARCHAR(11),
    sex CHAR(1) DEFAULT '0',
    avatar VARCHAR(100),
    password VARCHAR(100),
    status CHAR(1) DEFAULT '0',
    del_flag CHAR(1) DEFAULT '0',
    login_ip VARCHAR(128),
    login_date DATETIME,
    create_by VARCHAR(64),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    remark VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(30) NOT NULL,
    role_key VARCHAR(100) NOT NULL,
    role_sort INT NOT NULL,
    data_scope CHAR(1) DEFAULT '1',
    menu_check_strictly BOOLEAN DEFAULT TRUE,
    dept_check_strictly BOOLEAN DEFAULT TRUE,
    status CHAR(1) NOT NULL,
    del_flag CHAR(1) DEFAULT '0',
    create_by VARCHAR(64),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    remark VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    menu_name VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    order_num INT DEFAULT 0,
    path VARCHAR(200),
    component VARCHAR(255),
    query_param VARCHAR(255),
    is_frame BOOLEAN DEFAULT TRUE,
    is_cache BOOLEAN DEFAULT TRUE,
    menu_type CHAR(1),
    visible BOOLEAN DEFAULT TRUE,
    status CHAR(1) DEFAULT '0',
    perms VARCHAR(100),
    icon VARCHAR(100) DEFAULT '#',
    create_by VARCHAR(64),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    remark VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    dept_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT DEFAULT 0,
    ancestors VARCHAR(500),
    dept_name VARCHAR(30) NOT NULL,
    order_num INT DEFAULT 0,
    leader VARCHAR(20),
    phone VARCHAR(11),
    email VARCHAR(50),
    status CHAR(1) DEFAULT '0',
    del_flag CHAR(1) DEFAULT '0',
    create_by VARCHAR(64),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ========================================
-- 社区内容模块表
-- ========================================

-- 文章表
CREATE TABLE IF NOT EXISTS cms_article (
    article_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content TEXT,
    content_markdown TEXT,
    cover_url VARCHAR(255),
    author_id BIGINT NOT NULL,
    author_name VARCHAR(100),
    category_id BIGINT,
    category_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'draft',
    is_top BOOLEAN DEFAULT FALSE,
    is_recommend BOOLEAN DEFAULT FALSE,
    view_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    comment_count INT DEFAULT 0,
    word_count INT DEFAULT 0,
    reading_time VARCHAR(50),
    publish_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- 分类表
CREATE TABLE IF NOT EXISTS cms_category (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon VARCHAR(255),
    color VARCHAR(20),
    sort_order INT DEFAULT 0,
    article_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类表';

-- 标签表
CREATE TABLE IF NOT EXISTS cms_tag (
    tag_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(20),
    icon VARCHAR(255),
    article_count INT DEFAULT 0,
    usage_count INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签表';

-- 文章标签关联表
CREATE TABLE IF NOT EXISTS cms_article_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    INDEX idx_article_id (article_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

-- 评论表
CREATE TABLE IF NOT EXISTS cms_comment (
    comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    content_html TEXT,
    user_id BIGINT NOT NULL,
    user_nickname VARCHAR(100),
    user_avatar VARCHAR(255),
    reply_to_user_id BIGINT,
    reply_to_username VARCHAR(100),
    like_count INT DEFAULT 0,
    is_liked BOOLEAN DEFAULT FALSE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag BOOLEAN DEFAULT FALSE,
    INDEX idx_article_id (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 点赞表
CREATE TABLE IF NOT EXISTS cms_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    article_id BIGINT,
    comment_id BIGINT,
    like_type VARCHAR(20),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_article_id (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';

-- 收藏表
CREATE TABLE IF NOT EXISTS cms_collect (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_article_id (article_id),
    UNIQUE KEY uk_user_article (user_id, article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏记录表';

-- 用户资料表
CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(100),
    avatar VARCHAR(255),
    bio VARCHAR(500),
    gender VARCHAR(10),
    phone VARCHAR(20),
    wechat VARCHAR(100),
    position VARCHAR(100),
    points INT DEFAULT 0,
    level INT DEFAULT 1,
    ink_balance DECIMAL(10,2) DEFAULT 0.00,
    article_count INT DEFAULT 0,
    follower_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    is_author BOOLEAN DEFAULT FALSE,
    author_level INT DEFAULT 0,
    vip_expire_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料表';

-- 通知表
CREATE TABLE IF NOT EXISTS cms_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50),
    title VARCHAR(200),
    content TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    related_id VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入管理员用户（密码：admin123，使用 BCrypt 加密）
INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, password, status, del_flag, create_by, create_time) VALUES
(1, 'admin', '超级管理员', '00', 'admin@moyun.com', '15888888888', '1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW());

-- 插入普通用户
INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, password, status, del_flag, create_by, create_time) VALUES
(1, 'test', '测试用户', '00', 'test@moyun.com', '15999999999', '0', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW());

-- 插入角色
INSERT INTO sys_role (role_name, role_key, role_sort, status, del_flag, create_by, create_time) VALUES
('超级管理员', 'admin', 1, '0', '0', 'admin', NOW()),
('普通用户', 'user', 2, '0', '0', 'admin', NOW());

-- 插入用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2);

-- 插入分类
INSERT INTO cms_category (category_name, description, icon, color, sort_order, article_count, create_time) VALUES
('技术分享', '分享技术干货和经验总结', 'code', '#3b82f6', 1, 0, NOW()),
('生活随笔', '记录生活点滴和感悟', 'book', '#10b981', 2, 0, NOW()),
('学习笔记', '学习过程中的笔记和总结', 'document', '#f59e0b', 3, 0, NOW()),
('资源推荐', '推荐优质资源和工具', 'star', '#ef4444', 4, 0, NOW());

-- 插入标签
INSERT INTO cms_tag (tag_name, description, color, icon, article_count, sort_order, create_time) VALUES
('Java', 'Java 编程语言', '#ea7a00', NULL, 0, 1, NOW()),
('Vue', 'Vue.js 前端框架', '#42b883', NULL, 0, 2, NOW()),
('Spring Boot', 'Spring Boot 框架', '#6db33f', NULL, 0, 3, NOW()),
('MySQL', 'MySQL 数据库', '#00758f', NULL, 0, 4, NOW()),
('Python', 'Python 编程语言', '#3572A5', NULL, 0, 5, NOW());

-- 插入测试文章
INSERT INTO cms_article (title, summary, content, content_markdown, cover_url, author_id, author_name, category_id, category_name, status, is_top, is_recommend, view_count, like_count, comment_count, word_count, reading_time, publish_time, create_time) VALUES
('欢迎来到墨韵智库', '这是一个技术分享社区，欢迎大家交流分享！', '欢迎来到墨韵智库！\n\n这是一个技术分享社区，你可以在这里：\n\n1. 分享技术文章\n2. 交流学习经验\n3. 认识志同道合的朋友\n\n希望你能在这里有所收获！', '# 欢迎来到墨韵智库\n\n欢迎来到墨韵智库！\n\n这是一个技术分享社区，你可以在这里：\n\n1. 分享技术文章\n2. 交流学习经验\n3. 认识志同道合的朋友\n\n希望你能在这里有所收获！', NULL, 1, '超级管理员', 1, '技术分享', 'published', TRUE, TRUE, 100, 25, 5, 200, '5分钟', NOW(), NOW()),
('Spring Boot 快速入门', 'Spring Boot 是一个快速开发框架，让我们一起来学习吧！', '## Spring Boot 简介\n\nSpring Boot 是一个快速开发框架，它简化了 Spring 应用的初始搭建以及开发过程。', '## Spring Boot 简介\n\nSpring Boot 是一个快速开发框架，它简化了 Spring 应用的初始搭建以及开发过程。', NULL, 2, '测试用户', 1, '技术分享', 'published', FALSE, TRUE, 500, 89, 15, 3000, '10分钟', NOW(), NOW()),
('Vue 3 组合式 API', 'Vue 3 的组合式 API 是一个强大的特性，让我们一起来了解！', '## 组合式 API\n\nVue 3 的组合式 API 提供了一种更灵活的方式来组织组件逻辑。', '## 组合式 API\n\nVue 3 的组合式 API 提供了一种更灵活的方式来组织组件逻辑。', NULL, 2, '测试用户', 1, '技术分享', 'published', FALSE, FALSE, 234, 56, 8, 1500, '8分钟', NOW(), NOW());

-- 插入用户资料
INSERT INTO user_profile (user_id, nickname, avatar, bio, points, level, article_count, follower_count, following_count, like_count, is_author, create_time) VALUES
(1, '超级管理员', NULL, '墨韵智库管理员', 1000, 5, 1, 100, 50, 200, TRUE, NOW()),
(2, '测试用户', NULL, '热爱技术的程序员', 200, 2, 2, 20, 10, 50, TRUE, NOW());

-- 插入评论
INSERT INTO cms_comment (article_id, parent_id, content, content_html, user_id, user_nickname, user_avatar, like_count, create_time) VALUES
(1, NULL, '写得很好，支持！', '<p>写得很好，支持！</p>', 2, '测试用户', NULL, 10, NOW()),
(1, NULL, '期待更多文章！', '<p>期待更多文章！</p>', 1, '超级管理员', NULL, 5, NOW());

-- 插入文章标签关联
INSERT INTO cms_article_tag (article_id, tag_id) VALUES
(1, 1), (1, 3),
(2, 1), (2, 3),
(3, 2);
