import { httpGet } from './client';
import type { ApiResponse } from '@/types/api';

// 友情链接类型
export interface FriendLink {
    id: string;
    name: string;
    url: string;
    description?: string;
    logo?: string;
    sort: number;
    status: 'active' | 'inactive';
    createdAt: string;
    updatedAt: string;
}

// 获取友情链接列表
export const getFriendLinks = async (): Promise<ApiResponse<FriendLink[]>> => {
    return httpGet<FriendLink[]>('/portal/friend-link/list');
};
