import { httpGetList, httpPost, httpDelete } from './client';
import type {
  Comment,
  CommentListParams,
  CreateCommentParams,
  DeleteCommentParams,
  PaginationResponse,
} from '@/types/api';

// 获取评论列表
export const getCommentList = (params: CommentListParams) => {
  return httpGetList<Comment>('/portal/comment/list', params);
};

// 创建评论
export const createComment = (params: CreateCommentParams) => {
  return httpPost<Comment>('/portal/comment', params);
};

// 删除评论
export const deleteComment = (params: DeleteCommentParams) => {
  return httpDelete(`/portal/comment/${params.id}`);
};

// 评论点赞
export const likeComment = (commentId: string) => {
  return httpPost(`/portal/comment/${commentId}/like`);
};

// 取消评论点赞
export const unlikeComment = (commentId: string) => {
  return httpDelete(`/portal/comment/${commentId}/like`);
};

// 别名函数
export const addComment = createComment;
