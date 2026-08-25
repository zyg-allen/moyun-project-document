import request from '@/utils/request'

// 查询每日写作 prompt 列表
export function listPrompt(query) {
  return request({ url: '/cms/writing-prompt/list', method: 'get', params: query })
}

// 查询 prompt 详情
export function getPrompt(id) {
  return request({ url: '/cms/writing-prompt/' + id, method: 'get' })
}

// 新增 prompt
export function addPrompt(data) {
  return request({ url: '/cms/writing-prompt', method: 'post', data: data })
}

// 修改 prompt
export function updatePrompt(data) {
  return request({ url: '/cms/writing-prompt', method: 'put', data: data })
}

// 删除 prompt
export function delPrompt(id) {
  return request({ url: '/cms/writing-prompt/' + id, method: 'delete' })
}

// AI 为指定日期生成 prompt（已存在则跳过）
export function aiGeneratePrompt(date) {
  return request({ url: '/cms/writing-prompt/ai-generate', method: 'post', params: { date } })
}

// AI 批量补生成（从起始日起连续 N 天，已存在跳过）
export function aiGeneratePromptRange(startDate, days) {
  return request({ url: '/cms/writing-prompt/ai-generate-range', method: 'post', params: { startDate, days } })
}

// AI 重新生成指定 prompt（覆盖内容）
export function aiRegeneratePrompt(id) {
  return request({ url: '/cms/writing-prompt/ai-regenerate/' + id, method: 'put' })
}
