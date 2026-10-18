/**
 * 仓库管理开关。
 *
 * 开启后底部导航展示「入库/出库」入口，关闭则退回纯记账。
 * 当前存本地（UI 阶段），后续可切到后端用户配置。
 */

const KEY = 'moyun_warehouse_enabled';

/** 底部导航里「入库/出库」的下标（总览0 资产负债1 记一笔2 入库出库3 分析4 我的5） */
const TAB_INDEX = 3;
const TAB_TEXT = '入库/出库';

/** 默认开启（用户已在用仓库功能，不做静默关闭） */
export function isWarehouseEnabled() {
  const v = uni.getStorageSync(KEY);
  if (v === '' || v === null || v === undefined) return true;
  return !!v;
}

export function setWarehouseEnabled(on) {
  uni.setStorageSync(KEY, !!on);
  applyWarehouseTabBar();
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
