export type ReportType = 'spam' | 'inappropriate' | 'infringement' | 'fraud' | 'other';
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
export declare const submitReport: (params: SubmitReportParams) => Promise<import("./client").ApiResponse<string>>;
/**
 * 提交反馈
 * POST /portal/feedback/submit
 * 需要登录（PortalJwtAuthenticationTokenFilter 鉴权）
 */
export declare const submitFeedback: (params: SubmitFeedbackParams) => Promise<import("./client").ApiResponse<string>>;
