import { httpGet, httpPost, httpGetList } from './client';
import type {
  InterviewCategoryVO,
  InterviewCompanyVO,
  InterviewQuestionVO,
  InterviewQuestionDetailVO,
  InterviewQuestionQuery,
  InterviewSubmissionVO,
  InterviewExperienceVO,
  InterviewExperienceQuery,
  InterviewCommentVO,
  InterviewResumeTemplateVO,
  InterviewResumeTemplateQuery,
  InterviewBookmarkVO,
  InterviewHomeDataVO,
  TagVO,
  PageResult,
} from '@/types/api';

// ==================== 首页数据 ====================

export const getInterviewHome = () => {
  return httpGet<InterviewHomeDataVO>('/portal/interview/home');
};

// ==================== 分类 ====================

export const getInterviewCategoryList = () => {
  return httpGet<InterviewCategoryVO[]>('/portal/interview/category/list');
};

// ==================== 公司 ====================

export const getCompanyList = () => {
  return httpGet<InterviewCompanyVO[]>('/portal/interview/company/list');
};

export const getCompanyDetail = (companyId: string | number) => {
  return httpGet<InterviewCompanyVO>(`/portal/interview/company/${companyId}`);
};

export const getCompanyQuestions = (
  companyId: string | number,
  params?: InterviewQuestionQuery
) => {
  return httpGetList<InterviewQuestionVO>(
    `/portal/interview/company/${companyId}/questions`,
    params
  );
};

// ==================== 题目 ====================

export const getQuestionList = (params?: InterviewQuestionQuery) => {
  return httpGetList<InterviewQuestionVO>('/portal/interview/question/list', params);
};

export const getQuestionDetail = (questionId: string | number) => {
  return httpGet<InterviewQuestionDetailVO>(`/portal/interview/question/${questionId}`);
};

export const submitAnswer = (
  questionId: string | number,
  body: {
    code?: string;
    content?: string;
    language?: string;
    answerType?: 'code' | 'text' | 'design';
    note?: string;
  }
) => {
  return httpPost<InterviewSubmissionVO>(
    `/portal/interview/question/${questionId}/submit`,
    body
  );
};

// ==================== 题目点赞 ====================

export const toggleQuestionLike = (questionId: string | number) => {
  return httpPost<{ liked: boolean; likeCount: number }>(
    `/portal/interview/question/${questionId}/like`
  );
};

// ==================== 题目收藏 ====================

export const toggleQuestionBookmark = (
  questionId: string | number,
  note?: string
) => {
  return httpPost<{ bookmarked: boolean }>(
    `/portal/interview/question/${questionId}/bookmark`,
    note !== undefined ? { note } : {}
  );
};

export const getMyBookmarkList = (params?: {
  pageNum?: number;
  pageSize?: number;
}) => {
  return httpGetList<InterviewBookmarkVO>('/portal/interview/bookmark/list', params);
};

// ==================== 精选笔记 ====================

// 查询某题目的精选笔记列表（公开接口）
export const getFeaturedNotes = (questionId: string | number) => {
  return httpGet<InterviewSubmissionVO[]>(
    `/portal/interview/question/${questionId}/featured-notes`
  );
};

// ==================== 面经 ====================

export const getExperienceList = (params?: InterviewExperienceQuery) => {
  return httpGetList<InterviewExperienceVO>('/portal/interview/experience/list', params);
};

export const getExperienceDetail = (experienceId: string | number) => {
  return httpGet<InterviewExperienceVO>(
    `/portal/interview/experience/${experienceId}`
  );
};

export const publishExperience = (body: {
  title: string;
  company: string;
  position?: string;
  year?: number;
  month?: number;
  summary?: string;
  content?: string;
  coverImage?: string;
  tags?: string[];
}) => {
  return httpPost<InterviewExperienceVO>('/portal/interview/experience', body);
};

export const toggleExperienceLike = (experienceId: string | number) => {
  return httpPost<{ liked: boolean; likeCount: number }>(
    `/portal/interview/experience/${experienceId}/like`
  );
};

// ==================== 面经评论 ====================

export const getCommentList = (params: {
  experienceId: string | number;
  pageNum?: number;
  pageSize?: number;
}) => {
  return httpGetList<InterviewCommentVO>('/portal/interview/comment/list', params);
};

export const publishComment = (body: {
  experienceId: string | number;
  content: string;
  parentId?: string | number;
  replyToUserId?: string | number;
}) => {
  return httpPost<InterviewCommentVO>('/portal/interview/comment', body);
};

export const toggleCommentLike = (commentId: string | number) => {
  return httpPost<{ liked: boolean; likeCount: number }>(
    `/portal/interview/comment/${commentId}/like`
  );
};

// ==================== 简历模板 ====================

export const getResumeTemplateList = (params?: InterviewResumeTemplateQuery) => {
  return httpGetList<InterviewResumeTemplateVO>(
    '/portal/interview/resume/list',
    params
  );
};

export const getResumeTemplateDetail = (templateId: string | number) => {
  return httpGet<InterviewResumeTemplateVO>(`/portal/interview/resume/${templateId}`);
};

export const downloadResumeTemplate = (templateId: string | number) => {
  return httpGet<{ downloadUrl: string }>(
    `/portal/interview/resume/${templateId}/download`
  );
};

export const toggleResumeTemplateLike = (templateId: string | number) => {
  return httpPost<{ liked: boolean; likeCount: number }>(
    `/portal/interview/resume/${templateId}/like`
  );
};

// ==================== 通用标签系统 ====================

export const getTagsByEntity = (entityType: string, entityId: string | number) => {
  return httpGet<TagVO[]>(`/portal/tag/entity/${entityType}/${entityId}`);
};

export const bindTagsToEntity = (data: {
  entityType: string;
  entityId: string | number;
  tagIds?: (string | number)[];
  tagNames?: string[];
  module?: string;
}) => {
  return httpPost<void>('/portal/tag/bind', data);
};

export const getHotTags = (module?: string, limit = 20) => {
  return httpGet<TagVO[]>(
    `/portal/tag/hot?${[`module=${encodeURIComponent(module ?? '')}`, `limit=${limit}`]
      .filter((x) => x)
      .join('&')}`
  );
};

export const searchTags = (keyword: string) => {
  return httpGet<TagVO[]>(`/portal/tag/search?keyword=${encodeURIComponent(keyword)}`);
};

// 兼容类型别名（向后兼容）
export type { PageResult, TagVO };
