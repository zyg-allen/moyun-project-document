import request from '@/utils/request'

// 查询每日写作 prompt 列表
export function listPrompt(query) {
  return request({ url: '/cms/writing-prompt/list', method: 'get', params: query })
}

// 查询 prompt 详情
export function getPrompt(id) {
  return request({ url: '/cms/writing-prompt/' + id, method: 'get' })
}

// 新增 prompt
export function addPrompt(data) {
  return request({ url: '/cms/writing-prompt', method: 'post', data: data })
}

// 修改 prompt
export function updatePrompt(data) {
  return request({ url: '/cms/writing-prompt', method: 'put', data: data })
}

// 删除 prompt
export function delPrompt(id) {
  return request({ url: '/cms/writing-prompt/' + id, method: 'delete' })
}
