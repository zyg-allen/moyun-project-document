import { httpGet, httpPost, httpPut, httpGetList } from './client'
import type { ApiResponse, PaginationResponse } from '@/types/api'
import type {
    MessageSessionVO,
    MessageVO,
    SendMessageParams,
    MessageSessionListParams,
    MessageHistoryParams,
} from '@/types/api'

/**
 * 私信相关 API
 * 后端契约：
 * - GET  /portal/message/sessions           会话列表（分页）
 * - GET  /portal/message/{sessionId}/history 历史消息（分页）
 * - POST /portal/message/send               发送消息（body: {receiverId, content, msgType}）
 * - PUT  /portal/message/session/{id}/read   标记会话已读
 * - GET  /portal/message/unread-count       总未读数
 * - GET  /portal/message/session/with/{userId} 按对方用户ID获取或创建会话
 */

// 获取会话列表
export const getSessionList = (params?: MessageSessionListParams) => {
    return httpGetList<MessageSessionVO>('/portal/message/sessions', params)
}

// 获取历史消息
export const getMessageHistory = (sessionId: string, params?: MessageHistoryParams) => {
    return httpGetList<MessageVO>(`/portal/message/${sessionId}/history`, params)
}

// 发送消息
export const sendMessage = (params: SendMessageParams) => {
    return httpPost<MessageVO>('/portal/message/send', {
        receiverId: params.receiverId,
        content: params.content,
        msgType: params.msgType || 'text',
    })
}

// 标记会话已读
export const markSessionRead = (sessionId: string) => {
    return httpPut<void>(`/portal/message/session/${sessionId}/read`)
}

// 获取总未读数
export const getUnreadMessageCount = () => {
    return httpGet<number>('/portal/message/unread-count')
}

// 按对方用户ID获取或创建会话（用于作者主页发起新私信）
export const getOrCreateSession = (peerUserId: string | number) => {
    return httpGet<MessageSessionVO>(`/portal/message/session/with/${peerUserId}`)
}

export type {
    ApiResponse,
    PaginationResponse,
    MessageSessionVO,
    MessageVO,
    SendMessageParams,
    MessageSessionListParams,
    MessageHistoryParams,
}
