import request from '@/utils/request'

export function listArticle(query) {
  return request({
    url: '/admin/article/page',
    method: 'get',
    params: query
  })
}

export function listPendingArticle() {
  return request({
    url: '/admin/article/pending/page',
    method: 'get'
  })
}

export function approveArticle(articleId, reason) {
  return request({
    url: '/admin/article/audit/approve',
    method: 'post',
    data: { articleId, reason }
  })
}

export function rejectArticle(articleId, reason) {
  return request({
    url: '/admin/article/audit/reject',
    method: 'post',
    data: { articleId, reason }
  })
}

export function offlineArticle(articleId) {
  return request({
    url: `/admin/article/offline/${articleId}`,
    method: 'put'
  })
}

export function deleteArticle(articleId) {
  return request({
    url: `/admin/article/delete/${articleId}`,
    method: 'delete'
  })
}
