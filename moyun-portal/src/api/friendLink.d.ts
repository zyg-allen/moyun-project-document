import type { ApiResponse, PaginationResponse } from '@/types/api';
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
export declare const getFriendLinks: () => Promise<ApiResponse<PaginationResponse<FriendLink>>>;
