import type { Comment, CreateCommentParams } from '@/types/api';
export declare const getArticleComments: (articleId: string, pageNum?: number, pageSize?: number) => Promise<import("./client").ApiResponse<any>>;
export declare const createComment: (params: CreateCommentParams) => Promise<import("./client").ApiResponse<Comment>>;
export declare const likeComment: (commentId: string) => Promise<import("./client").ApiResponse<unknown>>;
export declare const addComment: (params: CreateCommentParams) => Promise<import("./client").ApiResponse<Comment>>;
