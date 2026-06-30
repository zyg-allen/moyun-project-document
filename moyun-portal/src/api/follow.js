import { httpGet, httpPost, httpDelete } from './client';
// 关注用户
export const followUser = (params) => {
    return httpPost(`/portal/follow/${params.userId}`);
};
// 取消关注
export const unfollowUser = (params) => {
    return httpDelete(`/portal/follow/${params.userId}`);
};
// 检查是否已关注
export const checkFollow = (params) => {
    return httpGet(`/portal/follow/check/${params.userId}`);
};
