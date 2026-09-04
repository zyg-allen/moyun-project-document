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

// #ifdef H5
export const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
// #endif
// #ifndef H5
export const BASE_URL = 'http://localhost:8080';
// #endif

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
          userStore.logout();
          uni.showToast({ title: '请先登录', icon: 'none' });
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
          userStore.logout();
          uni.showToast({ title: '请先登录', icon: 'none' });
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
