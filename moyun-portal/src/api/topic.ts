import { httpGet, httpPost, httpPut, httpDelete, httpGetList } from './client';
import type {
  Topic,
  TopicPost,
  TopicComment,
} from '@/types/api';

// ==================== 话题 ====================

/**
 * 话题列表（公开，分页）
 * GET /portal/topic/list
 */
export const getTopicList = (params: {
  pageNum: number;
  pageSize: number;
  sort?: string;
  keyword?: string;
}) => {
  return httpGetList<Topic>('/portal/topic/list', params);
};

/**
 * 话题详情（公开）
 * GET /portal/topic/{id}
 */
export const getTopicDetail = (id: number | string) => {
  return httpGet<Topic>(`/portal/topic/${id}`);
};

/**
 * 发起话题（需登录 + 创作者认证）
 * POST /portal/topic/save
 */
export const createTopic = (data: {
  title: string;
  description?: string;
  cover?: string;
}) => {
  return httpPost<Topic>('/portal/topic/save', data);
};

/**
 * 编辑话题（需登录，仅本人）
 * PUT /portal/topic/{id}
 */
export const updateTopic = (
  id: number | string,
  data: {
    title?: string;
    description?: string;
    cover?: string;
  }
) => {
  return httpPut<Topic>(`/portal/topic/${id}`, data);
};

/**
 * 删除话题（需登录，仅本人或管理员）
 * DELETE /portal/topic/{id}
 */
export const deleteTopic = (id: number | string) => {
  return httpDelete<number>(`/portal/topic/${id}`);
};

/**
 * 话题点赞（幂等 toggle，后端切换点赞状态）
 * POST /portal/topic/{id}/like
 */
export const toggleTopicLike = (id: number | string) => {
  return httpPost<{ isLiked: boolean; likeCount: number }>(`/portal/topic/${id}/like`);
};

// ==================== 话题观点 ====================

/**
 * 话题下的观点列表（公开，分页，按楼层升序）
 * GET /portal/topic/{topicId}/posts
 */
export const getTopicPosts = (
  topicId: number | string,
  params: { pageNum: number; pageSize: number }
) => {
  return httpGetList<TopicPost>(`/portal/topic/${topicId}/posts`, params);
};

/**
 * 发表观点（需登录）
 * POST /portal/topic/{topicId}/post
 */
export const createTopicPost = (
  topicId: number | string,
  data: {
    content: string;
    images?: string[];
    parentPostId?: number;
    replyToUserId?: number;
  }
) => {
  return httpPost<TopicPost>(`/portal/topic/${topicId}/post`, data);
};

/**
 * 删除观点（需登录，仅本人或话题发起人）
 * DELETE /portal/topic/post/{postId}
 */
export const deleteTopicPost = (postId: number | string) => {
  return httpDelete<number>(`/portal/topic/post/${postId}`);
};

/**
 * 观点点赞（幂等 toggle）
 * POST /portal/topic/post/{postId}/like
 */
export const toggleTopicPostLike = (postId: number | string) => {
  return httpPost<{ isLiked: boolean; likeCount: number }>(`/portal/topic/post/${postId}/like`);
};

// ==================== 话题评论 ====================

/**
 * 通用评论列表（按 targetType + targetId 查询，分页）
 * GET /portal/topic/comment/list
 */
export const getTopicComments = (params: {
  targetType: string;
  targetId: number | string;
  pageNum: number;
  pageSize: number;
}) => {
  return httpGetList<TopicComment>('/portal/topic/comment/list', params);
};

/**
 * 创建评论（需登录）
 * POST /portal/topic/comment
 */
export const createTopicComment = (data: {
  targetType: string;
  targetId: number | string;
  content: string;
  parentId?: number;
  replyTo?: number;
}) => {
  return httpPost<TopicComment>('/portal/topic/comment', data);
};

/**
 * 删除评论（需登录，仅本人或权限角色）
 * DELETE /portal/topic/comment/{commentId}
 */
export const deleteTopicComment = (commentId: number | string) => {
  return httpDelete<number>(`/portal/topic/comment/${commentId}`);
};

/**
 * 评论点赞（幂等 toggle）
 * POST /portal/topic/comment/{commentId}/like
 */
export const toggleTopicCommentLike = (commentId: number | string) => {
  return httpPost<{ isLiked: boolean; likeCount: number }>(`/portal/topic/comment/${commentId}/like`);
};

// ==================== 我的话题 / 观点 ====================

/**
 * 我发起的话题列表（需登录，分页）
 * GET /portal/topic/my/topics
 */
export const getMyTopics = (params: { pageNum: number; pageSize: number }) => {
  return httpGetList<Topic>('/portal/topic/my/topics', params);
};

/**
 * 我发表的观点列表（需登录，分页）
 * GET /portal/topic/my/posts
 */
export const getMyTopicPosts = (params: { pageNum: number; pageSize: number }) => {
  return httpGetList<TopicPost>('/portal/topic/my/posts', params);
};
