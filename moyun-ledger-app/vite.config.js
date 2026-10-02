import { defineConfig, loadEnv } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';

/**
 * 接口地址构建期守卫（与 moyun-portal 的 site-url 插件同思路）。
 *
 * 背景：`.env.production` 曾长期是占位域名 `https://api.example.com`，而 `request.js` 又有
 * `|| 'http://localhost:8080'` 的**静默回落** —— 生产包要么指向不存在的域名，要么在缺配置时
 * 请求"用户设备自身"（小程序真机必然失败），两种都极难排查。
 *
 * 本守卫让"配置漏填"在**构建期**就失败：生产构建要求 VITE_API_BASE_URL 非空、https、且非占位。
 */
/** 保留域名后缀（RFC 2606/6761）与本地地址：一律视为不可上线 */
const RESERVED_TLDS = ['.example', '.invalid', '.test', '.localhost', '.local'];
const LOCAL_HOSTS = ['localhost', '127.0.0.1', '0.0.0.0', '::1', '[::1]'];

/** 返回"为什么不可用"的原因；可用则返回 null。按**主机名**判断，避免子串误判/漏判。 */
function detectUnusableHost(url) {
  let host;
  try {
    host = new URL(url).hostname.toLowerCase();
  } catch {
    return '不是合法的绝对 URL';
  }
  if (LOCAL_HOSTS.includes(host)) {
    return '指向本机（localhost/127.0.0.1）';
  }
  if (host === 'example.com' || host.endsWith('.example.com')) {
    return '是示例域名 example.com';
  }
  const reserved = RESERVED_TLDS.find(t => host === t.slice(1) || host.endsWith(t));
  if (reserved) {
    return '使用保留域名后缀 ' + reserved;
  }
  return null;
}

function apiBaseUrlGuard() {
  return {
    name: 'moyun:api-base-url-guard',
    configResolved(config) {
      const isProductionBuild = config.command === 'build' && config.mode === 'production';
      if (!isProductionBuild) return;

      const env = loadEnv(config.mode, process.cwd(), 'VITE_');
      const url = String(env.VITE_API_BASE_URL || '').trim();

      if (!url) {
        throw new Error(
          '\n[api-base-url] 生产构建缺少 VITE_API_BASE_URL。\n' +
          '  请在 moyun-ledger-app/.env.production 填写真实接口地址（必须 https，且已在小程序后台\n' +
          '  配置为 request 合法域名），例如：\n' +
          '    VITE_API_BASE_URL=https://你的域名\n');
      }
      if (!/^https:\/\//i.test(url)) {
        throw new Error(
          '\n[api-base-url] 生产构建的 VITE_API_BASE_URL 必须是 https（当前：' + url + '）。\n' +
          '  小程序要求 request 域名为 https 且已备案；http 在真机与小程序端均不可用。\n');
      }
      const reason = detectUnusableHost(url);
      if (reason) {
        throw new Error(
          '\n[api-base-url] 生产构建的 VITE_API_BASE_URL ' + reason + '：' + url + '\n' +
          '  占位/本地域名上线会导致**所有请求失败**；请改为真实网关域名。\n');
      }
    }
  };
}

export default defineConfig({
  plugins: [apiBaseUrlGuard(), uni()]
});
