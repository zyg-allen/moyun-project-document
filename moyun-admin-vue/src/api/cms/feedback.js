import request from '@/utils/request'

// 查询反馈列表
export function listFeedback(query) {
  return request({ url: '/cms/feedback/list', method: 'get', params: query })
}

// 查询反馈详细
export function getFeedback(id) {
  return request({ url: '/cms/feedback/' + id, method: 'get' })
}

// 处理反馈
export function handleFeedback(data) {
  return request({ url: '/cms/feedback/handle', method: 'put', data: data })
}

// 删除反馈
export function delFeedback(ids) {
  return request({ url: '/cms/feedback/' + ids, method: 'delete' })
}
