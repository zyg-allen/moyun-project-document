/**
 * 统一解析后端列表返回，避免「接口返回 {records,total}，页面当数组用」导致假空态。
 *
 * 兼容历史结构：数组 / {records} / {rows} / {list} / {items} / {data} / {plans}
 * 新接口请统一返回 { records, total, pageNum, pageSize }。
 */
const LIST_KEYS = ['records', 'rows', 'list', 'items', 'data', 'plans', 'tasks'];

export function listOf(res) {
  if (Array.isArray(res)) return res;
  if (!res || typeof res !== 'object') return [];
  for (const key of LIST_KEYS) {
    if (Array.isArray(res[key])) return res[key];
  }
  return [];
}

export function totalOf(res, fallback = 0) {
  if (!res || typeof res !== 'object') return fallback;
  const n = Number(res.total);
  if (Number.isFinite(n)) return n;
  return listOf(res).length;
}

export function pageOf(res, pageNum = 1, pageSize = 20) {
  return {
    pageNum: Number(res && res.pageNum) || pageNum,
    pageSize: Number(res && res.pageSize) || pageSize,
    total: totalOf(res)
  };
}
