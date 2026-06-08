import {httpDelete, httpGet, httpPost} from './client';
import type {
    Bookmark,
    BookmarkListParams,
    CheckBookmarkParams,
    CheckBookmarkResponse,
    CreateBookmarkParams,
    DeleteBookmarkParams,
    PaginationResponse,
} from '@/types/api';

// 获取收藏列表
export const getBookmarkList = (params?: BookmarkListParams) => {
    return httpGet<PaginationResponse<Bookmark>>('/portal/bookmark/list', params);
};

// 创建收藏（保留旧接口兼容）
export const createBookmark = (params: CreateBookmarkParams) => {
    return httpPost<Bookmark>('/portal/bookmark', params);
};

// 删除收藏（保留旧接口兼容）
export const deleteBookmark = (params: DeleteBookmarkParams) => {
    return httpDelete(`/portal/bookmark/${params.id}`);
};

// 检查是否已收藏（保留旧接口兼容）
export const checkBookmark = (params: CheckBookmarkParams) => {
    return httpGet<CheckBookmarkResponse>('/portal/bookmark/check', params);
};

// 别名函数
export const addBookmark = createBookmark;
export const removeBookmark = deleteBookmark;

// ==================== 新的统一接口 ====================

// 切换收藏/取消收藏 - 行业标准接口
export interface ToggleBookmarkResponse {
    isBookmarked: boolean;
    articleId: number;
}

export const toggleBookmark = (articleId: string) => {
    return httpPost<ToggleBookmarkResponse>(`/portal/bookmark/${articleId}/toggle`);
};

// 检查是否已收藏 - 新接口
export interface CheckBookmarkStatusResponse {
    isBookmarked: boolean;
}

export const checkBookmarkStatus = (articleId: string) => {
    return httpGet<CheckBookmarkStatusResponse>(`/portal/bookmark/${articleId}/check`);
};
