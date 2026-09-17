import request from '@/utils/request'

// 预设分类列表
export function listLedgerCategory(query) {
  return request({
    url: '/cms/ledger/category/list',
    method: 'get',
    params: query
  })
}

// 新增预设分类
export function addLedgerCategory(data) {
  return request({
    url: '/cms/ledger/category',
    method: 'post',
    data: data
  })
}

// 修改预设分类
export function updateLedgerCategory(id, data) {
  return request({
    url: '/cms/ledger/category/' + id,
    method: 'put',
    data: data
  })
}

// 启用/停用分类
export function changeLedgerCategoryStatus(id, status) {
  return request({
    url: '/cms/ledger/category/' + id + '/status/' + status,
    method: 'put'
  })
}

// 删除分类（绑定流水则拒绝）
export function delLedgerCategory(id) {
  return request({
    url: '/cms/ledger/category/' + id,
    method: 'delete'
  })
}

// 运营统计总览（脱敏聚合）
export function getLedgerStats() {
  return request({
    url: '/cms/ledger/stats/overview',
    method: 'get'
  })
}

// 记账用户列表（用户维度聚合：流水/AI使用/token消费，脱敏）
export function listLedgerUsers(query) {
  return request({
    url: '/cms/ledger/users/list',
    method: 'get',
    params: query
  })
}

// 用户流水明细（脱敏简易版：类型/金额/分类/日期）
export function listUserTransactions(userId, query) {
  return request({
    url: '/cms/ledger/users/' + userId + '/transactions',
    method: 'get',
    params: query
  })
}

// 小程序功能配置列表（全量，含隐藏项）
export function listAppFeatures() {
  return request({
    url: '/cms/ledger/app-feature/list',
    method: 'get'
  })
}

// 修改功能配置（名称/图标/排序/可见/角标）
export function updateAppFeature(id, data) {
  return request({
    url: '/cms/ledger/app-feature/' + id,
    method: 'put',
    data: data
  })
}

