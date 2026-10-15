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
      // 允许两种形态：① 同源相对路径 /api（推荐，前端与后端同域，由 Nginx 反代，无跨源）；
      //              ② 独立网关的 https 绝对地址（小程序 request 域名要求 https 且已备案）。
      const isRelativeApi = /^\/[A-Za-z0-9_\-./]*$/.test(url);
      if (isRelativeApi) return;   // 相对路径：由部署侧（Nginx）保证可达，无需域名校验
      if (!/^https:\/\//i.test(url)) {
        throw new Error(
          '\n[api-base-url] 生产构建的 VITE_API_BASE_URL 必须是 https 绝对地址，或以 / 开头的同源相对路径（当前：' + url + '）。\n' +
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
  plugins: [apiBaseUrlGuard(), uni()],
  server: {
    // 监听所有网卡：natapp / 局域网设备（手机真机）都要能连到本机 dev server
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    proxy: {
      // ── 同源代理（解决内网穿透下的 CORS / Private Network Access 拦截）──
      // 背景：用 natapp 等内网穿透把 H5（5173）暴露到公网域名后，若前端仍直连
      //       http://localhost:8080，浏览器会以
      //       「not a secure context and the resource is in more-private address space `loopback`」
      //       直接拒绝（Chrome Private Network Access）——**加 CORS 响应头也无法绕过**，
      //       因为公网页面不允许访问 loopback 地址。
      // 做法：前端统一请求同源的 /api（见 .env.natapp），由 dev server 在服务端转发到 8080，
      //       浏览器视角全程同源 ⇒ 既无 CORS，也无 PNA 限制。
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        // 去掉 /api 前缀后转发：/api/captchaImage → http://127.0.0.1:8080/captchaImage
        rewrite: (p) => p.replace(/^\/api/, ''),
        // 关键：剥掉 Origin / Referer 再转发。
        // 浏览器→代理这一段本来就是同源，Origin 只在浏览器侧有意义；
        // 若原样转发，后端 CORS（moyun.cors 精确白名单 + fail-closed，dev 仅放行私网 IP 字面量）
        // 会因识别到公网域名 Origin 而直接返回 403 Forbidden。
        // 无 Origin 的请求在 CORS 过滤器看来不属于跨源请求 → 正常放行。
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.removeHeader('origin');
            proxyReq.removeHeader('referer');
          });
        },
      },
    },
  },
});
