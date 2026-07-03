import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

// 查询共读活动列表
export function listBookClub(query) {
  return request({
    url: '/cms/reading/club/list',
    method: 'get',
    params: query
  })
}

// 查询共读活动详情
export function getBookClub(id) {
  return request({
    url: '/cms/reading/club/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增共读活动
export function addBookClub(data) {
  return request({
    url: '/cms/reading/club',
    method: 'post',
    data: data
  })
}

// 修改共读活动
export function updateBookClub(data) {
  return request({
    url: '/cms/reading/club',
    method: 'put',
    data: data
  })
}

// 删除共读活动（支持批量，ids 逗号分隔）
export function delBookClub(ids) {
  return request({
    url: '/cms/reading/club/' + ids,
    method: 'delete'
  })
}

// 上下架共读活动（更新状态）
export function changeBookClubStatus(id, status) {
  return request({
    url: '/cms/reading/club/' + id + '/status',
    method: 'put',
    data: { status: status }
  })
}
