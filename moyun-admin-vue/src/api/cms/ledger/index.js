import request from '@/utils/request'

/**
 * 记账模块后台接口（管理端专用）
 *
 * 约定：
 * 1. 所有写接口幂等：重复提交由后端唯一约束/存在性判断兜底，前端按钮另加 loading 防抖；
 * 2. 所有列表返回 RuoYi 标准 { code, rows, total }；
 * 3. 用户个体数据（流水等）默认不可见，须用户授权（授权状态由 /cms/ledger/auth 提供）。
 */

// ---------------- 预设分类 ----------------
export function listLedgerCategory(query) {
  return request({ url: '/cms/ledger/category/list', method: 'get', params: query })
}
export function addLedgerCategory(data) {
  return request({ url: '/cms/ledger/category', method: 'post', data })
}
export function updateLedgerCategory(id, data) {
  return request({ url: '/cms/ledger/category/' + id, method: 'put', data })
}
export function changeLedgerCategoryStatus(id, status) {
  return request({ url: '/cms/ledger/category/' + id + '/status/' + status, method: 'put' })
}
export function delLedgerCategory(id) {
  return request({ url: '/cms/ledger/category/' + id, method: 'delete' })
}

// ---------------- 商品类目（两级树） ----------------
export function listProductCategory(query) {
  return request({ url: '/cms/ledger/product-category/list', method: 'get', params: query })
}
export function addProductCategory(data) {
  return request({ url: '/cms/ledger/product-category', method: 'post', data })
}
export function updateProductCategory(id, data) {
  return request({ url: '/cms/ledger/product-category/' + id, method: 'put', data })
}
export function changeProductCategoryStatus(id, status) {
  return request({ url: '/cms/ledger/product-category/' + id + '/status/' + status, method: 'put' })
}
export function delProductCategory(id) {
  return request({ url: '/cms/ledger/product-category/' + id, method: 'delete' })
}

// ---------------- 渠道（kind = sale 售卖 / source 进货） ----------------
export function listLedgerChannel(query) {
  return request({ url: '/cms/ledger/channel/list', method: 'get', params: query })
}
export function addLedgerChannel(data) {
  return request({ url: '/cms/ledger/channel', method: 'post', data })
}
export function updateLedgerChannel(kind, id, data) {
  return request({ url: '/cms/ledger/channel/' + kind + '/' + id, method: 'put', data })
}
export function changeLedgerChannelStatus(kind, id, status) {
  return request({ url: '/cms/ledger/channel/' + kind + '/' + id + '/status/' + status, method: 'put' })
}
export function delLedgerChannel(kind, id) {
  return request({ url: '/cms/ledger/channel/' + kind + '/' + id, method: 'delete' })
}

// ---------------- 小程序功能入口 ----------------
export function listAppFeatures() {
  return request({ url: '/cms/ledger/app-feature/list', method: 'get' })
}
export function addAppFeature(data) {
  return request({ url: '/cms/ledger/app-feature', method: 'post', data })
}
export function updateAppFeature(id, data) {
  return request({ url: '/cms/ledger/app-feature/' + id, method: 'put', data })
}
export function delAppFeature(id) {
  return request({ url: '/cms/ledger/app-feature/' + id, method: 'delete' })
}
/** App 内置路由白名单（只读）：后台只能从白名单里选，避免配置出死链 */
export function getAppFeatureRoutes() {
  return request({ url: '/cms/ledger/app-feature/routes', method: 'get' })
}

// ---------------- 运营统计 ----------------
export function getLedgerStats() {
  return request({ url: '/cms/ledger/stats/overview', method: 'get' })
}

// ---------------- 用户数据（需授权） ----------------
export function listLedgerUsers(query) {
  return request({ url: '/cms/ledger/users/list', method: 'get', params: query })
}
export function listUserTransactions(userId, query) {
  return request({ url: '/cms/ledger/users/' + userId + '/transactions', method: 'get', params: query })
}

// ---------------- 授权与审计 ----------------
/** 授权记录（用户主动授权的范围/期限/状态） */
export function listDataAuth(query) {
  return request({ url: '/cms/ledger/auth/list', method: 'get', params: query })
}
/** 敏感数据访问日志 */
export function listAccessLog(query) {
  return request({ url: '/cms/ledger/access-log/list', method: 'get', params: query })
}

// ---------------- 记账字典总览 ----------------
/** 记账下拉字典一览（维护跳转系统字典；值锁定规则由后端校验） */
export function getLedgerDictOverview() {
  return request({ url: '/cms/ledger/dict/overview', method: 'get' })
}
