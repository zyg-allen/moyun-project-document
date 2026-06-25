-- =============================================
-- 墨韵智库 - 清理 portal_tag.name 的 # 前缀（数据迁移）
-- 执行顺序: 32 (在 31_create_portal_entity_tag.sql 之后执行)
-- 背景:
--   历史初始化脚本 05_moyun_v2_init.sql 中标签 name 存储 '#生活哲思'（带 # 前缀）。
--   行业标准：数据库应存储纯文本，# 属于展示符号，由前端按需拼接。
--   本脚本用于清理已入库数据的 # 前缀，并合并可能产生的重复标签。
-- 兼容: MySQL 8+（使用存储过程，可重复执行）
-- =============================================

-- =============================================
-- 步骤 1：合并重复标签
-- 说明：去掉 # 前缀后可能出现重名标签（如 '#生活哲思' 与 '生活哲思' 同时存在）。
--       策略：保留 id 较小者（先创建的），把 portal_entity_tag 中重复标签的引用
--       迁移到保留标签上，累加 reference_count，最后删除重复标签。
-- =============================================

DROP PROCEDURE IF EXISTS proc_merge_duplicate_tags;
DELIMITER $$
CREATE PROCEDURE proc_merge_duplicate_tags()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_keep_id BIGINT;
    DECLARE v_dup_id BIGINT;
    DECLARE v_keep_name VARCHAR(255);
    DECLARE v_module VARCHAR(50);
    DECLARE v_keep_ref BIGINT;
    DECLARE v_dup_ref BIGINT;

    -- 查找需要合并的重复标签对（去掉 # 前缀后重名，且 module 相同）
    -- keep_id = 较小的 id（保留），dup_id = 较大的 id（待删除）
    DECLARE cur CURSOR FOR
SELECT
    t_keep.id AS keep_id,
    t_dup.id  AS dup_id,
    TRIM(LEADING '#' FROM t_keep.name) AS keep_name,
    t_keep.module
FROM portal_tag t_dup
         INNER JOIN portal_tag t_keep
                    ON TRIM(LEADING '#' FROM t_dup.name) = TRIM(LEADING '#' FROM t_keep.name)
                        AND (t_dup.module = t_keep.module OR (t_dup.module IS NULL AND t_keep.module IS NULL))
                        AND t_keep.id < t_dup.id
WHERE t_dup.name LIKE '#%' OR t_keep.name LIKE '#%';

DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

OPEN cur;
read_loop: LOOP
        FETCH cur INTO v_keep_id, v_dup_id, v_keep_name, v_module;
        IF done THEN LEAVE read_loop; END IF;

        -- 1) 累加 reference_count 到保留标签
SELECT reference_count INTO v_dup_ref FROM portal_tag WHERE id = v_dup_id;
IF v_dup_ref IS NULL THEN SET v_dup_ref = 0; END IF;
UPDATE portal_tag
SET reference_count = COALESCE(reference_count, 0) + v_dup_ref
WHERE id = v_keep_id;

-- 2) 把 portal_entity_tag 中指向 dup_id 的引用迁移到 keep_id
--    使用 INSERT IGNORE 防止唯一键冲突（同一 entity 已绑定 keep_id 的情况）
INSERT IGNORE INTO portal_entity_tag (tag_id, entity_type, entity_id, sort, create_time)
SELECT v_keep_id, entity_type, entity_id, sort, create_time
FROM portal_entity_tag
WHERE tag_id = v_dup_id;

-- 3) 删除 dup_id 在 portal_entity_tag 中的残留引用（已迁移或冲突未插入的）
DELETE FROM portal_entity_tag WHERE tag_id = v_dup_id;

-- 4) 删除重复标签本身
DELETE FROM portal_tag WHERE id = v_dup_id;

SELECT CONCAT('已合并标签: id=', v_dup_id, ' -> id=', v_keep_id, ' (', v_keep_name, ')') AS message;
END LOOP;
CLOSE cur;
END$$
DELIMITER ;

CALL proc_merge_duplicate_tags();
DROP PROCEDURE IF EXISTS proc_merge_duplicate_tags;

-- =============================================
-- 步骤 2：清理所有标签 name 的 # 前缀
-- 说明：经过步骤 1 后，已无重名冲突，可直接 UPDATE 去除 # 前缀。
--       TRIM(LEADING '#' FROM name) 仅去除左侧的 #，保留标签内容中的 #（如 'C#'）。
-- =============================================

UPDATE portal_tag
SET name = TRIM(LEADING '#' FROM name)
WHERE name LIKE '#%';

SELECT ROW_COUNT() AS updated_rows, '已清理 portal_tag.name 的 # 前缀' AS message;

-- =============================================
-- 步骤 3：校验结果
-- =============================================

-- 检查是否还有带 # 前缀的标签（应为 0）
SELECT
    (SELECT COUNT(*) FROM portal_tag WHERE name LIKE '#%') AS remaining_hash_tags,
    (SELECT COUNT(*) FROM portal_tag) AS total_tags,
    (SELECT COUNT(*) FROM portal_entity_tag) AS total_bindings;

-- 展示清理后的标签列表（前 30 条）
SELECT id, name, slug, module, reference_count
FROM portal_tag
ORDER BY sort ASC, id ASC
    LIMIT 30;

-- =============================================
-- 步骤 4：完成提示
-- =============================================
SELECT '标签 # 前缀清理完成！数据库现已统一存储纯文本，# 由前端按需拼接展示。' AS message;
