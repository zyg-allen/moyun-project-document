import request from '@/utils/request'

// ==================== 场景配置基础 CRUD ====================

/**
 * 查询场景配置列表
 * @param {Object} query 查询参数（sceneCode 场景代码筛选）
 */
export function listScene(query) {
  return request({
    url: '/cms/ai/scene/list',
    method: 'get',
    params: query
  })
}

/**
 * 查询单个场景配置详情
 * @param {Number} id 场景配置 ID
 */
export function getScene(id) {
  return request({
    url: '/cms/ai/scene/' + id,
    method: 'get'
  })
}

/**
 * 新增场景配置
 * @param {Object} data 场景配置数据
 */
export function addScene(data) {
  return request({
    url: '/cms/ai/scene/create',
    method: 'post',
    data: data
  })
}

/**
 * 修改场景配置
 * @param {Object} data 场景配置数据（含 id）
 */
export function updateScene(data) {
  return request({
    url: '/cms/ai/scene/update',
    method: 'put',
    data: data
  })
}

/**
 * 删除场景配置
 * @param {Number} id 场景配置 ID
 */
export function delScene(id) {
  return request({
    url: '/cms/ai/scene/' + id,
    method: 'delete'
  })
}

// ==================== 场景解析测试 ====================

/**
 * 测试场景配置（返回绑定解析结果摘要）
 * @param {Number} id 场景配置 ID
 */
export function testScene(id) {
  return request({
    url: `/cms/ai/scene/${id}/test`,
    method: 'post'
  })
}