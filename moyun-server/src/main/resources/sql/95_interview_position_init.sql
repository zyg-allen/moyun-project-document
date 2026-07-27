-- ============================================
-- 墨韵智库 - 面试岗位字典与画像驱动抽题支持（v5.9 阶段0）
-- ============================================
-- 本脚本完成三件事：
--   1. 新建 portal_interview_position 岗位字典表（驱动分类与画像抽题）
--   2. 扩展 portal_user_stats 表：新增 mock_interview_count / avg_mock_score / weak_tags 字段
--      —— weak_tags 缓存用户薄弱知识点（JSON 数组），供模拟面试优先抽题使用
--   3. 扩展 portal_mock_interview 表：新增 is_personalized / profile_snapshot 字段
--      —— 标记本次面试是否基于画像抽题，并快照当时的画像以便回溯
--   4. 预置 Java 后端 / 前端工程师 / 算法工程师 3 个岗位字典数据
--
-- 设计原则：
--   - 不破坏存量数据，所有新增字段均 DEFAULT NULL 或 DEFAULT 0
--   - weak_tags 用 JSON 字符串存储，避免新增关联表，Service 层负责解析
--   - profile_snapshot 同样为 JSON 字符串，仅在画像驱动面试时写入
--   - 岗位字典 required_skills 用 JSON 数组存储技能标签名（与 portal_tag.name 对齐）
--
-- @author moyun
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 面试岗位字典表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_position` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`            VARCHAR(64)  NOT NULL                COMMENT '岗位编码（如 java_backend）',
    `name`            VARCHAR(100) NOT NULL                COMMENT '岗位名称（如 Java后端工程师）',
    `industry`        VARCHAR(50)  DEFAULT NULL            COMMENT '所属行业（如 互联网/金融/制造）',
    `level`           VARCHAR(32)  DEFAULT NULL            COMMENT '岗位级别（junior/mid/senior）',
    `required_skills` TEXT                                 COMMENT '必备技能 JSON 数组（如 ["Spring","MySQL","Redis"]，与 portal_tag.name 对齐）',
    `hot_companies`   TEXT                                 COMMENT '热门公司 JSON 数组（如 ["阿里","腾讯","字节"]）',
    `description`     VARCHAR(500) DEFAULT NULL            COMMENT '岗位描述',
    `sort`            INT          DEFAULT 0               COMMENT '排序',
    `status`          VARCHAR(16)  DEFAULT 'active'        COMMENT '状态 active/inactive',
    `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`          VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试岗位字典表';

-- ----------------------------
-- 2. 扩展 portal_user_stats：新增面试统计与薄弱点字段
-- ----------------------------
-- 使用 ADD COLUMN IF NOT EXISTS 兼容性写法（MySQL 8.0 不支持 IF NOT EXISTS，依赖幂等性，重复执行会报错可忽略）
-- 这里采用 INFORMATION_SCHEMA 检查，确保脚本可重复执行
-- ----------------------------
DROP PROCEDURE IF EXISTS p_add_column_if_not_exists;
DELIMITER $$
CREATE PROCEDURE p_add_column_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT,
    IN p_comment VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition, ' COMMENT ''', p_comment, '''');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- portal_user_stats 扩展：面试统计
CALL p_add_column_if_not_exists('portal_user_stats', 'mock_interview_count',
    'INT DEFAULT 0', '模拟面试次数');
CALL p_add_column_if_not_exists('portal_user_stats', 'avg_mock_score',
    'INT DEFAULT 0', '模拟面试平均分');
CALL p_add_column_if_not_exists('portal_user_stats', 'weak_tags',
    'TEXT', '薄弱知识点 JSON 数组（如 [{"tagId":1,"tagName":"Spring","failRate":0.6}]）');
CALL p_add_column_if_not_exists('portal_user_stats', 'weak_tags_updated_time',
    'DATETIME DEFAULT NULL', '薄弱点最后计算时间');

-- portal_mock_interview 扩展：画像驱动标记
CALL p_add_column_if_not_exists('portal_mock_interview', 'is_personalized',
    'TINYINT(1) DEFAULT 0', '是否基于画像抽题（0随机 1画像驱动）');
CALL p_add_column_if_not_exists('portal_mock_interview', 'profile_snapshot',
    'TEXT', '抽题时的画像快照 JSON（含薄弱点列表，便于回溯分析）');

DROP PROCEDURE IF EXISTS p_add_column_if_not_exists;

-- ----------------------------
-- 3. 预置岗位字典数据：Java后端 / 前端工程师 / 算法工程师
-- ----------------------------
-- required_skills / hot_companies 为 JSON 字符串，与 portal_tag.name 对齐
-- 重复执行时通过 uk_code 唯一约束保证幂等（INSERT IGNORE）
-- ----------------------------
INSERT IGNORE INTO `portal_interview_position` (`code`, `name`, `industry`, `level`, `required_skills`, `hot_companies`, `description`, `sort`, `status`, `create_time`) VALUES
('java_backend', 'Java后端工程师', '互联网', 'mid',
 '["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]',
 '["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]',
 'Java 后端工程师岗位，重点考察 Java 基础、Spring 全家桶、MySQL/Redis、分布式与微服务、JVM 与并发编程', 1, 'active', NOW()),

('frontend', '前端工程师', '互联网', 'mid',
 '["JavaScript","TypeScript","Vue","React","HTML","CSS","Node.js","Webpack","Vite","性能优化","浏览器原理","HTTP"]',
 '["阿里","腾讯","字节跳动","美团","京东","百度","网易","小米","Shopee","滴滴"]',
 '前端工程师岗位，重点考察 JS/TS 基础、Vue/React 框架、工程化、浏览器原理、性能优化、HTTP 与网络', 2, 'active', NOW()),

('algorithm', '算法工程师', '互联网', 'mid',
 '["算法","数据结构","动态规划","图论","字符串","数组","链表","树","递归","排序","机器学习","深度学习","数学"]',
 '["阿里","腾讯","字节跳动","百度","美团","快手","小红书","华为","商汤","旷视"]',
 '算法工程师岗位，重点考察数据结构与算法、动态规划、图论、字符串算法、机器学习与深度学习基础', 3, 'active', NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 阶段0 SQL 迁移完成
-- ============================================
