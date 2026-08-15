import request from '@/utils/request'

// 查询VIP套餐列表
export function listVip(query) {
  return request({
    url: '/cms/vip/list',
    method: 'get',
    params: query
  })
}

// 查询VIP套餐详情
export function getVip(id) {
  return request({
    url: '/cms/vip/' + id,
    method: 'get'
  })
}

// 新增VIP套餐
export function addVip(data) {
  return request({
    url: '/cms/vip',
    method: 'post',
    data: data
  })
}

// 修改VIP套餐
export function updateVip(data) {
  return request({
    url: '/cms/vip',
    method: 'put',
    data: data
  })
}

// 删除VIP套餐
export function delVip(id) {
  return request({
    url: '/cms/vip/' + id,
    method: 'delete'
  })
}
