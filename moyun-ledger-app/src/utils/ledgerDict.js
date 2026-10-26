/**
 * 记账字典加载（后台可配，App 端只读）
 *
 * 约定：
 * 1. 统一走 /portal/ledger/dict 批量拉取，5 分钟本地缓存，避免每页一次请求；
 * 2. 拉取失败/字典为空时**回退到内置默认**，保证离线或旧后端下下拉不为空；
 * 3. 只做"值 → 标签"的展示与候选，不改变业务取值语义（取值由后端锁定）。
 */
import { get } from '@/utils/request';

const CACHE = {};      // type -> [{ value, label, sort }]
const CACHE_TS = {};   // type -> 时间戳
const TTL = 5 * 60 * 1000;

/** 批量加载字典；返回 { type: items[] }（无数据时为空数组） */
export async function loadDicts(types) {
  const list = (types || []).filter(Boolean);
  const need = list.filter(t => !CACHE[t] || Date.now() - CACHE_TS[t] > TTL);
  if (need.length) {
    try {
      const data = await get('/portal/ledger/dict', { types: need.join(',') }) || {};
      need.forEach(t => {
        if (Array.isArray(data[t]) && data[t].length) {
          CACHE[t] = data[t];
          CACHE_TS[t] = Date.now();
        }
      });
    } catch (e) { /* 网络异常：保留已有缓存，调用方用兜底 */ }
  }
  const out = {};
  list.forEach(t => { out[t] = CACHE[t] || []; });
  return out;
}

/**
 * 转成页面既有格式 [key, label] 数组（兼容仓库/出入库页的二维数组写法）
 * 字典为空时返回 fallback。
 */
export function asPairs(items, fallback) {
  if (items && items.length) {
    return items.map(i => [i.value, i.label]);
  }
  return fallback || [];
}

/** 转成 [{key,label}]（兼容记账类型/反馈类型等对象数组写法） */
export function asOptions(items, fallback) {
  if (items && items.length) {
    return items.map(i => ({ key: i.value, label: i.label }));
  }
  return fallback || [];
}

/** 值 → 标签映射（字典为空时用兜底 map） */
export function asLabelMap(items, fallback) {
  if (items && items.length) {
    const map = {};
    items.forEach(i => { map[i.value] = i.label; });
    return map;
  }
  return fallback || {};
}

/** 仅供测试/调试：清空缓存 */
export function clearDictCache() {
  Object.keys(CACHE).forEach(k => { delete CACHE[k]; delete CACHE_TS[k]; });
}

/** 公开字典（cms_ 前缀，走 /portal/dict）：如反馈类型 */
export async function loadPublicDict(type) {
  if (!type) return [];
  if (CACHE[type] && Date.now() - CACHE_TS[type] <= TTL) return CACHE[type];
  try {
    const data = await get('/portal/dict/' + type) || [];
    if (Array.isArray(data) && data.length) {
      CACHE[type] = data;
      CACHE_TS[type] = Date.now();
    }
  } catch (e) { /* 失败用兜底 */ }
  return CACHE[type] || [];
}
