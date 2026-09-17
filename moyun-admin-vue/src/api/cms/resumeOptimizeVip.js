import request from '@/utils/request'

// 查询简历优化会员套餐列表
export function listResumeOptimizeVipPackage(query) {
  return request({
    url: '/cms/resume/optimize/vipPackage/list',
    method: 'get',
    params: query
  })
}

// 新增简历优化会员套餐
export function addResumeOptimizeVipPackage(data) {
  return request({
    url: '/cms/resume/optimize/vipPackage',
    method: 'post',
    data: data
  })
}

// 修改简历优化会员套餐
export function updateResumeOptimizeVipPackage(data) {
  return request({
    url: '/cms/resume/optimize/vipPackage',
    method: 'put',
    data: data
  })
}

// 删除简历优化会员套餐（有订单仅可下架）
export function delResumeOptimizeVipPackage(id) {
  return request({
    url: '/cms/resume/optimize/vipPackage/' + id,
    method: 'delete'
  })
}
