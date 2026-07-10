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

// 按文件URL删除文件（用于附件删除/替换时清理存储+记录，组件只持有url无fileId时使用）
export function delFileByUrl(fileUrl) {
  return request({
    url: '/system/file/byUrl',
    method: 'delete',
    params: { fileUrl }
  })
}

// 按文件URL批量删除文件（用于文件管理页批量删除，统一走 byUrl 清理路径）
// 并行调用 delFileByUrl，任一失败不阻断其余；返回每个 URL 的结果
export function delFilesByUrl(fileUrls) {
  if (!Array.isArray(fileUrls) || fileUrls.length === 0) {
    return Promise.resolve([])
  }
  return Promise.all(
    fileUrls.map(url =>
      delFileByUrl(url)
        .then(res => ({ url, ok: true, res }))
        .catch(err => ({ url, ok: false, err }))
    )
  )
}

// 批量删除文件
export function delFiles(ids) {
  return request({
    url: '/system/file/' + ids,
    method: 'delete'
  })
}
