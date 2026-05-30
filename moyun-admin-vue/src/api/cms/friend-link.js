import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

export function listFriendLink(query) {
  return request({
    url: '/cms/friend-link/list',
    method: 'get',
    params: query
  })
}

export function getFriendLink(id) {
  return request({
    url: '/cms/friend-link/' + parseStrEmpty(id),
    method: 'get'
  })
}

export function addFriendLink(data) {
  return request({
    url: '/cms/friend-link',
    method: 'post',
    data: data
  })
}

export function updateFriendLink(data) {
  return request({
    url: '/cms/friend-link',
    method: 'put',
    data: data
  })
}

export function delFriendLink(id) {
  return request({
    url: '/cms/friend-link/' + id,
    method: 'delete'
  })
}
