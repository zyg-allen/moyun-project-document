import request from '@/utils/request'

/**
 * 查询工具列表（管理端，含禁用工具）
 * @param {Object} query 查询参数（name/category 等）
 */
export function listTool(query) {
  return request({
    url: '/cms/ai/tool/list',
    method: 'get',
    params: query
  })
}

/**
 * 查询所有已启用的工具（供智能体关联选择）
 */
export function listEnabledTool() {
  return request({
    url: '/cms/ai/tool/enabled',
    method: 'get'
  })
}

/**
 * 启用/禁用工具
 * @param {Number} id 工具 ID
 * @param {Boolean} enabled 是否启用
 */
export function toggleTool(id, enabled) {
  return request({
    url: `/cms/ai/tool/${id}/toggle`,
    method: 'put',
    params: { enabled }
  })
}

/**
 * 测试工具调用
 * @param {String} toolName 工具名称
 * @param {Object} params 调用参数
 */
export function testTool(toolName, params) {
  return request({
    url: `/cms/ai/tool/test/${toolName}`,
    method: 'post',
    data: params
  })
}

// ==================== 智能体 - 工具关联 ====================

/**
 * 查询智能体已绑定的工具 ID 列表
 * @param {Number} agentId 智能体 ID
 */
export function getAgentToolIds(agentId) {
  return request({
    url: `/cms/ai/tool/agent/${agentId}/ids`,
    method: 'get'
  })
}

/**
 * 绑定智能体与工具（全量覆盖）
 * @param {Number} agentId 智能体 ID
 * @param {Array<Number>} toolIds 工具 ID 数组
 */
export function bindAgentTools(agentId, toolIds) {
  return request({
    url: `/cms/ai/tool/agent/${agentId}/bind`,
    method: 'post',
    data: toolIds
  })
}
