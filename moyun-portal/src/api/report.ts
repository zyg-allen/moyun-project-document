import { httpPost } from './client';

// 举报类型
export type ReportType = 'spam' | 'inappropriate' | 'infringement' | 'fraud' | 'other';
// 反馈类型
export type FeedbackType = 'suggestion' | 'bug' | 'experience' | 'other';

export interface SubmitReportParams {
  reportType: ReportType;
  targetUrl?: string;
  description: string;
  contact?: string;
  images?: string[];
}

export interface SubmitFeedbackParams {
  feedbackType: FeedbackType;
  subject?: string;
  description: string;
  contact?: string;
}

/**
 * 提交举报
 * POST /portal/report/submit
 * 需要登录（PortalJwtAuthenticationTokenFilter 鉴权）
 */
export const submitReport = (params: SubmitReportParams) => {
  const payload = { ...params };
  // 后端 images 字段为 String（JSON 数组序列化）
  if (Array.isArray(params.images) && params.images.length > 0) {
    payload.images = JSON.stringify(params.images) as any;
  } else {
    delete (payload as any).images;
  }
  return httpPost<string>('/portal/report/submit', payload);
};

/**
 * 提交反馈
 * POST /portal/feedback/submit
 * 需要登录（PortalJwtAuthenticationTokenFilter 鉴权）
 */
export const submitFeedback = (params: SubmitFeedbackParams) => {
  return httpPost<string>('/portal/feedback/submit', params);
};
