/**
 * 仓库常用配置。
 *
 * 存储策略：**服务端为准**（ledger_warehouse_config），本机只做缓存：
 *   - getWarehouseCfg()  同步读本机缓存（首屏/离线用）
 *   - loadWarehouseCfg() 异步拉服务端并覆盖缓存（登录后调用）
 *   - saveWarehouseCfg() 先写本机再异步推服务端（失败不影响本地可用）
 *
 * 配置项都真正作用到列表与表单：
 *   lowStock      低库存阈值 → 商品列表库存低于该值标红
 *   showValue     列表是否显示库存金额
 *   defaultReason 默认出库类型（卖出/自己用/损耗）
 *   pageSize      商品列表每页条数
 *   showTags      列表是否显示标签
 *   showExpiry    列表是否显示到期日期
 */

import { getWarehouseConfig, saveWarehouseConfig } from '@/api/ledger';

const KEY = 'moyun_warehouse_cfg';

const DEFAULTS = {
  lowStock: 5,
  showValue: true,
  defaultReason: 'purchase',
  pageSize: 20,
  showTags: true,
  showExpiry: true
};

/** 服务端存的是字符串，转成正确类型 */
function coerce(raw) {
  const out = {};
  Object.keys(raw || {}).forEach((k) => {
    const v = raw[k];
    if (v === 'true' || v === 'false') {
      out[k] = v === 'true';
    } else if (v !== '' && v !== null && v !== undefined && !isNaN(Number(v))) {
      out[k] = Number(v);
    } else {
      out[k] = v;
    }
  });
  return out;
}

export function getWarehouseCfg() {
  try {
    const raw = uni.getStorageSync(KEY);
    if (raw && typeof raw === 'object') return Object.assign({}, DEFAULTS, raw);
  } catch (e) { /* 忽略 */ }
  return Object.assign({}, DEFAULTS);
}

/** 从服务端拉取并覆盖本机缓存（登录后调用） */
export async function loadWarehouseCfg() {
  try {
    const data = await getWarehouseConfig();
    if (data && typeof data === 'object' && Object.keys(data).length) {
      const merged = Object.assign({}, DEFAULTS, coerce(data));
      try { uni.setStorageSync(KEY, merged); } catch (e) { /* 忽略 */ }
      return merged;
    }
  } catch (e) { /* 未登录/网络异常：用本机缓存 */ }
  return getWarehouseCfg();
}

/** 先写本机（立即生效），再异步推服务端 */
export function saveWarehouseCfg(patch) {
  const next = Object.assign(getWarehouseCfg(), patch || {});
  try { uni.setStorageSync(KEY, next); } catch (e) { /* 忽略 */ }
  saveWarehouseConfig(next).catch(() => { /* 离线时不阻塞 */ });
  return next;
}

export const WAREHOUSE_CFG_DEFAULTS = DEFAULTS;