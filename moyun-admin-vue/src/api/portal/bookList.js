import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询书单列表
export function listBookList(query) {
  return request({
    url: '/portal/admin/book-lists/list',
    method: 'get',
    params: query
  })
}

// 查询书单详细
export function getBookList(id) {
  return request({
    url: '/portal/admin/book-lists/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增书单
export function addBookList(data) {
  return request({
    url: '/portal/admin/book-lists',
    method: 'post',
    data: data
  })
}

// 修改书单
export function updateBookList(data) {
  return request({
    url: '/portal/admin/book-lists',
    method: 'put',
    data: data
  })
}

// 删除书单
export function delBookList(id) {
  return request({
    url: '/portal/admin/book-lists/' + id,
    method: 'delete'
  })
}

// 批量删除书单
export function delBookListBatch(ids) {
  return request({
    url: '/portal/admin/book-lists/' + ids,
    method: 'delete'
  })
}
