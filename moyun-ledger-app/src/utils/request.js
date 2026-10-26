/**
 * 网络请求封装
 *
 * 约定与 moyun-portal 门户一致：
 * - Token 存储 key：moyun_token
 * - 请求头：Authorization: Bearer <token>
 * - 后端统一返回 AjaxResult：{ code: 200/500, msg, data }
 * - code != 200 视为业务失败；401 跳转登录页
 *
 * 环境地址：
 * - 开发：H5 走本地 8080；小程序在微信开发者工具中勾选"不校验合法域名"
 */
import { useUserStore } from '@/stores/user';

// 环境地址：**统一读 VITE_API_BASE_URL**（H5 与非 H5 一致）
// - .env.development → http://localhost:8080（小程序端需在微信开发者工具勾选"不校验合法域名"）
// - .env.production  → 真实网关域名，**必须 https 且已配置为小程序 request 合法域名**
// fail-closed：缺失即报错，**绝不回落到 localhost** —— 小程序端回落到 localhost 等于请求
// "用户设备自身"，真机必然失败且难以定位（历史上正是这个故障模式）。生产构建另有构建期守卫。
const rawBaseUrl = String(import.meta.env.VITE_API_BASE_URL || '').trim();
if (!rawBaseUrl) {
  throw new Error('[ledger] 缺少 VITE_API_BASE_URL：请在 .env.development / .env.production 配置接口地址');
}
export const BASE_URL = rawBaseUrl;

/** 需要登录态的页面：已在其上时不再重复跳转，避免来回叠加 */
const AUTH_PAGES = [
  'pages/mine/login/index',
  'pages/mine/register/index',
  'pages/mine/forgot-password/index'
];

/**
 * 不强制跳登录的页面：登录/注册/找回密码，以及总览。
 * 总览在未登录时展示产品落地页（价值点 + 三步引导），
 * 过期 token 触发 401 后应回到落地页，而不是把用户直接弹去登录。
 */
const PUBLIC_PAGES = AUTH_PAGES.concat(['pages/dashboard/index']);

/**
 * 401 统一处理：登出 → 提示 → 跳登录页
 *
 * 本文件顶部注释一直写着「401 跳转登录页」，但此前只做了 logout + toast：
 * 用户被登出后仍停在原页面，页面数据已失效却**没有任何去登录的路径**
 * （只有记账页自己弹了个引导去「我的」的弹窗）。
 *
 * 去重的必要性：一个页面常并发发起多个请求（Promise.all），令牌过期时会
 * 同时收到多个 401；逐个处理会连弹多个 toast、连续 navigateTo 多个登录页，
 * 把页面堆栈搞乱。故用 authRedirecting 锁住，跳转完成后释放。
 */
let authRedirecting = false;
const handleUnauthorized = (userStore) => {
  userStore.logout();
  if (authRedirecting) return;
  if (typeof getCurrentPages === 'function') {
    const stack = getCurrentPages();
    const current = stack.length ? stack[stack.length - 1].route : '';
    if (PUBLIC_PAGES.includes(current)) return;
  }
  authRedirecting = true;
  uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
  // 稍等再跳，让 toast 有机会被看到（跳转会立即关掉 toast）
  setTimeout(() => {
    uni.navigateTo({
      url: '/pages/mine/login/index',
      complete: () => { authRedirecting = false; }
    });
  }, 600);
};

const request = (options) => {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore();
    const header = { 'Content-Type': 'application/json' };
    if (userStore.token) {
      header['Authorization'] = 'Bearer ' + userStore.token;
    }
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => {
        if (res.statusCode === 401) {
          handleUnauthorized(userStore);
          const err = new Error('未登录'); err.code = 401;
          reject(err);
          return;
        }
        if (res.statusCode !== 200) {
          uni.showToast({ title: '服务异常(' + res.statusCode + ')', icon: 'none' });
          reject(new Error('HTTP ' + res.statusCode));
          return;
        }
        const body = res.data || {};
        // AjaxResult: { code, msg, data }，code=200 成功
        // 注意：未登录/登录过期时后端以 HTTP 200 + code:401 返回，需在此统一处理
        if (body.code === 401) {
          handleUnauthorized(userStore);
          const err = new Error('未登录'); err.code = 401;
          reject(err);
          return;
        }
        if (body.code !== undefined && body.code !== 200) {
          uni.showToast({ title: body.msg || '操作失败', icon: 'none' });
          reject(new Error(body.msg || '操作失败'));
          return;
        }
        resolve(body.data !== undefined ? body.data : body);
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，请检查服务', icon: 'none' });
        reject(err);
      }
    });
  });
};

export const get = (url, data) => request({ url, method: 'GET', data });
export const post = (url, data) => request({ url, method: 'POST', data });
export const put = (url, data) => request({ url, method: 'PUT', data });
export const del = (url, data) => request({ url, method: 'DELETE', data });
export default request;
