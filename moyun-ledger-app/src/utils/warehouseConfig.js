/**
 * 仓库常用配置（本机存储）。
 *
 * 这些不是装饰开关，而是真正作用到列表与表单：
 *   lowStock      低库存阈值 → 商品列表里低于该值时库存标红
 *   showValue     是否在商品列表显示库存金额
 *   defaultReason 出入库默认原因 → 入库/出库表单预选
 *   pageSize      商品列表每页条数
 *   showTags      列表是否显示标签
 *   showExpiry    列表是否显示到期日期
 */

const KEY = 'moyun_warehouse_cfg';

const DEFAULTS = {
  lowStock: 5,
  showValue: true,
  defaultReason: 'purchase',
  pageSize: 20,
  showTags: true,
  showExpiry: true
};

export function getWarehouseCfg() {
  try {
    const raw = uni.getStorageSync(KEY);
    if (raw && typeof raw === 'object') return Object.assign({}, DEFAULTS, raw);
  } catch (e) { /* 忽略 */ }
  return Object.assign({}, DEFAULTS);
}

export function saveWarehouseCfg(patch) {
  const next = Object.assign(getWarehouseCfg(), patch || {});
  try { uni.setStorageSync(KEY, next); } catch (e) { /* 忽略 */ }
  return next;
}

export const WAREHOUSE_CFG_DEFAULTS = DEFAULTS;
