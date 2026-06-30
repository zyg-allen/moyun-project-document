import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询评论列表
export function listComment(query) {
  return request({
    url: '/cms/comment/list',
    method: 'get',
    params: query
  })
}

// 查询评论详细
export function getComment(commentId) {
  return request({
    url: '/cms/comment/' + parseStrEmpty(commentId),
    method: 'get'
  })
}

// 删除评论
export function delComment(commentId) {
  return request({
    url: '/cms/comment/' + commentId,
    method: 'delete'
  })
}

// 评论审核
export function auditComment(id, status) {
  const data = {
    id,
    status
  }
  return request({
    url: '/cms/comment/audit',
    method: 'put',
    data: data
  })
}
