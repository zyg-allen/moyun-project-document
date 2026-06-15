import { httpGet, httpPost } from './client';
import type {
  Book,
  BookList,
  BookQuote,
  ReadingHomeResponse,
  BookListParams,
  PagedResponse,
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

// 获取书籍列表（分页）
export const getBookList = (params?: BookListParams) => {
  return httpGet<PagedResponse<Book>>('/portal/reading/books', params);
};

// 获取书籍详情
export const getBookDetail = (bookId: string | number) => {
  return httpGet<{ book: Book; quotes: BookQuote[] }>(`/portal/reading/books/${bookId}`);
};

// =====================================================
// 读书空间 - 书单
// =====================================================

// 获取书单列表（分页）
export const getBookLists = (params?: {
  categoryId?: number;
  isPublic?: boolean;
  keyword?: string;
  page?: number;
  pageSize?: number;
}) => {
  return httpGet<PagedResponse<BookList>>('/portal/reading/book-lists', params);
};

// 获取书单详情（包含书籍列表）
export const getBookListDetail = (listId: string | number) => {
  return httpGet<{ bookList: BookList; books: Book[] }>(`/portal/reading/book-lists/${listId}`);
};

// 书单点赞
export const likeBookList = (listId: string | number) => {
  return httpPost(`/portal/reading/book-lists/${listId}/like`);
};

// =====================================================
// 读书空间 - 金句摘录
// =====================================================

// 获取金句列表（分页）
export const getQuotes = (params?: {
  bookId?: number;
  isFeatured?: boolean;
  keyword?: string;
  page?: number;
  pageSize?: number;
}) => {
  return httpGet<PagedResponse<BookQuote>>('/portal/reading/quotes', params);
};

// 金句点赞
export const likeQuote = (quoteId: string | number) => {
  return httpPost(`/portal/reading/quotes/${quoteId}/like`);
};
