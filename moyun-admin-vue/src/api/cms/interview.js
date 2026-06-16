import request from '@/utils/request';

// ==================== 题目管理 ====================

// 查询题目分页列表
export function listInterviewQuestion(query) {
  return request({
    url: '/cms/interview/question/list',
    method: 'get',
    params: query
  });
}

// 查询题目详情
export function getInterviewQuestion(id) {
  return request({
    url: '/cms/interview/question/' + id,
    method: 'get'
  });
}

// 新增题目
export function addInterviewQuestion(data) {
  return request({
    url: '/cms/interview/question',
    method: 'post',
    data: data
  });
}

// 修改题目
export function updateInterviewQuestion(data) {
  return request({
    url: '/cms/interview/question',
    method: 'put',
    data: data
  });
}

// 批量删除题目
export function delInterviewQuestion(ids) {
  return request({
    url: '/cms/interview/question',
    method: 'delete',
    data: ids
  });
}

// ==================== 分类管理 ====================

// 查询分类列表
export function listInterviewCategory(query) {
  return request({
    url: '/cms/interview/category/list',
    method: 'get',
    params: query
  });
}

// 查询分类详情
export function getInterviewCategory(id) {
  return request({
    url: '/cms/interview/category/' + id,
    method: 'get'
  });
}

// 新增分类
export function addInterviewCategory(data) {
  return request({
    url: '/cms/interview/category',
    method: 'post',
    data: data
  });
}

// 修改分类
export function updateInterviewCategory(data) {
  return request({
    url: '/cms/interview/category',
    method: 'put',
    data: data
  });
}

// 删除分类
export function delInterviewCategory(ids) {
  return request({
    url: '/cms/interview/category/' + ids,
    method: 'delete'
  });
}

// ==================== 面经管理 ====================

// 查询面经分页列表
export function listInterviewExperience(query) {
  return request({
    url: '/cms/interview/experience/list',
    method: 'get',
    params: query
  });
}

// 查询面经详情
export function getInterviewExperience(id) {
  return request({
    url: '/cms/interview/experience/' + id,
    method: 'get'
  });
}

// 审核面经
export function auditInterviewExperience(data) {
  return request({
    url: '/cms/interview/experience/audit',
    method: 'put',
    data: data
  });
}

// 面经置顶
export function topInterviewExperience(data) {
  return request({
    url: '/cms/interview/experience/top',
    method: 'put',
    data: data
  });
}

// 新增面经
export function addInterviewExperience(data) {
  return request({
    url: '/cms/interview/experience',
    method: 'post',
    data: data
  });
}

// 修改面经
export function updateInterviewExperience(data) {
  return request({
    url: '/cms/interview/experience',
    method: 'put',
    data: data
  });
}

// 删除面经
export function delInterviewExperience(id) {
  return request({
    url: '/cms/interview/experience/' + id,
    method: 'delete'
  });
}

// ==================== 评论管理 ====================

// 查询评论分页列表
export function listInterviewComment(query) {
  return request({
    url: '/cms/interview/comment/list',
    method: 'get',
    params: query
  });
}

// 审核评论
export function auditInterviewComment(data) {
  return request({
    url: '/cms/interview/comment/audit',
    method: 'put',
    data: data
  });
}

// 删除评论
export function delInterviewComment(id) {
  return request({
    url: '/cms/interview/comment/' + id,
    method: 'delete'
  });
}

// ==================== 简历模板管理 ====================

// 查询简历模板分页列表
export function listInterviewResume(query) {
  return request({
    url: '/cms/interview/resume/list',
    method: 'get',
    params: query
  });
}

// 查询简历模板详情
export function getInterviewResume(id) {
  return request({
    url: '/cms/interview/resume/' + id,
    method: 'get'
  });
}

// 新增简历模板
export function addInterviewResume(data) {
  return request({
    url: '/cms/interview/resume',
    method: 'post',
    data: data
  });
}

// 修改简历模板
export function updateInterviewResume(data) {
  return request({
    url: '/cms/interview/resume',
    method: 'put',
    data: data
  });
}

// 删除简历模板
export function delInterviewResume(ids) {
  return request({
    url: '/cms/interview/resume/' + ids,
    method: 'delete'
  });
}

// ==================== 公司标签管理 ====================

// 查询公司列表
export function listInterviewCompany(query) {
  return request({
    url: '/cms/interview/company/list',
    method: 'get',
    params: query
  });
}

// 查询公司详情
export function getInterviewCompany(id) {
  return request({
    url: '/cms/interview/company/' + id,
    method: 'get'
  });
}

// 新增公司
export function addInterviewCompany(data) {
  return request({
    url: '/cms/interview/company',
    method: 'post',
    data: data
  });
}

// 修改公司
export function updateInterviewCompany(data) {
  return request({
    url: '/cms/interview/company',
    method: 'put',
    data: data
  });
}

// 删除公司
export function delInterviewCompany(ids) {
  return request({
    url: '/cms/interview/company/' + ids,
    method: 'delete'
  });
}
