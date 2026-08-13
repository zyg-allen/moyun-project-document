import request from '@/utils/request'

/**
 * 统一审核任务 API（v8.1）
 *
 * 后端入口：/system/audit-task
 * - pending        待办列表（审核中心 / 首页「更多」）
 * - my-handled     我的已办（按当前处理人过滤）
 * - list           全部任务（含 pending + 已处理）
 * - {id}           任务详情（含 bizDetail）
 * - handle         处理任务（同意/驳回，驳回时原因必填）
 * - todo-summary   首页待办摘要（前 N 条）
 * - my-handled-summary 首页我的已办摘要（前 N 条）
 * - count-pending  待办总数（首页角标）
 * - count-by-type  按类型统计待办数（审核中心 Tab 角标）
 */

// 查询待办列表（status=pending）
export function listPending(query) {
  return request({
    url: '/system/audit-task/pending',
    method: 'get',
    params: query
  })
}

// 查询我的已办列表（auditor_id=当前用户）
export function listMyHandled(query) {
  return request({
    url: '/system/audit-task/my-handled',
    method: 'get',
    params: query
  })
}

// 查询全部任务列表（含 pending + 已处理）
export function listAll(query) {
  return request({
    url: '/system/audit-task/list',
    method: 'get',
    params: query
  })
}

// 获取审核任务详情（含 bizDetail）
export function getAuditTask(id) {
  return request({
    url: '/system/audit-task/' + id,
    method: 'get'
  })
}

// 处理审核任务（同意/驳回）
// payload: { taskId, action: 'approve'|'reject', auditOpinion, notifyUser }
export function handleAuditTask(data) {
  return request({
    url: '/system/audit-task/handle',
    method: 'post',
    data: data
  })
}

// 首页待办摘要（默认前 5 条）
export function todoSummary(limit) {
  return request({
    url: '/system/audit-task/todo-summary',
    method: 'get',
    params: { limit }
  })
}

// 首页我的已办摘要（默认前 5 条）
export function myHandledSummary(limit) {
  return request({
    url: '/system/audit-task/my-handled-summary',
    method: 'get',
    params: { limit }
  })
}

// 待办总数（首页角标）
export function countPending() {
  return request({
    url: '/system/audit-task/count-pending',
    method: 'get'
  })
}

// 按任务类型统计待办数（审核中心 Tab 角标）
export function countByType() {
  return request({
    url: '/system/audit-task/count-by-type',
    method: 'get'
  })
}
