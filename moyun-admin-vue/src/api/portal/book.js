import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询书籍列表
export function listBook(query) {
  return request({
    url: '/portal/admin/books/list',
    method: 'get',
    params: query
  })
}

// 查询书籍详细
export function getBook(id) {
  return request({
    url: '/portal/admin/books/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增书籍
export function addBook(data) {
  return request({
    url: '/portal/admin/books',
    method: 'post',
    data: data
  })
}

// 修改书籍
export function updateBook(data) {
  return request({
    url: '/portal/admin/books',
    method: 'put',
    data: data
  })
}

// 删除书籍
export function delBook(id) {
  return request({
    url: '/portal/admin/books/' + id,
    method: 'delete'
  })
}

// 批量删除书籍
export function delBookBatch(ids) {
  return request({
    url: '/portal/admin/books/ids/' + ids,
    method: 'delete'
  })
}
