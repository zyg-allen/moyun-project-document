import request from '@/utils/request'

// 查询敏感词列表
export function listSensitiveWord(query) {
  return request({
    url: '/system/sensitiveWord/list',
    method: 'get',
    params: query
  })
}

// 查询敏感词详情
export function getSensitiveWord(id) {
  return request({
    url: '/system/sensitiveWord/' + id,
    method: 'get'
  })
}

// 新增敏感词
export function addSensitiveWord(data) {
  return request({
    url: '/system/sensitiveWord',
    method: 'post',
    data: data
  })
}

// 修改敏感词
export function updateSensitiveWord(data) {
  return request({
    url: '/system/sensitiveWord',
    method: 'put',
    data: data
  })
}

// 批量删除敏感词
export function delSensitiveWord(ids) {
  return request({
    url: '/system/sensitiveWord/' + ids,
    method: 'delete'
  })
}

// 刷新敏感词树（重新加载到内存）
export function reloadSensitiveWord() {
  return request({
    url: '/system/sensitiveWord/reload',
    method: 'put'
  })
}
