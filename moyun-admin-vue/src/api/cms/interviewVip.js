import request from '@/utils/request'

// 查询面试会员套餐列表
export function listInterviewVipPackage(query) {
  return request({
    url: '/cms/interview/vipPackage/list',
    method: 'get',
    params: query
  })
}

// 新增面试会员套餐
export function addInterviewVipPackage(data) {
  return request({
    url: '/cms/interview/vipPackage',
    method: 'post',
    data: data
  })
}

// 修改面试会员套餐
export function updateInterviewVipPackage(data) {
  return request({
    url: '/cms/interview/vipPackage',
    method: 'put',
    data: data
  })
}

// 删除面试会员套餐（有订单仅可下架）
export function delInterviewVipPackage(id) {
  return request({
    url: '/cms/interview/vipPackage/' + id,
    method: 'delete'
  })
}
