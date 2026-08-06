import request from '@/utils/request'

export function listKnowledgeBase(query) {
  return request({
    url: '/cms/ai/knowledge-base/list',
    method: 'get',
    params: query
  })
}

export function getKnowledgeBase(id) {
  return request({
    url: '/cms/ai/knowledge-base/' + id,
    method: 'get'
  })
}

export function uploadKnowledgeDoc(libraryId, file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/cms/ai/knowledge-base/upload',
    method: 'post',
    data: formData,
    params: { libraryId },
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function delKnowledgeDoc(id) {
  return request({
    url: '/cms/ai/knowledge-base/' + id,
    method: 'delete'
  })
}

export function reprocessKnowledgeDoc(id) {
  return request({
    url: '/cms/ai/knowledge-base/' + id + '/reprocess',
    method: 'post'
  })
}

export function getKnowledgeSegments(id) {
  return request({
    url: '/cms/ai/knowledge-base/' + id + '/segments',
    method: 'get'
  })
}

export function listKnowledgeLibrary(query) {
  return request({
    url: '/cms/ai/knowledge-library/list',
    method: 'get',
    params: query
  })
}

export function addKnowledgeLibrary(data) {
  return request({
    url: '/cms/ai/knowledge-library',
    method: 'post',
    data: data
  })
}

export function updateKnowledgeLibrary(data) {
  return request({
    url: '/cms/ai/knowledge-library',
    method: 'put',
    data: data
  })
}

export function delKnowledgeLibrary(id) {
  return request({
    url: '/cms/ai/knowledge-library/' + id,
    method: 'delete'
  })
}

export function getLibraryDocs(libraryId, params) {
  return request({
    url: '/cms/ai/knowledge-library/' + libraryId + '/documents',
    method: 'get',
    params: params
  })
}

export function getKnowledgeConfig(knowledgeBaseId) {
  return request({
    url: '/cms/ai/knowledge-base/' + knowledgeBaseId + '/config',
    method: 'get'
  })
}

export function saveKnowledgeConfig(knowledgeBaseId, config) {
  return request({
    url: '/cms/ai/knowledge-base/' + knowledgeBaseId + '/config',
    method: 'post',
    data: config
  })
}
