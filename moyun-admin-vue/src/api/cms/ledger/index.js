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

// 停用预设分类
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
