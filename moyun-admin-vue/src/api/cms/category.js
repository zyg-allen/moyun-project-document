import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询分类列表
export function listCategory(query) {
  return request({
    url: '/cms/category/list',
    method: 'get',
    params: query
  })
}

// 查询分类详细
export function getCategory(categoryId) {
  return request({
    url: '/cms/category/' + parseStrEmpty(categoryId),
    method: 'get'
  })
}

// 新增分类
export function addCategory(data) {
  return request({
    url: '/cms/category',
    method: 'post',
    data: data
  })
}

// 修改分类
export function updateCategory(data) {
  return request({
    url: '/cms/category',
    method: 'put',
    data: data
  })
}

// 删除分类（单个ID）
export function delCategory(categoryId) {
  return request({
    url: '/cms/category/' + categoryId,
    method: 'delete'
  })
}

// 删除分类（批量）
export function delCategoryBatch(ids) {
  return request({
    url: '/cms/category/batch',
    method: 'delete',
    data: ids
  })
}

// 分类状态修改
export function changeCategoryStatus(categoryId, status) {
  const data = {
    categoryId,
    status
  }
  return request({
    url: '/cms/category/changeStatus',
    method: 'put',
    data: data
  })
}
