-- ============================================================
-- 读书模块 v1.0 第三阶段：推荐位表
-- 创建时间：2026-06-30
-- 说明：
--   新建 portal_book_recommend 推荐位表（首页/分类页/发现页推荐位管理）
--   设计要点：
--     1. 推荐位与书籍解耦，运营上下架不动书籍数据
--     2. position 枚举：home_banner/home_hot/category_top/limit_free/discover_banner
--     3. 时间窗：start_time/end_time 控制推荐有效期，到期查询时自动过滤
--     4. UK(book_id, position) 防止同一书籍在同一位置重复推荐
-- ============================================================

-- -------------------------------------------------------
-- 推荐位表
--    使用 CREATE TABLE IF NOT EXISTS 保证脚本可重入
-- -------------------------------------------------------
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

-- ============================================================
-- 脚本执行完成
-- ============================================================
SELECT '推荐位表创建完成！' AS message;
