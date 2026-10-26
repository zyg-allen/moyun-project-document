/**
 * Web 质量检查（零依赖）：响应头 / 压缩 / 缓存 / 安全头 / TTFB / 页面体积
 * 用法：node webcheck.js
 */
const http = require('http');

const TARGETS = [
  { name: 'H5 应用', url: 'http://127.0.0.1:5173/', expectHtml: true },
  { name: '管理后台', url: 'http://localhost/', expectHtml: true },
  { name: '后端 API', url: 'http://127.0.0.1:8080/portal/ledger/dict/txn_type', expectHtml: false }
];

function fetchOnce(url, acceptEncoding) {
  return new Promise((resolve) => {
    const started = process.hrtime.bigint();
    let ttfb = 0;
    const req = http.get(url, { headers: { 'Accept-Encoding': acceptEncoding, 'User-Agent': 'moyun-webcheck/1.0' } }, (res) => {
      let bytes = 0;
      let body = '';
      res.on('data', (chunk) => {
        if (!ttfb) ttfb = Number(process.hrtime.bigint() - started) / 1e6;
        bytes += chunk.length;
        if (body.length < 400000) body += chunk.toString('utf8');
      });
      res.on('end', () => {
        resolve({
          status: res.statusCode,
          headers: res.headers,
          bytes,
          ttfb,
          total: Number(process.hrtime.bigint() - started) / 1e6,
          body
        });
      });
    });
    req.on('error', (e) => resolve({ error: e.code || e.message }));
    req.setTimeout(15000, () => req.destroy(new Error('timeout')));
  });
}

function summarizeHtml(html) {
  const scripts = (html.match(/<script[^>]+src=/g) || []).length;
  const links = (html.match(/<link[^>]+href=/g) || []).length;
  const inline = (html.match(/<script(?![^>]*src=)[^>]*>/g) || []).length;
  return { scripts, links, inline };
}

(async () => {
  for (const t of TARGETS) {
    const plain = await fetchOnce(t.url, 'identity');
    const gz = await fetchOnce(t.url, 'gzip, br');
    const out = {
      target: t.name,
      url: t.url,
      status: plain.status,
      bytesPlain: plain.bytes,
      bytesGzip: gz.bytes,
      compressed: gz.bytes < plain.bytes * 0.95,
      ttfbMs: Number((gz.ttfb || 0).toFixed(1)),
      totalMs: Number((gz.total || 0).toFixed(1)),
      headers: {
        'content-encoding': gz.headers && gz.headers['content-encoding'],
        'cache-control': gz.headers && gz.headers['cache-control'],
        'content-type': gz.headers && gz.headers['content-type'],
        'x-frame-options': gz.headers && gz.headers['x-frame-options'],
        'x-content-type-options': gz.headers && gz.headers['x-content-type-options'],
        'content-security-policy': gz.headers && gz.headers['content-security-policy'],
        'strict-transport-security': gz.headers && gz.headers['strict-transport-security'],
        server: gz.headers && gz.headers.server
      }
    };
    if (t.expectHtml && plain.body) out.html = summarizeHtml(plain.body);
    console.log(JSON.stringify(out, null, 2));
  }
})();
