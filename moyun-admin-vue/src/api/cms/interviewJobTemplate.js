import request from '@/utils/request';

// ==================== 岗位模板管理（v11.x 智能出题） ====================
// 说明：
//   - JD/关键词（LLM 提取）/出题权重/关联题目管理
//   - 权限前缀 cms:interview:jobTemplate: list/query/create/update/remove

// 查询岗位模板分页列表
export function listJobTemplate(query) {
  return request({
    url: '/cms/interview/jobTemplate/list',
    method: 'get',
    params: query
  });
}

// 查询岗位模板详情
export function getJobTemplate(id) {
  return request({
    url: '/cms/interview/jobTemplate/' + id,
    method: 'get'
  });
}

// 新增岗位模板
export function addJobTemplate(data) {
  return request({
    url: '/cms/interview/jobTemplate',
    method: 'post',
    data: data
  });
}

// 修改岗位模板
export function updateJobTemplate(data) {
  return request({
    url: '/cms/interview/jobTemplate',
    method: 'put',
    data: data
  });
}

// 删除岗位模板
export function delJobTemplate(id) {
  return request({
    url: '/cms/interview/jobTemplate/' + id,
    method: 'delete'
  });
}

// JD 关键词 LLM 提取（失败自动回退规则分词），返回字符串数组
export function extractJobTemplateKeywords(jdText) {
  return request({
    url: '/cms/interview/jobTemplate/extract-keywords',
    method: 'post',
    data: { jdText }
  });
}

// 获取模板已关联题目ID列表
export function getBoundQuestionIds(id) {
  return request({
    url: '/cms/interview/jobTemplate/' + id + '/questions',
    method: 'get'
  });
}

// 全量覆盖关联题目（data: 题目ID数组）
export function bindJobTemplateQuestions(id, questionIds) {
  return request({
    url: '/cms/interview/jobTemplate/' + id + '/questions',
    method: 'post',
    data: questionIds
  });
}