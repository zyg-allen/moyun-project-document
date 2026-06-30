import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询金句列表
export function listBookQuote(query) {
  return request({
    url: '/portal/admin/book-quotes/list',
    method: 'get',
    params: query
  })
}

// 查询金句详细
export function getBookQuote(id) {
  return request({
    url: '/portal/admin/book-quotes/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增金句
export function addBookQuote(data) {
  return request({
    url: '/portal/admin/book-quotes',
    method: 'post',
    data: data
  })
}

// 修改金句
export function updateBookQuote(data) {
  return request({
    url: '/portal/admin/book-quotes',
    method: 'put',
    data: data
  })
}

// 删除金句
export function delBookQuote(id) {
  return request({
    url: '/portal/admin/book-quotes/' + id,
    method: 'delete'
  })
}

// 批量删除金句
export function delBookQuoteBatch(ids) {
  return request({
    url: '/portal/admin/book-quotes/ids/' + ids,
    method: 'delete'
  })
}
