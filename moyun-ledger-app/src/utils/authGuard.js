/**
 * 页面级登录守卫
 *
 * 约定：只有总览（/pages/dashboard/index）允许未登录浏览（展示产品落地页）；
 * 其他页面进入时若未登录，统一提示并送登录页，取消则回总览。
 */
const TOKEN_KEY = 'moyun_token';

export function isLoggedIn() {
  try { return !!uni.getStorageSync(TOKEN_KEY); } catch (e) { return false; }
}

/**
 * 未登录时弹提示：去登录 / 回首页。
 * @returns {boolean} 已登录返回 true（调用方继续执行）；未登录返回 false（调用方直接 return）
 */
export function ensureLogin() {
  if (isLoggedIn()) return true;
  uni.showModal({
    title: '未登录',
    content: '登录后才能使用该功能',
    confirmText: '去登录',
    cancelText: '回首页',
    success: (r) => {
      if (r.confirm) {
        uni.navigateTo({ url: '/pages/mine/login/index' });
      } else {
        uni.switchTab({ url: '/pages/dashboard/index' });
      }
    }
  });
  return false;
}
