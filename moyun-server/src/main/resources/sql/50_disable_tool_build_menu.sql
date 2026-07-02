-- =============================================
-- 停用"表单构建"菜单（功能未实现）
-- 现象：访问 /tool/tool/build 或 /tool//tool/build 报错
--       [Vue Router warn]: Record with path "/tool//tool/build"
--       is either missing a "component(s)" or "children" property
-- 原因：
--   1. 组件文件 views/tool/build/index.vue 不存在（功能未实现）
--      前端 router/index.js 中相关路由已注释："暂时注释：表单构建器功能未实现"
--   2. path 配置异常（/tool/build 或 tool/build），拼接父 path 产生双斜杠
-- 修复：把菜单 status 改为 '1'（停用）
--   - 后端 selectMenuTreeByUserId 查询条件为 status = 0
--   - status='1' 的菜单不会被查询、不会注册路由、不显示在侧边栏
--   - 警告消失，不影响其他菜单
-- 可逆：以后实现功能时，把 status 改回 '0' 即可恢复
-- 幂等：仅当 status != '1' 时才修改
-- 作者：moyun  日期：2026-07-01
-- =============================================

UPDATE `sys_menu`
SET `status` = '1',
    `path` = 'build',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_name` = '表单构建'
  AND `status` != '1';

-- 验证（执行后可手动运行核对，应看到 status = '1'）
-- SELECT menu_id, menu_name, parent_id, path, component, status, visible, perms
-- FROM sys_menu
-- WHERE menu_name = '表单构建';
