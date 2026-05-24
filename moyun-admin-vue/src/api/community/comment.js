import request from '@/utils/request'

export function listComment(query) {
  return request({
    url: '/admin/comment/page',
    method: 'get',
    params: query
  })
}

export function deleteComment(commentId) {
  return request({
    url: `/admin/comment/delete/${commentId}`,
    method: 'delete'
  })
}

export function getCommentDetail(commentId) {
  return request({
    url: `/admin/comment/${commentId}`,
    method: 'get'
  })
}
