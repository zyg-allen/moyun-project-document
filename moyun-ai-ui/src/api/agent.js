/**
 * 智能体管理API
 */
import { get, post, put, del } from './request'

export const agentApi = {
  // 获取智能体列表
  list: (params) => get('/agent/list', params),
  
  // 获取智能体详情
  getById: (id) => get(`/agent/${id}`),
  
  // 创建智能体
  create: (data) => post('/agent', data),
  
  // 更新智能体
  update: (id, data) => put(`/agent/${id}`, data),
  
  // 删除智能体
  delete: (id) => del(`/agent/${id}`),
  
  // 获取智能体工具列表
  getTools: (agentId) => get(`/agent/${agentId}/tools`),
  
  // 更新智能体工具
  updateTools: (agentId, toolIds) => post(`/agent/${agentId}/tools`, toolIds),
  
  // 获取智能体工作流
  getWorkflows: (agentId) => get(`/agent/${agentId}/workflows`),
  
  // 更新智能体工作流
  updateWorkflows: (agentId, workflowIds) => post(`/agent/${agentId}/workflows`, workflowIds),
  
  // 测试智能体
  test: (agentId, message) => post(`/agent/${agentId}/test`, { message }),
  
  // 生成API Key
  generateApiKey: (agentId) => post(`/agent/${agentId}/api-key`),
}

export default agentApi
