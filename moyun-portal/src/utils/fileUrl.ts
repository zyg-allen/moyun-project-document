/**
 * 文件 URL 工具：将 MinIO 绝对 HTTP URL 转换为同源相对路径。
 *
 * 背景：后端 MinIO access-url 默认 http://127.0.0.1:9001，
 * 而 portal dev server 启用 HTTPS（vite-plugin-basic-ssl）。
 * HTTPS 页面加载 HTTP 图片会被浏览器作为「混合内容」拦截；
 * 且手机端访问 127.0.0.1 会指向设备自身导致不可达。
 *
 * 方案：Vite 代理 /moyun → http://127.0.0.1:9001（见 vite.config.ts），
 * 此函数把 MinIO 绝对 URL 中的 host 部分剥离，保留 /moyun/xxx 相对路径，
 * 这样浏览器以同源 HTTPS 请求图片，由 Vite 代理转发到 MinIO。
 *
 * 生产环境 MinIO access-url 应配置为外网 HTTPS 域名/CDN，无需转换。
 */

/** MinIO bucket 名，需与后端 minio.bucket-name 保持一致 */
const MINIO_BUCKET = 'moyun';

/**
 * 将文件 URL 规范化为可在当前页面同源加载的路径。
 * - data: / blob: / 已是相对路径 / 外部 HTTPS CDN → 原样返回
 * - 包含 /<bucket>/ 的 HTTP(S) 绝对 URL → 剥离 host，返回相对路径
 * - 其他绝对 URL → 原样返回（不强制转换，避免破坏外部资源）
 */
export function normalizeFileUrl(url?: string | null): string {
  if (!url || !url.trim()) return '';

  const trimmed = url.trim();

  // data URL / blob URL 直接返回
  if (trimmed.startsWith('data:') || trimmed.startsWith('blob:')) {
    return trimmed;
  }

  // 已是相对路径（以 / 开头但非 //）→ 原样返回
  if (trimmed.startsWith('/') && !trimmed.startsWith('//')) {
    return trimmed;
  }

  // 协议相对 URL（//host/...）→ 补全后按绝对 URL 处理
  if (trimmed.startsWith('//')) {
    return normalizeFileUrl('https:' + trimmed);
  }

  // 绝对 HTTP(S) URL
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    try {
      const parsed = new URL(trimmed);
      const pathname = parsed.pathname;
      // 仅转换 MinIO bucket 路径（如 /moyun/xxx），避免误伤其他外部资源
      const bucketPrefix = '/' + MINIO_BUCKET + '/';
      if (pathname.startsWith(bucketPrefix)) {
        return pathname;
      }
    } catch {
      // 非法 URL，原样返回
    }
  }

  return trimmed;
}

export default { normalizeFileUrl };
