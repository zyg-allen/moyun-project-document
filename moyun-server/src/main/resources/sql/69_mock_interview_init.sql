-- =====================================================
-- AI 模拟面试官 DDL 脚本（任务 3.10 学习者成长闭环）
-- 简化实现：不依赖外部 LLM，使用规则化评分（关键词匹配 + 答案长度）
-- 题目来源：portal_interview_question 表
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 57_column_init.sql 之后执行
-- =====================================================

-- 模拟面试会话表
CREATE TABLE IF NOT EXISTS `portal_mock_interview` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL                COMMENT '面试用户ID',
    `position`   VARCHAR(64)  DEFAULT NULL            COMMENT '面试岗位（如 后端开发/前端开发）',
    `scene`      VARCHAR(64)  DEFAULT NULL            COMMENT '面试场景（如 算法/系统设计/项目深挖，对应题目分类）',
    `status`     VARCHAR(16)  NOT NULL DEFAULT 'in_progress' COMMENT '状态 in_progress/finished',
    `total_qa`   INT          NOT NULL DEFAULT 0      COMMENT '题目总数',
    `score`      INT          DEFAULT NULL            COMMENT '面试总分（0-100，结束面试时计算）',
    `summary`    TEXT                                  COMMENT 'AI 生成的面试总结',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟面试会话';

-- 模拟面试问答表
-- 注：question_id 为关联 portal_interview_question.id 的冗余外键，
-- 用于 AI 评分时回查题目 tags/solution 作为评分参考要点（任务规范外必要扩展）
CREATE TABLE IF NOT EXISTS `portal_mock_interview_qa` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `interview_id`  BIGINT       NOT NULL                COMMENT '面试会话ID',
    `question_id`   BIGINT       DEFAULT NULL             COMMENT '关联题目ID（portal_interview_question.id）',
    `question_idx`  INT          NOT NULL                 COMMENT '题目序号（从 0 开始）',
    `question`      VARCHAR(1000) NOT NULL                COMMENT '面试问题（快照自题目标题）',
    `user_answer`   TEXT                                  COMMENT '用户回答',
    `ai_feedback`   TEXT                                  COMMENT 'AI 反馈（规则化生成）',
    `score`         INT          DEFAULT NULL             COMMENT '本题评分（0-100）',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_interview` (`interview_id`),
    KEY `idx_question_idx` (`interview_id`, `question_idx`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟面试问答';
