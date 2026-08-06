import request from '@/utils/request'

// 数据源管理接口：后端 DataAnalysisController 统一挂在 /cms/ai/data-analysis 下，
// 数据源相关子路径为复数 datasources。前端全部对齐后端实际路径。

const BASE = '/cms/ai/data-analysis/datasources'

export function listDatasource(query) {
  return request({
    url: BASE,
    method: 'get',
    params: query
  })
}

export function getDatasource(id) {
  return request({
    url: BASE + '/' + id,
    method: 'get'
  })
}

export function addDatasource(data) {
  return request({
    url: BASE,
    method: 'post',
    data: data
  })
}

export function updateDatasource(data) {
  return request({
    url: BASE + '/' + data.id,
    method: 'put',
    data: data
  })
}

export function delDatasource(id) {
  return request({
    url: BASE + '/' + id,
    method: 'delete'
  })
}

export function testDatasourceConnection(data) {
  return request({
    url: BASE + '/test',
    method: 'post',
    data: data
  })
}

export function getDatasourceTables(id) {
  return request({
    url: BASE + '/' + id + '/tables',
    method: 'get'
  })
}

export function getTableSchema(id, tableName) {
  return request({
    url: BASE + '/' + id + '/tables/' + tableName + '/schema',
    method: 'get'
  })
}

export function syncMetadata(id) {
  return request({
    url: BASE + '/' + id + '/sync',
    method: 'post'
  })
}
