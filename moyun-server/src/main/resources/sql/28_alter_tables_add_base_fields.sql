-- =====================================================
-- 墨韵·智库 - 补全 BaseEntity 审计字段
-- 版本: v1.0
-- 日期: 2026-06-24
-- 说明: 所有继承 BaseEntity 的 Entity 都需要表包含
--       create_by / create_time / update_by / update_time / remark
--       本脚本使用存储过程安全添加缺失字段（已存在则跳过）
-- =====================================================

-- -----------------------------------------------------
-- 存储过程：安全添加列（不存在时才添加）
-- -----------------------------------------------------
DROP PROCEDURE IF EXISTS `AddColumnIfNotExists`;
DELIMITER $$
CREATE PROCEDURE `AddColumnIfNotExists`(
    IN p_table_name  VARCHAR(100),
    IN p_column_name VARCHAR(100),
    IN p_definition  VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table_name
          AND COLUMN_NAME  = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name,
                          '` ADD COLUMN `', p_column_name, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- =====================================================
-- 一、面试模块表（来自 portal_module_ddl_v2.sql）
-- =====================================================

-- 1. portal_interview_experience（面经表）—— 报错表
--    已有: create_time, update_time
--    缺少: create_by, update_by, remark
CALL AddColumnIfNotExists('portal_interview_experience', 'create_by', "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_experience', 'update_by', "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_experience', 'remark',   "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 2. portal_interview_comment（面试评论表）
--    已有: create_time, update_time
--    缺少: create_by, update_by, remark
CALL AddColumnIfNotExists('portal_interview_comment', 'create_by', "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_comment', 'update_by', "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_comment', 'remark',   "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 3. portal_interview_resume_template（简历模板表）
--    已有: create_time, update_time
--    缺少: create_by, update_by, remark
CALL AddColumnIfNotExists('portal_interview_resume_template', 'create_by', "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_resume_template', 'update_by', "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_resume_template', 'remark',   "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 4. portal_interview_company（面试公司表）
--    已有: create_time, update_time
--    缺少: create_by, update_by, remark
CALL AddColumnIfNotExists('portal_interview_company', 'create_by', "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_company', 'update_by', "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_company', 'remark',   "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 5. portal_interview_submission（面试提交记录表）
--    已有: create_time
--    缺少: create_by, update_by, update_time, remark
CALL AddColumnIfNotExists('portal_interview_submission', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_submission', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_submission', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_interview_submission', 'remark',     "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 6. portal_interview_bookmark（面试题目书签表）
--    已有: create_time
--    缺少: create_by, update_by, update_time, remark
CALL AddColumnIfNotExists('portal_interview_bookmark', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_bookmark', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_bookmark', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_interview_bookmark', 'remark',     "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 7. portal_interview_question_like（面试题目点赞表）
--    已有: create_time
--    缺少: create_by, update_by, update_time, remark
CALL AddColumnIfNotExists('portal_interview_question_like', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_question_like', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_question_like', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_interview_question_like', 'remark',     "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 8. portal_interview_attempt（面试尝试表）
--    已有: 无审计字段
--    缺少: create_by, create_time, update_by, update_time, remark
CALL AddColumnIfNotExists('portal_interview_attempt', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_attempt', 'create_time', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL AddColumnIfNotExists('portal_interview_attempt', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_attempt', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_interview_attempt', 'remark',     "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 9. portal_interview_experience_like（面经点赞表）
--    已有: create_time
--    缺少: create_by, update_by, update_time, remark
CALL AddColumnIfNotExists('portal_interview_experience_like', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_experience_like', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_experience_like', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_interview_experience_like', 'remark',     "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 10. portal_interview_comment_like（面试评论点赞表）
--     已有: create_time
--     缺少: create_by, update_by, update_time, remark
CALL AddColumnIfNotExists('portal_interview_comment_like', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_comment_like', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_comment_like', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_interview_comment_like', 'remark',     "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 11. portal_interview_question_company（题目-公司关联表）
--     已有: create_time
--     缺少: create_by, update_by, update_time, remark
CALL AddColumnIfNotExists('portal_interview_question_company', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_question_company', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_question_company', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_interview_question_company', 'remark',     "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 12. portal_entity_tag（通用实体标签关联表）
--     已有: create_time
--     缺少: create_by, update_by, update_time, remark
CALL AddColumnIfNotExists('portal_entity_tag', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_entity_tag', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_entity_tag', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_entity_tag', 'remark',     "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- =====================================================
-- 二、读书模块表（来自 07_reading_interview_init.sql）
-- =====================================================

-- 13. portal_reading_progress（阅读进度表）
--     已有: create_time, update_time
--     缺少: create_by, update_by, remark
CALL AddColumnIfNotExists('portal_reading_progress', 'create_by', "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_reading_progress', 'update_by', "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_reading_progress', 'remark',   "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 14. portal_book_quote（金句摘录表）
--     已有: create_time, update_time
--     缺少: create_by, update_by, remark
CALL AddColumnIfNotExists('portal_book_quote', 'create_by', "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_book_quote', 'update_by', "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_book_quote', 'remark',   "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 15. portal_book_list_bookmark（书单收藏表，来自 22_booklist_bookmark.sql）
--     已有: create_time
--     缺少: create_by, update_by, update_time, remark
CALL AddColumnIfNotExists('portal_book_list_bookmark', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_book_list_bookmark', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_book_list_bookmark', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_book_list_bookmark', 'remark',     "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- =====================================================
-- 清理存储过程
-- =====================================================
DROP PROCEDURE IF EXISTS `AddColumnIfNotExists`;

-- =====================================================
-- 补丁执行完成
-- 共修复 15 张表，补全 BaseEntity 审计字段
-- =====================================================
