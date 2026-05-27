import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询文章列表
export function listArticle(query) {
  return request({
    url: '/cms/article/list',
    method: 'get',
    params: query
  })
}

// 查询文章详细
export function getArticle(articleId) {
  return request({
    url: '/cms/article/' + parseStrEmpty(articleId),
    method: 'get'
  })
}

// 新增文章
export function addArticle(data) {
  return request({
    url: '/cms/article',
    method: 'post',
    data: data
  })
}

// 修改文章
export function updateArticle(data) {
  return request({
    url: '/cms/article',
    method: 'put',
    data: data
  })
}

// 删除文章
export function delArticle(articleId) {
  return request({
    url: '/cms/article/' + articleId,
    method: 'delete'
  })
}

// 文章审核
export function auditArticle(articleId, auditStatus) {
  const data = {
    articleId,
    auditStatus
  }
  return request({
    url: '/cms/article/audit',
    method: 'put',
    data: data
  })
}

// 文章上下架
export function changeArticleStatus(articleId, status) {
  const data = {
    articleId,
    status
  }
  return request({
    url: '/cms/article/changeStatus',
    method: 'put',
    data: data
  })
}
