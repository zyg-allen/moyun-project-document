-- ============================================================
-- 41_init_comment_like.sql
-- 文章评论点赞表（对齐 portal_interview_comment_like 结构）
-- 对应 BUG：前端 comment.ts likeComment 调用 POST /portal/comment/{id}/like
--          后端原无此接口，本次补充
-- ============================================================

DROP TABLE IF EXISTS `portal_comment_like`;
CREATE TABLE `portal_comment_like` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `comment_id`  BIGINT       NOT NULL COMMENT '评论ID',
    `user_id`     BIGINT       NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME     DEFAULT NULL COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`) COMMENT '评论+用户唯一索引，防重复点赞',
    KEY `idx_user_id` (`user_id`) COMMENT '用户维度查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章评论点赞记录';
