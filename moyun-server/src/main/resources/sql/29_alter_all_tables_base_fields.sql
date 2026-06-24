-- =====================================================
-- 墨韵·智库 - 全表补全 BaseEntity 审计字段（统一补丁）
-- 版本: v1.0
-- 日期: 2026-06-24
-- 说明: 除工作流表外，所有业务表强制对齐 BaseEntity 的 5 个持久化字段：
--       create_by / create_time / update_by / update_time / remark
--       本脚本使用存储过程安全添加（已存在则跳过），可重复执行
-- 注意: 28_alter_tables_add_base_fields.sql 已处理 15 张面试/读书表，
--       本脚本覆盖剩余 25 张表（sys_* + portal_*），两者无冲突
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
-- 一、系统模块表（01_moyun_init.sql）
-- =====================================================

-- 1. sys_dept（缺 remark）
CALL AddColumnIfNotExists('sys_dept', 'remark', "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 2. sys_user_role（缺全部 5 个）
CALL AddColumnIfNotExists('sys_user_role', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('sys_user_role', 'create_time', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL AddColumnIfNotExists('sys_user_role', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('sys_user_role', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('sys_user_role', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 3. sys_role_menu（缺全部 5 个）
CALL AddColumnIfNotExists('sys_role_menu', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('sys_role_menu', 'create_time', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL AddColumnIfNotExists('sys_role_menu', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('sys_role_menu', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('sys_role_menu', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 4. sys_role_dept（缺全部 5 个）
CALL AddColumnIfNotExists('sys_role_dept', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('sys_role_dept', 'create_time', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL AddColumnIfNotExists('sys_role_dept', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('sys_role_dept', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('sys_role_dept', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 5. sys_user_post（缺全部 5 个）
CALL AddColumnIfNotExists('sys_user_post', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('sys_user_post', 'create_time', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL AddColumnIfNotExists('sys_user_post', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('sys_user_post', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('sys_user_post', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 6. sys_oper_log（缺全部 5 个）
CALL AddColumnIfNotExists('sys_oper_log', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('sys_oper_log', 'create_time', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL AddColumnIfNotExists('sys_oper_log', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('sys_oper_log', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('sys_oper_log', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 7. sys_logininfor（缺全部 5 个）
CALL AddColumnIfNotExists('sys_logininfor', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('sys_logininfor', 'create_time', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL AddColumnIfNotExists('sys_logininfor', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('sys_logininfor', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('sys_logininfor', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 8. sys_job_log（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('sys_job_log', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('sys_job_log', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('sys_job_log', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('sys_job_log', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 9. gen_table_column（缺 remark）
CALL AddColumnIfNotExists('gen_table_column', 'remark', "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 10. sys_deploy_form（缺全部 5 个）
CALL AddColumnIfNotExists('sys_deploy_form', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('sys_deploy_form', 'create_time', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL AddColumnIfNotExists('sys_deploy_form', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('sys_deploy_form', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('sys_deploy_form', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- =====================================================
-- 二、门户模块表（03_portal_init.sql）
-- =====================================================

-- 11. portal_article_tag（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_article_tag', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_article_tag', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_article_tag', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_article_tag', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 12. portal_bookmark（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_bookmark', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_bookmark', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_bookmark', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_bookmark', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 13. portal_like（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_like', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_like', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_like', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_like', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 14. portal_comment_like（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_comment_like', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_comment_like', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_comment_like', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_comment_like', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 15. portal_follow（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_follow', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_follow', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_follow', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_follow', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 16. portal_notification（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_notification', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_notification', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_notification', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_notification', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 17. portal_wallet（缺 create_by, update_by, remark）
CALL AddColumnIfNotExists('portal_wallet', 'create_by', "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_wallet', 'update_by', "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_wallet', 'remark',   "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 18. portal_wallet_transaction（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_wallet_transaction', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_wallet_transaction', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_wallet_transaction', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_wallet_transaction', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- =====================================================
-- 三、读书/面试模块表（07_reading_interview_init.sql）
-- 注：28 脚本已处理 portal_interview_submission/bookmark/question_like/
--     attempt/experience_like/reading_progress/book_quote，
--     此处补充 28 脚本未覆盖的 6 张表
-- =====================================================

-- 19. portal_book_list_item（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_book_list_item', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_book_list_item', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_book_list_item', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_book_list_item', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 20. portal_book_quote_like（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_book_quote_like', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_book_quote_like', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_book_quote_like', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_book_quote_like', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 21. portal_book_list_like（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_book_list_like', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_book_list_like', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_book_list_like', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_book_list_like', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 22. portal_book_club_participant（缺全部 5 个，使用 join_time 而非 create_time）
CALL AddColumnIfNotExists('portal_book_club_participant', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_book_club_participant', 'create_time', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL AddColumnIfNotExists('portal_book_club_participant', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_book_club_participant', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_book_club_participant', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 23. portal_book_club_record（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_book_club_record', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_book_club_record', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_book_club_record', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_book_club_record', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 24. portal_book_club_record_like（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_book_club_record_like', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_book_club_record_like', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_book_club_record_like', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_book_club_record_like', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- =====================================================
-- 四、文章浏览表（13_create_article_view_table.sql）
-- =====================================================

-- 25. portal_article_view（缺全部 5 个，使用 view_time 而非 create_time）
CALL AddColumnIfNotExists('portal_article_view', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_article_view', 'create_time', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL AddColumnIfNotExists('portal_article_view', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_article_view', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_article_view', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- =====================================================
-- 五、成长体系表（19_growth_system_init.sql）
-- =====================================================

-- 26. portal_user_growth（缺 create_by, update_by, remark）
CALL AddColumnIfNotExists('portal_user_growth', 'create_by', "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_user_growth', 'update_by', "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_user_growth', 'remark',   "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 27. portal_growth_log（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_growth_log', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_growth_log', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_growth_log', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_growth_log', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 28. portal_user_stats（缺 create_by, update_by, remark）
CALL AddColumnIfNotExists('portal_user_stats', 'create_by', "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_user_stats', 'update_by', "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_user_stats', 'remark',   "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 29. portal_growth_rule（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_growth_rule', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_growth_rule', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_growth_rule', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_growth_rule', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 30. portal_achievement（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_achievement', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_achievement', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_achievement', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_achievement', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- 31. portal_user_badge（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_user_badge', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_user_badge', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_user_badge', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_user_badge', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- =====================================================
-- 六、简历模板点赞表（20_resume_template_like.sql）
-- =====================================================

-- 32. portal_interview_resume_template_like（缺 create_by, update_by, update_time, remark）
CALL AddColumnIfNotExists('portal_interview_resume_template_like', 'create_by',   "VARCHAR(64) DEFAULT '' COMMENT '创建者'");
CALL AddColumnIfNotExists('portal_interview_resume_template_like', 'update_by',   "VARCHAR(64) DEFAULT '' COMMENT '更新者'");
CALL AddColumnIfNotExists('portal_interview_resume_template_like', 'update_time', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL AddColumnIfNotExists('portal_interview_resume_template_like', 'remark',      "VARCHAR(500) DEFAULT NULL COMMENT '备注'");

-- =====================================================
-- 清理存储过程
-- =====================================================
DROP PROCEDURE IF EXISTS `AddColumnIfNotExists`;

-- =====================================================
-- 补丁执行完成
-- 共修复 25 张表（28 脚本已处理 15 张，合计 40 张）
-- 所有业务表（除工作流表）已对齐 BaseEntity 审计字段
-- =====================================================
