import { httpGet, httpPost } from './client';
// =====================================================
// 读书空间 - 首页
// =====================================================
// 获取读书空间首页数据
export const getReadingHome = () => {
    return httpGet('/portal/reading/home');
};
// =====================================================
// 读书空间 - 书籍
// =====================================================
// 获取书籍详情
export const getBookDetail = (bookId) => {
    return httpGet(`/portal/reading/books/${bookId}`);
};
// =====================================================
// 读书空间 - 书单
// =====================================================
// 获取书单详情（包含书籍列表）
export const getBookListDetail = (listId) => {
    return httpGet(`/portal/reading/book-lists/${listId}`);
};
// 书单收藏（toggle，返回最新收藏状态）
export const toggleBookListBookmark = (listId) => {
    return httpPost(`/portal/reading/book-lists/${listId}/bookmark`);
};
// 查询书单收藏状态
export const checkBookListBookmark = (listId) => {
    return httpGet(`/portal/reading/book-lists/${listId}/bookmark`);
};
