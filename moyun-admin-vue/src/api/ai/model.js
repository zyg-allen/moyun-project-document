import request from '@/utils/request'

// 模型配置接口：与后端 ModelConfigController 路径对齐。
// 后端实际路径：
//   POST /cms/ai/model-config/create
//   PUT  /cms/ai/model-config/update
//   POST /cms/ai/model-config/test          （无 id，body 传配置）
//   POST /cms/ai/model-config/set-default/{id}
//   GET  /cms/ai/model-config/default       （无 id）
//   GET  /cms/ai/model-config/list
//   GET  /cms/ai/model-config/{id}
//   DELETE /cms/ai/model-config/{id}

export function listModelConfig(query) {
  return request({
    url: '/cms/ai/model-config/list',
    method: 'get',
    params: query
  })
}

export function getModelConfig(id) {
  return request({
    url: '/cms/ai/model-config/' + id,
    method: 'get'
  })
}

export function addModelConfig(data) {
  return request({
    url: '/cms/ai/model-config/create',
    method: 'post',
    data: data
  })
}

export function updateModelConfig(data) {
  return request({
    url: '/cms/ai/model-config/update',
    method: 'put',
    data: data
  })
}

export function delModelConfig(id) {
  return request({
    url: '/cms/ai/model-config/' + id,
    method: 'delete'
  })
}

export function testModelConfig(data) {
  return request({
    url: '/cms/ai/model-config/test',
    method: 'post',
    data: data
  })
}

export function getDefaultModel() {
  return request({
    url: '/cms/ai/model-config/default',
    method: 'get'
  })
}

export function setDefaultModel(id) {
  return request({
    url: '/cms/ai/model-config/set-default/' + id,
    method: 'post'
  })
}
