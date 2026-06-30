/**
 * 登录态弹框工具
 *
 * 独立于 Vue 组件上下文，可在 fetch 拦截器 / Vue composable 中直接调用。
 * 提供两种弹框：
 * - showAuthExpiredDialog：强制框（HTTP 401 链路拦截），"留在当前页"会 reload
 * - showLoginConfirmDialog：温和框（业务 code 401 / 前端预检），"继续浏览"仅关闭弹框
 *
 * showAuthExpiredDialog 行为：
 * - "重新登录"：清除本地登录状态，跳转登录页（携带 redirect）
 * - "留在当前页"：清除本地登录状态，刷新当前页（同步 Pinia store）
 * - 单例守卫：同时多个请求 401 时只弹一个框
 */
/**
 * 清除本地登录状态（token + 用户信息）
 */
export declare function clearAuthState(): void;
/**
 * 显示"登录已过期"弹框
 *
 * 当后端返回 401（HTTP 状态码或业务 code）时调用。
 * 用户必须选择"重新登录"或"留在当前页"，不能点击遮罩关闭。
 */
export declare function showAuthExpiredDialog(): void;
/**
 * 显示"需要登录"温和确认框
 *
 * @param actionName 操作名称（如 "点赞"、"收藏"、"继续操作"），用于提示文案
 * @returns true=用户选择去登录，false=用户选择继续浏览 / 已有弹框占用
 */
export declare function showLoginConfirmDialog(actionName?: string): Promise<boolean>;
/**
 * 处理"未登录 / 登录过期"（拦截器专用）
 *
 * 当后端返回业务 code 401 时调用：
 * 1. 弹出温和确认框（去登录 / 继续浏览）
 * 2. 无论用户选什么，都清除本地登录态（避免下次操作再次触发 401 死循环）
 * 3. 用户选"去登录"则跳转登录页（携带 redirect）
 * 4. 用户选"继续浏览"则仅清状态，保持当前页（后续操作走前端预检 withAuthConfirm）
 *
 * @param msg 后端返回的提示文案（可选）
 */
export declare function handleUnauthorized(msg?: string): Promise<void>;
