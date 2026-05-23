import { httpGet, httpPost, httpDelete } from './client';
import type {
  Comment,
  CommentListParams,
  CreateCommentParams,
  DeleteCommentParams,
  PaginationResponse,
} from '@/types/api';

// 获取评论列表
export const getCommentList = (params: CommentListParams) => {
  return httpGet<PaginationResponse<Comment>>('/comment/list', params);
};

// 创建评论
export const createComment = (params: CreateCommentParams) => {
  return httpPost<Comment>('/comment', params);
};

// 删除评论
export const deleteComment = (params: DeleteCommentParams) => {
  return httpDelete(`/comment/${params.id}`);
};

// 评论点赞
export const likeComment = (commentId: string) => {
  return httpPost(`/comment/${commentId}/like`);
};

// 取消评论点赞
export const unlikeComment = (commentId: string) => {
  return httpDelete(`/comment/${commentId}/like`);
};

// 别名函数
export const addComment = createComment;
