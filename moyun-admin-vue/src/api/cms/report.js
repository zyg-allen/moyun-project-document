import request from '@/utils/request'

// 查询举报列表
export function listReport(query) {
  return request({ url: '/cms/report/list', method: 'get', params: query })
}

// 查询举报详细
export function getReport(id) {
  return request({ url: '/cms/report/' + id, method: 'get' })
}

// 处理举报
export function handleReport(data) {
  return request({ url: '/cms/report/handle', method: 'put', data: data })
}

// 删除举报
export function delReport(ids) {
  return request({ url: '/cms/report/' + ids, method: 'delete' })
}
