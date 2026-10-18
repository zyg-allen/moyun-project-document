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

/** tabBar 页面路由：只有这些页面上才能调 setTabBarItem */
const TAB_PAGES = [
  'pages/dashboard/index',
  'pages/record/index',
  'pages/portfolio/index',
  'pages/warehouse/inout/index',
  'pages/mine/index'
];

const TAB_TEXT = '入库/出库';

/** 是否开启仓库管理（本地缓存，默认开启） */
export function isWarehouseEnabled() {
  try {
    const v = uni.getStorageSync(KEY);
    return v === '' || v === null || v === undefined ? true : !!v;
  } catch (e) {
    return true;
  }
}

/** 从服务端同步开关并刷新 tabBar */
export async function syncWarehouseSwitch() {
  try {
    // 动态 import：避免模块循环依赖，同时 Vite/ESM 下不能用 require
    const { getUserInfo } = await import('@/api/ledger');
    const u = await getUserInfo();
    if (u && u.warehouseEnabled !== undefined && u.warehouseEnabled !== null) {
      uni.setStorageSync(KEY, !!u.warehouseEnabled);
      applyWarehouseTabBar();
    }
  } catch (e) {
    /* 未登录/离线：保留本地缓存 */
  }
}

/** 设置开关：本地立即生效，同时写后端（后端失败不回滚本地） */
export function setWarehouseEnabled(on) {
  uni.setStorageSync(KEY, !!on);
  applyWarehouseTabBar();
  import('@/api/ledger')
    .then(({ updateProfile }) => updateProfile({ warehouseEnabled: !!on }))
    .catch(() => { /* 离线/未登录：忽略，本地已生效 */ });
}

/**
 * 刷新「入库/出库」tab 文案。
 *
 * 两个坑：
 *   ① 非 tab 页调用会失败（setTabBarItem:fail not TabBar page）→ 先判断当前路由
 *   ② uni.setTabBarItem 返回 Promise，try/catch 抓不到异步拒绝 → 必须挂 catch
 */
export function applyWarehouseTabBar() {
  const on = isWarehouseEnabled();
  try {
    const stack = typeof getCurrentPages === 'function' ? getCurrentPages() : [];
    const cur = stack.length ? (stack[stack.length - 1].route || '') : '';
    if (!cur || TAB_PAGES.indexOf(cur) < 0) return;
    const res = uni.setTabBarItem({ index: TAB_INDEX, text: on ? TAB_TEXT : '' });
    if (res && typeof res.catch === 'function') res.catch(() => { /* 忽略 */ });
  } catch (e) {
    /* 忽略 */
  }
}