import { httpGet, httpPost, httpDelete } from './client';
import type {
    FollowUserParams,
    UnfollowUserParams,
    CheckFollowParams,
    CheckFollowResponse,
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
