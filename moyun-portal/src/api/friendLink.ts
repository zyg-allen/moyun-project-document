import { httpGetList } from './client';
import type { ApiResponse, PaginationResponse } from '@/types/api';

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
export const getFriendLinks = async (): Promise<ApiResponse<PaginationResponse<FriendLink>>> => {
    return httpGetList<FriendLink>('/portal/friend-link/list');
};
