/**
 * 仓库管理开关。
 *
 * 开启后底部导航展示「入库/出库」入口，关闭则退回纯记账。
 * 真值存在后端 portal_user.warehouse_enabled，本地缓存一份用于首屏即时判断；
 * 网络失败时以本地缓存为准（离线也能用）。
 */

const KEY = 'moyun_warehouse_enabled';

/** 底部导航里「入库/出库」的下标（总览0 资产负债1 记一笔2 入库出库3 分析4 我的5） */
const TAB_INDEX = 3;
const TAB_TEXT = '入库/出库';

/** 本地缓存读取（同步，供首屏渲染） */
export function isWarehouseEnabled() {
  const v = uni.getStorageSync(KEY);
  if (v === '' || v === null || v === undefined) return true;
  return !!v;
}

/**
 * 从后端拉一次并覆盖本地缓存。换设备/重装后以服务端为准。
 */
export async function syncWarehouseSwitch() {
  try {
    const { getUserInfo } = require('@/api/ledger');
    const u = await getUserInfo();
    if (u && u.warehouseEnabled !== undefined && u.warehouseEnabled !== null) {
      uni.setStorageSync(KEY, !!u.warehouseEnabled);
      applyWarehouseTabBar();
    }
  } catch (e) {
    /* 未登录/离线：保留本地缓存 */
  }
}

/**
 * 设置开关：本地立即生效，同时写后端。
 * 后端失败不回滚本地（用户意图优先，下次同步会纠偏）。
 */
export function setWarehouseEnabled(on) {
  uni.setStorageSync(KEY, !!on);
  applyWarehouseTabBar();
  try {
    const { updateProfile } = require('@/api/ledger');
    updateProfile({ warehouseEnabled: !!on }).catch(() => { /* 忽略 */ });
  } catch (e) { /* 忽略 */ }
}

/**
 * 把开关状态同步到底部导航。
 *
 * uni-app 无法动态增删 tab，只能在关闭时把文字清空（隐藏入口）；
 * 未开启时用户仍可能点到那块空白，页面自身会再拦一次。
 */
export function applyWarehouseTabBar() {
  const on = isWarehouseEnabled();
  try {
    uni.setTabBarItem({ index: TAB_INDEX, text: on ? TAB_TEXT : '' });
  } catch (e) {
    /* 非 tab 页调用会失败，忽略即可 */
  }
}
