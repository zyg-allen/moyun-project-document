/**
 * 工作流管理API
 */
import { get, post, put, del } from './request'

export const workflowApi = {
  // 获取工作流列表
  list: (params) => get('/workflow/list', params),
  
  // 获取工作流详情
  getById: (id) => get(`/workflow/${id}`),
  
  // 创建工作流
  create: (data) => post('/workflow', data),
  
  // 更新工作流
  update: (id, data) => put(`/workflow/${id}`, data),
  
  // 删除工作流
  delete: (id) => del(`/workflow/${id}`),
  
  // 执行工作流
  execute: (id, input) => post(`/workflow/${id}/execute`, { input }),
  
  // 获取执行历史
  getExecutions: (id, params) => get(`/workflow/${id}/executions`, params),
  
  // 获取执行详情
  getExecution: (executionId) => get(`/workflow/execution/${executionId}`),
  
  // 启用/禁用工作流
  toggleStatus: (id, enabled) => put(`/workflow/${id}/status`, { enabled }),
  
  // AI生成工作流
  generate: (description) => post('/workflow-generator/generate', { description }),
  
  // 复制工作流
  copy: (id) => post(`/workflow/${id}/copy`),
}

export default workflowApi
