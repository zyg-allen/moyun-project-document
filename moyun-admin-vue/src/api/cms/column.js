import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

// 查询专栏列表
export function listColumn(query) {
  return request({
    url: '/cms/column/list',
    method: 'get',
    params: query
  })
}

// 查询专栏详情
export function getColumn(id) {
  return request({
    url: '/cms/column/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增专栏
export function addColumn(data) {
  return request({
    url: '/cms/column',
    method: 'post',
    data: data
  })
}

// 修改专栏
export function updateColumn(data) {
  return request({
    url: '/cms/column',
    method: 'put',
    data: data
  })
}

// 删除专栏（支持批量，ids 逗号分隔）
export function delColumn(ids) {
  return request({
    url: '/cms/column/' + ids,
    method: 'delete'
  })
}

// 审核专栏（状态流转 draft→published→archived）
export function changeColumnStatus(id, status) {
  return request({
    url: '/cms/column/' + id + '/status',
    method: 'put',
    data: { status: status }
  })
}

// 审核专栏（CMS 审核接口，写入 auditorId/auditRemark/auditTime 并通知作者）
// PUT /cms/column/{id}/audit  body: { status: 'published'|'rejected', auditRemark? }
export function auditColumn(id, data) {
  return request({
    url: '/cms/column/' + id + '/audit',
    method: 'put',
    data: data
  })
}
