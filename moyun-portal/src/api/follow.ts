import { httpGet, httpPost, httpDelete } from './client';
import type {
  FollowUserParams,
  UnfollowUserParams,
  CheckFollowParams,
  CheckFollowResponse,
  FollowerListParams,
  FollowingListParams,
  Follower,
  Following,
  PaginationResponse,
} from '@/types/api';

// 关注用户
export const followUser = (params: FollowUserParams) => {
  return httpPost(`/follow/${params.userId}`);
};

// 取消关注
export const unfollowUser = (params: UnfollowUserParams) => {
  return httpDelete(`/follow/${params.userId}`);
};

// 检查是否已关注
export const checkFollow = (params: CheckFollowParams) => {
  return httpGet<CheckFollowResponse>(`/follow/check/${params.userId}`);
};

// 获取粉丝列表
export const getFollowerList = (params: FollowerListParams) => {
  return httpGet<PaginationResponse<Follower>>(`/follow/${params.userId}/followers`, {
    page: params.page,
    pageSize: params.pageSize,
  });
};

// 获取关注列表
export const getFollowingList = (params: FollowingListParams) => {
  return httpGet<PaginationResponse<Following>>(`/follow/${params.userId}/following`, {
    page: params.page,
    pageSize: params.pageSize,
  });
};
