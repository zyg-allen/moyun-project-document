import request from '@/utils/request'

// 获取文件列表
export function listFile(query) {
  return request({
    url: '/system/file/list',
    method: 'get',
    params: query
  })
}

// 获取文件详情
export function getFile(id) {
  return request({
    url: '/system/file/' + id,
    method: 'get'
  })
}

// 上传文件
export function uploadFile(data) {
  return request({
    url: '/system/file/upload',
    method: 'post',
    data: data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 删除文件
export function delFile(id) {
  return request({
    url: '/system/file/' + id,
    method: 'delete'
  })
}

// 批量删除文件
export function delFiles(ids) {
  return request({
    url: '/system/file/' + ids,
    method: 'delete'
  })
}
