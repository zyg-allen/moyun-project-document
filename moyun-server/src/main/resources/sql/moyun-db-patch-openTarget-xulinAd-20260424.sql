-- =========================================================================================
-- 增量补丁：广告表增加 open_target 字段 + 字典新增"旭林广告位"和"首页VIP推广"
-- 修复问题 #5：首页加入旭林广告位，后台可编辑（图片上传 / 外链 / 新窗口打开）
-- 部署日期：2026-04-24
-- 执行方式：在已安装的 moyun-db 数据库中运行本 SQL
-- =========================================================================================
USE `moyun-db`;

/* ============================================================
 * 1. portal_ad_slot 表：新增 open_target 字段（打开方式）
 * ============================================================ */
ALTER TABLE `portal_ad_slot`
    ADD COLUMN `open_target` varchar(10) DEFAULT '_blank' COMMENT '链接打开方式：_blank=新窗口（默认），_self=当前页'
        AFTER `link`;

/* ============================================================
 * 2. sys_dict_data：字典 portal_ad_slot_key（广告位标识）
 *    新增：首页旭林广告位 home_xulin_ad
 *    新增：首页VIP推广位 home_vip_banner
 * ============================================================ */
INSERT INTO `sys_dict_data` (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag)
VALUES (500, 1, '首页-旭林广告位', 'home_xulin_ad', 'portal_ad_slot_key', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '首页-热门推荐上方的旭林广告位', '0')
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label), remark = VALUES(remark);

INSERT INTO `sys_dict_data` (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, del_flag)
VALUES (501, 2, '首页-VIP推广位', 'home_vip_banner', 'portal_ad_slot_key', '', 'primary', 'N', '0', 'admin', NOW(), '', NULL, '首页右侧/移动端下方的VIP推广位', '0')
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label), remark = VALUES(remark);

/* ============================================================
 * 3. portal_ad_slot：预置两条广告数据（可在后台编辑）
 *    slotKey = home_xulin_ad   ：首页旭林广告位
 *    slotKey = home_vip_banner ：首页VIP推广位（替换原来写死的内容）
 * ============================================================ */
INSERT INTO `portal_ad_slot` (slot_key, title, image, link, open_target, content, sort, status, create_by, create_time, update_by, update_time, remark, del_flag)
VALUES (
    'home_xulin_ad',
    '旭林广告位-占位',
    NULL,
    NULL,
    '_blank',
    '请在后台门户管理→广告位管理：设置图片、外链；或删除本条后重新发布',
    0, '1', 'admin', NOW(), '', NULL, '首页-热门推荐上方 旭林广告位 默认占位（停用时不展示）', '0'
)
ON DUPLICATE KEY UPDATE
    update_time   = NOW(),
    update_by     = 'admin',
    open_target   = IF(open_target IS NULL, '_blank', open_target);

INSERT INTO `portal_ad_slot` (slot_key, title, image, link, open_target, content, sort, status, create_by, create_time, update_by, update_time, remark, del_flag)
VALUES (
    'home_vip_banner',
    '加入旭林知行会员',
    'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=VIP%20membership%20premium%20card%20design%20with%20gold%20and%20purple%20gradient%20luxury%20style&image_size=square',
    '/vip',
    '_self',
    '解锁VIP专属内容、AI面试官、成长1.5倍加成等特权',
    0, '0', 'admin', NOW(), '', NULL, '首页VIP推广位，默认展示（启用）', '0'
)
ON DUPLICATE KEY UPDATE
    update_time   = NOW(),
    update_by     = 'admin',
    open_target   = IF(open_target IS NULL, '_self', open_target),
    title         = IF(title IS NULL OR title = '', VALUES(title), title),
    content       = IF(content IS NULL OR content = '', VALUES(content), content),
    link          = IF(link IS NULL OR link = '', VALUES(link), link),
    image         = IF(image IS NULL OR image = '', VALUES(image), image);
