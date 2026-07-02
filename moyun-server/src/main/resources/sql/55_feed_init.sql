-- =============================================================
-- 动态/Feed 流初始化（推拉结合，先实现"读时拉"模式）
-- @author moyun
-- =============================================================

-- 动态事件表
CREATE TABLE IF NOT EXISTS portal_feed_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '事件发布者',
    event_type VARCHAR(32) NOT NULL COMMENT 'publish_article/publish_experience/new_column/checkin等',
    target_type VARCHAR(32) NOT NULL COMMENT 'article/experience/column/book等',
    target_id BIGINT NOT NULL COMMENT '目标对象ID',
    title VARCHAR(256) COMMENT '目标标题',
    summary VARCHAR(500) COMMENT '动态摘要',
    cover VARCHAR(500) COMMENT '封面图',
    created_time DATETIME NOT NULL,
    INDEX idx_user_time (user_id, created_time),
    INDEX idx_type_time (event_type, created_time)
) COMMENT='动态事件流';

-- 用户收件箱（推模式，关注者收件箱）
CREATE TABLE IF NOT EXISTS portal_feed_inbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '接收者',
    event_id BIGINT NOT NULL COMMENT '动态事件ID',
    created_time DATETIME NOT NULL,
    INDEX idx_user_time (user_id, created_time)
) COMMENT='动态收件箱';
