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

// ==================== 题库导入导出（v8.2 通用导入模板） ====================
// 说明：
//   - exportInterviewQuestion：导出当前筛选条件下的题目到 Excel（POST blob）
//   - downloadInterviewQuestionTemplate：下载动态模板（优先 portal_import_template_config 配置）
//   - importInterviewQuestionData：上传 Excel 批量导入，返回 ImportResult
//       { totalRows, successCount, failCount, failRows:[{rowNo,reason,rowData}], msg }
//   - exportInterviewQuestionFailRows：将导入失败行导出 Excel，便于修正后重导
//   - 三按钮均挂 cms:interview:import / cms:interview:export 权限
// ----------------------------------------------------------------

// 导出题目（按筛选条件，POST 触发后端 blob 响应）
export function exportInterviewQuestion(query) {
  return request({
    url: '/cms/interview/question/export',
    method: 'post',
    data: query,
    responseType: 'blob'
  });
}

// 下载导入模板（动态字段配置优先，回退 @Excel 注解）
export function downloadInterviewQuestionTemplate() {
  return request({
    url: '/cms/interview/question/importTemplate',
    method: 'post',
    responseType: 'blob'
  });
}

// 上传题目 Excel 导入（multipart/form-data）
// formData: 包含 file 字段的 FormData 对象
// 返回：AjaxResult<ImportResult>
export function importInterviewQuestionData(formData, onUploadProgress) {
  return request({
    url: '/cms/interview/question/importData',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  });
}

// 下载导入失败行 Excel（POST JSON：failRows 数组）
export function exportInterviewQuestionFailRows(failRows) {
  return request({
    url: '/cms/interview/question/exportFailRows',
    method: 'post',
    data: { failRows: failRows || [] },
    responseType: 'blob'
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

// 查询评论详情
export function getInterviewComment(id) {
  return request({
    url: '/cms/interview/comment/' + id,
    method: 'get'
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

// ==================== 用户简历管理（Admin 只读查看 + 审计） ====================
// 独立权限 system:user:resume，挂在用户管理操作列，跳转独立只读列表页
// 设计：不复用 system:user:edit，权限解耦；后端 @Log 记录审计日志

// Admin 分页查询指定用户的简历列表（只读）
export function listUserResume(userId, query) {
  return request({
    url: '/cms/interview/user-resume/' + userId + '/list',
    method: 'get',
    params: query
  });
}

// Admin 查看指定用户的简历详情（只读）
export function getUserResumeDetail(userId, id) {
  return request({
    url: '/cms/interview/user-resume/' + userId + '/' + id,
    method: 'get'
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

// ==================== 精选笔记管理 ====================

// 查询提交笔记分页列表
export function listInterviewSubmission(query) {
  return request({
    url: '/cms/interview/submission/list',
    method: 'get',
    params: query
  });
}

// 采纳笔记为精选
export function featureSubmission(id) {
  return request({
    url: '/cms/interview/submission/featured',
    method: 'put',
    data: { id }
  });
}

// 取消精选笔记
export function unfeatureSubmission(id) {
  return request({
    url: '/cms/interview/submission/unfeatured',
    method: 'put',
    data: { id }
  });
}

// ==================== 测试用例管理（v6.3 OJ 判题） ====================
// 路径前缀 /portal/admin/ 由核心安全链识别 admin token；
// 原 /portal/judge/admin/** 走门户安全链（仅识别门户用户 token），后台访问会 401

// 查询题目全部用例（含隐藏用例）
export function listTestCase(questionId) {
  return request({
    url: '/portal/admin/judge/cases/' + questionId,
    method: 'get'
  });
}

// 新增测试用例
export function addTestCase(data) {
  return request({
    url: '/portal/admin/judge/cases',
    method: 'post',
    data: data
  });
}

// 修改测试用例
export function updateTestCase(id, data) {
  return request({
    url: '/portal/admin/judge/cases/' + id,
    method: 'put',
    data: data
  });
}

// 删除测试用例
export function delTestCase(id) {
  return request({
    url: '/portal/admin/judge/cases/' + id,
    method: 'delete'
  });
}

// ==================== 岗位模板（v11.x 智能出题·题目归属） ====================

// 查询启用状态的岗位模板分页列表（题目归属选择、列表 id→name 映射用）
// 返回分页对象：{ records: [{ id, name, ... }], total }
export function listJobTemplateSimple() {
  return request({
    url: '/cms/interview/jobTemplate/list',
    method: 'get',
    params: { pageNum: 1, pageSize: 200, status: 'active' }
  });
}
