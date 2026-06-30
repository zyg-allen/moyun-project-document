import type { InterviewCategoryVO, InterviewQuestionVO, InterviewQuestionDetailVO, InterviewQuestionQuery, InterviewSubmissionVO, InterviewExperienceVO, InterviewExperienceQuery, InterviewCommentVO, InterviewResumeTemplateVO, InterviewResumeTemplateQuery, InterviewHomeDataVO, TagVO, PageResult } from '@/types/api';
export declare const getInterviewHome: () => Promise<import("./client").ApiResponse<InterviewHomeDataVO>>;
export declare const getInterviewCategoryList: () => Promise<import("./client").ApiResponse<InterviewCategoryVO[]>>;
export declare const getQuestionList: (params?: InterviewQuestionQuery) => Promise<import("./client").ApiResponse<import("./client").PaginationResponse<InterviewQuestionVO>>>;
export declare const getQuestionDetail: (questionId: string | number) => Promise<import("./client").ApiResponse<InterviewQuestionDetailVO>>;
export declare const submitAnswer: (questionId: string | number, body: {
    code?: string;
    content?: string;
    language?: string;
    answerType?: "code" | "text" | "design";
    note?: string;
}) => Promise<import("./client").ApiResponse<InterviewSubmissionVO>>;
export declare const toggleQuestionLike: (questionId: string | number) => Promise<import("./client").ApiResponse<{
    liked: boolean;
    likeCount: number;
}>>;
export declare const toggleQuestionBookmark: (questionId: string | number, note?: string) => Promise<import("./client").ApiResponse<{
    bookmarked: boolean;
}>>;
export declare const getFeaturedNotes: (questionId: string | number) => Promise<import("./client").ApiResponse<InterviewSubmissionVO[]>>;
export declare const getExperienceList: (params?: InterviewExperienceQuery) => Promise<import("./client").ApiResponse<import("./client").PaginationResponse<InterviewExperienceVO>>>;
export declare const getExperienceDetail: (experienceId: string | number) => Promise<import("./client").ApiResponse<InterviewExperienceVO>>;
export declare const toggleExperienceLike: (experienceId: string | number) => Promise<import("./client").ApiResponse<{
    liked: boolean;
    likeCount: number;
}>>;
export declare const getCommentList: (params: {
    experienceId: string | number;
    pageNum?: number;
    pageSize?: number;
}) => Promise<import("./client").ApiResponse<import("./client").PaginationResponse<InterviewCommentVO>>>;
export declare const publishComment: (body: {
    experienceId: string | number;
    content: string;
    parentId?: string | number;
    replyToUserId?: string | number;
}) => Promise<import("./client").ApiResponse<InterviewCommentVO>>;
export declare const toggleCommentLike: (commentId: string | number) => Promise<import("./client").ApiResponse<{
    liked: boolean;
    likeCount: number;
}>>;
export declare const getResumeTemplateList: (params?: InterviewResumeTemplateQuery) => Promise<import("./client").ApiResponse<import("./client").PaginationResponse<InterviewResumeTemplateVO>>>;
export declare const downloadResumeTemplate: (templateId: string | number) => Promise<import("./client").ApiResponse<{
    downloadUrl: string;
}>>;
export declare const toggleResumeTemplateLike: (templateId: string | number) => Promise<import("./client").ApiResponse<{
    liked: boolean;
    likeCount: number;
}>>;
export declare const bindTagsToEntity: (data: {
    entityType: string;
    entityId: string | number;
    tagIds?: (string | number)[];
    tagNames?: string[];
    module?: string;
}) => Promise<import("./client").ApiResponse<void>>;
export declare const getHotTags: (module?: string, limit?: number) => Promise<import("./client").ApiResponse<TagVO[]>>;
export declare const searchTags: (keyword: string) => Promise<import("./client").ApiResponse<TagVO[]>>;
export type { PageResult, TagVO };
