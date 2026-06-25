-- =============================================
-- 墨韵智库 - portal_article 状态枚举补充
-- 执行顺序: 33 (在 32_cleanup_tag_name_prefix.sql 之后执行)
-- 背景:
--   BUG-002 要求文章支持完整的状态流转：
--   draft（草稿）→ pending（待审核）→ published（已发布）/ rejected（已拒绝）
--   原 DDL 注释仅包含 draft/published/archived，需补充 pending / rejected 状态说明。
--   本脚本仅修改字段注释，不修改数据（兼容已有数据）。
-- 兼容: MySQL 8+（可重复执行）
-- =============================================

-- 修改 status 字段注释，明确状态枚举值
ALTER TABLE `portal_article`
    MODIFY COLUMN `status` varchar(20) DEFAULT 'draft'
    COMMENT '状态：draft=草稿 / pending=待审核 / published=已发布 / rejected=已拒绝 / archived=已归档';

-- 校验结果
SELECT
    COLUMN_NAME,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'portal_article'
  AND COLUMN_NAME = 'status';

-- =============================================
-- 状态流转说明（供后台审核接口参考）：
--   draft     → pending     ：用户点击发布，提交审核
--   pending   → published   ：后台审核通过
--   pending   → rejected    ：后台审核拒绝（remark 字段记录拒绝原因）
--   rejected  → pending     ：用户修改后重新提交
--   published → archived    ：文章下架归档
-- =============================================

SELECT 'portal_article 状态枚举注释更新完成！' AS message;
