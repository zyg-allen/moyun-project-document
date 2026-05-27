import { httpGet, httpPost, httpDelete } from './client';
import type {
    Bookmark,
    BookmarkListParams,
    CreateBookmarkParams,
    DeleteBookmarkParams,
    CheckBookmarkParams,
    CheckBookmarkResponse,
    PaginationResponse,
} from '@/types/api';

// 获取收藏列表
export const getBookmarkList = (params?: BookmarkListParams) => {
    return httpGet<PaginationResponse<Bookmark>>('/portal/bookmark/list', params);
};

// 创建收藏
export const createBookmark = (params: CreateBookmarkParams) => {
    return httpPost<Bookmark>('/portal/bookmark', params);
};

// 删除收藏
export const deleteBookmark = (params: DeleteBookmarkParams) => {
    return httpDelete(`/portal/bookmark/${params.id}`);
};

// 检查是否已收藏
export const checkBookmark = (params: CheckBookmarkParams) => {
    return httpGet<CheckBookmarkResponse>('/portal/bookmark/check', params);
};

// 别名函数
export const addBookmark = createBookmark;
export const removeBookmark = deleteBookmark;
