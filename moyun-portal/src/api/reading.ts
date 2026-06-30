import { httpGet, httpPost } from './client';
import type {
  Book,
  BookList,
  BookQuote,
  ReadingHomeResponse,
} from '@/types/api';

// =====================================================
// 读书空间 - 首页
// =====================================================

// 获取读书空间首页数据
export const getReadingHome = () => {
  return httpGet<ReadingHomeResponse>('/portal/reading/home');
};

// =====================================================
// 读书空间 - 书籍
// =====================================================

// 获取书籍详情
export const getBookDetail = (bookId: string | number) => {
  return httpGet<{ book: Book; quotes: BookQuote[] }>(`/portal/reading/books/${bookId}`);
};

// =====================================================
// 读书空间 - 书单
// =====================================================

// 获取书单详情（包含书籍列表）
export const getBookListDetail = (listId: string | number) => {
  return httpGet<{ bookList: BookList; books: Book[] }>(`/portal/reading/book-lists/${listId}`);
};

// 书单收藏（toggle，返回最新收藏状态）
export const toggleBookListBookmark = (listId: string | number) => {
  return httpPost<{ bookmarked: boolean; message?: string }>(`/portal/reading/book-lists/${listId}/bookmark`);
};

// 查询书单收藏状态
export const checkBookListBookmark = (listId: string | number) => {
  return httpGet<{ bookmarked: boolean }>(`/portal/reading/book-lists/${listId}/bookmark`);
};
