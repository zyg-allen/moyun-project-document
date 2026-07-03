import { httpGet, httpGetList } from './client';
import type {
  InterviewCompanyVO,
  InterviewQuestionVO,
  InterviewQuestionQuery,
  InterviewExperienceVO,
  InterviewExperienceQuery,
} from '@/types/api';

/**
 * 公司主页详情（公开）
 * GET /portal/interview/company/{id}
 */
export const getCompanyDetail = (id: string | number) => {
  return httpGet<InterviewCompanyVO>(`/portal/interview/company/${id}`);
};

/**
 * 公司下相关题目列表（公开，分页）
 * 复用 GET /portal/interview/question/list?companyId=xxx
 */
export const getCompanyQuestions = (
  companyId: string | number,
  params?: Omit<InterviewQuestionQuery, 'companyId'>
) => {
  return httpGetList<InterviewQuestionVO>('/portal/interview/question/list', {
    ...params,
    companyId,
  });
};

/**
 * 公司相关面经列表（公开，分页）
 * 复用 GET /portal/interview/experience/list?company=xxx
 */
export const getCompanyExperiences = (
  companyName: string,
  params?: Omit<InterviewExperienceQuery, 'company'>
) => {
  return httpGetList<InterviewExperienceVO>('/portal/interview/experience/list', {
    ...params,
    company: companyName,
  });
};
