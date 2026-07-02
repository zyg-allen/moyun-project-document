import { httpGet, httpPost, httpDelete, httpGetList } from './client';
import type {
    FollowUserParams,
    UnfollowUserParams,
    CheckFollowParams,
    CheckFollowResponse,
    PaginationResponse,
    FollowUserItem,
} from '@/types/api';

// 关注用户
export const followUser = (params: FollowUserParams) => {
    return httpPost(`/portal/follow/${params.userId}`);
};

// 取消关注
export const unfollowUser = (params: UnfollowUserParams) => {
    return httpDelete(`/portal/follow/${params.userId}`);
};

// 检查是否已关注
export const checkFollow = (params: CheckFollowParams) => {
    return httpGet<CheckFollowResponse>(`/portal/follow/check/${params.userId}`);
};

// 关注列表（我关注的人）
export const getFollowingList = (
    userId: string | number,
    params?: { pageNum?: number; pageSize?: number }
) => {
    return httpGetList<FollowUserItem>(`/portal/follow/${userId}/following`, params);
};

// 粉丝列表（关注我的人）
export const getFollowerList = (
    userId: string | number,
    params?: { pageNum?: number; pageSize?: number }
) => {
    return httpGetList<FollowUserItem>(`/portal/follow/${userId}/followers`, params);
};

// 兼容别名（部分代码使用 getFollowersList）
export const getFollowersList = getFollowerList;

export type { PaginationResponse, FollowUserItem };
