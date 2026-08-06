import request from '@/utils/request'

export function listConversation(agentId) {
  return request({
    url: '/cms/ai/conversation/list',
    method: 'get',
    params: { agentId }
  })
}

export function addConversation(agentId, title) {
  return request({
    url: '/cms/ai/conversation/create',
    method: 'post',
    data: { agentId, title }
  })
}

export function delConversation(id) {
  return request({
    url: '/cms/ai/conversation/' + id,
    method: 'delete'
  })
}

export function getMessages(conversationId) {
  return request({
    url: '/cms/ai/conversation/' + conversationId + '/messages',
    method: 'get'
  })
}

// 对话发送：后端 ChatController 仅提供 /stream（SSE 流式），
// 非流式 /send 接口不存在。统一走 stream 接口（与 view 内直接调用保持一致）。
export function sendMessage(conversationId, message, agentId) {
  return request({
    url: '/cms/ai/chat/stream',
    method: 'post',
    data: { conversationId, message, agentId }
  })
}

export function abortChat(conversationId) {
  return request({
    url: '/cms/ai/chat/abort/' + conversationId,
    method: 'post'
  })
}

export function regenerateChat(conversationId, message, agentId) {
  return request({
    url: '/cms/ai/chat/regenerate',
    method: 'post',
    data: { conversationId, message, agentId }
  })
}

// 对话历史：后端无独立 ChatHistoryController，
// 改用 AgentController 提供的 /agent/{id}/sessions 与 /agent/{id}/stats。
export function listChatHistory(agentId, params) {
  return request({
    url: '/cms/ai/agent/' + agentId + '/sessions',
    method: 'get',
    params: params
  })
}

export function getChatStats(agentId) {
  return request({
    url: '/cms/ai/agent/' + agentId + '/stats',
    method: 'get'
  })
}
