import { httpGet, httpPost } from './client';
import type {
  Comment,
  CreateCommentParams,
} from '@/types/api';

// 获取文章的评论列表（含回复）- 分页版本
export const getArticleComments = (articleId: string, pageNum: number = 1, pageSize: number = 20) => {
  return httpGet<any>(`/portal/comment/article/${articleId}?pageNum=${pageNum}&pageSize=${pageSize}`);
};

// 创建评论
export const createComment = (params: CreateCommentParams) => {
  return httpPost<Comment>('/portal/comment', params);
};

// 评论点赞（幂等 toggle，后端切换点赞状态，返回最新点赞数和 liked 状态）
export const likeComment = (commentId: string) => {
  return httpPost(`/portal/comment/${commentId}/like`);
};

// 别名函数
export const addComment = createComment;
