/**
 * 数据源管理API
 */
import { get, post, put, del } from './request'

export const datasourceApi = {
  // 获取数据源列表
  list: (params) => get('/datasource/list', params),
  
  // 获取数据源详情
  getById: (id) => get(`/datasource/${id}`),
  
  // 创建数据源
  create: (data) => post('/datasource', data),
  
  // 更新数据源
  update: (id, data) => put(`/datasource/${id}`, data),
  
  // 删除数据源
  delete: (id) => del(`/datasource/${id}`),
  
  // 测试数据源连接
  testConnection: (data) => post('/datasource/test', data),
  
  // 获取表列表
  getTables: (id) => get(`/datasource/${id}/tables`),
  
  // 获取表结构
  getTableSchema: (id, tableName) => get(`/datasource/${id}/tables/${tableName}/schema`),
  
  // 同步表元数据
  syncMetadata: (id) => post(`/datasource/${id}/sync`),
}

export default datasourceApi
