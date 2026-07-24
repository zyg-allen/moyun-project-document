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