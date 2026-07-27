/**
 * 知识库管理API
 */
import { get, post, put, del, upload } from './request'

export const knowledgeApi = {
  // ========== 知识库 ==========
  
  // 获取知识库列表
  list: (params) => get('/knowledge-base/list', params),
  
  // 获取知识库详情
  getById: (id) => get(`/knowledge-base/${id}`),
  
  // 上传文档
  upload: (libraryId, file, onProgress) => {
    return upload(`/knowledge-base/upload/${libraryId}`, file, onProgress)
  },
  
  // 删除文档
  delete: (id) => del(`/knowledge-base/${id}`),
  
  // 重新处理文档
  reprocess: (id) => post(`/knowledge-base/${id}/reprocess`),
  
  // 获取文档分片
  getSegments: (id) => get(`/knowledge-base/${id}/segments`),
  
  // ========== 知识库库 ==========
  
  // 获取知识库库列表
  libraryList: (params) => get('/knowledge-library/list', params),
  
  // 创建知识库库
  createLibrary: (data) => post('/knowledge-library', data),
  
  // 更新知识库库
  updateLibrary: (id, data) => put(`/knowledge-library/${id}`, data),
  
  // 删除知识库库
  deleteLibrary: (id) => del(`/knowledge-library/${id}`),
  
  // 获取知识库库文档列表
  getLibraryDocs: (libraryId, params) => get(`/knowledge-library/${libraryId}/documents`, params),
  
  // ========== 知识库配置 ==========
  
  // 获取配置
  getConfig: (knowledgeBaseId) => get(`/knowledge-base/${knowledgeBaseId}/config`),
  
  // 保存配置
  saveConfig: (knowledgeBaseId, config) => post(`/knowledge-base/${knowledgeBaseId}/config`, config),
}

export default knowledgeApi
