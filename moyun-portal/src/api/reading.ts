import { httpGet, httpGetList } from './client';
import type {
  Book,
  BookList,
  Quote,
  ReadingHomeResponse,
  BookListParams,
  PaginationResponse,
} from '@/types/api';

// 获取读书空间首页数据
export const getReadingHome = () => {
  return httpGet<ReadingHomeResponse>('/portal/reading/home');
};

// 获取书籍列表
export const getBookList = (params?: BookListParams) => {
  return httpGet<{ list: Book[]; total: number }>('/portal/reading/books', params);
};

// 获取书籍详情
export const getBookDetail = (bookId: string) => {
  return httpGet<Book>(`/portal/reading/books/${bookId}`);
};
