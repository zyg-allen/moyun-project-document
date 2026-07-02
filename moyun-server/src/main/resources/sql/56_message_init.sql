-- =============================================================
-- 私信功能初始化（会话 + 消息）
-- 说明：时间字段使用 create_time / update_time，与 BaseEntity 保持一致
--       （MyMetaObjectHandler 自动填充 createTime/updateTime）
-- =============================================================

-- 私信会话
CREATE TABLE IF NOT EXISTS portal_message_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_a BIGINT NOT NULL COMMENT '用户A（较小ID）',
    user_b BIGINT NOT NULL COMMENT '用户B（较大ID）',
    last_message_id BIGINT COMMENT '最后一条消息ID',
    last_message_content VARCHAR(500) COMMENT '最后消息内容预览',
    last_message_time DATETIME COMMENT '最后消息时间',
    unread_a INT DEFAULT 0 COMMENT 'A未读数',
    unread_b INT DEFAULT 0 COMMENT 'B未读数',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_users (user_a, user_b),
    INDEX idx_user_a (user_a),
    INDEX idx_user_b (user_b),
    INDEX idx_last_time (last_message_time)
) COMMENT='私信会话';

-- 私信消息
CREATE TABLE IF NOT EXISTS portal_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者',
    receiver_id BIGINT NOT NULL COMMENT '接收者',
    content TEXT NOT NULL COMMENT '消息内容',
    msg_type VARCHAR(16) DEFAULT 'text' COMMENT 'text/image/file',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读',
    create_time DATETIME COMMENT '创建时间',
    INDEX idx_session_time (session_id, create_time),
    INDEX idx_receiver_read (receiver_id, is_read)
) COMMENT='私信消息';
