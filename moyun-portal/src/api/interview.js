import { httpGet, httpPost, httpGetList } from './client';
// ==================== 首页数据 ====================
export const getInterviewHome = () => {
    return httpGet('/portal/interview/home');
};
// ==================== 分类 ====================
export const getInterviewCategoryList = () => {
    return httpGet('/portal/interview/category/list');
};
// ==================== 题目 ====================
export const getQuestionList = (params) => {
    return httpGetList('/portal/interview/question/list', params);
};
export const getQuestionDetail = (questionId) => {
    return httpGet(`/portal/interview/question/${questionId}`);
};
export const submitAnswer = (questionId, body) => {
    return httpPost(`/portal/interview/question/${questionId}/submit`, body);
};
// ==================== 题目点赞 ====================
export const toggleQuestionLike = (questionId) => {
    return httpPost(`/portal/interview/question/${questionId}/like`);
};
// ==================== 题目收藏 ====================
export const toggleQuestionBookmark = (questionId, note) => {
    return httpPost(`/portal/interview/question/${questionId}/bookmark`, note !== undefined ? { note } : {});
};
// ==================== 精选笔记 ====================
// 查询某题目的精选笔记列表（公开接口）
export const getFeaturedNotes = (questionId) => {
    return httpGet(`/portal/interview/question/${questionId}/featured-notes`);
};
// ==================== 面经 ====================
export const getExperienceList = (params) => {
    return httpGetList('/portal/interview/experience/list', params);
};
export const getExperienceDetail = (experienceId) => {
    return httpGet(`/portal/interview/experience/${experienceId}`);
};
export const toggleExperienceLike = (experienceId) => {
    return httpPost(`/portal/interview/experience/${experienceId}/like`);
};
// ==================== 面经评论 ====================
export const getCommentList = (params) => {
    return httpGetList('/portal/interview/comment/list', params);
};
export const publishComment = (body) => {
    return httpPost('/portal/interview/comment', body);
};
export const toggleCommentLike = (commentId) => {
    return httpPost(`/portal/interview/comment/${commentId}/like`);
};
// ==================== 简历模板 ====================
export const getResumeTemplateList = (params) => {
    return httpGetList('/portal/interview/resume/list', params);
};
export const downloadResumeTemplate = (templateId) => {
    return httpGet(`/portal/interview/resume/${templateId}/download`);
};
export const toggleResumeTemplateLike = (templateId) => {
    return httpPost(`/portal/interview/resume/${templateId}/like`);
};
// ==================== 通用标签系统 ====================
export const bindTagsToEntity = (data) => {
    return httpPost('/portal/tag/bind', data);
};
export const getHotTags = (module, limit = 20) => {
    return httpGet(`/portal/tag/hot?${[`module=${encodeURIComponent(module ?? '')}`, `limit=${limit}`]
        .filter((x) => x)
        .join('&')}`);
};
export const searchTags = (keyword) => {
    return httpGet(`/portal/tag/search?keyword=${encodeURIComponent(keyword)}`);
};
