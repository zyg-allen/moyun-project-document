import request from '@/utils/request';

// AI 执行日志管理（v11.60 P1-1：统一网关 ai_execute_log 可观测性查询页）

// 分页查询日志列表
export function listExecuteLog(query) {
  return request({
    url: '/cms/ai/execute-log/list',
    method: 'get',
    params: query
  });
}

// 汇总卡片（调用量/成功率/Token/成本/平均耗时，随筛选联动）
export function getExecuteLogSummary(query) {
  return request({
    url: '/cms/ai/execute-log/summary',
    method: 'get',
    params: query
  });
}

// 日志中出现的场景代码（筛选下拉）
export function getSceneOptions() {
  return request({
    url: '/cms/ai/execute-log/scene-options',
    method: 'get'
  });
}

// 日志详情
export function getExecuteLog(id) {
  return request({
    url: `/cms/ai/execute-log/${id}`,
    method: 'get'
  });
}

// 删除日志（批量，过期数据清理）
export function delExecuteLog(ids) {
  return request({
    url: `/cms/ai/execute-log/${ids}`,
    method: 'delete'
  });
}
