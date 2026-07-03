-- =====================================================
-- 每日写作 prompt DDL 脚本（创作者天堂核心）
-- 每日推送一个写作 prompt，激发创作灵感
-- 幂等设计：CREATE TABLE IF NOT EXISTS + INSERT IGNORE
-- 执行顺序：本脚本在 60_contest_init.sql 之后执行
-- =====================================================

-- 每日写作 prompt
CREATE TABLE IF NOT EXISTS `portal_writing_prompt` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `prompt_date`  DATE          NOT NULL                COMMENT 'prompt 日期（唯一）',
    `title`        VARCHAR(128)  NOT NULL                COMMENT 'prompt 标题',
    `description`  TEXT                                  COMMENT 'prompt 描述',
    `category`     VARCHAR(32)   DEFAULT NULL            COMMENT '分类（如：生活/职场/情感/虚构/哲思）',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_prompt_date` (`prompt_date`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日写作 prompt';

-- 种子数据：最近 7 天的 prompt（INSERT IGNORE 保证幂等）
-- 注意：使用 CURDATE() 动态生成最近 7 天的日期，避免硬编码绝对日期
INSERT IGNORE INTO `portal_writing_prompt` (`prompt_date`, `title`, `description`, `category`) VALUES
(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '一封信', '请以书信的形式，写一封给十年后自己的信。可以是忠告、可以是期许，也可以是当下的困惑。', '生活'),
(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '雨夜', '描述一个雨夜的场景：一个未眠的人，一扇半开的窗，一段未说完的话。', '情感'),
(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '通勤路上', '记录一次通勤路上的所见所闻。一个陌生人、一段广播、一闪而过的风景，都可能成为故事的开端。', '生活'),
(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '如果时间可以暂停', '假如你拥有让时间暂停 30 秒的能力，你会用它做什么？请写一个具体的场景。', '虚构'),
(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '面试官的沉默', '一场面试中，面试官在某个问题后沉默了 10 秒。请描写那 10 秒里应聘者的内心活动。', '职场'),
(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '一件旧物', '选择一件你保留多年的旧物，写它背后的故事。它从哪里来，又见证了什么？', '情感'),
(CURDATE(), '此刻的光', '观察此刻你所在空间里的光：它的颜色、强度、来源、投下的影子。用 300 字描绘它，并赋予它一种情绪。', '哲思');
