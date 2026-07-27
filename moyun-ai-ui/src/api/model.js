/**
 * 模型配置API
 */
import { get, post, put, del } from './request'

export const modelApi = {
  // 获取模型配置列表
  list: (params) => get('/model-config/list', params),
  
  // 获取模型配置详情
  getById: (id) => get(`/model-config/${id}`),
  
  // 创建模型配置
  create: (data) => post('/model-config', data),
  
  // 更新模型配置
  update: (id, data) => put(`/model-config/${id}`, data),
  
  // 删除模型配置
  delete: (id) => del(`/model-config/${id}`),
  
  // 测试模型连接
  test: (id) => post(`/model-config/${id}/test`),
  
  // 获取可用模型列表（按提供商）
  getAvailableModels: (provider) => get('/model-config/available', { provider }),
  
  // 设置默认模型
  setDefault: (id, type) => put(`/model-config/${id}/default`, { type }),
}

export default modelApi
