import request from '@/utils/request'

// ==================== 智能体基础 CRUD ====================

/**
 * 查询智能体列表
 * @param {Object} query 查询参数（name/status 等）
 */
export function listAgent(query) {
  return request({
    url: '/cms/ai/agent/list',
    method: 'get',
    params: query
  })
}

/**
 * 查询单个智能体详情
 * @param {Number} id 智能体 ID
 */
export function getAgent(id) {
  return request({
    url: '/cms/ai/agent/' + id,
    method: 'get'
  })
}

/**
 * 新增智能体
 * @param {Object} data 智能体数据
 */
export function addAgent(data) {
  return request({
    url: '/cms/ai/agent/create',
    method: 'post',
    data: data
  })
}

/**
 * 修改智能体
 * @param {Object} data 智能体数据（含 id）
 */
export function updateAgent(data) {
  return request({
    url: '/cms/ai/agent/update',
    method: 'put',
    data: data
  })
}

/**
 * 删除智能体
 * @param {Number} id 智能体 ID
 */
export function delAgent(id) {
  return request({
    url: '/cms/ai/agent/' + id,
    method: 'delete'
  })
}

// ==================== 智能体关联关系 ====================

/**
 * 查询所有可被智能体关联的领域词典（已过滤不合法项）
 */
export function listAvailableDictionaries() {
  return request({
    url: '/cms/ai/agent/available-dictionaries',
    method: 'get'
  })
}

/**
 * 查询智能体已关联的领域词典列表
 * @param {Number} agentId 智能体 ID
 */
export function getAgentDictionaries(agentId) {
  return request({
    url: `/cms/ai/agent/${agentId}/dictionaries`,
    method: 'get'
  })
}

/**
 * 更新智能体关联的领域词典（全量覆盖）
 * @param {Number} agentId 智能体 ID
 * @param {Array<Number>} dictionaryIds 词典 ID 数组
 */
export function updateAgentDictionaries(agentId, dictionaryIds) {
  return request({
    url: `/cms/ai/agent/${agentId}/dictionaries`,
    method: 'post',
    data: dictionaryIds
  })
}

// ==================== 智能体统计 / 会话 ====================

/**
 * 查询智能体的使用统计（会话数、Token 数等）
 * @param {Number} agentId 智能体 ID
 */
export function getAgentStats(agentId) {
  return request({
    url: `/cms/ai/agent/${agentId}/stats`,
    method: 'get'
  })
}

/**
 * 查询智能体的最近会话列表
 * @param {Number} agentId 智能体 ID
 * @param {Number} limit 返回条数，默认 10
 */
export function getAgentSessions(agentId, limit = 10) {
  return request({
    url: `/cms/ai/agent/${agentId}/sessions`,
    method: 'get',
    params: { limit }
  })
}

/**
 * 清空智能体的历史会话记录
 * @param {Number} agentId 智能体 ID
 */
export function clearAgentHistory(agentId) {
  return request({
    url: `/cms/ai/agent/${agentId}/history`,
    method: 'delete'
  })
}
