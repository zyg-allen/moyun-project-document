/**
 * 记账后端压测脚本（零依赖）
 * 用法：node loadtest.js <token> <concurrency> <durationSec>
 */
const http = require('http');
const fs = require('fs');

const token = process.argv[2] || fs.readFileSync(process.env.TEMP + '/perf_token.txt', 'utf8').trim();
const CONC = Number(process.argv[3] || 20);
const DURATION = Number(process.argv[4] || 20) * 1000;

const ONLY = process.argv[5] || '';
const ENDPOINTS = [
  { name: 'dashboard', path: '/portal/ledger/dashboard', auth: true },
  { name: 'txn-page', path: '/portal/ledger/transactions?pageNum=1&pageSize=10', auth: true },
  { name: 'report', path: '/portal/ledger/reports/overview?year=2026', auth: true },
  { name: 'dict', path: '/portal/ledger/dict/txn_type', auth: false }
];

const ACTIVE = ONLY ? ENDPOINTS.filter((e) => e.name === ONLY) : ENDPOINTS;
const agent = new http.Agent({ keepAlive: true, maxSockets: CONC });

function once(ep) {
  return new Promise((resolve) => {
    const started = process.hrtime.bigint();
    const headers = ep.auth ? { Authorization: 'Bearer ' + token } : {};
    const req = http.request({ host: '127.0.0.1', port: 8080, path: ep.path, method: 'GET', headers, agent }, (res) => {
      let bytes = 0;
      res.on('data', (c) => { bytes += c.length; });
      res.on('end', () => {
        const ms = Number(process.hrtime.bigint() - started) / 1e6;
        resolve({ ok: res.statusCode >= 200 && res.statusCode < 300, status: res.statusCode, ms, bytes, ep: ep.name });
      });
    });
    req.on('error', (e) => {
      const ms = Number(process.hrtime.bigint() - started) / 1e6;
      resolve({ ok: false, status: 0, ms, bytes: 0, ep: ep.name, err: e.code || e.message });
    });
    req.setTimeout(10000, () => req.destroy(new Error('timeout')));
    req.end();
  });
}

function pct(arr, p) {
  if (!arr.length) return 0;
  const a = arr.slice().sort((x, y) => x - y);
  const i = Math.min(a.length - 1, Math.max(0, Math.ceil((p / 100) * a.length) - 1));
  return a[i];
}

(async () => {
  const stats = {};
  ACTIVE.forEach((e) => { stats[e.name] = { lat: [], ok: 0, fail: 0, bytes: 0, statuses: {} }; });
  const deadline = Date.now() + DURATION;
  let sent = 0;

  async function worker() {
    while (Date.now() < deadline) {
      const ep = ACTIVE[sent % ACTIVE.length];
      sent++;
      const r = await once(ep);
      const s = stats[r.ep];
      s.lat.push(r.ms);
      s.bytes += r.bytes;
      if (r.ok) s.ok++; else { s.fail++; s.statuses[r.status || r.err] = (s.statuses[r.status || r.err] || 0) + 1; }
    }
  }

  const t0 = Date.now();
  await Promise.all(Array.from({ length: CONC }, worker));
  const secs = (Date.now() - t0) / 1000;
  const out = { concurrency: CONC, durationSec: Number(secs.toFixed(1)), total: sent, rps: Number((sent / secs).toFixed(1)), endpoints: {} };
  for (const [name, s] of Object.entries(stats)) {
    if (!s.lat.length) continue;
    out.endpoints[name] = {
      count: s.lat.length,
      rps: Number((s.lat.length / secs).toFixed(1)),
      ok: s.ok,
      fail: s.fail,
      failStatus: s.statuses,
      p50: Number(pct(s.lat, 50).toFixed(1)),
      p95: Number(pct(s.lat, 95).toFixed(1)),
      p99: Number(pct(s.lat, 99).toFixed(1)),
      avgKB: Number((s.bytes / s.lat.length / 1024).toFixed(1))
    };
  }
  console.log(JSON.stringify(out, null, 2));
  agent.destroy();
})();
