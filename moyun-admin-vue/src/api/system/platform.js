import request from '@/utils/request'

// 查询平台端列表
export function listPlatform(query) {
  return request({
    url: '/system/platform/list',
    method: 'get',
    params: query
  })
}

// 查询平台端下拉选项（启用状态）
export function platformOptionselect() {
  return request({
    url: '/system/platform/optionselect',
    method: 'get'
  })
}

// 新增平台端
export function addPlatform(data) {
  return request({
    url: '/system/platform',
    method: 'post',
    data: data
  })
}

// 修改平台端
export function updatePlatform(data) {
  return request({
    url: '/system/platform',
    method: 'put',
    data: data
  })
}

// 删除平台端（portal/ledger/admin 预置端禁删）
export function delPlatform(ids) {
  return request({
    url: '/system/platform/' + ids,
    method: 'delete'
  })
}
