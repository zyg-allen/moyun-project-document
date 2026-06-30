import { httpGetList } from './client';
// 获取友情链接列表
export const getFriendLinks = async () => {
    return httpGetList('/portal/friend-link/list');
};
