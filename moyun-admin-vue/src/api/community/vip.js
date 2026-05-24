import request from '@/utils/request'

export function listVipPackage(query) {
  return request({
    url: '/admin/vip/package/page',
    method: 'get',
    params: query
  })
}

export function getAllVipPackages() {
  return request({
    url: '/admin/vip/package/list',
    method: 'get'
  })
}

export function getVipPackage(packageId) {
  return request({
    url: `/admin/vip/package/${packageId}`,
    method: 'get'
  })
}

export function addVipPackage(data) {
  return request({
    url: '/admin/vip/package/add',
    method: 'post',
    data: data
  })
}

export function updateVipPackage(data) {
  return request({
    url: '/admin/vip/package/edit',
    method: 'put',
    data: data
  })
}

export function deleteVipPackage(packageId) {
  return request({
    url: `/admin/vip/package/delete/${packageId}`,
    method: 'delete'
  })
}

export function updatePackageStatus(packageId, status) {
  return request({
    url: `/admin/vip/package/status/${packageId}/${status}`,
    method: 'put'
  })
}

export function listVipOrder(query) {
  return request({
    url: '/admin/vip/order/page',
    method: 'get',
    params: query
  })
}

export function listUserVip(query) {
  return request({
    url: '/admin/vip/user/page',
    method: 'get',
    params: query
  })
}
