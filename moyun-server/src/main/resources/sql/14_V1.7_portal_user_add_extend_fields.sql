-- =============================================
-- 墨韵智库 - portal_user 表结构更新脚本
-- 版本: v1.7
-- 日期: 2026-06-09
-- 说明: 新增用户资料扩展字段（性别、生日、地点、社交链接等）
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 1. 新增用户资料扩展字段
-- =============================================
ALTER TABLE `portal_user`
    -- 个人资料字段（按实体类顺序）
    ADD COLUMN `gender` VARCHAR(20) DEFAULT NULL COMMENT '性别' AFTER `wechat`,
    ADD COLUMN `birthday` VARCHAR(20) DEFAULT NULL COMMENT '生日' AFTER `gender`,
    ADD COLUMN `location` VARCHAR(100) DEFAULT NULL COMMENT '所在城市' AFTER `birthday`,
    ADD COLUMN `website` VARCHAR(200) DEFAULT NULL COMMENT '个人网站' AFTER `location`,
    ADD COLUMN `github` VARCHAR(100) DEFAULT NULL COMMENT 'GitHub账号' AFTER `website`,
    ADD COLUMN `company` VARCHAR(200) DEFAULT NULL COMMENT '公司' AFTER `github`,
    ADD COLUMN `school` VARCHAR(200) DEFAULT NULL COMMENT '学校' AFTER `company`,
    ADD COLUMN `language` VARCHAR(20) DEFAULT NULL COMMENT '语言偏好' AFTER `school`,
    ADD COLUMN `timezone` VARCHAR(50) DEFAULT NULL COMMENT '时区' AFTER `language`,
    
    -- 通知设置字段
    ADD COLUMN `notify_like` TINYINT(1) DEFAULT 1 COMMENT '是否接收点赞通知' AFTER `timezone`,
    ADD COLUMN `notify_comment` TINYINT(1) DEFAULT 1 COMMENT '是否接收评论通知' AFTER `notify_like`,
    ADD COLUMN `notify_follow` TINYINT(1) DEFAULT 1 COMMENT '是否接收关注通知' AFTER `notify_comment`,
    ADD COLUMN `notify_system` TINYINT(1) DEFAULT 1 COMMENT '是否接收系统通知' AFTER `notify_follow`,
    
    -- 隐私设置字段
    ADD COLUMN `privacy_follow` TINYINT(1) DEFAULT 1 COMMENT '是否允许被关注' AFTER `notify_system`,
    ADD COLUMN `privacy_bookmark` TINYINT(1) DEFAULT 1 COMMENT '是否公开收藏夹' AFTER `privacy_follow`,
    ADD COLUMN `privacy_email` TINYINT(1) DEFAULT 0 COMMENT '是否公开邮箱' AFTER `privacy_bookmark`,
    ADD COLUMN `privacy_phone` TINYINT(1) DEFAULT 0 COMMENT '是否公开手机号' AFTER `privacy_email`;

-- =============================================
-- 2. 添加字段注释（若需要）
-- =============================================
ALTER TABLE `portal_user`
    MODIFY COLUMN `gender` VARCHAR(20) DEFAULT NULL COMMENT '性别：male-男，female-女，other-其他',
    MODIFY COLUMN `birthday` VARCHAR(20) DEFAULT NULL COMMENT '生日：YYYY-MM-DD格式',
    MODIFY COLUMN `location` VARCHAR(100) DEFAULT NULL COMMENT '所在城市：如北京市',
    MODIFY COLUMN `website` VARCHAR(200) DEFAULT NULL COMMENT '个人网站URL',
    MODIFY COLUMN `github` VARCHAR(100) DEFAULT NULL COMMENT 'GitHub用户名或完整URL',
    MODIFY COLUMN `company` VARCHAR(200) DEFAULT NULL COMMENT '公司名称',
    MODIFY COLUMN `school` VARCHAR(200) DEFAULT NULL COMMENT '学校名称',
    MODIFY COLUMN `language` VARCHAR(20) DEFAULT NULL COMMENT '语言偏好：zh-CN，en-US等',
    MODIFY COLUMN `timezone` VARCHAR(50) DEFAULT NULL COMMENT '时区：如Asia/Shanghai';

-- =============================================
-- 3. 确认更新
-- =============================================
SELECT 'portal_user 表结构更新完成！' AS message;

SET FOREIGN_KEY_CHECKS = 1;