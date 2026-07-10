import request from '@/utils/request'

// 我的会话列表（管理员参与的所有私信会话，按最后消息时间倒序）
export function listSessions(query) {
  return request({
    url: '/system/message/sessions',
    method: 'get',
    params: query
  })
}

// 会话历史消息（仅会话成员可查）
export function getHistory(sessionId, query) {
  return request({
    url: '/system/message/' + sessionId + '/history',
    method: 'get',
    params: query
  })
}

// 发送消息（回复门户用户）
// data: { receiverId, receiverType(默认 portal), content, msgType }
export function sendMessage(data) {
  return request({
    url: '/system/message/send',
    method: 'post',
    data: data
  })
}

// 标记会话已读（清零当前管理员未读数，消息置为已读）
export function markSessionRead(sessionId) {
  return request({
    url: '/system/message/session/' + sessionId + '/read',
    method: 'put'
  })
}

// 当前管理员私信总未读数（用于头部铃铛徽章）
export function getUnreadCount() {
  return request({
    url: '/system/message/unread-count',
    method: 'get'
  })
}
