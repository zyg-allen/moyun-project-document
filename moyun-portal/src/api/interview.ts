import { httpGet, httpGetList } from './client';
import type {
  InterviewCategory,
  InterviewQuestion,
  InterviewExperience,
  ResumeTemplate,
  InterviewHomeResponse,
  QuestionListParams,
  PaginationResponse,
} from '@/types/api';

// 获取面试指南首页数据
export const getInterviewHome = () => {
  return httpGet<InterviewHomeResponse>('/portal/interview/home');
};

// 获取题目列表
export const getQuestionList = (params?: QuestionListParams) => {
  return httpGet<{ list: InterviewQuestion[]; total: number }>('/portal/interview/questions', params);
};

// 获取题目详情
export const getQuestionDetail = (questionId: string) => {
  return httpGet<InterviewQuestion>(`/portal/interview/questions/${questionId}`);
};
