import request from '@/utils/request';

export function listTopic(query) {
  return request({
    url: '/cms/topic/list',
    method: 'get',
    params: query
  });
}

export function getTopic(id) {
  return request({
    url: '/cms/topic/' + id,
    method: 'get'
  });
}

export function updateTopicStatus(id, status) {
  return request({
    url: '/cms/topic/' + id + '/status',
    method: 'put',
    data: { status }
  });
}

export function updateTopicPinned(id, pinned) {
  return request({
    url: '/cms/topic/' + id + '/pinned',
    method: 'put',
    data: { pinned }
  });
}

export function featureTopic(id) {
  return request({
    url: '/cms/topic/' + id + '/featured',
    method: 'put'
  });
}

export function delTopic(id) {
  return request({
    url: '/cms/topic/' + id,
    method: 'delete'
  });
}

export function listPost(query) {
  return request({
    url: '/cms/topic/post/list',
    method: 'get',
    params: query
  });
}

export function delPost(postId) {
  return request({
    url: '/cms/topic/post/' + postId,
    method: 'delete'
  });
}

export function listComment(query) {
  return request({
    url: '/cms/topic/comment/list',
    method: 'get',
    params: query
  });
}

export function delComment(commentId) {
  return request({
    url: '/cms/topic/comment/' + commentId,
    method: 'delete'
  });
}

// AI 生成今日话题草稿（v11.57 P0-3：daily_topic 场景走统一网关，不落库）
export function aiGenerateTopic(domain) {
  return request({
    url: '/cms/topic/ai-generate',
    method: 'post',
    data: { domain }
  });
}

// 发布官方话题（管理员确认 AI 草稿或手动录入，status=active 直接生效）
export function createOfficialTopic(data) {
  return request({
    url: '/cms/topic/create-official',
    method: 'post',
    data
  });
}