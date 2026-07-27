/**
 * 对话管理API
 */
import { get, post, del } from './request'
import axios from 'axios'

export const chatApi = {
  // ========== 对话 ==========
  
  // 获取会话列表
  getConversations: (agentId) => get('/conversation/list', { agentId }),
  
  // 创建会话
  createConversation: (agentId, title) => post('/conversation/create', { agentId, title }),
  
  // 删除会话
  deleteConversation: (id) => del(`/conversation/${id}`),
  
  // 获取会话消息
  getMessages: (conversationId) => get(`/conversation/${conversationId}/messages`),
  
  // 清空会话消息
  clearMessages: (conversationId) => del(`/conversation/${conversationId}/messages`),
  
  // ========== 流式对话 ==========
  
  // 发送消息（流式）
  sendMessageStream: (conversationId, message, agentId, onMessage, onError, onComplete) => {
    const token = localStorage.getItem('token')
    const eventSource = new EventSource(
      `/api/chat/stream?conversationId=${conversationId}&message=${encodeURIComponent(message)}&agentId=${agentId}&token=${token}`
    )
    
    eventSource.onmessage = (event) => {
      if (event.data === '[DONE]') {
        eventSource.close()
        onComplete && onComplete()
      } else {
        onMessage && onMessage(event.data)
      }
    }
    
    eventSource.onerror = (error) => {
      eventSource.close()
      onError && onError(error)
    }
    
    return eventSource
  },
  
  // 发送消息（非流式）
  sendMessage: (conversationId, message, agentId) => {
    return post('/chat/send', { conversationId, message, agentId })
  },
  
  // 中断对话
  abortChat: (conversationId) => {
    return post(`/chat/abort/${conversationId}`)
  },
  
  // 重新生成AI回复
  regenerate: (conversationId, message, agentId) => {
    return post('/chat/regenerate', { conversationId, message, agentId })
  },
  
  // ========== 对话历史 ==========
  
  // 获取对话历史
  getHistory: (agentId, params) => get(`/chat-history/${agentId}`, params),
  
  // 获取对话统计
  getStats: (agentId) => get(`/chat-history/${agentId}/stats`),
}

export default chatApi
