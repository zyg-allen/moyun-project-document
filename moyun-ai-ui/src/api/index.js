/**
 * API统一导出入口
 * 
 * 使用方式:
 * import { agentApi, knowledgeApi } from '@/api'
 * 
 * // 调用
 * const agents = await agentApi.list()
 */

export { default as agentApi } from './agent'
export { default as knowledgeApi } from './knowledge'
export { default as workflowApi } from './workflow'
export { default as chatApi } from './chat'
export { default as modelApi } from './model'
export { default as datasourceApi } from './datasource'

// 导出请求方法
export { default as request, get, post, put, del, upload, download } from './request'
